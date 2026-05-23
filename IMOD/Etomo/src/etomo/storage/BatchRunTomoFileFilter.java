package etomo.storage;

import java.io.File;

import etomo.type.DataFileType;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2013</p>
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
public final class BatchRunTomoFileFilter {
  public static final String rcsid = "$Id:$";

  /**
   * returns true if a file is a batch run tomo data file
   * File must be in the form:  {rootname}.ebrt
   * Example: tilta.ebrt
   */
  public boolean accept(File file) {
    if (file.isDirectory()) {
      return true;
    }
    String fileName = file.getName();
    if (fileName.endsWith(DataFileType.BATCH_RUN_TOMO.extension) && fileName.length() > 4) {
      return true;
    }
    return false;
  }
}
