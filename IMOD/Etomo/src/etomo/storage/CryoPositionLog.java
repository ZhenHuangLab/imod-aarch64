package etomo.storage;

import etomo.type.AxisID;

/**
 * <p>
 * Description: Handles high level message processing, management of other
 * high-level objects and signal routing for tomogram reconstructions.
 * </p>
 * 
 * <p>
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class CryoPositionLog {
  private final AxisID axisID;

  public CryoPositionLog(final AxisID axisID) {
    this.axisID = axisID;
  }
//TODO
  public int getBinning() {
    return 1;
  }
}
