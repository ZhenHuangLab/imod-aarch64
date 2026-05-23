/*
* angledialog.cpp - callbacks for the angle range dialog.
*
*  Author: Quanren Xiong and David Mastronarde
*
*  Copyright (C) 2008-2018 by the Regents of the University of 
*  Colorado.  See dist/COPYRIGHT for full copyright notice.
* 
*  $Id$
*/

#include "myapp.h"
#include "angledialog.h"
#include "plotter.h"
#include "b3dutil.h"
#include "ilist.h"
#include "parse_params.h" //for exitError()
#include "dia_qtutils.h"

AngleDialog::AngleDialog(MyApp *app, QWidget *parent): QDialog(parent)
{
  int width, i, numSkip;
  QString str;
  mParamsOpen = true;
  mSettingFocusForCurVals = false;
  mApp = app;
  setWindowTitle(tr("Angle Range & Tile Selection"));

  // Expected defocus
  mDefocusLabel = new QLabel(tr("E&xpected defocus (um): "), this);
  mDefocusEdit = new QLineEdit("6.0", this);

  // Buddy means the accelerator key for the label sets focus to the text box
  // This is meaningless if there is no unique & included in the label!
  mDefocusLabel->setBuddy(mDefocusEdit);
  mDefocusEdit->setFixedWidth(fontMetrics().width("    99.99"));
  mDefocusEdit->setToolTip("Nominal defocus value in microns");

  // Angle range
  mMidAngleLabel = new QLabel(tr("&Middle tilt angle: "), this);
  mMidAngleEdit = new QLineEdit(tr("-90.0"), this);
  mMidAngleLabel->setBuddy(mMidAngleEdit);
  mMidAngleEdit->setToolTip("Middle of tilt angle range to include in fit");

  QHBoxLayout *viewRangeHLay = new QHBoxLayout;
  mNumViewsSpin = (QSpinBox *)diaLabeledSpin
    (0, 1, mApp->getNzz(), 1, tr("Number of views to fit:"), this,
     viewRangeHLay, &mNumViewsLabel, 1.6);
  diaSetSpinBox(mNumViewsSpin, mApp->getNumViewsInRange());
  mNumViewsSpin->setToolTip("Number of consecutive views to combine for fitting");
  connect(mNumViewsSpin, SIGNAL(valueChanged(int)), this, 
        SLOT(viewRangeChanged(int)));

  mAngleRangeLabel = new QLabel("", this);
  mAngleRangeLabel->setTextFormat(Qt::RichText);

  // Steps
  QHBoxLayout *rangeStepHLay = new QHBoxLayout;
  mRangeStepSpin = (QSpinBox *)diaLabeledSpin
    (0, 1, mApp->getNzz(), 1, tr("Step view range by:  "), this,
     rangeStepHLay, &mRangeStepLabel, 1.6);
  mRangeStepSpin->setToolTip("Number of views by which to change the subset of views"
                             " being fit\n with the Step buttons, or when using"
                             " \"Autofit all Steps\"");
  diaSetSpinBox(mRangeStepSpin, mApp->getViewRangeStep());
  connect(mRangeStepSpin, SIGNAL(valueChanged(int)), this, 
        SLOT(rangeStepChanged(int)));

  mStepUpButton = new QPushButton( tr("Step &Up"), this);
  mStepUpButton->setToolTip("Increase starting and ending views by the step");
 
  mStepDownButton = new QPushButton( tr("Step &Down"), this);
  mStepDownButton->setToolTip("Decrease starting and ending views by the step");
  width = (int)(1.35 * mStepDownButton->fontMetrics().width("Step Down") + 0.5);
  mStepDownButton->setFixedWidth(width);
  mStepUpButton->setFixedWidth(width);
 
  // Autofitting
  QHBoxLayout *autofitHLay = new QHBoxLayout;
  mAutofitButton = diaPushButton(mApp->getFitSingleViews() ? 
                                 (tr("Autofit A&ll Views")).toLatin1() : 
                                 (tr("Autofit A&ll Steps")).toLatin1(), this, 
                                 autofitHLay);
  mAutofitButton->setEnabled(false);
  mAutofitButton->setToolTip("Fit to all tilt angle ranges that fit within limits below");
  mAutoStopButton = diaPushButton((tr("STOP")).toLatin1() , this, autofitHLay);
  mAutoStopButton->setEnabled(false);
  mAutoStopButton->setToolTip("Stop autofitting before it finishes");

  mAutoFromLabel = new QLabel(tr("Autof&it: "), this);
  mAutoFromEdit = new QLineEdit(tr("90.0"), this);
  mAutoFromLabel->setBuddy(mAutoFromEdit);
  mAutoFromEdit->setToolTip("Starting angle of range to cover with autofitting to "
                            "stepped ranges");
  width = mAutoFromEdit->fontMetrics().width("   -99.99");
  mAutoFromEdit->setFixedWidth(width);

  mAutoToLabel = new QLabel(tr("t&o"), this);
  mAutoToEdit = new QLineEdit(tr("90.0"), this);
  mAutoToEdit->setFixedWidth(width);
  mAutoToLabel->setBuddy(mAutoToEdit);
  mAutoToEdit->setToolTip("Ending angle of range to cover with autofitting to "
                            "stepped ranges");

  mFitSingleBox = new QCheckBox(tr("Fit each view separately"), this);
  mFitSingleBox->setToolTip("Fit every view in the range separately");
  mFitSingleBox->setChecked(mApp->getFitSingleViews());

  QFrame *line = diaFrameLine(this, NULL);
  QFrame *line2 = diaFrameLine(this, NULL);
  QFrame *line3 = diaFrameLine(this, NULL);
  QFrame *line4 = diaFrameLine(this, NULL);

  // Open/close button
  mTileParamButton = mApp->diaPlusMinusButton(false, this, NULL);
  QLabel *tileParamLabel = new QLabel(tr("Tile & wedge parameters"), this);

  mDefTolLabel = new QLabel(tr("Center defocus tol (nm)"), this);
  mDefTolEdit = new QLineEdit(tr("200"), this);
  mDefTolLabel->setBuddy(mDefTolEdit);
  mDefTolEdit->setMinimumWidth(fontMetrics().width(" 9999"));
  mDefTolEdit->setToolTip("Maximum defocus difference for central tiles");

  mLeftTolLabel = new QLabel(tr("Left defocus tol (nm)"), this);
  mLeftTolEdit = new QLineEdit(tr("200"), this);
  mLeftTolLabel->setBuddy(mLeftTolEdit);
  mLeftTolEdit->setToolTip("Maximum defocus difference for non-central tiles on left");

  mRightTolLabel = new QLabel(tr("Right defocus tol (nm)"), this);
  mRightTolEdit = new QLineEdit(tr("200"), this);
  mRightTolLabel->setBuddy(mRightTolEdit);
  mRightTolEdit->setToolTip("Maximum defocus difference for non-central tiles on right");

  mTileSizeLabel = new QLabel(tr("Tile size to analyze: "), this);
  mTileSizeEdit = new QLineEdit(tr("256"),this);
  mTileSizeLabel->setBuddy(mTileSizeEdit);
  mTileSizeEdit->setToolTip("Size of square, overlapping tiles to analyze, in pixel");

  mAxisAngleLabel = new QLabel(tr("Tilt axis angle:   "), this);
  mAxisAngleEdit = new QLineEdit( tr("0.0"), this);
  mAxisAngleLabel->setBuddy(mAxisAngleEdit);
  mAxisAngleEdit->setToolTip("Angle of tilt axis from vertical");

  mDefocusGroup = new QGroupBox(tr("Which defocus to use"), this);
  QButtonGroup *butGroup = new QButtonGroup(this);
  mExpDefocusRadio = new QRadioButton(tr("Expected defocus"));
  mExpDefocusRadio->setToolTip("Use expected defocus for shifting spectra "
                              "from off-center tiles");
  mCurrDefocusRadio = new QRadioButton(tr("Current defocus estimate"));
  mCurrDefocusRadio->setToolTip("Use current defocus estimate for shifting "
                               "spectra from off-center tiles");
  if (mApp->getUseCurDefocus())
    mCurrDefocusRadio->setChecked(true);
  else
    mExpDefocusRadio->setChecked(true);
  butGroup->addButton(mExpDefocusRadio);
  butGroup->addButton(mCurrDefocusRadio);

  QVBoxLayout *vbox = new QVBoxLayout;
  vbox->addWidget(mExpDefocusRadio);
  vbox->addWidget(mCurrDefocusRadio);
  mDefocusGroup->setLayout(vbox);

  // This is what I usually do for more compact group boxes
  vbox->setSpacing(0);
  vbox->setContentsMargins(5, 2, 5, 5);

  mInitCentralCheckBox = diaCheckBox(tr("Initially use only central tiles"), this,
                                     NULL);
  mInitCentralCheckBox->setChecked(mApp->getInitialCentralTiles());
  mInitCentralCheckBox->setToolTip("Include only tiles near tilt axis (within defocus "
                                   "tolerance)");
  connect(mInitCentralCheckBox, SIGNAL(toggled(bool)), this, 
          SLOT(initCentralTilesToggled(bool)));

  // Skip list entries
  QHBoxLayout *skipHlayout = new QHBoxLayout;
  diaLabel(tr("Views to skip:"), this, skipHlayout);
  mLastSkipString = "";
  int *skipList = mApp->getViewSkipList(numSkip);
  if (skipList && numSkip) {
    char *skipChars = listToString(skipList, numSkip);
    if (skipChars) {
      mLastSkipString = skipChars;
      free(skipChars);
    }
  }
  mSkipListEdit = new QLineEdit(mLastSkipString, this);
  skipHlayout->addWidget(mSkipListEdit);
  mSkipListEdit->setToolTip("List of views to skip (numbered from 1)");
  
  mSkipForAstigCheckBox = diaCheckBox(tr("Skip/break only for astig/phase"), this, NULL);
  mSkipForAstigCheckBox->setChecked(mApp->getSkipOnlyForAstig());
  mSkipForAstigCheckBox->setToolTip("Skip views and break at the one view only for "
                                    "finding astigmatism and phase");
  connect(mSkipForAstigCheckBox, SIGNAL(toggled(bool)), this,
          SLOT(skipForAstigToggled(bool)));

  // Break at view entries
  QHBoxLayout *breakHlayout = new QHBoxLayout;
  mBreakAtAngleCheckBox = diaCheckBox(tr("Break groups at view:"), this, breakHlayout);
  mBreakAtAngleCheckBox->setChecked(mApp->getBreakAtBidirView());
  mBreakAtAngleCheckBox->setToolTip("Do not combine data for analysis from this view "
                                   "and the next one");
  connect(mBreakAtAngleCheckBox, SIGNAL(toggled(bool)), this,
          SLOT(breakAtViewToggled(bool)));

  numSkip = mApp->getBidirectionalView();
  str = "";
  if (numSkip > 0 && numSkip < mApp->getNzz())
    str.sprintf("%d", numSkip);
  mBreakViewEdit = new QLineEdit(str, this);
  mBreakViewEdit->setFixedWidth(fontMetrics().width("999999"));
  mBreakViewEdit->setToolTip("View at which to break grouping for analysis, typically "
                              "view at starting angle of bidirectional tilt series");
  breakHlayout->addWidget(mBreakViewEdit);

  // Expected phase
  QHBoxLayout *phaseLayout = new QHBoxLayout;
  diaLabel(tr("Expected phase shift:"), this, phaseLayout);
  str.sprintf("%.1f", mApp->mDefocusFinder.getPlatePhase() / RADIANS_PER_DEGREE);
  mExpectedPhaseEdit = new QLineEdit(str, this);
  mExpectedPhaseEdit->setFixedWidth(fontMetrics().width("-199.999"));
  phaseLayout->addWidget(mExpectedPhaseEdit);
  mExpectedPhaseEdit->setToolTip("Phase shift from phase plate in degrees");

  QHBoxLayout *cutonLayout = new QHBoxLayout;
  diaLabel(tr("Cut-on frequency (1/nm):"), this, cutonLayout);
  str.sprintf("%.3f", mApp->mDefocusFinder.getCutOnFreq());
  mCutOnFreqEdit = new QLineEdit(str, this);
  mCutOnFreqEdit->setFixedWidth(fontMetrics().width("0.99999"));
  cutonLayout->addWidget(mCutOnFreqEdit);
  mCutOnFreqEdit->setToolTip("Fixed cut-on frequency for phase shift in 1/nm");

  mUseCurPhaseCheckBox = diaCheckBox(tr("Use current phase estimate"), this, NULL);
  mUseCurPhaseCheckBox->setChecked(mApp->getUseCurrentPhase());
  connect(mUseCurPhaseCheckBox, SIGNAL(toggled(bool)), this,
          SLOT(useCurrentPhaseToggled(bool)));
  mUseCurPhaseCheckBox->setToolTip("Use the current estimate of phase and cut-on "
                                   "frequency (if any) instead of expected values");
  
  // Buttons
  QHBoxLayout *applyStoreLayout = new QHBoxLayout;
  mApplyButton = diaPushButton( tr("&Apply"), this, applyStoreLayout);
  mApplyButton->setDefault(true);
  mApplyButton->setEnabled(false);
  mApplyButton->setToolTip("Compute power spectra with current settings");
  width = (int)(2.3 * mStepDownButton->fontMetrics().width("Apply") + 0.5);
  mApplyButton->setFixedWidth(width);

  mSaveButton = diaPushButton( tr("&Store in Table"), this, applyStoreLayout);
  mSaveButton->setToolTip("Add current defocus and other solved values to table below");
  width = (int)(1.35 * mStepDownButton->fontMetrics().width("Store in Table") + 0.5);
  mSaveButton->setFixedWidth(width);

  mTable = new QTableWidget(0, 4, this);
  mTable->setSelectionBehavior(QAbstractItemView::SelectRows);
  mTable->setSelectionMode(QAbstractItemView::SingleSelection);
  mTable->setEditTriggers(QAbstractItemView::NoEditTriggers);
  QStringList headers;
  headers << "Middle" << "Start" << "End" << "Defocus" << "Astig" << "Axis" << "Phase"
          << "Cuton";
  mTable->setHorizontalHeaderLabels(headers);
  width = (int)(1.66 * mTable->fontMetrics().width("Defocus"));
  for (i = 0; i < 4; i++)
    mTable->setColumnWidth(i, width);
  
  QHBoxLayout *tabHLayout = new QHBoxLayout;
  mDeleteButton = diaPushButton(tr("Delete Row"), this, tabHLayout);
  mReturnButton = diaPushButton(tr("Set &Tilt Angles"), this, tabHLayout);
  mGraphButton = diaPushButton(tr("Graph Values"), this, tabHLayout);
  mToFileButton = diaPushButton(tr("Sa&ve to File"), this, tabHLayout);
  mDeleteButton->setToolTip("Remove this row from the table");
  mReturnButton->setToolTip("Set starting and ending angles from this row and "
                            "recompute spectrum");
  mGraphButton->setToolTip("Use Tomodataplots to graph table values versus tilt angle");
  mToFileButton->setToolTip("Save angle and defocus values in table to file");


  QHBoxLayout *bottomHLayout = new QHBoxLayout;
  mCloseButton = diaPushButton( (tr("&Close")).toLatin1(), this, bottomHLayout);
  mCloseButton->setFixedWidth(3 * fontMetrics().width("Close"));
  mCloseButton->setToolTip("Close this dialog");

  mStepUpButton->setFocusPolicy(Qt::NoFocus);
  mStepDownButton->setFocusPolicy(Qt::NoFocus);
  mFitSingleBox->setFocusPolicy(Qt::NoFocus);

  connect(mDefocusEdit, SIGNAL(textChanged(const QString &)), this,
      SLOT(enableApplyButton(const QString &)) );
  connect(mMidAngleEdit, SIGNAL(textChanged(const QString &)), this,
      SLOT(enableApplyButton(const QString &)) );
  connect(mDefTolEdit, SIGNAL(textChanged(const QString &)), this,
      SLOT(enableApplyButton(const QString &)) );
  connect(mTileSizeEdit, SIGNAL(textChanged(const QString &)), this,
      SLOT(enableApplyButton(const QString &)) );
  connect(mAxisAngleEdit, SIGNAL(textChanged(const QString &)), this,
      SLOT(enableApplyButton(const QString &)) );
  connect(mLeftTolEdit, SIGNAL(textChanged(const QString &)), this,
      SLOT(enableApplyButton(const QString &)) );
  connect(mRightTolEdit, SIGNAL(textChanged(const QString &)), this,
      SLOT(enableApplyButton(const QString &)) );

  // If you connect editingFinished too, it happens whenever dialog loses focus too

  connect(mExpDefocusRadio, SIGNAL(clicked()), this, SLOT(expDefocusChecked()));
  connect(mCurrDefocusRadio, SIGNAL(clicked()),this, SLOT(currDefocusChecked()));
  
  connect(mTileParamButton, SIGNAL(clicked()), this, SLOT(tileParamsClicked()) );
  connect(mApplyButton, SIGNAL(clicked()), this, SLOT(applyClicked()) );
  connect(mStepUpButton, SIGNAL(clicked()), this, SLOT(stepUpClicked()) );
  connect(mAutofitButton, SIGNAL(clicked()), this, SLOT(autofitClicked()) );
  connect(mAutoStopButton, SIGNAL(clicked()), this, SLOT(autoStopClicked()) );
  connect(mStepDownButton, SIGNAL(clicked()), this, SLOT(stepDownClicked()) );
  connect(mFitSingleBox, SIGNAL(toggled(bool)), this, SLOT(fitSingleToggled(bool)));
  connect(mCloseButton, SIGNAL(clicked()), this, SLOT(close()) );
  connect(mSaveButton, SIGNAL(clicked()), mApp, SLOT(saveCurrentDefocus()) );
  connect(mDeleteButton, SIGNAL(clicked()), this, SLOT(deleteClicked()) );
  connect(mReturnButton, SIGNAL(clicked()), this, SLOT(setAnglesClicked()) );
  connect(mToFileButton, SIGNAL(clicked()), mApp, SLOT(writeDefocusFile()) );
  connect(mGraphButton, SIGNAL(clicked()), mApp, SLOT(graphTableValues()) );
  connect(mTable, SIGNAL(cellDoubleClicked(int, int)), this, 
          SLOT(rowDoubleClicked(int, int)));

  QHBoxLayout *defHLayout = new QHBoxLayout;
  defHLayout->addWidget(mDefocusLabel);
  defHLayout->addWidget(mDefocusEdit);
  
  QHBoxLayout *defTolHLayout = new QHBoxLayout;
  defTolHLayout->addWidget(mDefTolLabel);
  defTolHLayout->addWidget(mDefTolEdit);
  
  QHBoxLayout *leftTolHLayout = new QHBoxLayout;
  leftTolHLayout->addWidget(mLeftTolLabel);
  leftTolHLayout->addWidget(mLeftTolEdit);
  
  QHBoxLayout *rightTolHLayout = new QHBoxLayout;
  rightTolHLayout->addWidget(mRightTolLabel);
  rightTolHLayout->addWidget(mRightTolEdit);

  QHBoxLayout *midAngleHLayout = new QHBoxLayout;
  midAngleHLayout->addWidget(mMidAngleLabel);
  midAngleHLayout->addWidget(mMidAngleEdit);

  QHBoxLayout *stepButHLayout = new QHBoxLayout;
  stepButHLayout->addWidget(mStepUpButton);
  stepButHLayout->addWidget(mStepDownButton);

  QHBoxLayout *autoRangeHLayout = new QHBoxLayout;
  autoRangeHLayout->addWidget(mAutoFromLabel);
  autoRangeHLayout->addWidget(mAutoFromEdit);
  autoRangeHLayout->addWidget(mAutoToLabel);
  autoRangeHLayout->addWidget(mAutoToEdit);

  QHBoxLayout *tileParamHLayout = new QHBoxLayout;
  tileParamHLayout->addWidget(mTileParamButton);
  tileParamHLayout->addWidget(tileParamLabel);

  QHBoxLayout *tileSizeHLayout = new QHBoxLayout;
  tileSizeHLayout->addWidget(mTileSizeLabel);
  tileSizeHLayout->addWidget(mTileSizeEdit);

  QHBoxLayout *axisAngleHLayout = new QHBoxLayout;
  axisAngleHLayout->addWidget(mAxisAngleLabel);
  axisAngleHLayout->addWidget(mAxisAngleEdit);


  QVBoxLayout *leftVLayout = new QVBoxLayout;
  leftVLayout->addLayout(midAngleHLayout);
  leftVLayout->addLayout(viewRangeHLay);
  leftVLayout->addWidget(mAngleRangeLabel);
  leftVLayout->addWidget(line);
  leftVLayout->addLayout(rangeStepHLay);
  leftVLayout->addLayout(stepButHLayout);
  leftVLayout->addWidget(line2);
  leftVLayout->addLayout(autofitHLay);
  leftVLayout->addLayout(autoRangeHLayout);
  leftVLayout->addWidget(mFitSingleBox);
  leftVLayout->addWidget(line3);
  leftVLayout->addLayout(applyStoreLayout);
  leftVLayout->addLayout(tileParamHLayout);
  leftVLayout->addLayout(defTolHLayout);
  leftVLayout->addLayout(leftTolHLayout);
  leftVLayout->addLayout(rightTolHLayout);

  QVBoxLayout *rightVLayout = new QVBoxLayout;
  rightVLayout->addLayout(defHLayout);
  rightVLayout->addWidget(mDefocusGroup);
  rightVLayout->addWidget(mInitCentralCheckBox);
  rightVLayout->addLayout(phaseLayout);
  rightVLayout->addLayout(cutonLayout);
  rightVLayout->addWidget(mUseCurPhaseCheckBox);
  rightVLayout->addWidget(line4);
  rightVLayout->addLayout(skipHlayout);
  rightVLayout->addLayout(breakHlayout);
  rightVLayout->addWidget(mSkipForAstigCheckBox);
  rightVLayout->addStretch();
  rightVLayout->addLayout(tileSizeHLayout);
  rightVLayout->addLayout(axisAngleHLayout);

  QHBoxLayout *wedgeStepHLayout = new QHBoxLayout;
  float angleInc = mApp->getSectorWidth();
  float maxAngle = B3DNINT(30. / angleInc) * angleInc;
  mWedgeIntervalSpin = (QDoubleSpinBox *)diaLabeledSpin
    (1, angleInc, maxAngle, angleInc, "Wedge interval:", this, wedgeStepHLayout,
     &mWedgeIntervalLabel, 1.6);
  diaSetDoubleSpinBox(mWedgeIntervalSpin, mApp->getWedgeInterval());
  mWedgeIntervalSpin->setToolTip("Angular interval between the wedges to analyze");
  connect(mWedgeIntervalSpin, SIGNAL(valueChanged(double)), this, 
        SLOT(wedgeIntervalChanged(double)));
  rightVLayout->addLayout(wedgeStepHLayout);

  QHBoxLayout *cropHLayout = new QHBoxLayout;
  mCropSpecCheckBox = diaCheckBox("Analyze spectra cropped to pixel size (nm):", this,
                                  cropHLayout);
  mCropSpecCheckBox->setToolTip("Analyze data at the given pixel size (larger than "
                                "actual size) by extracting larger tiles\nand cropping "
                                "their spectra to the regular tile size");
  diaSetChecked(mCropSpecCheckBox, mApp->getCropSpectra());
  str.sprintf("%g", mApp->getCropPixelSizeInDia());
  mCropPixelEdit = new QLineEdit(str, this);
  cropHLayout->addWidget(mCropPixelEdit);
  mCropPixelEdit->setToolTip("Enter pixel size to crop to; must be larger than actual "
                             "size to have any effect");
  connect(mCropSpecCheckBox, SIGNAL(toggled(bool)), this, SLOT(cropSpectraToggled(bool)));

  QHBoxLayout *topHLayout = new QHBoxLayout;
  topHLayout->addLayout(leftVLayout);
  topHLayout->addLayout(rightVLayout);

  QVBoxLayout *mainVLayout = new QVBoxLayout(this);
  mainVLayout->addLayout(topHLayout);
  mainVLayout->addLayout(cropHLayout);
  mCropWarningLabel = diaLabel("", this, mainVLayout);
  mainVLayout->addWidget(mTable);
  mainVLayout->setStretchFactor(mTable, 100);
  mainVLayout->addLayout(tabHLayout);
  mainVLayout->addLayout(bottomHLayout);
  adjustSize();
}

