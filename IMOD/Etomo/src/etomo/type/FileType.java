package etomo.type;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.logic.DatasetTool;
import etomo.process.ImodManager;
import etomo.util.Utilities;

/**
 * <p>Description: A class that can describe the types of files used in Etomo.
 * Gives a description of the name where possible.  Includes the ImodManager
 * key if it exists.  Gives the location of the file if it is in a subdirectory
 * rather then in the main dataset directory.  The files types with name
 * descriptions are stored in a list.  Units test are used to prevent name
 * collisions of these files.</p>
 * <p/>
 * <p>Copyright: Copyright 2008 - 2022 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 *          <p/>
 *          <p> $Log$
 *          <p> Revision 1.14  2011/06/30 00:20:23  sueh
 *          <p> Bug# 1502 In getFileName removed Thread.dumpStack call.
 *          <p>
 *          <p> Revision 1.13  2011/05/03 02:54:19  sueh
 *          <p> bug# 1416 Added tilt_for_sirt.com, which has its axis letter after "tilt".  Modified equals(...String filename) to
 *          <p> handle an extension which doesn't start with a ".".  Using static getInstance functions to avoid having the
 *          <p> instances jump constructors.  Add template boolean so that a warning is printed when a getFileName is run on
 *          <p> a template file type.  Added getTemplate to avoid printing a warning when a template file name is intentionally
 *          <p> retrieved.
 *          <p>
 *          <p> Revision 1.12  2011/04/09 06:34:52  sueh
 *          <p> bug# 1416 Added composite and composite file types (TILT_OUTPUT, and SIRT file types).  Made the functions
 *          <p> work with composite.  Removed DUAL_ and SINGLE_AXIS_TOMOGRAM.  Replaced toString and toString2 with
 *          <p> getDescription and getIModManagerKey2.
 *          <p>
 *          <p> Revision 1.11  2011/04/04 17:06:06  sueh
 *          <p> bug# 1416 Added/modified ALIGNED_STACK, description, DUAL_AXIS_TOMOGRAM,
 *          <p> SINGLE_AXIS_TOMOGRAM, SIRT_OUTPUT_TEMPLACE, SIRT_SCALE_OUTPUT_TEMPLATE,
 *          <p> SIRTSETUP_COMSCRIPT, TILT_COMSCRIPT, constructors, getDescription, getFile, getFileName, getRoot,
 *          <p> hasFixedName.  Removed hasNameDescription.
 *          <p>
 *          <p> Revision 1.10  2011/02/15 04:56:37  sueh
 *          <p> bug# 1437 Reformatting.
 *          <p>
 *          <p> Revision 1.9  2010/04/28 16:27:44  sueh
 *          <p> bug# 1344 Added a second imodManagerKey and inSubdirectory
 *          <p> boolean.  Collecting the instances with file descriptions into
 *          <p> nameFileTypeList.  Implemented getting the subdirectory for peet file
 *          <p> types.  Added getInstance functions so a FileType instance can be derived
 *          <p> from a name description or a file name.  Added an iterator function for unit
 *          <p> testing.
 *          <p>
 *          <p> Revision 1.8  2010/03/09 22:06:20  sueh
 *          <p> bug# 1325 Changed CCD_ERASER_INPUT to RAW_STACK.
 *          <p>
 *          <p> Revision 1.7  2010/03/03 04:57:27  sueh
 *          <p> bug# 1311 Changed FileType.NEWST_OR_BLEND_OUTPUT to
 *          <p> ALIGNED_STACK.  Added file types for patch tracking.
 *          <p>
 *          <p> Revision 1.6  2010/02/17 04:52:18  sueh
 *          <p> bug# 1301 Sorted file types to make is easier to detect duplicates.  Added
 *          <p> flattening tool file types.
 *          <p>
 *          <p> Revision 1.5  2010/01/21 21:29:58  sueh
 *          <p> bug# 1305 Added ANISOTROPIC_DIFFUSION_OUTPUT.
 *          <p>
 *          <p> Revision 1.4  2009/12/19 01:10:40  sueh
 *          <p> bug# 1294 Added FIDUCIAL_3D_MODEL and
 *          <p> SMOOTHING_ASSESSMENT_OUTPUT_MODEL.
 *          <p>
 *          <p> Revision 1.3  2009/12/11 17:28:21  sueh
 *          <p> bug# 1291 Added CCD_ERASER_INPUT and CCD_ERASER_OUTPUT.
 *          <p>
 *          <p> Revision 1.2  2009/09/01 02:30:35  sueh
 *          <p> bug# 1222 Added new file types.
 *          <p>
 *          <p> Revision 1.1  2009/06/05 02:05:14  sueh
 *          <p> bug# 1219 A class to help other classes specify files without knowing very
 *          <p> much about them.
 *          <p> </p>
 */
public class FileType extends FileKey {
  public static final Map<String, FileTypeCollection> INSTANCES =
    new HashMap<String, FileTypeCollection>();
  public static final String COM_DIR = "com";

  /**
   * @deprecated 2/1/2019
   */
  private static final List namedFileTypeList = new Vector();
  private static final boolean DEBUG = EtomoDirector.INSTANCE.getArguments().isDebug();

  /**
   * The numeric integer match strings allow longer integers and require the number
   * mentioned - which is the format.  The numbers are file numbers and there may be
   * situations where the number files is larger then anticipated.
   */
  static final class Variable {
    private final String regex;
    private final String dualRegex;
    private final boolean numeric;

    // See Tomography Guide 1.5. File Format and Naming Conventions
    static final Variable DATASET = new Variable(
      "[^\\s`'\"!#$%&*(){}\\[;/?\\|"
        + (Utilities.isMacOS() || Utilities.isWindowsOS() ? ':' : "") + "]+",
      null, false);
    // a,b, or nothing
    private static final Variable AXIS = new Variable("[ab]?", "[ab]", false);
    // dataset followed by axis
    private static final Variable DATASET_AND_AXIS =
      new Variable(DATASET.regex + AXIS.regex, DATASET.regex + AXIS.dualRegex, false);
    //
    private static final Variable ORIG_RAW_IMAGE_EXTENSION = new Variable(
      "\\" + Extension.EXTENSION_DIVIDER + Extension.getImageInputRegex(), null, false);
    // nn
    private static final Variable TWO_DIGIT_INTEGER = new Variable("\\d\\d+", null, true);
    // nnn
    private static final Variable THREE_DIGIT_INTEGER =
      new Variable("\\d\\d\\d+", null, true);
    // nnnn
    private static final Variable FOUR_DIGIT_INTEGER =
      new Variable("\\d\\d\\d\\d+", null, true);
    // x.xxx
    private static final Variable PRECISION_THREE_FLOAT =
      new Variable("\\d+\\.\\d\\d\\d", null, true);
    // 0.xxx
    private static final Variable PRECISION_THREE_FRACTION =
      new Variable("0\\.\\d\\d\\d", null, true);

    private Variable(final String regex, final String dualRegex, final boolean numeric) {
      this.regex = regex;
      this.dualRegex = dualRegex;
      this.numeric = numeric;
    }

    public String toString() {
      return regex;
    }
  }

  /**
   * Checks a file name, or path to see if contains a valid dataset name.
   * @param filePath - input image file name or path
   * @param errMsg - if this is not null, a message will be added to it if the return value is null
   * @return
   */
  public static boolean containsValidDatasetName(final String filePath,
    final StringBuilder errMsg) {
    if (filePath == null || filePath.isEmpty()) {
      if (errMsg != null) {
        errMsg.append("No input image file path found.  ");
      }
      return false;
    }
    String fileName = new File(filePath).getName();
    if (fileName.indexOf(Extension.EXTENSION_DIVIDER) != -1) {
      fileName = Utilities.removeExtension(fileName);
    }
    if (!fileName.matches(Variable.DATASET_AND_AXIS.regex)) {
      if (errMsg != null) {
        errMsg.append("file name " + filePath
          + " contains illegal characters[s].  See Tomography Guide 1.5. File Format and "
          + "Naming Conventions.  ");
      }
      return false;
    }
    return true;
  }

  // FileType instances should be placed in alphabetical order, sorted first by
  // type string and then by extension. That should make it be easier to tell
  // if we have a file name collision. FileType instances may be used by
  // multiple types of dataset managers.
  //
  // Dataset coexistence:
  //
  // .edf can coexist with:
  // any .epp
  // any ToolsManager unless it has a different dataset name
  //
  // .ejf can coexist with:
  // Other .ejfs
  // any .epp
  // any ToolsManager unless it has a different dataset name
  //
  // .epe can coexist with:
  // any .epp
  // any ToolsManager unless it has a different dataset name
  //
  // .epp can coexist with:
  // anything
  //
  // ToolsManager can coexist with
  // any .epp
  // any .edf, .ejf, or .epe as long as it has a different dataset name

  // File types with a name description

