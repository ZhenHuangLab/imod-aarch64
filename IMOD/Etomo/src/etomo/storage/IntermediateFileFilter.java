package etomo.storage;

import java.io.File;
import java.util.regex.Pattern;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.type.AxisID;
import etomo.type.Extension;
import etomo.type.FileType;

/**
 * <p>Description:</p>
 * 
 * <p>Copyright: Copyright 2002 - 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 3.8  2011/04/04 16:58:01  sueh
 * <p> bug# 1416 Added .alisub and .alilog10 to accept.
 * <p>
 * <p> Revision 3.7  2011/02/22 04:34:39  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 3.6  2006/07/24 14:07:42  sueh
 * <p> bug# 878 Added .dcst to the files accepted by this filter.
 * <p>
 * <p> Revision 3.5  2005/12/13 00:29:49  sueh
 * <p> bug# 618 Made the file selection for files with dashes more selective.
 * <p>
 * <p> Revision 3.4  2005/12/07 17:43:07  sueh
 * <p> bug# 618 Was not handling dashes.  Made the code that recognizes
 * <p> parallel processing file more specific.
 * <p>
 * <p> Revision 3.3  2005/10/17 23:52:54  sueh
 * <p> bug# 532 Handling intermediate files created by splittilt, splitcombine, and
 * <p> processchunks in accept().
 * <p>
 * <p> Revision 3.2  2005/04/06 21:26:34  sueh
 * <p> bug# 533 Added the .bl file to the clean up panel.
 * <p>
 * <p> Revision 3.1  2005/03/29 23:50:39  sueh
 * <p> bug# 618 Added acceptPretrimmedTomograms boolean to configure
 * <p> whether the class will show sum.rec or _full.rec.
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:01  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 1.4  2003/11/06 18:30:40  sueh
 * <p> bug321 accept(File f): added matchcheck.rec
 * <p>
 * <p> Revision 1.3  2003/04/24 17:46:54  rickg
 * <p> Changed fileset name to dataset name
 * <p>
 * <p> Revision 1.2  2003/04/17 17:19:11  rickg
 * <p> Reformat
 * <p>
 * <p> Revision 1.1  2003/04/17 05:06:06  rickg
 * <p> Initial revision
 * <p>
 */
public class IntermediateFileFilter extends ExtensionFileFilter {
  private final String datasetName;
  private final BaseManager manager;

  private boolean acceptPretrimmedTomograms = false;

  /**
   * call init to complete.
   * @param manager
   * @param datasetName
   * @param imageFilenameStyle
   */
  private IntermediateFileFilter(final BaseManager manager, String datasetName) {
    super(manager, EtomoDirector.INSTANCE.isUnitTest(), false, false);
    this.datasetName = datasetName;
    this.manager = manager;
  }

  public static IntermediateFileFilter getInstance(final BaseManager manager,
    final String datasetName) {
    IntermediateFileFilter instance = new IntermediateFileFilter(manager, datasetName);
    instance.setup();
    return instance;
  }

  /**
   * Call after construction.
   * @param manager
   * @param imageFilenameStyle
   */
  private void setup() {
    // "matchcheck.rec", ".mat" have been removed. Matchcheck.rec and matchshift.mat have
    // been gone for many years.
    // processchunk rec files are standardized
    String recSuffix =
      Extension.REC.getSuffix(manager.getBaseMetaData().getImageFilenameStyle());
    super.setup(datasetName, null, null,
      new String[] { "~", "volcombine.log", "bot" + recSuffix,
        "bot" + AxisID.FIRST.getExtension() + recSuffix,
        "bot" + AxisID.SECOND.getExtension() + recSuffix, "mid" + recSuffix,
        "mid" + AxisID.FIRST.getExtension() + recSuffix,
        "mid" + AxisID.SECOND.getExtension() + recSuffix, "top" + recSuffix,
        "top" + AxisID.FIRST.getExtension() + recSuffix,
        "top" + AxisID.SECOND.getExtension() + recSuffix, "_3dfind" + recSuffix,
        datasetName + AxisID.FIRST.getExtension() + recSuffix,
        datasetName + AxisID.SECOND.getExtension() + recSuffix },
      new Extension[] { Extension.ALI, Extension.PREALI, Extension.BL, Extension.DCST,
        Extension.ALILOG10, Extension.VSR, Extension.MAT },
      new FileType[] { FileType.PROCESSCHUNKS_REC, FileType.FULL_VSR, FileType.SUB_VSR,
        FileType.FIXED_XRAYS_STACK });
  }

  public void setAcceptPretrimmedTomograms() {
    acceptPretrimmedTomograms = true;
    String recSuffix =
      Extension.REC.getSuffix(manager.getBaseMetaData().getImageFilenameStyle());
    add("sum" + recSuffix);
    add("full" + recSuffix);
  }

