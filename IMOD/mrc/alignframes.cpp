/*
 *  alignframes - align movie frames and stack multiple frame files
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 2016-2019 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */
#include <sys/stat.h>
#include <sys/types.h>
#ifndef _WIN32
#include <unistd.h>
#endif
#include "b3dutil.h"
#include "autodoc.h"
#include "cppdefs.h"
#include "iimage.h"
#include "parse_params.h"
#include "framealign.h"
#include "frameutil.h"
#include "alignframes.h"
#ifdef TEST_SHRMEM
#include "ShrMemClient.h"
#endif

#define SIG2_ROUND_FAC 10000.
#define FRAME_DOSE_KEY "FrameDosesAndNumber"

// When this was a member variable of AliFrame, it crashed opening a dm4 gain
// reference in Windows, spinning off into space instead of calling analyzeDM3
static FrameAlign sFA;

int main( int argc, char *argv[])
{
  AliFrame ali;
  ali.main(argc, argv);
  exit(0);
}

/*
 * Initializations of member variables
 */
AliFrame::AliFrame()
{
  mParallelRead = false;
  mWallRead = 0.;
  mPartialThresh[0] = 0.;
  mPartialThresh[1] = 0.;
  mDebug = 0;
  mUseGPU = -1;
  mGpuFlags = 0;
  mGpuMemLimit = 0.;
  mMemoryLimit = 12.;
  mTestMode = 0;
  mTruncLimit = 0.;
  mMaxDataSize = 0;
  mRefineAtEnd = 0;
  mGroupSize = 1;
  mUseBlockGroup = false;
  mMaxNumZ = 0;
  mHybridShifts = 0;
  mStartAssess = -1;
  mDeferSum = 0;
  mMinBinningToTest = 100;
  mDoseFileType = -1;
  mMaxFrameDoses = 0;
  mTotalDose = 0.; 
  mDoseAccumulates = -1;
  mNumBidir = 0;
  mGainSlice = NULL;
  mDarkSlice = NULL;
  mGainName = NULL;
  mDefectName = NULL;
  mCamSizeX = mCamSizeY = 0;  
  mFeiDefectPad = 1;
  mDefaultByteScale = 30.;
  mExtraHasGainRef = 0;
  mRotationFlip = 0;
  mCorDefBinning = 1;
  mIgnoreZvalue = 0;
  mNumOutFiles = 1;
  mNamesFromMdoc = false;
  mRelFrameStartsFound = false;
  mDoingFrameTS = false;
  mInitialDose = 0.;
  mDoseScaling = 1.;
  mReweightFilt = NULL;
  mOutNames[0] = mOutNames[1] = NULL;
  mOutHeads[0] = &mMainHead;
  mOutHeads[1] = &mUnwgtHead;
  for (int ind = 0; ind < 3; ind++) {
    mZinPartialBufs[ind] = -1;
    mPartialScanBufs[ind] = NULL;
    mPartialLinePtrs[ind] = NULL;
  }
}

/*
 * A BIG MAIN METHOD
 */
