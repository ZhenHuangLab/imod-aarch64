package etomo.type;

class ExtensionMarker {
  /** 
   * Standard IMAGE extensions: mrc, hdf.    Old to new filename conversion:
   * filename.oldext => filename_oldext.stdext.  Stdext is from
   * FilenameSettings.Settings.imageFilenameStyle when it is not 0 (old style).
   */
  static final ExtensionMarker IMAGE = new ExtensionMarker();
  /**
   * Standard INPUT_IMAGE extensions: mrc, hdf, st, tif, tiff.  Old to new filename
   * conversion:  filename.oldext => filename.stdext.  Stdext is from
   * FilenameSettings.Settings.rawStackExtension.
   */
  static final ExtensionMarker INPUT_IMAGE = new ExtensionMarker();
  /**
   * GENERIC allows all extensions.
   */
  static final ExtensionMarker GENERIC = new ExtensionMarker();

  private ExtensionMarker() {}

  boolean usesStandardExtension(ImageFilenameStyle imageFilenameStyle) {
    if (imageFilenameStyle == null || imageFilenameStyle == ImageFilenameStyle.OLD) {
      return false;
    }
    return this != GENERIC;
  }

  public String toString() {
    if (this == ExtensionMarker.IMAGE) {
      return "[IMAGE]";
    }
    if (this == ExtensionMarker.INPUT_IMAGE) {
      return "[INPUT_IMAGE]";
    }
    if (this == ExtensionMarker.GENERIC) {
      return "[GENERIC]";
    }
    return super.toString();
  }

  /**
   * Look for the first Extension following this extension marker in the pattern.  See if
   * it is compatible with this extension marker.
   * @param markerIndex 
   * @param pattern - File name pattern
   * @return
   */
  boolean isCompatible(final int markerIndex, final Object[] pattern) {
    if (this == GENERIC) {
      return true;
    }
    // If this isn't a pattern containing this Extension Marker and an Extension then
    // consider it non-compatible.
    if (pattern == null || markerIndex < 0 || markerIndex >= pattern.length
      || pattern[markerIndex] != this || markerIndex + 1 >= pattern.length) {
      return false;
    }
    for (int i = markerIndex + 1; i < pattern.length; i++) {
      if (pattern[i] instanceof Extension) {
        return ((Extension) pattern[i]).isCompatible(this);
      }
    }
    // No Extension was found. Consider this pattern to be non-compatible.
    return false;
  }
}
