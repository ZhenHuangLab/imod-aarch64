package etomo.type;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2010</p>
 *
 * <p>Organization:
 * Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
 * University of Colorado</p>
 * 
 * @author $Author$
 * 
 * @version $Revision$
 * 
 * <p> $Log$ </p>
 */
public final class ToolType {
  public static final String rcsid =
    "$Id$";

  public static final ToolType FLATTEN_VOLUME = new ToolType("Flatten Volume", "flatten");
  public static final ToolType GPU_TILT_TEST = new ToolType("GPU Test", "gpu");

  private final String string, lowerCaseIdString;

  private ToolType(final String string, final String lowerCaseIdString) {
    this.string = string;
    this.lowerCaseIdString = lowerCaseIdString;
  }

  public boolean equals(final String inputString) {
    if (inputString == null) {
      return false;
    }
    if (inputString.equals(string)) {
      return true;
    }
    String lcInputString = inputString.toLowerCase();
    if (lcInputString == null) {
      return false;
    }
    return lcInputString.indexOf(lowerCaseIdString) != -1;
  }

  public String toString() {
    return string;
  }
}
