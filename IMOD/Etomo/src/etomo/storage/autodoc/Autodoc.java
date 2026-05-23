package etomo.storage.autodoc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.storage.LogFile;
import etomo.type.AxisID;
import etomo.ui.swing.Token;
import etomo.util.EnvironmentVariable;
import etomo.util.Utilities;

/**
 * <p>Description:  Data storage for an autodoc file.
 * <p/>
 * Versions:
 * 1.0
 * 1.1:  Added the break character "^".  When a value is formatted, the "^" is
 * replaced with a "\n".
 * 1.2:  Handling duplicate attribute names.  Duplicate attributes are
 * attributes with the same parentage (section and parent attribute names) and
 * the same name.  Before version 1.2 the last value assigned to a duplicate
 * attribute could be retrieved by using the name as a key.  In version 1.2
 * the first value assigned to a duplicate attribute can be retrieved by using the
 * name as a key.  When attributes of the same parentage are retrieved as an
 * ordered list, each duplicate attribute, and their different values, will be
 * included in the list.
 * <p/>
 * The break character is no longer being handled by autodoc.  It is now considered
 * a "flavor" of autodoc.
 * <p/>
 * 1.3:  Fixed handling duplicate attribute names (see 1.2).  The rule for
 * autodoc is that is keeps the last value.  So it needs to keep acting like it
 * is doing this even though it is actually keeping  all the values.
 * <p/>
 * To Use:
 * <p/>
 * Set up environment variables:  make sure that either the AUTODOC_DIR or
 * IMOD_DIR/autodoc points to an autodoc directory (AUTODOC_DIR) is checked
 * first.  An autodoc file can also be placed in the current working directory.
 * <p/>
 * Call Autodoc.get(String) with the constant refering to the autodoc file you wish to
 * load.
 * <p/>
 * Use the get... and next... functions to retrieve sections and attributes.
 * <p/>
 * <p/>
 * To Test:
 * Call the print() function to print everything in the autodoc.
 * Test the parser of the autodoc file by modifying the code.  Set testMode to
 * true.  Uncomment the test function you wish to use.
 * <p/>
 * <p/>
 * Possible Upgrades:
 * <p/>
 * Required:  The Autodoc file names need to be added to this object.
 * <p/>
 * The ability to open an unknown autodoc file.
 * <p/>
 * Make testing accessible without modifying the code.
 * <p/>
 * After parsing is finished the Autodoc should be readOnly.  Allow Autodoc to be
 * set to ReadOnly.
 * <p/>
 * <p>Copyright: Copyright 2002 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * @notthreadsafe
 */
public final class Autodoc extends WriteOnlyStatementList implements WritableAutodoc {
  /**
   * The autodoc file name, excluding the extension.
   */
  private final String autodocName;

  private AutodocParser parser = null;

  // data
  private final List<Section> sectionList = new ArrayList<Section>();
  private final HashMap<String, Section> sectionMap = new HashMap<String, Section>();
  private final List<Statement> statementList = new ArrayList<Statement>();
  private final AttributeList attributeList;
  private String currentDelimiter = AutodocTokenizer.DEFAULT_DELIMITER;
  private boolean debug = false;
  private boolean writable = false;
  private final StringBuilder errMsg;

  Autodoc(String autodocName, final StringBuilder errMsg) {
    this.autodocName = autodocName;
    this.errMsg = errMsg;
    attributeList = new AttributeList(this);
  }

  /**
   * Wraps all global attribute leaf values in the autodoc, except values starting with
   * noWrapTag.  If a value starts with wrapTag, uses divider, otherwise uses
   * defaultDivider.  Not wrapping non-global attributes as this is not currently
   * required.
   * @param noWrapPrefix
   * @param wrapPrefix
   * @param divider
   * @param defaultDivider
   */
  public void wrapAttributeValues(final String noWrapPrefix, final String wrapPrefix,
    final String divider, final String defaultDivider, final int minLength,
    final int wrapLength) {
    int size = statementList.size();
    for (int i = 0; i < size; i++) {
      statementList.get(i).wrapValue(noWrapPrefix, wrapPrefix, divider, defaultDivider,
        minLength, wrapLength);
    }
    if (sectionList.size() > 0) {
      System.err.println("Info: Non-global sections will not be wrapped.");
    }
  }

  public void setDebug() {
    debug = true;
  }

