/*
 *  sliceproc.c -- image slice support for 3dmod and clip
 *
 *  Original author: James Kremer
 *  Revised and added to by: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2007 by Boulder Laboratory for 3-Dimensional Electron
 *  Microscopy of Cells ("BL3DEMC") and the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#include <math.h>
#include <stdlib.h>
#include "b3dutil.h"
#include "mrcc.h"
#include "sliceproc.h"

/* DNM 11/7/04: All routines expect the min and max of the input slice to be
   set to the desired range for scaling the output */

#define RANGE(s, i, j)  (((i)>0)&&((j)>0)&&((i)<(s)->xsize)&&((j)<(s)->ysize))
#define GETVAL(s, i, j) ((s)->data.b[(i) + ((j) * (s)->xsize)])
#define GETSVAL(t, i, j) ((t)->data.s[(i) + ((j) * (t)->xsize)])
#define GETFVAL(s, i, j) ((s)->data.f[(i) + ((j) * (s)->xsize)])


static int SmoothKernel[3][3] = 
  { 
    {1, 2, 1},
    {2, 4, 2},
    {1, 2, 1}
  };

static int SharpenKernel[3][3] =
  {
    {-1,  -1, -1},
    {-1,  9, -1},
    {-1,  -1, -1}
  };

static int LaplacianKernel[3][3] =
  {
    {1,  1, 1},
    {1,  -4, 1},
    {1,  1, 1}
  };


/*!
 * Adds [inVal] to all values in the slice [sin], which must contain bytes.
 * Returns 0.
 */
int sliceByteAdd(Islice *sin, int inVal)
{
  int imax = sin->xsize * sin->ysize;
  int aval;
  int i;
     
  unsigned char *image = sin->data.b;

  for(i = 0; i < imax; i++){
    aval = image[i] + inVal;
    if (aval > 255) aval = 255;
    if (aval < 0) aval = 0;
    image[i] = aval;
  }
  return 0;
}

/*!
 * Computes a Prewitt or Sobel edge filter on the slice [sin], depending on 
 * whether the center pixel weighting, [center], is 1 or 2.  The slice must 
 * contain bytes.  Returns 0.
 */
int sliceByteEdgeTwo(Islice *sin, int center)
{
  Islice *sout = sliceCreate(sin->xsize, sin->ysize,  SLICE_MODE_FLOAT);
  float val;
  int imax = sin->xsize - 1;
  int jmax = sin->ysize - 1;
  int i, j;
  float Sr, Sc;

  for(i = 1; i < imax; i++) 
    for(j = 1; j < jmax; j++){
      Sr = (GETVAL(sin, i-1, j-1) + 
            (center * GETVAL(sin, i, j-1)) + GETVAL(sin, i+1, j-1)) -
        (GETVAL(sin, i-1, j+1) +
         (center * GETVAL(sin, i, j+1)) + GETVAL(sin, i+1, j+1));
               
      Sc = (GETVAL(sin, i+1, j+1) +  center * GETVAL(sin, i+1, j) +
            GETVAL(sin, i+1, j-1)) -
        (GETVAL(sin, i-1, j+1) +
         (center * GETVAL(sin, i-1, j)) +
         GETVAL(sin, i-1, j-1));
      Sr *= Sr;
      Sc *= Sc;
      val = sqrt(Sr + Sc);

      GETFVAL(sout, i, j) = val;
    }

  for(j = 1; j < jmax; j++){
    GETFVAL(sout, 0, j) =  GETFVAL(sout, 1, j);
    GETFVAL(sout, imax, j) =  GETFVAL(sout, imax-1, j);
  }

  for(i = 0; i <= imax; i++){
    GETFVAL(sout, i, 0) =  GETFVAL(sout, i, 1);
    GETFVAL(sout, i, jmax) =  GETFVAL(sout, i, jmax-1);
  }
  sliceScaleAndFree(sout, sin);
  return(0);
}

/*!
 * Computes a Sobel edge filter on the slice [sin], which must
 * contain bytes.  Returns 0.
 */
