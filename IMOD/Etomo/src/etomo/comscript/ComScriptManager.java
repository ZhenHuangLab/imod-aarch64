package etomo.comscript;

import etomo.ApplicationManager;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.EtomoNumber;
import etomo.type.FileType;
import etomo.type.ProcessName;
import etomo.ui.swing.UIHarness;

/**
 * <p>Description: This class provides a high level manager for loading and
 * saving particlar com scripts and extracting the parameter sets for the 
 * commands within those scripts.</p>
 *
 * <p>Copyright: Copyright 2002 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 3.67  2011/04/09 06:22:51  sueh
 * <p> bug# 1416 Need to pass the manager to most FileType functions so that TILT_OUTPUT can distinguish
 * <p> between single and dual axis type.
 * <p>
 * <p> Revision 3.66  2011/04/04 16:46:35  sueh
 * <p> bug# 1416 Added scriptSirtsetupA and B, getSirtsetupParam, loadSirtsetup, saveSirtsetup.
 * <p>
 * <p> Revision 3.65  2011/02/21 21:40:18  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 3.64  2010/11/13 16:03:15  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 3.63  2010/04/28 15:49:07  sueh
 * <p> bug# 1344 Reformatted.
 * <p>
 * <p> Revision 3.62  2010/03/12 03:58:35  sueh
 * <p> bug# 1325 Added CommandMode to CCDEraserParam constructor.
 * <p>
 * <p> Revision 3.61  2010/03/03 04:51:10  sueh
 * <p> bug# 1311 Added scriptXcorrPtA and B.  Added
 * <p> getTiltxcorrParamFromXcorrPt, loadXCorrPt, and saveXcorrPt.
 * <p>
 * <p> Revision 3.60  2010/02/17 04:47:53  sueh
 * <p> bug# 1301 Using the manager instead of the manager key do pop up
 * <p> messages.
 * <p>
 * <p> Revision 3.59  2009/10/13 17:38:21  sueh
 * <p> bug# 1273 In loadCtfPlotter and loadCtfCorrection, required was passed
 * <p> as the wrong parameter to loadComScript.
 * <p>
 * <p> Revision 3.58  2009/09/22 20:58:18  sueh
 * <p> bug# 1259 In order to process nonstandard tilt.com, added
 * <p> caseInsensitive and separateWithASpace.
 * <p>
 * <p> Revision 3.57  2009/09/21 17:44:04  sueh
 * <p> bug# 1267 Corrected the blendmont mode in getBlendParamFromBlend3dFind.
 * <p>
 * <p> Revision 3.56  2009/09/01 03:17:46  sueh
 * <p> bug# 1222
 * <p>
 * <p> Revision 3.55  2009/06/05 01:44:55  sueh
 * <p> bug# 1219 Added scriptFlatten, getWarpVolParamFromFlatten,
 * <p> isWarpVolParamInFlatten, loadFlatten, and saveFlatten.
 * <p>
 * <p> Revision 3.54  2009/03/17 00:31:16  sueh
 * <p> bug# 1186 Pass managerKey to everything that pops up a dialog.
 * <p>
 * <p> Revision 3.53  2008/10/27 17:43:58  sueh
 * <p> bug# 1141 Added ctfCorrection and ctfPlotter scripts.
 * <p>
 * <p> Revision 3.52  2007/12/10 21:54:28  sueh
 * <p> bug# 1041 Formatted.
 * <p>
 * <p> Revision 3.51  2007/09/07 00:17:34  sueh
 * <p> bug# 989 Using a public INSTANCE to refer to the EtomoDirector singleton
 * <p> instead of getInstance and createInstance.
 * <p>
 * <p> Revision 3.50  2007/03/07 20:58:48  sueh
 * <p> bug# 981 Passing manager to TiltalignParam().
 * <p>
 * <p> Revision 3.49  2007/02/05 21:36:13  sueh
 * <p> bug# 962 Put comscript mode info into an inner class.
 * <p>
 * <p> Revision 3.48  2006/10/24 21:16:48  sueh
 * <p> bug# 947 Changed ProcessName.fromString() to getInstance().
 * <p>
 * <p> Revision 3.47  2006/10/13 22:22:39  sueh
 * <p> bug# 927 Added parameters String and int to getSetParamFromVolcombine().
 * <p> Added getSetParamFromVolcombine(String,int,String) to get the second set
 * <p> command in volcombine.  Added the ability to initialize an optional command
 * <p> using the previous command.  Added a new modifyCommand(), which can
 * <p> modify a command based on a previous command.
 * <p>
 * <p> Revision 3.46  2006/09/19 21:55:05  sueh
 * <p> bug# 920 Added a global key for saving param values in data files.
 * <p>
 * <p> Revision 3.45  2006/09/05 17:33:45  sueh
 * <p> bug# 917 Added scriptMatchvol1, getMatchvolParam(), loadMatchvol1(), and
 * <p> saveMatchvol().
 * <p>
 * <p> Revision 3.44  2006/08/25 22:42:30  sueh
 * <p> bug# 918 Changed updateComScript functions to modifyCommand and addModifyCommand to reduce the number of boolean parameters.  Added
 * <p> deleteCommand and modifyOptionalCommand.  Deleting the old patchcrawl
 * <p> script and added the new one.
 * <p>
 * <p> Revision 3.43  2006/08/14 22:22:14  sueh
 * <p> Improved the pop up error messages.
 * <p>
 * <p> Revision 3.42  2006/06/05 16:05:12  sueh
 * <p> bug# 766 In ProcessName:  Changed getCommand() and getCommandArray() to
 * <p> getComscript... because the fuctions are specialized for comscripts.
 * <p>
 * <p> Revision 3.41  2006/05/16 21:21:41  sueh
 * <p> bug# 856 Passing the BaseManager to SolvematchParam so that it can get the
 * <p> fiducial model.
 * <p>
 * <p> Revision 3.40  2006/01/26 21:47:52  sueh
 * <p> Added spaces to an error message.
 * <p>
 * <p> Revision 3.39  2005/09/01 17:45:45  sueh
 * <p> bug# 688 putting temporary prints (for finding cause of undistort
 * <p> parameters being set in xcorr) into the error log
 * <p>
 * <p> Revision 3.38  2005/08/27 22:17:01  sueh
 * <p> bug# 532 changed Utilities.timestamp to take a string status instead of
 * <p> integer
 * <p>
 * <p> Revision 3.37  2005/08/25 01:45:18  sueh
 * <p> removed print statement
 * <p>
 * <p> Revision 3.36  2005/08/24 22:32:12  sueh
 * <p> bug# 715 Added loadComScript(ProcessName...) to work with
 * <p> BlendmontParam, which uses getProcessName() instead of
 * <p> getCommandFileName().
 * <p>
 * <p> Revision 3.35  2005/07/29 00:43:19  sueh
 * <p> bug# 709 Going to EtomoDirector to get the current manager is unreliable
 * <p> because the current manager changes when the user changes the tab.
 * <p> Passing the manager where its needed.
 * <p>
 * <p> Revision 3.34  2005/06/17 20:03:06  sueh
 * <p> bug# 685 Added timestamp functions for ComScript and File types.
 * <p> Added code to the main timestamp function to strip the path from a file
 * <p> name.  These changes reduces the amount of timestamp related code
 * <p> being executed when debug is off.
 * <p>
 * <p> Revision 3.33  2005/06/17 00:32:14  sueh
 * <p> bug# 685 Added timestamp to deleteCommand(), initialize(),
 * <p> loadComScript(), updateComScript(), and useTemplate().
 * <p>
 * <p> Revision 3.32  2005/05/17 19:14:57  sueh
 * <p> bug# 658 Passing axisID to BeadtrackParam constructor.
 * <p>
 * <p> Revision 3.31  2005/04/26 17:36:13  sueh
 * <p> bug# 615 Change the name of the UIHarness member variable to
 * <p> uiHarness.
 * <p>
 * <p> Revision 3.30  2005/04/25 20:37:49  sueh
 * <p> bug# 615 Passing the axis where the command originated to the message
 * <p> functions so that the message will be popped up in the correct window.
 * <p> This requires adding AxisID to many objects.  Move the interface for
 * <p> popping up message dialogs to UIHarness.  It prevents headless
 * <p> exceptions during a test execution.  It also allows logging of dialog
 * <p> messages during a test.  It also centralizes the dialog interface and
 * <p> allows the dialog functions to be synchronized to prevent dialogs popping
 * <p> up in both windows at once.  All Frame functions will use UIHarness as a
 * <p> public interface.
 * <p>
 * <p> Revision 3.29  2005/04/13 20:32:13  sueh
 * <p> bug# 633 put updateComScript call back into savePrenewst().
 * <p>
 * <p> Revision 3.28  2005/04/07 21:50:48  sueh
 * <p> bug# 626 Added undistort script.  This script is contains a blendmont
 * <p> command which is loaded from xcorr, updated, and then written into
 * <p> undistort.com.
 * <p>
 * <p> Revision 3.27  2005/03/11 01:33:03  sueh
 * <p> bug# 533 printing a stack trace for an error in updateComScript.
 * <p>
 * <p> Revision 3.26  2005/03/09 18:00:35  sueh
 * <p> bug# 533 Added the blend script.
 * <p>
 * <p> Revision 3.25  2005/03/08 01:53:37  sueh
 * <p> bug# 533 Added preblend script.
 * <p>
 * <p> Revision 3.24  2005/03/04 00:08:37  sueh
 * <p> bug# 533 Added getBlendmontParamFromTiltxcorr(),
 * <p> getGotoParamFromTiltxcorr(), saveXcorr(BlendmontParam),
 * <p> saveXcorr(GotoParam).
 * <p>
 * <p> Revision 3.23  2005/02/23 18:48:09  sueh
 * <p> bug# 607 Fix problem: Etomo unable to exit because of null pointer
 * <p> exception.
 * <p>
 * <p> Revision 3.22  2005/01/29 00:17:58  sueh
 * <p> Checking for null values in initialize() to prevent null value exception on
 * <p> exit.
 * <p>
 * <p> Revision 3.21  2005/01/08 01:28:20  sueh
 * <p> bug# 578 Passing axisID to NewstParam constructor.  Passing dataset
 * <p> name and axisID to TiltParam constructor.
 * <p>
 * <p> Revision 3.20  2005/01/06 17:55:40  sueh
 * <p> bug# 567 Passing dataset name to TiltalignParam constructor to build the
 * <p> zFactorFile name without making TiltalignParam depend on one type of
 * <p> manager.
 * <p>
 * <p> Revision 3.19  2005/01/05 18:54:51  sueh
 * <p> bug# 578 Create tiltalignParam with axisID.
 * <p>
 * <p> Revision 3.18  2004/12/03 20:19:45  sueh
 * <p> bug# 556 SetupParam may be missing in volcombine.com in older .com
 * <p> scripts.
 * <p>
 * <p> Revision 3.17  2004/11/30 00:33:11  sueh
 * <p> bug# 556 Adding functions to parse volcombine.com.
 * <p>
 * <p> Revision 3.16  2004/11/19 22:42:19  sueh
 * <p> bug# 520 merging Etomo_3-4-6_JOIN branch to head.
 * <p>
 * <p> Revision 3.15.2.3  2004/10/11 02:00:21  sueh
 * <p> bug# 520 Using a variable called propertyUserDir instead of the "user.dir"
 * <p> property.  This property would need a different value for each manager.
 * <p>
 * <p> Revision 3.15.2.2  2004/10/08 15:45:42  sueh
 * <p> bug# 520 Since EtomoDirector is a singleton, made all functions and
 * <p> member variables non-static.
 * <p>
 * <p> Revision 3.15.2.1  2004/09/15 22:34:51  sueh
 * <p> bug# 520 call openMessageDialog in mainPanel instead of mainFrame
 * <p>
 * <p> Revision 3.15  2004/08/19 01:25:44  sueh
 * <p> Added functions to get a CombineComscriptState.  Added ComScript
 * <p> combine.  Added functions for echo, exit, and goto in the combine
 * <p> script.  Added new general functions to add a command to a script
 * <p> based on the location of the previous command or command index.
 * <p> Added a general function to delete a command based on the location of
 * <p> the previous command.  Added general initialization functions for
 * <p> optional command and for commands that must be located by
 * <p> specifying the previous command.  Added a second useTemplate
 * <p> command for the simpler case where the .com file did not change.
 * <p> Added:
 * <p> ComScript scriptCombine
 * <p> deleteCommand(ComScript script, String command, AxisID axisID,
 * <p>     String previousCommand)
 * <p> deleteFromCombine(String command, String previousCommand)
 * <p> getCombineComscript()
 * <p> getEchoParamFromCombine(String previousCommand)
 * <p> getGotoParamFromCombine()
 * <p> initialize(CommandParam param, ComScript comScript,
 * <p>     String command, AxisID axisID, boolean optionalCommand)
 * <p> initialize(CommandParam param, ComScript comScript,
 * <p>     String command, AxisID axisID, String previousCommand)
 * <p> loadCombine()
 * <p> saveCombine(EchoParam echoParam, String previousCommand)
 * <p> saveCombine(ExitParam exitParam, int previousCommandIndex)
 * <p> saveCombine(GotoParam gotoParam, int previousCommandIndex)
 * <p> saveCombine(GotoParam gotoParam)
 * <p> updateComScript(ComScript script, CommandParam params,
 * <p>     String command, AxisID axisID, boolean addNew,
 * <p>    int previousCommandIndex)
 * <p> updateComScript(ComScript script, CommandParam params,
 * <p>     String command, AxisID axisID, boolean addNew,
 * <p>     String previousCommand)
 * <p> useTemplate(String scriptName, AxisType axisType, AxisID axisID,
 * <p>     boolean rename)
 * <p> Changed:
 * <p> initialize(CommandParam param, ComScript comScript,
 * <p>     String command, AxisID axisID)
 * <p>
 * <p> Revision 3.14  2004/06/24 18:36:20  sueh
 * <p> bug# 482 Removing proof-of-concept test code.  Add functions
 * <p> to retrieve matchshifts from solvematchshift and add it to
 * <p> solvematch.
 * <p>
 * <p> Revision 3.13  2004/06/14 23:39:53  rickg
 * <p> Bug #383 Transitioned to using solvematch
 * <p>
 * <p> Revision 3.12  2004/06/13 17:03:23  rickg
 * <p> Solvematch mid change
 * <p>
 * <p> Revision 3.11  2004/05/05 19:15:05  sueh
 * <p> param test - proof of concept
 * <p>
 * <p> Revision 3.10  2004/05/03 17:59:36  sueh
 * <p> param testing proof of concept
 * <p>
 * <p> Revision 3.9  2004/04/27 00:50:16  sueh
 * <p> bug# 427 parse comments for tomopitch
 * <p>
 * <p> Revision 3.8  2004/04/26 21:09:50  sueh
 * <p> bug# 427 added tomopitch
 * <p>
 * <p> Revision 3.7  2004/04/19 19:24:46  sueh
 * <p> bug# 409 putting text back to pre-409, handling changes in
 * <p> ComScript
 * <p>
 * <p> Revision 3.6  2004/04/16 01:45:25  sueh
 * <p> bug# 409 changes for mtffilter where not working for newst - fixed
 * <p>
 * <p> Revision 3.5  2004/04/12 17:11:25  sueh
 * <p> bug# 409  In initialize() allow the param to initialize itself if necessary.  In update
 * <p> ComScript, get the commandIndex after running
 * <p> script.getScriptCommand(command) to make sure that the command exists in
 * <p> the ComScript object.
 * <p>
 * <p> Revision 3.4  2004/03/29 20:46:57  sueh
 * <p> bug# 409 add MTF Filter
 * <p>
 * <p> Revision 3.3  2004/03/13 00:30:49  rickg
 * <p> Bug# 390 Add prenewst and xfproduct management
 * <p>
 * <p> Revision 3.2  2004/03/12 00:04:10  rickg
 * <p> Bug #410 Newstack PIP transition
 * <p> Handle newst or newstack commands the same way
 * <p>
 * <p> Revision 3.1  2004/03/04 00:46:54  rickg
 * <p> Bug# 406 Correctly write out command when it isn't the first in the
 * <p> script
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:00  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.9  2003/07/25 22:57:30  rickg
 * <p> CommandParam method name changes
 * <p>
 * <p> Revision 2.8  2003/06/25 22:16:29  rickg
 * <p> changed name of com script parse method to parseComScript
 * <p>
 * <p> Revision 2.7  2003/06/23 23:28:32  rickg
 * <p> Return exception class name in error dialog
 * <p>
 * <p> Revision 2.6  2003/05/07 22:32:42  rickg
 * <p> System property user.dir now defines the working directory
 * <p>
 * <p> Revision 2.5  2003/03/27 00:27:49  rickg
 * <p> Fixed but in loading tilt with with respect to parsing comments.
 * <p>
 * <p> Revision 2.4  2003/03/07 07:22:49  rickg
 * <p> combine layout in progress
 * <p>
 * <p> Revision 2.3  2003/03/06 05:53:28  rickg
 * <p> Combine interface in progress
 * <p>
 * <p> Revision 2.2  2003/03/06 01:19:17  rickg
 * <p> Combine changes in progress
 * <p>
 * <p> Revision 2.1  2003/03/02 23:30:41  rickg
 * <p> Combine layout in progress
 * <p>
 * <p> Revision 2.0  2003/01/24 20:30:31  rickg
 * <p> Single window merge to main branch
 * <p>
 * <p> Revision 1.2  2002/10/09 00:00:34  rickg
 * <p> Fixed formatting due to emacs
 * <p>
 * <p> Revision 1.1  2002/09/09 22:57:02  rickg
 * <p> Initial CVS entry, basic functionality not including combining
 * <p> </p>
 */
