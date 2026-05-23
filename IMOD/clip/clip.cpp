/*
 *  clip -- Command Line Image Processing
 *
 *  Original author: James Kremer
 *  Revised by: David Mastronarde   email: mast@colorado.edu
 *
 *  Copyright (C) 1995-2015 by the Regents of the University of 
 *  Colorado.  See dist/COPYRIGHT for full copyright notice.
 *
 *  $Id$
 */

#include <string.h>
#include <stdarg.h>
#include "iimage.h"
#include "clip.h"
#include "imodconfig.h"
#include "parse_params.h"

void usage(void)
{
  char *name = "clip";

  printf( 
          "%s: Command Line Image Processing. %s, %s %s\n",
          name,
          VERSION_NAME, __DATE__, __TIME__);
  imodCopyright();
  printf( "----------------------------------------------------\n");

  printf( "%s usage:\n", name);
  printf( "%s [process] [options] [input files...] [output file]\n", name);
  printf( "\nprocess:\n");
  printf( "\tadd         - Add images together.\n");
  printf( "\taverage     - Average files together.\n");
  printf( "\tbrightness  - Increase or decrease brightness.\n");
  printf( "\tcolor       - Add false color.\n");
  printf( "\tcontrast    - Increase or decrease contrast.\n");
  printf( "\tcorrelation - Do a auto/cross correlation.\n");
  printf( "\tdefectmap   - Output map of all pixels in defects entered with -B.\n"
          );
  printf( "\tdiffusion   - Do 2-D anisotropic diffusion on slices.\n");
  printf( "\tdivide      - Divide one image volume by another.\n");
  printf( "\tgradient    - Compute gradient as in 3dmod.\n");
  printf( "\tgraham      - Apply Graham filter as in 3dmod.\n");
  printf( "\thistogram   - Print histogram of values.\n");
  printf( "\tinfo        - Print information to stdout.\n");
  printf( "\tfft         - Do a fft or inverse fft transform.\n");
  printf( "\tfilter      - Do a bandpass filter.\n");
  printf( "\tflip..      - Flip image about various axes.\n");
  /*     printf( "\tpeak        - Find peaks in image.\n"); */
  printf( "\tjoinrgb     - Join 3 byte files into an RGB file.\n");
  printf( "\tlaplacian   - Apply Laplacian filter as in 3dmod.\n");
  printf( "\tmedian      - Do median filtering.\n");
  printf( "\tmultiply    - Multiple one image volume by another.\n");
  printf( "\tnormalize   - Multiply by gain ref., scale, remove big values.\n");
  printf( "\tprewitt     - Apply Prewitt filter as in 3dmod.\n");
  printf( "\tquadrant    - Correct quadrant disparities from 4-port camera.\n");
  printf( "\tresize      - Cut out and/or pad image data.\n");
  printf( "\trotx        - Rotate volume by -90 about X axis.\n");
  /* printf( "\trotation    - Rotate image\n"); */
  printf( "\tshadow      - Increase or decrease image shadows.\n");
  printf( "\tsharpen     - Sharpen image as in 3dmod.\n");
  printf( "\tsmooth      - Smooth image as in 3dmod.\n");
  printf( "\tsobel       - Apply Sobel filter as in 3dmod.\n");
  printf( "\tspectrum    - Compute scaled, reduced power spectrum.\n");
  printf( "\tsplitrgb    - Split RGB image file into 3 byte files.\n");
  printf( "\tstandev     - Compute standard deviation for averaged images.\n");
  printf( "\tstats       - Print some stats on image file.\n");
  printf( "\tsubtract    - Subtract one image volume from another.\n");
  printf( "\tsupergain   - Analyze EER file(s) .\n");
  printf( "\tthreshold   - Apply threshold to make binary image.\n");
  printf( "\ttruncate    - Limit image values at low or high end.\n");
  printf( "\tunwrap      - Undo a wraparound of integer intensity values.\n");
  printf( "\tunpack      - Unpack 4-bit data into bytes - same as normalize.\n");
  printf( "\tvariance    - Compute variance for averaged images.\n");
  /* printf( "\ttranslate   - translate image.\n");
  printf( "\tzoom        - magnify image.\n"); */
  printf( "\noptions:\n");
  printf( "\t[-v]  view output data.\n");
  printf( "\t[-3d] or [-2d] treat image as 3d (default) or 2d.\n");
  printf( "\t[-s] Switch, [-n #] Amount; Depends on function.\n");
  printf( "\t[-n #] [-l #] Iterations and Gaussian sigma for smoothing.\n");
  printf( "\t[-h #] [-l #] Values for filter, threshold, or truncate.\n");
  printf( "\t[-cc #] [-l #] [-k #] values for anisotropic diffusion.\n");
  printf( "\t[-r #] [-g #] [-b #] red, green, blue values.\n");
  printf( "\t[-x #,#]  [-y #,#]  starting and ending input coords.\n");
  printf( "\t[-cx #]  [-cy #]  [-cz #]  center coords.\n");
  printf( "\t[-ix #]  [-iy #]  [-iz #]  input sizes.\n");
  printf( "\t[-ox #]  [-oy #]  [-oz #]  output sizes.\n");
  printf( "\t[-CX #]  [-CY #]  [-CZ #]  chunk sizes for tiled HDF output.\n");
  printf( "\t[-a] Append output to file.\n");
  printf( "\t[-ov #] Overwrite output starting at section #\n");
  printf( "\t[-m (mode#)] Output data mode.\n");
  printf( "\t[-f format] Output file format (MRC, TIFF, HDF, JPEG).\n");
  printf( "\t[-p (pad#)] Pad empty data value.\n");
  printf( "\t[-1] Number Z values from 1 instead of 0.\n");
  printf( "\t[-P file] Name of piece list file for stats on a montage.\n");
  printf( "\t[-O #,#]  Overlaps in X and Y in displayed montage.\n");
  printf( "\t[-D file] Apply defect correction using defect list in given "
          "file.\n");
  printf( "\t[-B #] Binning value to use in defect correction.\n");
  printf( "\t[-S]   Scale defect list up by 2 if it is not already scaled.\n");
  printf( "\t[-R #] Rotation/flip to apply to gain reference, or -1 for r/f.\n");
  printf( "\t[-E #,#] Analyze histogram for extra counts on one side.\n");
  printf( "\t[-F #,#] Analyze histogram for fastest falloff point.\n");
  printf( "\t[-M #,#] Set minimum size of connected regions when "
          "thresholding.\n");
  printf( "\t[-es #] Set super-resolution factor when reading from EER files.\n");
  printf( "\t[-ez #] Set summing of frames when reading from EER files.\n");
  printf( "\t[-et]   Read thumbnail, not frames, from EER file.\n");
  printf( "\t[-ep #] Set padding of defects from gain reference for EER file.\n");
  printf( "\t[-ed file] Write defects from EER gain reference to file.\n");
  printf( "\t[-eg file] File for adjusting super-resolution gain reference.\n");
  printf( "\n");
}

