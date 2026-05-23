package etomo;

import etomo.process.BaseProcessManager;
import etomo.storage.Storable;
import etomo.type.AxisType;
import etomo.type.BaseMetaData;
import etomo.type.ImageFilenameStyle;
import etomo.type.InterfaceType;
import etomo.type.TestMetaData;
import etomo.ui.swing.MainPanel;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2020 by the Regents of the University of Colorado</p>
* 
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
* 
* @version $Id$
*/
public final class TestManager extends BaseManager {
  public static final String DATASET_NAME = "TestDataset";

  private final TestMetaData metaData;

  public TestManager(final AxisType axisType,
    final ImageFilenameStyle imageFilenameStyle) {
    super();
    metaData = new TestMetaData(this, axisType, imageFilenameStyle, true);
  }

  public String toString() {
    return "[" + paramString() + ",metaData:" + metaData + "]";
  }

  public InterfaceType getInterfaceType() {
    return null;
  }

  void createMainPanel() {}

  public BaseMetaData getBaseMetaData() {
    return metaData;
  }

  public MainPanel getMainPanel() {
    return null;
  }

  public BaseProcessManager getProcessManager() {
    return null;
  }

  Storable[] getStorables(int offset) {
    return null;
  }

  public String getName() {
    return DATASET_NAME;
  }
}
