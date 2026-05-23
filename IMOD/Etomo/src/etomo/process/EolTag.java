package etomo.process;

import etomo.process.ProcessMessages.ListType;
import etomo.process.ProcessMessages.MessageType;

/**
 * <p>Description: Simple, non-blocking tag parser which parses a tag at the end of the
 * line.  Strips the tag and truncates text at the tag on the the line where the tag was
 * found.  Can be multi-line.  Multi-line messages don't block other messages and end with
 * an empty line.  Does not respond to mode changes.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class EolTag implements TagInterface {
  private final MessageType type;
  private final ListType listType;
  private final String tag;
  private final boolean multiLine;

  private String line = null;
  private int endIndex = -1;
  private boolean open = false;
  private boolean closed = false;

  EolTag(final MessageType type, final String tag, final boolean multiLine,
    final ListType listType) {
    this.type = type;
    this.tag = tag;
    this.multiLine = multiLine;
    this.listType = listType;
  }

  public String toString() {
    return getClass() + "," + type + "," + tag + "," + line;
  }

  /**
   * Sets line and finds the message in the line if it recognizes the tag.  Returns true
   * when it finds the tag,
   * @param line
   * @param parseIndex
   * @return
   */
  public boolean parse(final String line) {
    reset(line);
    if (line == null) {
      return false;
    }
    if ((endIndex = line.indexOf(tag)) != -1) {
      // Strip end tag
      open = true;
      if (!multiLine) {
        closed = true;
      }
      return true;
    }
    return false;
  }

  /**
   * Get the message.
   * @return
   */
  public final String getMessageString() {
    if (line == null || endIndex == -1) {
      return null;
    }
    return line.substring(0, endIndex).trim();
  }

  public void deleteMessageString() {
    reset(null);
  }

  private void reset(final String line) {
    this.line = line;
    endIndex = -1;
    open = false;
    closed = false;
  }

  public boolean isOpen() {
    return open;
  }

  public boolean isClosed() {
    return closed;
  }

  public boolean isChunk() {
    return listType != null && listType.isChunk();
  }

  public boolean isMultiLine() {
    return multiLine;
  }

  public boolean isFlag() {
    return false;
  }

  public boolean isPrepend() {
    return false;
  }

  public boolean isEnclosed() {
    return false;
  }

  /**
   * No endTag - return false.  And eol tag is a start tag which comes at the end of the
   * line.
   */
  public boolean equalsEndTag(TagInterface tag) {
    return false;
  }

  public void setPrepend(final PrependTag prepend) {}

  public void deletePrepend() {}

  public boolean takesPrepend() {
    return false;
  }

  public MessageType getMessageType() {
    return type;
  }

  public ListType getListType() {
    return listType;
  }
}
