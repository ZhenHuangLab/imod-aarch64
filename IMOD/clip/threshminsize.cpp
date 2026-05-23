/*
 *  threshminsize.cpp -- Thresholding with a minimum size of adjacent points
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2015 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#include <set>
#include <list>
#include <exception>
#include <stdio.h>
#include <string.h>
#include "cppdefs.h"
#include "clip.h"

// A structure to hold a connected set of points in one plane
struct PlaneConnectedPoints {
  set<unsigned int> points;
  int xmin, ymin, xmax, ymax;
};

// A structure to hold information about a collection of planar sets connected in Z
struct ZConnectedSets {
  vector<int> planeZ;
  vector<int> index;
  int xmin, ymin, xmax, ymax;
  int zmin, zmax;
  int numPoints;
};

// To go between an x,y coordinate and an unsigned int
#define COMBINE_X_Y(a, b) ((((unsigned int)a) << xShift) | (b))
#define SPLIT_X_Y(a, b, c) {b = (a) >> xShift ; c = (a) & yMask;}

// To test whether an adjacent point is to be added and then add it to the planar set
#define CHECK_AND_ADD(x, y)                                             \
  checkInd = x + nx * y;                                                \
  if (!grouped[checkInd] && direction * (fdata[checkInd] - thresh) >= 0.) { \
    xyCombo = COMBINE_X_Y(x, y);                                        \
    checkList.push_back(xyCombo);                                       \
    planeSet->points.insert(xyCombo);                                   \
    planeSet->xmin = B3DMIN(planeSet->xmin, x);                         \
    planeSet->xmax = B3DMAX(planeSet->xmax, x);                         \
    planeSet->ymin = B3DMIN(planeSet->ymin, y);                         \
    planeSet->ymax = B3DMAX(planeSet->ymax, y);                         \
    grouped[checkInd] = 1;                                              \
  }

/*
 * Function to threshold a volume with minimum size of segmented regions in one direction
 */
