package etomo.storage;

import etomo.storage.autodoc.AutodocTokenizer;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2013</p>
*
* <p>Organization:
* Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
* University of Colorado</p>
* 
* @author $Author$
* 
* @version $Revision$
* 
* <p> $Log$ </p>
*/
public final class DirectiveType {
  public static final String rcsid = "$Id:$";

  public static final DirectiveType SETUP_SET = new DirectiveType("setupset");
  public static final DirectiveType RUN_TIME = new DirectiveType("runtime");
  public static final DirectiveType COM_PARAM = new DirectiveType("comparam");
  public static final DirectiveType COPY_ARG = new DirectiveType("copyarg");

  private final String tag;

  private DirectiveType(final String tag) {
    this.tag = tag;
  }

  static DirectiveType getInstance(final String directive) {
    return getInstance(directive.split("\\" + AutodocTokenizer.SEPARATOR_CHAR));
  }

  static DirectiveType getInstance(final String[] directiveArray) {
    if (directiveArray == null) {
      return null;
    }
    int index = 0;
    if (directiveArray.length > index) {
      if (SETUP_SET.tag.equals(directiveArray[index])) {
        index++;
        if (directiveArray.length > index) {
          if (COPY_ARG.tag.equals(directiveArray[index])) {
            return COPY_ARG;
          }
        }
        return SETUP_SET;
      }
      if (RUN_TIME.tag.equals(directiveArray[index])) {
        return RUN_TIME;
      }
      if (COM_PARAM.tag.equals(directiveArray[index])) {
        return COM_PARAM;
      }
    }
    return null;
  }

  /**
   * @param input - directive name or first part of the name
   * @return the instance matching the first section of input
   */
  static DirectiveType getFirstSectionInstance(String input) {
    if (input == null) {
      return null;
    }
    if (input.indexOf('.') != -1) {
      String[] array = input.split("\\" + AutodocTokenizer.SEPARATOR_CHAR);
      if (array == null || array.length < 1 || array[0] == null) {
        return null;
      }
      input = array[0];
    }
    input = input.trim();
    if (SETUP_SET.equals(input)) {
      return SETUP_SET;
    }
    if (RUN_TIME.equals(input)) {
      return RUN_TIME;
    }
    if (COM_PARAM.equals(input)) {
      return COM_PARAM;
    }
    return null;
  }

  /**
   * @param input - directive name or first part of the name
   * @return true if this instance matches the first section of input
   */
  public boolean equals(String input) {
    if (input == null) {
      return false;
    }
    if (input.indexOf('.') != -1) {
      String[] array = input.split("\\" + AutodocTokenizer.SEPARATOR_CHAR);
      if (array == null || array.length < 1 || array[0] == null) {
        return false;
      }
      input = array[0];
    }
    input = input.trim();
    return input.equals(this.tag);
  }

  public String getKey() {
    if (this == COPY_ARG) {
      return SETUP_SET.tag + AutodocTokenizer.SEPARATOR_CHAR + tag;
    }
    return tag;
  }

  public String toString() {
    return tag;
  }
}