package etomo.storage;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import etomo.type.AxisID;
import etomo.util.TestUtilites;
import junit.framework.TestCase;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2014</p>
*
* <p>Organization:
* Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
* University of Colorado</p>
* 
* @author $Author$
* 
* @version $Revision$
* 
* <p> $Log$ </p>
*/
public class MatlabParamTest extends TestCase {
  public static final String rcsid = "$Id:$";

  public MatlabParamTest(String name) {
    super(name);
  }

  protected void setUp() throws Exception {
    super.setUp();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public final void testRead() {
    File[] vectorList = TestUtilites.INSTANCE.getUnitTestData().listFiles(
        new PrmFileNameFilter());
    assertTrue("Must test prm files", vectorList != null && vectorList.length > 0);
    for (int i = 0; i < vectorList.length; i++) {
      MatlabParam matlabParam = new MatlabParam(null, AxisID.ONLY, vectorList[i], false);
      List<String> errorList = new ArrayList<String>();
      matlabParam.read(null, errorList, null);
      if (!errorList.isEmpty()) {
        StringBuffer buffer = new StringBuffer();
        Iterator iterator = errorList.iterator();
        while (iterator.hasNext()) {
          buffer.append("\n" + iterator.next());
        }
        fail(buffer.toString());
      }
    }
  }

  private static final class PrmFileNameFilter implements FilenameFilter {
    public boolean accept(final File dir, final String fileName) {
      return fileName != null && fileName.endsWith(".prm");
    }
  }
}
