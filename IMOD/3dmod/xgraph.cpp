/*  
 *  xgraph.cp -- Imod graph window.
 *
 *  Original author: James Kremer
 *  Revised by: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2019 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#include <math.h>
#include <stdlib.h>
#include <qlabel.h>
#include <qapplication.h>
#include <qlayout.h>
#include <qbitmap.h>
#include <qcombobox.h>
#include <qsignalmapper.h>
#include <qtoolbutton.h>
#include <qfont.h>
#include <qfile.h>
#include <qspinbox.h>
#include <qtooltip.h>
#include <qtoolbutton.h>
#include <qpushbutton.h>
#include <qtextstream.h>
//Added by qt3to4:
#include <QHBoxLayout>
#include <QKeyEvent>
#include <QGridLayout>
#include <QFrame>
#include <QMouseEvent>
#include <QVBoxLayout>
#include <QCloseEvent>
#include "arrowbutton.h"
#include "xgraph.h"

#include "imod.h"
#include "xzap.h"
#include "imod_input.h"
#include "imod_edit.h"
#include "info_cb.h"
#include "display.h"
#include "pyramidcache.h"
#include "b3dgfx.h"
#include "preferences.h"
#include "dia_qtutils.h"

#define XGRAPH_WIDTH 320
#define XGRAPH_HEIGHT 160
#define BM_WIDTH 16
#define BM_HEIGHT 16

static void graphClose_cb(ImodView *vi, void *client, int junk);
static void graphDraw_cb(ImodView *vi, void *client, int drawflag);
static void graphKey_cb(ImodView *vi, void *client, int released, QKeyEvent *e);

static const char *sFileList[MAX_GRAPH_TOGGLES][2] =
  { {":/images/lowres.png", ":/images/highres.png"},
    {":/images/unlock.png", ":/images/lock.png"}};

static QIcon *sIcons[MAX_GRAPH_TOGGLES];
static int sFirstTime = 1;
static const char *sToggleTips[] = {"Display file values instead of scaled bytes",
                             "Lock X/Y/Z position being displayed"};
static QIcon *sExportIcon;

enum {GRAPH_XAXIS = 0, GRAPH_YAXIS, GRAPH_ZAXIS, GRAPH_CONTOUR, 
      GRAPH_HISTOGRAM};

// Open a graph dialog
int xgraphOpen(struct ViewInfo *vi)
{
  GraphWindow *xg;

  xg = new GraphWindow(vi, App->rgba, App->doublebuffer, App->qtEnableDepth, 
                       imodDialogManager.parent(IMOD_IMAGE));
  if (!xg){
    wprint("Error opening graph window.");
    return(-1);
  }

  if (!App->rgba)
    xg->mGLw->setColormap(*(App->qColormap));

  xg->setWindowTitle(imodCaption("3dmod Graph"));

  xg->mCtrl = ivwNewControl(vi, graphDraw_cb, graphClose_cb, graphKey_cb,
			    (void *)xg);
  imodDialogManager.add((QWidget *)xg, IMOD_IMAGE, GRAPH_WINDOW_TYPE, xg->mCtrl);

  if (App->devPixVaries)
    xg->show();
  imod_info_input();
  utilInitializeScreenChange(xg, xg->mDevicePixelRatio);
  xg->mZoom = utilUnitZoomForDeviceScaling(xg->mDevicePixelRatio);
  QSize size = xg->sizeHint();
  xg->resize(size.width(), (int)(0.65 * size.width()));
  adjustGeometryAndShow((QWidget *)xg, IMOD_IMAGE, false);

  return(0);
}

// The close signal back from the controller
static void graphClose_cb(ImodView *vi, void *client, int junk)
{
  GraphWindow *xg = (GraphWindow *)client;
  xg->close();
}

// The draw signal from the controller
static void graphDraw_cb(ImodView *vi, void *client, int drawflag)
{
  GraphWindow *xg = (GraphWindow *)client;
  if (xg->mClosing)
    return;

  if (drawflag & IMOD_DRAW_COLORMAP) {
    xg->mGLw->setColormap(*(App->qColormap));
    return;
  }

  if (drawflag & IMOD_DRAW_XYZ){
    if (!xg->mLocked){
      if ((xg->mXcur != xg->mVi->xmouse) ||
          (xg->mYcur != xg->mVi->ymouse) ||
          (xg->mZcur != xg->mVi->zmouse)){
        xg->draw();
        return;
      }
    }
  }

  if (drawflag & (IMOD_DRAW_ACTIVE | IMOD_DRAW_IMAGE)){
    xg->draw();
    return;
  }

  if ((drawflag & IMOD_DRAW_MOD) && xg->mAxis == GRAPH_CONTOUR){
    xg->draw();
  }
}

static void graphKey_cb(ImodView *vi, void *client, int released,
			QKeyEvent *e)
{
  GraphWindow *xg = (GraphWindow *)client;
  xg->externalKeyEvent (e, released);
}

/*
 * IMPLEMENTATION OF THE GraphWindow CLASS
 *
 * Constructor to build the window
 */
