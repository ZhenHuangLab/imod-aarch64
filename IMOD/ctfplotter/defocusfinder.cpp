/*
* defocusfinder.cpp - routines for finding defocus given a 1D power spectrum.
*
*  Authors: Quanren Xiong and David Mastronarde
*
*  Copyright (C) 2008-2018 by the Regents of the University of
*  Colorado.  See dist/COPYRIGHT for full copyright notice.
*
*  $Id$
*/

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include "b3dutil.h"
#include "myapp.h"

#define DEF_START -1.0
#define DEF_END -20.0
#define ZERO_START 0.2
#define ZERO_END 0.8
#define MY_PI 3.1415926

/*
 * Constructor: set the various constants and expected zero and defocus
 */
DefocusFinder::DefocusFinder(int volt, double pSize,
                             double ampContrast, double inputCs,
                             int dim, double expDef): mVoltage(volt), mPixelSize(pSize),
  mAmpRatio(ampContrast), mCs(inputCs), mDim(dim), mExpDefocus(expDef)
{
  mExpDefocus = mExpDefocus / 1000.0; //convert to microns;
  //wavelength in nm;
  mWavelength = 1.23984 / sqrt(mVoltage * (mVoltage +  1022.0));
  mCsOne = sqrt(mCs * mWavelength); // deltaZ=-deltaZ'/mCs1;  In microns
  mCsTwo = sqrt(sqrt(1000000.0 * mCs / mWavelength)); //theta=theta'*mCs2;
  mAmpAngle = 2. * atan(mAmpRatio / sqrt(1. - mAmpRatio * mAmpRatio)) / MY_PI;

  // Compute and set expected zero
  mPlatePhase = 0.;
  mCutOnFreq = 0.;
  setExpDefocus(mExpDefocus);
  mDefocus = -1000.0;
  mAvgDefocus = -1000.;
  mAmpComplement = sqrt(1. - mAmpRatio * mAmpRatio);
}

/*
 * Given the two fitted curves on the left and the right and an interval
 * in which they should intersect, it finds the intersection and returns the
 * zero from that.
 */
int DefocusFinder::findZero(const double *leftRes, const double *rightRes,
                            int x1, int x2, double *zero)
{
  if ((x2 - x1) < 2) {
    printf("findZero() error: range<2\n");
    return -1;
  }
  int dim = x2 - x1 + 1;
  double *diff = (double *)malloc(dim * sizeof(double));
  double minDiff;
  int i, minIndex;
  for (i = x1; i < x1 + dim; i++) {
    diff[i - x1] = leftRes[i] - rightRes[i];
  }

  int middle = dim / 2;

  //find the minimum difference in the first half;
  minDiff = fabs(diff[0]);
  minIndex = 0;
  for (i = 1; i < middle; i++) {
    if (fabs(diff[i]) < minDiff) {
      minDiff = fabs(diff[i]);
      minIndex = i;
    }
  }

  //confirm it is a zerocrossing;
  if ((minIndex - 1) < 0 || (minIndex + 1) > (dim - 1)) {
    printf("findZero() error: the sign of values can not be determined.  \
        zeroIndex=%d\n", minIndex);
    free(diff);
    return -1;
  }

  if (diff[minIndex - 1]*diff[minIndex + 1] < 0.0) { // it is a crossing
    //linear interpolation
    double interpolate = -(diff[minIndex + 1] + diff[minIndex - 1]) /
                         (diff[minIndex + 1] - diff[minIndex - 1]);
    *zero = (double)(x1 + minIndex + interpolate) / (mDim - 1);
    mZeroCrossing = *zero;
    free(diff);
    return 0;
  } else {

    // If that was not a zero crossing, find the minimum in the second half
    minDiff = fabs(diff[middle]);
    minIndex = middle;
    for (i = middle; i < dim; i++) {
      if (fabs(diff[i]) < minDiff) {
        minDiff = fabs(diff[i]);
        minIndex = i;
      }
    }
    if ((minIndex - 1) < 0 || (minIndex + 1) > (dim - 1)) {
      printf("findZero() error: the sign of values can not be determined.  \
          zeroIndex=%d\n", minIndex);
      free(diff);
      return -1;
    }
    if (diff[minIndex - 1]*diff[minIndex + 1] < 0.0) { // it is a crossing
      //linear interpolation
      double interpolate = -(diff[minIndex + 1] + diff[minIndex - 1]) /
                           (diff[minIndex + 1] - diff[minIndex - 1]);
      *zero = (double)(x1 + minIndex + interpolate) / (mDim - 1);
      mZeroCrossing = *zero;
      free(diff);
      return 0;
    }
  }//else

  printf("findZero(): no intersection found\n");
  free(diff);
  return -1;
}