  public static final FileType ORIG_COMS_DIR =
    FileType.constructInstance(false, false, "origcoms", "");
  public static final FileType FIDUCIAL_3D_MODEL = FileType.constructImodInstance(true,
    true, "", ".3dmod", ImodManager.FIDUCIAL_MODEL_KEY);
  public static final FileType BATCH_RUN_TOMO_GLOBAL_AUTODOC =
    FileType.constructInstance(true, false, "", ".adoc");
  public static final FileType DEFAULT_BATCH_RUN_TOMO_AUTODOC =
    FileType.constructIMODDirInstance(false, false, "batchDefaults", ".adoc", COM_DIR);
  public static final FileType LOCAL_BATCH_DIRECTIVE_FILE =
    FileType.constructInstance(false, false, "batchDirective", ".adoc");
  public static final FileType LOCAL_SCOPE_TEMPLATE =
    FileType.constructInstance(false, false, "scopeTemplate", ".adoc");
  public static final FileType LOCAL_SYSTEM_TEMPLATE =
    FileType.constructInstance(false, false, "systemTemplate", ".adoc");
  public static final FileType LOCAL_USER_TEMPLATE =
    FileType.constructInstance(false, false, "userTemplate", ".adoc");
  /**
   * @deprecated 
   */
  static final FileType ALIGNED_STACK_OLD =
    FileType.constructDescribedImodImageFileInstance(true, true, "", ".ali",
      ImodManager.FINE_ALIGNED_KEY, "the final aligned stack");
  public static final FileType ALIGNED_STACK = FileType.constructInstance(
    new Object[] { Variable.DATASET_AND_AXIS, ExtensionMarker.IMAGE, Extension.ALI },
    ImodManager.FINE_ALIGNED_KEY, "the final aligned stack");
  /**
   * @deprecated 
   */
  static final FileType NEWST_OR_BLEND_3D_FIND_OUTPUT_OLD =
    FileType.constructImodImageFileInstance(true, true, "_3dfind", ".ali",
      ImodManager.FINE_ALIGNED_3D_FIND_KEY);
  public static final FileType NEWST_OR_BLEND_3D_FIND_OUTPUT =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_3dfind",
      ExtensionMarker.IMAGE, Extension.ALI }, ImodManager.FINE_ALIGNED_3D_FIND_KEY);
  /**
   * @deprecated 
   */
  public static final FileType CTF_CORRECTED_STACK_OLD =
    FileType.constructImodImageFileInstance(true, true, "_ctfcorr", ".ali",
      ImodManager.CTF_CORRECTION_KEY);
  public static final FileType CTF_CORRECTED_STACK =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_ctfcorr",
      ExtensionMarker.IMAGE, Extension.ALI }, ImodManager.CTF_CORRECTION_KEY);
  /**
   * @deprecated 
   */
  public static final FileType ERASED_BEADS_STACK_OLD =
    FileType.constructImodImageFileInstance(true, true, "_erase", ".ali",
      ImodManager.ERASED_FIDUCIALS_KEY);
  public static final FileType ERASED_BEADS_STACK =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_erase",
      ExtensionMarker.IMAGE, Extension.ALI }, ImodManager.ERASED_FIDUCIALS_KEY);
  /**
   * @deprecated 
   */
  public static final FileType MTF_FILTERED_STACK_OLD =
    FileType.constructImodImageFileInstance(true, true, "_filt", ".ali",
      ImodManager.MTF_FILTER_KEY);
  public static final FileType MTF_FILTERED_STACK =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_filt",
      ExtensionMarker.IMAGE, Extension.ALI }, ImodManager.MTF_FILTER_KEY);
  //
  public static final FileType TRANSFORMED_REFINING_MODEL =
    FileType.constructImodInstance(true, false, "_refine", ".alimod",
      ImodManager.TRANSFORMED_MODEL_KEY);
  /**
   * @deprecated 
   */
  public static final FileType XCORR_BLEND_OUTPUT_OLD =
    FileType.constructImageFileInstance(true, true, "", ".bl");
  public static final FileType XCORR_BLEND_OUTPUT = FileType.constructInstance(
    new Object[] { Variable.DATASET_AND_AXIS, ExtensionMarker.IMAGE, Extension.BL });
  //
  public static final FileType CHECK_FILE =
    FileType.constructInstance(true, true, "", ".cmds");
  public static final FileType ALIGN_COMSCRIPT =
    FileType.constructInstance(false, true, "align", ".com");
  public static final FileType AUTOFIDSEED_COMSCRIPT =
    FileType.constructInstance(false, true, "autofidseed", ".com");
  public static final FileType BATCH_RUN_TOMO_COMSCRIPT =
    FileType.constructInstance(true, false, "", ".com");
  public static final FileType BLEND_COMSCRIPT =
    FileType.constructInstance(false, true, "blend", ".com");
  public static final FileType COPYTOMOCOMS_COMSCRIPT =
    FileType.constructInstance(false, false, "copytomocoms", ".com");
  public static final FileType CRYO_POSITION_COMSCRIPT =
    FileType.constructInstance(false, true, "cryoposition", ".com");
  public static final FileType CTF_CORRECTION_COMSCRIPT =
    FileType.constructInstance(false, true, "ctfcorrection", ".com");
  public static final FileType FIND_BEADS_3D_COMSCRIPT =
    FileType.constructInstance(false, true, "findbeads3d", ".com");
  public static final FileType FLATTEN_COMSCRIPT =
    FileType.constructInstance(false, false, "flatten", ".com");
  public static final FileType FLATTEN_TOOL_COMSCRIPT =
    FileType.constructInstance(true, false, "_flatten", ".com");
  public static final FileType GOLD_ERASER_COMSCRIPT =
    FileType.constructInstance(false, true, "golderaser", ".com");
  public static final FileType MULTIFILT_SETUP_COMSCRIPT =
    FileType.constructInstance(false, true, "multifiltsetup", ".com");
  public static final FileType MTF_FILTER_COMSCRIPT =
    FileType.constructInstance(false, true, "mtffilter", ".com");
  public static final FileType NEWST_COMSCRIPT =
    FileType.constructInstance(false, true, "newst", ".com");
  public static final FileType PREBLEND_COMSCRIPT =
    FileType.constructInstance(false, true, "preblend", ".com");
  public static final FileType SIRTSETUP_COMSCRIPT =
    FileType.constructInstance(false, true, "sirtsetup", ".com");
  public static final FileType SLOPPY_BLEND_COMSCRIPT =
    FileType.constructIMODDirInstance(false, false, "sloppyblend", ".com", COM_DIR);
  public static final FileType TILT_COMSCRIPT =
    FileType.constructInstance(false, true, "tilt", ".com");
  public static final FileType TILT_FOR_SIRT_COMSCRIPT =
    FileType.constructInstance(false, true, "tilt", "_for_sirt.com");
  public static final FileType TRACK_COMSCRIPT =
    FileType.constructInstance(false, true, "track", ".com");
  public static final FileType TRACK_ADJUSTED_COMSCRIPT =
    FileType.constructInstance(new Object[] { "track", Variable.AXIS, "_adjusted",
      ExtensionMarker.GENERIC, Extension.COM });
  public static final FileType TRACK_ORIG_COMSCRIPT =
    FileType.constructInstance(new Object[] { "track", Variable.AXIS, "_orig",
      ExtensionMarker.GENERIC, Extension.COM });
  public static final FileType CROSS_CORRELATION_COMSCRIPT =
    FileType.constructInstance(false, true, "xcorr", ".com");
  public static final FileType PATCH_TRACKING_COMSCRIPT =
    FileType.constructInstance(false, true, "xcorr_pt", ".com");
  public static final FileType DIRECTIVES_DESCR =
    FileType.constructIMODDirInstance(false, false, "directives", ".csv", COM_DIR);
  public static final FileType JOIN_WARP_2_MODEL_COMSCRIPT =
    FileType.constructInstance(new Object[] { ProcessName.JOIN_WARP_2_MODEL.toString(),
      ExtensionMarker.GENERIC, Extension.COM });
  /**
   * @deprecated 
   * 
   */
  public static final FileType DISTORTION_CORRECTED_STACK_OLD =
    FileType.constructImageFileInstance(true, true, "", ".dcst");
  public static final FileType DISTORTION_CORRECTED_STACK = FileType.constructInstance(
    new Object[] { Variable.DATASET_AND_AXIS, ExtensionMarker.IMAGE, Extension.DCST });
  //
  public static final FileType AUTOFIDSEED_DIR =
    FileType.constructInstance(false, true, "autofidseed", ".dir");
  public static final FileType PIECE_SHIFTS =
    FileType.constructInstance(true, true, "", ".ecd");
  public static final FileType MANUAL_REPLACEMENT_MODEL =
    FileType.constructInstance(true, true, "", ".erase");
  public static final FileType FIDUCIAL_MODEL =
    FileType.constructInstance(true, true, "", ".fid");
  public static final FileType CCD_ERASER_BEADS_INPUT_MODEL =
    FileType.constructInstance(true, true, "_erase", ".fid");
  public static final FileType FIDUCIAL_NO_GAPS_MODEL =
    FileType.constructInstance(true, true, "_nogaps", ".fid");
  public static final FileType FIDUCIAL_PATCH_TRACKING_MODEL =
    FileType.constructInstance(true, true, "_pt", ".fid");
  /**
   * @deprecated 
   */
  public static final FileType FLATTEN_TOOL_OUTPUT_OLD =
    FileType.constructImodImageFileInstance(true, false, "", ".flat",
      ImodManager.FLATTEN_TOOL_OUTPUT_KEY);
  public static final FileType FLATTEN_TOOL_OUTPUT = FileType.constructInstance(
    new Object[] { Variable.DATASET, ExtensionMarker.IMAGE, Extension.FLAT },
    ImodManager.FLATTEN_TOOL_OUTPUT_KEY);
  //
  public static final FileType EXCLUDE_VIEWS_INFO =
    FileType.constructVersionedInstance(true, true, "_cutviews", ".info");// _cutviews0.info
  /**
   * @deprecated 
   */
  public static final FileType NAD_TEST_INPUT_OLD =
    FileType.constructImodImageFileInstanceInSubdirectory(false, false, "test", ".input",
      ImodManager.TEST_VOLUME_KEY, null);
  public static final FileType NAD_TEST_INPUT =
    FileType.constructInstance(DirectoryType.NAD_SUBDIR,
      new Object[] { "test", ExtensionMarker.IMAGE, Extension.INPUT },
      ImodManager.TEST_VOLUME_KEY);
  //
  /**
   * @deprecated 
   */
  public static final FileType JOIN_OLD = FileType.constructImodImageFileInstance(true,
    false, "", ".join", ImodManager.JOIN_KEY);
  public static final FileType JOIN = FileType.constructInstance(
    new Object[] { Variable.DATASET, ExtensionMarker.IMAGE, Extension.JOIN },
    ImodManager.JOIN_KEY);
  /**
   * @deprecated 
   */
  public static final FileType MODELED_JOIN_OLD = FileType.constructImodImageFileInstance(
    true, false, "_modeled", ".join", ImodManager.MODELED_JOIN_KEY);
  public static final FileType MODELED_JOIN = FileType.constructInstance(
    new Object[] { Variable.DATASET, "_modeled", ExtensionMarker.IMAGE, Extension.JOIN },
    ImodManager.MODELED_JOIN_KEY);
  /**
   * @deprecated 
   */
  public static final FileType TRIAL_JOIN_OLD = FileType.constructImodImageFileInstance(
    true, false, "_trial", ".join", ImodManager.TRIAL_JOIN_KEY);
  public static final FileType TRIAL_JOIN = FileType.constructInstance(
    new Object[] { Variable.DATASET, "_trial", ExtensionMarker.IMAGE, Extension.JOIN },
    ImodManager.TRIAL_JOIN_KEY);
  //
  public static final FileType BATCH_RUN_TOMO_LOG =
    FileType.constructInstance(true, false, "", ".log");
  public static final FileType BATCH_RUN_TOMO_DATASET_LOG =
    FileType.constructInstance(false, false, "batchruntomo", ".log");
  public static final FileType ERASER_LOG =
    FileType.constructInstance(false, true, "eraser", ".log");
  public static final FileType TILT_ALIGN_LOG =
    FileType.constructInstance(false, true, "align", ".log");
  public static final FileType GPU_TEST_LOG =
    FileType.constructInstance(false, false, "gputest", ".log");
  public static final FileType PREBLEND_LOG =
    FileType.constructInstance(false, true, "preblend", ".log");
  public static final FileType ALIGN_ANGLES_LOG =
    FileType.constructInstance(false, true, "taAngles", ".log");
  public static final FileType ALIGN_ERROR_LOG =
    FileType.constructInstance(false, true, "taError", ".log");
  public static final FileType ALIGN_ROBUST_LOG =
    FileType.constructInstance(false, true, "taRobust", ".log");
  public static final FileType ALIGN_SOLUTION_LOG =
    FileType.constructInstance(false, true, "taSolution", ".log");
  public static final FileType CROSS_CORRELATION_LOG =
    FileType.constructInstance(false, true, "xcorr", ".log");

  public static final FileType MTF_FILTER_MDOC =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS,
      Variable.ORIG_RAW_IMAGE_EXTENSION, ExtensionMarker.GENERIC, Extension.MDOC });

  public static final FileType FIND_BEADS_3D_OUTPUT_MODEL =
    FileType.constructInstance(true, true, "_3dfind", ".mod");
  public static final FileType AUTOFIDSEED_BOUNDARY_MODEL =
    FileType.constructInstance(true, true, "_afsbound", ".mod");
  public static final FileType AUTO_ALIGN_BOUNDARY_MODEL =
    FileType.constructInstance(true, false, "_bound", ".mod");
  public static final FileType SMOOTHING_ASSESSMENT_OUTPUT_MODEL =
    FileType.constructImodInstance(true, true, "_checkflat", ".mod",
      ImodManager.SMOOTHING_ASSESSMENT_KEY);
  public static final FileType CLUSTERED_ELONGATED_MODEL =
    FileType.constructImodInstanceInSubdirectory(false, false, "clusterElong", ".mod",
      null, FileType.AUTOFIDSEED_DIR);
  public static final FileType FLATTEN_WARP_INPUT_MODEL =
    FileType.constructInstance(true, false, "_flat", ".mod");
  public static final FileType PATCH_VECTOR_MODEL = FileType.constructImodInstance(false,
    false, "patch_vector", ".mod", ImodManager.PATCH_VECTOR_MODEL_KEY);
  public static final FileType PATCH_VECTOR_CCC_MODEL = FileType.constructImodInstance(
    false, false, "patch_vector_ccc", ".mod", ImodManager.PATCH_VECTOR_CCC_MODEL_KEY);
  public static final FileType PATCH_TRACKING_BOUNDARY_MODEL =
    FileType.constructInstance(true, true, "_ptbound", ".mod");
  public static final FileType BATCH_RUN_TOMO_BOUNDARY_MODEL =
    FileType.constructInstance(true, false, "_rawbound", ".mod");
  public static final FileType TOMOPITCH_MODEL =
    FileType.constructInstance(false, true, "tomopitch", ".mod");
  /**
   * @deprecated 
   */
  public static final FileType ALIGNED_STACK_MRC_OLD =
    FileType.constructImodImageFileInstance(true, true, "_ali", ".mrc",
      ImodManager.ALIGNED_STACK_KEY);
  public static final FileType ALIGNED_STACK_MRC =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_ali",
      ExtensionMarker.IMAGE, Extension.MRC }, ImodManager.ALIGNED_STACK_KEY);
  /**
   * @deprecated 
   */
  public static final FileType PREBLEND_OUTPUT_MRC_OLD =
    FileType.constructImodImageFileInstance(true, true, "_preblend", ".mrc",
      ImodManager.PREBLEND_KEY);
  public static final FileType PREBLEND_OUTPUT_MRC =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_preblend",
      ExtensionMarker.IMAGE, Extension.MRC }, ImodManager.PREBLEND_KEY);
  /**
   * @deprecated 
   */
  public static final FileType MUTLIFILT_EXACT_OBJECT_SIZES_OUTPUT_TEMPLATE_OLD =
    FileType.constructTemplateImageFileInstance(true, true, "_efos", ".mrc",
      DynamicLocation.AFTER_TYPE_STRING, "Exact Filter Trials");
  // Template for dataset_efosnnnn.mrc
  public static final FileType MUTLIFILT_EXACT_OBJECT_SIZES_OUTPUT_TEMPLATE =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_efos",
      Variable.TWO_DIGIT_INTEGER, ExtensionMarker.IMAGE, Extension.MRC }, null,
      "Exact Filter Trials");
  /**
   * @deprecated 
   */
  public static final FileType MUTLIFILT_GAUSSIAN_OUTPUT_TEMPLATE_OLD =
    FileType.constructTemplateImageFileInstance(true, true, "_gfc", "-f", ".mrc",
      DynamicLocation.AFTER_TYPE_STRING_AND_AFTER_MIDDLE_PIECE,
      "Standard Gaussian Filter Trials");
  // Template for dataset_gfcx.xxx-fx.xxx.mrc
  // The dots are decimal places.
  public static final FileType MUTLIFILT_GAUSSIAN_OUTPUT_TEMPLATE =
    FileType.constructInstance(
      new Object[] { Variable.DATASET_AND_AXIS, "_gfc", Variable.PRECISION_THREE_FLOAT,
        "-f", Variable.PRECISION_THREE_FLOAT, ExtensionMarker.IMAGE, Extension.MRC },
      null, "Standard Gaussian Filter Trials");
  /**
   * @deprecated 
   */
  public static final FileType MUTLIFILT_HAMMING_LIKE_STARTS_OUTPUT_TEMPLATE_OLD =
    FileType.constructTemplateImageFileInstance(true, true, "_hlfs", ".mrc",
      DynamicLocation.AFTER_TYPE_STRING, "Hamming-Like Filter Trials");
  // Template for dataset_hlfs0.xxx.mrc
  public static final FileType MUTLIFILT_HAMMING_LIKE_STARTS_OUTPUT_TEMPLATE =
    FileType.constructInstance(
      new Object[] { Variable.DATASET_AND_AXIS, "_hlfs",
        Variable.PRECISION_THREE_FRACTION, ExtensionMarker.IMAGE, Extension.MRC },
      null, "Hamming-Like Filter Trials");
  /**
   * @deprecated 
   */
  public static final FileType MUTLIFILT_FAKE_SIRT_ITERATIONS_OUTPUT_TEMPLATE_OLD =
    FileType.constructTemplateImageFileInstance(true, true, "_slfi", ".mrc",
      DynamicLocation.AFTER_TYPE_STRING, "SIRT-like Filter Trials");
  // Template for dateset_slfinnnn.mrc
  public static final FileType MUTLIFILT_FAKE_SIRT_ITERATIONS_OUTPUT_TEMPLATE =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_slfi",
      Variable.TWO_DIGIT_INTEGER, ExtensionMarker.IMAGE, Extension.MRC }, null,
      "SIRT-like Filter Trials");
  //
  /**
   * @deprecated 
   */
  public static final FileType ANISOTROPIC_DIFFUSION_OUTPUT_OLD =
    FileType.constructImodImageFileInstance(true, false, "", ".nad",
      ImodManager.ANISOTROPIC_DIFFUSION_VOLUME_KEY);
  public static final FileType ANISOTROPIC_DIFFUSION_OUTPUT = FileType.constructInstance(
    new Object[] { Variable.DATASET, ExtensionMarker.IMAGE, Extension.NAD },
    ImodManager.ANISOTROPIC_DIFFUSION_VOLUME_KEY);
  //
  public static final FileType PIECE_LIST =
    FileType.constructInstance(true, true, "", ".pl");
  /**
   * @deprecated 
   */
  public static final FileType PREALIGNED_STACK_OLD =
    FileType.constructImodImageFileInstance(true, true, "", ".preali",
      ImodManager.COARSE_ALIGNED_KEY);
  public static final FileType PREALIGNED_STACK = FileType.constructInstance(
    new Object[] { Variable.DATASET_AND_AXIS, ExtensionMarker.IMAGE, Extension.PREALI },
    ImodManager.COARSE_ALIGNED_KEY);
  //
  public static final FileType PRE_TRANSFORMATION_LIST =
    FileType.constructInstance(true, true, "", ".prexf");
  public static final FileType PRE_XG =
    FileType.constructImodInstance(true, true, "", ".prexg", null);
  public static final FileType MATLAB_PARAM_FILE =
    FileType.constructInstance(true, false, "", ".prm");
  public static final FileType RAW_TILT_ANGLES =
    FileType.constructInstance(true, true, "", ".rawtlt");
  /**
   * @deprecated 
   */
  public static final FileType TRIM_VOL_OUTPUT_OLD =
    FileType.constructImodImageFileInstance(true, false, "", ".rec",
      ImodManager.TRIMMED_VOLUME_KEY);
  public static final FileType TRIM_VOL_OUTPUT = FileType.constructInstance(
    new Object[] { Variable.DATASET, ExtensionMarker.IMAGE, Extension.REC },
    ImodManager.TRIMMED_VOLUME_KEY);
  // -nnn.rec
  public static final FileType PROCESSCHUNKS_REC = FileType.constructInstance(
    new Object[] { Variable.DATASET_AND_AXIS, "-", Variable.THREE_DIGIT_INTEGER,
      ExtensionMarker.IMAGE, Extension.REC },
    ImodManager.TRIMMED_VOLUME_KEY);
  /**
   * @deprecated 
   */
  public static final FileType TILT_3D_FIND_OUTPUT_OLD =
    FileType.constructImodImageFileInstance(true, true, "_3dfind", ".rec",
      ImodManager.FULL_VOLUME_3D_FIND_KEY);
  public static final FileType TILT_3D_FIND_OUTPUT =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_3dfind",
      ExtensionMarker.IMAGE, Extension.REC }, ImodManager.FULL_VOLUME_3D_FIND_KEY);
  /**
   * @deprecated 
   */
  public static final FileType BOTTOM_SAMPLE_OLD =
    FileType.constructImageFileInstance(false, true, "bot", ".rec");
  public static final FileType BOTTOM_SAMPLE = FileType.constructInstance(
    new Object[] { "bot", Variable.AXIS, ExtensionMarker.IMAGE, Extension.REC });
  /**
   * @deprecated 
   */
  public static final FileType CRYO_POSITION_OUTPUT_OLD =
    FileType.constructImageFileInstance(true, true, "_cpos", ".rec");
  public static final FileType CRYO_POSITION_OUTPUT =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_cpos",
      ExtensionMarker.IMAGE, Extension.REC });
  //
  /**
   * @deprecated 
   */
  static final FileType TILT_OUTPUT_DUAL_OLD =
    FileType.constructImageFileInstance(true, true, "", ".rec");
  /**
   * @deprecated 
   */
  public static final FileType TILT_OUTPUT_SINGLE_OLD =
    FileType.constructImageFileInstance(true, true, "_full", ".rec");
  /**
   * Doesn't really use the axisID because its only for a single axis tomogram.
   */
  public static final FileType TILT_OUTPUT_SINGLE =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_full",
      ExtensionMarker.IMAGE, Extension.REC });
  public static final FileType TILT_OUTPUT_DUAL = FileType.constructInstance(
    new Object[] { Variable.DATASET_AND_AXIS, ExtensionMarker.IMAGE, Extension.REC });
  /**
   * @deprecated 
   */
  public static final FileType TILT_OUTPUT_OLD =
    FileType.constructDifferentDualSingleImageFileInstance(TILT_OUTPUT_SINGLE_OLD,
      TILT_OUTPUT_DUAL_OLD, ImodManager.FULL_VOLUME_KEY, "the tomogram");
  public static final FileType TILT_OUTPUT =
    FileType.constructInstance(TILT_OUTPUT_DUAL.fileNamePattern,
      TILT_OUTPUT_SINGLE.fileNamePattern, ImodManager.FULL_VOLUME_KEY, "the tomogram");
  //
  /**
   * @deprecated 
   */
  public static final FileType FLATTEN_OUTPUT_OLD =
    FileType.constructImodImageFileInstance(true, false, "_flat", ".rec",
      ImodManager.FLAT_VOLUME_KEY);
  public static final FileType FLATTEN_OUTPUT = FileType.constructInstance(
    new Object[] { Variable.DATASET, "_flat", ExtensionMarker.IMAGE, Extension.REC },
    ImodManager.FLAT_VOLUME_KEY);
  //
  /**
   * @deprecated 
   */
  public static final FileType MIDDLE_SAMPLE_OLD =
    FileType.constructImageFileInstance(false, true, "mid", ".rec");
  public static final FileType MIDDLE_SAMPLE = FileType.constructInstance(
    new Object[] { "mid", Variable.AXIS, ExtensionMarker.IMAGE, Extension.REC });
  /**
   * @deprecated 
   */
  public static final FileType COMBINED_VOLUME_OLD =
    FileType.constructImodImageFileInstance(false, false, "sum", ".rec",
      ImodManager.COMBINED_TOMOGRAM_KEY);
  public static final FileType COMBINED_VOLUME = FileType.constructInstance(
    new Object[] { "sum", ExtensionMarker.IMAGE, Extension.REC },
    ImodManager.COMBINED_TOMOGRAM_KEY);
  /**
   * @deprecated 
   */
  public static final FileType TOP_SAMPLE_OLD =
    FileType.constructImageFileInstance(false, true, "top", ".rec");
  public static final FileType TOP_SAMPLE = FileType.constructInstance(
    new Object[] { "top", Variable.AXIS, ExtensionMarker.IMAGE, Extension.REC });
  //
  /**
   * @deprecated 
   */
  public static final FileType JOIN_SAMPLE_AVERAGES_OLD =
    FileType.constructImodImageFileInstance(true, false, "", ".sampavg",
      ImodManager.JOIN_SAMPLE_AVERAGES_KEY);
  public static final FileType JOIN_SAMPLE_AVERAGES = FileType.constructInstance(
    new Object[] { Variable.DATASET, ExtensionMarker.IMAGE, Extension.SAMPAVG },
    ImodManager.JOIN_SAMPLE_AVERAGES_KEY);
  //
  /**
   * @deprecated 
   */
  public static final FileType JOIN_SAMPLE_OLD = FileType.constructImodImageFileInstance(
    true, false, "", ".sample", ImodManager.JOIN_SAMPLES_KEY);
  public static final FileType JOIN_SAMPLE = FileType.constructInstance(
    new Object[] { Variable.DATASET, ExtensionMarker.IMAGE, Extension.SAMPLE },
    ImodManager.JOIN_SAMPLES_KEY);
  //
  public static final FileType SEED_MODEL =
    FileType.constructInstance(true, true, "", ".seed");
  //
  /**
   * @deprecated 
   */
  public static final FileType SIRT_SCALED_OUTPUT_TEMPLATE_OLD =
    FileType.constructDerivedTemplateImageFileInstance(TILT_OUTPUT_OLD, ".sint",
      ImodManager.SIRT_KEY, DynamicLocation.AFTER_EXTENSION);
  // Template for .sintnn
  // Derived from TILT_OUTPUT
  public static final FileType SIRT_SCALED_OUTPUT_TEMPLATE = FileType.constructInstance(
    TILT_OUTPUT.fileNamePattern, TILT_OUTPUT.singleAxisFileNamePattern,
    new Object[] { ExtensionMarker.IMAGE, Extension.SINT, Variable.TWO_DIGIT_INTEGER },
    ImodManager.SIRT_KEY);
  /**
   * @deprecated 
   */
  public static final FileType SIRT_SUBAREA_SCALED_OUTPUT_TEMPLATE_OLD =
    FileType.constructTemplateImageFileInstance(true, true, "_sub", ".sint",
      DynamicLocation.AFTER_EXTENSION);
  // Template for dataseta_sub.sintnnn
  // Bug# 2402
  public static final FileType SIRT_SUBAREA_SCALED_OUTPUT_TEMPLATE =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_sub",
      ExtensionMarker.IMAGE, Extension.SINT, Variable.TWO_DIGIT_INTEGER });
  //
  /**
   * @deprecated 
   */
  public static final FileType SQUEEZE_VOL_OUTPUT_OLD =
    FileType.constructImodImageFileInstance(true, false, "", ".sqz",
      ImodManager.SQUEEZED_VOLUME_KEY);
  public static final FileType SQUEEZE_VOL_OUTPUT = FileType.constructInstance(
    new Object[] { Variable.DATASET, ExtensionMarker.IMAGE, Extension.SQZ },
    ImodManager.SQUEEZED_VOLUME_KEY);
  //
  /**
   * @deprecated 
   */
  public static final FileType SIRT_OUTPUT_TEMPLATE_OLD =
    FileType.constructDerivedTemplateImageFileInstance(TILT_OUTPUT_OLD, ".srec",
      ImodManager.SIRT_KEY, DynamicLocation.AFTER_EXTENSION);
  // Template for .srecnn
  // Derived from TILT_OUTPUT
  public static final FileType SIRT_OUTPUT_TEMPLATE = FileType.constructInstance(
    TILT_OUTPUT.fileNamePattern, TILT_OUTPUT.singleAxisFileNamePattern,
    new Object[] { ExtensionMarker.IMAGE, Extension.SREC, Variable.TWO_DIGIT_INTEGER },
    ImodManager.SIRT_KEY);
  //
  /**
   * @deprecated 
   */
  public static final FileType SIRT_SUBAREA_OUTPUT_TEMPLATE_OLD =
    FileType.constructTemplateImageFileInstance(true, true, "_sub", ".srec",
      DynamicLocation.AFTER_EXTENSION);
  // Template for dataseta_sub.srecnnn
  public static final FileType SIRT_SUBAREA_OUTPUT_TEMPLATE =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_sub",
      ExtensionMarker.IMAGE, Extension.SREC, Variable.TWO_DIGIT_INTEGER });
  //
  /**
   * @deprecated 
   */
  @SuppressWarnings("deprecation")
  public static final FileType RAW_STACK_OLD =
    FileType.constructTwoImodRawImageFileInstance(true, true, "",
      DatasetTool.STANDARD_DATASET_EXT, ImodManager.RAW_STACK_KEY,
      ImodManager.PREVIEW_KEY);
  public static final FileType RAW_STACK = FileType.constructInstance(
    new Object[] { Variable.DATASET_AND_AXIS, ExtensionMarker.INPUT_IMAGE, Extension.ST },
    ImodManager.RAW_STACK_KEY, ImodManager.PREVIEW_KEY, null);
  /**
   * @deprecated 
   */
  public static final FileType STATS_LOG_OLD =
    FileType.constructInstance(true, true, ".st_stats", ".log");
  public static final FileType STATS_LOG = FileType.constructInstance(
    new Object[] { RAW_STACK, "_stats", ExtensionMarker.GENERIC, Extension.LOG },
    ImodManager.JOIN_KEY);
  /**
   * @deprecated 
   */
  @SuppressWarnings("deprecation")
  public static final FileType FIXED_XRAYS_STACK_OLD =
    FileType.constructImodImageFileInstance(true, true, "_fixed",
      DatasetTool.STANDARD_DATASET_EXT, ImodManager.ERASED_STACK_KEY);
  public static final FileType FIXED_XRAYS_STACK =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_fixed",
      ExtensionMarker.INPUT_IMAGE, Extension.ST }, ImodManager.ERASED_STACK_KEY);
  /**
   * @deprecated 
   */
  public static final FileType FIXED_STATS_LOG_OLD =
    FileType.constructInstance(true, true, "_fixed.st_stats", ".log");
  public static final FileType FIXED_STATS_LOG = FileType.constructInstance(
    new Object[] { FIXED_XRAYS_STACK, "_stats", ExtensionMarker.GENERIC, Extension.LOG },
    ImodManager.JOIN_KEY);
  /**
   * @deprecated 
   */
  @SuppressWarnings("deprecation")
  public static final FileType ORIGINAL_RAW_STACK_OLD = FileType
    .constructRawImageFileInstance(true, true, "_orig", DatasetTool.STANDARD_DATASET_EXT);
  public static final FileType ORIGINAL_RAW_STACK =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_orig",
      ExtensionMarker.INPUT_IMAGE, Extension.ST });
  // _full.vsrnn
  public static final FileType FULL_VSR =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_full",
      ExtensionMarker.IMAGE, Extension.VSR, Variable.TWO_DIGIT_INTEGER });
  // _sub.vsrnn
  public static final FileType SUB_VSR =
    FileType.constructInstance(new Object[] { Variable.DATASET_AND_AXIS, "_sub",
      ExtensionMarker.IMAGE, Extension.VSR, Variable.TWO_DIGIT_INTEGER });
  //
  public static final FileType WARP_XG = FileType.constructInstance(
    new Object[] { Variable.DATASET, ExtensionMarker.GENERIC, Extension.WARPXG });
  public static final FileType EDGE_FUNCTIONS_X =
    FileType.constructInstance(true, true, "", ".xef");
  public static final FileType LOCAL_TRANSFORMATION_LIST =
    FileType.constructInstance(true, false, "", ".xf");
  public static final FileType AUTO_LOCAL_TRANSFORMATION_LIST =
    FileType.constructInstance(true, false, "_auto", ".xf");
  public static final FileType EMPTY_LOCAL_TRANSFORMATION_LIST =
    FileType.constructInstance(true, false, "_empty", ".xf");
  public static final FileType MIDAS_LOCAL_TRANSFORMATION_LIST =
    FileType.constructInstance(true, false, "_midas", ".xf");
  public static final FileType GLOBAL_TRANSFORMATION_LIST =
    FileType.constructInstance(true, false, "", ".xg");
  //

  private final boolean usesDataset;
  private final boolean usesAxisID;
  private final String typeString;
  private final String extension;
  private final boolean composite;
  private final boolean inSubdirectory;
  private final FileType subFileType;
  private final FileType singleFileType;
  private final FileType dualFileType;
  private final boolean unnamed;
  private final boolean template;
  private final String inImodSubdirectory;
  private final FileType subdir;
  private final boolean versioned;
  private final String templateOnlyMiddlePiece;
  private final DynamicLocation dynamicLocation;
  private final boolean imageFile;
  // For raw images which can be tif files.
  private final boolean expandedImageExtensionSet;
  private final Object[] fileNamePattern;
  private final DirectoryType directory;
  private final Object[] singleAxisFileNamePattern;

  private FileType parentFileType = null;

  /**
   * Creates an empty instance which is not added to the collection.  This constructor
   * should not be used, and exists only to allow the class to be inherited.
   * @see etomo.plugin.demo
   */
  protected FileType() {
    super(null, null, null);
    usesDataset = false;
    usesAxisID = false;
    typeString = null;
    extension = null;
    composite = false;
    inSubdirectory = false;
    subFileType = null;
    singleFileType = null;
    dualFileType = null;
    unnamed = true;
    template = false;
    inImodSubdirectory = null;
    subdir = null;
    versioned = false;
    templateOnlyMiddlePiece = null;
    dynamicLocation = null;
    imageFile = false;
    expandedImageExtensionSet = false;
    fileNamePattern = null;
    directory = null;
    singleAxisFileNamePattern = null;
    if (!unnamed) {
      namedFileTypeList.add(this);
    }
  }

  private FileType(final boolean usesDataset, final boolean usesAxisID,
    final String typeString, final String extension, final String imodManagerKey,
    final String imodManagerKey2, final String descr, final boolean composite,
    final boolean inSubdirectory, final FileType subFileType,
    final FileType singleFileType, final FileType dualFileType, final boolean unnamed,
    final boolean template, final String inImodSubdirectory, final FileType subdir,
    final boolean versioned, final String templateOnlyMiddlePiece,
    final DynamicLocation dynamicLocation, final boolean imageFile,
    final boolean expandedImageExtensionSet) {
    super(imodManagerKey, imodManagerKey2, descr);
    this.usesDataset = usesDataset;
    this.usesAxisID = usesAxisID;
    this.typeString = typeString;
    this.extension = extension;
    this.composite = composite;
    this.inSubdirectory = inSubdirectory;
    this.subFileType = subFileType;
    this.singleFileType = singleFileType;
    this.dualFileType = dualFileType;
    this.unnamed = unnamed;
    this.template = template;
    this.inImodSubdirectory = inImodSubdirectory;
    this.subdir = subdir;
    this.versioned = versioned;
    this.templateOnlyMiddlePiece = templateOnlyMiddlePiece;
    this.dynamicLocation = dynamicLocation;
    this.imageFile = imageFile;
    this.expandedImageExtensionSet = expandedImageExtensionSet;
    fileNamePattern = null;
    directory = null;
    singleAxisFileNamePattern = null;
    if (!unnamed) {
      namedFileTypeList.add(this);
    }
  }

  // New files types using a generic pattern
  FileType(final DirectoryType directory, final Object[] fileNamePattern,
    final Object[] singleAxisFileNamePattern, final String imodManagerKey,
    final String imodManagerKey2, final String descr) {
    super(imodManagerKey, imodManagerKey2, descr);
    this.directory = directory;
    this.fileNamePattern = fileNamePattern;
    this.singleAxisFileNamePattern = singleAxisFileNamePattern;
    //
    usesDataset = false;
    usesAxisID = false;
    typeString = null;
    extension = null;
    composite = false;
    inSubdirectory = false;
    subFileType = null;
    singleFileType = null;
    dualFileType = null;
    unnamed = true;
    template = false;
    inImodSubdirectory = null;
    subdir = null;
    versioned = false;
    templateOnlyMiddlePiece = null;
    dynamicLocation = null;
    imageFile = false;
    expandedImageExtensionSet = false;
    if (!unnamed) {
      namedFileTypeList.add(this);
    }
  }

  private static FileType constructInstance(final Object[] fileNamePattern,
    final String imodManagerKey, final String descr) {
    return new FileType(null, fileNamePattern, null, imodManagerKey, null, descr);
  }

  private static FileType constructInstance(final Object[] fileNamePattern,
    final String imodManagerKey) {
    return new FileType(null, fileNamePattern, null, imodManagerKey, null, null);
  }

  private static FileType constructInstance(final Object[] fileNamePattern,
    final Object[] singleAxisFileNamePattern, final String imodManagerKey,
    final String descr) {
    return new FileType(null, fileNamePattern, singleAxisFileNamePattern, imodManagerKey,
      null, descr);
  }

  private static FileType constructInstance(final Object[] sourceFileNamePattern,
    final Object[] sourceSingleAxisFileNamePattern, final Object[] newExtensionPattern,
    final String imodManagerKey) {
    return new FileType(null, joinPatterns(sourceFileNamePattern, newExtensionPattern),
      joinPatterns(sourceSingleAxisFileNamePattern, newExtensionPattern), imodManagerKey,
      null, null);
  }

  private static FileType constructInstance(final Object[] fileNamePattern) {
    return new FileType(null, fileNamePattern, null, null, null, null);
  }

  private static FileType constructInstance(final DirectoryType directory,
    final Object[] fileNamePattern, final String imodManagerKey) {
    return new FileType(directory, fileNamePattern, null, imodManagerKey, null, null);
  }

  private static FileType constructInstance(final Object[] fileNamePattern,
    final String imodManagerKey, final String imodManagerKey2, final String descr) {
    return new FileType(null, fileNamePattern, null, imodManagerKey, imodManagerKey2,
      descr);
  }

  // old file types

  private static FileType constructVersionedInstance(final boolean usesDataset,
    final boolean usesAxisID, final String typeString, final String extension) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, null, null, null,
      false, false, null, null, null, false, false, null, null, true, null, null, false,
      false);
  }

  private static FileType constructTemplateImageFileInstance(final boolean usesDataset,
    final boolean usesAxisID, final String typeString, final String extension,
    final DynamicLocation dynamicLocation) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, null, null, null,
      false, false, null, null, null, false, true, null, null, false, null,
      dynamicLocation, true, false);
  }

  private static FileType constructTemplateImageFileInstance(final boolean usesDataset,
    final boolean usesAxisID, final String typeString, final String extension,
    final DynamicLocation dynamicLocation, final String descr) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, null, null, descr,
      false, false, null, null, null, false, true, null, null, false, null,
      dynamicLocation, true, false);
  }

  private static FileType constructTemplateImageFileInstance(final boolean usesDataset,
    final boolean usesAxisID, final String typeString,
    final String templateOnlyMiddlePiece, final String extension,
    final DynamicLocation dynamicLocation, final String descr) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, null, null, descr,
      false, false, null, null, null, false, true, null, null, false,
      templateOnlyMiddlePiece, dynamicLocation, true, false);
  }

  /**
   * Get file types that are quite different in dual and single (BBa.rec and BBa_full.rec).
   *
   * @param singleFileType
   * @param dualFileType
   * @param imodManagerKey
   * @param description
   */
  private static FileType constructDifferentDualSingleImageFileInstance(
    final FileType singleFileType, final FileType dualFileType,
    final String imodManagerKey, final String descr) {
    FileType instance = new FileType(false, false, null, null, imodManagerKey, null,
      descr, true, false, null, singleFileType, dualFileType, false, false, null, null,
      false, null, null, true, false);
    // Child file types are not valid by themselves
    singleFileType.parentFileType = instance;
    dualFileType.parentFileType = instance;
    return instance;
  }

  protected static FileType constructInstance(final boolean usesDataset,
    final boolean usesAxisID, final String typeString, final String extension) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, null, null, null,
      false, false, null, null, null, false, false, null, null, false, null, null, false,
      false);
  }

  protected static FileType constructImageFileInstance(final boolean usesDataset,
    final boolean usesAxisID, final String typeString, final String extension) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, null, null, null,
      false, false, null, null, null, false, false, null, null, false, null, null, true,
      false);
  }

  protected static FileType constructRawImageFileInstance(final boolean usesDataset,
    final boolean usesAxisID, final String typeString, final String extension) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, null, null, null,
      false, false, null, null, null, false, false, null, null, false, null, null, true,
      true);
  }

  protected static FileType constructInstance(final boolean usesDataset,
    final boolean usesAxisID, final String typeString, final String extension,
    final String descr) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, null, null, descr,
      false, false, null, null, null, false, false, null, null, false, null, null, false,
      false);
  }

  /**
   * For use when everything is coming from another file type, except the extension
   * (BBa.srec, BBa_full.srec, BBa.sint, BBa_full.sint).
   *
   * @param usesDataset
   * @param usesAxisID
   * @param typeString
   * @param extension
   * @param imodManagerKey
   */
  protected static FileType constructDerivedTemplateImageFileInstance(
    final FileType subFileType, final String extension, final String imodManagerKey,
    final DynamicLocation dynamicLocation) {
    return new FileType(false, false, null, extension, imodManagerKey, null, null, true,
      false, subFileType, null, null, false, true, null, null, false, null,
      dynamicLocation, true, false);
  }

  private static FileType constructImodInstance(final boolean usesDataset,
    final boolean usesAxisID, final String typeString, final String extension,
    final String imodManagerKey) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, imodManagerKey,
      null, null, false, false, null, null, null, false, false, null, null, false, null,
      null, false, false);
  }

  private static FileType constructImodImageFileInstance(final boolean usesDataset,
    final boolean usesAxisID, final String typeString, final String extension,
    final String imodManagerKey) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, imodManagerKey,
      null, null, false, false, null, null, null, false, false, null, null, false, null,
      null, true, false);
  }

  private static FileType constructImodInstanceInSubdirectory(final boolean usesDataset,
    final boolean usesAxisID, final String typeString, final String extension,
    final String imodManagerKey, final FileType subdir) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, imodManagerKey,
      null, null, false, true, null, null, null, false, false, null, subdir, false, null,
      null, false, false);
  }

  private static FileType constructImodImageFileInstanceInSubdirectory(
    final boolean usesDataset, final boolean usesAxisID, final String typeString,
    final String extension, final String imodManagerKey, final FileType subdir) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, imodManagerKey,
      null, null, false, true, null, null, null, false, false, null, subdir, false, null,
      null, true, false);
  }

  private static FileType constructTwoImodRawImageFileInstance(final boolean usesDataset,
    final boolean usesAxisID, final String typeString, final String extension,
    final String imodManagerKey, final String imodManagerKey2) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, imodManagerKey,
      imodManagerKey2, null, false, false, null, null, null, false, false, null, null,
      false, null, null, true, true);
  }

  private static FileType constructDescribedImodImageFileInstance(
    final boolean usesDataset, final boolean usesAxisID, final String typeString,
    final String extension, final String imodManagerKey, final String descr) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, imodManagerKey,
      null, descr, false, false, null, null, null, false, false, null, null, false, null,
      null, true, false);
  }

  /**
   * Not added to the collection and can't be retrieved via getInstance.
   * @param imodManagerKey
   * @return
   */
  private static FileType constructUnamedImageFileInstance(final String imodManagerKey) {
    return new FileType(false, false, "", "", imodManagerKey, null, null, false, false,
      null, null, null, true, false, null, null, false, null, null, true, false);
  }

  /**
  * Not added to the collection and can't be retrieved via getInstance.
  * @param imodManagerKey
  * @return
  */
  private static FileType
    constructUnamedImageFileInstanceInSubdirectory(final String imodManagerKey) {
    return new FileType(false, false, "", "", imodManagerKey, null, null, false, true,
      null, null, null, true, false, null, null, false, null, null, true, false);
  }

  protected static FileType constructIMODDirInstance(final boolean usesDataset,
    final boolean usesAxisID, final String typeString, final String extension,
    final String imodSubdirectory) {
    return new FileType(usesDataset, usesAxisID, typeString, extension, null, null, null,
      false, false, null, null, null, false, false, imodSubdirectory, null, false, null,
      null, false, false);
  }

  /**
   * Get FileType instance from its name description.
   *
   * @param usesDataset
   * @param usesAxisID
   * @param typeString
   * @param extension
   * @return
   * @deprecated 2/1/2019
   */
  public static FileType getInstance(final AxisType axisType, boolean usesDataset,
    boolean usesAxisID, String typeString, String extension) {
    Iterator iterator = namedFileTypeList.iterator();
    while (iterator.hasNext()) {
      FileType fileType = (FileType) iterator.next();
      if (fileType.equals(axisType, usesDataset, usesAxisID, typeString, extension)) {
        if (fileType.parentFileType != null) {
          // This is a child file type which is not valid by itself
          return fileType.parentFileType;
        }
        return fileType;
      }
    }
    return null;
  }

  /**
   * Get FileType instance from its name description.  Uses regular expression pattern
   * matching.
   *
   * @param manager
   * @param axisID
   * @param usesDataset
   * @param usesAxisID
   * @param fileName
   * @return
   * @deprecated
   */
  public static FileType getInstance(final BaseManager manager, final AxisID axisID,
    final boolean usesDataset, final boolean usesAxisID, final String fileName) {
    if (fileName == null) {
      return null;
    }
    // Create a pattern for the dataset + axis, or for the axis by itself
    String fixedPattern = "";
    String axisPattern = "";
    String axisExtension = axisID.getExtension();
    if (usesDataset && manager != null) {
      // If the dataset is part of the file name, then there is a fixed pattern
      if (usesAxisID) {
        fixedPattern = Pattern.quote(manager.getBaseMetaData().getName() + axisExtension);
      }
      else {
        fixedPattern = Pattern.quote(manager.getBaseMetaData().getName());
      }
      // Eliminate file names that should start with the dataset but don't.
      if (!fileName.matches(fixedPattern + ".*")) {
        return null;
      }
    }
    else if (usesAxisID && !axisExtension.equals("")) {
      // If the dataset is not part of the file name, there is no fixed pattern, but the
      // axis is part of the file name, so add it to axisPattern.
      axisPattern = Pattern.quote(axisExtension);
    }
    Iterator iterator = namedFileTypeList.iterator();
    AxisType axisType = null;
    if (manager != null) {
      axisType = manager.getBaseMetaData().getAxisType();
    }
    while (iterator.hasNext()) {
      FileType fileType = (FileType) iterator.next();
      // Ignore child file types. Return a file type that equals patterns and booleans.
      if (fileType.parentFileType == null && fileType.equals(axisType, fileName,
        usesDataset, usesAxisID, fixedPattern, axisPattern)) {
        return fileType;
      }
    }
    return null;
  }

  /**
   * Returns true if the file name can be matched to the fixedPattern, typeString,
   * axisPattern, and extension.
   *
   * @param manager
   * @param fileName
   * @param fixedPattern
   * @param axisPattern
   * @return
   * @deprecated 2/2/2019
   */
  public boolean equals(final AxisType axisType, final String fileName,
    final boolean usesDataset, final boolean usesAxisID, final String fixedPattern,
    final String axisPattern) {
    if (composite) {
      // Handle type files which are based on another file type but have their own
      // extension.
      if (subFileType != null && extension != null) {
        return subFileType.equals(axisType, fileName, usesDataset, usesAxisID,
          fixedPattern, axisPattern, Pattern.quote(extension));
      }
      // Handle file types with single and dual file types instead of descriptions.
      return getChildFileType(axisType).equals(axisType, fileName, usesDataset,
        usesAxisID, fixedPattern, axisPattern);
    }
    return usesDataset == this.usesDataset && usesAxisID == this.usesAxisID
      && fileName.matches(fixedPattern + Pattern.quote(typeString) + axisPattern
        + Pattern.quote(extension));
  }

  /**
   * Returns true if the file name can be matches to the fixedPattern, typeString,
   * axisPattern, and extensionPattern.
   *
   * @param manager
   * @param fileName
   * @param fixedPattern
   * @param axisPattern
   * @param extensionPattern
   * @return
   * @deprecated 2/2/2019
   */
  public boolean equals(final AxisType axisType, final String fileName,
    final boolean usesDataset, final boolean usesAxisID, final String fixedPattern,
    final String axisPattern, final String extensionPattern) {
    if (composite) {
      // Handle type files which are based on another file type but have their own
      // extension.
      if (subFileType != null) {
        return subFileType.equals(axisType, fileName, usesDataset, usesAxisID,
          fixedPattern, axisPattern, extensionPattern);
      }
      // Handle file types with single and dual file types instead of descriptions.
      return getChildFileType(axisType).equals(axisType, fileName, usesDataset,
        usesAxisID, fixedPattern, axisPattern, extensionPattern);
    }
    return usesDataset == this.usesDataset && usesAxisID == this.usesAxisID && fileName
      .matches(fixedPattern + Pattern.quote(typeString) + axisPattern + extensionPattern);
  }

  /**
   * Compares the member variables that make a file type unique:  usesDataset,
   * usesAxisID, typeString, and extension.
   *
   * @param fileType
   * @return
   * @deprecated 2/2/2019
   */
  public boolean equals(final AxisType axisType, final FileType fileType) {
    return equals(axisType, fileType.usesDataset, fileType.usesAxisID,
      fileType.typeString, fileType.extension);
  }

  /**
   * Returns true if the dataset, axis, type string and extension are equal.
   *
   * @param usesDataset
   * @param usesAxisID
   * @param typeString
   * @param extension
   * @return
   * @deprecated 2/2/2019
   */
  public boolean equals(final AxisType axisType, final boolean usesDataset,
    final boolean usesAxisID, final String typeString, final String extension) {
    if (composite) {
      // Handle type files which are based on another file type but have their own
      // extension.
      if (subFileType != null && this.extension != null) {
        return this.extension.equals(extension)
          && subFileType.equals(axisType, usesDataset, usesAxisID, typeString);
      }
      // Handle file types with single and dual file types instead of descriptions.
      return getChildFileType(axisType).equals(axisType, usesDataset, usesAxisID,
        typeString, extension);
    }
    return this.usesDataset == usesDataset && this.usesAxisID == usesAxisID
      && this.typeString.equals(typeString) && this.extension.equals(extension);
  }

  /**
   * Returns true if the dataset and axis settings, and type string are equal.  Ignores
   * the extension.
   *
   * @param usesDataset
   * @param usesAxisID
   * @param typeString
   * @return
   * @deprecated 2/2/2019
   */
  public boolean equals(final AxisType axisType, final boolean usesDataset,
    final boolean usesAxisID, final String typeString) {
    if (composite) {
      // Handle type files which are based on another file type
      if (subFileType != null) {
        return subFileType.equals(axisType, usesDataset, usesAxisID, typeString);
      }
      // Handle file types with single and dual file types instead of descriptions.
      return getChildFileType(axisType).equals(axisType, usesDataset, usesAxisID,
        typeString);
    }
    if (this.usesDataset == usesDataset && this.usesAxisID == usesAxisID
      && this.typeString.equals(typeString)) {
      return true;
    }
    return false;
  }

  /**
   * If no manager is available, assumes single axis
   *
   * @param manager
   * @return
   */
  private final FileType getChildFileType(final AxisType axisType) {
    if (composite) {
      if (singleFileType != null && dualFileType != null) {
        if (axisType == AxisType.DUAL_AXIS) {
          return dualFileType;
        }
        return singleFileType;
      }
      else if (subFileType != null) {
        return subFileType;
      }
    }
    return this;
  }

  /**
   * Return true if the name can be generated.  All files with a fileNamePattern can be
   * generated.
   * @deprecated 8/3/2019 Will be reduced to private and undeprecated.
   * @param axisType
   * @return
   */
  public boolean hasFixedName(final AxisType axisType) {
    if (fileNamePattern != null) {
      return true;
    }
    if (composite) {
      if (subFileType != null && extension != null) {
        return !extension.equals("") || subFileType.hasFixedName(axisType);
      }
      return getChildFileType(axisType).hasFixedName(axisType);
    }
    return usesAxisID || usesDataset || !extension.equals("") || !typeString.equals("");
  }

  final boolean isInSubdirectory() {
    return inSubdirectory;
  }

  /**
   * @deprecated 8/3/2019 make this private and undeprecate.
   * Get the extension for files with no pattern.
   * @param axisType
   * @return
   */
  public String getExtension(final AxisType axisType) {
    if (fileNamePattern != null) {
      // Don't have this information - this function should be private
      return null;
    }
    if (composite) {
      if (subFileType != null && extension != null) {
        return extension;
      }
      return getChildFileType(axisType).getExtension(axisType);
    }
    return extension;
  }

  public File getFile(final BaseManager manager, final AxisID axisID) {
    return getFile(manager, null, null, null, axisID, null, null, null, null);
  }

  /**
   * Pass in either, manager, metaData, or rootName and axisType.  If manager is null,
   * pass in propertyUserDir and fileSubdirectoryName if necessary.
   *
   * @param manager
   * @param metaData
   * @param rootName - overrides dataset name
   * @param axisType
   * @param axisID
   * @param propertyUserDir
   * @param fileSubdirectoryName
   * @return
   */
  final File getFile(final BaseManager manager, BaseMetaData metaData, String rootName,
    AxisType axisType, final AxisID axisID, String propertyUserDir,
    String fileSubdirectoryName, final String numeric1, final String numeric2) {
    if (metaData == null && manager != null) {
      metaData = manager.getBaseMetaData();
    }
    if (axisType == null) {
      if (metaData != null) {
        axisType = metaData.getAxisType();
      }
      else {
        axisType = AxisType.SINGLE_AXIS;
      }
    }
    if (rootName == null) {
      if (metaData != null) {
        rootName = metaData.getName();
      }
      else {
        rootName = "";
      }
    }
    if (fileNamePattern == null && !hasFixedName(axisType)) {
      return null;
    }
    String fileName = getFileName(manager, metaData, rootName, axisType, axisID, false,
      null, numeric1, numeric2);
    if (fileName == null || fileName.equals("")) {
      return null;
    }
    if (fileNamePattern != null) {
      return new File(new File(getDirectory(manager, metaData, rootName, axisType, axisID,
        propertyUserDir, numeric1, numeric2), fileName).getAbsolutePath());
    }
    // Make the file. The file may be in a subdirectory under IMOD_DIR, in a
    // subdirectory
    // below the dataset location, or in the dataset.
    String subdirName;
    if (inImodSubdirectory != null) {
      return new File(
        new File(new File(EtomoDirector.INSTANCE.getIMODDirectory(), inImodSubdirectory),
          fileName).getAbsolutePath());
    }
    if (propertyUserDir == null && manager != null) {
      propertyUserDir = manager.getPropertyUserDir();
    }
    if (inSubdirectory) {
      if (fileSubdirectoryName == null && manager != null) {
        fileSubdirectoryName = manager.getFileSubdirectoryName();
      }
      if (subdir != null) {
        return new File(new File(subdir.getFileName(manager, metaData, rootName, axisType,
          axisID, false, null, numeric1, numeric2), fileName).getAbsolutePath());
      }
      if (manager != null && fileSubdirectoryName != null) {
        return new File(
          new File(new File(propertyUserDir, fileSubdirectoryName), fileName)
            .getAbsolutePath());
      }
    }
    String dir = propertyUserDir;
    if (propertyUserDir == null) {
      dir = EtomoDirector.INSTANCE.getOriginalUserDir();
    }
    return new File(new File(dir, fileName).getAbsolutePath());
  }

  /**
   * Call when pattern is set.  Only uses the directory member variable.
   * @param manager
   * @param metaData
   * @param rootName
   * @param axisType
   * @param axisID
   * @param propertyUserDir
   * @return
   */
  private final File getDirectory(final BaseManager manager, BaseMetaData metaData,
    String rootName, AxisType axisType, final AxisID axisID, String propertyUserDir,
    final String numeric1, final String numeric2) {
    if (directory != null) {
      return directory.getFile(manager, metaData, rootName, axisType, axisID,
        propertyUserDir, null, numeric1, numeric2);
    }
    String directoryName = propertyUserDir;
    if (directoryName == null && manager != null) {
      directoryName = manager.getPropertyUserDir();
    }
    if (directoryName == null) {
      directoryName = EtomoDirector.INSTANCE.getOriginalUserDir();
    }
    return new File(directoryName);
  }

  public boolean exists(final BaseManager manager, final AxisID axisID) {
    File file = getFile(manager, null, null, null, axisID, null, null, null, null);
    if (file != null) {
      return file.exists();
    }
    return false;
  }

  public long lastModified(final BaseManager manager, final AxisID axisID) {
    File file = getFile(manager, null, null, null, axisID, null, null, null, null);
    if (file != null) {
      return file.lastModified();
    }
    return -1;
  }

  public boolean exists(final BaseManager manager, final BaseMetaData metaData,
    final AxisID axisID) {
    File file = getFile(manager, null, null, null, axisID, null, null, null, null);
    if (file != null) {
      return file.exists();
    }
    return false;
  }

  /**
   * Return the file name, stripped of its extension.
   *
   * @param manager
   * @param axisID
   * @return
   */
  public String getRoot(BaseManager manager, AxisID axisID) {
    // Template OK at least for now - the current template types have extra text after the
    // extension.
    String fileName =
      getFileName(manager, null, null, null, axisID, true, null, null, null);
    int index = fileName.lastIndexOf('.');
    return fileName.substring(0, index);
  }

  public String getTemplate(final BaseManager manager, final AxisID axisID) {
    return getFileName(manager, null, null, null, axisID, true, null, null, null);
  }

  @Override
  public String getFileName(final BaseManager manager, final AxisID axisID) {
    return getFileName(manager, null, null, null, axisID, false, null, null, null);
  }

  public String getFileName(final BaseManager manager, final AxisID axisID,
    final String numeric1, final String numeric2) {
    return getFileName(manager, null, null, null, axisID, false, null, numeric1,
      numeric2);
  }

  public String getFileName(final BaseManager manager, final String rootName,
    final AxisType axisType, final AxisID axisID) {
    return getFileName(manager, null, rootName, axisType, axisID, false, null, null,
      null);
  }

  public String getFileName(final BaseManager manager, final BaseMetaData metaData,
    final AxisID axisID) {
    return getFileName(manager, metaData, null, null, axisID, false, null, null, null);
  }

  /**
   * Builds file in a directory
   * @param manager
   * @param dir - location of file
   * @param rootName - overrides the dataset name
   * @param axisType
   * @param axisID
   * @return
   */
  public File getFile(final BaseManager manager, final File dir, final String rootName,
    final AxisType axisType, final AxisID axisID) {
    String fileName =
      getFileName(manager, null, rootName, axisType, axisID, false, null, null, null);
    if (fileName != null) {
      return new File(dir, fileName);
    }
    return null;
  }

  /**
   * Pass in either manager, metaData, or rootName and axisType
   *
   * @param manager
   * @param filenameSettings - required
   * @param metaData
   * @param rootName
   * @param axisType
   * @param axisID
   * @param templateOK
   * @return
   */
  // manager, null, null, null, axisID, true, null
  private final String getFileName(final BaseManager manager, BaseMetaData metaData,
    String rootName, AxisType axisType, final AxisID axisID, final boolean templateOK,
    final String version, final String numeric1, final String numeric2) {
    if (metaData == null && manager != null) {
      metaData = manager.getBaseMetaData();
    }
    if (axisType == null) {
      if (metaData != null) {
        axisType = metaData.getAxisType();
      }
      else {
        axisType = AxisType.SINGLE_AXIS;
      }
    }
    if (rootName == null) {
      if (metaData != null) {
        rootName = metaData.getName();
      }
      else {
        rootName = "";
      }
    }
    if (fileNamePattern == null && !hasFixedName(axisType)) {
      return null;
    }
    boolean includeExtension = true;
    if (template) {
      if (template && !templateOK && (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG)) {
        System.err.println("Warning:  Getting the file name of template " + toString());
      }
      if (dynamicLocation == DynamicLocation.AFTER_TYPE_STRING
        || dynamicLocation == DynamicLocation.AFTER_TYPE_STRING_AND_AFTER_MIDDLE_PIECE) {
        includeExtension = false;
      }
    }
    if (composite && (subFileType == null || extension == null)) {
      return getChildFileType(axisType).getFileName(manager, metaData, rootName, axisType,
        axisID, true, version, numeric1, numeric2);
    }
    if (fileNamePattern != null) {
      Extension origRawImageStackExtension;
      ImageFilenameStyle imageFilenameStyle;
      Extension rawImageStackExtension;
      if (metaData != null) {
        origRawImageStackExtension =
          Extension.getInstance(metaData.getOrigRawImageStackExtension().toString());
        imageFilenameStyle = metaData.getImageFilenameStyle();
        rawImageStackExtension = metaData.getRawImageStackExtension();
      }
      else {
        origRawImageStackExtension = null;
        ImageFileMetaData imageFileMetaData = new ImageFileMetaData(false, null, true);
        imageFilenameStyle = imageFileMetaData.getImageFilenameStyle();
        rawImageStackExtension = imageFileMetaData.getDefaultRawImageStackExtension();
      }
      return getFileNameFromPattern(rootName, axisType, axisID, numeric1, numeric2,
        origRawImageStackExtension, imageFilenameStyle, rawImageStackExtension);
    }
    return getLeftSide(rootName, axisType, axisID, version)
      + (includeExtension ? extension : "");
  }

  public String getFileName(final BaseManager manager, final String rootName,
    final AxisID axisID) {
    return getFileName(manager, null, rootName, null, axisID, false, null, null, null);
  }

  /**
   * Returns the concatenation of the left side of patternForLeftSide with all of
   * suffixPattern.
   * @param patternForLeftSide
   * @param suffixPattern
   */
  private static Object[] joinPatterns(final Object[] patternForLeftSide,
    final Object[] suffixPattern) {
    if (patternForLeftSide == null && suffixPattern == null) {
      return null;
    }
    if (patternForLeftSide == null || patternForLeftSide.length == 0) {
      return suffixPattern;
    }
    // Construct the new pattern array with the largest possible length.
    Object[] pattern = new Object[patternForLeftSide.length + suffixPattern.length];
    int lastIndex = -1;
    for (int i = 0; i < patternForLeftSide.length; i++) {
      // Don't use the extension from patternForLeftSide
      if (patternForLeftSide[i] instanceof ExtensionMarker) {
        break;
      }
      pattern[i] = patternForLeftSide[i];
      lastIndex = i;
    }
    // Add the suffix
    if (suffixPattern != null && suffixPattern.length > 0) {
      for (int i = 0; i < suffixPattern.length; i++) {
        pattern[++lastIndex] = suffixPattern[i];
      }
    }
    // Set nulls for the elements left over from not using the patternForLeftSide
    // extension.
    for (int i = lastIndex + 1; i < pattern.length; i++) {
      pattern[i] = null;
    }
    return pattern;
  }

  /**
   * Builds either a file name or a regular expression that matches the file name.
   */
  private static final class FileNameBuilder {
    private final StringBuilder builder = new StringBuilder();

    private final boolean buildRegex;
    private final AxisType firstAxisType;
    private final String rootName;
    // The extension includes the "."!
    private final String origRawStackExtension;

    private final String numeric1;
    private final String numeric2;
    private final String startQuote;
    private final String endQuote;

    private String axisIDExtension = "";// single axis pattern

    private int numericIndex = 0;
    private String endRegex = "$";

    /**
     * @param buildRegex - when true a regular expression is built, otherwise the file name is built
     * @param firstAxisType - when set, expects two patterns (or'ed together) - ignored when buildRegex is false
     * @param rootName - when null uses dataset regex
     * @param axisType - when null finds both types, overridden by firstAxisType when it is set and buildRegex is true
     * @param axisID - when null finds both axes if axisType allows
     * @param numeric1 - when null adds a regex
     * @param numeric2 - when null adds a regex
     */
    private FileNameBuilder(final boolean buildRegex, final AxisType firstAxisType,
      final String rootName, AxisType axisType, final AxisID axisID,
      final String numeric1, final String numeric2,
      final Extension origRawImageStackExtension) {
      this.buildRegex = buildRegex;
      this.numeric1 = numeric1;
      this.numeric2 = numeric2;
      // Quotes for creating a pattern match string.
      startQuote = buildRegex ? "\\Q" : "";
      endQuote = buildRegex ? "\\E" : "";
      if (buildRegex) {
        if (firstAxisType == AxisType.NOT_SET) {
          this.firstAxisType = null;
        }
        else {
          this.firstAxisType = firstAxisType;
        }
        if (this.firstAxisType != null) {
          axisType = this.firstAxisType;
          // Putting the two patterns in a required non-capturing group.
          builder.append("(?:");
        }
        builder.append("^");
      }
      else {
        // ignored when building a file name
        this.firstAxisType = null;
      }
      if (rootName == null) {
        if (buildRegex) {
          // If the rootName is missing use pattern match string for it
          this.rootName = Variable.DATASET.regex;
        }
        else {
          this.rootName = "";
        }
      }
      else {
        this.rootName = startQuote + rootName + endQuote;
      }
      // The origRawStackExtension is currently only used in MTF_FILTER_MDOC. It comes
      // from MetaData - and only from interfaces which use a single raw stack (in other
      // words tomogram recontruction). It is the extension of the raw stack file the user
      // picked.
      if (origRawImageStackExtension == null) {
        if (buildRegex) {
          // If the origRawStackExtension is missing use pattern match string for it
          this.origRawStackExtension = Variable.ORIG_RAW_IMAGE_EXTENSION.regex;
        }
        else {
          this.origRawStackExtension = "";
        }
      }
      else {
        this.origRawStackExtension = startQuote + Extension.EXTENSION_DIVIDER
          + origRawImageStackExtension.toString() + endQuote;
      }
      if (!buildRegex || axisID != null) {
        axisIDExtension = axisID.getExtension();
      }
      else if (axisType == AxisType.DUAL_AXIS) {
        axisIDExtension = Variable.AXIS.dualRegex;
      }
      else if (axisType == null) {
        // match both single and dual.
        axisIDExtension = Variable.AXIS.regex;
      }
    }

    private boolean startSecondPattern() {
      if (firstAxisType == null || firstAxisType == AxisType.NOT_SET) {
        // There is no non-capturing group set up - do not add a second pattern.
        return false;
      }
      builder.append("|");
      // End pattern and non-capturing group when done is called.
      endRegex = "$)";
      if (firstAxisType == AxisType.DUAL_AXIS) {
        axisIDExtension = "";
      }
      else if (firstAxisType == AxisType.SINGLE_AXIS) {
        axisIDExtension = Variable.AXIS.dualRegex;
      }
      return true;
    }

    private void append(final Variable variable) {
      if (variable.numeric) {
        if (numericIndex == 0 && numeric1 != null) {
          numericIndex++;
          builder.append(startQuote + numeric1 + endQuote);
        }
        else if (numericIndex == 1 && numeric2 != null) {
          numericIndex++;
          builder.append(startQuote + numeric2 + endQuote);
        }
        else if (buildRegex) {
          // Use the variable's pattern match string if the corresponding parameter is
          // not set.
          builder.append(variable.regex);
        }
      }
      else if (variable == Variable.DATASET) {
        builder.append(rootName);
      }
      else if (variable == Variable.AXIS) {
        builder.append(axisIDExtension);
      }
      else if (variable == Variable.DATASET_AND_AXIS) {
        builder.append(rootName);
        builder.append(axisIDExtension);
      }
      else if (variable == Variable.ORIG_RAW_IMAGE_EXTENSION) {
        builder.append(origRawStackExtension);
      }
    }

    private void append(final String string) {
      if (string != null && !string.isEmpty()) {
        builder.append(startQuote + string + endQuote);
      }
    }

    private void done() {
      if (buildRegex) {
        builder.append(endRegex);
      }
    }

    public String toString() {
      return builder.toString();
    }
  }

  /**
   * Returns a file name search string (regular expression).  Requires fileNamePattern.
   * All fields are optional.
   * @param manager - settings from the environment are used if manager is null
   * @param rootName - when null uses a search pattern
   * @param axisType - when null or NOT_SET looks for both types - not set from manager
   * @param axisID - when null looks for both axes if axisType is null or dual
   * @return
   */
  public final String getRegex(final String rootName, AxisType axisType, AxisID axisID,
    final ImageFilenameStyle imageFilenameStyle, final Extension rawStackExtenson) {
    if (fileNamePattern == null) {
      return null;
    }
    // Allow regular expression to contain patterns instead of specific axis information.
    if (axisType == AxisType.NOT_SET) {
      axisType = null;
    }
    if (axisID != null) {
      axisID = correctAxisID(axisType, axisID);
    }
    AxisType firstAxisType = null;
    AxisType patternAxisType = null;
    if (singleAxisFileNamePattern != null) {
      if (axisType == null) {
        // Will have to "or" the two patterns together so the regex can match both axis
        // types. Tell the builder which axis type it is starting with.
        firstAxisType = AxisType.DUAL_AXIS;
      }
      else if (axisType == AxisType.SINGLE_AXIS) {
        patternAxisType = AxisType.SINGLE_AXIS;
      }
    }
    FileNameBuilder builder = new FileNameBuilder(true, firstAxisType, rootName, axisType,
      axisID, null, null, null);
    String regex =
      buildPattern(builder, patternAxisType, imageFilenameStyle, rawStackExtenson);
    if (firstAxisType == null || !builder.startSecondPattern()) {
      return regex;
    }
    // Add the single axis pattern
    return buildPattern(builder, AxisType.SINGLE_AXIS, imageFilenameStyle,
      rawStackExtenson);
  }

  /**
   * Returns a file name.  Requires fileNamePattern.
   * @param settings
   * @param rootName - when null uses a empty root name
   * @param axisType - defaults to single axis
   * @param axisID - AxisID.SECOND overrides axisType, otherwise follows axisType and defaults to FIRST when dual
   * @param numeric1 - required if the file has a variable piece
   * @param numeric2 - required if the file has a second variable piece
   * @return
   */
  private final String getFileNameFromPattern(String rootName, AxisType axisType,
    AxisID axisID, final String numeric1, final String numeric2,
    final Extension origRawImageStackExtension,
    final ImageFilenameStyle imageFilenameStyle, final Extension rawStackExtenson) {
    if (fileNamePattern == null) {
      return null;
    }
    if (axisType == null || axisType == AxisType.NOT_SET) {
      axisType = AxisType.SINGLE_AXIS;
    }
    axisID = correctAxisID(axisType, axisID);
    FileNameBuilder builder = new FileNameBuilder(false, null, rootName, axisType, axisID,
      numeric1, numeric2, origRawImageStackExtension);
    AxisType patternAxisType = null;
    if (singleAxisFileNamePattern != null && axisType == AxisType.SINGLE_AXIS) {
      patternAxisType = AxisType.SINGLE_AXIS;
    }
    return buildPattern(builder, patternAxisType, imageFilenameStyle, rawStackExtenson);
  }

  /**
   * fileNamePattern is required for this function
   * @param rootName
   * @param axisType
   * @param axisID
   * @param sections - if not null, will be filled with each section the file name, with null for the variable sections between them
   * @return
   */
  private final String buildPattern(final FileNameBuilder builder,
    final AxisType patternAxisType, final ImageFilenameStyle imageFilenameStyle,
    final Extension rawStackExtenson) {
    Object[] pattern = fileNamePattern;
    if (singleAxisFileNamePattern != null && patternAxisType == AxisType.SINGLE_AXIS) {
      pattern = singleAxisFileNamePattern;
    }
    ExtensionMarker extensionMarker = null;
    boolean addExtensionFromSettings = false;
    for (int i = 0; i < pattern.length; i++) {
      if (pattern[i] instanceof FileType) {
        ((FileType) pattern[i]).buildPattern(builder, patternAxisType, imageFilenameStyle,
          rawStackExtenson);
      }
      else if (pattern[i] instanceof Variable) {
        builder.append((Variable) pattern[i]);
      }
      else if (pattern[i] instanceof ExtensionMarker) {
        // There can be multiple extension at the end of the file name because a one file
        // name can be derived from another. Each extension needs to be processed the
        // same way.
        extensionMarker = (ExtensionMarker) pattern[i];
        if (extensionMarker == ExtensionMarker.INPUT_IMAGE) {
          // The standard extension is always required for input images. Substitute the
          // correct extension.
          addExtensionFromSettings = true;
          break;
        }
        // Place the correct divider for this file name.
        if (extensionMarker == ExtensionMarker.IMAGE
          && extensionMarker.usesStandardExtension(imageFilenameStyle)) {
          // Use the standard extension
          addExtensionFromSettings = true;
          if (!extensionMarker.isCompatible(i, pattern)) {
            // First standardize old style file name (dataset.flat => dataset_flat).
            builder.append(Extension.STANDARDIZATION_DIVIDER);
          }
          else {
            // The standard extension is required when the file style is standard.
            // Substitute the correct extension.
            break;
          }
        }
        else {
          builder.append(Extension.EXTENSION_DIVIDER);
        }
      }
      else if (pattern[i] != null) {
        builder.append(pattern[i].toString());
      }
    }
    // Add the standard extension if necessary.
    if (addExtensionFromSettings) {
      if (extensionMarker == ExtensionMarker.IMAGE) {
        // This doesn't make much sense if this was old style, but
        // addExtensionFromSettings shouldn't be true in cases like that.
        Extension extension = imageFilenameStyle.getDefaultRawImageStackExtension();
        if (extension != null) {
          builder.append(Extension.EXTENSION_DIVIDER);
          builder.append(extension.toString());
        }
      }
      else if (extensionMarker == ExtensionMarker.INPUT_IMAGE) {
        builder.append(Extension.EXTENSION_DIVIDER);
        builder.append(rawStackExtenson.toString());
      }
    }
    builder.done();
    return builder.toString();
  }

  /**
   * Get the typeString with the dataset and axis letter added as necessary.  For example,
   * the left side of BBa_fixed.st is "BBa_fixed", the left side of tilta.com is "tilta", the
   * left side of tilt.com is "tilt", and the left side of tilta_for_sirt.com is "tilta".
   *
   * @param rootName
   * @param axisType
   * @param axisID
   * @param version - always at the end of the left side
   * @return
   */
  private final String getLeftSide(String rootName, AxisType axisType,
    final AxisID axisID, String version) {
    if (rootName == null) {
      rootName = "";
    }
    if (version == null) {
      version = "";
    }
    if (axisType == null || axisType == AxisType.NOT_SET) {
      axisType = AxisType.SINGLE_AXIS;
    }
    if (!hasFixedName(axisType)) {
      return null;
    }
    if (composite) {
      return getChildFileType(axisType).getLeftSide(rootName, axisType, axisID, version);
    }
    if (!usesDataset && !usesAxisID) {
      // Example: flatten.com
      return typeString + version;
    }
    String axisIDExtension = "";
    if (usesAxisID) {
      axisIDExtension = correctAxisID(axisType, axisID).getExtension();
    }
    if (usesDataset) {
      // With the dataset the axis follows the dataset
      // Example: BBa_erase.fid
      return rootName + axisIDExtension + typeString + version;
    }
    // Without the dataset the axis follows the left extension
    // Example: tilta.com
    return typeString + axisIDExtension + version;
  }

  /**
   * Derive a file name with the same type as this instance, but with a different root
   * name and/or a different axis type as the manager parameter.
   *
   * @param rootName
   * @param axisType
   * @param axisID
   * @return
   */
  public String deriveFileName(final String rootName, final AxisType axisType,
    final AxisID axisID, final ImageFilenameStyle imageFilenameStyle,
    final Extension rawStackExtenson) {
    if (fileNamePattern != null) {
      return getFileNameFromPattern(rootName, axisType, axisID, null, null, null,
        imageFilenameStyle, rawStackExtenson);
    }
    return getLeftSide(rootName, axisType, axisID, null) + getExtension(axisType);
  }

  /**
   * Returns the non-generic part of the the left side of the file name.  For
   * example, the type string for BBa_fixed.st is "_fixed", the type string
   * for tilta.com is "tilt", and the type string for tilt.com is "tilt".
   *
   * @return
   * @deprecated 8/2/2019
   */
  public String getTypeString(final AxisType axisType) {
    if (fileNamePattern != null) {
      return null;
    }
    if (composite) {
      return getChildFileType(axisType).getTypeString(axisType);
    }
    return typeString;
  }

  /**
   * Returns the non-generic part of the the left side of the file name.  For
   * example, the type string for BBa_fixed.st is "_fixed", the type string
   * for tilta.com is "tilt", and the type string for tilt.com is "tilt".
   * <p/>
   * WARNING: Does not get the child file type.  Ignores the composite setting.  Just
   * returns the string in this instance.
   *
   * @return
   */
  public String getTypeString() {
    return typeString;
  }

  /**
   * Templates only.  Returns a part of the file name between the type string and the
   * extension.  Example:  In the Gaussian filter output file template of format
   * "dataset_gfc0.xxx-f0.xxx.mrc", "-f" is the middle piece.
   * @return
   */
  public String getMiddlePiece() {
    return templateOnlyMiddlePiece;
  }

  /**
   * Returns the extension.
   * <p/>
   * WARNING: Does not get the child file type.  Ignores the composite setting.  Just
   * returns the string in this instance.
   *
   * @return
   */
  // TODO 2206
  public String getExtension() {
    return extension;
  }

  public boolean usesDataset() {
    return usesDataset;
  }

  public boolean usesAxisID() {
    return usesAxisID;
  }

  /**
   * A null axisID or an ONLY axisID is sometimes used to signify a FIRST axisID
   * in a dual axis dataset.  A similar problem may exist for single axis
   * datasets.  The axisID must be corrected to get a valid file name.
   * <p/>
   * This is not true for Tomogram Combination file names, which do not have an
   * axisID letter (equivalent to AxisID.ONLY).  However these files would have
   * the usesAxisID member variable set to false, so that is not a problem.
   *
   * @param axisType
   * @param axisID - AxisID.SECOND takes precidence over AxisType
   * @return corrected axisID
   */
  private static AxisID correctAxisID(AxisType axisType, AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return axisID;
    }
    if (axisType == AxisType.DUAL_AXIS) {
      return AxisID.FIRST;
    }
    return AxisID.ONLY;
  }

  /**
   * @return namedFileTypeList.iterator()
   * @deprecated 3/14/2019
   */
  static Iterator iterator() {
    return namedFileTypeList.iterator();
  }

  @Override
  public String getDescription() {
    String retval = super.getDescription();
    if (retval != null) {
      return retval;
    }
    if (this == FIDUCIAL_3D_MODEL) {
      return "FIDUCIAL_3D_MODEL";
    }
    if (this == ALIGNED_STACK) {
      return "ALIGNED_STACK";
    }
    if (this == XCORR_BLEND_OUTPUT) {
      return "XCORR_BLEND_OUTPUT";
    }
    if (this == DISTORTION_CORRECTED_STACK) {
      return "DISTORTION_CORRECTED_STACK";
    }
    if (this == FIDUCIAL_MODEL) {
      return "FIDUCIAL_MODEL";
    }
    if (this == FLATTEN_TOOL_OUTPUT) {
      return "FLATTEN_TOOL_OUTPUT";
    }
    if (this == JOIN) {
      return "JOIN";
    }
    if (this == ANISOTROPIC_DIFFUSION_OUTPUT) {
      return "ANISOTROPIC_DIFFUSION_OUTPUT";
    }
    if (this == PREALIGNED_STACK) {
      return "PREALIGNED_STACK";
    }
    if (this == RAW_TILT_ANGLES) {
      return "RAW_TILT_ANGLES";
    }
    if (this == TRIM_VOL_OUTPUT) {
      return "TRIM_VOL_OUTPUT";
    }
    if (this == JOIN_SAMPLE_AVERAGES) {
      return "JOIN_SAMPLE_AVERAGES";
    }
    if (this == JOIN_SAMPLE) {
      return "JOIN_SAMPLE";
    }
    if (this == SQUEEZE_VOL_OUTPUT) {
      return "SQUEEZE_VOL_OUTPUT";
    }
    if (this == NEWST_OR_BLEND_3D_FIND_OUTPUT) {
      return "NEWST_OR_BLEND_3D_FIND_OUTPUT";
    }
    if (this == FIND_BEADS_3D_OUTPUT_MODEL) {
      return "FIND_BEADS_3D_OUTPUT_MODEL";
    }
    if (this == TILT_3D_FIND_OUTPUT) {
      return "TILT_3D_FIND_OUTPUT";
    }
    if (this == SMOOTHING_ASSESSMENT_OUTPUT_MODEL) {
      return "SMOOTHING_ASSESSMENT_OUTPUT_MODEL";
    }
    if (this == CTF_CORRECTED_STACK) {
      return "CTF_CORRECTED_STACK";
    }
    if (this == CTF_CORRECTION_COMSCRIPT) {
      return "CTF_CORRECTION_COMSCRIPT";
    }
    if (this == ERASED_BEADS_STACK) {
      return "ERASED_BEADS_STACK";
    }
    if (this == CCD_ERASER_BEADS_INPUT_MODEL) {
      return "CCD_ERASER_BEADS_INPUT_MODEL";
    }
    if (this == MTF_FILTERED_STACK) {
      return "MTF_FILTERED_STACK";
    }
    if (this == FIND_BEADS_3D_COMSCRIPT) {
      return "FIND_BEADS_3D_COMSCRIPT";
    }
    if (this == FIXED_XRAYS_STACK) {
      return "FIXED_XRAYS_STACK";
    }
    if (this == FLATTEN_WARP_INPUT_MODEL) {
      return "FLATTEN_WARP_INPUT_MODEL";
    }
    if (this == FLATTEN_COMSCRIPT) {
      return "FLATTEN_COMSCRIPT";
    }
    if (this == FLATTEN_OUTPUT) {
      return "FLATTEN_OUTPUT";
    }
    if (this == FLATTEN_TOOL_COMSCRIPT) {
      return "FLATTEN_TOOL_COMSCRIPT";
    }
    if (this == TILT_OUTPUT) {
      return "TILT_OUTPUT";
    }
    if (this == SIRT_SCALED_OUTPUT_TEMPLATE) {
      return "SIRT_SCALED_OUTPUT";
    }
    if (this == SIRT_OUTPUT_TEMPLATE) {
      return "SIRT_OUTPUT";
    }
    if (this == MODELED_JOIN) {
      return "MODELED_JOIN";
    }
    if (this == MTF_FILTER_COMSCRIPT) {
      return "MTF_FILTER_COMSCRIPT";
    }
    if (this == ORIGINAL_RAW_STACK) {
      return "ORIGINAL_RAW_STACK";
    }
    if (this == PATCH_VECTOR_MODEL) {
      return "PATCH_VECTOR_MODEL";
    }
    if (this == PATCH_VECTOR_CCC_MODEL) {
      return "PATCH_VECTOR_CCC_MODEL";
    }
    if (this == PATCH_TRACKING_BOUNDARY_MODEL) {
      return "PATCH_TRACKING_BOUNDARY_MODEL";
    }
    if (this == TRANSFORMED_REFINING_MODEL) {
      return "TRANSFORMED_REFINING_MODEL";
    }
    if (this == SIRTSETUP_COMSCRIPT) {
      return "SIRTSETUP_COMSCRIPT";
    }
    if (this == COMBINED_VOLUME) {
      return "COMBINED_VOLUME";
    }
    if (this == NAD_TEST_INPUT) {
      return "NAD_TEST_INPUT";
    }
    if (this == TILT_COMSCRIPT) {
      return "TILT_COMSCRIPT";
    }
    if (this == TILT_FOR_SIRT_COMSCRIPT) {
      return "TILT_FOR_SIRT_COMSCRIPT";
    }
    if (this == TRACK_COMSCRIPT) {
      return "TRACK_COMSCRIPT";
    }
    if (this == TRIAL_JOIN) {
      return "TRIAL_JOIN";
    }
    if (this == CROSS_CORRELATION_COMSCRIPT) {
      return "CROSS_CORRELATION_COMSCRIPT";
    }
    if (this == PATCH_TRACKING_COMSCRIPT) {
      return "PATCH_TRACKING_COMSCRIPT";
    }
    if (this == AVERAGED_VOLUMES) {
      return "AVERAGED_VOLUMES";
    }
    if (this == NAD_TEST_VARYING_ITERATIONS) {
      return "NAD_TEST_VARYING_ITERATIONS";
    }
    if (this == NAD_TEST_VARYING_K) {
      return "NAD_TEST_VARYING_K";
    }
    if (this == POSITIONING_SAMPLE) {
      return "POSITIONING_SAMPLE";
    }
    if (this == REFERENCE_VOLUMES) {
      return "REFERENCE_VOLUMES";
    }
    if (this == TRIAL_TOMOGRAM) {
      return "TRIAL_TOMOGRAM";
    }
    return getFileFormatDescr();
  }

  final String getFileFormatDescr() {
    AxisType axisType = AxisType.SINGLE_AXIS;
    String rootName = "";
    if (fileNamePattern == null && !hasFixedName(axisType)) {
      return super.getDescription();
    }
    boolean includeExtension = true;
    if (template) {
      if (dynamicLocation == DynamicLocation.AFTER_TYPE_STRING
        || dynamicLocation == DynamicLocation.AFTER_TYPE_STRING_AND_AFTER_MIDDLE_PIECE) {
        includeExtension = false;
      }
    }
    if (composite && (subFileType == null || extension == null)) {
      return getChildFileType(axisType).getFileFormatDescr();
    }
    if (fileNamePattern != null) {
      Object[] pattern = fileNamePattern;
      if (singleAxisFileNamePattern != null) {
        pattern = singleAxisFileNamePattern;
      }
      return pattern.toString();
    }
    return getLeftSide(rootName, axisType, null, null)
      + (includeExtension ? extension : "");
  }

  /**
   * For template file names, this is part of the file name that changes.
   */
  private static final class DynamicLocation {
    // After extension example: .srecnn
    private static final DynamicLocation AFTER_EXTENSION = new DynamicLocation();
    // After type string example: dateset_slfinnnn.mrc
    private static final DynamicLocation AFTER_TYPE_STRING = new DynamicLocation();
    // After type string and middle piece example: dataset_gfc0.xxx-f0.xxx.mrc
    private static final DynamicLocation AFTER_TYPE_STRING_AND_AFTER_MIDDLE_PIECE =
      new DynamicLocation();

    private DynamicLocation() {}
  }

}
