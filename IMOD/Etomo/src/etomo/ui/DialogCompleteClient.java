package etomo.ui;

/**
 * <p>Description: Client that DialogCompleteListener will contact.</p>
 * 
 * <p>Copyright: Copyright 2022 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface DialogCompleteClient {
  public void msgDialogComplete(final String string);
}
