/*
* slicecache.cpp - build a slice cache to speed up access to power spectra
*
*  Authors: Quanren Xiong and David Mastronarde
*
*  Copyright (C) 2008 by Boulder Laboratory for 3-Dimensional Electron
*  Microscopy of Cells ("BL3DEMC") and the Regents of the University of
*  Colorado.  See dist/COPYRIGHT for full copyright notice.
*
*  $Id$
*/

/**************how to use this class  **********************
  (When it was a class for holding slices)
  1) initCache() to init the class;
  2) whatIsNeeded() to tell the class what slices will be needed;
  3) mAccessOrder=optimalAccessOrder(). It needs to be called before
     getSlice() is called.
  4) use getSlice(mAccessOrder[i]), getAngle(mAccessOrder[i])
     to read slices starting from mAccessOrder[0].

  *************************************************************/
#include <math.h>
#include <vector>
#include "parse_params.h"
#include "slicecache.h"
#include "myapp.h"
#include "b3dutil.h"
#include "cfft.h"

SliceCache::SliceCache(int cacheSize, MyApp *app)
{
  mApp = app;
  mFpStack = NULL;
  mMaxCacheSize = cacheSize;
  mSliceData = NULL;
  mTile = NULL;
  mRawTile = NULL;
  mLeftPsTmp = NULL;
  mRightPsTmp = NULL;
  mLeftWedgeTmp = NULL;
  mRightWedgeTmp = NULL;
  mFreqCount = NULL;
  mTileToPsInd = NULL;
  mTileToSectorInd = NULL;
  mWedgeFreqCount = NULL;
}

SliceCache::~SliceCache()
{
  cleanupStripCache(-1);
  cleanupWedgeCache(-1);
  free(mTile);
  free(mRawTile);
  free(mTileToPsInd);
  free(mFreqCount);
  free(mSliceData);
  free(mLeftPsTmp);
  free(mRightPsTmp);
  free(mLeftWedgeTmp);
  free(mRightWedgeTmp);
  free(mTileToSectorInd);
  free(mWedgeFreqCount);

  // Clear cache components
  mCachedStripDone.clear();
  mStripLeftPS.clear();
  mStripRightPS.clear();
  mSectorLeftPS.clear();
  mSectorRightPS.clear();
  mStripWedgeDone.clear();
  mSliceAngles.clear();
  mCachedSliceMap.clear();
}

/*
 * Frees up slice cache components for one slice or all slices
 */
void SliceCache::cleanupStripCache(int sliceInd)
{
  int slice, strip;
  int slStart = sliceInd < 0 ? 0 : sliceInd;
  int slEnd = sliceInd < 0 ? mCachedStripDone.size() - 1 : sliceInd;
  if (sliceInd >= 0) {
    if (sliceInd >= mCachedStripDone.size())
      exitError("Cache slice index passed to cleanupStripCache, %d, is bigger than "
                "current size, %d", sliceInd, mCachedStripDone.size());
  }

  for (slice = slStart; slice <= slEnd; slice++) {
    mCachedStripDone[slice].clear();
    for (strip = 0; strip < (int)mStripLeftPS[slice].size(); strip++)
      free(mStripLeftPS[slice][strip]);
    mStripLeftPS[slice].clear();
    for (strip = 0; strip < (int)mStripRightPS[slice].size(); strip++)
      free(mStripRightPS[slice][strip]);
    mStripRightPS[slice].clear();
    for (strip = 0; strip < (int)mSectorLeftPS[slice].size(); strip++)
      free(mSectorLeftPS[slice][strip]);
    mSectorLeftPS[slice].clear();
    for (strip = 0; strip < (int)mSectorRightPS[slice].size(); strip++)
      free(mSectorRightPS[slice][strip]);
    mSectorRightPS[slice].clear();
  }
  if (sliceInd < 0) {
    for (slice = 0; slice < mStripXtiles.size(); slice++) {
      B3DFREE(mStripXtiles[slice]);
      B3DFREE(mStripYtiles[slice]);
      B3DFREE(mTileIsOnLeft[slice]);
      B3DFREE(mTileAxisDist[slice]);
    }
  }
}

/*
 * init SliceCache and clear its old contents
 * It needs to be called whenever the tomogram it manages changes.  Namely, ONCE!
 * Hyperres is passed with -1 if using only ctffind
 */
void SliceCache::initCache(const char *fnStack, int dim, int hyperRes, int numSectors, 
                           int &nx, int &ny, int &nz)
{
  int dsize, csize;
  mDataOffset = 0.;
  if (mFpStack)
    iiFClose(mFpStack);
  if ((mFpStack = iiFOpen(fnStack, "rb")) == 0)
    exitError("could not open input file %s", fnStack);

  /* read mHeader */
  if (mrc_head_read(mFpStack, &mHeader))
    exitError("could not read header of input file %s",  fnStack);
  mSliceMode = sliceModeIfReal(mHeader.mode);
  if (mSliceMode < 0)
    exitError("File mode is %d; only byte, short, integer allowed\n",
              mHeader.mode);
  mrc_getdcsize(mHeader.mode, &dsize, &csize);
  nx = mHeader.nx;
  ny = mHeader.ny;
  nz = mHeader.nz;
  B3DFREE(mFreqCount);
  B3DFREE(mSliceData);
  B3DFREE(mLeftPsTmp);
  B3DFREE(mRightPsTmp);
  B3DFREE(mLeftWedgeTmp);
  B3DFREE(mRightWedgeTmp);
  B3DFREE(mWedgeFreqCount);
  mNumSectors = numSectors;

  // If not doing Ctffind, allocate some temp arrays and others that depend on PS size
  // and sector count
  if (hyperRes > 0) {
    mOnePsSize = (dim + 1) * hyperRes + 1;
    // This assumes sector counts stored after the full one
    mFreqCount = B3DMALLOC(int, mOnePsSize * (numSectors + 1));
    mWedgeFreqCount = B3DMALLOC(int, mOnePsSize);
    mLeftPsTmp = B3DMALLOC(double, mOnePsSize);
    mRightPsTmp = B3DMALLOC(double, mOnePsSize);
    mLeftWedgeTmp = B3DMALLOC(float, mOnePsSize);
    mRightWedgeTmp = B3DMALLOC(float, mOnePsSize);
  }
  mSliceData = (float *)malloc(nx * ny * dsize);
  if (!mSliceData || (hyperRes > 0 && (!mFreqCount || !mLeftPsTmp || !mRightPsTmp ||
                                       !mLeftWedgeTmp || !mRightWedgeTmp)))
    exitError("Allocating memory for image slice, frequency counts, or mL/RPsTmp");
  mCurSlice = -1;
}

