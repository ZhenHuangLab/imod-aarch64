package etomo.type;

import java.util.Properties;

/**
 * <p>Description: Meta data for a row of the BatchRunTomo Interface table.</p>
 * <p>Copyright: Copyright 2014 by the Regents of the University of Colorado</p>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class BatchRunTomoRowMetaData {
  private static final String GROUP_KEY = "row";
  private static final String ROW_NUMBER_KEY = "RowNumber";
  private static final String DATASET_STATE_KEY = "DatasetStatus";
  private static final String ENDING_STEP_KEY = "EndingStep";

  private final EtomoNumber rowNumber = new EtomoNumber(ROW_NUMBER_KEY);
  private final EtomoNumber dual = new EtomoNumber(EtomoNumber.Type.BOOLEAN, "dual");
  private final StringProperty bskip = new StringProperty("bskip");
  private final EtomoNumber run = new EtomoNumber(EtomoNumber.Type.BOOLEAN, "Run");
  private final StringProperty origStack = new StringProperty("OrigStack");
  private final EtomoBoolean2 etomoEnabled = new EtomoBoolean2("Etomo.Enabled");
  private final EtomoBoolean2 recEnabled = new EtomoBoolean2("Rec.Enabled");
  private final EtomoBoolean2 logEnabled = new EtomoBoolean2("Log.Enabled");
  private final EtomoBoolean2 dualEnabled = new EtomoBoolean2("dual.Enabled");
  private final EtomoBoolean2 tomogramDone = new EtomoBoolean2("Tomogram.Done");
  private final EtomoBoolean2 trimvolDone = new EtomoBoolean2("Trimvol.Done");

  private BatchRunTomoDatasetMetaData datasetMetaData = null;
  private BatchRunTomoDatasetState datasetState = null;
  private EndingStep endingStep = null;

  private final String stackID;

  BatchRunTomoRowMetaData(final String stackID) {
    this.stackID = stackID;
  }

  public String toString() {
    return rowNumber.toString();
  }

  /**
   * This function is used to decide whether to load the instance associated with stackID.
   *
   * @return
   */
  public static boolean isRowNumberNull(final Properties props, String prepend,
    final String stackID) {
    prepend = createPrepend(prepend, stackID);
    EtomoNumber number = new EtomoNumber(ROW_NUMBER_KEY);
    number.load(props, prepend);
    return number.isNull();
  }

  private static String getGroupKey(final String stackID) {
    return GROUP_KEY + "." + stackID;
  }

  private static String createPrepend(String prepend, final String stackID) {
    if (prepend == null || prepend.matches("\\s*")) {
      return getGroupKey(stackID);
    }
    prepend = prepend.trim();
    if (prepend.endsWith(".")) {
      return prepend + getGroupKey(stackID);
    }
    return prepend + "." + getGroupKey(stackID);
  }

  private String getGroupKey() {
    return getGroupKey(stackID);
  }

  private String createPrepend(String prepend) {
    return createPrepend(prepend, stackID);
  }

  public void load(final Properties props, String prepend) {
    // reset
    rowNumber.reset();
    dual.reset();
    bskip.reset();
    run.reset();
    datasetState = null;
    endingStep = null;
    origStack.reset();
    etomoEnabled.reset();
    recEnabled.reset();
    logEnabled.reset();
    dualEnabled.set(true);
    tomogramDone.reset();
    trimvolDone.reset();
    prepend = createPrepend(prepend);
    String group = prepend + ".";
    rowNumber.load(props, prepend);
    dual.load(props, prepend);
    bskip.load(props, prepend);
    run.load(props, prepend);
    datasetState = BatchRunTomoDatasetState
      .getInstance(props.getProperty(group + DATASET_STATE_KEY));
    endingStep = EndingStep.getInstance(props.getProperty(group + ENDING_STEP_KEY));
    origStack.load(props, prepend);
    etomoEnabled.load(props, prepend);
    recEnabled.load(props, prepend);
    logEnabled.load(props, prepend);
    tomogramDone.load(props, prepend);
    trimvolDone.load(props, prepend);
    if (BatchRunTomoDatasetMetaData.exists(props, prepend)) {
      if (datasetMetaData == null) {
        datasetMetaData = new BatchRunTomoDatasetMetaData();
      }
      datasetMetaData.load(props, prepend);
    }
  }

  public void store(final Properties props, String prepend) {
    prepend = createPrepend(prepend);
    String group = prepend + ".";
    rowNumber.store(props, prepend);
    if (!rowNumber.isNull()) {
      dual.store(props, prepend);
      bskip.store(props, prepend);
      run.store(props, prepend);
      if (datasetState != null) {
        props.setProperty(group + DATASET_STATE_KEY, datasetState.getKey());
      }
      else {
        props.remove(group + DATASET_STATE_KEY);
      }
      if (endingStep != null) {
        props.setProperty(group + ENDING_STEP_KEY, endingStep.getValue().toString());
      }
      else {
        props.remove(group + ENDING_STEP_KEY);
      }
      origStack.store(props, prepend);
      etomoEnabled.store(props, prepend);
      recEnabled.store(props, prepend);
      logEnabled.store(props, prepend);
      tomogramDone.store(props, prepend);
      trimvolDone.store(props, prepend);
      if (datasetMetaData != null) {
        datasetMetaData.store(props, prepend);
      }
    }
  }

  public void setEtomoEnabled(final boolean input) {
    etomoEnabled.set(input);
  }

  public void setRecEnabled(final boolean input) {
    recEnabled.set(input);
  }

  public void setLogEnabled(final boolean input) {
    logEnabled.set(input);
  }

  public void setTomogramDone(final boolean input) {
    tomogramDone.set(input);
  }

  public void setTrimvolDone(final boolean input) {
    trimvolDone.set(input);
  }

  public boolean isEtomoEnabled() {
    return etomoEnabled.is();
  }

  public boolean isRecEnabled() {
    return recEnabled.is();
  }

  public boolean isLogEnabled() {
    return logEnabled.is();
  }

  public boolean isTomogramDone() {
    return tomogramDone.is();
  }

  public boolean isTrimvolDone() {
    return trimvolDone.is();
  }

  public boolean isDualEnabled() {
    return dualEnabled.is();
  }

  public void setOrigStack(final String input) {
    origStack.set(input);
  }

  public void setEndingStep(final EndingStep input) {
    endingStep = input;
  }

  public void setDatasetState(final BatchRunTomoDatasetState input) {
    datasetState = input;
  }

  public String getOrigStack() {
    return origStack.toString();
  }

  public EndingStep getEndingStep() {
    return endingStep;
  }

  public BatchRunTomoDatasetState getDatasetState() {
    return datasetState;
  }

  public String getStackID() {
    return stackID;
  }

  public void setDatasetDialog(final boolean input) {
    if (input && datasetMetaData == null) {
      datasetMetaData = new BatchRunTomoDatasetMetaData();
    }
    if (datasetMetaData != null) {
      datasetMetaData.setDataset(input);
    }
  }

  public boolean isDatasetDialog() {
    return datasetMetaData != null;
  }

  public BatchRunTomoDatasetMetaData getDatasetMetaData() {
    if (datasetMetaData == null) {
      datasetMetaData = new BatchRunTomoDatasetMetaData();
    }
    return datasetMetaData;
  }

  public void setRowNumber(final String input) {
    rowNumber.set(input);
  }

  public int getRowNumber() {
    return rowNumber.getInt();
  }

  public boolean isRowNumberNull() {
    return rowNumber.isNull();
  }

  public void setDual(final boolean input) {
    dual.set(input);
  }

  public void setDualEnabled(final boolean input) {
    dualEnabled.set(input);
  }

  public ConstEtomoNumber getDual() {
    return dual;
  }

  public void setBskip(final String input) {
    bskip.set(input);
  }

  public String getBskip() {
    return bskip.toString();
  }

  public void setRun(final boolean input) {
    run.set(input);
  }

  public ConstEtomoNumber getRun() {
    return run;
  }
}
