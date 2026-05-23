package etomo.ui;

public final class BooleanStateExtension {
  private final BooleanEfieldInterface field;

  private BooleanFieldSetting checkpoint = null;

  public BooleanStateExtension(final BooleanEfieldInterface field) {
    this.field = field;
  }

  public void checkpoint() {
    if (checkpoint == null) {
      checkpoint = new BooleanFieldSetting();
    }
    checkpoint.set(field.isSelected());
  }

  /**
   * @return the checkpoint value
   */
  public final boolean isCheckpointValue() {
    if (checkpoint == null) {
      return false;
    }
    return checkpoint.isValue();
  }
}