/*
 * Clear out and re-initialize cache for given conditions, 
 */
void SliceCache::clearAndSetSize(int dim, int hyperRes, int tileSize, int rawTile,
                                 bool isNoise)
{
  int fftXdim, i, ix, iy, rIndex, sectInd, rsIndex, maxStrips, psDim, cumStrips = 0;
  double freqInc, angleInc, pixelAngle;
  std::vector<int> stripsPerSlice;
  fftXdim = (tileSize + 2) / 2;
  if (hyperRes <= 0)
    mOnePsSize = psDim = fftXdim * tileSize;
  else {
    mOnePsSize = (mApp->getDim() + 1) * hyperRes + 1;
    psDim = mOnePsSize * (2 * mNumSectors + 1);
  }
  mNumXtiles = mHeader.nx / (rawTile / 2) - 1;
  mNumYtiles = mHeader.ny / (rawTile / 2) - 1;
  cleanupStripCache(-1);
  cleanupWedgeCache(-1);

  mTileSize = tileSize;
  mRawTileSize = rawTile;
  setupStripCache(isNoise);

  // Get array of number of strips per slice, sort it, and working from biggest down,
  // find the maximum slices that will stay within the maximum cumulative # of strips
  for (ix = 0; ix < mHeader.nz; ix++)
    stripsPerSlice.push_back(mStripStartInds[ix].size());
  rsSortInts(&stripsPerSlice[0], mHeader.nz);
  maxStrips = (int)(mMaxCacheSize * 1024. * 1024. / (psDim * sizeof(float)));
  mMaxSliceNum = 0;
  for (ix = mHeader.nz - 1; ix >= 0; ix--) {
    cumStrips += stripsPerSlice[ix];
    if (cumStrips <= maxStrips)
      mMaxSliceNum++;
    else
      break;
  }
  B3DCLAMP(mMaxSliceNum, 1, mHeader.nz);

  // Take care of tile arrays and indexes from tile to PS
  B3DFREE(mTile);
  B3DFREE(mRawTile);
  B3DFREE(mTileToPsInd);
  B3DFREE(mTileToSectorInd);
  mTile = B3DMALLOC(float, tileSize * (tileSize + 2));
  memset(mTile, 0, 4 * tileSize * (tileSize + 2));
  if (rawTile > mTileSize)
    mRawTile = B3DMALLOC(float, rawTile * (rawTile + 2));
  if (hyperRes > 0) {
    mTileToPsInd = B3DMALLOC(int, fftXdim * (tileSize / 2 + 1));
    mTileToSectorInd = B3DMALLOC(int, fftXdim * tileSize);
  }
  if (!mTile || (rawTile > mTileSize && ! mRawTile) || 
      (hyperRes > 0 && (!mTileToPsInd || !mTileToSectorInd)))
    exitError("Allocating memory for mTile or PS index");

  // Compute the index for each component of FFT and the counts in each bin
  mNDim = dim;
  mHyperRes = hyperRes;
  if (hyperRes > 0) {
    freqInc = 1. / ((mNDim - 1) * hyperRes);
    angleInc = 180. / mNumSectors;
    for (i = 0; i < mOnePsSize * (mNumSectors + 1); i++)
      mFreqCount[i] = 0;
    for (iy = 0; iy <= mTileSize / 2; iy++) {
      for (ix = 0; ix < fftXdim; ix++) {

        // Here make bins with integer truncation
        rIndex = (int)(sqrt((double)(iy * iy + ix * ix)) / (fftXdim - 1) / freqInc);
        rIndex = B3DMIN(rIndex, mOnePsSize - 1);
        mTileToPsInd[iy * fftXdim + ix] = rIndex;
        mFreqCount[rIndex] += 1;

        // Figure out what sector the pixel and the one in the upper part of the FFT
        // are in.  Sectors start at -90 degrees
        // The bottom line is unique, as is frequency 0.5, which we treat as part of the
        // bottom half even though it starts the top half
        pixelAngle = atan2((double)iy, (double)ix) / RADIANS_PER_DEGREE;
        sectInd = (int)((pixelAngle + 90.) / angleInc);
        rsIndex = mOnePsSize * sectInd + rIndex;
        mTileToSectorInd[iy * fftXdim + ix] = rsIndex;
        mFreqCount[mOnePsSize + rsIndex] += 1;

        // And for the rest of the lines, add an index for correspond top line
        if (iy > 0 && iy < mTileSize / 2) {
          mFreqCount[rIndex] += 1;
          sectInd = (int)((-pixelAngle + 90.) / angleInc);
          rsIndex = mOnePsSize * sectInd + rIndex;
          mTileToSectorInd[(mTileSize - iy) * fftXdim + ix] = rsIndex;
          mFreqCount[mOnePsSize + rsIndex] += 1;
        }
      }
    }
  } 

  /* For sanity checking
  for (sectInd = 0; sectInd < mNumSectors; sectInd++) {
    iy = (sectInd + 1) * mOnePsSize;
    ix = 0;
    for (i = iy; i < iy + mOnePsSize - 1; i++)
      ix += mFreqCount[i];
    PRINT2(sectInd, ix);
  } 
  */

  // Clear cache components
  mCachedStripDone.clear();
  mStripLeftPS.clear();
  mStripRightPS.clear();
  mSectorLeftPS.clear();
  mSectorRightPS.clear();
  mStripWedgeDone.clear();
  mSliceAngles.clear();
  mCachedSliceMap.clear();
  mOldestInd = -1;

  if (debugLevel >= 2)
    printf("cacheSize is set to %d \n", mMaxSliceNum);
}

