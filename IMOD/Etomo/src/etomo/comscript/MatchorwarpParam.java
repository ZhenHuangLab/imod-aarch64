package etomo.comscript;

import etomo.type.EtomoBoolean2;
import etomo.type.EtomoNumber;
import etomo.type.ScriptParameter;
import etomo.type.StringParameter;
import etomo.util.DatasetFiles;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2002 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 3.4  2010/04/28 15:59:50  sueh
 * <p> bug# 1344 Reformatted.
 * <p>
 * <p> Revision 3.3  2006/09/19 21:59:52  sueh
 * <p> bug# 928 Added residualFile, vectormodel, and clipsize.
 * <p>
 * <p> Revision 3.2  2004/04/12 16:49:27  sueh
 * <p> bug# 409 changed interface class CommandParam
 * <p>
 * <p> Revision 3.1  2004/03/06 03:46:55  sueh
 * <p> bug# 380 added useLinearInterpolation
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:00  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.5  2003/07/25 22:54:14  rickg
 * <p> CommandParam method name changes
 * <p>
 * <p> Revision 2.4  2003/06/25 22:16:29  rickg
 * <p> changed name of com script parse method to parseComScript
 * <p>
 * <p> Revision 2.3  2003/03/20 17:23:23  rickg
 * <p> Comment update
 * <p>
 * <p> Revision 2.2  2003/03/06 05:53:28  rickg
 * <p> Combine interface in progress
 * <p>
 * <p> Revision 2.1  2003/03/02 23:30:41  rickg
 * <p> Combine layout in progress
 * <p> </p>
 */

public final class MatchorwarpParam implements ConstMatchorwarpParam, CommandParam {
  private final StringParameter sizeXYZorVolume = new StringParameter("SizeXYZorVolume");
  private final ScriptParameter refineLimit = new ScriptParameter(
    EtomoNumber.Type.DOUBLE, "RefineLimit");
  private final StringParameter residualFile = new StringParameter("ResidualFile");
  private final StringParameter vectorModel = new StringParameter("VectorModel");
  private final ScriptParameter clipPlaneBoxSize =
    new ScriptParameter("ClipPlaneBoxSize");
  private final StringParameter warpLimits = new StringParameter("WarpLimits");
  private StringParameter modelFile = new StringParameter("ModelFile");
  private StringParameter patchFile = new StringParameter("PatchFile");
  private StringParameter solveFile = new StringParameter("SolveFile");
  private StringParameter refineFile = new StringParameter("RefineFile");
  private StringParameter inverseFile = new StringParameter("InverseFile");
  private StringParameter warpFile = new StringParameter("WarpFile");
  private StringParameter temporaryDirectory = new StringParameter("TemporaryDirectory");
  private final ScriptParameter xLowerExclude = new ScriptParameter("XLowerExclude");
  private final ScriptParameter xUpperExclude = new ScriptParameter("XUpperExclude");
  private final ScriptParameter zLowerExclude = new ScriptParameter("ZLowerExclude");
  private final ScriptParameter zUpperExclude = new ScriptParameter("ZUpperExclude");
  private final EtomoBoolean2 trialMode = new EtomoBoolean2("TrialMode");
  private final StringParameter inputVolume = new StringParameter("InputVolume");
  private final StringParameter outputVolume = new StringParameter("OutputVolume");
  private final EtomoBoolean2 linearInterpolation = new EtomoBoolean2(
    "LinearInterpolation");
  private final StringParameter structureCriteria = new StringParameter(
    "StructureCriteria");
  private final StringParameter extentToFit = new StringParameter("ExtentToFit");

  MatchorwarpParam() {}

  public void parseComScriptCommand(final ComScriptCommand scriptCommand)
    throws BadComScriptException, FortranInputSyntaxException, InvalidParameterException {
    initializeDefaults();
    if (!scriptCommand.isKeywordValuePairs()) {
      parseComScriptCommandForBackwardsCompatibility(scriptCommand);
    }
    else {
      sizeXYZorVolume.parse(scriptCommand);
      refineLimit.parse(scriptCommand);
      residualFile.parse(scriptCommand);
      vectorModel.parse(scriptCommand);
      warpLimits.parse(scriptCommand);
      modelFile.parse(scriptCommand);
      patchFile.parse(scriptCommand);
      solveFile.parse(scriptCommand);
      refineFile.parse(scriptCommand);
      inverseFile.parse(scriptCommand);
      warpFile.parse(scriptCommand);
      temporaryDirectory.parse(scriptCommand);
      xLowerExclude.parse(scriptCommand);
      xUpperExclude.parse(scriptCommand);
      zLowerExclude.parse(scriptCommand);
      zUpperExclude.parse(scriptCommand);
      trialMode.parse(scriptCommand);
      inputVolume.parse(scriptCommand);
      outputVolume.parse(scriptCommand);
      linearInterpolation.parse(scriptCommand);
      structureCriteria.parse(scriptCommand);
      extentToFit.parse(scriptCommand);
    }
  }