void AliFrame::main( int argc, char *argv[])
{
  const char *progname = imodProgName(argv[0]);
  char *filename, *xfExt = NULL;
  std::string xfName, sstr;
  FloatVec orderedAngles;
  int binsToTest[MAX_BINNINGS];
  float varyRadius2[MAX_BINNINGS][MAX_FILTERS];
  float varySigma2[MAX_BINNINGS][MAX_FILTERS];
  int numTimesBest[MAX_BINNINGS][MAX_FILTERS];
  char title[MRC_LABEL_SIZE + 1];
  
  char *framePath = NULL;
  char *extraName, *listName = NULL, *frcName = NULL;
  char *stackName = NULL, *mdocName = NULL, *tiltName = NULL;
  char *openTsNames[2] = {NULL, NULL};
  FILE *outFPs[2];
  FILE *extraFP, *stackFP, *frcFP = NULL, *plotFP = NULL, *fileListFP = NULL;
  unsigned char *readBuf, *sumBuf, *useBuf;
  int needAlloc, bufAllocSize = 0;
  MrcHeader stackHead;
  MrcHeader *headPtr;
  float *summed, *rotSum = NULL, *useSum, *unwgtSum = NULL;
  int defaultBinnings[] = {2, 3, 4, 6, 8};
  int targetAliSize = 1250;
  IloadInfo li;
  bool doRobust, copyShifts, wasGoodEnough;
  bool scaleToMeanSD, endReached = false;
  float taperFrac = 0.1;
  float scale = 1., totalScale = 0.;
  float meanScale = 0., sdScale = 0.;
  int reorderByTilt = 1;
  int numSummaryLines = 2;
  float kFactor = 4.5;
  int shiftLimit = 20;
  int antiFiltType = 4;
  int sumBin = 1;
  float sizeDiffCrit = 0.1;
  float maxMaxWeight = 0.1;
  float goodEnough = 0.;
  int sumRotationFlip = 0;
  int combineFiles = 0;
  int breakSetSize = 0;
  float refRadius2 = 0., refSigma2;
  int skipChecks = 0;
  int adjustMdoc = 0;
  int splineSmooth = 1;
  int minNumForSpline = 20;
  int trimCrit = 10;
  float iterCrit = 0.1;
  int groupRefine = 0;

  int nz, alignBin, ind, nxSum, nySum, ix, iy, itest, faBestFilt;
  int nxStack, nyStack, stackMode, relXbin, relYbin, numAVAuse;
  int numSingleFiles, startCombine, endCombine, adocType, alignBinIn, dataSize;
  size_t tind;
  float xScale, yScale, zScale, relBinning, error, minError, minMean, halfCross, truncUse;
  float quartCross, eighthCross, halfNyq;
  float memLimits[2];
  float fullTaperFrac = 0.02;

  // framealign uses fullTaperFrac as the padding fraction so a default trimming by the
  // same amount will keep the padded align within the original size, good if it is 4K
  float trimFrac = fullTaperFrac;   
  double diff, minDiff;
  int numAVAinput = 7;
  int minFractionalAVA = 7;
  int reverse = 0;
  int startFrame = -1, endFrame = -1;
  bool warnedTwoPass = false;
  float frcDeltaR = 0.005;
  float ringCorrs[510];
  float radius1 = 0., radius2 = 0.06, sigma1 = 0.03, sigma2 = 0.0086;
  float *xShifts, *yShifts, *bestXshifts, *bestYshifts, *rawXshifts, *rawYshifts;
  float *bestXraw, *bestYraw;
  int alsumBinEntered, targetEntered;
  int ierr, iz, zStart, zEnd, zDir, numOptArgs, numNonOptArgs, outMode;
  int nxOut, nyOut, numInByOpt, ifile;
  int numVaries, indBestBin, indBestFilt, slideGrpSize;
  int useInd, summingMode, numTestLoops, useStart, useEnd, indBinUse, indFiltUse;
  int numFiltUse, numDone, numFetch, filt, stackBin, outNum;
  float pixTemp, physMem;
  int nzAlign, groupEnd, groupStart, izLow, izHigh, useMode, group, blockGrpSize;
  int endAssess = -1;
  int numFrameUse, numSets, minSet, maxSet, fileHasTilts, maxReadThreads = 1;
  int extraHasTilts = 0, extraHasAxisPix = 0;
  int numAllSets, minSetSize, maxSetSize, superFile, setInFile, izRead;
  int skippedFrame[2], originalZval;
  int startingFile, endingFile, numFilesToDo, maxExclude, driftLoop, numDriftLoop;
  float fileAxis, extraAxis, filePix, extraPixSize, optionPixSize = 0., axisAngle = -999.;
  bool hasExtra, enteredScale, enteredMode, allPos, allNeg;
  bool suppressInitialShifts, excludingInitial;
  bool changedSetOrder = false;
  int refNamesFromTitles = 0;
  int useShrMem = 0;
  int eerZbinning = 10, eerSuperRes = 1;
  float stackBinX, stackBinY;
  float driftMaxFracNum = 0.2, driftMaxDist = 0.;
  FloatVec extraTilts;
  IntVec setOrderIndex;
  float *extraBuf = NULL;
  int extraBufSize = 0;
  float resMean[MAX_FILTERS + 1], resSD[MAX_FILTERS + 1], meanResMax[MAX_FILTERS + 1];
  float maxResMax[MAX_FILTERS + 1], meanRawMax[MAX_FILTERS + 1];
  float maxRawMax[MAX_FILTERS + 1], smoothDist[MAX_FILTERS + 1], rawDist[MAX_FILTERS + 1];
  float  tmin, tmax, tmean, alreadyScaledBy, tsd, scaleFac, addFac;

  // Dose weighting variables
  float doseAfac = 0., doseBfac = 0., doseCfac = 0.;
  float priorTemp, sumOfDoses;
  int sumOfFrames;  

  // Fallbacks from    ../manpages/autodoc2man 2 1 alignframes
  int numOptions = 84;
  const char *options[] = {
    "input:InputFile:FNM:", "output:OutputImageFile:FN:", "list:ListOfInputFiles:FN:",
    "break:BreakFramesIntoSets:I:", "saved:SavedFrameListFile:FN:",
    "gap:MaxGapWithinFrameSet:I:", "skip:SkipFileChecks:B:",
    "stack:CorrespondingStack:FN:", "mdoc:MetadataFile:FN:",
    "path:PathToFramesInMdoc:CH:", "ignore:IgnoreZvaluesInMdoc:B:",
    "adjust:AdjustAndWriteMdoc:B:", "reorder:ReorderByTiltAngle:I:",
    "pixel:PixelSize:F:", "eer:EERSuperResZSumPadding:IT:",
    "super:SuperGainFactorFile:FN:", "binning:AlignAndSumBinning:IP:",
    "target:TargetAlignSize:I:", "frames:StartingEndingFrames:IP:",
    "partial:PartialFrameThresholds:FP:", "drift:DriftLimitDistAndNumber:FP:",
    "sets:RangeOfSetsToDo:IP:", "mode:ModeToOutput:I:", "scale:ScalingOfSum:F:",
    "total:TotalScalingOfData:F:", "meansd:MeanAndSDtoScaleTo:FP:",
    "rfsum:SumRotationAndFlip:I:", "tilt:TiltAngleFile:FN:",
    "axis:AxisRotationAngle:F:", "xfext:TransformExtension:CH:",
    "frc:FRCOutputFile:FN:", "ring:RingSpacingForFRC:F:",
    "lines:LinesOfAlignSummary:I:", "plottable:PlottableShiftFile:FN:",
    "nosum:NoSumsOutput:B:", "titles:RefAndDefectFromTitles:B:",
    "gain:GainReferenceFile:FN:", "rotation:RotationAndFlip:I:",
    "dark:DarkReferenceFile:FN:", "defect:CameraDefectFile:FN:",
    "double:DoubleDefectCoords:B:", "imagebinned:ImagesAreBinned:F:",
    "truncate:TruncateAbove:F:", "pair:PairwiseFrames:I:", "reverse:ReverseOrder:B:",
    "shift:ShiftLimit:I:", "group:GroupSize:I:", "radius2:FilterRadius2:F:",
    "vary:VaryFilter:FAM:", "hybrid:UseHybridShifts:B:", "refine:RefineAlignment:I:",
    "rgroup:RefineWithGroupSums:B:", "stop:StopIterationsAtShift:F:",
    "rrad2:RefineRadius2:F:", "smooth:MinForSplineSmoothing:I:", "gpu:UseGPU:I:",
    "memory:MemoryLimitGB:FA:", "dtype:TypeOfDoseFile:I:",
    "dfile:DoseWeightingFile:FN:", "dtotal:FixedTotalDose:F:",
    "dframe:FixedFrameDoses:F:", "dprior:InitialPriorDose:F:",
    "bidir:BidirectionalNumViews:I:", "accum:DoseAccumulates:I:",
    "normalize:NormalizeDoseWeighting:B:", "volt:Voltage:I:",
    "optimal:OptimalDoseScaling:F:", "critical:CriticalDoseFactors:FT:",
    "unweight:UnweightedOutputFile:FN:", "test:TestBinnings:IA:",
    "assess:AssessWithFrames:IP:", "good:GoodEnoughError:F:",
    "weight:MaxResidualWeight:F:", "trim:TrimFraction:F:", "taper:TaperFraction:F:",
    "antialias:AntialiasFilter:I:", "radius1:FilterRadius1:F:",
    "sigma1:FilterSigma1:F:", "sigma2:FilterSigma2:F:", "kfactor:KFactorForFits:F:",
    "debug:DebugOutput:I:", "flags:FlagsForGPU:I:", "shrmem:ShrMemTest:B:",
    "help:usage:B:"};

  // Startup with fallback
  PipReadOrParseOptions(argc, argv, options, numOptions, progname, 
                        2, 1, 1, &numOptArgs, &numNonOptArgs, imodUsageHeader);

  // Get output file and number of input files
  PipGetBoolean("NoSumsOutput", &mTestMode);
  PipNumberOfEntries("InputFile", &numInByOpt);
  mNumInFiles = numInByOpt + numNonOptArgs;
  if (PipGetString("OutputImageFile", &mOutNames[0])) {
    if (!mTestMode) {
      if (!numNonOptArgs)
        exitError("No output file specified");
      mNumInFiles--;
      PipGetNonOptionArg(numNonOptArgs - 1, &mOutNames[0]);
    }
  } else if (mTestMode) {
    exitError("No output file should be specified when not making sums");
  }

  PipGetThreeIntegers("EERSuperResZSumPadding", &eerSuperRes, &eerZbinning, 
                      &mFeiDefectPad);
  ierr = tiffGetMaxEERsuperRes();
  if (eerSuperRes < 0 || eerSuperRes > ierr)
    exitError("Super-resolution for EER files must be between 0 and %d", ierr);
  if (eerZbinning < 1)
    exitError("Summing of frames for EER files must be greater than 0");
  tiffSetEERreadProperties(eerSuperRes, eerZbinning, 0);

  if (PipGetString("ListOfInputFiles", &listName) == 0) {
    if (mNumInFiles)
      exitError("You cannot enter input files as arguments with the -list option");
    fileListFP = fopen(listName, "r");
    if (!fileListFP)
      exitError("Could not open list of input files, %s", listName);
    mNumInFiles = 2000000000;
  }
  PipGetInteger("BreakFramesIntoSets", &breakSetSize);
  if (breakSetSize != 0 && breakSetSize < 2)
      exitError("The entry for -break must be at least 2");
  PipGetBoolean("SkipFileChecks", &skipChecks);
  if (skipChecks)
    iiAssumeDMfileMatches(1);

  mDoingFrameTS = readAnalyzeSavedFrameList(breakSetSize);

  // See if further trimming of frames is desired
  if (!PipGetTwoFloats("PartialFrameThreshold", &mPartialThresh[0], &mPartialThresh[1]) &&
      !mDoingFrameTS)
    exitError("You can enter -partial only with a frame list file");
  for (ind = 0; ind < 2; ind++) {
    if (mPartialThresh[ind] >= 1.)
      exitError("The threshold for dropping partial frames is relative and must be less "
                "than 1");
  }
  
  // Find out what auxiliary files are being used
  PipGetString("MetadataFile", &mdocName);
  PipGetString("CorrespondingStack", &stackName);
  if (mNumInFiles > 0 && mdocName && stackName)
    exitError("You cannot enter -mdoc with -stack; the mdoc would not be used");
  if (!mNumInFiles && !mdocName)
    exitError("Input file(s) must be specified with arguments, an mdoc file, or a "
              "list file");

  // Open the mdoc now and set flag to get names from it if necessary
  if (mdocName) {
    openMdocFile(mdocName, mNumSect, adocType);
    if (!mDoingFrameTS && 
        (AdocGetInteger(ADOC_GLOBAL_NAME, 0, "DataMode", &stackMode) || 
         AdocGetTwoIntegers(ADOC_GLOBAL_NAME, 0, "ImageSize", &nxStack, &nyStack)))
      exitError("Getting data mode or image size from mdoc file");

    mNamesFromMdoc = !mNumInFiles;
    if (mNamesFromMdoc) {
      mNumInFiles = mNumSect;
      PipGetString("PathToFramesInMdoc", &framePath);
    }
    PipGetBoolean("IgnoreZvaluesInMdoc", &mIgnoreZvalue);
    if (!mTestMode)
      PipGetBoolean("AdjustAndWriteMdoc", &adjustMdoc);
    if (mIgnoreZvalue)
      reorderByTilt = 0;
  }
  
  if (PipGetTwoIntegers("StartingEndingFrames", &startFrame, &endFrame) == 0) {
    if (startFrame <= 0 || endFrame < startFrame)
      exitError("Values for starting and ending frames are out of range");
    if (mDoingFrameTS)
      exitError("You cannot use -frame with a saved frame list file");
  }
  if (PipGetTwoIntegers("AssessWithFrames", &mStartAssess, &endAssess) == 0 &&
      (mStartAssess <= 0 || endAssess < mStartAssess))
    exitError("Values for starting and ending frames to use for assessment are out of "
              "range");
  if ((breakSetSize > 0 || mDoingFrameTS) && mStartAssess >= 0)
    exitError("You cannot enter -assess when breaking frames into sets to sum");
  if (breakSetSize > 0 && mNamesFromMdoc)
    exitError("You cannot break frames into sets when input filenames come from an mdoc "
              "file");

  PipGetFloat("TotalScalingOfData", &totalScale);
  enteredScale = PipGetFloat("ScalingOfSum", &scale) == 0;
  if (enteredScale && totalScale > 0.)
    exitError("You cannot enter both -scale and -total");
  scaleToMeanSD = PipGetTwoFloats("MeanAndSDtoScaleTo", &meanScale, &sdScale) == 0;
  if (scaleToMeanSD && (enteredScale || totalScale > 0.))
    exitError("You cannot enter -meansd with -scale or -total");
  alreadyScaledBy = 1.;
  enteredMode = PipGetInteger("ModeToOutput", &outMode) == 0;
  if (enteredMode && sliceModeIfReal(outMode) < 0)
    exitError("Output mode of %d is not allowed", outMode);

  // Get flag to get reference and defects from frame file title, but also bring in
  // the names if any since they override
  PipGetInteger("RefAndDefectFromTitles", &refNamesFromTitles);
  if (refNamesFromTitles)
    mRotationFlip = -1;
  PipGetString("GainReferenceFile", &mGainName);
  PipGetString("CameraDefectFile", &mDefectName);
  PipGetInteger("RotationAndFlip", &mRotationFlip);
  PipGetString("TiltAngleFile", &tiltName);
  PipGetFloat("AxisRotationAngle", &axisAngle);
  PipGetString("CorrespondingStack", &stackName);
  PipGetInteger("SumRotationAndFlip", &sumRotationFlip);
  if (!PipGetInteger("ReorderByTiltAngle", &reorderByTilt) && mIgnoreZvalue)
    exitError("You cannot enter both -reorder and -ignore");
  targetEntered = 1 - PipGetInteger("TargetAlignSize", &targetAliSize);
  if (targetAliSize < 64)
    exitError("Target size for align reduction is too small");
  PipGetInteger("LinesOfAlignSummary", &numSummaryLines);
  suppressInitialShifts = numSummaryLines < 0;
  numSummaryLines = B3DABS(numSummaryLines);
  B3DCLAMP(numSummaryLines, 1, 3);
  PipGetString("FRCOutputFile", &frcName);
  if (frcName && mTestMode)
    exitError("There is no FRC output available when not making sums");
  mGettingFRC = !mTestMode && (frcName || numSummaryLines > 2);

  // Get dose-weighting related options
  filename = NULL;
  if (PipGetFloat("FixedTotalDose", &mTotalDose) + 
      PipGetInteger("TypeOfDoseFile", &mDoseFileType) + PipGetString("FixedFrameDoses",
                                                                    &filename) < 2)
    exitError("You can enter only one of the dose weighting options -dtype, -dtotal,"
              " or -dframe");
  
  if (mDoseFileType > 0 || mTotalDose > 0 || filename) {
    if (mTestMode)
      exitError("You cannot enter dose weighting options with no summing");
    PipGetThreeFloats("CriticalDoseFactors", &doseAfac, &doseBfac, &doseCfac);
    PipGetInteger("DoseAccumulates", &mDoseAccumulates);
    if (mDoseAccumulates < 0)
      mDoseAccumulates = (stackName || mdocName || mDoingFrameTS) ? 1 : 0;
    processDoseWeightingOptions(filename, adocType);

    // Get option for unweighted output also
    if (!PipGetString("UnweightedOutputFile", &mOutNames[1]))
      mNumOutFiles = 2;
  }

  // Open and read header of every input file before starting
  numAllSets = 0;
  minSetSize= 0;
  for (ind = 0; ind < mNumInFiles; ind++) {
    filename = getNextFilename(ind, numInByOpt, fileListFP, framePath, listName,
                               endReached);
    if (!filename)
      break;

    // Save filename and check file
    mInFiles.push_back(filename);
    if (!ind || !skipChecks) {
      mInFP = openAndReadHeader(filename, &mInHead, "input image", 
                             mTestMode && ind == mNumInFiles - 1);
      checkInputFile(filename, &mInHead, ind ? mNx : 0, mNy, combineFiles);
      dataSizeForMode(mInHead.mode, &dataSize, &iz);
      ACCUM_MAX(mMaxDataSize, dataSize);
      if (!ind && refNamesFromTitles)
        checkTitlesForRefNames(filename);
    }
    
    // Set size and mode from first file.  Default to not do bytes as output
    if (!ind) {
      if (!enteredMode)
        outMode = mInHead.mode == MRC_MODE_BYTE ? MRC_MODE_SHORT : mInHead.mode;
      mNx = mInHead.nx;
      mNy = mInHead.ny;
      if (mDoingFrameTS) {
        if (mInHead.nz != mSavedFrames.size())
          exitError("The number of frames (%d) does not match the number of entries in "
                    "the saved frame list (%d)", mInHead.nz, mSavedFrames.size());
        nxStack = mNx;
        nyStack = mNy;
      }

      mMainHead = mInHead;
      mUnwgtHead = mInHead;
      mrc_get_scale(&mInHead, &xScale, &yScale, &zScale);

      // Also get the rotation/flip if needed
      if (mRotationFlip < 0 || totalScale > 0.) {
        for (ix = 0; ix < mInHead.nlabl; ix++) {
          if (mRotationFlip < 0) {
            extraName = strstr(mInHead.labels[ix], " r/f ");
            if (extraName)
              mRotationFlip = atoi(extraName + 4);
          }
          extraName = strstr(mInHead.labels[ix], ", scaled by");
          if (extraName)
            alreadyScaledBy = atof(extraName + 11);
        }
        if (mRotationFlip < 0)
          exitError("Cannot find r/f entry in header of first input file");
      }

      // And commit to combining files if one frame and breaking into sets, and disallow
      // frame subsets
      if (breakSetSize > 0 && mInHead.nz == 1)
        combineFiles = breakSetSize;
      if (combineFiles > 0 && (startFrame >= 0 || mStartAssess >= 0))
        exitError("You cannot enter -frames when combining single-frame files");
      if (!combineFiles && skipChecks)
        exitError("You cannot skip file checks unless combining single-frame files");

      // Determine if reading TIFF and if so, get number of threads to use
      if (!combineFiles) {
        mFileCopies[0] = iiLookupFileFromFP(mInFP);
        if (!mFileCopies[0])
          printf("WARNING: %s - Could not find iiFile from file pointer to assess " \
                 "whether to read a TIFF file in parallel\n", progname);
        if (mFileCopies[0] && mFileCopies[0]->file == IIFILE_TIFF) 
          maxReadThreads = tiffNumReadThreads(mNx, mNy, mFileCopies[0]->tiffCompression,
                                              MAX_READ_THREADS);
      }
    }

    // Get frames to use from file and make sure it is legal
    numFrameUse = mInHead.nz;
    if (startFrame > 0)
      numFrameUse = B3DMIN(mInHead.nz, endFrame) + 1 - startFrame;
    if (numFrameUse < 1)
      exitError("No frames would be included for file %s which has only %d frames",
                mInFiles[ind], mInHead.nz);
    if (!combineFiles && numFrameUse < breakSetSize)
      exitError("The available frames for file %s is %d, less than the set size of %d",
                mInFiles[ind], numFrameUse, breakSetSize);

    // Check for gain reference if one not entered
    hasExtra = mInHead.next && !extraIsNbytesAndFlags(mInHead.nint, mInHead.nreal);
    iz = 0;
    if (hasExtra && !mGainName && 
        mInHead.next >= mInHead.nz * 4 * (mInHead.nint + mInHead.nreal) + 4 * mNx * mNy)
      iz = 1;
    if (!ind)
      mExtraHasGainRef = iz;
    else if (mExtraHasGainRef != iz)
      exitError("All files must have gain references in their extended header if any do;"
                " it is missing in %s", mInFiles[ind]);

    // Get the min and max set sizes if breaking, or if extra header has tilt angles,
    // or for a frame list file
    minSet = 0;
    if (breakSetSize > 0 && !combineFiles) {
      minMaxSetSize(breakSetSize, numFrameUse, minSet, maxSet);
      numSets = numFrameUse / breakSetSize;
    } else if (mDoingFrameTS) {
      numSets = mNumInSets.size();
      minSet = mInHead.nz;
      maxSet = 0;
      for (ind = 0; ind < numSets; ind++) {
        ACCUM_MIN(minSet, mNumInSets[ind]);
        ACCUM_MAX(maxSet, mNumInSets[ind]);
      }

    } else if (!combineFiles && hasExtra) {
      ierr = analyzeExtraHeader(mInFP, &mInHead, startFrame, endFrame, breakSetSize > 0, 
                                &extraBuf, extraBufSize, extraTilts,
                                minSet, maxSet, fileAxis, filePix);

      // Save the axis rotation and pixel size if any, and make sure all files are 
      // consistent
      fileHasTilts = (mNamesFromMdoc || breakSetSize > 0) ? 0 : ierr / 2;
      if (!ind) {
        extraHasAxisPix = ierr % 2;
        if (ierr % 2) {
          extraAxis = fileAxis;
          extraPixSize = filePix;
        }
        extraHasTilts = fileHasTilts;
      }

      if (extraHasAxisPix != ierr % 2 || 
          (extraHasAxisPix && (fabs(extraAxis - fileAxis) > 0.01 || 
                               fabs(extraPixSize - filePix) > 0.01)))
        exitError("All files must have the same axis rotation angles and pixel sizes in "
                  "the extended header if any do; they differ in %s", mInFiles[ind]);
      if (extraHasTilts != fileHasTilts)
        exitError("All files must have valid tilt angles in extended header if any do "
                  "and if -break is not entered; they are invalid in %s", mInFiles[ind]);
      if (extraHasTilts) {
        numSets = extraTilts.size();
        if (!tiltName && !stackName) {
          mTiltAngles.reserve(mTiltAngles.size() + extraTilts.size());
          mTiltAngles.insert(mTiltAngles.end(), extraTilts.begin(), extraTilts.end());
        }
      }

      // Assign the axis angle to be output if not set already
      if (extraHasAxisPix && axisAngle < -990.)
        axisAngle = fileAxis;
    }

    // Keep track of minimum and maximum set size if sets came out either way
    if (minSet) {
      if (!minSetSize) {
        minSetSize = minSet;
        maxSetSize = maxSet;
      } else {
        ACCUM_MIN(minSetSize, minSet);
        ACCUM_MAX(maxSetSize, maxSet);
      }
      numAllSets += numSets;
    }
    if (!ind || !skipChecks)
      iiFClose(mInFP);

    ACCUM_MAX(mMaxNumZ, numFrameUse);
    ACCUM_MAX(mMaxFrameDoses, mInHead.nz);
  }

  // Finish up with processing a list of input files: just fix the # of files
  if (fileListFP) {
    fclose(fileListFP);
    mNumInFiles = (int)mInFiles.size();
    if (!mNumInFiles)
      exitError("There were no input files in the list file %s", listName);
    free(listName);
    printf("%d files in input file list\n", mNumInFiles);
  }

  if (refNamesFromTitles && !mGainName && !mExtraHasGainRef)
    exitError("No gain reference name was found in the frame file titles");

  // Handle combination of single-frame files or breaking frames into sets
  if (combineFiles) {
    if (mNumInFiles < combineFiles)
      exitError("The break entry, %d, is bigger than the number of single-frame input "
                "files, %d", combineFiles, mNumInFiles);
    numSingleFiles = mNumInFiles;
    minMaxSetSize(combineFiles, mNumInFiles, ierr, mMaxNumZ);
    mMaxFrameDoses = mMaxNumZ;
    mNumInFiles /= combineFiles;
    printf("%d files will be combined into %d summed images\n", numSingleFiles,
           mNumInFiles);
  } else if (breakSetSize > 0 || extraHasTilts || mDoingFrameTS) {
    numSingleFiles = mNumInFiles;
    mNumInFiles = numAllSets;
    mMaxNumZ = maxSetSize;
    mMaxFrameDoses = maxSetSize;
    if (extraHasTilts)
      printf("Tilt angles from extended header will be used to break frames into sets\n");
    printf("Frames from %d files will be broken into %d summed images\n", numSingleFiles,
           mNumInFiles);
  }
  if (mExtraHasGainRef)
    printf("Gain reference from extended header will be applied to frames\n");

  startingFile = 1;
  endingFile = mNumInFiles;
  PipGetTwoIntegers("RangeOfSetsToDo", &startingFile, &endingFile);
  if (startingFile > endingFile || startingFile < 1 || endingFile > mNumInFiles)
    exitError("Starting or ending set number to process is out of range");
  numFilesToDo = endingFile + 1 - startingFile;

  // Now that number of "files" is known, and maximum number of sections, return to
  // dealing with dose-weighting
  unifyDoseInformation(breakSetSize, combineFiles, adocType);

  // Adjust default memory if physical memory is available, and get 
  physMem = b3dPhysicalMemory() / (1024. * 1024. * 1024.);
  if (physMem > 0.) {
    if (physMem < 16.)
      mMemoryLimit = 0.75 * physMem;
    if (physMem > 24.)
      mMemoryLimit = 0.5 * physMem;
  }
  ind = 0;
  if (PipGetFloatArray("MemoryLimitGB", memLimits, &ind, 2) == 0) {
    mMemoryLimit = memLimits[0];
    if (memLimits[0] < -0.95 || (ind > 1 && memLimits[1] < -0.95) ||
        fabs(memLimits[0]) < 0.05 || (ind > 1 && fabs(memLimits[1]) < 0.05))
      exitError("You cannot enter a memory limit below -0.95 or between -0.05 and 0.05");
    if (memLimits[0] < 0) {
      if (!physMem)
        exitError("You cannot enter a negative CPU memory limit: system memory "
                  "not available");
      mMemoryLimit *= -physMem;
    }
    if (ind > 1)
      mGpuMemLimit = memLimits[1];
  }

  // Get lots more options
  xShifts = B3DMALLOC(float, mMaxNumZ);
  yShifts = B3DMALLOC(float, mMaxNumZ);
  bestXshifts = B3DMALLOC(float, mMaxNumZ);
  bestYshifts = B3DMALLOC(float, mMaxNumZ);
  rawXshifts = B3DMALLOC(float, mMaxNumZ);
  rawYshifts = B3DMALLOC(float, mMaxNumZ);
  bestXraw = B3DMALLOC(float, mMaxNumZ);
  bestYraw = B3DMALLOC(float, mMaxNumZ);
  if (!xShifts || !yShifts || !bestXshifts || !bestYshifts || !bestXraw || !bestYraw ||
      !rawXshifts || !rawYshifts)
    exitError("Allocating arrays for shifts");
  PipGetInteger("PairwiseFrames", &numAVAinput);
  PipGetFloat("TaperFraction", &taperFrac);
  if (!taperFrac)
    fullTaperFrac = 0.05;
  PipGetFloat("TrimFraction", &trimFrac);
  PipGetBoolean("ReverseOrder",  &reverse);
  PipGetInteger("ShiftLimit", &shiftLimit);
  PipGetFloat("TruncateAbove", &mTruncLimit);
  PipGetString("TransformExtension", &xfExt);
  PipGetInteger("DebugOutput", &mDebug);
#ifdef TEST_SHRMEM
  PipGetInteger("ShrMem", &useShrMem);
#endif
  PipGetFloat("KFactorForFits", &kFactor);
  PipGetFloat("MaxResidualWeight", &maxMaxWeight);
  PipGetFloat("GoodEnoughError", &goodEnough);
  PipGetBoolean("UseHybridShifts", &mHybridShifts);
  PipGetFloat("FilterSigma1", &sigma1);
  PipGetFloat("FilterSigma2", &sigma2);
  PipGetFloat("FilterRadius1", &radius1);
  PipGetFloat("FilterRadius2", &radius2);
  PipGetInteger("RefineAlignment", &mRefineAtEnd);
  PipGetFloat("RefineRadius2", &refRadius2);
  PipGetInteger("AntialiasFilter", &antiFiltType);
  PipGetFloat("RingSpacingForFRC", &frcDeltaR);
  PipGetInteger("UseGPU", &mUseGPU);
  PipGetInteger("GroupSize", &mGroupSize);
  PipGetInteger("RefineWithGroupSums", &groupRefine);
  PipGetFloat("StopIterationsAtShift", &iterCrit);
  PipGetFloat("PixelSize", &optionPixSize);
  splineSmooth = PipGetInteger("MinForSplineSmoothing", &minNumForSpline);
  if (minNumForSpline < 8)
    splineSmooth = 0;
  B3DCLAMP(antiFiltType, 1, 6);
  if (mRefineAtEnd < 0)
    exitError("Entry for -refine cannot be negative");
  mNumAllVsAll = numAVAinput;
  if (mNumAllVsAll < 0) {
    if (numAVAinput < -4)
      exitError("The value for the -pair option cannot be more negative than -4");
    if (numAVAinput == -1)
      mNumAllVsAll = B3DMIN(MAX_ALL_VS_ALL, mMaxNumZ + 4);
    else
      mNumAllVsAll = B3DMAX(minFractionalAVA, 
                           (mMaxNumZ - 1 - numAVAinput) / (-numAVAinput));
  }
  mNumAllVsAll = B3DMIN(MAX_ALL_VS_ALL, mNumAllVsAll);
  if (mStartAssess > 0 && !mNumAllVsAll)
    exitError("You cannot set frames for assessing fits with cumulative correlations");
  if (reverse && (breakSetSize > 0 || extraHasTilts))
    exitError("You cannot process in reverse when breaking frames into sets");

  // Get default binning for size: set the target size bigger for K3
  minDiff = 1.e20;
  ierr = sqrt((double)mNx * mNy);
  if (!targetEntered && ((ierr < 6000 && ierr > 4500) || (ierr < 12000 && ierr > 9000)))
    targetAliSize *= 1.25;
  for (ind = 0; ind < (int)(sizeof(defaultBinnings) / sizeof(int)); ind++) {
    diff = fabs(targetAliSize - sqrt((double)mNx * mNy) / defaultBinnings[ind]);
    if (diff < minDiff) {
      minDiff = diff;
      alignBin = defaultBinnings[ind];
    }
  }

  // Set output size based on this binning of frames
  alignBinIn = alignBin;
  alsumBinEntered = 1 - PipGetTwoIntegers("AlignAndSumBinning", &alignBinIn, &sumBin);
  if (alignBinIn == 0 || sumBin < 1 || alignBinIn > 16 || sumBin > 16)
    exitError("Binning value is out of allowed range");
  if (alignBinIn > 0)
    alignBin = alignBinIn;
  if (sumRotationFlip < 0 || sumRotationFlip > 7)
    exitError("Inappropriate value of rotation and flip for sum entered");
  nxOut = nxSum = B3DCHOICE(sumRotationFlip % 2, mNy, mNx) / sumBin;
  nyOut = nySum = B3DCHOICE(sumRotationFlip % 2, mNx, mNy) / sumBin;

  // Get tilt angles from file
  readTiltAngleFile(tiltName);

  // Collect information from stack or mdoc file
  if (stackName) {
    stackFP = openAndReadHeader(stackName, &stackHead, "stack");
    stackMode = stackHead.mode;
    nxStack = stackHead.nx;
    nyStack = stackHead.ny;
    mMainHead = stackHead;
    mUnwgtHead = stackHead;
    mNumSect = stackHead.nz;
  } 

  // Get tilt angles from mdoc if not gotten yet, transfer titles
  if (mdocName) {
    if (stackName)
      exitError("You cannot enter both a corresponding stack and an mdoc file");
    getAnglesAndTitlesFromMdoc(tiltName, axisAngle);
  } else if (axisAngle > -990.) {
    for (outNum = 0; outNum < mNumOutFiles; outNum++)
      addAxisAngleTitle(outNum, axisAngle);
  }

  // Now for tilt angles from either a tilt file or an mdoc, deal with a mismatch between
  // angles and frame sets
  if (tiltName || mdocName) {
    handleTooManyTiltAngles(progname);
  }

  // Set default index to sets then see if need to reorder: test for monotonic already
  for (ind = 0; ind < mNumInFiles; ind++)
    setOrderIndex.push_back(ind);
  if (mTiltAngles.size() > 0 && reorderByTilt != 0) {
    allNeg = true;
    allPos = true;
    for (ind = 1; ind < mTiltAngles.size(); ind++) {
      if (mTiltAngles[ind] > mTiltAngles[ind - 1] + 0.01)
        allNeg = false;
      if (mTiltAngles[ind] < mTiltAngles[ind - 1] - 0.01)
        allPos = false;
    }
    
    // Do not reorder if already all negative and it is not forced by a 2, or already
    // all positive and it is not forced by -2
    if (!(allNeg && reorderByTilt < 2 ) && !(allPos && reorderByTilt > -2)) {
      rsSortIndexedFloats(&mTiltAngles[0], &setOrderIndex[0], mNumInFiles);
      changedSetOrder = true;
      if (reorderByTilt < 0) {
        for (ind = 0; ind < mNumInFiles / 2; ind++) {
          iz = setOrderIndex[ind];
          setOrderIndex[ind] = setOrderIndex[mNumInFiles - 1 - ind];
          setOrderIndex[mNumInFiles - 1 - ind] = iz;
        }
      }
    }
  }
  if (mTiltAngles.size() > 0)
    for (ind = 0; ind < mNumInFiles; ind++)
      orderedAngles.push_back(mTiltAngles[setOrderIndex[ind]]);

  // Make sure things work out
  if (stackName || (mdocName && !mDoingFrameTS)) {
    if (stackMode != MRC_MODE_BYTE && !enteredMode)
      outMode = stackMode;
    if (mNumSect < mNumInFiles && !tiltName)
      exitError("There are fewer sections in the %s (%d) than frame files or sets (%d)", 
                stackName ? "stack" : "mdoc file", mNumSect, mNumInFiles);
    if (mNumSect > mNumInFiles && !tiltName)
      printf("WARNING: %s - There are fewer frame sets or files (%d) than sections in "
             "the %s (%d)\n", progname, mNumInFiles, stackName ? "stack" : "mdoc file",
             mNumSect);
    
    // Figure out if size works, requires trimming, or implies a binning relative to stack
    if ((nxStack <= nxOut && nyStack > nyOut) || (nxStack > nxOut && nyStack <= nyOut))
      exitError("The image size from the %s is bigger in one dimension than the frame "
                "size", stackName ? "stack" : "mdoc file");
    ierr = 0;

    // Look for integer binning difference that matches in each direction
    // And make sure each size is close enough after scaling
    if (nxStack <= nxOut) {
      relXbin = B3DNINT((double)nxOut / nxStack);
      relYbin = B3DNINT((double)nyOut / nyStack);
      if (fabs((double)relXbin * nxStack - nxOut) > sizeDiffCrit * nxOut || 
          fabs((double)relYbin * nyStack - nyOut) > sizeDiffCrit * nyOut || 
          relXbin != relYbin)
        ierr = 1;
      relBinning = 1. / relXbin;
    } else {
      relXbin = B3DNINT((double)nxStack / nxOut);
      relYbin = B3DNINT((double)nyStack / nyOut);
      if (fabs((double)relXbin * nxOut - nxStack) > sizeDiffCrit * nxStack || 
          fabs((double)relYbin * nyOut - nyStack) > sizeDiffCrit * nyStack || 
          relXbin != relYbin)
        ierr = 1;
      relBinning = relXbin;
    }
    if (ierr) 
      exitError("The image size does not correspond well enough between the %s and the "
                "frames to deduce their relationship", stackName ? "stack" : "mdoc file");

    // For same binning, trim if there is a small difference
    if (relXbin == 1) {
      if (nxOut - nxStack > trimCrit || nyOut - nyStack > trimCrit) {
        printf("The image size from the %s is significantly smaller and frames will not "
               "be trimmed to that size\n", stackName ? "stack" : "mdoc file");
      } else if (nxStack < nxOut || nyStack < nyOut) {
        printf("The image size from the %s is slightly smaller and frames will be trimmed"
               " to that size\n", stackName ? "stack" : "mdoc file");
        nxOut = nxStack;
        nyOut = nyStack;
      }
    } else {

      // Different binnings: look at labels to try to adjust it there
      printf("The %s is at a different binning from the frames; frame sizes will not be"
             " adjusted\n", stackName ? "stack" : "mdoc file"); 
      for (outNum = 0; outNum < mNumOutFiles; outNum++) {
        for (ind = 0; ind < mOutHeads[outNum]->nlabl; ind++) {
          strncpy(title, (const char *)&mOutHeads[outNum]->labels[ind], MRC_LABEL_SIZE);
          title[MRC_LABEL_SIZE] = 0x00;
          if (adjustTitleBinning(title, sstr, relBinning, true)) {
            strncpy((char *)&mOutHeads[outNum]->labels[ind][0], &sstr[0], MRC_LABEL_SIZE);
            fixTitlePadding(&mOutHeads[outNum]->labels[ind][0]);
            break;
          }
        }
      }

      // Find and fix title in mdoc too
      if (adjustMdoc) {
        nz = AdocGetNumberOfSections("T");
        for (ind = 0; ind < nz; ind++) {
          if (AdocGetSectionName("T", ind, &filename) >= 0) {
            if (adjustTitleBinning(filename, sstr, relBinning, false)) {
              if (AdocChangeSectionName("T", ind, sstr.c_str()))
                exitError("Adjusting title with binning in mdoc file");
              free(filename);
              break;
            }
            free(filename);
          }
        }
      }
    }      
  }

  mGroupSize = B3DMAX(1, mGroupSize);
  if (mGroupSize > 1) {
    ierr = B3DMIN(mMaxNumZ, mNumAllVsAll + mGroupSize - 1) + 1 - mGroupSize;
    mUseBlockGroup = ((ierr + 1 - mGroupSize) * (ierr - mGroupSize)) / 2 < ierr;
    if (mUseBlockGroup)
      printf("Using block grouping; too few frames being fit for slide grouping\n");
    else if (mNumAllVsAll <= MAX_ALL_VS_ALL + 1 - mGroupSize)
      mNumAllVsAll += mGroupSize - 1;
  } else
    groupRefine = 0;

  // Set up the binnings to test
  mNumBinTests = 1;
  mNumFiltTests[0] = 1;
  numVaries = 0;
  varyRadius2[0][0] = radius2;
  varySigma2[0][0] = sigma2;
  ierr = 0;
  if (PipGetIntegerArray("TestBinnings", binsToTest, &ierr, MAX_BINNINGS) == 0) {
    mNumBinTests = ierr;
    for (ind = 0; ind < mNumBinTests; ind++) {
      if (binsToTest[ind] < 1 || binsToTest[ind] > 16)
        exitError("Binning value %d not allowed", binsToTest[ind]);
      ACCUM_MIN(mMinBinningToTest, binsToTest[ind]);
    }
  } else {
    if (!alsumBinEntered || alignBinIn < 0)
      printf("Selected the default binning of %d for this image size\n", alignBin);
    binsToTest[0] = alignBin;
    mMinBinningToTest = alignBin;
  }

  // Get the filters to test
  PipNumberOfEntries("VaryFilter", &numVaries);
  
  for (ind = 0; ind < mNumBinTests; ind++) {
    if (ind < numVaries) {
      mNumFiltTests[ind] = 0;
      PipGetFloatArray("VaryFilter", &varyRadius2[ind][0], &mNumFiltTests[ind],
                       MAX_FILTERS);
      if (!numAVAinput && mNumFiltTests[ind] > 1)
        exitError("You cannot vary filter values when doing cumulative alignment");
      rsSortFloats(&varyRadius2[ind][0], mNumFiltTests[ind]);
      useInd = ind;
    } else {
      useInd = B3DMAX(0, numVaries - 1);
      mNumFiltTests[ind] = mNumFiltTests[useInd];
    }
    for (iz = 0; iz < mNumFiltTests[ind]; iz++) {
      varyRadius2[ind][iz] = varyRadius2[useInd][iz];
      varySigma2[ind][iz] = B3DNINT(SIG2_ROUND_FAC * sigma2 * varyRadius2[ind][iz] /
                                    radius2) / SIG2_ROUND_FAC;
      numTimesBest[ind][iz] = 0;
    }
  }

  if (!refRadius2)
    refRadius2 = varyRadius2[0][0];
  refSigma2 = B3DNINT(SIG2_ROUND_FAC * sigma2 * refRadius2 / radius2) / SIG2_ROUND_FAC;

  //
  numDriftLoop = 1;
  PipGetTwoFloats("DriftLimitDistAndNumber", &driftMaxDist, &driftMaxFracNum);
  if (driftMaxDist > 0.) {
    if (mNumBinTests > 1)
      exitError("You cannot enter -drift with test binnings");
    numDriftLoop = 2;
    if (driftMaxFracNum <= 0.)
      exitError("The maximum fraction or number of initial frames to drop must be "
                "positive");
    if (driftMaxFracNum < 1. && driftMaxFracNum >= 0.5)
      exitError("Maximum fraction of initial frames to drop must be less than 0.5");
  }

  // Get the gain reference, dark referemce, and camera defects
  getGainDarkDefects(useShrMem);

  // Handle scaling
  if ((mGainName || mExtraHasGainRef) && mInHead.mode == MRC_MODE_BYTE && !enteredScale &&
      !totalScale && !scaleToMeanSD && outMode != MRC_MODE_FLOAT) {
    printf("Applying default total scaling of %g because byte values are being "
           "gain-normalized\n", mDefaultByteScale);
    totalScale = mDefaultByteScale;
  }
  if (totalScale > 0.)
    scale = totalScale / alreadyScaledBy;
      
  // Get sizes
  sFA.getPadSizesBytes(mNx, mNy, fullTaperFrac, sumBin, mMinBinningToTest, mFullPadSize, 
                       mSumPadSize, mAlignPadSize);

  // See about GPU
  mFullDataSize = sizeof(float);
  if (mUseGPU && taperFrac <= 0. && trimFrac <= 0.) {
    printf("The GPU cannot be used when the taper fraction is set to 0\n");
      mUseGPU = -1;
  }

  mDoSpline = (splineSmooth && mMaxNumZ >= minNumForSpline) ? 1 : 0;
  assessGpuNeeds(useShrMem, frcName);
  if (mGpuFlags && mSuperFacForDefects > 0)
    mGpuFlags |= mSuperFacForDefects > 2 ? GPU_AVG_SUPER_4X : GPU_AVG_SUPER_2X;

  // FRC output file
  if (frcName) {
    imodBackupFile(frcName);
    frcFP = fopen(frcName, "w");
    if (!frcFP)
      exitError("Opening file for FRC curves, %s", frcName);
    free(frcName);
  }

  // Shift output file
  if (PipGetString("PlottableShiftFile", &extraName) == 0) {
    imodBackupFile(extraName);
    plotFP = fopen(extraName, "w");
    if (!plotFP)
      exitError("Opening file for shift curves, %s", extraName);
    free(extraName);
  }
  PipDone();

  if (!mTestMode) {

    // Set up output header(s)
    for (outNum = 0; outNum < mNumOutFiles; outNum++) {
      headPtr = mOutHeads[outNum];
      headPtr->nz = numFilesToDo;
      headPtr->mz = numFilesToDo;
      headPtr->nx = nxOut;
      headPtr->ny = nyOut;
      headPtr->mx = headPtr->nx;
      headPtr->my = headPtr->ny;
      headPtr->mode = outMode;
      headPtr->amax = -1.e30;
      headPtr->amin = 1.e30;
      headPtr->amean = 0.;
      if (xScale == 1.0 && extraHasAxisPix)
        xScale = yScale = zScale = extraPixSize;
      else if (xScale == 1.0 && stackName) {
        stackBinX = nxOut * sumBin / stackHead.nx;
        stackBinY = nyOut * sumBin / stackHead.ny;
        stackBin = B3DNINT(stackBinX);
        if (fabs(B3DNINT(stackBinX) - stackBinX) < 0.05 && 
            fabs(B3DNINT(stackBinY) - stackBinY) < 0.05 && stackBin == B3DNINT(stackBinY))
          mrc_get_scale(&stackHead, &xScale, &yScale, &zScale);
      }
      if (optionPixSize > 0.)
        xScale = yScale = zScale = 10. * optionPixSize;
      mrc_set_scale(headPtr, sumBin * xScale, sumBin * yScale, sumBin * zScale);

      // 11/4/20: Axis rotation was already output, no need to do here if fileHasAxisPix

      if (sumBin > 1)
        sprintf(title, "alignframes: summed frames scaled by %g, reduced %d", scale, 
                sumBin);
      else
        sprintf(title, "alignframes: summed frames scaled by %g", scale);
      mrc_head_label(headPtr, title);
      mrcInitOutputHeader(headPtr);
    
      // Set up output file(s) and li for writing
      imodBackupFile(mOutNames[outNum]);
      outFPs[outNum] = iiFOpen(mOutNames[outNum], "wb");
      if (!outFPs[outNum])
        exitError("Opening output file %s", mOutNames[outNum]);

      // Also create an .openTS file if it seems to be a tilt series
      if (mTiltAngles.size() > 0 || stackName || mDoingFrameTS) {
        openTsNames[outNum] = B3DMALLOC(char, strlen(mOutNames[outNum]) + 10);
        if (openTsNames[outNum]) {
          sprintf(openTsNames[outNum], "%s.openTS", mOutNames[outNum]);
          extraFP = fopen(openTsNames[outNum], "w");
          if (extraFP)
            fclose(extraFP);
        }
      }
      
      mrc_init_li(&li, NULL);
      mrc_init_li(&li, headPtr);
      headPtr->fp = outFPs[outNum];
  
      // Need to test for output file type 
      if (b3dOutputFileType() == OUTPUT_TYPE_MRC) {
        if (stackName && stackHead.next && !mTiltAngles.size()) {
          headPtr->next = stackHead.next;
          headPtr->nint = stackHead.nint;
          headPtr->nreal = stackHead.nreal;
          headPtr->headerSize = stackHead.headerSize;
          if (mrcCopyExtraHeader(&stackHead, headPtr))
            exitError("Copying extended header from stack to output file");
          iiFClose(stackFP);
        } else if (mTiltAngles.size() > 0) {
          mNumSect = mTiltAngles.size();
          headPtr->next = 4 * B3DMIN(mNumSect, numFilesToDo);
          headPtr->nint = 0;
          headPtr->nreal = 1;
          headPtr->headerSize += headPtr->next;
          if (b3dFseek(outFPs[outNum], 1024, SEEK_SET) ||
              (int)b3dFwrite(&orderedAngles[startingFile - 1], 1, headPtr->next, 
                          outFPs[outNum]) != headPtr->next)
            exitError("Writing tilt angles to extended header of output file");
        }
      }

      // Do modifications to the mdoc
      if (!outNum && adjustMdoc && !mDoingFrameTS && 
          (AdocSetKeyValue(ADOC_GLOBAL_NAME, 0, "ImageFile", mOutNames[0]) ||
           AdocSetTwoIntegers(ADOC_GLOBAL_NAME, 0, "ImageSize", nxOut, nyOut) ||
           AdocSetInteger(ADOC_GLOBAL_NAME, 0, "DataMode", outMode) ||
           AdocGetFloat(ADOC_GLOBAL_NAME, 0, "PixelSpacing", &pixTemp) ||
           AdocSetFloat(ADOC_GLOBAL_NAME, 0, "PixelSpacing", pixTemp * relBinning)))
        exitError("Adjusting global values in mdoc");
    }
  }

  // Get data
  summed = B3DMALLOC(float, (mNx / sumBin) * (mNy / sumBin));
  if (mNumOutFiles > 1)
    unwgtSum = B3DMALLOC(float, (mNx / sumBin) * (mNy / sumBin));
  if (!summed || (mNumOutFiles > 1 && !unwgtSum))
    exitError("Allocating memory for summed slice");
  if (sumRotationFlip) {
    rotSum = B3DMALLOC(float, (mNx / sumBin) * (mNy / sumBin));
    if (!rotSum)
      exitError("Allocating memory for rotating summed slice");
  }

  // Loop on frame files or frame sets
  superFile = 0;
  setInFile = 0;
  numSets = 0;
  for (ifile = startingFile - 1; ifile < endingFile; ifile++) {
    minError = 1.e30;
    originalZval = ifile;
    if (combineFiles) {
      balancedGroupLimits(numSingleFiles, mNumInFiles, ifile, &startCombine, 
                          &endCombine);
      filename = mInFiles[startCombine];
    } if (breakSetSize > 0 || extraHasTilts || mDoingFrameTS) {
      filename = mInFiles[superFile];
    } else {
      filename = mInFiles[setOrderIndex[ifile]];
      originalZval = setOrderIndex[ifile];
    }

    // Open file if it is time to do so
    if (!setInFile) {
      mInFP = openAndReadHeader(filename, &mInHead, "input image");
      checkInputFile(filename, &mInHead, mNx, mNy, combineFiles);
      nz = mInHead.nz;
      mParallelRead = false;

      // If not combining files, see if the file is a TIFF for parallel reading
      if (!combineFiles && maxReadThreads > 1) {
        mFileCopies[0] = iiLookupFileFromFP(mInFP);
        if (mFileCopies[0] && mFileCopies[0]->file == IIFILE_TIFF) {
          mNumReadThreads = iiOpenCopiesForThreads(mFileCopies, maxReadThreads);
          mParallelRead = mNumReadThreads > 1;
        }
      }

      // Get gain reference if it is in there
      if (mExtraHasGainRef) {
        if (b3dFseek(mInFP, MRC_HEADER_SIZE + 4 * (mInHead.nint + mInHead.nreal) * nz, 
                     SEEK_SET) || 
            (int)b3dFread(mGainSlice->data.f, 4, mNx * mNy, mInFP) != mNx *mNy)
          exitError("Reading gain reference from extended header");
        mNxGain = mNx;
        mNyGain = mNy;
        if (mRotationFlip > 0)
          rotateFlipGainReference(mGainSlice->data.f);
      }

      // Set or get group limits
      if (breakSetSize > 0 && !combineFiles) {
        numFrameUse = nz;
        if (startFrame > 0)
          numFrameUse = B3DMIN(nz, endFrame) + 1 - startFrame;
        numSets = numFrameUse / breakSetSize;

        // Get the set limits and then adjust by the start frame
        mSetStarts.clear();
        mNumInSets.clear();
        for (ind = 0; ind < numSets; ind++) {
          balancedGroupLimits(numFrameUse, numSets, ind, &startCombine, &endCombine);
          mSetStarts.push_back(startCombine);
        }
        mSetStarts.push_back(endCombine + 1);
        for (ind = 0; ind < numSets; ind++)
          mNumInSets.push_back(mSetStarts[ind + 1] - mSetStarts[ind]);
        if (startFrame > 1)
          for (ind = 0; ind <= numSets; ind++)
            mSetStarts[ind] += startFrame - 1;
        
      } else if (mDoingFrameTS) {
        numSets = mSetStarts.size();
        setInFile = ifile;

      } else if (extraHasTilts) {
        ierr = analyzeExtraHeader(mInFP, &mInHead, startFrame, endFrame, false,
                                  &extraBuf, extraBufSize, extraTilts, 
                                  minSet, maxSet, fileAxis, filePix);
        numSets = extraTilts.size();
        setInFile = ifile;
      }
    }

    // Now proceed with the current file/set of frames
    if (combineFiles) {
      nz = endCombine + 1 - startCombine;
      fclose(mInFP);
    } else if (numSets) {
      ind = setInFile;
      if ((breakSetSize > 0 || extraHasTilts || mDoingFrameTS) && numSingleFiles == 1) {
        ind = setOrderIndex[setInFile];
        originalZval = setOrderIndex[ifile];
      }
      startCombine = mSetStarts[ind];
      nz = mNumInSets[ind];
      endCombine = startCombine + nz - 1;
    }
    extractFileTail(filename, sstr);

    analyzeForPartialFrames(nz, startCombine, endCombine, ifile, skippedFrame);

    for (driftLoop = 0; driftLoop < numDriftLoop; driftLoop++) {
      nzAlign = nz;
      if (startFrame > 0 && !numSets)
        nzAlign = B3DMIN(nz, endFrame) + 1 - startFrame;
      numAVAuse = numAVAinput;
      if (numAVAuse == -1 || numAVAuse > nzAlign)
        numAVAuse = nzAlign;
      else if (numAVAuse < 0)
        numAVAuse = B3DMAX(minFractionalAVA, (nzAlign - 1 - numAVAinput) /(-numAVAinput));
      numAVAuse = B3DMIN(MAX_ALL_VS_ALL, numAVAuse);

      // Make per-file decision on grouping
      blockGrpSize = slideGrpSize = 1;
      if (mGroupSize > 1) {
        ierr = nzAlign + 1 - mGroupSize;
        if (mUseBlockGroup || 
            ((ierr + 1 - mGroupSize) * (ierr - mGroupSize)) / 2 < ierr) {
          blockGrpSize = mGroupSize;
          if (!mUseBlockGroup)
            printf("Using block grouping instead of sliding grouping for file # %d\n",
                   ifile + 1);
        } else {
          slideGrpSize = mGroupSize;
          if (numAVAuse <= MAX_ALL_VS_ALL + 1 - mGroupSize)
            numAVAuse += mGroupSize - 1;
        }
      }

      dataSizeForMode(mInHead.mode, &mInDataSize, &ind);
      needAlloc = mInDataSize * mNx * mNy;
      if (needAlloc > bufAllocSize) {
        readBuf = B3DMALLOC(unsigned char, needAlloc);
        if (blockGrpSize > 1)
          sumBuf = B3DMALLOC(unsigned char, needAlloc);
        if (!readBuf || (blockGrpSize > 1 && !sumBuf))
          exitError("Allocating memory for reading and/or grouping frames");
        bufAllocSize = needAlloc;
      }

      nzAlign = B3DMAX(1, nzAlign / blockGrpSize);
    
      // Set up for spline scaling if criteria met
      mDoSpline = 0;
      if (splineSmooth > 0 && nzAlign >= minNumForSpline)
        mDoSpline = 1;
                 
      // Estimate memory usage
      // Does summing in two passes work with cumulative alignment?
      tmean = sFA.totalMemoryNeeds(mFullPadSize, mFullDataSize, mSumPadSize,mAlignPadSize,
                                   numAVAuse, nzAlign, mRefineAtEnd, mNumBinTests,
                                   mNumFiltTests[0], mHybridShifts, mGroupSize, mDoSpline,
                                   mGpuFlags, mDeferSum, mTestMode, mStartAssess,
                                   mSumInOnePass, mNumHoldFull);
      if (tmean > mMemoryLimit) {
        if (mStartAssess >= 0) 
          exitError("The memory limit is too low to allow initial assessment and summing"
                    " of %d frames in one pass %s", nzAlign, mRefineAtEnd || mDoSpline ? 
                    "with refinement or smoothing at the end" : 
                    "with this many pairwise comparisons");
        if (mSumInOnePass) {
          mSumInOnePass = false;
          if (!warnedTwoPass)
            printf("Using two passes: a single pass with %d frames requires %.1f GB, "
                   "above the limit of %.1f GB\n", nzAlign, tmean, mMemoryLimit);
          warnedTwoPass = true;
          fflush(stdout);
        }
      }
      numTestLoops = mNumBinTests + B3DCHOICE(mSumInOnePass || mTestMode, 0, 1);

      // Loop on conditions
      wasGoodEnough = false;
      for (itest = 0; itest < numTestLoops; itest++) {

        // Set summing flag for summing with alignment if it can be done, otherwise set
        // for sum only on final loop unless assessing from subset, otherwise skip the sum
        if (mSumInOnePass)
          summingMode = 0;
        else if (itest == mNumBinTests)
          summingMode = mStartAssess >= 0 ? 0 : -1;
        else
          summingMode = 1;

        // Set limits and indices for the extra loop; if it has to compute alignment 
        // because it was assessed on a subset, then it needs either best filter or the 
        // whole set to do hybrid
        useStart = numSets ? 0 : startFrame;
        useEnd = endFrame;
        if (itest == mNumBinTests) {
          indBinUse = indBestBin;
          if (mHybridShifts) {
            indFiltUse = 0;
            numFiltUse = mNumFiltTests[itest];
          } else {
            indFiltUse = indBestFilt;
            numFiltUse = 1;
          }
        } else {

          // Otherwise set up for this round
          indBinUse = itest;
          indFiltUse = 0;
          numFiltUse = mNumFiltTests[itest];
          if (mStartAssess >= 0) {
            useStart = mStartAssess;
            useEnd = endAssess;
          }
        }
        if (mDebug % 10)
          PRINT4(itest, summingMode, indBinUse, indFiltUse);

        // Set up actual frame limits for this file
        zDir = reverse ? -1 : 1;
        if (useStart > 0) {
          zStart = B3DCHOICE(reverse, B3DMIN(useEnd, nz) - 1, useStart - 1);
          zEnd = B3DCHOICE(reverse, useStart - 1, B3DMIN(useEnd, nz) - 1);
        } else {
          zStart = reverse ? nz - 1 : 0;
          zEnd = reverse ? 0 : nz - 1;
        }
        numFetch = zDir * (zEnd - zStart) + 1;
        nzAlign = B3DMAX(1, numFetch / blockGrpSize);
        if (numAVAuse < 2 + slideGrpSize)
          numFiltUse = 1;

        // Get file, initialize
        if (useShrMem) {
#ifdef TEST_SHRMEM        
          ierr = mSMC.initialize
            (sumBin, binsToTest[indBinUse], trimFrac, numAVAuse, 
             mRefineAtEnd, mHybridShifts, 
             (summingMode == 0 && (mDeferSum || mDoSpline)) ? 1 : 0,
             slideGrpSize, mNx, mNy,
             fullTaperFrac, taperFrac, antiFiltType - 1, radius1, 
             &varyRadius2[indBinUse][indFiltUse], sigma1, 
             &varySigma2[indBinUse][indFiltUse], numFiltUse, shiftLimit,
             kFactor, maxMaxWeight, summingMode, nzAlign, mNumOutFiles > 1,
             mGpuFlags, mDebug);
#endif        
        } else {
          ierr = sFA.initialize
            (sumBin, binsToTest[indBinUse], trimFrac, numAVAuse, 
             mRefineAtEnd, mHybridShifts, 
             (summingMode == 0 && (mDeferSum || mDoSpline)) ? 1 : 0,
             slideGrpSize, mNx, mNy,
             fullTaperFrac, taperFrac, antiFiltType - 1, radius1, 
             &varyRadius2[indBinUse][indFiltUse], sigma1, 
             &varySigma2[indBinUse][indFiltUse], numFiltUse, shiftLimit,
             kFactor, maxMaxWeight, summingMode, nzAlign, mNumOutFiles > 1,
             mGpuFlags, mDebug);
        }
        if (ierr)
          exitError("Error %d initializing frame summing for file # %d", ierr, ifile + 1);

        // Initialize dose weighting: start by getting doses for all underlying frames
        if ((mTotalDose > 0. || mDoseFileType > 0) && !mTestMode) {
          sumOfFrames = 0;
          if (mDoseFileType > 3 && !mDoingFrameTS) {
            expandFrameDosesNumbers(mFrameDoseLines[originalZval], &mTempVal1[0], nz,
                                    sumOfFrames, sumOfDoses, &mFrameDoses[0]);
            if (sumOfFrames > 0 && sumOfFrames < nz)
              printf("WARNING: %s - The frame doses and numbers for file # %d include too"
                     " few frames (%d vs %d)", progname, ifile + 1, sumOfFrames, nz);
          }

          // Fall back to equal division
          if (sumOfFrames < nz)
            for (iz = 0; iz < nz; iz++)
              mFrameDoses[iz] = mTotalDoseVec[originalZval] / nz;

          // Combine doses for grouped frames and frames actually being used
          mTempVal1.resize(nzAlign);
          priorTemp = mInitialDose + mPriorDoseVec[originalZval];
          if (useStart > 0)
            for (iz = 0; iz < useStart; iz++)
              priorTemp += mFrameDoses[iz];
          for (group = 0; group < nzAlign; group++) {
            frameGroupLimits(numFetch, nzAlign, group, groupStart, groupEnd, zStart, zDir,
                             izLow, izHigh);
            mTempVal1[group] = 0.;
            for (iz = izLow; zDir * (iz - izHigh) <= 0; iz += zDir)
              mTempVal1[group] += mFrameDoses[iz];
          }
          if (mDebug % 10) {
            printf("Prior dose %.3f   total dose %.3f  frame doses:\n", priorTemp,
                   mTotalDoseVec[originalZval]);
            for (iz = 0; iz < nzAlign; iz++) {
              printf(" %.3f", mTempVal1[iz]);
              if ((iz + 1) % 12 == 0 || iz == nzAlign -1)
                printf("\n");
            }
          }
          ierr = sFA.setupDoseWeighting(priorTemp, &mTempVal1[0], xScale, mDoseScaling,
                                        doseAfac, doseBfac, doseCfac, mReweightFilt, iz);
          if (ierr)
            exitError("Error %d setting up dose weighting for file # %d", ierr, ifile +1);
        }
    
        // Loop on frames in selected order, but do groups backwards to put largest at end
        for (group = 0; group < nzAlign; group++) {
          frameGroupLimits(numFetch, nzAlign, group, groupStart, groupEnd, zStart, zDir,
                           izLow, izHigh);
          useBuf = readBuf;
          useMode = mInHead.mode;
          for (iz = izLow; zDir * (iz - izHigh) <= 0; iz += zDir) {
            if (combineFiles) {
              mInFP = openAndReadHeader(mInFiles[startCombine + iz], &mInHead,
                                        "input image");
              checkInputFile(mInFiles[startCombine + iz], &mInHead, mNx, mNy, 
                             combineFiles);
              if (mrc_read_slice(readBuf, mInFP, &mInHead, 0, 'Z'))
                exitError("Reading from file # %d: %s", startCombine + iz, 
                          mInFiles[startCombine + iz]);
              fclose(mInFP);
            } else {
              izRead = iz;
              if (numSets)
                izRead += startCombine;

              for (ind = 0; ind < 3; ind++)
                if (izRead == mZinPartialBufs[ind])
                  useBuf = mPartialScanBufs[ind];
              if (ind > 2)
                readOneFrame(readBuf, izRead, ifile);
            }
            if (izLow != izHigh) {
              if (iz == izLow)
                memset(sumBuf, 0, needAlloc);
              addToSumBuffer(useBuf, mInHead.mode, sumBuf, useMode, mNx * mNy);
              useBuf = sumBuf;
            }
          }

          // Get the truncation limit set on first, and on second one also if frame sets
          if (!group || (group == 1 && mDoingFrameTS)) {
            ierr = sFA.setTruncationLimit(useBuf, mNx, mNy, useMode, mTruncLimit, truncUse);
            if (ierr)
              exitError(ierr == 1 ? "Allocating line pointers for analyzing truncation limit" :
                        "Error computing mean and SD with sampling for setting truncation "
                        "limit");
          }

          // Pass the frame
          if (useShrMem) {
#ifdef TEST_SHRMEM        
            ierr = mSMC.nextFrame(useBuf, useMode, mGainSlice ? mGainSlice->data.f : NULL,
                                  mNxGain, mNyGain, 
                                  truncUse, mDefectString, mCamSizeX, mCamSizeY, 
                                  -1, bestXshifts[group], bestYshifts[group]);
#endif
          } else {
            ierr = sFA.nextFrame(useBuf, useMode, mGainSlice ? mGainSlice->data.f : NULL,
                                 mNxGain, mNyGain, mDarkSlice ? mDarkSlice->data.f : NULL,
                                 truncUse, &mDefects, mCamSizeX, mCamSizeY, 
                                 mCorDefBinning, bestXshifts[group], bestYshifts[group]);
          }
          if (ierr)
            exitError("Error %d processing frame/group %d from %s %d", ierr, group,
                      numSets ? "set" : "file", ifile + 1);
        }
        numDone = nzAlign;
        ierr = B3DMIN(numAVAuse, numDone) + 1 - mGroupSize;
        doRobust = ((ierr + 1 - mGroupSize) * (ierr - mGroupSize)) / 2 >= 2 * ierr &&
          kFactor > 0;

        // Finish up and get results; 
        if (useShrMem) {
#ifdef TEST_SHRMEM        
          float *alisum;
          ierr = mSMC.finishAlignAndSum
            (mNx / sumBin, mNy / sumBin, MAX_FILTERS + 1, 510, refRadius2, refSigma2,
             iterCrit, groupRefine, mDoSpline, &alisum, xShifts, yShifts,
             rawXshifts, rawYshifts, ringCorrs, frcDeltaR,
             faBestFilt, smoothDist, rawDist, resMean,
             resSD, meanResMax, maxResMax, meanRawMax, maxRawMax);
          memcpy(summed, alisum, 4 * (mNx / sumBin) * (mNy / sumBin));
#endif
        } else {
          ierr = sFA.finishAlignAndSum(refRadius2, refSigma2, iterCrit, groupRefine,
                                       mDoSpline, summed, xShifts, yShifts, rawXshifts,
                                       rawYshifts, ringCorrs, frcDeltaR, faBestFilt,
                                       smoothDist, rawDist, resMean, resSD, meanResMax,
                                       maxResMax, meanRawMax, maxRawMax);
        }
        if (ierr == 3)
          exitError("An unrecoverable error in GPU processing occurred for %s # %d",
                    numSets ? "set" : "file", ifile);
        else if (ierr)
          exitError("No frames were aligned for file # %d", ifile);

        // Evaluate whether to drop initial frames due to excessive shift
        excludingInitial = false;
        if (!driftLoop && numDriftLoop == 2) {
          maxExclude = B3DNINT(driftMaxFracNum >= 1. ? driftMaxFracNum : 
                               driftMaxFracNum * numDone);
          maxExclude = B3DMIN(maxExclude, B3DNINT(0.5 * numDone));
          for (ix = 1; ix <= maxExclude; ix++) {
            priorTemp = sqrt(pow(rawXshifts[ix] - rawXshifts[ix - 1], 2.f) +
                             pow(rawYshifts[ix] - rawYshifts[ix - 1], 2.f));
            if (priorTemp < driftMaxDist)
              break;
            if (!excludingInitial)
              printf("%s %d: drop frame (drift):", numSets ? "Set" : "File", ifile + 1);
            printf(" %d (%.1f) ", startCombine + 1, priorTemp);
            nzAlign--;
            startCombine++;
            excludingInitial = true;
          }
          nz = nzAlign;
          if (excludingInitial) {
            printf("\n");
            break;
          }
        }

        // Do the basic summary report starting with the header line
        if (itest < mNumBinTests) {
          numFetch = 1;
          if (numAVAuse && numFiltUse > 1)
            numFetch = numFiltUse + 1;
          if (!itest) {
            printf("%s %d (%s): %d frames", numSets ? "Set" : "File", ifile + 1,
                   sstr.c_str(), zDir * (zEnd - zStart) + 1);
            if (numSets) {
              printf(" from %d to %d", startCombine + 1, endCombine + 1);
              if (skippedFrame[0] >= 0) {
                printf(" (skip %d", skippedFrame[0] + 1);
                if (skippedFrame[1] >= 0)
                  printf(" %d", skippedFrame[1] + 1);
                printf(")");
              }
            }
            if (orderedAngles.size() > 0)
              printf("   (%.1f deg)", orderedAngles[ifile]);
            printf("\n");
          }
        
          // Report residuals and total distance
          for (filt = 0; filt < numFetch; filt++) {
            if (B3DMIN(numAVAuse, numDone) >= 3) {
              if (mNumBinTests * mNumFiltTests[0] > 1) {
                if (filt < mNumFiltTests[itest] || mNumFiltTests[itest] == 1)
                  printf("Results with bin = %d  rad2 = %.3f  sig2 = %.4f\n", 
                         binsToTest[itest], varyRadius2[itest][filt],
                         varySigma2[itest][filt]);
                else
                  printf("Hybrid results,  bin = %d\n", binsToTest[itest]);
              } 
              if (mNumBinTests * mNumFiltTests[0] > 1 || numSummaryLines > 1)
                printf("  %sesidual mean = %.3f, SD = %.3f, mean max = %.2f, max max "
                       "= %.2f\n", doRobust ? "Weighted r" : "R", resMean[filt],
                       resSD[filt], meanResMax[filt], maxResMax[filt]);
              else
                printf(" %sesidual mean = %.3f, max max = %.2f", 
                       doRobust ? "Weighted r" : "R", resMean[filt], maxResMax[filt]);
              
            } 
            if (mNumBinTests * mNumFiltTests[0] > 1 || numSummaryLines > 1) {
              if (doRobust && numAVAuse)
                printf("  Max unweighted resid mean = %.2f, max = %.2f ", 
                       meanRawMax[filt], maxRawMax[filt]);
              else
                printf("                                               ");
            }
            printf("  Dist = %.2f, smoothed = %.2f\n", rawDist[filt], smoothDist[filt]);

            // Report shifts for frame tilt series
            if (filt == numFetch - 1 && mDoingFrameTS && !suppressInitialShifts) {
              printf("  Initial raw inter-frame shifts:");
              for (ix = 1; ix < B3DMIN(numDone, 5); ix++)
                printf("   %.1f", sqrt(pow(rawXshifts[ix] - rawXshifts[ix - 1], 2.f) +
                                       pow(rawYshifts[ix] - rawYshifts[ix - 1], 2.f)));
              printf("\n");
            }
            fflush(stdout);
          }
        }
        
        // Keep track of best binning, copy shifts from it
        copyShifts = itest == mNumBinTests && summingMode == 0;
        error = resMean[faBestFilt] * (1. - maxMaxWeight) + 
          maxResMax[faBestFilt] * maxMaxWeight;
        if (error < minError && itest < mNumBinTests) {
          indBestBin = itest;
          indBestFilt = faBestFilt;
          minError = error;
          minMean = resMean[faBestFilt];
          copyShifts = true;
        }
        if (copyShifts) {
          if (mDebug % 10)
            printf("Copy shifts test %d\n", itest);
          for (ix = 0; ix < numDone; ix++) {
            bestXshifts[ix] = xShifts[ix];
            bestYshifts[ix] = yShifts[ix];
            bestXraw[ix] = rawXshifts[ix];
            bestYraw[ix] = rawYshifts[ix];
            if (mDebug % 10)
              printf("%.2f  %.2f\n", xShifts[ix], yShifts[ix]);
          }
        }

        // If the error is now good enough, advance to the end of the test runs
        if (minError <= goodEnough && itest < mNumBinTests - 1) {
          itest = mNumBinTests - 1;
          wasGoodEnough = true;
        }

      }  // End of test loop
      if (numDriftLoop > 1 && !excludingInitial)
        break;
    } // End of drift loop

    // Pick up the unweighted sum
    if (mNumOutFiles > 1) {
      if (sFA.getUnweightedSum(unwgtSum))
        exitError("getting non-dose-weighted sum");
    }

    // Advance set number and wrap it back to 0 after last file; close file when needed
    if (numSets) {
      setInFile++;
      if (setInFile == numSets) {
        superFile++;
        setInFile = 0;
      }
    }
    if (!combineFiles && !setInFile) {
      if (mParallelRead)
        for (ind = 1; ind < mNumReadThreads; ind++)
          iiDelete(mFileCopies[ind]);
      iiFClose(mInFP);
      if (mDebug && mParallelRead)
        PRINT2(mNumReadThreads, mWallRead);
    }

    // Write out data at end of loop
    if (summingMode <= 0) {
      if ((mDebug % 10) > 1 && mGettingFRC)
        for (ix = 0; ix < (int)floor(0.5 / frcDeltaR); ix++)
          printf("%.4f  %.5f\n", (ix + 0.5) * frcDeltaR, ringCorrs[ix]);

      for (outNum = 0; outNum < mNumOutFiles; outNum++) {
        headPtr = mOutHeads[outNum];
        useSum = outNum ? unwgtSum : summed;
        if (sumRotationFlip) {
          rotateFlipImage(useSum, MRC_MODE_FLOAT, mNx / sumBin, mNy / sumBin, 
                          sumRotationFlip, 0, 0, 0, rotSum, &ix, &iy, 0);
          useSum = rotSum;
        }

        // Trim if size is over
        ix = (nxSum - nxOut) / 2;
        iy = (nySum - nyOut) / 2;
        if (ix || iy)
          extractWithBinning(useSum, SLICE_MODE_FLOAT, nxSum, ix, ix + nxOut - 1, iy, 
                             iy + nyOut - 1, 1, useSum, 0, &iz, &ierr);
      
        // Scale the data and/or apply scale for binning
        if (scale != 1. || sumBin > 1)
          for (iz = 0; iz < headPtr->nx * headPtr->ny; iz++)
            useSum[iz] *= scale * sumBin * sumBin;
      
        // Manage header mmm and write the data
        if (scaleToMeanSD && sdScale > 0.)
          arrayMinMaxMeanSd(useSum, headPtr->nx, headPtr->ny, 0, headPtr->nx - 1, 0,
                            headPtr->ny - 1, &tmin, &tmax, &diff, &minDiff, &tmean, &tsd);
        else
          arrayMinMaxMean(useSum, headPtr->nx, headPtr->ny, 0, headPtr->nx - 1, 0, 
                          headPtr->ny - 1, &tmin, &tmax, &tmean);

        // If scaling to mean/sd, set scaling by either the mean or the SD and added 
        // factor as appropriate to match means, scale data and adjust min/max/mean
        if (scaleToMeanSD) {
          if (sdScale > 0.) {
            scaleFac = sdScale / tsd;
            addFac = meanScale - scaleFac * tmean;
          } else {
            scaleFac = meanScale / tmean;
            addFac = 0.;
          }
          for (iz = 0; iz < headPtr->nx * headPtr->ny; iz++)
            useSum[iz] = scaleFac * useSum[iz] + addFac;
          tmean = tmean * scaleFac + addFac;
          tmin = tmin * scaleFac + addFac;
          tmax = tmax * scaleFac + addFac;
        }
        ACCUM_MIN(headPtr->amin, tmin);
        ACCUM_MAX(headPtr->amax, tmax);
        headPtr->amean += tmean / numFilesToDo;
        ierr = mrcWriteZFloat(headPtr, &li, useSum, ifile);
        if (ierr)
          exitError("Writing summed data to file for input file # %d (error # %d)", 
                    ifile + 1, ierr);
        if (adjustMdoc && !outNum) {
          if (!mDoingFrameTS) {
            if (AdocGetFloat(ADOC_ZVALUE_NAME, originalZval, "PixelSpacing", &pixTemp))
              exitError("Getting pixel spacing in mdoc for output image");
            if (AdocSetFloat(ADOC_ZVALUE_NAME, originalZval, "PixelSpacing", 
                             pixTemp * relBinning))
              exitError("Adjusting pixel spacing in mdoc for output image");
            if (AdocSetThreeFloats(ADOC_ZVALUE_NAME, originalZval, "MinMaxMean", tmin,
                                   tmax, tmean))
              exitError("Adjusting pixel spacing in mdoc for output image");

            // Binning was a later addition so allow it not to exist
            ierr = AdocGetFloat(ADOC_ZVALUE_NAME, originalZval, "Binning", &pixTemp);
            if (ierr < 0)
              exitError("Getting binning in mdoc for output image");
            if (!ierr) {
              pixTemp *= relBinning;
              if (pixTemp > 0.55)
                pixTemp = B3DNINT(pixTemp);
              if (AdocSetFloat(ADOC_ZVALUE_NAME, originalZval, "Binning", pixTemp))
                exitError("Adjusting binning in mdoc for output image");
            }
          }

          // Change the Z value to be sequential in the mdoc if either ignoring current
          // Z values or reordering the processing
          if (mIgnoreZvalue || changedSetOrder) {
            sprintf(mInLine, "%d", ifile);
            if (AdocChangeSectionName(ADOC_ZVALUE_NAME, originalZval, mInLine))
              exitError("Changing section name to new Z value");
          }
        }
      }
    }

    if (mNumBinTests * mNumFiltTests[0] > 1) {
        printf("%s %d: %s at bin = %d  rad2 = %.3f  sig2 = %.4f  mean res = %.3f\n",
               numSets ? "Set" : "File", ifile + 1,
               wasGoodEnough ? "Good enough" : "Best", binsToTest[indBestBin],
               varyRadius2[indBestBin][indBestFilt],
               varySigma2[indBestBin][indBestFilt], minMean);
        numTimesBest[indBestBin][indBestFilt]++;
    }

    // Find FRC crossing and mean near half-nyquist
    if (!mTestMode && mGettingFRC) {
      if (useShrMem) {
#ifdef TEST_SHRMEM        
        mSMC.analyzeFRCcrossings(510, ringCorrs, frcDeltaR, halfCross, quartCross,
                                 eighthCross, halfNyq);
#endif
      } else {
        sFA.analyzeFRCcrossings(ringCorrs, frcDeltaR, halfCross, quartCross, eighthCross,
                                halfNyq);
      }

      if (numSummaryLines > 2)
        printf(" FRC crossings 0.5: %.4f  0.25: %.4f  0.125: %.4f  is %.4f at "
               "0.25/pix\n", halfCross, quartCross, eighthCross, halfNyq);
    }
    if (ifile < endingFile - 1 && mNumBinTests * mNumFiltTests[0] > 1)
      printf("\n");

    // Output the transforms
    if (xfExt) {
      if (!numSets || !ifile) {
        xfName = filename;
        tind = xfName.find_last_of('.');
        if (tind >= xfName.length() - 5 && tind > 1 && tind != string::npos)
          xfName.resize(tind + 1);
        xfName += xfExt;
        imodBackupFile(xfName.c_str());
        extraFP = fopen(xfName.c_str(), "w");
        if (!extraFP)
          exitError("Opening output file %s for transforms", xfName.c_str());
      }
      /*if (startFrame > 0)
        for (iz = 1; iz < startFrame; iz++)
          fprintf(extraFP, " 1.00000    0.00000    0.00000   1.00000     0.000   "
          " 0.000\n"); */
      /*for (iz = zStart; zDir * (zEnd - iz) >= 0; iz += zDir) {
        ind = startFrame > 0 ? iz + 1 - startFrame : iz;
        fprintf(extraFP, " 1.00000    0.00000    0.00000   1.00000  %8.3f %8.3f\n",
                bestXshifts[ind], bestYshifts[ind]);
                }*/
      numFetch = zDir * (zEnd - zStart) + 1;
      for (group = 0; group < nzAlign; group++) {
        if (reverse) {
          balancedGroupLimits(numFetch, nzAlign, group, &groupStart, 
                              &groupEnd);
          ind = nzAlign - 1 - group;
        } else {
          balancedGroupLimits(numFetch, nzAlign, nzAlign - 1 - group, &groupStart, 
                              &groupEnd);
          ind = group;
        }
        if (startFrame > 0 && !group)
          groupEnd += startFrame - 1;
        for (iz = groupStart; iz <= groupEnd; iz++)
          fprintf(extraFP, " 1.00000    0.00000    0.00000   1.00000  %8.3f %8.3f\n",
                  bestXshifts[ind], bestYshifts[ind]);
      }
      if (!numSets || ifile == numSets - 1)
        fclose(extraFP);
    }
    
    // Output plottable shifts
    if (plotFP) {
      for (ind = 0; ind < numDone; ind++)
        fprintf(plotFP, "%3d  %.3f  %.3f\n", 10 * ifile + 10, bestXraw[ind],
                bestYraw[ind]);
      if (mDoSpline)
        for (ind = 0; ind < numDone; ind++)
          fprintf(plotFP, "%3d  %.3f  %.3f\n", 10 * ifile + 11, bestXshifts[ind], 
                  bestYshifts[ind]);
    }

    // Output the FRC
    if (frcFP)
      for (ind = 0; ind < (int)floor(0.5 / frcDeltaR); ind++)
        fprintf(frcFP, "%2d  %.4f %10.6f\n", ifile + 1, (ind + 0.5) * frcDeltaR,
                ringCorrs[ind]);
  }

  if (numFilesToDo > 1 && mNumBinTests * mNumFiltTests[0] > 1) {
    printf("\nNumber of times each condition is best  (rad2 in parentheses):\n");
    for (itest = 0; itest < mNumBinTests; itest++) {
      printf("bin = %d  ", binsToTest[itest]);
      for (filt = 0; filt < mNumFiltTests[itest]; filt++)
        printf("  %3d (%.3f)", numTimesBest[itest][filt], varyRadius2[itest][filt]);
      printf("\n");
    }
  }

  // Write the new mdoc file, possibly with re-ordering
  if (adjustMdoc) {
    sstr = mOutNames[0];
    sstr += ".mdoc";
    if (changedSetOrder && AdocOrderWriteByValue(ADOC_ZVALUE_NAME))
      exitError("Memory problem in AdocOrderWriteByValue");
    if (AdocWrite(sstr.c_str()))
      exitError("Writing adjusted mdoc file");
  }

  // Finish up
  if (useShrMem) {
#ifdef TEST_SHRMEM        
    mSMC.cleanup();
    mSMC.Disconnect();
#endif
  } else {
    sFA.cleanup();
  }
  free(summed);
  B3DFREE(rotSum);
  B3DFREE(unwgtSum);
  for (ind = 0; ind < 3; ind++) {
    free(mPartialScanBufs[ind]);
    free(mPartialLinePtrs[ind]);
  }
  if (!mTestMode) {
    for (outNum = 0; outNum < mNumOutFiles; outNum++) {
      if (mrc_head_write(outFPs[outNum], mOutHeads[outNum]))
        exitError("Writing header to output file");
      iiFClose(outFPs[outNum]);
      if (openTsNames[outNum])
        remove(openTsNames[outNum]);
      free(mOutNames[outNum]);
      free(openTsNames[outNum]);
    }
  }
  if (frcFP)
    fclose(frcFP);
  exit(0);
}

