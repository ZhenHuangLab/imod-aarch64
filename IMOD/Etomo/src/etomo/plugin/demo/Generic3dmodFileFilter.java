package etomo.plugin.demo;

import etomo.BaseManager;
import etomo.storage.ExtensionFileFilter;
import etomo.type.Extension;

final class Generic3dmodFileFilter extends ExtensionFileFilter {
  private final Extension[] extensions = new Extension[] { Extension.ST, Extension.MRC,
    Extension.ALI, Extension.JOIN, Extension.TIF, Extension.PNG, Extension.REC,
    Extension.MOD, Extension.JPG, Extension.HDF };

  private Generic3dmodFileFilter(final BaseManager manager) {
    super(manager, true, false, false);
  }

  static Generic3dmodFileFilter getInstance(final BaseManager manager) {
    Generic3dmodFileFilter instance = new Generic3dmodFileFilter(manager);
    instance.setup(null, null, null, null,
      new Extension[] { Extension.ST, Extension.MRC, Extension.ALI, Extension.JOIN,
        Extension.TIF, Extension.PNG, Extension.REC, Extension.MOD, Extension.JPG,
        Extension.HDF, null },
      null);
    return instance;
  }

  public String getDescription() {
    return "File to open in 3dmod";
  }
}