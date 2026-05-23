/*
 *  processing.cpp -- Processing functions for clip.
 *
 *  Original author: James Kremer
 *  Revised by: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2020 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#include <limits.h>
#include <math.h>
#include <string.h>
#include "iimage.h"
#include "sliceproc.h"
#include "cfft.h"
#include "clip.h"
#include "parse_params.h"

#ifndef FLT_MIN
#define FLT_MIN INT_MIN
#endif
#ifndef FLT_MAX
#define FLT_MAX INT_MAX
#endif
#define KERNEL_MAXSIZE 7

static void writeBytePixel(float pixel, MrcHeader *hout);
static int quadrantSample(Islice *slice, int llx, int lly, int urx, int ury, 
                          double *mean);
static void correctQuadrant(Islice *slice, int llx, int lly, int urx, int ury, double g,
                            double base);
static int rotateFlipGainReference(float *ref, int &nxGain, int &nyGain, 
                                   int rotationFlip);
static float combineAreaSums(double *areaSums);


/*
 * Common routine for the rescaling options including resize (no scale)
 */
int clip_scaling(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt)
{
  int i,j,k, l, z;
  Ival val;
  Islice *slice;
  double min, alpha;
  float hival, loval, base = 0.;
  int truncLo = 0, truncHi = 0, truncToMean = 0;
  float threshLo = 0., threshHi = 255.;
  char title[80];
  float minForLog = B3DMAX(1.e-20, 1.e-5 * (hin->amax - hin->amin));

  // If no section list is entered, copy the full extended header for unwrap
  bool copyExtra = opt->process == IP_UNWRAP && opt->nofsecs == IP_DEFAULT && 
    opt->oz == IP_DEFAULT;

  z = set_options(opt, hin, hout);
  if (z < 0)
    return(z);

  if (opt->val != IP_DEFAULT)
    base = opt->val;

  /* give message, add title */
  switch (opt->process) {
  case IP_BRIGHTNESS:
    show_status("Brightness...\n");
    mrc_head_label(hout, "clip: brightness");
    min = hin->amin;
    break;
  case IP_SHADOW:
    show_status("Shadow...\n");
    mrc_head_label(hout, "clip: shadow");
    min = hin->amax;
    break;
  case IP_CONTRAST:
    show_status("Contrast...\n");
    mrc_head_label(hout, "clip: contrast");
    min = hin->amean;
    break;
  case IP_RESIZE:
    mrc_head_label(hout, "clip: resized image");
    break;
  case IP_THRESHOLD:
    show_status("Threshold...\n");
    if (opt->sano) {
      threshLo = hin->amin;
      threshHi = hin->amax;
    }
    if (opt->low != IP_DEFAULT)
      threshLo = opt->low;
    if (opt->high != IP_DEFAULT)
      threshHi = opt->high;
    if (opt->thresh == IP_DEFAULT) {
      show_error("clip threshold: You must enter a threshold value");
      return -1;
    }
    if (opt->minSize != IP_DEFAULT)
      return thresholdWithMinSize(hin, hout, opt, threshLo, threshHi, z);
    mrc_head_label(hout, "clip: thresholded");
    break;
  case IP_TRUNCATE:
    show_status("Truncate...\n");
    if (opt->low != IP_DEFAULT)
      truncLo = 1;
    if (opt->high != IP_DEFAULT)
      truncHi = 1;
    if (opt->sano)
      truncToMean = 1;
    if (!truncLo && !truncHi) {
      show_error("clip truncate: You must enter a low or a high limit");
      return -1;
    }
    mrc_head_label(hout, "clip: truncated");
    break;
  case IP_UNWRAP:
    show_status("Unwrap...\n");
    if (hin->mode != MRC_MODE_SHORT && hin->mode != MRC_MODE_USHORT) {
      show_error("clip truncate: Mode must be short or unsigned short integers");
      return -1;
    }
    if (hin->mode == MRC_MODE_USHORT && opt->val == IP_DEFAULT) {
      show_error("clip truncate: You must enter a value to add with -n for mode 6 input");
      return -1;
    }
    if (opt->val == IP_DEFAULT)
      opt->val = 32768.;
    mrc_head_label(hout, "clip: unwrapped integer values");
    if (copyExtra && mrcCopyExtraHeader(hin, hout))
      show_warning("clip warning: failed to copy extra header data");
    break;
  case IP_LOGARITHM:
    show_status("Logarithm...\n");
    sprintf(title, "clip: logarithm after adding %g", base);
    mrc_head_label(hout, title);
    break;
  case IP_SQROOT:
    show_status("Square root...\n");
    sprintf(title, "clip: square root after adding %g", base);
    mrc_head_label(hout, title);
    break;
  default:
    return(-1);
  }

  if (opt->val == IP_DEFAULT)
    opt->val = 1.0f;
  alpha = opt->val;

  for (k = 0; k < opt->nofsecs; k++) {
    slice = sliceReadSubm(hin, opt->secs[k], 'z', opt->ix, opt->iy,
                          (int)opt->cx, (int)opt->cy);
    if (!slice){
      show_error("clip: Error reading slice.");
      return(-1);
    }

    if ((opt->process == IP_LOGARITHM || opt->process == IP_SQROOT) && 
        hout->mode == MRC_MODE_FLOAT && sliceFloat(slice) < 0) {
      show_error("clip: Error getting memory to convert slice to float.");
      return(-1);
    }

    /* 2D: Scale based on min/max/mean of individual slice */
    if ((opt->dim == 2 && opt->process != IP_RESIZE) || 
        (opt->process == IP_TRUNCATE && truncToMean)) {
      sliceMMM(slice);
      if (opt->process == IP_BRIGHTNESS)
        min = (double)slice->min;
      else if (opt->process == IP_SHADOW)
        min = (double)slice->max;
      else
        min = (double)slice->mean;
    }
      
    if (opt->process == IP_THRESHOLD) {
      for (j = 0; j < opt->iy; j++) {
        for (i = 0; i < opt->ix; i++) {
          sliceGetVal(slice, i, j, val);
          for (l = 0; l < slice->csize; l++) {

            // The <= makes the result the same as in 3dmod
            if (val[l] <= opt->thresh)
              val[l] = threshLo;
            else
              val[l] = threshHi;
          }
          slicePutVal(slice, i, j, val);
        }
      }

    } else if (opt->process == IP_TRUNCATE) {
      for (j = 0; j < opt->iy; j++) {
        for (i = 0; i < opt->ix; i++) {
          sliceGetVal(slice, i, j, val);
          for (l = 0; l < slice->csize; l++) {
            if (truncLo && val[l] < opt->low)
              val[l] = truncToMean ? min : opt->low;
            if (truncHi && val[l] > opt->high)
              val[l] = truncToMean ? min : opt->high;
          }
          slicePutVal(slice, i, j, val);
        }
      }

    } else if (opt->process == IP_UNWRAP) {

      /* Unwrap: add a value and wrap values around */
      hival = hin->mode == MRC_MODE_USHORT ? 65535 : 32767;
      loval = hival - 65535.;
      for (j = 0; j < opt->iy; j++) {
        for (i = 0; i < opt->ix; i++) {
          sliceGetVal(slice, i, j, val);
          val[0] += opt->val;
          if (val[0] > hival)
            val[0] -= 65536.;
          else if (val[0] < loval)
            val[0] += 65536.;
          slicePutVal(slice, i, j, val);
        }
      }

    } else if (opt->process == IP_LOGARITHM) {
      for (j = 0; j < opt->iy; j++) {
        for (i = 0; i < opt->ix; i++) {
          sliceGetVal(slice, i, j, val);
          for (l = 0; l < slice->csize; l++)
            val[l] = log10(B3DMAX(minForLog, val[l] + base));
          slicePutVal(slice, i, j, val);
        }
      }

    } else if (opt->process == IP_SQROOT) {
      for (j = 0; j < opt->iy; j++) {
        for (i = 0; i < opt->ix; i++) {
          sliceGetVal(slice, i, j, val);
          for (l = 0; l < slice->csize; l++)
            val[l] = sqrt(B3DMAX(0., val[l] + base));
          slicePutVal(slice, i, j, val);
        }
      }

    } else if (opt->process != IP_RESIZE)
      mrc_slice_lie(slice, min, alpha);

    if (opt->readDefects && correctDefects(slice, hin->nx, hin->ny, opt))
      return -1;
    
    if (clipWriteSlice(slice, hout, opt, k, &z, 1))
      return -1;
  }

  return set_mrc_coords(opt);  
}


/*
 * Common routine for the edge filters that are not simple convolutions
 */
int clipEdge(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt)
{
  Islice *s;
  int k, z;
  Islice *slice;
  char *message;
  double scale, fixval;

  if (opt->mode == IP_DEFAULT)
    opt->mode = (opt->process == IP_GRADIENT) ? hin->mode : MRC_MODE_BYTE;

  if (hin->mode != MRC_MODE_BYTE && hin->mode != MRC_MODE_SHORT && 
      hin->mode != MRC_MODE_USHORT && hin->mode != MRC_MODE_FLOAT &&
      !(hin->mode == MRC_MODE_COMPLEX_FLOAT && opt->process != IP_GRADIENT)) {
    show_error("clip edge: only byte, integer and float modes can be used");
    return -1;
  }

  z = set_options(opt, hin, hout);
  if (z < 0)
    return(z);

  /* give message, add title */
  switch (opt->process) {
  case IP_GRADIENT:
    message = "Taking gradient of";
    mrc_head_label(hout, "clip: gradient");
    break;
  case IP_PREWITT:
    message = "Applying Prewitt filter to";
    mrc_head_label(hout, "clip: Prewitt filter");
    break;
  case IP_GRAHAM:
    message = "Applying Graham filter to";
    mrc_head_label(hout, "clip: Graham filter");
    break;
  case IP_SOBEL:
    message = "Applying Sobel filter to";
    mrc_head_label(hout, "clip: Sobel filter");
    break;
  default:
    return(-1);
  }

  printf("clip: %s %d slices...\n", message, opt->nofsecs);
  for (k = 0; k < opt->nofsecs; k++) {
    s = sliceReadSubm(hin, opt->secs[k], 'z', opt->ix, opt->iy,
                          (int)opt->cx, (int)opt->cy);
    if (!s){
      show_error("clip: Error reading slice.");
      return(-1);
    }

    if (opt->process == IP_GRADIENT) {

      /* Do gradient */
      slice = sliceGradient(s);
      sliceFree(s);
    } else {

      /* Otherwise scale slice and convert slice if not bytes */
      slice = s;
      if (slice->mode != SLICE_MODE_BYTE) {
        sliceMinMax(slice);
        scale = 1.;
        if (slice->max > slice->min)
          scale = 255. / (slice->max - slice->min);
        if (scale >= 0.95 && scale <= 1.05)
          scale = 0.95;
        fixval = slice->min * scale / (scale - 1.);
        mrc_slice_lie(slice, fixval, scale);
        sliceNewMode(slice, SLICE_MODE_BYTE);
      }

      /* Do selected filter after getting min/max quickly */
      sliceMinMax(slice);
      if (opt->process == IP_GRAHAM)
        sliceByteGraham(slice);
      else if (opt->process == IP_SOBEL)
        sliceByteEdgeSobel(slice);
      else
        sliceByteEdgePrewitt(slice);
    }

    if (clipWriteSlice(slice, hout, opt, k, &z, 1))
      return -1;
  }
  return set_mrc_coords(opt);  
}

/*
 * Common routine for kernel convolution filtering
 */
int clip_convolve(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt)
{
  Islice *s;
  float *blur;
  int k, z, iter, dim = 3, niter = 1;
  char title[100];
  float smoothKernel[] = {
    0.0625f, 0.125f, 0.0625f,
    0.125f,  0.25f,  0.125f, 
    0.0625f, 0.125f, 0.0625f};
  float sharpenKernel[] = {
    -1.,  -1., -1.,
    -1.,  9., -1.,
    -1.,  -1., -1.};
  float laplacianKernel[] = {
    1.,  1., 1.,
    1.,  -4., 1.,
    1.,  1., 1.};
  float gaussianKernel[KERNEL_MAXSIZE * KERNEL_MAXSIZE];
  Islice *slice;
  char *message;
  bool smooth3D = false;

  if (opt->mode == IP_DEFAULT)
    opt->mode = (opt->process == IP_SMOOTH) ? hin->mode : MRC_MODE_FLOAT;

  z = set_options(opt, hin, hout);
  if (z < 0)
    return(z);

  /* give message, add title */
  switch (opt->process) {
  case IP_SMOOTH:
    if (opt->val < 0) {
      smooth3D = true;
      if (opt->dim == 2) {
        show_error("clip smooth: the -2d option cannot be entered with smoothing in 3D");
        return -1;
      }
      if (opt->low == IP_DEFAULT)
        opt->low = 0.85;
    }
    if (opt->val > 1.)
      niter = B3DNINT(opt->val);
    if (opt->low > 0.) {
      scaledGaussianKernel(&gaussianKernel[0], &dim, KERNEL_MAXSIZE, opt->low);
      if (smooth3D) {
        if (hin->nz < dim) {
          sprintf(title, "clip smooth: 3D smoothing with sigma %.2f requires %d slices; "
                  "input has only %d", opt->low, dim, hin->nz);
          show_error(title);
          return -1;
        }
        message = "Gaussian kernel 3D smoothing";
        sprintf(title, "clip: Gaussian 3D smoothing, sigma %.2f", opt->low);
      } else {
        message = "Gaussian kernel smoothing";
        sprintf(title, "clip: Gaussian smoothing, sigma %.2f, %d iterations",
                opt->low, niter);
      }
      blur = gaussianKernel;
    } else {
      message = "Smoothing";
      sprintf(title, "clip: Standard smoothing, %d iterations", niter);
      blur = smoothKernel;
    }
    mrc_head_label(hout, title);
    break;
  case IP_SHARPEN:
    message = "Sharpening";
    mrc_head_label(hout, "clip: sharpen");
    blur = sharpenKernel;
    break;
  case IP_LAPLACIAN:
    message = "Applying Laplacian to";
    mrc_head_label(hout, "clip: Laplacian");
    blur = laplacianKernel;
    break;
  default:
    return(-1);
  }

  printf("clip: %s %d slices...\n", message, opt->nofsecs);
  if (smooth3D)
    return clipMedian(hin, hout, opt, blur, dim, z);

  for (k = 0; k < opt->nofsecs; k++) {
    s = sliceReadSubm(hin, opt->secs[k], 'z', opt->ix, opt->iy,
                          (int)opt->cx, (int)opt->cy);
    if (!s){
      show_error("clip: Error reading slice.");
      return(-1);
    }

    for (iter = 0; iter < niter; iter++) {
      s->mean = hin->amean;
      if (opt->process == IP_SMOOTH)
        sliceMMM(s);
      slice = slice_mat_filter(s, blur, dim);
      if (!slice) {
        show_error("clip: Error getting new slice for filtering.");
        return(-1);
      }
      sliceFree(s);
      s = slice;
    }

    if (clipWriteSlice(slice, hout, opt, k, &z, 1))
      return -1;
  }

  return set_mrc_coords(opt);  
}

/*
 * 2D or 3D median filtering and 3D gaussian kernel smoothing
 */
int clipMedian(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt, float *kernel, int size,
               int z)
{
  Islice *s, *sfilt;
  int i, j, k;
  Islice *slice;
  Istack v;
  char title[40];
  int firstInVol, lastInVol, firstNeed, lastNeed, depth;
  float zKernel[KERNEL_MAXSIZE], *dataPtr, zsum = 0;

  if (!kernel) {
    if (hin->mode != MRC_MODE_BYTE && hin->mode != MRC_MODE_SHORT && 
        hin->mode != MRC_MODE_USHORT && hin->mode != MRC_MODE_FLOAT) {
      show_error("clip median: only byte, integer and float modes can be used");
      return -1;
    }

    z = set_options(opt, hin, hout);
    if (z < 0)
      return(z);

    if (opt->val == IP_DEFAULT)
      opt->val = 3;
    if (opt->mode != MRC_MODE_BYTE && opt->mode != MRC_MODE_SHORT && 
        hin->mode != MRC_MODE_USHORT && opt->mode != MRC_MODE_FLOAT)
      opt->mode = hin->mode;
    size = (int)B3DMAX(2, opt->val);

    sprintf(title, "clip: %dD median filter, size %d", opt->dim, size);
    mrc_head_label(hout, title);
    printf("clip: median filtering %d slices...\n", opt->nofsecs);
  } else {

    // Basic setup was already done in the convolve function, start here for smoothing
    // And get the weighting Z from the midline of the kernel
    for (k = 0; k < size; k++) {
      zKernel[k] = kernel[k + size * (size / 2)];
      zsum += zKernel[k];
    }
    for (k = 0; k < size; k++)
      zKernel[k] /= zsum;
  }

  depth = opt->dim == 2 ? 1 : size;
  v.vol = (Islice **)malloc(depth * sizeof(Islice *));
  v.zsize = 0;

  for (k = 0; k < opt->nofsecs; k++) {

  /* Get slice for output.  Smoothing needs every time because float can turn to other
   * mode in saving */
    if (!k || kernel) {
      slice = sliceCreate(opt->ix, opt->iy, kernel ? SLICE_MODE_FLOAT : opt->mode);
      if (!slice)
        return (-1);
    }

    /* Set up slice numbers needed in volume (amazingly works for kernel case too) */
    if (opt->dim == 2) {
      firstNeed = lastNeed = opt->secs[k];
    } else if (kernel) {
      firstNeed = B3DMAX(0, opt->secs[k] - size / 2);
      lastNeed = B3DMIN(hin->nz - 1, firstNeed + size - 1);
      firstNeed = B3DMAX(0, lastNeed + 1 - size);
    } else {
      firstNeed = B3DMAX(0, opt->secs[k] - size / 2);
      lastNeed = B3DMIN(hin->nz - 1, opt->secs[k] + (size - 1) / 2);
    }
    //printf("k = %d, need %d to %d\n", k, firstNeed, lastNeed);

      /* Free or copy down existing slices in volume */
    for (i = 0, j = 0; i < v.zsize; i++) {
      if (firstInVol + i < firstNeed || firstInVol + i > lastNeed) {
        //printf("freeing %d  %p\n", firstInVol + i, v.vol[i]);
        sliceFree(v.vol[i]);
      } else {
        //printf("copying %d: %d to %d   %p \n", firstInVol + i, i, j, v.vol[i]);
        v.vol[j++] = v.vol[i];
      }
    }
    v.zsize = j;
    if (j)
      firstInVol = lastInVol + 1 - j;

    /* Load needed slices */
    for (i = firstNeed; i <= lastNeed; i++) {
      if (!v.zsize || i > lastInVol) {
        s = sliceReadSubm(hin, i, 'z', opt->ix, opt->iy,
                                         (int)opt->cx, (int)opt->cy);
        //printf("loaded %d into %d mode %d   %p\n", i,  v.zsize, hin->mode, s);
        if (!s)
          return -1;

        // Smoothing: filter the slice and replace it
        if (kernel) {
          sfilt = slice_mat_filter(s, kernel, size);
          if (!sfilt) {
            show_error("clip: Error getting filtered slice.");
            return(-1);
          }
          sliceFree(s);
          s = sfilt;
        }
        if (!v.zsize)
          firstInVol = i;
        v.vol[v.zsize++] = s;
        lastInVol = i;
      }
    }

    /* Filter and maintain mmm and write; smoothing adds up slices in volume with 
       weights */
    if (kernel) {
      memset(slice->data.f, 0, opt->ix * opt->iy * 4);
      for (i = 0; i < size; i++) {
        j = i + (k - size / 2) - firstInVol;
        B3DCLAMP(j, 0, size - 1);
        dataPtr = v.vol[j]->data.f;
        for (j = 0; j < opt->ix * opt->iy; j++)
          slice->data.f[j] += zKernel[i] * dataPtr[j];
      }
    } else if (sliceMedianFilter(slice, &v, size)) {
      return -1;
    }
    if (clipWriteSlice(slice, hout, opt, k, &z, kernel ? 1 : 0))
      return -1;
  }
  
  /* clean up */
  if (!kernel)
    sliceFree(slice);
  for (i = 0; i < v.zsize; i++)
    sliceFree(v.vol[i]);
  return set_mrc_coords(opt);  
}

