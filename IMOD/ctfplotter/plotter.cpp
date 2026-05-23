/*
* plotter.cpp - callbacks for the main widget of ctfplotter.
*
*  Authors: Quanren Xiong and David Mastronarde
*
*  Copyright (C) 2008-2019 by the Regents of the University of 
*  Colorado.  See dist/COPYRIGHT for full copyright notice.
* 
*  $Id$
*/

#include "myapp.h"
#include <QPrintDialog>

#include <cmath>

#include "fittingdialog.h"
#include "angledialog.h"
#include "plotter.h"
#include "ctfmain.h"

#include "b3dutil.h"
#include "dia_qtutils.h"

using namespace std;

Plotter::Plotter(MyApp *app, QWidget *parent) : QWidget(parent)
{
  int added = 0, width;
  mApp = app;
  setBackgroundRole(QPalette::Dark);
  setAutoFillBackground(true);
  setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
  setFocusPolicy(Qt::StrongFocus);
  mRubberBandIsShown = false;
  mFittingDia=NULL;
  mAngleDia=NULL;
  mInStartup = true;

  mZoomInButton = new QToolButton(this);
  mZoomInButton->setIcon(QIcon(":/images/zoomin.png"));
  mZoomInButton->adjustSize();
  mZoomInButton->setToolTip("Zoom in");
  connect(mZoomInButton, SIGNAL(clicked()), this, SLOT(zoomIn()));

  mZoomOutButton = new QToolButton(this);
  mZoomOutButton->setIcon( QIcon(":/images/zoomout.png"));
  mZoomOutButton->adjustSize();
  mZoomOutButton->setToolTip("Zoom out");
  connect(mZoomOutButton, SIGNAL(clicked()), this, SLOT(zoomOut()));

  mPrintButton= new QToolButton(this);
  mPrintButton->setIcon( QIcon(":/images/printer.png") );
  mPrintButton->adjustSize();
  mPrintButton->setToolTip("Print");
  connect(mPrintButton, SIGNAL(clicked()), this, SLOT(printIt()) );

  mHelpButton= new QToolButton(this);
  mHelpButton->setIcon( QIcon(":/images/ctfhelp.png") );
  mHelpButton->adjustSize();
  mHelpButton->setToolTip("Open Ctfplotter help");
  connect(mHelpButton, SIGNAL(clicked()), this, SLOT(ctfHelp()) );

  mRangeButton = new QPushButton("Fitting", this);
  mRangeButton->setToolTip( "Open Fitting Ranges and Methods dialog");
  connect(mRangeButton, SIGNAL(clicked()), this, SLOT(openFittingDia()) );

  mAngleButton = new QPushButton("Angles", this);
  mAngleButton->setToolTip("Open Angle Range and Tile Selection dialog");
  connect(mAngleButton, SIGNAL(clicked()), this, SLOT(openAngleDia()) );
#ifdef Q_OS_MACX  
  added = (int)(1.5 * mRangeButton->fontMetrics().height());
#endif
  width = (int)(1.35 * mAngleButton->fontMetrics().width("Angles") +0.5)+added;
  mAngleButton->setFixedWidth(width);
  mRangeButton->setFixedWidth(width);

  mTileButton=new QToolButton(this);
  mTileButton->setIcon(QIcon(":/images/moreTile.png") );
  mTileButton->adjustSize();
  mTileButton->setToolTip("Include all of the rest of the tiles");
  mTileButton->setEnabled(mApp->getInitialCentralTiles());
  connect(mTileButton, SIGNAL(clicked()), mApp, SLOT(moreTileCenterIncluded()));

  mColorCheckBox = diaCheckBox("Alternate colors", this, NULL);
  mColorCheckBox->setChecked(mApp->getChangeColors());
  connect(mColorCheckBox, SIGNAL(toggled(bool)), this, SLOT(colorToggled(bool)));
  diaSetWidgetColor(mColorCheckBox, Qt::white, QPalette::WindowText);
  mColorCheckBox->setToolTip("Switch to colors that are better for some forms of "
                             "color-blindess");

  mZeroLabel=new QLabel( tr("Z: NA       "), this);
  mZeroLabel->adjustSize();
  mDefocusLabel=new QLabel( tr("D: NA      "), this);
  mDefocusLabel->adjustSize();
  mDefoc2Label=new QLabel( tr("D2: NA      "), this);
  mDefoc2Label->adjustSize();
  mDefocAvgLabel=new QLabel( tr("D-avg: NA      "), this);
  mDefocAvgLabel->adjustSize();
  mErrScoreLabel = new QLabel( tr("Error: NA          "), this);
  mErrScoreLabel->adjustSize();
  mAstigDefLabel = new QLabel( tr("Astig: NA        "), this);
  mAstigDefLabel->adjustSize();
  mAstigAxisLabel = new QLabel( tr("Axis: NA        "), this);
  mAstigAxisLabel->adjustSize();
  mWedgeErrLabel = new QLabel( tr("Wedge err: NA        "), this);
  mWedgeErrLabel->adjustSize();
  mPhaseShiftLabel = new QLabel( tr("Phase: NA        "), this);
  mPhaseShiftLabel->adjustSize();
  mCutOnFreqLabel = new QLabel( tr("Cut-on: NA          "), this);
  mCutOnFreqLabel->adjustSize();

  mCurStack = 0;
  mPrinter= new QPrinter;
  mPrinter->setOrientation(QPrinter::Landscape);
  setPlotSettings(PlotSettings());
}

