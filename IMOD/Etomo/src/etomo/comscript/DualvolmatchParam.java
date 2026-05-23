package etomo.comscript;

import etomo.type.EtomoNumber;
import etomo.type.ScriptParameter;

/**
 * <p>Description: Represents the dualvolmatch parameters.</p>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class DualvolmatchParam implements CommandParam {
  public static final String MAXIMUM_RESIDUAL = "MaximumResidual";
  public static final String CENTER_SHIFT_LIMIT = "CenterShiftLimit";

  private final ScriptParameter maximumResidual = new ScriptParameter(
    EtomoNumber.Type.DOUBLE, MAXIMUM_RESIDUAL);
  private final ScriptParameter centerShiftLimit = new ScriptParameter(
    EtomoNumber.Type.DOUBLE, CENTER_SHIFT_LIMIT);

  DualvolmatchParam() {}

  public void initializeDefaults() {
    maximumResidual.reset();
    centerShiftLimit.reset();
  }

  public void parseComScriptCommand(final ComScriptCommand scriptCommand)
    throws FortranInputSyntaxException, InvalidParameterException {
    initializeDefaults();
    maximumResidual.parse(scriptCommand);
    centerShiftLimit.parse(scriptCommand);
  }

  public void updateComScriptCommand(final ComScriptCommand scriptCommand)
    throws BadComScriptException {
    scriptCommand.useKeywordValue();
    maximumResidual.updateComScript(scriptCommand);
    centerShiftLimit.updateComScript(scriptCommand);
  }

  public String getMaximumResidual() {
    return maximumResidual.toString();
  }

  public String getCenterShiftLimit() {
    return centerShiftLimit.toString();
  }

  public void setMaximumResidual(final String input) {
    maximumResidual.set(input);
  }

  public void setCenterShiftLimit(final String input) {
    centerShiftLimit.set(input);
  }
}
