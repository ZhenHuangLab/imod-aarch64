/*
 *  mv_listobj.cpp - Object list dialog with controls for grouping objects
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2019 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#include <qscrollarea.h>
#include <qframe.h>
#include <qapplication.h>
#include <qcheckbox.h>
#include <qsignalmapper.h>
#include <qpushbutton.h>
#include <qlabel.h>
#include <qtooltip.h>
#include <qlayout.h>
#include <qspinbox.h>
#include <qlineedit.h>
#include <qgroupbox.h>
//Added by qt3to4:
#include <QHBoxLayout>
#include <QCloseEvent>
#include <QGridLayout>
#include <QKeyEvent>
#include <QVBoxLayout>
#include <QDesktopWidget>
#include "dia_qtutils.h"
#include "objgroup.h"

#include "imodv.h"
#include "mv_gfx.h"
#include "imod.h"
#include "info_cb.h"
#include "control.h"
#include "preferences.h"
#include "mv_objed.h"
#include "mv_listobj.h"
#include "dockingdialog.h"
#include "mv_input.h"

// The pointer to the instance
static ImodvOlist *sOolist_dialog = NULL;
static DockingDialog *sTopWin = NULL;

#define MAX_OOLIST_BUTTONS  5000
#define MAX_OOLIST_WIDTH 384
#define MAX_LIST_IN_COL 36
#define MAX_LIST_NAME 40

enum {OBJGRP_NEW = 0, OBJGRP_DELETE, OBJGRP_CLEAR, OBJGRP_ADDALL, OBJGRP_SWAP,
      OBJGRP_TURNON, OBJGRP_TURNOFF, OBJGRP_OTHERSON, OBJGRP_OTHERSOFF};

// The button arrays, and variable to keep track of grouping
static QCheckBox **sOolistButtons;
static QCheckBox **sGroupButtons;
static int sNumOolistButtons = 0;
static bool sGrouping = false;

/*
 * Create the object list dialog
 */ 
void imodvObjectListDialog(ImodvApp *a, int state)
{
  int m;

  if (!state) {
    if (sOolist_dialog)
      sTopWin->close();
    return;
  }
  if (sOolist_dialog) {
    sTopWin->raise();
    return;
  }
  sGrouping = false;

  // Get number of buttons, number of columns and number per column
  // Make maximum number of buttons needed for all loaded models
  for (m = 0; m < a->numMods; m++)
    if (sNumOolistButtons < a->mod[m]->objsize) 
      sNumOolistButtons = a->mod[m]->objsize; 
  if (sNumOolistButtons > MAX_OOLIST_BUTTONS)
    sNumOolistButtons = MAX_OOLIST_BUTTONS;

  sOolistButtons = (QCheckBox **)malloc(sNumOolistButtons * sizeof(QCheckBox *));
  sGroupButtons = (QCheckBox **)malloc(sNumOolistButtons * sizeof(QCheckBox *));
  if (!sOolistButtons || !sGroupButtons) {
    if (sOolistButtons)
      free(sOolistButtons);
    if (sGroupButtons)
      free(sGroupButtons);
    sNumOolistButtons = 0;
    wprint("\aMemory error getting array for checkboxes\n");
    return;
  }
       
  sTopWin = new DockingDialog(imodvDialogManager.parent(IMODV_DIALOG), 
                              &imodvDialogManager, "Model View Object List", 
                              "objectList.html#TOP", 'L', Qt::Window);
  sOolist_dialog = new ImodvOlist(sTopWin, 0);
  sTopWin->addWidgetToStack(sOolist_dialog);
  setModvDialogTitle(sTopWin, "MV Obj List");

  imodvOlistUpdateOnOffs(a);

  // Get sizes to adjust window size with
  QSize svSize = sOolist_dialog->mScroll->sizeHint();
  QSize frameSize = sOolist_dialog->mFrame->sizeHint();
  imod_info_input();
  sTopWin->adjustSize();

  // 4 pixels added was enough to prevent scroll bars
  // If width is constrained, allow more height for horizontal scroll bar
  int newWidth = sTopWin->width() + frameSize.width() - svSize.width() +
    8;
  int newHeight = sTopWin->height() + frameSize.height() - svSize.height() + 8;
  if (newWidth > MAX_OOLIST_WIDTH) {
    newWidth = MAX_OOLIST_WIDTH;
    newHeight += 20;
  }
  if (newHeight > QApplication::desktop()->height() - 100)
    newHeight = QApplication::desktop()->height() - 100;
  sTopWin->resize(newWidth, newHeight);

  adjustGeometryAndShow((QWidget *)sTopWin, IMODV_DIALOG);

  imodvDialogManager.add((QWidget *)sTopWin, IMODV_DIALOG, DOCKING_DIALOG_TYPE);

  // After getting size with group buttons present, maybe hide them
  sOolist_dialog->updateGroups(a);
  sOolist_dialog->adjustFrameSize();
}

