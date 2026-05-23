package etomo.comscript;

import etomo.type.ConstEtomoNumber;
import etomo.type.EtomoBoolean2;
import etomo.type.EtomoNumber;
import etomo.type.ScriptParameter;
import etomo.type.StringParameter;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2008 - 2022 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.3  2009/10/19 16:28:20  sueh
 * <p> bug# 1253 Added invertTiltAngles.
 * <p>
 * <p> Revision 1.2  2009/02/25 00:14:32  sueh
 * <p> bug# 1182 Made sphericalAberration a double.
 * <p>
 * <p> Revision 1.1  2008/10/27 17:48:52  sueh
 * <p> bug# 1141 Class to update a ctfplotter call.
 * <p> </p>
 */
public final class CtfPlotterParam implements ConstCtfPlotterParam, CommandParam {
  public static final String COMMAND = "ctfplotter";
  public static final String CONFIG_FILE_OPTION = "ConfigFile";
  public static final String EXPECTED_DEFOCUS_OPTION = "ExpectedDefocus";
  public static final String PHASE_SHIFT_IN_DEGREES_OPTION = "PhaseShiftInDegrees";
  public static final String OFFSET_TO_ADD_OPTION = "OffsetToAdd";

  private final ScriptParameter voltage =
    new ScriptParameter(CtfPhaseFlipParam.VOLTAGE_OPTION);
  private final ScriptParameter sphericalAberration = new ScriptParameter(
    EtomoNumber.Type.DOUBLE, CtfPhaseFlipParam.SPHERICAL_ABERRATION_OPTION);
  private final EtomoBoolean2 invertTiltAngles =
    new EtomoBoolean2(CtfPhaseFlipParam.INVERT_TILT_ANGLES_OPTION);
  private final ScriptParameter amplitudeContrast = new ScriptParameter(
    EtomoNumber.Type.DOUBLE, CtfPhaseFlipParam.AMPLITUDE_CONTRAST_OPTION);
  private final StringParameter configFile = new StringParameter(CONFIG_FILE_OPTION);
  private final ScriptParameter expectedDefocus =
    new ScriptParameter(EtomoNumber.Type.DOUBLE, EXPECTED_DEFOCUS_OPTION);
  private final ScriptParameter phaseShiftInDegrees =
    new ScriptParameter(EtomoNumber.Type.DOUBLE, PHASE_SHIFT_IN_DEGREES_OPTION);
  private final ScriptParameter phasePlateShift =
    new ScriptParameter(EtomoNumber.Type.DOUBLE, "PhasePlateShift");
  private final ScriptParameter offsetToAdd =
    new ScriptParameter(EtomoNumber.Type.DOUBLE, OFFSET_TO_ADD_OPTION);
  FortranInputString autoFitRangeAndStep =
    new FortranInputString("AutoFitRangeAndStep", 2);

  public void parseComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException, InvalidParameterException, FortranInputSyntaxException {
    reset();
    voltage.parse(scriptCommand);
    sphericalAberration.parse(scriptCommand);
    invertTiltAngles.parse(scriptCommand);
    amplitudeContrast.parse(scriptCommand);
    configFile.parse(scriptCommand);
    expectedDefocus.parse(scriptCommand);
    // PhaseShiftInDegrees and PhasePlateShift are the same parameter - one in degrees and
    // the other in radians. PhaseShiftInDegrees takes precedence - fill it with
    // PhasePlateShift when it is has no value. Don't keep both.
    phaseShiftInDegrees.parse(scriptCommand);
    phasePlateShift.parse(scriptCommand);
    if (phaseShiftInDegrees.isNull() && !phasePlateShift.isNull()) {
      // Round converted phaseShiftInDegrees to xx.x.
      phaseShiftInDegrees
        .set(Math.round(Math.toDegrees(phasePlateShift.getDouble()) * 10) / 10);
      System.err.println("CtfPlotterParam: Converted the value of PhasePlateShift ("
        + phasePlateShift.toString()
        + ") to degrees and placed it in .PhaseShiftInDegrees.");
    }
    phasePlateShift.reset();
    //
    offsetToAdd.parse(scriptCommand);
    autoFitRangeAndStep.validateAndSet(scriptCommand);
  }

  public void updateComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException {
    voltage.updateComScript(scriptCommand);
    sphericalAberration.updateComScript(scriptCommand);
    invertTiltAngles.updateComScript(scriptCommand);
    amplitudeContrast.updateComScript(scriptCommand);
    configFile.updateComScript(scriptCommand);
    expectedDefocus.updateComScript(scriptCommand);
    // PhaseShiftInDegrees and PhasePlateShift: see parseComScriptCommand.
    phaseShiftInDegrees.updateComScript(scriptCommand);
    if (!phasePlateShift.isNull()) {
      System.err.println("CtfPlotterParam: PhasePlateShift has been deleted - was "
        + phasePlateShift.toString() + ".");
    }
    phasePlateShift.reset();
    phasePlateShift.updateComScript(scriptCommand);
    //
    offsetToAdd.updateComScript(scriptCommand);
    autoFitRangeAndStep.updateScriptParameter(scriptCommand);
  }

  public void initializeDefaults() {}

  private void reset() {
    voltage.reset();
    sphericalAberration.reset();
    invertTiltAngles.reset();
    amplitudeContrast.reset();
    expectedDefocus.reset();
    phaseShiftInDegrees.reset();
    phasePlateShift.reset();
    offsetToAdd.reset();
    autoFitRangeAndStep.reset();
  }

  public void setVoltage(String input) {
    voltage.set(input);
  }

  public void setSphericalAberration(String input) {
    sphericalAberration.set(input);
  }

  public void setInvertTiltAngles(boolean input) {
    invertTiltAngles.set(input);
  }

  public void setAmplitudeContrast(String input) {
    amplitudeContrast.set(input);
  }

  public void setAutoFitRangeAndStep(final FortranInputString input) {
    autoFitRangeAndStep.set(input);
  }

  public void setConfigFile(final String input) {
    configFile.set(input);
  }

  public String getConfigFile() {
    return configFile.toString();
  }

  public void setExpectedDefocus(String input) {
    expectedDefocus.set(input);
  }

  public void setPhaseShiftInDegrees(final String input) {
    phaseShiftInDegrees.set(input);
  }

  public String getPhaseShiftInDegrees() {
    return phaseShiftInDegrees.toString();
  }

  public void setOffsetToAdd(String input) {
    offsetToAdd.set(input);
  }

  public ConstEtomoNumber getExpectedDefocus() {
    return expectedDefocus;
  }

  public ConstEtomoNumber getOffsetToAdd() {
    return offsetToAdd;
  }
}
