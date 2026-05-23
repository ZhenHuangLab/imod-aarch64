/*
* fittingdialog.cpp - callbacks for the fitting range and parameters dialog.
*
*  Authors: Quanren Xiong and David Mastronarde
*
*  Copyright (C) 2008-2019 by the Regents of the University of 
*  Colorado.  See dist/COPYRIGHT for full copyright notice.
*
*  $Id$
*/

#include "myapp.h"
#include <stdio.h>
#include "dia_qtutils.h"
#include "plotter.h"
#include "tooledit.h"

#include "fittingdialog.h"

#define PRECISION 0.00005

FittingDialog::FittingDialog(MyApp *app, QWidget *parent) :QDialog(parent)
{
  mApp = app;
  mSettingFocusForCurVals = false;
  int width, which = mApp->getZeroFitMethod();

  setWindowTitle(tr("Fitting Params"));

  QVBoxLayout *mainLayout = new QVBoxLayout(this);
  mainLayout->setMargin(11);
  mainLayout->setSpacing(6);

  // Set up the zero-fitting method radio group
  QGroupBox *zeroGrpBox = new QGroupBox(tr("Zero-finding method"), this);
  mainLayout->addWidget(zeroGrpBox);
  QButtonGroup *zeroButGroup = new QButtonGroup(this);
  QVBoxLayout *vbox = new QVBoxLayout;
  vbox->setSpacing(0);
  vbox->setContentsMargins(5, 2, 5, 5);
  zeroGrpBox->setLayout(vbox);

  mFitCTFlikeRadio = diaRadioButton
    (tr("Fit to CTF-like curve"), this, zeroButGroup, vbox, FIT_CTF_LIKE,
     "Fit to a function based on a CTF curve over the selected range");
  mFitCtffindRadio = diaRadioButton
    (tr("Use Ctffind module"), this, zeroButGroup, vbox, FIT_USE_CTFFIND, 
     "Fit to 2D spectra with the Ctffind module over the selected range");
  diaShowWidget(mFitCtffindRadio, 
                mApp->getAllowCtffind() && !mApp->getFocalPairProcessing());

  mFitPolyRadio = diaRadioButton
    (tr("Fit polynomial to dip"), this, zeroButGroup, vbox, FIT_POLYNOMIAL,
     "Fit a polynomial over the selected range and take the minimum as the zero");
  mFitLinesRadio = diaRadioButton
    (tr("Find intersection of 2 curves"), this, zeroButGroup, vbox, FIT_TWO_CURVES,
     "Fit curves or lines over two frequency ranges and take the zero as their "
     "intersection");

  QAbstractButton *button = zeroButGroup->button(which);
  button->setChecked(true);
  connect(zeroButGroup, SIGNAL(buttonClicked(int)), this, SLOT(zeroMethodClicked(int)));

  // Checkbox for finding power and polynomial order spin button
  mPowerCheckBox = diaCheckBox(tr("Vary exponent of CTF function"), this, mainLayout);
  mPowerCheckBox->setChecked(mApp->getVaryCtfPowerInFit());
  mPowerCheckBox->setToolTip("Add a fifth parameter to the fit to vary the exponent of "
                             "the function being fit");
  connect(mPowerCheckBox, SIGNAL(clicked(bool)), this, SLOT(fitPowerClicked(bool)));

  QHBoxLayout *orderHbox = diaHBoxLayout(mainLayout);
  mOrderSpinBox = (QSpinBox *)diaLabeledSpin
    (0, 2, 6, 1, (tr("Order of polynomial:")).toLatin1(), this, orderHbox, &mOrderLabel,
     1.6);
  diaSetSpinBox(mOrderSpinBox, mApp->getPolynomialOrder());
  mOrderSpinBox->setToolTip("Set the order for the polynomial fit, e.g., 3 for cubic");
  connect(mOrderSpinBox, SIGNAL(valueChanged(int)), this, SLOT(orderChanged(int)));

  // Slow search button for ctffind
  mSlowSearchCheckBox = diaCheckBox(tr("Use 2-D initial search"), this, mainLayout);
  mSlowSearchCheckBox->setChecked(mApp->getSlowInitialSearch());
  mSlowSearchCheckBox->setToolTip("Use slower 2-D fitting in initial brute-force search");
  connect(mSlowSearchCheckBox, SIGNAL(clicked(bool)), this, 
          SLOT(slowSearchClicked(bool)));

  // X1 start and end fields
  mX1_label_1 = new QLabel(tr("X1 &Starts:"), this);
  mX1_edit_1 = new QLineEdit("", this);
  mX1_label_1->setBuddy(mX1_edit_1);
  mX1_edit_1->setToolTip("Starting frequency (in 1/pixel) of range for CTF-like, "
                         "polynomial, or first curve fit");
  mX1ResLabel1 = new QLabel("", this);

  mX1_label_2 = new QLabel(tr("X1 &Ends:"), this);
  mX1_edit_2 = new QLineEdit("", this);
  mX1_label_2->setBuddy(mX1_edit_1);
  mX1_edit_2->setToolTip("Ending frequency (in 1/pixel) of range for "
                         "first curve fit");
  mX1ResLabel2 = new QLabel("", this);

  // Set up the X1 fitting radio group
  mX1Group = new QGroupBox(tr("X1 fitting method"), this);
  QButtonGroup *x1ButGroup = new QButtonGroup(this);
  mX1LinearRadio = new QRadioButton(tr("Line"));
  mX1SimplexRadio = new QRadioButton(tr("Gaussian"));
  x1ButGroup->addButton(mX1LinearRadio, 0);
  x1ButGroup->addButton(mX1SimplexRadio, 1);
  mX1SimplexRadio->setChecked(true);

  vbox = new QVBoxLayout;
  vbox->addWidget(mX1LinearRadio);
  vbox->addWidget(mX1SimplexRadio);
  mX1Group->setLayout(vbox);
  vbox->setSpacing(0);
  vbox->setContentsMargins(5, 2, 5, 5);

  mX2_label_1 = new QLabel(tr("X2 S&tarts:"), this);
  mX2_edit_1 = new QLineEdit("", this);
  mX2_label_1->setBuddy(mX2_edit_1);
  mX2_edit_1->setToolTip("Starting frequency (in 1/pixel) of range for "
                         "second curve fit");
  mX2ResLabel1 = new QLabel("", this);

  mX2_label_2 = new QLabel(tr("X2 E&nds:"), this);
  mX2_edit_2 = new  QLineEdit("", this);
  mX2_label_2->setBuddy(mX2_edit_2);
  mX2_edit_2->setToolTip("Ending frequency (in 1/pixel) of range for CTF-like, "
                         "polynomial, or second curve fit");
  mX2ResLabel2 = new QLabel("", this);

  updateStartsEnds();

  mX2Group = new QGroupBox(tr("X2 fitting method"), this);
  mX2LinearRadio = new QRadioButton(tr("Line"));
  mX2SimplexRadio = new QRadioButton(tr("Gaussian"));
  QButtonGroup *x2ButGroup = new QButtonGroup(this);
  x2ButGroup->addButton(mX2LinearRadio, 0);
  x2ButGroup->addButton(mX2SimplexRadio, 1);
  mX2SimplexRadio->setChecked(true);

  QVBoxLayout *vbox2 = new QVBoxLayout;
  vbox2->addWidget(mX2LinearRadio);
  vbox2->addWidget(mX2SimplexRadio);
  mX2Group->setLayout(vbox2);
  vbox2->setSpacing(0);
  vbox2->setContentsMargins(5, 2, 5, 5);

  QHBoxLayout *x1HLayout_1 = new QHBoxLayout;
  x1HLayout_1->addWidget(mX1_label_1);
  x1HLayout_1->addWidget(mX1_edit_1);
  x1HLayout_1->addWidget(mX1ResLabel1);
  
  QHBoxLayout *x1HLayout_2 = new QHBoxLayout;
  x1HLayout_2->addWidget(mX1_label_2);
  x1HLayout_2->addWidget(mX1_edit_2);
  x1HLayout_2->addWidget(mX1ResLabel2);

  QVBoxLayout *x1VLayout = new QVBoxLayout;
  x1VLayout->addLayout(x1HLayout_1);
  x1VLayout->addLayout(x1HLayout_2);

  QHBoxLayout *x1HLayout_3= new QHBoxLayout;
  x1HLayout_3->addLayout(x1VLayout);
  x1HLayout_3->addWidget(mX1Group);

  QHBoxLayout *x2HLayout_1 = new QHBoxLayout;
  x2HLayout_1->addWidget(mX2_label_1);
  x2HLayout_1->addWidget(mX2_edit_1);
  x2HLayout_1->addWidget(mX2ResLabel1);

  QHBoxLayout *x2HLayout_2 = new QHBoxLayout;
  x2HLayout_2->addWidget(mX2_label_2);
  x2HLayout_2->addWidget(mX2_edit_2);
  x2HLayout_2->addWidget(mX2ResLabel2);

  QVBoxLayout *x2VLayout = new QVBoxLayout;
  x2VLayout->addLayout(x2HLayout_1);
  x2VLayout->addLayout(x2HLayout_2);

  QHBoxLayout *x2HLayout_3= new QHBoxLayout;
  x2HLayout_3->addLayout(x2VLayout);
  x2HLayout_3->addWidget(mX2Group);

  mainLayout->addLayout(x1HLayout_3);
  mainLayout->addLayout(x2HLayout_3);

  connect(mX1_edit_1, SIGNAL(textChanged(const QString &)), this,
      SLOT(enableApplyButton(const QString &)) );
  connect(mX1_edit_2, SIGNAL(textChanged(const QString &)), this,
      SLOT(enableApplyButton(const QString &)) );
  connect(mX2_edit_1, SIGNAL(textChanged(const QString &)), this,
      SLOT(enableApplyButton(const QString &)) );
  connect(mX2_edit_2, SIGNAL(textChanged(const QString &)), this,
      SLOT(enableApplyButton(const QString &)) );
  connect(mX1_edit_1, SIGNAL(editingFinished()), this, SLOT(x1editingFinished1()));
  connect(mX2_edit_2, SIGNAL(editingFinished()), this, SLOT(x2editingFinished2()));
  connect(mX1LinearRadio, SIGNAL(clicked()), this, SLOT(x1LinearChecked()) );
  connect(mX1SimplexRadio, SIGNAL(clicked()), this, SLOT(x1SimplexChecked()) );
  connect(mX2LinearRadio, SIGNAL(clicked()), this, SLOT(x2LinearChecked()) );
  connect(mX2SimplexRadio, SIGNAL(clicked()), this, SLOT(x2SimplexChecked()) );

  diaLimitEditStretch(mX1_edit_1, x1HLayout_1, 5);
  diaLimitEditStretch(mX1_edit_2, x1HLayout_2, 5);
  diaLimitEditStretch(mX2_edit_1, x2HLayout_1, 5);
  diaLimitEditStretch(mX2_edit_2, x2HLayout_2, 5);

  // Set start/end controls
  QHBoxLayout *setRangeHBox = diaHBoxLayout(mainLayout);
  setRangeHBox->setSpacing(1);
  setRangeHBox->setContentsMargins(0,0,0,0);
  mSetRangeLabel1 = diaLabel(tr("Set"), this, setRangeHBox);
  mSetStartButton = diaPushButton(tr("Start"), this, setRangeHBox);
  mSetStartButton->setToolTip("Reset the starting frequency based on current defocus "
                              "estimate");

  // Separate the buttons a bit but give more stretch to the spinner
  setRangeHBox->addStretch(5);
  mSetEndButton = diaPushButton(tr("End"), this, setRangeHBox);
  mSetEndButton->setToolTip("Reset the ending frequency to include given number of"
                              "zeros based on current defocus estimate");
  mNumZerosSpin = (QDoubleSpinBox *)diaLabeledSpin(1, 1.5, 30., 0.5, tr("at"), this,
                                                   setRangeHBox, &mSetRangeLabel2, 1.6);
  diaSetDoubleSpinBox(mNumZerosSpin, mApp->getNumZerosToFit());
  mNumZerosSpin->setToolTip("Number of zeros to include in fit, including fraction "
                            "between zeros");
  mSetRangeLabel3 = diaLabel(tr("zeros"), this, setRangeHBox);
  width = (int)(1.45 * mSetStartButton->fontMetrics().width(tr("Start")) + 0.5);
  mSetStartButton->setFixedWidth(width);
  mSetEndButton->setFixedWidth(width);
  setRangeHBox->setStretchFactor(mNumZerosSpin, 25);
  
  connect(mSetStartButton, SIGNAL(clicked()), this, SLOT(setStartClicked()) );
  connect(mSetEndButton, SIGNAL(clicked()), this, SLOT(setEndClicked()) );
  connect(mNumZerosSpin, SIGNAL(valueChanged(double)), this,
          SLOT(numZerosChanged(double)) );

  // Baseline fitting
  QHBoxLayout *baseOrderHbox = diaHBoxLayout(mainLayout);
  mBaselineSpinBox = (QSpinBox *)diaLabeledSpin
    (0, 0, 4, 1, (tr("Baseline fitting order:")).toLatin1(), this, baseOrderHbox,
     &mBaselineLabel, 1.6);
  diaSetSpinBox(mBaselineSpinBox, mApp->getBaselineOrder());
  mBaselineSpinBox->setToolTip("Set the order for a polynomial fit to make baseline "
                               "flat, or 0 for none (hot keys 0 to 4 in plotter window)");
  connect(mBaselineSpinBox, SIGNAL(valueChanged(int)), this, SLOT(baseOrderChanged(int)));

  mTwoLineCheckBox = diaCheckBox(tr("Get initial baseline from 2-line fit"), this, 
                                 mainLayout);
  mTwoLineCheckBox->setChecked(mApp->getDoTwoLineBaselineFit());
  mTwoLineCheckBox->setToolTip("Subtract baseline found from fitting two lines to "
                               "spectrum before baseline fitting, if any");
  connect(mTwoLineCheckBox, SIGNAL(clicked(bool)), this, SLOT(twoLineClicked(bool)));

  mSkipNoiseCheckBox = diaCheckBox(tr("Skip noise subtraction"), this, mainLayout);
  mSkipNoiseCheckBox->setChecked(mApp->getNoNoiseFiles());
  diaShowWidget(mSkipNoiseCheckBox, mApp->getNumNoiseFiles() > 0);
  mSkipNoiseCheckBox->setToolTip("Do not subtract noise; just use two-line initial "
                                 "baseline fit");
  connect(mSkipNoiseCheckBox, SIGNAL(clicked(bool)), this, SLOT(skipNoiseClicked(bool)));

  // Astigmatism section
  mAstigLine = diaFrameLine(this, mainLayout);

  QHBoxLayout *astigParamHLayout = diaHBoxLayout(mainLayout);
  mAstigParamButton = mApp->diaPlusMinusButton(mApp->getAstigParamsOpen(), this, 
                                               astigParamHLayout);
  mAstigParamButton->setToolTip("Open or close section for setting astigmatism "
                                "parameters");
  connect(mAstigParamButton, SIGNAL(clicked()), this, SLOT(astigParamClicked()));

  mFindAstigCheckBox = diaCheckBox(tr("Find astigmatism"), this, astigParamHLayout);
  mFindAstigCheckBox->setChecked(mApp->getFindAstigmatism());
  mFindAstigCheckBox->setToolTip("Search for min and max defocus instead of fixing "
                                 "astigmatism at zero");
  connect(mFindAstigCheckBox, SIGNAL(clicked(bool)), this, SLOT(findAstigClicked(bool)));

  QHBoxLayout *minViewsHLayout = diaHBoxLayout(mainLayout);
  mMinViewsSpinBox = (QSpinBox *)diaLabeledSpin
    (0, 1, 240, 1, tr("Min views for astigmatism:"), this, minViewsHLayout, 
     &mMinViewsLabel, 1.6);
  diaSetSpinBox(mMinViewsSpinBox, mApp->getMinViewsForAstig());
  mMinViewsSpinBox->setToolTip("Set the minimum number of views to combine to find "
                               "astigmatism");
  connect(mMinViewsSpinBox, SIGNAL(valueChanged(int)), this, SLOT(minViewsChanged(int)));

  QHBoxLayout *angleRangeHLayout = diaHBoxLayout(mainLayout);
  mWedgeAngleRangeSpin = (QDoubleSpinBox *)diaLabeledSpin
    (1, mApp->multipleOfSectorWidth(10.), mApp->multipleOfSectorWidth(120.), 
     mApp->getSectorWidth(), (tr("Wedge angle range:")).toLatin1(), this,
     angleRangeHLayout, &mWedgeRangeLabel, 1.6);
  diaSetDoubleSpinBox(mWedgeAngleRangeSpin, mApp->getWedgeAngleRange());
  connect(mWedgeAngleRangeSpin, SIGNAL(valueChanged(double)), this, 
          SLOT(wedgeRangeChanged(double)));

  QHBoxLayout *showWedgeHLayout = diaHBoxLayout(mainLayout);
  mShowWedgeFitsCheckBox = diaCheckBox(tr("Show wedge fits for"), this, showWedgeHLayout);
  mShowWedgeFitsCheckBox->setChecked(mApp->getShowWedgePlots());
  mShowWedgeFitsCheckBox->setToolTip("Display each wedge spectrum and its fitting "
                                     "results");
  connect(mShowWedgeFitsCheckBox, SIGNAL(clicked(bool)), this, 
          SLOT(showWedgeFitsClicked(bool)));

  QString str;
  str.sprintf("%.2f", mApp->getSecToShowWedges());
  mWedgeShowTimeEdit = new ToolEdit(this, 5, NULL);
  mWedgeShowTimeEdit->setText(str);
  showWedgeHLayout->addWidget(mWedgeShowTimeEdit);
  mWedgeShowTimeEdit->setToolTip("Seconds of delay after drawing a new wedge spectrum");
  mWedgeShowSecLabel = diaLabel(tr("sec"), this, showWedgeHLayout);

  // Phase section
  mPhaseLine = diaFrameLine(this, mainLayout);

  QHBoxLayout *phaseParamHLayout = diaHBoxLayout(mainLayout);
  mPhaseParamButton = mApp->diaPlusMinusButton(mApp->getPhaseParamsOpen(), this, 
                                               phaseParamHLayout);
  mPhaseParamButton->setToolTip("Open or close section for setting phase shift "
                                "parameters");
  connect(mPhaseParamButton, SIGNAL(clicked()), this, SLOT(phaseParamClicked()));

  mFindPhaseCheckBox = diaCheckBox(tr("Find phase shift"), this, phaseParamHLayout);
  mFindPhaseCheckBox->setChecked(mApp->getFindPhaseShift());
  mFindPhaseCheckBox->setToolTip("Search for phase shift from phase plate");
  connect(mFindPhaseCheckBox, SIGNAL(clicked(bool)), this, SLOT(findPhaseClicked(bool)));

  mFindZerosCheckBox = diaCheckBox(tr("Find and fit to zero positions"), this,
                                   mainLayout);
  mFindZerosCheckBox->setChecked(mApp->getFindAndFitZeros());
  mFindZerosCheckBox->setToolTip("Find positions of zeros in spectrum and use to search "
                                 "for phase shift (and cut-on frequency if selected)");
  connect(mFindZerosCheckBox, SIGNAL(clicked(bool)), this, SLOT(findZerosClicked(bool)));

  QHBoxLayout *phaseViewsHLayout = diaHBoxLayout(mainLayout);
  mPhaseViewsSpinBox = (QSpinBox *)diaLabeledSpin
    (0, 1, 240, 1, tr("Minimum views for phase:"), this, phaseViewsHLayout, 
     &mPhaseViewsLabel, 1.6);
  diaSetSpinBox(mPhaseViewsSpinBox, mApp->getMinViewsForPhase());
  mPhaseViewsSpinBox->setToolTip("Set the minimum number of views to combine to solve "
                                 "for phase");
  connect(mPhaseViewsSpinBox, SIGNAL(valueChanged(int)), this,
          SLOT(phaseViewsChanged(int)));
  
  QHBoxLayout *phaseRangeHLayout = diaHBoxLayout(mainLayout);
  mPhaseRangeLabel = diaLabel(tr("Phase search range (deg)"), this, phaseRangeHLayout);
  str.sprintf("%.1f", mApp->getPhaseSearchRange() / RADIANS_PER_DEGREE);
  mPhaseRangeEdit = new ToolEdit(this, 6, NULL);
  phaseRangeHLayout->addWidget(mPhaseRangeEdit);
  mPhaseRangeEdit->setText(str);
  mPhaseRangeEdit->setToolTip("Range of phase to search, centered on expected phase "
                              "shift");
  connect(mPhaseRangeEdit, SIGNAL(textChanged(const QString &)), this,
      SLOT(enableApplyButton(const QString &)) );
  
  mFindCutOnCheckBox = diaCheckBox(tr("Find cut-on frequency also"), this, mainLayout);
  mFindCutOnCheckBox->setChecked(mApp->getFindCutOnFreq());
  mFindCutOnCheckBox->setToolTip("Search for a cut-on frequency that reduces phase shift "
                                 "at low frequencies");
  connect(mFindCutOnCheckBox, SIGNAL(clicked(bool)), this, SLOT(findCutOnClicked(bool)));

  QHBoxLayout *maxCutonHLayout = diaHBoxLayout(mainLayout);
  mMaxCutOnLabel = diaLabel(tr("Maximum cut-on (1/nm)"), this, maxCutonHLayout);
  str.sprintf("%.3f", mApp->getMaxCutOnFreq());
  mMaxCutOnEdit = new ToolEdit(this, 6, NULL);
  maxCutonHLayout->addWidget(mMaxCutOnEdit);
  mMaxCutOnEdit->setText(str);
  mMaxCutOnEdit->setToolTip("Maximum cut-on frequency for search, in 1/nm");
  connect(mMaxCutOnEdit, SIGNAL(textChanged(const QString &)), this,
      SLOT(enableApplyButton(const QString &)) );
  
  // Bottom buttons
  mApplyButton= new QPushButton( tr("&Apply"), this);
  mApplyButton->setDefault(true);
  mApplyButton->setEnabled(false);

  mCloseButton = new QPushButton( tr("&Close"), this);
  mCloseButton->setFocusPolicy(Qt::NoFocus);

  connect(mApplyButton, SIGNAL(clicked()), this, SLOT(rangeWasSet()) );
  connect(mCloseButton, SIGNAL(clicked()), this, SLOT(close()) );

  QHBoxLayout *lowHLayout = diaHBoxLayout(mainLayout);
  lowHLayout->addWidget(mApplyButton);
  lowHLayout->addWidget(mCloseButton);

  manageWidgets(which);
  manageEnablesForAutofit(mApp->doingMultipleFits());
}

