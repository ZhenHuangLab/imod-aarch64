/*  iirawimage.cpp - Raw image reading module; opens RawImageForm for info
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2016 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 * $Id$
 */

#include <qlabel.h>
#include <qapplication.h>
#include <QDesktopWidget>
#include <qdatetime.h>
#include "b3dutil.h"
#include "imod.h"
#include "preferences.h"
#include "iirawimage.h"
#include "form_rawimage.h"

// Resident info structure
static RawImageInfo info = {0, 64, 64, 1, 0, 0, 0., 255., 1, 0, 0, 0, 0.};

/*
 * Routines to modify the structure from command line
 */
void iiRawSetSize(int nx, int ny, int nz)
{
  info.nx = B3DMAX(1, nx);
  info.ny = B3DMAX(1, ny);
  info.nz = B3DMAX(1, nz);
  info.allMatch = 1;
}

int iiRawSetMode(int mode)
{
  switch (mode) {
  case -1:
    info.type = RAW_MODE_SBYTE;
    break;
  case MRC_MODE_BYTE:
    info.type = RAW_MODE_BYTE;
    break;
  case MRC_MODE_SHORT:
    info.type = RAW_MODE_SHORT;
    break;
  case MRC_MODE_USHORT:
    info.type = RAW_MODE_USHORT;
    break;
  case MRC_MODE_FLOAT:
    info.type = RAW_MODE_FLOAT;
    break;
  case MRC_MODE_COMPLEX_FLOAT:
    info.type = RAW_MODE_COMPLEX_FLOAT;
    break;
  case MRC_MODE_RGB:
    info.type = RAW_MODE_RGB;
    break;
  default:
    return 1;
  }
  return 0;
}

void iiRawSetHeaderSize(int size)
{
  info.headerSize = B3DMAX(0, size);
}

void iiRawSetSwap()
{
  info.swapBytes = 1;
}

void iiRawSetInverted()
{
  info.yInverted = 1;
}

void iiRawSetScale(float smin, float smax)
{
  info.amin = smin;
  info.amax = smax;
  info.scanMinMax = 0;
}

/*
 * The routine to check and setup a file
 */
int iiRawCheck(ImodImageFile *inFile)
{
  int ind, err;
  QString str;

  if (!inFile)
    return IIERR_BAD_CALL;
  if (!inFile->fp)
    return IIERR_BAD_CALL;

  str = inFile->filename;
  ind = str.lastIndexOf('/');
  if (ind < 0)
    ind = str.lastIndexOf('\\');
  if (ind >= 0)
    str = str.right(str.length() - 1 - ind);

  // Unless the all match flag has been set, get the form to set info
  if (!info.allMatch) {
    RawImageForm *form = new RawImageForm(NULL, true, Qt::Window);
    form->setWindowIcon(*(App->iconPixmap));
    form->load(str, &info);
    if (form->exec() == QDialog::Rejected)
      return IIERR_NOT_FORMAT;
    form->unload(&info);
    delete form;
  }

  if ((err = iiSetupRawHeaders(inFile, &info)))
    return err;

  return (iiRawScan(inFile));
}

/*
 * To scan a file for min and max or mean and SD if necessary: not just for raw images
 */
