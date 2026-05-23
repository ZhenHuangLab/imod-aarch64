package etomo.type;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.TestManager;
import junit.framework.TestCase;

/**
 * <p>Description: Configurable tests for diagnosing unit test failures without modifying
 * the original unit test.  Not kept up to date.</p>
 * 
 * <p>Copyright: Copyright 2020</p>
 *
 * <p>Organization:
 * Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
 * University of Colorado</p>
 * 
 * @author $Author$
 * 
 * @version $Revision$
 **/

public class FileTypeDiagnosticTest extends TestCase {
  public static final String rcsid =
    "$Id$";

  private static final String VARIABLE1_INT = "1234";
  private static final String VARIABLE1_FLOAT = "0.123";
  private static final String VARIABLE2_FLOAT = "0.456";

  protected void setUp() throws Exception {
    EtomoDirector.INSTANCE.closeCurrentManager(AxisID.ONLY, true);
    EtomoDirector.INSTANCE.openFrontPage(true, AxisID.ONLY, ImageFilenameStyle.OLD);
    super.setUp();
  }

  // FIXME 2206
  @SuppressWarnings("deprecation")
  public void testPatternRegression() {
    // Variable.DATASET_AND_AXIS, "_gfc", Variable.VARIABLE1, "-f", Variable.VARIABLE2,
    // ExtensionMarker.IMAGE, Extension.MRC
    patternRegressionTest(true, FileType.MUTLIFILT_GAUSSIAN_OUTPUT_TEMPLATE,
      FileType.MUTLIFILT_GAUSSIAN_OUTPUT_TEMPLATE_OLD, null, VARIABLE1_FLOAT,
      VARIABLE2_FLOAT, TestManager.DATASET_NAME,
      "_gfc" + VARIABLE1_FLOAT + "-f" + VARIABLE2_FLOAT);
    // Variable.DATASET_AND_AXIS, ExtensionMarker.IMAGE, Extension.ALI
    patternRegressionTest(false, FileType.ALIGNED_STACK, FileType.ALIGNED_STACK_OLD, null,
      null, null, TestManager.DATASET_NAME, "_ali");
    // Variable.DATASET_AND_AXIS, "_3dfind", ExtensionMarker.IMAGE, Extension.ALI
    patternRegressionTest(false, FileType.NEWST_OR_BLEND_3D_FIND_OUTPUT,
      FileType.NEWST_OR_BLEND_3D_FIND_OUTPUT_OLD, null, null, null,
      TestManager.DATASET_NAME, "_3dfind_ali");
    patternRegressionTest(false, FileType.CTF_CORRECTED_STACK,
      FileType.CTF_CORRECTED_STACK_OLD, null, null, null, null, null);
    patternRegressionTest(false, FileType.ERASED_BEADS_STACK,
      FileType.ERASED_BEADS_STACK_OLD, null, null, null, null, null);
    patternRegressionTest(false, FileType.MTF_FILTERED_STACK,
      FileType.MTF_FILTERED_STACK_OLD, null, null, null, null, null);
    // Variable.DATASET_AND_AXIS, ExtensionMarker.IMAGE, Extension.BL
    patternRegressionTest(false, FileType.XCORR_BLEND_OUTPUT,
      FileType.XCORR_BLEND_OUTPUT_OLD, null, null, null, TestManager.DATASET_NAME, "_bl");
    // Variable.DATASET_AND_AXIS, ExtensionMarker.IMAGE, Extension.DCST
    patternRegressionTest(false, FileType.DISTORTION_CORRECTED_STACK,
      FileType.DISTORTION_CORRECTED_STACK_OLD, null, null, null, TestManager.DATASET_NAME,
      "_dcst");
    // Variable.DATASET, ExtensionMarker.IMAGE, Extension.FLAT
    patternRegressionTest(false, FileType.FLATTEN_TOOL_OUTPUT,
      FileType.FLATTEN_TOOL_OUTPUT_OLD, null, null, null,
      TestManager.DATASET_NAME + "_flat", null);
    // "test", ExtensionMarker.IMAGE, Extension.INPUT
    patternRegressionTest(false, FileType.NAD_TEST_INPUT, FileType.NAD_TEST_INPUT_OLD,
      "naddir." + TestManager.DATASET_NAME, null, null, "test_input", null);
    // Variable.DATASET, ExtensionMarker.IMAGE, Extension.JOIN
    patternRegressionTest(false, FileType.JOIN, FileType.JOIN_OLD, null, null, null,
      TestManager.DATASET_NAME + "_join", null);
    // Variable.DATASET, "_modeled", ExtensionMarker.IMAGE, Extension.JOIN
    patternRegressionTest(false, FileType.MODELED_JOIN, FileType.MODELED_JOIN_OLD, null,
      null, null, TestManager.DATASET_NAME + "_modeled_join", null);
    patternRegressionTest(false, FileType.TRIAL_JOIN, FileType.TRIAL_JOIN_OLD, null, null,
      null, null, null);
    // Variable.DATASET_AND_AXIS, "_ali", ExtensionMarker.IMAGE, Extension.MRC
    patternRegressionTest(true, FileType.ALIGNED_STACK_MRC,
      FileType.ALIGNED_STACK_MRC_OLD, null, null, null, TestManager.DATASET_NAME, "_ali");
    patternRegressionTest(true, FileType.PREBLEND_OUTPUT_MRC,
      FileType.PREBLEND_OUTPUT_MRC_OLD, null, null, null, null, null);
    // Variable.DATASET_AND_AXIS, "_efos", Variable.PARAMETER, ExtensionMarker.IMAGE,
    // Extension.MRC
    patternRegressionTest(true, FileType.MUTLIFILT_EXACT_OBJECT_SIZES_OUTPUT_TEMPLATE,
      FileType.MUTLIFILT_EXACT_OBJECT_SIZES_OUTPUT_TEMPLATE_OLD, null, VARIABLE1_INT,
      null, TestManager.DATASET_NAME, "_efos" + VARIABLE1_INT);
    // Variable.DATASET_AND_AXIS, "_hlfs", Variable.VARIABLE1, ExtensionMarker.IMAGE,
    // Extension.MRC
    patternRegressionTest(true, FileType.MUTLIFILT_HAMMING_LIKE_STARTS_OUTPUT_TEMPLATE,
      FileType.MUTLIFILT_HAMMING_LIKE_STARTS_OUTPUT_TEMPLATE_OLD, null, VARIABLE1_FLOAT,
      null, TestManager.DATASET_NAME, "_hlfs" + VARIABLE1_FLOAT);
    patternRegressionTest(true, FileType.MUTLIFILT_FAKE_SIRT_ITERATIONS_OUTPUT_TEMPLATE,
      FileType.MUTLIFILT_FAKE_SIRT_ITERATIONS_OUTPUT_TEMPLATE_OLD, null, VARIABLE1_INT,
      null, null, null);
    // Variable.DATASET, ExtensionMarker.IMAGE, Extension.NAD
    patternRegressionTest(false, FileType.ANISOTROPIC_DIFFUSION_OUTPUT,
      FileType.ANISOTROPIC_DIFFUSION_OUTPUT_OLD, null, null, null,
      TestManager.DATASET_NAME + "_nad", null);
    // Variable.DATASET_AND_AXIS, ExtensionMarker.IMAGE, Extension.PREALI
    patternRegressionTest(false, FileType.PREALIGNED_STACK, FileType.PREALIGNED_STACK_OLD,
      null, null, null, TestManager.DATASET_NAME, "_preali");
    // Variable.DATASET, ExtensionMarker.IMAGE, Extension.REC
    patternRegressionTest(false, FileType.TRIM_VOL_OUTPUT, FileType.TRIM_VOL_OUTPUT_OLD,
      null, null, null, TestManager.DATASET_NAME + "_rec", null);
    // Variable.DATASET_AND_AXIS, "_3dfind", ExtensionMarker.IMAGE, Extension.REC
    patternRegressionTest(false, FileType.TILT_3D_FIND_OUTPUT,
      FileType.TILT_3D_FIND_OUTPUT_OLD, null, null, null, TestManager.DATASET_NAME,
      "_3dfind_rec");
    // "bot", Variable.AXIS, ExtensionMarker.IMAGE, Extension.REC
    patternRegressionTest(false, FileType.BOTTOM_SAMPLE, FileType.BOTTOM_SAMPLE_OLD, null,
      null, null, "bot", "_rec");
    patternRegressionTest(false, FileType.CRYO_POSITION_OUTPUT,
      FileType.CRYO_POSITION_OUTPUT_OLD, null, null, null, null, null);
    // Variable.DATASET, "_flat", ExtensionMarker.IMAGE, Extension.REC
    patternRegressionTest(false, FileType.FLATTEN_OUTPUT, FileType.FLATTEN_OUTPUT_OLD,
      null, null, null, TestManager.DATASET_NAME + "_flat_rec", null);
    patternRegressionTest(false, FileType.MIDDLE_SAMPLE, FileType.MIDDLE_SAMPLE_OLD, null,
      null, null, null, null);
    // "sum", ExtensionMarker.IMAGE, Extension.REC
    patternRegressionTest(false, FileType.COMBINED_VOLUME, FileType.COMBINED_VOLUME_OLD,
      null, null, null, "sum_rec", null);
    patternRegressionTest(false, FileType.TOP_SAMPLE, FileType.TOP_SAMPLE_OLD, null, null,
      null, null, null);
    // Variable.DATASET, ExtensionMarker.IMAGE, Extension.SAMPAVG
    patternRegressionTest(false, FileType.JOIN_SAMPLE_AVERAGES,
      FileType.JOIN_SAMPLE_AVERAGES_OLD, null, null, null,
      TestManager.DATASET_NAME + "_sampavg", null);
    // Variable.DATASET, ExtensionMarker.IMAGE, Extension.SAMPLE
    patternRegressionTest(false, FileType.JOIN_SAMPLE, FileType.JOIN_SAMPLE_OLD, null,
      null, null, TestManager.DATASET_NAME + "_sample", null);
    // Variable.DATASET_AND_AXIS, "_sub", ExtensionMarker.IMAGE, Extension.SINT,
    // Variable.VARIABLE1
    patternRegressionTest(false, FileType.SIRT_SUBAREA_SCALED_OUTPUT_TEMPLATE,
      FileType.SIRT_SUBAREA_SCALED_OUTPUT_TEMPLATE_OLD, null, VARIABLE1_INT, null,
      TestManager.DATASET_NAME, "_sub_sint" + VARIABLE1_INT);
    // Variable.DATASET, ExtensionMarker.IMAGE, Extension.SQZ
    patternRegressionTest(false, FileType.SQUEEZE_VOL_OUTPUT,
      FileType.SQUEEZE_VOL_OUTPUT_OLD, null, null, null,
      TestManager.DATASET_NAME + "_sqz", null);
    // Variable.DATASET_AND_AXIS, "_sub", ExtensionMarker.IMAGE, Extension.SREC,
    // Variable.VARIABLE1
    patternRegressionTest(false, FileType.SIRT_SUBAREA_OUTPUT_TEMPLATE,
      FileType.SIRT_SUBAREA_OUTPUT_TEMPLATE_OLD, null, VARIABLE1_INT, null,
      TestManager.DATASET_NAME, "_sub_srec" + VARIABLE1_INT);
    // Variable.DATASET_AND_AXIS, ExtensionMarker.INPUT_IMAGE, Extension.ST
    patternRegressionTest(false, FileType.RAW_STACK, FileType.RAW_STACK_OLD, null, null,
      null, TestManager.DATASET_NAME, "");
    // Variable.DATASET_AND_AXIS, "_fixed", ExtensionMarker.INPUT_IMAGE, Extension.ST
    patternRegressionTest(false, FileType.FIXED_XRAYS_STACK,
      FileType.FIXED_XRAYS_STACK_OLD, null, null, null, TestManager.DATASET_NAME,
      "_fixed");
    patternRegressionTest(false, FileType.ORIGINAL_RAW_STACK,
      FileType.ORIGINAL_RAW_STACK_OLD, null, null, null, null, null);
    //
    // Variable.DATASET_AND_AXIS, "_full", ExtensionMarker.IMAGE, Extension.REC
    patternRegressionTest(false, FileType.TILT_OUTPUT_SINGLE,
      FileType.TILT_OUTPUT_SINGLE_OLD, null, null, null, TestManager.DATASET_NAME,
      "_full_rec");
    FileType fileType = FileType.TILT_OUTPUT;
    patternRegressionTest(false, fileType, FileType.TILT_OUTPUT_OLD, null, null, null,
      null, null);
    // Variable.DATASET_AND_AXIS, "_full", ExtensionMarker.IMAGE, Extension.REC
    newFilenameCheckSingleAxis(fileType, null, null, TestManager.DATASET_NAME,
      "_full_rec");
    // Variable.DATASET_AND_AXIS, ExtensionMarker.IMAGE, Extension.REC
    newFilenameCheckDualAxis(fileType, null, null, TestManager.DATASET_NAME, "_rec");
    //
    fileType = FileType.SIRT_SCALED_OUTPUT_TEMPLATE;
    patternRegressionTest(false, fileType, FileType.SIRT_SCALED_OUTPUT_TEMPLATE_OLD, null,
      VARIABLE1_INT, null, null, null);
    // Uses TILT_OUTOUT left side: Variable.DATASET_AND_AXIS, "_full",
    // ExtensionMarker.IMAGE, Extension.SINT, Variable.VARIABLE1
    newFilenameCheckSingleAxis(fileType, VARIABLE1_INT, null, TestManager.DATASET_NAME,
      "_full_sint" + VARIABLE1_INT);
    // Uses TILT_OUTOUT left side: Variable.DATASET_AND_AXIS,
    // ExtensionMarker.IMAGE, Extension.SINT, Variable.VARIABLE1
    newFilenameCheckDualAxis(fileType, VARIABLE1_INT, null, TestManager.DATASET_NAME,
      "_sint" + VARIABLE1_INT);
    //
    fileType = FileType.SIRT_OUTPUT_TEMPLATE;
    patternRegressionTest(false, fileType, FileType.SIRT_OUTPUT_TEMPLATE_OLD, null,
      VARIABLE1_INT, null, null, null);
    // Uses TILT_OUTOUT left side: Variable.DATASET_AND_AXIS, "_full",
    // ExtensionMarker.IMAGE, Extension.SREC, Variable.VARIABLE1
    newFilenameCheckSingleAxis(fileType, VARIABLE1_INT, null, TestManager.DATASET_NAME,
      "_full_srec" + VARIABLE1_INT);
    // Uses TILT_OUTOUT left side: Variable.DATASET_AND_AXIS,
    // ExtensionMarker.IMAGE, Extension.SREC, Variable.VARIABLE1
    newFilenameCheckDualAxis(fileType, VARIABLE1_INT, null, TestManager.DATASET_NAME,
      "_srec" + VARIABLE1_INT);
  }

