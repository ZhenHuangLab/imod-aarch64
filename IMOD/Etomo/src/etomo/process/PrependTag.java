package etomo.process;

import java.util.ArrayList;
import java.util.List;

import etomo.process.ProcessMessages.MessageType;

/**
 * <p>Description: Parser for prepends.  Looks for a prepend tag and considers
 * the whole line to be the message.  Never multi-line</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class PrependTag extends Tag {
  private final List<TagInterface> prependTakers = new ArrayList<TagInterface>();

  PrependTag(final String tag) {
    super(MessageType.PREPEND, tag, false, false, null, false);
  }

  void addPrependTaker(final TagInterface tag) {
    prependTakers.add(tag);
  }

  /**
   * Parse for tag.  Returns true if line.  Empty lines ignored.
   * @param line
   * @param parseIndex
   * @return
   */
  public boolean parse(final String line) {
    if (super.parse(line)) {
      setStartIndex(0);
      int size = prependTakers.size();
      for (int i = 0; i < size; i++) {
        prependTakers.get(i).setPrepend(this);
      }
      return true;
    }
    return false;
  }

  public String getMessageString() {
    String messageString = null;
    String line = getLine();
    if (line != null && getStartIndex() != -1) {
      // Whole line is part of prepend
      messageString = line;
    }
    // Prepend is not trimmed
    return messageString;
  }

  public void deleteMessageString() {
    reset(null);
    int size = prependTakers.size();
    for (int i = 0; i < size; i++) {
      prependTakers.get(i).deletePrepend();
    }
  }

  public boolean isPrepend() {
    return true;
  }

  public void setPrepend(final PrependTag prepend) {}

  public void deletePrepend() {}
}
