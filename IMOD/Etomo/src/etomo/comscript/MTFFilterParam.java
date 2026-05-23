/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright(c) 2002 - 2018</p>
 * 
 * <p>Organization:
 * Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEM),
 * University of Colorado</p>
 * 
 * @author $$Author$$
 * 
 * @version $$Revision$$
 * 
 * <p> $$Log$
 * <p> $Revision 1.10  2011/02/22 03:17:56  sueh
 * <p> $bug# 1437 Reformatting.
 * <p> $
 * <p> $Revision 1.9  2010/04/28 16:01:59  sueh
 * <p> $bug# 1344 Implemented Command.  Added getOutputImageFileType
 * <p> $functions.  Made Const ancestor class an interface.
 * <p> $
 * <p> $Revision 1.8  2004/06/13 17:03:23  rickg
 * <p> $Solvematch mid change
 * <p> $
 * <p> $Revision 1.7  2004/06/09 21:18:12  rickg
 * <p> $Changed upateParameter method to updateScriptParameter
 * <p> $
 * <p> $Revision 1.6  2004/05/03 18:01:17  sueh
 * <p> $bug# 418 standardizing set function
 * <p> $
 * <p> $Revision 1.5  2004/04/16 01:50:33  sueh
 * <p> $bug# 409 added startingAndEndingZ, formatted
 * <p> $
 * <p> $Revision 1.4  2004/04/12 17:15:46  sueh
 * <p> $bug# 409  Change HighFrequencyRadiusSigma to LowPassRadiusSigma.  Add
 * <p> $initializeDefaults() to set the default comscript values.
 * <p> $
 * <p> $Revision 1.3  2004/03/29 20:51:46  sueh
 * <p> $bug# 409 add inputFile
 * <p> $
 * <p> $Revision 1.2  2004/03/25 00:47:18  sueh
 * <p> $bug# 409, bug# 418 added InverseRooloffRadiusSigma, moved utilities functions
 * <p> $to ParamUtilities
 * <p> $
 * <p> $Revision 1.1  2004/03/24 18:15:51  sueh
 * <p> $bug# 409 MTF filter params
 * <p> $$ </p>
 */

package etomo.comscript;

import java.io.File;

import etomo.BaseManager;
import etomo.type.AxisID;
import etomo.type.EnumeratedType;
import etomo.type.EtomoNumber;
import etomo.type.FileKey;
import etomo.type.FileType;
import etomo.type.ProcessName;
import etomo.type.ScriptParameter;
import etomo.type.StringParameter;

public final class MTFFilterParam implements ConstMTFFilterParam, CommandParam {
  public static final String rcsid =
    "$$Id$$";

  public static final String FIXED_IMAGE_DOSE_KEY = "FixedImageDose";
  public static final String DOSE_WEIGHTING_FILE_KEY = "DoseWeightingFile";
  public static final String TYPE_OF_DOSE_FILE_KEY = "TypeOfDoseFile";
  public static final int VOLTAGE_200 = 200;
  public static final String OPTIMAL_DOSE_SCALING_KEY = "OptimalDoseScaling";
  public static final String BIDIRECTIONAL_NUM_VIEWS_KEY = "BidirectionalNumViews";
  public static final String PIXEL_SIZE_OPTION = "PixelSize";

  private final FortranInputString lowPassRadiusSigma = new FortranInputString(2);
  private final FortranInputString inverseRolloffRadiusSigma = new FortranInputString(2);
  private final FortranInputString startingAndEndingZ = new FortranInputString(2);
  private final ScriptParameter fixedImageDose =
    new ScriptParameter(EtomoNumber.Type.DOUBLE, FIXED_IMAGE_DOSE_KEY);
  private final StringParameter doseWeightingFile =
    new StringParameter(DOSE_WEIGHTING_FILE_KEY);
  private final ScriptParameter typeOfDoseFile =
    new ScriptParameter(TYPE_OF_DOSE_FILE_KEY);
  private final ScriptParameter voltage = new ScriptParameter("Voltage");
  private final ScriptParameter optimalDoseScaling =
    new ScriptParameter(EtomoNumber.Type.DOUBLE, OPTIMAL_DOSE_SCALING_KEY);
  private final ScriptParameter bidirectionalNumViews =
    new ScriptParameter(EtomoNumber.Type.INTEGER, BIDIRECTIONAL_NUM_VIEWS_KEY);
  private final ScriptParameter pixelSize =
    new ScriptParameter(EtomoNumber.Type.DOUBLE, PIXEL_SIZE_OPTION);

  private final BaseManager manager;
  private final AxisID axisID;

  private String inputFile = "";
  private String outputFile = "";
  private String mtfFile = "";
  private double maximumInverse = Double.NaN;

