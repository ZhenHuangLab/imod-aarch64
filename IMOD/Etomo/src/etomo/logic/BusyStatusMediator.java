package etomo.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import etomo.type.AxisID;

/**
 * <p>
 * Description: Mediator for busy status of a frame.  A frame is busy if its axis is busy,
 * it has a process or a series of processes running, or if a process monitor is running.
 * </p>
 * 
 * <p>Copyright: Copyright 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class BusyStatusMediator {
  List<BusyStatusListener> listeners = new ArrayList<BusyStatusListener>();

  private boolean busyStatusA = false;
  private boolean busyStatusB = false;
  private boolean guiBusyStatusA = false;
  private boolean guiBusyStatusB = false;
  private boolean processSeriesA = false;
  private boolean processSeriesB = false;
  private boolean threadA = false;
  private boolean threadB = false;
  private boolean monitorA = false;
  private boolean monitorB = false;
  private boolean processRunA = false;
  private boolean processRunB = false;
  private boolean killProcessButtonA = false;
  private boolean killProcessButtonB = false;
  private boolean packingA = false;
  private boolean packingB = false;
  private boolean fileChooserBuildA = false;
  private boolean fileChooserBuildB = false;

  public BusyStatusMediator() {}

  public void addBusyStatusListener(final BusyStatusListener listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  public void removeBusyStatusListener(final BusyStatusListener listener) {
    if (listener != null) {
      listeners.remove(listener);
    }
  }

  /**
   * Called when ProcessSeries constructed to run a set of processes.
   * @param axisID
   */
  public void msgProcessSeriesConstructed(final AxisID axisID) {
    updateProcessSeries(axisID, true);
  }

  /**
   * Called when ProcessSeries attempts to run a null process.
   * @param axisID
   */
  public void msgProcessSeriesDone(final AxisID axisID) {
    updateProcessSeries(axisID, false);
  }

  /**
   * Called when process is constructed to run a set of processes.
   * @param axisID
   */
  public void msgProcessConstructed(final AxisID axisID) {
    updateProcessRun(axisID, true);
  }

  /**
   * Called when processDone finished .
   * @param axisID
   */
  public void msgProcessDone(final AxisID axisID) {
    updateProcessRun(axisID, false);
  }

  /**
   * Called when a saved process thread has changed.
   * @param axisID
   * @param busy
   */
  public void msgThreadChanged(final AxisID axisID, final boolean busy) {
    if (busy == mapThread(axisID)) {
      return;
    }
    setThread(axisID, busy);
    updateBusyStatus(axisID);
  }

  public void msgMonitorConstructed(final AxisID axisID) {
    updateMonitor(axisID, true);
  }

  public void msgKillProcessButton(final AxisID axisID, final boolean enabled) {
    updateKillProcessButton(axisID, enabled);
  }

  public void msgPacking(final AxisID axisID, final boolean on) {
    updatePacking(axisID, on);
  }

  public void msgFileChooserBuild(final AxisID axisID, final boolean inProgress) {
    updateFileChooserBuild(axisID, inProgress);
  }

  public void msgMonitorStopped(final AxisID axisID) {
    updateMonitor(axisID, false);
  }

  private void updateMonitor(final AxisID axisID, final boolean busy) {
    if (busy == mapMonitor(axisID)) {
      return;
    }
    setMonitor(axisID, busy);
    updateBusyStatus(axisID);
  }

  private void updateKillProcessButton(final AxisID axisID, final boolean enabled) {
    if (enabled == mapKillProcessButton(axisID)) {
      return;
    }
    setKillProcessButton(axisID, enabled);
    updateBusyStatus(axisID);
  }

  private void updatePacking(final AxisID axisID, final boolean on) {
    if (on == mapPacking(axisID)) {
      return;
    }
    setPacking(axisID, on);
    updateGuiBusyStatus(axisID);
  }

  private void updateFileChooserBuild(final AxisID axisID, final boolean inProgress) {
    if (inProgress == mapFileChooserBuild(axisID)) {
      return;
    }
    setFileChooserBuild(axisID, inProgress);
    updateGuiBusyStatus(axisID);
  }

  /**
   * Updates processSeries and calls updateBusyStatus.
   * @param axisID
   * @param busy
   */
  private void updateProcessSeries(final AxisID axisID, final boolean busy) {
    if (busy != mapProcessSeries(axisID)) {
      setProcessSeries(axisID, busy);
      updateBusyStatus(axisID);
    }
  }

  /**
  * Updates process and calls updateBusyStatus.
  * @param axisID
  * @param busy
  */
  private void updateProcessRun(final AxisID axisID, final boolean busy) {
    if (busy == mapProcessRun(axisID)) {
      return;
    }
    setProcessRun(axisID, busy);
    updateBusyStatus(axisID);
  }

  private boolean calcBusyStatus(final AxisID axisID) {
    return mapProcessSeries(axisID) || mapThread(axisID) || mapMonitor(axisID)
      || mapProcessRun(axisID) || mapKillProcessButton(axisID);
  }

  private boolean calcGuiBusyStatus(final AxisID axisID) {
    return mapPacking(axisID) || mapFileChooserBuild(axisID);
  }

  /**
   * If busyStatus is changed then update it and tell the listeners.
   * @param axisID
   */
  private void updateBusyStatus(final AxisID axisID) {
    // If at least one of processSeries, Thread, Monitor, etc is on, then the busy
    // status is true. If all are not on, then the busy status is false.
    boolean busyStatus = calcBusyStatus(axisID);
    if (busyStatus != mapBusyStatus(axisID)) {
      setBusyStatus(axisID, busyStatus);
      Iterator<BusyStatusListener> iterator = listeners.iterator();
      while (iterator.hasNext()) {
        iterator.next().msgBusyStatusChanged(axisID, busyStatus);
      }
    }
  }

  /**
   * If guiBusyStatus is changed then update it and tell the listeners.
   * @param axisID
   */
  private void updateGuiBusyStatus(final AxisID axisID) {
    // If packing or fileChooserBuild on, then the GUI busy
    // status is true. If all are not on, then the GUI busy status is false.
    boolean guiBusyStatus = calcGuiBusyStatus(axisID);
    if (guiBusyStatus != mapGuiBusyStatus(axisID)) {
      setGuiBusyStatus(axisID, guiBusyStatus);
      Iterator<BusyStatusListener> iterator = listeners.iterator();
      while (iterator.hasNext()) {
        iterator.next().msgGuiBusyStatusChanged(axisID, guiBusyStatus);
      }
    }
  }

  private boolean mapProcessSeries(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return processSeriesB;
    }
    return processSeriesA;
  }

  private boolean mapProcessRun(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return processRunB;
    }
    return processRunA;
  }

  private void setProcessSeries(final AxisID axisID, final boolean busy) {
    if (axisID == AxisID.SECOND) {
      processSeriesB = busy;
    }
    else {
      processSeriesA = busy;
    }
  }

  private void setProcessRun(final AxisID axisID, final boolean busy) {
    if (axisID == AxisID.SECOND) {
      processRunB = busy;
    }
    else {
      processRunA = busy;
    }
  }

  private boolean mapMonitor(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return monitorB;
    }
    return monitorA;
  }

  private boolean mapKillProcessButton(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return killProcessButtonB;
    }
    return killProcessButtonA;
  }

  private boolean mapPacking(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return packingB;
    }
    return packingA;
  }

  private boolean mapFileChooserBuild(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return fileChooserBuildB;
    }
    return fileChooserBuildA;
  }

  private void setMonitor(final AxisID axisID, final boolean busy) {
    if (axisID == AxisID.SECOND) {
      monitorB = busy;
    }
    else {
      monitorA = busy;
    }
  }

  private void setKillProcessButton(final AxisID axisID, final boolean enabled) {
    if (axisID == AxisID.SECOND) {
      killProcessButtonB = enabled;
    }
    else {
      killProcessButtonA = enabled;
    }
  }

  private void setPacking(final AxisID axisID, final boolean on) {
    if (axisID == AxisID.SECOND) {
      packingB = on;
    }
    else {
      packingA = on;
    }
  }

  private void setFileChooserBuild(final AxisID axisID, final boolean inProgress) {
    if (axisID == AxisID.SECOND) {
      fileChooserBuildB = inProgress;
    }
    else {
      fileChooserBuildA = inProgress;
    }
  }

  private boolean mapThread(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return threadB;
    }
    return threadA;
  }

  private void setThread(final AxisID axisID, final boolean busy) {
    if (axisID == AxisID.SECOND) {
      threadB = busy;
    }
    else {
      threadA = busy;
    }
  }

  private boolean mapBusyStatus(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return busyStatusB;
    }
    return busyStatusA;
  }

  private boolean mapGuiBusyStatus(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return guiBusyStatusB;
    }
    return guiBusyStatusA;
  }

  private void setBusyStatus(final AxisID axisID, final boolean busy) {
    if (axisID == AxisID.SECOND) {
      busyStatusB = busy;
    }
    else {
      busyStatusA = busy;
    }
  }

  private void setGuiBusyStatus(final AxisID axisID, final boolean busy) {
    if (axisID == AxisID.SECOND) {
      guiBusyStatusB = busy;
    }
    else {
      guiBusyStatusA = busy;
    }
  }
}