  private void patternRegressionTest(final boolean standardName, final FileType fileType,
    final FileType oldFileType, final String fileSubdirectoryName, final String variable1,
    final String variable2, final String leftOfAxis, final String rightOfAxis) {
    assertEquals("must have same imod manager key", fileType.getImodManagerKey(),
      oldFileType.getImodManagerKey());
    assertEquals("must have same description", fileType.getDescr(),
      oldFileType.getDescr());
    //
    // The old system cannot generate files with variable names.
    if (variable1 == null) {
      filenameEqualsTest(ImageFilenameStyle.OLD, fileType, oldFileType,
        fileSubdirectoryName);
      if (!standardName) {
        filenameNotEqualsTest(ImageFilenameStyle.MRC, fileType, oldFileType,
          fileSubdirectoryName);
      }
      else {
        filenameEqualsTest(ImageFilenameStyle.MRC, fileType, oldFileType,
          fileSubdirectoryName);
      }
      filenameNotEqualsTest(ImageFilenameStyle.HDF, fileType, oldFileType,
        fileSubdirectoryName);
    }
    //
    if (leftOfAxis != null) {
      newFilenameCheck(fileType, variable1, variable2, leftOfAxis, rightOfAxis);
    }
  }

  private void newFilenameCheck(final FileType fileType, final String variable1,
    final String variable2, final String leftOfAxis, final String rightOfAxis) {
    newFilenameCheckSingleAxis(fileType, variable1, variable2, leftOfAxis, rightOfAxis);
    newFilenameCheckDualAxis(fileType, variable1, variable2, leftOfAxis, rightOfAxis);
  }

