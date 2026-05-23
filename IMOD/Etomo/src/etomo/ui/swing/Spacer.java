package etomo.ui.swing;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;

/**
* <p>Description: A rigid area for spacing components in a panel.  Can be queried.</p>
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
final class Spacer {
  public static final String rcsid = "$Id:$";

  private final Component rigidArea;
  private final int preferredWidth;

  Spacer(final Dimension dimension) {
    rigidArea = Box.createRigidArea(dimension);
    preferredWidth = dimension.width;
  }

  int getPreferredWidth() {
    return preferredWidth;
  }

  Component getComponent() {
    return rigidArea;
  }
}