// Set On/Off state for one object
void imodvOlistSetChecked(ImodvApp *a, int ob, bool state)
{
  if (!sOolist_dialog)
    return;
  if (ob < sNumOolistButtons && ob < a->imod->objsize)
    diaSetChecked(sOolistButtons[ob], state);
}

// Set color for one object
void imodvOlistSetColor(ImodvApp *a, int ob)
{
  Iobj *obj;
  if (!sOolist_dialog || ob >= sNumOolistButtons || ob >= a->imod->objsize)
    return;
  obj = &a->imod->obj[ob];
  diaSetWidgetColor
    (sOolistButtons[ob], QColor((int)(255 * obj->red), (int)(255 * obj->green),
            (int)(255 * obj->blue)));
}

// Update the group control buttons and buttons for objects
void imodvOlistUpdateGroups(ImodvApp *a)
{
  if (!sOolist_dialog || !sNumOolistButtons)
    return;
  sOolist_dialog->updateGroups(a);
}

void imodvOlistUpdateOnOffs(ImodvApp *a)
{
  int ob;
  bool state, nameChgd = false;
  QString qstr;
  char obname[MAX_LIST_NAME];
  int len;
  QColor bkgColor;
  QColor gray;
  if (!sOolist_dialog || !sNumOolistButtons)
    return;
  QPalette palette = sOolist_dialog->palette();
  gray = palette.color(sOolist_dialog->backgroundRole());

  // Add objects and see what happens
  if (sNumOolistButtons < a->imod->objsize) {
    B3DREALLOC(sOolistButtons, QCheckBox *, a->imod->objsize);
    B3DREALLOC(sGroupButtons, QCheckBox *, a->imod->objsize);
    if (!sOolistButtons || !sGroupButtons) {
      sTopWin->close();
      return;
    }
    if (a->imod->objsize <= MAX_LIST_IN_COL)
      sOolist_dialog->mNumPerCol = a->imod->objsize;
    for (ob = sNumOolistButtons; ob < a->imod->objsize; ob++)
      sOolist_dialog->addOneObject(ob);
    sNumOolistButtons = a->imod->objsize;
    sOolist_dialog->adjustFrameSize();
  }

  // Hiding seems to help the updates some, especially if there are text 
  // changes or massive turning off of buttons, but processing events after
  // hiding doesn't help the time on Linux and leaves it gray (3/26/09)
  if (sNumOolistButtons > 256)
    sOolist_dialog->mFrame->hide();

  for (ob = 0; ob < sNumOolistButtons; ob++) {
    if (ob < a->imod->objsize) {
      // Get a truncated name
      // DMN 9/20/04: just truncate all columns a little bit now
      len = strlen(a->imod->obj[ob].name);
      if (len > MAX_LIST_NAME - 1)
        len = MAX_LIST_NAME - 1;
      strncpy(obname, a->imod->obj[ob].name, len);
      obname[len] = 0x00;
      qstr.sprintf("%d: %s",ob + 1, obname);
      state = !(a->imod->obj[ob].flags & IMOD_OBJFLAG_OFF);
      bkgColor.setRgb((int)(255. * a->imod->obj[ob].red),
                      (int)(255. * a->imod->obj[ob].green),
                      (int)(255. * a->imod->obj[ob].blue));
      if (sOolistButtons[ob]->isHidden()) {
        sOolistButtons[ob]->show();
        nameChgd = true;
      }
      if (qstr != sOolistButtons[ob]->text()) {
        sOolistButtons[ob]->setText(qstr);
        nameChgd = true;
      }
      diaSetWidgetColor(sOolistButtons[ob], bkgColor);
      diaSetChecked(sOolistButtons[ob], state);
    } else if (!sOolistButtons[ob]->isHidden()) {
      sOolistButtons[ob]->hide();
      nameChgd = true;
    }
  }
  if (sNumOolistButtons > 256)
    sOolist_dialog->mFrame->show();
  if (nameChgd) 
    sOolist_dialog->adjustFrameSize();
}