/*
 * Anisotropic diffusion on slices
 */
int clipDiffusion(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt)
{
  int k, z;
  Islice *slice;
  double kk, lambda;
  int iterations, CC;

  if (hin->mode != MRC_MODE_BYTE && hin->mode != MRC_MODE_SHORT && 
      hin->mode != MRC_MODE_USHORT && hin->mode != MRC_MODE_FLOAT) {
    show_error("clip diffusion: only byte, integer and float modes can "
               "be used");
    return -1;
  }

  z = set_options(opt, hin, hout);
  if (z < 0)
    return(z);

  /* Set the parameters supplying weird defaults */
  if (opt->val == IP_DEFAULT)
    opt->val = 5.;
  iterations = B3DMAX(1, (int)opt->val);
  if (opt->thresh == IP_DEFAULT)
    opt->thresh = 2;
  CC = B3DMAX(1, B3DMIN(3, (int)(opt->thresh)));
  if (opt->weight == IP_DEFAULT)
    opt->weight = 2.;
  kk = B3DMAX(0, opt->weight);
  if (opt->low == IP_DEFAULT)
    opt->low = 0.2f;
  lambda = B3DMAX(0.001, opt->low);

  /* give message, add title */
  mrc_head_label(hout, "clip: diffusion");

  printf("clip: anistropic diffusion %d slices...\n", opt->nofsecs);
  for (k = 0; k < opt->nofsecs; k++) {
    slice = sliceReadSubm(hin, opt->secs[k], 'z', opt->ix, opt->iy,
                          (int)opt->cx, (int)opt->cy);
    if (!slice){
      show_error("clip: Error reading slice.");
      return(-1);
    }

    if (sliceAnisoDiff(slice, opt->mode, CC, kk, lambda, iterations, 
                       ANISO_CLEAR_AT_END))
      return -1;

    if (clipWriteSlice(slice, hout, opt, k, &z, 1))
      return -1;
  }

  return set_mrc_coords(opt);  
}

#define ADD_TO_WALL_SUM(a)                      \
  /*now = wallTime();                           \
  wallRead += wallTime() - wall;                \
  wall = now;*/


/*
 * All flipping operations
 */
int clip_flip(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt)
{
  int i,j,k, rotx, numDone, numTodo, maxSlices, yst, ynd, ydir, csize, dsize;
  int chunk, numLoadSlices, nyLoadSlice, maxLoad;
  int tileSizes[3] = {0, 0, 0}, numTiles[3], chunkLims[3] = {0, 0, 0};
  ImodImageFile *iiFile;
  int err, newMode;
  float memlim, memmin = 512000000., memmax = 2900000000.;
  float chunkXYcrit = 1.e6;
  size_t lineOfs, sliceOfs;
  float ycen, zcen;
  Islice *sl, *tsl, *outSlice;
  Islice **yslice;
  Ival val;
  IloadInfo li;

  /* Change mode (undone for yz/rotx).  For most variations, it gets and frees a slice
   inside the loop when the mode is changing, outside the loop otherwise */
  hout->mode = hin->mode;
  if (opt->mode != IP_DEFAULT)
    hout->mode = opt->mode;
  newMode = hout->mode != hin->mode ? 1 : 0;

  /* XY */
  if ((!strncmp(opt->command, "flipxy",6)) || 
      (!strncmp(opt->command, "flipyx",6))){
    mrc_head_label(hout, "clip: flipxy");
    hout->nz = hin->nz;
    hout->nx = hin->ny;
    hout->ny = hin->nx;
    hout->mz = hin->mz;
    hout->mx = hin->my;
    hout->my = hin->mx;
    hout->zlen = hin->zlen;
    hout->xlen = hin->ylen;
    hout->ylen = hin->xlen;
    if (setChunkOutput(opt, hout, chunkLims, numTiles, tileSizes))
      return -1;
    if (mrc_head_write(hout->fp, hout))
      return -1;
    if (!newMode)
      sl = sliceCreate(hout->nx, hout->nz, hout->mode);
    for(i = 0, j = 0; i < hin->nx; i++, j++){
      if (newMode)
        sl = sliceCreate(hout->nx, hout->nz, hin->mode);
      if (!sl)
        return -1;
      if (mrc_read_slice(sl->data.b, hin->fp, hin, i, 'x'))
        return -1;
      if (newMode && sliceNewMode(sl, hout->mode) < 0)
        return -1;
      if (opt->sano)
        sliceMirror(sl, 'y');
      if (mrc_write_slice(sl->data.b, hout->fp, hout, j, 'y'))
        return -1;
      if (newMode)
        sliceFree(sl);
    }
    if (!newMode)
      sliceFree(sl);
    puts(" Done!");
    return(0);
  }

  /* XZ */
  if ((!strncmp(opt->command, "flipzx",6)) ||
      (!strncmp(opt->command, "flipxz",6))){
    mrc_head_label(hout, "clip: flipxz");
    hout->nx = hin->nz;
    hout->mx = hin->mz;
    hout->xlen = hin->zlen;
    hout->ny = hin->ny;
    hout->my = hin->my;
    hout->ylen = hin->ylen;
    hout->nz = hin->nx;
    hout->mz = hin->mx;
    hout->zlen = hin->xlen;
    if (setChunkOutput(opt, hout, chunkLims, numTiles, tileSizes))
      return -1;
    if (mrc_head_write(hout->fp, hout))
      return -1;
    sl = sliceCreate(hin->ny, hin->nz, hin->mode);

    /* DNM 3/23/01: need to transpose into a new slice because it is
       read in as a y by z slice, not a z by y slice */
    tsl = sliceCreate(hout->nx, hout->ny, hout->mode);
    if (!sl || !tsl)
      return -1;
    for(k = 0; k < hin->nx; k++){
      if (mrc_read_slice(sl->data.b, hin->fp, hin, k, 'x'))
        return -1;
      for (i = 0; i < hin->ny; i++)
        for (j = 0; j < hin->nz; j++) {
          sliceGetVal(sl, i, j, val);
          slicePutVal(tsl, j, i, val);
        }
      if (mrc_write_slice(tsl->data.b, hout->fp, hout, k, 'z'))
        return -1;
    }
    sliceFree(sl);
    sliceFree(tsl);
    puts(" Done!");
    return(0);
  }

  /* YZ and ROTX */
  if ((!strncmp(opt->command, "flipyz",6)) ||
      (!strncmp(opt->command, "flipzy",6)) ||
      (!strncmp(opt->command, "rotx",4))) {
    rotx = strncmp(opt->command, "rotx",4) ? 0 : 1;
    if (rotx)
      mrc_head_label(hout, "clip: rotx - rotation by -90 around X");
    else
      mrc_head_label(hout, "clip: flipyz");

    /* This one is not allowed to change mode */
    hout->mode = hin->mode;
    hout->nx = hin->nx;
    hout->mx = hin->mx;
    hout->xlen = hin->xlen;
    hout->ny = hin->nz;
    hout->my = hin->mz;
    hout->ylen = hin->zlen;
    hout->nz = hin->ny;
    hout->mz = hin->my;
    hout->zlen = hin->ylen;

    /* For rotation try to adjust the header as in rotatevol */
    if (rotx && hin->my && hin->ylen && hin->mz && hin->zlen ) {
      for (i = 0; i < 3; i++)
        hout->tiltangles[i] = hout->tiltangles[i + 3];
      hout->tiltangles[3] -= 90.;
      ycen = hin->ny / 2. - hin->yorg * hin->my / hin->ylen;
      zcen = hin->nz / 2. - hin->zorg * hin->mz / hin->zlen;
      hout->yorg = (hout->ny / 2. - zcen) * hout->ylen / hout->my;
      hout->zorg = (hout->nz / 2. + ycen) * hout->zlen / hout->mz;
    }
      
    // Get the memory limit
    if (sizeof(void *) > 4)
      memmax = 0.4 * b3dPhysicalMemory();
    mrc_getdcsize(hout->mode, &dsize, &csize);
    memlim = ((((float)dsize * hout->nx) * hout->ny) * hout->nz);
    memlim = B3DMAX(memmin, B3DMIN(memmax, memlim));
    //memlim = 1.e8;

    /* Get the maximum number of full output slices to load */
    maxSlices = (int)((memlim / (dsize * hout->nx)) / hout->ny);
    maxSlices = B3DMAX(1, B3DMIN(hout->nz, maxSlices));
    k = (hout->nz + maxSlices - 1) / maxSlices;
    maxSlices = (hout->nz + k - 1) / k;
    numLoadSlices = hin->nz;
    nyLoadSlice = maxSlices;
    
    // If output is HDF, and this is not full size of input in Y, see if chunks can be 
    // done and full slices loaded for that
    iiFile = iiLookupFileFromFP(hout->fp);
    tileSizes[1] = 0;
    if (b3dOutputFileType() == OUTPUT_TYPE_HDF && iiFile && maxSlices < hout->nz) {
      maxLoad = (int)((memlim / (dsize * hout->nx)) / hout->nz);
      maxLoad = B3DMAX(1, B3DMIN(hout->ny, maxLoad));
      k = (hout->ny + maxLoad - 1) / maxLoad;
      maxLoad = (hout->ny + k - 1) / k;
      //printf("maxload %d  xych %f\n", maxLoad, maxLoad * ((float)dsize * hout->nx));
      if (maxLoad < hout->ny && maxLoad * ((float)dsize * hout->nx) > chunkXYcrit) {
        
        // Get a tile size that is no more than this
        if (opt->chunkY == IP_DEFAULT)
          opt->chunkY = maxLoad;
        opt->chunkY = B3DMIN(opt->chunkY, maxLoad);
        chunkLims[1] = maxLoad;
        if (setChunkOutput(opt, hout, chunkLims, numTiles, tileSizes))
          return -1;
        numLoadSlices = tileSizes[1];
        nyLoadSlice = hin->ny;
        //printf("cy %d ts %d  nt %d\n", opt->chunkY, tileSizes[1], numTiles[1]);
      }
    }
    if (!tileSizes[1] && setChunkOutput(opt, hout, chunkLims, numTiles, tileSizes))
      return -1;
    if (mrc_head_write(hout->fp, hout))
      return -1;

    // Get the slice storage
    yslice = (Islice **)malloc(numLoadSlices * sizeof(Islice));
    outSlice = sliceCreate(hout->nx, numLoadSlices, hout->mode);
    if (!yslice || !outSlice) {
      printf("ERROR: CLIP - getting memory for slice array or output slice\n");
      return (-1);
    }
    for (k = 0; k < numLoadSlices; k++) {
      yslice[k] = sliceCreate(hout->nx, nyLoadSlice, hout->mode);
      if (!yslice[k]) {
        printf("ERROR: CLIP - getting memory for slices\n");
        return (-1);
      }
    }
    //double wallStart = wallTime(), wallRead, wallCopy, wallWrite, wall, now;

    mrc_init_li(&li, NULL);
    mrc_init_li(&li, hin);
    numDone = 0;
    if (tileSizes[1]) {
      /*printf("memlim %.0f  tileSize %d memory %.f chunks %d\n", memlim, tileSizes[1], 
        (float)(hout->nx * nyLoadSlice) * numLoadSlices * dsize, numTiles[1]);*/

      iiFile->llx = 0;
      iiFile->urx = iiFile->nx - 1;
      iiFile->padLeft = iiFile->padRight = 0;
      for (chunk = 0; chunk < numTiles[1]; chunk++) {
        numTodo = B3DMIN(tileSizes[1], hin->nz - numDone);
        //wall = wallTime();
        for (k = 0; k < numTodo; k++) {
          if ((err = mrcReadZ(hin, &li, yslice[k]->data.b, k + numDone))) {
            printf("ERROR: CLIP - Reading full section %d (error # %d)\n", k,  err);
            return -1;
          }
        }
        ADD_TO_WALL_SUM(wallRead);

        // Set limits for output loop
        if (rotx) {
          ydir = -1;
          yst = hin->ny - 1;
          ynd = 0;
        } else {
          ydir = 1;
          yst = 0;
          ynd = hin->ny - 1;
        }
        
        /* Write current portion of Z slices in order after copying into output slice */
        for (j = yst; j * ydir <= ynd * ydir; j += ydir) {
          lineOfs = (size_t)(hout->nx * dsize) * (size_t)j;
          sliceOfs = 0;
          for (k = 0; k < numTodo; k++) {
            memcpy(outSlice->data.b + sliceOfs, yslice[k]->data.b + lineOfs, dsize *
                   hout->nx);
            sliceOfs += (size_t)(hout->nx * dsize);
          }
          ADD_TO_WALL_SUM(wallCopy);
          
          iiFile->lly = numDone;
          iiFile->ury = numDone + numTodo - 1;
          k = ydir * (j - yst);
          if (iiWriteSection(iiFile, (char *)outSlice->data.b, k)) {
            printf("ERROR: CLIP - Writing y %d to %d of section %d\n", iiFile->lly,
                   iiFile->ury, k);
            return -1;
          }
          ADD_TO_WALL_SUM(wallWrite);
        }
        numDone += numTodo;
        /*printf("%d done at %.1f  read %.2f  copy %.2f  write %.2f\n", numDone, 
          now - wallStart, wallRead, wallCopy, wallWrite); fflush(stdout);*/
      }

    } else {
      /*printf("memlim %.0f  maxslices %d memory %.f chunks %d\n", memlim, maxSlices, 
           (float)(hout->nx * maxSlices) * hin->nz * dsize,
           (hout->nz + maxSlices - 1) /maxSlices);*/

      /* Loop on chunks in Z of output */
      while (numDone < hout->nz) {
        numTodo = B3DMIN(maxSlices, hout->nz - numDone);

        /* Set up loading limits and limits for output loop */
        if (rotx) {
          li.ymax = hout->nz - 1 - numDone;
          li.ymin  = li.ymax - (numTodo - 1);
          ydir = -1;
          yst = numTodo - 1;
          ynd = 0;
        } else {
          li.ymin = numDone;
          li.ymax = numDone + numTodo - 1;
          ydir = 1;
          ynd = numTodo - 1;
          yst = 0;
        }

        /* Load the slices within the Y range */
        //wall = wallTime();
        for (k = 0; k < hin->nz; k++) {
          if ((err = mrcReadZ(hin, &li, yslice[k]->data.b, k))) {
            printf("ERROR: CLIP - Reading section %d, y %d to %d (error # %d)\n",
                   k, li.ymin, li.ymax, err);
            return -1;
          }
        }

        ADD_TO_WALL_SUM(wallRead);

        /* Write Z slices in order after copying into output slice */
        for (j = yst; j * ydir <= ynd * ydir; j += ydir) {
          lineOfs = (size_t)(hout->nx * dsize) * (size_t)j;
          sliceOfs = 0;
          for (k = 0; k < hin->nz; k++) {
            memcpy(outSlice->data.b + sliceOfs, yslice[k]->data.b + lineOfs, dsize *
                   hout->nx);
            sliceOfs += (size_t)(hout->nx * dsize);
          }
          ADD_TO_WALL_SUM(wallCopy);

          if (mrc_write_slice(outSlice->data.b, hout->fp, hout, numDone, 'z')) {
            printf("ERROR: CLIP - Writing section %d\n", numDone);
            return -1;
          }
          numDone++;
          ADD_TO_WALL_SUM(wallWrite);
        }
        /*printf("%d done at %.1f  read %.2f  copy %.2f  write %.2f\n", numDone, 
          now - wallStart, wallRead, wallCopy, wallWrite); fflush(stdout);*/
      }
    }

    /* Clean up */
    for (k = 0; k < numLoadSlices; k++)
      sliceFree(yslice[k]);
    sliceFree(outSlice);
    puts(" Done!");
    return(0);
  }

  /* X or Y */
  if ((!strncmp(opt->command, "flipx",5)) ||
      (!strncmp(opt->command, "flipy",5))){
    if (!strncmp(opt->command, "flipx",5))
      mrc_head_label(hout, "clip: flipx");
    else
      mrc_head_label(hout, "clip: flipy");
    hout->nx = hin->nx;
    hout->ny = hin->ny;
    hout->nz = hin->nz;
    /* DNM 3/21/01: added head_write here and below */
    if (mrc_head_write(hout->fp, hout))
      return -1;
    if (!newMode)
      sl = sliceCreate(hout->nx, hout->ny, hin->mode);
    for (k = 0; k < hin->nz; k++){
      if (newMode)
        sl = sliceCreate(hout->nx, hout->ny, hin->mode);
      if (!sl)
        return -1;
      if (mrc_read_slice(sl->data.b, hin->fp, hin, k, 'z'))
        return -1;
      if (newMode && sliceNewMode(sl, hout->mode) < 0)
        return -1;
      sliceMirror(sl, opt->command[4]);
      if (mrc_write_slice(sl->data.b, hout->fp, hout, k, 'z'))
        return -1;
      if (newMode)
        sliceFree(sl);
    }
    if (!newMode)
      sliceFree(sl);
    puts(" Done!");
    return(0);
  }

  /* Z */
  if (!strncmp(opt->command, "flipz",5)){
    mrc_head_label(hout, "clip: flipz");
    hout->nx = hin->nx;
    hout->ny = hin->ny;
    hout->nz = hin->nz;
    if (mrc_head_write(hout->fp, hout))
      return -1;
    if (!newMode)
      sl = sliceCreate(hout->nx, hout->ny, hout->mode);

    /* DNM 11/14/08: switch to doing file in order, don't pretend it could be
       done on a file in place */
    for (k = 0; k < hin->nz; k++){
      if (newMode)
        sl = sliceCreate(hout->nx, hout->ny, hin->mode);
      if (!sl)
        return -1;
      if (mrc_read_slice(sl->data.b, hin->fp, hin, hout->nz-k-1, 'z'))
        return -1;
      if (newMode && sliceNewMode(sl, hout->mode) < 0)
        return -1;
      if (mrc_write_slice(sl->data.b, hout->fp, hout, k, 'z'))
        return -1;
      if (newMode)
        sliceFree(sl);
    }
    if (!newMode)
      sliceFree(sl);
    puts(" Done!");
    return(0);
  }
	  
  show_warning("clip flip - no flipping was done.");
  return(-1);
}

