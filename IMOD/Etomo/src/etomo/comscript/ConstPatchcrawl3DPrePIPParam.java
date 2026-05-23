package etomo.comscript;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2002 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 3.1  2004/03/02 21:48:40  sueh
 * <p> bug# 250 added borders, moved reset() to const
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:00  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.1  2003/03/02 23:30:41  rickg
 * <p> Combine layout in progress
 * <p> </p>
 */
class ConstPatchcrawl3DPrePIPParam {
  int xPatchSize;
  int yPatchSize;
  int zPatchSize;
  int nX;
  int nY;
  int nZ;
  int xLow;
  int xHigh;
  int yLow;
  int yHigh;
  int zLow;
  int zHigh;
  int maxShift;
  String fileA = "";
  String fileB = "";
  String outputFile = "";
  String transformFile = "";
  String originalFileB = "";
  FortranInputString borders;
  String boundaryModel = "";

  ConstPatchcrawl3DPrePIPParam() {
    reset();
  }

  /**
   * @return String
   */
  String getFileA() {
    return fileA;
  }

  /**
   * @return String
   */
  String getFileB() {
    return fileB;
  }

  /**
   * @return int
   */
  int getNX() {
    return nX;
  }

  /**
   * @return int
   */
  int getNY() {
    return nY;
  }

  /**
   * @return int
   */
  int getNZ() {
    return nZ;
  }

  /**
   * @return String
   */
  String getOriginalFileB() {
    return originalFileB;
  }

  /**
   * @return String
   */
  String getTransformFile() {
    return transformFile;
  }

  String getBorders() {
    return borders.toString();
  }

  FortranInputString getBordersFortranInputString() {
    return borders;
  }

  /**
   * @return int
   */
  int getXHigh() {
    return xHigh;
  }

  /**
   * @return int
   */
  int getXLow() {
    return xLow;
  }

  /**
   * @return int
   */
  int getXPatchSize() {
    return xPatchSize;
  }

  /**
   * @return int
   */
  int getYHigh() {
    return yHigh;
  }

  /**
   * @return int
   */
  int getYLow() {
    return yLow;
  }

  /**
   * @return int
   */
  int getYPatchSize() {
    return yPatchSize;
  }

  /**
   * @return int
   */
  int getZHigh() {
    return zHigh;
  }

  /**
   * @return int
   */
  int getZLow() {
    return zLow;
  }

  /**
   * @return int
   */
  int getZPatchSize() {
    return zPatchSize;
  }

  /**
   * @return int
   */
  int getMaxShift() {
    return maxShift;
  }

  /**
   * @return String
   */
  String getBoundaryModel() {
    return boundaryModel;
  }

  boolean isUseBoundaryModel() {
    return !boundaryModel.equals("");
  }

  /**
   * @return String
   */
  String getOutputFile() {
    return outputFile;
  }

  void reset() {
    xPatchSize = 0;
    yPatchSize = 0;
    zPatchSize = 0;
    nX = 0;
    nY = 0;
    nZ = 0;
    xLow = 0;
    xHigh = 0;
    yLow = 0;
    yHigh = 0;
    zLow = 0;
    zHigh = 0;
    maxShift = 0;
    fileA = "";
    fileB = "";
    outputFile = "";
    transformFile = "";
    originalFileB = "";
    borders = new FortranInputString(4);
    boolean[] intFlag = { true, true, true, true };
    borders.setIntegerType(intFlag);
    boundaryModel = "";
  }

}
