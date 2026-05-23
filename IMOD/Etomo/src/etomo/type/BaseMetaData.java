package etomo.type;

import java.util.Properties;

import etomo.BaseManager;
import etomo.process.TomosetextsOutput;
import etomo.storage.Storable;
import etomo.ui.LogProperties;

/**
 * <p>Description: Parent class for meta data classes.</p>
 * 
 * <p>Copyright: Copyright 2002 - 2021 by the Regents of the University of Colorado</p>
 *
 *<p>Organization: Dept. of MCD Biology, University of Colorado</p>
 * 
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.8  2011/02/22 05:30:49  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.7  2007/12/10 22:33:43  sueh
 * <p> bug# 1041 Added current processchunks root name and subdir name.
 * <p>
 * <p> Revision 1.6  2007/03/26 23:34:22  sueh
 * <p> bug# 964 Reduced visibility of inherited fields.
 * <p>
 * <p> Revision 1.5  2007/02/05 23:09:42  sueh
 * <p> bug# 962 Made revisionNumber a EtomoVersion, so it can be compared.
 * <p>
 * <p> Revision 1.4  2005/12/14 01:28:07  sueh
 * <p> bug# 782 Updated toString().
 * <p>
 * <p> Revision 1.3  2005/05/17 19:15:31  sueh
 * <p> bug# 663 Allowing isValid() to be called from the base class.
 * <p>
 * <p> Revision 1.2  2004/11/19 23:31:33  sueh
 * <p> bug# 520 merging Etomo_3-4-6_JOIN branch to head.
 * <p>
 * <p> Revision 1.1.2.4  2004/10/29 01:18:18  sueh
 * <p> bug# 520 Removing unecessary abstract isValid functions.
 * <p>
 * <p> Revision 1.1.2.3  2004/10/15 00:16:21  sueh
 * <p> bug# 520 Added toString().
 * <p>
 * <p> Revision 1.1.2.2  2004/10/01 19:44:27  sueh
 * <p> bug# 520 provide a standard way to get the identifier of a meta data file.
 * <p> Add a file extension static, since there are two meta data file extensions.
 * <p>
 * <p> Revision 1.1.2.1  2004/09/29 19:15:08  sueh
 * <p> bug# 520 Added base class for ConstMetaData and ConstJoinMetaData.
 * <p> Implements Storable with abstract class.  Implements store(Properties),
 * <p> since this function is generic and suitable for a const class.
 * <p> </p>
 */
public abstract class BaseMetaData implements Storable {
  private static final String CURRENT_PROCESSCHUNKS_ROOT_NAME =
    "CurrentProcesschunksRootName";
  private static final String CURRENT_PROCESSCHUNKS_SUBDIR_NAME =
    "CurrentProcesschunksSubdirName";
  static final String revisionNumberString = "RevisionNumber";
  private static final String FIRST_SAVED_ETOMO_VERSION = "4.10.43";

  String fileExtension;

  // revisionNumber should be set only by load()
  EtomoVersion revisionNumber = EtomoVersion.getEmptyInstance(revisionNumberString);
  AxisType axisType = AxisType.NOT_SET;
  String invalidReason = "";

  private final StringProperty currentProcesschunksRootNameA =
    new StringProperty("A." + CURRENT_PROCESSCHUNKS_ROOT_NAME);
  private final StringProperty currentProcesschunksRootNameB =
    new StringProperty("B." + CURRENT_PROCESSCHUNKS_ROOT_NAME);
  private final StringProperty currentProcesschunksSubdirNameA =
    new StringProperty("A." + CURRENT_PROCESSCHUNKS_SUBDIR_NAME);
  private final StringProperty currentProcesschunksSubdirNameB =
    new StringProperty("B." + CURRENT_PROCESSCHUNKS_SUBDIR_NAME);

  private final EtomoVersion etomoCreatedVersion =
    EtomoVersion.getEmptyInstance("Version.Etomo.Created");
  private final EtomoVersion etomoModifiedVersion =
    EtomoVersion.getEmptyInstance("Version.Etomo.Modified");

  private final ImageFileMetaData imageFileMetaData;
  private final LogProperties logProperties;
  private final BaseManager manager;
  private final boolean newDataset;
  private final boolean canCorrectImageFilenameStyle;

  public abstract String getMetaDataFileName();

  public abstract String getName();

  public abstract String getDatasetName();

  public abstract boolean isValid();

  abstract String getGroupKey();

  BaseMetaData(final BaseManager manager, final LogProperties logProperties,
    final boolean forceOldStyle, final boolean newDataset,
    final boolean canCorrectImageFilenameStyle) {
    this.manager = manager;
    this.logProperties = logProperties;
    this.newDataset = newDataset;
    this.canCorrectImageFilenameStyle = canCorrectImageFilenameStyle;
    imageFileMetaData = new ImageFileMetaData(forceOldStyle, null, newDataset);
  }