// The ranges from myapp are indexes, divide by 2 * dim to get /pixels
void FittingDialog::updateStartsEnds()
{
  float nDim = 2 * (mApp->getDim() - 1);
  float x1Index1 = mApp->getX1RangeLow() / nDim;
  float x1Index2 = mApp->getX1RangeHigh() / nDim;
  float x2Index1 = mApp->getX2RangeLow() / nDim;
  float x2Index2 = mApp->getX2RangeHigh() / nDim;
  char tmpStr[20];
  float specPix = mApp->getSpectrumPixelSize() * 10.;

  sprintf(tmpStr, "%4.3f", x1Index1);
  mX1_edit_1->setText(tmpStr);
  sprintf(tmpStr, "%4.3f", x1Index2);
  mX1_edit_2->setText(tmpStr);
  sprintf(tmpStr, "%4.3f", x2Index2);
  mX2_edit_2->setText(tmpStr);
  sprintf(tmpStr, "%4.3f", x2Index1);
  mX2_edit_1->setText(tmpStr);
  sprintf(tmpStr, "%4.1f A", specPix / x1Index1);
  mX1ResLabel1->setText(tmpStr);
  sprintf(tmpStr, "%4.1f A", specPix / x1Index2);
  mX1ResLabel2->setText(tmpStr);
  sprintf(tmpStr, "%4.1f A", specPix / x2Index2);
  mX2ResLabel2->setText(tmpStr);
  sprintf(tmpStr, "%4.1f A", specPix / x2Index1);
  mX2ResLabel1->setText(tmpStr);
}