  /* (non-Javadoc)
   * @see javax.swing.filechooser.FileFilter#accept(java.io.File) */
  // TODO 2206
  public boolean accept(File f) {
    if (super.accept(f)) {
      return true;
    }
    if (f.isFile() || EtomoDirector.INSTANCE.isUnitTest()) {
      // These temporary files are not standardized.
      // .rec.mat1659344 and .rec.wrp0905524
      String name = f.getName();
      if (name.matches("\\S+" + Pattern.quote(".rec.mat") + "\\S+")
        || name.matches("\\S+" + Pattern.quote(".rec.wrp") + "\\S+")) {
        return true;
      }
      // handle split... and processchunks files
      // gets standardized
      // last one should be \d+ (one or more) because it can go beyond three digits
      /*   if (name.matches(datasetName + "[ab]?-\\d\\d\\d+\\.rec")) {
        return true;
      }*/
      if (name.matches("tilt[ab]?-\\d\\d\\d+\\.log")) {
        return true;
      }
      if (name.matches("tilt[ab]?-\\d\\d\\d+\\.com")) {
        return true;
      }
      if (name.matches("volcombine[ab]?-\\d\\d\\d+\\.log")) {
        return true;
      }
      if (name.matches("volcombine[ab]?-\\d\\d\\d+\\.com")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Old version.  For testing against the modified accept.
   * @param f
   * @deprecated only for testing
   * @return
   */
  @SuppressWarnings("deprecation")
  public boolean acceptForRegressionTest(File f) {
    String[] endsWith = { "~", "matchcheck.rec", ".mat", ".ali", ".preali", "bot.rec",
      "bota.rec", "botb.rec", "mid.rec", "mida.rec", "midb.rec", "top.rec", "topa.rec",
      "topb.rec", "volcombine.log", ".bl", ".dcst", ".alilog10", ".vsr", "_3dfind.rec" };
    String[] pretrimmedTomograms = { "sum.rec", "full.rec" };
    if (f.isFile() || EtomoDirector.INSTANCE.isUnitTest()) {
      // .rec.mat1659344 and .rec.wrp0905524
      String name = f.getName();
      if (name.matches("\\S+" + Pattern.quote(".rec.mat") + "\\S+")
        || name.matches("\\S+" + Pattern.quote(".rec.wrp") + "\\S+")) {
        return true;
      }
      String path = f.getAbsolutePath();
      for (int i = 0; i < endsWith.length; i++) {
        if (path.endsWith(endsWith[i])) {
          return true;
        }
      }
      if (acceptPretrimmedTomograms) {
        for (int i = 0; i < pretrimmedTomograms.length; i++) {
          if (path.endsWith(pretrimmedTomograms[i])) {
            return true;
          }
        }
      }
      if (path.endsWith(datasetName + "a.rec")) {
        return true;
      }
      if (path.endsWith(datasetName + "b.rec")) {
        return true;
      }
      // handle split... and processchunks files
      if (name.matches(datasetName + "[ab]?-\\d\\d\\d\\.rec")) {
        return true;
      }
      if (name.matches("tilt[ab]?-\\d\\d\\d\\.log")) {
        return true;
      }
      if (name.matches("tilt[ab]?-\\d\\d\\d\\.com")) {
        return true;
      }
      if (name.matches("volcombine[ab]?-\\d\\d\\d\\.log")) {
        return true;
      }
      if (name.matches("volcombine[ab]?-\\d\\d\\d\\.com")) {
        return true;
      }
      if (accept(name, FileType.ERASED_BEADS_STACK_OLD)) {
        return true;
      }
      if (accept(name, FileType.FIXED_XRAYS_STACK_OLD)) {
        return true;
      }
      if (accept(name, FileType.CTF_CORRECTED_STACK_OLD)) {
        return true;
      }
      if (accept(name, FileType.MTF_FILTERED_STACK_OLD)) {
        return true;
      }
      if (name.matches(datasetName + "[ab]?_full\\.vsr\\d\\d")) {
        return true;
      }
      if (name.matches(datasetName + "[ab]?_sub\\.vsr\\d\\d")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if fileName matches file type.  Only works for file types that use the
   * dataset and the axisID.
   * dataset and axisID
   * @param fileName
   * @deprecated
   * @param fileType
   * @return
   */
  private boolean accept(final String fileName, final FileType fileType) {
    if (!fileType.usesDataset() || !fileType.usesAxisID()) {
      return false;
    }
    if (fileName.endsWith(fileType.getTypeString() + fileType.getExtension())) {
      return true;
    }
    return false;
  }

  /* (non-Javadoc)
   * @see javax.swing.filechooser.FileFilter#getDescription() */
  public String getDescription() {
    return "Intermediate files";
  }
}
