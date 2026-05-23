package etomo.ui.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import etomo.BatchRunTomoManager;
import etomo.EtomoDirector;
import etomo.ProcessingMethodMediator;
import etomo.comscript.BatchruntomoParam;
import etomo.logic.BatchTool;
import etomo.logic.UserEnv;
import etomo.storage.AutodocFilter;
import etomo.storage.DirectiveDef;
import etomo.storage.DirectiveFile;
import etomo.storage.DirectiveFileCollection;
import etomo.storage.LogFile;
import etomo.storage.NameValuePairList;
import etomo.storage.autodoc.Autodoc;
import etomo.storage.autodoc.AutodocFactory;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.type.AxisID;
import etomo.type.BatchRunTomoMetaData;
import etomo.type.BatchRunTomoStatus;
import etomo.type.DialogType;
import etomo.type.DirectiveFileType;
import etomo.type.EtomoAutodoc;
import etomo.type.FileType;
import etomo.type.ImageFilenameStyle;
import etomo.type.ProcessingMethod;
import etomo.type.Status;
import etomo.type.StatusChangeEvent;
import etomo.type.StatusChangeListener;
import etomo.type.StatusChanger;
import etomo.type.TableReference;
import etomo.type.UserConfiguration;
import etomo.ui.BatchRunTomoTab;
import etomo.ui.BrowsingDirectory;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.util.Utilities;
import etomo.util.ValidDirectory;

