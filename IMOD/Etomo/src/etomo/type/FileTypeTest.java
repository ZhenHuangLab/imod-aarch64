package etomo.type;

import java.io.File;
import java.util.Iterator;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.FrontPageManager;
import etomo.TestManager;
import etomo.process.ImodManager;
import etomo.util.DatasetFiles;
import junit.framework.TestCase;

/**
 * <p>Description: Testing FileType for file name collisions.</p>
 * 
 * <p>Copyright: Copyright 2010</p>
 *
 * <p>Organization:
 * Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
 * University of Colorado</p>
 * 
 * @author $Author$
 * 
 * @version $Revision$
 * 
 * <p> $Log$
 * <p> Revision 1.3  2011/04/09 06:35:35  sueh
 * <p> bug# 1416 Added testGetFileName, testGetImodManagerKey, and testGetImodManagerKey2.
 * <p>
 * <p> Revision 1.2  2011/02/22 05:40:55  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.1  2010/04/28 16:29:05  sueh
 * <p> bug# 1344 Makes sure that the front page manager is the current
 * <p> manager.  Tests for file name collisions.  Tests getting FileType instances
 * <p> from name descriptions and file names.
 * <p> </p>
 */
public class FileTypeTest extends TestCase {
  public static final String rcsid = "$Id$";

  private static final String VARIABLE1_INT = "1234";
  private static final String VARIABLE1_FLOAT = "0.123";
  private static final String VARIABLE2_FLOAT = "0.456";

  protected void setUp() throws Exception {
    EtomoDirector.INSTANCE.closeCurrentManager(AxisID.ONLY, true);
    EtomoDirector.INSTANCE.openFrontPage(true, AxisID.ONLY, ImageFilenameStyle.OLD);
    super.setUp();
  }

