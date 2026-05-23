/*   preferences.h  -  declarations for preferences.cpp
 *
 *   Copyright (C) 1995-2016 by the Regents of the University of 
 *   Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */                                                                           

#ifndef IMOD_PREFERENCES_H
#define IMOD_PREFERENCES_H

#include <qfont.h>
#include <qstring.h>
#include <qstringlist.h>
#include <qstyle.h>
#include <qdialog.h>
//Added by qt3to4:
#include <QTimerEvent>
#include <QCloseEvent>
#include "imod.h"

class AppearanceForm;
class BehaviorForm;
class MouseForm;
class SnapshotForm;
class LayoutForm;
class PrefScalingForm;
class QSettings;
class QDialogButtonBox;
class QStackedWidget;
class QAbstractButton;
class QPushButton;
class QListWidget;
typedef struct ilist_struct Ilist;

#ifndef HOTSLIDER_H
// Defines for the states: these are replicated in colorselector.cpp for now
#define HOT_SLIDER_KEYUP 0
#define HOT_SLIDER_KEYDOWN 1
#define NO_HOT_SLIDER 2
int hotSliderFlag();
int hotSliderKey();
#endif

#define EXCLUDE_STYLES
#define MAXZOOMS 22
#define MAX_GEOMETRIES 10
#define MAX_NAMED_COLORS 12

#define TRIPLET(a,b) a b; \
  a b##Dflt; \
  bool b##Chgd;

#define RETURN_PREF(a, b) a b() {return mCurrentPrefs.b;}

// Define this to use a list of styles to exclude rather than ones to include
//#define EXCLUDE_STYLES

// The structure for new (default) object properties
typedef struct new_obj_props
{
  unsigned int    flags;
  int     pdrawsize;
  int     symbol;
  int     symsize;
  int     linewidth2;
  int     symflags;
  int     pointLimit;
  int     fillTrans;
} NewObjectProps;

// The structure of preferences.
// For each parameter, there is a default (Dflt) and a flag for whether
// the parameter has changed (Chgd)
typedef struct imod_pref_struct
{
  TRIPLET(int, hotSliderKey);        // Hot slider key (ctrl, Alt, Shift)
  TRIPLET(int, hotSliderFlag);       // Flag for whether active up or down
  TRIPLET(int, mouseMapping);          // Code for assignment of mouse keys
  TRIPLET(bool, modvSwapLeftMid);    // Swap left and middle in model view
  TRIPLET(bool, silentBeep);         // Silence the alarm in wprint
  //  TRIPLET(bool, tooltipsOn);         // Enable tool tips
  TRIPLET(bool, classicSlicer);       // Use classic slicer
  TRIPLET(bool, startInHQ);          // Start windows in HQ mode
  TRIPLET(bool, arrowsScrollZap);    // Use arrows for Page Up/Down in Zap
  TRIPLET(bool, startAtMidZ);        // Go to middle Z at start
  TRIPLET(int, autoConAtStart);      // Do autocontrast at start
  TRIPLET(bool, attachToOnObj);      // Attach to ON objects only
  TRIPLET(bool, slicerNewSurf);      // Slicer make new surfaces automatically 
  QFont font;              // Font
  bool fontChgd;
  QString styleKey;        // Style
  bool styleChgd;
  TRIPLET(int, bwStep);              // Step size for F1-F8 keys
  TRIPLET(int, pageStep);            // Step size for paging with keypad /,*
  TRIPLET(bool, iconifyImodvDlg);    // Iconify imodv dialogs with imodv window
  TRIPLET(bool, iconifyImodDlg);    // Iconify imod dialogs with info window
  TRIPLET(bool, iconifyImageWin);   // Iconify image windows with info window
  TRIPLET(bool, stackImodDlgs);      // Put docking imod dialogs in a stack
  TRIPLET(bool, stackImodvDlgs);     // Put docking imodv dialogs in a stack
  TRIPLET(bool, raiseImodDlgStack);  // Raise stack of imod dialogs when one is
  TRIPLET(bool, raiseImodvDlgStack); // Raise stack of imodv dialogs when one is
  TRIPLET(bool, keepDlgStackOnTop);  // Keep stack on top when info is on top
  TRIPLET(int, dlgFrameAdjustment);  // Amount to subtract from dialog frame geometry
  TRIPLET(int, eerSuperRes);        // Super-resolution factor reading from EER files
  TRIPLET(int, eerZbinning);        // Grouping (binning) in Z when reading EER files 
  TRIPLET(int, minModPtSize);       // Minimum size of current model point
  TRIPLET(int, minImPtSize);        // Minimum size of current image point
  double zooms[MAXZOOMS];    // Zooms
  double zoomsDflt[MAXZOOMS];
  bool zoomsChgd;
  TRIPLET(int, autosaveInterval);   // Interval to autosave at in minutes
  TRIPLET(bool, autosaveOn);        // Flag for doing autosaves
  TRIPLET(QString, autosaveDir);    // Location to save autosave file
  TRIPLET(bool, rememberGeom);     // Remember window size and locations
  TRIPLET(int, autoTargetMean);      // Target mean for autocontrast
  TRIPLET(int, autoTargetSD);        // Target SD
  int namedIndex[MAX_NAMED_COLORS];
  QRgb namedColor[MAX_NAMED_COLORS];
  QRgb namedColorDflt[MAX_NAMED_COLORS];
  bool namedColorChgd[MAX_NAMED_COLORS];
  TRIPLET(QString, snapFormat);     // Format for non-tif snapshot
  TRIPLET(int, snapQuality);        // Quality factor, controls compression
  TRIPLET(int, snapDPI);            // DPI value to set in snapshots
  TRIPLET(bool, scaleSnapDPI);      // Scale the DPI value up when montaging
  TRIPLET(int, slicerPanKb);        // Maximum KB for slicer panning
  TRIPLET(int, maxSlicerBufMB);     // Maximum MB for slicer buffer
  TRIPLET(bool, speedupSlider);     // Apply limit when using sliders too
  TRIPLET(bool, loadUshorts);       // Load data as ushorts
  TRIPLET(bool, loadIntIfMeanSD);   // Load data as ushorts if scaling by mean & SD
  TRIPLET(bool, loadIntIfEstimate); // Load data as ushorts if scaling is estimated
  TRIPLET(bool, preferMeanSD);      // Scale by mean & SD whenever available
  TRIPLET(int, numSDsForScaling);   // Number of SDs to truncate at when using mean & SD
  TRIPLET(int, scaleScanType);      // Type of scan: 0 full, 1 sub min/max, 2 sub mean/SD
  TRIPLET(bool, changeMRCstats);    // Set results from scan into an MRC header
  TRIPLET(int, useAliPieceCoords);  // Type of coordinates to use from montage mdoc file
  TRIPLET(bool, isoHighThresh);     // Set initial threshold above middle
  TRIPLET(int, isoBoxInitial);      // Initial box size
  TRIPLET(int, isoBoxLimit);        // Limit to box size
  TRIPLET(bool, keySetsHWstereo);   // Hardware stereo is turned on with hot key
  TRIPLET(bool, noVertBufForCont);  // Turn off vertex buffer use for contour drawing
  TRIPLET(bool, noVBOForSphere);    // Turn off vertex buffer use for meshed spheres
} ImodPrefStruct;

