package etomo.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import etomo.EtomoDirector;
import etomo.logic.AutodocAttributeRetriever;
import etomo.logic.FieldValidator;
import etomo.logic.ValidationSet;
import etomo.storage.DirectiveDef;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.ConstEtomoNumber;
import etomo.type.EtomoNumber;
import etomo.type.UITestFieldType;
import etomo.ui.Field;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldSettingInterface;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.TextFieldInterface;
import etomo.ui.TextFieldSetting;
import etomo.ui.UIComponent;
import etomo.util.Utilities;

/**
 * <p>Description: A self-naming, labeled text field.  Implements StateChangeSource with
 * its state equal to whether it has changed since it was checkpointed.</p>
 * 
 * <p>Copyright: Copyright 2002 - 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.5  2011/04/25 23:36:21  sueh
 * <p> bug# 1416 Implemented StateChangeActionSource.  Added equals(Object) and equals(Document) so
 * <p> StateChangedReporter can find instances of this class.  Changed isChanged to isDifferentFromCheckpoint.
 * <p> Added getState.
 * <p>
 * <p> Revision 1.4  2011/04/04 17:20:46  sueh
 * <p> bug# 1416 Added savedValue, checkpoint, isChanged.
 * <p>
 * <p> Revision 1.3  2011/03/02 00:00:12  sueh
 * <p> bug# 1452 Removing image rotation conversion between float and
 * <p> double.  Using string where possible.
 * <p>
 * <p> Revision 1.2  2011/02/22 18:13:44  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 3.36  2009/11/20 17:27:33  sueh
 * <p> bug# 1282 Added prefixes to all of the field names, so that the fields that
 * <p> are actually abstract buttons (radio buttons, etc) won't be activated by a
 * <p> "bn." field command.
 * <p>
 * <p> Revision 3.35  2009/10/15 23:36:45  sueh
 * <p> bug# 1274 in isEmpty, corrected compare, which was using "|" instead of
 * <p> "||".
 * <p>
 * <p> Revision 3.34  2009/04/13 22:56:07  sueh
 * <p> Removed newstuff.
 * <p>
 * <p> Revision 3.33  2009/02/27 03:53:18  sueh
 * <p> bug# 1172 Added experimental automation recording background color
 * <p> (newstuff only).
 * <p>
 * <p> Revision 3.32  2009/01/20 20:12:23  sueh
 * <p> bug# 1102 Changed UITestField to UITestFieldType.  Simplified the name
 * <p> by removing the expanded state portion.
 * <p>
 * <p> Revision 3.31  2008/10/27 20:40:31  sueh
 * <p> bug# 1141 In setToolTipText, added the tooltip to the label.
 * <p>
 * <p> Revision 3.30  2008/05/30 22:32:10  sueh
 * <p> bug# 1102 Isolating the etomo.uitest package so it is not need for
 * <p> running EtomoDirector.
 * <p>
 * <p> Revision 3.29  2008/05/30 21:31:39  sueh
 * <p> bug# 1102 Moved uitest classes to etomo.uitest.
 * <p>
 * <p> Revision 3.28  2008/04/02 19:06:24  sueh
 * <p> bug# 1104 Allow field specific debugging in ToolTipFormatter.
 * <p>
 * <p> Revision 3.27  2007/12/26 22:24:38  sueh
 * <p> bug# 1052 Moved argument handling from EtomoDirector to a separate class.
 * <p>
 * <p> Revision 3.26  2007/09/07 00:27:19  sueh
 * <p> bug# 989 Using a public INSTANCE to refer to the EtomoDirector singleton
 * <p> instead of getInstance and createInstance.
 * <p>
 * <p> Revision 3.25  2007/06/08 22:21:47  sueh
 * <p> bug# 1014 Added clear().
 * <p>
 * <p> Revision 3.24  2007/04/13 18:44:56  sueh
 * <p> bug# 964 Added debug member variable.
 * <p>
 * <p> Revision 3.23  2007/03/27 00:04:07  sueh
 * <p> bug# 964 Removed unused functions.
 * <p>
 * <p> Revision 3.22  2007/03/01 01:39:19  sueh
 * <p> bug# 964 Moved colors from UIUtilities to Colors.
 * <p>
 * <p> Revision 3.21  2007/02/09 00:50:32  sueh
 * <p> bug# 962 Made TooltipFormatter a singleton and moved its use to low-level ui
 * <p> classes.
 * <p>
 * <p> Revision 3.20  2007/02/05 23:39:48  sueh
 * <p> bug# 962 Added setHighlight.
 * <p>
 * <p> Revision 3.19  2006/05/19 19:47:37  sueh
 * <p> bug# 866 Added setText(ConstEtomoNumber)
 * <p>
 * <p> Revision 3.18  2006/05/16 21:36:20  sueh
 * <p> bug# 856 Changing the name whenever the label is changed so that its easy to
 * <p> see what the name is.
 * <p>
 * <p> Revision 3.17  2006/05/11 19:27:44  sueh
 * <p> bug# 838 Added setMinimumWidth
 * <p>
 * <p> Revision 3.16  2006/04/25 19:15:33  sueh
 * <p> bug# 787 Added UITestField, an enum style class which contains the
 * <p> fields found in uitestaxis.adoc files.
 * <p>
 * <p> Revision 3.15  2006/04/06 20:17:04  sueh
 * <p> bug# 808 Moved the function convertLabelToName from UIUtilities to
 * <p> util.Utilities.
 * <p>
 * <p> Revision 3.14  2006/03/16 01:56:38  sueh
 * <p> bug# 828 Added toString() and paramString().
 * <p>
 * <p> Revision 3.13  2006/01/12 17:11:40  sueh
 * <p> bug# 798 Moved the autodoc classes to etomo.storage.autodoc.
 * <p>
 * <p> Revision 3.12  2006/01/11 22:11:56  sueh
 * <p> bug# 675 No longer need to create an instance without a name -
 * <p> removed getNamelessInstance().
 * <p>
 * <p> Revision 3.11  2006/01/04 20:26:02  sueh
 * <p> bug# 675 For printing the name:  putting the type first and making the type
 * <p> as constant.
 * <p>
 * <p> Revision 3.10  2006/01/03 23:40:26  sueh
 * <p> bug# 675 Added a getNamelessInstance() to create an instance without
 * <p> a name.  Added setName().  These functions can be used to create an
 * <p> instances with an "unpublished" name.
 * <p>
 * <p> Revision 3.9  2005/12/23 02:15:43  sueh
 * <p> bug# 675 Named the text field so it can be found by JfcUnit.
 * <p>
 * <p> Revision 3.8  2005/07/01 23:03:50  sueh
 * <p> bug# 619 added setText(long)
 * <p>
 * <p> Revision 3.7  2005/06/13 23:37:28  sueh
 * <p> bug# 675 Added a setName() call to the constructor to try out jfcUnit.
 * <p>
 * <p> Revision 3.6  2005/03/24 17:52:24  sueh
 * <p> Removed unused functions.
 * <p>
 * <p> Revision 3.5  2005/01/05 00:08:00  sueh
 * <p> bug# 567 Added setTextPreferredWidth().
 * <p>
 * <p> Revision 3.4  2004/11/19 23:57:15  sueh
 * <p> bug# 520 merging Etomo_3-4-6_JOIN branch to head.
 * <p>
 * <p> Revision 3.3.4.1  2004/11/19 00:21:58  sueh
 * <p> bug# 520 Added equals(String) to compare a String parameter against
 * <p> getText().
 * <p>
 * <p> Revision 3.3  2004/04/16 02:10:04  sueh
 * <p> bug# 409 added addKeyListener() to allow keystrokes to reacted to
 * <p>
 * <p> Revision 3.2  2004/04/07 21:04:02  rickg
 * <p> Alignment is now set on the panel
 * <p>
 * <p> Revision 3.1  2004/03/24 03:04:41  rickg
 * <p> Added setText(float) methof
 * <p> Fixed setMaximumSize bug
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:01  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.2  2003/06/03 23:27:05  rickg
 * <p> Comment updates
 * <p> Removed ambiguous methods
 * <p>
 * <p> Revision 2.1  2003/02/24 23:26:14  rickg
 * <p> Added a get label preferred size method
 * <p>
 * <p> Revision 2.0  2003/01/24 20:30:31  rickg
 * <p> Single window merge to main branch
 * <p>
 * <p> Revision 1.4.2.1  2003/01/24 18:43:37  rickg
 * <p> Single window GUI layout initial revision
 * <p>
 * <p> Revision 1.4  2002/12/31 23:13:24  rickg
 * <p> Added possible setalignmentx method
 * <p>
 * <p> Revision 1.3  2002/12/27 05:50:37  rickg
 * <p> Set the text field maximum height to twice the largest of the
 * <p> label and text font size in points.
 * <p>
 * <p> Revision 1.2  2002/12/10 21:34:38  rickg
 * <p> Added get and set size methods
 * <p>
 * <p> Revision 1.1  2002/09/09 22:57:02  rickg
 * <p> Initial CVS entry, basic functionality not including combining
 * <p> </p>
 */
