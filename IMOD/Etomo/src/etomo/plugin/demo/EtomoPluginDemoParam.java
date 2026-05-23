package etomo.plugin.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import etomo.comscript.BadComScriptException;
import etomo.comscript.ComScriptCommand;
import etomo.comscript.CommandDetails;
import etomo.comscript.CommandMode;
import etomo.comscript.CommandParam;
import etomo.comscript.FieldInterface;
import etomo.comscript.FortranInputSyntaxException;
import etomo.comscript.InvalidParameterException;
import etomo.storage.LogFile;
import etomo.type.AxisID;
import etomo.type.ConstEtomoNumber;
import etomo.type.ConstIntKeyList;
import etomo.type.FileKey;
import etomo.type.FileType;
import etomo.type.IteratorElementList;
import etomo.type.ProcessName;
import etomo.type.ScriptParameter;
import etomo.type.StringParameter;

/**
 * <p>Description: Param for the parameters of etomoPluginDemo.  Runs from a com file
 * with a different name.</p>
 *
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 *
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 */
final class EtomoPluginDemoParam implements CommandParam, CommandDetails {
  static final ProcessName COMMAND_PROCESS_NAME = DemoProcessName.ETOMO_PLUGIN_DEMO;
  private static final ProcessName SCRIPT_PROCESS_NAME = DemoProcessName.DEMO;
  static final String SLEEP_TIME_KEY = "SleepTime";
  static final String MESSAGE_KEY = "Message";

  private final ScriptParameter sleepTime = new ScriptParameter(SLEEP_TIME_KEY);
  private final StringParameter message = new StringParameter("Message");

  private final AxisID axisID;

  EtomoPluginDemoParam(final AxisID axisID) {
    this.axisID = axisID;
    sleepTime.setDisplayValue(SleepTime.DEFAULT.getValue().getInt());
  }

  //
  // Implement CommandParam
  //

  /**
   * Initialize the parameter object from the ComScriptCommand object
   * @param scriptCommand
   */
  public void parseComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException, FortranInputSyntaxException, InvalidParameterException {
    // reset
    initializeDefaults();
    // parse
    sleepTime.parse(scriptCommand);
    message.parse(scriptCommand);
  }

  /**
   * Replace the parameters of the ComScriptCommand with the current 
   * CommandParameter object's parameters
   * @param scriptCommand
   */
  public void updateComScriptCommand(final ComScriptCommand scriptCommand)
    throws BadComScriptException {
    scriptCommand.useKeywordValue();
    sleepTime.updateComScript(scriptCommand);
    message.updateComScript(scriptCommand);
  }

  public void initializeDefaults() {
    sleepTime.reset();
    message.reset();
  }

  void setSleepTime(final String input) {
    sleepTime.set(input);
  }

  void resetSleepTime() {
    sleepTime.reset();
  }

  void setSleepTime(int input) {
    if (input < 1) {
      input = SleepTime.DEFAULT.getValue().getInt();
    }
    sleepTime.set(input);
  }

  boolean isSleepTimeGt(final int input) {
    return sleepTime.gt(input);
  }

  void setSleepTime(final ConstEtomoNumber input) {
    sleepTime.set(input);
  }

  SleepTime getSleepTime() {
    return SleepTime.getInstance(sleepTime);
  }

  int getSleepTimeValue() {
    return sleepTime.getInt();
  }

  void setMessage(final String input) {
    message.set(input);
  }

  void resetMessage() {
    message.reset();
  }

  boolean isMessageSet() {
    return !message.isEmpty();
  }

  String getMessage() {
    return message.toString();
  }

  //
  // Implementing Command
  //

  public AxisID getAxisID() {
    return axisID;
  }

  /**
   * For params that can be run for different purposes with different parameters, or run
   * from different comscripts.
   * @return
   */
  public CommandMode getCommandMode() {
    return null;
  }

  /**
   * For a comscript, the process name of the comscript
   * For a command line call, the process name of the process being run
   * @return
   */
  public ProcessName getProcessName() {
    return SCRIPT_PROCESS_NAME;
  }

