/*
 *  model_draw.cpp -- Model draw, used only by the slicer window
 *
 *  Original author: James Kremer
 *  Revised by: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2019 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#include "imod.h"
#include "imodv.h"
#include "display.h"
#include "b3dgfx.h"
#include "istore.h"
#include "sslice.h"
#include "finegrain.h"

static void imodDrawContourLines(ImodView * vi, Iobj *obj, int obnum, int co,
                                 GLenum mode);
static void imodDrawObjectSymbols(ImodView *vi, Iobj *obj);
static void imodDrawSpheres(ImodView *vi, Iobj *obj, float zscale);
void static imodDrawMesh(Iobj *obj, int obNum);
static void imodDrawSymbol(Ipoint *point, int sym, int size, int flags, 
                           int linewidth);

// Static for label drawing
static Imat *sLabMat = NULL;
static int sWinSizeY;
static float sDevPixRatio;
static int sFontHeight;
static QGLWidget *sGlw;

/*
 * Main call to draw the model
 */
void imodDrawModel(ImodView *vi, Imod *imod, int drawCurrent, float zscale)
{
  Iobj  *obj;
  Icont *cont;
  Ipoint *pnt;
  int imPtSize, modPtSize, backupSize, curSize;
  int ob, co, curob, curco, curpt;
  bool hasSpheres;

  if (imod->drawmode <= 0)
    return;

  imodGetIndex(imod, &curob, &curco, &curpt);

  // Loop on real and extra objects, drawing mesh only for extra
  for (ob = 0; ob < imod->objsize + vi->numExtraObj; ob++) {
    if (ob < imod->objsize) {
      obj = &imod->obj[ob];
    } else {
      obj = ivwGetAnExtraObject(vi, ob - imod->objsize);
      if (!obj || !(obj->flags & IMOD_OBJFLAG_EXTRA_EDIT))
        continue;
    }

    if (iobjOff(obj->flags))
      continue;

    if (ob < imod->objsize)
      imodSetObjectColor(ob);
    b3dLineWidth(obj->linewidth2, obj);
    ifgSetupValueDrawing(obj, GEN_STORE_MINMAX1);

    // Draw mesh if object's flag is set
    if ((obj->extra[IOBJ_EX_FLAGS] & IOBJ_EXFLAG_MESH_ON_IMG) && obj->meshsize) {
      imodDrawMesh(obj, ob);
    }
    if (ob >= imod->objsize)
      continue;

    hasSpheres = iobjScat(obj->flags) || obj->pdrawsize;
    for (co = 0; co < obj->contsize && !hasSpheres; co++)
      hasSpheres = obj->cont[co].sizes != NULL;

    if (hasSpheres)
      imodDrawSpheres(vi, obj, zscale);
    
    if (!iobjScat(obj->flags)) {
      
      /* Draw all of the objects lines first. */
      for(co = 0; co < obj->contsize; co++){
        cont = &obj->cont[co];
        if (!cont->psize)
          continue;
        
        /* DNM 2/21/03: don't draw contours from other times */
        if (ivwTimeMismatch(vi, 0, obj, cont))
          continue;

        /* todo: ghost contour ? */
        /* check ghostmode and surface ghostmode. */
        
        if (iobjOpen(obj->flags) || (cont->flags & ICONT_OPEN) ||
            (ob == curob && co == curco))
          imodDrawContourLines(vi, obj, ob, co, GL_LINE_STRIP);
        else
          imodDrawContourLines(vi, obj, ob, co, GL_LINE_LOOP);
      }
    }
    imodDrawObjectSymbols(vi, obj);
    if (sLabMat)
      imodvDrawLabels(imod, obj, sLabMat, sWinSizeY, sDevPixRatio, sFontHeight, sGlw);
  }
  sLabMat = NULL;
  imodvCleanupLabelFont();
     
  /* 7/3/07: switched to having slicer draw ends and current point symbol, but
     leave code in here just in case */
  if (!drawCurrent)
    return;

  /* draw ends of current contour. */
  /* draw current point symbol.    */
  obj = imodObjectGet(imod);
  cont = imodContourGet(imod);
  pnt = imodPointGet(imod);
  utilCurrentPointSize(obj, &modPtSize, &backupSize, &imPtSize);

  /* draw if current contour exists and it is not at wrong time */
  if (cont && cont->psize && !ivwTimeMismatch(vi, 0, obj, cont) && 
      vi->drawcursor) { 
    
    /* draw ends if more than one point */
    if (cont->psize > 1) {
      b3dColorIndex(App->bgnpoint);
      imodDrawSymbol(cont->pts, IOBJ_SYM_CIRCLE, 
                     modPtSize, 0, obj->linewidth2);
      b3dColorIndex(App->endpoint);
      imodDrawSymbol(&(cont->pts[cont->psize - 1]), IOBJ_SYM_CIRCLE, 
                     modPtSize, 0, obj->linewidth2);
    }

    /* draw current point in model mode */
    if (pnt && imod->mousemode == IMOD_MMODEL) {
      b3dColorIndex(App->curpoint);
      curSize = modPtSize;
      if (cont->psize > 1 && 
          (pnt == cont->pts || pnt == cont->pts + cont->psize - 1))
        curSize = backupSize;
      imodDrawSymbol(pnt, IOBJ_SYM_CIRCLE, curSize, 0, obj->linewidth2);
    }
  }   
}

