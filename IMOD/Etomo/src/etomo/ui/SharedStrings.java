package etomo.ui;

/**
* <p>Description: GUI labels that the rest of the program needs to know about.</p>
/**
 * <p>Description: GUI labels that the rest of the program needs to know about. </p>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class SharedStrings {
  public static final String D_PHI_LABEL = "Phi";
  public static final String D_THETA_LABEL = "Theta";
  public static final String D_PSI_LABEL = "Psi";
  public static final String EDGE_SHIFT_LABEL = "Edge shift";
  public static final String FLG_FAIR_REFERENCE_LABEL = "Multiparticle reference";
  public static final String INIT_MOTL_LABEL = "Initial Motive List";
  public static final String INIT_MOTL_X_AND_Z_AXIS_LABEL = "Align particle Y axes";
  public static final String INIT_MOTL_RANDOM_ROTATIONS = "Uniform random rotations";
  public static final String INIT_MOTL_RANDOM_AXIAL_ROTATIONS =
    "Random axial (Y) rotations";
  public static final String SAMPLE_SPHERE_LABEL = "Spherical Sampling for Theta and Psi";
  public static final String YAXIS_TYPE_LABEL = "Particle Y Axis";
  public static final String YAXIS_TYPE_Y_AXIS_LABEL = "Tomogram Y axis";
  public static final String YAXIS_TYPE_PARTICLE_MODEL_LABEL = "Particle model points";
  public static final String YAXIS_TYPE_CONTOUR_LABEL = "End points of contour";
  public static final String N_WEIGHT_GROUP_LABEL = "Weight groups";
  public static final String DEBUG_LEVEL_LABEL = "Debug level";
  public static final String FLG_ALIGN_AVERAGES_LABEL =
    "Align averages to have their Y axes vertical";
  public static final String FLG_REMOVE_DUPLICATES_LABEL =
    "Remove duplicate particles after each iteration";
  public static final String FLG_ABS_VALUE_LABEL =
    "Use absolute value of cross-correlation";
  public static final String FLG_STRICT_SEARCH_LIMITS_LABEL =
    "Strict search limit checking";
  public static final String FLG_NO_REFERENCE_REFINEMENT_LABEL =
    "No reference refinement";
  public static final String CSV_FILES_LABEL = "User supplied csv files";
  public static final String FLG_RANDOMIZE_LABEL = "Randomized particle selection";
  public static final String FLG_VOL_NAMES_ARE_TEMPLATES_LABEL =
    "File names are templates";
  public static final String STACK_TOOLTIP =
    "The A or B axis image stack for this dataset";
  public static final String IMOD_A_TOOLTIP = "Opens the A axis stack";
  public static final String IMOD_B_TOOLTIP = "Opens the B axis stack";
  public static final String EDIT_DATASET_TOOLTIP =
    "When \"Set\", dataset-specific values are in use.";
  public static final String DUAL_TOOLTIP = "Dataset is dual axis.";
  public static final String MONTAGE_TOOLTIP = "Dataset is a montage.";
  public static final String SKIP_TOOLTIP = "Views to exclude from A or only tilt series";
  public static final String BSKIP_TOOLTIP = "Views to exclude from B tilt series";
  public static final String RAW_BOUNDARY_MODEL =
    "Use a model drawn on raw stack to specify areas to include for autoseeding or patch "
      + "tracking.  Open stack, or A stack for dual axis, to create model.";
  public static final String SURFACES_TO_ANALYZE_2_TOOLTIP =
    "Select beads on two surfaces when autoseeding and assume beads on two surfaces when "
      + "aligning.";
  public static final String STEP_TOOLTIP = "Latest step successfully completed";
  public static final String TRUE_STRING = "Yes";
  public static final String FALSE_STRING = "No";
  public static final String OVERRIDE_TEXT = ">OVERRIDE<";
  public static final String SIRT_LIKE_FILTER_RADIO_BUTTON_TOOLTIP =
    "Use a radial filter that produces the same result as iterating with SIRT";
  public static final String GAUSSIAN_FILTER_RADIO_BUTTON_TOOLTIP =
    "Filter high frequencies with a Gaussian starting at the cutoff anfd falling off "
      + "with the given sigma";
  public static final String HAMMING_LIKE_FILTER_RADIO_BUTTON_TOOLTIP =
    "Filter high frequencies with a filter very similar to a Hamming window, starting at "
      + "the given frequency";
  public static final String EXACT_FILTER_RADIO_BUTTON_TOOLTIP =
    "Use 'exact filter' functions of Harauz and van Heel for the radial filter";
}