// Removed by DNM 7/7/09, available in revision 1.7
//older version, the same expected defocus may give slightly different defocus
//estimate depending on the current defocus found.
/*void DefocusFinder::setExpDefocus(double expDef) */

/*
 * Sets mExpDefocus and computes mExpZero from the input value expDef
 */
void DefocusFinder::setExpDefocus(double expDef)
{
  mExpDefocus = expDef;
  mExpZero = getAnyOneZero(expDef, 1);
}

/*
 * Returns the first and second zero for the given focus, as fraction of Nyquist
 * phase and cutOnFreq default to NO_PHASE_VALUE
 */
void DefocusFinder::getTwoZeros(double focus, double &firstZero, double &secondZero, 
                                double phase, double cutOnFreq)
{
  firstZero = getAnyOneZero(focus, 1, phase, cutOnFreq);
  secondZero = getAnyOneZero(focus, 2, phase, cutOnFreq);
}

double DefocusFinder::computeOneZero(double focus, int zeroNum, double phase)
{
  double delz = focus / mCsOne;
  double theta = sqrt(delz - sqrt(B3DMAX(0., delz * delz + mAmpAngle + 2. *
                                         (phase > PHASE_TEST ? phase : mPlatePhase) / 
                                         MY_PI - 2. * zeroNum)));
  return theta * mPixelSize * 2.0 / (mWavelength * mCsTwo);
}

/*
 * returns the frequency of the given zero at the given defocus
 * phase and cutOnFreq default to NO_PHASE_VALUE
 */
double DefocusFinder::getAnyOneZero(double focus, int zeroNum, double phase, 
                                    double cutOnFreq)
{
  double zero, fracAtRef, lastZero = -1.;
  int iter = 0;
  phase = (phase > PHASE_TEST ? phase : mPlatePhase);
  cutOnFreq = (cutOnFreq > PHASE_TEST ? cutOnFreq : mCutOnFreq);
  zero =  computeOneZero(focus, zeroNum, phase);
  if (phase && cutOnFreq) {
    fracAtRef = 1. / (1. - exp(-FREQ_FOR_PHASE / cutOnFreq));
    while (iter < 10 && fabs(lastZero - zero) > 1.e-4) {
      lastZero = zero;
      zero = computeOneZero(focus, zeroNum, phase * fracAtRef *
                            (1. - exp(-zero / (2. * mPixelSize * cutOnFreq))));
      iter++;
    }
  }
  return zero;
}

/*
 * Returns the number of zeros out to the given frequency, as fraction of Nyquist
 * phase and cutOnFreq default to NO_PHASE_VALUE
 */
int DefocusFinder::getNumberOfZerosInRange(double focus, double nyquistFrac,
                                           double phase, double cutOnFreq)
{
  int numZeros;
  for (numZeros = 0; numZeros < 1000; numZeros++)
    if (getAnyOneZero(focus, numZeros + 1, phase, cutOnFreq) > nyquistFrac)
      break;
  return numZeros;
}

/*
 * Find the defocus from the current zero crossing
 */
int DefocusFinder::findDefocus(double *focus)
{
  double theta;
  theta = (mZeroCrossing * mWavelength * mCsTwo) * 0.5 / mPixelSize;
  mDefocus = mCsOne * (pow(theta, 4.) + 2. - mAmpAngle - 2. * mPlatePhase / MY_PI) / 
    (2. * theta * theta);

  if (debugLevel >= 1)
    printf("defocus=%f   \n", mDefocus);
  *focus = mDefocus;
  return 0;
}

