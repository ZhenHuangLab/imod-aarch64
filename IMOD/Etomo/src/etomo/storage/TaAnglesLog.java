package etomo.storage;

import java.io.FileNotFoundException;
import java.io.IOException;

import etomo.ApplicationManager;
import etomo.process.AlignLogGenerator;
import etomo.type.AxisID;
import etomo.type.ConstEtomoNumber;
import etomo.type.EtomoNumber;
import etomo.type.FileType;

/**
 * <p>Description: Class for reading the tasurfaceangles.log file.</p>
 * 
 * <p>Copyright: Copyright 2009 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.4  2010/03/19 21:59:34  sueh
 * <p> bug# 1335 Class can't be a n'ton because the dataset tabs.
 * <p>
 * <p> Revision 1.3  2010/02/17 04:49:31  sueh
 * <p> bug# 1301 Using the manager instead of the manager key do pop up
 * <p> messages.
 * <p>
 * <p> Revision 1.2  2009/09/25 22:22:47  sueh
 * <p> bug# 1272 In get... handle line == null.
 * <p>
 * <p> Revision 1.1  2009/09/01 03:18:06  sueh
 * <p> bug# 1222
 * <p>
 */
public final class TaAnglesLog {
  private static final String CENTER_TO_CENTER_THICKNESS_TAG =
    "Unbinned thickness needed to contain centers of all fiducials";

  private final String userDir;
  private final ApplicationManager manager;
  private final AxisID axisID;

  private TaAnglesLog(final String userDir, final ApplicationManager manager,
    final AxisID axisID) {
    this.userDir = userDir;
    this.manager = manager;
    this.axisID = axisID;
  }

  /**
   * @param userDir
   * @param axisID
   * @param processName
   * @return
   */
  public static TaAnglesLog getInstance(String userDir, final ApplicationManager manager,
    AxisID axisID) {
    TaAnglesLog instance = new TaAnglesLog(userDir, manager, axisID);
    instance.createLog();
    return instance;
  }

  private void createLog() {
    FileType taLog = FileType.ALIGN_ANGLES_LOG;
    if (!taLog.exists(manager, axisID)
      || taLog.lastModified(manager, axisID) < FileType.TILT_ALIGN_LOG
        .lastModified(manager, axisID)) {
      manager.generateAlignLogs(axisID);
    }
  }

  /**
   * Get center to center thickness from the log.
   */
  public ConstEtomoNumber getCenterToCenterThickness() throws LogFile.LockException,
    FileNotFoundException, IOException {
    EtomoNumber centerToCenterThickness = new EtomoNumber(EtomoNumber.Type.DOUBLE);
    // refresh the log file
    LogFile taAnglesLog =
      LogFile.getInstance(userDir, axisID, AlignLogGenerator.ANGLES_LOG_NAME);
    if (taAnglesLog.exists()) {
      LogFile.ReaderId readerId = taAnglesLog.openReader();
      if (readerId != null && !readerId.isEmpty()) {
        String line = taAnglesLog.readLine(readerId);
        while (line != null) {
          line = line.trim();
          if (line
            .startsWith("Unbinned thickness needed to contain centers of all fiducials")) {
            String[] stringArray = line.split("\\s+");
            centerToCenterThickness.set(stringArray[10]);
            taAnglesLog.closeRead(readerId);
            return centerToCenterThickness;
          }
          line = taAnglesLog.readLine(readerId);
        }
        taAnglesLog.closeRead(readerId);
      }
    }
    return centerToCenterThickness;
  }

  /**
   * Get incremental shift to center from the log.
   */
  public ConstEtomoNumber getIncrementalShiftToCenter() throws LogFile.LockException,
    FileNotFoundException, IOException {
    EtomoNumber incrementalShiftToCenter = new EtomoNumber(EtomoNumber.Type.DOUBLE);
    // refresh the log file
    LogFile taAnglesLog =
      LogFile.getInstance(userDir, axisID, AlignLogGenerator.ANGLES_LOG_NAME);
    if (taAnglesLog.exists()) {
      LogFile.ReaderId readerId = taAnglesLog.openReader();
      if (readerId != null && !readerId.isEmpty()) {
        String line = taAnglesLog.readLine(readerId);
        while (line != null) {
          line = line.trim();
          if (line
            .startsWith("Incremental unbinned shift needed to center range of fiducials in Z")) {
            String[] stringArray = line.split("\\s+");
            incrementalShiftToCenter.set(stringArray[12]);
            taAnglesLog.closeRead(readerId);
            return incrementalShiftToCenter;
          }
          line = taAnglesLog.readLine(readerId);
        }
        taAnglesLog.closeRead(readerId);
      }
    }
    return incrementalShiftToCenter;
  }
}