/*
 * Set up the caching of strips by determining which tiles go in each strip for each slice
 * for the current tolerances and angles and tile sizes
 */
void SliceCache::setupStripCache(bool isNoise)
{
  int ind, slice;
  int halfSize = mRawTileSize / 2;
  int leftCounter, rightCounter;
  const int tileMax = mNumXtiles * mNumYtiles;
  double axisXAngle; //tilt axis angle with X coordinate, [0, 2*pi];
  double diagonal = sqrt((double)(mHeader.nx * mHeader.nx + mHeader.ny * mHeader.ny));
  double centerX, centerY, rad, alpha, currAngle, defDiff, dist;
  int xtile, ytile, scanCounter, iterNum, tileInd;
  bool isOnLeft; // is the tile center on the left of the tilt axis;
  float *angles = mApp->getTiltAngles();
  double pixelSize = mApp->getPixelSize();

  // First get all the vectors resized (Assumes previous contents are cleaned out)
  mStripXtiles.resize(mHeader.nz);
  mStripYtiles.resize(mHeader.nz);
  mTileIsOnLeft.resize(mHeader.nz);
  mTileAxisDist.resize(mHeader.nz);

  for (ind = 0; ind < mStripStartInds.size(); ind++)
    mStripStartInds[ind].clear();
  for (ind = 0; ind < mStripLeftCounters.size(); ind++)
    mStripLeftCounters[ind].clear();
  for (ind = 0; ind < mStripRightCounters.size(); ind++)
    mStripRightCounters[ind].clear();
  mStripStartInds.resize(mHeader.nz);
  mStripLeftMeans.resize(mHeader.nz);
  mStripRightMeans.resize(mHeader.nz);
  mStripLeftCounters.resize(mHeader.nz);
  mStripRightCounters.resize(mHeader.nz);
  mStripWidthPixels.resize(mHeader.nz);

  //tilt axis angle with X coordinate, [0, 2*pi];
  axisXAngle = 90 + mApp->getAxisAngle();
  axisXAngle = axisXAngle * MY_PI / 180.0;
  if (axisXAngle < 0)
    axisXAngle = 2.0 * MY_PI + axisXAngle;

  // Create the arrays for list of tiles in strips
  for (ind = 0; ind < mHeader.nz; ind++) {
    mStripXtiles[ind] = B3DMALLOC(int, tileMax);
    mStripYtiles[ind] = B3DMALLOC(int, tileMax);
    mTileIsOnLeft[ind] = B3DMALLOC(bool, tileMax);
    mTileAxisDist[ind] = B3DMALLOC(double, tileMax);
    if (!mStripXtiles[ind] || !mStripYtiles[ind] || !mTileIsOnLeft[ind] || 
        !mTileAxisDist[ind])
      exitError("Allocating arrays for tile lists in strips");
  }

  // Now figure out the strips for each slice
  for (slice = 0; slice < mHeader.nz; slice++) {
    currAngle = isNoise ? 0. : angles[slice] * RADIANS_PER_DEGREE;

    iterNum = 0;
    if (fabs(currAngle) > MIN_ANGLE)
      mStripWidthPixels[slice] = fabs(mApp->getDefocusTol() / tan(currAngle)) / pixelSize;
    else
      mStripWidthPixels[slice] = diagonal;
    if (mStripWidthPixels[slice] > diagonal)
      mStripWidthPixels[slice] = diagonal;

    // This approach with looping on the counter is taken out of the doComputePS routine
    scanCounter = 0;
    tileInd = 0;
    while (scanCounter < tileMax) {
      leftCounter = 0;
      rightCounter = 0;
      mStripStartInds[slice].push_back(tileInd);
      for (xtile = 0; xtile < mNumXtiles; xtile++) {
        for (ytile = 0; ytile < mNumYtiles; ytile++) {
          centerX = xtile * halfSize + halfSize - mHeader.nx / 2; // tile center
          centerY = ytile * halfSize + halfSize - mHeader.ny / 2;
          rad = sqrt((double)(centerX * centerX + centerY * centerY));
          alpha = atan2(centerY, centerX);
          if (alpha < 0)
            alpha = 2.0 * MY_PI + alpha; //convert to [0,2*pi];

          // pixels to tilt axis
          dist = rad * fabs(sin(alpha - axisXAngle));
          
          // outside of the strip, continue;
          if (dist < iterNum * mStripWidthPixels[slice] / 2.0 ||
              dist > (iterNum + 1) * mStripWidthPixels[slice] / 2.0)
            continue;
          scanCounter++;

          if (axisXAngle <= MY_PI)
            isOnLeft = alpha > axisXAngle && alpha < axisXAngle + MY_PI;
          else
            isOnLeft = alpha > axisXAngle || alpha < axisXAngle - MY_PI;
          
          // defocus difference (nm)
          defDiff = dist * pixelSize * fabs(tan(currAngle));
          
          if ((isOnLeft && defDiff > mApp->getLeftTol()) ||
              (!isOnLeft && defDiff > mApp->getRightTol()))
            continue;

          if (isOnLeft)
            leftCounter++;
          else
            rightCounter++;
          mStripXtiles[slice][tileInd] = xtile;
          mStripYtiles[slice][tileInd] = ytile;
          mTileAxisDist[slice][tileInd] = dist;
          mTileIsOnLeft[slice][tileInd++] = isOnLeft;
        }
      }  // loop on tiles
      
      // Strip is done; save the counters
      mStripLeftCounters[slice].push_back(leftCounter);
      mStripRightCounters[slice].push_back(rightCounter);
      iterNum++;
      if (debugLevel >= 3)
        printf("scanCounter=%d leftCounter=%d rightCounter=%d slice %d\n",
           scanCounter, leftCounter, rightCounter, slice);
    }  // end scan loop

    // resize arrays for means 
    mStripLeftMeans[slice].resize(mStripStartInds[slice].size());
    mStripRightMeans[slice].resize(mStripStartInds[slice].size());
  }

  // Record conditions of setup
  mStripCacheLeftTol = mApp->getLeftTol();
  mStripCacheRightTol = mApp->getRightTol();
  mStripCacheDefocusTol = mApp->getDefocusTol();
  mStripCacheAxisAngle = mApp->getAxisAngle();
  mApp->setAllViewsAstigmatism(0.);
}