  BaseMetaData(final BaseManager manager, final LogProperties logProperties,
    final ImageFilenameStyle imageFilenameStyle, final boolean newDataset,
    final boolean canCorrectImageFilenameStyle) {
    this.manager = manager;
    this.logProperties = logProperties;
    this.newDataset = newDataset;
    this.canCorrectImageFilenameStyle = canCorrectImageFilenameStyle;
    imageFileMetaData = new ImageFileMetaData(false, imageFilenameStyle, newDataset);
  }

  BaseMetaData(final BaseManager manager, final LogProperties logProperties,
    final boolean forceOldStyle, final ImageFilenameStyle imageFilenameStyle,
    final boolean newDataset, final boolean canCorrectImageFilenameStyle) {
    this.manager = manager;
    this.logProperties = logProperties;
    this.newDataset = newDataset;
    this.canCorrectImageFilenameStyle = canCorrectImageFilenameStyle;
    imageFileMetaData =
      new ImageFileMetaData(forceOldStyle, imageFilenameStyle, newDataset);
  }

  /**
   * No effect.  Override this for interfaces with an input image file
   * @return
   */
  public void setRawImageStackExtension(final Extension input) {}

  /**
   * No effect.  Override this for interfaces with an input image file
   * @return
   */
  public void setOrigRawImageStackExtension(final Extension input) {}

  public final ImageFilenameStyle getImageFilenameStyle() {
    return imageFileMetaData.getImageFilenameStyle();
  }

  public final ImageOutputFormat getImageOutputFormat() {
    return imageFileMetaData.getImageOutputFormat();
  }

  final boolean etomoModifiedVersionLt(final String version) {
    return etomoModifiedVersion.lt(version);
  }

  /**
   * Just returns the default.  MetaData that refers to a raw image input file
   * should override this and return
   * the extension of the file originally choosen by the user.
   * Should never return null.
   * @return
   */
  public Extension getOrigRawImageStackExtension() {
    return imageFileMetaData.getDefaultRawImageStackExtension();
  }

  /**
   * Just returns the default.  Override this if there is an actual raw image stack
   * involved.  Should never return null.
   * @return
   */
  public Extension getRawImageStackExtension() {
    return imageFileMetaData.getDefaultRawImageStackExtension();
  }

  public String toString() {
    return "[fileExtension:" + fileExtension + ",revisionNumber:" + revisionNumber
      + ",\naxisType:" + axisType + ",invalidReason:" + invalidReason + "super:"
      + super.toString() + "]";
  }

  public void store(Properties props) {
    store(props, "");
  }

  public void load(Properties props) {
    load(props, "");
  }

  String createPrepend(final String prepend) {
    if (prepend.equals("")) {
      return getGroupKey();
    }
    return prepend + "." + getGroupKey();
  }

  public void store(Properties props, String prepend) {
    if (newDataset) {
      etomoCreatedVersion.set(ImodVersion.CURRENT_VERSION);
    }
    etomoModifiedVersion.set(ImodVersion.CURRENT_VERSION);
    prepend = createPrepend(prepend);
    etomoCreatedVersion.store(props, prepend);
    etomoModifiedVersion.store(props, prepend);
    imageFileMetaData.store(props, prepend);
    currentProcesschunksRootNameA.store(props, prepend);
    currentProcesschunksRootNameB.store(props, prepend);
    currentProcesschunksSubdirNameA.store(props, prepend);
    currentProcesschunksSubdirNameB.store(props, prepend);
    if (logProperties != null) {
      logProperties.store(props, prepend);
    }
  }

  public void load(final Properties props, final String parentPrepend) {
    // reset
    etomoModifiedVersion.reset();
    currentProcesschunksRootNameA.reset();
    currentProcesschunksRootNameB.reset();
    currentProcesschunksSubdirNameA.reset();
    currentProcesschunksSubdirNameB.reset();
    // load
    String prepend = createPrepend(parentPrepend);
    etomoCreatedVersion.load(props, prepend);
    etomoModifiedVersion.load(props, prepend);
    imageFileMetaData.load(manager, props, prepend);
    currentProcesschunksRootNameA.load(props, prepend);
    currentProcesschunksRootNameB.load(props, prepend);
    currentProcesschunksSubdirNameA.load(props, prepend);
    currentProcesschunksSubdirNameB.load(props, prepend);
    if (logProperties != null) {
      logProperties.load(props, prepend);
    }

    // If canCorrectImageFilenameStyle is false, then correcting imageFilenameStyle may
    // require the child load function to be run before hand. It is the child class's
    // responsibility to correct imageFilenameStyle if canCorrectImageFilenameStyle is
    // false.
    if (canCorrectImageFilenameStyle) {
      checkImageFilenameStyleLoaded(prepend);
    }
  }

