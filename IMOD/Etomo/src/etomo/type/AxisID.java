package etomo.type;

import etomo.util.Utilities;

/**
 * <p>Description: </p>
 *
* <p>Copyright: Copyright 2002 - 2017 by the Regents of the University of Colorado</p>
*
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
* 
 * <p> $Log$
 * <p> Revision 3.9  2006/06/29 16:52:53  sueh
 * <p> bug# 888 getInstance():  if a null string is passed, return null.
 * <p>
 * <p> Revision 3.8  2006/04/28 20:55:37  sueh
 * <p> bug# 787 Added getInstance(String).
 * <p>
 * <p> Revision 3.7  2005/07/18 17:54:08  sueh
 * <p> bug# 692 Removed selftest function in AxisID because there are too many
 * <p> situations where it is valid for it to fail.  Remove
 * <p> AxisID.getStorageExtension() because it is the same as getExtension
 * <p> without the call to the selftest function.
 * <p>
 * <p> Revision 3.6  2005/06/21 00:46:31  sueh
 * <p> bug# 962 selfTestGetExtension() can be private.
 * <p>
 * <p> Revision 3.5  2005/06/20 16:51:04  sueh
 * <p> bug# 692 Moved selftest convenience variable to util.Utilities.  Make
 * <p> selfTest function level.
 * <p>
 * <p> Revision 3.4  2005/06/16 19:59:09  sueh
 * <p> bug# 692 Making self test variables boolean instead of EtomoBoolean2 to
 * <p> avoid test problems.
 * <p>
 * <p> Revision 3.3  2005/06/10 23:05:12  sueh
 * <p> bug# 671 Modified self test so it wouldn't check the EtomoDirector
 * <p> selftest setting over and over.
 * <p>
 * <p> Revision 3.2  2005/06/06 16:49:39  sueh
 * <p> bug# 671 Calling EtomoDirector.getInstance() in AxisID prior to running
 * <p> AxisID() is causing a NoClassDefFoundError when running JUnit.  Change
 * <p> code to call EtomoDirector.getInstance the first time getExtension is run.
 * <p>
 * <p> Revision 3.1  2005/06/03 20:56:29  sueh
 * <p> bug# 671 Added selfTest() to make sure that AxisID is always set to
 * <p> ONLY when the AxisType is single.  This is an issue for file names, so
 * <p> this test should only be performed when getExtension() is called.  Added
 * <p> getStorageExtension() which does the same thing but doesn't call
 * <p> selfTest().  This function is used to to get the extension for storage, when
 * <p> the "a" is used for the first axis regardless of the axis type.
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:01  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.0  2003/01/24 20:30:31  rickg
 * <p> Single window merge to main branch
 * <p>
 * <p> Revision 1.1.2.1  2003/01/24 18:37:54  rickg
 * <p> Single window GUI layout initial revision
 * <p>
 * <p> Revision 1.1  2002/09/09 22:57:02  rickg
 * <p> Initial CVS entry, basic functionality not including combining
 * <p> </p>
 */
public class AxisID {
  private static final String ONLY_EXT_STRING = "";
  private static final String FIRST_EXT_STRING = "a";
  private static final String SECOND_EXT_STRING = "b";

  private final String name;
  private final int axisOfExtension;// batchruntomo parameter based on the axis ID.

  private AxisID(String name, final int axisOfExtension) {
    this.name = name;
    this.axisOfExtension = axisOfExtension;
  }

  public static final AxisID ONLY = new AxisID("Only", 0);
  public static final AxisID FIRST = new AxisID("First", 1);
  public static final AxisID SECOND = new AxisID("Second", 2);

  public void dumpState() {
    System.err.println("[name:" + name + "]");
  }

  public int getAxisOfExtension() {
    return axisOfExtension;
  }

  /**
   * Returns a string representation of the object.
   */
  public String toString() {
    return name;
  }

  /**
   * Get the axis ID from the axis type and a file name.  The axis type takes precedence
   * so return A if its dual axis but the file name is wrong.
   * @param axisType
   * @param filename file name or path
   * @return
   */
  public static AxisID getInstanceFromFileName(final AxisType axisType,
    final String fileName) {
    if (axisType == AxisType.SINGLE_AXIS) {
      return ONLY;
    }
    if (fileName == null) {
      // Assume A axis if not information
      return FIRST;
    }
    // Strip off the extension.
    String leftSide = Utilities.removeExtension(fileName);
    if (leftSide != null && leftSide.endsWith(SECOND_EXT_STRING)) {
      return SECOND;
    }
    return FIRST;
  }

  public static AxisID getInstance(String extension) {
    if (extension == null) {
      return null;
    }
    if (extension.equals(ONLY_EXT_STRING)) {
      return ONLY;
    }
    if (extension.equals(FIRST_EXT_STRING)) {
      return FIRST;
    }
    if (extension.equals(SECOND_EXT_STRING)) {
      return SECOND;
    }
    return null;
  }

  public static AxisID getInstanceIgnoreCase(String extension) {
    if (extension == null) {
      return null;
    }
    if (extension.equalsIgnoreCase(ONLY_EXT_STRING)) {
      return ONLY;
    }
    if (extension.equalsIgnoreCase(FIRST_EXT_STRING)) {
      return FIRST;
    }
    if (extension.equalsIgnoreCase(SECOND_EXT_STRING)) {
      return SECOND;
    }
    return null;
  }

  public AxisID getOtherAxisID() {
    if (this == ONLY) {
      return null;
    }
    if (this == FIRST) {
      return SECOND;
    }
    if (this == SECOND) {
      return FIRST;
    }
    return null;
  }

  /**
   * Returns true if this instance is the same axis.  AxisID.SECOND is the B axis.
   * Everything else is the A axis (including null).
   * @param axisID
   * @return
   */
  public boolean isSameAxis(final AxisID axisID) {
    if (this == AxisID.SECOND) {
      return axisID == AxisID.SECOND;
    }
    return axisID != AxisID.SECOND;
  }

  public static AxisID getInstance(char extension) {
    if (FIRST_EXT_STRING.charAt(0) == extension) {
      return FIRST;
    }
    if (SECOND_EXT_STRING.charAt(0) == extension) {
      return SECOND;
    }
    return null;
  }

  /**
   * Returns the extension associated with the specific AxisID.  Used for
   * creating file names.
   */
  public String getExtension() {
    if (this == ONLY) {
      return ONLY_EXT_STRING;
    }
    if (this == FIRST) {
      return FIRST_EXT_STRING;
    }
    if (this == SECOND) {
      return SECOND_EXT_STRING;
    }
    return "ERROR";
  }

  public static int getExtensionLength() {
    return 1;
  }

  /**
   * @return A for only and first, otherwise B
   */
  public String getCapitalizedKeyString() {
    if (this == ONLY) {
      // Generallly only is retrieved the same way as first.
      return "A";
    }
    if (this == FIRST) {
      return "A";
    }
    if (this == SECOND) {
      return "B";
    }
    return "ERROR";
  }

  /**
   * Takes a string representation of an AxisID type and returns the correct
   * static object.  The string is case insensitive.  Null is returned if the
   * string is not one of the possibilities from toString().
   */
  public static AxisID fromString(String name) {
    if (name.compareToIgnoreCase(ONLY.toString()) == 0) {
      return ONLY;
    }
    if (name.compareToIgnoreCase(FIRST.toString()) == 0) {
      return FIRST;
    }
    if (name.compareToIgnoreCase(SECOND.toString()) == 0) {
      return SECOND;
    }

    return null;
  }
}