public final class ComScriptManager {
  static final String PARAM_KEY = "Param";

  private final ApplicationManager appManager;
  UIHarness uiHarness = UIHarness.INSTANCE;

  static final String MTFFILTER_COMMAND = "mtffilter";

  private ComScript scriptEraserA = null;
  private ComScript scriptEraserB = null;
  private ComScript scriptXcorrA = null;
  private ComScript scriptXcorrB = null;
  private ComScript scriptPrenewstA = null;
  private ComScript scriptPrenewstB = null;
  private ComScript scriptTrackA = null;
  private ComScript scriptTrackB = null;
  private ComScript scriptAlignA = null;
  private ComScript scriptAlignB = null;
  private ComScript scriptNewstA = null;
  private ComScript scriptNewstB;
  private ComScript scriptTiltA = null;
  private ComScript scriptTiltB = null;
  private ComScript scriptMTFFilterA = null;
  private ComScript scriptMTFFilterB = null;
  private ComScript scriptPreblendA = null;
  private ComScript scriptPreblendB = null;
  private ComScript scriptBlendA = null;
  private ComScript scriptBlendB = null;
  private ComScript scriptUndistortA = null;
  private ComScript scriptUndistortB = null;
  private ComScript scriptMatchvol1 = null;
  // The solvematch com script replaces the functionality of the
  // solvematchshift and solvematchmod com scripts
  private ComScript scriptSolvematch = null;
  private ComScript scriptDualvolmatch = null;
  private ComScript scriptSolvematchshift = null;
  private ComScript scriptSolvematchmod = null;
  private ComScript scriptPatchcorr = null;
  private ComScript scriptMatchorwarp = null;
  private ComScript scriptTomopitchA = null;
  private ComScript scriptTomopitchB = null;
  private ComScript scriptCombine = null;
  private ComScript scriptVolcombine = null;
  private ComScript scriptCtfCorrectionA = null;
  private ComScript scriptCtfCorrectionB = null;
  private ComScript scriptCtfPlotterA = null;
  private ComScript scriptCtfPlotterB = null;
  private ComScript scriptFlatten = null;
  private ComScript scriptNewst3dFindA = null;
  private ComScript scriptNewst3dFindB = null;
  private ComScript scriptBlend3dFindA = null;
  private ComScript scriptBlend3dFindB = null;
  private ComScript scriptTilt3dFindA = null;
  private ComScript scriptTilt3dFindB = null;
  private ComScript scriptFindBeads3dA = null;
  private ComScript scriptFindBeads3dB;
  private ComScript scriptTilt3dFindReprojectA = null;
  private ComScript scriptTilt3dFindReprojectB = null;
  private ComScript scriptXcorrPtA = null;
  private ComScript scriptXcorrPtB = null;
  private ComScript scriptSirtsetupA = null;
  private ComScript scriptSirtsetupB = null;
  private ComScript scriptTiltForSirtA = null;
  private ComScript scriptTiltForSirtB = null;
  private ComScript scriptAutofidseedA = null;
  private ComScript scriptAutofidseedB = null;
  private ComScript scriptGoldEraserA = null;
  private ComScript scriptGoldEraserB = null;
  private ComScript scriptCryoPositionA = null;
  private ComScript scriptCryoPositionB = null;
  private ComScript scriptMultifiltSetupA = null;
  private ComScript scriptMultifiltSetupB = null;

  public ComScriptManager(final ApplicationManager appManager) {
    this.appManager = appManager;
  }

