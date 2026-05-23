package etomo.type;

/**
 * <p>Description: Instances correspond to the startingStep and endingStep parameters in
 * batchruntomo.</p>
 * 
 * <p>Copyright: Copyright 2015 - 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class Step implements Status {
  public static final Step SETUP = new Step("0");
  public static final Step BEAD_TRACKING = new Step("5", "Tracking", "Track");
  public static final Step FINE_ALIGNMENT = new Step("6", "Fine alignment", "Align");
  public static final Step POSITIONING = new Step("7", "Positioning", "Pos");
  static final Step ALIGNED_STACK_GENERATION = new Step("8", "Aligned stack");
  public static final Step GOLD_DETECTION_3D =
    new Step("10", "CTF/gold detection", "CTF/gold");
  static final Step CTF_CORRECTION = new Step("11", "Finish CTF/gold");
  public static final Step TWO_D_FILTERING = new Step("13", "Finished stack", "Stack");
  public static final Step RECONSTRUCTION = new Step("14", "Reconstruction");

  private final EtomoNumber value;
  private final String label, text;

  private Step(final String value, final String label, final String text) {
    this.label = label;
    this.text = text;
    if (value.indexOf('.') == -1) {
      this.value = new EtomoNumber();
    }
    else {
      this.value = new EtomoNumber(EtomoNumber.Type.DOUBLE);
    }
    this.value.set(value);
  }

  private Step(final String value) {
    this(value, null, null);
  }

  private Step(final String value, final String label) {
    this(value, label, null);
  }

  public static Step getInstance(final String value) {
    if (value == null) {
      return null;
    }
    if (SETUP.value.equals(value)) {
      return SETUP;
    }
    if (BEAD_TRACKING.value.equals(value)) {
      return BEAD_TRACKING;
    }
    if (FINE_ALIGNMENT.value.equals(value)) {
      return FINE_ALIGNMENT;
    }
    if (POSITIONING.value.equals(value)) {
      return POSITIONING;
    }
    if (ALIGNED_STACK_GENERATION.value.equals(value)) {
      return ALIGNED_STACK_GENERATION;
    }
    if (GOLD_DETECTION_3D.value.equals(value)) {
      return GOLD_DETECTION_3D;
    }
    if (CTF_CORRECTION.value.equals(value)) {
      return CTF_CORRECTION;
    }
    if (TWO_D_FILTERING.value.equals(value)) {
      return TWO_D_FILTERING;
    }
    if (RECONSTRUCTION.value.equals(value)) {
      return RECONSTRUCTION;
    }
    return null;
  }

  static Step getInstanceFromText(final String text) {
    if (text == null) {
      return null;
    }
    if (SETUP.text != null && SETUP.text.equals(text)) {
      return SETUP;
    }
    if (BEAD_TRACKING.text != null && BEAD_TRACKING.text.equals(text)) {
      return BEAD_TRACKING;
    }
    if (FINE_ALIGNMENT.text != null && FINE_ALIGNMENT.text.equals(text)) {
      return FINE_ALIGNMENT;
    }
    if (POSITIONING.text != null && POSITIONING.text.equals(text)) {
      return POSITIONING;
    }
    if (ALIGNED_STACK_GENERATION.text != null
      && ALIGNED_STACK_GENERATION.text.equals(text)) {
      return ALIGNED_STACK_GENERATION;
    }
    if (GOLD_DETECTION_3D.text != null && GOLD_DETECTION_3D.text.equals(text)) {
      return GOLD_DETECTION_3D;
    }
    if (CTF_CORRECTION.text != null && CTF_CORRECTION.text.equals(text)) {
      return CTF_CORRECTION;
    }
    if (TWO_D_FILTERING.text != null && TWO_D_FILTERING.text.equals(text)) {
      return TWO_D_FILTERING;
    }
    if (RECONSTRUCTION.text != null && RECONSTRUCTION.text.equals(text)) {
      return RECONSTRUCTION;
    }
    return null;
  }

  public String toString() {
    if (label != null) {
      return label;
    }
    return value.toString();
  }

  String getLabel() {
    return label;
  }

  ConstEtomoNumber getValue() {
    return value;
  }

  public String getText() {
    return text;
  }

  /**
   * Less then function bases order on the parameter value in batchruntomo.
   * @param input
   * @return
   */
  public boolean lt(Step input) {
    if (input == null) {
      return false;
    }
    return value.lt(input.value);
  }

  /**
   * Less then or equal to function bases order on the parameter value in batchruntomo.
   * @param input
   * @return
   */
  boolean le(Step input) {
    return this.value.le(input.value);
  }
}
