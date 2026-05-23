package etomo.ui.swing;

import java.awt.Component;

import javax.swing.JLabel;

final class ControlComponentModule {
  private final JLabel controlComponent = new JLabel();

  private ControlState controlState = null;

  /**
   * Constructor for a field that does not have a label.  This module creates the panel.
   * @param mainComponent
   */
  ControlComponentModule() {
    // init
    setVisible(false);
  }

  Component getComponent() {
    return controlComponent;
  }

  void setTooltip(String text) {
    controlComponent.setToolTipText(TooltipFormatter.INSTANCE.format(text));
  }

  void setVisible(final boolean visible) {
    controlComponent.setVisible(visible);
  }

  void setText(final String text) {
    controlComponent.setText(text);
  }

  boolean isOverride() {
    return controlState == ControlState.OVERRIDE;
  }

  /**
   * @param control
   * @param controlState
   * @return true if the control component is in use
   */
  public final boolean setComponentControl(boolean control,
    final ControlState controlState) {
    if (controlState == null) {
      control = false;
    }
    if (control) {
      setText(controlState.getControlString());
    }
    setVisible(control);
    return control;
  }
}
