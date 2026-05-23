package etomo.type;

import java.io.IOException;
import java.util.ArrayList;

import etomo.storage.LogFile;
import etomo.storage.autodoc.ReadOnlyAttribute;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.storage.autodoc.ReadOnlySection;
import etomo.ui.swing.Token;
import etomo.util.PrimativeTokenizer;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2005 - 2016 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class EtomoAutodoc {
  public static final String HEADER_SECTION_NAME = "SectionHeader";
  public static final String FIELD_SECTION_NAME = "Field";
  public static final String REQUIRED_ATTRIBUTE_NAME = "required";
  public static final String TYPE_ATTRIBUTE_NAME = "type";
  public static final String SHORT_ATTRIBUTE_NAME = "short";
  public static final String FORMAT_ATTRIBUTE_NAME = "format";
  public static final String USAGE_ATTRIBUTE_NAME = "usage";
  public static final String MANPAGE_ATTRIBUTE_NAME = "manpage";
  public static final String DEFAULT_KEY = "default";
  public static final String BOOLEAN_TYPE = "B";
  public static final String FLOAT_TYPE = "F";
  public static final String INTEGER_TYPE = "I";
  public static final String COMMENT_KEY = "comment";
  public static final int REQUIRED_TRUE_VALUE = 1;
  public static final char VAR_TAG = '%';
  public static final char NEW_LINE_CHAR = '^';
  private static final String TOOLTIP_ATTRIBUTE_NAME = "tooltip";
  public static final String DOUBLE_DASH_ATTRIBUTE_NAME = "DoubleDashOptions";
  private static final char FORMAT_CHAR = '\\';
  private static final String[] FORMAT_STRINGS = new String[] { "fI", "fB", "fR" };

  private static boolean debug = false;

  private EtomoAutodoc() {}

  public static String getTooltip(final String autodocName, final ReadOnlySection section,
    final boolean addSource) {
    if (section == null) {
      if (debug) {
        System.out.println("EtomoAutodoc.getTooltip:section is null");
      }
      return null;
    }
    String text = null;
    ReadOnlyAttribute attribute = section.getAttribute(TOOLTIP_ATTRIBUTE_NAME);
    if (attribute == null || (text = attribute.getMultiLineValue()) == null) {
      attribute = section.getAttribute(USAGE_ATTRIBUTE_NAME);
    }
    if (attribute == null || (text = attribute.getMultiLineValue()) == null) {
      attribute = section.getAttribute(COMMENT_KEY);
    }
    if (attribute == null || (text = attribute.getMultiLineValue()) == null) {
      attribute = section.getAttribute(MANPAGE_ATTRIBUTE_NAME);
      text = attribute.getMultiLineValue();
    }
    if (text != null) {
      text = removeFormatting(text.trim());
      if ((attribute = section.getAttribute(DEFAULT_KEY)) != null) {
        String defaultValue = attribute.getValue();
        if (defaultValue != null) {
          text = SubstitutionString.substitute(text, DEFAULT_KEY, defaultValue);
        }
      }
      if (addSource) {
        String source = "(" + autodocName + ":  " + section.getName() + ")";
        if (text.endsWith(".")) {
          return text.substring(0, text.length() - 1) + " " + source + ".";
        }
        return text + " " + source + ".";
      }
      else {
        return text;
      }
    }
    return null;
  }

  public static String getTooltip(final String autodocName, final ReadOnlySection section,
    final String enumValueName) {
    try {
      String enumTooltip = section.getAttribute("enum").getAttribute(enumValueName)
        .getAttribute(TOOLTIP_ATTRIBUTE_NAME).getMultiLineValue();
      if (enumTooltip != null) {
        enumTooltip = removeFormatting(enumTooltip.trim());
        String source =
          "(" + autodocName + ":  " + section.getName() + " " + enumValueName + ")";
        if (enumTooltip.endsWith(".")) {
          return enumTooltip.substring(0, enumTooltip.length() - 1) + " " + source + ".";
        }
        return enumTooltip + " " + source + ".";
      }
      return getTooltip(autodocName, section, true);
    }
    catch (NullPointerException e) {
      return getTooltip(autodocName, section, true);
    }
  }

  public static String getTooltip(final ReadOnlyAutodoc autodoc, final String fieldName) {
    if (autodoc == null || fieldName == null) {
      return null;
    }
    return getTooltip(autodoc.getAutodocName(),
      autodoc.getSection(FIELD_SECTION_NAME, fieldName), true);
  }

  public static String getTooltip(final ReadOnlyAutodoc autodoc, final String fieldName,
    final boolean addSource) {
    if (autodoc == null || fieldName == null) {
      return null;
    }
    return getTooltip(autodoc.getAutodocName(),
      autodoc.getSection(FIELD_SECTION_NAME, fieldName), addSource);
  }

  /**
   * Removes formatting strings.
   * @param value
   * @return
   */
  public static String removeFormatting(final String value) {
    if (value == null) {
      return null;
    }
    PrimativeTokenizer tokenizer = PrimativeTokenizer.getStringInstance(value, debug);
    StringBuilder tooltip = new StringBuilder();
    Token token = null;
    try {
      tokenizer.initialize();
    }
    catch (IOException e) {
      return value;
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
      return value;
    }
    try {
      // Remove:
      // ^ followed by whitespace at start of line
      // \fB
      // \fI
      // \fR
      // Replace EOL with a space
      boolean eol = true;
      boolean newLine = false;
      boolean format = false;
      while (!(token = tokenizer.next()).is(Token.Type.EOF)) {
        String tokenValue = token.getValue();
        //
        // Process booleans from the previous iteration.
        //
        // Remove indent
        if (newLine) {
          newLine = false;
          // Remove indent.
          if (token.is(Token.Type.WHITESPACE)) {
            tokenValue = "";
          }
        }
        // Remove new line character
        if (eol) {
          eol = false;
          // Remove the formatting character ('^' at the beginning of the line).
          if (token.equals(Token.Type.SYMBOL, NEW_LINE_CHAR)) {
            newLine = true;
            tokenValue = "";
          }
        }
        // Remove specific formatting string
        if (format) {
          format = false;
          boolean found = false;
          // Look for formatting string
          if (token.is(Token.Type.ALPHANUM)) {
            String string = token.getValue();
            if (string != null) {
              for (int i = 0; i < FORMAT_STRINGS.length; i++) {
                // If specific formatting string is found, then omit it.
                if (string.startsWith(FORMAT_STRINGS[i])) {
                  found = true;
                  // Token may contain more then the formatting string.
                  int len = FORMAT_STRINGS[i].length();
                  if (string.length() > len) {
                    tokenValue = string.substring(len);
                  }
                  else {
                    tokenValue = "";
                  }
                }
              }
            }
          }
          if (!found) {
            // Failed to find a specific formatting string - add back the "\" from the
            // previous iteration, which was omitted in case it was part of a formatting
            // string.
            tooltip.append(FORMAT_CHAR);
          }
        }
        //
        // Finished processing booleans from the previous iteration.
        //
        // Convert end of line to a space.
        if (token.is(Token.Type.EOL)) {
          eol = true;
          tokenValue = " ";
        }
        // Remove formatting string
        if (token.equals(Token.Type.SYMBOL, FORMAT_CHAR)) {
          format = true;
          // Omit "\", in case it is part of a specific formatting string.
          tokenValue = "";
        }
        tooltip.append(tokenValue);
      }
    }
    catch (IOException e) {
      return value;
    }
    return tooltip.toString();
  }

  /**
   * Uses the '^' formatting to format the value.
   * @param value
   * @return
   */
  public static String[] format(final String value) {
    if (value == null) {
      return null;
    }
    PrimativeTokenizer tokenizer = PrimativeTokenizer.getStringInstance(value, debug);
    ArrayList list = new ArrayList();
    StringBuffer buffer = new StringBuffer(128);
    boolean startOfLine = true;
    boolean firstToken = true;
    Token token = null;
    try {
      tokenizer.initialize();
      token = tokenizer.next();
    }
    catch (IOException e) {
      return new String[] { value };
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
      return new String[] { value };
    }
    try {
      while (token != null && !token.is(Token.Type.EOF)) {
        if (startOfLine) {
          startOfLine = false;
          // Handle new-line character ('^' at the start of the line).
          if (token.equals(Token.Type.SYMBOL, NEW_LINE_CHAR)) {
            list.add(buffer.toString());
            buffer = new StringBuffer(128);
            startOfLine = false;
            token = tokenizer.next();
            continue;
          }
          else if (!firstToken) {
            // wasn't really the end of the line so convert EOL to a space.
            buffer.append(' ');
          }
          else {
            firstToken = false;
          }
          // still need to process the current token
        }
        if (token.is(Token.Type.EOL)) {
          // Wait to convert end-of-line to a space. If the next token is '^', then this
          // really is the end of a line.
          startOfLine = true;
        }
        else {
          buffer.append(token.getValue());
          startOfLine = false;
        }
        token = tokenizer.next();
      }
    }
    catch (IOException e) {
      return new String[] { value };
    }
    if (buffer.length() > 0) {
      list.add(buffer.toString());
    }
    if (list.size() == 0) {
      return new String[0];
    }
    if (list.size() == 1) {
      return new String[] { (String) list.get(0) };
    }
    return (String[]) list.toArray(new String[list.size()]);
  }

  public static String getTooltip(final String autodocName, final ReadOnlySection section,
    final int enumValueName) {
    return getTooltip(autodocName, section, Integer.toString(enumValueName));
  }

  public static void setDebug(final boolean debug) {
    EtomoAutodoc.debug = debug;
  }
}