void FittingDialog::x1editingFinished1()
{
  updateResolution(mX1_edit_1, mX1ResLabel1);
}
void FittingDialog::x2editingFinished2()
{
  updateResolution(mX2_edit_2, mX2ResLabel2);
}

void FittingDialog::updateResolution(QLineEdit *edit, QLabel *label)
{
  char tmpStr[20];
  float specPix = mApp->getSpectrumPixelSize() * 10.;
  bool xok;
  double x = edit->text().toDouble(&xok);
  if (xok) {
    sprintf(tmpStr, "%4.1f A", specPix / x);
    label->setText(tmpStr);
  }
}

/*
 * User clicked with right mouse buttom in plotter to set range end
 */
void FittingDialog::newX2EndClicked(double zero)
{
  float nDim = mApp->getDim() - 1;
  int index = B3DNINT(zero * nDim);
  if (index < mApp->getX1RangeLow() + 5)
    return;
  char tmpStr[20];
  sprintf(tmpStr, "%4.3f", 0.5 * index / nDim);
  mX2_edit_2->setText(tmpStr);
  rangeWasSet();
}

void FittingDialog::rangeWasSet()
{
  // FREQUENCY SCALE CHANGE: FOUR "2. * "
  bool x1_ok_1;
  double x1_1 = 2. * mX1_edit_1->text().toDouble(&x1_ok_1)+PRECISION;
  bool x1_ok_2;
  double x1_2 = 2. * mX1_edit_2->text().toDouble(&x1_ok_2)+PRECISION;

  bool x2_ok_1;
  double x2_1 = 2. * mX2_edit_1->text().toDouble(&x2_ok_1)+PRECISION;
  bool x2_ok_2;
  double x2_2 = 2.* mX2_edit_2->text().toDouble(&x2_ok_2)+PRECISION;
  bool phaseRangeOk, maxCutonOk;
  float phaseRange = mPhaseRangeEdit->text().toDouble(&phaseRangeOk) * RADIANS_PER_DEGREE;
  float maxCuton = mMaxCutOnEdit->text().toDouble(&maxCutonOk);
  int which = mApp->getZeroFitMethod();
  if (mApp->doingMultipleFits())
    return;

  if (phaseRangeOk && phaseRange > 0.) {
    mApp->setPhaseSearchRange(phaseRange);
  } else {
  }
    
  if (maxCutonOk && maxCuton > 0.) {
    mApp->setMaxCutOnFreq(maxCuton);
  } else {
  }

  if( x1_ok_1 && x1_ok_2 && x1_1>=0.0 && x1_2>0.0 && x1_2<=1.0+PRECISION && 
      x2_ok_1 && x2_ok_2 && x2_1>=0.0 && x2_2>0.0 && 
      x2_2<=1.0+PRECISION && ((x2_1<x2_2 && x1_1<x1_2 && which == 2) ||
                              (x1_1<x2_2 && which != 2)))

    // Plotter::openFittingDia connects this to MyApp::rangeChanged
    emit range(x1_1, x1_2, x2_1, x2_2); 
  else
    printf("Invalid range for x2 or x1 \n");
  mApplyButton->setEnabled(false);
}

