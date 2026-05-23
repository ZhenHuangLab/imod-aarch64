/*   form_autox.h  -  declarations for form_autox.cpp
 *
 *  $Id$
 *
 */
#ifndef AUTOXWINDOW_H
#define AUTOXWINDOW_H

#include <qvariant.h>

class MultiSlider;
class QButtonGroup;
class DockingDialog;

#include "ui_form_autox.h"

class AutoxWindow : public QWidget, public Ui::AutoxWindow
{
    Q_OBJECT

public:
    AutoxWindow(DockingDialog* parent, Qt::WindowFlags fl = 0);
    ~AutoxWindow();

public slots:
    virtual void init();
    virtual void setFontDependentWidths();
    virtual void contrastSelected( int which );
    virtual void sliderChanged( int which, int value, bool dragging );
    virtual void altMouse( bool state );
    virtual void followDiagonals( bool state );
    virtual void buildPressed();
    virtual void clearPressed();
    virtual void fillPressed();
    virtual void nextPressed();
    virtual void expandPressed();
    virtual void shrinkPressed();
    virtual void smoothPressed();
    virtual void setStates( int contrast, int threshold, int resolution, int altMouse, int follow );
    virtual void topCloseEvent( QCloseEvent * e );
    virtual void keyPressEvent( QKeyEvent * e );
    virtual void keyReleaseEvent( QKeyEvent * e );
    virtual void topChangeEvent(QEvent *e);

protected:
    DockingDialog *mTopWin;
    bool mCtrlPressed;
    MultiSlider *mSliders;
    QButtonGroup *contrastGroup;

protected slots:
    virtual void languageChange();



};

#endif // AUTOXWINDOW_H