/*
 * Quadrant correction based on averaging
 */
int clip_quadrant(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt)
{
  int width = 20;
  int numGroups, itmp, group, lastOut, newMode = -1;
  int nx = hin->nx, ny = hin->ny, nz = hin->nz;
  int numTodo = nz;
  int groupSize = 1;
  int iz, jz, ind, indStart, indEnd, i, ninGroup;
  double d[8], dt[8], g1, g2, g3, g4, base, c2, c3, c4, qmin, qmax, denom, userBase = 0;
  int vx1, vx2, vx3, vx4, hx1, hx2, hx3, hx4, vy1, vy2, vy3, vy4, hy1, hy2, hy3, hy4;
  Islice *slice;

  if ((opt->ix !=  IP_DEFAULT) || (opt->iy !=  IP_DEFAULT) || (opt->ox !=  IP_DEFAULT) ||
      (opt->oy !=  IP_DEFAULT) || (opt->oz !=  IP_DEFAULT))
    show_warning("clip quadrant - input and output sizes ignored.");
  
  if (opt->nofsecs != IP_DEFAULT) {
    numTodo = opt->nofsecs;

    /* Sort the entries */
    for (iz = 0; iz < numTodo - 1; iz++) {
      for (jz = iz + 1; jz < numTodo; jz++) {
        if (opt->secs[jz] < opt->secs[iz]) {
          itmp = opt->secs[jz];
          opt->secs[jz] = opt->secs[iz];
          opt->secs[iz] = itmp;
        }
      }
    }

    /* Eliminate duplicates */
    jz = 0;
    for (iz = 0; iz < numTodo; iz++)
      if (!jz || opt->secs[jz - 1] != opt->secs[iz])
        opt->secs[jz++] = opt->secs[iz];
    numTodo = jz;

  } else

    /* Or else get the default list */
    set_input_options(opt, hin);
  opt->nofsecs = nz;
  if (opt->val != IP_DEFAULT)
    groupSize = B3DNINT(opt->val);
  if (opt->high != IP_DEFAULT)
    width = B3DNINT(opt->high);
  if (opt->low != IP_DEFAULT)
    userBase = opt->low;
  if (width < 2 || width > nx / 4 || width > ny / 4) {
    show_error("clip: width entry too small or too large.");
    return -1;
  }
  groupSize = B3DMAX(1, B3DMIN(nz, groupSize));
  numGroups = B3DMAX(1, numTodo / groupSize);

  hout->mode = hin->mode;
  if (opt->mode != IP_DEFAULT && opt->mode != hin->mode) {
    newMode = sliceModeIfReal(opt->mode);
    hout->mode = opt->mode;
    if (newMode < 0) {
      show_error("clip: Inappropriate new mode entry.");
      return(-1);
    }
  }

  hout->amean = 0.;
  hout->amin = 1.e37;
  hout->amax = -1.e37;
  if (mrcCopyExtraHeader(hin, hout))
    show_warning("clip warning: failed to copy extra header data");

  /* Get quadrant sample coordinates */
  vx3 = nx / 2 + 1;
  vx4 = vx3 + width;
  vx2 = vx3 - 2;
  vx1 = vx2 - width;
  hx1 = nx / 20;
  hx2 = nx / 2 - 10;
  hx3 = nx / 2 + 10;
  hx4 = 19 * nx / 20;
  hy3 = ny / 2 + 1;
  hy4 = hy3 + width;
  hy2 = hy3 - 2;
  hy1 = hy2 - width;
  vy1 = ny / 20;
  vy2 = ny / 2 - 10;
  vy3 = ny / 2 + 10;
  vy4 = 19 * ny / 20;

  /* Loop on the groups */
  indStart = 0;
  lastOut = -1;
  for (group = 0; group < numGroups; group++) {
    ninGroup = groupSize;
    if (group < numTodo % groupSize)
      ninGroup++;
    
    /* Read each slice for sampling */
    for (i = 0; i < 8; i++)
      d[i] = 0.;
    for (ind = indStart; ind < indStart + ninGroup; ind++) {
      iz = opt->secs[ind];
      slice = sliceReadMRC(hin, iz, 'z');
      if (!slice){
        show_error("clip: Error reading slice.");
        return(-1);
      }

      /* Get samples and accumulate means */
      if (quadrantSample(slice, hx3, hy3, hx4, hy4, &dt[0]) || 
          quadrantSample(slice, vx3, vy3, vx4, vy4, &dt[1]) || 
          quadrantSample(slice, vx1, vy3, vx2, vy4, &dt[3]) || 
          quadrantSample(slice, hx1, hy3, hx2, hy4, &dt[2]) || 
          quadrantSample(slice, hx1, hy1, hx2, hy2, &dt[4]) || 
          quadrantSample(slice, vx1, vy1, vx2, vy2, &dt[5]) || 
          quadrantSample(slice, vx3, vy1, vx4, vy2, &dt[7]) || 
          quadrantSample(slice, hx3, hy1, hx4, hy2, &dt[6]))
        return -1;
      for (i = 0; i < 8; i++)
        d[i] += dt[i];
      sliceFree(slice);
    }

    /* Determine min and max in case a base is needed for logs */
    qmin = 1.e30;
    qmax = -qmin;
    for (i = 0; i < 8; i++) {
      d[i] /= ninGroup;
      qmin = B3DMIN(qmin, d[i]);
      qmax = B3DMAX(qmax, d[i]);
    }

    /* Set or adjust base including the user value */
    base = (userBase ? 0.01 : 0.05) * (qmax - qmin) - qmin - userBase;
    if (base > 0)
      show_warning("clip - intensities being adjusted to avoid taking log of small "
                   "or negative values");
    base = B3DMAX(0., base) + userBase;

    /* printf("%.0f %.0f %.0f %.0f %.0f %.0f %.0f %.0f \n", d[1], d[3], d[2], d[4], d[5],
       d[7], d[6], d[0]); */

    /* Take logs and solve equations for log scaling */
    for (i = 0; i < 8; i++)
      dt[i] = log10(d[i] + base);
    /* printf("%f %f %f %f %f %f %f %f \n", dt[1], dt[3], dt[2], dt[4], dt[5], dt[7], 
       dt[6], dt[0]); */
    c2 = dt[0] - dt[6] + 2. * dt[1] - 2. * dt[3] + dt[4] - dt[2];
    c3 = dt[0] - dt[6] + dt[1] - dt[3] + dt[2] - dt[4] + dt[7] - dt[5];
    c4 = 2. * dt[0] - 2. * dt[6] + dt[1] - dt[3] + dt[5] - dt[7];
    denom = determ3(6., 2., 4., 2., 4., 2., 4., 2., 6.);
    g2 = determ3(c2, 2., 4., c3, 4., 2., c4, 2., 6.) / denom;
    g3 = determ3(6., c2, 4., 2., c3, 2., 4., c4, 6.) / denom;
    g4 = determ3(6., 2., c2, 2., 4., c3, 4., 2., c4) / denom;
    /* printf("c's: %f %f %f  denom %f lg's: %f %f %f\n", c2, c3, c4, denom, g2, 
       g3, g4); */
    g1 = pow(10., -(g2 + g3 + g4));
    g2 = pow(10., g2);
    g3 = pow(10., g3);
    g4 = pow(10., g4);
    if (ninGroup > 1)
      printf("Group from %d to %d:", opt->secs[indStart], iz);
    else
      printf("Section %d:", iz);
    printf(" scale factors %.4f %.4f %.4f %.4f\n", g1, g2, g3, g4);
    printf("Boundary diffs before: %6.1f %6.1f %6.1f %6.1f\n", d[0] - d[6], d[3] - d[1],
           d[4] - d[2], d[7] - d[5]);
    printf("Boundary diffs after: %6.1f %6.1f %6.1f %6.1f\n", 
           g1 * (d[0]+base) - g4 * (d[6]+base), g2 * (d[3]+base) - g1 * (d[1]+base),
           g3 * (d[4]+base) - g2 * (d[2]+base), g4 * (d[7]+base) - g3 * (d[5]+base));

    indEnd = iz;
    if (group == numGroups - 1)
      indEnd = nz - 1;

    /* Loop on all slices in group range and correct or just write */
    for (iz = lastOut + 1; iz <= indEnd; iz++) {
      slice = sliceReadMRC(hin, iz, 'z');
      if (!slice){
        show_error("clip: Error reading slice.");
        return(-1);
      }

      /* Convert if needed */
      if (newMode >= 0 && sliceNewMode(slice, newMode) < 0) {
        show_error("clip: Error converting slice to new mode.");
        return(-1);
      }

      /* Correct if section is in the list */
      for (ind = indStart; ind < indStart + ninGroup; ind++) {
        if (iz == opt->secs[ind]) {
          correctQuadrant(slice, nx / 2, ny / 2, nx, ny, g1, base);
          correctQuadrant(slice, 0, ny / 2, nx / 2, ny, g2, base);
          correctQuadrant(slice, 0, 0, nx / 2, ny / 2, g3, base);
          correctQuadrant(slice, nx / 2, 0, nx, ny / 2, g4, base);
          break;
        }
      }

      /* Maintain MMM and write */
      sliceMMM(slice);
      hout->amin = B3DMIN(hout->amin, slice->min);
      hout->amax = B3DMAX(hout->amax, slice->max);
      hout->amean += slice->mean / nz;
      if (mrc_write_slice((void *)slice->data.b, hout->fp, hout, iz, 'z')) {
        show_error("clip: Error writing slice.");
        return(-1);
      }
      sliceFree(slice);
      lastOut = iz;
    }
    indStart += ninGroup; 
  }
  mrc_head_label(hout, "clip: quadrant correction");
  if (mrc_head_write(hout->fp, hout))
    return -1;
  return 0;
}

static int quadrantSample(Islice *slice, int llx, int lly, int urx, int ury, double *mean)
{
  Islice *box = sliceBox(slice, llx, lly, urx, ury);
  if (!box) {
    show_error("clip: Error extracting subslice.");
    return(-1);
  }
  sliceMMM(box);
  *mean = box->mean;
  sliceFree(box);
  return 0;
}

static void correctQuadrant(Islice *slice, int llx, int lly, int urx, int ury, double g,
                             double base)
{
  Ival val;
  int ix, iy;
  for (iy = lly; iy < ury; iy++) {
    if (slice->mode == SLICE_MODE_FLOAT) {
      for (ix = llx; ix < urx; ix++) {
        sliceGetVal(slice, ix, iy, val);
        val[0] = (float)(g * (val[0] + base) - base);
        slicePutVal(slice, ix, iy, val);
      }
    } else {
      for (ix = llx; ix < urx; ix++) {
        sliceGetVal(slice, ix, iy, val);
        val[0] = (float)floor(g * (val[0] + base) - base + 0.5);
        slicePutVal(slice, ix, iy, val);
      }
    }
  }
}

/*
 * Find and correct the dark edges from drift correction
 */
int fillDriftCorrectedEdges(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt)
{
  int width = hin->nx < 3000 ? 30 : 60;
  int length = B3DMAX(1024, hin->nx / 4);
  float crit = 9.;
  Islice *s;
  int k, z;
  double wallStart,wallSum = 0.;
  CameraDefects defects;
  
  if (opt->low != IP_DEFAULT)
    length = B3DNINT(opt->low);
  if (opt->high != IP_DEFAULT)
    crit = opt->high;
  if (opt->val != IP_DEFAULT)
    width = B3DNINT(opt->val);

  if (hin->mode != MRC_MODE_SHORT && hin->mode != MRC_MODE_USHORT) {
    show_error("clip fill: only signed or unsigned short integer modes can be used");
    return -1;
  }

  z = set_options(opt, hin, hout);
  if (z < 0)
    return(z);
  defects.wasScaled = 0;
  defects.rotationFlip = 0;
  defects.K2Type = 0;
  
  for (k = 0; k < opt->nofsecs; k++) {
    s = sliceReadSubm(hin, opt->secs[k], 'z', opt->ix, opt->iy,
                          (int)opt->cx, (int)opt->cy);
    if (!s){
      show_error("clip: Error reading slice.");
      return(-1);
    }
    printf("\nSLICE %d\n", opt->secs[k]);
    wallStart = wallTime();

    // Find the edges and return into usable area, then correct them
    if (CorDefFindDriftCorrEdges(s->data.b, hin->mode, opt->ix, opt->iy, length, width,
                                 crit, defects.usableLeft, defects.usableRight, 
                                 defects.usableTop, defects.usableBottom)) {
      show_error("clip: error from CorDefFindDriftCorrEdges for slice %d.", k);
      return -1;
    }
    CorDefCorrectDefects(&defects, s->data.b, hin->mode, 1, 0, 0, hin->ny, hin->nx);

    wallSum += wallTime() - wallStart;
    if (clipWriteSlice(s, hout, opt, k, &z, 1))
      return -1;
  }
  //printf("processing time %.3f\n", wallSum);
  return set_mrc_coords(opt);
}

/*
 * Compute a scaled, reduced power spectrum
 */
int clipSpectrum(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt)
{
  int k, sliceMode, err, padSize, zOut;
  float xScale, yScale, zScale;
  Islice *slice, *s;
  int defaultSize = 1024;
  int bkgdGray = 48;
  float truncDiam = 0.02;
  int filtType = 3;   // Mitchell

  // Set the background gray level from -l entry or default
  if (opt->low != IP_DEFAULT) {
    if (opt->low > 0 && opt->low < 1.)
      bkgdGray = (int)(255. * opt->low);
    else
      bkgdGray = (int)opt->low;
    if (bkgdGray >= 192) {
      return 1;
    }
  }

  // Set truncation diameter from -h entry or default
  if (opt->high != IP_DEFAULT) {
    truncDiam = opt->high;
    if (truncDiam < 0 || truncDiam >= 0.75) {
      return 1;
    }
  }

  // Check and act on an output size entry
  if (opt->ox != IP_DEFAULT || opt->oy != IP_DEFAULT) {
    if (opt->ox != IP_DEFAULT && opt->oy != IP_DEFAULT && opt->ox != opt->oy) {
      show_error("clip spectrum: -ox and -oy cannot be entered with different sizes");
      return 1;
    }
    if (opt->ox != IP_DEFAULT)
      opt->oy = opt->ox;
    else
      opt->ox = opt->oy;
  } else {
    opt->ox = opt->oy = defaultSize;
  }
  if (opt->oz != IP_DEFAULT) {
    show_error("clip spectrum: -oz is not allowed");
    return 1;
  }

  // Returned mode of slice depends on if scaling is set with bkgdGray
  sliceMode = bkgdGray > 0 ? MRC_MODE_BYTE : MRC_MODE_SHORT;
  if (opt->mode == IP_DEFAULT)
    opt->mode = sliceMode;
  if (opt->add2file) {
    show_error("clip spectrum: Cannot append to existing file");
    return 1;
  }

  // Set up the input size and outptu size
  set_input_options(opt, hin);
  padSize = B3DMAX(opt->ix, opt->iy);
  padSize = niceFrame(padSize, 2, niceFFTlimit());

  // Cancel reduction if size is smaller than output or even a little bigger
  if (padSize < opt->ox * 1.02)
    opt->ox = opt->oy = padSize;
  
  //Set up output and file header
  zOut = set_output_options(opt, hout);
  if (zOut < 0)
    return 1;
  mrc_head_label_cp(hin, hout);
  mrc_head_label(hout, "clip: scaled power spectrum");

  // read, process, and write the slices
  printf("clip: Taking power spectrum of %d slices...\n", opt->nofsecs);
  for (k = 0; k < opt->nofsecs; k++) {
    s = sliceReadSubm(hin, opt->secs[k], 'z', opt->ix, opt->iy,
                          (int)opt->cx, (int)opt->cy);
    if (!s) {
      show_error("clip: Error reading slice %d.", opt->secs[k]);
      return(-1);
    }
  
    slice = sliceCreate(opt->ox, opt->oy, sliceMode);
    if (!slice) {
      show_error("clip: Error getting memory for spectrum slice.");
      return(-1);
    }

    err = spectrumScaled(s->data.b, s->mode, s->xsize, s->ysize, slice->data.b, padSize, 
                         opt->ox, bkgdGray, truncDiam, filtType, todfft);
    if (err) {
      show_error("clip: Error %d calling spectrumScaled", err);
      return 1;
    }

    if (clipWriteSlice(slice, hout, opt, k, &zOut, 1))
      return -1;
  }    

  // adjust the pixel size to be correct for the scaling
  if (padSize > opt->ox) {
    mrc_get_scale(hin, &xScale, &yScale, &zScale);
    xScale *= (float)padSize / opt->ox;
    yScale *= (float)padSize / opt->ox;
    zScale *= (float)padSize / opt->ox;
    mrc_set_scale(hout, xScale, yScale, zScale);
  }
  if (mrc_head_write(hout->fp, hout))
    return -1;
  return 0;
}