/*
 * Call to draw the extra objects: drawAllZ = 0 means draw everything that is not flagged
 * as ALL_Z or CURSOR_LIKE; 1 means draw All_Z if not cursor-like; 2 means draw ALL_Z
 * and CURSOR_LIKE
 */
bool imodDrawExtraObjects(ImodView *vi, Imod *imod, float zscale, int drawAllZ)
{
  int ob, co, contAllZ;
  Iobj *xobj;
  Icont *cont;
  bool hasSpheres, cursorLike, retVal = false;

  if (imod->drawmode <= 0 && !drawAllZ)
    return 0;

  for (ob = 0; ob < vi->numExtraObj; ob++) {
   xobj = ivwGetAnExtraObject(vi, ob);
    if (!xobj || !xobj->contsize)
      continue;
    if (iobjOff(xobj->flags) || (xobj->flags & IMOD_OBJFLAG_MODV_ONLY) || 
        !(xobj->extra[IOBJ_EX_FLAGS] & IOBJ_EXFLAG_SLICER_ONLY))
      continue;

    customGhostColor((int)(255. * xobj->red), (int)(255. * xobj->green), 
                     (int)(255. * xobj->blue));
    b3dLineWidth(xobj->linewidth2, xobj);
    ifgResetValueSetup();

    if (!drawAllZ) {
      hasSpheres = iobjScat(xobj->flags) || xobj->pdrawsize;
      for (co = 0; co < xobj->contsize && !hasSpheres; co++)
        hasSpheres = xobj->cont[co].sizes != NULL;
      
      if (hasSpheres)
        imodDrawSpheres(vi, xobj, zscale);
    }

    if (!iobjScat(xobj->flags)) {
      for (co = 0; co < xobj->contsize; co++) {
        cont = &xobj->cont[co];
        if (!cont->psize || 
            ((cont->flags & ICONT_MMODEL_ONLY) && vi->imod->mousemode != IMOD_MMODEL))
          continue;
        cursorLike = (cont->flags & ICONT_CURSOR_LIKE) != 0;
        contAllZ = cont->flags & ICONT_DRAW_ALLZ;
        if ((!drawAllZ && !cursorLike && !contAllZ) ||
            (drawAllZ == 1 && contAllZ && !cursorLike) ||
            (drawAllZ == 2 && (contAllZ || cursorLike))) {
          if (cursorLike)
            retVal = true;
          if (iobjOpen(xobj->flags) || (cont->flags & ICONT_OPEN))
            imodDrawContourLines(vi, xobj, -1 - ob, co, GL_LINE_STRIP);
          else
            imodDrawContourLines(vi, xobj, -1 - ob, co, GL_LINE_LOOP);
        }
      }
    }
    if (!drawAllZ && sLabMat)
      imodvDrawLabels(imod, xobj, sLabMat, sWinSizeY, sDevPixRatio, sFontHeight, sGlw);
      
  }
  sLabMat = NULL;
  imodvCleanupLabelFont();
  resetGhostColor();
  return retVal;
}

