#ifndef GPUBP_H
#define GPUBP_H

int gpuAllocArrays(int width, int nyout, int nxProjPad, int nyProj,
                   int nplanes, int nviews, int numWarps, int numDelz,
                   int nfilt, int nreproj, int superSamp, int nxCropPad, int nyCropPad,
                   int cleanSuper, int firstNpl, int lastNpl, int use3D);
int gpuLoadLocals(float *packed, int numWarps);
int gpuAvailable(int nGPU, float &memory, int *maxTex2D, int *maxTexLayer, int *maxTex3D,
                 int debug);
int gpuLoadFilter(float *lines);
int gpuReprojLocal
(float *lines, float sinBeta, float cosBeta, float sinAlpha, float cosAlpha,
 float xzfac, float yzfac, int nxWarp, int nyWarp, int ixStartWarp, 
 int iyStartWarp, int iDelXwarp, int iDelYwarp, float *warpDelz, int nWarpDelz, 
 float dxWarpDelz, float xprojMin, float xprojMax, int lsliceStart, int lsliceEnd,
 int ithick, int iview, float xcenOut, float xcenIn, float axisXoffset, 
 int minXload, float xProjOffset, float ycenAdj, float yProjOffset,
 float centerSlice, float pmean);
int gpuBpLocal(float *slice, int lslice, int nxWarp, int nyWarp,
               int ixStartWarp, int iyStartWarp, int iDelXwarp, int iDelYwarp,
               int nxProj, float xcenOut, float xcenIn, float axisXoffset,
               float ycenOut, float centerSlice, float edgefill);
int gpuBpXtilt(float *slice, float *sinBeta, float *cosBeta, 
               float *sinAlpha, float *cosAlpha, float *xzfac, float *yzfac,
               int nxProj, int nyProj, float xcenIn, float xcenOut, float ycenOut,
               int lslice, float centerSlice, float edgefill);
int gpuBpNoX(float *slice, float *lines, float *sinBeta, float *cosBeta,
             int nxProj, float xcenIn, float xcenOut, float ycenOut,
             float edgefill);
int gpuShiftProj(int numPlanes, int lsliceStart, int loadStart);
int gpuLoadProj(float *lines, int numPlanes, int lsliceStart, int loadStart);
int gpuFilterLines(float *lines, int lslice, int filterSet);
int gpuReproject(float *lines, float sinBeta, float cosBeta, float sinAlpha, 
                 float cosAlpha, float xzfac, float yzfac, float delz,
                 int lsliceStart, int lsliceEnd, int ithick,
                 float xcenOut, float xcenPaxisOfs, int minXreproj, 
                 float xProjOffset, float ycenOut, int minYreproj,
                 float yProjOffset, float centerSlice, int ifAlpha, float pmean);
int gpuReprojOneSlice(float *slice, float *lines, float *sinBeta, float *cosBeta,
                      float ycen, int numProj, float pmean);
void gpuDone();

#endif
