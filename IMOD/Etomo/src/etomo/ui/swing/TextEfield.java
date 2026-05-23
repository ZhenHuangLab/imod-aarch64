package etomo.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import etomo.EtomoDirector;
import etomo.logic.FieldValidator;
import etomo.storage.DirectiveDef;
import etomo.storage.DirectiveValueType;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.UITestFieldType;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.FlagDisplay;
import etomo.ui.FlagOriginListener;
import etomo.ui.FlagType;
import etomo.ui.TextFlagExtension;
import etomo.ui.TextFlagOrigin;
import etomo.ui.TextStateExtension;
import etomo.ui.UIComponent;
import etomo.ui.ValueManipulationField;
import etomo.ui.ValueManipulationListener;
import etomo.util.Utilities;

/**
 * <p>Description: An extensible JTextField.</p>
 * 
 * <p>Copyright: Copyright 2017 - 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
class TextEfield implements TextEfieldInterface, UIComponent, SwingComponent,
  TextFlagOrigin, ValueManipulationField, ControlTarget {
  private final JTextField textField = new JTextField();

  private final FieldType fieldType;
  private final String labelText;
  private final ControlComponentModule controlComponent;
  final EfieldContainer container;
  private final JLabel label;

  private DirectiveDef directiveDef = null;
  private TextStateExtension stateExtension = null;
  private GridBagExtension gridBagExtension = null;
  AppearanceExtension appearanceExtension = null;
  private TextFlagExtension flagExtension = null;
  private ValueManipulationExtension valueManipulationExtension = null;
  private ValidationExtension validationExtension = null;
  private boolean debug = false;
  private ControlState enableControlState = null;
  private ArrayList<ControlListener> controlListeners = null;

  /**
   * 
   * @param label
   * @param editableField
   * @param fieldType
   * @param filePathSizeLimit - positive number limits the display size of a file name
   */
  TextEfield(final String labelText, final FieldType fieldType, final boolean useLabel,
    final boolean useControlComponent, final boolean enabledField,
    final boolean editableComponent, final boolean useGridBag) {
    this.fieldType = fieldType;
    this.labelText = labelText;
    // Add a label and control component as needed.
    if (useLabel) {
      label = new JLabel(labelText);
    }
    else {
      label = null;
    }
    if (useControlComponent) {
      controlComponent = new ControlComponentModule();
    }
    else {
      controlComponent = null;
    }
    setName();
    // Use the appearance extension if controlling the appearance characteristics will not
    // be simple.
    if (!enabledField || !editableComponent) {
      createAppearanceExtension(enabledField, editableComponent);
    }
    container = new EfieldContainer(false, useGridBag, label, textField,
      controlComponent != null ? controlComponent.getComponent() : null);
  }

  static TextEfield getInstance(final String labelText, final FieldType fieldType) {
    return new TextEfield(labelText, fieldType, false, false, true, true, false);
  }

  static TextEfield getLabeledInstance(final String labelText,
    final FieldType fieldType) {
    return new TextEfield(labelText, fieldType, true, false, true, true, false);
  }

  static final TextEfield getOverrideInstance(final String labelText,
    final DirectiveValueType valueType) {
    return new TextEfield(labelText, FieldType.getInstance(valueType), false, true, true,
      true, false);
  }

  static TextEfield getDisabledInstance(final String labelText,
    final FieldType fieldType) {
    return new TextEfield(labelText, fieldType, false, false, false, true, false);
  }

  private void setName() {
    String name = Utilities.convertLabelToName(labelText);
    if (name != null) {
      textField.setName(
        UITestFieldType.TEXT_FIELD.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
      if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
        System.out
          .println(textField.getName() + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
      }
    }
  }

  public String getLabel() {
    return labelText;
  }

  public String toString() {
    return labelText;
  }

  public void setDebug(final boolean debug) {
    this.debug = debug;
    if (appearanceExtension != null) {
      appearanceExtension.setDebug(debug);
    }
  }

  public final SwingComponent getUIComponent() {
    return this;
  }

  public Component getComponent() {
    return container.getComponent();
  }

  final void setColumns() {
    if (fieldType != null) {
      textField.setColumns(fieldType.getColumns());
    }
  }

  void setPreferredSize(final Dimension fieldSize, final int newWidth) {
    textField.setPreferredSize(UIParameters.adjustSize(fieldSize, newWidth));
  }

  Dimension getPreferredSize() {
    return textField.getPreferredSize();
  }

  public final File getFile() {
    String text = getText();
    if (text != null && !text.isEmpty()) {
      return new File(text);
    }
    return null;
  }

  public final String getText() {
    if (!isOverride()) {
      String text = textField.getText();
      if (valueManipulationExtension != null) {
        return valueManipulationExtension.getFilePath(text);
      }
      return text;
    }
    return "";
  }

  public final boolean isEmpty() {
    String text = getText();
    return text != null && text.matches("\\s*");
  }

  void addFocusListener(final FocusListener listener) {
    textField.addFocusListener(listener);
  }

  public final boolean equals(final String compareText) {
    String text = getText();
    if (text == null && compareText == null) {
      return true;
    }
    if (text == null || compareText == null) {
      return false;
    }
    if (text.isEmpty() && compareText.isEmpty()) {
      return true;
    }
    return text.trim().equals(compareText.trim());
  }

  void setTooltip(final String text) {
    textField.setToolTipText(TooltipFormatter.INSTANCE.format(text));
    if (label != null) {
      label.setToolTipText(TooltipFormatter.INSTANCE.format(text));
    }
    if (controlComponent != null) {
      controlComponent.setTooltip(text);
    }
  }

  final void setVisible(final boolean visible) {
    getComponent().setVisible(visible);
  }

  public final boolean isVisible() {
    return getComponent().isVisible();
  }

  void setFile(final String filePath) {
    if (filePath != null && !filePath.matches("\\s*")) {
      setText(new File(filePath));
    }
    else {
      clear();
    }
  }

  public final void setText(final File file) {
    if (valueManipulationExtension != null) {
      setText(valueManipulationExtension.createDisplayedFilePath(file));
    }
    else if (file == null) {
      setText("");
    }
    else {
      String absolutePath = file.getAbsolutePath();
      setText(absolutePath != null ? absolutePath : file.getName());
    }
  }

  public final void setText(String text) {
    if (text == null || text.equals("")) {
      clear();
      return;
    }
    textField.setText(text);
    updateFlagExtension();
  }

  public final DirectiveDef getDirectiveDef() {
    return directiveDef;
  }

  final void setDirectiveDef(final DirectiveDef directiveDef) {
    this.directiveDef = directiveDef;
  }

  boolean isTemplateValue() {
    FlagType flagType = getFlagType();
    return flagType != null && flagType.isTemplate();
  }

  /// control

  boolean isOverride() {
    if (controlComponent == null) {
      return false;
    }
    return controlComponent.isOverride();
  }

  public final void setComponentControl(boolean control,
    final ControlState controlState) {
    if (controlComponent == null) {
      return;
    }
    textField.setVisible(!controlComponent.setComponentControl(control, controlState));
  }

  void addControlListener(final ControlListener listener) {
    if (listener == null) {
      return;
    }
    if (controlListeners == null) {
      controlListeners = new ArrayList<ControlListener>();
    }
    controlListeners.add(listener);
  }

  public void sendControlEvent() {
    if (controlListeners != null) {
      Iterator<ControlListener> iterator = controlListeners.iterator();
      while (iterator.hasNext()) {
        iterator.next().controlEvent();
      }
    }
  }

  public final void setEnableControl(boolean control, final ControlState controlState) {
    if (controlState == null) {
      control = false;
    }
    if (appearanceExtension == null && control) {
      createAppearanceExtension(true, true);
    }
    if (control) {
      enableControlState = controlState;
    }
    else {
      enableControlState = null;
    }
    if (appearanceExtension != null) {
      appearanceExtension.setEnableControlState(enableControlState);
    }
  }

  /// valueManipulationExtension

  public final void clear() {
    textField.setText("");
    if (valueManipulationExtension != null) {
      valueManipulationExtension.clearFilePath();
      valueManipulationExtension.substitute();
    }
    updateFlagExtension();
  }

  public final void setSubstituteText(final String text) {
    textField.setText(text);
  }

  public final void
    addValueManipulationListener(final ValueManipulationListener listener) {
    textField.addFocusListener(listener);
  }

  /// validation & ValidationExtension

  final void setRequired(final boolean required) {
    if (required && validationExtension == null) {
      validationExtension = new ValidationExtension();
    }
    if (validationExtension != null) {
      validationExtension.setRequired(required);
    }
  }

  final void setLocationDescr(final String locationDescr) {
    if (locationDescr != null && validationExtension == null) {
      validationExtension = new ValidationExtension();
    }
    if (validationExtension != null) {
      validationExtension.setLocationDescr(locationDescr);
    }
  }

  final void setFileMustExist(final boolean fileMustExist) {
    if (fileMustExist && validationExtension == null) {
      validationExtension = new ValidationExtension();
    }
    if (validationExtension != null) {
      validationExtension.setFileMustExist(fileMustExist);
    }
  }

  final void setFileOnly(final boolean fileOnly) {
    if (fileOnly && validationExtension == null) {
      validationExtension = new ValidationExtension();
    }
    if (validationExtension != null) {
      validationExtension.setFileOnly(fileOnly);
    }
  }

  final void setMustBePositive(final boolean mustBePositive) {
    if (mustBePositive && validationExtension == null) {
      validationExtension = new ValidationExtension();
    }
    if (validationExtension != null) {
      validationExtension.setMustBePositive(mustBePositive);
    }
  }

  final String getText(final boolean doValidation) throws FieldValidationFailedException {
    return getText(doValidation, null, null);
  }

  // for non standard validation only member variables fileNustExist, fileOnly,
  // positiveNumberOnly
  final String getText(final boolean doValidation, final FieldDisplayer fieldDisplayer1,
    final FieldDisplayer fieldDisplayer2) throws FieldValidationFailedException {
    String text = getText();
    if (!isOverride() && doValidation && isEnabled()) {
      text = FieldValidator.validateText(text, fieldType, this,
        Utilities.quoteLabel(labelText)
          + (validationExtension != null ? validationExtension.getLocationAddon() : ""),
        validationExtension, fieldDisplayer1, fieldDisplayer2, debug);
    }
    return text;
  }

  public boolean isValid() {
    if (isOverride()) {
      return true;
    }
    return FieldValidator.isTextValid(getText(), fieldType, this, validationExtension,
      debug);
  }

  /// appearanceExtension

  void createAppearanceExtension(final boolean enabledField,
    final boolean editableComponent) {
    if (appearanceExtension == null) {
      // Using the text component ability to set the field to be editable
      appearanceExtension =
        new TextComponentAppearanceExtension(textField, enabledField, editableComponent);
      appearanceExtension.setDebug(debug);
    }
    // In this class the appearanceExtension acts as the field's flag display for all
    // types of flags.
    if (flagExtension != null) {
      flagExtension.addFlagDisplay(appearanceExtension);
    }
  }

  final void setEnabled(final boolean enabled) {
    if (appearanceExtension != null) {
      appearanceExtension.setEnabled(enabled);
    }
    else {
      textField.setEnabled(enabled);
    }
    if (label != null) {
      label.setEnabled(isEnabled());
    }
  }

  public final boolean isEnabled() {
    if (appearanceExtension != null) {
      return appearanceExtension.isEnabled();
    }
    return textField.isEnabled();
  }

  void setEditable(final boolean editable) {
    if (appearanceExtension != null) {
      appearanceExtension.setEditable(editable);
    }
    else {
      textField.setEditable(editable);
    }
  }

  final boolean isEditable() {
    if (appearanceExtension != null) {
      return appearanceExtension.isEditable();
    }
    return textField.isEditable();
  }

  /**
   * @param maxFilePathSize >0 to turn on limit, <=0 to turn if off
   */
  void setLimitDisplayedFilePath(final int maxFilePathSize) {
    if (maxFilePathSize > 0 && valueManipulationExtension == null) {
      valueManipulationExtension = new ValueManipulationExtension(this, debug);
    }
    if (valueManipulationExtension != null) {
      valueManipulationExtension.setLimitDisplayedFilePath(maxFilePathSize, fieldType);
    }
  }

  /// flagExtension

  void updateFlagExtension() {
    if (flagExtension != null) {
      flagExtension.update();
    }
  }

  /**
   * Creates flagExtension if it is null.
   * @return true if flagExtension was created
   */
  boolean createFlagExtension() {
    if (flagExtension == null) {
      flagExtension = new TextFlagExtension(this);
      createAppearanceExtension(true, true);
      return true;
    }
    return false;
  }

  FlagType getFlagType() {
    if (appearanceExtension != null) {
      return appearanceExtension.getFlagType();
    }
    return null;
  }

  /**
   * Allow flags to listen for changes that they need to react to.
   */
  public final void addFlagOriginListener(final FlagOriginListener listener) {
    textField.addFocusListener(listener);
  }

  /**
   * Change appearance of the flag displays when value matches the template value.
   * Automatically adds its appearance extension as a flag display.
   * @param templateValue
   */
  final void flagTemplate(final String templateValue) {
    createFlagExtension();
    flagExtension.flagTemplate(templateValue);
    flagExtension.update();
    if (valueManipulationExtension == null) {
      valueManipulationExtension = new ValueManipulationExtension(this, debug);
    }
    valueManipulationExtension.setPreventBlank(true, templateValue);
  }

  final void setFlagErrors() {
    createFlagExtension();
    flagExtension.setFlagErrors();
    flagExtension.update();
  }

  final void clearTemplateValue() {
    if (flagExtension != null) {
      flagExtension.clearFlags();
    }
    if (valueManipulationExtension != null) {
      valueManipulationExtension.clearPreventBlank();
    }
  }

  public final void setTemplateValue() {
    if (flagExtension != null) {
      setText(flagExtension.getFlaggedTemplateValue());
    }
  }

  final void addFlagDisplay(final FlagDisplay flagDisplay) {
    createFlagExtension();
    flagExtension.addFlagDisplay(flagDisplay);
    flagExtension.update();
  }

  final void addFinalFlagDisplay(final FlagDisplay flagDisplay) {
    createFlagExtension();
    flagExtension.addFinalFlagDisplay(flagDisplay);
    flagExtension.update();
  }

  public final void setFieldHighlight(final String value) {
    flagTemplate(value);
  }

  /// stateExtension

  final void backup() {
    if (stateExtension == null) {
      stateExtension = new TextStateExtension(this);
    }
    stateExtension.backup();
  }

  final void checkpoint() {
    if (stateExtension == null) {
      stateExtension = new TextStateExtension(this);
    }
    stateExtension.checkpoint();
  }

  final boolean isDifferentFromCheckpoint(final boolean alwaysCheck) {
    if (stateExtension == null) {
      return false;
    }
    return stateExtension.isDifferentFromCheckpoint(alwaysCheck);
  }

  final void restoreFromBackup() {
    if (stateExtension != null) {
      stateExtension.restoreFromBackup();
    }
  }

  /// Calls to gridBagExtension

  final void remove() {
    if (gridBagExtension != null) {
      gridBagExtension.remove(getComponent());
    }
  }

  final void add(final JPanel panel, final GridBagLayout layout,
    final GridBagConstraints constraints) {
    if (gridBagExtension == null) {
      gridBagExtension = new GridBagExtension();
    }
    gridBagExtension.add(getComponent(), panel, layout, constraints);
  }
}