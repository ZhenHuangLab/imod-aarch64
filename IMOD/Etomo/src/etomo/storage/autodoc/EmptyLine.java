package etomo.storage.autodoc;

import java.io.IOException;

import etomo.storage.LogFile;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2006 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.4  2009/02/04 23:30:00  sueh
 * <p> bug# 1158 Changed id and exceptions classes in LogFile.
 * <p>
 * <p> Revision 1.3  2009/01/20 19:34:54  sueh
 * <p> bug# 1102 Added getSubsection.
 * <p>
 * <p> Revision 1.2  2007/04/11 22:02:10  sueh
 * <p> bug# 964 Added a link list to Statement so that groups of statements could be
 * <p> removed.  Added the parameter Statement previousStatement to the Statement
 * <p> constructor.
 * <p>
 * <p> Revision 1.1  2007/04/09 20:32:28  sueh
 * <p> bug# 964 Changed NameValuePair to an abstract class called Statement and
 * <p> child classes representing name/value pair, comment, empty line, and
 * <p> subsection.  Made delimiter change an attribute of the name/value pair class.
 * <p> Added ReadOnlyStatement to provide a public interface for Statement classes.
 * <p> </p>
 */
final class EmptyLine extends Statement {
  private static final Type TYPE = Statement.Type.EMPTY_LINE;

  private final WriteOnlyStatementList parent;

  EmptyLine(WriteOnlyStatementList parent, Statement previousStatement, final int lineNum) {
    super(previousStatement, lineNum);
    this.parent = parent;
  }

  void wrapValue(final String noWrapPrefix, final String wrapPrefix,
    final String divider, final String defaultDivider, final int minLength,
    final int wrapLength) {}

  public Statement.Type getType() {
    return TYPE;
  }

  public String getString() {
    return "";
  }

  public int sizeLeftSide() {
    return 0;
  }

  public String getLeftSide() {
    return null;
  }

  public String getLeftSide(int index) {
    return null;
  }

  public String getRightSide() {
    return "";
  }

  public ReadOnlySection getSubsection() {
    return null;
  }

  void write(LogFile file, LogFile.WriterId writerId) throws LogFile.LockException,
    IOException {
    file.newLine(writerId);
  }

  void print(int level) {
    Autodoc.printIndent(level);
    System.out.println("<empty-line>");
  }
}
