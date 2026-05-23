package etomo.storage;

import java.io.File;

import etomo.storage.autodoc.AutodocFactory;

/**
 * <p>Description: Default autodoc extension file filter</p>
 * 
 * <p>Copyright: Copyright 2005 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */

public class AutodocFilter extends javax.swing.filechooser.FileFilter implements
    java.io.FileFilter {
  private final boolean excludeHidden;

  public AutodocFilter() {
    excludeHidden = false;
  }

  public AutodocFilter(final boolean excludeHidden) {
    this.excludeHidden = excludeHidden;
  }

  public boolean accept(File f) {
    if (!f.exists()) {
      System.err.println("Warning: " + f.getAbsolutePath() + " does not exist");
      return false;
    }
    if (f.isFile()) {
      String name = f.getName();
      return name.endsWith(AutodocFactory.Extension.DEFAULT.toString())
          && (!excludeHidden || !name.startsWith("."));
    }
    return true;
  }

  public String getDescription() {
    return "Autodoc file";
  }
}
/**
 * <p> $Log$
 * <p> Revision 1.1  2005/12/23 02:06:08  sueh
 * <p> bug# 675 Added a filter to find autodoc files.
 * <p> </p>
 */
