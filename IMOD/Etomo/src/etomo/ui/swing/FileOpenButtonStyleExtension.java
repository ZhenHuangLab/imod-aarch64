package etomo.ui.swing;

/**
 * <p>Description: A button style that added a folder icon.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class FileOpenButtonStyleExtension extends ButtonStyleExtension {
  private static FileOpenButtonStyleExtension INSTANCE = null;

  private FileOpenButtonStyleExtension() {
    super(false, new CompleteIcon("openFile.gif", null, null),
      new CompleteIcon("openFilePeet.png", null, null),
      new CompleteIcon("openFileRed.png", null, null));
  }

  static FileOpenButtonStyleExtension getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new FileOpenButtonStyleExtension();
    }
    return INSTANCE;
  }
}