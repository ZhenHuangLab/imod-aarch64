/* 
 *  control.cpp -- Document callback control for drawing, passing keys to, and
 *                  closing image windows
 *               Also implements the DialogManager class for hiding, showing
 *                  and closing dialog  or image windows together
 *
 *  Original author: James Kremer
 *  Revised by: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2019 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 * 
 * $Id$
 */
#include "cppdefs.h"
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <math.h>
#include <string.h>
#include <qwidget.h>
#include <qtimer.h>
#include <qobject.h>
#include <qapplication.h>
#include <QDesktopWidget>
#include <QKeyEvent>
#include "imod.h"
#include "info_setup.h"
#include "info_cb.h"
#include "imodv.h"
#include "mv_menu.h"
#include "mv_window.h"
#include "control.h"
#include "dockingdialog.h"
#include "xxyz.h"
#include "xzap.h"
#include "zap_classes.h"
#include "sslice.h"
#include "slicer_classes.h"
#include "workprocs.h"
#include "preferences.h"
#include "dia_qtutils.h"

/* Structure used by dialog manager */
typedef struct imod_dialog
{
  QWidget *widget;
  int iconified;
  int dlgClass;
  int dlgType;
  int ctrlId;
  int stackIndex;
  QPoint position;
} ImodDialog;

/* Global resident instances of dialog managers for imod and imodv */

DialogManager imodvDialogManager(IMODV_DIALOG);
DialogManager imodDialogManager(IMOD_DIALOG);

#define DOCKER_CHANGE_TIMEOUT 300
#define INFO_SHOW_HIDE_TIMEOUT 100
#define DIALOG_LIST_ITEM(a) (ImodDialog *)ilistItem(mDialogList, a);

static int removeControl(ImodView *iv, int inCtrlId, int callClose);
static QRect adjustedGeometry(QWidget *widget);

/****************************************************************************/

/* Add a control to the imodDraw command, and to the quit command.
 * the return value is used as the inCtrlId field in other control
 * funtions.
 */
int ivwNewControl(ImodView *iv,
                  ImodControlProc draw_cb,
                  ImodControlProc quit_cb,
                  ImodControlKey key_cb,
                  void *data)
{
  ImodControl ctrl;
  static int ctrlId  = 0;
  ctrl.userData = data;
  ctrl.draw_cb  = draw_cb;
  ctrl.close_cb = quit_cb;
  ctrl.key_cb   = key_cb;
  ctrl.id       = 0;
  ctrl.status   = 0;
     
  if (!iv->ctrlist){
    iv->ctrlist = (ImodControlList *)malloc
      (sizeof(ImodControlList));
    if (!iv->ctrlist){
      return(0);
    }
    iv->ctrlist->list = ilistNew(sizeof(ImodControl), 4);
    if (!iv->ctrlist->list){
      free(iv->ctrlist);
      return(0);
    }
    iv->ctrlist->active = 0;
    iv->ctrlist->top    = 0;
  }
  ctrlId++;
  ctrl.id = ctrlId;
  iv->ctrlist->top    = ctrlId;
  iv->ctrlist->active = ctrlId;
  ilistPush(iv->ctrlist->list, &ctrl);
  if (imodDebug('c'))
    imodPrintStderr("Control id %d\n", ctrlId);
  return(ctrlId);
}

/* remove the control associated with the inCtrlId value.
 * do not call the close method of the control
 * This avoids convoluted close logic in the controls!
 */
int ivwRemoveControl(ImodView *iv, int inCtrlId)
{
  return removeControl(iv, inCtrlId, 0);
}

/* delete the control associated with the inCtrlId value.
 * this will also call the close or quit method of the contorl.
 */
int ivwDeleteControl(ImodView *iv, int inCtrlId)
{
  return removeControl(iv, inCtrlId, 1);
}

static int removeControl(ImodView *iv, int inCtrlId, int callClose)
{
  ImodControl *ctrlPtr;
  int element = 0;

  if (!iv->ctrlist) return(0);
  if (iv->ctrlist->list->size < 1) return(0);
  ivwControlListDrawCancel(iv);
  iv->ctrlist->active = 0;
  ctrlPtr = (ImodControl *)ilistFirst(iv->ctrlist->list);
  while(ctrlPtr){
    if (ctrlPtr->id == inCtrlId){
      if (callClose)
        (*ctrlPtr->close_cb)(iv, ctrlPtr->userData, 0);
      ilistRemove(iv->ctrlist->list, element);
      ctrlPtr = (ImodControl *)ilistFirst(iv->ctrlist->list);
      if (ctrlPtr)
        iv->ctrlist->top = ctrlPtr->id;
      return(0);
    }
    element++;
    ctrlPtr = (ImodControl *)ilistNext(iv->ctrlist->list);
  }
  return(1);
}

/* move control to top of control Q if it exists 
 * also sets the control active flag.
 */
int ivwControlPriority(ImodView *iv, int inCtrlId)
{
  ImodControl *ctrlPtr;
  int element = 0;

  if (imodDebug('c'))
    imodPrintStderr("ivwControlPriority: %d\n", inCtrlId);

  if (!iv->ctrlist) return(0);

  iv->ctrlist->active = inCtrlId;
  if (!inCtrlId) return(iv->ctrlist->top);

  if (iv->ctrlist->top == inCtrlId)
    return(inCtrlId);

  ivwControlListDrawCancel(iv);
  ctrlPtr = (ImodControl *)ilistFirst(iv->ctrlist->list);
  while(ctrlPtr){
    if (ctrlPtr->id == inCtrlId){
      ilistFloat(iv->ctrlist->list, element);
      iv->ctrlist->top = inCtrlId;
      return(0);
    }
    element++;
    ctrlPtr = (ImodControl *)ilistNext(iv->ctrlist->list);
  }
  return(iv->ctrlist->top);
}

void ivwControlActive(ImodView *iv, int inCtrlId)
{
  if (iv->ctrlist)
    iv->ctrlist->active = inCtrlId;
}

void ivwControlDraw(ImodView *iv, int reason, int inCtrlId)
{
  ivwControlListDrawCancel(iv);
  ivwControlPriority(iv, inCtrlId);
  ivwControlListDraw(iv, reason);
}

void ivwControlListDrawCancel(ImodView *iv)
{
  if (!iv->ctrlist)
    return;
  iv->timers->mControlTimer->stop();
}

void ivwWorkProc(ImodView *iv)
{

  ImodControl *ctrlPtr;

  if ((!iv->ctrlist)||(!iv->ctrlist->list->size)){
    iv->timers->mControlTimer->stop();
    return;
  }

  ctrlPtr = (ImodControl *)ilistNext(iv->ctrlist->list);
  if (!ctrlPtr){
    iv->timers->mControlTimer->stop();
    return;
  }
  if (imodDebug('c'))
    imodPrintStderr("Drawing %d\n", ctrlPtr->id); 
  (*ctrlPtr->draw_cb)(iv, ctrlPtr->userData, iv->ctrlist->reason);
}

/*
 *  Draw first top priority control and setup workproc 
 *  to update the rest of the controls on the list.
 */
