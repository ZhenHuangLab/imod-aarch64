/*
 * rangedialog.h - Header for FittingDialog class
 *
 *  $Id$
 *
 */
#ifndef RANGEDIALOG_H
#define RANGEDIALOG_H

#include<QDialog>

class QLabel;
class QLineEdit;
class QPushButton;
class QGroupBox;
class QRadioButton;
class QCheckBox;
class QSpinBox;
class QDoubleSpinBox;
class QFrame;
class MyApp;
class ToolEdit;

class FittingDialog :public QDialog
{
  Q_OBJECT
  public:
    FittingDialog(MyApp *app, QWidget *parent=0);
    void manageEnablesForAutofit(bool autoFitting);
    void setBaselineOrder(int value);
    void updateStartsEnds();
    void updateSecToShowWedges();
    void updateFitRangeIfNeeded();
    void newX2EndClicked(double zero);

 protected:
    void closeEvent( QCloseEvent * e );

signals:
    void range(double lowX, double highX, double, double );
    void x1MethodChosen(int );
    void x2MethodChosen(int);
private slots:
    void enableApplyButton(const QString &text);
    void rangeWasSet();
    void x1LinearChecked();
    void x1SimplexChecked();
    void x2LinearChecked();
    void x2SimplexChecked();
    void zeroMethodClicked(int which);
    void fitPowerClicked(bool state);
    void orderChanged(int value);
    void baseOrderChanged(int value);
    void twoLineClicked(bool state);
    void slowSearchClicked(bool state);
    void findAstigClicked(bool state);
    void wedgeRangeChanged(double value);
    void minViewsChanged(int value);
    void showWedgeFitsClicked(bool state);
    void x1editingFinished1();
    void x2editingFinished2();
    void findPhaseClicked(bool state);
    void phaseViewsChanged(int value);
    void findCutOnClicked(bool state);
    void findZerosClicked(bool state);
    void skipNoiseClicked(bool state);
    void astigParamClicked();
    void phaseParamClicked();
    void setStartClicked();
    void setEndClicked();
    void numZerosChanged(double value);

 private:
    void manageWidgets(int which);
    void updateResolution(QLineEdit *edit, QLabel *label);
    void setFocusToGetCurrentsVals();
    void setStartOrEnd(bool doEnd);
    MyApp *mApp;
    QLabel *mX1_label_1;
    QLabel *mX1_label_2;
    QGroupBox *mX1Group; 
    QRadioButton *mX1LinearRadio;
    QRadioButton *mX1SimplexRadio;
    QLabel *mX2_label_1;
    QLabel *mX2_label_2;
    QLabel *mX1ResLabel1;
    QLabel *mX1ResLabel2;
    QLabel *mX2ResLabel1;
    QLabel *mX2ResLabel2;
    QLineEdit *mX1_edit_1;
    QLineEdit *mX1_edit_2;
    QLineEdit *mX2_edit_1;
    QLineEdit *mX2_edit_2;
    QGroupBox *mX2Group;
    QRadioButton *mX2LinearRadio;
    QRadioButton *mX2SimplexRadio;
    QCheckBox *mPowerCheckBox;
    QLabel *mOrderLabel;
    QSpinBox *mOrderSpinBox;
    QPushButton *mApplyButton;
    QPushButton *mCloseButton;
    QLabel *mBaselineLabel;
    QSpinBox *mBaselineSpinBox;
    QCheckBox *mTwoLineCheckBox;
    QCheckBox *mSlowSearchCheckBox;
    QCheckBox *mFindAstigCheckBox;
    QDoubleSpinBox *mWedgeAngleRangeSpin;
    QLabel *mWedgeRangeLabel;
    QSpinBox *mMinViewsSpinBox;
    QLabel *mMinViewsLabel;
    QFrame *mAstigLine;
    QCheckBox *mShowWedgeFitsCheckBox;
    ToolEdit *mWedgeShowTimeEdit;
    QLabel *mWedgeShowSecLabel;
    QRadioButton *mFitCTFlikeRadio;
    QRadioButton *mFitCtffindRadio;
    QRadioButton *mFitPolyRadio;
    QRadioButton *mFitLinesRadio;
    bool mSettingFocusForCurVals;
    QCheckBox *mFindPhaseCheckBox;
    QSpinBox *mPhaseViewsSpinBox;
    QLabel *mPhaseViewsLabel;
    QFrame *mPhaseLine;
    QLabel *mPhaseRangeLabel;
    ToolEdit *mPhaseRangeEdit;
    QCheckBox *mFindCutOnCheckBox;
    QLabel *mMaxCutOnLabel;
    ToolEdit *mMaxCutOnEdit;
    QCheckBox *mFindZerosCheckBox;
    QCheckBox *mSkipNoiseCheckBox;
    QPushButton *mAstigParamButton;
    QPushButton *mPhaseParamButton;
    QLabel *mSetRangeLabel1, *mSetRangeLabel2, *mSetRangeLabel3;
    QPushButton *mSetStartButton;
    QPushButton *mSetEndButton;
    QDoubleSpinBox *mNumZerosSpin;
};
#endif
