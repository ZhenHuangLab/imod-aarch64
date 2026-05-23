package etomo.type;

/**
* <p>Description: The states of the BatchRunTomo interface.  This status is passed by
* BatchRunTomoMonitor and BatchRunTomoDialog.  BatchRunTomoDialog, BatchRunTomoStepPanel,
* BatchRunTomoTable, BatchRunTomoTable.RowList, and BatchRunTomoRow respond to it.</p>
* 
* <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public final class BatchRunTomoStatus implements Status {
  public static final BatchRunTomoStatus OPEN = new BatchRunTomoStatus("Open");
  public static final BatchRunTomoStatus RUNNING = new BatchRunTomoStatus("Running");
  public static final BatchRunTomoStatus KILLED_PAUSED = new BatchRunTomoStatus(
    "Killed/Paused");
  public static final BatchRunTomoStatus STOPPED = new BatchRunTomoStatus("Stopped");

  public static final BatchRunTomoStatus DEFAULT = OPEN;

  private final String text;

  private BatchRunTomoStatus(final String text) {
    this.text = text;
  }

  public static BatchRunTomoStatus getInstance(final String text) {
    if (text == null) {
      return DEFAULT;
    }
    if (OPEN.text.equals(text)) {
      return OPEN;
    }
    if (RUNNING.text.equals(text)) {
      return RUNNING;
    }
    if (KILLED_PAUSED.text.equals(text)) {
      return KILLED_PAUSED;
    }
    if (STOPPED.text.equals(text)) {
      return STOPPED;
    }
    return DEFAULT;
  }

  public String getText() {
    return text;
  }

  public String toString() {
    return text;
  }
}