// setFocus is needed to get the changes in spin boxes processed, but set a flag
// so computations are not done
// This needs to be called by widgets with NoFocus that then start a computation
void FittingDialog::setFocusToGetCurrentsVals()
{
  mSettingFocusForCurVals = true;
  setFocus();
  qApp->processEvents();
  mSettingFocusForCurVals = false;
}

void FittingDialog::enableApplyButton(const QString &text)
{
  mApplyButton->setEnabled(!text.isEmpty() );
}

// DNM 7/13/09: got rid of enableApplyButtonX1,2 because the emitted signal
// got there first and changed the selected method, so need to take care of
// the enables here

void FittingDialog::x1LinearChecked()
{
  if (mApp->getX1Method() != 0)
    mApplyButton->setEnabled(true);
  // Plotter::openFittingDia connects this to MyApp::setX1Method
  emit x1MethodChosen(0);
}

void FittingDialog::x1SimplexChecked()
{
  if (mApp->getX1Method() != 1)
    mApplyButton->setEnabled(true);
  emit x1MethodChosen(1);
}

void FittingDialog::x2LinearChecked()
{
  if (mApp->getX2Method() != 0)
    mApplyButton->setEnabled(true);
  // Plotter::openFittingDia connects this to MyApp::setX2Method
  emit x2MethodChosen(0);
}

