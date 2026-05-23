package etomo.type;

import java.io.IOException;

import etomo.storage.LogFile;
import etomo.ui.swing.Token;
import etomo.util.PrimativeTokenizer;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2006</p>
 *
 * <p>Organization:
 * Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
 * University of Colorado</p>
 * 
 * @author $Author$
 * 
 * @version $Revision$
 * 
 * <p> $Log$
 * <p> Revision 1.15  2009/02/04 23:30:30  sueh
 * <p> bug# 1158 Changed id and exception classes in LogFile.
 * <p>
 * <p> Revision 1.14  2008/09/10 21:06:34  sueh
 * <p> bug# 1135 Check for null when calling ParsedElementList.get(int).  Check
 * <p> for null when calling ParsedElement.getElement or getRawNumber.
 * <p> arsedElementList will no longer create an empty element, so null returns
 * <p> will happen.  Remove all the array size functionality because the array descriptor will not automatically be padded out to three.
 * <p>
 * <p> Revision 1.13  2008/04/15 21:23:32  sueh
 * <p> bug# 1105 Simplified setting the default.  Move setDebug() to child
 * <p> classes.
 * <p>
 * <p> Revision 1.12  2008/04/08 23:58:03  sueh
 * <p> bug# 1105 Changed the array used in getParsedNumberExpandedArray
 * <p> to a ParsedElementList because it always holds ParsedNumbers.
 * <p>
 * <p> Revision 1.11  2008/04/02 02:17:49  sueh
 * <p> bug# 1097 Matching Matlab's syntax.  This simplifies many of the
 * <p> ParsedElement classes because there where too many special cases.
 * <p> Now it just follows Matlab's syntax instead of trying to immitate exactly
 * <p> how a field happened to be entered by hand in the .prm file.
 * <p>
 * <p> Revision 1.10  2007/11/06 19:48:05  sueh
 * <p> bug# 1047 Made class compatible with ParsedIteratorDescriptor.
 * <p>
 * <p> Revision 1.9  2007/07/31 20:40:10  sueh
 * <p> bug# 1028 added ge(int).
 * <p>
 * <p> Revision 1.8  2007/05/11 16:03:17  sueh
 * <p> bug# 964 Added getArray(List), which adds itself to the list, if it is not
 * <p> empty.
 * <p>
 * <p> Revision 1.7  2007/04/26 02:47:15  sueh
 * <p> bug# 964 Fixed problems with defaultValue.  Added ParsedArray.compact
 * <p> when empty array elements should not be displayed (lstThresholds).
 * <p>
 * <p> Revision 1.6  2007/04/19 21:43:48  sueh
 * <p> bug# 964 Added getRawString(int), moveElement(int, int), setRawString(int,
 * <p> float), setRawString(int, String).
 * <p>
 * <p> Revision 1.5  2007/04/13 21:51:17  sueh
 * <p> bug# 964 Not returning ConstEtomoNumber from ParsedElement, because it
 * <p> must be returned with a getDefaulted... function to be accurate.
 * <p> GetReferenceVolume is returning ParsedElement instead.
 * <p>
 * <p> Revision 1.4  2007/04/13 20:15:56  sueh
 * <p> bug# 964 Added setRawString(String).
 * <p>
 * <p> Revision 1.3  2007/04/09 21:01:33  sueh
 * <p> bug# 964 Placed createTokenizer(String) in the parent class, since it is used
 * <p> everywhere.
 * <p>
 * <p> Revision 1.2  2007/03/31 02:54:34  sueh
 * <p> bug# 964 Added isCollection().
 * <p>
 * <p> Revision 1.1  2007/03/30 23:42:20  sueh
 * <p> bug# 964 Abstract class to act as an interface for elements stored in ParsedElementList.
 * <p> </p>
 */
public abstract class ParsedElement {
  public static final String rcsid = "$Id$";

  private boolean failed = false;
  private String failedMessage = null;
  private boolean missingAttribute = false;

  final String descr;

  public abstract String getRawString();

  public abstract String getRawString(int index);

  public abstract void setDefault(int input);

  public abstract void setDebug(boolean input);

  public abstract boolean equals(int number);

  abstract void setRawString(String number, int lineNum);

  abstract ParsedElement getElement(int index);

  abstract void setRawString(int index, double number);

  abstract void setRawString(int index, String string, int lineNum);

  abstract String validate(int lineNum);

  abstract Token parse(Token token, PrimativeTokenizer tokenizer, int lineNum);

  abstract int size();

  abstract String getParsableString();

  abstract boolean isCollection();

  abstract boolean isDescriptor();

  abstract void setDefault(EtomoNumber input);

  abstract void removeElement(int index);

  abstract boolean ge(int number);

  abstract void clear();

  public final void setRawString(String number) {
    setRawString(number, 0);
  }

  public final void setRawString(int index, String string) {
    setRawString(index, string, 0);
  }

  public final boolean isValid() {
    return validate() == null;
  }

  public final String validate() {
    return validate(0);
  }

  ParsedElement(final String descr) {
    this.descr = descr;
  }

  /**
   * Append non-null ParsedNumbers to parsedNumberExpandedArray.  Create
   * parsedNumberExpandedArray if parsedNumberExpandedArray == null.
   * @param parsedNumberExpandedArray
   * @return parsedNumberExpandedArray
   */
  abstract ParsedElementList getParsedNumberExpandedArray(
      ParsedElementList parsedNumberExpandedArray);

  public boolean isEmpty() {
    if (size() == 0) {
      return true;
    }
    for (int i = 0; i < size(); i++) {
      ParsedElement element = getElement(i);
      if (element != null && !element.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public Number getRawNumber() {
    ParsedElement element = getElement(0);
    if (element != null) {
      return element.getRawNumber();
    }
    return null;
  }

  boolean isDefaultedEmpty() {
    if (size() == 0) {
      return true;
    }
    for (int i = 0; i < size(); i++) {
      ParsedElement element = getElement(i);
      if (element != null && !element.isDefaultedEmpty()) {
        return false;
      }
    }
    return true;
  }

  final PrimativeTokenizer createTokenizer(final String value, final int lineNum) {
    PrimativeTokenizer tokenizer = PrimativeTokenizer.getStringInstance(value, false);
    try {
      tokenizer.initialize();
    }
    catch (IOException e) {
      e.printStackTrace();
      fail(e.getMessage(), lineNum);
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
      fail(e.getMessage(), lineNum);
    }
    return tokenizer;
  }

  final void fail(final String message, final int lineNum) {
    failed = true;
    failedMessage = (descr != null ? descr : "") + ": " + message
        + (lineNum > 0 ? "  Line# " + lineNum : "");
  }

  public final boolean isMissingAttribute() {
    return missingAttribute;
  }

  final void setMissingAttribute() {
    missingAttribute = true;
  }

  final void resetFailed() {
    failed = false;
    failedMessage = null;
  }

  final void setFailed(final boolean failed, final String failedMessage, final int lineNum) {
    this.failed = failed;
    if (failed) {
      this.failedMessage = (descr != null ? descr : "") + ": " + failedMessage
          + (lineNum > 0 ? "  Line# " + lineNum : "");
    }
    else {
      this.failedMessage = null;
    }
  }

  final boolean isFailed() {
    return failed;
  }

  /**
   * Returns null if not failed, otherwise returns a string.
   * @return
   */
  final String getFailedMessage(final int lineNum) {
    if (!failed) {
      return null;
    }
    if (failedMessage == null) {
      return (descr != null ? descr : "") + ": Unable to parse."
          + (lineNum > 0 ? "  Line# " + lineNum : "");
    }
    return failedMessage;
  }
}
