package etomo.type;

/**
* <p>Description: An event with an identifying tag.  Also contains an extra, optional
* string.</p>
* 
* <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public class StatusChangeTaggedEvent implements StatusChangeEvent {
  private final String tag;
  private final String string;

  private Status status = null;

  public StatusChangeTaggedEvent(final String tag, final Status status) {
    this.tag = tag;
    string = null;
    this.status = status;
  }

  public StatusChangeTaggedEvent(final String tag, final String string,
    final Status status) {
    this.tag = tag;
    this.string = string;
    this.status = status;
  }

  public String toString() {
    return "[tag:" + tag + ",string:" + string + ",status:" + status + "]";
  }

  public boolean equals(final String input) {
    return (tag == null && input == null) || (tag != null && tag.equals(input));
  }

  public void setStatus(final Status status) {
    this.status = status;
  }

  public String getString() {
    return string;
  }

  public Status getStatus() {
    return status;
  }
}