/* 11/4/04: switch to standard out and standard format to some extent */
void show_error(const char *format, ...)
{
  char errorMess[512];
  va_list args;
  va_start(args, format);
  vsprintf(errorMess, format, args);
  printf("ERROR: %s\n", errorMess);
}

void show_warning(char *reason)
{
  printf("WARNING: %s\n", reason);
}

void show_status(char *info)
{
  printf("%s", info);
  fflush(stdout);
}

void default_options(ClipOptions *opt)
{
  opt->hin = opt->hin2 = opt->hout = NULL;
  opt->x  = IP_DEFAULT; opt->x2 = IP_DEFAULT;
  opt->y  = IP_DEFAULT; opt->y2 = IP_DEFAULT;
  opt->z  = IP_DEFAULT; opt->z2 = IP_DEFAULT;
  opt->ix = IP_DEFAULT; opt->iy = IP_DEFAULT; 
  opt->iz = IP_DEFAULT; opt->iz2 = IP_DEFAULT;
  opt->ox = IP_DEFAULT; opt->oy = IP_DEFAULT; opt->oz = IP_DEFAULT;
  opt->cx = IP_DEFAULT; opt->cy = IP_DEFAULT; opt->cz = IP_DEFAULT;
  opt->chunkX = IP_DEFAULT; opt->chunkY = IP_DEFAULT; opt->chunkZ = IP_DEFAULT;
  opt->outBefore = opt->outAfter = IP_DEFAULT;
  opt->red = IP_DEFAULT; opt->green = IP_DEFAULT; opt->blue = IP_DEFAULT;
  opt->high = IP_DEFAULT; opt->low = IP_DEFAULT;
  opt->thresh = IP_DEFAULT;
  opt->pctlFrac = IP_DEFAULT;
  opt->falloffFrac = IP_DEFAULT;
  opt->weight = IP_DEFAULT;
  opt->pad    = IP_DEFAULT;
  opt->minSize = IP_DEFAULT;
  opt->process = IP_NONE;
  opt->dim = 3;
  opt->add2file = IP_APPEND_FALSE;
  opt->sano = FALSE;
  opt->val = IP_DEFAULT;
  opt->mode = IP_DEFAULT;
  opt->nofsecs = IP_DEFAULT;
  opt->secs = NULL;
  opt->ocanresize = TRUE;
  opt->ocanchmode = TRUE;
  opt->fromOne = FALSE;
  opt->ofname = NULL;
  opt->plname = NULL;
  opt->superGainName = NULL;
  opt->newXoverlap = IP_DEFAULT;
  opt->newYoverlap = IP_DEFAULT;
  opt->readDefects = FALSE;
  opt->rotationFlip = 0;
  opt->binning = IP_DEFAULT;
  opt->scaleDefects = FALSE;
}



