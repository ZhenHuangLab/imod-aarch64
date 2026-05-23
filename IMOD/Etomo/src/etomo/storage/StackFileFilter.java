package etomo.storage;

import etomo.BaseManager;
import etomo.type.Extension;

/**
 * /**
 * <p>Description: Filter for the raw (input) stack. </p>
 * 
 * 
 * <p>Copyright: Copyright 2002 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 2.0  2003/01/24 20:30:31  rickg
 * <p> Single window merge to main branch
 * <p>
 * <p> Revision 1.2.2.1  2003/01/24 18:38:42  rickg
 * <p> Single window GUI layout initial revision
 * <p>
 * <p> Revision 1.2  2002/12/10 23:47:37  rickg
 * <p> Added rcsid
 * <p>
 * <p> Revision 1.1  2002/12/09 04:14:23  rickg
 * <p> Initial revision
 * <p>
 */
public class StackFileFilter extends ExtensionFileFilter {
  private StackFileFilter(final BaseManager manager, final boolean useAllFileStyles) {
    super(manager, true, useAllFileStyles, false);
  }

  public static StackFileFilter getInstance(final BaseManager manager,
    final boolean useAllFileStyles) {
    StackFileFilter instance = new StackFileFilter(manager, useAllFileStyles);
    instance.setup(null, null, null, null, new Extension[] { Extension.ST, Extension.MRC,
      Extension.HDF, Extension.TIF, Extension.TIFF }, null);
    return instance;
  }

  public String getDescription() {
    return "MRC, HDF, or TIF Image Stack";
  }
}
