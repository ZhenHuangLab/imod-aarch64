package etomo.storage;

import etomo.BaseManager;
import etomo.type.Extension;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2013 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class VolumeFileFilter extends ExtensionFileFilter {
  private VolumeFileFilter(final BaseManager manager) {
    super(manager, true, true, false);
  }

  public static VolumeFileFilter getInstance(final BaseManager manager) {
    VolumeFileFilter instance = new VolumeFileFilter(manager);
    instance.setup(null, null, null, null,
      new Extension[] { Extension.MRC, Extension.REC }, null);
    return instance;
  }

  public String getDescription() {
    return "Volume";
  }
}
