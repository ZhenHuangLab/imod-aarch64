package etomo.comscript;

import java.util.Vector;

import etomo.ApplicationManager;
import etomo.type.AxisID;
import etomo.type.EtomoNumber;
import etomo.type.ScriptParameter;

/**
 * <p>Description: A read only model of the parameter interface for the
 *  tiltxcorr program</p>
 *
 * <p>Copyright: Copyright 2004 - 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */

public class ConstTomopitchParam {
  public static final String COMMAND = "tomopitch";
  public static final String MODEL_FILE = "ModelFile";
  public static final String SPACING_IN_Y = "SpacingInY";
  public static final String SCALE_FACTOR = "ScaleFactor";
  public static final String PARAMETER_FILE = "ParameterFile";

  final ApplicationManager manager;
  final AxisID axisID;

  Vector modelFiles;
  ScriptParameter extraThickness =
    new ScriptParameter(EtomoNumber.Type.DOUBLE, "ExtraThickness");
  double spacingInY;
  double scaleFactor;
  String parameterFile;
  ScriptParameter angleOffsetOld =
    new ScriptParameter(EtomoNumber.Type.DOUBLE, "AngleOffsetOld");
  ScriptParameter zShiftOld = new ScriptParameter(EtomoNumber.Type.DOUBLE, "ZShiftOld");
  ScriptParameter xAxisTiltOld =
    new ScriptParameter(EtomoNumber.Type.DOUBLE, "XAxisTiltOld");

  public ConstTomopitchParam(final ApplicationManager manager, final AxisID axisID) {
    this.manager = manager;
    this.axisID = axisID;
    reset();
  }

  void reset() {
    modelFiles = new Vector();
    spacingInY = Double.NaN;
    scaleFactor = Double.NaN;
    parameterFile = new String();
    angleOffsetOld.reset();
    zShiftOld.reset();
    xAxisTiltOld.reset();
  }

  public int getModelFilesSize() {
    return modelFiles.size();
  }

  public String getModelFile(int index) {
    return (String) modelFiles.get(index);
  }

  public String getExtraThicknessString() {
    return extraThickness.toString();
  }

  public boolean isExtraThicknessNull() {
    return extraThickness.isNull();
  }

  public String getSpacingInYString() {
    return ParamUtilities.valueOf(spacingInY);
  }

  public String getScaleFactorString() {
    return ParamUtilities.valueOf(scaleFactor);
  }

  public String getParameterFile() {
    return parameterFile;
  }

  /**
   * Return a multiline string describing the class attributes.
   */
  public String toString() {
    StringBuffer string = new StringBuffer("\n");
    string.append("ModelFilesSize:" + getModelFilesSize() + "\n");
    for (int i = 0; i < getModelFilesSize(); i++) {
      string.append(MODEL_FILE + ":" + getModelFile(i) + "\n");
    }
    string.append(extraThickness.getName() + ":" + getExtraThicknessString() + "\n");
    string.append(SCALE_FACTOR + ":" + getScaleFactorString() + "\n");
    string.append(SPACING_IN_Y + ":" + getSpacingInYString() + "\n");
    string.append(PARAMETER_FILE + ":" + getParameterFile() + "\n");
    return string.toString();

  }
}
