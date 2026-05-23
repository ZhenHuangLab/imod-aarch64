package etomo.ui.swing;

import java.io.File;
import java.io.IOException;

import etomo.ApplicationManager;
import etomo.ProcessSeries;
import etomo.TaskInterface;
import etomo.comscript.BlendmontParam;
import etomo.comscript.ComScriptManager;
import etomo.comscript.ConstNewstParam;
import etomo.comscript.ConstTiltParam;
import etomo.comscript.ConstTiltalignParam;
import etomo.comscript.CryoPositionParam;
import etomo.comscript.FindSectionParam;
import etomo.comscript.FortranInputSyntaxException;
import etomo.comscript.MakecomfileParam;
import etomo.comscript.NewstParam;
import etomo.comscript.ProcessDetails;
import etomo.comscript.TiltParam;
import etomo.comscript.TiltalignParam;
import etomo.comscript.TomopitchParam;
import etomo.comscript.XfproductParam;
import etomo.process.ImodManager;
import etomo.process.ProcessState;
import etomo.storage.TomopitchLog;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.ConstEtomoNumber;
import etomo.type.DialogExitState;
import etomo.type.DialogType;
import etomo.type.EtomoBoolean2;
import etomo.type.FileType;
import etomo.type.PosSampleType;
import etomo.type.ProcessName;
import etomo.type.ProcessResult;
import etomo.type.ProcessResultDisplay;
import etomo.type.ProcessTrack;
import etomo.type.Run3dmodMenuOptions;
import etomo.type.TomogramState;
import etomo.type.ViewType;
import etomo.util.DatasetFiles;
import etomo.util.InvalidParameterException;
import etomo.util.Utilities;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2006 - 2016 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class TomogramPositioningExpert extends ReconUIExpert {
  static final String SAMPLE_TOMOGRAMS_LABEL = "Create Sample Tomograms";
  private final ComScriptManager comScriptMgr;
  private final TomogramState state;
  private final AxisType axisType;

  private TomogramPositioningDialog dialog = null;

  private boolean advanced = false;

  public TomogramPositioningExpert(ApplicationManager manager,
    MainTomogramPanel mainPanel, ProcessTrack processTrack, AxisID axisID,
    final AxisType axisType) {
    super(manager, mainPanel, processTrack, axisID, DialogType.TOMOGRAM_POSITIONING);
    this.axisType = axisType;
    comScriptMgr = manager.getComScriptManager();
    state = manager.getState();
  }

  /**
   * Start the next process specified by the nextProcess string
   */
  public boolean startNextProcess(ProcessSeries.Process process,
    ProcessResultDisplay processResultDisplay, ProcessSeries processSeries,
    DialogType dialogType, ProcessDisplay display) {
    // whole tomogram
    if (process.equals(ProcessName.TILT.toString())) {
      sampleTilt(processResultDisplay, processSeries);
      return true;
    }
    if (process.equals(ProcessName.FIND_SECTION.toString())) {
      findSection(processResultDisplay, processSeries);
      return true;
    }
    return false;
  }

  /**
   * Post processing for sample and tilt.  ProcessDetails is required in both
   * cases.
   * @param processDetails
   * @param state
   */
  public void postProcess(final ProcessDetails processDetails, final TomogramState state,
    final PosSampleType posSampleType) {
    state.setPosSampleType(axisID, posSampleType);
    if (posSampleType == PosSampleType.WHOLE_CRYO) {
      state.setSampleXAxisTilt(axisID, 0);
    }
    else {
      state.setSampleXAxisTilt(axisID,
        processDetails.getDoubleValue(TiltParam.Field.X_AXIS_TILT));
    }
    boolean fiducialess = processDetails.getBooleanValue(TiltParam.Field.FIDUCIALESS);
    state.setSampleFiducialess(axisID, fiducialess);
    if (!fiducialess) {
      state.setSampleAxisZShift(axisID, state.getAlignAxisZShift(axisID));
      state.setSampleAngleOffset(axisID, state.getAlignAngleOffset(axisID));
    }
    else {
      // no alignment for fidless
      state.setSampleAxisZShift(axisID,
        processDetails.getDoubleValue(TiltParam.Field.Z_SHIFT));
      state.setSampleAngleOffset(axisID,
        processDetails.getDoubleValue(TiltParam.Field.TILT_ANGLE_OFFSET));
    }
    if (dialog != null) {
      dialog.updateDisplay();
    }
  }

  /**
   * Open the tomogram positioning dialog
   * @param axisID
   */
  public void openDialog() {
    if (!canShowDialog()) {
      return;
    }
    String actionMessage = manager.setCurrentDialogType(dialogType, axisID);
    if (showDialog(dialog, actionMessage)) {
      return;
    }
    // Create the dialog and show it.
    // Create a new dialog panel and map it the generic reference
    Utilities.timestamp("new", "TomogramPositioningDialog", Utilities.STARTED_STATUS);
    dialog = TomogramPositioningDialog.getInstance(manager, this, axisID, axisType,
      metaData.getViewType());
    Utilities.timestamp("new", "TomogramPositioningDialog", Utilities.FINISHED_STATUS);
    // Read in the meta data parameters. WARNING this needs to be done
    // before reading the tilt paramers below so that the GUI knows how to
    // correctly scale the dimensions
    dialog.setParameters(metaData);
    if (metaData.getViewType() != ViewType.MONTAGE) {
      comScriptMgr.loadNewst(axisID);
    }
    else {
      comScriptMgr.loadBlend(axisID);
    }

    // Get the align{|a|b}.com parameters
    comScriptMgr.loadAlign(axisID);
    TiltalignParam tiltalignParam = comScriptMgr.getTiltalignParam(axisID);
    if (metaData.getViewType() != ViewType.MONTAGE) {
      // upgrade and save param to comscript
      UIExpertUtilities.INSTANCE.upgradeOldAlignCom(manager, axisID, tiltalignParam);
    }
    dialog.setAlignParam(tiltalignParam);

    // Get the tilt{|a|b}.com parameters
    comScriptMgr.loadTilt(axisID);
    TiltParam tiltParam = comScriptMgr.getTiltParam(axisID);
    tiltParam.setFiducialess(metaData.isFiducialess(axisID));
    dialog.setTiltParam(tiltParam, !metaData.isPosExists(axisID));
    // If this is a montage, then binning can only be 1, so no need to upgrade
    if (metaData.getViewType() != ViewType.MONTAGE) {
      // upgrade and save param to comscript
      UIExpertUtilities.INSTANCE.upgradeOldTiltCom(manager, axisID, tiltParam);
    }

    // Get the tomopitch{|a|b}.com parameters
    comScriptMgr.loadTomopitch(axisID);
    dialog.setTomopitchParam(comScriptMgr.getTomopitchParam(axisID));

    // Set the whole tomogram sampling state, fidcialess state, and tilt axis
    // angle
    dialog.setWholeTomogram(metaData.isWholeTomogramSample(axisID));

    dialog.setFiducialess(metaData);
    dialog.setImageRotation(metaData.getImageRotation(axisID).toString());
    dialog.setButtonState(manager.getScreenState(axisID));
    fiducialessAction();
    // cryoPosition
    if (comScriptMgr.loadCryoPosition(axisID, false)) {
      CryoPositionParam param = comScriptMgr.getCryoPositionParam(axisID, axisType);
      if (param != null) {
        dialog.setParameters(param);
      }
    }
    openDialog(dialog, actionMessage);
    metaData.setPosExists(axisID, true);
  }

  void sampleAction(ProcessResultDisplay sample, ProcessSeries processSeries,
    Deferred3dmodButton deferred3dmodButton, Run3dmodMenuOptions run3dmodMenuOptions) {
    if (processSeries == null) {
      processSeries = new ProcessSeries(manager, axisID, dialogType);
    }
    if (dialog == null) {
      processSeries.endSeries();
      return;
    }
    processSeries.setRun3dmodDeferred(deferred3dmodButton, run3dmodMenuOptions);
    if (!dialog.isSampleTypeCryo()) {
      if (dialog.isWholeTomogram()) {
        wholeTomogram(sample, processSeries);
      }
      else {
        createSample(sample, processSeries);
      }
    }
    else {
      cryoPosition(sample, processSeries);
    }
  }

  void createBoundary(Run3dmodMenuOptions menuOptions) {
    if (dialog == null) {
      return;
    }
    if (dialog.isWholeTomogram()) {
      manager.imodFullSample(axisID, menuOptions);
    }
    else {
      manager.imodSample(axisID, menuOptions);
    }
  }

  void doneDialog() {
    if (dialog == null) {
      return;
    }
    DialogExitState exitState = dialog.getExitState();
    if (exitState == DialogExitState.EXECUTE) {
      ConstEtomoNumber sampleFiducialess = state.getSampleFiducialess(axisID);
      if ((sampleFiducialess == null || !sampleFiducialess.is())
        && dialog.isTomopitchButton() && dialog.isAlignButtonEnabled()
        && !dialog.isAlignButton()) {
        UIHarness.INSTANCE
          .openMessageDialog(manager,
            "ERROR:  Final alignment is not done or is out of date.  Run "
              + "final alignment in positioning before continuing.",
            "User Error", axisID);
      }
      manager.closeImod(ImodManager.SAMPLE_KEY, axisID, "sample reconstruction", false);
    }
    if (exitState != DialogExitState.CANCEL) {
      saveDialog();
    }
    leaveDialog(exitState);
    // Hold onto the finished dialog in case anything is running that needs it or
    // there are next processes that need it.
  }

  void fiducialessAction() {
    if (dialog == null) {
      return;
    }
    dialog.updateDisplay();
    // Save tilt param.
    TiltParam tiltParam = comScriptMgr.getTiltParam(axisID);
    dialog.setParametersFiducialess(tiltParam, metaData);
    manager.updateExcludeList(tiltParam, axisID);
    comScriptMgr.saveTilt(tiltParam, axisID);
    metaData.setFiducialess(axisID, tiltParam.isFiducialess());
    UIHarness.INSTANCE.pack(axisID, manager);
  }

  void saveDialog() {
    if (dialog == null) {
      return;
    }
    dialog.getParameters(metaData);
    advanced = dialog.isAdvanced();
    // Get all of the parameters from the panel
    EtomoBoolean2 sampleFiducialess = state.getSampleFiducialess(axisID);
    if (sampleFiducialess == null || !sampleFiducialess.is()) {
      updateAlignCom(false);
    }
    updateTomoPosTiltCom(false, false);
    updateTomopitchCom(false);
    updateCryoPositionCom(false, null);
    UIExpertUtilities.INSTANCE.updateFiducialessParams(manager, dialog, axisID, false);
    if (metaData.getViewType() != ViewType.MONTAGE) {
      updateNewstCom();
    }
    manager.saveStorables(axisID);
  }

  /**
   * Run the sample com script
   */
  public void createSample(ProcessResultDisplay processResultDisplay,
    ProcessSeries processSeries) {
    if (processSeries == null) {
      processSeries = new ProcessSeries(manager, axisID, dialogType);
    }
    sendMsgProcessStarting(processResultDisplay);
    // Make sure that we have an active positioning dialog
    if (dialog == null) {
      UIHarness.INSTANCE.openMessageDialog(manager,
        "Can not update sample.com without an active positioning dialog",
        "Program logic error", axisID);
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      processSeries.endSeries();
      return;
    }
    // Get the user input data from the dialog box
    if (!UIExpertUtilities.INSTANCE.updateFiducialessParams(manager, dialog, axisID,
      true)) {
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      processSeries.endSeries();
      return;
    }
    ConstTiltParam tiltParam = updateTomoPosTiltCom(true, true);
    if (tiltParam == null) {
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      processSeries.endSeries();
      return;
    }
    comScriptMgr.loadTilt(axisID);
    setDialogState(ProcessState.INPROGRESS);
    manager.closeImod(FileType.ALIGNED_STACK, axisID, true);
    if (dialog.isSampleTypeAuto()) {
      processSeries.setNextProcess(ProcessName.FIND_SECTION.toString(), null);
    }
    sendMsg(manager.createSample(axisID, processResultDisplay, processSeries, tiltParam),
      processResultDisplay);
  }

  public void cryoPosition(final ProcessResultDisplay processResultDisplay,
    ProcessSeries processSeries) {
    if (processSeries == null) {
      processSeries = new ProcessSeries(manager, axisID, dialogType);
    }
    sendMsgProcessStarting(processResultDisplay);
    // Make sure that we have an active positioning dialog
    if (dialog == null) {
      UIHarness.INSTANCE.openMessageDialog(manager,
        "Can not run cryposition without an active positioning dialog",
        "Program logic error", axisID);
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      processSeries.endSeries();
      return;
    }
    // Get the user input data from the dialog box
    if (!UIExpertUtilities.INSTANCE.updateFiducialessParams(manager, dialog, axisID,
      true)) {
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      processSeries.endSeries();
      return;
    }
    ConstTiltParam tiltParam = updateTomoPosTiltCom(true, true);
    if (tiltParam == null) {
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      processSeries.endSeries();
      return;
    }
    CryoPositionParam param = updateCryoPositionCom(true, processResultDisplay);
    // Post process requires information from tilt.com.
    if (param == null) {
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      processSeries.endSeries();
      return;
    }
    param.setTiltParam(tiltParam);
    setDialogState(ProcessState.INPROGRESS);
    manager.closeImod(FileType.ALIGNED_STACK, axisID, true);
    processSeries.setNextProcess(Task.POST_CRYO_POSITION);
    sendMsg(manager.cryoPosition(axisID, processResultDisplay, processSeries, param),
      processResultDisplay);
  }

  public boolean makeCryoPositionComfile(final AxisID axisID) {
    if (dialog == null) {
      return false;
    }
    MakecomfileParam param =
      new MakecomfileParam(manager, axisID, FileType.CRYO_POSITION_COMSCRIPT);
    dialog.getParameters(param, true);
    return manager.makecomfile(axisID, param);
  }

  /**
   * Create a whole tomogram for positioning the tomogram in the volume
   * 
   * @param axisID
   */
  public void wholeTomogram(final ProcessResultDisplay processResultDisplay,
    ProcessSeries processSeries) {
    if (processSeries == null) {
      processSeries = new ProcessSeries(manager, axisID, dialogType);
    }
    sendMsgProcessStarting(processResultDisplay);
    // Make sure that we have an active positioning dialog
    if (dialog == null) {
      UIHarness.INSTANCE.openMessageDialog(manager,
        "Can not save comscripts without an active positioning dialog",
        "Program logic error", axisID);
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      processSeries.endSeries();
      return;
    }
    // Get the user input from the dialog
    if (!UIExpertUtilities.INSTANCE.updateFiducialessParams(manager, dialog, axisID,
      true)) {
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      processSeries.endSeries();
      return;
    }
    ConstNewstParam newstParam = null;
    BlendmontParam blendmontParam = null;
    if (metaData.getViewType() != ViewType.MONTAGE) {
      newstParam = updateNewstCom();
      if (newstParam == null) {
        sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
        processSeries.endSeries();
        return;
      }
    }
    else {
      try {
        blendmontParam = updateBlendCom();
      }
      catch (FortranInputSyntaxException e) {
        UIHarness.INSTANCE.openMessageDialog(manager, e.getMessage(), "Update Com Error");
      }
      catch (InvalidParameterException e) {
        UIHarness.INSTANCE.openMessageDialog(manager, e.getMessage(), "Update Com Error");
      }
      catch (IOException e) {
        UIHarness.INSTANCE.openMessageDialog(manager, e.getMessage(), "Update Com Error");
      }
      if (blendmontParam == null) {
        sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
        processSeries.endSeries();
        return;
      }
    }
    if (updateTomoPosTiltCom(true, true) == null) {
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      processSeries.endSeries();
      return;
    }
    setDialogState(ProcessState.INPROGRESS);
    ProcessResult processResult;
    if (metaData.getViewType() != ViewType.MONTAGE) {
      processResult =
        manager.wholeTomogram(axisID, processResultDisplay, processSeries, newstParam);
    }
    else {
      processResult = manager.wholeTomogram(axisID, processResultDisplay, processSeries,
        blendmontParam);
    }
    if (processResult != null) {
      sendMsg(processResult, processResultDisplay);
      return;
    }
    processSeries.setNextProcess(ProcessName.TILT.toString(), null);
    if (dialog.isSampleTypeAuto()) {
      processSeries.setLastProcess(ProcessName.FIND_SECTION.toString());
    }
  }

  public void tomopitch(ProcessResultDisplay processResultDisplay,
    final ProcessSeries processSeries) {
    sendMsgProcessStarting(processResultDisplay);
    if (dialog == null) {
      UIHarness.INSTANCE.openMessageDialog(manager,
        "Can not save comscript without an active positioning dialog",
        "Program logic error", axisID);
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      if (processSeries != null) {
        processSeries.endSeries();
      }
      return;
    }
    if (!updateTomopitchCom(true)) {
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      if (processSeries != null) {
        processSeries.endSeries();
      }
      return;
    }
    setDialogState(ProcessState.INPROGRESS);
    sendMsg(manager.tomopitch(axisID, processResultDisplay, processSeries),
      processResultDisplay);
  }

  /**
   * Open the tomopitch log file
   * 
   * @param axisID
   */
  private void openTomopitchLog() {
    String logFileName = DatasetFiles.getTomopitchLogFileName(manager, axisID);
    TextPageWindow logFileWindow = new TextPageWindow();
    logFileWindow.setVisible(
      logFileWindow.setFile(manager.getPropertyUserDir() + File.separator + logFileName));
  }

  /**
   * If dialog is null, then the tomopitch output will not end up on the tomo
   * pos dialog.  But this is unlikely to happen because tomopitch runs very
   * fast.  The values will have to be save somewhere else if this is a problem.
   * @param axisID
   */
  public void setTomopitchOutput() {
    if (dialog == null) {
      return;
    }
    TomopitchLog log = new TomopitchLog(manager, axisID);
    if (!dialog.setParameters(log)) {
      openTomopitchLog();
    }
  }

  public void finalAlign(ProcessResultDisplay processResultDisplay,
    final ProcessSeries processSeries) {
    sendMsgProcessStarting(processResultDisplay);
    if (dialog == null) {
      UIHarness.INSTANCE.openMessageDialog(manager,
        "Can not save comscript without an active positioning dialog",
        "Program logic error", axisID);
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      if (processSeries != null) {
        processSeries.endSeries();
      }
      return;
    }
    ConstTiltalignParam tiltalignParam = updateAlignCom(true);
    if (tiltalignParam == null) {
      sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      if (processSeries != null) {
        processSeries.endSeries();
      }
      return;
    }
    setDialogState(ProcessState.INPROGRESS);
    sendMsg(
      manager.finalAlign(axisID, processResultDisplay, processSeries, tiltalignParam),
      processResultDisplay);
  }

  /**
   * Whole tomogram
   * @param processResultDisplay
   */
  private void sampleTilt(ProcessResultDisplay processResultDisplay,
    final ProcessSeries processSeries) {
    comScriptMgr.loadTilt(axisID);
    TiltParam tiltParam = comScriptMgr.getTiltParam(axisID);
    tiltParam.setCommandMode(TiltParam.Mode.WHOLE);
    tiltParam.setFiducialess(metaData.isFiducialess(axisID));
    sendMsg(manager.sampleTilt(axisID, processResultDisplay, processSeries, tiltParam),
      processResultDisplay);
  }

  private void findSection(ProcessResultDisplay processResultDisplay,
    final ProcessSeries processSeries) {
    sendMsg(
      manager.findSection(axisID, processResultDisplay, processSeries,
        new FindSectionParam(manager, axisID, dialog.isWholeTomogram())),
      processResultDisplay);
  }

  private ConstTiltalignParam updateAlignCom(final boolean doValidation) {
    if (dialog == null) {
      return null;
    }
    TiltalignParam tiltalignParam;
    try {
      tiltalignParam = comScriptMgr.getTiltalignParam(axisID);
      if (!dialog.getAlignParams(tiltalignParam, metaData, doValidation)) {
        return null;
      }
      rollAlignComAngles();
      comScriptMgr.saveAlign(tiltalignParam, axisID);
      // update xfproduct in align.com
      XfproductParam xfproductParam = comScriptMgr.getXfproductInAlign(axisID);
      xfproductParam.setScaleShifts(UIExpertUtilities.INSTANCE.getStackBinning(manager,
        axisID, FileType.PREALIGNED_STACK));
      comScriptMgr.saveXfproductInAlign(xfproductParam, axisID);
    }
    catch (NumberFormatException except) {
      String[] errorMessage = new String[3];
      errorMessage[0] = "Tiltalign Parameter Syntax Error";
      errorMessage[1] = "Axis: " + axisID.getExtension();
      errorMessage[2] = except.getMessage();
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "Tiltalign Parameter Syntax Error", axisID);
      return null;
    }
    catch (FortranInputSyntaxException except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Xfproduct Parameter Syntax Error";
      errorMessage[1] = "Axis: " + axisID.getExtension();
      errorMessage[2] = except.getMessage();
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "Xfproduct Parameter Syntax Error", axisID);
      return null;
    }
    return tiltalignParam;
  }

  /**
   * Update the tilt{|a|b}.com file with sample parameters for the specified
   * axis
   */
  private ConstTiltParam updateTomoPosTiltCom(boolean positioning,
    final boolean doValidation) {
    // Make sure that we have an active positioning dialog
    if (dialog == null) {
      UIHarness.INSTANCE.openMessageDialog(manager,
        "Can not update sample.com without an active positioning dialog",
        "Program logic error", axisID);
      return null;
    }
    // Get the current tilt parameters, make any user changes and save the
    // parameters back to the tilt{|a|b}.com
    TiltParam tiltParam = null;
    try {
      tiltParam = comScriptMgr.getTiltParam(axisID);
      tiltParam.setFiducialess(metaData.isFiducialess(axisID));
      if (!dialog.getTiltParams(tiltParam, metaData, doValidation)) {
        return null;
      }
      // if not postioning then just saving tilt.com to continue, so want the
      // final thickness instead of the sample thickness.
      if (positioning) {
        if (!dialog.getTiltParamsForSample(tiltParam, doValidation)) {
          return null;
        }
      }
      // get the command mode right
      if (!dialog.isWholeTomogram()) {
        tiltParam.setCommandMode(TiltParam.Mode.SAMPLE);
      }
      else {
        tiltParam.setCommandMode(TiltParam.Mode.WHOLE);
      }
      dialog.getParameters(metaData);
      String outputFileName;
      outputFileName = FileType.TILT_OUTPUT.getFileName(manager, axisID);
      // outputFileName = metaData.getDatasetName() + "_full.rec";
      // outputFileName = metaData.getDatasetName() + axisID.getExtension() + ".rec";
      tiltParam.setOutputFile(outputFileName);
      if (metaData.getViewType() == ViewType.MONTAGE) {
        // binning is currently always 1 and correct size should be coming from
        // copytomocoms
        // tiltParam.setMontageFullImage(propertyUserDir,
        // tomogramPositioningDialog.getBinning());
      }
      rollTiltComAngles();
      manager.updateExcludeList(tiltParam, axisID);
      comScriptMgr.saveTilt(tiltParam, axisID);
      metaData.setFiducialess(axisID, tiltParam.isFiducialess());
    }
    catch (

    NumberFormatException except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Tilt Parameter Syntax Error";
      errorMessage[1] = "Axis: " + axisID.getExtension();
      errorMessage[2] = except.getMessage();
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "Tilt Parameter Syntax Error", axisID);
      return null;
    }
    return tiltParam;
  }

  /**
   * Update the tomopitch{|a|b}.com file with sample parameters for the
   * specified axis
   */
  private boolean updateTomopitchCom(final boolean doValidation) {
    // Make sure that we have an active positioning dialog
    if (dialog == null) {
      UIHarness.INSTANCE.openMessageDialog(manager,
        "Can not update tomopitch.com without an active positioning dialog",
        "Program logic error", axisID);
      return false;
    }
    // Get the current tilt parameters, make any user changes and save the
    // parameters back to the tilt{|a|b}.com
    try {
      TomopitchParam tomopitchParam = comScriptMgr.getTomopitchParam(axisID);
      if (!dialog.getTomopitchParam(tomopitchParam, metaData, doValidation)) {
        return false;
      }
      comScriptMgr.saveTomopitch(tomopitchParam, axisID);
    }
    catch (NumberFormatException except) {
      String[] errorMessage = new String[3];
      errorMessage[0] = "Tomopitch Parameter Syntax Error";
      errorMessage[1] = "Axis: " + axisID.getExtension();
      errorMessage[2] = except.getMessage();
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "Tomopitch Parameter Syntax Error", axisID);
      return false;
    }
    return true;
  }

  /**
   * Update the newst{|a|b}.com scripts with the parameters from the tomogram
   * positioning dialog. The positioning dialog is passed so that the
   * signature is different from the standard method.
   * 
   * @param tomogramPositioningDialog
   * @param axisID
   * @return
   */
  private ConstNewstParam updateNewstCom() {
    if (dialog == null) {
      return null;
    }
    // Get the whole tomogram positions state
    metaData.setWholeTomogramSample(axisID, dialog.isWholeTomogram());
    NewstParam newstParam = comScriptMgr.getNewstComNewstParam(axisID);
    getNewstParam(newstParam);
    comScriptMgr.saveNewst(newstParam, axisID);
    return newstParam;
  }

  /**
   * Update the blend{|a|b}.com scripts with the parameters from the tomogram
   * positioning dialog. The positioning dialog is passed so that the
   * signature is different from the standard method.
   * 
   * @param tomogramPositioningDialog
   * @param axisID
   * @return
   */
  private BlendmontParam updateBlendCom()
    throws FortranInputSyntaxException, InvalidParameterException, IOException {
    if (dialog == null) {
      return null;
    }
    // Get the whole tomogram positions state
    metaData.setWholeTomogramSample(axisID, dialog.isWholeTomogram());
    BlendmontParam blendmontParam = comScriptMgr.getBlendParam(axisID);
    getParameters(blendmontParam);
    comScriptMgr.saveBlend(blendmontParam, axisID);
    return blendmontParam;
  }

  private CryoPositionParam updateCryoPositionCom(final boolean forRun,
    final ProcessResultDisplay processResultDisplay) {
    if (dialog == null) {
      return null;
    }
    if (!comScriptMgr.loadCryoPosition(axisID, false)) {
      if (!makeCryoPositionComfile(axisID) && forRun) {
        sendMsg(ProcessResult.FAILED_TO_START, processResultDisplay);
      }
      comScriptMgr.loadCryoPosition(axisID, true);
    }
    CryoPositionParam param = comScriptMgr.getCryoPositionParam(axisID, axisType);
    if (!dialog.getParameters(param, forRun)) {
      return null;
    }
    comScriptMgr.saveCryoPosition(param, axisID);
    return param;
  }

  void rollAlignComAngles() {
    if (dialog != null) {
      dialog.rollAlignComAngles();
    }
  }

  void rollTiltComAngles() {
    if (dialog != null) {
      dialog.rollTiltComAngles();
    }
  }

  /**
   * Get the newst.com parameters from the dialog
   * @param newstParam
   */
  private void getNewstParam(NewstParam newstParam) {
    int binning = 1;
    if (dialog != null) {
      binning = dialog.getBinning();
    }
    // Only whole tomogram can change binning
    // Only explcitly write out the binning if its value is something other than
    // the default of 1 to keep from cluttering up the com script
    if (binning == 1) {
      newstParam.setBinByFactor(Integer.MIN_VALUE);
    }
    else {
      newstParam.setBinByFactor(binning);
    }
    if (dialog != null) {
      dialog.updateMetaData(metaData);
    }
    try {
      // Make sure the size output is removed, it was only there as a
      // copytomocoms template
      newstParam.setCommandMode(NewstParam.Mode.WHOLE_TOMOGRAM_SAMPLE);
      try {
        newstParam.setSizeToOutputInXandY("", dialog.getBinning(),
          metaData.getImageRotation(axisID).getDouble(), null);
      }
      catch (InvalidParameterException e) {
        e.printStackTrace();
        UIHarness.INSTANCE.openMessageDialog(manager,
          "Unable to update newst com: " + e.getMessage(), "Etomo Error", axisID);
      }
      catch (IOException e) {
        e.printStackTrace();
        UIHarness.INSTANCE.openMessageDialog(manager,
          "Unable to update newst com: " + e.getMessage(), "Etomo Error", axisID);
      }
    }
    catch (FortranInputSyntaxException e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the newst.com parameters from the dialog
   * @param newstParam
   */
  private void getParameters(BlendmontParam blendmontParam)
    throws FortranInputSyntaxException, InvalidParameterException, IOException {
    int binning = 1;
    if (dialog != null) {
      binning = dialog.getBinning();
    }
    blendmontParam.setBinByFactor(binning);
    if (dialog != null) {
      dialog.updateMetaData(metaData);
    }
    blendmontParam.setMode(BlendmontParam.Mode.WHOLE_TOMOGRAM_SAMPLE);
    blendmontParam.setBlendmontState(state.getInvalidEdgeFunctions(axisID));
    blendmontParam.resetStartingAndEndingXandY();
    blendmontParam.convertToStartingAndEndingXandY("",
      metaData.getImageRotation(axisID).getDouble(), null);
  }

  ProcessDialog getDialog() {
    return dialog;
  }

  public static final class Task implements TaskInterface {
    private static final Task POST_CRYO_POSITION = new Task();

    private final boolean droppable;

    private Task() {
      this(false);
    }

    private Task(final boolean droppable) {
      this.droppable = droppable;
    }

    public boolean okToDrop() {
      return droppable;
    }
  }
}
/**
 * <p> $Log$
 * <p> Revision 1.5  2011/03/02 00:00:12  sueh
 * <p> bug# 1452 Removing image rotation conversion between float and
 * <p> double.  Using string where possible.
 * <p>
 * <p> Revision 1.4  2011/02/22 21:40:51  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.3  2011/02/03 06:22:16  sueh
 * <p> bug# 1422 Control of the processing method has been centralized in the
 * <p> processing method mediator class.  Implementing ProcessInterface.
 * <p> Supplying processes with the current processing method.
 * <p>
 * <p> Revision 1.2  2010/12/05 05:24:00  sueh
 * <p> bug# 1420 Moved ProcessResultDisplayFactory to etomo.ui.swing package.  Removed static button construction functions.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.41  2010/04/28 16:48:25  sueh
 * <p> bug# 1344 In startNextProcess changed the process parameter into a
 * <p> ProcessSeries.Process.
 * <p>
 * <p> Revision 1.40  2010/04/08 04:36:47  sueh
 * <p> bug# 1348 Sending exception stack to log when tilt parameter error
 * <p> happens.
 * <p>
 * <p> Revision 1.39  2010/03/30 00:07:30  sueh
 * <p> bug# 1331 Added useGpu checkbox.  Added boolean initialize parameter
 * <p> to setTiltParam(ConstTiltParam).
 * <p>
 * <p> Revision 1.38  2010/03/12 04:28:07  sueh
 * <p> bug# 1325 Fixed typo in doneDialog.
 * <p>
 * <p> Revision 1.37  2010/03/05 22:33:50  sueh
 * <p> bug# 1313 In saveDialog looking at TomogramState.sampleFiducialess to
 * <p> prevent align.com from being updated.
 * <p>
 * <p> Revision 1.36  2010/02/18 17:14:57  sueh
 * <p> bug# 1313 In saveDialog only call updateAlignCom when
 * <p> metaData.isFiducialess is false.
 * <p>
 * <p> Revision 1.35  2010/02/17 05:03:12  sueh
 * <p> bug# 1301 Using manager instead of manager key for popping up messages.
 * <p>
 * <p> Revision 1.34  2009/09/17 19:12:58  sueh
 * <p> bug# 1257 In NewstParam.setSizeToOutputInXandY forgot to read the
 * <p> header.  Adding read call and throwing InvalidParameterException and
 * <p> IOException.
 * <p>
 * <p> Revision 1.33  2009/09/01 03:18:24  sueh
 * <p> bug# 1222
 * <p>
 * <p> Revision 1.32  2009/03/17 00:46:23  sueh
 * <p> bug# 1186 Pass managerKey to everything that pops up a dialog.
 * <p>
 * <p> Revision 1.31  2009/01/20 20:32:19  sueh
 * <p> bug# 1102 Removed unnecessary print.
 * <p>
 * <p> Revision 1.30  2008/12/15 23:04:51  sueh
 * <p> bug# 1161 Calling NewstParam.setSizeToOutputInXandY and
 * <p> BlendmontParam.convertToStartingAndEndXandY; calling them with an
 * <p> empty string in order to handle 90 degree image rotation.
 * <p>
 * <p> Revision 1.29  2008/11/20 01:49:03  sueh
 * <p> Bug# 1147  Getting binning only from meta data, not from newst, which
 * <p> is overwritten by Tomogram Positioning and Final Aligned Stack.
 * <p>
 * <p> Revision 1.28  2008/05/28 02:51:55  sueh
 * <p> bug# 1111 Add a dialogType parameter to the ProcessSeries
 * <p> constructor.  DialogType must be passed to any function that constructs
 * <p> a ProcessSeries instance.
 * <p>
 * <p> Revision 1.27  2008/05/13 23:08:54  sueh
 * <p> bug# 847 Adding a right click menu for deferred 3dmods to some
 * <p> process buttons.  Handling doneDialog without setting dialog = null, since
 * <p> a later process may want to use it.
 * <p>
 * <p> Revision 1.26  2008/05/07 02:45:46  sueh
 * <p> bug# 847 Getting the the postioning buttons from the expert.
 * <p>
 * <p> Revision 1.25  2008/05/07 00:27:55  sueh
 * <p> bug# 847 Putting a shared label into the same string.
 * <p>
 * <p> Revision 1.24  2008/05/03 00:57:48  sueh
 * <p> bug# 847 Passing null for ProcessSeries to process funtions.
 * <p>
 * <p> Revision 1.23  2008/01/30 21:35:12  sueh
 * <p> bug# 1074 In updateTomPosTiltCom changed the parameter name to positioning
 * <p> to distinguish between saving tilt.com when saving the dialog, which uses the final thickness; and saving tilt.com to do positioning, which uses the sample thickness.
 * <p>
 * <p> Revision 1.22  2008/01/29 01:40:24  sueh
 * <p> bug# 1073 Removed updateTiltCom, which was only being called by fiducialessAction, and put the functionality into the caller.  In fiducialesssAction, if the user checked Fiducialess, set the dialog values tiltAngleOffset and ZShift from tilt.com (not the other way around).  Keep the functionality as is for an unchecked Fiducialess.  The values in tilt.com are valid for creating a fiducialess sample.  If the tiltAngleOffset and ZShift values on the screen came from creating a sample with fiducials then they are not valid values to save in tilt.com
 * <p>
 * <p> Revision 1.21  2008/01/28 23:45:16  sueh
 * <p> bug# 1071 Setting commandMode to WHOLE in sampleTilt, which is only used
 * <p> for whole tomogram samples.  When using wholeTomogram to create a whole
 * <p> tomogram sample, call updateTomoPosTiltCom with sample == true.  Otherwise
 * <p> it sets the command mode to SAMPLE.
 * <p>
 * <p> Revision 1.20  2007/12/13 01:14:23  sueh
 * <p> bug# 1056 Setting the Mode in TiltParam.
 * <p>
 * <p> Revision 1.19  2007/08/16 16:37:52  sueh
 * <p> bug# 1035 Resetting sizeToOutputInXandY in newst.  Resetting
 * <p> startingAndEndingX and Y in blend.  Resetting SUBSETSTART to 0 0 in tilt.
 * <p>
 * <p> Revision 1.18  2007/02/09 00:54:22  sueh
 * <p> bug# 962 Made TooltipFormatter a singleton and moved its use to low-level ui
 * <p> classes.
 * <p>
 * <p> Revision 1.17  2007/02/05 23:46:14  sueh
 * <p> bug# 962 Moved comscript mode info to inner class.
 * <p>
 * <p> Revision 1.16  2006/11/15 21:36:00  sueh
 * <p> bug# 872 Changed saveIntermediateParamFile to saveStorables.
 * <p>
 * <p> Revision 1.15  2006/10/25 22:12:54  sueh
 * <p> bug# 952  fiducialessAction():  when unchecking fidless, update tilt.com from the
 * <p> screen version of z shift and tilt angle offset.
 * <p>
 * <p> Revision 1.14  2006/10/19 22:53:28  sueh
 * <p> bug# 941  Refit on fiducialess action
 * <p>
 * <p> Revision 1.13  2006/10/19 22:01:16  sueh
 * <p> bug# 942  updateFiducialessDisplay():  checking for dialog == null
 * <p>
 * <p> Revision 1.12  2006/09/19 22:38:35  sueh
 * <p> bug# 920 Refreshing meta data values in TiltParam each time tilt.com is loaded.
 * <p>
 * <p> Revision 1.11  2006/09/14 00:04:48  sueh
 * <p> bug# 920 Turn off tomopitch fields when the fiducialess setting with which
 * <p> sample was created does not match the fiducialess setting on the screen.
 * <p>
 * <p> Revision 1.10  2006/07/28 20:14:56  sueh
 * <p> bug# 868 checking for null dialog
 * <p>
 * <p> Revision 1.9  2006/07/26 16:43:08  sueh
 * <p> bug# 868  Created a base class for tomogram reconstruction experts called
 * <p> ReconUIExpert.  Moved responsibility for changing the process result display
 * <p> to the expert class.
 * <p>
 * <p> Revision 1.8  2006/07/18 18:02:22  sueh
 * <p> bug# 906 Don't complain about alignment not being done if the button is disabled.
 * <p>
 * <p> Revision 1.7  2006/07/04 20:42:51  sueh
 * <p> bug# 898 Return false from done and save functions if their dialog continues
 * <p> to be displayed.
 * <p>
 * <p> Revision 1.6  2006/07/04 18:04:49  sueh
 * <p> bug# 896 doneDialog():  return false if the user decides not to exit the dialog.
 * <p>
 * <p> Revision 1.5  2006/06/30 20:21:48  sueh
 * <p> bug# 884 Adding timestamps for constructing dialog objects.
 * <p>
 * <p> Revision 1.4  2006/06/30 20:04:44  sueh
 * <p> bug# 877 Calling all the done dialog functions from the dialog done() functions,
 * <p> which is called by the button action functions and saveAction() in
 * <p> ProcessDialog.  Removed the button action function overides.  Set displayed to
 * <p> false after the done dialog function is called.  Added saveAction().
 * <p>
 * <p> Revision 1.3  2006/06/19 17:07:43  sueh
 * <p> bug# 851 saveDialog():  calling manager.closeImod to close the sample 3dmod
 * <p> instead of doing it in this class.
 * <p>
 * <p> Revision 1.2  2006/06/09 19:52:23  sueh
 * <p> bug# 870 Added ways for ApplicationManager to force an exit state:
 * <p> doneDialog(DialogExitState) and saveDialog(DialogExitState).
 * <p>
 * <p> Revision 1.1  2006/05/19 19:52:01  sueh
 * <p> bug# 866 Class to contain all the functionality details associated with
 * <p> tomogram positioning.
 * <p> </p>
 */