  /**
   * For a comscript, the name of the comscript file (ProcessName.getComscript(axisID))
   * For a command line call, the name of the process being run
   * @return
   */
  public String getCommand() {
    return SCRIPT_PROCESS_NAME.getComscript(axisID);
  }

  /**
   * For a comscript, the name of the comscript (ProcessName.toString)
   * For a command line call, the name of the process being run
   * @return
   */
  public String getCommandName() {
    return SCRIPT_PROCESS_NAME.toString();
  }

  /**
   * For a comscript, the name of the comscript (ProcessName.toString)
   * For a command line call, the entire command line
   * @return
   */
  public String getCommandLine() {
    return SCRIPT_PROCESS_NAME.getComscript(axisID);
  }

  /**
   * For a comscript, the comscript file (without a path)
   * For a command line call, the entire command line
   * @return
   */
  public String[] getCommandArray() {
    return new String[] { SCRIPT_PROCESS_NAME.getComscript(axisID) };
  };

  /**
   * @return the input file parameter if available
   */
  public File getCommandInputFile() {
    return null;
  }

  /**
   * @return the output file parameter if available
   */
  public File getCommandOutputFile() {
    return null;
  }

  /**
   * @deprecated 3/15/2019
   */
  @SuppressWarnings("deprecation")
  public FileType getOutputImageFileType() {
    return null;
  }

  public FileKey getOutputImageFileKey() {
    return null;
  }

  /**
   * @deprecated 3/15/2019
   */
  @SuppressWarnings("deprecation")
  public FileType getOutputImageFileType2() {
    return null;
  }

  public FileKey getOutputImageFileKey2() {
    return null;
  }

  /**
   * Return yes if a message from the process needs to be popped up while the process is
   * running, rather then reported after the process is finished.  A process monitor which
   * recongizes the message will be needed.  If all messages need to be popped up while
   * the process is running, use ProcessMessages.startStringFeed with a process monitor.
   * @return
   */
  public boolean isMessageReporter() {
    return false;
  }

  /**
   * For a command that runs another command (like processchunks).
   * @return the process name of the other command
   */
  public String getSubcommandProcessName() {
    return null;
  }

  /**
   * For a command that runs another command (like processchunks).
   * @return the Param class instance of the other command
   */
  public CommandDetails getSubcommandDetails() {
    return null;
  }

  //
  // Implement Loggable
  //
  public ArrayList<String> getLogMessage()
    throws LogFile.LockException, FileNotFoundException, IOException {
    return null;
  }

  public String getName() {
    return SCRIPT_PROCESS_NAME.toString();
  }

  //
  // Implement ProcessDetails
  //

  public int getIntValue(FieldInterface field) {
    if (field == Field.SLEEP_TIME) {
      return sleepTime.getInt();
    }
    throw new IllegalArgumentException("field=" + field);
  }

  public boolean getBooleanValue(FieldInterface field) {
    throw new IllegalArgumentException("field=" + field);
  }

  public double getDoubleValue(FieldInterface field) {
    throw new IllegalArgumentException("field=" + field);
  }

  public Hashtable getHashtable(FieldInterface field) {
    throw new IllegalArgumentException("field=" + field);
  }

  public ConstEtomoNumber getEtomoNumber(FieldInterface field) {
    throw new IllegalArgumentException("field=" + field);
  }

  public ConstIntKeyList getIntKeyList(FieldInterface field) {
    throw new IllegalArgumentException("field=" + field);
  }

  public String getString(FieldInterface field) {
    throw new IllegalArgumentException("field=" + field);
  }

  public String[] getStringArray(FieldInterface field) {
    throw new IllegalArgumentException("field=" + field);
  }

  public IteratorElementList getIteratorElementList(FieldInterface field) {
    throw new IllegalArgumentException("field=" + field);
  }

  static final class Field implements FieldInterface {
    static final Field SLEEP_TIME = new Field("SLEEP_TIME");
    private final String string;

    Field(final String string) {
      this.string = string;
    }
  }
}
