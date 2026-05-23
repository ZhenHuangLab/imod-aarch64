/*
 * simplexfitting.h - Header for SimplexFitting class
 *
 *  $Id$
 *
 */
#ifndef CURVEFITTING_H
#define CURVEFITTING_H

#include "b3dutil.h"
#include "defocusfinder.h"
#include <vector>

#define MAX_WEDGES 72

class MyApp;

class SimplexFitting
{
 public:
  //SimplexFitting(double *rawData, int nRaw, int index1, int index2);
  SimplexFitting(int nRaw, MyApp *app);
  ~SimplexFitting();
  static void funk(float *, float *);
  static void funkCTF(float *, float *);
  static void funkBase(float *, float *);
  static void funkAstig(float *, float *);

  int fitGaussian(double* result, double &err, int howToInitParams);
  int fitCTF(int nvar, double initialPhase, double initialCutOn, double fixedFocus, 
             double* fitting, double &err, double &focus, double &phase, double &cuton);
  double evaluateZeroFit(double defocus, double phase, double cutonFreq);
  int findZerosInPS(double startDef, double initialPhase, double initialCutOn);
  void recomputeCTF(double *result, double defocus);
  static double CTFvalueAtIndex(int ind, float *params);
  void setRange(int n1 , int n2);
  setMember(float, DefocusRange);
  void setRaw(double *rawData);
  void printCTFresult(float err);
  void fitBaseline(double *average, double *baseline, int order, bool subtractBase);
  void addBasePoint(int ind, double *average, int &indLast, double &rawMin,
                    double &rawMax);
  void fitWedgesForAstig(std::vector<float> &wedgeAngles, std::vector<float> &fitDefocus,
                         float wedgeRange, float &meanFocus,
                         float &astigmatism, float &astigAngle, float &error);
  double getLastError() {return mLastError;};
  void outputWedgeFitVals();

 private:
  MyApp *mApp;
  double mLastError;
  int mLastNumCTFvars;
  float mDefocusRange;
};

#endif
