package etomo.plugin.demo;

import etomo.type.FileType;

/**
 * <p>Description: Extension to FileType class.  This class should contain instances of
 * FileType rather then instances of DemoFileType.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class DemoFileType extends FileType {
  static final FileType DEMO_COMSCRIPT =
    FileType.constructInstance(false, true, "demo", ".com", "DEMOSETUP_COMSCRIPT");

  static final FileType ETOMO_DEMO_PLUGIN_AUTODOC = FileType.constructIMODDirInstance(
    false, false, DemoProcessName.ETOMO_PLUGIN_DEMO.toString(), ".adoc", "Plugins");

  private DemoFileType() {
    super();
  }
}
