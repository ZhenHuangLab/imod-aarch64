package etomo.comscript;

import etomo.type.FileKey;
import etomo.type.FileType;

/**
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright 2004 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 1.2  2004/08/23 23:30:12  sueh
 * <p> bug# 508 added interface for retrieving comscript name and
 * <p> watched file
 * <p> 
 * <p> Revision 1.1  2004/08/19 01:29:34  sueh
 * <p> Interface to CombineComscriptState used by
 * <p> BackgroundComscriptProcess and BackgroundSystemProgram.
 * <p> It only interfaces one object, but an object specific to one comscript
 * <p> does not belong in BackgroundComscriptProcess or
 * <p> BackgroundSystemProgram.
 * <p> </p>
 */
public interface ComscriptState {
  public int getStartCommand();

  public int getEndCommand();

  public String getCommand(int commandIndex);

  public String getWatchedFile(int commandIndex);

  public String getComscriptName();

  public String getComscriptWatchedFile();

  /**
   * @deprecated
   * @return
   */
  public FileType getOutputImageFileType();

  /**
   * @deprecated
   * @return
   */
  public FileType getOutputImageFileType2();

  /**
   * @deprecated
   * @return
   */
  public FileType getOutputImageFileType3();

  public FileKey getOutputImageFileKey();

  public FileKey getOutputImageFileKey2();

  public FileKey getOutputImageFileKey3();
}
