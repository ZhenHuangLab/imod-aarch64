package etomo.util;

import java.io.File;

import etomo.EtomoDirector;
import etomo.process.BaseProcessManager;
import junit.framework.TestCase;

/**
 * <p>Description: Unit test for ValidDirectory.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class ValidDirectoryTest extends TestCase {
  static final File TEST_DIR = new File(UtilTests.TEST_ROOT_DIR, "ValidDirectory");
  static final File EXISTING_FILE = new File(TEST_DIR, "existingFile");
  static final File NONEXISTANT_CHILD =
    new File(TEST_DIR, "nonexistantChild-aeskefj8gtigkjfoi");
  static final File NONEXISTANT_GRANDCHILD =
    new File(NONEXISTANT_CHILD, "nonexistantGrandchild-fadhk54389aghuksyga89ug45");

  static final File NONEXISTANT =
    new File(new File("/", "nonexistant-lf2305f897"), "nonexistant-se5b4sebsr");

  static final File CURRENT_DIR = new File(".");
  private static final boolean DEBUG = EtomoDirector.INSTANCE.getArguments().isDebug();

  private final ValidDirectory dir = new ValidDirectory(null);
  private final ValidDirectory sdir = new ValidDirectory(null);

  public void setUp() {
    if (!TEST_DIR.exists()) {
      TEST_DIR.mkdirs();
    }
    if (!EXISTING_FILE.exists()) {
      BaseProcessManager.touch(EXISTING_FILE.getAbsolutePath(), null);
    }
    if (DEBUG) {
      System.err.println("TEST_DIR:" + TEST_DIR.getAbsolutePath());
      System.err.println("EXISTING_FILE:" + EXISTING_FILE.getAbsolutePath());
      System.err.println("NONEXISTANT_CHILD:" + NONEXISTANT_CHILD.getAbsolutePath());
      System.err
        .println("NONEXISTANT_GRANDCHILD:" + NONEXISTANT_GRANDCHILD.getAbsolutePath());
      System.err.println("NONEXISTANT:" + NONEXISTANT.getAbsolutePath());
      System.err.println("CURRENT_DIR:" + CURRENT_DIR.getAbsolutePath());
    }
  }

  public void testConstructed() {
    assertNotSet("first constructed - null");
  }

  public void testDirectory() {
    assertTrue("TEST_DIR exists", TEST_DIR.exists());
    assertTrue("TEST_DIR is a directory", TEST_DIR.isDirectory());
    assertTrue("TEST_DIR is readable", TEST_DIR.canRead());
    //SetReadable isn't working on Windows 7
    if (!Utilities.isWindowsOS()) {
      TEST_DIR.setReadable(false);
      assertFalse("TEST_DIR is not readable", TEST_DIR.canRead());
      set(TEST_DIR);
      assertNotSet(
        "set to unreadable directory - unchanged: only uses parent when it receives a "
          + "file");
      TEST_DIR.setReadable(true);
    }
    set(TEST_DIR);
    assertSet("set to readable directory - equal to directory", TEST_DIR);
  }

  public void testFile() {
    //SetReadable isn't working on Windows 7
    if (!Utilities.isWindowsOS()) {
      EXISTING_FILE.setReadable(false);
      set(EXISTING_FILE);
      assertSet("set to readable directory from unreadable file - uses parent", TEST_DIR);
      EXISTING_FILE.setReadable(true);
    }
    set(EXISTING_FILE);
    assertSet("set to readable directory from readable file - uses parent", TEST_DIR);
  }

  public void testReset() {
    set(TEST_DIR);
    dir.set((File) null);
    sdir.set((String) null);
    assertNotSet("reset with null - null");
  }

  public void testNonexistantChild() {
    set(NONEXISTANT_CHILD);
    assertSet("set to readable directory from an nonexistant file - uses parent",
      TEST_DIR);
  }

  public void testNonexistantGrandchild() {
    set(NONEXISTANT_GRANDCHILD);
    assertNotSet(
      "set to a file inside a nonexistant directory inside an existing, readable "
        + "directory - unchanged:  will only look at the parent, not the grandparent");
  }

  public void testNonexistant() {
    set(NONEXISTANT);
    assertNotSet("set to nonexistant file without an existing parent - unchanged");
  }

  public void testNoChange() {
    set(TEST_DIR);
    set(NONEXISTANT);
    assertSet("invalid directory does not cause a reset", TEST_DIR);
  }

  public void testEmpty() {
    sdir.set("");
    String testDescr = "set to file constructed empty string - current directory";
    assertFalse(testDescr, sdir.isNull());
    assertEquals(testDescr, CURRENT_DIR.getAbsolutePath(), sdir.get().getAbsolutePath());
  }

  public void testSpace() {
    sdir.set(" ");
    String testDescr = "set to file constructed with space - current directory";
    assertFalse(testDescr, sdir.isNull());
    assertEquals(testDescr, CURRENT_DIR.getAbsolutePath(), sdir.get().getAbsolutePath());
  }

  private void set(final File file) {
    dir.set(file);
    sdir.set(file.getAbsolutePath());
  }

  private void assertNotSet(final String testDescr) {
    File file = dir.get();
    if (file != null && DEBUG) {
      System.err.println("dir:" + file.getAbsolutePath());
    }
    assertTrue("assertTrue:" + testDescr + ",dir:" + dir.toString(), dir.isNull());
    assertNull("assertNull:" + testDescr + ",dir:" + dir.toString(), file);
    file = sdir.get();
    if (file != null && DEBUG) {
      System.err.println("dir:" + file.getAbsolutePath());
    }
    assertTrue("assertTrue:" + testDescr + ",sdir:" + sdir.toString(), sdir.isNull());
    assertNull("assertNull:" + testDescr + ",sdir:" + sdir.toString(), file);
  }

  private void assertSet(final String testDescr, final File file) {
    assertFalse(testDescr, dir.isNull());
    assertFalse(testDescr, sdir.isNull());
    assertEquals(testDescr, file.getAbsolutePath(), dir.get().getAbsolutePath());
    assertEquals(testDescr, file.getAbsolutePath(), sdir.get().getAbsolutePath());
  }
}
