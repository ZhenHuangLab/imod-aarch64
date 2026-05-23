package etomo.plugin;

import etomo.ApplicationManager;
import etomo.ui.swing.GlobalExpandButton;
import etomo.ui.swing.TiltPanel;
import etomo.ui.swing.TomogramGenerationDialog;
import etomo.ui.swing.TomogramGenerationExpert;

/**
 * <p>Description:  Please read all of the information in etomo.plugin.Plugin.  This
 * interface is integrated with TomogramGenerationDialog.  It represents an alternative
 * method of creating a tomogram.  It can be used to attach one panel to
 * TomogramGenerationDialog (along-side the SIRT radio button).  It can also be used to
 * supply a customized child of TiltPanel.  An example of the implementation of this
 * interface is in etomo.plugin.demo.  This interface is based on the requirements of
 * the Ettention plugin.</p>
 * 
 * <p>The class implementing this plugin must have a standard, plugin constructor.  It
 * will be managed by TomogramGenerationExpert.  There will be one instance per axis.
 * Only one plugin of this type will be loaded by PluginFactory.  For a given axis, an
 * instance of this class will exist from the time the Tomogram Generation dialog is first
 * opened, until the dataset is closed.  A new instance of TomogramGenerationDialog is
 * constructed each time the dialog is opened, while the instance of
 * TomogramGenerationExpert remains.  It is recommended that plugin's panel be contrusted
 * in the same way.</p>
 * 
 * <p>TomogramGenerationDialog will obtain a pointer to the plugin's panel, and will
 * create a radio button to select it.  It will treat the plugin's panel as one of its
 * own.  See etomo.plugin.PluginPanel.</p>
 * 
 * <p>This plugin's functionality can be extended on demand (as practical).  Please
 * contact the developers.</p>
 * 
 * <p>This information is in addition to the information in the etomo.plugin.Plugin
 * interface. </p>
 * 
 * <h2>Tips for creating a plugin</h2>
 * <pre>
 * - Use initTomoGenMethod in addition to init to pass useful class instances to the
 *   plugin.
 * - Implement desired functionality by creating plugin-specific versions of etomo classes
 *   (see Classes below).
 * - Much functionality can remain private, and move directly between the main plugin
 *   class and the panel.  See etomo.plugin.demo.DemoPluginManager and DemoPanel.
 * - TomogramGenerationDialog will communicate directly with the plugin's panel, using the
 *   PluginPanel interface.
 * </pre>
 * 
 * <h2>Classes</h2>
 * 
 * <pre>
 * Useful classes that may be inherited by a plugin class:
 * - TiltPanel: Inherit to modify some of tilt panel fields' behavior, labels, and 
 *   tooltips.  Fields may be switched between their regular labels and tooltips and
 *   alternative ones.
 * - PluginPanel interface.
 * 
 * Classes which may not be inherited:
 * - BaseManagerClass:  only one interface manager is allowed per interface.
 * 
 * Plugin classes are inserted into these classes:
 * - BaseScreenState:  Contains of list of storables.  The plugin's screen state class
 *   can be added to this list so it can be loaded and saved at the same time as
 *   BaseScreenState.  See etomo.plugin.demo.DemoScreenState.getInstance (the call to
 *   insert).
 * </pre>
 * 
 * <h2>How specific functionality will differ in the plugin</h2>
 * 
 * <p>ScreenState:  The TomogramGenerationDialog will call
 * PluginPanel.set/getParameters(BaseScreenState) from its
 * set/getParameters(ReconScreenState) functions.  These functions can be used to retrieve
 * from and save to both BaseScreenState and the plugin's screen state class.</p>
 * 
 * <p>The process state:  The public function ApplicationManager.setProcessState affects
 * the column of buttons on the left.</p>
 *
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface TomoGenMethodPlugin extends Plugin {

  /**
   * Will be called after Plugin.init.
   */
  public void initTomoGenMethod(ApplicationManager manager,
    TomogramGenerationExpert expert);

  /**
   * Called while TomogramGenerationDialog is being constructed.  Construct a new instance
   * of the panel in this function.
   * @return
   */
  public PluginPanel getPanel(TomogramGenerationDialog parent,
    GlobalExpandButton btnAdvancedDialog);

  /**
   * Called while TomogramGenerationDialog is being constructed.  Return true if the
   * plugin returns its own instance of a TiltPanel child class.
   * @return
   */
  public boolean hasCustomTiltPanel();

  /**
   * Called while TomogramGenerationDialog is being constructed.  Return an instance
   * of a TiltPanel child class or null.
   * @return
   */
  TiltPanel getTiltPanel(TomogramGenerationDialog dialog,
    GlobalExpandButton btnAdvancedDialog);
}