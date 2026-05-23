package etomo.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import etomo.BaseManager;
import etomo.logic.BatchTool;
import etomo.storage.DirectiveAdaptor;
import etomo.storage.DirectiveDef;
import etomo.storage.DirectiveDescrChoiceList;
import etomo.storage.DirectiveDescrElement;
import etomo.storage.DirectiveDescrEtomoColumn;
import etomo.storage.DirectiveFileInterface;
import etomo.storage.DirectiveValue;
import etomo.storage.DirectiveValueType;
import etomo.storage.autodoc.WritableAutodoc;
import etomo.type.BatchRunTomoStatus;
import etomo.type.DirectiveInterface;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.FlagType;
import etomo.ui.RowListener;
import etomo.ui.UIComponent;

/**
 * <p>Description: A row associated with one directive in the Directives Editor.</p>
 * 
 * <p>Copyright: Copyright 2017 - 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class DirectivesDirectiveRow
  implements DirectivesRow, DirectiveInterface, RowListener {

  private final Ebutton hTitle;
  private final BooleanComboBoxEfield bcbValue;
  private final ComboBoxEfield cbValue;
  private final TextEfield tfValue;
  private final ButtonControlTextEfield bctfValue;
  private final DirectiveDescrEtomoColumn etomoColumn;
  private final DirectiveValueType valueType;
  private final DirectiveDef directiveDef;
  private final boolean descriptionAvailable;
  private final BaseManager manager;
  private final DirectivesDialog dialog;
  private final Ebutton hEmpty;
  private final ToggleEbutton tbtnOverrideToggle;
  private final DirectivesSectionRow section;

  private static SelectFileExtension select_file_extension = null;

  private boolean open = false;
  private boolean showForTemplateOnly = true;
  private boolean showIncludedOnly = false;
  private boolean debug = false;
  private boolean showIfSet = false;

  /**
   * 
   * @param lineArray
   * @param directiveMap
   * @param isChoiceList
   */
  private DirectivesDirectiveRow(final BaseManager manager, final DirectivesDialog dialog,
    final DirectivesSectionRow section, final String[] lineArray,
    final boolean isChoiceList, final boolean dialogMode, final DirectiveDef directiveDef,
    final boolean debug) {
    this.manager = manager;
    this.dialog = dialog;
    this.directiveDef = directiveDef;
    this.debug = debug;
    this.section = section;
    descriptionAvailable = true;
    etomoColumn = DirectiveDescrElement.getEtomoColumn(lineArray);
    // Set the title
    String title = null;
    if (DirectiveDescrElement.isLabel(lineArray)) {
      title = DirectiveDescrElement.getLabel(lineArray);
    }
    else if (directiveDef != null) {
      title = directiveDef.toString();
    }
    else {
      title = DirectiveDescrElement.getName(lineArray);
    }
    hTitle = Ebutton.getHeaderInstance(title);
    hTitle.setAllowFlagEditableControl(false);
    valueType = DirectiveDescrElement.getValueType(lineArray);
    String sectionTitle = section.getTitle();
    if (valueType == DirectiveValueType.BOOLEAN) {
      bcbValue = new BooleanComboBoxEfield(title);
      bcbValue.setDirectiveDef(directiveDef);
      bcbValue.addFlagDisplay(hTitle);
      bcbValue.addFinalFlagDisplay(section);
      hEmpty = Ebutton.getHeaderInstance();
      cbValue = null;
      bctfValue = null;
      tfValue = null;
      tbtnOverrideToggle = null;
    }
    else if (isChoiceList) {
      cbValue = ComboBoxEfield.getOverrideInstance(title, true);
      cbValue.setDirectiveDef(directiveDef);
      cbValue.addFlagDisplay(hTitle);
      cbValue.addFinalFlagDisplay(section);
      tbtnOverrideToggle = ToggleEbutton.getOverrideInstance(cbValue);
      cbValue.addFlagDisplay(tbtnOverrideToggle);
      bcbValue = null;
      bctfValue = null;
      tfValue = null;
      hEmpty = null;
    }
    else if (valueType == DirectiveValueType.FILE) {
      bctfValue =
        ButtonControlTextEfield.getFileOverrideInstance(title, select_file_extension);
      if (select_file_extension == null) {
        select_file_extension = bctfValue.getSelectFileExtension();
        if (select_file_extension != null) {
          select_file_extension.setAltBrowsingDirectory(dialog.getBrowsingDirectory());
        }
      }
      if (directiveDef == DirectiveDef.DISTORT || directiveDef == DirectiveDef.GRADIENT
        || directiveDef == DirectiveDef.CTF_NOISE) {
        bctfValue.setOverrideFileOpenDirectory(dialog.getCalibrationDir());
        bctfValue.setFileOnly(true);
        bctfValue.setFileMustExist(true);
        bctfValue.setFlagErrors();
      }
      bctfValue.setColumns();
      bctfValue.setDirectiveDef(directiveDef);
      bctfValue.setLocationDescr(sectionTitle);
      bctfValue.addFinalFlagDisplay(section);
      bctfValue.addFlagDisplay(hTitle);
      tbtnOverrideToggle = ToggleEbutton.getOverrideInstance(bctfValue);
      bctfValue.addFlagDisplay(tbtnOverrideToggle);
      bcbValue = null;
      cbValue = null;
      tfValue = null;
      hEmpty = null;
    }
    else {
      tfValue = TextEfield.getOverrideInstance(title, valueType);
      tfValue.setColumns();
      tfValue.setDirectiveDef(directiveDef);
      tfValue.setFlagErrors();
      tfValue.addFlagDisplay(hTitle);
      tfValue.addFinalFlagDisplay(section);
      tfValue.setLocationDescr(sectionTitle);
      tbtnOverrideToggle = ToggleEbutton.getOverrideInstance(tfValue);
      tfValue.addFlagDisplay(tbtnOverrideToggle);
      bcbValue = null;
      cbValue = null;
      bctfValue = null;
      hEmpty = null;
    }
    // init
    if (tbtnOverrideToggle != null) {
      tbtnOverrideToggle.setEnabled(false);
    }
  }

  private DirectivesDirectiveRow(final BaseManager manager, final DirectivesDialog dialog,
    final DirectivesSectionRow section, final DirectiveAdaptor directive,
    final boolean dialogMode) {
    this.manager = manager;
    this.dialog = dialog;
    this.section = section;
    descriptionAvailable = false;
    directiveDef = directive.getDirectiveDef();
    etomoColumn = null;
    // Set the title
    String title = directiveDef.toString();
    hTitle = Ebutton.getHeaderInstance(title);
    hTitle.setAllowFlagEditableControl(false);
    valueType = DirectiveValueType.STRING;
    tfValue = TextEfield.getOverrideInstance(title, valueType);
    tfValue.setColumns();
    tfValue.setDirectiveDef(directiveDef);
    tfValue.setFlagErrors();
    tfValue.addFlagDisplay(hTitle);
    tfValue.addFinalFlagDisplay(section);
    tfValue.setLocationDescr(section.getTitle());
    tbtnOverrideToggle = ToggleEbutton.getOverrideInstance(tfValue);
    tfValue.addFlagDisplay(tbtnOverrideToggle);
    bcbValue = null;
    cbValue = null;
    bctfValue = null;
    hEmpty = null;
    // init
    if (tbtnOverrideToggle != null) {
      tbtnOverrideToggle.setEnabled(false);
    }
  }

  static DirectivesDirectiveRow getInstance(final BaseManager manager,
    final DirectivesDialog dialog, final DirectivesSectionRow section,
    final String[] lineArray, final JPanel panel, final GridBagLayout layout,
    final GridBagConstraints constraints, final boolean dialogMode,
    final DirectiveDef directiveDef, final boolean debug) {
    DirectiveDescrChoiceList choiceList = DirectiveDescrElement.getChoiceList(lineArray);
    // Create and setup instance
    DirectivesDirectiveRow instance = new DirectivesDirectiveRow(manager, dialog, section,
      lineArray, choiceList != null, dialogMode, directiveDef, debug);
    instance.createPanel(choiceList);
    instance.addListeners();
    if (section != null) {
      section.addDirective(instance);
    }
    instance.setTooltips(lineArray);
    return instance;
  }

  static DirectivesDirectiveRow getInstance(final BaseManager manager,
    final DirectivesDialog dialog, final DirectivesSectionRow section,
    final DirectiveAdaptor directive, final boolean dialogMode) {
    // Create and setup instance
    DirectivesDirectiveRow instance =
      new DirectivesDirectiveRow(manager, dialog, section, directive, dialogMode);
    instance.createPanel(null);
    instance.addListeners();
    if (section != null) {
      section.addDirective(instance);
    }
    instance.setTooltips(null);
    return instance;
  }

  public String toString() {
    return hTitle.getText();
  }

  DirectiveDef getDirectiveDef() {
    return directiveDef;
  }

  private void createPanel(final DirectiveDescrChoiceList choiceList) {
    // init
    hTitle.setHorizontalAlignment(SwingConstants.RIGHT);
    if (cbValue != null && choiceList != null) {
      cbValue.setChoiceList(choiceList);
    }
    rowEvent();
  }

  private void addListeners() {
    // Listen to the checkboxes in the show panel.
    dialog.addRowListener(this);
  }

  public void statusChanged(final BatchRunTomoStatus status) {
    setEditable(status == null || status == BatchRunTomoStatus.OPEN);
  }

  public void setEditable(final boolean editable) {
    if (bcbValue != null) {
      bcbValue.setEditable(editable);
    }
    else if (cbValue != null) {
      cbValue.setEditable(editable);
    }
    else if (bctfValue != null) {
      bctfValue.setEditable(editable);
    }
    else if (tfValue != null) {
      tfValue.setEditable(editable);
    }
  }

  void saveAutodoc(final WritableAutodoc autodoc, final boolean doValidation,
    final FieldDisplayer fieldDisplayer, final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    boolean overrideAvailable = tbtnOverrideToggle != null;
    boolean override = overrideAvailable && tbtnOverrideToggle.isSelected();
    if (bcbValue != null) {
      BatchTool.saveTextToAutodoc(bcbValue, bcbValue.getText(), autodoc, templateValues);
    }
    else if (cbValue != null) {
      BatchTool.saveTextToAutodoc(cbValue, cbValue.getText(), overrideAvailable, override,
        autodoc, templateValues);
    }
    else if (bctfValue != null) {
      BatchTool.saveTextToAutodoc(bctfValue,
        bctfValue.getText(doValidation, fieldDisplayer, section), overrideAvailable,
        override, autodoc, templateValues);
    }
    else if (tfValue != null) {
      BatchTool.saveTextToAutodoc(tfValue,
        tfValue.getText(doValidation, fieldDisplayer, section), overrideAvailable,
        override, autodoc, templateValues);
    }
  }

  /**
   * Compares the row, displays this row, and pops up errMsg.
   * @param row1
   * @param row2
   * @param errMsg
   * @param fieldDisplayer
   * @return false if invalid
   */
  boolean validateMutuallyExclusive(final DirectivesDirectiveRow row1,
    final DirectivesDirectiveRow row2, final String errMsg,
    final FieldDisplayer fieldDisplayer) {
    // Find out how many of the mutually exclusive fields are set.
    int nfieldsSet = (isSet() ? 1 : 0) + ((row1 != null && row1.isSet()) ? 1 : 0)
      + ((row2 != null && row2.isSet()) ? 1 : 0);
    if (nfieldsSet > 1) {
      Popup.getErrorInstance(getUIComponent(), "Mutually Exclusive Fields", errMsg,
        fieldDisplayer, section).open();
      return false;
    }
    return true;
  }

  boolean validateMutuallyExclusive(final boolean fieldSet, final String errMsg,
    final FieldDisplayer fieldDisplayer) {
    if (!isEmpty() && fieldSet) {
      Popup.getErrorInstance(getUIComponent(), "Mutually Exclusive Fields", errMsg,
        fieldDisplayer, section).open();
      return false;
    }
    return true;
  }

  public void setValue(final boolean value) {
    if (bcbValue != null) {
      bcbValue.setSelected(value);
    }
  }

  public void setValue(final String value) {
    if (cbValue != null) {
      cbValue.setText(value);
    }
    else if (bctfValue != null) {
      bctfValue.setText(value);
    }
    else if (tfValue != null) {
      tfValue.setText(value);
    }
  }

  void setValue(final DirectiveFileInterface directiveFiles,
    final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues) {
    DirectiveValue directiveValue = null;
    if (bcbValue != null) {
      directiveValue = BatchTool.setTextValue(bcbValue, directiveFiles,
        setFieldHighlightValue, templateValues, true);
    }
    else if (cbValue != null) {
      directiveValue = BatchTool.setTextValue(cbValue, directiveFiles,
        setFieldHighlightValue, templateValues, true);
    }
    else if (bctfValue != null) {
      directiveValue = BatchTool.setTextValue(bctfValue, directiveFiles,
        setFieldHighlightValue, templateValues, true);
    }
    else if (tfValue != null) {
      directiveValue = BatchTool.setTextValue(tfValue, directiveFiles,
        setFieldHighlightValue, templateValues, true);
    }
    // Handle overrides
    if (tbtnOverrideToggle == null || directiveValue == null) {
      return;
    }
    // Never disable the override toggle. Its too complex to keep track of it.
    boolean override = directiveValue.isOverride();
    // Allow override of template values
    if (setFieldHighlightValue && !override) {
      tbtnOverrideToggle.setEnabled(true);
    }
    else if (directiveValue.isBatch()) {
      // Set override from batch file
      if (override) {
        tbtnOverrideToggle.setEnabled(true);
      }
      if (tbtnOverrideToggle.isEnabled()) {
        tbtnOverrideToggle.setSelected(override);
      }
    }
  }

  public UIComponent getUIComponent() {
    if (bcbValue != null) {
      return bcbValue;
    }
    if (cbValue != null) {
      return cbValue;
    }
    if (bctfValue != null) {
      return bctfValue;
    }
    if (tfValue != null) {
      return tfValue;
    }
    return null;
  }

  public void resetValue() {
    if (bcbValue != null) {
      bcbValue.clear();
    }
    else if (cbValue != null) {
      cbValue.clear();
    }
    else if (bctfValue != null) {
      bctfValue.clear();
    }
    else if (tfValue != null) {
      tfValue.clear();
    }
  }

  void restoreFromBackup() {
    if (bcbValue != null) {
      bcbValue.restoreFromBackup();
    }
    else if (cbValue != null) {
      cbValue.restoreFromBackup();
    }
    else if (bctfValue != null) {
      bctfValue.restoreFromBackup();
    }
    else if (tfValue != null) {
      tfValue.restoreFromBackup();
    }
  }

  boolean isDifferentFromCheckpoint(final boolean alwaysCheck) {
    if (bcbValue != null) {
      return bcbValue.isDifferentFromCheckpoint(alwaysCheck);
    }
    if (cbValue != null) {
      return cbValue.isDifferentFromCheckpoint(alwaysCheck);
    }
    if (bctfValue != null) {
      return bctfValue.isDifferentFromCheckpoint(alwaysCheck);
    }
    if (tfValue != null) {
      return tfValue.isDifferentFromCheckpoint(alwaysCheck);
    }
    return false;
  }

  FlagType getFlagType() {
    if (bcbValue != null) {
      return bcbValue.getFlagType();
    }
    if (cbValue != null) {
      return cbValue.getFlagType();
    }
    if (bctfValue != null) {
      return bctfValue.getFlagType();
    }
    if (tfValue != null) {
      return tfValue.getFlagType();
    }
    return null;
  }

  void clearValue() {
    if (bcbValue != null) {
      bcbValue.clear();
    }
    else if (cbValue != null) {
      cbValue.clear();
    }
    else if (bctfValue != null) {
      bctfValue.clear();
    }
    else if (tfValue != null) {
      tfValue.clear();
    }
  }

  private boolean isEmpty() {
    if (bcbValue != null) {
      return bcbValue.isEmpty();
    }
    if (cbValue != null) {
      return cbValue.isEmpty();
    }
    if (bctfValue != null) {
      return bctfValue.isEmpty();
    }
    if (tfValue != null) {
      return tfValue.isEmpty();
    }
    return true;
  }

  private boolean isSet() {
    if (bcbValue != null) {
      return !bcbValue.isEmpty() && !bcbValue.isOverride() && bcbValue.isSelected();
    }
    if (cbValue != null) {
      return !cbValue.isEmpty() && !cbValue.isOverride();
    }
    if (bctfValue != null) {
      return !bctfValue.isEmpty() && !bctfValue.isOverride();
    }
    if (tfValue != null) {
      return !tfValue.isEmpty() && !tfValue.isOverride();
    }
    return true;
  }

  String getValue() {
    if (bcbValue != null) {
      return bcbValue.getText();
    }
    if (cbValue != null) {
      return cbValue.getText();
    }
    if (bctfValue != null) {
      return bctfValue.getText();
    }
    if (tfValue != null) {
      return tfValue.getText();
    }
    return null;
  }

  private boolean isTemplateValue() {
    if (bcbValue != null) {
      return bcbValue.isTemplateValue();
    }
    if (cbValue != null) {
      return cbValue.isTemplateValue();
    }
    if (bctfValue != null) {
      return bctfValue.isTemplateValue();
    }
    if (tfValue != null) {
      return tfValue.isTemplateValue();
    }
    return false;
  }

  private boolean isOverride() {
    if (bcbValue != null) {
      return bcbValue.isOverride();
    }
    if (cbValue != null) {
      return cbValue.isOverride();
    }
    if (bctfValue != null) {
      return bctfValue.isOverride();
    }
    if (tfValue != null) {
      return tfValue.isOverride();
    }
    return false;
  }

  private boolean isEnabled() {
    if (bcbValue != null) {
      return bcbValue.isEnabled();
    }
    if (cbValue != null) {
      return cbValue.isEnabled();
    }
    if (bctfValue != null) {
      return bctfValue.isEnabled();
    }
    if (tfValue != null) {
      return tfValue.isEnabled();
    }
    return true;
  }

  private boolean isEditable() {
    if (bcbValue != null) {
      return bcbValue.isEditable();
    }
    if (cbValue != null) {
      return cbValue.isEditable();
    }
    if (bctfValue != null) {
      return bctfValue.isEditable();
    }
    if (tfValue != null) {
      return tfValue.isEditable();
    }
    return true;
  }

  void checkpoint() {
    if (bcbValue != null) {
      bcbValue.checkpoint();
    }
    else if (cbValue != null) {
      cbValue.checkpoint();
    }
    else if (bctfValue != null) {
      bctfValue.checkpoint();
    }
    else if (tfValue != null) {
      tfValue.checkpoint();
    }
  }

  void clearTemplateValue() {
    if (bcbValue != null) {
      bcbValue.clearTemplateValue();
    }
    else if (cbValue != null) {
      cbValue.clearTemplateValue();
    }
    else if (bctfValue != null) {
      bctfValue.clearTemplateValue();
    }
    else if (tfValue != null) {
      tfValue.clearTemplateValue();
    }
  }

  void clear() {
    if (bcbValue != null) {
      bcbValue.clear();
    }
    else if (cbValue != null) {
      cbValue.clear();
    }
    else if (bctfValue != null) {
      bctfValue.clear();
    }
    else if (tfValue != null) {
      tfValue.clear();
    }
  }

  void backup() {
    if (bcbValue != null) {
      bcbValue.backup();
    }
    else if (cbValue != null) {
      cbValue.backup();
    }
    else if (bctfValue != null) {
      bctfValue.backup();
    }
    else if (tfValue != null) {
      tfValue.backup();
    }
  }

  public void remove() {
    hTitle.remove();
    if (bcbValue != null) {
      bcbValue.remove();
    }
    else if (cbValue != null) {
      cbValue.remove();
    }
    else if (bctfValue != null) {
      bctfValue.remove();
    }
    else if (tfValue != null) {
      tfValue.remove();
    }
    if (hEmpty != null) {
      hEmpty.remove();
    }
    if (tbtnOverrideToggle != null) {
      tbtnOverrideToggle.remove();
    }
  }

  public void display(final JPanel pnlTable, final GridBagLayout layout,
    final GridBagConstraints constraints) {
    constraints.gridwidth = 1;
    hTitle.add(pnlTable, layout, constraints);
    if (bcbValue != null) {
      bcbValue.add(pnlTable, layout, constraints);
    }
    else if (cbValue != null) {
      cbValue.add(pnlTable, layout, constraints);
    }
    else if (bctfValue != null) {
      bctfValue.add(pnlTable, layout, constraints);
    }
    else if (tfValue != null) {
      tfValue.add(pnlTable, layout, constraints);
    }
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    if (tbtnOverrideToggle != null) {
      tbtnOverrideToggle.add(pnlTable, layout, constraints);
    }
    else if (hEmpty != null) {
      hEmpty.add(pnlTable, layout, constraints);
    }
  }

  public void rowEvent() {
    showForTemplateOnly = dialog.isShowForTemplateOnly();
    showIncludedOnly = dialog.isShowIncludedOnly();
    showIfSet = dialog.isShowIfSet();
    updateVisible();
  }

  /**
   * @return true if the row is allowed by the show panel.
   */
  boolean canDisplay() {
    if (showIncludedOnly || showIfSet) {
      boolean set = !isEmpty() || isOverride();
      if (showIncludedOnly && set && !isTemplateValue()) {
        return true;
      }
      if (showIfSet && set && !showIncludedOnly) {
        return true;
      }
      return false;
    }
    return true;
    //
    // Old version
    /*  return (!showForTemplateOnly || !descriptionAvailable
    || (etomoColumn != null && etomoColumn.isRecommended()))
    && (!showIncludedOnly || ((!isEmpty() && !isTemplateValue()) || isOverride()));*/
  }

  private void updateVisible() {
    boolean visible = open && canDisplay();
    hTitle.setVisible(visible);
    if (bcbValue != null) {
      bcbValue.setVisible(visible);
    }
    else if (cbValue != null) {
      cbValue.setVisible(visible);
    }
    else if (bctfValue != null) {
      bctfValue.setVisible(visible);
    }
    else if (tfValue != null) {
      tfValue.setVisible(visible);
    }
    if (hEmpty != null) {
      hEmpty.setVisible(visible);
    }
    if (tbtnOverrideToggle != null) {
      tbtnOverrideToggle.setVisible(visible);
    }
  }

  void setOpen(final boolean open) {
    this.open = open;
    updateVisible();
  }

  private void setTooltips(final String[] lineArray) {
    String note = DirectiveDescrElement.getNote(lineArray);
    hTitle.setTooltip(
      directiveDef.toString() + " - " + DirectiveDescrElement.getDescription(lineArray)
        + " - " + DirectiveDescrElement.getValueType(lineArray)
        + (note == null || note.isEmpty() ? "" : " - " + note));
  }
}