void FittingDialog::x2SimplexChecked()
{
  if (mApp->getX2Method() != 1)
    mApplyButton->setEnabled(true);
  emit x2MethodChosen(1);
}

void FittingDialog::FittingDialog::zeroMethodClicked(int which)
{
  mApplyButton->setEnabled(true);
  manageWidgets(which);
  mApp->setZeroFitMethod(which);
}

// Refit automatically for simple changes like exponent and power
void FittingDialog::fitPowerClicked(bool state)
{
  setFocusToGetCurrentsVals();
  mApp->setVaryCtfPowerInFit(state);
  mApp->setAllViewsAstigmatism(0.);
  rangeWasSet();
}

void FittingDialog::orderChanged(int value)
{
  setFocusToGetCurrentsVals();
  mApp->setPolynomialOrder(value);
  mApp->setAllViewsAstigmatism(0.);
  rangeWasSet();
}

void FittingDialog::setStartClicked()
{
  setStartOrEnd(false);
}

void FittingDialog::setEndClicked()
{
  setStartOrEnd(true);
}

void  FittingDialog::setStartOrEnd(bool doEnd)
{
  double firstZero;
  int firstZeroIndex, secZeroIndex, backFromZero;
  setFocusToGetCurrentsVals();
  mApp->getFittingRangeFromDefocus(1, firstZero, firstZeroIndex, secZeroIndex,
                                   backFromZero);
  if (doEnd)
    mApp->setX2Range(mApp->getX2RangeLow(), secZeroIndex);
  else
    mApp->setX1Range(firstZeroIndex - backFromZero, mApp->getX1RangeHigh());
  updateStartsEnds();
  rangeWasSet();  
}

