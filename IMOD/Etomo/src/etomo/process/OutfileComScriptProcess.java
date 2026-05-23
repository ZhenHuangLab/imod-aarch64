package etomo.process;

import java.io.FileNotFoundException;

import etomo.BaseManager;
import etomo.comscript.Command;
import etomo.storage.LogFile;
import etomo.type.AxisID;
import etomo.type.FileType;
import etomo.type.ProcessName;

/**
* <p>Description: Process for a comscript that is detatched and watches a file.</p>
* 
* <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
 final class OutfileComScriptProcess extends ComScriptProcess {
  private final BaseProcessManager processManager;
  private final DetachedProcessMonitor monitor;

   OutfileComScriptProcess(final BaseManager manager, final String comScript,
    final BaseProcessManager processManager, final AxisID axisID,
    final DetachedProcessMonitor monitor, final Command command, final FileType fileType,
    final ProcessName processName, final boolean reconnectWhenNotRunning,
    final ProcessData managedProcessData) {
    super(manager, comScript, processManager, axisID, monitor, command, fileType,
      processName, reconnectWhenNotRunning, managedProcessData);
    this.processManager = processManager;
    this.monitor = monitor;
  }

  /**
   * Builds the log file, prefering to use fileType instead of the processName.  Either
   * fileType or getProcessName() must be non-null.
   */
  LogFile buildLogFile(final FileType fileType) {
    if (fileType != null) {
      return buildLogFileFromFileType(fileType);
    }
    return buildLogFileFromProcessName();
  }

  /**
   * Need this so the right msgComScriptDone function can be called.
   */
  void runMsgComScriptDone(final int exitValue) {
    try {
      // Wait for the monitor to complete.
      while (monitor.isProcessRunning()) {
        Thread.sleep(100);
      }
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    processManager.msgComScriptDone(this, exitValue, getNonBlocking());
  }

  ProcessMessages getMonitorProcessMessages() {
    if (monitor == null) {
      return null;
    }
    return monitor.getProcessMessages();
  }

  public final void kill(AxisID axisID) {
    monitor.kill(this, axisID);
  }

  public void signalKill(AxisID axisID) {}

  public final void pause(AxisID axisID) {
    monitor.pause(this, axisID);
  }

  final String getStatusString() {
    return monitor.getStatusString();
  }

  /**
   * Do not parse the log file for warnings.
   */
  void parse(String name, boolean mustExist) throws LogFile.LockException,
    FileNotFoundException {}
}