  MTFFilterParam(final BaseManager manager, final AxisID axisID) {
    this.manager = manager;
    this.axisID = axisID;
    startingAndEndingZ.setIntegerType(0, true);
    startingAndEndingZ.setIntegerType(1, true);
    maximumInverse = Double.NaN;
  }

  public void parseComScriptCommand(ComScriptCommand scriptCommand)
    throws FortranInputSyntaxException, InvalidParameterException {
    String[] cmdLineArgs = scriptCommand.getCommandLineArgs();
    if (scriptCommand.isKeywordValuePairs()) {
      if (scriptCommand.hasKeyword("InputFile")) {
        inputFile = scriptCommand.getValue("InputFile");
      }
      if (scriptCommand.hasKeyword("OutputFile")) {
        outputFile = scriptCommand.getValue("OutputFile");
      }
      if (scriptCommand.hasKeyword("MtfFile")) {
        mtfFile = scriptCommand.getValue("MtfFile");
      }
      if (scriptCommand.hasKeyword("MaximumInverse")) {
        maximumInverse = Double.parseDouble(scriptCommand.getValue("MaximumInverse"));
      }
      if (scriptCommand.hasKeyword("LowPassRadiusSigma")) {
        lowPassRadiusSigma.validateAndSet(scriptCommand.getValue("LowPassRadiusSigma"));
      }
      if (scriptCommand.hasKeyword("InverseRolloffRadiusSigma")) {
        inverseRolloffRadiusSigma
          .validateAndSet(scriptCommand.getValue("InverseRolloffRadiusSigma"));
      }
      if (scriptCommand.hasKeyword("StartingAndEndingZ")) {
        startingAndEndingZ.validateAndSet(scriptCommand.getValue("StartingAndEndingZ"));
      }
      fixedImageDose.parse(scriptCommand);
      doseWeightingFile.parse(scriptCommand);
      typeOfDoseFile.parse(scriptCommand);
      voltage.parse(scriptCommand);
      optimalDoseScaling.parse(scriptCommand);
      bidirectionalNumViews.parse(scriptCommand);
    }
    else {
      throw new InvalidParameterException(
        "MTF Filter:  Missing parameter, -StandardInput.  Use Etomo to create .com file.");
    }
  }

  public void updateComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException {
    scriptCommand.useKeywordValue();
    ParamUtilities.updateScriptParameter(scriptCommand, "InputFile", inputFile, true);
    ParamUtilities.updateScriptParameter(scriptCommand, "OutputFile", outputFile);
    ParamUtilities.updateScriptParameter(scriptCommand, "MtfFile", mtfFile);
    ParamUtilities.updateScriptParameter(scriptCommand, "MaximumInverse", maximumInverse);
    ParamUtilities.updateScriptParameter(scriptCommand, "LowPassRadiusSigma",
      lowPassRadiusSigma);
    ParamUtilities.updateScriptParameter(scriptCommand, "InverseRolloffRadiusSigma",
      inverseRolloffRadiusSigma);
    ParamUtilities.updateScriptParameter(scriptCommand, "StartingAndEndingZ",
      startingAndEndingZ);
    fixedImageDose.updateComScript(scriptCommand);
    doseWeightingFile.updateComScript(scriptCommand);
    typeOfDoseFile.updateComScript(scriptCommand);
    voltage.updateComScript(scriptCommand);
    optimalDoseScaling.updateComScript(scriptCommand);
    bidirectionalNumViews.updateComScript(scriptCommand);
    pixelSize.updateComScript(scriptCommand);
  }

  public void initializeDefaults() {}

  public void resetOptimalDoseScaling() {
    optimalDoseScaling.reset();
  }

  public void resetBidirectionalNumViews() {
    bidirectionalNumViews.reset();
  }

  public void setOptimalDoseScaling(final String input) {
    optimalDoseScaling.set(input);
  }

  public void setBidirectionalNumViews(final String input) {
    bidirectionalNumViews.set(input);
  }

  public void setPixelSize(double input) {
    pixelSize.set(input);
  }

  public String getOptimalDoseScaling() {
    return optimalDoseScaling.toString();
  }

  public String getBidirectionalNumViews() {
    return bidirectionalNumViews.toString();
  }

  public boolean isOptimalDoseScalingSet() {
    return optimalDoseScaling.is();
  }

  public boolean isBidirectionalNumViewsSet() {
    return bidirectionalNumViews.is();
  }

  public void resetVoltage() {
    voltage.reset();
  }

  public void setVoltage200(final boolean set) {
    if (set) {
      voltage.set(VOLTAGE_200);
    }
    else {
      voltage.reset();
    }
  }

  public boolean isVoltage200() {
    return voltage.equals(VOLTAGE_200);
  }

  public void resetTypeOfDoseFile() {
    typeOfDoseFile.reset();
  }

  public void setTypeOfDoseFile(final EnumeratedType enumeratedType) {
    if (enumeratedType == null) {
      typeOfDoseFile.reset();
    }
    else {
      typeOfDoseFile.set(enumeratedType.getValue());
    }
  }