Plotter::~Plotter(){
  delete mPrinter;
}

void Plotter::colorToggled(bool state)
{
  mApp->setChangeColors(state);
  refreshPixmap();
}

void Plotter::setPlotSettings(const PlotSettings &settings)
{
  mZoomStack[mCurStack].clear();
  mZoomStack[mCurStack].append(settings);
  mCurZoom[mCurStack] = 0;
  mZoomInButton->hide();
  mZoomOutButton->hide();
  refreshPixmap();
}

void Plotter::zoomOut()
{
  if (mCurZoom[mCurStack] > 0) {
    --mCurZoom[mCurStack];
    mZoomOutButton->setEnabled(mCurZoom[mCurStack] > 0);
    mZoomInButton->setEnabled(true);
    mZoomInButton->show();
    refreshPixmap();
  }
}

void Plotter::zoomIn()
{
  if (mCurZoom[mCurStack] < mZoomStack[mCurStack].count() - 1) {
    ++mCurZoom[mCurStack];
    mZoomInButton->setEnabled(
                             mCurZoom[mCurStack] < (int)mZoomStack[mCurStack].size() - 1);
    mZoomOutButton->setEnabled(true);
    mZoomOutButton->show();
    refreshPixmap();
  }
}

void Plotter::openFittingDia()
{
  bool newDia = !mFittingDia;
  if(!mFittingDia) {
    mFittingDia=new FittingDialog(mApp, this);
    connect(mFittingDia, SIGNAL( range(double, double, double, double) ), mApp,
            SLOT( rangeChanged(double, double, double, double)) );
    connect(mFittingDia, SIGNAL( x1MethodChosen(int) ), mApp,
            SLOT(setX1Method(int)) );
    connect(mFittingDia, SIGNAL( x2MethodChosen(int) ), mApp,
            SLOT(setX2Method(int)) );    
  }
  mFittingDia->show();
  mFittingDia->raise();
  mFittingDia->activateWindow();
  if (newDia && !mInStartup)
    positionDialog(mFittingDia, mAngleDia);
}

/* 
 * Open the angle dialog, set it with current values
 */
