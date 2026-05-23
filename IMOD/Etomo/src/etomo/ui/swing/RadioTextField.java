package etomo.ui.swing;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;

import etomo.EtomoDirector;
import etomo.storage.DirectiveDef;
import etomo.type.ConstEtomoNumber;
import etomo.type.EnumeratedType;
import etomo.type.ParsedElement;
import etomo.ui.BooleanTextFieldInterface;
import etomo.ui.Field;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldSettingBundle;
import etomo.ui.FieldSettingInterface;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.util.Utilities;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2006 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *          <p/>
 *          <p> $Log$
 *          <p> Revision 1.1  2010/11/13 16:07:34  sueh
 *          <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 *          <p>
 *          <p> Revision 1.10  2010/03/03 05:06:31  sueh
 *          <p> bug# 1311 Changed TextField.setSize to setPreferredSize.
 *          <p>
 *          <p> Revision 1.9  2009/11/20 17:31:49  sueh
 *          <p> bug# 1282 Changed validate to handle the name prefixes.
 *          <p>
 *          <p> Revision 1.8  2009/09/01 03:18:25  sueh
 *          <p> bug# 1222
 *          <p>
 *          <p> Revision 1.7  2009/02/19 01:45:50  sueh
 *          <p> bug# 1178 In setToolTipText stop formatting tooltip twice.
 *          <p>
 *          <p> Revision 1.6  2009/02/19 01:44:19  sueh
 *          <p> bug# 1178 In setToolTipText stop formatting tooltip twice.
 *          <p>
 *          <p> Revision 1.5  2008/11/20 01:46:57  sueh
 *          <p> bug# 1147 Commented out code that was not in use.
 *          <p>
 *          <p> Revision 1.4  2007/04/13 20:38:46  sueh
 *          <p> bug# 964Added EnumeratedType, which is the interface for enumeration types.
 *          <p>
 *          <p> Revision 1.3  2007/03/30 23:52:28  sueh
 *          <p> bug# 964 Switched from JTextField to etomo.ui.TextField, which names itself.
 *          <p>
 *          <p> Revision 1.2  2007/03/07 21:13:18  sueh
 *          <p> bug# 981 Turned RadioButton into a wrapper rather then a child of JRadioButton,
 *          <p> because it is getting more complicated.  Added radioValue - a way to assign an
 *          <p> integer value to each radio button in a group.
 *          <p>
 *          <p> Revision 1.1  2007/03/03 01:05:57  sueh
 *          <p> bug# 973 Class combining a RadioButton and JTextField.  Turns off the
 *          <p> JTextField when the radio button is not selected.
 *          <p> </p>
 */