/*
 * Return the next filename (for ind) by whatever means they are available
 */
char *AliFrame::getNextFilename(int ind, int numInByOpt, FILE *fileListFP, 
                                char *framePath, char *listName, bool &endReached)
{
  int iz, ierr;
  std::string sstr;
  char *filename, *tempname;

  if (mNamesFromMdoc) {

    // Get the name from the mdoc file and extract it from the path
    if (mIgnoreZvalue)
      iz = ind;
    else
      iz = AdocLookupByNameValue(ADOC_ZVALUE_NAME, ind);
    if (iz < 0)
      exitError("Looking up section with Z value %d in mdoc file", ind);
    if (AdocGetString(ADOC_ZVALUE_NAME, iz, "SubFramePath", &filename))
      exitError("Getting SubFramePath for %s %d in mdoc file", ind, 
                mIgnoreZvalue ? "section" : "Z value");
    extractFileTail(filename, sstr);
    free(filename);
    if (framePath) {
      sstr.insert(0, "/");
      sstr.insert(0, framePath);
    }
    filename = strdup(sstr.c_str());
    if (!filename)
      exitError("Duplicating filename from mdoc file");

    // Try to get a frame dose line from this section, store the dose data
    if (mDoseFileType == 4) {
      ierr = AdocGetString(ADOC_ZVALUE_NAME, iz, FRAME_DOSE_KEY, &tempname);
      if (ierr < 0)
        exitError("Trying to access "FRAME_DOSE_KEY" for %s %d in mdoc file", ind,
                  mIgnoreZvalue ? "section" : "Z value");
      if (!ierr) {
        mFrameDoseLines[ind] = tempname;
        free(tempname);
      }
      mTotalDoseVec.push_back(mDoseFromMdoc[iz]);
      mPriorDoseVec.push_back(mDoseAccumulates > 0 ? mPriorFromMdoc[iz] : 0.);
    }
          
  } else if (fileListFP) {

    // Or get name from file
    if (endReached)
      return NULL;
    do {
      iz = fgetline(fileListFP, mInLine, MAX_LINE);
    } while (iz == 0);
    if (iz == -2)
      return NULL;
    if (iz == -1)
      exitError("Reading line %d of list of input files %s", ind + 1, listName);
    if (iz < 0)
      endReached = true;
    filename = strdup(mInLine);
    if (!filename)
      exitError("Duplicating filename from list of input files");

  } else if (ind < numInByOpt) {

    // Or get arguments
    PipGetString("InputFile", &filename);
  } else {
    PipGetNonOptionArg(ind - numInByOpt, &filename);
  }
  return filename;
}

