package etomo.comscript;

import etomo.type.TiltAngleSpec;

/**
* <p>Description: Was ConstBeadtrack.  Do not modify this file except to
* deprecate functions that use member variables that are no longer in use.</p>
* 
* <p>Copyright: Copyright (c) 2005</p>
*
*<p>Organization:
* Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEM),
* University of Colorado</p>
* 
* @author $Author$
* 
* @version $Revision$
*/
 class OldConstBeadtrackParam {
  public static  final String  rcsid =  "$Id$";
   static final int nondefaultGroupSize = 3;
   static final boolean[] nondefaultGroupIntegerType = { true, true, true };
  
  //corresponds to ImageFile
   String inputFile;
  //corresponds to PieceListFile
   String pieceListFile;
  //corresponds to InputSeedModel
   String seedModelFile;
  //corresponds to OutputModel
   String outputModelFile;
  //changed to SkipViews
   String viewSkipList;
  //changed to RotationAngle
   double imageRotation;
  //not in use
   int nAdditionalViewSets;
  //corresponds to SeparateGroup
   StringList additionalViewGroups;
  //corresponds to FirstTiltAngle,TiltIncrement,TiltFile,TiltAngles
   TiltAngleSpec tiltAngleSpec;
  //changed to TiltDefaultGrouping
   FortranInputString tiltAngleGroupParams;
  //corresponds to TiltNondefaultGroup
   FortranInputString[] tiltAngleGroups;
  //changed to MagDefaultGrouping
   FortranInputString magnificationGroupParams;
  //corresponds to MagNondefaultGroup
   FortranInputString[] magnificationGroups;
  //changed to MinViewsForTiltalign
   int nMinViews;
  //changed to CentroidRadius,LightBeads
   FortranInputString fiducialParams;
  //corresponds to FillGaps
   boolean fillGaps;
  //changed to MaxGapSize
   int maxGap;

  //changed to MinTiltRangeToFindAxis,MinTiltRangeToFindAngles
   FortranInputString tiltAngleMinRange;
  //corresponds to BoxSizeXandY
   FortranInputString searchBoxPixels;
  //changed to MaxBeadsToAverage
   int maxFiducialsAvg;
  //corresponds to PointsToFitMaxAndMin
   FortranInputString fiducialExtrapolationParams;
  //corresponds to DensityRescueFractionAndSD
   FortranInputString rescueAttemptParams;
  //changed to DistanceRescueCriterion
   int minRescueDistance;
  //corresponds to RescueRelaxationDensityAndDistance
   FortranInputString rescueRelaxationParams;
  //changed to PostFitRescueResidual
   double residualDistanceLimit;
  //changed to DensityRelaxationPostFit,MaxRescueDistance
   FortranInputString secondPassParams;
  //corresponds to ResidualsToAnalyzeMaxAndMin
   FortranInputString meanResidChangeLimits;
  //corresponds to DeletionCriterionMinAndSD
   FortranInputString deletionParams;

   OldConstBeadtrackParam() {
    // The attributes of this class are initialized in the constructor because
    // many of them required additional calls besides construction for
    // appropriate initialization  
    additionalViewGroups = new StringList(0);

    tiltAngleSpec = new TiltAngleSpec();

    tiltAngleGroupParams = new FortranInputString(2);
    tiltAngleGroupParams.setIntegerType(0, true);
    tiltAngleGroupParams.setIntegerType(1, true);

    magnificationGroupParams = new FortranInputString(2);
    magnificationGroupParams.setIntegerType(0, true);
    magnificationGroupParams.setIntegerType(1, true);

    fiducialParams = new FortranInputString(2);
    fiducialParams.setIntegerType(1, true);
    tiltAngleMinRange = new FortranInputString(2);

    searchBoxPixels = new FortranInputString(2);
    searchBoxPixels.setIntegerType(0, true);
    searchBoxPixels.setIntegerType(1, true);

    fiducialExtrapolationParams = new FortranInputString(2);
    fiducialExtrapolationParams.setIntegerType(0, true);
    fiducialExtrapolationParams.setIntegerType(1, true);

    rescueAttemptParams = new FortranInputString(2);
    rescueAttemptParams.setIntegerType(1, true);

    rescueRelaxationParams = new FortranInputString(2);

    secondPassParams = new FortranInputString(2);

    meanResidChangeLimits = new FortranInputString(2);
    meanResidChangeLimits.setIntegerType(0, true);
    meanResidChangeLimits.setIntegerType(1, true);

    deletionParams = new FortranInputString(2);
    deletionParams.setIntegerType(0, false);
    deletionParams.setIntegerType(1, true);

  }

  /**
   * Validate the parameters stored in the BeadtrackObject
   */
   String validate() {
    StringBuffer errors = null;
    //  Compare the number of additional view sets and the number of entries in
    //  in additionalViewGroups
    if (nAdditionalViewSets != additionalViewGroups.getNElements()) {
      errors = new StringBuffer(
          "The number of additional view groups does not equal the number specified");
      errors.append("\nnumber of additional views sets: "
          + String.valueOf(nAdditionalViewSets));
      errors.append("\nnumber of list entries: "
          + String.valueOf(additionalViewGroups.getNElements()));
    }
    if (errors == null) {
      return null;
    }
    return errors.toString();
  }

   String getInputFile() {
    return inputFile;
  }

   String getPieceListFile() {
    return pieceListFile;
  }

   String getSeedModelFile() {
    return seedModelFile;
  }

   String getOutputModelFile() {
    return outputModelFile;
  }

  /**
   * @deprecated
   * @return
   */
   String getViewSkipList() {
    return viewSkipList;
  }

  /**
   * @deprecated
   * @return
   */
   double getImageRotation() {
    return imageRotation;
  }

  /**
   * @deprecated
   * @return
   */
   int getNAdditionalViewSets() {
    return nAdditionalViewSets;
  }

  public String getAdditionalViewGroups() {
    return additionalViewGroups.toString();
  }

   TiltAngleSpec getTiltAngleSpec() {
    return new TiltAngleSpec(tiltAngleSpec);
  }

  /**
   * @deprecated
   * @return
   */
   String getTiltAngleGroupParams() {
    return tiltAngleGroupParams.toString();
  }

  /**
   * @deprecated
   * @return
   */
   int getTiltAngleGroupSize() {
    return tiltAngleGroupParams.getInt(0);
  }

  public String getTiltAngleGroups() {
    return ParamUtilities.valueOf(tiltAngleGroups);
  }

  /**
   * @deprecated
   * @return
   */
   String getMagnificationGroupParams() {
    return magnificationGroupParams.toString();
  }

  /**
   * @deprecated
   * @return
   */
   int getMagnificationGroupSize() {
    return magnificationGroupParams.getInt(0);
  }

  public String getMagnificationGroups() {
    return ParamUtilities.valueOf(magnificationGroups);
  }

  /**
   * @deprecated
   * @return
   */
   int getNMinViews() {
    return nMinViews;
  }

  /**
   * @deprecated
   * @return
   */
   String getFiducialParams() {
    return fiducialParams.toString();
  }

  public boolean getFillGaps() {
    return fillGaps;
  }

  /**
   * @deprecated
   * @return
   */
   int getMaxGap() {
    return maxGap;
  }

  /**
   * @deprecated
   * @return
   */
   String getTiltAngleMinRange() {
    return tiltAngleMinRange.toString();
  }

  public String getSearchBoxPixels() {
    return searchBoxPixels.toString();
  }

  /**
   * @deprecated
   * @return
   */
   int getMaxFiducialsAvg() {
    return maxFiducialsAvg;
  }

  public String getFiducialExtrapolationParams() {
    return fiducialExtrapolationParams.toString();
  }

  public String getRescueAttemptParams() {
    return rescueAttemptParams.toString();
  }

  /**
   * @deprecated
   * @return
   */
   int getMinRescueDistance() {
    return minRescueDistance;
  }

  public String getRescueRelaxationParams() {
    return rescueRelaxationParams.toString();
  }

  /**
   * @deprecated
   * @return
   */
   double getResidualDistanceLimit() {
    return residualDistanceLimit;
  }

  /**
   * @deprecated
   * @return
   */
   String getSecondPassParams() {
    return secondPassParams.toString();
  }

  public String getMeanResidChangeLimits() {
    return meanResidChangeLimits.toString();
  }

  public String getDeletionParams() {
    return deletionParams.toString();
  }


}
/**
* <p> $Log$
* <p> Revision 1.1  2005/05/09 23:07:41  sueh
* <p> bug# 658 This class used to be ConstBeadtrackParam before the the PIP
* <p> upgrade.  Set functions that get member variables that are not used by
* <p> the new BeadtrackParam are deprecated.  Other then this sort of change,
* <p> this class should not be changed.
* <p> </p>
*/