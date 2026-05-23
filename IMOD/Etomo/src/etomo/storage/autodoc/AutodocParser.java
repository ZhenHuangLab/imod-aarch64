package etomo.storage.autodoc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.storage.LogFile;
import etomo.storage.autodoc.AutodocTokenizer.Location;
import etomo.type.AxisID;
import etomo.ui.swing.Token;

/**
 * <p>Description:
 * Parses an autodoc file.  Finds and saves autodoc elements in an Autodoc
 * object.
 * </p><p>
 * AutodocParser is not case sensitive.  It stores all text in the original case.
 * It retains the original whitespace, except for end of line.  It substitute one
 * space for each end of line character in a multi-line value.  Comments and
 * empty lines are stored.  Messages about syntax errors are sent to System.err.
 * It stores attributes in a tree structure and as individual ordered name/value
 * pairs.  When there are duplicate attributes in a section, retrieving the
 * value from the tree structure retrieves the last value.  It is extremely
 * important to keep the language definition up to date.
 * </p>
 * <pre>
 * To Use:
 * Construct the class with an Autodoc.
 * Run initialize().
 * Run parse().
 *
 * Testing:
 * Do not call initialize() when testing.
 * Call test() to test this class.
 * Call testPreprocessor() to test only the preprocessor in this class.
 * Call testAutodocTokenizer() to test the AutodocTokenizer.
 * Call testPrimativeTokenizer() to test the PrimativeTokenizer.
 * Call testStreamTokenizer() to test the StreamTokenizer.
 *
 *
 * Language Definition:
 *
 * Valid Autodoc Tokens:
 *
 * Special Character Tokens:
 * OPEN => [
 * CLOSE => ]
 * SUBOPEN => [[
 * SUBCLOSE => ]]
 * SEPARATOR => .
 * BREAK => ^ (formatting character - ignored except in valueline at startline)
 * COMMENT => # (and % when defined)
 * QUOTE " | ' | `
 *
 *
 * Other Tokens:
 * EOL => end of line
 * EOF => end of file
 * DELIMETER => A definable string of non-alphnumeric characters.  May not
 *              include characters used by Special Character Tokens.  The default
 *              is "=".
 * WHITESPACE => The largest possible string of whitespace.
 * WORD => The largest possible string of everything not used by other Autodoc
 *         tokens.
 * KEYWORD => A WORD that matches an autodoc keyword:  Version, Pip,
 *            KeyValueDelimiter
 *
 *
 * Language definition for the parser:
 * Syntax of language definition:
 * => equals
 * TOKEN
 * {  0 or more  }
 * [  1 or more  ]
 * {n  0 up to n  n}
 * (  group together  )
 * \any token except these\
 * -optional-
 * matches# #matches
 * | => or
 * & => and
 * ^ => beginning of the line
 * ! => not
 *
 * Preprocessor flags:
 * DelimiterInLine
 *
 * Definition:
 *
 * Autodoc => { emptyLine | comment | pair | section } EOF
 *
 * section => OPEN sectionHeader CLOSE -WHITESPACE- ( EOL | EOF )
 *            { emptyLine | comment | pair | subsection }
 *
 *
 * sectionHeader => sectionType -WHITESPACE- DELIMITER -WHITESPACE- sectionName -WHITESPACE-
 *
 * sectionType => ( WORD | KEYWORD )
 *
 * sectionName => [ \CLOSE & SUBCLOSE & WHITESPACE & EOL & EOF\ ]
 *
 * subsection => SUBOPEN sectionHeader SUBCLOSE -WHITESPACE- ( EOL | EOF )
 *               { emptyLine | comment | pair }
 *               subsectionClose
 *
 * subsectionClose => SUBOPEN -WHITESPACE- SUBCLOSE (EOL | EOF )
 *
 * pair => name -WHITESPACE- DELIMITER -WHITESPACE- -QUOTE#- value
 *
 * name => base-attribute { SEPARATOR attribute }
 *
 * base-attribute => ( WORD | KEYWORD | QUOTE ) -attribute-
 *
 * attribute => [ WORD | KEYWORD | COMMENT | QUOTE ]
 *
 * value => { \EOL & EOF\ } ( EOL | EOF ) { (!quoted & valueline ) | ( quoted & ( quotedValueLine | #QUOTE ) ) }
 *
 * valueLine => !emptyLine !DelimiterInLine !comment { \DELIMITER & SUBOPEN & EOL & EOF\ } ( EOL | EOF )
 *
 * quotedValueLine => !emptyLine { \EOL & EOF\ } #QUOTE -WHITESPACE- ( EOL | EOF )
 *
 * comment => COMMENT { \EOL\ } ( EOL | EOF )
 *
 * emptyLine => ^ -WHITESPACE- ( EOL | EOF )
 *
 * !emptyLine => ^ -WHITESPACE- ( \EOL & EOF\ )
 *
 *
 * PEET variant:
 * No quoted strings in the PEET variant.
 *
 * pair => name -WHITESPACE- DELIMITER -WHITESPACE- value
 *
 *
 * Non-embedded EOL and WHITESPACE are stripped from values.
 * Optional WHITESPACE is ignored in other elements.
 *
 * Required pairs:
 * Top level attributes (meta data):  Version and Pip.
 *
 * Flags:
 * The KeyValueDelimiter attribute changes the delimiter string for subsequent
 * lines.  It can be used as often as needed and placed anywhere in the autodoc
 * file.  The value of a KeyValueDelimiter must be one line
 *
 * Preprocessor:
 * Sets flags decribing the current line: delimiterInLine and line number.
 *
 * Postprocessor:
 * Checks for required elements.
 * </pre>
 *
 * <p>Copyright: Copyright 2002 - 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * @author $Author$
 * @version $Revision$
 *          <p/>
 *          <p> $$Log$
 *          <p> $Revision 1.23  2010/11/13 16:05:36  sueh
 *          <p> $bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 *          <p> $
 *          <p> $Revision 1.22  2009/03/09 17:30:25  sueh
 *          <p> $bug# 1199 Updated comments to show that the last value of duplicate
 *          <p> $attributes is retrieved as the default.  Also got rid of the keyword
 *          <p> $"CommandLanguage", which was never used and is unnecessary since
 *          <p> $autodocs are always stored in both a tree structure and sequentially.
 *          <p> $
 *          <p> $Revision 1.21  2009/02/04 23:30:00  sueh
 *          <p> $bug# 1158 Changed id and exceptions classes in LogFile.
 *          <p> $
 *          <p> $Revision 1.20  2009/01/20 19:34:21  sueh
 *          <p> $bug# 1102 Fixed bug - not allowing whitespace after subsection header close.
 *          <p> $
 *          <p> $Revision 1.19  2008/05/30 21:24:27  sueh
 *          <p> $bug# 1102 In autodoc() setting commandLanguage boolean.
 *          <p> $
 *          <p> $Revision 1.18  2007/08/01 22:44:20  sueh
 *          <p> $bug# 985 Moved look-ahead to tokenizer for subsection recognition.  Fixed bugs
 *          <p> $in subsection recognition.
 *          <p> $
 *          <p> $Revision 1.17  2007/06/07 21:32:20  sueh
 *          <p> $bug# Passing debug in constructor all the time.
 *          <p> $
 *          <p> $Revision 1.16  2007/04/13 18:44:17  sueh
 *          <p> $bug# 964 Fixed a recent bug in delimiter change:  AutodocTokenizer wasn't
 *          <p> $being told about the change.
 *          <p> $
 *          <p> $Revision 1.15  2007/04/11 22:00:38  sueh
 *          <p> $bug# 964 Fixed a bug in emptyLine() where the EOF was being treated as an
 *          <p> $empty line.
 *          <p> $
 *          <p> $Revision 1.14  2007/04/09 20:31:48  sueh
 *          <p> $bug# 964 Moved the value to the associated name/value pair.  Changed
 *          <p> $the Vector member variable from values to nameValuePairList.  Associated the
 *          <p> $last attribute in each name/value pair with the name value pair.  This is the
 *          <p> $attribute which used to contain the value.  The name/value pair also contained
 *          <p> $the value; so it was duplicated.  This made it difficult to add a value to an
 *          <p> $existing attribute.  GetValue() gets the value from the associated name/value
 *          <p> $pair.  Also removed the old nameValuePairList member variable, because it
 *          <p> $wasn't being used for anything.
 *          <p> $
 *          <p> $Revision 1.13  2007/03/26 18:36:50  sueh
 *          <p> $bug# 964 Made Version optional so that it is not necessary in matlab param files.
 *          <p> $
 *          <p> $Revision 1.12  2007/03/23 20:32:58  sueh
 *          <p> $bug# 964 Adding an entry to NameValuePairList which represents the change in
 *          <p> $delimiter.
 *          <p> $
 *          <p> $Revision 1.11  2007/03/21 18:15:41  sueh
 *          <p> $bug# 964 Removed mutable boolean.  Access-level will be controlled by the
 *          <p> $interfaces.
 *          <p> $
 *          <p> $Revision 1.10  2007/03/15 21:45:55  sueh
 *          <p> $bug# 964 Clarifying the code to show that the same value instance is saved to
 *          <p> $both attribute and name/value pair.
 *          <p> $
 *          <p> $Revision 1.9  2007/03/08 21:55:06  sueh
 *          <p> $bug# 964 Save name/value pairs in the parser instead of saving them from the
 *          <p> $Attribute.  This is necessary because the name/value pair must be placed in the
 *          <p> $autodoc or section as soon as they are found to preserve the original order of the
 *          <p> $autodoc file.  Also save the comment as early as possible, though this isn't such
 *          <p> $as big deal because comments are one line long.
 *          <p> $
 *          <p> $Revision 1.8  2007/03/07 21:06:23  sueh
 *          <p> $bug# 964 Fixed printing.  Fixed attributes:  the base attribute cannot contain a
 *          <p> $comment character, but the other attributes can.  This is necessary for the
 *          <p> $uitest autodoc flavor.
 *          <p> $
 *          <p> $Revision 1.7  2007/03/01 01:18:39  sueh
 *          <p> $bug# 964 Saving comments and empty lines in autodoc data structure.  Replaced
 *          <p> $startLine(), startLineDelimiter(), and startLineWithOutDelimiter() with emptyLine()
 *          <p> $and isBeginningOfLine().
 *          <p> $
 *          <p> $Revision 1.6  2006/06/22 22:08:09  sueh
 *          <p> $bug# 852 Added subsection(), subsectionClose(), lookAhead(), and
 *          <p> $lookAheadAhead().
 *          <p> $
 *          <p> $Revision 1.5  2006/06/14 21:22:06  sueh
 *          <p> $bug# 852 Moving the call the processMetaData() to value().  Passing the first
 *          <p> $line of the value to processMetaData to handle DelimiterKeyValue.
 *          <p> $
 *          <p> $Revision 1.4  2006/06/14 00:22:19  sueh
 *          <p> $bug# 852 Rewrote the autodoc language definitions to make them simpler and
 *          <p> $easier to understand.  Fixed an incorrect definition in Attribute.  Added a
 *          <p> $definition for sub-sections.  Rewrote the parser to conform to the new definitions.
 *          <p> $Added LinkList inner class to handle elements made of multiple Tokens.
 *          <p> $Removes all the preprocessor flags except for delimiterInLine.  Handling
 *          <p> $BREAK in the parser instead of the preprocessor.  Fixed the internal test
 *          <p> $print statements so that they are clearer.
 *          <p> $
 *          <p> $Revision 1.3  2006/06/05 18:05:47  sueh
 *          <p> $bug# 766 Removed commented out print statements.
 *          <p> $
 *          <p> $Revision 1.2  2006/05/01 21:16:57  sueh
 *          <p> $bug# 854
 *          <p> $
 *          <p> $Revision 1.1  2006/01/12 17:02:37  sueh
 *          <p> $bug# 798 Moved the autodoc classes to etomo.storage.autodoc.
 *          <p> $
 *          <p> $Revision 1.10  2006/01/11 21:57:36  sueh
 *          <p> $bug# 675 Replaced AttributeCollection with WriteOnlyAttributeMap.
 *          <p> $
 *          <p> $Revision 1.9  2005/11/10 18:15:22  sueh
 *          <p> $bug# 733 Changed the missing meta data warning to a single line warning.
 *          <p> $
 *          <p> $Revision 1.8  2005/09/21 16:36:58  sueh
 *          <p> $bug# 532 Changed Autodoc.getFile() to getAutodocFile().
 *          <p> $
 *          <p> $Revision 1.7  2005/09/01 17:59:47  sueh
 *          <p> $bug# 532 Allow a comment inside a section.  Changed the language
 *          <p> $definition and section().
 *          <p> $
 *          <p> $Revision 1.6  2005/02/15 19:51:54  sueh
 *          <p> $bug# 602 Preprocessor:  Converting BREAK token to WORD when it is
 *          <p> $not at the beginning of a line.  Converting the spaces following a BREAK
 *          <p> $to an INDENT.
 *          <p> $Parcer:  Saving BREAKS and INDENTS.  In value(), ignoring
 *          <p> $WHITESPACE that preceeds a BREAK.
 *          <p> $
 *          <p> $Revision 1.5  2004/01/01 00:45:17  sueh
 *          <p> $bug# 372 correcting interface name
 *          <p> $
 *          <p> $Revision 1.4  2003/12/31 17:47:41  sueh
 *          <p> $bug# 372 add doc, get file from Autodoc
 *          <p> $
 *          <p> $Revision 1.3  2003/12/31 01:27:04  sueh
 *          <p> $bug# 372 save recognized data, testing, delimiter change on
 *          <p> $the fly, checking for meta data
 *          <p> $
 *          <p> $Revision 1.2  2003/12/23 21:32:25  sueh
 *          <p> $bug# 372 Reformating.  Creating parser.  Test function for
 *          <p> $preprocessor.  Fixing preprocessor.
 *          <p> $
 *          <p> $Revision 1.1  2003/12/22 23:48:50  sueh
 *          <p> $bug# 372 parser and preprocessor for autodoc files.  Stores
 *          <p> $autodoc data.
 *          <p> $$ </p>
 */

