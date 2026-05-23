package etomo.ui;

import etomo.storage.DirectiveDef;

/**
 * <p>Description: An interface to allow the generic handling of GUI fields.</p>
 * 
 * <p>Copyright: Copyright 2012 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface Field {
  public boolean isDebug();

  public String getName();

  public boolean isBoolean();

  /**
   * Returns true if the field contains any kind of text - including spinners.
   *
   * @return
   */
  public boolean isText();

  public String getQuotedLabel();

  public boolean isEnabled();

  /**
   * Resets value - does not change other settings
   */
  public void clear();

  public void setValue(Field from);

  /**
   * No effect in boolean-only fields
   *
   * @param text
   */
  public void setValue(String text);

  /**
   * No effect in text-only fields
   *
   * @param bool boolean value
   */
  public void setValue(boolean bool);

  public boolean isEmpty();

  /**
   * @return true if this a binary control (toggle button, radio button,
   * checkbox) and it is selected
   */
  public boolean isSelected();

  /**
   * @return true if the field must currently contain text (not required when disabled)
   */
  public boolean isRequired();

  public String getText();

  public String getText(boolean doValidation, FieldDisplayer fieldDisplayer)
    throws FieldValidationFailedException;

  /**
   * Returns true if there is a non-empty, non-whitespace value.  Boolean fields
   * are never empty.
   *
   * @return
   */

  public DirectiveDef getDirectiveDef();

  /**
   * Use DefaultFinder to find and set a default value.  DefaultFinder requires
   * DirectiveDef, and only works for comparam directives.
   */
  public void useDefaultValue();

  public boolean equalsDefaultValue();

  public boolean equalsDefaultValue(final String value);

  public void backup();

  /**
   * Set the value from the backup, and delete the backup.
   */
  public void restoreFromBackup();

  public void checkpoint();

  public void setCheckpoint(FieldSettingInterface input);

  public FieldSettingInterface getCheckpoint();

  public boolean isDifferentFromCheckpoint(boolean alwaysCheck);

  public boolean isFieldHighlightSet();

  public void clearFieldHighlight();

  public void setFieldHighlight(FieldSettingInterface input);

  public void setFieldHighlight(String input);

  /**
   * No effect in text-only fields
   *
   * @param input
   */
  public void setFieldHighlight(boolean input);

  public FieldSettingInterface getFieldHighlight();

  public boolean equalsFieldHighlight();

  public boolean equalsFieldHighlight(String value);

  public void setToolTipText(String tooltip);

  public void setTooltip(Field field);

  public String getTooltip();

  /**
   * Compares with selectedStringValue - a value associated with the field being selected.
   * A boolean field make be selected when its associated directive has a specific value.
   * The selectedStringValue is this directive value.  If selectedStringValue is not set,
   * it defaults to any non-empty value.
   * @param value
   * @return
   */
  public boolean equalsSelectedStringValue(String value);

  public void setDirectiveDef(DirectiveDef directiveDef);

  public String getDescription();
}
