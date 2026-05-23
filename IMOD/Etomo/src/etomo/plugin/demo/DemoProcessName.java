package etomo.plugin.demo;

import etomo.type.ProcessName;

/**
 * <p>Description: Extension to ProcessName class.  This class should contain instances of
 * ProcessName rather then instances of DemoProcessName.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class DemoProcessName extends ProcessName {
  // script name
  public static final ProcessName ETOMO_PLUGIN_DEMO =
    ProcessName.constructInstance("etomoPluginDemo");
  // comscript name
  public static final ProcessName DEMO = ProcessName.constructInstance("demo");

  private DemoProcessName() {
    super();
  }
}
