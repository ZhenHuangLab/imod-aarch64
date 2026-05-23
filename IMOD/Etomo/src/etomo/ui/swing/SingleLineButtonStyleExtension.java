package etomo.ui.swing;

import java.awt.Dimension;

import javax.swing.AbstractButton;

final class SingleLineButtonStyleExtension extends ButtonStyleExtension {
  private static SingleLineButtonStyleExtension INSTANCE = null;

  private SingleLineButtonStyleExtension() {
    super(false, null, null, null);
  }

  static SingleLineButtonStyleExtension getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new SingleLineButtonStyleExtension();
    }
    return INSTANCE;
  }

  void setup(final AbstractButton button) {
    Dimension size = UIParameters.getInstance(UIUtilities.getFontMetrics(button))
      .getButtonSingleLineDimension();
    button.setPreferredSize(size);
    button.setMaximumSize(size);
  }
}