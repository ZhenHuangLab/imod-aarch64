package etomo.process;

import etomo.BatchRunTomoManager;
import etomo.comscript.BatchruntomoParam;
import etomo.storage.LogFile;
import etomo.type.AxisID;
import etomo.type.CurrentArrayList;
import etomo.type.FileType;
import etomo.type.ProcessName;
import etomo.ui.swing.UIHarness;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2014 - 2015 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public final class BatchRunTomoProcessManager extends BaseProcessManager {
  private static final AxisID AXID_ID = AxisID.ONLY;

  private final BatchRunTomoManager manager;

  public BatchRunTomoProcessManager(final BatchRunTomoManager manager) {
    super(manager);
    this.manager = manager;
  }

  /**
   * @param param
   * @param runKeys - array of keys to the rows involved with this run
   */
  public String batchruntomo(final BatchruntomoParam param,
    final CurrentArrayList<String> runKeys, final ProcessMessages messages)
    throws AxisBusyException {
    if (param == null) {
      return null;
    }
    String command = FileType.BATCH_RUN_TOMO_COMSCRIPT.getFileName(manager, AXID_ID);
    ProcessData managedProcessData =
      ProcessData.getManagedInstance(AXID_ID, manager, ProcessName.BATCHRUNTOMO);
    BatchRunTomoProcessMonitor monitor = BatchRunTomoProcessMonitor.getInstance(manager,
      AXID_ID, runKeys, managedProcessData, messages);
    // Start the com script in the background
    ComScriptProcess comScriptProcess = startOutfileComScript(command, monitor, AXID_ID,
      param, FileType.BATCH_RUN_TOMO_COMSCRIPT, ProcessName.BATCHRUNTOMO, true,
      managedProcessData);
    return comScriptProcess.getName();
  }

  public boolean reconnectBatchruntomo(final ProcessData processData,
    final ProcessMessages messages) {
    BatchRunTomoProcessMonitor monitor = BatchRunTomoProcessMonitor
      .getReconnectInstance(manager, AXID_ID, processData, messages);
    boolean ret;
    try {
      ReconnectProcess process = new LoggedReconnectProcess(manager, this, monitor,
        axisProcessData.getSavedProcessData(AXID_ID), AXID_ID, monitor.getLogFileName(),
        ProcessOutputStrings.BRT_SUCCESS_TAG, null, null, false, true);
      monitor.setProcess(process);
      Thread thread = new Thread(process);
      thread.start();
      axisProcessData.mapAxisThread(process, AXID_ID);
      axisProcessData.mapAxisProcessMonitor(null, monitor, AXID_ID);
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
      UIHarness.INSTANCE.openMessageDialog(manager,
        "Unable to reconnect to processchunks.\n" + e.getMessage(), "Reconnect Failure",
        AXID_ID);
      return false;
    }
    return true;
  }

  public void haltMonitorThread() {
    axisProcessData.haltMonitorThread(AXID_ID);
  }
}
