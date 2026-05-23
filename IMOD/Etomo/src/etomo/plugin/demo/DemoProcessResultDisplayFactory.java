package etomo.plugin.demo;

import etomo.ApplicationManager;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.BaseScreenState;
import etomo.type.DialogType;
import etomo.type.ProcessResultDisplay;
import etomo.ui.swing.AbstractProcessResultDisplayFactory;
import etomo.ui.swing.MultiLineButton;
import etomo.ui.swing.ProcessResultDisplayFactory;
import etomo.ui.swing.Run3dmodButton;

/**
 * <p>Description: Stores and manages process result displays (buttons).  This class can
 * insert its display list into the main process result display list.  This allows the
 * displays created here to interact with etomo's buttons.  To retrieve the displays
 * created here, place getters in this class.</p>
 * 
 * <p>This class can also give each of its displays a unique ID.  This allows the buttons
 * to be retrieved generically by their saved ID.  Button IDs are saved when a process is
 * running when eTomo exits.
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class DemoProcessResultDisplayFactory extends AbstractProcessResultDisplayFactory {

  // etomoPluginDemo is a button with a right-click menu. It can run a process and then
  // cause another button to run 3dmod.
  private final ProcessResultDisplay etomoPluginDemo =
    Run3dmodButton.getDeferredToggle3dmodInstance("Run EtomoPluginDemo",
      DialogType.TOMOGRAM_GENERATION);
  private final ProcessResultDisplay unselectDependencies = MultiLineButton
    .getToggleButtonInstance("Unselect Dependencies", DialogType.TOMOGRAM_GENERATION);

  DemoProcessResultDisplayFactory(final AxisID axisID, final AxisType axisType) {
    super("etomo.plugin.demo.ProcessResultDisplayFactory"
      + (axisType == AxisType.DUAL_AXIS ? axisID.toString() : ""));
  }

  /**
   * Inserts data from this class to into ProcessResultDisplayFactory.  Call this function
   * once per axis and manager.
   * @param manager
   * @param axisID
   */
  static DemoProcessResultDisplayFactory getInstance(final ApplicationManager manager,
    final AxisID axisID, final AxisType axisType) {
    DemoProcessResultDisplayFactory instance =
      new DemoProcessResultDisplayFactory(axisID, axisType);
    instance.initialize(manager, axisID);
    return instance;
  }

  /**
   * Build one or more dependency lists and insert them into the main factory's list.
   * @see ProcessResultDisplayFactory for more information on dependency lists.
   * @param manager
   * @param axisID
   */
  private void initialize(final ApplicationManager manager, AxisID axisID) {
    // Build a global dependency list in this instance.
    int id = 0;
    addDependency(etomoPluginDemo, id++);
    addDependency(unselectDependencies, id++);

    // Insert this instance's global dependency list into the manager factory's global
    // dependency list. Placing these two buttons before the first TomoGen button.
    ProcessResultDisplayFactory mainFactory =
      manager.getProcessResultDisplayFactory(axisID);
    mainFactory.insertAfter(this, mainFactory.getUseFilteredStack());
    // Resets this instance's global list - no effect on the displays.
    reset();

    // Multiple lists can be built, added, and then reset. But each display can only be
    // used once. This is because global lists are composed of displays linked together
    // with their own next pointers.

    // The individual buttons can be modified after the dependency list was added.

    // Setting the etomoPluginDemo display to only affect the unselectDependencies
    // display. This is how the creation of temporary files is handled. The button that
    // creates the temporary file does not use the global dependecy list, and so has no
    // effect any button other then the Use... button it is paired with.

    // The unselectDependencies display will affect all displays that come after it on the
    // manager factory's global dependency list. Both buttons will be affected by displays
    // preceding them.

    // setUseGlobalDependencyList has the opposite functionality as the addDependents
    // function, which as been removed. If addDependents was used, simply the delete the
    // calls. The addDpendents functionality is now the default state.
    etomoPluginDemo.setUseGlobalDependencyList(false);
    etomoPluginDemo.addDependentDisplay(unselectDependencies);

    // Set display states
    BaseScreenState screenState = manager.getBaseScreenState(axisID);
    etomoPluginDemo.setScreenState(screenState);
    unselectDependencies.setScreenState(screenState);
  }

  ProcessResultDisplay getEtomoPluginDemo() {
    return etomoPluginDemo;
  }

  ProcessResultDisplay getUnselectDependencies() {
    return unselectDependencies;
  }
}