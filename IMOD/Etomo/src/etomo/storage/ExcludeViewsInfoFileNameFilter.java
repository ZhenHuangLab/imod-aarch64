package etomo.storage;

import java.io.File;
import java.io.FilenameFilter;

import etomo.type.AxisID;
import etomo.type.FileType;

/**
* <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
*
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public final class ExcludeViewsInfoFileNameFilter implements FilenameFilter {
  private final String datasetName;

  private AxisID axisID = null;

  public ExcludeViewsInfoFileNameFilter(final String datasetName) {
    this.datasetName = datasetName;
  }

  public void setAxisID(final AxisID axisID) {
    this.axisID = axisID;
  }

  @Override
  public boolean accept(File file, String name) {
    if (name == null) {
      return false;
    }
    FileType fileType = FileType.EXCLUDE_VIEWS_INFO;
    String leftRegExp = "\\Q" + datasetName
      + (axisID != null ? axisID.getExtension() : "") + fileType.getTypeString() + "\\E";
    String rightRegExp = "\\Q" + fileType.getExtension() + "\\E";
    // midzone2a_cutviews2.info
    if (name.matches(leftRegExp + "\\d" + rightRegExp)) {
      return true;
    }
    // midzone2b_cutviews10.info
    if (name.matches(leftRegExp + "\\d\\d" + rightRegExp)) {
      return true;
    }
    return false;
  }
}
