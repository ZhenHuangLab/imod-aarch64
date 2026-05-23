package etomo.storage;

import java.io.FileNotFoundException;
import java.io.IOException;

import etomo.storage.LogFile.Id;
import etomo.storage.LogFile.LockException;
import etomo.storage.LogFile.ReaderId;
import etomo.storage.LogFile.WriterId;

/**
 * <p>Description:An interface for LogFile.  Allows testing of classes that use LogFile
 * without having to create a file.</p>
 * 
 * <p>Copyright: Copyright 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
interface LogFileInterface {
  public boolean exists();

  public ReaderId openReader() throws LockException, FileNotFoundException;

  public String readLine(ReaderId readId) throws LockException, IOException;

  public boolean closeRead(Id readId);

  public boolean backup() throws LockException;

  public WriterId openWriter() throws LockException, IOException;

  public void write(String string, WriterId writerId) throws LockException, IOException;

  public boolean closeWriter(WriterId writerId);
}