/*
 * Open an MRC file and read its header
 */
FILE *AliFrame::openAndReadHeader(const char *filename, MrcHeader *head,
                                  const char *descrip, bool testMode)
{
  FILE *inFP = iiFOpen(filename, "rb");
  if (!inFP)
    exitError("Opening %s file %s%s", descrip, filename, 
              testMode ? "; do not specify an output file when not making sums" : "");
  if (mrc_head_read(inFP, head))
    exitError("Reading header of %s file %s", descrip, filename);
  return inFP;
}

/*
 * Do basic checks on size and mode for a file
 */
void AliFrame::checkInputFile(const char *filename, MrcHeader *head, int nx, int ny, 
                              int combine)
{
  if (sliceModeIfReal(head->mode) < 0)
    exitError("File mode for %s is %d; only byte, short, float allowed", filename, 
              head->mode);
  if (nx > 0 && (nx != head->nx || ny != head->ny))
    exitError("File %s has a different size (%d x %d) from previous files (%d x %d)",
              filename, head->nx, head->ny, nx, ny);
  if (combine && head->nz > 1)
    exitError("File %s has more than one slice and cannot be used with -combine",
              filename);
}

/*
 * Get a list of frames saved from SEMCCD in a single exposure tilt series 
 */
bool AliFrame::readAnalyzeSavedFrameList(int breakSetSize)
{
  int ierr, savedNum, lastKeptNum, ind, startOfSet;
  int maxGapInFrameSet = 0, numInList;
  char *savedListName = NULL;
  bool inFrameSet, singleSetsOK;

  PipGetString("SavedFrameListFile", &savedListName);
  PipGetInteger("MaxGapWithinFrameSet", &maxGapInFrameSet);
  if (!savedListName)
    return false;
  if (mNumInFiles != 1)
    exitError("There must be only a single input file with a saved frame list");
  if (breakSetSize)
    exitError("You cannot use the -break option with a saved frame list");
  mInFP = fopen(savedListName, "r");
  if (!mInFP)
    exitError("Opening saved frame list file %s", savedListName);
  while (1) {
    ierr = fgetline(mInFP, mInLine, MAX_LINE);
    if (!ierr)
      continue;
    if (ierr == -2)
      break;
    if (ierr == -1)
      exitError("Reading saved frame list file %s", savedListName);
    mSavedFrames.push_back(atoi(mInLine));
    if (ierr < 0)
      break;
  }

  numInList = mSavedFrames.size();
  if (numInList < 10)
    exitError("There are only %d numbers in the saved frame list file %s", 
              numInList, savedListName);

  // Analyze it now so the number is known if tilt angles come in
  // Keep track if negative numbers seen
  inFrameSet = false;
  singleSetsOK = false;
  lastKeptNum = -1;
  for (ind = 0; ind < numInList; ind++) {
    savedNum = mSavedFrames[ind];
    if (savedNum < 0 || (lastKeptNum < 0 && savedNum >= 0) || 
        (inFrameSet && savedNum > lastKeptNum + maxGapInFrameSet + 1)) {
      if (inFrameSet) {
        mSetStarts.push_back(startOfSet);
        mNumInSets.push_back(ind - startOfSet);
        inFrameSet = false;
      }
      if (savedNum >= 0) {
        inFrameSet = true;
        startOfSet = ind;
      } else if (ind < numInList - 1) {
        singleSetsOK = true;
      }
    }
    if (savedNum >= 0)
      lastKeptNum = savedNum;
  }

  // Allow single frame set at start or end if negative values were seen or if there
  // are fewer than 4 frames per set on average (having -1 in file is less ambiguous)
  if (!singleSetsOK)
    singleSetsOK = numInList < 4 * mNumInSets.size();
  if (inFrameSet && (ind - startOfSet > 1 || singleSetsOK)) {
    mSetStarts.push_back(startOfSet);
    mNumInSets.push_back(ind - startOfSet);
  }
  if (mNumInSets[0] == 1 && !singleSetsOK) {
    mSetStarts.erase(mSetStarts.begin());
    mNumInSets.erase(mNumInSets.begin());
  }
  free(savedListName);
  return true;
}

