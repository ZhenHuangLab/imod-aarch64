/*
 * angledialog.h - Header for AngleDialog class
 *
 *  $Id$
 *
 */
#ifndef ANGLEDIALOG_H
#define ANGLEDIALOG_H

#include<QDialog>
class QLabel;
class QLineEdit;
class QPushButton;
class QGroupBox;
class QRadioButton;
class QTableWidget;
class QCheckBox;
class QSpinBox;
class QDoubleSpinBox;
class MyApp;

class AngleDialog :public QDialog
{
  Q_OBJECT
  public:
  AngleDialog(MyApp *app, QWidget *parent=0);
  void updateTable(int scroll = 0);
    bool getTileTolerances(double &defTol, int &tSize, double &axisAngle,
                           double &leftTol, double &rightTol, double &cropPixel);
    bool getAnglesAndStep(int doStep, double &lowAngle, double &highAngle,
                          int &rangeStep, bool &skipOk);
    void anglesSet(int step);
    void manageEnablesForAutofit(bool autoFitting);
    int getAndCheckBreakViewEntry();
    void adjustDialogSize();
    void setCropWarningLabel(QString str, int showColor);

    QLineEdit *mDefocusEdit;
    QLineEdit *mMidAngleEdit;
    QLineEdit *mDefTolEdit;
    QLineEdit *mLeftTolEdit;
    QLineEdit *mRightTolEdit;
    QLineEdit *mTileSizeEdit;
    QLineEdit *mAxisAngleEdit;
    QSpinBox *mRangeStepSpin;
    QLineEdit *mAutoFromEdit;
    QLineEdit *mAutoToEdit;
    
signals:
    void angle(double lAngle, double hAngle, double expDef, double defTol, int tileSize,
               double axisAngle, double leftTol, double rightTol, double cropPixel);
    void defocusMethod(int );
    void initialTileChoice(bool );
public slots:
    void setAnglesClicked();
    void tileParamsClicked();
private slots:
    void applyClicked();
    void enableApplyButton(const QString &text);
    void initCentralTilesToggled(bool state);
    void currDefocusChecked();
    void expDefocusChecked();
    void deleteClicked();
    void stepUpClicked();
    void stepDownClicked();
    void autofitClicked();
    void autoStopClicked();
    void rowDoubleClicked(int row, int column);
    void fitSingleToggled(bool state);
    void wedgeIntervalChanged(double value);
    void viewRangeChanged(int value);
    void rangeStepChanged(int value);
    void cropSpectraToggled(bool state);
    void skipForAstigToggled(bool state);
    void useCurrentPhaseToggled(bool state);
    void breakAtViewToggled(bool state);

protected:
    void closeEvent( QCloseEvent * e );

  private:
    void setFocusToGetCurrentsVals();
    MyApp *mApp;
    QLabel *mDefocusLabel;
    QLabel *mMidAngleLabel;
    QLabel *mNumViewsLabel;
    QLabel *mDefTolLabel;
    QLabel *mLeftTolLabel;
    QLabel *mRightTolLabel;
    QLabel *mTileSizeLabel;
    QLabel *mAxisAngleLabel;
    QLabel *mAngleRangeLabel;
    QGroupBox *mDefocusGroup;
    QRadioButton *mCurrDefocusRadio;
    QRadioButton *mExpDefocusRadio;
    QCheckBox *mInitCentralCheckBox;
    QTableWidget *mTable;
    QPushButton *mDeleteButton;
    QPushButton *mReturnButton;
    QPushButton *mToFileButton;
   
    QPushButton *mSaveButton; 
    QPushButton *mApplyButton;
    QPushButton *mCloseButton;
    QPushButton *mGraphButton;
    QLabel *mRangeStepLabel;
    QLabel *mAutoFromLabel;
    QLabel *mAutoToLabel;
    QCheckBox *mFitSingleBox;
    QPushButton *mStepUpButton;
    QPushButton *mStepDownButton;
    QPushButton *mAutofitButton;
    QPushButton *mTileParamButton;
    bool mParamsOpen;
    QDoubleSpinBox *mWedgeIntervalSpin;
    QLabel *mWedgeIntervalLabel;
    QPushButton *mAutoStopButton;
    QSpinBox *mNumViewsSpin;
    QCheckBox *mCropSpecCheckBox;
    QLineEdit *mCropPixelEdit;
    QLabel *mCropWarningLabel;
    bool mSettingFocusForCurVals;
    QLineEdit *mSkipListEdit;
    QCheckBox *mSkipForAstigCheckBox;
    QString mLastSkipString;
    QLineEdit *mExpectedPhaseEdit;
    QLineEdit *mCutOnFreqEdit;
    QCheckBox *mUseCurPhaseCheckBox;
    QCheckBox *mBreakAtAngleCheckBox;
    QLineEdit *mBreakViewEdit;
};
#endif