  public void testMtfFilterMdoc() {
    TestManager manager = new TestManager(AxisType.SINGLE_AXIS, ImageFilenameStyle.OLD);
    manager.getBaseMetaData().setOrigRawImageStackExtension(Extension.ST);
    assertEquals("MTF_FILTER_MDOC uses orig raw stack extension",
      TestManager.DATASET_NAME + ".st.mdoc",
      FileType.MTF_FILTER_MDOC.getFileName(manager, AxisID.ONLY));
    TestManager dualManager = new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.OLD);
    manager.getBaseMetaData().setOrigRawImageStackExtension(Extension.TIF);
    assertEquals("MTF_FILTER_MDOC uses orig raw stack extension",
      TestManager.DATASET_NAME + "b.tif.mdoc",
      FileType.MTF_FILTER_MDOC.getFileName(manager, AxisID.SECOND));
  }

  /**
   * Regression testing for replacing informal image file name generation with FileType in
   * order to allow for the new style - multiple image file types (Bug# 2206).  This
   * compares old style image file name generation because the informal metheds will not
   * be upgraded to new style naming.  Deprecated elements can be removed after one year.
   * If dependency is removed, either remove the assertion or place the dependency in this
   * file.
   */
  @SuppressWarnings("deprecation")
  public void testAgainstInformalImageFileNames() {
    TestManager singleManager =
      new TestManager(AxisType.SINGLE_AXIS, ImageFilenameStyle.OLD);
    TestManager dualManager = new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.OLD);
    String datasetName = TestManager.DATASET_NAME;
    //
    TestManager manager = singleManager;
    AxisType axisType = AxisType.SINGLE_AXIS;
    AxisID axisID = AxisID.ONLY;
    assertEquals("From FinishjoinParam - old style should be the same",
      new File(manager.getPropertyUserDir(),
        TestManager.DATASET_NAME + DatasetFiles.JOIN_EXT),
      FileType.JOIN.getFile(manager, null));
    assertEquals(
      "FileType.TILT_OUTPUT with old style endings must equal "
        + "TrimvolParam.getInputFileName for single axis",
      getTrimvolInputFileName(axisType, datasetName), FileType.TILT_OUTPUT
        .getFileName(singleManager, datasetName, axisType, AxisID.ONLY));
    // BlendmodParam
    assertEquals(datasetName + axisID.getExtension() + ".ali",
      FileType.ALIGNED_STACK.getFileName(manager, axisID));
    //
    manager = dualManager;
    axisType = AxisType.DUAL_AXIS;
    axisID = AxisID.FIRST;
    assertEquals(
      "FileType.COMBINED_VOLUME with old style endings must equal "
        + "TrimvolParam.getInputFileName for dual axis",
      getTrimvolInputFileName(axisType, datasetName),
      FileType.COMBINED_VOLUME.getFileName(manager, datasetName, axisType, AxisID.ONLY));
    assertEquals(datasetName + axisID.getExtension() + ".ali",
      FileType.ALIGNED_STACK.getFileName(manager, axisID));
    //
    axisID = AxisID.FIRST;
    // Trimvol is in post processing so it only exists in the A frame - no need to test B.
    assertEquals(datasetName + axisID.getExtension() + ".ali",
      FileType.ALIGNED_STACK.getFileName(manager, axisID));
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

  /**
   * Test the file types with name descriptions to make sure that none of them
   * are identical.
   */
  @SuppressWarnings("deprecation")
  public void testForFileNameCollisions() {
    Iterator iterator = FileType.iterator();
    AxisType axisType = AxisType.DUAL_AXIS;
    while (iterator.hasNext()) {
      Iterator comparisonIterator = FileType.iterator();
      while (comparisonIterator.hasNext()) {
        FileType fileType = (FileType) iterator.next();
        FileType comparisonFileType = (FileType) comparisonIterator.next();
        if (fileType != comparisonFileType) {
          assertFalse(
            "File name collison: " + fileType + " is the same as " + comparisonFileType,
            fileType.equals(axisType, comparisonFileType));
        }
      }
    }
    axisType = AxisType.SINGLE_AXIS;
    while (iterator.hasNext()) {
      Iterator comparisonIterator = FileType.iterator();
      while (comparisonIterator.hasNext()) {
        FileType fileType = (FileType) iterator.next();
        FileType comparisonFileType = (FileType) comparisonIterator.next();
        if (fileType != comparisonFileType) {
          assertFalse(
            "File name collison: " + fileType + " is the same as " + comparisonFileType,
            fileType.equals(axisType, comparisonFileType));
        }
      }
    }
  }

  @SuppressWarnings("deprecation")
  public void testGetInstanceFromNameDescription() {
    AxisType axisType = AxisType.DUAL_AXIS;
    assertEquals("getInstance(boolean,boolean,string,string) failed",
      FileType.FIDUCIAL_3D_MODEL,
      FileType.getInstance(axisType, true, true, "", ".3dmod"));
    assertEquals("getInstance(boolean,boolean,string,string) failed",
      FileType.FLATTEN_TOOL_OUTPUT_OLD,
      FileType.getInstance(axisType, true, false, "", ".flat"));
    assertEquals("getInstance(boolean,boolean,string,string) failed",
      FileType.NEWST_OR_BLEND_3D_FIND_OUTPUT_OLD,
      FileType.getInstance(axisType, true, true, "_3dfind", ".ali"));
    assertEquals("getInstance(boolean,boolean,string,string) failed",
      FileType.FLATTEN_WARP_INPUT_MODEL,
      FileType.getInstance(axisType, true, false, "_flat", ".mod"));
    assertEquals("getInstance(boolean,boolean,string,string) failed",
      FileType.FIND_BEADS_3D_COMSCRIPT,
      FileType.getInstance(axisType, false, true, "findbeads3d", ".com"));
    assertEquals("getInstance(boolean,boolean,string,string) failed",
      FileType.SIRT_SCALED_OUTPUT_TEMPLATE_OLD,
      FileType.getInstance(axisType, true, true, "", ".sint"));
    assertEquals("getInstance(boolean,boolean,string,string) failed",
      FileType.SIRT_OUTPUT_TEMPLATE_OLD,
      FileType.getInstance(axisType, true, true, "", ".srec"));
    assertEquals("getInstance(boolean,boolean,string,string) failed",
      FileType.TILT_FOR_SIRT_COMSCRIPT,
      FileType.getInstance(axisType, false, true, "tilt", "_for_sirt.com"));
    axisType = AxisType.SINGLE_AXIS;
    assertEquals("getInstance(boolean,boolean,string,string) failed",
      FileType.TILT_OUTPUT_OLD,
      FileType.getInstance(axisType, true, true, "_full", ".rec"));
    assertEquals("getInstance(boolean,boolean,string,string) failed",
      FileType.SIRT_SCALED_OUTPUT_TEMPLATE_OLD,
      FileType.getInstance(axisType, true, true, "_full", ".sint"));
    assertEquals("getInstance(boolean,boolean,string,string) failed",
      FileType.SIRT_OUTPUT_TEMPLATE_OLD,
      FileType.getInstance(axisType, true, true, "_full", ".srec"));
    assertEquals("getInstance(boolean,boolean,string,string) failed",
      FileType.TILT_FOR_SIRT_COMSCRIPT,
      FileType.getInstance(axisType, false, true, "tilt", "_for_sirt.com"));
    assertEquals("should find an imod file based on the description",
      FileType.SLOPPY_BLEND_COMSCRIPT,
      FileType.getInstance(axisType, false, false, "sloppyblend", ".com"));
  }

  @SuppressWarnings("deprecation")
  public void testGetInstanceFromFileName() {
    FrontPageManager manager =
      (FrontPageManager) EtomoDirector.INSTANCE.getCurrentManagerForTest();
    FrontPageMetaData metaData = manager.getMetaData();
    metaData.setAxisType(AxisType.DUAL_AXIS);
    metaData.setName("BB");
    assertEquals("getInstance failed", FileType.TILT_OUTPUT_OLD,
      FileType.getInstance(manager, AxisID.FIRST, true, true, "BBa.rec"));
    assertEquals("getInstance failed", FileType.TILT_OUTPUT_OLD,
      FileType.getInstance(manager, AxisID.SECOND, true, true, "BBb.rec"));
    assertEquals("getInstance failed", FileType.CCD_ERASER_BEADS_INPUT_MODEL,
      FileType.getInstance(manager, AxisID.FIRST, true, true, "BBa_erase.fid"));
    assertEquals("getInstance failed", FileType.CCD_ERASER_BEADS_INPUT_MODEL,
      FileType.getInstance(manager, AxisID.SECOND, true, true, "BBb_erase.fid"));
    assertEquals("getInstance failed", FileType.CROSS_CORRELATION_COMSCRIPT,
      FileType.getInstance(manager, AxisID.FIRST, false, true, "xcorra.com"));
    assertEquals("getInstance failed", FileType.CROSS_CORRELATION_COMSCRIPT,
      FileType.getInstance(manager, AxisID.SECOND, false, true, "xcorrb.com"));
    assertEquals("getInstance failed", FileType.DISTORTION_CORRECTED_STACK_OLD,
      FileType.getInstance(manager, AxisID.FIRST, true, true, "BBa.dcst"));
    assertEquals("getInstance failed", FileType.DISTORTION_CORRECTED_STACK_OLD,
      FileType.getInstance(manager, AxisID.SECOND, true, true, "BBb.dcst"));
    assertEquals("getInstance failed", FileType.FLATTEN_COMSCRIPT,
      FileType.getInstance(manager, AxisID.FIRST, false, false, "flatten.com"));
    assertEquals("getInstance failed", FileType.FLATTEN_COMSCRIPT,
      FileType.getInstance(manager, AxisID.SECOND, false, false, "flatten.com"));
    assertEquals("getInstance failed", FileType.FLATTEN_TOOL_COMSCRIPT,
      FileType.getInstance(manager, AxisID.FIRST, true, false, "BB_flatten.com"));
    assertEquals("getInstance failed", FileType.FLATTEN_TOOL_COMSCRIPT,
      FileType.getInstance(manager, AxisID.SECOND, true, false, "BB_flatten.com"));
    assertEquals("getInstance failed", FileType.TILT_FOR_SIRT_COMSCRIPT,
      FileType.getInstance(manager, AxisID.FIRST, false, true, "tilta_for_sirt.com"));
    assertEquals("getInstance failed", FileType.TILT_FOR_SIRT_COMSCRIPT,
      FileType.getInstance(manager, AxisID.SECOND, false, true, "tiltb_for_sirt.com"));
    assertEquals("getInstance failed", FileType.SIRT_OUTPUT_TEMPLATE_OLD,
      FileType.getInstance(manager, AxisID.FIRST, true, true, "BBa.srec"));
    assertEquals("getInstance failed", FileType.SIRT_OUTPUT_TEMPLATE_OLD,
      FileType.getInstance(manager, AxisID.SECOND, true, true, "BBb.srec"));
    assertEquals("getInstance failed", FileType.ANISOTROPIC_DIFFUSION_OUTPUT_OLD,
      FileType.getInstance(manager, AxisID.FIRST, true, false, "BB.nad"));
    assertEquals("getInstance failed", FileType.ANISOTROPIC_DIFFUSION_OUTPUT_OLD,
      FileType.getInstance(manager, AxisID.SECOND, true, false, "BB.nad"));
    assertEquals("getInstance failed", FileType.COMBINED_VOLUME_OLD,
      FileType.getInstance(manager, AxisID.FIRST, false, false, "sum.rec"));
    assertEquals("getInstance failed", FileType.COMBINED_VOLUME_OLD,
      FileType.getInstance(manager, AxisID.SECOND, false, false, "sum.rec"));
    assertEquals("getInstance failed", FileType.CTF_CORRECTED_STACK_OLD,
      FileType.getInstance(manager, AxisID.FIRST, true, true, "BBa_ctfcorr.ali"));
    assertEquals("getInstance failed", FileType.CTF_CORRECTED_STACK_OLD,
      FileType.getInstance(manager, AxisID.SECOND, true, true, "BBb_ctfcorr.ali"));
    assertEquals("getInstance failed", FileType.FIDUCIAL_3D_MODEL,
      FileType.getInstance(manager, AxisID.FIRST, true, true, "BBa.3dmod"));
    assertEquals("getInstance failed", FileType.FIDUCIAL_3D_MODEL,
      FileType.getInstance(manager, AxisID.SECOND, true, true, "BBb.3dmod"));
    assertEquals("getInstance failed", FileType.FLATTEN_TOOL_OUTPUT_OLD,
      FileType.getInstance(manager, AxisID.FIRST, true, false, "BB.flat"));
    assertEquals("getInstance failed", FileType.FLATTEN_TOOL_OUTPUT_OLD,
      FileType.getInstance(manager, AxisID.SECOND, true, false, "BB.flat"));
    assertEquals("getInstance failed", FileType.NAD_TEST_INPUT_OLD,
      FileType.getInstance(manager, AxisID.FIRST, false, false, "test.input"));
    assertEquals("getInstance failed", FileType.NAD_TEST_INPUT_OLD,
      FileType.getInstance(manager, AxisID.SECOND, false, false, "test.input"));
    assertEquals("getInstance failed", FileType.RAW_STACK_OLD,
      FileType.getInstance(manager, AxisID.FIRST, true, true, "BBa.st"));
    assertEquals("getInstance failed", FileType.RAW_STACK_OLD,
      FileType.getInstance(manager, AxisID.SECOND, true, true, "BBb.st"));
    assertEquals("getInstance failed", FileType.ALIGNED_STACK_OLD,
      FileType.getInstance(manager, AxisID.FIRST, true, true, "BBa.ali"));
    assertEquals("getInstance failed", FileType.ALIGNED_STACK_OLD,
      FileType.getInstance(manager, AxisID.SECOND, true, true, "BBb.ali"));

    metaData.setAxisType(AxisType.SINGLE_AXIS);
    metaData.setName("BBa");
    assertEquals("getInstance failed", FileType.TILT_OUTPUT_OLD,
      FileType.getInstance(manager, AxisID.ONLY, true, true, "BBa_full.rec"));
    assertEquals("getInstance failed", FileType.CCD_ERASER_BEADS_INPUT_MODEL,
      FileType.getInstance(manager, AxisID.ONLY, true, true, "BBa_erase.fid"));
    assertEquals("getInstance failed", FileType.CROSS_CORRELATION_COMSCRIPT,
      FileType.getInstance(manager, AxisID.ONLY, false, true, "xcorr.com"));
    assertEquals("getInstance failed", FileType.DISTORTION_CORRECTED_STACK_OLD,
      FileType.getInstance(manager, AxisID.ONLY, true, true, "BBa.dcst"));
    assertEquals("getInstance failed", FileType.FLATTEN_COMSCRIPT,
      FileType.getInstance(manager, AxisID.ONLY, false, false, "flatten.com"));
    assertEquals("getInstance failed", FileType.FLATTEN_TOOL_COMSCRIPT,
      FileType.getInstance(manager, AxisID.ONLY, true, false, "BBa_flatten.com"));
    assertEquals("getInstance failed", FileType.TILT_FOR_SIRT_COMSCRIPT,
      FileType.getInstance(manager, AxisID.ONLY, false, true, "tilt_for_sirt.com"));
    assertEquals("getInstance failed", FileType.SIRT_OUTPUT_TEMPLATE_OLD,
      FileType.getInstance(manager, AxisID.ONLY, true, true, "BBa_full.srec"));
    assertEquals("getInstance failed", FileType.ANISOTROPIC_DIFFUSION_OUTPUT_OLD,
      FileType.getInstance(manager, AxisID.ONLY, true, false, "BBa.nad"));
    assertEquals("getInstance failed", FileType.COMBINED_VOLUME_OLD,
      FileType.getInstance(manager, AxisID.ONLY, false, false, "sum.rec"));
    assertEquals("getInstance failed", FileType.CTF_CORRECTED_STACK_OLD,
      FileType.getInstance(manager, AxisID.ONLY, true, true, "BBa_ctfcorr.ali"));
    assertEquals("getInstance failed", FileType.FIDUCIAL_3D_MODEL,
      FileType.getInstance(manager, AxisID.ONLY, true, true, "BBa.3dmod"));
    assertEquals("getInstance failed", FileType.FLATTEN_TOOL_OUTPUT_OLD,
      FileType.getInstance(manager, AxisID.ONLY, true, false, "BBa.flat"));
    assertEquals("getInstance failed", FileType.NAD_TEST_INPUT_OLD,
      FileType.getInstance(manager, AxisID.ONLY, false, false, "test.input"));
    assertEquals("getInstance failed", FileType.RAW_STACK_OLD,
      FileType.getInstance(manager, AxisID.ONLY, true, true, "BBa.st"));
    assertEquals("getInstance failed", FileType.ALIGNED_STACK_OLD,
      FileType.getInstance(manager, AxisID.ONLY, true, true, "BBa.ali"));
  }

  @SuppressWarnings("deprecation")
  public void testGetImodManagerKey() {
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.FIDUCIAL_3D_MODEL.getImodManagerKey(), ImodManager.FIDUCIAL_MODEL_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.ALIGNED_STACK.getImodManagerKey(), ImodManager.FINE_ALIGNED_KEY);
    assertNull("getImodManagerKey did not return null",
      FileType.XCORR_BLEND_OUTPUT_OLD.getImodManagerKey());
    assertNull("getImodManagerKey did not return null",
      FileType.DISTORTION_CORRECTED_STACK_OLD.getImodManagerKey());
    assertNull("getImodManagerKey did not return null",
      FileType.FIDUCIAL_MODEL.getImodManagerKey());
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.FLATTEN_TOOL_OUTPUT_OLD.getImodManagerKey(),
      ImodManager.FLATTEN_TOOL_OUTPUT_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.JOIN_OLD.getImodManagerKey(), ImodManager.JOIN_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.ANISOTROPIC_DIFFUSION_OUTPUT.getImodManagerKey(),
      ImodManager.ANISOTROPIC_DIFFUSION_VOLUME_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.PREALIGNED_STACK.getImodManagerKey(), ImodManager.COARSE_ALIGNED_KEY);
    assertNull("getImodManagerKey did not return null",
      FileType.RAW_TILT_ANGLES.getImodManagerKey());
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.TRIM_VOL_OUTPUT.getImodManagerKey(), ImodManager.TRIMMED_VOLUME_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.JOIN_SAMPLE_AVERAGES_OLD.getImodManagerKey(),
      ImodManager.JOIN_SAMPLE_AVERAGES_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.JOIN_SAMPLE.getImodManagerKey(), ImodManager.JOIN_SAMPLES_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.SQUEEZE_VOL_OUTPUT.getImodManagerKey(), ImodManager.SQUEEZED_VOLUME_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.RAW_STACK.getImodManagerKey(), ImodManager.RAW_STACK_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.NEWST_OR_BLEND_3D_FIND_OUTPUT_OLD.getImodManagerKey(),
      ImodManager.FINE_ALIGNED_3D_FIND_KEY);
    assertNull("getImodManagerKey did not return null",
      FileType.FIND_BEADS_3D_OUTPUT_MODEL.getImodManagerKey());
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.TILT_3D_FIND_OUTPUT_OLD.getImodManagerKey(),
      ImodManager.FULL_VOLUME_3D_FIND_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.SMOOTHING_ASSESSMENT_OUTPUT_MODEL.getImodManagerKey(),
      ImodManager.SMOOTHING_ASSESSMENT_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.CTF_CORRECTED_STACK_OLD.getImodManagerKey(),
      ImodManager.CTF_CORRECTION_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.ERASED_BEADS_STACK_OLD.getImodManagerKey(),
      ImodManager.ERASED_FIDUCIALS_KEY);
    assertNull("getImodManagerKey did not return null",
      FileType.CCD_ERASER_BEADS_INPUT_MODEL.getImodManagerKey());
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.MTF_FILTERED_STACK_OLD.getImodManagerKey(), ImodManager.MTF_FILTER_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.FIXED_XRAYS_STACK.getImodManagerKey(), ImodManager.ERASED_STACK_KEY);
    assertNull("getImodManagerKey did not return null",
      FileType.FLATTEN_WARP_INPUT_MODEL.getImodManagerKey());
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.FLATTEN_OUTPUT.getImodManagerKey(), ImodManager.FLAT_VOLUME_KEY);
    assertNull("getImodManagerKey did not return null",
      FileType.FLATTEN_TOOL_COMSCRIPT.getImodManagerKey());
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.MODELED_JOIN_OLD.getImodManagerKey(), ImodManager.MODELED_JOIN_KEY);
    assertNull("getImodManagerKey did not return null",
      FileType.ORIGINAL_RAW_STACK.getImodManagerKey());
    assertNull("getImodManagerKey did not return null",
      FileType.PATCH_TRACKING_BOUNDARY_MODEL.getImodManagerKey());
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.TRANSFORMED_REFINING_MODEL.getImodManagerKey(),
      ImodManager.TRANSFORMED_MODEL_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.TRIAL_JOIN_OLD.getImodManagerKey(), ImodManager.TRIAL_JOIN_KEY);
    assertNull("getImodManagerKey did not return null",
      FileType.CTF_CORRECTION_COMSCRIPT.getImodManagerKey());
    assertNull("getImodManagerKey did not return null",
      FileType.FIND_BEADS_3D_COMSCRIPT.getImodManagerKey());
    assertNull("getImodManagerKey did not return null",
      FileType.FLATTEN_COMSCRIPT.getImodManagerKey());
    assertNull("getImodManagerKey did not return null",
      FileType.MTF_FILTER_COMSCRIPT.getImodManagerKey());
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.PATCH_VECTOR_MODEL.getImodManagerKey(),
      ImodManager.PATCH_VECTOR_MODEL_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.PATCH_VECTOR_CCC_MODEL.getImodManagerKey(),
      ImodManager.PATCH_VECTOR_CCC_MODEL_KEY);
    assertNull("getImodManagerKey did not return null",
      FileType.SIRTSETUP_COMSCRIPT.getImodManagerKey());
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.COMBINED_VOLUME_OLD.getImodManagerKey(),
      ImodManager.COMBINED_TOMOGRAM_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.NAD_TEST_INPUT_OLD.getImodManagerKey(), ImodManager.TEST_VOLUME_KEY);
    assertNull("getImodManagerKey did not return null",
      FileType.TILT_COMSCRIPT.getImodManagerKey());
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.TILT_OUTPUT.getImodManagerKey(), ImodManager.FULL_VOLUME_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.SIRT_SCALED_OUTPUT_TEMPLATE.getImodManagerKey(), ImodManager.SIRT_KEY);
    assertSame("getImodManagerKey did not return the correct ImodManager key",
      FileType.SIRT_OUTPUT_TEMPLATE.getImodManagerKey(), ImodManager.SIRT_KEY);
    assertNull("getImodManagerKey did not return null",
      FileType.CROSS_CORRELATION_COMSCRIPT.getImodManagerKey());
    assertNull("getImodManagerKey did not return null",
      FileType.PATCH_TRACKING_COMSCRIPT.getImodManagerKey());
    assertNull("getImodManagerKey did not return null",
      FileType.TILT_FOR_SIRT_COMSCRIPT.getImodManagerKey());
  }

  @SuppressWarnings("deprecation")
  public void testGetImodManagerKey2() {
    assertNull("getImodManagerKey2 did not return null",
      FileType.FIDUCIAL_3D_MODEL.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.ALIGNED_STACK.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.XCORR_BLEND_OUTPUT_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.DISTORTION_CORRECTED_STACK_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.FIDUCIAL_MODEL.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.FLATTEN_TOOL_OUTPUT_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.JOIN_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.ANISOTROPIC_DIFFUSION_OUTPUT_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.PREALIGNED_STACK.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.RAW_TILT_ANGLES.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.TRIM_VOL_OUTPUT.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.JOIN_SAMPLE_AVERAGES_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.JOIN_SAMPLE.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.SQUEEZE_VOL_OUTPUT.getImodManagerKey2());
    assertSame("getImodManagerKey2 did not return the correct ImodManager key",
      FileType.RAW_STACK.getImodManagerKey2(), ImodManager.PREVIEW_KEY);
    assertNull("getImodManagerKey2 did not return null",
      FileType.NEWST_OR_BLEND_3D_FIND_OUTPUT_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.FIND_BEADS_3D_OUTPUT_MODEL.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.TILT_3D_FIND_OUTPUT_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.SMOOTHING_ASSESSMENT_OUTPUT_MODEL.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.CTF_CORRECTED_STACK_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.ERASED_BEADS_STACK_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.CCD_ERASER_BEADS_INPUT_MODEL.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.MTF_FILTERED_STACK_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.FIXED_XRAYS_STACK.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.FLATTEN_WARP_INPUT_MODEL.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.FLATTEN_OUTPUT.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.FLATTEN_TOOL_COMSCRIPT.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.MODELED_JOIN_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.ORIGINAL_RAW_STACK.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.PATCH_TRACKING_BOUNDARY_MODEL.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.TRANSFORMED_REFINING_MODEL.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.TRIAL_JOIN_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.CTF_CORRECTION_COMSCRIPT.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.FIND_BEADS_3D_COMSCRIPT.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.FLATTEN_COMSCRIPT.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.MTF_FILTER_COMSCRIPT.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.PATCH_VECTOR_MODEL.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.PATCH_VECTOR_CCC_MODEL.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.SIRTSETUP_COMSCRIPT.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.COMBINED_VOLUME_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.NAD_TEST_INPUT_OLD.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.TILT_COMSCRIPT.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.TILT_OUTPUT.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.SIRT_SCALED_OUTPUT_TEMPLATE.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.SIRT_OUTPUT_TEMPLATE.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.CROSS_CORRELATION_COMSCRIPT.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.PATCH_TRACKING_COMSCRIPT.getImodManagerKey2());
    assertNull("getImodManagerKey2 did not return null",
      FileType.TILT_FOR_SIRT_COMSCRIPT.getImodManagerKey2());
  }

  @SuppressWarnings("deprecation")
  public void testGetFileName() {
    FrontPageManager manager =
      (FrontPageManager) EtomoDirector.INSTANCE.getCurrentManagerForTest();
    FrontPageMetaData metaData = manager.getMetaData();
    // test dual axis
    metaData.setAxisType(AxisType.DUAL_AXIS);
    metaData.setName("BB");
    assertEquals("file name is wrong", "BBa.st",
      FileType.RAW_STACK.getFileName(manager, AxisID.FIRST));
    assertEquals("file name is wrong",
      FileType.RAW_STACK.getFileName(manager, AxisID.FIRST), "BBa.st");
    assertEquals("file name is wrong",
      FileType.FIDUCIAL_3D_MODEL.getFileName(manager, AxisID.FIRST), "BBa.3dmod");
    assertEquals("file name is wrong",
      FileType.FIDUCIAL_3D_MODEL.getFileName(manager, AxisID.SECOND), "BBb.3dmod");
    assertEquals("file name is wrong",
      FileType.ALIGNED_STACK.getFileName(manager, AxisID.FIRST), "BBa.ali");
    assertEquals("file name is wrong",
      FileType.ALIGNED_STACK.getFileName(manager, AxisID.SECOND), "BBb.ali");
    assertEquals("file name is wrong",
      FileType.XCORR_BLEND_OUTPUT.getFileName(manager, AxisID.FIRST), "BBa.bl");
    assertEquals("file name is wrong",
      FileType.XCORR_BLEND_OUTPUT.getFileName(manager, AxisID.SECOND), "BBb.bl");
    assertEquals("file name is wrong",
      FileType.DISTORTION_CORRECTED_STACK.getFileName(manager, AxisID.FIRST), "BBa.dcst");
    assertEquals("file name is wrong",
      FileType.DISTORTION_CORRECTED_STACK.getFileName(manager, AxisID.SECOND),
      "BBb.dcst");
    assertEquals("file name is wrong",
      FileType.FIDUCIAL_MODEL.getFileName(manager, AxisID.FIRST), "BBa.fid");
    assertEquals("file name is wrong",
      FileType.FIDUCIAL_MODEL.getFileName(manager, AxisID.SECOND), "BBb.fid");
    assertEquals("file name is wrong",
      FileType.FLATTEN_TOOL_OUTPUT.getFileName(manager, AxisID.FIRST), "BB.flat");
    assertEquals("file name is wrong",
      FileType.FLATTEN_TOOL_OUTPUT.getFileName(manager, AxisID.SECOND), "BB.flat");
    assertEquals("file name is wrong",
      FileType.JOIN_OLD.getFileName(manager, AxisID.FIRST), "BB.join");
    assertEquals("file name is wrong",
      FileType.JOIN_OLD.getFileName(manager, AxisID.SECOND), "BB.join");
    assertEquals("file name is wrong",
      FileType.ANISOTROPIC_DIFFUSION_OUTPUT.getFileName(manager, AxisID.FIRST), "BB.nad");
    assertEquals("file name is wrong",
      FileType.ANISOTROPIC_DIFFUSION_OUTPUT.getFileName(manager, AxisID.SECOND),
      "BB.nad");
    assertEquals("file name is wrong", "BBa.preali",
      FileType.PREALIGNED_STACK.getFileName(manager, AxisID.FIRST));
    assertEquals("file name is wrong",
      FileType.PREALIGNED_STACK.getFileName(manager, AxisID.SECOND), "BBb.preali");
    assertEquals("file name is wrong",
      FileType.RAW_TILT_ANGLES.getFileName(manager, AxisID.FIRST), "BBa.rawtlt");
    assertEquals("file name is wrong",
      FileType.RAW_TILT_ANGLES.getFileName(manager, AxisID.SECOND), "BBb.rawtlt");
    assertEquals("file name is wrong",
      FileType.TRIM_VOL_OUTPUT.getFileName(manager, AxisID.FIRST), "BB.rec");
    assertEquals("file name is wrong",
      FileType.TRIM_VOL_OUTPUT.getFileName(manager, AxisID.SECOND), "BB.rec");
    assertEquals("file name is wrong",
      FileType.JOIN_SAMPLE_AVERAGES.getFileName(manager, AxisID.FIRST), "BB.sampavg");
    assertEquals("file name is wrong",
      FileType.JOIN_SAMPLE_AVERAGES.getFileName(manager, AxisID.SECOND), "BB.sampavg");
    assertEquals("file name is wrong",
      FileType.JOIN_SAMPLE.getFileName(manager, AxisID.FIRST), "BB.sample");
    assertEquals("file name is wrong",
      FileType.JOIN_SAMPLE.getFileName(manager, AxisID.SECOND), "BB.sample");
    assertEquals("file name is wrong",
      FileType.SQUEEZE_VOL_OUTPUT.getFileName(manager, AxisID.FIRST), "BB.sqz");
    assertEquals("file name is wrong",
      FileType.SQUEEZE_VOL_OUTPUT.getFileName(manager, AxisID.SECOND), "BB.sqz");
    assertEquals("file name is wrong",
      FileType.RAW_STACK.getFileName(manager, AxisID.FIRST), "BBa.st");
    assertEquals("file name is wrong",
      FileType.RAW_STACK.getFileName(manager, AxisID.SECOND), "BBb.st");
    assertEquals("file name is wrong",
      FileType.NEWST_OR_BLEND_3D_FIND_OUTPUT.getFileName(manager, AxisID.FIRST),
      "BBa_3dfind.ali");
    assertEquals("file name is wrong",
      FileType.NEWST_OR_BLEND_3D_FIND_OUTPUT.getFileName(manager, AxisID.SECOND),
      "BBb_3dfind.ali");
    assertEquals("file name is wrong",
      FileType.FIND_BEADS_3D_OUTPUT_MODEL.getFileName(manager, AxisID.FIRST),
      "BBa_3dfind.mod");
    assertEquals("file name is wrong",
      FileType.FIND_BEADS_3D_OUTPUT_MODEL.getFileName(manager, AxisID.SECOND),
      "BBb_3dfind.mod");
    assertEquals("file name is wrong",
      FileType.TILT_3D_FIND_OUTPUT.getFileName(manager, AxisID.FIRST), "BBa_3dfind.rec");
    assertEquals("file name is wrong",
      FileType.TILT_3D_FIND_OUTPUT.getFileName(manager, AxisID.SECOND), "BBb_3dfind.rec");
    assertEquals("file name is wrong",
      FileType.SMOOTHING_ASSESSMENT_OUTPUT_MODEL.getFileName(manager, AxisID.FIRST),
      "BBa_checkflat.mod");
    assertEquals("file name is wrong",
      FileType.SMOOTHING_ASSESSMENT_OUTPUT_MODEL.getFileName(manager, AxisID.SECOND),
      "BBb_checkflat.mod");
    assertEquals("file name is wrong", "BBa_ctfcorr.ali",
      FileType.CTF_CORRECTED_STACK.getFileName(manager, AxisID.FIRST));
    assertEquals("file name is wrong",
      FileType.CTF_CORRECTED_STACK.getFileName(manager, AxisID.SECOND),
      "BBb_ctfcorr.ali");
    assertEquals("file name is wrong",
      FileType.ERASED_BEADS_STACK.getFileName(manager, AxisID.FIRST), "BBa_erase.ali");
    assertEquals("file name is wrong",
      FileType.ERASED_BEADS_STACK.getFileName(manager, AxisID.SECOND), "BBb_erase.ali");
    assertEquals("file name is wrong",
      FileType.CCD_ERASER_BEADS_INPUT_MODEL.getFileName(manager, AxisID.FIRST),
      "BBa_erase.fid");
    assertEquals("file name is wrong",
      FileType.CCD_ERASER_BEADS_INPUT_MODEL.getFileName(manager, AxisID.SECOND),
      "BBb_erase.fid");
    assertEquals("file name is wrong",
      FileType.MTF_FILTERED_STACK.getFileName(manager, AxisID.FIRST), "BBa_filt.ali");
    assertEquals("file name is wrong",
      FileType.MTF_FILTERED_STACK.getFileName(manager, AxisID.SECOND), "BBb_filt.ali");
    assertEquals("file name is wrong",
      FileType.FIXED_XRAYS_STACK.getFileName(manager, AxisID.FIRST), "BBa_fixed.st");
    assertEquals("file name is wrong",
      FileType.FIXED_XRAYS_STACK.getFileName(manager, AxisID.SECOND), "BBb_fixed.st");
    assertEquals("file name is wrong",
      FileType.FLATTEN_WARP_INPUT_MODEL.getFileName(manager, AxisID.FIRST),
      "BB_flat.mod");
    assertEquals("file name is wrong",
      FileType.FLATTEN_WARP_INPUT_MODEL.getFileName(manager, AxisID.SECOND),
      "BB_flat.mod");
    assertEquals("file name is wrong",
      FileType.FLATTEN_OUTPUT.getFileName(manager, AxisID.FIRST), "BB_flat.rec");
    assertEquals("file name is wrong",
      FileType.FLATTEN_OUTPUT.getFileName(manager, AxisID.SECOND), "BB_flat.rec");
    assertEquals("file name is wrong",
      FileType.FLATTEN_TOOL_COMSCRIPT.getFileName(manager, AxisID.FIRST),
      "BB_flatten.com");
    assertEquals("file name is wrong",
      FileType.FLATTEN_TOOL_COMSCRIPT.getFileName(manager, AxisID.SECOND),
      "BB_flatten.com");
    assertEquals("file name is wrong",
      FileType.MODELED_JOIN.getFileName(manager, AxisID.FIRST), "BB_modeled.join");
    assertEquals("file name is wrong",
      FileType.MODELED_JOIN.getFileName(manager, AxisID.SECOND), "BB_modeled.join");
    assertEquals("file name is wrong",
      FileType.ORIGINAL_RAW_STACK.getFileName(manager, AxisID.FIRST), "BBa_orig.st");
    assertEquals("file name is wrong",
      FileType.ORIGINAL_RAW_STACK.getFileName(manager, AxisID.SECOND), "BBb_orig.st");
    assertEquals("file name is wrong",
      FileType.PATCH_TRACKING_BOUNDARY_MODEL.getFileName(manager, AxisID.FIRST),
      "BBa_ptbound.mod");
    assertEquals("file name is wrong",
      FileType.PATCH_TRACKING_BOUNDARY_MODEL.getFileName(manager, AxisID.SECOND),
      "BBb_ptbound.mod");
    assertEquals("file name is wrong",
      FileType.TRANSFORMED_REFINING_MODEL.getFileName(manager, AxisID.FIRST),
      "BB_refine.alimod");
    assertEquals("file name is wrong",
      FileType.TRANSFORMED_REFINING_MODEL.getFileName(manager, AxisID.SECOND),
      "BB_refine.alimod");
    assertEquals("file name is wrong",
      FileType.TRIAL_JOIN.getFileName(manager, AxisID.FIRST), "BB_trial.join");
    assertEquals("file name is wrong",
      FileType.TRIAL_JOIN.getFileName(manager, AxisID.SECOND), "BB_trial.join");
    assertEquals("file name is wrong",
      FileType.CTF_CORRECTION_COMSCRIPT.getFileName(manager, AxisID.FIRST),
      "ctfcorrectiona.com");
    assertEquals("file name is wrong",
      FileType.CTF_CORRECTION_COMSCRIPT.getFileName(manager, AxisID.SECOND),
      "ctfcorrectionb.com");
    assertEquals("file name is wrong",
      FileType.FIND_BEADS_3D_COMSCRIPT.getFileName(manager, AxisID.FIRST),
      "findbeads3da.com");
    assertEquals("file name is wrong",
      FileType.FIND_BEADS_3D_COMSCRIPT.getFileName(manager, AxisID.SECOND),
      "findbeads3db.com");
    assertEquals("file name is wrong",
      FileType.FLATTEN_COMSCRIPT.getFileName(manager, AxisID.FIRST), "flatten.com");
    assertEquals("file name is wrong",
      FileType.FLATTEN_COMSCRIPT.getFileName(manager, AxisID.SECOND), "flatten.com");
    assertEquals("file name is wrong",
      FileType.MTF_FILTER_COMSCRIPT.getFileName(manager, AxisID.FIRST), "mtffiltera.com");
    assertEquals("file name is wrong",
      FileType.MTF_FILTER_COMSCRIPT.getFileName(manager, AxisID.SECOND),
      "mtffilterb.com");
    assertEquals("file name is wrong",
      FileType.PATCH_VECTOR_MODEL.getFileName(manager, AxisID.FIRST), "patch_vector.mod");
    assertEquals("file name is wrong",
      FileType.PATCH_VECTOR_MODEL.getFileName(manager, AxisID.SECOND),
      "patch_vector.mod");
    assertEquals("file name is wrong",
      FileType.PATCH_VECTOR_CCC_MODEL.getFileName(manager, AxisID.FIRST),
      "patch_vector_ccc.mod");
    assertEquals("file name is wrong",
      FileType.PATCH_VECTOR_CCC_MODEL.getFileName(manager, AxisID.SECOND),
      "patch_vector_ccc.mod");
    assertEquals("file name is wrong",
      FileType.SIRTSETUP_COMSCRIPT.getFileName(manager, AxisID.FIRST), "sirtsetupa.com");
    assertEquals("file name is wrong",
      FileType.SIRTSETUP_COMSCRIPT.getFileName(manager, AxisID.SECOND), "sirtsetupb.com");
    assertEquals("file name is wrong",
      FileType.COMBINED_VOLUME.getFileName(manager, AxisID.FIRST), "sum.rec");
    assertEquals("file name is wrong",
      FileType.COMBINED_VOLUME.getFileName(manager, AxisID.SECOND), "sum.rec");
    assertEquals("file name is wrong",
      FileType.NAD_TEST_INPUT.getFileName(manager, AxisID.FIRST), "test.input");
    assertEquals("file name is wrong",
      FileType.NAD_TEST_INPUT.getFileName(manager, AxisID.SECOND), "test.input");
    assertEquals("file name is wrong",
      FileType.TILT_COMSCRIPT.getFileName(manager, AxisID.FIRST), "tilta.com");
    assertEquals("file name is wrong",
      FileType.TILT_COMSCRIPT.getFileName(manager, AxisID.SECOND), "tiltb.com");
    assertEquals("file name is wrong",
      FileType.TILT_OUTPUT.getFileName(manager, AxisID.FIRST), "BBa.rec");
    assertEquals("file name is wrong",
      FileType.TILT_OUTPUT.getFileName(manager, AxisID.SECOND), "BBb.rec");
    assertEquals("file name is wrong",
      FileType.TILT_OUTPUT.getFileName(manager, AxisID.FIRST), "BBa.rec");
    assertEquals("file name is wrong",
      FileType.TILT_OUTPUT.getFileName(manager, AxisID.SECOND), "BBb.rec");
    assertEquals("file name is wrong",
      FileType.SIRT_SCALED_OUTPUT_TEMPLATE.getFileName(manager, AxisID.FIRST),
      "BBa.sint");
    assertEquals("file name is wrong",
      FileType.SIRT_SCALED_OUTPUT_TEMPLATE.getFileName(manager, AxisID.SECOND),
      "BBb.sint");
    assertEquals("file name is wrong",
      FileType.SIRT_OUTPUT_TEMPLATE.getFileName(manager, AxisID.FIRST), "BBa.srec");
    assertEquals("file name is wrong",
      FileType.SIRT_OUTPUT_TEMPLATE.getFileName(manager, AxisID.SECOND), "BBb.srec");
    assertEquals("file name is wrong",
      FileType.TRACK_COMSCRIPT.getFileName(manager, AxisID.FIRST), "tracka.com");
    assertEquals("file name is wrong",
      FileType.TRACK_COMSCRIPT.getFileName(manager, AxisID.SECOND), "trackb.com");
    assertEquals("file name is wrong",
      FileType.CROSS_CORRELATION_COMSCRIPT.getFileName(manager, AxisID.FIRST),
      "xcorra.com");
    assertEquals("file name is wrong",
      FileType.CROSS_CORRELATION_COMSCRIPT.getFileName(manager, AxisID.SECOND),
      "xcorrb.com");
    assertEquals("file name is wrong",
      FileType.PATCH_TRACKING_COMSCRIPT.getFileName(manager, AxisID.FIRST),
      "xcorr_pta.com");
    assertEquals("file name is wrong",
      FileType.PATCH_TRACKING_COMSCRIPT.getFileName(manager, AxisID.SECOND),
      "xcorr_ptb.com");
    assertEquals("file name is wrong",
      FileType.TILT_FOR_SIRT_COMSCRIPT.getFileName(manager, AxisID.FIRST),
      "tilta_for_sirt.com");
    assertEquals("file name is wrong",
      FileType.TILT_FOR_SIRT_COMSCRIPT.getFileName(manager, AxisID.SECOND),
      "tiltb_for_sirt.com");

    // test single axis
    metaData.setAxisType(AxisType.SINGLE_AXIS);
    assertEquals("file name is wrong",
      FileType.FIDUCIAL_3D_MODEL.getFileName(manager, AxisID.ONLY), "BB.3dmod");
    assertEquals("file name is wrong",
      FileType.ALIGNED_STACK.getFileName(manager, AxisID.ONLY), "BB.ali");
    assertEquals("file name is wrong",
      FileType.XCORR_BLEND_OUTPUT.getFileName(manager, AxisID.ONLY), "BB.bl");
    assertEquals("file name is wrong",
      FileType.DISTORTION_CORRECTED_STACK.getFileName(manager, AxisID.ONLY), "BB.dcst");
    assertEquals("file name is wrong",
      FileType.FIDUCIAL_MODEL.getFileName(manager, AxisID.ONLY), "BB.fid");
    assertEquals("file name is wrong",
      FileType.FLATTEN_TOOL_OUTPUT.getFileName(manager, AxisID.ONLY), "BB.flat");
    assertEquals("file name is wrong", FileType.JOIN.getFileName(manager, AxisID.ONLY),
      "BB.join");
    assertEquals("file name is wrong",
      FileType.ANISOTROPIC_DIFFUSION_OUTPUT.getFileName(manager, AxisID.ONLY), "BB.nad");
    assertEquals("file name is wrong",
      FileType.PREALIGNED_STACK.getFileName(manager, AxisID.ONLY), "BB.preali");
    assertEquals("file name is wrong",
      FileType.RAW_TILT_ANGLES.getFileName(manager, AxisID.ONLY), "BB.rawtlt");
    assertEquals("file name is wrong",
      FileType.TRIM_VOL_OUTPUT.getFileName(manager, AxisID.ONLY), "BB.rec");
    assertEquals("file name is wrong",
      FileType.JOIN_SAMPLE_AVERAGES.getFileName(manager, AxisID.ONLY), "BB.sampavg");
    assertEquals("file name is wrong",
      FileType.JOIN_SAMPLE.getFileName(manager, AxisID.ONLY), "BB.sample");
    assertEquals("file name is wrong", "BB.sqz",
      FileType.SQUEEZE_VOL_OUTPUT.getFileName(manager, AxisID.ONLY));
    assertEquals("file name is wrong",
      FileType.RAW_STACK.getFileName(manager, AxisID.ONLY), "BB.st");
    assertEquals("file name is wrong",
      FileType.NEWST_OR_BLEND_3D_FIND_OUTPUT.getFileName(manager, AxisID.ONLY),
      "BB_3dfind.ali");
    assertEquals("file name is wrong",
      FileType.FIND_BEADS_3D_OUTPUT_MODEL.getFileName(manager, AxisID.ONLY),
      "BB_3dfind.mod");
    assertEquals("file name is wrong",
      FileType.TILT_3D_FIND_OUTPUT.getFileName(manager, AxisID.ONLY), "BB_3dfind.rec");
    assertEquals("file name is wrong",
      FileType.SMOOTHING_ASSESSMENT_OUTPUT_MODEL.getFileName(manager, AxisID.ONLY),
      "BB_checkflat.mod");
    assertEquals("file name is wrong", "BB_ctfcorr.ali",
      FileType.CTF_CORRECTED_STACK.getFileName(manager, AxisID.ONLY));
    assertEquals("file name is wrong",
      FileType.ERASED_BEADS_STACK.getFileName(manager, AxisID.ONLY), "BB_erase.ali");
    assertEquals("file name is wrong",
      FileType.CCD_ERASER_BEADS_INPUT_MODEL.getFileName(manager, AxisID.ONLY),
      "BB_erase.fid");
    assertEquals("file name is wrong",
      FileType.MTF_FILTERED_STACK.getFileName(manager, AxisID.ONLY), "BB_filt.ali");
    assertEquals("file name is wrong",
      FileType.FIXED_XRAYS_STACK.getFileName(manager, AxisID.ONLY), "BB_fixed.st");
    assertEquals("file name is wrong",
      FileType.FLATTEN_WARP_INPUT_MODEL.getFileName(manager, AxisID.ONLY), "BB_flat.mod");
    assertEquals("file name is wrong",
      FileType.FLATTEN_OUTPUT.getFileName(manager, AxisID.ONLY), "BB_flat.rec");
    assertEquals("file name is wrong",
      FileType.FLATTEN_TOOL_COMSCRIPT.getFileName(manager, AxisID.ONLY),
      "BB_flatten.com");
    assertEquals("file name is wrong",
      FileType.MODELED_JOIN.getFileName(manager, AxisID.ONLY), "BB_modeled.join");
    assertEquals("file name is wrong",
      FileType.ORIGINAL_RAW_STACK.getFileName(manager, AxisID.ONLY), "BB_orig.st");
    assertEquals("file name is wrong",
      FileType.PATCH_TRACKING_BOUNDARY_MODEL.getFileName(manager, AxisID.ONLY),
      "BB_ptbound.mod");
    assertEquals("file name is wrong",
      FileType.TRANSFORMED_REFINING_MODEL.getFileName(manager, AxisID.ONLY),
      "BB_refine.alimod");
    assertEquals("file name is wrong",
      FileType.TRIAL_JOIN.getFileName(manager, AxisID.ONLY), "BB_trial.join");
    assertEquals("file name is wrong",
      FileType.CTF_CORRECTION_COMSCRIPT.getFileName(manager, AxisID.ONLY),
      "ctfcorrection.com");
    assertEquals("file name is wrong",
      FileType.FIND_BEADS_3D_COMSCRIPT.getFileName(manager, AxisID.ONLY),
      "findbeads3d.com");
    assertEquals("file name is wrong",
      FileType.FLATTEN_COMSCRIPT.getFileName(manager, AxisID.ONLY), "flatten.com");
    assertEquals("file name is wrong",
      FileType.MTF_FILTER_COMSCRIPT.getFileName(manager, AxisID.ONLY), "mtffilter.com");
    assertEquals("file name is wrong",
      FileType.PATCH_VECTOR_MODEL.getFileName(manager, AxisID.ONLY), "patch_vector.mod");
    assertEquals("file name is wrong",
      FileType.PATCH_VECTOR_CCC_MODEL.getFileName(manager, AxisID.ONLY),
      "patch_vector_ccc.mod");
    assertEquals("file name is wrong",
      FileType.SIRTSETUP_COMSCRIPT.getFileName(manager, AxisID.ONLY), "sirtsetup.com");
    assertEquals("file name is wrong",
      FileType.COMBINED_VOLUME.getFileName(manager, AxisID.ONLY), "sum.rec");
    assertEquals("file name is wrong",
      FileType.NAD_TEST_INPUT.getFileName(manager, AxisID.ONLY), "test.input");
    assertEquals("file name is wrong", "tilt.com",
      FileType.TILT_COMSCRIPT.getFileName(manager, AxisID.ONLY));
    assertEquals("file name is wrong",
      FileType.TILT_OUTPUT.getFileName(manager, AxisID.ONLY), "BB_full.rec");
    assertEquals("file name is wrong",
      FileType.SIRT_SCALED_OUTPUT_TEMPLATE.getFileName(manager, AxisID.ONLY),
      "BB_full.sint");
    assertEquals("file name is wrong",
      FileType.SIRT_OUTPUT_TEMPLATE.getFileName(manager, AxisID.ONLY), "BB_full.srec");
    assertEquals("file name is wrong",
      FileType.TRACK_COMSCRIPT.getFileName(manager, AxisID.ONLY), "track.com");
    assertEquals("file name is wrong",
      FileType.CROSS_CORRELATION_COMSCRIPT.getFileName(manager, AxisID.ONLY),
      "xcorr.com");
    assertEquals("file name is wrong",
      FileType.PATCH_TRACKING_COMSCRIPT.getFileName(manager, AxisID.ONLY),
      "xcorr_pt.com");
    assertEquals("file name is wrong",
      FileType.TILT_FOR_SIRT_COMSCRIPT.getFileName(manager, AxisID.ONLY),
      "tilt_for_sirt.com");
  }

  @SuppressWarnings("deprecation")
  public void testIsInSubdirectory() {
    FrontPageManager manager =
      (FrontPageManager) EtomoDirector.INSTANCE.getCurrentManagerForTest();
    FrontPageMetaData metaData = manager.getMetaData();
    metaData.setAxisType(AxisType.SINGLE_AXIS);
    assertFalse("In subdirectory", FileType.TILT_OUTPUT_OLD.isInSubdirectory());
    assertFalse("In subdirectory",
      FileType.CCD_ERASER_BEADS_INPUT_MODEL.isInSubdirectory());
    assertFalse("In subdirectory", FileType.SIRT_OUTPUT_TEMPLATE.isInSubdirectory());
    assertFalse("In subdirectory",
      FileType.ANISOTROPIC_DIFFUSION_OUTPUT_OLD.isInSubdirectory());
    assertTrue("Not in subdirectory", FileType.NAD_TEST_INPUT_OLD.isInSubdirectory());
    assertFalse("In subdirectory", FileType.RAW_STACK.isInSubdirectory());
    assertFalse("In subdirectory", FileType.ALIGNED_STACK.isInSubdirectory());
  }
}
