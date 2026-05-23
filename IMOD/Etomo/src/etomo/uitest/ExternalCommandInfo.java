package etomo.uitest;

import etomo.type.AxisID;

public final class ExternalCommandInfo implements UITestCommandInfo {
  private String commandInfo = null;
  private String fieldName = null;

  public ExternalCommandInfo() {}

  ExternalCommandInfo set(final String functionName, final String name,
    final String value) {
    reset();
    commandInfo = functionName + "," + name + "," + value;
    return this;
  }

  ExternalCommandInfo set(final String functionName, final boolean bool,
    final String name) {
    reset();
    commandInfo = functionName + "," + bool + "," + name;
    return this;
  }

  ExternalCommandInfo set(final String functionName, final String name) {
    reset();
    commandInfo = functionName + "," + name;
    return this;
  }

  ExternalCommandInfo setFieldCommand(final String classTag, final String functionName,
    final String fieldName) {
    reset();
    this.fieldName = fieldName;
    commandInfo = classTag + "," + functionName + "," + fieldName;
    return this;
  }

  public String toString() {
    return commandInfo;
  }

  private void reset() {
    commandInfo = null;
    fieldName = null;
  }

  public String formatCommandInfo(final AxisID axisID) {
    String string = " (" + axisID.getExtension() + ": " + commandInfo + ")";
    reset();
    return string;
  }

  public String getFieldName() {
    return fieldName;
  }
}