/*
 * Return true if an object should be edited as part of the group
 */
bool imodvOlistObjInGroup(ImodvApp *a, int ob)
{
  if (!sOolist_dialog || !sNumOolistButtons || !sGrouping || 
      ob >= sNumOolistButtons) 
    return false;
  return sGroupButtons[ob]->isChecked();
}

// Simply return flag for whether sGrouping is on
bool imodvOlistGrouping(void)
{
  return sGrouping;
}

/*
 * Object list class constructor
 */
ImodvOlist::ImodvOlist(DockingDialog *parent, Qt::WindowFlags fl)
  : QWidget(parent, fl)
{
  int nPerCol, olistNcol, ob, i;
  QString qstr;
  QLabel *label;
  const char *labels[] = {"New", "Delete", "Clear", "Add All", "Swap", "ON", "OFF",
                    "On", "Off"};
  const char *tips[] = {"Start a new object group, copied from current group",
                  "Remove the current object group from list of groups",
                  "Remove all objects from the current group",
                  "Add all objects to the current group",
                  "Remove current members and add all non-members",
                  "Turn ON all objects in the current group",
                  "Turn OFF all objects in the current group",
                  "Turn ON objects NOT in the current group",
                  "Turn OFF objects NOT in the current group"};
  
  setAttribute(Qt::WA_DeleteOnClose);
  setAttribute(Qt::WA_AlwaysShowToolTips);
  QVBoxLayout *layout = new QVBoxLayout(this);
  layout->setContentsMargins(5, 5, 5, 5);
  layout->setSpacing(6);

  QGroupBox *grpbox = new QGroupBox("Object Group Selection");
  layout->addWidget(grpbox);
  QVBoxLayout *gblay = new QVBoxLayout(grpbox);
  gblay->setContentsMargins(6, 3, 6, 6);
  gblay->setSpacing(6);
  QHBoxLayout *hbox = diaHBoxLayout(gblay); 
  
  mGroupSpin = (QSpinBox *)diaLabeledSpin(0, 0., 0., 1., "Group", grpbox,hbox);
  mGroupSpin->setSpecialValueText("None");
  connect(mGroupSpin, SIGNAL(valueChanged(int)), this, 
          SLOT(curGroupChanged(int)));
  mGroupSpin->setToolTip("Select the current object group or turn off "
                "selection");

  mNumberLabel = diaLabel("/99", grpbox, hbox);
  mNameEdit = new QLineEdit(grpbox);
  hbox->addWidget(mNameEdit);
  mNameEdit->setMaxLength(OBJGRP_STRSIZE - 1);
  mNameEdit->setFocusPolicy(Qt::ClickFocus);
  connect(mNameEdit, SIGNAL(returnPressed()), this,
          SLOT(returnPressed()));
  connect(mNameEdit, SIGNAL(textChanged(const QString&)), this,
          SLOT(nameChanged(const QString&)));
  mNameEdit->setToolTip("Enter a name for the current group");

  QSignalMapper *clickMapper = new QSignalMapper(this);
  connect(clickMapper, SIGNAL(mapped(int)), this,
          SLOT(actionButtonClicked(int)));

  // Make the buttons and put in hbox
  for (i = 0; i < OBJLIST_NUMBUTTONS; i++) {

    if (i == 0 || i == 5)
      hbox = diaHBoxLayout(gblay);
    if (i == 7) {
      label = diaLabel("Others:", grpbox, hbox);
      label->setAlignment(Qt::AlignRight | Qt::AlignVCenter);
    }

    qstr = labels[i];
    mButtons[i] = diaPushButton(LATIN1(qstr), grpbox, hbox);
    clickMapper->setMapping(mButtons[i], i);
    connect(mButtons[i], SIGNAL(clicked()), clickMapper, SLOT(map()));
    mButtons[i]->setToolTip(tips[i]);
  }

  mScroll = new QScrollArea(this);
  layout->addWidget(mScroll);
  mFrame = new QFrame();
  mScroll->setAutoFillBackground(true);
  QPalette palette = mFrame->palette();
  QColor bkg = palette.color(mFrame->backgroundRole());
  diaSetWidgetColor(mScroll->viewport(), bkg);
  mGrid = new QGridLayout(mFrame);
  mGrid->setSpacing(2);
  olistNcol = (sNumOolistButtons + MAX_LIST_IN_COL - 1) / MAX_LIST_IN_COL;
  mNumPerCol = (sNumOolistButtons + olistNcol - 1) / olistNcol;

  // Get a signal mapper, connect to the slot for these buttons
  mMapper = new QSignalMapper(this);
  connect(mMapper, SIGNAL(mapped(int)), this, SLOT(toggleListSlot(int)));
  mGmapper = new QSignalMapper(this);
  connect(mGmapper, SIGNAL(mapped(int)), this, SLOT(toggleGroupSlot(int)));
  
  // Make the buttons, set properties and map them
  for (ob = 0; ob < sNumOolistButtons; ob++) {
    addOneObject(ob);

    // Hide the buttons later after window size is set
    sGrouping = true;
  }
  mScroll->setWidget(mFrame);
  connect(sTopWin, SIGNAL(dockerCloseEvent(QCloseEvent *)), this,
          SLOT(topCloseEvent(QCloseEvent *)));
  connect(sTopWin, SIGNAL(dockerChangeEvent(QEvent *)), this,
          SLOT(topChangeEvent(QEvent *)));

  setFontDependentWidths();
}