final class AutodocParser {
  private static final boolean DEBUG = EtomoDirector.INSTANCE.getArguments().isDebug();

  private final Vector line = new Vector();

  private final boolean peetVariant;

  private AutodocTokenizer tokenizer;
  private final LogFile logFile;
  private final StringBuilder errMsg;

  private int tokenIndex = 0;
  private WriteOnlyStatementList autodoc = null;
  private Token token = null;
  private Token prevToken = null;
  private Token prevPrevToken = null;
  private boolean parsed = false;

  // error flags
  private boolean errorsFound = false;
  private boolean error = false;

  // testing flags
  private boolean test = false;
  private boolean detailedTest = false;
  private boolean testWithTokens = false;
  private Token.Type lastTokenType = Token.Type.NULL;
  private int testIndent = -1;

  // Preprocessor flags
  private boolean delimiterInLine = false;
  private int lineNum = 0;
  private int lastLinePrinted = 0;

  // Postprocessor flags
  private boolean versionFound = false;
  private boolean pipFound = false;
  private boolean versionRequired = true;

  private boolean debug = false;
  private boolean debugLocal = false;

  private AutodocParser(final Autodoc autodoc, final boolean allowAltComment,
    final boolean versionRequired, final boolean peetVariant, final Location location,
    final String envVar, final String subdirName, final String name,
    final File autodocFile, final BaseManager manager, final AxisID axisID,
    final String notFoundMessage, final boolean debug, final boolean writable,
    final StringBuilder errMsg) {
    this.debug = debug;
    this.peetVariant = peetVariant;
    this.errMsg = errMsg;
    if (autodoc == null) {
      throw new IllegalArgumentException("autodoc is null.");
    }
    this.autodoc = autodoc;
    tokenizer = new AutodocTokenizer(allowAltComment, location, envVar, subdirName, name,
      autodocFile, manager, axisID, notFoundMessage, debug, writable);
    logFile = tokenizer.getLogFile();
    this.versionRequired = versionRequired;
  }

