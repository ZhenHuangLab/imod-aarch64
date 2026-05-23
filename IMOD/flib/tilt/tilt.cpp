// -----------------------------------------------------------------
//
// tilt  A program for reconstructing a 3-D volume by backprojection or reprojecting
//       such a volume
//
// $Id$
//
/*
 * A note on some aspects of the code that reflect its origin as a Fortran program:
 * Almost all of the translation from Fortran to C was done initially with a script 
 * (f90tocpp) that handled the routine modifications needed.  The script handled
 * simple do statements by converting to a for loop from the starting index through the
 * ending index (i.e., with <=).  Also, it inserted "- 1" when converting identifiable 
 * array references (i.e, ones following member variables) from (...) to [... - 1].
 * Thus in many situations, loop and array indexes are numbered from 1.  This was 
 * particularly important where the index corresponded to a coordinate, but was also 
 * helpful for minimizing risky hand-editing of indexes.  On the other hand, in many 
 * simple loops, loops and indexes were converted to 0-based.
 * Linear transformations are stored in memory with Fortran ordering, a11, a21, a12, a22.
 * The middle terms are swapped from the way they are stored in the file as well as from
 * the order in which they are used to call amatToRotmagstr.
 * The initial goal was to have a pure C program, but some Fortran components were
 * retained to avoid a serious loss in performance (by factors of 1.5 -2).
 */
#include "cppdefs.h"
#include "b3dutil.h"
#include "iiunit.h"
#include "parse_params.h"
#include "imodel.h"
#include "cfft.h"
#include "gpubp.h"
#include "tilt.h"

/*
 * Declarations for remaining Fortran routines
 */ 
#ifdef F77FUNCAP
#define bpsumnox BPSUMNOX
#define bpsumxtilt BPSUMXTILT
#define bpsumlocal BPSUMLOCAL
#define projsumlocal PROJSUMLOCAL
#define loadedprojectingpoint LOADEDPROJECTINGPOINT
#define set_projection_rays SET_PROJECTION_RAYS
#else
#define bpsumnox bpsumnox_
#define bpsumxtilt bpsumxtilt_
#define bpsumlocal bpsumlocal_
#define projsumlocal projsumlocal_
#define loadedprojectingpoint loadedprojectingpoint_
#ifdef G77__HACK
#define set_projection_rays set_projection_rays__
#else
#define set_projection_rays set_projection_rays_
#endif
#endif

extern "C" {
  void bpsumnox(float *array, int *ind1, float *input, int *ipoint, int *n,
                double *xProj1, float *cosBeta);
  void bpsumxtilt(float *array, int *ind1, float *input, int *ipbase, int *ipdel, int *n,
                  double *xproj1, float *cbeta, float *yfrac, float *omyfrac);
  void bpsumlocal(float *array, int *index, float *input, float *zz, float *xprojf,
                  float *xprojz, float *yprojf, float *yprojz, int *ipoint, int *ipdel,
                  int *lslice, int *jstrt, int *jend, int *projSuperFac);
  void set_projection_rays(float *sinAng, float *cosAng, int *nxSlice, int *nySlice,
                           int *nxOut, float *xrayStart, float *yrayStart, 
                           int *numRayIncrem, int *nrayMax);
  void projsumlocal(float *xx, float *yy, float *zz, double *sum, float *xproj,
                    float *yproj,
                    float *array, float *xProjFs, float *xProjZs, float *yProjFs, 
                    float *yProjZs, int *numWarpDelz, float *dxWarpDelz, float *warpDelz,
                    int *ithickReproj, float *sinBeta, int *nxLoad, int *inLoadStart,
                    int *inLoadEnd, int *inPlaneSize, float *zJump, float *ycenAdj);
  void loadedprojectingpoint(float *xproj, float *yproj, float *zz, int *nxLoad,
                             int *inLoadStart, int *inLoadEnd, float *xProjFs,
                             float *xProjZs, float *yProjFs, float *yProjZs,
                             float *xx, float *yy);
}

// A short main
int main(int argc, char *argv[])
{
  Tilt tilt;
  tilt.main(argc, argv);
  exit(0);
}

// Most initializations are in inputParameters
Tilt::Tilt()
{
  mNeededStarts = NULL;
  mNeededEnds = NULL;
  mFilterArray = NULL;
  mOutSliceArr = NULL;
  mVertSliceArr = NULL;
  mInputArray = NULL;
  mReadInArray = NULL;
  mSuperOutArr = NULL;
  mWorkArray = NULL;
  mReprojLines = NULL;
  mLoadBuffer = NULL;
}

/*
 * Main runs the sequence of operations
 */
void Tilt::main(int argc, char *argv[])
{
  int nxyzTmp[3], mxyzTmp[3], nxyzst[3] = {0., 0., 0.};
  char *format920 = "Reading in view %d for slice %d\n";
  float *loadBuf;
  //
  int numSliceOut, numSlices, nxprj2;
  int inLoadStart, inLoadEnd, lastReady, lastCalc, nextFreeVertSlice;
  int lvertSliceStart, lvertSliceEnd, numVertSliceInRing, ni, loadLimit, lsliceOut;
  int lsliceStart, lsliceEnd, lslice, needStart, needEnd, itryEnd, ibaseSIRT;
  int itry, ifEnough, lastStart, lastEnd, i, lsliceMin, lsliceMax, nextReadFree;
  int iv, numAlready, lsliceProjEnd, lReadStart, lReadEnd, numReadInRing;
  float dmin, dmax, ycenFix, absSinAlf, tanAlpha, dmin4, dmax4, dmin5, dmax5, dmin6;
  float valMin, vertSliceCen, vertYcenFix, riBot, riTop, tmax, tmean, tmin, dmax6;
  int lriMin, lriMax, loadStart, loadEnd, lri, isirtIter, ierr, j, k;
  int ibase, lreadStart, nv, istart, nl, indBuf, ix, ioffset, iyload;
  int iringStart, mode, needGpuStart, needGpuEnd, keepOnGpu, numLoadGpu;
  float unscaledMin, unscaledMax, recScale, recAdd, dmean, pixelTot, val;
  float reprojFill, curMean, firstMean, vertSum, numVertSum, edgeFillOrig;
  float composeFill;
  float *sirtArray;
  int iset, nz5, lfillStart, lfillEnd, indv;
  double dtot8, timeStart, dsum, dpix;
  bool shiftedGpuLoad, composedOne, truncations, extremes, lineByLine;
  //
  numSliceOut = 0;
  dtot8 = 0.;
  dmin = 1.e30;
  dmax = -1.e30;
  dmin4 = 1.e30;
  dmax4 = -1.e30;
  dmin5 = 1.e30;
  dmax5 = -1.e30;
  dmin6 = 1.e30;
  dmax6 = -1.e30;
  nz5 = 0;
  mDebug = false;
  mSkipUnseenPoints = 0;
  //
  // Open files and read control data
  inputParameters(argc, argv);
  numSlices = (mIsliceEnd - mIsliceStart) / mIdelSlice + 1;
  mIntervalHeadSave = B3DMAX(20, numSlices / 50);
  valMin = 1.e-3 * (mDmaxIn - mDminIn);
  mEdgeFill = mDmeanIn;
  if (mIfLog != 0) 
    mEdgeFill = log10(B3DMAX(valMin, mDmeanIn + mBaseForLog));
  mEdgeFill = mEdgeFill * mZeroWeight;
  edgeFillOrig = mEdgeFill;

  if (mDebug)
    printf("iflog= %d scale= %f  edgefill= %f\n", mIfLog, mOutScale, mEdgeFill);
  composeFill = mEdgeFill * mNumViews;
  //
  // recompute items not in common
  nxprj2 = mNxProj + 2 + mNumPad;
  //
  // initialize variables for loaded slices
  inLoadStart = 0;
  inLoadEnd = 0;
  lastReady = 0;
  lastCalc = 0;
  //
  // initialize variables for ring buffer of vertical slices: # in the ring, next free
  // position, starting and ending slice number of slices in ring
  numVertSliceInRing = 0;
  nextFreeVertSlice = 1;
  lvertSliceStart = -1;
  lvertSliceEnd = -1;
  //
  // initialize similar variables for ring buffer of read-in slices
  numReadInRing = 0;
  nextReadFree = 1;
  lReadStart = -1;
  lReadEnd = -1;
  lfillStart = -1;
  ycenFix = mYcenOut;
  absSinAlf = B3DABS(mSinAlpha[1 - 1]);
  composedOne = mIfAlpha >= 0;
  numVertSum = 0;
  vertSum = 0.;
  //
  // Report memory allocations for old-times sake
  ni = mNumPlanes * mInPlaneSize;
  if (! mRecReproj) {
    printf("---------------------------\nTotal floats in main arrays %11ld\n"
           "  Radial weighting function  %10d\n  Output slice             %12d\n",
           mMaxStack, mNeedForFiltArr, (int)mIsliceSizeBP);
    if (mNeedForSuperArr)
      printf("  Super-sampled slice      %12d\n", (int)mNeedForSuperArr);
    if (mIfAlpha < 0)
      printf(" %4d Untilted slices        %10ld\n" , mNumVertNeeded, mNeedForVertArr);
    if (mNumSIRTiter > 0) {
      if (mNeedForReadInArr)
        printf(" %4d Slice(s) for SIRT      %10ld\n", B3DMAX(1, mNumReadNeed),
               mNeedForReadInArr);
      printf("  Reprojection lines for SIRT %9d\n", mInPlaneSize);
    }
    printf(" %4d Transposed projections %10d\n", mNumPlanes, ni);
    if (mIpExtraSize != 0) 
      printf("  Stretching buffer           %9d\n", mIpExtraSize);
    //
    // Prepare fixed maskEdges
    if (mIfAlpha == 0 || !mMaskEdges) 
      maskPrep(mIsliceStart);
  }
  if (mDebug)
    printf("slicen %.1f\n", mCenterSlice);
  if (mIfAlpha >= 0) {
    loadLimit = mIsliceEnd;
  } else {
    loadLimit = mCenterSlice + (mIsliceEnd - mCenterSlice) * mCosAlpha[1 - 1] +  
      mYOffset * mSinAlpha[1 - 1] + 0.5 * mIthickOut * absSinAlf + 2.;
  }
  //
  // Main loop over slices perpendicular to tilt axis
  // ------------------------------------------------
  lsliceOut = mIsliceStart;
  while (lsliceOut <= mIsliceEnd) {
    //
    // get limits for slices that are needed: the slice itself for regular
    // work, or required vertical slices for new-style X tilting
    //
    if (mDebug)
      printf("working on %d\n", lsliceOut);
    if (mIfAlpha >= 0) {
      lsliceStart = lsliceOut;
      lsliceEnd = lsliceOut;
    } else {
      tanAlpha = mSinAlpha[1 - 1] / mCosAlpha[1 - 1];
      lsliceMin = mCenterSlice + (lsliceOut - mCenterSlice) * mCosAlpha[1 - 1] +  
        mYOffset * mSinAlpha[1 - 1] - 0.5 * mIthickOut * absSinAlf - 1.;
      lsliceMax = mCenterSlice + (lsliceOut - mCenterSlice) * mCosAlpha[1 - 1] +  
        mYOffset * mSinAlpha[1 - 1] + 0.5 * mIthickOut * absSinAlf + 2.;
      if (mDebug)
        printf("need slices %d %d\n", lsliceMin, lsliceMax);
      lsliceMin = B3DMAX(1, lsliceMin);
      lsliceMax = B3DMIN(mNyProj, lsliceMax);
      lsliceStart = lsliceMin;
      if (lsliceMin >= lvertSliceStart && lsliceMin <= lvertSliceEnd)
        lsliceStart = lvertSliceEnd + 1;
      lsliceEnd = lsliceMax;
    }
    //
    // loop on needed vertical slices
    //
    if (mDebug)
      printf("looping to get %d %d\n", lsliceStart, lsliceEnd);
    
    for (lslice = lsliceStart; lslice <= lsliceEnd; lslice++) {
      shiftedGpuLoad = false;
      //
      // Load stack with as many lines from projections as will
      // fit into the remaining space.
      //
      // Enter loading procedures unless the load is already set for the
      // current slice
      //
      if (inLoadStart == 0 || lslice > lastReady) {
        needStart = 0;
        needEnd = 0;
        itryEnd = 0;
        lastReady = 0;
        itry = lslice;
        ifEnough = 0;
        //
        // loop on successive output slices to find what input slices are
        // needed for them; until all slices would be loaded or there
        // would be no more room
        while (itry > 0 && itry <= mNyProj && ifEnough == 0 && 
               itry <= loadLimit && itryEnd - needStart + 1 <= mNumPlanes) {
          lastStart = mNeededStarts[itry - mIndNeededBase - 1];
          lastEnd = mNeededEnds[itry - mIndNeededBase - 1];
          //
          // values here must work in single-slice case too
          // if this is the first time, set needstart
          // if this load still fits, set needend and lastready
          //
          itryEnd = lastEnd;
          if (needStart == 0)
            needStart = lastStart;
          if (itryEnd - needStart + 1 <= mNumPlanes) {
            needEnd = itryEnd;
            lastReady = itry;
          } else {
            ifEnough = 1;
          }
          itry = itry + mIdelSlice;
        }
        if (mDebug)
          printf("itryend %d, needstart %d, needend %d, lastready %d\n", itryEnd, 
                 needStart, needEnd, lastReady);
        if (needEnd == 0)
          exitError("Insufficient size for input array to do a single slice");
        //
        // if some are already loaded, need to shift them down
        //
        numAlready = B3DMAX(0, inLoadEnd - needStart + 1);
        if (inLoadEnd == 0)
          numAlready = 0;
        ioffset = (needStart - inLoadStart) * mInPlaneSize;
        for (i = 0; i < numAlready; i++) {
          memcpy(&mInputArray[i * mInPlaneSize], &mInputArray[i * mInPlaneSize + ioffset],
                 mInPlaneSize * sizeof(float));
        }
        if (numAlready != 0 && mDebug)
          printf("shifting %d %d\n", needStart, inLoadEnd);
        //
        // If it is also time to load something on GPU, shift existing
        // data if appropriate and enable copy of filtered data
        if (mNumGpuPlanes > 0) {
          if (mLoadGpuStart <= 0 || mNeededEnds[lsliceOut - mIndNeededBase - 1] > 
              mLoadGpuEnd) 
            shiftGpuSetupCopy(lsliceOut, needEnd, needGpuStart, needGpuEnd, keepOnGpu,
                              numLoadGpu, shiftedGpuLoad);
        }
        //
        // load the planes in one plane high if stretching
        //
        ibase = 1 + numAlready * mInPlaneSize + mIpExtraSize;
        lreadStart = needStart + numAlready;
        lineByLine = lreadStart < 1 || needEnd > mNyProj || mIdelSlice != 1;
        //
        if (mDebug)
          printf("loading %d %d %s\n", lreadStart, needEnd, lineByLine ? "line by line" :
                 "");
        if (! mRecReproj) {
          for (nv = 1; nv <= mNumViews; nv++) {
            istart = ibase;

            if (!lineByLine) {

              // Read from projection NV, lines lreadStart to needEnd
              iiuSetPosition(1, mMapUsedView[nv - 1] - 1, lreadStart - 1);
              if (iiuReadLines(1, (char *)mLoadBuffer, needEnd + 1 - lreadStart))
                exitError(format920, mMapUsedView[nv - 1], lreadStart);
              indBuf = 0;
              loadBuf = mLoadBuffer;
            }

            // Distribute lines into the planes of the input array
            for (nl = lreadStart; nl <= needEnd; nl += mIdelSlice) {
              if (lineByLine) {
                // Position to read from projection NV at record NL
                iyload = B3DMAX(0, B3DMIN(mNyProj - 1, nl - 1));
                iiuSetPosition(1, mMapUsedView[nv - 1] - 1, iyload);
                if (iiuReadLines(1, (char *)&mInputArray[istart - 1], 1))
                  exitError(format920, mMapUsedView[nv - 1], nl);
                loadBuf = mInputArray;
                indBuf = istart - 1;
              }

              // Take log if requested
              // 3/31/04: limit values to .001 time dynamic range
              if (mIfLog != 0) {
                for (ix = istart; ix <= istart + mNxProj - 1; ix++) {
                  val = loadBuf[indBuf++] + mBaseForLog;
                  mInputArray[ix - 1] = log10(B3DMAX(valMin, val));
                }
              } else {
                for (ix = istart; ix <= istart + mNxProj - 1; ix++) {
                  mInputArray[ix - 1] = loadBuf[indBuf++] * mExposeWeight[nv - 1];
                }
              }
              //
              // pad with taper between start and end of line
              taperEndToStart(&mInputArray[istart - 1]);
              //
              istart = istart + mInPlaneSize;
            }
            ibase = ibase + nxprj2;
          }
          //
        } else {
          //
          // Load reconstruction for projections
          for (nl = lreadStart; nl <= needEnd; nl += mIdelSlice) {
            iiuSetPosition(3, nl - 1, 0);
            if (iiuReadSecPart(3, (char *)&mInputArray[ibase - 1], 
                               mMaxXload + 1 - mMinXload, mMinXload - 1,
                               mMaxXload - 1, mMinYreproj - 1, mMaxYreproj - 1))
              exitError(format920, mMapUsedView[nv - 1], nl);

            //
            // Undo the scaling that was used to write, and apply a
            // scaling that will make the data close for projecting
            for (indv = ibase; indv <= ibase + mIsliceSizeBP - 1; indv++)
              mInputArray[indv - 1] = (mInputArray[indv - 1] / mOutScale - mOutAdd) / 
                mFilterScale;
            ibase = ibase + mInPlaneSize;
          }
        }
        if (!mRecReproj && mNumSIRTiter <= 0) {
          ibase = numAlready * mInPlaneSize;
          for (nl = lreadStart; nl <= needEnd; nl += mIdelSlice) {
            transform(&mInputArray[ibase], nl, 1);
            ibase = ibase + mInPlaneSize;
          }
        }
        inLoadStart = needStart;
        inLoadEnd = needEnd;
        mIndLoadEnd = 1 + mInPlaneSize *  
          ((inLoadEnd - inLoadStart) / mIdelSlice + 1) - 1;
      }
      //
      // Stack is full.  Now check if GPU needs to be loaded
      if (mNumGpuPlanes > 0) {
        if (mLoadGpuStart <= 0 || mNeededEnds[lsliceOut - mIndNeededBase - 1] > 
            mLoadGpuEnd) {
          //
          // Load as much as possible.  If the shift fails the first time
          // it will set loadGpuStart to 0, call again to recompute for
          // full load
          if (! shiftedGpuLoad)
            shiftGpuSetupCopy(lsliceOut, needEnd, needGpuStart, needGpuEnd, keepOnGpu,
                              numLoadGpu, shiftedGpuLoad);
          if (! shiftedGpuLoad)
            shiftGpuSetupCopy(lsliceOut, needEnd, needGpuStart, needGpuEnd, keepOnGpu,
                              numLoadGpu, shiftedGpuLoad);
          ibase = 1 + (needGpuStart + keepOnGpu - inLoadStart) * mInPlaneSize;
          if (mDebug) 
            printf("Loading GPU, # %d  lstart %d  start pos base %d  %.0f\n", numLoadGpu,
                   needGpuStart + keepOnGpu, keepOnGpu + 1, 
                   (double)(ibase - 1));
          timeStart = wallTime();
          j = needGpuStart + keepOnGpu;
          k = keepOnGpu + 1;
          if (gpuLoadProj(&mInputArray[ibase - 1], numLoadGpu, j, k) == 0) {
            mLoadGpuStart = needGpuStart;
            mLoadGpuEnd = needGpuEnd;
          } else {
            mLoadGpuStart = 0;
          }
          if (mDebug) 
            printf("Loading time %.4f\n", wallTime() - timeStart);
        }
      }
      //
      istart = 1 + mInPlaneSize * (lslice - inLoadStart) / mIdelSlice;
      if (!mRecReproj) {
        //
        // Backprojection: Process all views for current slice
        // If new-style X tilt, set  the Y center based on the slice
        // number, and adjust the y offset slightly
        //
        if (mIfAlpha < 0)
          mYcenOut = ycenFix + (1. / mCosAlpha[1 - 1] - 1.) * mYOffset 
            - B3DNINT(tanAlpha * (lslice - mCenterSlice));
        if (mNumSIRTiter == 0) {
          //
          // PRINT3(1, lslice, inLoadStart);
          // print *,'projecting', lslice, ' at', istart, ', ycen =', ycenOut
          project(mInputArray, istart, lslice);
          if (lslice == 3000)
            mrcWriteImageToFile("mask3000.mrc", mOutSliceArr, 2, mIwidth, mIthickBP);
          //
          // move vertical slice into ring buffer, adjust ring variables
          //
          if (mIfAlpha < 0) {
            // print *,'moving slice to ring position', nextfreevs
            ioffset = (nextFreeVertSlice - 1) * mIthickBP * mIwidth;
            for (i = 0; i < mIthickBP * mIwidth; i++) {
              mVertSliceArr[i + ioffset] = mOutSliceArr[i];
            }
            manageRing(mNumVertNeeded, numVertSliceInRing, nextFreeVertSlice, 
                       lvertSliceStart, lvertSliceEnd, lslice);
          }
        } else {
          //
          // for SIRT, either do the zero iteration here, load single slice
          // into read-in slice spot, or decompose vertical slice from read-
          // in slices into spot in  vertical slice ring buffer.
          // Descale them by output scaling only
          // Set up for starting from read-in data
          recScale = mFilterScale * mNumViews;
          iset = 1;
          reprojFill = mDmeanIn;
          if (mSirtFromZero) {
            //
            // Doing zero iteration: set up location to place slice
            sirtArray = mReadInArray;
            ibaseSIRT = 1;
            if (mIfAlpha < 0) {
              sirtArray = mVertSliceArr;
              ibaseSIRT = 1 + (nextFreeVertSlice - 1) * mIthickBP * mIwidth;
              manageRing(mNumVertNeeded, numVertSliceInRing, nextFreeVertSlice, 
                         lvertSliceStart, lvertSliceEnd, lslice);
            }
            //
            // Copy and filter the lines needed by filter set 1
            for (iv = 1; iv <= mNumViews; iv++) {
              j = (iv - 1) * nxprj2;
              k = istart + (iv - 1) * nxprj2;
              for (i = 0; i <= nxprj2 - 3; i++) {
                mWorkArray[j + i] = mInputArray[k + i - 1];
              }
            }
            transform(mWorkArray, lslice, 1);
            mEdgeFill = edgeFillOrig;
            //
            // Backproject and maskEdges/taper edges
            project(mWorkArray, 1, lslice);
            if (mMaskEdges)
              maskSlice(mOutSliceArr, mIthickBP);
            for (indv = 0; indv < mIsliceSizeBP; indv++)
              sirtArray[indv + ibaseSIRT - 1] = mOutSliceArr[indv] / recScale;
            iset = 2;
            if (mIfOutSirtRec == 3) {
              printf("writing zero iteration slice %d\n", lslice);
              writeInternalSlice(5, &sirtArray[ibaseSIRT - 1], lslice, nxprj2, dmin4,
                                 dmax4, dmin5, dmax5, nz5);
            }

          } else if (mIfAlpha == 0) {
            //
            // Read in single slice
            iiuSetPosition(3, lslice - 1, 0);
            if (iiuReadSection(3, (char *)mReadInArray))
              exitError(format920, mMapUsedView[nv - 1], nl);
            dsum = 0.;
            dpix = 0.;
            for (i = 0; i <= mIthickOut * mIwidth - 1; i++) {
              mReadInArray[i] = (mReadInArray[i] / mOutScale - mOutAdd);
              dsum = dsum + mReadInArray[i];
              dpix = dpix + 1.;
            }
            if (mDebug)
              printf("Loaded mean= %f   pmean= %f\n", dsum / dpix, mDmeanIn);
            ibaseSIRT = 1;
            sirtArray = mReadInArray;
          } else {
            //
            // Vertical slices: read file directly if it is vertical slices
            ibaseSIRT = 1 + (nextFreeVertSlice - 1) * mIthickBP * mIwidth;
            sirtArray = mVertSliceArr;
            if (mVertSirtInput) {
              if (mDebug)
                printf("loading vertical slice %d\n", lslice);
              iiuSetPosition(3, lslice - 1, 0);
              if (iiuReadSection(3, (char *)&sirtArray[ibaseSIRT - 1]))
                exitError(format920, mMapUsedView[nv - 1], nl);

            } else {
              //
              // Decompose vertical slice from read-in slices
              // First see if necessary slices are loaded in ring
              vertSliceCen = lslice - mCenterSlice;
              vertYcenFix = (mIthickBP / 2 + 0.5) - 
                B3DNINT(tanAlpha * (lslice - mCenterSlice)) + mYOffset / mCosAlpha[1 - 1];
              riBot = mCenterSlice + vertSliceCen * mCosAlpha[1 - 1] + 
                (1 - vertYcenFix) * mSinAlpha[1 - 1];
              riTop = riBot + (mIthickBP - 1) * mSinAlpha[1 - 1];
              lriMin = B3DMAX(1, floor(B3DMIN(riBot, riTop)));
              lriMax = B3DMIN(mNyProj, ceil(B3DMAX(riBot, riTop)));

              loadStart = lriMin;
              if (lriMin >= lReadStart && lriMin <= lReadEnd)
                loadStart = lReadEnd + 1;
              loadEnd = lriMax;
              //
              // Read into ring and manage the ring pointers
              if (mDebug && loadEnd >= loadStart)
                printf("reading into ring %d %d\n", loadStart, loadEnd);
              for (lri = loadStart; lri <= loadEnd; lri++) {
                iiuSetPosition(3, lri - 1, 0);
                ioffset = (nextReadFree - 1) * mIthickOut * mIwidth;
                if (iiuReadSection(3, (char *)&mReadInArray[ioffset]))
                  exitError(format920, mMapUsedView[nv - 1], nl);
                for (i = 0; i < mIthickOut * mIwidth; i++) {
                  mReadInArray[i + ioffset] = (mReadInArray[i + ioffset] / mOutScale - 
                                               mOutAdd);
                }
                manageRing(mNumReadNeed, numReadInRing, nextReadFree, lReadStart, 
                           lReadEnd, lri);
              }
              iringStart = 1;
              if (numReadInRing == mNumReadNeed)
                iringStart = nextReadFree;
              decompose(lslice, lReadStart, lReadEnd, iringStart, 
                        &sirtArray[ibaseSIRT - 1]);
            }
            if (mIfOutSirtRec == 4) {
              printf("writing decomposed slice %d\n", lslice);
              writeInternalSlice(5, &sirtArray[ibaseSIRT - 1], lslice, nxprj2, dmin4,
                                 dmax4, dmin5, dmax5, nz5);
            }
            manageRing(mNumVertNeeded, numVertSliceInRing, nextFreeVertSlice, 
                       lvertSliceStart, lvertSliceEnd, lslice);
          }
          //
          // Ready to iterate.  First get the starting slice mean
          arrayMinMaxMean(&sirtArray[ibaseSIRT - 1], mIwidth, mIthickBP, 0, mIwidth - 1,
                          0, mIthickBP - 1, &unscaledMin, &unscaledMax, &firstMean);
          // It seems to give less edge artifacts using the mean all the time
          // if (sirtFromZero) rpfill = firstmean
          reprojFill = firstMean;
          //
          for (isirtIter = 1; isirtIter <= mNumSIRTiter; isirtIter++) {
            ierr = 1;
            timeStart = wallTime();

            if (mUseGPU) {
              ierr = gpuReprojOneSlice(&sirtArray[ibaseSIRT - 1], mWorkArray,
                                       mSinReproj, mCosReproj, mYcenOut, mNumViews, 
                                       reprojFill);
            }
            if (ierr != 0) {
              for (iv = 1; iv <= mNumViews; iv++) {
                i = (iv - 1) * mIwidth + 1;
                j = (iv - 1) * nxprj2;
                // call reproject(array(ibaseSIRT), iwidth, ithickBP, iwidth, &
                // sinReproj(iv), cosReproj(iv), xRayStart(i), yRayStart(i), &
                // numPixInRay(i), maxRayPixels(iv), dmeanIn, array(j), 1, 1)
                reprojOneAngle(&sirtArray[ibaseSIRT - 1], &mWorkArray[j], 1, 1, 1, 
                               mCosReproj[iv - 1], -mSinReproj[iv - 1], 1., 0.,
                               mCosReproj[iv - 1], mIwidth, mIthickBP,
                               mIwidth * mIthickBP, mIwidth, 1, 1, 0., 0., 
                               mIwidth / 2 + 0.5, mYcenOut, mIwidth / 2 + 0.5, 
                               mCenterSlice, 0, 0., 0., reprojFill);
              }
            }
            if (mDebug) 
              printf("Reproj time = %.4f\n", wallTime() - timeStart);
            if (isirtIter == mNumSIRTiter && mIfOutSirtProj == 1)
              writeInternalSlice(4, mWorkArray, lslice, nxprj2, dmin4, dmax4, 
                                 dmin5, dmax5, nz5);
            //
            // Subtract input projection lines from result.  No scaling
            // needed here; these intensities should be in register
            // Taper ends
            // dsum = 0.
            for (iv = 1; iv <= mNumViews; iv++) {
              j = (iv - 1) * nxprj2;
              k = istart + (iv - 1) * nxprj2;
              for (i = 0; i < mIwidth; i++) {
                mWorkArray[j + i] -= mInputArray[k + i - 1];
              }
              taperEndToStart(&mWorkArray[j]);
            }
            if (isirtIter == mNumSIRTiter && mIfOutSirtProj == 2)
              writeInternalSlice(4, mWorkArray, lslice, nxprj2, dmin4, dmax4, 
                                 dmin5, dmax5, nz5);
            //
            // filter the working difference lines and get a rough value for
            // the edgeFill.  Trying to get this better did not prevent edge
            // artifacts - the masking was needed
            transform(mWorkArray, lslice, iset);
            dsum = 0.;
            for (iv = 1; iv <= mNumViews; iv++) {
              j = (iv - 1) * nxprj2;
              dsum = dsum + mWorkArray[j + mIwidth + mNumPad / 2 - 1];
            }
            mEdgeFill = dsum / mNumViews;
            // print *,'   (for diff bp) edgefill =', edgeFill
            //
            // Backproject the difference
            project(mWorkArray, 1, lslice);
            if (isirtIter == mNumSIRTiter && mIfOutSirtRec == 1) {
              for (indv = 0; indv < mIsliceSizeBP; indv++)
                mOutSliceArr[indv] = (mOutSliceArr[indv] + mOutAdd * recScale) * 
                  (mOutScale / recScale);
              printf("writing bp difference %d\n", lslice);
              writeInternalSlice(5, mOutSliceArr, lslice, nxprj2, dmin4, dmax4,
                                 dmin5, dmax5, nz5);
              for (indv = 0; indv < mIsliceSizeBP; indv++)
                mOutSliceArr[indv] = mOutSliceArr[indv] / (mOutScale / recScale) - 
                  mOutAdd * recScale;
            }
            //
            // Accumulate report values
            if (mIterForReport > 0)
              sampleForReport(mOutSliceArr, lslice, mIthickBP, isirtIter, 
                              mOutScale / recScale, mOutAdd * recScale);
            //
            // Subtract from input slice
            // scale to account for difference between ordinary scaling
            // for output by 1 / numViews and the fact that input slice was
            // descaled by 1/filterScale
            i = ibaseSIRT + mIsliceSizeBP - 1;
            if (mIsignConstraint == 0) {
              for (indv = 0; indv < mIsliceSizeBP; indv++)
                sirtArray[ibaseSIRT + indv - 1] -= mOutSliceArr[indv] / recScale;
            } else if (mIsignConstraint < 0) {
              for (indv = 0; indv < mIsliceSizeBP; indv++)
                sirtArray[ibaseSIRT + indv - 1] = 
                  B3DMIN(0., sirtArray[ibaseSIRT + indv - 1] - mOutSliceArr[indv] /
                         recScale);
            } else {
              for (indv = 0; indv < mIsliceSizeBP; indv++)
                sirtArray[ibaseSIRT + indv - 1] = 
                  B3DMAX(0., sirtArray[ibaseSIRT + indv - 1] - mOutSliceArr[indv] /
                         recScale);
            }
            if (mMaskEdges) 
              maskSlice(&sirtArray[ibaseSIRT - 1], mIthickBP);
            if (isirtIter == mNumSIRTiter && mIfOutSirtRec == 2) {
              printf("writing internal slice %d\n", lslice);
              writeInternalSlice(5, &sirtArray[ibaseSIRT - 1], lslice, nxprj2, dmin4,
                                 dmax4, dmin5, dmax5, nz5);
            }
            if (isirtIter == mNumSIRTiter && mSaveVertSlices && 
                lslice >= mIsliceStart && lslice <= mIsliceEnd) {
              if (mDebug)
                printf("writing vertical slice %d\n", lslice);
              ierr = parWrtSetCurrent(1);
              arrayMinMaxMean(&sirtArray[ibaseSIRT - 1], mIwidth, mIthickBP, 0,
                              mIwidth - 1, 0, mIthickBP - 1, &tmin, &tmax, &tmean);
              dmin6 = B3DMIN(dmin6, tmin);
              dmax6 = B3DMAX(dmax6, tmax);
              parWrtSec(6, (char *)&sirtArray[ibaseSIRT - 1]);
              ierr = parWrtSetCurrent(0);
            }
            //
            // Adjust fill value by change in mean
            // Accumulate mean of starting vertical slices until one output
            // slice has been composed, and set composeFill from mean
            if (isirtIter < mNumSIRTiter || mIfAlpha < 0) {
              arrayMinMaxMean(&sirtArray[ibaseSIRT - 1], mIwidth, mIthickBP, 0,
                              mIwidth - 1, 0, mIthickBP - 1, &unscaledMin, &unscaledMax,
                              &curMean);
              //
              // Doing it the same way with restarts as with going from 0 seems
              // to prevent low frequency artifacts
              // if (sirtFromZero) then
              reprojFill = curMean;
              // else
              // rpfill = dmeanIn + curmean - firstmean
              // endif
              if (isirtIter == mNumSIRTiter && ! composedOne) {
                vertSum = vertSum + curMean;
                numVertSum = numVertSum + 1;
              }
            }
            if (numVertSum > 0)
              composeFill = vertSum / numVertSum;
          }
        }
      }
    }
    //
    if (!mRecReproj) {
      if (mIfAlpha < 0) {
        //
        // interpolate output slice from vertical slices
        //
        iringStart = 1;
        if (numVertSliceInRing == mNumVertNeeded)
          iringStart = nextFreeVertSlice;
        if (numVertSliceInRing > 0) {
          //
          // If there is anything in ring to use, then we can compose an
          // output slice, first dumping any deferred fill slices
          dumpFillSlices(lfillStart, lfillEnd, composeFill, dmin, dmax, dtot8, 
                         numSliceOut, nxyzTmp);
          //PRINT4("composing", lsliceOut, lvertSliceStart, lvertSliceEnd);
          compose(lsliceOut, lvertSliceStart, lvertSliceEnd, 1, iringStart, composeFill);
          composedOne = true;
        } else {
          //
          // But if there is nothing there, keep track of a range of slices
          // that need to be filled - after the composeFill value is set
          if (lfillStart < 0)
            lfillStart = lsliceOut;
          lfillEnd = lsliceOut;
        }
      }
      //
      // Dump slice unless there are fill slices being held
      if (lfillStart < 0)
        dumpUpdateHeader(lsliceOut, dmin, dmax, dtot8, numSliceOut, 
                         nxyzTmp);
      lsliceOut = lsliceOut + mIdelSlice;
    } else {
      //
      // REPROJECT all ready slices to minimize file mangling
      lsliceProjEnd = lastReady;
      reprojectRec(lsliceOut, lsliceProjEnd, inLoadStart, inLoadEnd, dmin, dmax, dtot8);
      lsliceOut = lsliceProjEnd + 1;
    }
  }
  //
  // End of main loop
  //-----------------
  //
  dumpFillSlices(lfillStart, lfillEnd, composeFill, dmin, dmax, dtot8, numSliceOut,
                 nxyzTmp);
  // Close files
  iiuClose(1);
  pixelTot = float(numSlices) * mIwidth * mIthickOut;
  if (mReprojBP || mRecReproj) 
    pixelTot = float(numSlices) * mIwidth * mNumReproj;
  dmean = dtot8 / pixelTot;
  //
  // get numbers used to compute scaling report, and then fix min/max if
  // necessary
  unscaledMin = dmin / mOutScale - mOutAdd;
  unscaledMax = dmax / mOutScale - mOutAdd;
  truncations = false;
  extremes = false;
  if (mNewMode == 0) {
    truncations = dmin < 0. || dmax > 256.;
    dmin = B3DMAX(0., dmin);
    dmax = B3DMIN(255., dmax);
  } else if (mNewMode == 1) {
    extremes = mUseGPU && B3DMAX(B3DABS(dmin), B3DABS(dmax)) > mEffectiveScale * 3.e5;
    truncations = dmin < -32768. || dmax > 32768.;
    dmin = B3DMAX(-32768., dmin);
    dmax = B3DMIN(32767., dmax);
  }
  if (mMinTotSlice <= 0) {
    if (mPerpendicular && mIntervalHeadSave > 0 && !(mReprojBP || mRecReproj)) {
      nxyzTmp[0] = mIwidth;
      nxyzTmp[1] = mIthickOut;
      nxyzTmp[2] = numSlices;
      iiuAltSize(2, nxyzTmp, nxyzst);
    }
    iiuWriteHeader(2, mTitle, 1, dmin, dmax, dmean);
    iiuRetSize(2, nxyzTmp, mxyzTmp, nxyzst);
    iiuPrintHeader(2, (mReprojBP || mRecReproj) ? "\nFinal reprojection file" :
                   "\nFinal reconstruction file");
    if (mSaveVertSlices)
      iiuWriteHeader(6, mTitle, 1, dmin6, dmax6, (dmin6 + dmax6) / 2.);
  } else {
    printf("Min, max, mean, # pixels= %15.7g %15.7g %15.7g %.0f\n", dmin, dmax, dmean, 
           pixelTot);
  }
  if (mParallelHDF) {
    if (iiuParWrtFlushBuffers(2) != 0) 
      exitError("Finishing writing to output HDF file");
    if (mSaveVertSlices) {
      ierr = parWrtSetCurrent(1);
      if (iiuParWrtFlushBuffers(6) != 0) 
        exitError("Finishing writing to vertical slice HDF file");
      ierr = parWrtSetCurrent(0);
    }
    parWrtClose();
  }
  iiuClose(2);
  if (mSaveVertSlices)
    iiuClose(6);
  if (!(mReprojBP || mRecReproj || mNumSIRTiter > 0)) {
    recScale = mNumViews * 235. / (unscaledMax - unscaledMin);
    recAdd = (10. *(unscaledMax - unscaledMin) / 235. -unscaledMin) / mNumViews;
    printf("\n To scale output to bytes (10-245), use SCALE to add %.3f, "
           " and scale by %.5g\n", recAdd, recScale);
    recScale = mNumViews * 30000. / (unscaledMax - unscaledMin);
    recAdd = (-15000. *(unscaledMax - unscaledMin) / 30000. -unscaledMin) / mNumViews;
    printf("\n To scale output to -15000 to 15000, use SCALE to add %.3f, "
           " and scale by %.5g\n", recAdd, recScale);
    printf("\n Reconstruction of %d slices complete.\n", numSlices);
  }
  if (extremes) 
    printf("\nWARNING: Tilt - Extremely large values occurred and values "
           "were truncated when output to file; there could be errors in GPU "
           "computation.  run gputilttest\n");
  if (truncations && ! extremes)
    printf("\n WARNING: Tilt - Some values were truncated when output to the"
           " file; check the output density scaling factor\n");
  if (mUseGPU) 
    gpuDone();
  if (mIterForReport > 0) {
    for (i = 1; i <= B3DMAX(1, mNumSIRTiter); i++) {
      if (mReportVals[2 + 3 * (i - 1)] > 0)
        printf("\nIter %4d, slices %6d %6d, diff rec mean&sd: %15.3f %15.3f\n",
               i + mIterForReport - 1, mIsliceStart, mIsliceEnd, 
               mReportVals[3 * (i - 1)] / mReportVals[2 + 3 * (i - 1)], 
               mReportVals[1 + 3 * (i - 1)] / mReportVals[2 + 3 * (i - 1)]);
    }
  }
  if (mIfOutSirtProj > 0) {
    iiuWriteHeader(4, mTitle, 1, dmin4, dmax4, (dmin4 + dmax4) / 2.);
    iiuClose(4);
  }
  if (mIfOutSirtRec > 0) {
    iiuWriteHeader(5, mTitle, 1, dmin5, dmax5, (dmin5 + dmax5) / 2.);
    iiuClose(5);
  }
  exit(0);
}
//

