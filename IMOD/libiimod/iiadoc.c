/*
 *    iiadoc.c    - specific routines for SerialEM idoc-type ImodImageFile's
 *
 *    Author:  David Mastronarde
 *
 *   Copyright (C) 1995-2020 by the Regents of the University of Colorado.
 *
 *  $Id$
 */

#include "b3dutil.h"
#include "iimage.h"
#include "autodoc.h"

#define IIADOC_IMAGE "Image"

static void adocClose(ImodImageFile *inFile);
static int adocReopen(ImodImageFile *inFile);
static int adocFillMrcHeader(ImodImageFile *inFile, MrcHeader *hdata);
static int readSectionFile(ImodImageFile *inFile, char *buf, int section, 
                           iiSectionFunc func);
static int adocReadSectionByte(ImodImageFile *inFile, char *buf, int inSection);
static int adocReadSectionUShort(ImodImageFile *inFile, char *buf, int inSection);
static int adocReadSectionFloat(ImodImageFile *inFile, char *buf, int inSection);
static int adocReadSection(ImodImageFile *inFile, char *buf, int inSection);

/*
 * Check for and open a JPEG file
 */
int iiADOCCheck(ImodImageFile *inFile)
{
  int montage, numSect, sectType, ind, saveStore;
  float tmin, tmax, tmean;
  FILE *fp;
  if (!inFile) 
    return IIERR_BAD_CALL;
  fp = inFile->fp;
  if (!fp)
    return IIERR_BAD_CALL;
  /* Close file now, but reopen it if there is a TIFF failure */
  fclose(fp);
  inFile->fp = NULL;

  saveStore = b3dGetStoreError();
  b3dSetStoreError(1);
  inFile->adocIndex = AdocOpenImageMetadata(inFile->filename, 0, &montage, &numSect,
                                            &sectType);
  
  if (inFile->adocIndex >= 0 && sectType != 2) {
    AdocClear(inFile->adocIndex);
    inFile->adocIndex = -1;
  }
  b3dSetStoreError(saveStore);
  if (inFile->adocIndex >= 0) {
    if (AdocGetTwoIntegers(ADOC_GLOBAL_NAME, 0, "ImageSize", &inFile->nx, &inFile->ny)
        || AdocGetInteger(ADOC_GLOBAL_NAME, 0, "DataMode", &inFile->mode)) {
      AdocClear(inFile->adocIndex);
      inFile->adocIndex = -1;
    }
    inFile->nz = numSect;
  }

  // Need to reopen unconditionally to have an fp for iiFOpen etc
  inFile->fp = fopen(inFile->filename, inFile->fmode);
  if (!inFile->fp) {
    b3dError(stderr, "ERROR: iiADOCCheck - reopening file after reading as adoc");
    AdocClear(inFile->adocIndex);
    return IIERR_IO_ERROR;
  }
  if (inFile->adocIndex < 0) {
    return IIERR_NOT_FORMAT;
  }
  inFile->file = IIFILE_ADOC;
  iiMRCmodeToFormatType(inFile, inFile->mode, 0);
  inFile->hasPieceCoords = montage;

  /* Get the min/max from all the sections */
  iiDefaultMinMaxMean(inFile->mode, &inFile->amin, &inFile->amax, &inFile->amean);
  for (ind = 0; ind < numSect; ind++) {
    if (!AdocGetThreeFloats(IIADOC_IMAGE, ind, "MinMaxMean", &tmin, &tmax, &tmean)) {
      if (!ind) {
        inFile->amin = tmin;
        inFile->amax = tmax;
      } else {
        ACCUM_MIN(inFile->amin, tmin);
        ACCUM_MAX(inFile->amax, tmax);
      }
    }
  }
  inFile->amean = (inFile->amax + inFile->amin) / 2.;
  inFile->smin = inFile->amin;
  inFile->smax = inFile->amax;
  if (!AdocGetFloat(ADOC_GLOBAL_NAME, 0, "PixelSpacing", &inFile->xscale)) {
    inFile->yscale = inFile->xscale = inFile->xscale;
  }
  
  inFile->fillMrcHeader = adocFillMrcHeader;
  inFile->readSection = adocReadSection;
  inFile->readSectionUShort = adocReadSectionUShort;
  inFile->readSectionByte = adocReadSectionByte;
  inFile->readSectionFloat = adocReadSectionFloat;
  inFile->cleanUp = adocClose;
  inFile->close = adocClose;
  inFile->reopen = adocReopen;
  return 0;
}

static void adocClose(ImodImageFile *inFile)
{
  if (inFile && inFile->adocIndex >= 0)
    AdocClear(inFile->adocIndex);
  if (inFile->fp)
    fclose(inFile->fp);
  inFile->fp = NULL;
  inFile->adocIndex = -1;
}

