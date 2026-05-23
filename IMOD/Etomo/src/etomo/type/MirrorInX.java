package etomo.type;

/**
 * <p>Description: Transferfid parameter MirrorInX.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class MirrorInX implements EnumeratedType {
  public static final MirrorInX ASSESS_BOTH = new MirrorInX(0);
  public static final MirrorInX ALWAYS = new MirrorInX(1);
  public static final MirrorInX NEVER = new MirrorInX(-1);

  public static final MirrorInX DEFAULT = ASSESS_BOTH;

  private EtomoNumber value = new EtomoNumber();

  private MirrorInX(final int value) {
    this.value.set(value);
  }

  public static MirrorInX getInstance(ConstEtomoNumber input) {
    if (input == null) {
      return DEFAULT;
    }
    if (input.equals(ASSESS_BOTH.value)) {
      return ASSESS_BOTH;
    }
    if (input.equals(ALWAYS.value)) {
      return ALWAYS;
    }
    if (input.equals(NEVER.value)) {
      return NEVER;
    }
    return DEFAULT;
  }

  @Override
  public boolean isDefault() {
    return this == DEFAULT;
  }

  @Override
  public ConstEtomoNumber getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value.toString();
  }

  @Override
  public String getLabel() {
    if (this == ASSESS_BOTH) {
      return "Try with and without";
    }
    if (this == ALWAYS) {
      return "Use mirroring";
    }
    if (this == NEVER) {
      return "Do not use mirroring";
    }
    return null;
  }
}