//
// Determine parameters for loading GPU and make call to shift existing
// data if any is to be retained
//
void Tilt::shiftGpuSetupCopy(int lsliceOut, int needEnd, int &needGpuStart,
                             int &needGpuEnd, int &keepOnGpu, int &numLoadGpu,
                             bool &shiftedGpuLoad)
{
  double timeStart;
  int j, k;
  needGpuStart = mNeededStarts[lsliceOut - mIndNeededBase - 1];
  needGpuEnd = B3DMIN(needGpuStart + mNumGpuPlanes - 1,  needEnd);
  keepOnGpu = 0;
  if (mLoadGpuStart > 0)
    keepOnGpu = mLoadGpuEnd + 1 - needGpuStart;
  numLoadGpu = needGpuEnd + 1 - needGpuStart - keepOnGpu;
  if (mDebug)
    printf("Shifting GPU, # %d  lstart %d  start pos %d\n", numLoadGpu, 
           needGpuStart + keepOnGpu, keepOnGpu + 1);
  timeStart = wallTime();
  j = needGpuStart + keepOnGpu;
  k = keepOnGpu + 1;
  if (gpuShiftProj(numLoadGpu, j, k) == 0) {
    shiftedGpuLoad = true;
  } else {
    mLoadGpuStart = 0;
  }
  if (mDebug)
    printf("Shifting time %.4f\n", wallTime() - timeStart);
}

// For writing two kinds of test output
//
void Tilt::writeInternalSlice(int isUnit, float *array, int lslice, int nxprj2, 
                              float &dmin4, float &dmax4, float &dmin5, float &dmax5,
                              int &nz5)
{
  float istmin, istmax, istmean;
  int iv, j, nxyz[3], nxyzst[3] = {0, 0, 0};
  float cell[6] = {0.,0.,0.,90.,90.,90.};
  if (isUnit == 4) {
    for (iv = 1; iv <= mNumViews; iv++) {
      j = (iv - 1) * nxprj2;
      iiuSetPosition(4, iv - 1, lslice - mIsliceStart);
      iiuWriteLines(4, (char *)&array[j], 1);
    }
    arrayMinMaxMean(array, nxprj2, iv, 0, mIwidth - 1, 0, 
                    mNumViews - 1, &istmin, &istmax, &istmean);
    dmin4 = B3DMIN(dmin4, istmin);
    dmax4 = B3DMAX(dmax4, istmax);
    
  } else {
    iiuWriteSection(5, (char *)array);
    arrayMinMaxMean(array, mIwidth, mIthickBP, 0, mIwidth - 1, 0,
                    mIthickBP - 1, &istmin, &istmax, &istmean);
    dmin5 = B3DMIN(dmin5, istmin);
    dmax5 = B3DMAX(dmax5, istmax);
    nz5 = nz5 + 1;
    cell[0] = nxyz[0] = mIwidth;
    cell[1] = nxyz[1] = mIthickBP;
    cell[2] = nxyz[2] = nz5;
    iiuAltSize(5, nxyz, nxyzst);
    iiuAltSample(5, nxyz);
    iiuAltCell(5, cell);
  }
}

// Dump a slice and update the header periodically if appropriate
//
void Tilt::dumpUpdateHeader(int lsliceDump, float &dmin, float &dmax, double &dtot8,
                            int &numSliceOut, int *nxyzTmp)
{
  int nxyzst[3] = {0, 0, 0};
  float dmean;
  //
  // Write out current slice
  dumpSlice(lsliceDump, dmin, dmax, dtot8);
  // DNM 10/22/03:  Can't use flush in Windows/Intel because of sample.com
  // call flush(6)
  //
  // write out header periodically, restore writing position
  if (mPerpendicular && mIntervalHeadSave > 0 && !mReprojBP && mMinTotSlice <= 0) {
    numSliceOut = numSliceOut + 1;
    nxyzTmp[0] = mIwidth;
    nxyzTmp[1] = mIthickOut;
    nxyzTmp[2] = numSliceOut;
    if ((numSliceOut % mIntervalHeadSave) == 1) {
      iiuAltSize(2, nxyzTmp, nxyzst);
      dmean = dtot8 / ((float)numSliceOut * mIwidth * mIthickOut);
      iiuWriteHeader(2, mTitle, -1, dmin, dmax, dmean);
      parWrtPosn(2, numSliceOut, 0);
    }
  }
}

// If there are fill slices that haven't been output yet, dump them now
// and turn off signal that they exist
void Tilt::dumpFillSlices(int &lfillStart, int lfillEnd, float composeFill, float &dmin,
                          float &dmax, double &dtot8,
                          int &numSliceOut, int *nxyzTmp)
{
  int i, indv;
  if (lfillStart >= 0) {
    for (i = lfillStart; i <= lfillEnd; i += mIdelSlice) {
      for (indv = 0; indv < mIwidth * mIthickOut; indv++)
        mOutSliceArr[indv] = composeFill;
      dumpUpdateHeader(i, dmin, dmax, dtot8, numSliceOut, nxyzTmp);
    }
    lfillStart = -1;
  }
}


// Taper intensities across pad region from end of line to start
// This is not a perfect solution, since it turns a single-pixel deviation at the edge
// of the image into a step, which gets accentuated by the radial filter and leaves a
// little artifact at the edge.  It used to average over 2 pixels, but this is actually
// slightly worse than just using the single pixels at each end.  Mirroring a few pixels
// on each edge before the taper was also worse. In any case, it is much better than
// padding with a constant.
//
void Tilt::taperEndToStart(float *array)
{
  float xsum, startMean, endMean, f;
  int ipad, nsum, ix, numAverage;
  numAverage = 1;
  if (numAverage > 1) {
    nsum = 0;
    xsum = 0.;
    for (ix = 0; ix <= B3DMIN(numAverage, mNxProj - 1); ix++) {
      nsum = nsum + 1;
      xsum = xsum + array[ix];
    }
    startMean = xsum / nsum;
    nsum = 0;
    xsum = 0.;
    for (ix = B3DMAX(0, mNxProj - numAverage - 1); 
         ix < mNxProj; ix++) {
      nsum = nsum + 1;
      xsum = xsum + array[ix];
    }
    endMean = xsum / nsum;
  } else {
    startMean = array[0];
    endMean = array[mNxProj - 1];
  }
  for (ipad = 1; ipad <= mNumPad; ipad++) {
    f = ipad / (mNumPad + 1.);
    array[mNxProj + ipad-1] = f * startMean + (1. -f) * endMean;
  }
}

// Advance in the ring of vertical slices and maintain information about slices in ring
//
void Tilt::manageRing(int numVertNeeded, int &numVertSliceInRing, int &nextFreeVertSlice, 
                      int &lvertSliceStart, int &lvertSliceEnd, int lslice)
{
  if (numVertSliceInRing < numVertNeeded) {
    if (numVertSliceInRing == 0)
      lvertSliceStart = lslice;
    numVertSliceInRing = numVertSliceInRing + 1;
  } else {
    lvertSliceStart = lvertSliceStart + 1;
  }
  lvertSliceEnd = lslice;
  nextFreeVertSlice = nextFreeVertSlice + 1;
  if (nextFreeVertSlice > numVertNeeded)
    nextFreeVertSlice = 1;
}

//
// Set Radial Transform weighting
//
void Tilt::radialWeights(int iradMaxIn, float radFallIn, int ifilterSet, 
                         int ifMultByGaussian)
{
  // Default is linear ramp plus Gaussian fall off
  int nxProjPad, iradEnd, iradMax, iv, iw, indBase, i, iradLimit, jv, imax;
  int irampEnd;
  float stretch, avgInterval, atten, sumInterval, wsum, z, arg, sirtFrac, zmax, freq;
  float diffMin, diff, attenSum, tabFac, sinDiff, fact, exactReachTopMean, fakeAlpha;
  float fakeIterUse, fakeMatchAdd, radFall;
  float *wgtAtten;
  float degToRad = RADIANS_PER_DEGREE;

  //
  wgtAtten = B3DMALLOC(float, mLimView);
  memoryError(wgtAtten, "array for weights per view");
  sirtFrac = 0.99;
  //
  // Empirically determined parameters for fake SIRT.  The alpha value is good for
  // low iteration numbers but then iteration needs to be scaled to match SIRT
  fakeMatchAdd = 0.3;
  fakeAlpha = 0.00195;
  fakeIterUse = mNumFakeSIRTiter;
  if (mNumFakeSIRTiter > 15) 
    fakeIterUse = 15 + 0.8 * (mNumFakeSIRTiter - 15);
  if (mNumFakeSIRTiter > 30)
    fakeIterUse = 27 + 0.6 * (mNumFakeSIRTiter - 30);
  nxProjPad = mNxProj + 2 + mNumPad;
  iradEnd = nxProjPad / 2;
  stretch = (float)(mNxProj + mNumPad) / mNxProj;
  iradMax = B3DNINT(iradMaxIn * stretch);
  radFall = radFallIn * stretch;
  //
  // Compute the ramp to the start of the falloff by default, or all the way out if
  // multiplying by Gaussian
  irampEnd = B3DMIN(iradMax, iradEnd);
  if (ifMultByGaussian > 0)
    irampEnd = iradEnd;
  avgInterval = 1.;
  attenSum = 0.;
  mZeroWeight = 0.;
  exactReachTopMean = 0.;
  if (mNumTiltIncWgt > 0 && mNumWgtAngles > 1) {
    avgInterval = (mWgtAngles[mNumWgtAngles - 1] - mWgtAngles[1 - 1]) / (mNumWgtAngles-1);
    if (mDebug) 
      printf("\n View  Angle Weighting\n");
  }
  //
  // Set up the attenuations for the weighting angles
  for (iv = 1; iv <= mNumWgtAngles; iv++) {
    atten = 1.;
    if (mNumTiltIncWgt > 0. && mNumWgtAngles > 1) {
      sumInterval = 0;
      wsum = 0.;
      for (iw = 1; iw <= mNumTiltIncWgt; iw++) {
        if (iv - iw > 0) {
          wsum = wsum + mTiltIncWgts[iw - 1];
          sumInterval = sumInterval + mTiltIncWgts[iw - 1] * (mWgtAngles[iv - iw] - 
              mWgtAngles[iv - iw - 1]);
        }
        if (iv + iw <= mNumViews) {
          wsum = wsum + mTiltIncWgts[iw - 1];
          sumInterval = sumInterval + mTiltIncWgts[iw - 1] * (mWgtAngles[iv + iw - 1] - 
              mWgtAngles[iv + iw - 2]);
        }
      }
      atten = atten * (sumInterval / wsum) / avgInterval;
    }
    wgtAtten[iv - 1] = atten;
  }
  //
  // Set up linear ramp
  if (mNumExactCycles == 0 || mFlatFrac > 0) {
    for (iv = 1; iv <= mNumViews; iv++) {
      //
      // Get weighting from nearest weighting angle
      atten = 1.;
      if (mNumTiltIncWgt > 0 && mNumWgtAngles > 1) {
        diffMin = 1.e10;
        for (iw = 1; iw <= mNumWgtAngles; iw++) {
          diff = B3DABS(mAngles[iv - 1] - mWgtAngles[iw - 1]);
          if (diff < diffMin) {
            diffMin = diff;
            atten = wgtAtten[iw - 1];
          }
        }
        if (mDebug) 
          printf("%4d %8.2f %10.5f\n", iv, mAngles[iv - 1] / degToRad, atten);
      }
      //
      // Take negative if subtracting
      if (mNumViewSubtract > 0) {
        for (i = 1; i <= mNumViewSubtract; i++) {
          if (mIviewSubtract[i - 1] == 0 || mMapUsedView[iv - 1] == mIviewSubtract[i - 1])
              atten = -atten;
        }
      }
      //
      attenSum = attenSum + atten;
      indBase = (iv - 1 + (ifilterSet - 1) * mNumViews) * nxProjPad;
      for(i = 1; i <= irampEnd; i++) {
        // This was the basic filter
        // ARRAY(ibase+2*I-1) =atten*(I-1)
        // This is the mixture of the basic filter and a flat filter with
        // a scaling that would give approximately the same output magnitude
        freq = (i - 1.) / nxProjPad;
        if (mNumFakeSIRTiter <= 0 || i == 1 || freq < fakeAlpha) {
          mFilterArray[indBase + 2 * i - 2] = atten * ((1. - mFlatFrac) * (i - 1.) + 
              mFlatFrac * mFilterScale);
        } else {
          mFilterArray[indBase + 2 * i - 2] = atten * (i - 1.) *  
            (1. - pow(1.f - fakeAlpha / freq, fakeIterUse + fakeMatchAdd));
        }
        //
        // This 0.2 is what Kak and Slaney's weighting function gives at 0
        if (i == 1)
          mFilterArray[indBase + 2 * i - 2] = atten * 0.2;
        //
        // This is the SIRT filter, which divides the error equally among
        // the pixels on a ray.
        if (mFlatFrac > 1)
          mFilterArray[indBase + 2 * i - 2] = sirtFrac * atten * 
            mFilterScale / (mIthickBP / mCosBeta[iv - 1]);
        //
        // And just the value and compute the mean zero weighting
        mFilterArray[indBase + 2 * i - 1] = mFilterArray[indBase + 2 * i - 2];
        if (i == 1)
          mZeroWeight = mZeroWeight + mFilterArray[indBase + 2 * i - 2] / mNumViews;
      }
    }
    if (mDebug)
      printf("Mean weighting factor %f\n", attenSum / mNumViews);
  } else {
    //
    // Exact filters
    // For each actual view, loop on weighting angles and compute influence factors
    for (i = 0; i < mNumViews * nxProjPad; i++)
      mFilterArray[i] = 0.;
    for (iv = 1; iv <= mNumViews; iv++) {
      indBase = (iv - 1) * nxProjPad;
      for (jv = 1; jv <= mNumWgtAngles; jv++) {
        sinDiff = sin(B3DABS(mWgtAngles[jv - 1] - mAngles[iv - 1]));
        fact = sinDiff * mExactObjSize / (mNxProj + mNumPad);
        iradLimit = mNumExactCycles / B3DMAX(1.e-6, fact);
        tabFac = mExactSamples * fact;

        // mExactTable was indexed from 0 in Fortran so do not subtract 1
        for (i = 1; i <= B3DMIN(iradLimit, irampEnd); i++) {
          mFilterArray[indBase + 2 * i - 1] += mExactTable[B3DNINT((i - 1) * tabFac)];
        }
      }
    }
    //
    // Invert the sum of influence factors
    for (iv = 1; iv <= mNumViews; iv++) {
      indBase = (iv - 1) * nxProjPad;
      zmax = 0.;
      for (i = 1; i <= irampEnd; i++) {
        if (mFilterArray[indBase + 2 * i - 1] == 0.)
          mFilterArray[indBase + 2 * i - 1] = 1.;
        z = iradEnd / mFilterArray[indBase + 2 * i - 1];
        mFilterArray[indBase + 2 * i - 2] = z;
        mFilterArray[indBase + 2 * i - 1] = z;
        if (z > zmax + 0.001) {
          zmax = z;
          imax = i;
        }
        //
        // Limit the zero frequency weighting as above to avoid different mean and max
        if (i == 1) {
          mFilterArray[indBase + 2 * i - 2] = B3DMIN(0.2, z);
          mFilterArray[indBase + 2 * i - 1] = B3DMIN(0.2, z);
          mZeroWeight += mFilterArray[indBase + 2 * i - 2] / mNumViews;
        }
      }
      exactReachTopMean = exactReachTopMean + 0.5 * (imax - 1.) / (iradEnd * mNumViews);
    }
    printf("\nExact filters reach highest point at mean frequency of %6.3f"
           " cycles/pixel\n", exactReachTopMean);
  }
  //
  // Set up Gaussian
  for (i = iradMax + 1; i <= iradEnd; i++) {
    atten = 0.;
    arg = (float)(i - iradMax) / B3DMAX(0.01, radFall);
    if (arg < 8) 
      atten = exp(-arg * arg / 2.);
    //
    // Scale current value if multiplying, or last value if just falling off
    if (ifMultByGaussian > 0) {
      imax = i;
    } else {
      imax = iradMax;
    }
    indBase = 0;
    for (iv = 1; iv <= mNumViews; iv++) {
      z = atten * mFilterArray[indBase + 2 * imax - 2];
      mFilterArray[indBase + 2 * i - 2] = z;
      mFilterArray[indBase + 2 * i - 1] = z;
      indBase = indBase + nxProjPad;
    }
  }
  if (mDebug) {
    // for (iv = 1; iv <= mNumViews; iv++) {
    iv = mNumViews / 2;
    indBase = (iv - 1) * nxProjPad;
    for (i = 1; i <= 19; i++) {
      printf("RF: %4d %4d %11.5f\n", iv, i, mFilterArray[indBase + 2 * i - 2]);
    }
    for (i = 20; i <= iradEnd; i += 10) {
      printf("RF: %4d %4d %11.5f\n", iv, i, mFilterArray[indBase + 2 * i - 2]);
    }
    // }
  }
  free(wgtAtten);
}

//
// Prepare the limits of the slice width to be computed if masking is used
//
void Tilt::maskPrep(int lslice)
{
  //
  float radiusLeft, radiusRight, y, yy, ycenUse;
  int i, ixLeft, ixRight;
  //
  // Compute left and right edges of unmasked area
  if (mMaskEdges) {
    //
    // Adjust the Y center for alpha tilt (already adjusted for ifAlpha < 0)
    ycenUse = mYcenOut;
    if (mIfAlpha > 0)
      ycenUse = mYcenOut + (1. / mCosAlpha[1 - 1] - 1.) * mYOffset 
        - B3DNINT((lslice - mCenterSlice) * mSinAlpha[1 - 1] / mCosAlpha[1 - 1]);
    //
    // Get square of radius of arcs of edge of input data from input center
    radiusLeft = pow(mXcenIn + mAxisXoffset - 1.f, 2.f);
    radiusRight = pow((float)mNxProj - mXcenIn - mAxisXoffset, 2.f);
    for (i = 1; i <= mIthickBP; i++) {
      y = i - ycenUse;
      yy = B3DMIN(y * y, radiusLeft);
      //
      // get distance of X coordinate from center, subtract from or add to
      // center and round up on left, down on right, plus added maskEdges pixels
      ixLeft = mXcenOut + 1. -sqrt(radiusLeft - yy);
      mIxUnmaskedSE[2 * (i - 1)] = B3DMAX(1, ixLeft + mNumExtraMaskPix);
      yy = B3DMIN(y * y, radiusRight);
      ixRight = mXcenOut + sqrt(radiusRight - yy);
      mIxUnmaskedSE[1 + 2 * (i - 1)] = B3DMIN(mIwidth, ixRight - mNumExtraMaskPix);
    }
    //-------------------------------------------------
    // If no maskEdges
  } else {
    for (i = 1; i <= mIthickBP; i++) {
      mIxUnmaskedSE[2 * (i - 1)] = 1;
      mIxUnmaskedSE[1 + 2 * (i - 1)] = mIwidth;
    }
  }
}

//
// This routine applies a one-dimensional Fourier transform to
// all views corresponding to a given slice, applies the radial
// weighting function and then applies an inverse Fourier transform.
//
void Tilt::transform(float *array, int lslice, int ifilterSet)
{
  int nxProjPad, indStart, index, indRad, iv, i, ibFrom, ibaseTo, ixp;
  int ixpP1, ixpM1, ixpP2, expandPad, inIndex, outIndex;
  float x, xp, dx, dxM1, v4, v5, v6, a, c, denNew, dxDxM1, diffMax;
  float fx1, fx2, fx3, fx4, tmpre;
  double tstart;

  nxProjPad = mNxProj + 2 + mNumPad;
  expandPad = mProjSuperFac * (mNxProj + mNumPad) + 2;
  indStart = mIpExtraSize;
  tstart = wallTime();
  index = 1;
  if (mUseGPU) {
    index = gpuFilterLines(&array[indStart], lslice, ifilterSet);
  }
  if (index != 0) {
    //
    // Apply forward Fourier transform
    odfftc(&array[indStart], mNxProj + mNumPad, mNumViews, 0);
    //
    // Apply Radial weighting
    // Do lines from the end backwards in case they are being spread apart for
    // supersampling
    v4 = 0.;
    v5 = 0.;
    for (iv = mNumViews - 1; iv >= 0; iv--) {
      inIndex = indStart + iv * nxProjPad;
      outIndex = indStart + iv * expandPad;
      indRad = ((ifilterSet - 1) * mNumViews + iv) * nxProjPad;
      for (i = 0; i < nxProjPad; i++) {
        v4 = v4 + array[inIndex];
        v5 = v5 + mFilterArray[indRad];
        array[outIndex++] = array[inIndex++] * mFilterArray[indRad++];
      }
      
      // Phase shift and zero-pad if supersampling
      if (mProjSuperFac > 1) {
        outIndex = indStart + iv * expandPad;
        for (i = 0; i < nxProjPad / 2; i++) {
          tmpre = array[outIndex];
          array[outIndex] = mPhaseReal[i] * tmpre - mPhaseImag[i] * array[outIndex + 1];
          array[outIndex + 1] = mPhaseImag[i] * tmpre + mPhaseReal[i] * array[outIndex+1];
          outIndex += 2;
          }
        for (i = nxProjPad; i < expandPad; i++)
          array[outIndex++] = 0.;
      }
    }
    //
    // Apply inverse transform
    odfftc(&array[indStart], mProjSuperFac * (mNxProj + mNumPad), mNumViews, 1);
  }
  if (mDebug)
    printf("Filter time %8.4f\n", wallTime() - tstart);
  if (mIpExtraSize == 0)
    return;
  //
  // do cosine stretch and move down one plane
  // Use cubic interpolation a la cubinterp
  //
  // print *,'istart, ibase', istart, ibase
  for (iv = 0; iv < mNumViews; iv++) {
    ibFrom = indStart + iv * nxProjPad;    // 0 based index
    ibaseTo = mIndStretchLine[iv];         // Should be same as it was
    // print *,nv, ibfrom, ibto, nxStretched(nv)
    diffMax = 0.;
    if (mInterpOrdStretch == 1) {
      //
      // linear interpolation
      //
      for (i = 1; i <= mNxStretched[iv]; i++) {
        x = i / (float)mInterpFacStretch + mStretchOffset[iv];
        xp = B3DMIN(B3DMAX(1., x * mCosBeta[iv]), (float)mNxProj);
        ixp = xp;
        dx = xp - ixp;
        ixp = ixp + ibFrom;
        ixpP1 = B3DMIN(ixp + 1, mNxProj + ibFrom);
        dxM1 = dx - 1.;
        array[ibaseTo + i - 1] = -dxM1 * array[ixp - 1] + dx * array[ixpP1 - 1];
      }
    } else if (mInterpOrdStretch == 2) {
      //
      // quadratic
      //
      for (i = 1; i <= mNxStretched[iv]; i++) {
        x = i / (float)mInterpFacStretch + mStretchOffset[iv];
        xp = B3DMIN(B3DMAX(1., x * mCosBeta[iv]), (float)mNxProj);
        ixp = B3DNINT(xp);
        dx = xp - ixp;
        ixp = ixp + ibFrom;
        ixpP1 = B3DMIN(ixp + 1, mNxProj + ibFrom);
        ixpM1 = B3DMAX(ixp - 1, 1 + ibFrom);
        v4 = array[ixpM1 - 1];
        v5 = array[ixp - 1];
        v6 = array[ixpP1 - 1];
        //
        a = (v6 + v4) * .5 - v5;
        c = (v6 - v4) * .5;
        //
        denNew = a * dx * dx + c * dx + v5;
        // dennew=min(dennew, max(v4, v5, v6))
        // dennew=max(dennew, min(v4, v5, v6))
        array[ibaseTo + i - 1] = denNew;
      }
    } else {
      //
      // cubic
      //
      for (i = 1; i <= mNxStretched[iv]; i++) {
        x = i / (float)mInterpFacStretch + mStretchOffset[iv];
        xp = B3DMIN(B3DMAX(1., x * mCosBeta[iv]), (float)mNxProj);
        ixp = xp;
        dx = xp - ixp;
        ixp = ixp + ibFrom;
        ixpP1 = B3DMIN(ixp + 1, mNxProj + ibFrom);
        ixpM1 = B3DMAX(ixp - 1, 1 + ibFrom);
        ixpP2 = B3DMIN(ixp + 2, mNxProj + ibFrom);

        dxM1 = dx - 1.;
        dxDxM1 = dx * dxM1;
        fx1 = -dxM1 * dxDxM1;
        fx4 = dx * dxDxM1;
        fx2 = 1 + dx * dx * (dx - 2.);
        fx3 = dx * (1. -dxDxM1);
        denNew = fx1 * array[ixpM1 - 1] + fx2 * array[ixp - 1] + 
            fx3 * array[ixpP1 - 1] + fx4 * array[ixpP2 - 1];
        // dennew=min(dennew, max(array(ixpm1), array(ixp), array(ixpp1), &
        // array(ixpp2)))
        // dennew=max(dennew, min(array(ixpm1), array(ixp), array(ixpp1), &
        // array(ixpp2)))
        array[ibaseTo + i - 1] = denNew;
      }
    }
  }
}

//
// This routine assembles one reconstructed slice perpendicular
// to the tilt axis, using a back projection method.
// inputArr is pointer to the full array of lines being used, indStart is a 1-based
// index into it
//
void Tilt::project(float *inputArr, int indStart, int lslice)
{
  int jStart[3], jEnd[3];
  double xproj8, tstart;
  int nxProjPad, iprojDelta, ipoint, iv, index, iz, j;
  float cbeta, sbeta, zz, zPart, yy, yproj, yfrac, oneMyFrac;
  int jProj, jLeft, jRight, iproj, ip1, ip2, ind, iprojBase, ifYtest, nxPad, nyPad;
  int jTestLeft, jTestRight, jregion, maskBase, unmaskStart, unmaskEnd;
  float xLeft, xRight, x, xfrac, oneMxFrac, zBottom, zTop, xproj, yEndTol;
  int superSamp = mSuperSampleFac;
  float *outArr = mOutSliceArr;
  int sliceSize = mIsliceSizeBP;
  int ssWidth = mIwidth * superSamp;
  int ssThick = mIthickBP * superSamp;
  //
  // A note on the ubiquitous ytol: It is needed to keep artifacts from
  // building up at ends of data set through SIRT, from reprojection of the
  // line between real data and fill, or backprojection of an edge in the
  // projection difference.  2.05 was sufficient for X-axis tilt cases but
  // 3.05 was needed for local alignments, thus it is set to 3.05 everywhere
  // (Here, reprojection routines, and in GPU routines)
  yEndTol = 3.05;
  //
  // Determine maskEdges extent if it is variable
  if (mIfAlpha != 0 && mMaskEdges)
    maskPrep(lslice);
  nxProjPad = mProjSuperFac * (mNxProj + mNumPad) + 2;
  tstart = wallTime();
  //
  // GPU backprojection
  if (mUseGPU) {
    ind = 1;
    x = mXcenIn + mAxisXoffset;
    if (mIfAlpha <= 0 && mNxWarp == 0) {
      ind = gpuBpNoX(mOutSliceArr, &inputArr[indStart - 1], mSinBeta, mCosBeta,
                     mNxProj, x, mXcenOut, mYcenOut, mEdgeFill);
    } else if (mNxWarp == 0 && mLoadGpuStart > 0) {
      ind = gpuBpXtilt(mOutSliceArr, mSinBeta, mCosBeta, mSinAlpha, 
                       mCosAlpha, mXZfac, mYZfac, mNxProj, mNyProj, x, mXcenOut, 
                       mYcenOut, lslice, mCenterSlice, mEdgeFill);
    } else if (mLoadGpuStart > 0) {
      ind = gpuBpLocal(mOutSliceArr, lslice, mNxWarp, mNyWarp,
                       mIxStartWarp, mIyStartWarp, mIdelXwarp, mIdelYwarp, mNxProj,
                       mXcenOut, mXcenIn, mAxisXoffset, mYcenOut, mCenterSlice,
                       mEdgeFill);
    }
    if (ind == 0) {
      if (mDebug)
        printf("GPU backprojection time %9.5f\n", wallTime() - tstart);
      return;
    }
  }

  if (superSamp > 1) {
    sliceSize *= superSamp * superSamp;
    outArr = mSuperOutArr;
  }
  //
  // CPU backprojection: clear out the slice
  for (iz = 0; iz < sliceSize; iz++) {
    outArr[iz] = 0.;
  }
  iprojDelta = mIdelSlice * mInPlaneSize;
  //
  if (mNxWarp == 0) {
    //
    // Loop over all views
    ipoint = indStart - 1;
    for (iv = 1; iv <= mNumViews; iv++) {
      //
      // Set view angle
      cbeta = mCosBeta[iv - 1];
      sbeta = mSinBeta[iv - 1];
      //
      // Loop over all points in output slice
      index = 1;
      //
      for (iz = 1; iz <= ssThick; iz++) {
        zz = ((iz - 0.5) / superSamp + 0.5 - mYcenOut) * mCompress[iv - 1];
        if (mIfAlpha <= 0) {
          zPart = zz * sbeta + mXcenIn + mAxisXoffset;
        } else {
          //
          // If x-axis tilting, find interpolation factor between the
          // slices
          //
          yy = lslice - mCenterSlice;
          zPart = yy * mSinAlpha[iv - 1] * sbeta + 
            zz * (mCosAlpha[iv - 1] * sbeta + mXZfac[iv - 1]) + mXcenIn + mAxisXoffset;
          yproj = yy * mCosAlpha[iv - 1] - 
            zz * (mSinAlpha[iv - 1] - mYZfac[iv - 1]) + mCenterSlice;
          //
          // if inside the tolerance, clamp it to the endpoints
          if (yproj >= 1. - yEndTol && yproj <= mNyProj + yEndTol)
            yproj = B3DMAX(1., B3DMIN((float)mNyProj, yproj));
          jProj = yproj;
          jProj = B3DMIN(mNyProj - 1, jProj);
          yfrac = yproj - jProj;
          oneMyFrac = 1. -yfrac;
        }
        //
        // compute left and right limits that come from legal data
        //
        x = cbeta;
        if (B3DABS(cbeta) < 0.001)
          x = B3DSIGN(0.001, cbeta);
        xLeft = (1. -zPart) / x + mXcenOut;
        xRight = (mNxProj - zPart) / x + mXcenOut;
        if (xRight < xLeft) {
          x = xLeft;
          xLeft = xRight;
          xRight = x;
        }
        maskBase = 2 * (1 + (iz - 1) / superSamp);
        jLeft = superSamp * xLeft;
        if (jLeft < superSamp * xLeft)
          jLeft += 1;
        jLeft = B3DMAX(jLeft, superSamp * (mIxUnmaskedSE[maskBase - 2] - 1) + 1);
        jRight = superSamp * xRight;
        if (jRight == superSamp * xRight)
          jRight -= 1;
        jRight = B3DMIN(jRight, superSamp * mIxUnmaskedSE[maskBase - 1]);
        //
        // If the limits are now crossed, just skip to full fill at end
        if (jLeft <= jRight) {
          //
          // set up starting projection position and index
          x = (jLeft - 0.5) / superSamp + 0.5 - mXcenOut;
          for (ind = index + superSamp * (mIxUnmaskedSE[maskBase - 2] - 1); 
               ind <= index + jLeft - 2; ind++) {
            outArr[ind - 1] += mEdgeFill;
          }
          index = index + (jLeft - 1);
          if (mInterpFacStretch != 0) {
            //
            // Computation with prestretched data
            //
            xproj8 = mInterpFacStretch * (zPart / cbeta + x - mStretchOffset[iv - 1]);
            B3DCLAMP(xproj8, 1., mNxStretched[iv - 1] - 0.001);
            iproj = xproj8;
            xfrac = xproj8 - iproj;
            iproj = iproj + ipoint + mIndStretchLine[iv - 1];
            oneMxFrac = 1. -xfrac;
            if (mIfAlpha <= 0) {
              //
              // interpolation in simple case of no x-axis tilt
              //
              for (ind = index; ind <= index + jRight - jLeft; ind++) {
                mOutSliceArr[ind - 1] += oneMxFrac * inputArr[iproj - 1] + 
                  xfrac * inputArr[iproj];
                iproj = iproj + mInterpFacStretch;
              }
              index = index + jRight + 1 - jLeft;
            } else {
              //
              // If x-axis tilting, interpolate from two lines
              //
              ip1 = iproj + (jProj - lslice) * iprojDelta;
              ip2 = ip1 + iprojDelta;
              if (yproj >= 1. && yproj <= mNyProj && 
                  ip1 >= 1 && ip2 >= 1 && ip1 < mIndLoadEnd && ip2 < mIndLoadEnd) {

                for (ind = index; ind <= index + jRight - jLeft; ind++) {
                  mOutSliceArr[ind - 1] += oneMxFrac * 
                    (oneMyFrac * inputArr[ip1 - 1] + yfrac * inputArr[ip2 - 1]) + 
                    xfrac * (oneMyFrac * inputArr[ip1] + yfrac * inputArr[ip2]);
                  ip1 = ip1 + mInterpFacStretch;
                  ip2 = ip2 + mInterpFacStretch;
                }
              } else {
                for (ind = index; ind <= index + jRight + 1 - jLeft - 1; ind++) {
                  mOutSliceArr[ind - 1] += mEdgeFill;
                }
              }
              index = index + jRight + 1 - jLeft;
            }
          } else {
            //
            // Computation direct from projection data
            //
            xproj8 = mProjSuperFac * (zPart + x * cbeta - 0.5) + 0.5;
            B3DCLAMP(xproj8, 1., mNxProj - 0.001);
            if (mIfAlpha <= 0) {
              //
              // interpolation in simple case of no x-axis tilt
              //
              bpSumNoX(outArr, index, inputArr, ipoint, jRight + 1 - jLeft, xproj8,
                       mProjSuperFac * cbeta / superSamp);
            } else {
              //
              // If x-axis tilting
              //
              iproj = xproj8;
              iprojBase = ipoint + (jProj - lslice) * iprojDelta;
              ip1 = iprojBase + iproj;
              ip2 = ip1 + iprojDelta;
              if (yproj >= 1. && yproj <= mNyProj && 
                  ip1 >= 1 && ip2 >= 1 && ip1 < mIndLoadEnd && ip2 < mIndLoadEnd) {

                bpSumXtilt(outArr, index, inputArr, iprojBase, iprojDelta, 
                           jRight + 1 - jLeft, xproj8, mProjSuperFac * cbeta / superSamp,
                           yfrac, oneMyFrac);
              } else {
                for (ind = index; ind <= index + jRight + 1 - jLeft - 1; ind++) {
                  outArr[ind - 1] += mEdgeFill;
                }
                index = index + jRight + 1 - jLeft;
              }
            }
          }
        } else {
          jRight = 0;
        }
        for (ind = index; ind <= index + ssWidth - jRight - 1; ind++) {
          outArr[ind - 1] += mEdgeFill;
        }
        index = index + ssWidth - jRight;
      }
      //
      //-------------------------------------------
      //
      // End of projection loop
      if (mInterpFacStretch == 0)
        ipoint = ipoint + nxProjPad;
    }
  } else {
    //
    // LOCAL ALIGNMENTS
    //
    // Loop over all views
    ipoint = indStart - 1;
    for (iv = 1; iv <= mNumViews; iv++) {
      //
      // precompute the factors for getting xproj and yproj all the
      // way across the slice
      //
      ifYtest = 0;
      zBottom = (1 - mYcenOut) * mCompress[iv - 1];
      zTop = (mIthickBP - mYcenOut) * mCompress[iv - 1];
      for (j = 1; j <= ssWidth; j++) {
        //
        // get the fixed and z-dependent component of the
        // projection coordinates

        localProjFactors((j - 0.5) / superSamp + 0.5, lslice, iv, mXprojFs[j - 1], 
                         mXprojZs[j - 1], mYprojFs[j - 1], mYprojZs[j - 1]);
        //
        // see if any y testing is needed in the inner loop by checking
        // yproj at top and bottom in Z
        //
        yproj = mYprojFs[j - 1] + mYprojZs[j - 1] * zBottom;
        jProj = yproj;
        ip1 = ipoint + (jProj - lslice) * iprojDelta + 1;
        ip2 = ip1 + iprojDelta;
        if (ip1 <= 1 || ip2 <= 1 || ip1 >= mIndLoadEnd 
            || ip2 >= mIndLoadEnd || jProj < 1 || jProj >= mNyProj) 
            ifYtest = 1;
        yproj = mYprojFs[j - 1] + mYprojZs[j - 1] * zTop;
        jProj = yproj;
        ip1 = ipoint + (jProj - lslice) * iprojDelta + 1;
        ip2 = ip1 + iprojDelta;
        if (ip1 <= 1 || ip2 <= 1 || ip1 >= mIndLoadEnd 
            || ip2 >= mIndLoadEnd || jProj < 1 || jProj >= mNyProj) 
          ifYtest = 1;
      }
      //
      // walk in from each end until xproj is safely within bounds
      // to define region where no x checking is needed
      //
      jTestLeft = 0;
      j = 1;
      while (jTestLeft == 0 && j < ssWidth) {
        if (B3DMIN(mXprojFs[j - 1] + zBottom * mXprojZs[j - 1], 
            mXprojFs[j - 1] + zTop * mXprojZs[j - 1]) >= 1)
          jTestLeft = j;
        j = j + 1;
      }
      if (jTestLeft == 0)
        jTestLeft = ssWidth;
      //
      jTestRight = 0;
      j = ssWidth;
      while (jTestRight == 0 && j > 1) {
        if (B3DMAX(mXprojFs[j - 1] + zBottom * mXprojZs[j - 1], 
            mXprojFs[j - 1] + zTop * mXprojZs[j - 1]) < mNxProj) 
          jTestRight = j;
        j = j - 1;
      }
      if (jTestRight == 0)
        jTestRight = 1;
      if (jTestRight < jTestLeft) {
        jTestRight = ssWidth / 2;
        jTestLeft = jTestRight + 1;
      }
      //
      index = 1;
      //
      // loop over the slice, outer loop on z levels
      //
      for (iz = 1; iz <= ssThick; iz++) {
        zz = ((iz - 0.5) / superSamp + 0.5 - mYcenOut) * mCompress[iv - 1];
        maskBase = 2 * (1 + (iz - 1) / superSamp);
        unmaskStart = superSamp * (mIxUnmaskedSE[maskBase - 2] - 1) + 1;
        unmaskEnd = superSamp * mIxUnmaskedSE[maskBase - 1];
        jLeft = B3DMAX(jTestLeft, unmaskStart);
        jRight = B3DMIN(jTestRight, unmaskEnd);
        index = index + unmaskStart - 1;
        //
        // set up to do inner loop in three regions of X
        //
        jStart[0] = unmaskStart;
        jEnd[0] = jLeft - 1;
        jStart[1] = jLeft;
        jEnd[1] = jRight;
        jStart[2] = jRight + 1;
        jEnd[2] = unmaskEnd;
        for (jregion = 1; jregion <= 3; jregion++) {
          if (jregion != 2 || ifYtest == 1) {
            //
            // loop involving full testing - either left or right
            // sides needing x testing, or anywhere if y testing
            // needed
            //
            for (j = jStart[jregion - 1]; j <= jEnd[jregion - 1]; j++) {
              xproj = mProjSuperFac * (mXprojFs[j - 1] + zz * mXprojZs[j - 1] - 0.5) +0.5;
              yproj = mYprojFs[j - 1] + zz * mYprojZs[j - 1];
              if (yproj >= 1. - yEndTol && yproj <= mNyProj + yEndTol)
                yproj = B3DMAX(1., B3DMIN((float)mNyProj, yproj));
              if (xproj >= 1 && xproj <= mNxProj && yproj >= 1. && yproj <= mNyProj) {
                //
                iproj = xproj;
                iproj = B3DMIN(mNxProj - 1, iproj);
                xfrac = xproj - iproj;
                jProj = yproj;
                jProj = B3DMIN(mNyProj - 1, jProj);
                yfrac = yproj - jProj;
                //
                ip1 = ipoint + (jProj - lslice) * iprojDelta + iproj;
                ip2 = ip1 + iprojDelta;
                if (ip1 >= 1 && ip2 >= 1 && 
                    ip1 < mIndLoadEnd && ip2 < mIndLoadEnd) {
                  outArr[index - 1] += 
                    (1. -yfrac) * ((1. -xfrac) * inputArr[ip1 - 1] 
                                   + xfrac * inputArr[ip1 + 1 - 1]) + 
                    yfrac * ((1. -xfrac) * inputArr[ip2 - 1] 
                             + xfrac * inputArr[ip2 + 1 - 1]);
                } else {
                  outArr[index - 1] += mEdgeFill;
                }
              } else {
                outArr[index - 1] += mEdgeFill;
              }
              index = index + 1;
            }
            //
            // loop for no x-testing and no y testing
            //
          } else {
            bpSumLocal(outArr, index, inputArr, zz, mXprojFs, mXprojZs, mYprojFs, 
                       mYprojZs, ipoint, iprojDelta, lslice, jStart[jregion - 1], 
                       jEnd[jregion - 1]);
          }
        }
        index = index + ssWidth - unmaskEnd;
      }
      //-------------------------------------------
      //
      // End of projection loop
      ipoint = ipoint + nxProjPad;
    }
  }
  if (superSamp > 1) {
    nxPad = superSamp * mNxOutPad;
    nyPad = superSamp * mNyOutPad;
    sliceTaperOutPad(outArr, SLICE_MODE_FLOAT, ssWidth, ssThick, outArr, 
                     nxPad + 2, nxPad, nyPad, 0, 0.);
    todfftc(outArr, nxPad, nyPad, 0);
    fourierReduceImage(outArr, nxPad, nyPad, mOutSliceArr, mNxOutPad, mNyOutPad, 0., 0.,
                       mSuperTempArr);
    cleanUpReducedFFT(mBetaMin, mBetaMax);
    todfftc(mOutSliceArr, mNxOutPad, mNyOutPad, 1);
    jLeft = (mNxOutPad - mIwidth) / 2;
    jRight = (mNyOutPad - mIthickBP) / 2;
    if (extractWithBinning(mOutSliceArr, SLICE_MODE_FLOAT, mNxOutPad + 2, jLeft, 
                           jLeft + mIwidth - 1, jRight, jRight + mIthickBP - 1, 1,
                           mOutSliceArr, 0, &ind, &index))
                       exitError("Reducing super-sampled slice");
  }
  if (mDebug) 
    printf("CPU backprojection time %9.5f\n", wallTime() - tstart);
}