  static AutodocParser getAutodocInstance(final Autodoc autodoc,
    final boolean allowAltComment, final boolean versionRequired,
    final boolean peetVariant, final String name, final BaseManager manager,
    final AxisID axisID, final String notFoundMessage, final boolean debug) {
    return new AutodocParser(autodoc, allowAltComment, versionRequired, peetVariant,
      Location.AUTODOC, null, null, name, null, manager, axisID, notFoundMessage, debug,
      false, null);
  }

  static AutodocParser getUnmanagedAutodocInstance(final Autodoc autodoc,
    final boolean allowAltComment, final boolean versionRequired,
    final boolean peetVariant, final String name, final File autodocFile,
    final BaseManager manager, final AxisID axisID, final String notFoundMessage) {
    return new AutodocParser(autodoc, allowAltComment, versionRequired, peetVariant,
      Location.AUTODOC, null, null, name, autodocFile, manager, axisID, notFoundMessage,
      false, false, null);
  }

  static AutodocParser getGenericInstance(final Autodoc autodoc,
    final boolean allowAltComment, final boolean versionRequired,
    final boolean peetVariant, final String envVar, final String subdirName,
    final String name, final BaseManager manager, final AxisID axisID,
    final String notFoundMessage, final boolean debug, final boolean writable) {
    return new AutodocParser(autodoc, allowAltComment, versionRequired, peetVariant, null,
      envVar, subdirName, name, null, manager, axisID, notFoundMessage, debug, writable,
      null);
  }

  static AutodocParser getGenericInstance(final Autodoc autodoc,
    final boolean allowAltComment, final boolean versionRequired,
    final boolean peetVariant, final File autodocFile, final BaseManager manager,
    final AxisID axisID, final String notFoundMessage, final boolean debug,
    final boolean writable, final StringBuilder errMsg) {
    return new AutodocParser(autodoc, allowAltComment, versionRequired, peetVariant, null,
      null, null, null, autodocFile, manager, axisID, notFoundMessage, debug, writable,
      errMsg);
  }

  void initialize() throws FileNotFoundException, IOException, LogFile.LockException {
    tokenizer.initialize();
  }

  boolean isError() {
    return error;
  }

