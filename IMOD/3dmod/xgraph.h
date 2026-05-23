/*   xgraph.h  -  declarations for xgraph.cpp
 *
 *   Copyright (C) 1995-2019 by the Regents of the University of 
 *   Colorado.  See implementation file for full copyright notice.
 *
 *  $Id$
 */                                                                           

#ifndef XGRAPH_H
#define XGRAPH_H

#define MAX_GRAPH_TOGGLES 2

#include <qmainwindow.h>
#include <qgl.h>
#include <QLabel>
#include <QMouseEvent>
#include <QKeyEvent>
#include <QCloseEvent>
#include "imodel.h"
#include "control.h"

class QToolButton;
class QLabel;
class QSignalMapper;
class QHBoxLayout;
class QSpinBox;
class GraphGL;
class GraphWindow;
class QScreen;
struct ViewInfo;

int  xgraphOpen(struct ViewInfo *vi);


class GraphWindow : public QMainWindow
{
  Q_OBJECT

 public:
  GraphWindow(struct ViewInfo *vi, bool rgba, bool doubleBuffer, bool enableDepth,
              QWidget * parent = 0, Qt::WindowFlags f = Qt::Window) ;
  ~GraphWindow() {};
  void setToggleState(int index, int state);
  void draw();
  void drawAxis();
  void drawPlot();
  void fillData();
  void externalKeyEvent ( QKeyEvent * e, int released);

  public slots:
    void zoomUp();
  void zoomDown();
  void help();
  void toggleClicked(int index);
  void axisSelected(int item);
  void exportToFile();
  void widthChanged(int value);
#ifdef WATCH_DPI_CHANGE
  void screenChanged(QScreen *screen);
#endif

 protected:
  void keyPressEvent ( QKeyEvent * e );
  void closeEvent ( QCloseEvent * e );
  void changeEvent(QEvent *e);
  
 private:
  int allocDataArray(int dsize);
  void makeBoundaryPoint(Ipoint pt1, Ipoint pt2, int ix1, int ix2,
                              int iy1, int iy2, Ipoint *newpt);
  
 public:
  GraphGL *mGLw;
  struct ViewInfo *mVi;
  int    mWidth, mHeight;
  float *mData;
  float  mZoom;
  int    mAxis;
  int    mLocked;
  int    mCtrl;
  int    mStart;
  float  mXcur, mYcur, mZcur; /* current location. */
  float mDevicePixelRatio;
  int    mClosing;

 private:
  QToolButton *mToggleButs[MAX_GRAPH_TOGGLES];
  int mToggleStates[MAX_GRAPH_TOGGLES];
  QLabel *mPlabel1;
  QLabel *mPlabel2;
  QLabel *mPlabel3;
  QLabel *mVlabel1;
  QLabel *mVlabel2;
  QLabel *mMeanLabel;
  QSpinBox *mWidthBox;
  int    mDataSize;
  int    mAllocSize;
  int    mCenterPt;
  int    mObjCur, mContCur, mPtCur; /* current object, contour, point */
  int    mSubStart;
  int    mHighRes;
  int    mNumLines;
  float  mOffset;
  float  mScale;
  float  mMin, mMax;
  float  mMean;
  int    mEnd;
};

class GraphGL : public QGLWidget
{
  Q_OBJECT

 public:
  GraphGL(GraphWindow *graph, QGLFormat format, QWidget * parent = 0);
  ~GraphGL() {};
  void setxyz(int mx, int my);
 
protected:
  void initializeGL() {};
  void paintGL();
  void resizeGL( int wdth, int hght );
  void mousePressEvent(QMouseEvent * e );

 private:
  GraphWindow *mGraph;
  bool mDrawing;
};
#endif     // XGRAPH_H
