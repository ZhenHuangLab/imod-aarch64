package etomo.ui;

import etomo.type.ConstEtomoNumber;
import etomo.type.EtomoNumber;

/**
 * <p>Description: Text setting that can be turned on and off.  Not thread safe.  A null
 * value can be a valid setting.  The set member variable is turned on when set() is
 * called.</p>
 * <p/>
 * <p>Copyright: Copyright 2014 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class TextFieldSetting implements FieldSettingInterface {
  private boolean set = false;
  private String value = null;

  private final EtomoNumber.Type type;

   TextFieldSetting() {
    type = null;
  }

  public TextFieldSetting(final FieldType fieldType) {
    type = fieldType.getNumericType();
  }

  public TextFieldSetting(final EtomoNumber.Type type) {
    this.type = type;
  }

  public boolean isBoolean() {
    return false;
  }

  public boolean isText() {
    return true;
  }

  public TextFieldSetting getTextSetting() {
    return this;
  }

  public BooleanFieldSetting getBooleanSetting() {
    return null;
  }

  public boolean equals(String input) {
    if (!set) {
      return false;
    }
    if (value == null && input == null) {
      // Treating two nulls as equal
      return true;
    }
    if (value == null || input == null) {
      // One is null - not equal
      return false;
    }
    // Ignore whitespace
    input = input.trim();
    if (value.equals(input)) {
      // Strings are identical - equal
      return true;
    }
    // Compare as a number if both are numbers
    ConstEtomoNumber nValue = createNumber(value);
    if (nValue != null) {
      ConstEtomoNumber nInput = createNumber(input);
      if (nInput == null) {
        // One is numeric and the other is not
        return false;
      }
      // Treating two nulls as equal (value and input may not have the same type)
      if (nValue.isNull() && nInput.isNull()) {
        return true;
      }
      // One is null - not equal
      if (nValue.isNull() || nInput.isNull()) {
        return false;
      }
      // Do a numeric comparison
      return nValue.equals(nInput);
    }
    // Not numeric and not the same string - equals
    return false;
  }

  public boolean equals(final Number input) {
    if (!set) {
      return false;
    }
    // Treating two nulls as equal
    if (value == null && input == null) {
      return true;
    }
    // One is null - not equal
    if (value == null || input == null) {
      return false;
    }
    // Compare as a number if value is a number
    ConstEtomoNumber nValue = createNumber(value);
    if (nValue != null) {
      // Treating two nulls as equal (value and input may not have the same type)
      if (nValue.isNull() && EtomoNumber.isNumberNull(input)) {
        return true;
      }
      // One is null - not equal
      if (nValue.isNull() || EtomoNumber.isNumberNull(input)) {
        return false;
      }
      // Do a numeric comparison
      return nValue.equals(input);
    }
    // Value is not a number, and input is - not equal
    return false;
  }

  /**
   * Returns a valid etomoNumber or null.  The type with be from the parameter, unless the
   * string has a decimal point and type is long or integer.  In that case the type will
   * be double.
   *
   * @param string
   * @return
   */
  private ConstEtomoNumber createNumber(final String string) {
    EtomoNumber number = new EtomoNumber(type);
    number.set(string);
    if (number.isValid()) {
      return number;
    }
    if (type != EtomoNumber.Type.DOUBLE) {
      number = new EtomoNumber(EtomoNumber.Type.DOUBLE);
      number.set(string);
      if (number.isValid()) {
        return number;
      }
    }
    return null;
  }

  /**
   * Sets checkoint (trims whitespace).
   *
   * @param input
   */
  public void set(final String input) {
    set = true;
    value = input;
    // Ignore whitespace
    if (value != null) {
      value = value.trim();
    }
  }

  public void set(final int input) {
    set(String.valueOf(input));
  }

  public void set(final double input) {
    set(String.valueOf(input));
  }

  public void set(final ConstEtomoNumber input) {
    if (input == null) {
      set = true;
      value = null;
    }
    else {
      set(input.toString());
    }
  }

  public void set(final Number input) {
    if (input == null) {
      set = true;
      value = null;
    }
    else {
      set(input.toString());
    }
  }

  public void reset() {
    set = false;
    value = null;
  }

  /**
   * Resets and then copies member variables
   * @param input
   */
  public void copy(final FieldSettingInterface input) {
    reset();
    TextFieldSetting setting = null;
    if (input != null) {
      setting = input.getTextSetting();
    }
    if (setting != null) {
      set = setting.set;
      value = setting.value;
    }
  }

  public boolean isSet() {
    return set;
  }

  public String getValue() {
    return value;
  }
}

