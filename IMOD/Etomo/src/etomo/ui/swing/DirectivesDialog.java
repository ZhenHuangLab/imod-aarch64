package etomo.ui.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import etomo.BaseManager;
import etomo.BatchRunTomoManager;
import etomo.EtomoDirector;
import etomo.storage.DirectiveDef;
import etomo.storage.DirectiveFileInterface;
import etomo.storage.autodoc.WritableAutodoc;
import etomo.type.AxisID;
import etomo.type.BatchRunTomoStatus;
import etomo.type.DirectiveFileType;
import etomo.ui.BrowsingDirectory;
import etomo.ui.FieldDisplayer;
import etomo.ui.RowListener;
import etomo.ui.SectionListener;

/**
 * <p>Description: The main dialog for the directives editor.</p>
 * 
 * <p>Copyright: Copyright 2017 - 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class DirectivesDialog implements ActionListener {
  private final JPanel pnlRoot = new JPanel();
  private final Ebutton btnCloseAllSections =
    Ebutton.getSingleLineInstance("Close All Sections");
  private final CheckBoxEfield cbShowForTemplateOnly =
    CheckBoxEfield.getInstance("Only items saved to template by default");
  private final CheckBoxEfield cbShowIfSet =
    CheckBoxEfield.getInstance("Only items containing a value");
  private final CheckBoxEfield cbShowIncludedOnly =
    CheckBoxEfield.getInstance("Only items output to batch files");
  private final File calibrationDir = EtomoDirector.INSTANCE.getIMODCalibDirectory();

  private final DirectivesTable table;
  private final BaseManager manager;
  private final DirectiveFileType directiveFileType;
  private final Ebutton btnSave;
  private final Ebutton btnCancel;
  private final CheckBoxEfield cbShowUnchanged;
  private final BrowsingDirectory browsingDirectory;
  private final BatchRunTomoDatasetDialog parent;

  private List<RowListener> rowListeners = null;
  private List<SectionListener> sectionListeners = null;

  private DirectivesDialog(final BaseManager manager,
    final BatchRunTomoDatasetDialog parent, final DirectiveFileType directiveFileType,
    final Map<DirectiveDef, String> templateValues,
    final Set<DirectiveDef> excludedDirectives,
    final BrowsingDirectory browsingDirectory) {
    this.manager = manager;
    this.parent = parent;
    this.directiveFileType = directiveFileType;
    this.browsingDirectory = browsingDirectory;
    table = new DirectivesTable(manager, this, directiveFileType, templateValues,
      excludedDirectives);
    if (directiveFileType != null) {
      btnSave = Ebutton.getSingleLineInstance("Save");
      btnCancel = Ebutton.getSingleLineInstance("Cancel");
      cbShowUnchanged = CheckBoxEfield.getInstance("Show unchanged");
    }
    else {
      btnSave = null;
      btnCancel = null;
      cbShowUnchanged = null;
    }
  }

  public static DirectivesDialog getInstance(final BaseManager manager,
    final BatchRunTomoDatasetDialog parent, final DirectiveFileType type,
    final BrowsingDirectory browsingDir, final FieldDisplayer fieldDisplayer) {
    DirectivesDialog instance =
      new DirectivesDialog(manager, parent, type, null, null, browsingDir);
    instance.createPanel(null, false);
    instance.setTooltips();
    instance.addListeners();
    return instance;
  }

  static DirectivesDialog getDialogInstance(final BatchRunTomoManager manager,
    final BatchRunTomoDatasetDialog parent,
    final Map<DirectiveDef, String> templateValues,
    final Set<DirectiveDef> excludedDirectives, final Ebutton btnBasic,
    final boolean shiftButton, final BrowsingDirectory browsingDir,
    final boolean global) {
    DirectivesDialog instance = new DirectivesDialog(manager, parent, null,
      templateValues, excludedDirectives, browsingDir);
    instance.createPanel(btnBasic, shiftButton);
    instance.setTooltips();
    instance.addListeners();
    return instance;
  }

  boolean isFindSecAddThicknessSet() {
    return parent.isFindSecAddThicknessSet();
  }

  boolean isScaleFromZSet() {
    return parent.isScaleFromZSet();
  }

  boolean validate(final FieldDisplayer fieldDisplayer) {
    return table.validate(fieldDisplayer);
  }

  boolean hasDual() {
    return parent.hasDual();
  }

  private void createPanel(final Ebutton btnBasic, final boolean shiftBasicButton) {
    // panels
    JPanel pnlControl = new JPanel();
    JPanel pnlShow = new JPanel();
    JPanel pnlButtons = new JPanel();
    JPanel pnlSave = null;
    if (btnSave != null) {
      pnlSave = new JPanel();
    }
    // init
    int index = -1;
    if (directiveFileType != null) {
      index = directiveFileType.getIndex();
    }
    if (directiveFileType != null) {
      if (directiveFileType.isBatch()) {
        cbShowForTemplateOnly.setEnabled(false);
        cbShowForTemplateOnly.setVisible(false);
      }
      else {
        cbShowForTemplateOnly.setSelected(true);
      }
    }
    table.init();
    // root
    ToolTipManager.sharedInstance().registerComponent(pnlRoot);
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
    pnlRoot.add(pnlControl);
    pnlRoot.add(table.getContainer());
    // control
    pnlControl.setLayout(new BoxLayout(pnlControl, BoxLayout.X_AXIS));
    pnlControl.add(pnlShow);
    pnlControl.add(Box.createRigidArea(FixedDim.x143_y0));
    pnlControl.add(pnlButtons);
    // Buttons
    pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.Y_AXIS));
    if (pnlSave != null) {
      pnlButtons.add(pnlSave);
    }
    if (btnBasic != null) {
      if (shiftBasicButton) {
        pnlButtons.add(Box.createRigidArea(FixedDim.x0_y5));
      }
      pnlButtons.add(btnBasic.getComponent());
    }
    pnlButtons.add(Box.createVerticalGlue());
    pnlButtons.add(btnCloseAllSections.getComponent());
    // show
    pnlShow.setLayout(new BoxLayout(pnlShow, BoxLayout.Y_AXIS));
    pnlShow.setBorder(new EtchedBorder("Which Directives to Show").getBorder());
    if (cbShowUnchanged != null) {
      pnlShow.add(cbShowUnchanged.getComponent());
    }
    // pnlShow.add(cbShowForTemplateOnly.getComponent());
    pnlShow.add(cbShowIfSet.getComponent());
    pnlShow.add(cbShowIncludedOnly.getComponent());
    // Save
    if (pnlSave != null) {
      pnlSave.setLayout(new BoxLayout(pnlSave, BoxLayout.X_AXIS));
      pnlSave.add(Box.createHorizontalGlue());
      pnlSave.add(btnSave.getComponent());
      pnlSave.add(Box.createHorizontalGlue());
      pnlSave.add(btnCancel.getComponent());
      pnlSave.add(Box.createHorizontalGlue());
    }
    //
    UIUtilities.alignComponentsX(pnlControl, Box.LEFT_ALIGNMENT);
    UIUtilities.alignComponentsX(pnlButtons, Box.RIGHT_ALIGNMENT);
    UIUtilities.alignComponentsX(pnlShow, Box.LEFT_ALIGNMENT);
    UIUtilities.alignComponentsX(pnlRoot, Box.LEFT_ALIGNMENT);
  }

  private void addListeners() {
    cbShowIncludedOnly.addActionListener(this);
    cbShowForTemplateOnly.addActionListener(this);
    cbShowIfSet.addActionListener(this);
    btnCloseAllSections.addActionListener(this);
    if (btnSave != null) {
      btnSave.addActionListener(this);
      btnCancel.addActionListener(this);
    }
  }

  DirectivesDirectiveRow getRow(final DirectiveDef directiveDef) {
    return table.getRow(directiveDef);
  }

  public void setValues(final BaseManager sourceManager) {
    table.setValues(sourceManager);
  }

  void setValues(final DirectiveFileInterface directiveFile,
    final boolean setFieldHighlightValue) {
    table.setValues(directiveFile, setFieldHighlightValue);
  }

  void clearTemplateValues() {
    table.clearTemplateValues();
  }

  void clear() {
    table.clear();
  }

  void checkpointAndRestoreFromBackup(final boolean retainUserValues) {
    table.checkpointAndRestoreFromBackup(retainUserValues);
  }

  boolean isShowForTemplateOnly() {
    return cbShowForTemplateOnly.isSelected() && cbShowForTemplateOnly.isEnabled();
  }

  boolean isShowIfSet() {
    return cbShowIfSet.isSelected() && cbShowIfSet.isEnabled();
  }

  boolean isShowIncludedOnly() {
    return cbShowIncludedOnly.isSelected() && cbShowIncludedOnly.isEnabled();
  }

  /**
   * Allows the row to react to cbShowIncludedOnly actions after checkbox
   * has been modified.  Allows the row to react checkbox actions.
   * @param listener
   * @see actionPerformed
   */
  void addRowListener(final RowListener listener) {
    if (rowListeners == null) {
      rowListeners = new ArrayList<RowListener>();
    }
    rowListeners.add(listener);
  }

  /**
   * Allows the sections to react to checkbox actions
   * after the row changes have completed.
   * @param listener
   * @see actionPerformed
   */
  void addSectionListener(final SectionListener listener) {
    if (sectionListeners == null) {
      sectionListeners = new ArrayList<SectionListener>();
    }
    sectionListeners.add(listener);
  }

  boolean isSelected(final String actionCommand) {
    if (actionCommand == null) {
      return false;
    }
    if (actionCommand.equals(cbShowIncludedOnly.getActionCommand())) {
      return cbShowIncludedOnly.isSelected();
    }
    if (actionCommand.equals(cbShowForTemplateOnly.getActionCommand())) {
      return cbShowForTemplateOnly.isSelected();
    }
    if (actionCommand.equals(cbShowIfSet.getActionCommand())) {
      return cbShowIfSet.isSelected();
    }
    return false;
  }

  boolean backupIfChanged() {
    return table.backupIfChanged();
  }

  boolean saveAutodoc(final WritableAutodoc autodoc, final boolean doValidation,
    final FieldDisplayer fieldDisplayer) {
    return table.saveAutodoc(autodoc, doValidation, fieldDisplayer);
  }

  public void setAdvanced(final boolean advanced) {}

  void statusChanged(final BatchRunTomoStatus status) {
    table.statusChanged(status);
  }

  private void setTooltips() {
    if (cbShowUnchanged != null) {
      cbShowUnchanged.setToolTipText("Show directives whose values have not changed.");
    }
    cbShowForTemplateOnly.setToolTipText(
      "Show only directives that are usually included in a directive file.");
    cbShowIfSet
      .setToolTipText("Show only directives that have a value or are overridden.");
  }

  Component getComponent() {
    return pnlRoot;
  }

  public Container getContainer() {
    return pnlRoot;
  }

  public void actionPerformed(final ActionEvent event) {
    if (event == null) {
      return;
    }
    String actionCommand = event.getActionCommand();
    if (btnCloseAllSections.getActionCommand().equals(actionCommand)) {
      table.closeAllSections();
    }
    else if (btnSave != null && btnSave.getActionCommand().equals(actionCommand)) {
      UIHarness.INSTANCE.save(manager, AxisID.ONLY);
    }
    else if (btnCancel != null && btnCancel.getActionCommand().equals(actionCommand)) {
      UIHarness.INSTANCE.cancel(manager);
    }
    else if (cbShowIncludedOnly.getActionCommand().equals(actionCommand)) {
      boolean enable = !cbShowIncludedOnly.isSelected();
      if (cbShowUnchanged != null) {
        cbShowUnchanged.setEnabled(enable);
      }
      cbShowForTemplateOnly.setEnabled(enable);
      sendEvents();
    }
    else if (cbShowForTemplateOnly.getActionCommand().equals(actionCommand)) {
      sendEvents();
    }
    else if (cbShowIfSet.getActionCommand().equals(actionCommand)) {
      sendEvents();
    }
  }

  void sendEvents() {
    if (rowListeners != null) {
      Iterator<RowListener> iterator = rowListeners.iterator();
      while (iterator.hasNext()) {
        iterator.next().rowEvent();
      }
    }
    if (sectionListeners != null) {
      Iterator<SectionListener> iterator = sectionListeners.iterator();
      while (iterator.hasNext()) {
        iterator.next().sectionEvent();
      }
    }
  }

  File getCalibrationDir() {
    return calibrationDir;
  }

  BrowsingDirectory getBrowsingDirectory() {
    return browsingDirectory;
  }
}