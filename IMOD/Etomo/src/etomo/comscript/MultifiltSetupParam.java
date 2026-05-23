package etomo.comscript;

import java.io.File;

import etomo.BaseManager;
import etomo.type.AxisID;
import etomo.type.EtomoNumber;
import etomo.type.FileKey;
import etomo.type.FileType;
import etomo.type.ProcessName;
import etomo.type.ScriptParameter;
import etomo.type.StringParameter;

public final class MultifiltSetupParam implements CommandParam, Command {
  private static final ProcessName PROCESS_NAME = ProcessName.MULTIFILT_SETUP;

  public static final String FAKE_SIRT_ITERATIONS = "FakeSIRTiterations";
  public static final String EXACT_OBJECT_SIZES = "ExactObjectSizes";
  public static final String GAUSSIAN_CUTOFFS = "GaussianCutoffs";
  public static final String GAUSSIAN_FALLOFFS = "GaussianFalloffs";
  public static final String HAMMING_LIKE_STARTS = "HammingLikeStarts";
  public static final String WIDTH_IN_X = "WidthInX";
  public static final String SHIFT_IN_X = "ShiftInX";
  public static final String SIZE_IN_Y = "SizeInY";
  public static final String SHIFT_IN_Y = "ShiftInY";
  public static final String THICKNESS_IN_Z = "ThicknessInZ";
  public static final String SHIFT_IN_DEPTH = "ShiftInDepth";

  private final StringParameter commandFile = new StringParameter("CommandFile");
  private final StringParameter fakeSIRTiterations =
    new StringParameter(FAKE_SIRT_ITERATIONS);
  private final StringParameter exactObjectSizes =
    new StringParameter(EXACT_OBJECT_SIZES);
  private final StringParameter gaussianCutoffs = new StringParameter(GAUSSIAN_CUTOFFS);
  private final StringParameter gaussianFalloffs = new StringParameter(GAUSSIAN_FALLOFFS);
  private final StringParameter hammingLikeStarts =
    new StringParameter(HAMMING_LIKE_STARTS);
  private final ScriptParameter widthInX =
    new ScriptParameter(EtomoNumber.Type.INTEGER, WIDTH_IN_X);
  private final ScriptParameter shiftInX =
    new ScriptParameter(EtomoNumber.Type.INTEGER, SHIFT_IN_X);
  private final ScriptParameter sizeInY =
    new ScriptParameter(EtomoNumber.Type.INTEGER, SIZE_IN_Y);
  private final ScriptParameter shiftInY =
    new ScriptParameter(EtomoNumber.Type.INTEGER, SHIFT_IN_Y);
  private final ScriptParameter thicknessInZ =
    new ScriptParameter(EtomoNumber.Type.INTEGER, THICKNESS_IN_Z);
  private final ScriptParameter shiftInDepth =
    new ScriptParameter(EtomoNumber.Type.INTEGER, SHIFT_IN_DEPTH);

  private final BaseManager manager;
  private final AxisID axisID;

  public MultifiltSetupParam(final BaseManager manager, final AxisID axisID) {
    this.manager = manager;
    this.axisID = axisID;
    commandFile
      .set(ProcessName.TILT.getText() + (axisID != null ? axisID.getExtension() : ""));
  }

  public void parseComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException, InvalidParameterException, FortranInputSyntaxException {
    scriptCommand.useKeywordValue();
    initializeDefaults();
    // commandFile is read-only
    fakeSIRTiterations.parse(scriptCommand);
    exactObjectSizes.parse(scriptCommand);
    gaussianCutoffs.parse(scriptCommand);
    gaussianFalloffs.parse(scriptCommand);
    hammingLikeStarts.parse(scriptCommand);
    widthInX.parse(scriptCommand);
    shiftInX.parse(scriptCommand);
    sizeInY.parse(scriptCommand);
    shiftInY.parse(scriptCommand);
    thicknessInZ.parse(scriptCommand);
    shiftInDepth.parse(scriptCommand);
  }

  public void updateComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException {
    scriptCommand.useKeywordValue();
    commandFile.updateComScript(scriptCommand);
    fakeSIRTiterations.updateComScript(scriptCommand);
    exactObjectSizes.updateComScript(scriptCommand);
    gaussianCutoffs.updateComScript(scriptCommand);
    gaussianFalloffs.updateComScript(scriptCommand);
    hammingLikeStarts.updateComScript(scriptCommand);
    widthInX.updateComScript(scriptCommand);
    shiftInX.updateComScript(scriptCommand);
    sizeInY.updateComScript(scriptCommand);
    shiftInY.updateComScript(scriptCommand);
    thicknessInZ.updateComScript(scriptCommand);
    shiftInDepth.updateComScript(scriptCommand);
  }

