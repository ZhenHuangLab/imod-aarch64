package etomo.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import etomo.BaseManager;
import etomo.type.AxisID;
import etomo.type.FileType;
import etomo.ui.swing.UIHarness;

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
public final class DirectiveDescrFile {
  public static final String rcsid = "$Id:$";

  private LogFile logFile = null;
  private File alternateFile = null;
  private Map<String, DirectiveDescrElement> directiveMap = null;

  public static final DirectiveDescrFile INSTANCE = new DirectiveDescrFile();

  private DirectiveDescrFile() {}

  public void releaseIterator(Iterator iterator) {
    if (logFile != null) {
      logFile.closeRead(iterator.id);
    }
    if (!logFile.isOpen()) {
      logFile = null;
    }
  }

  /**
   * Sets an alternative directives description file.  Pass null to remove the alternative
   * file.  Has no effect while the log file is in use.
   * @param input
   * @return true if alternateFile
   */
  boolean setFile(final File input) {
    if (input == null) {
      alternateFile = null;
    }
    else if (logFile != null) {
      System.err.println("Warning: unable to use " + input.getAbsolutePath()
          + " as the directives description file because the default file is already "
          + "in use.");
      return false;
    }
    alternateFile = input;
    return true;
  }

  /**
   * Opens the directives description file if necessary and returns an iterator for the
   * file.  When done with the iterator, call releaseIterator.
   * @param manager
   * @param axisID
   * @return
   */
  public Iterator getIterator(final BaseManager manager, final AxisID axisID) {
    if (logFile == null) {
      if (!open(manager, axisID)) {
        return null;
      }
    }
    try {
      return new Iterator(manager, axisID, logFile, logFile.openReader());
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
      UIHarness.INSTANCE.openMessageDialog(manager,
          "Unable to get a reader for "
              + FileType.DIRECTIVES_DESCR.getFile(manager, axisID).getAbsolutePath()
              + ".\n" + e.getMessage(),
          "Open File Failed", axisID);
      return null;
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
      UIHarness.INSTANCE.openMessageDialog(manager,
          "Unable to get a reader for "
              + FileType.DIRECTIVES_DESCR.getFile(manager, axisID).getAbsolutePath()
              + ".\n" + e.getMessage(),
          "Open File Failed", axisID);
      return null;
    }
  }

  private synchronized boolean open(final BaseManager manager, final AxisID axisID) {
    if (logFile != null) {
      return true;
    }
    File file;
    if (alternateFile == null) {
      file = FileType.DIRECTIVES_DESCR.getFile(manager, axisID);
    }
    else {
      file = alternateFile;
    }
    try {
      logFile = LogFile.getInstance(file);
      return true;
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
      UIHarness.INSTANCE.openMessageDialog(manager,
          "Unable to open " + file.getAbsolutePath() + ".\n" + e.getMessage(),
          "Open File Failed", axisID);
      return false;
    }
  }

  /**
   * Returns the description of a directive.
   * @param key - matches the first column of the directive.cvs file
   * @return
   */
  DirectiveDescrElement get(final String key) {
    if (directiveMap == null) {
      synchronized (this) {
        if (directiveMap == null) {
          directiveMap = new Hashtable<String, DirectiveDescrElement>();
          Iterator iterator = getIterator(null, null);
          while (iterator.hasNext()) {
            DirectiveDescrElement element = iterator.nextElement();
            if (element.isDirective()) {
              directiveMap.put(element.getName(),
                  new DirectiveDescrElement(element.getLineArray()));
            }
          }
          releaseIterator(iterator);
        }
      }
    }
    return directiveMap.get(key);
  }

  /**
   * Readonly iterator
   * @author sueh
   */
  public static final class Iterator {
    // Only one instance used - don't hang onto it
    private final DirectiveDescrElement curElement = new DirectiveDescrElement();

    private final AxisID axisID;
    private final LogFile.ReaderId id;
    private final LogFile logFile;
    private final BaseManager manager;

    private String nextLine = null;

    private Iterator(final BaseManager manager, final AxisID axisID,
        final LogFile logFile, final LogFile.ReaderId id) {
      this.manager = manager;
      this.axisID = axisID;
      this.logFile = logFile;
      this.id = id;
    }

    /**
     * Moves to the next line if nextLine is null.
     * @return true if there is another line to read and nextLine has been set
     */
    public boolean hasNext() {
      // hasNext has been run, but next was not, so line isn't incremented.
      if (nextLine != null) {
        return true;
      }
      try {
        nextLine = logFile.readLine(id);
        return nextLine != null;
      }
      catch (LogFile.LockException e) {
        e.printStackTrace();
        UIHarness.INSTANCE.openMessageDialog(manager,
            "Unable to read " + FileType.DIRECTIVES_DESCR.getFile(manager, axisID)
                .getAbsolutePath() + ".\n" + e.getMessage(),
            "Open File Failed", axisID);
        return false;
      }
      catch (IOException e) {
        e.printStackTrace();
        UIHarness.INSTANCE.openMessageDialog(manager,
            "Unable to read " + FileType.DIRECTIVES_DESCR.getFile(manager, axisID)
                .getAbsolutePath() + ".\n" + e.getMessage(),
            "Open File Failed", axisID);
        return false;
      }
    }

    /**
     * Increment the line, and null out nextLine
     */
    public String[] next() {
      if (hasNext()) {
        String[] lineArray = nextLine.split("\\s*,\\s*");
        nextLine = null;
        return lineArray;
      }
      return null;
    }

    public DirectiveDescrElement nextElement() {
      String[] lineArray = next();
      if (lineArray != null) {
        curElement.setLineArray(lineArray);
        return curElement;
      }
      return null;
    }
  }
}
