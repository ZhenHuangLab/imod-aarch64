package etomo.ui.swing;

import java.awt.Color;

import javax.swing.UIManager;

/**
 * <p>Description: Gets colors for various types of widgets.  Can be used to customize
 * colors.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class ColorTool {
  static final ColorTool TOGGLE_BUTTON =
    new ColorTool("ToggleButton.foreground", "ToggleButton.disabledText");
  static final ColorTool BUTTON =
    new ColorTool("Button.foreground", "Button.disabledText");

  private final String enabledForegroundProperty;
  private final String disabledForegroundProperty;
  private final Color enabledForeground;
  private final Color disabledForeground;

  private ColorTool(final String enabledForegroundProperty,
    final String disabledForegroundProperty) {
    this.enabledForegroundProperty = enabledForegroundProperty;
    this.disabledForegroundProperty = disabledForegroundProperty;
    enabledForeground = getColor(enabledForegroundProperty);
    disabledForeground = getColor(disabledForegroundProperty);
  }

  static ColorTool getButtonInstance(final boolean toggleButton) {
    return toggleButton ? TOGGLE_BUTTON : BUTTON;
  }
  
  public String toString() {
    if (this == TOGGLE_BUTTON) {
      return "TOGGLE_BUTTON";
    }
    if (this == BUTTON) {
      return "BUTTON";
    }
    return super.toString();
  }

  private Color getColor(final String property) {
    Color color = UIManager.getColor(property);
    if (color != null) {
      return color;
    }
    if (property == null || property.equals(enabledForegroundProperty)) {
      return new Color(51, 51, 51);
    }
    return new Color(153, 153, 153);
  }

  Color getForeground(final boolean enabled) {
    return enabled ? enabledForeground : disabledForeground;
  }
}