  private void newFilenameCheckSingleAxis(final FileType fileType, final String variable1,
    final String variable2, final String leftOfAxis, final String rightOfAxis) {
    ImageFilenameStyle imageFilenameStyle = ImageFilenameStyle.MRC;
    filenameCheck(imageFilenameStyle,
      new TestManager(AxisType.SINGLE_AXIS, imageFilenameStyle), AxisID.ONLY, fileType,
      variable1, variable2, leftOfAxis, rightOfAxis);
    imageFilenameStyle = ImageFilenameStyle.HDF;
    filenameCheck(imageFilenameStyle,
      new TestManager(AxisType.SINGLE_AXIS, imageFilenameStyle), AxisID.ONLY, fileType,
      variable1, variable2, leftOfAxis, rightOfAxis);
  }

  private void newFilenameCheckDualAxis(final FileType fileType, final String variable1,
    final String variable2, final String leftOfAxis, final String rightOfAxis) {
    ImageFilenameStyle imageFilenameStyle = ImageFilenameStyle.MRC;
    BaseManager manager = new TestManager(AxisType.DUAL_AXIS, imageFilenameStyle);
    filenameCheck(imageFilenameStyle, manager, AxisID.FIRST, fileType, variable1,
      variable2, leftOfAxis, rightOfAxis);
    filenameCheck(imageFilenameStyle, manager, AxisID.SECOND, fileType, variable1,
      variable2, leftOfAxis, rightOfAxis);
    imageFilenameStyle = ImageFilenameStyle.HDF;
    manager = new TestManager(AxisType.DUAL_AXIS, imageFilenameStyle);
    filenameCheck(imageFilenameStyle, manager, AxisID.FIRST, fileType, variable1,
      variable2, leftOfAxis, rightOfAxis);
    filenameCheck(imageFilenameStyle, manager, AxisID.SECOND, fileType, variable1,
      variable2, leftOfAxis, rightOfAxis);
  }

