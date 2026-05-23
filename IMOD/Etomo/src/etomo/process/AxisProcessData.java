package etomo.process;

import java.util.HashMap;

import etomo.BaseManager;
import etomo.logic.BusyStatusMediator;
import etomo.type.AxisID;

/**
* <p>Description: Data used by the process managers.  This class has a one-to-one
* relationship with BaseManager.</p>
* 
* <p>Copyright: Copyright 2012</p>
*
* <p>Organization:
* Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
* University of Colorado</p>
* 
* @author $Author$
* 
* @version $Revision$
* 
* <p> $Log$ </p>
*/
public final class AxisProcessData {
  public static final String rcsid = "$Id:$";

  private final HashMap killedList = new HashMap();

  private final ProcessData savedProcessDataA;
  private final ProcessData savedProcessDataB;
  private final BusyStatusMediator busyStatusMediator;

  private SystemProcessInterface threadAxisA = null;
  private SystemProcessInterface threadAxisB = null;
  private Thread processMonitorThreadA = null;
  private Thread processMonitorThreadB = null;
  private boolean blockAxisA = true;
  private boolean blockAxisB = true;
  private Monitor monitorA = null;
  private Monitor monitorB = null;

  void dumpState() {
    System.err.println("[processMonitorThreadA:" + processMonitorThreadA
      + ",processMonitorThreadB:" + processMonitorThreadB + ",killedList:");
    if (killedList != null) {
      System.err.println(killedList.toString());
    }
    System.err.println(",blockAxisA:" + blockAxisA + ",blockAxisB:" + blockAxisB + "]");
  }

  public AxisProcessData(final BaseManager manager) {
    savedProcessDataA = new ProcessData(AxisID.FIRST, manager);
    savedProcessDataB = new ProcessData(AxisID.SECOND, manager);
    busyStatusMediator = manager.getBusyStatusMediator();
  }

  public String toString() {
    return "threadAxisA=" + threadAxisA + ",threadAxisB=" + threadAxisB
      + ",\nprocessMonitorThreadA=" + processMonitorThreadA + ",processMonitorThreadB="
      + processMonitorThreadB + ",\nkilledList=" + killedList;
  }

  boolean isThreadAxisNull(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return threadAxisB == null;
    }
    return threadAxisA == null;
  }

  public boolean isPausing(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      if (monitorB != null) {
        return monitorB.isPausing();
      }
    }
    if (monitorA != null) {
      return monitorA.isPausing();
    }
    return false;
  }

  public void setWillResume(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      monitorB.setWillResume();
    }
    else {
      monitorA.setWillResume();
    }
  }

  /**
   * Save the process thread reference for the appropriate axis
   * 
   * @param thread
   * @param axisID
   */
  void mapAxisThread(final SystemProcessInterface thread, final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      threadAxisB = thread;
    }
    else {
      threadAxisA = thread;
    }
    busyStatusMediator.msgThreadChanged(axisID, thread != null);
  }

  public SystemProcessInterface getThread(final AxisID axisID) {
    SystemProcessInterface thread = null;
    if (axisID == AxisID.SECOND) {
      thread = threadAxisB;
    }
    else {
      thread = threadAxisA;
    }
    return thread;
  }

  /**
   * Save the process monitor thread reference for the appropriate axis
   * 
   * @param processMonitor
   * @param axisID
   */
  void mapAxisProcessMonitor(final Thread processMonitorThread, final Monitor monitor,
    final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      processMonitorThreadB = processMonitorThread;
      monitorB = monitor;
    }
    else {
      processMonitorThreadA = processMonitorThread;
      monitorA = monitor;
    }
  }

  /**
   * Tells the monitor to halt.  If the monitor has this functionality, it halts with a
   * valid state, without running end-of-monitor functionality.  Waits for monitor thread
   * to end.
   * @param axisID
   */
  void haltMonitorThread(final AxisID axisID) {
    try {
      if (axisID == AxisID.SECOND) {
        if (monitorB != null) {
          monitorB.halt();
          if (processMonitorThreadB != null) {
            processMonitorThreadB.join();
          }
        }
      }
      else {
        if (monitorA != null) {
          monitorA.halt();
          if (processMonitorThreadA != null) {
            processMonitorThreadA.join();
          }
        }
      }
    }
    catch (InterruptedException e) {}
  }

  void clearThread(final ComScriptProcess script) {
    // Null out the correct thread
    // Interrupt the process monitor and nulll out the appropriate references
    if (threadAxisA == script) {
      if (processMonitorThreadA != null) {
        processMonitorThreadA.interrupt();
        processMonitorThreadA = null;
      }
      monitorA = null;
      threadAxisA = null;
      busyStatusMediator.msgThreadChanged(AxisID.ONLY, false);
    }
    if (threadAxisB == script) {
      if (processMonitorThreadB != null) {
        processMonitorThreadB.interrupt();
        processMonitorThreadB = null;
      }
      monitorB = null;
      threadAxisB = null;
      busyStatusMediator.msgThreadChanged(AxisID.SECOND, false);
    }
  }

  void clearThread(final ReconnectProcess script) {
    // Null out the correct thread
    // Interrupt the process monitor and nulll out the appropriate references
    if (threadAxisA == script) {
      if (processMonitorThreadA != null) {
        processMonitorThreadA.interrupt();
        processMonitorThreadA = null;
      }
      monitorA = null;
      threadAxisA = null;
      busyStatusMediator.msgThreadChanged(AxisID.ONLY, false);
    }
    if (threadAxisB == script) {
      if (processMonitorThreadB != null) {
        processMonitorThreadB.interrupt();
        processMonitorThreadB = null;
      }
      monitorB = null;
      threadAxisB = null;
      busyStatusMediator.msgThreadChanged(AxisID.SECOND, false);
    }
  }

  void clearThread(final DetachedProcess process) {
    // Null the reference to the appropriate thread
    if (process == threadAxisA) {
      threadAxisA = null;
      busyStatusMediator.msgThreadChanged(AxisID.ONLY, false);
    }
    if (process == threadAxisB) {
      threadAxisB = null;
      busyStatusMediator.msgThreadChanged(AxisID.SECOND, false);
    }
  }

  void clearThread(final BackgroundProcess process) {
    // Null the reference to the appropriate thread
    if (process == threadAxisA) {
      threadAxisA = null;
      busyStatusMediator.msgThreadChanged(AxisID.ONLY, false);
    }
    if (process == threadAxisB) {
      threadAxisB = null;
      busyStatusMediator.msgThreadChanged(AxisID.SECOND, false);
    }
  }

  public ProcessData getSavedProcessData(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return savedProcessDataB;
    }
    return savedProcessDataA;
  }

  void putKilledList(final String processID) {
    killedList.put(processID, "");
  }

  boolean containsKeyKilledList(final String pidField) {
    return killedList.containsKey(pidField);
  }

  boolean isBlockAxis(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return blockAxisB;
    }
    return blockAxisA;
  }

  void setBlockAxis(final AxisID axisID, final boolean input) {
    if (axisID == AxisID.SECOND) {
      blockAxisB = input;
    }
    else {
      blockAxisA = input;
    }
  }
}
