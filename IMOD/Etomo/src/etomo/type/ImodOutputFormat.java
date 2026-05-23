package etomo.type;

public class ImodOutputFormat {
  public static final String ENV_VAR = "IMOD_OUTPUT_FORMAT";

  // Possible environment variable values.
  public static final ImodOutputFormat MRC = new ImodOutputFormat("MRC");
  public static final ImodOutputFormat HDF = new ImodOutputFormat("HDF");
  public static final ImodOutputFormat TIF = new ImodOutputFormat("TIF");
  public static final ImodOutputFormat TIFF = new ImodOutputFormat("TIFF");
  public static final ImodOutputFormat JPG = new ImodOutputFormat("JPG");
  public static final ImodOutputFormat JPEG = new ImodOutputFormat("JPEG");

  private final String value;

  private ImodOutputFormat(final String value) {
    this.value = value;
  }

  public String toString() {
    return value;
  }

  public static ImodOutputFormat getInstance(final String value) {
    if (MRC.value.equals(value)) {
      return MRC;
    }
    if (HDF.value.equals(value)) {
      return HDF;
    }
    if (TIF.value.equals(value)) {
      return TIF;
    }
    if (TIFF.value.equals(value)) {
      return TIFF;
    }
    if (JPG.value.equals(value)) {
      return JPG;
    }
    if (JPEG.value.equals(value)) {
      return JPEG;
    }
    return null;
  }

  public static ImodOutputFormat getInstance(final ImageOutputFormat imageOutputFormat) {
    if (imageOutputFormat == ImageOutputFormat.HDF) {
      return HDF;
    }
    return MRC;
  }

  public boolean equals(final String value) {
    return this.value.equals(value);
  }

  public String getValue() {
    return value;
  }
}
