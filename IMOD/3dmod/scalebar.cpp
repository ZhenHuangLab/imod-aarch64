/*
 *  scalebar.cpp - To draw scale bars and manage scale bar dialog
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2019 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#include <qgl.h>
#include "imod.h"
#include "imodv.h"
#include "form_scalebar.h"
#include "scalebar.h"
#include "control.h"
#include "xzap.h"
#include "xxyz.h"
#include "sslice.h"
#include "xcramp.h"
#include "display.h"
#include "dockingdialog.h"
#include "dia_qtutils.h"

// The resident parameters, accessible by other modules
static ScaleBar params = {true, false, 50, 8, false, 0, 20, 20, false, 25, false, 0.,
                          false, false, 100., false, 14, 2, 1.};

static int sNeedNoDia = 0;

// The instance of the dialog
static ScaleBarForm *sbDia = NULL;
static DockingDialog *sTopWin = NULL;

// Open the dialog
void scaleBarOpen()
{
  int dlgClass = IMOD_DIALOG;
  DialogManager *manager = &imodDialogManager;
  if (sbDia) {
    sbDia->raise();
    return;
  }
  if (imodvStandalone()) {
    dlgClass = IMODV_DIALOG;
    manager = &imodvDialogManager;
  }
  sTopWin = new DockingDialog(manager->parent(dlgClass), manager, "Scale Bar Controls", 
                              "scalebar.html#TOP", 'e', Qt::Window);
  sbDia = new ScaleBarForm(sTopWin, 0);
  if (!sbDia) {
    wprint("/aCould not open Scale Bar dialog\n");
    return;
  }
  sTopWin->addWidgetToStack(sbDia);
  if (imodvStandalone())
    setModvDialogTitle(sTopWin, "Scale Bar");
  else
    sTopWin->setWindowTitle(imodCaption("Scale Bar"));

  scaleBarRedraw();
  scaleBarUpdate();
  sTopWin->adjustSize();
  sTopWin->show();
  manager->add((QWidget *)sTopWin, dlgClass, DOCKING_DIALOG_TYPE);
}

// Process closing event
void scaleBarClosing()
{
  DialogManager *manager = imodvStandalone() ? &imodvDialogManager :
    &imodDialogManager;  
  manager->remove(sTopWin);
  sbDia = NULL;
  sTopWin = NULL;
  scaleBarRedraw();
}

// Enable bar without dialog open - keep a count to allow multiple callers
void setScaleBarWithoutDialog(bool enable)
{
  sNeedNoDia += enable ? 1 : -1;
  sNeedNoDia = B3DMAX(0, sNeedNoDia);
}

/*
 * Assess the scale bar length and start and size for pixel drawing 
 */
float scaleBarAssess(int winx, int winy, float zoom, int &pixlen, int &xst,
                     int &yst, int &xsize, int &ysize)
{
  Imod *imod;
  double expon, minlen, loglen, normlen, custlen;
  float truelen, pixsize;
  if (!params.draw || !(sbDia || sNeedNoDia))
    return -1.;

  imod = imodvStandalone() ? Imodv->imod : App->cvi->imod;
  pixsize = imod->pixsize * (imodvStandalone() ? 1. : App->cvi->xybin);

  // Get minimum length in units, then reduce that to number between 0 and 1.
  minlen = pixsize * params.minLength / zoom;
  loglen = log10(minlen);
  expon = floor(loglen);
  normlen = pow(10., loglen - expon);
  
  if (params.useCustom) {

    // If a custom length is specified, just use it; adjust by 10 either way
    custlen = params.customVal / 10.;
    if (custlen < normlen)
      custlen *= 10.;
    if (custlen >= 10. * normlen)
      custlen /= 10.;
    normlen = custlen;
  } else {

    // Otherwise set to next higher standard number
    if (normlen < 2.)
      normlen = 2.;
    else if (normlen < 5.)
      normlen = 5.;
    else
      normlen = 10.;
  }

  // Get real length then pixel length, starting points
  if (params.useExact)
    truelen = params.exactVal;
  else
    truelen = (float)normlen * pow(10., expon);
  pixlen = B3DNINT(truelen * zoom / pixsize);
  xsize = params.vertical ? params.thickness : pixlen;
  ysize = params.vertical ? pixlen : params.thickness;
  xst = params.indentX;
  if (params.position == 0 || params.position == 3)
    xst = winx - params.indentX - xsize;
  yst = params.indentY;
  if (params.position == 2 || params.position == 3)
    yst = winy - params.indentY - ysize;
  //imodPrintStderr("SBA: minLength  %d  indentX  %d  normlen %f truelen %f zoom %f\n", params.minLength,
  //              params.indentX, normlen, truelen, zoom);
  params.lastLength = truelen;
  return truelen;
}

