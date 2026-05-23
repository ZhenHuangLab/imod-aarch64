package etomo.storage;

import etomo.TestManager;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.ImageFilenameStyle;
import junit.framework.TestCase;

/**
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright 2011 - 2022 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class SirtOutputFileFilterTest extends TestCase {
  // filters
  // ImageFilenameStyle.OLD;
  private static final SirtOutputFileFilter SINGLE_FILTER_OLD = SirtOutputFileFilter
    .getInstance(new TestManager(AxisType.SINGLE_AXIS, ImageFilenameStyle.OLD),
      ImageFilenameStyle.OLD, AxisID.ONLY, true, true, true);
  private static final TestManager DUAL_MANAGER_OLD =
    new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.OLD);
  private static final SirtOutputFileFilter DUAL_A_FILTER_OLD =
    SirtOutputFileFilter.getInstance(DUAL_MANAGER_OLD, ImageFilenameStyle.OLD,
      AxisID.FIRST, true, true, true);
  private static final SirtOutputFileFilter DUAL_B_FILTER_OLD =
    SirtOutputFileFilter.getInstance(DUAL_MANAGER_OLD, ImageFilenameStyle.OLD,
      AxisID.SECOND, true, true, true);
  // ImageFilenameStyle.MRC;
  private static final SirtOutputFileFilter SINGLE_FILTER_MRC = SirtOutputFileFilter
    .getInstance(new TestManager(AxisType.SINGLE_AXIS, ImageFilenameStyle.MRC),
      ImageFilenameStyle.MRC, AxisID.ONLY, true, true, true);
  private static final TestManager DUAL_MANAGER_MRC =
    new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.MRC);
  private static final SirtOutputFileFilter DUAL_A_FILTER_MRC =
    SirtOutputFileFilter.getInstance(DUAL_MANAGER_MRC, ImageFilenameStyle.MRC,
      AxisID.FIRST, true, true, true);
  private static final SirtOutputFileFilter DUAL_B_FILTER_MRC =
    SirtOutputFileFilter.getInstance(DUAL_MANAGER_MRC, ImageFilenameStyle.MRC,
      AxisID.SECOND, true, true, true);
  // ImageFilenameStyle.HDF;
  private static final SirtOutputFileFilter SINGLE_FILTER_HDF = SirtOutputFileFilter
    .getInstance(new TestManager(AxisType.SINGLE_AXIS, ImageFilenameStyle.HDF),
      ImageFilenameStyle.HDF, AxisID.ONLY, true, true, true);
  private static final TestManager DUAL_MANAGER_HDF =
    new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.HDF);
  private static final SirtOutputFileFilter DUAL_A_FILTER_HDF =
    SirtOutputFileFilter.getInstance(DUAL_MANAGER_HDF, ImageFilenameStyle.HDF,
      AxisID.FIRST, true, true, true);
  private static final SirtOutputFileFilter DUAL_B_FILTER_HDF =
    SirtOutputFileFilter.getInstance(DUAL_MANAGER_HDF, ImageFilenameStyle.HDF,
      AxisID.SECOND, true, true, true);

  public void testAccept() {
    // FileType.SIRT_SCALED_OUTPUT_TEMPLATE: dataset.sintnn
    test(true, "_full", null, "sint67");
    // FileType.SIRT_OUTPUT_TEMPLATE: dateset.srecnn
    test(true, "_full", null, "srec12");
    // Bug# 2402
    // FileType.SIRT_SUBAREA_OUTPUT_TEMPLATE: dataset_sub.srecnn
    test(true, null, "_sub", "srec34");
    // FileType.SIRT_SUBAREA_SCALED_OUTPUT_TEMPLATE: dataset_sub.sintnn
    test(true, null, "_sub", "sint56");
    // the standard extension by itself should not be accepted
    test(false, null, null, "mrc");
  }

  @SuppressWarnings("deprecation")
  private void test(final boolean accept, final String singleMiddlePiece,
    final String middlePiece, final String extension) {
    String singleSuffix = (singleMiddlePiece != null ? singleMiddlePiece : "")
      + (middlePiece != null ? middlePiece : "") + "." + extension;
    String dualSuffix = (middlePiece != null ? middlePiece : "") + "." + extension;
    // original functionality
    SINGLE_FILTER_OLD.setDebug(true);
    assertEquals("original functionality should still work", accept, SINGLE_FILTER_OLD
      .acceptForRegressionTest(null, TestManager.DATASET_NAME + singleSuffix));
    SINGLE_FILTER_OLD.setDebug(false);
    assertEquals("original functionality should still work", accept, DUAL_A_FILTER_OLD
      .acceptForRegressionTest(null, TestManager.DATASET_NAME + "a" + dualSuffix));
    assertEquals("original functionality should still work", accept, DUAL_B_FILTER_OLD
      .acceptForRegressionTest(null, TestManager.DATASET_NAME + "b" + dualSuffix));
    // regression test - old style
    assertEquals("new functionality should accept names with old-style in force", accept,
      SINGLE_FILTER_OLD.accept(null, TestManager.DATASET_NAME + singleSuffix));
    assertEquals("new functionality should accept names with old-style in force", accept,
      DUAL_A_FILTER_OLD.accept(null, TestManager.DATASET_NAME + "a" + dualSuffix));
    assertEquals("new functionality should accept names with old-style in force", accept,
      DUAL_B_FILTER_OLD.accept(null, TestManager.DATASET_NAME + "b" + dualSuffix));
    // mrc style - standardize
    singleSuffix = (singleMiddlePiece != null ? singleMiddlePiece : "")
      + (middlePiece != null ? middlePiece : "") + "_" + extension + ".mrc";
    dualSuffix = (middlePiece != null ? middlePiece : "") + "_" + extension + ".mrc";
    assertEquals("new functionality should accept the same names with mrc-style in force",
      accept, SINGLE_FILTER_MRC.accept(null, TestManager.DATASET_NAME + singleSuffix));
    assertEquals("new functionality should accept the same names with mrc-style in force",
      accept,
      DUAL_A_FILTER_MRC.accept(null, TestManager.DATASET_NAME + "a" + dualSuffix));
    assertEquals("new functionality should accept the same names with mrc-style in force",
      accept,
      DUAL_B_FILTER_MRC.accept(null, TestManager.DATASET_NAME + "b" + dualSuffix));
    // hdf style - standardize
    singleSuffix = (singleMiddlePiece != null ? singleMiddlePiece : "")
      + (middlePiece != null ? middlePiece : "") + "_" + extension + ".hdf";
    dualSuffix = (middlePiece != null ? middlePiece : "") + "_" + extension + ".hdf";
    assertEquals(
      "new functionality should accept the hdf extension with hdf-style in force", accept,
      SINGLE_FILTER_HDF.accept(null, TestManager.DATASET_NAME + singleSuffix));
    assertEquals(
      "new functionality should accept the hdf extension with hdf-style in force", accept,
      DUAL_A_FILTER_HDF.accept(null, TestManager.DATASET_NAME + "a" + dualSuffix));
    assertEquals(
      "new functionality should accept the hdf extension with hdf-style in force", accept,
      DUAL_B_FILTER_HDF.accept(null, TestManager.DATASET_NAME + "b" + dualSuffix));
  }
}
