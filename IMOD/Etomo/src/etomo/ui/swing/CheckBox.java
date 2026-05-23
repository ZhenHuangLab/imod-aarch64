package etomo.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.text.Document;

import etomo.EtomoDirector;
import etomo.logic.AutodocAttributeRetriever;
import etomo.storage.DirectiveDef;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.storage.autodoc.ReadOnlySection;
import etomo.type.ConstEtomoNumber;
import etomo.type.EtomoAutodoc;
import etomo.type.UITestFieldType;
import etomo.ui.BooleanFieldInterface;
import etomo.ui.BooleanFieldSetting;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldSettingInterface;
import etomo.ui.Field;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.UIComponent;
import etomo.util.Utilities;

/**
 * <p>Description: A self-naming check box.</p>
 * 
 * <p>Copyright: Copyright 2005 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *          <p/>
 *          <p> $Log$
 *          <p> Revision 1.4  2011/04/25 23:30:53  sueh
 *          <p> bug# 1416 Implemented StateChangeActionSource.  Added equals(Object)
 *          and equals(Document) so
 *          <p> StateChangedReporter can find instances of this class.
 *          <p>
 *          <p> Revision 1.3  2011/04/04 17:17:19  sueh
 *          <p> bug# 1416 Added savedValue, checkpoint, isChanged.
 *          <p>
 *          <p> Revision 1.2  2011/02/22 18:04:56  sueh
 *          <p> bug# 1437 Reformatting.
 *          <p>
 *          <p> Revision 1.1  2010/11/13 16:07:34  sueh
 *          <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 *          <p>
 *          <p> Revision 1.19  2010/03/05 04:02:01  sueh
 *          <p> bug# 1319 Added a constructor with no label string.
 *          <p>
 *          <p> Revision 1.18  2009/11/20 17:02:08  sueh
 *          <p> bug# 1282 Added prefixes to all of the field names,
 *          so that the fields that
 *          <p> are actually abstract buttons (radio buttons, etc) won't be activated by a
 *          <p> "bn." field command.
 *          <p>
 *          <p> Revision 1.17  2009/04/13 22:55:46  sueh
 *          <p> Removed newstuff.
 *          <p>
 *          <p> Revision 1.16  2009/02/27 03:50:23  sueh
 *          <p> bug# 1172 Added experimental automation recording background color
 *          <p> (newstuff only).
 *          <p>
 *          <p> Revision 1.15  2009/01/20 19:49:28  sueh
 *          <p> bug# 1102 Changed this UITestField to UITestFieldType.
 *          <p>
 *          <p> Revision 1.14  2008/10/27 20:30:26  sueh
 *          <p> bug# 1141 Added printinfo (debugging tool).
 *          <p>
 *          <p> Revision 1.13  2008/05/30 22:31:35  sueh
 *          <p> bug# 1102 Isolating the etomo.uitest package so it is not need for
 *          <p> running EtomoDirector.
 *          <p>
 *          <p> Revision 1.12  2008/05/30 21:28:02  sueh
 *          <p> bug# 1102 Moved uitest classes to etomo.uitest.
 *          <p>
 *          <p> Revision 1.11  2007/12/26 22:22:42  sueh
 *          <p> bug# 1052 Moved argument handling from EtomoDirector to a separate class.
 *          <p>
 *          <p> Revision 1.10  2007/09/07 00:26:17  sueh
 *          <p> bug# 989 Using a public INSTANCE to refer to the EtomoDirector singleton
 *          <p> instead of getInstance and createInstance.
 *          <p>
 *          <p> Revision 1.9  2007/02/09 00:47:40  sueh
 *          <p> bug# 962 Made TooltipFormatter a singleton and moved its use to
 *          low-level ui
 *          <p> classes.
 *          <p>
 *          <p> Revision 1.8  2007/02/05 23:34:30  sueh
 *          <p> bug# 962 Removed commented out functions.
 *          <p>
 *          <p> Revision 1.7  2006/05/16 21:35:17  sueh
 *          <p> bug# 856 Changing the name whenever the label is changed so that its
 *          easy to
 *          <p> see what the name is.
 *          <p>
 *          <p> Revision 1.6  2006/04/25 19:12:23  sueh
 *          <p> bug# 787 Added UITestField, an enum style class which contains the
 *          <p> fields found in uitestaxis.adoc files.
 *          <p>
 *          <p> Revision 1.5  2006/04/06 20:15:51  sueh
 *          <p> bug# 808 Moved the function convertLabelToName from UIUtilities to
 *          <p> util.Utilities.
 *          <p>
 *          <p> Revision 1.4  2006/01/12 17:08:14  sueh
 *          <p> bug# 798 Moved the autodoc classes to etomo.storage.autodoc.
 *          <p>
 *          <p> Revision 1.3  2006/01/11 21:58:46  sueh
 *          <p> bug# 675 corrected print name functionality
 *          <p>
 *          <p> Revision 1.2  2006/01/04 20:23:29  sueh
 *          <p> bug# 675 For printing the name:  putting the type first and making the
 *          type
 *          <p> as constant.
 *          <p>
 *          <p> Revision 1.1  2006/01/03 23:30:46  sueh
 *          <p> bug# 675 Extends JCheckBox.  Names the check box using the label.
 *          <p> </p>
 */