int sliceByteEdgeSobel(Islice *sin)
{
  return(sliceByteEdgeTwo(sin, 2));
}

/*!
 * Computes a Prewitt edge filter on the slice [sin], which must
 * contain bytes.  Returns 0.
 */
int sliceByteEdgePrewitt(Islice *sin)
{
  return(sliceByteEdgeTwo(sin, 1));
}

/*!
 * Computes a Laplacian edge filter on the slice [sin], which must
 * contain bytes.  The Laplacian kernel is a -4 surrounded by 1's.  Returns 0.
 */
int sliceByteEdgeLaplacian(Islice *sin)
{
  return(sliceByteConvolve(sin, LaplacianKernel));
}

/*!
 * Computes a Laplacian edge filter on the slice [sin], which must contain 
 * bytes.  The sharpening kernel is a 9 surrounded by -1's.  Returns 0.
 */
int sliceByteSharpen(Islice *sin)
{
  return(sliceByteConvolve(sin, SharpenKernel));
}

/* DNM 11/07/04: Removed smoothing routine that did the same as convolving with
   this kernel */

/*!
 * Smooths the slice [sin], which must contain bytes, by convolving with a 
 * 3x3 smoothing kernel.  Returns 0.
 */
int sliceByteSmooth(Islice *sin)
{
  return(sliceByteConvolve(sin, SmoothKernel));
}

/*!
 * Convolves the slice [sin] with the 3x3 kernel in [mask].  The slice must
 * contain bytes.  Returns 0.
 */
int sliceByteConvolve(Islice *sin, int mask[3][3])
{
  Islice *sout = sliceCreate(sin->xsize, sin->ysize,  SLICE_MODE_SHORT);
  int imax = sin->xsize - 1;
  int jmax = sin->ysize - 1;
  int i, j;
  int val;

  /* 11/7/04: No longer need to determine mean of mask with new scaling */

  for(i = 1; i < imax; i++) 
    for(j = 1; j < jmax; j++){
      val = (GETVAL(sin, i+1, j+1) * mask[0][0] +
             GETVAL(sin,   i, j+1) * mask[0][1] +
             GETVAL(sin, i-1, j+1) * mask[0][2] +
             GETVAL(sin, i+1, j) * mask[1][0] +
             GETVAL(sin,   i, j) * mask[1][1] +
             GETVAL(sin, i-1, j) * mask[1][2] +
             GETVAL(sin, i+1, j-1) * mask[2][0] +
             GETVAL(sin,   i, j-1) * mask[2][1] +
             GETVAL(sin, i-1, j-1) * mask[2][2]);
      GETSVAL(sout, i, j) = val;
    }

  for(j = 1; j < jmax; j++){
    GETSVAL(sout, 0, j) =  GETSVAL(sout, 1, j);
    GETSVAL(sout, imax, j) =  GETSVAL(sout, imax-1, j);
  }
  for(i = 0; i <= imax; i++){
    GETSVAL(sout, i, 0) =  GETSVAL(sout, i, 1);
    GETSVAL(sout, i, jmax) =  GETSVAL(sout, i, jmax-1);
  }

  sliceScaleAndFree(sout, sin);
  return(0);
}




static int nay8(Islice *sin, int i, int j, int val)
{
  int n, m, k = 0;
  int x, y;

  if ( GETVAL(sin, i, j) != val)
    return(0);
     
  for (n = -1; n <= 1; n++){
    y = n + j;
    for(m = -1; m <= 1 ; m++){
      x = m + i;
      if ((x > 0) && (y > 0) && (x < sin->xsize) && (y < sin->ysize))
        if (GETVAL(sin, x, y) == val) k++; 
    }
  }
  return(k-1);
}

int sliceByteThreshold(Islice *sin, int val)
{
  int imax = sin->xsize * sin->ysize;
  int i;
  unsigned char *image = sin->data.b;
    
  int pmin = (int)sin->min;
  int pmax = (int)sin->max;
  int thresh = pmin + val;
    

  for(i = 0; i < imax; i++){
    if (image[i] < thresh)
      image[i] = pmax;
    else
      image[i] = pmin;
  }
  return 0;
}

