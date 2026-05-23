/*
 *  iproc.cpp -- image processing for 3dmod.
 *
 *  Original author: James Kremer
 *  Revised by: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2019 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#include <qlabel.h>
#include <QStackedWidget>
#include <qlayout.h>
#include <qtooltip.h>
#include <qcombobox.h>
#include <qlistwidget.h>
#include <qspinbox.h>
#include <QDoubleSpinBox>
#include <qcheckbox.h>
#include <qpushbutton.h>
#include <qradiobutton.h>
#include <QButtonGroup>
#include <QGroupBox>
//Added by qt3to4:
#include <QHBoxLayout>
#include <QTimerEvent>
#include <QKeyEvent>
#include <QVBoxLayout>
#include <QCloseEvent>
#include "dia_qtutils.h"
#include "tooledit.h"
#include "multislider.h"
#include "imod.h"
#include "display.h"
#include "iproc.h"
#include "dockingdialog.h"
#include "sliceproc.h"
#include "xcorr.h"
#include "xzap.h"
#include "info_setup.h"
#include "info_cb.h"
#include "control.h"
#include "preferences.h"

/* internal functions. */
static void clearsec(ImodIProc *ip);
static void savesec(ImodIProc *ip);
static void cpdslice(Islice *sl, ImodIProc *ip);
static void copyAndDisplay();
static void imageToBuffer(ImodIProc *ip, unsigned char **image, unsigned char *buf);
static int savedToImage(ImodIProc *ip);
static int fillMedianVol(ImodIProc *ip, int zStart, int zEnd);
static void freeMedianVol(ImodIProc *ip, int zsize);
static void setSliceMinMax(bool actual);
static void freeArrays(ImodIProc *ip);
static void  setUnscaledK();
static QString modeChangeStr(const char *modeOpt);
static QString clipFFTtoRealStr();
static void cannotDoFFTStr(QString &str, const char *operation);

static void edge_cb();
static void mkedge_cb(IProcWindow *win, QWidget *parent, QVBoxLayout *layout);
static void thresh_cb();
static void mkthresh_cb(IProcWindow *win, QWidget *parent, QVBoxLayout *layout);
static void smooth_cb();
static void sharpen_cb();
static void mkFourFilt_cb(IProcWindow *win, QWidget *parent, QVBoxLayout *layout);
static void fourFilt_cb();
static void mkFFT_cb(IProcWindow *win, QWidget *parent, QVBoxLayout *layout);
static void fft_cb();
static void mkMedian_cb(IProcWindow *win, QWidget *parent, QVBoxLayout *layout);
static void mkSmooth_cb(IProcWindow *win, QWidget *parent, QVBoxLayout *layout);
static void median_cb();
static void mkAnisoDiff_cb(IProcWindow *win, QWidget *parent, QVBoxLayout *layout);
static void anisoDiff_cb();

#define NO_KERNEL_SIGMA 0.4f
#define KERNEL_MAXSIZE 7
#define FILTER_SLICER (procTable[sParam.procNum].cb == fourFilt_cb && sProc.filterSlicer)

// An enum to protect against button renumbering
enum {APPLY_BUT = 0, MORE_BUT, LESS_BUT, DO_SAME_BUT, TOGGLE_BUT, RESET_BUT, SAVE_BUT,
      LIST_BUT, DONE_BUT, HELP_BUT};

/* The table of entries and callbacks */
static ImodIProcData procTable[] = {
  {"FFT", fft_cb, mkFFT_cb, NULL, NULL},
  {"Fourier filter", fourFilt_cb, mkFourFilt_cb, NULL, NULL},
  {"smooth", smooth_cb, mkSmooth_cb, NULL, NULL},
  {"median", median_cb, mkMedian_cb, NULL, NULL},
  {"diffusion", anisoDiff_cb, mkAnisoDiff_cb, NULL, NULL},
  {"edge", edge_cb, mkedge_cb, NULL, NULL},
  {"sharpen", sharpen_cb, NULL, "Sharpen Edges.", NULL},
  {"threshold", thresh_cb, mkthresh_cb, NULL, NULL},
  {NULL, NULL, NULL, NULL, NULL}
};

/* Static variables for proc structure and a slice */
static ImodIProc sProc = {0,0,0,0,0,0,{0,0},0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                          0,0,0,0,0,0,0,0,0,0};
static IProcParam sParam = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
static Islice sSlice;
static QString sCommand;
static DockingDialog *sTopWin = NULL;

/*
 * CALLBACK FUNCTIONS FOR THE VARIOUS FILTERS
 */

// New rule 11/07/04: Set the desired output min and max before calling 
// routines in sliceproc or xcorr to either the data range with 
// setSliceMinMax(false) or the existing input range with  setSliceMinMax(true)

// Edge enhancement
static void edge_cb()
{
  ImodIProc *ip = &sProc;
  Islice *gs;

  switch (sParam.edge){
  case 0:
    setSliceMinMax(false);
    sliceByteEdgeSobel(&sSlice);

    // Three of these filters operate in clip by converting each slice to a byte
    // so there is no need for a mode, and we set the flags appropriately for byte output
    sCommand = "clip sobel";
    ip->outputMode = 0;
    ip->wasByte = 1;
    break;

  case 1:
    setSliceMinMax(false);
    sliceByteEdgePrewitt(&sSlice);
    sCommand = "clip prewitt";
    ip->outputMode = 0;
    ip->wasByte = 1;
    break;

  case 2:
    setSliceMinMax(false);
    sliceByteEdgeLaplacian(&sSlice);

    // Laplacian, smooth and sharpen do convolve a kernel with all channels but need
    // to produce mode 2 for floating point
    sCommand = "clip laplac" + clipFFTtoRealStr();
    break;
	  
  case 3:
    setSliceMinMax(false);
    sliceByteGraham(&sSlice);
    sCommand = "clip graham";
    ip->outputMode = 0;
    ip->wasByte = 1;
    break;

  case 4:
    gs = sliceGradient(&sSlice);
    if (!gs) return;
    cpdslice(gs, ip);
    sCommand = "clip gradient";

    // Some command just can't work on FFT data
    cannotDoFFTStr(sCommand, "clip gradient");
    break;

  default:
    break;
  }
}

// Threshold
static void thresh_cb()
{
  ImodIProc *ip = &sProc;
  int xysize, thresh, minv, maxv;
  unsigned char *idat, *last;
     
  setSliceMinMax(false);
  thresh = sParam.threshold;
     
  if (App->depth == 8){
    thresh = (int)
      ((((float)ip->vi->rampsize/256.0f)*thresh) + ip->vi->rampbase);
    minv = ip->vi->rampbase;
    maxv = ip->vi->rampsize + minv - 1;
  }else{
    minv = 0; maxv = 255;
  }

  xysize = ip->vi->xsize * ip->vi->ysize;
  idat = ip->iwork;
  for(last = idat + xysize; idat != last; idat++){
    if (*idat > thresh)
      *idat = maxv;
    else
      *idat = minv;
  }

  if (sParam.threshGrow)
    sliceByteGrow(&sSlice,  (int)sSlice.max);
  if (sParam.threshShrink)
    sliceByteShrink(&sSlice,  (int)sSlice.max);
  if (ip->dia->mParamStack.size() > 1)
    sCommand = "Cannot do thresholding in clip except as first process";
  else if (sParam.threshGrow || sParam.threshShrink)
    sCommand = "Cannot do thresholding with grow or shrink in clip";
  else
    sCommand.sprintf("clip threshold -t %g", sProc.fileThreshold);
}