GraphWindow::GraphWindow(ImodView *vi, bool rgba, bool doubleBuffer, bool enableDepth,
                         QWidget * parent, Qt::WindowFlags f)
  : QMainWindow(parent, f)
{
  int j;
  ArrowButton *arrow;
  mVi      = vi;
  mAxis    = 0;
  mZoom    = utilUnitZoomForDeviceScaling();
  mData    = NULL;
  mDataSize   = 0;
  mAllocSize = 0;
  mLocked  = 0;
  mHighRes = 0;
  mNumLines = 1;
  mMean = 0.;
  mClosing = 0;
  mDevicePixelRatio = 0;

  setAttribute(Qt::WA_DeleteOnClose);
  setAttribute(Qt::WA_AlwaysShowToolTips);
  setAnimated(false);

  if (sFirstTime)  {
    utilFileListsToIcons(sFileList, sIcons, MAX_GRAPH_TOGGLES);
    sExportIcon = new QIcon();
    sExportIcon->addFile(QString(":/images/exportToFile.png"), 
                         QSize(BM_WIDTH, BM_HEIGHT));
  }
  sFirstTime = 0;

  // Make central vbox and top frame containing an hbox
  QWidget *central = new QWidget(this);
  setCentralWidget(central);
  QVBoxLayout *cenlay = new QVBoxLayout(central);
  cenlay->setContentsMargins(0,0,0,0);
  cenlay->setSpacing(0);
  QFrame * topFrame = new QFrame(central);
  cenlay->addWidget(topFrame);
  topFrame->setFrameStyle(QFrame::Raised | QFrame::StyledPanel);

  // Life lessons!  The frame needs a layout inside it; just putting a box
  // in it does not do the trick, and it asserts no size
  QHBoxLayout *topLayout = new QHBoxLayout(topFrame);
  topLayout->setContentsMargins(2,2,2,2);
  topLayout->setSpacing(1);

  // Add the toolbar widgets
  // Zoom arrows
  arrow = new ArrowButton(Qt::UpArrow, topFrame);
  topLayout->addWidget(arrow);
  arrow->setAutoRaise(TB_AUTO_RAISE);
  arrow->setFocusPolicy(Qt::NoFocus);
  connect(arrow, SIGNAL(clicked()), this, SLOT(zoomUp()));
  arrow->setToolTip("Increase scale along the pixel axis");
  arrow = new ArrowButton(Qt::DownArrow, topFrame);
  topLayout->addWidget(arrow);
  arrow->setAutoRaise(TB_AUTO_RAISE);
  arrow->setFocusPolicy(Qt::NoFocus);
  connect(arrow, SIGNAL(clicked()), this, SLOT(zoomDown()));
  arrow->setToolTip("Decrease scale along the pixel axis");

  // Make the 2 toggle buttons and their signal mapper
  QSignalMapper *toggleMapper = new QSignalMapper(topFrame);
  connect(toggleMapper, SIGNAL(mapped(int)), this, SLOT(toggleClicked(int)));
  for (j = 0; j < 2; j++) {
    utilSetupToggleButton(topFrame, NULL, topLayout, toggleMapper, sIcons, 
                          sToggleTips, mToggleButs, mToggleStates, j);
    connect(mToggleButs[j], SIGNAL(clicked()), toggleMapper, SLOT(map()));
  }

  mToggleButs[0]->setEnabled(App->cvi->noReadableImage == 0);
  
  QToolButton *button = new QToolButton(topFrame);
  topLayout->addWidget(button);
  button->setAutoRaise(TB_AUTO_RAISE);
  button->setFocusPolicy(Qt::NoFocus);
  button->setIcon(*sExportIcon);
  connect(button, SIGNAL(clicked()), this, SLOT(exportToFile()));
  button->setToolTip("Export the graph to a text file");

  // The axis combo box
  QComboBox *axisCombo = new QComboBox(topFrame);
  topLayout->addWidget(axisCombo);
  axisCombo->addItem("X-axis");
  axisCombo->addItem("Y-axis");
  axisCombo->addItem("Z-axis");
  axisCombo->addItem("Contour");
  if (!App->cvi->pyrCache)
    axisCombo->addItem("Histogram");
  axisCombo->setFocusPolicy(Qt::NoFocus);
  connect(axisCombo, SIGNAL(currentIndexChanged(int)), this, SLOT(axisSelected(int)));
  axisCombo->setToolTip("Select axis to graph");

  mWidthBox = (QSpinBox *)diaLabeledSpin(0, 1., 100., 1., "Width", topFrame, topLayout);
  mWidthBox->setValue(1);
  connect(mWidthBox, SIGNAL(valueChanged(int)), this, SLOT(widthChanged(int)));
  mWidthBox->setToolTip("Set number of lines to average over");

  mMeanLabel = diaLabel(" 0.0000", topFrame, topLayout);
  
  // Help button
  topLayout->addStretch();
  QPushButton *pbutton = diaPushButton("Help", topFrame, topLayout);
  diaSetButtonWidth(pbutton, ImodPrefs->getRoundedStyle(), 1.2, "Help");
  connect(pbutton, SIGNAL(pressed()), this, SLOT(help()));
  topLayout->addStretch();

  // Now a grid layout in
  QGridLayout *layout = new QGridLayout();
  cenlay->addLayout(layout);
  QVBoxLayout *leftBox = new QVBoxLayout();
  layout->addLayout(leftBox, 0, 0);
  QWidget *spacer = new QWidget(topFrame);
  layout->addWidget(spacer, 1, 0);
  QHBoxLayout *botBox = new QHBoxLayout();
  layout->addLayout(botBox, 1, 1);
  layout->setRowStretch(0, 1);
  layout->setColumnStretch(1, 1);

  // A frame for the graph widget, and a layout inside it, and the GL widget
  QFrame *graphFrame = new QFrame(central);
  graphFrame->setFrameStyle(QFrame::Sunken | QFrame::StyledPanel);
  layout->addWidget(graphFrame, 0, 1);
  QVBoxLayout *graphLayout = new QVBoxLayout(graphFrame);
  graphLayout->setContentsMargins(2,2,2,2);
  QGLFormat glFormat;
  glFormat.setRgba(rgba);
  glFormat.setDoubleBuffer(doubleBuffer);
  glFormat.setDepth(enableDepth);
  mGLw = new GraphGL(this, glFormat, graphFrame);
  graphLayout->addWidget(mGLw);

  // Get a bigger font for the labels
  float font_scale = 1.25;
  QFont newFont = QApplication::font();
  float pointSize = newFont.pointSizeF();
  if (pointSize > 0) {
    newFont.setPointSizeF(pointSize * font_scale);
  } else {
    int pixelSize = newFont.pixelSize();
    newFont.setPixelSize((int)floor(pixelSize * font_scale + 0.5));
  }

  // Get the labels, give them the bigger font
  mPlabel1 = diaLabel(" ", central, botBox);
  mPlabel1->setFont(newFont);
  mPlabel1->setAlignment(Qt::AlignLeft | Qt::AlignTop);
  mPlabel2 = diaLabel(" ", central, botBox);
  mPlabel2->setFont(newFont);
  mPlabel2->setAlignment(Qt::AlignCenter | Qt::AlignTop);
  mPlabel3 = diaLabel(" ", central, botBox);
  mPlabel3->setFont(newFont);
  mPlabel3->setAlignment(Qt::AlignRight | Qt::AlignTop);

  mVlabel1 = diaLabel("-88888", central, leftBox);
  mVlabel1->setFont(newFont);
  mVlabel1->setAlignment(Qt::AlignRight | Qt::AlignTop);
  mVlabel2 = diaLabel(" ", central, leftBox);
  mVlabel2->setFont(newFont);
  mVlabel2->setAlignment(Qt::AlignRight | Qt::AlignBottom);

  QSize hint = mVlabel1->sizeHint();
  mVlabel1->setMinimumWidth(hint.width() + 5);

  resize(XGRAPH_WIDTH, XGRAPH_HEIGHT);
  setFocusPolicy(Qt::StrongFocus);
}