// Function to remove corner pixels or missing wedge pixels from the FFT
// Missing wedge seemed problem and is not fully implemented, i.e, the limiting angles
// are not adjusted for local alignments
//
void Tilt::cleanUpReducedFFT(float betaMin, float betaMax)
{
  float tiltInc, angleLim, slopeMax, pixAngle, sinMax, cosMax, slopeMin, sinMin, cosMin;
  float x, y, delx, dely, ysq, distSq, atten, ratio, useAlpha;
  int ind, index, ix, iy, nxDiv2p1;
  float pixelMargin = 4.;
  float alphaMargin = 1.1;
  float cornerCutoff = 0.2352f;
  float cornerFalloff = 0.03f;
  if (mCleanSuperFFT <= 0)
    return;
  bool cleanCorners = (mCleanSuperFFT & 1) != 0;
  bool cleanWedge = (mCleanSuperFFT & 2) != 0;
  //mrcWriteFFT("fft-1.mrc", mOutSliceArr, mNxOutPad, mNyOutPad, 1);
  delx = 1. / mNxOutPad;
  dely = 1. / mNyOutPad;
  nxDiv2p1 = mNxOutPad / 2 + 1;

  // If cleaning the wedge, determine slope criteria and sin/cos for rotating to 
  // horizontal in pixel coordinates for testing the pixel distance
  if (cleanWedge) {
    angleLim = 89.5 * RADIANS_PER_DEGREE;
    tiltInc = (betaMax - betaMin) / B3DMAX(1, mNumViews - 1);
    if (betaMax + tiltInc / 2. > angleLim || betaMin - tiltInc / 2. < -angleLim) {
      cleanWedge = false;
      if (!cleanCorners)
        return;
    } else {
      useAlpha = mMinCosAlpha;
      if (useAlpha < 1)
        useAlpha = (1. - (1. - mMinCosAlpha) * alphaMargin);
      slopeMax = tan(betaMax + tiltInc / 2.) / useAlpha;
      pixAngle = atan(slopeMax * delx / dely);
      sinMax = sin(pixAngle);
      cosMax = cos(pixAngle);
      //PRINT4(slopeMax, pixAngle, sinMax, cosMax);
      slopeMin = tan(betaMin - tiltInc / 2.) / useAlpha;
      pixAngle = atan(slopeMin * delx / dely);
      sinMin = sin(pixAngle);
      cosMin = cos(pixAngle);
      //PRINT4(slopeMin, pixAngle, sinMin, cosMin);
    }
  }
  
  //int numCorn =0, numAxis = 0, numAbove =0, numBelow = 0;
  for (iy = 0; iy < mNyOutPad; iy++) {
    y = iy * dely;
    index = iy * nxDiv2p1;
    if (y > 0.5)
      y = y - 1.0f;
    ysq = y * y;
    for (ix = 0; ix < nxDiv2p1; ix++) {
      x = ix * delx;
      ind = 2 * (index + ix);
      distSq = x * x + ysq;

      // A hard edge is worrisome, so taper it in case there is data out there
      if (cleanCorners && distSq > cornerCutoff) {
        if (distSq < cornerCutoff  + cornerFalloff) {
          atten = (cornerCutoff  + cornerFalloff - distSq) / cornerFalloff;
          mOutSliceArr[ind] *= atten;
          mOutSliceArr[ind + 1] *= atten;
        } else {
          mOutSliceArr[ind] = 0.;
          mOutSliceArr[ind + 1] = 0.;
        }
        //numCorn++;
      } else if (cleanWedge) {
        if (ix) {
          ratio = y / x;
          if (ratio > slopeMax && -sinMax * ix + cosMax * y / dely > pixelMargin) {
              mOutSliceArr[ind] = 0.;
              mOutSliceArr[ind + 1] = 0.;
              //  numAbove++;
          } else if (ratio < slopeMin && -sinMin * ix + cosMin * y / dely < -pixelMargin){
            mOutSliceArr[ind] = 0.;
            mOutSliceArr[ind + 1] = 0.;
            //  numBelow++;
          }
        } else if (y / dely < pixelMargin || y / dely > pixelMargin) {
          mOutSliceArr[ind] = 0.;
          mOutSliceArr[ind + 1] = 0.;
          //numAxis++;
        }        
      }
    }
  }
  //PRINT4(numCorn, numAxis , numAbove , numBelow);
  //mrcWriteFFT("fft-2.mrc", mOutSliceArr, mNxOutPad, mNyOutPad, 1);
}

//
// COMPOSE will interpolate the output slice LSLICEOUT from vertical
// slices in the ring buffer, where lvertSliceStart and lVertSliceEnd are the starting
// and ending slices in the ring buffer, IDIR is the direction of
// reconstruction, and IRINGSTART is the position of lvertSliceStart in the
// ring buffer.
//
void Tilt::compose(int lsliceOut, int lvertSliceStart, int lVertSliceEnd, int idir, 
                   int iringStart, float composeFill)
{
  int ind1[4], ind2[4], ind3[4], ind4[4];
  float tanAlpha, vertCen, centeredJ, centeredL, vSlice, vyCentered, fx, vy, fy, f22, f23;
  float f32, f33, fxsq, fysq, fxcub, fycub;
  int ivSlice, ifMiss, i, lvSlice, iring, ibaseInd, ivy, indCen, jnd5, jnd2, j, k;
  float fx1, fx2, fx3, fx4, fy1, fy2, fy3, fy4, v1, v2, v3, v4, f5, f2, f8, f4, f6;
  int jnd8, jnd4, jnd6, numFill;
  //
  // 12/12/09: stopped reading base here, read on output; eliminate zeroing
  //
  tanAlpha = mSinAlpha[0] / mCosAlpha[0];
  vertCen = mIthickBP / 2 + 0.5;
  fx = 0.;
  fy = 0.;
  numFill = 0;
  //
  // loop on lines of data
  //
  for (j = 1; j <= mIthickOut; j++) {
    centeredJ = j - (mIthickOut / 2 + 0.5) - mYOffset;
    centeredL = lsliceOut - mCenterSlice;
    //
    // calculate slice number and y position in vertical slices
    //
    vSlice = centeredL * mCosAlpha[0] - centeredJ * mSinAlpha[0] + mCenterSlice;
    vyCentered = centeredL * mSinAlpha[0] + centeredJ * mCosAlpha[0];
    ivSlice = vSlice;
    fx = vSlice - ivSlice;
    ifMiss = 0;
    //
    // for each of 4 slices needed for cubic interpolation, initialize
    // data indexes at zero then see if slice exists in ring
    //
    for (i = 1; i <= 4; i++) {
      ind1[i - 1] = 0;
      ind2[i - 1] = 0;
      ind3[i - 1] = 0;
      ind4[i - 1] = 0;
      lvSlice = ivSlice + i - 2;
      if (idir * (lvSlice - lvertSliceStart) >= 0 &&
          idir * (lVertSliceEnd - lvSlice) >= 0) {
        //
        // if slice exists, get base index for the slice, compute the
        // y index in the slice, then set the 4 data indexes if they
        // are within the slice
        //
        iring = idir * (lvSlice - lvertSliceStart) + iringStart;
        if (iring > mNumVertNeeded)
          iring = iring - mNumVertNeeded;
        ibaseInd = 1 + (iring - 1) * mIthickBP * mIwidth;
        vy = vyCentered + vertCen - B3DNINT(tanAlpha * (lvSlice - mCenterSlice)) +  
            mYOffset / mCosAlpha[0];
        ivy = vy;
        fy = vy - ivy;
        if (ivy - 1 >= 1 && ivy - 1 <= mIthickBP)
            ind1[i - 1] = ibaseInd + mIwidth * (ivy - 2);
        if (ivy >= 1 && ivy <= mIthickBP)
          ind2[i - 1] = ibaseInd + mIwidth * (ivy - 1);
        if (ivy + 1 >= 1 && ivy + 1 <= mIthickBP) 
          ind3[i - 1] = ibaseInd + mIwidth * ivy;
        if (ivy + 2 >= 1 && ivy + 2 <= mIthickBP)
            ind4[i - 1] = ibaseInd + mIwidth * (ivy + 1);
      }
      if (ind1[i - 1] == 0 || ind2[i - 1] == 0 || ind3[i - 1] == 0 || 
          ind4[i - 1] == 0)
        ifMiss = 1;
    }
    ibaseInd = (j - 1) * mIwidth;
    if (mInterpOrdXtilt > 2 && ifMiss == 0) {
      //
      // cubic interpolation if selected, and no data missing
      //
      fxsq = fx * fx;
      fxcub = fxsq * fx;
      fysq = fy * fy;
      fycub = fysq * fy;
      fx1 = 2. *fxsq - fxcub - fx;
      fx2 = fxcub - 2. *fxsq + 1;
      fx3 = fxsq + fx - fxcub;
      fx4 = fxcub - fxsq;
      fy1 = 2. *fysq - fycub - fy;
      fy2 = fycub - 2. *fysq + 1;
      fy3 = fysq + fy - fycub;
      fy4 = fycub - fysq;
      for (i = 1; i <= mIwidth; i++) {
        v1 = fx1 * mVertSliceArr[ind1[0] - 1] + fx2 * mVertSliceArr[ind1[1] - 1] + 
            fx3 * mVertSliceArr[ind1[2] - 1] + fx4 * mVertSliceArr[ind1[3] - 1];
        v2 = fx1 * mVertSliceArr[ind2[0] - 1] + fx2 * mVertSliceArr[ind2[1] - 1] + 
            fx3 * mVertSliceArr[ind2[2] - 1] + fx4 * mVertSliceArr[ind2[3] - 1];
        v3 = fx1 * mVertSliceArr[ind3[0] - 1] + fx2 * mVertSliceArr[ind3[1] - 1] + 
            fx3 * mVertSliceArr[ind3[2] - 1] + fx4 * mVertSliceArr[ind3[3] - 1];
        v4 = fx1 * mVertSliceArr[ind4[0] - 1] + fx2 * mVertSliceArr[ind4[1] - 1] + 
            fx3 * mVertSliceArr[ind4[2] - 1] + fx4 * mVertSliceArr[ind4[3] - 1];
        mOutSliceArr[ibaseInd + i - 1] = fy1 * v1 + fy2 * v2 + fy3 * v3 + fy4 * v4;
        for (k = 1; k <= 4; k++) {
          ind1[k - 1] = ind1[k - 1] + 1;
          ind2[k - 1] = ind2[k - 1] + 1;
          ind3[k - 1] = ind3[k - 1] + 1;
          ind4[k - 1] = ind4[k - 1] + 1;
        }
      }
    } else if (mInterpOrdXtilt == 2 && ifMiss == 0) {
      //
      // quadratic interpolation if selected, and no data missing
      // shift to next column or row if fractions > 0.5
      //
      indCen = 2;
      if (fx > 0.5) {
        indCen = 3;
        fx = fx - 1.;
      }
      if (fy <= 0.5) {
        jnd5 = ind2[indCen - 1];
        jnd2 = ind1[indCen - 1];
        jnd8 = ind3[indCen - 1];
        jnd4 = ind2[indCen - 2];
        jnd6 = ind2[indCen];
      } else {
        fy = fy - 1.;
        jnd5 = ind3[indCen - 1];
        jnd2 = ind2[indCen - 1];
        jnd8 = ind4[indCen - 1];
        jnd4 = ind3[indCen - 2];
        jnd6 = ind3[indCen];
      }
      //
      // get coefficients and do the interpolation
      //
      fxsq = fx * fx;
      fysq = fy * fy;
      f5 = 1. -fxsq - fysq;
      f2 = (fysq - fy) / 2.;
      f8 = f2 + fy;
      f4 = (fxsq - fx) / 2.;
      f6 = f4 + fx;
      for (i = 1; i <= mIwidth; i++) {
        mOutSliceArr[ibaseInd + i - 1] = f5 * mVertSliceArr[jnd5 - 1] + 
          f2 * mVertSliceArr[jnd2 - 1] + f4 * mVertSliceArr[jnd4 - 1] + 
          f6 * mVertSliceArr[jnd6 - 1] + f8 * mVertSliceArr[jnd8 - 1];
        jnd5 = jnd5 + 1;
        jnd2 = jnd2 + 1;
        jnd4 = jnd4 + 1;
        jnd6 = jnd6 + 1;
        jnd8 = jnd8 + 1;
      }
    } else {
      //
      // linear interpolation
      //
      // print *,j, ind2[1], ind2[2], ind3[1], ind3[2]
      if (ind2[1] == 0 || ind2[2] == 0 || ind3[1] == 0 || ind3[2] == 0) {
        //
        // if there is a problem, see if it can be rescued by shifting
        // center back to left or below
        //
        if (fx < 0.02 && ind2[0] != 0 && ind3[0] != 0 && 
            ind2[1] != 0 && ind3[1] != 0) {
          fx = fx + 1;
          ind2[2] = ind2[1];
          ind2[1] = ind2[0];
          ind3[2] = ind3[1];
          ind3[1] = ind3[0];
        } else if (fy < 0.02 && ind1[1] != 0 && ind1[2] != 0 && 
            ind2[1] != 0 && ind3[1] != 0) {
          fy = fy + 1;
          ind3[1] = ind2[1];
          ind2[1] = ind1[1];
          ind3[2] = ind2[2];
          ind2[2] = ind1[2];
        }
      }
      //
      // do linear interpolation if conditions are right, otherwise fill
      //
      if (ind2[1] != 0 && ind2[2] != 0 && ind3[1] != 0 && 
          ind3[2] != 0) {
        f22 = (1. -fy) * (1. -fx);
        f23 = (1. -fy) * fx;
        f32 = fy * (1. -fx);
        f33 = fy * fx;
        for (i = 1; i <= mIwidth; i++) {
          mOutSliceArr[ibaseInd + i - 1] = f22 * mVertSliceArr[ind2[1] - 1] + 
            f23 * mVertSliceArr[ind2[2] - 1] + f32 * mVertSliceArr[ind3[1] - 1] +
            f33 * mVertSliceArr[ind3[2] - 1];
          ind2[1] = ind2[1] + 1;
          ind2[2] = ind2[2] + 1;
          ind3[1] = ind3[1] + 1;
          ind3[2] = ind3[2] + 1;
        }
      } else {
        // print *,'filling', j
        for (i = 1; i <= mIwidth; i++) {
          mOutSliceArr[i + ibaseInd - 1] = composeFill;
        }
        numFill = numFill + 1;
      }
    }
  }
  // if (nfill .ne. 0) print *,nfill, ' lines filled, edgefill =', composeFill
  return;
}

//
// DECOMPOSE will interpolate a vertical slice LSLICE from input slices
// in the read-in ring buffer, where lReadStart and lReadEnd are the
// starting and ending slices in the ring buffer, IRINGSTART is the
// position of lReadStart in the ring buffer, and ibaseSIRT is the index
// in stack at which to place the slice.
//
void Tilt::decompose(int lslice, int lReadStart, int lreadEnd, int iringStart,
                     float *vertArr)
{
  float tanAlpha, outCen, centeredL, vSliceCentered, vyCentered, outSlice, outYpos;
  float f11, f12, f21, f22, fx, fy;
  int ibaseVert, ibase1, ibase2, j, ioutSlice, jOut, i, iring;

  tanAlpha = mSinAlpha[0] / mCosAlpha[0];
  outCen = mIthickOut / 2 + 0.5;
  centeredL = lslice - mCenterSlice;
  vSliceCentered = centeredL;
  //
  // loop on lines of data
  for (j = 1; j <= mIthickBP; j++) {
    ibaseVert = (j - 1) * mIwidth;
    //
    // calculate slice number and y position in input slices
    //
    vyCentered = j - (mIthickBP / 2 + 0.5 - B3DNINT(tanAlpha * centeredL) +  
        mYOffset / mCosAlpha[0]);
    outSlice = mCenterSlice + vSliceCentered * mCosAlpha[0] + vyCentered * mSinAlpha[0];
    outYpos = outCen + mYOffset - vSliceCentered * mSinAlpha[0] + 
        vyCentered * mCosAlpha[0];
    // print *,j, vycen, outsl, outj
    // if (outsl >= lreadStart - 0.5 .and. outsl <= lreadEnd + 0.5 &
    // .and. outj >= 0.5 .and.outj <= ithickOut + 0.5) then
    //
    // For a legal position, get interpolation integers and fractions,
    // adjust if within half pixel of end
    ioutSlice = outSlice;
    fx = outSlice - ioutSlice;
    if (ioutSlice < lReadStart) {
      ioutSlice = lReadStart;
      fx = 0.;
    } else if (ioutSlice > lreadEnd - 1) {
      ioutSlice = lreadEnd - 1;
      fx = 1.;
    }
    jOut = outYpos;
    fy = outYpos - jOut;
    if (jOut < 1) {
      jOut = 1;
      fy = 0.;
    } else if (jOut > mIthickOut - 1) {
      jOut = mIthickOut - 1;
      fy = 1.;
    }
    //
    // Get slice indexes in ring
    iring = ioutSlice - lReadStart + iringStart;
    if (iring > mNumReadNeed)
      iring = iring - mNumReadNeed;
    ibase1 = (iring - 1) * mIthickOut * mIwidth + (jOut - 1) * mIwidth;
    iring = ioutSlice + 1 - lReadStart + iringStart;
    if (iring > mNumReadNeed)
      iring = iring - mNumReadNeed;
    ibase2 = (iring - 1) * mIthickOut * mIwidth + (jOut - 1) * mIwidth;
    //
    // Interpolate line
    f11 = (1. -fy) * (1. -fx);
    f12 = (1. -fy) * fx;
    f21 = fy * (1. -fx);
    f22 = fy * fx;
    for (i = 0; i < mIwidth; i++) {
      vertArr[ibaseVert + i] = f11 * mReadInArray[ibase1 + i] + 
        f12 * mReadInArray[ibase2 + i] + f21 * mReadInArray[ibase1 + i + mIwidth]
        + f22 * mReadInArray[ibase2 + i + mIwidth];
    }
    // else
    //
    // Otherwise fill line
    // do i = 0, iwidth - 1
    // array(ibasev+i) = dmeanIn
    // enddo
    // endif
  }
}

//
// Write the output to a file
//
void Tilt::dumpSlice(int lslice, float &dmin, float &dmax, double &dtot8)
{
  int numParExtraLines, iend, index, i, j, imapOut, indDel;
  float fill;
  double dtmp8;
  float *outArr;
  //
  // If adding to a base rec, read in each line and add scaled values
  outArr = mOutSliceArr;
  if (mNumSIRTiter > 0 && mIfAlpha >= 0)
    outArr = mReadInArray;
  if (mReadBaseRec) {
    index = 0;
    iiuSetPosition(3, lslice - 1, 0);
    if (mIterForReport > 0)
      sampleForReport(outArr, lslice, mIthickOut, 1, mOutScale, mOutAdd);
    for (j = 1; j <= mIthickOut; j++) {
      if (iiuReadLines(3, (char *)mProjLine, 1))
        exitError("Reading line for subtraction");
      if (mRecSubtraction) {
        //
        // SIRT subtraction from base with possible sign constraints
        if (mIsignConstraint == 0) {
          for (i = 0; i < mIwidth; i++)
            mOutSliceArr[index + i] = mProjLine[i] / mOutScale - mOutAdd - 
              mOutSliceArr[index + i];
        } else if (mIsignConstraint < 0) {
          for (i = 0; i < mIwidth; i++)
            mOutSliceArr[index + i] = B3DMIN(0., mProjLine[i] / mOutScale - mOutAdd - 
                                           mOutSliceArr[index + i]);
        } else {
          for (i = 0; i < mIwidth; i++)
            mOutSliceArr[index + i] = B3DMAX(0., mProjLine[i] / mOutScale - mOutAdd - 
                                           mOutSliceArr[index + i]);
        }
      } else {
        //
        // Generic addition of the scaled base data
        for (i = 0; i < mIwidth; i++)
          mOutSliceArr[index + i] += mProjLine[i] / mBaseOutScale - mBaseOutAdd;
      }
      index = index + mIwidth;
    }
  }
  //
  numParExtraLines = 100;
  iend = mIthickOut * mIwidth;
  //
  // scale
  // DNM simplified and fixed bug in getting min/max/mean
  dtmp8 = 0.;
  //
  // DNM 9/23/04: incorporate reprojBP option
  //
  if (mReprojBP) {
    //--------------scale
    for (i = 0; i < iend; i++) {
      outArr[i] = (outArr[i] + mOutAdd) * mOutScale;
    }
    //
    // Fill value assumes edge fill value
    //
    fill = (mEdgeFill + mOutAdd) * mOutScale;
    for (j = 1; j <= mNumReproj; j++) {
      i = (j - 1) * mIwidth + 1;
      reProject(outArr, mIwidth, mIthickBP, mIwidth, mSinReproj[j - 1], 
          mCosReproj[j - 1], &mXRayStart[i - 1], &mYRayStart[i - 1], 
          &mNumPixInRay[i - 1], mMaxRayPixels[j - 1], fill, mProjLine, 0, 0);
      for (i = 0; i < mIwidth; i++) {
        ACCUM_MIN(dmin, mProjLine[i]);
        ACCUM_MAX(dmax, mProjLine[i]);
        dtmp8 = dtmp8 + mProjLine[i];
      }
      i = (lslice - mIsliceStart) / mIdelSlice;
      if (mMinTotSlice > 0)
        i = lslice - mMinTotSlice;
      parWrtPosn(2, j - 1, i);
      parWrtLin(2, (char *)mProjLine);
    }
    dtot8 = dtot8 + dtmp8;
    return;
  }
  //
  //--------------scale and get min / max / sum
  for (i = 0; i < iend; i++) {
    outArr[i] = (outArr[i] + mOutAdd) * mOutScale;
    ACCUM_MIN(dmin, outArr[i]);
    ACCUM_MAX(dmax, outArr[i]);
    dtmp8 = dtmp8 + outArr[i];
    //
  }
  dtot8 = dtot8 + dtmp8;
  //
  // Dump slice
  if (mPerpendicular) {
    // ....slices correspond to sections of map
    parWrtSec(2, (char *)outArr);
  } else {
    // ....slices must be properly stored
    // Take each line of array and place it in the correct section of the map.
    index = 0;
    indDel = 1;
    if (mRotateBy90) {
      index = imapOut + (mIthickOut - 1) * mIwidth;
      indDel = -1;
    }
    for (j = 1; j <= mIthickOut; j++) {
      iiuSetPosition(2, j - 1, (lslice - mIsliceStart) / mIdelSlice);
      if (iiuWriteLines(2, (char *)&outArr[index], 1))
        exitError("Writing data");
      //
      // DNM 2/29/01: partially demangle the parallel output by writing
      // up to 100 lines at a time in this plane
      //
      if (((lslice - mIsliceStart) / mIdelSlice) % numParExtraLines == 0) {
        for (i = 1; i <= B3DMIN(numParExtraLines - 1,
                                (mIsliceEnd - lslice) / mIdelSlice); i++) {
          if (iiuWriteLines(2, (char *)&outArr[index], 1))
            exitError("Writing data");
        }
      }
      index = index + indDel * mIwidth;
    }
  }
}

//
// maskEdges out (blur) the edges of the slice at the given index
//
void Tilt::maskSlice(float *array, int ithickMask)
{
  int i, j, idir, limit, lr, nsum, numSmooth;
  int numTaper, index, ibase;
  float sum, edgeMean, frac;
  //
  numSmooth = 10;
  numTaper = 10;
  idir = -1;
  limit = 1;
  for (lr = 0; lr < 2; lr++) {
    //
    // find mean along edge
    sum = 0.;
    for (j = 0; j < ithickMask; j++) {
      sum = sum + array[j * mIwidth + mIxUnmaskedSE[lr + 2 * j] - 1];
    }
    edgeMean = sum / ithickMask;
    //
    // For each line, sum progressively more pixels along edge out to a limit
    for (j = 1; j <= ithickMask; j++) {

      // This is "-1-based" to take care of SE and index being 1-based
      ibase = (j - 1) * mIwidth - 1;
      index = mIxUnmaskedSE[lr + 2 * (j - 1)] + idir;
      sum = array[ibase + mIxUnmaskedSE[lr + 2 * (j - 1)]];
      nsum = 1;
      for (i = 1; i <= numSmooth; i++) {
        if (idir * (limit - index) < 0)
          break;
        if (j + i <= ithickMask) {
          nsum = nsum + 1;
          sum = sum + array[ibase + i * mIwidth + mIxUnmaskedSE[lr + 2 * (j + i - 1)]];
        }
        if (j - i >= 1) {
          nsum = nsum + 1;
          sum = sum + array[ibase - i * mIwidth + mIxUnmaskedSE[lr + 2 * (j - i - 1)]];
        }
        //
        // Taper partway to mean over this smoothing distance
        frac = i / (numTaper + numSmooth + 1.);
        array[ibase + index] = (1. - frac) * sum / nsum + frac * edgeMean;
        index = index + idir;
      }
      //
      // Then taper rest of way down to mean over more pixels, then fill
      // with mean
      for (i = 1; i <= numTaper; i++) {
        if (idir * (limit - index) < 0)
          break;
        frac = (i + numSmooth) / (numTaper + numSmooth + 1.);
        array[ibase + index] = (1. - frac) * sum / nsum + frac * edgeMean;
        index = index + idir;
      }

      // Beware of loops that can run both ways
      for (i = index; (limit - i) * idir >= 0; i += idir) {
        array[ibase + i] = edgeMean;
      }
    }
    //
    // Set up for other direction
    limit = mIwidth;
    idir = 1;
  }
}

