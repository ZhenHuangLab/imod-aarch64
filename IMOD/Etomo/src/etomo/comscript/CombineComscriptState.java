package etomo.comscript;

import etomo.type.FileKey;
import etomo.type.FileType;
import etomo.type.ProcessName;
import etomo.util.DatasetFiles;

/**
 * <p>Description: Represents the state of the combine.com.  Used to modify
 * combine.com.  Contains information about which commands will be run.
 * Also knows about watched files like patch.out.</p>
 *
 * <p>Copyright: Copyright 2004 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 1.14  2010/11/13 16:03:15  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.13  2010/05/21 00:10:36  sueh
 * <p> bug# 1374 Removed some dead code.
 * <p>
 * <p> Revision 1.12  2010/04/28 15:46:15  sueh
 * <p> bug# 1344 Added getOutputImageFileType functions.
 * <p>
 * <p> Revision 1.11  2008/01/25 18:24:13  sueh
 * <p> bug# 1069 In initializeComscriptMatchString made the word boundary optional to
 * <p> work with Java 1.6.
 * <p>
 * <p> Revision 1.10  2007/12/26 22:11:32  sueh
 * <p> bug# 1052 Moved argument handling from EtomoDirector to a separate class.
 * <p>
 * <p> Revision 1.9  2007/09/07 00:17:12  sueh
 * <p> bug# 989 Using a public INSTANCE to refer to the EtomoDirector singleton
 * <p> instead of getInstance and createInstance.
 * <p>
 * <p> Revision 1.8  2006/10/10 05:02:05  sueh
 * <p> bug# 931 Getting the patch out file name from DatasetFiles.
 * <p>
 * <p> Revision 1.7  2005/09/16 17:14:53  sueh
 * <p> bug# 532 Getting command strings from ProcessName.
 * <p>
 * <p> Revision 1.6  2004/11/19 22:39:07  sueh
 * <p> bug# 520 merging Etomo_3-4-6_JOIN branch to head.
 * <p>
 * <p> Revision 1.5.2.2  2004/10/08 15:45:16  sueh
 * <p> bug# 520 Since EtomoDirector is a singleton, made all functions and
 * <p> member variables non-static.
 * <p>
 * <p> Revision 1.5.2.1  2004/09/03 21:04:05  sueh
 * <p> bug# 520 calling isSelfTest from EtomoDirector
 * <p>
 * <p> Revision 1.5  2004/08/24 23:07:48  sueh
 * <p> bug# 508 in setEndCommand(): making sure I don't delete the wrong
 * <p> echo command in combine.com when getting rid of the code to exit
 * <p> before running volcombine.  Also checking for an incorrect end command
 * <p>
 * <p> Revision 1.4  2004/08/23 23:29:22  sueh
 * <p> bug# 508 added watched file combine.out
 * <p>
 * <p> Revision 1.3  2004/08/20 21:29:06  sueh
 * <p> bug# 508 Made the match string static.  Added equals() functions for
 * <p> testing.
 * <p> Added:
 * <p> String COMSCRIPT_MATCH_STRING
 * <p> String notEqualsReason
 * <p> equals(CombineComscriptState that)
 * <p> getNotEqualsReason()
 * <p> Deleted:
 * <p> String comscriptMatchString
 * <p> Changed:
 * <p> String getComscriptMatchString()
 * <p> initializeComscriptMatchString()
 * <p> selfTest(int state)
 * <p>
 * <p> $Revision 1.2  2004/08/19 20:01:55  sueh
 * <p> $bug# 508 Generated a regular expression that will match all 
 * <p> $the comscript names handled by this object.
 * <p> $Added:
 * <p> $String comscriptMatchString
 * <p> $getComscriptMatchString()
 * <p> $initializeComscriptMatchString()
 * <p> $Changed:
 * <p> $CombineComscriptState()
 * <p> $selfTest(int state)
 * <p> $
 * <p> $Revision 1.1  2004/08/19 00:36:18  sueh
 * <p> $bug# 508 adding state object for combine.com to update
 * <p> $combine.com, keep track of which commands will run,
 * <p> $and keep track of information required to run command.com
 * <p> </p>
 */
public final class CombineComscriptState implements ComscriptState {
  public static final String COMSCRIPT_NAME = "combine";
  public static final String COMSCRIPT_WATCHED_FILE = "combine.out";

  private final String commands[] = { ProcessName.SOLVEMATCH.toString(),
    ProcessName.MATCHVOL1.toString(), ProcessName.PATCHCORR.toString(),
    ProcessName.MATCHORWARP.toString(), ProcessName.VOLCOMBINE.toString() };

