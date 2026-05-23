package etomo.comscript;

import etomo.JoinManager;
import etomo.type.AxisID;
import etomo.type.FileType;

public class JoinComscriptManager {

  private final JoinManager manager;
  private ComScript scriptJoinWarp2Model = null;

  public JoinComscriptManager(final JoinManager manager) {
    this.manager = manager;
  }

  public void loadJoinWarp2Model(final AxisID axisID) {
    scriptJoinWarp2Model = ComScriptUtil.loadComScript(manager,
      FileType.JOIN_WARP_2_MODEL_COMSCRIPT, axisID, true, false, false);
  }

  public Joinwarp2modelParam getJoinwarp2modelParam(final AxisID axisID) {
    // Initialize a SetEnvParam object from the com script command
    // object
    Joinwarp2modelParam param = new Joinwarp2modelParam(manager);
    // Assuming its the first setenv command in the comfile.
    if (!ComScriptUtil.initialize(manager, param, scriptJoinWarp2Model,
      Joinwarp2modelParam.COMMAND_NAME, axisID, true, null, true, false, false, false,
      null)) {
      return null;
    }
    return param;
  }

  public void saveJoinwarp2model(final Joinwarp2modelParam param, final AxisID axisID) {
    ComScriptUtil.addModifyCommand(manager, scriptJoinWarp2Model, param,
      Joinwarp2modelParam.COMMAND_NAME, axisID, false, false, null);
  }

}
