package etomo.storage.autodoc;

import java.io.IOException;

import etomo.storage.LogFile;
import etomo.ui.swing.Token;

/**
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright 2006 - 2014 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.5  2010/02/17 04:49:43  sueh
 * <p> bug# 1301 Using the manager instead of the manager key do pop up
 * <p> messages.
 * <p>
 * <p> Revision 1.4  2009/02/04 23:30:00  sueh
 * <p> bug# 1158 Changed id and exceptions classes in LogFile.
 * <p>
 * <p> Revision 1.3  2007/04/11 22:10:08  sueh
 * <p> bug# 964 ADded removeNameValuePair, removeStatement, setDebug, and
 * <p> printStatementList.
 * <p>
 * <p> Revision 1.2  2007/04/09 20:56:40  sueh
 * <p> bug# 964 Changed addAttributeAndNameValuePair to addNameValuePair, since
 * <p> a name/value pair can't exist without Attributes; so the creation of the Attribute is
 * <p> implied.
 * <p>
 * <p> Revision 1.1  2007/03/23 20:37:14  sueh
 * <p> bug# 964 An interface which can be used to modify an autodoc.
 * <p> </p>
 */
public interface WritableAutodoc extends ReadOnlyAutodoc {
  public void addNameValuePairAttribute(String name, String value);

  public void addNameValuePairAttribute(String name, String value, int lineNum);

  public WritableAttribute getWritableAttribute(String name);

  public void write() throws LogFile.LockException, IOException;

  public void addComment(Token comment, int lineNum);

  public void addEmptyLine(int lineNum);

  public void addComment(String comment, int lineNum);

  public WritableStatement removeNameValuePair(String name);

  public WritableStatement removeStatement(WritableStatement statement);

  public void setDebug(boolean debug);

  public void printStatementList();

  public void wrapAttributeValues(String noWrapPrefix, String wrapPrefix, String divider,
    String defaultDivider, int minLength, int wrapLength);
}