int sliceByteGrow(Islice *sin, int val)
{
  int imax = sin->xsize;
  int jmax = sin->ysize;
  int i, j, m, n, x, y;

  for(j = 0; j < jmax; j++)
    for(i = 0; i < imax; i++){
      if ( GETVAL(sin, i, j) != val) continue;
      for(m = -1; m <= 1; m++){
        y = j + m;
        if ((y < 0) || (y >= sin->ysize)) continue;
        for(n = -1; n <= 1; n++){
          x = n + i;
          if ((x == i) && (y == j)) continue;
          if ((x < 0) || (x >= sin->xsize)) continue;
          if (GETVAL(sin, x, y) == sin->min)
            GETVAL(sin, x, y) = (unsigned char)(sin->max - 1);
        }
      }
    }


  for(j = 0; j < jmax; j++)
    for(i = 0; i < imax; i++){
      if ( GETVAL(sin, i, j) == (sin->max - 1)){
        GETVAL(sin, i, j) = val;
      }
    } 
  return(0);
}

int sliceByteShrink(Islice *sin, int val)
{
  Islice *sout = sliceCreate(sin->xsize, sin->ysize,  SLICE_MODE_BYTE);
  int imax = sin->xsize;
  int jmax = sin->ysize;
  int i, j;

  unsigned char pmin = (unsigned char)sin->min;
  unsigned char pmax = (unsigned char)sin->max;

  for(j = 0; j < jmax; j++)
    for(i = 0; i < imax; i++){
      GETVAL(sout, i, j) = 0;
    }


  for(j = 0; j < jmax; j++)
    for(i = 0; i < imax; i++){
      if (nay8(sin, i, j, pmax) < 7)
        GETVAL(sout, i, j)  = 1;
    } 

     
  for(j = 0; j < jmax; j++)
    for(i = 0; i < imax; i++){
      if (GETVAL(sout, i, j))
        GETVAL(sin, i, j)  = pmin;
    }
  return 0;
}

int sliceByteGraham(Islice *sin)
{
  Islice *sout = sliceCreate(sin->xsize, sin->ysize,  SLICE_MODE_FLOAT);
  float val;
  int imax = sin->xsize - 1;
  int jmax = sin->ysize - 1;
  int i, j;
  float Ixx, Iyy;
  float ld = 1.0f/6.0f;
  float hd = 1.0f/3.0f;
  float delta = 5;

  for(i = 1; i < imax; i++) 
    for(j = 1; j < jmax; j++){
      Ixx  = GETVAL(sin, i+1, j+1) * ld;
      Ixx -= GETVAL(sin,   i, j+1) * hd;
      Ixx += GETVAL(sin, i-1, j+1) * ld;
      Ixx += GETVAL(sin, i+1,   j) * ld;
      Ixx -= GETVAL(sin,   i,   j) * hd;
      Ixx += GETVAL(sin, i-1,   j) * ld;
      Ixx += GETVAL(sin, i+1, j-1) * ld;
      Ixx -= GETVAL(sin,   i, j-1) * hd;
      Ixx += GETVAL(sin, i-1, j-1) * ld;

      Iyy  = GETVAL(sin, i+1, j+1) * ld;
      Iyy += GETVAL(sin,   i, j+1) * ld;
      Iyy += GETVAL(sin, i-1, j+1) * ld;
      Iyy -= GETVAL(sin, i+1,   j) * hd;
      Iyy -= GETVAL(sin,   i,   j) * hd;
      Iyy -= GETVAL(sin, i-1,   j) * hd;
      Iyy += GETVAL(sin, i+1, j-1) * ld;
      Iyy += GETVAL(sin,   i, j-1) * ld;
      Iyy += GETVAL(sin, i-1, j-1) * ld;


      if (Ixx < delta){
        if (Iyy < delta){
          val = (GETVAL(sin, i+1, j+1) +
                 GETVAL(sin,   i, j+1) + 
                 GETVAL(sin, i-1, j+1) +
                 GETVAL(sin, i+1, j) +
                 GETVAL(sin,   i, j) +
                 GETVAL(sin, i-1, j) +
                 GETVAL(sin, i+1, j-1) +
                 GETVAL(sin,   i, j-1) +
                 GETVAL(sin, i-1, j-1)) / 9.0f;
        }else{
          val = (GETVAL(sin, i+1, j) +
                 GETVAL(sin,   i, j) +
                 GETVAL(sin, i-1, j)) / 3.0f;
        }
      }else{
        if (Iyy < delta){
          val = (GETVAL(sin, i, j+1) +
                 GETVAL(sin, i, j) +
                 GETVAL(sin, 1, j-1)) / 3.0f;
        }else{
          val = GETVAL(sin,   i, j);
        }
      }
      GETFVAL(sout, i, j) = val;
    }

  for(j = 1; j < jmax; j++){
    GETFVAL(sout, 0, j) =  GETFVAL(sout, 1, j);
    GETFVAL(sout, imax, j) =  GETFVAL(sout, imax-1, j);
  }

  for(i = 0; i <= imax; i++){
    GETFVAL(sout, i, 0) =  GETFVAL(sout, i, 1);
    GETFVAL(sout, i, jmax) =  GETFVAL(sout, i, jmax-1);
  }

  sliceScaleAndFree(sout, sin);
  return(0);

}


