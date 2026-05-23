package etomo.type;

/**
 * <p>Description: Instances correspond to the endingStep parameters in
 * batchruntomo.  An enumerated type for radio buttons.</p>
 * <p/>
 * <p>Copyright: Copyright 2015 - 2016 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class StartingStep implements EnumeratedType {
  private static final StartingStep FINE_ALIGNMENT =
    new StartingStep(0, Step.FINE_ALIGNMENT, "Start from fine alignment with Tiltalign.");
  private static final StartingStep POSITIONING =
    new StartingStep(1, Step.POSITIONING, "Start with tomogram positioning (if any).");
  private static final StartingStep ALIGNED_STACK_GENERATION =
    new StartingStep(2, Step.ALIGNED_STACK_GENERATION,
      "Start with generating the aligned stack from the raw stack.");
  private static final StartingStep CTF_CORRECTION = new StartingStep(3,
    Step.CTF_CORRECTION, "Start with correcting the CTF then erasing the gold (if any).");
  private static final StartingStep RECONSTRUCTION =
    new StartingStep(4, Step.RECONSTRUCTION, "Start with making the reconstruction.");

  private final int index;
  private final Step step;
  private final String tooltip;

  private StartingStep(final int index, final Step step, final String tooltip) {
    this.index = index;
    this.step = step;
    this.tooltip = tooltip;
  }

  public static StartingStep getInstance(final String stepValue) {
    return getInstance(Step.getInstance(stepValue));
  }

  public static StartingStep getInstance(final int index) {
    if (index == FINE_ALIGNMENT.index) {
      return FINE_ALIGNMENT;
    }
    if (index == POSITIONING.index) {
      return POSITIONING;
    }
    if (index == ALIGNED_STACK_GENERATION.index) {
      return ALIGNED_STACK_GENERATION;
    }
    if (index == CTF_CORRECTION.index) {
      return CTF_CORRECTION;
    }
    if (index == RECONSTRUCTION.index) {
      return RECONSTRUCTION;
    }
    return null;
  }

  private static StartingStep getInstance(final Step step) {
    if (step == FINE_ALIGNMENT.step) {
      return FINE_ALIGNMENT;
    }
    if (step == POSITIONING.step) {
      return POSITIONING;
    }
    if (step == ALIGNED_STACK_GENERATION.step) {
      return ALIGNED_STACK_GENERATION;
    }
    if (step == CTF_CORRECTION.step) {
      return CTF_CORRECTION;
    }
    if (step == RECONSTRUCTION.step) {
      return RECONSTRUCTION;
    }
    return null;
  }

  public boolean isDefault() {
    return this == CTF_CORRECTION;
  }

  public String toString() {
    return step.toString();
  }

  public String getTooltip() {
    return tooltip;
  }

  public int getIndex() {
    return index;
  }

  public String getLabel() {
    return step.getLabel();
  }

  public ConstEtomoNumber getValue() {
    return step.getValue();
  }
}
