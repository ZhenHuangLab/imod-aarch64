package etomo.storage;

import etomo.TestManager;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.ImageFilenameStyle;
import junit.framework.TestCase;

public class MultifiltOutputFileFilterTest extends TestCase {
  // filters
  // ImageFilenameStyle.OLD;
  private static final MultifiltOutputFileFilter SINGLE_FILTER_OLD =
    MultifiltOutputFileFilter.getInstance(
      new TestManager(AxisType.SINGLE_AXIS, ImageFilenameStyle.OLD),
      ImageFilenameStyle.OLD, AxisID.ONLY, null);
  private static final TestManager DUAL_MANAGER_OLD =
    new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.OLD);
  private static final MultifiltOutputFileFilter DUAL_A_FILTER_OLD =
    MultifiltOutputFileFilter.getInstance(DUAL_MANAGER_OLD, ImageFilenameStyle.OLD,
      AxisID.FIRST, null);
  private static final MultifiltOutputFileFilter DUAL_B_FILTER_OLD =
    MultifiltOutputFileFilter.getInstance(DUAL_MANAGER_OLD, ImageFilenameStyle.OLD,
      AxisID.SECOND, null);
  // ImageFilenameStyle.MRC;
  private static final MultifiltOutputFileFilter SINGLE_FILTER_MRC =
    MultifiltOutputFileFilter.getInstance(
      new TestManager(AxisType.SINGLE_AXIS, ImageFilenameStyle.MRC),
      ImageFilenameStyle.MRC, AxisID.ONLY, null);
  private static final TestManager DUAL_MANAGER_MRC =
    new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.MRC);
  private static final MultifiltOutputFileFilter DUAL_A_FILTER_MRC =
    MultifiltOutputFileFilter.getInstance(DUAL_MANAGER_MRC, ImageFilenameStyle.MRC,
      AxisID.FIRST, null);
  private static final MultifiltOutputFileFilter DUAL_B_FILTER_MRC =
    MultifiltOutputFileFilter.getInstance(DUAL_MANAGER_MRC, ImageFilenameStyle.MRC,
      AxisID.SECOND, null);
  // ImageFilenameStyle.HDF;
  private static final MultifiltOutputFileFilter SINGLE_FILTER_HDF =
    MultifiltOutputFileFilter.getInstance(
      new TestManager(AxisType.SINGLE_AXIS, ImageFilenameStyle.HDF),
      ImageFilenameStyle.HDF, AxisID.ONLY, null);
  private static final TestManager DUAL_MANAGER_HDF =
    new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.HDF);
  private static final MultifiltOutputFileFilter DUAL_A_FILTER_HDF =
    MultifiltOutputFileFilter.getInstance(DUAL_MANAGER_HDF, ImageFilenameStyle.HDF,
      AxisID.FIRST, null);
  private static final MultifiltOutputFileFilter DUAL_B_FILTER_HDF =
    MultifiltOutputFileFilter.getInstance(DUAL_MANAGER_HDF, ImageFilenameStyle.HDF,
      AxisID.SECOND, null);
  // constants
  private static final String MRC_EXT = "mrc";

  public void testAccept() {
    // FileType.MUTLIFILT_FAKE_SIRT_ITERATIONS_OUTPUT_TEMPLATE: dateset_slfinnnn.mrc
    test(true, "_slfi1234", MRC_EXT);
    // FileType.MUTLIFILT_EXACT_OBJECT_SIZES_OUTPUT_TEMPLATE: dataset_efosnnnn.mrc
    test(true, "_efos5678", MRC_EXT);
    // FileType.MUTLIFILT_GAUSSIAN_OUTPUT_TEMPLATE: dataset_gfcx.xxx-fx.xxx.mrc
    test(true, "_gfc8.012-f3.456", MRC_EXT);
    // FileType.MUTLIFILT_HAMMING_LIKE_STARTS_OUTPUT_TEMPLATE: dataset_hlfs0.xxx.mrc
    test(true, "_hlfs0.789", MRC_EXT);
    // the standard extension by itself should not be accepted
    test(false, null, MRC_EXT);
    // these are not standardizable file names - they never have this format:
    test(false, null, "slfi1234");
    test(false, null, "efos5678");
    test(false, null, "gfc8.012-f3.456");
    test(false, null, "hlfs0.789");
  }

  @SuppressWarnings("deprecation")
  private void test(final boolean accept, final String middlePiece,
    final String extension) {
    String suffix = (middlePiece != null ? middlePiece : "") + "." + extension;
    // original functionality
    assertEquals("original functionality should still work", accept,
      SINGLE_FILTER_OLD.acceptForRegressionTest(null, TestManager.DATASET_NAME + suffix));
    assertEquals("original functionality should still work", accept, DUAL_A_FILTER_OLD
      .acceptForRegressionTest(null, TestManager.DATASET_NAME + "a" + suffix));
    assertEquals("original functionality should still work", accept, DUAL_B_FILTER_OLD
      .acceptForRegressionTest(null, TestManager.DATASET_NAME + "b" + suffix));
    // regression test - old style
    assertEquals("new functionality should accept names with old-style in force", accept,
      SINGLE_FILTER_OLD.accept(null, TestManager.DATASET_NAME + suffix));
    assertEquals("new functionality should accept names with old-style in force", accept,
      DUAL_A_FILTER_OLD.accept(null, TestManager.DATASET_NAME + "a" + suffix));
    assertEquals("new functionality should accept names with old-style in force", accept,
      DUAL_B_FILTER_OLD.accept(null, TestManager.DATASET_NAME + "b" + suffix));
    // mrc style - names do not change
    assertEquals("new functionality should accept the same names with mrc-style in force",
      accept, SINGLE_FILTER_MRC.accept(null, TestManager.DATASET_NAME + suffix));
    assertEquals("new functionality should accept the same names with mrc-style in force",
      accept, DUAL_A_FILTER_MRC.accept(null, TestManager.DATASET_NAME + "a" + suffix));
    assertEquals("new functionality should accept the same names with mrc-style in force",
      accept, DUAL_B_FILTER_MRC.accept(null, TestManager.DATASET_NAME + "b" + suffix));
    // hdf style - change the extension
    if (extension.equals(MRC_EXT)) {
      suffix = (middlePiece != null ? middlePiece : "") + "." + "hdf";
    }
    assertEquals(
      "new functionality should accept the hdf extension with hdf-style in force", accept,
      SINGLE_FILTER_HDF.accept(null, TestManager.DATASET_NAME + suffix));
    assertEquals(
      "new functionality should accept the hdf extension with hdf-style in force", accept,
      DUAL_A_FILTER_HDF.accept(null, TestManager.DATASET_NAME + "a" + suffix));
    assertEquals(
      "new functionality should accept the hdf extension with hdf-style in force", accept,
      DUAL_B_FILTER_HDF.accept(null, TestManager.DATASET_NAME + "b" + suffix));
  }
}