void FittingDialog::numZerosChanged(double value)
{
  mApp->setNumZerosToFit(value);
}

void FittingDialog::baseOrderChanged(int value)
{
  mApp->setBaselineOrder(value, false);
}

void FittingDialog::twoLineClicked(bool state)
{
  setFocusToGetCurrentsVals();
  mApp->setDoTwoLineBaselineFit(state, true);
}

void FittingDialog::skipNoiseClicked(bool state)
{
  setFocusToGetCurrentsVals();
  mApp->setNoNoiseFiles(state);
  manageEnablesForAutofit(mApp->doingMultipleFits());
}

void FittingDialog::slowSearchClicked(bool state)
{
  setFocusToGetCurrentsVals();
  mApp->setSlowInitialSearch(state);
  mApp->setAllViewsAstigmatism(0.);
  rangeWasSet();
}

void FittingDialog::astigParamClicked()
{
  bool open = mApp->getAstigParamsOpen();
  mAstigParamButton->setText(open ? "+" : "-");
  mApp->setAstigParamsOpen(!open);
  manageWidgets(mApp->getZeroFitMethod());
}

void FittingDialog::phaseParamClicked()
{
  bool open = mApp->getPhaseParamsOpen();
  mPhaseParamButton->setText(open ? "+" : "-");
  mApp->setPhaseParamsOpen(!open);
  manageWidgets(mApp->getZeroFitMethod());
}