void Plotter::openAngleDia()
{
  QSize hint;
  bool newDia = !mAngleDia;
  int lowNear, highNear, numViews;
  if(!mAngleDia){
    mAngleDia=new AngleDialog(mApp, this);
    connect(mAngleDia, SIGNAL(angle(double,double,double,double,int,double,
                                    double,double,double) ), 
            mApp, SLOT(angleChanged(double,double,double,double,int,double,
                                    double,double,double)));
    connect(mAngleDia, SIGNAL( defocusMethod(int)), mApp, 
            SLOT( setUseCurDefocus(int)) );
    connect(mAngleDia, SIGNAL( initialTileChoice(bool)), mApp,
            SLOT( setInitialCentralTiles(bool)) );
    double expDefocus=mApp->mDefocusFinder.getExpDefocus();
    double lowAngle=mApp->getLowAngle();
    double highAngle=mApp->getHighAngle();
    double defTol=mApp->getDefocusTol();
    int    tSize=mApp->getTileSize();
    double axisAngle=mApp->getAxisAngle();
    double leftTol=mApp->getLeftTol();
    double rightTol=mApp->getRightTol();
    char tmpStr[20];
    sprintf(tmpStr, "%6.2f", expDefocus); 
    mAngleDia->mDefocusEdit->setText(tmpStr);
    sprintf(tmpStr, "%6.1f", mApp->getMidAngleAndRangeIndices
            (mApp->getLowAngle(), mApp->getHighAngle(), lowNear, highNear, numViews)); 
    mAngleDia->mMidAngleEdit->setText(tmpStr);
    sprintf(tmpStr, "%6d", B3DNINT(defTol)); 
    mAngleDia->mDefTolEdit->setText(tmpStr);
    sprintf(tmpStr, "%6d", tSize);
    mAngleDia->mTileSizeEdit->setText(tmpStr);
    sprintf(tmpStr, "%6.2f", axisAngle); 
    mAngleDia->mAxisAngleEdit->setText(tmpStr);
    sprintf(tmpStr, "%6d", B3DNINT(leftTol)); 
    mAngleDia->mLeftTolEdit->setText(tmpStr);
    sprintf(tmpStr, "%6d", B3DNINT(rightTol)); 
    mAngleDia->mRightTolEdit->setText(tmpStr);
    sprintf(tmpStr, "%6.2f", mApp->getAutoFromAngle()); 
    mAngleDia->mAutoFromEdit->setText(tmpStr);
    sprintf(tmpStr, "%6.2f", mApp->getAutoToAngle()); 
    mAngleDia->mAutoToEdit->setText(tmpStr);
    
    mAngleDia->updateTable();
    //mApp->manageAstigMinViews();
 }
  mAngleDia->show();
  mAngleDia->raise();
  mAngleDia->activateWindow();
  if (newDia) {
    mAngleDia->tileParamsClicked();
    if (!mInStartup)
      positionDialog(mAngleDia, mFittingDia);
  }
}

// Start the timer for placing the dialogs after event loop is started
void Plotter::startDlgPlacementTimer()
{
  mTimerID = startTimer(200);
}

/*
 * Do initial placement of the dialogs when the timer is up
 */
void Plotter::timerEvent(QTimerEvent *event)
{
  QMainWindow *mainWin = (QMainWindow *)parent();
  killTimer(mTimerID);
  mFittingDia->adjustSize();
  mAngleDia->adjustDialogSize();
  qApp->processEvents();

  // Determine if there is space to lay the dialogs out, and whether there is space
  // if the plotter is shifted to left or right; move it if it will help
  int deskWidth, deskHeight;
  diaMaximumWindowSize(deskWidth, deskHeight, mainWin);
  QRect plotRect = mainWin->frameGeometry();
  int assumeAngle = mAngleDia->width() + 8;
  int assumeFitting = mFittingDia->width() + 8;
  int leftOpen = plotRect.left(), rightOpen = deskWidth - plotRect.right();
  if (leftOpen + rightOpen > assumeAngle + assumeFitting &&
      !(leftOpen >  assumeAngle + assumeFitting ||
        rightOpen >  assumeAngle + assumeFitting ||
        (leftOpen > assumeAngle && rightOpen > assumeFitting) ||
        (rightOpen > assumeAngle && leftOpen > assumeFitting))) {
    if (leftOpen > rightOpen)
      mainWin->move(deskWidth -(plotRect.width() + 8), plotRect.top());
    else
      mainWin->move(8, plotRect.top());
    qApp->processEvents();
  }
  positionDialog(mAngleDia, NULL);
  positionDialog(mFittingDia, mAngleDia);
  qApp->processEvents();
}

/*
 * Position a dialog being opened so it is beside the plotter window, beside the other 
 * dialog if that is where there is space
 */
