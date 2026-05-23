/*   form_object_edit.h  -  declarations for form_object_edit.cpp
 *
 *  $Id$
 */
#ifndef OBJECTEDITFORM_H
#define OBJECTEDITFORM_H

#include <qvariant.h>
#include "ui_form_object_edit.h"
class QButtonGroup;
class DockingDialog;

class objectEditForm : public QWidget, public Ui::objectEditForm
{
  Q_OBJECT

    public:
  objectEditForm(DockingDialog* parent, Qt::WindowFlags fl = 0);
  ~objectEditForm();

  public slots:
  virtual void nameChanged( const QString & newName );
  virtual void symbolChanged( int value );
  virtual void OKPressed();
  virtual void radiusChanged( int value );
  virtual void ptLimitChanged( int value );
  virtual void selectedSurface( int value );
  virtual void selectedType( int value );
  virtual void sizeChanged( int value );
  virtual void toggledDraw( bool state );
  virtual void toggledDrawLabels( bool state );
  virtual void toggledScaleForDpi( bool state );
  virtual void labelSizeChanged( int value );
  virtual void toggledFill( bool state );
  virtual void toggledMarkEnds( bool state );
  virtual void toggledArrowAtEnd( bool state );
  virtual void toggledTime( bool state );
  virtual void toggledOnSection( bool state );
  virtual void widthChanged( int value );
  virtual void toggledPlanar( bool state );
  virtual void transChanged( int value );
  virtual void toggledOutline( bool state );
  virtual void copyClicked();
  virtual void setDefaultsClicked();
  virtual void restoreClicked();
  virtual void keyPressEvent( QKeyEvent * e );
  virtual void keyReleaseEvent( QKeyEvent * e );
  virtual void topChangeEvent(QEvent *e);
  virtual void topCloseEvent( QCloseEvent *e );

 public:
  virtual void setSymbolProperties( int which, bool fill, bool markEnds, bool arrowAtEnd,
                                    int size );
  virtual void setDrawBox( bool state );
  virtual void setDrawLabelsBox( bool state );
  virtual void setScaleForDpi(bool state, bool show);
  virtual void setLabelSize( int value );
  virtual void setObjectName( char * name );
  virtual void setObjectNum( int num );
  virtual void setTimeBox( bool state, bool enabled );
  virtual void setOnSecBox( bool state );
  virtual void setPointRadius( int value );
  virtual void setFrontSurface( int value );
  virtual void setObjectType( int value );
  virtual void setLineWidth( int value );
  virtual void setPlanarBox( bool state, bool enabled );
  virtual void setPointLimit( int value );
  virtual void setCopyObjLimit( int value );
  virtual void setFillTrans(int value, bool state, bool enabled);

 protected:
  DockingDialog *mTopWin;
  QButtonGroup *typeButtonGroup;
  QButtonGroup *surfaceButtonGroup;

  protected slots:
    virtual void languageChange();

};

#endif // OBJECTEDITFORM_H