/*
 * Find the defocus from the second zero
 */
double DefocusFinder::defocusFromSecondZero(double zero)
{
  double theta;
  theta = (zero * mWavelength * mCsTwo) * 0.5 / mPixelSize;
  return(mCsOne * (pow(theta, 4.) + 4. - mAmpAngle - 2. * mPlatePhase / MY_PI) /
         (2. * theta * theta));
}

/*
 * Return the CTF function value at the given frequency and defocus, and optionally phase
 * and cut-on frequency, which default to NO_PHASE_VALUE
 * Frequency in fraction of Nyquist, defocus in microns, phase in radians
 */
double DefocusFinder::CTFvalue(double freq, double def, double phase, double cutOnFreq)
{
  double theta = (freq * mWavelength * mCsTwo) * 0.5 / mPixelSize;
  double delz = def / mCsOne;
  double phaseFrac = 1.;
  cutOnFreq = cutOnFreq > PHASE_TEST ? cutOnFreq : mCutOnFreq;
  if (cutOnFreq > 0.)
    phaseFrac = (1. - exp(-freq / (2. * mPixelSize * cutOnFreq))) / 
      (1. - exp(-FREQ_FOR_PHASE / cutOnFreq));
  double phi = 0.5 * MY_PI * (pow(theta, 4.) - 2. * theta * theta * delz) - 
    phaseFrac * (phase > PHASE_TEST ? phase : mPlatePhase);
  return (-2. * (mAmpComplement * sin(phi) - mAmpRatio * cos(phi)));
}

/*
 * Get two factors from which CTF can be computed for any defocus at the given frequency,
 * phase, and cut-on
 */
void DefocusFinder::precomputeCTFfactors(double freq, double phase, double cutOnFreq, 
                                         double &phiFixed, double &phiVary)
{
  double theta = (freq * mWavelength * mCsTwo) * 0.5 / mPixelSize;
  double phaseFrac = 1.;
  cutOnFreq = cutOnFreq > PHASE_TEST ? cutOnFreq : mCutOnFreq;
  if (cutOnFreq > 0.)
    phaseFrac = (1. - exp(-freq / (2. * mPixelSize * cutOnFreq))) /
      (1. - exp(-FREQ_FOR_PHASE / cutOnFreq));
  phiFixed = 0.5 * MY_PI * pow(theta, 4.) - 
    phaseFrac * (phase > PHASE_TEST ? phase : mPlatePhase);
  phiVary = -0.5 * MY_PI * 2. * theta * theta / mCsOne;
}

/* 
 * Use the precomputed factors to return the CTF value for a given defocus
 */
double DefocusFinder::CTFvalueFromPhiFactors(double def, double phiFixed, double phiVary)
{
  double phi = phiFixed + def * phiVary;
  return (-2. * (mAmpComplement * sin(phi) - mAmpRatio * cos(phi)));
}

/*
 * Find the maximum focus range around the given focus such that the last zero in the
 * frequency range given by freqLim will not shift by more than critShift times the 
 * interzero distance.  Phase and cutOnFreq default to NO_PHASE_VALUE
 */
double DefocusFinder::findTolerance(double freqLim, double critShift, double focus, 
                                    double phase, double cutOnFreq)
{
  double lastZero, shiftedZero, interZero, shift, goodShift[2] = {0.005, 0.005};
  int numZeros = getNumberOfZerosInRange(focus, freqLim, phase, cutOnFreq);
  lastZero = getAnyOneZero(focus, numZeros, phase, cutOnFreq);
  interZero = getAnyOneZero(focus, numZeros + 1, phase, cutOnFreq) - lastZero;
  for (int dirLoop = 0; dirLoop < 2; dirLoop++) {
    for (int delLoop = 0; delLoop < 1000; delLoop++) {
      shift = (2 * delLoop - 1) * 0.005;
      shiftedZero = getAnyOneZero(focus + shift, numZeros, phase, cutOnFreq);
      if (fabs(shiftedZero - lastZero) > critShift * interZero)
        break;
      goodShift[dirLoop] = fabs(shift);
    }
  }
  return 2. * B3DMIN(goodShift[0], goodShift[1]);
}
   