/*
 * 3-D color - conversion to shades of one color
 */
int clip_color(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt)
{
  int xysize, k, i;
  unsigned char **idata;
  float pixin;

  if (opt->red == IP_DEFAULT)
    opt->red = 1.0f;
  if (opt->green == IP_DEFAULT)
    opt->green = 1.0f;
  if (opt->blue  == IP_DEFAULT)
    opt->blue = 1.0f;

  if (opt->dim == 2)
    return(clip2d_color(hin,hout,opt));

  if ((opt->ix !=  IP_DEFAULT) || (opt->iy !=  IP_DEFAULT) || 
      (opt->iz !=  IP_DEFAULT) || (opt->ox !=  IP_DEFAULT) ||
      (opt->oy !=  IP_DEFAULT) || (opt->oz !=  IP_DEFAULT))
    show_warning("clip 3d color - input and output sizes ignored.");

  printf("clip: color (red, green, blue) = ( %g, %g, %g).\n",
         opt->red, opt->green, opt->blue);
     
  idata  = mrc_read_byte(hin->fp, hin, NULL, NULL);
  xysize = hin->nx * hin->ny;
  hout->nx = hin->nx;
  hout->ny = hin->ny;
  hout->nz = hin->nz;
  hout->mode =  MRC_MODE_RGB;
  if (mrc_head_write(hout->fp, hout))
    return -1;

  for (k = 0; k < hout->nz; k++){
    for (i = 0; i < xysize; i++){

      pixin = idata[k][i];
      writeBytePixel((float)(pixin * opt->red), hout);
      writeBytePixel((float)(pixin * opt->green), hout);
      writeBytePixel((float)(pixin * opt->blue), hout);
    }
  }
  return(0);
}

static void writeBytePixel(float pixel, MrcHeader *hout)
{
  unsigned char bdata;
  if (pixel > 255.0)
    pixel = 255.0;
  bdata = pixel + 0.5;
  if (hout->bytesSigned)
    bdata = (unsigned char)(((int)bdata - 128) & 255);
  b3dFwrite(&bdata,  sizeof(unsigned char), 1, hout->fp);
}

/*
 * 2D conversion to shades of a color
 */
int clip2d_color(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt)
{
  Islice *slice;
  int k, z, i, j;
  float pixin;
  Islice *s;
  Ival val;

  if ((opt->mode != MRC_MODE_RGB) && (opt->mode != IP_DEFAULT)){
    show_warning("clip - color output mode must be rgb.");
  }
  opt->mode = MRC_MODE_RGB;
  z = set_options(opt, hin, hout);
  if (z < 0)
    return(z);

  show_status("False Color...\n");
  mrc_head_label(hout, "CLIP Color");

  s = sliceCreate(opt->ix, opt->iy, MRC_MODE_RGB);
  if (!s) {
    show_error("clip - creating slice");
    return -1;
  }

  for (k = 0; k < opt->nofsecs; k++) {
    slice = sliceReadSubm(hin, opt->secs[k], 'z', opt->ix, opt->iy,
                          (int)opt->cx, (int)opt->cy);
    if (!slice){
      show_error("clip: Error reading slice.");
      return(-1);
    }

    for (j = 0; j < opt->iy; j++) {
      for (i = 0; i < opt->ix; i++) {
        sliceGetVal(slice, i, j, val);
        pixin = val[0];
        val[0] = B3DMAX(0., B3DMIN(255., pixin * opt->red + 0.5));
        val[1] = B3DMAX(0., B3DMIN(255., pixin * opt->green + 0.5));
        val[2] = B3DMAX(0., B3DMIN(255., pixin * opt->blue + 0.5));
        slicePutVal(s, i, j, val);
      }
    }

    if (clipWriteSlice(s, hout, opt, k, &z, 0))
      return -1;
    sliceFree(slice);
  }
  sliceFree(s);

  return set_mrc_coords(opt);  
}

/*
 * Join 3 separate byte files into an RGB file
 */
int clip_joinrgb(MrcHeader *h1, MrcHeader *h2,
		 MrcHeader *hout, ClipOptions *opt)
{
  int i,j,k,l, zstart;
  float xs, ys, zs;
  float val[5];
  MrcHeader hdr[3];
  Islice *s, *srgb[3];

  if (opt->red == IP_DEFAULT)
    opt->red = 1.0;
  if (opt->green == IP_DEFAULT)
    opt->green = 1.0;
  if (opt->blue == IP_DEFAULT)
    opt->blue = 1.0;

  if (opt->infiles != 3) {
    printf("ERROR: clip joinrgb - three input files must be specified\n");
    return (-1);
  }
  hdr[0] = *h1;
  hdr[1] = *h2;
  hdr[2].fp = iiFOpen(opt->fnames[2], "rb");

  if (!hdr[2].fp){
      printf("ERROR: clip joinrgb - opening %s.\n", opt->fnames[2]);
      return(-1);
  }
  if (mrc_head_read(hdr[2].fp, &hdr[2])){
    printf("ERROR: clip joinrgb - reading %s.\n", opt->fnames[2]);
    return(-1);
  }
  
  if ( (h1->nx != hdr[2].nx) || (h1->ny != hdr[2].ny) ||
       (h1->nz != hdr[2].nz) || (h1->nx != h2->nx) || (h1->ny != h2->ny) ||
       (h1->nz != h2->nz)) {
    printf("ERROR: clip joinrgb - all files must be same size.\n");
    return(-1);
  }

  if (h1->mode != MRC_MODE_BYTE || h2->mode != MRC_MODE_BYTE || 
      hdr[2].mode != MRC_MODE_BYTE) {
    printf("ERROR: clip joinrgb - all files must be bytes.\n");
    return(-1);
  }

  zstart = 0;
  if (opt->add2file) {
    if (opt->add2file == IP_APPEND_OVERWRITE) {
      printf("ERROR: clip joinrgb - Overwriting is not allowed, only appending\n");
      return(-1);
    }
    if (hout->mode != MRC_MODE_RGB) {
      printf("ERROR: clip joinrgb - Mode of file being appended to must be 16\n");
      return(-1);
    }
    if (hout->nx != h1->nx || hout->ny != h1->ny) {
      printf("ERROR: clip joinrgb - File being appended to is not same X/Y size as input"
             " files\n");
      return(-1);
    }

    /* Get starting Z value and maintain the scale */
    mrc_get_scale(hout, &xs, &ys, &zs);
    zstart = hout->nz;
    if (hout->mz == hout->nz)
      hout->mz += h1->nz;
    hout->nz += h1->nz;
    mrc_set_scale(hout, xs, ys, zs);

  } else {

    /* Set up mode and label */
    hout->mode = MRC_MODE_RGB;
    mrc_head_label(hout, "CLIP Join 3 files into RGB");
  }
  hout->amin = 0;
  hout->amax = 255;
  hout->amean = 128;
  if (mrc_head_write(hout->fp, hout))
    return -1;

  s = sliceCreate(h1->nx, h1->ny, MRC_MODE_RGB);
  for (i = 0; i < 3; i++) {
    srgb[i] = sliceCreate(h1->nx, h1->ny, MRC_MODE_BYTE);
    if (!s || !srgb[i]) {
      printf("ERROR: CLIP - getting memory for slices\n");
      return (-1);
    }
  }

  /* Loop on slices */
  for (k = 0; k < h1->nz; k++) {
    printf("\rJoining section %d of %d", k + 1, h1->nz);
    fflush(stdout);

    /* Read three slices */
    for (l = 0; l < 3; l++)
      if (mrc_read_slice((void *)srgb[l]->data.b, hdr[l].fp, &hdr[l], k, 'z'))
        return -1;

    /* Get the components, scale, and put into output slice */
    for (j = 0; j < h1->ny; j++) {
      for(i = 0; i < h1->nx; i ++) {
        for (l = 0; l < 3; l++)
          sliceGetVal(srgb[l], i, j, &val[l]);
        val[0] *= opt->red;
        val[1] *= opt->green;
        val[2] *= opt->blue;
        slicePutVal(s, i, j, val);
      }
    }
  
    if (mrc_write_slice((void *)s->data.b, hout->fp, hout, k + zstart, 'z'))
      return -1;
  }
  printf("\n");

  for (i = 0; i < 3; i++)
    sliceFree(srgb[i]);
  sliceFree(s);
  return(0);
}

/*
 * Split an RGB file into 3 separate files
 */
int clip_splitrgb(MrcHeader *h1, ClipOptions *opt)
{
  int i,j,k,l;
  float val[5];
  char *ext[3] = {".r", ".g", ".b"};
  MrcHeader hdr[3];
  char *fname;
  int len = strlen(opt->fnames[1]);
  Islice *s, *srgb[3];

  if (h1->mode != MRC_MODE_RGB) {
    printf("ERROR: clip splitrgb - mode is not RGB\n");
    return(-1);
  }
   
  if (opt->infiles != 1) {
    printf("ERROR: clip splitrgb - only one input file should "
            "be specified\n");
    return (-1);
  }
 
  opt->ocanresize = FALSE;
  set_input_options(opt, h1);

  fname = (char *)malloc(len + 4);
  if (!fname) {
    printf("ERROR: clip - getting memory for filename\n");
    return (-1);
  }

  /* Set up output files and get slices */
  s = sliceCreate(h1->nx, h1->ny, MRC_MODE_RGB);
  for (i = 0; i < 3; i++) {
    sprintf(fname, "%s%s", opt->fnames[1], ext[i]);
    if (!getenv("IMOD_NO_IMAGE_BACKUP"))
      imodBackupFile(fname);
    hdr[i] = *h1;
    hdr[i].nz = hdr[i].mz = opt->nofsecs;
    mrc_coord_cp(&hdr[i], h1);
    if (hdr[i].mz)
      hdr[i].zorg -= opt->secs[0] * hdr[i].zlen / hdr[i].mz;
      
    hdr[i].fp = iiFOpen(fname, "wb+");
    if (!hdr[i].fp){
      printf("ERROR: clip - opening %s\n", fname);
      return(-1);
    }
    hdr[i].mode = 0;
    mrcInitOutputHeader(&hdr[i]);
    hdr[i].amin = 255;
    hdr[i].amean = 0;
    hdr[i].amax = 0;
    mrc_head_label(&hdr[i], "CLIP Split RGB into 3 files");
    if (mrc_head_write(hdr[i].fp, &hdr[i]))
      return -1;
    srgb[i] = sliceCreate(h1->nx, h1->ny, MRC_MODE_BYTE);
    if (!s || !srgb[i]) {
      printf("ERROR: clip - getting memory for slices\n");
      return (-1);
    }
  }

  /* Loop on sections */
  for (k = 0; k < opt->nofsecs; k++) {
    printf("\rSplitting section %d of %d", k + 1, opt->nofsecs);
    fflush(stdout);
	  
    /* Read slice and put its 3 components into output slices */
    if (mrc_read_slice((void *)s->data.b, h1->fp, h1, opt->secs[k], 'z'))
      return -1;
    for (j = 0; j < h1->ny; j++) {
      for(i = 0; i < h1->nx; i ++) {
        sliceGetVal(s, i, j, val);
        for (l = 0; l < 3; l++)
          slicePutVal(srgb[l], i, j, &val[l]);
      }
    }

    /* Maintain min/max/mean */
    for (i = 0; i < 3; i++) {
      if (mrc_write_slice((void *)srgb[i]->data.b, hdr[i].fp, &hdr[i], k, 'z'))
        return -1;
      sliceMMM(srgb[i]);
      if (srgb[i]->min < hdr[i].amin)
        hdr[i].amin = srgb[i]->min;
      if (srgb[i]->max > hdr[i].amax)
        hdr[i].amax = srgb[i]->max;
      hdr[i].amean += srgb[i]->mean;
    }
  }

  printf("\n");

  for (i = 0; i < 3; i++) {
    hdr[i].amean /= k;
    if (mrc_head_write(hdr[i].fp, &hdr[i]))
      return -1;
    sliceFree(srgb[i]);
    iiFClose(hdr[i].fp);
  }
  sliceFree(s);
  return(0);
}

/*
 * 3D additive operations on multiple files
 */
int clip_average(MrcHeader *h1, MrcHeader *h2, MrcHeader *hout, ClipOptions *opt)
{
  Islice *s, *so, *sso;
  MrcHeader **hdr;
  char *message;
  int i, j, k, z, l, f, variance = 0;
  double valscale = 1., varscale = 1.;
  float factor = 1.;
  Ival val, oval;

  if (opt->infiles == 1 && (opt->process == IP_AVERAGE || opt->process == IP_VARIANCE || 
                            opt->process == IP_STANDEV))
    return(clip2d_average(h1,hout,opt));

  if (opt->add2file != IP_APPEND_FALSE) {
    show_error("clip volume combining: you cannot add to an existing output file");
    return -1;
  }

  if (opt->process == IP_SUBTRACT && opt->infiles != 2 || opt->infiles < 2) {
    show_error("clip %s two input files.", opt->process == IP_SUBTRACT ?
            "subtract: needs exactly" : "add: needs at least");
    return(-1);
  }
  z = set_options(opt, h1, hout);
  if (z < 0)
    return(z);

  if (opt->low != IP_DEFAULT) {
    valscale = opt->low;
    varscale = opt->low * opt->low;
  }
  
  switch (opt->process) {
  case IP_AVERAGE:
    valscale /= opt->infiles;
    message = "Averaging";
    mrc_head_label(hout, "clip: 3D Averaged");
    break;    
  case IP_ADD:
    message = "Adding";
    mrc_head_label(hout, "clip: Summed");
    break;
  case IP_VARIANCE:
    variance = 1;
    valscale /= opt->infiles;
    message = "Averaging";
    mrc_head_label(hout, "clip: 3D Variance");
    break;
  case IP_STANDEV:
    variance = 2;
    valscale /= opt->infiles;
    message = "Averaging";
    mrc_head_label(hout, "clip: 3D Standard Deviation");
    break;
  case IP_SUBTRACT:
    message = "Subtracting";
    valscale = 1.;
    mrc_head_label(hout, "clip: Subtract");
    break;
  default:
    return -1;
  }

  if (variance) {
    if (h1->mode == MRC_MODE_COMPLEX_FLOAT) {
      sso = sliceCreate(opt->ix, opt->iy, SLICE_MODE_COMPLEX_FLOAT);
    } else if (h1->mode == MRC_MODE_RGB){
      sso = sliceCreate(opt->ix, opt->iy, SLICE_MODE_MAX);
    } else {
      sso = sliceCreate(opt->ix, opt->iy, SLICE_MODE_FLOAT);
    }
  }
  hdr = (MrcHeader **)malloc(opt->infiles * sizeof(MrcHeader *));
  if (!hdr || (variance && !sso))
    return(-1);
     
  for (f = 0; f < opt->infiles; f++){
    hdr[f] = (MrcHeader *)malloc(sizeof(MrcHeader));
    if (!hdr[f])
      return(-1);
    hdr[f]->fp = iiFOpen(opt->fnames[f], "rb");
    if (!hdr[f]->fp){
      show_error("clip volume combining: error opening %s.", opt->fnames[f]);
      return(-1);
    }
    if (mrc_head_read(hdr[f]->fp, hdr[f])){
      show_error("clip volume combining: error reading header of %s.", opt->fnames[f]);
      return(-1);
    }
    if ( (h1->nx != hdr[f]->nx) || (h1->ny != hdr[f]->ny) ||
         (h1->nz != hdr[f]->nz) || (h1->mode != hdr[f]->mode )) {
      show_error("clip volume combining: all files must be the same size and mode.");
      return(-1);
    }
  }

  for (k = 0; k < opt->nofsecs; k++) {
    printf("\rclip: %s slice %d of %d\n", message, k + 1, opt->nofsecs);
    fflush(stdout);

    if (h1->mode == MRC_MODE_COMPLEX_FLOAT){
      so = sliceCreate(opt->ix, opt->iy, SLICE_MODE_COMPLEX_FLOAT);
    } else if (h1->mode == MRC_MODE_RGB){
      so = sliceCreate(opt->ix, opt->iy, SLICE_MODE_MAX);
    } else {
      so = sliceCreate(opt->ix, opt->iy, SLICE_MODE_FLOAT);
    }
    if (!so)
      return -1;

    /* Zero the slice(s) */
    factor = 1.;
    val[0] = val[1] = val[2] = 0.;
    for (j = 0; j < opt->iy; j++)
      for(i = 0; i < opt->ix; i ++){
        slicePutVal(so, i, j, val);
        if (variance)
          slicePutVal(sso, i, j, val);
      }

    /* Accumulate sums and sum of squares */
    for (f = 0; f < opt->infiles; f++){
      s = sliceReadSubm( hdr[f], opt->secs[k], 'z', opt->ix, opt->iy,
                        (int)opt->cx, (int)opt->cy);
      if (!s)
        return -1;
      for (j = 0; j < opt->iy; j++)
        for(i = 0; i < opt->ix; i ++){
          sliceGetVal(s, i, j,   val);
          sliceGetVal(so, i, j, oval);
          oval[0] += factor * val[0];
          oval[1] += factor * val[1];
          oval[2] += factor * val[2];
          slicePutVal(so, i, j, oval);
          if (variance) {
            sliceGetVal(sso, i, j, oval);
            oval[0] += val[0] * val[0];
            oval[1] += val[1] * val[1];
            oval[1] += val[2] * val[2];
            slicePutVal(sso, i, j, oval);
          }
        }
      sliceFree(s);
      if (opt->process == IP_SUBTRACT)
        factor = -1.;
    }

    /* Compute mean and variance or SD */
    if (valscale != 1.)
      mrc_slice_valscale(so, valscale);
    if (variance) {
      for (j = 0; j < opt->iy; j++)
        for(i = 0; i < opt->ix; i ++){
          sliceGetVal(sso, i, j,   val);
          sliceGetVal(so, i, j, oval);
          for (l = 0; l < 3; l++) {
            oval[l] = (val[l] * varscale - f * oval[l] * oval[l]) / (f - 1.);
            oval[l] = B3DMAX(0., oval[l]);
            if (variance > 1)
              oval[l] = (float)sqrt(oval[l]);
          }
          slicePutVal(so, i, j, oval);
        }
    }

    if (clipWriteSlice(so, hout, opt, k, &z, 1))
      return -1;
  }
  printf("\n");
  hout->amean /= k;
  if (mrc_head_write(hout->fp, hout))
    return -1;
  if (variance)
    sliceFree(sso);
  for (f = 0; f < opt->infiles; f++)
    free(hdr[f]);
  free(hdr);
  return set_mrc_coords(opt);
}

