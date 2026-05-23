/*
* main.cpp - main function of ctfplotter, a GUI program for estimating the first
*            CTF zero of a tilt series.
*
*  Authors: Quanren Xiong and David Mastronarde
*
*  Copyright (C) 2008-2019 by the Regents of the University of
*  Colorado.  See dist/COPYRIGHT for full copyright notice.
*
*  $Id$
*/

#include <math.h>
#include <stdio.h>
#include <locale.h>
#include "myapp.h"
#include "angledialog.h"
#include "fittingdialog.h"

#include "simplexfitting.h"
#include "linearfitting.h"
#include "plotter.h"
#include "imod_assistant.h"

#include "b3dutil.h"
#include "mrcfiles.h"
#include "mrcslice.h"
#include "cfft.h"
#include "parse_params.h"
#include "dia_qtutils.h"

#define CACHESIZE 1000   // max cache size in Megs;
#define MIN_ANGLE 1.0e-6 //tilt Angle less than this is treated as 0.0;
ImodAssistant *ctfHelp = NULL;
int debugLevel;

int main(int argc, char *argv[])
{
  // Fallbacks from   ../manpages/autodoc2man 2 1 ctfplotter
  int numOptArgs, numNonOptArgs;
  int numOptions = 50;
  const char *options[] = {
    "input:InputStack:FN:", "offset:OffsetToAdd:F:", "config:ConfigFile:FN:",
    "angleFn:AngleFile:FN:", "invert:InvertTiltAngles:B:", "aAngle:AxisAngle:F:",
    "defFn:DefocusFile:FN:", "pixelSize:PixelSize:F:", "crop:CropToPixelSize:F:",
    "fitZeros:FitZerosForPhase:B:", "volt:Voltage:I:", "cs:SphericalAberration:F:",
    "ampContrast:AmplitudeContrast:F:", "phase:PhasePlateShift:F:",
    "degPhase:PhaseShiftInDegrees:F:", "phRange:PhaseRangeToSearch:F:",
    "cuton:CutOnFrequency:F:", "maxCuton:MaxCutOnToSearch:F:", "skip:ViewsToSkip:LI:",
    "bidir:BidirectionalNumViews:LI:", "onlyAP:SkipOnlyForAstigPhase:B:",
    "expDef:ExpectedDefocus:F:", "fpOffset:FocalPairDefocusOffset:F:",
    "range:AngleRange:FP:", "autoFit:AutoFitRangeAndStep:FP:",
    "useDef:UseExpectedDefForAuto:B:", "frequency:FrequencyRangeToFit:FP:",
    "extra:ExtraZerosToFit:F:", "vary:VaryExponentInFit:B:",
    "baseline:BaselineFittingOrder:I:", "find:FubdAstigPhaseCuton:IT:",
    "sAstig:SearchAstigmatism:B:", "sPhase:SearchPhaseShift:B:",
    "sCuton:SearchCutonFrequency:B:", "minViews:MinViewsAstigAndPhase:IP:",
    "save:SaveAndExit:B:", "psRes:PSResolution:I:", "tileSize:TileSize:I:",
    "defTol:DefocusTol:I:", "leftTol:LeftDefTol:F:", "rightTol:RightDefTol:F:",
    "cache:MaxCacheSize :I:", "hyper:HyperResolutionFactor:I:",
    "sectors:NumberOfSectors:I:", "wedge:WedgeRangeAndInterval:FP:",
    "astigMax:MaximumAstigmatism:F:", "colors:ChangeColors:B:", "debug:DebugLevel:I:",
    "param:ParameterFile:PF:", "help:usage:B:"};

  bool focalPairProcessing = false;
  bool noNoiseFiles, baseOrderEntered;
  char *configName = NULL, *stackFn, *angleFn, *defFn, *skipStr;
  double *noisePs = NULL;
  double *noiseMean = NULL;
  int *index = NULL;
  float tiltAxisAngle;
  int volt, nDim, tileSize, cacheSize, ifAutofit, ifAngleRange, ifFreqRange, ierr, ierr2;
  float defocusTol, pixelSize, lowAngle, highAngle, autoRange, autoStep;
  float autoFromAngle, autoToAngle, x1Start, x2End, phaseDeg, maxCuton = 0.;
  float maxAstig = 1.2;
  float expectedDef, fpdz, leftDefTol, rightDefTol, phaseShift, cutOnFreq;
  float ampContrast, cs, dataOffset = 0.0, extraZerosFit = 0.0, phaseRange = 120.;
  int invertAngles = 0, ifOffset = 0, saveAndExit = 0, varyExp = 0, baselineOrder = 4;
  float wedgeRange = 90., wedgeInterval = 0., cropPixel = 0.;
  int findPhase = 0, findAstig = 0, findCuton = 0, minAstigViews = 5, minPhaseViews = 1;
  int fitZeros = 0, numBidirViews = 0, skipOnlyAP = 0, numSkip = 0, changeColors = 0;
  int autoWithExpected = 0;
  int *skipList = NULL;
  QString noiseCfgDir, noiseFile;
  SliceCache *cache, *cache2;
  MrcHeader *header;
  MrcHeader noiseHead;
  QApplication *qapp = NULL;
  QMainWindow *mainWin = NULL;
  Plotter *plotter = NULL;
  QLabel *label;
  QWidget *splash;
  char *progname = imodProgName(argv[0]);

  // Set some sensible defaults
  defocusTol = 200;
  leftDefTol = rightDefTol = 2000;
  ampContrast = 0.07;
  nDim = 101;
  tileSize = 256;
  phaseShift = 0.;
  cutOnFreq = 0.;

  // These are used for a pointless initial fit if doing auto and no range entered
  lowAngle = -10.;
  highAngle = 10.;

  // This amount of hyperresolution in the stored PS gave < 0.1% difference
  // in the final PS from ones computed directly from summed FFTs for
  // frequencies higher than 0.2.
  int hyperRes = 20;
  int numSectors = 36;

  PipReadOrParseOptions(argc, argv, options, numOptions, "ctfplotter",
                        1, 0, 0, &numOptArgs, &numNonOptArgs, NULL);

  if (!PipGetBoolean("usage", &ierr)) {
    PipPrintHelp(progname, 0, 0, 0);
    exit(0);
  }
  if (PipGetInteger("DebugLevel", &debugLevel))
    debugLevel = 1;

  PipGetInteger("HyperResolutionFactor", &hyperRes);
  if (hyperRes > 40 || hyperRes < 2)
    exitError("Hyperresolution factor must be between 2 and 40");
  PipGetInteger("NumberOfSectors", &numSectors);
  if (numSectors < 12 || numSectors > 45)
    exitError("Number of sectors must be between 12 and 45");
  PipGetTwoFloats("WedgeRangeAndInterval", &wedgeRange, &wedgeInterval);
  if (wedgeRange > 120.)
    exitError("Wedge angle range must be no more than 120 degrees");

  noNoiseFiles = PipGetString("ConfigFile", &configName) != 0;
  if (PipGetString("InputStack", &stackFn))
    exitError("No stack specified");
  if (PipGetString("AngleFile", &angleFn)) {
    angleFn = NULL;
    if (debugLevel >= 1)
      printf("No angle file is specified, tilt angle is assumed to be 0.0\n");
  }
  if (PipGetString("DefocusFile", &defFn))
    exitError("output defocus file is not specified ");
  if (PipGetInteger("Voltage", &volt))
    exitError("Voltage is not specified");
  if (PipGetInteger("MaxCacheSize", &cacheSize)) {
    double physical = b3dPhysicalMemory();
    if (physical) {

      // If memory available, do it the same as newstack: the min of 15 GB, 3/4 of
      // physical, and physical  minus 1 GB, but at least 0.4 GB when memory is up to
      // 30 GB, and 1/2 of memory above that
      physical /= 1024. * 1024.;
      if (physical < 30000) {
        cacheSize = B3DMIN(0.75 * physical, physical - 1000.);
        B3DCLAMP(cacheSize, 400., 15000.);
      } else
        cacheSize = physical / 2.;
    } else
      cacheSize = CACHESIZE;
    if (debugLevel >= 1)
      printf("No MaxCacheSize is specified, set to default %d Megs\n", cacheSize);

  } else {
    if (debugLevel >= 1)
      printf(" MaxCacheSize is set to %d \n", cacheSize);
  }
  if (PipGetFloat("SphericalAberration", &cs))
    exitError("Spherical Aberration is not specified");
  cs = B3DMAX(0.01, cs);
  PipGetInteger("PSResolution", &nDim);
  PipGetInteger("TileSize", &tileSize);
  PipGetFloat("DefocusTol", &defocusTol);
  if (PipGetFloat("PixelSize", &pixelSize))
    exitError("No PixelSize specified");
  PipGetFloat("AmplitudeContrast", &ampContrast);
  ierr = PipGetFloat("PhasePlateShift", &phaseShift);
  if (!PipGetFloat("PhaseShiftInDegrees", &phaseDeg)) {
    if (!ierr)
      exitError("you cannot enter phase shift in both degrees and radians");
    phaseShift = phaseDeg * RADIANS_PER_DEGREE;
  }
  PipGetFloat("PhaseRangeToSearch", &phaseRange);
  PipGetFloat("CutOnFrequency", &cutOnFreq);
  PipGetFloat("MaxCutOnToSearch", &maxCuton);
  PipGetFloat("MaximumAstigmatism", &maxAstig);
  PipGetInteger("BidirectionalNumViews", &numBidirViews);
  if (PipGetFloat("AxisAngle", &tiltAxisAngle))
    exitError("No AxisAngle specified");
  if (PipGetFloat("ExpectedDefocus", &expectedDef))
    exitError("No expected defocus is specified");
  PipGetFloat("LeftDefTol", &leftDefTol);
  PipGetFloat("RightDefTol", &rightDefTol);
  ifAutofit = 1 - PipGetTwoFloats("AutoFitRangeAndStep", &autoRange, &autoStep);
  if (ifAutofit) {
    ifAngleRange = 1 - PipGetTwoFloats("AngleRange", &autoFromAngle, &autoToAngle);
  } else {
    if (PipGetTwoFloats("AngleRange", &lowAngle, &highAngle))
      exitError("No AngleRange specified");
    autoStep = (float)fabs((double)highAngle - lowAngle) / 2.f;
  }
  PipGetBoolean("UseExpectedDefForAuto", &autoWithExpected);
  ierr = PipGetThreeIntegers("FindAstigPhaseCuton", &findAstig, &findPhase, &findCuton);
  if (PipGetBoolean("SearchAstigmatism", &findAstig) == 0 && !ierr)
    exitError("You cannot enter -find with -sAstig");
  if (PipGetBoolean("SearchPhaseShift", &findPhase) == 0 && !ierr)
    exitError("You cannot enter -find with -sPhase");
  if (PipGetBoolean("SearchCutonFrequency", &findCuton) == 0 && !ierr)
    exitError("You cannot enter -find with -sCuton");
  PipGetTwoIntegers("MinViewsAstigAndPhase", &minAstigViews, &minPhaseViews);
  PipGetInteger("FitZerosForPhase", &fitZeros);
  PipGetInteger("SkipOnlyForAstigPhase", &skipOnlyAP);
  if (!PipGetString("ViewsToSkip", &skipStr)) {
    skipList = parselist(skipStr, &numSkip);
    if (!skipList || !numSkip)
      exitError("Parsing list of views to skip");
    free(skipStr);
  }
  PipGetFloat("CropToPixelSize", &cropPixel);
  PipGetBoolean("ChangeColors", &changeColors);

  PipGetBoolean("InvertTiltAngles", &invertAngles);
  ifOffset = 1 - PipGetFloat("OffsetToAdd", &dataOffset);
  ifFreqRange = 1 - PipGetTwoFloats("FrequencyRangeToFit", &x1Start, &x2End);
  if (ifFreqRange && (x1Start < 0.01 || x2End > 0.48 || x2End - x1Start < 0.03))
    exitError("Fitting range values are too extreme, out of order, or too close together"
             );
  if (ifAutofit)
    PipGetBoolean("SaveAndExit", &saveAndExit);
  PipGetBoolean("VaryExponentInFit", &varyExp);
  if (!PipGetFloat("FocalPairDefocusOffset", &fpdz)) {
    focalPairProcessing = true;
    printf("Fitting 2 tilt series with a defocus offset of %g nm\n", fpdz);
    if (noNoiseFiles)
      exitError("No noise configuration file specified; it is required for focal pairs");
  }
  baseOrderEntered = PipGetInteger("BaselineFittingOrder", &baselineOrder) == 0;
  B3DCLAMP(baselineOrder, 0, 4);
  if (!PipGetFloat("ExtraZerosToFit", &extraZerosFit) && ifFreqRange)
    exitError("You cannot specify both a frequency range and extra zeros to fit");
  if (extraZerosFit < -0.5)
    exitError("Extra zeros to fit must be no less than -0.5");

  double *rAvg = (double *)malloc(nDim * sizeof(double));
  if (!rAvg)
    exitError("Allocation failed: out of memory!");
  double *rAvg1 = NULL, *rAvg2 = NULL;
  if (focalPairProcessing) {
    rAvg1 = (double *)malloc(nDim * sizeof(double));
    rAvg2 = (double *)malloc(nDim * sizeof(double));
    if (!rAvg1 || !rAvg2)
      exitError("Allocation failed: out of memory!");
  }

  // Instantiate the QApplication and Qt UI elements only if not doing autofit and exiting
  // so that the program can run without a window system available
  diaSetQtLibraryPath();
  if (!saveAndExit) {
    diaHandleFontAndDPI(true);

    ctfHelp = new ImodAssistant("html", "IMOD.adp", "ctfguide");
    qapp = new QApplication(argc, argv);
    float devicePixelRatio, maxDPR;
    devicePixelRatio = diaGetAppDevPixRatioInfo(maxDPR, ierr);
    diaAdjustFontForDPI(devicePixelRatio, false);

  }
  if (noNoiseFiles && !baseOrderEntered)
    baselineOrder = 4;
  MyApp app(volt, pixelSize, (double)ampContrast, cs, defFn,
            (int)nDim, hyperRes, (double)defocusTol, tileSize,
            (double)tiltAxisAngle, -90.0, 90.0, (double)expectedDef,
            (double)leftDefTol, (double)rightDefTol, cacheSize, invertAngles,
            focalPairProcessing, (double)fpdz, baselineOrder, noNoiseFiles, numSectors,
            wedgeRange, wedgeInterval);
  setlocale(LC_NUMERIC, "C");

  // Set some variables
  app.setPS(rAvg, rAvg1, rAvg2);
  app.setSaveAndExit(saveAndExit != 0);
  app.setVaryCtfPowerInFit(varyExp != 0);
  if (app.mDefocusFinder.getPlatePhase() == 0.)
    app.mDefocusFinder.setPlatePhase(phaseShift);
  if (app.mDefocusFinder.getCutOnFreq() == 0.)
    app.mDefocusFinder.setCutOnFreq(cutOnFreq);
  app.setMaxCutOnFreq(app.mDefocusFinder.getAnyOneZero(expectedDef / 1000., 1) /
                      (2. * pixelSize));
  app.setFindAstigmatism(findAstig != 0, true);
  app.setMaxAstigmatism(maxAstig);
  app.setFindPhaseShift(findPhase != 0);
  app.setFindCutOnFreq(findCuton != 0);
  if (maxCuton > 0.)
    app.setMaxCutOnFreq(maxCuton);
  if (phaseRange > 0.)
    app.setPhaseSearchRange(phaseRange * RADIANS_PER_DEGREE);
  if (app.anyAstigmatismSaved(ierr, ierr2) > 1 || findAstig)
    app.setAstigParamsOpen(true);
  if (findPhase || findCuton || ierr > 2)
    app.setPhaseParamsOpen(true);
  app.setFindAndFitZeros(fitZeros != 0);
  app.setSkipOnlyForAstig(skipOnlyAP != 0);
  if (skipList)
    app.setViewSkipList(skipList, numSkip);
  app.setChangeColors(changeColors != 0);
  app.setNumZerosToFit(2. + extraZerosFit);

  // Create UI elements if appropriate
  if (!saveAndExit) {
    mainWin = new QMainWindow();
    plotter = new Plotter(&app, mainWin);
    plotter->setWindowTitle(QObject::tr("CTF Plot"));
    app.setPlotter(plotter);
  }

  /*****begin of computing noise PS;**********/
    if (!saveAndExit) {

      // It could be just a label, but this makes it bigger and lets the whole
      // title show up
      splash = new QWidget();
      QVBoxLayout *layout = new QVBoxLayout;
      splash->setLayout(layout);
      layout->setMargin(30);
      label = new QLabel(noNoiseFiles ? "Loading slices ..." : "Loading noise files ...", 
                         splash);
      layout->addWidget(label);
      splash->setWindowTitle("ctfplotter");
      QSize hint = splash->sizeHint();
      splash->move(QApplication::desktop()->width() / 2 - hint.width() / 2,
                   QApplication::desktop()->height() / 2 - hint.height() / 2);
      splash->show();

      // At least 4 of these are needed to make it work reliably!
      qApp->flush();
#if QT_VERSION < 0x050000
      qApp->syncX();
#endif
      qApp->processEvents();
      splash->repaint(0, 0, -1, -1);
#ifdef Q_OS_MACX  
      splash->raise();
#endif
      qApp->processEvents();
    }

  if (!noNoiseFiles) {
    FILE *fpConfig;
    if ((fpConfig = fopen(configName, "rb")) == 0)
      exitError(" could not open config file %s", configName);
    char p[1024];
    int read, numLoop = 0, loop;
    int noiseFileCounter = 0;

    b3dSetStoreError(1);
    bool noiseStack = !mrc_head_read(fpConfig, &noiseHead);
    fclose(fpConfig);
    b3dSetStoreError(0);
    if (noiseStack) {
      noiseFileCounter = numLoop = noiseHead.nz;
      app.setSlice(configName, NULL, SLICE_CACHE_PRIMARY, true);
    } else {
      if ((fpConfig = fopen(configName, "r")) == 0)
        exitError(" could not reopen config file %s", configName);

    // only to find how many noise files are provided;
      while ((read = fgetline(fpConfig, p, 1024)) >= 0) {
        numLoop++;
        if (read)
          noiseFileCounter++;
      }
      rewind(fpConfig);
    }
    if (debugLevel >= 1)
      printf("There are %d noise %s specified\n", noiseFileCounter, 
             noiseStack ? "files" : "images");
    if (noiseFileCounter < 2)
      exitError("There must be at least two noise images");

    noisePs = B3DMALLOC(double, noiseFileCounter * nDim);
    noiseMean = B3DMALLOC(double, noiseFileCounter * nDim);
    index = B3DMALLOC(int, noiseFileCounter);
    double *currPS;
    int i, j;

    fflush(stdout);

    if (!noiseStack) {
      noiseCfgDir = QDir::fromNativeSeparators(QString(configName));
      printf("noiseCfgDir read in: %s\n", (const char *)noiseCfgDir.toLatin1());
      i = noiseCfgDir.lastIndexOf('/');
      if (i >= 0)
        noiseCfgDir = noiseCfgDir.left(i + 1);
      else
        noiseCfgDir = "./";
      printf("i  %d noiseCfgDir used: %s\n", i, (const char *)noiseCfgDir.toLatin1());
    }
    noiseFileCounter = 0;
    for (loop = 0; loop < numLoop; loop++) {
      if (!noiseStack) {
        read = fgetline(fpConfig, p, 1024);
        if (read < 0)
          break;
        if (!read)
          continue;
        //if(p[read-1]=='\n') p[read-1]='\0';  //remove '\n' at the end;
        noiseFile = QString(p);
        if (QDir::isRelativePath(noiseFile))
          noiseFile = noiseCfgDir + noiseFile;
        app.setSlice((const char *)noiseFile.toLatin1(), NULL, SLICE_CACHE_PRIMARY, true);
      } else {
        app.getCache()->setOneNeededSlice(noiseFileCounter);
      }
      app.computeInitPS(true);
      double meanPower = 0.;

      // Smooth the noise PS
      if (app.smoothNoisePS())
        printf("WARNING: Computational error smoothing noise power spectrum %d\n", 
               noiseFileCounter + 1);
      //for(i=0;i<nDim;i++) noisePs[noiseFileCounter][i]=*(currPS+i);
      currPS = app.getPS();
      for (i = 0; i < nDim; i++) {
        noisePs[noiseFileCounter * nDim + i] = currPS[i];
        if (i)
          meanPower += currPS[i] / (nDim - 1.);
      }
      noiseMean[noiseFileCounter] = app.getStackMean();
      if (noiseMean[noiseFileCounter] < 0.)
        exitError("The mean of noise file %d is negative.  All noise files must have "
                  "positive means, with 0 mean corresponding to 0 exposure",
                  noiseFileCounter + 1);
      if (debugLevel >= 1)
        printf("noiseMean[%d]=%f   mean power=%g\n", noiseFileCounter, 
               noiseMean[noiseFileCounter], meanPower);
      index[noiseFileCounter] = noiseFileCounter;
      noiseFileCounter++;
    }
    fflush(stdout);

    //sorting;
    double tempMean;
    double tempIndex;
    for (i = 0; i < noiseFileCounter; i++)
      for (j = i + 1; j < noiseFileCounter; j++)
        if (noiseMean[i] > noiseMean[j]) {
          tempMean = noiseMean[i];
          noiseMean[i] = noiseMean[j];
          noiseMean[j] = tempMean;

          tempIndex = index[i];
          index[i] = index[j];
          index[j] = tempIndex;
        }
    app.setNumNoiseFiles(noiseFileCounter);
    app.setNoiseIndexes(index);
    app.setNoiseMeans(noiseMean);
    app.setAllNoisePS(noisePs);
    /****end of computing noise PS; otherwise set up for two-line baseline ******/
  } else {
    app.setDoTwoLineBaselineFit(true, false);
    //app.switchToOrFromCtffind();
  }

  if (!saveAndExit) {
    label->setText("Loading slices ...");
    qApp->processEvents();
  }

  // If doing auto without save and exit and the table is non-empty and there is an
  // an angle range, then suppress the autofit, assuming it happened previously
  if (ifAutofit && !saveAndExit && ilistSize(app.getSavedList()) > 0 && ifAngleRange) {
    lowAngle = autoFromAngle;
    highAngle = autoToAngle;
    ifAutofit = 0;
  }

  app.setAutoStepAngle(ifAutofit ? B3DMAX((angleFn ? 0.1 : 0.002), autoStep) : 0.);
  app.setAutoStepRange((ifAutofit && !autoStep) ? 0. : autoRange);
  app.setLowAngle(lowAngle);
  app.setHighAngle(highAngle);

  // Need to set to use expected values for autofit...
  // But if cropping pixel, there will be a possibly bad first fit, so defer it
  if (ifAutofit && !autoWithExpected && cropPixel <= pixelSize) {
    app.setUseCurDefocus(1);
    app.setUseCurrentPhase(true);
    app.mDefocusFinder.setDefocus(app.mDefocusFinder.getExpDefocus());
  }

  if (focalPairProcessing) {
    // When processing focal pairs, instead of (e.g) myData.st the two
    // stacks will be myData_1.st (at nominal defocus) and myData_2.st
    std::string stackFn1(stackFn), stackFn2(stackFn);
    size_t underpos = stackFn1.find_last_of('_');
    size_t dotpos = stackFn1.find_last_of('.');
    if (underpos == std::string::npos || 
        stackFn1[underpos + 1] != '1' ||
        dotpos != underpos + 2) 
      exitError("Input base name must end in _1, and suffix may not contain '_'.");
    stackFn2.replace(underpos, 2, "_2");
    app.setSlice(stackFn1.c_str(), angleFn, SLICE_CACHE_PRIMARY, false);
    app.setSlice(stackFn2.c_str(), angleFn, SLICE_CACHE_SECONDARY, false);
  }
  else
    app.setSlice(stackFn, angleFn, SLICE_CACHE_PRIMARY, false);

  cache = app.getCache();
  header = cache->getHeader();
  B3DCLAMP(minAstigViews, 1, header->nz);
  B3DCLAMP(minPhaseViews, 1, header->nz);
  app.setMinViewsForAstig(minAstigViews, true);
  app.setMinViewsForPhase(minPhaseViews, true);
  if (numBidirViews > 0) {
    B3DCLAMP(numBidirViews, 2, header->nz - 1);
    app.setBidirectionalView(numBidirViews);
    app.setBreakAtBidirView(true);
  }

  // If doing auto and range was entered, now fix the auto from and to angles
  if (ifAutofit && ifAngleRange) {
    app.setAutoFromAngle((double)autoFromAngle);
    app.setAutoToAngle((double)autoToAngle);
  }

  // Now determine and set data offset if necessary
  if (!ifOffset) {
    if (strstr(header->labels[0], "Fei Company")) {
      if (header->amin < 0.) {
        ifOffset = 1;
      } else {
        const char title[] = "WARNING: Data Offset May Be Needed";
        char message[] = 
          "The image stack originated from FEI software,\nbut the usual offset of "
          "32768 is not being added\nbecause the minimum of the file is positive.\n\n"
          "You may need to specify an offset to make the\n"
          "values be proportional to recorded electrons.";
        app.showWarning(title, message);
      }
    } else {
      if (header->amin < -20000 && header->amean < -1000) {
        ifOffset = 1;
        const char title[] = "WARNING: Data Offset Assumed";
        const char message[] = 
          "The image stack contains negative values\nunder -20000 and has a negative "
          "mean,\nso an offset of 32768 is being added.\n\n"
          "You may need to specify a different offset to make\n"
          "the values be proportional to recorded electrons.";
        app.showWarning(title, message);
      } else if (header->amean < 0)
        exitError("The mean of the input stack is negative.  You need to specify an "
          "offset to add to make values be proportional to recorded electrons");
    }
    if (ifOffset)
      dataOffset = 32768;
  }
  if (ifOffset) {
    cache->setDataOffset(dataOffset);
    // For focal pairs, assume both stacks need identical offset
    if (focalPairProcessing) {
      cache2 = app.getCache2();
      cache2->setDataOffset(dataOffset);
    }
  }

  // Removed setting of stack mean and noise files from here

  if (app.mDefocusFinder.getExpDefocus() <= 0)
    exitError("Invalid expected defocus, it must be >0");

  // Switch to calling routine for zero, scale correctly, go back at most 12, and limit
  // it for higher defocus
  double firstZero;
  int firstZeroIndex, secZeroIndex, backFromZero;
  app.getFittingRangeFromDefocus(0, firstZero, firstZeroIndex, secZeroIndex,
                                 backFromZero);
  if (firstZero > 0.49 && !baseOrderEntered)
    app.setBaselineOrder(1, true);

  if (ifFreqRange) {
    app.setX1Range(B3DNINT(2. * x1Start * (nDim - 1)), firstZeroIndex - 1);
    app.setX2Range(firstZeroIndex + 1, B3DNINT(2. * x2End * (nDim - 1)));
  } else {
    app.setX1Range(firstZeroIndex - backFromZero, firstZeroIndex - 1);
    app.setX2Range(firstZeroIndex + 1, secZeroIndex);
  }

  app.mSimplexEngine = new SimplexFitting(nDim, &app);
  app.mLinearEngine = new LinearFitting(nDim);
  app.computeInitPS(false);
  app.plotFitPS(true); //fit and plot the stack PS;

  // Did that really have to happen before cropping te pixel?
  // Here is cropping
  if (cropPixel > pixelSize) {
    cropPixel = B3DMIN(cropPixel, 5. * pixelSize);
    app.setCropPixelSizeInDia(cropPixel);
    app.setCropSpectra(true);
    app.adjustCachesForTileParams(defocusTol, tileSize, tiltAxisAngle, leftDefTol, 
                                  rightDefTol, cropPixel);

    // Now if autofitting set up to use current and flush value from previous fit
    if (ifAutofit && !autoWithExpected) {
      app.setUseCurDefocus(1);
      app.setUseCurrentPhase(true);
      app.mDefocusFinder.setDefocus(app.mDefocusFinder.getExpDefocus());
    }
    
    // IF WE WANT ENTERED VALUES TAKEN LITERALLY
    if (ifFreqRange) {
      app.setX1Range(B3DNINT(2. * x1Start * (nDim - 1)), firstZeroIndex - 1);
      app.setX2Range(firstZeroIndex + 1, B3DNINT(2. * x2End * (nDim - 1)));
    }
    if (!app.multipleSpectraAndFits()) {
      app.computeInitPS(false);
      app.plotFitPS(false);
    }
  } else if (findAstig || findPhase)
    app.multipleSpectraAndFits();

  fflush(stdout);

  if (!saveAndExit) {
    delete splash;
    mainWin->setCentralWidget(plotter);
    mainWin->resize(768, 624);
    mainWin->statusBar()->showMessage(QObject::tr("Ready"), 2000);

    // open all the windows in their default positions
    mainWin->show();
    plotter->openAngleDia();
    plotter->openFittingDia();
    qApp->processEvents();
    plotter->setInStartup(false);
    plotter->startDlgPlacementTimer();

    bool skipOk;
    double lowAngle, highAngle;
    plotter->mAngleDia->getAnglesAndStep(0, lowAngle, highAngle, ierr, skipOk);
  }
  if (ifAutofit) {
    app.autoFitToRanges(app.getAutoFromAngle(), app.getAutoToAngle(), 
                        app.getViewRangeStep(), 3);
    if (saveAndExit) {
      app.writeDefocusFile();
    }
  }
  if (!(ifAutofit && saveAndExit)) {
    qapp->exec();
    if (app.getSaveModified()) {
      int retval =  QMessageBox::information
        (0, "Save File?", "There are unsaved changes to the defocus table - "
         "save before exiting?", QMessageBox::Yes | QMessageBox::No,
         QMessageBox::NoButton);
      if (retval == QMessageBox::Yes)
        app.writeDefocusFile();
    }
  }
  free(rAvg);
  free(noisePs);
  free(noiseMean);
  free(index);
  delete(ctfHelp);
  delete mainWin;
  delete qapp;
  free(configName);
  free(stackFn);
  free(angleFn);
  free(defFn);
  free(rAvg1);
  free(rAvg2);
  return 0;
}

int ctfShowHelpPage(const char *page)
{
  if (ctfHelp)
    return (ctfHelp->showPage(page) > 0 ? 1 : 0);
  else
    return 1;
}
