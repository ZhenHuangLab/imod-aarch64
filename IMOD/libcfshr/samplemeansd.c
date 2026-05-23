/* 
 * samplemeansd.c : estimate mean and Sd of image from sample of pixels
 *
 * $Id$
 */

#include "imodconfig.h"
#include "b3dutil.h"

#define BYTE 0
#define SIGNED_BYTE 1
#define UNSIGNED_SHORT 2
#define SIGNED_SHORT 3
#define FLOAT 6
#define INTEGER 7
#define RGB   8
#define RGBA  9

#ifdef F77FUNCAP
#define samplemeansd SAMPLEMEANSD
#define sampleminmaxmeansd SAMPLEMINMAXMEANSD
#define typeforsamplemean TYPEFORSAMPLEMEAN
#else
#define samplemeansd samplemeansd_
#define sampleminmaxmeansd sampleminmaxmeansd_
#define typeforsamplemean typeforsamplemean_
#endif



#define ROW_SUM(ptr, tval, ts, cst, tsq, mn, mx)        \
  if (sd && amin) {                                                  \
    for (i = ixStart + xstart; i < ixStart + nxUse; i += dxSample) { \
      tval = ptr[j][i];                                              \
      ts += tval;                                                    \
      tsq += (cst)tval * tval;                                       \
      ACCUM_MIN(mn, tval);                                           \
      ACCUM_MAX(mx, tval);                                           \
    }                                                                \
  } else if (sd) {                                                    \
    for (i = ixStart + xstart; i < ixStart + nxUse; i += dxSample) {  \
      tval = ptr[j][i];                                          \
      ts += tval;                                                     \
      tsq += (cst)tval * tval;                                        \
    }                                                                 \
  } else if (amin) {                                                  \
    for (i = ixStart + xstart; i < ixStart + nxUse; i += dxSample) {  \
      tval = ptr[j][i];                                          \
      ts += tval;                                                     \
      ACCUM_MIN(mn, tval);                                            \
      ACCUM_MAX(mx, tval);                                            \
    }                                                                 \
  } else {                                                            \
    for (i = ixStart + xstart; i < ixStart + nxUse; i += dxSample) {  \
      ts += (cst)ptr[j][i];                                           \
    }                                                                 \
  }


/*!
 * Estimates mean and, optionally, the SD and the minimum and maximum values from a 
 * sample of an image. Returns nonzero for errors.
 * ^ [image] = array of pointers to each line of data 
 * ^ [type] = data type, 0 for bytes, 2 for unsigned shorts, 3 for signed shorts, 6 for 
 * floats, 7 for 4-byte integers, 8 for RGB triples of bytes or 9 for RGBA bytes;
 * RGB values are weighted with NTSC weighting
 * ^ [nx], [ny] = X and Y dimensions of image
 * ^ [sample] = fraction of pixels to sample
 * ^ [ixStart], [iyStart] = starting X and Y index to include
 * ^ [nxUse], [nyUse] = number of pixels in X and Y to include
 * ^ [mean] = returned value of mean
 * ^ [sd] = returned value of SD; call with NULL to skip the SD computation
 * ^ [amin], [amax] = returned values of estimate minimum and maximum; call with both 
 * NULL to skip the min and max.
 */
