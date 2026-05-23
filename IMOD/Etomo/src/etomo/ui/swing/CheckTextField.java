package etomo.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import etomo.EtomoDirector;
import etomo.logic.FieldValidator;
import etomo.storage.DirectiveDef;
import etomo.type.ConstEtomoNumber;
import etomo.type.EtomoNumber;
import etomo.ui.BooleanTextFieldInterface;
import etomo.ui.Field;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldSettingBundle;
import etomo.ui.FieldSettingInterface;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.UIComponent;
import etomo.util.Utilities;

/**
 * <p>Description: A self-naming check box and text field.  The text field is enabled only
 * when the check box is checked.  Implements StateChangeSource with its state equal to
 * whether it has changed since it was checkpointed.</p>
 * 
 * <p>Copyright: Copyright 2010 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.6  2011/05/03 03:13:51  sueh
 * <p> bug# 1416 Checkpointing from a parameter instead of isSelected().  Removed the reporter (phased out).
 * <p>
 * <p> Revision 1.5  2011/04/25 23:36:03  sueh
 * <p> bug# 1416 Implemented StateChangeActionSource.  Added equals(Object) and equals(Document) so
 * <p> StateChangedReporter can find instances of this class.  Changed isChanged to isDifferentFromCheckpoint.
 * <p> Added getState.
 * <p>
 * <p> Revision 1.4  2011/04/04 17:17:27  sueh
 * <p> bug# 1416 Added savedValue, checkpoint, isChanged.
 * <p>
 * <p> Revision 1.3  2011/02/22 18:05:48  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.2  2010/12/05 04:58:39  sueh
 * <p> bug# 1416 Added setEnabled.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.2  2010/03/05 04:02:32  sueh
 * <p> bug# 1319 Added setLabel.
 * <p>
 * <p> Revision 1.1  2010/03/03 05:02:42  sueh
 * <p> bug# 1311 A checkbox which enables/disables a text field.
 * <p> </p>
 */
