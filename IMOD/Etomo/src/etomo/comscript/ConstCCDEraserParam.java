package etomo.comscript;

import etomo.type.EtomoNumber;
import etomo.type.FileType;
import etomo.type.ScriptParameter;

/**
 * <p>Description: A read only model of the parameter interface for the
 *  ccderaser program</p>
 *
 * <p>Copyright: Copyright 2002 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 3.4  2010/04/28 15:49:51  sueh
 * <p> bug# 1344 Added outputFileType.
 * <p>
 * <p> Revision 3.3  2007/12/13 21:54:13  sueh
 * <p> bug# 1057 Added boundaryReplacementList.
 * <p>
 * <p> Revision 3.2  2005/02/22 20:55:00  sueh
 * <p> bug# 600 Making parameter name constants into public static final strings.
 * <p>
 * <p> Revision 3.1  2004/06/25 00:32:00  sueh
 * <p> bug# 467 OuterRadius is out-of-date.
 * <p> New parameter annulusWidth
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:00  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.2  2003/07/11 23:15:53  rickg
 * <p> new ccderaser mode
 * <p>
 * <p> Revision 2.1  2003/03/02 23:30:41  rickg
 * <p> Combine layout in progress
 * <p>
 * <p> Revision 2.0  2003/01/24 20:30:31  rickg
 * <p> Single window merge to main branch
 * <p>
 * <p> Revision 1.1.2.1  2003/01/24 18:33:42  rickg
 * <p> Single window GUI layout initial revision
 * <p>
 * <p> Revision 1.1  2002/09/09 22:57:02  rickg
 * <p> Initial CVS entry, basic functionality not including combining
 * <p> </p>
 */

public class ConstCCDEraserParam {
  public static final String rcsid = "$Id$";

  public static final String ANNULUS_WIDTH_KEY = "AnnulusWidth";
  public static final String INPUT_FILE_KEY = "InputFile";
  public static final String OUTPUT_FILE_KEY = "OutputFile";
  public static final String FIND_PEAKS_KEY = "FindPeaks";
  public static final String PEAK_CRITERION_KEY = "PeakCriterion";
  public static final String DIFF_CRITERION_KEY = "DiffCriterion";
  public static final String GROW_CRITERION_KEY = "GrowCriterion";
  public static final String SCAN_CRITERION_KEY = "ScanCriterion";
  public static final String MAXIMUM_RADIUS_KEY = "MaximumRadius";
  public static final String EXPAND_CIRCLE_ITERATIONS_KEY = "ExpandCircleIterations";
  public static final String X_Y_SCAN_SIZE_KEY = "XYScanSize";
  public static final String EDGE_EXCLUSION_WIDTH_KEY = "EdgeExclusionWidth";
  public static final String LINE_OBJECTS_KEY = "LineObjects";
  public static final String ALL_SECTION_OBJECTS_KEY = "AllSectionObjects";
  public static final String BORDER_SIZE_KEY = "BorderSize";
  public static final String POLYNOMIAL_ORDER_KEY = "PolynomialOrder";
  public static final String TRIAL_MODE_KEY = "TrialMode";
  public static final String BOUNDARY_OBJECTS_KEY = "BoundaryObjects";
  public static final String MODEL_FILE_KEY = "ModelFile";
  public static final String GIANT_CRITERION_KEY = "GiantCriterion";
  public static final String BIG_DIFF_CRITERION_KEY = "BigDiffCriterion";
  public static final String EXTRA_LARGE_RADIUS_KEY = "ExtraLargeRadius";

  final ScriptParameter betterRadius =
    new ScriptParameter(EtomoNumber.Type.DOUBLE, "BetterRadius");

  String inputFile = "";
  String outputFile = "";
  /**
   * Set to null when outputFile assigned to an unknown file
   */
  FileType outputFileType = null;

  boolean findPeaks = false;
  String peakCriterion = "";
  String diffCriterion = "";
  String growCriterion = "";
  String scanCriterion = "";
  String edgeExclusion = "";
  String maximumRadius = "";
  String expandCircleIterations = "";
  String annulusWidth;
  String xyScanSize = "";
  String pointModel = "";
  boolean trialMode;

  String modelFile = "";
  String globalReplacementList = "";
  String localReplacementList = "";
  String boundaryReplacementList = "";
  String borderPixels = "";
  String polynomialOrder = "";
  boolean includeAdjacentPoints = true;
  String giantCriterion = "";
  String bigDiffCriterion = "";
  String extraLargeRadius = "";

  // out of date parameter
  String outerRadius = ""; // replaced by annulusWidth

  public boolean isValid() {
    boolean valid = true;

    // Check to see if any of the integer parameters do not parse as integers
    // or there range is in appropriate
    try {
      int intBorderPixels = Integer.parseInt(borderPixels);
      if (intBorderPixels < 0) {
        valid = false;
      }
    }
    catch (NumberFormatException e) {
      valid = false;
    }
    return valid;
  }

  public String getInputFile() {
    return inputFile;
  }

  public String getOutputFile() {
    return outputFile;
  }

  public String getModelFile() {
    return modelFile;
  }

  public String getGlobalReplacementList() {
    return globalReplacementList;
  }

  public String getlocalReplacementList() {
    return localReplacementList;
  }

  public String getBoundaryReplacementList() {
    return boundaryReplacementList;
  }

  public String getBorderPixels() {
    return borderPixels;
  }

  public String getPolynomialOrder() {
    return polynomialOrder;
  }

  public boolean getIncludeAdjacentPoints() {
    return includeAdjacentPoints;
  }

  /**
   * @return
   */
  public String getDiffCriterion() {
    return diffCriterion;
  }

  /**
   * @return
   */
  public String getEdgeExclusion() {
    return edgeExclusion;
  }

  /**
   * @return
   */
  public boolean isFindPeaks() {
    return findPeaks;
  }

  /**
   * @return
   */
  public String getGrowCriterion() {
    return growCriterion;
  }

  public String getGiantCriterion() {
    return giantCriterion;
  }

  public String getBigDiffCriterion() {
    return bigDiffCriterion;
  }

  public String getExtraLargeRadius() {
    return extraLargeRadius;
  }

  /**
   * @return
   */
  public String getMaximumRadius() {
    return maximumRadius;
  }

  public boolean isExpandCircleIterationsSet() {
    return expandCircleIterations != "";
  }

  public boolean isBetterRadiusSet() {
    return !betterRadius.isNull();
  }

  public double getBetterRadius() {
    return betterRadius.getDouble();
  }

  /**
   * @return
   */
  public String getExpandCircleIterations() {
    return expandCircleIterations;
  }

  /**
   * @return
   */
  public String getAnnulusWidth() {
    return annulusWidth;
  }

  /**
   * @return
   */
  public String getPeakCriterion() {
    return peakCriterion;
  }

  /**
   * @return
   */
  public String getPointModel() {
    return pointModel;
  }

  /**
   * @return
   */
  public boolean isTrialMode() {
    return trialMode;
  }

  /**
   * @return
   */
  public String getXyScanSize() {
    return xyScanSize;
  }

  /**
   * @return
   */
  public String getScanCriterion() {
    return scanCriterion;
  }

}
