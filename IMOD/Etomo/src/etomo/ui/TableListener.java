package etomo.ui;

import java.util.EventListener;
import java.util.EventObject;

/**
 * <p>Description: Listener for table events.</p>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface TableListener extends EventListener {

  public void lastRowDeleted(EventObject event);

  public void firstRowAdded(EventObject event);
}