/*
 * 2D averaging of input slices into one slice 
 */
int clip2d_average(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt)
{
  Islice *slice;
  Islice *avgs, *cnts, *ssq;
  Ival val, aval;
  int k, z, i, j, l, variance = 0, avgMode;
  int thresh = FALSE;
  float dval,scale,dscale, sx, sy, sz;
     
  if (opt->ox != IP_DEFAULT || opt->oy != IP_DEFAULT || 
      opt->oz != IP_DEFAULT)
    show_warning("clip - ox, oy, oz have no effect for 2d average.");
    
  opt->ox = opt->oy = opt->oz = IP_DEFAULT;
  set_input_options(opt, hin);
  opt->oz = 1;
  z = set_output_options(opt, hout);
  if (z < 0)
    return(z);

  mrc_head_label_cp(hin, hout);
  switch (opt->process) {
  case IP_AVERAGE:
    mrc_head_label(hout, "clip: 2D Average");
    break;    
    break;
  case IP_VARIANCE:
    variance = 1;
    mrc_head_label(hout, "clip: 2D Variance");
    break;
  case IP_STANDEV:
    variance = 2;
    mrc_head_label(hout, "clip: 2D Standard Deviation");
    break;
  default:
    return -1;
  }
  if (opt->val != IP_DEFAULT)
    thresh = TRUE;

  show_status("2D Averaging...\n");
  avgMode = hin->mode == MRC_MODE_RGB ? SLICE_MODE_MAX : SLICE_MODE_FLOAT;
  avgs = sliceCreate(opt->ix, opt->iy, avgMode);
  cnts = sliceCreate(opt->ix, opt->iy, SLICE_MODE_FLOAT);
  if (variance)
    ssq = sliceCreate(opt->ix, opt->iy, avgMode);
  if (!avgs || !cnts || (variance && !ssq))
    return -1;

  /* Initialize sums */
  aval[0] = aval[1] = aval[2] = 0.0f;
  for(j = 0; j < avgs->ysize; j++)
    for(i = 0; i < avgs->xsize; i++){
      slicePutVal(avgs, i, j, aval);
      cnts->data.f[i + (j * cnts->xsize)] = 0.0f;
      if (variance)
        slicePutVal(ssq, i, j, aval);
    }

  for (k = 0; k < opt->nofsecs; k++) {
    slice = sliceReadSubm(hin, opt->secs[k], 'z', opt->ix, opt->iy,
                          (int)opt->cx, (int)opt->cy);
    if (!slice){
      show_error("clip: Error reading slice.");
      return(-1);
    }

    /* Add each pixel into average */
    for (j = 0; j < slice->ysize; j++) {
      for (i = 0; i < slice->xsize; i++) {

        /* If thresholding, only add to sums if magnitude is greater than threshold. */
        if (thresh) {
          dval = sliceGetPixelMagnitude(slice, i, j);
          if (dval <= opt->val)
            continue;
        }
        sliceGetVal(slice, i, j,  val);
        sliceGetVal(avgs,  i, j, aval);
        aval[0] += val[0];
        aval[1] += val[1];
        aval[2] += val[2];

        /* Add up the number of items contributing to the average */
        cnts->data.f[i + (j * cnts->xsize)] += 1.0f;
        slicePutVal(avgs,  i, j, aval);
        if (variance) {
          sliceGetVal(ssq,  i, j, aval);
          aval[0] += val[0] * val[0];
          aval[1] += val[1] * val[1];
          aval[2] += val[2] * val[2];
          slicePutVal(ssq,  i, j, aval);
        }
      }
    }
    sliceFree(slice);
  }

  /* Set scaling based on -l input (no longer special for RGB) */
  scale = 1.0;
  if (opt->low != IP_DEFAULT)
    scale = opt->low;

  /* create the output slice by dividing each value by counts */
  for (j = 0; j < avgs->ysize; j++) {
    for (i = 0; i < avgs->xsize; i++) {
      sliceGetVal(avgs, i, j, aval);
      dval = cnts->data.f[i + (j * cnts->xsize)];
      if (dval){
        dscale = scale/dval;
        aval[0] *= dscale;
        aval[1] *= dscale;
        aval[2] *= dscale;
        if (variance) {
          if (dval > 1.5) {
            sliceGetVal(ssq, i, j, val);
            for (l = 0; l < 3; l++) {
              aval[l] = (val[l] * scale * scale - dval * aval[l] * aval[l]) / (dval-1.);
              aval[l] = B3DMAX(0., aval[l]);
              if (variance > 1)
                aval[l] = (float)sqrt(aval[l]);
            }
          } else {
            aval[0] = aval[1] = aval[2] = 0.;
          }
        }
      } else {
        aval[0] = aval[1] = aval[2] = 0.;
      }
      slicePutVal(avgs, i, j, aval);
    }
  }

  /* Take care of min/max/mean and write the slice */
  if (avgs->mode != hout->mode && sliceNewMode(avgs, hout->mode) < 0)
    return -1;
  sliceMMM(avgs);
  hout->amin = B3DMIN(avgs->min, hout->amin);
  hout->amax = B3DMAX(avgs->max, hout->amax);
  if (opt->add2file != IP_APPEND_OVERWRITE)
    hout->amean += avgs->mean / hout->nz;
  mrc_get_scale(hin, &sx, &sy, &sz);
  mrc_set_scale(hout, sx, sy, sz);
  if (mrc_write_slice((void *)avgs->data.b, hout->fp, hout, z, 'z'))
    return -1;
  if (mrc_head_write(hout->fp, hout))
    return -1;
  sliceFree(avgs);
  sliceFree(cnts);
  if (variance)
    sliceFree(ssq);
  return(0);
}

/*
 * 3D multiplying or dividing
 */
int clip_multdiv(MrcHeader *h1, MrcHeader *h2, MrcHeader *hout,
                 ClipOptions *opt)
{
  Islice *s, *so;
  char *message, *titleProc;
  int i, j, k, z, dsize, csize1, csize2, divByZero = 0, readOnce, doRound;
  Ival val, oval;
  float tmp, denom, scaledVal, scale = 1.;
  char title[54];

  if (opt->infiles != 2) {
    show_error("clip multiply/divide: Need exactly two input files.");
    return(-1);
  }

  z = set_options(opt, h1, hout);
  if (z < 0)
    return(z);

  if (opt->add2file != IP_APPEND_FALSE) {
    show_error("clip multiply/divide: you cannot add to an existing output file");
    return -1;
  }

  mrc_getdcsize(h1->mode, &dsize, &csize1);
  mrc_getdcsize(h2->mode, &dsize, &csize2);
  if (!(csize2 == 1 || (h1->mode == MRC_MODE_COMPLEX_FLOAT && 
                        h2->mode == MRC_MODE_COMPLEX_FLOAT) || 
        hout->mode == MRC_MODE_FLOAT)) {
    show_error("clip multiply/divide: second file must have single-channel data "
            "unless both are FFTs or output mode is float");
    return(-1);
  }
  
  if ((h1->nx != h2->nx) || (h1->ny != h2->ny)) {
    show_error("clip  multiply/divide: X and Y sizes must be equal");
    return(-1);
  }
  if (h1->nz != h2->nz && h2->nz > 1) {
    show_error("clip  multiply/divide: Z sizes must be the same, or equal to 1 "
            "for second file");
    return(-1);
  }

  if (opt->val != IP_DEFAULT)
    scale = opt->val;

  switch (opt->process) {
  case IP_MULTIPLY:
    message = "Multiplying";
    titleProc = "Multiply";
    break;
  case IP_DIVIDE:
    message = "Dividing";
    titleProc = "Divide";
    break;
  default:
    return -1;
  }
  if (opt->val != IP_DEFAULT)
    sprintf(title, "clip: %s, scaled by %.2f", titleProc, scale);
  else 
    sprintf(title, "clip: %s", titleProc);
  mrc_head_label(hout, title);

  /* Do rounding if there is a single channel in and out and the output mode is not float
     and either we are doing division or one of the input modes is float */
  doRound = csize1 * csize2 == 1 &&  hout->mode != MRC_MODE_FLOAT && 
    (h1->mode == MRC_MODE_FLOAT || h2->mode == MRC_MODE_FLOAT || 
     opt->process == IP_DIVIDE);
  readOnce = (h2->nz == 1 && h1->nz > 1) ? 1 : 0;
  for (k = 0; k < opt->nofsecs; k++) {
    printf("\rclip: %s slice %d of %d", message, k + 1, opt->nofsecs);
    fflush(stdout);
    so = sliceReadSubm(h1, opt->secs[k], 'z', opt->ix, opt->iy, (int)opt->cx,
                       (int)opt->cy);
    if (!so)
      return -1;
    if (hout->mode != so->mode && sliceNewMode(so, hout->mode) < 0) {
      show_error("CLIP - getting memory for slice array");
      return -1;
    }

    if (readOnce >= 0) {
      s = sliceReadSubm(h2, readOnce ? 0 : opt->secs[k], 'z', opt->ix, opt->iy, 
                        (int)opt->cx, (int)opt->cy);
      if (!s)
        return -1;
      readOnce = -readOnce;
      if (csize2 > 1 && hout->mode == MRC_MODE_FLOAT && sliceFloat(s)) {
        printf("ERROR: CLIP - getting memory for slice array\n");
        return -1;
      }
    }
    for (j = 0; j < opt->iy; j++) {
      for (i = 0; i < opt->ix; i ++) {
        sliceGetVal(s, i, j,   val);
        sliceGetVal(so, i, j, oval);
        if (doRound) {

          /* Single channel with rounding because of integer output from float */
          if (opt->process == IP_MULTIPLY)
            oval[0] = B3DNINT(oval[0] * val[0] * scale);
          else if (val[0] != 0)
            oval[0] = B3DNINT(oval[0] * (scale / val[0]));
          else {
            divByZero++;
            oval[0] = 0.;
          }

        } else if (csize2 == 1 || hout->mode == MRC_MODE_FLOAT) {

          /* Ordinary mult or div, possible multi-channel */
          if (opt->process == IP_MULTIPLY) {
            scaledVal = val[0] * scale;
            oval[0] *= scaledVal;
            oval[1] *= scaledVal;
            oval[2] *= scaledVal;
          } else {
            if (val[0] != 0) {
              scaledVal = scale / val[0];
              oval[0] *= scaledVal;
              oval[1] *= scaledVal;
              oval[2] *= scaledVal;
            } else {
              divByZero++;
              oval[0] = oval[1] = oval[2] = 0.;
            }
          }
        } else {
         
          /* Complex mult/div */
          if (opt->process == IP_MULTIPLY) {
            tmp = (oval[0] * val[0] - oval[1] * val[1]) * scale;
            oval[1] = (oval[0] * val[1] + val[0] * oval[1]) * scale;
            oval[0] = tmp;
          } else {
            denom = val[0] * val[0] + val[1] * val[1];
            if (denom != 0) {
              tmp = (oval[0] * val[0] + oval[1] * val[1]) * scale / denom;
              oval[1] = (oval[1] * val[0] - oval[0] * val[1]) * scale / denom;
              oval[0] = tmp;
            } else {
              divByZero++;
              oval[0] = oval[1] = oval[2] = 0.;
            }
          }
        }
        slicePutVal(so, i, j, oval);
      }
    }
    
    if (opt->readDefects && correctDefects(so, h2->nx, h2->ny, opt))
      return -1;
    
    if (clipWriteSlice(so, hout, opt, k, &z, 1))
      return -1;

    // Free slice only on last slice if reading once
    if (readOnce == 0 || k == opt->nofsecs - 1)
      sliceFree(s);
  }
  printf("\n");
  if (divByZero)
    printf("WARNING: Division by zero occurred %d times\n", divByZero);
  if (mrc_head_write(hout->fp, hout))
    return -1;
  return set_mrc_coords(opt);
}

/*
 * Unpacking 4-bit data with scaling and truncation -> Now normalizing
 */
