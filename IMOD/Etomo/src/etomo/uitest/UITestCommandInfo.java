package etomo.uitest;

import etomo.type.AxisID;

interface UITestCommandInfo {
  public String formatCommandInfo(AxisID axisID);

  public String getFieldName();
}
