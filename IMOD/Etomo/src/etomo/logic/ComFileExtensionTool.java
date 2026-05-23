package etomo.logic;

/**
 * <p>Description: The plan is to replace the current comfile extension (.com) with
 * something less problematic.  Use this library to generically work with the comfile
 * extension.  Will be modified to handle changes, including a transition period.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class ComFileExtensionTool {
  private static final String EXTENSION = ".com";

  private ComFileExtensionTool() {}

  public static int lastIndexOf(final String str, final int fromIndex) {
    if (str == null) {
      return -1;
    }
    return str.lastIndexOf(EXTENSION, fromIndex);
  }
}
