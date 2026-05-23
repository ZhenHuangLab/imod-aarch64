#include "gpuframe.h"

int gpuAvailable(int nGPU, float *memory, int debug)
{
  *memory = 0.;
  return 0;
}

int gpuInitializeSlice(float *sliceData, int nxFile, int nyFile, int stripXdim, int nxPad,
                       int nyPad, bool doFullImages) {return 1;};
int gpuExtractAndTransform(int stripInd, int stripBegin, int stripEnd, int nxTaper,
                           int nyTaper) {return 1;};
int gpuCorrectCTF(int stripInd, float freq_scalex, float freq_scaley, float pointDefocus,
                  float cosAstig, float sinAstig, float focusSum, float focusDiff,
                  float cutonAngstroms, float phaseFracFactor, float phaseShift, 
                  float ampAngle, float C1, float C2, float scaleByPower, 
                  bool powerIsHalf, bool generalPower, float firstZeroFreqSq, 
                  float attenStartFrac, float minAttenFreqSq) {return 1;};
int gpuInterpolateColumns(int stripInd, int yoff, int stripStride, int stripMid,
                          int halfStrip, int curOffset, int lastOffset) {return 1;};
int gpuCopyColumns(int stripInd, int xoff, int yoff, int startCol, int endCol) 
{return 1;};
int gpuInterpDiagonals(int stripInd, int xoff, int yoff, int stripStride, 
                       float sinViewAxis, float cosViewAxis, float lastAxisDist,
                       float curAxisDist) {return 1;};
int gpuCopyDiagonals(int stripInd, int xoff, int yoff, float sinViewAxis, 
                     float cosViewAxis, float lowLim, float highLim) {return 1;};
int gpuReturnImage(float *finalImage) {return 1;};
void gpuGetTimes(double &copy, double &prep, double &FFT, double &correct, 
                 double &interp) {};