// Smoothing
static void smooth_cb()
{
  ImodIProc *ip = &sProc;
  float kernel[KERNEL_MAXSIZE*KERNEL_MAXSIZE];
  unsigned char *planes[KERNEL_MAXSIZE];
  int dim, zStart, zEnd, ind;
  float useSigma;
  Islice *sout;
  sSlice.min = sSlice.max = 0.;
  if (sParam.rescaleSmooth)
    setSliceMinMax(true);
  if (sParam.kernelSigma > NO_KERNEL_SIGMA + 0.001 || sParam.smooth3D) {
    // Why was this one using:     sliceMMM(&sSlice);
    useSigma = sParam.kernelSigma > NO_KERNEL_SIGMA + 0.001 ? sParam.kernelSigma : 0.85f;
    scaledGaussianKernel(kernel, &dim, KERNEL_MAXSIZE, useSigma);
    if (sParam.smooth3D) {
      zStart = B3DMAX(0, ip->idataSec - dim / 2);
      zEnd = B3DMIN(ip->vi->zsize - 1, ip->idataSec + (dim - 1) / 2);
      if (fillMedianVol(ip, zStart, zEnd))
        return;
      for (ind = 0; ind < dim; ind++)
        planes[ind] = ip->medianVol.vol[ind]->data.b;
      if (volByteSmooth(planes, sSlice.xsize, sSlice.ysize, dim, useSigma, 1)) {
        freeMedianVol(ip, dim);
        return;
      }
      sliceScaleAndFree(ip->medianVol.vol[dim / 2], &sSlice);
      ip->medianVol.vol[dim / 2] = NULL;
      freeMedianVol(ip, dim);
      
    } else {
      sout = slice_mat_filter(&sSlice, kernel, dim);
      sliceScaleAndFree(sout, &sSlice);
      sCommand.sprintf("clip smooth -l %f%s", useSigma, LATIN1(clipFFTtoRealStr()));
      if (sParam.smooth3D)
        sCommand += " -n -1";
    }

  } else {
    sliceByteSmooth(&sSlice);
    sCommand = "clip smooth" + clipFFTtoRealStr();
  }
}

// Sharpening
static void sharpen_cb()
{
  setSliceMinMax(true);
  sliceByteSharpen(&sSlice);
  sCommand = "clip sharpen" + modeChangeStr("m");
}

// Fourier filter
static void fourFilt_cb()
{
  IProcParam *pp = &sParam;
  sCommand.sprintf("mtffilter -high %.3f -low %.3f,%.3f", pp->sigma1,  pp->radius2,
                   pp->sigma2);
  if (sProc.inputMode != 4 && pp->sigma1 > 0.)
    sCommand += modeChangeStr("mode");
  if (sProc.filterSlicer) {
    sProc.slicerFiltApplied = true;
    return;
  }
  setSliceMinMax(true);
  sliceFourierFilter(&sSlice, pp->sigma1, pp->sigma2, pp->radius1, pp->radius2);
}

// FFT
static void fft_cb()
{
  int ix0, ix1, iy0, iy1, nxuse, nyuse;
  ix0 = iy0 = 0;
  nxuse = sSlice.xsize;
  nyuse = sSlice.ysize;
  if (sParam.fftSubset)
    zapSubsetLimits(sProc.vi, ix0, iy0, nxuse, nyuse);
  ix1 = ix0 + nxuse - 1;
  iy1 = iy0 + nyuse - 1;
  setSliceMinMax(false);
  sProc.fftScale = sliceByteBinnedFFT(&sSlice, sParam.fftBinning, ix0, ix1, iy0, iy1,
                                     &sProc.fftXcen, &sProc.fftYcen);
  if (sParam.fftBinning > 1)
    sCommand = "Cannot do FFT with binning in one operation";
  else if (sParam.fftSubset)
    sCommand = "Cannot do FFT on subset in one operation";
  else
    sCommand = "clip fft -2d";
  sProc.outputMode = 4;
}

// Median filter
static void median_cb()
{
  unsigned char *to;
  unsigned char **from;
  int i, j, z;
  ImodIProc *ip = &sProc;
  int depth = sParam.median3D ? sParam.medianSize : 1;
  int zst = B3DMAX(0, ip->idataSec - depth / 2);
  int znd = B3DMIN(ip->vi->zsize - 1, ip->idataSec + (depth - 1) / 2);
  if (fillMedianVol(ip, zst, znd))
    return;

  sliceMedianFilter(&sSlice, &ip->medianVol, sParam.medianSize);

  // Clean up
  freeMedianVol(ip, depth);
  sCommand.sprintf("clip median -%dd -n %d", sParam.median3D ? 3 : 2,
                   sParam.medianSize);
  cannotDoFFTStr(sCommand, "clip median");
}

// Anisotropic diffusion
static void anisoDiff_cb()
{
  ImodIProc *ip = &sProc;

  // Get this memory only when needed because it is so big
  if (!ip->andfImage) {
    ip->andfImage = allocate2D_float(ip->vi->ysize + 2, ip->vi->xsize + 2);
    if (ip->andfImage) {
      ip->andfImage2 = allocate2D_float(ip->vi->ysize + 2, ip->vi->xsize + 2);
      if (!ip->andfImage2) {
        free(ip->andfImage[0]);
        free(ip->andfImage);
        ip->andfImage = NULL;
      }
    }
  }
  if (!ip->andfImage) {
    wprint("\aCould not get memory for diffusion\n");
    return;
  }
  sParam.andfK = ip->andfKEdit->text().toDouble();
  sParam.andfLambda = ip->andfLambdaEdit->text().toDouble();
  sliceByteAnisoDiff(&sSlice, ip->andfImage, ip->andfImage2, sParam.andfStopFunc + 2,
                     sParam.andfK, sParam.andfLambda, sParam.andfIterations, 
                     &sParam.andfIterDone);
  sCommand.sprintf("clip diffusion -cc %d -k %.5g -l %.3f -n %d", sParam.andfStopFunc + 2,
                   sParam.andfK / sProc.vi->image->slope, sParam.andfLambda,
                   sParam.andfIterations);
  cannotDoFFTStr(sCommand, "clip diffusion");
}

// Set the min and max of the static slice to full range, or actual values
static void setSliceMinMax(bool actual)
{
  if (actual) {
    sliceMinMax(&sSlice);
  } else if (App->depth == 8){
    sSlice.min = sProc.vi->rampbase;
    sSlice.max = sProc.vi->rampsize + sSlice.min - 1;
  } else {
    sSlice.min = 0;
    sSlice.max = 255;
  }
}

// Figure out if there needs to be a mode change in the command string for processing
// that produces an expanded range or results in a mean of 0
static QString modeChangeStr(const char *modeOpt)
{
  QString str = "";
  ImodIProc *ip = &sProc;
  float amin = App->cvi->image->amin;
  float amax = App->cvi->image->amax;

  // Byte must be promoted to signed integer
  if (ip->inputMode == 0)
    ip->outputMode = 1;

  // Complex must be converted to float (processing may not work right)
  else if (ip->inputMode == 4)
    ip->outputMode = 2;

  // integer has to be promoted to 2 if original range is big enough for ones that
  // expand the range, or if a mean of zero would put min/max out of range
  else if (ip->inputMode == 1 || ip->inputMode == 6) {
    if (!ip->wasByte && 
        ((sParam.procNum != 1 && (amin < -10000 || amax > 10000)) || 
         (sParam.procNum == 1 && (amin - App->cvi->image->amean < -30000
                                  || amax - App->cvi->image->amean > 30000))))
      ip->outputMode = 2;
    else if (ip->inputMode == 6)
      ip->outputMode = 1;
  }
  if (ip->outputMode != ip->inputMode)
    str.sprintf(" -%s %d", modeOpt, ip->outputMode);
  return str;
}

// Return string if regular clip operation on a FFT needs to change to float
static QString clipFFTtoRealStr()
{
  if (sProc.inputMode == 4) {
    sProc.outputMode = 2;
    return QString(" -m 2");
  }
  return QString("");
}

static void cannotDoFFTStr(QString &str, const char *operation)
{
  if (sProc.inputMode == 4)
    str.sprintf("Cannot run %s on FFT data", operation);
}

/* Reset and get new data buffer */
int iprocRethink(struct ViewInfo *vi)
{
  if (sProc.dia){
    if (sProc.isaved) {
      clearsec(&sProc);
      sProc.idataSec = -1;
      freeArrays(&sProc);
    }
    sProc.isaved = (unsigned char *)malloc(vi->xsize * vi->ysize);
    sProc.iwork = (unsigned char *)malloc(vi->xsize * vi->ysize);

    sParam.andfIterDone = 0;
    sProc.andfDoneLabel->setText("0 done");

    if (!sProc.isaved || !sProc.iwork) {
      freeArrays(&sProc);
      wprint("\aCannot get new memory for processing window!\n");
      sProc.dia->close();
      return 1;
    }

    sProc.dia->limitFFTbinning();
  }
  return 0;
}

/* Update for changes in the system (i.e., autoapply if section changed */
void iprocUpdate(void)
{
  if (!sProc.dia || sProc.vi->loadingImage || sProc.dia->mRunningProc || 
      sProc.dia->mUseStackInd >= 0)
    return;

  sProc.dia->calcFileThreshold();

  /* If time or section has changed, do a save or apply if option checked */
  if (B3DNINT(sProc.vi->zmouse) != sProc.idataSec ||
      sProc.vi->curTime != sProc.idataTime) {
    if (sProc.autoSave)
      sProc.dia->buttonClicked(SAVE_BUT);
    if (sProc.autoApply)
      sProc.dia->apply(true);
  }
}

