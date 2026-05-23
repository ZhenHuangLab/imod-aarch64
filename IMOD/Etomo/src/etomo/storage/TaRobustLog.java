package etomo.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import etomo.ApplicationManager;
import etomo.process.AlignLogGenerator;
import etomo.type.AxisID;
import etomo.type.FileType;
import etomo.type.ProcessName;

/**
* <p>Description: </p>
* 
 * <p>Copyright: Copyright 2012 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
* 
* <p> $Log$ </p>
*/
public final class TaRobustLog implements Loggable {
  private final ArrayList<String> lineList = new ArrayList<String>();

  private final String userDir;
  private final ApplicationManager manager;
  private final AxisID axisID;

  private TaRobustLog(String userDir, final ApplicationManager manager, AxisID axisID) {
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
  public static TaRobustLog getInstance(String userDir, final ApplicationManager manager,
    AxisID axisID) {
    TaRobustLog instance= new TaRobustLog(userDir, manager, axisID);
    instance.createLog();
    return instance;
  }

  private void createLog() {
    FileType taLog = FileType.ALIGN_ROBUST_LOG;
    if (!taLog.exists(manager, axisID)
      || taLog.lastModified(manager, axisID) < FileType.TILT_ALIGN_LOG.lastModified(
        manager, axisID)) {
      manager.generateAlignLogs(axisID);
    }
  }

  public String getName() {
    return ProcessName.ALIGN.toString();
  }

  /**
   * Get a message to be logged in the LogPanel.
   */
  public ArrayList<String> getLogMessage() throws LogFile.LockException, FileNotFoundException,
    IOException {
    lineList.clear();
    // refresh the log file
    LogFile taRobustLog =
      LogFile.getInstance(userDir, axisID, AlignLogGenerator.ROBUST_LOG_NAME);
    if (taRobustLog.exists()) {
      LogFile.ReaderId readerId = taRobustLog.openReader();
      if (readerId != null && !readerId.isEmpty()) {
        String line = taRobustLog.readLine(readerId);
        while (line != null) {
          if (line.trim().startsWith("Residual error weighted mean")
            && line.indexOf("Local area") == -1) {
            lineList.add(line);
          }
          else if (line.trim().startsWith("Weighted error local mean")) {
            lineList.add(line);
          }
          line = taRobustLog.readLine(readerId);
        }
        taRobustLog.closeRead(readerId);
      }
    }
    return lineList;
  }
}