/* 
 * SLOTS FOR GRAPHWINDOW
 *
 * Zoom up and down
 */
void GraphWindow::zoomUp()
{
  mZoom = b3dStepPixelZoom(mZoom, 1);
  draw();
}

void GraphWindow::zoomDown()
{
  mZoom = b3dStepPixelZoom(mZoom, -1);
  draw();
}

void GraphWindow::help()
{
  imodShowHelpPage("graph.html#TOP");
}

// Toggle button
void GraphWindow::toggleClicked(int index)
{
  int state = mToggleButs[index]->isChecked() ? 1 : 0;
  mToggleStates[index] = state;
  if (!index) {

    // High res button toggled
    mHighRes = state;
    draw();
  } else {

    // Lock button toggled: draw if unlocking
    mLocked = state;
    if (!state)
      draw();
  }
}

// Axis selection
void GraphWindow::axisSelected(int item)
{
  mAxis = item;
  mWidthBox->setEnabled(item != GRAPH_ZAXIS && item != GRAPH_HISTOGRAM);
  draw();
}

// For the program to set toggle states
void GraphWindow::setToggleState(int index, int state)
{
  mToggleStates[index] = state ? 1 : 0;
  diaSetChecked(mToggleButs[index], state != 0);
}

// Width change
void GraphWindow::widthChanged(int value)
{
  if (mClosing)
    return;
  mNumLines = value;
  draw();
  setFocus();
}

