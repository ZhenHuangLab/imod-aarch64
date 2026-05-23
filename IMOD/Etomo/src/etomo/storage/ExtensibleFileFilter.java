package etomo.storage;

import java.io.File;

import etomo.BaseManager;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2011 - 2019 </p>
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
public abstract class ExtensibleFileFilter extends ExtensionFileFilter {
  public static final String rcsid = "$Id:$";

  public abstract void addExtension(File file);

  public ExtensibleFileFilter(final BaseManager manager, final boolean acceptDirectory,
    final boolean useAllFileStyles, final boolean debug) {
    super(manager, acceptDirectory, useAllFileStyles, debug);
  }
}
