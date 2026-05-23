package etomo.ui.swing;

/**
 * <p>Description: An extension which gives a button an eraser icon.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class ClearButtonStyleExtension extends ButtonStyleExtension {
  private static ClearButtonStyleExtension INSTANCE = null;

  private ClearButtonStyleExtension() {
    super(false, new CompleteIcon("clear.png", null, null),
      new CompleteIcon("clearBlue.png", null, null),
      new CompleteIcon("clearRed.png", null, null));
  }

  static ClearButtonStyleExtension getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new ClearButtonStyleExtension();
    }
    return INSTANCE;
  }
}