class PrefsDialog : public QDialog
{
  Q_OBJECT
    
    public:
  PrefsDialog(QWidget *parent = 0);
  QStackedWidget *mStackWidget;
  AppearanceForm *mAppearForm;
  BehaviorForm *mBehaveForm;
  LayoutForm *mLayoutForm;
  MouseForm *mMouseForm;
  SnapshotForm *mSnapForm;
  PrefScalingForm *mScalingForm;
  QListWidget *mListBox;
  QPushButton *mDoneBut, *mDefaultsBut, *mCancelBut;

  public slots:
    void panelSelected(int which);

 protected:
  void closeEvent ( QCloseEvent * e );
  void changeEvent(QEvent *e);

 private:
  void setFontDependentWidths();
};

class ImodPreferences : public QObject
{
  Q_OBJECT

 public:
  ImodPreferences(char *cmdLineStyle);
  ~ImodPreferences() {};
  void saveSettings(int modvAlone);
  void editPrefs();
  double *getZooms() {return &mCurrentPrefs.zooms[0];};
  int getBwStep() {return mCurrentPrefs.bwStep;};
  int getPageStep() {return mCurrentPrefs.pageStep;};
  RETURN_PREF(bool, iconifyImodvDlg);
  RETURN_PREF(bool, iconifyImodDlg);
  RETURN_PREF(bool, iconifyImageWin);
  RETURN_PREF(bool, stackImodDlgs);
  RETURN_PREF(bool, stackImodvDlgs);
  RETURN_PREF(bool, raiseImodDlgStack);
  RETURN_PREF(bool, raiseImodvDlgStack);
  RETURN_PREF(bool, keepDlgStackOnTop);
  int dlgFrameAdjustment();
  RETURN_PREF(int, eerSuperRes);
  RETURN_PREF(int, eerZbinning);
  RETURN_PREF(int, hotSliderKey);
  RETURN_PREF(int, hotSliderFlag);
  RETURN_PREF(int, autoConAtStart);
  RETURN_PREF(bool, startAtMidZ);
  bool hotSliderActive(int ctrlPressed);
  int minCurrentImPtSize();
  int minCurrentModPtSize();
  RETURN_PREF(bool, silentBeep);
  RETURN_PREF(bool, classicSlicer);
  RETURN_PREF(bool, startInHQ);
  RETURN_PREF(bool, arrowsScrollZap);
  bool classicWarned();
  RETURN_PREF(bool, attachToOnObj);
  RETURN_PREF(bool, slicerNewSurf);
  NewObjectProps *newObjectProps() {return &mNewObjProps;};
  void setDefaultObjProps();
  void restoreDefaultObjProps();
  int actualButton(int logicalButton);
  int actualModvButton(int logicalButton);
  QString autosaveDir();
  int autosaveSec();
  ImodPrefStruct *getDialogPrefs() {return &mDialogPrefs;};
  void changeFont(QFont newFont);
  void changeStyle(QString newKey);
  void pointSizeChanged();
  void dlgFrameAdjChanged();
  bool equiv(bool b1, bool b2) {return ((b1 && b2) || (!b1 && !b2));};
  void findCurrentTab();
  void userCanceled();
  const char **getStyleList();
  bool styleOK(QString key);
  int *getStyleStatus();
  void setInfoGeometry();
  QRect getZapGeometry();
  void getAutoContrastTargets(int &mean, int &sd);
  QColor namedColor(int index);
  int saveGenericSettings(const char *key, int numVals, double *values);
  int getGenericSettings(const char *key, double *values, int maxVals);
  QSettings *getSettingsObject();
  void recordZapGeometry();
  void recordModViewGeometry(QRect geom) {mRecordedModViewGeom = geom;};
  QRect getModViewGeometry();
  int getGeometryIndex();
  void recordMultiZparams(QRect geom, int numx, int numy, int zstep, 
                          int drawCen, int drawOther);
  QRect getMultiZparams(int &numx, int &numy, int &zstep, int &drawCen, 
                        int &drawOther);
  bool getRoundedStyle();
  RETURN_PREF(QString, snapFormat);
  RETURN_PREF(int, snapQuality);
  RETURN_PREF(int, snapDPI);
  RETURN_PREF(bool, scaleSnapDPI);
  void setSnapQuality(int value);
  RETURN_PREF(int, slicerPanKb);
  RETURN_PREF(int, maxSlicerBufMB);
  RETURN_PREF(bool, speedupSlider);
  RETURN_PREF(bool, loadUshorts);
  RETURN_PREF(bool, loadIntIfMeanSD);
  RETURN_PREF(bool, loadIntIfEstimate);
  RETURN_PREF(bool, preferMeanSD);
  RETURN_PREF(int, numSDsForScaling);
  RETURN_PREF(int, scaleScanType);
  RETURN_PREF(bool, changeMRCstats);
  RETURN_PREF(int, useAliPieceCoords);  
  RETURN_PREF(bool, isoHighThresh);
  RETURN_PREF(int, isoBoxInitial);
  RETURN_PREF(int, isoBoxLimit);
  RETURN_PREF(bool, keySetsHWstereo);
  RETURN_PREF(bool, noVertBufForCont);
  RETURN_PREF(bool, noVBOForSphere);
  QString snapFormat2(QString *curFormat = NULL);
  void set2ndSnapFormat();
  void restoreSnapFormat();
  QStringList snapFormatList();
  getSetMember(bool, XyzApplyZscale);
  getMember(int, GhostMode);
  getMember(int, GhostDist);
  getSetMember(QString, ImodDlgsInStack);
  getSetMember(QString, ImodDlgStackStates);
  getSetMember(QString, ImodvDlgsInStack);
  getSetMember(QString, ImodvDlgStackStates);
  getMember(bool, ChangingFrameAdj);

