/*
 *  mv_views.cpp -- Edit, store, and access view list in model
 *                     Companion form class is imodvViewsForm in 
 *                     formv_views.ui[.h]
 *
 *  Original author: James Kremer
 *  Revised by: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2019 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#include <stdio.h>
#include <string.h>
#include "formv_views.h"
#include "imodv.h"
#include "dia_qtutils.h"
#include "imod.h"
#include "imod_input.h"
#include "mv_gfx.h"
#include "mv_input.h"
#include "mv_menu.h"
#include "mv_control.h"
#include "mv_light.h"
#include "mv_views.h"
#include "dockingdialog.h"
#include "mv_depthcue.h"
#include "mv_objed.h"
#include "control.h"
#include "undoredo.h"


struct imodv_viewed
{
  imodvViewsForm *dia;
  ImodvApp  *a;
};

static struct imodv_viewed viewStruct;
static struct imodv_viewed *ved = &viewStruct;
static DockingDialog *sTopWin = NULL;

static void build_list(ImodvApp *a);
static void imodvUpdateView(ImodvApp *a);
static void manage_world_flags(ImodvApp *a, Iview *view);

static int auto_store = 1;

static void imodvUpdateView(ImodvApp *a)
{
  a->wireframe = (a->imod->view->world & VIEW_WORLD_WIREFRAME) ? 1 : 0;
  a->lowres = (a->imod->view->world & VIEW_WORLD_LOWRES) ? 1 : 0;
  a->lighting = (a->imod->view->world & VIEW_WORLD_LIGHT) ? 1 : 0;
  a->drawLabels = (a->imod->view->world & VIEW_WORLD_LABELS) ? 1 : 0;
  a->invertZ = (a->imod->view->world & VIEW_WORLD_INVERT_Z) ? 1 : 0;
  imodvControlSetView(a);
  imodvObjedNewView();
  imodvDepthCueSetWidgets();
  imodvMenuLight(a->imod->view->world & VIEW_WORLD_LIGHT);
  imodvMenuLabels(a->imod->view->world & VIEW_WORLD_LABELS);
  imodvMenuWireframe(a->imod->view->world & VIEW_WORLD_WIREFRAME);
  imodvMenuLowres(a->imod->view->world & VIEW_WORLD_LOWRES);
  imodvMenuInvertZ(a->imod->view->world & VIEW_WORLD_INVERT_Z);
}

/* Update when a different model is being displayed (setView true) or when
   a model is being restored by undo/redo (setView false) */
void imodvUpdateModel(ImodvApp *a, bool setView)
{
  imodvUpdateView(a);
  if (!ved->dia)
    return;
  ved->dia->removeAllItems();
  build_list(a);
  if (setView)
    imodvViewsGoto(a->imod->cview - 1, false, false);
  ved->dia->selectItem(a->imod->cview - 1, true);
  ved->dia->setAutostore(auto_store);
}

/* set the current application flags into the current view */
static void manage_world_flags(ImodvApp *a, Iview *view)
{
  setOrClearFlags(&view->world, VIEW_WORLD_INVERT_Z, a->invertZ);
  setOrClearFlags(&view->world, VIEW_WORLD_LIGHT, a->lighting);
  setOrClearFlags(&view->world, VIEW_WORLD_LABELS, a->drawLabels);
  setOrClearFlags(&view->world, VIEW_WORLD_WIREFRAME, a->wireframe);
  setOrClearFlags(&view->world, VIEW_WORLD_LOWRES, a->lowres);
}

/* Automatically store the current view if auto_store is set */
void imodvAutoStoreView(ImodvApp *a)
{
  /* This is a common place to complete the set of object views */
  imodObjviewComplete(a->imod);

  if (!auto_store || !a->imod->cview)
    return;

  manage_world_flags(a, a->imod->view);
  imodViewStore(a->imod, a->imod->cview);
}

// Done: tell the box to close
void imodvViewsDone()
{
  sTopWin->close();
}
  
// Closing - do we need to autostore view?
void imodvViewsClosing()
{
  imodvDialogManager.remove((QWidget *)sTopWin);
  sTopWin = NULL;
  ved->dia = NULL;
}

// Open, close, or raise the dialog box
void imodvViewEditDialog(ImodvApp *a, int state)
{
  static int first = 1;

  if (first) {
    ved->dia = NULL;
    first = 0;
  }
  if (!state) {
    if (ved->dia)
      sTopWin->close();
    return;
  }
  if (ved->dia) {
    sTopWin->raise();
    return;
  }
  ved->a = a;

  sTopWin = new DockingDialog(imodvDialogManager.parent(IMODV_DIALOG), 
                              &imodvDialogManager, "Save/Restore Model Views",
                              "modvViewEdit.html#TOP", 'V', Qt::Window);
  ved->dia = new imodvViewsForm(sTopWin, 0);
  sTopWin->addWidgetToStack(ved->dia);

  // Set title bar
  setModvDialogTitle(sTopWin, "MV Views");

  build_list(a);
  ved->dia->setAutostore(auto_store);
  ved->dia->selectItem(a->imod->cview - 1, true);
  adjustGeometryAndShow((QWidget *)sTopWin, IMODV_DIALOG);
  imodvDialogManager.add((QWidget *)sTopWin, IMODV_DIALOG, DOCKING_DIALOG_TYPE);
}

// Save model: call appropriate routine depending on whether imod or standalone
void imodvViewsSave()
{
  if (ved->a->standalone)
    imodvFileSave();
  else
    inputSaveModel(App->cvi);
}

/*
 * The goto view callback.
 * item is numbered from 0 and incremented internally
 */