  String getFileName() {
    if (logFile == null) {
      return "";
    }
    return logFile.getName();
  }

  boolean exists() {
    if (logFile == null) {
      return false;
    }
    return logFile.exists();
  }

  String getAbsolutePath() {
    if (logFile == null) {
      return "";
    }
    return logFile.getAbsolutePath();
  }

  LogFile getLogFile() {
    return logFile;
  }

  /**
   * Parses an autodoc file.
   * This function can be run only once per instance of the object.
   *
   * @throws IOException
   */
  void parse() throws IOException {
    autodoc();
    tokenizer = null;
  }

  /**
   * Autodoc => { emptyLine | comment | pair | section } EOF
   * <p/>
   * Parses an autodoc file.
   * This function can be run only once per instance of the object.
   *
   * @throws IOException
   */
  private void autodoc() throws IOException {
    if (parsed) {
      return;
    }
    parsed = true;
    nextToken();
    boolean globalSection = true;
    while (!token.is(Token.Type.EOF)) {
      /*  if (!emptyLine(autodoc) && !comment(autodoc) && !section()
        && !pair(autodoc, true)) {
        reportError("Unknown statement.");
      }*/
      if (!emptyLine(autodoc) && !comment(autodoc)) {
        if (globalSection) {
          if (section(false)) {
            globalSection = false;
          }
          else {
            pair(autodoc, true);
          }
        }
        else {
          // Sections can contain empty lines. They only end when another section starts.
          // Once the sections start there are no more autodoc-level pairs.
          section(true);
        }
      }
    }
  }

  /**
   * emptyLine => ^ -WHITESPACE- ( EOL | EOF )
   * <p/>
   * !emptyLine => ^ -WHITESPACE-
   * <p/>
   * Eats up an empty line, starting from the beginning of the line.  Writes the
   * empty line to the autodoc, section, or subsection is in.
   *
   * @return true if line is empty, false if line is not empty
   * @throws IOException
   */
  private boolean emptyLine(WriteOnlyStatementList list) throws IOException {
    if (!isBeginningOfLine()) {
      // empty lines must start at the beginning of the line
      return false;
    }
    // Eat up the whitespace. This helps identify the empty line. But if its not
    // an empty line, it gets rid of the white space at the beginning of the line
    while (matchToken(Token.Type.WHITESPACE) != null) {}
    if (token.is(Token.Type.EOL)) {
      testStartFunction("emptyline");
      // found empty line
      list.addEmptyLine(lineNum);
      nextToken();
      testEndFunction("emptyline", true);
      return true;
    }
    return false;
  }

  /**
   * comment => COMMENT { \EOL\ } ( EOL | EOF )
   * <p/>
   * Parses a comment.
   *
   * @return true if comment found
   * @throws IOException
   */
  private boolean comment(WriteOnlyStatementList list) throws IOException {
    if (matchToken(Token.Type.COMMENT) == null) {
      // not a comment
      return false;
    }
    testStartFunction("comment");
    // comments can be made of multiple tokens, so use a link list. A comment entry
    // should not contain the EOL or EOF
    LinkList commentLinkList = null;
    if (!token.is(Token.Type.EOL) && !token.is(Token.Type.EOF)) {
      // Non-empty comment
      commentLinkList = new LinkList(token);
    }
    // Add comment
    if (commentLinkList != null) {
      // Add non-empty comment
      list.addComment(commentLinkList.getHead(), lineNum);
    }
    else {
      // Add empty comment
      list.addComment(null, lineNum);
    }
    // add tokens to the non-empty comment
    while (!token.is(Token.Type.EOL) && !token.is(Token.Type.EOF)) {
      commentLinkList.append(token);
      nextToken();
    }
    // eat up the EOL
    matchToken(Token.Type.EOL);
    testEndFunction("comment", true);
    return true;
  }

  /**
   * section => OPEN sectionHeader CLOSE -WHITESPACE- ( EOL | EOF )
   * { emptyLine | comment | pair | subsection }
   * <p/>
   * Parses a section.
   *
   * @return true if section found
   * @throws IOException
   */
  private boolean section(final boolean required) throws IOException {
    if (matchToken(Token.Type.OPEN) == null) {
      // not a section
      if (required) {
        reportError("Unknown statement.  Expecting a section (missing '"
          + Token.Type.OPEN.getDescr() + "').");
      }
      return false;
    }
    testStartFunction("section");
    // save the new section in the autodoc
    Section section = null;
    // sectionHeader saves the section
    section = sectionHeader(autodoc);
    if (section == null) {
      // bad section
      testEndFunction("section", false);
      return false;
    }
    if (matchToken(Token.Type.CLOSE) == null) {
      // bad section
      reportError(
        "A section header must end with '" + Token.Type.CLOSE.getDescr() + "'.");
      testEndFunction("section", false);
      return false;
    }
    matchToken(Token.Type.WHITESPACE);
    if (matchToken(Token.Type.EOL) == null && matchToken(Token.Type.EOF) == null) {
      // bad section
      reportError(
        "A section header must end with '" + Token.Type.CLOSE.getDescr() + "'.");
      testEndFunction("section", false);
      return false;
    }
    while (!token.is(Token.Type.EOF)) {
      // look for elements in the section
      if (!emptyLine(section) && !comment(section) && !pair(section, false)
        && !subsection(section)) {
        // end of section
        testEndFunction("section", true);
        return true;
      }
    }
    // empty section
    testEndFunction("section", true);
    return true;
  }

