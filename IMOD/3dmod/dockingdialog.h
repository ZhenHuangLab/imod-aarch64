/*   dockingdialog.h  -  declarations for dockingdialog.cpp
 *
 *  $Id$
 */

#ifndef DOCKING_DIALOG_H
#define DOCKING_DIALOG_H
#include <qwidget.h>
#include "control.h"

class QStackedWidget;
class QToolButton;
class QIcon;
class DialogManager;
class QScreen;

class DLL_EX_IM DockingDialog : public QWidget
{
  Q_OBJECT

 public:
  DockingDialog(QWidget *parent, DialogManager *manager, const char *innerTitle,
                const char *helpPage, char keyLetter, Qt::WindowFlags fl = Qt::Window);
  ~DockingDialog();
  void addWidgetToStack(QWidget *dialog, bool initialState = true);
  void setDialogState(bool state);
  void getDialogKeyAndState(char &key, bool &openState);
  bool getScreenChanged() {return mScreenChanged;};
  void resetScreenChanged() {mScreenChanged = false;};
  float getCurDevPixRatio() {return mCurDevPixRatio;};
  void setCurDevPixRatio(float newDPR) {mCurDevPixRatio = newDPR;};
  void resizeToHintWidthAfterShow();
  
 signals:
  void dockerCloseEvent(QCloseEvent *e);
  void dockerChangeEvent(QEvent *e);

  public slots:
    void openClosePressed();
    void helpPressed();
#ifdef WATCH_DPI_CHANGE
  void screenChanged(QScreen *);
#endif

protected:
    void closeEvent(QCloseEvent *e);
    void changeEvent(QEvent *e);
    void resizeEvent(QResizeEvent *e);
    void moveEvent(QMoveEvent *e);
    bool event(QEvent *e);

 private:
  char *mHelpPage;
  char mKeyLetter;
  DialogManager *mManager;
  QStackedWidget *mStack;
  QWidget *mDialog;
  bool mOpened;
  int mFrameWidth, mOpenHeight, mClosedHeight;
  int mHintedWidth, mHintedHeight;
  QToolButton *mOpenCloseBut;
  QIcon *mOpenItIcon;
  QIcon *mCloseItIcon;
  bool mMinimized;
  float mCurDevPixRatio;
  bool mScreenChanged;
};

#endif