/* 
 * Show or hide the tile parameters and resize the window
 */
void AngleDialog::tileParamsClicked()
{
  mParamsOpen = !mParamsOpen;
  mTileParamButton->setText(mParamsOpen ? "-" : "+");
  mTileParamButton->setToolTip
    (QString(mParamsOpen ? "Hide" : "Show") + 
     QString(" tile size and cropping, tolerance, tilt axis angle, and wedge interval "
             "entries"));

  diaShowWidget(mDefTolLabel, mParamsOpen);
  diaShowWidget(mLeftTolLabel, mParamsOpen);
  diaShowWidget(mRightTolLabel, mParamsOpen);
  diaShowWidget(mTileSizeLabel, mParamsOpen);
  diaShowWidget(mAxisAngleLabel, mParamsOpen);
  diaShowWidget(mDefTolEdit, mParamsOpen);
  diaShowWidget(mLeftTolEdit, mParamsOpen);
  diaShowWidget(mRightTolEdit, mParamsOpen);
  diaShowWidget(mTileSizeEdit, mParamsOpen);
  diaShowWidget(mAxisAngleEdit, mParamsOpen);
  diaShowWidget(mWedgeIntervalSpin, mParamsOpen);
  diaShowWidget(mWedgeIntervalLabel, mParamsOpen);

  QApplication::processEvents();
  adjustDialogSize();
}

