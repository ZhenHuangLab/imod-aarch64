package etomo.util;

import java.io.FileNotFoundException;
import java.util.concurrent.Callable;

import etomo.storage.LogFile;

/**
* <p>Description: Attempts to open a file reader until it is told to stop or gets an
* exception other then FileNotFoundException.  This loops infinitely if the file is not
* found and the stop function is never called.</p>
* 
* <p>Copyright: Copyright 2019 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public final class ReaderOpener implements Callable<LogFile.ReaderId> {
  private final LogFile logFile;

  private boolean stop = false;

  public ReaderOpener(final LogFile logFile) {
    this.logFile = logFile;
  }

  public void stop() {
    stop = true;
  }

  /**
   * 
   */
  public LogFile.ReaderId call() throws Exception {
    LogFile.ReaderId readerId = null;
    while (!stop && logFile != null && readerId == null) {
      try {
        readerId = logFile.openReader();
        if (readerId != null) {
          return readerId;
        }
      }
      catch (FileNotFoundException e) {}
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException e) {}
    }
    if (readerId == null) {
      readerId = logFile.openReader();
    }
    return readerId;
  }
}