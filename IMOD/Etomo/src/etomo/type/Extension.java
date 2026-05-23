package etomo.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import etomo.BaseManager;
import etomo.util.Utilities;

/**
 * <p>Description:Holds all file extensions, including customizable image file extensions.
 * </p>
 * 
 * <p>Copyright: Copyright 2019 - 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class Extension {
  // Place a regular expression for all image input extensions is here:
  private static StringBuilder _imageInputRegex_ = new StringBuilder();

  public static final String EXTENSION_DIVIDER = ".";
  public static final String STANDARDIZATION_DIVIDER = "_";
  public static final String BACKUP_SUFFIX = "~";
  private static final StringBuilder INPUT_IMAGE_FILE_DESCR = new StringBuilder();

  private static final Map<String, Extension> INSTANCES =
    new HashMap<String, Extension>();
  private static final List<Extension> STANDARDIZABLE_IMAGE_FILE_LIST =
    new ArrayList<Extension>();
  private static final List<Extension> INPUT_IMAGE_FILE_LIST = new ArrayList<Extension>();
  private static final List<Extension> VARIABLE_LIST = new ArrayList<Extension>();

  public static final Extension HDF = new Extension("hdf", true, true, true, false);
  public static final Extension MRC = new Extension("mrc", true, true, true, false);
  //
  public static final Extension ST = new Extension("st", true, true, false, false);
  public static final Extension TIF = new Extension("tif", true, true, false, false);
  public static final Extension TIFF = new Extension("tiff", true, true, false, false);
  //
  public static final Extension ALI = new Extension("ali", true, false, false, false);
  public static final Extension ALILOG10 =
    new Extension("alilog10", true, false, false, false);
  public static final Extension BL = new Extension("bl", true, false, false, false);
  public static final Extension DCST = new Extension("dcst", true, false, false, false);
  public static final Extension FLAT = new Extension("flat", true, false, false, false);
  public static final Extension FLIP = new Extension("flip", true, false, false, false);
  static final Extension INPUT = new Extension("input", true, false, false, false);
  public static final Extension JOIN = new Extension("join", true, false, false, false);
  public static final Extension NAD = new Extension("nad", true, false, false, false);
  public static final Extension PREALI =
    new Extension("preali", true, false, false, false);
  public static final Extension REC = new Extension("rec", true, false, false, false);
  public static final Extension ROT = new Extension("rot", true, false, false, false);
  static final Extension SAMPAVG = new Extension("sampavg", true, false, false, false);
  static final Extension SAMPLE = new Extension("sample", true, false, false, false);
  public static final Extension SQZ = new Extension("sqz", true, false, false, false);
  public static final Extension VSR = new Extension("vsr", true, false, false, false);
  //
  public static final Extension SINT = new Extension("sint", true, false, false, true);
  static final Extension SREC = new Extension("srec", true, false, false, true);
  //
  public static final Extension COM = new Extension("com", false, false, false, false);
  public static final Extension JPG = new Extension("jpg", false, false, false, false);
  public static final Extension LOG = new Extension("log", false, false, false, false);
  public static final Extension MAT = new Extension("mat", false, false, false, false);
  public static final Extension MDOC = new Extension("mdoc", false, false, false, false);
  public static final Extension MOD = new Extension("mod", false, false, false, false);
  public static final Extension PNG = new Extension("png", false, false, false, false);
  public static final Extension PRM = new Extension("prm", false, false, false, false);

  public static final Extension PL = new Extension("pl", false, false, false, false);
  public static final Extension WARPXG =
    new Extension("warpxg", false, false, false, false);

  private final String extension;
  private final boolean etomoImageFile, standardImageFileExtension, variable;
  // For file formats that can hold input image stacks (st,mrc,hdf,tif,tiff):
  private final boolean inputImageFile;

  public boolean equals(final String string) {
    if (string == null) {
      return false;
    }
    if (string.equalsIgnoreCase(extension)) {
      return true;
    }
    return false;
  }

  private Extension(final String extension, final boolean etomoImageFile,
    final boolean inputImageFile, final boolean standardImageFileExtension,
    final boolean variable) {
    this.extension = extension;
    this.etomoImageFile = etomoImageFile;
    this.inputImageFile = inputImageFile;
    this.standardImageFileExtension = standardImageFileExtension;
    this.variable = variable;
    INSTANCES.put(extension, this);
    if (inputImageFile) {
      INPUT_IMAGE_FILE_DESCR
        .append((INPUT_IMAGE_FILE_DESCR.length() > 0 ? ", " : "") + extension);
    }
    if (isStandardizableImageFile()) {
      STANDARDIZABLE_IMAGE_FILE_LIST.add(this);
    }
    if (inputImageFile) {
      INPUT_IMAGE_FILE_LIST.add(this);
      if (_imageInputRegex_.length() > 0) {
        _imageInputRegex_.append("|");
      }
      _imageInputRegex_.append("\\Q" + extension + "\\E");
    }
    if (variable) {
      VARIABLE_LIST.add(this);
    }
  }

  public static Iterator<Extension> getStandardizableImageFileIterator() {
    return STANDARDIZABLE_IMAGE_FILE_LIST.iterator();
  }

  public static Iterator<Extension> getInputImageFileIterator() {
    return INPUT_IMAGE_FILE_LIST.iterator();
  }

  /**
   * Returns the instance matching the regular extension of this file name.  Does not
   * return the standardized extension.  Does handle variable extensions.  When the file
   *  name is dataset_rec.mrc, returns MRC.
   * @param fileName
   * @return
   */
  public static Extension getInstance(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return null;
    }
    // Ignore the backup character(s).
    if (fileName.endsWith(BACKUP_SUFFIX)) {
      fileName = fileName.substring(0, fileName.length() - 1);
    }
    String ext = Utilities.getExtension(fileName);
    if (ext == null) {
      // No "." so treat fileName as an extension string.
      ext = fileName;
    }
    if (ext.isEmpty()) {
      return null;
    }
    Extension extension = INSTANCES.get(ext);
    if (extension == null) {
      // may be a variable extension (.srec86).
      extension = getVariableInstance(ext);
    }
    if (extension == null) {
      System.err.println("(1)Filename contains unknown extension:" + fileName);
    }
    return extension;
  }

  /**
   * Returns the instance matching the regular extension or the standardized image
   * extension.  Handles variable extensions.  When the imageFilenameStyle is MRC and the
   * file name is dataset_rec.mrc, returns REC.  When the imageFilenameStyle is not MRC
   * and the file name is dataset_rec.mrc, returns MRC.
   * @param fileName
   * @param imageFilenameStyle
   * @return
   */
  public static Extension getInstance(final BaseManager manager, final String fileName,
    ImageFilenameStyle imageFilenameStyle) {
    if (fileName == null || fileName.isEmpty()) {
      return null;
    }
    String ext = Utilities.getExtension(fileName);
    Boolean extOnly = false;
    if (ext == null) {
      // No "." so treat fileName as an extension string.
      ext = fileName;
      extOnly = true;
    }
    if (ext.isEmpty()) {
      return null;
    }
    Extension extension = INSTANCES.get(ext);
    if (extension == null) {
      // may be a variable extension (.srec86).
      extension = getVariableInstance(ext);
    }
    // Unknown extension
    if (extension == null) {
      return null;
    }
    if (imageFilenameStyle == null) {
      imageFilenameStyle = manager.getBaseMetaData().getImageFilenameStyle();
    }
    if (extOnly || imageFilenameStyle == ImageFilenameStyle.OLD
      || imageFilenameStyle.getDefaultRawImageStackExtension() != extension) {
      // This is not a standardized image file name. Return the regular extension.
      return extension;
    }
    // The extension is MRC or HDF and the image filename style matches the extension
    // Return the standardizable image file extension if possible. For example for a
    // fileName that is dataset_rec.mrc, return REC.
    Extension standardizedExtension = getStandardizedInstance(fileName);
    if (standardizedExtension != null) {
      return standardizedExtension;
    }
    // Not a standarized image file name. Return MRC or HDF.
    return extension;
  }

  /**
   * Returns a variable extension if it matches ext - or null.
   * @param ext - extension string (with no .)
   * @return
   */
  private static Extension getVariableInstance(final String ext) {
    // may be a variable extension (.srec86).
    Iterator<Extension> iterator = VARIABLE_LIST.iterator();
    while (iterator.hasNext()) {
      Extension variableExtension = iterator.next();
      if (ext.startsWith(variableExtension.extension) && variableExtension
        .isVariablePiece(ext.substring(variableExtension.extension.length()))) {
        return variableExtension;
      }
    }
    return null;
  }

  public static String getImageInputRegex() {
    return _imageInputRegex_.toString();
  }

  /**
   * Get the standardizeable extension from a standardized file name.  Returns null if
   * fileName is not a standardized file name.
   * @param fileName
   * @return
   */
  private static Extension getStandardizedInstance(final String fileName) {
    if (fileName == null || fileName.isEmpty()
      || fileName.indexOf(STANDARDIZATION_DIVIDER) == -1
      || fileName.indexOf(EXTENSION_DIVIDER) == -1) {
      return null;
    }
    // Standardized file name always end in .mrc or .hdf.
    Extension regExtension = getInstance(fileName);
    if (regExtension == null || !regExtension.isStandardImageFileExtension()) {
      return null;
    }
    // Remove the regular extension. Get the poosible standardizable extension. Call is
    // the same as getSuffix but it removes the "_".
    Extension extension =
      getInstance(Utilities.stripString(true, false, Utilities.removeExtension(fileName),
        true, false, STANDARDIZATION_DIVIDER, true, true));
    // Only return it if its standardizeable.
    if (extension != null && extension.isStandardizableImageFile()) {
      return extension;
    }
    return null;
  }

  /**
   * Returns fileName with newExtension substituted for its extension.  If there is no
   * extension just tacks newExtension onto the end.  Doesn't change a null or empty file
   * name.  Doesn't change the file name if newExtension is null.
   * @param fileName
   * @param newExtension
   * @return
   */
  public static String substituteExtension(final String fileName,
    final Extension newExtension) {
    if (newExtension == null || fileName == null || fileName.isEmpty()) {
      return fileName;
    }
    return Utilities.removeExtension(fileName) + EXTENSION_DIVIDER
      + newExtension.toString();
  }

  /**
   * Strictly substitutes the standardized extension (_ext), leaving the regular extension
   * alone.  Returns null if the file does not have a standardized format, does not
   * have a standard extension (mrc/hdf), if it doesn't contain a standardizeable
   * extension, or if newExtension is not standardizable.
   * @param fileName
   * @param newExtension
   * @return
   */
  public static String substituteStandardizedExtension(String fileName,
    final Extension newExtension) {
    if (newExtension == null || !newExtension.isStandardizableImageFile()
      || fileName == null || fileName.isEmpty()) {
      return null;
    }
    Extension regExtension = getInstance(fileName);
    if (regExtension == null || !regExtension.isStandardImageFileExtension()) {
      return null;
    }
    if (getStandardizedInstance(fileName) == null) {
      return null;
    }
    return Utilities.removeRightSide(fileName, STANDARDIZATION_DIVIDER)
      + STANDARDIZATION_DIVIDER + newExtension.toString() + EXTENSION_DIVIDER
      + regExtension.toString();
  }

  public static String getInputImageFileDescr() {
    return INPUT_IMAGE_FILE_DESCR.toString();
  }

  public boolean isEtomoImageFile() {
    return etomoImageFile;
  }

  public boolean isInputImageFile() {
    return inputImageFile;
  }

  public static boolean isInputImageFile(final String filePath) {
    Extension extension = Extension.getInstance(filePath);
    if (extension == null) {
      return false;
    }
    return extension.isInputImageFile();
  }

  public boolean isVariable() {
    return variable;
  }

  public boolean isStandardizableImageFile() {
    return etomoImageFile && !inputImageFile && !standardImageFileExtension;
  }

  /**
   * Returns true if the fileName ends with this instance's suffix.  A suffix includes the
   * "." or "_".
   * @param imageFilenameStyle
   * @param fileName - file name, path, or extension
   * @return true if fileName ends with this extension
   */
  public boolean fileNameEndsWith(ImageFilenameStyle imageFilenameStyle,
    String fileName) {
    if (fileName == null) {
      return false;
    }
    fileName = fileName.trim();
    if (fileName == null || fileName.isEmpty()) {
      return false;
    }
    if (imageFilenameStyle == null) {
      imageFilenameStyle = ImageFilenameStyle.DEFAULT;
    }
    if (!variable) {
      // Non-variable extension. Test with endsWith.
      String suffix = getSuffix(imageFilenameStyle);
      if (fileName.length() >= suffix.length()) {
        return fileName.endsWith(suffix);
      }
      else {
        // If fileName is the suffix without the divider character, return true;
        return fileName.equals(suffix.substring(1));
      }
    }
    // File has a variable extension. The numeric piece of the file name is required so
    // this file name matches only if a matching extension and valid file number can be
    // found.
    return getFileNumber(fileName, imageFilenameStyle) != null;
  }

  /**
   * The file number is an integer that follows the old style extension.  Only instance
   * with the member variable "variable" can have one and it is at least two digits long.
   * Return the file number of fileName if there is a legal one and the extension matches
   * this extension.
   * @param imageFilenameStyle
   * @param fileName
   * @return
   */
  public ConstEtomoNumber getFileNumber(String fileName,
    ImageFilenameStyle imageFilenameStyle) {
    if (fileName == null) {
      return null;
    }
    fileName = fileName.trim();
    if (fileName == null || fileName.isEmpty()) {
      return null;
    }
    if (imageFilenameStyle == null) {
      imageFilenameStyle = ImageFilenameStyle.DEFAULT;
    }
    if (!variable) {
      return null;
    }
    String fileNumberString = null;
    // Old style file name
    if (imageFilenameStyle == ImageFilenameStyle.OLD || !isStandardizableImageFile()) {
      // Old style file name
      // Look for variable extension: .sintnn, .sintnnn, .srecnn, or .srecnnn.
      fileNumberString = Utilities.getSuffix(fileName, EXTENSION_DIVIDER);
      // Suffix is .sint or .srec.
      String suffix = EXTENSION_DIVIDER + extension;
      if (fileNumberString == null || !fileNumberString.startsWith(suffix)) {
        // name doesn't have an extension or doesn't match.
        return null;
      }
      // Get nn or nnn.
      fileNumberString = Utilities.removeLeftSide(fileNumberString, suffix);
    }
    else {
      // Standardized file name
      // Look for variable extension: _sintnn.mrc, srecnnn.hdf, etc.
      fileNumberString = Utilities.getSuffix(fileName, STANDARDIZATION_DIVIDER);
      // midfix is _sint, _srec.
      String midfix = STANDARDIZATION_DIVIDER + extension;
      if (fileNumberString == null || !fileNumberString.startsWith(midfix)) {
        // name doesn't have an extension or doesn't match.
        return null;
      }
      // reduce to nn.mrn, nn.hdf, nnn.mrc, or nnn.hdf
      fileNumberString = Utilities.removeLeftSide(fileNumberString, midfix);
      // Get nn or nnn.
      fileNumberString = Utilities.removeRightSide(fileNumberString, EXTENSION_DIVIDER);
    }
    if (isVariablePiece(fileNumberString)) {
      EtomoNumber fileNumber = new EtomoNumber(EtomoNumber.Type.LONG);
      fileNumber.set(fileNumberString);
      if (!fileNumber.isNull() && fileNumber.isValid()) {
        return fileNumber;
      }
    }
    return null;
  }

  /**
   * 
   * @param number
   * @return true if an integer at least two digits long (nn, nnn)
   */
  private boolean isVariablePiece(String number) {
    // More then three digits is fine. A dataset may need more then the predicted maximum
    // number of files.
    if (number == null || number.length() < 2) {
      // must be at least nn.
      return false;
    }
    try {
      Long.parseLong(number);
      return true;
    }
    catch (NumberFormatException e) {}
    // not an integer
    return false;
  }

  public String toString() {
    return extension;
  }

  public int length() {
    return extension.length();
  }

  /**
   * Returns the current extension or standardized suffix: .ext, _ext.mrc, or _ext.hdf.
   * Includes the "." (and the "_" where used).
   * @param imageFilenameStyle
   * @return
   */
  public String getSuffix(ImageFilenameStyle imageFilenameStyle) {
    if (imageFilenameStyle == null) {
      imageFilenameStyle = ImageFilenameStyle.DEFAULT;
    }
    if (imageFilenameStyle == ImageFilenameStyle.OLD || !isStandardizableImageFile()) {
      return EXTENSION_DIVIDER + extension;
    }
    return STANDARDIZATION_DIVIDER + extension + EXTENSION_DIVIDER
      + imageFilenameStyle.getDefaultRawImageStackExtension().extension;
  }

  public boolean isStandardImageFileExtension() {
    return standardImageFileExtension;
  }

  boolean isCompatible(final ExtensionMarker extensionMarker) {
    if (extensionMarker == ExtensionMarker.INPUT_IMAGE) {
      return inputImageFile;
    }
    if (extensionMarker == ExtensionMarker.IMAGE) {
      return standardImageFileExtension;
    }
    return true;
  }
}