int iiRawScan(ImodImageFile *inFile)
{
  Islice slice;
  int z, ind, csize, dsize;
  MrcHeader *hdr = (MrcHeader *)inFile->header;
  FILE *tempFP;
  MrcHeader tempHdr;
  QString str;
  QLabel *splash = NULL;
  float amin, amax, fval;
  short int sval;
  unsigned short usval;
  bool doScan = info.scanMinMax;
  bool modeIsReal, doMeanSD = false;
  int yborder, xborder, linesToScan, skipLines, iy, typeSMSD, fullSkip, secSkip, remSkip;
  float bufPixels, sampleFrac, sampMean, sampSD, expand, pow10;
  double totPixels = 0., totSum = 0., totSumSq = 0.;
  unsigned char *buffer = NULL;
  unsigned char **bufPtrs = NULL;
  short *sdata;
  unsigned short *usdata;
  float *fdata;
  float borderFrac = 0.025;  // Leave this border except for full scan
  float subsetFrac = 0.1;    // Use this subset in sample scans
  float randFrac = 0.1;      // Vary the interval by this amount +/-
  float expandMinMaxFrac = 0.1;  // Expand estimated min/max by this much sometimes
  float targetSample = 10000.;   // Sample size in sampleMeanSD
  float subsetBytes = 1.e6;      // Size of subsets to load in bytes
  IloadInfo li;
  static int seed = 123456;      // So multiple loads are reproducible

  if (!inFile)
    return -1;
  if (!inFile->fp || !hdr)
    return 1;

  str = inFile->filename;
  ind = str.lastIndexOf('/');
  if (ind < 0)
    ind = str.lastIndexOf('\\');
  if (ind >= 0)
    str = str.right(str.length() - 1 - ind);

  amin = info.amin;
  amax = info.amax;
  mrc_init_li(&li, NULL);
  if (seed)
    srand(seed);
  seed = 0;

  // Don't bother to scan bytes, 1:1 scaling is fine
  if ((hdr->mode == MRC_MODE_BYTE || hdr->mode == MRC_MODE_RGB) && info.scanMinMax) {
    amin = 0;
    amax = 255;
    doScan = false;
  }

  // Just load as 16-bit if the preference is to do so
  if ((hdr->mode == MRC_MODE_SHORT || hdr->mode == MRC_MODE_USHORT) && 
      ImodPrefs->loadIntIfEstimate()) {
    App->cvi->switchToUshort = 1;
    iiDefaultMinMaxMean(inFile->type, &amin, &amax, &fval);
    doScan = false;
    imodTrace('r', "skipping scan, loading ints as 16-bit");
  }

  // Scan through file to find min and max or mean and SD if flag set
  if (doScan) {
    mrc_getdcsize(hdr->mode, &dsize, &csize);
    imodTrace('r', "Scan type %d  nxyz %d %d %d", App->cvi->scaleScanType, hdr->nx, 
              hdr->ny, hdr->nz);

    // Set up for subsets
    if (App->cvi->scaleScanType) {
      yborder = borderFrac * hdr->ny;
      linesToScan = subsetBytes / (hdr->nx * dsize * csize);
      B3DCLAMP(linesToScan, 1, hdr->ny - 2 * yborder);
      skipLines = linesToScan / subsetFrac;
      li.ymin = yborder;
      xborder = borderFrac * hdr->nx;
    } else {

      // Or full scan
      linesToScan = hdr->ny;
      skipLines = linesToScan;
      li.ymin = 0;
      yborder = 0;
      xborder = 0;
    }
    z = 0;
    li.xmin = 0;
    li.xmax = hdr->nx - 1;
    buffer = B3DMALLOC(unsigned char, hdr->nx * dsize * csize * linesToScan);
    if (!buffer) {
      b3dError(stderr, "ERROR allocating buffer for scanning\n");
      return IIERR_IO_ERROR;
    }

    // Set up for mean/SD
    modeIsReal = hdr->mode == MRC_MODE_SHORT || hdr->mode == MRC_MODE_USHORT ||
      hdr->mode == MRC_MODE_FLOAT;
    doMeanSD = modeIsReal && App->cvi->scaleScanType > 1;
    if (doMeanSD) {
      bufPtrs = makeLinePointers(buffer, hdr->nx, linesToScan, dsize * csize);
      if (!bufPtrs) {
        b3dError(stderr, "ERROR allocating line pointers for scanning\n");
        free(buffer);
        return IIERR_IO_ERROR;
      }
    }

    // If estimating, set flag to switch to integer loading
    if (App->cvi->scaleScanType && ImodPrefs->loadIntIfEstimate())
      App->cvi->switchToUshort = 1;

    // Show splash label if > 1 MB
    if ((double)hdr->nx * hdr->ny * hdr->nz * dsize * csize * 
        B3DCHOICE(App->cvi->scaleScanType, subsetFrac, 1.) > 1.e6) {
      splash = new QLabel("Scanning " + str + 
                          B3DCHOICE(doMeanSD, " for min/max", " for mean/SD"), NULL);
      qApp->flush();
      QSize hint = splash->sizeHint();
      splash->move(QApplication::desktop()->width() / 2 - hint.width() / 2, 
                   QApplication::desktop()->height() / 2 - hint.height() / 2);
      splash->show();
      qApp->flush();
#if QT_VERSION < 0x050000
      qApp->syncX();
#endif
      qApp->processEvents();
    }

    // Loop on slices.  Believe it or not, it's not that much faster to
    // do the min/max here than in sliceMMM
    amin = 1.e37;
    amax = -amin;
    while (z < hdr->nz) {
      li.ymax = li.ymin + linesToScan - 1;
      if ((iy = mrcReadZ(hdr, &li, buffer, z)) != 0) {
        free(buffer);
        B3DFREE(bufPtrs);
        return IIERR_IO_ERROR;
      }
      if (modeIsReal) {
        if (!doMeanSD) {

          // Get min and max for real modes
          for (iy = 0; iy < linesToScan; iy++) {
            switch (hdr->mode) {
            case MRC_MODE_SHORT:
              sdata = (short *)buffer + iy * hdr->nx + xborder;
              for (ind = xborder; ind < hdr->nx - xborder; ind++) {
                sval = sdata[ind];
                ACCUM_MIN(amin, sval);
                ACCUM_MAX(amax, sval);
              }
              break;

            case MRC_MODE_USHORT:
              usdata = (unsigned short *)buffer + iy * hdr->nx + xborder;
              for (ind = xborder; ind < hdr->nx - xborder; ind++) {
                usval = usdata[ind];
                ACCUM_MIN(amin, usval);
                ACCUM_MAX(amax, usval);
              }
              break;

            case MRC_MODE_FLOAT:
              fdata = (float *)buffer + iy * hdr->nx + xborder;
              for (ind = xborder; ind < hdr->nx - xborder; ind++) {
                fval = fdata[ind];
                ACCUM_MIN(amin, fval);
                ACCUM_MAX(amax, fval);
              }
              break;
              
            }
          }
        } else {

          // Get mean and SD and accumulate total sum and sum of squares
          bufPixels = (hdr->nx - 2 * xborder) * linesToScan;
          sampleFrac = B3DMIN(1., targetSample / bufPixels);
          typeSMSD = 6;
          if (hdr->mode != MRC_MODE_FLOAT)
            typeSMSD = B3DCHOICE(hdr->mode == MRC_MODE_SHORT, 3, 2);
          sampleMeanSD(bufPtrs, typeSMSD, hdr->nx, linesToScan, sampleFrac, xborder, 0,
                       hdr->nx - 2 * xborder, linesToScan, &sampMean, &sampSD);
          bufPixels *= sampleFrac;
          totPixels += bufPixels;
          totSum += sampMean * bufPixels;
          totSumSq += sampSD * sampSD * (bufPixels - 1) + sampMean * sampMean * bufPixels;
        }
      } else {
        sliceInit(&slice, hdr->nx, linesToScan, hdr->mode, buffer);
        sliceMMM(&slice);
        amin = B3DMIN(amin, slice.min);
        amax = B3DMAX(amax, slice.max);
      }

      // Advance to next block to read
      fullSkip = linesToScan;
      if (App->cvi->scaleScanType)
        fullSkip = skipLines * (1. + randFrac *((2. * rand()) / RAND_MAX - 1.));
      secSkip = fullSkip / hdr->ny;
      remSkip = fullSkip % hdr->ny;
      z += secSkip + (li.ymin + remSkip) / hdr->ny;
      li.ymin = (li.ymin + remSkip) % hdr->ny;

      // Round onto nearest full section if it crosses a section
      if (li.ymin < yborder)
        li.ymin = yborder;
      if (li.ymin + linesToScan > hdr->ny - yborder) {
        if (hdr->ny - yborder - li.ymin > linesToScan / 2) {
          li.ymin = hdr->ny - yborder - linesToScan;
        } else {
          li.ymin = yborder;
          z++;
        }
      }
    }

    // When done, process result for mean/SD
    if (splash)
      delete splash;
    if (doMeanSD) {
      inFile->amean = hdr->amean = totSum / totPixels;
      fval = (totSumSq - totPixels * hdr->amean * hdr->amean) / (totPixels - 1.);
      inFile->rms = hdr->rms = sqrt(B3DMAX(fval, 0.));
      free(bufPtrs);

      // Make up invalid min and max
      pow10 = pow(10., floor(log10(fabs(hdr->amean) + 1.)));
      inFile->smin = inFile->amin = hdr->amin = pow10 * (floor(hdr->amean / pow10) - 1.);
      inFile->smax = inFile->amax = hdr->amax = pow10 * (floor(hdr->amean / pow10) - 2.);
    } 
    expand = expandMinMaxFrac * (amax - amin);

    // Store in file if requested
    if (App->cvi->storeScanInMRC && inFile->file == IIFILE_MRC) {
      tempFP = fopen(inFile->filename, "rb+");
      if (tempFP && !mrc_head_read(tempFP, &tempHdr)) {
        if (doMeanSD) {
          tempHdr.amin = hdr->amin;
          tempHdr.amax = hdr->amax;
          tempHdr.amean = hdr->amean;
          tempHdr.rms = hdr->rms;
          imodTrace('r', "Setting min/max %f %f mean/sd %f %f in header", 
                    tempHdr.amin, tempHdr.amax, tempHdr.amean, tempHdr.rms );
        } else {
          tempHdr.amin = amin;
          tempHdr.amax = amax;
          if (App->cvi->scaleScanType) {
            tempHdr.amin -= expand;
            tempHdr.amax += expand;
          }

          // the mean WAS invalid, better make it look that way now
          if (hdr->amean < B3DMIN(hdr->amin, hdr->amax) || 
              (hdr->amean == 0. && hdr->amin == 0. || hdr->amax == 0.)) {
            pow10 = pow(10., floor(log10(fabs(tempHdr.amin) + 1.)));
            tempHdr.amean = pow10 * (floor(tempHdr.amin / pow10) - 1.);
          }
          imodTrace('r', "Setting min/max %f %f in header", tempHdr.amin, tempHdr.amax);
        }
        if (mrc_head_write(tempFP, &tempHdr))
          imodTrace('r', "Head write error %s", b3dGetError());
        fclose(tempFP);
      }
    }

    // Expand the limits if it is an estimate and we are loading 16 bits
    if (!doMeanSD) {
      if (App->cvi->scaleScanType && 
          (App->cvi->switchToUshort || App->cvi->rawImageStore == MRC_MODE_USHORT)) {
        amin -= expand;
        amax += expand;
      }
      hdr->amin = amin;
      hdr->amax = amax;
    }
    free(buffer);
  }

  if (!doMeanSD) {
    inFile->smin = inFile->amin = hdr->amin = amin;
    inFile->smax = inFile->amax = hdr->amax = amax;
    inFile->amean = hdr->amean = (amax + amin) / 2.;
  }

  return 0;
}
