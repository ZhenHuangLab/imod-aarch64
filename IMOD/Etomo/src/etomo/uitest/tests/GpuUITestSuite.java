package etomo.uitest.tests;

import etomo.type.AxisID;
import etomo.uitest.AutodocTester;
import etomo.uitest.ExternalCommandInfo;
import etomo.uitest.UITestAPI;

final class GpuUITestSuite extends UITestAPI implements ExternalUITestSuite {
  private final AutodocTester autodocTester;
  private final AxisID axisID;

  GpuUITestSuite(final AutodocTester autodocTester) {
    this.autodocTester = autodocTester;
    axisID = autodocTester.getAxisID();
  }

  public void go() {
    ExternalCommandInfo commandInfo = new ExternalCommandInfo();
    String numberOfMinutesField = "#-of-minutes";
    String runGpuTestButton = "run-gpu-test";
    //
    if (isVariableSet("test-gui", autodocTester)) {
      String numberOfMinutesValue = "1";
      String gpuNumberField = "gpu-#";
      // Test values
      assertEqualsTextField(numberOfMinutesField, numberOfMinutesValue, autodocTester,
        axisID, commandInfo);
      assertEqualsSpinner(gpuNumberField, "0", autodocTester, axisID, commandInfo);
      // check enabled/disabled
      assertEnabledTextField(true, numberOfMinutesField, autodocTester, axisID,
        commandInfo);
      assertEnabledSpinner(true, gpuNumberField, autodocTester, axisID, commandInfo);
      assertEnabledButton(true, runGpuTestButton, autodocTester, axisID, commandInfo);
      // test field validation
      setValueTextField(numberOfMinutesField, "1abc", autodocTester, axisID, commandInfo);
      clickButton(runGpuTestButton, autodocTester, axisID, commandInfo);
      closePopup("field-validation-failed", "OK", autodocTester, axisID, commandInfo);
      assertEnabledButton(false, "kill-process", autodocTester, axisID, commandInfo);
      // Reset fields
      setValueTextField(numberOfMinutesField, numberOfMinutesValue, autodocTester, axisID,
        commandInfo);
    }
    // tf.#-of-minutes=.5
    setValueTextField(numberOfMinutesField, ".5", autodocTester, axisID, commandInfo);
    // bn.run-gpu-test=
    clickButton(runGpuTestButton, autodocTester, axisID, commandInfo);
    // wait.popup.gputilttest-succeeded=OK
    waitPopup("gputilttest-succeeded", "OK", autodocTester, axisID, commandInfo);
    // wait.process.running-gputilttest=done
    waitProcess("running-gputilttest", "done", autodocTester, axisID, commandInfo);
    // sp.gpu-#=8

  }
}