void ivwControlListDraw(ImodView *iv, int reason)
{
  ImodControl *ctrlPtr;

  if (!iv->ctrlist) return;
  if (!iv->ctrlist->list->size) return;
  iv->ctrlist->reason = reason;
  ivwControlListDrawCancel(iv);
     
  ctrlPtr = (ImodControl *)ilistFirst(iv->ctrlist->list);
  if (ctrlPtr) {
    if (imodDebug('c'))
      imodPrintStderr("Drawing priority %d\n", ctrlPtr->id);
    if (ctrlPtr->id == iv->ctrlist->active){
      iv->ctrlist->active = 0;
      (*ctrlPtr->draw_cb)(iv, ctrlPtr->userData,
                          reason | IMOD_DRAW_TOP | IMOD_DRAW_ACTIVE);
    }else{
      (*ctrlPtr->draw_cb)(iv, ctrlPtr->userData, 
                          reason | IMOD_DRAW_TOP);
    }
  }

  /* draw the rest of the windows. */
  iv->timers->mControlTimer->start(1);

  /*     while(NULL != (ctrlPtr = ilistNext(iv->ctrlist->list))){
         (*ctrlPtr->draw_cb)(iv, ctrlPtr->userData, reason);
         }
  */
  return;
}

void ivwControlListDelete(ImodView *iv)
{
  ImodControl *ctrlPtr;
     
  if (!iv->ctrlist) return;
  ctrlPtr = (ImodControl *)ilistFirst(iv->ctrlist->list);
  while(ctrlPtr){
    (*ctrlPtr->close_cb)(iv, ctrlPtr->userData, 0);
    ctrlPtr = (ImodControl *)ilistNext(iv->ctrlist->list);
  }
  ilistDelete(iv->ctrlist->list);
  free(iv->ctrlist);
  iv->ctrlist = NULL;
}

// Pass a key event on to the first control on the list that has a callback
// Leave out the iv parameter so form classes can call directly
void ivwControlKey(/*ImodView *iv, */int released, QKeyEvent *e)
{
  ImodView *iv = App->cvi;
  ImodControl *ctrlPtr;
  
  if (!iv->ctrlist) return;
  if (!iv->ctrlist->list->size) return;
     
  ctrlPtr = (ImodControl *)ilistFirst(iv->ctrlist->list);
  while (ctrlPtr) {
    if (imodDebug('c'))
      imodPrintStderr("checking %d\n", ctrlPtr->id);
    if (ctrlPtr->key_cb) {
      if (imodDebug('c'))
        imodPrintStderr("sending to %d\n", ctrlPtr->id);
      (*ctrlPtr->key_cb)(iv, ctrlPtr->userData, released, e);
      return;
    }
    ctrlPtr = (ImodControl *)ilistNext(iv->ctrlist->list);
  }
}
static bool sModelViewActive = false;
static bool sNeedModvActive;
static QWidget *sActivatingWidget;
static bool sSwitchBusy = false;

void ivwCheckAndSetMacMenu(QEvent *event, QWidget *widget, int dialogType)
{
#if defined(Q_OS_MACX) && QT_VERSION >= 0x050000
  if (event->type() != QEvent::ActivationChange || sSwitchBusy || 
      !widget->isActiveWindow())
    return;
  sNeedModvActive = dialogType == IMODV_DIALOG;
  sActivatingWidget = widget;
  if (sModelViewActive && !sNeedModvActive || !sModelViewActive && sNeedModvActive) {
    sSwitchBusy = true;
    App->cvi->timers->startMenuSwitch();
  }
#endif
}
void ivwSwitchMacMenus()
{
  if (sNeedModvActive)
    Imodv->mainWin->activateWindow();
  else
    ImodInfoWin->activateWindow();
  sModelViewActive = sNeedModvActive;
  imod_info_input();
  sActivatingWidget->activateWindow();
  sSwitchBusy = false; 
}

void ivwModvMenuIsActive(bool inVal)
{
  sModelViewActive = inVal;
}

bool ivwGetRoundedStyle(void)
{
  return ImodPrefs->getRoundedStyle();
}

/***************************************************************************
 * DIALOG MANAGER CLASS - FOR HIDING, SHOWING, AND CLOSING DIALOGS
 *      This may include image windows
 */

DialogManager::DialogManager(int dlgClass)
{
  mDialogList = NULL;
  mLastZapGeom.setRect(0, 0, 0, 0);
  mDlgClass = dlgClass;
  mMovedDlgIndex = -1;
  mRestacking = false;
  mRestoringStack = false;
  mNextDockerState = -1;
  mKeepStackOnTop = false;
  mHidingOrShowing = false;
  mInfoShowing = false;
  mInfoHiding = false;
}

// Add a dialog to the list.  A docking dialog must be shown at its proper size first
void DialogManager::add(QWidget *widget, int dlgClass, int dlgType, int ctrlId)
{
  ImodDialog dia;

  if (!mDialogList) {
    mDialogList = ilistNew(sizeof(ImodDialog), 4);
    if (!mDialogList) {
      imodError(NULL, "3DMOD WARNING: Failure to get memory for dialog list\n");
      return;
    }
  }

  dia.widget = widget;
  dia.iconified = 0;
  dia.dlgClass = dlgClass;
  dia.dlgType = dlgType;
  dia.ctrlId = ctrlId;
  dia.stackIndex = -2;

  // Add a docking dialog to the stack at this point
  if (dlgType == DOCKING_DIALOG_TYPE && 
      ((dlgClass == IMOD_DIALOG && ImodPrefs->stackImodDlgs()) ||
       (dlgClass == IMODV_DIALOG && ImodPrefs->stackImodvDlgs()))) {

    // If restoring stack, a new list is being built for that;
    // otherwise add to list of ones to add to end
    if (mRestoringStack) {
      dia.stackIndex = 0;
    } else {
      mNewDlgIndexes.push_back(ilistSize(mDialogList));
      startDockTimer(dlgClass, 10);
    }
  }
  ilistAppend(mDialogList, &dia);

  // Set the icon from the appropriate place
  switch (dlgClass) {
  case IMOD_DIALOG:
  case IMOD_IMAGE:
    widget->setWindowIcon(*(App->iconPixmap));
    break;
  case IMODV_DIALOG:
    widget->setWindowIcon(*(Imodv->iconPixmap));
    break;
  }
}

// Remove a dialog from the list
void DialogManager::remove(QWidget * widget)
{
  int index, stk;
  bool needRestack = false;
  if (!mDialogList)
    return;
  ImodDialog *dia = lookupDialogInList(widget, index);
  
  if (!dia) {
    imodError(NULL, "3DMOD WARNING: Failed to find closing dialog on list\n");
    return;
  }
  if (dia->dlgType == ZAP_WINDOW_TYPE)
    mLastZapGeom = ivwRestorableGeometry(widget);
  if (dia->dlgType == DOCKING_DIALOG_TYPE && dia->stackIndex >= 0) {
    needRestack = true;
    mDockedDiaInd.erase(mDockedDiaInd.begin() + dia->stackIndex);
  }
  for (stk = 0; stk < mDockedDiaInd.size(); stk++)
    if (mDockedDiaInd[stk] >= index)
      mDockedDiaInd[stk]--;
  ilistRemove(mDialogList, index);
  if (needRestack)
    restackDialogs();
}

// Close all dialogs and delete the list
void DialogManager::close()
{
  ImodDialog *dia;
  Ilist *tempList = mDialogList;

  // Put list in a local variable and null out the static one so that removals
  // are ignored
  if (!mDialogList)
    return;

  mDialogList = NULL;

  dia = (ImodDialog *)ilistFirst(tempList);
  while (dia){
    dia->widget->close();
    dia = (ImodDialog *)ilistNext(tempList);
  }
  ilistDelete(tempList);
  mDockedDiaInd.clear();
}

// The info window is hiding: start timer
void DialogManager::hide()
{
  startDockTimer(mDlgClass, INFO_SHOW_HIDE_TIMEOUT);
  mInfoHiding = true;
  mInfoShowing = false;
}

