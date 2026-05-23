package etomo.comscript;

import etomo.BaseManager;
import etomo.BatchRunTomoManager;
import etomo.type.AxisID;
import etomo.type.FileType;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2014 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class BatchRunTomoComScriptManager {
  private final BaseManager manager;

  private ComScript scriptBatchRunTomo = null;

  public BatchRunTomoComScriptManager(final BatchRunTomoManager manager) {
    this.manager = manager;
  }

  public void loadBatchRunTomo(final AxisID axisID) {
    scriptBatchRunTomo = ComScriptUtil.loadComScript(manager,
      FileType.BATCH_RUN_TOMO_COMSCRIPT.getFileName(manager, axisID), axisID, true, true,
      false, false);
  }

  public void loadBatchRunTomo(final AxisID axisID, final String rootName) {
    scriptBatchRunTomo = ComScriptUtil.loadComScript(manager,
      FileType.BATCH_RUN_TOMO_COMSCRIPT.getFileName(manager, rootName, axisID), axisID,
      true, true, false, false);
  }

  public boolean isBatchRunTomoLoaded() {
    return scriptBatchRunTomo != null;
  }

  public void saveBatchRunTomo(final BatchruntomoParam param, final AxisID axisID) {
    ComScriptUtil.modifyCommand(manager, scriptBatchRunTomo, param, "batchruntomo",
      axisID, false, false);
  }

  public BatchruntomoParam getBatchRunTomoParam(final AxisID axisID,
    final boolean doValidation) {
    BatchruntomoParam param = BatchruntomoParam.getInstance(manager, axisID);
    ComScriptUtil.initialize(manager, param, scriptBatchRunTomo, "batchruntomo", axisID,
      false, false, true);
    return param;
  }
}