/*
 * Clean up the wedge cache and free its components for one slice or all slices
 */
void SliceCache::cleanupWedgeCache(int sliceInd)
{
  int i, ix, start = 0, end = mStripWedgeDone.size() - 1;
  if (sliceInd >= 0) {
    start = end = sliceInd;
    if (sliceInd >= mStripWedgeDone.size())
      exitError("Cache slice index passed to cleanupWedgeCache, %d, is bigger than "
                "current size, %d", sliceInd, mStripWedgeDone.size());
  }
  for (i = start; i <= end; i++) {
    for (ix = 0; ix < mWedgeLeftPS[i].size(); ix++)
      free(mWedgeLeftPS[i][ix]);
    mWedgeLeftPS[i].clear();
    for (ix = 0; ix < mWedgeRightPS[i].size(); ix++)
      free(mWedgeRightPS[i][ix]);
    mWedgeRightPS[i].clear();
    for (ix = 0; ix < mStripWedgeDone[i].size(); ix++)
      free(mStripWedgeDone[i][ix]);
    mStripWedgeDone[i].clear();
    for (ix = 0; ix < mLastWedgeLeftPS[i].size(); ix++)
      free(mLastWedgeLeftPS[i][ix]);
    mLastWedgeLeftPS[i].clear();
    for (ix = 0; ix < mLastWedgeRightPS[i].size(); ix++)
      free(mLastWedgeRightPS[i][ix]);
    mLastWedgeRightPS[i].clear();
  }
}


/*
 * This routine used to read in angles from file, now it gets them from array
 */
float  SliceCache::readAngle(int whichSlice)
{
  if (whichSlice < 0 || whichSlice >= mHeader.nz)
    exitError("Slice index is out of range");

  float currAngle;
  float *angles = mApp->getTiltAngles();
  if (angles) {
    currAngle = angles[whichSlice];
    return currAngle * MY_PI / 180.0;
  } else {
    if (debugLevel > 1)
      printf("No angle is specified, set to 0.0\n");
    return 0.0;
  }
}

/*
 * Stores needed slices (file Z indexes) for the given angle range in mNeededSlices and 
 * returns the starting and ending Z values
 * It needs to be called whenever the angle range changes.
 */
void SliceCache::whatIsNeeded(float lowLimit, float highLimit, int &start,
                              int &end)
{
  mNeededSlices.clear();

  int k, numSkip;
  int *skipList = mApp->getViewSkipList(numSkip);
  bool excludeSkips = mApp->getExcludingSkipList();
  float eps = mApp->getNoAnglesEntered() ? 0.002f : 0.02f;
  float currAngle;
  float *angles = mApp->getTiltAngles();
  for (k = 0; k < mHeader.nz; k++) {
    if (angles) {
      currAngle = angles[k];
    } else {
      currAngle = 0.0;
    }
    if (currAngle < lowLimit - eps || currAngle > highLimit + eps)
      continue;
    if (angles && excludeSkips && numberInList(k + 1, skipList, numSkip, 0))
      continue;
    mNeededSlices.push_back(k);
  }

  int totalSlice = mNeededSlices.size();
  if (totalSlice > 0) {
    start = mNeededSlices[0];
    end = mNeededSlices[totalSlice - 1];
  } else {
    end = -1;
    start = -1;
  }
}

/*
 * Set the needed slice array to one specific slice
 */
int SliceCache::setOneNeededSlice(int sliceNum)
{
  if (sliceNum < 0 || sliceNum >= mHeader.nz)
    return 1;
  mNeededSlices.clear();
  mNeededSlices.push_back(sliceNum);
  return 0;
}


//return the index of the slice in cache;
//return -1, if the slice is not in cache;
int SliceCache::cacheIndex(int whichSlice)
{
  int currSliceNum = mStripLeftPS.size();
  for (int i = 0; i < currSliceNum; i++) {
    if (mCachedSliceMap[i] == whichSlice)
      return i;
  }
  return -1;
}

/*
 * Return array of needed slices with ones already in cache first 
 */
std::vector<int> &SliceCache::optimalAccessOrder()
{
  mAccessOrder.clear();

  int neededSliceNum = mNeededSlices.size();
  for (int i = 0; i < neededSliceNum; i++) {
    if (cacheIndex(mNeededSlices[i]) > -1)
      mAccessOrder.insert(mAccessOrder.begin(), mNeededSlices[i]);
    else
      mAccessOrder.push_back(mNeededSlices[i]);
  }
  return mAccessOrder;
}

/*
 * Returns index of slice in cache, adding it to the cache if it is not there yet
 */
