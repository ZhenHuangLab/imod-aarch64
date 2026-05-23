package etomo.ui.swing;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JTextField;

import etomo.logic.TextFieldState;
import etomo.storage.DirectiveDef;
import etomo.type.ConstEtomoNumber;
import etomo.type.EtomoNumber;
import etomo.type.ParsedElementType;
import etomo.type.UITestFieldType;
import etomo.ui.Field;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldSettingInterface;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.TableComponent;
import etomo.ui.TextFieldInterface;
import etomo.ui.UIComponent;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2002 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *          <p/>
 *          <p> $Log$
 *          <p> Revision 1.1  2010/11/13 16:07:34  sueh
 *          <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 *          <p>
 *          <p> Revision 1.29  2010/04/10 23:30:41  sueh
 *          <p> bug# 1343 Added a FocusEventListener to select and unselect the text in
 *          <p> the text field.
 *          <p>
 *          <p> Revision 1.28  2009/09/01 03:18:25  sueh
 *          <p> bug# 1222
 *          <p>
 *          <p> Revision 1.27  2009/01/20 20:01:53  sueh
 *          <p> bug# 1102 Added getFieldType so that the ancestor class can name itself
 *          <p> and print out the name.
 *          <p>
 *          <p> Revision 1.26  2008/09/30 20:59:24  sueh
 *          <p> bug# 1113 Added toString.
 *          <p>
 *          <p> Revision 1.25  2008/08/21 00:07:22  sueh
 *          <p> bug# 1132 Added isEnabled.
 *          <p>
 *          <p> Revision 1.24  2008/06/20 20:05:50  sueh
 *          <p> bug# 1119 For clarity added _NUMBER to MATLAB and NON_MATLAB.
 *          <p>
 *          <p> Revision 1.23  2008/04/02 02:26:03  sueh
 *          <p> bug# 1097 Added ParsedElementType to work with ParsedArray correctly.
 *          <p>
 *          <p> Revision 1.22  2007/09/27 20:50:20  sueh
 *          <p> bug# 1044 Added getIneditableInstance(String value) to create an
 *          instance with
 *          <p> a value set.  Useful for debugging.
 *          <p>
 *          <p> Revision 1.21  2007/07/31 20:42:20  sueh
 *          <p> bug# 1028 Added getParsedArray().
 *          <p>
 *          <p> Revision 1.20  2007/07/18 23:21:07  sueh
 *          <p> bug# 1022 Added getEtomoNumber() and
 *          <p> getEtomoNumber(EtomoNumber.Type).
 *          <p>
 *          <p> Revision 1.19  2007/04/09 21:13:09  sueh
 *          <p> bug# 964 Added clearExpandableValues().  Clears contractedValue and
 *          <p> expandedValue, and sets the displayed value to nothing.
 *          <p>
 *          <p> Revision 1.18  2007/04/02 21:49:28  sueh
 *          <p> bug# 964 Added FieldCell.editable to make instances of FieldCell that
 *          can't be
 *          <p> edited.  This allows FieldCell.setEditable and setEnabled to be called
 *          without
 *          <p> checking whether a field should be editable.
 *          <p>
 *          <p> Revision 1.17  2007/03/27 19:30:59  sueh
 *          <p> bug# 964 Changed InputCell.setEnabled() to setEditable.  Added
 *          setEnabled().
 *          <p>
 *          <p> Revision 1.16  2007/03/26 18:38:34  sueh
 *          <p> bug# 964 Prevented getContractedValue and getExpandedValue from
 *          returning null.
 *          <p>
 *          <p> Revision 1.15  2007/03/20 23:10:15  sueh
 *          <p> bug# 964 Added fixedValues, to prevent the field from being enabled.
 *          Used by
 *          <p> getExpandableInstance().
 *          <p>
 *          <p> Revision 1.14  2007/03/01 01:34:18  sueh
 *          <p> bug# 964 Made InputCell colors constant and moved them to Colors.  Added
 *          <p> setExpandableValues, getContractedValue, and getExpandedValue.
 *          <p>
 *          <p> Revision 1.13  2007/02/09 00:49:04  sueh
 *          <p> bug# 962 Made TooltipFormatter a singleton and moved its use to
 *          low-level ui
 *          <p> classes.
 *          <p>
 *          <p> Revision 1.12  2007/02/05 23:36:06  sueh
 *          <p> bug# 962 Added getFloatValue and setValue(ConstEtomoNumber).
 *          <p>
 *          <p> Revision 1.11  2006/03/21 19:38:00  sueh
 *          <p> bug# 807 Add the ability to set and retrieve a range of integers.  Added
 *          <p> range, endValue, getEndValue(), setRangeValue().
 *          <p>
 *          <p> Revision 1.10  2005/12/14 20:55:14  sueh
 *          <p> bug# 784 Added setToolTipText().
 *          <p>
 *          <p> Revision 1.9  2005/09/01 18:00:33  sueh
 *          <p> bug# 532 Added setValue(void) to empty a field.  Removed
 *          <p> setValueEmpty().
 *          <p>
 *          <p> Revision 1.8  2005/08/27 22:36:43  sueh
 *          <p> bug# 532 Added isEmpty(), which returns true for a field that is empty or
 *          <p> only contains whitespace.
 *          <p>
 *          <p> Revision 1.7  2005/08/04 20:10:05  sueh
 *          <p> bug# 532 added getWidth().
 *          <p>
 *          <p> Revision 1.6  2005/07/19 22:31:38  sueh
 *          <p> bug# 532 changing the look of inUse == false to greyed out text.
 *          <p> Changing the look of error == true to red background.
 *          <p>
 *          <p> Revision 1.5  2005/07/11 22:57:04  sueh
 *          <p> bug# 619 Don't assume that a blank value is 0, return a null value from
 *          <p> getIntValue().
 *          <p>
 *          <p> Revision 1.4  2005/07/01 23:03:33  sueh
 *          <p> bug# 619 added getIntValue
 *          <p>
 *          <p> Revision 1.3  2005/07/01 21:16:07  sueh
 *          <p> bug# 619 Pulled an ancestor class (InputCell) out of FieldCell because we
 *          <p>  need several typs of input cells.  Added setHideValue().  Changed
 *          <p> setText() and getText() to setValue() and getValue() to standardize all
 *          <p> input cell, one of which has a label (CheckBoxCell).
 *          <p>
 *          <p> Revision 1.2  2004/11/19 23:53:44  sueh
 *          <p> bug# 520 merging Etomo_3-4-6_JOIN branch to head.
 *          <p>
 *          <p> Revision 1.1.2.2  2004/10/13 23:06:57  sueh
 *          <p> bug# 520 Changed the add() functions.  No longer relaying on the Table
 *          <p> interface because a FieldCell instance could be added to different panels.
 *          <p> The Add() functions remember what panel they where added to.  The
 *          <p> remove() requires no parameters.
 *          <p>
 *          <p> Revision 1.1.2.1  2004/10/01 19:53:42  sueh
 *          <p> bug# 520 A text field designed designed to be used with a gridbag layout.
 *          <p> It can be used with any ui object with implements Table.  It has three
 *          <p> state variables: isUse (lightens foreground when not in use), enabled
 *          <p> darkens background when not enabled), and highlighted (uses table
 *          <p> selection color for background when highlighted, setting it darker when
 *          <p> field is disabled).
 *          <p> </p>
 */
