package etomo.ui;

import etomo.type.Run3dmodMenuOptions;
import etomo.ui.swing.SwingComponent;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2014</p>
*
* <p>Organization:
* Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
* University of Colorado</p>
* 
* @author $Author$
* 
* @version $Revision$
* 
* <p> $Log$ </p>
*/
public interface Run3dmodMenuTarget extends SwingComponent {
  public static final String rcsid = "$Id:$";

  public void menuAction(Run3dmodMenuOptions run3dmodMenuOptions);

  public boolean isEnabled();
}
