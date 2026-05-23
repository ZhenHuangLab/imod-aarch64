package etomo.uitest;

import etomo.type.AxisID;

/**
 * <p>Description: Storage for uitest language variables.</p>
 * 
 * <p>Copyright: Copyright 2008 - 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 1.2  2009/09/01 03:18:33  sueh
 * <p> bug# 1222
 * <p>
 * <p> Revision 1.1  2009/01/20 20:51:46  sueh
 * <p> bug# 1102 Interface for classes that hold list(s) of variable name/value
 * <p> pairs.
 * <p> </p>
 */
interface VariableList {
  public String getVariableValue(String variableName, AxisID axisID);

  public boolean isVariableSet(String variableName, AxisID axisID);

  public void setVariable(String variableName, Object variableValue);

  public void unsetVariable(String variableName, AxisID axisID);
}