/**
 * <p> $Log$
 * <p> Revision 1.19  2009/06/05 02:04:12  sueh
 * <p> bug# 1219 Added the autodocName (the file name minus the extension).
 * <p>
 * <p> Revision 1.18  2009/02/04 23:30:30  sueh
 * <p> bug# 1158 Changed id and exception classes in LogFile.
 * <p>
 * <p> Revision 1.17  2008/10/27 18:38:00  sueh
 * <p> bug# 1141 Added debug only print statements.
 * <p>
 * <p> Revision 1.16  2007/04/13 20:12:56  sueh
 * <p> bug# 964 Added debug member variable.
 * <p>
 * <p> Revision 1.15  2007/03/26 23:35:21  sueh
 * <p> bug# 964 Using getMultiLineValue so formatting can be removed.
 * <p>
 * <p> Revision 1.14  2007/03/23 20:42:00  sueh
 * <p> bug# 964 Added formt(String) and removingFormatting(String).
 * <p> RemoveFormatting is necessary since the BREAK character is no longer being
 * <p> handled by Autodoc and Token.
 * <p>
 * <p> Revision 1.13  2007/03/21 19:43:22  sueh
 * <p> bug# 964 Limiting access to autodoc classes by using ReadOnly interfaces.
 * <p> Added AutodocFactory to create Autodoc instances.
 * <p>
 * <p> Revision 1.12  2007/03/15 21:47:14  sueh
 * <p> bug# 964 Added ReadOnlyAttribute, which is used as an interface for Attribute,
 * <p> unless the Attribute needs to be modified.
 * <p>
 * <p> Revision 1.11  2006/09/13 23:34:51  sueh
 * <p> bug# 921 Preventing null pointer exception in getTooltip.
 * <p>
 * <p> Revision 1.10  2006/04/25 18:57:00  sueh
 * <p> bug# 787 Added VAR_TAG, the variable character for autodoc variables.
 * <p>
 * <p> Revision 1.9  2006/01/12 17:04:10  sueh
 * <p> bug# 798 Moved the autodoc classes to etomo.storage.autodoc.
 * <p>
 * <p> Revision 1.8  2006/01/11 21:48:24  sueh
 * <p> bug# 675 Replaced attribute.getUnformattedValue() with attribute.getValue().
 * <p>
 * <p> Revision 1.7  2005/05/17 19:16:29  sueh
 * <p> bug# 663 Added static values for required attributes.
 * <p>
 * <p> Revision 1.6  2005/05/14 01:00:23  sueh
 * <p> bug# 658 Take out unnecessary trim (and prevent a null point exception).
 * <p>
 * <p> Revision 1.5  2005/05/12 01:29:18  sueh
 * <p> bug# 658 In getTooltip(Autodoc, String fieldName), added the fieldName
 * <p> to the end of tooltip.
 * <p>
 * <p> Revision 1.4  2005/02/21 23:02:55  sueh
 * <p> bug# 600 Return field-level tooltips when enum tooltip is not available.
 * <p>
 * <p> Revision 1.3  2005/02/18 01:28:47  sueh
 * <p> bug# 600 Adding getTooltip(Section, int) to get an enum with the value of
 * <p> the radio button choice.
 * <p>
 * <p> Revision 1.2  2005/02/15 19:29:24  sueh
 * <p> bug# 602 Getting unformatted value from attribute for tooltips.
 * <p>
 * <p> Revision 1.1  2005/02/11 16:44:17  sueh
 * <p> bug# 600 Adding class which knows about key words used in the autodocs
 * <p> and the rules for getting a tooltip.
 * <p> </p>
 */