#define MEDIAN_MAX_THREADS 6
// Common code for median filtering integers
#define FILTER_INTS(in, u)                                              \
  intOut = iVals;                                                       \
  for (oz = 0; oz < stack->zsize; oz++)                                 \
    for (iy = yStart; iy < yEnd; iy++) {                                \
      in = &stack->vol[oz]->data.u[xStart + iy * slIn->xsize];          \
      for (ix = xStart; ix < xEnd; ix++)                                \
        *intOut++ = *in++;                                              \
    }                                                                   \
  val[0] = percentileInt(select, iVals, numVals);                       \
  if (!(numVals % 2))                                                   \
    val[0] = 0.5f * (val[0] + percentileInt(select + 1, iVals, numVals)); \
  break;                                                                \

// Macros and function for optimized median of 9 values
// From: http://ndevilla.free.fr/median/median/index.html
#define PIX_SORT(a,b) { if ((a) > (b)) PIX_SWAP((a), (b)); }
#define PIX_SWAP(a,b) {  ftemp = (a); (a) = (b); (b) = ftemp; }
static float opt_med9(float * p)
{
  float ftemp;
    PIX_SORT(p[1], p[2]) ; PIX_SORT(p[4], p[5]) ; PIX_SORT(p[7], p[8]) ;
    PIX_SORT(p[0], p[1]) ; PIX_SORT(p[3], p[4]) ; PIX_SORT(p[6], p[7]) ;
    PIX_SORT(p[1], p[2]) ; PIX_SORT(p[4], p[5]) ; PIX_SORT(p[7], p[8]) ;
    PIX_SORT(p[0], p[3]) ; PIX_SORT(p[5], p[8]) ; PIX_SORT(p[4], p[7]) ;
    PIX_SORT(p[3], p[6]) ; PIX_SORT(p[1], p[4]) ; PIX_SORT(p[2], p[5]) ;
    PIX_SORT(p[4], p[7]) ; PIX_SORT(p[4], p[2]) ; PIX_SORT(p[6], p[4]) ;
    PIX_SORT(p[4], p[2]) ; return(p[4]) ;
}


/*!
 * Applies a 2-D or 3-D median filter to the slices contained in the 
 * @@mrcslice.html#Istack structure@ [stack],
 * with the size in X and Y specified by [size] and the size in Z determined
 * by the number of slices in the stack.  Puts the output into [slOut], which
 * should already be set up for size and mode and have a data array, but need not have the
 * same mode as the input slices.  
 * Input and output modes can be byte, short, or float.  Computation of a 2-D 3x3 filter
 * on a float image into a float image is highly optimized as well as parallelized with
 * OpenMP.  Parallelization was not effective for other cases.  Returns -1 for memory
 * error or -2 for an illegal mode.
 */