void ImodvOlist::addOneObject(int ob)
{
  QString qstr;
  QHBoxLayout *hLayout = new QHBoxLayout();
  mGrid->addLayout(hLayout, ob % mNumPerCol, ob / mNumPerCol);
  sGroupButtons[ob] = diaCheckBox("", mFrame, hLayout);
  qstr.sprintf("%d: ",ob + 1);
  sOolistButtons[ob] = diaCheckBox(LATIN1(qstr), mFrame, hLayout);
  sOolistButtons[ob]->setAutoFillBackground(true);
  mMapper->setMapping(sOolistButtons[ob], ob);
  connect(sOolistButtons[ob], SIGNAL(toggled(bool)), mMapper, SLOT(map()));
  mGmapper->setMapping(sGroupButtons[ob], ob);
  connect(sGroupButtons[ob], SIGNAL(toggled(bool)), mGmapper, SLOT(map()));
  hLayout->setStretchFactor(sOolistButtons[ob], 100);
}

void ImodvOlist::toggleGroupSlot(int ob)
{
  int index;
  bool state;
  IobjGroup *group = (IobjGroup *)ilistItem(Imodv->imod->groupList, 
                                            Imodv->imod->curObjGroup);
  if (!group)
    return;
  index = objGroupLookup(group, ob);
  state = sGroupButtons[ob]->isChecked();
  if (state && index < 0)
    objGroupAppend(group, ob);
  else if (!state && index >= 0)
    ilistRemove(group->objList, index);
}
 
