package etomo.ui.swing;

import java.awt.Insets;

import javax.swing.AbstractButton;

/**
 * <p>Description: A button style that adds a plus/minus icon to a button.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class OpenCloseButtonStyleExtension extends ButtonStyleExtension {
  private static OpenCloseButtonStyleExtension INSTANCE = null;

  private OpenCloseButtonStyleExtension() {
    super(true, new CompleteIcon("openSmall.png", "closeSmall.png", null), null,
      new CompleteIcon("openRedSmall.png", "closeRedSmall.png", null));
  }

  static OpenCloseButtonStyleExtension getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new OpenCloseButtonStyleExtension();
    }
    return INSTANCE;
  }

  void setup(final AbstractButton button, final String label) {
    super.setup(button, label);
    if (button == null) {
      return;
    }
    button.setMargin(new Insets(0, 8, 0, 8));
  }
}