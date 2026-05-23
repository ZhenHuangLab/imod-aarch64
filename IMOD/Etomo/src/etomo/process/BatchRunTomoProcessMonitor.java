package etomo.process;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import etomo.BatchRunTomoManager;
import etomo.logic.BusyStatusMediator;
import etomo.storage.LogFile;
import etomo.type.AxisID;
import etomo.type.BatchRunTomoDatasetState;
import etomo.type.BatchRunTomoDatasetStatus;
import etomo.type.BatchRunTomoStatus;
import etomo.type.CurrentArrayList;
import etomo.type.EndingStep;
import etomo.type.FileType;
import etomo.type.ProcessEndState;
import etomo.type.ProcessName;
import etomo.type.Status;
import etomo.type.StatusChangeEvent;
import etomo.type.StatusChangeListener;
import etomo.type.StatusChangeTaggedEvent;
import etomo.type.StatusChanger;
import etomo.type.Step;
import etomo.util.ReaderOpener;
import etomo.util.TimeLimitedCodeBlock;

/**
* <p>Description: Monitor for batchruntomo in BATCH mode.</p>
* 
* <p>Copyright: Copyright 2015 - 2020 by the Regents of the University of Colorado</p>
* 
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public final class BatchRunTomoProcessMonitor
  implements OutfileProcessMonitor, StatusChanger {
  private static final String TITLE = "Batchruntomo";

  private final ProcessMessages messages;
  private final BatchRunTomoManager manager;
  private final AxisID axisID;
  private final CurrentArrayList<String> runKeys;
  private final boolean reconnect;
  private final ArrayList<String> stacks = new ArrayList<String>();
  private final ProcessData processData;
  private final BusyStatusMediator busyStatusMediator;

  private boolean updateProgressBar = false;// turn on to changed the progress bar title
  private ProcessEndState endState = null;
  private LogFile commandsPipe = null;
  private LogFile.WriterId commandsPipeWriterId = null;
  private boolean useCommandsPipe = true;
  private LogFile processOutput = null;
  private LogFile.ReaderId processOutputReaderId = null;
  private boolean processRunning = true;
  private boolean pausing = false;
  private boolean killing = false;
  private boolean stop = false;
  private boolean running = false;
  private SystemProcessInterface process = null;
  private Vector<StatusChangeListener> listeners = null;
  private String currentStep = null;
  private String currentDataset = null;
  private boolean willResume = false;
  private boolean interrupted = false;
  private boolean datasetRunning = false;
  private boolean endingStepSet = false;
  private boolean startingStepSet = false;
  private int nDatasets = 0;
  private boolean datasetFailed = false;
  private boolean datasetDelivered = false;
  private boolean datasetRenamed = false;
  private boolean live = true;// The program is running
  private int processedLineNumber = 0;
  private boolean halt = false;

  private BatchRunTomoProcessMonitor(final BatchRunTomoManager manager,
    final AxisID axisID, final CurrentArrayList<String> runKeys, ProcessData processData,
    final ProcessMessages messages, final boolean reconnect) {
    this.manager = manager;
    this.axisID = axisID;
    this.messages = messages;
    this.runKeys = runKeys;
    this.reconnect = reconnect;
    this.processData = processData;
    busyStatusMediator = manager.getBusyStatusMediator();
    busyStatusMediator.msgMonitorConstructed(axisID);
    if (runKeys != null) {
      nDatasets = runKeys.size();
    }
  }

  static BatchRunTomoProcessMonitor getInstance(final BatchRunTomoManager manager,
    final AxisID axisID, final CurrentArrayList<String> runKeys, ProcessData processData,
    final ProcessMessages messages) {
    BatchRunTomoProcessMonitor instance = new BatchRunTomoProcessMonitor(manager, axisID,
      runKeys, processData, messages, false);
    return instance;
  }

  static BatchRunTomoProcessMonitor getReconnectInstance(
    final BatchRunTomoManager manager, final AxisID axisID, final ProcessData processData,
    final ProcessMessages messages) {
    BatchRunTomoProcessMonitor instance = new BatchRunTomoProcessMonitor(manager, axisID,
      processData.getKeyArray(), processData, messages, true);
    instance.live = false;
    instance.processedLineNumber = processData.getLineNumber();
    processData.resetLineNumber();
    if (instance.processedLineNumber > 0) {
      messages.hibernate();
    }
    return instance;
  }

  public void dumpState() {
    System.err.print("[updateProgressBar:" + updateProgressBar + ",useCommandsPipe:"
      + useCommandsPipe + ",\nprocessRunning:" + processRunning + ",pausing:" + pausing
      + ",\nkilling:" + killing + ",stop:" + stop + ",running:" + running
      + ",\nreconnect:" + reconnect + "]");
  }

  /**
   * Sets the process.
   */
  public void setProcess(final SystemProcessInterface process) {
    this.process = process;
    if (process != null) {
      process.setKeyArray(runKeys);
    }
  }

  public boolean isRunning() {
    return running;
  }

  public void stop() {
    stop = true;
  }

  public void addStatusChangeListener(final StatusChangeListener listener) {
    if (listener == null) {
      return;
    }
    boolean newCollection = false;
    if (listeners == null) {
      synchronized (this) {
        if (listeners == null) {
          listeners = new Vector<StatusChangeListener>();
          newCollection = true;
        }
      }
    }
    if (!newCollection && listeners.contains(listener)) {
      return;
    }
    listeners.add(listener);
  }

  synchronized public void halt() {
    halt = true;
  }

  public void run() {
    running = true;
    manager.msgStatusChangerStarted(this);
    if (!reconnect) {
      manager.logMessage("Running batchruntomo");
    }
    try {
      /* Wait for processchunks or prochunks to delete .cmds file before enabling the Kill
       * Process button and Pause button. The main loop uses a sleep of 2000 millisecs.
       * This change pushes the first sleep back before the command buttons are turned on.
       * The monitor starts running before processchunks starts, so its easy to send a
       * command to a file which is not being watched and will be deleted by
       * processchunks. Not allowing commands to be sent for the period of the first sleep
       * also reduces the chance of a collision on Windows - where processchunks cannot
       * delete the command pipe file (.cmds file) because it is in use. */
      Thread.sleep(2000);
    }
    catch (InterruptedException e) {}
    // Get ready to respond to the Kill Process button and Pause button.
    useCommandsPipe = true;
    // Turn on the Kill Process button and Pause button.
    initializeProgressBar();
    sendStatusChanged(BatchRunTomoStatus.RUNNING);
    try {
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException e) {}
      while (processRunning && !stop) {
        try {
          if (updateState() || updateProgressBar) {
            updateProgressBar();
          }
          if (halt) {
            // Can halt since the updateState has returned. But all existing messages
            // must be processed before state is valid.
            messages.feedEndMessage();
            busyStatusMediator.msgMonitorStopped(axisID);
            return;
          }
          if (!reconnect || live) {
            Thread.sleep(100);
          }
        }
        catch (InterruptedException e) {
          e.printStackTrace();
          interrupted = true;
        }
      }
      endMonitor();
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
      endMonitor(ProcessEndState.FAILED);
    }
    catch (FileNotFoundException e) {
      // Reader opener failed
      e.printStackTrace();
      endMonitor(ProcessEndState.FAILED);
    }
    catch (IOException e) {
      e.printStackTrace();
      endMonitor(ProcessEndState.FAILED);
    }
    // Disable the use of the commands pipe.
    useCommandsPipe = false;
    running = false;
    if (listeners != null) {
      BatchRunTomoStatus status;
      if (pausing || killing) {
        status = BatchRunTomoStatus.KILLED_PAUSED;
        if (killing) {
          // runKeys should be pointing to the last completed dataset.
          runKeys.previous();
        }
      }
      else if (endingStepSet) {
        status = BatchRunTomoStatus.STOPPED;
      }
      else {
        status = BatchRunTomoStatus.OPEN;
      }
      sendStatusChanged(status);
    }
    busyStatusMediator.msgMonitorStopped(axisID);
  }

  private void sendStatusChanged(final BatchRunTomoStatus status) {
    SwingUtilities.invokeLater(new StatusChangeEventSender(status));
  }

  private void sendStatusChanged(final Status status) {
    SwingUtilities.invokeLater(new StatusChangeEventSender(
      new StatusChangeTaggedEvent(runKeys.getCurrent(), status)));
  }

  private void sendStatusChanged(final Status status, final String string) {
    SwingUtilities.invokeLater(new StatusChangeEventSender(
      new StatusChangeTaggedEvent(runKeys.getCurrent(), string, status)));
  }

  public void msgLogFileRenamed() {}

  public void endMonitor(final ProcessEndState endState) {
    // Output file will use the success tag when it ends from a pause. Don't lose the
    // pause state unless overriding it with something other then Done.
    if (this.endState == null || (endState != null && endState != ProcessEndState.DONE)) {
      setProcessEndState(endState);
    }
    endMonitor();
  }

  void endMonitor() {
    processRunning = false;// the only place that this should be changed
    closeProcessOutput();
    messages.feedEndMessage();
  }

  String getLogFileName() {
    try {
      return getProcessOutputFileName();
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
      return "";
    }
  }

  public String getProcessOutputFileName() throws LogFile.LockException {
    createProcessOutput();
    return processOutput.getName();
  }

  /**
   * set end state
   * @param endState
   */
  public synchronized void setProcessEndState(final ProcessEndState endState) {
    this.endState = ProcessEndState.precedence(this.endState, endState);
  }

  public ProcessEndState getProcessEndState() {
    return endState;
  }

  public ProcessMessages getProcessMessages() {
    return messages;
  }

  public String getSubProcessName() {
    return null;
  }

  public void kill(final SystemProcessInterface process, final AxisID axisID) {
    try {
      writeCommand("Q");
      killing = true;
      updateProgressBar = true;
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void pause(final SystemProcessInterface process, final AxisID axisID) {
    try {
      writeCommand("F");
      pausing = true;
      updateProgressBar = true;
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean isPausing() {
    return pausing && processRunning;
  }

  public void setWillResume() {
    willResume = true;
    setProgressBarTitle();
  }

  public String getStatusString() {
    return runKeys.getTotal() + " of " + nDatasets + " completed";
  }

  public boolean isProcessRunning() {
    return processRunning;
  }

  AxisID getAxisID() {
    return axisID;
  }

  public final String getPid() {
    return null;
  }

  synchronized void closeProcessOutput() {
    if (processOutput != null && processOutputReaderId != null
      && !processOutputReaderId.isEmpty()) {
      processOutput.closeRead(processOutputReaderId);
      processOutput = null;
    }
  }

  String readParameters(String line)
    throws LogFile.LockException, FileNotFoundException, IOException {
    while (processOutput != null
      && (line = processOutput.readLine(processOutputReaderId)) != null) {
      line = line.trim();
      messages.feedString(line);
      if (line.indexOf(ProcessOutputStrings.END_PARAMETERS_TAG) != -1) {
        break;
      }
      if (line.indexOf(ProcessOutputStrings.BRT_ENDING_STEP_PARAM_TAG) != -1) {
        endingStepSet = true;
      }
      if (line.indexOf(ProcessOutputStrings.BRT_STARTING_STEP_PARAM_TAG) != -1) {
        startingStepSet = true;
      }
    }
    line = processOutput.readLine(processOutputReaderId);
    if (line != null) {
      line = line.trim();
    }
    return line;
  }

  @SuppressWarnings("deprecation")
  boolean updateState() throws LogFile.LockException, FileNotFoundException, IOException {
    createProcessOutput();
    if (processRunning
      && (processOutputReaderId == null || processOutputReaderId.isEmpty())) {
      ReaderOpener readerOpener = new ReaderOpener(processOutput);
      try {
        long timeout = 10;
        processOutputReaderId = TimeLimitedCodeBlock.runWithTimeout(readerOpener,
          reconnect ? timeout / 2 : timeout, TimeUnit.SECONDS);
        readerOpener.stop();
      }
      catch (LogFile.LockException e) {
        e.printStackTrace();
        readerOpener.stop();
        throw e;
      }
      catch (FileNotFoundException e) {
        e.printStackTrace();
        readerOpener.stop();
        throw e;
      }
      catch (IOException e) {
        e.printStackTrace();
        readerOpener.stop();
        throw e;
      }
      catch (Exception e) {
        e.printStackTrace();
        readerOpener.stop();
        stop = true;
      }
    }
    if (processOutputReaderId == null || processOutputReaderId.isEmpty()) {
      return false;
    }
    String line = null;
    int index;
    int index2;
    while (!halt && processOutput != null
      && (line = processOutput.readLine(processOutputReaderId)) != null) {
      processData.incrementLineNumber();
      // Avoid processing output more then once (for reconnect).
      if (reconnect && processData.isGtLineNumber(processedLineNumber)) {
        messages.wake();
      }
      line = line.trim();
      // Handle parameter list at the top of the log
      if (line.indexOf(ProcessOutputStrings.BRT_START_PARAMETERS_TAG) != -1) {
        line = readParameters(line);
      }
      // Send all output to the ProcessMessages blocking queue, so each line can be
      // processed immediately.
      // Handle messages that need to be logged, and send other lines to ProcessMessages.
      boolean recognized = false;
      if ((line.indexOf(ProcessOutputStrings.BRT_DATASET_MSG_ID) != -1
        || line.indexOf(ProcessOutputStrings.BRT_DATASET_TAG) != -1)
        && (index = line.indexOf(ProcessOutputStrings.BRT_DATASET_LOCATION_TAG)) != -1) {
        recognized = true;
        // Location of the dataset is after the first "set ".
        currentDataset =
          line.substring(index + ProcessOutputStrings.BRT_DATASET_LOCATION_TAG.length());
        if (currentDataset != null) {
          currentDataset = currentDataset.trim();
          // The dataset name does not contain whitespace
          if (!currentDataset.isEmpty() && currentDataset.matches(".+\\s+.+")) {
            String splitArray[] = currentDataset.split("\\s+");
            if (splitArray != null && splitArray.length > 0) {
              currentDataset = splitArray[0];
            }
          }
        }
        // New dataset
        datasetFailed = false;
        // Send a linefeed and the dataset start message to the project log. Use the
        // ProcessMessages string feed so that messages get to the project log in the
        // right order.
        messages.feedNewline(ProcessMessages.MessageType.LOG);
        messages.feedMessage(ProcessMessages.MessageType.LOG, line);
        return true;
      }
      if (recognized) {
        continue;
      }
      // Handle tagged lines.
      messages.feedString(line);
      // Find deprecate logged messages. Now handled by the addition of a log message tag.
      if (!ProcessMessages.MessageType.LOG.isType(line)) {
        for (int i = 0; i < ProcessOutputStrings.BRT_LOG_TAGS.length; i++) {
          if (line.indexOf(ProcessOutputStrings.BRT_LOG_TAGS[i]) != -1) {
            recognized = true;
            // Send output that users want to see in the project log.
            messages.feedMessage(ProcessMessages.MessageType.LOG, line);
            break;
          }
        }
        // Backwards compatibility
        if (line.indexOf(ProcessOutputStrings.BRT_AXIS_B_TAG) != -1) {
          recognized = true;
          messages.feedMessage(ProcessMessages.MessageType.LOG, line);
        }
        if (recognized) {
          continue;
        }
      }
      if (line.indexOf(ProcessOutputStrings.BRT_STARTING_DATASET_MSG_ID) != -1
        || line.indexOf(ProcessOutputStrings.BRT_STARTING_DATASET_TAG) != -1) {
        recognized = true;
        runKeys.next();
        datasetRunning = true;
        datasetFailed = false;
        datasetDelivered = false;
        datasetRenamed = false;
        if (startingStepSet) {
          sendStatusChanged(BatchRunTomoDatasetState.STARTING);
        }
        else {
          sendStatusChanged(BatchRunTomoDatasetState.RUNNING);
        }
        currentStep = null;
        return true;
      }
      // check for the real batchruntomo error message. Everything else will be logged.
      if (line.indexOf(ProcessOutputStrings.BRT_BATCH_RUN_TOMO_ERROR_TAG) != -1) {
        currentStep = "failed";
        datasetRunning = false;
        if (runKeys.getTotal() == 0) {
          endMonitor(ProcessEndState.FAILED);
        }
        else {
          endMonitor(ProcessEndState.DONE);

        }
        return true;
      }
      if (line.equals(ProcessOutputStrings.BRT_SUCCESS_TAG)) {
        endMonitor(ProcessEndState.DONE);
        return true;
      }
      if (line.indexOf(ProcessOutputStrings.BRT_KILLING_MSG_ID) != -1
        || line.equals(ProcessOutputStrings.BRT_KILLING_TAG)) {
        // A kill is asynchronous, so it could happen just after a dataset is completed.
        // In that case no dataset was killed.
        if (endState == null && datasetRunning) {
          sendStatusChanged(BatchRunTomoDatasetState.KILLED);
        }
        setProcessEndState(ProcessEndState.KILLED);
        return true;
      }
      if (line.indexOf(ProcessOutputStrings.BRT_PAUSED_MSG_ID) != -1
        || line.equals(ProcessOutputStrings.BRT_PAUSED_TAG)) {
        endMonitor(ProcessEndState.PAUSED);
        return true;
      }
      if (line.indexOf(ProcessOutputStrings.BRT_ETOMO_TAG) != -1
        || line.indexOf(ProcessOutputStrings.BRT_ETOMO_TAG_OLD) != -1) {
        currentStep = "Etomo setup";
        return true;
      }
      if (line.indexOf(ProcessOutputStrings.BRT_STEP_SUCCESS_MSG_ID) != -1
        || line.indexOf(ProcessOutputStrings.BRT_STEP_SUCCESS_TAG) != -1) {
        if (currentStep != null) {
          ProcessName processName = ProcessName.getInstance(currentStep);
          if (processName == ProcessName.VOLCOMBINE
            || processName == ProcessName.TRIMVOL) {
            sendStatusChanged(processName);
          }
        }
        currentStep += " - done";
        return true;
      }
      //first one was BRT_DATASET_SUCCESS_MSG_ID
      if (line.indexOf(ProcessOutputStrings.BRT_DATASET_LOG_CLOSED_TAG) != -1
     /*   || line.indexOf(ProcessOutputStrings.BRT_DATASET_SUCCESS_TAG) != -1*/) {
        // Dataset succeeded
        currentStep = "done";
        datasetRunning = false;
        BatchRunTomoDatasetState state;
        if (datasetFailed) {
          state = BatchRunTomoDatasetState.FAILED;
        }
        else {
          runKeys.incrementTotal();
          if (endingStepSet) {
            state = BatchRunTomoDatasetState.STOPPED;
          }
          else {
            state = BatchRunTomoDatasetState.DONE;
          }
        }
        sendStatusChanged(state);
        return true;
      }
      if (line.indexOf(ProcessOutputStrings.BRT_ABORT_SET_TAG) != -1) {
        // Dataset failed
        currentStep = "failed";
        datasetRunning = false;
        datasetFailed = true;
        sendStatusChanged(BatchRunTomoDatasetState.FAILED);
        return true;
      }
      if (line.indexOf(ProcessOutputStrings.BRT_ABORT_AXIS_TAG) != -1) {
        datasetFailed = true;
        currentStep = "axis failed";
        sendStatusChanged(BatchRunTomoDatasetState.FAILING);
        return true;
      }
      if ((line.indexOf(ProcessOutputStrings.BRT_STEP_MSG_ID) != -1
        || line.indexOf(ProcessOutputStrings.BRT_STEP_TAG) != -1)
        && (index = line.lastIndexOf(ProcessOutputStrings.BRT_STEP_END_TAG)) != -1) {
        // The current step is the name of the com file (which may not contain
        // whitespace).
        String substring = line.substring(0, index);
        if (substring != null) {
          String[] splitArray = substring.split("\\s+");
          if (splitArray != null && splitArray.length > 0) {
            currentStep = splitArray[splitArray.length - 1];
            return true;
          }
        }
      }
      if (line.indexOf(ProcessOutputStrings.BRT_REACHED_STEP_TAG) != -1) {
        String stepValue = line.substring(line.lastIndexOf(" ")).trim();
        EndingStep endingStep = EndingStep.getInstance(stepValue);
        if (endingStep != null) {
          sendStatusChanged(endingStep);
          continue;
        }
        Step step = Step.getInstance(stepValue);
        if (step != null) {
          sendStatusChanged(step);
          continue;
        }
      }
      String file = null;
      if (!datasetDelivered) {
        file = getFileFromOutput(line, ProcessOutputStrings.BRT_DELIVERED_MSG_ID,
          ProcessOutputStrings.BRT_DELIVERED_TAG);
      }
      if (file != null) {
        datasetDelivered = true;
        sendStatusChanged(BatchRunTomoDatasetStatus.DELIVERED, file);
        continue;
      }
      if (!datasetRenamed) {
        file = getFileFromOutput(line, ProcessOutputStrings.BRT_RENAMED_MSG_ID,
          ProcessOutputStrings.BRT_RENAMED_TAG);
      }
      if (file != null) {
        datasetRenamed = true;
        sendStatusChanged(BatchRunTomoDatasetStatus.RENAMED, file);
        continue;
      }
    }
    if (processRunning && line == null) {
      if (interrupted) {
        endMonitor();
        return true;
      }
      else {
        // Waiting for something to complete - must have caught up with the process.
        live = true;
      }
    }
    return false;
  }

  /**
   * Gets a file path from a recognized output line.  Returns null if the status had been
   * previously sent.  Returns the file path if it is found.
   * @param line
   * @param msgId
   * @param deprecatedTag optional
   * @return
   */
  static String getFileFromOutput(final String line, final String msgId,
    final String deprecatedTag) {
    // Attempt to recognize the output line.
    if (line.indexOf(msgId) == -1
      && (deprecatedTag == null || line.indexOf(deprecatedTag) == -1)) {
      return null;
    }
    // Attempt to get the file.
    int index = line.indexOf(ProcessOutputStrings.BRT_FILE_LOCATION_TAG);
    if (index == -1) {
      return null;
    }
    String file =
      line.substring(index + ProcessOutputStrings.BRT_FILE_LOCATION_TAG.length());
    if (file == null) {
      return null;
    }
    file = file.trim();
    if (file.isEmpty()) {
      return null;
    }
    // Remove the message ID if it is at the end of the line.
    if ((index = file.indexOf(msgId)) != -1) {
      file = file.substring(0, index);
      file = file.trim();
    }
    if (file.isEmpty()) {
      return null;
    }
    return file;
  }

  void updateProgressBar() {
    updateProgressBar = false;
    setProgressBarTitle();
    manager.getMainPanel().setProgressBarValue(runKeys.getTotal(), getStatusString(),
      axisID);
  }

  private void initializeProgressBar() {
    setProgressBarTitle();
    if (reconnect) {
      manager.getMainPanel().setProgressBarValue(runKeys.getTotal(), "Reconnecting...",
        axisID);
    }
    else {
      manager.getMainPanel().setProgressBarValue(runKeys.getTotal(), "Starting...",
        axisID);
    }
  }

  public void useMessageReporter() {}

  private final void setProgressBarTitle() {
    StringBuilder title = new StringBuilder();
    if (processRunning) {
      if (killing) {
        title.append("killing ");
      }
      else if (pausing) {
        title.append("pausing ");
        if (willResume) {
          title.append("(will resume) ");
        }
      }
    }
    else if (killing) {
      title.append("killed ");
    }
    else if (pausing) {
      title.append("paused ");
      if (willResume) {
        title.append("(will resume) ");
      }
    }
    title.append(TITLE);
    if (currentDataset != null) {
      title.append(": " + currentDataset);
    }
    if (currentStep != null) {
      title.append(": " + currentStep);
    }
    manager.getMainPanel().setProgressBar(title.toString(), nDatasets, axisID, !killing);
  }

  /**
   * create commandsWriter if necessary, write command, add newline, flush
   * @param command
   * @throws IOException
   */
  private void writeCommand(final String command)
    throws LogFile.LockException, IOException {
    if (!useCommandsPipe) {
      return;
    }
    if (commandsPipe == null) {
      commandsPipe = LogFile.getInstance(FileType.CHECK_FILE.getFile(manager, axisID));
    }
    if (commandsPipeWriterId == null || commandsPipeWriterId.isEmpty()) {
      commandsPipeWriterId = commandsPipe.openWriter(true);
    }
    if (commandsPipeWriterId == null || commandsPipeWriterId.isEmpty()) {
      return;
    }
    commandsPipe.write(command, commandsPipeWriterId);
    commandsPipe.newLine(commandsPipeWriterId);
    commandsPipe.flush(commandsPipeWriterId);
    // Close writer after each write. If it is kept open, the file would not be writeable
    // from the command line in Windows.
    commandsPipe.closeWriter(commandsPipeWriterId);
    commandsPipeWriterId = null;
  }

  /**
   * Not responsible for backing up the process output file
   * @throws LogFile.LockException
   */
  private synchronized void createProcessOutput() throws LogFile.LockException {
    if (processOutput == null) {
      processOutput =
        LogFile.getInstance(FileType.BATCH_RUN_TOMO_LOG.getFile(manager, axisID));
    }
  }

  private final class StatusChangeEventSender implements Runnable {
    private final Status status;
    private final StatusChangeEvent event;

    private StatusChangeEventSender(Status status) {
      this.status = status;
      event = null;
    }

    private StatusChangeEventSender(StatusChangeEvent event) {
      status = null;
      this.event = event;
    }

    public void run() {
      if (listeners == null) {
        return;
      }
      int size = listeners.size();
      if (status != null) {
        for (int i = 0; i < size; i++) {
          listeners.get(i).statusChanged(status);
        }
      }
      else if (event != null) {
        for (int i = 0; i < size; i++) {
          listeners.get(i).statusChanged(event);
        }
      }
    }
  }
}
