/*   form_layout.h  -  declarations for form_layout.cpp
 *
 *  $Id$
 *
 */
#ifndef LAYOUTFORM_H
#define LAYOUTFORM_H

#include <qvariant.h>
typedef struct imod_pref_struct ImodPrefStruct;

#include "ui_form_layout.h"


class LayoutForm : public QWidget, public Ui::LayoutForm
{
    Q_OBJECT

public:
    LayoutForm(QWidget* parent = 0, Qt::WindowFlags fl = 0);
    ~LayoutForm();

public slots:
    virtual void init();
    virtual void update();
    virtual void unload();
    virtual void borderAdjChanged(int value);
    //virtual void toolTipsToggled( bool state );

protected:
    ImodPrefStruct *mPrefs;

protected slots:
    virtual void languageChange();

};

#endif // LAYOUTFORM_H
