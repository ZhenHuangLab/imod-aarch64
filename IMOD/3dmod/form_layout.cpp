/*
 *  form_layout.cpp - Class for Windows tab of preferences form
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2016 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 * 
 * $Id$
 */

#include "form_layout.h"

#include <stdlib.h>

#include "dia_qtutils.h"
#include "preferences.h"

/*
 *  Constructs a LayoutForm as a child of 'parent', with the
 *  name 'name' and widget flags set to 'f'.
 */
LayoutForm::LayoutForm(QWidget* parent, Qt::WindowFlags fl)
  : QWidget(parent, fl)
{
  setupUi(this);

  init();
}

/*
 *  Destroys the object and frees any allocated resources
 */
LayoutForm::~LayoutForm()
{
  // no need to delete child widgets, Qt does it all for us
}

/*
 *  Sets the strings of the subwidgets using the current
 *  language.
 */
void LayoutForm::languageChange()
{
  retranslateUi(this);
}


void LayoutForm::init()
{
  mPrefs = ImodPrefs->getDialogPrefs();
  connect(frameAdjSpinBox, SIGNAL(valueChanged(int)), this, SLOT(borderAdjChanged(int)));
  update();
}

void LayoutForm::borderAdjChanged(int value)
{
  mPrefs->dlgFrameAdjustment = value;
  ImodPrefs->dlgFrameAdjChanged();
}

// Update all widgets with the current structure values
void LayoutForm::update()
{
  diaSetChecked(geomCheckBox, mPrefs->rememberGeom);
  diaSetChecked(imageIconifyBox, mPrefs->iconifyImageWin);
  diaSetChecked(imodDlgIconifyBox, mPrefs->iconifyImodDlg);
  diaSetChecked(imodvDlgIconifyBox, mPrefs->iconifyImodvDlg);
  diaSetChecked(dockImodDlgsBox, mPrefs->stackImodDlgs);
  diaSetChecked(raiseImodStackBox, mPrefs->raiseImodDlgStack);
  diaSetChecked(keepStackOnTopBox, mPrefs->keepDlgStackOnTop);
  diaSetChecked(dockImodvDlgsBox, mPrefs->stackImodvDlgs);
  diaSetChecked(raiseImodvStackBox, mPrefs->raiseImodvDlgStack);
  diaSetSpinBox(frameAdjSpinBox, mPrefs->dlgFrameAdjustment);
}

// Get state of widgets other than zoom-related and put in structure
void LayoutForm::unload()
{
  mPrefs->rememberGeom = geomCheckBox->isChecked();
  mPrefs->iconifyImageWin = imageIconifyBox->isChecked();
  mPrefs->iconifyImodDlg = imodDlgIconifyBox->isChecked();
  mPrefs->iconifyImodvDlg = imodvDlgIconifyBox->isChecked();
  mPrefs->stackImodDlgs = dockImodDlgsBox->isChecked();
  mPrefs->raiseImodDlgStack = raiseImodStackBox->isChecked();
  mPrefs->keepDlgStackOnTop = keepStackOnTopBox->isChecked();
  mPrefs->stackImodvDlgs = dockImodvDlgsBox->isChecked();
  mPrefs->raiseImodvDlgStack = raiseImodvStackBox->isChecked();
  mPrefs->dlgFrameAdjustment = frameAdjSpinBox->value();
}
