package etomo.type;

import java.util.Properties;

import etomo.util.Utilities;

public final class ImageOutputFormat {
  public static final ImageOutputFormat MRC = new ImageOutputFormat("MRC");
  public static final ImageOutputFormat HDF = new ImageOutputFormat("HDF");

  public static final ImageOutputFormat DEFAULT = MRC;

  // Warning: do not change propertyValue - required for backward compatibility.
  private final String propertyValue;

  private ImageOutputFormat(final String propertyValue) {
    this.propertyValue = propertyValue;
  }

  /**
   * @return instance based on ImodOutputFormat
   */
  public static ImageOutputFormat getInstance(final ImodOutputFormat imodOutputFormat) {
    if (imodOutputFormat == ImodOutputFormat.MRC) {
      return MRC;
    }
    if (imodOutputFormat == ImodOutputFormat.HDF) {
      return HDF;
    }
    return DEFAULT;
  }

  /**
   * @return instance based on ImodOutputFormat.value
   */
  public static ImageOutputFormat getInstance(final String propertyValue) {
    if (MRC.propertyValue.equals(propertyValue)) {
      return MRC;
    }
    if (HDF.propertyValue.equals(propertyValue)) {
      return HDF;
    }
    return DEFAULT;
  }

  /**
   * @return instance based on imageFilenameStyle
   */
  public static ImageOutputFormat
    getInstance(final ImageFilenameStyle imageFilenameStyle) {
    if (imageFilenameStyle == ImageFilenameStyle.MRC) {
      return MRC;
    }
    if (imageFilenameStyle == ImageFilenameStyle.HDF) {
      return HDF;
    }
    return DEFAULT;
  }

  public static ImageOutputFormat
    getInstanceFromPropertyValue(final String propertyValue) {
    if (MRC.propertyValue.equals(propertyValue)) {
      return MRC;
    }
    if (HDF.propertyValue.equals(propertyValue)) {
      return HDF;
    }
    return null;
  }

  public String toString() {
    return propertyValue;
  }

  public static ImageOutputFormat load(final Properties props, final String prepend,
    final String key) {
    return getInstanceFromPropertyValue(
      props.getProperty(Utilities.createPropertyKey(prepend, key)));
  }

  public void store(final Properties props, final String prepend, final String key) {
    props.setProperty(Utilities.createPropertyKey(prepend, key), propertyValue);
  }
}