// Iconify any dialogs that are shown now; keep track of them
void DialogManager::delayedHide()
{
  int hideTogether[6];
  ImodDialog *dia;

  // Get flags for whether dialog classes should be hidden together
  hideTogether[IMODV_DIALOG] = ImodPrefs->iconifyImodvDlg();
  hideTogether[IMOD_DIALOG] = ImodPrefs->iconifyImodDlg();
  hideTogether[IMOD_IMAGE] = ImodPrefs->iconifyImageWin();
  mHidingOrShowing = true;
  dia = (ImodDialog *)ilistFirst(mDialogList);
  while (dia){
    // Do not iconify if it is already minimized or if this class should
    // not be hidden together
    if (dia->widget->isHidden() || dia->widget->isMinimized() || 
        (!hideTogether[dia->dlgClass] && dia->stackIndex < 0))
      dia->iconified = 0;
    else {
      dia->iconified = 1;
      dia->position = (ivwRestorableGeometry(dia->widget)).topLeft();
      imodTrace('c', "Hiding %p", dia->widget);

      // This was showMinimized for everyone originally; worked fine on
      // Windows and Linux under Qt 3.0.5, didn't work on SGI because the boxes
      // didn't come back up.  Then the boxes didn't come up under RH 9 
      // (Qt 3.1), so switched to hide, which works on Linux, SGI, and Windows
      // and has the advantage of producing a single icon.
      // But on the Mac it made the main imodv window bounce back out!
      // showNormal and show are interchangeable for getting back up
#ifdef Q_OS_MACX
      dia->widget->showMinimized();
#else
      dia->widget->hide();
#endif
    }
    dia = (ImodDialog *)ilistNext(mDialogList);
  }
  imod_info_input();
  mHidingOrShowing = false;
}

// Info is showing
void DialogManager::show()
{
  startDockTimer(mDlgClass, INFO_SHOW_HIDE_TIMEOUT);
  mInfoShowing = true;
  mInfoHiding = false;
}

// Show dialogs that were brought down by the hide operation
void DialogManager::delayedShow()
{
  ImodDialog *dia;
  mHidingOrShowing = true;
  dia = (ImodDialog *)ilistFirst(mDialogList);
  while (dia){
    if (dia->iconified) {
      imodTrace('c', "Showing %p", dia->widget);
      dia->widget->move(dia->position);
      dia->widget->showNormal();
    }
    dia = (ImodDialog *)ilistNext(mDialogList);
  }
  imod_info_input();
  mHidingOrShowing = false;
}

// Return the appropriate parent based on type of window and OS
QWidget *DialogManager::parent(int dlgClass)
{
#ifdef Q_OS_MACX
  return stackMasterWindow();
#else
  return NULL;
#endif
}

// Return the master window of the stack for the given type of window
QWidget *DialogManager::stackMasterWindow()
{
  if (mDlgClass == IMODV_DIALOG)
    return (QWidget *)Imodv->mainWin;
  else
    return (QWidget *)ImodInfoWin;
}

// Raise all windows of the given class
void DialogManager::raise(int dlgClass)
{
  ImodDialog *dia;
  dia = (ImodDialog *)ilistFirst(mDialogList);
  while (dia){
    if (dia->dlgClass == dlgClass && dia->widget->isVisible())
      dia->widget->raise();
    dia = (ImodDialog *)ilistNext(mDialogList);
  }
}

// Return the number of windows of a given type
int DialogManager::windowCount(int dlgType)
{
  int num = 0;
  ImodDialog *dia;
  dia = (ImodDialog *)ilistFirst(mDialogList);
  while (dia){
    if (dia->dlgType == dlgType) 
      num++;
    dia = (ImodDialog *)ilistNext(mDialogList);
  }
  return num;
}

// Return the size of the biggest Zap window still open, or of the last
// Zap window closed if none are open
QRect DialogManager::biggestGeometry(int dlgType)
{
  QRect biggest(0, 0, 0, 0);
  
  ImodDialog *dia;
  dia = (ImodDialog *)ilistFirst(mDialogList);
  while (dia){
    if (dia->dlgType == dlgType) {
      QRect geom = ivwRestorableGeometry(dia->widget);
      if (geom.width() * geom.height() > biggest.width() * biggest.height())
        biggest = geom;
    }
    dia = (ImodDialog *)ilistNext(mDialogList);
  }
  if (!biggest.width())
    return mLastZapGeom;
  return biggest;
}

// Construct a list of widgets matching the given class and type; pass -1 to
// match any class or type
void DialogManager::windowList(QObjectList *objList, int dlgClass, int dlgType)
{
  ImodDialog *dia;
  objList->clear();
  dia = (ImodDialog *)ilistFirst(mDialogList);
  while (dia){
    if ((dlgType < 0 || dia->dlgType == dlgType) && 
        (dlgClass < 0 || dia->dlgClass == dlgClass))
      objList->append((QObject *)dia->widget);
    dia = (ImodDialog *)ilistNext(mDialogList);
  }
}

// Find the window of a given type closest to top of control list, where dlgType
// can be any of the IMOD_IMAGE types that can open multiple copies
QObject *DialogManager::getTopWindow(int dlgType)
{
  int found;
  return getTopWindow(dlgType, dlgType, found);
}

// Find the first window of either of two types closest to top of control list,
// where the two types can be any of the IMOD_IMAGE types that can open multiple copies.
QObject *DialogManager::getTopWindow(int dlgType, int dlgType2, int &typeFound)
{
  ImodControl *ctrlPtr;
  int i, j, curSave;
  bool match = false;
  ImodDialog *dia;

  if (!ilistSize(mDialogList) || !App->cvi->ctrlist)
    return NULL;

  // Look through the current control list, find first dialog with matching ID and type
  // Here we MUST save and restore current item to avoid screwing up draws
  typeFound = -1;
  curSave = App->cvi->ctrlist->list->current;
  for (j = 0; !match && j < ilistSize(App->cvi->ctrlist->list); j++) {
    ctrlPtr = (ImodControl *)ilistItem(App->cvi->ctrlist->list, j);
    for (i = 0; i < ilistSize(mDialogList); i++) {
      dia = DIALOG_LIST_ITEM(i);
      if (dia->dlgType == dlgType || dia->dlgType == dlgType2) {
        match = ctrlPtr->id == dia->ctrlId;
        if (match) {
          typeFound = dia->dlgType;
          break;
        }
      }
    }
  }
  App->cvi->ctrlist->list->current = curSave;
  if (!match)
    return NULL;
  return dia->widget;
}

/*
 * Find either the first Zap window of the given type (default regular zap) or the first 
 * slicer window.  If seeking a Zap window, it looks for one with a rubberband 
 * if flag is set, or with a lasso if that flag is set (default false) or with either one
 * if both flags are set.  It tests that the band is usable for file extraction (either
 * starting with low-high set, or big enough).  If seeking a slicer, it looks for one
 * with a rubberband starting or present, with no tests for extraction validity.
 * Returns the window list index in *index if index is nonNULL.
 */
