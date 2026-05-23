package etomo.comscript;

import etomo.ToolsManager;
import etomo.type.AxisID;
import etomo.type.FileType;

/**
 * <p>Description: Stores and manages comscripts.</p>
 * 
 * <p>Copyright: Copyright 2010 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 3.2  2010/04/28 16:10:39  sueh
 * <p> bug# 1344 Constructing WarpVolParam with the mode.
 * <p>
 * <p> Revision 3.1  2010/02/17 04:47:54  sueh
 * <p> bug# 1301 Using the manager instead of the manager key do pop up
 * <p> messages.
 * <p> </p>
 */

public final class ToolsComScriptManager {
  private final ToolsManager manager;

  private ComScript scriptFlatten = null;

  public ToolsComScriptManager(final ToolsManager manager) {
    this.manager = manager;
  }

  public void loadFlatten(final AxisID axisID) {
    scriptFlatten = ComScriptUtil.loadComScript(manager, FileType.FLATTEN_TOOL_COMSCRIPT,
      axisID, true, false, false);
  }

  public boolean isWarpVolParamInFlatten(AxisID axisID) {
    return ComScriptUtil
      .loadComScript(manager, FileType.FLATTEN_TOOL_COMSCRIPT, axisID, true, false, false)
      .isCommandLoaded();
  }

  public WarpVolParam getWarpVolParamFromFlatten(final AxisID axisID) {
    // Initialize a WarpVolParam object from the com script command
    // object
    WarpVolParam param = new WarpVolParam(manager, axisID, WarpVolParam.Mode.TOOLS);
    ComScriptUtil.initialize(manager, param, scriptFlatten, WarpVolParam.COMMAND, axisID,
      false, false, true);
    return param;
  }

  /**
   * Save the WarpVolParam command to the flatten com script
   * @param warpVolParam
   */
  public void saveFlatten(final WarpVolParam param, final AxisID axisID) {
    ComScriptUtil.modifyCommand(manager, scriptFlatten, param, WarpVolParam.COMMAND,
      axisID, true, false);
  }
}
