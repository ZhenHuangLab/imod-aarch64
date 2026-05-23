package etomo.uitest;

import etomo.type.AxisID;
import etomo.type.UITestFieldType;

public class UITestAPI {
  // assertEnabled

  public static void assertEnabledButton(final boolean enabled, final String name,
    final AutodocTester autodocTester, final AxisID axisID,
    final ExternalCommandInfo commandInfo) {
    autodocTester.assertEnabled(autodocTester.findButton(UITestFieldType.BUTTON, name, 0),
      enabled,
      commandInfo.set("assertEnabledButton", enabled, name).formatCommandInfo(axisID));
  }

  public static void assertEnabledSpinner(final boolean enabled, final String name,
    final AutodocTester autodocTester, final AxisID axisID,
    final ExternalCommandInfo commandInfo) {
    autodocTester.assertEnabled(autodocTester.findSpinner(name, 0, commandInfo), enabled,
      commandInfo.set("assertEnabledSpinner", enabled, name).formatCommandInfo(axisID));
  }

  public static void assertEnabledTextField(final boolean enabled, final String name,
    final AutodocTester autodocTester, final AxisID axisID,
    final ExternalCommandInfo commandInfo) {
    autodocTester.assertEnabled(autodocTester.findTextField(name, 0), enabled,
      commandInfo.set("assertEnabledTextField", enabled, name).formatCommandInfo(axisID));
  }

  // assertEquals

  public static void assertEqualsSpinner(final String name, final String value,
    final AutodocTester autodocTester, final AxisID axisID,
    final ExternalCommandInfo commandInfo) {
    autodocTester.assertEquals(autodocTester.findSpinner(name, 0, commandInfo), value,
      commandInfo.set("assertEqualsSpinner", name, value).formatCommandInfo(axisID));
  }

  public static void assertEqualsTextField(final String name, final String value,
    final AutodocTester autodocTester, final AxisID axisID,
    final ExternalCommandInfo commandInfo) {
    autodocTester.assertEquals(autodocTester.findTextField(name, 0), value,
      commandInfo.set("assertEqualsTextField", name, value).formatCommandInfo(axisID));
  }

  // assertExists
  public static void assertExistsImageFile(final String fileName,
    final AutodocTester autodocTester, final AxisID axisID,
    final ExternalCommandInfo commandInfo) {
    autodocTester.assertExistsImageFile(fileName,
      commandInfo.set("assertExistsImageFile", fileName).formatCommandInfo(axisID));
  }

  // click
  public static void clickButton(final String name, final AutodocTester autodocTester,
    final AxisID axisID, final ExternalCommandInfo commandInfo) {
    autodocTester.click(autodocTester.findButton(UITestFieldType.BUTTON, name, 0), name,
      null, commandInfo.set("clickButton", name).formatCommandInfo(axisID));
  }

  // close
  public static void closePopup(final String title, final String closeButtonLabel,
    final AutodocTester autodocTester, final AxisID axisID,
    final ExternalCommandInfo commandInfo) {
    autodocTester.closePopup(title, closeButtonLabel,
      commandInfo.set("closePopup", title, closeButtonLabel).formatCommandInfo(axisID));
  }

  // copy
  public static void copyFile(final String fileName, final AutodocTester autodocTester,
    final AxisID axisID, final ExternalCommandInfo commandInfo) {

    autodocTester.copyFile(fileName, (String) null, false,
      commandInfo.set("copyFile", fileName).formatCommandInfo(axisID));
  }

  // pause
  public static void pause(final AutodocTester autodocTester) {
    autodocTester.pause();
  }

  // setValue

  public static void setValueFileChooser(final String title, final String fileName,
    final AutodocTester autodocTester, final AxisID axisID,
    final ExternalCommandInfo commandInfo) {
    autodocTester.setValueFileChooser(title, fileName,
      commandInfo.set("setValueFileChooser", title, fileName).formatCommandInfo(axisID));
  }

  public static void setValueTextField(final String name, final String value,
    final AutodocTester autodocTester, final AxisID axisID,
    final ExternalCommandInfo commandInfo) {
    autodocTester.setValue(autodocTester.findTextField(name, 0), value,
      commandInfo.set("setValueTextField", name, value).formatCommandInfo(axisID));
  }

  /* public static void setValueSpinner(final String name, final String value,
    final AutodocTester autodocTester, final AxisID axisID,
    final ExternalCommandInfo commandInfo) {
     autodocTester.setValue(autodocTester.findSpinner(name, 0), value,
     commandInfo.set("setValueSpinner", name, value).formatCommandInfo(axisID));
  }*/

  // variables

  public String getVariableValue(final String variableName,
    final AutodocTester autodocTester) {
    return autodocTester.getVariableValue(variableName, autodocTester.getAxisID());
  }

  public boolean isVariableSet(final String variableName,
    final AutodocTester autodocTester) {
    return autodocTester.isVariableSet(variableName, autodocTester.getAxisID());
  }

  // wait
  public void waitProcess(final String processName, final String endState,
    final AutodocTester autodocTester, final AxisID axisID,
    final ExternalCommandInfo commandInfo) {
    autodocTester.waitProcess(processName, endState,
      commandInfo.set("waitProcess", processName, endState).formatCommandInfo(axisID));
  }

  public void waitPopup(final String processName, final String buttonLabel,
    final AutodocTester autodocTester, final AxisID axisID,
    final ExternalCommandInfo commandInfo) {
    autodocTester.waitPopup(processName, buttonLabel,
      commandInfo.set("waitPopup", processName, buttonLabel).formatCommandInfo(axisID));
  }
}
