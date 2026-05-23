package etomo.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTextField;

import etomo.BatchRunTomoManager;
import etomo.EtomoDirector;
import etomo.comscript.BatchruntomoParam;
import etomo.logic.BatchTool;
import etomo.logic.DatasetTool;
import etomo.storage.DirectiveDef;
import etomo.storage.DirectiveFile;
import etomo.storage.DirectiveFileCollection;
import etomo.storage.DirectiveFileInterface;
import etomo.storage.LogFile;
import etomo.storage.NameValuePairList;
import etomo.storage.autodoc.Autodoc;
import etomo.storage.autodoc.AutodocFactory;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.BatchRunTomoDatasetState;
import etomo.type.BatchRunTomoDatasetStatus;
import etomo.type.BatchRunTomoMetaData;
import etomo.type.BatchRunTomoRowMetaData;
import etomo.type.BatchRunTomoRowStatus;
import etomo.type.BatchRunTomoStatus;
import etomo.type.DirectiveFileType;
import etomo.type.EndingStep;
import etomo.type.Extension;
import etomo.type.FileType;
import etomo.type.FrameStatus;
import etomo.type.ProcessName;
import etomo.type.Run3dmodMenuOptions;
import etomo.type.Status;
import etomo.type.StatusChangeEvent;
import etomo.type.StatusChangeListener;
import etomo.type.StatusChangeTaggedEvent;
import etomo.type.StatusChanger;
import etomo.type.Step;
import etomo.type.UserConfiguration;
import etomo.ui.BatchRunTomoTab;
import etomo.ui.Field;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.PreferredTableSize;
import etomo.ui.SharedStrings;
import etomo.util.UniqueKey;
import etomo.util.Utilities;