//
// The giant input routine, gets input and sets up almost all parameters
// -----------------------------------------------------------------
void Tilt::inputParameters(int argc, char *argv[])
{
  const int LIMNUM = 100;
  //
  int mpxyz[3], noutXyz[3], nrecXyz[3], nvsXyz[3], maxNeeds[LIMNUM];
  int maxTex2D[2], maxTexLayer[3], maxTex3D[3];
  float outHdrTilt[3] = {90., 0., 0.};
  float cell[6] = {0., 0., 0., 90., 90., 90.};
  float degToRad = RADIANS_PER_DEGREE;
  float delta[3];
  FILE *fp;
  Imod *imodToProject;
  //
  char *card;
  char *inputFile, *outputFile, *recFile, *baseFile, *boundFile, *vertBoundFile;
  char *vertOutFile, *angleOutput, *transformFile, *defocusFile;
  int nfields, inum[LIMNUM], nprojXyz[3];
  int *listTemp;
  float xnum[LIMNUM];
  //
  int *ivExclude, *ivReproj = NULL;
  float *packLocal = NULL, *angReproj, *tempArr;
  int mode, newAngles, ifTiltFile, numViewUse, numViewExclude, numNeedEval;
  float delAngle, compFactor, globalAlpha, xOffset, scaleLocal, radMax, radFall, xoffAdj;
  int iradFall, iradMax, numCompress, nxFull, nyFull, ixSubset, iySubset, ixSubsetIn;
  int kti, indBase, ipos, idType, lens, nxFullIn;
  int nd1, nd2, nv, numSlices, indi, i, iex, numViewOrig, iv;
  float vd1, vd2, delTheta, theta, thetaView;
  int nxProjPad, numInput, numExcludeList, j, ind, numEnt;
  int numPadTmp, nprojPad, ifZfactors, localZfacs, adjustOrigin;
  int ifThickIn, ifSliceIn, ifWidthIn, imageBinned, ifSubsetIn, ierr;
  float pixelLocal, dminTmp, dmaxTmp, dmeanTmp, frac, originX, originY, originZ;
  float gpuMemoryFrac, gpuMemory, pixForDefocus, focusInvert, freq, arg, dxLine;
  int nViewsReproj, iwideReproj, k, ind1, ind2, ifExpWeight;
  int minMemory, indGPU, maxSuperFac;
  int indDelta, ifExit, ifMultByGaussian, ifHammingLike, if3Dtexture;
  bool projModel, realChunkRun;
  double wallStart;
  const float pi = 3.141593;
  //
  int numOptArg, numNonOptArg;
  //
  // fallbacks from ../../manpages/autodoc2man 2 2  tilt
  //
  int numOptions = 77;
  const char *options[] = {
    "input:InputProjections:FN:", "output:OutputFile:FN:", ":TILTFILE:FN:",
    ":XTILTFILE:FN:", ":ZFACTORFILE:FN:", ":LOCALFILE:FN:", ":BoundaryInfoFile:FN:",
    ":WeightAngleFile:FN:", ":WeightFile:FN:", ":WIDTH:I:", ":SLICE:IA:",
    ":TOTALSLICES:IP:", ":THICKNESS:I:", ":OFFSET:FA:", ":SHIFT:FA:", ":ANGLES:FAM:",
    ":XAXISTILT:F:", ":COMPFRACTION:F:", ":COMPRESS:FAM:", ":FULLIMAGE:IP:",
    ":SUBSETSTART:IP:", ":IMAGEBINNED:I:", ":LOCALSCALE:F:", ":LOG:F:", ":RADIAL:FP:",
    ":FalloffIsTrueSigma:B:", ":MultiplyByGaussian:B:", ":HammingLikeFilter:F:",
    ":DENSWEIGHT:FA:", ":ExactFilterSize:I:", ":FakeSIRTiterations:I:", ":INCLUDE:LIM:",
    ":EXCLUDELIST2:LIM:", ":COSINTERP:IA:", ":XTILTINTERP:I:", ":UseGPU:I:",
    ":ActionIfGPUFails:IP:", ":MODE:I:", ":SCALE:FP:", ":MASK:I:", ":PERPENDICULAR:B:",
    ":PARALLEL:B:", ":RotateBy90:B:", ":AdjustOrigin:B:", ":TITLE:CH:",
    ":BaseRecFile:FN:", ":BaseNumViews:I:", ":SubtractFromBase:LI:", ":MinMaxMean:IT:",
    ":REPROJECT:FAM:", ":ViewsToReproject:LI:", "recfile:RecFileToReproject:FN:",
    "xminmax:XMinAndMaxReproj:IP:", "yminmax:YMinAndMaxReproj:IP:",
    "zminmax:ZMinAndMaxReproj:IP:", "threshold:ThresholdedReproj:FT:",
    ":FlatFilterFraction:F:", ":SIRTIterations:I:", ":SIRTSubtraction:B:",
    ":StartingIteration:I:", ":VertBoundaryFile:FN:", ":VertSliceOutputFile:FN:",
    ":VertForSIRTInput:B:", ":ConstrainSign:I:", ":ProjectModel:FN:",
    ":AngleOutputFile:FN:", ":AlignTransformFile:FN:", ":DefocusFile:FN:",
    ":PixelForDefocus:FP:", ":DONE:B:", ":FBPINTERP:I:", ":REPLICATE:FPM:",
    "debug:DebugOutput:B:", "internal:InternalSIRTSlices:IP:", "texture:TextureType:I:",
    "param:ParameterFile:PF:", "help:usage:B:"};
  //
  mRecReproj = false;
  nViewsReproj = 0;
  mUseGPU = false;
  indGPU = -1;;
  mNumGpuPlanes = 0;
  mNumSIRTiter = 0;
  mSirtFromZero = false;
  mReadBaseRec = false;
  mRecSubtraction = false;
  mProjSubtraction = false;
  mVertSirtInput = false;
  mSaveVertSlices = false;
  angleOutput = NULL;
  transformFile = NULL;
  mFlatFrac = 0.;
  mIterForReport = 0;
  mThreshPolarity = 0.;
  focusInvert = 0.;
  defocusFile = NULL;
  mNeedForFiltArr = 0;
  mNeedForOutArr = 0;
  mNeedForVertArr = 0;
  mNeedForReadInArr = 0;
  mNeedForWorkArr = 0;
  mNeedForSuperArr = 0;
  maxSuperFac = 8;
  //
  // Minimum array size to allocate, desired number of slices to allocate
  // for if it exceeds that minimum size
  minMemory = 20000000;
  numNeedEval = 10;
  gpuMemoryFrac = 0.8;
  //
  // Pip startup: set error, parse options, check help, set flag if used
  //
  PipSetSpecialFlags(1, 1, 2, 2, 0);
  PipReadOrParseOptions(argc, argv, options, numOptions, "tilt", 
                        0, 1, 1, &numOptArg, &numNonOptArg, NULL);

  // Handle help here, this program still allows no arguments
  ind  = 0;
  PipGetBoolean("usage", &ind);
  if (ind) {
    PipPrintHelp("tilt", 0, 0, 0);
    exit(0);
  }

  if (PipGetInOutFile("InputProjections", 0, &inputFile) != 0) 
      exitError("No input file with projections specified");
  if (PipGetInOutFile("OutputFile", 1, &outputFile) != 0) 
      exitError("No output file specified");
  //
  // Allocate array a little bit for temp use
  mLimReproj = 100000;
  tempArr = B3DMALLOC(float, mLimReproj);
  angReproj = B3DMALLOC(float, mLimReproj);
    memoryError(!tempArr || !angReproj ? NULL : tempArr, "small temporary arrays");
  PipGetBoolean("DebugOutput", &mDebug);
  //
  // Open input projection file
  iiuOpen(1, inputFile, "RO");
  iiuPrintHeader(1, "\nInput projection file:");
  iiuRetBasicHead(1, nprojXyz, mpxyz, &mode, &mDminIn, &mDmaxIn, &mDmeanIn);
  iiuRetDataType(1, &idType, &lens, &nd1, &nd2, &vd1, &vd2);
  mNxProj = nprojXyz[0];
  mNyProj = nprojXyz[1];
  mNumViews = nprojXyz[2];
  mLimView = mNumViews + 10;
  mSinBeta = B3DMALLOC(float, mLimView);
  mCosBeta = B3DMALLOC(float, mLimView);
  mSinAlpha  = B3DMALLOC(float, mLimView);
  mCosAlpha = B3DMALLOC(float, mLimView);
  mAlpha = B3DMALLOC(float, mLimView);
  mAngles = B3DMALLOC(float, mLimView);
  mXZfac = B3DMALLOC(float, mLimView);
  mYZfac  = B3DMALLOC(float, mLimView);
  mCompress = B3DMALLOC(float, mLimView);
  mNxStretched = B3DMALLOC(int, mLimView);
  mIndStretchLine = B3DMALLOC(int, mLimView);
  mStretchOffset = B3DMALLOC(float, mLimView);
  mExposeWeight  = B3DMALLOC(float, mLimView);
  mMapUsedView = B3DMALLOC(int, mLimView);
  mWgtAngles = B3DMALLOC(float, mLimView);
  ivExclude = B3DMALLOC(int, mLimView);
  if (!(mSinBeta && mCosBeta && mSinAlpha && mCosAlpha && mAlpha && mAngles && mXZfac &&
        mYZfac && mCompress && mNxStretched && mIndStretchLine && mStretchOffset &&
        mExposeWeight && mMapUsedView && mWgtAngles && ivExclude))
    memoryError(NULL, "arrays for variables per view");
  //
  // The approximate implicit scaling caused by the default radial filter
  mFilterScale = mNxProj / 2.2;
  //
  newAngles = 0;
  ifTiltFile = 0;
  //
  // Get model file to project and other optional files
  projModel = PipGetString("ProjectModel", &recFile) == 0;
  if (projModel) {
    imodToProject = imodRead(recFile);
    if (!imodToProject)
      exitError("Reading model file to reproject");
    free(recFile);
    PipGetString("AngleOutputFile", &angleOutput);
    PipGetString("AlignTransformFile", &transformFile);
    PipGetString("DefocusFile", &defocusFile);
    PipGetTwoFloats("PixelForDefocus", &pixForDefocus, &focusInvert);
    PipGetBoolean("SkipTurnedOffPoints", &mSkipUnseenPoints);
  }
  //
  // Get entries for reprojection from rec file
  PipGetInteger("SIRTIterations", &mNumSIRTiter);
  PipGetThreeFloats("ThresholdedReproj", &mThreshForReproj, &mThreshPolarity, 
      &mThreshSumFac);
  if (PipGetString("RecFileToReproject", &recFile) == 0) {
    if (projModel)
      exitError("You cannot use -RecFileToReproject with -ProjectModel");
    mProjMean = mDmeanIn;
    iiuOpen(3, recFile, "RO");
    iiuPrintHeader(3, "\nFile to reproject:");
    iiuRetBasicHead(3, nrecXyz, mpxyz, &mode, &mDminIn, &mDmaxIn, &mDmeanIn);
    if (mNumSIRTiter <= 0) {
      mRecReproj = true;
      mMinXreproj = 0;
      mMinYreproj = 0;
      mMinZreproj = 0;
      mMaxXreproj = nrecXyz[0] - 1;
      mMaxYreproj = nrecXyz[1] - 1;
      mMaxZreproj = nrecXyz[2] - 1;
      PipGetTwoIntegers("XMinAndMaxReproj", &mMinXreproj, &mMaxXreproj);
      PipGetTwoIntegers("YMinAndMaxReproj", &mMinYreproj, &mMaxYreproj);
      PipGetTwoIntegers("ZMinAndMaxReproj", &mMinZreproj, &mMaxZreproj);
      if (mMinXreproj < 0 || mMinYreproj < 0 || mMaxXreproj >= 
          nrecXyz[0] || mMaxYreproj >= nrecXyz[1])
        exitError("Min or Max X, or Y coordinate to project is out of range");
      if (PipGetString("ViewsToReproj", &card) == 0) {
        ivReproj = parseCard(card, nViewsReproj, "list of views to reproject");
      }
      PipGetBoolean("SIRTSubtraction", &mProjSubtraction);
    }
    mMinXreproj = mMinXreproj + 1;
    mMaxXreproj = mMaxXreproj + 1;
    mMinYreproj = mMinYreproj + 1;
    mMaxYreproj = mMaxYreproj + 1;
    mMinZreproj = mMinZreproj + 1;
    mMaxZreproj = mMaxZreproj + 1;
    //
    // If not reading from a rec and doing sirt, then must be doing from 0
  } else if (mNumSIRTiter > 0) {
    mSirtFromZero = true;
    mFlatFrac = 1.;
  }
  if (mThreshPolarity != 0. && ! mRecReproj)
    exitError("Thresholded reprojection can be used only with the -RecFileToReproject "
              "option");
  if (mThreshPolarity != 0. && (mNumSIRTiter > 0 || mProjSubtraction)) 
    exitError("Thresholded reprojection cannot be used with -SIRTSubtraction "
              "or -SIRTIterations");

  if (! mRecReproj && !projModel && mNumSIRTiter <= 0 && 
      PipGetString("BaseRecFile", &baseFile) == 0) {
    mReadBaseRec = true;
    if (PipGetString("SubtractFromBase", &card) == 0) {
      mIviewSubtract = parseCard(card, mNumViewSubtract, "list of views to subtract");
      if (mNumViewSubtract == 1 && mIviewSubtract[0] < 0) {
        mRecSubtraction = true;
        mNumViewSubtract = 0;
      }
    }
    if (! mRecSubtraction && PipGetInteger("BaseNumViews", &mNumViewBase) != 0)
      exitError("You must enter -BaseNumViews with -BaseRecFile");
    iiuOpen(3, baseFile, "RO");
    iiuRetBasicHead(3, nrecXyz, mpxyz, &mode, &dminTmp, &dmaxTmp, &dmeanTmp);
  }
  //
  //-------------------------------------------------------------
  // Set up defaults:
  //
  //...... Default slice is all rows in a projection plane
  mIsliceStart = 1;
  mIsliceEnd = mNyProj;
  mIdelSlice = 1;
  //...... and has the same number of columns.
  mIwidth = mNxProj;
  //...... Default is no maskEdges and extra pixels to maskEdges is 0
  mMaskEdges = false;
  mNumExtraMaskPix = 0;
  //...... Default is no scaling of output map
  mOutAdd = 0.;
  mOutScale = 1.;
  //...... Default is no offset or rotation
  delAngle = 0.;
  //...... Default is output mode 2
  mNewMode = 2;
  //...... Start with no list of views to use or exclude
  numViewUse = 0;
  numViewExclude = 0;
  //...... Default is no logarithms
  mIfLog = 0;
  //...... Default radial weighting parameters - no filtering
  iradMax = mNxProj / 2 + 1;
  radFall = 0.;
  ifMultByGaussian = 0;
  //...... Default overall and individual compression of 1; no alpha tilt
  numCompress = 0;
  compFactor = 1.;
  for (nv = 0; nv < mNumViews; nv++) {
    mCompress[nv] = 1.;
    mAlpha[nv] = 0.;
    mXZfac[nv] = 0.;
    mYZfac[nv] = 0.;
    mExposeWeight[nv] = 1.;
  }
  mIfAlpha = 0;
  globalAlpha = 0.;
  ifZfactors = 0;
  //
  //...... Default weighting by density of adjacent views
  mNumTiltIncWgt = 2;
  for (i = 1; i <= mNumTiltIncWgt; i++) {
    mTiltIncWgts[i - 1] = 1. / (i - 0.5);
  }
  mNumWgtAngles = 0;
  //
  xOffset = 0;
  mYOffset = 0;
  mAxisXoffset = 0.;
  mNxWarp = 0;
  mNyWarp = 0;
  scaleLocal = 0.;
  nxFullIn = 0;
  nyFull = 0;
  ixSubsetIn = 0;
  iySubset = 0;
  mIthickBP = 10;
  imageBinned = 1;
  ifThickIn = 0;
  ifWidthIn = 0;
  ifSliceIn = 0;
  ifSubsetIn = 0;
  ifExpWeight = 0;
  mMinTotSlice = -1;
  mMaxTotSlice = -1;
  mExactSamples = 100.;
  mNumExactCycles = 0;
  mNumFakeSIRTiter = 0;
  //
  //...... Default double - width linear interpolation in cosine stretching
  mInterpFacStretch = 2;
  mInterpOrdStretch = 1;
  mInterpOrdXtilt = 1;
  mSuperSampleFac = 1;
  mCleanSuperFFT = 1;
  mProjSuperFac = 1;
  mPerpendicular = true;
  mRotateBy90 = false;
  mReprojBP = false;
  mNumReproj = 0;
  adjustOrigin = 0;
  mNumViewSubtract = 0;
  mIactGpuFailOption = 0;
  mIactGpuFailEnviron = 0;
  mIfOutSirtProj = 0;
  mIfOutSirtRec = 0;
  mIsignConstraint = 0;
  vertOutFile = NULL;
  vertBoundFile = NULL;
  //
  //...... Default title
  mrcFillLabelString(mRecReproj ? "TILT: Reprojection from tomogram" : 
                     "TILT: Tomographic reconstruction", mTitle);
  if (PipGetString("TITLE", &card) == 0) {
    mrcFillLabelString(card, mTitle);
    free(card);
  }
  //
  nfields = 0;
  if (PipGetIntegerArray("SLICE", inum, &nfields, LIMNUM) == 0) {
    if (nfields / 2 != 1)
      exitError("Wrong number of fields on SLICE line");
    mIsliceStart = inum[0] + 1;
    mIsliceEnd = inum[1] + 1;
    if (nfields > 2)
      mIdelSlice = inum[2];
    if (mIdelSlice <= 0)
      exitError("Negative slice increments are not allowed");
    ifSliceIn = 1;
  }
  //
  if (PipGetInteger("THICKNESS", &mIthickBP) == 0)
    ifThickIn = 1;
  //
  if (PipGetInteger("MASK", &mNumExtraMaskPix) == 0) {
    mMaskEdges = true;
    mNumExtraMaskPix = B3DMAX(-mNxProj / 50, B3DMIN(mNxProj / 50, mNumExtraMaskPix));
    printf("\n Mask applied to edges of output slices with %d extra pixels masked\n", 
           mNumExtraMaskPix);
  }
  if3Dtexture = -999;
  PipGetInteger("TextureType", &if3Dtexture);
  //
  // Hamming and RADIAL entry are mutually exclusive
  ifHammingLike = 1 - PipGetFloat("HammingLikeFilter", &radMax);
  if (ifHammingLike > 0) {
    iradMax = mNxProj * radMax;
    radFall = 0.438 * (0.5 * mNxProj - iradMax);
    ifMultByGaussian = 1;
    printf("\n Doing Hamming-like filter by multiplying by Gaussian with cutoff =%7.4f"
           "  sigma = %7.4f\n", iradMax / float(mNxProj), radFall / mNxProj);
  }

  if (PipGetTwoFloats("RADIAL", &radMax, &radFall) == 0) {
    if (ifHammingLike > 0)
      exitError("You cannot enter HammingLikeFilter with RADIAL");
    iradMax = radMax;
    iradFall = radFall;
    if (iradMax == 0)
      iradMax = mNxProj * radMax;
    if (iradFall == 0)
      radFall = mNxProj * radFall;
    //
    // Adjust the falloff if it is NOT true sigma, the function now uses value correctly
    iv = 0;
    PipGetBoolean("FalloffIsTrueSigma", &iv);
    if (iv == 0)
      radFall = sqrt(0.5) * radFall;
    printf("\n Radial weighting function Gaussian starts at cutoff =%7.4f"
           "  sigma =%7.4f\n", iradMax / float(mNxProj), radFall / mNxProj);
  }
  //
  if (PipGetBoolean("MultiplyByGaussian", &ifMultByGaussian) == 0) {
    if (ifHammingLike > 0 && ifMultByGaussian == 0)
      exitError("You cannot enter HammingLikeFilter and MultiplyByGaussian 0");
    if (ifHammingLike == 0) 
      printf("\n Multiplying radial function by Gaussian\n");
  }
  //
  nfields = 0;
  if (PipGetFloatArray("OFFSET", xnum, &nfields, LIMNUM) == 0) {
    if (nfields == 0 || nfields >= 3)
        exitError("Wrong number of fields on OFFSET line");
    if (nfields == 2)
      mAxisXoffset = xnum[1];
    delAngle = xnum[0];
  }
  //
  if (PipGetTwoFloats("SCALE", &mOutAdd, &mOutScale) == 0) 
    printf("\n Output map densities incremented by %.2f and then multiplied by %.2f\n",
           mOutAdd, mOutScale);
  //
  iv = PipGetBoolean("PERPENDICULAR", &mPerpendicular);
  j = PipGetBoolean("RotateBy90", &mRotateBy90);
  i = 0;
  k = PipGetBoolean("PARALLEL", &i);
  if ((iv == 0 && mPerpendicular && (i > 0 || mRotateBy90)) ||  
      (i > 0 && mRotateBy90)) 
    exitError("You can select only one of PERPENDICULAR, PARALLEL, and RotateBy90");
  if (i > 0 || mRotateBy90)
    mPerpendicular = false;
  if (mPerpendicular) {
    printf("\n Output map is sectioned perpendicular to the tilt axis\n");
  } else {
    printf(" Output map is sectioned parallel to the zero tilt projection, %s "
           "handedness\n", mRotateBy90 ? "retaining" : "inverting");
  }
  //
  if (PipGetInteger("MODE", &mNewMode) == 0) {
    if (mNewMode < 0 || mNewMode > 15 || (mNewMode > 2 && mNewMode < 9)) 
        exitError("Illegal output mode");
    printf("\n Data mode of output file is %d\n", mNewMode);
  }
  //
  PipNumberOfEntries("INCLUDE", &numEnt);
  for (j = 1; j <= numEnt; j++) {
    PipGetString("INCLUDE", &card);
    listTemp = parseCard(card, numExcludeList, "list of included views");
    if (numViewUse + numExcludeList > mLimView)
      exitError("More included views then views in the input file");
    for (i = 0; i < numExcludeList; i++) {
      if (listTemp[i] < 1 || listTemp[i] > mNumViews)
        exitError("Illegal view number in INCLUDE list");
      mMapUsedView[numViewUse++] = listTemp[i];
    }
    free(listTemp);
  }
  //
  PipNumberOfEntries("EXCLUDELIST2", &numEnt);
  for (j = 1; j <= numEnt; j++) {
    PipGetString("EXCLUDELIST2", &card);
    listTemp = parseCard(card, numExcludeList, "list of excluded views");
    if (numViewExclude + numExcludeList > mLimView)
      exitError("More excluded views then views in the input file");
    for (i = 0; i < numExcludeList; i++) {
      if (listTemp[i] < 1 || listTemp[i] > mNumViews)
        exitError("Illegal view number in EXCLUDE list");
      ivExclude[numViewExclude++] = listTemp[i];
    }
    free(listTemp);
  }
  //
  if (numViewUse > 0 && numViewExclude > 0)
    exitError("Illegal to have both INCLUDE and EXCLUDE entries");
  //
  if (PipGetFloat("LOG", &mBaseForLog) == 0) {
    mIfLog = 1;
    printf("\n Taking logarithm of input data plus %.3f\n", mBaseForLog);
  }
  //
  // Removed replications
  //
  PipNumberOfEntries("ANGLES", &numEnt);
  for (j = 1; j <= numEnt; j++) {
    nfields = 0;
    PipGetFloatArray("ANGLES",  &mAngles[newAngles], &nfields, mLimView - newAngles);
    newAngles = newAngles + nfields;
  }
  //
  PipNumberOfEntries("COMPRESS", &numEnt);
  for (j = 1; j <= numEnt; j++) {
    nfields = 0;
    PipGetFloatArray("COMPRESS",  &mCompress[numCompress], &nfields,
                     mLimView - numCompress);
    numCompress = numCompress + nfields;
  }
  //
  if (PipGetFloat("COMPFRACTION", &compFactor) == 0) 
    printf("\n Compression was confined to %.3f of the distance over which it was "
           "measured\n", compFactor);
  //
  nfields = 0;
  if (PipGetFloatArray("DENSWEIGHT", xnum, &nfields, LIMNUM) == 0) {
    mNumTiltIncWgt = B3DNINT(xnum[0]);
    if (mNumTiltIncWgt > 0) {
      for (i = 1; i <= mNumTiltIncWgt; i++) {
        mTiltIncWgts[i - 1] = 1. / (i - 0.5);
      }
      if (nfields == mNumTiltIncWgt + 1) {
        for (i = 1; i <= mNumTiltIncWgt; i++) {
          mTiltIncWgts[i - 1] = xnum[i];
        }
      } else if (nfields != 1) {
        exitError("Wrong number of fields on DENSWEIGHT line");
      }
      printf("\n Weighting by tilt density computed to distance of %d views\n"
             "  weighting factors:", mNumTiltIncWgt);
      for (i = 1; i <= mNumTiltIncWgt; i++) {
        printf("%6.3f", mTiltIncWgts[i - 1]);
        if (i % 10 == 0 || i == mNumTiltIncWgt)
          printf("\n");
      }
    } else {
      printf("\n No weighting by tilt density\n");
    }
  }
  //
  // Keep this with the above entry so that nfields indicates if it was entered
  if (PipGetInteger("ExactFilterSize", &numEnt) == 0) {
    if (nfields > 0)
      exitError("You cannot enter DENSWEIGHT with ExactFilterSize");
    mExactObjSize = numEnt;
    mExactTable = B3DMALLOC(float, B3DNINT(mExactSamples) + 1);
    memoryError(mExactTable, "array for exact filter samples");
    mNumExactCycles = 1;

    // This array was indexed from 0 in Fortran
    for (i = 0; i <= B3DNINT(mExactSamples); i++) {
      mExactTable[i] = 1. - i / mExactSamples;
    }
  }
  //
  if (PipGetInteger("FakeSIRTiterations", &mNumFakeSIRTiter) == 0 && mNumExactCycles > 0) 
    exitError("You cannot enter FakeSIRTiterations with ExactFilterSize");
  //
  if (PipGetString("TILTFILE", &card) == 0) {
    getValuesFromLines(NULL, card, mAngles, NULL, false, mNumViews, "tilt angles");
    ifTiltFile = 1;
  }
  //
  if (PipGetInteger("WIDTH", &mIwidth) == 0)  
    ifWidthIn = 1;
  //
  nfields = 0;
  if (PipGetFloatArray("SHIFT", xnum, &nfields, LIMNUM) == 0) {
    if ((nfields + 1) / 2 != 1) 
        exitError("Wrong number of fields on SHIFT line");
    xOffset = xnum[0];
    if (nfields == 2) 
      mYOffset = xnum[1];
  }
  //
  if (PipGetString("XTILTFILE", &card) == 0) {
    getValuesFromLines(NULL, card, mAlpha, NULL, false, mNumViews, "X-axis tilt angles");
    mIfAlpha = 1;
    for (i = 0; i < mNumViews; i++) {
      if (B3DABS(mAlpha[i] - mAlpha[0]) > 1.e-5)
        mIfAlpha = 2;
    }
    if (mIfAlpha == 2)
      printf("\n Alpha tilting to be applied with angles from file\n");
    if (mIfAlpha == 1)
      printf("\n Constant alpha tilt of %.1f  to be applied based on angles from file\n",
             -mAlpha[0]);
  }
  //
  if (PipGetString("WeightFile", &card) == 0) {
    getValuesFromLines(NULL, card, mExposeWeight, NULL, false, mNumViews, 
                       "weighting factors");
    ifExpWeight = 1;
  }
  //
  boundFile = NULL;
  PipGetString("BoundaryInfoFile", &boundFile);
  //
  if (PipGetFloat("XAXISTILT", &globalAlpha) == 0) {
    printf("\n Global alpha tilt of %.1f will be applied\n", globalAlpha);
    if (B3DABS(globalAlpha) > 1.e-5 && mIfAlpha == 0)
      mIfAlpha = 1;
  }
  //
  // REPROJECT entry must be read in before local alignments
  // violates original unless a blank entry is allowed
  PipNumberOfEntries("REPROJECT", &numEnt);
  for (j = 1; j <= numEnt; j++) {
    nfields = 0;
    PipGetFloatArray("REPROJECT",  xnum, &nfields, LIMNUM);
    if (nfields == 0) {
      nfields = 1;
      xnum[0] = 0.;
    }
    if (nfields + mNumReproj > mLimReproj)
      exitError("Too many reprojection angles for arrays");
    for (i = 0; i < nfields; i++) {
      angReproj[mNumReproj++] = xnum[i];
    }
    if (j == 1)
      printf("\n Output will be one or more reprojections\n");
    mReprojBP = !mRecReproj;
  }
  //
  if (PipGetString("LOCALFILE", &card) == 0) {
    fp = fopen(card, "r");
    numInput = -1;
    ierr = fgetline(fp, mLine, MAX_LINE);
    if (ierr < 0)
      exitError("Reading top line of local alignment file %s", card);
    if (PipGetLineOfValues(mLine, mLine, xnum, PIP_FLOAT, &numInput, LIMNUM)) {
      PipGetError(&card);
      exitError("Getting values from top line of local alignment file: %s", card);
    }
    free(card);
    mIfDelAlpha = 0;
    if (numInput > 6)
      mIfDelAlpha = B3DNINT(xnum[6]);
    pixelLocal = 0.;
    if (numInput > 7) 
      pixelLocal = xnum[7];
    localZfacs = 0;
    if (numInput > 8) 
      localZfacs = xnum[8];
    mNxWarp = B3DNINT(xnum[0]);
    mNyWarp = B3DNINT(xnum[1]);
    mIxStartWarp = B3DNINT(xnum[2]);
    mIyStartWarp = B3DNINT(xnum[3]);
    mIdelXwarp = B3DNINT(xnum[4]);
    mIdelYwarp = B3DNINT(xnum[5]);
    mNumWarpPos = mNxWarp * mNyWarp;
    mLimWarp = mNxWarp * mNyWarp * mNumViews;
    ipos = mLimWarp;
    indDelta = mNumViews;
    //
    // If reprojecting rec, make sure arrays are big enough for all the
    // reprojections and  allocate extra space at top of some arrays for
    // temporary use
    if (mRecReproj) {
      indDelta = B3DMAX(mNumViews, mNumReproj);
      mLimWarp = mNxWarp * mNyWarp * indDelta;
      ipos = mLimWarp + indDelta;
    }
    if (mNxWarp < 2 || mNyWarp < 2)
      exitError("There must be at least two local alignment areas in X and in Y");
    mIndWarp = B3DMALLOC(int, mNumWarpPos);
    mDelAlpha = B3DMALLOC(float, ipos);
    mCWarpBeta = B3DMALLOC(float, mLimWarp);
    mSWarpBeta = B3DMALLOC(float, mLimWarp);
    mCWarpAlpha = B3DMALLOC(float, mLimWarp);
    mSWarpAlpha = B3DMALLOC(float, mLimWarp);
    mFwarp = B3DMALLOC(float, 6 * ipos);
    mDelBeta = B3DMALLOC(float, ipos);
    mWarpXZfac = B3DMALLOC(float, ipos);
    mWarpYZfac = B3DMALLOC(float, ipos);
    if (!(mIndWarp && mDelAlpha && mCWarpBeta && mSWarpBeta && mCWarpAlpha && mSWarpAlpha
          && mFwarp && mDelBeta && mWarpXZfac && mWarpYZfac))
        memoryError(NULL, "arrays for local alignment data");

    indBase = 0;
    for (ipos = 1; ipos <= mNxWarp * mNyWarp; ipos++) {
      mIndWarp[ipos - 1] = indBase;
      getValuesFromLines(fp, NULL, &mDelBeta[indBase], NULL, false, mNumViews,
                         "local tilt alignment data");
      if (mIfDelAlpha > 0) {
        getValuesFromLines(fp, NULL, &mDelAlpha[indBase], NULL, false, mNumViews,
                           "local tilt alignment data");
      } else {
        for (i = indBase; i < indBase + mNumViews; i++) {
          mDelAlpha[i] = 0.;
        }
      }
      //
      // Set z factors to zero, read in if supplied, then negate them
      //
      for (i = indBase; i < indBase + mNumViews; i++) {
        mWarpXZfac[i] = 0.;
        mWarpYZfac[i] = 0.;
      }
      if (localZfacs > 0)
        getValuesFromLines(fp, NULL, &mWarpXZfac[indBase], &mWarpYZfac[indBase], false,
                           mNumViews, "local tilt alignment data");
      for (i = indBase; i < indBase + mNumViews; i++) {
        mWarpXZfac[i] = -mWarpXZfac[i];
        mWarpYZfac[i] = -mWarpYZfac[i];
      }
      nfields = 6 * mNumViews;
      getValuesFromLines(fp, NULL, &mFwarp[6 * indBase], NULL, false, nfields,
                           "local tilt alignment data");
      // Transforms are stored as a11 a12 a21 a22
      // In fortran they are placed into (1,1) (1,2) (2,1) (2,2) of a 2-D array xf(2,3)
      // So in memory they are supposed to be (1,1) (2,1) (1,2) (2,2)
      // Since we just read in a11 a12 a21 a22 we need to swap a12 and a21
      for (kti = 0; kti < mNumViews; kti++)
        B3DSWAP(mFwarp[6 * (indBase + kti) + 1], mFwarp[6 * (indBase + kti) + 2],xnum[0]);
      indBase = indBase + indDelta;
    }
    printf("\n Local tilt alignment information read from file\n");
  }
  //
  if (PipGetFloat("LOCALSCALE", &scaleLocal) == 0) 
    printf("\n Local alignment positions and shifts reduced by %.4f\n", scaleLocal);

  //
  PipGetTwoIntegers("FULLIMAGE", &nxFullIn, &nyFull);
  if (PipGetTwoIntegers("SUBSETSTART", &ixSubsetIn, &iySubset) == 0) 
    ifSubsetIn = 1;
  // 
  if (!(mRecReproj || mNumSIRTiter > 0)) {
    PipGetInteger("SuperSampleFactor", &mSuperSampleFac);
    if (mSuperSampleFac < 1 || mSuperSampleFac > maxSuperFac)
      exitError("Super-sampling factor must be between 1 and %", maxSuperFac);
    if (mSuperSampleFac > 1) {
      ierr = 0;
      PipGetBoolean("ExpandInputLines", &ierr);
      if (ierr)
        mProjSuperFac = mSuperSampleFac;
      mInterpOrdStretch = 0;
      mInterpFacStretch = 0;
    }    
  }
  //
  nfields = 0;
  if (mSuperSampleFac < 2 && 
      PipGetIntegerArray("COSINTERP", inum, &nfields, LIMNUM) == 0) {
    mInterpOrdStretch = inum[0];
    if (nfields > 1)
      mInterpFacStretch = inum[1];
    mInterpOrdStretch = B3DMAX(0, B3DMIN(3, mInterpOrdStretch));
    if (mInterpOrdStretch == 0)
      mInterpFacStretch = 0;
    if (mInterpFacStretch == 0) {
      printf("Cosine stretching is disabled\n");
    } else {
      printf("\n Cosine stretching, if any, will have interpolation order %d"
             ", sampling factor %d\n", mInterpOrdStretch, mInterpFacStretch);
    }
  }
  //
  if (PipGetInteger("XTILTINTERP", &mInterpOrdXtilt) == 0) {
    if (mInterpOrdXtilt > 2)
      mInterpOrdXtilt = 3;
    if (mInterpOrdXtilt <= 0) {
      printf("New-style X-tilting with vertical slices is disabled\n");
    } else {
      printf("\n X-tilting with vertical slices, if any, will have interpolation order"
             " %d\n", mInterpOrdXtilt);
    }
  }
  //
  // Read environment variable first, then override by entry
  card = getenv("IMOD_USE_GPU");
  if (card)
    indGPU = atoi(card);
  mIfGpuByEnviron = PipGetInteger("UseGPU", &indGPU);
  card = getenv("IMOD_USE_GPU2");
  if (card) {
    indGPU = atoi(card);
    mIfGpuByEnviron = 1;
  }
  mUseGPU = indGPU >= 0 && mThreshPolarity == 0;
  PipGetTwoIntegers("ActionIfGPUFails", &mIactGpuFailOption, 
      &mIactGpuFailEnviron);
  //
  if (PipGetString("ZFACTORFILE", &card) == 0) {
    getValuesFromLines(NULL, card, mXZfac, mYZfac, false, mNumViews, "Z factors");
    ifZfactors = 1;
    printf("\n Z-dependent shifts to be applied with factors from file\n");
  }
  //
  if (PipGetInteger("IMAGEBINNED", &imageBinned) == 0) {
    imageBinned = B3DMAX(1, imageBinned);
    if (imageBinned > 1)
    printf("\n Dimensions and coordinates will be scaled down by a factor of %d\n",
           imageBinned);
  }
  //
  if (PipGetTwoIntegers("TOTALSLICES", &inum[0], &inum[1]) == 0) {
    mMinTotSlice = inum[0] + 1;
    mMaxTotSlice = inum[1] + 1;
  }
  //
  PipGetFloat("FlatFilterFraction", &mFlatFrac);
  mFlatFrac = B3DMAX(0.,  mFlatFrac);
  if (mFlatFrac > 0 && mNumFakeSIRTiter > 0)
    exitError("You cannot enter FlatFilterFraction with FakeSIRTiterations");
  //
  PipGetBoolean("AdjustOrigin", &adjustOrigin);
  //
  if (! mRecReproj)
      PipGetThreeFloats("MinMaxMean", &mDminIn, &mDmaxIn, &mDmeanIn);
  //
  if (PipGetString("WeightAngleFile", &card) == 0) {
    fp = fopen(card, "r");
    if (!fp)
      exitError("Opening weight angle file %s", card);
    free(card);
    mNumWgtAngles = -mLimView;
    getValuesFromLines(fp, NULL, mWgtAngles, NULL, false, mNumWgtAngles, 
                       "angles for weighting");
    //
    // Sort the angles
    rsSortFloats(mWgtAngles, mNumWgtAngles);
  }
  //
  // SIRT-related options
  PipGetInteger("ConstrainSign", &mIsignConstraint);
  //
  PipGetTwoIntegers("InternalSIRTSlices", &mIfOutSirtProj, &mIfOutSirtRec);
  if (mNumSIRTiter > 0 || mRecSubtraction)
      PipGetInteger("StartingIteration", &mIterForReport);
  PipGetBoolean("VertForSIRTInput", &mVertSirtInput);
  PipGetString("VertSliceOutputFile", &vertOutFile);
  PipGetString("VertBoundaryFile", &vertBoundFile);
  //
  PipDone();
  //
  // END OF OPTION READING
  //
  if (mIfAlpha != 0 && mIdelSlice != 1)
    exitError("Cannot do X axis tilt with non-consecutive slices");
  if (mNxWarp != 0 && mIdelSlice != 1)
    exitError("Cannot do local alignments with non-consecutive slices");
  if (mMinTotSlice > 0 && mIdelSlice != 1)
    exitError("Cannot do chunk writing with non-consecutive slices");
  if (nxFullIn == 0 && nyFull == 0 && (ixSubsetIn != 0 || iySubset != 0)) 
    exitError("You must enter the full image size if you have a subset");
  if (!mPerpendicular && mMinTotSlice > 0)
    exitError("Cannot do chunk writing with parallel slices");
  if (mNumReproj > 0 && nViewsReproj > 0)
    exitError("You cannot enter both views and angles to reproject");
  if (projModel && (mNumReproj > 0 || nViewsReproj > 0))
    exitError("You cannot do projection from a model with image reprojection");
  if (mNumSIRTiter > 0 && (mNumReproj > 0 || nViewsReproj > 0))
    exitError("You cannot do SIRT with entries for angles/views to reproject");
  //
  // Scale dimensions down by binning then report them
  //
  ixSubset = ixSubsetIn;
  nxFull = nxFullIn;
  if (imageBinned > 1) {
    if (ifSliceIn != 0 && (mMinTotSlice <= 0 || mIsliceStart > 0)) {
      mIsliceStart = B3DMAX(1, B3DMIN(mNyProj, (mIsliceStart + imageBinned - 1) /
                                      imageBinned));
      mIsliceEnd = B3DMAX(1, B3DMIN(mNyProj, (mIsliceEnd + imageBinned - 1) /
                                    imageBinned));
    }
    if (ifThickIn != 0)
      mIthickBP = mIthickBP / imageBinned;
    mAxisXoffset = mAxisXoffset / imageBinned;
    nxFull = (nxFullIn + imageBinned - 1) / imageBinned;
    nyFull = (nyFull + imageBinned - 1) / imageBinned;
    ixSubset = ixSubsetIn / imageBinned;
    iySubset = iySubset / imageBinned;
    xOffset = xOffset / imageBinned;
    mYOffset = mYOffset / imageBinned;
    if (ifWidthIn != 0)
      mIwidth = mIwidth / imageBinned;
    if (mMinTotSlice >= 0 && ! mRecReproj) {
      mMinTotSlice = B3DMAX(1, B3DMIN(mNyProj, (mMinTotSlice + imageBinned - 1) /
                                      imageBinned));
      mMaxTotSlice = B3DMAX(1, B3DMIN(mNyProj, (mMaxTotSlice + imageBinned - 1) /
                                      imageBinned));
    }
    if (mNumExactCycles > 0)
      mExactObjSize = mExactObjSize / imageBinned;
  }
  //
  // Set up an effective scaling factor for non-log scaling to test output
  // The equation in ( ) is based on fitting to the amplification of the range
  // with linear scaling for different input sizes
  mEffectiveScale = 1.;
  if (mIfLog == 0)
    mEffectiveScale = mOutScale * (0.011 * mNxProj + 6) / 2.;
  //
  if (mRecReproj) {
    if (mDebug)
      printf("%d %d %d %d %d %d %d\n", mMinTotSlice, mMaxTotSlice, mMinZreproj, 
             mMaxZreproj, nrecXyz[0], nrecXyz[1], nrecXyz[2]);
    if ((mMinTotSlice <= 0 && (mMinZreproj <= 0 || mMaxZreproj > nrecXyz[2])) ||
        (mMinTotSlice >= 0 && mMaxTotSlice > nrecXyz[2]))
      exitError("Min or Max Z coordinate to project is out of range");

    if (!mPerpendicular)
      exitError("Cannot reproject from reconstruction output with PARALLEL");
    if (mIdelSlice != 1)
      exitError("Cannot reproject from reconstruction with a slice increment");
    if (mIwidth != nrecXyz[0] || mIsliceEnd + 1 - mIsliceStart != nrecXyz[2] || 
        mIthickBP != nrecXyz[1])
      exitError("Dimensions of rec file do not match expected values");
  } else {
    //
    // Check conditions of SIRT (would slice increment work?)
    if (mNumSIRTiter > 0) {
      if (!mPerpendicular || mIdelSlice != 1 || xOffset != 0.)
        exitError("Cannot do SIRT with PARALLEL output, slice increment, or X shifts");
      if (mNxWarp != 0 || ifZfactors != 0 || mIfAlpha > 1 || 
          (mIfAlpha == 1 && mInterpOrdXtilt == 0)) 
          exitError("Cannot do SIRT with  local alignments, Z "
                    "factors, or variable or old-style X tilt");
      if (mIwidth != mNxProj || 
          (! mSirtFromZero && (mIwidth != nrecXyz[0] || nrecXyz[2] != mNyProj ||
                               (mIthickBP != nrecXyz[1] && !mVertSirtInput))))
        exitError( "For SIRT, sizes of input projections, rec file, and width/thickness"
                   " entries must match");
      printf("\n %d iterations of SIRT algorithm will be done\n", mNumSIRTiter);
      mInterpFacStretch = 0;
    }
    if (ifSliceIn != 0)
      printf("\n Rows %d to %d, (at intervals of %d) of the projection planes will "
             "be reconstructed.\n", mIsliceStart, mIsliceEnd, mIdelSlice);
    if (ifThickIn != 0)
      printf("\n Thickness of reconstructed slice is %d pixels.\n", mIthickBP);
    if (delAngle != 0. || mAxisXoffset != 0.)
      printf("\n Output map rotated by %.1f degrees about tilt axis"
             " with respect to tilt origin\n Tilt axis displaced by %.2f" 
             " pixels from centre of projection\n", delAngle, mAxisXoffset);
    if (nxFull != 0 || nyFull != 0)
      printf("\n Full aligned stack will be assumed to be %d by %d pixels\n",
             nxFull, nyFull);
    if (ifSubsetIn != 0)
      printf("\n Aligned stack will be assumed to be a subset starting at %d %d\n",
             ixSubset, iySubset);
    if (ifWidthIn != 0)
      printf("\n Width of reconstruction is %d pixels\n", mIwidth);
    if (xOffset != 0 || mYOffset != 0)
      printf("\n Output slice shifted up %.1f and to right %.1f pixels\n",
             mYOffset, xOffset);
    if (mMinTotSlice > 0)
      printf("\n Computed slices are part of a total volume from slice %d to %d\n",
             mMinTotSlice, mMaxTotSlice);
  }
  //
  // If NEWANGLES is 0, get angles from file header.  Otherwise check if angles OK
  //
  if (newAngles == 0 && ifTiltFile == 0) {
    //
    // Tilt information is stored in stack header. Read into angles
    // array. All sections are assumed to be equally spaced. If not,
    // you need to set things up differently. In such a case, great
    // care should be taken, since missing views may have severe
    // effects on the quality of the reconstruction.
    //
    //
    // call irtdat(1, idtype, lens, nd1, nd2, vd1, vd2)
    //
    if (idType != 1)
      exitError( "There are no tilt angles from ANGLES or "// 
        "TILTFILE entries or from the image file header");
    //
    if (nd1 != 2)
      exitError(" Tilt axis not along Y.");
    //
    delTheta = vd1;
    theta = vd2;
    //
    for (nv = 1; nv <= mNumViews; nv++) {
      mAngles[nv - 1] = theta;
      theta = theta + delTheta;
    }
    //
  } else {
    if (ifTiltFile == 1 && newAngles != 0) {
      exitError("Tried to enter angles with both ANGLES and TILTFILE");
    } else if (ifTiltFile == 1) {
      printf(" Tilt angles were entered from a tilt file\n");
    } else if (newAngles == mNumViews) {
      printf(" Tilt angles were entered with ANGLES card(s)\n");
    } else {
      exitError("If using ANGLES, a value must be entered for each view");
    }
  }
  //
  if (numCompress > 0) {
    if (numCompress == mNumViews) {
      printf(" Compression values were entered with COMPRESS card(s)\n");
    } else {
      exitError("If using COMPRESS, a value must be entered for each view");
    }
    for (nv = 0; nv < mNumViews; nv++)
      mCompress[nv] = 1. +(mCompress[nv] - 1.) / compFactor;
  }
  //
  if (globalAlpha != 0.) {
    for (iv = 0; iv < mNumViews; iv++)
      mAlpha[iv] = mAlpha[iv] - globalAlpha;
  }
  //
  if (ifExpWeight != 0) {
    if (mIfLog == 0) 
      printf(" Weighting factors were entered from a file\n");
    else
      printf(" Weighting factors were entered but will be ignored because log is "
             "being taken\n");
  }
  //
  // if no INCLUDE cards, set up map to views, excluding any specified by
  // EXCLUDE cards
  if (numViewUse == 0) {
    for (i = 1; i <= mNumViews; i++) {
      if (!numberInList(i, ivExclude, numViewExclude, 0))
        mMapUsedView[numViewUse++] = i;
    }
  }
  //
  // Replace angles at +/-90 with 89.95 etc
  for (i = 1; i <= numViewUse; i++) {
    j = mMapUsedView[i - 1];
    if (B3DABS(B3DABS(mAngles[j - 1]) - 90.) < 0.05) 
        mAngles[j - 1] = B3DSIGN(90. - B3DSIGN(0.05, 90 - B3DABS(mAngles[j - 1])), 
                                 mAngles[j - 1]);
  }
  //
  // If reprojecting from rec and no angles entered, copy angles in original
  // order
  if (mRecReproj && mNumReproj == 0) {
    if (nViewsReproj == 1 && ivReproj[0] == 0) {
      mNumReproj = mNumViews;
      for (i = 0; i < mNumViews; i++) {
        angReproj[i] = mAngles[i];
      }
    } else if (nViewsReproj > 0) {
      mNumReproj = nViewsReproj;
      for (i = 0; i < mNumReproj; i++) {
        if (ivReproj[i] < 1 || ivReproj[i] > mNumViews)
          exitError("View number to reproject is out of range");
        angReproj[i] = mAngles[ivReproj[i] - 1];
      }
    } else {
      //
      // For default set of included views, order them by view number by
      // first ordering the mapUsedView array by view number.
      rsSortInts(mMapUsedView, numViewUse);
      mNumReproj = numViewUse;
      for (i = 0; i < numViewUse; i++) {
        angReproj[i] = mAngles[mMapUsedView[i] - 1];
      }
    }
  }
  //
  // order the MAPUSE array by angle
  for (i = 0; i < numViewUse - 1; i++) {
    for (j = i + 1; j < numViewUse; j++) {
      if (mAngles[mMapUsedView[i] - 1] > mAngles[mMapUsedView[j] - 1]) {
        indi = mMapUsedView[i];
        mMapUsedView[i] = mMapUsedView[j];
        mMapUsedView[j] = indi;
      }
    }
  }
  //
  // For SIRT, now copy the angles in the ordered list; this is the order
  // in which the reprojections are needed internally
  // Also adjust the recon mean for a fill value
  if (mNumSIRTiter > 0) {
    mNumReproj = numViewUse;
    for (i = 0; i < numViewUse; i++) {
      angReproj[i] = mAngles[mMapUsedView[i] - 1];
    }
    mOutAdd = mOutAdd / mFilterScale;
    mOutScale = mOutScale * mFilterScale;
    mDmeanIn = mDmeanIn / mOutScale - mOutAdd;
  }
  if (mNumReproj > 0) {
    mCosReproj = B3DMALLOC(float, mNumReproj);
    mSinReproj = B3DMALLOC(float, mNumReproj);
    if (!mCosReproj || !mSinReproj)
      memoryError(NULL, "arrays for reprojection sines/cosines");
    for (i = 0; i < mNumReproj; i++) {
      mCosReproj[i] = cos(degToRad * angReproj[i]);
      mSinReproj[i] = sin(degToRad * angReproj[i]);
    }
  }

  //
  // Open output map file
  iiuRetDelta(1, delta);
  iiuRetOrigin(1, &originX, &originY, &originZ);
  if (! mRecReproj) {
    if ((mMinTotSlice <= 0 && (mIsliceStart < 1 || mIsliceEnd < 1)) 
        || mIsliceStart > mNyProj || mIsliceEnd > mNyProj) 
      exitError("Slice numbers out of range");
    numSlices = (mIsliceEnd - mIsliceStart) / mIdelSlice + 1;
    if (mMinTotSlice > 0 && mIsliceStart < 1)
        numSlices = mMaxTotSlice + 1 - mMinTotSlice;
    // print *,'NSLICE', minTotSlice, maxTotSlice, isliceStart, numSlices
    if (numSlices <= 0)
      exitError( "Slice numbers reversed");
    if (mReprojBP || mReadBaseRec) {
      mProjLine = B3DMALLOC(float, mIwidth);
      memoryError(mProjLine, "array for projection line");
    }
    if (defocusFile != NULL && pixForDefocus == 0.) 
      pixForDefocus = delta[0] / 10.;
    //
    // DNM 7/27/02: transfer pixel sizes depending on orientation of output
    //
    noutXyz[0] = mIwidth;
    cell[0] = mIwidth * delta[0];
    if (mPerpendicular) {
      noutXyz[1] = mIthickBP;
      noutXyz[2] = numSlices;
      cell[1] = mIthickBP * delta[0];
      cell[2] = numSlices * mIdelSlice * delta[1];
    } else {
      noutXyz[1] = numSlices;
      noutXyz[2] = mIthickBP;
      cell[2] = mIthickBP * delta[0];
      cell[1] = numSlices * mIdelSlice * delta[1];
    }
    if (mReprojBP) {
      noutXyz[1] = numSlices;
      noutXyz[2] = mNumReproj;
      cell[1] = numSlices * mIdelSlice * delta[1];
      cell[2] = delta[0] * mNumReproj;
      j = mIwidth * mNumReproj;
      mXRayStart = B3DMALLOC(float, j);
      mYRayStart = B3DMALLOC(float, j);
      mNumPixInRay = B3DMALLOC(int, j);
      mMaxRayPixels = B3DMALLOC(int, mNumReproj);
      if (!(mXRayStart && mYRayStart && mNumPixInRay && mMaxRayPixels))
        memoryError(NULL, "arrays for projection ray data");
      for (i = 0; i < mNumReproj; i++) {
        j = i * mIwidth;
        //
        // Note that this will set cosReproj to 0 after you carefully kept it from being 0
        set_projection_rays(&mSinReproj[i], &mCosReproj[i], &mIwidth, &mIthickBP, 
                            &mIwidth, &mXRayStart[j], &mYRayStart[j], &mNumPixInRay[j],
                            &mMaxRayPixels[i]);
      }
    }
  } else {
    //
    // recReproj stuff
    noutXyz[0] = mMaxXreproj + 1 - mMinXreproj;
    noutXyz[1] = mMaxZreproj + 1 - mMinZreproj;
    mIthickReproj = mMaxYreproj + 1 - mMinYreproj;
    noutXyz[2] = mNumReproj;
    if (mMinTotSlice > 0)
      noutXyz[1] = mMaxTotSlice + 1 - mMinTotSlice;
    if (noutXyz[0] < 1 || noutXyz[1] < 1 || mIthickReproj < 1) 
        exitError("Min and max limits for output are reversed for X, Y, or Z");
    cell[0] = noutXyz[0] * delta[0];
    cell[1] = noutXyz[1] * delta[0];
    cell[2] = delta[0] * mNumReproj;
    if (mProjSubtraction && (noutXyz[0] != mNxProj || mMaxZreproj > mNyProj ||
                             noutXyz[2] != mNumViews))
      exitError("Output size must match original projection file size for SIRT "
                "subtraction");
  }
  //
  // Check compatibility of base rec file
  if ((mReadBaseRec && ! mRecReproj) && 
      (nrecXyz[0] != mIwidth || nrecXyz[1] != mIthickBP || nrecXyz[2] < mIsliceEnd)) 
    exitError("Base rec file is not the same size as output file");
  //
  // Initialize parallel writing routines if bound file entered
  mParallelHDF = false;
  realChunkRun = mMinTotSlice > 0 && ((!mRecReproj && mIsliceStart > 0) ||
                                      (mRecReproj && mMinZreproj > 0));
  if (!projModel) {
    if (realChunkRun) {
      mParallelHDF = boundFile != NULL && iiTestIfHDF(outputFile) > 0;
    } else if (mMinTotSlice > 0) {
      mParallelHDF = b3dOutputFileType() == 5;
    }
  }
  ind1 = noutXyz[0];
  if (mParallelHDF)
    ind1 = -ind1;
  ierr = iiuParWrtInitialize(boundFile, 7, ind1, noutXyz[1], noutXyz[2]);
  if (ierr != 0)
    exitError("Initializing parallel write boundary file, error %d", ierr);
  //
  // open old file if in chunk mode and there is real starting slice
  // otherwise open new file
  //
  if (!projModel) {
    if (realChunkRun) {
      if (mParallelHDF) {
        parWrtProperties(&ind1, &ind2, &k);
        if (b3dLockFile(ind1) != 0) 
          exitError("Could not get lock for opening HDF file");
        ind2 = noutXyz[2];
      }
      iiuOpen(2, outputFile, "OLD");
      iiuRetBasicHead(2, noutXyz, mpxyz, &mNewMode, &dminTmp, &dmaxTmp, &dmeanTmp);
      if (mParallelHDF) 
        noutXyz[2] = ind2;
    } else {
      iiuOpen(2, outputFile, "NEW");
      iiuCreateHeader(2, noutXyz, noutXyz, mNewMode, mTitle, 0);
    }
    if (mParallelHDF && iiuFileType(2) != 5)  
      exitError("Expected output file to be an HDF file but it is not");
    iiuTransLabels(2, 1);
    iiuAltCell(2, cell);
  }
  //
  // if doing perpendicular slices, set up header info to make coordinates
  // congruent with those of tilt series
  //
  if (mRecReproj) {
    iiuAltOrigin(2, 0., 0., 0.);
    outHdrTilt[0] = 0.;
    iiuAltTilt(2, outHdrTilt);
  } else if (mPerpendicular) {
    outHdrTilt[0] = 90;
    if (adjustOrigin) {
      //
      // Full adjustment if requested
      originX = originX  - delta[0] * (mNxProj / 2 - mIwidth / 2 - xOffset);
      originZ = originY - delta[0] * float(B3DMAX(0, mIsliceStart - 1));
      if (mMinTotSlice > 0 && mIsliceStart <= 0)
          originZ = originY - delta[0] * (mMinTotSlice-1);
      originY = delta[0] * (mIthickBP / 2 + mYOffset);
    } else {
      //
      // Legacy origin.  All kinds of wrong.
      originX = cell[0] / 2. +mAxisXoffset;
      originY = cell[1] / 2.;
      originZ = -(float)B3DMAX(0, mIsliceStart - 1);
    }

    if (!projModel) {
      iiuAltOrigin(2, originX, originY, originZ);
      iiuAltTilt(2, outHdrTilt);
      iiuAltSpaceGroup(2, 1);
    }
  }
  //
  // chunk mode starter run: write header and exit
  //
  ifExit = 0;
  if (!projModel) {
    if (mMinTotSlice > 0 && !realChunkRun) {
      if (mParallelHDF) 
        iiuWriteDummySecToHDF(2);
      iiuWriteHeader(2, mTitle, 1, mDminIn, mDmaxIn, mDmeanIn);
      iiuClose(2);
      ifExit = 1;
    } else if (mMinTotSlice > 0) {
      if (! mRecReproj)
        parWrtPosn(2, mIsliceStart - mMinTotSlice, 0);
      if (mReadBaseRec && ! mRecReproj)
          iiuSetPosition(3, mIsliceStart - mMinTotSlice, 0);
      if (mParallelHDF)
        iiuParWrtRecloseHDF(2, 1);
    }
  }
  //
  // If reprojecting, need to look up each angle in full list of angles and
  // find ones to interpolate from, then pack data into arrays that are
  // otherwise used for packing these factors down
  // lookupAngle returns 0-based indices
  if (mRecReproj) {
    for (i = 0; i < mNumReproj; i++) {
      mSinBeta[i] = angReproj[i];
      lookupAngle(angReproj[i], mAngles, mNumViews, ind1, ind2, frac);
      mCosBeta[i] = (1. - frac) * mCompress[ind1] + frac * mCompress[ind2];
      mSinAlpha[i] = (1. - frac) * mAlpha[ind1] + frac * mAlpha[ind2];
      mCosAlpha[i] = (1. - frac) * mXZfac[ind1] + frac * mXZfac[ind2];
      tempArr[i] = (1. - frac) * mYZfac[ind1] + frac * mYZfac[ind2];
      tempArr[i + mNumViews] = (1. - frac) * mExposeWeight[ind1] + 
        frac * mExposeWeight[ind2];
    }
    //
    // Do the same thing with all the local data: pack it into the spot at
    // the top of the local data then copy it back into the local area
    if (mNxWarp > 0) {
      for (i = 0; i < mNxWarp * mNyWarp; i++) {
        for (iv = 0; iv < mNumReproj; iv++) {
          lookupAngle(angReproj[iv], mAngles, mNumViews, ind1, ind2, frac);
          ind1 = ind1 + mIndWarp[i];
          ind2 = ind2 + mIndWarp[i];
          mDelBeta[indBase + iv] = (1. -frac) * mDelBeta[ind1] + frac * mDelBeta[ind2];
          mDelAlpha[indBase + iv] = (1. -frac) * mDelAlpha[ind1] + frac * mDelAlpha[ind2];
          mWarpXZfac[indBase + iv] = (1. -frac) * mWarpXZfac[ind1] + 
            frac * mWarpXZfac[ind2];
          mWarpYZfac[indBase + iv] = (1. -frac) * mWarpYZfac[ind1] + 
            frac * mWarpYZfac[ind2];
          for (j = 0; j < 6; j++) {
            mFwarp[j + 6 * (indBase + iv)] = (1. -frac) * mFwarp[j + 6 * ind1] +
              frac * mFwarp[j + 6 * ind2];
          }
        }
        for (iv = 0; iv < mNumReproj; iv++) {
          ind1 = indBase + iv;
          ind2 = mIndWarp[i] + iv;
          mDelBeta[ind2] = mDelBeta[ind1];
          mDelAlpha[ind2] = mDelAlpha[ind1];
          mWarpXZfac[ind2] = mWarpXZfac[ind1];
          mWarpYZfac[ind2] = mWarpYZfac[ind1];
          for (j = 0; j < 6; j++) {
            mFwarp[j + 6 * ind2] = mFwarp[j + 6 * ind1];
          }
        }
      }
    }
    //
    // Replace the mapUsedView array
    numViewUse = mNumReproj;
    for (i = 1; i <= numViewUse; i++) {
      mMapUsedView[i - 1] = i;
    }
  } else {
    //
    // pack angles and other data down as specified by MAPUSE
    // Negate the z factors since things are upside down here
    // Note that local data is not packed but always referenced by mapUsedView
    //
    for (i = 0; i < numViewUse; i++) {
      mSinBeta[i] = mAngles[mMapUsedView[i] - 1];
      mCosBeta[i] = mCompress[mMapUsedView[i] - 1];
      mSinAlpha[i] = mAlpha[mMapUsedView[i] - 1];
      mCosAlpha[i] = mXZfac[mMapUsedView[i] - 1];
      tempArr[i] = mYZfac[mMapUsedView[i] - 1];
      tempArr[i + mNumViews] = mExposeWeight[mMapUsedView[i] - 1];
    }
  }
  for (i = 0; i < numViewUse; i++) {
    mAngles[i] = mSinBeta[i];
    mCompress[i] = mCosBeta[i];
    mAlpha[i] = mSinAlpha[i];
    mXZfac[i] = -mCosAlpha[i];
    mYZfac[i] = -tempArr[i];
    mExposeWeight[i] = tempArr[i + mNumViews];
  }
  numViewOrig = mNumViews;
  mNumViews = numViewUse;
  //
  printf("\n Projection angles:\n");
  for (nv = 0; nv < mNumViews; nv++) {
    printf("%9.2f", mAngles[nv]);
    if ((nv + 1) % 8 == 0 || nv == mNumViews - 1)
      printf("\n");
  }
  printf("\n");
  //
  // Turn off cosine stretch for high angles
  if (mAngles[0] < -80. || mAngles[mNumViews - 1] > 80.) {
    if (mInterpFacStretch > 0)
      printf("\n Tilt angles are too high to use cosine stretching\n");
    mInterpFacStretch = 0;
  }
  //
  // Set up trig tables -  Then convert angles to radians
  //
  mBetaMin = 10.;
  mBetaMax = -10.;
  mMinCosAlpha = 10.;
  for (iv = 0; iv < mNumViews; iv++) {
    thetaView = mAngles[iv] + delAngle;
    if (thetaView > 180.)
      thetaView = thetaView - 360.;
    if (thetaView <= -180.)
      thetaView = thetaView + 360.;
    mCosBeta[iv] = cos(thetaView * degToRad);
    //
    // Keep cosine from going to zero so it can be divided by
    if (B3DABS(mCosBeta[iv]) < 1.e-6) 
      mCosBeta[iv] = B3DSIGN(1.e-6, mCosBeta[iv]);
    //
    // Take the negative of the sine of tilt angle to account for the fact
    // that all equations are written for rotations in the X/Z plane,
    // viewed from  the negative Y axis
    // Take the negative of alpha because the entered value is the amount
    // that the specimen is tilted and we need to rotate by negative of that
    mSinBeta[iv] = -sin(thetaView * degToRad);
    mCosAlpha[iv] = cos(mAlpha[iv] * degToRad);
    mSinAlpha[iv] = -sin(mAlpha[iv] * degToRad);
    mAngles[iv] = -degToRad * (mAngles[iv] + delAngle);
    ACCUM_MIN(mBetaMin, mAngles[iv]);
    ACCUM_MAX(mBetaMax, mAngles[iv]);
    ACCUM_MIN(mMinCosAlpha, mCosAlpha[iv]);
  }
  //
  // If there are weighting angles, convert those the same way, otherwise
  // copy the main angles to weighting angles
  if (mNumWgtAngles > 0) {
    for (iv = 0; iv < mNumWgtAngles; iv++) {
      mWgtAngles[iv] = -degToRad * (mWgtAngles[iv] + delAngle);
    }
  } else {
    mNumWgtAngles = mNumViews;
    for (iv = 0; iv < mNumViews; iv++) {
      mWgtAngles[iv] = mAngles[iv];
    }
  }
  //
  // if fixed x axis tilt, set up to try to compute vertical planes
  // and interpolate output planes: adjust thickness that needs to
  // be computed, and find number of vertical planes that are needed
  //
  if (ifZfactors > 0 && mIfAlpha == 0)
    mIfAlpha = 1;
  mIthickOut = mIthickBP;
  mYcenModProj = mIthickBP / 2 + 0.5 + mYOffset;
  mNumReadNeed = 0;
  if (mIfAlpha == 1 && mNxWarp == 0 && mInterpOrdXtilt > 0 && ifZfactors == 0 &&
      !mRecReproj) {
    mIfAlpha = -1;
    mIthickOut = mIthickBP;
    mIthickBP = mIthickBP / mCosAlpha[0] + 4.5;
    mNumVertNeeded = mIthickOut * B3DABS(mSinAlpha[0]) + 5.;
    if (mNumSIRTiter > 0) {
      mSaveVertSlices = vertOutFile != NULL;
      if (!mSirtFromZero && !mVertSirtInput)
        mNumReadNeed = mIthickBP * B3DABS(mSinAlpha[0]) + 4.;
      if (!mSirtFromZero && mVertSirtInput && mIthickBP != nrecXyz[1]) 
        exitError("Thickness of vertical slice input file does not match needed "
                  "thickness");
    }
  }
  if (mIfAlpha != -1 && mNumSIRTiter > 0 && (mVertSirtInput || vertOutFile))
    exitError("VertForSIRTInput or VertSliceOutputFile "
              "cannot be entered unless vertical slices are being computed");
  //
  // Now that we know vertical slices, open or set up output file for them under SIRT
  if (mNumSIRTiter > 0 && mSaveVertSlices) {
    //
    // Initialize parallel writing if vertical bound file
    for (i = 0; i < 3; i++)
      nvsXyz[i] = noutXyz[i];
    nvsXyz[1] = mIthickBP;
    if (mParallelHDF && mMinTotSlice > 0 && mIsliceStart > 0 && !vertBoundFile)  
      exitError("A boundary file for vertical slices must be entered if there "
                "is one for primary output and output file type is HDF");
    ind1 = nvsXyz[0];
    if (mParallelHDF)
      ind1 = -ind1;
    ierr = iiuParWrtInitialize(vertBoundFile, 8, ind1, nvsXyz[1], nvsXyz[2]);
    if (ierr != 0)
      exitError("Initializing parallel write boundary file for vertical slices, error",
                ierr);
    if (mMinTotSlice > 0 && mIsliceStart > 0) {
      if (mParallelHDF) {
        parWrtProperties(&ind1, &ind2, &k);
        if (b3dLockFile(ind1) != 0)  
            exitError("Could not get lock for opening HDF file");
        ind2 = nvsXyz[2];
      }
      iiuOpen(6, vertOutFile, "OLD");
      iiuRetBasicHead(6, nvsXyz, mpxyz, &mNewMode, &dminTmp, &dmaxTmp, &dmeanTmp);
      if (mParallelHDF) 
        nvsXyz[2] = ind2;
    } else {
      iiuOpen(6, vertOutFile, "NEW");
      iiuCreateHeader(6, nvsXyz, nvsXyz, 2, mTitle, 0);
    }
    if (mParallelHDF && iiuFileType(6) != 5)  
      exitError("Expected vertical slice file to be an HDF file but it is not");
    //
    // chunk mode: either write header, or set up to write correct location
    if (ifExit != 0) {
      if (mParallelHDF) 
        iiuWriteDummySecToHDF(6);
      iiuWriteHeader(6, mTitle, 0, mDminIn, mDmaxIn, mDmeanIn);
      iiuClose(6);
    } else if (mMinTotSlice > 0) {
      parWrtPosn(6, mIsliceStart - mMinTotSlice, 0);
      if (mParallelHDF)
        iiuParWrtRecloseHDF(6, 1);
    }
    ierr = parWrtSetCurrent(0);
  }

  if (ifExit != 0) {
    printf("Exiting after setting up output file for chunk writing\n");
    exit(0);
  }
  //
  // Set center of output plane and center of input for transformations
  // Allow the full size to be less than the aligned stack, with a negative
  // subset start
  //
  if (nxFull == 0) {
    nxFull = mNxProj;
    nxFullIn = mNxProj * imageBinned;
  }
  if (nyFull == 0)
    nyFull = mNyProj;
  mXcenIn = nxFull / 2. + 0.5 - ixSubset;
  mCenterSlice = nyFull / 2. + 0.5 - iySubset;
  xoffAdj = xOffset - ((mNxProj * imageBinned - nxFullIn) / 2 + ixSubsetIn) /
    (float)imageBinned;
  mXcenOut = mIwidth / 2 + 0.5 + mAxisXoffset + xoffAdj;
  mYcenOut = mIthickBP / 2 + 0.5 + mYOffset;
  if (mNumSIRTiter > 0 && B3DABS(xoffAdj + mAxisXoffset) > 0.1) {
    if (mNxProj % 2 > 0)
      exitError("Cannot do internal SIRT with an odd input size in X");
    exitError("Cannot do internal SIRT with a tilt axis offset from center of input "
              "images");
  }
  //
  // if doing warping, convert the angles to radians and set sign
  // Also cancel the z factors if global entry was not made
  //
  if (mNxWarp > 0) {
    for (i = 0; i < numViewOrig * mNxWarp * mNyWarp; i++) {
      mDelBeta[i] = -degToRad * mDelBeta[i];
    }
    for (iv = 0; iv < mNumViews; iv++) {
      for (i = 0; i < mNxWarp * mNyWarp; i++) {
        ind = mIndWarp[i] + mMapUsedView[iv] - 1;
        mCWarpBeta[ind] = cos(mAngles[iv] + mDelBeta[ind]);
        mSWarpBeta[ind] = sin(mAngles[iv] + mDelBeta[ind]);
        mCWarpAlpha[ind] = cos(degToRad * (mAlpha[iv] + mDelAlpha[ind]));
        mSWarpAlpha[ind] = -sin(degToRad * (mAlpha[iv] + mDelAlpha[ind]));
        if (ifZfactors == 0) {
          mWarpXZfac[ind] = 0.;
          mWarpYZfac[ind] = 0.;
        }
      }
    }
    //
    // See if local scale was entered; if not see if it can be set from
    // pixel size and local align pixel size
    if (scaleLocal <= 0.) {
      scaleLocal = 1.;
      if (pixelLocal > 0) {
        scaleLocal = pixelLocal / delta[0];
        if (B3DABS(scaleLocal - 1.) > 0.001) 
          printf("\nScaling of local alignments by %.3f determined from pixel sizes\n",
                 scaleLocal);
      }
    }
    //
    // scale the x and y dimensions and shifts if aligned data were
    // shrunk relative to the local alignment solution
    // 10/16/04: fixed to use mapUsedView to scale used views properly
    //
    if (scaleLocal != 1.) {
      mIxStartWarp = B3DNINT(mIxStartWarp * scaleLocal);
      mIyStartWarp = B3DNINT(mIyStartWarp * scaleLocal);
      mIdelXwarp = B3DNINT(mIdelXwarp * scaleLocal);
      mIdelYwarp = B3DNINT(mIdelYwarp * scaleLocal);
      for (iv = 0; iv < mNumViews; iv++) {
        for (i = 0; i < mNxWarp * mNyWarp; i++) {
          ind = mIndWarp[i] + mMapUsedView[iv] - 1;
          mFwarp[4 + 6 * ind] *= scaleLocal;
          mFwarp[5 + 6 * ind] *= scaleLocal;
        }
      }
    }
    //
    // if the input data is a subset in X or Y, subtract starting
    // coordinates from ixStartWarp and iyStartWarp
    //
    mIxStartWarp = mIxStartWarp - ixSubset;
    mIyStartWarp = mIyStartWarp - iySubset;
  }
  //
  // Done with array in its small form and with angReproj
  B3DFREE(tempArr);
  free(angReproj);
  free(ivExclude);
  free(ivReproj);
  //
  // Here is the place to project model points and exit
  if (projModel)
    projectModel(imodToProject, outputFile, angleOutput, transformFile, 
                 numViewOrig, defocusFile, pixForDefocus, focusInvert);
  //
  // If reprojecting, set the pointers and return
  if (mRecReproj) {
    mMinXload = mMinXreproj;
    mMaxXload = mMaxXreproj;
    if (mNxWarp != 0) {
      mMinXload = B3DMAX(1, mMinXload - 100);
      mMaxXload = B3DMIN(nrecXyz[0], mMaxXload + 100);
    }
    iwideReproj = mMaxXload + 1 - mMinXload;
    mIsliceSizeBP = iwideReproj * mIthickReproj;
    mInPlaneSize = mIsliceSizeBP;
    if (mNxWarp != 0) {
      mDxWarpDelz = mIdelXwarp / 2.;
      mNumWarpDelz = B3DMAX(2., (iwideReproj - 1) / mDxWarpDelz) + 1;
      mDxWarpDelz = (iwideReproj - 1.) / (mNumWarpDelz - 1.);
    }
    //
    // Get projection offsets for getting from coordinate in reprojection
    // to coordinate in original projections.  The X coordinate must account
    // for the original offset in building the reconstruction plus any
    // additional offset.  But the line number here is the line # in the
    // reconstruction so we only need to adjust Y by the original starting
    // line.  Also replace the slice limits.
    mXprojOffset = mMinXreproj - 1 + mNxProj / 2 - mIwidth / 2 - xOffset;
    mYprojOffset = mIsliceStart - 1;
    mIsliceStart = mMinZreproj;
    mIsliceEnd = mMaxZreproj;
    mIwidth = noutXyz[0];
    mNyProj = nrecXyz[2];
    mDmeanIn = (mDmeanIn / mOutScale - mOutAdd) / mFilterScale;
    if (mThreshPolarity != 0.) {
      mThreshForReproj = (mThreshForReproj / mOutScale - mOutAdd) / mFilterScale;
      mThreshMarkVal = mThreshForReproj;
      mThreshFillVal = mDmeanIn;
      if (mThreshSumFac < 1.)
        mThreshMarkVal = mThreshMarkVal * mIthickReproj;
    }
    mIpExtraSize = 0;
    mNumPad = 0;
    if (mDebug)
      printf("scale: %f %f", mOutScale, mOutAdd);

    numNeedEval = B3DMIN(numNeedEval, mIsliceEnd + 1 - mIsliceStart);
    setNeededSlices(maxNeeds, numNeedEval);
    if (allocateArray(maxNeeds, numNeedEval, 1, minMemory) == 0) 
      exitError("The main array cannot be allocated large enough"
                " to reproject a single Y value");
    
    mReprojLines = B3DMALLOC(float, mIwidth * mNumPlanes);
    memoryError(mReprojLines, "array for reprojected lines");
    //
    if (mProjSubtraction) {
      mOrigLines = B3DMALLOC(float, mIwidth * mNumPlanes);
      memoryError(mOrigLines, "array for original lines");
    }
    if (mUseGPU) {
      ind = 0;
      if (mDebug)
        ind = 1;
      mUseGPU = gpuAvailable(indGPU, gpuMemory, maxTex2D, maxTexLayer, maxTex3D, ind) 
        != 0;
      if (! mUseGPU)
        mGpuErrString = "No GPU is available";
      if (! mUseGPU && ! mDebug)
        mGpuErrString += ", run gputilttest for more details";
      ind = maxNeeds[0] * mInPlaneSize + mIwidth * mNumPlanes;
      iex = mIwidth * mNumPlanes;
      kti = 0;
      if (mUseGPU && mNxWarp > 0) {
        ind = ind + maxNeeds[0] * (8 * mIwidth + mNumWarpDelz) +
          12 * mNumWarpPos * mNumViews;
        iex = iex + 12 * mNumWarpPos * mNumViews;
        kti = mNumWarpDelz;
        packLocal = packLocalData();
      }
      if (mUseGPU) {
        mUseGPU = 4 * ind <= gpuMemoryFrac * gpuMemory;
        if (! mUseGPU)
          mGpuErrString = "GPU is available but it has insufficient "
            "memory to reproject with current parameters";
      }
      if (mUseGPU)
         allocateGpuPlanes(iex, mNxWarp * mNyWarp, kti, 0, mNumPlanes, iwideReproj,
                           mIthickReproj, maxNeeds, ifZfactors, gpuMemory, gpuMemoryFrac,
                           if3Dtexture, maxTex2D, maxTex3D, maxTexLayer);
      if (mUseGPU && mNxWarp != 0) {
        mUseGPU = gpuLoadLocals(packLocal, mNxWarp * mNyWarp) == 0;
        if (! mUseGPU)
          mGpuErrString = "Failed to load local alignment data into GPU";
      }
      if (mUseGPU)
        printf("Using the GPU for reprojection\n");
      free(packLocal);
      warnOrExitIfNoGPU();
      if (! mUseGPU)
        printf("The GPU cannot be used, using the CPU for reprojection\n");
    }
    //
    // Finally allocate the warpDelz now that number of lines is known,
    // and projecton factors now that number of planes is known
    if (mNxWarp != 0) {
      kti = iwideReproj * mNumPlanes;
      mWarpDelz = B3DMALLOC(float, mNumWarpDelz * B3DMAX(1, mNumGpuPlanes));
      mXprojFs = B3DMALLOC(float, kti);
      mXprojZs = B3DMALLOC(float, kti);
      mYprojFs = B3DMALLOC(float, kti);
      mYprojZs = B3DMALLOC(float, kti);
      if (!(mWarpDelz && mXprojFs && mXprojZs && mYprojFs && mYprojZs))
        memoryError(NULL, "local projection factor or warpdelz arrays");
    }
    return;
  }
  //
  // BACKPROJECTION ONLY.  First allocate the projection factor array
  if (mNxWarp != 0) {
    kti = mIwidth * mSuperSampleFac;
    mXprojFs = B3DMALLOC(float, kti);
    mXprojZs = B3DMALLOC(float, kti);
    mYprojFs = B3DMALLOC(float, kti);
    mYprojZs = B3DMALLOC(float, kti);
    if (!(mXprojFs && mXprojZs && mYprojFs && mYprojZs))
      memoryError(NULL, "arrays for local projection factors");
  }
  //
  // If reading base, figure out total views being added and adjust scales
  if (mReadBaseRec && ! mRecSubtraction) {
    iv = mNumViews;
    for (j = 0; j < mNumViews; j++) {
      k = 0;
      for (i = 0; i < mNumViewSubtract; i++) {
        if (mIviewSubtract[i] == 0 || mMapUsedView[j] == mIviewSubtract[i])
          k = 1;
      }
      iv = iv - 2 * k;
    }
    mBaseOutScale = mOutScale / mNumViewBase;
    mBaseOutAdd = mOutAdd * mNumViewBase;
    mOutScale = mOutScale / (iv + mNumViewBase);
    mOutAdd = mOutAdd * (iv + mNumViewBase);
    if (mDebug)
      printf("base: %f  %f\n", mBaseOutScale, mBaseOutAdd);
  } else if (mNumSIRTiter <= 0) {
    mOutScale = mOutScale / mNumViews;
    mOutAdd = mOutAdd * mNumViews;
  }
  if (mDebug)
    printf("scale: %f %f\n", mOutScale, mOutAdd);
  numNeedEval = B3DMIN(numNeedEval, mIsliceEnd + 1 - mIsliceStart);
  setNeededSlices(maxNeeds, numNeedEval);
  if (mDebug) {
    for (i = 0; i < numNeedEval; i++)
      printf("  %d", maxNeeds[i]);
    printf("\n");
  }
  if (mIterForReport > 0) {
    kti = 3 * B3DMAX(1, mNumSIRTiter);
    mReportVals = B3DMALLOC(float, kti);
    memoryError(mReportVals, "report value array");
    memset(mReportVals, 0, kti * sizeof(float));
  }
  //
  // 12/13/09: removed fast backprojection code
  //
  // Set up padding: 10% of X size or minimum of 16, max of 50
  numPadTmp = B3DMIN(50, 2 * B3DMAX(8, mNxProj / 20));
  // npadtmp = 2 * nprojXyz[0]
  nprojPad = niceFrame(2 * ((mNxProj + numPadTmp) / 2), 2, niceFFTlimit());
  mNumPad = nprojPad - mNxProj;
  //
  // Set up defaults for plane size and start of planes of input data
  mIsliceSizeBP = mIwidth * mIthickBP;
  mIpExtraSize = 0;
  nxProjPad = mNxProj + 2 + mNumPad;
  mInPlaneSize = nxProjPad * mNumViews;
  mNeedForFiltArr = mInPlaneSize;
  mNeedForOutArr = mIsliceSizeBP;
  setupSizesForSupersampling();
  if (mNumSIRTiter > 0) {
    if (mSirtFromZero) {
      mNeedForFiltArr *= 2;
    }
    mNeedForReadInArr = mIsliceSizeBP;
    mNeedForWorkArr = mInPlaneSize;
    nvsXyz[0] = mIwidth;
    nvsXyz[1] = mIsliceEnd + 1 - mIsliceStart;
    nvsXyz[2] = mNumViews;
    if (mIfOutSirtProj > 0) {
      iiuOpen(4, "sirttst.prj", "NEW");
      iiuCreateHeader(4, nvsXyz, nvsXyz, 2, mTitle, 0);
      iiuWriteHeader(4, mTitle, 0, -1.e6, 1.e6, 0.);
    }
    nvsXyz[1] = mIthickBP;
    nvsXyz[2] = mIsliceEnd + 1 - mIsliceStart;
    if (mIfOutSirtRec > 0) {
      iiuOpen(5, "sirttst.drec", "NEW");
      iiuCreateHeader(5, nvsXyz, nvsXyz, 2, mTitle, 0);
      iiuWriteHeader(5, mTitle, 0, -1.e6, 1.e6, 0.);
    }
  }
  mMaxStack = 0;
  //
  // Determine if GPU can be used, but don't try to allocate yet
  if (mUseGPU) {
    ind = 0;
    if (mDebug)
      ind = 1;
    wallStart = wallTime();
    mUseGPU = gpuAvailable(indGPU, gpuMemory, maxTex2D, maxTexLayer, maxTex3D, ind) != 0;
    if (mDebug)
      printf("Time to test if GPU available: %.4f\n", wallTime() - wallStart);
    if (! mUseGPU)
      mGpuErrString = "No GPU is available";
    if (! mUseGPU && ! mDebug)
      mGpuErrString += ", run gputilttest for more details";
    if (mUseGPU) {
      //
      // Basic need is input planes for reconstructing one slice plus 2
      // slices for radial filter and planes being filtered, plus output
      // slice. Local alignment adds 4 arrays for local proj factors
      iv = (maxNeeds[0] + 2) * mInPlaneSize + mIsliceSizeBP;
      if (mNxWarp != 0)
        iv = iv + 4 * (mIwidth * mNumViews + 12 * mNumWarpPos * mNumViews);
      if (mNumSIRTiter > 0)
        iv = iv + mIsliceSizeBP + mInPlaneSize;
      if (mSirtFromZero)
        iv = iv + mInPlaneSize;
      mUseGPU = 4 * iv <= gpuMemoryFrac * gpuMemory;
      if (mUseGPU) {
        mInterpFacStretch = 0;
      } else {
        mGpuErrString = "GPU is available but it has insufficient "
            "memory to backproject with current parameters";
      }
    }
    warnOrExitIfNoGPU();
    if (! mUseGPU)
      printf("The GPU cannot be used; using CPU for backprojection\n");
  }
  //
  // next evaluate cosine stretch and new-style tilt for memory
  //
  if (mIfAlpha < 0) {
    //
    // new-style X-axis tilt
    //
    mNeedForVertArr = mIsliceSizeBP * mNumVertNeeded;
    if (mNumSIRTiter > 0) {
      mNeedForReadInArr = (b3dInt64)mNumReadNeed * mIthickOut * mIwidth;
      mNeedForWorkArr = mInPlaneSize;
    }
    //
    // find out what cosine stretch adds if called for  (not allowed if SIRT)
    //
    if (mInterpFacStretch > 0) {
      setCosStretch();
      mInPlaneSize = B3DMAX(mInPlaneSize, mIndStretchLine[mNumViews]);
      mIpExtraSize = mInPlaneSize;
      mNeedForFiltArr = mInPlaneSize;
    }
    //
    // Does everything fit?  If not, drop back to old style tilting
    //
    if (allocateArray(maxNeeds, numNeedEval, 4, minMemory) == 0) {
      if (mNumSIRTiter > 0)
        exitError("Allocating arrays needed for in-memory SIRT iterations");
      mIfAlpha = 1;
      mIthickBP = mIthickOut;
      mIsliceSizeBP = mIwidth * mIthickBP;
      mYcenOut = mIthickBP / 2 + 0.5 + mYOffset;
      mIpExtraSize = 0;
      mInPlaneSize = nxProjPad * mNumViews;
      mNeedForFiltArr = mInPlaneSize;
      mNeedForOutArr = mIsliceSizeBP;
      mNeedForVertArr = 0;
      setupSizesForSupersampling();
      printf("\n Failed to allocate an array big enough to use new-style X-axis "
             "tilting\n");
      setNeededSlices(maxNeeds, numNeedEval);
    }
  }
  //
  // If not allocated yet and not warping, try cosine stretch here
  //
  if (mMaxStack == 0 && mNxWarp == 0 && mInterpFacStretch > 0) {
    setCosStretch();
    //
    // set size of plane as max of loading size and stretched size
    // also set that an extra plane is needed
    // if there is not enough space for the planes needed, then
    // disable stretching and drop back to regular code
    //
    mInPlaneSize = B3DMAX(mInPlaneSize, mIndStretchLine[mNumViews]);
    mIpExtraSize = mInPlaneSize;
    mNeedForFiltArr = (mSirtFromZero ? 2 : 1) * mInPlaneSize;
    if (allocateArray(maxNeeds, numNeedEval, 1, minMemory) == 0) {
      mIpExtraSize = 0;
      mInPlaneSize = nxProjPad * mNumViews;
      mNeedForFiltArr = (mSirtFromZero ? 2 : 1) * mInPlaneSize;
      mInterpFacStretch = 0;
      printf("\n Failed to allocate an array big enough to use cosine stretching\n");
    }
  }
  //
  // If array still not allocated (failure, or local alignments), do it now
  if (mMaxStack == 0) {
    if (allocateArray(maxNeeds, numNeedEval, 1, minMemory) == 0) 
      exitError("Could not allocate main array large enough to " 
                "reconstruct a single slice");
  }
  if (mSuperSampleFac > 1) {
    mSuperTempArr = B3DMALLOC(float, mNxOutPad * mSuperSampleFac + 2);
    memoryError(mSuperTempArr, "temporary array for reducing super-res slice");
    if (mProjSuperFac > 1) {

      // Set up the phase shift for the supersampled line and incorporate scaling
      mPhaseReal = B3DMALLOC(float, nxProjPad / 2);
      mPhaseImag = B3DMALLOC(float, nxProjPad / 2);
      if (!mPhaseReal || !mPhaseImag)
        exitError(NULL, "arrays for phase shift when expanding input lines");
      dxLine = (mProjSuperFac - 1.) / (2. * mProjSuperFac);
      for (ind = 0; ind < nxProjPad / 2; ind++) {
        freq = 0.5 * ind / (nxProjPad / 2 - 1.);
        arg = -2. * pi * freq * dxLine;
        mPhaseReal[ind] = cos(arg) * sqrt((double)mProjSuperFac);
        mPhaseImag[ind] = sin(arg) * sqrt((double)mProjSuperFac);
      }
    }
  }
  //
  // Set up radial weighting
  if (mNumSIRTiter > 0 && !mSirtFromZero)
    mFlatFrac = 2.;
  radialWeights(iradMax, radFall, 1, ifMultByGaussian);
  if (mSirtFromZero) {
    frac = mZeroWeight;
    mFlatFrac = 2.;
    radialWeights(iradMax, radFall, 2, ifMultByGaussian);
    mZeroWeight = frac;
  }
  //
  // If Using GPU, make sure memory is OK now and allocate and load things
  // Add up "non-plane" memory needed there: output slice, filters and FFT array
  ind = mIsliceSizeBP + 2 * mNeedForFiltArr;
  if (mUseGPU && mNxWarp != 0) {
    ind = ind + 4 * mNumViews * mIwidth + 12 * mNumWarpPos * mNumViews;
    packLocal = packLocalData();
  }

  // Add supersample needs: two super-sized and another regular size, plus large FFT
  // for input if expanding
  if (mSuperSampleFac > 1) {
    ind += (2 * mSuperSampleFac * mSuperSampleFac + 1) * mIsliceSizeBP;
    if (mProjSuperFac > 1)
      ind += mInPlaneSize;
  }
  wallStart = wallTime();
  if (mUseGPU) {
    if (mIfAlpha <= 0 && mNxWarp == 0) {
      iv = 0;
      j = 1;
      if (mNumSIRTiter > 0)
        iv = mNumViews;
      if (mSirtFromZero)
        j = 2;
      mUseGPU = gpuAllocArrays(mIwidth, mIthickBP, nxProjPad, mNumViews, 1, mNumViews, 
                               0, 0, j, iv, mSuperSampleFac * (mProjSuperFac > 1 ? -1 : 1)
                               , mNxOutPad, mNyOutPad, mCleanSuperFFT, 1, 1, 0) == 0;
    } else {
      allocateGpuPlanes(ind, mNxWarp * mNyWarp, 0, 1, mIthickBP, nxProjPad, mNumViews,
                        maxNeeds, ifZfactors, gpuMemory, gpuMemoryFrac, if3Dtexture, 
                        maxTex2D, maxTex3D, maxTexLayer);
    }
    if (mDebug && mUseGPU)
      printf("Time to allocate on GPU: %.4f\n", wallTime() - wallStart);

    // print *,useGPU
    wallStart = wallTime();
    if (mUseGPU) {
      mUseGPU = gpuLoadFilter(mFilterArray) == 0;
      if (! mUseGPU)
        mGpuErrString = "Failed to load filter array into GPU";
    }
    // print *,useGPU
    if (mUseGPU && mNxWarp != 0) {
      mUseGPU = gpuLoadLocals(packLocal, mNxWarp * mNyWarp) == 0;
      if (! mUseGPU)
        mGpuErrString = "Failed to load local alignment data into GPU";
    }
    // print *,useGPU
    if (mDebug && mUseGPU)
      printf("Time to load filter/locals on GPU: %.4f\n", wallTime() - wallStart);
    free(packLocal);
    warnOrExitIfNoGPU();
    if (mUseGPU) {
      printf("Using GPU for backprojection\n");
    } else {
      printf("The GPU cannot be used - using CPU for backprojection\n");
    }
  }

  // Allocate maskEdges array
  if (! mRecReproj) {
    mIxUnmaskedSE = B3DMALLOC(int, 2 * mIthickBP);
    memoryError(mIxUnmaskedSE, "mask array");
  }
  
}