public final class RadioTextField
  implements RadioButtonInterface, BooleanTextFieldInterface {
  private final JPanel rootPanel = new JPanel();
  private final RadioButton radioButton;
  private final TextField textField;

  private boolean debug = false;
  private DirectiveDef directiveDef = null;
  private boolean enabled = true;
  private boolean editable = true;

  /**
   * Constructs local instance, adds listener, and returns.
   *
   * @param label
   * @param group
   * @return
   */
  static RadioTextField getInstance(final FieldType fieldType, final String label,
    final ButtonGroup group) {
    return new RadioTextField(fieldType, label, null, group, null);
  }

  public static RadioTextField getInstance(final FieldType fieldType,
    final EnumeratedType enumeratedType, final ButtonGroup group) {
    return new RadioTextField(fieldType, null, enumeratedType, group, null);
  }

  public static RadioTextField getInstance(final FieldType fieldType, final String label,
    final ButtonGroup group, String locationDescr) {
    return new RadioTextField(fieldType, label, null, group, locationDescr);
  }

  static RadioTextField getInstance(final FieldType fieldType,
    final EnumeratedType enumeratedType, final ButtonGroup group, String locationDescr) {
    return new RadioTextField(fieldType, null, null, group, locationDescr);
  }

  private RadioTextField(final FieldType fieldType, final String label,
    final EnumeratedType enumeratedType, final ButtonGroup group,
    final String locationDescr) {
    radioButton = new RadioButton(label, enumeratedType, group,
      new RadioButton.RadioButtonModel(this));
    textField = new TextField(fieldType, label, locationDescr);
    init();
  }

  private void init() {
    rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.X_AXIS));
    rootPanel.add(radioButton.getComponent());
    rootPanel.add(textField.getComponent());
    updateDisplay();
  }

  public String getName() {
    return radioButton.getName();
  }

  public Field getField() {
    return this;
  }

  public boolean isText() {
    return true;
  }

  public boolean isBoolean() {
    return true;
  }

  public boolean isDebug() {
    return debug || EtomoDirector.INSTANCE.getArguments().isDebug();
  }

  public boolean equalsSelectedStringValue(final String value) {
    return radioButton.equalsSelectedStringValue(value);
  }

  void setTextPreferredWidth(final double minWidth) {
    Dimension prefSize = textField.getPreferredSize();
    prefSize.setSize(minWidth, prefSize.getHeight());
    textField.setTextPreferredSize(prefSize);
  }

  public Container getContainer() {
    return rootPanel;
  }

  public void setText(final int value) {
    textField.setText(String.valueOf(value));
  }

  void setText(final long value) {
    textField.setText(String.valueOf(value));
  }

  void setText(final double value) {
    textField.setText(String.valueOf(value));
  }

  void setText(final ParsedElement value) {
    if (value == null) {
      textField.setText("");
    }
    else {
      textField.setText(value.getRawString());
    }
  }

  public void setText(final String text) {
    textField.setText(text);
  }

  public void setText(final String text, final boolean allowEmpty) {
    if (allowEmpty || (text != null && !text.isEmpty())) {
      setText(text);
    }
  }

  public void backup() {
    radioButton.backup();
    textField.backup();
  }

  /**
   * If a field was backed up, make the backup value the displayed value, and turn off
   * the back up.  This has no effect on a radio button with a backupValue of false,
   * other then to turn off the backup.
   */
  public void restoreFromBackup() {
    radioButton.restoreFromBackup();
    textField.restoreFromBackup();
    updateDisplay();
  }

  public void clear() {
    radioButton.clear();
    textField.clear();
    updateDisplay();
  }

  public void setValue(final Field input) {
    radioButton.setValue(input);
    textField.setValue(input);
    updateDisplay();
  }

  public void setValue(final String value) {
    textField.setValue(value);
  }

  public void setValue(final boolean value) {
    radioButton.setValue(value);
    updateDisplay();
  }

  public void setDirectiveDef(final DirectiveDef directiveDef) {
    this.directiveDef = directiveDef;
  }

  public void useDefaultValue() {
    radioButton.useDefaultValue();
    textField.useDefaultValue();
    updateDisplay();
  }

  public boolean equalsDefaultValue() {
    return radioButton.equalsDefaultValue() && textField.equalsDefaultValue();
  }

  public boolean equalsDefaultValue(final String value) {
    return radioButton.equalsDefaultValue(value) && textField.equalsDefaultValue(value);
  }

  public DirectiveDef getDirectiveDef() {
    return directiveDef;
  }

  public boolean isFieldHighlightSet() {
    return radioButton.isFieldHighlightSet() || textField.isFieldHighlightSet();
  }

  public void setFieldHighlight(final String text) {
    textField.setFieldHighlight(text);
  }

  public void setFieldHighlight(final boolean bool) {
    radioButton.setFieldHighlight(bool);
  }

  public FieldSettingInterface getFieldHighlight() {
    FieldSettingBundle bundle = new FieldSettingBundle();
    bundle.addBooleanSetting(radioButton.getFieldHighlight());
    bundle.addTextSetting(textField.getFieldHighlight());
    return bundle;
  }

  public void setFieldHighlight(final FieldSettingInterface input) {
    radioButton.setFieldHighlight(input);
    textField.setFieldHighlight(input);
  }

  public void clearFieldHighlight() {
    textField.clearFieldHighlight();
    radioButton.clearFieldHighlight();
  }

  public boolean equalsFieldHighlight() {
    return textField.equalsFieldHighlight() && radioButton.equalsFieldHighlight();
  }

  public boolean equalsFieldHighlight(final String value) {
    return textField.equalsFieldHighlight(value)
      && radioButton.equalsFieldHighlight(value);
  }

  public void checkpoint() {
    radioButton.checkpoint();
    textField.checkpoint();
  }

  public void setCheckpoint(final FieldSettingInterface input) {
    radioButton.setCheckpoint(input);
    textField.setCheckpoint(input);
  }

  public FieldSettingInterface getCheckpoint() {
    FieldSettingBundle bundle = new FieldSettingBundle();
    bundle.addBooleanSetting(radioButton.getCheckpoint());
    bundle.addTextSetting(textField.getCheckpoint());
    return bundle;
  }

  public boolean isDifferentFromCheckpoint(final boolean alwaysCheck) {
    return radioButton.isDifferentFromCheckpoint(alwaysCheck)
      || textField.isDifferentFromCheckpoint(alwaysCheck);
  }

  void setDebug(final boolean debug) {
    this.debug = debug;
    radioButton.setDebug(debug);
  }

  void setText(final ConstEtomoNumber text) {
    textField.setText(text.toString());
  }

  public String getLabel() {
    return radioButton.getText();
  }

  public String getDescription() {
    return getQuotedLabel();
  }

  public String getQuotedLabel() {
    return Utilities.quoteLabel(getLabel());
  }

  void setRequired(final boolean required) {
    textField.setRequired(required);
  }

  public String getText(final boolean doValidation)
    throws FieldValidationFailedException {
    return getText(doValidation, null);
  }

  public boolean isRequired() {
    return textField.isRequired();
  }

  public String getText(final boolean doValidation, final FieldDisplayer fieldDisplayer)
    throws FieldValidationFailedException {
    String text = textField.getText(doValidation, fieldDisplayer);
    if (text == null || text.matches("\\s*")) {
      return "";
    }
    return text;
  }

  /**
   * return text without validation
   *
   * @return
   */
  public String getText() {
    String text = textField.getText();
    if (text == null || text.matches("\\s*")) {
      return "";
    }
    return text;
  }

  public EnumeratedType getEnumeratedType() {
    return radioButton.getEnumeratedType();
  }

  public boolean isSelected() {
    return radioButton.isSelected();
  }

  public boolean isEmpty() {
    String text = textField.getText();
    return text == null || text.matches("\\s*");
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    // Only visually enabled if both enabled and editable
    radioButton.setEnabled(enabled && editable);
    updateDisplay();
  }

  private void updateDisplay() {
    textField.setEnabled(enabled && radioButton.isSelected());
  }

  void setEditable(final boolean editable) {
    this.editable = editable;
    // Editable has no visible effect if the button is disabled.
    if (enabled) {
      radioButton.setEnabled(editable);
      textField.setEditable(editable);
    }
  }

  public void setVisible(final boolean visible) {
    radioButton.setVisible(visible);
    textField.setVisible(visible);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void msgSelected() {
    updateDisplay();
  }

  public void setSelected(boolean selected) {
    radioButton.setSelected(selected);
    updateDisplay();
  }

  /**
   * Only sets when selected is not null or empty
   * @param selected
   */
  void setSelected(final ConstEtomoNumber selected, final boolean allowEmpty) {
    if (allowEmpty || (selected != null && !selected.isNull())) {
      setSelected(selected.is());
    }
  }

  public void setToolTipText(final String text) {
    radioButton.setToolTipText(text);
    textField.setToolTipText(text);
  }

  void setRadioButtonToolTipText(final String text) {
    radioButton.setToolTipText(text);
  }

  void setTextFieldToolTipText(final String text) {
    textField.setToolTipText(text);
  }

  public void setTooltip(final Field field) {
    if (field != null) {
      String tooltip = field.getTooltip();
      radioButton.setPreformattedTooltip(tooltip);
      textField.setPreformattedTooltip(tooltip);
    }
  }

  public String getTooltip() {
    return textField.getTooltip();
  }

  public void addActionListener(ActionListener actionListener) {
    radioButton.addActionListener(actionListener);
  }

  public String getActionCommand() {
    return radioButton.getActionCommand();
  }

  /**
   * @return null if instance is in a valid state
   */
  String validate() {
    if (!radioButton.getName().endsWith(textField.getName().substring(2))) {
      return "Fields should have the same name, except for the prefix";
    }
    if (!enabled && textField.isEnabled()) {
      return "Fields should enable and disable together";
    }
    if (!radioButton.isSelected() && textField.isEnabled()) {
      return "Text field should be disabled when radio button is not selected";
    }
    if (enabled && radioButton.isSelected() && !textField.isEnabled()) {
      return "text field should be enabled when radio button is selected";
    }
    return null;
  }
}
