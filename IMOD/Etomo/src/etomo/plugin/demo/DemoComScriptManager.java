package etomo.plugin.demo;

import etomo.BaseManager;
import etomo.comscript.ComScript;
import etomo.comscript.ComScriptUtil;
import etomo.type.AxisID;
import etomo.type.AxisType;

/**
 * <p>Description: Stores and manages comscripts.  This class just uses a utility class,
 * so its functionality could be handled by any class.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class DemoComScriptManager {
  private final BaseManager manager;
  private final AxisID axisID;
  private final AxisType axisType;

  private ComScript scriptDemo = null;

  DemoComScriptManager(final BaseManager manager, final AxisID axisID,
    final AxisType axisType) {
    this.manager = manager;
    this.axisID = axisID;
    this.axisType = axisType;
  }

  boolean loadDemo() {
    // Assign the new ComScriptObject object to the appropriate reference
    scriptDemo = ComScriptUtil.loadComScript(manager,
      DemoFileType.DEMO_COMSCRIPT.getFileName(manager, axisID), axisID, true, false,
      false, false);
    return scriptDemo != null;
  }

  EtomoPluginDemoParam getEtomoDemoPluginParam() {
    // Initialize a DemosetupParam object from the com script command object
    EtomoPluginDemoParam param = new EtomoPluginDemoParam(axisID);
    ComScriptUtil.initialize(manager, param, scriptDemo,
      EtomoPluginDemoParam.COMMAND_PROCESS_NAME.toString(), axisID, true, true, true);
    return param;
  }

  void saveDemo(EtomoPluginDemoParam param, AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript script = scriptDemo;
    ComScriptUtil.modifyCommand(manager, script, param,
      EtomoPluginDemoParam.COMMAND_PROCESS_NAME.toString(), axisID, false, false);
  }
}