package etomo.type;

import java.util.Properties;

import etomo.EtomoDirector;
import etomo.util.Utilities;

public final class ImageFilenameStyle {
  private static int INDEX = 0;
  public static final ImageFilenameStyle OLD =
    new ImageFilenameStyle(0, null, Extension.ST, INDEX++, "OLD");
  public static final ImageFilenameStyle MRC =
    new ImageFilenameStyle(1, ImodOutputFormat.MRC, Extension.MRC, INDEX++, "MRC");
  public static final ImageFilenameStyle HDF =
    new ImageFilenameStyle(2, ImodOutputFormat.HDF, Extension.HDF, INDEX++, "HDF");
  public static final int TOTAL = INDEX;

  public static final ImageFilenameStyle DEFAULT =
    EtomoDirector.DEFAULT_TO_STANDARD_IMAGE_FILE_NAMES ? MRC
      : OLD/*2206 Change to MRC when we decide to*/;
  public static final ImageFilenameStyle BACKWARDS_COMPATIBLE = OLD;
  public static final String ENV_VAR = "ETOMO_NAMING_STYLE";

  private final EtomoNumber value = new EtomoNumber();
  // Set for filename styles that use a standard naming convention.
  private final ImodOutputFormat imodOutputFormat;
  // Don't allow defaultRawImageStackExtension to be null.
  final Extension defaultRawImageStackExtension;
  private final int index;
  // Warning: do not change propertyValue - required for backward compatibility. Don't
  // allow to be null.
  private final String propertyValue;

  private ImageFilenameStyle(final int value, final ImodOutputFormat imodOutputFormat,
    final Extension defaultRawImageStackExtension, final int index,
    final String propertyValue) {
    this.value.set(value);
    this.imodOutputFormat = imodOutputFormat;
    this.defaultRawImageStackExtension = defaultRawImageStackExtension;
    this.index = index;
    this.propertyValue = propertyValue;
  }

  /**
   * @return instance based on ImodOutputFormat
   */
  public static ImageFilenameStyle getInstance(final ImodOutputFormat imodOutputFormat) {
    if (MRC.imodOutputFormat == imodOutputFormat) {
      return MRC;
    }
    if (HDF.imodOutputFormat == imodOutputFormat) {
      return HDF;
    }
    return DEFAULT;
  }

  /**
   * Returns default for bad value
   * @param value
   * @return
   */
  public static ImageFilenameStyle getInstance(final String string,
    final boolean allowDefault) {
    if (OLD.equals(string)) {
      return OLD;
    }
    if (MRC.equals(string)) {
      return MRC;
    }
    if (HDF.equals(string)) {
      return HDF;
    }
    if (allowDefault) {
      return DEFAULT;
    }
    return null;
  }

  /**
   * Returns null for bad value
   * @param value
   * @return
   */
  public static ImageFilenameStyle getInstance(final ConstEtomoNumber namingStyle) {
    if (namingStyle == null) {
      return null;
    }
    if (OLD.value.equals(namingStyle)) {
      return OLD;
    }
    if (MRC.value.equals(namingStyle)) {
      return MRC;
    }
    if (HDF.value.equals(namingStyle)) {
      return HDF;
    }
    return null;
  }

  public static boolean isValidValue(final ConstEtomoNumber namingStyle) {
    if (namingStyle == null) {
      return false;
    }
    if (OLD.value.equals(namingStyle)) {
      return true;
    }
    if (MRC.value.equals(namingStyle)) {
      return true;
    }
    if (HDF.value.equals(namingStyle)) {
      return true;
    }
    return false;
  }

  /**
   * Returns default for bad index
   * @param value
   * @return
   */
  public static ImageFilenameStyle getInstanceFromIndex(final int index) {
    if (OLD.index == index) {
      return OLD;
    }
    if (MRC.index == index) {
      return MRC;
    }
    if (HDF.index == index) {
      return HDF;
    }
    return DEFAULT;
  }

  public static ImageFilenameStyle
    getInstanceFromPropertyValue(final String propertyValue) {
    if (OLD.propertyValue.equals(propertyValue)) {
      return OLD;
    }
    if (MRC.propertyValue.equals(propertyValue)) {
      return MRC;
    }
    if (HDF.propertyValue.equals(propertyValue)) {
      return HDF;
    }
    return null;
  }

  public static ImageFilenameStyle load(final Properties props, final String prepend,
    final String key) {
    return getInstanceFromPropertyValue(
      props.getProperty(Utilities.createPropertyKey(prepend, key)));
  }

  /**
   * index and TOTAL are for enumerating through all instances.  
   * @return
   */
  public int getIndex() {
    return index;
  }

  public void store(final Properties props, final String prepend, final String key) {
    props.setProperty(Utilities.createPropertyKey(prepend, key), propertyValue);
  }

  public boolean isStandard() {
    // Standard styles have a standard output format.
    return imodOutputFormat != null;
  }

  public ConstEtomoNumber getValue() {
    return value;
  }

  public String getPropertyValue() {
    return propertyValue;
  }

  public boolean equals(final ImodOutputFormat imodOutputFormat) {
    return this.imodOutputFormat == imodOutputFormat;
  }

  public boolean equals(final String string) {
    if (string == null) {
      return false;
    }
    if (value.equals(string) || defaultRawImageStackExtension.equals(string)
      || propertyValue.equalsIgnoreCase(string)) {
      return true;
    }
    return false;
  }

  public Extension getDefaultRawImageStackExtension() {
    return defaultRawImageStackExtension;
  }

  public String toString() {
    return value.toString();
  }

  static final class ImageFilenameStyleException extends Exception {
    ImageFilenameStyleException(final String message) {
      super(message);
    }
  }
}
