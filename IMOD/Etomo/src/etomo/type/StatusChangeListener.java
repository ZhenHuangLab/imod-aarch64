package etomo.type;

/**
* <p>Description: Listens for status changes.</p>
* 
* <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public interface StatusChangeListener {
  public void statusChanged(Status status);

  public void statusChanged(StatusChangeEvent statusChangeEvent);
}