int sliceMedianFilter(Islice *slOut, Istack *stack, int size)
{
  Islice *slIn = stack->vol[0];
  float *fVals = NULL;
  int *iVals = NULL;
  float *lineVals, *line1, *line2, *line3;
  float *fThreadLine[MEDIAN_MAX_THREADS];
  float *fThreadVals[MEDIAN_MAX_THREADS];
  int initialLoaded[MEDIAN_MAX_THREADS];
  int isFloat = 0;
  int blockSize = stack->zsize * size * size;
  int numVals, ox, oy, oz, xStart, yStart, ix, iy, xEnd, yEnd, delMinus, delPlus, select;
  int numThreads, thrd, offset;
  int *intOut;
  float *fltOut;
  Ival val;
  b3dFloat *fltIn;
  b3dUByte *byteIn;
  b3dInt16 *shortIn;
  b3dUInt16 *usIn;

  /* If it is a 3x3 2D filter, float to float, set up threads for copying each set of
     three lines into a single line that can be copied quickly for the median.  This helps
     some but not a lot (e.g., 6.6 -> 5.7 sec on 8K image.)  Switching to the fast median
     function helped more (-> 3.6 sec) and this was worth parallelizing.  The standard
     percentile routine does not run well in parallel so the general case below is 
     not parallelized */
  if (stack->zsize == 1 && size == 3 && slIn->mode == SLICE_MODE_FLOAT && 
      slOut->mode == SLICE_MODE_FLOAT) {
    numThreads = B3DMIN(MEDIAN_MAX_THREADS, 
                        B3DNINT(0.002 * sqrt((double)slIn->xsize * slIn->ysize)));
    numThreads = numOMPthreads(B3DMAX(1, numThreads));

    for (thrd = 0; thrd < numThreads; thrd++) {
      fThreadLine[thrd] = B3DMALLOC(float, size * slIn->xsize);
      fThreadVals[thrd] = B3DMALLOC(float, blockSize);
      initialLoaded[thrd] = 0;
      if (!fThreadLine[thrd] || !fThreadVals[thrd]) {
        for (oy = 0; oy <= thrd; oy++) {
          free(fThreadLine[oy]);
          free(fThreadVals[oy]);
        }
        return(-1);
      }
    }
    
#pragma omp parallel for default(none) num_threads(numThreads)          \
  shared(slIn, fThreadLine, fThreadVals, slOut, initialLoaded)                        \
  private(oy, ox, oz, select, fltOut, fVals, iVals, thrd, line1, line2, line3, lineVals, \
          offset)
    for (oy = 0; oy < slIn->ysize; oy++) {
      thrd = b3dOMPthreadNum();
      fltOut = lineVals = fThreadLine[thrd];
      fVals = fThreadVals[thrd];
      
      if (!initialLoaded[thrd]) {

        /* Set up pointers and copy the three lines interleaved the first time */
        line1 = &slIn->data.f[B3DMAX(0, oy - 1) * slIn->xsize];
        line2 = &slIn->data.f[oy * slIn->xsize];
        line3 = &slIn->data.f[B3DMIN(slIn->ysize - 1, oy + 1) * slIn->xsize];
        for (ox = 0; ox < slIn->xsize; ox++) {
          *fltOut++ = *line1++;
          *fltOut++ = *line2++;
          *fltOut++ = *line3++;
        }
        offset = 0;
        initialLoaded[thrd] = 1;
      } else {

        /* Thereafter just copy the third line into the free slot */
        line3 = &slIn->data.f[B3DMIN(slIn->ysize - 1, oy + 1) * slIn->xsize];
        fltOut += offset;
        for (ox = 0; ox < slIn->xsize; ox++) {
          *fltOut = *line3++;
          fltOut += 3;
        }
        offset = (offset + 1) % 3;
      }

      /* Step across, copying the chunk of values as needed and taking the median */
      for (ox = 1; ox < slIn->xsize - 1; ox++) {
        memcpy(fVals, lineVals + 3 * (ox - 1), 9 * sizeof(float));
        slOut->data.f[ox + oy * slOut->xsize] = opt_med9(fVals);
      }

      /* Handle the endpoints */
      memcpy(fVals, lineVals, 9 * sizeof(float));
      slOut->data.f[oy * slOut->xsize] = opt_med9(fVals);
      memcpy(fVals, lineVals + 3 * (slIn->xsize - 3), 9 * sizeof(float));
      slOut->data.f[slIn->xsize + oy * slOut->xsize - 1] = opt_med9(fVals);
    }
    for (oy = 0; oy < numThreads; oy++) {
      free(fThreadLine[oy]);
      free(fThreadVals[oy]);
    }
    return 0;
  }

  /* Back to general case: get array for data, make sure mode is legal */
  if (slIn->mode == SLICE_MODE_FLOAT) {
    fVals = (float *)malloc(blockSize * sizeof(float));
    if (!fVals)
      return (-1);
    isFloat = 1;
  } else if (slIn->mode == SLICE_MODE_BYTE || slIn->mode == SLICE_MODE_SHORT ||
             slIn->mode == SLICE_MODE_USHORT) {
    iVals = (int *)malloc(blockSize * sizeof(int));
    if (!iVals)
      return (-1);
  } else
    return (-2);

  delMinus = size / 2;
  delPlus = (size + 1) / 2;

  /* Loop on positions */
  for (oy = 0; oy < slIn->ysize; oy++) {
    yStart = B3DMAX(0, oy - delMinus);
    yEnd = B3DMIN(slIn->ysize, oy + delPlus);
    for (ox = 0; ox < slIn->xsize; ox++) {
      xStart = B3DMAX(0, ox - delMinus);
      xEnd = B3DMIN(slIn->xsize, ox + delPlus);
      numVals = stack->zsize * (xEnd - xStart) * (yEnd - yStart);
      select = (numVals + 1) / 2;

      /* Loop on slices and subareas to load arrays */
      switch (slIn->mode) {
      case SLICE_MODE_FLOAT:
        fltOut = fVals;
        for (oz = 0; oz < stack->zsize; oz++)
          for (iy = yStart; iy < yEnd; iy++) {
            fltIn = &stack->vol[oz]->data.f[xStart + iy * slIn->xsize];
            for (ix = xStart; ix < xEnd; ix++)
              *fltOut++ = *fltIn++;
          }
        val[0] = percentileFloat(select, fVals, numVals);
        if (!(numVals % 2))
          val[0] = 0.5 * (val[0] + percentileFloat(select + 1, fVals, numVals));
        break;

      case SLICE_MODE_SHORT:
        FILTER_INTS(shortIn, s);

      case SLICE_MODE_USHORT:
        FILTER_INTS(usIn, us);

      case SLICE_MODE_BYTE:
        FILTER_INTS(byteIn, b);
      }
      slicePutVal(slOut, ox, oy, val);
    }
  }
  if (isFloat)
    free(fVals);
  else
    free(iVals);
  return 0;
}

