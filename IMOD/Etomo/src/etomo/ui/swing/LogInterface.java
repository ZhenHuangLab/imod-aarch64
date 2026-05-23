package etomo.ui.swing;

import java.io.File;
import java.util.ArrayList;

import javax.swing.text.BadLocationException;

import etomo.storage.Loggable;
import etomo.type.AxisID;

/**
 * <p>Description: An interface for anything that can act as a log display.
 * Used by classes that have messages to log.  Also used by EtomoLogger which is
 * a utility for LogInterface classes.
 * 
 * <p>Copyright: Copyright 2010 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.2  2011/02/22 18:14:05  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.1  2010/02/17 04:56:35  sueh
 * <p> bug# 1301 An interface for classes that receive messages to log.
 * <p> </p>
 */
public interface LogInterface {
  public void logMessage(String title, AxisID axisID, String[] message);

  public void logMessage(String title, AxisID axisID, ArrayList<String> message);

  public void logMessage(AxisID axisID, ArrayList<String> message);

  public void logMessage(Loggable loggable, AxisID axisID);

  public void logMessage(String title, AxisID axisID);

  public void logMessage(String message);

  public void logMessage(String message, boolean timestamp, boolean newline);

  public void logMessage(File file);

  public void logMessage(File file, boolean newline);

  public void save();

  // Functions used by EtomoLogger

  public void append(String line);

  public void msgChanged();

  public int getLineEndOffset() throws BadLocationException;
}
