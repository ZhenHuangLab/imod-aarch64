package etomo.ui;

import java.io.File;

import etomo.JUnitTests;
import junit.framework.Test;
import junit.framework.TestSuite;

public final class UITests {
  private static final String TEST_DIR = "etomo/ui";
  static final File TEST_ROOT_DIR = new File(JUnitTests.TEST_ROOT_DIR, TEST_DIR);

  public static Test suite() {
    TestSuite suite = new TestSuite("Tests:  " + TEST_DIR);
    // $JUnit-BEGIN$
    suite.addTestSuite(SetupReconUIHarnessTest.class);
    // $JUnit-END$
    return suite;
  }
}