  void checkImageFilenameStyleLoaded(final String parentPrepend) {
    if (!wasImageFilenameStyleLoaded()) {
      repairImageFilenameStyle(parentPrepend);
    }
  }

  final String getImageFilenameStyleKey(final String parentPrepend) {
    return imageFileMetaData.getImageFilenameStyleKey(createPrepend(parentPrepend));
  }

  final boolean correctImageFilenameStyle(final String parentPrepend,
    final ImageFilenameStyle input) {
    return imageFileMetaData.correctImageFilenameStyle(parentPrepend, input);
  }

  public final boolean isOldImageFilenameStyle() {
    return imageFileMetaData.isOldImageFilenameStyle();
  }

  public final EtomoVersion getRevisionNumber() {
    return revisionNumber;
  }

  public final AxisType getAxisType() {
    return axisType;
  }

  public String getInvalidReason() {
    return invalidReason;
  }

  public final String getFileExtension() {
    return fileExtension;
  }

  public final boolean equals(BaseMetaData input) {
    if (!currentProcesschunksRootNameA.equals(input.currentProcesschunksRootNameA)) {
      return false;
    }
    if (!currentProcesschunksRootNameB.equals(input.currentProcesschunksRootNameB)) {
      return false;
    }
    if (axisType != input.axisType) {
      return false;
    }
    return true;
  }

  public boolean equals(Object object) {
    if (!(object instanceof BaseMetaData))
      return false;
    return equals((BaseMetaData) object);
  }

  final boolean isEtomoCreatedVersionSet() {
    return etomoCreatedVersion.isNull();
  }

  final boolean wasImageFilenameStyleLoaded() {
    return imageFileMetaData.wasImageFilenameStyleLoaded();
  }

  /**
   * Set an empty image filename style to old style.
   * 
   * @param parentPrepend prepend needed to generate the property key
   */
  void repairImageFilenameStyle(final String parentPrepend) {
    if (wasImageFilenameStyleLoaded()) {
      return;
    }
    String repairType = "old image file name style";
    ImageFilenameStyle imageFilenameStyle = ImageFilenameStyle.OLD;
    String key = getImageFilenameStyleKey(parentPrepend);
    if (imageFilenameStyle != null) {
      System.err.println("\nINFO: Attempting to repair the " + key
        + " property, which is missing from the\ndataset file.  Using the " + repairType
        + " and\nsetting " + key + " to " + imageFilenameStyle + ".\n");
      if (correctImageFilenameStyle(parentPrepend, imageFilenameStyle)) {
        return;
      }
    }
    System.err.println("\nERROR: Unable to repair the " + key
      + " property, which is missing from the\ndataset file."
      + (repairType != null ? "  Unable to use the " + repairType : null) + ".");
  }

  final ImageFilenameStyle getDatasetImageFilenameStyle() {
    ImageFilenameStyle imageFilenameStyle = null;
    TomosetextsOutput output = manager.tomosetexts();
    if (output != null) {
      imageFilenameStyle = output.getImageFilenameStyle();
    }
    if (imageFilenameStyle != null) {
      return imageFilenameStyle;
    }
    return null;
  }

  public final String getCurrentProcesschunksRootName(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return currentProcesschunksRootNameB.toString();
    }
    return currentProcesschunksRootNameA.toString();
  }

  public final String getCurrentProcesschunksSubdirName(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return currentProcesschunksSubdirNameB.toString();
    }
    return currentProcesschunksSubdirNameA.toString();
  }

  public final boolean isCurrentProcesschunksSubdirNameSet(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return !currentProcesschunksSubdirNameB.isEmpty();
    }
    return !currentProcesschunksSubdirNameA.isEmpty();
  }

  public final void setCurrentProcesschunksRootName(final AxisID axisID,
    final String input) {
    if (axisID == AxisID.SECOND) {
      currentProcesschunksRootNameB.set(input);
    }
    else {
      currentProcesschunksRootNameA.set(input);
    }
  }

  public final void setCurrentProcesschunksSubdirName(final AxisID axisID,
    final String input) {
    if (axisID == AxisID.SECOND) {
      currentProcesschunksSubdirNameB.set(input);
    }
    else {
      currentProcesschunksSubdirNameA.set(input);
    }
  }

  public final void resetCurrentProcesschunksRootName(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      currentProcesschunksRootNameB.reset();
    }
    else {
      currentProcesschunksRootNameA.reset();
    }
  }

  public final void resetCurrentProcesschunksSubdirName(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      currentProcesschunksSubdirNameB.reset();
    }
    else {
      currentProcesschunksSubdirNameA.reset();
    }
  }
}