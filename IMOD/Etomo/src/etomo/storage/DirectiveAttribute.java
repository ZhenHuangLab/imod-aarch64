package etomo.storage;

import java.util.Hashtable;

import etomo.storage.autodoc.ReadOnlyAttribute;
import etomo.type.AxisID;
import etomo.type.DirectiveFileType;

/**
 * <p>Description: Returns a primary directive attribute match and also a secondary one.
 * Contains a table of instances that are likely to be accessed again.  Only the TABLE is
 * thread safe.</p>
 * <p/>
 * <p>Copyright: Copyright 2014</p>
 * <p/>
 * <p>Organization:
 * Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
 * University of Colorado</p>
 *
 * @author $Author$
 * @version $Revision$
 *          <p/>
 *          <p> $Log$ </p>
 */
public final class DirectiveAttribute {
  public static final String rcsid = "$Id:$";

  /**
   * Table of directives that are likely to be accessed again.
   */
  private static final Hashtable<Key, AttributeMatch> TABLE =
    new Hashtable<Key, AttributeMatch>();

  public static final String TRUE_VALUE = "1";
  public static final String FALSE_VALUE = "0";
  static final DirectiveAttribute INSTANCE = new DirectiveAttribute();

  private DirectiveAttribute() {}

  /**
   * Returns the AttributeMatch that matches the match parameter.  The AttributeMatch is
   * either found in the TABLE or is constructed.
   *
   * @param match
   * @return
   */
  static AttributeMatch getMatch(final Match match, final DirectiveFile directiveFile,
    final ReadOnlyAttribute parentAttribute, final DirectiveDef directiveDef,
    final AxisID axisID) {
    Key key;
    if (match == Match.PRIMARY) {
      key = new Key(directiveFile, directiveDef, axisID, Match.PRIMARY);
    }
    else if (match == Match.SECONDARY && directiveDef.hasSecondaryMatch(axisID)) {
      key = new Key(directiveFile, directiveDef, axisID, Match.SECONDARY);
    }
    else {
      return null;
    }
    if (TABLE.containsKey(key)) {
      return TABLE.get(key);
    }
    AttributeMatch attributeMatch = new AttributeMatch(match, key, directiveDef);
    attributeMatch.loadAttribute(parentAttribute, axisID);
    return attributeMatch;
  }

  /**
   * Translates the value of the attribute into a boolean.  A null value
   * returns false.  A "0" value returns false.  A "1" returns true.  Everything else
   * returns true.
   *
   * @param value
   * @return
   */
  static boolean toBoolean(String value) {
    if (value == null) {
      return false;
    }
    value = value.trim();
    if (value.equals(FALSE_VALUE)) {
      return false;
    }
    return true;
  }

  static final class AttributeMatch implements DirectiveValue {
    private final Match match;
    private final Key key;
    private final DirectiveDef directiveDef;

    private boolean loaded = false;
    private ReadOnlyAttribute attribute = null;

    private AttributeMatch(final Match match, final Key key,
      final DirectiveDef directiveDef) {
      this.match = match;
      this.key = key;
      this.directiveDef = directiveDef;
    }

    public String toString() {
      return "[match:" + match + ",\nkey:" + key + ",\ndirectiveDef:" + directiveDef
        + ",loaded:" + loaded + ",attribute:" + attribute + ",value:" + getValue() + "]";
    }

    private void loadAttribute(final ReadOnlyAttribute parentAttribute,
      final AxisID axisID) {
      if (loaded) {
        return;
      }
      loaded = true;
      if (parentAttribute == null || directiveDef == null) {
        return;
      }
      loaded = true;
      DirectiveType type = directiveDef.getDirectiveType();
      String name = directiveDef.getName(match, axisID);
      // copyarg and setupset
      if (type == DirectiveType.COPY_ARG || type == DirectiveType.SETUP_SET) {
        attribute = parentAttribute.getAttribute(name);
      }
      // runtime
      else if (type == DirectiveType.RUN_TIME) {
        attribute = findAttribute(parentAttribute, directiveDef.getModule(),
          directiveDef.getAxis(match, axisID), name);
      }
      // comparam
      else if (type == DirectiveType.COM_PARAM) {
        attribute = findAttribute(parentAttribute, directiveDef.getComfile(match, axisID),
          directiveDef.getCommand(), name);
      }
      if (attribute != null) {
        // This instance may be reused - save it
        TABLE.put(key, this);
      }
      return;
    }

