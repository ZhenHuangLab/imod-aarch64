package etomo.comscript;

import java.io.IOException;

import etomo.ApplicationManager;
import etomo.EtomoDirector;
import etomo.type.AxisID;
import etomo.type.AxisType;
import junit.framework.TestCase;

/**
 * <p>Description: Unit test of ComScriptUtil.  Was ComScriptManagerTest.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class ComScriptUtilTest extends TestCase {

  // FIXME this needs to really test the method, not just exercise it.

  public void testUseTemplate() {
    // Need an application manger to get the IMOD_DIR environment
    // variable
    ApplicationManager manager =
      (ApplicationManager) EtomoDirector.INSTANCE.getCurrentManagerForTest();
    System.out.println(EtomoDirector.INSTANCE.getIMODDirectory().getAbsolutePath());
    ComScriptManager comScriptManager = manager.getComScriptManager();
    try {
      ComScriptUtil.useTemplate(manager, "mtffilter", "datasetName", AxisType.SINGLE_AXIS,
        AxisID.ONLY);
      ComScriptUtil.useTemplate(manager, "eraser", "datasetName", AxisType.DUAL_AXIS,
        AxisID.SECOND);
      ComScriptUtil.useTemplate(manager, "volcombine", "datasetName", AxisType.DUAL_AXIS,
        AxisID.ONLY);
      ComScriptUtil.useTemplate(manager, "solvematch", "datasetName", AxisType.DUAL_AXIS,
        AxisID.ONLY);
    }
    catch (BadComScriptException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}