/*! 
 * Does anisotropic diffusion on slice [sl] using the Perona and Malik 
 * filtering method, based on a program by Alejandro Canterero.
 * [outMode] specifies the output mode; [CC] is edge stopping type (1, 2, 3),
 * [k] is the threshold parameter, [lambda] is the step size, [iterations] is 
 * the number of iterations to run.  [clearFlag] is one of ANISO_CLEAR_AT_END,
 * ANISO_LEAVE_OPEN, or ANISO_CLEAR_ONLY to allow additional iterations to be 
 * done on the existing data.  Returns -1 for memory error.
 */
int sliceAnisoDiff(Islice *sl,  int outMode, int CC, double k, double lambda,
                   int iterations, int clearFlag)
{
  static float **image, **image2, **imout;
  static int iterDone = 0;
  int i, j;
  int n = sl->xsize;
  int m = sl->ysize;

  /* If just clearing, free arrays if allocated, set iterations to 0 */
  if (clearFlag == ANISO_CLEAR_ONLY) {
    if (iterDone) {
      free(image[0]);
      free(image);
      free(image2[0]);
      free(image2);
      iterDone = 0;
    }
    return 0;
  }

  /* Convert slice to float for copying to/from double */
  if (sl->mode != SLICE_MODE_FLOAT && sliceNewMode(sl, SLICE_MODE_FLOAT) < 0)
    return -1;


  /* If no iterations yet, get double arrays */
  if (!iterDone) {
    image = allocate2D_float(m + 2, n + 2);
    if (!image)
      return -1;
    image2 = allocate2D_float(m + 2, n + 2);
    if (!image2) {
      free(image[0]);
      free(image);
      return -1;
    }

    /* Copy data into array */
    for (j = 0; j < m; j++)
      for (i = 0; i < n; i++)
        image[j + 1][i + 1] = sl->data.f[i + j * sl->xsize];
  }

  /* alternate between two matrices to avoid memcopy */	
  /* printf("m = %d n = %d CC = %d k = %f lambda = %f, iter = %d\n",
     m,n,CC,k,lambda, iterations); */
  for (i = 0; i < iterations; i++, iterDone++) {
	if ( iterDone % 2 == 0 ) {
      updateMatrix(image2,image,m,n,CC,k,lambda);
      imout = image2;
	} else {
      updateMatrix(image,image2,m,n,CC,k,lambda);
      imout = image;
	}
  }
  
  /* Copy data back to slice */
  for (j = 0; j < m; j++)
    for (i = 0; i < n; i++)
      sl->data.f[i + j * sl->xsize] = imout[j + 1][i + 1];

  /* Free data if doing one-shot operation */
  if (clearFlag == ANISO_CLEAR_AT_END) {
    free(image[0]);
    free(image);
    free(image2[0]);
    free(image2);
    iterDone = 0;
  }

  /* convert slice to output mode */
  if (outMode != SLICE_MODE_FLOAT && sliceNewMode(sl, outMode) < 0)
    return -1;

  return 0;
}