/* Apply if window is open */
void iprocApply(void)
{
  if (sProc.dia && !iprocBusy() && !sProc.vi->loadingImage)
    sProc.dia->apply();
}

/* Test whether the window is open */
bool iprocIsOpen()
{
  return sProc.dia != NULL;
}

/* Return the list of commands */
QStringList iprocCommandList()
{
  if (!sProc.dia)
    return QStringList();
  return sProc.dia->mCommandList;
}

// Return current parameters for fourier filter and whether it should be applied
bool iprocFilterForSlicer(float &radius1, float &sigma1, float &radius2, float &sigma2)
{
  radius1 = sParam.radius1;
  sigma1 = sParam.sigma1;
  radius2 = sParam.radius2;
  sigma2 = sParam.sigma2;
  return sProc.dia && (FILTER_SLICER || sProc.dia->mParamStack.size() > 1) && 
    sProc.slicerFiltApplied && !sProc.toggling;
}

/* Open the processing dialog box */
int inputIProcOpen(struct ViewInfo *vi)
{
  size_t dataSize = (size_t)vi->xsize * (size_t)vi->ysize;
  if (dataSize > 2147000000) {
    wprint("\aData are too large to apply image processing to\n");
    return 1;
  }
  dataSize *= ivwGetPixelBytes(vi->rawImageStore);
  if (!sProc.dia) {
    if (!sProc.vi) {
      sParam.procNum = 0;
      sProc.autoApply = false;
      sProc.autoSave = false;
      sProc.applyThreshChange = false;
      sParam.threshold = 128;
      sParam.threshGrow = false;
      sParam.threshShrink = false;
      sParam.edge = 0;
      sParam.kernelSigma = NO_KERNEL_SIGMA;
      sParam.smooth3D = false;
      sParam.rescaleSmooth = true;
      sParam.sigma1 = 0.;
      sParam.sigma2 = 0.05f;
      sParam.radius1 = 0.;
      sParam.radius2 = 0.5f;
      sProc.filterSlicer = false;
      sParam.fftBinning = 1;
      sProc.fftScale = 0.;
      sParam.fftSubset = false;
      sParam.medianSize = 3;
      sParam.median3D = true;
      sParam.andfK = 2.;
      sParam.andfStopFunc = 0;
      sParam.andfLambda = 0.2;
      sParam.andfIterations = 5;
      sProc.isaved = NULL;
      sProc.iwork = NULL;
      sProc.andfImage = NULL;
      sProc.andfImage2 = NULL;
    }
    sProc.vi = vi;
    sProc.idataSec = -1;
    sProc.idataTime = 0;
    sProc.modified = 0;
    sParam.andfIterDone = 0;
    sProc.slicerFiltApplied = false;
    sProc.toggling = false;

    sProc.isaved = (unsigned char *)malloc(dataSize);
    sProc.iwork = (unsigned char *)malloc(dataSize);

    if (!sProc.isaved || !sProc.iwork) {
      freeArrays(&sProc);
      wprint("\aCannot get memory for processing window!\n");
      return(-1);
    }

    sTopWin = new DockingDialog(imodDialogManager.parent(IMOD_DIALOG), &imodDialogManager,
                                "Image Processing", "imageProc.html#TOP", 'p', 
                                Qt::Window);
    sProc.dia = new IProcWindow(sTopWin);
    sTopWin->addWidgetToStack(sProc.dia);
    adjustGeometryAndShow((QWidget *)sTopWin, IMOD_DIALOG);
    imodDialogManager.add((QWidget *)sTopWin, IMOD_DIALOG, DOCKING_DIALOG_TYPE);
    ImodInfoWin->manageMenus();

  } else {
    sTopWin->raise();
  }
  return(0);
}

static void freeArrays(ImodIProc *ip)
{
  if (ip->isaved)
    free(ip->isaved);
  ip->isaved = NULL;
  if (ip->iwork)
    free(ip->iwork);
  ip->iwork = NULL;
  if (ip->andfImage) {
    free(ip->andfImage[0]);
    free(ip->andfImage);
    ip->andfImage = NULL;
  }
  if (ip->andfImage2) {
    free(ip->andfImage2[0]);
    free(ip->andfImage2);
    ip->andfImage2 = NULL;
  }
}

// Inform other program components if thread is busy
bool iprocBusy(void)
{
  if (!sProc.dia)
    return false;
  return sProc.dia->mRunningProc;
}

// If the thread is busy, save the callback function; otherwise call it now
void iprocCallWhenFree(void (*func)())
{
  if (sProc.dia && iprocBusy())
    sProc.dia->mCallback = func;
  else
    func();
}

/*
 * DATA COPYING FUNCTIONS
 */

/* Copy data from a generated slice into the working buffer and free slice */
static void cpdslice(Islice *sl, ImodIProc *ip)
{
  register unsigned char *from, *to, *last;
  int rampbase = ip->vi->rampbase;
  from = sl->data.b;
  to = ip->iwork;
  if (!to) return;

  last = to + (ip->vi->xsize * ip->vi->ysize);
  if (App->depth > 8){
    do{
      *to++ = *from++;
    }while (to !=  last);
  }else{
    do{
      *to++ = *from++ + rampbase;
    }while (to !=  last);
  }
  sliceFree(sl);
}

/* Copy the working buffer back to the display memory and draw */
static void copyAndDisplay()
{
  ImodIProc *ip = &sProc;
  unsigned char **to = ivwGetZSectionTime(ip->vi, ip->idataSec, ip->idataTime);
  unsigned char *from = ip->iwork;
  b3dUInt16 **usto = (b3dUInt16 **)to;
  int i, j;
  float slope;
  b3dUInt16 *usmap = NULL;
  int cz =  (int)(ip->vi->zmouse + 0.5f);
  
  if (ip->vi->ushortStore) {
    slope = (ip->rangeHigh - ip->rangeLow) / 255.;
    usmap = (b3dUInt16 *)get_byte_map(slope, (float)ip->rangeLow, 0, 65535, 0);
  }
  for (j = 0; j < ip->vi->ysize; j++) {
    if (usmap)
      for (i = 0; i < ip->vi->xsize; i++)

        usto[j][i] = usmap[*from++];
    else
      for (i = 0; i < ip->vi->xsize; i++)
        to[j][i] = *from++;
  }

  imod_info_float_clear(cz, ip->vi->curTime);
  imodDraw(ip->vi, IMOD_DRAW_IMAGE);
}


/* clear the section back to original data. */
static void clearsec(ImodIProc *ip)
{
  unsigned char **savePtrs;
     
  if (ip->idataSec < 0 || !ip->modified)
    return;

  savePtrs = ivwMakeLinePointers(ip->vi, ip->isaved, ip->vi->xsize, ip->vi->ysize,
                                 ip->vi->rawImageStore);
  if (!savePtrs)
    return;
  imageToBuffer(ip, savePtrs, ip->iwork);
  if (savedToImage(ip))
    return;

  ip->modified = 0;
  ip->slicerFiltApplied = false;
  imod_info_float_clear(ip->idataSec, ip->idataTime);
}

/* save the displayed image to saved and working buffers. */
static void savesec(ImodIProc *ip)
{
  unsigned char **image;
  int j;
  unsigned char *isaved = ip->isaved;
  int numbytes = ivwGetPixelBytes(ip->vi->rawImageStore) * ip->vi->xsize;
     
  if (ip->idataSec < 0)
    return;

  image = ivwGetZSectionTime(ip->vi, ip->idataSec, ip->idataTime);
  if (!image) 
    return;
  imageToBuffer(ip, image, ip->iwork);
  for (j = 0; j < ip->vi->ysize; j++) {
    memcpy(isaved, image[j], numbytes);
    isaved += numbytes;
  }
}

/* Copy an image described by line pointers to a byte buffer */
static void imageToBuffer(ImodIProc *ip, unsigned char **image, unsigned char *buf)
{
  if (ivwCopyImageToByteBuffer(ip->vi, image, buf))
    return;
  if (ip->vi->ushortStore) {
    ip->rangeLow = ip->vi->rangeLow;
    ip->rangeHigh = ip->vi->rangeHigh;
  }
}

/* Copy saved buffer back to current image */
static int savedToImage(ImodIProc *ip)
{
  unsigned char *isaved = ip->isaved;
  int numbytes = ivwGetPixelBytes(ip->vi->rawImageStore) * ip->vi->xsize;
  int j;
  unsigned char **image = ivwGetZSectionTime(ip->vi, ip->idataSec, ip->idataTime);
  if (!image)
    return 1;
  for (j = 0; j < ip->vi->ysize; j++) {
    memcpy(image[j], isaved, numbytes);
    isaved += numbytes;
  }
  return 0;
}

