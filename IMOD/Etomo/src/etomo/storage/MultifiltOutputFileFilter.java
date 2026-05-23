package etomo.storage;

import java.io.File;
import java.math.BigDecimal;

import etomo.BaseManager;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.BaseMetaData;
import etomo.type.FileType;
import etomo.type.ImageFilenameStyle;

public final class MultifiltOutputFileFilter extends ExtensionFileFilter {
  private final BaseManager manager;
  private final AxisID axisID;
  private final FileType fileType;

  private MultifiltOutputFileFilter(final BaseManager manager, final AxisID axisID,
    final FileType fileType) {
    super(manager, false, false, false);
    this.manager = manager;
    this.axisID = axisID;
    this.fileType = fileType;
  }

  public static MultifiltOutputFileFilter getInstance(final BaseManager manager,
    final ImageFilenameStyle imageFilenameStyle, final AxisID axisID,
    final FileType fileType) {
    MultifiltOutputFileFilter instance =
      new MultifiltOutputFileFilter(manager, axisID, fileType);
    instance.setup(manager);
    return instance;
  }

  private void setup(final BaseManager manager) {
    FileType[] fileTypes;
    if (fileType != null) {
      fileTypes = new FileType[] { fileType };
    }
    else {
      fileTypes =
        new FileType[] { FileType.MUTLIFILT_FAKE_SIRT_ITERATIONS_OUTPUT_TEMPLATE,
          FileType.MUTLIFILT_EXACT_OBJECT_SIZES_OUTPUT_TEMPLATE,
          FileType.MUTLIFILT_GAUSSIAN_OUTPUT_TEMPLATE,
          FileType.MUTLIFILT_HAMMING_LIKE_STARTS_OUTPUT_TEMPLATE };
    }
    String datasetName = null;
    AxisType axisType = null;
    if (manager != null) {
      BaseMetaData metaData = manager.getBaseMetaData();
      if (metaData != null) {
        datasetName = metaData.getName();
        axisType = metaData.getAxisType();
      }
    }
    super.setup(datasetName, axisType, axisID, null, null, fileTypes);
  }

  @Override
  /**
   * Return true if the file name contains a multifilt output template followed by valid
   * number(s).
   */
  public boolean accept(final File file) {
    return super.accept(file);
  }

  /**
   * Return true if the file name contains a multifilt output template followed by valid
   * number(s).  If the fileType member variable is set, it only return true if the fileName
   * type matches it.
   * @param dir ignored
   * @param fileName must match a fileType for true to be returned
   */
  public boolean accept(final File dir, final String fileName) {
    return super.accept(dir, fileName);
  }

  /**
   * Return true if the file name contains a multifilt output template followed by valid
   * number(s).  If the fileType member variable is set, it only return true if the fileName
   * type matches it.
   * @param dir ignored
   * @param fileName must match a fileType for true to be returned
   * @deprecated only for testing
   */
  @SuppressWarnings("deprecation")
  public boolean acceptForRegressionTest(final File dir, final String fileName) {
    if (fileName == null || fileName.endsWith("~")) {
      return false;
    }
    if (fileType != null) {
      return acceptTemplate(fileName, fileType);
    }
    // dateset_slfinnnn.mrc
    if (acceptTemplate(fileName,
      FileType.MUTLIFILT_FAKE_SIRT_ITERATIONS_OUTPUT_TEMPLATE_OLD)) {
      return true;
    }
    // dataset_efosnnnn.mrc
    if (acceptTemplate(fileName,
      FileType.MUTLIFILT_EXACT_OBJECT_SIZES_OUTPUT_TEMPLATE_OLD)) {
      return true;
    }
    // dataset_gfc0.xxx-f0.xxx.mrc
    if (acceptTemplate(fileName, FileType.MUTLIFILT_GAUSSIAN_OUTPUT_TEMPLATE_OLD)) {
      return true;
    }
    // dataset_hlfs0.xxx.mrc
    if (acceptTemplate(fileName,
      FileType.MUTLIFILT_HAMMING_LIKE_STARTS_OUTPUT_TEMPLATE_OLD)) {
      return true;
    }
    return false;
  }

  /**
   * The file name should be longer then the template.  The part of the file name that
   * extends beyond the template should be a valid number. 
   * @param fileName
   * @param template
   * @param middlePiece - optional the "-f" in the gaussian output
   * @deprecated
   * @return
   */
  private boolean acceptTemplate(final String fileName, final FileType fileType) {
    String template = fileType.getTemplate(manager, axisID);
    if (!fileName.startsWith(template)) {
      return false;
    }
    int templateLength = template.length();
    if (fileName.length() <= templateLength) {
      return false;
    }
    String middlePiece = fileType.getMiddlePiece();
    int middlePieceIndex = -1;
    int middlePieceLength = 0;
    if (middlePiece != null) {
      middlePieceLength = middlePiece.length();
      middlePieceIndex = fileName.indexOf(middlePiece, templateLength);
      if (middlePieceIndex == -1) {
        return false;
      }
    }
    String extension = fileType.getExtension();
    if (extension == null) {
      return false;
    }
    int extIndex = fileName.indexOf(extension, templateLength + middlePieceLength);
    if (extIndex == -1) {
      return false;
    }
    // Check the first number
    String substring;
    if (middlePieceIndex != -1) {
      substring = fileName.substring(templateLength, middlePieceIndex);
    }
    else {
      substring = fileName.substring(templateLength, extIndex);
    }
    try {
      new BigDecimal(substring);
    }
    catch (NumberFormatException e) {
      return false;
    }
    // Check the second number
    if (middlePieceIndex != -1) {
      substring = fileName.substring(middlePieceIndex + middlePieceLength, extIndex);
      try {
        new BigDecimal(substring);
      }
      catch (NumberFormatException e) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String getDescription() {
    if (fileType != null) {
      return fileType.getDescription();
    }
    return "All Filter Trials";
  }
}