void GraphWindow::exportToFile()
{
  int ind;
  QString str;
  QString filename = imodPlugGetSaveName(this, "Text file to export graph values into:");
  if (filename.isEmpty())
    return;
  QFile file(filename);
  if (!file.open(QIODevice::WriteOnly | QIODevice::Text)) {
    wprint("\aCould not open %s\n", LATIN1(filename));
    return;
  }
  QTextStream stream(&file);
  
  for (ind = 0; ind < mDataSize; ind++) {
    str.sprintf("%6d %12.6g", ind + mSubStart, mData[ind]);
    stream << str << "\n";
  }
  file.close();
}

#ifdef WATCH_DPI_CHANGE
void GraphWindow::screenChanged(QScreen *screen)
{
  float scaling, newDPR = utilGetNewDevPixRatio(this);
  if (newDPR) {

    // For some reason this is the one window that needed this trick instead of just
    // pumping the assumed size into the resize function
    scaling = newDPR / mDevicePixelRatio;
    mDevicePixelRatio = newDPR;
    if (fabs(scaling - 1.0) > 0.05) {
      mZoom *= scaling;
#ifdef Q_OS_MACX
      resize(width() + 1, height());
      imod_info_input();
      resize(width() - 1, height());
#endif      
    }
  }
}
#endif

/*
 * EVENT RESPONSES
 *
 * Key press: close on Escape, zoom up or down on =/-, and pass on others
 */
void GraphWindow::keyPressEvent ( QKeyEvent * e )
{
  ivwControlPriority(mVi, mCtrl);
  int key = e->key();
  if (utilCloseKey(e))
    close();

  else if (key == Qt::Key_Equal || key == Qt::Key_Plus)
    zoomUp();

  else if (key == Qt::Key_Minus)
    zoomDown();

  else
    inputQDefaultKeys(e, mVi);
}

// Pass on a key press to event processor
void GraphWindow::externalKeyEvent ( QKeyEvent * e, int released)
{
  if (!released)
    keyPressEvent(e);
}

// When close event comes in, clean up and accept the event
void GraphWindow::closeEvent ( QCloseEvent * e )
{
  mClosing = 1;
  ivwRemoveControl(mVi, mCtrl);
  imodDialogManager.remove((QWidget *)this);
  if (mData)
    free(mData);
  e->accept();
}

void GraphWindow::changeEvent(QEvent *e)
{
  QMainWindow::changeEvent(e);
  ivwCheckAndSetMacMenu(e, this, IMOD_IMAGE);
  if (e->type() != QEvent::FontChange)
    return;
}

/*
 * DRAWING ROUTINES 
 *
 * The called drawing routine just calls an update on the GL widget
 */
void GraphWindow::draw()
{
  mGLw->updateGL();
}

// Allocate or reallocate the data array to the correct size and zero it out
int GraphWindow::allocDataArray(int dsize)
{
  int i;
  if (dsize > mAllocSize) {
    if (mData)
      free(mData);
    mData  = (float *)malloc(dsize * sizeof(float));
    if (!mData) 
      return 1;
    mAllocSize = dsize;
  }
  mDataSize = dsize;
  for (i = 0; i < dsize; i++)
    mData[i] = 0.0f;
  return 0;
}

