#include "gpuframe.h"
#include "framealign.h"

int fgpuGpuAvailable(int nGPU, float *memory, int debug) 
{
  *memory = 0.;
  return 0;
}

void fgpuSetUnpaddedSize(int unpadX, int unpadY, int flags, int debug) {}
int fgpuSetPreProcParams(float *gainRef, int nxGain, int nyGain,
                         float truncLimit, unsigned char *defectMap,
                         int camSizeX, int camSizeY) {return 1;}
void fgpuSetBinPadParams(int xstart, int xend, int ystart, int yend,
                         int binning, int nxTaper, int nyTaper, 
                         int type, int filtType, int noiseLen) {}
int fgpuSetupSumming(int fullXpad, int fullYpad, int sumXpad, int sumYpad, int evenOdd) {return 1;}
int fgpuSetupAligning(int alignXpad, int alignYpad, int sumXpad, int sumYpad,
                    float *alignMask, int aliFiltSize, int groupSize, int expectStackSize,
                      int doAlignSum) {return 1;}
int fgpuSetupDoseWeighting(float *filter, int filtSize, float delta) {return 1;}
int fgpuAddToFullSum(float *fullArr, float shiftX, float shiftY) {return 1;}
int fgpuReturnSums(float *sumArr, float *evenArr, float *oddArr, int evenOddOnly) {return 1;}
int fgpuReturnUnweightedSum(float *sumArr) {return 1;}
void fgpuCleanup() {}
void fgpuRollAlignStack() {}
void fgpuRollGroupStack() {}
int fgpuSubtractAndFilterAlignSum(int stackInd, int groupRefine) {return 1;}
int fgpuNewFilterMask(float *alignMask) {return 1;}
int fgpuShiftAddToAlignSum(int stackInd, float shiftX, float shiftY, int shiftSource) {return 1;}
int fgpuCrossCorrelate(int aliInd, int refInd, float *subarea, int subXoffset,
                       int subYoffset) {return 1;}
int fgpuProcessAlignImage(float *binArr, int stackInd, int groupInd, 
                          int stackOnGpu) {return 1;}
void fgpuNumberOfAlignFFTs(int *numBinPad, int *numGroups) {}
int fgpuReturnAlignFFTs(float **saved, float **groups, float *alignSum, 
                        float *workArr) {return 1;}
int fgpuReturnStackedFrame(float *array, int *frameNum) {return 1;}
void fgpuCleanSumItems() {}
void fgpuCleanAlignItems() {}
void fgpuZeroTimers() {}
void fgpuPrintTimers() {}
int fgpuClearAlignSum() {return 1;}
int fgpuSumIntoGroup(int stackInd, int groupInd) {return 1;}
void fgpuSetGroupSize(int inVal) {}
int fgpuGetVersion() {return 1;}
void fgpuSetPrintFunc(CharArgType func) {}
