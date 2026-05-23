package etomo.ui;

import etomo.ui.swing.TextEfieldInterface;

public final class TextStateExtension {
  private final TextEfieldInterface field;

  private TextFieldSetting backup = null;
  private TextFieldSetting checkpoint = null;

  public TextStateExtension(final TextEfieldInterface field) {
    this.field = field;
  }

  public void backup() {
    if (backup == null) {
      backup = new TextFieldSetting(FieldType.STRING);
    }
    backup.set(field.getText());
  }

  public void checkpoint() {
    if (checkpoint == null) {
      checkpoint = new TextFieldSetting(FieldType.STRING);
    }
    checkpoint.set(field.getText());
  }

  public boolean isCheckpointed() {
    return checkpoint != null && checkpoint.isSet();
  }

  /**
   * If the field was backed up, make the backup value the displayed value, and turn off
   * the back up.
   */
  public void restoreFromBackup() {
    if (backup != null && backup.isSet()) {
      field.setText(backup.getValue());
      backup.reset();
    }
  }

  /**
   * @param alwaysCheck - when false return false when the field is disabled or invisible
   * @return true if text field is different from checkpoint
   */
  public boolean isDifferentFromCheckpoint(final boolean alwaysCheck) {
    if (!alwaysCheck && (!field.isEnabled() || !field.isVisible())) {
      return false;
    }
    return checkpoint == null || !checkpoint.equals(field.getText());
  }
}