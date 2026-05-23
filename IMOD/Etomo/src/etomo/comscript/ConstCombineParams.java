package etomo.comscript;

import etomo.type.CombinePatchSize;
import etomo.type.ConstEtomoNumber;
import etomo.type.FiducialMatch;
import etomo.type.MatchMode;

/**
 * <p>Description: A read only model of the parameter interface for the
 * setupcombine script</p>
 *
 * <p>Copyright: Copyright 2002 - 2015 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 3.10  2010/02/17 04:47:54  sueh
 * <p> bug# 1301 Using the manager instead of the manager key do pop up
 * <p> messages.
 * <p>
 * <p> Revision 3.9  2009/03/17 00:31:26  sueh
 * <p> bug# 1186 Pass managerKey to everything that pops up a dialog.
 * <p>
 * <p> Revision 3.8  2009/02/13 02:12:24  sueh
 * <p> bug# 1176 Checking return value of MRCHeader.read.
 * <p>
 * <p> Revision 3.7  2006/07/19 15:15:45  sueh
 * <p> bug# 903 Change patchZMin and Max to EtomoNumbers so they won't generate
 * <p> an exception when they are set to a blank string.
 * <p>
 * <p> Revision 3.6  2006/05/16 21:23:42  sueh
 * <p> bug# 856 Added transfer and useList.  Removed dialogMatchMode from
 * <p> CombineParam.  Letting the screen save the script state, since it already is.
 * <p> Going to revision 1.2.
 * <p>
 * <p> Revision 3.5  2006/03/16 01:48:38  sueh
 * <p> bug# 828 Changed matchBtoA to dialogMatchMode.  Added matchMode.
 * <p> DialogMatchMode reflects the state of the dialog.  MatchMode reflects the
 * <p> state of the script.  MatchMode is set equal to dialogMatchMode when the
 * <p> script is updated.
 * <p>
 * <p> Revision 3.4  2005/07/29 00:44:21  sueh
 * <p> bug# 709 Going to EtomoDirector to get the current manager is unreliable
 * <p> because the current manager changes when the user changes the tab.
 * <p> Passing the manager where its needed.
 * <p>
 * <p> Revision 3.3  2005/07/26 17:09:59  sueh
 * <p> bug# 700 Don't respond to MRCHeader exceptions in isValid() because it
 * <p> is run without the user directly requesting it.
 * <p>
 * <p> Revision 3.2  2005/07/20 17:30:52  sueh
 * <p> bug# 700  In isValid() validate the x, y, z values against the tomogram
 * <p> header.  Pass YAndZFlipped.  Get the tomogram file from DatasetFile.
 * <p>
 * <p> Revision 3.1  2004/03/06 00:26:37  sueh
 * <p> bug# 318 add maxPatchZMax - get, use to validate patchZMax
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:00  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.5  2003/10/20 16:51:29  rickg
 * <p> Removed scriptsCreated flag, use existence of combine com scripts instead
 * <p>
 * <p> Revision 2.4  2003/07/28 22:44:56  rickg
 * <p> Added an equals method
 * <p>
 * <p> Revision 2.3  2003/03/18 23:49:40  rickg
 * <p> Added scripts created state variable
 * <p>
 * <p> Revision 2.2  2003/03/18 16:38:18  rickg
 * <p> Added model based boolean
 * <p>
 * <p> Revision 2.1  2003/02/24 23:29:54  rickg
 * <p> Added use patch region model method
 * <p>
 * <p> Revision 2.0  2003/01/24 20:30:31  rickg
 * <p> Single window merge to main branch
 * <p>
 * <p> Revision 1.4.2.1  2003/01/24 18:33:42  rickg
 * <p> Single window GUI layout initial revision
 * <p>
 * <p> Revision 1.4  2002/10/09 17:28:37  rickg
 * <p> Fixed javadoc tag
 * <p>
 * <p> Revision 1.3  2002/10/08 23:59:22  rickg
 * <p> Added isPatchBoundarySet method
 * <p> Added basic isValid method
 * <p>
 * <p> Revision 1.2  2002/10/03 03:59:31  rickg
 * <p> Added path X,Y,Z min and max attributes
 * <p>
 * <p> Revision 1.1  2002/09/09 22:57:02  rickg
 * <p> Initial CVS entry, basic functionality not including combining
 * <p> </p>
 */

public interface ConstCombineParams {

  public boolean isPatchSizeSet(boolean autoFinal);

  public boolean isExtraResidualTargetsSet();

  /**
   * Returns true if the patch boundary values have been modified
   */
  public boolean isPatchBoundarySet();

  /**
   * Checks the validity of the attribute values.
   * @return true if all entries are valid, otherwise the reasons are 
   * available through the method getInvalidReasons.
   */
  public boolean isValid(boolean YAndZflipped);

  /**
   * Returns the reasons the attribute values are invalid as a string array.
   */
  public String[] getInvalidReasons();

  public MatchMode getMatchMode();

  public boolean isTransfer();

  public FiducialMatch getFiducialMatch();

  public String getUseList();

  public String getFiducialMatchListA();

  public String getFiducialMatchListB();

  public CombinePatchSize getPatchSize(boolean autoFinal);

  public String[] getPatchSizeXYZArray(boolean autoFinal);

  public String getExtraResidualTargets();

  public String getTempDirectory();

  public boolean getManualCleanup();

  /**
   * Returns the patchXMax.
   * @return int
   */
  public int getPatchXMax();

  /**
   * Returns the patchXMin.
   * @return int
   */
  public int getPatchXMin();

  /**
   * Returns the patchYMax.
   * @return int
   */
  public int getPatchYMax();

  /**
   * Returns the patchYMin.
   * @return int
   */
  public int getPatchYMin();

  /**
   * Returns the patchZMax.
   * @return int
   */
  public ConstEtomoNumber getPatchZMax();

  /**
   * Returns the patchZMin.
   * @return int
   */
  public ConstEtomoNumber getPatchZMin();

  public int getMaxPatchZMax();

  /**
   * Returns true if a patch region model has been specified.
   * @return boolean
   */
  public boolean usePatchRegionModel();

  public String getWedgeReductionFraction();

  public String getLowFromBothRadius();

  public boolean isInitialVolumeMatching();
}