/*
 * Read tilt angles from a file
 */
void AliFrame::readTiltAngleFile(char *tiltName)
{
  int ierr, ix, iy;
  char *endptr, *newptr;
  if (!tiltName)
    return;
  mInFP = fopen(tiltName, "r");
  if (!mInFP)
    exitError("Opening tilt angle file %s", tiltName);
  while (1) {
    ierr = fgetline(mInFP, mInLine, MAX_LINE);
    if (ierr == -2)
      break;
    if (ierr == -1)
      exitError("Reading tilt angle file %s", tiltName);
    if (ierr > 0) {

      // Convert the angle as a float then look for two integers for frame start/end
      mTiltAngles.push_back((float)strtod(mInLine, &endptr));
      newptr = endptr;
      ix = strtol(newptr, &endptr, 10);
      iy = -1;
      if (endptr == newptr) {
        ix = -1;
      } else {
        newptr = endptr;
        iy = strtol(newptr, &endptr, 10);
        if (endptr == newptr)
          iy = -1;
        else
          mRelFrameStartsFound = true;
      }
      mTiltRelStartFrame.push_back(ix);
      mTiltRelEndFrame.push_back(iy);
    }
    if (ierr < 0)
      break;
  }
  fclose(mInFP);
}

/*
 * Get tilt angles, axis angle, and titles from an mdoc file 
 */