//
// Allocate as many planes as possible on the GPU up to the number
// allowed in ARRAY; nonPlane gives the number of floats needed for
// other data and numWarps is number of local positions, numDelz is
// size of warpDelz array, numfilts is number of filter
// filter arrays needed, nygout is size of output in y, nxGPlane and
// nxyplane are size of input data
//
void Tilt::allocateGpuPlanes(int nonPlane, int numWarps, int numDelz, int numFilts,
                             int nygout, int nxGPlane, int nyGPlane, int *maxNeeds,
                             int ifZfactors, float gpuMemory, float gpuMemoryFrac,
                             int &if3Dtexture, int *maxTex2D, int *maxTex3D, 
                             int *maxTexLayer)
{
  int i, maxGpuPlane, iperPlane, indEval3D;

  // Evaluate texture types in this order: 3D, layered, 2D.
  // But for no X tilt/Z-factors or locals, only 2D textures are allowed
  // And for no locals or no reprojection, only 2D layers or 2D textures are allowed
  // (Unimplemented options had no advantage)
  if (mIfAlpha <= 0 && ifZfactors == 0 && mNxWarp == 0) {
    indEval3D = 2;
  } else if (mNxWarp == 0 && ! mRecReproj) {
    indEval3D = 1;
  } else {
    indEval3D = 0;
  }
  if (mDebug) {
    printf("In allocateGpuPlanes: %d %d %d %d %d\n", mIfAlpha, ifZfactors, mNxWarp,
           mRecReproj,indEval3D);
    printf("%d %d %d %d %d %d \n",nxGPlane, maxTex3D[0], nyGPlane, maxTex3D[1],
           maxNeeds[0], maxTex3D[2]);
  }
  //
  // Check that the test entry was not for an unimplemented type
  if ((indEval3D > 0 && if3Dtexture > 0) || 
      (indEval3D > 1 && if3Dtexture > -999 && if3Dtexture != 0)) 
    exitError("Entry for TextureType not legal with current type of computation");
  //
  // If 3D is allowed and fits, take it; then if Layered is allowed and fits, take it
  // then fall back to 2D
  if (if3Dtexture == -999 && indEval3D == 0 && mProjSuperFac * nxGPlane < maxTex3D[0] && 
      nyGPlane < maxTex3D[1] && maxNeeds[0] < maxTex3D[2]) 
    if3Dtexture = 1;
  if (if3Dtexture == -999 && indEval3D <= 1 && mProjSuperFac * nxGPlane < maxTexLayer[0]
      && nyGPlane < maxTexLayer[1] && maxNeeds[0] < maxTexLayer[2]) 
    if3Dtexture = -1;
  if (if3Dtexture == -999)
    if3Dtexture = 0;
  if (mDebug)
    printf("Proceeding to allocate on GPU with texture type %d\n", if3Dtexture);

  //
  // Start with as many planes as possible but no more than in array
  // and if doing 2D textures, no more lines for array on GPU than the Y limit for
  // 2D textures (32767 before Fermi, typically 65536)
  iperPlane = mInPlaneSize;
  if (numDelz > 0)
    iperPlane = mInPlaneSize + 8 * mIwidth + mNumWarpDelz;
  maxGpuPlane = (gpuMemoryFrac * gpuMemory / 4. - nonPlane) / iperPlane;
  if (maxGpuPlane < maxNeeds[0]) {
    sprintf(mLine, "The GPU only has enough memory to load %d planes of data and,"
            " with current parameters, up to %d input planes are required to "
            "reconstruct a single plane\n", maxGpuPlane, maxNeeds[0]);
    mGpuErrString = mLine;
  } else if (if3Dtexture == 0 && maxTex2D[1] / nyGPlane < maxNeeds[0]) {
    sprintf(mLine, "With current parameters, up to %d input planes are required to "
            "reconstruct a single plane, while the allowed number of lines in GPU "
            "texture memory will fit only %d input planes\n", maxNeeds[0], 
            maxTex2D[1] / nyGPlane);
    mGpuErrString = mLine;
  }
  //
  // Limit maximum planes by the type allowed for the texture
  if (if3Dtexture > 0) {
    maxGpuPlane = b3dIMin(3, maxGpuPlane, mNumPlanes, maxTex3D[2]);
  } else if (if3Dtexture < 0) {
    maxGpuPlane = b3dIMin(3, maxGpuPlane, mNumPlanes, maxTexLayer[2]);
  } else {
    maxGpuPlane = b3dIMin(3, maxGpuPlane, mNumPlanes, maxTex2D[1] / nyGPlane);
  }

  // FOR TESTING MEMORY SHIFTING ETC
  // ind = max(maxNeeds(1), min(ind, numPlanes / 3))
  mNumGpuPlanes = 0;
  // print *, ind, numPlanes, maxNeeds(1), maxGpuPlane
  for (i = maxGpuPlane; i >= maxNeeds[0]; i--) {
    if (gpuAllocArrays(mIwidth, nygout, nxGPlane, nyGPlane, i, mNumViews, numWarps,
                       numDelz, numFilts, 0,
                       mSuperSampleFac * (mProjSuperFac > 1 ? -1 : 1), mNxOutPad,
                       mNyOutPad, mCleanSuperFFT, maxGpuPlane, maxNeeds[0], if3Dtexture)
        == 0) {
      mNumGpuPlanes = i;
      mLoadGpuStart = 0;
      mLoadGpuEnd = 0;
      break;
    }
  }
  mUseGPU = mNumGpuPlanes > 0;
  if (! mUseGPU && maxGpuPlane >= maxNeeds[0]) 
    mGpuErrString = "Failed to allocate enough memory on the GPU to reconstruct even "
      "a single plane";
  return;
}

