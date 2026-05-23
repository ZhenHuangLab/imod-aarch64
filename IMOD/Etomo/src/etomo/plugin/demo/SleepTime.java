package etomo.plugin.demo;

import etomo.type.ConstEtomoNumber;
import etomo.type.EnumeratedType;
import etomo.type.EtomoNumber;

/**
 * <p>Description: Demonstates the use of EnumeratedType and radio buttons.
 * EnumeratedTypes are actually used for sets of values or string.</p>
 *
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 *
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 */
final class SleepTime implements EnumeratedType {
  private static final SleepTime ONE = new SleepTime(1, "1 sec");
  static final SleepTime TWO = new SleepTime(2, "2 sec");
  static final SleepTime THREE = new SleepTime(3, "3 sec");
  static final SleepTime USER_ENTRY = new SleepTime("Sleep time:");

  static final SleepTime DEFAULT = ONE;

  private final EtomoNumber value;
  // Not necessary to put the label here - can be placed directly in the radio button.
  private final String label;

  private SleepTime(final String label) {
    value = null;
    this.label = label;
  }

  private SleepTime(final int value, final String label) {
    this.value = new EtomoNumber();
    this.value.set(value);
    this.label = label;
  }

  static SleepTime getInstance(ConstEtomoNumber value) {
    if (value == null || value.isNull() || !value.isValid()) {
      return DEFAULT;
    }
    if (value.equals(ONE.value)) {
      return ONE;
    }
    if (value.equals(TWO.value)) {
      return TWO;
    }
    if (value.equals(THREE.value)) {
      return THREE;
    }
    return USER_ENTRY;
  }

  public boolean isDefault() {
    return this == DEFAULT;
  }

  public ConstEtomoNumber getValue() {
    return value;
  }

  public String toString() {
    return label;
  }

  public String getLabel() {
    return label;
  }
}