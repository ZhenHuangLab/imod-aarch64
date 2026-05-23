package etomo.comscript;

import etomo.JoinManager;
import etomo.type.EtomoNumber;
import etomo.type.ScriptParameter;
import etomo.type.StringParameter;

public final class Joinwarp2modelParam implements CommandParam {

  public static final String COMMAND_NAME = "joinwarp2model";

  private final StringParameter joinedFile = new StringParameter("JoinedFile");
  private final StringParameter appliedTransformFile =
    new StringParameter("AppliedTransformFile");
  private final StringParameter offsetInXandY = new StringParameter("OffsetInXandY");
  private final StringParameter chunkSizes = new StringParameter("ChunkSizes");
  private final StringParameter inputWarpFile = new StringParameter("InputWarpFile");
  private final StringParameter ouputModelFile = new StringParameter("OutputModelFile");

  private final ScriptParameter binningOfJoin =
    new ScriptParameter(EtomoNumber.Type.INTEGER, "BinningOfJoin");

  private final JoinManager manager;

  public Joinwarp2modelParam(JoinManager manager) {
    this.manager = manager;
  }

  @Override
  public void parseComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException, FortranInputSyntaxException, InvalidParameterException {
    scriptCommand.useKeywordValue();
    initializeDefaults();
    joinedFile.parse(scriptCommand);
    appliedTransformFile.parse(scriptCommand);
    offsetInXandY.parse(scriptCommand);
    binningOfJoin.parse(scriptCommand);
    chunkSizes.parse(scriptCommand);
    inputWarpFile.parse(scriptCommand);
    ouputModelFile.parse(scriptCommand);
  }

  @Override
  public void updateComScriptCommand(ComScriptCommand scriptCommand)
    throws BadComScriptException {
    scriptCommand.useKeywordValue();
    ouputModelFile.updateComScript(scriptCommand);
    joinedFile.updateComScript(scriptCommand);
    appliedTransformFile.updateComScript(scriptCommand);
    offsetInXandY.updateComScript(scriptCommand);
    binningOfJoin.updateComScript(scriptCommand);
    chunkSizes.updateComScript(scriptCommand);
    inputWarpFile.updateComScript(scriptCommand);
  }

  @Override
  public void initializeDefaults() {
    joinedFile.reset();
    appliedTransformFile.reset();
    offsetInXandY.reset();
    binningOfJoin.reset();
    chunkSizes.reset();
    inputWarpFile.reset();
    ouputModelFile.reset();
  }

  public void setRefineModelFilename(final String input) {
    ouputModelFile.set(input);
  }

  public void setModeledJoinFilename(final String input) {
    joinedFile.set(input);
  }

  public void setInputWarpFilename(final String input) {
    inputWarpFile.set(input);
  }

  public void setAppliedTransformFilename(final String input) {
    appliedTransformFile.set(input);
  }

  public void setBinningOfJoin(final String input) {
    binningOfJoin.set(input);
  }

  public void setOffsetInXandY(final String input) {
    offsetInXandY.set(input);
  }

  public void setChunkSizes(final String input) {
    chunkSizes.set(input);
  }

  public String getCommandName() {
    return COMMAND_NAME.toString();
  }

}
