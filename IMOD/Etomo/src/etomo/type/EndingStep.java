package etomo.type;

/**
 * <p>Description: Instances correspond to the endingStep parameters in
 * batchruntomo.  An enumerated type for radio buttons.  Status: from
 * BatchRunTomoProcessMonitor to BatchRunTomoRow.</p>
 * <p/>
 * <p>Copyright: Copyright 2015 - 2016 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class EndingStep implements EnumeratedType, Status {
  private static final EndingStep BEAD_TRACKING = new EndingStep(0, Step.BEAD_TRACKING,
    "Stop after fiducial model generation or patch tracking.");
  private static final EndingStep FINE_ALIGNMENT =
    new EndingStep(1, Step.FINE_ALIGNMENT, "Stop after fine alignment with Tiltalign.");
  private static final EndingStep POSITIONING =
    new EndingStep(2, Step.POSITIONING, "Stop after tomogram positioning (if any).");
  private static final EndingStep GOLD_DETECTION_3D =
    new EndingStep(3, Step.GOLD_DETECTION_3D,
      "Stop after CTF estimation and detection of gold in 3D (if any).");
  private static final EndingStep TWO_D_FILTERING = new EndingStep(4,
    Step.TWO_D_FILTERING, "Stop when all steps on the aligned stack are completed.");

  public static final EndingStep MAX = TWO_D_FILTERING;

  private final int index;
  private final Step step;
  final String tooltip;

  private EndingStep(final int index, final Step step, final String tooltip) {
    this.index = index;
    this.step = step;
    this.tooltip = tooltip;
  }

  public static EndingStep getInstance(final String stepValue) {
    return getInstance(Step.getInstance(stepValue));
  }

  public static EndingStep getInstanceFromText(final String stepText) {
    return getInstance(Step.getInstanceFromText(stepText));
  }

  public static EndingStep getInstance(final int index) {
    if (index == BEAD_TRACKING.index) {
      return BEAD_TRACKING;
    }
    if (index == FINE_ALIGNMENT.index) {
      return FINE_ALIGNMENT;
    }
    if (index == POSITIONING.index) {
      return POSITIONING;
    }
    if (index == GOLD_DETECTION_3D.index) {
      return GOLD_DETECTION_3D;
    }
    if (index == TWO_D_FILTERING.index) {
      return TWO_D_FILTERING;
    }
    return null;
  }

  private static EndingStep getInstance(final Step step) {
    if (step == BEAD_TRACKING.step) {
      return BEAD_TRACKING;
    }
    if (step == FINE_ALIGNMENT.step) {
      return FINE_ALIGNMENT;
    }
    if (step == POSITIONING.step) {
      return POSITIONING;
    }
    if (step == GOLD_DETECTION_3D.step) {
      return GOLD_DETECTION_3D;
    }
    if (step == TWO_D_FILTERING.step) {
      return TWO_D_FILTERING;
    }
    return null;
  }

  public boolean isDefault() {
    return this == GOLD_DETECTION_3D;
  }

  /**
   * @return true if this is the first ending step to be executed by batchruntomo.
   */
  public boolean isFirst() {
    return this == FINE_ALIGNMENT;
  }

  public String toString() {
    return step.toString();
  }

  public int getIndex() {
    return index;
  }

  public String getTooltip() {
    return tooltip;
  }

  public String getLabel() {
    return step.getLabel();
  }

  public ConstEtomoNumber getValue() {
    return step.getValue();
  }

  public String getText() {
    return step.getText();
  }

  public boolean le(final EndingStep input) {
    if (input == null) {
      return true;
    }
    return step.le(input.step);
  }

  public boolean lt(final EndingStep input) {
    if (input == null) {
      return true;
    }
    return step.lt(input.step);
  }
}
