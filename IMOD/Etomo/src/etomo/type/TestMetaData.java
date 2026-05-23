package etomo.type;

import etomo.BaseManager;
import etomo.TestManager;

public final class TestMetaData extends BaseMetaData {
  private Extension rawImageStackExtension = null;
  private Extension origRawImageStackExtension = null;

  public TestMetaData(final BaseManager manager, final AxisType axisType,
    final ImageFilenameStyle imageFilenameStyle, final boolean newDataset) {
    super(manager, null, imageFilenameStyle, newDataset, true);
    this.axisType = axisType;
  }

  public String toString() {
    return "\n[imageFilenameStyle:" + getImageFilenameStyle() + ",rawImageStackExtension:"
      + rawImageStackExtension + ",origRawImageStackExtension:"
      + origRawImageStackExtension + "]";
  }

  @Override
  public void setRawImageStackExtension(final Extension input) {
    if (input != null) {
      rawImageStackExtension = input;
    }
  }

  @Override
  public void setOrigRawImageStackExtension(final Extension input) {
    if (input != null) {
      origRawImageStackExtension = input;
    }
  }

  public Extension getOrigRawImageStackExtension() {
    if (origRawImageStackExtension != null) {
      return origRawImageStackExtension;
    }
    return super.getOrigRawImageStackExtension();
  }

  /**
   * Just returns the default.  Override this if there is an actual raw image stack
   * involved.  Should never return null.
   * @return
   */
  public Extension getRawImageStackExtension() {
    if (rawImageStackExtension != null) {
      return rawImageStackExtension;
    }
    return super.getRawImageStackExtension();
  }

  public String getMetaDataFileName() {
    return null;
  }

  public String getName() {
    return TestManager.DATASET_NAME;
  }

  public String getDatasetName() {
    return TestManager.DATASET_NAME;
  }

  public boolean isValid() {
    return true;
  }

  String getGroupKey() {
    return null;
  }
}