  /**
   * A check for standardized filename
   * @param imageFilenameStyle
   * @param manager
   * @param axisID
   * @param fileType
   * @param leftOfAxis - left of axis letter
   * @param rightOfAxis - null if axis not used, or from right of axisID to the extension
   */
  private void filenameCheck(final ImageFilenameStyle imageFilenameStyle,
    final BaseManager manager, final AxisID axisID, final FileType fileType,
    final String variable1, final String variable2, final String leftOfAxis,
    final String rightOfAxis) {
    assertEquals("file name must standardize correctly",
      leftOfAxis + (rightOfAxis != null ? axisID.getExtension() + rightOfAxis : "")
        + Extension.EXTENSION_DIVIDER
        + imageFilenameStyle.getDefaultRawImageStackExtension(),
      fileType.getFileName(manager, axisID, variable1, variable2));
  }

  private void filenameEqualsTest(final ImageFilenameStyle imageFilenameStyle,
    final FileType fileType, final FileType oldFileType,
    final String fileSubdirectoryName) {
    filenameEqualsTest(new TestManager(AxisType.SINGLE_AXIS, imageFilenameStyle),
      AxisID.ONLY, fileType, oldFileType, fileSubdirectoryName);
    TestManager manager = new TestManager(AxisType.DUAL_AXIS, imageFilenameStyle);
    filenameEqualsTest(manager, AxisID.FIRST, fileType, oldFileType,
      fileSubdirectoryName);
    filenameEqualsTest(manager, AxisID.SECOND, fileType, oldFileType,
      fileSubdirectoryName);
  }

