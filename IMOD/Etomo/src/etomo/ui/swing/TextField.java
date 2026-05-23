package etomo.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.text.Document;

import etomo.EtomoDirector;
import etomo.logic.AutodocAttributeRetriever;
import etomo.logic.FieldValidator;
import etomo.storage.DirectiveDef;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.UITestFieldType;
import etomo.ui.FieldDisplayer;
import etomo.ui.TextFieldSetting;
import etomo.ui.Field;
import etomo.ui.FieldSettingInterface;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.UIComponent;
import etomo.util.Utilities;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2006 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class TextField implements UIComponent, SwingComponent, Field, FocusListener {
  private final JTextField textField = new JTextField();

  private final FieldType fieldType;
  private final String reference;
  private final String locationDescr;

  private boolean required = false;
  private Color origForeground = null;
  private DirectiveDef directiveDef = null;
  // Never reassign TextFieldSetting to null. If null means that they have never
  // been
  // used, less updating is required.
  private TextFieldSetting backup = null;
  private TextFieldSetting defaultValue = null;
  private TextFieldSetting checkpoint = null;
  private TextFieldSetting fieldHighlight = null;
  private FontMetrics fontMetrics = null;
  private Dimension fixedSize = null;
  private boolean debug = false;
  private String tooltip = null;
  private String alternateTooltip = null;

  TextField(final FieldType fieldType, final String reference,
    final String locationDescr) {
    this.locationDescr = locationDescr;
    this.fieldType = fieldType;
    this.reference = reference;
    setName(reference);
    // Set the maximum height of the text field box to twice the
    // font size since it is not set by default
    JLabel label = new JLabel(reference);
    Dimension maxSize = textField.getMaximumSize();
    if (label.getFont().getSize() > textField.getFont().getSize()) {
      maxSize.setSize(maxSize.getWidth(), 2 * label.getFont().getSize());
    }
    else {
      maxSize.setSize(maxSize.getWidth(), 2 * textField.getFont().getSize());
    }
    textField.setMaximumSize(maxSize);
  }

  void setColumns(final int columns) {
    textField.setColumns(columns);
  }

  public void setDebug(final boolean input) {
    debug = input;
  }

  int getPreferredWidth() {
    if (fixedSize != null) {
      return fixedSize.width;
    }
    int width = 0;
    if (fontMetrics == null) {
      fontMetrics = UIUtilities.getFontMetrics(textField);
    }
    return UIUtilities.getPreferredWidth(textField.getText(), fontMetrics);
  }

  public boolean isBoolean() {
    return false;
  }

  public boolean isDebug() {
    return debug || EtomoDirector.INSTANCE.getArguments().isDebug();
  }

  public boolean isText() {
    return true;
  }

  /**
   * No selectedStringValue is implemented so all non-empty values are true.
   */
  public boolean equalsSelectedStringValue(final String value) {
    return value != null && !value.isEmpty();
  }

  public void setToolTipText(String text) {
    setPreformattedTooltip(TooltipFormatter.INSTANCE.format(text));
  }

  public void setTooltip(final Field field) {
    if (field != null) {
      setPreformattedTooltip(field.getTooltip());
    }
  }

  /**
   * Sets a preformatted tooltip.  Handles alternate tooltips.
   * @param tooltip
   */
  void setPreformattedTooltip(final String tooltip) {
    textField.setToolTipText(tooltip);
    // If the alternate tooltip is in use, store this tooltip as the main one.
    if (alternateTooltip != null) {
      this.tooltip = tooltip;
    }
  }

  /**
   * Stores a second tooltip.  Does not switch to the second tooltip.
   * @see switchTooltips
   */
  public void setAlternateTooltipText(final String text) {
    // Save the current tooltip as the main tooltip.
    if (tooltip == null) {
      String curTooltip = textField.getToolTipText();
      if (curTooltip != null && !curTooltip.equals("")) {
        tooltip = curTooltip;
      }
    }
    alternateTooltip = TooltipFormatter.INSTANCE.format(text);
  }

  /**
   * Switch to/from the alternate tooltip.
   * @param alternate
   */
  public void switchTooltips(final boolean alternate) {
    if (alternate) {
      textField.setToolTipText(alternateTooltip);
    }
    else {
      textField.setToolTipText(tooltip);
    }
  }

  /**
   * Gets the tooltip that is currently in use.
   */
  public String getTooltip() {
    return textField.getToolTipText();
  }

  public SwingComponent getUIComponent() {
    return this;
  }

  public Component getComponent() {
    return textField;
  }

  void setAlignmentX(float alignmentX) {
    textField.setAlignmentX(alignmentX);
  }

  void setEnabled(boolean enabled) {
    textField.setEnabled(enabled);
    if (enabled) {
      updateFieldHighlight();
    }
  }

  void setEditable(boolean editable) {
    textField.setEditable(editable);
  }

  public boolean isEnabled() {
    return textField.isEnabled();
  }

  boolean isEditable() {
    return textField.isEditable();
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

  public void backup() {
    if (debug) {
      System.out
        .println("C:checkpoint:" + checkpoint + ".textField:" + textField.getText());
    }
    if (backup == null) {
      backup = new TextFieldSetting(fieldType);
    }
    backup.set(textField.getText());
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

  public void clear() {
    setText("");
  }

  public boolean isSelected() {
    return false;
  }

  public void setDirectiveDef(final DirectiveDef directiveDef) {
    this.directiveDef = directiveDef;
  }

  public DirectiveDef getDirectiveDef() {
    return directiveDef;
  }

  Document getDocument() {
    return textField.getDocument();
  }

  void setColumns() {
    textField.setColumns(fieldType.getColumns());
  }

  void setPreferredSize(final Dimension size) {
    textField.setPreferredSize(size);
  }

  void setVisible(boolean visible) {
    textField.setVisible(visible);
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
   * Saves the current text as the checkpoint.
   */
  public void checkpoint() {
    if (checkpoint == null) {
      checkpoint = new TextFieldSetting(fieldType);
    }
    checkpoint.set(getText());
  }

  void checkpoint(final String value) {
    if (checkpoint == null) {
      checkpoint = new TextFieldSetting(fieldType);
    }
    checkpoint.set(value);
  }

  void resetToCheckpoint() {
    if (checkpoint == null || !checkpoint.isSet()) {
      return;
    }
    setText(checkpoint.getValue());
  }

  public TextFieldSetting getCheckpoint() {
    return checkpoint;
  }

  public void setCheckpoint(final FieldSettingInterface input) {
    if (checkpoint == null && input != null && input.isSet() && input.isText()) {
      checkpoint = new TextFieldSetting(fieldType);
    }
    if (checkpoint != null) {
      checkpoint.copy(input);
    }
  }

  public boolean isFieldHighlightSet() {
    return fieldHighlight != null && fieldHighlight.isSet();
  }

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

  public void setFieldHighlight(final String value) {
    if (fieldHighlight == null) {
      fieldHighlight = new TextFieldSetting(fieldType);
      textField.addFocusListener(this);
    }
    fieldHighlight.set(value);
    updateFieldHighlight();
  }

  public void setFieldHighlight(final boolean value) {}

  public TextFieldSetting getFieldHighlight() {
    return fieldHighlight;
  }

  public void clearFieldHighlight() {
    if (fieldHighlight != null && fieldHighlight.isSet()) {
      fieldHighlight.reset();
      updateFieldHighlight();
    }
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
  
  void addFocusListener(final FocusListener listener) {
    textField.addFocusListener(listener);
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
      if (origForeground == null) {
        origForeground = textField.getForeground();
        if (origForeground == null) {
          origForeground = Color.BLACK;
        }
      }
      textField.setForeground(Colors.FIELD_HIGHLIGHT);
    }
    else if (origForeground != null) {
      // field highlight has been turned off or field highlight doesn't match
      textField.setForeground(origForeground);
    }
  }

  void setText(final File file) {
    textField.setText(file.getAbsolutePath());
    updateFieldHighlight();
  }

  public void setText(String text) {
    textField.setText(text);
    updateFieldHighlight();
  }

  /**
   * Sets a validation setting.
   * @param required
   */
  void setRequired(final boolean required) {
    this.required = required;
  }

  String getText(final boolean doValidation) throws FieldValidationFailedException {
    return getText(doValidation, null);
  }

  public boolean isRequired() {
    return required && textField.isEnabled();
  }

  /**
   * Validates and returns text in text field.  Should never throw a
   * FieldValidationFailedException when doValidation is true.
   *
   * @param doValidation
   * @return
   * @throws FieldValidationFailedException
   */
  public String getText(final boolean doValidation, final FieldDisplayer fieldDisplayer)
    throws FieldValidationFailedException {
    String text = textField.getText();
    if (doValidation && textField.isEnabled()) {
      text = FieldValidator.validateText(text, fieldType, this,
        getQuotedReference() + (locationDescr == null ? "" : " in " + locationDescr),
        required, false, false, null, fieldDisplayer);
    }
    return text;
  }

  /**
   * Calls two field validation for arrays that need to have a one to one correspondence.
   *
   * @param otherArrayField
   * @param fieldDisplayer
   * @throws FieldValidationFailedException
   */
  public void validatePairedArrays(final Field field2)
    throws FieldValidationFailedException {
    if (!textField.isEnabled() || field2 == null || !field2.isEnabled()) {
      return;
    }
    FieldValidator.validatePairedArrays(textField.getText(), this, getDescription(),
      field2.getText(), field2.getDescription());
  }

  public String getDescription() {
    return getQuotedReference() + (locationDescr == null ? "" : " in " + locationDescr);
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

  /**
   * get text without validation
   *
   * @return
   */
  public String getText() {
    return textField.getText();
  }

  public boolean isEmpty() {
    String text = getText();
    return text == null || text.matches("\\s*");
  }

  private String getQuotedReference() {
    return Utilities.quoteLabel(reference);
  }

  Font getFont() {
    return textField.getFont();
  }

  Dimension getMaximumSize() {
    return textField.getMaximumSize();
  }

  void setMaximumSize(Dimension size) {
    textField.setMaximumSize(size);
  }

  void setTextPreferredWidth(final double minWidth) {
    Dimension prefSize = textField.getPreferredSize();
    prefSize.setSize(minWidth, prefSize.getHeight());
    textField.setPreferredSize(prefSize);
  }

  void setTextPreferredSize(Dimension size) {
    fixedSize = size;
    textField.setPreferredSize(size);
    textField.setMaximumSize(size);
  }

  void setSize(Dimension size) {
    textField.setSize(size);
  }
  
  void setPreferredWidth(final int width) {
    Dimension dim = textField.getPreferredSize();
    dim.width =
      width * (int) Math.round(UIParameters.getInstance().getFontSizeAdjustment());
    textField.setPreferredSize(dim);
    textField.setMaximumSize(dim);
  }

  Dimension getSize() {
    return textField.getSize();
  }

  Dimension getPreferredSize() {
    return textField.getPreferredSize();
  }

  public String getName() {
    return textField.getName();
  }

  boolean isVisible() {
    return textField.isVisible();
  }

  public String getQuotedLabel() {
    return getQuotedReference();
  }

  void setName(String reference) {
    String name = Utilities.convertLabelToName(reference);
    textField.setName(
      UITestFieldType.TEXT_FIELD.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
      System.out.println(getName() + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
    }
  }
}
/**
 * <p> $Log$
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.11  2010/03/03 05:08:06  sueh
 * <p> bug# 1311 Changed TextField.setSize to setPreferredSize.  Added getFont, getMaximumSize, setMaximumSize, and setSize.
 * <p>
 * <p> Revision 1.10  2009/11/20 17:37:52  sueh
 * <p> bug# 1282 Added prefixes to all of the field names, so that the fields that
 * <p> are actually abstract buttons (radio buttons, etc) won't be activated by a
 * <p> "bn." field command.
 * <p>
 * <p> Revision 1.9  2009/01/20 20:31:23  sueh
 * <p> bug# 1102 Changed UITestField to UITestFieldType.
 * <p>
 * <p> Revision 1.8  2008/05/30 22:36:39  sueh
 * <p> bug# 1102 Isolating the etomo.uitest package so it is not need for
 * <p> running EtomoDirector.
 * <p>
 * <p> Revision 1.7  2008/05/30 21:34:57  sueh
 * <p> bug# 1102 Moved uitest classes to etomo.uitest.
 * <p>
 * <p> Revision 1.6  2008/02/19 00:47:46  sueh
 * <p> bug# 1078 Added setTextPreferredWidth.
 * <p>
 * <p> Revision 1.5  2007/12/26 22:35:07  sueh
 * <p> bug# 1052 Moved argument handling from EtomoDirector to a separate class.
 * <p>
 * <p> Revision 1.4  2007/09/07 00:29:17  sueh
 * <p> bug# 989 Using a public INSTANCE to refer to the EtomoDirector singleton
 * <p> instead of getInstance and createInstance.
 * <p>
 * <p> Revision 1.3  2007/03/30 23:54:19  sueh
 * <p> bug# 964 Wrapping JTextField instead of inheriting it.  Added automatic sizing.
 * <p>
 * <p> Revision 1.2  2007/02/09 00:53:33  sueh
 * <p> bug# 962 Made TooltipFormatter a singleton and moved its use to low-level ui
 * <p> classes.
 * <p>
 * <p> Revision 1.1  2006/09/13 23:58:14  sueh
 * <p> bug# 924 Added TextField:  extends JTextField and automatically names itself.
 * <p> </p>
 */
