package etomo.storage;

import java.io.File;
import java.io.IOException;

import etomo.logic.NumericComparisonStrategy;
import etomo.process.SystemProcessException;
import etomo.storage.autodoc.AutodocFactory;
import etomo.util.TestUtilites;
import junit.framework.TestCase;

/**
 * <p>Description: Test case for NameValuePairList.</p>
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class NameValuePairListTest extends TestCase {
  // directories
  private static final String TEST_DIR_NAME = "NameValuePairList";
  private static final File TEST_DIR =
    new File(StorageTests.TEST_ROOT_DIR, TEST_DIR_NAME);
  // files
  private static final String CLEAN_UP = "clean-up.adoc";
  private static final String CPU = "cpu.adoc";
  private static final String PRM = "binnedBPV.prm";
  private static final String NVPL = "nvpl.adoc";
  // names
  private static final String VERSION_NAME = "Version";
  private static final String VOLCOMBINE = "max.volcombine";
  // values
  private static final String VERSION_VALUE_CLEAN_UP = "2.1";
  private static final String VERSION_VALUE_CPU = "1.0";

  public void setUp() throws Exception {
    if (!TEST_DIR.exists()) {
      TEST_DIR.mkdirs();
    }
    TestUtilites.INSTANCE.copyTestFile(StorageTests.TEST_ROOT_DIR.getAbsolutePath(),
      TEST_DIR_NAME, CLEAN_UP);
    TestUtilites.INSTANCE.copyTestFile(StorageTests.TEST_ROOT_DIR.getAbsolutePath(),
      TEST_DIR_NAME, CPU);
    TestUtilites.INSTANCE.copyTestFile(StorageTests.TEST_ROOT_DIR.getAbsolutePath(),
      TEST_DIR_NAME, PRM);
    TestUtilites.INSTANCE.copyTestFile(StorageTests.TEST_ROOT_DIR.getAbsolutePath(),
      TEST_DIR_NAME, NVPL);
  }

  public void testGlobalOnly()
    throws SystemProcessException, IOException, LogFile.LockException {
    NameValuePairList cleanUp = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, CLEAN_UP)));
    //
    assertEquals("only one global pair", 1, cleanUp.size());
    NameValuePairList.Pair pair = cleanUp.get(0);
    assertNotNull("can retrieve pair", pair);
    assertEquals("the one global pair is the version statement", VERSION_NAME,
      pair.getName());
    assertEquals("the one global pair is the version statement", VERSION_VALUE_CLEAN_UP,
      pair.getValue());
  }

  public void testPairsOnly()
    throws SystemProcessException, IOException, LogFile.LockException {
    NameValuePairList cpu = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, CPU)));
    //
    assertEquals("empty lines and comments are not list entries", 7, cpu.size());
  }

  public void testAllowNullValue()
    throws SystemProcessException, IOException, LogFile.LockException {
    NameValuePairList nvpl = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, NVPL)));
    //
    NameValuePairList.Pair pair = nvpl.get("this.pair.has.no.value");
    assertNotNull("pairs with no value are allowed", pair);
    assertNull("value is set to null", pair.getValue());
  }

  public void testCopyConstructor()
    throws SystemProcessException, IOException, LogFile.LockException {
    NameValuePairList cpu = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, CPU)));
    //
    NameValuePairList cpuCopy = new NameValuePairList(cpu);
    assertEquals("same size as cpu", 7, cpuCopy.size());
  }

  public void testCopyIndependent()
    throws SystemProcessException, IOException, LogFile.LockException {
    NameValuePairList cpu = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, CPU)));
    NameValuePairList prm = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, PRM)));
    //
    NameValuePairList cpuCopy = new NameValuePairList(cpu);
    cpu.merge(prm);
    assertEquals("size comes from copying original", 7, cpuCopy.size());
  }

  public void testCopyOriginalIndependent()
    throws SystemProcessException, IOException, LogFile.LockException {
    NameValuePairList cpu = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, CPU)));
    NameValuePairList prm = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, PRM)));
    //
    NameValuePairList cpuCopy = new NameValuePairList(cpu);
    cpuCopy.merge(prm);
    assertEquals("size unchanged", 7, cpu.size());
  }

  public void testMerge()
    throws SystemProcessException, IOException, LogFile.LockException {
    NameValuePairList cleanUp = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, CLEAN_UP)));
    NameValuePairList cpu = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, CPU)));
    NameValuePairList prm = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, PRM)));
    //
    cleanUp.merge(cpu);
    NameValuePairList.Pair pair = cleanUp.get(VERSION_NAME);
    assertNotNull("list retains its pairs", pair);
    assertEquals("list pairs retains their values", VERSION_VALUE_CLEAN_UP,
      pair.getValue());
    assertEquals("list gets pairs from the merge list", 7, cleanUp.size());
    // merge list is unchanged
    pair = cpu.get(VERSION_NAME);
    assertNotNull("merge list retains its pairs", pair);
    assertEquals("merge list pairs retains their values", VERSION_VALUE_CPU,
      pair.getValue());
    assertEquals("merge list unchanged", 7, cpu.size());
    // Further changes to the merge list do not affect the list that was merged into.
    cpu.merge(prm);
    assertEquals("list unchanged by changes to a previously merged list", 7,
      cleanUp.size());
  }

  public void testSubtract()
    throws SystemProcessException, IOException, LogFile.LockException {
    NameValuePairList cleanUp = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, CLEAN_UP)));
    NameValuePairList cpu = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, CPU)));
    NameValuePairList nvpl = new NameValuePairList(
      AutodocFactory.getAutodocInstance(null, new File(TEST_DIR, NVPL)));
    // Removes global name/value pairs
    cpu.subtract(cleanUp, null);
    // both name and value must match for subtraction
    assertEquals("cpu unchanged - Version was not removed because value did not match", 7,
      cpu.size());
    assertNotNull("volcombine pair exists", cpu.get(VOLCOMBINE));
    cpu.subtract(nvpl, NumericComparisonStrategy.INSTANCE);
    // 3 pairs removed because the name and numeric values matched. 1 non-numeric pair
    // removed
    assertNull("1 == 1.0", cpu.get("Version"));
    assertNull("non numeric comparison", cpu.get("units.speed"));
    assertNull("12. == 12", cpu.get("max.tilt"));
    assertNull("8 == 8", cpu.get(VOLCOMBINE));
  }
}