int sampleMinMaxMeanSD(unsigned char **image, int type, int nx, int ny, 
                       float sample, int ixStart, int iyStart, int nxUse, int nyUse,
                       float *mean, float *sd, float *amin, float *amax)
{
  int nSample;

  /* pointers for data of different types */
  char **bytep;
  unsigned char **ubytep;
  unsigned short int **ushortp;
  short int **shortp;
  float **floatp;
  int **intp;

  int ixUse, iyUse, dxSample, dySample, nySample;
  int i, j, xstart, nchan = type == RGBA ? 4 : 3;
  int val;
  unsigned int uval;
  double tsum, sum, sumsq, nPixUse, tsumsq;
  float fval, tmin, tmax, fracY;
  int nsum, itsum, itsumsq, itmin, itmax;
  
  if (!image)
    return (-1);
  if ((amin && !amax) || (amax && !amin) || (!mean))
    return (-1);

  /* get the number of points to sample */
  nPixUse = ((double)nxUse) * nyUse;
  nSample = (int)(sample * nPixUse);
  if (nxUse < 2 || nyUse < 2 || nSample < 5)
    return(1);

  /* If it all of them, just set up to do all */
  if (nSample >= nPixUse)  {
    nSample = (int)nPixUse;
    dxSample = 1;
    dySample = 1;
  } else {

    /* Otherwise sample on near-equal X/Y grid: take sample fraction in one dimension,
       get interval of sampling in Y from that, which gives number of samples in Y, and
       use that to get the sampling interval in X */
    fracY = sqrt(sample);
    dySample = B3DNINT(1. / fracY);
    B3DCLAMP(dySample, 1, nyUse / 4);
    nySample = nyUse / dySample;
    dxSample = B3DNINT(nxUse / (float)B3DMAX(1., nSample / (float)nySample));
    B3DCLAMP(dxSample, 1, nxUse / 4);
  }

  /* get pointer based on type of data */
  switch (type) {
  case BYTE :
  case RGBA :
  case RGB :
    ubytep = image;
    break;

  case SIGNED_BYTE :
    bytep = (char **) image;
    break;

  case SIGNED_SHORT :
    shortp = (short int **)image;
    break;

  case UNSIGNED_SHORT :
    ushortp = (unsigned short int **)image;
    break;

  case FLOAT :
    floatp = (float **)image;
    break;
  case INTEGER :
    intp = (int **)image;
    break;
  default :
    return(2);
  }

  /* Setup and loop on lines */
  sum = 0.;
  sumsq = 0.;
  nsum = 0;
  tmin = 1.e37;
  tmax = -tmin;
  for (j = iyStart; j < iyStart + nyUse; j += dySample) {
    tsum = 0.;
    tsumsq = 0.;
    itsum = 0;
    itsumsq = 0;
    itmin = 2147000000;
    itmax = -itmin;

    /* Adjust start so that columns are not sampled at least */
    xstart = (j % 17) % dxSample;
    nsum += ((nxUse - xstart) + dxSample - 1) / dxSample;

    /* For each type, loop in X for the kind of data requested */
    switch (type) {
    case BYTE:
      ROW_SUM(ubytep, val, itsum, int, itsumsq, itmin, itmax);
      ACCUM_MIN(tmin, itmin);
      ACCUM_MAX(tmax, itmax);
      tsum = itsum;
      tsumsq = itsumsq;
      break;

    case RGBA:
    case RGB:
      if (sd && amin) {
        for (i = ixStart + xstart; i < ixStart + nxUse; i += dxSample) {
          fval = 0.3f * ubytep[j][nchan*i] + 0.59f * ubytep[j][nchan*i+1] + 
            0.11f * ubytep[j][nchan*i+2];
          tsum += fval;
          tsumsq += fval * fval;
          ACCUM_MIN(tmin, fval);
          ACCUM_MAX(tmax, fval);
        }
      } else if (sd) {
        for (i = ixStart + xstart; i < ixStart + nxUse; i += dxSample) {
          fval = 0.3f * ubytep[j][nchan*i] + 0.59f * ubytep[j][nchan*i+1] + 
            0.11f * ubytep[j][nchan*i+2];
          tsum += fval;
          tsumsq += fval * fval;
        }
      } else if (amin) {
        for (i = ixStart + xstart; i < ixStart + nxUse; i += dxSample) {
          fval = 0.3f * ubytep[j][nchan*i] + 0.59f * ubytep[j][nchan*i+1] + 
            0.11f * ubytep[j][nchan*i+2];
          tsum += fval;
          ACCUM_MIN(tmin, fval);
          ACCUM_MAX(tmax, fval);
        }
      } else {
        for (i = ixStart + xstart; i < ixStart + nxUse; i += dxSample)
          tsum += 0.3f * ubytep[j][nchan*i] + 0.59f * ubytep[j][nchan*i+1] + 
            0.11f * ubytep[j][nchan*i+2];
      }
      break;

    case SIGNED_BYTE :
      ROW_SUM(bytep, val, itsum, int, itsumsq, itmin, itmax);
      ACCUM_MIN(tmin, itmin);
      ACCUM_MAX(tmax, itmax);
      tsum = itsum;
      tsumsq = itsumsq;
      break;
      
    case SIGNED_SHORT :
      ROW_SUM(shortp, val, tsum, float, tsumsq, tmin, tmax);
      break;

    case UNSIGNED_SHORT :
      ROW_SUM(ushortp, val, tsum, float, tsumsq, tmin, tmax);
      break;

    case FLOAT :
      ROW_SUM(floatp, fval, tsum, float, tsumsq, tmin, tmax);
      break;

    case INTEGER :
      ROW_SUM(intp, fval, tsum, float, tsumsq, tmin, tmax);
      break;

    }
    sum += tsum;
    sumsq += tsumsq;
  }

  /* Finish up and compute/return whatever is requested */
  sum /= nsum;
  if (nsum > 1 && sd) {
    sumsq = (sumsq - nsum * sum * sum)/(nsum - 1.);
    if (sumsq < 0.0)
      sumsq = 0.0;
    sumsq = sqrt(sumsq);
  } else
    sumsq = 0.;

  *mean = (float)sum;
  if (sd)
    *sd = (float)sumsq;
  if (amin)
    *amin = tmin;
  if (amax)
    *amax = tmax;

  return(0);
}

