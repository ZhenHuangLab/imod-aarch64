package etomo.process;

import etomo.type.ImageFilenameStyle;

/**
 * <p>Description: Parses the output of b3dtomosetexts.</p>
 * 
 * <p>Copyright: Copyright 2021 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class TomosetextsOutput {
  private final String output;

  TomosetextsOutput(final String[] stdout) {
    if (stdout != null && stdout.length > 0) {
      output = stdout[0];
    }
    else {
      output = null;
    }
  }

  public String toString() {
    return "[output:" + output + "]";
  }

  /**
   * Get the image filename style from the dataset
   * @return
   */
  public ImageFilenameStyle getImageFilenameStyle() {
    if (output == null) {
      return null;
    }
    String[] split = output.split("\\s+");
    if (split != null && split.length > 0) {
      return ImageFilenameStyle.getInstance(split[0], false);
    }
    return null;
  }
}