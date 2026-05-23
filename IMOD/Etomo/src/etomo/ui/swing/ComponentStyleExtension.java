package etomo.ui.swing;

import java.awt.Color;
import java.awt.Component;

import etomo.ui.FlagType;

final class ComponentStyleExtension {
  static ComponentStyleExtension INSTANCE = new ComponentStyleExtension();

  private ComponentStyleExtension() {}

  /**
   * Changes the foreground color based on the flag type and whether the field is enabled.
   * @param component
   * @param flagType
   * @param defaultForeground
   * @param defaultBackground
   */
  void updateAppearance(final Component component, final FlagType flagType,
    final Color defaultForeground, final Color defaultBackground) {
    if (component == null) {
      return;
    }
    //Remove the flag color when there is no flag or the component is disabled.
    if (flagType == null || !component.isEnabled()) {
      if (defaultForeground != null) {
        component.setForeground(defaultForeground);
      }
      if (defaultBackground != null) {
        component.setBackground(defaultBackground);
      }
    }
    else if (!flagType.background) {
      component.setForeground(flagType.color);
    }
    else {
      component.setBackground(flagType.color);
    }
  }
}