/*
 * Separate function to pass in all the parameters needed for label drawing
 * Must be renewed before each call to general or label draw
 */
void imodDrawSetupLabelDraw(Imat *mat, int winSizeY, float devPixRatio,
                            int fontHeight, QGLWidget *glw)
{
  sLabMat = mat;
  sWinSizeY = winSizeY;
  sDevPixRatio = devPixRatio;
  sFontHeight = fontHeight;
  sGlw = glw;
  imodvCleanupLabelFont();
}


/*
 * Draws contour lines for regular or extra objects (obnum negative)
 */
static void imodDrawContourLines(ImodView * vi, Iobj *obj, int obnum, int co,
                                 GLenum mode)
{
  Ipoint *point;
  Icont *cont = &obj->cont[co];
  DrawProps contProps, ptProps;
  int pt, lpt;
  int nextChange, stateFlags, changeFlags;
  int handleFlags = HANDLE_LINE_COLOR | HANDLE_2DWIDTH;
  int scaleSizes = getSlicerThicknessScaling();
  bool selected = imodSelectionListQuery(vi, obnum, co) > -2;
     
  if (ifgGetValueSetupState())
    handleFlags |= HANDLE_VALUE1;

  point = cont->pts;
  lpt = cont->psize;
  nextChange = ifgHandleContChange(obj, co, &contProps, &ptProps, &stateFlags,
                                   handleFlags, selected, scaleSizes);
  if (contProps.gap)
    return;

  utilEnableStipple(vi, cont);

  glBegin(GL_LINE_STRIP);
  for (pt = 0; pt < lpt; pt++, point++) {
    glVertex3fv((float *)point);
    ptProps.gap = 0;
    if (nextChange == pt)
      nextChange = ifgHandleNextChange(obj, cont->store, &contProps, 
                                       &ptProps, &stateFlags, &changeFlags,
                                       handleFlags, 0, scaleSizes);
    if (ptProps.gap) {
      glEnd();
      glBegin(GL_LINE_STRIP);
    }
  }
  if (mode == GL_LINE_LOOP && !ptProps.gap)
    glVertex3fv((float *)cont->pts);
  glEnd();
  utilDisableStipple(vi, cont);
}

/*
 * Draw symbols at points and end markers 
 */
