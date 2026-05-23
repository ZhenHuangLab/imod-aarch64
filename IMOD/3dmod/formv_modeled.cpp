/*
 *  formv_modeled.cpp - Class for model view model edit dialog form
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2019 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 * 
 * $Id$
 */

#include "formv_modeled.h"

#include <qvariant.h>
#include <qimage.h>
#include <qpixmap.h>
#include <QButtonGroup>

//Added by qt3to4:
#include <QKeyEvent>
#include <QCloseEvent>

#include "imod.h"
#include "control.h"
#include "imodv.h"
#include "mv_modeled.h"
#include "mv_input.h"
#include "dia_qtutils.h"
#include "dockingdialog.h"

/*
 *  Constructs a imodvModeledForm as a child of 'parent', with the
 *  name 'name' and widget flags set to 'f'.
 */
imodvModeledForm::imodvModeledForm(DockingDialog* parent, Qt::WindowFlags fl)
  : QWidget(parent, fl)
{
  mTopWin = parent;
  setupUi(this);
  setAttribute(Qt::WA_DeleteOnClose);
  setAttribute(Qt::WA_AlwaysShowToolTips);

  moveGroup = new QButtonGroup(this);
  moveGroup->addButton(moveOneRadio, 0);
  moveGroup->addButton(moveAllRadio, 1);
  connect(moveGroup, SIGNAL(buttonClicked(int)), this,
          SLOT(moveClicked(int)));
  editGroup = new QButtonGroup(this);
  editGroup->addButton(editOneRadio, 0);
  editGroup->addButton(editAllRadio, 1);
  connect(editGroup, SIGNAL(buttonClicked(int)), this,
          SLOT(editClicked(int)));
  connect(sameScaleButton, SIGNAL(clicked()), this, SLOT(sameScaleClicked()));
  sameScaleButton->setEnabled(Imodv->standalone != 0);
  connect(mTopWin, SIGNAL(dockerCloseEvent(QCloseEvent *)), this,
          SLOT(topCloseEvent(QCloseEvent *)));
  connect(mTopWin, SIGNAL(dockerChangeEvent(QEvent *)), this,
          SLOT(topChangeEvent(QEvent *)));
}

/*
 *  Destroys the object and frees any allocated resources
 */
imodvModeledForm::~imodvModeledForm()
{
  // no need to delete child widgets, Qt does it all for us
}

/*
 *  Sets the strings of the subwidgets using the current
 *  language.
 */
void imodvModeledForm::languageChange()
{
  retranslateUi(this);
}

// Respond to actions in the box
void imodvModeledForm::modelChanged( int which )
{
  imodvModeledNumber(which);
}

void imodvModeledForm::editClicked( int which )
{
  imodvModeledEdit(which);
}

void imodvModeledForm::moveClicked( int which )
{
  imodvModeledMove(which);
}

void imodvModeledForm::viewSelected( int which )
{
  imodvModeledView(which);
}

// Pass a name on as it changes
void imodvModeledForm::nameChanged( const QString &name )
{
  imodvModeledName(name);
}

// Inform of pixel size change when return pressed
void imodvModeledForm::newPixelSize()
{
  imodvModeledScale(1);
}

void imodvModeledForm::sameScaleClicked()
{
  imodvModeledSameScale();
}

// Return the pixel size string when requested
QString imodvModeledForm::getPixelString()
{
  return pixelSizeEdit->text();
}

// Set states for a new model
void imodvModeledForm::setModel( int curModel, int numModels, QString filename, 
				 QString internalName, QString pixelString )
{
  currentSpinBox->setValue(curModel);
  currentSpinBox->setMaximum(numModels);
  filenameLabel->setText(filename);
  internalNameEdit->setText(internalName);
  pixelSizeEdit->setText(pixelString);
}

// Initialize radio buttons
void imodvModeledForm::setMoveEdit( int move, int edit )
{
  diaSetGroup(moveGroup, move);
  diaSetGroup(editGroup, edit);
}

// Change the view selection
void imodvModeledForm::setViewSelection( int which )
{
  viewComboBox->setCurrentIndex(which);
}

void imodvModeledForm::topCloseEvent( QCloseEvent * e )
{
  imodvModeledClosing();
  e->accept();
}

// Pass on keys
void imodvModeledForm::keyPressEvent( QKeyEvent * e )
{
  if (utilCloseKey(e))
    imodvModeledDone();
  else
    imodvKeyPress(e);
}

void imodvModeledForm::keyReleaseEvent( QKeyEvent * e )
{
  imodvKeyRelease(e);
}

void imodvModeledForm::topChangeEvent(QEvent *e)
{
  QWidget::changeEvent(e);
  ivwCheckAndSetMacMenu(e, this, IMODV_DIALOG);
  if (e->type() != QEvent::FontChange)
    return;
}