// Set up medianVol with a set of slices and load the Z range into them
static int fillMedianVol(ImodIProc *ip, int zStart, int zEnd)
{
  unsigned char **from;
  int i, j, z;
  ip->medianVol.zsize = zEnd + 1 - zStart;
  ip->medianVol.vol = B3DMALLOC(Islice *, ip->medianVol.zsize);
  if (!ip->medianVol.vol)
    return 1;

  for (i = 0, z = zStart; z <= zEnd; z++, i++) {

    // Get a slice and get the array of pointers
    ip->medianVol.vol[i] = sliceCreate(ip->vi->xsize, ip->vi->ysize, SLICE_MODE_BYTE);
    from = ivwGetZSectionTime(ip->vi, z, ip->idataTime);

    // If either is in error, clean up what is already allocated
    if (!from || !ip->medianVol.vol[i]) {
      freeMedianVol(ip, i + 1);
      return 1;
    }

    // Copy data
    imageToBuffer(ip, from, ip->medianVol.vol[i]->data.b);
  }
  return 0;
}

// Clean up medianVol
static void freeMedianVol(ImodIProc *ip, int zsize)
{
  for (int j = 0; j < zsize; j++)
    sliceFree(ip->medianVol.vol[j]);
  free(ip->medianVol.vol); 
}

/*
 * FUNCTIONS TO MAKE THE WIDGETS FOR PARTICULAR FILTERS
 */
static void mkedge_cb(IProcWindow *win, QWidget *parent, QVBoxLayout *layout)
{
  diaLabel("Edge Enhancement Filter Type:", parent, layout);
  QComboBox *edgeBox = new QComboBox(parent);
  layout->addWidget(edgeBox);
  QStringList items;
  items << "Sobel" << "Prewitt" << "Laplacian" << "Graham" << "Gradient";
  edgeBox->addItems(items);
  edgeBox->setFocusPolicy(Qt::NoFocus);
  edgeBox->setCurrentIndex(sParam.edge);
  QObject::connect(edgeBox, SIGNAL(activated(int)), win, 
                   SLOT(edgeSelected(int)));
}

static void mkSmooth_cb(IProcWindow *win, QWidget *parent, QVBoxLayout *layout)
{
  diaLabel("Smoothing", parent, layout);
  diaLabel(" Uses standard 3x3 kernel", parent, layout);
  diaLabel(" or Gaussian if sigma or 3D set", parent, layout);
  QHBoxLayout *hLayout = diaHBoxLayout(layout);
  sProc.kernelSpin = (QDoubleSpinBox *)diaLabeledSpin
    (2, NO_KERNEL_SIGMA, 10., 0.1f, "Kernel sigma", parent, hLayout, NULL, 1.6);
  diaLabel("pixels", parent, hLayout);
  sProc.kernelSpin->setSpecialValueText("None");
  sProc.kernelSpin->setCorrectionMode(QAbstractSpinBox::CorrectToNearestValue);
  sProc.kernelSpin->setValue(sParam.kernelSigma);
  QObject::connect(sProc.kernelSpin, SIGNAL(valueChanged(double)), win, 
                   SLOT(kernelChanged(double)));

  QCheckBox *check = diaCheckBox("Filter in 3D cube", parent, layout);
  diaSetChecked(check, sParam.smooth3D);
  QObject::connect(check, SIGNAL(toggled(bool)), win, SLOT(smooth3DChanged(bool)));
  check->setToolTip("Smooth in 3D cube instead of in 2D within this section; uses "
                    "sigma 0.85 when none set");

  check = diaCheckBox("Rescale to match min/max", parent, layout);
  diaSetChecked(check, sParam.rescaleSmooth);
  QObject::connect(check, SIGNAL(toggled(bool)), win,
                   SLOT(scaleSmthToggled(bool)));
  check->setToolTip("Rescale smoothed slice so its min/max matches original"
                " slice");

}

static void mkthresh_cb(IProcWindow *win, QWidget *parent, QVBoxLayout *layout)
{
  const char *sliderLabel[] = {"Threshold filter value" };
  MultiSlider *slider = new MultiSlider(parent, 1, sliderLabel, 0, 254);
  slider->setValue(0, sParam.threshold);
  QObject::connect(slider, SIGNAL(sliderChanged(int, int, bool)), win, 
          SLOT(threshChanged(int, int, bool)));
  layout->addLayout(slider->getLayout());
  sProc.threshFileLabel = diaLabel("   ", parent, layout);
  QCheckBox *check = diaCheckBox("Grow thresholded area", parent, layout);
  diaSetChecked(check, sParam.threshGrow);
  QObject::connect(check, SIGNAL(toggled(bool)), win, SLOT(growChanged(bool)));
  check->setToolTip("Apply dilation to grow selected area ");
  check = diaCheckBox("Shrink thresholded area", parent, layout);
  diaSetChecked(check, sParam.threshShrink);
  QObject::connect(check, SIGNAL(toggled(bool)), win, SLOT(shrinkChanged(bool)));
  check->setToolTip("Apply erosion to shrink selected area");

  check = diaCheckBox("Apply changes automatically", parent, layout);
  diaSetChecked(check, sProc.applyThreshChange);
  QObject::connect(check, SIGNAL(toggled(bool)), win, SLOT(applyThreshToggled(bool)));
  check->setToolTip("Apply or redo processing when a threshold setting is changed");
}

static void mkFourFilt_cb(IProcWindow *win, QWidget *parent,
                          QVBoxLayout *layout)
{
  const char *sliderLabel[] = {"Low-frequency sigma", "High-frequency cutoff",
                         "High-frequency falloff"};
  diaLabel("Filtering in Fourier Space", parent, layout);
  MultiSlider *slider = new MultiSlider(parent, 3, sliderLabel, 0, 200, 3);
  win->mFourFiltSliders = slider;
  win->setSigma1Slider(sParam.sigma1);
  slider->setRange(1, 0, 500);
  slider->setRange(2, 1, 200);
  slider->setValue(1, (int)(1000. * sParam.radius2));
  slider->setValue(2, (int)(1000. * sParam.sigma2));
  QObject::connect(slider, SIGNAL(sliderChanged(int, int, bool)), win, 
          SLOT(fourFiltChanged(int, int, bool)));
  layout->addLayout(slider->getLayout());
  slider->getSlider(0)->setToolTip("Sigma for inverted Gaussian"
                " high-pass filter (0 at origin)");
  slider->getSlider(1)->setToolTip("Cutoff radius for Gaussian low-pass filter");
  slider->getSlider(2)->setToolTip("Sigma for Gaussian low-pass filter starting"
                                            " at cutoff");
  QCheckBox *check = diaCheckBox("Filter Slicer image instead", parent, layout);
  diaSetChecked(check, sProc.filterSlicer);
  QObject::connect(check, SIGNAL(toggled(bool)), win, SLOT(filtSlicerToggled(bool)));
  check->setToolTip("Filter Slicer window images instead of Z slice in memory");
}

static void mkFFT_cb(IProcWindow *win, QWidget *parent, QVBoxLayout *layout)
{
  diaLabel("Fourier transform", parent, layout);
  
  QHBoxLayout *hLayout = diaHBoxLayout(layout);
  sProc.fftBinSpin = (QSpinBox *)diaLabeledSpin(0, 1, 8, 1, "Binning", parent,
                                               hLayout);
  QObject::connect(sProc.fftBinSpin, SIGNAL(valueChanged(int)), win, 
                   SLOT(binningChanged(int)));
  QCheckBox *check = diaCheckBox("Use Zap window subarea", parent, layout);
  diaSetChecked(check, sParam.fftSubset);
  QObject::connect(check, SIGNAL(toggled(bool)), win,
                   SLOT(subsetChanged(bool)));
  check->setToolTip("Do FFT of area displayed or within rubber band "
                "in active Zap window");

  sProc.fftLabel1 = diaLabel("  ", parent, layout);
  sProc.fftLabel2 = diaLabel("  ", parent, layout);

  hLayout = diaHBoxLayout(layout);
  hLayout->addStretch(0);
  sProc.freqButton = diaPushButton("Report frequency", parent, hLayout);
  QObject::connect(sProc.freqButton, SIGNAL(clicked()), win,
                   SLOT(reportFreqClicked()));
  sProc.freqButton->setToolTip(
                "Compute resolution at current marker or model point");
  sProc.freqButton->setEnabled(false);
  hLayout->addStretch(0);

  sProc.fftLabel3 = diaLabel("  ", parent, layout);
  win->limitFFTbinning();
}