QObject *DialogManager::getTopWindow(bool withBand, bool withLasso, int type, int *index)
{
  QObjectList objList;
  ZapFuncs *zap;
  SlicerFuncs *slicer;
  ImodControl *ctrlPtr;
  int ixl, ixr, iyb, iyt;
  int i, j, curSave, topOne;

  if (index)
    *index = -1;
  imodDialogManager.windowList(&objList, -1, type);
  if (!objList.count())
    return NULL;

  // Loop through the control list and find the first window that is a zap with a viable
  // rubberband if required, which is either a drawn band or Z limits set & starting band
  // It is best to save and restore ctrlist current item
  topOne = -1;
  curSave = App->cvi->ctrlist->list->current;
  for (j = 0; topOne < 0 && j < ilistSize(App->cvi->ctrlist->list); j++) {
    ctrlPtr = (ImodControl *)ilistItem(App->cvi->ctrlist->list, j);
    for (i = 0; i < objList.count(); i++) {
      if (type == SLICER_WINDOW_TYPE) {

        // Check a slicer
        slicer = ((SlicerWindow *)objList.at(i))->mFuncs;
        if (ctrlPtr->id == slicer->mCtrl && 
            (!withBand || slicer->mRubberband || slicer->mStartingBand)) {
          if (index)
            *index = j;
          topOne = i;
          break;
        }
      } else {

        // Or check a zap
        zap = ((ZapWindow *)objList.at(i))->mZap;
        if (ctrlPtr->id == zap->mCtrl && 
            (!withLasso || (zap->mLassoOn && !zap->mDrawingLasso)) &&
            (!withBand || zap->mRubberband || 
             (zap->mStartingBand && zap->getLowHighSection(ixl, ixr)))) {
          if (zap->mRubberband) {
            ixl = (int)floor(zap->mRbImageX0 + 0.5);
            ixr = (int)floor(zap->mRbImageX1 - 0.5);
            iyb = (int)floor(zap->mRbImageY0 + 0.5);
            iyt = (int)floor(zap->mRbImageY1 - 0.5);
            if (ixr < 1 || iyt < 1 || ixl > zap->mVi->xsize - 2 || 
                iyb > zap->mVi->ysize - 2)
              continue;
          }
          topOne = i;
          if (index)
            *index = j;
          break;
        }
      }
    }
  }
  App->cvi->ctrlist->list->current = curSave;
  if (topOne < 0 && (withBand || withLasso))
    return NULL;
  if (topOne < 0)
    topOne = 0;
  return objList.at(topOne);
}

/*
 *  DOCKER - STACK FUNCTIONS
 */

// External call from a dialog that it has moved - save index and start a timer in case
// there are multiple events in quick succession
void DialogManager::dockerHasMoved(DockingDialog *dialog, QMoveEvent *e)
{
  int index;
  ImodDialog *dia = lookupDialogInList((QWidget *)dialog, index);
  if (!dia || dia->dlgType != DOCKING_DIALOG_TYPE || dia->stackIndex < -1 || mRestacking)
    return;
  //imodPrintStderr("docker move %d %d\n", e->pos().x(), e->pos().y());
  startDockTimer(dia->dlgClass, DOCKER_CHANGE_TIMEOUT);
  mMovedDlgIndex = index;
}

// External call from a dialog that it has resized, also start the timer
void DialogManager::dockerHasResized(DockingDialog *dialog, QResizeEvent *e)
{
  int index;
  ImodDialog *dia = lookupDialogInList((QWidget *)dialog, index);
  if (!dia || dia->dlgType != DOCKING_DIALOG_TYPE || dia->stackIndex < 0)
    return;
  //imodPrintStderr("docker resize %d %d\n", e->size().width(), e->size().height());
  startDockTimer(dia->dlgClass, DOCKER_CHANGE_TIMEOUT);
  // Do not preempt a moved index
  //mMovedDlgIndex = -1;
}

// External call from info/MV that it has resized or moved, also start the timer
void DialogManager::masterWinHasChanged(int dlgClass)
{
  mMovedDlgIndex = -1;
  startDockTimer(dlgClass, DOCKER_CHANGE_TIMEOUT);
}

// External call from a dialog that is being hidden
// Add to list of hidden dialogs and start timer
void DialogManager::dockerWasHidden(DockingDialog *dialog)
{
  int index;
  ImodDialog *dia;
  if (mHidingOrShowing)
    return;
  dia = lookupDialogInList((QWidget *)dialog, index);
  if (!dia || dia->stackIndex < 0)
    return;
  mHiddenDlgListInds.push_back(index);
  startDockTimer(mDlgClass, 50);
}

// External call from a dialog that is being unhidden
// Add to list of shown dialogs and start timer
void DialogManager::dockerWasUnhidden(DockingDialog *dialog)
{
  int index;
  ImodDialog *dia;
  if (mHidingOrShowing)
    return;
  dia = lookupDialogInList((QWidget *)dialog, index);
  if (!dia)
    return;
  imodTrace('c', "Docker %d %p unhidden added to list", index, dialog);
  mShownDlgListInds.push_back(index);
  startDockTimer(mDlgClass, 50);
}

void DialogManager::dockerChangedScreen(DockingDialog *dialog)
{
  int index;
  ImodDialog *dia = lookupDialogInList((QWidget *)dialog, index);
  if (!dia || dia->dlgType != DOCKING_DIALOG_TYPE || dia->stackIndex < -1 || mRestacking)
    return;
  startDockTimer(dia->dlgClass, 3 * DOCKER_CHANGE_TIMEOUT);
  mMovedDlgIndex = index;
}

void DialogManager::masterChangedScreen(float oldDPR, float newDPR)
{
  int index;
  ImodDialog *dia;
  if (fabs(oldDPR - newDPR) < 0.01)
    return;
  for (index = 0; index < ilistSize(mDialogList); index++) {
    dia = DIALOG_LIST_ITEM(index);
    if (dia->stackIndex >= 0)
      dia->stackIndex = -1;
  }
  mDockedDiaInd.clear();
}

// Start the timer that exists depending on how the program was started
void DialogManager::startDockTimer(int dlgClass, int timeout)
{
  if (dlgClass == IMODV_DIALOG && imodvStandalone())
    Imodv->vi->timers->startDlgDockTimer(dlgClass, timeout);
  else
    App->cvi->timers->startDlgDockTimer(dlgClass, timeout);
}

// A dialog received the input focus
void DialogManager::dockerWasActivated(DockingDialog *dialog)
{
  int stkInd, index = -1;
  ImodDialog *dia;

  // Ignore if the appropriate option is not set
  if ((mDlgClass == IMOD_DIALOG && !ImodPrefs->raiseImodDlgStack()) ||
      (mDlgClass == IMODV_DIALOG && (!ImodPrefs->raiseImodvDlgStack() || ImodvClosed)) || 
      mNewDlgIndexes.size() > 0 || mRestacking || App->exiting)
    return;

  // Get the dialog and see if it is in the stack, then do the raise
  if (dialog) {
    dia = lookupDialogInList((QWidget *)dialog, index);
    if (!dia || index < 0)
      return;

    // If it is the master and there are no stacked dialogs, skip it
  } else if (!mDockedDiaInd.size()) {
    return;
  }
  stackMasterWindow()->raise();

  // Include the provoking dialog in case it is move to focus not click to focus
  for (stkInd = 0; stkInd < mDockedDiaInd.size(); stkInd++) {
    dia = DIALOG_LIST_ITEM(mDockedDiaInd[stkInd]);
    dia->widget->raise();
  }
}

// Notification from info win or preferences to determine whether to keep dialogs on top
// and call the function for each with the state
void DialogManager::manageStayingOnTop()
{
  int stkInd;
  ImodDialog *dia;
  bool keep = ImodPrefs->keepDlgStackOnTop() && ImodInfoWin->getStayingOnTop();
  if ((keep && mKeepStackOnTop) || (!keep && !mKeepStackOnTop))
    return;
  mKeepStackOnTop = keep;
  for (stkInd = 0; stkInd < mDockedDiaInd.size(); stkInd++) {
    dia = DIALOG_LIST_ITEM(mDockedDiaInd[stkInd]);
    utilSetStayOnTop(dia->widget, keep);
  }
}

