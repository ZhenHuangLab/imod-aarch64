/*   form_prefscaling.h  -  declarations for form_prefscaling.cpp
 *
 *  $Id$
 *
 */
#ifndef PREFSCALINGFORM_H
#define PREFSCALINGFORM_H

#include <qvariant.h>
typedef struct imod_pref_struct ImodPrefStruct;

#include "ui_form_prefscaling.h"


class PrefScalingForm : public QWidget, public Ui::PrefScalingForm
{
    Q_OBJECT

public:
    PrefScalingForm(QWidget* parent = 0, Qt::WindowFlags fl = 0);
    ~PrefScalingForm();

public slots:
    virtual void init();
    virtual void setFontDependentWidths();
    virtual void update();
    virtual void unload();
    virtual void scanTypeChanged(int value);
    virtual void pieceTypeChanged(int value);
    virtual void autoMeanChanged( int value );
    virtual void autoSDChanged( int value );
    virtual void setTargetClicked();
    
protected:
    ImodPrefStruct *mPrefs;
    QButtonGroup *scanGroup;
    QButtonGroup *pieceGroup;

protected slots:
    virtual void languageChange();

};

#endif