  public String getTypeOfDoseFile() {
    return typeOfDoseFile.toString();
  }

  public String getDoseWeightingFile() {
    return doseWeightingFile.toString();
  }

  public boolean isDoseWeightingFileSet() {
    return !doseWeightingFile.isEmpty();
  }

  public boolean isTypeOfDoseFileSet() {
    return !typeOfDoseFile.isNull();
  }

  public boolean isInverseRolloffRadiusSigmaSet() {
    return !inverseRolloffRadiusSigma.isNull();
  }

  public String getFixedImageDose() {
    return fixedImageDose.toString();
  }

  public boolean isFixedImageDoseSet() {
    return fixedImageDose.is();
  }

  public String getMtfFile() {
    return mtfFile;
  }

  public boolean isMtfFileSet() {
    return mtfFile != null && !mtfFile.equals("");
  }

  public String getMaximumInverseString() {
    return ParamUtilities.valueOf(maximumInverse);
  }

  public boolean isMaximumInverseSet() {
    return maximumInverse != Double.NaN;
  }

  public String getLowPassRadiusSigmaString() {
    return lowPassRadiusSigma.toString(true);
  }

  public boolean isLowPassRadiusSigmaSet() {
    return !lowPassRadiusSigma.isNull();
  }

  public String getStartingAndEndingZString() {
    return startingAndEndingZ.toString(true);
  }

  public boolean isStartingZSet() {
    return !startingAndEndingZ.isNull(0) && !startingAndEndingZ.isNull(0);
  }

  public boolean isEndingZSet() {
    return !startingAndEndingZ.isNull(1) && !startingAndEndingZ.isNull(1);
  }

  public int getStartingZ() {
    return startingAndEndingZ.getInt(0);
  }

  public int getEndingZ() {
    return startingAndEndingZ.getInt(1);
  }

  public String getInverseRolloffRadiusSigmaString() {
    return inverseRolloffRadiusSigma.toString(true);
  }

  public String getOutputFile() {
    return outputFile;
  }

  public void setInputFile(final String inputFile) {
    this.inputFile = inputFile;
  }

  public void setOutputFile(final String outputFile) {
    this.outputFile = outputFile;
  }

  public void setMtfFile(final String mtfFile) {
    this.mtfFile = mtfFile;
  }

  public void setFixedImageDose(final String input) {
    fixedImageDose.set(input);
  }

  public void setDoseWeightingFile(final String input) {
    doseWeightingFile.set(input);
  }

  public void resetDoseWeightingFile() {
    doseWeightingFile.reset();
  }

  public void resetFixedImageDose() {
    fixedImageDose.reset();
  }

  public void resetMtfFile() {
    this.mtfFile = "";
  }

  public void setMaximumInverse(final String maximumInverse) {
    this.maximumInverse = ParamUtilities.parseDouble(maximumInverse);
  }

  public void resetMaximumInverse() {
    maximumInverse = Double.NaN;
  }

  public void setLowPassRadiusSigma(String lowPassRadiusSigma)
    throws FortranInputSyntaxException {
    ParamUtilities.set(lowPassRadiusSigma, this.lowPassRadiusSigma);
  }

  public void resetLowPassRadiusSigma() {
    lowPassRadiusSigma.setDefault();
  }

  public void setInverseRolloffRadiusSigma(String inverseRolloffRadiusSigma)
    throws FortranInputSyntaxException {
    ParamUtilities.set(inverseRolloffRadiusSigma, this.inverseRolloffRadiusSigma);
  }

  public void resetInverseRolloffRadiusSigma() {
    inverseRolloffRadiusSigma.setDefault();
  }

  public void setStartingAndEndingZ(String startingAndEndingZ)
    throws FortranInputSyntaxException {
    ParamUtilities.set(startingAndEndingZ, this.startingAndEndingZ);
  }

  /**
   * @deprecated 3/15/2019
   */
  @SuppressWarnings("deprecation")
  public FileType getOutputImageFileType() {
    return FileType.MTF_FILTERED_STACK;
  }

  public FileKey getOutputImageFileKey() {
    return FileType.MTF_FILTERED_STACK;
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

  public AxisID getAxisID() {
    return axisID;
  }

  public String getCommand() {
    return FileType.MTF_FILTER_COMSCRIPT.getFileName(manager, axisID);
  }

  public String[] getCommandArray() {
    String[] array = { getCommandLine() };
    return array;
  }

  public File getCommandInputFile() {
    return null;
  }

  public String getCommandLine() {
    return getCommand();
  }

  public CommandMode getCommandMode() {
    return null;
  }

  public String getCommandName() {
    return ProcessName.MTFFILTER.toString();
  }

  public File getCommandOutputFile() {
    return null;
  }

  public ProcessName getProcessName() {
    return ProcessName.MTFFILTER;
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
