/*
 * THIS FILE IS COPIED TO qttools/mrc2tif: EDIT ONLY THE MASTER IN 3dmod ***
 *
 * iiqimage.cpp  - Implements an ImodImageFile for any file type that QImage
 *                  can read.  Would have been part of libiimod, but then all
 *                  programs using libiimod would need Qt
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2014 by Boulder Laboratory for 3-Dimensional Electron
 *  Microscopy of Cells ("BL3DEMC") and the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 * $Id$
 */

#include <qimage.h>
#include <qstring.h>
#include "imodconfig.h"
#include "b3dutil.h"
#include "iimage.h"

// C linkage is required for these functions which are called from iimage
extern "C" {
  int qimageReadSectionByte(ImodImageFile *inFile, char *buf, int inSection);
  int qimageReadSectionFloat(ImodImageFile *inFile, char *buf, int inSection);
  int qimageReadSection(ImodImageFile *inFile, char *buf, int inSection);
  int qimageReopen(ImodImageFile *inFile);
  void qimageClose(ImodImageFile *inFile);
}

static int ReadSection(ImodImageFile *inFile, char *buf, int byte);

// Check the file for being a readable QImage and set up parameters
int iiQImageCheck(ImodImageFile *inFile)
{
  QImage *image;
  if (!inFile) 
    return IIERR_BAD_CALL;
  image = new QImage(QString(inFile->filename));
  if (image->isNull()) {
    delete image;
    return IIERR_NOT_FORMAT;
  }

  if (image->depth() < 8) {
    delete image;
    b3dError(NULL, "%s is a recognized file type but data type is not "
             "supported\n", inFile->filename);
    return IIERR_NO_SUPPORT;
  }

  if(inFile->fp)
    fclose(inFile->fp);
  inFile->nx = image->width();
  inFile->ny = image->height();
  inFile->nz = 1;
  inFile->file = IIFILE_QIMAGE;
  inFile->type   = IITYPE_UBYTE;
  inFile->amean  = 128;
  inFile->amax   = 255;
  inFile->smax   = 255;

  // Grayscale images have depth 8 and color tables that are gray
  if (image->depth() == 8 && image->isGrayscale()) {
    inFile->format = IIFORMAT_LUMINANCE;
    inFile->readSectionByte = qimageReadSectionByte;
    inFile->readSectionFloat = qimageReadSectionFloat;
    inFile->mode   = MRC_MODE_BYTE;
    inFile->readSection = qimageReadSection;
  } else {

    // Otherwise images are RGB and can be read as RGB or gray-scale
    inFile->format = IIFORMAT_RGB;
    inFile->mode   = MRC_MODE_RGB;
    inFile->readSection = qimageReadSection;
    inFile->readSectionByte = qimageReadSectionByte;
  }

  // Imitate TIFF for some of these behaviors - close file, assign image to
  // header but not fp here, assign to fp when reopen
  // Except that the fill header is not generic - for now, jpeg is.
  inFile->headerSize = 8;
  inFile->header = (char *)image;

  inFile->cleanUp = qimageClose;
  inFile->reopen = qimageReopen;
  inFile->close = qimageClose;
  inFile->fillMrcHeader = iiSimpleFillMrcHeader;

  return 0;
}

int qimageReadSectionByte(ImodImageFile *inFile, char *buf, int inSection)
{ 
  return(ReadSection(inFile, buf, 1));
}

int qimageReadSectionFloat(ImodImageFile *inFile, char *buf, int inSection)
{ 
  return(ReadSection(inFile, buf, 2));
}

int qimageReadSection(ImodImageFile *inFile, char *buf, int inSection)
{
  return(ReadSection(inFile, buf, 0));
}

int qimageReopen(ImodImageFile *inFile)
{
  QImage *image;
  image = new QImage(QString(inFile->filename));
  if (image->isNull()) {
    delete image;
    return 1;
  }
  inFile->headerSize = 8;
  inFile->header = (char *)image;    
  inFile->fp = (FILE *)image;    
  return 0;
}

void qimageClose(ImodImageFile *inFile)
{
  QImage *image = (QImage *)inFile->header;
  if (image)
    delete image;
  inFile->header = NULL;
  inFile->fp = NULL;
}