int clipUnpack(MrcHeader *hin1, MrcHeader *hin2, MrcHeader *hout, ClipOptions *opt)
{
  Islice *slRef, *slOut, *slIn;
  ImodImageFile *iiFile, *iiFile2;
  float *refTemp;
  int z, i, j, k, bval, base, numThreads, nxGain, nyGain, llx, urx, lly, ury, superFac;
  int numInX, xStart, xInterval, numInY, yStart, yInterval;
  std::vector<std::vector<float> > biases;
  char *extraName;
  float offset, scaleUse;
  float val[3] = {0., 0., 0.};
  float ival[3] = {0., 0., 0.};
  int doRef = opt->infiles == 2 ? TRUE : FALSE;
  float scale = doRef ? 16. : 1.;
  float truncThresh, unscaledThresh = 1.e30;
  char title[54];

  if (opt->add2file != IP_APPEND_FALSE) {
    show_error("clip unpack/normalize - you cannot add to an existing output file");
    return -1;
  }

  if (doRef && hin2->mode != MRC_MODE_FLOAT) {
    show_error("clip unpack/normalize - mode of second input file must be floats");
    return -1;
  }
  if (opt->infiles > 2) {
    show_error("clip unpack/normalize - There can be only 2 input files");
    return -1;
  }
  z = set_options(opt, hin1, hout);
  if (z < 0)
    return z;
  
  // Read the reference if any and scale it by the factor
  if (opt->val != IP_DEFAULT)
    scale = opt->val;
  if (doRef) {

    // Get the full slice in case it needs rotate/flip
    slRef = sliceReadMRC(hin2, 0, 'z');
    if (!slRef)
      return -1;
    nxGain = hin2->nx;
    nyGain = hin2->ny;
    if (opt->rotationFlip < 0) {

      // Try to find r/f in the header title
      for (i = 0; i < hin1->nlabl; i++) {
        extraName = strstr(hin1->labels[i], " r/f ");
        if (extraName) {
          opt->rotationFlip = atoi(extraName + 4);
          break;
        }
      }
      if (opt->rotationFlip < 0) {
        show_error("Cannot find r/f entry in header of first input file");
        return -1;
      }
    }
    if (opt->rotationFlip > 7) {
      show_error("Rotation/flip value of %d is out of range", opt->rotationFlip);
      return -1;
    }

    // Do the operation
    if (opt->rotationFlip > 0) {
      if (rotateFlipGainReference(slRef->data.f, nxGain, nyGain, opt->rotationFlip)) {
        show_error("Error allocating memory for rotating gain reference");
        return -1;
      }
      slRef->xsize = nxGain;
      slRef->ysize = nyGain;
    }

    // Now expand the gain reference by 2 or 4 if that is appropriate
    superFac = hin1->nx / hin2->nx;
    i = hin1->ny / hin2->ny;
    if (!(i != superFac || superFac * hin2->nx != hin1->nx || superFac * hin2->ny != 
          hin1->ny || (superFac != 1 && superFac != 2 && superFac != 4))) {
      if (superFac > 1) {
        iiFile = iiLookupFileFromFP(hin1->fp);
        iiFile2 = iiLookupFileFromFP(hin2->fp);
        if (!(iiFile && iiFile->file == IIFILE_TIFF && iiFile->numFramesInEERfile > 0) &&
            !(iiFile2 && iiFile2->file == IIFILE_TIFF))
          printf("WARNING: clip - Expanding gain reference because image is exactly %d "
                 "times as big as reference\n", superFac);
        refTemp = B3DMALLOC(float, hin1->nx * hin1->ny);
        if (!refTemp) {
          show_error("Error allocating memory for expanded gain reference");
          return -1;
        }
        CorDefExpandGainReference(slRef->data.f, hin2->nx, hin2->ny, superFac, refTemp);
        nxGain *= superFac;
        nyGain *= superFac;
        free(slRef->data.f);
        slRef->data.f = refTemp;
        slRef->xsize = nxGain;
        slRef->ysize = nyGain;

        if (opt->superGainName) {
          llx = CorDefReadSuperGain(opt->superGainName, superFac, biases, numInX, 
                                    xStart, xInterval, numInY, yStart, yInterval);
          if (llx)
            exitError("Reading file with super-resolution gain adjustments (error %d)",
                      llx);
          CorDefRefineSuperResRef(refTemp, nxGain, nyGain, superFac, biases, numInX,
                                  xStart, xInterval, numInY, yStart, yInterval);
        }
      }
    }

    // Now make sure the size matches
    if (hin1->nx != nxGain || hin1->ny != nyGain) {
      show_error("clip unpack/normalize - reference size must match the input file size");
      return -1;
    }

    // Get the coordinates as in sliceReadSubm
    llx = (int)opt->cx - opt->ix / 2;
    lly = (int)opt->cy - opt->iy / 2;
    urx = llx + opt->ix;
    ury = lly + opt->iy;
    if (llx < 0 || lly < 0 || urx > nxGain || ury > nyGain) {
      show_error("Selected area goes outside of actual data for gain reference");
      return -1;
    }

    // Box it out if necessary
    if (llx > 0 || lly > 0 || urx < nxGain || ury < nyGain) {
      if (sliceBoxIn(slRef, llx, lly, urx, ury)) {
        show_error("Error allocating memory for taking subarea of gain reference");
        return -1;
      }
    }

    // Scale it
    for (i = 0; i < opt->ix * opt->iy; i++)
      slRef->data.f[i] *= scale;
  }

  // Create output slice
  offset = hout->mode == MRC_MODE_FLOAT ? 0. : 0.5f;
  slOut = sliceCreate(opt->ix, opt->iy, hout->mode);
  if (!slOut) {
    show_error("CLIP - Memory error");
    return -1;
  }
  
  // set up truncation if high limit entered
  //double wallCum = 0., wallStart, readCum = 0.;
  if (opt->high != IP_DEFAULT) 
    unscaledThresh = opt->high;
  truncThresh = unscaledThresh * scale;
  sprintf(title, opt->process == IP_UNPACK ? "clip: Unpack 4-bit values, scaled by %.2f" :
          "clip: Normalize, scaled by %.2f", scale);
  mrc_head_label(hout, title);

  // Loop on sections
  for (k = 0; k < opt->nofsecs; k++) {
    printf("\rclip: processing slice %d of %d", k + 1, opt->nofsecs);
    fflush(stdout);
    //wallStart = wallTime();
    slIn = sliceReadSubm(hin1, opt->secs[k], 'z', opt->ix, opt->iy, (int)opt->cx, 
                         (int)opt->cy);
    if (!slIn)
      return -1;
    //readCum += wallTime() - wallStart;

    // The logic here is 12 for K2 super-res, 8 for K2 counting, 4 for 2K, 1 for 1K
    numThreads = B3DNINT(4. * scale * log(sqrt((double)opt->ix * opt->iy) / 944.) /
                         log(2.));
    numThreads = numOMPthreads(numThreads);

    //wallStart = wallTime();
#pragma omp parallel for num_threads(numThreads)                        \
  shared(opt, hin1, slIn, doRef, slRef, scale, offset, truncThresh, unscaledThresh, \
         slOut)                                                         \
  private(j, base, i, bval, scaleUse, val, ival)
    for (j = 0; j < opt->iy; j++) {
      
      // get pixel, scale, then truncate; first bytes
      base = j * opt->ix;
      if (hin1->mode == MRC_MODE_BYTE) {
        for (i = 0; i < opt->ix; i++) {
          bval = slIn->data.b[i + base];
          scaleUse = doRef ? slRef->data.f[i + base] : scale;
          val[0] = bval * scaleUse + offset;
          if (val[0] > truncThresh) {

            // truncate to the surrounding mean, or to low value if that was entered 
            if (opt->low == IP_DEFAULT)
              val[0] = CorDefSurroundingMean(slIn->data.b, MRC_MODE_BYTE, slIn->xsize,
                                             slIn->ysize, unscaledThresh, i, j) * 
                scaleUse + offset;
            else
              val[0] = opt->low * scaleUse + offset;
          }
          slicePutVal(slOut, i, j, val);
        }
      } else {

        // Or other modes, a bit slower
        for (i = 0; i < opt->ix; i++) {
          sliceGetVal(slIn, i, j, ival);
          scaleUse = doRef ? slRef->data.f[i + base] : scale;
          val[0] = ival[0] * scaleUse + offset;
          if (val[0] > truncThresh) {
            if (opt->low == IP_DEFAULT)
              val[0] = CorDefSurroundingMean(slIn->data.b, slIn->mode, slIn->xsize,
                                             slIn->ysize, unscaledThresh, i, j) * 
                scaleUse + offset;
            else
              val[0] = opt->low * scaleUse + offset;
          }
          slicePutVal(slOut, i, j, val);
        }
      }
    }

    //wallCum += wallTime() - wallStart;

    if (opt->readDefects && correctDefects(slOut, hin1->nx, hin1->ny, opt))
      return -1;

    if (clipWriteSlice(slOut, hout, opt, k, &z, 0))
      return -1;
  }
  sliceFree(slIn);
  if (doRef)
    sliceFree(slRef);
  printf("\n");

  //printf("Cumulative read & proc time %.3f  %.3f\n", readCum, wallCum);
  return set_mrc_coords(opt);
}

// Copied almost verbatim from alignframes
static int rotateFlipGainReference(float *ref, int &nxGain, int &nyGain, int rotationFlip)
{
  double angle;
  float rotMat[2][2];
  int err, nxIn = nxGain, nyIn = nyGain;
  float *summed;
  
  summed = B3DMALLOC(float, nxGain * nyGain);
  if (!summed)
    return 1;

  err = rotateFlipImage(ref, MRC_MODE_FLOAT, nxIn, nyIn, rotationFlip, 0, 0, 0, summed,
                        &nxGain, &nyGain, 0);
  if (!err)
    memcpy(ref, summed, nxGain * nyGain * sizeof(float));
  free(summed);
  return err;
}

/*
 * Make a map of all pixels in defects
 */
int clipDefectMap(MrcHeader *hin, MrcHeader *hout, ClipOptions *opt)
{
  int ind, tolerance = 10;
  float sx, sy, sz;
  unsigned char *map;
  if (!opt->readDefects) {
    show_error("clip defectmap - you must provide a defect file with the -D option");
    return -1;
  }
  
  // Handle scaling for K2 images if within tolerance
  if (opt->defects.K2Type > 0) {
    if (opt->defects.wasScaled > 0 && B3DABS(opt->camSizeX / 2 - hin->nx) < tolerance &&
        B3DABS(opt->camSizeY / 2 - hin->ny) < tolerance) {
      CorDefScaleDefectsForK2(&opt->defects, true);
      opt->camSizeX /= 2;
      opt->camSizeY /= 2;
    } else if (opt->defects.wasScaled <= 0 && B3DABS(opt->camSizeX * 2 - hin->nx) < 
               tolerance && B3DABS(opt->camSizeY * 2 - hin->ny) < tolerance) {
      CorDefScaleDefectsForK2(&opt->defects, false);
      opt->camSizeX *= 2;
      opt->camSizeY *= 2;
    }
  }

  if (opt->defects.FalconType > 0) {
    ind = B3DNINT((float)hin->nx / opt->camSizeX);
    if ((ind == 2 || ind == 4) && B3DABS(opt->camSizeX * ind - hin->nx) < tolerance &&
        B3DABS(opt->camSizeY * ind - hin->ny) < tolerance) {
      CorDefScaleDefectsForFalcon(&opt->defects, ind);
      opt->camSizeX *= ind;
      opt->camSizeY *= ind;
    }
  }


  // Now test for match in size
  if (B3DABS(opt->camSizeX - hin->nx) >= tolerance ||
      B3DABS(opt->camSizeY - hin->ny) >= tolerance) {
    show_error("clip defectmap - Image size must be within %d pixels of the camera size "
               "stored in the defect list, after possible scaling up or down by 2 for "
               "K2", tolerance);
    return -1;
  }
  
  // Set up array and fill it
  hout->mode = MRC_MODE_BYTE;
  if (b3dOutputFileType() == OUTPUT_TYPE_MRC)
    hout->bytesSigned = 0;
  map = (unsigned char *)malloc(hin->nx * hin->ny);
  if (!map) {
    show_error("CLIP - Memory error");
    return -1;
  }
  if (CorDefFillDefectArray(&opt->defects, opt->camSizeX, opt->camSizeY, map, hin->nx,
                            hin->ny, opt->sano)) {
    show_error("clip defectmap - Bad size parameters passed to routine");
    return -1;
  }

  // Set up header and get a quick mean, then write the slice
  mrc_head_label_cp(hin, hout);
  mrc_head_label(hout, "clip defectmap: Map of defective pixels in image");
  hout->amin = 0.;
  hout->amax = 0.;
  hout->amean = 0.;
  for (ind = 0; ind < hin->nx * hin->ny; ind++) {
    hout->amean += map[ind];
    ACCUM_MAX(hout->amax, map[ind]);
  }
  hout->amean /= hin->nx * hin->ny;
  hout->nz = 1;
  mrc_get_scale(hin, &sx, &sy, &sz);
  mrc_set_scale(hout, sx, sy, sz);
  if (mrc_head_write(hout->fp, hout))
    return -1;
  if (mrc_write_slice(map, hout->fp, hout, 0, 'z'))
    return -1;
  return 0;
}

/*
 * Compute gain adjustments for EER file images by summing as many aimges as are provided
 */
int clipSuperGain(MrcHeader *h1, FILE *outFP, ClipOptions *opt)
{
  ImodImageFile *iiFile;
  MrcHeader **hdr;
  unsigned char *inputArr;
  float *imageArr;
  std::vector<double *> biases;
  double *bias;
  double areaSum[16], bsum;
  int div, numDiv, xdiv, ydiv, divInd, xSpacing, ySpacing, i, j, ind, f, iz;
  int physX, physY, xOffset, yOffset;

  if (opt->val < 2 || opt->val > 64) {
    show_error("clip supergain: the number of subdivisions must be between 2 and 64.");
    return(-1);
  }
  numDiv = 2 * opt->val;
  for (div = 0; div < numDiv * numDiv; div++) {
    bias = B3DMALLOC(double, 16);
    if (!bias)
      exitError("Allocating small array for biases");
    biases.push_back(bias);
  }

  hdr = (MrcHeader **)malloc(opt->infiles * sizeof(MrcHeader *));
  if (!hdr)
    exitError("Allocating array for headers");
  hdr[0] = h1;

  // Open any additional files, check size and mode and that it is EER
  for (f = 0; f < opt->infiles; f++){
    if (f) {
      hdr[f] = (MrcHeader *)malloc(sizeof(MrcHeader));
      if (!hdr[f])
        exitError("Allocating header structure");
      hdr[f]->fp = iiFOpen(opt->fnames[f], "rb");
      if (!hdr[f]->fp){
        show_error("clip supergain: error opening %s.", opt->fnames[f]);
        return(-1);
      }
      if (mrc_head_read(hdr[f]->fp, hdr[f])){
        show_error("clip supergain: error reading header of %s.", opt->fnames[f]);
        return(-1);
      }
    }
    if ( (h1->nx != hdr[f]->nx) || (h1->ny != hdr[f]->ny) || 
         (hdr[f]->mode != MRC_MODE_BYTE )) {
      show_error("clip supergain: all input files must be the same X/Y size and mode 0.");
      return(-1);
    }
    iiFile = iiLookupFileFromFP(hdr[f]->fp);
    if (!iiFile)
      exitError("Cannot find image file structure from file pointer");
    if (iiFile->file != IIFILE_TIFF || !iiFile->numFramesInEERfile) {
      show_error("clip supergain: all input files must be EER files.");
      return(-1);
    }
  }

  // Get arrays
  imageArr = B3DMALLOC(float, h1->nx * h1->ny);
  memset(imageArr, 0, h1->nx * h1->ny * sizeof(float));
  inputArr = B3DMALLOC(unsigned char, h1->nx * h1->ny);

  // Read the data and sum them
  for (f = 0; f < opt->infiles; f++) {
    for (iz = 0; iz < hdr[f]->nz; iz++) {
      if (mrc_read_slice(inputArr, hdr[f]->fp, hdr[f], iz, 'z'))
        exitError("Reading super-slice %d from file %s", iz, opt->fnames[f]);
      for (ind = 0; ind < h1->nx * h1->ny; ind++)
        imageArr[ind] = inputArr[ind];
    }
    if (f)
      iiFClose(hdr[f]->fp);
  }

  // Loop on the divisions, subdivided by 2 so they can be added together 2x2
  // and accumulate the bias sums
  // A few pixels difference in areas will make numbers surprisingly different, but
  // trimming off the edges seems to avoid larger variability
  physX = h1->nx / 4 - 4;
  xSpacing = 4 * (physX / numDiv);
  xOffset = 4 * ((physX % numDiv) / 2);
  physY = h1->ny / 4 - 4;
  ySpacing = 4 * (physY / numDiv);
  yOffset = 4 * ((physY % numDiv) / 2);
  for (i = 0; i < 16; i++)
    areaSum[i] = 0.;
  for (ydiv = 0; ydiv < numDiv; ydiv++) {
    for (xdiv = 0; xdiv < numDiv; xdiv++) {
      divInd = xdiv + ydiv * numDiv;
      bias = biases[divInd];
      for (i = 0; i < 16; i++)
        bias[i] = 0.;
      for (j = yOffset + ydiv * ySpacing; j < yOffset + (ydiv + 1) * ySpacing; j++) {
        for (i = xOffset + xdiv * xSpacing; i < xOffset + (xdiv + 1) * xSpacing; i++) {
          ind = i % 4 + 4 * (j % 4);
          bias[ind] += imageArr[i + j * h1->nx];
        }
      }
      for (i = 0; i < 16; i++)
        areaSum[i] += bias[i];
    }
  }

  // Output summaries from the grand sums
  bsum = 0.;
  for (i = 0; i < 16; i++)
    bsum += areaSum[i] / 16.;
  printf("Overall gain factors for 4x4 super-resolution:\n");
  for (i = 12; i >= 0; i -= 4) {
    for (j = 0; j < 4; j++)
      printf("  %.6f", bsum / areaSum[i + j]);
    printf("\n");
  }

  bsum = combineAreaSums(areaSum);
  printf("Overall gain factors for 2x2 super-resolution:\n");
  for (i = 2; i >= 0; i -= 2)
    printf("  %.6f  %.6f\n", bsum / areaSum[i], bsum / areaSum[i + 1]);

  // Output to file
  fprintf(outFP, "1  4\n");
  fprintf(outFP, "%d %d %d %d %d %d\n", numDiv - 1, xOffset + xSpacing, xSpacing, 
          numDiv - 1, yOffset + ySpacing, ySpacing);
  for (ydiv = 0; ydiv < numDiv - 1; ydiv++) {
    for (xdiv = 0; xdiv < numDiv - 1; xdiv++) {
      divInd = xdiv + ydiv * numDiv;
      bsum = 0.;
      for (i = 0; i < 16; i++) {
        areaSum[i] = biases[divInd][i] + biases[divInd + 1][i] + 
          biases[divInd + numDiv][i] + biases[divInd + numDiv + 1][i];
        bsum += areaSum[i] / 16.;
      }
      for (i = 0; i < 16; i++)
        fprintf(outFP, " %.5f", bsum / areaSum[i]);
      fprintf(outFP, "\n");
      
      bsum = combineAreaSums(areaSum);
      for (i = 0; i < 4; i++)
        fprintf(outFP, " %.5f", bsum / areaSum[i]);
      fprintf(outFP, "\n");
    }
  }      
  fclose(outFP);
  free(imageArr);
  free(inputArr);
  for (i = 0; i < numDiv; i++)
    free(biases[i]);
  return 0;
}

static float combineAreaSums(double *areaSum)
{
  float bsum = 0.;
  areaSum[0] = areaSum[0] + areaSum[1] + areaSum[4] + areaSum[5];
  areaSum[1] = areaSum[2] + areaSum[3] + areaSum[6] + areaSum[7];
  areaSum[2] = areaSum[8] + areaSum[9] + areaSum[12] + areaSum[13];
  areaSum[3] = areaSum[10] + areaSum[11] + areaSum[14] + areaSum[15];
  for (int i = 0; i < 4; i++)
    bsum += areaSum[i] / 4.;
  return bsum;
}

int clip_parxyz(Istack *v, 
                int xmax, int ymax, int zmax,
                float *rx, float *ry, float *rz)
{ 
  float a,b;
  float x,y,z;
  int x1,x2,x3;
  float y1,y2,y3;

  x1 = xmax - 1; x2 = xmax; x3 = xmax + 1;
  if (x1 < 0)
    x1 = v->vol[0]->xsize - 1;
  if (x3 >= v->vol[0]->xsize) 
    x3 = 0;
  y1 = sliceGetPixelMagnitude(v->vol[zmax], x1, ymax);
  y2 = sliceGetPixelMagnitude(v->vol[zmax], x2, ymax);
  y3 = sliceGetPixelMagnitude(v->vol[zmax], x3, ymax);
  x1 = xmax - 1; x3 = xmax + 1;
  a = (y1*(x2-x3)) + (y2*(x3-x1)) + (y3*(x1-x2));
  b = (x1*x1*(y2-y3)) + (x2*x2*(y3-y1)) + (x3*x3*(y1-y2));
  if (a)
    x = -b/(2*a);
  else
    x = xmax;

  x1 = ymax - 1; x2 = ymax; x3 = ymax + 1;
  if (x1 < 0)
    x1 = v->vol[0]->ysize - 1;
  if (x3 >= v->vol[0]->ysize)
    x3 = 0;
  y1 = sliceGetPixelMagnitude(v->vol[zmax], xmax, x1);
  y2 = sliceGetPixelMagnitude(v->vol[zmax], xmax, x2);
  y3 = sliceGetPixelMagnitude(v->vol[zmax], xmax, x3);
  x1 = ymax - 1; x3 = ymax + 1;
  a = (y1*(x2-x3)) + (y2*(x3-x1)) + (y3*(x1-x2));
  b = (x1*x1*(y2-y3)) + (x2*x2*(y3-y1)) + (x3*x3*(y1-y2));
  if (a)
    y = -b/(2*a);
  else
    y = ymax;

  x1 = zmax - 1; x2 = zmax; x3 = zmax + 1;
  if (x1 < 0)
    x1 = v->zsize - 1;
  if (x3 >= v->zsize)
    x3 = 0;
  y1 = sliceGetPixelMagnitude(v->vol[x1], xmax, ymax);
  y2 = sliceGetPixelMagnitude(v->vol[x2], xmax, ymax);
  y3 = sliceGetPixelMagnitude(v->vol[x3], xmax, ymax);
  x1 = zmax - 1; x3 = zmax + 1;
  a = (y1*(x2-x3)) + (y2*(x3-x1)) + (y3*(x1-x2));
  b = (x1*x1*(y2-y3)) + (x2*x2*(y3-y1)) + (x3*x3*(y1-y2));
  if (a)
    z = -b/(2*a);
  else
    z = zmax;

     
  *rx = x;
  *ry = y;
  *rz = z;

  return(0);

}

