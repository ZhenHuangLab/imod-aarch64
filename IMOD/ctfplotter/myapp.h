/* myapp.h - the main class for ctfplotter, originally a QApplication
 *
 *  $Id$
 *
 */
#ifndef MYAPP_H
#define MYAPP_H

#include <QObject>
#include <QtGui>
#if QT_VERSION >= 0x050000
#include <QtWidgets>
#endif

#define NO_PHASE_VALUE -9999.    // Value when the current phase in finder should be used
#define PHASE_TEST -9990.        // Value to test against in comparisons

#include "cppdefs.h"
#include "defocusfinder.h"
#include "iimage.h"
#include "slicecache.h"
#include "ctfutils.h"
#include "ctffind.h"

class SimplexFitting;
class LinearFitting;
class Plotter;

#define MIN_ANGLE 1.0e-6  //tilt Angle less than this is treated as 0.0;
#define MY_PI 3.1415926

enum sliceCacheEnum {SLICE_CACHE_PRIMARY, SLICE_CACHE_SECONDARY};

enum zeroFitMethods {FIT_CTF_LIKE = 0, FIT_USE_CTFFIND, FIT_POLYNOMIAL, FIT_TWO_CURVES};

extern int debugLevel;
int ctfShowHelpPage(const char *page);

class MyApp : public QObject
{
  Q_OBJECT
  public:
    MyApp(int volt, double pSize, double ampRatio, float cs, char *defFn, int dim,
          int hyperRes, double focusTol, int tSize, double tAxisAngle, double lAngle,
          double hAngle, double expDefocus, double leftTol, double rightTol,
          int maxCacheSize, int invertAngles, bool doFocalPairProcessing, double fpdz,
          int baselineOrder, bool noNoise, int numSectors, float wedgeRange,
          float wedgeInterval);
    ~MyApp();

    SimplexFitting* mSimplexEngine;
    LinearFitting*  mLinearEngine;
    DefocusFinder   mDefocusFinder;
    Plotter *mPlotter;
    int getEndingSliceNum() {return mEndingSlice;}
    int getStartingSliceNum() {return mStartingSlice;}
    void plotFitPS(bool flagSetInitSetting);
    void replotWithDefocus(double defocus);
    void fitPsFindZero();
    void setPlotter( Plotter *p) {mPlotter=p;}
    void setSlice(const char *stackFile, char *angleFile, sliceCacheEnum cacheSelector,
                  bool isNoise);
    getSetMember(double, LowAngle);
    char *getStackName() {return mFnStack;}
    getSetMember(double, StackMean);
    getSetMember(double, HighAngle);
    getMember(double, DefocusTol);
    double getLeftTol() {return mLeftDefTol;}
    double getRightTol() {return mRightDefTol;}
    getMember(int, TileSize);
    double getAxisAngle() {return mTiltAxisAngle;}
    getSetMember(double, AutoFromAngle);
    getSetMember(double, AutoToAngle);
    getSetMember(int, ViewRangeStep);
    getSetMember(int, NumViewsInRange);
    setMember(float, AutoStepAngle);
    setMember(float, AutoStepRange);
    
    void setPS(double *rAvg, double *rAvg1, double *rAvg2) {
      mRAverage = rAvg; 
      mRAverage1 = rAvg1;
      mRAverage2 = rAvg2;
    }

