package etomo.plugin.demo;

import java.io.File;
import java.io.IOException;

import etomo.ApplicationManager;
import etomo.BaseManager;
import etomo.ProcessSeries;
import etomo.TaskInterface;
import etomo.plugin.PluginPanel;
import etomo.plugin.TomoGenMethodPlugin;
import etomo.process.AxisBusyException;
import etomo.process.BaseProcessManager;
import etomo.process.ProcessState;
import etomo.process.SystemProcessException;
import etomo.storage.LogFile;
import etomo.storage.autodoc.AutodocFactory;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.AxisTypeException;
import etomo.type.DialogType;
import etomo.type.EtomoNumber;
import etomo.type.NextProcessTarget;
import etomo.type.ProcessResult;
import etomo.type.ProcessResultDisplay;
import etomo.type.Run3dmodMenuOptions;
import etomo.ui.UIComponent;
import etomo.ui.swing.Deferred3dmodButton;
import etomo.ui.swing.GlobalExpandButton;
import etomo.ui.swing.ProcessDisplay;
import etomo.ui.swing.TiltPanel;
import etomo.ui.swing.TomogramGenerationDialog;
import etomo.ui.swing.TomogramGenerationExpert;
import etomo.ui.swing.UIHarness;

/**
 * <p>This demo may not be ideal for copying and modifying, but it does contain
 * up to date ways of using etomo's functions and classes.  It may be better to copy and
 * modify very similar functionality in etomo, build and test your classes inside your
 * copy of etomo's packages, and then use this demo as a guide to relocating your
 * extension to a separate package.</p>
 * 
 * <p>When relocating your extension, you are likely to
 * find that functions you need are not visible outside their package.  Please contact
 * the developers about this and we will be happy to increase visibility of functions you
 * need.  Etomo is a very active project and we are trying to minimize
 * the use of deprecation, so Etomo is kept package private except as required.</p>
 * 
 * <p>Description:The manager for the plugin.  There is a separate instance for each axis.
 * This plugin creates a panel which is extremely integrated with etomo.  Functions not
 * currently required by a plugin have not been made available.  Please contact the
 * developer for more access to etomo functionality.</p>
 *
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 *
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class DemoPluginManager implements TomoGenMethodPlugin, NextProcessTarget {
  private ApplicationManager manager = null;
  private AxisID axisID = null;
  private AxisType axisType = null;
  private DemoComScriptManager comScriptMgr = null;
  // A separate process manager that shares axis blocking information with the manager's
  // process manager.
  private DemoProcessManager processMgr = null;
  private DialogType dialogType = null;
  private TomogramGenerationExpert expert = null;
  private DemoScreenState screenState = null;
  private DemoProcessResultDisplayFactory processResultDisplayFactory = null;
  private DemoPanel panel = null;
  private DemoImodManager imodManager = null;
  private ReadOnlyAutodoc autodoc = null;

  /**
   * Generic constructor required by service loader.
   */
  public DemoPluginManager() {}

  public void init(final BaseManager manager, final AxisID axisID,
    final AxisType axisType, final DialogType dialogType) {
    if (axisType == null || axisType == AxisType.NOT_SET
      || axisType == AxisType.SINGLE_AXIS) {
      this.axisID = AxisID.ONLY;
    }
    else {
      this.axisID = axisID;
    }
    if (dialogType == null) {
      this.dialogType = DialogType.TOMOGRAM_GENERATION;
    }
    else {
      this.dialogType = dialogType;
    }
    if (dialogType != DialogType.TOMOGRAM_GENERATION) {
      System.out
        .println("Error: this plugin only works with the tomogram generation dialog");
      Thread.dumpStack();
    }
    comScriptMgr = new DemoComScriptManager(manager, axisID, axisType);
    processMgr = new DemoProcessManager(manager, axisID, this);
    imodManager = new DemoImodManager(manager);
  }

  ReadOnlyAutodoc getAutodoc() throws IOException, LogFile.LockException {
    if (autodoc == null) {
      autodoc = AutodocFactory.getUnmanagedInstance(manager, axisID,
        DemoFileType.ETOMO_DEMO_PLUGIN_AUTODOC);
    }
    return autodoc;
  }

  public void initTomoGenMethod(final ApplicationManager manager,
    final TomogramGenerationExpert expert) {
    this.manager = manager;
    this.expert = expert;
    screenState = DemoScreenState.getInstance(manager, axisID);
  }

  public boolean hasCustomTiltPanel() {
    return true;
  }

  public TiltPanel getTiltPanel(final TomogramGenerationDialog dialog,
    final GlobalExpandButton btnAdvancedDialog) {
    if (manager == null) {
      return null;
    }
    return DemoTiltPanel.getInstance(manager, axisID, dialogType, btnAdvancedDialog,
      dialog);
  }

  DemoScreenState getDemoScreenState() {
    return screenState;
  }

  public PluginPanel getPanel(final TomogramGenerationDialog parent,
    final GlobalExpandButton btnAdvancedDialog) {
    if (manager == null) {
      return null;
    }
    panel =
      DemoPanel.getInstance(this, manager, axisID, dialogType, btnAdvancedDialog, parent);
    return panel;
  }

  public void setParameters() {
    if (comScriptMgr == null || panel == null) {
      return;
    }
    EtomoPluginDemoParam param;
    if (comScriptMgr.loadDemo()) {
      param = comScriptMgr.getEtomoDemoPluginParam();
    }
    else {
      // create an empty param to get the defaults.
      param = new EtomoPluginDemoParam(axisID);
    }
    panel.setParameters(param);
  }

  DemoProcessResultDisplayFactory getProcessResultDisplayFactory() {
    if (processResultDisplayFactory == null) {
      processResultDisplayFactory =
        DemoProcessResultDisplayFactory.getInstance(manager, axisID, axisType);
    }
    return processResultDisplayFactory;
  }

  public boolean startNextProcess(final UIComponent uiComponent, final AxisID axisID,
    final ProcessSeries.Process process, final ProcessResultDisplay processResultDisplay,
    final ProcessSeries processSeries, final DialogType dialogType,
    final ProcessDisplay display) {
    if (panel == null) {
      return false;
    }
    if (process.equals(Task.ETOMO_PLUGIN_DEMO)) {
      etomoPluginDemo(processResultDisplay, processSeries, null, null);
      return true;
    }
    if (process.equals(Task.UNSELECT_BUTTONS)) {
      unselectDependencies(null, processSeries);
      return true;
    }
    return false;
  }

  public void save() {
    updateEtomoPluginDemoCom(false, false);
  }

  /**
  * Updates demosetup.com. Created demosetup.com if it is not there, and required is
  * true.
  * @param axisID
  * @param display
  * @param required
  * @return
  */
  EtomoPluginDemoParam updateEtomoPluginDemoCom(final boolean required,
    final boolean doValidation) {
    if (manager == null || panel == null) {
      return null;
    }

    File demoSetupComFile = DemoFileType.DEMO_COMSCRIPT.getFile(manager, axisID);
    EtomoPluginDemoParam param = null;
    if (!demoSetupComFile.exists()) {
      if (!required) {
        return null;
      }
      // Calling makecomfile is unnecessary. Just create the file.
      BaseProcessManager.touch(demoSetupComFile.getAbsolutePath(), manager);
      comScriptMgr.loadDemo();
    }
    param = comScriptMgr.getEtomoDemoPluginParam();
    if (!panel.getParameters(param, doValidation)) {
      return null;
    }
    comScriptMgr.saveDemo(param, axisID);
    return param;
  }

  void updateValues(final int sleepTime) {
    if (panel != null) {
      panel.setSleepTimeUsed(sleepTime);
      // How to get the number of CPUs selected from the parallel panel
      // Instead of using a getParameters function, just get the number of CPUs.
      panel.setCpus(
        manager.getMainPanel().getParallelPanel(axisID).getNumberOfProcessors(false));
    }
  }

  void unselectDependencies(ProcessResultDisplay processResultDisplay,
    final ProcessSeries processSeries) {
    if (processResultDisplay == null) {
      processResultDisplay = processResultDisplayFactory.getUnselectDependencies();
    }
    manager.startProgressBar("Unsetting dependents", axisID, null);
    // Must tell the button that the process is starting.
    processResultDisplay.msgProcessStarting();
    // This causes dependent buttons to be unselected.
    processResultDisplay.msg(ProcessResult.SUCCEEDED);
    manager.stopProgressBar(axisID);
    if (processSeries != null) {
      // This continues the process series.
      processSeries.startNextProcess(axisID);
    }
  }

  /**
   * Updates an etomo com file.  Updates a demo com file.  Runs the demo com file.  Adds
   * processes to a process series.
   * @param processResultDisplay
   * @param processingMethod
   */
  void etomoPluginDemo(final ProcessResultDisplay processResultDisplay,
    ProcessSeries processSeries, final Deferred3dmodButton deferred3dmodButton,
    final Run3dmodMenuOptions run3dmodMenuOptions) {
    if (manager == null) {
      return;
    }
    if (processResultDisplay != null) {
      processResultDisplay.msgProcessStarting();
    }
    // Demonstration of using manager's functionality
    if (manager.updateTiltCom(expert.getTiltDisplay(), axisID, true) == null) {
      if (processResultDisplay != null) {
        processResultDisplay.msg(ProcessResult.FAILED_TO_START);
      }
    }
    EtomoPluginDemoParam param = updateEtomoPluginDemoCom(true, true);
    if (param == null) {
      if (processResultDisplay != null) {
        processResultDisplay.msg(ProcessResult.FAILED_TO_START);
      }
      return;
    }
    manager.setProcessState(ProcessState.INPROGRESS, axisID, dialogType);
    EtomoNumber sleepTime = new EtomoNumber();
    sleepTime.set(panel.getSleepTimeUsed());
    boolean startSeries = false;
    // The initiator of the sequence will create the processSeries.
    if (processSeries == null) {
      startSeries = true;
      processSeries = new ProcessSeries(manager, axisID, dialogType);
    }
    else if (!sleepTime.isNull() && sleepTime.gt(1)) {
      // Reduce sleep time each time etomoPluginDemo is run
      param.setSleepTime(sleepTime.getInt() / 2);
    }
    else {
      // shouldn't happen
      return;
    }
    String threadName;
    try {
      threadName = processMgr.etomoPluginDemo(processResultDisplay, processSeries, param);
    }
    catch (AxisBusyException e) {
      e.printStackTrace();
      UIHarness.INSTANCE
        .openMessageDialog(
          manager, panel, "Can not execute "
            + DemoProcessName.ETOMO_PLUGIN_DEMO.toString() + e.getMessage(),
          "Unable to execute command", axisID);
      if (processResultDisplay != null) {
        processResultDisplay.msg(ProcessResult.FAILED_TO_START);
      }
      return;
    }
    // Use processSeries to run multiple commands in sequence.
    if (startSeries) {
      String message = param.getMessage();
      if (message != null && message.toLowerCase().indexOf("unselect") != -1) {
        // ProcessSeries will use the manager's startNextProcess function, unless this
        // instance is set as the next process target.
        processSeries.setLastProcess(this, Task.UNSELECT_BUTTONS);
      }
    }
    if (param.isSleepTimeGt(1)) {
      processSeries.setNextProcess(this, Task.ETOMO_PLUGIN_DEMO);
    }
    // This will open a file using 3dmod at the end of the process series. This is done
    // with the right-click menu in the etomoPluginDemo button.
    if (deferred3dmodButton != null) {
      processSeries.setRun3dmodDeferred(deferred3dmodButton, run3dmodMenuOptions);
    }
    manager.setThreadName(threadName, axisID);
    manager.startProgressBar("Running " + DemoProcessName.ETOMO_PLUGIN_DEMO, axisID,
      DemoProcessName.ETOMO_PLUGIN_DEMO);
    return;
  }

  void open3dmod(final File file, final boolean swapYZ,
    Run3dmodMenuOptions run3dmodMenuOptions) {
    if (file == null) {
      return;
    }
    try {
      imodManager.setSwapYZ(DemoImodManager.DEMO_KEY, axisID, swapYZ);
      imodManager.setFile(DemoImodManager.DEMO_KEY, axisID, file);
      imodManager.open(DemoImodManager.DEMO_KEY, run3dmodMenuOptions);
    }
    catch (SystemProcessException e) {
      e.printStackTrace();
      UIHarness.INSTANCE.openMessageDialog(manager, panel,
        e.getMessage() + "\nCan't open " + file.getAbsolutePath(), "Cannot Open 3dmod",
        axisID);
    }
    catch (AxisTypeException e) {
      e.printStackTrace();
      UIHarness.INSTANCE.openMessageDialog(manager, panel,
        e.getMessage() + "\nCan't open " + file.getAbsolutePath(), "Cannot Open 3dmod",
        axisID);
    }
    catch (IOException e) {
      e.printStackTrace();
      UIHarness.INSTANCE.openMessageDialog(manager, panel,
        e.getMessage() + "\nCan't open " + file.getAbsolutePath(), "Cannot Open 3dmod",
        axisID);
    }
  }

  //
  // Information functions
  //

  public String getKey() {
    return "etomo.plugin.demo.DemoPluginManager";
  }

  public String getTitle() {
    return "Etomo Demo of TomoGenMethodPlugin";
  }

  public String getVersion() {
    return "1.0";
  }

  public String getDescription() {
    return "Demo of TomoGenMethodPlugin interface";
  }

  private static final class Task implements TaskInterface {
    private static final Task UNSELECT_BUTTONS = new Task("UNSELECT_BUTTONS");

    private static final Task ETOMO_PLUGIN_DEMO = new Task("ETOMO_PLUGIN_DEMO");
    private final String string;

    private Task(final String string) {
      this.string = string;
    }

    public boolean okToDrop() {
      return false;
    }

    public String toString() {
      return string;
    }
  }
}