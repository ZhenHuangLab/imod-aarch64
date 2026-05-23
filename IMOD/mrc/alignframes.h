/*
 * Header file for alignframes and the AliFrame class
 * $Id$
 */
#ifndef ALIGNFRAMES_H
#define ALIGNFRAMES_H

#define MAX_BINNINGS 6 
#define MAX_LINE 600
#define MAX_READ_THREADS 16

class AliFrame
{
public:
  AliFrame();

  void main( int argc, char *argv[]);
 private:

  // Methods
  char *getNextFilename(int ind, int numInByOpt, FILE *fileListFP, char *framePath, 
                        char *listName, bool &endReached);
  FILE *openAndReadHeader(const char *filename, MrcHeader *head, 
                          const char *descrip, bool testMode = false);
  void checkInputFile(const char *filename, MrcHeader *head, int nx, int ny, 
                      int combine);
  void addToSumBuffer(void *readBuf, int inMode, void *sumBuf, int &useMode, 
                      int nxy);
  void extractFileTail(const char *filename, std::string &sstr);
  int adjustTitleBinning(const char *title, std::string &sstr, float relBinning,
                         bool printChange);
  void minMaxSetSize(int basicSize, int numFrames, int &minSize, int &maxSize);
  void getGainDarkDefects(int useShrMem);
  int analyzeExtraHeader(FILE *inFP, MrcHeader *head, int startFrame, 
                         int endFrame, bool axisPixOnly, float **buffer,
                         int &bufSize, FloatVec &tilts, int &minSet, int &maxSet,
                         float &axisAngle, float &pixSize);
  void readTiltAngleFile(char *tiltName);
  void getAnglesAndTitlesFromMdoc(char *tiltName, float axisAngle);
  void addAxisAngleTitle(int outNum, float axisAngle);
  void checkTitlesForRefNames(const char *filename);
  void handleTooManyTiltAngles(const char *progname);
  void assessGpuNeeds(int useShrMem, char *frcName);
  void readOneFrame(unsigned char *readBuf, int izRead, int ifile);
  void rotateFlipGainReference(float *ref);
  void openMdocFile(const char *filename, int &numSect, int &adocType);
  void processDoseWeightingOptions(char *frameDoses, int &adocType);
  void unifyDoseInformation(int breakSetSize, int combineFiles, int adocType);
  void expandFrameDosesNumbers(std::string &line, float *tempArr, int maxFrames,
                              int &totalFrames, float &totalDose, float *frameDoses);
  void frameGroupLimits(int numFetch, int nzAlign, int group, int &groupStart, 
                      int &groupEnd, int zStart, int zDir, int &izLow, int &izHigh);
  bool readAnalyzeSavedFrameList(int breakSetSize);
  void analyzeForPartialFrames(int &nz, int &startCombine, int &endCombine, int ifile,
                               int dropped[2]);
  void setTruncationLimit(void *array, int useMode, float &truncUse);

  // Member variables
  ImodImageFile *mFileCopies[MAX_READ_THREADS];
  FILE *mInFP;
  MrcHeader mInHead, mMainHead, mUnwgtHead;
  MrcHeader *mOutHeads[2];
  char *mOutNames[2];
  bool mParallelRead, mNamesFromMdoc, mRelFrameStartsFound, mDoingFrameTS, mGettingFRC;
  int mNx, mNy, mNumReadThreads, mInDataSize, mDebug, mNumInFiles, mDoseAccumulates;
  int mNxGain, mNyGain, mExtraHasGainRef, mRotationFlip, mCorDefBinning, mIgnoreZvalue;
  int mNumOutFiles;
  int mUseGPU;
  int mGpuFlags;
  int mTestMode;
  int mDeferSum;
  float mTruncLimit, mGpuMemLimit, mMemoryLimit;
  int mMaxDataSize;
  int mRefineAtEnd;
  int mGroupSize;
  bool mUseBlockGroup;
  int mMaxNumZ;
  int mHybridShifts;
  int mStartAssess;
  int mMinBinningToTest;
  int mNumFiltTests[MAX_BINNINGS];
  int mDoSpline, mFullDataSize, mNumHoldFull, mNumBinTests, mNumAllVsAll;
  float mSumPadSize, mAlignPadSize, mFullPadSize;
  bool mSumInOnePass;

  double mWallRead;
  std::vector<char *> mInFiles;
  unsigned char *mPartialScanBufs[3];
  unsigned char **mPartialLinePtrs[3];
  int mZinPartialBufs[3];
  float mPartialThresh[2], mTotalDose;
  char *mGainName;
  char *mDefectName;
  float mDefaultByteScale;
  float mInitialDose, mDoseScaling;
  int mNumSect, mAdocInd, mNumMdocSect, mDoseFileType, mMaxFrameDoses, mNumBidir;
  int mCamSizeX, mCamSizeY;  // Camera size in X and Y represented in table
  FloatVec mDoseFromMdoc, mPriorFromMdoc, mTotalDoseVec, mPriorDoseVec, mFrameDoses;
  FloatVec mTempVal1;
  FloatVec mReweightOnes;
  float *mReweightFilt;
  IntVec mIzPiece;
  IntVec mSetStarts, mSavedFrames, mNumInSets;
  std::vector<std::string> mFrameDoseLines;
  std::string mFixedFrameDoses;
  Islice *mGainSlice;
  Islice *mDarkSlice;
  CameraDefects mDefects;
  std::string mDefectString;
  int mFeiDefectPad;
  int mSuperFacForDefects;
  char mInLine[MAX_LINE + 1];
  FloatVec mTiltAngles;
  IntVec mTiltRelStartFrame, mTiltRelEndFrame;

#ifdef TEST_SHRMEM
  void defectFileToString(char *name, std::string &outStr);
  ShrMemClient mSMC;
#endif
};


#endif
