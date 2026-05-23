package etomo.storage;

import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.AxisID;

/**
* <p>Description: Handles the left side of a directive set.
* Directive set:
* - 1 directive with no axisID information or
* - 3 directives: A, B, and both axes.
* This class can return a key, which is identical to the directive name for both axes.  It
* can also return a directive name for a specific axisID.</p>
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
public final class DirectiveName {
  public static final String rcsid = "$Id:$";

  private static final int TYPE_INDEX = 0;
  private static final int COM_FILE_NAME_INDEX = 1;
  private static final int PROGRAM_INDEX = 2;
  private static final int PARAMETER_NAME_INDEX = 3;
  private static final int RUNTIME_AXIS_INDEX = 2;

  private String[] key = null;
  private DirectiveType type = null;

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("[key:");
    if (key != null) {
      for (int i = 0; i < key.length; i++) {
        buffer.append(key[i] + " ");
      }
    }
    return buffer + ",type:" + type + "]";
  }

  public DirectiveName() {
  }

  public static boolean equals(final String key, final DirectiveType input) {
    if (key == null || input == null) {
      return false;
    }
    return key.startsWith(input.toString() + AutodocTokenizer.SEPARATOR_CHAR);
  }

  public boolean equals(final DirectiveType input) {
    if (isNull()) {
      return false;
    }
    return type == input;
  }

  /**
   * @return the name with no axis ID
   */
  public String getKey() {
    return convertKeyToString(key);
  }

  public String getKeyDescription() {
    String key = getKey();
    if (type == DirectiveType.RUN_TIME) {
      return key.replace(AutodocTokenizer.SEPARATOR_CHAR
          + DirectiveDef.RUN_TIME_ANY_AXIS_TAG, "");
    }
    return key;
  }

  public String getComFileName() {
    if (isNull()) {
      return null;
    }
    // Only comparam directives have a comfile name. Missing comfile name.
    if (type != DirectiveType.COM_PARAM || key.length <= COM_FILE_NAME_INDEX) {
      return null;
    }
    return key[COM_FILE_NAME_INDEX];
  }

  public String getParameterName() {
    if (isNull()) {
      return null;
    }
    if (type == DirectiveType.SETUP_SET) {
      if (key.length < 2) {
        return null;
      }
      boolean copyarg = DirectiveType.COPY_ARG.equals(key[1]);
      if (copyarg) {
        if (key.length > 2) {
          return key[2];
        }
      }
      else {
        return key[1];
      }
    }
    else if ((type == DirectiveType.COM_PARAM || type == DirectiveType.RUN_TIME)
        && key.length > PARAMETER_NAME_INDEX) {
      return key[PARAMETER_NAME_INDEX];
    }
    return null;
  }

  public String getProgramName() {
    if (isNull()) {
      return null;
    }
    // Only comparam directives have a program name. Missing program name.
    if (type != DirectiveType.COM_PARAM || key.length <= PROGRAM_INDEX) {
      return null;
    }
    return key[PROGRAM_INDEX];
  }

  public String getTitle() {
    if (isNull()) {
      return null;
    }
    if (type == DirectiveType.COM_PARAM) {
      return getComFileName() + "." + getParameterName();
    }
    else {
      return getParameterName();
    }
  }

  public DirectiveType getType() {
    return type;
  }

  boolean isCopyArg() {
    return type == DirectiveType.SETUP_SET && key.length > 1 && key[1].equals("copyarg");
  }

  public boolean isValid() {
    if (isNull()) {
      return false;
    }
    return type != null && key.length > 1;
  }

  /**
   * Copies the data rather then pointers to mutable objects in the parameter.
   * @param directiveName
   */
  void deepCopy(final DirectiveName directiveName) {
    if (directiveName.key == null) {
      key = null;
    }
    else {
      key = new String[directiveName.key.length];
      for (int i = 0; i < key.length; i++) {
        key[i] = directiveName.key[i];
      }
    }
    type = directiveName.type;
  }

  void setKey(DirectiveDescr descr) {
    // The a and b axes are not included for comparam and runtime directives in the
    // directive.csv file, so no need to remove them.
    key = splitKey(descr.getName());
    type = getType(key);
  }

  static String makeKey(final String input) {
    String[] staticKey = splitKey(input);
    DirectiveType staticType = getType(staticKey);
    stripAxis(staticKey, staticType);
    return convertKeyToString(staticKey);
  }

  private static String[] splitKey(final String input) {
    if (input != null && !input.matches("\\s*")) {
      return input.split("\\" + AutodocTokenizer.SEPARATOR_CHAR);
    }
    return null;
  }

  private static DirectiveType getType(String[] key) {
    if (key != null && key.length > TYPE_INDEX) {
      return DirectiveType.getFirstSectionInstance(key[TYPE_INDEX]);
    }
    return null;
  }

  private static AxisID stripAxis(final String[] key, final DirectiveType type) {
    if (type != DirectiveType.COM_PARAM && type != DirectiveType.RUN_TIME) {
      return null;
    }
    // Remove axisID from the name to create a key. Standardize the key to the Any form of
    // the directive name, and return the axisID that was found.
    AxisID axisID = null;
    for (int i = 0; i < key.length; i++) {
      if (type == DirectiveType.COM_PARAM
          && i == COM_FILE_NAME_INDEX
          && key[i] != null
          && (key[i].endsWith(AxisID.FIRST.getExtension()) || key[i]
              .endsWith(AxisID.SECOND.getExtension()))) {
        if (key[i].endsWith(AxisID.FIRST.getExtension())) {
          axisID = AxisID.FIRST;
        }
        else {
          axisID = AxisID.SECOND;
        }
        // Strip off the a or b
        key[i] = key[i].substring(0, key[i].length() - 1);
      }
      else if (type == DirectiveType.RUN_TIME
          && i == RUNTIME_AXIS_INDEX
          && key[i] != null
          && (key[i].equals(AxisID.FIRST.getExtension()) || key[i].equals(AxisID.SECOND
              .getExtension()))) {
        if (key[i].equals(AxisID.FIRST.getExtension())) {
          axisID = AxisID.FIRST;
        }
        else {
          axisID = AxisID.SECOND;
        }
        // Replace with "any".
        key[i] = DirectiveDef.RUN_TIME_ANY_AXIS_TAG;
      }
    }
    return axisID;
  }

  private static String convertKeyToString(final String[] key) {
    if (key == null || key.length == 0) {
      return null;
    }
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < key.length; i++) {
      buffer.append((i > 0 ? "." : "") + key[i]);
    }
    return buffer.toString();
  }

  /**
   * Strips axis information and saves a key containing the "any" form of the directive
   * name.  For a directive with no axis information or an "any" directive name, the key
   * is the same as the input string, and null is returned.
   * @param input
   * @return the axisID that was removed from the name (or null for "any").
   */
  public AxisID setKey(String input) {
    key = splitKey(input);
    type = getType(key);
    return stripAxis(key, type);
  }

  private boolean isNull() {
    return key == null || key.length == 0;
  }

  private static boolean mayContainAxisID(final String name) {
    return name != null
        && (name.startsWith(DirectiveType.RUN_TIME.toString()
            + AutodocTokenizer.SEPARATOR_CHAR) || name.startsWith(DirectiveType.COM_PARAM
            .toString() + AutodocTokenizer.SEPARATOR_CHAR))
        && (name.indexOf(AxisID.FIRST.getExtension() + AutodocTokenizer.SEPARATOR_CHAR) != -1 || name
            .indexOf(AxisID.SECOND.getExtension() + AutodocTokenizer.SEPARATOR_CHAR) != -1);
  }

  /**
   * Returns the directive name for the axisID specified.
   * @param axisID
   * @return
   */
  public String getName() {
    if (key == null) {
      return null;
    }
    // Set ext to the correct form
    String ext = "";
    if (type == DirectiveType.RUN_TIME) {
      ext = DirectiveDef.RUN_TIME_ANY_AXIS_TAG;
    }
    // Create a string version of the directive name with the correct axisID string
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < key.length; i++) {
      if (type == DirectiveType.COM_PARAM && i == COM_FILE_NAME_INDEX) {
        buffer.append((i > 0 ? AutodocTokenizer.SEPARATOR_CHAR : "") + key[i] + ext);
      }
      else if (type == DirectiveType.RUN_TIME && i == RUNTIME_AXIS_INDEX) {
        buffer.append((i > 0 ? AutodocTokenizer.SEPARATOR_CHAR : "") + ext);
      }
      else {
        buffer.append((i > 0 ? AutodocTokenizer.SEPARATOR_CHAR : "") + key[i]);
      }
    }
    return buffer.toString();
  }
}
