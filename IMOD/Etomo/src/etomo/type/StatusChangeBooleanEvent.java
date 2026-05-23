package etomo.type;

/**
 * <p>Description: Bundles status with a boolean value.</p>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class StatusChangeBooleanEvent implements StatusChangeEvent {
  private final boolean bool;
  private final Status status;

  public StatusChangeBooleanEvent(final boolean bool, final Status status) {
    this.bool = bool;
    this.status = status;
  }

  public boolean is() {
    return bool;
  }

  public Status getStatus() {
    return status;
  }

  public String toString() {
    return "[bool:" + bool + ",status:" + status;
  }
}
