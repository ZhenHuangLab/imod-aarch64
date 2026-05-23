package etomo.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import etomo.type.AxisID;
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
public final class AutofidseedLog implements Loggable {
  private final ArrayList<String> lineList = new ArrayList<String>();

  private final String userDir;
  private final AxisID axisID;

  private AutofidseedLog(String userDir, AxisID axisID) {
    this.userDir = userDir;
    this.axisID = axisID;
  }

  /**
   * @param userDir
   * @param axisID
   * @param processName
   * @return
   */
  public static AutofidseedLog getInstance(String userDir, AxisID axisID) {
    return new AutofidseedLog(userDir, axisID);
  }

  public String getName() {
    return ProcessName.AUTOFIDSEED.toString();
  }

  /**
   * Get a message to be logged in the LogPanel.
   */
  public ArrayList<String> getLogMessage()
    throws LogFile.LockException, FileNotFoundException, IOException {
    lineList.clear();
    // refresh the log file
    LogFile log = LogFile.getInstance(userDir, axisID, ProcessName.AUTOFIDSEED);
    if (log.exists()) {
      LogFile.ReaderId readerId = log.openReader();
      if (readerId != null && !readerId.isEmpty()) {
        String line = log.readLine(readerId);
        while (line != null) {
          if (line.indexOf("candidate points") != -1) {
            lineList.add(line);
          }
          else if (line.trim().startsWith("Final:")) {
            lineList.add(line);
          }
          // Look for "Tracking parameters adjusted for new unbinned bead size" message
          else if (line.indexOf("AFS1") != -1) {
            lineList.add(line);
          }
          line = log.readLine(readerId);
        }
        log.closeRead(readerId);
        return lineList;
      }
    }
    return lineList;
  }

  public boolean isTrackingAdjusted()
    throws LogFile.LockException, FileNotFoundException, IOException {
    // refresh the log file
    LogFile log = LogFile.getInstance(userDir, axisID, ProcessName.AUTOFIDSEED);
    if (log.exists()) {
      LogFile.ReaderId readerId = log.openReader();
      if (readerId != null && !readerId.isEmpty()) {
        String line = null;
        while ((line = log.readLine(readerId)) != null) {
          if (line.indexOf("AFS1") != -1 || line.indexOf("AFS2") != -1
            || line.indexOf("AFS3") != -1 || line.indexOf("AFS4") != -1) {
            log.closeRead(readerId);
            return true;
          }
        }
        log.closeRead(readerId);
      }
    }
    return false;
  }
}
