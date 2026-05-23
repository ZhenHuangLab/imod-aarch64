/*  isosurface.h  -  declarations for isosurface.cpp
 *
 *  $Id$
 *
 */

#ifndef IMODV_ISOSURFACE_H
#define IMODV_ISOSURFACE_H

typedef struct __imodv_struct ImodvApp;
typedef struct  ImodImageFileStruct ImodImageFile;
typedef unsigned int Index;
extern void smooth_vertex_positions(float *varray, Index nv,
    const Index *tarray, Index nt,
    float smoothing_factor, int smoothing_iterations);

// Based on timing tests in May 2008, confirmed on Nehalem March 2010
// 1/15/18: raised to 16 and set practical max to 4 in the main code
#define MAX_THREADS 16

/* Image Control functions. */
void imodvIsosurfaceEditDialog(ImodvApp *a, int state);
bool imodvIsosurfaceUpdate(int drawFlags);
bool imodvIsosurfaceBoxLimits(int &ixStart, int &iyStart, int &nxUse, int &nyUse);
void imodvIsosurfaceInvertThreshold();
void imodvIsosurfaceNewModel();

#include "imodel.h"
#include "dialog_frame.h"
#include <qspinbox.h>
#include <vector>

class MultiSlider;
class QCheckBox;
class QLabel;
class QSlider;
class QLineEdit;
//class QSpinBox;
class QPushButton;
class QHBoxLayout;
class HistWidget;
struct ViewInfo;
class IsoThread;
class Surface_Pieces;
class DockingDialog;

typedef struct {
  int ix, iy, iz;
} IsoPoint3D;

typedef struct {
  int trans;
  unsigned char r, g, b, dummy;
} IsoColor;

typedef struct {
  float x, y, z;
  float size;
  int colorInd;
  bool drawn;
} IsoPaintPoint;


class ImodvIsosurface : public DialogFrame
{
  Q_OBJECT

 public:
  ImodvIsosurface(struct ViewInfo *vi, bool fillBut, DockingDialog *parent,
                  const char *name = NULL) ;
  ~ImodvIsosurface();
  void updateCoords(bool setLocal);
  void setBoundingBox();
  void setBoundingObj();
  void setViewCenter();
  int getCurrStackIndex();
  void setIsoObj(bool fillPaint);
  void fillAndProcessVols(float percentile, bool skipFill = false, float useThresh = -1.);
  void resizeToContours(bool draw);
  int getBinning();
  bool fillPaintVol();
  void paintMesh();
  void managePaintObject();
  void paintListToString(QString &str);
  void modelToPaintList();
  void invertThreshold();
  int getParametersFromStore(bool thresholdOnly, bool setControls);
  void setParametersInStore();

  int mLocalX;
  int mLocalY;
  int mLocalZ;
  int mMaskObj;
  int mMaskCont;
  int mMaskPsize;
  double mMaskChecksum;
  int mCurrTime;
  int mBoxOrigin[3];
  int mBoxEnds[3];
  int mRangeLow, mRangeHigh;
  unsigned char *mVolume;
  int mCurrStackIndex; // which stack is current;
  std::vector<float> mStackThresholds;
  float mThreshold; //threshold for the current stack;
  float mMedian;
  std::vector<int> mStackOuterLims;
  int mOuterLimit;
  int mBinBoxSize[3];
  int mSubZEnds[MAX_THREADS+1];
  unsigned char *mBinVolume;
  int mNThreads;
  int mLastObjsize;
  HistWidget *mHistPanel;
  std::vector<int> mPaintObjList;
  QLineEdit *mPaintListEdit;
  int mParamLoadConfirmed;
  bool mNewModelOpened;

  public slots:
    void viewIsoToggled(bool state) ;
    void viewModelToggled(bool state) ;
    void viewBoxingToggled(bool state);
    void centerVolumeToggled(bool state);
    void deletePiecesToggled(bool state);
    void linkXYZToggled(bool state);
    void closeFacesToggled(bool state);
    void histChanged(int, int, bool );
    void iterNumChanged(int);
    void binningNumChanged(int);
    void kernelSigmaChanged(double);
    void sliderMoved(int which, int value, bool dragging);
    void buttonPressed(int which);
    void showRubberBandArea();
    void numOfTrianglesChanged(int);
    void maskSelected(int which);
    void areaFromContClicked();
    void paintObjToggled(bool state);
    void paintListEditFinished();
    void topChangeEvent(QEvent *e);
  void topCloseEvent ( QCloseEvent * e );

 protected:
  void keyPressEvent ( QKeyEvent * e );
  void keyReleaseEvent ( QKeyEvent * e );

 private:
  float fillVolumeArray();
  void  fillBinVolume();
  void removeOuterPixels();
  void smoothMesh(Imesh *, int);
  void filterMesh(bool);
  bool allocArraysIfNeeded();
  int maskWithContour(Icont *inCont, int iz);
  void closeBoxFaces();
  void applyMask();
  void showDefinedArea(float x0, float x1, float y0, float y1, bool draw);
  int findClosestZ(int iz, int *listz, int zlsize, int **contatz, int *numatz, int zmin,
                   int &otherSide);
  void addToNeighborList(IsoPoint3D **neighp, int &numNeigh, int &maxNeigh,
                         int ix, int iy, int iz);
  float fileValueOfThreshold();
  float thresholdFromFileValue(ImodImageFile *image, float value);
  void dumpVolume(const char *filename, bool binned);
  void setFontDependentWidths();

  bool mCtrlPressed;
  struct ViewInfo *mVi;
  int mExtraObjNum;
  int mCurrMax;
  IsoThread *threads[MAX_THREADS];

  Imesh * mOrigMesh;
  Imesh * mFilteredMesh;
  bool mMadeFiltered;
  QCheckBox *mViewIso, *mViewModel, *mViewBoxing, *mCenterVolume;
  QCheckBox *mLinkXYZ, *mDeletePieces, *mPaintCheck, *mCapFaces; 
  MultiSlider *mSliders;
  MultiSlider *mHistSlider;
  QPushButton *mUseRubber, *mSizeContours;
  QSpinBox *mSmoothBox;
  QSpinBox *mBinningBox;
  QSpinBox *mPiecesBox;
  QSpinBox *mPaintObjSpin;
  QDoubleSpinBox *mSigmaBox;
  QHBoxLayout *mPaintLayout;

  int mBoxObjNum;
  int mBinBoxEnds[3];
  int mBoxSize[3];
  int mPaintSize[3];

  unsigned char *mTrueBinVol;
  unsigned char *mPaintVol;
  int mVolMin, mVolMax;
  int mInitNThreads;
  Surface_Pieces *mSurfPieces;

  std::vector<IsoColor> mColorList;
  std::vector<IsoPaintPoint> mPaintPoints;

};

#endif
