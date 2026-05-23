package etomo.ui;

import etomo.comscript.ExcludeViewsParam;
import etomo.storage.DirectiveFileCollection;
import etomo.type.AxisID;
import etomo.type.TiltAngleSpec;
import etomo.type.UserConfiguration;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2012 - 2016 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public interface SetupReconInterface {
  public void setBinning(String input);

  public void setImageRotation(String input);

  public void setPixelSize(double input);

  /**
   * @deprecated 4/8/2019
   * @return
   */
  public String getDataset();
  public String getRawImageStack();

  public boolean isDualAxisSelected();

  public String getDistortionFile();

  public String getMagGradientFile();

  public boolean validateTiltAngle(AxisID axisID, String errorTitle);

  public boolean isSingleViewSelected();

  public String getBackupDirectory();

  public String getBinning(boolean doValidation) throws FieldValidationFailedException;

  public String getExcludeList(AxisID axisID, boolean doValidation)
    throws FieldValidationFailedException;

  public String getTwodir(AxisID axisID, boolean doValidation)
    throws FieldValidationFailedException;

  public boolean isTwodir(AxisID axisID);

  public void setTwodir(AxisID axisID, double input);

  public String getFiducialDiameter(boolean doValidation)
    throws FieldValidationFailedException;

  public String getImageRotation(AxisID axisID, boolean doValidation)
    throws FieldValidationFailedException;

  public String getPixelSize(boolean doValidation) throws FieldValidationFailedException;

  public boolean isAdjustedFocusSelected(AxisID axisID);

  public boolean isSingleAxisSelected();

  public boolean isGpuProcessingSelected(String propertyUserDir);

  public boolean isParallelProcessSelected(String propertyUserDir);

  public boolean getTiltAngleFields(AxisID axisID, TiltAngleSpec tiltAngleSpec,
    boolean doValidation);

  public DirectiveFileCollection getDirectiveFileCollection();

  public void initTiltAngleFields(AxisID axisID, TiltAngleSpec tiltAngleSpec,
    UserConfiguration userConfiguration);

  public boolean getParameters(ExcludeViewsParam param, AxisID axisID, boolean dualAxis,
    boolean doValidation);

  public void msgExcludeViewsSucceeded(AxisID axisID, boolean processRunning,
    boolean processDone);
}
