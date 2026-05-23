package etomo.comscript;

import java.io.File;

import etomo.type.AxisID;
import etomo.type.EtomoNumber;
import etomo.type.FileKey;
import etomo.type.FileType;
import etomo.type.ProcessName;
import etomo.type.ScriptParameter;

/**
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class CryoPositionParam implements CommandParam, Command {
  private static final ProcessName PROCESS_NAME = ProcessName.CRYO_POSITION;
  private static final int LEAVE_TEMP_FILES_DEFAULT = -1;

  private final AxisID axisID;
  private ScriptParameter beadSize =
    new ScriptParameter(EtomoNumber.Type.DOUBLE, "BeadSize");
  private ScriptParameter findBeadsInVolume = new ScriptParameter("FindBeadsInVolume");
  private ScriptParameter leaveTempFiles = new ScriptParameter("LeaveTempFiles");
  private ScriptParameter thicknessOfTomograms =
    new ScriptParameter("ThicknessOfTomograms");
  private ConstTiltParam tiltParam = null;

  CryoPositionParam(final AxisID axisID) {
    this.axisID = axisID;
    leaveTempFiles.setDisplayValue(LEAVE_TEMP_FILES_DEFAULT);
  }

  public void initializeDefaults() {}

  public void parseComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException, InvalidParameterException, FortranInputSyntaxException {
    beadSize.parse(scriptCommand);
    findBeadsInVolume.parse(scriptCommand);
    leaveTempFiles.parse(scriptCommand);
    thicknessOfTomograms.parse(scriptCommand);
  }

  public void updateComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException {
    scriptCommand.useKeywordValue();
    beadSize.updateComScript(scriptCommand);
    findBeadsInVolume.updateComScript(scriptCommand);
    leaveTempFiles.updateComScript(scriptCommand);
    thicknessOfTomograms.updateComScript(scriptCommand);
  }

  public void setThicknessOfTomograms(final String input) {
    thicknessOfTomograms.set(input);
  }

  public void resetBeadSize() {
    beadSize.reset();
  }

  public void setBeadSize(final String input) {
    beadSize.set(input);
  }

  public boolean isBeadSizeSet() {
    return !beadSize.isNull();
  }

  public String getBeadSize() {
    return beadSize.toString();
  }

  public void setFindBeadsInVolume(final boolean input) {
    if (input) {
      findBeadsInVolume.set(2);
    }
    else {
      findBeadsInVolume.reset();
    }
  }

  public void setTiltParam(final ConstTiltParam param) {
    tiltParam = param;
  }

  public AxisID getAxisID() {
    return axisID;
  }

  public String getCommand() {
    return PROCESS_NAME.getComscript(axisID);
  }

  public String[] getCommandArray() {
    return new String[] { PROCESS_NAME.getComscript(axisID) };
  }

  public File getCommandInputFile() {
    return null;
  }

  public String getCommandLine() {
    return PROCESS_NAME.getComscript(axisID);
  }

  public CommandMode getCommandMode() {
    return null;
  }

  public String getCommandName() {
    return PROCESS_NAME.toString();
  }

  public File getCommandOutputFile() {
    return null;
  }

  /**
   * @deprecated 3/15/2019
   */
  @SuppressWarnings("deprecation")
  public FileType getOutputImageFileType() {
    return null;
  }

  public FileKey getOutputImageFileKey() {
    return null;
  }

  /**
   * @deprecated 3/15/2019
   */
  @SuppressWarnings("deprecation")
  public FileType getOutputImageFileType2() {
    return null;
  }

  public FileKey getOutputImageFileKey2() {
    return null;
  }

  public ProcessName getProcessName() {
    return PROCESS_NAME;
  }

  public CommandDetails getSubcommandDetails() {
    // Cryoposition uses tilt.com. Information from tilt.com is required for post
    // processing
    return tiltParam;
  }

  public String getSubcommandProcessName() {
    return null;
  }

  public boolean isMessageReporter() {
    return false;
  }
}