// Read an image as raw or byte data
static int ReadSection(ImodImageFile *inFile, char *buf, int byte)
{
  int ysize = inFile->ny;
  int i, pixel, y, maxind;
  float slope = inFile->slope;
  float offset = inFile->offset;
  bool directBytes;
  int outmin = 0;
  int outmax = 255;
  int xmin, xmax, ymin, ymax;
  unsigned char *obuf;
  float *fbuf;
  unsigned char *bdata;
  unsigned char map[256];
  QRgb *rgbdata;
  QRgb rgbval;
  QImage *image;
  QImage image2;
  unsigned char *map2 = NULL;
  QVector<QRgb> colorTable;
     
  if (inFile->axis == 2)
    return -1;
  if (byte > 1 && (inFile->format != IIFORMAT_LUMINANCE))
    return -1;
  if (!inFile->header && iiReopen(inFile))
    return -1;
  image = (QImage *)inFile->header;
  directBytes = !byte && image->depth() == 8 && image->isGrayscale();

  // Convert a 16-bpp image to 32
  if (image->depth() == 16) {
    image2 = image->convertToFormat(QImage::Format_RGB32);
    if (image2.isNull())
      return -1;
    image = &image2;
  }

  colorTable = image->colorTable();

  /* set the dimensions to read in */
  xmin   = inFile->llx;
  ymin   = inFile->lly;
  if (inFile->urx < 0)
    xmax = inFile->nx-1;
  else
    xmax = inFile->urx;
  if (inFile->ury < 0)
    ymax = inFile->ny-1;
  else
    ymax = inFile->ury;
    
  if (byte || directBytes) {

    // Get a map for scaling the bytes, then get composite map to go from
    // pixel indexes to scaled value;
    if (byte)
      map2 = get_byte_map(slope, offset, outmin, outmax, 0);
    maxind = B3DMAX(0, colorTable.size() - 1);
    for (i = 0; i < 256; i++) {
      if (maxind > 0)
        pixel = qRed(colorTable[B3DMIN(i, maxind)]);
      else
        pixel = i;
      map[i] = byte ? map2[pixel] : pixel;
    }
  }

  // Loop on the lines to get data
  obuf = (unsigned char *)buf;
  fbuf = (float *)buf;
  for (y = ymin; y <= ymax; y++) {
    bdata = image->scanLine(ysize - 1 - y);

    // Convert RGB to gray-scale bytes
    if (byte && inFile->format != IIFORMAT_LUMINANCE) {

      // For 8 bit color table, convert to gray scale
      if (image->depth() == 8) {
        for (i = xmin; i <= xmax; i++) {
          rgbval = colorTable[bdata[i]];
          *obuf++ = map2[(unsigned char)(0.3 * qRed(rgbval) + 0.59 * qGreen(rgbval) + 
                                         0.11 * qBlue(rgbval))];
        }

        // 32-bit: convert scan line pointer to rgb, get components
      } else {
        rgbdata = (QRgb *)bdata;
        for (i = xmin; i <= xmax; i++) {
          rgbval = rgbdata[i];
          *obuf++ = map2[(unsigned char)(0.3 * qRed(rgbval) + 0.59 * qGreen(rgbval) + 
                                         0.11 * qBlue(rgbval))];
        }
      }

      // Bytes: just map the index values
    } else if (byte == 1 || directBytes) {
      for (i = xmin; i <= xmax; i++)
        *obuf++ = map[bdata[i]];

    } else if (byte == 2) {
      for (i = xmin; i <= xmax; i++)
        *fbuf++ = map[bdata[i]];

      // 8 bit RGB: look up in color table
    } else if (image->depth() == 8) {
      for (i = xmin; i <= xmax; i++) {
        rgbval = colorTable[bdata[i]];
        *obuf++ = qRed(rgbval);
        *obuf++ = qGreen(rgbval);
        *obuf++ = qBlue(rgbval);
      }

      // 32-bit: convert scan line pointer to rgb, get components
    } else {
      rgbdata = (QRgb *)bdata;
      for (i = xmin; i <= xmax; i++) {
        rgbval = rgbdata[i];
        *obuf++ = qRed(rgbval);
        *obuf++ = qGreen(rgbval);
        *obuf++ = qBlue(rgbval);
      }
    }
  }

  return 0;
}