/**
 * <p>Description: A row of the BatchRunTomo table.</p>
 * 
 * <p>Copyright: Copyright 2014 - 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class BatchRunTomoRow implements Highlightable, Run3dmodButtonContainer,
  ActionListener, StatusChangeListener, StatusChanger {
  private static final String SURFACES_TO_ANALYZE_TWO = "2";
  private static final String SURFACES_TO_ANALYZE_ONE = "1";
  private static final URL IMOD_ICON_URL =
    ClassLoader.getSystemResource("images/b3dicon.png");
  private static final URL IMOD_DISABLED_ICON_URL =
    ClassLoader.getSystemResource("images/b3dicon-disabled.png");
  private static final URL IMOD_PRESSED_ICON_URL =
    ClassLoader.getSystemResource("images/b3dicon-pressed.png");
  private static final URL ETOMO_ICON_URL =
    ClassLoader.getSystemResource("images/etomoicon.png");
  private static final URL ETOMO_DISABLED_ICON_URL =
    ClassLoader.getSystemResource("images/etomoicon-disabled.png");
  private static final URL ETOMO_PRESSED_ICON_URL =
    ClassLoader.getSystemResource("images/etomoicon-pressed.png");
  private static final String EDIT_DATASET_VALUE = "   Set";
  private static final URL LOG_ICON_URL =
    ClassLoader.getSystemResource("images/logicon.png");
  private static final URL LOG_DISABLED_ICON_URL =
    ClassLoader.getSystemResource("images/logicon-disabled.png");
  private static final URL LOG_PRESSED_ICON_URL =
    ClassLoader.getSystemResource("images/logicon-pressed.png");

  private final CheckBoxCell cbcBoundaryModel = new CheckBoxCell();
  private final CheckBoxCell cbcDual = new CheckBoxCell();
  private final CheckBoxCell cbcMontage = new CheckBoxCell();
  private final FieldCell fcSkip = FieldCell.getEditableInstance();
  private final FieldCell fcBskip = FieldCell.getEditableInstance();
  private final CheckBoxCell cbcSurfacesToAnalyze2 = new CheckBoxCell();
  private final FieldCell fcEditDataset = FieldCell.getIneditableInstance();
  private final FieldCell fcDatasetState = FieldCell.getIneditableInstance();
  private final FieldCell fcEndingStep = FieldCell.getIneditableInstance();
  private final CheckBoxCell cbcRun = new CheckBoxCell();
  private final MinibuttonCell mbcEtomo =
    MinibuttonCell.getInstance(new ImageIcon(ETOMO_ICON_URL));
  private final MinibuttonCell mbc3dmodA =
    MinibuttonCell.getRun3dmodInstance(new ImageIcon(IMOD_ICON_URL), this);
  private final MinibuttonCell mbc3dmodB =
    MinibuttonCell.getRun3dmodInstance(new ImageIcon(IMOD_ICON_URL), this);
  private final HeaderCell hcNumber = new HeaderCell();
  private final ButtonCell bcEditDataset = ButtonCell.getToggleInstance("Open");
  private final boolean[] firstSteps = new boolean[BatchRunTomoStepPanel.STEP_PAIRS];
  private final MinibuttonCell mbcRec =
    MinibuttonCell.getInstance(new ImageIcon(IMOD_ICON_URL));
  private final MinibuttonCell mbcLog =
    MinibuttonCell.getInstance(new ImageIcon(LOG_ICON_URL));

  private ArrayList<StatusChangeListener> listeners = null;

  private final HighlighterButton hbRow;
  private final FieldCell fcStack;
  private final String stackID;
  private final BatchRunTomoTable table;
  private final Set<DirectiveDef> basicDirectives;

  private int imodIndexA = -1;
  private int imodIndexB = -1;
  private int imodRec = -1;
  private int imodTrimVol = -1;
  private BatchRunTomoDatasetDialog datasetDialog = null;
  private BatchRunTomoRowMetaData metaData = null;
  private BatchRunTomoStatus status = BatchRunTomoStatus.DEFAULT;
  private boolean debug = false;
  private String origStack = null;
  private BatchRunTomoDatasetState datasetState = null;
  private EndingStep endingStep = null;
  private boolean tomogramDone = false;
  private boolean trimvolDone = false;
  private File log = null;
  private UniqueKey managerKey = null;

  private BatchRunTomoRow(final BatchRunTomoTable table,
    final Set<DirectiveDef> basicDirectives, final int number, final File stack,
    final BatchRunTomoRow prevRow, final AxisType axisType, final String stackID,
    final PreferredTableSize preferredTableSize) {
    this.stackID = stackID;
    this.table = table;
    this.basicDirectives = basicDirectives;
    hcNumber.setText(number);
    hbRow = HighlighterButton.getInstance(this, table);
    fcStack = FieldCell.getExpandableIneditableInstance(null);
    fcStack.setValue(stack);
    setLog(stack);
    // icons
    if (IMOD_DISABLED_ICON_URL != null) {
      mbc3dmodA.setDisabledIcon(new ImageIcon(IMOD_DISABLED_ICON_URL));
      mbc3dmodB.setDisabledIcon(new ImageIcon(IMOD_DISABLED_ICON_URL));
      mbcRec.setDisabledIcon(new ImageIcon(IMOD_DISABLED_ICON_URL));
    }
    if (IMOD_PRESSED_ICON_URL != null) {
      mbc3dmodA.setPressedIcon(new ImageIcon(IMOD_PRESSED_ICON_URL));
      mbc3dmodB.setPressedIcon(new ImageIcon(IMOD_PRESSED_ICON_URL));
      mbcRec.setPressedIcon(new ImageIcon(IMOD_PRESSED_ICON_URL));
    }
    if (ETOMO_DISABLED_ICON_URL != null) {
      mbcEtomo.setDisabledIcon(new ImageIcon(ETOMO_DISABLED_ICON_URL));
    }
    if (ETOMO_PRESSED_ICON_URL != null) {
      mbcEtomo.setPressedIcon(new ImageIcon(ETOMO_PRESSED_ICON_URL));
    }
    if (LOG_DISABLED_ICON_URL != null) {
      mbcLog.setDisabledIcon(new ImageIcon(LOG_DISABLED_ICON_URL));
    }
    if (LOG_PRESSED_ICON_URL != null) {
      mbcLog.setPressedIcon(new ImageIcon(LOG_PRESSED_ICON_URL));
    }
    // preferred width
    if (preferredTableSize != null) {
      preferredTableSize.addColumn(BatchRunTomoTable.DatasetColumn.NUMBER.getIndex(),
        hcNumber);
      preferredTableSize.addColumn(BatchRunTomoTable.DatasetColumn.STACK.getIndex(),
        fcStack);
      preferredTableSize.addColumn(
        BatchRunTomoTable.DatasetColumn.EDIT_DATASET.getIndex(), bcEditDataset,
        fcEditDataset);
    }
    // init
    fcEndingStep.setHorizontalAlignment(JTextField.CENTER);
    copy(prevRow);
    setTooltips(prevRow);
    // If axisType is set, use it to set dual checkbox, otherwise keep the
    // default
    // axisType from the previous row.
    if (axisType == AxisType.DUAL_AXIS) {
      cbcDual.setSelected(true);
    }
    else if (axisType == AxisType.SINGLE_AXIS) {
      cbcDual.setSelected(false);
    }
    cbcRun.setSelected(true);
    // set directives
    AxisID axisID = AxisID.getInstanceFromFileName(
      cbcDual.isSelected() ? AxisType.DUAL_AXIS : AxisType.SINGLE_AXIS,
      fcStack.getContractedValue());
    setupField(fcStack, axisID == AxisID.SECOND ? DirectiveDef.CURRENT_B_STACK_EXT
      : DirectiveDef.CURRENT_STACK_EXT);
    setupField(cbcDual, DirectiveDef.DUAL);
    setupField(cbcMontage, DirectiveDef.MONTAGE);
    setupField(cbcSurfacesToAnalyze2, DirectiveDef.SURFACES_TO_ANALYZE,
      DirectiveDef.TWO_SURFACES);
    setupField(fcSkip, DirectiveDef.SKIP);
    setupField(fcBskip,
      DirectiveDef.getInstanceFromCsv("setupset.copyarg.bskip", DirectiveDef.SKIP));
    // Also works with DirectiveDef.RAW_BOUNDARY_MODEL_FOR_PATCH_TRACKING
    setupField(cbcBoundaryModel, DirectiveDef.RAW_BOUNDARY_MODEL_FOR_SEED_FINDING,
      DirectiveDef.RAW_BOUNDARY_MODEL_FOR_PATCH_TRACKING);
    mbcEtomo.setEnabled(false);
    mbcRec.setEnabled(false);
    mbcLog.setEnabled(false);
    for (int i = 0; i < firstSteps.length; i++) {
      firstSteps[i] = false;
    }
    updateDisplay();
    statusChanged(status);
  }

  private void setupField(final Field field, final DirectiveDef directiveDef) {
    setupField(field, directiveDef, null);
  }

  private void setupField(final Field field, final DirectiveDef directiveDef,
    final DirectiveDef directiveDef2) {
    field.setDirectiveDef(directiveDef);
    if (basicDirectives != null) {
      if (directiveDef != null && !basicDirectives.contains(directiveDef)) {
        basicDirectives.add(directiveDef);
      }
      if (directiveDef2 != null && !basicDirectives.contains(directiveDef2)) {
        basicDirectives.add(directiveDef2);
      }
    }
  }

  static BatchRunTomoRow getInstance(final BatchRunTomoTable table,
    final Set<DirectiveDef> basicDirectives, final int number, final File stack,
    final BatchRunTomoRow prevRow, final AxisType axisType, final String stackID,
    final PreferredTableSize datasetWidth) {
    BatchRunTomoRow instance = new BatchRunTomoRow(table, basicDirectives, number, stack,
      prevRow, axisType, stackID, datasetWidth);
    instance.addListeners();
    return instance;
  }

  static BatchRunTomoRow getDefaultsInstance(final Set<DirectiveDef> basicDirectives) {
    return new BatchRunTomoRow(null, basicDirectives, -1, null, null, null, null, null);
  }

  void copy(final BatchRunTomoRow prevRow) {
    if (prevRow != null) {
      cbcDual.setSelected(prevRow.cbcDual.isSelected());
      cbcMontage.setSelected(prevRow.cbcMontage.isSelected());
      cbcSurfacesToAnalyze2.setSelected(prevRow.cbcSurfacesToAnalyze2.isSelected());
    }
    updateDisplay();
  }

  boolean validate(final BatchRunTomoDatasetDialog globalDatasetDialog) {
    boolean surfacesToAnalyze2 = cbcSurfacesToAnalyze2.isSelected();
    if (datasetDialog != null) {
      return datasetDialog.validate(surfacesToAnalyze2);
    }
    if (globalDatasetDialog != null) {
      return globalDatasetDialog.validate(surfacesToAnalyze2);
    }
    return true;
  }

  boolean isDual() {
    return cbcDual.isSelected();
  }

  boolean isRun() {
    return cbcRun.isEnabled() && cbcRun.isSelected();
  }

  boolean isMontage() {
    return cbcMontage.isSelected();
  }

  EndingStep getEndingStep() {
    EndingStep endingStep = EndingStep.getInstanceFromText(fcEndingStep.getText());
    if (endingStep != null) {
      return endingStep;
    }
    if (datasetState == BatchRunTomoDatasetState.DONE) {
      return EndingStep.MAX;
    }
    return null;
  }

  BatchRunTomoDatasetState getDatasetState() {
    if (fcDatasetState.isEmpty()) {
      return null;
    }
    return BatchRunTomoDatasetState.getInstance(fcDatasetState.getText());
  }

  String getStackID() {
    return stackID;
  }

  File getStackPath() {
    File file = fcStack.getFile();
    if (file != null) {
      return file.getParentFile();
    }
    else {
      return null;
    }
  }

  private void addListeners() {
    // give each listened to field an unique action command
    mbc3dmodA.setActionCommand(mbc3dmodA.getUniqueActionCommand());
    mbc3dmodB.setActionCommand(mbc3dmodB.getUniqueActionCommand());
    cbcDual.setActionCommand(cbcDual.getUniqueActionCommand());
    mbcEtomo.setActionCommand(mbcEtomo.getUniqueActionCommand());
    mbcRec.setActionCommand(mbcRec.getUniqueActionCommand());
    mbcLog.setActionCommand(mbcLog.getUniqueActionCommand());
    cbcBoundaryModel.setActionCommand(cbcBoundaryModel.getUniqueActionCommand());
    bcEditDataset.setActionCommand(bcEditDataset.getUniqueActionCommand());
    cbcRun.setActionCommand(cbcRun.getUniqueActionCommand());
    cbcMontage.setActionCommand(cbcMontage.getUniqueActionCommand());
    // set listeners
    mbc3dmodA.addActionListener(this);
    mbc3dmodB.addActionListener(this);
    cbcDual.addActionListener(this);
    mbcEtomo.addActionListener(this);
    mbcRec.addActionListener(this);
    mbcLog.addActionListener(this);
    cbcBoundaryModel.addActionListener(this);
    bcEditDataset.addActionListener(this);
    cbcRun.addActionListener(this);
    cbcMontage.addActionListener(this);
  }

  /**
   * Opens 3dmod on the A axis
   * @param modelFileType
   * @return the file created from modelFileType
   */
  File imodStack(final FileType modelFileType) {
    return imodStack(modelFileType, AxisID.FIRST, cbcDual.isSelected(), null);
  }

  /**
   * Opens 3dmod on the A axis
   * @param modelFile
   */
  void imodStack(final File modelFile) {
    imodStack(modelFile, AxisID.FIRST, cbcDual.isSelected(), null);
  }

  /**
   * Opens 3dmod.
   * @param modelFileType
   * @param axisID
   * @param dual
   * @param run3dmodMenuOptions
   * @return the file created from modelFileType
   */
  private File imodStack(final FileType modelFileType, final AxisID axisID,
    final boolean dual, final Run3dmodMenuOptions run3dmodMenuOptions) {
    File stack = new File(fcStack.getExpandedValue());
    if (modelFileType != null) {
      File modelFile = new File(stack.getParentFile(), BatchTool
        .getModelFileName(table.getManager(), modelFileType, stack.getName(), dual));
      imodStack(modelFile, axisID, dual, run3dmodMenuOptions);
      return modelFile;
    }
    // No model is required
    imodStack(axisID, dual, run3dmodMenuOptions);
    return null;
  }

  /**
   * Opens 3dmod stack without a model.
   * @param axisID
   * @param dual
   * @param run3dmodMenuOptions
   */
  private void imodStack(final AxisID axisID, final boolean dual,
    final Run3dmodMenuOptions run3dmodMenuOptions) {
    imodStack((File) null, axisID, dual, run3dmodMenuOptions);
  }

  /**
   * Opens 3dmod, with a model if modelFile is set.
   * @param modelFile
   * @param axisID
   * @param dual
   * @param run3dmodMenuOptions
   */
  private void imodStack(final File modelFile, final AxisID axisID, final boolean dual,
    final Run3dmodMenuOptions run3dmodMenuOptions) {
    File stackFile = DatasetTool.getStackFile(fcStack.getExpandedValue(), axisID, dual);
    if (axisID == AxisID.SECOND) {
      imodIndexB = table.getManager().imodStack(stackFile, axisID, imodIndexB, modelFile,
        dual, run3dmodMenuOptions);
    }
    else {
      imodIndexA = table.getManager().imodStack(stackFile, axisID, imodIndexA, modelFile,
        dual, run3dmodMenuOptions);
    }
  }

  public void actionPerformed(final ActionEvent event) {
    action(event.getActionCommand(), null, null);
  }

  public void action(final String actionCommand,
    final Deferred3dmodButton deferred3dmodButton,
    final Run3dmodMenuOptions run3dmodMenuOptions) {
    if (actionCommand == null) {
      return;
    }
    BatchRunTomoManager manager = table.getManager();
    if (actionCommand.equals(cbcDual.getActionCommand())) {
      updateDisplay();
    }
    else {
      File stack = new File(fcStack.getExpandedValue());
      boolean dual = cbcDual.isSelected();
      if (actionCommand.equals(mbc3dmodA.getActionCommand())) {
        FileType modelFile = null;
        if (cbcBoundaryModel.isSelected()) {
          modelFile = FileType.BATCH_RUN_TOMO_BOUNDARY_MODEL;
        }
        imodStack(modelFile, AxisID.FIRST, dual, run3dmodMenuOptions);
      }
      else if (actionCommand.equals(mbc3dmodB.getActionCommand())) {
        // The model is only opened for the A axis
        imodStack(AxisID.SECOND, dual, run3dmodMenuOptions);
      }
      else if (actionCommand.equals(mbcEtomo.getActionCommand())) {
        if (managerKey != null && EtomoDirector.INSTANCE.isOpen(managerKey)) {
          EtomoDirector.INSTANCE.setCurrentManager(managerKey);
        }
        else {
          managerKey =
            EtomoDirector.INSTANCE
              .openTomogram(
                DatasetTool.getDatasetFile(DatasetTool
                  .getStackFile(fcStack.getExpandedValue(), AxisID.FIRST, dual), dual),
                true, null, mbcEtomo);
        }
      }
      else if (actionCommand.equals(mbcRec.getActionCommand())) {
        if (!trimvolDone) {
          imodRec = manager.imodRec(stack.getParentFile(),
            DatasetTool.getDatasetName(stack.getName(), cbcDual.isSelected()),
            getAxisType(), imodRec, run3dmodMenuOptions);
        }
        else {
          imodRec = manager.imodTrimvol(stack.getParentFile(),
            DatasetTool.getDatasetName(stack.getName(), cbcDual.isSelected()),
            getAxisType(), imodRec, run3dmodMenuOptions);

        }
      }
      else if (actionCommand.equals(mbcLog.getActionCommand())) {
        manager.openLog(log);
      }
      else if (actionCommand.equals(cbcBoundaryModel.getActionCommand())
        && cbcBoundaryModel.isSelected()) {
        // The model is only opened for the A axis
        if (imodIndexA != -1) {
          manager.imodModel(AxisID.FIRST, imodIndexA, stack.getParentFile(),
            stack.getName(), FileType.BATCH_RUN_TOMO_BOUNDARY_MODEL, dual);
        }
      }
      else if (actionCommand.equals(bcEditDataset.getActionCommand())) {
        if (datasetDialog == null) {
          // Save the state. This call will only save this row's autodoc.
          manager.saveBatchRunTomoDialog(false, stackID, false);
          datasetDialog = BatchRunTomoDatasetDialog.getRowInstance(manager,
            DatasetTool.getDatasetFile(
              DatasetTool.getStackFile(fcStack.getExpandedValue(), AxisID.FIRST, dual),
              dual),
            this, table.getTemplateValues(), basicDirectives,
            table.getBrowsingDirectory(), stackID);
          manager.initDialog(stackID, false);
          fcEditDataset.setValue("   Set");
          datasetDialog.setMontage(cbcMontage.isSelected());
        }
        else {
          datasetDialog.setVisible(true);
          bcEditDataset.setSelected(true);
        }
      }
      else if (actionCommand.equals(cbcRun.getActionCommand())) {
        sendStatusChange(BatchRunTomoRowStatus.RUN);
      }
      else if (actionCommand.equals(cbcMontage.getActionCommand())) {
        boolean montage = cbcMontage.isSelected();
        table.msgMontage(montage);
        if (datasetDialog != null) {
          datasetDialog.setMontage(montage);
        }
      }
    }
  }

  void remove() {
    hcNumber.remove();
    hbRow.remove();
    fcStack.remove();
    cbcBoundaryModel.remove();
    cbcDual.remove();
    cbcMontage.remove();
    fcSkip.remove();
    fcBskip.remove();
    cbcSurfacesToAnalyze2.remove();
    fcEditDataset.remove();
    fcDatasetState.remove();
    fcEndingStep.remove();
    cbcRun.remove();
    mbcEtomo.remove();
    mbcRec.remove();
    mbcLog.remove();
    mbc3dmodA.remove();
    mbc3dmodB.remove();
    bcEditDataset.remove();
  }

  void delete() {
    if (metaData != null) {
      metaData.setRowNumber(null);
    }
    deleteDataset();
  }

  void deleteDataset() {
    if (datasetDialog != null) {
      datasetDialog.setVisible(false);
      datasetDialog = null;
      bcEditDataset.setSelected(false);
      fcEditDataset.setValue("");
    }
  }

  private void updateDisplay() {
    boolean dual = cbcDual.isSelected();
    fcBskip.setEnabled(dual);
    mbc3dmodB.setEnabled(dual);
    mbcRec.setEnabled(tomogramDone);
    if (log != null) {
      mbcLog.setEnabled(log.exists());
    }
  }

  /**
   * Handles global status changes.
   * @param status
   */
  public void statusChanged(final Status status) {
    if (status == null || !(status instanceof BatchRunTomoStatus)) {
      return;
    }
    this.status = (BatchRunTomoStatus) status;
    boolean open = status == BatchRunTomoStatus.OPEN;
    cbcDual.setEditable(open);
    cbcBoundaryModel.setEditable(open);
    cbcMontage.setEditable(open);
    fcSkip.setEditable(open);
    fcBskip.setEditable(open);
    cbcSurfacesToAnalyze2.setEditable(open);
    bcEditDataset.setEditable(open);
    // Run checkbox:
    // -ineditable when running
    // -editable when killed or paused and selected (so it can be removed from
    // the resume.
    cbcRun.setEditable(
      (status != BatchRunTomoStatus.RUNNING && status != BatchRunTomoStatus.KILLED_PAUSED)
        || (status == BatchRunTomoStatus.KILLED_PAUSED && cbcRun.isSelected()));
    // Run checkbox is disabled via the runKeys when killed or paused (see the
    // table
    // rowlist). Enable it the rest of the time.
    if (status != BatchRunTomoStatus.KILLED_PAUSED) {
      cbcRun.setEnabled(true);
    }
    // Etomo and 3dmod buttons should not be used while the dataset is being run
    // up, or
    // when the dataset is scheduled to be run up. But there is no need to block
    // buttons
    // for datasets that are not part of the run.
    boolean available = status != BatchRunTomoStatus.RUNNING || !cbcRun.isSelected();
    mbcEtomo.setEditable(available);
    mbcRec.setEditable(available);
    mbc3dmodA.setEditable(available);
    mbc3dmodB.setEditable(available);
    if (datasetDialog != null) {
      datasetDialog.statusChanged(this.status);
    }
  }

  void setEnabledRun(final boolean enabled) {
    cbcRun.setEnabled(enabled);
  }

  void setOrigStack(final File origStack) {
    if (origStack == null) {
      System.err.println("WARNING:  OrigStack is null.");
      Thread.dumpStack();
      return;
    }
    this.origStack = origStack.getAbsolutePath();
  }

  /**
   * Handles dataset-level status changes and changes that the row-level dataset dialog
   * responds to.
   * @param statusChangeEvent
   */
  public void statusChanged(final StatusChangeEvent event) {
    boolean dual = cbcDual.isSelected();
    // Only respond so an event directed towards this row instance.
    if (event == null || ((event instanceof StatusChangeTaggedEvent)
      && !((StatusChangeTaggedEvent) event).equals(stackID))) {
      return;
    }
    Status status = event.getStatus();
    if (status == null) {
      return;
    }
    if (status instanceof FrameStatus) {
      if (datasetDialog != null) {
        datasetDialog.statusChanged(event);
      }
    }
    else if (status instanceof BatchRunTomoDatasetState) {
      // Keep these two variables in sync.
      datasetState = (BatchRunTomoDatasetState) status;
      fcDatasetState.setValue(datasetState.getText());
      fcDatasetState.setError(false);
      fcDatasetState.setRunHighlight(false);
      if (!datasetState.isActive()) {
        // Open up the etomo/3dmod buttons as soon as batchruntomo is done with
        // the
        // dataset.
        mbcEtomo.setEditable(true);
        mbcRec.setEditable(true);
        mbc3dmodA.setEditable(true);
        mbc3dmodB.setEditable(true);
      }
      else if (datasetState == BatchRunTomoDatasetState.FAILING) {
        fcDatasetState.setError(true);
      }
      else {
        fcDatasetState.setRunHighlight(true);
        if (datasetState == BatchRunTomoDatasetState.RUNNING) {
          // Starting dataset from the beginning
          tomogramDone = false;
          trimvolDone = false;
        }
      }
      // Monitor has found information on the state of the current dataset being
      // run up
      if (status == BatchRunTomoDatasetState.DONE
        || status == BatchRunTomoDatasetState.RUNNING) {
        // Remove ending step.
        fcEndingStep.setValue();
        endingStep = null;
        if (status == BatchRunTomoDatasetState.DONE) {
          // Keep user from mistakenly rerunning a finished dataset
          cbcRun.setSelected(false);
        }
      }
    }
    else if (status instanceof BatchRunTomoDatasetStatus) {
      BatchRunTomoDatasetStatus datasetStatus = (BatchRunTomoDatasetStatus) status;
      String currentFile = fcStack.getExpandedValue();
      String newFile = ((StatusChangeTaggedEvent) event).getString();
      if (datasetStatus == BatchRunTomoDatasetStatus.RENAMED) {
        // Switching only the file name.
        newFile =
          new File(new File(currentFile).getParentFile(), newFile).getAbsolutePath();
      }
      else if (datasetStatus == BatchRunTomoDatasetStatus.DELIVERED) {
        // Dual is in use, but not modifiable.
        cbcDual.setEditable(false);
        // New location
        setLog(new File(newFile));
      }
      table.getTableReference().changeFilePath(stackID, newFile);
      fcStack.setValue(newFile);
    }
    else if (status instanceof EndingStep) {
      endingStep = (EndingStep) status;
      // A step isn't completed until both axes have finished it. Ignore the first axis
      // in a dual axis tomogram.
      if (dual && !firstSteps[endingStep.getIndex()]) {
        // Need to see the step for both axes before it can appear in the field
        firstSteps[endingStep.getIndex()] = true;
        return;
      }
      fcEndingStep.setValue(endingStep.getText());
      return;
    }
    else if (status instanceof Step) {
      if (status == Step.SETUP) {
        // The .edf file has been created.
        mbcEtomo.setEnabled(true);
        mbcRec.setEnabled(true);
        mbcLog.setEnabled(true);
      }
      else if (status == Step.RECONSTRUCTION && !dual) {
        tomogramDone = true;
      }
    }
    else if (status instanceof ProcessName) {
      if (status == ProcessName.VOLCOMBINE) {
        // Volcombine is done
        if (dual) {
          tomogramDone = true;
        }
      }
      else if (status == ProcessName.TRIMVOL) {
        // Trimvol is done
        trimvolDone = true;
      }
    }
    updateDisplay();
  }

  private void setLog(final File stack) {
    if (stack != null) {
      // The dataset log does not contain the dataset name.
      log = FileType.BATCH_RUN_TOMO_DATASET_LOG.getFile(table.getManager(),
        stack.getParentFile(), null, getAxisType(), null);
    }
  }

  private AxisType getAxisType() {
    if (cbcDual.isSelected()) {
      return AxisType.DUAL_AXIS;
    }
    return AxisType.SINGLE_AXIS;
  }

  public void addStatusChangeListener(final StatusChangeListener listener) {
    if (listener == null) {
      return;
    }
    if (listeners == null) {
      listeners = new ArrayList<StatusChangeListener>();
    }
    listeners.add(listener);
  }

  private void sendStatusChange(final StatusChangeEvent statusChangeEvent) {
    if (listeners != null) {
      int len = listeners.size();
      for (int i = 0; i < len; i++) {
        listeners.get(i).statusChanged(statusChangeEvent);
      }
    }
  }

  private void sendStatusChange(final Status status) {
    if (listeners != null) {
      int len = listeners.size();
      for (int i = 0; i < len; i++) {
        listeners.get(i).statusChanged(status);
      }
    }
  }

  void display(final Viewport viewport, final BatchRunTomoTab tab) {
    // See if index is in the viewport
    if (viewport.inViewport(hcNumber.getInt() - 1)) {
      JPanel panel = table.getTablePanel();
      GridBagLayout layout = table.getGridBagLayout();
      GridBagConstraints constraints = table.getGridBagConstraints();
      constraints.gridwidth = 1;
      hcNumber.add(panel, layout, constraints);
      if (tab == BatchRunTomoTab.STACKS) {
        hbRow.add(panel, layout, constraints);
      }
      constraints.gridwidth = 2;
      fcStack.add(panel, layout, constraints);
      constraints.gridwidth = 1;
      if (tab == BatchRunTomoTab.STACKS) {
        cbcDual.add(panel, layout, constraints);
        cbcMontage.add(panel, layout, constraints);
        fcSkip.add(panel, layout, constraints);
        fcBskip.add(panel, layout, constraints);
        cbcBoundaryModel.add(panel, layout, constraints);
        cbcSurfacesToAnalyze2.add(panel, layout, constraints);
        mbc3dmodA.add(panel, layout, constraints);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        mbc3dmodB.add(panel, layout, constraints);
      }
      else if (tab == BatchRunTomoTab.DATASET) {
        bcEditDataset.add(panel, layout, constraints);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        fcEditDataset.add(panel, layout, constraints);
      }
      else {
        constraints.ipadx = 1;
        fcDatasetState.add(panel, layout, constraints);
        constraints.ipadx = 0;
        fcEndingStep.add(panel, layout, constraints);
        cbcRun.add(panel, layout, constraints);
        mbcEtomo.add(panel, layout, constraints);
        mbcRec.add(panel, layout, constraints);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        mbcLog.add(panel, layout, constraints);
      }
    }
  }

  void expandStack(final boolean expanded) {
    fcStack.expand(expanded);
  }

  public void highlight(final boolean highlight) {
    fcStack.setHighlight(highlight);
    cbcBoundaryModel.setHighlight(highlight);
    cbcDual.setHighlight(highlight);
    cbcMontage.setHighlight(highlight);
    fcSkip.setHighlight(highlight);
    fcBskip.setHighlight(highlight);
    cbcSurfacesToAnalyze2.setHighlight(highlight);
    fcEditDataset.setHighlight(highlight);
    fcDatasetState.setHighlight(highlight);
    fcEndingStep.setHighlight(highlight);
    cbcRun.setHighlight(highlight);
  }

  boolean equalsStackID(final String stackID) {
    return this.stackID.equals(stackID);
  }

  /**
   * @param metaData
   * @param onlyDatasetDialog only loading a newly created dataset dialog
   */
  public void setParameters(final BatchRunTomoMetaData metaData,
    final boolean onlyDatasetDialog) {
    BatchRunTomoRowMetaData rowMetaData = metaData.getRowMetaData(stackID);
    boolean isDatasetDialog = rowMetaData.isDatasetDialog();
    if (!onlyDatasetDialog) {
      this.metaData = rowMetaData;
      cbcDual.setEnabled(rowMetaData.isDualEnabled());
      cbcDual.setSelected(rowMetaData.getDual(), false);
      fcBskip.setValue(rowMetaData.getBskip(), false);
      cbcRun.setSelected(rowMetaData.getRun(), false);
      bcEditDataset.setSelected(isDatasetDialog);
      if (isDatasetDialog) {
        fcEditDataset.setValue(EDIT_DATASET_VALUE);
      }
      else {
        fcEditDataset.setValue();
      }
      origStack = rowMetaData.getOrigStack();
      mbcEtomo.setEnabled(rowMetaData.isEtomoEnabled());
      mbcRec.setEnabled(rowMetaData.isRecEnabled());
      mbcLog.setEnabled(rowMetaData.isLogEnabled());
      tomogramDone = rowMetaData.isTomogramDone();
      trimvolDone = rowMetaData.isTrimvolDone();
      if (isDatasetDialog) {
        boolean dual = cbcDual.isSelected();
        datasetDialog = BatchRunTomoDatasetDialog.getSavedRowInstance(table.getManager(),
          DatasetTool.getDatasetFile(
            DatasetTool.getStackFile(fcStack.getExpandedValue(), AxisID.FIRST, dual),
            dual),
          this, table.getTemplateValues(), basicDirectives, table.getBrowsingDirectory(),
          stackID);
      }
    }
    if (!isDatasetDialog && onlyDatasetDialog) {
      // For a new row-level dataset dialog initialize with data from the main dataset
      // dialog.
      datasetDialog.setParameters(metaData.getDatasetMetaData());
    }
    if (isDatasetDialog || onlyDatasetDialog) {
      // TODO 2342
      datasetDialog.setParameters(rowMetaData.getDatasetMetaData());
    }
    if (!onlyDatasetDialog) {
      // Set off internal status changed events.
      statusChanged(metaData.getStatus());
      BatchRunTomoDatasetState datasetState = rowMetaData.getDatasetState();
      StatusChangeTaggedEvent event = null;
      if (datasetState != null) {
        event = new StatusChangeTaggedEvent(stackID, datasetState);
        statusChanged(event);
      }
      EndingStep endingStep = rowMetaData.getEndingStep();
      if (endingStep != null) {
        fcEndingStep.setValue(endingStep.getText());
      }
      updateDisplay();
    }
  }

  void getParameters(final BatchRunTomoMetaData metaData) {
    BatchRunTomoRowMetaData rowMetaData = metaData.getRowMetaData(stackID);
    this.metaData = rowMetaData;
    rowMetaData.setRowNumber(hcNumber.getText());
    rowMetaData.setDualEnabled(cbcDual.isEnabled());
    rowMetaData.setDual(cbcDual.isSelected());
    rowMetaData.setBskip(fcBskip.getValue());
    rowMetaData.setRun(cbcRun.isSelected());
    rowMetaData.setDatasetDialog(datasetDialog != null);
    rowMetaData.setOrigStack(origStack);
    rowMetaData.setEtomoEnabled(mbcEtomo.isEnabled());
    rowMetaData.setRecEnabled(mbcRec.isEnabled());
    rowMetaData.setLogEnabled(mbcLog.isEnabled());
    rowMetaData.setTomogramDone(tomogramDone);
    rowMetaData.setTrimvolDone(trimvolDone);
    if (datasetDialog != null) {
      datasetDialog.getParameters(rowMetaData.getDatasetMetaData());
    }
    rowMetaData.setDatasetState(datasetState);
    rowMetaData.setEndingStep(endingStep);
  }

  /**
   * Update param and return true if the row can be run.
   * @param param
   * @param deliverToDirectory
   * @param errMsg
   * @return true if run is checked and enabled
   */
  boolean getParameters(final BatchruntomoParam param, final boolean deliverOff,
    final boolean deliverToDirectory, final StringBuilder errMsg,
    final boolean doValidation, final List<UniqueKey> managerKeyList) {
    if (!cbcRun.isSelected() || !cbcRun.isEnabled()) {
      return false;
    }
    StringBuilder deliveryErrMsg = new StringBuilder();
    // Collect all dataset file error messages.
    File stack = new File(fcStack.getExpandedValue());
    param.addDirectiveFile(getAutodocFile(fcStack.getContractedValue()));
    String rootName = DatasetTool.getDatasetName(stack.getName(), cbcDual.isSelected());
    if (param.addRootName(rootName, deliverToDirectory, cbcDual.isSelected(),
      doValidation, deliveryErrMsg)) {

    }
    String origStackLocation = null;
    String tempOrigStack = metaData.getOrigStack();
    if (tempOrigStack != null) {
      origStackLocation = new File(tempOrigStack).getParent();
    }
    if (!param.addCurrentLocation(origStackLocation, stack.getParent(), deliverOff,
      doValidation, deliveryErrMsg)) {
      deliveryErrMsg.append(": " + stack.getAbsolutePath() + ".  ");
    }
    // If doValidation is true, will be run
    if (doValidation) {
      for (int i = 0; i < firstSteps.length; i++) {
        firstSteps[i] = false;
        if (managerKeyList != null) {
          if (EtomoDirector.INSTANCE.isOpen(managerKey)) {
            managerKeyList.add(managerKey);
          }
        }
      }
    }
    if (deliveryErrMsg.length() > 0) {
      errMsg.append(deliveryErrMsg.toString());
      errMsg.append("\n\nChange the option for moving the stack in the "
        + BatchRunTomoTab.BATCH.getQuotedLabel() + " tag.");
    }
    return true;
  }

  private File getAutodocFile(final String fileName) {
    String parent;
    if (origStack != null && !origStack.isEmpty()) {
      parent = new File(origStack).getParent();
    }
    else {
      parent = new File(fcStack.getExpandedValue()).getParent();
    }
    return new File(parent, table.getManager().getName() + "_"
      + DatasetTool.getDatasetName(fileName, cbcDual.isSelected()) + ".adoc");
  }

  /**
   * @param onlyLoadDataset only load into datasetDialog
   * @param onlyAdvancedDatasetDialog only loading an advanced dataset dialog
   */
  void loadAutodoc(final boolean onlyLoadDataset,
    final boolean onlyAdvancedDatasetDialog) {
    DirectiveFile directiveFile = DirectiveFile.getInstance(table.getManager(), null,
      getAutodocFile(fcStack.getContractedValue()), DirectiveFileType.BATCH);
    if (!onlyLoadDataset) {
      setValues(directiveFile);
      BatchTool.setTextValue(fcSkip, directiveFile, false, null);
      BatchTool.setTextValue(fcBskip, directiveFile, false, null, AxisID.SECOND);
      if (directiveFile.containsValue(DirectiveDef.RAW_BOUNDARY_MODEL_FOR_SEED_FINDING)
        || directiveFile
          .containsValue(DirectiveDef.RAW_BOUNDARY_MODEL_FOR_PATCH_TRACKING)) {
        BatchTool.setBooleanValue(cbcBoundaryModel, true, false);
      }
    }
    if (datasetDialog != null) {
      datasetDialog.setValues(directiveFile, false, onlyAdvancedDatasetDialog, true);
    }
  }

  boolean saveAutodoc(final TemplatePanel templatePanel,
    final NameValuePairList templates, final boolean doValidation,
    final File deliverToDirectory, final FieldDisplayer fieldDisplayer) {
    BatchRunTomoManager manager = table.getManager();
    File stack = new File(fcStack.getExpandedValue());
    File batchFile = getAutodocFile(fcStack.getContractedValue());
    // If the advanced dialog was never created, then load the save file first as not all
    // of it was loaded into the dialog.
    boolean advancedDialogExists =
      datasetDialog != null && datasetDialog.isAdvancedDialogExists();
    NameValuePairList loadedBatchList = null;
    try {
      if (batchFile.exists()) {
        // If the dataset level dialog is not in use, the local batch file is not loaded.
        if (!advancedDialogExists) {
          loadedBatchList =
            new NameValuePairList(AutodocFactory.getAutodocInstance(manager, batchFile));
        }
        if (batchFile.exists()) {
          Utilities.deleteFile(batchFile, manager, null);
        }
      }
      Autodoc batchAutodoc =
        AutodocFactory.getWritableAutodocInstance(manager, batchFile);
      String stackName = stack.getName();
      Extension extension = Extension.getInstance(stackName);
      if (extension != null) {
        // Save setupset.currentStackExt or currentBStackExt.
        BatchTool.saveTextToAutodoc(true, fcStack.getDirectiveDef(), extension.toString(),
          null, batchAutodoc, null);
        if (extension.isInputImageFile()) {
          BatchTool.saveTextToAutodoc(true, DirectiveDef.STACK_EXT, extension.toString(),
            null, batchAutodoc, null);
        }
      }
      else if (doValidation) {
        UIHarness.INSTANCE.openMessageDialog(
          manager, cbcBoundaryModel, "Row# " + hcNumber.getText()
            + ":  Missing or invalid file extension - " + stackName,
          "Invalid Extension", fieldDisplayer);
        return false;
      }
      BatchTool.saveBooleanToAutodoc(cbcDual, batchAutodoc, null);
      BatchTool.saveBooleanToAutodoc(cbcMontage, batchAutodoc, null);
      BatchTool.saveTextToAutodoc(fcSkip, batchAutodoc, doValidation, null, null);
      BatchTool.saveTextToAutodoc(fcBskip, batchAutodoc, doValidation, null, null);
      String boundaryModelName = "";
      if (cbcBoundaryModel.isSelected()) {
        boundaryModelName = BatchTool.getModelFileName(table.getManager(),
          FileType.BATCH_RUN_TOMO_BOUNDARY_MODEL, stackName, cbcDual.isSelected());
        // Validation: make sure the boundary model file exists
        if (doValidation && !new File(stack.getParentFile(), boundaryModelName).exists()
          && (deliverToDirectory == null || !(new File(
            new File(deliverToDirectory,
              DatasetTool.getDatasetName(stackName, cbcDual.isSelected())),
            boundaryModelName)).exists())) {
          UIHarness.INSTANCE.openMessageDialog(
            manager, cbcBoundaryModel, "Row# " + hcNumber.getText()
              + ":  Missing boundary model file - " + boundaryModelName,
            "Missing File", fieldDisplayer);
          return false;
        }
      }
      // Save the correct boundary model directive.
      boolean trackingMethodSeed = datasetDialog == null ? table.isTrackingMethodSeed()
        : datasetDialog.isTrackingMethodSeed();
      if (trackingMethodSeed) {
        BatchTool.saveBooleanTextToAutodoc(cbcBoundaryModel, boundaryModelName,
          batchAutodoc);
      }
      else {
        BatchTool.saveTextToAutodoc(true,
          DirectiveDef.RAW_BOUNDARY_MODEL_FOR_PATCH_TRACKING, boundaryModelName, null,
          batchAutodoc, null);
      }
      //
      BatchTool.saveBooleanTextToAutodoc(cbcSurfacesToAnalyze2, batchAutodoc,
        SURFACES_TO_ANALYZE_TWO, SURFACES_TO_ANALYZE_ONE);
      if (templatePanel != null) {
        templatePanel.saveAutodoc(batchAutodoc);
      }
      if (datasetDialog != null) {
        if (!datasetDialog.saveAutodoc(batchAutodoc, doValidation)) {
          return false;
        }
      }
      else {
        BatchRunTomoDatasetDialog globalDatasetDialog = table.getDatasetDialog();
        if (globalDatasetDialog != null) {
          // The global is validated when the main .adoc file is saved
          if (!globalDatasetDialog.saveAutodoc(batchAutodoc, false)) {
            return false;
          }
        }
      }
      // Create and write a dataset-level batch file.
      NameValuePairList saveBatchList;
      if (advancedDialogExists) {
        saveBatchList = BatchTool.createBatchFile(manager, batchAutodoc,
          advancedDialogExists, null, null, null, templates);
      }
      else {
        // No advanced dialog - take the advanced directives from the files
        saveBatchList = BatchTool.createBatchFile(manager, batchAutodoc,
          advancedDialogExists, loadedBatchList, basicDirectives,
          table.getAdvancedStartingBatch(), templates);
      }
      saveBatchList.write(batchAutodoc.getLogFile());
    }
    catch (

    IOException e) {
      e.printStackTrace();
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    catch (FieldValidationFailedException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  boolean isHighlighted() {
    return hbRow.isHighlighted();
  }

  void selectHighlightButton() {
    hbRow.setSelected(true);
  }

  /**
   * Check isDifferentFromCheckpoint on all data entry fields that are loaded from
   * directive files.
   * 
   * @param onlyDatasetDialog only loading the dataset dialog
   * @param onlyAdvancedDatasetDialog only loading the advanced dataset dialog - no effect if onlyDatasetDialog is false
   *
   * @return true if any field's isDifferentFromCheckpoint function returned true
   */
  boolean backupIfChanged(final boolean onlyDatasetDialog,
    final boolean onlyAdvancedDatasetDialog) {
    boolean changed = false;
    if (!onlyDatasetDialog) {
      if (cbcDual.isDifferentFromCheckpoint(true)) {
        cbcDual.backup();
        changed = true;

      }
      if (cbcMontage.isDifferentFromCheckpoint(true)) {
        cbcMontage.backup();
        changed = true;
      }
      if (cbcSurfacesToAnalyze2.isDifferentFromCheckpoint(true)) {
        cbcSurfacesToAnalyze2.backup();
        changed = true;
      }
    }
    if (datasetDialog != null
      && datasetDialog.backupIfChanged(onlyAdvancedDatasetDialog)) {
      changed = true;
    }
    return changed;
  }

  /**
   * @param init
   * @param retainUserValues
   * @param userConfiguration
   * @param directiveFileCollection
   * @param onlyDatasetDialog only loading the dataset dialog
   * @param onlyAdvancedDatasetDialog only loading the advanced dataset dialog - no effect if onlyDatasetDialog is false
   */
  void applyValues(final boolean init, final boolean retainUserValues,
    final UserConfiguration userConfiguration,
    final DirectiveFileCollection directiveFileCollection,
    final boolean onlyDatasetDialog, final boolean onlyAdvancedDatasetDialog) {
    if (!onlyDatasetDialog) {
      // to apply values and highlights, start with a clean slate
      if (!init) {
        cbcDual.clear();
        cbcMontage.clear();
        cbcSurfacesToAnalyze2.clear();
      }
      // no default values to apply to table
      // Apply settings values
      setValues(userConfiguration);
      // Apply the directive collection values
      setValues(directiveFileCollection);
      // checkpoint template/starting batch
      cbcDual.checkpoint();
      cbcMontage.checkpoint();
      cbcSurfacesToAnalyze2.checkpoint();
      // If the user wants to retain their values, apply backed up values and then
      // delete
      // them.
      if (retainUserValues) {
        cbcDual.restoreFromBackup();
        cbcMontage.restoreFromBackup();
        cbcSurfacesToAnalyze2.restoreFromBackup();
        updateDisplay();
      }
      // no field highlight values to set in table
    }
    // Dataset dialog
    if (datasetDialog != null) {
      datasetDialog.applyValues(init, retainUserValues, directiveFileCollection,
        onlyAdvancedDatasetDialog);
    }
    if (!onlyDatasetDialog) {
      updateDisplay();
    }
  }

  void setNumber(final int input) {
    hcNumber.setText(input);
  }

  /**
   * Set values from the directive file collection - only for directives that are present.
   *
   * @param directiveFiles - templates and the base directive file,
   *                       or a single directive file
   */
  void setValues(final DirectiveFileInterface directiveFiles) {
    BatchTool.setBooleanValue(cbcDual, directiveFiles, false, null);
    BatchTool.setBooleanValue(cbcMontage, directiveFiles, false, null);
    BatchTool.setBooleanValueFromSelectedText(cbcSurfacesToAnalyze2,
      SURFACES_TO_ANALYZE_TWO, directiveFiles, false, null);
  }

  void setValues(final UserConfiguration userConfiguration) {
    // Only use this if the settings checkbox is checked. Otherwise ignore it.
    if (userConfiguration.getSingleAxis()) {
      cbcDual.setSelected(false);
    }
    cbcMontage.setSelected(userConfiguration.getMontage());
    updateDisplay();
  }

  private void setTooltips(final BatchRunTomoRow prevRow) {
    cbcBoundaryModel.setToolTipText(SharedStrings.RAW_BOUNDARY_MODEL);
    cbcDual.setToolTipText(SharedStrings.DUAL_TOOLTIP);
    cbcMontage.setToolTipText(SharedStrings.MONTAGE_TOOLTIP);
    fcSkip.setToolTipText(SharedStrings.SKIP_TOOLTIP);
    fcBskip.setToolTipText(SharedStrings.BSKIP_TOOLTIP);
    cbcSurfacesToAnalyze2.setToolTipText(SharedStrings.SURFACES_TO_ANALYZE_2_TOOLTIP);
    fcEditDataset.setToolTipText(SharedStrings.EDIT_DATASET_TOOLTIP);
    fcDatasetState.setToolTipText("Completion status of the dataset");
    cbcRun.setToolTipText("This dataset will be included in the batchruntomo run");
    mbcEtomo.setToolTipText("Opens a tab in Etomo contain this dataset");
    mbcRec.setToolTipText("Opens the current .rec file for this dataset");
    mbcLog.setToolTipText("Opens the current log file for this dataset");
    mbc3dmodA.setToolTipText(SharedStrings.IMOD_A_TOOLTIP);
    mbc3dmodB.setToolTipText(SharedStrings.IMOD_B_TOOLTIP);
    bcEditDataset.setToolTipText("Open dialog to set dataset-specific values.");
    fcStack.setToolTipText(SharedStrings.STACK_TOOLTIP);
    fcEndingStep.setToolTipText(SharedStrings.STEP_TOOLTIP);
  }
}
