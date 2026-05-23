package etomo.process;

import etomo.process.ProcessMessages.ListType;
import etomo.process.ProcessMessages.MessageType;

/**
 * <p>Description: Tag parser for a multi-line message that is only closed by an end tag.
 * Line containing end tag is trucated after end tag. </p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class EnclosedTag extends Tag {
  private final String endTag;
  private final boolean stripEndTag;

  // Message settings
  private int endIndex = -1;

  EnclosedTag(final MessageType type, final String tag, final String endTag,
    final boolean stripEndTag, final ListType listType, final boolean takesPrepend) {
    super(type, tag, true, false, listType, takesPrepend);
    this.endTag = endTag;
    this.stripEndTag = stripEndTag;
  }

  /**
   * Looks for both tag and end-tag.  Return true if one or both where found.  Sets found,
   * open and close booleans.
   * @param line
   * @param parseIndex
   * @return
   */
  public final boolean parse(final String line) {
    reset(line);
    boolean openFound = super.parse(line);
    if (line == null || line.isEmpty()) {
      return false;
    }
    // Parse for the end tag
    int index = 0;
    if (openFound) {
      index = getStartIndex() + getTagLength();
    }
    if ((endIndex = line.indexOf(endTag, index)) != -1) {
      if (!stripEndTag) {
        endIndex += endTag.length();
      }
      setClosed();
      return true;
    }
    return openFound;
  }

  void reset(final String line) {
    super.reset(line);
    endIndex = -1;
  }

  /**
   * Get the message.
   * @return
   */
  public final String getMessageString() {
    String line = getLine();
    int startIndex = getStartIndex();
    String messageString = null;
    if (line != null && (startIndex != -1 || endIndex != -1)) {
      if (startIndex != -1 && endIndex != -1) {
        messageString = line.substring(startIndex, endIndex);
      }
      else if (endIndex != -1) {
        messageString = line.substring(0, endIndex);
      }
      else {
        // already contains prepend;
        return super.getMessageString();
      }
    }
    if (messageString == null) {
      return null;
    }
    return addPrepend(messageString.trim());
  }

  public boolean isEnclosed() {
    return true;
  }

  public boolean equalsEndTag(TagInterface tag) {
    if (tag == null) {
      return false;
    }
    if (tag == this) {
      return true;
    }
    if (tag instanceof EnclosedTag) {
      EnclosedTag enclosedTag = (EnclosedTag) tag;
      return endTag.equals(enclosedTag.endTag);
    }
    else if (tag.isEnclosed()) {
      System.err.println("ERROR: Unknown enclosed tag.");
      Thread.dumpStack();
    }
    return false;
  }
}