void Plotter::positionDialog(QWidget *newDia, QWidget *otherDia)
{
  QMainWindow *mainWin = (QMainWindow *)parent();
  QApplication::processEvents();
  
  QRect plotRect = mainWin->frameGeometry();
  QRect otherRect;
  QRect newRect = newDia->frameGeometry();
  int width = newRect.width() + 8;
  int deskWidth, deskHeight, otherLeft, otherRight = 0;
  int newX = newRect.left(), newY = plotRect.top();
  diaMaximumWindowSize(deskWidth, deskHeight, mainWin);
  otherLeft = deskWidth;
  if (otherDia) {
    otherRect = otherDia->frameGeometry();
    otherLeft = otherRect.left();
    otherRight = otherRect.right();
  }
  if (deskWidth - plotRect.right() >= width && (otherRight <= plotRect.right() || 
                                                otherLeft  - plotRect.right() >= width)) {
    newX = plotRect.right() + 8;
  } else if (plotRect.left() >= width && (otherLeft >= plotRect.left() ||
                                          plotRect.left() - otherRight >= width)) {
    newX = plotRect.left() - width;
  } else if (otherRight > plotRect.right() && deskWidth - otherRight >= width) {
    newX = otherRight + 8;
  } else if (otherLeft < plotRect.left() && otherLeft >= width) {
    newX = otherLeft - width;
  } else {
    newY = newRect.top();
  }
  diaLimitWindowPos(width, newRect.height(), newX, newY, mainWin);
  newDia->move(newX, newY);
}

void Plotter::printIt()
{
  QPrintDialog printDialog(mPrinter, this);
  printDialog.setWindowTitle("Printing plot");

  if ( printDialog.exec() ) {
    QPainter painter;
    if( !painter.begin( mPrinter ) ) return;
    painter.setWindow(0, 0, width(), height() );
    drawGrid(&painter, false);
    drawCurves(&painter);
  }
}

void Plotter::ctfHelp()
{
  ctfShowHelpPage("ctfHelp/ctfguide.html#TOP");
}

void Plotter::setCurveData(int id, const QVector<QPointF> &data)
{
    mCurveMap[id] = data;
   // int n=mCurveMap[id].size() / 2;
   // int i;
   // double min, max;

    //find and reset x, y ranges;
    /*if(n>1 && id==0){
      if( data[0]>data[2] ){
        max=data[0];
        min=data[2];
      }else{
        min=data[0];
        max=data[2];
      }
      for(i=2;i<n;i++){
        if( data[2*i]>max ) max=data[2*i];
        else if( data[2*i]<min ) min=data[2*i];
      }
      if( min<mZoomStack[0].minX ) mZoomStack[0].minX=min;
      if( max>mZoomStack[0].maxX ) mZoomStack[0].maxX=max; 

      if( data[1]>data[3] ){
        max=data[1];
        min=data[3];
      } else{
        min=data[1];
        max=data[3];
      }
      for(i=2;i<n;i++){
        if( data[2*i+1]>max ) max=data[2*i+1];
        else if( data[2*i+1]<min ) min=data[2*i+1];
      }
      if( min<mZoomStack[0].minY ) mZoomStack[0].minY=min;
      if( max>mZoomStack[0].maxY ) mZoomStack[0].maxY=max;
    }*/
    
    refreshPixmap();
}

void Plotter::clearCurve(int id)
{
  mCurveMap.remove(id);
  refreshPixmap();
}

QSize Plotter::minimumSizeHint() const
{
  return QSize(2 * (LeftMargin + RightMargin), 2 * (TopMargin + BottomMargin));
}

QSize Plotter::sizeHint() const
{
  return QSize(13 * (LeftMargin + RightMargin), 16 * (TopMargin + BottomMargin));
}

void Plotter::paintEvent(QPaintEvent *event)
{
  QStylePainter painter(this);
  painter.drawPixmap(0,0, mPixmap);


  if (mRubberBandIsShown) {
    //painter.setPen(palette().light().color());
    painter.setPen(Qt::white);
    painter.drawRect(mRubberBandRect.normalized().adjusted(0,0,-1,-1));

  }
  if (hasFocus()) {
    int gray = mApp->getChangeColors() ? 0 : 64;
    QStyleOptionFocusRect option;
    option.initFrom(this);
    //option.backgroundColor=palette().dark().color();
    option.backgroundColor=QColor(gray, gray, gray);
    painter.drawPrimitive(QStyle::PE_FrameFocusRect, option);
  }
}

