package etomo.comscript;

import java.io.File;

import etomo.JUnitTests;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright 2004 - 2012 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 3.5  2005/11/10 18:02:42  sueh
 * <p> bug# 758 Placed the root test directory in a File object in JUnitTests.  It is
 * <p> instanciated once so there won't be a problem if the working directory is
 * <p> changed.  Added a root test directory File object to each of the suites,
 * <p> which is based on the JUnitTests root test directory.
 * <p>
 * <p> Revision 3.4  2005/07/29 00:44:10  sueh
 * <p> bug# 709 Removed SetupCombineTest because is has no tests.
 * <p>
 * <p> Revision 3.3  2004/08/31 16:46:25  sueh
 * <p> bug# 508 removing JUnit tests that require an X server
 * <p>
 * <p> Revision 3.2  2004/08/20 21:38:42  sueh
 * <p> bug# 508 Added CombineComscriptStateTest and log.
 * <p> </p>
 */
public class ComScriptTests {
  private static final String TEST_DIR = "etomo/comscript";
  static final File TEST_ROOT_DIR = new File(JUnitTests.TEST_ROOT_DIR, TEST_DIR);

  public static Test suite() {
    TestSuite suite = new TestSuite("Tests:  " + TEST_DIR);
    suite.addTestSuite(StringListTest.class);
    suite.addTestSuite(FortranInputStringTest.class);

    // suite.addTestSuite(CombineComscriptStateTest.class);
    // suite.addTest(new NewstParamTest("testParseComScriptCommand"));
    return suite;
  }
}
