/*
 *  mv_control.h -  header for mv_control.cpp
 *
 *  $Id$
 */

#ifndef IMODV_CONTROL_H
#define IMODV_CONTROL_H

typedef struct __imodv_struct ImodvApp;

#define IMODV_ROTATION_FACTOR 1.26

#define IMODV_CONTROL_NEAR 1
#define IMODV_CONTROL_FAR 2
#define IMODV_CONTROL_FOVY 3
#define IMODV_CONTROL_ZSCALE 4
#define IMODV_CONTROL_XAXIS 1
#define IMODV_CONTROL_YAXIS 2
#define IMODV_CONTROL_ZAXIS 3
void imodvControlAxisText(int axis, float rot);
void imodvControlScale(float value);
void imodvControlKickClips(bool state);
void imodvControlZscale(int value, bool dragging);
void imodvControlRate(int value);
void imodvControlSpeed(float value);
void imodvControlIncSpeed(int step);
void imodvControlClip(int plane, int value, bool dragging);
void imodvControlZoom(int zoom);
void imodvControlStart(void);
void imodvControlAxisButton(int axisDir);
void imodvControlClosing(void);
void imodvControlQuit(void);

int imodv_control(ImodvApp *a, int state);
void imodvControlSetArot(ImodvApp *a, int newval);
void imodvControlSetView(ImodvApp *a);
void imodvControlUpdate(ImodvApp *a);
void imodvControlChangeSteps(ImodvApp *a, int delta);

#endif /* IMODV_CONTROL_H */
