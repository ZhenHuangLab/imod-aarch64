package etomo.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;
import javax.swing.event.ChangeListener;

import etomo.EtomoDirector;
import etomo.logic.AutodocAttributeRetriever;
import etomo.storage.DirectiveDef;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.storage.autodoc.ReadOnlySection;
import etomo.type.ConstEtomoNumber;
import etomo.type.EnumeratedType;
import etomo.type.EtomoAutodoc;
import etomo.type.UITestFieldType;
import etomo.ui.BooleanFieldInterface;
import etomo.ui.BooleanFieldSetting;
import etomo.ui.Field;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldSettingInterface;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.UIComponent;
import etomo.util.Utilities;

/**
 * <p>Description: </p>
 * <p>Copyright: Copyright 2005 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class RadioButton implements RadioButtonInterface, BooleanFieldInterface,
  /*ActionListener,*/ItemListener, UIComponent, SwingComponent {
  private final JRadioButton radioButton;
  private final EnumeratedType enumeratedType;
  private final ButtonGroup group;

  private boolean debug = false;
  private Color origForeground = null;
  private DirectiveDef directiveDef = null;
  private BooleanFieldSetting backup = null;
  private BooleanFieldSetting checkpoint = null;
  private BooleanFieldSetting defaultValue = null;
  private BooleanFieldSetting fieldHighlight = null;
  private boolean enabled = true;
  private boolean editable = true;
  // Directive value associated with this instance being selected.
  private String selectedStringValue = null;

  RadioButton(final String text) {
    this(text, null, null, null);
  }

  public RadioButton(final String text, final ButtonGroup group) {
    this(text, null, group, null);
  }

  RadioButton(final String text, final ButtonGroup group, final RadioButtonModel model) {
    this(text, null, group, model);
  }

  RadioButton(final ButtonGroup group) {
    this("", null, group, null);
  }

  RadioButton(final String text, final EnumeratedType enumeratedType) {
    this(text, enumeratedType, null, null);
  }

  public RadioButton(final String text, final EnumeratedType enumeratedType,
    final ButtonGroup group) {
    this(text, enumeratedType, group, null);
  }

  RadioButton(String text, final EnumeratedType enumeratedType, final ButtonGroup group,
    RadioButtonModel model) {
    this.group = group;
    if (text == null && enumeratedType != null) {
      text = enumeratedType.getLabel();
    }
    radioButton = new JRadioButton(text);
    radioButton.setFocusable(false);
    if (model == null) {
      model = new RadioButtonModel(this);
    }
    radioButton.setModel(model);
    setName(text);
    this.enumeratedType = enumeratedType;
    if (group != null) {
      group.add(radioButton);
    }
    if (enumeratedType != null) {
      if (enumeratedType.isDefault()) {
        radioButton.setSelected(true);
      }
      ConstEtomoNumber number = enumeratedType.getValue();
      if (number != null) {
        selectedStringValue = number.toString();
      }
    }
  }

  public RadioButton(final EnumeratedType enumeratedType, final ButtonGroup group) {
    this.group = group;
    String text = enumeratedType.getLabel();
    radioButton = new JRadioButton(text);
    radioButton.setModel(new RadioButtonModel(this));
    setName(text);
    this.enumeratedType = enumeratedType;
    if (group != null) {
      group.add(radioButton);
    }
    if (enumeratedType != null) {
      if (enumeratedType.isDefault()) {
        radioButton.setSelected(true);
      }
      ConstEtomoNumber number = enumeratedType.getValue();
      if (number != null) {
        selectedStringValue = number.toString();
      }
    }
  }

  RadioButton(final EnumeratedType enumeratedType, final ButtonGroup group,
    final String addToLabel) {
    this.group = group;
    String text = enumeratedType.getLabel() + (addToLabel != null ? addToLabel : "");
    radioButton = new JRadioButton(text);
    radioButton.setModel(new RadioButtonModel(this));
    setName(text);
    this.enumeratedType = enumeratedType;
    if (group != null) {
      group.add(radioButton);
    }
    if (enumeratedType != null) {
      if (enumeratedType.isDefault()) {
        radioButton.setSelected(true);
      }
      ConstEtomoNumber number = enumeratedType.getValue();
      if (number != null) {
        selectedStringValue = number.toString();
      }
    }
  }

  public Field getField() {
    return this;
  }

  public boolean isBoolean() {
    return true;
  }

  public boolean isDebug() {
    return debug || EtomoDirector.INSTANCE.getArguments().isDebug();
  }

  public boolean isText() {
    return false;
  }

  boolean isVisible() {
    return radioButton.isVisible();
  }

  public boolean equalsSelectedStringValue(final String value) {
    if (selectedStringValue == null) {
      return value != null && !value.isEmpty();
    }
    return selectedStringValue.equals(value);
  }

  public String toString() {
    return radioButton.getText() + ": " + (radioButton.isSelected() ? "On" : "Off");
  }

  public FieldSettingInterface getCheckpoint() {
    return checkpoint;
  }

  void doClick() {
    radioButton.doClick();
  }

  public void checkpoint() {
    if (checkpoint == null) {
      checkpoint = new BooleanFieldSetting();
    }
    checkpoint.set(isSelected());
  }

  void checkpoint(final boolean value) {
    if (checkpoint == null) {
      checkpoint = new BooleanFieldSetting();
    }
    checkpoint.set(value);
  }

  public void setCheckpoint(FieldSettingInterface input) {
    if (checkpoint == null && input != null && input.isSet() && input.isBoolean()) {
      checkpoint = new BooleanFieldSetting();
    }
    if (checkpoint != null) {
      checkpoint.copy(input);
    }
  }

  public void backup() {
    if (backup == null) {
      backup = new BooleanFieldSetting();
    }
    backup.set(isSelected());
  }

  /**
   * If the field was backed up, make the backup value the displayed value if possible,
   * and turn off the back up.  Its impossible to turn off a radio button, so this only
   * works if the backupValue is true.  This relies on the other radio buttons in the
   * group also being backed up.
   */
  public void restoreFromBackup() {
    if (backup != null && backup.isSet()) {
      setSelected(backup.isValue());
      backup.reset();
    }
  }

  public void setValue(final Field input) {
    if (input == null) {
      clear();
    }
    else {
      setSelected(input.isSelected());
    }
  }

  public void setValue(final String value) {}

  public void setValue(final boolean value) {
    setSelected(value);
  }

  /**
   * No way to clear a radio button
   */
  public void clear() {}

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
      defaultValue = new BooleanFieldSetting();
      String value = AutodocAttributeRetriever.INSTANCE.getDefaultValue(directiveDef);
      if (value != null) {
        // if default value has been found, set it in the field setting
        defaultValue.set(value);
      }
    }
    if (defaultValue.isSet()) {
      setSelected(defaultValue.isValue());
    }
  }

  public boolean equalsDefaultValue() {
    return defaultValue != null && defaultValue.isSet()
      && defaultValue.equals(isSelected());
  }

  public boolean equalsDefaultValue(final String value) {
    return defaultValue != null && defaultValue.isSet()
      && defaultValue.equals(value != null && !value.matches("\\s*"));
  }

  public boolean equalsDefaultValue(final boolean input) {
    return defaultValue != null && defaultValue.isSet() && defaultValue.equals(input);
  }

  boolean isCheckpointValue() {
    if (checkpoint == null) {
      return false;
    }
    return checkpoint.isValue();
  }

  /**
   * @param alwaysCheck - check for difference even when the field is disables or
   *                    invisible
   * @return
   */
  public boolean isDifferentFromCheckpoint(final boolean alwaysCheck) {
    if (!alwaysCheck && (!isEnabled() || !radioButton.isVisible())) {
      return false;
    }
    return checkpoint == null || !checkpoint.equals(isSelected());
  }

  void setText(final String text) {
    radioButton.setText(text);
    setName(text);
  }

  public String getDescription() {
    return getQuotedLabel();
  }

  public String getQuotedLabel() {
    return Utilities.quoteLabel(getText());
  }

  void setBorderPainted(boolean b) {
    radioButton.setBorderPainted(b);
  }

  void setBorder(Border border) {
    radioButton.setBorder(border);
  }

  Font getFont() {
    return radioButton.getFont();
  }

  void setForeground(Color fg) {
    radioButton.setForeground(fg);
  }

  int getWidth() {
    return radioButton.getWidth();
  }

  int getHeight() {
    return radioButton.getHeight();
  }

  Border getBorder() {
    return radioButton.getBorder();
  }

  void setName(final String text) {
    String name = Utilities.convertLabelToName(text);
    radioButton.setName(
      UITestFieldType.RADIO_BUTTON.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
      System.out
        .println(radioButton.getName() + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
    }
  }

  public boolean equals(final EnumeratedType enumeratedType) {
    return this.enumeratedType == enumeratedType;
  }

  void setDebug(final boolean input) {
    debug = input;
  }

  public boolean isFieldHighlightSet() {
    return fieldHighlight != null && fieldHighlight.isSet();
  }

  public void setFieldHighlight(FieldSettingInterface input) {
    if (fieldHighlight == null && input != null && input.isSet() && input.isBoolean()) {
      fieldHighlight = new BooleanFieldSetting();
      addFieldHighlightActionListeners();
    }
    if (fieldHighlight != null) {
      fieldHighlight.copy(input);
      updateFieldHighlight(isSelected());
    }
  }

  private void addFieldHighlightActionListeners() {
    // Radio buttons turn off when another button in the group is turned on. So
    // listen
    // to all of the radio buttons in the group.
    boolean listenerAdded = false;
    if (group != null) {
      Enumeration<AbstractButton> enumeration = group.getElements();
      if (enumeration != null) {
        while (enumeration.hasMoreElements()) {
          listenerAdded = true;
          // enumeration.nextElement().addActionListener(this);
          enumeration.nextElement().addItemListener(this);
        }
      }
    }
    if (!listenerAdded) {
      // radioButton.addActionListener(this);
      radioButton.addItemListener(this);
    }
  }

  public void setFieldHighlight(final boolean value) {
    if (fieldHighlight == null) {
      fieldHighlight = new BooleanFieldSetting();
      addFieldHighlightActionListeners();
    }
    fieldHighlight.set(value);
    updateFieldHighlight(isSelected());
  }

  public void setFieldHighlight(final String value) {}

  public void clearFieldHighlight() {
    if (fieldHighlight != null && fieldHighlight.isSet()) {
      fieldHighlight.reset();
      // Turn off field highlight - parameter doesn't matter since field
      // highlight is off.
      updateFieldHighlight(false);
    }
  }

  public FieldSettingInterface getFieldHighlight() {
    return fieldHighlight;
  }

  public boolean equalsFieldHighlight() {
    return fieldHighlight != null && fieldHighlight.isSet()
      && fieldHighlight.equals(isSelected());
  }

  public boolean equalsFieldHighlight(final String value) {
    return fieldHighlight != null && fieldHighlight.isSet()
      && fieldHighlight.equals(value != null && !value.matches("\\s*"));
  }

  boolean equalsFieldHighlight(final boolean input) {
    return fieldHighlight != null && fieldHighlight.isSet()
      && fieldHighlight.equals(input);
  }

  public void itemStateChanged(final ItemEvent itemEvent) {
    boolean selected;
    // Radio buttons cannot be turned off directly. When another button in the
    // group is
    // clicked, this button will turn off if it was on. This response doesn't
    // happen
    // instantly, but its accurate to assume that this button is off when
    // another button
    // was clicked.
    // if (!event.getActionCommand().equals(radioButton.getActionCommand())) {
    selected = false;
    // }
    // else {
    selected = isSelected();
    // }
    updateFieldHighlight(selected);
  }

  void updateFieldHighlight(final boolean isSelected) {
    if (fieldHighlight != null && fieldHighlight.isSet()
      && fieldHighlight.isValue() == isSelected) {
      if (origForeground == null) {
        origForeground = radioButton.getForeground();
        if (origForeground == null) {
          origForeground = Color.black;
        }
      }
      radioButton.setForeground(Colors.FIELD_HIGHLIGHT);
      return;
    }
    if (origForeground != null) {
      radioButton.setForeground(origForeground);
    }
  }

  /**
   * Sets a tooltip from a section using the enumeratedType, if it exists.
   *
   * @param section
   */
  void setToolTipText(final String autodocName, final ReadOnlySection section) {
    String text;
    if (enumeratedType == null) {
      text = EtomoAutodoc.getTooltip(autodocName, section, true);
    }
    else {
      text = EtomoAutodoc.getTooltip(autodocName, section, enumeratedType.toString());
    }
    setToolTipText(text);
  }

  public void setToolTipText(final String text) {
    radioButton.setToolTipText(TooltipFormatter.INSTANCE.format(text));
  }

  public void setTooltip(final Field field) {
    if (field != null) {
      radioButton.setToolTipText(field.getTooltip());
    }
  }

  public String getTooltip() {
    return radioButton.getToolTipText();
  }

  void setPreformattedTooltip(final String tooltip) {
    radioButton.setToolTipText(tooltip);
  }

  public void addTooltip(final String text) {
    if (text == null) {
      return;
    }
    String tooltip = radioButton.getToolTipText();
    if (tooltip == null) {
      setToolTipText(text);
    }
    else {
      radioButton
        .setToolTipText(tooltip + " & " + TooltipFormatter.INSTANCE.format(text));
    }
  }

  public void setVisible(final boolean visible) {
    radioButton.setVisible(visible);
  }

  public void addActionListener(final ActionListener actionListener) {
    radioButton.addActionListener(actionListener);
  }

  public final void removeActionListener(ActionListener actionListener) {
    radioButton.removeActionListener(actionListener);
  }

  void addChangeListener(final ChangeListener listener) {
    radioButton.addChangeListener(listener);
  }

  public void setSelected(final boolean selected) {
    radioButton.setSelected(selected);
    if (fieldHighlight != null) {
      updateFieldHighlight(isSelected());
    }
  }

  void setSelected(final ConstEtomoNumber selected, final boolean allowEmpty) {
    if (allowEmpty || (selected != null && !selected.isNull())) {
      setSelected(selected.is());
    }
  }

  public void msgSelected() {}

  public boolean isSelected() {
    return radioButton.isSelected();
  }

  public boolean isEmpty() {
    return false;
  }

  AbstractButton getAbstractButton() {
    return radioButton;
  }

  void setPreferredSize(final Dimension preferredSize) {
    radioButton.setPreferredSize(preferredSize);
  }

  public boolean isRequired() {
    return false;
  }

  /**
   * Returns button label.  No validation available.
   */
  public String getText(final boolean doValidation, final FieldDisplayer fieldDisplayer)
    throws FieldValidationFailedException {
    return radioButton.getText();
  }

  public String getText() {
    return radioButton.getText();
  }

  void setModel(final ButtonModel newModel) {
    radioButton.setModel(newModel);
  }

  public String getName() {
    return radioButton.getName();
  }

  public EnumeratedType getEnumeratedType() {
    return enumeratedType;
  }

  public SwingComponent getUIComponent() {
    return this;
  }

  public Component getComponent() {
    return radioButton;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    // Only visually enabled if both enabled and editable
    radioButton.setEnabled(enabled && editable);
    if (enabled && editable) {
      updateFieldHighlight(isSelected());
    }
  }

  public void setEditable(final boolean editable) {
    this.editable = editable;
    // Editable has no visible effect if the button is disabled.
    if (enabled) {
      radioButton.setEnabled(editable);
    }
    if (enabled && editable) {
      updateFieldHighlight(isSelected());
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isEditable() {
    return editable;
  }

  public String getActionCommand() {
    return radioButton.getActionCommand();
  }

  void setAlignmentX(float alignmentX) {
    radioButton.setAlignmentX(alignmentX);
  }

  Object[] getSelectedObjects() {
    return radioButton.getSelectedObjects();
  }

  public static final class RadioButtonModel extends AbstractRadioButtonModel {
    private final RadioButtonInterface radioButton;

    RadioButtonModel(RadioButtonInterface radioButton) {
      super();
      this.radioButton = radioButton;
    }

    RadioButtonInterface getButton() {
      return radioButton;
    }

    public void setSelected(boolean selected) {
      super.setSelected(selected);
      if (radioButton != null) {
        radioButton.msgSelected();
      }
    }

    public EnumeratedType getEnumeratedType() {
      return radioButton.getEnumeratedType();
    }

    public Field getField() {
      return radioButton.getField();
    }
  }
}
/**
 * <p> $Log$
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.25  2010/03/03 05:05:08  sueh
 * <p> bug# 1311 Added getFont and setVisible.
 * <p>
 * <p> Revision 1.24  2009/11/20 17:30:51  sueh
 * <p> bug# 1282 Added prefixes to all of the field names, so that the fields that
 * <p> are actually abstract buttons (radio buttons, etc) won't be activated by a
 * <p> "bn." field command.
 * <p>
 * <p> Revision 1.23  2009/04/13 22:58:01  sueh
 * <p> Removed newstuff.
 * <p>
 * <p> Revision 1.22  2009/02/27 03:54:22  sueh
 * <p> bug# 1172 Added experimental automation recording background color
 * <p> (newstuff only).
 * <p>
 * <p> Revision 1.21  2009/01/20 20:23:00  sueh
 * <p> bug# 1102 Changed UITestField to UITestFieldType.
 * <p>
 * <p> Revision 1.20  2008/05/30 22:33:01  sueh
 * <p> bug# 1102 Isolating the etomo.uitest package so it is not need for
 * <p> running EtomoDirector.
 * <p>
 * <p> Revision 1.19  2008/05/30 21:33:56  sueh
 * <p> bug# 1102 Moved uitest classes to etomo.uitest.
 * <p>
 * <p> Revision 1.18  2008/02/29 20:51:20  sueh
 * <p> bug# 1092 Added toString().
 * <p>
 * <p> Revision 1.17  2007/12/26 22:26:16  sueh
 * <p> bug# 1052 Moved argument handling from EtomoDirector to a separate class.
 * <p>
 * <p> Revision 1.16  2007/09/27 21:04:32  sueh
 * <p> bug# 1044 Expanded RadioButton so it can be used in RadioButtonCell.
 * <p>
 * <p> Revision 1.15  2007/09/07 00:28:23  sueh
 * <p> bug# 989 Using a public INSTANCE to refer to the EtomoDirector singleton
 * <p> instead of getInstance and createInstance.
 * <p>
 * <p> Revision 1.14  2007/05/17 23:50:02  sueh
 * <p> bug# 964 In setToolTipText(), return the tooltip.
 * <p>
 * <p> Revision 1.13  2007/05/08 01:20:51  sueh
 * <p> bug# 964 Added setToolTipText(ReadOnlySection) to set an enum tooltip.
 * <p>
 * <p> Revision 1.12  2007/04/13 20:37:15  sueh
 * <p> bug# 964 Removed radioValue and added EnumeratedType, which is the
 * <p> interface for enumeration types.
 * <p>
 * <p> Revision 1.11  2007/03/20 00:45:40  sueh
 * <p> bug# 964 Added constructors which take ButtonGroup.
 * <p>
 * <p> Revision 1.10  2007/03/07 21:12:32  sueh
 * <p> bug# 981 Turned RadioButton into a wrapper rather then a child of JRadioButton,
 * <p> because it is getting more complicated.
 * <p>
 * <p> Revision 1.9  2007/03/03 01:03:49  sueh
 * <p> bug# 973 Added a RadioButtonModel for classes that use a radio button, and
 * <p> want to respond to the setSelected calls that automatically turn off other buttons
 * <p> in the group.
 * <p>
 * <p> Revision 1.8  2007/02/09 00:52:13  sueh
 * <p> bug# 962 Made TooltipFormatter a singleton and moved its use to low-level ui
 * <p> classes.
 * <p>
 * <p> Revision 1.7  2006/05/16 21:36:30  sueh
 * <p> bug# 856 Changing the name whenever the label is changed so that its easy to
 * <p> see what the name is.
 * <p>
 * <p> Revision 1.6  2006/04/25 19:19:47  sueh
 * <p> bug# 787 Added UITestField, an enum style class which contains the
 * <p> fields found in uitestaxis.adoc files.
 * <p>
 * <p> Revision 1.5  2006/04/06 20:17:51  sueh
 * <p> bug# 808 Moved the function convertLabelToName from UIUtilities to
 * <p> util.Utilities.
 * <p>
 * <p> Revision 1.4  2006/01/12 17:37:28  sueh
 * <p> bug# 798 Moved the autodoc classes to etomo.storage.autodoc.
 * <p>
 * <p> Revision 1.3  2006/01/11 22:32:48  sueh
 * <p> bug# 675 fixed print names functionality
 * <p>
 * <p> Revision 1.2  2006/01/04 20:28:01  sueh
 * <p> bug# 675 For printing the name:  putting the type first and making the type
 * <p> as constant.
 * <p>
 * <p> Revision 1.1  2005/12/23 02:19:17  sueh
 * <p> bug# 675 A class to allow automatic naming of radio buttons.
 * <p> </p>
 */
