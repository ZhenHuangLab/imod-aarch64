package etomo;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2012 by the Regents of the University of Colorado</p>
* 
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
* 
* @version $Id$
*/
public interface TaskInterface {
  /**
   * If true, then the user will be warned if the they exit before the task is started.
   * @return
   */
  public boolean okToDrop();
}
