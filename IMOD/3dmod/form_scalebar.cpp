/*
 *  form_scalebar.cpp - Class for scale bar dialog form
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2019 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 * 
 * $Id$
 */

#include "form_scalebar.h"
#include "dockingdialog.h"

#include <qvariant.h>
#include <qimage.h>
#include <qpixmap.h>
#include <QTimerEvent>
#include <QKeyEvent>
#include <QCloseEvent>

#include "scalebar.h"
#include "imodv.h"
#include "mv_input.h"
#include "dia_qtutils.h"
#include "control.h"
#include "imod.h"
#include "object_edit.h"

/*
 *  Constructs a ScaleBarForm as a child of 'parent', with the
 *  name 'name' and widget flags set to 'f'.
 */
ScaleBarForm::ScaleBarForm(DockingDialog* parent, Qt::WindowFlags fl)
  : QWidget(parent, fl)
{
  mTopWin = parent;
  setupUi(this);

  init();
}

/*
 *  Destroys the object and frees any allocated resources
 */
ScaleBarForm::~ScaleBarForm()
{
  // no need to delete child widgets, Qt does it all for us
}

/*
 *  Sets the strings of the subwidgets using the current
 *  language.
 */
void ScaleBarForm::languageChange()
{
  retranslateUi(this);
}

void ScaleBarForm::init()
{
  const char *units = imodUnits(imodvStandalone() ? Imodv->imod : App->cvi->imod);
  setAttribute(Qt::WA_DeleteOnClose);
  setAttribute(Qt::WA_AlwaysShowToolTips);
  
  mTimerID = 0;
  mParams = scaleBarGetParams();
  diaSetChecked(drawCheckBox, mParams->draw);
  diaSetChecked(whiteCheckBox, mParams->white); 
  diaSetChecked(verticalCheckBox, mParams->vertical); 
  diaSetChecked(colorCheckBox, mParams->colorRamp);
  diaSetChecked(invertCheckBox, mParams->invertRamp);
  diaSetSpinBox(lengthSpinBox, mParams->minLength);
  diaSetSpinBox(thicknessSpinBox, mParams->thickness);
  diaSetSpinBox(indentXspinBox, mParams->indentX);
  diaSetSpinBox(indentYspinBox, mParams->indentY);
  positionComboBox->setCurrentIndex(mParams->position);
  diaSetChecked(customCheckBox, mParams->useCustom);
  diaSetChecked(makeExactlyBox, mParams->useExact);
  diaSetSpinBox(customSpinBox, mParams->customVal);
  diaSetChecked(drawLabelsBox, mParams->drawLabels);
  diaSetSpinBox(labelSizeSpinBox, mParams->labelSize);
  diaSetSpinBox(labelOffsetSpinBox, mParams->labelYoffset);
  if (imodvStandalone()) {
    zapLabel->hide();
    zapValue->hide();
    slicerLabel->hide();
    slicerValue->hide();
    multiZLabel->hide();
    multiZValue->hide();
    xyzLabel->hide();
    xyzValue->hide();
  }
  customSpinBox->setEnabled(mParams->useCustom && !mParams->useExact);
  customCheckBox->setEnabled(!mParams->useExact);
  exactValEdit->setEnabled(mParams->useExact);
  multUnitLabel->setText(QString(units));
  exactUnitLabel->setText(QString(units));
  setExactValText();
  labelSizeSpinBox->setEnabled(mParams->drawLabels);
  labelOffsetSpinBox->setEnabled(mParams->drawLabels);
  connect(makeExactlyBox, SIGNAL(toggled(bool)), this, SLOT(exactToggled(bool)));
  connect(exactValEdit, SIGNAL(editingFinished()), this, SLOT(exactValChanged()));
  connect(drawLabelsBox, SIGNAL(toggled(bool)), this, SLOT(drawLabelsToggled(bool)));
  connect(labelSizeSpinBox, SIGNAL(valueChanged(int)), this, SLOT(labelSizeChanged(int)));
  connect(labelOffsetSpinBox, SIGNAL(valueChanged(int)), this,
          SLOT(labelOffsetChanged(int)));
  connect(mTopWin, SIGNAL(dockerCloseEvent(QCloseEvent *)), this,
          SLOT(topCloseEvent(QCloseEvent *)));
  connect(mTopWin, SIGNAL(dockerChangeEvent(QEvent *)), this,
          SLOT(topChangeEvent(QEvent *)));
}

// Respond to the changes: put value in structure and redraw
void ScaleBarForm::drawToggled( bool state )
{
  mParams->draw = state;
  scaleBarRedraw();
}

void ScaleBarForm::whiteToggled( bool state )
{
  mParams->white = state;
  scaleBarRedraw();
}

void ScaleBarForm::verticalToggled( bool state )
{
  mParams->vertical = state;
  scaleBarRedraw();
}

void ScaleBarForm::colorToggled( bool state )
{
  mParams->colorRamp = state;
  scaleBarRedraw();
}