final class FieldCell extends InputCell implements ActionTarget, TableComponent,
  TextFieldInterface, UIComponent, SwingComponent {
  private final JTextField textField;
  private final ParsedElementType parsedElementType;

  private boolean inUse = true;
  private FontMetrics fontMetrics = null;
  private DirectiveDef directiveDef = null;
  private boolean enabled = true;

  private TextFieldState state;

  private FieldCell(final boolean editable, final ParsedElementType parsedElementType,
    final String rootDir) {
    this.parsedElementType = parsedElementType;
    state = new TextFieldState(editable, parsedElementType, rootDir);
    // construction
    textField = new JTextField();
    // field
    textField.setBorder(BorderFactory.createEtchedBorder());
    // color
    setBackground();
    setForeground();
    setFont();
    setExpanded();
  }

  private FieldCell(final TextFieldState state) {
    parsedElementType = null;
    this.state = new TextFieldState(state);
    // construction
    textField = new JTextField();
    // field
    textField.setBorder(BorderFactory.createEtchedBorder());
    // color
    setBackground();
    setForeground();
    setFont();
    setExpanded();
  }

  static FieldCell getInstance(final FieldCell fieldCell) {
    FieldCell instance = new FieldCell(fieldCell.state);
    instance.inUse = fieldCell.inUse;
    instance.setValue(fieldCell.getExpandedValue());
    instance.addListeners();
    instance.setToolTipText(fieldCell.textField.getToolTipText());
    return instance;
  }

  static FieldCell getEditableInstance() {
    FieldCell instance = new FieldCell(true, ParsedElementType.NON_MATLAB_NUMBER, null);
    instance.addListeners();
    return instance;
  }

  static FieldCell getEditableMatlabInstance() {
    FieldCell instance = new FieldCell(true, ParsedElementType.MATLAB_NUMBER, null);
    instance.addListeners();
    return instance;
  }

  static FieldCell getIneditableInstance() {
    boolean editable = false;
    FieldCell instance =
      new FieldCell(editable, ParsedElementType.NON_MATLAB_NUMBER, null);
    instance.setEditable(editable);
    instance.addListeners();
    return instance;
  }

  static FieldCell getExpandableInstance(String rootDir) {
    FieldCell instance =
      new FieldCell(true, ParsedElementType.NON_MATLAB_NUMBER, rootDir);
    instance.addListeners();
    return instance;
  }

  static FieldCell getExpandableIneditableInstance(final String rootDir) {
    boolean editable = false;
    FieldCell instance =
      new FieldCell(editable, ParsedElementType.NON_MATLAB_NUMBER, rootDir);
    instance.setEditable(editable);
    instance.addListeners();
    return instance;
  }

  public String getName() {
    return textField.getName();
  }

  public boolean isText() {
    return true;
  }

  public boolean isBoolean() {
    return false;
  }

  void setDebug(final boolean input) {
    if (state != null) {
      state.setDebug(input);
    }
    super.setDebug(input);
  }

  void setRootDir(final String input) {
    state = new TextFieldState(isEditable(), parsedElementType, input);
  }

  private void addListeners() {
    textField.addFocusListener(new TextFieldFocusListener(textField));
  }

  public SwingComponent getUIComponent() {
    return this;
  }

  public String toString() {
    return textField.getText();
  }

  public void setDirectiveDef(final DirectiveDef directiveDef) {
    this.directiveDef = directiveDef;
  }

  public DirectiveDef getDirectiveDef() {
    return directiveDef;
  }

  public int getPreferredWidth() {
    if (fontMetrics == null) {
      fontMetrics = UIUtilities.getFontMetrics(textField);
    }
    return UIUtilities.getPreferredWidth(textField.getText(), fontMetrics);
  }

  void setEnabled(boolean enabled) {
    this.enabled = enabled;
    textField.setEnabled(enabled && isEditable());
    setBackground();
    if (enabled && isEditable()) {
      setForeground();
    }
    else {
      textField.setForeground(Colors.CELL_DISABLED_FOREGROUND);
      textField.setDisabledTextColor(Colors.CELL_DISABLED_FOREGROUND);
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  void setEditable(boolean editable) {
    if (!editable || state.isEditableField()) {
      super.setEditable(editable);
    }
  }

  void setInUse(final boolean inUse) {
    this.inUse = inUse;
    setForeground();
  }

  public boolean isEmpty() {
    String value = textField.getText();
    return value == null || value.matches("\\s*");
  }

  public void backup() {
    System.err.println("Warning: Backup has not been implemented in FieldCell.");
  }

  public void restoreFromBackup() {}

  public void checkpoint() {
    System.err.println("Warning: Checkpoint has not been implemented in FieldCell.");
  }

  public FieldSettingInterface getCheckpoint() {
    return null;
  }

  public boolean isDifferentFromCheckpoint(final boolean alwaysCheck) {
    return false;
  }

  public void setCheckpoint(final FieldSettingInterface input) {
    System.err.println("Warning: Checkpoint has not been implemented in FieldCell.");
  }

  public boolean equalsDefaultValue() {
    return false;
  }

  public boolean equalsDefaultValue(final String value) {
    return false;
  }

  public void useDefaultValue() {
    System.err.println("Warning: Default value has not been implemented in FieldCell.");
  }

  public boolean isFieldHighlightSet() {
    return false;
  }

  public void clearFieldHighlight() {
    System.err.println("Warning: Field highlight has not been implemented in FieldCell.");
  }

  public boolean equalsFieldHighlight() {
    return false;
  }

  public boolean equalsFieldHighlight(final String value) {
    return false;
  }

  public FieldSettingInterface getFieldHighlight() {
    return null;
  }

  public void setFieldHighlight(final boolean value) {}

  public void setFieldHighlight(final FieldSettingInterface settingInterface) {
    System.err.println("Warning: Field highlight has not been implemented in FieldCell.");
  }

  public void setFieldHighlight(final String value) {
    System.err.println("Warning: Field highlight has not been implemented in FieldCell.");

  }

  public Component getComponent() {
    return textField;
  }

  public void setTargetFile(final File file) {
    setFile(file);
  }

  public void setFile(final File file) {
    setValue(file);
  }

  void setValue(final File file) {
    textField.setText(state.convertToFieldText(file));
  }

  public void setValue(final String value) {
    textField.setText(state.convertToFieldText(value));
  }

  void setValue(final String value, final boolean allowEmpty) {
    if (allowEmpty || (value != null && !value.isEmpty())) {
      setValue(value);
    }
  }

  public void setValue(final Field input) {
    if (input == null) {
      clear();
    }
    else {
      setValue(input.getText());
    }
  }

  public void setValue(final boolean value) {}

  String getContractedValue() {
    return state.convertToContractedString(textField.getText());
  }

  public String getExpandedValue() {
    return state.convertToExpandedString(textField.getText());
  }

  public File getFile() {
    String expandedValue = getExpandedValue();
    if (expandedValue != null && !expandedValue.isEmpty()) {
      return new File(expandedValue);
    }
    return null;
  }

  void expand(final boolean expand) {
    textField.setText(state.expandFieldText(expand, textField.getText()));
  }

  void setHorizontalAlignment(final int alignment) {
    textField.setHorizontalAlignment(JTextField.CENTER);
  }

  void setExpanded() {
    textField.setText(state.applyExpandedToFieldText(textField.getText()));
  }

  void setValue(ConstEtomoNumber value) {
    setValue(value.toString());
  }

  void setRangeValue(int start, int end) {
    textField.setText(state.convertRangeToFieldText(start, end));
  }

  void setValue() {
    state.msgResettingFieldText();
    textField.setText("");
  }

  public void clear() {
    state.msgResettingFieldText();
    textField.setText("");
  }

  void setValue(int value) {
    setValue(new Integer(value).toString());
  }

  void setValue(double value) {
    setValue(new Double(value).toString());
  }

  void setValue(long value) {
    setValue(new Long(value).toString());
  }

  int getEndValue() {
    return state.extractEndValue(textField.getText());
  }

  UITestFieldType getFieldType() {
    return UITestFieldType.TEXT_FIELD;
  }

  public String getDescription() {
    return "";
  }

  public String getQuotedLabel() {
    System.err.println("Warning: A label cannot be set in FieldCell.");
    return null;
  }

  String getValue() {
    return textField.getText();
  }

  public boolean isRequired() {
    return false;
  }

  /**
   * Field validation is currently not available for this class.
   */
  public String getText(final boolean doValidation, final FieldDisplayer fieldDisplayer)
    throws FieldValidationFailedException {
    return textField.getText();
  }

  public String getText() {
    return textField.getText();
  }

  public boolean isSelected() {
    return false;
  }

  int getIntValue() {
    try {
      return Integer.parseInt(textField.getText());
    }
    catch (NumberFormatException e) {
      return EtomoNumber.INTEGER_NULL_VALUE;
    }
  }

  double getDoubleValue() {
    try {
      return Double.parseDouble(textField.getText());
    }
    catch (NumberFormatException e) {
      return EtomoNumber.DOUBLE_NULL_VALUE;
    }
  }

  ConstEtomoNumber getEtomoNumber() {
    return state.convertToEtomoNumber(textField.getText());
  }

  ConstEtomoNumber getEtomoNumber(EtomoNumber.Type type) {
    return state.convertToEtomoNumber(type, textField.getText());
  }

  void setForeground() {
    if (inUse) {
      textField.setForeground(Colors.CELL_FOREGROUND);
      textField.setDisabledTextColor(Colors.CELL_FOREGROUND);
    }
    else {
      textField.setForeground(Colors.CELL_NOT_IN_USE_FOREGROUND);
      textField.setDisabledTextColor(Colors.CELL_NOT_IN_USE_FOREGROUND);
    }
  }

  int getWidth() {
    return textField.getWidth();
  }

  int getRightBorder() {
    return textField.getBorder().getBorderInsets(textField).right;
  }

  public void setToolTipText(String text) {
    textField.setToolTipText(TooltipFormatter.INSTANCE.format(text));
  }

  public void setTooltip(final Field field) {
    if (field != null) {
      textField.setToolTipText(field.getTooltip());
    }
  }

  public String getTooltip() {
    return textField.getToolTipText();
  }

  public boolean equals(String comp) {
    return getValue().equals(comp);
  }

  private static final class TextFieldFocusListener implements FocusListener {
    private final JTextField textField;

    private TextFieldFocusListener(final JTextField textField) {
      this.textField = textField;
    }

    public void focusGained(final FocusEvent focusEvent) {
      textField.selectAll();
    }

    public void focusLost(final FocusEvent focusEvent) {
      textField.select(0, 0);
    }
  }
}