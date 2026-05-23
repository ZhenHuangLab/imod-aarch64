package etomo.process;

import etomo.ApplicationManager;
import etomo.comscript.MidasParam;
import etomo.comscript.MidasParam.Mode;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.FileType;
import etomo.type.MetaData;
import etomo.type.ViewType;
import junit.framework.TestCase;

public class ProcessManagerTest extends TestCase {

  public ProcessManagerTest(String name) {
    super(name);
  }

  protected void setUp() throws Exception {
    super.setUp();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  @SuppressWarnings("deprecation")
  public void testRegressionMidasFixEdges() {
    AxisID axisID = AxisID.ONLY;
    ApplicationManager manager = new ApplicationManager("", axisID);
    MidasParam param = new MidasParam(manager, axisID, Mode.FIX_EDGES);
    param.setInputFileName(FileType.RAW_STACK.getFileName(manager, axisID));
    param.setBinning(0);
    String[] expected =
      new ProcessManager(manager).getMidasFixEdgesCommandLine("Setup Tomogram", axisID);
    String[] actual = param.getCommandArray();
    assertEquals(
      "MidasParam creates the same raw stack command as ProcessManager.midasRawStack().",
      expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(
        "MidasParam creates the same raw stack command as ProcessManager.midasRawStack().",
        expected[i].trim(), actual[i].trim());
    }
  }

  @SuppressWarnings("deprecation")
  public void testRegressionMidasRawStack() {
    AxisID axisID = AxisID.FIRST;
    ApplicationManager manager = new ApplicationManager("", axisID);
    MetaData metaData = manager.getMetaData();
    String datasetName = "TestRawStack";
    metaData.setDatasetName(datasetName);
    metaData.setAxisType(AxisType.DUAL_AXIS);
    MidasParam param = new MidasParam(manager, axisID, Mode.RAW_STACK);
    double imageRotation = 8.4;
    param.setInputFileName(FileType.RAW_STACK.getFileName(manager, axisID));
    param.setImageRotation(imageRotation);
    String[] expected = new ProcessManager(manager)
      .getMidasRawStackCommandLine(datasetName, axisID, imageRotation);
    String[] actual = param.getCommandArray();
    assertEquals(
      "MidasParam creates the same fix edges command as ProcessManager.midasRawStack().",
      expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(
        "MidasParam creates the same fix edges command as ProcessManager.midasRawStack().",
        expected[i].trim(), actual[i].trim());
    }
  }

  @SuppressWarnings("deprecation")
  public void testRegressionMidasBlendStack() {
    AxisID axisID = AxisID.SECOND;
    ApplicationManager manager = new ApplicationManager("", axisID);
    MetaData metaData = manager.getMetaData();
    String datasetName = "TestBlendStack";
    metaData.setDatasetName(datasetName);
    metaData.setAxisType(AxisType.DUAL_AXIS);
    metaData.setViewType(ViewType.MONTAGE);
    MidasParam param = new MidasParam(manager, axisID, Mode.RAW_STACK);
    double imageRotation = -4.2;
    param.setInputFileName(FileType.XCORR_BLEND_OUTPUT.getFileName(manager, axisID));
    param.setImageRotation(imageRotation);
    String[] expected = new ProcessManager(manager)
      .getMidasBlendStackCommandLine(datasetName, axisID, imageRotation);
    String[] actual = param.getCommandArray();
    assertEquals(
      "MidasParam creates the same fix edges command as ProcessManager.midasBlendStack().",
      expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(
        "MidasParam creates the same fix edges command as ProcessManager.midasBlendStack().",
        expected[i].trim(), actual[i].trim());
    }
  }
}
