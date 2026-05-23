package etomo.comscript;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import etomo.BaseManager;
import etomo.type.AxisID;
import etomo.type.EtomoBoolean2;
import etomo.type.FileKey;
import etomo.type.FileType;
import etomo.type.ProcessName;
import etomo.type.StringParameter;

/**
 * <p>Description: Parameters and run command for excludeviews batch process.</p>
 *
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class ExcludeViewsParam implements Command {
  private static final ProcessName PROCESS_NAME = ProcessName.EXCLUDE_VIEWS;
  private static final String MAX_EXCLUSION = "9";

  private final String datasetDir;

  private StringParameter stackName = new StringParameter("StackName");
  private StringParameter viewsToExclude = new StringParameter("ViewsToExclude");
  private EtomoBoolean2 montagedImages = new EtomoBoolean2("MontagedImages");
  private EtomoBoolean2 deleteOldFiles = new EtomoBoolean2("DeleteOldFiles");

  private final AxisID axisID;

  private String[] commandArray = null;

  public String toString() {
    return super.toString() + ":[axisID:" + axisID + ",stackName:" + stackName
      + ",viewsToExclude:" + viewsToExclude + ",montagedImages:" + montagedImages
      + ",deleteOldFiles:" + deleteOldFiles;
  }

  public ExcludeViewsParam(final AxisID axisID, final String datasetDir) {
    this.axisID = axisID;
    this.datasetDir = datasetDir;
  }

  public void setStackName(final String input) {
    stackName.set(input);
  }

  public void setViewsToExclude(final String input) {
    viewsToExclude.set(input);
  }

  public void setMontagedImages(final boolean input) {
    montagedImages.set(input);
  }

  public void setDeleteOldFiles(final boolean input) {
    deleteOldFiles.set(input);
  }

  private void createCommand() {
    List<String> command = new ArrayList<String>();
    command.add("python");
    command.add("-u");
    command.add(BaseManager.getIMODBinPath() + PROCESS_NAME.getText());
    command.add("-PID");
    command.add("-" + stackName.getName());
    command.add(stackName.toString());
    command.add("-" + viewsToExclude.getName());
    command.add(viewsToExclude.toString());
    if (montagedImages.is()) {
      command.add("-" + montagedImages.getName());
    }
    if (deleteOldFiles.is()) {
      command.add("-" + deleteOldFiles.getName());
    }
    commandArray = command.toArray(new String[0]);
  }

  public String[] getCommandArray() {
    createCommand();
    return commandArray;
  }

  public String getCommandLine() {
    createCommand();
    if (commandArray == null) {
      return "";
    }
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < commandArray.length; i++) {
      buffer.append(commandArray[i] + " ");
    }
    return buffer.toString();
  }

  public AxisID getAxisID() {
    return axisID;
  }

  public String getCommand() {
    return PROCESS_NAME.getText();
  }

  public String getCommandName() {
    return PROCESS_NAME.getText();
  }

  public ProcessName getProcessName() {
    return PROCESS_NAME;
  }

  public File getCommandInputFile() {
    if (datasetDir != null) {
      return new File(datasetDir, stackName.toString());
    }
    return new File(stackName.toString());
  }

  public File getCommandOutputFile() {
    return getCommandInputFile();
  }

  public CommandMode getCommandMode() {
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