void imodvViewsGoto(int item, bool draw, bool regChg)
{
  item++;
  if (!ved->a->imod)
    return;

  imodvViewsSetView(ved->a, item, draw, false, regChg);
}

// A call that can be made when the dialog is not open and from external sources
// View is numbered from 1 here
void imodvViewsSetView(ImodvApp *a, int view, bool draw, bool external, bool regChg)
{
  /* If registering changes, do so before the autostore to capture revertable
     state */
  if (regChg) {
    if (!a->standalone)
      a->vi->undo->modelChange(UndoRedo::ViewChanged);
    imodvFinishChgUnit();
  }

  /* If changing views, store the current view before changing */
  if (view != a->imod->cview)
    imodvAutoStoreView(a);

  a->imod->cview = view;

  imodViewUse(a->imod);
  imodvNewModelAngles(&a->imod->view->rot);
  imodvDrawImodImages(a->linkToSlicer);
  if (ved->dia)
    ved->dia->selectItem(view - 1, true);

  imodvUpdateView(a);
  if (draw) 
    imodvDraw(a);

}

/* 
 * The store view callback.
 * Sets the view in the window to the current view. 
 * item is numbered from 0 and incremented internally
 */

void imodvViewsStore(int item)
{
  item++;
  Iview *view = ved->a->imod->view;

  /* 11/20/04: eliminated autostore if changing views; this is always current
     view */
  imodvRegisterModelChg();
  imodvFinishChgUnit();

  ved->a->imod->cview = item;
     
  manage_world_flags(ved->a, view);

  imodViewStore(ved->a->imod, item);
}

/* Make a new view, the form already found a unique label  */
void imodvViewsNew(const char *label)
{
  int cview;
  Iview *view = ved->a->imod->view;

  if (!ved->a->imod) return;

  imodvRegisterModelChg();
  imodvFinishChgUnit();

  imodvAutoStoreView(ved->a);

  cview = ved->a->imod->cview;
  imodViewModelNew(ved->a->imod);
  view = ved->a->imod->view;
  ved->a->imod->cview = ved->a->imod->viewsize - 1;
    
  if (cview == ved->a->imod->cview) {
    imodError(NULL,"3dmodv: error creating view\n");
    return; /* no view created. */
  }

  manage_world_flags(ved->a, view);

  cview = ved->a->imod->cview;

  strcpy(ved->a->imod->view[cview].label, label);
  imodViewStore(ved->a->imod, cview);

  ved->dia->addItem(ved->a->imod->view[cview].label);
  ved->dia->selectItem(cview - 1, true);
}

/* Delete a view
 * item is numbered from 0 and incremented internally, newCurrent is also
 */
void imodvViewsDelete(int item, int newCurrent)
{
  item++;
  Imod *imod = ved->a->imod;
  int i;

  if (!imod || item <= 0 || imod->viewsize < 2)
    return;

  if (!ved->a->standalone)
    ved->a->vi->undo->modelChange(UndoRedo::ViewChanged);
  imodvFinishChgUnit();

  // Need to free object views of the view being deleted
  if (imod->view[item].objvsize)
    free (imod->view[item].objview);

  // How about deleting the existing view?  No need, if the array grows
  // again the space is reused
  // 7/24/03 discovered it was copying from non-existent view at top
  for (i = item; i < imod->viewsize - 1; i++) {
    imod->view[i] = imod->view[i+1];
  }

  imod->viewsize--;
  imod->cview = (newCurrent >= 0 ? newCurrent : 0) + 1;
  imodvViewsGoto(imod->cview - 1, true, false);

  /* DNM 7/31/03: at least on RH 9, new selection doesn't show after deletion
     unless select another item first */
  if (imod->viewsize > 1)
    ved->dia->selectItem(imod->cview > 1 ? imod->cview - 2 : 1, true);
  ved->dia->selectItem(imod->cview - 1, true);
}

// A new label has been entered
void imodvViewsLabel(const char *label, int item)
{
  imodvRegisterModelChg();
  imodvFinishChgUnit();
  strcpy( ved->a->imod->view[item + 1].label, label);
}

void imodvViewsAutostore(int state)
{
  auto_store = state;
}

// A new initialization routine to make sure there is a real view in
// addition to the working view, and put it into use.
// This should be called whenever a model is loaded or created
void imodvViewsInitialize(Imod *imod)
{
  Ipoint imageMax = {0., 0., 0.};

  // Create view 1 if it does not exist yet, make it current if current is 0
  if (imod->viewsize < 2) {

    imodViewModelNew(imod);
    strcpy(imod->view[1].label, "view 1");
  }
  if (!imod->cview)
    imod->cview = 1;

  // Correct the scaling for the current view if it is 1:
  if (imod->view[imod->cview].rad == 1.)
    imodViewDefaultScale(imod, &imod->view[imod->cview], &imageMax, 1.);

  // Put the current view into use
  imodViewUse(imod);

  // Set the world flags in the Imodv structure
  Imodv->invertZ = (imod->view->world & VIEW_WORLD_INVERT_Z)  ? 1 : 0;
  Imodv->lighting = (imod->view->world & VIEW_WORLD_LIGHT)  ? 1 : 0;
  Imodv->drawLabels = (imod->view->world & VIEW_WORLD_LABELS)  ? 1 : 0;
  Imodv->wireframe = (imod->view->world & VIEW_WORLD_WIREFRAME) ? 1 : 0;
  Imodv->lowres = (imod->view->world & VIEW_WORLD_LOWRES) ? 1 : 0;
}

// send the list of views to the form
static void build_list(ImodvApp *a)
{
  int i;

  //  sprintf(a->imod->view->label, "Original Default View");
  for(i = 1; i < a->imod->viewsize; i++)
    ved->dia->addItem(a->imod->view[i].label);
}
