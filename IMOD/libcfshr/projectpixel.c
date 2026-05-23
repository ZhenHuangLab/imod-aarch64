/*  projectpixel.c - routines for getting intersections between projection rays and pixels
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 2012-2020 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 * $Id$
 */

#include "imodconfig.h"
#include "b3dutil.h"

#ifdef F77FUNCAP
#define maxcornerdistfromcen MAXCORNERDISTFROMCEN
#define makerayarealookuptable MAKERAYAREALOOKUPTABLE
#else
#define maxcornerdistfromcen maxcornerdistfromcen_
#define makerayarealookuptable makerayarealookuptable_
#endif

/*!
 * For a direction [angle] through a unit pixel, returns the maximum distance to the 
 * corner in [dist].
 */
void maxCornerDistFromCen(float angle, float *dist)
{
  float cosAng = cos(angle * RADIANS_PER_DEGREE);
  float sinAng = sin(angle * RADIANS_PER_DEGREE);
  float c1 = 0.5 * cosAng + 0.5 *sinAng;
  float c2 = 0.5 * cosAng - 0.5 *sinAng;
  float c3 = -0.5 * cosAng + 0.5 *sinAng;
  float c4 = -0.5 * cosAng - 0.5 *sinAng;
  *dist = B3DMAX(c1, c2);
  ACCUM_MAX(*dist, c3);
  ACCUM_MAX(*dist, c4);
}

void maxcornerdistfromcen(float *angle, float *dist)
{
  maxCornerDistFromCen(*angle, dist);
}

/*!
 * For a direction through a unit pixel with cosine [cosAng] and sine [sinArg], returns in
 * [length] the length of a zero-width array through the pixel at distance [dist] from the
 * center of the pixel.
 */
void rayLengthAtDistFromCen(float cosAng, float sinAng, float dist, float *length)
{
  /* Intersections of ray with X = -0.5 and 0.5 and range of t values between them */
  float txp = (dist * cosAng + 0.5) / sinAng;
  float txm = (dist * cosAng - 0.5) / sinAng;
  float txmin = B3DMIN(txp, txm);
  float txmax = B3DMAX(txp, txm);

  /* Intersections of ray with Y = -0.5 and 0.5 and range of t values between them */
  float typ = (-dist * sinAng + 0.5) / cosAng;
  float tym = (-dist * sinAng - 0.5) / cosAng;
  float tymin = B3DMIN(typ, tym);
  float tymax = B3DMAX(typ, tym);

  /* The intersection of those two ranges.  
     Length is delta t because a  unit of t moves by cosAng, sinAng with length 1 */
  float tmin = B3DMAX(txmin, tymin);
  float tmax = B3DMIN(txmax, tymax);
  *length = B3DMAX(0., tmax - tmin);
}

/*!
 * For a direction [angle] through a unit pixel and a ray of width [rayWidth] whose center
 * is [cenToCenDist] from the pixel center, returns in [area] the area of the intersection
 * between the pixel and the ray by integrating with increment [delDist].
 */
void rayPixelIntersectingArea(float angle, float cenToCenDist, int rayWidth, 
                              float delDist, float *area)
{
  int ind, numRays;
  float maxCornDist, leftDist, rightDist, dist, frac, length;
  float cosAng = cos(angle * RADIANS_PER_DEGREE);
  float sinAng = sin(angle * RADIANS_PER_DEGREE);

  maxCornerDistFromCen(angle, &maxCornDist);
  leftDist = cenToCenDist - rayWidth / 2.;
  rightDist = cenToCenDist + rayWidth / 2.;
  *area = 0.;
  if (rightDist <= -maxCornDist || leftDist >= maxCornDist)
    return;
  if (leftDist <= -maxCornDist && rightDist >= maxCornDist) {
    *area = 1.;
    return;
  }
  ACCUM_MAX(leftDist, -maxCornDist);
  ACCUM_MIN(rightDist, maxCornDist);

  /* Integrate over rays at given increment */
  numRays = (rightDist - leftDist) / delDist;
  for (ind = 0; ind < numRays; ind++) {
    dist = leftDist + (ind + 0.5) * delDist;
    length = 1.;
    if (cosAng != 0. && sinAng != 0.)
      rayLengthAtDistFromCen(cosAng, sinAng, dist, &length);
    *area += length;
  }
  *area *= delDist;
  frac = (rightDist - leftDist) - delDist * numRays;
  dist = leftDist + numRays * delDist + 0.5 * frac;
  length = 1.;
  if (cosAng != 0. && sinAng != 0.)
    rayLengthAtDistFromCen(cosAng, sinAng, dist, &length);
  *area += length * frac;
}