  /**
   * subsection => SUBOPEN sectionHeader SUBCLOSE ( EOL | EOF )
   * { emptyLine | comment | pair }
   * subsectionClose
   * Parses a subsection
   *
   * @return
   * @throws IOException
   */
  private boolean subsection(Section section) throws IOException {
    // use look ahead because this could be a section
    if (!token.is(Token.Type.SUBOPEN)) {
      // not a subsection
      return false;
    }
    testStartFunction("subsection");
    // its a subsection so eat up SUBOPEN
    nextToken();
    // save the new subsection in the section
    WriteOnlyStatementList subsection = null;
    subsection = sectionHeader(section);
    if (subsection == null) {
      // bad subsection
      testEndFunction("subsection", false);
      return false;
    }
    if (matchToken(Token.Type.SUBCLOSE) == null) {
      // bad subsection
      reportError(
        "A subsection header must end with '" + Token.Type.SUBCLOSE.getDescr() + "'.");
      testEndFunction("subsection", false);
      return false;
    }
    matchToken(Token.Type.WHITESPACE);
    if (matchToken(Token.Type.EOL) == null && matchToken(Token.Type.EOF) == null) {
      // bad section
      reportError(
        "A section header must end with '" + Token.Type.SUBCLOSE.getDescr() + "'.");
      testEndFunction("section", false);
      return false;
    }
    boolean done = false;
    boolean closed = false;
    while (!done) {
      // look for elements in the subsection
      if (!emptyLine(subsection) && !comment(subsection) && !pair(subsection, false)) {
        closed = subsectionClose(subsection);
        done = true;
      }
    }
    // end subsection
    testEndFunction("subsection", closed);
    return closed;
  }

  /**
   * * subsectionClose -> SUBOPEN -WHITESPACE- SUBCLOSE (EOL | EOF )
   *
   * @return
   */
  private boolean subsectionClose(WriteOnlyStatementList subsection) throws IOException {
    if (!token.is(Token.Type.SUBOPEN)) {
      // not a subsectionClose
      reportError("The subsection must be closed - missing '"
        + Token.Type.SUBOPEN.getDescr() + Token.Type.SUBCLOSE.getDescr() + "'.");
      return false;
    }
    testStartFunction("subsectionClose");
    // its a subsectionClose so eat up SUBOPEN
    nextToken();
    matchToken(Token.Type.WHITESPACE);
    // eat up SUBCLOSE
    if (matchToken(Token.Type.SUBCLOSE) == null) {
      // bad subsection
      reportError("The subsection must be closed - '" + Token.Type.SUBOPEN.getDescr()
        + Token.Type.SUBCLOSE.getDescr() + "'.");
      testEndFunction("subsectionClose", false);
      return false;
    }
    matchToken(Token.Type.WHITESPACE);
    if (matchToken(Token.Type.EOL) == null && matchToken(Token.Type.EOF) == null) {
      // bad subsection
      reportError(
        "A subsection close must end with '" + Token.Type.SUBCLOSE.getDescr() + "'.");
      testEndFunction("section", false);
      return false;
    }
    testEndFunction("subsectionClose", true);
    return true;
  }

  /**
   * calls nextToken() and returns the matched token, if token is tokenType.
   * If token is not tokenType, returns null and does not call nextToken().
   *
   * @param tokenType
   * @return the matched token if token matches, otherwise null
   */
  private Token matchToken(final Token.Type tokenType) throws IOException {
    if (token.is(tokenType)) {
      Token matchedToken = token;
      nextToken();
      return matchedToken;
    }
    return null;
  }

  /**
   * sectionHeader => sectionType -WHITESPACE- DELIMITER -WHITESPACE- sectionName -WHITESPACE-
   *
   * @return a Section if sectionHeader found, otherwise return null
   * @throws IOException
   */
  private Section sectionHeader(final WriteOnlyStatementList nameValuePairList)
    throws IOException {
    testStartFunction("sectionHeader");
    matchToken(Token.Type.WHITESPACE);
    // get the section type
    Token type = sectionType();
    if (type == null) {
      // bad section header
      testEndFunction("sectionHeader", false);
      return null;
    }
    matchToken(Token.Type.WHITESPACE);
    if (matchToken(Token.Type.DELIMITER) == null) {
      // bad section header
      reportError("A section header must contain a delimiter - missing '"
        + tokenizer.getDelimiterString() + "'.");
      testEndFunction("sectionHeader", false);
      return null;
    }
    matchToken(Token.Type.WHITESPACE);
    // get the section name
    LinkList nameLinkList = sectionName();
    if (nameLinkList.size() == 0) {
      // bad section header
      testEndFunction("sectionHeader", false);
      return null;
    }
    matchToken(Token.Type.WHITESPACE);
    testEndFunction("sectionHeader", true);
    // assume that this is a good section and save it now
    return nameValuePairList.addSection(type, nameLinkList.getHead(), lineNum);
  }

  /**
   * sectionType => ( WORD | KEYWORD )
   *
   * @return section type or null
   */
  private Token sectionType() throws IOException {
    testStartFunction("sectionType");
    if (matchToken(Token.Type.WORD) != null || matchToken(Token.Type.KEYWORD) != null) {
      // return the section type
      testEndFunction("sectionType", true);
      return prevToken;
    }
    // did not find section type
    reportError("Missing section type (right side of the '"
      + tokenizer.getDelimiterString() + "' missing).");
    testEndFunction("sectionType", false);
    return null;
  }

  /**
   * sectionName => [ \CLOSE & SUBCLOSE & WHITESPACE & EOL & EOF\ ]
   *
   * @return section name or null
   */
  private LinkList sectionName() throws IOException {
    testStartFunction("sectionName");
    // section name may contain multiple tokens
    LinkList nameLinkList = new LinkList(token);
    // link the section name together
    while (!token.is(Token.Type.CLOSE) && !token.is(Token.Type.SUBCLOSE)
      && !token.is(Token.Type.WHITESPACE) && !token.is(Token.Type.EOL)
      && !token.is(Token.Type.EOF)) {
      nameLinkList.append(token);
      nextToken();
    }
    if (nameLinkList.size() == 0) {
      // bad section name
      reportError("Missing section name (left side of the '"
        + tokenizer.getDelimiterString() + "' missing).");
      testEndFunction("sectionName", false);
      return null;
    }
    testEndFunction("sectionName", true);
    return nameLinkList;
  }

