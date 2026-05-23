package etomo.type;

import java.util.Properties;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.util.EnvironmentVariable;
import etomo.util.Utilities;

/**
 * <p>Description: Protects image file settings.  Does not have its own prepend.</p>
 * 
 * <p>Actually "ImageFile" is basically its prepend, but there's only one property being
 * saved so it doesn't need special createPrepend functionality.</p>
 * 
 * <p>Copyright: Copyright 2020 - 2022 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class ImageFileMetaData {
  private static final String IMAGE_FILENAME_STYLE_KEY = "ImageFile.ImageFilenameStyle";

  private final ImodOutputFormat envImodOutputFormat;
  private final boolean newDataset;

  // Set from command line and environment when dataset in new. After that should always
  // come from the dataset. When null the default (ImageFIlenameStyle.OLD) is returned.
  private ImageFilenameStyle imageFilenameStyle;

  /**
   * <p>Sets imageFilenameStyle for a new dataset.  For an existing dataset this value
   * with be overridden in the load() function.</p>
   */
  ImageFileMetaData(final boolean forceOldStyle,
    final ImageFilenameStyle overrideImageFilenameStyle, final boolean newDataset) {
    envImodOutputFormat = getEnvImodOutputFormat();
    this.newDataset = newDataset;
    // Set imageFilenameStyle
    // Set imageFilenameStyle to the new dataset value.
    if (!forceOldStyle && overrideImageFilenameStyle == null) {
      // Parameter overrides the environment variables.
      imageFilenameStyle = EtomoDirector.INSTANCE.getArguments().getImageFilenameStyle();
      if (imageFilenameStyle == null) {
        // Set from environment variable(s).
        // ETOMO_NAMING_STYLE overrides IMOD_OUTPUT_FORMAT for image file name style.
        // ETOMO_NAMING_STYLE allows OLD style to be used with HDF output format.
        imageFilenameStyle = getEnvImageFilenameStyle();
      }
      if (imageFilenameStyle == null) {
        // Derive imageFilenameStyle from imageOutputFormat or use the default
        // imageFilenameStyle.
        imageFilenameStyle = ImageFilenameStyle.getInstance(envImodOutputFormat);
      }
    }
    else if (overrideImageFilenameStyle != null) {
      // Use the override image filename style parameter. This is most likely from the
      // test manager.
      imageFilenameStyle = overrideImageFilenameStyle;
    }
    else {
      // ForceOnlyStyle: Some managers such as PEET are old style only.
      imageFilenameStyle = ImageFilenameStyle.OLD;
    }
  }

  public static ImageFileMetaData getTempInstance() {
    return new ImageFileMetaData(false, null, false);
  }

  /**
   * 
   * @param manager
   * @param props
   * @param parentPrepend
   */
  final void load(final BaseManager manager, Properties props,
    final String parentPrepend) {
    if (newDataset) {
      // New dataset - use the settings from the constructor.
      System.err.println("INFO:Dataset in " + manager.getPropertyUserDir() + " is new.");
    }
    else {
      // reset
      imageFilenameStyle = null;
      // load
      imageFilenameStyle =
        ImageFilenameStyle.load(props, parentPrepend, IMAGE_FILENAME_STYLE_KEY);
      if (imageFilenameStyle == null) {
        System.err.println("\nINFO: The " + getImageFilenameStyleKey(parentPrepend)
          + " property is missing from the dataset file.  Unless it is\nrepaired, "
          + "old-style names will be assumed.  If the repair fails, then set this "
          + "property to the correct\nvalue (" + ImageFilenameStyle.MRC.getPropertyValue()
          + " or " + ImageFilenameStyle.HDF.getPropertyValue()
          + ").  Further messages will show "
          + "whether or not the correction succeeded.\n");
      }
    }
  }

  /**
   * @return the real state of imageFilenameStyle.
   */
  boolean wasImageFilenameStyleLoaded() {
    return imageFilenameStyle != null;
  }

  String getImageFilenameStyleKey(final String parentPrepend) {
    return Utilities.createPropertyKey(parentPrepend, IMAGE_FILENAME_STYLE_KEY);
  }

  // Bug# 2403
  /**
   * WARNING: changing imageFilenameStyle can make a dataset incompatible with its files.
   * Only use this function to fix this incompatibility.  Function will not allow
   * imageFilenameStyle to change unless it is null, and will not allow
   * imageFilenameStyle to be set to null.  [Bug# 2386, Bug# 2403]
   * @param input
   * @return true of correction was done or was not necessary
   */
  final boolean correctImageFilenameStyle(final String parentPrepend,
    final ImageFilenameStyle newImageFilenameStyle) {
    String key = getImageFilenameStyleKey(parentPrepend);
    if (newImageFilenameStyle == imageFilenameStyle) {
      if (newImageFilenameStyle != null) {
        System.err.println("\nINFO: " + key + " already equals "
          + newImageFilenameStyle.getPropertyValue() + ".  No correction necessary.\n");
        return true;
      }
      else {
        System.err.println("\nWARNING: Unable to correct " + key
          + " with a null value.  No correction made.\n");
        return false;
      }
    }
    String warning = null;
    if (newImageFilenameStyle == null) {
      // Bug# 2403
      warning = "\nWARNING: Attempt to delete " + key + " (currently set to "
        + imageFilenameStyle.getPropertyValue() + ") blocked.  [Bug# 2386, Bug# 2403]\n";
    }
    else if (imageFilenameStyle != null) {
      // Bug# 2403
      warning = "\nWARNING: Attempt to change " + key + " from "
        + imageFilenameStyle.getPropertyValue() + " to "
        + newImageFilenameStyle.getPropertyValue()
        + " blocked.  [Bug# 2386, Bug# 2403]\n";
    }
    if (warning == null) {
      imageFilenameStyle = newImageFilenameStyle;
      // Bug# 2403
      System.err.println("\nINFO: Corrected empty " + key + "property.  Value was set to "
        + imageFilenameStyle.getPropertyValue() + ".  [Bug# 2386, Bug# 2403]\n");
      return true;
    }
    System.out.println(warning);
    System.err.println(warning);
    new ImageFilenameStyle.ImageFilenameStyleException(warning).printStackTrace();
    return false;
  }

  /**
   * Retrieves value of environment variable IMOD_OUTPUT_FORMAT.
   * @return null if IMOD_OUTPUT_FORMAT does not exist, "" if its empty.
   */
  private final static ImodOutputFormat getEnvImodOutputFormat() {
    // TODO 2325 Remember this environment variable and its setting
    if (!EnvironmentVariable.INSTANCE.exists(null, null, ImodOutputFormat.ENV_VAR,
      null)) {
      return null;
    }
    return ImodOutputFormat.getInstance(
      EnvironmentVariable.INSTANCE.getValue(null, null, ImodOutputFormat.ENV_VAR, null));
  }

  private final static ImageFilenameStyle getEnvImageFilenameStyle() {
    if (!EnvironmentVariable.INSTANCE.exists(null, null, ImageFilenameStyle.ENV_VAR,
      null)) {
      return null;
    }
    return ImageFilenameStyle.getInstance(
      EnvironmentVariable.INSTANCE.getValue(null, null, ImageFilenameStyle.ENV_VAR, null),
      true);
  }

  /**
   * Store using the prepend that was passed in - this class does not have its own
   * prepend.
   * @param props
   * @param parentPrepend
   */
  final void store(final Properties props, final String parentPrepend) {
    imageFilenameStyle.store(props, parentPrepend, IMAGE_FILENAME_STYLE_KEY);
  }

  /**
   * Returns imageFilenameStyle or ImageFilenameStyle.OLD.  Never returns null;
   * @return
   */
  public ImageFilenameStyle getImageFilenameStyle() {
    if (imageFilenameStyle != null) {
      return imageFilenameStyle;
    }
    return ImageFilenameStyle.OLD;
  }

  final ImageOutputFormat getImageOutputFormat() {
    if (imageFilenameStyle == null || imageFilenameStyle == ImageFilenameStyle.OLD) {
      // In non-standard datasets the ImageOutputFormat is based on the IMOD_OUTPUT_FORMAT
      // environment variable, or is the default.
      return ImageOutputFormat.getInstance(envImodOutputFormat);
    }
    // In standard datasets the ImageOutputFormat is based on the imageFilenameStyle.
    return ImageOutputFormat.getInstance(imageFilenameStyle);
  }

  /**
   * Do not return null.
   * @return
   */
  public final Extension getDefaultRawImageStackExtension() {
    if (imageFilenameStyle != null) {
      return imageFilenameStyle.getDefaultRawImageStackExtension();
    }
    return ImageFilenameStyle.OLD.getDefaultRawImageStackExtension();
  }

  final boolean isOldImageFilenameStyle() {
    return imageFilenameStyle == null || imageFilenameStyle == ImageFilenameStyle.OLD;
  }
}