    double* getPS() {return mRAverage;}
    //recompute mRAverage for the central strips after calling setSlice();
    void computeInitPS(bool noisePS);  
    void doComputePS(bool initialPS, bool noisePS, bool hasIncludedCentralTiles);
    int autoFitToRanges(float minAngle, float maxAngle, int rangeStep, int numIter);
    int smoothNoisePS();
    void getFittingRangeFromDefocus(int which, double &firstZero, int &firstZeroIndex,
                                    int &secZeroIndex, int &backFromZero);
    void setNoiseForMean(double mean);
    double getStartingDefocusAndZeros(double &zero1, double &zero2, 
                                      double *allZeros = NULL, int maxZeros = 0);
    getSetMember(int, NumNoiseFiles);
    void setNoiseIndexes(int *indexes) {mNoiseIndexes = indexes;};
    void setNoiseMeans(double *means) {mNoiseMeans = means;};
    void setAllNoisePS(double *ps) {mAllNoisePS = ps;};
    void setX1Range(int n1, int n2){mX1Index1=n1;mX1Index2=n2;}
    int getX1RangeLow(){return mX1Index1;}
    int getX1RangeHigh(){return mX1Index2;}
    void setX2Range(int n1, int n2){mX2Index1=n1;mX2Index2=n2;}
    int getX2RangeLow(){return mX2Index1;}
    int getX2RangeHigh(){return mX2Index2;}
    getMember(int, Dim);
    char *getDefFn(){return mFnDefocus;}
    void saveAllPs();
    int getX1Method(){return mX1MethodIndex;}
    int getX2Method(){return mX2MethodIndex;}
    getMember(int, ZeroFitMethod);
    void setZeroFitMethod(int which);
    getSetMember(int, PolynomialOrder);
    getMember(int, BaselineOrder);
    getMember(int, UseCurDefocus);
    getSetMember(bool, VaryCtfPowerInFit);
    float *getTiltAngles() {return &mTiltAngles[0];};
    float *getSortedAngles(int whichWay, int *numSorted = NULL);
    getMember(float, MinAngle);
    getMember(float, MaxAngle);
    Ilist *getSavedList() {return mSaved;};
    getMember(bool, SaveModified);
    void setSaveModified() {mSaveModified = true;};
    getSetMember(bool, SaveAndExit);
    SliceCache *getCache() {return mCache;};
    SliceCache *getCache2() {return mCache2;};
    void showWarning(const char *title, const char *message);
    getSetMember(bool, FitSingleViews);
    getMember(bool, NoNoiseFiles);
    void setNoNoiseFiles(bool state);
    getSetMember(bool, SlowInitialSearch);
    getMember(bool, FindAstigmatism);
    void setFindAstigmatism(bool state, bool doingOtherAction);
    getSetMember(bool, FindPhaseShift);
    getSetMember(bool, FindCutOnFreq);
    getMember(bool, FocalPairProcessing);
    getMember(int, MinViewsForAstig);
    getMember(int, MinViewsForPhase);
    getSetMember(float, WedgeAngleRange);
    getSetMember(float, WedgeCenterAngle);
    getMember(float, NextSpecWedgeRange);
    getSetMember(float, WedgeInterval);
    getSetMember(bool, ShowWedgePlots);
    setMember(float, MaxAstigmatism);
    setMember(bool, StopAutofitting);
    setMember(bool, SettingFromTable);
    setMember(float, LastSpecAstigmatism);
    setMember(float, LastSpecAstigAngle);
    getMember(bool, DoingAutofit);
    setMember(double, AllViewsAstigmatism);
    getMember(int, Nzz);
    getMember(double, PixelSize);
    getSetMember(double, CropPixelSizeInDia);
    getSetMember(bool, CropSpectra);
    getMember(int, RawTileSize);
    getMember(bool, DoTwoLineBaselineFit);
    void setDoTwoLineBaselineFit(bool state, bool replot);
    getSetMember(float, SecToShowWedges);
    getMember(bool, InitialCentralTiles);
    getMember(bool, SkipOnlyForAstig);
    getMember(bool, ExcludingSkipList);
    getSetMember(float, PhaseSearchRange);
    getSetMember(float, MaxCutOnFreq);
    setMember(double, LastFitCutOnFreq);
    setMember(double, LastFitPhaseShift);
    getSetMember(bool, UseCurrentPhase);
    getSetMember(bool, FindAndFitZeros);
    getSetMember(bool, BreakAtBidirView);
    getSetMember(int, BidirectionalView);
    getSetMember(bool, AllowCtffind);
    getSetMember(bool, AstigParamsOpen);
    getSetMember(bool, PhaseParamsOpen);
    getSetMember(bool, ChangeColors);
    getSetMember(float, NumZerosToFit);
    getMember(bool, NoAnglesEntered);

    void setSkipOnlyForAstig(bool state){mSkipOnlyForAstig = state; 
      mExcludingSkipList = !state;};
    int *getViewSkipList(int &numList) {numList = mNumViewsToSkip; return mViewSkipList;};
    void setViewSkipList(int *skipList, int numList) 
    {mViewSkipList = skipList; mNumViewsToSkip = numList;};