    /**
     * Get the three-level down descendant of an attribute.
     *
     * @param attribute
     * @param name1
     * @param name2
     * @param name3
     * @return
     */
    private ReadOnlyAttribute findAttribute(ReadOnlyAttribute attribute,
      final String name1, final String name2, final String name3) {
      if (attribute == null || name1 == null) {
        return null;
      }
      attribute = attribute.getAttribute(name1);
      if (attribute == null || name2 == null) {
        return null;
      }
      attribute = attribute.getAttribute(name2);
      if (attribute == null || name3 == null) {
        return null;
      }
      return attribute.getAttribute(name3);
    }

    /**
     * @return true if the directive is not boolean and the attribute value is null
     */
    public boolean isOverride() {
      if (directiveDef == null || directiveDef.isBoolean() || attribute == null) {
        return false;
      }
      String value = attribute.getValue();
      return value == null || value.matches("\\s*");
    }

    /**
     * @return true if no attribute has been loaded
     */
    boolean isEmpty() {
      return attribute == null;
    }

    /**
     * @return the value of the attribute
     */
    public String getValue() {
      if (attribute == null) {
        return null;
      }
      // The value has been retrieved, so no further use for this instance.
      TABLE.remove(key);
      return attribute.getValue();
    }

    public boolean isBatch() {
      DirectiveFileType fileType;
      if (key == null || key.directiveFile == null
        || (fileType = key.directiveFile.getDirectiveFileType()) == null) {
        return false;
      }
      return fileType.isBatch();
    }

    /**
     * @return the boolean value of the primary or secondary attribute
     */
    boolean isValue() {
      String value = getValue();
      if (directiveDef.isBoolean()) {
        if (value == null) {
          System.err.println(
            "Warning: " + directiveDef + " is boolean and its value should not be null");
        }
        else {
          value = value.trim();
          if (!value.equals(FALSE_VALUE) && !value.equals(TRUE_VALUE)) {
            System.err.println("Warning: " + directiveDef
              + " is boolean and its value is invalid: " + value);
          }
        }
      }
      else {
        System.err.println("Warning: " + directiveDef + " is not a boolean");
      }
      return toBoolean(value);
    }
  }

  private static final class Key {
    private final DirectiveFile directiveFile;
    private final DirectiveDef directiveDef;
    private final AxisID axisID;
    private final Match match;

    private Key(final DirectiveFile directiveFile, final DirectiveDef directiveDef,
      final AxisID axisID, final Match match) {
      this.directiveFile = directiveFile;
      this.directiveDef = directiveDef;
      this.axisID = axisID;
      this.match = match;
    }

    public int hashCode() {
      if (directiveDef == null) {
        return super.hashCode();
      }
      return toString().hashCode();
    }

    public String toString() {
      if (directiveDef == null) {
        return super.toString();
      }
      return directiveFile.toString() + directiveDef.toString()
        + (axisID != null ? "," + axisID.toString() : "") + match.toString();
    }
  }

  /**
   * <p>How well an attribute matches an axis.  Primary is the first match, secondary is
   * the second match.</p>
   *
   * @see DirectiveDef
   */
  static final class Match {
    static final Match PRIMARY = new Match("primary");
    static final Match SECONDARY = new Match("secondary");

    private final String tag;

    private Match(final String tag) {
      this.tag = tag;
    }

    public String toString() {
      return tag;
    }
  }
}