void AliFrame::getAnglesAndTitlesFromMdoc(char *tiltName, float axisAngle)
{
  int ix, iy, iz, ind, ierr, outNum;
  char *sectName;
  std::string sstr;

  // Get tilt angles if not already got them
  if (!tiltName) {
    mTiltAngles.resize(mNumSect);
    for (ind = 0; ind < mNumSect; ind++) {
      if (mIgnoreZvalue) 
        iz = ind;
      else
        iz = AdocLookupByNameValue(ADOC_ZVALUE_NAME, ind);
      if (iz < 0)
        exitError("Looking up section with Z value %d in mdoc file", ind);
      if (AdocGetFloat(ADOC_ZVALUE_NAME, iz, "TiltAngle", &mTiltAngles[ind]))
        exitError("Getting tilt angle for %s %d in mdoc file", ind,
                  mIgnoreZvalue ? "section" : "Z value");

      // Get start and end frames for a saved frame list
      if (mDoingFrameTS) {
        ix = iy = -1;
        ierr = AdocGetTwoIntegers(ADOC_ZVALUE_NAME, iz, "FrameTSStartEndFrames", &ix,
                                  &iy);
        if (ierr < 0)
          exitError("Looking up frame starts and ends in mdoc file");
        if (!ierr)
          mRelFrameStartsFound = true;
        mTiltRelStartFrame.push_back(ix);
        mTiltRelEndFrame.push_back(iy);
      }
    }
  }
  // Get titles
  ind = AdocGetNumberOfSections("T");
  if (ind < 0)
    exitError("Looking up titles in mdoc file");

  for (outNum = 0; outNum < mNumOutFiles; outNum++) {
    mOutHeads[outNum]->nlabl = 0;

    // Transfer real titles, keep track if axis angle was gotten
    ix = 0;
    for (iz = 0; iz < ind; iz++) {
      if (AdocGetSectionName("T", iz, &sectName))
        exitError("Getting title from mdoc file");
      sstr = sectName;
      if ((sstr.find("Tilt axis angle") != string::npos) && (sstr[0] != ' ')) {
        if (axisAngle > -990.)
          continue;
        ix = 1;
        sstr = "    " + sstr;
      }
      strncpy(&mOutHeads[outNum]->labels[iz][0], sstr.c_str(), MRC_LABEL_SIZE);
      fixTitlePadding(&mOutHeads[outNum]->labels[mOutHeads[outNum]->nlabl++][0]);
      free(sectName);
    }

    // If no real titles, look for a T = at top of frame stack mdoc
    if (!ind && !AdocGetString(ADOC_GLOBAL_NAME, 0, "T", &sectName)) {
      strncpy(&mOutHeads[outNum]->labels[iz][0], sectName, MRC_LABEL_SIZE);
      fixTitlePadding(&mOutHeads[outNum]->labels[iz++][0]);
      free(sectName);
    }

    // And if no axis angle gotten, get the rotation angle
    if (!ix && (axisAngle > -990. || !AdocGetFloat("FrameSet", 0, "RotationAngle", 
                                                   &axisAngle))) {
      axisAngle -= 90.;
      addAxisAngleTitle(outNum, axisAngle);
    }
  }
}

/*
 * Looks in the titles of a frame file for gain reference name and possible defect name
 */
void AliFrame::checkTitlesForRefNames(const char *filename)
{
  int ind;
  size_t first = 0, last, ext;
  std::string sstr, fileStr;
  bool gainRef, defect;
  struct stat statBuf;
  
  for (ind = 0; ind < mInHead.nlabl; ind++) {
    sstr = mInHead.labels[ind];

    // Strip the blanks
    first = sstr.find_first_not_of(' ');
    last = sstr.find_last_not_of(' ');
    if (first >= 0 && last > first) {
      sstr = sstr.substr(first, last + 1 - first);
      ext = sstr.size() - 4;

      // See if it qualifies as a gain ref (and none already) or defect file
      gainRef = (sstr.find("ref") != string::npos || sstr.find("Ref") != string::npos) && 
        (sstr.find(".mrc") == ext || sstr.find(".dm4") == ext || 
         sstr.find(".tif") == ext) && !mGainName;
      defect = sstr.find("defect") != string::npos && sstr.find(".txt") == ext && 
        !mDefectName;
      if (defect || gainRef) {
        fileStr = filename;

        // If there is a path to the frame filename, add it to front
        ext = fileStr.find_last_of("/\\");
        if (ext != string::npos)
          sstr = fileStr.substr(0, ext + 1) + sstr;

        // Look for the file and accept it
        if (!stat(sstr.c_str(), &statBuf)) {
          if (gainRef)
            mGainName = strdup(sstr.c_str());
          else
            mDefectName = strdup(sstr.c_str());
        }
      }
    }
  }
}

/*
 * Adds title with tilt axis angle to one of the output headers
 */
void AliFrame::addAxisAngleTitle(int outNum, float axisAngle)
{
  int iz = B3DMIN(mOutHeads[outNum]->nlabl, 8);
  sprintf(mInLine, "    Tilt axis angle = %.1f", axisAngle);
  strncpy(&mOutHeads[outNum]->labels[iz][0], mInLine, MRC_LABEL_SIZE);
  fixTitlePadding(&mOutHeads[outNum]->labels[iz++][0]);
  mOutHeads[outNum]->nlabl = iz;
}

/*
 * If there are too many tilt angles howevere they came in, try to sort out what is
 * missing from a frame tilt series and give warnings
 */
void AliFrame::handleTooManyTiltAngles(const char *progname)
{
  bool trimTilt = false;
  int ix, iy, iz, ind;
  if ((int)mTiltAngles.size() > mNumInFiles && mDoingFrameTS && mRelFrameStartsFound) {
    ix = mSavedFrames[mSetStarts[0]];
    printf("There are more tilt angles than frame sets: analyzing starting and ending"
           " frames\n");

    // For each tilt angle, look at relative frame starts and find overlap with actual
    // relative frame starts
    for (ind = (int)mTiltAngles.size() - 1; ind >= 0; ind--) {
      if (mTiltRelStartFrame[ind] < 0 || mTiltRelEndFrame[ind] < 0)
        continue;
      trimTilt = true;
      for (iy = 0; iy < (int)mSetStarts.size(); iy++) {
        iz = mSavedFrames[mSetStarts[iy]] - ix;

        // If overlap is found, it is good
        if (!(iz > mTiltRelEndFrame[ind] || 
              iz + mNumInSets[iy] < mTiltRelStartFrame[ind])) {
          trimTilt = false;
          break;
        }
      }
        
      // No overlap, remove the tilt
      if (trimTilt) {
        printf("All frames seem to be lost from %.1f deg tilt\n", mTiltAngles[ind]);
        mTiltAngles.erase(mTiltAngles.begin() + ind);
        mTiltRelStartFrame.erase(mTiltRelStartFrame.begin() + ind);
        mTiltRelEndFrame.erase(mTiltRelEndFrame.begin() + ind);
        if (mTotalDoseVec.size() > 0 && mTotalDoseVec.size() > ind) {
          mTotalDoseVec.erase(mTotalDoseVec.begin() + ind);
          mPriorDoseVec.erase(mPriorDoseVec.begin() + ind);
        }
      }
    }
  }
  if ((int)mTiltAngles.size() < mNumInFiles)
    exitError("There are %sfewer tilt angles in the file (%d) than frame files or sets"
              " (%d)", trimTilt ? "now " : "", mTiltAngles.size(), mNumInFiles);
  if ((int)mTiltAngles.size() > mNumInFiles)
    printf("WARNING: %s - There are %sfewer frame files or sets (%d) than tilt angles "
           "in the file (%d)\n", progname,  trimTilt ? "still " : "", mNumInFiles,
           (int)mTiltAngles.size());
}

/*
 * Determine what if anything can be done on the GPU
 */
