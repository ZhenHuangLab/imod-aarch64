package etomo.ui.swing;

import java.io.File;

import javax.swing.JFileChooser;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.AxisID;
import etomo.ui.BrowsingDirectory;
import etomo.util.Utilities;
import etomo.util.ValidDirectory;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2008 - 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.2  2009/11/20 17:11:29  sueh
 * <p> bug# 1282 The default title of a file chooser is "Open", so naming
 * <p> instances of this class "open" as the default.
 * <p>
 * <p> Revision 1.1  2009/01/20 20:02:12  sueh
 * <p> bug# 1102 A self-naming JFileChooser.
 * <p> </p>
 */
public final class FileChooser extends JFileChooser {
  static final String DEFAULT_TITLE = "Open";

  private final BaseManager manager;
  private final AxisID axisID;

  public FileChooser() {
    this(null, null, (String) null, null);
  }

  public FileChooser(final BaseManager manager) {
    this(manager, null, (String) null, null);
  }

  public FileChooser(final String browsingDir) {
    this(null, null, browsingDir, null);
  }

  public FileChooser(final BaseManager manager, final File browsingDir) {
    this(manager, null, browsingDir != null ? browsingDir.getAbsolutePath() : null, null);
  }

  public FileChooser(final BaseManager manager, final String browsingDir) {
    this(manager, null, browsingDir, null);
  }

  public FileChooser(final BaseManager manager, final File browsingDir,
    final BrowsingDirectory altBrowsingDir) {
    this(manager, null, browsingDir != null ? browsingDir.getAbsolutePath() : null,
      altBrowsingDir);
  }

  public FileChooser(final BrowsingDirectory browsingDir) {
    this((BaseManager) null, null, (String) null, browsingDir);
  }

  public FileChooser(final BaseManager manager, final AxisID axisID,
    final String browsingDir, final BrowsingDirectory altBrowsingDir) {
    super(getBrowsingDir(manager, browsingDir, altBrowsingDir));
    this.manager = manager;
    this.axisID = axisID;
    setName(DEFAULT_TITLE);
  }

  private static File getBrowsingDir(final BaseManager manager, final String browsingDir,
    final BrowsingDirectory altBrowsingDir) {
    if (browsingDir != null && !browsingDir.matches("\\s*")) {
      ValidDirectory dir = new ValidDirectory(manager);
      dir.set(browsingDir);
      if (!dir.isNull()) {
        return dir.get();
      }
    }
    if (altBrowsingDir != null) {
      return altBrowsingDir.getBrowsingDir();
    }
    if (manager != null) {
      String userDir = manager.getPropertyUserDir();
      if (userDir != null) {
        return new File(userDir);
      }
    }
    return null;
  }

  public void setDialogTitle(String dialogTitle) {
    super.setDialogTitle(dialogTitle);
    setName(dialogTitle);
  }

  public void setName(String text) {
    String name = Utilities.convertLabelToName(text);
    super.setName(name);
    if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
      System.out.println(getName() + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
    }
  }

  /**
   * Set the busy status before showing the dialog.
   */
  @Override
  @SuppressWarnings("deprecation")
  public void show() {
    if (manager != null && axisID != null) {
      manager.getBusyStatusMediator().msgFileChooserBuild(axisID, true);
    }
    super.show();
    if (manager != null && axisID != null) {
      manager.getBusyStatusMediator().msgFileChooserBuild(axisID, false);
    }
  }
}
