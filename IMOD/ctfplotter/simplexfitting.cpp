/*
* simplexfitting.cpp - Fits Gaussian or CTF curve to segment of a 1D power
*                      spectrum by the simplex method.
*
*  Authors: Quanren Xiong and David Mastronarde
*
*  Copyright (C) 2008-2016 by the Regents of the University of
*  Colorado.  See dist/COPYRIGHT for full copyright notice.
*
*  $Id$
*/
#include <math.h>
#include <stdio.h>
#include "myapp.h"
#include "simplexfitting.h"
#include "parse_params.h" //for exitError()

#define VARNUM 9
#define MAX_ITER 100
#define MIN_ERROR 0.1

// Sorry, C++ purists, I got tired of declaring class statics in two places
static int sDim = 0;
static int sIndex1 = 0;
static int sIndex2 = 0;
static int sPowerInd = 4;
static bool sFitPhase = false;
static int sPhaseInc;
static double sPhaseShiftForFit;
static double sCutOnForFit;
static double * sRaw = NULL;
static double *sDelx = NULL;
static double *sPhiFixed = NULL;
static double *sPhiVary = NULL;
static DefocusFinder *sFinder = NULL;
static int sNumVar = 0;
static float sA[10] = {0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
static double sExpZero = 0.;
static double sExpSecondZero = 0.;
static float *sBaseFreq = NULL;
static float *sBaseAvg = NULL;
static float *sBaseWgt = NULL;
static int sNumBase = 0;
static int sBaseOrder;
static double sMinErr;
static vector < float > sWedgeAngle;
static vector < float > sFitDefocus;
static vector < float > sAstigResids;
static vector < float > sFitWeights;
static float sWedgeRange;
static float sMinPhase;
static float sMaxPhase;
static float sMinDefocus = 0.;
static float sMaxDefocus = 0.;
static float sMaxCutOnFreq;


//SimplexFitting::SimplexFitting(double *rawData, int nRaw, int i_1, int i_2)
SimplexFitting::SimplexFitting(int nRaw, MyApp *app)
{
  mApp = app;
  sRaw = new double[nRaw];
  sDelx = new double[nRaw];
  sPhiFixed = new double[nRaw];
  sPhiVary = new double[nRaw];
  sBaseFreq = new float[nRaw];
  sBaseAvg = new float[nRaw];
  sBaseWgt = new float[nRaw];
  sDim = nRaw;
  sFinder = &mApp->mDefocusFinder;
  mLastNumCTFvars = 0;
  mDefocusRange = 0.;
}

SimplexFitting::~SimplexFitting()
{
  delete[] sRaw;
  delete[] sBaseFreq;
  delete[] sBaseAvg;
  delete[] sBaseWgt;
  delete [] sDelx;
  delete [] sPhiFixed;
  delete [] sPhiVary;
}

void SimplexFitting::setRaw(double *rawData)
{
  for (int i = 0;i < sDim;i++)
    sRaw[i] = rawData[i];
}

void SimplexFitting::setRange(int n1 , int n2)
{
  sIndex1 = n1;
  sIndex2 = n2;
}

/*
 * Fit to a gaussian
 */
int SimplexFitting::fitGaussian(double * fitting, double &err, int
                                   howToInitParams)
// mIndex1 and mIndex2 are the
//starting index and endind index of the fitting range.
{
  float yy[VARNUM + 1];
  float da[VARNUM] = {2.0, 2.0, 2.0, 2.0, 2.0};
  float a[VARNUM] = {0.7, 0.0, 0.2, 0.0, 0.0};
  float min_a[VARNUM];
  int nvar = 4;
  int iter, i,  iter_counter;
  float errmin;

  float delfac = 2.0;
  float ptolFacs[2] = {5.0e-4, 1.0e-5};
  float ftolFacs[2] = {5.0e-4, 1.0e-5};

  //init params
  if (howToInitParams == 0) {
   a[0] = sRaw[sIndex1];
   a[2] = 1.414 *(sIndex2 - sIndex1) / (sDim - 1);
  } else {
   a[0] = sRaw[(sIndex1 + sIndex2) / 2 - 1];
   a[1] = 0.5 *(sIndex2 - sIndex1) / (sDim - 1);
   a[2] = 1.414 *0.3 *(sIndex2 - sIndex1) / (sDim - 1);
  }

  //find the range of fitting data;
  double min, max;
  double range;
  if ( (sIndex2 - sIndex1) > 0 ){
     if ( sRaw[sIndex1] > sRaw[sIndex1 + 1] ){
       max = sRaw[sIndex1];
       min = sRaw[sIndex1 + 1];
     }else{
       min = sRaw[sIndex1];
       max = sRaw[sIndex1 + 1];
     }
     for (i = sIndex1 + 1;i < sIndex2;i++){
       if ( sRaw[i] > max )
         max = sRaw[i];
       else if ( sRaw[i] < min)
         min = sRaw[i];
     }
     range = max - min;
  } else
    range = sRaw[sIndex1];

  iter_counter = 0;
  err = 100000.0;
  double scaling;

  while(iter_counter < MAX_ITER){
    dualAmoeba(yy, nvar, delfac, ptolFacs, ftolFacs, a, da, &SimplexFitting::funk, &iter);

    funk(a, &errmin);
    if ( (errmin < err || errmin < MIN_ERROR *range) && a[0] > 0.0){
      err = errmin;
      for (i = 0;i < nvar;i++)
        min_a[i] = a[i];
    }

    iter_counter++;
    if (errmin < MIN_ERROR *range && a[0] > 0.0)
      break;

    //re-initialize parameters
    if (iter_counter % 2)
      scaling = 0.5 *iter_counter / (MAX_ITER - 1);
    else
      scaling = -0.5 *iter_counter / (MAX_ITER - 1);

    a[0] = (1 + scaling) *sRaw[sIndex1];
    if (howToInitParams == 0)
      a[1] = 0.0;
    else
      a[1] = 0.5 *(sIndex2 - sIndex1) / (sDim - 1);
    a[2] = (1 + scaling) *1.414 *(sIndex2 - sIndex1) / (sDim - 1);
    a[3] = 0.0;
  } // iteration

  for (i = 0; i < sDim; i++) {
    fitting[i] = min_a[0] *exp( -((float)(i - sIndex1) / (sDim - 1) - min_a[1]) *
        ((float)(i - sIndex1) / (sDim - 1) - min_a[1]) / (min_a[2] *min_a[2])) + min_a[3];
  }

  if ( debugLevel >= 3){
   printf("Iteration Num = % d threshold = % f Simplex fitting parameters for \
      range % d to % d are:\n", iter_counter, MIN_ERROR *range, sIndex1, sIndex2);
   printf("Fitting error=%f\t a[0]=%f\t a[1]=%f\t a[2]=%f\t a[3]=%f\n", err,
      min_a[0], min_a[1], min_a[2], min_a[3]);
  }
  fflush(stdout);
  return 0;
}

/*
 * Callback function for simplex search for a gaussian
 */
void SimplexFitting::funk(float * param, float * fValue)
{
  int i;
  double x;
  *fValue = 0.0;
  // This is not a sum of squares!
  for (i = sIndex1; i < sIndex2; i++) {
    x = (double)(i - sIndex1) / (sDim - 1.) - param[1];
    *fValue = *fValue + fabs(sRaw[i] - param[0] *
                           exp(-(x * x) / (param[2] * param[2])) - param[3]);
  }
}

static bool printErr;
/*
 * Fit to a CTF-like curve
 */
int SimplexFitting::fitCTF(int nvar, double initialPhase, double initialCutOn,
                           double fixedFocus, double * fitting, double &err, double &focus,
                           double &phase, double &cuton)
{
  float yy[VARNUM + 1];
  float da[VARNUM] = {2.0, 2.0, 2.0, 2.0, 0.1};
  int iter, i;
  float errmin;
  float delfac = 2.0;
  float ptolFacs[2] = {5.0e-4, 1.0e-5};
  float ftolFacs[2] = {5.0e-4, 1.0e-5};
  double startDef;
  double rawMin = 1.e30;
  double rawMax = -1.e30;
  bool lastFitPhase = sFitPhase;
  sPhaseInc = 0;
  printErr = false;


  // Get starting defocus depending on current option setting, and get
  // the zero at that defocus
  sPhaseShiftForFit = initialPhase;
  sCutOnForFit = initialCutOn;
  startDef = mApp->getStartingDefocusAndZeros(sExpZero, sExpSecondZero);
  if (fixedFocus > 0) {
    startDef = fixedFocus;
    sFinder->getTwoZeros(fixedFocus, sExpZero, sExpSecondZero, initialPhase,
                         initialCutOn);
    sMinDefocus = sMaxDefocus = startDef;
  } else if (mDefocusRange > 0.) {
    sMinDefocus = startDef - mDefocusRange / 2.;
    sMaxDefocus = startDef + mDefocusRange / 2.;
  }

  sFitPhase = initialPhase > PHASE_TEST && nvar < 4;
  if (sFitPhase) {
    sPhaseInc = 2;

    // Shift solution from no phase just in case parts of it are being reused
    if (!lastFitPhase) {
      for (i = VARNUM - 1; i >= 2; i--)
        sA[i] = sA[i - sPhaseInc];
      sPowerInd += sPhaseInc;
    }
    sA[1] = initialPhase;
    da[1] = 0.1;    // radians
    sA[2] = initialCutOn;
    sMaxCutOnFreq = mApp->getMaxCutOnFreq();
    da[2] = sMaxCutOnFreq / 4.;
    errmin = mApp->getPhaseSearchRange();
    sMinPhase = initialPhase - errmin / 2;
    sMaxPhase = initialPhase + errmin / 2;
  }

  // Initialize values, or leave previous values for some kinds of
  // restricted fits
  sNumVar = nvar;
  for (i = sIndex1; i <= sIndex2; i++) {
    rawMin = B3DMIN(rawMin, sRaw[i]);
    rawMax = B3DMAX(rawMax, sRaw[i]);
  }
  sA[0] = startDef;
  if (nvar > 1 + sPhaseInc) {
    sA[1 + sPhaseInc] = rawMin - 0.1 * (rawMax - rawMin);
    da[1 + sPhaseInc] = 0.1 * (rawMax - rawMin);
  }
  if (nvar > 2 + sPhaseInc) {
    sA[2 + sPhaseInc] = (sRaw[sIndex1] - rawMin) /
      sFinder->CTFvalue(sIndex1 / (sDim - 1.), startDef, initialPhase, initialCutOn);
    da[2 + sPhaseInc] = 0.2 * sA[2 + sPhaseInc];
  }
  if (nvar > 3 + sPhaseInc) {
    sA[3 + sPhaseInc] = 10.;
    sPowerInd = 4 + sPhaseInc;
    if (nvar > 5 + sPhaseInc) {
      sPowerInd = 6 + sPhaseInc;
      sA[4 + sPhaseInc] = sA[2 + sPhaseInc] / 10.;
      sA[5 + sPhaseInc] = 1.;
      da[4 + sPhaseInc] = 0.2 * sA[4 + sPhaseInc];
      da[5 + sPhaseInc] = 0.2;
    }

    // Initialize power for search or use, unless it is reusable from last search,
    // i.e. phase was
    // added and # of variables is the same as last time, meaning power was dropped
    if (!(sFitPhase && !lastFitPhase && mLastNumCTFvars == nvar)) {
      sA[sPowerInd] = 1.5;
      da[sPowerInd] = 0.1;
    }
  }
  mLastNumCTFvars = nvar;

  // Now that all that is set up, slide everything down one position with fixed focus
  if (fixedFocus > 0.) {
    for (i = 1; i < nvar; i++) {
      sA[i - 1] = sA[i];
      da[i - 1] = da[i];
    }
    nvar--;
  }

  // precompute delx and simple factors for computing CTF if phase/cuton are fixed
  if (!sFitPhase) {
    for (i = 0; i < sDim; i++) {
      sDelx[i] = (double)(i - sIndex1) / (sDim - 1.);
      sFinder->precomputeCTFfactors
        ((double)i / (sDim - 1.), sPhaseShiftForFit, sCutOnForFit, sPhiFixed[i],
         sPhiVary[i]);
    }
  }

  dualAmoeba(yy, nvar, delfac, ptolFacs, ftolFacs, sA, da, &SimplexFitting::funkCTF,
             &iter);

  //printErr = true;
  funkCTF(sA, &errmin);

  // Roll variables up and add focus in
  if (fixedFocus > 0.) {
    for (i = nvar; i > 0; i--)
      sA[i] = sA[i - 1];
    sA[0] = fixedFocus;
  }

  err = sqrt(errmin / (sIndex2 + 1 - sIndex1));
  focus = sA[0];
  if (sFitPhase) {
    phase = sA[1];
    if (sNumVar == 3)
      cuton = sA[2];
  }

  for (i = 0; i < sDim; i++) {
    fitting[i] = CTFvalueAtIndex(i, sA);
  }
  printCTFresult(err);
  mLastError = err;
  sMinDefocus = sMaxDefocus = 0.;
  fflush(stdout);
  return 0;
}

/*
 * Computes the CTF value at the given index for the params array and other aspects of fit
 *
 */
double SimplexFitting::CTFvalueAtIndex(int ind, float *params)
{
  double ctfval, delx, x;
  int phInc = sPhaseInc;

  // Get the model value from the routine if finding phase, or use pre-computed factors
  // if not
  if (sFitPhase) {
    x = (double)ind / (sDim - 1.);
    delx = (double)(ind - sIndex1) / (sDim - 1.);
    ctfval = sFinder->CTFvalue(x, (double)params[0],
                               sFitPhase ? (double)params[1] : sPhaseShiftForFit,
                               sFitPhase ? params[2] : sCutOnForFit);
  } else {
    delx = sDelx[ind];
    ctfval = sFinder->CTFvalueFromPhiFactors((double)params[0], sPhiFixed[ind],
                                             sPhiVary[ind]);
  }

  // Apply the current fitting formula
  if (sPowerInd == 4 + phInc)
    return params[1 + phInc] + params[2 + phInc] * exp(-params[3 + phInc] * delx) *
      pow(fabs(ctfval), (double)params[4 + phInc]);
  else
    return params[1 + phInc] + (params[2 + phInc] * exp(-params[3 + phInc] * delx) +
                                params[4 + phInc] * exp(-params[5 + phInc] * delx)) *
      pow(fabs(ctfval), (double)params[6 + phInc]);
}

/*
 * Recompute a previously fit CTF-like curve at given defocus
 */
void SimplexFitting::recomputeCTF(double * result, double defocus)
{
  int i;
  float err;

  sA[0] = defocus;
  funkCTF(sA, &err);
  err = sqrt(err);
  for (i = 0; i < sDim; i++) {
    result[i] = CTFvalueAtIndex(i, sA);
  }
  printCTFresult(err);
  mLastError = err;
  fflush(stdout);
}

void SimplexFitting::printCTFresult(float err)
{
  if (debugLevel >= 1) {
    if (sPhaseShiftForFit > PHASE_TEST && !sFitPhase)
      printf("Phase= %.1f   Cuton=%.3f   ", sPhaseShiftForFit / RADIANS_PER_DEGREE,
             sCutOnForFit);
    printf("CTF fitting parameters for range %d to %d are:\n", sIndex1, sIndex2);
    if (sFitPhase) {
      if (sPowerInd == 6)
        printf("Fit error=%f\t def=%f\t pha=%f\t cuton=%f\t base=%f\t scale=%g\t "
               "decay=%f\t pow=%f\n", err, sA[0], sA[1] / RADIANS_PER_DEGREE,
               sA[2], sA[3], sA[4], sA[5], sA[5]);
      else
        printf("Fit error=%f\t def=%f\t pha=%f\t cuton=%f\t base=%f\t scale=%g\t "
               "decay=%f\t pow=%f\t scale2=%g\t decay2=%f\n" , err, sA[0],
               sA[1] / RADIANS_PER_DEGREE, sA[2], sA[3], sA[4], sA[5], sA[8], sA[6],
               sA[7]);
    } else {
      if (sPowerInd == 4)
        printf("Fit error=%f\t def=%f\t base=%f\t scale=%g\t decay=%f\t pow=%f\n",
               err, sA[0], sA[1], sA[2], sA[3], sA[4]);
      else
        printf("Fit error=%f\t def=%f\t base=%f\t scale=%g\t decay=%f\t pow=%f\t "
               "scale2=%g\t decay2=%f\n" , err, sA[0], sA[1], sA[2], sA[3], sA[6], sA[4],
               sA[5]);
    }
  }
}

/*
 * The callback function for the simplex search fitting to a CTF
 */
void SimplexFitting::funkCTF(float * param, float * fValue)
{
  double x, y, err, zero1, zero2, diff;
  int i, j, indStart, indEnd, avgWidth = (sDim - 1) * (sExpSecondZero - sExpZero);
  float parUse[VARNUM];
  int indKeepPos[5] = {0, 2, 3, 4, 5};
  int numKeepPos = (sPowerInd == 4 + sPhaseInc) ? 3 : 5;
  int useInc = 0;

  // For fixed focus, set it into first use parameter and set increment, then
  // slide the incoming parameters up
  if (sMinDefocus > 0. && fabs(sMaxDefocus - sMinDefocus) < 1.e-6) {
    parUse[0] = sMinDefocus;
    useInc = 1;
  }

  for (i = useInc; i < sNumVar; i++)
    parUse[i] = param[i - useInc];
  for (i = sNumVar; i < VARNUM; i++)
    parUse[i] = sA[i];

  if (sFitPhase)
    for (i = 1; i < numKeepPos; i++)
      indKeepPos[i] += sPhaseInc;

  // Compute the error
  err = 0.;
  for (i = sIndex1; i <= sIndex2; i++) {
    y = CTFvalueAtIndex(i, parUse) - sRaw[i];
    err += y * y;
  }

  // Make error much bigger if power gets too far from 1
  if (parUse[sPowerInd] > 2. || parUse[sPowerInd] < 0.5) {
    x = B3DMAX(parUse[sPowerInd] - 2., 0.5 - parUse[sPowerInd]);
    err *= 5 * (1 + 5 * x);
  }

  // If the defocus places the second zero closer to the expected defocus than
  // the first zero, make error bigger
  sFinder->getTwoZeros(parUse[0], zero1, zero2, sPhaseShiftForFit, sCutOnForFit);
  if (fabs(sExpZero - zero2) < 0.33 * fabs(sExpZero - zero1)) {
    err *= 5. * (3. - fabs(sExpZero - zero2) / sExpZero);
  } else if (sExpZero > zero2) {
    err *= 5. * (1 + 5. * (sExpZero - zero2));
  } else if (fabs(zero1 - sExpSecondZero) < 0.33 * fabs(zero1 - sExpZero)) {
    err *= 5. * (3. - fabs(zero1 - sExpSecondZero) / zero1);
  } else if (zero1 > sExpSecondZero) {
    err *= 5. * (1 + 5. * (zero1 - sExpSecondZero));
  }
  if (zero1 > 0.9) {
    err *= 5. * (1. + 5. * (zero1 - 0.9));
  }
  if (!useInc && sMaxDefocus > 0.) {
    if (parUse[0] > sMaxDefocus)
      err *= 5. * (1. + parUse[0] - sMaxDefocus);
    if (parUse[0] <  sMinDefocus)
      err *= 5. * (1. + sMinDefocus - parUse[0]);
  }


  // Make error much bigger if defocus becomes negative (overfocus), or if a scale or
  // decay factor does
  for (i = 0; i < numKeepPos; i++)
    if (parUse[indKeepPos[i]] < 0.0)
      err *= 5. * (1. - 5. * parUse[indKeepPos[i]]);

  // Keep the phase and the cut-on frequency in range
  if (sFitPhase && (parUse[1] < sMinPhase || parUse[1] > sMaxPhase))
    err *= 5.;
  if (sFitPhase && sNumVar > 2 && (parUse[2] < 0. || parUse[2] > sMaxCutOnFreq))
    err *= 5.;

  *fValue = (float)err;
  /*printf("err=%f  def=%f  fc=%f  scale=%f  decay=%f  sc2=%f  dec2=%f\n", err,
    parUse[0], parUse[1], parUse[2], parUse[3], parUse[4], parUse[5]);*/
}

/*
 * Fit a polynomial to smoothed points, mostly at minima if they exist, constraining the
 * polynominal to have a monotonic first derivative, and produce a baseline to subtract
 * from the log power
 */
void SimplexFitting::fitBaseline(double *average, double *baseline, int order,
                                 bool subtractBase)
{
  const int numSearchIters = 5;
  float yy[VARNUM + 1];
  float da[VARNUM];
  float var[VARNUM];
  float varSave[VARNUM][numSearchIters];
  float errSave[numSearchIters];
  int iter, indZero, numBound, numUpBound, numDownBound, bestIter;
  float errmin, temp;
  float delfac = 2.0;
  float ptolFacs[2] = {5.0e-4, 1.0e-5};
  float ftolFacs[2] = {5.0e-4, 1.0e-5};
  double rawMin = 1.e30;
  double rawMax = -1.e30;
  int boxSize = 5;
  float minSearchFrac = 0.5;
  int tailInterval = 3, maxInterForGoodCheck = 9;
  float goodMinFrac = 0.75, goodMaxFrac = 1.25;
  int left = boxSize / 2;
  int right = boxSize - 1 - boxSize / 2;
  int loop, box, ind, indStart, indEnd, nvar, indLast = sDim, lastGoodMin = 0;
  int numAtLastGood = 0;
  float sigma = 1.5, wsum = 0.;
  float sigWeights[10];
  float *interZeros;
  bool checkGoodMin;
  double err, x0, freq, firstFit;
  double *boxAvg, *newBaseline, *allZeros;
  float *work, *xbound, *ybound, *splineWork;
  float *upXbound, *upYbound, *downXbound, *downYbound;

  if (!subtractBase)
    for (ind = 0; ind < sDim; ind++)
      baseline[ind] = 0.;
  if (!order) {
    err = 0.;
    indStart = sDim / 2;
    indEnd = (int)(0.95 *sDim);
    for (ind = indStart; ind < indEnd; ind++)
      err += (average[ind] - baseline[ind]) / (indEnd - indStart);
    for (ind = 0; ind < sDim; ind++)
      baseline[ind] += err;
    return;
  }

  boxAvg = B3DMALLOC(double, sDim);
  newBaseline = B3DMALLOC(double, sDim);
  ind = (order + 2) * sDim + (order + 1) * (order + 3);
  work = B3DMALLOC(float, ind);
  if (!work || !newBaseline || !boxAvg)
    exitError("Allocating memory for fitting arrays");

  interZeros = work;
  allZeros = newBaseline;
  mApp->getStartingDefocusAndZeros(x0, err, allZeros, sDim - 1);

  // Set sigma for gaussian smoothing proportional to interzero distance within 1 to 2
  // Make weights for the given box size and Gaussian sigma
  sigma = 0.1 * (err - x0) * (sDim - 1);
  B3DCLAMP(sigma, 1., 2.5);
  for (ind = 0; ind < boxSize; ind++) {
    sigWeights[ind] = exp(-0.5 * pow((double)(ind - left) / sigma, 2.));
    wsum += sigWeights[ind];
  }
  for (ind = 0; ind < boxSize; ind++)
    sigWeights[ind] /= wsum;

  // Gaussian smooth the curve
  for (box = 0; box < sDim; box++) {
    boxAvg[box] = 0.;
    for (loop = -left; loop <= right; loop++) {
      ind = loop + box;
      B3DCLAMP(ind, 0, sDim - 1);
      boxAvg[box] += sigWeights[loop + left] * (average[ind] - baseline[ind]);
    }
  }

  // Search from  first zero to a certain point for true minima
  ind = B3DMAX(3, B3DNINT(0.12 * x0 * (sDim - 1)));
  indStart = (int)floor(x0 * (sDim - 1)) - ind;
  indStart = B3DMAX(sIndex1, indStart);
  indEnd = indStart + B3DNINT(minSearchFrac * (sDim - indStart));
  sNumBase = 0;
  for (loop = 0; loop < 2; loop++) {
    for (ind = indStart; ind <= indEnd; ind++) {
      if (boxAvg[ind] < boxAvg[ind - 1] && boxAvg[ind] < boxAvg[ind + 1]) {

        // It must be monotonic to the next points up on either side, or to next point on
        // one side and at least continuing to rise at point after that on the other
        // But on second loop through, just a minimum will do
        if (loop || 
            ((boxAvg[ind - 1] < boxAvg[ind - 2] && 
              (boxAvg[ind + 1] < boxAvg[ind + 2] || boxAvg[ind + 1] < boxAvg[ind + 3])) ||
             (boxAvg[ind + 1] < boxAvg[ind + 2] && 
              (boxAvg[ind - 1] < boxAvg[ind - 2] || boxAvg[ind - 1] < boxAvg[ind - 3])))) {
          addBasePoint(ind, boxAvg, indLast, rawMin, rawMax);
          lastGoodMin = indLast;
          numAtLastGood = sNumBase;
        }
      }
    }
    if (sNumBase > 0)
      break;
  }

  if (sNumBase > 0)
    indStart = min(indLast, indEnd) + 1;

  // Analyze the interzero distances from here outward to see if we should try to
  // pick only the bottom of zeros
  indZero = 0;
  checkGoodMin = true;
  for (ind = indStart; ind < sDim - 1; ind++) {
    while (ind / (sDim - 1.) > allZeros[indZero] && indZero < sDim - 2)
      indZero++;
    interZeros[ind] = (allZeros[indZero + 1] - allZeros[indZero]) * (sDim - 1);
    if (B3DNINT(goodMinFrac * interZeros[ind]) > maxInterForGoodCheck)
      checkGoodMin = false;
  }

  // Now add any local minima and points at minimum intervals after the first local
  // minimum if there have been no points before this
  for (ind = indStart; ind < (int)(0.95 * sDim); ind++) {
    bool localMin = boxAvg[ind] < boxAvg[ind - 1] && boxAvg[ind] < boxAvg[ind + 1];
    if (localMin ||
        (sNumBase > 0 && ind - indLast >= tailInterval &&
         (!checkGoodMin || ind - lastGoodMin >= B3DNINT(goodMinFrac * interZeros[ind])))){
      if (localMin) {
        if (sNumBase > 0 && ind - indLast < 2 && indLast != lastGoodMin) {
          //printf("Local Min: Removing %.3f as too close\n", sBaseFreq[sNumBase - 1]/2.);
          sNumBase--;
        }  //else
          //printf("local min\n");
      }  //else
      //printf("min interval\n");
      addBasePoint(ind, boxAvg, indLast, rawMin, rawMax);
      if (localMin && boxAvg[ind - 1] < boxAvg[ind - 2] &&
          boxAvg[ind + 1] < boxAvg[ind + 2]) {
        if (numAtLastGood > 0 && ind - lastGoodMin <= 
            B3DNINT(goodMaxFrac * interZeros[ind]) && numAtLastGood < sNumBase - 1) {
          //printf("Good Min: Removing from %.3f to %.3f\n", sBaseFreq[numAtLastGood]/2.,
          //sBaseFreq[sNumBase - 2]/2.);
          sBaseFreq[numAtLastGood] = sBaseFreq[sNumBase - 1];
          sBaseAvg[numAtLastGood] = sBaseAvg[sNumBase - 1];
          sNumBase = numAtLastGood + 1;
        } else
        //printf("Good min %d  %f %f\n", ind - lastGoodMin, interZeros[ind],
          //goodMaxFrac * interZeros[ind]);
        lastGoodMin = indLast;
        numAtLastGood = sNumBase;
      }
    }
  }

  order = B3DMAX(0, B3DMIN(order, sNumBase / 2 - 1));

  // Assign weights proportional to square root of point spacing
  // 8/29/18: This was proportional to INVERSE of spacing, no idea why
  // no longer give first point extra weight
  if (order) {
    err = 0.;
    for (ind = 0; ind < sNumBase; ind++) {
      if (!ind)
        sBaseWgt[ind] = 1. * sqrt(sBaseFreq[ind + 1] - sBaseFreq[ind]);
      else if (ind == sNumBase - 1)
        sBaseWgt[ind] = 1. * sqrt(sBaseFreq[ind] - sBaseFreq[ind - 1]);
      else
        sBaseWgt[ind] = 0.5 * (sqrt(sBaseFreq[ind + 1] - sBaseFreq[ind]) +
                              sqrt(sBaseFreq[ind] - sBaseFreq[ind - 1]));
      err += sBaseWgt[ind];
    }
    for (ind = 0; ind < sNumBase; ind++) {
      sBaseWgt[ind] /= (err / sNumBase);
      if (debugLevel > 2)
        printf("1  %6.3f  %g   %g\n", sBaseFreq[ind] / 2., sBaseAvg[ind], sBaseWgt[ind]);
    }
  }

  // Start (or end) with a quadratic or linear fit to initialize the search
  var[2] = var[3] = var[4] = 0.;
  if (order && weightedPolyFit(sBaseFreq, sBaseAvg, sBaseWgt, sNumBase, B3DMIN(order, 2),
                               &var[1], &var[0], work))
    order = 0;
  sBaseOrder = order;
  if (order > 2) {

    // Do simplex fitting
    nvar = order + 1;
    da[0] = da[1] = da[2] = 0.2 * (rawMax - rawMin);
    da[3] = 0.5 * da[2];
    da[4] = 0.5 * da[2];
    errmin = 1.e20;

    // Run it 5 times, varying the start point and adjusting the increments
    for (iter = 0; iter < numSearchIters; iter++) {
      sMinErr = 1.e20;
      dualAmoeba(yy, nvar, delfac, ptolFacs, ftolFacs, var, da, &SimplexFitting::funkBase,
                 &ind);
      funkBase(var, &errSave[iter]);
      for (ind = 0; ind < nvar; ind++) {
        varSave[ind][iter] = var[ind];
        da[ind] = B3DMAX(0.2 * (rawMax - rawMin), 0.5 * var[ind]);
        var[ind] *= (0.7 + 0.2 * iter);
      }
      if (errSave[iter] < errmin) {
        bestIter = iter;
        errmin = errSave[iter];
      }
    }
    for (ind = 0; ind < nvar; ind++)
      var[ind] = varSave[ind][bestIter];
  }

  if (order) {
    funkBase(var, &errmin);
    err = sqrt(errmin);
    if (debugLevel >= 3) { // WAS 1
      printf("Base function fit error=%f  const=%f  coeff=%f", err, var[0], var[1]);
      for (ind = 2; ind <= order; ind++)
        printf("  %f", var[ind]);
      printf("\n");
    }

    // Start the baseline at 0 and compute from the polynomial over the fitted range
    indStart = B3DNINT(sBaseFreq[0] * (sDim - 1.));
    indEnd = B3DNINT(sBaseFreq[sNumBase - 1] * (sDim - 1.));
    freq = sBaseFreq[0];
    for (ind = indStart; ind <= indEnd; ind++) {
      freq = ind  / (sDim - 1.);
      newBaseline[ind] = var[0] + var[1] * freq + var[2] * freq * freq +
        var[3] * freq * freq * freq + var[4] * freq * freq * freq * freq;
      // printf("%3d  %f\n", ind, baseline[ind]);
    }

    // Extrapolate baseline linearly from end of polynomial fit
    for (; ind < sDim; ind++)
      newBaseline[ind] = 2 * newBaseline[ind - 1] - newBaseline[ind - 2];
    for (ind = indStart - 1; ind >= 0; ind--)
      newBaseline[ind] = 2 * newBaseline[ind + 1] - newBaseline[ind + 2];
    if (subtractBase) {
      for (ind = 0; ind < sDim; ind++) {
        //printf("%.3f  %g  %g  %g\n", ind / (mDim - 1.) / 2., newBaseline[ind], baseline[ind], baseline[ind] + newBaseline[ind]);
        baseline[ind] += newBaseline[ind];
      }
    } else {
      for (ind = 0; ind < sDim; ind++) {
        //printf("%.3f  %g\n", ind / (sDim - 1.) / 2., newBaseline[ind]);
        baseline[ind] = newBaseline[ind];
      }
    }
  }

  fflush(stdout);
  free(boxAvg);
  free(newBaseline);
  free(work);
}

// Add a point to the baseline fitting set
void SimplexFitting::addBasePoint(int ind, double *average, int &indLast, double &rawMin,
                                  double &rawMax)
{
  sBaseFreq[sNumBase] = ind / (sDim - 1.);
  sBaseAvg[sNumBase++] = average[ind];
  indLast = ind;
  rawMin = B3DMIN(rawMin, average[ind]);
  rawMax = B3DMAX(rawMax, average[ind]);
  //printf("0  %6.3f  %g\n", ind / (sDim - 1.) / 2., average[ind]);
}

/*
 * The callback function for the simplex search fitting to baseline polynomial
 */
void SimplexFitting::funkBase(float *param, float *fValue)
{
  double delx, delx2, y, err, freq, rootTerm, inflect;
  int ind;

  // Compute error for different orders of polynomial
  err = 0;
  for (ind = 0; ind < sNumBase; ind++) {
    freq = sBaseFreq[ind];
    y = param[0] + param[1] * freq + param[2] * freq * freq - sBaseAvg[ind];
    if (sBaseOrder > 2)
      y += param[3] * freq * freq * freq;
    if (sBaseOrder > 3)
      y += param[4] * freq * freq * freq * freq;
    err += sBaseWgt[ind] * y * y;
  }

  delx = 0.;
  delx2 = 0.;
  inflect = 0.;
  if (sBaseOrder < 4) {

    // Evaluate zero of second derivative for cubic function and set delx to how far
    // inside the fitting interval it is
    if (sBaseOrder > 2 && fabs(param[3]) > 1.e-10) {
      inflect = param[2] / (3. * param[3]);
      if (inflect > sBaseFreq[0] && inflect < sBaseFreq[sNumBase - 1])
        delx = B3DMIN(inflect - sBaseFreq[0], sBaseFreq[sNumBase - 1] - inflect);
    }
  } else {

    // Evaluate two zeros if second derivative of 4th degree function, set either
    // delx or delx2 positive by how far it is inside the fitting interval, take the
    // max of those two
    rootTerm = 9. * param[3] - 24. * param[2] * param[4];
    if (rootTerm >= 0) {
      inflect = (-3 * param[3] + sqrt(rootTerm)) / (6. * param[4]);
      if (inflect > sBaseFreq[0] && inflect < sBaseFreq[sNumBase - 1])
        delx = B3DMIN(inflect - sBaseFreq[0], sBaseFreq[sNumBase - 1] - inflect);
      inflect = (-3 * param[3] - sqrt(rootTerm)) / (6. * param[4]);
      if (inflect > sBaseFreq[0] && inflect < sBaseFreq[sNumBase - 1])
        delx2 = B3DMIN(inflect - sBaseFreq[0], sBaseFreq[sNumBase - 1] - inflect);
      delx = B3DMAX(delx, delx2);
    }
  }

  // Bump up the error, more the farther inside the interval the inflection is
  if (delx > 0)
    err *= 5. * (1. + 5. * delx);
  *fValue = (float)err;
  //printf("%s%f  %f %f %f %f infl %.2f delx %.2f\n", err < mMinErr ? "*" : " ",
  //   sqrt(err), param[0], param[1], param[2], param[3], inflect, delx);
  if (err < sMinErr) {
    sMinErr = err;
  }
}

/*
 * Given the arrays of wedge angles and defocus values, determines the defocus and
 * astigmatism and an error of fit, compensating for wedge thickness
 */
void SimplexFitting::fitWedgesForAstig(vector < float > &wedgeAngles,
                                       vector < float > &fitDefocus, float wedgeRange,
                                       float &meanFocus, float &astigmatism,
                                       float &astigAngle, float &error)
{
  float da[3] = {0.5, 0.5, 10.};
  float aa[3], aaTry[3];
  float work[3 * (5 + MAX_WEDGES)], xx[MAX_WEDGES], yy[MAX_WEDGES];
  float polySlopes[4], polyIntcp;
  int iter, ii, ind, indAng, numOut, numOutTest;
  float errMin;
  float delfac = 2.0;
  float ptolFacs[2] = {5.0e-4, 1.0e-5};
  float ftolFacs[2] = {5.0e-4, 1.0e-5};
  float focus = 0.;
  int numWedges = wedgeAngles.size();

  sWedgeAngle = wedgeAngles;
  sFitDefocus = fitDefocus;
  CLEAR_RESIZE(sFitWeights, float, numWedges);
  CLEAR_RESIZE(sAstigResids, float, numWedges);
  for (ii = 0; ii < numWedges; ii++) {
    sFitWeights[ii] = 1.;
    focus += fitDefocus[ii] / numWedges;
  }
  sWedgeRange = wedgeRange;

  // Do an initial scan search on astigmatism amount and angle
  errMin = 1.e30;
  aa[0] = aa[1] = focus;
  aa[2] = 0.;
  funkAstig(aa, &errMin);
  for (ind = 1; ind <= 10; ind++) {
    for (indAng = -90; indAng < 90; indAng += 10) {
      aaTry[0] = focus + 0.1 * ind;
      aaTry[1] = focus - 0.1 * ind;
      aaTry[2] = indAng;
      funkAstig(aaTry, &error);
      if (error < errMin) {
        errMin = error;
        aa[0] = aaTry[0];
        aa[1] = aaTry[1];
        aa[2] = aaTry[2];
      }
    }
  }
  if (debugLevel > 1)
    printf("Scan   df1 %.3f  df2 %.3f  avg %.3f  astig %.3f  angle %.1f  error %g\n",
           aa[0], aa[1], (aa[0] + aa[1]) / 2., aa[0] - aa[1], aa[2], errMin);

  // Do the amoeba fit
  dualAmoeba(yy, 3, delfac, ptolFacs, ftolFacs, aa, da, &funkAstig, &iter);
  funkAstig(aa, &error);
  if (debugLevel)
    printf("Amoeba df1 %.3f  df2 %.3f  avg %.3f  astig %.3f  angle %.1f  error %g\n",
         aa[0], aa[1], (aa[0] + aa[1]) / 2., aa[0] - aa[1], aa[2], error);

  // Fit local polynomials to find outliers; this is to avoid picking systematic
  // errors as outliers
  numOut = 0;
  numOutTest = B3DMAX(7, numWedges / 4);
  numOutTest = B3DMIN(numOutTest, numWedges);
  for (ii = 0; ii < numWedges; ii++) {
    for (ind = 0; ind < numOutTest; ind++) {
      yy[ind] = sAstigResids[(ii + ind) % numWedges];
      xx[ind] = ind;
    }
    if (!polynomialFit(xx, yy, numOutTest, 2, polySlopes, &polyIntcp, work)) {
      for (ind = 0; ind < numOutTest; ind++)
        yy[ind] -= (ind * polySlopes[0] + ind * ind * polySlopes[1] + polyIntcp);

      rsMadMedianOutliers(yy, numOutTest, 3., work);
      if (work[numOutTest / 2]) {
        sFitWeights[(ii + numOutTest / 2) % numWedges] = 0.;
        numOut++;
      }
    }
  }

  // Report outliers and redo fit if reasonable number found
  if (numOut && numOut < numWedges / 4) {
    if (debugLevel) {
      printf("Outliers: ");
      for (ii = 0; ii < numWedges; ii++)
        if (!sFitWeights[ii])
          printf("%d ", ii);
      printf("\n");
    }

    dualAmoeba(yy, 3, delfac, ptolFacs, ftolFacs, aa, da, &funkAstig, &iter);
    funkAstig(aa, &error);
    if (debugLevel)
      printf("Outlie df1 %.3f  df2 %.3f  avg %.3f  astig %.3f  angle %.1f  error %g\n",
             aa[0], aa[1], (aa[0] + aa[1]) / 2., aa[0] - aa[1], aa[2], error);
  }
  meanFocus = (aa[0] + aa[1]) / 2.;
  astigmatism = aa[0] - aa[1];
  astigAngle = aa[2];
  mLastError = error;
}

void SimplexFitting::funkAstig(float *aa, float *fValue)
{
  float df1 = aa[0];
  float df2 = aa[1];
  float axis = aa[2];
  float pred;
  float sinFac = 2. * RADIANS_PER_DEGREE;
  float range = RADIANS_PER_DEGREE * sWedgeRange;
  int ind;
  double error = 0.;
  for (ind = 0; ind < sWedgeAngle.size(); ind++) {
    pred = 0.5f * (df1 + df2 + (df1 - df2) * 0.5f *
                   ((float)sin(sinFac * (sWedgeAngle[ind] + sWedgeRange / 2. - axis)) -
                    (float)sin(sinFac * (sWedgeAngle[ind] - sWedgeRange / 2. - axis))) /
                   range);
    sAstigResids[ind] = pred - sFitDefocus[ind];
    error += sFitWeights[ind] * sAstigResids[ind] * sAstigResids[ind];
  }
  *fValue = sqrt(error / sWedgeAngle.size());
}

void SimplexFitting::outputWedgeFitVals()
{
  for (int ind = 0; ind < sWedgeAngle.size(); ind++)
    printf("%.2f  %.4f  %.4f  %.4f\n", sWedgeAngle[ind], sFitDefocus[ind], 
           sAstigResids[ind], sAstigResids[ind] + sFitDefocus[ind]);
}

/*
 * Finds zeros in the power spectrum by correlating with windowed pieces of CTF curve
 */
int SimplexFitting::findZerosInPS(double startDef, double initialPhase,
                                  double initialCutOn)
{
  int ind, indc, dist, minDist, minInd, window, numLeft, numRight, hyperInd, left, right;
  int minZeroNum;
  double oneVal, lastVal;
  float slope, intcp, freq, interZero, zeroFreq;
  int hyperRes = 10;
  int hyperDim = hyperRes * sDim;
  std::vector < float > hyperCTF, ctfVal, rawVal, ccc;
  std::vector < int > hyperZeros;
  hyperCTF.resize(2 * hyperDim);
  rawVal.resize(sDim);
  ccc.resize(sDim);

  // Get a model CTF curve at hyper-res and a list of zero indexes
  for (ind = 0; ind < hyperCTF.size(); ind++) {
    oneVal = sFinder->CTFvalue(ind / (hyperDim - 1.), startDef, initialPhase,
                               initialCutOn);
    hyperCTF[ind] = pow(fabs(oneVal), 1.5);
    if (ind > 3 && ((lastVal < 0. && oneVal >= 0.) || (lastVal > 0. && oneVal <= 0.)))
        hyperZeros.push_back(ind);
    lastVal = oneVal;
  }

  for (ind = 0; ind < sDim; ind++)
    rawVal[ind] = sRaw[ind];

  // Correlate with an odd window width equal to first interzero distance
  window = (hyperZeros[1] - hyperZeros[0]) / hyperRes;
  window = 2 * (window / 2) + 1;

  // For each point in fitting range, find nearest zero and correlate with it
  for (ind = sIndex1; ind <= sIndex2; ind++) {
    hyperInd = B3DNINT(ind * (hyperDim - 1.) / (sDim - 1.));
    minDist = 100000;
    for (indc = 0; indc < hyperZeros.size(); indc++) {
      dist = B3DABS(hyperInd - hyperZeros[indc]);
      if (dist < minDist) {
        minDist = dist;
        minZeroNum = indc;
      }
    }

    minInd = hyperZeros[minZeroNum];

    // Reduce the window size if necessary for the raw curve or start of CTF curve
    // then load the CTF values into the array
    ctfVal.clear();
    left = B3DMAX(0, ind - window / 2);
    right = B3DMIN(sDim - 1, ind + window / 2);
    numLeft = B3DMIN(ind - left, minInd / hyperRes);
    numRight = right - ind;
    if (!minZeroNum && numLeft > 3)
      numLeft = B3DMAX(3, window / 6);
    for (indc = minInd - numLeft * hyperRes; indc <= minInd + numRight * hyperRes;
         indc += hyperRes) {
      ctfVal.push_back(hyperCTF[indc]);
    }

    // Correlate
    lsFit(&ctfVal[0], &rawVal[ind - numLeft], numLeft + numRight + 1, &slope, &intcp,
          &ccc[ind]);
    //printf("%.3f  %.5f  %g\n", 0.5*ind / (mDim - 1.), ccc[ind], slope);
  }

  // Find peaks with about the right spacing
  sNumBase = 0;
  interZero = (hyperZeros[1] - hyperZeros[0]) / (hyperDim - 1.);
  zeroFreq = hyperZeros[0] / (hyperDim - 1.);
  for (ind = sIndex1 + 1; ind < sIndex2; ind++) {
    freq = ind / (sDim - 1.);
    if (ccc[ind] >= ccc[ind - 1] && ccc[ind] > ccc[ind + 1] &&
        ((!sNumBase && freq > zeroFreq - 0.25 * interZero) ||
         (sNumBase && freq - sBaseFreq[sNumBase - 1] > 0.75 * interZero))) {
      if (ccc[ind] < 0.25 || (!sNumBase && freq > zeroFreq + 1.25 * interZero) ||
          (sNumBase && freq - sBaseFreq[sNumBase - 1] > 1.25 * interZero)) {
        if (debugLevel > 1)
          printf("  no zero at %3f, ccc=%.4f, interval=%.3f, 1.25*interzero=%.2f",
                 freq / 2., ccc[ind], freq - sBaseFreq[sNumBase - 1], 1.25 * interZero);
        break;
      }

      sBaseFreq[sNumBase] = (ind + parabolicFitPosition(ccc[ind - 1], ccc[ind],
                                                         ccc[ind + 1])) / (sDim - 1.);
      sBaseWgt[sNumBase++] = ccc[ind] * ccc[ind];
      interZero = (hyperZeros[sNumBase] - hyperZeros[sNumBase - 1]) / (hyperDim - 1.);
      if (debugLevel > 1)
        printf("  Zero= %f  weight= %f  inter-zero= %f\n", sBaseFreq[sNumBase - 1] / 2.,
               sBaseWgt[sNumBase - 1], interZero / 2.);
    }
  }
  if (debugLevel > 1)
    printf("Found %d zeros\n", sNumBase);
  return sNumBase;
}

/*
 * Computes the weighted summed squared error from differences between the zeros at the
 * given parameters and the found zeros
 */
double SimplexFitting::evaluateZeroFit(double defocus, double phase, double cutonFreq)
{
  int ind;
  double diff, err = 0.;
  for (ind = 0; ind < sNumBase; ind++) {
    diff = sBaseFreq[ind] - sFinder->getAnyOneZero(defocus, ind + 1, phase, cutonFreq);
    err += sBaseWgt[ind] * diff * diff;
  }
  return err;
}

