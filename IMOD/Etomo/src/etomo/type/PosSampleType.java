package etomo.type;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 *
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class PosSampleType {
  public static final PosSampleType SAMPLES = new PosSampleType(0);
  public static final PosSampleType WHOLE = new PosSampleType(1);
  public static final PosSampleType WHOLE_CRYO = new PosSampleType(2);

  private final int sharedValue;

  private PosSampleType(final int sharedValue) {
    this.sharedValue = sharedValue;
  }

  public static PosSampleType getInstance(final int value) {
    if (value == SAMPLES.sharedValue) {
      return SAMPLES;
    }
    if (value == WHOLE.sharedValue) {
      return WHOLE;
    }
    if (value == WHOLE_CRYO.sharedValue) {
      return WHOLE_CRYO;
    }
    return null;
  }

  public int getValue() {
    return sharedValue;
  }
}