/*! 
 * Does anisotropic diffusion on slice [sl], which must contain bytes.
 * [image] and [image2] are float arrays of size {xsize} + 2 by {ysize} + 2 
 * that have already been allocated with @@allocate2D_float@.
 * [CC] is edge stopping type (1, 2, 3),
 * [k] is the threshold parameter, [lambda] is the step size, [iterations] is 
 * the number of iterations to run.  [iterDone] is maintained with the total
 * number of iterations done.
 */
void sliceByteAnisoDiff(Islice *sl, float **image, float **image2, 
                        int CC, double k, double lambda, int iterations, 
                        int *iterDone)
{
  float **imout;
  int val;
  int i, j;
  int n = sl->xsize;
  int m = sl->ysize;

  /* If no iterations yet, Copy data into array */
  if (!(*iterDone)) {
    for (j = 0; j < m; j++)
      for (i = 0; i < n; i++)
        image[j + 1][i + 1] = sl->data.b[i + j * sl->xsize];
  }

  /* alternate between two matrices to avoid memcopy */	
  /* printf("m = %d n = %d CC = %d k = %f lambda = %f, iter = %d\n",
     m,n,CC,k,lambda, iterations); */
  for (i = 0; i < iterations; i++, (*iterDone)++) {
	if ( (*iterDone) % 2 == 0 ) {
      updateMatrix(image2,image,m,n,CC,k,lambda);
      imout = image2;
	} else {
      updateMatrix(image,image2,m,n,CC,k,lambda);
      imout = image;
	}
  }
  
  /* Copy data back to slice */
  for (j = 0; j < m; j++) {
    for (i = 0; i < n; i++) {
      val = (int)imout[j + 1][i + 1];
      if (val < 0)
        val = 0;
      if (val > 255)
        val = 255;
      sl->data.b[i + j * sl->xsize] = (unsigned char)val;
    }
  }
}

