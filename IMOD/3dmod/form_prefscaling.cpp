/*
 *  form_prefscaling.cpp - Class for snapshot tab of preferences form
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2019 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 * 
 * $Id$
 */

#include "form_prefscaling.h"

#include <qvariant.h>
#include <qtooltip.h>
#include <stdlib.h>
#include <qimage.h>
#include <qpixmap.h>
#include <QButtonGroup>

#include "dia_qtutils.h"
#include "preferences.h"
#include "info_cb.h"

/*
 *  Constructs a PrefScalingForm as a child of 'parent', with the
 *  name 'name' and widget flags set to 'f'.
 */
PrefScalingForm::PrefScalingForm(QWidget* parent, Qt::WindowFlags fl)
  : QWidget(parent, fl)
{
  setupUi(this);

  init();
}

/*
 *  Destroys the object and frees any allocated resources
 */
PrefScalingForm::~PrefScalingForm()
{
  // no need to delete child widgets, Qt does it all for us
}

/*
 *  Sets the strings of the subwidgets using the current
 *  language.
 */
void PrefScalingForm::languageChange()
{
  retranslateUi(this);
}


void PrefScalingForm::init()
{
  mPrefs = ImodPrefs->getDialogPrefs();
  scanGroup =  new QButtonGroup(this);
  scanGroup->addButton(fullScanButton, 0);
  scanGroup->addButton(subsetMinMaxButton, 1);
  scanGroup->addButton(subsetMeanSDButton, 2);
  connect(scanGroup, SIGNAL(buttonClicked(int)), this, SLOT(scanTypeChanged(int)));
  pieceGroup =  new QButtonGroup(this);
  pieceGroup->addButton(unaliCoordButton, 0);
  pieceGroup->addButton(alignedCoordButton, 1);
  pieceGroup->addButton(alignedVSCoordButton, 2);
  connect(pieceGroup, SIGNAL(buttonClicked(int)), this, SLOT(pieceTypeChanged(int)));
  setFontDependentWidths();
  if (App->cvi->fakeImage || App->cvi->rgbStore) {
    setTargetButton->setEnabled(false);
    autoMeanSpinBox->setEnabled(false);
    autoSDspinBox->setEnabled(false);
  }
  update();
}

void PrefScalingForm::setFontDependentWidths()
{
}

void PrefScalingForm::scanTypeChanged(int value)
{
  mPrefs->scaleScanType = value;
}

void PrefScalingForm::pieceTypeChanged(int value)
{
  mPrefs->useAliPieceCoords = value;
}

// Target Mean or SD for autocontrast has changed: set and display
void PrefScalingForm::autoMeanChanged( int value )
{
  mPrefs->autoTargetMean = value;
  imodInfoAutoContrast(mPrefs->autoTargetMean,  mPrefs->autoTargetSD);
}

void PrefScalingForm::autoSDChanged( int value )
{
  mPrefs->autoTargetSD = value;
  imodInfoAutoContrast(mPrefs->autoTargetMean,  mPrefs->autoTargetSD);
}

// Get the current mean and Sd and use to set the targets
void PrefScalingForm::setTargetClicked()
{
  float imageMean, imageSD, scaleLo, scaleHi;
  int targetMean, targetSD;
  int range = App->cvi->white - App->cvi->black;
  
  // Get current mean and SD, compute the target from current B/W settings
  if (imodInfoCurrentMeanSD(imageMean, imageSD, scaleLo, scaleHi))
    return;
  if (range <= 0)
    range = 1;
  targetMean = (int)(255. * (imageMean - App->cvi->black) / range + 0.5);
  targetSD = (int)(255. * imageSD / range - 0.5);
  
  // Set the spin boxes, then unload values back to preferences
  diaSetSpinBox(autoMeanSpinBox, targetMean);
  diaSetSpinBox(autoSDspinBox, targetSD);
  mPrefs->autoTargetMean = autoMeanSpinBox->value();
  mPrefs->autoTargetSD = autoSDspinBox->value();
}

// Update all widgets with the current structure values
void PrefScalingForm::update()
{
  diaSetChecked(loadUshortBox, mPrefs->loadUshorts);
  diaSetChecked(loadIntIfMeanSDBox, mPrefs->loadIntIfMeanSD);
  diaSetChecked(loadIntIfEstimateBox, mPrefs->loadIntIfEstimate);
  diaSetSpinBox(superResSpinBox, mPrefs->eerSuperRes);
  diaSetSpinBox(eerZbinSpinBox, mPrefs->eerZbinning);
  diaSetChecked(preferMeanSDBox, mPrefs->preferMeanSD);
  diaSetChecked(changeMRCstatsBox, mPrefs->changeMRCstats);
  diaSetSpinBox(truncateAtSpinBox, mPrefs->numSDsForScaling);
  diaSetGroup(scanGroup, mPrefs->scaleScanType);
  diaSetGroup(pieceGroup, mPrefs->useAliPieceCoords);
  diaSetSpinBox(autoMeanSpinBox, mPrefs->autoTargetMean);
  diaSetSpinBox(autoSDspinBox, mPrefs->autoTargetSD);
  autoConAtStartBox->setCurrentIndex(mPrefs->autoConAtStart);
}

// Get state of widgets other than zoom-related and put in structure
void PrefScalingForm::unload()
{
  mPrefs->loadUshorts = loadUshortBox->isChecked();
  mPrefs->loadIntIfMeanSD = loadIntIfMeanSDBox->isChecked();
  mPrefs->loadIntIfEstimate = loadIntIfEstimateBox->isChecked();
  mPrefs->eerSuperRes = superResSpinBox->value();
  mPrefs->eerZbinning = eerZbinSpinBox->value();
  mPrefs->preferMeanSD = preferMeanSDBox->isChecked();
  mPrefs->changeMRCstats = changeMRCstatsBox->isChecked();
  mPrefs->numSDsForScaling = truncateAtSpinBox->value();
  mPrefs->autoConAtStart = autoConAtStartBox->currentIndex();
}