    double getSpectrumPixelSize() {return mRawTileSize <= mTileSize ? mPixelSize :
        (mRawTileSize * mPixelSize) / mTileSize;};
    bool doingMultipleFits() {return mDoingAutofit || mNextSpecWedgeNum >= 0;};
    double getMidAngleAndRangeIndices(double lowAngle, double highAngle, int &lowNear,
                                      int &highNear, int &numViews);
    void getRangesFromMidAngle(int numViews, int rangeStep, double &midAngle,
                               double &lowAngle, double &highAngle, int &lowSortInd,
                               int &highSortInd, int &lowViewInd, int &highViewInd);
    int findNearestAngleIndex(float angle, float *angleList, int numAngles);
    void findBaselinePastBreak(double *average, double *baseline);

    float getSectorWidth() {return 180. / mNumSectors;};
    float multipleOfSectorWidth(float value) 
    {return floor(value * mNumSectors / 180. + 0.5) * 180. / mNumSectors;};

    void setMinViewsForAstig(int value, bool doingOtherAction);
    void setMinViewsForPhase(int value, bool doingOtherAction);
    void combinePowerSpectra();
    void addAmplitudeSpectrum(float *ampArray, float *spectrum, float *scaledSpec, 
                              double refDefocus, double curDefocus);
    void scaleAndAddStrip(
      float *psSum, double *stripAvg, int *stripCounter, int counter,
      double mean, double refDefocus, double curDefocus, float freqInc,
      SliceCache *cachePtr, double *avgPtr, int *freqTileCounterPtr);
    bool switchToOrFromCtffind();
    //void manageAstigMinViews();
    void setupCtffindParams(CtffindParams &params, bool astigKnown, float astigmatism,
                            float astigAngle, bool knowPhase = true, 
                            float phaseShift = NO_PHASE_VALUE);
    int multipleSpectraAndFits();
    int getNumWedges();
    void manageEnablesForMultipleFits(bool fitting);
    int adjustCachesForTileParams(double defTol, int tSize, double axisAngle, 
                                  double leftTol, double rightTol, double cropPixel);
    int adjustCachesForTileParams();
    int anyAstigmatismSaved(int &phase, int &cutonFreq);
    int writeDefocusOrTempFile(bool ifTemp);
    void setupNextSpecDefocuses();
    void setWiderAngleRange(double midAngle, int minViews);
    void badEntryMessage(bool valOK, const QString &message, bool append = true);
    double nearbyAverageCutonFreq(int numVals);
    QPushButton *diaPlusMinusButton(bool open, QWidget *parent, QBoxLayout *layout);
    void ensureFitRangeUpdated();
    void manageCropWarning();
    
 public slots:
    void rangeChanged(double x1, double x2, double, double);
    void angleChanged(double mLowAngle, double mHighAngle, double defocus, 
                      double defTol,int tSize,double axisAngle,double leftTol,
                      double rightTol, double cropPixel);
    void moreTile(bool hasIncludedCentralTiles);
    void moreTileCenterIncluded();
    void setX1Method(int index){mX1MethodIndex=index; mAllViewsAstigmatism = 0.;}
    void setX2Method(int index){mX2MethodIndex=index; mAllViewsAstigmatism = 0.;}
    void setUseCurDefocus(int index){mUseCurDefocus=index; mAllViewsAstigmatism = 0.;}
    void setInitialCentralTiles(bool state);
    void setBaselineOrder(int order, bool initialize);
    void saveCurrentDefocus();
    void writeDefocusFile();
    void graphTableValues();


