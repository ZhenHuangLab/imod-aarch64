package etomo.logic;

import java.util.ArrayList;

/**
 * <p>Description: Handles message wrapping.</p>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class PopupTool {
  private static final int MAX_LINES = 20;
  private static final int WIDTH = 60;

  public static ArrayList<String> linefeed(ArrayList<String> messageArray) {
    if (messageArray == null) {
      messageArray = new ArrayList<String>();
    }
    messageArray.add(" ");
    return messageArray;
  }

  /**
   * Wrap a message and add it to messageArray.  Multiple messages may be added to
   * messageArray.  MessageArray may be passed in as null.
   *
   * @param message
   * @param messageArray
   * @return messageArray
   */
  public static ArrayList<String> wrapMessage(final String message,
    ArrayList<String> messageArray) {
    if (messageArray == null) {
      messageArray = new ArrayList<String>();
    }
    if (message == null) {
      if (messageArray.size() == 0) {
        messageArray.add(" ");
      }
      return messageArray;
    }
    if (message.equals("\n")) {
      messageArray.add(" ");
      return messageArray;
    }
    // first - break up the message piece by line
    String[] lineArray = message.split("\n");
    // second - break up each line by maximum length
    for (int i = 0; i < lineArray.length; i++) {
      // handle empty lines
      if (lineArray[i] == null || lineArray[i].length() == 0) {
        messageArray.add(" ");
      }
      else {
        int len = lineArray[i].length();
        int index = 0;
        while (index < len && messageArray.size() < MAX_LINES) {
          int endIndex = Math.min(len, index + WIDTH);
          StringBuilder newLine =
            new StringBuilder(lineArray[i].substring(index, endIndex));
          // overflowing line - look for whitespace or a comma
          index = endIndex;
          char lastChar = ' ';
          while (index < len && lineArray[i].substring(index, index + 1).matches("\\S+")
            && lastChar != ',') {
            lastChar = lineArray[i].charAt(index++);
            newLine.append(lastChar);
          }
          messageArray.add(newLine.toString());
        }
      }
    }
    return messageArray;
  }

  /**
   * Makes sure the messages isn't too high or low.
   * @param parentY
   * @param parentHeight
   * @param messageHeight
   * @return
   */
  public static int adjustLocationY(int popupY, final int parentHeight,
    final int popupHeight) {
    // In RedHat the popup appears to center itself over the center of the field. Raise
    // the popup so it is a few pixels above the field. If the new location is off the
    // monitor (negative y), set it to the top of monitor.
    popupY -= (parentHeight / 2) + (popupHeight / 2) + 8;
    if (popupY < 0) {
      popupY = 0;
    }
    return popupY;
  }
}
