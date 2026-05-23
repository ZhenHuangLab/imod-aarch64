package etomo.plugin.demo;

import java.util.Properties;

import etomo.ApplicationManager;
import etomo.storage.Storable;
import etomo.type.AxisID;
import etomo.type.DialogType;
import etomo.type.PanelHeaderState;
import etomo.type.ReconScreenState;

/**
 * <p>Description: Extension to ReconScreenState class.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class DemoScreenState implements Storable {
  private final PanelHeaderState tomoGenDemoHeaderState =
    new PanelHeaderState(DialogType.TOMOGRAM_GENERATION.getStorableName() + ".Demo"
      + ReconScreenState.HEADER_GROUP);

  private DemoScreenState() {}

  /**
   * Uses ReconScreenState.insert to add an instance of this class to ReconScreenState.
   * @param manager
   * @param axisID
   */
  static DemoScreenState getInstance(final ApplicationManager manager,
    final AxisID axisID) {
    DemoScreenState instance = new DemoScreenState();
    manager.getScreenState(axisID).insert(instance);
    return instance;
  }

  public void store(Properties props) {
    store(props, "");
  }

  public void store(Properties props, String prepend) {
    tomoGenDemoHeaderState.store(props, prepend);
  }

  public void load(Properties props) {
    load(props, "");
  }

  public void load(Properties props, String prepend) {
    tomoGenDemoHeaderState.load(props, prepend);
  }
  
   PanelHeaderState getTomoGenDemoHeaderState() {
    return tomoGenDemoHeaderState;
  }
}