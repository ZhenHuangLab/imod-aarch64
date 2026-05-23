package etomo.uitest.tests;

import etomo.type.AxisID;
import etomo.uitest.AutodocTester;
import etomo.uitest.ExternalCommandInfo;
import etomo.uitest.UITestAPI;

final class GenericUITestSuite extends UITestAPI implements ExternalUITestSuite {
  private final AutodocTester autodocTester;
  private final AxisID axisID;

  GenericUITestSuite(final AutodocTester autodocTester) {
    this.autodocTester = autodocTester;
    axisID = autodocTester.getAxisID();
  }

  public void go() {
    ExternalCommandInfo commandInfo = new ExternalCommandInfo();
    // copy files
    String dataset = getVariableValue("dataset", autodocTester);
    String comExt = ".com";
    copyFile(dataset + "-start" + comExt, autodocTester, axisID, commandInfo);
    copyFile(dataset + "-finish" + comExt, autodocTester, axisID, commandInfo);
    // copy -001 - 011 com files
    for (int i = 1; i <= 11; i++) {
      copyFile(dataset + "-" + String.format("%03d", i) + comExt, autodocTester, axisID,
        commandInfo);
    }
    String bbaDataset = "BBa";
    copyFile(bbaDataset + ".ali", autodocTester, axisID, commandInfo);
    copyFile(bbaDataset + ".tlt", autodocTester, axisID, commandInfo);
    copyFile(bbaDataset + ".xtilt", autodocTester, axisID, commandInfo);
    copyFile(dataset + "-bound.info", autodocTester, axisID, commandInfo);
    copyFile(bbaDataset + ".zfac", autodocTester, axisID, commandInfo);
    copyFile(bbaDataset + "local.xf", autodocTester, axisID, commandInfo);
    clickButton("process-name", autodocTester, axisID, commandInfo);
    setValueFileChooser("open", dataset + "-001" + comExt, autodocTester, axisID,
      commandInfo);
    clickButton("run-parallel-process", autodocTester, axisID, commandInfo);
    waitProcess("processchunks-tilt", "done", autodocTester, axisID, commandInfo);
    // check image file name
    assertExistsImageFile(bbaDataset + "_full.rec", autodocTester, axisID, commandInfo);
  }
}