public final class CheckTextField
  implements UIComponent, SwingComponent, BooleanTextFieldInterface {
  private final JPanel pnlRoot = new JPanel();
  private final CheckBox checkBox;
  private final TextField textField;
  private final String label;
  private final EtomoNumber.Type numericType;
  private final FieldType fieldType;

  private boolean required = false;
  private DirectiveDef directiveDef = null;
  private boolean debug = false;

  private CheckTextField(final FieldType fieldType, final String label,
    final EtomoNumber.Type numericType) {
    textField = new TextField(fieldType, label, null);
    this.label = label;
    this.numericType = numericType;
    this.fieldType = fieldType;
    checkBox = new CheckBox();
    setLabel(label);
  }

  public static CheckTextField getInstance(final FieldType fieldType,
    final String label) {
    CheckTextField instance = new CheckTextField(fieldType, label, null);
    instance.createPanel();
    instance.updateDisplay();
    instance.addListeners();
    return instance;
  }

  public static CheckTextField getNumericInstance(final FieldType fieldType,
    final String tfLabel, final EtomoNumber.Type numericType) {
    CheckTextField instance = new CheckTextField(fieldType, tfLabel, numericType);
    instance.createPanel();
    instance.updateDisplay();
    instance.addListeners();
    return instance;
  }

  public String getName() {
    return checkBox.getName();
  }

  public boolean equals(final Object object) {
    return object == checkBox || object == textField;
  }

  public boolean equals(final Document document) {
    return textField.getDocument() == document;
  }

  void setLabel(final String label) {
    checkBox.setText(label);
    textField.setName(label);
  }

  public void setAlternateLabel(final String label) {
    checkBox.setAlternateText(label);
    textField.setName(label);
  }

  public void switchLabels(final boolean alternate) {
    checkBox.switchText(alternate);
    // Set text field name to newly set text
    textField.setName(checkBox.getText());
  }

  /**
   * Checkpoints checkbox from checkboxValue.  Saves textValue as the text field
   * checkpoint.
   */
  void checkpoint(final boolean checkboxValue, final String textValue) {
    checkBox.checkpoint(checkboxValue);
    textField.checkpoint(textValue);
  }

  public void checkpoint() {
    checkBox.checkpoint();
    textField.checkpoint();
  }

  /**
   * Resets to checkpointValue if checkpointValue has been set.  Otherwise has no effect.
   */
  void resetToCheckpoint() {
    checkBox.resetToCheckpoint();
    textField.resetToCheckpoint();
  }

  void setColumns() {
    if (fieldType != null) {
      textField.setColumns();
    }
  }

  /**
   * @return true if checkBox is visible, enabled, and different from checkpoint or text field is visible, enabled, and different from checkpoint
   */
  boolean isDifferentFromCheckpoint() {
    return checkBox.isDifferentFromCheckpoint(false)
      || textField.isDifferentFromCheckpoint(false);
  }

  void setDebug(final boolean input) {
    debug = input;
    textField.setDebug(input);
  }

  void setEnabled(final boolean enable) {
    checkBox.setEnabled(enable);
    updateDisplay();
  }

  private void updateDisplay() {
    // checkBox can handle enabled versus editable
    textField.setEnabled(checkBox.isEnabled() && checkBox.isSelected());
  }

  void setEditable(final boolean editable) {
    // checkBox can handle enabled versus editable
    checkBox.setEditable(editable);
    textField.setEditable(editable);
  }

  public boolean isEnabled() {
    return checkBox.isEnabled();
  }

  private void createPanel() {
    // root panel
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.X_AXIS));
    pnlRoot.add(checkBox.getComponent());
    pnlRoot.add(textField.getComponent());
  }

  public void setToolTipText(final String text) {
    textField.setToolTipText(text);
    checkBox.setToolTipText(text);
  }

  /**
   * Stores a second tooltip.  Does not switch to the second tooltip.
   * @see switchTooltips
   */
  public void setAlternateTooltipText(final String text) {
    textField.setAlternateTooltipText(text);
    checkBox.setAlternateTooltipText(text);
  }

  /**
   * Switch to/from the alternate tooltip.
   * @param alternate
   */
  public void switchTooltips(final boolean alternate) {
    textField.switchTooltips(alternate);
    checkBox.switchTooltips(alternate);
  }

  public void setVisible(final boolean visible) {
    pnlRoot.setVisible(visible);
  }

  public void setDirectiveDef(final DirectiveDef directiveDef) {
    this.directiveDef = directiveDef;
  }

  public void backup() {
    checkBox.backup();
    textField.backup();
  }

  public void clear() {
    checkBox.clear();
    textField.clear();
    updateDisplay();
  }

  public void clearFieldHighlight() {
    checkBox.clearFieldHighlight();
    textField.clearFieldHighlight();
  }

  public boolean equalsDefaultValue() {
    return checkBox.equalsDefaultValue() && textField.equalsDefaultValue();
  }

  public boolean equalsDefaultValue(final String value) {
    return checkBox.equalsDefaultValue(value) && textField.equalsDefaultValue(value);
  }

  public boolean equalsFieldHighlight() {
    return checkBox.equalsFieldHighlight() && textField.equalsFieldHighlight();
  }

  public boolean equalsFieldHighlight(final String value) {
    return checkBox.equalsFieldHighlight(value) && textField.equalsFieldHighlight(value);
  }

  public boolean equalsSelectedStringValue(final String value) {
    return checkBox.equalsSelectedStringValue(value);
  }

  public String getTooltip() {
    return textField.getTooltip();
  }

  public boolean isBoolean() {
    return true;
  }

  public boolean isDebug() {
    return debug || EtomoDirector.INSTANCE.getArguments().isDebug();
  }

  public boolean isDifferentFromCheckpoint(final boolean alwaysCheck) {
    return checkBox.isDifferentFromCheckpoint(alwaysCheck)
      || textField.isDifferentFromCheckpoint(alwaysCheck);
  }

  public FieldSettingInterface getCheckpoint() {
    FieldSettingBundle bundle = new FieldSettingBundle();
    bundle.addBooleanSetting(checkBox.getCheckpoint());
    bundle.addTextSetting(textField.getCheckpoint());
    return bundle;
  }

  public DirectiveDef getDirectiveDef() {
    return directiveDef;
  }

  public FieldSettingInterface getFieldHighlight() {
    FieldSettingBundle bundle = new FieldSettingBundle();
    bundle.addBooleanSetting(checkBox.getFieldHighlight());
    bundle.addTextSetting(textField.getFieldHighlight());
    return bundle;
  }

  public boolean isEmpty() {
    String text = textField.getText();
    return text == null || text.matches("\\s*");
  }

  public boolean isFieldHighlightSet() {
    return checkBox.isFieldHighlightSet() || textField.isFieldHighlightSet();
  }

  public boolean isRequired() {
    return textField.isRequired();
  }

  public boolean isText() {
    return true;
  }

  /**
   * If a field was backed up, make the backup value the displayed value, and turn off
   * the back up.  This has no effect on a check box with a backupValue of false,
   * other then to turn off the backup.
   */
  public void restoreFromBackup() {
    checkBox.restoreFromBackup();
    textField.restoreFromBackup();
    updateDisplay();
  }

  public void setCheckpoint(final FieldSettingInterface input) {
    checkBox.setCheckpoint(input);
    textField.setCheckpoint(input);
  }

  public void setFieldHighlight(final boolean bool) {
    checkBox.setFieldHighlight(bool);
  }

  public void setFieldHighlight(final FieldSettingInterface input) {
    checkBox.setFieldHighlight(input);
    textField.setFieldHighlight(input);
  }

  public void setFieldHighlight(final String text) {
    textField.setFieldHighlight(text);
  }

  public void setTooltip(final Field field) {
    if (field != null) {
      String tooltip = field.getTooltip();
      checkBox.setPreformattedTooltip(tooltip);
      textField.setPreformattedTooltip(tooltip);
    }
  }

  public void setValue(final boolean value) {
    checkBox.setValue(value);
    updateDisplay();
  }

  public void setValue(final Field input) {
    checkBox.setValue(input);
    textField.setValue(input);
    updateDisplay();
  }

  public void setValue(final String value) {
    textField.setValue(value);
  }

  public void useDefaultValue() {
    checkBox.useDefaultValue();
    textField.useDefaultValue();
    updateDisplay();
  }

  void setCheckBoxToolTipText(final String text) {
    checkBox.setToolTipText(text);
  }

  void setFieldToolTipText(final String text) {
    textField.setToolTipText(text);
  }

  Component getRootComponent() {
    return pnlRoot;
  }

  private void addListeners() {
    checkBox.addActionListener(new CheckTextFieldActionListener(this));
  }

  void addActionListener(ActionListener actionListener) {
    checkBox.addActionListener(actionListener);
  }

  void addDocumentListener(DocumentListener listener) {
    textField.getDocument().addDocumentListener(listener);
  }

  public void setText(final String input) {
    textField.setText(input);
  }

  void setText(final String input, final boolean nonempty) {
    if (!nonempty || (input != null && !input.isEmpty())) {
      setText(input);
    }
  }

  String getActionCommand() {
    return checkBox.getActionCommand();
  }

  public String getLabel() {
    return label;
  }

  public void setSelected(final boolean selected) {
    checkBox.setSelected(selected);
    updateDisplay();
  }

  void setSelected(final ConstEtomoNumber selected, final boolean allowEmpty) {
    if (allowEmpty || (selected != null && !selected.isNull())) {
      setSelected(selected.is());
    }
  }

  public boolean isSelected() {
    return checkBox.isSelected();
  }

  public SwingComponent getUIComponent() {
    return this;
  }

  public Component getComponent() {
    return pnlRoot;
  }

  void setRequired(final boolean required) {
    this.required = required;
  }

  public String getText(final boolean doValidation)
    throws FieldValidationFailedException {
    return getText(doValidation, null);
  }

  public String getText(final boolean doValidation, final FieldDisplayer fieldDisplayer)
    throws FieldValidationFailedException {
    String text = textField.getText();
    if (doValidation && textField.isEnabled()) {
      text = FieldValidator.validateText(text, fieldType, this, getDescription(),
        required, false, false, null, fieldDisplayer);
    }
    return text;
  }

  public String getDescription() {
    return getQuotedLabel();
  }

  /**
   * Get text without validation
   */
  public String getText() {
    return textField.getText();
  }

  public String getQuotedLabel() {
    return Utilities.quoteLabel(checkBox.getText());
  }

  void setTextPreferredWidth(int width) {
    Dimension size = textField.getSize();
    size.width = width;
    textField.setPreferredSize(size);
  }

  public void setTextFieldVisible(boolean visible) {
    textField.setVisible(visible);
  }

  private void action() {
    updateDisplay();
  }

  private static final class CheckTextFieldActionListener implements ActionListener {
    private final CheckTextField checkTextField;

    private CheckTextFieldActionListener(final CheckTextField checkTextField) {
      this.checkTextField = checkTextField;
    }

    public void actionPerformed(final ActionEvent actionEvent) {
      checkTextField.action();
    }
  }
}