int main( int argc, char *argv[] )
{
  MrcHeader hin, hin2, hout;
  ClipOptions opt;

  int process = IP_NONE;  /* command to run.             */
  int view    = FALSE;    /* view file at end?           */
  int procout = TRUE;     /* will process write output?. */
  int needtwo = FALSE;    /* Does process need two input files? */
  int iarg, j, itemp, superFac;
  int retval = 0;
  int formatSet = -2;
  int feiDefPad = 1;
  int eerGroup = 1, eerSuper = 2, eerFlags = 0;
  char *dumpDefectName = NULL;
  ImodImageFile *iiFile;
  char *strng, *strCopy;
  b3dUInt16 count;
  std::string sstr;
  FILE *defFP;
  
  char viewcmd[1024];
  char *progname = imodProgName(argv[0]);

  sprintf(viewcmd, "ERROR: %s - ", progname);
  setExitPrefix(viewcmd);

  if (argc < 3){
    usage();
    exit(3);
  }

  opt.command = argv[1];

  /* Find which process to run */
  if (!strncmp( argv[1], "add", 3)) {
    process = IP_ADD;
    needtwo = TRUE;
  }
  if ((!strncmp( argv[1], "avg", 3)) || 
      (!strncmp( argv[1], "average", 3)) )
    process = IP_AVERAGE;
  if (!strncmp( argv[1], "standev", 4))
    process = IP_STANDEV;
  if (!strncmp( argv[1], "variance", 3))
    process = IP_VARIANCE;
  if (!strncmp( argv[1], "multiply", 3)) {
    process = IP_MULTIPLY;
    needtwo = TRUE;
  }
  if (!strncmp( argv[1], "subtract", 3)) {
    process = IP_SUBTRACT;
    needtwo = TRUE;
  }
  if (!strncmp( argv[1], "divide", 3)) {
    process = IP_DIVIDE;
    needtwo = TRUE;
  }
  if (!strncmp( argv[1], "brightness", 3))
    process = IP_BRIGHTNESS;
  if (!strncmp( argv[1], "color", 3))
    process = IP_COLOR;
  if (!strncmp( argv[1], "contrast", 3))
    process = IP_CONTRAST;

  if (!strncmp( argv[1], "correlation", 3))
    process = IP_CORRELATE; 
  if (!strncmp( argv[1], "diffusion", 3))
    process = IP_DIFFUSION; 

  if (!strncmp( argv[1], "info", 3)){
    process = IP_INFO;
    procout = FALSE;
  }

  if (!strncmp( argv[1], "fft", 3))
    process = IP_FFT;
  if (!strncmp( argv[1], "filter", 3))
    process = IP_FILTER;

  if (!strncmp( argv[1], "flip", 4))
    process = IP_FLIP;
  if (!strncmp( argv[1], "rotx", 4))
    process = IP_FLIP;
  if (!strncmp( argv[1], "gradient", 4))
    process = IP_GRADIENT;
  if (!strncmp( argv[1], "graham", 4))
    process = IP_GRAHAM;
  if (!strncmp( argv[1], "histogram", 2)) {
    process = IP_HISTOGRAM;
    procout = FALSE;
  }
  if (!strncmp( argv[1], "laplacian", 2))
    process = IP_LAPLACIAN;
  if (!strncmp( argv[1], "median", 2))
    process = IP_MEDIAN;
  /*  if (!strncmp( argv[1], "peak", 3)){
    process = IP_PEAK;
    procout = FALSE;
    } */
  if (!strncmp( argv[1], "prewitt", 2))
    process = IP_PREWITT;
  if (!strncmp( argv[1], "resize", 3))
    process = IP_RESIZE;
  /* if (!strncmp( argv[1], "rotate", 3))
     process = IP_ROTATE; */
  if (!strncmp( argv[1], "shadow", 4))
    process = IP_SHADOW;
  if (!strncmp( argv[1], "sharpen", 4))
    process = IP_SHARPEN;
  if (!strncmp( argv[1], "smooth", 2))
    process = IP_SMOOTH;
  if (!strncmp( argv[1], "sobel", 2))
    process = IP_SOBEL;
  if (!strncmp( argv[1], "spectrum", 2))
    process = IP_SPECTRUM;
  if (!strncmp( argv[1], "stat", 4)){
    process = IP_STAT;
    procout = FALSE;
  }
  /* if (!strncmp( argv[1], "translate", 2))
    process = IP_TRANSLATE;
  if (!strncmp( argv[1], "zoom", 3))
     process = IP_ZOOM;
  if (!strncmp( argv[1], "project", 3)){
    process = IP_PROJECT;
    } */

  if (!strncmp( argv[1], "threshold", 3))
    process = IP_THRESHOLD;
  if (!strncmp( argv[1], "truncate", 3))
    process = IP_TRUNCATE;
  if (!strncmp( argv[1], "unwrap", 3))
    process = IP_UNWRAP;
  if (!strncmp( argv[1], "sqroot", 3))
    process = IP_SQROOT;
  if (!strncmp( argv[1], "logarithm", 3))
    process = IP_LOGARITHM;
  if (!strncmp( argv[1], "quadrant", 2)) {
    process = IP_QUADRANT;
    opt.dim = 2;
  } 
  if (!strncmp( argv[1], "edgefill", 2)) {
    process = IP_FILLEDGE;
    opt.dim = 2;
  }
  if (!strncmp( argv[1], "unpack", 3))
    process = IP_UNPACK;
  if (!strncmp( argv[1], "normalize", 3))
    process = IP_NORMALIZE;
  if (!strncmp( argv[1], "defectmap", 3))
    process = IP_DEFECTMAP;
  if (!strncmp( argv[1], "supergain", 3))
    process = IP_SUPERGAIN;
  if (!strncmp( argv[1], "splitrgb", 3)){
    process = IP_SPLITRGB;
  }
  if (!strncmp( argv[1], "joinrgb", 3)){
    process = IP_JOINRGB;
  }

  if (process == IP_NONE){
    usage();
    exit(1);
  }

  default_options(&opt);
  opt.process = process;
  opt.pname = progname;
  if (process == IP_SUPERGAIN) {
    eerGroup = 250;
    eerSuper = 2;
    opt.val = 4;
  }

  /* get options */
  for (iarg = 2; iarg < argc; iarg++) {
    if (argv[iarg][0] == '-') {
      switch (argv[iarg][1]){
		    
      case 'a':
        opt.add2file = IP_APPEND_ADD;  break;

      case '3':
        if (process != IP_QUADRANT)
          opt.dim = 3;
        break;

      case '2':
        opt.dim = 2; break;

      case '1':
        opt.fromOne = TRUE; break;

      case 's':
        opt.sano = TRUE; break;		    
		    
      case 'n':
        sscanf(argv[++iarg], "%f", &(opt.val)); break;
		    
      case 'm':
        if (strcmp(argv[iarg + 1], "4-bit") == 0 || strcmp(argv[iarg + 1], "101") == 0) {
          set4BitOutputMode(1);
          opt.mode = MRC_MODE_BYTE;
          iarg++;
        } else {
          opt.mode = sliceMode(argv[++iarg]); 
        }
        if (opt.mode == SLICE_MODE_UNDEFINED)
          exitError("Invalid mode entry %s.", argv[iarg]);
        if (opt.mode == SLICE_MODE_SBYTE || opt.mode == SLICE_MODE_UBYTE) {
          overrideWriteBytes(opt.mode == SLICE_MODE_SBYTE ? 1 : 0);
          opt.mode = 0;
        }
        break;

      case 'f':
        iarg++;
        formatSet = setOutputTypeFromString(argv[iarg]);
        if (formatSet < 0)
          exitError("Output file format entry %s is not %s.", argv[iarg],
                    formatSet == -1 ? "recognized" : "available in this copy of IMOD");
        break;

      case 'v':
        view = TRUE; break;

      case 'p':  /* Pad option */
        opt.pad = (float)atof(argv[++iarg]); break;

      case 't':
        sscanf(argv[++iarg], "%f", &(opt.thresh)); break;
      case 'E':
        sscanf(argv[++iarg], "%f%*c%d", &(opt.pctlFrac), &itemp); 
        if (itemp < 0)
          opt.pctlFrac = -opt.pctlFrac;
        break;
      case 'F':
        sscanf(argv[++iarg], "%f%*c%d", &(opt.falloffFrac), &itemp);
        if (itemp < 0)
          opt.falloffFrac = -opt.falloffFrac;
        break;
      case 'M':
        sscanf(argv[++iarg], "%d%*c%d", &(opt.minSize), &itemp);
        if (itemp < 0)
          opt.minSize = -opt.minSize;
        break;

      case 'k':
      case 'w':
        sscanf(argv[++iarg], "%f", &(opt.weight)); break;

      case 'r':
        sscanf(argv[++iarg], "%f", &(opt.red)); break;
      case 'g':
        sscanf(argv[++iarg], "%f", &(opt.green)); break;
      case 'b':
        sscanf(argv[++iarg], "%f", &(opt.blue)); break;

      case 'l':
        sscanf(argv[++iarg], "%f", &(opt.low)); break;
      case 'h':
        sscanf(argv[++iarg], "%f", &(opt.high)); break;

      case 'x': case 'X':
        sscanf(argv[++iarg], "%d%*c%d", &(opt.x), &(opt.x2)); break;
      case 'y': case 'Y':
        sscanf(argv[++iarg], "%d%*c%d", &(opt.y), &(opt.y2)); break;
        /*case 'z': case 'Z':
          sscanf(argv[++iarg], "%f%*c%f", &(opt.z), &(opt.z2)); break; */

      case 'o':
        switch (argv[iarg][2]){
        case 0x00:
        case ' ':
        case 'v':
          opt.add2file = IP_APPEND_OVERWRITE;
          sscanf(argv[++iarg], "%d", &(opt.isec));
          break;
        case 'x': case 'X':
          sscanf(argv[++iarg], "%d", &(opt.ox)); break;
        case 'y': case 'Y':
          sscanf(argv[++iarg], "%d", &(opt.oy)); break;
        case 'z': case 'Z':
          sscanf(argv[++iarg], "%d", &(opt.oz)); break;
        default:
          exitError("Invalid option %s.", argv[iarg]);
        }
        break;
		    
      case 'i': case 'I':
        switch (argv[iarg][2]){
        case 'x': case 'X':
          sscanf(argv[++iarg], "%d", &(opt.ix)); break;
        case 'y': case 'Y':
          sscanf(argv[++iarg], "%d", &(opt.iy)); break;
        case 'z': case 'Z':
          sscanf(argv[++iarg], "%d%*c%d", &(opt.iz),&opt.iz2);
          opt.secs = clipMakeSecList(argv[iarg], &opt.nofsecs);
          break;
        default:
          exitError("Invalid option %s.", argv[iarg]);
        }
        break;

      case 'c':
        switch (argv[iarg][2]){
        case 'x':
          sscanf(argv[++iarg], "%g", &(opt.cx)); break;
        case 'y':
          sscanf(argv[++iarg], "%g", &(opt.cy)); break;
        case 'z':
          sscanf(argv[++iarg], "%g", &(opt.cz)); break;
        case 'c':
          sscanf(argv[++iarg], "%f", &(opt.thresh)); break;
        default:
          exitError("Invalid option %s.", argv[iarg]);
        }
        break;

      case 'C':
        switch (argv[iarg][2]){
        case 'X':
          sscanf(argv[++iarg], "%d", &(opt.chunkX)); break;
        case 'Y':
          sscanf(argv[++iarg], "%d", &(opt.chunkY)); break;
        case 'Z':
          sscanf(argv[++iarg], "%d", &(opt.chunkZ)); break;
        default:
          exitError("Invalid option %s.", argv[iarg]);
        }
        break;

      case 'P':
        opt.plname = strdup(argv[++iarg]);
        break;

      case 'O':
        sscanf(argv[++iarg], "%d%*c%d", &opt.newXoverlap, &opt.newYoverlap);
        break;

      case 'D':
        j = CorDefParseDefects(argv[++iarg], 0, opt.defects, opt.camSizeX, opt.camSizeY);
        if (j)
          exitError("Error %s", (j == 1) ? "opening" : 
                  "reading or parsing lines in", argv[iarg]);
        opt.readDefects = TRUE;
        break;

      case 'B':
        opt.binning = atof(argv[++iarg]);
        if (opt.binning <= 0.49)
          exitError("Binning must be at least 0.5"); 
        break;

      case 'S':
        opt.scaleDefects = TRUE;
        break;

      case 'R':
        opt.rotationFlip = atoi(argv[++iarg]);
        break;
 
      case 'e':
        if (process == IP_SUPERGAIN)
          exitError("The -es, -ez, and other EER options cannot be entered with "
                    "the supergain operation");
        if (argv[iarg][2] == 's') {
          eerSuper = atoi(argv[++iarg]);
          B3DCLAMP(eerSuper, 0, tiffGetMaxEERsuperRes());
        } else if (argv[iarg][2] == 'z') {
          eerGroup = atoi(argv[++iarg]);
          eerGroup = B3DMAX(eerGroup, 0);
        } else if (argv[iarg][2] == 't') {
          eerFlags = IIFLAG_SKIP_EER_DIRS;
        } else if (argv[iarg][2] == 'p') {
          feiDefPad = atoi(argv[++iarg]);
          feiDefPad = B3DMAX(feiDefPad, 0);
        } else if (argv[iarg][2] == 'd') {
          dumpDefectName = strdup(argv[++iarg]);
        } else if (argv[iarg][2] == 'g') {
          opt.superGainName = strdup(argv[++iarg]);
        } else
          exitError("Invalid option %s.", argv[iarg]);
        break;

      default:
        exitError("Invalid option %s.", argv[iarg]);
      }
    } else {
      break;
    }
  }

  if (iarg < argc && replaceFileArgVec((const char ***)&argv, &argc, &iarg, &j))
    exit(1);

  tiffSetEERreadProperties(eerSuper, eerGroup, eerFlags);
  opt.fnames  = &(argv[iarg]);

  if (!procout) {
    /* check for at least one input file */
    if ((argc - 1) < iarg) {
      usage();
      exit(3);
    }
    opt.infiles = argc - iarg;
  } else {
    /* check for at least one input and one output file name. */
    if ((argc - 2) < iarg){
      usage();
      exit(3);
    }
    opt.infiles = argc - iarg - 1;
  }

  if (opt.chunkX != IP_DEFAULT || opt.chunkY != IP_DEFAULT || opt.chunkZ != IP_DEFAULT) {
    if (formatSet >= 0 && formatSet != OUTPUT_TYPE_HDF)
      exitError("You cannot specify chunk sizes and an output format other than HDF");
    overrideOutputType(OUTPUT_TYPE_HDF);
  }

  // Check defect entries, flip in Y and find touching pixels
  if (opt.readDefects) {
    if (!opt.camSizeX || !opt.camSizeY)
      exitError("Problem with defect correction - Defect list file must"
              " have CameraSizeX and CameraSizeY entries");
    CorDefFlipDefectsInY(&opt.defects, opt.camSizeX, opt.camSizeY, 0);
    CorDefFindTouchingPixels(opt.defects, opt.camSizeX, opt.camSizeY, 0);
  }

  if (process == IP_DEFECTMAP && b3dOutputFileType() == IIFILE_TIFF)
    setTiffCompressionType(3, 0);

  if (opt.x != IP_DEFAULT) {
    if (opt.cx != IP_DEFAULT || opt.ix != IP_DEFAULT)
      exitError("You cannot use -x together with -cx or -ix");
  }
  if (opt.y != IP_DEFAULT)
    if (opt.cy != IP_DEFAULT || opt.iy != IP_DEFAULT) {
      exitError("You cannot use -y together with -cy or -iy");
  }

  /* check and load files:
   * Always at least one input file.
   */
  hin.fp = iiFOpen(argv[iarg], "rb");
  hin.pathname = argv[iarg];
  if (!hin.fp)
    exitError("Error opening %s", argv[iarg]);
  if (mrc_head_read(hin.fp, &hin))
    exitError("Error reading %s", argv[iarg]);

  /* Set output header default same as first input file. */
  hout = hin;

  // Do parallel read from a TIFF unless appending/overwriting
  if (opt.add2file == IP_APPEND_FALSE)
    iiUseTiffThreadsForFP(hin.fp, 0);

  /* 7/20/11: Consolidate all changes for output header into this call; eliminate 
   * setting swapped to 0 below; this header is replaced if appending
   * There is also a new header made in lots of places, not sure how much this is used */
  mrcInitOutputHeader(&hout);

  /* Load additional input files. */
  iarg++;
  if (opt.infiles > 1){
    hin2.fp = iiFOpen(argv[iarg], "rb");
    hin2.pathname = argv[iarg];
    if (!hin2.fp)
      exitError("Error opening %s", argv[iarg]);
    if (mrc_head_read(hin2.fp, &hin2)){
      if (process == IP_INFO){
        mrc_head_print(&hin);
        printf("**********************************************\n");
        printf("WARNING: This file is not a readable MRC file.\n");
        printf("**********************************************\n");
      }
      exitError("Error reading %s", argv[iarg]);
    }
    iarg++;
  }

  // Check for norm or unpack with a tiff file and as second of two inputs
  if ((process == IP_NORMALIZE || process == IP_UNPACK || process == IP_DEFECTMAP) && 
      opt.infiles == 2 && !opt.readDefects) {
    iiFile = iiLookupFileFromFP(hin2.fp);
    if (iiFile && iiFile->file == IIFILE_TIFF &&
        tiffGetArray(iiFile, FEI_TIFF_DEFECT_TAG, &count, &strng) > 0) {
      strCopy = B3DMALLOC(char, count + 1);
      if (!strCopy)
        exitError("Memory error copying defect string from TIFF file");
      strncpy(strCopy, strng, count);
      strCopy[count] = 0x00;
      retval = CorDefParseFeiXml(strCopy, opt.defects, feiDefPad);
      if (retval)
        exitError("Parsing defect string from TIFF file (error %d)", retval);
      free(strCopy);
      opt.readDefects = TRUE;
      if (dumpDefectName) {
        imodBackupFile(dumpDefectName);
        defFP = fopen(dumpDefectName, "w");
        if (!defFP)
          exitError("Opening file to write defects to, %s", dumpDefectName);
        CorDefDefectsToString(opt.defects, sstr, hin2.nx, hin2.ny);
        fprintf(defFP, "%s", sstr.c_str());
        fclose(defFP);
        B3DFREE(dumpDefectName);
      }
      superFac = hin.nx / hin2.nx;
      j = hin.ny / hin2.ny;
      if (j != superFac || superFac * hin2.nx != hin.nx || superFac * hin2.ny != hin.ny
          || (superFac != 1 && superFac != 2 && superFac != 4))
        exitError("Image file size (%d x %d) must be exactly the same, twice, or 4 "
                  "times the gain reference size (%d x %d)", hin.nx, hin.ny, hin2.nx, 
                  hin2.ny);
      CorDefFlipDefectsInY(&opt.defects, hin2.nx, hin2.ny, 0);
      CorDefFindTouchingPixels(opt.defects, hin2.nx, hin2.ny, 0);
      if (superFac > 1)
        CorDefScaleDefectsForFalcon(&opt.defects, superFac);
      opt.camSizeX = hin.nx;
      opt.camSizeY = hin.ny;
    }
  }

  /* Setup output file if needed and there are enough - otherwise let the process give 
     the error message . */
  if (procout && (!needtwo || opt.infiles > 1)) {
    opt.ofname = argv[argc - 1];
    if (process == IP_SUPERGAIN) {
      imodBackupFile(opt.ofname);
      hout.fp = fopen(opt.ofname, "w");
    } else {

      if (opt.add2file){
        hout.fp = iiFOpen(argv[argc - 1], "rb+");
        if (!hout.fp)
          exitError("Error finding %s", argv[argc - 1]);

        if (mrc_head_read(hout.fp, &hout))
          exitError("Error reading %s", argv[iarg]);

      } else if (process != IP_SPLITRGB) {

        /* DNm 10/20/03: switch to calling routine for backup file */
        if (!getenv("IMOD_NO_IMAGE_BACKUP"))
          imodBackupFile(argv[argc - 1]);

        if (process == IP_FFT && hin.mode != MRC_MODE_COMPLEX_FLOAT && 
            (b3dOutputFileType() == IIFILE_TIFF || b3dOutputFileType() == IIFILE_JPEG)) {
          printf("WARNING: clip - Writing an MRC file; TIFF or JPEG files cannot contain "
                 "FFTs\n");
          overrideOutputType(IIFILE_MRC);
        }

        hout.fp = iiFOpen(argv[argc - 1], "wb+");
        
      }
    }
    if (!hout.fp)
      exitError("Error opening output file %s", argv[argc - 1]);
  }
    
  opt.hin = &hin;
  opt.hout = &hout;

  /* Massage the Z values if numbered from one */
  if (opt.fromOne) {
    if (opt.add2file == IP_APPEND_OVERWRITE)
      opt.isec--;
    if (opt.nofsecs != IP_DEFAULT)
      for (j = 0; j < opt.nofsecs; j++)
        opt.secs[j]--;
    if (opt.cz != IP_DEFAULT)
        opt.cz--;
    if (opt.dim == 2) {
      if (opt.iz != IP_DEFAULT)
        opt.iz--;
      if (opt.iz2 != IP_DEFAULT)
        opt.iz2--;
    }
  }

  // Set the mode to float automatically for log
  if (process == IP_LOGARITHM && opt.mode == IP_DEFAULT)
    opt.mode = MRC_MODE_FLOAT;

  /* run the selected process */
  switch(process){
  case IP_AVERAGE:
  case IP_STANDEV:
  case IP_VARIANCE:
  case IP_ADD:
  case IP_SUBTRACT:
    retval = clip_average(&hin, &hin2, &hout, &opt);
    break;
  case IP_MULTIPLY:
  case IP_DIVIDE:
    retval = clip_multdiv(&hin, &hin2, &hout, &opt);
    break;
  case IP_UNPACK:
  case IP_NORMALIZE:
    retval = clipUnpack(&hin, &hin2, &hout, &opt);
    break;
  case IP_DEFECTMAP:
    retval = clipDefectMap(&hin, &hout, &opt);
    break;
  case IP_SUPERGAIN:
    retval = clipSuperGain(&hin, hout.fp, &opt);
    break;
  case IP_BRIGHTNESS:
  case IP_CONTRAST:
  case IP_SHADOW:
  case IP_RESIZE:
  case IP_THRESHOLD:
  case IP_TRUNCATE:
  case IP_UNWRAP:
  case IP_LOGARITHM:
  case IP_SQROOT:
    retval = clip_scaling(&hin, &hout, &opt);
    break;
  case IP_COLOR:
    retval = clip_color(&hin, &hout, &opt);
    break;
  case IP_QUADRANT:
    retval = clip_quadrant(&hin, &hout, &opt);
    break;
  case IP_SPECTRUM:
    retval = clipSpectrum(&hin, &hout, &opt);
    break;
  case IP_FILLEDGE:
    retval = fillDriftCorrectedEdges(&hin, &hout, &opt);
    break;
  case IP_CORRELATE:
    retval = grap_corr(&hin, &hin2, &hout, &opt);
    break;
  case IP_DIFFUSION:
    retval = clipDiffusion(&hin, &hout, &opt);
    break;
  case IP_GRADIENT:
  case IP_GRAHAM:
  case IP_PREWITT:
  case IP_SOBEL:
    retval = clipEdge(&hin, &hout, &opt);
    break;
  case IP_INFO:
    retval = mrc_head_print(&hin);
    break;
  case IP_FFT:
    retval = clip_fft(&hin, &hout, &opt);
    break;
  case IP_FILTER:
    /* opt.dim = 2; */
    retval = clip_bandpass_filter(&hin, &hout, &opt);
    break;
  case IP_FLIP:
    retval = clip_flip(&hin, &hout, &opt);
    break;
  case IP_HISTOGRAM:
    retval = clipHistogram(&hin, &opt);
    break;
  case IP_JOINRGB:
    retval = clip_joinrgb(&hin, &hin2, &hout, &opt);
    break;
  case IP_LAPLACIAN:
  case IP_SMOOTH:
  case IP_SHARPEN:
    retval = clip_convolve(&hin, &hout, &opt);
    break;
  case IP_MEDIAN:
    retval = clipMedian(&hin, &hout, &opt, NULL, 0, 0);
    break;
    /*case IP_PEAK:
    retval = puts("peak: future function\n");
    break;
  case IP_ROTATE:
    retval = grap_rotate(&hin, &hout, &opt);
    break; */
  case IP_SPLITRGB:
    retval = clip_splitrgb(&hin, &opt);
    break;
  case IP_STAT:
    retval = clip_stat(&hin, &opt);
    break;
    /*case IP_TRANSLATE:
    retval = grap_trans(&hin, &hout, &opt);
    break;
  case IP_ZOOM:
    if (grap_zoom(&hin, &hout, &opt)) {
      fprintf(stderr, "%s: error doing zoom.\n", progname);
      retval = 1;
    }
    break;
    */
  default:
    exitError("No process selected.");
  }

  if (retval)
    exit(retval);
  iiCloseTiffCopiesForFP(hin.fp);
  iiFClose (hin.fp);
  if (procout && process != IP_SPLITRGB && process != IP_SUPERGAIN)
    iiFClose (hout.fp);

  if (view){
    sprintf(viewcmd, "3dmod %s", argv[argc - 1]);
    system(viewcmd);
  }

  // Had to switch from return(0) after converting to C++. It gave status 127 on Windows
  exit(0);
}
     
