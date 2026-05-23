package etomo.ui.swing;

import java.awt.event.FocusEvent;
import java.io.File;

import etomo.ui.FieldType;
import etomo.ui.ValueManipulationField;
import etomo.ui.ValueManipulationListener;

/**
 * <p>Description: Manipulates values in fields.</p>
 * 
 * <p>Current functionality: In text fields it replaces an empty field with a value on
 * focus loss.  In combo boxes it causes the blank item to be removed.</p>
 * 
 * <p>Copyright: Copyright 2017 - 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class ValueManipulationExtension implements ValueManipulationListener {
  private static final String PREFIX = "....";

  private final ValueManipulationField field;

  private boolean preventBlank = false;
  private String substituteValue = null;
  private boolean limitDisplayedFilePath = false;
  private int maxFilePathSize = -1;
  private String filePath = null;
  private boolean debug = false;

  ValueManipulationExtension(final ValueManipulationField field, final boolean debug) {
    this.field = field;
    this.debug = debug;
    field.addValueManipulationListener(this);
  }

  /**
   * Limit the size of the display value.  Truncates only on the system file separator.
   * WARNING:  Really not a good idea to use this on anything that is not a file as the
   * behavior will vary depending on the operating system.
   * @see createDisplayValue
   * @param maxSize >0 to turn on limit, <=0 to turn if off
   * @param fieldType
   */
  void setLimitDisplayedFilePath(final int maxFilePathSize, final FieldType fieldType) {
    if (fieldType != FieldType.FILE) {
      this.maxFilePathSize = -1;
      limitDisplayedFilePath = false;
    }
    else {
      this.maxFilePathSize = maxFilePathSize;
      limitDisplayedFilePath = maxFilePathSize > 0;
    }
  }

  /**
   * If limitDisplayedFilePath is set, returns a limited size version of the a file's
   * absolute path, truncated on the file separator; and saves the absolute path.
   * @param file
   * @return
   */
  String createDisplayedFilePath(final File file) {
    // Get the value would normally be displayed.
    String filePath = null;
    if (file != null) {
      filePath = file.getAbsolutePath();
    }
    return createDisplayedFilePath(filePath, FieldType.FILE);
  }

  /**
   * If limitDisplayedFilePath is set, returns a limited size version of the filePath
   * parameter, truncated on the file separator; and saves the orginal filePath.
   * Warning:  This truncates of the file separator so passing anything but a file path
   * will create different results depending on the operating system.
   * @param filePath - the absolute path of a file or a file name
   * @param fieldType - must be FieldType.File or this will return the filePath parameter.
   * @return
   */
  String createDisplayedFilePath(final String filePath, final FieldType fieldType) {
    // Save the filePath if only limitFilePathSize is active and filePath is actually a
    // file path.
    if (limitDisplayedFilePath && fieldType == FieldType.FILE) {
      this.filePath = filePath;
    }
    else {
      this.filePath = null;
    }
    int filePathLen = filePath.length();
    // See if the filePath should be returned without truncation. Truncation is only done
    // on the file separator.
    if (fieldType != FieldType.FILE || !limitDisplayedFilePath || filePath == null
      || filePathLen <= maxFilePathSize || filePath.indexOf(File.separatorChar) == -1) {
      return filePath;
    }
    // Find the first separator within the area that can be retained.
    int separatorIndex = filePath.indexOf(File.separatorChar,
      filePathLen - (maxFilePathSize - PREFIX.length()));
    if (separatorIndex == -1) {
      // File name is longer then the area that can be retained. Return the file name.
      return PREFIX + filePath.substring(filePath.lastIndexOf(File.separatorChar));
    }
    return PREFIX + filePath.substring(separatorIndex);
  }

  void clearFilePath() {
    filePath = null;
  }

  /**
   * Returns the saved filePath if limitDisplayedFilePath is on and the filePath is not
   * null.  Otherwise return displayFilePath.
   * @param displayFilePath
   * @return
   */
  String getFilePath(final String displayFilePath) {
    if (!limitDisplayedFilePath || filePath == null) {
      return displayFilePath;
    }
    return filePath;
  }

  void setPreventBlank(final boolean preventBlank, final String substituteValue) {
    this.preventBlank = preventBlank;
    this.substituteValue = substituteValue;
  }

  void clearPreventBlank() {
    this.preventBlank = false;
    this.substituteValue = null;
  }

  /**
   * Sets substitueValue in field if preventBlank is true and field is empty
   * @return true if a substitution is done.
   */
  void substitute() {
    if (preventBlank && field.isEmpty() && substituteValue != null) {
      field.setText(substituteValue);
    }
  }

  public void focusLost(FocusEvent e) {
    substitute();
  }

  public void focusGained(FocusEvent e) {}
}