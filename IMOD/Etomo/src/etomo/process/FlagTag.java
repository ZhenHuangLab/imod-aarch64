package etomo.process;

import etomo.process.ProcessMessages.ListType;
import etomo.process.ProcessMessages.MessageType;

/**
 * <p>Description: A tag with no message.  Just used for setting a flag.  Has up to two
 * tags, which must be in order, not overlap, and be on the same line.  Does not respond
 * to mode changes.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class FlagTag implements TagInterface {
  private final MessageType type;
  private final String tag1;
  private final String tag2;
  private final ListType listType;

  // Settings
  private boolean found = false;

  /**
   * Tag1 is required.  Tag2 is optional.
   * @param type
   * @param tag1
   * @param tag2
   * @param listType
   */
  FlagTag(final MessageType type, final String tag1, final String tag2,
    final ListType listType) {
    this.type = type;
    this.tag1 = tag1;
    this.tag2 = tag2;
    this.listType = listType;
  }

  public String toString() {
    return getClass() + "," + type + "," + tag1 + "," + tag2;
  }

  /**
   * Must find all set tags to return true.
   * @param line
   * @param parseIndex
   * @return
   */
  public boolean parse(final String line) {
    reset();
    if (line == null || line.isEmpty()) {
      return false;
    }
    int index = -1;
    if ((index = line.indexOf(tag1)) != -1) {
      found = true;
      if (tag2 != null) {
        index += tag1.length();
        if (line.indexOf(tag2, index) == -1) {
          found = false;
        }
      }
    }
    return found;
  }

  public void deleteMessageString() {
    reset();
  }

  private void reset() {
    found = false;
  }

  public boolean isFlag() {
    return true;
  }

  public MessageType getMessageType() {
    return type;
  }

  public ListType getListType() {
    return listType;
  }

  public TagInterface getPrepend() {
    return null;
  }

  public String getMessageString() {
    return null;
  }

  public boolean isPrepend() {
    return false;
  }

  public boolean isOpen() {
    return found;
  }

  public boolean isClosed() {
    return found;
  }

  public boolean isMultiLine() {
    return false;
  }

  public boolean takesPrepend() {
    return false;
  }

  public boolean isChunk() {
    return false;
  }

  public boolean isEnclosed() {
    return false;
  }

  public boolean equalsEndTag(TagInterface tag) {
    return false;
  }

  public void setPrepend(final PrependTag prepend) {}

  public void deletePrepend() {}
}
