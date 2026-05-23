package etomo.ui.swing;

import java.io.File;

interface ControlTarget {
  public void clear();

  public void setText(File file);

  public String getLabel();

  public void setComponentControl(boolean control, ControlState state);

  public void setEnableControl(boolean control, ControlState state);

  public void sendControlEvent();
}
