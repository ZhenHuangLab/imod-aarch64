package etomo.type;

import java.util.Vector;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Organization:
 * Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
 * University of Colorado</p>
 * 
 * @author $Author$
 * 
 * @version $Revision$
 */
public final class ProcessResultDisplayState {
  public static final String rcsid =
    "$Id$";

  private final ProcessResultDisplay display;
  private Vector dependentDisplayList = null;
  private Vector failureDisplayList = null;
  private Vector successDisplayList = null;
  private int displayID = -1;
  private String factoryID = null;

  // will go back to the original state if the process failed to run
  private boolean originalState = false;
  // tells which process is current being run
  private boolean secondaryProcess = false;
  // will ignore most messages when the process is not running
  private boolean processRunning = false;
  private boolean useGlobalDependencyList = true;
  private ProcessResultDisplay next = null;

  public ProcessResultDisplayState(ProcessResultDisplay display) {
    this.display = display;
  }

  public void setOriginalState(boolean originalState) {
    this.originalState = originalState;
  }

  /**
   * Call this function when the first process starts.
   *
   */
  public final synchronized void msgProcessStarting() {
    if (!processRunning) {
      originalState = display.getOriginalState();
      // on average the process will complete, so set process done to true at the
      // beginning in case the user exits etomo.
      display.setProcessDone(true);
      secondaryProcess = false;
    }
    processRunning = true;
  }

  public final void msg(final ProcessResult processResult) {
    if (processResult == ProcessResult.SUCCEEDED) {
      msgProcessSucceeded();
    }
    else if (processResult == ProcessResult.FAILED) {
      msgProcessFailed();
    }
    else if (processResult == ProcessResult.FAILED_TO_START) {
      msgProcessFailedToStart();
    }
  }

  public void setUseGlobalDependencyList(final boolean use) {
    useGlobalDependencyList = use;
  }

  /**
   * Call this function if the final process succeeds.
   *
   */
  public void msgProcessSucceeded() {
    if (!processRunning) {
      return;
    }
    display.setProcessDone(true);
    setProcessDone(false, display);
    setProcessDone(false, dependentDisplayList);
    setProcessDone(true, successDisplayList);
    processRunning = false;
  }

  /**
   * Call this function if the final process fails.
   *
   */
  public void msgProcessFailed() {
    if (!processRunning) {
      return;
    }
    display.setProcessDone(false);
    setProcessDone(false, display);
    setProcessDone(false, dependentDisplayList);
    setProcessDone(false, failureDisplayList);
    processRunning = false;
  }

  /**
   * sets process done in the global dependency list
   * @param done
   * @param displays
   */
  private void setProcessDone(final boolean done, final ProcessResultDisplay display) {
    if (useGlobalDependencyList) {
      // Go through the general depend display list
      ProcessResultDisplay current = display.getNext();
      while (current != null) {
        current.setProcessDone(done);
        current.setOriginalState(done);
        current = current.getNext();
      }
    }
  }

  /**
   * sets process done in the ProcessResultDisplay's in displays
   * @param done
   * @param displays
   */
  private void setProcessDone(final boolean done, final Vector displayList) {
    if (displayList == null) {
      return;
    }
    for (int i = 0; i < displayList.size(); i++) {
      ProcessResultDisplay display = (ProcessResultDisplay) displayList.get(i);
      display.setProcessDone(done);
      display.setOriginalState(done);
    }
  }

  /**
   * Call this function if any process fails to start.
   * For a secondary process (any process after the first process) this is
   * equivalent to calling msgProcessFailed.
   */
  public void msgProcessFailedToStart() {
    if (!processRunning) {
      return;
    }
    if (secondaryProcess) {
      msgProcessFailed();
    }
    else {
      display.setProcessDone(originalState);
      processRunning = false;
    }
  }

  public void setID(final int displayID, final String factoryID) {
    this.displayID = displayID;
    this.factoryID = factoryID;
  }

  public boolean equalsID(final int displayID, final String factoryID) {
    // Display ID is required for equalsID to return true.
    if (this.displayID < 0 || this.displayID != displayID) {
      return false;
    }
    // Include factoryID if it is set.
    return this.factoryID == null || this.factoryID.equals(factoryID);
  }

  public int getDisplayID() {
    return displayID;
  }

  public void setFactoryID(final String input) {
    factoryID = input;
  }

  public String getFactoryID() {
    return factoryID;
  }

  public ProcessResultDisplay getNext() {
    return next;
  }

  public void setNext(final ProcessResultDisplay display) {
    next = display;
  }

  public void addDependentDisplay(final ProcessResultDisplay dependentDisplay) {
    if (dependentDisplay == null) {
      return;
    }
    if (dependentDisplayList == null) {
      dependentDisplayList = new Vector();
    }
    dependentDisplayList.add(dependentDisplay);
  }

  public void addFailureDisplay(final ProcessResultDisplay failureDisplay) {
    if (failureDisplay == null) {
      return;
    }
    if (failureDisplayList == null) {
      failureDisplayList = new Vector();
    }
    failureDisplayList.add(failureDisplay);
  }

  public void addSuccessDisplay(final ProcessResultDisplay successDisplay) {
    if (successDisplay == null) {
      return;
    }
    if (successDisplayList == null) {
      successDisplayList = new Vector();
    }
    successDisplayList.add(successDisplay);
  }

  /**
   * Call this function when a secondary process (any process after the first
   * process) starts.
   *
   */
  public void msgSecondaryProcess() {
    secondaryProcess = true;
  }

  boolean isOriginalState() {
    return originalState;
  }

   boolean isProcessRunning() {
    return processRunning;
  }

   boolean isSecondaryProcess() {
    return secondaryProcess;
  }
}
/**
 * <p> $Log$
 * <p> Revision 1.4  2006/07/26 16:38:42  sueh
 * <p> bug# 868 Added msg(ProcessResult)
 * <p>
 * <p> Revision 1.3  2006/02/06 21:18:45  sueh
 * <p> bug# 521 Changed following display to dependent display.  Added
 * <p> dependecy index and initialized.
 * <p>
 * <p> Revision 1.2  2006/01/31 20:48:32  sueh
 * <p> bug# 521 Added failureDisplayList, successDisplayList, and
 * <p> followingDisplayList to change the state of other displays when the
 * <p> process associated with the current instance succeeds or fails.
 * <p>
 * <p> Revision 1.1  2006/01/26 21:59:31  sueh
 * <p> bug# 401 Turn ProcessResultDisplay into an interface.  Place the
 * <p> functionality into ProcessResultDisplayState.  This allows a greater
 * <p> variety of classes to be ProcessResultDisplay's.
 * <p> </p>
 */