int SliceCache::indexInCacheAddIfNeeded(int whichSlice)
{
  int sliceInd = cacheIndex(whichSlice);
  int currCacheSize = mStripLeftPS.size();
  int ii, numStrips = mStripStartInds[whichSlice].size();
  float *stripLPS, *stripRPS, *sectorLPS, *sectorRPS;

  if (sliceInd > -1) { // already in cache
    /*if (debugLevel >= 1)
      printf("Slice %d is in cache and is included\n", whichSlice);*/

  } else {
    if (currCacheSize < mMaxSliceNum) { //not in cache and cache not full

      if (debugLevel >= 2)
        printf("Slice %d is NOT in cache and is included \n", whichSlice);
      if (mOldestInd == -1)
        mOldestInd = currCacheSize;
      sliceInd = currCacheSize;
      mCachedSliceMap.push_back(whichSlice);
      mSliceAngles.push_back(readAngle(whichSlice));

      // push empty vectors for the strips and wedges
      std::vector<int> stripDone;
      std::vector<float> lastCenter, lastRange;
      std::vector<int *> wedgeDone;
      std::vector<float *> wedgeLPS, wedgeRPS, sectLPS, sectRPS, stripLPS, stripRPS;
      std::vector<double *> lastLPS, lastRPS;
      mCachedStripDone.push_back(stripDone);
      mStripLeftPS.push_back(stripLPS);
      mStripRightPS.push_back(stripRPS);
      mSectorLeftPS.push_back(sectLPS);
      mSectorRightPS.push_back(sectRPS);
      mStripWedgeDone.push_back(wedgeDone);
      mWedgeLeftPS.push_back(wedgeLPS);
      mWedgeRightPS.push_back(wedgeRPS);
      mLastWedgeLeftPS.push_back(lastLPS);
      mLastWedgeRightPS.push_back(lastRPS);
      mLastWedgeCenters.push_back(lastCenter);
      mLastWedgeRanges.push_back(lastRange);
      mStripWedgeRange.push_back(0.);
      mStripWedgeInterval.push_back(0.);

    } else { // not in cache and cache is full
      if (debugLevel >= 2)
        printf("Slice %d is NOT in cache and replaces slice %d \n", whichSlice,
               mCachedSliceMap[mOldestInd]);

      mCachedSliceMap[mOldestInd] = whichSlice;
      mSliceAngles[mOldestInd] = readAngle(whichSlice);
      sliceInd = mOldestInd;
      mOldestInd = (mOldestInd + 1) % mMaxSliceNum;

      // Clean up all the PS from the previous slice's strips
      cleanupWedgeCache(sliceInd);
      cleanupStripCache(sliceInd);
    }

    // Create new arrays sized for this slice
    mCachedStripDone[sliceInd].resize(numStrips);
    for (ii = 0; ii < numStrips; ii++)
      mCachedStripDone[sliceInd][ii] = 0;
    for (ii = 0; ii < numStrips; ii++) {
      stripLPS = B3DMALLOC(float, mOnePsSize);
      stripRPS = B3DMALLOC(float, mOnePsSize);
      mStripLeftPS[sliceInd].push_back(stripLPS);
      mStripRightPS[sliceInd].push_back(stripRPS);
      if (mHyperRes > 0) {
        sectorLPS = B3DMALLOC(float, mOnePsSize * mNumSectors);
        sectorRPS = B3DMALLOC(float, mOnePsSize * mNumSectors);
        mSectorLeftPS[sliceInd].push_back(sectorLPS);
        mSectorRightPS[sliceInd].push_back(sectorRPS);
      }
      if (!stripLPS || !stripRPS || (mHyperRes > 0 && (!sectorLPS || !sectorRPS)))
        exitError("Allocating memory for power spectra");
    }
  }
  fflush(stdout);
  return sliceInd;
}

/*
 * Get pointers to left and right wedge spectra in wedge cache of regularly spaced wedges
 * for the given slice, strip, and wedge indexes, and set notDone if not computed yet
 */
void SliceCache::getWedgePS(int whichSlice, int stripInd, int wedgeInd, 
                            float **leftPS, float **rightPS, bool &notDone)
{
  int numWedges = mApp->getNumWedges();
  int sliceInd = cacheIndex(whichSlice);
  int ind, strp, numStrips = mStripStartInds[whichSlice].size();

  if (fabs(mStripWedgeRange[sliceInd] - mApp->getWedgeAngleRange()) > 1.e-4 ||
      fabs(mStripWedgeInterval[sliceInd] - mApp->getWedgeInterval()) > 1.e-4)
    cleanupWedgeCache(sliceInd);

  // Allocate the wedgeDone and PS arrays
  if (!mStripWedgeDone[sliceInd].size()) {
    mStripWedgeDone[sliceInd].resize(numStrips);
    mWedgeLeftPS[sliceInd].resize(numStrips);
    mWedgeRightPS[sliceInd].resize(numStrips);
    mLastWedgeLeftPS[sliceInd].resize(numStrips);
    mLastWedgeRightPS[sliceInd].resize(numStrips);
    mLastWedgeCenters[sliceInd].resize(numStrips);
    mLastWedgeRanges[sliceInd].resize(numStrips);
    for (strp = 0; strp < numStrips; strp++) {
      mLastWedgeRanges[sliceInd][strp] = 0.;
      if (mStripLeftCounters[whichSlice][strp] + mStripRightCounters[whichSlice][strp]) {
        mStripWedgeDone[sliceInd][strp] = B3DMALLOC(int, numWedges);
        mWedgeLeftPS[sliceInd][strp] = B3DMALLOC(float, numWedges * mOnePsSize);
        mWedgeRightPS[sliceInd][strp] = B3DMALLOC(float, numWedges * mOnePsSize);
        mLastWedgeLeftPS[sliceInd][strp] = B3DMALLOC(double, mOnePsSize);
        mLastWedgeRightPS[sliceInd][strp] = B3DMALLOC(double, mOnePsSize);
        if (!mStripWedgeDone[sliceInd][strp] || !mWedgeLeftPS[sliceInd][strp] ||
            !mWedgeRightPS[sliceInd][strp] || !mLastWedgeLeftPS[sliceInd][strp] ||
            !mLastWedgeRightPS[sliceInd][strp])
          exitError("Allocating strip PS arrays");
        for (ind = 0; ind < numWedges; ind++)
          mStripWedgeDone[sliceInd][strp][ind] = 0;
      } else {
        mStripWedgeDone[sliceInd][strp] = NULL;
        mWedgeLeftPS[sliceInd][strp] = NULL;
        mWedgeRightPS[sliceInd][strp] = NULL;
        mLastWedgeLeftPS[sliceInd][strp] = NULL;
        mLastWedgeRightPS[sliceInd][strp] = NULL;
      }
    }
    mStripWedgeInterval[sliceInd] = mApp->getWedgeInterval();
    mStripWedgeRange[sliceInd] = mApp->getWedgeAngleRange();   
  }
  if (wedgeInd < 0 || wedgeInd > numWedges || stripInd < 0 || stripInd >= numStrips)
    exitError("Wedge or strip index out of bounds in getWedgePS (%d %d %d %d)", 
              wedgeInd, numWedges, stripInd, numStrips);
  notDone = mStripWedgeDone[sliceInd][stripInd] &&
    !mStripWedgeDone[sliceInd][stripInd][wedgeInd];
  *leftPS = mWedgeLeftPS[sliceInd][stripInd] + wedgeInd * mOnePsSize;
  *rightPS = mWedgeRightPS[sliceInd][stripInd] + wedgeInd * mOnePsSize;
}

