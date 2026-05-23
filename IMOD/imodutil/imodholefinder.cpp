/*
 *  imodholefinder.c -- Find regular holes in carbon film
 *
 *  Author: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 2020 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#include "b3dutil.h"
#include "cppdefs.h"
#include "imodel.h"
#include "iimage.h"
#include "sliceproc.h"
#include "parse_params.h"
#include "holefinder.h"

#define MAX_TRIALS 8

#define EXIT_IF_ERR(a) err = (a); \
  if (err > 0) exitError("%s", finder.returnErrorString(err));

static int sVerbose = 0;

class FindHoles
{
  public:
  FindHoles();

  void main( int argc, char *argv[]);
 private:
  float scaleSizeInput(float value);
  float mPixelScale;
};

// Main: just construct object and call its main
int main( int argc, char *argv[])
{
  FindHoles find;
  find.main(argc, argv);
  exit(0);
}

FindHoles::FindHoles()
{

}

void FindHoles::main(int argc,char *argv[])
{
  const char *progname = "imodholefinder";
  int numOptArgs, numNonOptArgs;
  HoleFinder finder;
  Imod *areaMod = NULL;
  MrcHeader inHead, montHead;
  Istore store;
  char *filename;
  char *modFile;
  Imod *outMod;
  Icont *cont;
  Iobj *obj;
  FILE *outfp, *summFP = NULL;
  FILE *inFp, *montFP = NULL, *listFP;
  const int maxLine = 120;
  char line[maxLine];
  void *buffer;
  FloatVec xBoundary, yBoundary, xCenters, yCenters, peakVals, xMissing, yMissing;
  FloatVec xCenClose, yCenClose, peakClose, meanVec, sdVec, outlieVec;
  FloatVec *intensityVec;
  FloatVec *xForObj[3] = {&xCenters, &xMissing, &xCenClose};
  FloatVec *yForObj[3] = {&yCenters, &yMissing, &yCenClose};
  FloatVec *peakForObj[3] = {&peakVals, NULL, &peakClose};
  FloatVec *objXptr, *objYptr, *objPeakPtr;
  ShortVec gridXvec, gridYvec;
  FloatVec valMin, valMax;
  float yscale, zscale, errorMaxOrig, reduction, bestRadius, extra;
  float bestDiam, trueSpacing, intensityDiam = 0., useIntensRad, statType = 0;
  int ncum, i, nxIn, nyIn, nzout, iz, err, ind, izInd, numWidths, dataSize, bufBytes;
  int numIncrem, numNums, numMissAdded, sliceMode, ob, coBase, numMedians, montBytes;
  int *zList;
  int numAreaCont, areaConts[4];
  float diamOrig, diamReduced, spacingOrig, spacingReduced, errorMax, xx, yy, maxRadius;
  float threshUsed, sigUsed, avgAngle = -999, avgLen = -1.;
  int numToGet, rawXsize, rawYsize, minXpiece, numXpieces, xOverlap, montYoffset;
  int minYpiece, numYpieces, yOverlap, xRawTotPix, yRawTotPix, montXoffset, ixpc, iypc;
  int numPcInSec, xCoord, yCoord, pie, fullBinning = 0, numFromCorr, indThresh, indSig;
  int retainDups = 0, showPiece = 0;
  float pieceXscale, pieceYscale, pieceZscale, foundSpacing, binFac;
  IntVec ixPiece, iyPiece, izPiece, ixAliPiece, iyAliPiece, secIxAliPiece, secIyAliPiece;
  IntVec pieceIndex, tileXnum, tileYnum, fileZindex, pieceOn;
  FloatVec xInPiece, yInPiece;
  FloatVecArray pieceXcenVec, pieceYcenVec, piecePeakVec, pieceMeanVec, pieceSDsVec;
  FloatVecArray pieceOutlieVec;
  float sigmas[MAX_TRIALS] = {1.5, 2., 3., -3.}, thresholds[MAX_TRIALS] = {2., 3.2, 4.4};
  float widths[MAX_TRIALS] = {4, 2, 1.5}, increments[MAX_TRIALS] = {3., 1.5, 1.};
  int numCircles[MAX_TRIALS] = {7, 3, 1};
  int numSigmas = 4;
  int numThresholds = 3;
  int useNumSigmas, useNumThresh, bestSigInd, bestThreshInd;
  float *useSigmas, *useThresholds;
  int numScans = 3;
  int debugImages = 0;

  // 1: Target hole diameter to reduce images to if holes are bigger than it
  float targetDiamPix = 50.;
  // 2: Fraction of nominal diameter to use as maximum error when analyzing grid, if
  // the maximum error is not entered
  float fractionalErrMax = 0.05;
  // 3: 0 use filter/threshold values from first section for the rest; 1 to use filter
  // value from first section and keep varying threshold; 2 to vary on all sections
  int varyForAllSecs = 2;
  // 4: Retain FFTs of circle and average templates when appropriate
  int retainFFTs = 1;

  // See documentation of these items in holefinder.cpp
  int minNumForTemplate = 5;
  float fractionToAverage = 0.5;
  float maxDiamErrFrac = 0.2;
  float avgOutlieCrit = 4.5;
  float finalPosOutlieCrit = 4.5;
  float finalNegOutlieCrit = 9;
  float finalOutlieRadFrac = 0.9;
  float pcToPcSameFrac = 0.5;
  float pcToFullSameFrac = 0.5;
  float substOverlapDistFrac = 0.;
  float usePieceEdgeDistFrac = 0.25;
  float addOverlapFrac = 0.75;

  // Fallbacks from    ../manpages/autodoc2man 2 1 imodholefinder
  int numOptions = 24;
  const char *options[] = {
    "input:InputImageFile:FN:", "output:OutputModelFile:FN:",
    "boundary:BoundaryModel:FN:", "summary:SummaryOutputFile:FN:",
    "diameter:DiameterOfHoles:F:", "spacing:SpacingOfHoles:F:", "error:MaximumError:F:",
    "thresh:ThresholdPercentiles:FA:", "filter:FilterSigmaOrIterations:FA:",
    "thicknesses:CircleThicknesses:FA:", "number:NumberOfCircles:IA:",
    "step:CircleStepSize:FA:", "sections:SectionsToDo:LI:",
    "intensity:DiamForIntensityStats:FP:", "montage:RawMontageFile:FN:",
    "piece:PieceListFile:FN:", "aligned:AlignedPieceList:FN:",
    "binned:FullImageIsBinned:I:", "retain:RetainDuplicates:B:",
    "show:ShowPieceObjects:B:", "control:ControlValue:FPM:", "verbose:VerboseOutput:I:",
    "debug:DebugImages:I:", "help:usage:B:"};

  /* Startup with fallback */
  PipReadOrParseOptions(argc, argv, options, numOptions, progname, 
                        3, 1, 1, &numOptArgs, &numNonOptArgs, imodUsageHeader);
  if (!PipGetBoolean("usage", &ind)) {
    PipPrintHelp(progname, 0, 1, 1);
    exit(0);
  }

  if (PipGetInOutFile("InputImageFile", 0, &filename))
    exitError("No input image file specified");
  inFp = iiFOpen(filename, "rb");
  if (!inFp)
    exitError("Opening input image file %s", filename);
  free(filename);
  if (mrc_head_read(inFp, &inHead))
    exitError("Reading header of image file %s", filename);

  mrc_get_scale(&inHead, &mPixelScale, &yscale, &zscale);
  
  // Check if it is the correct data type and set slice type
  sliceMode = sliceModeIfReal(inHead.mode);
  if (sliceMode < 0)
    exitError("File mode is %d; only byte, short, float allowed", inHead.mode);
  nxIn = inHead.nx;
  nyIn = inHead.ny;
  dataSizeForMode(sliceMode, &dataSize, &iz);
  bufBytes = dataSize * nxIn * nyIn;

  PipNumberOfEntries("ControlValue", &ncum);
  for (i = 0; i < ncum; i++) {
    PipGetTwoFloats("ControlValue", &xx, &yy);
    switch (B3DNINT(xx)) {
      SET_CONTROL_FLOAT(1, targetDiamPix);
      SET_CONTROL_FLOAT(2, fractionalErrMax);
      SET_CONTROL_INT(3, varyForAllSecs);
      SET_CONTROL_INT(4, retainFFTs);
      SET_CONTROL_INT(5, minNumForTemplate);
      SET_CONTROL_FLOAT(6, fractionToAverage);
      SET_CONTROL_FLOAT(7, maxDiamErrFrac);
      SET_CONTROL_FLOAT(8, avgOutlieCrit);
      SET_CONTROL_FLOAT(9, finalNegOutlieCrit);
      SET_CONTROL_FLOAT(10, finalPosOutlieCrit);
      SET_CONTROL_FLOAT(11, finalOutlieRadFrac);
      SET_CONTROL_FLOAT(12, pcToPcSameFrac);
      SET_CONTROL_FLOAT(13, pcToFullSameFrac);
      SET_CONTROL_FLOAT(14, substOverlapDistFrac);
      SET_CONTROL_FLOAT(15, usePieceEdgeDistFrac);
      SET_CONTROL_FLOAT(16, addOverlapFrac);
    default:
      break;
    }
  }

  // Read boundary model
  if (!PipGetString("BoundaryModel", &filename)) {
    areaMod = imodRead(filename);
    if (!areaMod)
      exitError("Reading boundary model %s", filename);
    free(filename);
    if (!areaMod->objsize || !areaMod->obj[0].contsize)
      exitError("No contours in object 1 of boundary model");
  }

  // Get output file
  if (PipGetInOutFile("OutputModelFile", 1, &modFile))
    exitError("No output model file specified");
  if (!PipGetString("SummaryOutputFile", &filename)) {
    summFP = fopen(filename, "w");
    if (!summFP)
      exitError("Opening summary output file %s", filename);
    free(filename);
  }

  if (PipGetFloat("DiameterOfHoles", &diamOrig))
    exitError("No hole diameter specified");
  if (PipGetFloat("SpacingOfHoles", &spacingOrig))
    exitError("No hole spacing specified");
  
  diamOrig = scaleSizeInput(diamOrig);
  spacingOrig = scaleSizeInput(spacingOrig);

  if (!PipGetFloat("MaximumError", &errorMaxOrig))
    fractionalErrMax = scaleSizeInput(errorMaxOrig) / diamOrig;
  errorMax = fractionalErrMax * diamOrig;
  if (!PipGetTwoFloats("DiamForIntensityStats", &intensityDiam, &statType)) {
    if (intensityDiam > 0)
      intensityDiam = scaleSizeInput(intensityDiam);
    intensityVec = &meanVec;
    if (statType)
      intensityVec = statType > 0 ? &sdVec : &outlieVec;
  }

  numSigmas = 0;
  if (PipGetFloatArray("FilterSigmaOrIterations", sigmas, &numSigmas, MAX_TRIALS))
    numSigmas = 3;
  numThresholds = 0;
  if (PipGetFloatArray("ThresholdPercentiles", thresholds, &numThresholds, MAX_TRIALS))
    numThresholds = 3;
  numWidths = numNums = numIncrem = 0;
  iz = PipGetFloatArray("CircleThicknesses", widths, &numWidths, MAX_TRIALS);
  ind = PipGetFloatArray("CircleStepSize", increments, &numIncrem, MAX_TRIALS);
  izInd = PipGetIntegerArray("NumberOfCircles", numCircles, &numNums, MAX_TRIALS);
  if (!iz || !ind || !izInd) {
    if (iz || ind || izInd) {
      if ((!iz && numWidths != numScans) || (!ind && numIncrem != numScans) || 
          (!izInd && numNums != numScans))
        exitError("If only one or two of -number, -step, and -thickness is entered, "
                  "%d values must be entered", numScans);
    } else {
      if (numWidths != numNums || numWidths != numIncrem)
        exitError("The same number of values must be entered for -number, -step, and"
                  " -thickness");
      numScans = numWidths;
    }
  }
  valMin.resize(3, 1.e30);
  valMax.resize(3, -1.e30);

  // RAW MONTAGE FILE WITH PIECES
  montBytes = bufBytes;
  if (!PipGetString("RawMontageFile", &filename)) {
    montFP = iiFOpen(filename, "rb");
    if (!montFP)
      exitError("Opening raw montage file %s", filename);
    if (mrc_head_read(montFP, &montHead))
      exitError("Reading header of montage file %s", filename);
    free(filename);
    numToGet = montHead.nz;
    ixPiece.resize(numToGet);
    iyPiece.resize(numToGet);
    izPiece.resize(numToGet);
    ixAliPiece.resize(numToGet);
    iyAliPiece.resize(numToGet);
    
    if (PipGetString("PieceListFile", &filename))
      exitError("A piece list file must be entered if a raw montage is");
    listFP = fopen(filename, "r");
    if (!listFP)
      exitError("Opening piece list file %s", filename);
    if (readLinesForValues(listFP, &numToGet, montHead.nz, line, maxLine, 
                           RLFV_SEPARATE_LINES, "iii", &ixPiece[0], &iyPiece[0],
                           &izPiece[0]))
      exitError("Reading lines from piece list file %s", filename);
    free(filename);
    fclose(listFP);
    if (PipGetString("AlignedPieceList", &filename)) {
      ixAliPiece = ixPiece;
      iyAliPiece = iyPiece;
    } else {
      listFP = fopen(filename, "r");
      if (!listFP)
        exitError("Opening aligned piece list file %s", filename);
      numToGet = montHead.nz;
      if (readLinesForValues(listFP, &numToGet, montHead.nz, line, maxLine, 
                             RLFV_SEPARATE_LINES, "iii", &ixAliPiece[0], &iyAliPiece[0],
                           &izPiece[0]))
        exitError("Reading lines from aligned piece list file %s", filename);
      free(filename);
      fclose(listFP);
    }
    rawXsize = montHead.nx;
    rawYsize = montHead.ny;
    dataSizeForMode(montHead.mode, &dataSize, &iz);
    montBytes = dataSize * rawXsize * rawYsize;
    if (checkPieceList(&ixPiece[0], 1, montHead.nz, 1, rawXsize, &minXpiece, &numXpieces,
                       &xOverlap) ||
        checkPieceList(&iyPiece[0], 1, montHead.nz, 1, rawYsize, &minYpiece, &numYpieces,
                       &yOverlap))
      exitError("Inconsistency in piece coordinates");
    
    
    // Get possible binning between blend and raw pieces
    if (PipGetInteger("FullImageIsBinned", &fullBinning)) {
      mrc_get_scale(&montHead, &pieceXscale, &pieceYscale, &pieceZscale);
      fullBinning = B3DNINT(mPixelScale / pieceXscale);
      if (fabs(fullBinning - mPixelScale / pieceXscale) > 0.01)
        exitError("Ratio of montage to full image file pixel scale is not close enough "
                  "to an integer; you must enter -binning");
    }
    PipGetBoolean("RetainDuplicates", &retainDups);
    PipGetBoolean("ShowPieceObjects", &showPiece);

    // Get full raw image size, offset from blended image, and adjust aligned coords
    xRawTotPix = numXpieces * (rawXsize - xOverlap) + xOverlap;
    yRawTotPix = numYpieces * (rawYsize - yOverlap) + yOverlap;
    montXoffset = (inHead.nx * fullBinning - xRawTotPix) / 2 - minXpiece;
    montYoffset = (inHead.ny * fullBinning - yRawTotPix) / 2 - minYpiece;
    for (ind = 0; ind < montHead.nz; ind++) {
      ixAliPiece[ind] += montXoffset;
      iyAliPiece[ind] += montYoffset;
    }
    pieceIndex.resize(numXpieces * numYpieces);
    valMin.resize(3 + numXpieces * numYpieces, 1.e30);
    valMax.resize(3 + numXpieces * numYpieces, -1.e30);
  }

  PipGetInteger("VerboseOutput", &sVerbose);
  finder.setVerbose(sVerbose > 0 ? sVerbose : 0);
  PipGetInteger("DebugImages", &debugImages);
  finder.setDebugImages(debugImages);

  if (!PipGetString("SectionsToDo", &filename)) {
    zList = parselist(filename, &nzout);
    if (!zList)
      exitError("Bad entry in list of sections to do");
    free(filename);
  } else {
    nzout = inHead.nz;
    zList = B3DMALLOC(int, nzout);
    if (zList)
      for (i = 0; i < nzout; i++)
        zList[i] = i;
  }
  if (!zList)
    exitError("Failed to get memory for list of sections");
  for (i = 0; i < nzout; i++)
    if (zList[i] < 0 || zList[i] >= inHead.nz)
      exitError("Section # %d is out of range", zList[i]);

  // Determine reduction 
  reduction = B3DMAX(1., diamOrig/ targetDiamPix);
  diamReduced = diamOrig / reduction;
  spacingReduced = spacingOrig / reduction;
  
  // get the maximum radius assuming worst-case expansion
  maxRadius = diamReduced / 2.;
  for (ind = 0; ind < numScans; ind++)
    maxRadius += (numCircles[ind] / 2.) * increments[ind] + widths[ind];

  buffer = (void *)malloc(B3DMAX(bufBytes, montBytes));
  if (!buffer)
    exitError("Allocating input image buffer");

  // Start the output model
  outMod = imodNew();
  if (!outMod)
    exitError("Allocating new model");
  if (imodNewObject(outMod) || imodNewObject(outMod) || imodNewObject(outMod))
    exitError("Creating new objects in model");
  imodObjectSetColor(&outMod->obj[0], 1., 0., 1.);
  imodObjectSetColor(&outMod->obj[1], 0., 1., 0.);
  imodObjectSetColor(&outMod->obj[2], 1., 1., 0.);

  useSigmas = sigmas;
  useNumSigmas = numSigmas;
  useThresholds = thresholds;
  useNumThresh = numThresholds;
  for (izInd = 0; izInd < nzout; izInd++) {
    iz = zList[izInd];

    // Get image
    if (mrc_read_slice(buffer, inFp, &inHead, iz, 'z'))
      exitError("Reading slice %d from file", iz);
    
    // Get boundary contour if any
    xBoundary.clear();
    yBoundary.clear();
    if (areaMod) {
      i = makeAreaContList(&areaMod->obj[0], iz, areaConts, &numAreaCont, 4);
      if (i < 0 || numAreaCont > 1)
        exitError("More than one contour on section %d in boundary model)", iz + 1);
      cont = &areaMod->obj[0].cont[areaConts[0]];
      if (B3DNINT(cont->pts[0].z) == iz) {
        for (ind = 0; ind < cont->psize; ind++) {
          xBoundary.push_back(cont->pts[ind].x);
          yBoundary.push_back(cont->pts[ind].y);
        }
      }
    }

    // Get arrays for pieces in raw montage
    if (montFP) {
      tileXnum.clear();
      tileYnum.clear();
      fileZindex.clear();
      secIxAliPiece.clear();
      secIyAliPiece.clear();
      numPcInSec = 0;
      for (iypc = 0; iypc < numYpieces; iypc++) {
        for (ixpc = 0; ixpc < numXpieces; ixpc++) { 
          pieceIndex[ixpc + iypc * numXpieces] = -1;
          xCoord = minXpiece + ixpc * (rawXsize - xOverlap);
          yCoord = minYpiece + iypc * (rawYsize - yOverlap);
          for (ind = 0; ind < montHead.nz; ind++) {
            if (iz == izPiece[ind] && xCoord == ixPiece[ind] && yCoord == iyPiece[ind]) {
              tileXnum.push_back(ixpc);
              tileYnum.push_back(iypc);
              fileZindex.push_back(ind);
              secIxAliPiece.push_back(ixAliPiece[ind]);
              secIyAliPiece.push_back(iyAliPiece[ind]);
              pieceIndex[ixpc + iypc * numXpieces] = numPcInSec++;
              break;
            }
          }
        }
      }
      CLEAR_RESIZE(pieceXcenVec, FloatVec, numPcInSec);
      pieceYcenVec.resize(numPcInSec);
      piecePeakVec.resize(numPcInSec);
      pieceMeanVec.resize(numPcInSec);
      pieceSDsVec.resize(numPcInSec);
      pieceOutlieVec.resize(numPcInSec);
    }

    numMedians = 0;
    for (ind = 0; ind < useNumSigmas; ind++)
      if (useSigmas[ind] < 0)
        numMedians++;

    // Initialize for image
    EXIT_IF_ERR(finder.initialize(buffer, sliceMode, nxIn, nyIn, reduction, maxRadius, 
                                  CACHE_KEEP_BOTH + 
                                  (numMedians > 1 ? CACHE_KEEP_MEDIAN : 0)));
    finder.setSequenceParams(diamReduced, spacingReduced, retainFFTs, errorMax,
                             fractionToAverage,  minNumForTemplate, maxDiamErrFrac,
                             avgOutlieCrit, finalNegOutlieCrit, finalPosOutlieCrit,
                             finalOutlieRadFrac);
    finder.setRunsInSequence(increments, widths, numCircles, numScans, useSigmas,
                             useNumSigmas, useThresholds, useNumThresh);

    // Run the sequence
    indSig = indThresh = 0;
    err = -1;
    while (err < 0) {
      EXIT_IF_ERR(finder.runSequence
                  (indSig, sigUsed, indThresh, threshUsed, xBoundary, yBoundary, 
                   bestSigInd, bestThreshInd, bestRadius, trueSpacing, xCenters, yCenters,
                   peakVals, xMissing, yMissing, xCenClose, yCenClose, peakClose,
                   numMissAdded));
      if ((numSigmas > 1 || numThresholds > 1) && (sVerbose || !err)) {
        if (sigUsed > 0)
          printf("%s sigma %.1f  threshold %.1f  found %4d  missing %3d\n", 
                 !err ? "best:" : "     ", sigUsed, threshUsed, (int)xCenters.size(), 
                 (int)xMissing.size());
        else
          printf("%s median %d  threshold %.1f  found %4d  missing %3d\n", 
                 !err ? "best:" : "     ", -B3DNINT(sigUsed), threshUsed, 
                 (int)xCenters.size(), (int)xMissing.size());
      }
    }

    bestDiam = bestRadius * 2. * reduction;
    foundSpacing = trueSpacing;
    if (mPixelScale != 1.) {
      bestDiam *= mPixelScale / 1.e4;
      foundSpacing *= mPixelScale / 1.e4;
    }

    // On the first section, take the best sigma and stop varying it unless flag is 2
    // And take the best threshold and stop varying unless that flag is non-zero
    if (!iz && varyForAllSecs < 2 && numSigmas > 1) {
      useNumSigmas = 1;
      useSigmas = &sigmas[bestSigInd];
    }
    if (!iz && !varyForAllSecs && numThresholds > 1) {
      useNumThresh = 1;
      useThresholds = &thresholds[bestThreshInd];
    }

    if (intensityDiam > 0) 
      useIntensRad = 0.5 * intensityDiam / reduction;
    else if (intensityDiam < 0 || summFP)
      useIntensRad = bestRadius - errorMax / reduction;

    // Get stats as needed
    if (intensityDiam || summFP) {
      EXIT_IF_ERR(finder.holeIntensityStats
                  (useIntensRad, *xForObj[0], *yForObj[0], false, true, 
                   (summFP || statType == 0.) ? &meanVec : NULL,
                   (summFP || statType > 0.) ? &sdVec : NULL,
                   (summFP || statType < 0.) ? &outlieVec : NULL));
    }
    
    // Process montage pieces: set up, then loop on pieces
    if (montFP) {
      finder.setMontPieceVectors
        (&pieceXcenVec, &pieceYcenVec, &piecePeakVec,
         (summFP || statType == 0.) ? &pieceMeanVec : NULL, 
         (summFP || statType > 0.) ? &pieceSDsVec : NULL,
         (summFP || statType < 0.) ? &pieceOutlieVec : NULL, &secIxAliPiece, 
         &secIyAliPiece, &pieceIndex, &tileXnum, &tileYnum);
      finder.setMontageParams
        (numXpieces, numYpieces,rawXsize, rawYsize, fullBinning, pcToPcSameFrac,
         pcToFullSameFrac, substOverlapDistFrac, usePieceEdgeDistFrac, addOverlapFrac);
      for (pie = 0; pie < numPcInSec; pie++) {
        if (mrc_read_slice(buffer, montFP, &montHead, fileZindex[pie], 'z'))
          exitError("Reading slice %d from raw montage file", fileZindex[pie]);
        EXIT_IF_ERR(finder.initialize(buffer, montHead.mode, rawXsize, rawYsize, 
                                      fullBinning * reduction, bestRadius + 4., 
                                      CACHE_KEEP_BOTH + CACHE_KEEP_AVGS));
        EXIT_IF_ERR(finder.processMontagePiece
                    (sigmas[bestSigInd], thresholds[bestThreshInd], 
                     trueSpacing / reduction, pie, xBoundary, yBoundary, 
                     (intensityDiam || summFP) ? useIntensRad : 0., numFromCorr));
        if (sVerbose < 0)
          printf("     Piece %d %d: %d from templates, %d in grid\n", tileXnum[pie],
                 tileYnum[pie], numFromCorr, (int)pieceXcenVec[pie].size());

      }

      // USE the positions to improve the centers arrays
      finder.resolvePiecePositions
        (!retainDups, xCenters, yCenters, peakVals, xMissing,
         yMissing, xCenClose, yCenClose, peakClose, pieceOn, xInPiece, yInPiece,
         (summFP || statType == 0.) ? &meanVec : NULL,
         (summFP || statType > 0.) ? &sdVec : NULL,
         (summFP || statType < 0.) ? &outlieVec : NULL);
    }

    printf("iz = %3d  diam %.2f  spacing %.2f  found %3d  missing %d (%d close)\n",
           iz, bestDiam, foundSpacing, (int)xCenters.size(),
           (int)xMissing.size(), (int)xCenClose.size());

    // If outputting summary file, get the grid positions and write everything
    if (summFP) {
      finder.assignGridPositions(xCenters, yCenters, gridXvec, gridYvec, avgAngle, 
                                 avgLen);
      for (ind = 0; ind < xCenters.size(); ind++) {
        fprintf(summFP, "%7.1f %7.1f %3d  %10.6g %10.6g %10.6g %.4f %3d %3d", 
                xCenters[ind], yCenters[ind], iz, peakVals[ind], meanVec[ind], sdVec[ind],
                outlieVec[ind], (int)gridXvec[ind], (int)gridYvec[ind]);
        if (montFP)
          fprintf(summFP, " %3d %6.1f %6.1f", pieceOn[ind], xInPiece[ind], yInPiece[ind]);
        fprintf(summFP, "\n");
      }
    }
    
    // Store points in model
    store.flags = GEN_STORE_FLOAT << 2;
    store.type = GEN_STORE_VALUE1;
    for (ob = 0; ob < 3 + (montFP ? numPcInSec : 0); ob++) {
      if (intensityDiam) {
        if (ob > 0 && ob < 2) {
          EXIT_IF_ERR(finder.holeIntensityStats
                      (useIntensRad, *xForObj[ob], *yForObj[ob], false,
                       true, statType == 0. ? &meanVec : NULL,
                       statType > 0. ? &sdVec : NULL,
                       statType < 0. ? &outlieVec : NULL));
        }
        peakForObj[ob] = intensityVec;
      }
      if (ob < 3) {
        
        // Set up to add to regular object
        objXptr = xForObj[ob];
        objYptr = yForObj[ob];
        objPeakPtr = peakForObj[ob];
        binFac = 1.;
        if (showPiece)
          outMod->obj[ob].flags |= IMOD_OBJFLAG_OFF;
      } else {

        // Set up to add to piece object
        if (ob >= outMod->objsize && imodNewObject(outMod))
          exitError("Creating new object %d in model", ob + 1);
 
        pie = ob - 3;
        objXptr = &pieceXcenVec[pie];
        objYptr = &pieceYcenVec[pie];
        objPeakPtr = &piecePeakVec[pie];
        if (intensityDiam) {
          if (statType == 0.)
            objPeakPtr = &pieceMeanVec[pie];
          else
            objPeakPtr = (statType > 0. ? &pieceSDsVec[pie] : &pieceOutlieVec[pie]);
        }
        binFac = fullBinning;
        if (!showPiece)
          outMod->obj[ob].flags |= IMOD_OBJFLAG_OFF;
        if (ob == 3)
          imodObjectSetColor(&outMod->obj[ob], 0., 1., 1.);
      }
      if (objXptr->size()) {

        // Make a contour for each point and add the points
        obj = &outMod->obj[ob];
        cont = imodContoursNew(objXptr->size());
        if (!cont)
          exitError("Allocating new contours");
        coBase = obj->contsize;
        for (ind = 0; ind < objXptr->size(); ind++) {
          if (!imodPointAppendXYZ(&cont[ind], (*objXptr)[ind] / binFac, 
                                  (*objYptr)[ind] / binFac, iz))
            exitError("Adding point to contour");

          // Add chosen value
          if (objPeakPtr) {
            store.value.f = (*objPeakPtr)[ind];
            store.index.i = ind + coBase;
            if (istoreInsert(&obj->store, &store))
              exitError("Could not add general storage item");
            ACCUM_MIN(valMin[ob], store.value.f);
            ACCUM_MAX(valMax[ob], store.value.f);
          }
          if (imodObjectAddContour(obj, &cont[ind]) < 0)
            exitError("Adding contour to object");
        }

        // Set flags and get rid of conts
        obj->flags |= IMOD_OBJFLAG_OPEN;
        if (objPeakPtr) {
          obj->valwhite = 255;
          obj->valblack = 0;
          obj->flags |= IMOD_OBJFLAG_USE_VALUE;
          obj->matflags2 |= MATFLAGS2_CONSTANT | MATFLAGS2_SKIP_LOW;
        }
        obj->symbol = IOBJ_SYM_CIRCLE;
        obj->symsize = 7;
        obj->linewidth2 = 2;
        free(cont);
      }
    }
  }
  imodSetIndex(outMod, 0, -1, -1);
  for (ob = 0; ob < outMod->objsize; ob++) {
    if (outMod->obj[ob].contsize) {
      extra = (valMax[ob] - valMin[ob]) / 254.;
      if (istoreAddMinMax(&outMod->obj[ob].store, GEN_STORE_MINMAX1, valMin[ob] - extra, 
                          valMax[ob] + extra))
        exitError("Could not add general storage item");
    }
  }
    
  iiFClose(inFp);
  if (montFP)
    iiFClose(montFP);
  if (summFP)
    fclose(summFP);

  // Finish model
  outMod->xmax = nxIn;
  outMod->ymax = nyIn;
  outMod->zmax = inHead.nz;
  if (mPixelScale != 1.) {
    outMod->pixsize = mPixelScale / 10.f;
    outMod->units = IMOD_UNIT_NM;
  }
  
  imodSetRefImage(outMod, &inHead);
  imodBackupFile(modFile);
  imodFileWrite(outMod, modFile);

  // Clean up for the leak-checker!
  free(buffer);
  imodDelete(outMod);
  imodDelete(areaMod);
}


float FindHoles::scaleSizeInput(float value)
{
  if (value < 0)
    return -value;
  if (mPixelScale == 1.)
    return value;
  return value * 1.e4 / mPixelScale;
}
