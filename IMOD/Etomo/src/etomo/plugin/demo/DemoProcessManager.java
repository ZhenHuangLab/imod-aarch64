package etomo.plugin.demo;

import etomo.BaseManager;
import etomo.ProcessSeries;
import etomo.comscript.ProcessDetails;
import etomo.process.AxisBusyException;
import etomo.process.BaseProcessManager;
import etomo.process.ComScriptProcess;
import etomo.type.AxisID;
import etomo.type.ProcessResultDisplay;

/**
 * <p>Description: Runs processes.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class DemoProcessManager extends BaseProcessManager {
  private final AxisID axisID;
  final DemoPluginManager pluginManager;

  DemoProcessManager(final BaseManager manager, final AxisID axisID,
    final DemoPluginManager pluginManager) {
    super(manager);
    this.axisID = axisID;
    this.pluginManager = pluginManager;
  }

  String etomoPluginDemo(final ProcessResultDisplay processResultDisplay,
    final ProcessSeries processSeries, final EtomoPluginDemoParam param)
    throws AxisBusyException {
    // Start the com script in the background
    ComScriptProcess comScriptProcess =
      startComScript(param, null, axisID, processResultDisplay, processSeries);
    return comScriptProcess.getName();
  }

  protected void postProcess(final ComScriptProcess script) {
    if (script.getProcessName() == DemoProcessName.DEMO) {
      ProcessDetails details = script.getProcessDetails();
      pluginManager.updateValues(
        details != null ? details.getIntValue(EtomoPluginDemoParam.Field.SLEEP_TIME)
          : SleepTime.DEFAULT.getValue().getInt());
    }
  }
}