 private:
    int mDim;               // Size of power spectrum display
    int mTileSize;          // Tile size being analyzed, in pixels
    int mHyperRes;          // Hyper-resolution factor for 1-D power spectra
    Ilist *mSaved;          // The table of stored data
    bool mSaveModified;     // Flag that the saved data was changed
    bool mBackedUp;         // Flag that defocus file was backed up
    Ilist *mSavedCopy;      // A copy of the table of stored data when doing autofitting
    bool mSaveAndExit;      // Flag to run without UI
    char *mFnStack;         // Name of stack file
    char *mFnAngle;         // Name of angle file
    char *mFnDefocus;       // Name of defocus file
    QString mTempFileToRemove;     // File to remove when graphs close
    double mLowAngle;       // Low and high end of fitting range in degrees;
    double mHighAngle;
    double mLeftDefTol;     // Focus tolerance on left and right side in nm;
    double mRightDefTol;
    int mViewRangeStep;     // How many views to step the range
    float mAutoStepAngle;   // Angle to step range when doing automatic on startup
    float mAutoStepRange;   // Angle range for autofitting on startup
    int mNumViewsInRange;   // Number of views in fitting range
    double mAutoFromAngle;  // Starting and ending angles of autofit range
    double mAutoToAngle;
    float *mTiltAngles;     // Array of tilt angles
    float *mSortedAngles;   // Tilt angles sorted and with skips removed base on context
    int mNumSortedAngles;   // Number of angles after current exclusion
    float mAngleSign;       // -1 to invert angles, 1 not to
    float mMinAngle, mMaxAngle;   // Min and max tilt angles
    int mNxx;               // x dimension;
    int mNyy;               // y dimension;
    int mNzz;               // views in file
    double mDefocusTol;     // Defocus tolerance for strips in nm;
    double mPixelSize;      // True pixel size in nm;
    int mVoltage;           // Voltage in Kv;
    double mCropPixelSizeInDia;  // Cropped pixel size shown in dialog
    bool mCropSpectra;      // Flag to crop spectra
    int mRawTileSize;       // Tile size read from file, before cropping
    bool mNoAnglesEntered;  // Flag that no angles were entered and angles are fake
    
