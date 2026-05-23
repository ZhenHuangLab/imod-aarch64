package etomo.ui.swing;

import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.BaseScreenState;
import etomo.type.DialogType;
import etomo.type.ProcessResultDisplay;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2006 - 2016 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version "$Id$
 */
public final class ProcessResultDisplayFactory
  extends AbstractProcessResultDisplayFactory {
  private final BaseScreenState screenState;

  // Recon
  // preprocessing

  private final ProcessResultDisplay findXRays =
    Run3dmodButton.getDeferredToggle3dmodInstance("Find X-rays (Trial Mode)",
      DialogType.PRE_PROCESSING);
  private final ProcessResultDisplay createFixedStack =
    Run3dmodButton.getDeferredToggle3dmodInstance(CcdEraserXRaysPanel.ERASE_LABEL,
      DialogType.PRE_PROCESSING);
  private final ProcessResultDisplay useFixedStack =
    MultiLineButton.getToggleButtonInstance(CcdEraserXRaysPanel.USE_FIXED_STACK_LABEL,
      DialogType.PRE_PROCESSING);

  // coarse alignment

  private final ProcessResultDisplay coarseTiltxcorr = MultiLineButton
    .getToggleButtonInstance("Calculate Cross-Correlation", DialogType.COARSE_ALIGNMENT);
  private final ProcessResultDisplay distortionCorrectedStack =
    MultiLineButton.getToggleButtonInstance("Make Distortion Corrected Stack",
      DialogType.COARSE_ALIGNMENT);
  private final ProcessResultDisplay fixEdgesMidas = MultiLineButton
    .getToggleButtonInstance("Fix Edges With Midas", DialogType.COARSE_ALIGNMENT);
  private final ProcessResultDisplay coarseAlign =
    Run3dmodButton.getDeferredToggle3dmodInstance("Generate Coarse Aligned Stack",
      DialogType.COARSE_ALIGNMENT);
  private final ProcessResultDisplay midas = MultiLineButton
    .getToggleButtonInstance("Fix Alignment With Midas", DialogType.COARSE_ALIGNMENT);

  // fiducial model

  private final ProcessResultDisplay transferFiducials =
    Run3dmodButton.getDeferredToggle3dmodInstance("Transfer Fiducials From Other Axis",
      DialogType.FIDUCIAL_MODEL);
  private final ProcessResultDisplay raptor =
    Run3dmodButton.getDeferredToggle3dmodInstance(RaptorPanel.RUN_RAPTOR_LABEL,
      DialogType.FIDUCIAL_MODEL);
  private final ProcessResultDisplay useRaptor = MultiLineButton.getToggleButtonInstance(
    RaptorPanel.USE_RAPTOR_RESULT_LABEL, DialogType.FIDUCIAL_MODEL);
  private final ProcessResultDisplay seedFiducialModel =
    Run3dmodButton.getToggle3dmodInstance(FiducialModelDialog.SEEDING_NOT_DONE_LABEL,
      DialogType.FIDUCIAL_MODEL);
  private final ProcessResultDisplay trackFiducials = MultiLineButton
    .getToggleButtonInstance(BeadtrackPanel.TRACK_LABEL, DialogType.FIDUCIAL_MODEL);
  private final ProcessResultDisplay fixFiducialModel = Run3dmodButton
    .getToggle3dmodInstance("Fix Fiducial Model", DialogType.FIDUCIAL_MODEL);
  private final ProcessResultDisplay trackTiltxcorr = Run3dmodButton
    .getDeferredToggle3dmodInstance("Track Patches", DialogType.FIDUCIAL_MODEL);
  private final Run3dmodButton autofidseed =
    Run3dmodButton.getDeferredToggle3dmodInstance(
      FiducialModelDialog.AUTOFIDSEED_NEW_MODEL_LABEL, DialogType.FIDUCIAL_MODEL);
  private final ProcessResultDisplay imodchopconts =
    Run3dmodButton.getDeferredToggle3dmodInstance("Recut or Restore Contours",
      DialogType.FIDUCIAL_MODEL);
  private final ProcessResultDisplay useAdjustedTrackCom = MultiLineButton
    .getToggleButtonInstance("Use Adjusted Track Com File", DialogType.FIDUCIAL_MODEL);

  // fine alignment

  private final ProcessResultDisplay computeAlignment = MultiLineButton
    .getToggleButtonInstance("Compute Alignment", DialogType.FINE_ALIGNMENT);

  // positioning

  private final ProcessResultDisplay sampleTomogram =
    Run3dmodButton.getDeferredToggle3dmodInstance(
      TomogramPositioningDialog.SAMPLE_TOMOGRAMS_LABEL, DialogType.TOMOGRAM_POSITIONING);
  private final ProcessResultDisplay computePitch =
    MultiLineButton.getToggleButtonInstance("Compute Z Shift & Pitch Angles",
      DialogType.TOMOGRAM_POSITIONING);
  private final ProcessResultDisplay finalAlignment = MultiLineButton
    .getToggleButtonInstance("Create Final Alignment", DialogType.TOMOGRAM_POSITIONING);

  // final aligned stack

  private final ProcessResultDisplay fullAlignedStack =
    Run3dmodButton.getDeferredToggle3dmodInstance(
      NewstackOrBlendmontPanel.RUN_BUTTON_LABEL, DialogType.FINAL_ALIGNED_STACK);
  private final ProcessResultDisplay ctfCorrection =
    Run3dmodButton.getDeferredToggle3dmodInstance(
      FinalAlignedStackDialog.CTF_CORRECTION_LABEL, DialogType.FINAL_ALIGNED_STACK);
  private final ProcessResultDisplay useCtfCorrection =
    MultiLineButton.getToggleButtonInstance(
      FinalAlignedStackDialog.USE_CTF_CORRECTION_LABEL, DialogType.FINAL_ALIGNED_STACK);
  private final ProcessResultDisplay xfModel =
    Run3dmodButton.getDeferredToggle3dmodInstance("Transform Fiducial Model",
      DialogType.FINAL_ALIGNED_STACK);
  private final ProcessResultDisplay stackTilt =
    Run3dmodButton.getDeferredToggle3dmodInstance(Tilt3dFindPanel.TILT_3D_FIND_LABEL,
      DialogType.FINAL_ALIGNED_STACK);
  private final ProcessResultDisplay findBeads3d = Run3dmodButton
    .getDeferredToggle3dmodInstance("Run Findbeads3d", DialogType.FINAL_ALIGNED_STACK);
  private final ProcessResultDisplay reprojectModel =
    Run3dmodButton.getDeferredToggle3dmodInstance(
      ReprojectModelPanel.REPROJECT_MODEL_LABEL, DialogType.FINAL_ALIGNED_STACK);
  private final ProcessResultDisplay ccdEraserBeads =
    Run3dmodButton.getDeferredToggle3dmodInstance(CcdEraserBeadsPanel.CCD_ERASER_LABEL,
      DialogType.FINAL_ALIGNED_STACK);
  private final ProcessResultDisplay useCcdEraserBeads =
    Run3dmodButton.getDeferredToggle3dmodInstance(
      CcdEraserBeadsPanel.USE_ERASED_STACK_LABEL, DialogType.FINAL_ALIGNED_STACK);
  private final ProcessResultDisplay filter = Run3dmodButton
    .getDeferredToggle3dmodInstance("Filter", DialogType.FINAL_ALIGNED_STACK);
  private final ProcessResultDisplay useFilteredStack =
    MultiLineButton.getToggleButtonInstance(
      FinalAlignedStackDialog.USE_FILTERED_STACK_LABEL, DialogType.FINAL_ALIGNED_STACK);

  // generation

  private final ProcessResultDisplay useTrialTomogram =
    MultiLineButton.getToggleButtonInstance("Use Current Trial Tomogram",
      DialogType.TOMOGRAM_GENERATION);
  private final ProcessResultDisplay genTilt = Run3dmodButton
    .getDeferredToggle3dmodInstance("Generate Tomogram", DialogType.TOMOGRAM_GENERATION);
  private final ProcessResultDisplay deleteAlignedStack =
    MultiLineButton.getToggleButtonInstance("Delete Intermediate Image Stacks",
      DialogType.TOMOGRAM_GENERATION);
  private final ProcessResultDisplay multifiltSetup = Run3dmodButton
    .getDeferredToggle3dmodInstance("Run Filter Trials", DialogType.TOMOGRAM_GENERATION);
  private final ProcessResultDisplay sirtsetup = Run3dmodButton
    .getDeferredToggle3dmodInstance("Run SIRT", DialogType.TOMOGRAM_GENERATION);
  private final ProcessResultDisplay useSirt = MultiLineButton
    .getToggleButtonInstance("Use SIRT Output File", DialogType.TOMOGRAM_GENERATION);

  // combination

  private final ProcessResultDisplay createCombine = MultiLineButton
    .getToggleButtonInstance("Create Combine Scripts", DialogType.TOMOGRAM_COMBINATION);
  private final ProcessResultDisplay combine = Run3dmodButton
    .getDeferredToggle3dmodInstance("Start Combine", DialogType.TOMOGRAM_COMBINATION);
  private final ProcessResultDisplay restartCombine = Run3dmodButton
    .getDeferredToggle3dmodInstance("Restart Combine", DialogType.TOMOGRAM_COMBINATION);
  private final ProcessResultDisplay restartMatchvol1 =
    Run3dmodButton.getDeferredToggle3dmodInstance("Restart at Matchvol1",
      DialogType.TOMOGRAM_COMBINATION);
  private final ProcessResultDisplay restartPatchcorr =
    Run3dmodButton.getDeferredToggle3dmodInstance("Restart at Patchcorr",
      DialogType.TOMOGRAM_COMBINATION);
  private final ProcessResultDisplay restartMatchorwarp =
    Run3dmodButton.getDeferredToggle3dmodInstance("Restart at Matchorwarp",
      DialogType.TOMOGRAM_COMBINATION);
  private final ProcessResultDisplay restartVolcombine =
    Run3dmodButton.getDeferredToggle3dmodInstance("Restart at Volcombine",
      DialogType.TOMOGRAM_COMBINATION);

  // post processing
  private final ProcessResultDisplay trimVolume = Run3dmodButton
    .getDeferredToggle3dmodInstance("Trim Volume", DialogType.POST_PROCESSING);
  private final ProcessResultDisplay flatten =
    Run3dmodButton.getDeferredToggle3dmodInstance(FlattenVolumePanel.FLATTEN_LABEL,
      DialogType.POST_PROCESSING);
  private final ProcessResultDisplay flattenWarp =
    new MultiLineButton(FlattenVolumePanel.FLATTEN_WARP_LABEL);
  private final ProcessResultDisplay squeezeVolume = Run3dmodButton
    .getDeferredToggle3dmodInstance("Squeeze Volume", DialogType.POST_PROCESSING);
  private final ProcessResultDisplay smoothingAssessment =
    Run3dmodButton.getDeferredToggle3dmodInstance(
      SmoothingAssessmentPanel.FLATTEN_WARP_LABEL, DialogType.POST_PROCESSING);

  private ProcessResultDisplayFactory(BaseScreenState screenState, final AxisID axisID,
    final AxisType axisType) {
    // The factory instance's ID must be unique to the dataset.
    super("etomo.ui.swing.ProcessResultDisplayFactory"
      + (axisType == AxisType.DUAL_AXIS ? axisID.toString() : ""));
    this.screenState = screenState;
  }

  public static ProcessResultDisplayFactory getInstance(final BaseScreenState screenState,
    final AxisID axisID, final AxisType axisType) {
    ProcessResultDisplayFactory instance =
      new ProcessResultDisplayFactory(screenState, axisID, axisType);
    instance.initialize();
    return instance;
  }

  private void initialize() {
    // The purpose of dependency list is to give every toggle button the correct setting
    // when another button is pressed. The two situations currently handled are:
    // - going back to an earlier step and invalidating the output of a later step, and
    // - button mirroring.

    // The global dependency list:
    // All displays should be added to this list. The order in which the displays are
    // added affects the behavior.

    // The function of the global dependency list: when a display's process succeeds or
    // fails, the displays that follow it in the list will be unselected.

    // The display ID (second parameter) must be unique to each dependency within this
    // factory. The display ID will be saved to files as process data, so it must be the
    // same each time etomo runs. However, because process data is short-lived, this ID
    // does not have to be stable from version to version.
    int id = 0;
    // preprocessing
    addDependency(findXRays, id++);
    addDependency(createFixedStack, id++);
    addDependency(useFixedStack, id++);
    // coarse alignment
    addDependency(coarseTiltxcorr, id++);
    addDependency(distortionCorrectedStack, id++);
    addDependency(fixEdgesMidas, id++);
    addDependency(coarseAlign, id++);
    addDependency(midas, id++);
    // fiducial model
    addDependency(transferFiducials, id++);
    addDependency(seedFiducialModel, id++);
    addDependency(autofidseed, id++);
    addDependency(useAdjustedTrackCom, id++);
    addDependency(trackTiltxcorr, id++);
    addDependency(imodchopconts, id++);
    addDependency(raptor, id++);
    addDependency(useRaptor, id++);
    addDependency(trackFiducials, id++);
    addDependency(fixFiducialModel, id++);
    // fine alignment
    addDependency(computeAlignment, id++);
    // positioning
    addDependency(sampleTomogram, id++);
    addDependency(computePitch, id++);
    addDependency(finalAlignment, id++);
    // stack
    addDependency(fullAlignedStack, id++);
    addDependency(ctfCorrection, id++);
    addDependency(useCtfCorrection, id++);
    addDependency(xfModel, id++);
    addDependency(stackTilt, id++);
    addDependency(findBeads3d, id++);
    addDependency(reprojectModel, id++);
    addDependency(ccdEraserBeads, id++);
    addDependency(useCcdEraserBeads, id++);
    addDependency(filter, id++);
    addDependency(useFilteredStack, id++);
    // generation
    addDependency(genTilt, id++);
    addDependency(useTrialTomogram, id++);
    addDependency(deleteAlignedStack, id++);
    addDependency(multifiltSetup, id++);
    addDependency(sirtsetup, id++);
    addDependency(useSirt, id++);
    // combination
    addDependency(createCombine, id++);
    addDependency(combine, id++);
    addDependency(restartCombine, id++);
    addDependency(restartMatchvol1, id++);
    addDependency(restartPatchcorr, id++);
    addDependency(restartMatchorwarp, id++);
    addDependency(restartVolcombine, id++);
    // post processing
    addDependency(trimVolume, id++);
    addDependency(smoothingAssessment, id++);
    addDependency(flattenWarp, id++);
    addDependency(flatten, id++);
    addDependency(squeezeVolume, id++);

    // Turning off the use of the global dependency list for individual displays.
    // This is mostly used for buttons that create temporary or optional files. Temporary
    // file creaters are usually paired with a "Use..." button, which will still use the
    // global list.

    // These displays are still unselected by previous displays, since they are dependent
    // on the output of these displays. This is why they where added to the global list.

    midas.setUseGlobalDependencyList(false);
    raptor.setUseGlobalDependencyList(false);
    ctfCorrection.setUseGlobalDependencyList(false);
    xfModel.setUseGlobalDependencyList(false);
    stackTilt.setUseGlobalDependencyList(false);
    findBeads3d.setUseGlobalDependencyList(false);
    reprojectModel.setUseGlobalDependencyList(false);
    ccdEraserBeads.setUseGlobalDependencyList(false);
    filter.setUseGlobalDependencyList(false);
    deleteAlignedStack.setUseGlobalDependencyList(false);
    smoothingAssessment.setUseGlobalDependencyList(false);
    flattenWarp.setUseGlobalDependencyList(false);
    flatten.setUseGlobalDependencyList(false);
    squeezeVolume.setUseGlobalDependencyList(false);

    //
    // The display level lists:
    // For these lists, order does not count.
    //

    // The dependency list:
    // Works like the global list: when a display's process succeeds or fails, all
    // displays on the display's list are unselected. This can be used to allow temporary
    // and optional file creaters to affect only the buttons that they are teamed with.

    // fiducial model
    raptor.addDependentDisplay(useRaptor);
    // stack
    ctfCorrection.addDependentDisplay(useCtfCorrection);
    xfModel.addDependentDisplay(ccdEraserBeads);
    xfModel.addDependentDisplay(useCcdEraserBeads);
    stackTilt.addDependentDisplay(findBeads3d);
    stackTilt.addDependentDisplay(reprojectModel);
    stackTilt.addDependentDisplay(ccdEraserBeads);
    stackTilt.addDependentDisplay(useCcdEraserBeads);
    findBeads3d.addDependentDisplay(reprojectModel);
    findBeads3d.addDependentDisplay(ccdEraserBeads);
    findBeads3d.addDependentDisplay(useCcdEraserBeads);
    reprojectModel.addDependentDisplay(ccdEraserBeads);
    reprojectModel.addDependentDisplay(useCcdEraserBeads);
    ccdEraserBeads.addDependentDisplay(useCcdEraserBeads);
    filter.addDependentDisplay(useFilteredStack);
    // gen
    sirtsetup.addDependentDisplay(useSirt);
    deleteAlignedStack.addDependentDisplay(fullAlignedStack);
    // post processing
    flattenWarp.addDependentDisplay(flatten);

    // Display mirroring:

    // The failure list: when the process fails (and is therefore unselected), all
    // displays on this list are unselected.

    // The success list: when the process succeeds (and is selected), all displays on this
    // list are selected.

    // gen
    // Creating a trial tomogram and then using it is equivalent to running genTilt.
    useTrialTomogram.addSuccessDisplay(genTilt);
    // combination
    // Completely mirror combine and restartCombine
    combine.addFailureDisplay(restartCombine);
    combine.addSuccessDisplay(restartCombine);
    restartCombine.addFailureDisplay(combine);
    restartCombine.addSuccessDisplay(combine);

    // Setting display states:
    // Display states are generically saved and loaded from the .edf file using
    // screenState.

    // preprocessing
    findXRays.setScreenState(screenState);
    createFixedStack.setScreenState(screenState);
    useFixedStack.setScreenState(screenState);
    // coarse alignment
    coarseTiltxcorr.setScreenState(screenState);
    distortionCorrectedStack.setScreenState(screenState);
    fixEdgesMidas.setScreenState(screenState);
    coarseAlign.setScreenState(screenState);
    midas.setScreenState(screenState);
    // fiducial model
    transferFiducials.setScreenState(screenState);
    trackTiltxcorr.setScreenState(screenState);
    imodchopconts.setScreenState(screenState);
    raptor.setScreenState(screenState);
    useRaptor.setScreenState(screenState);
    seedFiducialModel.setScreenState(screenState);
    autofidseed.setScreenState(screenState);
    useAdjustedTrackCom.setScreenState(screenState);
    trackFiducials.setScreenState(screenState);
    fixFiducialModel.setScreenState(screenState);
    // fine alignment
    computeAlignment.setScreenState(screenState);
    // positioning
    sampleTomogram.setScreenState(screenState);
    computePitch.setScreenState(screenState);
    finalAlignment.setScreenState(screenState);
    // stack
    fullAlignedStack.setScreenState(screenState);
    ctfCorrection.setScreenState(screenState);
    useCtfCorrection.setScreenState(screenState);
    xfModel.setScreenState(screenState);
    stackTilt.setScreenState(screenState);
    findBeads3d.setScreenState(screenState);
    reprojectModel.setScreenState(screenState);
    ccdEraserBeads.setScreenState(screenState);
    useCcdEraserBeads.setScreenState(screenState);
    filter.setScreenState(screenState);
    useFilteredStack.setScreenState(screenState);
    // generation
    useTrialTomogram.setScreenState(screenState);
    genTilt.setScreenState(screenState);
    deleteAlignedStack.setScreenState(screenState);
    multifiltSetup.setScreenState(screenState);
    sirtsetup.setScreenState(screenState);
    useSirt.setScreenState(screenState);
    // combination
    createCombine.setScreenState(screenState);
    combine.setScreenState(screenState);
    restartCombine.setScreenState(screenState);
    restartMatchvol1.setScreenState(screenState);
    restartPatchcorr.setScreenState(screenState);
    restartMatchorwarp.setScreenState(screenState);
    restartVolcombine.setScreenState(screenState);
    // post processing
    trimVolume.setScreenState(screenState);
    smoothingAssessment.setScreenState(screenState);
    flattenWarp.setScreenState(screenState);
    flatten.setScreenState(screenState);
    squeezeVolume.setScreenState(screenState);
  }

  // preprocessing

  ProcessResultDisplay getFindXRays() {
    return findXRays;
  }

  ProcessResultDisplay getCreateFixedStack() {
    return createFixedStack;
  }

  ProcessResultDisplay getUseFixedStack() {
    return useFixedStack;
  }

  // coarse alignment

  ProcessResultDisplay getTiltxcorr(final DialogType dialogType) {
    if (dialogType == DialogType.COARSE_ALIGNMENT) {
      return coarseTiltxcorr;
    }
    else if (dialogType == DialogType.FIDUCIAL_MODEL) {
      return trackTiltxcorr;
    }
    return null;
  }

  ProcessResultDisplay getImodchopconts() {
    return imodchopconts;
  }

  ProcessResultDisplay getAutofidseed() {
    return autofidseed;
  }

  ProcessResultDisplay getUseAdjustedTrackCom() {
    return useAdjustedTrackCom;
  }

  ProcessResultDisplay getDistortionCorrectedStack() {
    return distortionCorrectedStack;
  }

  ProcessResultDisplay getFixEdgesMidas() {
    return fixEdgesMidas;
  }

  ProcessResultDisplay getCoarseAlign() {
    return coarseAlign;
  }

  ProcessResultDisplay getMidas() {
    return midas;
  }

  // fiducial model

  ProcessResultDisplay getTransferFiducials() {
    return transferFiducials;
  }

  ProcessResultDisplay getRaptor() {
    return raptor;
  }

  ProcessResultDisplay getUseRaptor() {
    return useRaptor;
  }

  ProcessResultDisplay getSeedFiducialModel() {
    return seedFiducialModel;
  }

  ProcessResultDisplay getTrackFiducials() {
    return trackFiducials;
  }

  ProcessResultDisplay getFixFiducialModel() {
    return fixFiducialModel;
  }

  // fine alignment

  public ProcessResultDisplay getComputeAlignment() {
    return computeAlignment;
  }

  // positioning

  ProcessResultDisplay getSampleTomogram() {
    return sampleTomogram;
  }

  ProcessResultDisplay getComputePitch() {
    return computePitch;
  }

  ProcessResultDisplay getFinalAlignment() {
    return finalAlignment;
  }

  // stack

  ProcessResultDisplay getFullAlignedStack() {
    return fullAlignedStack;
  }

  public ProcessResultDisplay getCtfCorrection() {
    return ctfCorrection;
  }

  ProcessResultDisplay getUseCtfCorrection() {
    return useCtfCorrection;
  }

  ProcessResultDisplay getXfModel() {
    return xfModel;
  }

  ProcessResultDisplay getFindBeads3d() {
    return findBeads3d;
  }

  public ProcessResultDisplay getTilt(DialogType dialogType) {
    if (dialogType == DialogType.FINAL_ALIGNED_STACK) {
      return stackTilt;
    }
    return genTilt;
  }

  ProcessResultDisplay getMultifiltSetup() {
    return multifiltSetup;
  }

  ProcessResultDisplay getSirtsetup() {
    return sirtsetup;
  }

  public ProcessResultDisplay getUseSirt() {
    return useSirt;
  }

  ProcessResultDisplay getReprojectModel() {
    return reprojectModel;
  }

  public ProcessResultDisplay getCcdEraserBeads() {
    return ccdEraserBeads;
  }

  ProcessResultDisplay getUseCcdEraserBeads() {
    return useCcdEraserBeads;
  }

  public ProcessResultDisplay getFilter() {
    return filter;
  }

  public ProcessResultDisplay getUseFilteredStack() {
    return useFilteredStack;
  }

  // generation

  ProcessResultDisplay getUseTrialTomogram() {
    return useTrialTomogram;
  }

  ProcessResultDisplay getDeleteAlignedStack() {
    return deleteAlignedStack;
  }

  // combination

  ProcessResultDisplay getCreateCombine() {
    return createCombine;
  }

  ProcessResultDisplay getCombine() {
    return combine;
  }

  ProcessResultDisplay getRestartCombine() {
    return restartCombine;
  }

  public ProcessResultDisplay getRestartMatchvol1() {
    return restartMatchvol1;
  }

  public ProcessResultDisplay getRestartPatchcorr() {
    return restartPatchcorr;
  }

  public ProcessResultDisplay getRestartMatchorwarp() {
    return restartMatchorwarp;
  }

  public ProcessResultDisplay getRestartVolcombine() {
    return restartVolcombine;
  }

  // post processing

  ProcessResultDisplay getTrimVolume() {
    return trimVolume;
  }

  ProcessResultDisplay getFlatten() {
    return flatten;
  }

  ProcessResultDisplay getFlattenWarp() {
    return flattenWarp;
  }

  ProcessResultDisplay getSmoothingAssessment() {
    return smoothingAssessment;
  }

  ProcessResultDisplay getSqueezeVolume() {
    return squeezeVolume;
  }
}
/**
 * <p> $Log$
 * <p> Revision 1.2  2011/02/11 23:10:57  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.1  2010/12/05 05:15:34  sueh
 * <p> bug# 1420 Moved ProcessResultDisplayFactory to etomo.ui.swing package.
 * <p>
 * <p> Revision 1.16  2010/11/13 16:06:53  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.15  2010/03/03 04:59:07  sueh
 * <p> bug# 1311 Added patchTracking.
 * <p>
 * <p> Revision 1.14  2009/12/19 01:11:16  sueh
 * <p> bug# 1294 Added smoothingAssessment.
 * <p>
 * <p> Revision 1.13  2009/10/01 18:49:00  sueh
 * <p> bug# 1239 Changed PostProcessDialog.getFlattenWarpDisplay to
 * <p> getFlattenWarpButton.
 * <p>
 * <p> Revision 1.12  2009/09/01 03:14:14  sueh
 * <p> bug# 1222 Added findBeads3d, flatten, flattenWarp, reprojectModel, and
 * <p> tilt3dFind.  Clarified code the adds dependancies.
 * <p>
 * <p> Revision 1.11  2009/05/02 01:12:06  sueh
 * <p> bug# 1216 Added raptor and useRaptor.
 * <p>
 * <p> Revision 1.10  2008/11/20 01:38:23  sueh
 * <p> bug# 1147 added ccdEraserBeads, useCcdEraserBeads, and xfModel.
 * <p>
 * <p> Revision 1.9  2008/10/27 20:15:32  sueh
 * <p> bug# 1141 Added ctfCorrection and useCtfCorrection.
 * <p>
 * <p> Revision 1.8  2008/10/16 20:59:47  sueh
 * <p> bug# 1141 Created FinalAlignedStack dialog to run full aligned stack and mtf filter.
 * <p>
 * <p> Revision 1.7  2008/05/07 02:45:21  sueh
 * <p> bug# 847 Getting the the postioning buttons from the expert.
 * <p>
 * <p> Revision 1.6  2008/05/03 00:45:32  sueh
 * <p> bug# 847 Reformatted.
 * <p>
 * <p> Revision 1.5  2008/01/14 22:02:44  sueh
 * <p> bug# 1050 Added getProcessResultDisplay(int dependencyIndex) which retrieves
 * <p> a display based on the unique dependency index.
 * <p>
 * <p> Revision 1.4  2007/09/10 20:34:58  sueh
 * <p> bug# 925 Using getInstance to construct ProcessResultDisplayFactory.  Calling
 * <p> initialize() from getInstance.  Putting all initialization into initialize().  Simplified
 * <p> the get functions.
 * <p>
 * <p> Revision 1.3  2006/03/20 17:59:11  sueh
 * <p> reformatted
 * <p>
 * <p> Revision 1.2  2006/02/06 21:18:17  sueh
 * <p> bug 521 Added all the process dialog toggle buttons.  Added an array
 * <p> of dependent displays.  Making the displays final so they can be added as
 * <p> dependent displays before they are initialized.
 * <p>
 * <p> Revision 1.1  2006/01/31 20:52:33  sueh
 * <p> bug# 521 Class to manage toggle buttons that affect or can be affected
 * <p> by other toggle buttons.  Defines how ProcessResultDisplay's affect each
 * <p> other.  Buttons continue to exist after their dialogs are removed.
 * <p> </p>
 */