// Fill the data structure for drawing
void GraphWindow::fillData()
{
  int dsize;
  unsigned char **image = NULL;
  b3dUInt16 **usimage;
  unsigned char *bmap;
  int cx, cy, cz, i, j, jy, nlines;
  int ixStart, iyStart, nxUse, nyUse, ixEnd, iyEnd;
  int cp;
  Icont *cont;
  Ipoint *pt1, *pt2, *pts;
  unsigned char **imdata;
  int   pt;
  int curpt, vecpt, istr, iend, skipStart, skipEnd;
  float frac, totlen, curint, dx, dy, smin, smax;
  Ipoint scale, startPt, endPt, pmin, pmax;
  double sum, lensq;
  ImodView *vi = mVi;
  int cacheInd = vi->pyrCache ? vi->pyrCache->getBaseIndex() : -1;
  bool highRes = mHighRes && vi->image->file != IIFILE_JPEG;

  if (!vi)
    return;

  if (! (mData))
    mAllocSize = 0;

  mXcur = vi->xmouse;
  cx = (int)mXcur;
  mYcur = vi->ymouse;
  cy = (int)mYcur;
  mZcur = vi->zmouse;
  cz = (int)(mZcur + 0.5);

  ixStart = 0;
  iyStart = 0;
  nxUse = vi->xsize;
  nyUse = vi->ysize;
  zapSubsetLimits(vi, ixStart, iyStart, nxUse, nyUse);
  if (mAxis != GRAPH_HISTOGRAM && !highRes && cacheInd < 0) {
    if (ivwSetupFastAccess(vi, &imdata, 0, &istr, vi->curTime))
      return;
  }

  switch(mAxis){
  case GRAPH_XAXIS:
    dsize = nxUse;
    mSubStart = ixStart;
    if (allocDataArray(dsize))
      return;

    /* DNM: skip out if outside limits */
    if (cz < 0 || cz >= vi->zsize || cy < 0 || cy >= vi->ysize)
      break;

    if (cx < ixStart || cx > ixStart + nxUse - 1) {
      cx = mXcur = vi->xmouse = 
        B3DMIN(ixStart + nxUse - 1, B3DMAX(ixStart, cx));
      imodDraw(vi, IMOD_DRAW_XYZ);
    }
    mCenterPt = cx;

    // For tile cache, get the tiles needed from base cache and then set up fast access
    if (cacheInd >= 0 && !highRes) {
      vi->pyrCache->loadTilesContainingArea(cacheInd, ixStart, cy - mNumLines / 2 - 1,
                                            dsize, mNumLines + 3, cz);
      if (ivwSetupFastTileAccess(vi, cacheInd, 0, istr))
        return;
    }

    nlines = 0;
    for (j = 0; j < mNumLines; j++) {
      jy = cy + j - (mNumLines - 1) / 2;
      if (jy < 0 || jy >= vi->ysize)
        continue;
      nlines++;
      if (highRes)
        for(i = 0; i < dsize; i++)
          mData[i] += ivwGetFileValue(vi, i + ixStart, jy, cz);
      else
        for(i = 0; i < dsize; i++)
          mData[i] += ivwFastGetValue(i + ixStart, jy, cz);
    }
    if (nlines > 1)
      for(i = 0; i < dsize; i++)
        mData[i] /= nlines;
    break;


  case GRAPH_YAXIS:
    dsize = nyUse;
    mSubStart = iyStart;
    if (allocDataArray(dsize))
      return;

    /* DNM: skip out if outside limits */
    if (cx < 0 || cx >= vi->xsize || cz < 0 || cz >= vi->zsize)
      break;

    if (cy < iyStart || cy > iyStart + nyUse - 1) {
      cy = mYcur = vi->ymouse = 
        B3DMIN(iyStart + nyUse - 1, B3DMAX(iyStart, cy));
      imodDraw(vi, IMOD_DRAW_XYZ);
    }
    mCenterPt = cy;

    // For tile cache, get the tiles needed from base cache and then set up fast access
    if (cacheInd >= 0 && !highRes) {
      vi->pyrCache->loadTilesContainingArea(cacheInd, cx - mNumLines / 2 - 1, iyStart, 
                                            mNumLines + 3, dsize, cz);
      if (ivwSetupFastTileAccess(vi, cacheInd, 0, istr))
        return;
    }

    nlines = 0;
    for (j = 0; j < mNumLines; j++) {
      jy = cx + j - (mNumLines - 1) / 2;
      if (jy < 0 || jy >= vi->xsize)
        continue;
      nlines++;
      if (highRes)
        for (i = 0; i < dsize; i++)
          mData[i] += ivwGetFileValue(vi, jy, i + iyStart, cz);
      else
        for (i = 0; i < dsize; i++)
          mData[i] += ivwFastGetValue(jy, i + iyStart, cz);
    }
    if (nlines > 1)
      for(i = 0; i < dsize; i++)
        mData[i] /= nlines;
    break;

  case GRAPH_ZAXIS:
    dsize = vi->zsize;
    if (allocDataArray(dsize))
      return;
    mSubStart = 0;
    mCenterPt = cz;

    /* DNM: skip out if outside limits */
    if (cx < 0 || cx >= vi->xsize || cy < 0 || cy >= vi->ysize)
      break;
    if (cacheInd >= 0 && !highRes) {
      if (ivwSetupFastTileAccess(vi, cacheInd, 0, istr))
        return;
    }
    if (highRes)
      for(i = 0; i < dsize; i++)
        mData[i] = ivwGetFileValue(vi, cx, cy, i);
    else
      for(i = 0; i < dsize; i++)
        mData[i] = ivwFastGetValue(cx, cy, i);
    break;


    /* Contour : DNM got this working properly, and in 3D */
  case GRAPH_CONTOUR:
    cp = mPtCur = vi->imod->cindex.point;
    cont = imodContourGet(vi->imod);
    if (!cont)
      return;
    if (cont->psize < 2) 
      return;
    pts = cont->pts;

    // Analyze for whether contour crosses into and out of a subarea
    skipStart = 0;
    skipEnd = 0;
    totlen = 0.;
    scale.x = 1.0;
    scale.y = 1.0;
    scale.z = 1.0;
    if (nxUse < vi->xsize || nyUse < vi->ysize) {
      ixEnd = ixStart + nxUse - 1;
      iyEnd = iyStart + nyUse - 1;
      
      // Find first point inside box
      for (i = 0; i < cont->psize; i++) {
        if (pts[i].x >= ixStart && pts[i].x <= ixEnd && 
            pts[i].y >= iyStart && pts[i].y <= iyEnd) {
          if (!i)
            break;
          skipStart = i;
          makeBoundaryPoint(pts[i - 1], pts[i], ixStart, ixEnd, iyStart, iyEnd,
                            &startPt);
          totlen += imodPoint3DScaleDistance(&pts[i - 1], &startPt, &scale);
          break;
        }
        if (i)
          totlen += imodPoint3DScaleDistance(&pts[i - 1], &pts[i], &scale);
      }
      

      // If completely outside, skip
      if (i >= cont->psize)
        return;

      // Now search for first point outside box
      for (i++; i < cont->psize; i++) {
        if (!(pts[i].x >= ixStart && pts[i].x <= ixEnd && 
              pts[i].y >= iyStart && pts[i].y <= iyEnd)) {
          skipEnd = i;
          makeBoundaryPoint(pts[i], pts[i - 1], ixStart, ixEnd, iyStart, iyEnd,
                            &endPt);
          break;
        }
      }
    }

    // For tile cache, find limits of contour within area being used and load it if it
    // is planar
    if (cacheInd >= 0 && !highRes) {
      imodContourGetBBox(cont, &pmin, &pmax);
      if (B3DNINT(pmin.z) == B3DNINT(pmax.z)) {
        pmin.x = B3DMAX(pmin.x, ixStart) - mNumLines / 2 - 1;
        pmin.x = B3DMAX(0, pmin.x);
        pmax.x = B3DMIN(pmax.x, ixStart + nxUse) + mNumLines / 2 + 1;
        pmax.x = B3DMIN(pmax.x, vi->xsize - 1);
        pmin.y = B3DMAX(pmin.y, iyStart) - mNumLines / 2 - 1;
        pmin.y = B3DMAX(0, pmin.y);
        pmax.y = B3DMIN(pmax.y, iyStart + nyUse) + mNumLines / 2 + 1;
        pmax.y = B3DMIN(pmax.y, vi->ysize - 1);
        vi->pyrCache->loadTilesContainingArea(cacheInd, pmin.x, pmin.y, pmax.x + 1 - 
                                              pmin.x, pmax.y + 1 - pmin.y, 
                                              B3DNINT(pmin.z));
      }
      if (ivwSetupFastTileAccess(vi, cacheInd, 0, istr))
        return;
    }

    /* Get true 3D length, record where current point falls */
    mSubStart = (int)(totlen + 0.5);
    totlen = 0.;
    if (cp < 0) 
      cp = 0;
    if (cp >= (int)cont->psize) 
      cp = (int)cont->psize - 1;
    mCenterPt = mSubStart;

    pt1 = cont->pts;
    if (skipStart)
      pt1 = &startPt;
    istr = skipStart ? skipStart : 1;
    iend = skipEnd ? skipEnd + 1 : cont->psize;
    for (i = istr; i < iend; i++) {
      pt2 = &cont->pts[i];
      if (i == skipEnd)
        pt2 = &endPt;
      totlen += imodPoint3DScaleDistance(pt1, pt2, &scale);
      if (i == cp)
        mCenterPt = (int)(totlen + 0.5) + mSubStart;
      pt1 = pt2;
    }
    if (cp >= iend)
      mCenterPt = (int)(totlen + 0.5) + mSubStart;

    dsize = (int)(totlen + 1.0);
    if (allocDataArray(dsize))
      return;
    mXcur = cont->pts[cp].x + 0.5f;
    mYcur = cont->pts[cp].y + 0.5f;
    mZcur = cont->pts[cp].z + 0.5f;

    /* Advance through data points, finding nearest pixel along each line
       of contour */

    totlen = 0.0;
    curint = 0.0;
    curpt = istr;
    vecpt = 0;
    dx = 0.;
    dy = 0.;
    pt2 = cont->pts;
    if (skipStart)
      pt2 = &startPt;
    for (i = 0; i < dsize; i++) {

      /* Advance as needed until i is inside the current interval */

      while (i > totlen + curint || !curint) {
        totlen += curint;
        pt1 = pt2;
        pt2 = &cont->pts[curpt++];
        if (curpt == skipEnd + 1)
          pt2 = &endPt;
        curint = imodPoint3DScaleDistance(pt1, pt2, &scale);
	if (curpt >= iend)
	  break;
      }
      frac = 0;
      if (curint)
        frac = (i - totlen) / curint;

      // Compute delta for cross-averaging
      if (mNumLines > 1 && curpt != vecpt) {
        dx = pt1->y - pt2->y;
        dy = pt2->x - pt1->x;
        lensq = dx * dx + dy * dy;
        if (lensq > 1.e-3) {
          lensq = sqrt(lensq);
          dx /= lensq;
          dy /= lensq;
        } else {
          dx = 0.;
          dy = 1.;
        }
        vecpt = curpt;
      }

      nlines = 0;
      for (jy = -(mNumLines - 1) / 2 ; jy < mNumLines - (mNumLines - 1) / 2;
           jy++) {
        cx = (int)(pt1->x + frac * (pt2->x - pt1->x) + jy * dx + 0.5);
        cy = (int)(pt1->y + frac * (pt2->y - pt1->y) + jy * dy + 0.5);
        cz = (int)(pt1->z + frac * (pt2->z - pt1->z) + 0.5);
        if (cx >= 0 && cx < vi->xsize &&
            cy >= 0 && cy < vi->ysize &&
            cz >= 0 && cz < vi->zsize) {
          nlines++;
          if (highRes)
            mData[i] += ivwGetFileValue(vi, cx, cy, cz);
          else
            mData[i] += ivwFastGetValue(cx, cy, cz);
        }
      }
      if (nlines)
        mData[i] /= nlines;
    }

    break;

  case GRAPH_HISTOGRAM:
    image = ivwGetCurrentZSection(vi);
    usimage = (b3dUInt16 **)image;
    if (image){
      dsize = 256;
      sum = 0.;
      if (allocDataArray(dsize))
        return;
      mSubStart = 0;
      cz = (int)(vi->zmouse + 0.5f);
      if (vi->ushortStore) {
        bmap = ivwUShortInRangeToByteMap(vi);
        if (!bmap)
          return;
        for (j = iyStart; j < iyStart + nyUse; j++) {
          for (i = ixStart; i < ixStart + nxUse; i++) {
            pt = bmap[usimage[j][i]];
            mData[pt] += 1.0f;
            sum += pt;
          }
        }
        mCenterPt = bmap[usimage[cy][cx]];
        free(bmap);
      } else {
        for (j = iyStart; j < iyStart + nyUse; j++) {
          for (i = ixStart; i < ixStart + nxUse; i++) {
            pt = image[j][i];
            mData[pt] += 1.0f;
            sum += pt;
          }
        }
        mCenterPt = image[cy][cx];
      }

      // For high res, too painful to get file mean
      /*if (highRes) {
        sum = 0.;
        for (j = iyStart; j < iyStart + nyUse; j++)
          for (i = ixStart; i < ixStart + nxUse; i++)
            sum += ivwGetFileValue(vi, i, j, cz);
            } */

      // For high res, rescale the sum by the load in scaling, ignoring
      // truncation (ignoring outmin/outmax ...)
      if (highRes) {
        smin = vi->image->smin;
        smax = vi->image->smax;
        if (vi->multiFileZ > 0) {
          smin = vi->imageList[cz + vi->li->zmin].smin;
          smax = vi->imageList[cz + vi->li->zmin].smax;
        }
        if (smin != smax) 
          sum  = sum * (smax - smin) / 255. + smin;
      }
      mMean = sum / (nxUse * nyUse);
    }
    break;

  default:
    break;
  }

  if (mAxis != GRAPH_HISTOGRAM) {
    sum = 0.;
    for (i = 0; i < dsize; i++)
      sum += mData[i];
    mMean = sum / dsize;
  }
}

