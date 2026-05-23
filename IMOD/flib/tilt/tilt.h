#ifndef _TILT_H
#define _TILT_H

#include <string>
#define MAX_LINE 1000

class Tilt
{
 public:
  Tilt();

  void main( int argc, char *argv[]);
 private:

  // Methods
  void shiftGpuSetupCopy(int lsliceOut, int needEnd, int &needGpuStart, int &needGpuEnd,
                         int &keepOnGpu, int &numLoadGpu, bool &shiftedGpuLoad);
  void writeInternalSlice(int isUnit, float *array, int lslice, int nxprj2,
                          float &dmin4, float &dmax4, float &dmin5, float &dmax5,
                          int &nz5);
  void dumpUpdateHeader(int lsliceDump, float &dmin, float &dmax, double &dtot8,
                        int &numSliceOut, int *nxyzTmp);
  void dumpFillSlices(int &lfillStart, int lfillEnd, float composeFill, float &dmin,
                      float &dmax, double &dtot8, int &numSliceOut, int *nxyzTmp);
  void taperEndToStart(float *array);
  void manageRing(int numVertNeeded, int &numVertSliceInRing, int &nextFreeVertSlice, 
                  int &lvertSliceStart, int &lvertSliceEnd, int lslice);
  void radialWeights(int iradMaxIn, float radFallIn, int ifilterSet, 
                     int ifMultByGaussian);

  void maskPrep(int lslice);
  void transform(float *array, int lslice, int ifilterSet);
  void project(float *inputArray, int indStart, int lslice);
  void cleanUpReducedFFT(float betaMin, float betaMax);
  void compose(int lsliceOut, int lvertSliceStart, int lVertSliceEnd, int idir, 
               int iringStart, float composeFill);
  void decompose(int lslice, int lReadStart, int lreadEnd, int iringStart, 
                 float *vertArr);
  void dumpSlice(int lslice, float &dmin, float &dmax, double &dtot8);
  void maskSlice(float *array, int ithickMask);
  void inputParameters(int argc, char *argv[]);
  void allocateGpuPlanes(int nonPlane, int numWarps, int numDelz, int numFilts,
                         int nygout, int nxGPlane, int nyGPlane, int *maxNeeds, 
                         int ifZfactors, float gpuMemory, float gpuMemoryFrac,
                         int &if3Dtexture, int *maxTex2D, int *maxTex3D, 
                         int *maxTexLaye);
  void warnOrExitIfNoGPU();
  float *packLocalData();
  int *parseCard(char *card, int &nViews, const char *descrip);
  void getValuesFromLines(FILE *fp, char *filename, float *values1, float *values2, 
                          bool separateLines, int &numValues, const char *descrip);
  void lookupAngle(float projAngle, float *angles, int numViews, int &ind1, int &ind2, 
                   float &frac);
  void sampleForReport(float *slice, int lslice, int kthick, int iteration, 
                       float sampScale, float sampAdd);
  void localFactors(float x, int iy, int iv, int &ind1, int &ind2, int &ind3, int &ind4, 
                    float &f1, float &f2, float &f3, float &f4);
  void localProjFactors(float x, int lslice, int iv, float &xprojFix, float &xprojZ, 
                        float &yprojFix, float &yprojZ);
  void findProjectingPoint(float xproj, float yproj, float zz, int &iv, float &xx, 
                           float &yy);
  void setCosStretch();
  void setNeededSlices(int *maxNeeds, int numEval);
  int allocateArray(int *maxNeeds, int numEval, int minLoad, int minMemory);
  void setupSizesForSupersampling();
  void reProject(float *array, int nxs, int nys, int nxOut, float sinAngle,
                 float cosAngle, float *xRayStart, float *yRayStart, int *numPixInRay,
                 int maxRayPixels, float fill, float *projLine, int linear, int noScale);
  float reprojDelZ(float sinBet, float cosBet, float sinAlph, float cosAlph, float mXZfac,
                   float mYZfac);
  void reprojectRec(int lsliceStart, int lsliceEnd, int inLoadStart, int inLoadEnd, 
                    float &dmin, float &dmax, double &dtot8);
  void fillWarpDelz(float *warpDelz, int iv, int line);
  void thresholdedReproj(int iv, int lsliceStart, int lsliceEnd, int inLoadStart, 
                         int inLoadEnd);
  void loadedProjectingPoint(float xproj, float yproj, float zz, int nxLoad, 
                             int inLoadStart, int inLoadEnd, float &xx, float &yy);
  void reprojOneAngle(float *array, float *reprojLines, int inLoadStart, int inLoadEnd, 
                      int line, float cosBet, float sinBet, float cosAlph, float sinAlph,
                      float delzIn, int iwidth, int ithickReproj, int inPlaneSize, 
                      int nxLoad, int minXreproj, int minYreproj, float xprojOffset, 
                      float yprojOffset, float xcenOut, float ycenOut, float xcenPdelxx,
                      float centerSlice, int ifAlpha, float xzFacView, float yzFacView,
                      float dmeanIn);
  void writeReprojLines(int iv, int lineStart, int lineEnd, float &dmin, float &dmax, 
                        double &dtot8);
  void projectModel(Imod *imod, char *outModel, char *outAngles, char *transformFile,
                    int numViewsOrig, char *defocusFile, float pixForDefocus, 
                    float focusInvert);
  void projectionPosition(int iv, float rlX, float rlZ, float rlSlice, float &xproj,
                          float &yproj, int &j, int &lslice, float &f11, float &f12,
                          float &f21, float &f22);