//
// Issue desired warning or exit on error if GPU not available
//
void Tilt::warnOrExitIfNoGPU()
{
  if (mUseGPU)
    return;
  if ((mIfGpuByEnviron != 0 && mIactGpuFailEnviron == 2) ||  
      (mIfGpuByEnviron == 0 && mIactGpuFailOption == 2)) {
    printf("\nERROR: TILT - %s\n", mGpuErrString.c_str());
    exitError("Use of the GPU was requested but a GPU cannot be used");
  }
  printf("%s\n", mGpuErrString.c_str());
  if ((mIfGpuByEnviron != 0 && mIactGpuFailEnviron == 1) ||  
      (mIfGpuByEnviron == 0 && mIactGpuFailOption == 1))
    printf("MESSAGE: Use of the GPU was requested but a GPU will not be used\n");
  fflush(stdout);
  return;
}

//
// Load local data into a single array for upload to the GPU
//
float *Tilt::packLocalData()
{
  int ipos, iv, i, j, stride = mNumWarpPos * mNumViews;
  float *packLocal = B3DMALLOC(float, 12 * stride);
  if (!packLocal) {
    mUseGPU = false;
    mGpuErrString = "Failed to allocate array needed for using GPU with local alignments";
    warnOrExitIfNoGPU();
  } else {
    //
    // Pack data into one array
    for (ipos = 0; ipos < mNumWarpPos; ipos++) {
      for (iv = 0; iv < mNumViews; iv++) {
        i = mIndWarp[ipos] + mMapUsedView[iv] - 1;
        j = ipos * mNumViews + iv;
        packLocal[j] = mFwarp[6 * i];
        packLocal[j + stride] = mFwarp[1 + 6 * i];
        packLocal[j + stride * 2] = mFwarp[2 + 6 * i];
        packLocal[j + stride * 3] = mFwarp[3 + 6 * i];
        packLocal[j + stride * 4] = mFwarp[4 + 6 * i];
        packLocal[j + stride * 5] = mFwarp[5 + 6 * i];
        packLocal[j + stride * 6] = mCWarpAlpha[i];
        packLocal[j + stride * 7] = mSWarpAlpha[i];
        packLocal[j + stride * 8] = mCWarpBeta[i];
        packLocal[j + stride * 9] = mSWarpBeta[i];
        packLocal[j + stride * 10] = mWarpXZfac[i];
        packLocal[j + stride * 11] = mWarpYZfac[i];
      }
    }
  }
  return packLocal;
}

//
// Get a list from an input line, handling errors
//
int *Tilt::parseCard(char *card, int &nViews, const char *descrip)
{
  int *list = parselist(card, &nViews);
  if (nViews == -1)
    exitError("Illegal entry for %s", descrip);
  if (!list)
    exitError("Allocating array for %s", descrip);
  if (nViews > mLimView)
    exitError("More views in %s than in input file", descrip);
  free(card);
  return list;
}

//
// Read one or two values into array(s) using the general value reader function
// The Fortran reads were all into the array(s) with a single read statement, and
// the help never said it had to be 1/2 values per line, so read it all the same way
// (Locals are multiple values per line)
//
void Tilt::getValuesFromLines(FILE *fp, char *filename, float *values1, float *values2, 
                              bool separateLines, int &numValues, const char *descrip)
{
  int ierr;
  int numToGet = B3DABS(numValues);
  char *temp;
  if (filename) {
    fp = fopen(filename, "r");
    if (!fp)
      exitError("Opening file of %s, %s", descrip, filename);
  }
  if (values2)
    ierr = readLinesForValues(fp, &numValues, numToGet, mLine, MAX_LINE, 
                              separateLines ? RLFV_SEPARATE_LINES : 0, "ff", values1,
                              values2);
  else
    ierr = readLinesForValues(fp, &numValues, numToGet, mLine, MAX_LINE, 
                              separateLines ? RLFV_SEPARATE_LINES : 0, "f", values1);
  if (ierr == -1)
    exitError("Reading a line from file of %s", descrip);
  if (ierr == -2)
    exitError("End of file before all values gotten from file of %s", descrip);
  if (ierr == -3)
    exitError("Array full before all values gotten from file of %s", descrip);
  if (ierr == -4) {
    PipGetError(&temp);
    exitError("Parsing values on line from file of %s: %s", descrip, temp);
  }
  if (ierr == -5)
    exitError("Non-integer value when expecting an integer in file %s", descrip);
  if (ierr == -6)
    exitError("Allocating temporary array to get values from file of %s", descrip);
  if (ierr == -7)
    exitError("Programming error trying to get values from file of %s", descrip);
  if (filename) {
    fclose(fp);
    free(filename);
  }
}
  
//
// Finds two nearest angles to projAngle and returns their indices ind1 and
// ind2 and and interpolation fraction frac.  THESE INDICES ARE 0-BASED
//
void Tilt::lookupAngle(float projAngle, float *angles, int numViews, int &ind1, int &ind2,
                       float &frac)
{
  int  i;
  ind1 = -1;
  ind2 = -1;
  for (i = 0; i < numViews; i++) {
    if (projAngle >= angles[i]) {
      if (ind1 < 0)
        ind1 = i;
      if (angles[i] > angles[ind1]) 
        ind1 = i;
    } else {
      if (ind2 < 0)
        ind2 = i;
      if (angles[i] < angles[ind2])
        ind2 = i;
    }
  }
  frac = 0.;
  if (ind1 < 0) {
    ind1 = ind2;
  } else if (ind2 < 0) {
    ind2 = ind1;
  } else {
    frac = (projAngle - angles[ind1]) / (angles[ind2] - angles[ind1]);
  }
}

//
// Compute mean and SD of interior of a slice for SIRT report
//
void Tilt::sampleForReport(float *slice, int lslice, int kthick, int iteration, 
                           float sampScale, float sampAdd)
{
  float avg, SD;
  int  iskip, ixLow, iyLow, ierr;
  unsigned char **linePtrs;
  //
  iskip = (mIsliceEnd - mIsliceStart) / 10;
  if (lslice < mIsliceStart + iskip || lslice > mIsliceEnd - iskip)
    return;
  ixLow = mIwidth / 10;
  iyLow = kthick / 4;
  linePtrs = makeLinePointers(slice, mIwidth, kthick, sizeof(float));
  if (!linePtrs)
    return;
  ierr = sampleMeanSD(linePtrs, typeForSampleMean(MRC_MODE_FLOAT), mIwidth, kthick, 0.02,
                      ixLow, iyLow, mIwidth - 2 * ixLow, kthick - 2 * iyLow, &avg, &SD);
  free(linePtrs);
  if (ierr != 0)
    return;
  mReportVals[3 * (iteration - 1)] += (avg + sampAdd) * sampScale;
  mReportVals[1 + 3 * (iteration - 1)] += SD * sampScale;
  mReportVals[2 + 3 * (iteration - 1)]++;
}

//
// return indices (1-based) to four local areas, and fractions to apply for each
// at location ix, iy in view iv, where ix and iy are indexes in
// the reconstruction adjusted to match coordinates of projections
//
void Tilt::localFactors(float x, int iy, int iv, int &ind1, int &ind2, int &ind3, 
                        int &ind4, float &f1, float &f2, float &f3, float &f4)
{
  //
  int ixPos, iyt, iyPos;
  float xt, fx, fy;
  //
  xt = B3DMIN(B3DMAX(x - mIxStartWarp, 0.), (mNxWarp - 1) * mIdelXwarp);
  ixPos = B3DMIN(xt / mIdelXwarp + 1, mNxWarp - 1);
  fx = (xt - (ixPos - 1) * mIdelXwarp) / mIdelXwarp;
  iyt = B3DMIN(B3DMAX(iy - mIyStartWarp, 0), (mNyWarp - 1) * mIdelYwarp);
  iyPos = B3DMIN(iyt / mIdelYwarp + 1, mNyWarp - 1);
  fy = (float)(iyt - (iyPos - 1) * mIdelYwarp) / mIdelYwarp;

  ind1 = mIndWarp[mNxWarp * (iyPos - 1) + ixPos - 1] + iv;
  ind2 = mIndWarp[mNxWarp * (iyPos - 1) + ixPos] + iv;
  ind3 = mIndWarp[mNxWarp * iyPos + ixPos - 1] + iv;
  ind4 = mIndWarp[mNxWarp * iyPos + ixPos] + iv;
  f1 = (1. -fy) * (1. -fx);
  f2 = (1. -fy) * fx;
  f3 = fy * (1. -fx);
  f4 = fy * fx;
}

//
// Compute local projection factors at a position in a column for view iv:
// j is X index in the reconstruction, lslice is slice # in aligned stack
//
void Tilt::localProjFactors(float x, int lslice, int iv, float &xprojFix, float &xprojZ, 
                            float &yprojFix, float &yprojZ)
{
  int ind1, ind2, ind3, ind4;
  float f1, f2, f3, f4, xx, yy, xc;
  float cosAlph, sinAlph, a11, a12, a21, a22, xAdd, yAdd, xAllAdd, yAllAdd;
  float cosAlph2, sinAlph2, a112, a122, a212, a222, xAdd2, yAdd2;
  float cosAlph3, sinAlph3, a113, a123, a213, a223, xAdd3, yAdd3;
  float cosAlph4, sinAlph4, a114, a124, a214, a224, xAdd4, yAdd4;
  float f1x, f2x, f3x, f4x, f1xy, f2xy, f3xy, f4xy;
  float f1y, f2y, f3y, f4y, f1yy, f2yy, f3yy, f4yy;
  float xp1f, xp1z, yp1f, xp2f, xp2z, yp2f, xp3f, xp3z, yp3f, xp4f, xp4z, yp4f;
  float cosBet, sinBet, cosBet2, sinBet2, cosBet3, sinBet3, cosBet4, sinBet4;
  int fwInd1, fwInd2, fwInd3, fwInd4;
  //
  // get transform and angle adjustment
  //
  xc = x - mXcenOut + mXcenIn + mAxisXoffset;
  localFactors(xc, lslice, mMapUsedView[iv - 1], ind1, ind2, ind3, ind4, f1, f2, f3,f4);
  //
  // get all the factors needed to compute a projection position
  // from the four local transforms
  //
  ind1--;
  ind2--;
  ind3--;
  ind4--;
  fwInd1 = 6 *ind1;
  fwInd2 = 6 *ind2;
  fwInd3 = 6 *ind3;
  fwInd4 = 6 *ind4;
  cosBet = mCWarpBeta[ind1];
  sinBet = mSWarpBeta[ind1];
  cosAlph = mCWarpAlpha[ind1];
  sinAlph = mSWarpAlpha[ind1];
  a11 = mFwarp[fwInd1];
  a12 = mFwarp[2 + fwInd1];
  a21 = mFwarp[1 + fwInd1];
  a22 = mFwarp[3 + fwInd1];
  xAdd = mFwarp[4 + fwInd1] + mXcenIn - mXcenIn * a11 - mCenterSlice * a12;
  yAdd = mFwarp[5 + fwInd1] + mCenterSlice - mXcenIn * a21 - mCenterSlice * a22;
  //
  cosBet2 = mCWarpBeta[ind2];
  sinBet2 = mSWarpBeta[ind2];
  cosAlph2 = mCWarpAlpha[ind2];
  sinAlph2 = mSWarpAlpha[ind2];
  a112 = mFwarp[fwInd2];
  a122 = mFwarp[2 + fwInd2];
  a212 = mFwarp[1 + fwInd2];
  a222 = mFwarp[3 + fwInd2];
  xAdd2 = mFwarp[4 + fwInd2] + mXcenIn - mXcenIn * a112 - mCenterSlice * a122;
  yAdd2 = mFwarp[5 + fwInd2] + mCenterSlice - mXcenIn * a212 - mCenterSlice * a222;
  //
  cosBet3 = mCWarpBeta[ind3];
  sinBet3 = mSWarpBeta[ind3];
  cosAlph3 = mCWarpAlpha[ind3];
  sinAlph3 = mSWarpAlpha[ind3];
  a113 = mFwarp[fwInd3];
  a123 = mFwarp[2 + fwInd3];
  a213 = mFwarp[1 + fwInd3];
  a223 = mFwarp[3 + fwInd3];
  xAdd3 = mFwarp[4 + fwInd3] + mXcenIn - mXcenIn * a113 - mCenterSlice * a123;
  yAdd3 = mFwarp[5 + fwInd3] + mCenterSlice - mXcenIn * a213 - mCenterSlice * a223;
  //
  cosBet4 = mCWarpBeta[ind4];
  sinBet4 = mSWarpBeta[ind4];
  cosAlph4 = mCWarpAlpha[ind4];
  sinAlph4 = mSWarpAlpha[ind4];
  a114 = mFwarp[fwInd4];
  a124 = mFwarp[2 + fwInd4];
  a214 = mFwarp[1 + fwInd4];
  a224 = mFwarp[3 + fwInd4];
  xAdd4 = mFwarp[4 + fwInd4] + mXcenIn - mXcenIn * a114 - mCenterSlice * a124;
  yAdd4 = mFwarp[5 + fwInd4] + mCenterSlice - mXcenIn * a214 - mCenterSlice * a224;
  //
  f1x = f1 * a11;
  f2x = f2 * a112;
  f3x = f3 * a113;
  f4x = f4 * a114;
  f1xy = f1 * a12;
  f2xy = f2 * a122;
  f3xy = f3 * a123;
  f4xy = f4 * a124;
  // fxfromy=f1*a12+f2*a122+f3*a123+f4*a124
  f1y = f1 * a21;
  f2y = f2 * a212;
  f3y = f3 * a213;
  f4y = f4 * a214;
  f1yy = f1 * a22;
  f2yy = f2 * a222;
  f3yy = f3 * a223;
  f4yy = f4 * a224;
  // fyfromy=f1*a22+f2*a222+f3*a223+f4*a224
  xAllAdd = f1 * xAdd + f2 * xAdd2 + f3 * xAdd3 + f4 * xAdd4;
  yAllAdd = f1 * yAdd + f2 * yAdd2 + f3 * yAdd3 + f4 * yAdd4;
  //
  // Each projection position is a sum of a fixed factor ("..f")
  // and a factor that multiplies z ("..z")
  //
  xx = x - mXcenOut;
  yy = lslice - mCenterSlice;
  xp1f = xx * cosBet + yy * sinAlph * sinBet + mXcenIn + mAxisXoffset;
  xp1z = cosAlph * sinBet + mWarpXZfac[ind1];
  xp2f = xx * cosBet2 + yy * sinAlph2 * sinBet2 + mXcenIn + mAxisXoffset;
  xp2z = cosAlph2 * sinBet2 + mWarpXZfac[ind2];
  xp3f = xx * cosBet3 + yy * sinAlph3 * sinBet3 + mXcenIn + mAxisXoffset;
  xp3z = cosAlph3 * sinBet3 + mWarpXZfac[ind3];
  xp4f = xx * cosBet4 + yy * sinAlph4 * sinBet4 + mXcenIn + mAxisXoffset;
  xp4z = cosAlph4 * sinBet4 + mWarpXZfac[ind4];

  yp1f = yy * cosAlph + mCenterSlice;
  yp2f = yy * cosAlph2 + mCenterSlice;
  yp3f = yy * cosAlph3 + mCenterSlice;
  yp4f = yy * cosAlph4 + mCenterSlice;
  //
  // store the fixed and z-dependent component of the
  // projection coordinates
  //
  xprojFix = f1x * xp1f + f2x * xp2f + f3x * xp3f + f4x * xp4f + 
    f1xy * yp1f + f2xy * yp2f + f3xy * yp3f + f4xy * yp4f + xAllAdd;
  xprojZ = f1x * xp1z + f2x * xp2z + f3x * xp3z + f4x * xp4z - 
      (f1xy * (sinAlph - mWarpYZfac[ind1]) + f2xy * (sinAlph2 - mWarpYZfac[ind2]) + 
       f3xy * (sinAlph3 - mWarpYZfac[ind3]) + f4xy * (sinAlph4 - mWarpYZfac[ind4]));
  yprojFix = f1y * xp1f + f2y * xp2f + f3y * xp3f + f4y * xp4f + 
    f1yy * yp1f + f2yy * yp2f + f3yy * yp3f + f4yy * yp4f + yAllAdd;
  yprojZ = f1y * xp1z + f2y * xp2z + f3y * xp3z + f4y * xp4z - 
    (f1yy * (sinAlph - mWarpYZfac[ind1]) + f2yy * (sinAlph2 - mWarpYZfac[ind2]) + 
     f3yy * (sinAlph3 - mWarpYZfac[ind3]) + f4yy * (sinAlph4 - mWarpYZfac[ind4]));
}

//
// Finds the point at centered Z coordinate zz projecting to
// xproj, yproj in view iv of original projections.  xx is X index in
// reconstruction, yy is slice number in original projections
//
void Tilt::findProjectingPoint(float xproj, float yproj, float zz, int &iv, float &xx, 
                               float &yy)
{
  int iter, ifDone, ixAssay, iyAssay;
  float xprojFix11, xprojZ11, yprojFix11, yprojZ11, xprojFix21, xprojZ21;
  float yprojFix21, yprojZ21, xprojFix12, xprojZ12, yprojFix12, yprojZ12;
  float xp11, yp11, xp12, yp12, xp21, yp21, xerr, yerr, dxpx, dxpy, dypx;
  float dypy, fx, fy, den;
  
  iter = 1;
  ifDone = 0;
  while (ifDone == 0 && iter <= 5) {
    ixAssay = floor(xx);
    iyAssay = floor(yy);
    localProjFactors(ixAssay, iyAssay, iv, xprojFix11, xprojZ11, yprojFix11, yprojZ11);
    localProjFactors(ixAssay + 1, iyAssay, iv, xprojFix21, xprojZ21, yprojFix21,
                     yprojZ21);
    localProjFactors(ixAssay, iyAssay + 1, iv, xprojFix12, xprojZ12, yprojFix12, 
                     yprojZ12);
    xp11 = xprojFix11 + xprojZ11 * zz;
    yp11 = yprojFix11 + yprojZ11 * zz;
    xp21 = xprojFix21 + xprojZ21 * zz;
    yp21 = yprojFix21 + yprojZ21 * zz;
    xp12 = xprojFix12 + xprojZ12 * zz;
    yp12 = yprojFix12 + yprojZ12 * zz;
    xerr = xproj - xp11;
    yerr = yproj - yp11;
    dxpx = xp21 - xp11;
    dxpy = xp12 - xp11;
    dypx = yp21 - yp11;
    dypy = yp12 - yp11;
    den = dxpx * dypy - dxpy * dypx;
    fx = (xerr * dypy - yerr * dxpy) / den;
    fy = (dxpx * yerr - dypx * xerr) / den;
    xx = ixAssay + fx;
    yy = iyAssay + fy;
    if (fx > -0.1 && fx < 1.1 && fy > -0.1 && fy < 1.1)
      ifDone = 1;
    iter = iter + 1;
  }
}