  public void initializeDefaults() {
    // commandFile is read-only
    fakeSIRTiterations.reset();
    exactObjectSizes.reset();
    gaussianCutoffs.reset();
    gaussianFalloffs.reset();
    hammingLikeStarts.reset();
    widthInX.reset();
    shiftInX.reset();
    sizeInY.reset();
    shiftInY.reset();
    thicknessInZ.reset();
    shiftInDepth.reset();
  }

  public boolean isFakeSIRTiterations() {
    return !fakeSIRTiterations.isEmpty();
  }

  public boolean isExactObjectSizes() {
    return !exactObjectSizes.isEmpty();
  }

  public boolean isGaussianCutoffs() {
    return !gaussianCutoffs.isEmpty();
  }

  public boolean isGaussianFalloffs() {
    return !gaussianFalloffs.isEmpty();
  }

  public boolean isHammingLikeStarts() {
    return !hammingLikeStarts.isEmpty();
  }

  public String getFakeSIRTiterations() {
    return fakeSIRTiterations.toString();
  }

  public String getExactObjectSizes() {
    return exactObjectSizes.toString();
  }

  public String getGaussianCutoffs() {
    return gaussianCutoffs.toString();
  }

  public String getGaussianFalloffs() {
    return gaussianFalloffs.toString();
  }

  public String getHammingLikeStarts() {
    return hammingLikeStarts.toString();
  }

  public String getWidthInX() {
    return widthInX.toString();
  }

  public String getShiftInX() {
    return shiftInX.toString();
  }

  public String getSizeInY() {
    return sizeInY.toString();
  }

  public String getShiftInY() {
    return shiftInY.toString();
  }

  public String getThicknessInZ() {
    return thicknessInZ.toString();
  }

  public String getShiftInDepth() {
    return shiftInDepth.toString();
  }

  public void setFakeSIRTiterations(final String input) {
    fakeSIRTiterations.set(input);
  }

  public void resetFakeSIRTiterations() {
    fakeSIRTiterations.reset();
  }

  public void setExactObjectSizes(final String input) {
    exactObjectSizes.set(input);
  }

  public void resetExactObjectSizes() {
    exactObjectSizes.reset();
  }

  public void setGaussianCutoffs(final String input) {
    gaussianCutoffs.set(input);
  }

  public void resetGaussianCutoffs() {
    gaussianCutoffs.reset();
  }

  public void setGaussianFalloffs(final String input) {
    gaussianFalloffs.set(input);
  }

  public void resetGaussianFalloffs() {
    gaussianFalloffs.reset();
  }

  public void setHammingLikeStarts(final String input) {
    hammingLikeStarts.set(input);
  }

  public void resetHammingLikeStarts() {
    hammingLikeStarts.reset();
  }

  public void setWidthInX(final String input) {
    widthInX.set(input);
  }

  public void setShiftInX(final String input) {
    shiftInX.set(input);
  }

  public void setSizeInY(final String input) {
    sizeInY.set(input);
  }

  public void setShiftInY(final String input) {
    shiftInY.set(input);
  }

  public void setThicknessInZ(final String input) {
    thicknessInZ.set(input);
  }

  public void setShiftInDepth(final String input) {
    shiftInDepth.set(input);
  }

  public AxisID getAxisID() {
    return axisID;
  }

  public CommandMode getCommandMode() {
    return null;
  }

  public ProcessName getProcessName() {
    return PROCESS_NAME;
  }

  public String getCommand() {
    return PROCESS_NAME.getComscript(axisID);
  }

  public String getCommandName() {
    return PROCESS_NAME.getText();
  }

  public String[] getCommandArray() {
    return PROCESS_NAME.getComscriptArray(axisID);
  }

  public File getCommandInputFile() {
    return FileType.TILT_COMSCRIPT.getFile(manager, axisID);
  }

  public String getCommandLine() {
    return getCommand();
  }

  /**
   * Returning null because there are multiple types of files.
   */
  public File getCommandOutputFile() {
    return null;
  }

  /**
   * @deprecated 3/15/2019
   */
  @SuppressWarnings("deprecation")
  public FileType getOutputImageFileType() {
    return null;
  }

  public FileKey getOutputImageFileKey() {
    return null;
  }

  /**
   * @deprecated 3/15/2019
   */
  @SuppressWarnings("deprecation")
  public FileType getOutputImageFileType2() {
    return null;
  }

  public FileKey getOutputImageFileKey2() {
    return null;
  }

  public CommandDetails getSubcommandDetails() {
    return null;
  }

  public String getSubcommandProcessName() {
    return null;
  }

  public boolean isMessageReporter() {
    return false;
  }
}
