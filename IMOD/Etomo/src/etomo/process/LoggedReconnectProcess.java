package etomo.process;

import etomo.BaseManager;
import etomo.ProcessSeries;
import etomo.storage.LogFile;
import etomo.type.AxisID;
import etomo.type.ConstStringProperty;

/**
 * <p>Description: </p>
 * 
* <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
 */
 class LoggedReconnectProcess extends ReconnectProcess {
  LoggedReconnectProcess(final BaseManager manager,
    final BaseProcessManager processManager, final ProcessMonitor monitor,
    final ProcessData processData, final AxisID axisID, final String logFileName,
    final String logSuccessTag, final ConstStringProperty subDirName,
    final ProcessSeries processSeries, final boolean popupChunkWarnings,
    final boolean reconnectWhenNotRunning) throws LogFile.LockException {
    super(manager, processManager, monitor, processData, axisID, processSeries,
      popupChunkWarnings, reconnectWhenNotRunning);
    initLogInstance(logFileName, logSuccessTag, subDirName);
  }

  void msgDone(final int exitValue) {
    processManager.msgReconnectDone(this, exitValue, popupChunkWarnings);
  }

  ProcessMessages getProcessMessages() {
    if (monitor != null) {
      return monitor.getProcessMessages();
    }
    return super.getProcessMessages();
  }
}
