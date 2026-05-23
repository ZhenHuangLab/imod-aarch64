package etomo.storage;

import java.io.File;

import etomo.EtomoDirector;
import etomo.TestManager;
import etomo.process.BaseProcessManager;
import etomo.type.AxisType;
import etomo.type.ImageFilenameStyle;
import junit.framework.TestCase;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2008 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.1  2009/10/29 19:53:53  sueh
 * <p> bug# 1280 Unit tests for TomogramFileFilter.
 * <p> </p>
 */
public class TomogramFileFilterTest extends TestCase {
  private static final File testDir =
    new File(StorageTests.TEST_ROOT_DIR, "TomogramFileFilter");
  private static final TomogramFileFilter tomogramFileFilter = TomogramFileFilter
    .getInstance(new TestManager(AxisType.SINGLE_AXIS, ImageFilenameStyle.MRC));

  public TomogramFileFilterTest() {}

  public void testGetInstance() {
    TomogramFileFilter filter = null;
    try {
      filter =
        TomogramFileFilter.getInstance(EtomoDirector.INSTANCE.getCurrentManagerForTest());
    }
    catch (Throwable t) {
      fail("GetInstance should work");
    }
    assertNotNull("GetInstance should construct an instance", filter);
  }

  public void testDefaultExtensions() {
    assertTrue("Should accept .flip files", tomogramFileFilter
      .accept(new File(testDir, "testDefaultExtensions" + "_flip.mrc")));
    assertTrue("Should accept .rec files",
      tomogramFileFilter.accept(new File(testDir, "testDefaultExtensions" + "_rec.mrc")));
    assertTrue("Should accept .sqz files",
      tomogramFileFilter.accept(new File(testDir, "testDefaultExtensions" + "_sqz.mrc")));
    assertTrue("Should accept .join files", tomogramFileFilter
      .accept(new File(testDir, "testDefaultExtensions" + "_join.mrc")));
  }

  public void testDirectory() {
    File dir = new File(testDir, "testDirectory");
    if (!dir.exists()) {
      assertTrue(dir.mkdirs());
    }
    if (dir.exists()) {
      assertTrue("Should accept all directories", tomogramFileFilter.accept(dir));
    }
  }

  @SuppressWarnings("deprecation")
  public void testNewExtension() {
    File file = new File(testDir, "testNewExtension" + ".dummy");
    BaseProcessManager.touch(file.getAbsolutePath(),
      EtomoDirector.INSTANCE.getCurrentManagerForTest());
    tomogramFileFilter.addExtension(file);
    assertTrue("Should accept unknown extension after its been added",
      tomogramFileFilter.accept(file));
  }

  @SuppressWarnings("deprecation")
  public void testAllowAll() {
    File file = new File(testDir, "testAllowAll");
    BaseProcessManager.touch(file.getAbsolutePath(),
      EtomoDirector.INSTANCE.getCurrentManagerForTest());
    assertFalse("Should not accept without an extension",
      tomogramFileFilter.accept(file));
    tomogramFileFilter.addExtension(file);
    assertTrue("Should accept without an extension once its been added",
      tomogramFileFilter.accept(file));

    file = new File(testDir, "testAllowAll" + ".dummy");
    BaseProcessManager.touch(file.getAbsolutePath(),
      EtomoDirector.INSTANCE.getCurrentManagerForTest());
    assertTrue("Should accept all files once a file without an extension has been added",
      tomogramFileFilter.accept(file));
  }
}