/*!
 * Fills a table that can be used to project a pixel into one to three intersecting rays 
 * at direction [angle] with width [rayWidth].  The number of entries to be computed is
 * specified by [numDists] and [delForArea] sets the increment for integrating areas.
 * [delRayInd] and [numRaysHit] are arrays of at least [numDists] elements. [delRayInd]
 * is returned with the delta to the first ray index from the index of the nearest ray 
 * to center of the pixel (0 or -1).  [numRaysHit] is returned with the number of
 * intersecting rays (1 - 3).  [areas] must have at least 3 * [numDists] elements and is
 * returned with the intersecting areas of the 1 to 3 rays.  Th distances from the center
 * of the nearest ray to the pixel center start at -[rayWidth] / 2. and are spaced at
 * [rayWidth] / [numDists].
 */
void makeRayAreaLookupTable(float angle, int rayWidth, int numDists, float delForArea,
                            int *delRayInd, int *numRaysHit, float *areas)
{
  int ind;
  float maxDist, dist, sum;
  float delTable = (float)rayWidth / numDists;
  maxCornerDistFromCen(angle, &maxDist);
  for (ind = 0; ind < numDists; ind++) {
    dist = (ind + 0.5) * delTable - 0.5 * rayWidth;
    sum = 0.;
    delRayInd[ind] = 0;
    numRaysHit[ind] = 0;
    if (dist - rayWidth / 2. > -maxDist) {
      delRayInd[ind] = -1;
      numRaysHit[ind] = 1;
      rayPixelIntersectingArea(angle, dist - rayWidth, rayWidth, delForArea, 
                               &areas[3 * ind]);
      sum += areas[3 * ind];
    } 

    rayPixelIntersectingArea(angle, dist, rayWidth, delForArea, 
      &areas[3 * ind + numRaysHit[ind]]);
    sum += areas[3 * ind + numRaysHit[ind]];
    numRaysHit[ind]++;
    if (dist + rayWidth / 2. < maxDist) {
      rayPixelIntersectingArea(angle, dist + rayWidth, rayWidth, delForArea, 
                               &areas[3 * ind + numRaysHit[ind]]);
      sum += areas[3 * ind + numRaysHit[ind]];
      numRaysHit[ind]++;
    }
  }
}

void makerayarealookuptable(float *angle, int *rayWidth, int *numDists, float *delForArea,
                            int *delRayInd, int *numRaysHit, float *areas)
{
  makeRayAreaLookupTable(*angle, *rayWidth, *numDists, *delForArea,
                         delRayInd, numRaysHit, areas);
}


#ifdef TEST_RAY_AREA
int main(int argc, char *argv[])
{
  float angle, tableDel, areaDel, maxDist, areas[3000], sum, dist;
  int numDist, width, ind, ray;
  int delRayInd[1000], numRaysHit[1000];
  if (argc < 5) {
    printf("Usage: angle rayWidth tableDelta areaDelta\n");
    exit(1);
  }
  angle = atof(argv[1]);
  width = atoi(argv[2]);
  tableDel = atof(argv[3]);
  areaDel = atof(argv[4]);
  maxCornerDistFromCen(angle, &maxDist);
  printf("Max corner distance %f\n", maxDist);
  numDist = width / tableDel;
  makeRayAreaLookupTable(angle, width, numDist, areaDel, delRayInd, numRaysHit, areas);
  tableDel = (float)width / numDist;
  for (ind = 0; ind < numDist; ind++) {
    sum = 0.;
    dist = (ind + 0.5) * tableDel - 0.5 * width;
    printf("%.3f: %2d", dist, delRayInd[ind]);
    for (ray = 0; ray < numRaysHit[ind]; ray++) {
      sum += areas[3 * ind + ray];
      printf("  %.4f", areas[3 * ind + ray]);
    }
    printf("    %.6f\n", sum);
  }
}
#endif
