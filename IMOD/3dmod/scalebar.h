/*   scalebar.h  -  declarations for scalebar.cpp
 *
 *  $Id$
 *
 */                                                                           

#ifndef SCALEBAR_H
#define SCALEBAR_H

class QGLWidget;

typedef struct scale_bar {
  bool draw;
  bool white;
  int minLength;
  int thickness;
  bool vertical;
  int position;
  int indentX;
  int indentY;
  bool useCustom;
  int customVal;
  bool useExact;
  float exactVal;
  bool colorRamp;
  bool invertRamp;
  float lastLength;
  bool drawLabels;
  int labelSize;
  int labelYoffset;
  float scaleLabel;
} ScaleBar;

float scaleBarDraw(int winx, int winy, float zoom, int background, QGLWidget *GLw,
                   float aDevicePixelRatio);
float scaleBarAssess(int winx, int winy, float zoom, int &pixlen, int &xst,
                     int &yst, int &xsize, int &ysize);
void scaleBarTestAdjust(int winx, int winy, float zoom);
void scaleBarUpdate();
void scaleBarOpen();
ScaleBar *scaleBarGetParams();
void scaleBarRedraw();
void scaleBarClosing();

#endif