/*
 * Test size of scale bar for a montage snapshot and adjust to fit in one
 * panel
 */
void scaleBarTestAdjust(int winx, int winy, float zoom)
{
  int pixlen,barXst,barYst, barXsize, barYsize, minIndent, lengthLim;
  float truelen;
  truelen = scaleBarAssess(winx, winy, zoom, pixlen,
                           barXst, barYst, barXsize, barYsize);
  if (truelen > 0 && (barXst < 0 || barYst < 0 || barXst + barXsize >= 
                      winx || barYst + barYsize >= winy)) {
          
    // Need to get the bar all in one panel: first adjust the indent
    minIndent = B3DMIN(params.indentX, params.indentY);
    while ((barXst < 0 || barYst < 0 || barXst + barXsize >= winx ||
            barYst + barYsize >= winy) && 
           (params.indentX >minIndent || params.indentY > minIndent)) {
      if (params.indentX > minIndent)
        params.indentX--;
      if (params.indentY > minIndent)
        params.indentY--;
      scaleBarAssess(winx, winy, zoom, pixlen, barXst,
                     barYst, barXsize, barYsize);
    }
    lengthLim = params.minLength / 2;
    while ((barXst < 0 || barYst < 0 || barXst + barXsize >= winx ||
            barYst + barYsize >= winy) && params.minLength > lengthLim) {
      params.minLength--;
      scaleBarAssess(winx, winy, zoom, pixlen, barXst,
                     barYst, barXsize, barYsize);
    }
    if (barXst < 0 || barYst < 0 || barXst + barXsize >= winx ||
        barYst + barYsize >= winy) {
      imodPrintStderr("Scale bar cannot be adjusted to fit in one "
                      "panel\n");
      params.draw = false;
    } else
      imodPrintStderr("Scale bar position or size was adjusted to fit "
                      "in one panel\n");
  }
}

/*
 * Draw a scale bar for a window, called from inside its paint routine
 */