static void mkMedian_cb(IProcWindow *win, QWidget *parent, 
                        QVBoxLayout *layout)
{
  diaLabel("Median filter", parent, layout);
  QHBoxLayout *hLayout = diaHBoxLayout(layout);
  QSpinBox *sizeSpin = (QSpinBox *)diaLabeledSpin(0., 2., 9., 1., "Size",
                                                  parent, hLayout, NULL, 1.6);
  diaSetSpinBox(sizeSpin, sParam.medianSize);
  QObject::connect(sizeSpin, SIGNAL(valueChanged(int)), win, 
                   SLOT(medSizeChanged(int)));
  QCheckBox *check = diaCheckBox("Compute median in 3D cube", parent, layout);
  diaSetChecked(check, sParam.median3D);
  QObject::connect(check, SIGNAL(toggled(bool)), win,
                   SLOT(med3DChanged(bool)));
  check->setToolTip("Take median in 3D cube instead of median in 2D within"
                " this section");
}

static void mkAnisoDiff_cb(IProcWindow *win, QWidget *parent,
                           QVBoxLayout *layout)
{
  QString str;
  diaLabel("Anisotropic diffusion", parent, layout);

  // The edge stopping radio buttons
  QGroupBox *gbox = new QGroupBox("Edge Stopping Function", parent);
  layout->addWidget(gbox);
  QVBoxLayout *gbLayout = new QVBoxLayout(gbox);
  gbLayout->setSpacing(0);
  gbLayout->setContentsMargins(5, 2, 5, 5);
  QButtonGroup *stopGroup = new QButtonGroup(parent);
  QRadioButton *radio = diaRadioButton
    ("Rational", gbox, stopGroup, gbLayout, 0, "Use rational edge stopping "
     "function; may require smaller K values");
  radio = diaRadioButton("Tukey biweight", gbox, stopGroup, gbLayout, 1,
                         "Use Tukey biweight edge stopping function; may "
                         "require larger K values");
  diaSetGroup(stopGroup, sParam.andfStopFunc);
  QObject::connect(stopGroup, SIGNAL(buttonClicked(int)), win, 
                   SLOT(andfFuncClicked(int)));

  // Iteration spin box and report of # done
  QBoxLayout *hLayout = diaHBoxLayout(layout);
  
  QSpinBox *iterSpin = (QSpinBox *)diaLabeledSpin
    (0, 1., 1000., 1., "Iterations", parent, hLayout);
  diaSetSpinBox(iterSpin, sParam.andfIterations);
  iterSpin->setToolTip("Number of time steps to take in one run");
  QObject::connect(iterSpin, SIGNAL(valueChanged(int)), win, 
                   SLOT(andfIterChanged(int)));
  sProc.andfDoneLabel = new QLabel("0 done", parent);
  hLayout->addWidget(sProc.andfDoneLabel);

  // K edit box and report of unscaled K value
  hLayout = diaHBoxLayout(layout);
  QLabel *label = new QLabel("K", parent);
  label->setAlignment(Qt::AlignRight | Qt::AlignVCenter);
  hLayout->addWidget(label);
  sProc.andfKEdit = new ToolEdit(parent, 6);
  hLayout->addWidget(sProc.andfKEdit);
  sProc.andfKEdit->setToolTip("Gradient threshold parameter controlling "
                "edge stopping function");
  str.sprintf("%.5g", sParam.andfK);
  sProc.andfKEdit->setText(str);
  sProc.andfKEdit->setFocusPolicy(Qt::ClickFocus);
  sProc.andfScaleLabel = new QLabel(" ", parent);
  setUnscaledK();
  hLayout->addWidget(sProc.andfScaleLabel);
  QObject::connect(sProc.andfKEdit, SIGNAL(returnPressed()), win,
                   SLOT(setFocus()));
  QObject::connect(sProc.andfKEdit, SIGNAL(returnPressed()), win,
                   SLOT(andfKEntered()));
  QObject::connect(sProc.andfKEdit, SIGNAL(focusLost()), win,
                   SLOT(andfKEntered()));

  // Lambda edit box
  hLayout = diaHBoxLayout(layout);
  label = new QLabel("Lambda", parent);
  label->setAlignment(Qt::AlignRight | Qt::AlignVCenter);
  hLayout->addWidget(label);
  sProc.andfLambdaEdit = new ToolEdit(parent, 6);
  hLayout->addWidget(sProc.andfLambdaEdit);
  sProc.andfLambdaEdit->setToolTip("Size of time step");
  str.sprintf("%g", sParam.andfLambda);
  sProc.andfLambdaEdit->setText(str);
  sProc.andfLambdaEdit->setFocusPolicy(Qt::ClickFocus);
  QObject::connect(sProc.andfLambdaEdit, SIGNAL(returnPressed()), win,
                   SLOT(setFocus()));
  hLayout->addStretch();
}

static void setUnscaledK()
{
  QString str;
  str.sprintf("unscaled: %.5g",  sParam.andfK / sProc.vi->image->slope);
  sProc.andfScaleLabel->setText(str);
}

/* THE WINDOW CLASS CONSTRUCTOR */
static const char *buttonLabels[] = {"Apply", "More", "Less", "Do Same", 
                                     "Toggle", "Reset", "Save", " List "};
static const char *buttonTips[] = {"Operate on current section (hot key A)",
                                   "Apply operation to current processed section (hot key"
                                   " B)",
                                   "Reprocess, removing one operation done with More",
                                   "Do last sequence of operations on current section",
                                   "Toggle between processed and original image",
                                   "Reset section to unprocessed image",
                                   "Replace section in memory with processed image",
                                   "List command(s) for processing image file"};

IProcWindow::IProcWindow(DockingDialog *parent, const char *name)
  : DialogFrame(parent, 8, 2, buttonLabels, buttonTips, false, 
                ImodPrefs->getRoundedStyle(), " ", "", name, 0)
{
  int i;
  QString str;
  QVBoxLayout *vLayout;
  QWidget *control;
  mTopWin = parent;
  mRunningProc = false;
  mUseStackInd = -1;
  mSigma1Scale = 0.;
  mLastFourFiltRange = 0.2;

  // Put an H layout inside the main layout, then fill that with the
  // List box and the widget stack
  QHBoxLayout *hLayout = diaHBoxLayout(mLayout);
  vLayout = diaVBoxLayout(hLayout);
  mListBox = new QListWidget(this);
  vLayout->addWidget(mListBox);
  mListBox->setFocusPolicy(Qt::NoFocus);
  mListBox->setSelectionMode(QAbstractItemView::SingleSelection);

  vLayout->addStretch();
  vLayout->setSpacing(3);
  QCheckBox *check = diaCheckBox("Autoapply", this, vLayout);
  diaSetChecked(check, sProc.autoApply);
  QObject::connect(check, SIGNAL(toggled(bool)), this, SLOT(autoApplyToggled(bool)));
  check->setToolTip("Apply current process automatically when changing section");

  check = diaCheckBox("Autosave", this, vLayout);
  diaSetChecked(check, sProc.autoSave);
  QObject::connect(check, SIGNAL(toggled(bool)), this, SLOT(autoSaveToggled(bool)));
  check->setToolTip("Save processed data in memory automatically when changing section");

  mStack = new QStackedWidget(this);
  hLayout->addWidget(mStack);

  // Put a spacer on the right to keep the list box position from changing
  hLayout->addStretch(0);

  for (i = 0; (procTable[i].name); i++) {

    // For each item, add to list box, make a widget and give it a V layout
    mListBox->addItem(procTable[i].name);
    control = new QWidget(this);
    vLayout = new QVBoxLayout(control);
    vLayout->setContentsMargins(3, 3, 3, 3);
    vLayout->setSpacing(6);

    // Call the make widget function or just add a label
    if (procTable[i].mkwidget)
      procTable[i].mkwidget (this, control, vLayout);
    else {
      diaLabel(procTable[i].label, control, vLayout);
    }
    vLayout->addStretch(0);

    // Add widget to stack and set size policy to ignored
    mStack->addWidget(control);
    control->setSizePolicy(QSizePolicy(QSizePolicy::Ignored,
                                       QSizePolicy::Ignored));
    procTable[i].control = control;
  }
  calcFileThreshold();

  // Finalize list box setting and connections
  utilManageListStackSizes(mListBox, mStack);
  filterHighlighted(sParam.procNum);
  mListBox->setCurrentRow(sParam.procNum);
  connect(mListBox, SIGNAL(currentRowChanged(int)), this,
          SLOT(filterHighlighted(int)));
  connect(mListBox, SIGNAL(itemDoubleClicked(QListWidgetItem *)), this, 
          SLOT(filterSelected(QListWidgetItem *)));
  
  connect(this, SIGNAL(actionClicked(int)), this, SLOT(buttonClicked(int)));
  connect(this, SIGNAL(actionPressed(int)), this, SLOT(buttonPressed(int)));
  connect(mTopWin, SIGNAL(dockerCloseEvent(QCloseEvent *)), this,
          SLOT(topCloseEvent(QCloseEvent *)));
  connect(mTopWin, SIGNAL(dockerChangeEvent(QEvent *)), this,
          SLOT(topChangeEvent(QEvent *)));
  parent->setWindowTitle(imodCaption("Process"));
  mButtons[2]->setEnabled(false);

}

