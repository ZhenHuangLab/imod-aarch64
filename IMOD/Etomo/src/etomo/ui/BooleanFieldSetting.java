package etomo.ui;

/**
 * <p>Description: Three state boolean setting (not set, on, and off).  NOT thread-safe.
 * The set member variable is turned on when set() is called.  Handles string values by
 * translating them into a boolean, and also storing the original string in a
 * TextFieldSetting instance.  Currently doesn't handle string types other then string and
 * integer.</p>
 * <p/>
 * <p>Copyright: Copyright 2014 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class BooleanFieldSetting implements FieldSettingInterface {
  private boolean set = false;
  private boolean value = false;
  private TextFieldSetting textSetting = null;

  public BooleanFieldSetting() {}

  public String toString() {
    return "[set:" + set + ",value:" + value + "]";
  }

  public boolean isBoolean() {
    return true;
  }

  public boolean isText() {
    return textSetting != null && textSetting.isSet();
  }

  public BooleanFieldSetting getBooleanSetting() {
    return this;
  }

  public TextFieldSetting getTextSetting() {
    if (textSetting != null && textSetting.isSet()) {
      return textSetting;
    }
    return null;
  }

  public boolean equals(final boolean input) {
    return set && value == input;
  }

  /**
   * Compares input with textSetting member variable if set.  Otherwise does a
   * comparison with value.  If set is false, always returns false.
   *
   * @param input
   * @return
   */
  public boolean equals(final String input) {
    if (!set) {
      return false;
    }
    if (textSetting != null && textSetting.isSet()) {
      return textSetting.equals(input);
    }
    return value == stringToBoolean(input);
  }

  /**
   * Changes set to true and sets value member variable.  TextSetting is reset.
   *
   * @param setting
   */
  public void set(final boolean setting) {
    set = true;
    value = setting;
    if (textSetting != null) {
      textSetting.reset();
    }
  }

  /**
   * Changes set to true and sets value member variable.  TextSetting is set to preserve
   * string for comparison.
   *
   * @param setting
   */
  public void set(String setting) {
    set = true;
    value = stringToBoolean(setting);
    if (textSetting == null) {
      textSetting = new TextFieldSetting();
    }
    textSetting.set(setting);
  }
  /**
   * For when the setting string does not translate to the right boolean value.
   * Changes set to true and sets value member variable to the boolean setting.
   * TextSetting is set to settingString, in order to preserve it for comparison.
   *
   * @param setting
   * @param settingString
   */
  public void set(final boolean setting, final String settingString) {
    set = true;
    value = setting;
    if (textSetting == null) {
      textSetting = new TextFieldSetting();
    }
    textSetting.set(settingString);
  }

  /**
   * Changes set to false.  Resets value and textSetting
   */
  public void reset() {
    set = false;
    value = false;
    if (textSetting != null) {
      textSetting.reset();
    }
  }

  /**
   * Resets and then copies set, value, and textFieldSetting.
   *
   * @param input
   */
  public void copy(final FieldSettingInterface input) {
    reset();
    BooleanFieldSetting setting = null;
    if (input != null) {
      setting = input.getBooleanSetting();
    }
    if (setting != null) {
      set = setting.set;
      value = setting.value;
      if (setting.textSetting != null && setting.textSetting.isSet()) {
        if (textSetting == null) {
          textSetting = new TextFieldSetting();
        }
        textSetting.copy(setting.textSetting);
      }
    }
  }

  public boolean isSet() {
    return set;
  }

  public boolean isValue() {
    return value;
  }

  /**
   * <p>
   * Translates a string to a boolean as best it can:
   * </p>
   * <ul>
   * <li>Null:  false (an empty non-boolean directive is being overridden?)</li>
   * <li>Empty:  true (an empty attribute or parameter is true (may have to intentionally pass an empty string to get this)</li>
   * <li>Zero:  false</li>
   * <li>False string:  false (f, false, n, no, na, off)</li>
   * <li>Any other string:  true (the parameter, directive, or attribute exists and has a value)</li>
   * </ul>
   *
   * @param string string to be converted into a boolean
   * @return boolean representation of string parameter
   */
  public static boolean stringToBoolean(String string) {
    if (string == null) {
      return false;
    }
    string = string.trim();
    if (string.equals("")) {
      return true;
    }
    try {
      if (Integer.parseInt(string) == 0) {
        return false;
      }
    }
    catch (NumberFormatException e) {}
    if (string.compareToIgnoreCase("f") == 0 || string.compareToIgnoreCase("false") == 0
      || string.compareToIgnoreCase("n") == 0 || string.compareToIgnoreCase("na") == 0
      || string.compareToIgnoreCase("no") == 0 || string.compareToIgnoreCase("off") == 0) {
      return false;
    }
    return true;
  }
}
