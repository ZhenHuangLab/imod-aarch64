package etomo.ui.swing;

import java.io.File;

interface Controller {
  public boolean isControl();

  public void setEditable(boolean editable);

  public void setEnabled(boolean enabled);
  
  public File selectFile();
}
