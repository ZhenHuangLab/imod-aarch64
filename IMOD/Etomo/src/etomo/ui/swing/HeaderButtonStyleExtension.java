package etomo.ui.swing;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;

/**
 * <p>Description: A button style for creating non-interactive table headers and empty
 * areas.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class HeaderButtonStyleExtension extends ButtonStyleExtension {
  private static HeaderButtonStyleExtension INSTANCE = null;

  private HeaderButtonStyleExtension() {
    super(false, null, null, null);
  }

  static HeaderButtonStyleExtension getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new HeaderButtonStyleExtension();
    }
    return INSTANCE;
  }

  final void setup(final AbstractButton button, String label) {
    if (button == null) {
      return;
    }
    if (label != null) {
      button.setText(label + ": ");
    }
    button.setFocusable(false);
    button.setContentAreaFilled(false);
    button.setBorder(BorderFactory.createEtchedBorder());
  }
}