  private void filenameNotEqualsTest(final ImageFilenameStyle imageFilenameStyle,
    final FileType fileType, final FileType oldFileType,
    final String fileSubdirectoryName) {
    filenameNotEqualsTest(new TestManager(AxisType.SINGLE_AXIS, imageFilenameStyle),
      AxisID.ONLY, fileType, oldFileType, fileSubdirectoryName);
    TestManager manager = new TestManager(AxisType.DUAL_AXIS, imageFilenameStyle);
    filenameNotEqualsTest(manager, AxisID.FIRST, fileType, oldFileType,
      fileSubdirectoryName);
    filenameNotEqualsTest(manager, AxisID.SECOND, fileType, oldFileType,
      fileSubdirectoryName);
  }

  private void filenameEqualsTest(final TestManager manager, final AxisID axisID,
    final FileType fileType, final FileType oldFileType,
    final String fileSubdirectoryName) {
    assertEquals("fileType filename must match old filename",
      oldFileType.getFileName(manager, axisID), fileType.getFileName(manager, axisID));
    assertEquals(
      "pattern file must match old file", oldFileType.getFile(manager, null, null, null,
        axisID, null, fileSubdirectoryName, null, null),
      fileType.getFile(manager, axisID));
  }

  private void filenameNotEqualsTest(final TestManager manager, final AxisID axisID,
    final FileType pattern, final FileType oldFileType,
    final String fileSubdirectoryName) {
    assertFalse("with mrc pattern filename must not match old filename", oldFileType
      .getFileName(manager, axisID).equals(pattern.getFileName(manager, axisID)));
    assertFalse("with mrc pattern file must not match old file", oldFileType
      .getFile(manager, null, null, null, axisID, null, fileSubdirectoryName, null, null)
      .equals(pattern.getFile(manager, axisID)));
  }

  /**
   * This is the old version of TrimvolParam.getInputFileName.
   * @param axisType
   * @param datasetName
   * @return trimvol input file name
   */
  private static String getTrimvolInputFileName(AxisType axisType, String datasetName) {
    if (axisType == AxisType.SINGLE_AXIS) {
      return datasetName + "_full.rec";
    }
    return "sum.rec";
  }

}