void Plotter::resizeEvent(QResizeEvent *)
{
  int base = width() - (mZoomInButton->width() + mTileButton->width() +
                        mZoomOutButton->width() +mPrintButton->width());
  int x = base - (mZeroLabel->width()+ mErrScoreLabel->width() +
                  mDefocusLabel->width() + mDefoc2Label->width() + 
                  mDefocAvgLabel->width() + 70);
  int xsave = x;

  // Put text buttons on left
  mAngleButton->move(10, 5);
  mRangeButton->move(15 + mAngleButton->width(), 5);
  mColorCheckBox->move(10, height() - 25);

  // Then lay out the labels and tool buttons at fixed distance from right edge
  mZeroLabel->move(x, 7);
  x += mZeroLabel->width()+5;
  mDefocusLabel->move(x, 7);
  x += mDefocusLabel->width()+5;
  mDefoc2Label->move(x, 7);
  x += mDefoc2Label->width()+5;
  mDefocAvgLabel->move(x, 7);
  x += mDefocAvgLabel->width()+5;
  mErrScoreLabel->move(x, 7);
  x += mErrScoreLabel->width() + 5;
    
  mZoomInButton->move(x, 5);
  x += mZoomInButton->width() + 5;
  mZoomOutButton->move(x, 5);
  x += mZoomOutButton->width() + 5;
  mTileButton->move(x, 5);
  x += mTileButton->width() + 5;
  mPrintButton->move(x, 5);
  x += mPrintButton->width() + 5;
  mHelpButton->move(x, 5);
  x = base - (mAstigDefLabel->width() + mAstigAxisLabel->width() + mWedgeErrLabel->width()
              + mPhaseShiftLabel->width() + mCutOnFreqLabel->width() + 70);
  x = xsave;
  mAstigDefLabel->move(x, TopMargin - 18);
  x += mAstigDefLabel->width();
  mAstigAxisLabel->move(x, TopMargin - 18);
  x += mAstigAxisLabel->width();
  mWedgeErrLabel->move(x, TopMargin - 18);
  x += mWedgeErrLabel->width();
  mPhaseShiftLabel->move(x, TopMargin - 18);
  x += mPhaseShiftLabel->width();
  mCutOnFreqLabel->move(x, TopMargin - 18);
  refreshPixmap();
}

/*
 * Mouse events for the zooming rubberband
 */
void Plotter::mousePressEvent(QMouseEvent *event)
{
  QRect rect(LeftMargin, TopMargin, width() - (LeftMargin + RightMargin), 
             height() - (TopMargin + BottomMargin));
  if (event->button() ==Qt::LeftButton) {
    if(rect.contains(event->pos() ) ){
      mRubberBandIsShown = true;
      mRubberBandRect.setTopLeft(event->pos());
      mRubberBandRect.setBottomRight(event->pos());
      updateRubberBandRegion();
      setCursor(Qt::CrossCursor);
    }
  }
}

void Plotter::mouseMoveEvent(QMouseEvent *event)
{
  if (mRubberBandIsShown) {
    updateRubberBandRegion();
    mRubberBandRect.setBottomRight(event->pos());
    updateRubberBandRegion();
  }
}

void Plotter::mouseReleaseEvent(QMouseEvent *event)
{
  if (event->button() == Qt::LeftButton && mRubberBandIsShown) {
    mRubberBandIsShown = false;
    updateRubberBandRegion();
    unsetCursor();

    QRect rect = mRubberBandRect.normalized();
    if (rect.width() < 4 || rect.height() < 4)
      return;
    rect.translate(-LeftMargin, -TopMargin);

    PlotSettings prevSettings = mZoomStack[mCurStack][mCurZoom[mCurStack]];
    PlotSettings settings;
    double dx = prevSettings.spanX() / (width() - (LeftMargin + RightMargin));
    double dy = prevSettings.spanY() / (height() - (TopMargin + BottomMargin));
    settings.minX = prevSettings.minX + dx * rect.left();
    settings.maxX = prevSettings.minX + dx * rect.right();
    settings.minY = prevSettings.maxY - dy * rect.bottom();
    settings.maxY = prevSettings.maxY - dy * rect.top();
    settings.adjust();

    mZoomStack[mCurStack].resize(mCurZoom[mCurStack] + 1);
    mZoomStack[mCurStack].append(settings);
    zoomIn();
  }
}

/*
 * Use double-click to define zeros
 */