  /**
   * pair => name -WHITESPACE- DELIMITER -WHITESPACE- -QUOTE#- value
   * <p/>
   * PEET variant:
   * pair => name -WHITESPACE- DELIMITER -WHITESPACE- value
   * <p/>
   * Adds an attribute tree to the attribute map.
   *
   * @throws IOException
   */
  private boolean pair(WriteOnlyStatementList list, final boolean required)
    throws IOException {
    if ((!token.is(Token.Type.WORD) && !token.is(Token.Type.KEYWORD)
      && !token.is(Token.Type.QUOTE))) {
      if (required) {
        reportError("Unknown statement.  Expecting a name/value pair (name "
          + tokenizer.getDelimiterString() + " value).");
      }
      return false;
    }
    testStartFunction("pair");
    NameValuePair pair = list.addNameValuePair(lineNum);
    Attribute leafAttribute = name(list, pair);
    if (leafAttribute == null) {
      // bad pair
      testEndFunction("pair", false);
      return false;
    }
    matchToken(Token.Type.WHITESPACE);
    if (matchToken(Token.Type.DELIMITER) == null) {
      // bad pair
      reportError("Missing '" + tokenizer.getDelimiterString()
        + "'.  Invalid name/value pair (name " + tokenizer.getDelimiterString()
        + " value).");
      testEndFunction("pair", false);
      return false;
    }
    matchToken(Token.Type.WHITESPACE);
    // attach the value to the last attribute
    value(list, leafAttribute, pair, peetVariant ? null : matchToken(Token.Type.QUOTE));
    testEndFunction("pair", true);
    return true;
  }

  /**
   * name => base-attribute { SEPARATOR attribute }
   *
   * @throws IOException
   */
  private Attribute name(WriteOnlyStatementList list, NameValuePair pair)
    throws IOException {
    String function = "name";
    testStartFunction(function);
    Attribute attribute = baseAttribute(list, pair);
    if (attribute == null) {
      // bad name
      testEndFunction(function, false);
      return null;
    }
    while (matchToken(Token.Type.SEPARATOR) != null) {
      // add the attribute to the map, point to the child attribute
      attribute = attribute(attribute, pair);
      if (attribute == null) {
        // bad name
        testEndFunction(function, false);
        return null;
      }
    }
    testEndFunction(function, true);
    return attribute;
  }

  /**
   * base-attribute => ( WORD | KEYWORD | QUOTE ) -attribute-
   * <p/>
   * Adds the base attribute to an attribute list and a name/value pair.
   *
   * @return Attribute or null
   */
  private Attribute baseAttribute(WriteOnlyAttributeList attributeList,
    NameValuePair pair) throws IOException {
    Attribute attribute = buildAttribute(attributeList, pair, true);
    if (attribute == null) {
      reportError("Missing attribute.");
    }
    return attribute;
  }

  /**
   * attribute => [ WORD | KEYWORD | COMMENT | QUOTE ]
   * <p/>
   * Adds attributes to an attribute list and a name/value pair.
   *
   * @return Attribute or null
   */
  private Attribute attribute(WriteOnlyAttributeList attributeList, NameValuePair pair)
    throws IOException {
    Attribute attribute = buildAttribute(attributeList, pair, false);
    if (attribute == null) {
      // bad name
      reportError("Another attribute must follow the '" + Token.Type.SEPARATOR.getDescr()
        + "' (attribute" + Token.Type.SEPARATOR.getDescr() + "attribute"
        + Token.Type.SEPARATOR.getDescr() + "attribute...).");
    }
    return attribute;
  }

  /**
   * Builds base-attribute and attribute
   *
   * @param attributeList
   * @param pair
   * @param base
   * @return
   * @throws IOException
   */
  private Attribute buildAttribute(final WriteOnlyAttributeList attributeList,
    final NameValuePair pair, final boolean base) throws IOException {
    String function;
    if (base) {
      function = "base-attribute";
    }
    else {
      function = "attribute";
    }
    if (base) {
      testStartFunction(function);
    }
    if (!token.is(Token.Type.WORD) && !token.is(Token.Type.KEYWORD)
      && !token.is(Token.Type.QUOTE) && (base || !token.is(Token.Type.COMMENT))) {
      if (base) {
        testEndFunction(function, false);
      }
      return null;
    }
    if (!base) {
      testStartFunction(function);
    }
    LinkList valueLinkList = new LinkList(token);
    valueLinkList.append(token);
    nextToken();
    while (token.is(Token.Type.WORD) || token.is(Token.Type.KEYWORD)
      || token.is(Token.Type.QUOTE) || token.is(Token.Type.COMMENT)) {
      valueLinkList.append(token);
      nextToken();
    }
    testEndFunction(function, true);
    // add and return the new attribute
    Attribute attribute =
      (Attribute) attributeList.addAttribute(valueLinkList.getHead(), lineNum);
    pair.addAttribute(attribute);
    return attribute;
  }

  /**
   * value => { \EOL & EOF\ } ( EOL | EOF ) { (!quoted & valueline ) | ( quoted & ( quotedValueLine | #QUOTE ) ) }
   * <p/>
   * sets the value in the attribute, if the value exists
   *
   * @throws IOException
   */
  private void value(WriteOnlyStatementList parent, Attribute attribute,
    NameValuePair pair, final Token closeQuote) throws IOException {
    testStartFunction("value");
    // values can be made of multiple tokens, so use a link list
    LinkList valueLinkList = new LinkList(token);
    boolean foundCloseQuote = false;
    while (!token.is(Token.Type.EOL) && !token.is(Token.Type.EOF)) {
      // add the token to the value link list
      valueLinkList.append(token);
      // look for the close quote
      if (closeQuote != null) {
        if (token.equals(closeQuote)) {
          foundCloseQuote = true;
        }
        // check the tokens that follow a possible close quote
        else if (foundCloseQuote) {
          // ignore embedded quotes
          if (!token.is(Token.Type.WHITESPACE)) {
            foundCloseQuote = false;
          }
        }
      }
      nextToken();
    }
    // check for keywords
    processMetaData(attribute);
    // Finished the first line of the value (excluding the EOL) if this is
    // delimiter reassignment, it must be set now, or the following pair will be
    // mistaken for part of this value.
    if (isDelimiterChange(attribute)) {
      tokenizer.setDelimiterString(valueLinkList.getHead().getValues());
      pair.setDelimiterChange(valueLinkList.getHead());
    }
    // grab the EOL in case the value continues in the value line
    if (token.is(Token.Type.EOL)) {
      valueLinkList.append(token);
    }
    nextToken();
    if (!foundCloseQuote) {
      if (closeQuote == null) {
        while (!error && valueLine(parent, valueLinkList)) {}
      }
      else {
        while (!error && quotedValueLine(parent, valueLinkList, closeQuote)) {}
      }
    }
    // Strip non-embedded EOL, EOF, and WHITESPACE at the start and end of the value
    while (valueLinkList.size() > 0
      && (valueLinkList.isFirstElement(Token.Type.WHITESPACE)
        || valueLinkList.isFirstElement(Token.Type.EOL)
        || valueLinkList.isFirstElement(Token.Type.EOF))) {
      valueLinkList.dropFirstElement();
    }
    while (valueLinkList.size() > 0 && (valueLinkList.isLastElement(Token.Type.WHITESPACE)
      || valueLinkList.isLastElement(Token.Type.EOL)
      || valueLinkList.isLastElement(Token.Type.EOF))) {
      valueLinkList.dropLastElement();
    }
    // Remove the closing quote
    if (closeQuote != null && valueLinkList.lastElementEquals(closeQuote)) {
      valueLinkList.dropLastElement();
    }
    // Save the value to the name/value pair even if it doesn't exist. This is
    // how the name/value pair knows that its name is complete and can assign itself
    // to the last attribute
    pair.addValue(valueLinkList.getHead());
    testEndFunction("value", true);
    return;
  }

