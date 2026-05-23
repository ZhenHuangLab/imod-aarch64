package etomo.comscript;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import etomo.BaseManager;
import etomo.storage.Storable;
import etomo.type.AxisID;
import etomo.type.CombinePatchSize;
import etomo.type.ConstEtomoNumber;
import etomo.type.EtomoNumber;
import etomo.type.FiducialMatch;
import etomo.type.MatchMode;
import etomo.type.MetaData;
import etomo.type.StringProperty;
import etomo.util.DatasetFiles;
import etomo.util.InvalidParameterException;
import etomo.util.MRCHeader;

/**
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright 2002 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 3.16  2010/02/17 04:47:54  sueh
 * <p> bug# 1301 Using the manager instead of the manager key do pop up
 * <p> messages.
 * <p>
 * <p> Revision 3.15  2009/03/17 00:31:05  sueh
 * <p> bug# 1186 Pass managerKey to everything that pops up a dialog.
 * <p>
 * <p> Revision 3.14  2009/02/13 02:11:59  sueh
 * <p> bug# 1176 Checking return value of MRCHeader.read.
 * <p>
 * <p> Revision 3.13  2007/08/21 21:51:06  sueh
 * <p> bug# 771 Factored out get xyborder functionality from
 * <p> setDefaultPatchBoundaries into getXYBorder.
 * <p>
 * <p> Revision 3.12  2007/07/27 21:38:32  sueh
 * <p> bug# 980 Removed unnecessary resetUserList.
 * <p>
 * <p> Revision 3.11  2007/07/27 21:23:31  sueh
 * <p> bug# 980 Added resetUseList.
 * <p>
 * <p> Revision 3.10  2006/07/19 15:14:37  sueh
 * <p> bug# 903 Change patchZMin and Max to EtomoNumbers so they won't generate
 * <p> an exceptionMatchMode when they are set to a blank string.
 * <p>
 * <p> Revision 3.9  2006/05/16 21:20:23  sueh
 * <p> bug# 856 Added transfer and useList.  Removed dialogMatchMode from
 * <p> CombineParam.  Letting the screen save the script state, since it already is.
 * <p> Going to revision 1.2.
 * <p>
 * <p> Revision 3.8  2006/03/16 01:48:26  sueh
 * <p> bug# 828 Changed matchBtoA to dialogMatchMode.  Added matchMode.
 * <p> DialogMatchMode reflects the state of the dialog.  MatchMode reflects the
 * <p> state of the script.  MatchMode is set equal to dialogMatchMode when the
 * <p> script is updated.
 * <p>
 * <p> Revision 3.7  2005/10/19 19:49:29  mast
 * <p> Limited borderindex by the number of xyborder values
 * <p>
 * <p> Revision 3.6  2005/07/29 00:43:05  sueh
 * <p> bug# 709 Going to EtomoDirector to get the current manager is unreliable
 * <p> because the current manager changes when the user changes the tab.
 * <p> Passing the manager where its needed.
 * <p>
 * <p> Revision 3.5  2005/06/20 16:40:15  sueh
 * <p> bug# 522 Made MRCHeader an n'ton.  Getting instance instead of
 * <p> constructing in setDefaultPatchBoundardies().
 * <p>
 * <p> Revision 3.4  2005/04/25 20:35:19  sueh
 * <p> bug# 615 Passing the axis where the command originated to the message
 * <p> functions so that the message will be popped up in the correct window.
 * <p> This requires adding AxisID to many objects.
 * <p>
 * <p> Revision 3.3  2004/06/24 18:32:36  sueh
 * <p> bug# 482 handling USE_MODEL_USE should also cause
 * <p> modelBased to be set to true
 * <p>
 * <p> Revision 3.2  2004/03/22 23:19:30  sueh
 * <p> bug# 250 synchronizing fiducialMatch and modelBased
 * <p>
 * <p> Revision 3.1  2004/03/06 00:25:31  sueh
 * <p> bug# 318 add maxPatchZMax - set, setDefault, load, store
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:00  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.5  2003/10/20 16:51:41  rickg
 * <p> Removed scriptsCreated flag, use existence of combine com scripts instead
 * <p>
 * <p> Revision 2.4  2003/03/18 23:50:08  rickg
 * <p> Added scripts created state variable
 * <p>
 * <p> Revision 2.3  2003/03/18 16:38:04  rickg
 * <p> Added model based boolean
 * <p>
 * <p> Revision 2.2  2003/03/06 05:53:28  rickg
 * <p> Combine interface in progress
 * <p>
 * <p> Revision 2.1  2003/02/24 23:28:15  rickg
 * <p> Added default patch region model setter
 * <p>
 * <p> Revision 2.0  2003/01/24 20:30:31  rickg
 * <p> Single window merge to main branch
 * <p>
 * <p> Revision 1.8.2.1  2003/01/24 18:33:42  rickg
 * <p> Single window GUI layout initial revision
 * <p>
 * <p> Revision 1.8  2003/01/06 05:49:40  rickg
 * <p> Fixed fiducial list bug
 * <p>
 * <p> Revision 1.7  2002/10/09 00:01:57  rickg
 * <p> added copy constructor
 * <p> convert whitespace strings to zero length string in set methods
 * <p> added patchBoundaries to load and store
 * <p> added revision number
 * <p>
 * <p> Revision 1.6  2002/10/08 04:39:59  rickg
 * <p> Added setDefaultPatchBoundaryMethod
 * <p>
 * <p> Revision 1.5  2002/10/07 22:23:14  rickg
 * <p> removed unused imports
 * <p> reformat after emacs messed it up
 * <p> started defaultPatchSize
 * <p>
 * <p> Revision 1.4  2002/10/03 04:00:13  rickg
 * <p> Added path X,Y,Z min and max attributes
 * <p>
 * <p> Revision 1.3  2002/10/01 21:46:35  rickg
 * <p> Implemented load and save methods
 * <p>
 * <p> Revision 1.2  2002/09/30 23:45:12  rickg
 * <p> Reformatted after emacs trashed it
 * <p>
 * <p> Revision 1.1  2002/09/09 22:57:02  rickg
 * <p> Initial CVS entry, basic functionality not including combining
 * <p> </p>
 */

