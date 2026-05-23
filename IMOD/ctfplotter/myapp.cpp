/*
 * myapp.cpp - the main class for ctfplotter
 *
 *  It was originally a QApplication but is now just a QObject so program can run in
 *  autofit mode without making a UI and needing a window system
 *  
 *  Authors: Quanren Xiong, David Mastronarde, John Heumann
 *
 *  Copyright (C) 2018 by the Regents of the University of
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#include "myapp.h"
#include <qlabel.h>
#include <qfile.h>
#include <qtoolbutton.h>
#include <qcursor.h>
#include <qmessagebox.h>

#include <stdio.h>
#include <math.h>
#include <time.h>

#include "plotter.h"
#include "fittingdialog.h"
#include "angledialog.h"
#include "simplexfitting.h"
#include "linearfitting.h"

#include "b3dutil.h"
#include "ilist.h"
#include "mrcfiles.h"
#include "mrcslice.h"
#include "sliceproc.h"
#include "cfft.h"
#include "parse_params.h" //for exitError()

// image writing function for ctffind
static int writeSlice(const char *filename, float *data, int xsize, int ysize)
{
  Islice slice;
  sliceInit(&slice, xsize, ysize, MRC_MODE_FLOAT, data);
	return sliceWriteMRCfile(filename, &slice);
}

/*
 * THE CLASS CONSTRUCTOR
 */
MyApp::MyApp(int volt, double pSize,
             double ampRatio, float cs, char *defFn, int dim, int hyperRes,
             double focusTol, int tSize, double tAxisAngle, double lAngle,
             double hAngle, double expDefocus, double leftTol, double rightTol,
             int maxCacheSize, int invertAngles, bool doFocalPairProcessing,
             double fpdz, int baselineOrder, bool noNoise, int numSectors, 
             float wedgeRange, float wedgeInterval)
    : mDefocusFinder(volt, pSize, ampRatio, cs, dim, expDefocus)
{
  double phaseShift = 0., cutonFreq = 0.;
  int defFlags;
  float sector;
  ctffindSetSliceWriteFunc(writeSlice);
  mSaveModified = false;
  mSaveAndExit = false;
  mPlotter = NULL;
  mNoiseMeans = NULL;
  mNoiseIndexes = NULL;
  mAllNoisePS = NULL;
  mAmpSpecSum = NULL;
  mSavedCopy = NULL;
  mDim = dim;
  mHyperRes = hyperRes;
  mNumSectors = numSectors;
  mDefocusTol = focusTol;
  mTileSize = tSize;
  mRawTileSize = tSize;
  mTiltAxisAngle = tAxisAngle;
  mPixelSize = pSize;
  mCropPixelSizeInDia = pSize;
  mCropSpectra = false;
  mVoltage = volt;
  mLowAngle = lAngle;
  mHighAngle = hAngle;
  mNoNoiseFiles = noNoise;
  mDoTwoLineBaselineFit = noNoise;
  mX1MethodIndex = 1; //0:Linear, 1:Simplex;
  mX2MethodIndex = 1; //0:Linear, 1:Simplex;
  mZeroFitMethod = FIT_CTF_LIKE;
  mVaryCtfPowerInFit = false;
  mPolynomialOrder = 4;
  mBaselineOrder = baselineOrder;
  mUseCurDefocus = 0;     //0 = use expected defocus, 1 = use current
  mInitialCentralTiles = false;
  mLeftDefTol = leftTol;
  mRightDefTol = rightTol;
  mBackedUp = false;
  mFnDefocus = defFn;
  mTileIncluded = NULL;
  mStackMean = -1000.0;
  mStackMean2 = -1000.0;
  mFreqTileCounter = (int *)malloc(mDim * sizeof(int));
  mNumNoiseFiles = 0;
  mNoisePS = (double *)malloc(mDim * sizeof(double));
  mTiltAngles = NULL;
  mSortedAngles = NULL;
  mNoAnglesEntered = false;
  mNumZerosToFit = 2.;
  mAngleSign = invertAngles ? -1. : 1.;
  mFocalPairProcessing = doFocalPairProcessing;
  mAllowCtffind = false;
  mSlowInitialSearch = false;
  mFindAstigmatism = false;
  mFindPhaseShift = false;
  mFindCutOnFreq = false;
  mFindAndFitZeros = true;
  mMinViewsForAstig = 3;
  mMinViewsForPhase = 1;
  mSkipOnlyForAstig = false;
  mExcludingSkipList = true;
  mViewSkipList = NULL;
  mNumViewsToSkip = 0;
  mShowWedgePlots = true;
  mSecToShowWedges = 0.05;
  mAutofitIteration = 0;
  mSettingFromTable = false;
  mNextFitPhaseShift = NO_PHASE_VALUE;
  mLastFitPhaseShift = NO_PHASE_VALUE;
  mLastFitCutOnFreq = NO_PHASE_VALUE;
  mNextFitCutOnFreq = NO_PHASE_VALUE;
  mPhaseSearchRange = 0.66667 * MY_PI;
  mNextFitKnowPhase = false;
  mUseCurrentPhase = false;
  mAstigParamsOpen = false;
  mPhaseParamsOpen = false;

  if (doFocalPairProcessing) {
    mCache = new SliceCache(maxCacheSize / 2, this);
    mCache2 = new SliceCache(maxCacheSize / 2, this);
    mDefocusOffset = fpdz;
    mFreqTileCounter2 = (int *)malloc(mDim * sizeof(double));
    if (!mCache || !mCache2 || !mFreqTileCounter2)
      exitError("Allocation failed: out of memory!");
  }
  else {
    mCache = new SliceCache(maxCacheSize, this);
    mCache2 = NULL;
    mDefocusOffset = 0;
    mFreqTileCounter2 = NULL;
    if (!mCache)
      exitError("Allocation failed: out of memory!");
  }

  // read in existing defocus data, check for astigmatism
  mDefVersionIn = 0;
  mSaved = readDefocusFile(mFnDefocus, mDefVersionIn, defFlags);

  // Look for phase shift entries, use mean value in defocus module
  if (defFlags & DEF_FILE_HAS_PHASE) {
    for (int ind = 0; ind < ilistSize(mSaved); ind++) {
      SavedDefocus *saved = (SavedDefocus *)ilistItem(mSaved, ind);
      phaseShift += saved->platePhase / ilistSize(mSaved);
      cutonFreq += saved->cutOnFreq / ilistSize(mSaved);
    }
    mDefocusFinder.setPlatePhase(phaseShift);
    mDefocusFinder.setCutOnFreq(cutonFreq);
  }
  mDefVersionOut = 2;
  mFitSingleViews = false;
  mPlotSetInitialized[0] = mPlotSetInitialized[1] = false;
  mSwitchedZoomStacks = false;
  mNextSpecAstigmatism = 0.;
  mNextSpecAstigAngle = 0.;
  mNextSpecEffectiveDefocus = 0.;
  mWedgeCenterAngle = 0;
  mNextFitStartDefocus = 0.;
  mUpdatingFitRange = false;
  sector = getSectorWidth();
  if (wedgeInterval > 0) {
    mWedgeInterval = multipleOfSectorWidth(wedgeInterval);
    if (!wedgeInterval)
      exitError("The wedge interval must be at least half as big as the sector width "
                "of %.1 deg)", sector);
    if (mWedgeInterval > multipleOfSectorWidth(30.))
      exitError("The wedge interval must be no more than %.1f", 
                (0.4 + B3DNINT(30. / sector)) * sector);
  } else {
    mWedgeInterval = sector;
  }
  mWedgeAngleRange = B3DMAX(sector, multipleOfSectorWidth(wedgeRange));
  mNextSpecWedgeRange = 0.;
  mNextSpecWedgeNum = -1;
  mDoingAutofit = false;
  mLastWedgeFitError = -1.;
  mAllViewsAstigmatism = 0.;
  mBreakAtBidirView = false;
  mBidirectionalView = 0;
}

MyApp::~MyApp()
{
  delete mSimplexEngine;
  delete mLinearEngine;
  delete mCache;
  if (mCache2)
    delete(mCache2);
  B3DFREE(mTiltAngles);
  B3DFREE(mSortedAngles);
  ilistDelete(mSaved);
  B3DFREE(mFreqTileCounter);
  B3DFREE(mFreqTileCounter2);
  B3DFREE(mNoisePS);
  B3DFREE(mTileIncluded);
  B3DFREE(mAmpSpecSum);
  //if(mSaveFp) fclose(mSaveFp);
  //for(int k=0;k<MAXSLICENUM;k++) if(slice[k]) sliceFree(slice[k]);
}

//save all PS in text file for plotting in Matlab
//when being called in writeDefocusFile
//The calling of it is commented out in release version.
void MyApp::saveAllPs()
{
  char fnInitPs[40];
  char fnBackPs[40];
  char fnSubtracted[40];
  char fnDim[40];

  sprintf(fnInitPs, "%s.init", mFnDefocus);
  sprintf(fnBackPs, "%s.floor", mFnDefocus);
  sprintf(fnSubtracted, "%s.final", mFnDefocus);
  sprintf(fnDim, "%s.dim", mFnDefocus);

  FILE *fpInit = fopen(fnInitPs, "w");
  FILE *fpFloor = fopen(fnBackPs, "w");
  FILE *fpFinal = fopen(fnSubtracted, "w");
  FILE *fpDim = fopen(fnDim, "w");

  int ii;
  double *ps = (double *)malloc(mDim * sizeof(double));
  setNoiseForMean(mStackMean);
  PlotSettings settings = mPlotter->mZoomStack[0][mPlotter->mCurZoom[0]];

  for (ii = 0; ii < mDim; ii++) {
    if (settings.minX > ii / 100.0 || settings.maxX < ii / 100.0)
      continue;
    fprintf(fpDim, "%7.4f\n", ii / 100.0);
    fprintf(fpFinal, "%7.4f\n", log(mRAverage[ii])) ;
    fprintf(fpFloor, "%7.4f\n", log(mNoisePS[ii]));
    ps[ii] = mRAverage[ii] * mNoisePS[ii];
    fprintf(fpInit, "%7.4f\n", log(ps[ii]));
  }

  fclose(fpInit);
  fclose(fpFloor);
  fclose(fpFinal);
  fclose(fpDim);
  free(ps);
}

/*
 * Do baseline fitting to the PS stored in mRAverage and plot it, then call fitPsFindZero
 * to find the defocus and update display
 */
void MyApp::plotFitPS(bool flagSetInitSetting)
{
  ensureFitRangeUpdated();
  if (mZeroFitMethod == FIT_USE_CTFFIND) {
    fitPsFindZero();
    return;
  }
  double *ps = (double *)malloc(mDim * sizeof(double));
  double *baseline = (double *)malloc(mDim * sizeof(double));
  QVector<QPointF> data;
  int ii;
  double freq, scale, nonZeroMin = 1.e36;

  // Dividing by the Noise PS happened earlier

  PlotSettings plotSetting = PlotSettings();
  double initialMaxY = -100;
  double initialSecondY = -100;
  double initialThirdY = -100;
  double initialFourthY = -100;
  double initialMinY = 100;
  bool doTwoLine = mDoTwoLineBaselineFit || mNoNoiseFiles;
  
  scale = B3DMAX(mRawTileSize, mTileSize) / (double)mTileSize;
  scale *= scale;

  // Find minimum of non-zero bins and adjust the others up to the minimum to avoid log(0)
  for (ii = 0; ii < mDim; ii++) {
    if (mRAverage[ii] > 0) {
      ACCUM_MIN(nonZeroMin, mRAverage[ii]);
    }
  }
  for (ii = 0; ii < mDim; ii++) {
    ps[ii] = log(scale * B3DMAX(mRAverage[ii], nonZeroMin));
  }

  if (mZeroFitMethod == FIT_CTF_LIKE)
    mSimplexEngine->setRange(mX1Index1, mX2Index2);

  // Do the two-line fit if requested, then do a regular baseline or 
  if (doTwoLine)
    findBaselinePastBreak(ps, baseline);
  mSimplexEngine->fitBaseline(ps, baseline, mBaselineOrder, doTwoLine);
  if (fabs(baseline[2] - baseline[3]) * (mDim - 1) > 0.25)
    mSimplexEngine->fitBaseline(ps, baseline, mBaselineOrder, true);
  /*if (fabs(baseline[2] - baseline[3]) > 
      2. * fabs(baseline[mDim - 3] - baseline[mDim - 4])) {
    if (doTwoLine)
      findBaselinePastBreak(ps, baseline);
    mSimplexEngine->fitBaseline(ps, baseline, 1, doTwoLine);
    mSimplexEngine->fitBaseline(ps, baseline, mBaselineOrder, true);
    }*/
  for (ii = 0; ii < mDim; ii++) {
    freq = ii / (float)(mDim - 1);
    ps[ii] -= baseline[ii];
    data.append(QPointF(freq, ps[ii]));
    if (ps[ii] > initialMaxY) {
      initialFourthY = initialThirdY;
      initialThirdY = initialSecondY;
      initialSecondY = initialMaxY;
      initialMaxY = ps[ii];
    } else if (ps[ii] > initialSecondY) {
      initialFourthY = initialThirdY;
      initialThirdY = initialSecondY;
      initialSecondY = ps[ii];
    } else if (ps[ii] > initialThirdY) {
      initialFourthY = initialThirdY;
      initialThirdY = ps[ii];
    } else if (ps[ii] > initialFourthY)
      initialFourthY = ps[ii];
    if (ps[ii] < initialMinY)
      initialMinY = ps[ii];
  }
  initialMaxY = ceil(5. * (0.25 * initialThirdY + 0.75 * initialFourthY)) / 5.;
  plotSetting.maxY = initialMaxY;
  plotSetting.minY = floor(5. *(initialMinY - 0.05 * (initialMaxY - initialMinY))) / 5.;

  //adjust initial plot setting to data if needed;
  if ((flagSetInitSetting || !mPlotSetInitialized[0]) && mPlotter) {
    mPlotter->setPlotSettings(plotSetting);
    mPlotSetInitialized[0] = true;
  }
  if (mPlotter && (mNextSpecWedgeNum < 0 || mShowWedgePlots))
    mPlotter->setCurveData(0, data);

  mSimplexEngine->setRaw(&ps[0]);
  mLinearEngine->setRaw(&ps[0]);
  fitPsFindZero();
  free(ps);
  free(baseline);
}

/*
 * Fits the power spectrum by the chosen method and finds the zero
 */
