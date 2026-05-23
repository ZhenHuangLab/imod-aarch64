package etomo.comscript;

import java.io.File;

import etomo.type.AxisID;
import etomo.type.FileKey;
import etomo.type.FileType;
import etomo.type.ProcessName;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2005 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface Command {
  /**
   * The axisID of input/output/comscript files.  Use AxisID.ONLY for single axis
   * datasets.
   * @return
   */
  public AxisID getAxisID();

  /**
   * For params that can be run for different purposes with different parameters, or run
   * from different comscripts.
   * @return
   */
  public CommandMode getCommandMode();

  /**
   * For a comscript, the process name of the comscript
   * For a command line call, the process name of the process being run
   * @return
   */
  public ProcessName getProcessName();

  /**
   * For a comscript, the name of the comscript file (ProcessName.getComscript(axisID))
   * For a command line call, the name of the process being run
   * @return
   */
  public String getCommand();

  /**
   * For a comscript, the name of the comscript (ProcessName.toString)
   * For a command line call, the name of the process being run
   * @return
   */
  public String getCommandName();

  /**
   * For a comscript, the name of the comscript (ProcessName.toString)
   * For a command line call, the entire command line
   * @return
   */
  public String getCommandLine();

  /**
   * For a comscript, the comscript file (without a path)
   * For a command line call, the entire command line
   * @return
   */
  public String[] getCommandArray();

  /**
   * @return the input file parameter if available
   */
  public File getCommandInputFile();

  /**
   * @return the output file parameter if available
   */
  public File getCommandOutputFile();

  /**
   * @return the output file type if available
   * @deprecated 3/14/2019
   */
  public FileType getOutputImageFileType();

  /**
   * @return description of output file key if available
   */
  public FileKey getOutputImageFileKey();

  /**
   * @return a second output file type if available
   * @deprecated 3/14/2019
   */
  public FileType getOutputImageFileType2();

  /**
   * @return a second output file key if available
   */
  public FileKey getOutputImageFileKey2();

  /**
   * Return yes if a message from the process needs to be popped up while the process is
   * running, rather then reported after the process is finished.  A process monitor which
   * recongizes the message will be needed.  If all messages need to be popped up while
   * the process is running, use ProcessMessages.startStringFeed with a process monitor.
   * @return
   */
  public boolean isMessageReporter();

  /**
   * For a command that runs another command (like processchunks).
   * @return the process name of the other command
   */
  public String getSubcommandProcessName();

  /**
   * For a command that runs another command (like processchunks).
   * @return the Param class instance of the other command
   */
  public CommandDetails getSubcommandDetails();
}
/**
 * <p> $Log$
 * <p> Revision 1.16  2010/04/28 15:46:37  sueh
 * <p> bug# 1344 Added getOutputImageFileType functions.
 * <p>
 * <p> Revision 1.15  2010/01/11 23:49:01  sueh
 * <p> bug# 1299 Added isMessageReporter.
 * <p>
 * <p> Revision 1.14  2009/12/11 17:26:12  sueh
 * <p> bug# 1291 Added getCommandInputFile.
 * <p>
 * <p> Revision 1.13  2009/09/01 03:17:46  sueh
 * <p> bug# 1222
 * <p>
 * <p> Revision 1.12  2007/11/06 19:04:49  sueh
 * <p> bug# 1047 Added getSubcommandDetails to allow a Command to contain
 * <p> another command.  This is useful for processchunks.
 * <p>
 * <p> Revision 1.11  2007/02/05 21:34:29  sueh
 * <p> bug# 962 Changed getCommandMode to return an interface for inner Mode classes (CommandMode) instead of an int.
 * <p>
 * <p> Revision 1.10  2006/05/22 22:35:33  sueh
 * <p> bug# 577 Added getCommand().
 * <p>
 * <p> Revision 1.9  2006/05/11 19:38:30  sueh
 * <p> bug# 838 Add CommandDetails, which extends Command and
 * <p> ProcessDetails.  Changed ProcessDetails to only contain generic get
 * <p> functions.  Command contains all the command oriented functions.
 * <p>
 * <p> Revision 1.8  2006/01/20 20:45:32  sueh
 * <p> updated copyright year
 * <p>
 * <p> Revision 1.7  2005/11/19 01:50:22  sueh
 * <p> bug# 744 Moved functions only used by process manager post
 * <p> processing and error processing from Commands to ProcessDetails.
 * <p> This allows ProcesschunksParam to be passed to DetackedProcess
 * <p> without having to add unnecessary functions to it.
 * <p> </p>
 */
