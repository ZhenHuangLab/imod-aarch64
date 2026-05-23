package etomo.storage;

import java.util.HashMap;
import java.util.Map;

import etomo.storage.DirectiveAttribute.Match;
import etomo.storage.DirectiveFile.Comfile;
import etomo.storage.DirectiveFile.Command;
import etomo.storage.DirectiveFile.Module;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.AxisID;

/**
 * <p>Description: Describes directives as required.  Each instance should be unique.</p>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class DirectiveDef {
  public static final String RUN_TIME_ANY_AXIS_TAG = "any";
  public static final String RUN_TIME_A_AXIS_TAG = "a";
  public static final String B_AXIS_TAG = "b";

  private static final String RUN_TIME_BIN_BY_FACTOR_NAME = "binByFactor";
  private static final String COM_PARAM_BIN_BY_FACTOR_NAME = "BinByFactor";
  private static final String BINNING_NAME = "binning";
  private static final String THICKNESS_NAME = "thickness";
  public static final String TRUE_VALUE = "1";
  public static final String FALSE_VALUE = "0";
  public static final String SAMPLE_TYPE_PLASTIC_SECTION_VALUE = "1";
  static final Map<String, DirectiveDef> MAP = new HashMap<String, DirectiveDef>();

  // copyarg

  public static final DirectiveDef BINNING =
    new DirectiveDef(DirectiveType.COPY_ARG, BINNING_NAME);
  public static final DirectiveDef CS = new DirectiveDef(DirectiveType.COPY_ARG, "Cs");
  public static final DirectiveDef CTF_NOISE =
    new DirectiveDef(DirectiveType.COPY_ARG, "ctfnoise");
  public static final DirectiveDef DEFOCUS =
    new DirectiveDef(DirectiveType.COPY_ARG, "defocus");
  public static final DirectiveDef DISTORT =
    new DirectiveDef(DirectiveType.COPY_ARG, "distort");
  public static final DirectiveDef DUAL =
    new DirectiveDef(DirectiveType.COPY_ARG, "dual");
  public static final DirectiveDef EXTRACT =
    new DirectiveDef(DirectiveType.COPY_ARG, "extract", true);
  public static final DirectiveDef BEXTRACT =
    getBInstance(DirectiveType.COPY_ARG, "bextract");
  public static final DirectiveDef FIRST_INC =
    new DirectiveDef(DirectiveType.COPY_ARG, "firstinc", true);
  public static final DirectiveDef BFIRST_INC =
    getBInstance(DirectiveType.COPY_ARG, "bfirstinc");
  public static final DirectiveDef FOCUS =
    new DirectiveDef(DirectiveType.COPY_ARG, "focus", true);
  public static final DirectiveDef BFOCUS =
    getBInstance(DirectiveType.COPY_ARG, "bfocus");
  public static final DirectiveDef GOLD =
    new DirectiveDef(DirectiveType.COPY_ARG, "gold");
  public static final DirectiveDef GRADIENT =
    new DirectiveDef(DirectiveType.COPY_ARG, "gradient");
  public static final DirectiveDef MONTAGE =
    new DirectiveDef(DirectiveType.COPY_ARG, "montage");
  public static final DirectiveDef NAME =
    new DirectiveDef(DirectiveType.COPY_ARG, "name");
  public static final DirectiveDef PIXEL =
    new DirectiveDef(DirectiveType.COPY_ARG, "pixel");
  public static final DirectiveDef ROTATION =
    new DirectiveDef(DirectiveType.COPY_ARG, "rotation", true);
  public static final DirectiveDef BROTATION =
    getBInstance(DirectiveType.COPY_ARG, "brotation");
  public static final DirectiveDef SKIP =
    new DirectiveDef(DirectiveType.COPY_ARG, "skip", true);
  public static final DirectiveDef BSKIP = getBInstance(DirectiveType.COPY_ARG, "bskip");
  public static final DirectiveDef STACK_EXT =
    new DirectiveDef(DirectiveType.COPY_ARG, "stackext");
  public static final DirectiveDef TWODIR =
    new DirectiveDef(DirectiveType.COPY_ARG, "twodir", true);
  public static final DirectiveDef BTWODIR =
    getBInstance(DirectiveType.COPY_ARG, "btwodir");
  public static final DirectiveDef USE_RAW_TLT =
    new DirectiveDef(DirectiveType.COPY_ARG, "userawtlt", true);
  public static final DirectiveDef BUSE_RAW_TLT =
    getBInstance(DirectiveType.COPY_ARG, "buserawtlt");
  public static final DirectiveDef VOLTAGE =
    new DirectiveDef(DirectiveType.COPY_ARG, "voltage");

  // setupset

  public static final DirectiveDef CURRENT_B_STACK_EXT =
    new DirectiveDef(DirectiveType.SETUP_SET, "currentBStackExt");
  public static final DirectiveDef CURRENT_STACK_EXT =
    new DirectiveDef(DirectiveType.SETUP_SET, "currentStackExt");
  public static final DirectiveDef DATASET_DIRECTORY =
    new DirectiveDef(DirectiveType.SETUP_SET, "datasetDirectory");
  public static final DirectiveDef SCAN_HEADER =
    new DirectiveDef(DirectiveType.SETUP_SET, "scanHeader");
  public static final DirectiveDef SCOPE_TEMPLATE =
    new DirectiveDef(DirectiveType.SETUP_SET, "scopeTemplate");
  public static final DirectiveDef SYSTEM_TEMPLATE =
    new DirectiveDef(DirectiveType.SETUP_SET, "systemTemplate");
  public static final DirectiveDef USER_TEMPLATE =
    new DirectiveDef(DirectiveType.SETUP_SET, "userTemplate");

  // runtime

  public static final DirectiveDef BIN_BY_FACTOR_FOR_ALIGNED_STACK = new DirectiveDef(
    DirectiveType.RUN_TIME, Module.ALIGNED_STACK, RUN_TIME_BIN_BY_FACTOR_NAME);
  public static final DirectiveDef CORRECT_CTF =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.ALIGNED_STACK, "correctCTF");
  public static final DirectiveDef ERASE_GOLD =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.ALIGNED_STACK, "eraseGold");
  public static final DirectiveDef FILTER_STACK =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.ALIGNED_STACK, "filterStack");
  public static final DirectiveDef LINEAR_INTERPOLATION =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.ALIGNED_STACK, "linearInterpolation");
  public static final DirectiveDef SIZE_IN_X_AND_Y =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.ALIGNED_STACK, "sizeInXandY");

  public static final DirectiveDef NUMBER_OF_RUNS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.BEAD_TRACKING, "numberOfRuns");

  public static final DirectiveDef DO_SIRT_IF_BOTH =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.COMBINE, "doSIRTifBoth");
  public static final DirectiveDef EXTRA_TARGETS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.COMBINE, "extraTargets");
  public static final DirectiveDef FINAL_PATCH_SIZE =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.COMBINE, "finalPatchSize");
  public static final DirectiveDef FIND_SEC_BOX_SIZE =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.COMBINE, "findSecBoxSize");
  public static final DirectiveDef FIND_SEC_NUM_SCALES =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.COMBINE, "findSecNumScales");
  public static final DirectiveDef LOW_FROM_BOTH_RADIUS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.COMBINE, "lowFromBothRadius");
  public static final DirectiveDef MATCH_A_TO_B_THICK_RATIO =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.COMBINE, "matchAtoBThickRatio");
  public static final DirectiveDef PATCH_SIZE =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.COMBINE, "patchSize");
  public static final DirectiveDef WEDGE_REDUCTION =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.COMBINE, "wedgeReduction");

  public static final DirectiveDef CORRECT_FOR_X_AXIS_TILT = new DirectiveDef(
    DirectiveType.RUN_TIME, Module.CTF_CORRECTION, "correctForXAxisTilt");

  public static final DirectiveDef AUTO_FIT_RANGE_AND_STEP =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.CTF_PLOTTING, "autoFitRangeAndStep");

  public static final DirectiveDef DELETE_OLD_FILES =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.EXCLUDE_VIEWS, "deleteOldFiles");

  public static final DirectiveDef FIDUCIALLESS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.FIDUCIALS, "fiducialless");
  public static final DirectiveDef SEEDING_METHOD =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.FIDUCIALS, "seedingMethod");
  public static final DirectiveDef TRACKING_METHOD =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.FIDUCIALS, "trackingMethod");

  public static final DirectiveDef BINNING_FOR_GOLD_ERASING =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.GOLD_ERASING, BINNING_NAME);
  public static final DirectiveDef EXTRA_DIAMETER =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.GOLD_ERASING, "extraDiameter");
  public static final DirectiveDef THICKNESS_FOR_GOLD_ERASING =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.GOLD_ERASING, THICKNESS_NAME);

  public static final DirectiveDef ITERATIONS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.NAD, "iterations");
  public static final DirectiveDef K_VALUE =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.NAD, "Kvalue");
  public static final DirectiveDef CHUNK_MEMORY_MB =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.NAD, "chunkMemoryMB");

  public static final DirectiveDef ADJUST_TILT_ANGLES =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.PATCH_TRACKING, "adjustTiltAngles");
  public static final DirectiveDef CONTOUR_PIECES =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.PATCH_TRACKING, "contourPieces");
  public static final DirectiveDef RAW_BOUNDARY_MODEL_FOR_PATCH_TRACKING =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.PATCH_TRACKING, "rawBoundaryModel");

  public static final DirectiveDef BIN_BY_FACTOR_FOR_POSITIONING = new DirectiveDef(
    DirectiveType.RUN_TIME, Module.POSITIONING, RUN_TIME_BIN_BY_FACTOR_NAME);
  public static final DirectiveDef CENTER_ON_GOLD =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.POSITIONING, "centerOnGold");
  public static final DirectiveDef HAS_GOLD_BEADS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.POSITIONING, "hasGoldBeads");
  public static final DirectiveDef SAMPLE_TYPE =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.POSITIONING, "sampleType");
  public static final DirectiveDef THICKNESS_FOR_POSITIONING =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.POSITIONING, THICKNESS_NAME);
  public static final DirectiveDef WHOLE_TOMOGRAM =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.POSITIONING, "wholeTomogram");

  public static final DirectiveDef DO_TRIMVOL =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.POSTPROCESS, "doTrimvol");

  public static final DirectiveDef ARCHIVE_ORIGINAL =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.PREPROCESSING, "archiveOriginal");
  public static final DirectiveDef DARK_EXCLUDE_RATIO =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.PREPROCESSING, "darkExcludeRatio");
  public static final DirectiveDef DARK_EXCLUDE_FRACTION =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.PREPROCESSING, "darkExcludeFraction");
  public static final DirectiveDef END_EXCLUDE_CRITERION =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.PREPROCESSING, "endExcludeCriterion");
  public static final DirectiveDef REMOVE_EXCLUDED_VIEWS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.PREPROCESSING, "removeExcludedViews");
  public static final DirectiveDef REMOVE_XRAYS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.PREPROCESSING, "removeXrays");

  public static final DirectiveDef NUMBER_OF_MARKERS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.RAPTOR, "numberOfMarkers");
  public static final DirectiveDef USE_ALIGNED_STACK =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.RAPTOR, "useAlignedStack");

  public static final DirectiveDef BINNED_THICKNESS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.RECONSTRUCTION, "binnedThickness");
  public static final DirectiveDef DO_BACKPROJ_ALSO =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.RECONSTRUCTION, "doBackprojAlso");
  public static final DirectiveDef EXTRA_THICKNESS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.RECONSTRUCTION, "extraThickness");
  public static final DirectiveDef FALLBACK_THICKNESS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.RECONSTRUCTION, "fallbackThickness");
  public static final DirectiveDef USE_SIRT =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.RECONSTRUCTION, "useSirt");

  public static final DirectiveDef MIN_MEASUREMENT_RATIO = new DirectiveDef(
    DirectiveType.RUN_TIME, Module.RESTRICT_ALIGN, "minMeasurementRatio");
  public static final DirectiveDef ORDER_OF_RESTRICTIONS = new DirectiveDef(
    DirectiveType.RUN_TIME, Module.RESTRICT_ALIGN, "orderOfRestrictions");
  public static final DirectiveDef SKIP_BEAM_TILT_WITH_ONE_ROT = new DirectiveDef(
    DirectiveType.RUN_TIME, Module.RESTRICT_ALIGN, "skipBeamTiltWithOneRot");
  public static final DirectiveDef TARGET_MEASUREMENT_RATIO = new DirectiveDef(
    DirectiveType.RUN_TIME, Module.RESTRICT_ALIGN, "targetMeasurementRatio");

  public static final DirectiveDef RAW_BOUNDARY_MODEL_FOR_SEED_FINDING =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.SEED_FINDING, "rawBoundaryModel");

  public static final DirectiveDef ENABLE_STRETCHING =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.TILT_ALIGNMENT, "enableStretching");

  public static final DirectiveDef DO_A_OR_B_OF_DUAL_AXIS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.TRIMVOL, "doAorBofDualAxis");
  public static final DirectiveDef DO_SIRT_IF_BOTH_FOR_TRIMVOL =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.TRIMVOL, "doSIRTifBoth");
  public static final DirectiveDef FIND_SEC_ADD_THICKNESS =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.TRIMVOL, "findSecAddThickness");
  public static final DirectiveDef REORIENT =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.TRIMVOL, "reorient");
  public static final DirectiveDef SIZE_IN_X =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.TRIMVOL, "sizeInX");
  public static final DirectiveDef SIZE_IN_Y =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.TRIMVOL, "sizeInY");
  public static final DirectiveDef SCALE_FROM_X =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.TRIMVOL, "scaleFromX");
  public static final DirectiveDef SCALE_FROM_Y =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.TRIMVOL, "scaleFromY");
  public static final DirectiveDef SCALE_FROM_Z =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.TRIMVOL, "scaleFromZ");
  public static final DirectiveDef SCALE_TO_MEAN_SD =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.TRIMVOL, "scaleToMeanSD");
  public static final DirectiveDef THICKNESS_FOR_TRIMVOL =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.TRIMVOL, THICKNESS_NAME);

  public static final DirectiveDef REPLACE_STEP_NINE =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.REPLACE_STEP, "9");
  public static final DirectiveDef REPLACE_STEP_THIRTEEN =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.REPLACE_STEP, "13");
  public static final DirectiveDef RUN_AFTER_STEP_NINE =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.RUN_AFTER_STEP, "9");
  public static final DirectiveDef RUN_AFTER_STEP_THIRTEEN =
    new DirectiveDef(DirectiveType.RUN_TIME, Module.RUN_AFTER_STEP, "13");

  // comparam

  public static final DirectiveDef LOCAL_ALIGNMENTS = new DirectiveDef(
    DirectiveType.COM_PARAM, Comfile.ALIGN, Command.TILTALIGN, "LocalAlignments");
  public static final DirectiveDef SURFACES_TO_ANALYZE = new DirectiveDef(
    DirectiveType.COM_PARAM, Comfile.ALIGN, Command.TILTALIGN, "SurfacesToAnalyze");

  public static final DirectiveDef TARGET_NUMBER_OF_BEADS =
    new DirectiveDef(DirectiveType.COM_PARAM, Comfile.AUTOFIDSEED, Command.AUTOFIDSEED,
      "TargetNumberOfBeads");
  public static final DirectiveDef MIN_GUESS_NUM_BEADS =
    new DirectiveDef(DirectiveType.COM_PARAM, Comfile.AUTOFIDSEED, Command.AUTOFIDSEED,
      "MinGuessNumBeads");
  public static final DirectiveDef TARGET_DENSITY_OF_BEADS =
    new DirectiveDef(DirectiveType.COM_PARAM, Comfile.AUTOFIDSEED, Command.AUTOFIDSEED,
      "TargetDensityOfBeads");
  public static final DirectiveDef TWO_SURFACES = new DirectiveDef(
    DirectiveType.COM_PARAM, Comfile.AUTOFIDSEED, Command.AUTOFIDSEED, "TwoSurfaces");

  public static final DirectiveDef MODEL_FILE = new DirectiveDef(DirectiveType.COM_PARAM,
    Comfile.ERASER, Command.CCDERASER, "ModelFile");

  public static final DirectiveDef EXPAND_CIRCLE_ITERATIONS =
    new DirectiveDef(DirectiveType.COM_PARAM, Comfile.GOLD_ERASER, Command.CCDERASER,
      "ExpandCircleIterations");

  public static final DirectiveDef BIN_BY_FACTOR_FOR_PREBLEND =
    new DirectiveDef(DirectiveType.COM_PARAM, Comfile.PREBLEND, Command.BLENDMONT,
      COM_PARAM_BIN_BY_FACTOR_NAME);

  public static final DirectiveDef BIN_BY_FACTOR_FOR_PRENEWST =
    new DirectiveDef(DirectiveType.COM_PARAM, Comfile.PRENEWST, Command.NEWSTACK,
      COM_PARAM_BIN_BY_FACTOR_NAME);

  public static final DirectiveDef LEAVE_ITERATIONS = new DirectiveDef(
    DirectiveType.COM_PARAM, Comfile.SIRTSETUP, Command.SIRTSETUP, "LeaveIterations");
  public static final DirectiveDef SCALE_TO_INTEGER = new DirectiveDef(
    DirectiveType.COM_PARAM, Comfile.SIRTSETUP, Command.SIRTSETUP, "ScaleToInteger");

  // comparam.tilt.tilt.FakeSIRTiterations
  public static final DirectiveDef FAKE_SIRT_ITERATIONS = new DirectiveDef(
    DirectiveType.COM_PARAM, Comfile.TILT, Command.TILT, "FakeSIRTiterations");
  public static final DirectiveDef THICKNESS_FOR_TILT =
    new DirectiveDef(DirectiveType.COM_PARAM, Comfile.TILT, Command.TILT, "THICKNESS");

  public static final DirectiveDef LOCAL_AREA_TARGET_SIZE = new DirectiveDef(
    DirectiveType.COM_PARAM, Comfile.TRACK, Command.BEADTRACK, "LocalAreaTargetSize");

  public static final DirectiveDef LENGTH_OF_PIECES = new DirectiveDef(
    DirectiveType.COM_PARAM, Comfile.XCORR_PT, Command.IMODCHOPCONTS, "LengthOfPieces");

  public static final DirectiveDef SEARCH_MAG_CHANGES = new DirectiveDef(
    DirectiveType.COM_PARAM, Comfile.XCORR, Command.TILTXCORR, "SearchMagChanges");

  public static final DirectiveDef NUMBER_OF_PATCHES_X_AND_Y = new DirectiveDef(
    DirectiveType.COM_PARAM, Comfile.XCORR_PT, Command.TILTXCORR, "NumberOfPatchesXandY");
  public static final DirectiveDef OVERLAP_OF_PATCHES_X_AND_Y =
    new DirectiveDef(DirectiveType.COM_PARAM, Comfile.XCORR_PT, Command.TILTXCORR,
      "OverlapOfPatchesXandY");
  public static final DirectiveDef SIZE_OF_PATCHES_X_AND_Y = new DirectiveDef(
    DirectiveType.COM_PARAM, Comfile.XCORR_PT, Command.TILTXCORR, "SizeOfPatchesXandY");

  private final DirectiveType directiveType;
  private final String module;
  private final String comfile;
  private final String command;
  private final String name;

  // preconstructed: Should only be true when the instance is a static member of this
  // class.
  private final boolean preconstructed;

  // aOnlyDirective: Do not change if preconstructed is true. True for A axis CopyArg
  // if B axis CopyArg twin exists. Example setupset.copyarg.skip.
  private boolean aOnlyDirective = false;
  // bOnlyDirective: Do not change if preconstructed is true. True for B axis CopyArg.
  // Example
  // setupset.copyarg.bskip.
  private boolean bOnlyDirective = false;;
  private boolean bool = false;
  private boolean templateA = false;
  private boolean templateB = false;
  private boolean batchA = false;
  private boolean batchB = false;
  private boolean directiveDescrLoaded = false;
  private String description = null;

  /**
   * General constructor.  Saves to MAP if the instance doesn't have a duplicate key.
   * Saves directives with a separate B directive twice.
   *
   * @param directiveType
   * @param module
   * @param comfile
   * @param command
   * @param name - required
   */
  private DirectiveDef(final DirectiveType directiveType, final String module,
    final String comfile, final String command, final String name,
    final boolean aOnlyDirective, final boolean bOnlyDirective,
    final boolean preconstructed) {
    this.directiveType = directiveType;
    this.module = module;
    this.comfile = comfile;
    this.command = command;
    this.name = name;
    this.aOnlyDirective = aOnlyDirective;
    this.bOnlyDirective = bOnlyDirective;
    this.preconstructed = preconstructed;
    String descrKey = getStandardKey();
    if (descrKey != null && !MAP.containsKey(descrKey)) {
      MAP.put(descrKey, this);
    }
  }

  /**
   * A null pairAxisID is NOT treated as axis A.  When it is null the same
   * directiveDef will be returned.
   * @param pairAxisID
   * @return
   */
  DirectiveDef getAxisIDInstance(final AxisID pairAxisID) {
    if (pairAxisID == null || directiveType != DirectiveType.COPY_ARG
      || !aOnlyDirective && !bOnlyDirective
      || (pairAxisID != AxisID.SECOND && aOnlyDirective)
      || (pairAxisID == AxisID.SECOND && bOnlyDirective)) {
      return this;
    }
    return getInstance(switchStandardKey(pairAxisID));
  }

  /**
   * Constructor for precontructed setupset.copyarg and setupset.  Keep private.
   *
   * @param directiveType
   * @param name
   */
  private DirectiveDef(final DirectiveType directiveType, final String name) {
    this(directiveType, null, null, null, name, false, false, true);
  }

  /**
   * Constructor for precontructed setupset.copyarg.  Keep private.
   *
   * @param directiveType
   * @param name
   */
  private DirectiveDef(final DirectiveType directiveType, final String name,
    final boolean aOnlyDirective) {
    this(directiveType, null, null, null, name, aOnlyDirective, false, true);
  }

  /**
   * getInstance function for precontructed setupset.copyarg.  Keep private.
   *
   * @param directiveType
   * @param name
   */
  private static DirectiveDef getBInstance(DirectiveType directiveType,
    final String name) {
    return new DirectiveDef(directiveType, null, null, null, name, false, true, true);
  }

  /**
   * Constructor for preconstructed runtime.  Keep private.
   *
   * @param directiveType
   * @param module
   * @param name
   */
  private DirectiveDef(final DirectiveType directiveType, final Module module,
    final String name) {
    this(directiveType, module != null ? module.toString() : null, null, null, name,
      false, false, true);
  }

  /**
   * Constructor for preconstructed comparam.  Keep private.
   *
   * @param directiveType
   * @param comfile
   * @param command
   * @param name
   */
  private DirectiveDef(final DirectiveType directiveType, final Comfile comfile,
    final Command command, final String name) {
    this(directiveType, null, comfile != null ? comfile.toString() : null,
      command != null ? command.toString() : null, name, false, false, true);
  }

  public static DirectiveDef getInstance(final String directive) {
    return getInstance(directive, null);
  }

  public static DirectiveDef getInstanceFromCsv(final String directive,
    final DirectiveDef prevDirectiveDefFromCsv) {
    return getInstance(directive, prevDirectiveDefFromCsv);
  }

  /**
   * Gets or builds a directiveDef from any directive string.  Works with directives from
   * an autodoc file, or from directives.csv.  Saves the instance in MAP under the
   * standard key if it is new.  Returns an existing directiveDef if it already exists.
   * The directive type must be known.  The module, comfile, and command are treated as
   * strings and don't have to be recognized.  Assumes that a comfile ending in a or b has
   * an axis letter.
   * 
   * Can create directiveDefs that are not in the directives.csv file.
   * 
   * Corrects aOnlyDirective and bOnlyDirective when both the A and B members of a CopyArg
   * pair are present.  PrevDirectiveDefFromCsv makes the correction process slightly more
   * reliable if the directives are coming from the directives.csv file.
   * 
   * Accepted forms:
   * setupset.copyarg.name
   * setupset.copyarg.bname
   * setupset.name
   * runtime.module.any.name
   * runtime.module.name
   * runtime.module..name
   * runtime.module.a.name
   * runtime.module.b.name
   * comparam.comfile.command.name
   * comparam.comfilea.command.name
   * comparam.comfileb.command.name
   *
   * @see static getStandardKey
   * @param directive
   * @param prevDirectiveDefFromCsv - set when loading from the directives.cvs file, otherwise leave null
   * @return
   */
  private static DirectiveDef getInstance(final String directive,
    final DirectiveDef prevDirectiveDefFromCsv) {
    if (directive == null) {
      return null;
    }
    // Construct or find directiveDef.
    // First see if the directive string is a key to a saved directive
    DirectiveDef directiveDef = MAP.get(directive);
    if (directiveDef == null) {
      // So far can't find it, pull the fields out of the directive string..
      String[] array = directive.split("\\" + AutodocTokenizer.SEPARATOR_CHAR);
      DirectiveType directiveType = DirectiveType.getInstance(array);
      String module = null;
      String comfile = null;
      String command = null;
      String name = null;
      int index = -1;
      if (directiveType == DirectiveType.COPY_ARG) {
        index = 2;
        if (array.length > index) {
          name = array[index];
        }
      }
      else if (directiveType == DirectiveType.SETUP_SET) {
        // name
        index = 1;
        if (array.length > index) {
          name = array[index];
        }
      }
      else if (directiveType == DirectiveType.RUN_TIME) {
        // module
        index = 1;
        if (array.length > index) {
          module = array[index];
        }
        // Check for optional axisID
        index++;
        if (array.length > index + 1 && (array[index] == null || array[index].isEmpty()
          || array[index].equals(RUN_TIME_ANY_AXIS_TAG)
          || array[index].equals(RUN_TIME_A_AXIS_TAG)
          || array[index].equals(B_AXIS_TAG))) {
          // found axisID - will be replaced with "any" in the key
          index++;
        }
        // name
        if (array.length > index) {
          name = array[index];
        }
      }
      else if (directiveType == DirectiveType.COM_PARAM) {
        // comfile
        index = 1;
        if (array.length > index) {
          comfile = array[index];
          // check for axisID - the a or b addition to comfile names
          if (comfile != null && comfile.length() > 1
            && (comfile.endsWith(AxisID.FIRST.getExtension())
              || comfile.endsWith(AxisID.SECOND.getExtension()))) {
            // found axisID - stripping from comfile
            comfile = comfile.substring(0, comfile.length() - 1);
          }
        }
        // command
        index++;
        if (array.length > index) {
          command = array[index];
        }
        // name
        index++;
        if (array.length > index) {
          name = array[index];
        }
      }
      if (name == null || name.isEmpty()) {
        return null;
      }
      // Continue constructing or finding directiveDef
      //
      // With the axis information turned off, the function getSimpleStandardKey will get
      // the default key based on fields from the directives string.
      directiveDef =
        MAP.get(getStandardKey(directiveType, module, comfile, command, name));
      if (directiveDef == null) {
        directiveDef = new DirectiveDef(directiveType, module, comfile, command, name,
          false, false, false);
      }
    }
    // The preconstructed ones don't have to be corrected. Only CopyArg has paired
    // directives.
    if (directiveDef.preconstructed
      || directiveDef.directiveType != DirectiveType.COPY_ARG) {
      return directiveDef;
    }
    // If there are paired A and B copyArg directives available then correct them.
    DirectiveDef aDirectiveDef = null;
    DirectiveDef bDirectiveDef = null;
    if (prevDirectiveDefFromCsv != null
      && directiveDef.name.equals(B_AXIS_TAG + prevDirectiveDefFromCsv.name)) {
      // In the directives.csv file, A axis copyArg directives always comes right before
      // the corresponding B directive.
      aDirectiveDef = prevDirectiveDefFromCsv;
      bDirectiveDef = directiveDef;
    }
    else {
      // Look for a matching B directive
      bDirectiveDef =
        MAP.get(getStandardKey(directiveDef.directiveType, directiveDef.module,
          directiveDef.comfile, directiveDef.command, B_AXIS_TAG + directiveDef.name));
      if (bDirectiveDef != null) {
        aDirectiveDef = directiveDef;
      }
      else if (directiveDef.name.startsWith(B_AXIS_TAG)) {
        // Look for a matching A directive
        aDirectiveDef =
          MAP.get(getStandardKey(directiveDef.directiveType, directiveDef.module,
            directiveDef.comfile, directiveDef.command, directiveDef.name.substring(1)));
        if (aDirectiveDef != null) {
          bDirectiveDef = directiveDef;
        }
      }
    }
    // If a pair was found, correct them.
    if (aDirectiveDef != null && bDirectiveDef != null) {
      if (!aDirectiveDef.preconstructed) {
        aDirectiveDef.aOnlyDirective = true;
      }
      if (!bDirectiveDef.preconstructed) {
        bDirectiveDef.bOnlyDirective = true;
      }
    }
    return directiveDef;
  }

  boolean isAOnlyDirective() {
    return aOnlyDirective;
  }

  boolean isBOnlyDirective() {
    return bOnlyDirective;
  }

  /**
   * Returns the standard key to this directive, unless this directive is part of a pair.
   * In that case it returns the key of the dirctive that cooresponds to pairAxisID
   * if pairAxisID is not null.
   * @param pairAxisID
   * @return
   */
  public String switchStandardKey(final AxisID pairAxisID) {
    return getStandardKey(directiveType, module, comfile, command,
      getPairName(pairAxisID));
  }

  /**
   * If this is a paired dirctive (copyArg and representing a directive that has an A axis
   * and B axis form), return the name corresponding to pairAxisID.  A null pairAxisID
   * returns the name of this directiveDef.
   * 
   * @param pairAxisID
   * @return
   */
  private String getPairName(final AxisID pairAxisID) {
    if (pairAxisID == null || directiveType != DirectiveType.COPY_ARG
      || (!aOnlyDirective && !bOnlyDirective)
      || (aOnlyDirective && pairAxisID != AxisID.SECOND)
      || (bOnlyDirective && pairAxisID == AxisID.SECOND)) {
      return name;
    }
    if (pairAxisID != AxisID.SECOND) {
      return name.substring(1);
    }
    return B_AXIS_TAG + name;
  }

  /**
   * Returns the key to the MAP in this class and and the one in DirectiveDescrFile.
   * This class's hashCode also uses this key (with copyArgAxisID null).
   * 
   * Can convert an A axis CopyArg def into a B axis key with the following settings:
   * aOnlyDirective && copyArgAxisID == AxisID.SECOND && !bOnlyDirective)
   * 
   * Descr key formats:
   * setupset.copyarg.name
   * setupset.copyarg.bname
   * setupset.name
   * runtime.module.any.name
   * comparam.comfile.command.name
   * 
   * @param directiveType
   * @param module
   * @param comfile
   * @param command
   * @param name
   * @param aOnlyDirective
   * @return
   */
  private static String getStandardKey(final DirectiveType directiveType,
    final String module, final String comfile, final String command, final String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }
    if (directiveType == DirectiveType.COPY_ARG) {
      return DirectiveType.SETUP_SET.toString() + AutodocTokenizer.SEPARATOR_CHAR
        + directiveType.toString() + AutodocTokenizer.SEPARATOR_CHAR + name;
    }
    if (directiveType == DirectiveType.SETUP_SET) {
      return directiveType.toString() + AutodocTokenizer.SEPARATOR_CHAR + name;
    }
    if (directiveType == DirectiveType.RUN_TIME) {
      if (module == null || module.isEmpty()) {
        return null;
      }
      return directiveType.toString() + AutodocTokenizer.SEPARATOR_CHAR + module
        + AutodocTokenizer.SEPARATOR_CHAR + RUN_TIME_ANY_AXIS_TAG
        + AutodocTokenizer.SEPARATOR_CHAR + name;
    }
    if (directiveType == DirectiveType.COM_PARAM) {
      if (comfile == null || comfile.isEmpty() || command == null || command.isEmpty()) {
        return null;
      }
      return directiveType.toString() + AutodocTokenizer.SEPARATOR_CHAR + comfile
        + AutodocTokenizer.SEPARATOR_CHAR + command + AutodocTokenizer.SEPARATOR_CHAR
        + name;
    }
    return null;
  }

  /**
   * Returns a key to the MAP.  Always returns the A version of a copyarg directive key.
   * @return
   */
  public String getStandardKey() {
    return getStandardKey(directiveType, module, comfile, command, name);
  }

  /**
   * Returns the Axis of copyArgDirective, if this instance is part of a pair of CopyArg
   * directives.  Returns null instead of A axis.  Also return null in case of failure.
   * @param copyArgDirective
   * @return
   */
  public AxisID getCopyArgAxisID(final String copyArgDirective) {
    if (directiveType != DirectiveType.COPY_ARG
      || DirectiveType.getInstance(copyArgDirective) != directiveType) {
      return null;
    }
    AxisID copyArgAxisID = null;
    // If this directive has a separate b directive, or is a b directive, use the string
    // parameter to find out the axisID to use.
    if (directiveType == DirectiveType.COPY_ARG && (aOnlyDirective || bOnlyDirective)
      && copyArgDirective != null) {
      String[] array = copyArgDirective.split("\\" + AutodocTokenizer.SEPARATOR_CHAR);
      int nameIndex = 2;
      if (array != null && array.length > nameIndex && array[nameIndex] != null
        && array[nameIndex].length() > B_AXIS_TAG.length()
        && array[nameIndex].startsWith(B_AXIS_TAG)) {
        return AxisID.SECOND;
      }
    }
    return null;
  }

  /**
   * @return
   */
  DirectiveType getDirectiveType() {
    return directiveType;
  }

  /**
   * Load information about directive from the directive.csv file.
   */
  boolean loadDirectiveDescr() {
    if (directiveDescrLoaded) {
      return true;
    }
    DirectiveDescrElement element =
      DirectiveDescrFile.INSTANCE.get(switchStandardKey(AxisID.FIRST));
    if (element != null) {
      DirectiveValueType type = element.getValueType();
      if (type == DirectiveValueType.BOOLEAN) {
        bool = true;
      }
      templateA = element.isTemplate();
      batchA = element.isBatch();
    }
    if (directiveType == DirectiveType.COPY_ARG) {
      element = DirectiveDescrFile.INSTANCE.get(switchStandardKey(AxisID.SECOND));
      if (element != null) {
        templateB = element.isTemplate();
        batchB = element.isBatch();
      }
    }
    if (!aOnlyDirective && !bOnlyDirective) {
      templateB = templateA;
      batchB = batchA;
    }
    if (element != null) {
      description = element.getDescription();
    }
    else {
      System.err.println("\nError:  Unknown directive: " + toString() + "\n");
      return false;
    }
    directiveDescrLoaded = true;
    return true;
  }

  boolean hasSecondaryMatch(final AxisID axisID) {
    if (directiveType == DirectiveType.COPY_ARG) {
      return false;
    }
    return true;
  }

  public boolean isCopyArg() {
    return directiveType == DirectiveType.COPY_ARG;
  }

  public boolean isComparam() {
    return directiveType == DirectiveType.COM_PARAM;
  }

  public boolean isRuntime() {
    return directiveType == DirectiveType.RUN_TIME;
  }

  public boolean isBoolean() {
    loadDirectiveDescr();
    return bool;
  }

  public static String getBooleanValue(final boolean bool) {
    if (bool) {
      return TRUE_VALUE;
    }
    return FALSE_VALUE;
  }

  /**
   * Returns true if value equals TRUE_VALUE.  Otherwise returns false
   * @param value
   * @return
   */
  public static boolean convertToBoolean(final String value) {
    if (value == null) {
      return false;
    }
    return value.equals(TRUE_VALUE);
  }

  public static boolean isValidBooleanValue(String value) {
    if (value == null) {
      return false;
    }
    value = value.trim();
    return value.equals(TRUE_VALUE) || value.equals(FALSE_VALUE);
  }

  public static String getBooleanValue(final String value) {
    if (isValidBooleanValue(value)) {
      return value;
    }
    if (value == null || value.equals("")) {
      return FALSE_VALUE;
    }
    return TRUE_VALUE;
  }

  /**
   * Gets a tooltip from the description in the directive description file.  No tooltip is
   * loaded for comparam directives.
   * @return
   */
  public String getTooltip() {
    loadDirectiveDescr();
    return description + " (" + toString() + ")";
  }

  boolean isTemplate(final AxisID axisID) {
    loadDirectiveDescr();
    if (axisID == AxisID.SECOND) {
      return templateB;
    }
    return templateA;
  }

  boolean isBatch(final AxisID axisID) {
    loadDirectiveDescr();
    if (axisID == AxisID.SECOND) {
      return batchB;
    }
    return batchA;
  }

  /**
   * Get name according to which match is being done
   * * <p>How well an attribute matches an axis.  Primary overrides secondary match even
   * in a lower precedence directive file.</p>
   * <p>Copyarg:
   * No Secondary match for CopyArg.  Paired directives are only used on their own axis.
   * Unpaired directives are a First level match for both axes.  A null pairAxisID causes
   * the name of this directive to be returned.</p>
   * @param match
   * @param pairAxisID
   * @return
   */
  String getName(final Match match, final AxisID pairAxisID) {
    if (directiveType != DirectiveType.COPY_ARG) {
      return name;
    }
    // Get the name for copyArg directives.
    if (match == Match.SECONDARY) {
      return null;
    }
    if (pairAxisID == null || !aOnlyDirective && !bOnlyDirective) {
      return name;
    }
    // Get the name for paired copyArg directives.
    if ((aOnlyDirective && pairAxisID != AxisID.SECOND)
      || (bOnlyDirective && pairAxisID == AxisID.SECOND)) {
      return name;
    }
    return null;
  }

  /**
   * @return module name for runtime directives
   */
  public String getModule() {
    return module;
  }

  /**
   * Returns the axis tag.
   * <p>Runtime and Comparam:</p>
   * <p>axis = null:  Primary match: any.  Secondary match: a, does not match b</p>
   * <p>axis = only: Primary match: any.  Secondary match: a, does not match b</p>
   * <p>axis = first: Primary match: a.  Secondary match: any, does not match b</p>
   * <p>axis = second: Primary match: b.  Secondary match: any, does not match a</p>
   *
   * @param match
   * @param axisID
   * @return axis name for runtime directives
   */
  String getAxis(final Match match, final AxisID axisID) {
    if (directiveType != DirectiveType.RUN_TIME) {
      return null;
    }
    if (match == Match.PRIMARY) {
      if (axisID == AxisID.FIRST) {
        return RUN_TIME_A_AXIS_TAG;
      }
      if (axisID == AxisID.SECOND) {
        return B_AXIS_TAG;
      }
    }
    else if (match == Match.SECONDARY) {
      if (axisID == null || axisID == AxisID.ONLY) {
        return RUN_TIME_A_AXIS_TAG;
      }
    }
    return RUN_TIME_ANY_AXIS_TAG;
  }

  /**
   * Same functionality as getAxis, but for comparam instead of runtime
   * @param match
   * @param axisID
   * @return axis name for comparam directives
   */
  String getComfile(final Match match, final AxisID axisID) {
    if (directiveType != DirectiveType.COM_PARAM) {
      return null;
    }
    if (match == Match.PRIMARY) {
      if (axisID == AxisID.FIRST) {
        return comfile + AxisID.FIRST.getExtension();
      }
      if (axisID == AxisID.SECOND) {
        return comfile + AxisID.SECOND.getExtension();
      }
    }
    else if (match == Match.SECONDARY) {
      if (axisID == null || axisID == AxisID.ONLY) {
        return comfile + AxisID.FIRST.getExtension();
      }
    }
    return comfile;
  }

  /**
   * @return the command name element for comparam directives
   */
  public String getCommand() {
    return command;
  }

  public String getName() {
    return name;
  }

  /**
   * Get the correct name for the axis without having to use the other axis DirectiveDef
   * instance.  A null pairAxisID always returns the name from this directiveDef.
   *
   * @param pairAxisID
   * @return
   */
  public String getName(final AxisID pairAxisID) {
    if (pairAxisID == null || directiveType != DirectiveType.COPY_ARG) {
      return name;
    }
    if ((aOnlyDirective && pairAxisID == AxisID.SECOND)) {
      return B_AXIS_TAG + name;
    }
    if (bOnlyDirective && pairAxisID != AxisID.SECOND) {
      return name.substring(1);
    }
    return name;
  }

  /**
   * Returns the full directive string with the axis tag.  An unrecognized directive type
   * causes the function to return all of directive elements that are set, followed by
   * the axisID extension.
   *
   * @return
   */
  public String getDirective() {
    return getPrefix() + getAxisTag() + getPostfix();
  }

  /**
   * Returns the directive string with no axis tag.  Each instance should return a unique string.
   */
  public String toString() {
    return getPrefix() + getPostfix();
  }

  public int hashCode() {
    String key = getStandardKey();
    if (key != null) {
      return key.hashCode();
    }
    return toString().hashCode();
  }

  public boolean equals(final Object object) {
    if (object == null) {
      return false;
    }
    if (this == object) {
      return true;
    }
    return hashCode() == object.hashCode();
  }

  boolean equalsName(final String name, final AxisID pairAxisID) {
    if (getName(pairAxisID).equals(name)) {
      return true;
    }
    return false;
  }

  /**
   * Gets the default axis tag.
   * @return
   */
  private String getAxisTag() {
    if (directiveType == DirectiveType.RUN_TIME) {
      return RUN_TIME_ANY_AXIS_TAG;
    }
    return "";
  }

  /**
   * Creates the directive string up to the axis tag, or the whole directive string for
   * directives with no axis tag.
   *
   * @return
   */
  private String getPrefix() {
    if (directiveType == DirectiveType.COPY_ARG) {
      return directiveType.getKey() + AutodocTokenizer.SEPARATOR_CHAR + name;
    }
    if (directiveType == DirectiveType.SETUP_SET) {
      return directiveType.getKey() + AutodocTokenizer.SEPARATOR_CHAR + name;
    }
    if (directiveType == DirectiveType.RUN_TIME) {
      return directiveType.getKey() + AutodocTokenizer.SEPARATOR_CHAR + module
        + AutodocTokenizer.SEPARATOR_CHAR;
    }
    if (directiveType == DirectiveType.COM_PARAM) {
      return directiveType.getKey() + AutodocTokenizer.SEPARATOR_CHAR + comfile;
    }
    return (directiveType != null ? directiveType.getKey() : "")
      + (module != null ? AutodocTokenizer.SEPARATOR_CHAR + module : module)
      + (comfile != null ? AutodocTokenizer.SEPARATOR_CHAR + comfile : comfile)
      + (command != null ? AutodocTokenizer.SEPARATOR_CHAR + command : command)
      + (name != null ? AutodocTokenizer.SEPARATOR_CHAR + name : name);
  }

  /**
   * Creates the directive string after the axis tag.  Returns nothing for directives with
   * no axis tag.
   *
   * @return
   */
  private String getPostfix() {
    if (directiveType == DirectiveType.COPY_ARG) {
      return "";
    }
    if (directiveType == DirectiveType.SETUP_SET) {
      return "";
    }
    if (directiveType == DirectiveType.RUN_TIME) {
      return AutodocTokenizer.SEPARATOR_CHAR + name;
    }
    if (directiveType == DirectiveType.COM_PARAM) {
      return AutodocTokenizer.SEPARATOR_CHAR + command + AutodocTokenizer.SEPARATOR_CHAR
        + name;
    }
    return "";
  }
}
