package etomo.storage;

import java.util.Properties;

/**
 * <p>Description: Defines the methods necessary to save and store data objects</p>
 *
 * <p>Copyright: Copyright 2002 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 3.0  2003/11/07 23:19:01  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.0  2003/01/24 20:30:31  rickg
 * <p> Single window merge to main branch
 * <p>
 * <p> Revision 1.1  2002/09/09 22:57:02  rickg
 * <p> Initial CVS entry, basic functionality not including combining
 * <p> </p>
 */

public interface Storable {
  /**
   * The store methods expects the object to add the key/element pairs to the
   * Properties object passed.
   */
  public void store(Properties props);

  public void store(Properties props, String prepend);

  /**
   * The load method provides a loaded Properties object so that the object can
   * get its stored data through the getProperty call
   */
  public void load(Properties props);

  public void load(Properties props, String prepend);
}
