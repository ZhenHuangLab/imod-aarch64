package etomo.ui.swing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import etomo.storage.LogFile;
import etomo.storage.Loggable;
import etomo.type.AxisID;
import etomo.util.Utilities;

/**
 * <p>Description: Uses SwingUtilities.invokeLater to add timestamps and lines
 * to a LogInterface.</p>
 * 
 * <p>Copyright: Copyright 2010 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.2  2011/02/22 18:08:09  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.1  2010/02/17 04:54:36  sueh
 * <p> bug# 1301 Logging functionaity shared by LogPanel and ToolsDialog.
 * <p> </p>
 */
final class EtomoLogger {
  private final LogInterface logInterface;

  EtomoLogger(final LogInterface logInterface) {
    this.logInterface = logInterface;
  }

  synchronized void loadMessages(ArrayList<String> lineList)
    throws LogFile.LockException, IOException {
    SwingUtilities.invokeLater(new AppendLater(true, lineList));
  }

   void logMessage(String line1, String line2) {
    SwingUtilities
      .invokeLater(new AppendLater(Utilities.getDateTimeStamp(), line1, line2));
  }

   void logMessage(Loggable loggable, AxisID axisID) {
    if (loggable == null) {
      return;
    }
    try {
      SwingUtilities.invokeLater(new AppendLater(Utilities.getDateTimeStamp(), loggable
        .getName(), loggable.getLogMessage(), axisID));
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
      SwingUtilities
        .invokeLater(new AppendLater("Unable to log message:", e.getMessage()));
    }
    catch (IOException e) {
      e.printStackTrace();
      SwingUtilities
        .invokeLater(new AppendLater("Unable to log message:", e.getMessage()));
    }
  }

   void logMessage(String title, AxisID axisID, String[] message) {
    SwingUtilities.invokeLater(new AppendLater(Utilities.getDateTimeStamp(), title,
      message));
  }

   void logMessage(String title, AxisID axisID, ArrayList<String> message) {
    SwingUtilities.invokeLater(new AppendLater(Utilities.getDateTimeStamp(), title,
      message, axisID));
  }

   void logMessage(final AxisID axisID, final ArrayList<String> message) {
    SwingUtilities.invokeLater(new AppendLater(Utilities.getDateTimeStamp(), message,
      axisID));
  }

  public void logMessage(final String title, final AxisID axisID) {
    SwingUtilities.invokeLater(new AppendLater(Utilities.getDateTimeStamp(), title,
      axisID));
  }

   void logMessage(final String message) {
    SwingUtilities.invokeLater(new AppendLater(Utilities.getDateTimeStamp(), message));
  }

   void logMessage(final String message, final boolean timestamp,
    final boolean newline) {
    SwingUtilities.invokeLater(new AppendLater((timestamp ? Utilities.getDateTimeStamp()
      : null), message, newline));
  }

   void logMessage(final File file) {
    SwingUtilities.invokeLater(new AppendLater(file));
  }

  public void logMessage(final File file, final boolean newline) {
    SwingUtilities.invokeLater(new AppendLater(file, newline));
  }

  private final class AppendLater implements Runnable {
    boolean loadingFromFile = false;
    private String timestamp = null;
    private String line1 = null;
    private String line2 = null;
    private String[] stringArray = null;
    private ArrayList<String> lineList = null;
    private File file = null;
    private AxisID axisID = null;
    private boolean newline = true;// prevents excessive empty lines

    private AppendLater(final String timestamp, final String line1, final boolean newline) {
      this.timestamp = timestamp;
      this.line1 = line1;
      this.newline = newline;
    }

    private AppendLater(final String timestamp, final String line1) {
      this.timestamp = timestamp;
      this.line1 = line1;
    }

    private AppendLater(final String timestamp, final String line1, final AxisID axisID) {
      this.timestamp = timestamp;
      this.line1 = line1;
      this.axisID = axisID;
    }

    private AppendLater(final String timestamp, final String line1, final String line2) {
      this.timestamp = timestamp;
      this.line1 = line1;
      this.line2 = line2;
    }

    private AppendLater(final String timestamp, final String line1,
      final String[] stringArray) {
      this.timestamp = timestamp;
      this.line1 = line1;
      this.stringArray = stringArray;
    }

    private AppendLater(final String timestamp, final ArrayList<String> lineList,
      final AxisID axisID) {
      this.timestamp = timestamp;
      this.lineList = lineList;
      this.axisID = axisID;
    }

    private AppendLater(final String timestamp, final String line1,
      final ArrayList<String> lineList, final AxisID axisID) {
      this.timestamp = timestamp;
      this.line1 = line1;
      this.lineList = lineList;
      this.axisID = axisID;
    }

    private AppendLater(final boolean loadingFromFile, final ArrayList<String> lineList) {
      this.loadingFromFile = loadingFromFile;
      this.lineList = lineList;
    }

    private AppendLater(final File file) {
      this.file = file;
    }

    private AppendLater(final File file, final boolean newline) {
      this.file = file;
      this.newline = newline;
    }

    /**
     * Append lines and lineList to textArea.
     */
    public void run() {
      if (newline) {
        newLine(null);
      }
      if (line1 != null) {
        logInterface.append(line1 + (timestamp != null ? " - " + timestamp : ""));
        newLine(line1);
      }
      else if (timestamp != null) {
        logInterface.append(timestamp);
      }
      if (line2 != null) {
        logInterface.append(line2);
        newLine(line2);
      }
      if (stringArray != null) {
        for (int i = 0; i < stringArray.length; i++) {
          if (stringArray[i] != null) {
            logInterface.append((String) stringArray[i]);
            newLine(stringArray[i]);
          }
        }
      }
      if (lineList != null) {
        int len = lineList.size();
        for (int i = 0; i < len; i++) {
          String line = lineList.get(i);
          if (line != null) {
            logInterface.append(line);
            newLine(line);
          }
        }
      }
      if (file != null && file.exists() && file.isFile() && file.canRead()) {
        logInterface.append("Logging from file: " + file.getAbsolutePath());
        newLine(null);
        try {
          LogFile logFile = LogFile.getInstance(file);
          LogFile.ReaderId id = logFile.openReader();
          String line = null;
          while ((line = logFile.readLine(id)) != null) {
            logInterface.append(line);
            newLine(line);
          }
        }
        catch (LogFile.LockException e) {
          e.printStackTrace();
          System.err.println("Unable to log from file.  " + e.getMessage());
        }
        catch (FileNotFoundException e) {
          e.printStackTrace();
          System.err.println("Unable to log from file.  " + e.getMessage());
        }
        catch (IOException e) {
          e.printStackTrace();
          System.err.println("Unable to log from file.  " + e.getMessage());
        }
      }
      if (!loadingFromFile) {
        logInterface.msgChanged();
      }
      if (axisID == AxisID.FIRST || axisID == AxisID.SECOND) {
        logInterface.append(axisID + " axis");
        newLine(null);
      }
    }

    /**
     * Appends a newline character if the last line in the text area is not empty
     * Put a null in previousLine to force a new line.
     */
    private void newLine(final String previousLine) {
      try {
        // messages should be alone on a line
        int lastLineEndOffset = logInterface.getLineEndOffset();
        if (lastLineEndOffset != 0
          && (previousLine == null || !previousLine.endsWith("\n"))) {
          logInterface.append("\n");
        }
      }
      catch (BadLocationException e) {
        e.printStackTrace();
      }
    }
  }
}
