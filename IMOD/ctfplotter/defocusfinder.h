/*
 * defocusfinder.h - Header for DefocusFinder class
 *
 *  $Id$
 *
 */
#ifndef DEFOCUSFINDER_H
#define DEFOCUSFINDER_H

class DefocusFinder
{
 public:
  DefocusFinder(int volt, double pSize, double ampContrast, double inputCs, 
                int dim, double expDef);
  int findZero(const double* simplexFitting, const double* linearFitting, 
               int x1, int x2, double* zero);
  int findDefocus(double *focus);
  double defocusFromSecondZero(double zero);
  double getZero(){return mZeroCrossing;}
  getMember(double, ExpZero);
  void setZero( double zero){ mZeroCrossing=zero;}
  getSetMember(double, Defocus);
  getSetMember(double, AvgDefocus);
  getMember(double, ExpDefocus);
  getSetMember(double, PlatePhase);
  getSetMember(double, CutOnFreq);
  getMember(double, AmpRatio);
  getMember(double, Cs);
  setMember(double, PixelSize);
  void setExpDefocus(double expDef);
  void getTwoZeros(double focus, double &firstZero, double &secondZero, 
                   double phase = NO_PHASE_VALUE, double cutOnFreq = NO_PHASE_VALUE);
  double computeOneZero(double focus, int zeroNum, double phase);
  double getAnyOneZero(double focus, int zeroNum, double phase = NO_PHASE_VALUE,
                       double cutOnFreq = NO_PHASE_VALUE);
  int getNumberOfZerosInRange(double focus, double nyquistFrac, 
                              double phase = NO_PHASE_VALUE,
                              double cutOnFreq = NO_PHASE_VALUE);
  double CTFvalue(double freq, double def, double phase = NO_PHASE_VALUE, 
                  double cutOnFreq = NO_PHASE_VALUE);
  double findTolerance(double freqLim, double critShift, double def, 
                       double phase = NO_PHASE_VALUE, double cutOnFreq = NO_PHASE_VALUE);
  void precomputeCTFfactors(double freq, double phase, double cutOnFreq, 
                              double &phiFixed, double &phiVary);
  double CTFvalueFromPhiFactors(double def, double phiFixed, double phiVary);
  double mWavelength;
  double mCsOne;
  double mCsTwo;
  
 private:
  int mVoltage;
  double mPixelSize;
  double mAmpRatio;
  double mCs;
  double mZeroCrossing;
  double mExpZero;
  int mDim;
  double mDefocus;
  double mExpDefocus;
  double mAmpAngle;
  double mAvgDefocus;
  double mPlatePhase;
  double mCutOnFreq;
  double mAmpComplement;
};

#endif
