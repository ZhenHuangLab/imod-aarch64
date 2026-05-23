package etomo.ui.swing;

import etomo.ui.SharedStrings;

final class ControlState extends ControlMode {
  static ControlState OVERRIDE =
    new ControlState("override", SharedStrings.OVERRIDE_TEXT, DisplayType.OVERRIDE);
  static ControlState ENABLE = new ControlState("enable", null, DisplayType.ENABLE);

  private final String controlString;
  private final DisplayType displayType;

  ControlState(final String fieldName, final String controlString,
    final DisplayType displayType) {
    super(fieldName);
    this.controlString = controlString;
    this.displayType = displayType;
  }

  boolean isComponentDisplay() {
    return displayType == DisplayType.OVERRIDE;
  }

  boolean isEnableDisplay() {
    return displayType == DisplayType.ENABLE;
  }

  String getControlString() {
    return controlString;
  }

  private static final class DisplayType {
    private static final DisplayType OVERRIDE = new DisplayType();
    private static final DisplayType ENABLE = new DisplayType();
  }
}