void ImodvOlist::toggleListSlot(int ob)
{
  objedToggleObj(ob, sOolistButtons[ob]->isChecked());
}

void ImodvOlist::actionButtonClicked(int which)
{
  Imod *imod = Imodv->imod;
  IobjGroup *group, *ogroup;
  int ob, index, changed = 1;

  if (which != OBJGRP_NEW && which != OBJGRP_DELETE) {
    group = (IobjGroup *)ilistItem(imod->groupList, imod->curObjGroup);
    if (!group)
      return;
  }

  switch (which) {
  case OBJGRP_NEW:
    imodvRegisterModelChg();
    group = objGroupListExpand(&imod->groupList);
    if (!group)
      break;
    ogroup = (IobjGroup *)ilistItem(imod->groupList, imod->curObjGroup);
    if (ogroup)
      group->objList = ilistDup(ogroup->objList);
    imod->curObjGroup = ilistSize(imod->groupList) - 1;
    updateGroups(Imodv);
    break;

  case OBJGRP_DELETE:
    if (imod->curObjGroup < 0)
      return;
    imodvRegisterModelChg();
    if (objGroupListRemove(imod->groupList, imod->curObjGroup))
      break;
    imod->curObjGroup = B3DMIN(B3DMAX(0, imod->curObjGroup - 1),
                               ilistSize(imod->groupList) - 1);
    updateGroups(Imodv);
    break;

  case OBJGRP_CLEAR:
    imodvRegisterModelChg();
    ilistTruncate(group->objList, 0);
    updateGroups(Imodv);
    break;

  case OBJGRP_ADDALL:
    imodvRegisterModelChg();
    if (group->objList)
      ilistTruncate(group->objList, 0);
    for (ob = 0; ob < imod->objsize; ob++)
      if (objGroupAppend(group, ob))
        return;
    updateGroups(Imodv);
    break;

  case OBJGRP_SWAP:
    imodvRegisterModelChg();
    for (ob = 0; ob < imod->objsize; ob++) {
      index = objGroupLookup(group, ob);
      if (index >= 0)
        ilistRemove(group->objList, index);
      else
        if (objGroupAppend(group, ob))
          return;
    }
    updateGroups(Imodv);
    break;

  case OBJGRP_TURNON:
  case OBJGRP_TURNOFF:
  case OBJGRP_OTHERSON:
  case OBJGRP_OTHERSOFF:
    changed = 0;
    for (ob = 0; ob < imod->objsize; ob++) {
      index = objGroupLookup(group, ob);
      if (((index >= 0 && which == OBJGRP_TURNON) || 
           (index < 0 && which == OBJGRP_OTHERSON)) && 
          iobjOff(imod->obj[ob].flags)) {
        imodvRegisterObjectChg(ob);
        imod->obj[ob].flags &= ~IMOD_OBJFLAG_OFF;
        changed = 1;
      } else if (((index >= 0 && which == OBJGRP_TURNOFF) ||
                  (index < 0 && which == OBJGRP_OTHERSOFF)) &&
                 !iobjOff(imod->obj[ob].flags)) {
        imodvRegisterObjectChg(ob);
        imod->obj[ob].flags |= IMOD_OBJFLAG_OFF;
        changed = 1;
      }
    }
    imodvDraw(Imodv);
    imodvDrawImodImages();
    imodvObjedNewView();
    break;

  }
  if (changed)
    imodvFinishChgUnit();
}

void ImodvOlist::nameChanged(const QString &str)
{
  IobjGroup *group = (IobjGroup *)ilistItem(Imodv->imod->groupList, 
                                            Imodv->imod->curObjGroup);
  if (!group)
    return;
  strncpy(&group->name[0], LATIN1(str), OBJGRP_STRSIZE);
  group->name[OBJGRP_STRSIZE - 1] = 0x00;
}

void ImodvOlist::curGroupChanged(int value)
{
  setFocus();
  Imodv->imod->curObjGroup = value - 1;
  updateGroups(Imodv);
}

void ImodvOlist::returnPressed()
{
  setFocus();
}

