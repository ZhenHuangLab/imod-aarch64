package etomo.storage;

import java.io.File;

import etomo.BaseManager;
import etomo.TestManager;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.Extension;
import etomo.type.FileType;
import etomo.type.ImageFilenameStyle;
import junit.framework.TestCase;

/**
 * <p>Description: Unit tests for ExtensionFileFilter.  Currently not testing accepting
 * directories because the files used are not real</p>
 * 
 * <p>Copyright: Copyright 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class ExtensionFileFilterTest extends TestCase {
  private static final String SUFFIX = "_test_suffix.foo";
  private final FileType[] FILE_TYPES = new FileType[] { FileType.TILT_OUTPUT };

  public void testDoublePattern() {
    ImageFilenameStyle imageFilenameStyle = ImageFilenameStyle.OLD;
    AxisType axisType = AxisType.SINGLE_AXIS;
    TestFilter filter =
      TestFilter.getInstance(new TestManager(axisType, imageFilenameStyle),
        TestManager.DATASET_NAME, null, null, null, null, FILE_TYPES, false);
    assertTrue("single axis is _full.rec",
      filter.accept(new File(TestManager.DATASET_NAME + "_full.rec")));
    assertFalse(
      "fails to find files types not included if there is something to distinguish is",
      filter.accept(new File(TestManager.DATASET_NAME + "_flat.rec")));
    filter = TestFilter.getInstance(new TestManager(axisType, imageFilenameStyle), null,
      null, null, null, null, FILE_TYPES, false);
    assertTrue("can find single with dataset name pattern",
      filter.accept(new File(TestManager.DATASET_NAME + "_full.rec")));
    assertFalse("fails to find a different file type for if there is some required text",
      filter.accept(new File(TestManager.DATASET_NAME + "_flat.rec")));
    axisType = AxisType.DUAL_AXIS;
    filter = TestFilter.getInstance(new TestManager(axisType, imageFilenameStyle),
      TestManager.DATASET_NAME, null, null, null, null, FILE_TYPES, false);
    assertTrue("without axis information - should accept wrong axis type",
      filter.accept(new File(TestManager.DATASET_NAME + "_full.rec")));
    assertTrue("dual axis is .rec",
      filter.accept(new File(TestManager.DATASET_NAME + "a.rec")));
    filter = TestFilter.getInstance(new TestManager(axisType, imageFilenameStyle), null,
      null, null, null, null, FILE_TYPES, false);
    assertTrue("can find dual with dataset name pattern",
      filter.accept(new File(TestManager.DATASET_NAME + "a.rec")));
    assertFalse(
      "because the dual pattern requires a/b it won't accept this - even though no axis"
        + "type is included",
      filter.accept(new File(TestManager.DATASET_NAME + "a_flat.rec")));
    imageFilenameStyle = ImageFilenameStyle.MRC;
    axisType = AxisType.SINGLE_AXIS;
    assertTrue("single axis is _full_rec.mrc",
      TestFilter
        .getInstance(new TestManager(axisType, imageFilenameStyle),
          TestManager.DATASET_NAME, null, null, null, null, FILE_TYPES, false)
        .accept(new File(TestManager.DATASET_NAME + "_full_rec.mrc")));
    assertTrue("single axis is _full_rec.mrc - can find with dataset name pattern",
      TestFilter
        .getInstance(new TestManager(axisType, imageFilenameStyle), null, null, null,
          null, null, FILE_TYPES, false)
        .accept(new File(TestManager.DATASET_NAME + "_full_rec.mrc")));
    axisType = AxisType.DUAL_AXIS;
    assertTrue(" dual axis is _rec.mrc",
      TestFilter
        .getInstance(new TestManager(axisType, imageFilenameStyle),
          TestManager.DATASET_NAME, null, null, null, null, FILE_TYPES, false)
        .accept(new File(TestManager.DATASET_NAME + "b_rec.mrc")));
    assertTrue(" dual axis is _rec.mrc - dataset name pattern",
      TestFilter
        .getInstance(new TestManager(axisType, imageFilenameStyle), null, null, null,
          null, null, FILE_TYPES, false)
        .accept(new File(TestManager.DATASET_NAME + "a_rec.mrc")));
  }

  public void testEmpty() {
    AxisType axisType = AxisType.SINGLE_AXIS;
    assertFalse("empty filter can't recognize anything",
      TestFilter
        .getInstance(new TestManager(axisType, ImageFilenameStyle.OLD),
          TestManager.DATASET_NAME, axisType, AxisID.ONLY, null, null, null, false)
        .accept(new File(TestManager.DATASET_NAME + ".rec")));
    axisType = AxisType.DUAL_AXIS;
    assertFalse("empty filter can't recognize anything",
      TestFilter
        .getInstance(new TestManager(axisType, ImageFilenameStyle.MRC),
          TestManager.DATASET_NAME, axisType, AxisID.SECOND, new String[] {},
          new Extension[] {}, new FileType[] {}, false)
        .accept(new File(TestManager.DATASET_NAME + "b.mrc")));
    assertFalse("empty filter can't recognize anything",
      TestFilter
        .getInstance(new TestManager(axisType, ImageFilenameStyle.HDF),
          TestManager.DATASET_NAME, axisType, AxisID.FIRST, new String[] { null },
          new Extension[] { null }, new FileType[] { null }, false)
        .accept(new File(TestManager.DATASET_NAME + "a.hdf")));
  }

  public void testAllFileStyles() {
    TestFilter filter =
      TestFilter.getInstance(new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.OLD),
        null, null, null, null, null, FILE_TYPES, true);
    // FIXME 2206
    assertTrue("works on the mrc style",
      filter.accept(new File(TestManager.DATASET_NAME + "a_rec.mrc")));
    assertTrue("works on the old style",
      filter.accept(new File(TestManager.DATASET_NAME + "a.rec")));
    // FIXME 2206
    assertTrue("works on the hdf style",
      filter.accept(new File(TestManager.DATASET_NAME + "a_rec.hdf")));
    filter =
      TestFilter.getInstance(new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.MRC),
        null, null, null, null, null, FILE_TYPES, true);
    // FIXME 2206
    assertTrue("works on the old style",
      filter.accept(new File(TestManager.DATASET_NAME + "a.rec")));
    assertTrue("works on the mrc style",
      filter.accept(new File(TestManager.DATASET_NAME + "a_rec.mrc")));
    // FIXME 2206
    assertTrue("works on the hdf style",
      filter.accept(new File(TestManager.DATASET_NAME + "a_rec.hdf")));
    filter =
      TestFilter.getInstance(new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.HDF),
        null, null, null, null, null, FILE_TYPES, true);
    // FIXME 2206
    assertTrue("works on the old style",
      filter.accept(new File(TestManager.DATASET_NAME + "a.rec")));
    assertTrue("works on the mrc style",
      filter.accept(new File(TestManager.DATASET_NAME + "a_rec.mrc")));
    assertTrue("works on the hdf style",
      filter.accept(new File(TestManager.DATASET_NAME + "a_rec.hdf")));
  }

  private static final class TestFilter extends ExtensionFileFilter {

    private TestFilter(final BaseManager manager, final boolean useAllFileStyles,
      final boolean debug) {
      super(manager, false, useAllFileStyles, debug);
    }

    private static TestFilter getInstance(final BaseManager manager,
      final String datasetName, final AxisType axisType, final AxisID axisID,
      final String[] suffixes, final Extension[] extensions, final FileType[] fileTypes,
      final boolean useAllFileStyles) {
      TestFilter instance = new TestFilter(manager, useAllFileStyles, false);
      instance.setup(datasetName, axisType, axisID, suffixes, extensions, fileTypes);
      return instance;
    }

    private static TestFilter getDebugInstance(final BaseManager manager,
      final String datasetName, final AxisType axisType, final AxisID axisID,
      final String[] suffixes, final Extension[] extensions, final FileType[] fileTypes,
      final boolean useAllFileStyles) {
      TestFilter instance = new TestFilter(manager, useAllFileStyles, true);
      instance.setup(datasetName, axisType, axisID, suffixes, extensions, fileTypes);
      return instance;
    }

    public void setup(final String datasetName, final AxisType axisType,
      final AxisID axisID, final String[] suffixes, final Extension[] extensions,
      final FileType[] fileTypes) {
      super.setup(datasetName, axisType, axisID, suffixes, extensions, fileTypes);
    }

    public String getDescription() {
      return "test";
    }
  }
}