  /**
   * Load the specified eraser com script
   * @param axisID the AxisID to load.
   */
  public void loadEraser(final AxisID axisID) {
    // Assign the new ComScript object object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptEraserB =
        ComScriptUtil.loadComScript(appManager, "eraser", axisID, true, false, false);
    }
    else {
      scriptEraserA =
        ComScriptUtil.loadComScript(appManager, "eraser", axisID, true, false, false);
    }
  }

  /**
   * @param axisID
   * @param required
   * @return true if script has loaded
   */
  public boolean loadGoldEraser(final AxisID axisID, final boolean required) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptGoldEraserB = ComScriptUtil.loadComScript(appManager,
        FileType.GOLD_ERASER_COMSCRIPT.getFileName(appManager, axisID), axisID, true,
        required, false, false);
      return scriptGoldEraserB != null;
    }
    scriptGoldEraserA = ComScriptUtil.loadComScript(appManager,
      FileType.GOLD_ERASER_COMSCRIPT.getFileName(appManager, axisID), axisID, true,
      required, false, false);
    return scriptGoldEraserA != null;
  }

  /**
   * @param axisID
   * @param required
   * @return true if script has loaded
   */
  public boolean loadMultifiltSetup(final AxisID axisID, final boolean required) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptMultifiltSetupB = ComScriptUtil.loadComScript(appManager,
        FileType.MULTIFILT_SETUP_COMSCRIPT.getFileName(appManager, axisID), axisID, true,
        required, false, false);
      return scriptMultifiltSetupB != null;
    }
    scriptMultifiltSetupA = ComScriptUtil.loadComScript(appManager,
      FileType.MULTIFILT_SETUP_COMSCRIPT.getFileName(appManager, axisID), axisID, true,
      required, false, false);
    return scriptMultifiltSetupA != null;
  }

  public SetEnvParam getSetEnvParamFromMultifiltSetup(final AxisID axisID,
    final String envVar) {
    ComScript comScript;
    if (axisID == AxisID.SECOND) {
      comScript = scriptMultifiltSetupB;
    }
    else {
      comScript = scriptMultifiltSetupA;
    }
    // Initialize a SetEnvParam object from the com script command
    // object
    SetEnvParam param = new SetEnvParam(envVar);
    // Assuming its the first setenv command in the comfile.
    if (!ComScriptUtil.initialize(appManager, param, comScript, SetEnvParam.COMMAND_NAME,
      axisID, true, null, true, false, false, false, envVar)) {
      return null;
    }
    return param;
  }

  public MultifiltSetupParam getMultifiltSetupParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript comScript;
    if (axisID == AxisID.SECOND) {
      comScript = scriptMultifiltSetupB;
    }
    else {
      comScript = scriptMultifiltSetupA;
    }

    // Initialize a MultifiltSetupParam object from the com script command object
    MultifiltSetupParam param = new MultifiltSetupParam(appManager, axisID);
    ComScriptUtil.initialize(appManager, param, comScript,
      ProcessName.MULTIFILT_SETUP.getText(), axisID, false, false, true);
    return param;
  }

  public void saveMultifiltSetup(final SetEnvParam param, final AxisID axisID,
    final String envVar) {
    ComScript script;
    if (axisID == AxisID.SECOND) {
      script = scriptMultifiltSetupB;
    }
    else {
      script = scriptMultifiltSetupA;
    }
    ComScriptUtil.addModifyCommand(appManager, script, param, SetEnvParam.COMMAND_NAME,
      axisID, false, false, envVar);
  }

  public void saveMultifiltSetup(final MultifiltSetupParam param, final AxisID axisID) {
    ComScript script;
    if (axisID == AxisID.SECOND) {
      script = scriptMultifiltSetupB;
    }
    else {
      script = scriptMultifiltSetupA;
    }
    ComScriptUtil.modifyCommand(appManager, script, param,
      ProcessName.MULTIFILT_SETUP.getText(), axisID, false, false);
  }

  public boolean loadCryoPosition(final AxisID axisID, final boolean required) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptCryoPositionB = ComScriptUtil.loadComScript(appManager,
        FileType.CRYO_POSITION_COMSCRIPT.getFileName(appManager, axisID), axisID, true,
        required, false, false);
      return scriptCryoPositionB != null;
    }
    scriptCryoPositionA = ComScriptUtil.loadComScript(appManager,
      FileType.CRYO_POSITION_COMSCRIPT.getFileName(appManager, axisID), axisID, true,
      required, false, false);
    return scriptCryoPositionA != null;
  }

  public CryoPositionParam getCryoPositionParam(final AxisID axisID,
    final AxisType axisType) {
    ComScript script;
    if (axisID == AxisID.SECOND) {
      script = scriptCryoPositionB;
    }
    else {
      script = scriptCryoPositionA;
    }
    // Initialize
    CryoPositionParam param = new CryoPositionParam(axisID);
    ComScriptUtil.initialize(appManager, param, script,
      ProcessName.CRYO_POSITION.toString(), axisID, false, false, true);
    return param;
  }

  /**
   * Get the CCD eraser parameters from the specified eraser script object
   * @param axisID the AxisID to read.
   * @return a CCDEraserParam object that will be created and initialized
   * with the input arguments from eraser in the com script.
   */
  public CCDEraserParam getCCDEraserParam(final AxisID axisID, final CommandMode mode) {
    // Get a reference to the appropriate script object
    ComScript eraser;
    if (axisID == AxisID.SECOND) {
      eraser = scriptEraserB;
    }
    else {
      eraser = scriptEraserA;
    }

    // Initialize a CCDEraserParam object from the com script command object
    CCDEraserParam ccdEraserParam = new CCDEraserParam(appManager, axisID, mode);
    ComScriptUtil.initialize(appManager, ccdEraserParam, eraser, "ccderaser", axisID,
      false, false, true);
    return ccdEraserParam;
  }

  public CCDEraserParam getCCDEraserParamFromGoldEraser(final AxisID axisID,
    final CommandMode mode) {
    // Get a reference to the appropriate script object
    ComScript script;
    if (axisID == AxisID.SECOND) {
      script = scriptGoldEraserB;
    }
    else {
      script = scriptGoldEraserA;
    }

    // Initialize a param object from the com script command object
    CCDEraserParam param = new CCDEraserParam(appManager, axisID, mode);
    ComScriptUtil.initialize(appManager, param, script, CCDEraserParam.COMMAND_NAME,
      axisID, false, false, true);
    param.isExpandCircleIterationsSet();
    return param;
  }

  /**
   * Get the CCD eraser parameters from the specified eraser script object
   * @param axisID the AxisID to read.
   * @return a CCDEraserParam object that will be created and initialized
   * with the input arguments from eraser in the com script.
   */
  public CCDEraserParam getGoldEraserParam(final AxisID axisID, final CommandMode mode) {
    // Get a reference to the appropriate script object
    ComScript eraser;
    if (axisID == AxisID.SECOND) {
      eraser = scriptGoldEraserB;
    }
    else {
      eraser = scriptGoldEraserA;
    }

    // Initialize a CCDEraserParam object from the com script command object
    CCDEraserParam ccdEraserParam = new CCDEraserParam(appManager, axisID, mode);
    ComScriptUtil.initialize(appManager, ccdEraserParam, eraser, "ccderaser", axisID,
      false, false, true);
    return ccdEraserParam;
  }

  /**
   * Save the specified eraser com script updating the ccderaser parmaeters
   * @param axisID the AxisID to load.
   * @param ccdEraserParam a CCDEraserParam object containing the new input
   * parameters for the ccderaser command.
   */
  public void saveEraser(final CCDEraserParam ccdEraserParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptEraser;
    if (axisID == AxisID.SECOND) {
      scriptEraser = scriptEraserB;
    }
    else {
      scriptEraser = scriptEraserA;
    }

    // update the ccderaser parameters
    ComScriptUtil.modifyCommand(appManager, scriptEraser, ccdEraserParam, "ccderaser",
      axisID, false, false);
  }

  /**
   * Save the specified gold eraser com script updating the ccderaser parmaeters
   * @param axisID the AxisID to load.
   * @param ccdEraserParam a CCDEraserParam object containing the new input
   * parameters for the ccderaser command.
   */
  public void saveGoldEraser(final CCDEraserParam ccdEraserParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptEraser;
    if (axisID == AxisID.SECOND) {
      scriptEraser = scriptGoldEraserB;
    }
    else {
      scriptEraser = scriptGoldEraserA;
    }

    // update the ccderaser parameters
    ComScriptUtil.modifyCommand(appManager, scriptEraser, ccdEraserParam, "ccderaser",
      axisID, false, false);
  }

  /**
   * Load the specified xcorr com script and initialize the TiltXcorrParam
   * object
   * @param axisID the AxisID to load.
   */
  public void loadXcorr(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptXcorrB =
        ComScriptUtil.loadComScript(appManager, "xcorr", axisID, true, false, false);
    }
    else {
      scriptXcorrA =
        ComScriptUtil.loadComScript(appManager, "xcorr", axisID, true, false, false);
    }
  }

  /**
   * load or create undistort.com
   * @param axisID
   */
  public void loadUndistort(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      scriptUndistortB = ComScriptUtil.loadComScript(appManager,
        BlendmontParam.getProcessName(BlendmontParam.Mode.UNDISTORT), axisID, true, false,
        false);
    }
    else {
      scriptUndistortA = ComScriptUtil.loadComScript(appManager,
        BlendmontParam.getProcessName(BlendmontParam.Mode.UNDISTORT), axisID, true, false,
        false);
    }
  }

  /**
   * Get the tiltxcorr parameters from the specified xcorr_pt script object
   * @param axisID the AxisID to read.
   * @return a TiltxcorrParam object that will be created and initialized
   * with the input arguments from xcorr in the com script.
   */
  public TiltxcorrParam getTiltxcorrParamFromXcorrPt(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript xcorrPt;
    if (axisID == AxisID.SECOND) {
      xcorrPt = scriptXcorrPtB;
    }
    else {
      xcorrPt = scriptXcorrPtA;
    }

    // Initialize a TiltxcorrParam object from the com script command object
    TiltxcorrParam tiltXcorrParam =
      new TiltxcorrParam(appManager, axisID, ProcessName.XCORR_PT);
    ComScriptUtil.initialize(appManager, tiltXcorrParam, xcorrPt, "tiltxcorr", axisID,
      false, false, true);
    return tiltXcorrParam;
  }

  public ImodchopcontsParam getImodchopcontsParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript xcorrPt;
    if (axisID == AxisID.SECOND) {
      xcorrPt = scriptXcorrPtB;
    }
    else {
      xcorrPt = scriptXcorrPtA;
    }

    // Initialize a TiltxcorrParam object from the com script command object
    ImodchopcontsParam param = new ImodchopcontsParam();
    ComScriptUtil.initialize(appManager, param, xcorrPt, "imodchopconts", axisID, false,
      false, true, true, null);
    return param;
  }

  /**
   * Get the first goto command from xcorr.com
   * @param axisID
   * @return
   */
  public GotoParam getGotoParamFromXcorrPt(final AxisID axisID, final boolean required) {
    ComScript xcorrPt;
    if (axisID == AxisID.SECOND) {
      xcorrPt = scriptXcorrPtB;
    }
    else {
      xcorrPt = scriptXcorrPtA;
    }
    // Initialize a GotoParam object from the com script command
    // object
    GotoParam gotoParam = new GotoParam();
    if (!ComScriptUtil.initialize(appManager, gotoParam, xcorrPt, GotoParam.COMMAND_NAME,
      axisID, false, false, required)) {
      return null;
    }
    return gotoParam;
  }

  public SetEnvParam getSetEnvParamFromXcorr(final AxisID axisID, final String name,
    final boolean required) {
    ComScript xcorr;
    if (axisID == AxisID.SECOND) {
      xcorr = scriptXcorrB;
    }
    else {
      xcorr = scriptXcorrA;
    }
    // Initialize a SetEnvParam object from the com script command
    // object
    SetEnvParam param = new SetEnvParam(name);
    // Assuming its the first setenv command in the comfile.
    if (!ComScriptUtil.initialize(appManager, param, xcorr, SetEnvParam.COMMAND_NAME,
      axisID, true, null, true, false, false, required)) {
      return null;
    }
    return param;
  }

  /**
   * Get the autofidseed parameters
   * @param axisID the AxisID to read.
   * @return a TiltxcorrParam object that will be created and initialized
   * with the input arguments from xcorr in the com script.
   */
  public AutofidseedParam getAutofidseedParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript comScript;
    if (axisID == AxisID.SECOND) {
      comScript = scriptAutofidseedB;
    }
    else {
      comScript = scriptAutofidseedA;
    }

    // Initialize a TiltxcorrParam object from the com script command object
    AutofidseedParam param = new AutofidseedParam(appManager, axisID);
    ComScriptUtil.initialize(appManager, param, comScript, "autofidseed", axisID, false,
      false, true);
    return param;
  }

  /**
   * Get the tiltxcorr parameters from the specified xcorr script object
   * @param axisID the AxisID to read.
   * @return a TiltxcorrParam object that will be created and initialized
   * with the input arguments from xcorr in the com script.
   */
  public TiltxcorrParam getTiltxcorrParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript xcorr;
    if (axisID == AxisID.SECOND) {
      xcorr = scriptXcorrB;
    }
    else {
      xcorr = scriptXcorrA;
    }

    // Initialize a TiltxcorrParam object from the com script command object
    TiltxcorrParam tiltXcorrParam =
      new TiltxcorrParam(appManager, axisID, ProcessName.XCORR);
    ComScriptUtil.initialize(appManager, tiltXcorrParam, xcorr, "tiltxcorr", axisID,
      false, false, true);
    return tiltXcorrParam;
  }

  /**
   * Save the specified xcorr_pt com script updating the tiltxcorr parameters
   * @param axisID the AxisID to load.
   * @param tiltXcorrParam a TiltxcorrParam object that will be used to update
   * the xcorr com script
   */
  public void saveXcorrPt(final TiltxcorrParam tiltXcorrParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptXcorrPt;
    if (axisID == AxisID.SECOND) {
      scriptXcorrPt = scriptXcorrPtB;
    }
    else {
      scriptXcorrPt = scriptXcorrPtA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptXcorrPt, tiltXcorrParam, "tiltxcorr",
      axisID, false, false);
  }

  public void saveXcorrPt(final ImodchopcontsParam param, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptXcorrPt;
    if (axisID == AxisID.SECOND) {
      scriptXcorrPt = scriptXcorrPtB;
    }
    else {
      scriptXcorrPt = scriptXcorrPtA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptXcorrPt, param, "imodchopconts", axisID,
      false, false);
  }

  public void saveXcorrPt(final GotoParam param, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptXcorrPt;
    if (axisID == AxisID.SECOND) {
      scriptXcorrPt = scriptXcorrPtB;
    }
    else {
      scriptXcorrPt = scriptXcorrPtA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptXcorrPt, param, "goto", axisID, false,
      false);
  }

  /**
   * Save the specified autofidseed com script, updating the autofidseed parameters
   * @param axisID the AxisID to load.
   * @param param an AutofidseedParam object that will be used to update
   * the autofidseed com script
   */
  public void saveAutofidseed(final AutofidseedParam param, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript comScript;
    if (axisID == AxisID.SECOND) {
      comScript = scriptAutofidseedB;
    }
    else {
      comScript = scriptAutofidseedA;
    }
    ComScriptUtil.modifyCommand(appManager, comScript, param, "autofidseed", axisID,
      false, false);
  }

  /**
   * Save the specified xcorr com script updating the tiltxcorr parameters
   * @param axisID the AxisID to load.
   * @param tiltXcorrParam a TiltxcorrParam object that will be used to update
   * the xcorr com script
   */
  public void saveXcorr(final TiltxcorrParam tiltXcorrParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptXcorr;
    if (axisID == AxisID.SECOND) {
      scriptXcorr = scriptXcorrB;
    }
    else {
      scriptXcorr = scriptXcorrA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptXcorr, tiltXcorrParam, "tiltxcorr",
      axisID, false, false);
  }

  /**
   * Save the blendmont param object to xcorr.com.
   * @param blendmontParam
   * @param axisID
   */
  public void saveXcorr(final BlendmontParam blendmontParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptXcorr;
    if (axisID == AxisID.SECOND) {
      scriptXcorr = scriptXcorrB;
    }
    else {
      scriptXcorr = scriptXcorrA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptXcorr, blendmontParam,
      BlendmontParam.COMMAND_NAME, axisID, false, false);
  }

  public void saveXcorrToUndistort(final BlendmontParam blendmontParam,
    final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptUndistort;
    ComScript scriptXcorr;
    if (axisID == AxisID.SECOND) {
      scriptUndistort = scriptUndistortB;
      scriptXcorr = scriptXcorrB;
    }
    else {
      scriptUndistort = scriptUndistortA;
      scriptXcorr = scriptXcorrA;
    }
    ComScriptUtil.addModifyCommand(appManager, scriptXcorr, scriptUndistort,
      blendmontParam, BlendmontParam.COMMAND_NAME, axisID, false, false);
  }

  /**
   * Save the goto param object to xcorr.com.
   * Saves to the first instance of the goto command in xcorr.com.
   * @param gotoParam
   * @param axisID
   */
  public void saveXcorr(final GotoParam gotoParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptXcorr;
    if (axisID == AxisID.SECOND) {
      scriptXcorr = scriptXcorrB;
    }
    else {
      scriptXcorr = scriptXcorrA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptXcorr, gotoParam,
      GotoParam.COMMAND_NAME, axisID, false, false);
  }

  /**
   * Load the specified prenewst com script and initialize the NewstParam
   * object
   * @param axisID the AxisID to load.
   */
  public void loadPrenewst(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptPrenewstB =
        ComScriptUtil.loadComScript(appManager, "prenewst", axisID, true, false, false);
    }
    else {
      scriptPrenewstA =
        ComScriptUtil.loadComScript(appManager, "prenewst", axisID, true, false, false);
    }
  }

  public void loadPreblend(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptPreblendB = ComScriptUtil.loadComScript(appManager,
        BlendmontParam.getProcessName(BlendmontParam.Mode.PREBLEND), axisID, true, false,
        false);
    }
    else {
      scriptPreblendA = ComScriptUtil.loadComScript(appManager,
        BlendmontParam.getProcessName(BlendmontParam.Mode.PREBLEND), axisID, true, false,
        false);
    }
  }

  public void loadBlend(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptBlendB = ComScriptUtil.loadComScript(appManager,
        BlendmontParam.getProcessName(BlendmontParam.Mode.BLEND), axisID, true, false,
        false);
    }
    else {
      scriptBlendA = ComScriptUtil.loadComScript(appManager,
        BlendmontParam.getProcessName(BlendmontParam.Mode.BLEND), axisID, true, false,
        false);
    }
  }

  public void loadBlend3dFind(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptBlend3dFindB = ComScriptUtil.loadComScript(appManager,
        ProcessName.BLEND_3D_FIND, axisID, true, false, false);
    }
    else {
      scriptBlend3dFindA = ComScriptUtil.loadComScript(appManager,
        ProcessName.BLEND_3D_FIND, axisID, true, false, false);
    }
  }

  /**
   * @param axisID
   * @param required
   * @return true if script has loaded
   */
  public boolean loadAutofidseed(final AxisID axisID, final boolean required) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptAutofidseedB = ComScriptUtil.loadComScript(appManager,
        FileType.AUTOFIDSEED_COMSCRIPT.getFileName(appManager, axisID), axisID, true,
        required, false, false);
      return scriptAutofidseedB != null;
    }
    scriptAutofidseedA = ComScriptUtil.loadComScript(appManager,
      FileType.AUTOFIDSEED_COMSCRIPT.getFileName(appManager, axisID), axisID, true,
      required, false, false);
    return scriptAutofidseedA != null;
  }

  /**
   * @param axisID
   * @param required
   * @return true if script has loaded
   */
  public boolean loadXcorrPt(final AxisID axisID, final boolean required) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptXcorrPtB = ComScriptUtil.loadComScript(appManager,
        FileType.PATCH_TRACKING_COMSCRIPT.getFileName(appManager, axisID), axisID, true,
        required, false, false);
      return scriptXcorrPtB != null;
    }
    scriptXcorrPtA = ComScriptUtil.loadComScript(appManager,
      FileType.PATCH_TRACKING_COMSCRIPT.getFileName(appManager, axisID), axisID, true,
      required, false, false);
    return scriptXcorrPtA != null;
  }

  public boolean loadSirtsetup(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptSirtsetupB = ComScriptUtil.loadComScript(appManager,
        FileType.SIRTSETUP_COMSCRIPT.getFileName(appManager, axisID), axisID, true, false,
        false, false);
      return scriptSirtsetupB != null;
    }
    scriptSirtsetupA = ComScriptUtil.loadComScript(appManager,
      FileType.SIRTSETUP_COMSCRIPT.getFileName(appManager, axisID), axisID, true, false,
      false, false);
    return scriptSirtsetupA != null;
  }

  public SirtsetupParam getSirtsetupParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript sirtsetup;
    if (axisID == AxisID.SECOND) {
      sirtsetup = scriptSirtsetupB;
    }
    else {
      sirtsetup = scriptSirtsetupA;
    }

    // Initialize a SirtsetupParam object from the com script command object
    SirtsetupParam param = new SirtsetupParam(appManager, axisID);
    ComScriptUtil.initialize(appManager, param, sirtsetup,
      ProcessName.SIRTSETUP.getText(), axisID, true, true, true);
    return param;
  }

  public void saveSirtsetup(final SirtsetupParam param, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript script;
    if (axisID == AxisID.SECOND) {
      script = scriptSirtsetupB;
    }
    else {
      script = scriptSirtsetupA;
    }
    ComScriptUtil.modifyCommand(appManager, script, param,
      ProcessName.SIRTSETUP.toString(), axisID, false, false);
  }

  /**
   * @param axisID
   * @param required
   * @return true if script has loaded
   */
  public boolean loadCtfPlotter(final AxisID axisID, final boolean required) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptCtfPlotterB = ComScriptUtil.loadComScript(appManager,
        ProcessName.CTF_PLOTTER.getComscript(axisID), axisID, true, required, false,
        false);
      return scriptCtfPlotterB != null;
    }
    scriptCtfPlotterA = ComScriptUtil.loadComScript(appManager,
      ProcessName.CTF_PLOTTER.getComscript(axisID), axisID, true, required, false, false);
    return scriptCtfPlotterA != null;
  }

  public boolean loadCtfCorrection(final AxisID axisID, final boolean required) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptCtfCorrectionB = ComScriptUtil.loadComScript(appManager,
        ProcessName.CTF_CORRECTION.getComscript(axisID), axisID, true, required, false,
        false);
      return scriptCtfCorrectionB != null;
    }
    scriptCtfCorrectionA = ComScriptUtil.loadComScript(appManager,
      ProcessName.CTF_CORRECTION.getComscript(axisID), axisID, true, required, false,
      false);
    return scriptCtfCorrectionA != null;
  }

  /**
   * Get the newstack parameters from the specified prenewst script object
   * @param axisID the AxisID to read.
   * @return a NewstParam object that will be created and initialized
   * with the input arguments from prenewst in the com script.
   */
  public NewstParam getPrenewstParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptPrenewst;
    if (axisID == AxisID.SECOND) {
      scriptPrenewst = scriptPrenewstB;
    }
    else {
      scriptPrenewst = scriptPrenewstA;
    }

    // Initialize a NewstParam object from the com script command object
    NewstParam prenewstParam = NewstParam.getInstance(appManager, axisID);

    // Implementation note: since the name of the command newst was changed to
    // newstack we need to figure out which one it is before calling initialize.
    String cmdName = newstOrNewstack(scriptPrenewst);
    ComScriptUtil.initialize(appManager, prenewstParam, scriptPrenewst, cmdName, axisID,
      false, false, true);
    return prenewstParam;
  }

  public BlendmontParam getPreblendParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptPreblend;
    if (axisID == AxisID.SECOND) {
      scriptPreblend = scriptPreblendB;
    }
    else {
      scriptPreblend = scriptPreblendA;
    }

    // Initialize a BlendmontParam object from the com script command object
    BlendmontParam preblendParam = new BlendmontParam(appManager,
      appManager.getMetaData().getDatasetName(), axisID, BlendmontParam.Mode.PREBLEND);
    ComScriptUtil.initialize(appManager, preblendParam, scriptPreblend,
      BlendmontParam.COMMAND_NAME, axisID, false, false, true);
    return preblendParam;
  }

  public BlendmontParam getBlendParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptBlend;
    if (axisID == AxisID.SECOND) {
      scriptBlend = scriptBlendB;
    }
    else {
      scriptBlend = scriptBlendA;
    }

    // Initialize a BlendmontParam object from the com script command object
    BlendmontParam blendParam = new BlendmontParam(appManager,
      appManager.getMetaData().getDatasetName(), axisID, BlendmontParam.Mode.BLEND);
    ComScriptUtil.initialize(appManager, blendParam, scriptBlend,
      BlendmontParam.COMMAND_NAME, axisID, false, false, true);
    return blendParam;
  }

  /**
   * Get BlendmontParam from blend_3dfind.com
   * @param axisID
   * @return
   */
  public BlendmontParam getBlendParamFromBlend3dFind(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptBlend3dFind;
    if (axisID == AxisID.SECOND) {
      scriptBlend3dFind = scriptBlend3dFindB;
    }
    else {
      scriptBlend3dFind = scriptBlend3dFindA;
    }

    // Initialize a BlendmontParam object from the com script command object
    BlendmontParam blendParam =
      new BlendmontParam(appManager, appManager.getMetaData().getDatasetName(), axisID,
        BlendmontParam.Mode.BLEND_3DFIND);
    ComScriptUtil.initialize(appManager, blendParam, scriptBlend3dFind,
      BlendmontParam.COMMAND_NAME, axisID, false, false, true);
    return blendParam;
  }

  /**
   * Get MrcTaperParam from blend_3dfind.com
   * @param axisID
   * @return
   */
  public MrcTaperParam getMrcTaperParamFromBlend3dFind(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptBlend3dFind;
    if (axisID == AxisID.SECOND) {
      scriptBlend3dFind = scriptBlend3dFindB;
    }
    else {
      scriptBlend3dFind = scriptBlend3dFindA;
    }

    // Initialize a MrcTaperParam object from the com script command object
    MrcTaperParam param = new MrcTaperParam(appManager, axisID);
    ComScriptUtil.initialize(appManager, param, scriptBlend3dFind,
      MrcTaperParam.COMMAND_NAME, axisID, false, false, true);
    return param;
  }

  /**
   * Save the specified prenewst com script updating the newst parameters
   * @param axisID the AxisID to load.
   * @param tiltXcorrParam a TiltxcorrParam object that will be used to update
   * the xcorr com script
   */
  public void savePrenewst(final NewstParam prenewstParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptPrenewst;
    if (axisID == AxisID.SECOND) {
      scriptPrenewst = scriptPrenewstB;
    }
    else {
      scriptPrenewst = scriptPrenewstA;
    }

    // Implementation note: since the name of the command newst was changed to
    // newstack we need to figure out which one it is before calling initialize.
    String cmdName = newstOrNewstack(scriptPrenewst);
    ComScriptUtil.modifyCommand(appManager, scriptPrenewst, prenewstParam, cmdName,
      axisID, false, false);
  }

  public void savePreblend(final BlendmontParam blendmontParam, final AxisID axisID) {
    // TEMP
    System.err.println("savePreblend:mode=" + blendmontParam.getMode());
    // Get a reference to the appropriate script object
    ComScript scriptPreblend;
    if (axisID == AxisID.SECOND) {
      scriptPreblend = scriptPreblendB;
    }
    else {
      scriptPreblend = scriptPreblendA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptPreblend, blendmontParam,
      BlendmontParam.COMMAND_NAME, axisID, false, false);
  }

  public void saveCryoPosition(final CryoPositionParam param, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript script;
    if (axisID == AxisID.SECOND) {
      script = scriptCryoPositionB;
    }
    else {
      script = scriptCryoPositionA;
    }
    ComScriptUtil.modifyCommand(appManager, script, param,
      ProcessName.CRYO_POSITION.toString(), axisID, false, false);
  }

  public void saveBlend(final BlendmontParam blendmontParam, final AxisID axisID) {
    // TEMP
    System.err.println("saveBlend:mode=" + blendmontParam.getMode());
    // Get a reference to the appropriate script object
    ComScript scriptBlend;
    if (axisID == AxisID.SECOND) {
      scriptBlend = scriptBlendB;
    }
    else {
      scriptBlend = scriptBlendA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptBlend, blendmontParam,
      BlendmontParam.COMMAND_NAME, axisID, false, false);
  }

  public void saveBlend3dFind(final BlendmontParam blendmontParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptBlend3dFind;
    if (axisID == AxisID.SECOND) {
      scriptBlend3dFind = scriptBlend3dFindB;
    }
    else {
      scriptBlend3dFind = scriptBlend3dFindA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptBlend3dFind, blendmontParam,
      BlendmontParam.COMMAND_NAME, axisID, false, false);
  }

  public void saveBlend3dFind(final MrcTaperParam param, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptBlend3dFind;
    if (axisID == AxisID.SECOND) {
      scriptBlend3dFind = scriptBlend3dFindB;
    }
    else {
      scriptBlend3dFind = scriptBlend3dFindA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptBlend3dFind, param,
      MrcTaperParam.COMMAND_NAME, axisID, false, false);
  }

  public void saveFindBeads3d(final FindBeads3dParam param, final AxisID axisID) {
    ComScript script;
    if (axisID == AxisID.SECOND) {
      script = scriptFindBeads3dB;
    }
    else {
      script = scriptFindBeads3dA;
    }
    ComScriptUtil.modifyCommand(appManager, script, param,
      ProcessName.FIND_BEADS_3D.toString(), axisID, false, false);
  }

  public void saveCtfPlotter(final CtfPlotterParam param, final AxisID axisID) {
    ComScript script;
    if (axisID == AxisID.SECOND) {
      script = scriptCtfPlotterB;
    }
    else {
      script = scriptCtfPlotterA;
    }
    ComScriptUtil.modifyCommand(appManager, script, param,
      ProcessName.CTF_PLOTTER.toString(), axisID, false, false);
  }

  public void saveCtfPhaseFlip(final CtfPhaseFlipParam param, final AxisID axisID) {
    ComScript script;
    if (axisID == AxisID.SECOND) {
      script = scriptCtfCorrectionB;
    }
    else {
      script = scriptCtfCorrectionA;
    }
    ComScriptUtil.modifyCommand(appManager, script, param, CtfPhaseFlipParam.COMMAND,
      axisID, false, false);
  }

  /**
   * Load the specified track com script
   * @param axisID the AxisID to load.
   */
  public void loadTrack(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptTrackB =
        ComScriptUtil.loadComScript(appManager, "track", axisID, true, false, false);
    }
    else {
      scriptTrackA =
        ComScriptUtil.loadComScript(appManager, "track", axisID, true, false, false);
    }
  }

  /**
   * Load the specified findbeads3d com script
   * @param axisID the AxisID to load.
   */
  public void loadFindBeads3d(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptFindBeads3dB = ComScriptUtil.loadComScript(appManager,
        ProcessName.FIND_BEADS_3D.toString(), axisID, true, false, false);
    }
    else {
      scriptFindBeads3dA = ComScriptUtil.loadComScript(appManager,
        ProcessName.FIND_BEADS_3D.toString(), axisID, true, false, false);
    }
  }

  /**
   * Get the beadtrack parameters from the specified track script object
   * @param axisID the AxisID to read.
   * @return a BeadtrackParam object that will be created and initialized
   * with the input arguments from beadtrack in the com script.
   */
  public BeadtrackParam getBeadtrackParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript track;
    if (axisID == AxisID.SECOND) {
      track = scriptTrackB;
    }
    else {
      track = scriptTrackA;
    }

    // Initialize a BeadtrckParam object from the com script command object
    BeadtrackParam beadtrackParam = new BeadtrackParam(axisID, appManager);
    ComScriptUtil.initialize(appManager, beadtrackParam, track, "beadtrack", axisID,
      false, false, true);
    return beadtrackParam;
  }

  /**
   * Save the specified track com script updating the beadtrack parameters
   * @param axisID the AxisID to load.
   * @param beadtrackParam a BeadtrackParam object that will be used to update
   * the track com script
   */
  public void saveTrack(final BeadtrackParam beadtrackParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptTrack;
    if (axisID == AxisID.SECOND) {
      scriptTrack = scriptTrackB;
    }
    else {
      scriptTrack = scriptTrackA;
    }
    // update the beadtrack parameters
    ComScriptUtil.modifyCommand(appManager, scriptTrack, beadtrackParam, "beadtrack",
      axisID, false, false);
  }

  /**
   * Load the specified align com script object
   * @param axisID the AxisID to load.
   */
  public void loadAlign(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptAlignB =
        ComScriptUtil.loadComScript(appManager, "align", axisID, true, false, false);
    }
    else {
      scriptAlignA =
        ComScriptUtil.loadComScript(appManager, "align", axisID, true, false, false);
    }
  }

  public void resetAlign(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      scriptAlignB = null;
    }
    else {
      scriptAlignA = null;
    }
  }

  /**
   * Get the tiltalign parameters from the specified align script object
   * @param axisID the AxisID to read.
   * @return a TiltalignParam object that will be created and initialized
   * with the input arguments from tiltalign in the com script.
   */
  public TiltalignParam getTiltalignParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript align;
    if (axisID == AxisID.SECOND) {
      align = scriptAlignB;
    }
    else {
      align = scriptAlignA;
    }

    // Initialize a BeadtrckParam object from the com script command object
    TiltalignParam tiltalignParam =
      new TiltalignParam(appManager, appManager.getMetaData().getDatasetName(), axisID);
    ComScriptUtil.initialize(appManager, tiltalignParam, align, "tiltalign", axisID,
      false, false, true);
    return tiltalignParam;
  }

  /**
   * Get the xfproduct parameter from the align script
   * @param axisID
   * @return
   */
  public XfproductParam getXfproductInAlign(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript align;
    if (axisID == AxisID.SECOND) {
      align = scriptAlignB;
    }
    else {
      align = scriptAlignA;
    }

    // Initialize a BeadtrckParam object from the com script command object
    XfproductParam xfproductParam = new XfproductParam();
    ComScriptUtil.initialize(appManager, xfproductParam, align, "xfproduct", axisID,
      false, false, true);
    return xfproductParam;
  }

  /**
   * Save the specified align com script updating the tiltalign parameters
   * @param axisID the AxisID to load.
   * @param tiltalignParam a TiltalignParam object that will be used to update
   * tiltalign command in the align com script
   */
  public void saveAlign(final TiltalignParam tiltalignParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptAlign;
    if (axisID == AxisID.SECOND) {
      scriptAlign = scriptAlignB;
    }
    else {
      scriptAlign = scriptAlignA;
    }

    // update the tiltalign parameters
    ComScriptUtil.modifyCommand(appManager, scriptAlign, tiltalignParam, "tiltalign",
      axisID, false, false);
  }

  /**
   * Save the xfproduct command to the specified align com script
   * @param xfproductParam
   * @param axisID
   */
  public void saveXfproductInAlign(final XfproductParam xfproductParam,
    final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptAlign;
    if (axisID == AxisID.SECOND) {
      scriptAlign = scriptAlignB;
    }
    else {
      scriptAlign = scriptAlignA;
    }

    // update the tiltalign parameters
    ComScriptUtil.modifyCommand(appManager, scriptAlign, xfproductParam, "xfproduct",
      axisID, false, false);
  }

  /**
   * Load the specified newst com script
   * @param axisID the AxisID to load.
   */
  public void loadNewst(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptNewstB =
        ComScriptUtil.loadComScript(appManager, "newst", axisID, true, false, false);
    }
    else {
      scriptNewstA =
        ComScriptUtil.loadComScript(appManager, "newst", axisID, true, false, false);
    }
  }

  /**
   * Load the specified newst_3dfind com script
   * @param axisID the AxisID to load.
   */
  public void loadNewst3dFind(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptNewst3dFindB = ComScriptUtil.loadComScript(appManager,
        ProcessName.NEWST_3D_FIND, axisID, true, false, false);
    }
    else {
      scriptNewst3dFindA = ComScriptUtil.loadComScript(appManager,
        ProcessName.NEWST_3D_FIND, axisID, true, false, false);
    }
  }

  /**
   * Return true if scriptNewst3dFind is null.
   * @param axisID
   * @return
   */
  public boolean isScriptNewst3dFindNull(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return scriptNewst3dFindB == null;
    }
    return scriptNewst3dFindA == null;
  }

  /**
   * Return true if scriptBlend3dFind is null.
   * @param axisID
   * @return
   */
  public boolean isScriptBlend3dFindNull(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return scriptBlend3dFindB == null;
    }
    return scriptBlend3dFindA == null;
  }

  /**
   * Get the newst parameters from the specified newst script object
   * @param axisID the AxisID to read.
   * @return a NewstParam object that will be created and initialized
   * with the input arguments from newst in the com script.
   */
  public NewstParam getNewstComNewstParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptNewst;
    if (axisID == AxisID.SECOND) {
      scriptNewst = scriptNewstB;
    }
    else {
      scriptNewst = scriptNewstA;
    }

    // Initialize a NewstParam object from the com script command object
    NewstParam newstParam = NewstParam.getInstance(appManager, axisID);

    // Implementation note: since the name of the command newst was changed to
    // newstack we need to figure out which one it is before calling initialize.
    String cmdName = newstOrNewstack(scriptNewst);
    ComScriptUtil.initialize(appManager, newstParam, scriptNewst, cmdName, axisID, false,
      false, true);
    return newstParam;
  }

  /**
   * Get the newst_3dfind parameters from the specified newst_3dfind script object
   * @param axisID the AxisID to read.
   * @return a NewstParam object that will be created and initialized
   * with the input arguments from newst in the com script.
   */
  public NewstParam getNewstParamFromNewst3dFind(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptNewst3dfind;
    if (axisID == AxisID.SECOND) {
      scriptNewst3dfind = scriptNewst3dFindB;
    }
    else {
      scriptNewst3dfind = scriptNewst3dFindA;
    }

    // Initialize a NewstParam object from the com script command object
    NewstParam newstParam = NewstParam.getInstance(appManager, axisID);

    // Implementation note: since the name of the command newst was changed to
    // newstack we need to figure out which one it is before calling initialize.
    String cmdName = newstOrNewstack(scriptNewst3dfind);
    ComScriptUtil.initialize(appManager, newstParam, scriptNewst3dfind, cmdName, axisID,
      false, false, true);
    return newstParam;
  }

  public FindBeads3dParam getFindBeads3dParam(final AxisID axisID) {
    ComScript script;
    if (axisID == AxisID.SECOND) {
      script = scriptFindBeads3dB;
    }
    else {
      script = scriptFindBeads3dA;
    }
    FindBeads3dParam param = new FindBeads3dParam(appManager, axisID);
    ComScriptUtil.initialize(appManager, param, script,
      ProcessName.FIND_BEADS_3D.toString(), axisID, false, false, true);
    return param;
  }

  /**
   * Get the newst_3dfind parameters from the specified newst_3dfind script object
   * @param axisID the AxisID to read.
   * @return a MrcTaperParam object that will be created and initialized
   * with the input arguments from newst in the com script.
   */
  public MrcTaperParam getMrcTaperParamFromNewst3dFind(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptNewst3dfind;
    if (axisID == AxisID.SECOND) {
      scriptNewst3dfind = scriptNewst3dFindB;
    }
    else {
      scriptNewst3dfind = scriptNewst3dFindA;
    }

    // Initialize a NewstParam object from the com script command object
    MrcTaperParam param = new MrcTaperParam(appManager, axisID);

    // Implementation note: since the name of the command newst was changed to
    // newstack we need to figure out which one it is before calling initialize.
    ComScriptUtil.initialize(appManager, param, scriptNewst3dfind,
      MrcTaperParam.COMMAND_NAME, axisID, false, false, true);
    return param;
  }

  /**
   * Save the specified newst com script updating the newst parameters
   * @param axisID the AxisID to load.
   * @param tiltalignParam a TiltalignParam object that will be used to update
   * tiltalign command in the align com script
   */
  public void saveNewst(final NewstParam newstParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptNewst;
    if (axisID == AxisID.SECOND) {
      scriptNewst = scriptNewstB;
    }
    else {
      scriptNewst = scriptNewstA;
    }

    // Implementation note: since the name of the command newst was changed to
    // newstack we need to figure out which one it is before calling initialize.
    String cmdName = newstOrNewstack(scriptNewst);

    // update the newst parameters
    ComScriptUtil.modifyCommand(appManager, scriptNewst, newstParam, cmdName, axisID,
      false, false);
  }

  /**
   * Save the specified newst_3dfind com script updating the newst_3dfind parameters
   * @param axisID the AxisID to load.
   * @param newstParam a NewstParam object that will be used to update
   * NewstParam command in the newst_3dfind com script
   */
  public void saveNewst3dFind(final NewstParam newstParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptNewst3dFind;
    if (axisID == AxisID.SECOND) {
      scriptNewst3dFind = scriptNewst3dFindB;
    }
    else {
      scriptNewst3dFind = scriptNewst3dFindA;
    }

    // Implementation note: since the name of the command newst was changed to
    // newstack we need to figure out which one it is before calling initialize.
    String cmdName = newstOrNewstack(scriptNewst3dFind);

    // update the newst parameters
    ComScriptUtil.modifyCommand(appManager, scriptNewst3dFind, newstParam, cmdName,
      axisID, false, false);
  }

  /**
   * Save the specified newst_3dfind com script updating the newst_3dfind parameters
   * @param axisID the AxisID to load.
   * @param param a MrcTaperParam object that will be used to update
   * MrcTaperParam command in the newst_3dfind com script
   */
  public void saveNewst3dFind(final MrcTaperParam param, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptNewst3dFind;
    if (axisID == AxisID.SECOND) {
      scriptNewst3dFind = scriptNewst3dFindB;
    }
    else {
      scriptNewst3dFind = scriptNewst3dFindA;
    }
    // update the newst parameters
    ComScriptUtil.modifyCommand(appManager, scriptNewst3dFind, param,
      MrcTaperParam.COMMAND_NAME, axisID, false, false);
  }

  /**
   * Load the specified tilt com script
   * @param axisID the AxisID to load.
   */
  public void loadTilt(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptTiltB = ComScriptUtil.loadComScript(appManager, ProcessName.TILT, axisID,
        false, true, true);
    }
    else {
      scriptTiltA = ComScriptUtil.loadComScript(appManager, ProcessName.TILT, axisID,
        false, true, true);
    }
  }

  /**
   * Load the specified tilt_for_sirt com script
   * @param axisID the AxisID to load.
   */
  public void loadTiltForSirt(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptTiltForSirtB = ComScriptUtil.loadComScript(appManager,
        FileType.TILT_FOR_SIRT_COMSCRIPT, axisID, false, true, true);
    }
    else {
      scriptTiltForSirtA = ComScriptUtil.loadComScript(appManager,
        FileType.TILT_FOR_SIRT_COMSCRIPT, axisID, false, true, true);
    }
  }

  /**
   * Load the specified tilt_3dfind com script
   * @param axisID the AxisID to load.
   */
  public void loadTilt3dFind(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptTilt3dFindB =
        ComScriptUtil.loadComScript(appManager, "tilt_3dfind", axisID, false, true, true);
    }
    else {
      scriptTilt3dFindA =
        ComScriptUtil.loadComScript(appManager, "tilt_3dfind", axisID, false, true, true);
    }
  }

  /**
   * Load the specified tilt_3dfind_reproject com script
   * @param axisID the AxisID to load.
   */
  public void loadTilt3dFindReproject(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      scriptTilt3dFindReprojectB = ComScriptUtil.loadComScript(appManager,
        ProcessName.TILT_3D_FIND_REPROJECT, axisID, false, true, true);
    }
    else {
      scriptTilt3dFindReprojectA = ComScriptUtil.loadComScript(appManager,
        ProcessName.TILT_3D_FIND_REPROJECT, axisID, false, true, true);
    }
  }

  /**
   * Get the tilt parameters from the specified tilt script object
   * @param axisID the AxisID to read.
   * @return a TiltParam object that will be created and initialized
   * with the input arguments from tilt in the com script.
   */
  public TiltParam getTiltParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript tilt;
    if (axisID == AxisID.SECOND) {
      tilt = scriptTiltB;
    }
    else {
      tilt = scriptTiltA;
    }

    // Initialize a TiltParam object from the com script command object
    TiltParam tiltParam =
      new TiltParam(appManager, appManager.getMetaData().getDatasetName(), axisID);
    ComScriptUtil.initialize(appManager, tiltParam, tilt, "tilt", axisID, true, true,
      true);
    return tiltParam;
  }

  /**
   * Get the tilt parameters from the specified tilt script object
   * @param axisID the AxisID to read.
   * @return a TiltParam object that will be created and initialized
   * with the input arguments from tilt in the tilt for sirt com script.
   */
  public TiltParam getTiltParamFromTiltForSirt(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript comScript;
    if (axisID == AxisID.SECOND) {
      comScript = scriptTiltForSirtB;
    }
    else {
      comScript = scriptTiltForSirtA;
    }
    // Initialize a TiltParam object from the com script command object
    TiltParam tiltParam =
      new TiltParam(appManager, appManager.getMetaData().getDatasetName(), axisID);
    ComScriptUtil.initialize(appManager, tiltParam, comScript, "tilt", axisID, true, true,
      true);
    return tiltParam;
  }

  /**
   * Get the tilt parameters from the specified tilt_3dfind script object
   * @param axisID the AxisID to read.
   * @return a TiltParam object that will be created and initialized
   * with the input arguments from tilt in the tilt_3dfind com script.
   */
  public TiltParam getTiltParamFromTilt3dFind(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript tilt;
    if (axisID == AxisID.SECOND) {
      tilt = scriptTilt3dFindB;
    }
    else {
      tilt = scriptTilt3dFindA;
    }

    // Initialize a TiltParam object from the com script command object
    TiltParam tiltParam =
      new TiltParam(appManager, appManager.getMetaData().getDatasetName(), axisID);
    ComScriptUtil.initialize(appManager, tiltParam, tilt, "tilt", axisID, true, true,
      true);
    return tiltParam;
  }

  /**
   * Get the tilt parameters from the specified tilt_3dfind_reproject script
   * object.
   * @param axisID the AxisID to read.
   * @return a TiltParam object that will be created and initialized
   * with the input arguments from tilt in the tilt_3dfind_reproject com script.
   */
  public TiltParam getTiltParamFromTilt3dFindReproject(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript tilt;
    if (axisID == AxisID.SECOND) {
      tilt = scriptTilt3dFindReprojectB;
    }
    else {
      tilt = scriptTilt3dFindReprojectA;
    }

    // Initialize a TiltParam object from the com script command object
    TiltParam tiltParam =
      new TiltParam(appManager, appManager.getMetaData().getDatasetName(), axisID);
    ComScriptUtil.initialize(appManager, tiltParam, tilt, "tilt", axisID, true, true,
      true);
    return tiltParam;
  }

  /**
   * Save the specified tilt com script updating the tilt parameters
   * @param axisID the AxisID to load.
   * @param tiltalignParam a TiltalignParam object that will be used to update
   * tiltalign command in the align com script
   */
  public void saveTilt(final TiltParam tiltParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptTilt;
    if (axisID == AxisID.SECOND) {
      scriptTilt = scriptTiltB;
    }
    else {
      scriptTilt = scriptTiltA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptTilt, tiltParam, "tilt", axisID, true,
      true);
  }

  /**
   * Save the specified tilt_3dfind com script updating the tilt parameters
   * @param axisID the AxisID to load.
   * @param tiltParam a TiltParam object that will be used to update
   * tilt command in the tilt_3dfind com script
   */
  public void saveTilt3dFind(final TiltParam tiltParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptTilt;
    if (axisID == AxisID.SECOND) {
      scriptTilt = scriptTilt3dFindB;
    }
    else {
      scriptTilt = scriptTilt3dFindA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptTilt, tiltParam, "tilt", axisID, true,
      true);
  }

  /**
   * Save the specified tilt_3dfind_reproject com script updating the tilt
   * parameters
   * @param axisID the AxisID to load.
   * @param tiltParam a TiltParam object that will be used to update
   * tilt command in the tilt_3dfind_reproject com script
   */
  public void saveTilt3dFindReproject(final TiltParam tiltParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptTilt;
    if (axisID == AxisID.SECOND) {
      scriptTilt = scriptTilt3dFindReprojectB;
    }
    else {
      scriptTilt = scriptTilt3dFindReprojectA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptTilt, tiltParam, "tilt", axisID, true,
      true);
  }

  /**
   * Load the specified tomopitch com script
   * @param axisID the AxisID to load.
   */
  public void loadTomopitch(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptTomopitchB =
        ComScriptUtil.loadComScript(appManager, "tomopitch", axisID, true, false, false);
    }
    else {
      scriptTomopitchA =
        ComScriptUtil.loadComScript(appManager, "tomopitch", axisID, true, false, false);
    }
  }

  /**
   * Get the tomopitch parameters from the specified tomopitch script object
   * @param axisID the AxisID to read.
   * @return a TomopitchParam object that will be created and initialized
   * with the input arguments from tomopitch in the com script.
   */
  public TomopitchParam getTomopitchParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript tomopitch;
    if (axisID == AxisID.SECOND) {
      tomopitch = scriptTomopitchB;
    }
    else {
      tomopitch = scriptTomopitchA;
    }

    // Initialize a TomopitchParam object from the com script command object
    TomopitchParam tomopitchParam = new TomopitchParam(appManager, axisID);
    ComScriptUtil.initialize(appManager, tomopitchParam, tomopitch, "tomopitch", axisID,
      false, false, true);
    return tomopitchParam;
  }

  /**
   * Save the specified tomopitch com script updating the tomopitch parameters
   * @param axisID the AxisID to load.
   * @param tomopitchParam a TomopitchParam object that will be used to update
   * tomopitch command in the tomopitch com script
   */
  public void saveTomopitch(final TomopitchParam tomopitchParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptTomopitch;
    if (axisID == AxisID.SECOND) {
      scriptTomopitch = scriptTomopitchB;
    }
    else {
      scriptTomopitch = scriptTomopitchA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptTomopitch, tomopitchParam, "tomopitch",
      axisID, false, false);
  }

  public void loadMTFFilter(final AxisID axisID) {
    // Assign the new ComScriptObject object to the appropriate reference
    if (axisID == AxisID.SECOND) {
      scriptMTFFilterB = ComScriptUtil.loadComScript(appManager, MTFFILTER_COMMAND,
        axisID, false, false, false);
    }
    else {
      scriptMTFFilterA = ComScriptUtil.loadComScript(appManager, MTFFILTER_COMMAND,
        axisID, false, false, false);
    }
  }

  public CtfPhaseFlipParam getCtfPhaseFlipParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript ctfPhaseflip;
    if (axisID == AxisID.SECOND) {
      ctfPhaseflip = scriptCtfCorrectionB;
    }
    else {
      ctfPhaseflip = scriptCtfCorrectionA;
    }

    // Initialize from the com script command object
    CtfPhaseFlipParam ctfPhaseFlipParam = new CtfPhaseFlipParam(appManager, axisID);
    ComScriptUtil.initialize(appManager, ctfPhaseFlipParam, ctfPhaseflip,
      CtfPhaseFlipParam.COMMAND, axisID, false, false, true);
    return ctfPhaseFlipParam;
  }

  public CtfPlotterParam getCtfPlotterParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript ctfPlotter;
    if (axisID == AxisID.SECOND) {
      ctfPlotter = scriptCtfPlotterB;
    }
    else {
      ctfPlotter = scriptCtfPlotterA;
    }

    // Initialize from the com script command object
    CtfPlotterParam ctfPlotterParam = new CtfPlotterParam();
    ComScriptUtil.initialize(appManager, ctfPlotterParam, ctfPlotter,
      CtfPlotterParam.COMMAND, axisID, false, false, true);
    return ctfPlotterParam;
  }

  public MTFFilterParam getMTFFilterParam(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript mtfFilter;
    if (axisID == AxisID.SECOND) {
      mtfFilter = scriptMTFFilterB;
    }
    else {
      mtfFilter = scriptMTFFilterA;
    }

    // Initialize a TiltParam object from the com script command object
    MTFFilterParam mtfFilterParam = new MTFFilterParam(appManager, axisID);
    ComScriptUtil.initialize(appManager, mtfFilterParam, mtfFilter, MTFFILTER_COMMAND,
      axisID, false, false, true);
    return mtfFilterParam;
  }

  public void saveMTFFilter(final MTFFilterParam mtfFilterParam, final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript scriptMTFFilter;
    if (axisID == AxisID.SECOND) {
      scriptMTFFilter = scriptMTFFilterB;
    }
    else {
      scriptMTFFilter = scriptMTFFilterA;
    }
    ComScriptUtil.modifyCommand(appManager, scriptMTFFilter, mtfFilterParam,
      MTFFILTER_COMMAND, axisID, false, false);
  }

  /**
   * Load in the solvematch com script.
   */
  public void loadSolvematch() {
    scriptSolvematch = ComScriptUtil.loadComScript(appManager, "solvematch", AxisID.ONLY,
      true, false, false);
  }

  public void loadDualvolmatch() {
    scriptDualvolmatch = ComScriptUtil.loadComScript(appManager,
      ProcessName.DUALVOLMATCH.toString(), AxisID.ONLY, true, false, false);
  }

  /**
   * Return the solvematch parameter object from the currently loaded 
   * solvematch com script.
   * @return
   */
  public SolvematchParam getSolvematch() {
    // Initialize a SolvematchParam object from the com script command
    // object
    SolvematchParam solveMatchParam = new SolvematchParam(appManager);
    ComScriptUtil.initialize(appManager, solveMatchParam, scriptSolvematch, "solvematch",
      AxisID.ONLY, false, false, true);
    return solveMatchParam;
  }

  public DualvolmatchParam getDualvolmatch() {
    DualvolmatchParam param = new DualvolmatchParam();
    ComScriptUtil.initialize(appManager, param, scriptDualvolmatch,
      ProcessName.DUALVOLMATCH.toString(), AxisID.ONLY, false, false, true);
    return param;
  }

  /**
   * Replace the solvematch command in the solvematch com script with the info
   * in the specified SolvematchParam object
   * @param solveMatchParam
   */
  public void saveSolvematch(final SolvematchParam solveMatchParam) {
    ComScriptUtil.modifyCommand(appManager, scriptSolvematch, solveMatchParam,
      "solvematch", AxisID.ONLY, false, false);
  }

  public void saveDualvolmatch(final DualvolmatchParam param) {
    ComScriptUtil.modifyCommand(appManager, scriptDualvolmatch, param,
      ProcessName.DUALVOLMATCH.toString(), AxisID.ONLY, false, false);
  }

  /**
   * Save the matchshifts command to the solvematch com script
   * @param matchshiftsParam
   */
  public void saveSolvematch(final MatchshiftsParam matchshiftsParam) {
    ComScriptUtil.modifyCommand(appManager, scriptSolvematch, matchshiftsParam,
      matchshiftsParam.getCommand(), AxisID.ONLY, true, false, false, false, null);
  }

  /**
   * Load the solvematchshift com script
   */
  public void loadSolvematchshift() {
    scriptSolvematchshift = ComScriptUtil.loadComScript(appManager, "solvematchshift",
      AxisID.ONLY, true, false, false);
  }

  /**
   * Parse the solvematch command from the solvematchshift script
   * @return MatchorwarpParam
   */
  public SolvematchshiftParam getSolvematchshift() {
    // Initialize a SolvematchshiftParam object from the com script command
    // object
    SolvematchshiftParam solveMatchshiftParam = new SolvematchshiftParam();
    ComScriptUtil.initialize(appManager, solveMatchshiftParam, scriptSolvematchshift,
      "solvematch", AxisID.ONLY, false, false, true);
    return solveMatchshiftParam;
  }

  public MatchshiftsParam getMatchshiftsFromSolvematchshifts() {
    // Initialize a MatchshiftsParam object from the com script command
    // object
    MatchshiftsParam matchshiftsParam = new MatchshiftsParam();
    ComScriptUtil.initialize(appManager, matchshiftsParam, scriptSolvematchshift,
      "matchshifts", AxisID.ONLY, false, false, true, true, null);
    return matchshiftsParam;
  }

  /**
   * Get the blendmont command from xcorr.com
   * @param axisID
   * @return
   */
  public BlendmontParam getBlendmontParamFromTiltxcorr(final AxisID axisID) {
    // Get a reference to the appropriate script object
    ComScript xcorr;
    if (axisID == AxisID.SECOND) {
      xcorr = scriptXcorrB;
    }
    else {
      xcorr = scriptXcorrA;
    }

    // Initialize a TiltxcorrParam object from the com script command object
    BlendmontParam blendmontParam =
      new BlendmontParam(appManager, appManager.getMetaData().getDatasetName(), axisID);
    ComScriptUtil.initialize(appManager, blendmontParam, xcorr,
      BlendmontParam.COMMAND_NAME, axisID, false, false, true);
    return blendmontParam;
  }

  /**
   * Get the first goto command from xcorr.com
   * @param axisID
   * @return
   */
  public GotoParam getGotoParamFromTiltxcorr(final AxisID axisID) {
    ComScript xcorr;
    if (axisID == AxisID.SECOND) {
      xcorr = scriptXcorrB;
    }
    else {
      xcorr = scriptXcorrA;
    }
    // Initialize a GotoParam object from the com script command
    // object
    GotoParam gotoParam = new GotoParam();
    if (!ComScriptUtil.initialize(appManager, gotoParam, xcorr, GotoParam.COMMAND_NAME,
      axisID, false, false, true)) {
      return null;
    }
    return gotoParam;
  }

  /**
   * Save the solvematchshift com script updating the solveMatchshiftParam
   * parameters
   * @param solveMatchshiftParam
   */
  public void saveSolvematchshift(final SolvematchshiftParam solveMatchshiftParam) {
    Exception except = new Exception();
    except.printStackTrace();
    System.err.println("WARNING: call to saveSolvematchshift");
    ComScriptUtil.modifyCommand(appManager, scriptSolvematchshift, solveMatchshiftParam,
      "solvematch", AxisID.ONLY, false, false, true, true, null);
  }

  /**
   * Load the solvematchmod com script
   */
  public void loadSolvematchmod() {
    scriptSolvematchmod = ComScriptUtil.loadComScript(appManager, "solvematchmod",
      AxisID.ONLY, true, false, false);
  }

  /**
   * Parse the solvematch command from the solvematchmod script
   * @return MatchorwarpParam
   */
  public SolvematchmodParam getSolvematchmod() {
    // Initialize a SolvematchmodParam object from the com script command
    // object
    SolvematchmodParam solveMatchmodParam = new SolvematchmodParam();
    ComScriptUtil.initialize(appManager, solveMatchmodParam, scriptSolvematchmod,
      "solvematch", AxisID.ONLY, false, false, true);
    return solveMatchmodParam;
  }

  /**
   * Save the solvematchmod com script updating the solveMatchmodParam
   * parameters
   * @param solveMatchmodParam
   */
  public void saveSolvematchmod(final SolvematchmodParam solveMatchmodParam) {
    Exception except = new Exception();
    except.printStackTrace();
    System.err.println("WARNING: call to saveSolvematchshift");
    ComScriptUtil.modifyCommand(appManager, scriptSolvematchmod, solveMatchmodParam,
      "solvematch", AxisID.ONLY, false, false);
  }

  /**
   * Load the patchcorr com script
   */
  public void loadPatchcorr() {
    scriptPatchcorr = ComScriptUtil.loadComScript(appManager, "patchcorr", AxisID.ONLY,
      true, false, false);
  }

  /**
   * Parse the patchrawl3D command from the patchcorr script
   * @return MatchorwarpParam
   */
  public Patchcrawl3DParam getPatchcrawl3D() {
    // Initialize a Patchcrawl3DParam object from the com script command object
    Patchcrawl3DParam patchcrawl3DParam = new Patchcrawl3DParam(appManager);
    if (!ComScriptUtil.initialize(appManager, patchcrawl3DParam, scriptPatchcorr,
      Patchcrawl3DParam.COMMAND, AxisID.ONLY, true, false, false, true, null)) {
      ComScriptUtil.initialize(appManager, patchcrawl3DParam, scriptPatchcorr,
        Patchcrawl3DPrePIPParam.COMMAND, AxisID.ONLY, false, false, true);
    }
    return patchcrawl3DParam;
  }

  /**
   * Save the patchcorr com script updating the patchcrawl3d parameters
   * @param patchcrawl3DParam
   */
  public void savePatchcorr(final Patchcrawl3DParam patchcrawl3DParam) {
    if (!ComScriptUtil.modifyOptionalCommand(appManager, scriptPatchcorr,
      patchcrawl3DParam, Patchcrawl3DParam.COMMAND, AxisID.ONLY, false, false)) {
      ComScriptUtil.deleteCommand(appManager, scriptPatchcorr,
        Patchcrawl3DPrePIPParam.COMMAND, AxisID.ONLY, false, false);
      ComScriptUtil.addModifyCommand(appManager, scriptPatchcorr, patchcrawl3DParam,
        Patchcrawl3DParam.COMMAND, AxisID.ONLY, -1, false, false, false);
    }
  }

  /**
   * Load the matchvol1 com script
   */
  public void loadMatchvol1() {
    scriptMatchvol1 = ComScriptUtil.loadComScript(appManager, MatchvolParam.COMMAND + '1',
      AxisID.ONLY, true, false, false);
  }

  /**
   * Parse the matchvol command from the matchvol1 script
   * @return MatchvolParam
   */
  public MatchvolParam getMatchvolParam() {
    // Initialize a MatchvolParam object from the com script command object
    MatchvolParam param = new MatchvolParam();
    ComScriptUtil.initialize(appManager, param, scriptMatchvol1, MatchvolParam.COMMAND,
      AxisID.ONLY, false, false, true);
    return param;
  }

  /**
   * Save the matchvol1 com script updating the matchvol parameters
   * @param matchvolParam
   */
  public void saveMatchvol(final MatchvolParam matchvolParam) {
    ComScriptUtil.modifyCommand(appManager, scriptMatchvol1, matchvolParam,
      MatchvolParam.COMMAND, AxisID.ONLY, false, false);
  }

  /**
   * Load the matchorwarp com script
   */
  public void loadMatchorwarp() {
    scriptMatchorwarp = ComScriptUtil.loadComScript(appManager, "matchorwarp",
      AxisID.ONLY, true, false, false);
  }

  /**
   * Parse the matchorwarp command from the matchorwarp script
   * @return MatchorwarpParam
   */
  public MatchorwarpParam getMatchorwarParam() {
    // Initialize a MatchorwarpParam object from the com script command object
    MatchorwarpParam matchorwarpParam = new MatchorwarpParam();
    ComScriptUtil.initialize(appManager, matchorwarpParam, scriptMatchorwarp,
      "matchorwarp", AxisID.ONLY, false, false, true);
    return matchorwarpParam;
  }

  /**
   * Save the matchorwarp com script updating the matchorwarp parameters
   * @param matchorwarpParam
   */
  public void saveMatchorwarp(final MatchorwarpParam matchorwarpParam) {
    ComScriptUtil.modifyCommand(appManager, scriptMatchorwarp, matchorwarpParam,
      "matchorwarp", AxisID.ONLY, false, false);
  }

  /**
   * 
   * @return
   */
  public CombineComscriptState getCombineComscript(final boolean initialVolumeMatching) {
    // Initialize a CombineComscript object from the com script command object
    CombineComscriptState combineComscriptState =
      new CombineComscriptState(initialVolumeMatching);
    if (!combineComscriptState.initialize(this)) {
      return null;
    }
    return combineComscriptState;
  }

  /**
   * Load the combine com script
   */
  public void loadCombine() {
    scriptCombine = ComScriptUtil.loadComScript(appManager,
      CombineComscriptState.COMSCRIPT_NAME, AxisID.ONLY, true, false, false);
  }

  public void loadVolcombine() {
    scriptVolcombine = ComScriptUtil.loadComScript(appManager, "volcombine", AxisID.ONLY,
      true, false, false);
  }

  /**
   * 
   * @param gotoParam
   */
  public void saveCombine(final GotoParam gotoParam) {
    ComScriptUtil.modifyCommand(appManager, scriptCombine, gotoParam,
      GotoParam.COMMAND_NAME, AxisID.ONLY, false, false);
  }

  /**
   * returns index of saved command
   * @param echoParam
   * @param previousCommand
   */
  public int saveCombine(final EchoParam echoParam, final String previousCommand) {
    return ComScriptUtil.addModifyCommand(appManager, scriptCombine, echoParam,
      EchoParam.COMMAND_NAME, AxisID.ONLY, previousCommand, false, false);
  }

  /**
   * 
   * @param gotoParam
   */
  public void saveCombine(final ExitParam exitParam, final int previousCommandIndex) {
    ComScriptUtil.addModifyCommand(appManager, scriptCombine, exitParam,
      ExitParam.COMMAND_NAME, AxisID.ONLY, previousCommandIndex, false, false, false);
  }

  /**
   * 
   * @param gotoParam
   * @param previousCommandIndex
   */
  public void saveCombine(final GotoParam gotoParam, final int previousCommandIndex) {
    ComScriptUtil.addModifyCommand(appManager, scriptCombine, gotoParam,
      GotoParam.COMMAND_NAME, AxisID.ONLY, previousCommandIndex, false, false, false);
  }

  public void saveVolcombine(final SetParam setParam) {
    ComScriptUtil.modifyCommand(appManager, scriptVolcombine, setParam,
      SetParam.COMMAND_NAME, AxisID.ONLY, false, false);
  }

  public void saveVolcombine(final SetParam setParam, final String previousCommand) {
    ComScriptUtil.modifyCommand(appManager, scriptVolcombine, setParam,
      SetParam.COMMAND_NAME, AxisID.ONLY, false, false, previousCommand, false, false);
  }

  /**
   * returns index of saved command
   * @param echoParam
   * @param previousCommand
   */
  public void deleteFromCombine(final String command, final String previousCommand) {
    ComScriptUtil.deleteCommand(appManager, scriptCombine, command, AxisID.ONLY,
      previousCommand, false, false);
  }

  /**
   * 
   * @return
   */
  public GotoParam getGotoParamFromCombine() {
    // Initialize a GotoParam object from the com script command
    // object
    GotoParam gotoParam = new GotoParam();
    if (!ComScriptUtil.initialize(appManager, gotoParam, scriptCombine,
      GotoParam.COMMAND_NAME, AxisID.ONLY, true, false, false, true, null)) {
      return null;
    }
    return gotoParam;
  }

  public SetParam getSetParamFromVolcombine(final String name,
    final EtomoNumber.Type type) {
    SetParam setParam = new SetParam(name, type);
    if (!ComScriptUtil.initialize(appManager, setParam, scriptVolcombine,
      SetParam.COMMAND_NAME, AxisID.ONLY, true, false, false, true, null)) {
      return null;
    }
    return setParam;
  }

  public SetParam getSetParamFromVolcombine(final String name,
    final EtomoNumber.Type type, final String previousCommand) {
    SetParam setParam = new SetParam(name, type);
    if (!ComScriptUtil.initialize(appManager, setParam, scriptVolcombine,
      SetParam.COMMAND_NAME, AxisID.ONLY, true, previousCommand, true, false, false,
      true)) {
      return null;
    }
    return setParam;
  }

  /**
   * 
   * @param previousCommand
   * @return
   */
  public EchoParam getEchoParamFromCombine(final String previousCommand) {
    // Initialize an EchoParam object from a location after previousCommand
    // in the com script command
    // object
    EchoParam echoParam = new EchoParam();
    if (!ComScriptUtil.initialize(appManager, echoParam, scriptCombine,
      EchoParam.COMMAND_NAME, AxisID.ONLY, false, previousCommand, false, false, false,
      true)) {
      return null;
    }
    return echoParam;
  }

  public boolean isDualvolmatchLabelInCombine() {
    if (scriptCombine == null) {
      loadCombine();
    }
    if (scriptCombine == null) {
      return false;
    }
    LabelParam param = new LabelParam(ProcessName.DUALVOLMATCH);
    return ComScriptUtil.initialize(appManager, param, scriptCombine, param.getLabel(),
      AxisID.ONLY, false, false, false);
  }

  public void loadFlatten(final AxisID axisID) {
    scriptFlatten = ComScriptUtil.loadComScript(appManager,
      ProcessName.FLATTEN.toString(), axisID, true, false, false);
  }

  public boolean isWarpVolParamInFlatten(final AxisID axisID) {
    return ComScriptUtil.loadComScript(appManager, ProcessName.FLATTEN.toString(), axisID,
      true, false, false).isCommandLoaded();
  }

  public WarpVolParam getWarpVolParamFromFlatten(final AxisID axisID) {
    // Initialize a WarpVolParam object from the com script command
    // object
    WarpVolParam param =
      new WarpVolParam(appManager, axisID, WarpVolParam.Mode.POST_PROCESSING);
    ComScriptUtil.initialize(appManager, param, scriptFlatten, WarpVolParam.COMMAND,
      axisID, false, false, true);
    return param;
  }

  /**
   * Save the WarpVolParam command to the flatten com script
   * @param warpVolParam
   */
  public void saveFlatten(final WarpVolParam param, final AxisID axisID) {
    ComScriptUtil.modifyCommand(appManager, scriptFlatten, param, WarpVolParam.COMMAND,
      axisID, true, false);
  }

  /**
   * Examine the com script to see whether it contains newst or newstack
   * commands.
   * @param comScript  The com script object to examine
   * @return The 
   */
  private String newstOrNewstack(final ComScript comScript) {
    if (comScript == null) {
      return "";
    }
    String[] commands = comScript.getCommandArray();
    for (int i = 0; i < commands.length; i++) {
      if (commands[i].equals("newst")) {
        return "newst";
      }
      if (commands[i].equals("newstack")) {
        return "newstack";
      }
    }
    return "";
  }
}