  public String getAutodocName() {
    return autodocName;
  }

  public String getName() {
    if (parser != null) {
      return parser.getFileName();
    }
    return null;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  Section addSection(Token type, Token name, final int lineNum) {
    Section existingSection = null;
    String key = Section.getKey(type, name);
    existingSection = (Section) sectionMap.get(key);
    if (existingSection == null) {
      Section newSection = new Section(type, name, this);
      sectionList.add(newSection);
      sectionMap.put(newSection.getKey(), newSection);
      return newSection;
    }
    return existingSection;
  }

  boolean isGlobal() {
    return true;
  }

  boolean isAttribute() {
    return false;
  }

  void setCurrentDelimiter(Token newDelimiter) {
    currentDelimiter = newDelimiter.getValues();
  }

  String getCurrentDelimiter() {
    return currentDelimiter;
  }

  WriteOnlyAttributeList addAttribute(final Token name, final int lineNum) {
    return attributeList.addAttribute(name, lineNum);
  }

  public void write() throws LogFile.LockException, IOException {
    if (!writable) {
      new IllegalStateException("Not a writable autodoc.").printStackTrace();
      return;
    }
    LogFile autodocFile = parser.getLogFile();
    LogFile.WriterId writerId = autodocFile.openWriter();
    for (int i = 0; i < statementList.size(); i++) {
      ((Statement) statementList.get(i)).write(autodocFile, writerId);
    }
    for (int j = 0; j < sectionList.size(); j++) {
      sectionList.get(j).write(autodocFile, writerId);
    }
    autodocFile.closeWriter(writerId);
  }

  public LogFile getLogFile() {
    return parser.getLogFile();
  }

  public void addNameValuePairAttribute(final String name, final String value) {
    addNameValuePairAttribute(name, value, 0);
  }

  /**
   * add a name/value pair with a name containing one attribute
   */
  public void addNameValuePairAttribute(String name, final String value,
    final int lineNum) {
    if (name == null) {
      return;
    }
    name = name.trim();
    if (name.indexOf(AutodocTokenizer.SEPARATOR_CHAR) == -1) {
      // Not dot separators - entire attribute is saved at this level.
      // add attribute
      Token nameToken = new Token();
      nameToken.set(Token.Type.ANYTHING, name);
      attributeList.addAttribute(nameToken, lineNum);
      // add value to attribute
      Attribute attribute = attributeList.getAttribute(name);
      Token valueToken = new Token();
      valueToken.set(Token.Type.ANYTHING, value);
      // add name/value pair
      NameValuePair pair = addNameValuePair(lineNum);
      // add attribute and value to pair
      pair.addAttribute(attribute);
      pair.addValue(valueToken);
    }
    else {
      // Multiple-level attribute
      attributeList.addAttribute(0,
        name.split("\\Q" + AutodocTokenizer.SEPARATOR_CHAR + "\\E"), lineNum, value,
        addNameValuePair(lineNum));
    }
  }

  /**
   * Removes a simple (single attribute) name/value pair.  Removes the occurrance
   * of the attribute in the name/value pair.
   *
   * @Return the previous statement in statementList
   */
  public WritableStatement removeNameValuePair(String name) {
    Attribute attribute = attributeList.getAttribute(name);
    if (attribute == null) {
      // unable to find an attribute with this name
      return null;
    }
    NameValuePair pair = attribute.getNameValuePair();
    statementList.remove(pair);
    return pair.remove();
  }

  /**
   * Removes a statement.
   *
   * @Return the previous statement in statementList
   */
  public WritableStatement removeStatement(WritableStatement statement) {
    statementList.remove(statement);
    return statement.remove();
  }

  public ReadOnlyAttribute getAttribute(String name) {
    return attributeList.getAttribute(name);
  }

  public WritableAttribute getWritableAttribute(String name) {
    return attributeList.getAttribute(name);
  }

  NameValuePair addNameValuePair(final int lineNum) {
    NameValuePair pair = new NameValuePair(this, getMostRecentStatement(), lineNum);
    statementList.add(pair);
    return pair;
  }

  public void addComment(Token comment, final int lineNum) {
    statementList.add(new Comment(comment, this, getMostRecentStatement(), lineNum));
  }

  public void addComment(String comment, final int lineNum) {
    Token token = new Token();
    token.set(Token.Type.ANYTHING, comment);
    addComment(token, lineNum);
  }

  public void addEmptyLine(final int lineNum) {
    statementList.add(new EmptyLine(this, getMostRecentStatement(), lineNum));
  }

  public StatementLocation getStatementLocation() {
    if (statementList == null) {
      return null;
    }
    return new StatementLocation();
  }

  public Statement nextStatement(StatementLocation location) {
    if (location == null || location.isOutOfRange(statementList)) {
      return null;
    }
    Statement statement = (Statement) statementList.get(location.getIndex());
    location.increment();
    return statement;
  }

  public ReadOnlySection getSection(String type, String name) {
    if (debug) {
      System.out.println("Autodoc.getSection:type=" + type + ",name=" + name);
    }
    if (sectionMap == null) {
      return null;
    }
    String key = Section.getKey(type, name);
    if (debug) {
      System.out.println("Autodoc.getSection:key=" + key);
    }
    Section section = (Section) sectionMap.get(key);
    if (debug) {
      System.out.println("Autodoc.getSection:section=" + section);
    }
    return section;
  }

  public boolean sectionExists(String type) {
    return getSectionLocation(type) != null;
  }

  /**
   * Sets a SectionLocation index to the first section with the type the same as
   * the type parameter.  Returns the SectionLocation index.
   */
  public SectionLocation getSectionLocation(String type) {
    Section section = null;
    for (int i = 0; i < sectionList.size(); i++) {
      section = (Section) sectionList.get(i);
      if (section.equalsType(type)) {
        return new SectionLocation(type, i);
      }
    }
    return null;
  }

  public SectionLocation getSectionLocation() {
    if (sectionList.size() > 0) {
      return new SectionLocation(0);
    }
    return null;
  }

  /**
   * Starts with the section that location is pointing to returns the first
   * section which the same type as location.  Increments location.
   */
  public ReadOnlySection nextSection(SectionLocation location) {
    if (location == null) {
      return null;
    }
    Section section = null;
    for (int i = location.getIndex(); i < sectionList.size(); i++) {
      section = (Section) sectionList.get(i);
      if (section.equalsType(location.getType())) {
        location.setIndex(i + 1);
        return section;
      }
    }
    return null;
  }

  public HashMap getAttributeValues(String sectionType, String attributeName) {
    return getAttributeValues(sectionType, attributeName, false);
  }

  public HashMap getAttributeMultiLineValues(String sectionType, String attributeName) {
    return getAttributeValues(sectionType, attributeName, true);
  }

  private Statement getMostRecentStatement() {
    if (statementList.size() == 0) {
      return null;
    }
    return (Statement) statementList.get(statementList.size() - 1);
  }

  /**
   * Returns a HashMap containing a list of attribute values, keyed by
   * sectionName.  The elements in each list is using section type and attribute
   * name.  Each list is saved in attributeValuesCache.  If a saved list is
   * requested, then the cached HashMap is returned.
   *
   * @param sectionType
   * @param attributeName
   * @return
   */
  private HashMap getAttributeValues(String sectionType, String attributeName,
    boolean multiLine) {
    if (sectionType == null || attributeName == null) {
      return null;
    }
    // Create attributeValues
    HashMap attributeValues = new HashMap();
    SectionLocation sectionLocation = getSectionLocation(sectionType);
    ReadOnlySection section = nextSection(sectionLocation);
    while (section != null) {
      try {
        String sectionName = section.getName();
        if (multiLine) {
          attributeValues.put(sectionName,
            section.getAttribute(attributeName).getMultiLineValue());
        }
        else {
          attributeValues.put(sectionName,
            section.getAttribute(attributeName).getValue());
        }
      }
      catch (NullPointerException e) {
        // An attribute with attributeName doesn't exist
      }
      // Go to next section
      section = nextSection(sectionLocation);
    }
    return attributeValues;
  }

  public void printStatementList() {
    System.out.println("statementList=" + statementList);
  }

  public void printStoredData() {
    System.out.println("Printing stored data:");
    // name value pair list
    System.out.println("LIST:");
    if (statementList != null) {
      Statement statement = null;
      for (int i = 0; i < statementList.size(); i++) {
        statement = (Statement) statementList.get(i);
        statement.print(0);
      }
    }
    // attribute map
    System.out.println("Attributes:");
    attributeList.print(0);
    // section list
    for (int i = 0; i < sectionList.size(); i++) {
      Section section = (Section) sectionList.get(i);
      section.print(0);
    }
  }

  static void printIndent(int level) {
    if (level == 0) {
      return;
    }
    for (int i = 0; i < level; i++) {
      System.out.print("  ");
    }
  }

  public boolean exists() {
    return parser.exists();
  }

  public String getString() {
    if (parser == null) {
      return "";
    }
    return parser.getAbsolutePath();
  }

  public String toString() {
    if (parser == null) {
      return "";
    }
    return parser.getAbsolutePath();
  }

  private File getDir(BaseManager manager, String envVariable, String dirName,
    AxisID axisID) {
    File parentDir = Utilities.getExistingDir(manager, envVariable, axisID);
    if (parentDir == null) {
      return null;
    }
    File dir = new File(parentDir, dirName);
    if (!Utilities.checkExistingDir(dir, envVariable)) {
      return null;
    }
    return dir;
  }

  void initializeGenericInstance(final BaseManager manager, final String envVar,
    final String subdirName, final String name, final AxisID axisID,
    final boolean writable)
    throws FileNotFoundException, IOException, LogFile.LockException {
    parser = AutodocParser.getGenericInstance(this, false, true, false, envVar,
      subdirName, name, manager, axisID, null, debug, writable);
    // To test comment out initialize and parse and uncomment runInternalTest.
    // runInternalTest(InternalTestType.STREAM_TOKENIZER, true, false);
    parser.initialize();
    parser.parse();
  }

  void initializeGenericInstance(final BaseManager manager, final File autodocFile,
    final AxisID axisID, final boolean writable, final StringBuilder errMsg)
    throws FileNotFoundException, IOException, LogFile.LockException {
    this.writable = writable;
    parser = AutodocParser.getGenericInstance(this, false, false, false, autodocFile,
      manager, axisID, null, debug, writable, errMsg);
    // To test comment out initialize and parse and uncomment runInternalTest.
    // runInternalTest(InternalTestType.STREAM_TOKENIZER, true, false);
    if (autodocFile.exists()) {
      parser.initialize();
      parser.parse();
    }
  }

  public boolean isDebug() {
    return debug;
  }

  void initializeMatlabInstance(final BaseManager manager, final File file,
    final boolean writable)
    throws FileNotFoundException, IOException, LogFile.LockException {
    this.writable = writable;
    parser = AutodocParser.getGenericInstance(this, true, false, true, file, manager,
      null, null, debug, true, null);
    // To test comment out initialize and parse and uncomment runInternalTest.
    // runInternalTest(InternalTestType.STREAM_TOKENIZER, true, false);
    parser.initialize();
    parser.parse();
  }

  void initializeWritableInstance(final BaseManager manager, final File file)
    throws FileNotFoundException, IOException, LogFile.LockException {
    writable = true;
    parser = AutodocParser.getGenericInstance(this, false, false, false, file, manager,
      null, null, debug, true, null);
    // To test comment out initialize and parse and uncomment runInternalTest.
    // runInternalTest(InternalTestType.STREAM_TOKENIZER, true, false);
    parser.initialize();
    parser.parse();
  }

  void initializeEmptyWritableInstance(BaseManager manager, File file)
    throws FileNotFoundException, IOException, LogFile.LockException {
    this.writable = true;
    parser = AutodocParser.getGenericInstance(this, false, false, false, file, manager,
      null, null, debug, true, null);
  }

  /**
   * Initializes a writable peet autodoc.  Parse is not initialized
   *
   * @param manager
   * @param file
   * @throws FileNotFoundException
   * @throws IOException
   * @throws LogFile.LockException
   */
  void initializeEmptyMatlapInstance(BaseManager manager, File file)
    throws FileNotFoundException, IOException, LogFile.LockException {
    this.writable = true;
    parser = AutodocParser.getGenericInstance(this, true, false, true, file, manager,
      null, null, debug, true, null);
  }

  void initializeAutodocInstance(BaseManager manager, String name, AxisID axisID)
    throws FileNotFoundException, IOException, LogFile.LockException {
    parser = AutodocParser.getAutodocInstance(this, false, true, false, name, manager,
      axisID, null, debug);
    // To test comment out initialize and parse and uncomment runInternalTest.
    // runInternalTest(InternalTestType.STREAM_TOKENIZER, true, false);
    parser.initialize();
    parser.parse();
  }

  void initializeUnmanagedAutodocInstance(final BaseManager manager, final String name,
    final File autodocFile, final AxisID axisID)
    throws FileNotFoundException, IOException, LogFile.LockException {
    parser = AutodocParser.getUnmanagedAutodocInstance(this, false, true, false, name,
      autodocFile, manager, axisID, null);
    // To test comment out initialize and parse and uncomment runInternalTest.
    // runInternalTest(InternalTestType.STREAM_TOKENIZER, true, false);
    parser.initialize();
    parser.parse();
  }

  void initializeUITestInstance(BaseManager manager, String name, AxisID axisID)
    throws FileNotFoundException, IOException, LogFile.LockException {
    parser = AutodocParser.getGenericInstance(this, false, true, false,
      EtomoDirector.SOURCE_ENV_VAR, null, name, manager, axisID, null, debug, false);
    // To test comment out initialize and parse and uncomment runInternalTest.
    // runInternalTest(InternalTestType.STREAM_TOKENIZER, true, false);
    parser.initialize();
    parser.parse();
  }

  void initializeCpuInstance(BaseManager manager, String name, AxisID axisID)
    throws FileNotFoundException, IOException, LogFile.LockException {
    parser = AutodocParser.getGenericInstance(this, false, true, false,
      EnvironmentVariable.CALIB_DIR, null, name, manager, axisID,
      "Info:  No local calibration information is available.  There is no "
        + "cpu.adoc file.  Parallel processing on multiple machines will not "
        + "be available unless it has been enabled in the Etomo Settings "
        + "dialog (under the Options menu).",
      debug, false);
    // To test comment out initialize and parse and uncomment runInternalTest.
    // runInternalTest(InternalTestType.STREAM_TOKENIZER, true, false);
    parser.initialize();
    parser.parse();
  }

  /**
   * Initializes parser and prints parsing data instead of storing it.
   *
   * @param type
   * @param showTokens
   * @param showDetails
   * @throws IOException
   * @throws LogFile.LockException
   */
  public void runInternalTest(InternalTestType type, boolean showTokens,
    boolean showDetails) throws IOException, LogFile.LockException {
    System.out.println("runInternalTest");
    if (type == InternalTestType.STREAM_TOKENIZER) {
      parser.testStreamTokenizer(showTokens, showDetails);
    }
    else if (type == InternalTestType.PRIMATIVE_TOKENIZER) {
      parser.testPrimativeTokenizer(showTokens);
    }
    else if (type == InternalTestType.AUTODOC_TOKENIZER) {
      parser.testAutodocTokenizer(showTokens);
    }
    else if (type == InternalTestType.PREPROCESSOR) {
      parser.testPreprocessor(showTokens);
    }
    else if (type == InternalTestType.PARSER) {
      parser.test(showTokens, showDetails);
    }
  }

  public boolean isError() {
    if (parser == null) {
      return true;
    }
    return parser.isError();
  }

  static final class InternalTestType {
    static final InternalTestType STREAM_TOKENIZER = new InternalTestType();
    static final InternalTestType PRIMATIVE_TOKENIZER = new InternalTestType();
    static final InternalTestType AUTODOC_TOKENIZER = new InternalTestType();
    static final InternalTestType PREPROCESSOR = new InternalTestType();
    static final InternalTestType PARSER = new InternalTestType();

    private InternalTestType() {}
  }
}
/**
 *<p> $$Log$
 *<p> $Revision 1.37  2010/11/13 16:05:36  sueh
 *<p> $bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 *<p> $
 *<p> $Revision 1.36  2010/05/28 18:48:29  sueh
 *<p> $bug# 1360 In initializeCpu changed the error message for a missing ImodCalib
 * directory
 *<p> $to mention setting up parallel processing using Settings dialog.
 *<p> $
 *<p> $Revision 1.35  2010/05/20 23:50:59  sueh
 *<p> $bug# 1360 In initializeCpu using a custom failure message.  Changing file
 *<p> $load functions to allow the custom message.
 *<p> $
 *<p> $Revision 1.34  2010/02/17 04:49:43  sueh
 *<p> $bug# 1301 Using the manager instead of the manager key do pop up
 *<p> $messages.
 *<p> $
 *<p> $Revision 1.33  2010/01/14 22:06:39  sueh
 *<p> $bug# 1299 In exists() checking that the autodoc file is not null.  If it is null
 *<p> $then the directory that the file is supposed to be in wasn't found.
 *<p> $
 *<p> $Revision 1.32  2010/01/11 23:57:36  sueh
 *<p> $bug# 1299 Added exists.
 *<p> $
 *<p> $Revision 1.31  2009/06/05 02:00:28  sueh
 *<p> $bug# 1219 Added autodocName.
 *<p> $
 *<p> $Revision 1.30  2009/03/17 00:45:43  sueh
 *<p> $bug# 1186 Pass managerKey to everything that pops up a dialog.
 *<p> $
 *<p> $Revision 1.29  2009/03/09 17:26:24  sueh
 *<p> $bug# 1199 Documents version 1.3.
 *<p> $
 *<p> $Revision 1.28  2009/02/04 23:30:00  sueh
 *<p> $bug# 1158 Changed id and exceptions classes in LogFile.
 *<p> $
 *<p> $Revision 1.27  2009/01/20 19:32:37  sueh
 *<p> $bug# 1102 Hooked up internal testing.
 *<p> $
 *<p> $Revision 1.26  2008/10/27 18:34:58  sueh
 *<p> $bug# 1141 Added debug.
 *<p> $
 *<p> $Revision 1.25  2008/05/30 21:22:21  sueh
 *<p> $bug# 1102 Added commandLanguage and writable.  Will be used to limit
 *<p> $functionality of regular autodocs to original autodoc definition.
 *<p> $
 *<p> $Revision 1.24  2008/01/31 20:24:40  sueh
 *<p> $bug# 1055 throwing a FileException when LogFile.getInstance fails.
 *<p> $
 *<p> $Revision 1.23  2007/08/01 22:43:05  sueh
 *<p> $bug# 985 Added runInternalTest to ReadOnlyAutodoc.
 *<p> $
 *<p> $Revision 1.22  2007/07/17 21:32:52  sueh
 *<p> $bug# 1018 Added toString().
 *<p> $
 *<p> $Revision 1.21  2007/06/07 21:31:26  sueh
 *<p> $bug# 1012 Moved the file back up out of Autodoc.write() into
 *<p> $MatlabParam.write().
 *<p> $
 *<p> $Revision 1.20  2007/04/13 18:42:29  sueh
 *<p> $bug# 964 Added debug member variable.
 *<p> $
 *<p> $Revision 1.19  2007/04/11 21:59:21  sueh
 *<p> $bug# 964 Added removeNameValuePair(String name):  removes a name/value pair with
 *  a simple (one-attribute) name.  Added removeStatement(WritableStatement):  removes
 *  the passed-in Statement.
 *<p> $
 *<p> $Revision 1.18  2007/04/09 20:18:48  sueh
 *<p> $bug# 964 Moved the value to the associated name/value pair.  Changed
 *<p> $the Vector member variable from values to nameValuePairList.  Associated the
 *<p> $last attribute in each name/value pair with the name value pair.  This is the
 *<p> $attribute which used to contain the value.  The name/value pair also contained
 *<p> $the value; so it was duplicated.  This made it difficult to add a value to an
 *<p> $existing attribute.  GetValue() gets the value from the associated name/value
 *<p> $pair.  Also removed the old nameValuePairList member variable, because it
 *<p> $wasn't being used for anything.
 *<p> $
 *<p> $Revision 1.17  2007/03/26 18:36:27  sueh
 *<p> $bug# 964 Made Version optional so that it is not necessary in matlab param files.
 *<p> $
 *<p> $Revision 1.16  2007/03/23 20:30:09  sueh
 *<p> $bug# 964 Added addAttributeAndNameValuePair, addComment(String), write(),
 *<p> $getWritableAttribute,getAttributeMultiLineValues.
 *<p> $
 *<p> $Revision 1.15  2007/03/21 18:14:26  sueh
 *<p> $bug# 964 Limiting access to autodoc classes by using ReadOnly interfaces.
 *<p> $Added AutodocFactory to create Autodoc instances.
 *<p> $
 *<p> $Revision 1.14  2007/03/15 21:44:50  sueh
 *<p> $bug# 964 Added ReadOnlyAttribute, which is used as an interface for Attribute,
 *<p> $unless the Attribute needs to be modified.
 *<p> $
 *<p> $Revision 1.13  2007/03/08 21:53:12  sueh
 *<p> $bug# 964 Save name/value pairs in the parser instead of saving them from the
 *<p> $Attribute.  This is necessary because the name/value pair must be placed in the
 *<p> $autodoc or section as soon as they are found to preserve the original order of the
 *<p> $autodoc file.
 *<p> $
 *<p> $Revision 1.12  2007/03/07 21:05:24  sueh
 *<p> $bug# 964 Fixed printing.  Made internal tests runnable from unit tests.
 *<p> $
 *<p> $Revision 1.11  2007/03/05 21:28:28  sueh
 *<p> $bug# 964 Stop controlling autodoc instances, except for the standard ones.
 *<p> $
 *<p> $Revision 1.10  2007/03/01 01:16:12  sueh
 *<p> $bug# 964 Added mutable boolean.  Added getMatlabInstance.
 *<p> $
 *<p> $Revision 1.9  2007/02/09 00:42:37  sueh
 *<p> $bug# 962 Added xfjointomo autodoc.
 *<p> $
 *<p> $Revision 1.8  2006/11/16 23:38:16  sueh
 *<p> $bug# 872 Changed setDir_test to setTestDir.  Changed getTestAutodocDir to
 *<p> $getTestDir.
 *<p> $
 *<p> $Revision 1.7  2006/09/13 23:30:50  sueh
 *<p> $bug# 921 Adding corrsearch3d.adoc.
 *<p> $
 *<p> $Revision 1.6  2006/07/21 22:11:32  sueh
 *<p> $bug# 901 Getting the calibration directory environment variable name from
 *<p> $EnvironmentVariable.
 *<p> $
 *<p> $Revision 1.5  2006/06/14 21:19:54  sueh
 *<p> $bug# 852 Added isAttribute().
 *<p> $
 *<p> $Revision 1.4  2006/06/14 00:15:20  sueh
 *<p> $bug# 852 Added densmatch because it is small and good for basic debugging of
 *<p> $the parcer.  Added getInstance(String fileName...) so that it is easy to open a
 *<p> $uitestaxis autodoc in the ui test source directory.
 *<p> $
 *<p> $Revision 1.3  2006/05/01 21:16:26  sueh
 *<p> $bug# 854
 *<p> $
 *<p> $Revision 1.2  2006/04/25 18:54:22  sueh
 *<p> $bug# 787 Implemented ReadOnlyNameValuePairList so that name/value
 *<p> $pairs can be read from either a global or section area using the same
 *<p> $code.
 *<p> $
 *<p> $Revision 1.1  2006/01/12 17:02:22  sueh
 *<p> $bug# 798 Moved the autodoc classes to etomo.storage.autodoc.
 *<p> $
 *<p> $Revision 1.25  2006/01/11 21:55:25  sueh
 *<p> $bug# 675 Removed attributeList.  Added attributeMap and
 *<p> $nameValuePairList.  Removed getAttributeLocation and nextAttribute.
 *<p> $Added addNameValuePair, getNameValuePairLocation, and
 *<p> $nextNameValuePair.
 *<p> $
 *<p> $Revision 1.24  2006/01/03 23:27:49  sueh
 *<p> $bug# 675 Converted metaData to an AttributeList.  Added UITEST_AXIS.
 *<p> $Added UITEST_AXIS_MAP, so that multiple UITEST_AXIS adocs can be
 *<p> $opened at once.  Added getAttributeLocation and nextAttribute so that
 *<p> $a list of attributes can be traversed.
 *<p> $
 *<p> $Revision 1.23  2005/12/23 02:13:36  sueh
 *<p> $bug# 675 Added UITEST type of autodoc.  Added the ability to pass the
 *<p> $autodoc file when opening an autodoc.  This is for test only and gives more
 *<p> $flexibility in autodoc names.  The name must still start with the standard
 *<p> $autodoc name and end in .adoc.
 *<p> $
 *<p> $Revision 1.22  2005/11/10 22:20:31  sueh
 *<p> $bug# 759 Added VERSION constant.
 *<p> $
 *<p> $Revision 1.21  2005/11/10 18:14:16  sueh
 *<p> $bug# 733 added setTestDir(), which sets the autodoc directory directly and
 *<p> $can only be used in test mode.  Rewrote getTestAutodocDir() to let it use
 *<p> $the test directory and to break it up into a group of less complicated
 *<p> $functions.
 *<p> $
 *<p> $Revision 1.20  2005/10/12 22:43:12  sueh
 *<p> $bug# 532 Added isSectionExists(String type) to find out if a type of
 *<p> $section is in the autodoc.
 *<p> $
 *<p> $Revision 1.19  2005/09/21 16:36:26  sueh
 *<p> $bug# 532 Removed initialization from constructor.  Calling initialization
 *<p> $from getAutodoc().  Removed getFile().  Added
 *<p> $setAutodocFile(String name, AxisID, String envVariable), which is called
 *<p> $by initialize().  It sets autodocFile (was file).  SetAutodocFile() reports all
 *<p> $problems it finds to the error log.
 *<p> $
 *<p> $Revision 1.18  2005/09/01 17:58:14  sueh
 *<p> $bug# 532  Change getAutodoc() to allow specification of the autodoc
 *<p> $location environment variable for each autodoc.  Get the cpu autodoc from
 *<p> $the calibration directory.
 *<p> $
 *<p> $Revision 1.17  2005/08/27 22:34:44  sueh
 *<p> $bug# 532 Added cpu.adoc.  Added getAttribute(String name) to get the
 *<p> $file level attributes.
 *<p> $
 *<p> $Revision 1.16  2005/07/29 00:53:55  sueh
 *<p> $bug# 709 Going to EtomoDirector to get the current manager is unreliable
 *<p> $because the current manager changes when the user changes the tab.
 *<p> $Passing the manager where its needed.
 *<p> $
 *<p> $Revision 1.15  2005/05/17 19:35:44  sueh
 *<p> $bug# 658 Added getSection(SectionLocation) to return a section based on
 *<p> $SectionLocation.  Added getAttributeValues() to get a HashMap of
 *<p> $attribute values based on section type and attribute name.  This is being
 *<p> $used to get the values of all attributes called "required" in field sections.
 *<p> $
 *<p> $Revision 1.14  2005/05/12 01:30:37  sueh
 *<p> $bug# 658 Added beadtrack.
 *<p> $
 *<p> $Revision 1.13  2005/04/25 20:53:33  sueh
 *<p> $bug# 615 Passing the axis where a command originates to the message
 *<p> $functions so that the message will be popped up in the correct window.
 *<p> $This requires adding AxisID to many objects.
 *<p> $
 *<p> $Revision 1.12  2005/02/23 01:42:10  sueh
 *<p> $bug# 600 Adding solvematch to autodoc.
 *<p> $
 * <p> $Revision 1.11  2005/02/22 20:57:22  sueh
 * <p> $bug# 600 Adding ccderaser to autodoc.
 * <p> $
 * <p> $Revision 1.10  2005/02/11 16:44:50  sueh
 * <p> $bug# 600 Adding tiltalign.
 * <p> $
 * <p> $Revision 1.9  2004/11/30 18:28:54  sueh
 * <p> $bug# 556 Added combinefft autodoc.
 * <p> $
 * <p> $Revision 1.8  2004/03/30 17:42:16  sueh
 * <p> $bug# 409 adding mtffilter.adoc
 * <p> $
 * <p> $Revision 1.7  2004/01/02 18:04:53  sueh
 * <p> $bug# 372 adding doc
 * <p> $
 * <p> $Revision 1.6  2004/01/01 00:44:50  sueh
 * <p> $bug# 372 correcting interface name
 * <p> $
 * <p> $Revision 1.5  2003/12/31 17:46:43  sueh
 * <p> $bug# 372 add getFile()
 * <p> $
 * <p> $Revision 1.4  2003/12/31 02:01:36  sueh
 * <p> $bug# 372 fixed environment variable names
 * <p> $
 * <p> $Revision 1.3  2003/12/31 01:24:44  sueh
 * <p> $bug# 372 added autodoc data storage and retrieval
 * <p> $
 * <p> $Revision 1.2  2003/12/23 21:31:04  sueh
 * <p> $bug# 372 reformat.  Pass this pointer to AutodocParser, so
 * <p> $autodoc info can be stored in Autodoc.
 * <p> $
 * <p> $Revision 1.1  2003/12/22 23:47:45  sueh
 * <p> $bug# 372 Autodoc contains informatio from the autodoc file.
 * <p> $It instantiates at most one per type of autodoc file.
 * <p> $$ </p>
 */
