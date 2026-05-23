package etomo.ui;

/**
 * <p>Description: Listener to be responded to when a dialog is complete.</p>
 * 
 * <p>Copyright: Copyright 2022 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class DialogCompleteListener {
  private final DialogCompleteClient client;
  private final String string;

  /**
   * @param client instance to be informed when msgDialogComplete is called.
   * @param string whatever information needs to be passed back to the client.
   */
  public DialogCompleteListener(final DialogCompleteClient client, final String string) {
    this.client = client;
    this.string = string;
  }

  public void msgDialogComplete() {
    if (client != null) {
      client.msgDialogComplete(string);
    }
  }
}
