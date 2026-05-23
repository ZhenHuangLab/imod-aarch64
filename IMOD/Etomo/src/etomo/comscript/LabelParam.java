package etomo.comscript;

import etomo.type.ProcessName;

/**
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
 final class LabelParam implements CommandParam {
  private final String label;

  LabelParam(final ProcessName processName) {
    label = processName.toString();
  }

  String getLabel() {
    return label + ":";
  }

  public void parseComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException, FortranInputSyntaxException, InvalidParameterException {}

  /**
   * Replace the parameters of the ComScriptCommand with the current 
   * CommandParameter object's parameters
   * @param scriptCommand
   */
  public void updateComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException {}

  public void initializeDefaults() {}
}