/*
 * Get a power spectrum, either 1D spectrum with hyper-resolution or a 2D FFT amplitude
 * array, and the associated counters and means, for the given strip of whichSlice
 * It does a regular wedge (wedgeInd >= 0) or an arbitrary one if mNextSpecWedgeRange > 0
 */
void SliceCache::getHyperPS(int whichSlice, int stripInd, int wedgeInd,
                            float **leftPsSum, float **rightPsSum, int &leftCounter,
                            int &rightCounter, double &leftMean, double &rightMean)
{
  if (whichSlice < 0 || whichSlice >= mHeader.nz)
    exitError("slice Num is out of range");
  int sliceInd = indexInCacheAddIfNeeded(whichSlice);
  int tileInd, tileX, tileY, numTot, numAddOrSub, num, start, leftRight;
  float wedgeCenter = mApp->getWedgeCenterAngle();
  float wedgeRange = mApp->getNextSpecWedgeRange();
  float *retPS;
  float *useTile = mRawTile ? mRawTile : mTile;
  float *sectorLeftPS, *sectorRightPS, *sectorPS, *ampSpecTmp;
  double *wedgeLeftPS, *wedgeRightPS, *wedgePS, *psTmp;
  int halfSize = mRawTileSize / 2;
  int tileXdim = mTileSize + 2, rawXdim = mRawTileSize + 2;
  int fftXdim = tileXdim / 2;
  int idir = 0; //FFT direction;
  int ii, ix, iy, ind, ind2, ix0, iy0;
  double mean;
  float plusMin, plusMax, minusMin, minusMax, sectAngle;
  float lastRange, lastPlusMin, lastPlusMax, lastMinusMin, lastMinusMax;
  bool tileIsOnLeft, needTiles, adjustLastWedge, inCurWedge, inLastWedge;
  bool notDone = true;
  bool doingWedge = wedgeRange > 0 && mHyperRes > 0;
  double wallStart = wallTime();


  // Get the return arrays from different places, and the double arrays for summing wedge
  if (doingWedge) {
    if (wedgeInd >= 0) {

      // Wedge cache arrays to return, sum into "last" arrays
      getWedgePS(whichSlice, stripInd, wedgeInd, leftPsSum, rightPsSum, notDone);
      wedgeLeftPS = mLastWedgeLeftPS[sliceInd][stripInd];
      wedgeRightPS = mLastWedgeRightPS[sliceInd][stripInd];
    } else {
      
      // or temporary arrays for return and summing if not from cache
      *leftPsSum = mLeftWedgeTmp;
      *rightPsSum = mRightWedgeTmp;
      wedgeLeftPS = mLeftPsTmp;
      wedgeRightPS = mRightPsTmp;
    }
    sectorLeftPS = mSectorLeftPS[sliceInd][stripInd];
    sectorRightPS = mSectorRightPS[sliceInd][stripInd];
  } else {

    // strip cache arrays for full sum
    *leftPsSum = mStripLeftPS[sliceInd][stripInd];
    *rightPsSum = mStripRightPS[sliceInd][stripInd];
    notDone = mCachedStripDone[sliceInd][stripInd] <= 0;
  }
  leftCounter = mStripLeftCounters[whichSlice][stripInd];
  rightCounter = mStripRightCounters[whichSlice][stripInd];

  // Initialize mean for summing, or get it from cache
  leftMean = 0.;
  rightMean = 0.;
  if (mHyperRes > 0 && mCachedStripDone[sliceInd][stripInd]) {
    leftMean = mStripLeftMeans[whichSlice][stripInd];
    rightMean = mStripRightMeans[whichSlice][stripInd];
  }

  // return if done
  if (!notDone)
    return;

  needTiles = mCachedStripDone[sliceInd][stripInd] <= 0 || 
    (doingWedge && mCachedStripDone[sliceInd][stripInd] < 2);

  // Have to compute the power spectrum
  // Read in slice if needed
  if (needTiles && mCurSlice != whichSlice) {
    // Reallocate as floating point when necessary
    if (mDataOffset && mSliceMode != MRC_MODE_FLOAT) {
      free(mSliceData);
      mSliceData = (float *)malloc(mHeader.nx * mHeader.ny * 4);
      if (!mSliceData)
        exitError("Allocating memory for floating point image slice");
      mSliceMode = MRC_MODE_FLOAT;
    }

    if (mrc_read_slice(mSliceData, mFpStack, &mHeader, whichSlice, 'Z'))
      exitError("Reading slice %d", whichSlice);

    // Apply any offset needed. Done as float to avoid over/underflow.
    if (mDataOffset) {
      unsigned char *bdata = (unsigned char *)mSliceData;
      b3dUInt16 *usdata = (b3dUInt16 *)mSliceData;
      b3dInt16 *sdata = (b3dInt16 *)mSliceData;

      switch (mHeader.mode) {
      case MRC_MODE_BYTE:
        for (ii = mHeader.nx * mHeader.ny - 1; ii >= 0; ii--)
          mSliceData[ii] = bdata[ii] + mDataOffset;
        break;
      case MRC_MODE_USHORT:
        for (ii = mHeader.nx * mHeader.ny - 1; ii >= 0; ii--)
          mSliceData[ii] = usdata[ii] + mDataOffset;
        break;
      case MRC_MODE_SHORT:
        for (ii = mHeader.nx * mHeader.ny - 1; ii >= 0; ii--)
          mSliceData[ii] = sdata[ii] + mDataOffset;
        break;
      case MRC_MODE_FLOAT:
        for (ii = 0; ii < mHeader.nx * mHeader.ny; ii++)
          mSliceData[ii] += mDataOffset;
        break;
      }
    }
    mCurSlice = whichSlice;
  }

  if (needTiles) {

    // If need to process tiles, zero out the place where the PS/ampSpec will be summed
    if (mHyperRes > 0) {
      if (!mCachedStripDone[sliceInd][stripInd])
        for (ii = 0; ii < mOnePsSize; ii++) 
          mLeftPsTmp[ii] = mRightPsTmp[ii] = 0.;
      if (wedgeRange && mCachedStripDone[sliceInd][stripInd] < 2)
        for (ii = 0; ii < mOnePsSize * mNumSectors; ii++) 
          sectorLeftPS[ii] = sectorRightPS[ii] = 0.;
    } else {
      for (ii = 0; ii < mOnePsSize; ii++) {
        (*leftPsSum)[ii] = 0.;
        (*rightPsSum)[ii] = 0.;
      }
    }
  
    // Loop on the tiles in the strip
    num = mStripLeftCounters[whichSlice][stripInd] + 
      mStripRightCounters[whichSlice][stripInd];
    start = mStripStartInds[whichSlice][stripInd];
    for (tileInd = start; tileInd < start + num; tileInd++) {
      tileX = mStripXtiles[whichSlice][tileInd];
      tileY = mStripYtiles[whichSlice][tileInd];
      tileIsOnLeft = mTileIsOnLeft[whichSlice][tileInd];
      
      // get the mTile
      ix0 = tileX * halfSize;
      iy0 = tileY * halfSize;
      sliceTaperInPad(mSliceData, mSliceMode, mHeader.nx, ix0, ix0 + mRawTileSize - 1,
                      iy0, iy0 + mRawTileSize - 1, useTile, rawXdim, mRawTileSize, 
                      mRawTileSize, 9, 9);

      if (mHyperRes > 0 && !mCachedStripDone[sliceInd][stripInd]) {
        
        // Get its mean and add it in
        mean = 0.0;
        for (iy = 0; iy < mRawTileSize; iy++)
          for (ix = 0; ix < mRawTileSize; ix++)
            mean += useTile[iy * rawXdim + ix];
        mean /= (mRawTileSize * mRawTileSize);
        if (tileIsOnLeft)
          leftMean += mean / leftCounter;
        else
          rightMean += mean / rightCounter;
      } 

      // FFT 
      todfft(useTile, &mRawTileSize, &mRawTileSize, &idir);
      mWall2D += wallTime() - wallStart; wallStart = wallTime();

      // Crop raw tile without shifting phases
      if (mRawTile) {
        fourierReduceImage(mRawTile, mRawTileSize, mRawTileSize, mTile, mTileSize,
                           mTileSize, 0., 0., NULL);
      }

      if (mHyperRes > 0) {
        
        psTmp = tileIsOnLeft ? mLeftPsTmp : mRightPsTmp;
        if (!mCachedStripDone[sliceInd][stripInd]) {
    
          // sum into the PS curve.  First add the top and bottom lines
          // from the bottom half of the spectrum
          for (iy = 0; iy <= mTileSize / 2; iy += mTileSize / 2) {
            for (ix = 0; ix < fftXdim; ix++) {
              ix0 = iy * fftXdim + ix;
              ind = iy * tileXdim + 2 * ix;
              psTmp[mTileToPsInd[ix0]] +=
                mTile[ind] * mTile[ind] + mTile[ind + 1] * mTile[ind + 1];
            }
          }

          // Then add complementary lines from top and bottom half for the rest
          for (iy = 1; iy < mTileSize / 2; iy++) {
            for (ix = 0; ix < fftXdim; ix++) {
              ix0 = iy * fftXdim + ix;
              ind = iy * tileXdim + 2 * ix;
              ind2 = (mTileSize - iy) * tileXdim + 2 * ix;
              psTmp[mTileToPsInd[ix0]] +=
                mTile[ind] * mTile[ind] + mTile[ind + 1] * mTile[ind + 1] +
                mTile[ind2] * mTile[ind2] + mTile[ind2 + 1] * mTile[ind2 + 1];
            }
          }
        }

        // Get the sector PS into the cache if necessary
        if (doingWedge && mCachedStripDone[sliceInd][stripInd] < 2) {
          sectorPS = tileIsOnLeft ? sectorLeftPS : sectorRightPS;
          for (iy = 0; iy < mTileSize; iy++) {
            for (ix = 0; ix < fftXdim; ix++) {
              ix0 = iy * fftXdim + ix;
              ind = iy * tileXdim + 2 * ix;
              sectorPS[mTileToSectorInd[ix0]] +=
                mTile[ind] * mTile[ind] + mTile[ind + 1] * mTile[ind + 1];
            }
          }
        }
      } else {

        // Add the amplitude spectrum to the sum right in the cache
        ampSpecTmp = tileIsOnLeft ? *leftPsSum : *rightPsSum;
        for (iy = 0; iy < mTileSize; iy++) {
          for (ix = 0; ix < fftXdim; ix++) {
            ind = 2 * (iy * fftXdim + ix);
            ampSpecTmp[iy * fftXdim + ix] += 
              sqrtf(mTile[ind] * mTile[ind] + mTile[ind + 1] * mTile[ind + 1]);
          }
        }
      }
    }
    
    if (mHyperRes > 0) {
      mStripLeftMeans[whichSlice][stripInd] = leftMean;
      mStripRightMeans[whichSlice][stripInd] = rightMean;

      // Store the strip spectrum into the cache array
      if (!mCachedStripDone[sliceInd][stripInd]) {
        for (ii = 0; ii < mOnePsSize; ii++) {
          mStripLeftPS[sliceInd][stripInd][ii] = mLeftPsTmp[ii];
          mStripRightPS[sliceInd][stripInd][ii] = mRightPsTmp[ii];
        }
      }
    }
    mCachedStripDone[sliceInd][stripInd] = (mHyperRes > 0 && wedgeRange) ? 2 : 1;
  }
     
  if (doingWedge) {

    // Determine angular range of wedge components
    getWedgeMinMaxes(wedgeCenter, wedgeRange, plusMin, plusMax, minusMin, minusMax);

    // If using wedge cache, get the last ranges for this strip
    adjustLastWedge = false;
    if (wedgeInd >= 0) {
      lastRange = mLastWedgeRanges[sliceInd][stripInd];
      if (lastRange > 0.) {
        numTot = 0;
        numAddOrSub = 0;
        getWedgeMinMaxes(mLastWedgeCenters[sliceInd][stripInd], lastRange, lastPlusMin,
                         lastPlusMax, lastMinusMin, lastMinusMax);
      
        // Count up total sectors to do and number not overlapping
        for (ix = 0; ix < mNumSectors; ix++) {
          sectAngle = (ix + 0.5) * 180. / mNumSectors - 90.;
          inCurWedge = (sectAngle >= plusMin && sectAngle < plusMax) || 
            (sectAngle >= minusMin && sectAngle < minusMax);
          inLastWedge = (sectAngle >= lastPlusMin && sectAngle < lastPlusMax) ||
            (sectAngle >= lastMinusMin && sectAngle < lastMinusMax);
          if (inCurWedge)
            numTot += 1;
          if ((inLastWedge && !inCurWedge) || (!inLastWedge && inCurWedge))
            numAddOrSub++;
        }

        // Do add and subtract if it saves 2 or more operations
        adjustLastWedge = numAddOrSub < numTot - 1;
      }  
    }
      
    // Now loop on the sectors and add them in if in the range, or subtract if now out
    sectorPS = sectorLeftPS;
    wedgePS = wedgeLeftPS;
    retPS = *leftPsSum;
    for (leftRight = 0; leftRight < 2; leftRight++) {

      // Clear wedge unless adjusting
      if (!adjustLastWedge)
        for (ii = 0; ii < mOnePsSize; ii++)
          wedgePS[ii] = 0.;
    
      for (ix = 0; ix < mNumSectors; ix++) {
        sectAngle = (ix + 0.5) * 180. / mNumSectors - 90.;
        ind = ix * mOnePsSize;
        
        // If adjusting, add in if in current and not last, or subtract if in last range,
        // and not current
        inCurWedge = (sectAngle >= plusMin && sectAngle < plusMax) || 
          (sectAngle >= minusMin && sectAngle < minusMax);
        if (adjustLastWedge) {
          inLastWedge = (sectAngle >= lastPlusMin && sectAngle < lastPlusMax) ||
            (sectAngle >= lastMinusMin && sectAngle < lastMinusMax);
          
          if (inCurWedge && !inLastWedge)
            for (ii = 0; ii < mOnePsSize; ii++)
              wedgePS[ii] += sectorPS[ii + ind];
          if (inLastWedge && !inCurWedge)
            for (ii = 0; ii < mOnePsSize; ii++)
              wedgePS[ii] -= sectorPS[ii + ind];
        } else if (inCurWedge) {

          // Or just add in if in current wedge
          for (ii = 0; ii < mOnePsSize; ii++) 
            wedgePS[ii] += sectorPS[ii + ind];
        }
      }

      // Copy result to cache and/or temp array being returned, set pointers for right
      for (ii = 0; ii < mOnePsSize; ii++)
        retPS[ii] = wedgePS[ii];
      sectorPS = sectorRightPS;
      wedgePS = wedgeRightPS;
      retPS = *rightPsSum;
    }
    
    // Set wedge information if using cache
    if (wedgeInd >= 0) {
      mStripWedgeDone[sliceInd][stripInd][wedgeInd] = 1;
      mLastWedgeCenters[sliceInd][stripInd] = wedgeCenter;
      mLastWedgeRanges[sliceInd][stripInd] = wedgeRange;
    }
    
  } 
  mWall1D += wallTime() - wallStart;
}