// Set the text of the Axis labels
void GraphWindow::drawAxis()
{
  QString str;

  // Output integers on the value axis unless we are in high res mode and 
  // the file is not byte or integer
  int mode = mVi->image->mode;
  int floats = !mHighRes || mVi->image->file == IIFILE_JPEG || mode == MRC_MODE_BYTE || 
    mode == MRC_MODE_SHORT || mode == MRC_MODE_USHORT ? 0 : 1;

  if (floats)
    str.sprintf("%9g", mMin);
  else
    str.sprintf("%6d", (int)mMin);
  mVlabel2->setText(str);

  if (floats)
    str.sprintf("%9g", mMax);
  else
    str.sprintf("%6d", (int)mMax);
  mVlabel1->setText(str);

  // Output pixel position axis
  str.sprintf("%d", mStart);
  mPlabel1->setText(str);
  str.sprintf("%d", (mStart + mEnd) / 2);
  mPlabel2->setText(str);
  str.sprintf("%d", mEnd);
  mPlabel3->setText(str);
  str.sprintf(" %.5g", mMean);
  mMeanLabel->setText(str);
}

// Actually draw the data
void GraphWindow::drawPlot()
{
  int spnt, epnt, i;
  int cpntx = mWidth/2;
  float min, max, yoffset, yscale, extra;
  float zoom = mZoom;

  if (!mData)
    return;

  b3dSetCurDevPixRatio(mDevicePixelRatio);
  b3dLineWidth(1);
  spnt = mCenterPt - (int)(cpntx / zoom);
  epnt = mCenterPt + (int)(cpntx / zoom);

  // Give a histogram no more than full range if possible
  if (mAxis == GRAPH_HISTOGRAM) {
    spnt = B3DMAX(0, spnt);
    epnt = B3DMIN(256, epnt);
    zoom = (float)mWidth / (epnt - spnt);
  }

  b3dColorIndex(App->foreground);
  max = -1.e37;
  min = 1.e37;
  for (i = spnt - mSubStart; i < epnt - mSubStart; i++) {
    if (i < 0)
      continue;
    if (i >= mDataSize)
      break;
    if (mData[i] < min)
      min = mData[i];
    if (mData[i] > max)
      max = mData[i];
  }

  // Increase range a bit but keep integer values and keep a min of 0
  extra = 0.02 * (max - min);
  if (extra != (int)extra && (max == (int)max || min == (int)min))
    extra = extra > 0.2 ? (int)(extra + 1.) : 0;
  if (min != 0.)
    min -= extra;
  max += extra;

  mMin = min;
  mMax = max;
  yoffset = min;
  if (max-min)
    yscale = mHeight/(max - min);
  else yscale = 1.0f;

  b3dBeginLine();
  for (i = spnt - mSubStart; (i <= epnt - mSubStart) && (i < mDataSize);
      i++) {
    if (i < 0)
      continue;
    b3dVertex2i((int)((i  + mSubStart - spnt) * zoom),
                (int)((mData[i] - yoffset) * yscale));
  }
  b3dEndLine();

  b3dColorIndex(App->endpoint);
  b3dDrawLine((int)((mCenterPt - spnt) * zoom), 0,
              (int)((mCenterPt - spnt) * zoom), mHeight);

  mOffset = yoffset;
  mScale  = yscale;
  mStart  = spnt;
  mEnd    = epnt;
}