// Resize the dialog to the right width and  height determined by number of table entries
void AngleDialog::adjustDialogSize()
{
  QSize hint = sizeHint();
  QSize tabhint = mTable->sizeHint();
  int curWidth = width();

  // This number controls the number of lines shown in the table initially and whenever 
  //this section is toggled
  int height = tabhint.height() - 11.4 * mTable->fontMetrics().height();
  height = hint.height() - B3DMAX(0, height);
  resize(mParamsOpen ? curWidth : (int)(0.7 * hint.width()), height);

  // Qt 5 in linux really needed this done twice for 100% reliability
  qApp->processEvents();
  resize(mParamsOpen ? curWidth : (int)(0.7 * hint.width()), height);
}

// setFocus is needed to get the changes in spin boxes processed, but set a flag
// so computations are not done
// This needs to be called by widgets with NoFocus that then start a computation
void AngleDialog::setFocusToGetCurrentsVals()
{
  mSettingFocusForCurVals = true;
  setFocus();
  qApp->processEvents();
  mSettingFocusForCurVals = false;
}

// Clears the warning label for cropping or sets it with given text and chosen color
void AngleDialog::setCropWarningLabel(QString str, int showColor)
{
  mCropWarningLabel->setText(showColor ? str : "");
  if (showColor) {
    QPalette palette = mCropWarningLabel->palette();
    palette.setColor(mCropWarningLabel->foregroundRole(), showColor > 2 ? Qt::red :
                     (showColor > 1 ? Qt::darkMagenta : Qt::darkGreen));
    mCropWarningLabel->setPalette(palette);
  }
}