// The timeout instigates the insertion of new dialogs, adaptation to a resize, or 
// detection of where a moved dialog has inserted itself
void DialogManager::stackChangeTimeout()
{
  ImodDialog *dia, *movedDia, *pdia;
  QRect rect, movedRect, masterRect, prect;
  int tbYmid, tbXmid, colInd, newInd, stackInd, stkLim, bottom, indent;
  int edgeYtop, edgeYbot, edgeXmid, masterIndex, prevInd;
  int movedDlgIndex = mMovedDlgIndex;
  float fracMaster = 0.5, fracDia = 0.5;
  int glueDist = 5, detachDist = 10;
  bool inMasterTB, inMasterLR, isSplit, inDiaTop, inDiaBot;
  bool addBefore, addAfter, testLateral, doAddToEnd, justUnstack;
  bool alreadyDocked, inExtendedTop, inExtendedBot, lateralToMaster, touchingPrev;

  mMovedDlgIndex = -1;

  // If info is showing or hiding, do the delayed operation and ignore any dialog
  // shows/hides that came in, probably from desktop change
  if (mInfoShowing || mInfoHiding) {
    if (mInfoHiding)
      delayedHide();
    if (mInfoShowing)
      delayedShow();
    mInfoShowing =false;
    mInfoHiding = false;

  } else if (mHiddenDlgListInds.size() || mShownDlgListInds.size()) {

    // Otherwise process any dialog hides that have come in, then any shows
    for (newInd = 0; newInd < mHiddenDlgListInds.size(); newInd++) {
      dia = DIALOG_LIST_ITEM(mHiddenDlgListInds[newInd]);
      if (dia && dia->stackIndex < mDockedDiaInd.size()) {
        mDockedDiaInd.erase(mDockedDiaInd.begin() + dia->stackIndex);
        dia->stackIndex = -1;
      }
    }

    // Not clear what to do here, treating it like a move puts it back into the 
    // original place but loses it from stack if info has moved, and can't handle
    // multiple shows.  Thus treat it like addition at the end
    for (newInd = 0; newInd < mShownDlgListInds.size(); newInd++)
      mNewDlgIndexes.push_back(mShownDlgListInds[newInd]);
    /* dia = DIALOG_LIST_ITEM(mShownDlgListInds[newInd]);
      if (dia) {
        movedDlgIndex = mShownDlgListInds[newInd];
      }
      }*/
  }
  mHiddenDlgListInds.clear();
  mShownDlgListInds.clear();

  // If there were new dialogs, add them to stack now and go on to restack
  if (mNewDlgIndexes.size() || mRestoringStack) {
    for (newInd = 0; newInd < mNewDlgIndexes.size(); newInd++) {
      dia = DIALOG_LIST_ITEM(mNewDlgIndexes[newInd]);
      dia->stackIndex = mDockedDiaInd.size();
      mDockedDiaInd.push_back(mNewDlgIndexes[newInd]);
      if (mKeepStackOnTop)
        utilSetStayOnTop(dia->widget, true);
    }
    mNewDlgIndexes.clear();
    mRestoringStack = false;
    
    // If a dialog moved, get it and see if its title bar fits in the stack
  } else if (movedDlgIndex >= 0) {
    movedDia = DIALOG_LIST_ITEM(movedDlgIndex);
    if (!movedDia)
      return;

    DockingDialog *docker = (DockingDialog *)movedDia->widget;
    if (docker->getScreenChanged()) {
      float curDPR = docker->getCurDevPixRatio();
      docker->resetScreenChanged();
      float newDPR = utilGetNewDevPixRatio(docker);
      if (newDPR) {
        docker->setCurDevPixRatio(newDPR);
        if (curDPR > 0. && newDPR != curDPR) {
          docker->adjustSize();
          if (movedDia->stackIndex >= 0 && !mRestacking) {
            mDockedDiaInd.erase(mDockedDiaInd.begin() + movedDia->stackIndex);
            movedDia->stackIndex = -1;
            restackDialogs();
          }
          return;
        }
      }
    }

    // Get middle of title bar
    alreadyDocked = movedDia->stackIndex >= 0;
    movedRect = adjustedGeometry(movedDia->widget);
    indent = (movedRect.height() - movedDia->widget->height()) / 2;
    tbYmid = movedRect.top() + indent;
    tbXmid = movedRect.left() + movedRect.width() / 2;
    masterRect = adjustedGeometry(stackMasterWindow());

    // Get an equivalent position in from the side opposite to stack row direction
    edgeYtop = movedRect.top() + indent;
    edgeYbot = movedRect.bottom() - indent;
    edgeXmid = mStackRowDir > 0 ? (movedRect.left() - glueDist) : 
      (movedRect.right() + glueDist);

    // Is it within the vertical or horizontal extent of the master?
    inMasterTB = tbYmid > masterRect.top() && tbYmid <= masterRect.bottom();
    inMasterLR = tbXmid > masterRect.left() && tbXmid < masterRect.right();
    justUnstack = false;
    //PRINT3(inMasterTB, inMasterLR, alreadyDocked);
    //PRINT4(mStackColLeft[0], masterRect.right(), mStackColRight[0], masterRect.left());

    // If there is nothing stacked or stack starts beside master and it falls in 
    // the master column, just compare with master
    if (!mDockedDiaInd.size() || !mStackColLeft.size() || 
        mStackColLeft[0] == mStackColRight[0] || 
        (inMasterLR && (mStackColLeft[0] >= masterRect.right() ||
                        mStackColRight[0] <= masterRect.left()))) {

      //PRINT3(movedRect.top(), masterRect.top(), movedRect.bottom());

      // If it falls in the master, try to add it at front of stack
      if ((inMasterTB || 
           (movedRect.top() < masterRect.top() && 
            movedRect.bottom() >  masterRect.top() - glueDist) ||
           (movedRect.bottom() > masterRect.bottom() && 
            movedRect.top() <  masterRect.bottom() + glueDist)) && inMasterLR) {
        if (alreadyDocked)
          mDockedDiaInd.erase(mDockedDiaInd.begin() + movedDia->stackIndex);
        mDockedDiaInd.insert(mDockedDiaInd.begin(), movedDlgIndex);
        movedDia->stackIndex = 0;
      } else {

        // Otherwise take it out of stack if in
        if (alreadyDocked)
          justUnstack = true;
        else
          return;
      }

    } else {

      // If there is a stack, see if it falls into a column
      for (colInd = 0; colInd < mStackColLeft.size(); colInd++)
        if (tbXmid >= mStackColLeft[colInd] && tbXmid <= mStackColRight[colInd]) 
          break;

      //PRINT3(colInd, mStackColLeft.size(), tbXmid);
      // If it falls outside, see if that edge is in the last column, set up to test that
      // Also test if the edge falls with the master extent
      lateralToMaster = edgeXmid <= masterRect.right() && edgeXmid >= masterRect.left();
      testLateral = colInd >= mStackColLeft.size() && 
        ((edgeXmid > mStackColLeft[colInd - 1] && edgeXmid < mStackColRight[colInd - 1])
         || (mColIsSplit[colInd - 1] && lateralToMaster));
      if (testLateral)
        colInd--;
      if (colInd >= mStackColLeft.size()) {

        // if it still falls outside, take it out if in
        if (alreadyDocked)
          justUnstack = true;
        else 
          return;

      } else {

        // Otherwise need to walk through column to find where to put it
        isSplit = mColIsSplit[colInd];
        stkLim = mDockedDiaInd.size();
        if (colInd < mColStartInd.size() - 1)
          stkLim = mColStartInd[colInd + 1];

        // Find index of first dialog past the master in a split column, if any
        masterIndex = -1;
        if (isSplit) {
          for (masterIndex = mColStartInd[colInd]; masterIndex < stkLim; masterIndex++) {
            dia = DIALOG_LIST_ITEM(mDockedDiaInd[masterIndex]);
            if (dia->position.y() > masterRect.top())
              break;
          }
        }

        // If it touches the master, set flag to add at end of column
        doAddToEnd = testLateral && isSplit && lateralToMaster && 
          edgeYbot > masterRect.top() && edgeYtop < masterRect.bottom();

        // Go through the dialogs in this column
        for (stackInd = mColStartInd[colInd]; stackInd < stkLim; stackInd++) {

          // Once a lateral test is satisfied, skip all the dialogs but the last one,
          // the one after which it will be added
          if (doAddToEnd && stackInd < stkLim - 1)
            continue;
          dia = DIALOG_LIST_ITEM(mDockedDiaInd[stackInd]);
          rect = adjustedGeometry(dia->widget);
          bottom = dia->position.y() + rect.height();
          addBefore = false;

          // For lateral test see if it falls within the box
          if (testLateral && !doAddToEnd && mDockedDiaInd[stackInd] != movedDlgIndex &&
              edgeXmid > dia->position.x() && edgeXmid < dia->position.x() + rect.width()
              && edgeYbot > dia->position.y() && edgeYtop < bottom) {
            doAddToEnd = true;
            if (stackInd < stkLim - 1)
              continue;
          }
          addAfter = doAddToEnd;

          if (!testLateral) {

            // Evaluate if in top or bottom of dialog, or in master
            // Use original position in case this is the one that moved
            // In a split column, the top is extended such that any part of the dialog
            // intersecting either this dialog or the master above it will cause it 
            // to be inserted before
            inExtendedTop = false;
            if (stackInd == mColStartInd[colInd] && isSplit) {
              inExtendedTop = movedRect.bottom() < bottom && 
                  movedRect.bottom() > B3DMIN(dia->position.y(), masterRect.top()) - 
                  glueDist;
            }  
            inDiaTop = tbYmid >= dia->position.y() && 
              tbYmid <= dia->position.y() + fracDia * rect.height();

            // The bottom is extended in any column, but evaluated differently for 
            // a split column to include the master if it is below
            inExtendedBot = false;
            if (stackInd == stkLim - 1) {
              if (isSplit)
                inExtendedBot = movedRect.top() > dia->position.y() && 
                  movedRect.top() < B3DMAX(bottom, masterRect.bottom()) + glueDist;
              else
                inExtendedBot = movedRect.top() > dia->position.y() && 
                  movedRect.top() < bottom + glueDist;
              //PRINT4(dia->position.y(), bottom, movedRect.top(), inExtendedBot);
            }
            inDiaBot = tbYmid <= bottom && tbYmid >= bottom - fracDia * rect.height();
            //PRINT4(inDiaTop, inDiaBot, inExtendedTop, inExtendedBot);

            // If it falls in itself but off to the side by enough, take it out
            if (!isSplit && (inDiaBot || inDiaTop)
                && mDockedDiaInd[stackInd] == movedDlgIndex && 
                mStackRowDir * (movedRect.left() - dia->position.x()) > detachDist) {
              justUnstack = true;

            } else {
              //PRINT3(stackInd, masterIndex, masterRect.bottom());
            
              // Otherwise decide whether to add before or after the current one 
              addBefore = inDiaTop || (!inDiaBot && inExtendedTop) || 
                (stackInd == masterIndex && inMasterTB);
              addAfter = inDiaBot || (!inDiaTop && inExtendedBot) ||
                (stackInd == masterIndex - 1 && inMasterTB);
            }

            // If it is not being added here and it is above the first one in this column
            // or below the last one, see if it is touching one in the previous column:
            // If so add it before or after this one
            if (!addBefore && !addAfter && colInd > 0 &&
                ((stackInd == mColStartInd[colInd] && 
                  movedRect.bottom() < dia->position.y()) ||
                 (stackInd == stkLim - 1 && movedRect.top() > bottom))) {
              touchingPrev = lateralToMaster;
              for (prevInd = mColStartInd[colInd - 1];
                   !touchingPrev && prevInd < mColStartInd[colInd]; prevInd++) {
                pdia = DIALOG_LIST_ITEM(mDockedDiaInd[prevInd]);
                prect = pdia->widget->frameGeometry();
                touchingPrev = edgeXmid > pdia->position.x() && 
                  edgeXmid < pdia->position.x() + prect.width()
                  && edgeYbot > pdia->position.y() && 
                  edgeYtop < pdia->position.y() + prect.bottom();
              }
              addBefore == touchingPrev && movedRect.bottom() < dia->position.y();
              addAfter = touchingPrev && movedRect.top() > bottom;
            }
              
          }
          if (addBefore || addAfter) {

            // If adding, set up new index
            newInd = stackInd;
            if (addAfter)
              newInd++;

            // If it already is in the stack, just restack if it comes out the same,
            // or take it out first, dropping the new index if it is higher 
            if (alreadyDocked) {
              if (newInd == movedDia->stackIndex)
                break;
              if (movedDia->stackIndex < newInd)
                newInd--;
              mDockedDiaInd.erase(mDockedDiaInd.begin() + movedDia->stackIndex);
            }

            // Add the index (back) to the stack
            if (newInd < mDockedDiaInd.size())
              mDockedDiaInd.insert(mDockedDiaInd.begin() + newInd, movedDlgIndex);
            else
              mDockedDiaInd.push_back(movedDlgIndex);
            movedDia->stackIndex = newInd;
            break;
          }
        }
      }

      // If it should be unstacked, or if it was lateral test and never got added,
      // take it out and cancel the stay on top
      if (justUnstack || (testLateral && !doAddToEnd && alreadyDocked)) {
        mDockedDiaInd.erase(mDockedDiaInd.begin() + movedDia->stackIndex);
        movedDia->stackIndex = -1;
        if (mKeepStackOnTop)
          utilSetStayOnTop(movedDia->widget, false);
      }
    }
  }
  restackDialogs();
}