/*!
 * Allocates a  2D array of doubles of size [m] by [n] that is contiguous in 
 * memory.  The array is actually a 1D vector of length [m] x [n], and this
 * routine returns an array of pointers to the rows.
 */
float **allocate2D_float(int m, int n ) 
{
  float  **a;
  float  *fake;
  int i;
  
  if ( (a = (float **)malloc(sizeof(float *)*m)) == NULL )
    return NULL;
  
  if ( (fake = (float *) malloc(m*n*sizeof(float))) == NULL ) {
    free(a);
    return NULL;
  }

  for (i = 0; i < m; i++)
	a[i] = &(fake[i*n]);
  
  return a;
}


/*!
 * Finds the min and max of slice [s] as rapidly as possible for the four
 * basic data modes, byte, float, and signed and unsigned short integer.
 * Returns 1 for other modes.
 */
int sliceMinMax(Islice *s)
{
  int imin, imax, ival, i;
  float fmin, fmax, fval;
  switch (s->mode) {
  case SLICE_MODE_BYTE:
    imin = imax = s->data.b[0];
    for (i = 1; i < s->xsize * s->ysize; i++) {
      ival = s->data.b[i];
      if (imin > ival)
        imin = ival;
      if (imax < ival)
        imax = ival;
    }
    s->min = imin;
    s->max = imax;
    break;

  case SLICE_MODE_SHORT:
    imin = imax = s->data.s[0];
    for (i = 1; i < s->xsize * s->ysize; i++) {
      ival = s->data.s[i];
      if (imin > ival)
        imin = ival;
      if (imax < ival)
        imax = ival;
    }
    s->min = imin;
    s->max = imax;
    break;

  case SLICE_MODE_USHORT:
    imin = imax = s->data.us[0];
    for (i = 1; i < s->xsize * s->ysize; i++) {
      ival = s->data.us[i];
      if (imin > ival)
        imin = ival;
      if (imax < ival)
        imax = ival;
    }
    s->min = imin;
    s->max = imax;
    break;

  case SLICE_MODE_FLOAT:
    fmin = fmax = s->data.f[0];
    for (i = 1; i < s->xsize * s->ysize; i++) {
      fval = s->data.f[i];
      if (fmin > fval)
        fmin = fval;
      if (fmax < fval)
        fmax = fval;
    }
    s->min = fmin;
    s->max = fmax;
    break;

  default:
    return 1;
  }
  return 0;
}

/*!
 * Determines min and max of slice [sout], determines scaling to match them to
 * the min and max values provided in the byte slice [sin], scales data into
 * [sin] and frees [sout].  To prevent rescaling, set the min and max of [sin]
 * to zero.
 */ 
void sliceScaleAndFree(Islice *sout, Islice *sin)
{
  float aval = 0., mval = 1.;
  int imax, i;

  if (sin->min || sin->max) {
    sliceMinMax(sout);
    mval = (sin->max - sin->min) / (sout->max - sout->min);
    aval = sin->min - mval * sout->min;
  }
  imax = sin->xsize * sin->ysize;
  switch (sout->mode) {
  case SLICE_MODE_FLOAT:
    for(i = 0; i < imax; i++)
      sin->data.b[i] = (unsigned char)(sout->data.f[i] * mval + aval);
    break;
  case SLICE_MODE_SHORT:
    for(i = 0; i < imax; i++)
      sin->data.b[i] = (unsigned char)(sout->data.s[i] * mval + aval);
    break;
  case SLICE_MODE_USHORT:
    for(i = 0; i < imax; i++)
      sin->data.b[i] = (unsigned char)(sout->data.us[i] * mval + aval);
    break;
  case SLICE_MODE_BYTE:
    for(i = 0; i < imax; i++)
      sin->data.b[i] = (unsigned char)(sout->data.b[i] * mval + aval);
    break;
  }
  sliceFree(sout);
}