void AngleDialog::viewRangeChanged(int value)
{
  mApp->setNumViewsInRange(value);
  if (!mSettingFocusForCurVals)
    anglesSet(0);
}

void AngleDialog::rangeStepChanged(int value)
{
  mApp->setViewRangeStep(value);
}

// This one does not have NoFocus policy
void AngleDialog::applyClicked()
{
  setFocusToGetCurrentsVals();
  anglesSet(0);
}

void AngleDialog::stepUpClicked()
{
  setFocusToGetCurrentsVals();
  anglesSet(1);
}

void AngleDialog::stepDownClicked()
{
  setFocusToGetCurrentsVals();
  anglesSet(-1);
}

void AngleDialog::fitSingleToggled(bool state)
{
  mApp->setFitSingleViews(state);
  mAutofitButton->setText(state ? tr("Autofit A&ll Views") :
                         tr("Autofit A&ll Steps"));
}

void AngleDialog::cropSpectraToggled(bool state)
{
  mApp->setCropSpectra(state);
  setFocusToGetCurrentsVals();
  anglesSet(0);
}

void AngleDialog::skipForAstigToggled(bool state)
{
  mApp->setSkipOnlyForAstig(state);
  setFocusToGetCurrentsVals();
  anglesSet(0);
}

void AngleDialog::breakAtViewToggled(bool state)
{
  if (state && getAndCheckBreakViewEntry()) {
    diaSetChecked(mBreakAtAngleCheckBox, false);
    return;
  }
  mApp->setBreakAtBidirView(state);
  setFocusToGetCurrentsVals();
  anglesSet(0);
}