// Rebuild the dialog stack given the current list of ones in it and their positions
void DialogManager::restackDialogs()
{
  int deskHeight, deskWidth, usableWidth, usableHeight, usableTop, usableBottom;
  int colInd, maxWidth, freeStart, freeInCol, colPart, stackInd, colWidth, colStart;
  int newLeft, newTop, usableRight, usableLeft, masterLeft, masterRight, masterWidth;
  int placeInd, placeColInd, firstDiaToPlace, nextDiaToPlace,loop;
  float fracRemain = 0.33, fracOverhang = 0.3;
  QWidget *masterWin = stackMasterWindow();
  bool isSplit;
  ImodDialog *dia;
  QRect *stkRects, masterRect;
  if (!mDockedDiaInd.size())
    return;
  diaScreenSize(deskWidth, deskHeight, masterWin);
  diaMaximumWindowSize(usableWidth, usableHeight, masterWin);
  diaMinimumWindowPos(usableLeft, usableTop, masterWin);
  usableBottom = usableTop + usableHeight;
  usableRight = usableLeft + usableWidth;
  masterRect = adjustedGeometry(masterWin);
  masterLeft = masterRect.left();
  masterRight = masterRect.right();
  masterWidth = masterRect.width();

  // Set up start of first column, above master
  mStackColLeft.clear();
  mStackColRight.clear();
  mColIsSplit.clear();
  mColStartInd.clear();
  if (masterLeft - usableLeft < usableRight - masterRight) {
    mStackRowDir = 1;
    mStackColLeft.push_back(masterLeft);
    mStackColRight.push_back(masterLeft);
    maxWidth = usableRight - masterLeft;
  } else {
    mStackRowDir = -1;
    mStackColLeft.push_back(masterRight);
    mStackColRight.push_back(masterRight);
    maxWidth = masterRight - usableLeft;
  }

  // Get array for rectangles
  stkRects = B3DMALLOC(QRect, mDockedDiaInd.size());
  for (stackInd = 0; stackInd < mDockedDiaInd.size(); stackInd++) {
    dia = DIALOG_LIST_ITEM(mDockedDiaInd[stackInd]);
    if (stkRects) {

      // Get rectangle and make sure it fits inside maximum possible height/width
      stkRects[stackInd] = adjustedGeometry(dia->widget);
      if (stkRects[stackInd].width() > maxWidth || 
          stkRects[stackInd].height() > usableBottom - usableTop) {
        mDockedDiaInd.erase(mDockedDiaInd.begin() + (stackInd--));
        dia->stackIndex = -1;
      }
    } else
      dia->stackIndex = -1;
  }
  if (!stkRects) {
    mDockedDiaInd.clear();
    return;
  }

  // Keep track of free space in column while determining what dialogs fit
  mRestacking = true;
  colInd = 0;
  mColIsSplit.push_back(true);
  colPart = 0;
  freeInCol = masterRect.top() - usableTop;
  firstDiaToPlace = 0;
  mColStartInd.push_back(-1);
  
  // Loop on dialogs in stack order
  for (stackInd = 0; stackInd < mDockedDiaInd.size(); stackInd++) {
    placeColInd = -1;

    // Loop until something fits
    while (stkRects[stackInd].height() >= freeInCol) {

      // Nothing fit in top of split; switch to bottom
      if (mColIsSplit[colInd] && !colPart) {
        //printf("%d not fit in top of %d\n", stackInd, colInd);

        // Go to lower part of a split: set up to place if anything in top 
        // Now we can define freeStart for the placement
        if (placeColInd < 0 && stackInd > firstDiaToPlace) {
          placeColInd = colInd;
          freeStart = usableTop + freeInCol;
          nextDiaToPlace = stackInd;
          //PRINT2(freeStart, stackInd);
        }
        colPart++;
        freeInCol = usableBottom - masterRect.bottom();
        //PRINT1(freeInCol);
      } else {

        // End of column: did anything fit in the column?
        if (mStackColLeft[colInd] == mStackColRight[colInd]) {
          //printf("%d not fit, nothing in %d\n", stackInd, colInd);

          // If nothing fit in column, it had to be a split column, so advance it
          // past the master
          mStackColLeft[colInd] = mStackColRight[colInd] = 
            (mStackRowDir > 0 ? masterRight : masterLeft);
          mColIsSplit[colInd] = false;
          freeInCol = usableBottom - usableTop;
          colPart = 0;
          maxWidth = B3DCHOICE(mStackRowDir > 0, usableRight - masterRight, 
                               masterLeft - usableLeft);
            
        } else {
          //printf("%d not fit, something was in %d\n", stackInd, colInd);

          // If last column had something, start a new column.
          // First set up to place any dialogs in this column
          if (placeColInd < 0 && stackInd > firstDiaToPlace) {
            placeColInd = colInd;
            nextDiaToPlace = stackInd;
            freeStart = B3DCHOICE(mColIsSplit[colInd], masterRect.bottom(), usableTop);
            
            //PRINT4(mColIsSplit[colInd], freeStart, stackInd, placeColInd);
          }

          // Decide whether to split it again if remaining space is big enough
          // or if the next dialog overhangs by acceptable amount
          // If not splitting adjust current column border to include master
          if (mColIsSplit[colInd]) {
            colWidth = mStackColRight[colInd] - mStackColLeft[colInd];
            if (mStackRowDir > 0) {
              isSplit = masterRight - mStackColRight[colInd] > fracRemain * masterWidth ||
                (stackInd > firstDiaToPlace && 
                 (mStackColRight[colInd] + stkRects[stackInd].width() - masterRight < 
                  fracOverhang * masterWidth));
              if (!isSplit)
                ACCUM_MAX(mStackColRight[colInd], masterRight);
            } else {
              isSplit = mStackColLeft[colInd] - masterLeft > fracRemain * masterWidth || 
                (stackInd > firstDiaToPlace && 
                 (masterLeft - (mStackColLeft[colInd] - stkRects[stackInd].width()) < 
                  fracOverhang * masterWidth));
              if (!isSplit)
                ACCUM_MIN(mStackColLeft[colInd], masterLeft);
            }
          } else {
            isSplit = false;
          }

          // Maintain border and allowed width
          if (mStackRowDir > 0) {
            colStart = mStackColRight[colInd];
            maxWidth = usableRight - colStart;
          } else {
            colStart = mStackColLeft[colInd];
            maxWidth = colStart - usableLeft;
          }

          // Now finish starting new column, initializing freeInCol
          mColIsSplit.push_back(isSplit);
          mStackColLeft.push_back(colStart);
          mStackColRight.push_back(colStart);
          mColStartInd.push_back(-1);
          colPart = 0;
          if (isSplit) {
            freeInCol = masterRect.top() - usableTop;
          } else {
            freeInCol = usableBottom - usableTop;
          }
          colInd++;
          //PRINT2(colInd, freeInCol);
        }
      }
    }
    
    // A dialog fit vertically
    // Does it fit horizontally?  If not, kick it out
    if (stkRects[stackInd].width() > maxWidth) {
      dia = DIALOG_LIST_ITEM(mDockedDiaInd[stackInd]);
      mDockedDiaInd.erase(mDockedDiaInd.begin() + (stackInd--));
      dia->stackIndex = -1;
    } else {
    
      //printf("%d fits in %d\n", stackInd, colInd);
      
      // It fits in the next spot
      // Get left coordinate and maintain other side of column 
      if (mStackRowDir > 0) {
        newLeft = mStackColLeft[colInd];
        ACCUM_MAX(mStackColRight[colInd], newLeft + stkRects[stackInd].width());
      } else {
        newLeft = mStackColRight[colInd] - stkRects[stackInd].width();
        ACCUM_MIN(mStackColLeft[colInd], newLeft);
      }
      if (mColStartInd[colInd] < 0)
        mColStartInd[colInd] = stackInd;
      freeInCol -= stkRects[stackInd].height();
      //PRINT2(freeInCol, colInd);
    }
    
    // Now loop on placing a column ready to place, and/or the last column if we have 
    // reached the end, or just go on to next dialog if nothing is ready to place
    for (loop = 0; loop < 2; loop++) {
      if ((!loop && placeColInd < 0) || (loop && stackInd < mDockedDiaInd.size() - 1))
        continue;
      if (loop) {
        placeColInd = colInd;
        nextDiaToPlace = mDockedDiaInd.size();
        if (mColIsSplit[colInd])
          freeStart = B3DCHOICE(colPart > 0, masterRect.bottom(), usableTop + freeInCol);
        else
          freeStart = usableTop;
      }

      //PRINT4(loop, placeColInd, firstDiaToPlace, nextDiaToPlace);
      for (placeInd = firstDiaToPlace; placeInd < nextDiaToPlace; placeInd++) {
        dia = DIALOG_LIST_ITEM(mDockedDiaInd[placeInd]);
        newLeft = B3DCHOICE(mStackRowDir > 0, mStackColLeft[placeColInd],
                            mStackColRight[placeColInd] - stkRects[placeInd].width());

        // Get the top coord and maintain the free space
        // Also set flag if a split column has anything in the current direction
        newTop = freeStart;
        freeStart += stkRects[placeInd].height();

        // Move the dialog if it is not there already, maintain cross-index
        if (newTop != stkRects[placeInd].top() || newLeft != stkRects[placeInd].left() ||
            ImodPrefs->getChangingFrameAdj())
          dia->widget->move(newLeft - ImodPrefs->dlgFrameAdjustment(), newTop);
        dia->stackIndex = placeInd;
        dia->position = QPoint(newLeft, newTop);
      }
      firstDiaToPlace = nextDiaToPlace;
    }
  }
  free(stkRects);
  imod_info_input();
  mRestacking = false;
}