  public void updateComScriptCommand(final ComScriptCommand scriptCommand)
    throws BadComScriptException {
    if (vectorModel.isEmpty()) {
      vectorModel.set(DatasetFiles.PATCH_VECTOR_MODEL);
    }
    scriptCommand.useKeywordValue();
    sizeXYZorVolume.updateComScript(scriptCommand);
    refineLimit.updateComScript(scriptCommand);
    residualFile.updateComScript(scriptCommand);
    vectorModel.updateComScript(scriptCommand);
    warpLimits.updateComScript(scriptCommand);
    modelFile.updateComScript(scriptCommand);
    patchFile.updateComScript(scriptCommand);
    solveFile.updateComScript(scriptCommand);
    refineFile.updateComScript(scriptCommand);
    inverseFile.updateComScript(scriptCommand);
    warpFile.updateComScript(scriptCommand);
    temporaryDirectory.updateComScript(scriptCommand);
    xLowerExclude.updateComScript(scriptCommand);
    xUpperExclude.updateComScript(scriptCommand);
    zLowerExclude.updateComScript(scriptCommand);
    zUpperExclude.updateComScript(scriptCommand);
    trialMode.updateComScript(scriptCommand);
    inputVolume.updateComScript(scriptCommand);
    outputVolume.updateComScript(scriptCommand);
    linearInterpolation.updateComScript(scriptCommand);
    structureCriteria.updateComScript(scriptCommand);
    extentToFit.updateComScript(scriptCommand);
  }

  /**
   * Reset the state of the object to it initial defaults
   */
  public void initializeDefaults() {
    sizeXYZorVolume.reset();
    refineLimit.reset();
    residualFile.reset();
    clipPlaneBoxSize.reset();
    warpLimits.reset();
    modelFile.reset();
    patchFile.reset();
    solveFile.reset();
    refineFile.reset();
    inverseFile.reset();
    warpFile.reset();
    temporaryDirectory.reset();
    xLowerExclude.reset();
    xUpperExclude.reset();
    zLowerExclude.reset();
    zUpperExclude.reset();
    trialMode.reset();
    inputVolume.reset();
    outputVolume.reset();
    linearInterpolation.reset();
    structureCriteria.reset();
    extentToFit.reset();
  }

  /**
   * @deprecated
   * @param scriptCommand
   * @throws BadComScriptException
   * @throws FortranInputSyntaxException
   * @throws InvalidParameterException
   */
  public void parseComScriptCommandForBackwardsCompatibility(
    final ComScriptCommand scriptCommand) throws BadComScriptException,
    FortranInputSyntaxException, InvalidParameterException {
    // TODO error checking - throw exceptions for bad syntax
    String[] cmdLineArgs = scriptCommand.getCommandLineArgs();

    for (int i = 0; i < cmdLineArgs.length - 2; i++) {
      if (cmdLineArgs[i].startsWith("-siz")) {
        i++;
        sizeXYZorVolume.set(cmdLineArgs[i]);
      }

      if (cmdLineArgs[i].startsWith("-refinel")) {
        i++;
        refineLimit.set(cmdLineArgs[i]);
      }
      if (cmdLineArgs[i].startsWith("-res")) {
        i++;
        residualFile.set(cmdLineArgs[i]);
      }
      if (cmdLineArgs[i].startsWith("-vec")) {
        i++;
        vectorModel.set(cmdLineArgs[i]);
      }
      if (cmdLineArgs[i].startsWith("-cli")) {
        i++;
        clipPlaneBoxSize.set(cmdLineArgs[i]);
      }
      if (cmdLineArgs[i].startsWith("-warpl")) {
        i++;
        warpLimits.set(cmdLineArgs[i]);
      }
      if (cmdLineArgs[i].startsWith("-mod")) {
        i++;
        modelFile.set(cmdLineArgs[i]);
      }

      if (cmdLineArgs[i].startsWith("-pat")) {
        i++;
        patchFile.set(cmdLineArgs[i]);
      }

      if (cmdLineArgs[i].startsWith("-sol")) {
        i++;
        solveFile.set(cmdLineArgs[i]);
      }

      if (cmdLineArgs[i].startsWith("-refinef")) {
        i++;
        refineFile.set(cmdLineArgs[i]);
      }

      if (cmdLineArgs[i].startsWith("-inv")) {
        i++;
        inverseFile.set(cmdLineArgs[i]);
      }

      if (cmdLineArgs[i].startsWith("-warpf")) {
        i++;
        warpFile.set(cmdLineArgs[i]);
      }

      if (cmdLineArgs[i].startsWith("-tem")) {
        i++;
        temporaryDirectory.set(cmdLineArgs[i]);
      }

      if (cmdLineArgs[i].startsWith("-xlo")) {
        i++;
        xLowerExclude.set(cmdLineArgs[i]);
      }

      if (cmdLineArgs[i].startsWith("-xup")) {
        i++;
        xUpperExclude.set(cmdLineArgs[i]);
      }

      if (cmdLineArgs[i].startsWith("-zlo")) {
        i++;
        zLowerExclude.set(cmdLineArgs[i]);
      }

      if (cmdLineArgs[i].startsWith("-zup")) {
        i++;
        zUpperExclude.set(cmdLineArgs[i]);
      }

      if (cmdLineArgs[i].startsWith("-lin")) {
        i++;
        linearInterpolation.set(true);
      }

      if (cmdLineArgs[i].startsWith("-tri")) {
        trialMode.set(true);
      }

      if (cmdLineArgs[i].startsWith("-str")) {
        i++;
        structureCriteria.set(cmdLineArgs[i]);
      }

      if (cmdLineArgs[i].startsWith("-ext")) {
        i++;
        extentToFit.set(cmdLineArgs[i]);
      }
    }

    inputVolume.set(cmdLineArgs[cmdLineArgs.length - 2]);
    outputVolume.set(cmdLineArgs[cmdLineArgs.length - 1]);

    // Backwards compatibility (before 3.8.25):
    // Update so that the user can look at the .resid file.
    if (residualFile.isEmpty() && vectorModel.isEmpty() && clipPlaneBoxSize.isNull()) {
      residualFile.set("patch.resid");
      vectorModel.set(DatasetFiles.PATCH_VECTOR_MODEL);
      clipPlaneBoxSize.set(600);
    }
  }