public final class LabeledTextField
  implements UIComponent, SwingComponent, TextFieldInterface, FocusListener {
  private final JPanel panel = new JPanel();
  private final JLabel label = new JLabel();
  private final JTextField textField = new JTextField();
  private final FieldType fieldType;
  private final String locationDescr;
  private boolean debug = false;
  private Color origTextForeground = null;
  private Color origLabelForeground = null;
  private DirectiveDef directiveDef = null;
  private EtomoNumber maxDecimalPlaces = null;
  // Never reassign TextFieldSetting to null. If null means that they have never
  // been
  // used, less updating when checking the value of TextFieldSetting variables
  // is
  // required.
  private TextFieldSetting backup = null;
  private TextFieldSetting defaultValue = null;
  private TextFieldSetting fieldHighlight = null;
  private TextFieldSetting checkpoint = null;
  private boolean required = false;
  private ValidationSet validationSet = null;
  private FieldDisplayer fieldDisplayer = null;

  public boolean isDebug() {
    return debug || EtomoDirector.INSTANCE.getArguments().isDebug();
  }

  public String toString() {
    return "[label:" + getLabel() + "]";
  }

  String paramString() {
    return "label=" + label.getText() + ",textField=" + textField.getText();
  }

  public boolean equals(final Object object) {
    return object == textField;
  }

  public boolean equals(final Document document) {
    return textField.getDocument() == document;
  }

  private LabeledTextField(final FieldType fieldType, final String tfLabel,
    final int hgap, final String locationDescr) {
    this.locationDescr = locationDescr;
    this.fieldType = fieldType;
    // set label
    setLabel(tfLabel);

    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.add(label);
    if (hgap > 0) {
      panel.add(Box.createRigidArea(new Dimension(hgap, 0)));
    }
    panel.add(textField);

    // Set the maximum height of the text field box to twice the
    // font size since it is not set by default
    Dimension maxSize = textField.getMaximumSize();
    if (label.getFont().getSize() > textField.getFont().getSize()) {
      maxSize.setSize(maxSize.getWidth(), 2 * label.getFont().getSize());
    }
    else {
      maxSize.setSize(maxSize.getWidth(), 2 * textField.getFont().getSize());
    }
    textField.setMaximumSize(maxSize);
    if (fieldType == FieldType.FILE) {
      textField.setHorizontalAlignment(JTextField.RIGHT);
    }
  }

  public LabeledTextField(final FieldType fieldType, final String tfLabel) {
    this(fieldType, tfLabel, 0, null);
  }

  LabeledTextField(final FieldType fieldType, final String tfLabel,
    final String locationDescr) {
    this(fieldType, tfLabel, 0, locationDescr);
  }

  LabeledTextField(final FieldType fieldType, final String tfLabel, final int hgap) {
    this(fieldType, tfLabel, hgap, null);
  }

  static LabeledTextField getNumericInstance(final String tfLabel,
    final EtomoNumber.Type numericType) {
    FieldType fieldType = FieldType.INTEGER;
    if (numericType == EtomoNumber.Type.DOUBLE) {
      fieldType = FieldType.FLOATING_POINT;
    }
    return new LabeledTextField(fieldType, tfLabel, 0, null);
  }

  static LabeledTextField getNumericInstance(final String tfLabel) {
    return getNumericInstance(tfLabel, EtomoNumber.Type.INTEGER);
  }

  public void setMaxDecimalPlaces(final int digits) {
    if (maxDecimalPlaces == null) {
      maxDecimalPlaces = new EtomoNumber();
    }
    maxDecimalPlaces.set(digits);
  }

  public String getName() {
    return textField.getName();
  }

  public boolean isBoolean() {
    return false;
  }

  public boolean isText() {
    return true;
  }

  /**
   * Not true value is implemented so all non-empty values are true.
   */
  public boolean equalsSelectedStringValue(final String value) {
    return value != null && !value.isEmpty();
  }

  void setName(final String tfLabel) {
    String name = Utilities.convertLabelToName(tfLabel);
    textField.setName(
      UITestFieldType.TEXT_FIELD.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
      System.out
        .println(textField.getName() + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
    }
  }

  /**
   * Saves the current text as the checkpoint.
   */
  public void checkpoint() {
    if (checkpoint == null) {
      checkpoint = new TextFieldSetting(fieldType);
    }
    checkpoint.set(getText());
  }

  /**
   * Saves the current text in backup.
   */
  public void backup() {
    if (backup == null) {
      backup = new TextFieldSetting(fieldType);
    }
    backup.set(getText());
  }

  /**
   * If the field was backed up, make the backup value the displayed value, and turn off
   * the back up.
   */
  public void restoreFromBackup() {
    if (backup != null && backup.isSet()) {
      setText(backup.getValue());
      backup.reset();
    }
  }

  public void setDirectiveDef(final DirectiveDef directiveDef) {
    this.directiveDef = directiveDef;
  }

  public DirectiveDef getDirectiveDef() {
    return directiveDef;
  }

  public void useDefaultValue() {
    if (directiveDef == null) {
      if (defaultValue != null && defaultValue.isSet()) {
        defaultValue.reset();
      }
      return;
    }
    // only search for default value once
    if (defaultValue == null) {
      defaultValue = new TextFieldSetting(fieldType);
      String value = AutodocAttributeRetriever.INSTANCE.getDefaultValue(directiveDef);
      if (value != null) {
        // if default value has been found, set it in the field setting
        defaultValue.set(value);
      }
    }
    if (defaultValue.isSet()) {
      setText(defaultValue.getValue());
    }
  }

  public boolean equalsDefaultValue() {
    return defaultValue != null && defaultValue.isSet() && defaultValue.equals(getText());
  }

  public boolean equalsDefaultValue(final String value) {
    return defaultValue != null && defaultValue.isSet() && defaultValue.equals(value);
  }

  /**
   * Saves value as the checkpoint.
   */
  void checkpoint(final int value) {
    if (checkpoint == null) {
      checkpoint = new TextFieldSetting(fieldType);
    }
    checkpoint.set(value);
  }

  /**
   * Saves value as the checkpoint.
   */
  void checkpoint(final ConstEtomoNumber value) {
    if (checkpoint == null) {
      checkpoint = new TextFieldSetting(fieldType);
    }
    checkpoint.set(value);
  }

  /**
   * Saves value as the checkpoint.
   */
  void checkpoint(final double value) {
    if (checkpoint == null) {
      checkpoint = new TextFieldSetting(fieldType);
    }
    checkpoint.set(value);
  }

  /**
   * Saves value as the checkpoint.
   */
  void checkpoint(final String value) {
    if (checkpoint == null) {
      checkpoint = new TextFieldSetting(fieldType);
    }
    checkpoint.set(value);
  }

  public void setCheckpoint(final FieldSettingInterface input) {
    if (checkpoint == null && input != null && input.isSet() && input.isText()) {
      checkpoint = new TextFieldSetting(fieldType);
    }
    if (checkpoint != null) {
      checkpoint.copy(input);
    }
  }

  public FieldSettingInterface getCheckpoint() {
    return checkpoint;
  }

  /**
   * Resets to checkpointValue if checkpointValue has been set.  Otherwise has no effect.
   */
  void resetToCheckpoint() {
    if (checkpoint == null || !checkpoint.isSet()) {
      return;
    }
    setText(checkpoint.getValue());
  }

  public boolean isFieldHighlightSet() {
    return fieldHighlight != null && fieldHighlight.isSet();
  }

  public void setFieldHighlight(final String value) {
    if (fieldHighlight == null) {
      fieldHighlight = new TextFieldSetting(fieldType);
      textField.addFocusListener(this);
    }
    fieldHighlight.set(value);
    updateFieldHighlight();
  }

  public void setFieldHighlight(final boolean value) {}

  public void setFieldHighlight(final FieldSettingInterface input) {
    if (fieldHighlight == null && input != null && input.isSet() && input.isText()) {
      fieldHighlight = new TextFieldSetting(fieldType);
      textField.addFocusListener(this);
    }
    if (fieldHighlight != null) {
      fieldHighlight.copy(input);
      updateFieldHighlight();
    }
  }

  public void clearFieldHighlight() {
    if (fieldHighlight != null && fieldHighlight.isSet()) {
      fieldHighlight.reset();
      updateFieldHighlight();
    }
  }

  public FieldSettingInterface getFieldHighlight() {
    return fieldHighlight;
  }

  public boolean equalsFieldHighlight() {
    return fieldHighlight != null && fieldHighlight.isSet()
      && fieldHighlight.equals(getText());
  }

  public boolean equalsFieldHighlight(final String value) {
    return fieldHighlight != null && fieldHighlight.isSet()
      && fieldHighlight.equals(value);
  }

  public void focusGained(final FocusEvent event) {}

  public void focusLost(final FocusEvent event) {
    updateFieldHighlight();
  }

  /**
   * If the field highlight is in use, use the field highlight color on the foreground of
   * the text field if the value of the text field equals the field highlight value.  Save
   * the original foreground.  If the field highlight is in use and the value of the text
   * field does not equal the field highlight value, try to restore the original
   * foreground - or set a foreground color similar to the original one.  Assumes that
   * field highlight is not used when the field is disabled.
   */
  void updateFieldHighlight() {
    // To avoid constantly updating the foreground color, assuming that
    // fieldHighlight is
    // never reassigned to null
    if (fieldHighlight == null || !textField.isEnabled()) {
      return;
    }
    if (fieldHighlight.isSet() && fieldHighlight.equals(textField.getText())) {
      // save the original color
      if (origTextForeground == null) {
        origTextForeground = textField.getForeground();
        if (origTextForeground == null) {
          origTextForeground = Color.BLACK;
        }
      }
      if (origLabelForeground == null) {
        origLabelForeground = label.getForeground();
        if (origLabelForeground == null) {
          origLabelForeground = Color.BLACK;
        }
      }
      label.setForeground(Colors.FIELD_HIGHLIGHT);
      textField.setForeground(Colors.FIELD_HIGHLIGHT);
    }
    else {
      if (origTextForeground != null) {
        textField.setForeground(origTextForeground);
      }
      if (origLabelForeground != null) {
        label.setForeground(origLabelForeground);
      }
    }
  }

  void addActionListener(ActionListener listener) {
    textField.addActionListener(listener);
  }

  /**
   * If the field is disabled then return false because its value doesn't matter.
   * First it returns true if the checkpoint has not been done; the checkpoint value is
   * from an outside value, so the current value must be different from a non-existant
   * checkpoint value.  After eliminating this possibility, it returns a boolean based on
   * the difference between the text field value and the checkpointed value.
   *
   * @return
   */
  boolean isDifferentFromCheckpoint() {
    return isDifferentFromCheckpoint(false);
  }

  /**
   * @param alwaysCheck - when false return false when the field is disabled or invisible
   * @return true if text field is different from checkpoint
   */
  public boolean isDifferentFromCheckpoint(final boolean alwaysCheck) {
    if (!alwaysCheck && (!textField.isEnabled() || !textField.isVisible())) {
      return false;
    }
    return checkpoint == null || !checkpoint.equals(getText());
  }

  public void clear() {
    textField.setText("");
  }

  public void setValue(final Field input) {
    if (input == null) {
      clear();
    }
    else {
      setText(input.getText());
    }
  }

  public void setValue(final String value) {
    setText(value);
  }

  public void setValue(final boolean value) {}

  public boolean isSelected() {
    return false;
  }

  public boolean equals(final String thatText) {
    String text = getText();
    if (text == null) {
      if (thatText == null) {
        return true;
      }
      return false;
    }
    if (thatText == null) {
      return false;
    }
    if (text.trim().equals(thatText.trim())) {
      return true;
    }
    return false;
  }

  void setHighlight(final boolean highlight) {
    if (highlight) {
      textField.setBackground(Colors.HIGHLIGHT_BACKGROUND);
    }
    else {
      textField.setBackground(Colors.BACKGROUND);
    }
  }

  public SwingComponent getUIComponent() {
    return this;
  }

  public Component getComponent() {
    return panel;
  }

  public Container getContainer() {
    return panel;
  }

  public String getLabel() {
    return label.getText();
  }

  public String getQuotedLabel() {
    return Utilities.quoteLabel(label.getText());
  }

  void setLabel(final String label) {
    this.label.setText(label);
    setName(label);
  }

  void setRequired(final boolean required) {
    this.required = required;
  }

  void setValidationSet(final ValidationSet input) {
    if (validationSet == null) {
      validationSet = input;
    }
    else if (input != null) {
      validationSet.copy(input);
    }
    else {
      validationSet.clear();
    }
  }

  void setNumberMustBePositive(final boolean input) {
    if (validationSet == null) {
      validationSet = new ValidationSet(fieldType);
    }
    validationSet.setNumberMustBePositive(input);
  }

  public String getText(final boolean doValidation)
    throws FieldValidationFailedException {
    return getText(doValidation, null);
  }

  public boolean isRequired() {
    return required && textField.isEnabled();
  }

  void setFieldDisplayer(final FieldDisplayer fieldDisplayer) {
    this.fieldDisplayer = fieldDisplayer;
  }

  public String getText(final boolean doValidation, final FieldDisplayer fieldDisplayer)
    throws FieldValidationFailedException {
    String text = textField.getText();
    if (debug) {
      System.out.println(
        "C:doValidation:" + doValidation + ",text:" + text + ",required:" + required);
    }
    if (doValidation && textField.isEnabled()) {
      text = FieldValidator.validateText(text, fieldType, this, getDescription(),
        required, false, false, validationSet, fieldDisplayer, this.fieldDisplayer);
    }
    return text;
  }

  public String getDescription() {
    return getQuotedLabel() + (locationDescr == null ? "" : " in " + locationDescr);
  }

  /**
   * return text without validation
   */
  public String getText() {
    return textField.getText();
  }

  public boolean isEmpty() {
    String text = textField.getText();
    return text == null || text.matches("\\s*");
  }

  void setText(final File file) {
    textField.setText(file.getAbsolutePath());
  }

  public void setText(ConstEtomoNumber text) {
    if (text == null) {
      textField.setText("");
    }
    else {
      textField.setText(Utilities.roundToMaxDecimalPlaces(text, maxDecimalPlaces));
    }
  }

  void setText(final Number input) {
    if (input == null) {
      textField.setText("");
    }
    else {
      textField.setText(Utilities.roundToMaxDecimalPlaces(input, maxDecimalPlaces));
    }
  }

  public void setText(final String text) {
    textField.setText(Utilities.roundToMaxDecimalPlaces(text, maxDecimalPlaces));
  }

  /**
   * Set text if parameter is not empty.  Prevents a value from being overridden by
   * nothing.
   * @param text
   */
  public void setNonEmptyText(final String text) {
    if (text != null && !text.isEmpty()) {
      setText(text);
    }
  }

  void setText(final String text, final boolean allowEmpty) {
    if (allowEmpty || (text != null && !text.isEmpty())) {
      setText(text);
    }
  }

  public void setText(final int value) {
    // textField.setText(String.valueOf(value));
    textField.setText(Utilities.roundToMaxDecimalPlaces(value, maxDecimalPlaces));
  }

  void setText(final long value) {
    // textField.setText(String.valueOf(value));
    textField.setText(Utilities.roundToMaxDecimalPlaces(value, maxDecimalPlaces));
  }

  void setText(final double value) {
    // textField.setText(Double.toString(value));
    textField.setText(Utilities.roundToMaxDecimalPlaces(value, maxDecimalPlaces));
  }

  public void setTextFieldEnabled(final boolean enabled) {
    textField.setEnabled(enabled);
    if (enabled) {
      updateFieldHighlight();
    }
  }

  public void setEnabled(final boolean enabled) {
    textField.setEnabled(enabled);
    label.setEnabled(enabled);
    if (enabled) {
      updateFieldHighlight();
    }
  }

  public void setEditable(final boolean editable) {
    // Label is not changed by editable status
    textField.setEditable(editable);
  }

  public boolean isEnabled() {
    return textField.isEnabled();
  }

  boolean isEditable() {
    return textField.isEditable();
  }

  boolean isVisible() {
    return panel.isVisible();
  }

  public void setVisible(final boolean isVisible) {
    panel.setVisible(isVisible);
  }

  void setDebug(final boolean debug) {
    this.debug = debug;
  }

  public void addDocumentListener(final DocumentListener listener) {
    textField.getDocument().addDocumentListener(listener);
  }

  void setTextPreferredSize(final Dimension size) {
    textField.setPreferredSize(size);
    textField.setMaximumSize(size);
  }

  void setPreferredWidth(final int width) {
    Dimension dim = textField.getPreferredSize();
    dim.width =
      width * (int) Math.round(UIParameters.getInstance().getFontSizeAdjustment());
    textField.setPreferredSize(dim);
    textField.setMaximumSize(dim);
  }

  void setTextPreferredWidth(final double minWidth) {
    Dimension prefSize = textField.getPreferredSize();
    prefSize.setSize(minWidth, prefSize.getHeight());
    textField.setPreferredSize(prefSize);
  }

  void setMinimumWidth(final double minWidth) {
    Dimension prefSize = textField.getPreferredSize();
    prefSize.setSize(minWidth, prefSize.getHeight());
    textField.setMinimumSize(prefSize);
  }

  /**
   * Set the absolute preferred size of the panel
   * @param size
  
  public void setPreferredSize(Dimension size) {
  panel.setPreferredSize(size);
  }*/

  /**
   * Set the absolute maximum size of the panel
   *
   * @param size public void setMaximumSize(Dimension size) {
   *             panel.setMaximumSize(size);
   *             }
   */

  Dimension getLabelPreferredSize() {
    return label.getPreferredSize();
  }

  void setColumns(final int columns) {
    textField.setColumns(columns);
  }

  void setAlignmentX(final float alignment) {
    panel.setAlignmentX(alignment);
  }

  public void setToolTipText(final String text) {
    boolean setDebug = debug && !TooltipFormatter.INSTANCE.isDebug();
    if (setDebug) {
      TooltipFormatter.INSTANCE.setDebug(debug);
    }
    String tooltip = TooltipFormatter.INSTANCE.format(text);
    if (setDebug) {
      TooltipFormatter.INSTANCE.setDebug(false);
    }
    panel.setToolTipText(tooltip);
    textField.setToolTipText(tooltip);
    label.setToolTipText(tooltip);
  }

  public void setTooltip(final Field field) {
    if (field != null) {
      String tooltip = field.getTooltip();
      panel.setToolTipText(tooltip);
      textField.setToolTipText(tooltip);
      label.setToolTipText(tooltip);
    }
  }

  public String getTooltip() {
    return textField.getToolTipText();
  }

  void addMouseListener(final MouseListener listener) {
    panel.addMouseListener(listener);
    label.addMouseListener(listener);
    textField.addMouseListener(listener);
  }
}