/* Action functions */

void IProcWindow::autoApplyToggled(bool state)
{
  setFocus();
  sProc.autoApply = state;
}

void IProcWindow::autoSaveToggled(bool state)
{
  setFocus();
  sProc.autoSave = state;
}

void IProcWindow::applyThreshToggled(bool state)
{
  setFocus();
  sProc.applyThreshChange = state;
}

void IProcWindow::threshChanged(int which, int value, bool dragging)
{
  sParam.threshold = value;
  newThreshSetting();
}

void IProcWindow::fourFiltChanged(int which, int value, bool dragging)
{
  if (!which)
    setSigma1Slider(value / mSigma1Scale);
  else if (which == 1)
    sParam.radius2 = 0.001 * value;
  else if (which == 2)
    sParam.sigma2 = 0.001 * value;
}

void IProcWindow::filtSlicerToggled(bool state)
{
  setFocus();
  sProc.filterSlicer = state;
  apply(true);
}

// Dynamic adjustment of step size from small at 0 to 10 times larger at 0.005
void IProcWindow::setSigma1Slider(float value)
{
  int newIntVal, newMax, decimals = 4;
  float newRange;
  if (value <= 0.00101) {
    mSigma1Scale = 10000.;
    newMax = 360;
  } else if (value <= 0.00201) {
    mSigma1Scale = 5000.;
    newMax = 300;
  } else if (value <= 0.00501) {
    mSigma1Scale = 2000.;
    newMax = 240;
  } else {
    mSigma1Scale = 1000.;
    decimals = 3;
    newMax = 200;
  }

  // When it is clicked into small-step range from larger range, adjust the value to be
  // the value at that position on the slider in the new range
  newRange = newMax / mSigma1Scale;
  if (mLastFourFiltRange < 0.19 && value > 0.0055)
    value = newRange * value / mLastFourFiltRange;
  mLastFourFiltRange = newRange;
    
  newIntVal = B3DNINT(mSigma1Scale * value);
  
  if (fabs(newIntVal / mSigma1Scale - sParam.sigma1) < 1.e-5) {
    if (value > sParam.sigma1)
      newIntVal++;
    if (value < sParam.sigma1)
      newIntVal = B3DMAX(0., newIntVal - 1);
  }
  sParam.sigma1 = newIntVal / mSigma1Scale;
  mFourFiltSliders->setDecimals(0, decimals);
  mFourFiltSliders->setScaling(0, mSigma1Scale);
  mFourFiltSliders->setMinMaxVal(0, 0, newMax, newIntVal);
}

void IProcWindow::kernelChanged(double val)
{
  setFocus();
  sParam.kernelSigma = val;
}

void IProcWindow::smooth3DChanged(bool state)
{
  setFocus();
  sParam.smooth3D = state;
}

void IProcWindow::scaleSmthToggled(bool state)
{
  setFocus();
  sParam.rescaleSmooth = state;
}

void IProcWindow::binningChanged(int val)
{
  setFocus();
  sParam.fftBinning = val;
}
void IProcWindow::subsetChanged(bool state)
{
  sParam.fftSubset = state;
}

void IProcWindow::reportFreqClicked()
{
  double dx, dy, xpt, ypt, dist;
  Ipoint *curpt;
  QString str;
  Imod *imod = sProc.vi->imod;
    
  if (sProc.fftScale <= 0.)
    return;

  // Use the current model point if defined and in model mode, or use 
  // current marker point
  curpt = imodPointGet(imod);
  if (sProc.vi->imod->mousemode == IMOD_MMODEL && curpt) {
    xpt = curpt->x - 0.5f;
    ypt = curpt->y - 0.5f;
  } else {
    xpt = (int)sProc.vi->xmouse;
    ypt = (int)sProc.vi->ymouse;
  }
  dx = xpt - sProc.fftXcen;
  dy = ypt - sProc.fftYcen;
  dist = sProc.fftScale * sqrt(dx * dx + dy * dy) / 
    (imod->pixsize * sProc.vi->xybin);
  if (dist) 
    str.sprintf("Freq: %.4g/%s  (%.4g %s)", dist, imodUnits(imod), 
                1. / dist, imodUnits(imod));
  else
    str.sprintf("Freq: 0/%s", imodUnits(imod));
  sProc.fftLabel3->setText(str);
}


void IProcWindow::growChanged(bool state)
{
  sParam.threshGrow = state;
  newThreshSetting();
}
void IProcWindow::shrinkChanged(bool state)
{
  sParam.threshShrink = state;
  newThreshSetting();
}

void IProcWindow::newThreshSetting()
{
  calcFileThreshold();
  if (sProc.applyThreshChange && !(mRunningProc || mUseStackInd >= 0))
    apply(mParamStack.size() > 1);
}

// To switch filters, set the size policy of the current widget back to ignored
// raise the new widget, set its size policy, make the stack process geometry
// again then adjust window size
void IProcWindow::filterHighlighted(int which)
{
  QWidget *control = mStack->currentWidget();
  if (control)
    control->setSizePolicy(QSizePolicy(QSizePolicy::Ignored,
                                       QSizePolicy::Ignored));
  sParam.procNum = which;
  mStack->setCurrentIndex(which);
  control = mStack->currentWidget();
  control->setSizePolicy(QSizePolicy(QSizePolicy::Expanding,
                                     QSizePolicy::Expanding));
  imod_info_input();
  mStack->adjustSize();
  imod_info_input();
  adjustSize();
}

void IProcWindow::filterSelected(QListWidgetItem *item)
{
  int which = mListBox->row(item);
  filterHighlighted(which);
  if (!sProc.dia->mRunningProc)
    apply();
}

void IProcWindow::edgeSelected(int which)
{
  sParam.edge = which;
}

void IProcWindow::medSizeChanged(int val)
{ 
  setFocus();
  sParam.medianSize = val;
}

void IProcWindow::med3DChanged(bool state)
{
  sParam.median3D = state;
}

void IProcWindow::andfIterChanged(int val)
{
  setFocus();
  sParam.andfIterations = val;
}
void IProcWindow::andfFuncClicked(int val)
{
  sParam.andfStopFunc = val;
}

void IProcWindow::andfKEntered()
{
  sParam.andfK = sProc.andfKEdit->text().toDouble();
  setUnscaledK();
}


// Respond to button click (release)
void IProcWindow::buttonClicked(int which)
{
  ImodIProc *ip = &sProc;

  int cz =  (int)(ip->vi->zmouse + 0.5f);
  setFocus();

  if (which < DONE_BUT && ip->vi->loadingImage)
    return;

  switch (which) {
  case APPLY_BUT:  // Apply
    apply();
    break;

  case MORE_BUT:  // More
    /* If this is not the same section, treat it as an Apply */
    if (cz != ip->idataSec || ip->vi->curTime != ip->idataTime) {
      apply();
      break;
    }

    /* Otherwise operate on the current data without restoring it */
    if (procTable[sParam.procNum].cb) {
      mParamStack.push_back(sParam);
      mUseStackInd = -1;
      startProcess();
    }
    break;

  case LESS_BUT:  // Less
    if (mParamStack.size() > 1) {
      mParamStack.resize(mParamStack.size() - 1);
      mCommandList.removeLast();
      mDataModes.pop_back();
      apply(true);
    }
    break;
    
  case DO_SAME_BUT:  // Do Same
    if (mParamStack.size() > 0)
      apply(true);
    break;

  case TOGGLE_BUT:  // Toggle
    ip->toggling = false;
    if (ip->modified && cz == ip->idataSec && ip->vi->curTime == ip->idataTime)
      copyAndDisplay();
    break;

  case RESET_BUT: // reset
    clearsec(ip);
    imodDraw(ip->vi, IMOD_DRAW_IMAGE);
    break;

  case SAVE_BUT: // save
    ip->modified = 0;
    ip->idataSec = -1;
    break;

  case LIST_BUT: // List commands
    if (!mCommandList.size()) {
      wprint("\aThere are no commands in the command list\n");
    } else {
      wprint("IMOD commands for processing file:\n");
      for (cz = 0; cz < mCommandList.size(); cz++)
        wprint(" %s\n", LATIN1(mCommandList[cz]));
    }
    break;

  }
}

