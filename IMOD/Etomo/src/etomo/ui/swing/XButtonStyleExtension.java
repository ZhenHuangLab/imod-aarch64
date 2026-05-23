package etomo.ui.swing;

/**
 * <p>Description: A button style that adds an X icon.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class XButtonStyleExtension extends ButtonStyleExtension {
  private static XButtonStyleExtension INSTANCE = null;

  private XButtonStyleExtension() {
    super(false, new CompleteIcon("x.png", null, "x-pressed.png"),
      new CompleteIcon("xBlue.png", null, "xBlue-pressed.png"),
      new CompleteIcon("xRed.png", null, "xRed-pressed.png"));
  }

  static XButtonStyleExtension getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new XButtonStyleExtension();
    }
    return INSTANCE;
  }
}