void FittingDialog::findAstigClicked(bool state)
{
  if (!mApp->getAstigParamsOpen() && state)
    astigParamClicked();
  setFocusToGetCurrentsVals();
  mApp->setFindAstigmatism(state, state);
  mApp->setAllViewsAstigmatism(0.);
  rangeWasSet();
}

void FittingDialog::findPhaseClicked(bool state)
{
  if (!mApp->getPhaseParamsOpen() && state)
    phaseParamClicked();
  setFocusToGetCurrentsVals();
  mApp->setFindPhaseShift(state);
  rangeWasSet();
}

void FittingDialog::findZerosClicked(bool state)
{
  setFocusToGetCurrentsVals();
  mApp->setFindAndFitZeros(state);
  if (mApp->getFindPhaseShift())
    rangeWasSet();
}

void FittingDialog::findCutOnClicked(bool state)
{
  setFocusToGetCurrentsVals();
  mApp->setFindCutOnFreq(state);
  if (mApp->getFindPhaseShift())
    rangeWasSet();
}

void FittingDialog::minViewsChanged(int value)
{
  mApp->setMinViewsForAstig(value, mSettingFocusForCurVals);
}

void FittingDialog::phaseViewsChanged(int value)
{
  mApp->setMinViewsForPhase(value, mSettingFocusForCurVals);
}

void FittingDialog::wedgeRangeChanged(double value)
{
  value = mApp->multipleOfSectorWidth(value);
  diaSetDoubleSpinBox(mWedgeAngleRangeSpin, value);
  mApp->setWedgeAngleRange(value);
  mApp->setAllViewsAstigmatism(0.);
  if (mApp->getFindAstigmatism() && !mSettingFocusForCurVals)
    rangeWasSet();
}

void FittingDialog::showWedgeFitsClicked(bool state)
{
  mApp->setShowWedgePlots(state);
}

void FittingDialog::updateSecToShowWedges()
{
  double time = atof((mWedgeShowTimeEdit->text()).toLatin1());
  mApp->setSecToShowWedges(time);
}

void FittingDialog::setBaselineOrder(int value)
{
  mBaselineSpinBox->blockSignals(true);
  mBaselineSpinBox->setValue(value);
  mBaselineSpinBox->blockSignals(false);
}

void FittingDialog::closeEvent( QCloseEvent *e )
{
  mApp->mPlotter->mFittingDia = NULL;
  e->accept();
}

void FittingDialog::updateFitRangeIfNeeded()
{
  if (mApplyButton->isEnabled())
    rangeWasSet();
}