// Respond to button press for toggle button only - redisplay original data
// but re-mark as modified
void IProcWindow::buttonPressed(int which)
{
  ImodIProc *ip = &sProc;
  int cz =  (int)(ip->vi->zmouse + 0.5f);

  if (which != TOGGLE_BUT || !ip->modified || cz != ip->idataSec || 
      ip->vi->curTime != ip->idataTime)
    return;
     
  if (savedToImage(ip))
    return;
  ip->toggling = true;
  imod_info_float_clear(ip->idataSec, ip->idataTime);
  imodDraw(ip->vi, IMOD_DRAW_IMAGE);
}

// Apply the current filter or start using the filter(s) on the stack
void IProcWindow::apply(bool useStack)
{
  ImodIProc *ip = &sProc;
  IProcParam *topParam;
  sliceInit(&sSlice, ip->vi->xsize, ip->vi->ysize, 0, ip->iwork);

  int cz =  (int)(ip->vi->zmouse + 0.5f);

  // If using the stack, get the first param on the stack; otherwise clear out the stack
  // and put the param on it
  if (useStack && mParamStack.size() > 0) {
    mSavedParam = sParam;
    topParam = &mParamStack[mParamStack.size() - 1];

    // If current selection is the same operation as the last one on stack, update the
    // stack with current params; otherwise the saved param will be restored at end 
    // to match screen state, which is not what is being run in that case
    if (topParam->procNum == sParam.procNum)
      *topParam = sParam;
    sParam = mParamStack[0];
    mUseStackInd = 0;
  } else {
    mParamStack.clear();
    mParamStack.push_back(sParam);
    mUseStackInd = -1;
  }

  /* Unconditionally restore data if modified */
  clearsec(ip);

  mCommandList.clear();
  mDataModes.clear();
  mDataModes.push_back(App->cvi->image->mode);
  sProc.wasByte = App->cvi->image->mode == 0;
  sParam.andfIterDone = 0;
  ip->andfDoneLabel->setText("0 done");

  /* If this is a new section, save the data */
  if (cz != ip->idataSec || ip->vi->curTime != ip->idataTime) {
    ip->idataSec = cz;
    ip->idataTime = ip->vi->curTime;
    savesec(ip);
  }

  // Make sure there is floating info for this data so it can be saved when
  // it is cleared
  imod_info_bwfloat(ip->vi, ip->idataSec, ip->idataTime);
  imodInfoSaveNextClear();
    
  /* Operate on the original data */
  startProcess();
}

// Start one processing operation on a thread
void IProcWindow::startProcess()
{
  ImodIProc *ip = &sProc;
  int i;
  if (!procTable[sParam.procNum].cb)
    return;
  ip->fftScale = 0.;
  ip->freqButton->setEnabled(false);
  mCallback = NULL;
  sCommand = "";
  ip->inputMode = ip->outputMode = mDataModes.back();

  // For running in a thread, set flag, disable buttons except help,
  // start timer and start thread
  mRunningProc = true;
  for (i = 0; i < mNumButtons - 1; i++)
    mButtons[i]->setEnabled(false);
  ImodInfoWin->manageMenus();
  mTimerID = startTimer(50);
  mProcThread = new IProcThread;
  mProcThread->start(QThread::LowPriority);
}

// Do needed operations when operation is finished
void IProcWindow::finishProcess()
{
  ImodIProc *ip = &sProc;
  QString str;
  float xrange, yrange;
  ip->modified = 1;
  copyAndDisplay();
  mCommandList << sCommand;
  mDataModes.push_back(ip->outputMode);
  if (ip->fftScale < 0.) {
    wprint("\aMemory error trying to do FFT!\n");
  } else if (ip->fftScale > 0.) {
    str.sprintf("Scale: %.3g/pixel per FFT pixel", ip->fftScale);
    ip->fftLabel1->setText(str);
    xrange = 0.5 * ip->fftScale * ip->vi->xsize;
    xrange = xrange <= 0.5 ? xrange : 0.5f;
    yrange = 0.5 * ip->fftScale * ip->vi->ysize;
    yrange = yrange <= 0.5 ? yrange : 0.5f;
    str.sprintf("Range: +/- %.4f in X, +/- %.4f in Y", xrange, yrange);
    ip->fftLabel2->setText(str);
    ip->freqButton->setEnabled(true);
  }
  if (sParam.andfIterDone) {
    str.sprintf("%d done", sParam.andfIterDone);
    ip->andfDoneLabel->setText(str);
    setUnscaledK();
  }

  // Start the next process if running through the stack automatically
  if (mUseStackInd >= 0) {
    mUseStackInd++;
    if (mUseStackInd  < mParamStack.size()) {
      sParam = mParamStack[mUseStackInd];
      startProcess();
    } else {
      mUseStackInd = -1;
      sParam = mSavedParam;
    }
  }
}

// Check for whether the thread is running, if not, finish up and call callback
void IProcWindow::timerEvent(QTimerEvent *e)
{
  int i;
  if (mProcThread->isRunning())
    return;
  killTimer(mTimerID);
  for (i = 0; i < mNumButtons - 1; i++)
    if (i != 2)
      mButtons[i]->setEnabled(true);
  mButtons[2]->setEnabled(mParamStack.size() > 1);
  delete mProcThread;
  mRunningProc = false;
  finishProcess();
  ImodInfoWin->manageMenus();
  if (mCallback)
    mCallback();
}

void IProcWindow::limitFFTbinning()
{
  ImodIProc *ip = &sProc;
  int limit = 16;
  if (limit > ip->vi->xsize)
    limit = ip->vi->xsize;
  if (limit > ip->vi->ysize)
    limit = ip->vi->ysize;
  if (sParam.fftBinning > limit)
    sParam.fftBinning = limit;
  diaSetSpinMMVal(ip->fftBinSpin, 1, limit, sParam.fftBinning);
}

void IProcWindow::calcFileThreshold()
{
  ImodView *vi = sProc.vi;
  float range = vi->ushortStore ? 65535. : 255.;
  float low = vi->ushortStore ? vi->rangeLow : 0.;
  float high = vi->ushortStore ? vi->rangeHigh : 255.;
  float temp;
  QString str;
  float fthresh = ((sParam.threshold + 0.5) * (high - low) / 255. + low) * 
    (vi->image->smax - vi->image->smin) / range + vi->image->smin;
  if (vi->image->mode != MRC_MODE_FLOAT && vi->image->mode != MRC_MODE_COMPLEX_FLOAT) {
    temp = fthresh;
    fthresh = floor((double)fthresh);
    if (temp - fthresh < 1.e-6 * fthresh)
      fthresh -= 1.;
  }
  if (fabs((double)fthresh - sProc.fileThreshold) > 1.e-5 * fthresh) {
    str.sprintf("Threshold in file: %.5g", fthresh);
    sProc.threshFileLabel->setText(str);
    sProc.fileThreshold = fthresh;
  }
}

void IProcWindow::topChangeEvent(QEvent *e)
{
  mRoundedStyle = ImodPrefs->getRoundedStyle();
  DialogFrame::changeEvent(e);
  ivwCheckAndSetMacMenu(e, this, IMOD_DIALOG);
  if (e->type() == QEvent::FontChange)
    utilManageListStackSizes(mListBox, mStack);
}

// The window is closing, clean up and remove from manager
void IProcWindow::topCloseEvent ( QCloseEvent * e )
{
  ImodIProc *ip = &sProc;
  if (!ip->dia || mRunningProc)
    return;
  clearsec(ip);
  imodDialogManager.remove((QWidget *)sTopWin);
  imodDraw(ip->vi, IMOD_DRAW_IMAGE);
  freeArrays(ip);
  ip->dia = NULL;
  sTopWin = NULL;
  ImodInfoWin->manageMenus();
  e->accept();
}

