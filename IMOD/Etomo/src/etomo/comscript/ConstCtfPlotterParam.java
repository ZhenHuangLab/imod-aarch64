package etomo.comscript;

import etomo.type.ConstEtomoNumber;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2008 - 2022 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
*/
public interface ConstCtfPlotterParam {
  public String getConfigFile();

  public ConstEtomoNumber getExpectedDefocus();

  public String getPhaseShiftInDegrees();

  public ConstEtomoNumber getOffsetToAdd();
}
