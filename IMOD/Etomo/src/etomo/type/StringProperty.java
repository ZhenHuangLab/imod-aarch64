package etomo.type;

import java.util.Properties;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2006 - 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * @notthreadsafe
 * 
 * <p> $Log$
 * <p> Revision 1.6  2009/04/13 22:55:22  sueh
 * <p> bug# 1207 Fixed equals(String).
 * <p>
 * <p> Revision 1.5  2008/11/20 01:39:55  sueh
 * <p> bug# 1149 Simplified StringProperty - set string to null when it is empty.
 * <p>
 * <p> Revision 1.4  2008/01/14 22:03:11  sueh
 * <p> bug# 1050 Made the class public.
 * <p>
 * <p> Revision 1.3  2007/12/10 22:39:19  sueh
 * <p> bug# 1041 Added loadFromOtherKey to load value with a key that is different
 * <p> from the one saved in the instance.  Useful for backwards compatibility.
 * <p>
 * <p> Revision 1.2  2007/05/16 22:59:31  sueh
 * <p> bug# 964 Added set(StringProperty).
 * <p>
 * <p> Revision 1.1  2007/04/09 21:12:14  sueh
 * <p> bug# 964 A class to make storing, loading, and removing strings from Properties
 * <p> easier.
 * <p> </p>
 */
public final class StringProperty implements ConstStringProperty {
  private final String key;
  private final boolean returnNullWhenEmpty;

  private boolean debug = false;
  private String string = null;
  private String backwardCompatibleKey = null;
  private String displayValue = null;

  public StringProperty() {
    this.key = null;
    this.returnNullWhenEmpty = false;
  }

  public StringProperty(final String key) {
    this.key = key;
    this.returnNullWhenEmpty = false;
  }

  public StringProperty(final String key, final boolean returnNullWhenEmpty) {
    this.key = key;
    this.returnNullWhenEmpty = returnNullWhenEmpty;
  }

  public String getKey() {
    return key;
  }

  public void setDisplayValue(final String displayValue) {
    this.displayValue = displayValue;
  }

  public String toString() {
    if (isEmpty()) {
      if (displayValue != null) {
        return displayValue;
      }
      if (returnNullWhenEmpty) {
        return null;
      }
      return "";
    }
    return string;
  }

  /**
   * Sets string to input.  If string is null, empty, or contains only
   * whitespace, sets string to null.
   * @param input
   */
  public void set(final String input) {
    if (isEmpty(input)) {
      reset();
    }
    else {
      string = input;
    }
  }

  int length() {
    if (isEmpty()) {
      return 0;
    }
    return string.length();
  }

  public void set(final StringProperty input) {
    string = input.string;
  }

  public boolean isEmpty() {
    return string == null || string.equals("");
  }

  private boolean isEmpty(String string) {
    return string == null || string.matches("\\s*");
  }

  public boolean equals(final String string) {
    if (string == null || string.matches("\\*")) {
      return isEmpty(this.string);
    }
    if (isEmpty(this.string)) {
      return false;
    }
    return this.string.equals(string);
  }

  public boolean equals(final StringProperty stringProperty) {
    if (string == null) {
      return isEmpty(stringProperty.string);
    }
    return string.equals(stringProperty.string);
  }

  public void load(final Properties props) {
    load(props, "", key, null);
  }

  public void load(final Properties props, final String prepend) {
    load(props, prepend, key, null);
  }

  public void load(final Properties props, final String prepend,
    final String defaultString) {
    load(props, prepend, key, defaultString);
  }

  void loadFromOtherKey(Properties props, String prepend, String key) {
    load(props, prepend, key, null);
  }

  /**
   * Load string.  Retrieves with backwardCompatibleKey if default retrieve returns null.
   * Removes entry containing backwardCompatibleKey.
   * @param props
   * @param prepend
   * @param key
   * @param defaultString
   */
  private void load(final Properties props, final String prepend, final String key,
    final String defaultString) {
    if (props == null) {
      if (defaultString == null) {
        reset();
      }
      else {
        set(defaultString);
      }
    }
    else {
      String currentKey = createKey(prepend, key);
      string = props.getProperty(currentKey);
      // Use the backward compatible key if regular key did not work. Remove the backward
      // compatible key.
      if (backwardCompatibleKey != null) {
        currentKey = createKey(prepend, backwardCompatibleKey);
        if (props.containsKey(currentKey)) {
          if (string == null) {
            string = props.getProperty(currentKey);
          }
          props.remove(currentKey);
        }
      }
      // Apply the default string
      if (defaultString != null && string == null) {
        string = defaultString;
      }
    }
  }

  void setBackwardCompatibleKey(final String input) {
    backwardCompatibleKey = input;
  }

  public void setDebug(final boolean input) {
    debug = input;
  }

  public void store(Properties props) {
    store(props, "");
  }

  public void store(Properties props, String prepend) {
    if (props == null) {
      return;
    }
    String currentKey = createKey(prepend);
    if (isEmpty()) {
      props.remove(currentKey);
    }
    else {
      props.setProperty(currentKey, string);
    }
  }

  public void remove(Properties props, String prepend) {
    if (props == null) {
      return;
    }
    String currentKey = createKey(prepend);
    props.remove(currentKey);
  }

  public void reset() {
    string = null;
  }

  private String createKey(String prepend) {
    return createKey(prepend, key);
  }

  private String createKey(String prepend, String key) {
    if (isEmpty(prepend)) {
      return key;
    }
    return prepend + "." + key;
  }
}
