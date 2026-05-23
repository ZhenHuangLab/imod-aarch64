package etomo.process;

import etomo.process.ProcessMessages.ListType;
import etomo.process.ProcessMessages.MessageType;

final class Message {
  private final StringBuilder builder = new StringBuilder();

  private final TagInterface tag;

  private boolean endWithNewLine = false;

  Message(final TagInterface tag) {
    this.tag = tag;
    endWithNewLine = tag.isMultiLine();
  }

  public String toString() {
    return builder.toString();
  }

  /**
   * Returns message.  If endWithNewLine is true, a new line is appended at the end of the
   * message before returning and endWithNewLine is turned off.  This means that there is
   * an assumption that this function is only called when the message is complete.  This
   * appears to be true right now.  If something changes, add a "completeMessage" boolean
   * parameter to this function.
   * @return
   */
  String getMessageString() {
    if (endWithNewLine) {
      builder.append("\n");
      endWithNewLine = false;
    }
    return builder.toString();
  }

  void append(final String string) {
    if (string == null) {
      return;
    }
    if (builder.length() > 0) {
      builder.append("\n");
    }
    builder.append(string);
  }

  boolean equalsEndTag(final TagInterface tag) {
    return this.tag.equalsEndTag(tag);
  }

  boolean isEnclosed() {
    return tag.isEnclosed();
  }

  MessageType getMessageType() {
    return tag.getMessageType();
  }

  ListType getListType() {
    return tag.getListType();
  }

  boolean isChunk() {
    return tag.isChunk();
  }
}
