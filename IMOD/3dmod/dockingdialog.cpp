/*
 *  dockingdialog.cpp - Container widget fo dialogs that dock in a stack
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2019 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#include "dockingdialog.h"
#include "qapplication.h"
#include <QStackedWidget>
#include <QHBoxLayout>
#include <QVBoxLayout>
#include <qtoolbutton.h>
#include <qlabel.h>
#include <qtooltip.h>
#include "imod.h"
#include "dia_qtutils.h"
#ifdef WATCH_DPI_CHANGE
#include <QWindow>
#endif

#define TOOLBUT_SIZE 16
#define BM_WIDTH 12
#define BM_HEIGHT 12

/* TO CONVERT A DIALOG:
   For a designer-based form, in designer remove Done and Help and divider line if
   that eliminates the area.  If there is one more button, think about it
   in form_.h, add
   class DockingDialog;
   change constructor QWidget to DockingDialog
   change protected closeEvent to public slot topCloseEvent
   change protected changeEvent to public slot topChangeEvent
   remove helpPressed and donePressed, add
   DockingDialog *mTopWin;
   in form_.cpp add   (or in subclass of DialogFrame)
   #include "dockingdialog.h"
   change constructor QWidget to DockingDialog
   mTopWin = parent;
   connect(mTopWin, SIGNAL(dockerCloseEvent(QCloseEvent *)), this, 
   SLOT(topCloseEvent(QCloseEvent *)));
   connect(mTopWin, SIGNAL(dockerChangeEvent(QEvent *)), this,
   SLOT(topChangeEvent(QEvent *)));
   change close() to mTopWin->close();
   change closeEvent to topCloseEvent
   change changeEvent to topChangeEvent
   get rid of help page call from helpPressed
   get rid of close from donePressed
   add mTopWin-> if call setWindowTitle
   in top-level .cpp add
   #include "dockingdialog.h"
   declare 
   static DockingDialog *sTopWin = NULL;
   switch raise to
   sTopWin->raise();
   before making the form class:
   sTopWin = new DockingDialog(imodDialogManager.parent(IMOD_DIALOG), &imodDialogManager,
   "innertitle line", "helppage.html#TOP", 'x', Qt::Window);
   sbDia = new ScaleBarForm(sTopWin);
   after success
   sTopWin->addWidgetToStack(sbDia);
   change to sTopWin-> if call setWindowTitle
   Move the manager add line to after it is shown and change to sTopWin
   adjustGeometryAndShow((QWidget *)sTopWin, IMOD_DIALOG);
   imodDialogManager.add((QWidget *)sTopWin, IMOD_DIALOG, DOCKING_DIALOG_TYPE);
   When closing, remove sTopWin not the form dia
 */

DockingDialog::DockingDialog(QWidget *parent, DialogManager *manager, 
                             const char *innerTitle, const char *helpPage, char keyLetter,
                             Qt::WindowFlags)
{
  mClosedHeight = 0;
  mHelpPage = strdup(helpPage);
  mManager = manager;
  mKeyLetter = keyLetter;
  mMinimized = false;
  mCurDevPixRatio = 0.;
  mScreenChanged = false;
  mHintedWidth = mHintedHeight = 0;

  // make the layouts and the stack widget
  QVBoxLayout *outerLayout = new QVBoxLayout(this);
  outerLayout->setSpacing(0);
  outerLayout->setContentsMargins(0,3,0,0);  // Top space only needed
  QHBoxLayout *topLayout = diaHBoxLayout(outerLayout);
  outerLayout->setSpacing(6);
  topLayout->setContentsMargins(8, 0, 8, 0);
  mStack = new QStackedWidget(this);
  outerLayout->addWidget(mStack);
  mOpened = false;
  mDialog = NULL;

  // Make the icons
  QIcon *helpIcon = new QIcon();
  helpIcon->addFile(QString(":/images/questionMark.png"), QSize(BM_WIDTH, BM_HEIGHT));
  mOpenItIcon = new QIcon();
  mOpenItIcon->addFile(QString(":/images/plusOpen.png"), QSize(BM_WIDTH, BM_HEIGHT));
  mCloseItIcon = new QIcon();
  mCloseItIcon->addFile(QString(":/images/minusClose.png"), QSize(BM_WIDTH, BM_HEIGHT));

  // Open close is a regular button because it looks weird pressed
  mOpenCloseBut = new QToolButton(this);
  topLayout->addWidget(mOpenCloseBut);
  mOpenCloseBut->setAutoRaise(false);
  mOpenCloseBut->setFixedSize(TOOLBUT_SIZE, TOOLBUT_SIZE);
  mOpenCloseBut->setFocusPolicy(Qt::NoFocus);
  mOpenCloseBut->setIcon(*mOpenItIcon);
  connect(mOpenCloseBut, SIGNAL(clicked(bool)), this, SLOT(openClosePressed()));

  // Centered title
  QLabel *title = diaLabel(innerTitle, this, topLayout);
  title->setAlignment(Qt::AlignCenter | Qt::AlignVCenter);

  // The help button
  QToolButton *helpButton = new QToolButton(this);
  topLayout->addWidget(helpButton);
  helpButton->setAutoRaise(false);
  helpButton->setFixedSize(TOOLBUT_SIZE, TOOLBUT_SIZE);
  helpButton->setFocusPolicy(Qt::NoFocus);
  helpButton->setIcon(*helpIcon);
  helpButton->setToolTip("Open Help page for this dialog");
  connect(helpButton, SIGNAL(clicked()), this, SLOT(helpPressed()));
  delete helpIcon;
}

