package etomo.ui;

import java.io.File;

/**
 * <p>Description: For coordinating the browsing directory of multiple file chooser
 * buttons and fields.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * @see ValidDirectory
 */
public interface BrowsingDirectory {
  /**
   * @return a valid browsing directory
   */
  public File getBrowsingDir();

  /**
   * Attempts set a valid browsing directory from file.
   * @param file
   */
  public void setBrowsingDir(File file);
}