void Plotter::mouseDoubleClickEvent(QMouseEvent *event)
{
  DefocusFinder *finder = &mApp->mDefocusFinder;
  PlotSettings settings = mZoomStack[mCurStack][mCurZoom[mCurStack]];
  double dx = settings.spanX()/(width() - (LeftMargin + RightMargin));
  double zero = settings.minX + dx * (event->x() - LeftMargin);
  double defocus, defoc2, defocAvg;
  
  // For a left button, redefine the first zero
  if (event->button() == Qt::LeftButton) {
    finder->setZero(zero);
    finder->findDefocus(&defocus);
    finder->setAvgDefocus(-1.);
    manageLabels(zero, defocus, 0., 0., 0);
    // If using ctf-like curve fit, replot with selected
    // defocus and all other parameters unchanged
    if (mApp->getZeroFitMethod() == FIT_CTF_LIKE)
      mApp->replotWithDefocus(defocus);

  } else if (event->button() == Qt::RightButton) {
      
      // Send the value in Nyquist units and let it decide what to do
    if (mFittingDia)
      mFittingDia->newX2EndClicked(zero);
  } else {
    // Otherwise define the second zero and the average
    defocus = finder->getDefocus();
    if (defocus <= 0)
      return;
    defoc2 = finder->defocusFromSecondZero(zero);
    defocAvg = 0.5 * (defocus + defoc2);
    finder->setAvgDefocus(defocAvg);
    manageLabels(finder->getZero(), defocus, defoc2, defocAvg, 1);
  }
}

/*
 * Set the labels appropriately for the given values: set to NA if type = -1,
 * set first zero if type is 0 or 1, set second zero if type is 1
 */
void Plotter::manageLabels(double zero, double defocus, double def2,
                           double defAvg, int type)
{
  QString zeroString = "Z: NA ";
  QString defocusString = "D: NA ";
  QString defoc2String = "D2: NA ";
  QString defocAvgString = "D-avg: NA ";
  // FREQUENCY SCALE CHANGE: " / 2."
  if (type >= 0) {
    zeroString.sprintf("Z: %4.3f", zero / 2.);
    defocusString.sprintf("D%s: %4.2f", type ? "1" : "", defocus);
  }
  if (type > 0) {
    defoc2String.sprintf("D2: %4.2f", def2);
    defocAvgString.sprintf("D-avg: %4.2f", defAvg);
  }
  mZeroLabel->setText(zeroString);
  mDefocusLabel->setText(defocusString);
  mDefoc2Label->setText(defoc2String);
  mDefocAvgLabel->setText(defocAvgString);
}

void Plotter::manageLabels2(double astig, double axis, double error, double wedge, 
                            double phase, double cuton, int flags)
{
  QString errString = "Error: NA ";
  QString astigString = "";//"Astig: NA ";
  QString axisString = "";//"Axis: NA ";
  QString wedgeString = "";//"Wedge err: NA ";
  QString phaseString = "";//"Phase: NA ";
  QString cutonString = "";//"Cut-on: NA ";
  
  if (flags & LABEL2_ZFERR)
    errString.sprintf("ZF err: %7.5f", error);
  else if (flags & LABEL2_ERROR)
    errString.sprintf("Error: %7.4f", error);
  else if (flags & LABEL2_SCORE)
    errString.sprintf("Score: %6.4f", error);
  if (flags & LABEL2_ASTIG) {
    astigString.sprintf("Astig: %4.2f", astig);
    axisString.sprintf("Axis: %5.1f", axis);
  }
  if (flags & LABEL2_CEN_ANGLE)
    astigString.sprintf("Wedge: %5.1f", mApp->getWedgeCenterAngle());
  if (flags & LABEL2_WEDGE)
    wedgeString.sprintf("Wedge err: %.3f", wedge);
  if (flags & LABEL2_PH_FAIL)
    phaseString = "Phase: FAIL";
  else if (flags & LABEL2_PHASE)
    phaseString.sprintf("Phase: %.1f", phase / RADIANS_PER_DEGREE);
  if (flags & LABEL2_CO_FAIL)
    cutonString = "Cuton: FAIL";
  else if (flags & LABEL2_CUTON) {
    cutonString.sprintf("Cut-on: %.3f", cuton);
    if (flags & LABEL2_FALLBACK)
      cutonString += " FB";
  }
  mErrScoreLabel->setText(errString);
  mAstigDefLabel->setText(astigString);
  mAstigAxisLabel->setText(axisString);
  mWedgeErrLabel->setText(wedgeString);
  mPhaseShiftLabel->setText(phaseString);
  mCutOnFreqLabel->setText(cutonString);
}