// Close on escape, pass on keys
void IProcWindow::keyPressEvent ( QKeyEvent * e )
{
  int modkey = e->modifiers() & (Qt::ShiftModifier | Qt::ControlModifier);
  if (e->key() == Qt::Key_A && !modkey) {
    if (!iprocBusy() && !sProc.vi->loadingImage)
      apply();
  } else if (e->key() == Qt::Key_B && !modkey) {
    if (!iprocBusy() && !sProc.vi->loadingImage)
      buttonClicked(MORE_BUT);
  } else if (utilCloseKey(e))
    mTopWin->close();
  else
    ivwControlKey(0, e);
}

void IProcWindow::keyReleaseEvent ( QKeyEvent * e )
{
  ivwControlKey(1, e);
}

// A very simple thread run command!
void IProcThread::run()
{
  procTable[sParam.procNum].cb();
}

/*
 * Volume smoothing with a 3D kernel, for byte data.  Will replace the data in the volume
 * either filtering at every slice (flor isosurface) or doing just the middle slice (for 
 * iproc).  Dimens must be 3, 5, or 7
 */
int volByteSmooth(unsigned char **vol, int nx, int ny, int nz, float sigma, int midZonly)
{
  float floatKernel[KERNEL_MAXSIZE * KERNEL_MAXSIZE];
  int intKernel[KERNEL_MAXSIZE * KERNEL_MAXSIZE], zKernel[KERNEL_MAXSIZE];
  int dimens, ind, kernelSum = 0, zsum = 0, maxZ = KERNEL_MAXSIZE;
  int below, above, iz, outZ, sliceBaseZ, indSlice, halfSum, ix, iy, numThreads, numBuf;
  float scaling = 100.;
  short *sliceBuffer[KERNEL_MAXSIZE], *saveBuf, *useSlice[KERNEL_MAXSIZE];
  int zStart = midZonly ? nz / 2 : 0;
  int zEnd = midZonly ? nz / 2 : nz - 1;
  if (nz < KERNEL_MAXSIZE)
    maxZ = B3DMAX(3, 2 * ((nz - 1) / 2) + 1);
  
  /* Get the kernel, scale it, get sums to divide by */
  scaledGaussianKernel(&floatKernel[0], &dimens, maxZ, sigma);
  for (ind = 0; ind < dimens * dimens; ind++) {
    intKernel[ind] = B3DNINT(floatKernel[ind] * scaling);
    kernelSum += intKernel[ind];
  }
  for (ind = 0; ind < dimens; ind++) {
    zKernel[ind] = intKernel[ind + (dimens / 2) * dimens];
    zsum += zKernel[ind];
  }
    
  kernelSum *= zsum;
  halfSum = (kernelSum - 1) / 2;
  below = dimens / 2;
  above = dimens - 1 - dimens / 2;

  // The usual rough estimate of limits
  numThreads = dimens * sqrt((double)nx * ny) / 400.;
  B3DCLAMP(numThreads, 1, 4);
  numThreads = numOMPthreads(numThreads);

  /* Allocate the planes for individual slices */
  numBuf = B3DMIN(dimens, nz);
  for (ind = 0; ind < numBuf; ind++) {
    sliceBuffer[ind] = B3DMALLOC(short, nx * ny);
    if (!sliceBuffer[ind]) {
      for (iz = 0; iz < ind; iz++)
        free(sliceBuffer[iz]);
      return 1;
    }
  }

  /* Make initial filtered planes */
  for (ind = 0; ind < numBuf; ind++)
    byteKernelFilter(vol[ind], sliceBuffer[ind], nx, nx, nx, ny, intKernel,
                     dimens);
  sliceBaseZ = 0;

  /* Loop on output slices */
  //double wallCum = 0.;
  for (outZ = zStart; outZ <= zEnd; outZ++) {

    /* Determine input slice to add for each Z */
    for (iz = outZ - below; iz <= outZ + above; iz++) {
      indSlice = iz - sliceBaseZ;
      B3DCLAMP(indSlice, 0, numBuf - 1);
      useSlice[iz + below - outZ] = sliceBuffer[indSlice];
    }

    /* Form the sum with one pass in parallel on all slices */
    //double wallStart = wallTime();

#pragma omp parallel for num_threads(numThreads)                           \
  shared(dimens, nx, ny, zKernel, vol, outZ, useSlice, halfSum, kernelSum) \
  private(iy, ix, ind)
    for (iy = 0; iy < ny; iy++) {
      ind = nx * iy;
      if (dimens == 3) {
        for (ix = 0; ix < nx; ix++) 
          vol[outZ][ind++] = (unsigned char)
            ((zKernel[0] * useSlice[0][ind] + zKernel[1] * useSlice[1][ind] +
              zKernel[2] * useSlice[2][ind] + halfSum)  / kernelSum);
      } else if (dimens == 5) {
        for (ix = 0; ix < nx; ix++) 
          vol[outZ][ind++] = (unsigned char)
            ((zKernel[0] * useSlice[0][ind] + zKernel[1] * useSlice[1][ind] +
              zKernel[2] * useSlice[2][ind] + zKernel[3] * useSlice[3][ind] +
              zKernel[4] * useSlice[4][ind] + halfSum)  / kernelSum);
      } else {
        for (ix = 0; ix < nx; ix++) 
          vol[outZ][ind++] = (unsigned char)
            ((zKernel[0] * useSlice[0][ind] + zKernel[1] * useSlice[1][ind] +
              zKernel[2] * useSlice[2][ind] + zKernel[3] * useSlice[3][ind] +
              zKernel[4] * useSlice[4][ind] + zKernel[5] * useSlice[5][ind] +
              zKernel[6] * useSlice[6][ind] + halfSum)  / kernelSum);
      }
    }
    //wallCum += wallTime() - wallStart;

    /* If not at end, roll the slices and get next filtered slice */
    if (outZ >= below && sliceBaseZ + dimens < nz) {
      saveBuf = sliceBuffer[0];
      for (ind = 1; ind < numBuf; ind++)
        sliceBuffer[ind - 1] = sliceBuffer[ind];
      sliceBuffer[numBuf - 1] = saveBuf;
      byteKernelFilter(vol[sliceBaseZ + numBuf], sliceBuffer[numBuf - 1],
                       nx, nx, nx, ny, intKernel, dimens);
      sliceBaseZ++;
    }
  }
  //printf("%d threads, Z sum time %.1f\n", numThreads, 1000.*wallCum);

  for (ind = 0; ind < numBuf; ind++)
    free(sliceBuffer[ind]);
  return 0;
}

/*
 * A byte version of sliceKernelFilter
 */
void byteKernelFilter(unsigned char *array, short *brray, int nxAdim, int nxBdim,
                             int nx, int ny, int *mat, int kdim)
{
  int iyo, ixo, iy, ix;
  int below = kdim / 2, above = kdim - 1 - kdim / 2;
  int nregion, ireg, xstr, xend, aix, aiy, numThreads;
  float sum;

  numThreads = B3DNINT(0.04 * sqrt((double)nx * ny));
  numThreads = numOMPthreads(numThreads);
#pragma omp parallel for num_threads(numThreads) \
  shared(kdim, nx, ny, nxAdim, nxBdim, array, brray, mat, below, above)  \
  private(iyo, ixo, iy, ix, nregion, ireg, xstr, xend, aix, aiy, sum)
  for (iyo = 0; iyo < ny; iyo++) {
    xstr = 0;
    xend = below;
    nregion = 2;
    if (iyo < below || iyo >= ny - above) {
      nregion = 1;
      xend = nx;
    } else {
      for (ixo = below; ixo < nx - above; ixo++) {
        sum = 0.;
        for (iy = 0; iy < kdim; iy++) {
          aiy = iy + iyo - below;
          for (ix = 0; ix < kdim; ix++) {
            aix = ix + ixo - below;
            sum += mat[ix + iy * kdim] * array[aix + aiy * nxAdim];
          }
        }
        brray[ixo + nxAdim * iyo] = sum;
      }
    }
    for (ireg = 0; ireg < nregion; ireg++) {
      for (ixo = xstr; ixo < xend; ixo++) {
        sum = 0.;
        for (iy = 0; iy < kdim; iy++) {
          aiy = iy + iyo - below;
          B3DCLAMP(aiy, 0, ny - 1);
          for (ix = 0; ix < kdim; ix++) {
            aix = ix + ixo - below;
            B3DCLAMP(aix, 0, nx - 1);
            sum += mat[ix + iy * kdim] * array[aix + aiy * nxAdim];
          }
        }
        brray[ixo + nxBdim * iyo] = sum;
      }
      xstr = nx - above;
      xend = nx;
    }
  }
}