public final class CheckBox
  implements BooleanFieldInterface, ActionListener, UIComponent, SwingComponent {
  private final JCheckBox checkBox;

  private boolean debug = false;
  private Color origForeground = null;
  private DirectiveDef directiveDef = null;
  // Field settings should not be reset to null.
  private BooleanFieldSetting backup = null;
  private BooleanFieldSetting defaultValue = null;
  private BooleanFieldSetting checkpoint = null;
  private BooleanFieldSetting fieldHighlight = null;

  private boolean enabled = true;
  private boolean editable = true;
  private String selectedStringValue = null;
  private String tooltip = null;
  private String alternateTooltip = null;
  private String text = null;
  private String alternateText = null;

  CheckBox() {
    checkBox = new JCheckBox();
  }

  public CheckBox(String text) {
    checkBox = new JCheckBox(text);
    setName(text);
  }

  public String getName() {
    return checkBox.getName();
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
    return checkBox.isVisible();
  }

  public boolean isEmpty() {
    return false;
  }

  public boolean equals(final Object object) {
    return object == this;
  }

  public boolean equals(final Document document) {
    return false;
  }

  public void setSelectedStringValue(final String value) {
    selectedStringValue = value;
  }

  public boolean equalsSelectedStringValue(final String value) {
    if (selectedStringValue == null) {
      return value != null && !value.isEmpty();
    }
    return selectedStringValue.equals(value);
  }

  public boolean isRequired() {
    return false;
  }

  public boolean isSelected() {
    return checkBox.isSelected();
  }

  public String getText() {
    return checkBox.getText();
  }

  /**
   * Validation is not available for the label.
   * @return checkbox label
   */
  public String getText(final boolean doValidation, final FieldDisplayer fieldDisplayer)
    throws FieldValidationFailedException {
    return getText();
  }

  public String toString() {
    return "[text:" + getText() + "]";
  }

  public String getDescription() {
    return getQuotedLabel();
  }

  public String getQuotedLabel() {
    String label = getText();
    if (label == null || label.matches("\\s*")) {
      label = checkBox.getName();
    }
    return Utilities.quoteLabel(label);
  }

  /**
   * Sets the label.  Handles alternate text.
   * @param text
   */
  void setText(String text) {
    checkBox.setText(text);
    setName(text);
    this.text = text;
  }

  /**
   * Stores a second text.  Does not switch to the second text.
   * @see switchText
   */
  void setAlternateText(final String text) {
    alternateText = text;
  }

  /**
   * Switch to/from the alternate tooltip.
   * @param alternate
   */
  void switchText(final boolean alternate) {
    if (alternate && alternateText != null) {
      checkBox.setText(alternateText);
    }
    else {
      checkBox.setText(text);
    }
  }

  public void setSelected(final boolean input) {
    checkBox.setSelected(input);
    updateFieldHighlight();
  }

  void setSelected(final ConstEtomoNumber input, final boolean allowEmpty) {
    if (allowEmpty || (input != null && !input.isNull())) {
      setSelected(input.is());
    }
  }

  void setName(String text) {
    String name = Utilities.convertLabelToName(text);
    checkBox.setName(
      UITestFieldType.CHECK_BOX.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
      System.out
        .println(checkBox.getName() + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
    }
  }

  public void backup() {
    if (backup == null) {
      backup = new BooleanFieldSetting();
    }
    backup.set(isSelected());
  }

  /**
   * If the field was backed up, make the backup value the displayed value, and turn off
   * the back up.
   */
  public void restoreFromBackup() {
    if (backup != null && backup.isSet()) {
      checkBox.setSelected(backup.isValue());
      updateFieldHighlight();
    }
  }

  void setActionCommand(final String input) {
    checkBox.setActionCommand(input);
  }

  public void setAlignmentX(final float input) {
    checkBox.setAlignmentX(input);
  }

  void setBackground(final Color color) {
    checkBox.setBackground(color);
  }

  public void clear() {
    checkBox.setSelected(false);
    updateFieldHighlight();
  }

  /**
   * Copy the value, checkpoint, and field highlight settings.
   *
   * @param input
   */
  public void setValue(final Field input) {
    if (input == null) {
      clear();
    }
    else {
      checkBox.setSelected(input.isSelected());
      updateFieldHighlight();
    }
  }

  public void setValue(final String value) {}

  public void setVisible(final boolean input) {
    checkBox.setVisible(input);
  }

  public void setValue(final boolean value) {
    checkBox.setSelected(value);
    updateFieldHighlight();
  }

  public void setDirectiveDef(final DirectiveDef directiveDef) {
    this.directiveDef = directiveDef;
    if (defaultValue != null) {
      defaultValue.reset();
    }
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
    // only search for default value once for this directiveDef
    if (defaultValue == null) {
      defaultValue = new BooleanFieldSetting();
      String value = AutodocAttributeRetriever.INSTANCE.getDefaultValue(directiveDef);
      if (value != null) {
        // if default value has been found, set it in the field setting
        defaultValue.set(value);
      }
    }
    if (defaultValue.isSet()) {
      checkBox.setSelected(defaultValue.isValue());
      updateFieldHighlight();
    }
  }

  public boolean equalsDefaultValue() {
    return defaultValue != null && defaultValue.equals(isSelected());
  }

  public boolean equalsDefaultValue(final String value) {
    return defaultValue != null && defaultValue.equals(value);
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

  public FieldSettingInterface getCheckpoint() {
    return checkpoint;
  }

  public SwingComponent getUIComponent() {
    return this;
  }

  public Component getComponent() {
    return checkBox;
  }

  public void setCheckpoint(final FieldSettingInterface input) {
    if (checkpoint == null && input != null && input.isSet() && input.isBoolean()) {
      checkpoint = new BooleanFieldSetting();
    }
    if (checkpoint != null) {
      checkpoint.copy(input);
    }
  }

  /**
   * Resets to checkpointValue if checkpointValue has been set.  Otherwise has no effect.
   */
  void resetToCheckpoint() {
    if (checkpoint == null || !checkpoint.isSet()) {
      return;
    }
    checkBox.setSelected(checkpoint.isValue());
    updateFieldHighlight();
  }

  void setDebug(final boolean input) {
    debug = input;
  }

  public void setFieldHighlight(final boolean value) {
    if (fieldHighlight == null) {
      fieldHighlight = new BooleanFieldSetting();
      checkBox.addActionListener(this);
    }
    fieldHighlight.set(value);
    updateFieldHighlight();
  }

  public void setFieldHighlight(final String input) {
    if (fieldHighlight == null && input != null) {
      fieldHighlight = new BooleanFieldSetting();
      checkBox.addActionListener(this);
    }
    if (fieldHighlight != null) {
      fieldHighlight.set(input);
      updateFieldHighlight();
    }
  }

  public BooleanFieldSetting getFieldHighlight() {
    return fieldHighlight;
  }

  public void setFieldHighlight(FieldSettingInterface input) {
    if (fieldHighlight == null && input != null && input.isSet() && input.isBoolean()) {
      fieldHighlight = new BooleanFieldSetting();
      checkBox.addActionListener(this);
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

  public boolean isFieldHighlightSet() {
    return fieldHighlight != null && fieldHighlight.isSet();
  }

  public boolean equalsFieldHighlight() {
    return fieldHighlight != null && fieldHighlight.isSet()
      && fieldHighlight.equals(isSelected());
  }

  public boolean equalsFieldHighlight(final String value) {
    return fieldHighlight != null && fieldHighlight.isSet()
      && fieldHighlight.equals(value);
  }

  public String getActionCommand() {
    return checkBox.getActionCommand();
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    // Only visually enabled if both enabled and editable
    checkBox.setEnabled(enabled && editable);
    if (enabled && editable) {
      updateFieldHighlight();
    }
  }

  void setEditable(final boolean editable) {
    this.editable = editable;
    // Editable has no visible effect if the button is disabled.
    if (enabled) {
      checkBox.setEnabled(editable);
    }
    if (enabled && editable) {
      updateFieldHighlight();
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  boolean isEditable() {
    return editable;
  }

  public void actionPerformed(ActionEvent e) {
    updateFieldHighlight();
  }

  public void addActionListener(final ActionListener listener) {
    checkBox.addActionListener(listener);
  }

  private void updateFieldHighlight() {
    if (fieldHighlight != null && fieldHighlight.equals(isSelected())) {
      if (origForeground == null) {
        // origForeground must be set if the foreground is going to be changed
        origForeground = checkBox.getForeground();
        if (origForeground == null) {
          origForeground = Color.black;
        }
      }
      checkBox.setForeground(Colors.FIELD_HIGHLIGHT);
      return;
    }
    if (origForeground != null) {
      // Field highlight value currently doesn't match the field text, or field
      // highlight
      // was removed.
      checkBox.setForeground(origForeground);
    }
  }

  /**
   * If the field is disabled or not visible then return false because its value doesn't
   * matter.  It returns true if the checkpoint has not been done; the checkpoint value is
   * from an outside value, so the current value must be different from a non-existant
   * checkpoint value.  After eliminating this possibility, it returns a boolean based on
   * the difference between the selected state of the checkbox and the checkpointed value.
   *
   * @return
   */
  boolean isDifferentFromCheckpoint() {
    return isDifferentFromCheckpoint(false);
  }

  /**
   * @param alwaysCheck - check for difference even when the field is disables or
   *                    invisible
   * @return
   */
  public boolean isDifferentFromCheckpoint(final boolean alwaysCheck) {
    if (!alwaysCheck && (!isEnabled() || !checkBox.isVisible())) {
      return false;
    }
    return checkpoint == null || !checkpoint.equals(isSelected());
  }

  public void setToolTipText(String text) {
    setPreformattedTooltip(TooltipFormatter.INSTANCE.format(text));
  }

  void setToolTipText(final String autodocName, final ReadOnlySection section,
    final String enumValue) {
    setToolTipText(EtomoAutodoc.getTooltip(autodocName, section, enumValue));
  }

  public void setTooltip(final Field field) {
    if (field != null) {
      setPreformattedTooltip(field.getTooltip());
    }
  }

  /**
   * Sets a preformatted tooltip.
   * @param tooltip
   */
  void setPreformattedTooltip(final String tooltip) {
    checkBox.setToolTipText(tooltip);
    this.tooltip = tooltip;
  }

  /**
   * Stores a second tooltip.  Does not switch to the second tooltip.
   * @see switchTooltips
   */
  public void setAlternateTooltipText(final String text) {
    alternateTooltip = TooltipFormatter.INSTANCE.format(text);
  }

  /**
   * Switch to/from the alternate tooltip.
   * @param alternate
   */
  public void switchTooltips(final boolean alternate) {
    if (alternate && alternateTooltip != null) {
      checkBox.setToolTipText(alternateTooltip);
    }
    else {
      checkBox.setToolTipText(tooltip);
    }
  }

  /**
   * Gets the tooltip that is currently in use.
   */
  public String getTooltip() {
    return checkBox.getToolTipText();
  }

  void printInfo() {
    System.out.println(checkBox.getName());
    printInfo(checkBox.getParent());
  }

  private void printInfo(Container parent) {
    System.out.println(parent);
    if (parent != null) {
      String parentName = parent.getName();
      if (parentName != null) {
        System.out.println(parent.getName());
      }
      else {
        printInfo(parent.getParent());
      }
    }
  }
}