void GraphWindow::makeBoundaryPoint(Ipoint pt1, Ipoint pt2, int ix1, int ix2,
                              int iy1, int iy2, Ipoint *newpt)
{
  float t, tmax;
  // Find the maximum t parameter implied by X intersections, for t within
  // range of being in the segment
  tmax = 0.;
  if (fabs((double)(pt1.x - pt2.x)) > 1.e-4) {
    t = (ix1 - pt1.x) / (pt2.x - pt1.x);
    if (t >=0 && t < 1)
      tmax = t;
    t = (ix2 - pt1.x) / (pt2.x - pt1.x);
    if (t >=0 && t < 1)
      tmax = B3DMAX(t, tmax);
  }

  // Get maximum from t parameters of Y intersections too
  if (fabs((double)(pt1.y - pt2.y)) > 1.e-4) {
    t = (iy1 - pt1.y) / (pt2.y - pt1.y);
    if (t >=0 && t < 1)
      tmax = B3DMAX(t, tmax);
    t = (iy2 - pt1.y) / (pt2.y - pt1.y);
    if (t >=0 && t < 1)
      tmax = B3DMAX(t, tmax);
  }

  newpt->x = tmax * (pt2.x - pt1.x) + pt1.x;
  newpt->y = tmax * (pt2.y - pt1.y) + pt1.y;
  newpt->z = tmax * (pt2.z - pt1.z) + pt1.z;
}