//
// Compute space needed for cosine stretched data
//
void Tilt::setCosStretch()
{
  int lsliceMin, lsliceMax, iv, ix, iy, lslice;
  float tanAlph, xpMax, xpMin, zz, zPart, yy, xproj;
  // make the indexes be bases, numbered from 0
  //
  mIndStretchLine[0] = 0;
  lsliceMin = B3DMIN(mIsliceEnd, mIsliceStart);
  lsliceMax = B3DMAX(mIsliceEnd, mIsliceStart);
  if (mIfAlpha < 0) {
    //
    // New-style X tilting: SET MINIMUM NUMBER OF INPUT SLICES HERE
    //
    lsliceMin = mCenterSlice + (lsliceMin - mCenterSlice) * mCosAlpha[0] + 
      mYOffset * mSinAlpha[0] - 0.5 * mIthickOut * B3DABS(mSinAlpha[0]) - 1.;
    lsliceMax = mCenterSlice + (lsliceMax - mCenterSlice) * mCosAlpha[0] + 
      mYOffset * mSinAlpha[0] + 0.5 * mIthickOut * B3DABS(mSinAlpha[0]) + 2.;
    tanAlph = mSinAlpha[0] / mCosAlpha[0];
    lsliceMin = B3DMAX(1, lsliceMin);
    lsliceMax = B3DMIN(lsliceMax, mNyProj);
  }

  // iv can be indexed from zero, it is used only as a subscript
  for (iv = 0; iv < mNumViews; iv++) {
    xpMax = 1;
    xpMin = mNxProj;
    //
    // find min and max position of 8 corners of reconstruction
    //
    for (ix = 1; ix <= mIwidth; ix += mIwidth - 1) {
      for (iy = 1; iy <= mIthickBP; iy += mIthickBP - 1) {
        for (lslice = lsliceMin; lslice <= lsliceMax; 
             lslice += B3DMAX(1, lsliceMax - lsliceMin)) {
          zz = (iy - mYcenOut) * mCompress[iv];
          if (mIfAlpha < 0) 
            zz = mCompress[iv] * (iy - (mYcenOut - B3DNINT(tanAlph * 
                                                           (lslice - mCenterSlice))));
          if (mIfAlpha <= 0) {
            zPart = zz * mSinBeta[iv] + mXcenIn + mAxisXoffset;
          } else {
            yy = lslice - mCenterSlice;
            zPart = yy * mSinAlpha[iv] * mSinBeta[iv] + 
              zz * (mCosAlpha[iv] * mSinBeta[iv] + mXZfac[iv]) + mXcenIn + mAxisXoffset;
          }
          xproj = zPart + (ix - mXcenOut) * mCosBeta[iv];
          xpMin = B3DMAX(1., B3DMIN(xpMin, xproj));
          xpMax = B3DMIN((float)mNxProj, B3DMAX(xpMax, xproj));
        }
      }
    }
    // print *,iv, xpmin, xpmax
    //
    // set up extent and offset of stretches
    //
    mStretchOffset[iv] = xpMin / mCosBeta[iv] - 1. / mInterpFacStretch;
    mNxStretched[iv] = mInterpFacStretch * (xpMax - xpMin) / mCosBeta[iv] + 2.;
    mIndStretchLine[iv + 1] = mIndStretchLine[iv] + mNxStretched[iv];
    // print *,iv, xpmin, xpmax, stretchOffset[iv], nxStretched[iv], indStretchLine[iv]
  }
}

//
// Determine starting and ending input slice needed to reconstruct
// each output slice, as well as the maximum needed over all slices
// for a series of numbers of output slices up to numEval
//
void Tilt::setNeededSlices(int *maxNeeds, int numEval)
{
  int lsliceMin, lsliceMax, ierr, itry, nxAssay, minSlice, ixAssay;
  int maxSlice, iassay, ixSample, iv, iy, iyp, ivm1;
  float dxAssay, dxTemp, xx, yy, zz, xp, yp;
  float xprojFix, xprojZ, yprojFix, yprojZ, xproj, yproj;
  lsliceMin = mIsliceStart;
  lsliceMax = mIsliceEnd;
  if (mIfAlpha < 0) {
    lsliceMin = mCenterSlice + (mIsliceStart - mCenterSlice) * mCosAlpha[0] +  
      mYOffset * mSinAlpha[0] - 0.5 * mIthickOut * B3DABS(mSinAlpha[0]) - 1.;
    lsliceMax = mCenterSlice + (mIsliceEnd - mCenterSlice) * mCosAlpha[0] +  
      mYOffset * mSinAlpha[0] + 0.5 * mIthickOut * B3DABS(mSinAlpha[0]) + 2.;
    lsliceMin = B3DMAX(1, lsliceMin);
    lsliceMax = B3DMIN(lsliceMax, mNyProj);
  }
  mIndNeededBase = lsliceMin - 1;
  mNumNeedSE = lsliceMax - mIndNeededBase;
  if (!mNeededStarts) {
    mNeededStarts = B3DMALLOC(int, mNumNeedSE);
    mNeededEnds = B3DMALLOC(int, mNumNeedSE);
    if (!mNeededStarts || !mNeededEnds)
      memoryError(NULL, "arrays needstarts/needends");
  }

  for (itry = lsliceMin; itry <= lsliceMax; itry++) {

    if (mIfAlpha <= 0 && mNxWarp == 0) {
      //
      // regular case is simple: just need the current slice
      //
      mNeededStarts[itry - mIndNeededBase - 1] = itry;
      mNeededEnds[itry - mIndNeededBase - 1] = itry;
      } else {
      //
      // for old-style X-tilt or local alignment, determine what
      // slices are needed by sampling
      // set up sample points: left and right if no warp,
      // or half the warp spacing
      //
      if (mNxWarp == 0) {
        nxAssay = 2;
        dxAssay = mIwidth - 1;
        } else {
        dxTemp = mIdelXwarp / 2;
        nxAssay = B3DMAX(2., mIwidth / dxTemp + 1.);
        dxAssay = (mIwidth - 1.) / (nxAssay - 1.);
      }
      //
      // sample top and bottom at each position
      //
      minSlice = mNyProj + 1;
      maxSlice = 0;
      for (iassay = 1; iassay <= nxAssay; iassay++) {
        ixAssay = B3DNINT(1 + (iassay - 1) * dxAssay);
        for (iv = 1; iv <= mNumViews; iv++) {
          ivm1 = iv - 1;
          if (! mRecReproj) {
            ixSample = B3DNINT(ixAssay - mXcenOut + mXcenIn + mAxisXoffset);
            if (mNxWarp != 0) {
              localProjFactors(ixAssay, itry, iv, xprojFix, 
                               xprojZ, yprojFix, yprojZ);
            }
            for (iy = 1; iy <= mIthickBP; iy += mIthickBP - 1) {

              //
              // for each position, find back-projection location
              // transform if necessary, and use to get min and
              // max slices needed to get this position
              //
              xx = ixSample - mXcenOut;
              yy = itry - mCenterSlice;
              zz = iy - mYcenOut;
              xp = xx * mCosBeta[ivm1] + yy * mSinAlpha[ivm1] * mSinBeta[ivm1] + 
                zz * (mCosAlpha[ivm1] * mSinBeta[ivm1] + mXZfac[ivm1]) + 
                mXcenIn +mAxisXoffset;
              yp = yy * mCosAlpha[ivm1] - zz * (mSinAlpha[ivm1] - mYZfac[ivm1]) + 
                mCenterSlice;
              if (mNxWarp != 0) {
                xp = xprojFix + xprojZ * zz;
                yp = yprojFix + yprojZ * zz;
              }
              iyp = B3DMAX(1., yp);
              minSlice = B3DMIN(minSlice, iyp);
              maxSlice = B3DMAX(maxSlice, B3DMIN(mNyProj, iyp + 1));
              // if (debug) print *,xx, yy, zz, iyp, minslice, maxslice
            }
            } else {
            //
            // Projections: get Y coordinate in original projection
            // if local, get the X coordinate in reconstruction too
            // then get the refinement
            xproj = ixAssay + mXprojOffset;
            yproj = itry + mYprojOffset;
            for (iy = 1; iy <= mIthickReproj; iy += mIthickReproj - 1) {
              zz = iy + mMinYreproj - 1 - mYcenOut;
              yy = (yproj + zz * (mSinAlpha[ivm1] - mYZfac[ivm1]) - mCenterSlice) /  
                mCosAlpha[ivm1] + mCenterSlice;
              if (mNxWarp != 0) {
                xx = (xproj - yy * mSinAlpha[ivm1] * mSinBeta[ivm1] - 
                      zz * (mCosAlpha[ivm1] * mSinBeta[ivm1] + mXZfac[ivm1]) - 
                      mXcenIn - mAxisXoffset) / mCosBeta[ivm1] + mXcenOut;
                findProjectingPoint(xproj, yproj, zz, iv, xx, yy);
              }
              iyp = B3DMAX(1., yy - mYprojOffset);
              minSlice = B3DMIN(minSlice, iyp);
              maxSlice = B3DMAX(maxSlice, B3DMIN(mNyProj, iyp + 1));
            }
          }
        }
      }
      //
      // set up starts and ends
      //
      mNeededStarts[itry - mIndNeededBase - 1] = B3DMAX(1, minSlice);
      mNeededEnds[itry - mIndNeededBase - 1] = B3DMIN(mNyProj, maxSlice);
    }
  }
  //
  // Count maximum # of slices needed for number of slices to be computed
  for (iv = 0; iv < numEval; iv++) {
    maxNeeds[iv] = 0;
    for (iy = lsliceMin; iy <= lsliceMax - iv; iy++) {
      maxSlice = mNeededEnds[iy + iv - mIndNeededBase - 1] + 1 -  
        mNeededStarts[iy - mIndNeededBase - 1];
      maxNeeds[iv] = B3DMAX(maxNeeds[iv], maxSlice);
    }
  }
}

#define ALLOC_IF_NEEDED(need, array)            \
  if (!ierr && need) {                          \
    array = B3DMALLOC(float, need);               \
    if (!array)                                   \
      ierr = 1;                                   \
  }

//
// Allocate main data array, trying to get enough to do numEval slices without
// reloading any data, based on the numbers in maxNeeds, and trying fewer
// slices down to minLoad if that fails.  MinMemory is the minimum amount
// it will allocate.
//
int Tilt::allocateArray(int *maxNeeds, int numEval, int minLoad, int minMemory)
{
  int ierr, i;
  b3dInt64 memNeed, minNeed, baseNeed, inputNeed, wholeNeed;
  bool allocLoad = !mRecReproj && mIdelSlice == 1;
  baseNeed = (b3dInt64)mNeedForFiltArr + mNeedForOutArr + mNeedForVertArr +
    mNeedForReadInArr + mNeedForWorkArr + mNeedForSuperArr + 32;
  wholeNeed = baseNeed + (b3dInt64)mInPlaneSize * (mNeededEnds[mNumNeedSE - 1] + 1 -
                                                   mNeededStarts[0]) + mIpExtraSize;
  minNeed = B3DMIN(wholeNeed, minMemory);
  ierr = 0;
  ALLOC_IF_NEEDED(mNeedForFiltArr, mFilterArray);
  ALLOC_IF_NEEDED(mNeedForOutArr, mOutSliceArr);
  ALLOC_IF_NEEDED(mNeedForVertArr, mVertSliceArr);
  ALLOC_IF_NEEDED(mNeedForReadInArr, mReadInArray);
  ALLOC_IF_NEEDED(mNeedForWorkArr, mWorkArray);
  ALLOC_IF_NEEDED(mNeedForSuperArr, mSuperOutArr);
  if (!ierr) {
    for (i = numEval; i >= B3DMIN(numEval, minLoad); i--) {
      memNeed = mInPlaneSize;
      inputNeed = (b3dInt64)mInPlaneSize * maxNeeds[i - 1] + mIpExtraSize;
      memNeed = baseNeed + inputNeed;
      memNeed = B3DMAX(memNeed, minNeed);
      inputNeed = memNeed - baseNeed;
      if (memNeed < 2147000000) {
        mInputArray = B3DMALLOC(float, inputNeed);
        if (mInputArray) {
          mMaxStack = memNeed;
          mNumPlanes = ((memNeed - mIpExtraSize) - baseNeed) / mInPlaneSize;
          if (allocLoad)
            mLoadBuffer = B3DMALLOC(float, mNumPlanes * mNxProj);
          if (!allocLoad || mLoadBuffer) {
            printf("\nAllocated %d MB for main arrays\n", 
                   B3DNINT(mMaxStack / (1024*256.)));
            return i;
          }
        }
      }
    }
  }
  B3DFREE(mFilterArray);
  B3DFREE(mOutSliceArr);
  B3DFREE(mVertSliceArr);
  B3DFREE(mReadInArray);
  B3DFREE(mWorkArray);
  B3DFREE(mSuperOutArr);
  B3DFREE(mInputArray);
  return 0;
}

// This function determines the sizes for the super-sampled slice array and the regular 
// slice array when super-sampling, including the size that would be needed on the GPU
//
void Tilt::setupSizesForSupersampling()
{
  int niceGpuLimit = 5;
  if (mSuperSampleFac > 1) {
    mNxOutPad = 2 * ((mIwidth + B3DMAX(16, 0.01 * mIwidth) + 1) / 2);
    mNxGpuCropPad = niceFrame(mNxOutPad, 2, niceGpuLimit);
    mNxOutPad = niceFrame(mNxOutPad, 2, niceFFTlimit());
    mNyOutPad = 2 * ((mIthickBP + B3DMAX(16, 0.01 * mIthickBP) + 1) / 2);
    mNyGpuCropPad = niceFrame(mNyOutPad, 2, niceGpuLimit);
    mNyOutPad = niceFrame(mNyOutPad, 2, niceFFTlimit());
    mNeedForOutArr = (mNxOutPad + 2) * mNyOutPad;
    mNeedForSuperArr = (b3dInt64)(mNxOutPad * mSuperSampleFac + 2) * 
      mNyOutPad * mSuperSampleFac;
    mInPlaneSize = (mProjSuperFac * (mNxProj + mNumPad) + 2) * mNumViews;
  }
}

//
// This is the old reprojection from a single slice that matches xyzproj output
//
void Tilt::reProject(float *array, int nxs, int nys, int nxOut, float sinAngle, 
                     float cosAngle, float *xRayStart, float *yRayStart, int *numPixInRay,
                     int maxRayPixels, float fill, float *projLine, int linear, 
                     int noScale)
{
  int ixOut, iray, ixr, iyr, numRayPts, idir;
  float rayFac, rayAdd, xRay, yRay, pixTemp, fullFill;
  float dx, dy, v2, v4, v6, v8, v5, a, b, c, d;
  //
  rayFac = 1. / maxRayPixels;
  fullFill = fill;
  if (noScale != 0) {
    rayFac = 1.;
    fullFill = fill * maxRayPixels;
  }

  // ixOut used only as an index: loop from 0
  for (ixOut = 0; ixOut < nxOut; ixOut++) {
    projLine[ixOut] = fullFill;
    numRayPts = numPixInRay[ixOut];
    if (numRayPts > 0) {
      pixTemp = 0.;
      if (sinAngle != 0.) {
        if (linear == 0) {
          for (iray = 0; iray <= numRayPts - 1; iray++) {
            xRay = xRayStart[ixOut] + iray * sinAngle;
            yRay = yRayStart[ixOut] + iray * cosAngle;
            ixr = B3DNINT(xRay);
            iyr = B3DNINT(yRay);
            dx = xRay - ixr;
            dy = yRay - iyr;
            v2 = array[ixr - 1 + nxs * (iyr - 2)];
            v4 = array[ixr - 2 + nxs * (iyr - 1)];
            v5 = array[ixr - 1 + nxs * (iyr - 1)];
            v6 = array[ixr     + nxs * (iyr - 1)];
            v8 = array[ixr - 1 + nxs * iyr];
            //
            a = (v6 + v4) * .5 - v5;
            b = (v8 + v2) * .5 - v5;
            c = (v6 - v4) * .5;
            d = (v8 - v2) * .5;
            pixTemp = pixTemp + a * dx * dx + b * dy * dy + c * dx + d * dy + v5;
          }
        } else {
          for (iray = 0; iray <= numRayPts - 1; iray++) {
            xRay = xRayStart[ixOut] + iray * sinAngle;
            yRay = yRayStart[ixOut] + iray * cosAngle;
            ixr = xRay;
            iyr = yRay;
            dx = xRay - ixr;
            dy = yRay - iyr;
            pixTemp = pixTemp + (1 - dy) * 
              ((1. -dx) * array[ixr - 1 + nxs * (iyr - 1)] + 
               dx * array[ixr + nxs * (iyr - 1)]) + 
              dy * ((1. -dx) * array[ixr - 1 + nxs * iyr] + dx * array[ixr + nxs * iyr]);
          }
        }
      } else {
        //
        // vertical projection
        //
        ixr = B3DNINT(xRayStart[ixOut]);
        iyr = B3DNINT(yRayStart[ixOut]);
        idir = B3DSIGN(1., cosAngle);
        for (iray = 0; iray <= numRayPts - 1; iray++) {
          pixTemp = pixTemp + array[ixr - 1 + nxs * (iyr + idir * iray - 1)];
        }
      }

      rayAdd = rayFac * (maxRayPixels - numRayPts) * fill;
      projLine[ixOut] = rayFac * pixTemp + rayAdd;
    }
  }
}

//
// Computes the change in Z that moves by 1 pixel along a projection
// ray given the sines and cosines of alpha and beta and the z factors
//
float Tilt::reprojDelZ(float sinBet, float cosBet, float sinAlph, float cosAlph,
                       float mXZfac, float mYZfac)
{
  float dyFac = (sinAlph - mYZfac) / cosAlph;
  float bigFac = (dyFac * sinAlph * sinBet + cosAlph * sinBet + mXZfac) / cosBet;
  return 1.f / sqrtf(1. + dyFac * dyFac + bigFac * bigFac);
}

//
// Reprojects slices from lsliceStart to lsliceEnd and writes the reprojections
// Fewer slices may be done if on the GPU, and the ending slice done is
// returned in lsliceEnd.  inLoadStart and inLoadEnd are slice numbers of
// started and ending slices loaded; min/max/sum densities are maintained
// in dmin, dmax, and dtot8
//
void Tilt::reprojectRec(int lsliceStart, int lsliceEnd, int inLoadStart, int inLoadEnd, 
                        float &dmin, float &dmax, double &dtot8)
{
  int iv, ix, iy, iz, ixp, line, i, iys, ind;
  int ind1, ind2, ind3, ind4, load;
  float cosAlph, sinAlph, cosBet, sinBet, delZ, delX, fz, oneMfz, zz, xx, fx;
  float oneMfx, yy, fy, oneMfy, xproj, yproj, d11, d12, d21, d22;
  float f1, f2, f3, f4, xxGood, yyGood, zzGood;
  float yEndTol, xprojMin, xprojMax, xJump, zJump, delY, diffXmax, diffYmax;
  int indBase, nxLoad, ixc, lastZdone;
  int indJump, numJump, lgpuEnd, lineBase;
  float ycenAdj, tmpCenIn;
  double sum, wallStart, wallCumul;
  bool tryJump;

  yEndTol = 3.05;
  xJump = 5.0;
  nxLoad = mMaxXload + 1 - mMinXload;
  wallStart = wallTime();
  wallCumul = 0.;
  ycenAdj = mYcenOut - (mMinYreproj - 1);
  //
  if (mUseGPU && mLoadGpuStart > 0) {
    indJump = 1;
    //
    // GPU REPROJECTION: Find last slice that can be done
    for (lgpuEnd = lsliceEnd; lgpuEnd >= lsliceStart; lgpuEnd--) {
      if (mNeededEnds[lgpuEnd - mIndNeededBase - 1] <= mLoadGpuEnd) {
        indJump = 0;
        break;
      }
    }
    if (indJump == 0) {
      //
      // Loop on views; do non-local case first
      for (iv = 1; iv <= mNumViews; iv++) {
        if (mNxWarp == 0) {
          delZ = reprojDelZ(mSinBeta[iv - 1], mCosBeta[iv - 1], mSinAlpha[iv - 1], 
                            mCosAlpha[iv - 1], mXZfac[iv - 1], mYZfac[iv - 1]);
          tmpCenIn = mXcenIn + mAxisXoffset;
          indJump = gpuReproject
            (mReprojLines, mSinBeta[iv - 1], mCosBeta[iv - 1], mSinAlpha[iv - 1],
             mCosAlpha[iv - 1], mXZfac[iv - 1], mYZfac[iv - 1], delZ, lsliceStart,
             lgpuEnd, mIthickReproj, mXcenOut, tmpCenIn, mMinXreproj,
             mXprojOffset, mYcenOut, mMinYreproj, mYprojOffset, mCenterSlice,
             mIfAlpha, mDmeanIn);
        } else {
          //
          // GPU with local alignments: fill warpDelz array for all lines
          for (line = lsliceStart; line <= lgpuEnd; line++) {
            fillWarpDelz(&mWarpDelz[(line - lsliceStart) * mNumWarpDelz], iv, line);
          }
          //
          // Get the xprojmin and max adjusted by 5
          xprojMin = 10000000.;
          xprojMax = 0.;
          for (load = inLoadStart; load <= inLoadEnd; load++) {
            iys = B3DNINT(load + mYprojOffset);
            for (ix = 1; ix <= nxLoad; ix += nxLoad - 1) {
              localProjFactors(ix + mMinXload - 1, iys, iv, sinBet, 
                  cosBet, sinAlph, cosAlph);
              sinAlph = sinBet + (1 - ycenAdj) * cosBet;
              cosAlph = sinBet + (mIthickReproj - ycenAdj) * cosBet;
              
              ACCUM_MIN(xprojMin, sinAlph - 5.);
              ACCUM_MIN(xprojMin, cosAlph - 5.);
              ACCUM_MAX(xprojMax, sinAlph + 5.);
              ACCUM_MAX(xprojMax, cosAlph + 5.);
            }
          }
          // print *,'xprojmin, max', xprojMin, xprojMax
          //
          // Do it
          indJump = gpuReprojLocal
            (mReprojLines, mSinBeta[iv - 1], mCosBeta[iv - 1], mSinAlpha[iv - 1],
             mCosAlpha[iv - 1], mXZfac[iv - 1], mYZfac[iv - 1], mNxWarp, mNyWarp, 
             mIxStartWarp, mIyStartWarp, mIdelXwarp, mIdelYwarp, mWarpDelz, 
             mNumWarpDelz, mDxWarpDelz, xprojMin, xprojMax, lsliceStart, lgpuEnd, 
             mIthickReproj, iv, mXcenOut, mXcenIn, mAxisXoffset, mMinXload, 
             mXprojOffset, ycenAdj, mYprojOffset, mCenterSlice, mDmeanIn);
        }
        if (indJump != 0)
          break;
        wallCumul = wallCumul + wallTime() - wallStart;
        writeReprojLines(iv, lsliceStart, lgpuEnd, dmin, dmax, dtot8);
        wallStart = wallTime();
      }
      if (indJump == 0) {
        if (mDebug)
          printf("GPU reprojection time %.5f\n", wallCumul);
        lsliceEnd = lgpuEnd;
        return;
      }
    }
  }
  //
  // CPU REPROJECTION: loop on views; first handle non-local alignments
  for (iv = 1; iv <= mNumViews; iv++) {
    if (mThreshPolarity != 0.) {
      thresholdedReproj(iv, lsliceStart, lsliceEnd, inLoadStart, inLoadEnd);
    } else if (mNxWarp == 0) {
      //
      // Get the delta z for this view
      cosAlph = mCosAlpha[iv - 1];
      sinAlph = mSinAlpha[iv - 1];
      cosBet = mCosBeta[iv - 1];
      sinBet = mSinBeta[iv - 1];
      delZ = reprojDelZ(sinBet, cosBet, sinAlph, cosAlph, mXZfac[iv - 1], mYZfac[iv - 1]);
      // print *,sbeta, cbeta, salf, calf, xzfac(iv), yzfac(iv)
      // print *,delx, delz
      //
      // Loop on the output lines to be done
      for (line = lsliceStart; line <= lsliceEnd; line++) {
        lineBase = (line - lsliceStart) * mIwidth + 1;
        reprojOneAngle(mInputArray, &mReprojLines[lineBase - 1], 
                       inLoadStart, inLoadEnd, line, cosBet, sinBet, cosAlph, sinAlph, 
                       delZ, mIwidth, mIthickReproj, mInPlaneSize, nxLoad, mMinXreproj, 
                       mMinYreproj, mXprojOffset, mYprojOffset, mXcenOut, mYcenOut, 
                       mXcenIn + mAxisXoffset, mCenterSlice, mIfAlpha, mXZfac[iv - 1], 
                       mYZfac[iv - 1], mDmeanIn);
      }
    } else {
      //
      // LOCAL ALIGNMENTS
      //
      // first step: precompute all the x/yprojf/z  for all slices
      // general BUG ycenAdj replaces ycenOut - minYreproj (off by 1)
      xprojMin = 10000000.;
      xprojMax = 0.;
      for (load = inLoadStart; load <= inLoadEnd; load++) {
        indBase = nxLoad * (load - inLoadStart);
        iys = B3DNINT(load + mYprojOffset);
        for (ix = 1; ix <= nxLoad; ix++) {
          ind = indBase + ix;
          localProjFactors(ix + mMinXload - 1, iys, iv, mXprojFs[ind - 1], 
              mXprojZs[ind - 1], mYprojFs[ind - 1], mYprojZs[ind - 1]);
          if (ix == 1) {
            ACCUM_MIN(xprojMin, mXprojFs[ind - 1] + (1 - ycenAdj) * mXprojZs[ind - 1]);
            ACCUM_MIN(xprojMin, mXprojFs[ind - 1] + (mIthickReproj - ycenAdj) * 
                      mXprojZs[ind - 1]);
          }
          if (ix == nxLoad) {
            ACCUM_MAX(xprojMax, mXprojFs[ind - 1] + (1 - ycenAdj) * mXprojZs[ind - 1]);
            ACCUM_MAX(xprojMax, mXprojFs[ind - 1] + (mIthickReproj - ycenAdj) *
                      mXprojZs[ind - 1]);
          }
        }
      }
      // print *,'xprojmin, max', xprojMin, xprojMax
      //
      // loop on lines to be done
      for (line = lsliceStart; line <= lsliceEnd; line++) {
        lineBase = (line - lsliceStart) * mIwidth;
        fillWarpDelz(mWarpDelz, iv, line);
        // print *,iv, line, inloadstr, inloadend
        //
        // loop on pixels across line
        yproj = line + mYprojOffset;
        for (ixp = 1; ixp <= mIwidth; ixp++) {
          //
          // Get x projection coord, starting centered Z coordinate, and
          // approximate x and y coordinates
          // xproj, yproj are coordinates in original projections
          // Equations relate them to coordinates in reconstruction
          // and then X coordinate is adjusted to be a loaded X index
          // and Y coordinate is adjusted to be a slice of reconstruction
          xproj = ixp + mXprojOffset;
          zz = 1. -ycenAdj;
          sum = 0.;
          // print *,ixp, xproj, yproj, xx, yy
          // BUG these lines needed to be swapped and yprojOffset deferred
          yy = (yproj + zz * (mSinAlpha[iv - 1] - mYZfac[iv - 1]) - mCenterSlice) / 
            mCosAlpha[iv - 1] + mCenterSlice;
          xx = (xproj - yy * mSinAlpha[iv - 1] * mSinBeta[iv - 1] -  
                zz * (mCosAlpha[iv - 1] * mSinBeta[iv - 1] + mXZfac[iv - 1]) -
                mXcenIn - mAxisXoffset) / mCosBeta[iv - 1] + mXcenOut - (mMinXload - 1);
          yy = yy - mYprojOffset;
          //
          // Move on ray up in Z
          lastZdone = 0;
          tryJump = true;
          diffXmax = 0;
          diffYmax = 0;

          zJump = xJump * mCosBeta[iv - 1] / B3DMAX(0.2, B3DABS(mSinBeta[iv - 1]));
          while (zz < mIthickReproj + 1 - ycenAdj && lastZdone == 0) {
            if (xproj < xprojMin - 5. || xproj > xprojMax + 5.) {
              sum = sum + mDmeanIn;
            } else {
              loadedProjectingPoint(xproj, yproj, zz, nxLoad, 
                  inLoadStart, inLoadEnd, xx, yy);
              //
              // If X or Y is out of bounds, fill with mean
              if (yy < inLoadStart - yEndTol || yy > inLoadEnd + yEndTol 
                  || xx < 1. || xx >= nxLoad) {
                sum = sum + mDmeanIn;
              } else {
                //
                // otherwise, get x, y, z indexes, clamp y to limits, allow
                // a fractional Z pixel at top of volume
                ix = xx;
                fx = xx - ix;
                oneMfx = 1. - fx;
                yy = B3DMAX((float)inLoadStart, B3DMIN(inLoadEnd - 0.01, yy));
                iy = yy;
                fy = yy - iy;
                oneMfy = 1. - fy;
                // BUG ????  Shouldn't this be + ycenOut - minYreproj?
                iz = B3DMAX(1., zz + ycenAdj);
                fz = zz + ycenAdj - iz;
                oneMfz = 1. - fz;
                if (iz == mIthickReproj) {
                  iz = iz - 1;
                  fz = oneMfz;
                  oneMfz = 0.;
                  lastZdone = 1;
                }
                //
                // Do the interpolation
                d11 = oneMfx * oneMfy;
                d12 = oneMfx * fy;
                d21 = fx * oneMfy;
                d22 = fx * fy;
                ind = mInPlaneSize * (iy - inLoadStart) + (iz - 1) * nxLoad + ix;
                sum = sum + oneMfz * (d11 * mInputArray[ind - 1] 
                    + d12 * mInputArray[ind + mInPlaneSize - 1] + d21 * mInputArray[ind] 
                    + d22 * mInputArray[ind + mInPlaneSize]) 
                    + fz * (d11 * mInputArray[ind + nxLoad - 1] 
                    + d12 * mInputArray[ind + mInPlaneSize + nxLoad - 1] 
                    + d21 * mInputArray[ind + nxLoad] 
                    + d22 * mInputArray[ind + mInPlaneSize + nxLoad]);
                //
                if (tryJump) {
                  projSumLocal(xx, yy, zz, sum, xproj, yproj, mSinBeta[iv - 1], nxLoad,
                               inLoadStart, inLoadEnd,  zJump, ycenAdj);
                }
              }
            }
            //
            // Adjust Z by local factor, move X approximately for next pixel
            ind = B3DMAX(1., B3DMIN((float)mNumWarpDelz, xx / mDxWarpDelz));
            zz = zz + mWarpDelz[ind - 1];
            xx = xx + mSinBeta[iv - 1];
          }
          mReprojLines[lineBase + ixp - 1] = sum;
          // write (*,'(i5,2f10.4)') ixp, diffxmax, diffymax
        }
      }
    }
    wallCumul = wallCumul + wallTime() - wallStart;
    writeReprojLines(iv, lsliceStart, lsliceEnd, dmin, dmax, dtot8);
    wallStart = wallTime();
  }
  if (mDebug) 
    printf("CPU reprojection time %9.5f\n", wallCumul);
}

//
// compute delta z as function of X across the loaded slice
// which is not ideal since the data will not be coming from slice
//
void Tilt::fillWarpDelz(float *warpDelz, int iv, int line)
{
  float xx, f1, f2, f3, f4;
  int ixc, iys, ind1, ind2, ind3, ind4, i;

  iys = B3DNINT(line + mYprojOffset);
  for (i = 1; i <= mNumWarpDelz; i++) {
    xx = 1 + mDxWarpDelz * (i - 1);
    ixc = B3DNINT(xx + mMinXload - 1 - mXcenOut + mXcenIn + mAxisXoffset);
    localFactors(ixc, iys, iv, ind1, ind2, ind3, ind4, f1, f2, f3, f4);
    warpDelz[i - 1] = 
      f1 * reprojDelZ(mSWarpBeta[ind1 - 1], mCWarpBeta[ind1 - 1], mSWarpAlpha[ind1 - 1], 
                      mCWarpAlpha[ind1 - 1], mWarpXZfac[ind1 - 1], mWarpYZfac[ind1 - 1]) +
      f2 * reprojDelZ(mSWarpBeta[ind2 - 1], mCWarpBeta[ind2 - 1], mSWarpAlpha[ind2 - 1],
                      mCWarpAlpha[ind2 - 1], mWarpXZfac[ind2 - 1], mWarpYZfac[ind2 - 1]) +
      f3 * reprojDelZ(mSWarpBeta[ind3 - 1], mCWarpBeta[ind3 - 1], mSWarpAlpha[ind3 - 1],
                      mCWarpAlpha[ind3 - 1], mWarpXZfac[ind3 - 1], mWarpYZfac[ind3 - 1]) +
      f4 * reprojDelZ(mSWarpBeta[ind4 - 1], mCWarpBeta[ind4 - 1], mSWarpAlpha[ind4 - 1],
                      mCWarpAlpha[ind4 - 1], mWarpXZfac[ind4 - 1], mWarpYZfac[ind4 - 1]);
  }
  // print *,'got delz eg:', wrpdlz(1), wrpdlz(numWarpDelz/2), &
  // wrpdlz(numWarpDelz)
}

//
// Does a reprojection only of discrete points beyond a threshold
//
void Tilt::thresholdedReproj(int iv, int lsliceStart, int lsliceEnd, int inLoadStart, 
                             int inLoadEnd)
{
  float polarity, f11, f12, f21, f22, rlX, rlSlice, rlZ;
  int numLines, iyp, loadedSlice, ix, iy, ind, ind1, ind2, ixp;
  float xproj, yproj, fx, fy;
  int nxLoad = mMaxXload + 1 - mMinXload;
  numLines = lsliceEnd + 1 - lsliceStart;
  for (ind = 0; ind < numLines * mIwidth; ind++)
    mReprojLines[ind] = mThreshFillVal * mIthickReproj;
  polarity = B3DSIGN(1., mThreshPolarity);
  for (loadedSlice = inLoadStart; loadedSlice <= inLoadEnd; loadedSlice++) {
    ind = (loadedSlice - inLoadStart) * mInPlaneSize;
    for (iy = 1; iy <= mIthickReproj; iy++) {
      for (ix = 1; ix <= nxLoad; ix++) {
        if (polarity * (mInputArray[ind] - mThreshForReproj) >= 0) {
          //
          // Get real coordinate position within full reconstruction and projection
          // position in full aligned stack
          rlX = ix + mMinXreproj - 1;
          rlSlice = loadedSlice;
          rlZ = iy + mMinYreproj - 1;
          projectionPosition(iv, rlX, rlZ, rlSlice, xproj, yproj, ind1, ind2, 
              f11, f12, f21, f22);
          //
          // Adjust for position in reprojection being produced then adjust Y to
          // be an index in the lines being produced
          xproj = xproj - mXprojOffset;
          yproj = yproj - (mMinZreproj - 1 + mYprojOffset);
          yproj = yproj + mIsliceStart - lsliceStart;
          ixp = xproj;
          iyp = yproj;
          if (ixp >= 0 && ixp <= mIwidth && iyp >= 0 && iyp <= numLines) {
            //
            // If any of the 4 actual pixels is in range, get the interpolation factors
            // for those 4 surrounding pixels
            fx = xproj - ixp;
            fy = yproj - iyp;
            f11 = (1. - fx) * (1. - fy);
            f12 = (1. - fx) * fy;
            f21 = fx * (1. - fy);
            f22 = fx * fy;
            ind1 = ixp + mIwidth * (iyp - 1);
            //
            // If NOT summing, just mark any pixel with fraction above threshold
            // Otherwise add the fraction times the value
            if (mThreshSumFac < 1.) {
              if (f11 >= mThreshSumFac && ixp > 0 && iyp > 0) 
                  mReprojLines[ind1 - 1] = mThreshMarkVal;
              if (f12 >= mThreshSumFac && ixp > 0 && iyp < numLines) 
                  mReprojLines[ind1 + mIwidth - 1] = mThreshMarkVal;
              if (f21 >= mThreshSumFac && ixp < mIwidth && iyp > 0) 
                  mReprojLines[ind1 + 1 - 1] = mThreshMarkVal;
              if (f22 >= mThreshSumFac && ixp < mIwidth && iyp < numLines) 
                  mReprojLines[ind1 + 1 + mIwidth - 1] = mThreshMarkVal;
            } else {
              if (ixp > 0 && iyp > 0) mReprojLines[ind1 - 1] = 
                  mReprojLines[ind1 - 1] + f11 * mThreshMarkVal - mThreshFillVal;
              if (ixp > 0 && iyp < numLines) mReprojLines[ind1 + mIwidth - 1] = 
                  mReprojLines[ind1 + mIwidth - 1] + f12 * mThreshMarkVal - mThreshFillVal;
              if (ixp < mIwidth && iyp > 0) mReprojLines[ind1 + 1 - 1] = 
                  mReprojLines[ind1 + 1 - 1] + f21 * mThreshMarkVal - mThreshFillVal;
              if (ixp < mIwidth && iyp < numLines) mReprojLines[ind1 + 1 + mIwidth - 1]=
                  mReprojLines[ind1 + 1 + mIwidth - 1] + f22 * mThreshMarkVal -  
                  mThreshFillVal;
            }
          }
        }
        ind = ind + 1;
      }
    }
  }
}