int AngleDialog::getAndCheckBreakViewEntry()
{
  QString str = mBreakViewEdit->text();
  bool valOK;
  int view = str.toInt(&valOK);
  int nzz = mApp->getNzz();
  if (!valOK || view < 2 || view > nzz - 1) {
    if (str.isEmpty() || valOK) {
      str.sprintf("Enter an view between 2 and %d in the \"Break groups at view\" "
                  "text box", nzz - 1);
      QMessageBox::critical(NULL, "Ctfplotter: Incorrect entry", str);
    } else 
      mApp->badEntryMessage(valOK, "Break groups at view");
    return 1;
  }
  mApp->setBidirectionalView(view);
  return 0;
}

void AngleDialog::useCurrentPhaseToggled(bool state)
{  
  mApp->setUseCurrentPhase(state);
  setFocusToGetCurrentsVals();
  anglesSet(0);
}


/*
 * This is called after Apply or general Enter on text fields, and by the step buttons
 * It fetches the angles and other parameters from the dialog and adjusts angles as needed
 */
void AngleDialog::anglesSet(int step)
{
  bool defOk, skipOk, phaseOk, cutonOk = true, breakOk = true;
  double defocus = mDefocusEdit->text().toDouble(&defOk);
  double phaseShift = mExpectedPhaseEdit->text().toDouble(&phaseOk) * RADIANS_PER_DEGREE;
  double cutonFreq = mCutOnFreqEdit->text().toDouble(&cutonOk);
  double lowAngle, highAngle;
  double defTol, axisAngle, leftTol, rightTol, cropPixel;
  int tSize, rangeStep;
  QString str;
  
  bool anglesOk = getAnglesAndStep(step, lowAngle, highAngle, rangeStep, skipOk);

  bool tolOk = getTileTolerances(defTol, tSize, axisAngle, leftTol, rightTol, cropPixel);
  if (mApp->doingMultipleFits())
    return;
  if (phaseOk) {
    if (phaseShift > MY_PI || phaseShift < -MY_PI) {
      while (phaseShift > MY_PI)
        phaseShift -= MY_PI;
      while (phaseShift < -MY_PI)
        phaseShift += MY_PI;
      str.sprintf("%.1f", phaseShift / RADIANS_PER_DEGREE);
      mExpectedPhaseEdit->setText(str);
    }
    mApp->mDefocusFinder.setPlatePhase(phaseShift);
  }
  if (cutonOk)
    mApp->mDefocusFinder.setCutOnFreq(cutonFreq);
  if (mApp->getBreakAtBidirView())
    breakOk = getAndCheckBreakViewEntry() == 0;
  

  printf("lowAngle=%7.2f, highAngle=%7.2f\n", lowAngle, highAngle);
  fflush(stdout);
  if( defOk && anglesOk && tolOk && skipOk && phaseOk && breakOk && cutonOk)

    // Plotter::openAngleDia connects this to MyApp::angleChanged
    emit angle(lowAngle, highAngle, defocus, 
               defTol, tSize, axisAngle, leftTol, rightTol, cropPixel);
  else {
    mApp->badEntryMessage(defOk, "defocus");
    mApp->badEntryMessage(phaseOk, "expected phase shift");
    mApp->badEntryMessage(anglesOk, "middle angle");
    mApp->badEntryMessage(cutonOk, "cut-on frequency");
    mApp->badEntryMessage
      (tolOk, QString("There is a non-numeric character in one of the\n"
                      "tolerance entries or in the tilt axis angle entry.")
       + QString(mParamsOpen ? "" : "\n\nOpen Tile & wedge Parameters to see these "
                 "entries"), false);
    if (!skipOk)
      QMessageBox::critical(NULL, "Ctfplotter: Bad skip list entry", "Either there is a "
                            "non-numeric character in the "
                            "list of views to skip, or the list excludes all the views");
  }
}

