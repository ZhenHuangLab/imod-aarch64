package etomo.ui;

import etomo.comscript.BatchruntomoParam;
import etomo.type.BatchRunTomoMetaData;

public interface BatchRunTomoDialogInterface {
  public void updateDirectives(boolean init);

  public void setParameters(BatchRunTomoMetaData metaData);

  public void setParameters(BatchruntomoParam param);

  public void loadAutodocs();
}