void ImodvOlist::updateGroups(ImodvApp *a)
{
  IobjGroup *group;
  int i, ob, numGroups = ilistSize(a->imod->groupList);
  int curGrp = a->imod->curObjGroup;
  int *objs;
  bool *states;
  bool showChgd = false;
  QString str;
  diaSetSpinMMVal(mGroupSpin, 0, numGroups, curGrp + 1);
  str.sprintf("/%d", numGroups);
  mNumberLabel->setText(str);
  mNameEdit->setEnabled(curGrp >= 0);
  for (i = 1; i < OBJLIST_NUMBUTTONS; i++)
    mButtons[i]->setEnabled(curGrp >= 0);
  if (curGrp < 0) {
    if (sGrouping) {
      mFrame->hide();
      for (ob = 0; ob < sNumOolistButtons; ob++)
        sGroupButtons[ob]->hide();
      mFrame->show();
      adjustFrameSize();
    }
    sGrouping = false;
    return;
  }
  if (!sGrouping) {
    mFrame->hide();
    for (ob = 0; ob < sNumOolistButtons; ob++)
      sGroupButtons[ob]->show();
    mFrame->show();
    adjustFrameSize();
  }
  sGrouping = true;

  group = (IobjGroup *)ilistItem(a->imod->groupList, curGrp);
  if (!group)
    return;
  mNameEdit->setText(group->name);
  states = (bool *)malloc(sizeof(bool) * sNumOolistButtons);
  if (!states)
    return;
  for (ob = 0; ob < sNumOolistButtons; ob++)
    states[ob] = false;
  objs = (int *)ilistFirst(group->objList);
  if (objs) {
    for (i = 0; i < ilistSize(group->objList); i++)
      if (objs[i] >= 0 && objs[i] < sNumOolistButtons)
        states[objs[i]] = true;
  }
  for (ob = 0; ob < sNumOolistButtons; ob++) {
    if (ob < a->imod->objsize) {
      diaSetChecked(sGroupButtons[ob], states[ob]);
      if (sGroupButtons[ob]->isHidden()) {
        sGroupButtons[ob]->show();
        showChgd = true;
      }
    } else if (!sGroupButtons[ob]->isHidden()) {
      sGroupButtons[ob]->hide();
      showChgd = true;
    }
  }
  free(states);
  if (showChgd)
    adjustFrameSize();
}

void ImodvOlist::adjustFrameSize()
{
  imod_info_input();
  mFrame->adjustSize();
}

void ImodvOlist::setFontDependentWidths()
{
  int i, width, minWidth;
  bool rounded = ImodPrefs->getRoundedStyle();
  minWidth = diaGetButtonWidth(this, rounded, 1.3, mButtons[0]->text());
  for (i = 0; i < OBJLIST_NUMBUTTONS; i++) {
    width = diaGetButtonWidth(this, rounded, 1.3, mButtons[i]->text());
    width = B3DMAX(minWidth, width);
    mButtons[i]->setFixedWidth(width);
  }
}

void ImodvOlist::topChangeEvent(QEvent *e)
{
  QWidget::changeEvent(e);
  ivwCheckAndSetMacMenu(e, this, IMODV_DIALOG);
  if (e->type() != QEvent::FontChange)
    return;
  setFontDependentWidths();
  adjustFrameSize();
}

void ImodvOlist::topCloseEvent ( QCloseEvent * e )
{
  imodvDialogManager.remove((QWidget *)sTopWin);
  sOolist_dialog  = NULL;
  sTopWin = NULL;
  sNumOolistButtons = 0;
  free(sOolistButtons);
  free(sGroupButtons);
  sGrouping = false;
  e->accept();
}

void ImodvOlist::keyPressEvent ( QKeyEvent * e )
{
  if (utilCloseKey(e))
    sTopWin->close();
  else
    imodvKeyPress(e);
}

void ImodvOlist::keyReleaseEvent ( QKeyEvent * e )
{
  imodvKeyRelease(e);
}