  /**
   * Sets the modelFile.
   * @param modelFile The modelFile to set
   */
  public void setModelFile(final String input) {
    modelFile.set(input);
  }

  /**
   * Set the default patch region model file
   * @param patchFile
   */
  public void setDefaultModelFile() {
    modelFile.set(getDefaultPatchRegionModel());
  }

  /**
   * Sets the refineLimit.
   * @param refineLimit The refineLimit to set
   */
  public void setRefineLimit(final String input) {
    refineLimit.set(input);
  }

  /**
   * Sets the warpLimit.
   * @param warpLimit The warpLimit to set
   */
  public void setWarpLimits(final String input) {
    warpLimits.set(input);
  }

  /**
   * Sets the xLowerExclude.
   * @param xLowerExclude The xLowerExclude to set
   */
  public void setXLowerExclude(final String input) {
    xLowerExclude.set(input);
  }

  public void setXLowerExclude(final int input) {
    xLowerExclude.set(input);
  }

  public void resetXLowerExclude() {
    xLowerExclude.reset();
  }

  /**
   * Sets the xUpperExclude.
   * @param xUpperExclude The xUpperExclude to set
   */
  public void setXUpperExclude(final String input) {
    xUpperExclude.set(input);
  }

  public void setXUpperExclude(final int input) {
    xUpperExclude.set(input);
  }

  public void resetXUpperExclude() {
    xUpperExclude.reset();
  }

  /**
   * Sets the zLowerExclude.
   * @param zLowerExclude The zLowerExclude to set
   */
  public void setZLowerExclude(final String input) {
    zLowerExclude.set(input);
  }

  public void setZLowerExclude(final int input) {
    zLowerExclude.set(input);
  }

  public void resetZLowerExclude() {
    zLowerExclude.reset();
  }

  /**
   * Sets the zUpperExclude.
   * @param zUpperExclude The zUpperExclude to set
   */
  public void setZUpperExclude(final String input) {
    zUpperExclude.set(input);
  }

  public void setZUpperExclude(final int input) {
    zUpperExclude.set(input);
  }

  public void resetZUpperExclude() {
    zUpperExclude.reset();
  }

  /**
   * Sets the trial flag.
   * @param trial Specify the trial state
   */
  public void setTrialMode(final boolean input) {
    trialMode.set(input);
  }

  public void setLinearInterpolation(final boolean input) {
    this.linearInterpolation.set(input);
  }

  public boolean isUseModelFile() {
    return !modelFile.isEmpty();
  }

  public String getRefineLimit() {
    return refineLimit.toString();
  }

  /**
   * @return String
   */
  public String getWarpLimits() {
    return warpLimits.toString();
  }

  /**
   * @return int
   */
  public int getXLowerExclude() {
    return xLowerExclude.getInt();
  }

  public boolean isXLowerExcludeSet() {
    return !xLowerExclude.isNull();
  }

  /**
   * @return int
   */
  public int getXUpperExclude() {
    return xUpperExclude.getInt();
  }

  public boolean isXUpperExcludeSet() {
    return !xUpperExclude.isNull();
  }

  /**
   * @return int
   */
  public int getZLowerExclude() {
    return zLowerExclude.getInt();
  }

  public boolean isZLowerExcludeSet() {
    return !zLowerExclude.isNull();
  }

  /**
   * @return int
   */
  public int getZUpperExclude() {
    return zUpperExclude.getInt();
  }

  public boolean isZUpperExcludeSet() {
    return !zUpperExclude.isNull();
  }

  public boolean isLinearInterpolation() {
    return linearInterpolation.is();
  }

  /**
   * Return the default patch region model file name
   * @return String
   */
  public static String getDefaultPatchRegionModel() {
    return "patch_region.mod";
  }
}