  /**
   * valueLine =>  !emptyLine !DelimiterInLine !comment { \DELIMITER & SUBOPEN & EOL & EOF\ } ( EOL | EOF )
   * <p/>
   * Adds to the end of a value link list
   * returns the new end of the link list
   *
   * @throws IOException
   */
  private boolean valueLine(WriteOnlyStatementList parent, LinkList valueLinkList)
    throws IOException {
    if (emptyLine(parent) || delimiterInLine || token.is(Token.Type.COMMENT)
      || token.is(Token.Type.EOL) || token.is(Token.Type.EOF)
      || token.is(Token.Type.SUBOPEN)) {
      // not a value line
      return false;
    }
    testStartFunction("valueLine");
    while (!token.is(Token.Type.DELIMITER) && !token.is(Token.Type.EOL)
      && !token.is(Token.Type.EOF)) {
      // add the token to the value link list
      valueLinkList.append(token);
      nextToken();
    }
    if (token.is(Token.Type.DELIMITER)) {// really bad error - preprocessor is wrong
      // bad value line
      reportError("A value line cannot contain the delimiter string (\""
        + tokenizer.getDelimiterString() + "\").");
      testEndFunction("valueLine", false);
      return false;
    }
    // grab the EOL in case another value line follows
    if (token.is(Token.Type.EOL)) {
      valueLinkList.append(token);
    }
    nextToken();
    testEndFunction("valueLine", true);
    return true;
  }

  /**
   * quotedValueLine => !emptyLine { \EOL & EOF\ } #QUOTE -WHITESPACE- ( EOL | EOF )
   * <p/>
   * Adds to the end of a value link list
   * returns the new end of the link list
   *
   * @param closeQuote quote to match - must not be null
   * @throws IOException
   */
  private boolean quotedValueLine(WriteOnlyStatementList parent, LinkList valueLinkList,
    final Token closeQuote) throws IOException {
    if (valueLinkList.isDone()) {
      // quoted value was completed in the previous line
      return false;
    }
    if (emptyLine(parent) || token.is(Token.Type.EOL)) {
      // illegal empty line inside a quoted value
      reportError("An empty line cannot be embedded in a quoted value (\""
        + closeQuote.getChar() + "\").");
      return false;
    }
    if (token.is(Token.Type.EOF)) {
      // incomplete quoted value
      reportError("Close quote not found (\"" + closeQuote.getChar() + "\").");
      return false;
    }
    testStartFunction("valueLine");
    boolean foundCloseQuote = false;
    while (!token.is(Token.Type.EOL) && !token.is(Token.Type.EOF)) {
      // add the token to the value link list
      valueLinkList.append(token);
      // look for the close quote
      if (closeQuote != null) {
        if (token.equals(closeQuote)) {
          foundCloseQuote = true;
        }
        // check the tokens that follow a possible close quote
        else if (foundCloseQuote) {
          // ignore embedded quotes
          if (!token.is(Token.Type.WHITESPACE)) {
            foundCloseQuote = false;
          }
        }
      }
      nextToken();
    }
    // grab the EOL in case another value line follows
    if (token.is(Token.Type.EOL)) {
      valueLinkList.append(token);
    }
    if (foundCloseQuote) {
      // close quote has been found - added a done flag to the value link list
      valueLinkList.setDone();
    }
    else if (token.is(Token.Type.EOF)) {
      // incomplete quoted value
      reportError("Close quote not found (\"" + closeQuote.getChar() + "\").");
      testEndFunction("valueLine", false);
      return false;
    }
    nextToken();
    testEndFunction("valueLine", true);
    return true;
  }