void AliFrame::assessGpuNeeds(int useShrMem, char *frcName)
{
  float gpuFracMem = 0.85;
  float gpuMemory, needed, needForAli, gpuUsableMem, needForGPUsum, totNeed;
  float needForPreOps;
  int nzAlign, ind;
  bool sumWithAlign;
  bool needPreprocess = mTruncLimit != 0. || mCamSizeX > 0 || mGainSlice != NULL;

  if (mUseGPU < 0)
    return;
  if (useShrMem) {
#ifdef TEST_SHRMEM
    if (!mSMC.gpuAvailable(mUseGPU, &gpuMemory, mDebug % 10))
      mUseGPU = -1;
#endif
  } else {
    if (!sFA.gpuAvailable(mUseGPU, &gpuMemory, mDebug % 10))
      mUseGPU = -1;
  }
  if (mUseGPU < 0)
    return;

  gpuUsableMem = gpuMemory * gpuFracMem;
  if (mGpuMemLimit > 0)
    gpuUsableMem = 1024. * 1024. * 1024. * mGpuMemLimit;
  else if (mGpuMemLimit < 0)
    gpuUsableMem = -gpuMemory * mGpuMemLimit;
  needed = 0.;
  nzAlign = mMaxNumZ;
  if (mUseBlockGroup)
    nzAlign = B3DMAX(1, nzAlign / mGroupSize);
  sFA.gpuMemoryNeeds(mFullPadSize, mSumPadSize, mAlignPadSize, mNumAllVsAll, nzAlign,
                     mRefineAtEnd, mGroupSize, needForGPUsum, needForAli);
  if (mNumOutFiles > 1) 
    needForGPUsum += mSumPadSize;
  
  // First see if summing can be done
  if (!mTestMode) {
    if (needForGPUsum > gpuUsableMem) {
      printf("Insufficient memory on GPU to use it for summing (%.0f MB needed of "
             "%.0f MB total)\n", needForGPUsum / 1.048e6, gpuMemory / 1.048e6);
      mUseGPU = -1;
      return;
    } else {
      mGpuFlags = GPU_FOR_SUMMING + (mNumOutFiles > 1 ? GPU_DO_UNWGT_SUM : 0);
      needed = needForGPUsum;
    }
  }

  // Summing will be done with alignment if only one binning, only one filter or
  // using hybrid shift, and not assessing or doing spline or refining at end
  sumWithAlign = ((mNumBinTests == 1 && (mHybridShifts || mNumFiltTests[0] == 1)) ||
                  mStartAssess >= 0) && !mTestMode && !mDoSpline && !mRefineAtEnd;
  
  // If that is the case, and alignment alone would fit but both would not,
  // then see if deferring the sum will require less memory than the limit and if
  // so, then defer the summing
  // Call this first unconditionally to get mNumHoldFull set properly
  sFA.totalMemoryNeeds(mFullPadSize, 4, mSumPadSize, mAlignPadSize, mNumAllVsAll, nzAlign,
                       mRefineAtEnd, mNumBinTests, mNumFiltTests[0], mHybridShifts, 
                       mGroupSize, mDoSpline, GPU_FOR_ALIGNING, 0, mTestMode,
                       mStartAssess, mSumInOnePass, mNumHoldFull);
  if (sumWithAlign && needForAli < gpuUsableMem && needForAli + needed > 
      gpuUsableMem) {
    totNeed = sFA.totalMemoryNeeds
      (mFullPadSize, 4, mSumPadSize, mAlignPadSize, mNumAllVsAll, nzAlign, mRefineAtEnd,
       mNumBinTests, mNumFiltTests[0], mHybridShifts, mGroupSize, mDoSpline,
       GPU_FOR_ALIGNING, 1, mTestMode, mStartAssess, mSumInOnePass, mNumHoldFull);
    if (totNeed < mMemoryLimit) {
      sumWithAlign = false;
      mDeferSum = 1;
    }
  }

  // If summing is done with aligning add the usage for summing if any
  if (sumWithAlign)
    needForAli += needed;
      
  // Decide if alignment can be done there
  if (needForAli > gpuUsableMem) {
    printf("Insufficient memory on GPU to do alignment (%.0f MB needed"
           " of %.0f MB total)\n", needForAli / 1.048e6, gpuMemory / 1.048e6);
  } else {
    if (sumWithAlign)
      needed = needForAli;
    mGpuFlags |= GPU_FOR_ALIGNING;
  }

  // Now if summing, see if there is room for even/odd
  if ((mGpuFlags & GPU_FOR_SUMMING) && mGettingFRC) {
    needed += mSumPadSize;
    if (needed > gpuUsableMem) {
      printf("Insufficient memory on GPU to get even/odd sums for FRC (%.0f MB needed"
             " of %.0f MB total)\n", needed / 1.048e6, gpuMemory / 1.048e6);
      if (frcName)
        exitError("Insufficient memory on GPU to get FRC output");
      mGettingFRC = false;
      needed -= mSumPadSize;
    } else {
      mGpuFlags |= GPU_DO_EVEN_ODD;
    }
  }

  // Now see about adding bin, noise, and preproc to GPU
  if (mGpuFlags) {

    // If option is entered, just use it to set the flags
    ind = 0;
    if (PipGetInteger("FlagsForGPU", &ind) == 0) {
      if (ind % 10)
        mGpuFlags |= GPU_DO_NOISE_TAPER;
      if ((ind / 10) % 10)
        mGpuFlags |= GPU_DO_BIN_PAD;
      if (mGpuFlags & (GPU_DO_NOISE_TAPER | GPU_DO_BIN_PAD)) {
        if ((ind / 100) % 10) {
          mGpuFlags |= STACK_FULL_ON_GPU;
          if (ind / 10000)
            mGpuFlags |= GPU_STACK_LIMITED + ((ind / 10000) << GPU_STACK_LIM_SHIFT);
        }
        if ((ind / 1000 % 10) && needPreprocess) {
          mGpuFlags |= GPU_DO_PREPROCESS;
          if (mGainSlice)
            mGpuFlags |= GPU_DO_GAIN_NORM;
          if (mCamSizeX > 0)
            mGpuFlags |= GPU_CORRECT_DEFECTS;
        }
      }
    } else {

      // Otherwise, need to analyze which preprocessing/bin/pad operations to perform
      needForPreOps = sFA.findPreprocPadGpuFlags
        (mNx, mNy, mDarkSlice ? sizeof(float) : mMaxDataSize, mMinBinningToTest,
         !mDarkSlice && mGainSlice != NULL, !mDarkSlice && mCamSizeX > 0,
         !mDarkSlice && mTruncLimit != 0., B3DMAX(mNumHoldFull, 1), 
         gpuUsableMem - needed, 0.5 * (1. - gpuFracMem) * gpuUsableMem, mGpuFlags, 
         mGpuFlags);
      needed += needForPreOps;
    }
    ind = (mGpuFlags >> GPU_STACK_LIM_SHIFT) & GPU_STACK_LIM_MASK;
    if (mDebug && (mGpuFlags & (GPU_DO_NOISE_TAPER | GPU_DO_BIN_PAD)))
      printf("%s  %s  %s  %s %s %d on GPU\n", (mGpuFlags & GPU_DO_NOISE_TAPER) ? 
             "noise-pad" : "", (mGpuFlags & GPU_DO_BIN_PAD) ? "bin-pad" : "",
             (mGpuFlags & GPU_DO_PREPROCESS) ? "preprocess" : "", 
             (mGpuFlags & STACK_FULL_ON_GPU) ? "stack" : "", 
             (mGpuFlags & GPU_STACK_LIMITED) ? "limit" : "   ",
             (mGpuFlags & GPU_STACK_LIMITED) ? ind : 0);

    // Can stack smaller size if doing either operation on GPU and either there is
    // no preprocess or preprocessing is on GPU
    if ((mGpuFlags & (GPU_DO_BIN_PAD | GPU_DO_NOISE_TAPER)) && 
        (!needPreprocess || (mGpuFlags & GPU_DO_PREPROCESS)))
      mFullDataSize = mMaxDataSize;
  }
}


/*
 * Read one frame of data in parallel for TIFF file or with regular call
 */
void AliFrame::readOneFrame(unsigned char *readBuf, int izRead, int ifile)
{
  double wallStart = wallTime();
  if (mParallelRead) {
    if (tiffParallelRead(mFileCopies, mNumReadThreads, 0, mNx - 1, 0, mNy - 1,
                         mInDataSize, (char *)readBuf, izRead, MRSA_NOPROC))
      exitError("Reading frame %d from file # %d: %s", izRead, ifile + 1,
                b3dGetError());
    
  } else {
    if (mrc_read_slice(readBuf, mInFP, &mInHead, izRead, 'Z'))
      exitError("Reading frame %d from file # %d", izRead, ifile + 1);
  }
  mWallRead += wallTime() - wallStart;
}

/*
 * Add a read-in image to a sum buffer of the proper type
 */
void AliFrame::addToSumBuffer(void *readBuf, int inMode, void *sumBuf, int &useMode, 
                              int nxy)
{
  unsigned char *bData = (unsigned char *)readBuf;
  short *sData = (short *)readBuf;
  short *sBuf = (short *)sumBuf;
  unsigned short *usBuf = (unsigned short *)sumBuf;
  unsigned short *usData = (unsigned short *)readBuf;
  float *fBuf = (float *)sumBuf;
  float *fData = (float *)readBuf;
  int ix;
  switch (inMode) {
  case MRC_MODE_BYTE:
    useMode = SLICE_MODE_SHORT;
    for (ix = 0; ix < nxy; ix++)
      *sBuf++ += *bData++;
    break;
  case MRC_MODE_SHORT:
    useMode = SLICE_MODE_SHORT;
    for (ix = 0; ix < nxy; ix++)
      *sBuf++ += *sData++;
    break;
  case MRC_MODE_USHORT:
    useMode = SLICE_MODE_USHORT;
    for (ix = 0; ix < nxy; ix++)
      *usBuf++ += *usData++;
    break;
  case MRC_MODE_FLOAT:
    useMode = SLICE_MODE_FLOAT;
    for (ix = 0; ix < nxy; ix++)
      *fBuf++ += *fData++;
    break;
  }
}

// Get the tail from a filename, i.e. strip the path
void AliFrame::extractFileTail(const char *filename, std::string &sstr)
{
  sstr = filename;
  size_t iz = sstr.find_last_of("/\\");
  if (iz == string::npos)
    iz = 0;
  else
    iz++;
  sstr = sstr.substr(iz);
}

/*
 * Checks if the given title has the binning value in it, adjusts by relBinning and places
 * into sstr, and gives message if printChange true.  Returns 1 if binning found
 */
int AliFrame::adjustTitleBinning(const char *title, std::string &sstr, float relBinning,
                                 bool printChange)
{
  int ierr, stackBin;
  char binText[32];
  size_t iz;
  sstr = title;

  // Find binning = string
  iz = sstr.find("binning =");
  if (iz == 0 || iz == string::npos)
    return 0;
  iz += 9;
    
  // Find beginning and end of the binning number
  while (title[iz] == ' ')
    iz++;
  if (title[iz] == 0x00)
    return 0;
  ierr = iz;
  while (title[iz] != ' ' && title[iz] != 0x00)
    iz++;
  stackBin = atoi(&title[ierr]);

  // Make sure it reads, then adjust and make sure that is a sensible value
  if (!stackBin)
    return 0;
  relBinning *= stackBin;
  if (relBinning < 0.46 || (relBinning > 0.54 && relBinning < 0.96) || 
      (relBinning > 0.96 && fabs(B3DNINT(relBinning) - relBinning) > 0.04))
    return 0;

  // Replace with new value
  if (relBinning < 0.54) {
    sprintf(binText, "%.1f", relBinning);
    if (printChange)
      printf("Adjusted binning in header title to %.1f\n", relBinning);
  } else {
    sprintf(binText, "%d", B3DNINT(relBinning));
    if (printChange)
      printf("Adjusted binning in header title to %d\n", B3DNINT(relBinning));
  }
  sstr.replace(ierr, iz - ierr, binText);
  if (sstr[0] != ' ')
    sstr.insert(0, "    ");
  return 1;
}

/*
 * Determine actual minimum and maximum size for sets or groups given the nominal size and
 * the total to be divided into the groups.
 */
void AliFrame::minMaxSetSize(int basicSize, int numFrames, int &minSize, int &maxSize)
{
  int numSets = numFrames / basicSize;
  int remainder = numFrames % basicSize;
  minSize = basicSize + remainder / numSets;
  maxSize = minSize + (remainder % numSets > 0 ? 1 : 0);
}

/*
 * Read in gain reference, dark reference, and defect file
 */
void AliFrame::getGainDarkDefects(int useShrMem)
{
  FILE *extraFP;
  MrcHeader gainHead, darkHead;
  char *strCopy, *strng, *extraName;
  int ind, ierr, yfac, superFac, retval, scaleDefects = 0;
  float imagesBinned = -1.;
  ImodImageFile *iiGain, *iiFrames;
  bool superResOK, gainIsTIFF, framesAreEER;
  b3dUInt16 count;
  float *refTemp;
  int numInX, xStart, xInterval, numInY, yStart, yInterval;
  std::vector<std::vector<float> > biases;

  // Defect file first to override defects in Falcon gain ref
  if (mDefectName) {
#ifdef TEST_SHRMEM
    if (useShrMem)
      defectFileToString(mDefectName, mDefectString);
#endif
    
    ierr = CorDefParseDefects(mDefectName, 0, mDefects, mCamSizeX, mCamSizeY);
    if (ierr)
      exitError("%s defect file %s\n", (ierr == 1) ? "Opening" : 
                  "Reading or parsing lines in", mDefectName);
    free(mDefectName);
    if (!mCamSizeX || !mCamSizeY)
      exitError("Defect list file must have CameraSizeX and CameraSizeY entries");
    PipGetFloat("ImagesAreBinned", &imagesBinned);
    PipGetInteger("DoubleDefectCoords", &scaleDefects);
    CorDefFlipDefectsInY(&mDefects, mCamSizeX, mCamSizeY, 0);
    CorDefFindTouchingPixels(mDefects, mCamSizeX, mCamSizeY, 0);
    if (CorDefSetupToCorrect(mInHead.nx, mInHead.ny, mDefects, mCamSizeX, mCamSizeY, 
                             scaleDefects, imagesBinned, mCorDefBinning, "-imagebinned"))
      exitError("Image size is more than twice the size stored in the camera defect "
                "list");
  }

  // Gain reference
  mNxGain = mNyGain = 0;
  mSuperFacForDefects = 0;
  if (mGainName) {
    extraFP = openAndReadHeader(mGainName, &gainHead, "gain reference");
    free(mGainName);
    if (gainHead.mode != MRC_MODE_FLOAT)
      exitError("Gain reference must be floating point");
    mGainSlice = sliceReadMRC(&gainHead, 0, 'Z');
    if (!mGainSlice)
      exitError("Reading gain reference file");

    // General evaluation of super-resolution relative to the gain
    superFac = mInHead.nx / gainHead.nx;
    yfac = mInHead.ny / gainHead.ny;
    superResOK = !(yfac != superFac || superFac * gainHead.nx != mInHead.nx || 
                   superFac * gainHead.ny != mInHead.ny || 
                   (superFac != 1 && superFac != 2 && superFac != 4));

    // Find out if frames are in an EER file and if gain is in a TIFF file
    iiGain = iiLookupFileFromFP(extraFP);
    gainIsTIFF = iiGain && iiGain->file == IIFILE_TIFF;
    iiFrames = iiLookupFileFromFP(mInHead.fp);
    framesAreEER = iiFrames && iiFrames->file == IIFILE_TIFF && 
      iiFrames->numFramesInEERfile > 0;

    if ((gainIsTIFF || framesAreEER) && !superResOK)
      exitError("Image file size (%d x %d) must be exactly the same, twice, or 4 "
                "times the gain reference size (%d x %d)", mInHead.nx, mInHead.ny,
                gainHead.nx, gainHead.ny);

    // If no defects entered, look for defects in a TIFF gain file
    if (!mCamSizeX && gainIsTIFF &&
        tiffGetArray(iiGain, FEI_TIFF_DEFECT_TAG, &count, &strng) > 0) {
      strCopy = B3DMALLOC(char, count + 1);
      if (!strCopy)
        exitError("Memory error copying defect string from TIFF file");
      strncpy(strCopy, strng, count);
      strCopy[count] = 0x00;
      retval = CorDefParseFeiXml(strCopy, mDefects, mFeiDefectPad);
      if (retval)
        exitError("Parsing defect string from TIFF file (error %d)", retval);
      free(strCopy);

      // Manage them as usual before scaling, set the size to the frame size
      CorDefFlipDefectsInY(&mDefects, gainHead.nx, gainHead.ny, 0);
      CorDefFindTouchingPixels(mDefects, gainHead.nx, gainHead.ny, 0);
      if (superFac > 1) {
        CorDefScaleDefectsForFalcon(&mDefects, superFac);
        if (mDefects.FalconType && mDefects.numAvgSuperRes > 0)
          mSuperFacForDefects = superFac;
      }
      mCamSizeX = mInHead.nx;
      mCamSizeY = mInHead.ny;
    }

    // Finish up with the gain file and apply rotation
    iiFClose(extraFP);
    mNxGain = gainHead.nx;
    mNyGain = gainHead.ny;
    if (mRotationFlip)
      rotateFlipGainReference(mGainSlice->data.f);

    // Now expand the gain reference for super-resolution
    if ((gainIsTIFF || framesAreEER) && superFac > 1) {
      refTemp = B3DMALLOC(float, mInHead.nx * mInHead.ny);
      if (!refTemp)
        exitError("Allocating memory for expanding gain reference");
      CorDefExpandGainReference(mGainSlice->data.f, gainHead.nx, gainHead.ny, superFac,
                                refTemp);
      mNxGain *= superFac;
      mNyGain *= superFac;
      free(mGainSlice->data.f);
      mGainSlice->data.f = refTemp;
      mGainSlice->xsize = mNxGain;
      mGainSlice->ysize = mNyGain;

      if (PipGetString("SuperGainFactorFile", &extraName) == 0) {
        ind = CorDefReadSuperGain(extraName, superFac, biases, numInX, 
                                  xStart, xInterval, numInY, yStart, yInterval);
        if (ind)
          exitError("Reading file with super-resolution gain adjustments (error %d)",
                    ind);
        CorDefRefineSuperResRef(refTemp, mNxGain, mNyGain, superFac, biases, numInX,
                                xStart, xInterval, numInY, yStart, yInterval);
      }
    }

    if (mNxGain < mNx || mNyGain < mNy)
      exitError("Gain reference is smaller than image in %s%s%s", mNxGain < mNx ? "X" :"",
                mNxGain < mNx && mNyGain < mNy ? " and " : "", mNyGain < mNy ? "Y" : "");

    // Recognize K3 and set scaling to 32
    for (ind = 1; ind <= 2; ind++)
      if ((mNxGain == ind * 5760 && mNyGain == ind * 4092) || 
          (mNyGain == ind * 5760 && mNxGain == ind * 4092))
        mDefaultByteScale = 32.;
  } else if (mExtraHasGainRef) {
    mGainSlice = sliceCreate(mNx, mNy, SLICE_MODE_FLOAT);
    if (!mGainSlice)
      exitError("Allocating memory for gain reference");
  }
      
  // Dark reference
  if (PipGetString("DarkReferenceFile", &extraName) == 0) {
    extraFP = openAndReadHeader(extraName, &darkHead, "dark reference");
    free(extraName);
    if (darkHead.mode != MRC_MODE_SHORT && darkHead.mode != MRC_MODE_USHORT)
      exitError("Dark reference must be signed or unsigned short integers");
    if (darkHead.nx != mNx || darkHead.ny != mNy)
      exitError("Dark reference is not the same size as the image");
    mDarkSlice = sliceReadMRC(&darkHead, 0, 'Z');
    if (!mDarkSlice)
      exitError("Reading dark reference file");
    iiFClose(extraFP);
  }
      
}

/*
 * Analyze the extra header from a UCSFtomo file for pixel size, rotation angle, and
 * tilt angles and determine division into groups by tilt angle
 */