//
// reprojOneAngle reprojects a line at one angle from projection data
// in ARRAY into reprojLines.  LINE is the Y value in projections, Z value
// in reconstructed slices.  ARRAY is loaded with slices from inLoadStart to
// inLoadEnd, with a slice size of inPlaneSize and NXLOAD values on each line
// in X.  COSBET, SINBET, COSALPH, SINALPH are cosines and sines of tilt angle
// and alpha tilt.  IFALPHA is non-zero for tilt araound the X axis.
// iwidth is the width and ithickReproj is the thickness to reprojection;
// the coordinates of the region in the slice to reproject start at
// minXreproj, minYreproj and are offset from center by xprojOffset,
// yprojOffset.  DELZIN is the spacing in Y at which to sample points along
// a projection ray. XCENout, YCENout are center coordinates of output;
// xcenPdelxx is xcenIn + axisXoffset; centerSlice is the middle coordinate in Z
// XZFACVIEW, YZFACVIEW are Z factors for this view, and PMEAN is the mean of
// the slices for filling.
// Many of these names are the same as in the rest of the program and
// have the same meaning, but they are passed in, not taken from the
// tiltvars module, so that this can be called in cases other than the
// reprojectRec where they are defined.
//
void Tilt::reprojOneAngle
(float *array, float *reprojLines, int inLoadStart, int inLoadEnd, 
 int line, float cosBet, float sinBet, float cosAlph, float sinAlph,
 float delzIn, int iwidth, int ithickReproj, int inPlaneSize, 
 int nxLoad, int minXreproj, int minYreproj, float xprojOffset, 
 float yprojOffset, float xcenOut, float ycenOut, float xcenPdelxx,
 float centerSlice, int ifAlpha, float xzFacView, float yzFacView,
 float dmeanIn)
{
  float delX;
  //
  int ix, iz, i, numZ, kz, iys, ixEnd, ixStart, ind, indBase, indv;
  float zNum, fz, oneMfz, zz, xx, fx, yEndTol, pfill, salfSbetOverCalf, xcenAdj, yslc;
  float oneMfx, yy, fy, oneMfy, xproj, yproj, ySlice, d11, d12, d21, d22, delY;
  double xx8, yy8, zz8;
  int numX, kx, ixyOKstart, ixyOKend, iyFix, ifixStart, ifixEnd;
  float delZ, xNum, eps;

  yEndTol = 3.05; eps = 0.01;
  for (indv = 0; indv < iwidth; indv++)
    reprojLines[indv] = 0.;
  if (B3DABS(sinBet * ithickReproj) <= B3DABS(cosBet * iwidth)) {
    //
    delZ = delzIn;
    delX = 1. / cosBet;
    zNum = 1. + (ithickReproj - 1) / delZ;
    numZ = zNum;
    if (zNum - numZ >= 0.1) 
      numZ = numZ + 1;
    //
    // Loop up in Z through slices, adding in lines of data to the
    // output line
    for (kz = 1; kz <= numZ; kz++) {
      zz = 1 + (kz - 1) * delZ;
      iz = zz;
      fz = zz - iz;
      oneMfz = 1. - fz;
      pfill = dmeanIn;
      //
      // If Z is past the top, drop back one line and set up fractions
      // to take just a fraction of the top line
      if (zz >= ithickReproj) {
        zz = ithickReproj;
        iz = ithickReproj - 1;
        fz = oneMfz;
        oneMfz = 0.;
        pfill = dmeanIn * fz;
      }
      zz = zz + minYreproj - 1 - ycenOut;
      //
      // Get y slice for this z value
      yproj = line + yprojOffset;
      yy = (yproj + zz * (sinAlph - yzFacView) - centerSlice) / cosAlph;
      ySlice = yy + centerSlice - yprojOffset;
      if (ifAlpha == 0)
        ySlice = line;
      // if (line==591) print *,kz, zz, iz, fz, omfz, yproj, yy, yslice
      if (ySlice < inLoadStart - yEndTol || ySlice > inLoadEnd + yEndTol) {
        //
        // Really out of bounds, do fill
        // if (line==591) print *,'Out of bounds, view, line, zz', line, zz
        for (indv = 0; indv < iwidth; indv++)
          reprojLines[indv] += pfill;

      } else {
        //
        // otherwise set up iy and interpolation factors
        iys = floor(ySlice);
        if (ifAlpha != 0) {
          if (iys < inLoadStart) {
            iys = inLoadStart;
            fy = 0.;
          } else if (iys >= inLoadEnd) {
            iys = inLoadEnd - 1;
            fy = 1.;
          } else {
            fy = ySlice - iys;
          }
          oneMfy = 1. - fy;
        }
        //
        // Now get starting X coordinate, fill to left
        xproj = 1 + xprojOffset;
        xx = (xproj - (yy * sinAlph * sinBet + zz * (cosAlph * sinBet + 
            xzFacView) + xcenPdelxx)) / cosBet + xcenOut - (minXreproj - 1);
        ixStart = 1;
        if (xx < 1) {
          ixStart = ceil((1. - xx) / delX + 1.);
        } else if (xx >= iwidth) {
          ixStart = ceil((iwidth - xx) / delX + 1.);
        }
        xx = xx + (ixStart - 1) * delX;
        if (xx < 1 || xx >= iwidth) {
          ixStart = ixStart + 1;
          xx = xx + delX;
        }
        if (ixStart > 1) 
          for (indv = 0; indv < ixStart - 1; indv++)
            reprojLines[indv] += pfill;
        //
        // get ending X coordinate, fill to right
        ixEnd = iwidth;
        if (xx + (ixEnd - ixStart) * delX >= iwidth - eps) {
          ixEnd = (iwidth - xx) / delX + ixStart;
          if (xx + (ixEnd - ixStart) * delX >= iwidth - eps) 
            ixEnd = ixEnd - 1;
        } else if (xx + (ixEnd - ixStart) * delX < 1 + eps) {
          ixEnd = (1. - xx) / delX + ixStart;
          if (xx + (ixEnd - ixStart) * delX < 1 + eps)
            ixEnd = ixEnd - 1;
        }
        if (ixEnd < iwidth) 
          for (indv = ixEnd; indv < iwidth; indv++)
            reprojLines[indv] += pfill;

        // if (line == lsStart) write(*,'(3i6,3f11.3)') iv, ixst, ixnd, xx, &
        // (ixst+xprojOffset - &
        // (yy * salf * sbeta + zz * (calf * sbeta + &
        // xzfacv) + xcenPdelxx)) / cbeta + xcenOut - (minXreproj-1) &
        // , (ixnd+xprojOffset - &
        // (yy * salf * sbeta + zz * (calf * sbeta + &
        // xzfacv) + xcenPdelxx)) / cbeta + xcenOut - (minXreproj-1)
        //
        // Add the line in: do simple 2x2 interpolation if no alpha
        indBase = 1 + inPlaneSize * (iys - inLoadStart) + (iz - 1) * nxLoad;
        // if (line==591) print *,ixst, ixnd
        xx8 = xx;
        if (ifAlpha == 0) {
          for (i = ixStart; i <= ixEnd; i++) {
            ix = xx8;
            fx = xx8 - ix;
            oneMfx = 1. - fx;
            ind = indBase + ix - 1;
            reprojLines[i - 1] += 
              oneMfz * oneMfx * array[ind - 1] +
              oneMfz * fx * array[ind] + 
              fz * oneMfx * array[ind + nxLoad - 1] + 
              fz * fx * array[ind + nxLoad];

            // if (line==591.and.i==164) print *,reprojLines(i), array(ind), &
            // array(ind + 1), array(ind + nxload), array(ind + nxload + 1)
            xx8 = xx8 + delX;
          }
        } else {
          //
          // Or do the full 3D interpolation if any variation in Y
          for (i = ixStart; i <= ixEnd; i++) {
            ix = xx8;
            fx = xx8 - ix;
            oneMfx = 1. - fx;
            d11 = oneMfx * oneMfy;
            d12 = oneMfx * fy;
            d21 = fx * oneMfy;
            d22 = fx * fy;
            ind = indBase + ix - 1;
            reprojLines[i - 1] += 
                oneMfz * (d11 * array[ind - 1] 
                          + d12 * array[ind + inPlaneSize - 1] + d21 * array[ind] 
                          + d22 * array[ind + inPlaneSize]) 
              + fz * (d11 * array[ind + nxLoad - 1] 
                      + d12 * array[ind + inPlaneSize + nxLoad - 1] 
                      + d21 * array[ind + nxLoad] 
                      + d22 * array[ind + inPlaneSize + nxLoad]);
            xx8 = xx8 + delX;
          }
        }
      }
    }

  } else {
    //
    // angles higher than the corner angle need to be done in vertical lines,
    // outer loop on X instead of z
    // Spacing between vertical lines is now sine beta
    // The step between pixels along a line is 1/sin beta with no alpha tilt,
    // The alpha tilt compresses it by the delta Z factor divided by cosine
    // beta, the amount that delta Z factor is compressed from cosine beta.
    delX = B3DABS(sinBet);
    xNum = 1. + (iwidth - 1) / delX;
    numX = xNum;
    if (xNum - numX >= 0.1)
      numX = numX + 1;
    delZ = delzIn / (sinBet * B3DABS(cosBet));
    delY = delZ * (sinAlph - yzFacView) / cosAlph;
    if (B3DABS(delY) < 1.e-10) 
      delY = 0.;
    // print *,'delx, dely, delz', delx, dely, delz
    //
    // Loop in X across slices, adding in vertical lines of data to the output line
    for (kx = 1; kx <= numX; kx++) {
      xx = 1 + (kx - 1) * delX;
      ix = xx;
      fx = xx - ix;
      oneMfx = 1. - fx;
      pfill = dmeanIn;
      //
      // If X is past the end, drop back one line and set up fractions
      // to take just a fraction of the right column
      if (xx >= iwidth) {
        xx = iwidth;
        ix = iwidth - 1;
        fx = oneMfx;
        oneMfx = 0.;
        pfill = dmeanIn * fx;
      }

      // get starting Z coordinate
      salfSbetOverCalf = sinAlph * sinBet / cosAlph;
      xcenAdj = xcenOut - (minXreproj - 1);
      xproj = 1 + xprojOffset;
      yproj = line + yprojOffset;

      zz = (xproj - (yproj - centerSlice) * salfSbetOverCalf - xcenPdelxx -  
          (xx - xcenAdj) * cosBet) / ((sinAlph - yzFacView) * salfSbetOverCalf +  
          cosAlph * sinBet + xzFacView);
      //
      // Get y slice for this z value, then convert Z to be index coordinates in slice
      yy = (yproj + zz * (sinAlph - yzFacView) - centerSlice) / cosAlph;
      ySlice = yy + centerSlice - yprojOffset;
      if (ifAlpha == 0)
        ySlice = line;
      zz = zz - (minYreproj - 1 - ycenOut);
      //
      // Get starting X proj limit based on Z
      ixStart = 1;
      if (zz < 1.) {
        ixStart = ceil((1. - zz) / delZ + 1.);
      } else if (zz >= ithickReproj) {
        ixStart = ceil((ithickReproj - zz) / delZ + 1.);
      }
      //
      // Revise starting limit for Y
      if (ifAlpha != 0 && delY != 0) {
        yslc = ySlice + (ixStart - 1.) * delY;
        if (yslc < inLoadStart - yEndTol) {
          ixStart = ceil((inLoadStart - yEndTol - ySlice) / delY + 1.);
        } else if (yslc > inLoadEnd + yEndTol) {
          ixStart = ceil((inLoadEnd + yEndTol - ySlice) / delY + 1.);
        }
      }
      //
      // Adjust Z start for final start and make sure it works, adjust Y also
      zz = zz + (ixStart - 1.) * delZ;
      if (zz < 1. || zz >= ithickReproj) {
        zz = zz + delZ;
        ixStart = ixStart + 1;
      }
      ySlice = ySlice + (ixStart - 1.) * delY;
      if (ifAlpha == 0)
        ySlice = line;
      //
      // get ending coordinate based on limits in Z and Y
      ixEnd = iwidth;
      if (zz + (ixEnd - ixStart) * delZ >= ithickReproj - eps) {
        ixEnd = (ithickReproj - zz) / delZ + ixStart;
        if (zz + (ixEnd - ixStart) * delZ >= ithickReproj - eps) 
          ixEnd = ixEnd - 1;
      } else if ( zz + (ixEnd - ixStart) * delZ < 1. + eps) {
        ixEnd = (1. - zz) / delZ + ixStart;
        if (zz + (ixEnd - ixStart) * delZ < 1. + eps) 
          ixEnd = ixEnd - 1;
      }
      if (ifAlpha != 0 && delY != 0) {
        yslc = ySlice + (ixEnd - ixStart) * delY;
        if (yslc < inLoadStart - yEndTol) {
          ixEnd = (inLoadStart - yEndTol - ySlice) / delY + ixStart;
        } else if (yslc > inLoadEnd + yEndTol) {
          ixEnd = (inLoadEnd + yEndTol - ySlice) / delY + ixStart;
        }
      }
      //
      // Now get X indexes within which Y can safely be varied
      ixyOKstart = ixStart;
      ixyOKend = ixEnd;
      if (ifAlpha != 0 && delY != 0.) {
        if (ySlice < inLoadStart) {
          ixyOKstart = ceil((inLoadStart - ySlice) / delY + ixStart);
          if (ySlice + (ixyOKstart - ixStart) * delY < inLoadStart + eps) 
              ixyOKstart = ixyOKstart + 1;
        } else if (ySlice >= inLoadEnd) {
          ixyOKstart = ceil((inLoadEnd - ySlice) / delY + ixStart);
          if (ySlice + (ixyOKstart - ixStart) * delY >= inLoadEnd - eps) 
              ixyOKstart = ixyOKstart + 1;
        }
        ySlice = ySlice + (ixyOKstart - ixStart) * delY;
        //
        yslc = ySlice + (ixEnd - ixyOKstart) * delY;
        if (yslc < inLoadStart) {
          ixyOKend = (inLoadStart - ySlice) / delY + ixyOKstart;
          if (ySlice + (ixyOKend - ixyOKstart) * delY < inLoadStart + eps) 
              ixyOKend = ixyOKend-1;
        } else if (yslc >= inLoadEnd) {
          ixyOKend = (inLoadEnd - ySlice) / delY + ixyOKstart;
          if (ySlice + (ixyOKend - ixyOKstart) * delY >= inLoadEnd - eps) 
              ixyOKend = ixyOKend-1;
        }
      }
      // write( *,'(i5,f7.1,4i5,2f7.1)') kx, xx, ixst, ixyOKst, ixyOKnd, ixnd, zz, &
      // zz+(ixnd-ixst)*delz
      //
      // Do the fills
      if (ixStart > 1) 
        for (indv = 0; indv < ixStart - 1; indv++)
          reprojLines[indv] += pfill;
      if (ixEnd < iwidth) 
        for (indv = ixEnd; indv < iwidth; indv++)
          reprojLines[indv] += pfill;
      //
      // Add the line in: do simple 2x2 interpolation if no alpha
      // if (line==591) print *,ixst, ixnd
      if (ifAlpha == 0) {
        zz8 = zz;
        indBase = 1 + inPlaneSize * (line - inLoadStart) + ix - 1;
        for (i = ixStart; i <= ixEnd; i++) {
          iz = zz8;
          fz = zz8 - iz;
          oneMfz = 1. - fz;
          ind = indBase + (iz - 1) * nxLoad;
          reprojLines[i - 1] +=  
              oneMfz * oneMfx * array[ind - 1] + 
              oneMfz * fx * array[ind] + 
              fz * oneMfx * array[ind + nxLoad - 1] + 
              fz * fx * array[ind + nxLoad];
          // if (i==70) print *,reprojLines(i)
          // if (line==591.and.i==164) print *,reprojLines(i), array(ind), &
          // array(ind + 1), array(ind + nxload), array(ind + nxload + 1)
          zz8 = zz8 + delZ;
        }
      } else {
        //
        // Or do the full 3D interpolation if any variation in Y, starting with
        // the loop where Y varies
        yy8 = ySlice;
        zz8 = zz + (ixyOKstart - ixStart) * delZ;
        indBase = 1 - inPlaneSize * inLoadStart + ix - 1;
        for (i = ixyOKstart; i <= ixyOKend; i++) {
          iz = zz8;
          fz = zz8 - iz;
          oneMfz = 1. - fz;
          iys = yy8;
          fy = yy8 - iys;
          oneMfy = 1. - fy;
          d11 = oneMfx * oneMfy;
          d12 = oneMfx * fy;
          d21 = fx * oneMfy;
          d22 = fx * fy;
          ind = indBase + inPlaneSize * iys + (iz - 1) * nxLoad;
          reprojLines[i - 1] +=
            oneMfz * (d11 * array[ind - 1] 
                      + d12 * array[ind + inPlaneSize - 1] + d21 * array[ind] 
                      + d22 * array[ind + inPlaneSize]) 
            + fz * (d11 * array[ind + nxLoad - 1] 
                    + d12 * array[ind + inPlaneSize + nxLoad - 1] 
                    + d21 * array[ind + nxLoad] 
                    + d22 * array[ind + inPlaneSize + nxLoad]);
          zz8 = zz8 + delZ;
          yy8 = yy8 + delY;
        }
        //
        // Now do special loops with Y fixed - do the one at the end first
        // since Y and Z are all set for that
        ifixStart = ixyOKend + 1;
        ifixEnd = ixEnd;
        for (iyFix = 1; iyFix <= 2; iyFix++) {
          for (i = ifixStart; i <= ifixEnd; i++) {
            iz = zz8;
            fz = zz8 - iz;
            oneMfz = 1. - fz;
            d11 = oneMfx * oneMfy;
            d12 = oneMfx * fy;
            d21 = fx * oneMfy;
            d22 = fx * fy;
            ind = indBase + inPlaneSize * iys + (iz - 1) * nxLoad;
            reprojLines[i - 1] += 
                oneMfz * (d11 * array[ind - 1] 
                + d12 * array[ind + inPlaneSize - 1] + d21 * array[ind] 
                + d22 * array[ind + inPlaneSize]) 
                + fz * (d11 * array[ind + nxLoad - 1] 
                + d12 * array[ind + inPlaneSize + nxLoad - 1] 
                + d21 * array[ind + nxLoad] 
                + d22 * array[ind + inPlaneSize + nxLoad]);
            zz8 = zz8 + delZ;
          }
          //
          // Set up for loop with Y fixed at start, reset y and z
          yy8 = ySlice;
          zz8 = zz;
          iys = yy8;
          fy = yy8 - iys;
          oneMfy = 1. - fy;
          ifixStart = ixStart;
          ifixEnd = ixyOKstart - 1;
        }
      }
    }
  }
}

//
// Writes line LINE for view IV of a reprojection
//
void Tilt::writeReprojLines(int iv, int lineStart, int lineEnd, float &dmin, float &dmax, 
                            double &dtot8)
{
  int line, i, iyOut, numVals;
  float val;
  //
  // Write the line after scaling.  Scale log data to give approximately
  // constant mean levels.  Descale non-log data by exposure weights
  numVals = mIwidth * (lineEnd + 1 - lineStart);
  if (mIfLog != 0) {
    // Hopefully this works for local as well
    if (mThreshPolarity != 0.) {
      val = log10(mProjMean + mBaseForLog) - mIthickReproj * mDmeanIn;
    } else if (B3DABS(mSinBeta[iv - 1] * mIthickReproj) <= 
             B3DABS(mCosBeta[iv - 1] * mIwidth)) {
      val = log10(mProjMean + mBaseForLog) - mIthickReproj * mDmeanIn / 
        B3DABS(mCosBeta[iv - 1]);
    } else {
      val = log10(mProjMean + mBaseForLog) - mIwidth * mDmeanIn / B3DABS(mSinBeta[iv -1]);
    }
    if (mDebug)
      PRINT4(iv, lineStart, lineEnd, val);
    for (i = 0; i < numVals; i++) {
      mReprojLines[i] = pow(10.f, mReprojLines[i] + val) - mBaseForLog;
    }
  } else {
    val = mExposeWeight[iv - 1];
    if (mThreshPolarity != 0.) 
      val = mExposeWeight[(mNumViews + 1) / 2 - 1];
    for (i = 0; i < numVals; i++) {
      mReprojLines[i] = mReprojLines[i] / val;
    }
  }
  if (mProjSubtraction) {
    iiuSetPosition(1, iv - 1, lineStart - 1);
    if (iiuReadLines(1, (char *)mOrigLines, lineEnd + 1 - lineStart) != 0)  
      exitError("Reading from original projection file");
    for (i = 0; i < numVals; i++)
      mReprojLines[i] -= mOrigLines[i];
  }
  for (i = 0; i < numVals; i++) {
    val = mReprojLines[i];
    // if (debug .and. val < dmin) print *,'min:', i, val
    // if (debug .and. val > dmax) print *,'max:', i, val
    dmin = B3DMIN(dmin, val);
    dmax = B3DMAX(dmax, val);
    dtot8 = dtot8 + val;
  }
  for (line = lineStart; line <= lineEnd; line++) {
    iyOut = line - mIsliceStart;
    if (mMinTotSlice > 0)
      iyOut = line - mMinTotSlice;
    parWrtPosn(2, iv - 1, iyOut);
    parWrtLin(2, (char *)(&mReprojLines[(line - lineStart) * mIwidth]));
  }
}

//
// Projects model points onto the included views
//
void Tilt::projectModel(Imod *imod, char *outModel, char *outAngles, char *transformFile,
                        int numViewsOrig, char *defocusFile, float pixForDefocus, 
                        float focusInvert)
{
  int numPoints, fwBase, ipt, ip1, iv, nfv, numXfs;
  float oneVal, rlX, rlZ, rlSlice, yy, zz, xproj, yproj, zOffset, f11, f12, f21, f22;
  int j, lslice, imodObj, imodCont, ierr, size, ob, co, pt, index, after, skipBelow;
  Ipoint *pts;
  Istore store;
  Istore *item;
  Iobj *obj;
  MrcHeader *inputHead;
  float yz22, xprojf, xprojz, yprojf, yprojz, degToRad, ptDefocus, betaInv, zzp, zzpp;
  float fjlMat[2][2], f1234[4], gamma, stretch, strPhi, smag, gammaSum, betaSum, alphaSum;
  float ximScale = 1., yimScale = 1., zimScale = 1., xOffs = 0., yOffs = 0.;
  const float noValue = -1.e30, valueTest = -0.9e30;
  float zOffs = 0., obj1Thresh = noValue;
  float thresh, valMin, valMax;
  int ind1234[4], jInc, lsInc, ic;
  float *values, *coords, *alixf, *aliRot, *defocus;
  int *mapFileToView;
  FILE *angleFP;
  Imod *newMod;
  degToRad = RADIANS_PER_DEGREE;

  if (imod->flags & IMODF_FLIPYZ)
    imodFlipYZ(imod);

  // get header offsets and scales
  if (imod->refImage) {
    ximScale = imod->refImage->cscale.x;
    yimScale = imod->refImage->cscale.y;
    zimScale = imod->refImage->cscale.z;
    if (imod->flags & IMODF_OTRANS_ORIGIN) {
      xOffs = -imod->refImage->otrans.x;
      yOffs = -imod->refImage->otrans.y;
      zOffs = -imod->refImage->otrans.z;
    } else {
      xOffs = -imod->refImage->ctrans.x;
      yOffs = -imod->refImage->ctrans.y;
      zOffs = -imod->refImage->ctrans.z;
    }
  }
  //  add up the points
  numPoints = 0;
  for (ob = 0; ob < imod->objsize; ob++) {
    for (co = 0; co < imod->obj[ob].contsize; co++)
      numPoints += imod->obj[ob].cont[co].psize;
  }
    
  size = imod->obj[0].pdrawsize;
  if (!size)
    size = 5;
  //
  // Models are defined as having Z coordinates ranging from -0.5 to NZ - 0.5 so Z
  // will need to be shifted up by 1 to get to pixel index coordinates
  // But in old beadtrack models, Z started at 0 so the offset needs to be only 0.5
  // Recognize old beadtrack model by lack of flag AND original object name so that
  // other old models will work
  zOffset = 1.0;
  if (!(imod->flags & IMODF_Z_FROM_MINUSPT5) == 0 && 
      strstr(imod->obj[0].name, "Wimp no.") != NULL)
    zOffset = 0.5;
  j = numViewsOrig + 10;
  values = B3DMALLOC(float, numPoints + 10);
  coords = B3DMALLOC(float, 3 * numPoints + 30);
  alixf = B3DMALLOC(float, 6 * j);
  aliRot = B3DMALLOC(float, j);
  defocus = B3DMALLOC(float, j);
  mapFileToView = B3DMALLOC(int, mLimView);
  if (!(values && coords && alixf && aliRot && defocus && mapFileToView))
    memoryError(NULL, "arrays for reprojecting model");
  if (outAngles) {
    imodBackupFile(outAngles);
    angleFP = fopen(outAngles, "w");
    if (!angleFP)
      exitError("Opening angle output file %s", outAngles);
    if (transformFile) {
      numXfs = -6 * (numViewsOrig + 10);
      getValuesFromLines(NULL, transformFile, alixf, NULL, false, numXfs, "transforms");
      numXfs /= 6;
      if (numXfs > numViewsOrig)
        printf("\nWARNING: TILT - More alignment transforms than views in input stack\n");
      if (numXfs < numViewsOrig)
        exitError("Fewer alignment transforms than views in input stack");
      for (j = 0; j < numViewsOrig; j++) {
        amatToRotmagstr(alixf[6 * j], alixf[6 * j + 1], alixf[6 * j + 2], 
                        alixf[6 * j + 3], &aliRot[j], &smag, &stretch, &strPhi);
      }
    }
    //
    if (defocusFile) {
      getValuesFromLines(NULL, defocusFile, defocus, NULL, true, numViewsOrig, 
                         "defocus values");
      if (focusInvert == 0) {
        focusInvert = 1.;
      } else {
        focusInvert = -1.;
      }
    }
  }
  //
  // get each point and its contour value into the arrays
  numPoints = 0;
  for (ob = 0; ob < imod->objsize; ob++) {
    obj = &imod->obj[ob];
    skipBelow = 0;
    if ((obj->flags & IMOD_OBJFLAG_USE_VALUE) &&
        istoreGetMinMax(obj->store, obj->contsize, GEN_STORE_MINMAX1, &valMin, &valMax)) {
      thresh = obj->valblack * (valMax - valMin) / 255. + valMin;
      if (ob == 0)
        obj1Thresh = thresh;
      if (obj->matflags2 & MATFLAGS2_SKIP_LOW)
        skipBelow = mSkipUnseenPoints;
    }
    for (co = 0; co < imod->obj[ob].contsize; co++) {
      pts = imod->obj[ob].cont[co].pts;
      oneVal = noValue;
      index = istoreLookup(imod->obj[ob].store, co, &after);
      if (index >= 0) {
        for (j = index; j < after; j++) {
          item = istoreItem(imod->obj[ob].store, j);
          if (item->type == GEN_STORE_VALUE1) {
            oneVal = item->value.f;
            break;
          }
        }
      }
      if (oneVal > valueTest && skipBelow && oneVal < thresh)
        continue;
      for (pt = 0; pt < imod->obj[ob].cont[co].psize; pt++) {
        coords[3 * numPoints] = pts[pt].x;
        coords[1 + 3 * numPoints] = pts[pt].y;
        coords[2 + 3 * numPoints] = pts[pt].z;
        values[numPoints++] = oneVal;
      }
    }
  }
  //
  // Start a new model
  newMod = imodNew();
  if (!newMod)
    exitError("Allocating a new model structure");
  newMod->xmax = mNxProj;
  newMod->ymax = mNyProj;
  newMod->zmax = mNumViews;

  inputHead = iiuMrcHeader(1, "projectModel", 1, 0);
  if (imodSetRefImage(newMod, inputHead))
    exitError("Putting image reference information into output model");
  //
  // Build a map from views in file to ordered views in program
  for (nfv = 1; nfv <= numViewsOrig; nfv++) {
    mapFileToView[nfv - 1] = 0;
  }
  for (iv = 1; iv <= mNumViews; iv++) {
    mapFileToView[mMapUsedView[iv - 1] - 1] = iv;
  }
  //
  // Loop on the points, start new contour for each
  if (imodNewObject(newMod))
    exitError("Allocating new object in model");
  obj = newMod->obj;
  obj->cont = imodContoursNew(numPoints);
  memoryError(obj->cont, "contour array in model");
  obj->contsize = numPoints;
  store.flags = GEN_STORE_FLOAT << 2;
  store.type = GEN_STORE_VALUE1;

  for (ipt = 0; ipt < numPoints; ipt++) {
    store.value.f = values[ipt];
    store.index.i = ipt;
    if (values[ipt] > valueTest && istoreInsert(&obj->store, &store))
      exitError("Inserting value information into object");
    //
    // Get real pixel coordinates in tomogram file
    rlX = coords[3 * ipt] + 0.5;
    rlZ = coords[1 + 3 * ipt] + 0.5;
    rlSlice = coords[2 + 3 * ipt] + zOffset;
    //
    // This may never be tested but seems simple enough
    if (!mPerpendicular) {
      rlZ = coords[2 + 3 * ipt] + zOffset;
      rlSlice = coords[1 + 3 * ipt] + 0.5;
    }

    // Get point array
    pts = B3DMALLOC(Ipoint, numViewsOrig);
    memoryError(pts, "point array for contour");
    obj->cont[ipt].pts = pts;
    obj->cont[ipt].psize = numViewsOrig;
      
    //
    // Loop on the views in the file
    for (nfv = 1; nfv <= numViewsOrig; nfv++) {
      iv = mapFileToView[nfv - 1];
      if (iv > 0) {
        projectionPosition(iv, rlX, rlZ, rlSlice, xproj, yproj, j, lslice,  
            fjlMat[0][0], fjlMat[0][1], fjlMat[1][0], fjlMat[1][1]);
        if (outAngles) {
          betaSum = 0.;
          alphaSum = 0.;
          gammaSum = 0.;
          if (mNxWarp == 0) {
            alphaSum = mAlpha[iv - 1];
            betaSum = -mAngles[iv - 1] / degToRad;
          } else {
            for (jInc = 0; jInc < 2; jInc++) {
              for (lsInc = 0; lsInc < 2; lsInc++) {
                localFactors(B3DNINT(j + jInc - mXcenOut + mXcenIn + mAxisXoffset)
                    , lslice + lsInc,  nfv, ind1234[0], ind1234[1], 
                    ind1234[2], ind1234[3], f1234[0], f1234[1], f1234[2], f1234[3]);
                for (ic = 0; ic < 4; ic++)
                  f1234[ic] *= fjlMat[jInc][lsInc];
                for (ic = 0; ic < 4; ic++) {
                  fwBase = 6 * (ind1234[ic] - 1);

                  // These arguments are a11, a12, a21, a22 and in memory they are
                  // (1,1) (2,1) (1,2) (2,2), so swap middle terms
                  amatToRotmagstr(mFwarp[fwBase], mFwarp[fwBase + 2], mFwarp[fwBase + 1],
                                  mFwarp[fwBase + 3], &gamma, &smag, &stretch, &strPhi);
                  //
                  // alpha was left as degrees and with its native sign, with the negative
                  // taken when taking the sin; but tilt angle and delBeta were converted
                  // to radians and made negative
                  // The transform has the amount image needs to be rotated; need
                  // negative for rotation of specimen
                  alphaSum = alphaSum + f1234[ic] * (mAlpha[iv - 1] + 
                                                     mDelAlpha[ind1234[ic] - 1]);
                  betaSum = betaSum - f1234[ic] *  (mAngles[iv - 1] + 
                                                    mDelBeta[ind1234[ic] - 1]) / degToRad;
                  gammaSum = gammaSum - f1234[ic] * gamma;
                }
              }
            }
          }
          if (transformFile) 
            gammaSum = gammaSum - aliRot[nfv - 1];
          ptDefocus = 0.;
          if (defocusFile) {

            // Start with the centered Y position used in projectionPosition
            // but invert the centered Z position because the Z axis is pointing
            // down in the modeled X/Z planes and we want a correct Z
            // beta was inverted above from the stored (inverted) angles and is
            // now correct for working with true Z coordinates
            yy = rlSlice - mCenterSlice;
            zz = -(rlZ - mYcenModProj) * mCompress[iv - 1];
            betaInv = focusInvert * betaSum * degToRad;

            // Now get the Z' after the alpha tilt from Y and Z
            // then get the Z'' after the beta tilt from Z' and centered X
            // The right side swings to negative Z at positive tilt and this corresponds
            // to more underfocus (per Ctfplotter), so subtract the Z from defocus
            zzp = yy * sin(alphaSum * degToRad) + zz * cos(alphaSum * degToRad);
            zzpp = zzp * cos(betaInv) - (rlX - mXcenOut) * sin(betaInv);
            ptDefocus = defocus[nfv - 1] - pixForDefocus * zzpp;
          }
          fprintf(angleFP, "%6d %5d %5d %8.3f %8.3f %8.3f %8.0f\n",
                  ipt + 1, iv, nfv, alphaSum, betaSum, gammaSum, ptDefocus);
        }
        //
        // Store model coordinates
        pts[nfv - 1].x = xproj - 0.5;
        pts[nfv - 1].y = yproj - 0.5;
        pts[nfv - 1].z = nfv - 1.;
      }
    }
  }
  //
  // Save model
  
  //
  // Set to open contour, show values etc., and show sphere on section only
  obj->flags |= (IMOD_OBJFLAG_OPEN | IMOD_OBJFLAG_USE_VALUE | 
                         IMOD_OBJFLAG_PNT_ON_SEC);
  obj->matflags2 |= (MATFLAGS2_CONSTANT | MATFLAGS2_SKIP_LOW);
  obj->pdrawsize = size;
  istoreFindAddMinMax1(obj);

  // Transfer the threshold from the first object using the new min/max
  if (obj1Thresh > valueTest && istoreGetMinMax(obj->store, obj->contsize, 
                                                GEN_STORE_MINMAX1, &valMin, &valMax)) {
    ipt = B3DNINT((obj1Thresh - valMin) * 255. / (valMax - valMin));
    B3DCLAMP(ipt, 0, 255);
    obj->valblack = ipt;
  }

  if (outAngles) 
    fclose(angleFP);
  imodBackupFile(outModel);
  if (imodFileWrite(newMod, outModel))
    exitError("Writing new model to file %s", outModel);
  printf("%d points written to output model\n", numPoints * numViewsOrig);
  exit(0);
}

//
// Computes the projection position xproj, yproj on view iv of position rlX, rlZ,
// rlSlice in the reconstruction, where these are real pixel coordinates, equal to 1
// in the middle of the first pixel.  Other returned values are the rounded down X and
// slice numbers in J and lslice, and the four interpolation factors for the real pixel
// position
//
void Tilt::projectionPosition(int iv, float rlX, float rlZ, float rlSlice, float &xproj,
                              float &yproj, int &j, int &lslice, float &f11, float &f12,
                              float &f21, float &f22)
{
  float zz, yy, zpart;
  float fj, fls, xf11, xz11, yf11, yz11;
  float xf21, xz21, yf21, yz21, xf12, xz12, yf12, yz12, xf22, xz22, yf22;
  float yz22, xprojf, xprojz, yprojf, yprojz;

  zz = (rlZ - mYcenModProj) * mCompress[iv - 1];
  yy = rlSlice - mCenterSlice;
  if (mNxWarp == 0) {
    zpart = yy * mSinAlpha[iv - 1] * mSinBeta[iv - 1] + zz *
      (mCosAlpha[iv - 1] * mSinBeta[iv - 1] + mXZfac[iv - 1]) + mXcenIn + mAxisXoffset;
    yproj = yy * mCosAlpha[iv - 1] - zz * (mSinAlpha[iv - 1] - mYZfac[iv - 1]) + 
      mCenterSlice;
    xproj = zpart + (rlX - mXcenOut) * mCosBeta[iv - 1];
  } else {
    //
    // local alignments
    j = rlX;
    fj = rlX - j;
    lslice = rlSlice;
    fls = rlSlice - lslice;
    f11 = (1. -fj) * (1. -fls);
    f12 = (1. -fj) * fls;
    f21 = fj * (1. -fls);
    f22 = fj * fls;
    localProjFactors(j, lslice, iv, xf11, xz11, yf11, yz11);
    localProjFactors(j + 1, lslice, iv, xf21, xz21, yf21, yz21);
    localProjFactors(j, lslice + 1, iv, xf12, xz12, yf12, yz12);
    localProjFactors(j + 1, lslice + 1, iv, xf22, xz22, yf22, yz22);
    xprojf = f11 * xf11 + f12 * xf12 + f21 * xf21 + f22 * xf22;
    xprojz = f11 * xz11 + f12 * xz12 + f21 * xz21 + f22 * xz22;
    yprojf = f11 * yf11 + f12 * yf12 + f21 * yf21 + f22 * yf22;
    yprojz = f11 * yz11 + f12 * yz12 + f21 * yz21 + f22 * yz22;
    xproj = xprojf + zz * xprojz;
    yproj = yprojf + zz * yprojz;
    //
  }
}

// 
// Replacement for Fortran memoryError
//
void Tilt::memoryError(void *ptr, const char *mess)
{
  if (!ptr)
    exitError("Failure to allocate %s", mess);
}

//
// Wrappers to Fortran routines
//
void Tilt::bpSumNoX(float *array, int &ind1, float *input, int ipoint, int n,
                    double xproj1, float cbeta)
{
  bpsumnox(array, &ind1, input, &ipoint, &n, &xproj1, &cbeta);
}

void Tilt::bpSumXtilt(float *array, int &ind1, float *input, int ipbase, int ipdel, int n,
                      double xproj1, float cbeta, float yfrac, float omyfrac)
{
  bpsumxtilt(array, &ind1, input, &ipbase, &ipdel, &n, &xproj1, &cbeta, &yfrac, &omyfrac);
}

void Tilt::bpSumLocal(float *array, int &index, float *input, float zz, float *xprojf,
                      float *xprojz, float *yprojf, float *yprojz, int ipoint, int ipdel,
                      int lslice, int jstrt, int jend)
{
  bpsumlocal(array, &index, input, &zz, xprojf, xprojz, yprojf, yprojz, &ipoint, &ipdel,
             &lslice, &jstrt, &jend, &mProjSuperFac);
}

void Tilt::projSumLocal(float &xx, float &yy, float &zz, double &sum, float xproj, 
                        float yproj, float sinBeta, int nxLoad, int inLoadStart,
                        int inLoadEnd, float zJump, float ycenAdj)
{
  projsumlocal(&xx, &yy, &zz, &sum, &xproj, &yproj, mInputArray, mXprojFs, mXprojZs,
               mYprojFs, mYprojZs, &mNumWarpDelz, &mDxWarpDelz, mWarpDelz, &mIthickReproj,
               &sinBeta, &nxLoad, &inLoadStart, &inLoadEnd,
               &mInPlaneSize, &zJump, &ycenAdj);
}

void Tilt::loadedProjectingPoint(float xproj, float yproj, float zz, int nxLoad, 
                                 int inLoadStart, int inLoadEnd, float &xx, float &yy)
{
  loadedprojectingpoint(&xproj, &yproj, &zz, &nxLoad, &inLoadStart, &inLoadEnd,
                        mXprojFs, mXprojZs, mYprojFs, mYprojZs, &xx, &yy);
}
