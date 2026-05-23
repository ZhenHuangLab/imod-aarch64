package etomo.process;

import etomo.type.ProcessResultDisplay;

/**
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright 2006 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface ProcessResultDisplayFactoryInterface {
  public ProcessResultDisplay getProcessResultDisplay(int displayID, String factoryID);
}