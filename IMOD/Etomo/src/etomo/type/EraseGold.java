package etomo.type;

public final class EraseGold implements EnumeratedType {
  public static final EraseGold FID = new EraseGold(1);
  public static final EraseGold FIND_3D = new EraseGold(2);

  private static final EraseGold DEFAULT = FID;

  private final EtomoNumber value = new EtomoNumber();

  private EraseGold(final int value) {
    this.value.set(value);
  }

  public static EraseGold getInstance(final String value) {
    if (FID.value.equals(value)) {
      return FID;
    }
    if (FIND_3D.value.equals(value)) {
      return FIND_3D;
    }
    return null;
  }

  public boolean isDefault() {
    return this == DEFAULT;
  }

  public ConstEtomoNumber getValue() {
    return value;
  }

  public String toString() {
    return value.toString();
  }

  public String getLabel() {
    return null;
  }
}