static void imodDrawObjectSymbols(ImodView *vi, Iobj *obj)
{
  Icont  *cont;
  Ipoint *point;
  Ipoint vert;
  int co, pt, lpt;
  DrawProps contProps, ptProps;
  int nextChange, stateFlags, changeFlags;
  int handleFlags = HANDLE_LINE_COLOR | HANDLE_2DWIDTH;

  if (ifgGetValueSetupState())
    handleFlags |= HANDLE_VALUE1;

  for (co = 0; co < obj->contsize; co++) {
    cont = &obj->cont[co];

    if (ivwTimeMismatch(vi, 0, obj, cont))
      continue;

    nextChange = ifgHandleContChange(obj, co, &contProps, &ptProps,
                                     &stateFlags, handleFlags, 0);
    if (contProps.gap)
      continue;

    lpt = cont->psize;
    point = cont->pts;
    for (pt = 0; pt < lpt; pt++, point++){
      ptProps.gap = 0;
      if (nextChange == pt)
        nextChange = ifgHandleNextChange(obj, cont->store, &contProps, 
                                         &ptProps, &stateFlags, 
                                         &changeFlags, handleFlags, 0);
      if (ptProps.symtype != IOBJ_SYM_NONE && 
          !(ptProps.gap && ptProps.valskip))
        imodDrawSymbol(point, ptProps.symtype, ptProps.symsize, 
                       ptProps.symflags, ptProps.linewidth2);
    }

    /* draw end markers for all kinds of contours */
    if (obj->symflags & IOBJ_SYMF_ENDS && lpt) {
      point = cont->pts;
      b3dColorIndex(App->bgnpoint);
      b3dLineWidth(contProps.linewidth2);

      for (pt = 0; pt < 2; pt++) {
        vert = *point;
        glBegin(GL_LINES);
        vert.x -= obj->symsize/2.;
        vert.y -= obj->symsize/2.;
        glVertex3fv((float *)&vert);
        vert.x += obj->symsize;
        vert.y += obj->symsize;
        glVertex3fv((float *)&vert);
        glEnd();

        glBegin(GL_LINES);
        vert.x -= obj->symsize;
        glVertex3fv((float *)&vert);
        vert.x += obj->symsize;
        vert.y -= obj->symsize;
        glVertex3fv((float *)&vert);
        glEnd();

        point = &(cont->pts[lpt - 1]);
        b3dColorIndex(App->endpoint);
      }
      
    }
  }
  return;
}

/*
 * Draw spheres
 */
static void imodDrawSpheres(ImodView *vi, Iobj *obj, float zscale)
{
  Icont *cont;
  Ipoint *point;
  int pt, co;
  int stepRes;
  DrawProps contProps, ptProps;
  int nextChange, stateFlags, changeFlags;
  int handleFlags = HANDLE_LINE_COLOR;
  float zinv = 1.0f / zscale;
  GLdouble drawsize;
  GLuint listIndex;
#ifdef GLU_QUADRIC_HACK
  GLUquadricObj *qobj = gluNewQuadric();
#else
  static GLUquadricObj *qobj = NULL;
  if (!qobj)
    qobj = gluNewQuadric();
#endif

  /* The default is GLU_FILL, and it makes it dotty at the equator */
  /* But GLU_LINE does not give a circle on every slice */
  gluQuadricDrawStyle(qobj, GLU_FILL);

  /* Make a display list for the default size */
  drawsize = obj->pdrawsize / vi->xybin;
  if (drawsize) {
    stepRes = (int)(drawsize < 5 ? drawsize + 4 : 8);
    listIndex = glGenLists(1);
    glNewList(listIndex, GL_COMPILE);
    gluSphere(qobj, drawsize , stepRes * 2, stepRes);
    glEndList();
  }

  if (ifgGetValueSetupState())
    handleFlags |= HANDLE_VALUE1;

  glMatrixMode(GL_MODELVIEW);
  glPushMatrix();



  for (co = 0; co < obj->contsize; co++) {
    cont = &obj->cont[co];
    if (!cont->psize)
      continue;
    if (ivwTimeMismatch(vi, 0, obj, cont))
      continue;

    nextChange = ifgHandleContChange(obj, co, &contProps, &ptProps, 
                                     &stateFlags, handleFlags, 0);
    if (contProps.gap)
      continue;
    for (pt = 0, point = cont->pts; pt < cont->psize; pt++, point++) {
      ptProps.gap = 0;
      if (nextChange == pt)
        nextChange = ifgHandleNextChange(obj, cont->store, &contProps, 
                                         &ptProps, &stateFlags, 
                                         &changeFlags, handleFlags, 0);
      drawsize = imodPointGetSize(obj, cont, pt)  / vi->xybin;
      if (!drawsize || (ptProps.gap && ptProps.valskip))
        continue;
      glPushMatrix();
      glTranslatef(point->x, point->y, point->z);

      // Undo the z-scaling that is applied later to get points in the right
      // place, to get round spheres
      glScalef(1.0f, 1.0f, zinv);

      /* Use the display list if default; otherwise individual size */
      if (drawsize == obj->pdrawsize)
        glCallList(listIndex);
      else {
        stepRes = (int)(drawsize < 5 ? drawsize + 4 : 8);
        gluSphere(qobj, drawsize , stepRes * 2, stepRes);
      }
      glPopMatrix();
    }
  }
  if (obj->pdrawsize)
    glDeleteLists(listIndex, 1);

  glPopMatrix();
#ifdef GLU_QUADRIC_HACK
  gluDeleteQuadric(qobj);
#endif
}