  private static final String WATCHED_FILES[] =
    { null, null, DatasetFiles.PATCH_OUT, null, null };

  private static final int NULL_INDEX = -1;
  private static final int SOLVEMATCH_DUALVOLMATCH_INDEX = 0;
  private static final int MATCHVOL1_INDEX = 1;
  private static final int PATCHCORR_INDEX = 2;
  private static final int MATCHORWARP_INDEX = 3;
  private static final int VOLCOMBINE_INDEX = 4;
  private static final char LABEL_DELIMITER = ':';

  private static final String SUCCESS_TEXT = "COMBINE SUCCESSFULLY COMPLETED";
  private static final String THROUGH_TEXT = " THROUGH ";

  private static final int CONSTRUCTED_STATE = 1;
  private static final int INITIALIZED_STATE = 2;
  private static final int START_COMMAND_SET_STATE = 3;
  private static final int END_COMMAND_SET_STATE = 4;

  private int startCommand = NULL_INDEX;
  private int endCommand = NULL_INDEX;
  private FileType outputImageFileType = null;
  private FileType outputImageFileType2 = null;
  private FileType outputImageFileTypeExternal = null;

  // testing variables
  private String notEqualsReason = null;
  private boolean selfTest = false;

  public CombineComscriptState(final boolean initialVolumeMatching) {
    if (initialVolumeMatching) {
      commands[SOLVEMATCH_DUALVOLMATCH_INDEX] = ProcessName.DUALVOLMATCH.toString();
    }
  }

  /**
   * initialize instance from combine.com
   * @param comScriptManager
   * @return
   */
  boolean initialize(final ComScriptManager comScriptManager) {
    if (!loadStartCommand(comScriptManager)) {
      return false;
    }
    loadEndCommand(comScriptManager);
    return true;
  }

  /**
   * sets startCommand in combine.com
   * @param startCommand
   * @param comScriptManager
   */
  public void setStartCommand(final int startCommand,
    final ComScriptManager comScriptManager) {
    if (startCommand < 0 || startCommand >= commands.length) {
      throw new IndexOutOfBoundsException();
    }
    this.startCommand = startCommand;
    if (startCommand <= PATCHCORR_INDEX) {
      outputImageFileType = FileType.PATCH_VECTOR_MODEL;
      outputImageFileType2 = FileType.PATCH_VECTOR_CCC_MODEL;
    }
    else {
      outputImageFileType = null;
      outputImageFileType2 = null;
    }
    GotoParam gotoParam = new GotoParam();
    gotoParam.setLabel(commands[startCommand]);
    comScriptManager.saveCombine(gotoParam);
  }

  /**
   * sets endCommand in combine.com
   * @param endCommand
   * @param comScriptManager
   */
  public void setEndCommand(final int endCommand,
    final ComScriptManager comScriptManager) {
    if (endCommand < 0 || endCommand >= commands.length) {
      throw new IndexOutOfBoundsException();
    }
    this.endCommand = endCommand;
    String commandLabel = toLabel(VOLCOMBINE_INDEX);
    // if the endCommand is the last command (volcombine), remove the exit
    // success commands from after the volcombine label, if they are there
    if (endCommand == VOLCOMBINE_INDEX) {
      // look for the success echo. Delete it and the exit command if it is
      // found
      EchoParam echoParamInComscript =
        comScriptManager.getEchoParamFromCombine(commandLabel);
      if (echoParamInComscript != null
        && echoParamInComscript.getString().startsWith(SUCCESS_TEXT)) {
        comScriptManager.deleteFromCombine(EchoParam.COMMAND_NAME, commandLabel);
        comScriptManager.deleteFromCombine(ExitParam.COMMAND_NAME, commandLabel);
      }
    }
    else if (endCommand == MATCHORWARP_INDEX) {
      // if the endCommand is not the last command (must be matchorwarp), add
      // or update exit success commands after the volcombine label
      // insert echo param if it is not there, otherwise update it
      EchoParam echoParam = new EchoParam();
      echoParam.setString(
        SUCCESS_TEXT + THROUGH_TEXT + commands[MATCHORWARP_INDEX].toUpperCase());
      int echoIndex = comScriptManager.saveCombine(echoParam, commandLabel);
      // insert exit param if it is not there, otherwise update it
      ExitParam exitParam = new ExitParam();
      exitParam.setResultValue(0);
      comScriptManager.saveCombine(exitParam, echoIndex);
    }
    else {
      throw new IllegalStateException(
        "EndCommand can only be volcombine or matchorwarp.  endCommand=" + endCommand);
    }
  }

