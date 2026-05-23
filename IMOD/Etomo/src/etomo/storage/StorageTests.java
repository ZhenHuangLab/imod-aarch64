package etomo.storage;

import java.io.File;

import etomo.JUnitTests;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2006 - 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
* 
* <p> $Log$
* <p> Revision 1.3  2006/11/15 20:41:17  sueh
* <p> bug# 872 Changed getParamFileStorableArray to getStorables in the managers.
* <p>
* <p> Revision 1.2  2006/10/16 22:48:39  sueh
* <p> bug# 919  Added JoinInfoFileTest.
* <p>
* <p> Revision 1.1  2006/10/10 05:19:38  sueh
* <p> bug# 931 Test suite for the storage package.
* <p> </p>
*/

public class StorageTests {
  private static final String TEST_DIR = "etomo/storage";
  static final File TEST_ROOT_DIR = new File(JUnitTests.TEST_ROOT_DIR, TEST_DIR);

  public static Test suite() {
    TestSuite suite = new TestSuite("Tests:  " + TEST_DIR);
    // $JUnit-BEGIN$
    suite.addTestSuite(TomogramFileFilterTest.class);
    suite.addTestSuite(ExtensionFileFilterTest.class);
    suite.addTestSuite(SirtOutputFileFilterTest.class);
    suite.addTestSuite(MultifiltOutputFileFilterTest.class);
    suite.addTestSuite(IntermediateFileFilterTest.class);
    suite.addTestSuite(SimpleDefocusFileTest.class);
    suite.addTestSuite(JoinInfoFileTest.class);
    suite.addTestSuite(LogFileTest.class);
    suite.addTestSuite(ParameterStoreTest.class);
    suite.addTestSuite(DirectiveMapTest.class);
    suite.addTestSuite(DirectiveNameTest.class);
    suite.addTestSuite(ComFileTest.class);
    suite.addTestSuite(MatlabParamTest.class);
    suite.addTestSuite(DirectiveDefTest.class);
    suite.addTestSuite(DirectiveDescrFileTest.class);
    suite.addTestSuite(ProcessSignalTest.class);
    suite.addTestSuite(NameValuePairListTest.class);
    // $JUnit-END$
    return suite;
  }
}
