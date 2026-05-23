/*   cont_copy.h  -  declarations for cont_copy.cpp
 *
 *  $Id$
 */                                                                           

#ifndef IMOD_CONT_COPY_H
#define IMOD_CONT_COPY_H

#include "dialog_frame.h"
//Added by qt3to4:
#include <QKeyEvent>
#include <QCloseEvent>
class QButtonGroup;
class QSpinBox;
class QComboBox;
class QCheckBox;
class QRadioButton;
class DockingDialog;

typedef struct ViewInfo ImodView;

class ContourCopy : public DialogFrame
{
  Q_OBJECT

 public:
  ContourCopy(DockingDialog *parent, const char *name = NULL);
  ~ContourCopy() {};
  void apply();
  void update();

  public slots:
  void buttonPressed(int which);
  void placeSelected(int which);
  void toValueChanged(int value);
  void rangeSelected(int which);
  void ellipseToggled(bool state);
  void topCloseEvent ( QCloseEvent * e );
  void topChangeEvent(QEvent *e);

 protected:
  void keyPressEvent ( QKeyEvent * e );
  void keyReleaseEvent ( QKeyEvent * e );

 private:
  DockingDialog *mTopWin;
  QComboBox *mToCombo;
  QButtonGroup *mRadioGroup;
  QSpinBox *mToSpinBox;
  QRadioButton *mTimeRadio;
  QCheckBox *mEllipseBox;
};

int openContourCopyDialog(ImodView *vw);
void iccCopyContour(void);
void imodContCopyUpdate(void);

#endif