// Draw mesh lines as in mv_ogl
void static imodDrawMesh(Iobj *obj, int obNum)
{
  Imesh *mesh;
  DrawProps defProps, curProps;
  Ipoint *vert;
  float *firstPt;
  int    *mlist;
  int i, j, me, nextChange, stateFlags, changeFlags, nextItemIndex, resol;
  float firstRed, firstGreen, firstBlue;

  imodMeshNearestRes(obj->mesh, obj->meshsize, ImodvClosed ? 0 : Imodv->lowres, &resol);
  if (utilManagePairedMeshes(obj, obNum))
    return;

  for (me = 0; me < obj->meshsize; me++) {
    mesh = &obj->mesh[me];
    if (imeshResol(mesh->flag) != resol)
      continue;
    if (imeshThickness(mesh->flag) != obj->meshThickness)
      continue;
    
    vert = mesh->vert;
    mlist = mesh->list;

    // Initialize the state if there is a store, and get the color set
    stateFlags = 0;
    changeFlags = 0;
    ifgHandleSurfChange(obj, mesh->surf, &defProps, &curProps, &stateFlags, 0);
    nextItemIndex = nextChange = istoreFirstChangeIndex(mesh->store);
    glColor3f(curProps.red, curProps.green, curProps.blue);

    for (i  = 0; i < mesh->lsize; i++) {
      switch(mlist[i]) {
      case IMOD_MESH_BGNPOLY:
      case IMOD_MESH_BGNBIGPOLY:
      case IMOD_MESH_BGNPOLYNORM:
        while (mlist[i] != IMOD_MESH_ENDPOLY)
          i++;
        break;
        
      case IMOD_MESH_BGNPOLYNORM2:
        i++;
        while (mlist[i] != IMOD_MESH_ENDPOLY) {

          // Draw triangle with no changes as a simple loop
          if (nextChange < i || nextChange > i + 2) {
            glBegin(GL_LINE_LOOP);
            glVertex3fv( (float *)&(vert[mlist[i++]]));
            glVertex3fv( (float *)&(vert[mlist[i++]]));
            glVertex3fv( (float *)&(vert[mlist[i++]]));
            glEnd();
          } else {

            // Otherwise record first point and loop through them
            firstPt = (float *)&(vert[mlist[i]]);
            for (j = 0; j < 3; j++) {
              changeFlags = 0;

              // Process color change if any
              if (stateFlags || i == nextChange) {
                nextChange = ifgHandleMeshChange
                  (obj, mesh->store, &defProps, &curProps, &nextItemIndex, i,
                   &stateFlags, &changeFlags, 0);
                if (changeFlags & CHANGED_COLOR) {
                  glColor3f(curProps.red, curProps.green, curProps.blue);
                  //printf("chg %d %d %.2f %.2f %.2f\n", i, nextChange, curProps.red, 
                  //curProps.green, curProps.blue);
                }
                
              }

              // Save state at first point, and draw point
              if (!j) {
                glBegin(GL_LINE_STRIP);
                firstRed = curProps.red;
                firstGreen =curProps.green;
                firstBlue = curProps.blue;
              }
              //printf("%d %.1f %.1f %.1f\n", i, vert[mlist[i]].x, vert[mlist[i]].y,
              //vert[mlist[i]].z);
              glVertex3fv((float *)&(vert[mlist[i++]]));
            }

            // Reset color to first point
            if (firstRed != curProps.red || firstGreen != curProps.green ||
                firstBlue != curProps.blue) {
              //printf("reset %.2f %.2f %.2f\n", firstRed, firstGreen, firstBlue);
              glColor3f(firstRed, firstGreen, firstBlue);
            }

            //printf("first %.1f %.1f %.1f\n", firstPt[0], firstPt[1], firstPt[2]);
            glVertex3fv(firstPt);
            glEnd();

            // If reset color, better set it back to match curprops
            if (firstRed != curProps.red || firstGreen != curProps.green ||
                firstBlue != curProps.blue)
              glColor3f(curProps.red, curProps.green, curProps.blue);
 
            // Reset if not in default state and the next positive change will
            // not be in the next triangle
            if (stateFlags && (nextItemIndex < i || nextItemIndex > i + 2 || 
                               mlist[i] == IMOD_MESH_ENDPOLY)) {
              nextChange = ifgHandleMeshChange
                (obj, mesh->store, &defProps, &curProps, &nextItemIndex, i,
                 &stateFlags, &changeFlags, 0);
              if (changeFlags & CHANGED_COLOR)
                glColor3f(curProps.red, curProps.green, curProps.blue);
            }
          }      
        }
        break;
      }
    }
  }      
}

