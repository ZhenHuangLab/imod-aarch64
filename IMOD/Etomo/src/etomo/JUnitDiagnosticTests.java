package etomo;

import java.io.File;
import java.util.Enumeration;

import etomo.util.UtilDiagnosticTests;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * <p>Description: Configurable tests for diagnosing unit test failures without modifying
 * the original unit test.  Not kept up to date.</p>
 * 
 * <p>Copyright: Copyright 2020 by the Regents of the University of Colorado</p>
 *
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 **/
public class JUnitDiagnosticTests {
  public static final File TEST_ROOT_DIR =
    new File(System.getProperty("user.dir"), "JUnitDiagnosticTests");
  public static final String[] ETOMO_ARGUMENTS =
    { Arguments.TEST_TAG, Arguments.HEADLESS_TAG, Arguments.SELFTEST_TAG };

  public static Test suite() {
    System.setProperty("java.awt.headless", "true");
    EtomoDirector.main(ETOMO_ARGUMENTS);
    TestSuite suite = new TestSuite("Test for etomo");

    TestSuite testSuite;
    Enumeration tests;
    Object test;

    // $JUnit-BEGIN$
    // $JUnit-END$

    testSuite = (TestSuite) UtilDiagnosticTests.suite();
    tests = testSuite.tests();
    while (tests.hasMoreElements()) {
      test = tests.nextElement();
      if (test instanceof Test) {
        suite.addTest((Test) test);
      }
    }

    return suite;
  }
}