/*
 * Process hot keys for zoom and scroll
 */
void Plotter::keyPressEvent(QKeyEvent *event)
{
  switch (event->key()) {
  case Qt::Key_Plus:
  case Qt::Key_Equal:
    zoomIn();
    break;
  case Qt::Key_Minus:
  case Qt::Key_Underscore:
    zoomOut();
    break;
  case Qt::Key_Left:
    mZoomStack[mCurStack][mCurZoom[mCurStack]].scroll(-1, 0);
    refreshPixmap();
    break;
  case Qt::Key_Right:
    mZoomStack[mCurStack][mCurZoom[mCurStack]].scroll(+1, 0);
    refreshPixmap();
    break;
  case Qt::Key_Down:
    mZoomStack[mCurStack][mCurZoom[mCurStack]].scroll(0, -1);
    refreshPixmap();
    break;
  case Qt::Key_Up:
    mZoomStack[mCurStack][mCurZoom[mCurStack]].scroll(0, +1);
    refreshPixmap();
    break;
  case Qt::Key_0:
  case Qt::Key_1:
  case Qt::Key_2:
  case Qt::Key_3:
  case Qt::Key_4:
    mApp->setBaselineOrder(event->key() - Qt::Key_0, false);
    if (mFittingDia)
      mFittingDia->setBaselineOrder(event->key() - Qt::Key_0);
    break;
  default:
    QWidget::keyPressEvent(event);
  }
}

/*
 * Use wheel to scroll up/down
 */
void Plotter::wheelEvent(QWheelEvent *event)
{
  int numDegrees = event->delta() / 8;
  int numTicks = numDegrees / 15;

  if (event->orientation() == Qt::Horizontal)
    mZoomStack[mCurStack][mCurZoom[mCurStack]].scroll(numTicks, 0);
  else
    mZoomStack[mCurStack][mCurZoom[mCurStack]].scroll(0, numTicks);
  refreshPixmap();
}

void Plotter::updateRubberBandRegion()
{
    QRect rect = mRubberBandRect.normalized();
    update(rect.left(), rect.top(), rect.width(), 1);
    update(rect.left(), rect.top(), 1, rect.height());
    update(rect.left(), rect.bottom(), rect.width(), 1);
    update(rect.right(), rect.top(), 1, rect.height());
}

void Plotter::refreshPixmap()
{
  int gray = mApp->getChangeColors() ? 0 : 64;
  mPixmap=QPixmap(size());
  //mPixmap.fill(this, 0, 0);
  mPixmap.fill(QColor(gray, gray, gray));
  QPainter painter(&mPixmap);
  painter.initFrom(this);
  drawGrid(&painter, true);
  drawCurves(&painter);
  update();
}

/*
 * Draw the grid lines and labels
 */
