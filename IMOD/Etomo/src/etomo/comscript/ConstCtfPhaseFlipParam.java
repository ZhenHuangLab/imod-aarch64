package etomo.comscript;

import etomo.type.ConstEtomoNumber;
import etomo.type.ConstStringParameter;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2008 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.2  2009/10/19 16:27:43  sueh
 * <p> bug# 1253 Added getInvertTiltAngles.
 * <p>
 * <p> Revision 1.1  2008/10/27 17:44:34  sueh
 * <p> bug# 1141 Constant interface to CtfPhaseFlipParam.
 * <p> </p>
 */
public interface ConstCtfPhaseFlipParam extends Command {
  public ConstEtomoNumber getVoltage();

  public ConstEtomoNumber getSphericalAberration();

  public boolean getInvertTiltAngles();

  public ConstEtomoNumber getAmplitudeContrast();

  public ConstStringParameter getDefocusFile();

  public ConstEtomoNumber getInterpolationWidth();

  public ConstEtomoNumber getDefocusTol();

  public boolean isUseGpu();

  public String getXAxisTilt();

  public String getScaleByCtfPower();

  public String getMinimumZeroSpacing();
}