int thresholdWithMinSize(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt,
                         float threshLo, float threshHi, int zWrite)
{
  int ksecIn, ksecOut, zvalIn, nx, ny, xShift, yMask, lookX, lookY, minSize, ybase;
  int ix, iy, checkX, checkY, ind, psInd, zcInd, checkInd, nextCheck, lookInd, zActive;
  int direction = 1;
  int ksecOffset = opt->dim == 3 ? 1 : 0;
  vector<vector<PlaneConnectedPoints> > connPoints;
  vector<unsigned int> checkList;
  list<ZConnectedSets> zConnSets;
  list<ZConnectedSets>::iterator zIter, firstIter;
  set<unsigned int>::iterator psIter;
  Islice *inSlice, *outSlice, *nextSlice = NULL, *lastSlice = NULL;
  bool foundZset, foundPlane;
  unsigned int xyCombo;
  unsigned char *grouped;
  float *fdata;
  float thresh = opt->thresh;
  Ival fillVal, setVal;
  set<unsigned int> *pointSet;
  PlaneConnectedPoints *planeSet;
  ZConnectedSets *zSet;

  mrc_head_label(hout, "clip: thresholded with minimum size constraint");
  if (opt->minSize == 0) {
    printf("ERROR: CLIP - The minimum size entry must be non-zero\n");
    return -1;
  }

  // Set up direction and the values to use for background and connected points
  minSize = opt->minSize;
  if (minSize < 0) {
    minSize = -minSize;
    direction = -1;
    fillVal[0] = fillVal[1] = fillVal[2] = threshHi;
    setVal[0] = setVal[1] = setVal[2] = threshLo;
  } else {

    // The threshold comparison with >= 0 matches 3dmod display only for negative 
    // direction, so increase the threshold by minimal amount to make it instead of > 0
    // comparison work for positive direction
    thresh *= 1.0000006;
    fillVal[0] = fillVal[1] = fillVal[2] = threshLo;
    setVal[0] = setVal[1] = setVal[2] = threshHi;
  }

  // Compute the shift and mask values by shifting ny to right until it goes to 0
  nx = opt->ix - 1;
  ny = opt->iy - 1;
  xShift = 0;
  yMask = 0;
  while (ny > 0) {
    xShift++;
    ny = ny >> 1;
    nx = nx << 1;
    yMask = (yMask << 1) | 1;
    if (ny > 0 && (nx & 0x80000000) != 0) {
      printf("ERROR: CLIP - Images are too large in X and Y for the thresholding"
             " procedure\n");
      return -1;
    }
  }
  //PRINT2(xShift, yMask);
  nx = opt->ix;
  ny = opt->iy;
  ksecOut = 0;
  
  // Get memory for keeping track of grouped points and for output slice
  grouped = B3DMALLOC(unsigned char, nx * ny);
  outSlice = sliceCreate(nx, ny, opt->mode);
  if (!grouped || !outSlice) {
    printf("ERROR: CLIP - Allocating  memory\n");
    return -1;
  }

  try {
    connPoints.resize(opt->nofsecs);

    // Loop on input slices
    for (ksecIn = -ksecOffset; ksecIn < opt->nofsecs; ksecIn++) {
      if (ksecIn + ksecOffset < opt->nofsecs) {

        // Read and convert the next slice
        zvalIn = opt->secs[ksecIn + ksecOffset];
        nextSlice = sliceReadSubm(hin, zvalIn, 'z', opt->ix, opt->iy, (int)opt->cx,
                                  (int)opt->cy);
        if (!nextSlice || sliceFloat(nextSlice) < 0) {
          printf("ERROR: CLIP - %s slice %d %s\n", nextSlice ? "Converting" : "Reading",
                 zvalIn, inSlice ? "to floating point" : "from file");
          return -1;
        }

        if (!ksecOffset || ksecIn == -1)
          inSlice = nextSlice;
        if (ksecIn == -1)
          continue;
      }
      fdata = inSlice->data.f;
      memset(grouped, 0, nx * ny);

      // Loop on points in the slice looking for ones not yet in group
      for (lookY = 0; lookY < ny; lookY++) {
        ybase = nx * lookY;
        for (lookX = 0; lookX < nx; lookX++) {
          lookInd = ybase + lookX;
          if (!grouped[lookInd] && direction * (fdata[lookInd] - thresh) >= 0.) {
            
            // Start the list of ones to check and the planar set
            checkList.clear();
            xyCombo = COMBINE_X_Y(lookX, lookY);
            checkList.push_back(xyCombo);
            connPoints[ksecIn].push_back(PlaneConnectedPoints());
            planeSet = &connPoints[ksecIn].back();
            planeSet->points.insert(xyCombo);
            planeSet->xmin = lookX;
            planeSet->xmax = lookX;
            planeSet->ymin = lookY;
            planeSet->ymax = lookY;
            nextCheck = 0;
            grouped[lookInd] = 1;
            
            // loop on the check set until all are checked
            while (nextCheck < checkList.size()) {
              SPLIT_X_Y(checkList[nextCheck], checkX, checkY);
              CHECK_AND_ADD(B3DMAX(0, checkX - 1), checkY);
              CHECK_AND_ADD(B3DMIN(nx - 1, checkX + 1), checkY);
              CHECK_AND_ADD(checkX, B3DMAX(0, checkY - 1));
              CHECK_AND_ADD(checkX, B3DMIN(ny - 1, checkY + 1));
              nextCheck++;
            }

            // If there is only a single point and it is not present on previous and
            // next slice, remove it
            if (nextCheck == 1 && lastSlice && nextSlice && 
                direction * (lastSlice->data.f[lookInd] - thresh) < 0. &&
                direction * (nextSlice->data.f[lookInd] - thresh) < 0.)
              connPoints[ksecIn].erase(connPoints[ksecIn].end() - 1);
          }
        }
      }
      //PRINT2(ksecIn, connPoints[ksecIn].size());

      // The slice is done.  Handle rolling the slice pointers for 3D and freeing a slice
      if (ksecOffset) {
        if (lastSlice)
          sliceFree(lastSlice);
        lastSlice = inSlice;
        inSlice = nextSlice;
        nextSlice = NULL;
      } else
        sliceFree(inSlice);

      // Loop through the Z-connected sets for each planar set and find ones to connect to
      for (psInd = 0; psInd < connPoints[ksecIn].size(); psInd++) {
        planeSet = &connPoints[ksecIn][psInd];
        foundZset = false;

        // Handle 2-D simply by not making any connections
        if (opt->dim == 3) {
          for (zIter = zConnSets.begin(); zIter != zConnSets.end(); zIter++) {

            // If the Z set and plane set have intersecting ranges,loop on plane sets
            // inside the Z set and find ones on previous plane
            if (!(zIter->xmax < planeSet->xmin || zIter->xmin > planeSet->xmax || 
                  zIter->ymax < planeSet->ymin || zIter->ymin > planeSet->ymax)) {
              foundPlane = false;
              for (zcInd = 0; !foundPlane && zcInd < zIter->index.size(); zcInd++) {
                if (zIter->planeZ[zcInd] == ksecIn - 1) {

                  // Then loop through points in the current plane set and see if any are
                  // in the planar set on previous plane
                  pointSet = &connPoints[ksecIn - 1][zIter->index[zcInd]].points;
                  for (psIter = planeSet->points.begin(); 
                       psIter != planeSet->points.end(); psIter++) {
                    if (pointSet->count(*psIter) > 0) {
                      if (!foundZset) {

                        // First one that is found, add the current planar set to Z set
                        firstIter = zIter;
                        foundZset = true;
                        zIter->planeZ.push_back(ksecIn);
                        zIter->index.push_back(psInd);
                        zIter->xmin = B3DMIN(zIter->xmin, planeSet->xmin);
                        zIter->xmax = B3DMAX(zIter->xmax, planeSet->xmax);
                        zIter->ymin = B3DMIN(zIter->ymin, planeSet->ymin);
                        zIter->ymax = B3DMAX(zIter->ymax, planeSet->ymax);
                        zIter->zmax = ksecIn;
                        zIter->numPoints += planeSet->points.size();
                      } else {

                        // Otherwise need to merge the Z set that was just found to the
                        // first one found: add the planar sets, adjust mins/maxes
                        for (ind = 0; ind < zIter->index.size(); ind++) {
                          firstIter->index.push_back(zIter->index[ind]);
                          firstIter->planeZ.push_back(zIter->planeZ[ind]);
                        }
                        firstIter->xmin = B3DMIN(zIter->xmin, firstIter->xmin);
                        firstIter->xmax = B3DMAX(zIter->xmax, firstIter->xmax);
                        firstIter->ymin = B3DMIN(zIter->ymin, firstIter->ymin);
                        firstIter->ymax = B3DMAX(zIter->ymax, firstIter->ymax);
                        firstIter->zmin = B3DMIN(zIter->zmin, firstIter->zmin);
                        firstIter->numPoints += zIter->numPoints;
                 
                        // Erasing this item leaves the firstIter valid
                        // and returns iterator to next element.  Need to back up so that
                        // it will increment to next element in the main loop
                        zIter = zConnSets.erase(zIter);
                        zIter--;
                      }

                      // Break all the way out of this Z set
                      foundPlane = true;
                      break;
                    }
                  }
                }
              }
            }
          }
        }

        // If no existing set found, start a Z-connected set with this planar set
        if (!foundZset) {
          zConnSets.push_back(ZConnectedSets());
          zSet = &zConnSets.back();
          zSet->index.push_back(psInd);
          zSet->planeZ.push_back(ksecIn);
          zSet->xmin = planeSet->xmin;
          zSet->xmax = planeSet->xmax;
          zSet->ymin = planeSet->ymin;
          zSet->ymax = planeSet->ymax;
          zSet->zmin = ksecIn;
          zSet->zmax = ksecIn;
          zSet->numPoints = planeSet->points.size();
        }
      }
      //PRINT1(zConnSets.size());

      // Set the "active" Z plane as the current one, or one past that if we have just 
      // done the last plane, then loop on potential output planes
      zActive = B3DCHOICE(ksecIn < opt->nofsecs - 1, ksecIn, opt->nofsecs);
      for (; ksecOut < zActive; ksecOut++) {

        // A plane can be output if all sets that start on or before it end before the
        // active plane
        foundZset = false;
        for (zIter = zConnSets.begin(); zIter != zConnSets.end(); zIter++) {
          if (zIter->zmin <= ksecOut && zIter->zmax == zActive) {
            foundZset = true;
            break;
          }
        }
        if (foundZset)
          break;

        // Output a plane.  Fill the plane first
        for (iy = 0; iy < ny; iy++)
          for (ix = 0; ix < nx; ix++)
            slicePutVal(outSlice, ix, iy, fillVal);


        // Loop on Z sets and find large enough ones that intersect output Z
        ind = 0;
        for (zIter = zConnSets.begin(); zIter != zConnSets.end(); zIter++) {
          if (zIter->numPoints >= minSize && zIter->zmin <= ksecOut && 
              zIter->zmax >= ksecOut) {
            for (zcInd = 0; zcInd < zIter->index.size(); zcInd++) {
              if (zIter->planeZ[zcInd] == ksecOut) {
                ind++;
                planeSet = &connPoints[ksecOut][zIter->index[zcInd]];
                for (psIter = planeSet->points.begin(); 
                     psIter != planeSet->points.end(); psIter++) {
                  SPLIT_X_Y(*psIter, ix, iy);
                  slicePutVal(outSlice, ix, iy, setVal);
                }
              }
            }
          }
        }

        //PRINT3(ksecOut, zWrite, ind);
        if (clipWriteSlice(outSlice, hout, opt, ksecOut, &zWrite, 0))
          return -1;

        // Clean up all planar sets on the output Z plane
        // This is essential for recovering this memory
        CLEAR_RESIZE(connPoints[ksecOut], PlaneConnectedPoints, 0);
      }

      // Now clean up any Z sets that end before next output slice or are too small
      // and end before active slice
      // This doesn't seem to recovery much memory but supposedly the library reuses it
      for (zIter = zConnSets.begin(); zIter != zConnSets.end(); ) {
        if ((zIter->numPoints < minSize && zIter->zmax < zActive) || 
            zIter->zmax < ksecOut) {
          zIter = zConnSets.erase(zIter);
        } else
          zIter++;
      }
      //PRINT1(zConnSets.size());
    }
  }
  catch (bad_alloc &ba) {
    printf("ERROR: CLIP - Memory allocation failed in a standard template operation\n");
    return -1;
  }
  catch (exception &e) {
    printf("ERROR: CLIP - An exception (%s) occurred in a standard template operation\n",
           e.what());
    return -1;
  }

  // Clean up so memory leaks can be detected
  free(grouped);
  sliceFree(outSlice);
  if (lastSlice)
    sliceFree(lastSlice);
  return set_mrc_coords(opt);
}
