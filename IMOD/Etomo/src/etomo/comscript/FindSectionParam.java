package etomo.comscript;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import etomo.BaseManager;
import etomo.type.AxisID;
import etomo.type.FileKey;
import etomo.type.FileType;
import etomo.type.ProcessName;

/**
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class FindSectionParam implements Command {
  private static final ProcessName PROCESS_NAME = ProcessName.FIND_SECTION;
  private static final String TOMOGRAM_FILE_TAG = "-tomo";

  private final AxisID axisID;
  private final BaseManager manager;
  private final Mode mode;

  private String[] commandArray = null;

  public FindSectionParam(final BaseManager manager, final AxisID axisID,
    final boolean wholeTomogram) {
    this.axisID = axisID;
    this.manager = manager;
    if (!wholeTomogram) {
      mode = Mode.SAMPLE;
    }
    else {
      mode = Mode.WHOLE_TOMOGRAM;
    }
  }

  private void buildCommandArray() {
    List<String> array;
    synchronized (this) {
      if (commandArray != null) {
        return;
      }
      array = new ArrayList<String>();
    }
    array.add(BaseManager.getIMODBinPath() + PROCESS_NAME.toString());
    array.add("-scales");
    if (mode == Mode.SAMPLE) {
      array.add("4");
    }
    else {
      array.add("2");
    }
    array.add("-pitch");
    array.add(FileType.TOMOPITCH_MODEL.getFileName(manager, axisID));
    array.add("-size");
    if (mode == Mode.SAMPLE) {
      array.add("50,1,20");
    }
    else {
      array.add("16,1,16");
    }
    if (mode == Mode.WHOLE_TOMOGRAM) {
      array.add("-block");
      array.add("48");
      array.add("-samples");
      array.add("5");
    }
    array.add(TOMOGRAM_FILE_TAG);
    if (mode == Mode.SAMPLE) {
      array.add(FileType.TOP_SAMPLE.getFileName(manager, axisID));
      array.add(TOMOGRAM_FILE_TAG);
      array.add(FileType.MIDDLE_SAMPLE.getFileName(manager, axisID));
      array.add(TOMOGRAM_FILE_TAG);
      array.add(FileType.BOTTOM_SAMPLE.getFileName(manager, axisID));
    }
    else {
      array.add(FileType.TILT_OUTPUT.getFileName(manager, axisID));
    }
    commandArray = (String[]) array.toArray(new String[0]);
  }

  public AxisID getAxisID() {
    return axisID;
  }

  public String getCommand() {
    return PROCESS_NAME.toString();
  }

  public String[] getCommandArray() {
    buildCommandArray();
    return commandArray;
  }

  public File getCommandInputFile() {
    return null;
  }

  public String getCommandLine() {
    buildCommandArray();
    if (commandArray == null) {
      return "";
    }
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < commandArray.length; i++) {
      buffer.append(commandArray[i] + " ");
    }
    return buffer.toString();
  }

  public CommandMode getCommandMode() {
    return mode;
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
    return null;
  }

  public String getSubcommandProcessName() {
    return null;
  }

  public boolean isMessageReporter() {
    return false;
  }

  public final static class Mode implements CommandMode {
    private static final Mode SAMPLE = new Mode("Sample");
    private static final Mode WHOLE_TOMOGRAM = new Mode("WholeTomogram");

    private final String string;

    private Mode(String string) {
      this.string = string;
    }

    public String toString() {
      return string;
    }
  }
}
