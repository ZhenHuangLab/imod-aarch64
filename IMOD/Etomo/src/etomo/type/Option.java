package etomo.type;

/**
 * <p>Description: Value/description pair.  Equals functions check value and then
 * description.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class Option {
  private static final char DELIMITER = ':';

  private final String value;
  private final String descr;

  private boolean includeValue = false;

  public Option(final String[] array) {
    if (array == null || array.length == 0) {
      value = null;
      descr = null;
    }
    else if (array.length == 1) {
      value = array[0];
      descr = array[0];
    }
    else {
      value = array[0];
      descr = array[1];
    }
  }

  public Option(final String value, final String descr) {
    this.value = value;
    this.descr = descr;
  }

  public Option(final Option option) {
    value = option.value;
    descr = option.descr;
  }

  public void setIncludeValue(final boolean includeValue) {
    this.includeValue = includeValue;
  }

  public boolean equals(String string) {
    if (string != null) {
      string = string.trim();
    }
    if (equals((Object) string)) {
      return true;
    }
    // Check value first
    if (string.equalsIgnoreCase(value) || string.equalsIgnoreCase(descr)) {
      return true;
    }
    return false;
  }

  public boolean equals(final Object object) {
    if (object == null && value == null && descr == null) {
      return true;
    }
    if (object == null) {
      return false;
    }
    String displayString = getDisplayString();
    if (displayString != null && displayString.equals(object)) {
      return true;
    }
    // Check value first
    if (object.equals(value) || object.equals(descr)) {
      return true;
    }
    return false;
  }

  public String toString() {
    return getDisplayString();
  }

  public String getDisplayString() {
    if (descr == null) {
      return value;
    }
    if (value == null || !includeValue) {
      return descr;
    }
    // IncludeValue is true and value and descr are present
    return value + DELIMITER + " " + descr;
  }

  public String getValue() {
    return value;
  }
}