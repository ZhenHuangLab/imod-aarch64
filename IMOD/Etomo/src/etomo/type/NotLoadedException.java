package etomo.type;

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
public final class NotLoadedException extends Exception {
  public static final String rcsid = "$Id:$";

  NotLoadedException(final String message) {
    super(message);
  }
}
