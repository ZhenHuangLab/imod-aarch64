package etomo.util;

import java.io.File;

import etomo.JUnitTests;
import junit.framework.Test;
import junit.framework.TestSuite;

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
public class UtilDiagnosticTests {
  private static final String TEST_DIR = "etomo/type";
  static final File TEST_ROOT_DIR = new File(JUnitTests.TEST_ROOT_DIR, TEST_DIR);

  public static Test suite() {
    TestSuite suite = new TestSuite("Tests:  " + TEST_DIR);
    // $JUnit-BEGIN$
    suite.addTestSuite(UtilitiesTest.DiagnosticTest.class);
    // $JUnit-END$
    return suite;
  }
}