int clip_stat3d(Istack *v)
{
  float min, max, mean;
  float x, y, z;
  int   xmax, ymax, zmax;

  clip_get_stat3d(v, &min, &max, &mean, &xmax, &ymax, &zmax);
  clip_parxyz(v, xmax, ymax, zmax, &x, &y, &z);

  printf("max = %g  min = %g  mean = %g\n",max,min,mean);
  printf("location of max pixel ( %d, %d, %d) is \n", xmax, ymax, zmax);
  printf("( %.2f, %.2f, %.2f)\n", x, y, z);
  return(0);
}


int clip_get_stat3d(Istack *v, 
                    float *rmin, float *rmax, float *rmean,
                    int *rx, int *ry, int *rz)
{
  int nx, ny, nz;
  int xmax, ymax, zmax;
  int i, j, k;
  float min, max, mean, m;
  double dmean;
  xmax = ymax = zmax = 0;

  max = FLT_MIN;
  min = 1e36f;
  dmean = 0.0;
  nx = v->vol[0]->xsize;
  ny = v->vol[0]->ysize;
  nz = v->zsize;

  for (k = 0; k < nz; k++){
    for(j = 0; j < ny; j++)
      for(i = 0; i < nx; i++){
        m = sliceGetPixelMagnitude(v->vol[k], i, j);
        if (m > max){
          max = m;
          xmax = i;
          ymax = j;
          zmax = k;
        }
        if (m < min)
          min = m;
        dmean += m;
      }
  }
  mean = (float)(dmean / (double)(nx * ny * nz));

  *rmin = min;
  *rmean = mean;
  *rmax = max;
  *rx = xmax;
  *ry = ymax;
  *rz = zmax;
  return(0);
}

struct zstats {
  float x, y;
  double mean, std;
  int xmin, ymin, outlier;
};

#define PROCESS_PIXEL  \
  if (m > max) {       \
    max = m;           \
    xmax = i;          \
    ymax = j;          \
  }                    \
  if (m < min) {       \
    min = m;           \
    xmin = i;          \
    ymin = j;          \
  }                    \
  m -= prelimMean;     \
  tsum += m;           \
  tsumsq += m * m;

int clip_stat(MrcHeader *hin, ClipOptions *opt)
{
  int i, j, k, iz, di, dj, nsum, length, kk, outlast;
  int xmax, ymax, xmin, ymin, zmin, zmax, izo, xmaxo, ymaxo;
  int minxpiece, xoverlap, numXpieces, minypiece, yoverlap, numYpieces;
  Islice *slice;
  float min, max, m, ptnum;
  double mean, std, sumsq, tsum, tsumsq, vmean, vsumsq, cx, cy, prelimMean, data[3][3];
  float x,y;
  float vmin, vmax, kcrit = 2.24f;
  char starmin, starmax;
  b3dInt16 *sdata;
  b3dUInt16 *usdata;
  float *fdata;
  IloadInfo li;
  int viewAdd = opt->fromOne ? 1 : 0;
  struct zstats *stats;
  float *allmins, *allmaxes, *ifdrop;
  int outliers = (opt->val != IP_DEFAULT || opt->low != IP_DEFAULT) ? 1 : 0;

  /* Try to get a piece list from file and adjust it for overlaps */
  mrc_init_li(&li, NULL);
  if (opt->plname && mrc_plist_li(&li, hin, opt->plname)) {
    show_error("stat: error reading piece list file");
    return -1;
  }
  if (li.plist) {
    if (li.plist < hin->nz) {
      show_error("stat: not enough piece coordinates in file");
      return -1;
    }
    if (opt->newXoverlap == IP_DEFAULT)
      opt->newXoverlap = 0;
    if (opt->newYoverlap == IP_DEFAULT)
      opt->newYoverlap = 0;
    if (checkPieceList(li.pcoords, 3, li.plist, 1, hin->nx, &minxpiece, &numXpieces,
                       &xoverlap) || 
        checkPieceList(li.pcoords + 1, 3, li.plist, 1, hin->ny, &minypiece, &numYpieces,
                       &yoverlap)) {
      show_error("stat: piece coordinates are not regularly spaced");
      return -1;
    }
    if (numXpieces > 1)
      adjustPieceOverlap(li.pcoords, 3, li.plist, hin->nx, minxpiece, xoverlap, 
                         opt->newXoverlap);
    if (numYpieces > 1)
      adjustPieceOverlap(li.pcoords + 1, 3, li.plist, hin->ny, minypiece, yoverlap,
                         opt->newYoverlap);
    printf("piece|   min   |(   x,  %s)|    max  |(   x,  %s)|   mean\n",
           opt->fromOne ? "y, view" : " y,   z", opt->fromOne ? "y, view" : " y,   z");
    printf("-----|---------|----------------|---------|----------------|---------\n");
  } else {

    printf("%s|   min   |(   x,   y)|    max  |(      x,      y)|   mean    |  "
           "std dev.\n", opt->fromOne ? "view " : "slice");
    printf("-----|---------|-----------|---------|-----------------|-----------|--"
           "--------\n");
  }
     
  set_input_options(opt, hin);
  stats = (struct zstats *)malloc(opt->nofsecs * sizeof(struct zstats));
  allmins = (float *)malloc(opt->nofsecs * sizeof(float));
  allmaxes = (float *)malloc(opt->nofsecs * sizeof(float));
  ifdrop = (float *)malloc(opt->nofsecs * sizeof(float));
  if (!stats || !allmins || !allmaxes || !ifdrop) {
    show_error("stat: error allocating array for statistics.");
    return(-1);
  }
  if (outliers) {
    length = opt->nofsecs;
    if (opt->low != IP_DEFAULT)
      length = B3DNINT(opt->low);
    if (opt->val != IP_DEFAULT)
      kcrit = opt->val;
    length = B3DMIN(opt->nofsecs, B3DMAX(5, length));
    outlast = -1;
  }

  for (k = 0; k < opt->nofsecs; k++) {
    iz = opt->secs[k];
    if ((iz < 0) || (iz >= hin->nz)) {
      show_error("stat: slice out of range.");
      return(-1);
    }
    slice = sliceReadSubm(hin, iz, 'z', opt->ix, opt->iy,
                          (int)opt->cx, (int)opt->cy);
    if (!slice){
      show_error("stat: error reading slice.");
      return(-1);
    }

    m = sliceGetPixelMagnitude(slice, slice->xsize / 2, slice->ysize / 2);
    min = m;
    max = m;
    std = 0;
    sumsq = 0.;
    mean = 0;
    xmax = 0;
    ymax = 0;
    xmin = 0;
    ymin = 0;
    ptnum = (float)(slice->xsize * slice->ysize);
    prelimMean = m;
    if (slice->xsize > 10 && slice->ysize > 10) {
      
      // Get a preliminary mean from widely spaced samples to reduce numerical errors
      tsum = 0.;
      for (j = 1; j < 9; j++)
        for (i = 1; i < 9; i++)
          tsum += sliceGetPixelMagnitude(slice, (i * slice->xsize) / 10, 
                                         (j * slice->ysize) / 10);
      prelimMean = tsum / 64.;
    }
    for (j = 0; j < slice->ysize; j++) {
      tsum = 0.;
      tsumsq = 0.;
      switch (slice->mode) {

        // 5/19/17: It now is about twice as fast to do it directly, added USHORT/FLOAT
      case SLICE_MODE_SHORT:
        sdata = &slice->data.s[slice->xsize * j];
        for (i = 0; i < slice->xsize; i++) {
          m = *sdata++;
          PROCESS_PIXEL;
        }
        break;
        
      case SLICE_MODE_USHORT:
        usdata = &slice->data.us[slice->xsize * j];
        for (i = 0; i < slice->xsize; i++) {
          m = *usdata++;
          PROCESS_PIXEL;
        }
        break;
        
      case SLICE_MODE_FLOAT:
        fdata = &slice->data.f[slice->xsize * j];
        for (i = 0; i < slice->xsize; i++) {
          m = *fdata++;
          PROCESS_PIXEL;
        }
        break;
      default:
        for (i = 0; i < slice->xsize; i++) {
          m = sliceGetPixelMagnitude(slice, i, j);
          PROCESS_PIXEL;
        }
        break;
      }
      mean += tsum;
      sumsq += tsumsq;
    }
    mean = mean / ptnum;

    /* DNM 5/23/05: switched to computing from differences to using sum of
       squares, which gives identical results with doubles  */
    // 5/19/17: No it isn't when the SD is small!  Like 3 with a mean of 32763
    // So get SD from the sum of deviations from the preliminary mean, then adjust mean
    std = (sumsq - ptnum * mean * mean) / B3DMAX(1.,(ptnum - 1.));
    std = sqrt(B3DMAX(0., std));
    mean += prelimMean;
  
    /* 3/23/09: switched to parabolic fit, center of gravity is ridiculous */
    for (dj = 0, j = ymax - 1; j <= ymax + 1; j++, dj++)
      for(di = 0,i = xmax - 1; i <= xmax + 1; i++ , di++)
        data[dj][di] = sliceGetPixelMagnitude(slice, i, j);

    parabolic_fit(&cx, &cy, data);
    x = cx + xmax;
    y = cy + ymax;
    
    /* x = xmax;
       y = ymax;
       corr_getmax(slice, 7, xmax, ymax, &x, &y); */

    if (opt->sano){
      x -= (float)slice->xsize/2;
      y -= (float)slice->ysize/2;
    } else {

      /* Adjust coordinates for subarea */
      x += (int)opt->cx - opt->ix / 2;
      y += (int)opt->cy - opt->iy / 2;
      xmin += (int)opt->cx - opt->ix / 2;
      ymin += (int)opt->cy - opt->iy / 2;
    }

    if (k == 0) {
      vmin  = min;
      vmax  = max;
      vmean = 0.;
      zmin = iz;
      zmax = 0;
    } else {
      if (min < vmin) {
        vmin = min;
        zmin = iz;
      }
      if (max > vmax) {
        vmax = max;
        zmax = iz;
      }
    }
    vmean += mean;

    allmins[k] = min;
    stats[k].xmin = xmin;
    stats[k].ymin = ymin;
    allmaxes[k] = max;
    stats[k].x = x;
    stats[k].y = y;
    stats[k].mean = mean;
    stats[k].std = std;
    if (!outliers) {
      if (li.plist) {
        xmin += li.pcoords[3*iz] + viewAdd;
        ymin += li.pcoords[3*iz+1] + viewAdd;
        xmaxo = B3DNINT(x) + li.pcoords[3*iz] + viewAdd;
        ymaxo = B3DNINT(y) + li.pcoords[3*iz+1] + viewAdd;
        printf("%4d  %9.4f (%4d,%4d,%4d) %9.4f (%4d,%4d,%4d) %9.4f\n", iz + viewAdd, min,
               xmin, ymin, li.pcoords[3*iz+2] + viewAdd, max, xmaxo, ymaxo,
               li.pcoords[3*iz+2] + viewAdd, mean);
      } else
        printf("%4d  %9.4f (%4d,%4d) %9.4f (%7.2f,%7.2f) %9.4f  %9.4f\n", iz + viewAdd,
               min, xmin + viewAdd, ymin + viewAdd, max, x, y, mean, std);
    }
    else if (k >= length - 1) {
      for (kk = outlast + 1; kk <= k; kk++) {
        starmin = ' ';
        starmax = ' ';
        di = B3DMAX(0, kk - length / 2);
        dj = B3DMIN(opt->nofsecs, di + length);
        di = B3DMAX(0, dj - length);
        if (dj > k + 1)
          break;
        //printf("kk %d  di %d  dj %d  kcrit %f\n", kk, di, dj, kcrit);
        rsMadMedianOutliers(&allmins[di], dj-di, kcrit, &ifdrop[di]);
        stats[kk].outlier = 0;
        if (ifdrop[kk] < 0.) {
          starmin = '*';
          stats[kk].outlier = 1;
        }
        rsMadMedianOutliers(&allmaxes[di], dj-di, kcrit, &ifdrop[di]);
        if (ifdrop[kk] > 0.) {
          starmax = '*';
          stats[kk].outlier = 1;
        }

      if (li.plist) {
        xmin = stats[kk].xmin + li.pcoords[3*kk] + viewAdd;
        ymin = stats[kk].ymin + li.pcoords[3*kk+1] + viewAdd;
        xmaxo = B3DNINT(stats[kk].x) + li.pcoords[3*kk] + viewAdd;
        ymaxo = B3DNINT(stats[kk].y) + li.pcoords[3*kk+1] + viewAdd;
        izo = li.pcoords[3*opt->secs[kk] + 2] + viewAdd;
        printf("%4d  %9.4f%c(%4d,%4d,%4d) %9.4f%c(%4d,%4d,%4d) %9.4f  %9.4f\n", 
               opt->secs[kk]+ viewAdd, allmins[kk], starmin, xmin, ymin, izo, 
               allmaxes[kk], starmax, xmaxo, ymaxo, izo, stats[kk].mean, stats[kk].std);
      } else
        printf("%4d  %9.4f%c(%4d,%4d) %9.4f%c(%7d,%7d) %9.4f  %9.4f\n", 
               opt->secs[kk]+ viewAdd, allmins[kk], starmin,
               stats[kk].xmin, stats[kk].ymin, allmaxes[kk], starmax,
               B3DNINT(stats[kk].x), B3DNINT(stats[kk].y), stats[kk].mean, stats[kk].std);
        outlast = kk;
      }
      
    }
    sliceFree(slice);
  }

  // Compute sum-squared deviation from overall mean by adjusting the deviation from
  // mean of each section for difference between its mean and overall mean
  vmean /= (float)opt->nofsecs;
  vsumsq = 0.;
  for (k = 0; k < opt->nofsecs; k++)
    vsumsq += ptnum * (stats[k].mean * stats[k].mean - vmean * vmean) + (ptnum - 1.) * 
      stats[k].std * stats[k].std;
  ptnum *= opt->nofsecs;
  std = vsumsq / B3DMAX(1.,(ptnum - 1.));
  std = sqrt(B3DMAX(0., std));

  if (li.plist)
    printf(" all  %9.4f (@ piece =%5d) %9.4f (@ piece =%5d) %9.4f  %9.4f\n",
           vmin, zmin + 1, vmax, zmax+1, vmean, std);
  else
    printf(" all  %9.4f (@ z=%5d) %9.4f (@ z=%5d      ) %9.4f  %9.4f\n",
           vmin, zmin + viewAdd, vmax, zmax + viewAdd, vmean, std);

  if (outliers) {
    printf("\n%s with %sextreme values:", 
           li.plist ? "Pieces" : (opt->fromOne ? "Views" : "Slices"),
           opt->low != IP_DEFAULT ? "locally " : "");
    nsum = 0;
    length = 28 + (opt->low != IP_DEFAULT ? 8 : 0);
    for (k = 0; k < opt->nofsecs; k++) {
      if (stats[k].outlier) {
        printf(" %3d", opt->secs[k] + viewAdd);
        length += 4;
        if (length > 74) {
          printf("\n");
          length = 0;
        }
        nsum++;
      }
    }
    if (!nsum)
      printf(" None");
    printf("\n");
  }
  return(0);
}

/*
 * Histograms
 */
