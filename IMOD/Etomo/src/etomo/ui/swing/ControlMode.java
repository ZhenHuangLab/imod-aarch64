package etomo.ui.swing;

import etomo.util.Utilities;

/**
 * Types of control modes.  Currently there is only one mode with state (OVERRIDE).  If
 * a second stateful mode is added, ControlTargets will need to be changed to handle this.
 * @author sueh
 *
 */
class ControlMode {
  static ControlMode CLEAR = new ControlMode("clear");
  static ControlMode SELECT_FILE =
    new ControlMode("select" + Utilities.NAME_SEPARATOR + "file");

  private final String fieldName;

  ControlMode(final String fieldName) {
    this.fieldName = fieldName;
  }

  String getFieldName() {
    return fieldName;
  }

  public String toString() {
    return fieldName;
  }
}
