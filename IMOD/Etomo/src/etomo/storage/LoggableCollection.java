package etomo.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
public final class LoggableCollection implements Loggable {
  private final List<Loggable> loggableList = new ArrayList<Loggable>();

  public void addLoggable(final Loggable loggable) {
    loggableList.add(loggable);
  }

  /**
   * Returns the name of the first Loggable in the collection.  Returns an empty string
   * if the collection is empty.
   */
  public String getName() {
    if (loggableList.isEmpty()) {
      return "";
    }
    return loggableList.get(0).getName();
  }

  public ArrayList<String> getLogMessage() throws LogFile.LockException, FileNotFoundException,
      IOException {
    ArrayList<String> logMessageList = new ArrayList<String>();
    Iterator<Loggable> i = loggableList.iterator();
    while (i.hasNext()) {
      logMessageList.addAll(i.next().getLogMessage());
    }
    return logMessageList;
  }
}