int AliFrame::analyzeExtraHeader(FILE *inFP, MrcHeader *head, int startFrame, 
                                 int endFrame, bool axisPixOnly, float **buffer,
                                 int &bufSize, FloatVec &tilts,
                                 int &minSet, int &maxSet,
                                 float &axisAngle, float &pixSize)
{
  int numIntReal = head->nint + head->nreal;
  int num = numIntReal * head->nz;
  int iz, set, izStart, izEnd, size, lastSize = 0, numAtSize, retVal = 0, gotDouble = 0;
  float temp, lastTilt;
  if (!num)
    return 0;
  axisAngle = -999.;
  if (bufSize < num) {
    B3DFREE(*buffer);
    *buffer = B3DMALLOC(float, num);
    if (!*buffer)
      exitError("Allocating buffer for extended header data");
    bufSize = num;
  }
  if (b3dFseek(inFP, MRC_HEADER_SIZE, SEEK_SET) || 
      (int)b3dFread(*buffer, 4, num, inFP) != num)
    exitError("Reading extended header data");

  // Look for a legal pixel size and rotation angle
  if (head->nreal >= 12) {
    pixSize = 0.;
    temp = (*buffer)[head->nint + 11];

    // UCSF tomo puts out angstroms, but it could still be meters.  These are limits for
    // angstroms in flib/image/header.f90
    if (temp > 0.05 && temp < 100000.) {
      pixSize = temp;
    } else {
      temp *= 1.e10;
      if (temp > 0.05 && temp < 100000.)
        pixSize = temp;
    }
    temp = (*buffer)[head->nint + 10];

    // set return to 1 if both are legal
    if (pixSize > 0. && temp >= -360. && temp <= 360.) {
      retVal = 1;
      if (temp < -180.)
        temp += 360.;
      if (temp > 180.)
        temp -= 360.;
      axisAngle = temp;
    }
  }
    
  if (axisPixOnly)
    return retVal;

  // Now analyze tilt angles in slot 1.  Loop on the subset of frames if any
  izStart = 0;
  izEnd = head->nz - 1;
  if (startFrame > 0) {
    izStart = startFrame - 1;
    izEnd = endFrame - 1;
  }
  tilts.clear();
  mSetStarts.clear();

  // Look for each place where tilt angle changes and save the angle and set start
  for (iz = izStart; iz <= izEnd; iz++) {
    temp = (*buffer)[head->nint + iz * numIntReal];
    if (temp < -180. || temp > 180.)
      return retVal;
    if (iz == izStart || fabs(temp - lastTilt) > 0.01) {
      if (iz > izStart) {

        // Keep track of size of sets and number of ones at the last size
        // If there have been at least 5 in a row at a size and there is one at twice 
        // the size, it must be the repeated one at the starting angle
        size = iz - mSetStarts.back();
        if (numAtSize > 5 && size == 2 * lastSize && !gotDouble) {
          mSetStarts.push_back(iz - lastSize);
          tilts.push_back(temp);
          size = lastSize;
          gotDouble = 1;
        }
        if (size == lastSize) {
          numAtSize++;
        } else {
          numAtSize = 1;
          lastSize = size;
        }
      }
      mSetStarts.push_back(iz);
      tilts.push_back(temp);
      lastTilt = temp;
    }
  }
  mSetStarts.push_back(iz);

  // Get the min and max set size
  for (iz = 0; iz < (int)tilts.size(); iz++) {
    set = mSetStarts[iz + 1] - mSetStarts[iz];
    mNumInSets.push_back(set);
    if (!iz) 
      minSet = maxSet = set;
    ACCUM_MIN(minSet, set);
    ACCUM_MAX(maxSet, set);
  }
  return 2 + retVal;
}

/*
 * Apply rotation and flip operation to gain reference: now it's simple
 */
void AliFrame::rotateFlipGainReference(float *ref)
{
  int nxIn = mNxGain, nyIn = mNyGain;
  float *summed = B3DMALLOC(float, mNxGain * mNyGain);
  if (!summed)
    exitError("Allocating array for rotating gain reference");
  if (rotateFlipImage(ref, MRC_MODE_FLOAT, nxIn, nyIn, mRotationFlip, 0, 0, 0, summed,
                      &mNxGain, &mNyGain, 0))
    exitError("Inappropriate rotation/flip value %d entered", mRotationFlip);
  memcpy(ref, summed, mNxGain * mNyGain * sizeof(float));
  free(summed);
}

/*
 * Common operations when opening mdoc for either option
 */
void AliFrame::openMdocFile(const char *filename, int &numSect, int &adocType)
{
  int ind;
  mAdocInd = AdocOpenImageMetadata(filename, 0, &ind, &numSect, &adocType);
  if (mAdocInd == -1)
    exitError("Opening or reading mdoc file %s", filename);
  if (mAdocInd == -2)
    exitError("Metadata file %s does not exist", filename);
  if (mAdocInd == -3)
    exitError("Metadata file %s does not have image stack information", filename);
}

/*
 * Read some dose weighting options and get information from the dose weighting file
 */
void AliFrame::processDoseWeightingOptions(char *frameDoses, int &adocType)
{
  char *doseName = NULL;
  int iz, ierr;
  float doseTemp, priorTemp;
  float critDoseScale200KV = 0.8;
  int voltage = 300;

  PipGetFloat("InitialPriorDose", &mInitialDose);
  PipGetInteger("Voltage", &voltage);
  PipGetInteger("BidirectionalNumViews", &mNumBidir);
  PipGetFloat("OptimalDoseScaling", &mDoseScaling);
  iz = 0;
  PipGetBoolean("NormalizeDoseWeighting", &iz);
  if (iz) {
    mReweightOnes.resize(9000, 1.);
    mReweightFilt = &mReweightOnes[0];
  }

  // Incorporate voltage info into scaling factor
  if (voltage != 300) {
    if (voltage != 200)
      exitError("Voltage must be either 200 or 300");
    mDoseScaling *= critDoseScale200KV;
  }
  if (frameDoses) {
    mFixedFrameDoses = frameDoses;
    free(frameDoses);
  }

  // Get dose file of various kinds
  if (mDoseFileType > 0) {
    PipGetString("DoseWeightingFile", &doseName);
    if (mDoseFileType == 4) {

      // Mdoc file, either existing one or specified one
      if (doseName && mAdocInd >= 0)
        exitError("You cannot enter a dose weighting mdoc file name if you also enter "
                  "the -mdoc option");
      if (mAdocInd >= 0)
        mNumMdocSect = mNumSect;
      else
        openMdocFile(doseName, mNumMdocSect, adocType);

      // Set up vectors to call for doses from it
      mDoseFromMdoc.resize(mNumMdocSect);
      mPriorFromMdoc.resize(mNumMdocSect);
      mIzPiece.resize(mNumMdocSect);
      mFrameDoseLines.resize(mNumInFiles);
      for (iz = 0; iz < mNumMdocSect; iz++)
        mIzPiece[iz] = iz;
      ierr = getMetadataWeightingDoses(mAdocInd, adocType, mNumMdocSect, &mIzPiece[0],
                                       mNumBidir, &mPriorFromMdoc[0], &mDoseFromMdoc[0]);
      if (ierr == 1)
        exitError("Problems occurred accessing data in the mdoc file");
      if (ierr == 2)
        exitError("The dose information in the mdoc file was not usable");
    } else {

      // Other text files, the name must be provided; open the file and read lines
      if (!doseName)
        exitError("You must enter a dose weighting file also");
      mInFP = fopen(doseName, "r");
      if (!mInFP)
        exitError("Opening dose file %s", doseName);
      while (1) {
        ierr = fgetline(mInFP, mInLine, MAX_LINE);
        if (!ierr)
          continue;
        if (ierr == -2)
          break;
        if (ierr == -1)
          exitError("Reading dose file %s", doseName);

        // A single dose value for type 1, a line for type 4, or prior and another value
        if (mDoseFileType == 1) {
          mTotalDoseVec.push_back(atof(mInLine));
          mPriorDoseVec.push_back(0.);
        } else if (mDoseFileType > 4) {
          mFrameDoseLines.push_back(std::string(mInLine));
        } else {
          sscanf(mInLine, "%f %f", &priorTemp, &doseTemp);
          if (mDoseFileType == 3)
            doseTemp -= priorTemp;
          mTotalDoseVec.push_back(doseTemp);
          mPriorDoseVec.push_back(priorTemp);
        }
        if (ierr < 0)
          break;
      }
    }
    B3DFREE(doseName);
  }
}

/*
 * Process dose information some more so all pathways end up in mTotalDoseVec and
 * mPriorDoseVec
 */
void AliFrame::unifyDoseInformation(int breakSetSize, int combineFiles, int adocType)
{
  std::vector<std::string> mdocFileTails;
  std::vector<char *> fullFramePaths;
  std::string sstr;
  int ind, ifile, iz, ierr;
  char *tempname;
  float doseTemp;

  if (mTotalDose > 0. || mDoseFileType > 0 || !mFixedFrameDoses.empty()) {
    mFrameDoses.resize(mMaxFrameDoses);
    mTempVal1.resize(2 * mMaxFrameDoses);
  }
  if (mDoseFileType > 0) {
    ind = mDoseFileType < 4 ? mTotalDoseVec.size() : mFrameDoseLines.size();
    if (mDoseFileType != 4 && ind < mNumInFiles)
      exitError("The dose file has fewer lines (%d) than frame sets to be aligned %d",
                ind, mNumInFiles);
    if ((combineFiles > 0 || breakSetSize > 0) && mDoseFileType == 4)
      exitError("You cannot use an mdoc for a dose file when combining files or "
                "breaking frames into sets");
    if (mDoseFileType == 4 && !mNamesFromMdoc && !mDoingFrameTS) {
      
      // Need to match frame paths in mdoc to actual files being aligned
      fullFramePaths.resize(mNumMdocSect, NULL);
      mTempVal1.resize(mNumMdocSect, 0);
      if (getMetadataByKey(mAdocInd, adocType, mNumMdocSect, "SubFramePath", 0,
                           &mTempVal1[0], NULL, NULL, &fullFramePaths[0], &ind, &iz,
                           mNumMdocSect, &mIzPiece[0]) || !iz)
        exitError("Getting all frame paths from mdoc file");

      // Got some names: reduce them all to filename only
      mdocFileTails.resize(mNumMdocSect);
      for (ind = 0; ind < mNumMdocSect; ind++) {
        if (fullFramePaths[ind]) {
          extractFileTail(fullFramePaths[ind], sstr);
          mdocFileTails[ind] = sstr;
          B3DFREE(fullFramePaths[ind]);
        }
      }

      // Loop on the filenames
      for (ifile = 0; ifile < mNumInFiles; ifile++) {
        extractFileTail(mInFiles[ifile], sstr);
        for (ind = 0; ind < mNumMdocSect; ind++) {
          if (!mdocFileTails[ind].compare(sstr)) {
            mTotalDoseVec.push_back(mDoseFromMdoc[ind]);
            mPriorDoseVec.push_back
              (B3DCHOICE((mAdocInd >= 0 && mDoseAccumulates > 0) || mDoseAccumulates > 1,
                         mPriorFromMdoc[ind], 0.));
            mdocFileTails[ind] = "";
            break;
          }
        }
        if (ind >= mNumMdocSect)
          exitError("No section was found in the mdoc file with a filename matching "
                    "input file %s", mInFiles[ifile]);
        ierr = AdocGetString(ADOC_ZVALUE_NAME, ind, FRAME_DOSE_KEY, &tempname);
        if (ierr < 0)
          exitError("Trying to access "FRAME_DOSE_KEY" from section %d in mdoc file",
                    ind);
        if (!ierr) {
          mFrameDoseLines[ifile] = tempname;
          free(tempname);
        } 
      }
    }
    
    // When there is a saved frame list, just copy the entries over
    if (mDoseFileType == 4 && mDoingFrameTS) {
      mTotalDoseVec = mDoseFromMdoc;
      mPriorDoseVec = mPriorFromMdoc;
    }

    // Now want to check mFrameDoses for reasonableness and get a total dose for type 5
    if (mDoseFileType > 3 && !mDoingFrameTS) {
      for (ifile = 0; ifile < mNumInFiles; ifile++) {
        expandFrameDosesNumbers(mFrameDoseLines[ifile], &mTempVal1[0], mMaxFrameDoses, 
                                ind, doseTemp, NULL);
        if (mDoseFileType > 4) {
          mTotalDoseVec.push_back(doseTemp);
          mPriorDoseVec.push_back(0.);
        }
      }
    }
  }

  // Single frame dose entry: check it, fill arrays, and pretend it is type 5
  if (!mFixedFrameDoses.empty()) {
    expandFrameDosesNumbers(mFixedFrameDoses, &mTempVal1[0], mMaxFrameDoses,
                            ind, doseTemp, NULL);
    for (ifile = 0; ifile < mNumInFiles; ifile++) {
      mFrameDoseLines.push_back(mFixedFrameDoses);
      mTotalDoseVec.push_back(doseTemp);
      mPriorDoseVec.push_back(0.);
    }
    mDoseFileType = 5;
  }

  // Fixed dose, fill the arrays
  if (mTotalDose > 0.) {
    mTotalDoseVec.resize(mNumInFiles, mTotalDose);
    mPriorDoseVec.resize(mNumInFiles, 0.);
  }
    
  // If assuming a tilt series, assign prior doses when none available
  if ((mDoseAccumulates > 0 && (mDoseFileType == 1 || mDoseFileType > 4 || 
                                mTotalDose > 0.))
      || (mDoseAccumulates == 1 && mDoseFileType == 4 && mAdocInd < 0))
    priorDosesFromImageDoses(&mTotalDoseVec[0], (int)mTotalDoseVec.size(), mNumBidir,
                             &mPriorDoseVec[0]);
}

/*
 * Convert a text line for frame doses and number into an array of doses
 */
void AliFrame::expandFrameDosesNumbers(std::string &line, float *tempArr, int maxFrames,
                                      int &totalFrames, float &totalDose,
                                      float *frameDoses)
{
  int ind, add, numSame, numToGet = 0;
  totalFrames = 0;
  totalDose = 0.;
  if (line.empty())
    return;
  if (PipGetLineOfValues(FRAME_DOSE_KEY, line.c_str(), tempArr, PIP_FLOAT, 
                         &numToGet, 2 * maxFrames))
    exitError("Processing an entry for frame doses and numbers: %s", line.c_str());
  if (numToGet % 2)
    exitError("Odd number of numbers in entry for frame doses and numbers: %s", 
              line.c_str());
  for (ind = 0; ind < numToGet; ind += 2) {
    numSame = B3DNINT(tempArr[ind + 1]);
    if (fabs(numSame - tempArr[ind + 1]) > 1.e-3)
      exitError("Non-integer value for count in entry for frame doses and numbers: %s",
                line.c_str());
    if (totalFrames + numSame > maxFrames)
      exitError("Frame numbers add up to more than maximum expected number of frames "
                "in: %s", line.c_str());
    totalDose += numSame * tempArr[ind];
    if (frameDoses)
      for (add = 0; add < numSame; add++)
        frameDoses[totalFrames++] = tempArr[ind];
    else
      totalFrames += numSame;
  }
}

/*
 * Compute limits for looping over frame groups.  Yes, class members would be easier
 */
void AliFrame::frameGroupLimits(int numFetch, int nzAlign, int group, int &groupStart, 
                              int &groupEnd, int zStart, int zDir, int &izLow, 
                              int &izHigh)
{
  int iz;
  balancedGroupLimits(numFetch, nzAlign, nzAlign - 1 - group, &groupStart, &iz);
  groupEnd = numFetch - 1 - groupStart;
  groupStart = numFetch - 1 - iz;
  izLow = zStart + zDir * groupStart;
  izHigh = zStart + zDir * groupEnd;
}

/*
 * Load in first, last, and middle frame of a frame set and compare their means to 
 * see if first or last should be skipped
 */
void AliFrame::analyzeForPartialFrames(int &nz, int &startCombine, int &endCombine, 
                                       int ifile, int dropped[2])
{
  int ind, dropInd = 0;
  int numSample = 40000;
  float sample, means[3];
  int trim = B3DMIN(mNx, mNy) / 20;
  int nxUse = mNx - 2 * trim;
  int nyUse = mNy - 2 * trim;
  int type = typeForSampleMean(mInHead.mode);
  dropped[0] = dropped[1] = -1;

  // Skip if no partial thresholds or too few frames
  if ((mPartialThresh[0] <= 0. && mPartialThresh[1] <= 0.) || nz < 3) 
    return;

  dataSizeForMode(mInHead.mode, &mInDataSize, &ind);

  // Allocation buffers and their line pointers the first time
  if (!mPartialScanBufs[0]) {
    for (ind = 0; ind < 3; ind++) {
      mPartialScanBufs[ind] = B3DMALLOC(unsigned char, mInDataSize * mNx * mNy);
      mPartialLinePtrs[ind] = makeLinePointers(mPartialScanBufs[ind], mNx, mNy,
                                               mInDataSize);
      if (!mPartialScanBufs[ind] || !mPartialLinePtrs[ind])
        exitError("Allocating memory for partial frame analysis");
    }
  }
  
  //Keep track of what frames these are
  sample = B3DMIN(numSample, nxUse * nyUse) / (float)(nxUse * nyUse);
  mZinPartialBufs[0] = startCombine;
  mZinPartialBufs[1] = startCombine + nz / 2;
  mZinPartialBufs[2] = endCombine;

  // Read and get the sample mean
  for (ind = 0; ind < 3; ind++) {
    readOneFrame(mPartialScanBufs[ind], mZinPartialBufs[ind], ifile);
    if (sampleMeanOnly(mPartialLinePtrs[ind], type, mNx, mNy, sample, trim, trim, nxUse, 
                     nyUse, &means[ind]))
      exitError("Error computing mean with sampling for partial frame analysis");
  }

  // Make decisions and adjust the frame set
  if (mPartialThresh[0] > 0. && means[0] < mPartialThresh[0] * means[1]) {
    if (mDebug)
      printf("Skipping frame %d  mean %.3f  ref  %.3f\n", startCombine + 1, means[0],
             means[1]);
    dropped[dropInd++] = startCombine;
    startCombine += 1;
  }
  if (mPartialThresh[1] > 0. && means[2] < mPartialThresh[1] * means[1] &&
      endCombine - startCombine > 1) {
    if (mDebug)
      printf("Skipping frame %d  mean %.3f  ref  %.3f\n", endCombine + 1, means[2],
             means[1]);
    dropped[dropInd++] = endCombine;
    endCombine -= 1;
  }
  nz = endCombine + 1 - startCombine;
}

#ifdef TEST_SHRMEM

/*
 * Function to get a string from the defect file that can be passed and processed
 */
void AliFrame::defectFileToString(char *name, std::string &outStr)
{
  int len;
  bool endReached = false;
  FILE *fp = fopen(name, "r");
  char buf[MAX_LINE];
  outStr = "";
  if (!fp)
    exitError("Failed to open defect file");
  while (true) {
    if (endReached)
      break;
    len = fgetline(fp, buf, MAX_LINE);

    // interpret the len value
    if (!len)
      continue;
    if (len == -2)
      break;
    if (len == -1)
      exitError("Error reading defect file");
    if (len < 0) {
      endReached = true;
      len = -len - 2;
    }
    outStr += buf;
    outStr += "\n";
    if (endReached)
      break;
  }
  fclose(fp);
}
#endif