  public slots:
    void donePressed();
  void cancelPressed();
  void defaultPressed();

 protected:
  void timerEvent(QTimerEvent *e);

 private:
  ImodPrefStruct mCurrentPrefs;
  ImodPrefStruct mDialogPrefs;
  PrefsDialog *mTabDlg;
  int mCurrentTab;
  int mTimerID;
  int mGeomImageXsize[MAX_GEOMETRIES];
  int mGeomImageYsize[MAX_GEOMETRIES];
  QRect mGeomInfoWin[MAX_GEOMETRIES];
  QRect mGeomZapWin[MAX_GEOMETRIES];
  QRect mGeomModView[MAX_GEOMETRIES];
  TRIPLET(NewObjectProps, mNewObjProps); // new object properties
  QRect mRecordedZapGeom;
  QRect mRecordedModViewGeom;
  int mGeomLastSaved;
  QRect mMultiZgeom;
  int mMultiZnumX, mMultiZnumY;
  int mMultiZstep;
  int mMultiZdrawCen, mMultiZdrawOthers;
  bool mXyzApplyZscale;
  int mGhostMode;
  int mGhostDist;
  bool mClassicWarned;
  QString mSavedSnapFormat;
  QString mImodDlgsInStack;
  QString mImodDlgStackStates;
  QString mImodvDlgsInStack;
  QString mImodvDlgStackStates;
  bool mChangingFrameAdj;
  Ilist *mGenericList;
};

extern ImodPreferences *ImodPrefs;


#endif // IMOD_PREFERENCES_H