  private void reportError(String message) throws IOException {
    if (message == null) {
      message = "Unknown error.";
    }
    error = true;
    String errorMessage = "Line# " + lineNum + ": " + message;
    int errorIndex = tokenIndex - 1;
    int size = line.size();
    Token token = null;
    String value = null;
    int errorPosition = 0;
    int index = 0;
    StringBuffer printLine = new StringBuffer(124);
    StringBuffer carat = new StringBuffer(124);
    for (index = 0; index <= errorIndex && index < size - 1; index++) {
      token = (Token) line.get(index);
      value = token.getValue();
      if (index < errorIndex) {
        errorPosition += value.length();
      }
      printLine.append(value);
    }
    for (index = errorIndex + 1; index < size - 1; index++) {
      token = (Token) line.get(index);
      printLine.append(token.getValue());
    }
    for (index = 0; index < errorPosition; index++) {
      carat.append(' ');
    }
    carat.append("^");
    if (!errorsFound) {
      errorsFound = true;
      if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
        if (logFile != null) {
          System.err.println("Errors in " + logFile.getName());
        }
        else {
          System.err.println("Errors found");
        }
        System.err.println();
      }
    }
    if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
      System.err.println(errorMessage);
      System.err.println(printLine);
      System.err.println(carat);
      System.err.println();
      if (errMsg != null) {
        errMsg.append(errorMessage + "\n" + printLine + "\n\n");
      }
    }
    if (detailedTest) {
      throw new IllegalStateException();
    }
    tokenIndex = line.size();
    nextToken();
  }

  /**
   * get the next token
   * set prevToken and prevPrevToken
   */
  private void nextToken() throws IOException {
    if (token != null && token.is(Token.Type.EOF)) {
      // It may try nextToken() a few times at the end of file before it figures
      // out that its done, but that's OK
      return;
    }
    if (line == null || tokenIndex == line.size()) {
      preprocess();
      tokenIndex = 0;
    }
    prevPrevToken = prevToken;
    prevToken = token;
    token = (Token) line.get(tokenIndex);
    tokenIndex++;
    if (detailedTest && (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG)) {
      System.err.println(tokenIndex + ":" + token + ",delimiterInLine=" + delimiterInLine
        + ":" + tokenizer.getDelimiterString());
    }
  }

  boolean isBeginningOfLine() {
    return tokenIndex == 1;
  }

  /**
   * Set line level preprocessor flag delimiterInLine.
   * Also set the line number
   */
  private void preprocess() throws IOException {
    Token token = null;
    lineNum++;
    line.clear();
    delimiterInLine = false;
    do {
      token = tokenizer.next();
      if (token.is(Token.Type.DELIMITER)) {
        delimiterInLine = true;
      }
      line.add(token);
    } while (!token.is(Token.Type.EOL) && !token.is(Token.Type.EOF));
  }

  // postprocessor

  private void processMetaData(Attribute attribute) {
    if (!attribute.isBase()) {
      return;
    }
    Token name = attribute.getNameToken();
    if (attribute.isGlobal()) {
      if (name.equals(Token.Type.KEYWORD, AutodocTokenizer.VERSION_KEYWORD)) {
        versionFound = true;
        return;
      }
      if (name.equals(Token.Type.KEYWORD, AutodocTokenizer.PIP_KEYWORD)) {
        pipFound = true;
        return;
      }
    }
  }

  private boolean isDelimiterChange(Attribute attribute) {
    if (attribute.getNameToken().equals(Token.Type.KEYWORD,
      AutodocTokenizer.DELIMITER_KEYWORD)) {
      return true;
    }
    return false;
  }

  void testStreamTokenizer(boolean tokens, boolean details)
    throws IOException, LogFile.LockException {
    tokenizer.testStreamTokenizer(tokens, details);
  }

  void testPrimativeTokenizer(boolean tokens) throws IOException, LogFile.LockException {
    tokenizer.testPrimativeTokenizer(tokens);
  }

  void testAutodocTokenizer(boolean tokens) throws IOException, LogFile.LockException {
    tokenizer.test(tokens);
  }

  void testPreprocessor(boolean tokens) throws IOException, LogFile.LockException {
    if (tokens) {
      System.err.println("(type,value):delimiterInLine");
    }
    tokenizer.initialize();
    do {
      nextToken();
      if (tokens) {
        System.err.println(token.toString() + ":" + delimiterInLine);
      }
      else if (token.is(Token.Type.EOL)) {
        System.err.println();
      }
      else if (!token.is(Token.Type.EOF)) {
        System.err.print(token.getValue());
      }
    } while (!token.is(Token.Type.EOF));
  }

  /**
   * @param tokens:  display tokens rather then text.
   * @param details: display more information and throw an exception as the
   *                 first error.
   * @throws IOException
   */
  void test(boolean tokens, boolean details) throws IOException, LogFile.LockException {
    detailedTest = details;
    test = true;
    testWithTokens = tokens;
    initialize();
    parse();
  }

  private void testStartFunction(String functionName) {
    if (test) {
      testIndent++;
      printTestIndent();
      System.err.println(functionName + " {");
    }
  }

  private boolean testEndFunction(String functionName, boolean result) {
    if (test) {
      printTestIndent();
      if (result) {
        System.err.println(functionName + " } succeeded");
      }
      else {
        System.err.println(functionName + " } failed");
      }
      testIndent--;
    }
    return result;
  }

  private void printTestLine() {
    if (lastLinePrinted == lineNum) {
      return;
    }
    lastLinePrinted = lineNum;
    Token token = null;
    int size = line.size();
    printTestIndent();
    System.err.print("Line# " + lineNum + ": ");
    for (int index = 0; index < size; index++) {
      token = (Token) line.get(index);
      if (testWithTokens) {
        System.err.print(token);
      }
      else if (!token.is(Token.Type.EOL) && !token.is(Token.Type.EOF)) {
        System.err.print(token.getValue());
      }
    }
    System.err.println();
  }

  private void printTestIndent() {
    for (int i = 0; i < testIndent; i++) {
      System.err.print("\t");
    }
  }

  private final static class LinkList {
    Token head;
    Token tail;
    int size;
    boolean done = false;

    LinkList(Token start) {
      head = start;
      tail = head;
      size = 1;
    }

    private void setDone() {
      done = true;
    }

    private boolean isDone() {
      return done;
    }

    void append(Token token) {
      if (tail == null) {
        head = token;
        tail = token;
        size = 1;
      }
      else {
        tail = tail.setNext(token);
        size++;
      }
    }

    int size() {
      return size;
    }

    boolean isFirstElement(Token.Type compareType) {
      if (head == null) {
        return false;
      }
      return head.is(compareType);
    }

    boolean isLastElement(Token.Type compareType) {
      if (tail == null) {
        return false;
      }
      return tail.is(compareType);
    }

    void dropFirstElement() {
      if (head == null) {
        return;
      }
      head = head.dropFromList();
      if (head == null) {
        tail = null;
        size = 0;
      }
      else {
        size--;
      }
    }

    boolean lastElementEquals(final Token token) {
      if (tail == null) {
        return token == null;
      }
      return tail.equals(token);
    }

    void dropLastElement() {
      if (tail == null) {
        return;
      }
      tail = tail.dropFromList();
      if (tail == null) {
        head = null;
        size = 0;
      }
      else {
        size--;
      }
    }

    Token getHead() {
      return head;
    }

    public String toString() {
      return getHead().toString();
    }
  }
}