public final class CombineParams implements ConstCombineParams, Storable {

  private static final String MATCH_B_TO_A_KEY = "MatchBtoA";
  private static final String MATCH_MODE_KEY = "MatchMode";
  private static final String DIALOG_MATCH_MODE_KEY = "DialogMatchMode";
  public static final String PATCH_Z_MIN_LABEL = "Z axis min";
  public static final String PATCH_Z_MAX_LABEL = "Z axis max";
  private static final String AUTO_PATCH_FINAL_SIZE_KEY = "AutoPatchFinalSize";
  private static final String XYZ_KEY = "XYZ";
  private static final String PATCH_SIZE_KEY = "PatchSize";
  private static final String DEFAULT_PATCH_SIZE = CombinePatchSize.MEDIUM.toString();
  private static final String REVISION = "1.2";

  private final ArrayList invalidReasons = new ArrayList();
  private final StringProperty patchSize = new StringProperty(PATCH_SIZE_KEY);
  private final EtomoNumber patchZMin = new EtomoNumber("PatchBoundaryZMin");
  private final EtomoNumber patchZMax = new EtomoNumber("PatchBoundaryZMax");
  private final StringProperty patchSizeXYZ =
    new StringProperty(PATCH_SIZE_KEY + "." + XYZ_KEY);
  private final StringProperty autoPatchFinalSize =
    new StringProperty(AUTO_PATCH_FINAL_SIZE_KEY);
  private final StringProperty autoPatchFinalSizeXYZ =
    new StringProperty(AUTO_PATCH_FINAL_SIZE_KEY + "." + XYZ_KEY);
  private final StringProperty extraResidualTargets =
    new StringProperty("ExtraResidualTargets");
  private final StringProperty wedgeReductionFraction =
    new StringProperty("WedgeReductionFraction");
  private final StringProperty lowFromBothRadius =
    new StringProperty("LowFromBothRadius");
  private final EtomoNumber initialVolumeMatching =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN, "InitialVolumeMatching");
  private final StringProperty extraResidualTargetsFromBatchruntomo = new StringProperty(
    MetaData.BATCHRUNTOMO_KEY + "." + MetaData.COMBINE_KEY + ".ExtraResidualTargets");
  private final StringProperty fiducialMatchFromBatchruntomo = new StringProperty(
    MetaData.BATCHRUNTOMO_KEY + "." + MetaData.COMBINE_KEY + ".FiducialMatch");
  private final StringProperty autoPatchFinalSizeFromBatchruntomo = new StringProperty(
    MetaData.BATCHRUNTOMO_KEY + "." + MetaData.COMBINE_KEY + ".FinalPatchSize");
  private final StringProperty matchModeFromBatchruntomo = new StringProperty(
    MetaData.BATCHRUNTOMO_KEY + "." + MetaData.COMBINE_KEY + ".MatchMode");
  private final StringProperty patchTypeOrXYZFromBatchruntomo = new StringProperty(
    MetaData.BATCHRUNTOMO_KEY + "." + MetaData.COMBINE_KEY + ".PatchSize");
  private final EtomoNumber initialVolumeMatchingFromBatchruntomo =
    new EtomoNumber(EtomoNumber.Type.BOOLEAN,
      MetaData.BATCHRUNTOMO_KEY + "." + MetaData.COMBINE_KEY + ".InitialVolumeMatching");

  private final BaseManager manager;

  private StringList useList = new StringList(0);
  private StringList fiducialMatchListA = new StringList(0);
  private StringList fiducialMatchListB = new StringList(0);
  private MatchMode matchMode = null;
  private FiducialMatch fiducialMatch = FiducialMatch.BOTH_SIDES;
  private int patchXMin = 0;
  private int patchXMax = 0;
  private int patchYMin = 0;
  private int patchYMax = 0;
  private int maxPatchZMax = 0;
  private String patchRegionModel = "";
  private String tempDirectory = "";
  private boolean manualCleanup = false;
  private boolean modelBased = false;
  private boolean transfer = true;
  private String revisionNumber = REVISION;
  private boolean newBatchruntomoSettings = false;

  /**
   * Default constructor
   */
  public CombineParams(final BaseManager manager) {
    this.manager = manager;
    initialVolumeMatching.setDisplayValue(false);
    reset();
  }

  private void reset() {
    invalidReasons.clear();
    patchSize.set(DEFAULT_PATCH_SIZE);
    patchZMin.set(0);
    patchZMax.set(0);
    patchSizeXYZ.reset();
    autoPatchFinalSize.reset();
    autoPatchFinalSizeXYZ.reset();
    extraResidualTargets.reset();
    wedgeReductionFraction.reset();
    lowFromBothRadius.reset();
    initialVolumeMatching.reset();
    useList.reset();
    fiducialMatchListA.reset();
    fiducialMatchListB.reset();
    matchMode = null;
    fiducialMatch = FiducialMatch.BOTH_SIDES;
    patchXMin = 0;
    patchXMax = 0;
    patchYMin = 0;
    patchYMax = 0;
    maxPatchZMax = 0;
    patchRegionModel = "";
    tempDirectory = "";
    manualCleanup = false;
    modelBased = false;
    transfer = true;
    revisionNumber = REVISION;
    extraResidualTargetsFromBatchruntomo.reset();
    fiducialMatchFromBatchruntomo.reset();
    autoPatchFinalSizeFromBatchruntomo.reset();
    matchModeFromBatchruntomo.reset();
    patchTypeOrXYZFromBatchruntomo.reset();
    initialVolumeMatchingFromBatchruntomo.reset();
  }

  public void resetBatchruntomoCombineSettings() {
    newBatchruntomoSettings = false;
  }

  public boolean isNewBatchruntomoCombineSettings() {
    return newBatchruntomoSettings;
  }

  public void moveBatchruntomoSettings() {
    if (!extraResidualTargetsFromBatchruntomo.isEmpty()) {
      newBatchruntomoSettings = true;
      setExtraResidualTargets(extraResidualTargetsFromBatchruntomo.toString());
      extraResidualTargetsFromBatchruntomo.reset();
    }
    if (!fiducialMatchFromBatchruntomo.isEmpty()) {
      newBatchruntomoSettings = true;
      setFiducialMatch(fiducialMatchFromBatchruntomo);
      fiducialMatchFromBatchruntomo.reset();
    }
    if (!autoPatchFinalSizeFromBatchruntomo.isEmpty()) {
      newBatchruntomoSettings = true;
      setPatchSize(true, autoPatchFinalSizeFromBatchruntomo);
      autoPatchFinalSizeFromBatchruntomo.reset();
    }
    if (!matchModeFromBatchruntomo.isEmpty()) {
      newBatchruntomoSettings = true;
      setMatchMode(matchModeFromBatchruntomo);
      matchModeFromBatchruntomo.reset();
    }
    if (!patchTypeOrXYZFromBatchruntomo.isEmpty()) {
      newBatchruntomoSettings = true;
      setPatchSize(false, patchTypeOrXYZFromBatchruntomo);
      patchTypeOrXYZFromBatchruntomo.reset();
    }
    if (!initialVolumeMatchingFromBatchruntomo.isNull()) {
      newBatchruntomoSettings = true;
      setInitialVolumeMatching(initialVolumeMatchingFromBatchruntomo.is());
      initialVolumeMatchingFromBatchruntomo.reset();
    }
  }

  public void setLowFromBothRadius(final String input) {
    lowFromBothRadius.set(input);
  }

  public void setMatchMode(final boolean isBtoA) {
    if (isBtoA) {
      matchMode = MatchMode.B_TO_A;
    }
    else {
      matchMode = MatchMode.A_TO_B;
    }
  }

  public void setMatchMode(final MatchMode matchMode) {
    this.matchMode = matchMode;
  }

  public void setMatchMode(final StringProperty input) {
    if (input != null) {

      matchMode = MatchMode.getInstance(input.toString());
    }
    else {
      matchMode = null;
    }
  }

  @SuppressWarnings("deprecation")
  public void setFiducialMatch(final FiducialMatch match) {
    fiducialMatch = match;
    if (match == FiducialMatch.USE_MODEL || match == FiducialMatch.USE_MODEL_ONLY) {
      modelBased = true;
    }
    else {
      modelBased = false;
    }
  }

  public void setFiducialMatch(final StringProperty input) {
    if (input != null) {
      fiducialMatch = FiducialMatch.getInstance(input.toString());
      if (fiducialMatch == null) {
        fiducialMatch = FiducialMatch.BOTH_SIDES;
      }
    }
    else {
      fiducialMatch = FiducialMatch.BOTH_SIDES;
    }
  }

  public void setUseList(final String useList) {
    this.useList.parseString(useList);
  }

  public void setFiducialMatchListA(final String list) {
    fiducialMatchListA.parseString(list);
  }

  public void setFiducialMatchListB(final String list) {
    fiducialMatchListB.parseString(list);
  }

  public void setWedgeReductionFraction(final String input) {
    wedgeReductionFraction.set(input);
  }

  public void setPatchSize(final boolean autoFinal, final CombinePatchSize input) {
    if (input != null) {
      if (!autoFinal) {
        patchSize.set(input.toString());
      }
      else {
        autoPatchFinalSize.set(input.toString());
      }
      if (input != CombinePatchSize.CUSTOM) {
        if (!autoFinal) {
          patchSizeXYZ.reset();
        }
        else {
          autoPatchFinalSizeXYZ.reset();
        }
      }
    }
    else if (!autoFinal) {
      patchSize.set(DEFAULT_PATCH_SIZE);
      patchSizeXYZ.reset();
    }
    else {
      autoPatchFinalSize.reset();
      autoPatchFinalSizeXYZ.reset();
    }
  }

  public void setPatchSize(final boolean autoFinal, final String input) {
    CombinePatchSize combinePatchSize = CombinePatchSize.getInstance(input);
    setPatchSize(autoFinal, combinePatchSize);
    if (combinePatchSize == CombinePatchSize.CUSTOM) {
      if (!autoFinal) {
        patchSizeXYZ.set(input);
      }
      else {
        autoPatchFinalSizeXYZ.set(input);
      }
    }
  }

  public void setPatchSizeXYZ(final boolean autoFinal, final String[] xyz) {
    if (xyz == null) {
      if (!autoFinal) {
        patchSize.set(CombinePatchSize.CUSTOM.toString());
        patchSizeXYZ.reset();
      }
      else {
        autoPatchFinalSize.set(CombinePatchSize.CUSTOM.toString());
        autoPatchFinalSizeXYZ.reset();
      }
    }
    CombinePatchSize combinePatchSize = CombinePatchSize.getInstance(xyz);
    if (!autoFinal) {
      patchSize.set(combinePatchSize.toString());
      if (patchSize.isEmpty()) {
        patchSize.set(DEFAULT_PATCH_SIZE);
      }
    }
    else {
      autoPatchFinalSize.set(combinePatchSize.toString());
    }
    if (combinePatchSize == CombinePatchSize.CUSTOM) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < xyz.length; i++) {
        builder.append(xyz[i]);
        if (i < xyz.length - 1) {
          builder.append(", ");
        }
      }
      if (!autoFinal) {
        patchSize.set(CombinePatchSize.CUSTOM.toString());
        patchSizeXYZ.set(builder.toString());
      }
      else {
        autoPatchFinalSize.set(CombinePatchSize.CUSTOM.toString());
        autoPatchFinalSizeXYZ.set(builder.toString());
      }
    }
  }

  public void setPatchSize(final boolean autoFinal, final StringProperty input) {
    if (input != null) {
      setPatchSize(autoFinal, input.toString());
    }
    else if (!autoFinal) {
      patchSize.set(DEFAULT_PATCH_SIZE);
      patchSizeXYZ.reset();
    }
    else {
      autoPatchFinalSize.reset();
      autoPatchFinalSizeXYZ.reset();
    }
  }

  public void setInitialVolumeMatching(final boolean input) {
    initialVolumeMatching.set(input);
  }

  public void setExtraResidualTargets(final String input) {
    extraResidualTargets.set(input);
  }

  public void resetExtraResidualTargets() {
    extraResidualTargets.reset();
  }

  public void resetPatchSize(final boolean autoFinal) {
    if (!autoFinal) {
      patchSize.set(DEFAULT_PATCH_SIZE);
    }
    else {
      autoPatchFinalSize.reset();
    }
  }

  public void setPatchRegionModel(final String modelFileName) {
    if (modelFileName.matches("^\\s+$")) {
      patchRegionModel = "";
    }
    else {
      patchRegionModel = modelFileName;
    }
  }

  public void setDefaultPatchRegionModel() {
    patchRegionModel = MatchorwarpParam.getDefaultPatchRegionModel();
  }

  /**
   * Sets the patchXMax.
   * @param patchXMax The patchXMax to set
   */
  public void setPatchXMax(final int patchXMax) {
    this.patchXMax = patchXMax;
  }

  /**
   * Sets the patchXMin.
   * @param patchXMin The patchXMin to set
   */
  public void setPatchXMin(final int patchXMin) {
    this.patchXMin = patchXMin;
  }

  /**
   * Sets the patchYMax.
   * @param patchYMax The patchYMax to set
   */
  public void setPatchYMax(final int patchYMax) {
    this.patchYMax = patchYMax;
  }

  /**
   * Sets the patchYMin.
   * @param patchYMin The patchYMin to set
   */
  public void setPatchYMin(final int patchYMin) {
    this.patchYMin = patchYMin;
  }

  /**
   * Sets the patchZMax.
   * @param patchZMax The patchZMax to set
   */
  public void setPatchZMax(final String patchZMax) {
    this.patchZMax.set(patchZMax);
  }

  /**
   * Sets the patchZMin.
   * @param patchZMin The patchZMin to set
   */
  public void setPatchZMin(final String patchZMin) {
    this.patchZMin.set(patchZMin);
  }

  public void setMaxPatchZMax(final String fileName)
    throws InvalidParameterException, IOException {

    // Get the data size limits from the image stack
    MRCHeader mrcHeader =
      MRCHeader.getInstance(manager.getPropertyUserDir(), fileName, AxisID.ONLY);
    if (!mrcHeader.read(manager)) {
      throw new IOException("file does not exist");
    }
    maxPatchZMax = mrcHeader.getNRows();
  }

  public void setMaxPatchZMax(final int maxPatchZMax) {
    this.maxPatchZMax = maxPatchZMax;
  }

  public void setTempDirectory(final String directoryName) {
    if (directoryName.matches("^\\s+$")) {
      tempDirectory = "";
    }
    else {
      tempDirectory = directoryName;
    }
  }

  public void setTransfer(final boolean transfer) {
    this.transfer = transfer;
  }

  public void setManualCleanup(final boolean isManual) {
    manualCleanup = isManual;
  }

  /**
   * Sets the modelBased state.
   * @param modelBased True if a model based combine is being used.
   */
  @SuppressWarnings("deprecation")
  public void setModelBased(final boolean modelBased) {
    this.modelBased = modelBased;
    if (modelBased) {
      fiducialMatch = FiducialMatch.USE_MODEL;
    }
    else {
      fiducialMatch = FiducialMatch.BOTH_SIDES;
    }
  }

  /**
   *  Insert the objects attributes into the properties object.
   */
  public void store(final Properties props) {
    store(props, "");
  }

  public void store(final Properties props, String prepend) {
    String group;
    if (prepend == "") {
      prepend = "Combine";
    }
    else {
      prepend = prepend + "Combine";
    }
    group = prepend + ".";
    revisionNumber = REVISION;
    props.setProperty(group + "RevisionNumber", revisionNumber);

    // Start backwards compatibility with RevisionNumber = 1.0
    props.remove(group + MATCH_B_TO_A_KEY);
    // backwards compatibility with 1.1
    props.remove(group + DIALOG_MATCH_MODE_KEY);
    // End backwards compatibility with RevisionNumber = 1.0

    // if (dialogMatchMode == null) {
    // props.remove(group + DIALOG_MATCH_MODE_KEY);
    // }
    // else {
    // props.setProperty(group + DIALOG_MATCH_MODE_KEY, dialogMatchMode
    // .toString());
    // }
    if (matchMode == null) {
      props.remove(group + MATCH_MODE_KEY);
    }
    else {
      props.setProperty(group + MATCH_MODE_KEY, matchMode.toString());
    }
    props.setProperty(group + "FiducialMatch", fiducialMatch.toString());
    props.setProperty(group + "UseList", useList.toString());
    props.setProperty(group + "FiducialMatchListA", fiducialMatchListA.toString());
    props.setProperty(group + "FiducialMatchListB", fiducialMatchListB.toString());
    patchSize.store(props, prepend);
    props.setProperty(group + "PatchBoundaryXMin", String.valueOf(patchXMin));
    props.setProperty(group + "PatchBoundaryXMax", String.valueOf(patchXMax));
    props.setProperty(group + "PatchBoundaryYMin", String.valueOf(patchYMin));
    props.setProperty(group + "PatchBoundaryYMax", String.valueOf(patchYMax));
    patchZMin.store(props, prepend);
    patchZMax.store(props, prepend);
    props.setProperty(group + "PatchRegionModel", patchRegionModel);
    props.setProperty(group + "TempDirectory", tempDirectory);
    props.setProperty(group + "ManualCleanup", String.valueOf(manualCleanup));
    props.setProperty(group + "ModelBased", String.valueOf(modelBased));
    props.setProperty(group + "Transfer", String.valueOf(transfer));
    props.setProperty(group + "MaxPatchBoundaryZMax", String.valueOf(maxPatchZMax));
    patchSizeXYZ.store(props, prepend);
    autoPatchFinalSize.store(props, prepend);
    autoPatchFinalSizeXYZ.store(props, prepend);
    extraResidualTargets.store(props, prepend);
    wedgeReductionFraction.store(props, prepend);
    lowFromBothRadius.store(props, prepend);
    initialVolumeMatching.store(props, prepend);
    // batchruntomo settings
    extraResidualTargetsFromBatchruntomo.store(props);
    fiducialMatchFromBatchruntomo.store(props);
    autoPatchFinalSizeFromBatchruntomo.store(props);
    matchModeFromBatchruntomo.store(props);
    patchTypeOrXYZFromBatchruntomo.store(props);
    initialVolumeMatchingFromBatchruntomo.store(props);
  }

  /**
   *  Get the objects attributes from the properties object.
   */
  public void load(final Properties props) {
    load(props, "");
  }

  @SuppressWarnings("deprecation")
  public void load(final Properties props, String prepend) {
    reset();
    String group;
    if (prepend == "") {
      prepend = "Combine";
    }
    else {
      prepend = prepend + "Combine";
    }
    group = prepend + ".";

    // Load the combine values if they are present, don't change the
    // current value if the property is not present

    revisionNumber = props.getProperty(group + "RevisionNumber", "1.2");

    // Start backwards compatibility with RevisionNumber = 1.0
    // load dialogMatchMode
    // old property was MatchBtoA. MatchBtoA should be deleted in store()
    String dialogMatchModeString = props.getProperty(group + DIALOG_MATCH_MODE_KEY);
    // backwards compatibility with 1.1
    MatchMode dialogMatchMode = null;
    if (dialogMatchModeString == null) {
      String matchBtoA = props.getProperty(group + MATCH_B_TO_A_KEY);
      if (matchBtoA == null) {
        dialogMatchMode = null;
      }
      else if ((Boolean.valueOf(matchBtoA).booleanValue())) {
        dialogMatchMode = MatchMode.B_TO_A;
      }
      else {
        dialogMatchMode = MatchMode.A_TO_B;
      }
    }
    else {
      MatchMode loadDialogMatchMode = MatchMode.getInstance(dialogMatchModeString);
      if (loadDialogMatchMode != null) {
        dialogMatchMode = loadDialogMatchMode;
      }
    }
    // End backwards compatibility with RevisionNumber = 1.0

    if (matchMode == null) {
      matchMode = MatchMode.getInstance(props.getProperty(group + MATCH_MODE_KEY));
      if (matchMode == null) {
        // backwards compatibility with 1.1
        matchMode = dialogMatchMode;
      }
    }
    else {
      matchMode = MatchMode
        .getInstance(props.getProperty(group + MATCH_MODE_KEY, matchMode.toString()));
    }
    fiducialMatch = FiducialMatch
      .fromString(props.getProperty(group + "FiducialMatch", fiducialMatch.toString()));

    useList.parseString(props.getProperty(group + "UseList", useList.toString()));

    fiducialMatchListA.parseString(
      props.getProperty(group + "FiducialMatchListA", fiducialMatchListA.toString()));

    fiducialMatchListB.parseString(
      props.getProperty(group + "FiducialMatchListB", fiducialMatchListB.toString()));

    patchSize.load(props, prepend, DEFAULT_PATCH_SIZE.toString());
    patchRegionModel = props.getProperty(group + "PatchRegionModel", patchRegionModel);

    patchXMin = Integer.parseInt(
      props.getProperty(group + "PatchBoundaryXMin", String.valueOf(patchXMin)));

    patchXMax = Integer.parseInt(
      props.getProperty(group + "PatchBoundaryXMax", String.valueOf(patchXMax)));

    patchYMin = Integer.parseInt(
      props.getProperty(group + "PatchBoundaryYMin", String.valueOf(patchYMin)));

    patchYMax = Integer.parseInt(
      props.getProperty(group + "PatchBoundaryYMax", String.valueOf(patchYMax)));
    patchZMin.load(props, prepend);
    patchZMax.load(props, prepend);

    tempDirectory = props.getProperty(group + "TempDirectory", tempDirectory);

    manualCleanup = Boolean
      .valueOf(
        props.getProperty(group + "ManualCleanup", Boolean.toString(manualCleanup)))
      .booleanValue();

    modelBased = Boolean
      .valueOf(props.getProperty(group + "ModelBased", Boolean.toString(modelBased)))
      .booleanValue();
    transfer =
      Boolean.valueOf(props.getProperty(group + "Transfer", Boolean.toString(transfer)))
        .booleanValue();

    if (fiducialMatch == FiducialMatch.USE_MODEL) {
      modelBased = true;
    }
    else {
      modelBased = false;
    }
    maxPatchZMax = Integer.parseInt(
      props.getProperty(group + "MaxPatchBoundaryZMax", String.valueOf(maxPatchZMax)));
    patchSizeXYZ.load(props, prepend);
    autoPatchFinalSize.load(props, prepend);
    autoPatchFinalSizeXYZ.load(props, prepend);
    extraResidualTargets.load(props, prepend);
    wedgeReductionFraction.load(props, prepend);
    lowFromBothRadius.load(props, prepend);
    initialVolumeMatching.load(props, prepend);
    // batchruntomo settings
    extraResidualTargetsFromBatchruntomo.load(props);
    fiducialMatchFromBatchruntomo.load(props);
    autoPatchFinalSizeFromBatchruntomo.load(props);
    matchModeFromBatchruntomo.load(props);
    patchTypeOrXYZFromBatchruntomo.load(props);
    initialVolumeMatchingFromBatchruntomo.load(props);
  }

  /**
   * Sets the patch boundaries to the default value that matches the logic in
   * the setupcombine script.
   * @param fileName The MRC iamge stack file name used to set the patch
   * boundaries.
   */
  public void setDefaultPatchBoundaries(final String fileName)
    throws InvalidParameterException, IOException {
    // Get the data size limits from the image stack
    MRCHeader mrcHeader =
      MRCHeader.getInstance(manager.getPropertyUserDir(), fileName, AxisID.ONLY);
    if (!mrcHeader.read(manager)) {
      return;
    }
    int xyborder = getXYBorder(mrcHeader);
    patchXMin = xyborder;
    patchXMax = mrcHeader.getNColumns() - xyborder;
    patchYMin = xyborder;
    patchYMax = mrcHeader.getNSections() - xyborder;
    patchZMin.set(1);
    patchZMax.set(mrcHeader.getNRows());
    maxPatchZMax = patchZMax.getInt();
  }

  /**
   * gets the border for xy using logic from setupcombine and the mrcheader
   * @param mrcHeader
   * @return
   */
  public static int getXYBorder(final MRCHeader mrcHeader) {
    // Logic from setupcombine to provide the default border size, the variable
    // names used match those from the setupcombine script
    int[] xyborders = { 24, 36, 54, 68, 80 };
    int borderinc = 1000;

    // Assume that Y and Z domains are swapped
    int minsize = Math.min(mrcHeader.getNColumns(), mrcHeader.getNSections());
    int borderindex = (int) (minsize / borderinc);
    if (borderindex > 4)
      borderindex = 4;
    return xyborders[borderindex];
  }

  public boolean isPatchSizeSet(final boolean autoFinal) {
    if (!autoFinal) {
      return !patchSize.isEmpty();
    }
    else {
      return !autoPatchFinalSize.isEmpty();
    }
  }

  public boolean isExtraResidualTargetsSet() {
    return !extraResidualTargets.isEmpty();
  }

  public boolean isInitialVolumeMatching() {
    return initialVolumeMatching.is();
  }

  public boolean isLowFromBothRadiusSet() {
    return !lowFromBothRadius.isEmpty();
  }

  public boolean isWedgeReductionFractionSet() {
    return !wedgeReductionFraction.isEmpty();
  }

  /**
   * Returns true if the patch boundary values have been modified
   */
  public boolean isPatchBoundarySet() {
    if (patchXMin == 0 && patchXMax == 0 && patchYMin == 0 && patchYMax == 0
      && patchZMin.equals(0) && patchZMax.equals(0)) {
      return false;
    }
    return true;
  }

  /**
   * Checks the validity of the attribute values.
   * @return true if all entries are valid, otherwise the reasons are 
   * available through the method getInvalidReasons.
   */
  public final boolean isValid(final boolean YAndZflipped) {
    boolean valid = true;
    // Clear any previous reasons from the list
    invalidReasons.clear();
    if (patchXMin < 1) {
      valid = false;
      invalidReasons.add("X min value is less than 1");
    }
    if (patchXMax < 1) {
      valid = false;
      invalidReasons.add("X max value is less than 1");
    }
    if (patchXMin > patchXMax) {
      valid = false;
      invalidReasons.add("X min value is greater than the X max value");
    }

    if (patchYMin < 1) {
      valid = false;
      invalidReasons.add("Y min value is less than 1");
    }
    if (patchYMax < 1) {
      valid = false;
      invalidReasons.add("Y max value is less than 1");
    }
    if (patchYMin > patchYMax) {
      valid = false;
      invalidReasons.add("Y min value is greater than the Y max value");
    }

    if (patchZMin.getInt() < 1) {
      valid = false;
      invalidReasons.add("Z min value is less than 1");
    }
    if (patchZMax.getInt() < 1) {
      valid = false;
      invalidReasons.add("ZX max value is less than 1");
    }
    if (maxPatchZMax > 0 && patchZMax.gt(maxPatchZMax)) {
      valid = false;
      invalidReasons.add(
        "Z max value is greater than the maximum Z max value (" + maxPatchZMax + ")");
    }
    if (patchZMin.gt(patchZMax)) {
      valid = false;
      invalidReasons.add("Z min value is greater than the Z max value");
    }
    // get the tomogram header to check x, y, and z
    AxisID axisID;
    // if (dialogMatchMode == null || dialogMatchMode == MatchMode.B_TO_A) {
    // axisID = AxisID.FIRST;
    // }
    // else {
    // axisID = AxisID.SECOND;
    // }
    if (matchMode == null || matchMode == MatchMode.B_TO_A) {
      axisID = AxisID.FIRST;
    }
    else {
      axisID = AxisID.SECOND;
    }
    MRCHeader header = MRCHeader.getInstance(manager.getPropertyUserDir(),
      DatasetFiles.getTomogram(manager, axisID).getAbsolutePath(), axisID);
    try {
      if (!header.read(manager)) {
        return true;
      }
    }
    catch (IOException e) {
      return true;
    }
    catch (Exception e) {
      e.printStackTrace();
      return true;
    }
    int x = header.getNColumns();
    if (x < patchXMin || x < patchXMax) {
      valid = false;
      invalidReasons.add("X values cannot be greater then " + x);
    }
    int y;
    int z;
    if (YAndZflipped) {
      y = header.getNSections();
      z = header.getNRows();
    }
    else {
      y = header.getNRows();
      z = header.getNSections();
    }
    if (y < patchYMin || y < patchYMax) {
      valid = false;
      invalidReasons.add("Y values cannot be greater then " + y);
    }

    if (patchZMin.gt(z) || patchZMax.gt(z)) {
      valid = false;
      invalidReasons.add("Z values cannot be greater then " + z);
    }
    return valid;
  }

  /**
   * Returns the reasons the attribute values are invalid as a string array.
   */
  public final String[] getInvalidReasons() {
    return (String[]) invalidReasons.toArray(new String[invalidReasons.size()]);
  }

  public MatchMode getMatchMode() {
    return matchMode;
  }

  public boolean isTransfer() {
    return transfer;
  }

  public FiducialMatch getFiducialMatch() {
    return fiducialMatch;
  }

  public String getUseList() {
    return useList.toString();
  }

  public String getFiducialMatchListA() {
    return fiducialMatchListA.toString();
  }

  public String getFiducialMatchListB() {
    return fiducialMatchListB.toString();
  }

  public String getPatchRegionModel() {
    return patchRegionModel;
  }

  public CombinePatchSize getPatchSize(final boolean autoFinal) {
    if (!autoFinal) {
      return CombinePatchSize.getInstance(patchSize.toString());
    }
    else {
      return CombinePatchSize.getInstance(autoPatchFinalSize.toString());
    }
  }

  public String getPatchSizeXYZ(final boolean autoFinal) {
    if (!autoFinal) {
      return patchSizeXYZ.toString();
    }
    else {
      return autoPatchFinalSizeXYZ.toString();
    }
  }

  public String[] getPatchSizeXYZArray(final boolean autoFinal) {
    String xyz;
    if (!autoFinal) {
      xyz = patchSizeXYZ.toString();
    }
    else {
      xyz = autoPatchFinalSizeXYZ.toString();
    }
    if (xyz == null) {
      return null;
    }
    return xyz.split("\\s*,\\s*");
  }

  public String getExtraResidualTargets() {
    return extraResidualTargets.toString();
  }

  public String getLowFromBothRadius() {
    return lowFromBothRadius.toString();
  }

  public String getWedgeReductionFraction() {
    return wedgeReductionFraction.toString();
  }

  public String getTempDirectory() {
    return tempDirectory;
  }

  public boolean isTempDirectorySet() {
    return !tempDirectory.equals("");
  }

  public boolean getManualCleanup() {
    return manualCleanup;
  }

  /**
   * Returns the patchXMax.
   * @return int
   */
  public int getPatchXMax() {
    return patchXMax;
  }

  /**
   * Returns the patchXMin.
   * @return int
   */
  public int getPatchXMin() {
    return patchXMin;
  }

  /**
   * Returns the patchYMax.
   * @return int
   */
  public int getPatchYMax() {
    return patchYMax;
  }

  /**
   * Returns the patchYMin.
   * @return int
   */
  public int getPatchYMin() {
    return patchYMin;
  }

  /**
   * Returns the patchZMax.
   * @return int
   */
  public ConstEtomoNumber getPatchZMax() {
    return patchZMax;
  }

  /**
   * Returns the patchZMin.
   * @return int
   */
  public ConstEtomoNumber getPatchZMin() {
    return patchZMin;
  }

  public int getMaxPatchZMax() {
    return maxPatchZMax;
  }

  /**
   * Returns true if a patch region model has been specified.
   * @return boolean
   */
  public boolean usePatchRegionModel() {
    return !patchRegionModel.matches("^\\s*$");
  }
}