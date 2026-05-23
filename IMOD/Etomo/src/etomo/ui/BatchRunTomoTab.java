package etomo.ui;

import etomo.util.Utilities;

/**
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright 2014 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class BatchRunTomoTab {
  public static final BatchRunTomoTab BATCH = new BatchRunTomoTab(0, "Batch Setup");
  public static final BatchRunTomoTab STACKS = new BatchRunTomoTab(1, "Stacks");
  public static final BatchRunTomoTab DATASET = new BatchRunTomoTab(2, "Dataset Values");
  public static final BatchRunTomoTab RUN = new BatchRunTomoTab(3, "Run");

  public static final int SIZE = RUN.index + 1;

  public static final BatchRunTomoTab DEFAULT = BATCH;

  private final int index;
  private final String title;

  private BatchRunTomoTab(final int index, final String title) {
    this.index = index;
    this.title = title;
  }

  public static BatchRunTomoTab getInstance(final int index) {
    if (index == BATCH.index) {
      return BATCH;
    }
    if (index == STACKS.index) {
      return STACKS;
    }
    if (index == DATASET.index) {
      return DATASET;
    }
    if (index == RUN.index) {
      return RUN;
    }
    return DEFAULT;
  }

  public String toString() {
    return title;
  }

  public String getTitle() {
    return title;
  }

  public String getQuotedLabel() {
    return Utilities.quoteLabel(title);
  }

  public int getIndex() {
    return index;
  }
}
