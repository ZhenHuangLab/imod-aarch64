package etomo.type;

import java.io.File;

import etomo.BaseManager;
import etomo.process.ImodManager;

/**
 * <p>Description: A type of file associated with a process-level panel.</p>
 * 
 * <p>Copyright: Copyright 2008 - 2015 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.1  2009/06/05 02:05:14  sueh
 * <p> bug# 1219 A class to help other classes specify files without knowing very
 * <p> much about them.
 * <p> </p>
 */
public final class ImageFileType {
  public static final ImageFileType TRIM_VOL_OUTPUT =
    new ImageFileType(ImodManager.TRIMMED_VOLUME_KEY);
  public static final ImageFileType SQUEEZE_VOL_OUTPUT =
    new ImageFileType(ImodManager.SQUEEZED_VOLUME_KEY);
  public static final ImageFileType FLATTEN_OUTPUT =
    new ImageFileType(ImodManager.FLAT_VOLUME_KEY);

  private final String imodManagerKey;

  private ImageFileType(String imodManagerKey) {
    this.imodManagerKey = imodManagerKey;
  }

  public String getImodManagerKey() {
    return imodManagerKey;
  }

  public String getFileName(BaseManager manager) {
    if (manager == null) {
      return null;
    }
    FileType fileType = getFileType();
    if (fileType == null) {
      return null;
    }
    return fileType.getFileName(manager, null);
  }

  public File getFile(BaseManager manager) {
    if (manager == null) {
      return null;
    }
    FileType fileType = getFileType();
    if (fileType == null) {
      return null;
    }
    return fileType.getFile(manager, null);
  }

  public FileType getFileType() {
    if (this == TRIM_VOL_OUTPUT) {
      return FileType.TRIM_VOL_OUTPUT;
    }
    if (this == SQUEEZE_VOL_OUTPUT) {
      return FileType.SQUEEZE_VOL_OUTPUT;
    }
    if (this == FLATTEN_OUTPUT) {
      return FileType.FLATTEN_OUTPUT;
    }
    return null;
  }

  public String toString() {
    return imodManagerKey;
  }
}
