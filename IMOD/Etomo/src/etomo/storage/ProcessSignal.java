package etomo.storage;

import etomo.logic.ComFileExtensionTool;
import etomo.process.ProcessOutputStrings;
import etomo.process.ProcessState;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.DialogType;
import etomo.type.ProcessName;
import etomo.type.Step;

/**
* <p>Description: Information about a process or step from a string parameter.  This class
* looks at one line at a time and has no context.</p>
* 
* <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
*
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public final class ProcessSignal {
  private final String timestamp;
  private final boolean finished;
  private final ProcessName processName;
  private final Step step;
  private final String line;

  private AxisID axisID = null;
  private ProcessState processState = null;
  private DialogType dialogType = null;

  public String toString() {
    return "[timestamp:" + timestamp + ",finished:" + finished + ",processName:"
      + processName + ",axisID:" + axisID + ",step:" + step + ",processState:"
      + processState + ",dialogType:" + dialogType + ",line:\\n" + line + "]";
  }

  private ProcessSignal(final String timestamp, final boolean finished,
    final String line) {
    this.timestamp = timestamp;
    this.finished = finished;
    this.line = line;
    step = null;
    processName = null;
    axisID = null;
  }

  private ProcessSignal(final AxisID axisID, final String line) {
    this.axisID = axisID;
    this.line = line;
    timestamp = null;
    finished = false;
    step = null;
    processName = null;

  }

  private ProcessSignal(final ProcessState processState, final ProcessName processName,
    final AxisID axisID, final String line) {
    this.processState = processState;
    this.processName = processName;
    this.axisID = axisID;
    this.line = line;
    step = null;
    timestamp = null;
    finished = false;
  }

  /**
   * The step signal.  It has no axis information.
   * @param step
   */
  private ProcessSignal(final Step step, final String line) {
    this.step = step;
    this.processName = null;
    this.axisID = null;
    this.line = line;
    timestamp = null;
    finished = false;
  }

  /**
   * Parses line and returns a ProcessSignal if the line contains information.  Otherwise
   * returns null.
   * @param axisType
   * @param line
   * @param timestamp - when true only gets timestamps
   * @return
   */
  static ProcessSignal getInstance(final AxisType axisType, final String line,
    final boolean timestamp) {
    if (line == null) {
      return null;
    }
    int tagIndex = -1;
    // Look for timestamp.
    if ((tagIndex = line.indexOf(ProcessOutputStrings.BRT_STARTED_DATASET_TAG)) != -1) {
      return getInstance(line, tagIndex, false);
    }
    if ((tagIndex =
      line.indexOf(ProcessOutputStrings.BRT_DATASET_LOG_CLOSED_TAG)) != -1) {
      return getInstance(line, tagIndex, true);
    }
    if (timestamp) {
      return null;
    }
    // Look for information about the current axis
    if ((tagIndex = line.indexOf(ProcessOutputStrings.BRT_START_AXIS)) != -1) {
      return getAxisInstance(line, tagIndex);
    }
    // Look for a process lines or a step line.
    if ((tagIndex = line.indexOf(ProcessOutputStrings.BRT_STEP_MSG_ID)) != -1) {
      return getInstance(axisType, line, tagIndex, ProcessState.INPROGRESS);
    }
    else if ((tagIndex =
      line.indexOf(ProcessOutputStrings.BRT_STEP_SUCCESS_MSG_ID)) != -1) {
      return getInstance(axisType, line, tagIndex, ProcessState.COMPLETE);
    }
    else if (line.indexOf(ProcessOutputStrings.BRT_REACHED_STEP_TAG) != -1) {
      return getInstance(line, tagIndex);
    }
    return null;
  }

  /**
   * Find a process signal from a Step string.
   * @param line
   * @param tagIndex
   * @return
   */
  private static ProcessSignal getInstance(final String line, final int tagIndex) {
    // Assumes:
    // step is at the end of the line
    // Step is preceeded by white space
    String[] lineArray = line.split("\\s+");
    if (lineArray == null || lineArray.length == 0) {
      return null;
    }
    Step step = Step.getInstance(lineArray[lineArray.length - 1]);
    if (step == null) {
      return null;
    }
    return new ProcessSignal(step, line);
  }

  /**
   * Find a process signal with a dataset started or finished tag.  This is to get the
   * timestamp.
   * @param line
   * @param tagIndex
   * @param finished
   * @return
   */
  private static ProcessSignal getInstance(final String line, final int tagIndex,
    final boolean finished) {
    // Batchruntomo started on data set BB, Wed Apr 19 17:37:39 2017
    // Batchruntomo finished with data set, Mon Apr 24 13:29:46 2017
    // Assumes:
    // The timestamp is between the last comma and the tag.
    String[] lineArray = line.substring(0, tagIndex).split("\\s*\\,\\s*");
    if (lineArray == null || lineArray.length == 0) {
      return null;
    }
    return new ProcessSignal(lineArray[lineArray.length - 1], finished, line);
  }

  /**
   * Find a process signal with an axis started tag.  This is to get the
   * current axis being worked on.
   * @param line
   * @param tagIndex
   * @param finished
   * @return
   */
  private static ProcessSignal getAxisInstance(final String line, final int tagIndex) {
    // Completed axis A of dataset BB through step 7
    // Starting axis B [:LOG]
    // Assumes:
    // The axis letter, preceded by whitespace, follows the tag.
    return new ProcessSignal(AxisID.getInstanceIgnoreCase(
      line.substring(tagIndex + ProcessOutputStrings.BRT_START_AXIS.length()).trim()
        .substring(0, 1)),
      line);
  }

  /**
   * Find a process signal with a process name.
   * @param axisType
   * @param line
   * @param tagIndex
   * @param processState
   * @return
   */
  private static ProcessSignal getInstance(final AxisType axisType, final String line,
    final int tagIndex, final ProcessState processState) {
    // Assumes:
    // The tag comes after the process name
    // the process name includes the extension
    // The process name's extension comes after anything that looks like the
    // extension.
    // the process name contains no spaces
    // the process name is preceeded by white space
    int processEndIndex = ComFileExtensionTool.lastIndexOf(line, tagIndex)
      - (axisType == AxisType.DUAL_AXIS ? 1 : 0);
    if (processEndIndex < 0) {
      return null;
    }
    AxisID axisID = AxisID.FIRST;
    if (axisType == AxisType.DUAL_AXIS) {
      axisID = AxisID.getInstance(line.charAt(processEndIndex));
      if (axisID == null) {
        // a process without an axisID such as volcombine.
        processEndIndex++;
      }
    }
    String[] lineArray = line.substring(0, processEndIndex).split("\\s+");
    if (lineArray == null || lineArray.length == 0) {
      return null;
    }
    ProcessName processName = ProcessName.getInstance(lineArray[lineArray.length - 1]);
    if (processName == null) {
      return null;
    }
    return new ProcessSignal(processState, processName, axisID, line);
  }

  public AxisID getAxisID() {
    return axisID;
  }

  public Step getStep() {
    return step;
  }

  public void setAxisID(final AxisID input) {
    axisID = input;
  }

  public void setProcessState(final ProcessState input) {
    processState = input;
  }

  public void setDialogType(final DialogType input) {
    dialogType = input;
  }

  public ProcessState getProcessState() {
    return processState;
  }

  public ProcessName getProcessName() {
    return processName;
  }

  public boolean isTimestamp() {
    return timestamp != null;
  }

  public boolean isAxisID() {
    return axisID != null && timestamp == null && processName == null && step == null;
  }

  public boolean isFinished() {
    return finished;
  }

  public boolean equalsTimestamp(final String input) {
    if (timestamp == null || input == null) {
      return false;
    }
    return timestamp.equals(input);
  }

  public String getTimestamp() {
    return timestamp;
  }

  public DialogType getDialogType() {
    return dialogType;
  }
}