/*
 * plotter.h - Header for Plotter class
 *
 *  $Id$
 *
 */
#ifndef PLOTTER_H
#define PLOTTER_H

#include <QMap>
#include <QPixmap>
#include <QVector>
#include <QWidget>
#include <QPrinter>

class QLabel;
class QToolButton;
class PlotSettings;
class FittingDialog;
class AngleDialog;
class MyApp;
class QPushButton;
class QCheckBox;

#define LABEL2_ERROR    (1 << 0)
#define LABEL2_SCORE    (1 << 1)
#define LABEL2_ASTIG    (1 << 2)
#define LABEL2_WEDGE    (1 << 3)
#define LABEL2_PHASE    (1 << 4)
#define LABEL2_CUTON    (1 << 5)
#define LABEL2_ZFERR    (1 << 6)
#define LABEL2_PH_FAIL  (1 << 7)
#define LABEL2_CO_FAIL  (1 << 8)
#define LABEL2_FALLBACK (1 << 9)
#define LABEL2_CEN_ANGLE (1 << 10)

class Plotter : public QWidget
{
    Q_OBJECT
public:
  Plotter(MyApp *app, QWidget *parent = 0);
    ~Plotter();
    void setPlotSettings(const PlotSettings &settings);
    void setCurveData(int id, const QVector<QPointF> &data);
    void clearCurve(int id);
    QSize minimumSizeHint() const;
    QSize sizeHint() const;
    void manageLabels(double zero, double defocus, double def2, double defAvg,
                      int type);
    void manageLabels2(double astig, double axis, double error, double wedge, 
                       double phase, double cuton, int flags);
    void startDlgPlacementTimer();
    QToolButton *mTileButton;
    FittingDialog *mFittingDia;
    AngleDialog *mAngleDia;
    QVector<PlotSettings> mZoomStack[2];
    int mCurZoom[2];
    int mCurStack;

public slots:
    void zoomIn();
    void zoomOut();
    void printIt();
    void openFittingDia();
    void openAngleDia();
    void ctfHelp();
    void colorToggled(bool state);
    void setInStartup(bool inStartup) {mInStartup = inStartup;};
    void positionDialog(QWidget *newDia, QWidget *otherDia);

protected:
    void paintEvent(QPaintEvent *event);
    void resizeEvent(QResizeEvent *event);
    void mousePressEvent(QMouseEvent *event);
    void mouseDoubleClickEvent(QMouseEvent *event);
    void mouseMoveEvent(QMouseEvent *event);
    void mouseReleaseEvent(QMouseEvent *event);
    void keyPressEvent(QKeyEvent *event);
    void wheelEvent(QWheelEvent *event);
    void timerEvent(QTimerEvent *event);

private:
    void updateRubberBandRegion();
    void refreshPixmap();
    void drawGrid(QPainter *painter, bool onScreen);
    void drawCurves(QPainter *painter);

    enum { LeftMargin = 60, RightMargin = 20, TopMargin = 50, BottomMargin = 55};

    MyApp *mApp;
    QLabel* mZeroLabel;
    QLabel* mDefocusLabel;
    QLabel* mDefoc2Label;
    QLabel* mDefocAvgLabel;
    QLabel *mErrScoreLabel;
    QLabel *mAstigDefLabel;
    QLabel *mAstigAxisLabel;
    QLabel *mWedgeErrLabel;
    QLabel *mPhaseShiftLabel;
    QLabel *mCutOnFreqLabel;
    QToolButton *mZoomInButton;
    QToolButton *mZoomOutButton;
    QToolButton *mPrintButton;
    QPushButton *mRangeButton;
    QPushButton *mAngleButton;
    QToolButton *mSaveButton;
    QToolButton *mHelpButton;
    QCheckBox *mColorCheckBox;
    QMap<int, QVector<QPointF> > mCurveMap;
    bool mRubberBandIsShown;
    QRect mRubberBandRect;
    QPixmap mPixmap;
    QPrinter *mPrinter;
    bool mInStartup;
    int mTimerID;
};

class PlotSettings
{
public:
    PlotSettings();

    void scroll(int dx, int dy);
    void adjust();
    double spanX() const { return maxX - minX; }
    double spanY() const { return maxY - minY; }

    double minX;
    double maxX;
    int numXTicks;
    double minY;
    double maxY;
    int numYTicks;

private:
    void adjustAxis(double &min, double &max, int &numTicks);
};
#endif
