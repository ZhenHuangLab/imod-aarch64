package etomo.comscript;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import etomo.BaseManager;
import etomo.type.AxisID;
import etomo.type.EtomoNumber;
import etomo.type.FileKey;
import etomo.type.FileType;
import etomo.type.ProcessName;
import etomo.type.StringParameter;

/**
 * <p>Description: Contains parameters and command string for restrictalign. </p>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class RestrictalignParam implements Command {
  private static final ProcessName PROCESS_NAME = ProcessName.RESTRICTALIGN;
  public static final String TARGET_MEASUREMENT_RATIO_KEY = "TargetMeasurementRatio";
  public static final String MIN_MEASUREMENT_RATIO_KEY = "MinMeasurementRatio";

  private EtomoNumber targetMeasurementRatio =
    new EtomoNumber(EtomoNumber.Type.DOUBLE, TARGET_MEASUREMENT_RATIO_KEY);
  private EtomoNumber minMeasurementRatio =
    new EtomoNumber(EtomoNumber.Type.DOUBLE, MIN_MEASUREMENT_RATIO_KEY);
  private StringParameter orderOfRestrictions =
    new StringParameter("OrderOfRestrictions");
  private EtomoNumber skipBeamTiltWithOneRot =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "SkipBeamTiltWithOneRot");

  private final BaseManager manager;
  private final AxisID axisID;
  private final File alignComscriptFile;

  private String[] commandArray = null;

  public RestrictalignParam(final BaseManager manager, final AxisID axisID) {
    this.manager = manager;
    this.axisID = axisID;
    alignComscriptFile = FileType.ALIGN_COMSCRIPT.getFile(manager, axisID);
  }

  /**
   * Creates and returns the command array.  Command array will not be changed after this
   * call.  Not thread safe.
   */
  public String[] getCommandArray() {
    if (commandArray != null) {
      return commandArray;
    }
    List<String> command = new ArrayList<String>();
    command.add("python");
    command.add("-u");
    command.add(BaseManager.getIMODBinPath() + PROCESS_NAME);
    command.add("-AlignCommandFile");
    command.add(alignComscriptFile.getName());
    if (!targetMeasurementRatio.isNull()) {
      command.add("-" + targetMeasurementRatio.getName());
      command.add(targetMeasurementRatio.toString());
    }
    if (!minMeasurementRatio.isNull()) {
      command.add("-" + minMeasurementRatio.getName());
      command.add(minMeasurementRatio.toString());
    }
    if (!orderOfRestrictions.isEmpty()) {
      command.add("-" + orderOfRestrictions.getName());
      command.add(orderOfRestrictions.toString());
    }
    if (!skipBeamTiltWithOneRot.isNull() && skipBeamTiltWithOneRot.is()) {
      command.add("-" + skipBeamTiltWithOneRot.getName());
    }
    commandArray = new String[command.size()];
    commandArray = command.toArray(commandArray);
    return commandArray;
  }

  public void setTargetMeasurementRatio(final String input) {
    targetMeasurementRatio.set(input);
  }

  public void setMinMeasurementRatio(final String input) {
    minMeasurementRatio.set(input);
  }

  public void setOrderOfRestrictions(final String input) {
    orderOfRestrictions.set(input);
  }

  public void setSkipBeamTiltWithOneRot(final String input) {
    skipBeamTiltWithOneRot.set(input);
  }

  public AxisID getAxisID() {
    return AxisID.ONLY;
  }

  public String getCommand() {
    return PROCESS_NAME.toString();
  }

  public File getCommandInputFile() {
    return new File(alignComscriptFile.getAbsolutePath());
  }

  /**
   * Not for running the command.  Use getCommandArray.
   */
  public String getCommandLine() {
    if (commandArray == null) {
      return PROCESS_NAME.toString();
    }
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < commandArray.length; i++) {
      buffer.append(commandArray[i] + " ");
    }
    return buffer.toString();
  }

  public CommandMode getCommandMode() {
    return null;
  }

  public String getCommandName() {
    return PROCESS_NAME.toString();
  }

  public File getCommandOutputFile() {
    return new File(alignComscriptFile.getAbsolutePath());
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
    return null;
  }

  public String getSubcommandProcessName() {
    return null;
  }

  public boolean isMessageReporter() {
    return false;
  }
}