/*
 * Get the middle angle from text box and compute  starting
 * and ending angles; step them both by doStep if nonzero (should be +1 or -1), return
 * false if angle has bad characters
 */
bool AngleDialog::getAnglesAndStep(int doStep, double &lowAngle, double &highAngle,
                                   int &rangeStep, bool &skipOK)
{
  int ind, midInd = -1, lowInd, highInd, numViews, numSorted, numList, numOld;
  int lowSortInd, highSortInd;
  QString str;
  bool angleOK;
  float *sortedAngles;;
  float *viewAngles = mApp->getTiltAngles();
  float diff, minDiff;
  int nzz = mApp->getNzz();
  int *skipList, *oldSkip;
  double midAngle = mMidAngleEdit->text().toDouble(&angleOK);
  double midInput = midAngle;
  setFocusToGetCurrentsVals();
  rangeStep = mApp->getViewRangeStep();
  numViews = mApp->getNumViewsInRange();

  str = mSkipListEdit->text();
  skipOK = str == mLastSkipString;
  if (!skipOK) {
    skipList = parselist((const char *)(str.toLatin1()), &numList);
    skipOK = skipList || !numList;
    if (skipOK) {
      oldSkip = mApp->getViewSkipList(numOld);
      mApp->setViewSkipList(skipList, numList);
      mApp->getSortedAngles(1, &numSorted);
      if (!numSorted) {
        skipOK = false;
        mApp->setViewSkipList(oldSkip, numOld);
        mSkipListEdit->setText(mLastSkipString);
        free(skipList);
      } else {
        mLastSkipString = str;
        free(oldSkip);
      }
    }
  }

  if (angleOK && skipOK) {
    mApp->getRangesFromMidAngle(numViews, doStep * rangeStep, midAngle, lowAngle, 
                                highAngle, lowSortInd, highSortInd, lowInd, highInd);
    str.sprintf("%6.2f", midAngle);
    if (fabs(midAngle - midInput) > 0.001)
      mMidAngleEdit->setText(str);

    if (lowInd == highInd) {
      str.sprintf("%.2f&deg;, view %d", lowAngle, lowInd + 1);
    } else {
      str.sprintf("%.2f&deg; to %.2f&deg;, views %d-%d", lowAngle, highAngle, 
                  lowInd + 1, highInd + 1);
    }
    mAngleRangeLabel->setText(str);
  }
  return angleOK;
}

/*
 * Get the five tile parameters from the text boxes; return false if any have bad
 * characters
 */
bool AngleDialog::getTileTolerances(double &defTol, int &tSize, double &axisAngle,
                                    double &leftTol, double &rightTol, double &cropPixel)
{
  bool defTolOk, tileSizeOk, axisAngleOk, leftTolOk, rightTolOk, cropPixelOk;
  QString str;
  defTol = mDefTolEdit->text().toDouble(&defTolOk);
  tSize = mTileSizeEdit->text().toInt(&tileSizeOk);
  axisAngle = mAxisAngleEdit->text().toDouble(&axisAngleOk);
  leftTol = mLeftTolEdit->text().toDouble(&leftTolOk);
  rightTol = mRightTolEdit->text().toDouble(&rightTolOk);
  cropPixel = mCropPixelEdit->text().toDouble(&cropPixelOk);
  if (cropPixelOk && cropPixel < mApp->getPixelSize()) {
    cropPixel = mApp->getPixelSize();
    str.sprintf("%g", cropPixel);
    mCropPixelEdit->setText(str);
  }
  return defTolOk && tileSizeOk && axisAngleOk && leftTolOk && rightTolOk && cropPixelOk;
}

/*
 * Respond to autofit button being clicked: get the angles for the range, and the range
 * step, then get the from and to angles and correct them, then call autofit
 */
