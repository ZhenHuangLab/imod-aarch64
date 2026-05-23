package etomo.comscript;

import etomo.type.ConstEtomoNumber;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2009 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface ConstFindBeads3dParam extends CommandDetails {
  String getBeadSize();

  String getMinRelativeStrength();

  String getThresholdForAveraging();

  ConstEtomoNumber getStorageThreshold();

  String getMinSpacing();

  String getGuessNumBeads();

  String getMaxNumBeads();
}
