/*   mv_listobj.h  -  declarations for list_obj.cpp
 *
 *  $Id$
 */                                                                           

#ifndef IMODV_LISTOBJ_H
#define IMODV_LISTOBJ_H

//Added by qt3to4:
#include <QLabel>
#include <QKeyEvent>
#include <QCloseEvent>

#define OBJLIST_NUMBUTTONS 9

typedef struct __imodv_struct ImodvApp;
#include <qwidget.h>
class QGridLayout;
class QFrame;
class QScrollArea;
class QPushButton;
class QSpinBox;
class QLineEdit;
class QLabel;
class DockingDialog;
class QSignalMapper;

void imodvObjectListDialog(ImodvApp *a, int state);
void imodvOlistSetChecked(ImodvApp *a, int ob, bool state);
void imodvOlistSetColor(ImodvApp *a, int ob);
void imodvOlistUpdateOnOffs(ImodvApp *a);
void imodvOlistUpdateGroups(ImodvApp *a);
bool imodvOlistObjInGroup(ImodvApp *a, int ob);
bool imodvOlistGrouping(void);

class ImodvOlist : public QWidget
{
  Q_OBJECT

 public:
  ImodvOlist(DockingDialog *parent, Qt::WindowFlags fl = Qt::Window);
  ~ImodvOlist() {};
  QFrame *mFrame;
  QScrollArea *mScroll;
  void updateGroups(ImodvApp *a);
  void adjustFrameSize();
  void addOneObject(int ob);
    int mNumPerCol;

 public slots:
  void toggleListSlot(int ob);
  void toggleGroupSlot(int ob);
  void actionButtonClicked(int which);
  void nameChanged ( const QString & );
  void curGroupChanged(int value);
  void returnPressed();
  void topCloseEvent ( QCloseEvent * e );
  void topChangeEvent(QEvent *e);

 protected:
    void keyPressEvent ( QKeyEvent * e );
    void keyReleaseEvent ( QKeyEvent * e );

 private:
    void setFontDependentWidths();
  QGridLayout *mGrid;
  QSpinBox *mGroupSpin;
  QLineEdit *mNameEdit;
  QPushButton *mButtons[OBJLIST_NUMBUTTONS];
  QLabel *mNumberLabel;
  QSignalMapper *mMapper, *mGmapper;
};

#endif
