package etomo.type;

import java.io.File;
import java.util.Iterator;
import java.util.Properties;

import etomo.BatchRunTomoManager;
import etomo.ui.DialogCompleteClient;
import etomo.ui.DialogCompleteListener;
import etomo.ui.LogProperties;
import etomo.util.DatasetFiles;

/**
 * <p>Description: Main dialog for the batchruntomo interface.</p>
 * 
 * <p>Copyright: Copyright 2014 - 2022 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class BatchRunTomoMetaData extends BaseMetaData
  implements DialogCompleteClient {
  public static final String NEW_TITLE = "Batch Run Tomo";
  private static final String ENDING_STEP_KEY = "EndingStep";
  private static final String EARLIEST_RUN_KEY = "EarliestRun";
  private static final String STATUS_KEY = "Status";
  private static final String STARTING_STEP_KEY = "StartingStep";

  private StringProperty rootName = new StringProperty("RootName");
  private StringProperty deliverToDirectory = new StringProperty("DeliverToDirectory");
  private StringProperty inputDirectiveFile = new StringProperty("InputDirectiveFile");
  // Key is stackID
  private final OrderedHashMap<String, BatchRunTomoRowMetaData> rowMetaDataMap =
    new OrderedHashMap<String, BatchRunTomoRowMetaData>();
  // metadata for the global dataset dialog
  private final BatchRunTomoDatasetMetaData datasetMetaData =
    new BatchRunTomoDatasetMetaData();
  private final PanelHeaderSettings datasetTableHeader =
    new PanelHeaderSettings("datasetTableHeader");
  private EtomoBoolean2 enableStartingStep = new EtomoBoolean2("EnableStartingStep");
  private EtomoBoolean2 useEndingStep = new EtomoBoolean2(ENDING_STEP_KEY + ".Use");
  private EtomoBoolean2 useStartingStep = new EtomoBoolean2(STARTING_STEP_KEY + ".Use");
  private StringProperty browsingDirectory = new StringProperty("BrowsingDirectory");
  private EtomoBoolean2 delivered = new EtomoBoolean2("Delivered");

  private final TableReference tableReference;
  private final BatchRunTomoManager manager;

  private EndingStep earliestRunEndingStep = null;
  private BatchRunTomoStatus status = BatchRunTomoStatus.DEFAULT;
  private EndingStep endingStep = null;
  private StartingStep startingStep = null;

  public BatchRunTomoMetaData(final BatchRunTomoManager manager,
    final LogProperties logProperties, final TableReference tableReference,
    final boolean newDataset) {
    super(manager, logProperties, false, newDataset, false);
    this.manager = manager;
    this.tableReference = tableReference;
    axisType = AxisType.SINGLE_AXIS;
    fileExtension = DataFileType.BATCH_RUN_TOMO.extension;
  }

  public void setName(final String rootName) {
    this.rootName.set(rootName);
  }

  public boolean isRootNameNull() {
    return rootName.isEmpty();
  }

  public String getDatasetName() {
    return rootName.toString();
  }

  public String getRootName() {
    return rootName.toString();
  }

  public void setRootName(final String input) {
    rootName.set(input);
  }

  public boolean isValid() {
    return validate() == null;
  }

  public static String getNewFileTitle() {
    return NEW_TITLE;
  }

  /**
   * returns null if valid
   *
   * @return error message if invalid
   */
  public String validate() {
    if (rootName.isEmpty()) {
      return "Missing root name.";
    }
    return null;
  }

  public String getMetaDataFileName() {
    if (rootName.isEmpty()) {
      return null;
    }
    return DatasetFiles.getBatchRunTomoDataFileName(rootName.toString());
  }

  String getGroupKey() {
    return "meta";
  }

  public String getName() {
    if (rootName.isEmpty()) {
      return NEW_TITLE;
    }
    return rootName.toString();
  }

  /**
   * Better quality createPrepend.  Parent createPrepend cannot be improved without
   * breaking backwards compatibility.
   */
  public String createPrepend(String prepend) {
    if (prepend == null || prepend.matches("\\s*")) {
      return getGroupKey();
    }
    prepend = prepend.trim();
    if (prepend.endsWith(".")) {
      return prepend + getGroupKey();
    }
    return prepend + "." + getGroupKey();
  }

  public void load(final Properties props, final String parentPrepend) {
    super.load(props, parentPrepend);
    // reset
    rootName.reset();
    deliverToDirectory.reset();
    inputDirectiveFile.reset();
    datasetTableHeader.reset();
    earliestRunEndingStep = null;
    status = null;
    enableStartingStep.reset();
    useEndingStep.reset();
    rowMetaDataMap.clear();
    endingStep = null;
    startingStep = null;
    useStartingStep.reset();
    browsingDirectory.reset();
    delivered.reset();
    // load
    String prepend = createPrepend(parentPrepend);
    String group = prepend + ".";
    rootName.load(props, prepend);
    deliverToDirectory.load(props, prepend);
    inputDirectiveFile.load(props, prepend);
    datasetTableHeader.load(props, prepend);
    datasetMetaData.load(props, prepend);
    tableReference.load(props, prepend);
    earliestRunEndingStep = EndingStep
      .getInstance(props.getProperty(group + ENDING_STEP_KEY + "." + EARLIEST_RUN_KEY));
    Iterator<String> iterator = tableReference.idIterator();
    status = BatchRunTomoStatus.getInstance(props.getProperty(group + STATUS_KEY));
    enableStartingStep.load(props, prepend);
    useEndingStep.load(props, prepend);
    endingStep = EndingStep.getInstance(props.getProperty(group + ENDING_STEP_KEY));
    startingStep = StartingStep.getInstance(props.getProperty(group + STARTING_STEP_KEY));
    useStartingStep.load(props, prepend);
    browsingDirectory.load(props, prepend);
    delivered.load(props, prepend);
    while (iterator.hasNext()) {
      String stackID = iterator.next();
      if (!BatchRunTomoRowMetaData.isRowNumberNull(props, prepend, stackID)) {
        BatchRunTomoRowMetaData rowMetaData = new BatchRunTomoRowMetaData(stackID);
        rowMetaData.load(props, prepend);
        rowMetaDataMap.put(rowMetaData.getRowNumber(), stackID, rowMetaData);
      }
    }
    // Bug# 2403 - complex image filename style correct must be done in child class.
    checkImageFilenameStyleLoaded(prepend);
  }

  @Override
  void checkImageFilenameStyleLoaded(final String parentPrepend) {
    if (!wasImageFilenameStyleLoaded()
      && !repairImageFilenameStyleFromParam(parentPrepend)) {
      repairImageFilenameStyle(parentPrepend);
    }
  }

  /**
   * If possible uses the batchruntomo comfile to repair imageFilenameStyle [Bug# 2386].
   * Repair for all brt datasets created before the Bug# 2388 fix is needed because
   * BaseMetaData was never saved.  Before the Bug# 2388 fix, once this dataset was
   * reloaded, the incorrect image filename style made this dataset incompatible from
   * datasets it created before it was first reloaded.
   * 
   * After it was loaded without a saved imageFilenameStyle it always sets the naming
   * style in the brt comfile to OLD.  So if the naming style is set to OLD, or not set at
   * all, then it can be repaired via the brt comfile.
   * @param parentPrepend prepend needed to generate the property key
   * @preturn true if filename style didn't need repair or was repaired.
   */
  public boolean repairImageFilenameStyleFromParam(final String prepend) {
    if (wasImageFilenameStyleLoaded()) {
      return true;
    }
    // An incorrect image filename style makes this dataset incompatible with the
    // datasets it created in the past.
    ConstEtomoNumber namingStyle = manager.getBatchruntomoComfileNamingStyle();
    if (namingStyle != null && !namingStyle.isValid()) {
      return false;
    }
    ImageFilenameStyle imageFilenameStyle = null;
    String repairType = null;
    if (namingStyle == null || namingStyle.isNull()) {
      // If namingStyle in the brt comfile is missing, then this dataset was created
      // before image filename standardization and never reloaded until now. Its
      // correct image filename style is "OLD".
      imageFilenameStyle = ImageFilenameStyle.OLD;
      repairType = "old image file name style\n";
    }
    else {
      imageFilenameStyle = ImageFilenameStyle.getInstance(namingStyle);
      if (imageFilenameStyle != ImageFilenameStyle.OLD) {
        // If namingStyle in the brt comfile is not 0 (old style), then this dataset
        // was created after image filename standardization and never reloaded until
        // now. Its correct image filename style is the whatever was used when it was
        // originally created.
        repairType = "image file name style from the batchruntomo\ncomfile ";
      }
    }
    if (repairType == null) {
      return false;
    }
    String key = getImageFilenameStyleKey(prepend);
    System.err.println("\nINFO: Attempting to repair the " + key
      + " property (which is missing from the\ndataset file) based on the batchruntomo "
      + "comfile.  Using the " + repairType + "to set the " + key + " to "
      + imageFilenameStyle + ".  [Bug# 2386]\n");
    return correctImageFilenameStyle(prepend, imageFilenameStyle);
  }

  public void msgDialogComplete(final String prepend) {
    repairImageFilenameStyle(prepend);
  }

  /**
   * Uses the created datasts or the environment (as a fallback) to repair
   * imageFilenameStyle [Bug# 2386].  Repair for all brt datasets created before the
   * Bug# 2388 fix is needed because BaseMetaData was never saved.  Before the Bug# 2388
   * fix, once this dataset was reloaded, the incorrect image filename style made this
   * dataset incompatible from datasets it created before it was first reloaded.
   * 
   * This repair only looks at one dataset, ignoring the situation where there are
   * different namingStyles in different datasets.  This is because that situation is
   * considered to be unlikely.  If this situation did happen, it would be caused by some
   * of the datasets being created before any reloading, and other being created after a
   * reload.
   * @param parentPrepend prepend needed to generate the property key
   */
  // Bug# 2403
  @Override
  void repairImageFilenameStyle(final String prepend) {
    if (wasImageFilenameStyleLoaded()) {
      return;
    }
    if (!manager.isSetupDone()) {
      // This repair cannot be done until the dialog containing the dataset table is
      // complete.
      manager.addDialogCompleteListener(new DialogCompleteListener(this, prepend));
      return;
    }
    String repairType = null;
    // Get the naming style from one of the datasets.
    ImageFilenameStyle imageFilenameStyle = manager.getDatasetImageFilenameStyle();
    if (imageFilenameStyle != null) {
      repairType =
        "image file name style from one of the datasets generated by this project";
    }
    else {
      // Fallback: use the environment's naming style
      imageFilenameStyle = ImageFileMetaData.getTempInstance().getImageFilenameStyle();
      repairType = "image file name style from the environment";
    }
    String key = getImageFilenameStyleKey(prepend);
    if (imageFilenameStyle != null) {
      System.err.println("\nINFO: Attempting to repair the " + key
        + " property, which is missing from the\ndataset file.  Using the " + repairType
        + " and\nsetting " + key + " to " + imageFilenameStyle + ".  [Bug# 2386]\n");
      if (correctImageFilenameStyle(prepend, imageFilenameStyle)) {
        return;
      }
    }
    System.err.println("\nERROR: Unable to repair the " + key
      + " property, which is missing from the\ndataset file."
      + (repairType != null ? "  Unable to use the " + repairType : null)
      + ".\n[Bug# 2386]\n");
  }

  public void store(Properties props, String prepend) {
    super.store(props, prepend);
    prepend = createPrepend(prepend);
    String group = prepend + ".";
    rootName.store(props, prepend);
    deliverToDirectory.store(props, prepend);
    inputDirectiveFile.store(props, prepend);
    datasetTableHeader.store(props, prepend);
    datasetMetaData.store(props, prepend);
    tableReference.store(props, prepend);
    if (earliestRunEndingStep != null) {
      props.setProperty(group + ENDING_STEP_KEY + "." + EARLIEST_RUN_KEY,
        earliestRunEndingStep.getValue().toString());
    }
    else {
      props.remove(group + ENDING_STEP_KEY + "." + EARLIEST_RUN_KEY);
    }
    if (status != null) {
      props.setProperty(group + STATUS_KEY, status.getText().toString());
    }
    else {
      props.remove(group + STATUS_KEY);
    }
    enableStartingStep.store(props, prepend);
    useEndingStep.store(props, prepend);
    if (endingStep != null) {
      props.setProperty(group + ENDING_STEP_KEY, endingStep.getValue().toString());
    }
    else {
      props.remove(group + ENDING_STEP_KEY);
    }
    if (startingStep != null) {
      props.setProperty(group + STARTING_STEP_KEY, startingStep.getValue().toString());
    }
    else {
      props.remove(group + STARTING_STEP_KEY);
    }
    useStartingStep.store(props, prepend);
    browsingDirectory.store(props, prepend);
    delivered.store(props, prepend);
    Iterator<BatchRunTomoRowMetaData> iterator = rowMetaDataMap.values().iterator();
    while (iterator.hasNext()) {
      iterator.next().store(props, prepend);
    }
  }

  public void setUseStartingStep(final boolean input) {
    useStartingStep.set(input);
  }

  public void setUseEndingStep(final boolean input) {
    useEndingStep.set(input);
  }

  public String getBrowsingDirectory() {
    return browsingDirectory.toString();
  }

  public boolean isDelivered() {
    return delivered.is();
  }

  public void setDelivered(final boolean input) {
    delivered.set(input);
  }

  public void setBrowsingDirectory(final File input) {
    if (input == null) {
      browsingDirectory.reset();
    }
    else {
      browsingDirectory.set(input.getAbsolutePath());
    }
  }

  public void setStartingStep(final StartingStep input) {
    startingStep = input;
  }

  public void setEndingStep(final EndingStep input) {
    endingStep = input;
  }

  public boolean isUseStartingStep() {
    return useStartingStep.is();
  }

  public boolean isUseEndingStep() {
    return useEndingStep.is();
  }

  public StartingStep getStartingStep() {
    return startingStep;
  }

  public EndingStep getEndingStep() {
    return endingStep;
  }

  public void setEnableStartingStep(final boolean input) {
    enableStartingStep.set(input);
  }

  public boolean isEnableStartingStep() {
    return enableStartingStep.is();
  }

  public void setStatus(final BatchRunTomoStatus input) {
    status = input;
  }

  public void setEarliestRunEndingStep(final EndingStep input) {
    earliestRunEndingStep = input;
  }

  public BatchRunTomoStatus getStatus() {
    return status;
  }

  public EndingStep getEarliestRunEndingStep() {
    return earliestRunEndingStep;
  }

  public OrderedHashMap.ReadOnlyArray<BatchRunTomoRowMetaData> getOrderedRows() {
    return rowMetaDataMap.orderedValues();
  }

  /**
   * Gets the rowMetaData for this stackID.  If it doesn't exist, create it and add it to
   * the map.  Do not add the ordinal at this point.  The order of the rows only matters
   * when they are being loaded into the table.
   * @param stackID
   * @return
   */
  public BatchRunTomoRowMetaData getRowMetaData(final String stackID) {
    BatchRunTomoRowMetaData rowMetaData = rowMetaDataMap.get(stackID);
    if (rowMetaData == null) {
      rowMetaData = new BatchRunTomoRowMetaData(stackID);
      rowMetaDataMap.put(stackID, rowMetaData);
    }
    return rowMetaData;
  }

  public BatchRunTomoDatasetMetaData getDatasetMetaData() {
    return datasetMetaData;
  }

  public boolean isRowNumberNull(final String stackID) {
    BatchRunTomoRowMetaData rowMetaData = rowMetaDataMap.get(stackID);
    return rowMetaData == null || rowMetaData.isRowNumberNull();
  }

  public ConstPanelHeaderSettings getDatasetTableHeader() {
    return datasetTableHeader;
  }

  public void setDatasetTableHeader(final ConstPanelHeaderSettings input) {
    datasetTableHeader.set(input);
  }

  public String getDeliverToDirectory() {
    return deliverToDirectory.toString();
  }

  public void setDeliverToDirectory(final File input) {
    if (input != null) {
      deliverToDirectory.set(input.getAbsolutePath());
    }
    else {
      deliverToDirectory.reset();
    }
  }

  public String getInputDirectiveFile() {
    return inputDirectiveFile.toString();
  }

  public void setInputDirectiveFile(final File input) {
    if (input != null) {
      inputDirectiveFile.set(input.getAbsolutePath());
    }
    else {
      inputDirectiveFile.reset();
    }
  }
}
