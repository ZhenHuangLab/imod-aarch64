package etomo.ui.swing;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.SwingUtilities;

/**
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright 2002 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 3.1  2006/01/12 17:11:20  sueh
 * <p> bug# 798 Reducing the visibility and inheritability of ui classes.
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:01  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.1  2003/10/15 16:38:22  rickg
 * <p> Responds to pressed instead clicked right mouse button events now
 * <p>
 * <p> Revision 2.0  2003/01/24 20:30:31  rickg
 * <p> Single window merge to main branch
 * <p>
 * <p> Revision 1.1.2.1  2003/01/24 18:43:37  rickg
 * <p> Single window GUI layout initial revision
 * <p>
 * <p> Revision 1.1  2002/09/09 22:57:02  rickg
 * <p> Initial CVS entry, basic functionality not including combining
 * <p> </p>
 */
public final class GenericMouseAdapter implements MouseListener {
  private final ContextMenu adaptee;

  public GenericMouseAdapter(final ContextMenu adaptee) {
    this.adaptee = adaptee;
  }

  public void mouseClicked(final MouseEvent event) {}

  public void mousePressed(final MouseEvent event) {
    if (SwingUtilities.isRightMouseButton(event))
      adaptee.popUpContextMenu(event);
  }

  public void mouseReleased(final MouseEvent event) {}

  public void mouseEntered(final MouseEvent event) {}

  public void mouseExited(final MouseEvent event) {}
}
