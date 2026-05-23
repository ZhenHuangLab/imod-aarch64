package etomo.type;

/**
* <p>Description: The state of a dataset in the BatchRunTomo interface.  This status is
* passed by BatchRunTomoMonitor (in StatusChangeIndexedEvent).  BatchRunTomoRow responds
* to it.</p>
* 
* <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public final class BatchRunTomoDatasetState implements Status {
  public static final BatchRunTomoDatasetState DONE = new BatchRunTomoDatasetState(
    "Done", false, "Done");
  public static final BatchRunTomoDatasetState FAILED = new BatchRunTomoDatasetState(
    "Failed", false, "Failed");
  public static final BatchRunTomoDatasetState FAILING = new BatchRunTomoDatasetState(
    "Running", true, "Failing");
  public static final BatchRunTomoDatasetState KILLED = new BatchRunTomoDatasetState(
    "Killed", false, "Killed");
  public static final BatchRunTomoDatasetState RUNNING = new BatchRunTomoDatasetState(
    "Running", true, "Running");
  public static final BatchRunTomoDatasetState STARTING = new BatchRunTomoDatasetState(
    "Running", true, "Starting");
  public static final BatchRunTomoDatasetState STOPPED = new BatchRunTomoDatasetState(
    "Stopped", false, "Stopped");

  private final String text;
  private final boolean active;
  private final String key;

  private BatchRunTomoDatasetState(final String text, final boolean active,
    final String key) {
    this.text = text;
    this.active = active;
    this.key = key;
  }

  public static BatchRunTomoDatasetState getInstance(final String key) {
    if (key == null) {
      return null;
    }
    if (DONE.key.equals(key)) {
      return DONE;
    }
    if (FAILED.key.equals(key)) {
      return FAILED;
    }
    if (FAILING.key.equals(key)) {
      return FAILING;
    }
    if (KILLED.key.equals(key)) {
      return KILLED;
    }
    if (RUNNING.key.equals(key)) {
      return RUNNING;
    }
    if (STARTING.key.equals(key)) {
      return STARTING;
    }
    if (STOPPED.key.equals(key)) {
      return STOPPED;
    }
    return null;
  }

  public boolean isActive() {
    return active;
  }

  public String getText() {
    return text;
  }

  public String getKey() {
    return key;
  }

  public String toString() {
    return text;
  }

  public boolean equals(final String input) {
    return text.equals(input);
  }
}