/*
 * Returns the whole-spectrum frequency count array or an array for the current wedge
 */
int *SliceCache::getFreqCount()
{
  int wedgeCenter = mApp->getWedgeCenterAngle();
  int wedgeRange = mApp->getNextSpecWedgeRange();
  float plusMin, plusMax, minusMin, minusMax;
  if (wedgeRange <= 0.)
    return mFreqCount;
  float sectAngle;
  int ii, ix, ind;

  getWedgeMinMaxes(wedgeCenter, wedgeRange, plusMin, plusMax, minusMin, minusMax);
  for (ii = 0; ii < mOnePsSize; ii++)
    mWedgeFreqCount[ii] = 0;

  for (ix = 0; ix < mNumSectors; ix++) {
    sectAngle = (ix + 0.5) * 180. / mNumSectors - 90.;
    if ((sectAngle >= plusMin && sectAngle < plusMax) || 
        (sectAngle >= minusMin && sectAngle < minusMax)) {
      ind = (ix + 1) * mOnePsSize;
      for (ii = 0; ii < mOnePsSize; ii++)
        mWedgeFreqCount[ii] += mFreqCount[ii + ind];
    }
  }
  return mWedgeFreqCount;
}

// Returns angle of specific slice
float SliceCache::getAngle(int whichSlice)
{
  int sliceInd = cacheIndex(whichSlice);
  if (mApp->getNoAnglesEntered())
    return 0.;

  if (sliceInd >= 0)
    return mSliceAngles[sliceInd];
  return readAngle(whichSlice);
}

/*
 * Returns two pairs of min/max angle values for testing whether a sector is in the wedge
 * with givem center and range
 */
void SliceCache::getWedgeMinMaxes(float center, float range, float &plusMin,
                                  float &plusMax, float &minusMin, float &minusMax)
{
  plusMin = center - range / 2.;
  plusMax = center + range / 2.;
  minusMin = center + B3DCHOICE(center > 0, -180, 180) - range / 2.;
  minusMax = center + B3DCHOICE(center > 0, -180, 180) + range / 2.;
}
