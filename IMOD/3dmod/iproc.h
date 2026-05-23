/*
 *  iproc.h - declarations for IProcWindow class and processing window
 *
 *  $Id$
 */

#ifndef BD_IPROC_H_
#define BD_IPROC_H_

#define PROC_BACKGROUND 0
#define PROC_FOREGROUND 255

#include "mrcslice.h"
#include "dialog_frame.h"
//Added by qt3to4:
#include <QCloseEvent>
#include <QTimerEvent>
#include <QLabel>
#include <QKeyEvent>
#include <vector>
class QStackedWidget;
class QListWidget;
class QVBoxLayout;
class QLabel;
class QSpinBox;
class QPushButton;
class ToolEdit;
class QDoubleSpinBox;
class QListWidgetItem;
class DockingDialog;
class MultiSlider;

typedef struct ViewInfo ImodView;

#include <qthread.h>

typedef struct
{
  int           procNum;
  int           threshold;  /* Parameters for individual filters */
  bool          threshGrow;
  bool          threshShrink;
  int           edge;
  float         kernelSigma;
  bool          smooth3D;
  bool          rescaleSmooth;
  float         radius1;
  float         radius2;
  float         sigma1;
  float         sigma2;
  int           fftBinning;
  bool          fftSubset;
  bool          median3D;
  int           medianSize;
  int           andfIterations;
  int           andfIterDone;
  double        andfK;
  double        andfLambda;
  int           andfStopFunc;
} IProcParam;

class IProcThread : public QThread
{
 public:
  IProcThread() {};
  ~IProcThread() {};

 protected:
  void run();
};

class IProcWindow : public DialogFrame
{
  Q_OBJECT

 public:
  IProcWindow(DockingDialog *parent, const char *name = NULL);
  ~IProcWindow() {};
  bool mRunningProc;
  int mUseStackInd;
  QStringList mCommandList;
  void (*mCallback)();
  void limitFFTbinning();
  void apply(bool useStack = false);
  void setSigma1Slider(float value);
  MultiSlider *mFourFiltSliders;
  float mSigma1Scale, mLastFourFiltRange;

  public slots:
  void buttonClicked(int which);
  void buttonPressed(int which);
  void autoApplyToggled(bool state);
  void autoSaveToggled(bool state);
  void applyThreshToggled(bool state);
  void edgeSelected(int which);
  void filterSelected(QListWidgetItem *item);
  void filterHighlighted(int which);
  void threshChanged(int which, int value, bool dragging);
  void fourFiltChanged(int which, int value, bool dragging);
  void filtSlicerToggled(bool state);
  void binningChanged(int val);
  void kernelChanged(double val);
  void smooth3DChanged(bool state);
  void scaleSmthToggled(bool state);
  void subsetChanged(bool state);
  void growChanged(bool state);
  void shrinkChanged(bool state);
  void medSizeChanged(int val);
  void med3DChanged(bool state);
  void andfIterChanged(int val);
  void andfFuncClicked(int val);
  void andfKEntered();
  void reportFreqClicked();
  void calcFileThreshold();
  void topCloseEvent ( QCloseEvent * e );
  void topChangeEvent(QEvent *e);

 protected:
  void keyPressEvent ( QKeyEvent * e );
  void keyReleaseEvent ( QKeyEvent * e );
  void timerEvent(QTimerEvent *e);

 private:
  DockingDialog *mTopWin;
  QStackedWidget *mStack;
  QListWidget *mListBox;
  void startProcess();
  void finishProcess();
  void newThreshSetting();
  int mTimerID;
  QThread *mProcThread;
  IProcParam mSavedParam;
  std::vector<int> mDataModes;
 public:
  std::vector<IProcParam> mParamStack;
};

typedef struct
{
  IProcWindow   *dia;
  ImodView      *vi;        /* image data to model                       */
  unsigned char *iwork;     /* Image data processing buffer.             */
  unsigned char *isaved;     /* buffer for saving original data.         */
  float         **andfImage; /* Double buffers for aniso diff */
  float         **andfImage2;
  Istack         medianVol;

  int           idataSec;   /* data section. */
  int           idataTime;  /* time value of section */
  int           modified;   /* flag that section data are modified */
  bool          autoApply;  /* Apply automatically when changing section */
  bool          autoSave;   /* Save automatically when changing section */
  bool          filterSlicer;  // Flag to filter in slicers
  bool          slicerFiltApplied;  // Flag that it has been applied in a filter sequence 
  bool          toggling;
  bool          applyThreshChange;  /* Automatically apply threshold changes */
  float         fileThreshold;      /* Threshold value in file */
  QLabel        *threshFileLabel;
  int           rangeLow;   /* Low and high range values when image mapped to slice */
  int           rangeHigh;
  int           inputMode;
  int           outputMode;
  bool          wasByte;         
  QDoubleSpinBox *kernelSpin;
  float         fftScale;
  float         fftXrange;
  float         fftYrange;
  QSpinBox      *fftBinSpin;
  QLabel        *fftLabel1;
  QLabel        *fftLabel2;
  QLabel        *fftLabel3;
  QPushButton   *freqButton;
  int           fftXcen;
  int           fftYcen;
  ToolEdit      *andfKEdit;
  ToolEdit      *andfLambdaEdit;
  QLabel        *andfScaleLabel;
  QLabel        *andfDoneLabel;
  
} ImodIProc;

typedef struct
{
  const char *name;         /* Name of index */
  void (*cb)();       /* callback to do action */
  /* function to make widget */
  void (*mkwidget)(IProcWindow *, QWidget *, QVBoxLayout *); 
  const char *label;
  QWidget *control;
} ImodIProcData;

int inputIProcOpen(ImodView *vw);
int iprocRethink(ImodView *vw);
bool iprocBusy(void);
void iprocApply(void);
void iprocUpdate(void);
void iprocCallWhenFree(void (*func)());
bool iprocIsOpen();
QStringList iprocCommandList();
bool iprocFilterForSlicer(float &radius1, float &sigma1, float &radius2, float &sigma2);
int volByteSmooth(unsigned char **vol, int nx, int ny, int nz, float sigma, int midZonly);
void byteKernelFilter(unsigned char *array, short *brray, int nxAdim, int nxBdim,
                      int nx, int ny, int *mat, int kdim);


#endif /* BD_IPROC_H_ */
