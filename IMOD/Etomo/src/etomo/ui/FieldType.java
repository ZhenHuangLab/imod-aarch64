package etomo.ui;

import etomo.storage.DirectiveValueType;
import etomo.type.EtomoNumber;

/**
* <p>Description: An enumerator class which decribes text field types.  Used for
* validation.  Types correspond to the types in PIP.</p>
* 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
*/
public final class FieldType {
  public static final FieldType STRING = new FieldType(ValidationType.STRING, 9);
  public static final FieldType FILE = new FieldType(ValidationType.STRING, 15);
  public static final FieldType INTEGER = new FieldType(ValidationType.INTEGER, 3);
  public static final FieldType FLOATING_POINT =
    new FieldType(ValidationType.FLOATING_POINT, 3);
  public static final FieldType INTEGER_PAIR =
    new FieldType(ValidationType.INTEGER, CollectionType.ARRAY, 2, 6);
  public static final FieldType FLOATING_POINT_PAIR =
    new FieldType(ValidationType.FLOATING_POINT, CollectionType.ARRAY, 2, 6);
  public static final FieldType INTEGER_TRIPLE =
    new FieldType(ValidationType.INTEGER, CollectionType.ARRAY, 3, 9);
  public static final FieldType FLOATING_POINT_ARRAY =
    new FieldType(ValidationType.FLOATING_POINT, CollectionType.ARRAY, 9);
  public static final FieldType INTEGER_ARRAY =
    new FieldType(ValidationType.INTEGER, CollectionType.ARRAY, 9);
  public static final FieldType INTEGER_LIST =
    new FieldType(ValidationType.INTEGER, CollectionType.LIST, 9);

  public final ValidationType validationType;
  private final CollectionType collectionType;
  /**
   * For arrays with a fixed number of elements (pairs and triples).
   */
  public final int requiredSize;
  private final int columns;

  /**
   * 
   * @param validationType required
   * @param columns
   */
  private FieldType(final ValidationType validationType, final int columns) {
    this.validationType = validationType;
    collectionType = null;
    requiredSize = -1;
    this.columns = columns;
  }

  /**
   * 
   * @param validationType required
   * @param collectionType
   * @param columns
   */
  private FieldType(final ValidationType validationType,
    final CollectionType collectionType, final int columns) {
    this.validationType = validationType;
    this.collectionType = collectionType;
    this.requiredSize = -1;
    this.columns = columns;
  }

  /**
   * 
   * @param validationType required
   * @param collectionType
   * @param requiredSize
   * @param columns
   */
  private FieldType(final ValidationType validationType,
    final CollectionType collectionType, final int requiredSize, final int columns) {
    this.validationType = validationType;
    this.collectionType = collectionType;
    this.requiredSize = requiredSize;
    this.columns = columns;
  }

  public static FieldType getInstance(final DirectiveValueType valueType) {
    if (valueType == DirectiveValueType.BOOLEAN) {
      return null;
    }
    if (valueType == DirectiveValueType.FLOATING_POINT) {
      return FLOATING_POINT;
    }
    if (valueType == DirectiveValueType.FLOATING_POINT_PAIR) {
      return FLOATING_POINT_PAIR;
    }
    if (valueType == DirectiveValueType.INTEGER) {
      return INTEGER;
    }
    if (valueType == DirectiveValueType.INTEGER_PAIR) {
      return INTEGER_PAIR;
    }
    if (valueType == DirectiveValueType.LIST) {
      return INTEGER_LIST;
    }
    if (valueType == DirectiveValueType.STRING) {
      return STRING;
    }
    if (valueType == DirectiveValueType.UNKNOWN) {
      return STRING;
    }
    if (valueType == DirectiveValueType.FILE) {
      return FILE;
    }
    return null;
  }

  /**
   * Return the largest possible equivalent numeric type.  Returns null for strings,
   * arrays, and lists.
   */
  public EtomoNumber.Type getNumericType() {
    return validationType.numericType;
  }

  public String toString() {
    return "[validationType:" + validationType + ",collectionType:" + collectionType
      + ",requiredSize:" + requiredSize;
  }

  public boolean hasRequiredSize() {
    return requiredSize != -1;
  }

  public boolean isCollection() {
    return collectionType != null;
  }

  public int getColumns() {
    return columns;
  }

  public String getSplitter() {
    return collectionType.splitter;
  }

  /**
   * Contains the types of data and whether they can be validated.
   */
  public static final class ValidationType {
    public static final ValidationType STRING =
      new ValidationType(false, null, "a string");
    public static final ValidationType INTEGER =
      new ValidationType(true, EtomoNumber.Type.INTEGER, "an integer");
    public static final ValidationType FLOATING_POINT =
      new ValidationType(true, EtomoNumber.Type.DOUBLE, "a floating point number");

    public final boolean numeric;
    private final EtomoNumber.Type numericType;
    private final String descr;

    private ValidationType(final boolean numeric, final EtomoNumber.Type numericType,
      final String descr) {
      this.numeric = numeric;
      this.numericType = numericType;
      this.descr = descr;
    }

    public String toString() {
      return descr;
    }
  }

  /**
   * Contains the types of collections and a splitter that can be used to divide them
   * into numeric elements.
   */
  public static final class CollectionType {
    /**
     * Array separators are commas and whitespace.
     */
    public static final CollectionType ARRAY = new CollectionType("\\s*,\\s*|\\s+");
    /**
     * List separators are commas, whitespace, and dashes.
     */
    public static final CollectionType LIST =
      new CollectionType("\\s*,\\s*|\\s+|\\s*\\-\\s*");

    private final String splitter;

    private CollectionType(final String splitter) {
      this.splitter = splitter;
    }

    public String getSplitter() {
      return splitter;
    }
  }
}