void AngleDialog::autofitClicked()
{
  int err, rangeStep;
  double lowAngle, highAngle, rangeLow, rangeHigh;
  bool lAngleOk, hAngleOk, skipOk;
  bool anglesOk = getAnglesAndStep(0, rangeLow, rangeHigh, rangeStep, skipOk);
  float minAngle = mApp->getMinAngle();
  float maxAngle = mApp->getMaxAngle();
  QString str;
  lowAngle = mAutoFromEdit->text().toDouble(&lAngleOk);
  highAngle = mAutoToEdit->text().toDouble(&hAngleOk);

  // Check the angles and adjust them if necessary
  if (lAngleOk && hAngleOk && (lowAngle < minAngle || lowAngle > maxAngle ||
                               highAngle < minAngle || highAngle > maxAngle || 
                               highAngle < lowAngle)) {
    B3DCLAMP(lowAngle, minAngle, maxAngle);
    B3DCLAMP(highAngle, minAngle, maxAngle);
    if (highAngle < lowAngle) {
      minAngle = lowAngle;
      lowAngle = highAngle;
      highAngle = minAngle;
    }
    
    str.sprintf("%.2f",lowAngle);
    mAutoFromEdit->setText(str);
    str.sprintf("%6.2f",highAngle);
    mAutoToEdit->setText(str);
  }

  if (lAngleOk && hAngleOk && anglesOk & skipOk) {
    err = mApp->autoFitToRanges((float)lowAngle, (float)highAngle, rangeStep,
                                 mApp->getUseCurDefocus() ? 3 : 1);
    if (err == 1)
      QMessageBox::critical
        (NULL, "Ctfplotter: Bad character in entry",
         QString("There is a non-numeric character in one of the\n"
                 "tolerance entries or in the tilt axis angle entry.")
         + QString(mParamsOpen ? "" : "\n\nOpen Tile & wedge Parameters to see these "
                   "entries"));
    if (err == 2)
      QMessageBox::critical(NULL, "Ctfplotter: No autofit steps",
                             "Something is wrong with the entries for autofitting.\n"
                             "No tilt angle ranges could be defined for fitting over.");

  } else if (!skipOk)
    QMessageBox::critical(NULL, "Ctfplotter: Bad skip list entry", "There is either a "
                          "non-numeric character in the "
                          "list of views to skip, or the list excludes all the views");
   else
    QMessageBox::critical(NULL, "Ctfplotter: Bad character in entry", "There is a "
                          "non-numeric character in one of the angle entries");
}

void AngleDialog::autoStopClicked()
{
  mApp->setStopAutofitting(true);
}


void AngleDialog::enableApplyButton(const QString &text)
{
  mApplyButton->setEnabled(!text.isEmpty());
  mAutofitButton->setEnabled(!text.isEmpty());
}

void AngleDialog::expDefocusChecked()
{
  // Plotter::openAngleDia connects this to MyApp::setUseCurDefocus
  emit defocusMethod(0);
  anglesSet(0);
}

void AngleDialog::currDefocusChecked()
{
  emit defocusMethod(1);
  anglesSet(0);
}

void AngleDialog::initCentralTilesToggled(bool state)
{
  // Plotter::openAngleDia connects this to MyApp::setInitCentralTiles
  emit initialTileChoice(state);
  anglesSet(0);
}

void AngleDialog::wedgeIntervalChanged(double value)
{
  value = mApp->multipleOfSectorWidth(value);
  diaSetDoubleSpinBox(mWedgeIntervalSpin, value);
  mApp->setWedgeInterval(value);
  if (!mSettingFocusForCurVals)
    anglesSet(0);
}

/*
 * Update all the entries in the table
 */
void AngleDialog::updateTable(int scroll)
{
  Ilist *saved = mApp->getSavedList();
  SavedDefocus *item;
  QString str;  
  int row, col, oldNum, j, numCols = 4, phaseVary, cutonVary;
  bool enable, hasPhase, hasAstig, hasCuton;

  // See if any astigmatism appears to have been found
  oldNum = mTable->columnCount();
  hasAstig = mApp->anyAstigmatismSaved(phaseVary, cutonVary) > 0;
  if (hasAstig)
    numCols = 6;
  hasPhase = phaseVary > 1;   // SHOULD IT BE 1 OR 2?
  hasCuton = cutonVary > 1;
  if (hasPhase)
    numCols++;
  if (hasCuton)
    numCols++;

  while (mTable->columnCount() < numCols) {
    col = mTable->columnCount();
    mTable->insertColumn(col);
    for (row = 0; row < ilistSize(saved); row++) {
      QTableWidgetItem *witem = new QTableWidgetItem();
      witem->setTextAlignment(Qt::AlignRight);
      mTable->setItem(row, col, witem);
    }
  }      
    
  while (mTable->columnCount() > numCols)
    mTable->removeColumn(numCols);
    
  if (oldNum != numCols) {
    for (col = 0; col < numCols; col++) {
      row = (int)(1.66 * mTable->fontMetrics().width
                  ((numCols == 4 ? "Defocus" : (hasPhase || (col && col != 3)) ? 
                    "Astig" : "Middle")));
      mTable->setColumnWidth(col, row);
    }
    if (numCols > 4) {
      QStringList headers;
      headers << (hasPhase ? "Mid" : "Middle") << "Start" << "End" << 
        (hasPhase ? "Defoc": "Defocus");
      if (hasAstig) 
        headers << "Astig" << "Axis";
      if (hasPhase)
        headers << "Phase";
      if (hasCuton)
        headers << "Cuton";
      mTable->setHorizontalHeaderLabels(headers);
    }
  }

  for (row = 0; row < ilistSize(saved); row++) {
    
    // Add a row if needed
    if (row + 1 > mTable->rowCount()) {
      mTable->insertRow(row);
      mTable->setRowHeight(row, mTable->fontMetrics().height() + 3);
      for (j = 0; j < numCols; j++) {
        QTableWidgetItem *witem = new QTableWidgetItem();
        witem->setTextAlignment(Qt::AlignRight);
        mTable->setItem(row, j, witem);
      }
    }
    
    // Load the data
    item = (SavedDefocus *)ilistItem(saved, row);
    str.sprintf("%.2f", (item->lAngle + item->hAngle) / 2.);
    mTable->item(row,0)->setText(str);
    str.sprintf("%.2f", item->lAngle);
    mTable->item(row,1)->setText(str);
    str.sprintf("%.2f", item->hAngle);
    mTable->item(row,2)->setText(str);
    if (item->defocus2) {
      str.sprintf("%.2f", 0.5 * (item->defocus + item->defocus2));
      mTable->item(row,3)->setText(str);
      str.sprintf("%.2f", item->defocus - item->defocus2);
      mTable->item(row,4)->setText(str);
      str.sprintf("%.1f", item->astigAngle);
      mTable->item(row,5)->setText(str);
    } else {
      str.sprintf("%.2f", item->defocus);
      mTable->item(row,3)->setText(str);
      if (hasAstig) {
        mTable->item(row,4)->setText("");
        mTable->item(row,5)->setText("");
      }
    }
    if (hasPhase) {
      str.sprintf("%.1f", item->platePhase / RADIANS_PER_DEGREE);
      mTable->item(row, numCols - (hasCuton ? 2 : 1))->setText(str);
    }
    if (hasCuton) {
      str.sprintf("%.3f", item->cutOnFreq);
      mTable->item(row, numCols - 1)->setText(str);
    }
  }

  // Get rid of extra rows (hope it deletes the items)
  for (j = mTable->rowCount() - 1; j >= row; j--)
    mTable->removeRow(j);
  enable = row > 0 && !mApp->doingMultipleFits();
  mDeleteButton->setEnabled(enable);
  mReturnButton->setEnabled(enable);
  mToFileButton->setEnabled(enable);
  if (mTable->currentRow() < 0 && mTable->rowCount())
    mTable->setCurrentCell(0, 0);
  if (scroll >= 0) {
    mTable->setCurrentCell(scroll,0 );
    mTable->scrollTo(mTable->currentIndex());
  }
}