float scaleBarDraw(int winx, int winy, float zoom, int background, QGLWidget *GLw,
                   float aDevicePixelRatio)
{
  float truelen;
  int xst, yst, color, pixlen, xsize, ysize, i, j, red, green, blue, index;
  int labelYtop, labelXmid;
  GLboolean depthEnabled;
  Imod *imod = imodvStandalone() ? Imodv->imod : App->cvi->imod;

  truelen = scaleBarAssess(winx, winy, zoom, pixlen, xst, yst, xsize, ysize);
  if (truelen < 0)
    return truelen;
  /*imodPrintStderr("Actual zoom in draw call %f   truelen  %f\n", zoom, truelen);
    imodPrintStderr("SBD: %d %d %d %d %d %d %d\n", winx, winy, pixlen, xst, yst, xsize, ysize);*/

  // Disable depth test and enable at end
  depthEnabled = glIsEnabled(GL_DEPTH_TEST);
  if (depthEnabled)
    glDisable(GL_DEPTH_TEST);

  // If a background color is set, take the opposite; otherwise follow option
  color = params.white ? 255 : 0;
  if (background)
    color = background > 0 ? 0 : 255;

  if (!params.colorRamp) {
    customGhostColor(color, color, color);
    b3dDrawFilledRectangle(xst, yst, xsize, ysize);
  } else {
    
    // Drawing a color ramp
    pixlen = B3DMAX(1, pixlen);
    for (i = 0; i <= pixlen; i++) {
      j = params.invertRamp ? pixlen - i : i;
      index = B3DNINT((255. * j) / pixlen);
      xcramp_mapfalsecolor(index, &red, &green, &blue);
      customGhostColor(red, green, blue);
      if (params.vertical)
        b3dDrawLine(xst, yst + i, xst + xsize, yst + i);
      else
        b3dDrawLine(xst + i, yst, xst + i, yst + ysize);
    }
  }

  // To draw a label, first set the color if a color ramp was done
  if (params.drawLabels && GLw) {
    if (params.colorRamp)
      customGhostColor(color, color, color);

    // Get the font and scale it
    QFont *labelFont = new QFont(QApplication::font());
    if (params.labelSize) {
        labelFont->setPointSizeF(params.scaleLabel * params.labelSize);
    } else if (params.scaleLabel > 1.) {
      if (labelFont->pointSizeF() > 0)
        labelFont->setPointSizeF(params.scaleLabel * labelFont->pointSizeF());
      else
        labelFont->setPixelSize(B3DNINT(params.scaleLabel * labelFont->pixelSize()));
    }

    // Get metrics and figure out how to place center of text below center of bar
    QFontMetrics metrics(*labelFont);
    XY_DEVICE_TO_PIXEL(a, xst + xsize / 2, 
                       winy - (yst - B3DNINT(params.labelYoffset * params.scaleLabel)),
                       labelXmid, labelYtop);
    QString label;
    label.sprintf("%g %s", truelen, imodUnits(imod));

    // The bounding rectangle has the coordinates if drawing was at 0,0, which puts the
    // top above 0,0 at negative Y
    QRect rect = metrics.boundingRect(label);
    GLw->renderText(labelXmid - (rect.left() + rect.width() / 2),
                    labelYtop - rect.top(), label, *labelFont);
    delete labelFont;
  }

  resetGhostColor();
  if (depthEnabled)
    glEnable(GL_DEPTH_TEST);

  // Start timer every time this routine draws a bar so updates occur
  if (sbDia)
    sbDia->startUpdateTimer();
  return truelen;
}

/*
 * Update the dialog's listing of scale bars for each kind of window
 */
void scaleBarUpdate()
{
  Imod *imod = imodvStandalone() ? Imodv->imod : App->cvi->imod;
  float zapLen, slicerLen, xyzLen, multiZlen, modvLen;
  if ((!imodvStandalone() && App->cvi->loadingImage) || !imod)
    return;
  scaleBarAllLengths(zapLen, slicerLen, xyzLen, multiZlen, modvLen);
  if (!sbDia)
    return;
  sbDia->updateValues(zapLen, multiZlen, slicerLen, xyzLen, modvLen, imodUnits(imod));
}

/*
 * Get the lengths of all scale bars (exported function declared in imodview.h)
 */
void scaleBarAllLengths(float &zapLen, float &slicerLen, float &xyzLen, float &multiZlen,
                        float &modvLen)
{
  SlicerFuncs *ss;
  ZapFuncs *zap;
  slicerLen = zapLen = multiZlen = modvLen = xyzLen = -1.;
  if (!(sbDia || sNeedNoDia))
    return;

  if (!imodvStandalone()) {
    ss = getTopSlicer();
    if (ss)
      slicerLen = ss->mScaleBarSize;
    zap = getTopZapWindow(false, false, ZAP_WINDOW_TYPE);
    if (zap)
      zapLen = zap->mScaleBarSize;
    zap = getTopZapWindow(false, false, MULTIZ_WINDOW_TYPE);
    if (zap)
      multiZlen = zap->mScaleBarSize;
    xyzLen = xyzScaleBarSize();
  }
  if (!ImodvClosed)
    modvLen = Imodv->scaleBarSize;
}

ScaleBar *scaleBarGetParams()
{
  return &params;
}

// Dialog change calls this to redraw
void scaleBarRedraw()
{
  if (!imodvStandalone() && App->cvi->loadingImage)
    return;
  if (!imodvStandalone())
    imodDraw(App->cvi, IMOD_DRAW_MOD | IMOD_DRAW_SKIPMODV);
  imodv_draw();
}

