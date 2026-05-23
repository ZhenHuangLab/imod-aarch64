package etomo;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2012 by the Regents of the University of Colorado</p>
*
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
* 
* @version $Id$
*/
public final class EtomoTests {
  public static Test suite() {
    TestSuite suite = new TestSuite("etomo tests");
    // $JUnit-BEGIN$
    suite.addTestSuite(ProcessSeriesTest.class);
    // $JUnit-END$
    return suite;
  }
}