DockingDialog::~DockingDialog() 
{
  free(mHelpPage);
}

void DockingDialog::openClosePressed()
{
  setDialogState(!mOpened);
}

void DockingDialog::setDialogState(bool state)
{
  // To open the dialog
  if (state) {
    mOpenCloseBut->setIcon(*mCloseItIcon);
    mOpenCloseBut->setToolTip("Close up the control section in this dialog");
    mStack->show();
  } else {
    mOpenCloseBut->setIcon(*mOpenItIcon);
    mOpenCloseBut->setToolTip("Show the controls in this dialog");
    mStack->hide();      
  }
  diaShowWidget(mStack, state);
  mOpened = state;

  // If we had to pound it with hints, apply the hints here and skip adjustSize
  if (state && mHintedWidth) {
    qApp->processEvents();
    resize(mHintedWidth, mHintedHeight);
    qApp->processEvents();
  } else
    adjustSize();

  if (!state && !mClosedHeight) {
    qApp->processEvents();
    mClosedHeight = frameGeometry().height();
  }
}

void DockingDialog::helpPressed()
{
  if (mHelpPage)
    imodShowHelpPage(mHelpPage);
}

// Add the real widget to the dialog stack, record the dialog size size
void DockingDialog::addWidgetToStack(QWidget *dialog, bool initialState)
{
  int left, top, right, bot;
  QLayout *layout = dialog->layout();
  if (layout) {
    layout->getContentsMargins(&left, &top, &right, &bot);
    layout->setContentsMargins(left, 0, right, (bot + 1) / 2);
    layout->setSpacing(3);
  }
  left = mManager->getNextDockerState();
  if (left >= 0)
    initialState = left > 0;
  mDialog = dialog;
  mStack->addWidget(dialog);
  setDialogState(true);
  qApp->processEvents();
  setMinimumWidth(width());
  mFrameWidth = frameGeometry().width();
  mOpenHeight = frameGeometry().height();
  if (!initialState)
    setDialogState(false);

  utilInitializeScreenChange(this, mCurDevPixRatio);
}

// If the initial size is too big and gets locked in by setting minimum size above
// this call can be done after the show.  It was needed for bead fixer and some plugins, 
// might be needed elsewhere.
void DockingDialog::resizeToHintWidthAfterShow()
{
  int hwidth;
  qApp->processEvents();
  hwidth = sizeHint().width();
  if (minimumWidth() > hwidth)
    setMinimumWidth(hwidth);
  mHintedWidth = B3DMIN(width(), hwidth);
  mHintedHeight = B3DMIN(sizeHint().height(), height());
  resize(mHintedWidth, mHintedHeight);
  qApp->processEvents();
  mFrameWidth = frameGeometry().width();
}

void DockingDialog::getDialogKeyAndState(char &key, bool &openState)
{
  key = mKeyLetter;
  openState = mOpened;
}

void DockingDialog::closeEvent(QCloseEvent *e)
{
  emit dockerCloseEvent(e);
}

void DockingDialog::changeEvent(QEvent *e)
{
  emit dockerChangeEvent(e);
  if (e->type() == QEvent::FontChange)
    adjustSize();
}

bool DockingDialog::event(QEvent *e)
{
  bool minimized = e->type() == QEvent::WindowStateChange &&
    (windowState() & Qt::WindowMinimized);
  bool normal = e->type() == QEvent::WindowStateChange &&
    (windowState() & (Qt::WindowNoState | Qt::WindowMaximized));
  if (e->type() == QEvent::ActivationChange && isActiveWindow())
    mManager->dockerWasActivated(this);
  if ((minimized || e->type() == QEvent::Hide) && !mMinimized) {
    mMinimized = true;
    mManager->dockerWasHidden(this);
  } else  if ((normal || e->type() == QEvent::Show) && mMinimized) {
    mMinimized = false;
    mManager->dockerWasUnhidden(this);
  }
  return QWidget::event(e);
}

void DockingDialog::resizeEvent(QResizeEvent *e)
{
  mManager->dockerHasResized(this, e);
}

void DockingDialog::moveEvent(QMoveEvent *e)
{
  mManager->dockerHasMoved(this, e);
}

#ifdef WATCH_DPI_CHANGE
void DockingDialog::screenChanged(QScreen *)
{
  mScreenChanged = true;
  mManager->dockerChangedScreen(this);
}
#endif
