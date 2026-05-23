/*   formv_objed.h  -  declarations for formv_objed.cpp
 *
 *  $Id$
 *
 *
 */
#ifndef IMODVOBJEDFORM_H
#define IMODVOBJEDFORM_H

#include <qvariant.h>

class QStackedWidget;
class DockingDialog;

#include "ui_formv_objed.h"

class imodvObjedForm : public QWidget, public Ui::imodvObjedForm
{
  Q_OBJECT

    public:
  imodvObjedForm(DockingDialog* parent, Qt::WindowFlags fl = 0);
  ~imodvObjedForm();

  public slots:
    virtual void init();
  virtual void setFontDependentSizes( int width, int height );
  virtual void topChangeEvent(QEvent *e);
  virtual void objectSelected( int which );
  virtual void editSelected( int item );
  virtual void objSliderChanged( int value );
  virtual void nameChanged( const QString & str );
  virtual void typeSelected( int item );
  virtual void styleSelected( int item );
  virtual void frameSelected( int item );
  virtual void updateObject( int ob, int numObj, int drawType, int drawStyle, QColor color, char * name );
  virtual void updateColorBox( QColor color );
  virtual void setCurrentFrame( int frame, int editData );
  virtual void syncToCurObjToggled(bool state);
  virtual void topCloseEvent( QCloseEvent * e );
  virtual void keyPressEvent( QKeyEvent * e );
  virtual void keyReleaseEvent( QKeyEvent * e );

 protected:
  DockingDialog *mTopWin;
  QStackedWidget *mStack;

  protected slots:
    virtual void languageChange();

};

#endif // IMODVOBJEDFORM_H