void MyApp::fitPsFindZero()
{
  QVector<QPointF> data, data1, data2;
  double *resLeftFit = (double *)malloc(mDim * sizeof(double));
  double *resRightFit = (double *)malloc(mDim * sizeof(double));
  double inc = 1.0 / (mDim - 1);
  double zero, defocus, err, startDef, fallbackCuton;
  double initialPhase = NO_PHASE_VALUE, phase = NO_PHASE_VALUE, cuton = NO_PHASE_VALUE; 
  float curValue, initialPhaseStep, curPhase, nextPhase, phaseBrack[16];
  float initialCutonStep, curCuton, nextCuton, cutonBrack[16];
  int numPhaseScanSteps[2] = {16, 10}, numPhaseCutsDone;
  int numCutonScanSteps[2] = {16, 10}, numCutonCutsDone;
  int maxPhaseCuts[2] = {8, 6}, maxCutonCuts[2] = {8, 6};
  int numFocusCutsDone, phaseFitLoop = -1, numZeros, focusErr, fallbackLoop;
  float curFocus, nextFocus, initialFocusStep, focusBrack[16];
  double bestPhase, bestCuton, bestFocus, bestFocForPhase, minForPhase, minErr = 1.e20;
  bool sawMin = false;
  bool knowPhase, findCuton;
  CtffindParams params;
  float resultsArray[7], lastBinFreq;
  PlotSettings plotSetting = PlotSettings();
  float minY = 1.e20, maxY = -1.e20;
  float *rotationalAvg = NULL, *normalizedAvg = NULL, *fitCurve = NULL;
  int label2Flags = 0;
  double ctfFitErr;
  QString str;

  double model[7] = {0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
  int ii, order, numPoints, numPlot, error = 0, error2 = 0;

  // Revert phase and cuton to global value if not finding them or setting from table
  if (!mFindPhaseShift && !mSettingFromTable)
    mLastFitPhaseShift = NO_PHASE_VALUE;
  if ((!mFindPhaseShift || !mFindCutOnFreq) && !mSettingFromTable)
    mLastFitCutOnFreq = NO_PHASE_VALUE;

  switch (mZeroFitMethod) {
    /* 
     * CTF like curve
     */
  case FIT_CTF_LIKE:
    mSimplexEngine->setRange(mX1Index1, mX2Index2);
    order = mVaryCtfPowerInFit ? 5 : 4;
    startDef = getStartingDefocusAndZeros(zero, err);
    if (inc * mX2Index2 >= zero + 1.5 * (err - zero) && (mX2Index2 + 1 - mX1Index1) > 
        2 * (order + 2))
      order += 2;

    /*
     * Finding phase and cuton with brute-force searches
     */
    phase = NO_PHASE_VALUE;
    if (mFindPhaseShift && !mNextFitKnowPhase && mNextSpecWedgeNum < 0 &&
        !mSettingFromTable && (!mAutofitIteration || mFindAndFitZeros)) {

      // Loop twice if finding cuton: if it fails the first time, solve for phase
      // with a last or expected or nearby cuton value
      findCuton = mFindCutOnFreq;
      for (fallbackLoop = 0; fallbackLoop < (mFindCutOnFreq ? 2 : 1); fallbackLoop++) {
        if (fallbackLoop) {
          
          // In the fallback loop, set up the fallback cuton value
          fallbackCuton = mUseCurrentPhase ? mLastFitCutOnFreq : NO_PHASE_VALUE;
          if (fallbackCuton < PHASE_TEST && mDefocusFinder.getCutOnFreq() == 0.) {
            fallbackCuton = nearbyAverageCutonFreq(5);
          }
          findCuton = false;
          if (fallbackCuton < PHASE_TEST)
            fallbackCuton = mDefocusFinder.getCutOnFreq();
          if (debugLevel)
            printf("Searching for phase only with fallback cuton=%f\n", fallbackCuton <
                   PHASE_TEST ? mDefocusFinder.getCutOnFreq() : fallbackCuton);
        }

        // Loop twice if necessary, first to find and fit zeros, then to fit spectrum
        phaseFitLoop = mFindAndFitZeros ? 0 : 1;
        for (; phaseFitLoop < 2; phaseFitLoop++) {

          // Initially find the zeros in the PS
          if (!phaseFitLoop) {
            curCuton = mUseCurrentPhase ? mLastFitCutOnFreq : NO_PHASE_VALUE;
            if (fallbackLoop)
              curCuton = fallbackCuton;
            numZeros = mSimplexEngine->findZerosInPS
              (startDef, mUseCurrentPhase ? mLastFitPhaseShift : NO_PHASE_VALUE,curCuton);
            if (numZeros < 3 || (findCuton && numZeros < 5)) {
              if (debugLevel)
                printf("Found %d zeros, need at least %d to fit to zeros\n", numZeros,
                       mFindCutOnFreq ? 5 : 3);
              continue;
            }
          }

          // Set up search
          initialCutonStep = mMaxCutOnFreq / numCutonScanSteps[phaseFitLoop];
          initialPhaseStep = mPhaseSearchRange / numPhaseScanSteps[phaseFitLoop];
          sawMin = false;
          error = 0;
          minErr = 1.e20;

          // start search at 0 if searching or use expected cuton if not, or fallback 
          curCuton = mFindCutOnFreq ? 0. : NO_PHASE_VALUE;
          if (fallbackLoop)
            curCuton = fallbackCuton;
          numCutonCutsDone = -1;

          // Loop on cut-on search: set up phase search
          while (!error) {
            curPhase = mDefocusFinder.getPlatePhase() - mPhaseSearchRange / 2.;
            numPhaseCutsDone = -1;
            minForPhase = 1.e20;
            bestFocForPhase = startDef;

            // Loop on phase search: find defocus either way
            while (!error) {
              if (phaseFitLoop) {

                // Do the fit to the PS
                debugLevel -= 2;
                if ((focusErr = mSimplexEngine->fitCTF(order, curPhase, curCuton, 0.,
                                                       resLeftFit, err, defocus, phase,
                                                       cuton)))
                  printf("simplexEngine error\n");
                else
                  curFocus = defocus;
                debugLevel += 2;
              } else {

                // Or search focus to fit the zeros
                curFocus = startDef;
                initialFocusStep = 0.1;
                numFocusCutsDone = -1;
                while (1) {
                  curValue = err = mSimplexEngine->evaluateZeroFit(curFocus, curPhase,
                                                                   curCuton);
                  focusErr = minimize1D(curFocus, curValue, initialFocusStep, 0,
                                        &numFocusCutsDone, focusBrack, &nextFocus);
                  if (nextFocus < 0.3 || nextFocus < startDef - 2. || 
                      nextFocus > startDef + 3.)
                    focusErr = 1;
                  if (focusErr || numFocusCutsDone > 7)
                    break;
                  curFocus = nextFocus;
                } // end of focus search
              }
              if (focusErr) {
                error = focusErr;
                break;
              }

              // Save focus at best phase seen
              if (err < minForPhase) {
                minForPhase = err;
                bestFocForPhase = curFocus;
              }

              //printf("Cuton= %.3f   phase=%.1f  focus=%7.3f  error=%.9f\n", curCuton, 
              //     curPhase / RADIANS_PER_DEGREE, curFocus, err);
              curValue = err;
              error = minimize1D(curPhase, curValue, initialPhaseStep, 
                                 numPhaseScanSteps[phaseFitLoop],
                                 &numPhaseCutsDone, phaseBrack, &nextPhase);
              if (error || numPhaseCutsDone > maxPhaseCuts[phaseFitLoop])
                break;
              curPhase = nextPhase;
            }  // End of phase loop
            if (debugLevel > 1)
              printf("Cuton= %.3f   best phase=%6.1f  focus=%7.3f  error=%.9f\n",curCuton,
                     phaseBrack[1] / RADIANS_PER_DEGREE, bestFocForPhase, phaseBrack[8]);
          
            // If we have a new minimum from this phase search, record the phase and cuton
            // But now we have no longer seen a minimum that is bracketed for cutons if
            // still in initial walk for cutons, so clear flag on that case
            if (!error && phaseBrack[8] < minErr) {
              minErr = phaseBrack[8];
              bestPhase = phaseBrack[1];
              bestFocus = bestFocForPhase;
              if (numCutonCutsDone <= 0)
                sawMin = false;
            }
          
            // If there was no error and the current search gave a higher result than the
            // min, then we have seen a minimum in a search.  Or if there was an error and
            // it is high enough this time, that also validates the minimum.
            // Errors in a phase search after this can be ignored
            if ((!error && phaseBrack[8] / minErr > 1.01) ||
                (!focusErr && phaseBrack[8] / minErr > 1.1))
              sawMin = true;
            if (error && !sawMin)
              break;
          
            // Go to next cuton, or break out after one go.
            if (findCuton) {
              curValue = error ? 2. * minErr : phaseBrack[8];
              error = minimize1D(curCuton, curValue, initialCutonStep, 
                                 numCutonScanSteps[phaseFitLoop],
                                 &numCutonCutsDone, cutonBrack, &nextCuton);
              if (error || numCutonCutsDone > maxCutonCuts[phaseFitLoop])
                break;
              curCuton = nextCuton;
            } else
              break;
          } // End of cuton loop

          str = "spectrum";
          if (!phaseFitLoop)
            str.sprintf("%d zeros", numZeros);
        
            // Success
          if (sawMin || (!findCuton && !error)) {
            bestCuton = findCuton ? cutonBrack[1] : curCuton;
            if (debugLevel) {
              printf("Fitting to %s gave best phase= %.1f", (const char *)str.toLatin1(),
                       bestPhase / RADIANS_PER_DEGREE);
              if (findCuton)
                printf("  cuton= %.3f", bestCuton);
              printf("\n");
            }

            // Do a "refinement" fit to either find better defocus or just get a curve 
            // that fits the best at the fixed focus
            error = mSimplexEngine->fitCTF(order, bestPhase, bestCuton,
                                           phaseFitLoop ? 0. : bestFocus, resLeftFit, err,
                                           defocus, phase, cuton);

            // Then if finding from PS fit, do focus and phase only refinement
            if (!error && phaseFitLoop) {
              error = mSimplexEngine->fitCTF(2, bestPhase, bestCuton, 0., resLeftFit, err,
                                             defocus, phase, cuton);
            } else if (!error)
              phase = bestPhase;
            
            if (!error) {
              mLastFitPhaseShift = phase;
              if (mFindCutOnFreq) {
                cuton = bestCuton;
                mLastFitCutOnFreq = cuton;
              }
              break;
            }
          } else if (debugLevel) {
            error = 1;
            printf("Fitting to %s failed to find phase%s\n", (const char *)str.toLatin1(),
                   findCuton ? " and cuton" : "");
          }
        }  // End of loop on zeros and spectrum
        if (!error)
          break;
      }  // End of loop on regular or phase-only search with fallback cuton
    } else  // No phase: set flag to do regular fit
      error = 1;

    /*
     * Regular fit: do it if there is a phase failure, or normally
     */
    if (error) {
    
      // To experiment with restricted fitting when there is one slice
      //if (mNumSlicesDone == 1)
      //order = 2;
      if ((error = mSimplexEngine->fitCTF
           (order, mNextFitPhaseShift, mNextFitCutOnFreq,
            mSettingFromTable ? mDefocusFinder.getDefocus() : 0., resLeftFit, err,
            defocus, phase, cuton))) {
        printf("simplexEngine error\n");
      }

      // On iterations with finding phase, follow that with refinement of focus/phase as
      // long as phase is being found from same spectrum as defocus
      if (!error && mAutofitIteration && mFindPhaseShift && !mFindAndFitZeros &&
          mMinViewsForPhase <= mNumViewsInRange) {
        error = mSimplexEngine->fitCTF(2, mNextFitPhaseShift, mNextFitCutOnFreq,
                                       0., resLeftFit, err, defocus, phase, cuton);
        if (!error) 
          mLastFitPhaseShift = phase;
      }
    }

    if (!error) {

      // If no error, save and set up to display the results
      mDefocusFinder.getTwoZeros(defocus, zero, err, phase, cuton);
      mDefocusFinder.setZero(zero);
      mDefocusFinder.setDefocus(defocus);
      ctfFitErr = mSimplexEngine->getLastError();;
      label2Flags = LABEL2_ERROR;
      if (mFindPhaseShift && !phaseFitLoop) {
        ctfFitErr = sqrt(minErr /numZeros) / 2.;
        label2Flags |= LABEL2_ZFERR;
      }
      if ((mFindAstigmatism && !mSettingFromTable) || 
          (mSettingFromTable && mCurrentAstigmatism)) {
        label2Flags |= (mNextSpecWedgeNum < 0 ? LABEL2_ASTIG : LABEL2_CEN_ANGLE);
        if (mLastWedgeFitError >= 0 && !mSettingFromTable)
          label2Flags |= LABEL2_WEDGE;
      }
      if (phaseFitLoop > 1) {
        label2Flags |= LABEL2_PH_FAIL;
        if (mFindCutOnFreq)
          label2Flags |= LABEL2_CO_FAIL;
      }
      if (((mFindPhaseShift && !mSettingFromTable) || 
           (mSettingFromTable && mLastFitPhaseShift > PHASE_TEST &&
            fabs(mLastFitPhaseShift - mDefocusFinder.getPlatePhase()) > 0.0001)) && 
          phaseFitLoop < 2 && mNextSpecWedgeNum < 0) {
        label2Flags |= LABEL2_PHASE;
        if ((mFindCutOnFreq && !mSettingFromTable) || 
            (mSettingFromTable && mLastFitCutOnFreq > PHASE_TEST &&
             fabs(mLastFitCutOnFreq - mDefocusFinder.getCutOnFreq()) > 0.0001)) {
          label2Flags |= LABEL2_CUTON;
          if (!mSettingFromTable && !findCuton)
            label2Flags |= LABEL2_FALLBACK;
        }
      }
    }
    if (mPlotter && (mNextSpecWedgeNum < 0 || mShowWedgePlots))
      mPlotter->clearCurve(2);
    break;

    /*
     * CTFFIND FITTING
     */
  case FIT_USE_CTFFIND:
    if (debugLevel)
      printf("Doing ctffind for iter %d  astigmatism %s known %s %s %s\n", 
             mAutofitIteration, mNextFitKnowAstig ? "":"NOT", 
             mFindPhaseShift ? " phase" : "",
             mNextFitKnowPhase || !mFindPhaseShift ? "" : "NOT",
             mFindPhaseShift ? "know" : "");
    initialPhase = mDefocusFinder.getPlatePhase();
    knowPhase = !mFindPhaseShift;
    if (mNextFitKnowPhase && mNextFitPhaseShift > PHASE_TEST) {
      initialPhase = mNextFitPhaseShift;
      knowPhase = true;
    }
    setupCtffindParams(params, !mFindAstigmatism || mNextFitKnowAstig,
                       mNextFitKnowAstig ? mNextFitAstigmatism : 0., 
                       mNextFitKnowAstig ? mNextFitAstigAngle : 0., knowPhase,
                       initialPhase);
      
    //imodBackupFile("spectrum.mrc");
    //writeSlice("spectrum.mrc", mAmpSpecSum, mTileSize + 2, mTileSize);
    //printf("pix %g volt %d idx1 %d idx2 %d\n", mPixelSize, mVoltage, mX1Index1, 
    //mX2Index2);
    /*printf("dmm %g %g rmm %g %g v %d cs %g ac %g\n", params.minimum_defocus, 
      params.maximum_defocus, params.minimum_resolution, params.maximum_resolution, 
      mVoltage, params.spherical_aberration, params.amplitude_contrast); */
    if ((!ctffind(params, mAmpSpecSum, params.box_size + 2, resultsArray, 
                  &rotationalAvg, &normalizedAvg, &fitCurve, numPoints,
                  lastBinFreq)) != 0) {
      printf("Error in ctffind\n");
    } else {

      // Process results and set up the curves to display
      inc = lastBinFreq / (numPoints - 1);
      numPlot = B3DMIN(numPoints, 0.5 / inc);
      inc *= 2.;
      for (ii = 0; ii < numPlot; ii++) {
        if (ii >= 4 * mX1Index1 / 5) {
          ACCUM_MIN(minY, rotationalAvg[ii]);
          ACCUM_MIN(minY, normalizedAvg[ii]);
          ACCUM_MIN(minY, fitCurve[ii]);
          ACCUM_MAX(maxY, rotationalAvg[ii]);
          ACCUM_MAX(maxY, normalizedAvg[ii]);
          ACCUM_MAX(maxY, fitCurve[ii]);
        }
        data.append(QPointF(ii * inc, rotationalAvg[ii]));
        data1.append(QPointF(ii * inc, normalizedAvg[ii]));
        data2.append(QPointF(ii * inc, fitCurve[ii]));
      }
    
      if (!mPlotSetInitialized[1] && mPlotter) {
        plotSetting.maxY = 3.5; //maxY + 0.1 * (maxY - minY);
        plotSetting.minY = -1.5;//minY - 0.1 * (maxY - minY);
        mPlotter->setPlotSettings(plotSetting);
        mPlotSetInitialized[1] = true;
      }
      if (mPlotter) {
        mPlotter->setCurveData(0, data);
        mPlotter->setCurveData(2, data1);
        mPlotter->setCurveData(1, data2);
      }
 
      defocus = 0.5 * (resultsArray[0] + resultsArray[1]) / 10000.;
      mDefocusFinder.getTwoZeros(defocus, zero, err);
      mDefocusFinder.setZero(zero);
      mDefocusFinder.setDefocus(defocus);
      mCurrentAstigmatism = (resultsArray[0] - resultsArray[1]) / 1.e4;
      mCurrentAstigAngle = resultsArray[2];
      printf("Ctffind f1=%.3f  f2=%.3f avg=%.3f  astig=%.3f  ang=%.1f  score=%.4f  "
             "fit to %.1f  alias %.1f", resultsArray[0] / 1.e4, resultsArray[1] / 1.e4,
             defocus, mCurrentAstigmatism, resultsArray[2], 
             resultsArray[4], resultsArray[5], resultsArray[6]);
      if (!knowPhase) {
        mLastFitPhaseShift = resultsArray[3];
        printf("  phase=%.4f", mLastFitPhaseShift);
      }
      printf("\n");
      ctfFitErr = resultsArray[4];
      label2Flags = LABEL2_SCORE;
      if (mFindAstigmatism)
        label2Flags |= LABEL2_ASTIG;
      if (mFindPhaseShift && mNextSpecWedgeNum < 0)
        label2Flags |= LABEL2_PHASE;
    }
    free(rotationalAvg);
    free(normalizedAvg);
    free(fitCurve);
    break;

    /*
     * POLYNOMIAL FIT
     */
  case FIT_POLYNOMIAL:
    order = B3DMIN(mPolynomialOrder, mX2Index2 - mX1Index1);
    if ((error = mLinearEngine->computeFitting(resLeftFit, model, order + 1,
                                               mX1Index1, mX2Index2, zero)))
      printf("linearEngine error\n");
    else {
      mDefocusFinder.setZero(zero);
      mDefocusFinder.findDefocus(&defocus);
    }
    if (mPlotter)
      mPlotter->clearCurve(2);
    break;

    /*
     * INTERSECTION OF TWO CURVES
     */
  case FIT_TWO_CURVES:
    switch (mX1MethodIndex) {
    case 0: // Line
      if ((error = mLinearEngine->computeFitting(resLeftFit, model, 2,
                                                 mX1Index1, mX1Index2, zero)))
        printf("linearEngine error\n");
      break;
    case 1: // Gaussian
      mSimplexEngine->setRange(mX1Index1, mX1Index2);
      if ((error = mSimplexEngine->fitGaussian(resLeftFit, err, 0)))
        printf("simplexEngine error\n");
      break;
    default:
      error = 1;
      printf("unknown fitting method chosen\n");
    }

    switch (mX2MethodIndex) {
    case 0: // Line
      if ((error2 = mLinearEngine->computeFitting(resRightFit, model, 2,
                                                  mX2Index1, mX2Index2, zero)))
        printf("linearEngine error\n");
      break;
    case 1: // Gaussian
      mSimplexEngine->setRange(mX2Index1, mX2Index2);
      if ((error2 = mSimplexEngine->fitGaussian(resRightFit, err, 1)))
        printf("simplexEngine error\n");
      break;
    default:
      error2 = 1;
      printf("unknown fitting method chosen\n");
    }
    if (!error2) {
      for (ii = 0; ii < mDim; ii++)
        data1.append(QPointF(ii * inc, resRightFit[ii]));
      if (mPlotter)
        mPlotter->setCurveData(2, data1);
    }
    break;

  default:
    break;
  }

  // Use up the "next fit" items
  mNextFitPhaseShift = NO_PHASE_VALUE;
  mNextFitKnowPhase = false;
  mNextFitCutOnFreq = NO_PHASE_VALUE;

  // Send data to plotter
  if (!error && mZeroFitMethod != FIT_USE_CTFFIND) {
    for (ii = 0; ii < mDim; ii++)
      data.append(QPointF(ii * inc, resLeftFit[ii]));
    if (mPlotter && (mNextSpecWedgeNum < 0 || mShowWedgePlots))
      mPlotter->setCurveData(1, data);
  }

  // calculate defocus from the two curves and update display;
  if (!(error + error2) && mZeroFitMethod == FIT_TWO_CURVES) {
    error = mDefocusFinder.findZero(resLeftFit, resRightFit, mX1Index1,
                                    (mX2Index1 + mX2Index2) / 2, &zero);
    if (!error)
      mDefocusFinder.findDefocus(&defocus);
  }
  if (error + error2)
    mDefocusFinder.setDefocus(-2000.0);

  // Manage the labels
  if (mPlotter && (mNextSpecWedgeNum < 0 || mShowWedgePlots)) {
    mPlotter->manageLabels(zero, defocus, 0., 0., error);
    mPlotter->manageLabels2
      (mCurrentAstigmatism, mCurrentAstigAngle, ctfFitErr, mLastWedgeFitError,
       mLastFitPhaseShift, mLastFitCutOnFreq < PHASE_TEST ? 
       mDefocusFinder.getCutOnFreq() : mLastFitCutOnFreq, label2Flags);
  }
  mDefocusFinder.setAvgDefocus(-1.);
  free(resLeftFit);
  free(resRightFit);
  fflush(stdout);
}

/*
 * Compute a fallback cuton frequency by trying to average any nearby ones
 */
double MyApp::nearbyAverageCutonFreq(int numVals)
{
  SavedDefocus *item;
  int ind, lowNear, numAvg;
  float midAngle;
  double meanCuton = 0.;
  std::vector<float> angleDiffs, cutons;
  std::vector<int> sortInd;
  midAngle = getMidAngleAndRangeIndices(mLowAngle, mHighAngle, lowNear, numAvg, ind);
  for (ind = 0; ind < ilistSize(mSaved); ind++) {
    item = (SavedDefocus *)ilistItem(mSaved, ind);
    if (item->platePhase != 0. && item->cutOnFreq != 0.) {
      sortInd.push_back(cutons.size());
      cutons.push_back(item->cutOnFreq);
      angleDiffs.push_back(fabs(midAngle - 0.5 * (item->lAngle + item->hAngle)));
    }
  }
  if (!cutons.size())
    return NO_PHASE_VALUE;
  if (cutons.size() > numVals)
    rsSortIndexedFloats(&angleDiffs[0], &sortInd[0], cutons.size());
  numAvg = B3DMIN(numVals, cutons.size());
  for (ind = 0; ind < numAvg; ind++)
    meanCuton += cutons[sortInd[ind]] / numAvg;
  return meanCuton;
}

/*
 * Set all the ctffind parameters
 */
void MyApp::setupCtffindParams(CtffindParams &params, bool astigKnown, float astigmatism,
                               float astigAngle, bool phaseKnown, float phaseShift)
{
  double zero, err, startDef;
  double platePhase = mDefocusFinder.getPlatePhase();
  float phaseDiff = phaseKnown ? 0. : mPhaseSearchRange / 2.;
  params.acceleration_voltage = mVoltage;
  params.spherical_aberration = mDefocusFinder.getCs();
  params.amplitude_contrast = mDefocusFinder.getAmpRatio();
  params.box_size = mTileSize;
  params.pixel_size_of_input_image = 10. * mPixelSize * mRawTileSize / mTileSize;
  params.minimum_resolution = params.pixel_size_of_input_image / 
    (0.5 * mX1Index1/ mDim);
  params.maximum_resolution = params.pixel_size_of_input_image / 
    (0.5 * mX2Index2 / mDim);
  startDef = 10000. * getStartingDefocusAndZeros(zero, err);
  params.minimum_defocus = B3DMAX(5000., startDef - 20000.);
  params.maximum_defocus = startDef + 20000.;
  params.defocus_search_step = 500.;
  params.astigmatism_tolerance = -100.;
  params.known_astigmatism = astigmatism;
  params.known_astigmatism_angle = astigAngle;
  params.astigmatism_is_known = astigKnown;
  params.slower_search = mSlowInitialSearch;
  params.find_additional_phase_shift = phaseShift > PHASE_TEST || platePhase != 0.;
  if (phaseShift < PHASE_TEST) {
    params.minimum_additional_phase_shift = platePhase;
    params.maximum_additional_phase_shift = platePhase;
  } else {
    params.minimum_additional_phase_shift = phaseShift - phaseDiff;
    params.maximum_additional_phase_shift = phaseShift + phaseDiff;
  }    
  params.additional_phase_shift_search_step = B3DMAX(.1, mPhaseSearchRange / 10.);
  params.compute_extra_stats = true;  
}

/*
 * Take care of whatever has to happen when switching between Ctffind and other methods
 * Returns true if the switch occurred (new PS needed)
 */
bool MyApp::switchToOrFromCtffind()
{
  mSwitchedZoomStacks = true;
  if (mZeroFitMethod == FIT_USE_CTFFIND && !mAmpSpecSum) {
    mCache->clearAndSetSize(mDim, -1, mTileSize, mRawTileSize, false);
    mAmpSpecSum = B3DMALLOC(float, mTileSize * (mTileSize + 2));
    if (!mAmpSpecSum)
      exitError("Error allocating memory for amplitude spectrum");
    mPlotter->mCurStack = 1;

  } else if (mZeroFitMethod != FIT_USE_CTFFIND && mAmpSpecSum) {
    mCache->clearAndSetSize(mDim, mHyperRes, mTileSize, mRawTileSize, false);
    B3DFREE(mAmpSpecSum);
    mPlotter->mCurStack = 0;
  } else
    mSwitchedZoomStacks = false;
  return mSwitchedZoomStacks;
}

/*
 * Zero fit method selection changed 
 */
void MyApp::setZeroFitMethod(int which) 
{
  mZeroFitMethod = which;
  if (switchToOrFromCtffind()) {
    if (adjustCachesForTileParams())
      return;
    if (multipleSpectraAndFits())
      return;
    doComputePS(mInitialCentralTiles, false, false);
  }
  plotFitPS(false);
}

/*
 * Functions for responding to changes in fitting dialog, and recomputing or replotting
 * as appropriate
 */
void MyApp::setBaselineOrder(int order, bool initialize)
{
  if (debugLevel >= 1)
    printf("Baseline fitting order set to %d\n", order);
  mBaselineOrder = order;
  if (initialize)
    return;
  mAllViewsAstigmatism = 0.;
  if ((mFindAstigmatism || (mFindPhaseShift && mMinViewsForPhase > mNumViewsInRange)) &&
      multipleSpectraAndFits())
    return;
  plotFitPS(false);
}

void MyApp::setDoTwoLineBaselineFit(bool state, bool replot)
{
  mDoTwoLineBaselineFit = state;
  mAllViewsAstigmatism = 0.;
  if (replot) {
    if ((mFindAstigmatism || (mFindPhaseShift && mMinViewsForPhase > mNumViewsInRange)) &&
        multipleSpectraAndFits())
      return;
    plotFitPS(false);
  }
}

void MyApp::setNoNoiseFiles(bool state)
{
  mNoNoiseFiles = state;
  setFindAstigmatism(mFindAstigmatism, false);
}

void MyApp::setFindAstigmatism(bool state, bool doingOtherAction)
{
  mFindAstigmatism = state;
  if (doingOtherAction || adjustCachesForTileParams())
    return;
  if (multipleSpectraAndFits())
    return;
  doComputePS(mInitialCentralTiles, false, false);
  plotFitPS(false);
}

void MyApp::setMinViewsForAstig(int value, bool doingOtherAction)
{
  mMinViewsForAstig = value;
  mAllViewsAstigmatism = 0.;
  if (mFindAstigmatism) {
    if (adjustCachesForTileParams() || doingOtherAction)
      return;
    multipleSpectraAndFits();
  }
}

void MyApp::setMinViewsForPhase(int value, bool doingOtherAction)
{
  mMinViewsForPhase = value;
  if (mFindPhaseShift) {
    if (adjustCachesForTileParams() || doingOtherAction)
      return;
    multipleSpectraAndFits();
  }
}

/*
 * Replot using previously fit parameters with a different defocus
 */
void MyApp::replotWithDefocus(double defocus) 
{
  int i;
  QVector<QPointF> data;
  const double inc = 1.0 / (mDim - 1);
  double *fittedData = (double *)malloc(mDim * sizeof(double));
  if (!fittedData)
    exitError("Allocation failed: out of memory!");

  mSimplexEngine->recomputeCTF(fittedData, defocus);
  for (i = 0; i < mDim; i++)
    data.append(QPointF(i * inc, fittedData[i]));
  if (mPlotter)
    mPlotter->setCurveData(1, data);
  free(fittedData);
}

/*
 * Initializes slice cache, opens stack, reads angle file
 * This should only be called once (per cache) with actual data stack
 */
void MyApp::setSlice(const char *stackFile, char *angleFile, sliceCacheEnum cacheSelector,
                     bool isNoise)
{
  int ind, lowNear = 0, highNear = 0, numInSum = 0, numTemp, lowTemp, highTemp;
  int numSum = 0;
  float midAngle, minAngle = 1.e10;
  SliceCache *pCache =
    (cacheSelector == SLICE_CACHE_PRIMARY) ? mCache : mCache2;

  //init and clear old contents; allocate temp PS
  pCache->initCache(stackFile, mDim, mZeroFitMethod == FIT_USE_CTFFIND ? -1 : mHyperRes,
                    (mFocalPairProcessing || isNoise) ? 1 : mNumSectors,
                    mNxx, mNyy, mNzz);

  // Read in array of tilt angles once from file
  B3DFREE(mTiltAngles);
  mTiltAngles = NULL;
  mMinAngle = 10000.;
  mMaxAngle = -10000.;
  if (!isNoise) {
    if (angleFile) {
      mTiltAngles = readTiltAngles(angleFile, mNzz, mAngleSign, mMinAngle,
                                   mMaxAngle);
    } else {
      mNoAnglesEntered = true;
      mTiltAngles = B3DMALLOC(float, mNzz);
      if (!mTiltAngles)
        exitError("Allocating array for tilt angles");
      for (ind = 0; ind < mNzz; ind++)
        mTiltAngles[ind] = ind * 0.01;
      mMinAngle = 0.;
      mMaxAngle = 0.01 * (mNzz - 1.);
    }
  
    B3DFREE(mSortedAngles);
    mSortedAngles = B3DMALLOC(float, mNzz);
    if (!mSortedAngles)
      exitError("Allocating array for sorted angles");
    getSortedAngles(1);
    if (!mNumSortedAngles)
      exitError("The list of views to skip excludes all of the views");
    getSortedAngles(0);
    mAutoFromAngle = mSortedAngles[0];
    mAutoToAngle = mSortedAngles[mNumSortedAngles - 1];
  }

  // Needed angles to exist before this is called: this determines strips
  pCache->clearAndSetSize(mDim, mZeroFitMethod == FIT_USE_CTFFIND ? -1 : mHyperRes, 
                          mTileSize, mRawTileSize, isNoise || !angleFile);
  
  if (!isNoise) {
    if (angleFile) {

      // Now that we finally know z size, we can check the starting and ending
      // slices and fix them if they are off by 1 or otherwise inconsistent
      if (ilistSize(mSaved) && 
          checkAndFixDefocusList(mSaved, mTiltAngles, mNzz, mDefVersionIn)) {
        const char title[] = "WARNING: Inconsistent view numbers";
        const char message[] = "The view numbers in the existing defocus file were not"
        " all\nconsistent with the angular ranges.  You should find the defocus\n"
          "again in all of the ranges and save the new data.";
        showWarning(title, message);
      }
    }

    // Get the initial # views in range and range step from angular input
    if (mAutoStepAngle) {

      // For autofitting, go through the whole tilt range asking for the given step range
      // and take the mean number of views in those ranges
      for (ind = 0; ind < mNumSortedAngles && 
             (ind == 0 || mSortedAngles[ind] + mAutoStepRange <
              mSortedAngles[mNumSortedAngles - 1]); ind++) {
        midAngle = getMidAngleAndRangeIndices(mSortedAngles[ind], mSortedAngles[ind] + 
                                              mAutoStepRange, lowTemp, highTemp, numTemp);
        numInSum++;
        numSum += numTemp;
        if (fabs(midAngle) < minAngle) {
          minAngle = fabs(midAngle);
          lowNear = lowTemp;
          highNear = highTemp;
        }
      }
      mNumViewsInRange = B3DNINT((float)numSum / numInSum);
      mViewRangeStep = (mNzz - 1) * (mAutoStepAngle / (mMaxAngle - mMinAngle));
    } else {
      getMidAngleAndRangeIndices(mLowAngle, mHighAngle, lowNear, highNear, 
                                 mNumViewsInRange);
      mViewRangeStep = mNumViewsInRange / 2;
    }
    B3DCLAMP(mViewRangeStep, 1, mNzz / 2);
    mLowAngle = mSortedAngles[lowNear];
    mHighAngle = mSortedAngles[highNear];
  }

  pCache->whatIsNeeded(mLowAngle, mHighAngle, mStartingSlice, mEndingSlice);

  if (mTileIncluded)
    free(mTileIncluded);
  mTileIncluded = (int *)malloc(mNzz * sizeof(int));
}

/*
 * Computes PS from central tiles for noise, or chosen set of tiles otherwise
 */
void MyApp::computeInitPS(bool noisePS)
{
  doComputePS(noisePS || mInitialCentralTiles, noisePS, false);
}

/*
 * Slot for adding more tiles
 */
void MyApp::moreTileCenterIncluded()
{
  mAllViewsAstigmatism = 0.;
  moreTile(true);
  plotFitPS(false);
}

/*
 * Computes PS from all available tiles by scaling the frequencies to match
 * the first and second zero of the central tiles; skips central tiles if they
 * are already included
 */
void MyApp::moreTile(bool hasIncludedCentralTiles)
{
  doComputePS(false, false, hasIncludedCentralTiles);
}

void MyApp::setupNextSpecDefocuses()
{
  SavedDefocus *saved;

  float currAngle;
  int whichSlice, k, ind, centerInd = -1;
  std::vector<int> accessOrder = mCache->optimalAccessOrder();
  Ilist *savedList = mDoingAutofit ? mSavedCopy : mSaved;

  // Require fitting to only one view and "Use current defocus"
  if (mNumViewsInRange > 1 || !mUseCurDefocus)
    return;

  // Look at each slice and see if it is in table as a single-slice fit, save defocus
  mNextSpecSliceDefocus.resize(accessOrder.size());
  for (k = 0; k < (int)accessOrder.size(); k++) {
    whichSlice = accessOrder[k];
    currAngle = mCache->getAngle(whichSlice) / RADIANS_PER_DEGREE;
    if (fabs(currAngle - mLowAngle) < 0.1)
      centerInd = k;
    for (ind = 0; ind < ilistSize(savedList); ind++) {
      saved = (SavedDefocus *)ilistItem(savedList, ind);
      if (fabs(saved->lAngle - saved->hAngle) < 0.1 && fabs(currAngle - saved->hAngle) <
          0.1) {
        mNextSpecSliceDefocus[k] = B3DCHOICE(saved->defocus2 != 0., 
                                             0.5 * (saved->defocus + saved->defocus2),
                                             saved->defocus);
        break;
      }
    }
    
    // But require every one to be in the table
    if (ind >= ilistSize(savedList))
      return;
  }

  // And make sure the center angle was in there too; without this nothing is used
  if (centerInd >= 0)
    mNextSpecEffectiveDefocus = mNextSpecSliceDefocus[centerInd];
}

/*
 * Master computation routine with flags for whether it is doing an initial PS, a noise 
 * PS, (initialPS should be set as well), and if it has central tiles already
 */
void MyApp::doComputePS(bool initialPS, bool isNoisePS, bool hasIncludedCentralTiles)
{
  float astigTolFactor = 0.75;
  int halfSize = mTileSize / 2;
  int leftCounter, rightCounter;
  int k, ii, strip;
  float *spectrumTmp = NULL;
  float *scaledSpec = NULL;
  int ampArrSize = mTileSize * (halfSize + 1);
  double deltaZ; // in microns;
  const float freqInc = 1.0 / (mDim - 1);
  int *stripCounter = NULL;
  double *stripAvg = NULL;
  double leftMean, rightMean, stripDefocus, effectiveDefocus, astigOffset, sliceDefocus;
  double allTime, wedgeMeanDef;
  int stripWedge;
  bool failed;
  float *cacheLeftPS, *cacheRightPS;
  float plusMin, plusMax, minusMin, minusMax;
  std::vector<float> sectorFocus, wedgeCenters, wedgeRanges, wedgeOffsets;
  std::vector<int> focusBandNum, realSectorInd;
  const int numTiltSeries = (mFocalPairProcessing && !isNoisePS) ? 2 : 1;
  double phaseToUse = (mUseCurrentPhase || mNextSpecWedgeNum >= 0) ? mLastFitPhaseShift :
    NO_PHASE_VALUE;
  double cutonToUse = (mUseCurrentPhase || mNextSpecWedgeNum >= 0) ? mLastFitCutOnFreq : 
    NO_PHASE_VALUE;

  if (!initialPS && mUseCurDefocus && mDefocusFinder.getDefocus() < 0) {
    printf("Warning invalid defocus, computation halts\n");
    return;
  }
  //mDefocusFinder.setDefocus(mDefocusFinder.getExpDefocus());
  ensureFitRangeUpdated();

  if (mZeroFitMethod != FIT_USE_CTFFIND) {
    stripAvg = B3DMALLOC(double, mDim);
    stripCounter = B3DMALLOC(int, mDim);
    failed = !stripAvg || !stripCounter;
  } else {
    spectrumTmp = B3DMALLOC(float, 2 * ampArrSize);
    scaledSpec = B3DMALLOC(float, 2 * ampArrSize);
    failed = !spectrumTmp || !scaledSpec;
  }
  if (failed)
    exitError("Failed to allocate memory for sums");

  // These items need to be initialized when there are no central tiles already
  if (initialPS || !hasIncludedCentralTiles) {
    mTotalTileIncluded = 0;
    for (k = 0; k < mNzz; k++)
      mTileIncluded[k] = 0;
    for (k = 0; k < mDim; k++) {
      mRAverage[k] = 0.0;
      mFreqTileCounter[k] = 0;
      if (numTiltSeries == 2) {
        mRAverage1[k] = 0.0;
        mRAverage2[k] = 0.0;
        mFreqTileCounter2[k] = 0;
      }
    }
    if (mZeroFitMethod == FIT_USE_CTFFIND)
      for (k = 0; k < 2 * ampArrSize; k++)
        mAmpSpecSum[k] = 0.;
  }

  if (mPlotter)
    mPlotter->mTileButton->setEnabled(initialPS);

  if (mNextSpecEffectiveDefocus > 0.)
    effectiveDefocus = mNextSpecEffectiveDefocus;
  else if (!initialPS && mUseCurDefocus)
    effectiveDefocus = mDefocusFinder.getDefocus();
  else
    effectiveDefocus = mDefocusFinder.getExpDefocus();

  // Figure out the zero(s) to use for scaling given two defocus values
  // Fall back is the ratio of first zeros
  mLowZeroForScaling = 1;
  mHighZeroForScaling = 0;
  if (!isNoisePS && (mZeroFitMethod == FIT_CTF_LIKE || 
                     mZeroFitMethod == FIT_USE_CTFFIND)) {

    // When there is a true fitting range defined (for these methods), get number of
    // zeros in the range.  For CTF-like, set up two-point scaling between endpoints, or
    // just within the endpoints if there are enough zeros
    k = mDefocusFinder.getNumberOfZerosInRange(effectiveDefocus, mX2Index2 / (mDim - 1.),
                                               phaseToUse, cutonToUse);
    if (k > 1 && mZeroFitMethod == FIT_CTF_LIKE) {
      if (k < 5) {
        mHighZeroForScaling = k;
      } else {
        mLowZeroForScaling = 2;
        mHighZeroForScaling = k - 1;
      }
    } else if (k > 1) {
      
      // For Ctffind, a scaling ratio is needed with no offset, pick the middle of range
      mLowZeroForScaling = (k + 1) / 2;
    }
  }

  // Determine wedges if astigmatism defined
  mCurrentAstigmatism = 0.;
  mCurrentAstigAngle = 0.;
  if (mNextSpecAstigmatism != 0. && mZeroFitMethod != FIT_USE_CTFFIND && 
      !mFocalPairProcessing) {
    //mNextSpecAstigmatism = 0.00001;
    float astigTol, angleInc, df1, df2, angle, defMin = 1.e20, defMax = -1.e20;
    float wedgeStart, wedgeEnd, defocus;
    int sector, band, lastBand, startInd, endInd, numInWedges, numIn, numBands;
    mCurrentAstigmatism = mNextSpecAstigmatism;
    mCurrentAstigAngle = mNextSpecAstigAngle;

    // Get the defocus of each sector (in range of current wedge if doing a wedge)
    // and the min and max
    angleInc = 180. / mNumSectors;
    if (mNextSpecWedgeNum >= 0)
      mCache->getWedgeMinMaxes(mWedgeCenterAngle, mNextSpecWedgeRange, plusMin, plusMax, 
                               minusMin, minusMax);
    wedgeMeanDef = 0.;
    for (sector = 0; sector < mNumSectors; sector++) {
      angle = (sector + 0.5) * angleInc - 90.;
      if (mNextSpecWedgeNum < 0 || (angle >= plusMin && angle < plusMax) ||
          angle >= minusMin && angle < minusMax) {
        defocus = effectiveDefocus + 0.5 * mNextSpecAstigmatism * 
          cos(2. * (angle - mNextSpecAstigAngle) * RADIANS_PER_DEGREE);
        ACCUM_MIN(defMin, defocus);
        ACCUM_MAX(defMax, defocus);
        sectorFocus.push_back(defocus);
        realSectorInd.push_back(sector);
        wedgeMeanDef += defocus;
      }
    }
    wedgeMeanDef /= sectorFocus.size();

    // Get the tolerance for dividing into bands; keep this synced with 
    // evaluation in the astigmatism loop for wedge computations
    // On general principles, if the astigmatism is big enough, make it do two bands
    // for a final spectrum
    astigTol = mDefocusFinder.findTolerance(mX2Index2 / (mDim - 1.), 0.2,
                                             effectiveDefocus, phaseToUse, cutonToUse);
    df1 = defMax - defMin;
    if (df1 <= astigTol && df1 > 0.4 && mNextSpecWedgeNum < 0)
      astigTol = 0.6 * df1;

    // If the range is too big, divide it into bands and adjust tolerance to equalize
    if (df1 > astigTol)  {
      numBands = df1 / astigTol + 1;
      astigTol = (df1 + 0.001) / numBands;
      
      // Assign the sectors to bands first and find the first place where one starts
      startInd = -1;
      for (sector = 0; sector < sectorFocus.size(); sector++) {
        band = (int)((sectorFocus[sector] - defMin) / astigTol);
        focusBandNum.push_back(band);
        if (sector && startInd < 0 && band != lastBand)
          startInd = sector;
        lastBand = band;
      }

      // Walk from the first start until all the sectors are in wedges
      sector = startInd;
      numInWedges = 0;
      while (numInWedges < sectorFocus.size()) {

        // Walk along until the band number changes
        df1 = 0.;
        numIn = 0;
        while (focusBandNum[sector] == focusBandNum[startInd]) {
          endInd = sector;
          numInWedges++;
          numIn++;
          df1 += sectorFocus[sector];
          sector = (sector + 1) % sectorFocus.size();

          // Break for a new band if the sectors are not contiguous
          if (realSectorInd[sector] != (realSectorInd[endInd] + 1) % mNumSectors)
            break;
        }

        // Compute the wedge angles and save them, wrapping around as needed to give a
        // sensible range
        wedgeStart = realSectorInd[startInd] * angleInc - 90.;
        wedgeEnd = (realSectorInd[endInd] + 1) * angleInc - 90.;
        if (wedgeEnd < wedgeStart)
          wedgeEnd += 180.;
        wedgeRanges.push_back(wedgeEnd - wedgeStart);
        angle= (wedgeEnd + wedgeStart) / 2.;
        if (angle >= 90.)
          angle -= 180.;
        wedgeCenters.push_back(angle);
        df1 = df1 / numIn - 
          B3DCHOICE(mNextSpecWedgeNum < 0, effectiveDefocus, wedgeMeanDef);
        wedgeOffsets.push_back(df1);
        if (debugLevel > 1)
          printf("wedge %d  sectors %d to %d  center %f  range %f  offset %.3f\n", 
                 (int)wedgeCenters.size(), realSectorInd[startInd], realSectorInd[endInd],
                 angle, wedgeEnd - wedgeStart, df1);
        startInd = sector;
      }
    }        
  }

  std::vector<int> accessOrder = mCache->optimalAccessOrder();
  float currAngle;
  int minSlice = 100000, maxSlice = -1, whichSlice, totalTiles = 0;

  allTime = wallTime();
  mNumSlicesDone = accessOrder.size();
  if (mNextSpecWedgeRange == 0 && debugLevel && !isNoisePS)
    printf("Making spectrum for iter %d from %d slices with astigmatism %s known\n", 
           mAutofitIteration, mNumSlicesDone, mNextSpecAstigmatism ? "" : "NOT");
  if (debugLevel >= 1) {
    if (mNumSlicesDone == 1)
      printf("Slice %d is included, tilt angle is %.2f degrees. \n", accessOrder[0],
             mCache->getAngle(accessOrder[0]) / RADIANS_PER_DEGREE);
    else  {
      for (k = 0; k < (int)accessOrder.size(); k++) {
        ACCUM_MIN(minSlice, accessOrder[k]);
        ACCUM_MAX(maxSlice, accessOrder[k]);
      }
      printf("Slices %d to %d are included, tilt angles are %.2f to %.2f degrees. \n", 
             minSlice, maxSlice, mCache->getAngle(minSlice) / RADIANS_PER_DEGREE,
             mCache->getAngle(maxSlice) / RADIANS_PER_DEGREE);
    }
  }

  // Loop on the slices
  for (k = 0; k < (int)accessOrder.size(); k++) {
    whichSlice = accessOrder[k];
    currAngle = mCache->getAngle(whichSlice);
    sliceDefocus = effectiveDefocus;
    if (mNextSpecEffectiveDefocus > 0.)
      sliceDefocus = mNextSpecSliceDefocus[k];
    for (strip = (hasIncludedCentralTiles ? 1 : 0); 
         strip < (initialPS ? 1 : mCache->getNumStrips(whichSlice)); strip++) {

      // For compensating for fixed astigmatism, loop over wedges and set the wedge
      // range/center and amount to adjust the defocus used for scaling
      int wedgeLoopLim = B3DMAX(1, wedgeCenters.size());
      for (int wedge = 0; wedge < wedgeLoopLim; wedge++) {
        astigOffset = 0.;
        if (wedgeCenters.size()) {
          astigOffset = wedgeOffsets[wedge];
          mWedgeCenterAngle = wedgeCenters[wedge];
          mNextSpecWedgeRange = wedgeRanges[wedge];
        }

        // For focal pair processing, loop over the 2 tilt series
        for (int tiltSeries = 0; tiltSeries < numTiltSeries; tiltSeries++) {
          SliceCache *cachePtr;
          double *avgPtr;
          int *freqTileCounterPtr;
          if (tiltSeries == 0) {
            cachePtr = mCache;
            freqTileCounterPtr = mFreqTileCounter;
            if (mFocalPairProcessing)
              avgPtr = mRAverage1;
            else
              avgPtr = mRAverage;
          }
          else { // tiltSeries == 1
            cachePtr = mCache2;
            avgPtr = mRAverage2;
            freqTileCounterPtr = mFreqTileCounter2;
          }

          // Get the strip sum from the data or the cache
          stripWedge = -1;
          if (mNextSpecWedgeRange && mNextSpecWedgeNum >= 0 && !wedgeCenters.size() &&
              mZeroFitMethod != FIT_USE_CTFFIND)
            stripWedge = mNextSpecWedgeNum;
          cachePtr->getHyperPS(whichSlice, strip, stripWedge, &cacheLeftPS, 
                                 &cacheRightPS, leftCounter, rightCounter, leftMean, 
                                 rightMean);
                               
          if (initialPS && isNoisePS)
            mStackMean = (leftMean * leftCounter + rightMean * rightCounter) / 
              (leftCounter + rightCounter);
          
          // Get the signed delta Z for this strip
          deltaZ = (strip + 0.5) * (cachePtr->getStripWidthPixels(whichSlice) / 2.0) * 
            mPixelSize * tan(currAngle) / 1000.0; // in microns;
          
          // Compute scale and offset for the strip to the right of center
          stripDefocus = sliceDefocus + astigOffset + B3DCHOICE(initialPS, 0, deltaZ);
          if (tiltSeries > 0)
            stripDefocus = stripDefocus + mDefocusOffset / 1000.0;

          // Add in right side strip
          if (mZeroFitMethod != FIT_USE_CTFFIND) {
            scaleAndAddStrip(cacheRightPS, stripAvg, stripCounter, rightCounter,
                             rightMean, effectiveDefocus, stripDefocus, freqInc, 
                             cachePtr, avgPtr,freqTileCounterPtr);
          } else {
            addAmplitudeSpectrum(cacheRightPS, spectrumTmp, scaledSpec,
                                 effectiveDefocus, stripDefocus);
          }

          // Compute scale and offset for the strip to the left of center
          stripDefocus = sliceDefocus + astigOffset - B3DCHOICE(initialPS, 0.,deltaZ);
          if (tiltSeries > 0)
            stripDefocus = stripDefocus + mDefocusOffset / 1000.0;

          // Add in left side strip
          if (mZeroFitMethod != FIT_USE_CTFFIND) {
            scaleAndAddStrip(cacheLeftPS, stripAvg, stripCounter, leftCounter,
                             leftMean, effectiveDefocus, stripDefocus, freqInc, 
                             cachePtr, avgPtr, freqTileCounterPtr);
          } else {
            addAmplitudeSpectrum(cacheLeftPS, spectrumTmp, scaledSpec,
                                 effectiveDefocus, stripDefocus);
          }
        } // for wedges
      }  // for tiltSeries

      if (mFocalPairProcessing)
        combinePowerSpectra();

      mTileIncluded[whichSlice] += leftCounter + rightCounter;
      mTotalTileIncluded += leftCounter + rightCounter;
      if (debugLevel >= 3)
        printf("strip=%d leftCounter=%d rightCounter=%d mTileIncluded[%d]=%d\n",
           strip, leftCounter, rightCounter, whichSlice, mTileIncluded[whichSlice]);
    } // loop on strips

    if (debugLevel >= 2)
      printf("%d tiles of slice %d have been included\n",
             mTileIncluded[whichSlice], whichSlice);
    totalTiles += mTileIncluded[whichSlice];

    if (debugLevel >= 3)
      printf("***strip=%d deltaZ=%f(microns) mTileIncluded[%d]=%d\n",
             strip, deltaZ, whichSlice, mTileIncluded[whichSlice]);
  }// the k-th slice

  if (mZeroFitMethod == FIT_USE_CTFFIND && totalTiles)
    for (ii = 0; ii < 2 * ampArrSize; ii++)
      mAmpSpecSum[ii] /= totalTiles;

  if (debugLevel >= 3)
    printf("Spectrum time %.4f\n", wallTime() - allTime);
  
  // Try setting this here and making the caller responsible for managing it otherwise
  // Also "use up" the astigmatism settings
  mNextFitKnowAstig = false;
  mNextFitKnowPhase = false;
  mNextSpecAstigmatism = 0.;
  mNextSpecAstigAngle = 0.;
  mNextSpecWedgeRange = 0.;
  mNextSpecEffectiveDefocus = 0.;

  free(stripCounter);
  free(stripAvg);
  free(spectrumTmp);
  free(scaledSpec);
}

/*
 * Add in the summed strip power spectrum with linear frequency  
 * scaling to match locations of the zeros and add into array avgPtr. Note
 * that *avgPtr (both input and output) is a real power spectrum, not just
 * a sum of energies; i.e. the summed energy has already been divided by 
 * the product of the number of pixels and tiles.
 */
void MyApp::scaleAndAddStrip
(float *psSum, double *stripAvg, int *stripCounter, int counter,
 double mean, double refDefocus, double curDefocus, float freqInc, SliceCache *cachePtr, 
 double *avgPtr, int *freqTileCounter)
{
  int ii, jj, npsIndex, stripRIndex, stripLIndex, lnum;
  double freq, freqst, freqnd, lfrac, freqOffset, freqScale, curHighZero, refHighZero;
  double hyperInc = freqInc / mHyperRes;
  int *freqCount = cachePtr->getFreqCount();
  double phase = (mUseCurrentPhase || mNextSpecWedgeNum >= 0) ? mLastFitPhaseShift :
    NO_PHASE_VALUE;
  double cuton = (mUseCurrentPhase || mNextSpecWedgeNum >= 0) ? mLastFitCutOnFreq : 
    NO_PHASE_VALUE;

  double refLowZero = mDefocusFinder.getAnyOneZero(refDefocus, mLowZeroForScaling, phase,
                                                   cuton);
  double curLowZero = mDefocusFinder.getAnyOneZero(curDefocus, mLowZeroForScaling, phase,
                                                   cuton);
  double npsScale = (double)mTileSize / (mRawTileSize * mHyperRes);
  
  if (mHighZeroForScaling <= 0) {
    freqScale = refLowZero / curLowZero;
    freqOffset = 0.;
  } else {
    refHighZero = mDefocusFinder.getAnyOneZero(refDefocus, mHighZeroForScaling, phase,
                                               cuton);
    curHighZero = mDefocusFinder.getAnyOneZero(curDefocus, mHighZeroForScaling, phase,
                                               cuton);
    freqScale = (refHighZero - refLowZero) / (curHighZero - curLowZero);
    freqOffset = refLowZero - freqScale * curLowZero;
  }
  
  if (!counter)
    return;

  // Get noise PS based on the mean intensity
  setNoiseForMean(mean);

  // Zero average arrays
  for (ii = 0; ii < mDim; ii++) {
    stripCounter[ii] = 0;
    stripAvg[ii] = 0.0;
  }

  // Loop over average PS, divide by the noise appropriate to the
  // unshifted frequency, then scale/shift and add into the average
  for (ii = 0; ii < mDim * mHyperRes - mHyperRes / 2; ii++) {
    // Get index for dividing by noise
    npsIndex = B3DNINT(npsScale * ii);

    // Get starting and ending frequency of hyper bin and adjust them, get
    // index of strip bin each side falls into
    // hyperbins are based on truncating the computed index, hence this start-end is
    // simply multiplying by the increment
    // The NINT here makes strip bins be centered on thieir nominal frequencies
    freqst = ii * hyperInc;
    freqnd = freqst + hyperInc;
    freqst = freqScale * freqst + freqOffset;
    stripLIndex = B3DNINT(freqst / freqInc);
    freqnd = freqScale * freqnd + freqOffset;
    stripRIndex = B3DNINT(freqnd / freqInc);
    if (stripRIndex >= mDim || stripLIndex < 0)
      continue;

    // If both sides of hyper bin are in the strip bin, then just add it in
    if (stripRIndex == stripLIndex) {
      stripAvg[stripRIndex] += psSum[ii] / mNoisePS[npsIndex];
      stripCounter[stripRIndex] += freqCount[ii] * counter;
    } else if (freqCount[ii]) {
      // Otherwise figure out how to split the hyper bin between two strip bins
      // based on frequency at left edge of upper bin
      freq = (stripRIndex - 0.5) * freqInc;
      lfrac = (freq - freqst) / hyperInc;
      lfrac = B3DMAX(0., B3DMIN(1., lfrac));
      lnum = B3DNINT(lfrac * freqCount[ii]);
      lfrac = (double)lnum / freqCount[ii];
      stripAvg[stripLIndex] += lfrac * psSum[ii] / mNoisePS[npsIndex];
      stripCounter[stripLIndex] += lnum * counter;
      stripAvg[stripRIndex] += (1. - lfrac) * psSum[ii] / mNoisePS[npsIndex];
      stripCounter[stripRIndex] += (freqCount[ii] - lnum) * counter;
    }
  } // for ii

  for (ii = 0; ii < mDim; ii++) {
    jj = stripCounter[ii];
    if (jj) {
      avgPtr[ii] = (freqTileCounter[ii] * avgPtr[ii] + stripAvg[ii]) /
                   (freqTileCounter[ii] + jj);
      freqTileCounter[ii] += jj;
    }
  }
}

/* 
 * Form an amplitude spectrum of the correct size from the array of summed FFT amplitudes
 * for a strip in ampArray, putting it into "spectrum", scaling it between defocuses into
 * scaledSpec, then adding into mAmpSpecSum
 */
void MyApp::addAmplitudeSpectrum(float *ampArray, float *spectrum, float *scaledSpec, 
                                 double refDefocus, double curDefocus)
{
  int ind, iy, iCen, nxDim = mTileSize + 2;
  float center = 0., dmean = 0.;
  float amat[2][2], mag, stretch;
  int linear = 0;
  float astigDiff = mNextSpecAstigmatism / 2.;

  // Mirror and center the spectrum from the fft-amplitude array
  makeAmplitudeSpectrum(ampArray, spectrum, -mTileSize, nxDim);

  // Copy the right edge pixels from the last defined one
  for (iy = 0; iy < mTileSize; iy++) {
    ind = iy * nxDim + mTileSize - 1;
    spectrum[ind + 1] = spectrum[ind];
    spectrum[ind + 2] = spectrum[ind];
  }

  // Get the edge mean
  dmean = sliceEdgeMean(spectrum, nxDim, 0, mTileSize - 1, 0, mTileSize - 1);

  // Fix the center pixel
  iCen = mTileSize / 2;
  for (iy = iCen - 1; iy <= iCen + 1; iy++) {
    for (ind = iCen - 1; ind <= iCen + 1; ind++) {
      if (iy != iCen || ind != iCen)
        center += spectrum[iy * nxDim + ind];
    }
  }
  spectrum[iCen * nxDim + iCen] = center / 8.;

  // Scale and stretch the spectrum to fit the reference defocus
  if (fabs(refDefocus / curDefocus) < 1.e-5) {
    scaledSpec = spectrum;
  } else {
    mag = mDefocusFinder.getAnyOneZero(refDefocus - astigDiff, mLowZeroForScaling) / 
      mDefocusFinder.getAnyOneZero(curDefocus - astigDiff, mLowZeroForScaling);
    stretch = (mDefocusFinder.getAnyOneZero(refDefocus + astigDiff, mLowZeroForScaling) /
               mDefocusFinder.getAnyOneZero(curDefocus + astigDiff, mLowZeroForScaling)) /
      mag;
    //printf("ref %.3f  cur %.3f  astig %.3f  mag %.4f  stretch %.4f\n",
    //     refDefocus, curDefocus, astigDiff, mag, stretch);
    rotmagstrToAmat(0., mag, stretch, mNextSpecAstigAngle, &amat[0][0], &amat[1][0], 
                    &amat[0][1], &amat[1][1]);
    //amat[0][0] = amat[1][1] = curDefocus / refDefocus;
    //amat[0][1] = amat[1][0] = 0.;
    cubinterp(spectrum, scaledSpec, nxDim, mTileSize, nxDim, mTileSize, amat, 
              mTileSize / 2 + 0.5, mTileSize / 2 + 0.5, -0.5, 0.5, 1., dmean, linear);
  }
  
  for (ind = 0; ind < nxDim * mTileSize; ind++)
    mAmpSpecSum[ind] += scaledSpec[ind];
}

/* 
 * Return number of wedges from number of sectors and wedge intervale
 */
int MyApp::getNumWedges()
{
  float angleInc = 180. / mNumSectors;
  int numSectInStep = B3DNINT(mWedgeInterval / angleInc);
  return B3DNINT((float)mNumSectors / numSectInStep);
}


/*
 * Slot that is called when the fitting range changes or some other parameters change
 */
void MyApp::rangeChanged(double x1_1, double x1_2, double x2_1, double x2_2)
{
  mX1Index1 = B3DNINT(x1_1 * (mDim - 1));
  B3DCLAMP(mX1Index1, 1, mDim - 1);
  mX1Index2 = B3DNINT(x1_2 * (mDim - 1));
  B3DCLAMP(mX1Index2, 1, mDim - 1);
  mX2Index1 = B3DNINT(x2_1 * (mDim - 1));
  B3DCLAMP(mX2Index1, 1, mDim - 1);
  mX2Index2 = B3DNINT(x2_2 * (mDim - 1));
  B3DCLAMP(mX2Index2, 1, mDim - 1);
  if (mPlotter && mPlotter->mFittingDia)
    mPlotter->mFittingDia->updateStartsEnds();
  manageCropWarning();
  if (mUpdatingFitRange)
    return;
  if (adjustCachesForTileParams())
    return;
  if (multipleSpectraAndFits())
    return;
  fitPsFindZero();
}

/*
 * Slot for responding to angle range change or change in tile definition by
 * computing tile PS if needed and recomputing the PS curves
 */
void MyApp::angleChanged(double lAngle, double hAngle, double expDefocus, double defTol,
                         int tSize, double axisAngle, double leftTol, double rightTol,
                         double cropPixel)
{
  //If tilt angle range does not change, do not need to reload slices;
  if (lAngle != mLowAngle || hAngle != mHighAngle) {
    setLowAngle(lAngle);
    setHighAngle(hAngle);
    mCache->whatIsNeeded(lAngle, hAngle, mStartingSlice, mEndingSlice);
    if (mCache2)
      mCache2->whatIsNeeded(lAngle, hAngle, mStartingSlice, mEndingSlice);
  }
  if (mStartingSlice < 0 || mEndingSlice < 0)
    return;

  adjustCachesForTileParams(defTol, tSize, axisAngle, leftTol, rightTol, cropPixel);
  mDefocusFinder.setExpDefocus(expDefocus);

  // plot and fit the new PS
  if (multipleSpectraAndFits())
    return;

  // On an iteration with astigmatism, adopt the values from the previous fit(s)
  if (((mAutofitIteration && mFindAstigmatism) || mSettingFromTable) && 
      (mZeroFitMethod == FIT_USE_CTFFIND || mZeroFitMethod == FIT_CTF_LIKE)) {
    mNextSpecAstigmatism = mLastSpecAstigmatism;
    mNextSpecAstigAngle = mLastSpecAstigAngle;
    mNextFitAstigmatism = 1.e4 * mNextSpecAstigmatism;
    mNextFitAstigAngle = mNextSpecAstigAngle;
  }

  // Similarly for phase shift, for an iteration or setting from table, use "Last"
  if (((mAutofitIteration && mFindPhaseShift) || mSettingFromTable) &&
      mZeroFitMethod == FIT_CTF_LIKE) {
    mNextFitPhaseShift = mLastFitPhaseShift;
    mNextFitCutOnFreq = mLastFitCutOnFreq;
  }

  mCache->setWall1D(0.);
  mCache->setWall2D(0.);
  doComputePS(mInitialCentralTiles, false, false);
  //printf("times 2D %.4f  1D %.4f\n", mCache->getWall1D(), mCache->getWall2D());
  mNextFitKnowAstig = mZeroFitMethod == FIT_USE_CTFFIND && 
    mEndingSlice + 1 - mStartingSlice < mMinViewsForAstig;
  plotFitPS(false);
}

/*
 * Check a variety of changes that would require setting the cache again
 * This variant gets all the tile parameters needed for the real call
 */
int MyApp::adjustCachesForTileParams()
{
  double defTol, axisAngle, leftTol, rightTol, cropPixel;
  int tSize;
  bool tolOK;
  if (mPlotter && mPlotter->mAngleDia) {
    tolOK = mPlotter->mAngleDia->getTileTolerances(defTol, tSize, axisAngle, leftTol, 
                                                   rightTol, cropPixel);
    if (!tolOK)
      return 1;
    adjustCachesForTileParams(defTol, tSize, axisAngle, leftTol, rightTol, cropPixel);
  }
  return 0;
}

/*
 * Free and recreate various components if any of these passed in parameters or some other
 * conditions such as cropping have changed
 */
int MyApp::adjustCachesForTileParams(double defTol, int tSize, double axisAngle, 
                                     double leftTol, double rightTol, double cropPixel)
{
  int rawTile = tSize;
  float changeRatio;
  if (mCropSpectra) {
    rawTile = 2 * B3DNINT(0.5 * cropPixel * tSize / mPixelSize);
    rawTile = B3DMAX(rawTile, tSize);
    rawTile = niceFrame(rawTile, 2, niceFFTlimit());
  }
  mTiltAxisAngle = axisAngle;
  mDefocusTol = defTol;
  mLeftDefTol = leftTol;
  mRightDefTol = rightTol;
  mCropPixelSizeInDia = cropPixel;

  // Re-analyze the stripping of the tiles if tile size or any tolerance changes
  if (tSize != mTileSize || fabs(mCache->getStripCacheLeftTol() - leftTol) > 1.e-4 ||
      fabs(mCache->getStripCacheRightTol() - rightTol) > 1.e-4 || rawTile != mRawTileSize
      || fabs(mCache->getStripCacheDefocusTol() - defTol) > 1.e-4 || 
      fabs(mCache->getStripCacheAxisAngle() - axisAngle) > 1.e-4) {
    mCache->clearAndSetSize(mDim, mZeroFitMethod == FIT_USE_CTFFIND ? -1 : mHyperRes, 
                            tSize, rawTile, false);
    if (mCache2)
      mCache2->clearAndSetSize(mDim, mZeroFitMethod == FIT_USE_CTFFIND ? -1 : mHyperRes,
                               tSize, rawTile, false);
    if (mAmpSpecSum) {
      free(mAmpSpecSum);
      mAmpSpecSum = B3DMALLOC(float, tSize * (tSize + 2));
    if (!mAmpSpecSum)
      exitError("Error allocating memory for amplitude spectrum");
    }
    mAllViewsAstigmatism = 0.;
  }

  // If the effective pixel size changed, need to modify defocus finder and modify all
  // the fitting limits
  if (fabs((double)rawTile / tSize - (double)mRawTileSize / mTileSize) > 1.e-4) {
    mDefocusFinder.setPixelSize(rawTile <= tSize ? mPixelSize :
                                (rawTile * mPixelSize) / tSize);
    changeRatio = ((float)rawTile * mTileSize) / (tSize * mRawTileSize);
    mX1Index1 = B3DNINT(changeRatio * mX1Index1);
    B3DCLAMP(mX1Index1, 1, mDim - 1);
    mX2Index1 = B3DNINT(changeRatio * mX2Index1);
    B3DCLAMP(mX2Index1, 1, mDim - 1);
    mX1Index2 = B3DNINT(changeRatio * mX1Index2);
    B3DCLAMP(mX1Index2, 1, mDim - 1);
    mX2Index2 = B3DNINT(changeRatio * mX2Index2);
    B3DCLAMP(mX2Index2, 1, mDim - 1);
    if (mPlotter && mPlotter->mFittingDia)
      mPlotter->mFittingDia->updateStartsEnds();
  }

  mTileSize = tSize;
  mRawTileSize = rawTile;
  return 0;
}

/*
 * Master routine for the full sequence of spectra and fits required for finding 
 * astigmatism, or for finding phase from more views than the are used for defocus
 * Returns 0 if nothing was done, -1 if everything was done, or 1 for an error
 */
int MyApp::multipleSpectraAndFits()
{
  int numForDefocus, defLowSort, defHighSort, defLowView, defHighView, minForPrePhase;
  int astigStart, astigEnd, temp, numPoints, numSectInStep, numInWedge, numSteps;
  int astigLowSort, astigHighSort, astigLowView, astigHighView;
  int phaseLowSort, phaseHighSort, phaseLowView, phaseHighView;
  float angleInc, angleStep, meanFocus, error;
  float angle, maxRange = -1., defMin, defMax, astigTol;
  float defocus, plusMin, plusMax, minusMin, minusMax, errSum;
  int sector, wedge;
  const int maxAstigLoops = 5;
  float astigmatisms[maxAstigLoops];
  float minAstigChange = 0.01;
  double midAngle, tempMid, tempLow, tempHigh;
  float nextSpecAstig = 0., nextSpecAngle = 0., nextFitAstig = 0., nextFitAngle = 0.;
  double nextFitPhaseShift = NO_PHASE_VALUE, nextFitCutOnFreq = NO_PHASE_VALUE;
  double nextEffectiveDefocus = 0.;
  double defocusToRestore = mDefocusFinder.getDefocus();
  CtffindParams params;
  float resultsArray[7], lastBinFreq;
  vector<float> centerAngles, fitDefocus;
  int widerRange = 0;
  bool excludeSave = mExcludingSkipList;
  bool findAstig, findPhase, ast, needAstigRange, needPhaseRange;
  bool nextFitKnowAstig = false;
  bool ctfLike = mZeroFitMethod == FIT_CTF_LIKE;
  bool useCtffind = mZeroFitMethod == FIT_USE_CTFFIND;
  double wallStart, specCum = 0., plotCum = 0.;

  mLastWedgeFitError = -1.;
  ensureFitRangeUpdated();

  if (mAutofitIteration || mSettingFromTable)
    return 0;

  // Get middle angle and number being fit for defocus
  midAngle = getMidAngleAndRangeIndices(mLowAngle, mHighAngle, defLowSort, defHighSort, 
                                        numForDefocus);
  getRangesFromMidAngle(numForDefocus, 0, midAngle, mLowAngle, mHighAngle, defLowSort, 
                        defHighSort, defLowView, defHighView);

  // Do the same for astigmatism and phase
  tempMid = midAngle;
  getRangesFromMidAngle(B3DMAX(numForDefocus, mMinViewsForAstig), 0, tempMid, tempLow, 
                        tempHigh, astigLowSort, astigHighSort, astigLowView, 
                        astigHighView);
  tempMid = midAngle;
  getRangesFromMidAngle(B3DMAX(numForDefocus, mMinViewsForPhase), 0, tempMid, tempLow, 
                        tempHigh, phaseLowSort, phaseHighSort, phaseLowView, 
                        phaseHighView);
  mExcludingSkipList = excludeSave;

  // Flags for whether bigger ranges are needed for astigmatism or phase
  needAstigRange = defLowSort != astigLowSort || defLowView != astigLowView || 
    defHighSort != astigHighSort || defHighView != astigHighView || 
    numForDefocus < mMinViewsForAstig;
  needPhaseRange = defLowSort != phaseLowSort || defLowView != phaseLowView || 
    defHighSort != phaseHighSort || defHighView != phaseHighView || 
    numForDefocus < mMinViewsForPhase;

  // Set flags for the operations needing wider ranges or multiple runs here
  findAstig = ((useCtffind && needAstigRange) || ctfLike) && mFindAstigmatism;
  findPhase = mFindPhaseShift && (ctfLike || useCtffind) && needPhaseRange; 
  if (!findAstig && !findPhase)
    return 0;

  /*
   * OPERATIONS TO FIND ASTIGMATISM
   */
  if (findAstig) {

    // If all views are needed and it has been done, use that result
    if (mMinViewsForAstig >= mNzz && mAllViewsAstigmatism) {
      mNextSpecAstigmatism = mLastSpecAstigmatism = mAllViewsAstigmatism;
      mNextSpecAstigAngle = mLastSpecAstigAngle = mAllViewsAstigAngle;
      mNextFitAstigmatism = 1.e4 * mNextSpecAstigmatism;
      mNextFitAstigAngle = mNextSpecAstigAngle;

    } else {

      // If finding phase too, first find phase for either set of slices being used
      // for astig, or those required for phase
      if (ctfLike && mFindPhaseShift) {
        minForPrePhase = B3DMAX(mMinViewsForAstig, mMinViewsForPhase);
        ast = mMinViewsForPhase < mMinViewsForAstig;
        if (numForDefocus < minForPrePhase || 
            defLowSort != (ast ? astigLowSort : phaseLowSort) ||
            defLowView != (ast ? astigLowView : phaseLowView) || 
            defHighSort != (ast ? astigHighSort : phaseHighSort) ||
            defHighView != (ast ? astigHighView : phaseHighView)) {
          setWiderAngleRange(midAngle, minForPrePhase);
          widerRange = minForPrePhase;
          setupNextSpecDefocuses();
          nextEffectiveDefocus = mNextSpecEffectiveDefocus;
        }

        // Compute and plot, store results to use in wedge loop
        doComputePS(mInitialCentralTiles, false, false);
        plotFitPS(false);
        nextFitPhaseShift = mLastFitPhaseShift;
        nextFitCutOnFreq = mLastFitCutOnFreq;

        // Restore the original range if astigmatism doesn't need it
        if (!needAstigRange && widerRange) {
          if (debugLevel)
            printf("restoring range to %d\n", numForDefocus);
          mExcludingSkipList = excludeSave;
          mCache->whatIsNeeded(mLowAngle, mHighAngle, mStartingSlice, mEndingSlice);
          widerRange = 0;
          setupNextSpecDefocuses();
          nextEffectiveDefocus = mNextSpecEffectiveDefocus;
        }
      }  // done finding phase for astig

      // Adjust the starting and ending slices if necessary
      if (needAstigRange) {
        setWiderAngleRange(midAngle, mMinViewsForAstig);
        widerRange = mMinViewsForAstig;
        setupNextSpecDefocuses();
        nextEffectiveDefocus = mNextSpecEffectiveDefocus;
      }

      // Ctffind variation
      if (useCtffind) {      
        doComputePS(mInitialCentralTiles, false, false);
    
        // Set up and solve for astigmatism
        setupCtffindParams(params, false, 0., 0.);
        if (!ctffind(params, mAmpSpecSum, params.box_size + 2, resultsArray, NULL, NULL, 
                     NULL, numPoints, lastBinFreq)) {
          printf("Error in ctffind\n");
          return 1;
        }
    
        // Get results and set up astigmatism for next spec and fit
        mNextFitAstigmatism = resultsArray[0] - resultsArray[1];
        mNextSpecAstigAngle = resultsArray[2];
        mNextSpecAstigmatism = mNextFitAstigmatism / 1.e4;
        mNextFitAstigAngle = mNextSpecAstigAngle;
        mLastSpecAstigmatism = mNextSpecAstigmatism;
        mLastSpecAstigAngle = mNextSpecAstigAngle;
        mNextFitKnowAstig = true;
        printf("Astigfit f1=%.3f  f2=%.3f ang=%.1f  astig=%.3f  score=%.4f  "
               "fit to %.1f  alias %.1f\n", resultsArray[0] / 1.e4, resultsArray[1] / 1.e4,
               resultsArray[2], mNextSpecAstigmatism, 
               resultsArray[4], resultsArray[5], resultsArray[6]);
        //imodBackupFile("astig-spectrum.mrc");
        //writeSlice("astig-spectrum.mrc", mAmpSpecSum, mTileSize + 2, mTileSize);
      } else {

        /*
         * CTF-LIKE FITTING
         */
        // Preliminaries: get the wedge parameters
        if (mPlotter && mPlotter->mFittingDia)
          mPlotter->mFittingDia->updateSecToShowWedges();
        angleInc = 180. / mNumSectors;
        numSectInStep = B3DNINT(mWedgeInterval / angleInc);
        numInWedge = B3DNINT(mWedgeAngleRange / angleInc);
        angleStep = angleInc * numSectInStep;
        numSteps = B3DNINT(180. / angleStep);
        if (!mDoingAutofit)
          manageEnablesForMultipleFits(true);

        // Need an initial fit to set center of defocus range
        doComputePS(mInitialCentralTiles, false, false);
        plotFitPS(false);
        mWedgeFitCenterDefocus = mDefocusFinder.getDefocus();
        if (!mSaveAndExit)
          QApplication::setOverrideCursor(QCursor(Qt::WaitCursor));

        // Loop on multiple iterations with astigmatism adjustment in the wedges on the
        // later iterations if needed
        for (int astigLoop = 0; astigLoop < maxAstigLoops; astigLoop++) {
          mSimplexEngine->setDefocusRange(mMaxAstigmatism);
          centerAngles.clear();
          fitDefocus.clear();
          errSum = 0.;

          // Loop on the wedges
          for (mNextSpecWedgeNum = 0; mNextSpecWedgeNum < numSteps; mNextSpecWedgeNum++) {

            // Set up one-shot parameters for the spectrum computation
            mNextSpecWedgeRange = mWedgeAngleRange;
            mNextSpecEffectiveDefocus = nextEffectiveDefocus;
            mNextFitPhaseShift = nextFitPhaseShift;
            mNextFitCutOnFreq = nextFitCutOnFreq;
            mWedgeCenterAngle = mNextSpecWedgeNum * angleStep + 
              ((numInWedge % 2) ? 0.5 * angleInc : 0.) - 90.;
            centerAngles.push_back(mWedgeCenterAngle);

            // On an iteration, set up to use the last astigmatism and adjust the starting
            // defocus for fitting to the expected measured value, averaging over the 
            // wedge
            if (astigLoop) {
              mNextSpecAstigmatism = mLastSpecAstigmatism;
              mNextSpecAstigAngle = mLastSpecAstigAngle;
              mNextFitStartDefocus = mWedgeFitCenterDefocus;
              for (int div = -10; div < 10; div++)
                mNextFitStartDefocus += 0.5 * mNextSpecAstigmatism * 
                  cos(2. * (mWedgeCenterAngle + (div + 0.5) * mWedgeAngleRange / 20. - 
                      mNextSpecAstigAngle) * RADIANS_PER_DEGREE) / 20.;
            }

            // Get the spectrum
            debugLevel -= 2;
            if (debugLevel >= 1) wallStart = wallTime();
            doComputePS(mInitialCentralTiles, false, false);
            if (debugLevel >= 1) specCum += wallTime() - wallStart;
            if (debugLevel >= 1) wallStart = wallTime();

            // Do the fit
            plotFitPS(false);
            if (debugLevel >= 1) plotCum += wallTime() - wallStart;
            debugLevel += 2;
            if (!mSaveAndExit) {
              qApp->processEvents();
              if (mShowWedgePlots && mSecToShowWedges > 0)
                b3dMilliSleep((int)(1000. * mSecToShowWedges) + 1);
            }
              
            // Save result in array
            fitDefocus.push_back(mDefocusFinder.getDefocus());
            if (debugLevel == 2)
              printf("Fitting angle %g  defocus %.3f   error %.4f\n", 
                     centerAngles[centerAngles.size() - 1], mDefocusFinder.getDefocus(),
                     mSimplexEngine->getLastError());
            errSum += mSimplexEngine->getLastError();
            mDefocusFinder.setDefocus(defocusToRestore);
          } // end of wedge loop
          mNextFitStartDefocus = 0.;
          if (debugLevel)
            printf("Mean fit error for wedges %.5f\n", errSum / numSteps);

          // Fit to wedge defocuses to find the astigmatism and report and save
          mSimplexEngine->fitWedgesForAstig(centerAngles, fitDefocus, mWedgeAngleRange,
                                            meanFocus, mNextSpecAstigmatism, 
                                            mNextSpecAstigAngle, error);

          // Fix negative astigmatism and angles outside of +/90
          if (mNextSpecAstigmatism < 0) {
            mNextSpecAstigmatism = -mNextSpecAstigmatism;
            mNextSpecAstigAngle = mNextSpecAstigAngle + 
              B3DCHOICE(mNextSpecAstigAngle < 0, 90., - 90.);
          }
          if (mNextSpecAstigAngle <= -90)
            mNextSpecAstigAngle += 180.;
          if (mNextSpecAstigAngle > 90.)
            mNextSpecAstigAngle -= 180.;

          printf("Wedgefit f1=%.3f  f2=%.3f  mean=%.3f  astig=%.3f  ang=%.1f  "
                 "error=%.4f\n", meanFocus + mNextSpecAstigmatism / 2., 
                 meanFocus - mNextSpecAstigmatism / 2., meanFocus, mNextSpecAstigmatism,
                 mNextSpecAstigAngle, error);
          if (mNextSpecAstigmatism == 0.)
            mNextSpecAstigmatism = 0.00001;
          if (debugLevel > 2)
            mSimplexEngine->outputWedgeFitVals();
          astigmatisms[astigLoop] = mLastSpecAstigmatism = mNextSpecAstigmatism;
          mLastSpecAstigAngle = mNextSpecAstigAngle;
          mLastWedgeFitError = mSimplexEngine->getLastError();

          // On first iteration, evaluate whether the astigmatism is big enough do that
          // any of the wedge spectra blur the highest zero being fit too much
          if (!astigLoop) {

            // Get the min and max defocus of each wedge and the maximum range
            angleInc = 180. / mNumSectors;
            for (wedge = 0; wedge < centerAngles.size(); wedge++) {
              mCache->getWedgeMinMaxes(centerAngles[wedge], mWedgeAngleRange, plusMin, 
                                       plusMax, minusMin, minusMax);
              defMin = 1.e20;
              defMax = -1.e20;
              for (sector = 0; sector < mNumSectors; sector++) {
                angle = (sector + 0.5) * angleInc - 90.;
                if ((angle >= plusMin && angle < plusMax) ||
                    angle >= minusMin && angle < minusMax) {
                  defocus = mWedgeFitCenterDefocus + 0.5 * mNextSpecAstigmatism * 
                    cos(2. * (angle - mNextSpecAstigAngle) * RADIANS_PER_DEGREE);
                  ACCUM_MIN(defMin, defocus);
                  ACCUM_MAX(defMax, defocus);
                }
              }
              ACCUM_MAX(maxRange, defMax - defMin);
            }

            // Tolerance for zero shift of 0.2 times interzero distance
            astigTol = mDefocusFinder.findTolerance
              (mX2Index2 / (mDim - 1.), 0.20, mWedgeFitCenterDefocus, nextFitPhaseShift,
               nextFitCutOnFreq);

            // Break iteration loop if the wedges are all OK
            if (maxRange < astigTol &&
                !(!astigLoop && maxRange > 0.05 * mWedgeFitCenterDefocus && 
                  mBaselineOrder > 0))
              break;

            // Limit astigmatism for next round to 1 micron because this initial estimate
            // can really mushroom and then oscillate badly
            mLastSpecAstigmatism = B3DMIN(mNextSpecAstigmatism, 1.0);
          } else {

            // Later iterations: terminate if the change is below threshold, or if the
            // change is BIGGER than last time which would be bad
            if (fabs(astigmatisms[astigLoop] - astigmatisms[astigLoop - 1]) < 
                minAstigChange)
              break;
            if (astigLoop > 1 && 
                fabs(astigmatisms[astigLoop] - astigmatisms[astigLoop - 1]) > 
                fabs(astigmatisms[astigLoop -1] - astigmatisms[astigLoop - 2]))
              break;
          }
        } // end of astig iteration loop
          
        // Restore everything from astig fitting
        mSimplexEngine->setDefocusRange(0.);
        mNextSpecWedgeNum = -1;
        if (!mDoingAutofit)
          manageEnablesForMultipleFits(false);
        if (!mSaveAndExit)
          QApplication::restoreOverrideCursor();
      } // End of CTF-like fitting

      // Save if it was done for all views
      if (mMinViewsForAstig >= mNzz && mNzz > 1) {
        mAllViewsAstigmatism = mNextSpecAstigmatism;
        mAllViewsAstigAngle = mNextSpecAstigAngle;
      }
    } // End of using all views value or computing astig

    // Set parameters for spectrum/fits solving for phase
    nextSpecAstig = mNextSpecAstigmatism;
    nextSpecAngle = mNextSpecAstigAngle;
    nextFitAstig = mNextFitAstigmatism;
    nextFitAngle = mNextFitAstigAngle;
    nextFitKnowAstig = mNextFitKnowAstig;
  } // END OF FINDING ASTIGMATISM

  /*
   * FINDING PHASE FROM WIDER RANGE THAN DEFOCUS NEEDS
   */ 
  if (findPhase) {

    // Set up different range if needed
    if (widerRange != mMinViewsForPhase) {
      setWiderAngleRange(midAngle, mMinViewsForPhase);
      widerRange = mMinViewsForPhase;
      setupNextSpecDefocuses();
      nextEffectiveDefocus = mNextSpecEffectiveDefocus;
    } 
    
    // Get spectrum
    mNextSpecEffectiveDefocus = nextEffectiveDefocus;
    doComputePS(mInitialCentralTiles, false, false);
    
    // Ctffind
    if (useCtffind) {
    
        // Set up and solve for phase
        setupCtffindParams(params, nextFitKnowAstig, nextFitAstig, nextFitAngle, false, 
                           mDefocusFinder.getPlatePhase());
        if (!ctffind(params, mAmpSpecSum, params.box_size + 2, resultsArray, NULL, NULL, 
                     NULL, numPoints, lastBinFreq)) {
          printf("Error in ctffind\n");
          return 1;
        }
        mNextFitPhaseShift = resultsArray[3];
        printf("Phasefit focus=%.3f  phase=%.4f  score=%.4f  "
               "fit to %.1f  alias %.1f\n", (resultsArray[0] + resultsArray[1]) / 2.e4,
               mNextFitPhaseShift, resultsArray[4], resultsArray[5], resultsArray[6]);

    } else {
      
      // CTF-like fitting
      plotFitPS(false);
      mNextFitPhaseShift = mLastFitPhaseShift;
      mNextFitCutOnFreq = mLastFitCutOnFreq;
    }
  }

  // Restore the angle setup and compute spectrum again with all conditions set
  mExcludingSkipList = excludeSave;
  if (widerRange > 0) {
    if (debugLevel > 0)
      printf("restoring range to %d\n", numForDefocus);
    mCache->whatIsNeeded(mLowAngle, mHighAngle, mStartingSlice, mEndingSlice);
  }

  if (debugLevel >= 2) wallStart = wallTime();
  mNextSpecAstigmatism = nextSpecAstig;
  mNextSpecAstigAngle = nextSpecAngle;
  doComputePS(mInitialCentralTiles, false, false);
  if (debugLevel >= 2) specCum += wallTime() - wallStart;
  if (debugLevel >= 2)
    printf("Total spectrum time %.4f  fit/plot time %.4f\n", specCum, plotCum);
  
  // Do the fit and signal that everything is done in return
  mNextFitKnowAstig = nextFitKnowAstig;
  mNextFitKnowPhase = findPhase;
  mNextFitAstigmatism = nextFitAstig;
  mNextFitAstigAngle = nextFitAngle;
  plotFitPS(false);
  return -1;
}

/*
 * Set the angle range higher or different for a set of views, with unconditional
 * exclusion of the skip list
 */
void MyApp::setWiderAngleRange(double midAngle, int minViews)
{
  int lowSort, highSort, lowView, highView;
  double lowAngle, highAngle;
  mExcludingSkipList = true;
  if (debugLevel > 0)
    printf("setting wider range to %d\n", minViews);

  getRangesFromMidAngle(minViews, 0, midAngle, lowAngle, highAngle, lowSort, highSort, 
                        lowView, highView);
  
  // Set up the wider range for the spectra
  mCache->whatIsNeeded(lowAngle, highAngle, mStartingSlice, mEndingSlice);
}
  
/*
 * If dialogs are open, call their enable-managing functions
 */
void MyApp::manageEnablesForMultipleFits(bool fitting)
{
  if (!mSaveAndExit) {
    if (mPlotter->mAngleDia)
      mPlotter->mAngleDia->manageEnablesForAutofit(fitting);
    if (mPlotter->mFittingDia)
      mPlotter->mFittingDia->manageEnablesForAutofit(fitting);
    qApp->processEvents();
  }
}


/*
 * Does multiple fits to ranges of size rangeSize, at intervals of rangeStep,
 * for all ranges that fit between minAngle and maxAngle.  The fit is iterated
 * numIter times, with the current defocus estimate used after the first iteration.
 */
int MyApp::autoFitToRanges(float minAngle, float maxAngle, int rangeStep, int numIter)
{
  int tSize, numSteps, i, step, autoRange, sortInd = 0;
  int  indLo, indHi, minDelInd, maxDelInd, numDel, rangeSize, lowNear, highNear, midStep;
  int startStep, endStep, loop, numLoops, interval, remainder;
  float eps = mNoAnglesEntered ? 0.002f : 0.02f;
  double expDefocus = mDefocusFinder.getExpDefocus();
  int saveUseDefocus = mUseCurDefocus;
  int angleDir = 0;
  bool saveUsePhase = mUseCurrentPhase;
  SavedDefocus *item;
  double defTol, axisAngle, leftTol, rightTol, cropPixel;
  double trueStep, minDel, maxDel, mid, minMid, maxMid, midAngle, bestStart;
  double firstDefocus, firstPhase, firstCuton;
  std::vector<float> lowAngles, highAngles;
  bool tolOk = true;
  bool removeAll = false;

  // Determine current middle angle for figuring out where to start
  midAngle = getMidAngleAndRangeIndices(mLowAngle, mHighAngle, lowNear, highNear,
                                        autoRange);

  // Quick way to find the indices of start and end of autofit range and # views there
  getMidAngleAndRangeIndices(minAngle, maxAngle, lowNear, highNear, autoRange);

  if (mPlotter) {
    tolOk = mPlotter->mAngleDia->getTileTolerances(defTol, tSize, axisAngle, leftTol, 
                                                   rightTol, cropPixel);
  } else {
    defTol = mDefocusTol;
    tSize = mTileSize;
    axisAngle = mTiltAxisAngle;
    leftTol = mLeftDefTol;
    rightTol = mRightDefTol;
    cropPixel = mCropPixelSizeInDia;
  }

  if (!tolOk)
    return 1;

  // Determine how many ranges to do
  rangeSize = mNumViewsInRange;
  if (mFitSingleViews) {
    rangeStep = 1;
    rangeSize = 1;
  }

  // Make sure these nearest angles are actually in the range in the text boxes, with a
  // little more tolerance
  if (lowNear != highNear) {
    angleDir = highNear > lowNear ? 1 : -1;
    if (mSortedAngles[lowNear] + 0.2 < minAngle && lowNear + angleDir >= 0 &&
        lowNear + angleDir < mNumSortedAngles) {
      lowNear += angleDir;
      autoRange--;
    }
    if (mSortedAngles[highNear] - 0.2 > maxAngle && highNear - angleDir >= 0 &&
        highNear - angleDir < mNumSortedAngles) {
      highNear -= angleDir;
      autoRange--;
    }
  }
  
  numSteps = (autoRange - rangeSize + rangeStep - 1) / rangeStep + 1;
  PRINT3(rangeStep, rangeSize, numSteps);

  minDelInd = lowNear + rangeSize / 4;
  B3DCLAMP(minDelInd, 0, mNumSortedAngles - 1);
  maxDelInd = highNear - rangeSize / 4;
  B3DCLAMP(maxDelInd, 0, mNumSortedAngles - 1);
  minDel = mSortedAngles[minDelInd] - eps;
  maxDel = mSortedAngles[maxDelInd] + eps;
  if (minDelInd == 0 && maxDelInd == mNumSortedAngles - 1) {
    minDel = mMinAngle - eps;
    maxDel = mMaxAngle + eps;
    removeAll = true;
  }

  if (numSteps < 1)
    return 2;

  // Remove existing items from the table - first count them
  numDel = 0;
  minMid = 100000.;
  maxMid = -100000.;
  for (i = 0; i < ilistSize(mSaved); i++) {
    item = (SavedDefocus *)ilistItem(mSaved, i);
    mid = (item->hAngle + item->lAngle) / 2.;
    if (mid >= minDel && mid <= maxDel) {
      numDel++;
      minMid = B3DMIN(minMid, mid);
      maxMid = B3DMAX(maxMid, mid);

    }
  }

  // Confirm and remove
  if (ilistSize(mSaved))
    mSavedCopy = ilistDup(mSaved);
  if (numDel > 0) {
    if (mSaveAndExit) {
      i = QMessageBox::Yes;
    } else {
      QString str;
      if (removeAll)
        str = "You are autofitting to the entire range.\nDo you want to remove all "
          "existing entries in the table?";
      else
        str.sprintf("Do you want to remove existing entries in the \n"
                    "table with midpoint tilt angles from %.2f to %.2f degrees?", minMid,
                    maxMid);
      i = QMessageBox::question(NULL, QString("Ctfplotter"), str,
                                QMessageBox::Yes | QMessageBox::No | QMessageBox::Cancel,
                                QMessageBox::Yes);
    }
    if (i == QMessageBox::Cancel) {
      ilistDelete(mSavedCopy);
      mSavedCopy = NULL;
      return 0;
    }
    if (i == QMessageBox::Yes) {
      for (i = 0; i < ilistSize(mSaved); i++) {
        item = (SavedDefocus *)ilistItem(mSaved, i);
        mid = (item->hAngle + item->lAngle) / 2.;
        if (mid >= minDel && mid <= maxDel)
          ilistRemove(mSaved, i--);
      }
    }
  }

  // Loop on the ranges to make a list of steps
  indLo = lowNear;
  minMid = 1000.;
  interval = (autoRange - rangeSize) / B3DMAX(1, numSteps - 1);
  remainder = (autoRange - rangeSize) % B3DMAX(1, numSteps - 1);
  bestStart = 0.;
  PRINT2(interval, remainder);
  if (midAngle >= minAngle - eps && midAngle <= maxAngle + eps)
    bestStart = midAngle;
  PRINT4(minAngle, maxAngle, midAngle, bestStart);
  for (step = 0; step < numSteps; step++) {
    indHi = indLo + rangeSize - 1;
    lowAngles.push_back(mSortedAngles[indLo]);
    highAngles.push_back(mSortedAngles[indHi]);
    PRINT4(indLo, indHi, mSortedAngles[indLo], mSortedAngles[indHi]);
    mid = fabs((mSortedAngles[indLo] + mSortedAngles[indHi]) / 2 - bestStart);
    if (mid < minMid) {
      minMid = mid;
      midStep = step;
    }

    // Advance the range
    indLo += interval + (step < remainder ? 1 : 0);
  }

  // Set up for one loop, do two loops if either "Use current" was on, starting from
  // current or middle location
  numLoops = 1;
  startStep = 0;
  endStep = numSteps - 1;
  mStepDir = 1;
  if (mUseCurDefocus || mUseCurrentPhase) {
    numLoops = 2;
    startStep = midStep;
    endStep = 0;
    mStepDir = -1;
  }

  // Do the one or two loops
  mStopAutofitting = false;
  mDoingAutofit = true;
  manageEnablesForMultipleFits(true);
  for (loop = 0; loop < numLoops; loop ++) {

    // Do the steps in the loop
    for (step = startStep; mStepDir * step <= mStepDir * endStep; step += mStepDir) {
      PRINT2(lowAngles[step], highAngles[step]);
      // Iterate spectrum and fitting
      for (mAutofitIteration = 0; mAutofitIteration < numIter; mAutofitIteration++) {
        angleChanged(lowAngles[step], highAngles[step], expDefocus, defTol, tSize,
                     axisAngle, leftTol, rightTol, cropPixel);
        mUseCurDefocus = 1;
        mUseCurrentPhase = true;
      }
      mAutofitIteration = 0;
      mUseCurDefocus = saveUseDefocus;
      mUseCurrentPhase = saveUsePhase;
      
      // Save in table
      saveCurrentDefocus();
      if (!mSaveAndExit) {
        qApp->processEvents();
        b3dMilliSleep(50);
      }
      if (mStopAutofitting)
        break;

      // Save the first values for focus and phase
      if (!loop && step == startStep) {
        firstDefocus = mDefocusFinder.getDefocus();
        firstPhase = mLastFitPhaseShift;
        firstCuton = mLastFitCutOnFreq;
      }
    }

    // Go forward for second loop
    startStep = midStep + 1;
    endStep = numSteps - 1;
    mStepDir = 1;

    // Restore first values if their respective Use options were selected
    if (saveUseDefocus)
      mDefocusFinder.setDefocus(firstDefocus);
    if (saveUsePhase) {
      mLastFitPhaseShift = firstPhase;
      mLastFitCutOnFreq = firstCuton;
    }
  }
  mDoingAutofit = false;
  manageEnablesForMultipleFits(false);
  ilistDelete(mSavedCopy);
  mSavedCopy = NULL;
  return 0;
}

/*
 * Set whether to use central tiles only initially
 */
void MyApp::setInitialCentralTiles(bool state)
{
  if (mPlotter)
    mPlotter->mTileButton->setEnabled(state);
  mInitialCentralTiles = state;
  mAllViewsAstigmatism = 0.;
}

/*
 * Determine the noise images to interpolate between and compute the noise PS for
 * the given mean level, or just set the noise PS to 1 when doing noise files
 */
void MyApp::setNoiseForMean(double mean)
{
  int i, j, highInd = mNumNoiseFiles - 1;
  double lowMean, highMean;
  double *lowPs, *highPs;

  if (!mNumNoiseFiles || mNoNoiseFiles) {
    for (i = 0; i < mDim; i++)
      mNoisePS[i] = 1.;
    return;
  }
    
  for (i = 1; i < mNumNoiseFiles; i++) {
    if (mNoiseMeans[i] > mean) {
      highInd = i;
      break;
    }
  }
  lowMean = mNoiseMeans[highInd - 1];
  highMean = mNoiseMeans[highInd];
  //printf("For mean %f  noise index %d noise means %f %f\n", mean, highInd-1,
  //  lowMean, highMean);
  lowPs = mAllNoisePS + mNoiseIndexes[highInd - 1] * mDim;
  highPs = mAllNoisePS + mNoiseIndexes[highInd] * mDim;

  // 12/29/14: Linear interpolation/extrapolation is OK; the relation is indeed linear
  // for CCD and DDD, but for counting cameras there may be curvature
  for (i = 0; i < mDim; i++) {
    mNoisePS[i] = lowPs[i] + (highPs[i] - lowPs[i]) * (mean - lowMean) /
      (highMean - lowMean);

    // If extrapolation gave a negative result, replace the whole spectrum with one 
    // the nearest one
    if (mNoisePS[i] <= 0) {
      if (mean < lowMean)
        for (j = 0; j < mDim; j++)
          mNoisePS[i] = lowPs[i];
      else
        for (j = 0; j < mDim; j++)
          mNoisePS[i] = highPs[i];
    }
  }
}

/*
 * Save the current defocus and angular range in the list and display in table
 */
void MyApp::saveCurrentDefocus()
{
  int newInd;
  SavedDefocus toSave;

  toSave.startingSlice = getStartingSliceNum();
  toSave.endingSlice = getEndingSliceNum();
  toSave.lAngle = getLowAngle();
  toSave.hAngle = getHighAngle();
  toSave.defocus = mDefocusFinder.getDefocus();
  toSave.defocus2 = 0.;
  toSave.astigAngle = 0.;
  toSave.platePhase = mDefocusFinder.getPlatePhase();
  toSave.cutOnFreq = mDefocusFinder.getCutOnFreq();
  if (mCurrentAstigmatism != 0.) {
    toSave.defocus += mCurrentAstigmatism / 2.;
    toSave.defocus2 = toSave.defocus - mCurrentAstigmatism;
    toSave.astigAngle = mCurrentAstigAngle;
  }
  if (mLastFitPhaseShift > PHASE_TEST) {
    toSave.platePhase = mLastFitPhaseShift;
  }
  if (mLastFitCutOnFreq > PHASE_TEST) {
    toSave.cutOnFreq = mLastFitCutOnFreq;
  }
  newInd = addItemToDefocusList(mSaved, toSave);
  mSaveModified = true;
  if (mPlotter && mPlotter->mAngleDia)
    mPlotter->mAngleDia->updateTable(newInd);
}

/*
 * Returns 1 if there are any astigmatism values in the table and 2 if they vary, and
 * for phase and cuton, returns 0 if all zero, 1 if all match current expected value,
 * 2 if all match another value, or 3 if they vary
 */
int MyApp::anyAstigmatismSaved(int &phase, int &cuton)
{
  SavedDefocus *item;
  int astig = 0;
  double expPhase = mDefocusFinder.getPlatePhase();
  double expCuton = mDefocusFinder.getCutOnFreq();
  double firstPhase = 0., firstCuton = 0., tolerance = 0.00001, firstAstig = -1.;
  double firstAxis = 0.;
  int ind, val;
  phase = 0;
  cuton = 0;
  
  if (!ilistSize(mSaved))
    return false;
  for (int i = 0; i < ilistSize(mSaved); i++) {
    item = (SavedDefocus *)ilistItem(mSaved, i);

    // test astigmatism
    if (item->defocus2) {
      if (firstAstig < 0.) {
        astig = 1;
        firstAstig = item->defocus - item->defocus2;
        firstAxis = item->astigAngle;
      } else if (fabs(firstAstig - (item->defocus - item->defocus2)) > 0.002 ||
                 fabs(firstAxis - item->astigAngle) > 0.001) {
        astig = 2;
      }
    }

    // test phase
    if (item->platePhase) {
      val = 1;
      if (fabs(item->platePhase - expPhase) > tolerance)
        val = 2;
      if ((firstPhase || i == ilistSize(mSaved) - 1) 
          && fabs(item->platePhase - firstPhase) > tolerance)
        val = 3;
      phase = B3DMAX(val, phase);
      if (!firstPhase)
        firstPhase = item->platePhase;
    }

    // test cut-on freq
    if (item->cutOnFreq) {
      val = 1;
      if (fabs(item->cutOnFreq - expCuton) > tolerance) 
        val = 2;
      if ((firstCuton || i == ilistSize(mSaved) - 1) && 
          fabs(item->cutOnFreq - firstCuton) > tolerance)
        val= 3;
      cuton = B3DMAX(val, cuton);
      if (!firstCuton)
        firstCuton = item->cutOnFreq;
    }
    if (astig > 1 && cuton == 3 && phase == 3)
      return astig;
  }
  return astig;
}

/*
 * Write out the defocus file or a temporary file for graphing
 */
void MyApp::writeDefocusFile()
{
  writeDefocusOrTempFile(false);
}

int MyApp::writeDefocusOrTempFile(bool ifTemp)
{
  SavedDefocus *item;
  FILE *fp;
  QString qname = mFnDefocus;
  int i, flags, phaseVary, cutonVary;
  bool hasPhase, hasCuton, hasAstig = anyAstigmatismSaved(phaseVary, cutonVary) > 0;
  if (!ilistSize(mSaved))
    return 1;
  hasPhase = phaseVary > 0;
  hasCuton = cutonVary > 0;
  if (!mBackedUp && !ifTemp) {
    imodBackupFile(qname.toLatin1());
    mBackedUp = true;
  }
  if (ifTemp)
    qname.sprintf("ctfplotter.temp.%d", imodGetpid());
  fp = fopen((const char *)qname.toLatin1(), "w");
  if (!fp) {
    if (mSaveAndExit)
      exitError("Cannot open output file for saving defocus values");
    QMessageBox::critical(NULL, "Ctfplotter: File save error",
                          "Can not open output file");
    return 1;
  }

  // Write a version 3 file with flags entry to indicate phase shifts if there is
  // a fixed value being used
  if (!hasPhase)
    hasPhase = mDefocusFinder.getPlatePhase() != 0.;
  if (!hasCuton && hasPhase)
    hasCuton = mDefocusFinder.getCutOnFreq() != 0.;
  if (hasPhase || hasAstig) {
    mDefVersionOut = 3;
    flags = hasAstig ? DEF_FILE_HAS_ASTIG : 0;
    if (hasPhase) {
      flags += DEF_FILE_HAS_PHASE;
      if (hasCuton)
        flags += DEF_FILE_HAS_CUT_ON;
    }
    fprintf(fp, "%d\t0\t0.0\t0.0\t0.0\t%d\n", flags, mDefVersionOut);
    for (i = 0; i < ilistSize(mSaved); i++) {
      item = (SavedDefocus *)ilistItem(mSaved, i);
      if (hasPhase && hasAstig && hasCuton)
        fprintf(fp, "%d\t%d\t%5.2f\t%5.2f\t%7.1f\t%7.1f\t%7.2f\t%9.2f\t%9.4f\n", 
                item->startingSlice + 1, item->endingSlice + 1, item->lAngle,
                item->hAngle, item->defocus * 1000, item->defocus2 * 1000,
                item->astigAngle, item->platePhase / RADIANS_PER_DEGREE, item->cutOnFreq);
      else if (hasPhase && hasAstig)
        fprintf(fp, "%d\t%d\t%5.2f\t%5.2f\t%7.1f\t%7.1f\t%7.2f\t%9.2f\n", 
                item->startingSlice + 1, item->endingSlice + 1, item->lAngle,
                item->hAngle, item->defocus * 1000, item->defocus2 * 1000,
                item->astigAngle, item->platePhase / RADIANS_PER_DEGREE);
      else if (hasPhase && hasCuton)
        fprintf(fp, "%d\t%d\t%5.2f\t%5.2f\t%7.1f\t%9.2f\t%9.4f\n", 
                item->startingSlice + 1, item->endingSlice + 1, item->lAngle,
                item->hAngle, item->defocus * 1000, item->platePhase / RADIANS_PER_DEGREE,
                item->cutOnFreq);
      else if (hasPhase)
        fprintf(fp, "%d\t%d\t%5.2f\t%5.2f\t%7.1f\t%9.2f\n", item->startingSlice + 1,
                item->endingSlice + 1, item->lAngle, item->hAngle,
                item->defocus * 1000, item->platePhase / RADIANS_PER_DEGREE);
      else
        fprintf(fp, "%d\t%d\t%5.2f\t%5.2f\t%7.1f\t%7.1f\t%7.2f\n", 
                item->startingSlice + 1, item->endingSlice + 1, item->lAngle,
                item->hAngle, item->defocus * 1000, item->defocus2 * 1000,
                item->astigAngle);
    }
  } else {

    // Otherwise write the regular version 2 file
    for (i = 0; i < ilistSize(mSaved); i++) {
      item = (SavedDefocus *)ilistItem(mSaved, i);
      fprintf(fp, "%d\t%d\t%5.2f\t%5.2f\t%6.0f", item->startingSlice + 1,
              item->endingSlice + 1, item->lAngle, item->hAngle,
              item->defocus * 1000);
      if (i)
        fprintf(fp, "\n");
      else
        fprintf(fp, "   %d\n", mDefVersionOut);
    }
  }
  fclose(fp);
  if (ifTemp)
    mTempFileToRemove = qname;
  else
    mSaveModified = false;
  return 0;
  //saveAllPs();
}

/*
 * Put up graphs of all of the values in the table
 */
void MyApp::graphTableValues()
{
  QString command;
  QDir dir;
  bool hasPhase, hasCuton, hasAstig;
  int types[5];
  int ind, phaseVary, cutonVary, numTypes = 1;
  int xsize = 640, ysize = 512;
  int xpos[5] = {2, 640, 640, 1280, 1280};
  int ypos[5] = {10, 10, 550, 10, 550};
  struct tm *tmp;
  time_t time_tval;
  const int timeLen = 12;
  char timeBuf[timeLen];
  hasAstig = anyAstigmatismSaved(phaseVary, cutonVary) > 1;
  hasPhase = phaseVary > 2;
  hasCuton = cutonVary > 2;
  types[0] = 15;
  if (hasAstig) {
    types[numTypes++] = 16;
    types[numTypes++] = 17;
  }
  if (hasPhase) {
    types[numTypes++] = 18;
    if (hasCuton)
      types[numTypes++] = 19;
  }
  if (writeDefocusOrTempFile(true))
    return;
  time_tval = time(NULL);
  tmp = localtime(&time_tval);
  strftime(timeBuf, timeLen, "%H:%M:%S", tmp);
  for (ind = 0; ind < numTypes; ind++) {
    command.sprintf("tomodataplots -back -type %d -append %s -position %d,%d -size %d,%d "
                    "%s", types[ind], timeBuf, xpos[ind], ypos[ind], xsize, ysize,
                    (const char *)mTempFileToRemove.toLatin1());
    system(command.toLatin1());
  }
  dir.remove(mTempFileToRemove);
}


/*
 * Function to print a warning or put up a message box
 */
void MyApp::showWarning(const char *title, const char *message)
{
  if (mSaveAndExit) {
    printf("%s.  %s\n\n", title, message);
  } else {
    QMessageBox::warning(0, title, message, QMessageBox::Ok, QMessageBox::Ok);
    qApp->processEvents();
  }
}

/*
 * Combine primary and secondary power spectra for focal pair processing.
 * We are combining power spectra which may have different baselines. 
 * All we really care about is the frequency of the minima, so one 
 * approach is to compute their geometric mean, which is equivalent to 
 * averaging log plots of the spectra. We also have to handle cases
 * where one or both datasets are empty (0) over part of the range.
 */
void MyApp::combinePowerSpectra()
{
  double baseline = 0;
  int i;

  for (i = 0; i < mDim; i++) {
    if (mRAverage1[i] == 0 && mRAverage2[i] != 0) {
      mRAverage[i] = mRAverage2[i];
    }
    else {
      if (mRAverage1[i] != 0 && mRAverage2[i] == 0) {
        mRAverage[i] = mRAverage1[i];
      }
      else {
	mRAverage[i] = sqrt(mRAverage1[i] * mRAverage2[i]);        
      }
    }
    
    // average of 2nd half of combined power spectrum
    if (i >= mDim / 2)
      baseline += mRAverage[i];
  }

  // Adjust baseline to approximately 0 on a log scale
  baseline /= 0.5 * mDim;
  for (i = 0; i < mDim; i++)
    mRAverage[i] = mRAverage1[i] / baseline;
}

/*
 * Convenience function to return the starting defocus for a fit and the corresponding
 * first and second zeros, with phase and cuton taken into account
 */
double MyApp::getStartingDefocusAndZeros(double &zero1, double &zero2, double *allZeros, 
                                         int maxZeros)
{
  double startDef, phase, cuton;
  int numPast1 = 0;
  if (mNextSpecWedgeNum >= 0)
    startDef = mNextFitStartDefocus > 0. ? mNextFitStartDefocus : mWedgeFitCenterDefocus;
  else if (mUseCurDefocus)
    startDef = mDefocusFinder.getDefocus();
  else
    startDef = mDefocusFinder.getExpDefocus();
  phase = mNextFitPhaseShift;
  if (phase < PHASE_TEST && mUseCurrentPhase)
    phase = mLastFitPhaseShift;
  cuton = mNextFitCutOnFreq;
  if (cuton < PHASE_TEST && mUseCurrentPhase)
    cuton = mLastFitCutOnFreq;
  mDefocusFinder.getTwoZeros(startDef, zero1, zero2, phase, cuton);
  if (maxZeros > 2 && allZeros) {
    allZeros[0] = zero1;
    allZeros[1] = zero2;
    for (int ind = 2; ind < maxZeros; ind++) {
      allZeros[ind] = mDefocusFinder.getAnyOneZero(startDef, ind, phase, cuton);
      if (allZeros[ind] > 1.)
        numPast1++;
      if (numPast1 > 2)
        break;
    }
  }
  return startDef;
}

/*
 * Smooth the noise spectrum with iterative polynomial fitting, it is more predictable 
 * than spline smoothing which I think I tried...
 */
int MyApp::smoothNoisePS()
{
  int indStart, indEnd, ind, ifit, del;
  int nfit = B3DMIN(9, mDim);
  int order = 2;
  int firstInd = 2;
  float xx[32], yy[32], slopes[5], intercept;
  double *smooth = B3DMALLOC(double, mDim);
  float *work = B3DMALLOC(float,  (order + 1) * (order + 3 + nfit));
  if (!smooth || !work)
    exitError("Array allocation failed");
  for (ifit = firstInd; ifit < mDim; ifit++) {
    indStart = B3DMAX(firstInd, ifit - nfit / 2);
    indEnd = B3DMIN(mDim - 1, indStart + nfit - 1);
    indStart = indEnd - (nfit - 1);
    for (ind = indStart; ind <= indEnd; ind++) {
      del = ind - indStart;
      xx[del] = del;
      yy[del] = log(mRAverage[ind]);
    }
    if (polynomialFit(xx, yy, nfit, order, slopes, &intercept, work)) {
      free(work);
      free(smooth);
      return 1;
    }
    smooth[ifit] = intercept;
    for (ind = 0; ind < order; ind++)
      smooth[ifit] += slopes[ind] * pow((double)(ifit - indStart), ind + 1.);
  }
  for (ifit = firstInd; ifit < mDim; ifit++)
    mRAverage[ifit] = exp(smooth[ifit]);
  free(work);
  free(smooth);
  return 0;
}

/*
 * Fit two lines to the power spectrum, with or without a prior noise division, and 
 * find the place where there is the best break between initial and final slopes
 */
void MyApp::findBaselinePastBreak(double *average, double *baseline)
{
  float *xx = B3DMALLOC(float, mDim);
  float *yy = B3DMALLOC(float, mDim);
  double firstZero, secondZero;
  float diff, maxDiff, slopeLeft, slopeRight, intcpLeft, intcpRight, ro;
  float leftAngle, rightAngle, bestSlope, bestIntcp;
  int numLeft = 4, ind, center, searchStart, searchEnd, fitStart = 3, fitEnd = mDim - 4;
  int start, bestCenter;
  for (ind = 0; ind < mDim; ind++) {
    xx[ind] = ind / (mDim - 1.);
    yy[ind] = average[ind];
  }
  getStartingDefocusAndZeros(firstZero, secondZero);
  searchStart = B3DNINT(0.67 * firstZero * (mDim - 1));
  searchEnd = B3DNINT(1.33 * firstZero * (mDim - 1));
  searchStart = B3DMAX(fitStart + 3, searchStart);
  searchEnd = B3DMIN(fitEnd - 3, searchEnd);
  for (center = searchStart; center <= searchEnd; center++) {
    start = B3DMAX(fitStart, center + 1 - numLeft);
    lsFit(&xx[start], &yy[start], center + 1 - start, &slopeLeft, &intcpLeft, 
          &ro);
    lsFit(&xx[center], &yy[center], fitEnd + 1 - center, &slopeRight, &intcpRight, &ro);
    leftAngle = atan(slopeLeft) / RADIANS_PER_DEGREE;
    rightAngle = atan(slopeRight) / RADIANS_PER_DEGREE;
    diff = rightAngle - leftAngle;
    /*printf("%.3f  %.3f  %.3f  %.1f %.1f  %.1f\n", center / (2. *(mDim - 1.)), slopeLeft,
      slopeRight, leftAngle, rightAngle, diff);*/

    // Save slope and intercept of right-hand fit for a bigger slope change
    if (center == searchStart || diff > maxDiff) {
      maxDiff = diff;
      bestSlope = slopeRight;
      bestIntcp = intcpRight;
      bestCenter = center;
    }
  }
  if (debugLevel > 1)
    printf("Best break at %.3f, slope=%f intercept=%f\n", bestCenter / (2. *(mDim - 1.)),
           bestSlope, bestIntcp);
  
  // Compute baseline as just the right-hand line
  for (ind = 0; ind < mDim; ind++)
    baseline[ind] = bestSlope * xx[ind] + bestIntcp;
  free(xx);
  free(yy);
}

/*
 * Find the index in the given angle list of the angle closest to "angle"
 */
int MyApp::findNearestAngleIndex(float angle, float *angleList, int numAngles)
{
  int ind, nearInd = 0;
  for (ind = 0; ind < numAngles; ind++)
    if (fabs(angle - angleList[ind]) < fabs(angle - angleList[nearInd]))
      nearInd = ind;
  return nearInd;
}

/*
 * Prepares and returns pointer to sorted tilt angles based on current value of
 * mExcludingSkipList, overriden by whichWay (< 0 to not exclude, > 0 to exclude anyway)
 * returns number in list in numSorted
 */
float *MyApp::getSortedAngles(int whichWay, int *numSorted)
{
  int ind;
  bool excludeSkip = mExcludingSkipList;
  if (whichWay)
    excludeSkip = whichWay > 0;
  mNumSortedAngles = 0;
  for (ind = 0; ind < mNzz; ind++)
    if (!numberInList(ind + 1, excludeSkip ? mViewSkipList : NULL, mNumViewsToSkip, 0))
      mSortedAngles[mNumSortedAngles++] = mTiltAngles[ind];
  
  if (mNumSortedAngles)
    rsSortFloats(mSortedAngles, mNumSortedAngles);
  if (numSorted)
    *numSorted = mNumSortedAngles;
  return &mSortedAngles[0];
}

/*
 * Given a low and high angle for a range, find the nearest angles to these and return 
 * their indexes in the sorted angle list, the number of views in range, and the middle 
 * actual angle of the range, below the midpoint angle for an even number of views
 */
double MyApp::getMidAngleAndRangeIndices(double lowAngle, double highAngle, int &lowNear,
                                         int &highNear, int &numViews)
{
  int ind;
  double temp = B3DMIN(lowAngle, highAngle);
  highAngle = B3DMAX(lowAngle, highAngle);
  getSortedAngles(0);
  lowAngle = temp;
  lowNear = 0;
  highNear = mNumSortedAngles - 1;
  lowNear = findNearestAngleIndex(lowAngle, mSortedAngles, mNumSortedAngles);
  highNear = findNearestAngleIndex(highAngle, mSortedAngles, mNumSortedAngles);
  numViews = B3DABS(highNear - lowNear) + 1;
  return mSortedAngles[highNear - numViews / 2];
}

/*
 * Given a number of views in range, a possible number of views to step the range, and
 * the current actual middle angle, makes the step as far as possible and returns new 
 * mid angle, and low and high angles, their indexes in the sorted angle list, and their 
 * indexes in the original angle list (view numbers, but numbered from 0)
 */
void MyApp::getRangesFromMidAngle(int numViews, int rangeStep, double &midAngle, 
                                  double &lowAngle, double &highAngle, int &lowSortInd,
                                  int &highSortInd, int &lowViewInd, int &highViewInd)
{
  int oldMidInd, newMidInd, breakSortInd, firstLimit, secondLimit, lowLimit, highLimit;
  getSortedAngles(0, &mNumSortedAngles);

  // Find sorted index closest to the middle angle
  oldMidInd = findNearestAngleIndex(midAngle, mSortedAngles, mNumSortedAngles);

  // Make the step
  newMidInd = oldMidInd + rangeStep;
  B3DCLAMP(newMidInd, 0, mNumSortedAngles - 1);
  
  // Default limits of sorted indexes
  lowLimit = 0;
  highLimit = mNumSortedAngles - 1;

  // If there is a bidirectional view and we are supposed to break there, 
  // find limits of sorted indexes on the two side of the break to get the sorted index
  // of the break
  if (mBreakAtBidirView && mExcludingSkipList) {
    firstLimit = findNearestAngleIndex(mTiltAngles[mBidirectionalView - 1], 
                                              mSortedAngles, mNumSortedAngles);
    secondLimit = findNearestAngleIndex(mTiltAngles[mBidirectionalView], mSortedAngles, 
                                               mNumSortedAngles);
    breakSortInd = B3DMIN(firstLimit, secondLimit);

    // Require that there be enough views on both sides of the break to enforce it
    // If taking a step and the old range was right against the break, jump across it
    if (breakSortInd + 1 >= numViews && 
        mNumSortedAngles - (breakSortInd + 1) >= numViews) {
      if (rangeStep > 0 && newMidInd <= breakSortInd && oldMidInd + numViews / 2 ==
          breakSortInd) 
        newMidInd = breakSortInd + 1;
      else if (rangeStep < 0 && newMidInd > breakSortInd && oldMidInd + (numViews / 2) -
               (numViews - 1) == breakSortInd + 1)
        newMidInd = breakSortInd;

      // Set the limits based on which side the middle is on
      if (newMidInd <= breakSortInd)
        highLimit = breakSortInd;
      else
        lowLimit = breakSortInd + 1;
    }
  }

  // Get the proper range above that angle and clamp it to the limits
  highSortInd = newMidInd + numViews / 2;
  B3DCLAMP(highSortInd, lowLimit, highLimit);
  lowSortInd = highSortInd + 1 - numViews;
  B3DCLAMP(lowSortInd, lowLimit, highLimit);
  highSortInd = lowSortInd + numViews - 1;
  B3DCLAMP(highSortInd, lowLimit, highLimit);

  // These are the low and high angles and the new middle angle
  lowAngle = mSortedAngles[lowSortInd];
  highAngle = mSortedAngles[highSortInd];
  newMidInd = highSortInd - (highSortInd + 1 - lowSortInd) / 2;
  midAngle = mSortedAngles[newMidInd];

  // Find the corresponding view numbers
  lowViewInd = findNearestAngleIndex(lowAngle, mTiltAngles, mNzz);
  highViewInd = findNearestAngleIndex(highAngle, mTiltAngles, mNzz);
}

/*
 * Put out a message for a bad entry in a text field
 */
void MyApp::badEntryMessage(bool valOK, const QString &message, bool append)
{
  if (!valOK)
    QMessageBox::critical(NULL, "Ctfplotter: Bad character in entry", 
                          QString(append ? "There is a non-numeric character in the " : 
                                  "") + message + QString(append ? " entry" : ""));
}

/*
 * Call the fitting dialog to get its changed values set if its Apply is enabled
 */
void MyApp::ensureFitRangeUpdated()
{
  mUpdatingFitRange = true;
  if (mPlotter && mPlotter->mFittingDia)
    mPlotter->mFittingDia->updateFitRangeIfNeeded();
  mUpdatingFitRange = false;
}

/*
 * Provide indexes for the fitting range given the specified type of defocus (expected
 * or current) and the number of zeros to fit.
 */
void MyApp::getFittingRangeFromDefocus(int which, double &firstZero, int &firstZeroIndex,
                                int &secZeroIndex, int &backFromZero)
{
  double backFrac = 0.55;    // Overall best compromise from looking at 7 sets
  double secondZero, farZero, nextZero, defocus;
  int farZeroNum = (int)mNumZerosToFit;
  float fracFar = mNumZerosToFit - farZeroNum;
  defocus = which ? mDefocusFinder.getDefocus() : mDefocusFinder.getExpDefocus();
  mDefocusFinder.getTwoZeros(defocus, firstZero, secondZero);

  // Get the zeros bracketing the number to fit and interpolate
  farZero = mDefocusFinder.getAnyOneZero(defocus, farZeroNum);
  nextZero = mDefocusFinder.getAnyOneZero(defocus, farZeroNum + 1);
  secZeroIndex = B3DMIN(mDim - 1, B3DNINT((farZero + fracFar * (nextZero - farZero))
                                              * (mDim - 1)));
  firstZeroIndex = B3DMIN(secZeroIndex - 3, B3DNINT(firstZero * (mDim - 1)));
  backFromZero = B3DNINT(backFrac * (secondZero - firstZero) * (mDim - 1));
  B3DCLAMP(backFromZero, 4, 12);
}

/*
 * Evaluate whether interzero spacing gets too small within the fitting range and set up a * warning line about what value is needed
 */
void MyApp::manageCropWarning()
{
  double defocus = mUseCurDefocus ? mDefocusFinder.getDefocus() :
    mDefocusFinder.getExpDefocus();
  float minInterval = 3.5;
  float alarmFrac = 1.167;
  float zero, lastZero, newPixel, interval, addedCrop;
  QString str;
  int num, level = 0;

  if (!mPlotter || !mPlotter->mAngleDia)
    return;

  // Walk out through zeros until the end of the fitting range, keep track of interval
  // in bin units
  for (num = 1; num < mDim; num++) {
    zero = mDefocusFinder.getAnyOneZero(defocus, num) * (mDim - 1.);
    if (zero > mX2Index2)
      break;
    if (num > 1)
      interval = zero - lastZero;
    lastZero = zero;
  }
  if (num < 3) {
    mPlotter->mAngleDia->setCropWarningLabel("", 0);
    return;
  }

  // Get relative cropping needed from the current one and te implied pixel size,
  // and compose message;
  addedCrop = minInterval / interval;
  newPixel = addedCrop * getSpectrumPixelSize();
  str.sprintf("Crop to %.3f nm to have >= %.1f points/zero in %sfitting range", 
              newPixel, minInterval, addedCrop > (mDim - 1.) / mX2Index2 ? "this " : "");
  
  // Pick the alarm level
  if (newPixel > mPixelSize) {
    if (addedCrop < 1.01)
      level = 1;
    else
      level = addedCrop > alarmFrac ? 3 : 2;
  }
  mPlotter->mAngleDia->setCropWarningLabel(str, level);
}

/*
 * Make a section opening-closing button
 */
QPushButton *MyApp::diaPlusMinusButton(bool open, QWidget *parent, QBoxLayout *layout)
{
  QPushButton *button = new QPushButton(open ? "-" : "+", parent);
  int width = button->fontMetrics().width(" + ");
  button->setFixedWidth(width);
  button->setFixedHeight(width);
  button->setFocusPolicy(Qt::NoFocus);
  if (layout)
    layout->addWidget(button);
  return button;
}
