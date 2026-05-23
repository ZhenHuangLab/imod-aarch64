package etomo.plugin;

import etomo.BaseManager;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.DialogType;

/**
 * <p>Description: The base interface for eTomo-compatible plugins.  External plugins must
 * have a public default constructor.</p>
 * 
 * <p>If a problem arises, most likely it will require something to be fixed in your
 * plugin, but we can help you to identify the source of the problem. </p>
 *
 * <p>This interface is the base interface for more specialized interfaces, and currently
 * not integrated with etomo, but can be on demand.  Integration points, insertion points,
 * and increases in the visibility of classes and functions are all on-demand.</p>
 * 
 * <p>Compatibility with future versions:  Public and protected classes, functions, etc
 * will be deprecated before removal.  The removal will not happen until two years after
 * the next major release has passed.  No such deprecation with be employed for private
 * and package-private items.</p>
 * 
 * <p>How to create a plugin:  In most cases the best way to create a plugin that is
 * highly integrated with etomo is to start by obtaining a private copy of the etomo code,
 * and developing your panels, classes, and functions inside the etomo packages.  Do this
 * by copying and modifying similar classes, and adding your own functions and variables
 * to etomo classes as necessary.  This gives the quickest and safest route for creating
 * something that works with etomo and is easily testable - without concern for the
 * visibility level of classes and functions.  When you are ready, you will need to
 * transfer your code to a separate package.  Functions and variables that you have added
 * to etomo classes will need to be moved to classes within your package.  In many cases
 * classes you will need to create your own, plugin-only version of the class.  See the
 * sections below for more information.  See the specific plugin interface you are
 * implementing for more information.</p>
 * 
 * <h2>Tips for creating a plugin</h2>
 * <pre>
 * - Use the init functioin to pass useful class instances to the plugin.
 * - Your main plugin class should contain manager-like functionality (see
 *   etomo.plugin.demo.DemoPluginManager).
 * - Implement desired functionality by creating plugin-specific versions of etomo classes
 *   (see Classes below).
 * - See etomo.plugin.demo!
 * </pre>
 * 
 * <h2>Classes</h2>
 * 
 * <pre>
 * Useful classes that may be inherited by a plugin class:
 * - FileType: standard file identifier.  Inherit to allow plugin file names to be used in
 *   etomo code.
 * - ProcessName: standard process names and .com file names identifier.  Inherit to allow
 *   plugin process names to be used with etomo code.  ProcessName.getInstance can return
 *   your instances of ProcessName.  See etomo.plugin.demo.DemoProcessName.
 * - BaseImodManager: for running 3dmod.
 * - BaseProcessManager: for running processes, post-process functionality, and preventing
 *   more then one process from running at the same time.
 * - AbstractProcessResultDisplayFactory: allows buttons to affect each other's state, so
 *   the user knows where they left off.
 * 
 * Classes which should not be inherited:
 * - BaseScreenState:  Unnecessary to inherit this class.
 * 
 * Plugin classes are inserted into these classes:
 * - AbstractProcessResultDisplayFactory: This is the base class for both the plugin
 *   version and the interface's version.  It contains a link list of buttons (process
 *   result displays).  The plugin instance can insert its link list into the interface's
 *   instance.  This will allow the plugin buttons to affect the standard etomo buttons
 *   and vice vera. See etomo.plugin.demo.DemoProcessResultDisplayFactory.initialize (the
 *   call to insertAfter).
 * 
 * Recommended classes:
 * - A class containing interface manager functions.
 * - Param classes: represents the parameters of a process.
 * 
 * Other useful classes
 * - EnumeratedType implementations: for managing radio buttons (see
 *   etomo.plugin.demo.DemoPanel).
 * - ComScriptManager: manages .com files
 * </pre>
 * 
 * <h2>How specific functionality will differ in the plugin</h2>
 * 
 * <p>Getting the currently selected CPUs/GPUs/etc from the parallel panel:  The public
 * function ParallelPanel.getNumberOfProcessors() is available for this purpose.</p>
 * 
 * <p>Autodocs:  Plugin autodoc files can be placed anywhere, including in
 * $IMOD_DIR/Plugins.  Use AutodocFactory.getUnmanagedInstance to retrieve them.  It is
 * not necessary to create a plugin version of AutodocFactory.</p>
 * 
 * <p>Creating a .com file:  Create a .com file by simply creating the file.  Use
 * BaseProcessManager.touch to do this.  In order to save a command into a .com file,
 * implement CommandParam in command's Param class, and use ComScriptUtil (see
 * etomo.plugin.demo.DemoComScriptManager).  When an autodoc associated with the command
 * is available (see $IMOD_DIR/autodoc), ScriptCommand.useKeyword can be called
 * from updateComScriptCommand in order to use standard input.</p>
 * 
 * <p>Creating a process series:  An instance of ProcessSeries is created as usual.  It
 * will normally only call the startNextProcess function in the interface manager.  To
 * have it call this function in the plugin, implement NextProcessTarget.  Individual
 * processes can be constructed with the NextProcessTarget instance.  A process with this
 * target set will only call the startNextProcess function in the target.  This allows
 * plugin and etomo processes to be placed together in the same series.</p>
 * 
 * <p>The process bar:  The public functions BaseManager.start/stopProgressBar
 * controls the function of the progress bar.  The progress bar must be modified directly
 * when a process monitor class is not employed.</p>
 * 
 * <p>Blocking the axis when a process is running:  It is necessary to inherit
 * BaseProcessManager to have the plugin's own processes run safely - without another
 * process potentially running at the same time.  The thread name should also be set in
 * the interface manager.  The public function BaseManager.setThreadName is available for
 * this purpose.</p>
 * 
 * <p>Other Param class interfaces:  The other Param class interfaces are not always
 * necessary.  The actual command array can be sent to some BaseProcessManager start
 * functions.  Command is the standard interface, and contains functions that return
 * general information.  ProcessDetails provides field-level information.  ComandDetails
 * combines Command and ProcessDetails.  When post-processing is used, one of these
 * interfaces is often required.</p>
 * 
 * <p>Post-processing:  To call plugin functionality when a process succeeds or fails,
 * inherit BaseProcessManager.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface Plugin {
  /**
   * @return Unique key for retrieving plugin information.
   */
  public String getKey();

  public String getTitle();

  public String getVersion();

  public String getDescription();

  /**
   * Will be called as soon as the plugin instance is constructed.
   */
  public void init(BaseManager manager, AxisID axisID, AxisType axisType,
    DialogType dialogType);

  /**
   * Called by an openDialog function.  Signal that the plugin should load data from
   * files.
   */
  public void setParameters();

  /**
   * Called by a saveDialog function.  Signal that the plugin should save data to files.
   */
  public void save();
}