  public String getMatchingCommand(final String line) {
    if (line == null) {
      return null;
    }
    for (int i = 0; i < commands.length; i++) {
      if (line.indexOf(commands[i]) != -1) {
        return commands[i];
      }
    }
    return null;
  }

  public void resetOutputImageFileType() {
    outputImageFileTypeExternal = null;
  }

  public void setOutputImageFileType(final FileType input) {
    outputImageFileTypeExternal = input;
  }

  /**
   * 
   * @return true if volcombine will run
   */
  public boolean isRunVolcombine() {
    return endCommand >= VOLCOMBINE_INDEX;
  }

  /**
   * 
   */
  public int getStartCommand() {
    return startCommand;
  }

  /**
   * 
   */
  public int getEndCommand() {
    return endCommand;
  }

  /**
   * 
   */
  public String getCommand(final int commandIndex) {
    if (commandIndex == NULL_INDEX) {
      return null;
    }
    return commands[commandIndex];
  }

  public ProcessName getInitialProcessName() {
    return ProcessName.getInstance(commands[SOLVEMATCH_DUALVOLMATCH_INDEX]);
  }

  /**
   * 
   */
  public String getWatchedFile(final int commandIndex) {
    if (commandIndex == NULL_INDEX) {
      return null;
    }
    return WATCHED_FILES[commandIndex];
  }

  public String getComscriptName() {
    return COMSCRIPT_NAME;
  }

  public String getComscriptWatchedFile() {
    return COMSCRIPT_WATCHED_FILE;
  }

  public static String getSuccessText() {
    return SUCCESS_TEXT;
  }

  /**
   * convert a command name to a command index
   * @param commandName
   * @return
   */
  private int getCommandIndex(final String commandName) {
    for (int i = 0; i < commands.length; i++) {
      if (commandName.equals(commands[i])) {
        return i;
      }
    }
    return NULL_INDEX;
  }

  /**
   * convert a command index to a label string
   * @param commandIndex
   * @return
   */
  private String toLabel(final int commandIndex) {
    return commands[commandIndex] + LABEL_DELIMITER;
  }

  /**
   * load startCommand from combine.com
   * @param comScriptManager
   * @return
   */
  private boolean loadStartCommand(final ComScriptManager comScriptManager) {
    // check the first goto to see which command will be run first
    GotoParam gotoParam = comScriptManager.getGotoParamFromCombine();
    // backward compatibility - old combine.com did not have this goto
    if (gotoParam == null) {
      return false;
    }
    startCommand = getCommandIndex(gotoParam.getLabel());
    return true;
  }

  /**
   * load end command from combine.com.  Check to see if combine.com is exiting
   * before running volcombine.
   * @param comScriptManager
   */
  private void loadEndCommand(final ComScriptManager comScriptManager) {
    EchoParam echoParam =
      comScriptManager.getEchoParamFromCombine(toLabel(VOLCOMBINE_INDEX));
    if (echoParam != null && echoParam.getString().startsWith(SUCCESS_TEXT)) {
      endCommand = VOLCOMBINE_INDEX - 1;
    }
    else {
      endCommand = VOLCOMBINE_INDEX;
    }
  }

  public boolean isDualvolmatchPresent(final ComScriptManager comScriptManager) {
    return comScriptManager.isDualvolmatchLabelInCombine();
  }

  public boolean equals(final CombineComscriptState that) {
    if (startCommand != that.startCommand) {
      notEqualsReason = "StartCommand is not equal.  this.startCommand=" + startCommand
        + ",that.startCommand=" + that.startCommand;
      return false;
    }
    if (endCommand != that.endCommand) {
      notEqualsReason = "EndCommand is not equal.  this.endCommand=" + endCommand
        + ",that.endCommand=" + that.endCommand;
      return false;
    }
    notEqualsReason = null;
    return true;
  }

  /**
   * @deprecated 6/18/19
   */
  @SuppressWarnings("deprecation")
  public FileType getOutputImageFileType() {
    return outputImageFileType;
  }

  public FileKey getOutputImageFileKey() {
    return outputImageFileType;
  }

  /**
   * @deprecated 6/18/19
   */
  @SuppressWarnings("deprecation")
  public FileType getOutputImageFileType2() {
    return outputImageFileType2;
  }

  public FileKey getOutputImageFileKey2() {
    return outputImageFileType2;
  }

  /**
  * @deprecated 6/18/19
  */
  @SuppressWarnings("deprecation")
  public FileType getOutputImageFileType3() {
    return outputImageFileTypeExternal;
  }

  public FileKey getOutputImageFileKey3() {
    return outputImageFileTypeExternal;
  }
}