  void memoryError(void *ptr, const char *mess);
  void bpSumNoX(float *array, int &ind1, float *input, int ipoint, int n, double xproj1,
                float cbeta);
  void bpSumXtilt(float *array, int &ind1, float *input, int ipbase, int ipdel, int n,
                  double xproj1, float cbeta, float yfrac, float omyfrac);
  void bpSumLocal(float *array, int &index, float *input, float zz, float *xprojf,
                  float *xprojz, float *yprojf, float *yprojz, int ipoint, int ipdel,
                  int lslice, int jstrt, int jend);
  void projSumLocal(float &xx, float &yy, float &zz, double &sum, float xproj, 
                    float yproj, float sinBeta, int nxLoad, int inLoadStart,
                    int inLoadEnd, float zJump, float ycenAdj);

  // Member variables
  int mIndLoadEnd;       // index of end of loaded input data
  b3dInt64 mMaxStack;    // Full size of data array
  int mIndNeededBase;    // Base index for start / end arrays
  int mNumNeedSE;        // Number of starting / ending slice numbers
  float *mFilterArray;   // Array for radial filters (one or two sets)
  float *mOutSliceArr;   // Array for output slice from back-projection
  float *mVertSliceArr;  // Array for vertical slices
  float *mInputArray;    // Array for input projections, or slices when reprojecting
  float *mReadInArray;   // Array for read-in slice(s) for SIRT
  float *mWorkArray;     // Work array for SIRT subtraction
  float *mSuperOutArr;   // Output slice array for super-sampling    
  int mNeedForFiltArr;   // Array sizes needed for:  radial filter
  int mNeedForOutArr;            //              Slice output array
  b3dInt64 mNeedForVertArr;      //              Vertical slice arrays
  int mNeedForWorkArr;           //              Work array for SIRT
  b3dInt64 mNeedForReadInArr;    //              Read-in slices for SIRT
  b3dInt64 mNeedForSuperArr;     //              Slice for super-sampled reconstruction
  float *mLoadBuffer;    // Buffer for as many lines as can be loaded from one image
  float *mReprojLines;   // Lines into which reprojection from rec is done
  float *mProjLine;      // Line for reprojecting from BP
  float *mOrigLines;     // Original projection lines for subtracting from reprojection
  float *mSuperTempArr;  // Temporary array for Fourier reduction of supersampled slice
  float *mPhaseReal;     // Sines and cosines for phase shift after expanding input FFT
  float *mPhaseImag;
  int *mNeededStarts;    // Starting slice number needed for each slice
  int *mNeededEnds;      // Ending slice number needed for each slice
  int mIntervalHeadSave; // Interval at which to save header
  int mIwidth;           // Width of output slice
  int mIthickBP;         // thickness of back-projection slice made by PROJECT
  int mIsliceStart;      // starting slice to reconstruct
  int mIsliceEnd;        // ending slice to reconstruct
  int mNumPad;           // # of pixels to pad for 1D FFTs
  int mNxProj;           // width of input (formerly nprj)
  int mNyProj;           // y-dimension of input (formerly mprj)
  int mNumViews;         // number of input views, or number used
  int mLimView;          // Size of view-based arrays
  int mLimReproj;        // Size for reprojection-based arrays
  int mIthickOut;        // thickness of final tilted output slice
  int mNxOutPad;         // Padded size of output array when super-sampling
  int mNyOutPad;
  int mCleanSuperFFT;    // Clean up FFT when reducing: 1 for past Nyquist, 2 for wedge
  int mNxGpuCropPad;     // Size of padded array for cropping super-sample slice on GPU
  int mNyGpuCropPad;
  float mBetaMin;        // Minimum and maximum (inverted) tilt angle in radians
  float mBetaMax;
  float mMinCosAlpha;    // Minimum cosine of alpha
  int mTitle[20];        // Title for header
  char mLine[MAX_LINE];  // Line for reading text files
  std::string mGpuErrString;   // Error string for GPU problems
  float mDminIn;         // Min/max/mean of input file
  float mDmaxIn;
  float mDmeanIn;
  int mMaskEdges;        // Flag to mask edges of output
  int mPerpendicular;    // Flag to output XZ slices to file
  int mReprojBP;         // Flag to reproject at zero tilt
  int mRecReproj;        // Flag to reproject from a reconstruction file
  int mDebug;            // Debug output flag
  int mReadBaseRec;      // Flag to read in a base reconstruction
  int mRecSubtraction;   // Flag for doing subtraction of read-in slices for SIRT
  int mProjSubtraction;  // Flag for doing subtraction of original projections
  int mVertSirtInput;    // Input for SIRT is vertical slices
  int mSaveVertSlices;   // Save vertical slices at last SIRT iteration
  int mRotateBy90;
  int mParallelHDF;      // Flag that output is being done to an HDF file in parallel
  int mUseGPU;           // Flag to use the GPU
  int mSirtFromZero;     // Do zero iteration of SIRT too
  float mAxisXoffset;    // offset of tilt axis from center of input images
  float mXcenIn;         // center coordinate of input slice
  float mCenterSlice;    // center slice for x-axis tilting
  float mXcenOut;        // center x coordinate of output slice
  float mYcenOut;        // center y coordinate of output slice
  float mBaseForLog;     // Value to add before taking log
  float mYOffset;        // vertical shift of data in an output slice
  float *mXZfac;         // Z factors
  float *mYZfac;
  float *mCompress;      // Compression (thinning) factors
  float *mExposeWeight;  // Exposure weights
  float *mAngles;        // Angles of tilt around tilt axis
  float mEdgeFill;       // Value to fill edges with
  float mZeroWeight;     // Sum of weights at 0 frequency, to scale projection fill value
  float mFlatFrac;       // Fraction of flat filter to use in SIRT
  float mYcenModProj;    // Center in Y for model projection
  int mIdelSlice;        // Increment of slices to reconstruct
  int mNewMode;          // New output mode
  int mIfLog;            // Flag to take the log
  int mInPlaneSize;      // Size needed for input plane, possibly stretched
  int mIpExtraSize;      // size of extra buffer for loading for cosine stretching
  int mNumPlanes;        // Number of input planes that can be loaded
  int mInterpFacStretch; // interpolation factor for cosine stretched data, or 0
  int mInterpOrdStretch; // interpolation order for cosine stretching
  int mInterpOrdXtilt;   // order for interpolating X-tilted slice
  int mSuperSampleFac;   // Factor to supersample by
  int mProjSuperFac;     // Factor applied to projection lines
  int mMinTotSlice;      // Full range of reconstruction being done when doing a chunk
  int mMaxTotSlice;
  int mNumViewBase;      // Number of views in base rec
  int mNumViewSubtract;  // # of views to subtract
  int mNumExtraMaskPix;  // # of extra pixels for the mask at edges
  int mNumVertNeeded;    // # of vertical slices needed to make tilted output slice
  b3dInt64 mIsliceSizeBP; // size = thickness x width of output slice (space allowed)
  int *mNxStretched;     // Size of each stretched input line
  int *mIndStretchLine;  // Index of start of stretched input line
  int *mMapUsedView;     // Map from view index to original view
  float *mStretchOffset; // X offset of stretched line
  float *mWgtAngles;     // Full set of angles for computing weighting by local tilt incre
  float *mExactTable;
  int mNumTiltIncWgt;    // # of weights for finding local tilt increment
  int mNumWgtAngles;     // # of angles in mWgtAngles used in weighting by local increment
  int mNumExactCycles;   // Cycle of exact filter weighting funct to use (1 since no sinc)
  int mNumFakeSIRTiter;  // # of iterations for fake SIRT bp weighting function
  float mTiltIncWgts[20]; // Weight factors for computing local tilt increment
  float mExactSamples;   // Number of elements in the weightfunction table
  float mExactObjSize;   // Object size parameter for exact filters
  int *mIxUnmaskedSE;    // Starting and ending pixels on output line that are NOT masked
  int *mIviewSubtract;   // array of views to subtract
  int *mMaxRayPixels;    // Maximum number of pixels in a reprojection ray
  float mOutAdd;         // Value to add to reconstruction before scaling
  float mOutScale;       // Scaling factor to multiply by before output
  float mBaseOutAdd;     // Scaling factors to use adding to a base rec
  float mBaseOutScale;   
  float mEffectiveScale; // Scaling factor with non-log scaling to use testing GPU output
  float *mSinAlpha;      // Sines of alpha and tilt angles
  float *mSinBeta;
  float *mCosAlpha;      // Cosines of alpha and tilt angles
  float *mCosBeta;
  float *mAlpha;         // X tilt angles
  int mIfAlpha;          // Flag for whether there are X axis tilts
  int mNumWarpPos;       // Number of local positions
  int mLimWarp;          // # of local areas time # of views
  int mNxWarp;           // # of local positions in X and Y
  int mNyWarp;
  int mIxStartWarp;      // X, Y coordinates of first local area
  int mIyStartWarp; 
  int mIdelXwarp;        // X and Y intervals between local areas
  int mIdelYwarp;
  int mIfDelAlpha;       // If there are local changes in alpha
  int *mIndWarp;         // Index to current local area data
  int *mNumPixInRay;     // # of pixels in each projection ray
  float *mDelAlpha;      // Local alignment delta alpha and tilt values
  float *mDelBeta;
  float *mCWarpAlpha;    // Cosines of local area alpha and tilt angles
  float *mCWarpBeta;
  float *mSWarpAlpha;    // Sines of local alpha and beta
  float *mSWarpBeta;
  float *mFwarp;         // Local area transformation
  float *mWarpXZfac;     // Local z factors
  float *mWarpYZfac;
  float *mWarpDelz;      // Delta Z steps when reprojecting from local alignment rec
  float *mXRayStart;     // Starting X and Y of reprojection ray
  float *mYRayStart;
  float *mXprojFs;       // Projection factors for reprojection from local aligned rec
  float *mXprojZs;
  float *mYprojFs;
  float *mYprojZs;
  int mNumReproj;        // # of reprojection angles
  int mMaxZreproj;       // Limits of input in X, Y, Z for reprojection
  int mMinXreproj;
  int mMaxXreproj;
  int mMinYreproj;
  int mMaxYreproj;
  int mMinZreproj;
  int mIthickReproj;
  int mMinXload;         // Range of X loaded when reprojecting a rec
  int mMaxXload;
  int mNumWarpDelz;      // Number of positions in mWarpDelz array when reproj local
  int mNumSIRTiter;      // # of SIRT iterations
  int mIfOutSirtProj;    // Flags to output reprojections and rec slices in internal SIRT
  int mIfOutSirtRec;
  int mIsignConstraint;  // Positive or negative sign constraint in SIRT
  int mIterForReport;    // Starting iteration number for mean / sd reports
  int mNumReadNeed;      // # of tilted slices to read into ring
  float mXprojOffset;    // Projection offsets for getting from coordinate in reprojection
  float mYprojOffset;    // to coordinate in original projections
  float mProjMean;       // Mean of input when reprojecting rec
  float mFilterScale;    // Approximate implicit scaling caused by default radial filter
  float mDxWarpDelz;     // Interval in X between positions
  float mThreshForReproj; // Threshold for reprojection of pixels beyond threshold
  float mThreshPolarity; // Polarity for thresholding: < 0 for pixles below threshold
  float mThreshSumFac;   // Controls whether pixels are marked or summed
  float mThreshMarkVal;  // Value to use for a pixel above threshold
  float mThreshFillVal;  // Value to fill with when thresholding
  float *mReportVals;    // array for mean, sd, and # of slices for each iteration
  float *mCosReproj;     // Sines and cosines of reprojection angles
  float *mSinReproj;
  int mNumGpuPlanes;     // Number of planes available to be stored on GPU
  int mLoadGpuStart;     // First slice loaded onto GPU
  int mLoadGpuEnd;       // Last slice loaded onto GPU
  int mIfGpuByEnviron;   // Flag for whether GPU number came in by environment variable
  int mIactGpuFailOption;  // Action to take when GPU came in by option
  int mIactGpuFailEnviron; // Action to take when GPU came in by environment variable
  int mSkipUnseenPoints; // Flag to skip contours not visible in 3dmod when projecting
};
#endif
