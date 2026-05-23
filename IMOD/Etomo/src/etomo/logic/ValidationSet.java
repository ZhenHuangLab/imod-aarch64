package etomo.logic;

import etomo.type.EtomoNumber;
import etomo.ui.FieldType;

/**
 * <p>Description: Set of validation data.  Most fields require no validation data other
 * then field type.  So it is a waste of space for all fields to have empty validation data.
 * This way they can just have a pointer to an empty validation set if they don't need
 * validation data.  Because the field type is used for basic validation, it is better for
 * the field to contain the it, rather then this class.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class ValidationSet {
  private final EtomoNumber.Type numericType;// defaults to integer

  private EtomoNumber zero = null;// used only with setNumberMustBePositive
  private EtomoNumber minimum = null;
  private EtomoNumber maximum = null;

  public ValidationSet(final FieldType fieldType) {
    if (fieldType != null) {
      numericType = fieldType.getNumericType();
    }
    else {
      numericType = null;
    }
  }

  public ValidationSet(final EtomoNumber.Type numericType) {
    this.numericType = numericType;
  }

  public void clear() {
    if (zero != null) {
      zero.reset();
    }
    if (minimum != null) {
      minimum.reset();
    }
    if (maximum != null) {
      maximum.reset();
    }
  }

  /**
   * This is not a deep copy.
   * @param input
   */
  public void copy(final ValidationSet input) {
    if (input == null) {
      clear();
    }
    else {
      if (zero == null) {
        zero = input.zero;
      }
      else {
        zero.set(input.zero);
      }
      if (minimum == null) {
        minimum = input.minimum;
      }
      else {
        minimum.set(input.minimum);
      }
      if (maximum == null) {
        maximum = input.maximum;
      }
      else {
        maximum.set(input.maximum);
      }
    }
  }

  /**
   * Validates text based on whether text is a number that corresponds to numericType.
   * Returns error message, or null if text is valid or doesn't exist.
   * @param text
   * @param type - defaults to integer
   * @return errror message
   */
  public static String validate(String text, final EtomoNumber.Type type) {
    if (text == null || text.matches("\\s*")) {
      return null;
    }
    return _validate(text, type);
  }

  /**
   * Validates text based on member variable settings and whether text is a number that
   * corresponds to numericType.  Returns error message, or null if text is valid or
   * doesn't exist.
   * @param text
   * @return error message
   */
  public String validate(String text) {
    if (text == null || text.matches("\\s*")) {
      return null;
    }
    String errmsg = _validate(text, numericType);
    if (errmsg != null) {
      return errmsg;
    }
    if (zero != null && !zero.isNull() && zero.gt(text)) {
      return "must be a positive number";
    }
    else if (minimum != null && !minimum.isNull() && minimum.gt(text)) {
      return "must be greater or or equals to " + minimum.toString();
    }
    else if (maximum != null && !maximum.isNull() && maximum.lt(text)) {
      return "must be less then or equal to " + maximum.toString();
    }
    return null;
  }

  /**
   * Private static validate function.  Validates whether text is a number of the
   * specified type.  Assumes text is not null or empty.
   * @param text required, non-empty, contains non-whitespace
   * @param type
   * @return error message
   */
  private static String _validate(String text, final EtomoNumber.Type type) {
    text = text.trim();
    try {
      if (type == EtomoNumber.Type.LONG) {
        Long.parseLong(text);
      }
      else if (type == EtomoNumber.Type.DOUBLE) {
        Double.parseDouble(text);
      }
      else {
        Integer.parseInt(text);
      }
    }
    catch (NumberFormatException e) {
      return "must be of type " + (type != null ? type : "integer");
    }
    return null;
  }

  public void setNumberMustBePositive(final boolean input) {
    if (input) {
      zero = new EtomoNumber();
      zero.set(0);
    }
    else {
      zero.reset();
    }
  }

  public void setMinimum(final double input) {
    if (minimum == null) {
      minimum = new EtomoNumber(EtomoNumber.Type.DOUBLE);
    }
    minimum.set(input);
  }

  public void setMaximum(final int input) {
    if (maximum == null) {
      maximum = new EtomoNumber();
    }
    maximum.set(input);
  }
}