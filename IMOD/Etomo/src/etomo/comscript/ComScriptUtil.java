package etomo.comscript;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.FileType;
import etomo.type.ProcessName;
import etomo.ui.swing.UIHarness;
import etomo.util.Utilities;

/**
 * <p>Description:  Utility class for comscript managers.  Replaces BaseComScriptManager.</p>
 * 
 * <p>Copyright: Copyright 2016 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class ComScriptUtil {
  static ComScript loadComScript(final BaseManager manager, final FileType scriptFileType,
    final AxisID axisID, final boolean parseComments, final boolean caseInsensitive,
    final boolean separateWithASpace) {
    return loadComScript(manager, scriptFileType.getFileName(manager, axisID), axisID,
      parseComments, true, caseInsensitive, separateWithASpace);
  }

  /**
   * Load the comscript file in commandComScript.
   * @param commandComScriptFileName - file name
   * @param axisID
   * @param parseComments
   * @param required
   * @param caseInsensitive
   * @param separateWithASpace
   * @return
   */
  public static final ComScript loadComScript(final BaseManager manager,
    final String commandComScriptFileName, final AxisID axisID,
    final boolean parseComments, final boolean required, final boolean caseInsensitive,
    final boolean separateWithASpace) {
    Utilities.timestamp("load", commandComScriptFileName, Utilities.STARTED_STATUS);
    File comFile = new File(manager.getPropertyUserDir(), commandComScriptFileName);
    // If the file isn't there and its not required then just return null without
    // any error messages.
    if (!required && !comFile.exists()) {
      return null;
    }
    ComScript comScript = new ComScript(comFile);
    try {
      comScript.setParseComments(parseComments);
      comScript.readComFile(caseInsensitive, separateWithASpace);
    }
    catch (Exception except) {
      except.printStackTrace();
      String[] errorMessage = new String[2];
      errorMessage[0] = "Com file: " + comScript.getComFileName();
      errorMessage[1] = except.getMessage();
      JOptionPane.showMessageDialog(
        null, errorMessage, "Can't parse " + commandComScriptFileName
          + axisID.getExtension() + ".com file: " + comScript.getComFileName(),
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("load", commandComScriptFileName, Utilities.FAILED_STATUS);
      return null;
    }
    Utilities.timestamp("load", commandComScriptFileName, Utilities.FINISHED_STATUS);
    return comScript;
  }

  /**
   * Initialize the CommandParam object from the specified command in the
   * comscript.  True is returned if the initialization is successful, false if
   * the initialization fails.
   * 
   * @param param
   * @param comScript
   * @param command
   * @param axisID
   * @return boolean
   */
  public static final boolean initialize(final BaseManager manager,
    final CommandParam param, final ComScript comScript, final String command,
    final AxisID axisID, final boolean caseInsensitive, final boolean separateWithASpace,
    final boolean required) {
    return initialize(manager, param, comScript, command, axisID, false, caseInsensitive,
      separateWithASpace, required, null);
  }

  public static final boolean initialize(final BaseManager manager,
    final CommandParam param, final ComScript comScript, final String command,
    final AxisID axisID, final boolean caseInsensitive, final boolean separateWithASpace,
    final boolean required, final String requiredOption) {
    return initialize(manager, param, comScript, command, axisID, false, caseInsensitive,
      separateWithASpace, required, requiredOption);
  }

  static final boolean initialize(final BaseManager manager, final CommandParam param,
    final ComScript comScript, final String command, final AxisID axisID,
    final boolean optionalCommand, final boolean caseInsensitive,
    final boolean separateWithASpace, final boolean required,
    final String requiredOption) {
    if (comScript == null) {
      return false;
    }
    Utilities.timestamp("initialize", command, comScript, Utilities.STARTED_STATUS);
    if (!comScript.isCommandLoaded()) {
      param.initializeDefaults();
    }
    else {
      int index = comScript.getScriptCommandIndex(command, caseInsensitive,
        separateWithASpace, requiredOption);
      if (optionalCommand) {
        if (index == -1) {
          Utilities.timestamp("initialize", command, comScript, Utilities.FAILED_STATUS);
          return false;
        }
      }
      try {
        param.parseComScriptCommand(
          comScript.getScriptCommand(command, caseInsensitive, separateWithASpace));
      }
      catch (Exception except) {
        if (required) {
          except.printStackTrace();
          String[] errorMessage = new String[4];
          errorMessage[0] = "Com file: " + comScript.getComFileName();
          errorMessage[1] = "Command: " + command;
          errorMessage[2] = except.getClass().getName();
          errorMessage[3] = except.getMessage();
          JOptionPane.showMessageDialog(null, errorMessage,
            "Com Script Command Parse Error", JOptionPane.ERROR_MESSAGE);
          Utilities.timestamp("initialize", command, comScript, Utilities.FAILED_STATUS);
        }
        return false;
      }
    }
    Utilities.timestamp("initialize", command, comScript, Utilities.FINISHED_STATUS);
    return true;
  }

  /**
   * Update the specified comscript with 
   * @param script
   * @param params
   * @param command
   * @param axisID
   */
  public static final void modifyCommand(final BaseManager manager,
    final ComScript script, final CommandParam params, final String command,
    final AxisID axisID, final boolean caseInsensitive,
    final boolean separateWithASpace) {
    modifyCommand(manager, script, params, command, axisID, false, false, caseInsensitive,
      separateWithASpace, null);
  }

  /**
   * Update the specified comscript with 
   * @param script
   * @param params
   * @param command
   * @param axisID
   */
  public static final void modifyCommand(final BaseManager manager,
    final ComScript script, final CommandParam params, final String command,
    final AxisID axisID, final boolean caseInsensitive, final boolean separateWithASpace,
    final String requiredOption) {
    modifyCommand(manager, script, params, command, axisID, false, false, caseInsensitive,
      separateWithASpace, requiredOption);
  }

  /**
   * Modify and/or add command (depending on addNew boolean)
   * May treate command as optional, depending on optional boolean
   * @param script
   * @param params
   * @param command
   * @param axisID
   * @param addNew
   * @param optional
   * @return
   */
  static final boolean modifyCommand(final BaseManager manager, final ComScript script,
    final CommandParam params, final String command, final AxisID axisID,
    final boolean addNew, final boolean optional, final boolean caseInsensitive,
    final boolean separateWithASpace, final String requiredOption) {
    if (script == null) {
      (new IllegalStateException()).printStackTrace();
      String[] errorMessage = new String[2];
      errorMessage[0] = "Unable to update comscript.";
      errorMessage[1] = "\nscript=" + script + "\ncommand=" + command;
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "ComScriptManager Error", axisID);
      return false;
    }
    Utilities.timestamp("update", command, script, Utilities.STARTED_STATUS);

    // Update the specified com script command from the CommandParam object
    ComScriptCommand comScriptCommand = null;
    int commandIndex = script.getScriptCommandIndex(command, addNew, caseInsensitive,
      separateWithASpace, requiredOption);
    // optional return false if failed
    if (optional && commandIndex == -1) {
      Utilities.timestamp("updateComScript", command, script, Utilities.FAILED_STATUS);
      return false;
    }
    try {
      comScriptCommand =
        script.getScriptCommand(command, caseInsensitive, separateWithASpace);
      params.updateComScriptCommand(comScriptCommand);
    }
    catch (BadComScriptException except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + script.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, errorMessage,
        "Can't update " + command + " in " + script.getComFileName(),
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("update", command, script, Utilities.FAILED_STATUS);
      return false;
    }

    // Replace the specified command by the updated comScriptCommand
    script.setScriptComand(commandIndex, comScriptCommand);

    // Write the script back out to disk
    try {
      script.writeComFile();
    }
    catch (Exception except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + script.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, except.getMessage(),
        "Can't write " + command + axisID.getExtension() + ".com",
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("update", command, script, Utilities.FAILED_STATUS);
      return false;
    }
    Utilities.timestamp("update", command, script, Utilities.FINISHED_STATUS);
    return true;
  }

  /**
   * Use a template script from the $IMOD_DIR/com directory.  This is useful if
   * we encounter an old data set that does not have a current complete set of
   * com scripts
   * @param scriptName the basename of the template file
   * @param axisType axisType Specify whether this is a single or dual axis
   * tomogram. 
   * @param axisID the AxisID to create.  This is ignored for single axis 
   * tomogram.  For a dual axis tomogram AxisID.FIRST will create an a.com
   * script, AxisID.SECOND will create a b.com script.  AxisID.ONLY will
   * create a .com script replacing the g5a and g5b tags.
   *  
   */
  static void useTemplate(final BaseManager manager, final String scriptName,
    String datasetName, final AxisType axisType, final AxisID axisID)
    throws BadComScriptException, IOException {
    Utilities.timestamp("copy from template", scriptName, Utilities.STARTED_STATUS);
    // Read in the template file from the IMOD_DIR/com directory replacing all
    // instances of the tag g5a and g5b with the appropriate dataset name
    String comDirectory = EtomoDirector.INSTANCE.getIMODDirectory().getAbsolutePath()
      + File.separator + "com";

    File template = new File(comDirectory, scriptName + ".com");
    if (!template.exists()) {
      String message = "Unknown template: " + scriptName;
      Utilities.timestamp("copy from template", scriptName, Utilities.FAILED_STATUS);
      throw new BadComScriptException(message);
    }
    BufferedReader templateReader = new BufferedReader(new FileReader(template));

    // The ouput script
    File script;
    BufferedWriter scriptWriter;

    // Open the appropriate output script and change the dataset name if
    // necessary.
    if (axisType == AxisType.SINGLE_AXIS) {
      script = new File(manager.getPropertyUserDir(), scriptName + ".com");
      scriptWriter = new BufferedWriter(new FileWriter(script));
    }
    else {
      script = new File(manager.getPropertyUserDir(),
        scriptName + axisID.getExtension() + ".com");
      scriptWriter = new BufferedWriter(new FileWriter(script));
      datasetName = datasetName + axisID.getExtension();
    }

    if (axisType == AxisType.DUAL_AXIS && axisID == AxisID.ONLY) {
      String line;
      while ((line = templateReader.readLine()) != null) {
        line = line.replaceAll("g5a", datasetName + "a");
        line = line.replaceAll("g5b", datasetName + "b");
        scriptWriter.write(line);
        scriptWriter.newLine();
      }
    }
    else {
      String line;
      while ((line = templateReader.readLine()) != null) {
        line = line.replaceAll("g5a", datasetName);
        scriptWriter.write(line);
        scriptWriter.newLine();
      }
    }
    templateReader.close();
    scriptWriter.close();
    Utilities.timestamp("copy from template", scriptName, Utilities.FINISHED_STATUS);
  }

  /**
   * Use a template script from the $IMOD_DIR/com directory.  This is useful if
   * we encounter an old data set that does not have a current complete set of
   * com scripts
   * @param scriptName the basename of the template file
   * @param axisType axisType Specify whether this is a single or dual axis
   * tomogram. 
   * @param axisID the AxisID to create.  This is ignored for single axis 
   * tomogram.  For a dual axis tomogram AxisID.FIRST will create an a.com
   * script, AxisID.SECOND will create a b.com script.  AxisID.ONLY will
   * create a .com script.
   * @param rename rename the original script file
   * @throws BadComScriptException
   * @throws IOException
   */
  public static void useTemplate(final BaseManager manager, final String scriptName,
    final AxisType axisType, final AxisID axisID, final boolean rename)
    throws BadComScriptException, IOException {
    Utilities.timestamp("copy from template", scriptName, Utilities.STARTED_STATUS);
    // Copy the template file from the IMOD_DIR/com directory to the script
    String comDirectory = EtomoDirector.INSTANCE.getIMODDirectory().getAbsolutePath()
      + File.separator + "com";

    File template = new File(comDirectory, scriptName + ".com");
    if (!template.exists()) {
      String message = "Unknown template: " + scriptName;
      Utilities.timestamp("copy from template", scriptName, Utilities.FAILED_STATUS);
      throw new BadComScriptException(message);
    }

    // The ouput script
    File script;
    if (axisType == AxisType.SINGLE_AXIS) {
      script = new File(manager.getPropertyUserDir(), scriptName + ".com");
    }
    else {
      script = new File(manager.getPropertyUserDir(),
        scriptName + axisID.getExtension() + ".com");
    }

    if (rename) {
      Utilities.renameFile(script, new File(script.getAbsolutePath() + "~"));
    }
    Utilities.copyFile(template, script);
    Utilities.timestamp("copy from template", scriptName, Utilities.FINISHED_STATUS);
  }

  static ComScript loadComScript(final BaseManager manager, final ProcessName processName,
    final AxisID axisID, final boolean parseComments, final boolean caseInsensitive,
    final boolean separateWithASpace) {
    return loadComScript(manager, processName.getComscript(axisID), axisID, parseComments,
      true, caseInsensitive, separateWithASpace);
  }

  static ComScript loadComScript(final BaseManager manager, final String scriptName,
    final AxisID axisID, final boolean parseComments, final boolean caseInsensitive,
    final boolean separateWithASpace) {
    return loadComScript(manager,
      ProcessName.getInstance(scriptName, axisID).getComscript(axisID), axisID,
      parseComments, true, caseInsensitive, separateWithASpace);
  }

  static boolean modifyOptionalCommand(final BaseManager manager, final ComScript script,
    final CommandParam params, final String command, final AxisID axisID,
    final boolean caseInsensitive, final boolean separateWithASpace) {
    return modifyCommand(manager, script, params, command, axisID, false, true,
      caseInsensitive, separateWithASpace, null);
  }

  static boolean modifyCommand(final BaseManager manager, final ComScript script,
    final CommandParam params, final String command, final AxisID axisID,
    final boolean addNew, final boolean optional, final String previousCommand,
    final boolean caseInsensitive, final boolean separateWithASpace) {
    if (script == null) {
      (new IllegalStateException()).printStackTrace();
      String[] errorMessage = new String[2];
      errorMessage[0] = "Unable to update comscript.";
      errorMessage[1] = "\nscript=" + script + "\ncommand=" + command;
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "ComScriptManager Error", axisID);
      return false;
    }
    Utilities.timestamp("update", command, script, Utilities.STARTED_STATUS);

    // locate previous command
    ComScriptCommand previousComScriptCommand = null;
    int previousCommandIndex =
      script.getScriptCommandIndex(previousCommand, caseInsensitive, separateWithASpace);

    // previous command must exist
    if (previousCommandIndex == -1) {
      String[] errorMessage = new String[2];
      errorMessage[0] = "Unable to update " + script.getName() + ".  ";
      errorMessage[1] = "Unable to update " + command + " because the previous command, "
        + previousCommand + ", is missing.";
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "ComScriptManager Error", axisID);
      Utilities.timestamp("update", command, script, Utilities.FAILED_STATUS);
      return false;
    }
    // Update the specified com script command from the CommandParam object
    ComScriptCommand comScriptCommand = null;
    int commandIndex = script.getScriptCommandIndex(command, previousCommandIndex + 1,
      addNew, caseInsensitive, separateWithASpace);
    // optional return false if failed
    if (optional && commandIndex == -1) {
      Utilities.timestamp("updateComScript", command, script, Utilities.FAILED_STATUS);
      return false;
    }
    try {
      comScriptCommand = script.getScriptCommand(commandIndex);
      params.updateComScriptCommand(comScriptCommand);
    }
    catch (BadComScriptException except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + script.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, errorMessage,
        "Can't update " + command + " in " + script.getComFileName(),
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("update", command, script, Utilities.FAILED_STATUS);
      return false;
    }

    // Replace the specified command by the updated comScriptCommand
    script.setScriptComand(commandIndex, comScriptCommand);

    // Write the script back out to disk
    try {
      script.writeComFile();
    }
    catch (Exception except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + script.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, except.getMessage(),
        "Can't write " + command + axisID.getExtension() + ".com",
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("update", command, script, Utilities.FAILED_STATUS);
      return false;
    }
    Utilities.timestamp("update", command, script, Utilities.FINISHED_STATUS);
    return true;
  }

  /**
   * Find the command in fromScript, update it, and write it to toScript.
   * it back to toScript.  AddNew is only used with the toScript.  The
   * fromScript should not be changed.  If the fromScript is not null, the
   * command must be in it.
   * @param fromScript
   * @param toScript
   * @param params
   * @param command
   * @param axisID
   * @param addNew
   */
  static void addModifyCommand(final BaseManager manager, final ComScript fromScript,
    final ComScript toScript, final CommandParam params, final String command,
    final AxisID axisID, final boolean caseInsensitive,
    final boolean separateWithASpace) {
    if (toScript == null) {
      (new IllegalStateException()).printStackTrace();
      String[] errorMessage = new String[2];
      errorMessage[0] = "Unable to update comscript.  ";
      errorMessage[1] = "\ntoScript=" + toScript + "ncommand=" + command;
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "ComScriptManager Error", axisID);
      return;
    }
    Utilities.timestamp("update", command, toScript, Utilities.STARTED_STATUS);
    // If no fromScript, then default to reading from and writing to the
    // toScript.
    if (fromScript == null) {
      modifyCommand(manager, toScript, params, command, axisID, true, false,
        caseInsensitive, separateWithASpace, null);
      Utilities.timestamp("update", command, toScript, Utilities.FINISHED_STATUS);
      return;
    }
    // Update the specified com script command from the CommandParam object
    ComScriptCommand comScriptCommand = null;
    int toScriptCommandIndex = toScript.getScriptCommandIndex(command, true,
      caseInsensitive, separateWithASpace, null);
    try {
      // Get comScriptCommand from fromScript
      comScriptCommand =
        fromScript.getScriptCommand(command, caseInsensitive, separateWithASpace);
    }
    catch (BadComScriptException except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + fromScript.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, errorMessage,
        "Can't read " + command + " in " + fromScript.getComFileName(),
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("update", command, toScript, Utilities.FAILED_STATUS);
      return;
    }
    try {
      // Update comScriptCommand
      params.updateComScriptCommand(comScriptCommand);
    }
    catch (BadComScriptException except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + toScript.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, errorMessage,
        "Can't update " + command + " in " + toScript.getComFileName(),
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("update", command, toScript, Utilities.FAILED_STATUS);
      return;
    }

    // Replace the specified command by the updated comScriptCommand in toScript
    toScript.setScriptComand(toScriptCommandIndex, comScriptCommand);

    // Write toScript back out to disk
    try {
      toScript.writeComFile();
    }
    catch (Exception except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + toScript.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, except.getMessage(),
        "Can't write " + command + axisID.getExtension() + ".com",
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("update", command, toScript, Utilities.FAILED_STATUS);
      return;
    }
    Utilities.timestamp("update", command, toScript, Utilities.FINISHED_STATUS);
  }

  /**
   * Modify and/or add command
   * @param script
   * @param params
   * @param command
   * @param axisID
   * @param addNew
   * @param previousCommand
   * @return index of updated command
   */
  static int addModifyCommand(final BaseManager manager, final ComScript script,
    final CommandParam params, final String command, final AxisID axisID,
    final String previousCommand, final boolean caseInsensitive,
    final boolean separateWithASpace) {
    if (script == null) {
      String[] errorMessage = new String[1];
      errorMessage[0] =
        "Unable to update comscript.\nscript=" + script + "\ncommand=" + command;
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "ComScriptManager Error", axisID);
      return -1;
    }
    Utilities.timestamp("update", command, script, Utilities.STARTED_STATUS);
    // locate previous command
    ComScriptCommand previousComScriptCommand = null;
    int previousCommandIndex =
      script.getScriptCommandIndex(previousCommand, caseInsensitive, separateWithASpace);

    if (previousCommandIndex == -1) {
      String[] errorMessage = new String[2];
      errorMessage[0] = "Unable to update " + script.getName() + ".  ";
      errorMessage[1] = "Unable to update " + command + " because the previous command, "
        + previousCommand + ", is missing.";
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "ComScriptManager Error", axisID);
      Utilities.timestamp("update", command, script, Utilities.FAILED_STATUS);
      return -1;
    }

    return addModifyCommand(manager, script, params, command, axisID,
      previousCommandIndex, true, caseInsensitive, separateWithASpace);
  }

  // 2206
  /**
   * Modify and/or add command
   * @param script
   * @param params
   * @param command
   * @param axisID
   * @param addNew
   * @param previousCommandIndex
   * @return
   */
  static void addModifyCommand(final BaseManager manager, final ComScript script,
    final CommandParam params, final String command, final AxisID axisID,
    final boolean caseInsensitive, final boolean separateWithASpace,
    final String requiredOption) {
    if (script == null) {
      String[] errorMessage = new String[2];
      errorMessage[0] = "Unable to update comscript.";
      errorMessage[1] = "\nscript=" + script + "\ncommand=" + command;
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "ComScriptManager Error", axisID);
      return;
    }
    Utilities.timestamp("update", command, script, Utilities.STARTED_STATUS);
    int index = script.getScriptCommandIndex(command, caseInsensitive, separateWithASpace,
      requiredOption);
    ComScriptCommand comScriptCommand = null;
    // Update the specified com script command from the CommandParam object
    try {
      if (index >= 0) {
        comScriptCommand = script.getScriptCommand(command, index, true, caseInsensitive,
          separateWithASpace);
      }
      else {
        // Command is not in the script.
        comScriptCommand = new ComScriptCommand(caseInsensitive, separateWithASpace);
        comScriptCommand.setCommand(command);
      }
      params.updateComScriptCommand(comScriptCommand);
    }
    catch (BadComScriptException except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + script.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, errorMessage,
        "Can't update " + command + " in " + script.getComFileName(),
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("update", command, script, Utilities.FAILED_STATUS);
      return;
    }

    // Replace the specified command by the updated comScriptCommand
    if (index >= 0) {
      script.setScriptComand(index, comScriptCommand);
    }
    else {
      // Add the command to the beginning of script.
      script.addScriptComand(0, comScriptCommand);
    }
    // Write the script back out to disk
    try {
      script.writeComFile();
    }
    catch (Exception except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + script.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, except.getMessage(),
        "Can't write " + command + axisID.getExtension() + ".com",
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("update", command, script, Utilities.FAILED_STATUS);
      return;
    }
    Utilities.timestamp("update", command, script, Utilities.FINISHED_STATUS);
    return;
  }

  /**
   * Modify and/or add command
   * @param script
   * @param params
   * @param command
   * @param axisID
   * @param addNew
   * @param previousCommandIndex
   * @return
   */
  static int addModifyCommand(final BaseManager manager, final ComScript script,
    final CommandParam params, final String command, final AxisID axisID,
    final int previousCommandIndex, final boolean updateStarted,
    final boolean caseInsensitive, final boolean separateWithASpace) {
    if (script == null) {
      String[] errorMessage = new String[2];
      errorMessage[0] = "Unable to update comscript.";
      errorMessage[1] = "\nscript=" + script + "\ncommand=" + command;
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "ComScriptManager Error", axisID);
      return -1;
    }
    if (!updateStarted) {
      Utilities.timestamp("update", command, script, Utilities.STARTED_STATUS);
    }
    if (previousCommandIndex < -1) {
      String[] errorMessage = new String[2];
      errorMessage[0] = "Unable to update " + script.getName() + ".  ";
      errorMessage[1] = "Can't find " + command + " because the previous command index, "
        + previousCommandIndex + ", is invalid.\npreviousCommandIndex="
        + previousCommandIndex;
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "ComScriptManager Error", axisID);
      Utilities.timestamp("update", command, script, Utilities.FAILED_STATUS);
      return -1;
    }

    // Update the specified com script command from the CommandParam object
    ComScriptCommand comScriptCommand = null;
    int commandIndex = previousCommandIndex + 1;

    try {
      comScriptCommand = script.getScriptCommand(command, commandIndex, true,
        caseInsensitive, separateWithASpace);
      params.updateComScriptCommand(comScriptCommand);
    }
    catch (BadComScriptException except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + script.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, errorMessage,
        "Can't update " + command + " in " + script.getComFileName(),
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("update", command, script, Utilities.FAILED_STATUS);
      return commandIndex;
    }

    // Replace the specified command by the updated comScriptCommand
    script.setScriptComand(commandIndex, comScriptCommand);
    // Write the script back out to disk
    try {
      script.writeComFile();
    }
    catch (Exception except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + script.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, except.getMessage(),
        "Can't write " + command + axisID.getExtension() + ".com",
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("update", command, script, Utilities.FAILED_STATUS);
      return commandIndex;
    }
    Utilities.timestamp("update", command, script, Utilities.FINISHED_STATUS);
    return commandIndex;
  }

  static void deleteCommand(final BaseManager manager, final ComScript script,
    final String command, final AxisID axisID, final String previousCommand,
    final boolean caseInsensitive, final boolean separateWithASpace) {
    if (script == null) {
      String[] errorMessage = new String[2];
      errorMessage[0] = "Unable to update comscript.  ";
      errorMessage[1] = "Cannot delete" + command + ".\nscript=" + script;
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "ComScriptManager Error", axisID);
      return;
    }
    Utilities.timestamp("delete", command, script, Utilities.STARTED_STATUS);

    // locate previous command
    ComScriptCommand previousComScriptCommand = null;
    int previousCommandIndex =
      script.getScriptCommandIndex(previousCommand, caseInsensitive, separateWithASpace);

    if (previousCommandIndex == -1) {
      String[] errorMessage = new String[2];
      errorMessage[0] = "Unable to update " + script.getName() + ".  ";
      errorMessage[1] = "Unable to delete " + command + " because the previous command, "
        + previousCommand + ", is missing.";
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "ComScriptManager Error", axisID);
      Utilities.timestamp("delete", command, script, Utilities.FAILED_STATUS);
      return;
    }

    // Update the specified com script command from the CommandParam object
    ComScriptCommand comScriptCommand = null;
    int commandIndex = script.getScriptCommandIndex(command, previousCommandIndex + 1,
      caseInsensitive, separateWithASpace);

    if (commandIndex == -1) {
      Utilities.timestamp("delete", command, script, Utilities.FAILED_STATUS);
      return;
    }
    script.deleteCommand(commandIndex);

    // Write the script back out to disk
    try {
      script.writeComFile();
    }
    catch (Exception except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + script.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, except.getMessage(),
        "Can't write " + command + axisID.getExtension() + ".com",
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("delete", command, script, Utilities.FAILED_STATUS);
    }
    Utilities.timestamp("delete", command, script, Utilities.FINISHED_STATUS);
  }

  static void deleteCommand(final BaseManager manager, final ComScript script,
    final String command, final AxisID axisID, final boolean caseInsensitive,
    final boolean separateWithASpace) {
    if (script == null) {
      (new IllegalStateException()).printStackTrace();
      String[] errorMessage = new String[2];
      errorMessage[0] = "Unable to update comscript.";
      errorMessage[1] = "\nscript=" + script + "\ncommand=" + command;
      UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
        "ComScriptManager Error", axisID);
      return;
    }
    Utilities.timestamp("delete", command, script, Utilities.STARTED_STATUS);

    // Update the specified com script command from the CommandParam object
    ComScriptCommand comScriptCommand = null;
    int commandIndex =
      script.getScriptCommandIndex(command, caseInsensitive, separateWithASpace);

    try {
      comScriptCommand =
        script.getScriptCommand(command, caseInsensitive, separateWithASpace);
    }
    catch (BadComScriptException except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + script.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, errorMessage,
        "Can't delete " + command + " in " + script.getComFileName(),
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("delete", command, script, Utilities.FAILED_STATUS);
      return;
    }

    // Delete the specified command
    script.deleteCommand(commandIndex);

    // Write the script back out to disk
    try {
      script.writeComFile();
    }
    catch (Exception except) {
      except.printStackTrace();
      String[] errorMessage = new String[3];
      errorMessage[0] = "Com file: " + script.getComFileName();
      errorMessage[1] = "Command: " + command;
      errorMessage[2] = except.getMessage();
      JOptionPane.showMessageDialog(null, except.getMessage(),
        "Can't write " + command + axisID.getExtension() + ".com",
        JOptionPane.ERROR_MESSAGE);
      Utilities.timestamp("delete", command, script, Utilities.FAILED_STATUS);
      return;
    }
    Utilities.timestamp("delete", command, script, Utilities.FINISHED_STATUS);
  }

  /**
   * Initialize the CommandParam object from the specified command in the
   * comscript.  True is returned if the initialization is successful, false if
   * the initialization fails.
   * 
   * @param param
   * @param comScript
   * @param command
   * @param axisID
   * @param optionalCommand - missing command returns false, no exception thrown
   * @param optionalPreviousCommand - missing previous command returns false, no exception thrown
   * @return boolean
   */
  static boolean initialize(final BaseManager manager, final CommandParam param,
    final ComScript comScript, final String command, final AxisID axisID,
    final boolean optionalCommand, final String previousCommand,
    final boolean optionalPreviousCommand, final boolean caseInsensitive,
    final boolean separateWithASpace, final boolean required) {
    if (previousCommand == null) {
      return initialize(manager, param, comScript, command, axisID, caseInsensitive,
        separateWithASpace, required);
    }
    Utilities.timestamp("initialize", command, comScript, Utilities.STARTED_STATUS);
    if (!comScript.isCommandLoaded()) {
      param.initializeDefaults();
    }
    else {
      // locate previous command
      ComScriptCommand previousComScriptCommand = null;
      int previousCommandIndex = comScript.getScriptCommandIndex(previousCommand,
        caseInsensitive, separateWithASpace);

      if (previousCommandIndex == -1) {
        if (optionalPreviousCommand) {
          return false;
        }
        String[] errorMessage = new String[2];
        errorMessage[0] = "Unable to read " + comScript.getName() + ".  ";
        errorMessage[1] = "Unable to read " + command + " because the previous command, "
          + previousCommand + ", is missing.";
        UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
          "ComScriptManager Error", axisID);
        Utilities.timestamp("initialize", command, comScript, Utilities.FAILED_STATUS);
        return false;
      }

      ComScriptCommand comScriptCommand = null;
      int commandIndex = comScript.getScriptCommandIndex(command,
        previousCommandIndex + 1, caseInsensitive, separateWithASpace);

      if (commandIndex == -1) {
        Utilities.timestamp("initialize", command, comScript, Utilities.FAILED_STATUS);
        return false;
      }
      if (optionalCommand) {
        if (comScript.getScriptCommandIndex(command, previousCommandIndex + 1,
          caseInsensitive, separateWithASpace) == -1) {
          Utilities.timestamp("initialize", command, comScript, Utilities.FAILED_STATUS);
          return false;
        }
      }
      try {
        param.parseComScriptCommand(comScript.getScriptCommand(command, commandIndex,
          false, caseInsensitive, separateWithASpace));
      }
      catch (Exception except) {
        except.printStackTrace();
        String[] errorMessage = new String[4];
        errorMessage[0] = "Com file: " + comScript.getComFileName();
        errorMessage[1] = "Command: " + command + " after " + previousCommand;
        errorMessage[2] = except.getClass().getName();
        errorMessage[3] = except.getMessage();
        JOptionPane.showMessageDialog(null, errorMessage,
          "Com Script Command Parse Error", JOptionPane.ERROR_MESSAGE);
        Utilities.timestamp("initialize", command, comScript, Utilities.FAILED_STATUS);
        return false;
      }
    }
    Utilities.timestamp("initialize", command, comScript, Utilities.FINISHED_STATUS);
    return true;
  }

  /**
   * Initialize the CommandParam object from the specified command in the
   * comscript.  True is returned if the initialization is successful, false if
   * the initialization fails.
   * 
   * @param param
   * @param comScript
   * @param command
   * @param axisID
   * @param optionalCommand - missing command returns false, no exception thrown
   * @param optionalPreviousCommand - missing previous command returns false, no exception thrown
   * @return boolean
   */
  static boolean initialize(final BaseManager manager, final CommandParam param,
    final ComScript comScript, final String command, final AxisID axisID,
    final boolean optionalCommand, final String previousCommand,
    final boolean optionalPreviousCommand, final boolean caseInsensitive,
    final boolean separateWithASpace, final boolean required,
    final String requiredOption) {
    if (previousCommand == null) {
      return initialize(manager, param, comScript, command, axisID, caseInsensitive,
        separateWithASpace, required, requiredOption);
    }
    Utilities.timestamp("initialize", command, comScript, Utilities.STARTED_STATUS);
    if (!comScript.isCommandLoaded()) {
      param.initializeDefaults();
    }
    else {
      // locate previous command
      ComScriptCommand previousComScriptCommand = null;
      int previousCommandIndex = comScript.getScriptCommandIndex(previousCommand,
        caseInsensitive, separateWithASpace);

      if (previousCommandIndex == -1) {
        if (optionalPreviousCommand) {
          return false;
        }
        String[] errorMessage = new String[2];
        errorMessage[0] = "Unable to read " + comScript.getName() + ".  ";
        errorMessage[1] = "Unable to read " + command + " because the previous command, "
          + previousCommand + ", is missing.";
        UIHarness.INSTANCE.openMessageDialog(manager, errorMessage,
          "ComScriptManager Error", axisID);
        Utilities.timestamp("initialize", command, comScript, Utilities.FAILED_STATUS);
        return false;
      }

      ComScriptCommand comScriptCommand = null;
      int commandIndex = comScript.getScriptCommandIndex(command,
        previousCommandIndex + 1, caseInsensitive, separateWithASpace);

      if (commandIndex == -1) {
        Utilities.timestamp("initialize", command, comScript, Utilities.FAILED_STATUS);
        return false;
      }
      if (optionalCommand) {
        if (comScript.getScriptCommandIndex(command, previousCommandIndex + 1,
          caseInsensitive, separateWithASpace) == -1) {
          Utilities.timestamp("initialize", command, comScript, Utilities.FAILED_STATUS);
          return false;
        }
      }
      try {
        param.parseComScriptCommand(comScript.getScriptCommand(command, commandIndex,
          false, caseInsensitive, separateWithASpace));
      }
      catch (Exception except) {
        except.printStackTrace();
        String[] errorMessage = new String[4];
        errorMessage[0] = "Com file: " + comScript.getComFileName();
        errorMessage[1] = "Command: " + command + " after " + previousCommand;
        errorMessage[2] = except.getClass().getName();
        errorMessage[3] = except.getMessage();
        JOptionPane.showMessageDialog(null, errorMessage,
          "Com Script Command Parse Error", JOptionPane.ERROR_MESSAGE);
        Utilities.timestamp("initialize", command, comScript, Utilities.FAILED_STATUS);
        return false;
      }
    }
    Utilities.timestamp("initialize", command, comScript, Utilities.FINISHED_STATUS);
    return true;
  }
}