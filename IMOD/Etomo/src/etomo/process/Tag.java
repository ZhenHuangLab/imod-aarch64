package etomo.process;

import etomo.process.ProcessMessages.ListType;
import etomo.process.ProcessMessages.MessageType;

/**
 * <p>Description: Simple, non-blocking tag parser.  Looks for a single tag and considers
 * the tag and the rest of the line to be the message.  Can be multi-line.  Multi-line
 * messages don't block other messages and end with an empty line.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
class Tag implements TagInterface {
  private final MessageType type;
  private final ListType listType;
  private final boolean multiLine;
  private final boolean stripTag;
  private final boolean takesPrepend;
  private final String[] antitags;

  private String tag = null;
  private String prependLine = null;
  private PrependTag prepend = null;
  private String line = null;
  private int startIndex = -1;
  private boolean open = false;
  private boolean closed = false;

  Tag(final MessageType type, final String tag, final boolean multiLine,
    final boolean stripTag, final ListType listType, final boolean takesPrepend) {
    this.type = type;
    this.tag = tag;
    this.multiLine = multiLine;
    this.listType = listType;
    this.stripTag = stripTag;
    this.takesPrepend = takesPrepend;
    antitags = null;
  }

  Tag(final MessageType type, final String tag, final boolean multiLine,
    final boolean stripTag, final ListType listType, final boolean takesPrepend,
    final String[] antitags) {
    this.type = type;
    this.tag = tag;
    this.multiLine = multiLine;
    this.listType = listType;
    this.stripTag = stripTag;
    this.takesPrepend = takesPrepend;
    this.antitags = antitags;
  }

  public String toString() {
    return getClass() + "," + type + "," + tag + "," + line;
  }

  /**
   * Parse for tag.  Returns true if line.  Empty lines ignored.
   * @param line
   * @param parseIndex
   * @return
   */
  public boolean parse(final String line) {
    reset(line);
    if (tag == null || line == null || line.isEmpty()) {
      return false;
    }
    // Ignore lines containing antitags.
    if (antitags != null) {
      for (int i = 0; i < antitags.length; i++) {
        if (line.indexOf(antitags[i]) != -1) {
          return false;
        }
      }
    }
    if ((startIndex = line.indexOf(tag)) != -1) {
      // Found the start tag of a message
      if (stripTag) {
        startIndex += tag.length();
      }
      open = true;
      if (!multiLine) {
        closed = true;
      }
      return true;
    }
    return false;
  }

  void reset(final String line) {
    this.line = line;
    startIndex = -1;
    open = false;
    closed = false;
  }

  void setClosed() {
    closed = true;
  }

  void setStartIndex(final int startIndex) {
    this.startIndex = startIndex;
  }

  /**
   * Changes the tag.  If the tag is null, this has no effect.  A tag can't be set in an
   * empty instance (use copy first).  Deletes current message found with old tag.
   * @param newTag
   */
  void setTag(final String newTag) {
    if (newTag == null) {
      return;
    }
    tag = newTag;
    deleteMessageString();
  }

  void deleteTag() {
    tag = null;
    deleteMessageString();
  }

  public void deleteMessageString() {
    reset(null);
  }

  String addPrepend(final String messageString) {
    if (!open || !takesPrepend || prependLine == null) {
      return messageString;
    }
    String prependString = prependLine;
    prepend.deleteMessageString();
    if (prependString == null || prependString.isEmpty()) {
      return messageString;
    }
    if (messageString != null) {
      return prependString + "\n" + messageString;
    }
    return prependString;
  }

  /**
   * Get the message.
   * @return
   */
  public String getMessageString() {
    String messageString = null;
    if (line != null && startIndex != -1) {
      if (startIndex == 0) {
        messageString = line;
      }
      else {
        messageString = line.substring(startIndex);
      }
    }
    if (messageString == null) {
      return null;
    }
    return addPrepend(messageString.trim());
  }

  int getTagLength() {
    if (tag == null) {
      return 0;
    }
    return tag.length();
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

  public ListType getListType() {
    return listType;
  }

  public final boolean isMultiLine() {
    return multiLine;
  }

  public boolean isPrepend() {
    return false;
  }

  public boolean isFlag() {
    return false;
  }

  public boolean isEnclosed() {
    return false;
  }

  public boolean equalsEndTag(TagInterface tag) {
    return false;
  }

  public MessageType getMessageType() {
    return type;
  }

  public void setPrepend(final PrependTag prepend) {
    if (!takesPrepend || prepend == null) {
      return;
    }
    this.prepend = prepend;
    this.prependLine = prepend.getMessageString();
  }

  public void deletePrepend() {
    prepend = null;
    prependLine = null;
  }

  public boolean takesPrepend() {
    return takesPrepend;
  }

  final String getLine() {
    return line;
  }

  final int getStartIndex() {
    return startIndex;
  }
}