/**
 * <p>Description: Interface for batchruntomo.</p>
 * 
 * <p>Copyright: Copyright 2014 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class BatchRunTomoDialog
  implements ActionListener, ResultListener, ChangeListener, Expandable, ProcessInterface,
  StatusChanger, StatusChangeListener, ContextMenu, BrowsingDirectory, FieldDisplayer {
  private static final String DELIVER_TO_DIRECTORY_LABEL =
    "Move all stacks to dataset directories under: ";

  private final JPanel pnlRoot = new JPanel();
  private final LabeledTextField ltfRootName =
    new LabeledTextField(FieldType.STRING, "Batchruntomo root name: ");
  private final ButtonGroup bgDeliver = new ButtonGroup();
  private final RadioButton rbDeliverOff =
    new RadioButton("Stacks are already in dataset directories", bgDeliver);
  private final RadioButton rbDeliverToDirectory =
    new RadioButton(DELIVER_TO_DIRECTORY_LABEL, bgDeliver);
  private final RadioButton rbDeliverMakeSubDirectory = new RadioButton(
    "Move stacks to dataset directories under their current locations", bgDeliver);
  private final CheckTextField ctfEmailAddress =
    CheckTextField.getInstance(FieldType.STRING, "Email notification: ");
  private final CheckBox cbCPUMachineList = new CheckBox("Use multiple CPUs");
  private final ButtonGroup bgGPUMachineList = new ButtonGroup();
  private final RadioButton rbGPUMachineListOff =
    new RadioButton("No GPU", bgGPUMachineList);
  private final RadioButton rbGPUMachineListLocal =
    new RadioButton("Local GPU", bgGPUMachineList);
  private final RadioButton rbGPUMachineList =
    new RadioButton("Parallel GPUs", bgGPUMachineList);
  private final TabbedPane tabbedPane = new TabbedPane();
  private final JPanel[] pnlTabs = new JPanel[BatchRunTomoTab.SIZE];
  private final JPanel pnlBatch = new JPanel();
  private final JPanel pnlStacks = new JPanel();
  private final JPanel pnlDataset = new JPanel();
  private final JPanel pnlRun = new JPanel();
  private final JPanel pnlTable = new JPanel();
  private final SingleLineButton btnRun = new SingleLineButton("Run");
  private final JPanel pnlParallelSettings = new JPanel();
  private final UserConfiguration userConfiguration =
    EtomoDirector.INSTANCE.getUserConfiguration();
  private final JPanel pnlDatasetTableBody = new JPanel();
  private final JPanel pnlUntitledTable = new JPanel();
  private final SingleLineButton btnReset = new SingleLineButton("Reset");
  private final SingleLineButton btnPause = new SingleLineButton("Pause");
  private final SingleLineButton btnResume = new SingleLineButton("Resume");
  private final SingleLineButton btnClearInputDirectiveFile =
    SingleLineButton.getHtmlInstance("Clear");

  private final FileTextField2 ftfRootDir;
  private final FileTextField2 ftfInputDirectiveFile;
  private final TemplatePanel templatePanel;
  private final FileTextField2 ftfDeliverToDirectory;
  private final BatchRunTomoTable table;
  private final BatchRunTomoManager manager;
  private final AxisID axisID;
  private final BatchRunTomoDatasetDialog datasetDialog;
  private final DirectiveFileCollection directiveFileCollection;
  private final PanelHeader phDatasetTable;
  private final ProcessingMethodMediator mediator;
  private final BatchRunTomoStepPanel stepPanel;
  private final JPanel parallelStatusPanel;
  private final Map<DirectiveDef, String> templateValues =
    new HashMap<DirectiveDef, String>();
  private final Set<DirectiveDef> basicDirectives = new HashSet<DirectiveDef>();
  private JPanel pnlDatasetTable = new JPanel();
  private RunFieldDisplayer runFieldDisplayer = new RunFieldDisplayer(this);

  private BatchRunTomoTab curTab = null;
  private ArrayList<StatusChangeListener> listeners = null;
  private BatchRunTomoStatus status = BatchRunTomoStatus.DEFAULT;
  private ValidDirectory validbrowsingDirectory = null;
  private NameValuePairList advancedStartingBatch = null;

  private BatchRunTomoDialog(final BatchRunTomoManager manager, final AxisID axisID,
    final TableReference tableReference, final JPanel parallelStatusPanel) {
    this.manager = manager;
    this.axisID = axisID;
    this.parallelStatusPanel = parallelStatusPanel;
    ftfRootDir = FileTextField2.getAltLayoutInstance(manager, "Location: ");
    ftfInputDirectiveFile =
      FileTextField2.getAltLayoutInstance(manager, "Starting directive file: ");
    ftfInputDirectiveFile.checkpoint();
    ftfDeliverToDirectory =
      FileTextField2.getUnlabeledInstance(manager, DELIVER_TO_DIRECTORY_LABEL + ": ");
    table = BatchRunTomoTable.getInstance(manager, this, basicDirectives, tableReference,
      pnlRoot);
    datasetDialog = BatchRunTomoDatasetDialog.getGlobalInstance(manager, this,
      templateValues, basicDirectives, this);
    directiveFileCollection = DirectiveFileCollection.getBatchInstance(manager, axisID);
    templatePanel = TemplatePanel.getBorderlessInstance(manager, axisID, null, null, null,
      directiveFileCollection, true, true);
    phDatasetTable = PanelHeader.getInstance("Datasets", this, DialogType.BATCH_RUN_TOMO);
    mediator = manager.getProcessingMethodMediator(axisID);
    stepPanel = BatchRunTomoStepPanel.getInstance(manager, axisID, table);
    mediator.register(this);
  }

  public static BatchRunTomoDialog getInstance(final BatchRunTomoManager manager,
    final AxisID axisID, final TableReference tableReference,
    final JPanel parallelStatusPanel) {
    BatchRunTomoDialog instance =
      new BatchRunTomoDialog(manager, axisID, tableReference, parallelStatusPanel);
    instance.createPanel();
    instance.setTooltips();
    return instance;
  }

  boolean hasDual() {
    return table.hasDual();
  }

  private void createPanel() {
    // local panels
    JPanel pnlRootName = new JPanel();
    JPanel pnlDeliver = new JPanel();
    JPanel pnlTemplates = new JPanel();
    JPanel pnlEmail = new JPanel();
    JPanel pnlSettings = new JPanel();
    JPanel pnlRunButtons = new JPanel();
    JPanel pnlDeliverToDirectory = new JPanel();
    JPanel pnlInputDirectiveFile = new JPanel();
    // init
    basicDirectives.add(DirectiveDef.NAME);
    basicDirectives.add(DirectiveDef.SCOPE_TEMPLATE);
    basicDirectives.add(DirectiveDef.SYSTEM_TEMPLATE);
    basicDirectives.add(DirectiveDef.USER_TEMPLATE);
    basicDirectives.add(DirectiveDef.DATASET_DIRECTORY);
    templatePanel.setFieldHighlight();
    ftfInputDirectiveFile.setAbsolutePath(true);
    ftfInputDirectiveFile.setTextEntryPolicy(false);
    ftfInputDirectiveFile.setFileFilter(new AutodocFilter());
    ftfDeliverToDirectory.setAbsolutePath(true);
    ftfDeliverToDirectory.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    ftfDeliverToDirectory.setRequired(true);
    btnReset.setToPreferredSize();
    btnPause.setToPreferredSize();
    btnRun.setToPreferredSize(btnPause.getPreferredSize());
    btnResume.setToPreferredSize();
    tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    ftfRootDir.setAbsolutePath(true);
    ftfRootDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    ftfRootDir.setText(new File(System.getProperty("user.dir")).getAbsolutePath());
    ftfInputDirectiveFile.setOrigin(ftfRootDir.getFile());
    ftfInputDirectiveFile.setOriginReference(ftfRootDir);
    // Make sure that the machine lists from the batchruntomo .com file get loaded.
    cbCPUMachineList.setSelected(true);
    rbGPUMachineList.setSelected(true);
    rbDeliverOff.setSelected(true);
    btnClearInputDirectiveFile.setToPreferredSize();
    // root panel
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
    pnlRoot.setBorder(new BeveledBorder("Batchruntomo Interface").getBorder());
    pnlRoot.add(tabbedPane);
    // tabbedPane
    for (int i = 0; i < BatchRunTomoTab.SIZE; i++) {
      pnlTabs[i] = new JPanel();
      BatchRunTomoTab tab = BatchRunTomoTab.getInstance(i);
      tabbedPane.addTab(tab.getTitle(), pnlTabs[i]);
    }
    // Batch
    pnlBatch.setLayout(new BoxLayout(pnlBatch, BoxLayout.Y_AXIS));
    pnlBatch.setBorder(new EtchedBorder("Batch Setup Parameters").getBorder());
    pnlBatch.add(pnlDeliver);
    pnlBatch.add(Box.createRigidArea(FixedDim.x0_y20));
    pnlBatch.add(pnlInputDirectiveFile);
    pnlBatch.add(Box.createRigidArea(FixedDim.x0_y6));
    pnlBatch.add(pnlTemplates);
    pnlBatch.add(Box.createRigidArea(FixedDim.x0_y15));
    pnlBatch.add(pnlRootName);
    // Stacks
    pnlStacks.setLayout(new BoxLayout(pnlStacks, BoxLayout.Y_AXIS));
    pnlStacks.setBorder(BorderFactory.createEtchedBorder());
    // panel created on tab change
    // Dataset
    pnlDataset.setLayout(new BoxLayout(pnlDataset, BoxLayout.Y_AXIS));
    pnlDataset.setBorder(BorderFactory.createEtchedBorder());
    pnlDataset.add(datasetDialog.getComponent());
    pnlDataset.add(pnlDatasetTable);
    // Run
    pnlRun.setLayout(new BoxLayout(pnlRun, BoxLayout.Y_AXIS));
    pnlRun.setBorder(BorderFactory.createEtchedBorder());
    pnlRun.add(parallelStatusPanel);
    pnlRun.add(Box.createRigidArea(FixedDim.x0_y2));
    pnlRun.add(pnlEmail);
    pnlRun.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlRun.add(pnlSettings);
    pnlRun.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlRun.add(pnlRunButtons);
    pnlRun.add(Box.createRigidArea(FixedDim.x0_y5));
    // DatasetTable
    pnlDatasetTable.setLayout(new BoxLayout(pnlDatasetTable, BoxLayout.Y_AXIS));
    pnlDatasetTable.setBorder(BorderFactory.createEtchedBorder());
    pnlDatasetTable.add(phDatasetTable.getContainer());
    pnlDatasetTable.add(Box.createRigidArea(FixedDim.x0_y2));
    pnlDatasetTable.add(pnlDatasetTableBody);
    // DatasetTableBody
    pnlDatasetTableBody.setLayout(new BoxLayout(pnlDatasetTableBody, BoxLayout.X_AXIS));
    // panel created on tab change
    // Table
    pnlTable.setLayout(new BoxLayout(pnlTable, BoxLayout.Y_AXIS));
    pnlTable.setBorder(new EtchedBorder("Datasets").getBorder());
    // UntitledTable
    pnlUntitledTable.setLayout(new BoxLayout(pnlUntitledTable, BoxLayout.Y_AXIS));
    // Email
    pnlEmail.setLayout(new BoxLayout(pnlEmail, BoxLayout.X_AXIS));
    pnlEmail.add(ctfEmailAddress.getComponent());
    // pnlSettings
    pnlSettings.setLayout(new BoxLayout(pnlSettings, BoxLayout.X_AXIS));
    pnlSettings.add(pnlParallelSettings);
    pnlSettings.add(Box.createHorizontalGlue());
    pnlSettings.add(stepPanel.getComponent());
    pnlSettings.add(Box.createHorizontalGlue());
    // RunButton
    pnlRunButtons.setLayout(new BoxLayout(pnlRunButtons, BoxLayout.X_AXIS));
    pnlRunButtons.add(Box.createHorizontalGlue());
    pnlRunButtons.add(btnRun.getComponent());
    pnlRunButtons.add(Box.createHorizontalGlue());
    pnlRunButtons.add(btnPause.getComponent());
    pnlRunButtons.add(Box.createHorizontalGlue());
    pnlRunButtons.add(btnResume.getComponent());
    pnlRunButtons.add(Box.createHorizontalGlue());
    pnlRunButtons.add(btnReset.getComponent());
    pnlRunButtons.add(Box.createHorizontalGlue());
    // ParallelSettings
    pnlParallelSettings.setLayout(new BoxLayout(pnlParallelSettings, BoxLayout.Y_AXIS));
    pnlParallelSettings.setBorder(new EtchedBorder("Run Actions").getBorder());
    pnlParallelSettings.add(cbCPUMachineList.getComponent());
    pnlParallelSettings.add(rbGPUMachineListOff.getComponent());
    pnlParallelSettings.add(rbGPUMachineListLocal.getComponent());
    pnlParallelSettings.add(rbGPUMachineList.getComponent());
    // RootName
    pnlRootName.setLayout(new BoxLayout(pnlRootName, BoxLayout.Y_AXIS));
    pnlRootName.setBorder(new EtchedBorder("Batchruntomo Project Files").getBorder());
    pnlRootName.add(ltfRootName.getComponent());
    pnlRootName.add(Box.createRigidArea(FixedDim.x0_y2));
    pnlRootName.add(ftfRootDir.getRootPanel());
    // Templates
    pnlTemplates.setLayout(new BoxLayout(pnlTemplates, BoxLayout.X_AXIS));
    pnlTemplates.add(templatePanel.getComponent());
    pnlTemplates.add(Box.createHorizontalGlue());
    // Deliver
    pnlDeliver.setLayout(new BoxLayout(pnlDeliver, BoxLayout.Y_AXIS));
    pnlDeliver.add(rbDeliverOff.getComponent());
    pnlDeliver.add(pnlDeliverToDirectory);
    pnlDeliver.add(rbDeliverMakeSubDirectory.getComponent());
    // InputDirectiveFile
    pnlInputDirectiveFile
      .setLayout(new BoxLayout(pnlInputDirectiveFile, BoxLayout.X_AXIS));
    pnlInputDirectiveFile.add(ftfInputDirectiveFile.getRootPanel());
    pnlInputDirectiveFile.add(btnClearInputDirectiveFile.getComponent());
    pnlInputDirectiveFile.add(Box.createRigidArea(new Dimension(100, 0)));
    pnlInputDirectiveFile.add(Box.createHorizontalGlue());
    // DeliverToDirectory
    pnlDeliverToDirectory
      .setLayout(new BoxLayout(pnlDeliverToDirectory, BoxLayout.X_AXIS));
    pnlDeliverToDirectory.add(rbDeliverToDirectory.getComponent());
    pnlDeliverToDirectory.add(ftfDeliverToDirectory.getRootPanel());
    // align
    UIUtilities.alignComponentsX(pnlBatch, Component.LEFT_ALIGNMENT);
    UIUtilities.alignComponentsX(pnlRoot, Component.LEFT_ALIGNMENT);
    UIUtilities.alignComponentsX(pnlDeliver, Component.LEFT_ALIGNMENT);
    // update
    processResult(ftfRootDir, true);
    stateChanged(null);
    statusChanged(status);
    mediator.setMethod(this, getProcessingMethod());
    updateDisplay();
  }

  private void addListeners() {
    templatePanel.addListeners();
    rbDeliverOff.addActionListener(this);
    rbDeliverToDirectory.addActionListener(this);
    rbDeliverMakeSubDirectory.addActionListener(this);
    templatePanel.addActionListener(this);
    cbCPUMachineList.addActionListener(this);
    rbGPUMachineListOff.addActionListener(this);
    rbGPUMachineListLocal.addActionListener(this);
    rbGPUMachineList.addActionListener(this);
    btnRun.addActionListener(this);
    ftfInputDirectiveFile.addResultListener(this);
    tabbedPane.addChangeListener(this);
    table.setTableListener(datasetDialog);
    btnReset.addActionListener(this);
    ctfEmailAddress.addActionListener(this);
    btnResume.addActionListener(this);
    btnPause.addActionListener(this);
    btnClearInputDirectiveFile.addActionListener(this);
    // This dialog can set the global status to open.
    addStatusChangeListener(stepPanel);
    table.msgStatusChangerStarted(this);
    // The step panel needs to listen for changes to the earliestRunStep.
    table.addStatusChangeListener(this);
    table.addStatusChangeListenerToRowList(this);
    tabbedPane.addMouseListener(new GenericMouseAdapter(this));
  }

  /**
   * Add listeners for StartOver button
   */
  public void addStatusChangeListener(final StatusChangeListener listener) {
    if (listener == null) {
      return;
    }
    boolean newElement = false;
    if (listeners == null) {
      synchronized (this) {
        if (listeners == null) {
          listeners = new ArrayList<StatusChangeListener>();
          newElement = true;
        }
      }
    }
    if (!newElement && listeners.contains(listener)) {
      return;
    }
    listeners.add(listener);
  }

  /**
   * Right mouse button context menu
   */
  public void popUpContextMenu(MouseEvent mouseEvent) {
    String[] manPagelabel = new String[] { "batchruntomo", "3dmod" };
    String[] manPage = new String[] { "batchruntomo.html", "3dmod.html" };
    String[] logFileLabel = new String[] { "batchruntomo" };
    String[] logFile = new String[] { getRootName() + ".log" };
    String anchor = null;
    if (curTab == BatchRunTomoTab.BATCH) {
      anchor = "BatchSetup";
    }
    else if (curTab == BatchRunTomoTab.STACKS) {
      anchor = "Stacks";
    }
    else if (curTab == BatchRunTomoTab.DATASET) {
      anchor = "SetValues";
    }
    else if (curTab == BatchRunTomoTab.RUN) {
      anchor = "Run";
    }
    ContextPopup contextPopup =
      new ContextPopup(pnlRoot, mouseEvent, anchor, ContextPopup.BATCHRUNTOMO_GUIDE,
        manPagelabel, manPage, logFileLabel, logFile, manager, axisID);
  }

  public DirectivesDialog getGlobalDirectivesDialog() {
    return datasetDialog.getDirectivesDialog();
  }

  Set<DirectiveDef> getBasicDirectives() {
    return basicDirectives;
  }

  public void msgStatusChangerStarted(final StatusChanger changer) {
    // Listen to the monitor.
    changer.addStatusChangeListener(this);
    changer.addStatusChangeListener(stepPanel);
    table.msgStatusChangerStarted(changer);
  }

  public Container getContainer() {
    return pnlRoot;
  }

  public void msgLoadDone() {
    addListeners();
    table.setFrame(true);
  }

  Map<DirectiveDef, String> getTemplateValues() {
    return templateValues;
  }

  /**
   * The dialog replaces the manager as the browsing directory.
   * @return
   */
  BrowsingDirectory getBrowsingDirectory() {
    return this;
  }

  BatchRunTomoDatasetDialog getDatasetDialog() {
    return datasetDialog;
  }

  public File getBrowsingDir() {
    if (validbrowsingDirectory == null) {
      validbrowsingDirectory = new ValidDirectory(manager);
      validbrowsingDirectory.setToPropertyUserDir();
    }
    if (!rbDeliverOff.isSelected()) {
      return validbrowsingDirectory.get();
    }
    // If the stacks are not going to be delivered there must only one stack in each
    // directory.
    return validbrowsingDirectory.getParent();
  }

  /**
   * Set valid browsing directory.
   */
  public void setBrowsingDir(final File input) {
    if (validbrowsingDirectory == null && input != null) {
      validbrowsingDirectory = new ValidDirectory(manager);
    }
    if (validbrowsingDirectory != null) {
      validbrowsingDirectory.set(input);
    }
  }

  void setBrowsingDir(final String input) {
    if (validbrowsingDirectory == null && input != null && !input.matches("\\s*")) {
      validbrowsingDirectory = new ValidDirectory(manager);
    }
    if (validbrowsingDirectory != null) {
      validbrowsingDirectory.set(input);
    }
  }

  /**
   * @deprecated 3/29/18
   * @param metaData
   */
  public void setParameters(final BatchRunTomoMetaData metaData) {
    setParameters(metaData, null, false);
  }

  /**
   * @param metaData
   * @param onlyStackIDDatasetDialog only loading the dataset dialog attached to the row with this stackID
   * @param onlyAdvancedDatasetDialog only loading an advanced dataset dialog
   */
  public void setParameters(final BatchRunTomoMetaData metaData,
    final String onlyStackIDDatasetDialog, final boolean onlyAdvancedDatasetDialog) {
    boolean initEntireDialog =
      onlyStackIDDatasetDialog == null && !onlyAdvancedDatasetDialog;
    boolean onlyGlobalDatasetDialog =
      onlyStackIDDatasetDialog == null && onlyAdvancedDatasetDialog;
    if (initEntireDialog) {
      setBrowsingDir(metaData.getBrowsingDirectory());
      ftfDeliverToDirectory.setText(metaData.getDeliverToDirectory());
    }
    if (!metaData.isRootNameNull()) {
      if (initEntireDialog) {
        ltfRootName.setText(metaData.getRootName());
        ftfRootDir.setText(manager.getPropertyUserDir());
        ftfInputDirectiveFile.setText(metaData.getInputDirectiveFile());
        ftfInputDirectiveFile.checkpoint();
        validateInputDirectiveFile();
      }
      if (!onlyGlobalDatasetDialog) {
        table.setParameters(metaData, onlyStackIDDatasetDialog,
          onlyAdvancedDatasetDialog);
      }
      if (initEntireDialog) {
        stepPanel.setParameters(metaData);
      }
      if (initEntireDialog) {
        datasetDialog.setParameters(metaData.getDatasetMetaData());
        phDatasetTable.set(metaData.getDatasetTableHeader());
        disableRootFields();
        statusChanged(metaData.getStatus());
      }
    }
    else if (initEntireDialog) {
      ltfRootName.setText("batch" + Utilities.getDateTimeStampRootName());
    }
    if (initEntireDialog && metaData.isDelivered()) {
      disableDeliveryFields();
    }
  }

  public boolean isParamFileModifiable() {
    return ltfRootName.isEditable();
  }

  public boolean isParamFileEmpty() {
    return ltfRootName.isEmpty() || ftfRootDir.isEmpty();
  }

  public void disableRootFields() {
    ltfRootName.setEditable(false);
    ftfRootDir.setEditable(false);
  }

  private void disableDeliveryFields() {
    manager.getMetaData().setDelivered(true);
    rbDeliverOff.setEditable(false);
    rbDeliverToDirectory.setEditable(false);
    ftfDeliverToDirectory.setEditable(false);
    rbDeliverMakeSubDirectory.setEditable(false);
  }

  public void getParameters(final UserConfiguration userConfiguration) {
    userConfiguration.setUseEmailAddress(ctfEmailAddress.isSelected());
    userConfiguration.setEmailAddress(ctfEmailAddress.getText());
  }

  /**
   * Get environment parameters.
   */
  public void getParameters() {
    cbCPUMachineList.setSelected(UserEnv.isParallelProcessing(null, AxisID.ONLY, null));
    if (UserEnv.isGpuProcessing(null, AxisID.ONLY, null)) {
      rbGPUMachineListLocal.setSelected(true);
    }
    else {
      rbGPUMachineListOff.setSelected(true);
    }
  }

  public void setParameters(final UserConfiguration userConfiguration,
    final boolean newDataset) {
    if (newDataset) {
      templatePanel.setParameters(userConfiguration);
    }
    ctfEmailAddress.setSelected(userConfiguration.isUseEmailAddress());
    ctfEmailAddress.setText(userConfiguration.getEmailAddress());
  }

  public void getParameters(final BatchRunTomoMetaData metaData) {
    if (validbrowsingDirectory != null) {
      metaData.setBrowsingDirectory(validbrowsingDirectory.get());
    }
    metaData.setDeliverToDirectory(ftfDeliverToDirectory.getFile());
    metaData.setInputDirectiveFile(ftfInputDirectiveFile.getFile());
    table.getParameters(metaData);
    stepPanel.getParameters(metaData);
    datasetDialog.getParameters(metaData.getDatasetMetaData());
    metaData.setDatasetTableHeader(phDatasetTable);
    metaData.setStatus(status);
  }

  public void setParameters(final BatchruntomoParam param) {
    rbDeliverOff.setSelected(true);
    if (param.isDeliverToDirectorySet()) {
      rbDeliverToDirectory.setSelected(true);
      ftfDeliverToDirectory.setText(param.getDeliverToDirectory());
    }
    if (param.isMakeSubDirectory()) {
      rbDeliverMakeSubDirectory.setSelected(true);
    }
    cbCPUMachineList.setSelected(!param.isCpuMachineListNull());
    if (param.isGpuMachineListNull()) {
      rbGPUMachineListOff.setSelected(true);
    }
    else if (param.gpuMachineListEquals(BatchruntomoParam.MACHINE_LIST_LOCAL_VALUE)) {
      rbGPUMachineListLocal.setSelected(true);
    }
    else {
      rbGPUMachineList.setSelected(true);
    }
    if (!param.isEmailAddressNull()) {
      ctfEmailAddress.setSelected(true);
      ctfEmailAddress.setText(param.getEmailAddress());
    }
    stepPanel.setParameters(param);
    updateDisplay();
  }

  /**
   * @param param
   * @param doValidation
   * @param forUpdate
   * @return
   */
  public boolean getParameters(final BatchruntomoParam param, final boolean doValidation,
    final boolean forUpdate) {
    StringBuilder errMsg = new StringBuilder();
    try {
      if (!forUpdate) {
        if (rbDeliverOff.isSelected()) {
          param.resetDeliver();
        }
        else if (rbDeliverToDirectory.isSelected()) {
          param.setDeliverToDirectory(ftfDeliverToDirectory.getFile(doValidation, this));
        }
        else if (rbDeliverMakeSubDirectory.isSelected()) {
          param.setMakeSubDirectory(true);
        }
        if (ctfEmailAddress.isSelected()) {
          param.setEmailAddress(ctfEmailAddress.getText(doValidation, runFieldDisplayer));
        }
        else {
          param.resetEmailAddress();
        }
      }
      if (!cbCPUMachineList.isSelected()) {
        param.setCPUMachineList(BatchruntomoParam.MACHINE_LIST_LOCAL_VALUE);
      }
      if (rbGPUMachineListOff.isSelected()) {
        param.resetGPUMachineList();
      }
      else if (rbGPUMachineListLocal.isSelected()) {
        param.setGPUMachineList(BatchruntomoParam.MACHINE_LIST_LOCAL_VALUE);
      }
      if (!table.getParameters(param, rbDeliverOff.isSelected(),
        rbDeliverToDirectory.isSelected(), errMsg, doValidation)) {
        return false;
      }
      if (!forUpdate) {
        stepPanel.getParameters(param);
      }
      if (doValidation) {
        if (errMsg.length() > 0) {
          UIHarness.INSTANCE.openMessageDialog(manager, errMsg.toString(),
            "Unable to Set Up Directories");
          return false;
        }
        disableDeliveryFields();
      }
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  public void loadTemplates() {
    // load templates from global autodoc
    DirectiveFile directiveFile = DirectiveFile.getInstance(manager, axisID,
      FileType.BATCH_RUN_TOMO_GLOBAL_AUTODOC.getFile(manager, axisID),
      DirectiveFileType.BATCH);
    templatePanel.setParameters(directiveFile);
  }

  /**
   * @deprecated 3/29/18
   */
  public void loadAutodocs() {
    loadAutodocs(null, false);
  }

  /**
   * @param onlyStackIDDatasetDialog only loading the dataset dialog attached to the row with this stackID
   * @param onlyAdvancedDatasetDialog only loading an advanced dataset dialog
   */
  public void loadAutodocs(final String onlyStackIDDatasetDialog,
    final boolean onlyAdvancedDatasetDialog) {
    if (onlyStackIDDatasetDialog == null) {
      // load global autodoc
      datasetDialog.setValues(DirectiveFile.getInstance(manager, axisID,
        FileType.BATCH_RUN_TOMO_GLOBAL_AUTODOC.getFile(manager, axisID),
        DirectiveFileType.BATCH), false, onlyAdvancedDatasetDialog, true);
    }
    // load dataset autodocs
    table.loadAutodocs(onlyStackIDDatasetDialog, onlyAdvancedDatasetDialog);
  }

  /**
   * @deprecated 3/29/18
   * @param metaData
   */
  public boolean saveAutodocs(final boolean doValidation) {
    return saveAutodocs(doValidation, null, false);
  }

  /**
   * @param doValidation
   * @param autodocStackID don't save all dataset autodocs - just this one
   * @param onlyGlobalAutodoc don't save any dataset autodocs
   * 
   * @return
   */
  public boolean saveAutodocs(final boolean doValidation, final String autodocStackID,
    final boolean onlyGlobalAutodoc) {
    // save global autodoc
    File batchFile = FileType.BATCH_RUN_TOMO_GLOBAL_AUTODOC.getFile(manager, null);
    NameValuePairList templates = null;
    // If the advanced dialog was never created, then load the save file first as not all
    // of it was loaded into the dialog.
    boolean advancedDialogExists = datasetDialog.isAdvancedDialogExists();
    NameValuePairList loadedBatchList = null;
    try {
      if (batchFile.exists()) {
        if (!advancedDialogExists) {
          loadedBatchList =
            new NameValuePairList(AutodocFactory.getAutodocInstance(manager, batchFile));
        }
        Utilities.deleteFile(batchFile, manager, axisID);
      }
      Autodoc batchAutodoc = null;
      batchAutodoc = AutodocFactory.getWritableAutodocInstance(manager, batchFile);
      templatePanel.saveAutodoc(batchAutodoc);
      if (!datasetDialog.saveAutodoc(batchAutodoc, doValidation)) {
        return false;
      }
      templates = BatchTool.mergeTemplates(manager, templatePanel.getFiles());
      NameValuePairList saveBatchList;
      if (advancedDialogExists) {
        saveBatchList = BatchTool.createBatchFile(manager, batchAutodoc,
          advancedDialogExists, null, null, null, templates);
      }
      else {
        // No advanced dialog - take the advanced directives from the files
        saveBatchList =
          BatchTool.createBatchFile(manager, batchAutodoc, advancedDialogExists,
            loadedBatchList, basicDirectives, getAdvancedStartingBatch(), templates);
      }
      saveBatchList.write(batchAutodoc.getLogFile());
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    // save dataset autodocs with the starting batch and default batch directive files
    // grafted on.
    if (autodocStackID != null || !onlyGlobalAutodoc) {
      return table.saveAutodocs(templatePanel, templates, doValidation,
        (rbDeliverToDirectory.isSelected() ? ftfDeliverToDirectory.getFile() : null),
        autodocStackID);
    }
    return true;
  }

  private Autodoc getInputDirectiveAutodoc(final StringBuilder errMsg) {
    File file = ftfInputDirectiveFile.getFile();
    if (file == null || !file.exists()) {
      return null;
    }
    try {
      return AutodocFactory.getAutodocInstance(manager, file, errMsg);
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private boolean validateInputDirectiveFile() {
    StringBuilder errMsg = new StringBuilder();
    Autodoc autodoc = getInputDirectiveAutodoc(errMsg);
    if (autodoc == null) {
      return true;
    }
    if (autodoc.isError()) {
      display();
      Popup.getUnformattedErrorInstance(ftfInputDirectiveFile,
        "Errors in Starting Directive File",
        "Syntax error in the Starting Directive File - unable to load.\nPlease correct "
          + "the file and reload.\n\n" + errMsg.toString())
        .open();
      return false;
    }
    return true;
  }

  NameValuePairList getAdvancedStartingBatch() {
    if (advancedStartingBatch == null) {
      Autodoc autodoc = getInputDirectiveAutodoc(null);
      if (autodoc != null) {
        advancedStartingBatch = new NameValuePairList(autodoc);
        advancedStartingBatch.subtract(basicDirectives);
      }
    }
    return advancedStartingBatch;
  }

  BatchRunTomoRow getFirstRow() {
    return table.getFirstRow();
  }

  /**
   * @deprecated 3/29/18
   * @param init
   */
  public void updateDirectives(final boolean init) {
    updateDirectives(init, null, false);
  }

  public ImageFilenameStyle getDatasetImageFilenameStyle() {
    return table.getImageFilenameStyle();
  }

  /**
   * @param init
   * @param onlyStackIDDatasetDialog only loading the dataset dialog attached to the row with this stackID
   * @param onlyAdvancedDatasetDialog only loading an advanced dataset dialog
   */
  public void updateDirectives(final boolean init, final String onlyStackIDDatasetDialog,
    final boolean onlyAdvancedDatasetDialog) {
    boolean onlyGlobalDatasetDialog =
      onlyStackIDDatasetDialog == null && onlyAdvancedDatasetDialog;
    boolean retainUserValues = false;
    if (!init) {
      // See if the user has changed any values (and back up the changed values).
      boolean changed = false;
      if (!onlyGlobalDatasetDialog
        && table.backupIfChanged(onlyStackIDDatasetDialog, onlyAdvancedDatasetDialog)) {
        changed = true;
      }
      if (onlyStackIDDatasetDialog == null
        && datasetDialog.backupIfChanged(onlyAdvancedDatasetDialog)) {
        changed = true;
      }
      if (!retainUserValues && changed) {
        // Ask the user whether they want to keep the values they changed.
        retainUserValues = UIHarness.INSTANCE.openYesNoDialog(manager,
          "New batch directive/template values will be applied.  Keep your changed "
            + "values?",
          axisID);
      }
    }
    if (!onlyGlobalDatasetDialog) {
      table.applyValues(init, retainUserValues, directiveFileCollection,
        onlyStackIDDatasetDialog, onlyAdvancedDatasetDialog);
    }
    if (onlyStackIDDatasetDialog == null) {
      datasetDialog.applyValues(init, retainUserValues, directiveFileCollection,
        onlyAdvancedDatasetDialog);
    }
  }

  private boolean validate() {
    if (!validateInputDirectiveFile()) {
      return false;
    }
    if (!datasetDialog.validate()) {
      return false;
    }
    if (!table.validate(datasetDialog)) {
      return false;
    }
    return true;
  }

  public DirectiveFileCollection getDirectiveFileCollection() {
    return directiveFileCollection;
  }

  public void actionPerformed(final ActionEvent event) {
    String actionCommand = event.getActionCommand();
    if (actionCommand == null) {
      return;
    }
    if (templatePanel.equalsActionCommand(actionCommand)) {
      // refresh the shared directive file collection
      templatePanel.refreshDirectiveFileCollection();
      updateDirectives(false, null, false);
    }
    else if (actionCommand.equals(btnRun.getActionCommand())) {
      if (validate()) {
        manager.batchruntomo(table.createRunKeys());
      }
    }
    else if (actionCommand.equals(btnResume.getActionCommand())) {
      manager.resumeBatchruntomo(table.updateRunKeys());
    }
    else if (actionCommand.equals(cbCPUMachineList.getActionCommand())
      || actionCommand.equals(rbGPUMachineListOff.getActionCommand())
      || actionCommand.equals(rbGPUMachineListLocal.getActionCommand())
      || actionCommand.equals(rbGPUMachineList.getActionCommand())) {
      mediator.setMethod(this, getProcessingMethod(), getSecondaryProcessingMethod(),
        curTab == BatchRunTomoTab.RUN);
    }
    else if (actionCommand.equals(btnReset.getActionCommand())) {
      startOver();
    }
    else if (actionCommand.equals(btnPause.getActionCommand())) {
      manager.pause(axisID);
    }
    else if (actionCommand.equals(btnClearInputDirectiveFile.getActionCommand())) {
      ftfInputDirectiveFile.clear();
      ftfInputDirectiveFile.checkpoint();
    }
    else {
      updateDisplay();
    }
  }

  public void startOver() {
    statusChanged(BatchRunTomoStatus.OPEN);
    if (listeners != null) {
      for (int i = 0; i < listeners.size(); i++) {
        listeners.get(i).statusChanged(status);
      }
    }
  }

  /**
   * Returns one of the two possible methods.  Always returns a processing method.
   */
  public ProcessingMethod getProcessingMethod() {
    if (cbCPUMachineList.isSelected()) {
      return ProcessingMethod.PP_CPU;
    }
    if (rbGPUMachineList.isSelected()) {
      return ProcessingMethod.PP_GPU;
    }
    if (rbGPUMachineListLocal.isSelected()) {
      return ProcessingMethod.LOCAL_GPU;
    }
    return ProcessingMethod.DEFAULT;
  }

  /**
   * Returns a processing method when there are two non-default methods in force,
   * otherwise returns null.
   */
  public ProcessingMethod getSecondaryProcessingMethod() {
    if (cbCPUMachineList.isSelected()) {
      // two non-default processing methods are in force
      if (rbGPUMachineList.isSelected()) {
        return ProcessingMethod.PP_GPU;
      }
      if (rbGPUMachineListLocal.isSelected()) {
        return ProcessingMethod.LOCAL_GPU;
      }
    }
    return null;
  }

  /**
   * No effect because queue is not available
   */
  public void disableGpu(final boolean disable) {}

  /**
   * No effect because the processing method is not used for running processes by etomo.
   */
  public void lockProcessingMethod(boolean lock) {}

  public String getRootName() {
    return ltfRootName.getText();
  }

  public File getRootDir() {
    return ftfRootDir.getFile();
  }

  boolean isTrackingMethodSeed() {
    return datasetDialog.isTrackingMethodSeed();
  }

  /**
   * Processes a result from object.
   * @param object
   * @param init - true when result was caused by the creating a dialog, rather then by a direct user action.
   */
  public void processResult(final Object object, final boolean init) {
    if (object == ftfInputDirectiveFile
      && ftfInputDirectiveFile.isDifferentFromCheckpoint(false)) {
      ftfInputDirectiveFile.checkpoint();
      if (validateInputDirectiveFile()) {
        directiveFileCollection.setDirectiveFile(ftfInputDirectiveFile.getFile(),
          DirectiveFileType.BATCH);
        // The templates in the starting batch are valid with the start batch values and
        // should be used.
        templatePanel.activateActions(false);
        templatePanel.clear();
        templatePanel.setParameters(
          directiveFileCollection.getDirectiveFile(DirectiveFileType.BATCH));
        templatePanel.activateActions(true);
        updateDirectives(init);
      }
    }
  }

  public void expand(final ExpandButton button) {
    boolean expanded = button.isExpanded();
    if (button == phDatasetTable.getOpenCloseButton()) {
      pnlDatasetTableBody.setVisible(expanded);
    }
    UIHarness.INSTANCE.pack(manager);
  }

  public void pack() {
    // Prevent the table from expanding horizontally
    if (curTab == BatchRunTomoTab.DATASET) {
      pnlDatasetTableBody.removeAll();
      int datasetWidth = datasetDialog.getPreferredWidth();
      int tableWidth = table.getPreferredWidth();
      if (datasetWidth != 0 && tableWidth != 0 && datasetWidth > tableWidth) {
        int padding = (datasetWidth - tableWidth) / 2;
        pnlDatasetTableBody.add(Box.createHorizontalStrut(padding));
        pnlDatasetTableBody.add(pnlUntitledTable);
        pnlDatasetTableBody.add(Box.createHorizontalStrut(padding));
      }
      else {
        pnlDatasetTableBody.add(pnlUntitledTable);
      }
    }
  }

  public void expand(final GlobalExpandButton button) {}

  /**
   * Display the correct tab.
   */
  public void display() {
    display(BatchRunTomoTab.BATCH);
  }

  /**
   * Displays the tab, if it is not displayed.
   */
  void display(final BatchRunTomoTab tab) {
    if (curTab != tab && tab != null) {
      tabbedPane.setSelectedIndex(tab.getIndex());
    }
  }

  void setDatasetTableVisible(final boolean visible) {
    pnlDatasetTable.setVisible(visible);
  }

  /**
   * Handle tab change event
   */
  public void stateChanged(final ChangeEvent event) {
    int curIndex;
    if (curTab == null) {
      tabbedPane.setSelectedIndex(BatchRunTomoTab.DEFAULT.getIndex());
      curTab = BatchRunTomoTab.DEFAULT;
      curIndex = curTab.getIndex();
    }
    else {
      pnlTabs[curTab.getIndex()].removeAll();
      curTab = BatchRunTomoTab.getInstance(tabbedPane.getSelectedIndex());
      curIndex = curTab.getIndex();
      pnlTable.removeAll();
      pnlUntitledTable.removeAll();
    }
    if (curTab == BatchRunTomoTab.BATCH) {
      pnlTabs[curIndex].add(pnlBatch);
    }
    else if (curTab == BatchRunTomoTab.STACKS) {
      pnlTabs[curIndex].add(pnlStacks);
      table.msgTabChanged(curTab);
      pnlStacks.add(pnlTable);
      pnlTable.add(table.getComponent());
    }
    else if (curTab == BatchRunTomoTab.DATASET) {
      pnlTabs[curIndex].add(pnlDataset);
      table.msgTabChanged(curTab);
      pnlUntitledTable.add(table.getComponent());
      UIUtilities.alignComponentsX(pnlDataset, Component.LEFT_ALIGNMENT);
    }
    else if (curTab == BatchRunTomoTab.RUN) {
      pnlTabs[curIndex].add(pnlRun);
      table.msgTabChanged(curTab);
      pnlRun.add(pnlTable);
      pnlTable.add(table.getComponent());
      UIUtilities.alignComponentsX(pnlRun, Component.LEFT_ALIGNMENT);
    }
    mediator.setMethod(this, getProcessingMethod(), getSecondaryProcessingMethod(),
      curTab == BatchRunTomoTab.RUN);
    UIHarness.INSTANCE.pack(axisID, manager);
  }

  private void updateDisplay() {
    ftfDeliverToDirectory.setEnabled(rbDeliverToDirectory.isSelected());
  }

  public boolean isStatusKilledPaused() {
    return status == BatchRunTomoStatus.KILLED_PAUSED;
  }

  public void statusChanged(final Status status) {
    if (status == null || !(status instanceof BatchRunTomoStatus)) {
      return;
    }
    this.status = (BatchRunTomoStatus) status;
    boolean open = status == BatchRunTomoStatus.OPEN;
    ctfEmailAddress.setEditable(open);
    btnRun.setEditable(open || status == BatchRunTomoStatus.STOPPED);
    ftfInputDirectiveFile.setEditable(open);
    btnClearInputDirectiveFile.setEditable(open);
    templatePanel.setEditable(open);
    // Running - enable
    // pause
    boolean running = status == BatchRunTomoStatus.RUNNING;
    btnPause.setEditable(running);
    cbCPUMachineList.setEditable(!running);
    rbGPUMachineListOff.setEditable(!running);
    rbGPUMachineListLocal.setEditable(!running);
    rbGPUMachineList.setEditable(!running);
    // Killed/paused - enable:
    // resume
    // start over
    boolean killedPaused = status == BatchRunTomoStatus.KILLED_PAUSED;
    btnResume.setEditable(killedPaused);
    // Stopped - enable:
    // start over
    btnReset.setEditable(killedPaused || status == BatchRunTomoStatus.STOPPED);
    datasetDialog.statusChanged(this.status);
  }

  public void statusChanged(final StatusChangeEvent statusChangeEvent) {
    datasetDialog.statusChanged(statusChangeEvent);
  }

  private void setTooltips() {
    ReadOnlyAutodoc autodoc = null;
    try {
      autodoc =
        AutodocFactory.getInstance(manager, AutodocFactory.BATCH_RUN_TOMO, axisID, false);
    }
    catch (FileNotFoundException except) {
      except.printStackTrace();
    }
    catch (IOException except) {
      except.printStackTrace();
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    rbDeliverOff.setToolTipText(
      "No delivery.  Dataset will be processed in the original location of the stack; "
        + "stacks must all be in separate directories.");
    String tooltip =
      EtomoAutodoc.getTooltip(autodoc, BatchruntomoParam.DELIVER_TO_DIRECTORY_TAG);
    rbDeliverToDirectory.setToolTipText(tooltip);
    ftfDeliverToDirectory.setToolTipText(tooltip);
    rbDeliverMakeSubDirectory.setToolTipText(
      "Make a subdirectory for each dataset under the directory where the stack is "
        + "currently located");
    ctfEmailAddress.setToolTipText("Send emails on failure or final completion.");
    cbCPUMachineList
      .setToolTipText("Use multiple cores or multiple computers for the processing.");
    rbGPUMachineListLocal
      .setToolTipText("Use one GPU on the local machine for reconstruction.");
    rbGPUMachineList.setToolTipText("Use multiple GPUs for reconstruction.");
    ltfRootName.setToolTipText("Root name for batch project files (.com, .adoc, .ebt).");
    ftfRootDir.setToolTipText("Location into which batch project files will be written");
    rbGPUMachineListOff.setToolTipText("No GPU will be used.");
    btnRun.setToolTipText("Saves with validation and runs batchruntomo.");
    ftfInputDirectiveFile.setToolTipText(
      "Select an existing batch directive file to set initial values of parameters, "
        + "after applying template values");
    btnClearInputDirectiveFile.setToolTipText(
      "Clears the entry in " + ftfInputDirectiveFile.getQuotedLabel() + ".");
    btnReset.setToolTipText("Makes fields editable. Cancels ability to Resume.  You must "
      + "uncheck Run checkboxes or change Start From entries, if you wish to avoid "
      + "rerunning Stopped datasets.");
    btnPause.setToolTipText("Finishes the current dataset and then stops.");
    btnResume.setToolTipText("Continues the current batchruntomo run.");
    templatePanel.setScopeTooltip(
      "Select the first system-wide template file from which parameters will be set.");
    templatePanel.setSystemTooltip(
      "Select the second system-wide template file from which parameters will be set.");
    templatePanel.setUserTooltip(
      "Select a personal template file from which parameters will be set.");
  }

  private final class RunFieldDisplayer implements FieldDisplayer {
    private final BatchRunTomoDialog dialog;

    private RunFieldDisplayer(final BatchRunTomoDialog dialog) {
      this.dialog = dialog;
    }

    public void display() {
      dialog.display(BatchRunTomoTab.RUN);
    }
  }
}