void FittingDialog::manageWidgets(int which)
{
  bool eitherCTF = which == FIT_USE_CTFFIND || which == FIT_CTF_LIKE;
  bool astigOpen = mApp->getAstigParamsOpen();
  bool phaseOpen = mApp->getPhaseParamsOpen();
  diaShowWidget(mPowerCheckBox, which == FIT_CTF_LIKE);
  diaShowWidget(mSlowSearchCheckBox, which == FIT_USE_CTFFIND);
  diaShowWidget(mOrderLabel, which == FIT_POLYNOMIAL);
  diaShowWidget(mOrderSpinBox, which == FIT_POLYNOMIAL);
  diaShowWidget(mX1_label_2, which == FIT_TWO_CURVES);
  diaShowWidget(mX2_label_1, which == FIT_TWO_CURVES);
  diaShowWidget(mX1_edit_2, which == FIT_TWO_CURVES);
  diaShowWidget(mX2_edit_1, which == FIT_TWO_CURVES);
  diaShowWidget(mX1ResLabel2, which == FIT_TWO_CURVES);
  diaShowWidget(mX2ResLabel1, which == FIT_TWO_CURVES);
  diaShowWidget(mX1Group, which == FIT_TWO_CURVES);
  diaShowWidget(mX1LinearRadio, which == FIT_TWO_CURVES);
  diaShowWidget(mX1LinearRadio, which == FIT_TWO_CURVES);
  diaShowWidget(mX2Group, which == FIT_TWO_CURVES);
  diaShowWidget(mX2LinearRadio, which == FIT_TWO_CURVES);
  diaShowWidget(mX2LinearRadio, which == FIT_TWO_CURVES);
  diaShowWidget(mBaselineLabel, which != FIT_USE_CTFFIND);
  diaShowWidget(mBaselineSpinBox, which != FIT_USE_CTFFIND);
  diaShowWidget(mTwoLineCheckBox, which != FIT_USE_CTFFIND);
  diaShowWidget(mSkipNoiseCheckBox, which != FIT_USE_CTFFIND && 
                mApp->getNumNoiseFiles() > 0);
  diaShowWidget(mSetRangeLabel1, eitherCTF);
  diaShowWidget(mSetRangeLabel2, eitherCTF);
  diaShowWidget(mSetRangeLabel3, eitherCTF);
  diaShowWidget(mSetStartButton, eitherCTF);
  diaShowWidget(mSetEndButton, eitherCTF);
  diaShowWidget(mNumZerosSpin, eitherCTF);
  diaShowWidget(mAstigParamButton, eitherCTF);
  diaShowWidget(mFindAstigCheckBox, eitherCTF);
  diaShowWidget(mMinViewsSpinBox, eitherCTF && astigOpen);
  diaShowWidget(mMinViewsLabel, eitherCTF && astigOpen);
  diaShowWidget(mAstigLine, eitherCTF);
  diaShowWidget(mPhaseParamButton, eitherCTF);
  diaShowWidget(mFindPhaseCheckBox, eitherCTF);
  diaShowWidget(mFindZerosCheckBox, which == FIT_CTF_LIKE && phaseOpen);
  diaShowWidget(mPhaseViewsSpinBox, eitherCTF && phaseOpen);
  diaShowWidget(mPhaseViewsLabel, eitherCTF && phaseOpen);
  diaShowWidget(mPhaseRangeLabel, eitherCTF && phaseOpen);
  diaShowWidget(mPhaseRangeEdit, eitherCTF && phaseOpen);
  diaShowWidget(mFindCutOnCheckBox, which == FIT_CTF_LIKE && phaseOpen);
  diaShowWidget(mMaxCutOnLabel, which == FIT_CTF_LIKE && phaseOpen);
  diaShowWidget(mMaxCutOnEdit, which == FIT_CTF_LIKE && phaseOpen);
  diaShowWidget(mPhaseLine, eitherCTF);
  diaShowWidget(mWedgeAngleRangeSpin, which == FIT_CTF_LIKE && astigOpen);
  diaShowWidget(mWedgeRangeLabel, which == FIT_CTF_LIKE && astigOpen);
  diaShowWidget(mShowWedgeFitsCheckBox, which == FIT_CTF_LIKE && astigOpen);
  diaShowWidget(mWedgeShowTimeEdit, which == FIT_CTF_LIKE && astigOpen);
  diaShowWidget(mWedgeShowSecLabel, which == FIT_CTF_LIKE && astigOpen);
  QApplication::processEvents();
  adjustSize();
}

void FittingDialog::manageEnablesForAutofit(bool autoFitting)
{
  mX1LinearRadio->setEnabled(!autoFitting);
  mX1SimplexRadio->setEnabled(!autoFitting);
  mX1_edit_1->setEnabled(!autoFitting);
  mX1_edit_2->setEnabled(!autoFitting);
  mX2_edit_1->setEnabled(!autoFitting);
  mX2_edit_2->setEnabled(!autoFitting);
  mX2LinearRadio->setEnabled(!autoFitting);
  mX2SimplexRadio->setEnabled(!autoFitting);
  mPowerCheckBox->setEnabled(!autoFitting);
  mOrderSpinBox->setEnabled(!autoFitting);
  mBaselineSpinBox->setEnabled(!autoFitting);
  mTwoLineCheckBox->setEnabled(!autoFitting && !mApp->getNoNoiseFiles());
  mSkipNoiseCheckBox->setEnabled(!autoFitting);
  mSlowSearchCheckBox->setEnabled(!autoFitting);
  mFindAstigCheckBox->setEnabled(!autoFitting);
  mWedgeAngleRangeSpin->setEnabled(!autoFitting);
  mMinViewsSpinBox->setEnabled(!autoFitting);
  mFindPhaseCheckBox->setEnabled(!autoFitting);
  mFindZerosCheckBox->setEnabled(!autoFitting);
  mPhaseViewsSpinBox->setEnabled(!autoFitting);
  mPhaseRangeEdit->setEnabled(!autoFitting);
  mFindCutOnCheckBox->setEnabled(!autoFitting);
  mMaxCutOnEdit->setEnabled(!autoFitting);
  mFitCTFlikeRadio->setEnabled(!autoFitting);
  mFitCtffindRadio->setEnabled(!autoFitting);
  mFitPolyRadio->setEnabled(!autoFitting);
  mFitLinesRadio->setEnabled(!autoFitting);
  mSetStartButton->setEnabled(!autoFitting);
  mSetEndButton->setEnabled(!autoFitting);
  mNumZerosSpin->setEnabled(!autoFitting);
}
