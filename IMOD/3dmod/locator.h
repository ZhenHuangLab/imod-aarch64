/*   locator.h  -  declarations for locator.cpp
 *
 *  $Id$
 */                                                                           

#ifndef LOCATOR_H
#define LOCATOR_H
#include <qmainwindow.h>
#include <qgl.h>
#include <QLabel>
#include <QMouseEvent>
#include <QTimerEvent>
#include <QKeyEvent>
#include <QCloseEvent>
#include "control.h"

class LocatorGL;
class QPushButton;
class QLabel;
class ArrowButton;
class QScreen;
typedef struct ViewInfo ImodView;
typedef struct b3d_ci_image B3dCIImage;

int locatorOpen(ImodView *vi);
void locatorScheduleDraw(ImodView *vi);

class LocatorWindow : public QMainWindow
{
  Q_OBJECT

 public:
  LocatorWindow(bool rgba, bool doubleBuffer, bool enableDepth,
                QWidget * parent = 0, Qt::WindowFlags f = Qt::Window) ;
  ~LocatorWindow() {};

  int mCtrl;
  QLabel *mZoomLabel;
  ArrowButton *mUpArrow, *mDownArrow;
  float mDevicePixelRatio;
  
 public slots:
  void help();
  void zoomUp();
  void zoomDown();
#ifdef WATCH_DPI_CHANGE
  void screenChanged(QScreen *screen);
#endif

 protected:
  void closeEvent ( QCloseEvent * e );
  void keyPressEvent ( QKeyEvent * e );
  void changeEvent(QEvent *e);

 private:
  void setFontDependentWidths();
  QPushButton *mHelpButton;
};

class LocatorGL : public QGLWidget
{
  Q_OBJECT

 public:
  LocatorGL(QGLFormat format, LocatorWindow *topWin, QWidget * parent = 0);
  ~LocatorGL() {};
  void drawIfNeeded(int drawflag);
  void scheduleDraw();
  void getImxy(int x, int y, float &imx, float &imy);
  ImodView *mVi;
  B3dCIImage *mImage;
  int mWinx, mWiny;
  void changeSize(float factor);
  void fakeResize(int width, int height) {resizeGL(width, height);};
 
protected:
  void initializeGL() {};
  void paintGL();
  void resizeGL( int wdth, int hght );
  void mousePressEvent(QMouseEvent * e );
  void mouseReleaseEvent ( QMouseEvent * e );
  void mouseMoveEvent ( QMouseEvent * e );
  void timerEvent(QTimerEvent * e );

 private:
  double mZoom;
  int mLastAxis;
  int mLastSubRet;
  int mSection, mTime;
  int mLastX0, mLastY0;
  int mLastNx, mLastNy;
  int mBlack, mWhite;
  bool mFirstDraw;
  int mXborder, mYborder;
  int mTimerID;
  int mCursorSet;
  int mMouseX, mMouseY;
  LocatorWindow *mTopWin;
};

#endif
