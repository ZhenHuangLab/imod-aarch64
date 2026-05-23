package etomo.type;

/**
* <p>Description: Miscellaneous dataset status values.</p>
* 
* <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public class BatchRunTomoDatasetStatus implements Status {
  public static final BatchRunTomoDatasetStatus DELIVERED =
    new BatchRunTomoDatasetStatus("Delivered");
  public static final BatchRunTomoDatasetStatus RENAMED =
    new BatchRunTomoDatasetStatus("Renamed");
  private final String descr;

  private BatchRunTomoDatasetStatus(final String descr) {
    this.descr = descr;
  }

  public String getText() {
    return null;
  }

  public String toString() {
    return descr;
  }
}