// Find dialog in list by its address if possible
ImodDialog *DialogManager::lookupDialogInList(QWidget *window, int &index)
{
  ImodDialog *dia;
  if (!mDialogList)
    return NULL;
  for (index = 0; index < ilistSize(mDialogList); index++) {
    dia = DIALOG_LIST_ITEM(index);
    if (window == dia->widget)
      return dia;
  }
  return NULL;
}

// Get and save the stack state in settings
void DialogManager::saveStackStateToSettings()
{
  QString keyList, stateList;
  summarizeStackState(keyList, stateList);
  if (mDlgClass == IMOD_DIALOG) {
    ImodPrefs->setImodDlgsInStack(keyList);
    ImodPrefs->setImodDlgStackStates(stateList);
  } else {
    ImodPrefs->setImodvDlgsInStack(keyList);
    ImodPrefs->setImodvDlgStackStates(stateList);
  }
}

// Recreate the stack that was set up when last saved
void DialogManager::restoreStackFromSettings()
{
  QString restoreKeys, restoreStates, curKeys, curStates;
  int restInd, curInd, newListInd;
  char key;
  ImodDialog *dia;
  char passKey[2] = {0x0, 0x0};
  std::vector<int> tempDockedInds;

  // Get stored state
  mRestoringStack = true;
  summarizeStackState(curKeys, curStates);
  if (mDlgClass == IMOD_DIALOG) {
    restoreKeys = ImodPrefs->getImodDlgsInStack();
    restoreStates = ImodPrefs->getImodDlgStackStates();
  } else {
    restoreKeys = ImodPrefs->getImodvDlgsInStack();
    restoreStates = ImodPrefs->getImodvDlgStackStates();
  }
    
  // Loop on the keys
  for (restInd = 0; restInd < restoreKeys.length(); restInd++) {
    key = (char)(restoreKeys[restInd].toLatin1());
    curInd = curKeys.indexOf(key);
    newListInd = ilistSize(mDialogList);
    if (curInd < 0) {

      // If not currently open, set state to put it in and open it with deferred stacking
      mNextDockerState = restoreStates[restInd] == '1' ? 1 : 0;
      passKey[0] = key;
      if (mDlgClass == IMOD_DIALOG) {
        ImodInfoWin->openSelectedWindows(passKey, ImodvClosed ? 0 : 1);
      } else {
        if ((imodvStandalone() && (key == 'I' || key == 'U')) ||
            (!imodvStandalone() && key == 'e'))
          continue;
        imodvOpenSelectedWindows(passKey);
      }
      tempDockedInds.push_back(newListInd);
    } else {
      
      // If it IS open, just add to new list of indexes and restore state
      tempDockedInds.push_back(mDockedDiaInd[curInd]);
      if (restoreStates[restInd] != curStates[curInd]) {
        dia = DIALOG_LIST_ITEM(mDockedDiaInd[curInd]);
        ((DockingDialog *)dia->widget)->setDialogState(restoreStates[restInd] == '1');
      }
    }
  }
  
  // Add any dialogs NOT in the restore list to the end of the new list
  for (curInd = 0; curInd < curKeys.length(); curInd++) {
    if (restoreKeys.indexOf(curKeys[curInd]) < 0)
      tempDockedInds.push_back(mDockedDiaInd[curInd]);
  }

  // Copy the list over and set up to restack
  mDockedDiaInd = tempDockedInds;
  startDockTimer(mDlgClass, 10);
  mNextDockerState = -1;
}