int clipHistogram(MrcHeader *hin, ClipOptions *opt)
{
#define MAX_HIST_BINS 65536
  int bins[MAX_HIST_BINS];
  int *comboBins;
  float xx[11], yy[11];
  Islice *slice;
  int numBins, i, j, ind, combine, maxBin, minBin, sum, k, iz, peakInd, dir, diffInd;
  int nxSlice, nySlice;
  int start, end, offset = 0, numFit = 7;
  int floatVals = FALSE;
  float delta, histMin, histMax, val, comboLeft, frac, thresh, ff, rInd, maxDiff;
  double threshCounts, cumulCounts = 0.;
  set_input_options(opt, hin);

  if (opt->sano)
    return histogramPeaksAndDip(hin, opt);

  switch (hin->mode) {

    // For integer types, set up to  place counts in bins without scaling
  case MRC_MODE_BYTE:
  case MRC_MODE_RGB:
    numBins = 256;
    break;
  case MRC_MODE_SHORT:
    offset = 32768;
  case MRC_MODE_USHORT:
    numBins = 65536;
    break;
  case MRC_MODE_FLOAT:
  case MRC_MODE_COMPLEX_FLOAT:

    // For floating point types, set up the min and max of the histogram as the file 
    // min/max, modified by the entries
    floatVals = TRUE;
    histMin = hin->amin;
    if (opt->low != IP_DEFAULT)
      histMin = opt->low;
    histMax = hin->amax;
    if (opt->high != IP_DEFAULT)
      histMax = opt->high;
    if (histMin >= histMax) {
      show_error("clip histogram - minimum (%f) must be less than maximum (%f)", histMin,
                 histMax);
      return -1;
    }
    delta = (histMax - histMin) / 256.;
    if (opt->val != IP_DEFAULT) {
      delta = opt->val;
      if (delta <= 0) {
        show_error("clip histogram - histogram bin size (%f) must be positive", delta);
        return -1;
      }
    }
    numBins = (int)ceil((histMax - histMin) / delta);
    if ((histMax - histMin) / delta >= numBins - 0.01)
      numBins++;
    B3DCLAMP(numBins, 0, MAX_HIST_BINS - 1);
  }
  
  for (ind = 0; ind < numBins; ind++)
    bins[ind] = 0;
  for (k = 0; k < opt->nofsecs; k++) {
    iz = opt->secs[k];
    slice = sliceReadSubm(hin, iz, 'z', opt->ix, opt->iy, (int)opt->cx, (int)opt->cy);
    if (!slice){
      show_error("clip histogram - error reading slice %d", iz);
      return(-1);
    }
    nxSlice = slice->xsize;
    nySlice = slice->ysize;
    
    // Get the values and put them in the bins
    for (j = 0; j < slice->ysize; j++) {
      if (floatVals) {
        for (i = 0; i < slice->xsize; i++) {
          val = sliceGetPixelMagnitude(slice, i, j);
          ind = (val - histMin) / delta;
          if (ind >= 0 && ind < numBins)
            bins[ind]++;
        }
      } else {
        for (i = 0; i < slice->xsize; i++) {
          ind = B3DNINT(sliceGetPixelMagnitude(slice, i, j));
          bins[ind + offset]++;
        }
      }
    }
    sliceFree(slice);
  }

  // Get the threshold at a percentile if asked for
  if (opt->thresh != IP_DEFAULT) {
    threshCounts = ((opt->thresh * opt->nofsecs) * nxSlice) * nySlice;
    for (ind = 0; ind < numBins; ind++) {
      cumulCounts += bins[ind];
      if (cumulCounts >= threshCounts) {
        frac = (cumulCounts - threshCounts) / bins[ind];
        if (floatVals)
          thresh = histMin + (ind - frac) * delta;
        else
          thresh = (ind - frac) - offset;
        break;
      }
    }
  }

  // Find first and last bin with counts
  minBin = -1;
  maxBin = -1;
  for (ind = 0; ind < numBins; ind++) {
    if (bins[ind]) {
      maxBin = ind;
      if (minBin < 0)
        minBin = ind;
    }
  }

  // For integers, revise the limits based on entries
  if (!floatVals && minBin >= 0) {
    if (opt->low != IP_DEFAULT && opt->high != IP_DEFAULT && opt->low > opt->high) {
      show_error("clip histogram - minimum (%f) must be less than maximum (%f)", opt->low,
                 opt->high);
      return -1;
    }
    if (opt->low != IP_DEFAULT)
      minBin = B3DMAX(minBin, B3DNINT(opt->low + offset));
    if (opt->high != IP_DEFAULT)
      maxBin = B3DMIN(maxBin, B3DNINT(opt->high + offset));
    if (minBin > maxBin)
      minBin = 1;
  }
  if (minBin < 0) {
    printf("There are no values within the specified range\n");
    return 0;
  }
  numBins = maxBin + 1 - minBin;
  comboBins = &bins[minBin];
  peakInd = -1;
  if (floatVals) {
    comboLeft = histMin + minBin * delta;
    printf(" Bin midpoint   counts    (bin interval is %f)\n", delta);
    for (ind = minBin; ind <= maxBin; ind++) {
      printf("%13.6g %8d\n", histMin + (ind + 0.5) * delta, bins[ind]);
      if (peakInd < 0 || bins[ind] > bins[peakInd])
        peakInd = ind;
    }
    peakInd -= minBin;
  } else {

    // For integers, figure out combining of bins
    combine = 1;
    if (opt->val != IP_DEFAULT) {
      combine = B3DNINT(opt->val);
      if (combine < 1) {
        show_error("clip histogram - Entered bin size (%f) must be > 0.5", opt->val);
        return -1;
      }
    } else {
      combine = B3DNINT((maxBin - minBin) / 256.);
      combine = B3DMAX(1, combine);
    }

    // Combine and print bins
    delta = combine;
    comboLeft = minBin - offset;
    if (combine > 1) {
      printf("Bin midpoint   counts    (bin interval is %d)\n", combine);
      numBins = ((maxBin + 1 - minBin) + combine - 1) / combine;
      comboBins = bins;
      for (ind = 0; ind < numBins; ind++) {
        sum = 0;
        for (i = 0; i < combine; i++)
          sum += bins[B3DMIN(maxBin, minBin + ind * combine + i)];
        printf("%12.1f %8d\n", minBin + (ind + 0.5) * combine - offset, sum);
        bins[ind] = sum;
        if (peakInd < 0 || bins[ind] > bins[peakInd])
          peakInd = ind;
      }
    } else {

      // Or just print bins
      printf(" Value   counts    (bin interval is %d)\n", combine);
      for (ind = minBin; ind <= maxBin; ind++) {
        printf("%6d %8d\n", ind - offset, bins[ind]);
        if (peakInd < 0 || bins[ind] > bins[peakInd])
          peakInd = ind;
      }
      peakInd -= minBin;
    }
  }

  if (opt->thresh != IP_DEFAULT)
    printf("Threshold value for reaching %g of counts = %g\n", opt->thresh, thresh);

  if (opt->falloffFrac != IP_DEFAULT) {

    // For finding a maximal falloff point, set up the direction based on sign
    if (opt->falloffFrac > 0) {
      dir = 1;
      ind = 1;
    } else {
      dir = -1;
      ind = numBins - 2;
    }
    diffInd = -1;
    maxDiff = 0.;
    cumulCounts = 0;
    threshCounts = ((fabs(opt->falloffFrac) * opt->nofsecs) * nxSlice) *nySlice;

    // Loop and find maximum falloff ratio past the required cumulative counts
    // Need to use fits to measure the ratio well once the counts get low enough
    while (ind > 0 && ind < numBins - 1) {
      cumulCounts += comboBins[ind - dir];
      if (comboBins[ind] > 3000 && comboBins[ind - dir] > 3000) {
        frac = comboBins[ind - dir] / (float)comboBins[ind];
      } else {
        if (comboBins[ind] > 1000 && comboBins[ind - dir] > 1000)
          numFit = 3;
        if (comboBins[ind] > 300 && comboBins[ind - dir] > 300)
          numFit = 5;
        if (comboBins[ind] > 100 && comboBins[ind - dir] > 100)
          numFit = 7;
        if (comboBins[ind] > 30 && comboBins[ind - dir] > 30)
          numFit = 9;
        else
          numFit = 11;
        start = B3DMAX(0, ind - numFit / 2);
        end = B3DMIN(numBins - 1, start + numFit - 1);
        start = B3DMAX(0, end + 1 - numFit);
        for (j = 0; j <= end - start; j++) {
          xx[j] = j;
          yy[j] = comboBins[j + start];
        }
        lsFit(xx, yy, end + 1 - start, &frac, &ff, &val);
        frac = (frac * (ind - dir - start) + ff) / B3DMAX(1., frac * (ind - start) + ff);
      }
      if (comboBins[ind] > 100 && frac > maxDiff && cumulCounts > threshCounts) {
        maxDiff = frac;
        diffInd = ind;
      }
      ind += dir;
    }

    if (diffInd < 0) {
      printf("ERROR: CLIP - No point of maximum falloff could be found past %.3f of "
             "total cumulative counts\n", fabs(opt->falloffFrac));
      return(-1);
    }

    printf("Maximum falloff occurs at %g\n", comboLeft + diffInd * delta);
  }

  if (opt->pctlFrac == IP_DEFAULT)
    return 0;

  // Now for looking for extra counts, need a peak not too close to end
  if (peakInd > 0.8 * numBins || peakInd < 0.2 * numBins) {
    printf("ERROR: CLIP - Peak is at %f, too close to end of range to analyze for "
           "extra counts\n", comboLeft + (peakInd + 0.5) * delta);
    return(-1);
  }

  frac = parabolicFitPosition(bins[peakInd - 1], bins[peakInd], bins[peakInd + 1]);
  if (opt->pctlFrac > 0) {
    dir = 1;
    start = numBins - 1;
  } else {
    dir = -1;
    start = 0;
  }
  diffInd = -1;
  printf("Interpolated peak position %13.6g\n", comboLeft + (peakInd + frac + 0.5) * 
         delta);

  // For each position on the side to be computed, get the corresponding position on the
  // opposite side and interpolate if it is in legal range, then subtract and replace
  // Find the peak in this pass
  ind = start;
  printf("Bins %s peak minus bins %s peak:\n", dir > 0 ? "above" : "below", 
         dir > 0 ?  "below" : "above");
  while (dir * (ind - (peakInd + dir * 3)) > 0) {
    rInd = 2 * (peakInd + frac) - ind;
    val = 0;
    j = (int)floor(rInd);
    ff = rInd - j;
    if (j >= 0 && j < numBins - 1) {
      val = (1. - ff) * comboBins[j] + ff * comboBins[j + 1];
      printf("%13.6g %8d\n", comboLeft + (ind + 0.5) * delta, 
             comboBins[ind] - B3DNINT(val));
      comboBins[ind] -= B3DNINT(val);
      if (diffInd < 0 || comboBins[ind] > comboBins[diffInd])
        diffInd = ind;
    }
    ind -= dir;
  }

  if (diffInd < 0 || comboBins[diffInd] <= 0) {
    printf("ERROR: CLIP - There are fewer counts %s the peak than %s it\n",
           dir > 0 ? "above" : "below", dir > 0 ?  "below" : "above");
    return(-1);
  }
  
  // Now get cumulative count to end until the first crossing past the peak
  cumulCounts = 0.;
  ind = start;
  while (dir * (ind - (peakInd + dir * 3)) > 0) {
    if (dir * (ind - diffInd) < 0 && comboBins[ind] < 0)
      break;
    cumulCounts += comboBins[ind];
    ind -= dir;
  }
  if (cumulCounts <= 0) {
    printf("ERROR: CLIP - There are fewer counts %s the peak than %s it\n",
           dir > 0 ? "above" : "below", dir > 0 ?  "below" : "above");
    return(-1);
  }

  threshCounts = fabs(opt->pctlFrac) * cumulCounts;
  cumulCounts = 0.;
  ind = start;
  while (dir * (ind - (peakInd + dir * 3)) > 0) {
    if (dir * (ind - diffInd) < 0 && comboBins[ind] < 0)
      break;
    cumulCounts += comboBins[ind];
    if (cumulCounts > threshCounts) {
      frac = (cumulCounts - threshCounts) / comboBins[ind];
      rInd = ind + dir * frac;
      val = comboLeft + rInd * delta;
      printf("%.3f of the extra counts %s the peak occur %s %g\n", fabs(opt->pctlFrac),
             dir > 0 ? "above" : "below", dir > 0 ? "above" : "below", val);
      return 0;
    }
    ind -= dir;
  }
  printf("ERROR: CLIP - Extra counts occurred %s the peak but percentile analysis "
         "failed\n", dir > 0 ? "above" : "below");
  return(-1);
}

/*
 * Completely different histogram analysis for peaks, dip, and fraction below dip
 */
int histogramPeaksAndDip(MrcHeader *hin, ClipOptions *opt)
{
#define KERNEL_HIST_BINS 1000
  float *sample = NULL;
  float bins[KERNEL_HIST_BINS];
  size_t fullSize;
  Islice *slice;
  int i, j, k, iz, ind, numSample, interval, sampleSize, numBelow;
  float firstVal, lastVal, peakBelow, peakAbove, histDip;
  int izAdd = opt->fromOne ? 1 : 0;
  bool wrap, asVol = opt->dim == 3;

  if (opt->low != IP_DEFAULT || opt->high != IP_DEFAULT || opt->val != IP_DEFAULT) {
    printf("ERROR: CLIP - The -n, -l, and -h options have no effect when doing a "
           "histogram with -s\n");
    return (-1);
  }

  // Loop through the slices
  for (k = 0; k < opt->nofsecs; k++) {
    iz = opt->secs[k];
    slice = sliceReadSubm(hin, iz, 'z', opt->ix, opt->iy, (int)opt->cx, (int)opt->cy);
    if (!slice){
      printf("ERROR: CLIP - reading %s %d", opt->fromOne ? "view" : "slice", iz + izAdd);
      return(-1);
    }

    // Set up array and sampling the first time, for sampling the entire volume or
    // each slice one at a time
    if (!k) {
      fullSize = (size_t)slice->xsize * slice->ysize * (asVol ? opt->nofsecs : 1);
      sampleSize = B3DMIN(1000000, fullSize);
      sample = B3DMALLOC(float, sampleSize);
      if (!sample) {
        printf("ERROR: CLIP - getting memory for pixel sample for histogram\n");
        return (-1);
      }
      interval = B3DMAX(1, (fullSize + sampleSize - 1) / sampleSize);
      numSample = fullSize / interval;
    }

    // Prepare for the next histogram, zeroing out the sample indexes
    if (!k || !asVol) {
      i = 0;
      j = 0;
      ind = 0;
      lastVal = -1.e37;
      firstVal = 1.e37;
    }
    
    // Step through the slice at the interval, adding pixels to the sample
    wrap = false;
    while (ind < numSample && !wrap) {
      sample[ind] = sliceGetPixelMagnitude(slice, i, j);
      lastVal = B3DMAX(lastVal, sample[ind]);
      firstVal = B3DMIN(firstVal, sample[ind]);
      ind++;
      i += interval;

      // When X overflows, increment Y until X gets back into range.
      while (i >= slice->xsize) {
        i -= slice->xsize;
        j++;

        // Just wrap Y around if necessary and set flag that it did to stop loop
        if (j >= slice->ysize) {
          j = 0;
          wrap = true;
        }
      }
    }

    // Time to do a histogram.  Trim the extremes, this is essential
    if (!asVol || k == opt->nofsecs - 1) {
      if (numSample > 4000) {
        ind = (int)(0.0005 * numSample);
        firstVal = percentileFloat(ind + 1, sample, numSample);
        lastVal = percentileFloat(numSample - ind, sample, numSample);
      }

      // Skip if the limits are equal (it looks like ANY difference is enough to work)
      if (lastVal > firstVal)
        ind = findHistogramDip(sample, numSample, 0, bins, KERNEL_HIST_BINS, firstVal,
                               lastVal, &histDip, &peakBelow, &peakAbove, 0);
      else
        ind = 1;
      if (asVol)
        printf("All %ss: ", opt->fromOne ? "view" : "slice");
      else
        printf("%s %d: ", opt->fromOne ? "View" : "Slice", iz + izAdd);
      if (ind) {
        printf("no histogram dip could be found\n");
      } else {
        numBelow = 0;
        for (ind = 0; ind < numSample; ind++)
          if (sample[ind] < histDip)
            numBelow++;
        printf("peaks at %.5g and %.5g  dip at %.5g  fraction below dip = "
               "%.4f\n", peakBelow, peakAbove, histDip, (float)numBelow / numSample);
      }
    }
    sliceFree(slice);
  }
  return 0;
}

/*
 * Common routine for defect processing before output
 */
int correctDefects(Islice *slice, int nxFull, int nyFull,  ClipOptions *opt)
{
  int top, left, bottom, right, binning = 1;
  static bool firstTime = true;

  if (sliceModeIfReal(slice->mode) < 0) {
    show_error("clip with defect correction - The output slice mode must be byte, "
               "integer or floating point");
    return -1;
  }

  if (CorDefSetupToCorrect(nxFull, nyFull, opt->defects, opt->camSizeX, opt->camSizeY,
                           opt->scaleDefects, opt->binning, binning, 
                           firstTime ? "-B" : NULL)) {
    show_error("clip with defect correction - Image size is more than twice the size "
               "stored in the defect list");
      return -1;
  }
  firstTime = false;

  // Compute the coordinates for the defect routine
  left = (opt->camSizeX / binning - nxFull) / 2 + (int)opt->cx - opt->ix / 2;
  right = left + opt->ix;
  top = (opt->camSizeY / binning - nyFull) / 2 + (int)opt->cy - opt->iy / 2;
  bottom = top + opt->iy;
  if (left < 0 || top < 0 || right > opt->camSizeX / binning || 
      bottom > opt->camSizeY / binning) {
    show_error("clip with defect correction - The size and centering options "
               "select an area outside the camera field");
    return -1;
  }

  CorDefCorrectDefects(&opt->defects, slice->data.b, slice->mode, binning, top, left,

                       bottom, right);
  return 0;
}

int write_vol(Islice **vol, MrcHeader *hout)
{
  int k;

  for (k = 0; k < hout->nz; k++){
    if (mrc_write_slice((void *)vol[k]->data.b, hout->fp, hout, k, 'z'))
      return -1;
    sliceMMM(vol[k]);
    if (!k){
      hout->amin = vol[k]->min;
      hout->amax = vol[k]->max;
      hout->amean = vol[k]->mean;
    }else{
      if (vol[k]->min < hout->amin)
        hout->amin = vol[k]->min;
      if (vol[k]->max > hout->amax)
        hout->amax = vol[k]->max;
      hout->amean += vol[k]->mean;
    }
  }
  hout->amean /= hout->nz;
  return mrc_head_write(hout->fp, hout);
}

int free_vol(Islice **vol, int z)
{
  int k;
  for (k = 0; k < z; k++){
    sliceFree(vol[k]);
  }
  free(vol);
  return(0);
}


/*
  clipProject(char **argv, int argc,
  MrcHeader *hin,
  MrcHeader *hout,
  ClipOptions *opt)
  {

  char command[1024];
  char axis = 'Y';
  float scale_add = 0, scale_mult = 1;
  int x1,x2,y1,y2,z1,z2;
  float start,end,inc;
  int width;

  sprintf(command, "echo '%s\n%s\n%d,%d,%d,%d%d,%d\n%c\n%g,%g,%g\n%d\n%d\n%d,%d\n%g\n' | xyzproj",
  argv[argc-2], argv[argc-1],
  x1,x2,y1,y2,z1,z2,
  axis,
  start, end, inc,
  width,
  opt->mode,
  scale_add, scale_mult,
  opt->pad);
  #ifndef __vms
  system(command);
  #endif

  }
*/
