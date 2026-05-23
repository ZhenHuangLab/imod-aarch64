package etomo.process;

/**
* <p>Description: </p>
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
interface Monitor extends Runnable {
  public static final String rcsid = "$Id:$";

  public boolean isPausing();

  public void setWillResume();

  /**
   * Halt the monitor as quickly as possible with a valid state, but without running
   * end-of-monitor or end-of-process functionality.  May not be implemented by all
   * monitors.
   */
  public void halt();
}
