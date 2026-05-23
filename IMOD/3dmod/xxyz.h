/* 
 *  xxyz.h -- Include file for xyz.c, the XYZ Window.
 *
 *  Original author: James Kremer
 *  Revised by: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2019 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#ifndef XXYZ_H
#define XXYZ_H

#include <qmainwindow.h>
#include <qgl.h>
//Added by qt3to4:
#include <QCloseEvent>
#include <QTimerEvent>
#include <QLabel>
#include <QMouseEvent>
#include <QKeyEvent>
#include "b3dgfx.h"
#include "pyramidcache.h"
#include "imodel.h"
#include "control.h"
#include <QTime>

/* Forward declarations to minimize includes */
struct ViewInfo;
struct Mod_object;
struct Mod_contour;
class XyzWindow;
class XyzGL;
class HotToolBar;
class QToolBar;
class QLabel;
class QToolButton;
class QSignalMapper;
class ToolEdit;
class QSlider;
class MultiSlider;
class QPushButton;
class QSpinBox;
class QScreen;

#define MAX_XYZ_TOGGLES  3
#define NUM_AXIS 3

enum {NOT_IN_BOX = 0, X_SLICE_BOX, Y_SLICE_BOX, Z_SLICE_BOX, Y_GADGET_BOX,
      X_GADGET_BOX, Z_GADGET_BOX, FRACTION_BOX};

class XyzWindow : public QMainWindow
{
  Q_OBJECT

    public:
  XyzWindow(struct ViewInfo *vi, bool rgba, bool doubleBuffer, 
	    bool enableDepth, QWidget * parent = 0, const char * name = 0,
	    Qt::WindowFlags f = Qt::Window) ;
  ~XyzWindow() {};

  XyzGL *mGLw;
  //HotToolBar *mToolBar;
  QToolBar *mToolBar;
  unsigned char *mFdataXY; /* tmp data storage for summed xy image       */
  unsigned char *mFdataXZ; /* tmp data storage for xz image       */
  unsigned char *mFdataYZ; /* tmp data storage for yz image       */
  B3dCIImage *mXyImage;    /* Draw buffer for Z slices.            */
  B3dCIImage *mXzImage;    /* Draw buffer for Y slices.            */
  B3dCIImage *mYzImage;    /* Draw buffer for X slices.            */
  int *mSumTemp;           /* Temp array to sum into */
  int mLx, mLy, mLz;
  int mToolMaxY;
  struct ViewInfo *mVi;   /* Image Data information.              */
  int mCtrl;              /* id of control */
     
  int mWinx, mWiny;         /* Size of xyz window.                  */
  int mExposed;
  float mZoom;
  int mXwoffset1, mXwoffset2;  /* offset in window coordinates */
  int mYwoffset1, mYwoffset2;
  int mLmx, mLmy;           /* last mouse position for panning */
  int mFirstMx, mFirstMy;   /* initial mouse position for panning threshsold */
  int mToolMaxX;
  int mToolMaxZ;
  int mWinXdim1,mWinXdim2,mWinYdim1,mWinYdim2;
  int mXorigin1, mXorigin2, mYorigin1, mYorigin2;
  float mScaleBarSize;     /* Actual size of draw scale bar in model units */
  int mWhichbox;           /* box that left mouse button went down in */
  int mMousemode;          /* Current mode for cursor */
  float mToolZoom;
  QTime mScreenResizeTime[2];
  bool mScreenChanged;
  float mLastXsizeChange;
  float mDevicePixelRatio;

  void Draw();
  int Getxyz(int x, int y, float &mx, float &my, int &mz);
  void B1Press(int x, int y);
  void B2Press(int x, int y);
  void B3Press(int x, int y);
  void B1Drag(int x, int y);
  void B2Drag(int x, int y);
  void B3Drag(int x, int y);
  void DrawAuto();
  void DrawModel();
  void DrawImage();
  void drawTools();
  void GetCIImages();
  void DrawGhost();
  void DrawContour(struct Mod_Object *obj, int ob, int co);
  void DrawCurrentPoint();
  void DrawCurrentLines();
  void DrawScatSymAllSpheres(Iobj *obj, int ob,  Icont *cont, int co, 
                             DrawProps *contProps,
                             DrawProps *ptProps, int *stateFlags,
                             int handleFlags, int nextChange, 
                             int indx, int indy, int indz,
                             int zmouse, float bx, float by,float zscale);
  void DrawSymProj(Iobj *obj,  Icont *cont, int co, DrawProps *contProps, 
                   DrawProps *ptProps, int *stateFlags, 
                   int handleFlags, int nextChange, int indx, 
                   int indy, int indz, int currentZ, float bx, float by,
                   bool currentCont);
  void keyPressPassedOn ( QKeyEvent * e ) {keyPressEvent(e);};
  void SetCursor(int mode, bool setAnyway = false);
  void setZoomText(float zoom);
  void setSlider(int which, int section);
  void setMaxAxis(int which, int max);
  void getLocation(int &cx, int &cy, int &cz);
  void setLocation(int newx, int newy, int newz);
  void finishNewModelPoint(int mx, int my, int mz);
  void getSubsetLimits(int &ixStart, int &iyStart, int &nxUse, int &nyUse);
    