static int adocReopen(ImodImageFile *inFile)
{
  inFile->adocIndex = AdocRead(inFile->filename);
  inFile->fp = fopen(inFile->filename, inFile->fmode);
  if (!inFile->fp) {
    b3dError(stderr, "ERROR: adocReopen - reopening file");
    AdocClear(inFile->adocIndex);
  }
  return (inFile->adocIndex < 0 || !inFile->fp) ? 1 : 0;
}

static int adocFillMrcHeader(ImodImageFile *inFile, MrcHeader *hdata)
{
  int ind;
  char *label;
  iiSimpleFillMrcHeader(inFile, hdata);
  mrc_set_scale(hdata, (double)inFile->xscale, (double)inFile->yscale,
                (double)inFile->zscale);
  hdata->nlabl = AdocGetNumberOfSections("T");
  for (ind = 0; ind < hdata->nlabl; ind++) {
    if (AdocGetSectionName("T", ind, &label))
      return 1;
    strncpy(hdata->labels[ind], label, MRC_LABEL_SIZE);
    free(label);
    fixTitlePadding(hdata->labels[ind]);
  }
  return 0;
}

/*
 * Common function to open a file for the given section, read it, and close it
 */
static int readSectionFile(ImodImageFile *inFile, char *buf, int section,
                           iiSectionFunc func)
{
  char *filename, *slash, *backSlash, *useName;
  ImodImageFile *sectFile;
  int err, slashInd = -1, bsInd = -1, fullLen;
  if (AdocGetSectionName(IIADOC_IMAGE, section, &filename)) {
    b3dError(stderr, "ERROR: iiadoc - Getting filename for section %d", section);
    return 1;
  }

  /* Look for directory in front of idoc filename */
  slash = strrchr(inFile->filename, '/');
  backSlash = strrchr(inFile->filename, '\\');
  if (slash)
    slashInd = slash - inFile->filename;
  if (backSlash)
    bsInd = backSlash - inFile->filename;
  ACCUM_MAX(slashInd, bsInd);
  useName = filename;
  
  /* Compose full name */
  if (slashInd >= 0) {
    fullLen = slashInd + strlen(filename) + 4;
    useName = (char *)malloc(fullLen);
    if (!useName) {
      b3dError(stderr, "ERROR: iiadoc - Allocating memory for filename\n");
      return 1;
    }
    strncpy(useName, inFile->filename, slashInd + 1);
    strcpy(useName + slashInd + 1, filename);
  }

  sectFile = iiOpen(useName, "rb");
  free(filename);
  if (slashInd >= 0)
    free(useName);
  if (!sectFile) {
    b3dError(stderr, "ERROR: iiadoc - Cannot open file for section %d\n", section);
    return 1;
  }
  if (sectFile->nx != inFile->nx || sectFile->ny != inFile->ny ||
      sectFile->mode != inFile->mode) {
    b3dError(stderr, "ERROR: iiadoc - File for section %d has wrong size or mode "
             "(section: " "%d x %d mode %d, file: %d x %d mode %d)\n", section,
             sectFile->nx, sectFile->ny, sectFile->mode, inFile->nx, inFile->ny, 
             inFile->mode);
    iiDelete(sectFile);
    return 1;
  }

  /* Copy anything that might affect loading */
  sectFile->llx = inFile->llx;
  sectFile->urx = inFile->urx;
  sectFile->lly = inFile->lly;
  sectFile->ury = inFile->ury;
  sectFile->amin = inFile->amin;
  sectFile->amax = inFile->amax;
  sectFile->amean = inFile->amean;
  sectFile->smin = inFile->smin;
  sectFile->smax = inFile->smax;
  sectFile->slope = inFile->slope;
  sectFile->offset = inFile->offset;
  err = func(sectFile, buf, 0);
  iiDelete(sectFile);
  return err;
}

/*
 * Functions for the individual types of reads
 */
static int adocReadSectionByte(ImodImageFile *inFile, char *buf, int inSection)
{ 
  return readSectionFile(inFile, buf, inSection, iiReadSectionByte);
}

static int adocReadSectionUShort(ImodImageFile *inFile, char *buf, int inSection)
{ 
  return readSectionFile(inFile, buf, inSection, iiReadSectionUShort);
}

static int adocReadSectionFloat(ImodImageFile *inFile, char *buf, int inSection)
{ 
  return readSectionFile(inFile, buf, inSection, iiReadSectionFloat);
}

static int adocReadSection(ImodImageFile *inFile, char *buf, int inSection)
{
  return readSectionFile(inFile, buf, inSection, iiReadSection);
}