void ScaleBarForm::invertToggled( bool state )
{
  mParams->invertRamp = state;
  scaleBarRedraw();
}

void ScaleBarForm::lengthChanged( int value )
{
  mParams->minLength = value;
  scaleBarRedraw();
}

void ScaleBarForm::thicknessChanged( int value )
{
  mParams->thickness = value;
  scaleBarRedraw();
}

void ScaleBarForm::positionChanged( int value )
{
  mParams->position = value;
  scaleBarRedraw();
}

void ScaleBarForm::indentXchanged( int value )
{
  mParams->indentX = value;
  scaleBarRedraw();
}

void ScaleBarForm::indentYchanged( int value )
{
  mParams->indentY = value;
  scaleBarRedraw();
}

void ScaleBarForm::customToggled( bool state )
{
  mParams->useCustom = state;
  customSpinBox->setEnabled(state);
  scaleBarRedraw();
}

void ScaleBarForm::customValChanged( int value )
{
  mParams->customVal = value;
  scaleBarRedraw();
}

void ScaleBarForm::exactToggled( bool state )
{
  mParams->useExact = state;
  exactValEdit->setEnabled(state);
  customCheckBox->setEnabled(!state);
  customSpinBox->setEnabled(!state && mParams->useCustom);
  if (state && mParams->exactVal <= 0) {
    mParams->exactVal = mParams->lastLength;
    setExactValText();
  }
  scaleBarRedraw();
}
  
void ScaleBarForm::setExactValText()
{
  QString str;
  str.sprintf("%g", mParams->exactVal);
  diaSetEditText(exactValEdit, str);
}

void ScaleBarForm::exactValChanged()
{
  QString str = exactValEdit->text();
  float value = atof(LATIN1(str));
  if  (value < 0.00001)
    value = 0.00001;
  mParams->exactVal = value;
  setExactValText();
  setFocus();
  scaleBarRedraw();
}

void ScaleBarForm::drawLabelsToggled(bool state)
{
  mParams->drawLabels = state;
  labelSizeSpinBox->setEnabled(mParams->drawLabels);
  labelOffsetSpinBox->setEnabled(mParams->drawLabels);
  scaleBarRedraw();
}

void ScaleBarForm::labelSizeChanged(int value)
{
  if (value < MIN_LABEL_SIZE)
    value = 0;
  mParams->labelSize = value;
  if (mParams->drawLabels)
    scaleBarRedraw();
}

void ScaleBarForm::labelOffsetChanged(int value)
{
  mParams->labelYoffset = value;
  if (mParams->drawLabels)
    scaleBarRedraw();
}

// Update the value labels if the values are positive
void ScaleBarForm::updateValues( float zapv, float multiZv, float slicerv, 
                                 float xyzv, float modvv, char *units )
{
  QString str;
  if (mTimerID)
    killTimer(mTimerID);
  mTimerID = 0;
  if (!imodvStandalone()) {
    if (zapv > 0)
      str.sprintf("%g %s", zapv, units);
    else
      str = "";
    zapValue->setText(str);
    if (multiZv > 0)
      str.sprintf("%g %s", multiZv, units);
    else
      str = "";
    multiZValue->setText(str);
    if (slicerv > 0)
      str.sprintf("%g %s", slicerv, units);
    else
      str = "";
    slicerValue->setText(str);
    if (xyzv > 0)
      str.sprintf("%g %s", xyzv, units);
    else
      str = "";
    xyzValue->setText(str);
  }
  if (modvv > 0)
    str.sprintf("%g %s", modvv, units);
  else
    str = "";
  modvValue->setText(str);
}

// Start a timer to do an update after window draws a bar
void ScaleBarForm::startUpdateTimer()
{
  if (mTimerID)
    killTimer(mTimerID);
  mTimerID = startTimer(100);
}

// If the timer fires before an update, do an update
void ScaleBarForm::timerEvent( QTimerEvent *e )
{
  mTimerID = 0;
  scaleBarUpdate();
}

void ScaleBarForm::keyPressEvent( QKeyEvent * e )
{
  if (utilCloseKey(e))
    mTopWin->close();
  else if (imodvStandalone())
    imodvKeyPress(e);
  else
    ivwControlKey(0, e);
}

void ScaleBarForm::keyReleaseEvent( QKeyEvent * e )
{
  if (imodvStandalone())
    imodvKeyRelease(e);
  else
    ivwControlKey(1, e);
}

void ScaleBarForm::topCloseEvent( QCloseEvent * e )
{
  scaleBarClosing();
  e->accept();
}

void ScaleBarForm::topChangeEvent(QEvent *e)
{
  QWidget::changeEvent(e);
  ivwCheckAndSetMacMenu(e, this, imodvStandalone() ? IMODV_DIALOG : IMOD_DIALOG);
  if (e->type() != QEvent::FontChange)
    return;
}