/* 
 * Delete Row clicked for table
 */
void AngleDialog::deleteClicked()
{
  int row = mTable->currentRow();
  Ilist *saved = mApp->getSavedList();
  if (row < 0 || row >= ilistSize(saved))
    return;
  ilistRemove(saved, row);
  updateTable(B3DMAX(0, row - 1));
  mApp->setSaveModified();
}

/* 
 * Set Angles clicked for table, set the angles from the current row
 */
void AngleDialog::setAnglesClicked()
{
  int row = mTable->currentRow();
  int lowNear, highNear, numViews;
  Ilist *saved = mApp->getSavedList();
  SavedDefocus *item;
  QString str;
  if (row < 0 || row >= ilistSize(saved))
    return;
  item = (SavedDefocus *)ilistItem(saved, row);
  str.sprintf("%.2f", mApp->getMidAngleAndRangeIndices(item->lAngle, item->hAngle,
                                                       lowNear, highNear, numViews));
  mMidAngleEdit->setText(str);
  diaSetSpinBox(mNumViewsSpin, numViews);
  mApp->setNumViewsInRange(numViews);
  qApp->processEvents();
  mApp->mDefocusFinder.setDefocus
    (B3DCHOICE(item->defocus2, 0.5 * (item->defocus + item->defocus2), item->defocus));
  mApp->setSettingFromTable(true);
  //if (mApp->getFindAstigmatism()) {
    mApp->setLastSpecAstigmatism(item->defocus2 ? (item->defocus - item->defocus2) : 0.);
    mApp->setLastSpecAstigAngle(item->defocus2 ? item->astigAngle : 0.);
    //}
    //if (mApp->getFindPhaseShift()) {
    mApp->setLastFitPhaseShift(item->platePhase);
    mApp->setLastFitCutOnFreq(item->cutOnFreq);
    //}
  anglesSet(0);
  mApp->setSettingFromTable(false);
}

void AngleDialog::rowDoubleClicked(int row, int column)
{
  setAnglesClicked();
}

void AngleDialog::closeEvent( QCloseEvent *e )
{
  mApp->mPlotter->mAngleDia = NULL;
  e->accept();
}

void AngleDialog::manageEnablesForAutofit(bool autoFitting)
{
  mAutoStopButton->setEnabled(mApp->getDoingAutofit());
  mInitCentralCheckBox->setEnabled(!autoFitting);
  mCurrDefocusRadio->setEnabled(!autoFitting);
  mExpDefocusRadio->setEnabled(!autoFitting);
  mDeleteButton->setEnabled(!autoFitting);
  mReturnButton->setEnabled(!autoFitting);
  mToFileButton->setEnabled(!autoFitting);
  mSaveButton->setEnabled(!autoFitting);
  mApplyButton->setEnabled(!autoFitting);
  mCloseButton->setEnabled(!autoFitting);
  mFitSingleBox->setEnabled(!autoFitting);
  mStepUpButton->setEnabled(!autoFitting);
  mStepDownButton->setEnabled(!autoFitting);
  mAutofitButton->setEnabled(!autoFitting);
  mWedgeIntervalSpin->setEnabled(!autoFitting);
  mDefocusEdit->setEnabled(!autoFitting);
  mMidAngleEdit->setEnabled(!autoFitting);
  mNumViewsSpin->setEnabled(!autoFitting);
  mDefTolEdit->setEnabled(!autoFitting);
  mLeftTolEdit->setEnabled(!autoFitting);
  mRightTolEdit->setEnabled(!autoFitting);
  mTileSizeEdit->setEnabled(!autoFitting);
  mAxisAngleEdit->setEnabled(!autoFitting);
  mRangeStepSpin->setEnabled(!autoFitting);
  mAutoFromEdit->setEnabled(!autoFitting);
  mAutoToEdit->setEnabled(!autoFitting);
  mCropPixelEdit->setEnabled(!autoFitting);
  mCropSpecCheckBox->setEnabled(!autoFitting);
  mSkipForAstigCheckBox->setEnabled(!autoFitting);
  mSkipListEdit->setEnabled(!autoFitting);
  mExpectedPhaseEdit->setEnabled(!autoFitting);
  mCutOnFreqEdit->setEnabled(!autoFitting);
  mUseCurPhaseCheckBox->setEnabled(!autoFitting);
}
