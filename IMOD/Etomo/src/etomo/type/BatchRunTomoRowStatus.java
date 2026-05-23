package etomo.type;

/**
* <p>Description: Events coming from BatchRunTomoRow.</p>
* 
* <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
*
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public class BatchRunTomoRowStatus implements Status {
  public static final BatchRunTomoRowStatus RUN = new BatchRunTomoRowStatus("Run");

  private final String descr;

  private BatchRunTomoRowStatus(final String descr) {
    this.descr = descr;
  }

  public String getText() {
    return "Run checkbox";
  }

  public String toString() {
    return descr;
  }
}
