package etomo.plugin.demo;

import java.io.File;

import etomo.BaseManager;
import etomo.process.BaseImodManager;
import etomo.process.ImodState;
import etomo.type.AxisID;

/**
 * <p>Description: Allows this plugin to run 3dmod.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class DemoImodManager extends BaseImodManager {
  DemoImodManager(final BaseManager manager) {
    super(manager);
  }

  public static final String DEMO_KEY = new String("Demo output files");
  private static final String demoKey = DEMO_KEY;

  protected ImodState newImodState(final String key, final String fileExtension,
    final AxisID axisID, final String datasetName, final File file,
    final String[] fileNameArray, final String subdirName, final File[] fileList) {
    if (key.equals(DEMO_KEY) && axisID != null) {
      return newDemo(file, axisID);
    }
    return null;
  }

  private ImodState newDemo(final File file, final AxisID axisID) {
    ImodState imodState = new ImodState(manager, axisID);
    return imodState;
  }
}
