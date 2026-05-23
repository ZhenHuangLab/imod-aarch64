package etomo.ui.swing;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import etomo.ui.BrowsingDirectory;
import etomo.util.ValidDirectory;

/**
 * <p>Description: An extension for storing information about a file chooser and opening
 * the file chooser.  Should not contain any information about a specific component.
 * Sharable between multiple buttons which need the same file chooser.</p>
 * 
 * <p>Copyright: Copyright 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class SelectFileExtension {
  private String dir = null;
  private BrowsingDirectory altBrowsingDirectory = null;
  private FileFilter fileFilter = null;
  private int fileSelectionMode = -1;

  SelectFileExtension() {}

  void setDir(final String dir) {
    this.dir = dir;
  }

  void setAltBrowsingDirectory(final BrowsingDirectory altBrowsingDirectory) {
    this.altBrowsingDirectory = altBrowsingDirectory;
  }

  void setFileFilter(final FileFilter fileFilter) {
    this.fileFilter = fileFilter;
  }

  void setFileSelectionMode(final int fileSelectionMode) {
    this.fileSelectionMode = fileSelectionMode;
  }

  public File selectFile(final Component component,
    final File overrideFileOpenDirectory) {
    JFileChooser chooser = new JFileChooser(getDirectory(overrideFileOpenDirectory));
    chooser.setDialogTitle("Select File");
    chooser.setPreferredSize(UIParameters.getInstance().getFileChooserDimension());
    if (fileSelectionMode != -1) {
      chooser.setFileSelectionMode(fileSelectionMode);
    }
    if (fileFilter != null) {
      chooser.setFileFilter(fileFilter);
    }
    if (chooser.showOpenDialog(component) == JFileChooser.APPROVE_OPTION) {
      return chooser.getSelectedFile();
    }
    return null;
  }

  private File getDirectory(final File overrideFileOpenDirectory) {
    if (ValidDirectory.isValid(overrideFileOpenDirectory)) {
      return ValidDirectory.get(overrideFileOpenDirectory);
    }
    if (ValidDirectory.isValid(altBrowsingDirectory)) {
      return ValidDirectory.get(altBrowsingDirectory);
    }
    if (dir != null) {
      return new File(dir);
    }
    return null;
  }

}
