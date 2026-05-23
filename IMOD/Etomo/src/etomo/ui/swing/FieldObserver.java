package etomo.ui.swing;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2011</p>
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
@Deprecated // 8/4/2018
public interface FieldObserver {
  public static final String rcsid = "$Id$";

  @Deprecated // 8/4/2018
  public void msgFieldChanged(final boolean differentFromCheckpoint);
}
