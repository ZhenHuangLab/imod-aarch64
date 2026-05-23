package etomo.ui;

/**
 * <p>Description: Interface for a class that makes sure a field or group of fields are
 * displayed.</p>
 * <p/>
 * <p>Copyright: Copyright 2015 - 2018 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface FieldDisplayer {
  /**
   * Function to make a field or group of fields visible.  This function may be called
   * when the field is already visible.  It should do nothing in this case.
   */
  public void display();
}