  public slots:
    void toggleClicked(int index);
    void toolKeyPress(QKeyEvent *e) {keyPressEvent(e);};
    void toolKeyRelease(QKeyEvent *e) {keyReleaseEvent(e);};
    void zoomUp();
    void zoomDown();
    void newZoom();
    void centerClicked();
    void fillCachePressed();
    void thicknessChanged(int value);
    void sliderChanged(int which, int value, bool dragging);
    void help();
    void contextMenuHit(int val);
    void toolbarMenuEvent(QContextMenuEvent *event);
#ifdef WATCH_DPI_CHANGE
  void screenChanged(QScreen *screen);
#endif

 protected:
  void keyPressEvent ( QKeyEvent * e );
  void closeEvent ( QCloseEvent * e );
  void keyReleaseEvent ( QKeyEvent * e );
  void changeEvent(QEvent *e);

 private:

  int mLastCacheSum;       /* Sum of cache Z values on last draw */

  int mXtrans1, mYtrans1;   /* translation (pan) in image coords */
  int mXtrans2, mYtrans2; 
  int mHq;                 /* High resolution flag */
  int mProject;            /* Flag to project current contour into planes */
  int mApplyZscale;
  int mThickness;
  int mLastThickness;
  size_t mFdataXYsize;
  size_t mFdataXZsize;
  size_t mFdataYZsize;
  size_t mSumTempSize;
  int mLastXoffset1;
  int mLastYoffset1;
  int mLastWidth1;
  int mLastHeight1;
  int mLastXoffset2;
  int mLastYoffset2;
  int mLastWidth2;
  int mLastHeight2;
  int mLastTileCacheInd;
  int mDrawCurrentOnly;
  float mXzFraction, mYzFraction;
  int mLock;
  int mXlock, mYlock, mZlock;
  int mTimeLock;
  int mTimeDrawn;
  
  QToolButton *mToggleButs[MAX_XYZ_TOGGLES];
  int mToggleStates[MAX_XYZ_TOGGLES];
  ToolEdit *mZoomEdit;
  bool mCtrlPressed;
  int mDisplayedAxisLocation[NUM_AXIS];
  MultiSlider *mSliders;
  QPushButton *mHelpButton;
  QSpinBox *mThickSpin;

  void stateToggled(int index, int state);
  void enteredZoom(float newZoom);
  void setControlAndLimits();
  void stepZoom(int step);
  void keyRelease(QKeyEvent *event);
  void enteredAxisLocation(int which, int value);
  void allocateDim(int winsize, float zFraction, int &dim1, int &dim2);
  void fillArrayFromTiles(unsigned char *fdata,
                          std::vector<FastSegment> &segments, 
                          std::vector<int> &startInds, int ushort, bool doingYZ,
                          int istart, int iend, int jstart, int jend);
  bool newPointOutOfPlane(Icont *cont, int plane, int mx, int my, int mz);
  void setFontDependentWidths();
  void allocateSumTemp(int maxSize, int xySize);
  void addIntoSumTemp(unsigned char *fdata, int numPix, int thickness);
  void averageSumTemp(unsigned char *fdata, int numPix, int numSlices);
  int thicknessToUse(int iz, int zsize, int scale, int &izStart, int &izEnd);
};

class XyzGL : public QGLWidget
{
  Q_OBJECT

    public:
  XyzGL(QGLFormat format, XyzWindow * parent = 0);
  ~XyzGL() {};
  bool mClosing;
  void fakeResize(int width, int height) {resizeGL(width, height);};
   int mInitWidth, mInitHeight;

 protected:
  void paintGL();
  void resizeGL( int wdth, int hght );
  void mousePressEvent(QMouseEvent * e );
  void mouseReleaseEvent ( QMouseEvent * e );
  void mouseMoveEvent ( QMouseEvent * e );
  void timerEvent(QTimerEvent *e);
  void wheelEvent (QWheelEvent *e);

 private:
  XyzWindow *mWin;
  int mFirstDraw;
  int mTimerID;
  bool mMousePressed;
};

/* Global functions */
int xxyz_open(struct ViewInfo *vi);
void xyzPixelViewState(bool state);
float xyzScaleBarSize();
XyzWindow *getTopXYZ();

#endif /* xxyz.h */