void Plotter::drawGrid(QPainter *painter, bool onScreen)
{
  QString str;
  QRect rect(LeftMargin, TopMargin, width() - (LeftMargin + RightMargin), 
             height() - (TopMargin + BottomMargin));
  if(!rect.isValid()) return;
  PlotSettings settings = mZoomStack[mCurStack][mCurZoom[mCurStack]];
  //QPen quiteDark = palette().dark().color().light();
  //QPen light = palette().light().color();
  //QPen quiteDark=QPen(Qt::white);
  QPen quiteDark;
  QPen light;
  double pixel = mApp->getPixelSize();
  if (mApp->getCropSpectra())
    pixel = B3DMAX(pixel, mApp->getCropPixelSizeInDia());
  if(onScreen){
    int dark = mApp->getChangeColors() ? 64 : 0;
    quiteDark=QPen(QColor(dark, dark, dark));
    light=QPen(Qt::white);
  }else{
    quiteDark=QPen(QColor(64,64,64));
    light=QPen(Qt::black);
  }

  for (int i = 0; i <= settings.numXTicks; ++i) {
    int x = rect.left() + (i * (rect.width() - 1)
                           / settings.numXTicks);
    double label = settings.minX + (i * settings.spanX()
                                    / settings.numXTicks);
    painter->setPen(quiteDark);
    painter->drawLine(x, rect.top(), x, rect.bottom());
    painter->setPen(light);
    painter->drawLine(x, rect.bottom(), x, rect.bottom() + 5);
    // FREQUENCY SCALE CHANGE: " / 2."
    painter->drawText(QRectF(x - 50., rect.bottom() + 5., 100., 15.),
                      Qt::AlignHCenter | Qt::AlignTop,
                      QString::number(label / 2.));
    if (label > 0.02) {
      str.sprintf("%.1f", 20. * pixel / label);
      painter->drawText(QRectF(x - 50., rect.bottom() + 20., 100., 15.),
                        Qt::AlignHCenter | Qt::AlignTop, str);
    }
  }
  painter->drawText(QRectF(rect.left() + rect.width() / 2. - 250., rect.bottom() + 35.,
                          500., 15.), Qt::AlignHCenter | Qt::AlignTop, 
                    "Frequency (1/pixel)  /  Periodicity (Angstroms)");
  for (int j = 0; j <= settings.numYTicks; ++j) {
    int y = rect.bottom() - (j * (rect.height() - 1)
                             / settings.numYTicks);
    double label = settings.minY + (j * settings.spanY()
                                    / settings.numYTicks);
    if (fabs(label) < 1.e-6)
      label = 0.;
    painter->setPen(quiteDark);
    painter->drawLine(rect.left(), y, rect.right(), y);
    painter->setPen(light);
    painter->drawLine(rect.left() - 5, y, rect.left(), y);
    painter->drawText(QRectF(rect.left() - LeftMargin, y - 10.,
                             LeftMargin - 5., 20.),
                      Qt::AlignRight | Qt::AlignVCenter,
                      QString::number(label));
  }
  painter->drawRect(rect.adjusted(0,0,-1,-1));
}

/*
 * Draw the curves
 */
void Plotter::drawCurves(QPainter *painter)
{
  QColor colorForIds[6] = {
    Qt::magenta, Qt::green, Qt::cyan, Qt::red, Qt::blue, Qt::yellow
  };
  PlotSettings settings = mZoomStack[mCurStack][mCurZoom[mCurStack]];
  QRect rect(LeftMargin, TopMargin, width() - (LeftMargin + RightMargin), 
             height() - (TopMargin + BottomMargin));

  if(!rect.isValid()) 
    return;
  if (mApp->getChangeColors()) {
    colorForIds[0] = Qt::white;
    colorForIds[1] = QColor(255, 64, 255);
  }

  painter->setClipRect(rect.adjusted(+1, +1, -1, -1) );
    
  QMapIterator<int, QVector<QPointF> > i(mCurveMap);
  while (i.hasNext() ) {
    i.next();
    int id = i.key();
    const QVector<QPointF> &data = i.value();
    QPolygonF polyline(data.count());

    for (int j = 0; j <data.count(); ++j) {
      double dx = data[j].x() - settings.minX;
      double dy = data[j].y() - settings.minY;
      double x = rect.left() + (dx * (rect.width() - 1)
                                / settings.spanX());
      double y = rect.bottom() - (dy * (rect.height() - 1)
                                  / settings.spanY());
      polyline[j]=QPointF(x,y);
    }
    painter->setPen(colorForIds[(uint)id % 6]);
    painter->drawPolyline(polyline);
  }
}

PlotSettings::PlotSettings()
{
  minX = 0.0;
  maxX = 1.0;
  numXTicks = 5;
  
  minY = -0.3;
  maxY = 20;
  numYTicks = 5;
}

void PlotSettings::scroll(int dx, int dy)
{
  double stepX = spanX() / numXTicks;
  minX += dx * stepX;
  maxX += dx * stepX;
  
  double stepY = spanY() / numYTicks;
  minY += dy * stepY;
  maxY += dy * stepY;
}

void PlotSettings::adjust()
{
  adjustAxis(minX, maxX, numXTicks);
  adjustAxis(minY, maxY, numYTicks);
}

void PlotSettings::adjustAxis(double &min, double &max,
                              int &numTicks)
{
  const int MinTicks = 4;
  double grossStep = (max - min) / MinTicks;
  double step = pow(10.0, floor(log10(grossStep)));
  
  if (5 * step < grossStep)
    step *= 5;
  else if (2 * step < grossStep)
    step *= 2;
  
  numTicks = (int)(ceil(max / step) - floor(min / step));
  min = floor(min / step) * step;
  max = ceil(max / step) * step;
}
