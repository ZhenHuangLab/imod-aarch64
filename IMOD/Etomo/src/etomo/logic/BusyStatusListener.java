package etomo.logic;

import etomo.type.AxisID;

/**
 * <p>
 * Description: Listener for BusyStatusMediator.  MsgBusyStatusChanged is called when the
 * busy status has changed.  A frame is busy if its axis is busy, it has a process or a
 * series of processes running, or if a process monitor is running.</p>
 * 
 * <p>Copyright: Copyright 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface BusyStatusListener {
  public void msgBusyStatusChanged(AxisID axisID, boolean busyStatus);

  public void msgGuiBusyStatusChanged(AxisID axisID, boolean guiBusyStatus);
}
