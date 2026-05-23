package etomo.comscript;

import etomo.SerialSectionsManager;
import etomo.type.AxisID;
import etomo.type.FileType;

/**
 * <p>Description: Stores and manages comscripts.</p>
 * 
 * <p>Copyright: Copyright 2012 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class SerialSectionsComScriptManager {
  public static final String rcsid = "$Id:$";

  private final SerialSectionsManager manager;

  private ComScript scriptPreblend = null;
  private ComScript scriptBlend = null;
  private ComScript scriptNewst = null;

  public SerialSectionsComScriptManager(final SerialSectionsManager manager) {
    this.manager = manager;
  }

  public void loadPreblend(final AxisID axisID) {
    scriptPreblend = ComScriptUtil.loadComScript(manager, FileType.PREBLEND_COMSCRIPT,
      axisID, true, false, false);
  }

  public SetEnvParam getSetEnvParamFromPreblend(final AxisID axisID,
    final String envVar) {
    // Initialize a SetEnvParam object from the com script command
    // object
    SetEnvParam param = new SetEnvParam(envVar);
    // Assuming its the first setenv command in the comfile.
    if (!ComScriptUtil.initialize(manager, param, scriptPreblend,
      SetEnvParam.COMMAND_NAME, axisID, true, null, true, false, false, false, envVar)) {
      return null;
    }
    return param;
  }

  public BlendmontParam getBlendmontParamFromPreblend(final AxisID axisID,
    final String rootName) {
    BlendmontParam param = new BlendmontParam(manager, rootName, axisID,
      BlendmontParam.Mode.SERIAL_SECTION_PREBLEND);
    ComScriptUtil.initialize(manager, param, scriptPreblend, BlendmontParam.COMMAND_NAME,
      axisID, false, false, true);
    return param;
  }

  public void savePreblend(final SetEnvParam param, final AxisID axisID,
    final String envVar) {
    ComScriptUtil.addModifyCommand(manager, scriptPreblend, param,
      SetEnvParam.COMMAND_NAME, axisID, false, false, envVar);
  }

  public void savePreblend(final BlendmontParam param, final AxisID axisID) {
    ComScriptUtil.modifyCommand(manager, scriptPreblend, param,
      BlendmontParam.COMMAND_NAME, axisID, true, false);
  }

  public void loadBlend(final AxisID axisID) {
    scriptBlend = ComScriptUtil.loadComScript(manager, FileType.BLEND_COMSCRIPT, axisID,
      true, false, false);
  }

  public SetEnvParam getSetEnvParamFromBlend(final AxisID axisID, final String envVar) {
    // Initialize a SetEnvParam object from the com script command
    // object
    SetEnvParam param = new SetEnvParam(envVar);
    // Assuming its the first setenv command in the comfile.
    if (!ComScriptUtil.initialize(manager, param, scriptBlend, SetEnvParam.COMMAND_NAME,
      axisID, true, null, true, false, false, false, envVar)) {
      return null;
    }
    return param;
  }

  public BlendmontParam getBlendmontParamFromBlend(final AxisID axisID,
    final String rootName) {
    BlendmontParam param = new BlendmontParam(manager, rootName, axisID,
      BlendmontParam.Mode.SERIAL_SECTION_BLEND);
    ComScriptUtil.initialize(manager, param, scriptBlend, BlendmontParam.COMMAND_NAME,
      axisID, false, false, true);
    return param;
  }

  public void saveBlend(final SetEnvParam param, final AxisID axisID,
    final String envVar) {
    ComScriptUtil.addModifyCommand(manager, scriptBlend, param, SetEnvParam.COMMAND_NAME,
      axisID, false, false, envVar);
  }

  public void saveBlend(final BlendmontParam param, final AxisID axisID) {
    ComScriptUtil.modifyCommand(manager, scriptBlend, param, BlendmontParam.COMMAND_NAME,
      axisID, true, false);
  }

  public void loadNewst(final AxisID axisID) {
    scriptNewst = ComScriptUtil.loadComScript(manager, FileType.NEWST_COMSCRIPT, axisID,
      true, false, false);
  }

  public SetEnvParam getSetEnvParamFromNewst(final AxisID axisID, final String envVar) {
    // Initialize a SetEnvParam object from the com script command
    // object
    SetEnvParam param = new SetEnvParam(envVar);
    // Assuming its the first setenv command in the comfile.
    if (!ComScriptUtil.initialize(manager, param, scriptNewst, SetEnvParam.COMMAND_NAME,
      axisID, true, null, true, false, false, false, envVar)) {
      return null;
    }
    return param;
  }

  public NewstParam getNewstackParam(final AxisID axisID, final String rootName) {
    NewstParam param = NewstParam.getColorInstance(manager, axisID);
    ComScriptUtil.initialize(manager, param, scriptNewst, param.getCommandName(), axisID,
      false, false, true);
    return param;
  }

  public void saveNewst(final SetEnvParam param, final AxisID axisID,
    final String envVar) {
    ComScriptUtil.addModifyCommand(manager, scriptNewst, param, SetEnvParam.COMMAND_NAME,
      axisID, false, false, envVar);
  }

  public void saveNewst(final NewstParam param, final AxisID axisID) {
    ComScriptUtil.modifyCommand(manager, scriptNewst, param, param.getCommandName(),
      axisID, true, false);
  }
}
