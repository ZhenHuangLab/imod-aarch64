package etomo.process;

import etomo.process.ProcessMessages.ListType;
import etomo.process.ProcessMessages.MessageType;

interface TagInterface {
  public void deleteMessageString();

  public String getMessageString();

  public MessageType getMessageType();
  
  /**
   * Returns true if both tags have the same, non-null endTag.
   * @param tag
   * @return
   */
  public boolean equalsEndTag(TagInterface tag) ;

  /**
   * Return true if this message as a chunk error or warning.
   * @return
   */
  public boolean isChunk();

  /**
   * List type defines where the tag's message with be stored.
   * @return
   */
  public ListType getListType();

  /**
   * Returns true if the tag instance can get a prepend from another tag.
   * @return
   */
  public boolean takesPrepend();

  /**
   * Sets the prepend line.
   * @param tag
   */
  public void setPrepend(PrependTag prepend);
  
  /**
   * Deletes the prepend because it was already used by a tag.
   */
  public void deletePrepend();
  /**
   * Returns true if the instance is a prepend rather then an independent message tag.
   * @return
   */
  public boolean isPrepend();

  /**
   * Returns true if the tag stops with an end tag rather then an empty line.
   * @return
   */
  public boolean isEnclosed();

  /**
   * Sets the line and reinitialized message indices and settings.  Sets indices and
   * settings.  Returns true in the message was opened or closed on the line.
   * @param line
   * @param parseIndex
   * @return
   */
  public boolean parse(String line);

  /**
   * Returns true if the tag can have multi-line messages.
   * @return
   */
  public boolean isMultiLine();

  /**
   * True when the start of the message is found.
   * @return
   */
  public boolean isOpen();

  /**
   * True when the end of a message is found.
   * @return
   */
  public boolean isClosed();

  /**
   * True for a tag that is a flag.  A flag tag does not return a message.
   * @return
   */
  public boolean isFlag();
}
