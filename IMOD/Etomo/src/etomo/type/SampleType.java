package etomo.type;

/**
 * <p>Description: Values for runtime.Positioning.any.sampleType.</p>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class SampleType implements EnumeratedType {
  public static final SampleType NONE = new SampleType(0);
  public static final SampleType PLASTIC_SECTION = new SampleType(1);
  public static final SampleType CRYO = new SampleType(2);

  private static final SampleType DEFAULT = PLASTIC_SECTION;

  private final EtomoNumber value = new EtomoNumber();

  private SampleType(final int value) {
    this.value.set(value);
  }

  /**
   * @param input
   * @param useDefault - returns null if input is null
   * @return
   */
  public static SampleType getInstance(final ConstEtomoNumber input,
    final boolean useDefault) {
    if (input == null) {
      if (useDefault) {
        return DEFAULT;
      }
      return null;
    }
    if (input.equals(NONE.value)) {
      return NONE;
    }
    if (input.equals(PLASTIC_SECTION.value)) {
      return PLASTIC_SECTION;
    }
    if (input.equals(CRYO.value)) {
      return CRYO;
    }
    return null;
  }

  public static SampleType getInstance(final String value) {
    if (NONE.value.equals(value)) {
      return NONE;
    }
    if (PLASTIC_SECTION.value.equals(value)) {
      return PLASTIC_SECTION;
    }
    if (CRYO.value.equals(value)) {
      return CRYO;
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