/*
 * The GL WIDGET CLASS: PAINT, RESIZE, MOUSE EVENTS
 */
GraphGL::GraphGL(GraphWindow *graph, QGLFormat format, QWidget * parent)
  : QGLWidget(format, parent)
{
  mGraph = graph;
  mDrawing = false;
}

void GraphGL::paintGL()
{
  if (mDrawing)
    return;
  mDrawing = true;
  if (!mGraph->mLocked)
    mGraph->fillData();

  if (!mGraph->mData)
    return;
  b3dColorIndex(App->background);
  glClear(GL_COLOR_BUFFER_BIT);
  mGraph->drawPlot();
  mGraph->drawAxis();
  mDrawing = false;
}

void GraphGL::resizeGL( int wdth, int hght )
{
  mGraph->mWidth  = wdth;
  mGraph->mHeight = hght;
  b3dResizeViewportXY(wdth, hght);
}

void GraphGL::mousePressEvent(QMouseEvent * e )
{
  int ex, ey;
  XY_PIXEL_TO_DEVICE(mGraph->m, e->x(), e->y(), ex, ey);
  ivwControlPriority(mGraph->mVi, mGraph->mCtrl);
  utilRaiseIfNeeded(mGraph, e);
  if (e->buttons() & ImodPrefs->actualButton(1))
    setxyz(ex, ey);
}

// Routine to change the position on the current axis based upon a mouse click
void GraphGL::setxyz(int mx, int my)
{
  int ni = (int)((mx/mGraph->mZoom) + mGraph->mStart);
  int x,y,z;

  ivwGetLocation(mGraph->mVi, &x, &y, &z);

  switch(mGraph->mAxis){
  case GRAPH_XAXIS:
    x = ni;
    break;
  case GRAPH_YAXIS:
    y = ni;
    break;
  case GRAPH_ZAXIS:
    z = ni;
    break;

  case GRAPH_CONTOUR:
    return;
  case GRAPH_HISTOGRAM:
    return;
  }
  ivwSetLocation(mGraph->mVi, x, y, z);
  ivwControlPriority(mGraph->mVi, mGraph->mCtrl);
  imodDraw(mGraph->mVi, IMOD_DRAW_XYZ);
}
