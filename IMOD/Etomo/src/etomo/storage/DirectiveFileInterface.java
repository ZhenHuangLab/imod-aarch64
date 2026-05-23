package etomo.storage;

import java.util.Iterator;

import etomo.storage.autodoc.ReadOnlyStatement;
import etomo.type.AxisID;

/**
 * <p>Description: Interface for DirectiveFile and DirectiveFileCollection.</p>
 * <p/>
 * <p>Copyright: Copyright 2014 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface DirectiveFileInterface {
  public boolean contains(DirectiveDef directiveDef);

  public String getValue(DirectiveDef directiveDef);

  public String getValue(DirectiveDef directiveDef, int index);

  public String getValue(DirectiveDef directiveDef, boolean templateOnly);

  public DirectiveValue getValue(DirectiveDef directiveDef, boolean templateOnly,
    boolean ignoreFileType);

  public DirectiveValue getValue(DirectiveDef directiveDef, boolean templateOnly,
    boolean ignoreFileType, boolean includeOverride);

  public boolean contains(DirectiveDef directiveDef, boolean templateOnly);

  public boolean contains(DirectiveDef directiveDef, boolean templateOnly,
    boolean ignoreFileType);

  public boolean contains(DirectiveDef directiveDef, boolean templateOnly,
    boolean ignoreFileType, boolean includeOverride);

  public boolean isValue(DirectiveDef directiveDef, boolean templateOnly,
    boolean ignoreFileType);

  public boolean isValue(DirectiveDef directiveDef);

  public void setDebug(boolean input);

  public boolean contains(DirectiveDef directiveDef, AxisID axisID, boolean templateOnly);

  public boolean contains(DirectiveDef directiveDef, AxisID axisID, boolean templateOnly,
    boolean ignoreFileType);

  public boolean isValue(DirectiveDef directiveDef, AxisID axisID, boolean templateOnly);

  public DirectiveValue getValue(DirectiveDef directiveDef, AxisID axisID,
    boolean templateOnly, boolean ignoreFileType);

  public Iterator<ReadOnlyStatement> iterator(boolean templateOnly);

}
