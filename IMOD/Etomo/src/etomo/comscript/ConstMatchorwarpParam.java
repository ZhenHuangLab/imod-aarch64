package etomo.comscript;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2002 - 2015 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 3.3  2006/09/19 21:55:47  sueh
 * <p> bug# 928 Added residualFile, vectormodel, and clipsize.
 * <p>
 * <p> Revision 3.2  2004/06/25 23:23:23  sueh
 * <p> bug# 485 adding isUseModelFile()
 * <p>
 * <p> Revision 3.1  2004/03/06 03:46:07  sueh
 * <p> bug# 380 added useLinearInterpolation
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:00  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.2  2003/03/06 05:53:28  rickg
 * <p> Combine interface in progress
 * <p>
 * <p> Revision 2.1  2003/03/02 23:30:41  rickg
 * <p> Combine layout in progress
 * <p> </p>
 */
public interface ConstMatchorwarpParam {
  public boolean isUseModelFile();

  public String getRefineLimit();

  public String getWarpLimits();

  public int getXLowerExclude();

  public int getXUpperExclude();

  public int getZLowerExclude();

  public int getZUpperExclude();

  public boolean isLinearInterpolation();

  public boolean isXLowerExcludeSet();

  public boolean isXUpperExcludeSet();

  public boolean isZLowerExcludeSet();

  public boolean isZUpperExcludeSet();
}