/*!
 * Estimates mean only from a sample of an image by calling @@sampleMinMaxMeanSD@. 
 * Returns nonzero for errors.
 */
int sampleMeanOnly(unsigned char **image, int type, int nx, int ny, float sample,
                   int ixStart, int iyStart, int nxUse, int nyUse, float *mean)
{
  return sampleMinMaxMeanSD(image, type, nx, ny, sample, ixStart, iyStart, nxUse, nyUse,
                            mean, NULL, NULL, NULL);
}

/*!
 * Estimates mean and SD from a sample of an image by calling @@sampleMinMaxMeanSD@. 
 * Returns nonzero for errors.
 */
int sampleMeanSD(unsigned char **image, int type, int nx, int ny, float sample,
                 int ixStart, int iyStart, int nxUse, int nyUse, float *mean, float *sd)
{
  return sampleMinMaxMeanSD(image, type, nx, ny, sample, ixStart, iyStart, nxUse, nyUse,
                            mean, sd, NULL, NULL);
}

/*!
 * Estimates mean, minimum and maximum from a sample of an image by calling 
 * @@sampleMinMaxMeanSD@. Returns nonzero for errors.
 */
int sampleMinMaxMean(unsigned char **image, int type, int nx, int ny, 
                 float sample, int ixStart, int iyStart, int nxUse, int nyUse,
                     float *mean, float *amin, float *amax)
{
  return sampleMinMaxMeanSD(image, type, nx, ny, sample, ixStart, iyStart, nxUse, nyUse,
                            mean, NULL, amin, amax);
}

/*!
 * Fortran wrapper for @sampleMinMaxMeanSD with a floating point array in [image].  Set 
 * [which] to 1 for mean only, 2 for mean and SD, 3 for mean, min, and max, and 4 for all
 * 4 values.
 */
int sampleminmaxmeansd(float *image, int *nx, int *ny, 
                       float *sample, int *ixStart, int *iyStart, int *nxUse, int *nyUse,
                       int which, float *mean, float *sd, float *dmin, float *dmax)
{
  int i;
  unsigned char **lines;
  if (which < 1 || which > 4)
    return -2;
  lines = makeLinePointers(image, *nx, *ny, 4);
  if (!lines)
    return -1;
  if (which == 1)
    i = sampleMinMaxMeanSD(lines, FLOAT, *nx, *ny, *sample, *ixStart, *iyStart, *nxUse,
                           *nyUse, mean, NULL, NULL, NULL);
  else if (which == 2)
    i = sampleMinMaxMeanSD(lines, FLOAT, *nx, *ny, *sample, *ixStart, *iyStart, *nxUse,
                           *nyUse, mean, sd, NULL, NULL);
  else if (which == 3)
    i = sampleMinMaxMeanSD(lines, FLOAT, *nx, *ny, *sample, *ixStart, *iyStart, *nxUse,
                           *nyUse, mean, NULL, dmin, dmax);
  else
    i = sampleMinMaxMeanSD(lines, FLOAT, *nx, *ny, *sample, *ixStart, *iyStart, *nxUse,
                           *nyUse, mean, sd, dmin, dmax);
  free(lines);
  return i;
}

/*!
 * Fortran wrapper for @sampleMeanSD with a floating point array in [image]
 */
int samplemeansd(float *image, int *nx, int *ny, float *sample, int *ixStart,
                 int *iyStart, int *nxUse, int *nyUse, float *mean, float *sd,
                 float *dmin, float *dmax)
{
  return sampleminmaxmeansd(image, nx, ny, sample, ixStart, iyStart, nxUse, nyUse, 2,
                            mean, sd, dmin, dmax);
}

/*!
 * Returns the type needed for calling the sampleMean routines if [mrcMode] is one of the
 * MRC_MODE_... (or SLICE_MODE_...) values supported by the routines, otherwise returns 
 * -1.
 */
int typeForSampleMean(int mrcMode)
{
  switch (mrcMode) {
  case MRC_MODE_BYTE:
    return 0;
  case MRC_MODE_USHORT:
    return 2;
  case MRC_MODE_SHORT:
    return 3;
  case MRC_MODE_FLOAT:
    return 6;
  case MRC_MODE_RGB:
    return 8;
  default:
    return -1;
  }
}

/*! Fortran wrapper for @typeForSampleMean */
int typeforsamplemean(int *mrcMode)
{
  return typeForSampleMean(*mrcMode);
}