/*
 * Construct two strings that contain the key letter and open/close state of each
 * dialog in the stack
 */
void DialogManager::summarizeStackState(QString &keyList, QString &stateList)
{
  int ind;
  bool state;
  char key;
  ImodDialog *dia;
  for (ind = 0; ind < mDockedDiaInd.size(); ind++) {
    dia = DIALOG_LIST_ITEM(mDockedDiaInd[ind]);
    ((DockingDialog *)dia->widget)->getDialogKeyAndState(key, state);
    keyList += key;
    stateList += state ? '1' : '0';
  }
}

/***************************************************************
 * Ancillary functions unrelated to the control class and structure
 * Could go in utilities
 */

// Adjust size of an arbitrary widget
void adjustGeometryAndShow(QWidget *widget, int dlgClass, bool doSize)
{
  QWidget *parwidg = NULL;
  int deskWidth, deskHeight, xleft, ytop;
  QRect pos, parpos, newpos;

  // Adjust the size and get it on the screen, then show it
  imod_info_input();
  if (doSize)
    widget->adjustSize();
  pos = widget->frameGeometry();
  xleft = pos.x();
  ytop = pos.y();
  //imodPrintStderr("%d %d %d %d\n", xleft, ytop, pos.width(), pos.height());
  diaLimitWindowPos(pos.width(), pos.height(), xleft, ytop, widget);
  if (xleft != pos.x() || ytop != pos.y())
    widget->move(xleft, ytop);
  newpos = pos;
  newpos.moveTo(xleft, ytop);
  //imodPrintStderr("%d %d %d %d\n", xleft, ytop, pos.width(), pos.height());
  widget->show();

  // Get parent if appropriate (Mac OSX) and make sure it doesn't intersect
  // it, but at least restore window manager's position
  if (dlgClass >= 0)
    parwidg = imodDialogManager.parent(dlgClass);
  if (parwidg) {

    // Get the geometry again since Y size was not right for tallest widgets
    // in cocoa Qt 4.5.0
    pos = widget->frameGeometry();
    //imodPrintStderr("%d %d %d %d\n",pos.x(),pos.y(),pos.width(),pos.height());
    parpos = parwidg->frameGeometry();
    /*imodPrintStderr("parent %d %d %d %d\n", parpos.x(), parpos.y(),
      parpos.width(), parpos.height());*/
    // If parent intersects, check if can be moved to below, above, right, left
    if (newpos.intersects(parpos)) {
      diaScreenSize(deskWidth, deskHeight, widget);

      if (pos.height() <= deskHeight - (parpos.y() + parpos.height()))
        ytop = parpos.y() + parpos.height();
      else if (pos.height() < parpos.y() - 32)
        ytop = parpos.y() - pos.height();
      else if (pos.width() <= deskWidth - (parpos.x() + parpos.width()))
        xleft = parpos.x() + parpos.width();
      else if (pos.width() < parpos.x())
        xleft = parpos.x() - pos.width();
    }
    diaLimitWindowPos(pos.width(), pos.height(), xleft, ytop, widget);
    widget->move(xleft, ytop);
    //imodPrintStderr("%d %d\n", xleft, ytop);
  }
}

// Return system-dependent rectangle that can be used to restore window size
// and position with resize and move
// This is a hybrid: the frame position and the client size inside the frame
QRect ivwRestorableGeometry(QWidget *widget)
{
  return QRect(widget->pos(), widget->size());
}

// Return geometry, possibly adjusted for specified error in the frame border size on
// left, right, and bottom, which comes from settings
static QRect adjustedGeometry(QWidget *widget)
{
  int adjust = ImodPrefs->dlgFrameAdjustment();
  QRect rect = widget->frameGeometry();
  if (adjust)
    return QRect(rect.left() + adjust, rect.top(), rect.width() - 2 * adjust,
                 rect.height() - adjust);
  return rect;
}