/*
 * Make a list of sections for use with the 
 * -2d and -z option. commands can be like
 * -z 0,4-10 or -z 2,3,4-8,12,19
 */
int *clipMakeSecList(char *clst, int *nofsecs)
{
  int len;
  int i;
  int *secs;
  int *tsecs;
  int range, top, r;

  secs = (int *)malloc(sizeof(int));
  len = strlen(clst);
  secs[0] = atoi(clst);
  *nofsecs = 1;
     
  for(i = 0; i < len; i++){
    switch(clst[i]){
    case ',':
      (*nofsecs)++;
      tsecs = (int *)realloc(secs, sizeof(int) * (*nofsecs));
      secs = tsecs;
      secs[*nofsecs - 1] = atoi(&(clst[++i]));
      break;
	       
    case '-':
      top = atoi(&(clst[++i]));
      if (top < secs[*nofsecs - 1]){
        range = top;
        top = secs[*nofsecs - 1];
        secs[*nofsecs - 1] = range;
      }
      if (top == secs[*nofsecs - 1])
        continue;
      range = top - secs[*nofsecs - 1];
      tsecs = (int *)realloc(secs, sizeof(int) * (*nofsecs + range));
      secs = tsecs;
      for (r = 0; r < range; r++)
        secs[*nofsecs + r] = secs[*nofsecs - 1] + r + 1;
      *nofsecs += range;
      break;
	       
    default:
      break;
    }
  }
  return(secs);
}