    SliceCache *mCache, *mCache2;    // SliceCache for primary and secondary TS
    // x-direction tile number already included in PS computation;
    int *mTileIncluded; 
    int mTotalTileIncluded;
    double *mRAverage;          // is the signal PS
    double *mRAverage1;         // is the primary focal pair tilt-series PS
    double *mRAverage2;         // is the secondary focal pair tilt-series PS
    int *mFreqTileCounter;      // Count of tiles * pixels in each bin of PS
    int *mFreqTileCounter2;     // Count of tiles * pixels in each bin of secondary PS
    double mStackMean, mStackMean2;    // Mean of current noise spectrum
    float *mAmpSpecSum;         // Current sum of 2D amplitude spectra
    int mLowZeroForScaling;     // Zero numbers for scaling spectra between defocuses
    int mHighZeroForScaling;
    double mTiltAxisAngle;      // Rotation angle of tilt axis in degrees
    int mX1MethodIndex;         // Gaussian or linear for curve 1 and 2
    int mX2MethodIndex;
    int mZeroFitMethod;         // Choice of method for fitting
    bool mVaryCtfPowerInFit;    // Open to solve for power in CTF function
    int mPolynomialOrder;       // Order for fitting polynomial to dip
    int mUseCurDefocus;         // Flag to use current fitted defocus as base for fitting
    bool mInitialCentralTiles;  // Flag to compute just from central tiles
    int mX1Index1;              // Starting index to fit
    int mX1Index2;              // Ending index for curve 1 when 2 curves
    int mX2Index1;              // Starting index for curve 2 when 2 curves
    int mX2Index2;              // Ending index to fit
    float mNumZerosToFit;       // Number of zeros to set end of fitting range to
    int mBaselineOrder;         // Order for baseline fitting polynomial
    bool mDoTwoLineBaselineFit; // Flag to do initial 2-line fit for baseline
    int mMinViewsForAstig;      // Minimum views to combine for astigmatism
    float mWedgeAngleRange;     // Size of wedges in degrees
    float mWedgeCenterAngle;    // Center angle of current wedge, -90 to 90
    float mWedgeInterval;       // Step between wedges
    int mMinViewsForPhase;      // Minimum views to combine for finding phase shift
    int mStartingSlice;         // Starting and ending Z value in file of angle range
    int mEndingSlice;
    int mNumSlicesDone;         // Number of slices being combined
    int mNumNoiseFiles;         // Number of noise files, can be 0
    int *mNoiseIndexes;         // Index to the noise spectra in order by mean
    double *mNoiseMeans;        // Means of the noise spectra
    double *mAllNoisePS;        // The collection of noise spectra
    double *mNoisePS;           // Current noise spectrum to use
    double mDefocusOffset;      // defocus offset between 1st and 2nd tilt series
    int mDefVersionIn;          // Version number if any in read-in file
    int mDefVersionOut;         // Version number to write in output file
    bool mFitSingleViews;       // Flag to fit single views
    bool mFocalPairProcessing;  // Flag for focal pair tilt series
    bool mNoNoiseFiles;         // Flag for skipping noise subtraction
    bool mSlowInitialSearch;    // Flag for slow initial search in Ctffind
    bool mFindAstigmatism;      // Flag to find astigmatism
    bool mPlotSetInitialized[2];  // Whether regular and Ctffind plot are initialized
    bool mShowWedgePlots;       // Flag to show the graphs for each wedge
    float mSecToShowWedges;     // How long to show them
    bool mSwitchedZoomStacks;   // Flag that the plot zoom stacks were switched
    bool mNextFitKnowAstig;     // Astigmatism is known in the next fit by ctffind
    float mNextFitAstigmatism;  // Astigmatism and angle for next fit with ctffind
    float mNextFitAstigAngle;
    float mNextSpecWedgeRange;   // Wedge range, either fixed or variable in one spectrum
    int mNextSpecWedgeNum;       // Wedge number when looping over regular wedges
    float mNextSpecAstigmatism;  // Astigmatism to adjust for in next spectrum
    float mNextSpecAstigAngle;   // And angle
    float mLastSpecAstigmatism;  // Astigmatism and angle found in last analysis
    float mLastSpecAstigAngle;
    float mNextFitStartDefocus;  // Starting defocus for fits on an astigmatism iteration
    float mAllViewsAstigmatism;  // Value of astigmatism and angle found from all views
    float mAllViewsAstigAngle;
    int mNumSectors;             // Number of angular sectors, components of wedges
    std::vector<double> mNextSpecSliceDefocus; // Defocus to use for each slice in spec
    double mNextSpecEffectiveDefocus;  // Overall effective (target) defocus for spec
    double mWedgeFitCenterDefocus;   // Focus to use as starting one for wedge fits
    float mMaxAstigmatism;       // Limit to focus range in searches for wedges
    bool mStopAutofitting;       // Flag that stop was pressed
    int mAutofitIteration;       // Iteration number when autofitting
    float mCurrentAstigmatism;   // A successful astig result, available for saving
    float mCurrentAstigAngle;
    bool mSettingFromTable;      // Flag that parameters are being set from table
    bool mDoingAutofit;          // Flag that autofit is underway
    int mStepDir;                // Current direction of autofitting, so table can scroll
    double mLastWedgeFitError;   // Error from last astigmatism fit
    bool mSkipOnlyForAstig;      // User flag to skip the skip views only for astig/phase
    int *mViewSkipList;          // The skip list numbered from 1
    int mNumViewsToSkip;         // Number of views in list
    bool mExcludingSkipList;     // Flag that the skip list should be excluded
    bool mFindPhaseShift;        // Flags to find phase shift
    double mNextFitPhaseShift;   // Phase to assume in next PS fit
    bool mNextFitKnowPhase;      // Flag that we know phase in next PS fit
    float mPhaseSearchRange;     // Search range for phase in radians
    double mLastFitPhaseShift;   // {hase shift from the last fit
    bool mFindCutOnFreq;         // Flag to find cut-on pfrequency if phase being found
    float mMaxCutOnFreq;         // Maximum cut-on to search
    double mLastFitCutOnFreq;    // Cuton found in last fit, and to be used in next fit
    double mNextFitCutOnFreq;
    bool mUseCurrentPhase;       // Flag to use current (last found) phase when solving
    bool mFindAndFitZeros;       // Flag to find the PS zeros and fit to them for phase
    bool mBreakAtBidirView;      // Flag to break at bidirectional view
    int mBidirectionalView;      // First view of second direction, numbered from 0
    bool mAllowCtffind;          // Set this to expose ctffind in interface
    bool mAstigParamsOpen;       // Flag that astig and phase dialog sections are open
    bool mPhaseParamsOpen;
    bool mChangeColors;          // Flag to use alternate colors
    bool mUpdatingFitRange;      // Flag that ranges are being updated before new fit
};

#endif
