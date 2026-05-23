package etomo.ui.swing;

/**
 * <p>Description: Extension for allowing an extensible field to do extra validation. </p>
 * 
 * <p>Copyright: Copyright 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class ValidationExtension {
  private String locationDescr = null;
  private boolean fileOnly = false;
  private boolean fileMustExist = false;
  private boolean mustBePositive = false;
  private boolean required = false;

  ValidationExtension() {}

  void setRequired(final boolean required) {
    this.required = required;
  }

  String getLocationAddon() {
    return locationDescr != null ? " in " + locationDescr : "";
  }

  void setLocationDescr(final String locationDescr) {
    this.locationDescr = locationDescr;
  }

  void setFileOnly(final boolean fileOnly) {
    this.fileOnly = fileOnly;
  }

  void setFileMustExist(final boolean fileMustExist) {
    this.fileMustExist = fileMustExist;
  }

  final void setMustBePositive(final boolean mustBePositive) {
    this.mustBePositive = mustBePositive;
  }

  public boolean isRequired() {
    return required;
  }

  public boolean isFileOnly() {
    return fileOnly;
  }

  public boolean isFileMustExist() {
    return fileMustExist;
  }

  public boolean isMustBePositive() {
    return mustBePositive;
  }
}
