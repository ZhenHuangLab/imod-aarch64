/*
 * slicecache.h - Header for SliceCache class
 *
 *  $Id$
 *
 */
#ifndef SLICECACHE_H
#define SLICECACHE_H

#include "mrcslice.h"
#include "cppdefs.h"
#include <vector>

class MyApp;

class SliceCache
{

  public:
  SliceCache(int cacheSize, MyApp *app);
  void initCache(const char *fnStack, int dim, int hyperRes, int numSectors,
                 int& nx, int &ny, int &nz);
   void setDataOffset(float inval) {mDataOffset = inval;};
   float readAngle(int whichSlice);
   void whatIsNeeded(float lowLimit, float highLimit, int &startSliceNum, int&
       endSliceNum); 
   int setOneNeededSlice(int sliceNum);
   std::vector<int>& optimalAccessOrder();
   void getHyperPS(int stripInd, int whichSlice, int wedgeInd,
              float **leftPsSum, float **rightPsSum, int &leftCounter,
              int &rightCounter, double &leftMean, double &rightMean);
   void getWedgePS(int whichSlice, int stripInd, int wedgeInd,
                   float **leftPS, float **rightPS, bool &notDone);
   float   getAngle(int whichSlice);
   void clearAndSetSize(int dim, int hyperRes, int tSize, int rawTile, bool isNoise);
   void setupStripCache(bool isNoise);
   int *getFreqCount();
   MrcHeader *getHeader() {return &mHeader;};
   ~SliceCache();
   getSetMember(double, Wall2D);
   getSetMember(double, Wall1D);
   getMember(int, MaxSliceNum);
   getMember(int, StripCacheTileSize);
   getMember(double, StripCacheLeftTol);
   getMember(double, StripCacheRightTol);
   getMember(double, StripCacheDefocusTol);
   getMember(double, StripCacheAxisAngle);
   int getStripWidthPixels(int slice) {return mStripWidthPixels[slice];};
   int getNumStrips(int slice) {return mStripStartInds[slice].size();};
   float *getStripPS(int whichSlice, bool right, int stripInd, int wedgeInd, 
                     std::vector<int> &leftCounters,
                     std::vector<int> &rightCounters, bool &notDone);
   int indexInCacheAddIfNeeded(int whichSlice);
   void cleanupStripCache(int sliceInd);
   void cleanupWedgeCache(int sliceInd);
   void getWedgeMinMaxes(float center, float range, float &plusMin, float &plusMax,
                         float &minusMin, float &minusMax);

      
  private:
   MyApp *mApp;
   int mMaxCacheSize; // in megs;
   int mMaxSliceNum;
   FILE *mFpStack;
   MrcHeader mHeader;
   int mSliceMode;
   float *mSliceData;
   float mDataOffset;
   int mNumXtiles, mNumYtiles;
   int mCurSlice;
   int mNDim;
   int mHyperRes;
   int mTileSize;
   int mRawTileSize;
   int mOnePsSize;
   int mNumSectors;
   float *mTile, *mRawTile;
   int *mTileToPsInd;
   int *mFreqCount;
   double *mLeftPsTmp, *mRightPsTmp;
   float *mLeftWedgeTmp, *mRightWedgeTmp;
   int *mTileToSectorInd;
   int *mWedgeFreqCount;
   double mWall2D, mWall1D;

   // These cache components are for each slice in the cache
   std::vector< std::vector<int> > mCachedStripDone;   // 1 for full PS, 2 for sectors
   std::vector< std::vector<float *> > mStripLeftPS;
   std::vector< std::vector<float *> > mStripRightPS;
   std::vector< std::vector<float *> > mSectorLeftPS;
   std::vector< std::vector<float *> > mSectorRightPS;
   std::vector< std::vector<int *> > mStripWedgeDone;
   std::vector< std::vector<float *> > mWedgeLeftPS;
   std::vector< std::vector<float *> > mWedgeRightPS;
   std::vector< std::vector<double *> > mLastWedgeLeftPS;
   std::vector< std::vector<double *> > mLastWedgeRightPS;
   std::vector< std::vector<float> > mLastWedgeCenters;
   std::vector< std::vector<float> > mLastWedgeRanges;

   std::vector<float> mStripWedgeRange;
   std::vector<float> mStripWedgeInterval;
   std::vector<int>   mCachedSliceMap; // which slices are in the cache;
   std::vector<float>    mSliceAngles; // tilt angles in RADIAN for slices in cache;

   std::vector<int>   mNeededSlices; // slices the user asks;
   std::vector<int>   mAccessOrder;
   int mOldestInd; //the one that stays in the cache longest will be replaced next;

   // Information about strips: these are all stored for all slices in file
   std::vector<int *> mStripXtiles;
   std::vector<int *> mStripYtiles;
   std::vector<bool *> mTileIsOnLeft;
   std::vector<double *> mTileAxisDist;
   std::vector< std::vector<int> > mStripStartInds;
   std::vector< std::vector<int> > mStripLeftCounters;
   std::vector< std::vector<int> > mStripRightCounters;
   std::vector< std::vector<double> > mStripLeftMeans;
   std::vector< std::vector<double> > mStripRightMeans;
   std::vector<double> mStripWidthPixels;
   int mStripCacheTileSize;
   double mStripCacheLeftTol;
   double mStripCacheRightTol;
   double mStripCacheDefocusTol;
   double mStripCacheAxisAngle;


   int cacheIndex(int whichSlice);

};

#endif
