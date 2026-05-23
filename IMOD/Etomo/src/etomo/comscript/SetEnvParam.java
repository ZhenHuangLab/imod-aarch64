package etomo.comscript;

import java.util.ArrayList;

import etomo.type.ImodOutputFormat;

public final class SetEnvParam implements CommandParam {
  public static final String COMMAND_NAME = "setenv";
  private static final int NAME_INDEX = 0;
  private static final int VALUE_INDEX = 1;

  private final String name;// fixed - command needs to match

  private boolean valid = true;
  private String value = null;

  public SetEnvParam(final String name) {
    this.name = name;
  }

  public String toString() {
    return "[name:" + name + ",value:" + value + ",valid:" + valid + "]";
  }

  public ImodOutputFormat getImodOutputFormatValue() {
    return ImodOutputFormat.getInstance(value);
  }

  public void initializeDefaults() {
    valid = true;
    value = null;
  }

  public void parseComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException, FortranInputSyntaxException, InvalidParameterException {
    // TODO error checking - throw exceptions for bad syntax
    String[] cmdLineArgs = scriptCommand.getCommandLineArgs();
    initializeDefaults();
    if (cmdLineArgs[NAME_INDEX] != null && !cmdLineArgs[NAME_INDEX].equals(name)) {
      valid = false;
    }
    if (cmdLineArgs.length > VALUE_INDEX) {
      value = cmdLineArgs[VALUE_INDEX];
    }
  }

  public void updateComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException {
    ArrayList cmdLineArgs = new ArrayList(VALUE_INDEX + 1);
    cmdLineArgs.add(name);
    cmdLineArgs.add(value);
    scriptCommand
      .setCommandLineArgs((String[]) cmdLineArgs.toArray(new String[cmdLineArgs.size()]));
  }

  public void setValue(final String value) {
    this.value = value;
  }
}
