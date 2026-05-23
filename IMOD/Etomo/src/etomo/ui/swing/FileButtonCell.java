package etomo.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileFilter;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.storage.ExtensibleFileFilter;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.UITestFieldType;
import etomo.ui.BrowsingDirectory;
import etomo.util.Utilities;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2011 - 2017 by the Regents of the University of Colorado</p>
* 
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
final class FileButtonCell extends InputCell {
  private final BaseManager manager;

  // Field that this button is associated with:
  private ActionTarget actionTarget = null;
  private String label = null;
  private FileFilter fileFilter = null;
  private boolean enabled = true;
  private BrowsingDirectory browsingDir = null;

  private final SimpleButton button =
    new SimpleButton(new ImageIcon(ClassLoader.getSystemResource(
      !Utilities.APRIL_FOOLS ? "images/openFilePeet.png" : "images/openFileFool.png")));

  private FileButtonCell(final BaseManager manager) {
    this.manager = manager;
    button.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    // button.setBorder(BorderFactory.createEtchedBorder());
    Dimension size = button.getPreferredSize();
    if (size.width < size.height) {
      size.width = size.height;
    }
    button.setSize(size);
  }

  static FileButtonCell getInstance(final FileButtonCell fileButtonCell) {
    FileButtonCell instance = new FileButtonCell(fileButtonCell.manager);
    instance.browsingDir = fileButtonCell.browsingDir;
    instance.label = fileButtonCell.label;
    instance.fileFilter = fileButtonCell.fileFilter;
    instance.addListeners();
    return instance;
  }

  static FileButtonCell getInstance(final BaseManager manager) {
    FileButtonCell instance = new FileButtonCell(manager);
    instance.addListeners();
    return instance;
  }

  void setBrowsingDirectory(final BrowsingDirectory input) {
    browsingDir = input;
  }

  public void add(JPanel panel, GridBagLayout layout, GridBagConstraints constraints) {
    double oldWeightx = constraints.weightx;
    constraints.weightx = 0.0;
    super.add(panel, layout, constraints);
    constraints.weightx = oldWeightx;
  }

  private void addListeners() {
    button.addActionListener(new FileButtonActionListener(this));
  }

  void setActionTarget(final ActionTarget input) {
    actionTarget = input;
  }

  void setHeaders(String tableHeader, HeaderCell rowHeader, HeaderCell columnHeader) {
    if (label == null) {
      label = columnHeader.getText();
    }
    super.setHeaders(tableHeader, rowHeader, columnHeader);
  }

  void setName() {
    String name = convertLabelToName();
    button.setName(name);
    if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
      System.out.println(
        getComponent().getName() + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
    }
  }

  void setLabel(final String input) {
    label = input;
  }

  void setFileFilter(final FileFilter input) {
    fileFilter = input;
  }

  void setFileFilter(final ExtensibleFileFilter input) {
    fileFilter = input;
  }

  FileFilter getFileFilter() {
    return fileFilter;
  }

  Component getComponent() {
    return button;
  }

  UITestFieldType getFieldType() {
    return UITestFieldType.BUTTON;
  }

  int getWidth() {
    return button.getSize().width;
  }

  void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    button.setEnabled(enabled && isEditable());
  }

  boolean isEnabled() {
    return enabled;
  }

  private void action() {
    JFileChooser chooser = new FileChooser(manager, null,
      actionTarget != null ? actionTarget.getExpandedValue() : null, browsingDir);
    chooser.setDialogTitle(label == null ? "Open File" : label);
    chooser.setPreferredSize(UIParameters.getInstance().getFileChooserDimension());
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    if (fileFilter != null) {
      chooser.setFileFilter(fileFilter);
    }
    int returnVal = chooser.showOpenDialog(button.getParent());
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      if (actionTarget != null) {
        actionTarget.setTargetFile(file);
      }
      if (browsingDir != null && file != null) {
        browsingDir.setBrowsingDir(file.getParentFile());
      }
    }
  }

  void setToolTipText(String text) {
    button.setToolTipText(TooltipFormatter.INSTANCE.format(text));
  }

  private static final class FileButtonActionListener implements ActionListener {
    private final FileButtonCell fileButtonCell;

    private FileButtonActionListener(final FileButtonCell fileButtonCell) {
      this.fileButtonCell = fileButtonCell;
    }

    public void actionPerformed(final ActionEvent event) {
      fileButtonCell.action();
    }
  }
}