/*
 * Draw indicated type of symbol
 */
void imodDrawSymbol(Ipoint *point, int sym, int size, int flags, int linewidth)
{
  double inner, outer;
  int slices, loops;
  Ipoint vert;
#ifdef GLU_QUADRIC_HACK
  GLUquadricObj *qobj;
  if (sym == IOBJ_SYM_CIRCLE)
    qobj = gluNewQuadric();
#else
  static GLUquadricObj *qobj = NULL;
  if (!qobj)
    qobj = gluNewQuadric();
#endif

  //PIXEL_TO_DEVICE(size, size);
  switch(sym){
          
  case IOBJ_SYM_CIRCLE:
    outer = size;
    if (flags  & IOBJ_SYMF_FILL)
      inner = 0.0;
    else
      inner = outer - linewidth;
    slices = (int)(outer + 4);
    loops = 1;
    glPushMatrix();
    glTranslatef(point->x, point->y, point->z);
    gluDisk(qobj, inner, outer, slices, loops);
    glPopMatrix();
    break;
          
  case IOBJ_SYM_SQUARE:
    glBegin((flags & IOBJ_SYMF_FILL) ? GL_POLYGON : GL_LINE_LOOP);
    vert = *point;
    vert.x -= (size/2);
    vert.y -= (size/2);
    glVertex3fv((float *)&vert);
    vert.x += size;
    glVertex3fv((float *)&vert);
    vert.y += size;
    glVertex3fv((float *)&vert);
    vert.x -= size;
    glVertex3fv((float *)&vert);
    glEnd();
    break;
          
  case IOBJ_SYM_TRIANGLE:
    glBegin((flags & IOBJ_SYMF_FILL) ? GL_POLYGON : GL_LINE_LOOP);
    vert = *point;
    vert.y += size;
    glVertex3fv((float *)&vert);
    vert.x += size;
    vert.y -= (size + (size/2));
    glVertex3fv((float *)&vert);
    vert.x -= 2 * size;
    glVertex3fv((float *)&vert);
    glEnd();
    break;
          
  case IOBJ_SYM_STAR:
    break;

  case IOBJ_SYM_NONE:
    glBegin(GL_POINTS);
    glVertex3fv((float *)point);
    glEnd();
    break;
          

  default:
    break;
  }
     
#ifdef GLU_QUADRIC_HACK
  if (sym == IOBJ_SYM_CIRCLE)
    gluDeleteQuadric(qobj);
#endif
  return;
}

