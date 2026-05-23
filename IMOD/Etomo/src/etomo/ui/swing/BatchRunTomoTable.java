package etomo.ui.swing;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import etomo.BatchRunTomoManager;
import etomo.EtomoDirector;
import etomo.comscript.BatchruntomoParam;
import etomo.logic.DatasetTool;
import etomo.process.TomosetextsOutput;
import etomo.storage.DirectiveDef;
import etomo.storage.DirectiveFileCollection;
import etomo.storage.NameValuePairList;
import etomo.storage.StackFileFilter;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.BatchRunTomoMetaData;
import etomo.type.BatchRunTomoRowMetaData;
import etomo.type.BatchRunTomoStatus;
import etomo.type.CurrentArrayList;
import etomo.type.DuplicateException;
import etomo.type.EndingStep;
import etomo.type.FrameStatus;
import etomo.type.ImageFilenameStyle;
import etomo.type.NotLoadedException;
import etomo.type.OrderedHashMap;
import etomo.type.Status;
import etomo.type.StatusChangeBooleanEvent;
import etomo.type.StatusChangeEvent;
import etomo.type.StatusChangeListener;
import etomo.type.StatusChanger;
import etomo.type.TableReference;
import etomo.type.UserConfiguration;
import etomo.ui.BatchRunTomoTab;
import etomo.ui.BrowsingDirectory;
import etomo.ui.FieldDisplayer;
import etomo.ui.PreferredTableSize;
import etomo.ui.SharedStrings;
import etomo.ui.TableListener;
import etomo.ui.UIComponent;
import etomo.util.UniqueKey;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2014 - 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class BatchRunTomoTable extends HighlightableTable
  implements Viewable, Expandable, ActionListener, StatusChangeListener, StatusChanger,
  UIComponent, SwingComponent, FieldDisplayer {
  private static final String STACK_TITLE = "Stack";
  private static final int MAX_HEADER_ROWS = 3;
  private static final int NUM_STACKS_HEADER_ROWS = MAX_HEADER_ROWS;
  private static final int NUM_DATASET_HEADER_ROWS = 1;
  private static final int NUM_RUN_HEADER_ROWS = 2;
  private static final AxisID AXIS_ID = AxisID.ONLY;
  private static final String RUN_LABEL = "Run";
  private static final String UNIQUE_KEY = "BatchRunTomo";

  private final JPanel pnlRoot = new JPanel();
  private final JPanel pnlTable = new JPanel();
  private final GridBagLayout layout = new GridBagLayout();
  private final GridBagConstraints constraints = new GridBagConstraints();
  // Both tabs
  private final HeaderCell[] hcNumber = new HeaderCell[NUM_STACKS_HEADER_ROWS];
  private final HeaderCell[] hcStack = new HeaderCell[NUM_STACKS_HEADER_ROWS];
  // stacks tab
  private final HeaderCell[] hcDual = new HeaderCell[NUM_STACKS_HEADER_ROWS];
  private final HeaderCell[] hcMontage = new HeaderCell[NUM_STACKS_HEADER_ROWS];
  private final HeaderCell[] hcSkip = new HeaderCell[NUM_STACKS_HEADER_ROWS];
  private final HeaderCell hcSkipB = new HeaderCell("from B");
  private final HeaderCell[] hcBoundaryModel = new HeaderCell[NUM_STACKS_HEADER_ROWS];
  private final HeaderCell[] hcSurfacesToAnalyze = new HeaderCell[NUM_STACKS_HEADER_ROWS];
  private final HeaderCell[] hc3dmod = new HeaderCell[NUM_STACKS_HEADER_ROWS];
  private final HeaderCell hc3dmodB = new HeaderCell("B");
  // dataset tab
  private final HeaderCell[] hcEditDataset = new HeaderCell[NUM_DATASET_HEADER_ROWS];
  // run tab
  private final HeaderCell[] hcStatus = new HeaderCell[NUM_RUN_HEADER_ROWS];
  private final HeaderCell[] hcStep = new HeaderCell[NUM_RUN_HEADER_ROWS];
  private final HeaderCell[] hcRun = new HeaderCell[NUM_RUN_HEADER_ROWS];
  private final HeaderCell[] hcEtomo = new HeaderCell[NUM_RUN_HEADER_ROWS];
  private final HeaderCell[] hcRec = new HeaderCell[NUM_RUN_HEADER_ROWS];
  private final HeaderCell[] hcLog = new HeaderCell[NUM_RUN_HEADER_ROWS];
  private final SingleLineButton btnAdd = new SingleLineButton("Add Stack(s)");
  private final SingleLineButton btnCopyDown = new SingleLineButton("Copy Down");
  private final SingleLineButton btnDelete = new SingleLineButton("Delete");
  private final JPanel pnlStackButtons = new JPanel();
  private final PreferredTableSize preferredTableSize =
    new PreferredTableSize(DatasetColumn.TOTAL);
  private final Viewport viewport = new Viewport(this,
    EtomoDirector.INSTANCE.getUserConfiguration().getBatchTableSize().getInt(),
    UNIQUE_KEY);

  private final RowList rowList;
  private final BatchRunTomoManager manager;
  private final ExpandButton btnStack;
  private final JComponent[] focusableParents;
  private final BatchRunTomoDialog dialog;
  private final Set<DirectiveDef> basicDirectives;

  private ArrayList<StatusChangeListener> listeners = null;
  private BatchRunTomoTab curTab = null;
  private BatchRunTomoStatus status = BatchRunTomoStatus.DEFAULT;
  private boolean montageFrame = false;
  private boolean singleFrame = false;

  private BatchRunTomoTable(final BatchRunTomoManager manager,
    final BatchRunTomoDialog dialog, final Set<DirectiveDef> basicDirectives,
    final TableReference tableReference, final JComponent focusableParent) {
    super(UNIQUE_KEY);
    this.manager = manager;
    this.dialog = dialog;
    this.basicDirectives = basicDirectives;
    rowList = new RowList(this, tableReference);
    btnStack = ExpandButton.getInstance(this, dialog, ExpandButton.Type.MORE);
    focusableParents = new JComponent[] { pnlRoot, focusableParent };
  }

  static BatchRunTomoTable getInstance(final BatchRunTomoManager manager,
    final BatchRunTomoDialog dialog, final Set<DirectiveDef> basicDirectives,
    final TableReference tableReference, final JComponent focusableParent) {
    BatchRunTomoTable instance = new BatchRunTomoTable(manager, dialog, basicDirectives,
      tableReference, focusableParent);
    instance.createPanel();
    instance.setTooltips();
    instance.addListeners();
    return instance;
  }

  boolean hasDual() {
    return rowList.hasDual();
  }

  public JComponent[] getFocusableParents() {
    return focusableParents;
  }

  private void createPanel() {
    // init
    JPanel pnlView = new JPanel();
    btnAdd.setToPreferredSize();
    btnCopyDown.setToPreferredSize();
    btnDelete.setToPreferredSize();
    btnStack.setName(STACK_TITLE);
    viewport.initPaging();
    initHighlightHotkeys();
    // all table tabs
    hcNumber[0] = new HeaderCell();
    hcNumber[1] = new HeaderCell();
    hcNumber[2] = new HeaderCell("#");
    hcStack[0] = new HeaderCell();
    hcStack[1] = new HeaderCell();
    hcStack[2] = new HeaderCell(STACK_TITLE);
    // stacks tab
    hcDual[0] = new HeaderCell();
    hcDual[1] = new HeaderCell("Dual");
    hcDual[2] = new HeaderCell("Axis");
    hcMontage[0] = new HeaderCell();
    hcMontage[1] = new HeaderCell();
    hcMontage[2] = new HeaderCell("Montage");
    hcSkip[0] = new HeaderCell("Exclude");
    hcSkip[1] = new HeaderCell("Views");
    hcSkip[2] = new HeaderCell("from A");
    hcBoundaryModel[0] = new HeaderCell();
    hcBoundaryModel[1] = new HeaderCell("Boundary");
    hcBoundaryModel[2] = new HeaderCell("Model");
    hcSurfacesToAnalyze[0] = new HeaderCell("Beads");
    hcSurfacesToAnalyze[1] = new HeaderCell("on Two");
    hcSurfacesToAnalyze[2] = new HeaderCell("Surfaces");
    hc3dmod[0] = new HeaderCell("Open");
    hc3dmod[1] = new HeaderCell("Stack");
    hc3dmod[2] = new HeaderCell("A");
    // dataset tab
    hcEditDataset[0] = new HeaderCell("Specific Values");
    // run tab
    hcStatus[0] = new HeaderCell();
    hcStatus[1] = new HeaderCell("Status");
    hcStep[0] = new HeaderCell();
    hcStep[1] = new HeaderCell("Reached");
    hcRun[0] = new HeaderCell();
    hcRun[1] = new HeaderCell(RUN_LABEL);
    hcEtomo[0] = new HeaderCell("Open");
    hcEtomo[1] = new HeaderCell("Set");
    hcRec[0] = new HeaderCell("Open");
    hcRec[1] = new HeaderCell("Rec");
    hcLog[0] = new HeaderCell("Open");
    hcLog[1] = new HeaderCell("Log");
    // preferred width of the dataset view
    preferredTableSize.addColumn(DatasetColumn.NUMBER.index, hcNumber[2]);
    preferredTableSize.addColumn(DatasetColumn.STACK.index, hcStack[2], btnStack);
    preferredTableSize.addColumn(DatasetColumn.EDIT_DATASET.index, hcEditDataset[0]);
    // Root
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
    pnlRoot.add(pnlView);
    pnlRoot.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlRoot.add(pnlStackButtons);
    // View
    pnlView.setLayout(new BoxLayout(pnlView, BoxLayout.X_AXIS));
    pnlView.add(viewport.getPagingPanel());
    pnlView.add(pnlTable);
    // stack Buttons
    pnlStackButtons.setLayout(new BoxLayout(pnlStackButtons, BoxLayout.X_AXIS));
    pnlStackButtons.add(Box.createHorizontalGlue());
    pnlStackButtons.add(btnAdd.getComponent());
    pnlStackButtons.add(Box.createHorizontalGlue());
    pnlStackButtons.add(btnCopyDown.getComponent());
    pnlStackButtons.add(Box.createHorizontalGlue());
    pnlStackButtons.add(btnDelete.getComponent());
    pnlStackButtons.add(Box.createHorizontalGlue());
    // Table
    pnlTable.setLayout(layout);
    pnlTable.setBorder(LineBorder.createBlackLineBorder());
    // constraints
    constraints.fill = GridBagConstraints.BOTH;
    constraints.anchor = GridBagConstraints.CENTER;
    constraints.gridheight = 1;
    constraints.weighty = 1.0;
    // align
    UIUtilities.alignComponentsX(pnlRoot, Component.LEFT_ALIGNMENT);
    // update
    viewport.adjustViewport(-1);
    updateDisplay();
    statusChanged(status);
  }

  public void display() {
    dialog.display(BatchRunTomoTab.STACKS);
  }

  NameValuePairList getAdvancedStartingBatch() {
    return dialog.getAdvancedStartingBatch();
  }

  int getPreferredWidth() {
    return preferredTableSize.getPreferredWidth();
  }

  BatchRunTomoDatasetDialog getDatasetDialog() {
    return dialog.getDatasetDialog();
  }

  boolean isTrackingMethodSeed() {
    return dialog.isTrackingMethodSeed();
  }

  EndingStep getEarliestRunEndingStep() {
    return rowList.getEarliestRunEndingStep();
  }

  public ImageFilenameStyle getImageFilenameStyle() {
    return rowList.getImageFilenameStyle();
  }

  private void rebuildTable() {
    // remove table
    rowList.removeAll();
    pnlTable.removeAll();
    // header
    constraints.weightx = 0.0;
    int numRows;
    if (curTab == BatchRunTomoTab.STACKS) {
      numRows = NUM_STACKS_HEADER_ROWS;
      for (int i = 0; i < numRows; i++) {
        addStandardHeaders(i, numRows);
        constraints.gridwidth = 1;
        hcDual[i].add(pnlTable, layout, constraints);
        hcMontage[i].add(pnlTable, layout, constraints);
        // exclude views has A and B columns
        if (i < numRows - 1) {
          constraints.gridwidth = 2;
        }
        hcSkip[i].add(pnlTable, layout, constraints);
        constraints.gridwidth = 1;
        if (i == numRows - 1) {
          hcSkipB.add(pnlTable, layout, constraints);
        }
        hcBoundaryModel[i].add(pnlTable, layout, constraints);
        hcSurfacesToAnalyze[i].add(pnlTable, layout, constraints);
        if (i < numRows - 1) {
          constraints.gridwidth = GridBagConstraints.REMAINDER;
        }
        hc3dmod[i].add(pnlTable, layout, constraints);
        if (i == numRows - 1) {
          constraints.gridwidth = GridBagConstraints.REMAINDER;
          hc3dmodB.add(pnlTable, layout, constraints);
        }
      }
    }
    else if (curTab == BatchRunTomoTab.DATASET) {
      numRows = NUM_DATASET_HEADER_ROWS;
      for (int i = 0; i < numRows; i++) {
        addStandardHeaders(i, numRows);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 100.0;
        hcEditDataset[i].add(pnlTable, layout, constraints);
        constraints.weightx = 0.0;
      }
    }
    else if (curTab == BatchRunTomoTab.RUN) {
      numRows = NUM_RUN_HEADER_ROWS;
      for (int i = 0; i < numRows; i++) {
        addStandardHeaders(i, numRows);
        constraints.gridwidth = 1;
        hcStatus[i].add(pnlTable, layout, constraints);
        hcStep[i].add(pnlTable, layout, constraints);
        hcRun[i].add(pnlTable, layout, constraints);
        hcEtomo[i].add(pnlTable, layout, constraints);
        hcRec[i].add(pnlTable, layout, constraints);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        hcLog[i].add(pnlTable, layout, constraints);
      }
    }
    // rows
    rowList.removeAll();
    rowList.display(viewport);
    // buttons
    pnlStackButtons.setVisible(curTab == BatchRunTomoTab.STACKS);
  }

  private void addStandardHeaders(int index, int numRows) {
    // Use the bottom rows of the standard headers
    if (numRows < MAX_HEADER_ROWS) {
      int shift = MAX_HEADER_ROWS - numRows;
      index += shift;
      numRows += shift;
    }
    // the stacks tab rows are highlightable
    if (curTab == BatchRunTomoTab.STACKS) {
      constraints.gridwidth = 2;
    }
    else {
      constraints.gridwidth = 1;
    }
    int newPreferredWidth = 0;
    hcNumber[index].add(pnlTable, layout, constraints);
    // The stack header has a button on the bottom row
    if (index == numRows - 1) {
      constraints.gridwidth = 1;
    }
    else {
      constraints.gridwidth = 2;
    }
    constraints.weightx = 10.0;
    int width = 0;
    hcStack[index].add(pnlTable, layout, constraints);
    if (curTab == BatchRunTomoTab.DATASET) {
      // Save the maximum width of the stack column - this is the minimum width
      // of the
      // stack in each row.
      width = hcStack[index].getPreferredWidth();
    }
    constraints.weightx = 0.0;
    if (index == numRows - 1) {
      btnStack.add(pnlTable, layout, constraints);
    }
  }

  private void addListeners() {
    btnAdd.addActionListener(this);
    btnCopyDown.addActionListener(this);
    btnDelete.addActionListener(this);
  }

  public void addStatusChangeListener(final StatusChangeListener listener) {
    if (listener == null) {
      return;
    }
    boolean newCollection = false;
    if (listeners == null) {
      synchronized (this) {
        if (listeners == null) {
          listeners = new ArrayList<StatusChangeListener>();
          newCollection = true;
        }
      }
    }
    if (!newCollection && listeners.contains(listener)) {
      return;
    }
    listeners.add(listener);
  }

  boolean validate(final BatchRunTomoDatasetDialog globalDatasetDialog) {
    return rowList.validate(globalDatasetDialog);
  }

  void msgStatusChangerStarted(final StatusChanger changer) {
    changer.addStatusChangeListener(this);
    rowList.msgStatusChangerStarted(changer);
  }

  void addStatusChangeListenerToRowList(final StatusChangeListener listener) {
    rowList.addStatusChangeListener(listener);
  }

  void addStatusChangeListenerToRows(final StatusChangeListener listener) {
    rowList.addStatusChangeListenerToRows(listener);
  }

  public SwingComponent getUIComponent() {
    return this;
  }

  public Component getComponent() {
    return pnlRoot;
  }

  /**
   * Currently only have room for one table listener.  This can be changed by changing
   * how table listeners are stored.
   * @param tableListener
   */
  void setTableListener(final TableListener tableListener) {
    rowList.setTableListener(tableListener);
  }

  void msgMontage(final boolean montage) {
    if (montage) {
      if (!montageFrame) {
        montageFrame = true;
        sendStatusChanged(true, FrameStatus.MONTAGE);
      }
      // See if any single frame settings are left
      setSingleFrame();
    }
    else {
      if (!singleFrame) {
        singleFrame = true;
        sendStatusChanged(true, FrameStatus.SINGLE);
      }
      setMontageFrame();
    }
  }

  private void sendStatusChanged(final boolean bool, final Status status) {
    StatusChangeBooleanEvent event = new StatusChangeBooleanEvent(bool, status);
    if (listeners != null) {
      int size = listeners.size();
      for (int i = 0; i < size; i++) {
        listeners.get(i).statusChanged(event);
      }
    }
  }

  private void setSingleFrame() {
    boolean origSingleFrame = singleFrame;
    if (rowList.isEmpty()) {
      // Default to enabled
      singleFrame = true;
    }
    else {
      singleFrame = rowList.isSingleFrame();
    }
    if (origSingleFrame != singleFrame) {
      sendStatusChanged(singleFrame, FrameStatus.SINGLE);
    }
  }

  private void setMontageFrame() {
    boolean origMontageFrame = montageFrame;
    if (rowList.isEmpty()) {
      // Default to enabled
      montageFrame = true;
    }
    else {
      montageFrame = rowList.isMontageFrame();
    }
    if (origMontageFrame != montageFrame) {
      sendStatusChanged(montageFrame, FrameStatus.MONTAGE);
    }
  }

  void setFrame(final boolean force) {
    rowList.setFrame(force);
  }

  /**
   * @deprecated 3/29/18
   * @param metaData
   */
  void setParameters(final BatchRunTomoMetaData metaData) {
    setParameters(metaData, null, false);
  }

  /**
   * @param metaData
   * @param onlyStackIDDatasetDialog only loading the dataset dialog attached to the row with this stackID
   * @param onlyAdvancedDatasetDialog only loading an advanced dataset dialog
   */
  void setParameters(final BatchRunTomoMetaData metaData,
    final String onlyStackIDDatasetDialog, boolean onlyAdvancedDatasetDialog) {
    // onlyAdvancedDatasetDialog refers to the global dataset dialog if the stackID is
    // null.
    if (onlyStackIDDatasetDialog == null) {
      onlyAdvancedDatasetDialog = false;
    }
    rowList.setParameters(metaData, onlyStackIDDatasetDialog);
    if (onlyStackIDDatasetDialog == null) {
      statusChanged(metaData.getStatus());
    }
  }

  /**
   * @param metaData
   */
  void getParameters(final BatchRunTomoMetaData metaData) {
    rowList.getParameters(metaData);
    metaData.setEarliestRunEndingStep(getEarliestRunEndingStep());
  }

  /**
   * @param param
   */
  boolean getParameters(final BatchruntomoParam param, final boolean deliverOff,
    final boolean deliverToDirectory, final StringBuilder errMsg,
    final boolean doValidation) {
    List<UniqueKey> managerKeyList = null;
    if (doValidation) {
      managerKeyList = new ArrayList<UniqueKey>();
    }
    boolean retval = rowList.getParameters(param, deliverOff, deliverToDirectory, errMsg,
      doValidation, managerKeyList);
    if (retval && managerKeyList != null && !managerKeyList.isEmpty()) {
      UserConfiguration userConfig = EtomoDirector.INSTANCE.getUserConfiguration();
      if (!userConfig.isBatchCloseDatasets()) {
        Popup popup = Popup.getYesNoInstance(this, "Close Datasets?",
          "Datasets must be closed before batchruntomo is run.  Close datasets?",
          "Always close");
        UIHarness.INSTANCE.openPopup(popup);
        if (!popup.isYes()) {
          // Cannot run batchruntomo unless datasets are closed, so Checkbox in popup must
          // be ignored when No is selected.
          return false;
        }
        if (popup.isCheckboxSelected()) {
          userConfig.setBatchCloseDatasets(true);
        }
      }
      EtomoDirector.INSTANCE.closeManagers(managerKeyList);
    }
    return retval;
  }

  /**
   * @deprecated 3/29/18
   * @param templatePanel
   * @param directiveFiles
   * @param templates
   * @param doValidation
   * @param deliverToDirectory
   * @return
   */
  boolean saveAutodocs(final TemplatePanel templatePanel,
    final NameValuePairList directiveFiles, final NameValuePairList templates,
    final boolean doValidation, final File deliverToDirectory) {
    return saveAutodocs(templatePanel, templates, doValidation, deliverToDirectory, null);
  }

  /**
   * @param templatePanel
   * @param templates
   * @param doValidation
   * @param deliverToDirectory
   * @param autodocStackID don't save all dataset autodocs - just this one
   * @return
   */
  boolean saveAutodocs(final TemplatePanel templatePanel,
    final NameValuePairList templates, final boolean doValidation,
    final File deliverToDirectory, final String autodocStackID) {
    return rowList.saveAutodocs(templatePanel, templates, doValidation,
      deliverToDirectory, autodocStackID);
  }

  /**
   * @deprecated 3/29/18
   */
  void loadAutodocs() {
    loadAutodocs(null, false);
  }

  /**
   * @param onlyStackIDDatasetDialog only loading the dataset dialog attached to the row with this stackID
   * @param onlyAdvancedDatasetDialog only loading an advanced dataset dialog
   */
  void loadAutodocs(final String onlyStackIDDatasetDialog,
    boolean onlyAdvancedDatasetDialog) {
    // onlyAdvancedDatasetDialog refers to the global dataset dialog if the stackID is
    // null.
    if (onlyStackIDDatasetDialog == null) {
      onlyAdvancedDatasetDialog = false;
    }
    rowList.loadAutodocs(onlyStackIDDatasetDialog, onlyAdvancedDatasetDialog);
  }

  void highlightDownActionPerformed() {
    // If the highlight is not visible, don't change the viewport
    boolean adjustViewport = viewport.inViewport(rowList.getHighlightedIndex());
    int index = rowList.highlightDown();
    if (index != -1 && adjustViewport && !viewport.inViewport(index)) {
      // The highlight is visible - keep it in the viewport
      if (index == 0) {
        viewport.homeButtonAction();
      }
      else {
        viewport.downButtonAction();
      }
    }
  }

  void highlightUpActionPerformed() {
    // If the highlight is not visible, don't change the viewport
    boolean adjustViewport = viewport.inViewport(rowList.getHighlightedIndex());
    int index = rowList.highlightUp();
    if (index != -1 && adjustViewport && !viewport.inViewport(index)) {
      // The highlight is visible - keep it in the viewport
      if (index == rowList.size() - 1) {
        viewport.endButtonAction();
      }
      else {
        viewport.upButtonAction();
      }
    }
  }

  BatchRunTomoRow getFirstRow() {
    return rowList.getFirstRow();
  }

  CurrentArrayList<String> createRunKeys() {
    return rowList.createRunKeys();
  }

  CurrentArrayList<String> updateRunKeys() {
    return rowList.updateRunKeys();
  }

  /**
   * @deprecated 3/29/18
   * @return
   */
  boolean backupIfChanged() {
    return backupIfChanged(null, false);
  }

  /**
   * Check each field to see if it has been changed from its checkpoint.  If it has
   * changed, then back up its current value.
   *
   * @param onlyStackIDDatasetDialog only loading the dataset dialog attached to the row with this stackID
   * @param onlyAdvancedDatasetDialog only loading an advanced dataset dialog
   * 
   * @return true if any field has been changed from its checkpoint
   */
  boolean backupIfChanged(final String onlyStackIDDatasetDialog,
    boolean onlyAdvancedDatasetDialog) {
    // onlyAdvancedDatasetDialog refers to the global dataset dialog if the stackID is
    // null.
    if (onlyStackIDDatasetDialog == null) {
      onlyAdvancedDatasetDialog = false;
    }
    return rowList.backupIfChanged(onlyStackIDDatasetDialog, onlyAdvancedDatasetDialog);
  }

  /**
   * @deprecated 3/29/18
   * @return
   */
  void applyValues(final boolean init, final boolean retainUserValues,
    final DirectiveFileCollection directiveFileCollection) {
    applyValues(init, retainUserValues, directiveFileCollection, null, false);
  }

  /**
   * @param init
   * @param retainUserValues
   * @param directiveFileCollection
   * @param onlyStackIDDatasetDialog only loading the dataset dialog attached to the row with this stackID
   * @param onlyAdvancedDatasetDialog only loading an advanced dataset dialog
   */
  void applyValues(final boolean init, final boolean retainUserValues,
    final DirectiveFileCollection directiveFileCollection,
    final String onlyStackIDDatasetDialog, boolean onlyAdvancedDatasetDialog) {
    // onlyAdvancedDatasetDialog refers to the global dataset dialog if the stackID is
    // null.
    if (onlyStackIDDatasetDialog == null) {
      onlyAdvancedDatasetDialog = false;
    }
    rowList.applyValues(init, retainUserValues, directiveFileCollection,
      onlyStackIDDatasetDialog, onlyAdvancedDatasetDialog);
  }

  private void updateDisplay() {
    int size = rowList.size();
    boolean enable = size > 0 && rowList.isHighlighted();
    btnDelete.setEnabled(enable);
    int index = rowList.getHighlightedIndex();
    btnCopyDown.setEnabled(index != -1 && index < size - 1);
  }

  public void statusChanged(final Status status) {
    if (status == null || !(status instanceof BatchRunTomoStatus)) {
      return;
    }
    this.status = (BatchRunTomoStatus) status;
    boolean open = status == BatchRunTomoStatus.OPEN;
    btnAdd.setEditable(open);
    btnCopyDown.setEditable(open);
    btnDelete.setEditable(open);
  }

  public void statusChanged(final StatusChangeEvent statusChangeEvent) {
    // Does not respond to dataset-level or row-level status changes
  }

  void msgTabChanged(final BatchRunTomoTab tab) {
    if (tab == BatchRunTomoTab.STACKS || tab == BatchRunTomoTab.DATASET
      || tab == BatchRunTomoTab.RUN) {
      if (tab != curTab) {
        curTab = tab;
        rebuildTable();
      }
    }
  }

  public void msgViewportPaged() {
    rowList.removeAll();
    rowList.display(viewport);
    UIHarness.INSTANCE.pack(manager);
  }

  public void highlight(final boolean highlight) {
    updateDisplay();
  }

  public void actionPerformed(final ActionEvent actionEvent) {
    if (actionEvent == null) {
      return;
    }
    String actionCommand = actionEvent.getActionCommand();
    if (actionCommand == null) {
      return;
    }
    if (actionCommand.equals(btnAdd.getActionCommand())) {
      JFileChooser chooser = new FileChooser(manager, AxisID.ONLY, (String) null, dialog);
      chooser.setDialogTitle("Select a stack for each dataset");
      chooser.setFileFilter(StackFileFilter.getInstance(manager, true));
      chooser.setMultiSelectionEnabled(true);
      chooser.setPreferredSize(UIParameters.getInstance().getFileChooserDimension());
      int returnVal = chooser.showOpenDialog(btnAdd.getComponent());
      File[] stackList = null;
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File[] files = chooser.getSelectedFiles();
        stackList = chooser.getSelectedFiles();
        if (files != null && files.length > 0) {
          dialog.setBrowsingDir(files[files.length - 1]);
        }
      }
      if (stackList != null) {
        // Remove matching B stacks and set dual to true for the A stack
        List<DatasetTool.StackInfo> filteredStackList =
          DatasetTool.removeMatchingBStacks(hcStack[0], stackList);
        rowList.addNew(filteredStackList);
        setFrame(true);
      }
    }
    else if (actionCommand.equals(btnDelete.getActionCommand())) {
      if (rowList.removeHighlighted()) {
        rebuildTable();
        updateDisplay();
        UIHarness.INSTANCE.pack(manager);
      }
    }
    else if (actionCommand.equals(btnCopyDown.getActionCommand())) {
      rowList.copyDown();
    }
  }

  public void expand(final ExpandButton button) {
    if (button == btnStack) {
      rowList.expandStack(btnStack.isExpanded());
    }
    UIHarness.INSTANCE.pack(manager);
  }

  public void expand(final GlobalExpandButton button) {}

  public int size() {
    return rowList.size();
  }

  private void setTooltips() {
    String imodTooltip = "Opens the stacks";
    for (int i = 0; i < NUM_STACKS_HEADER_ROWS; i++) {
      hcStack[i].setToolTipText(SharedStrings.STACK_TOOLTIP);
      hcDual[i].setToolTipText(SharedStrings.DUAL_TOOLTIP);
      hcMontage[i].setToolTipText(SharedStrings.MONTAGE_TOOLTIP);
      hcBoundaryModel[i].setToolTipText(SharedStrings.RAW_BOUNDARY_MODEL);
      hcSurfacesToAnalyze[i].addTooltip(SharedStrings.SURFACES_TO_ANALYZE_2_TOOLTIP);
      if (i < NUM_STACKS_HEADER_ROWS - 1) {
        hcSkip[i].setToolTipText("Views to exclude");
        hc3dmod[i].setToolTipText(imodTooltip);
      }
      else {
        hcSkip[i].setToolTipText(SharedStrings.SKIP_TOOLTIP);
      }
    }
    for (int i = 0; i < NUM_RUN_HEADER_ROWS; i++) {
      hcStep[i].setToolTipText(SharedStrings.STEP_TOOLTIP);
    }
    hcSkipB.setToolTipText(SharedStrings.BSKIP_TOOLTIP);
    hc3dmod[NUM_STACKS_HEADER_ROWS - 1].setToolTipText(SharedStrings.IMOD_A_TOOLTIP);
    hc3dmodB.setToolTipText(SharedStrings.IMOD_B_TOOLTIP);
  }

  static final class DatasetColumn {
    static final DatasetColumn NUMBER = new DatasetColumn(0);
    static final DatasetColumn STACK = new DatasetColumn(1);
    static final DatasetColumn EDIT_DATASET = new DatasetColumn(2);

    static final int TOTAL = 3;
    private final int index;

    private DatasetColumn(final int index) {
      this.index = index;
    }

    int getIndex() {
      return index;
    }
  }

  JPanel getTablePanel() {
    return pnlTable;
  }

  GridBagLayout getGridBagLayout() {
    return layout;
  }

  BatchRunTomoManager getManager() {
    return manager;
  }

  GridBagConstraints getGridBagConstraints() {
    return constraints;
  }

  TableReference getTableReference() {
    return rowList.getTableReference();
  }

  Map<DirectiveDef, String> getTemplateValues() {
    return dialog.getTemplateValues();
  }

  BrowsingDirectory getBrowsingDirectory() {
    return dialog.getBrowsingDirectory();
  }

  private class RowList implements StatusChangeListener, StatusChanger {
    private final ArrayList<BatchRunTomoRow> list = new ArrayList<BatchRunTomoRow>();

    private final TableReference tableReference;
    private final BatchRunTomoTable table;

    private BatchRunTomoRow initialValueRow = null;
    // Currently only one table listener is required
    private TableListener tableListener = null;
    private EventObject eventObject = null;
    CurrentArrayList<String> runKeys = null;
    private ArrayList<StatusChangeListener> listeners = null;
    private ArrayList<StatusChangeListener> rowListeners = null;
    private ArrayList<StatusChanger> rowChangers = null;

    private RowList(final BatchRunTomoTable table, final TableReference tableReference) {
      this.tableReference = tableReference;
      this.table = table;
    }

    private void setTableListener(final TableListener tableListener) {
      this.tableListener = tableListener;
      eventObject = new EventObject(table);
    }

    private void msgStatusChangerStarted(final StatusChanger changer) {
      changer.addStatusChangeListener(this);
      int len = list.size();
      for (int i = 0; i < len; i++) {
        changer.addStatusChangeListener(list.get(i));
      }
      if (rowChangers == null) {
        rowChangers = new ArrayList<StatusChanger>();
      }
      rowChangers.add(changer);
    }

    /**
     * Adds listeners for earliestRunStep
     */
    public void addStatusChangeListener(final StatusChangeListener listener) {
      if (listener == null) {
        return;
      }
      if (listeners == null) {
        listeners = new ArrayList<StatusChangeListener>();
      }
      listeners.add(listener);
    }

    private void addStatusChangeListenerToRows(final StatusChangeListener listener) {
      if (listener == null) {
        return;
      }
      if (rowListeners == null) {
        rowListeners = new ArrayList<StatusChangeListener>();
      }
      rowListeners.add(listener);
      int len = list.size();
      for (int i = 0; i < len; i++) {
        list.get(i).addStatusChangeListener(listener);
      }
    }

    TableReference getTableReference() {
      return tableReference;
    }

    private void addNew(final List<DatasetTool.StackInfo> stackInfoList) {
      if (stackInfoList == null) {
        return;
      }
      int firstIndex = list.size();
      boolean overridePrevRow = false;
      boolean dual = false;
      List<String> notAdded = new ArrayList<String>();
      boolean fileAdded = false;
      int stackInfoLen = stackInfoList.size();
      for (int i = 0; i < stackInfoLen; i++) {
        DatasetTool.StackInfo stackInfo = stackInfoList.get(i);
        File stack = stackInfo.getStack();
        if (stack != null) {
          // See if there is an ID for this stack.
          String absPath = stack.getAbsolutePath();
          // Get the stackID from absPath minus the extension
          String stackID = tableReference.getID(absPath);
          if (stackID != null) {
            // Check for duplicate files
            if (rowExists(stackID)) {
              notAdded.add(absPath);
              continue;
            }
          }
          else {
            try {
              stackID = tableReference.put(absPath);
            }
            catch (DuplicateException e) {
              e.printStackTrace();
              continue;
            }
            catch (NotLoadedException e) {
              e.printStackTrace();
              if (!fileAdded) {
                return;
              }
              else {
                continue;
              }
            }
          }
          // Put settings from the previous row.
          int index = list.size();
          BatchRunTomoRow prevRow = null;
          if (index > 0) {
            prevRow = list.get(index - 1);
          }
          else {
            prevRow = initialValueRow;
          }
          // Decide how to set the "dual" checkbox.
          dual = stackInfo.isMatched();
          overridePrevRow = dual || stackInfo.isSingleAxis();
          AxisType axisType = null;
          if (stackInfo.isSingleAxis()) {
            axisType = AxisType.SINGLE_AXIS;
          }
          else if (stackInfo.isMatched()) {
            axisType = AxisType.DUAL_AXIS;
          }
          // Add the row.
          BatchRunTomoRow row = BatchRunTomoRow.getInstance(table, basicDirectives,
            index + 1, stack, prevRow, axisType, stackID, preferredTableSize);
          // OrigStack is essential for limiting batchruntomo to one delivery. Should
          // only be set when the user chooses the file.
          row.setOrigStack(stack);
          row.addStatusChangeListener(this);
          if (rowChangers != null) {
            int size = rowChangers.size();
            for (int j = 0; j < size; j++) {
              rowChangers.get(j).addStatusChangeListener(row);
            }
          }
          if (rowListeners != null) {
            Iterator<StatusChangeListener> iterator = rowListeners.iterator();
            while (iterator.hasNext()) {
              row.addStatusChangeListener(iterator.next());
            }
          }
          row.expandStack(btnStack.isExpanded());
          list.add(row);
          fileAdded = true;
          row.display(viewport, curTab);
          if (tableListener != null && i == 0 && firstIndex == 0) {
            tableListener.firstRowAdded(eventObject);
          }
        }
      }
      if (fileAdded) {
        viewport.adjustViewport(firstIndex);
        rowList.removeAll();
        rowList.display(viewport);
        UIHarness.INSTANCE.pack(manager);
        updateDisplay();
      }
      // Pop up a warning if there where any duplicate files.
      if (!notAdded.isEmpty()) {
        StringBuilder warning = new StringBuilder();
        warning.append("The stack table already contains file(s) that match ");
        Iterator<String> i = notAdded.iterator();
        if (i.hasNext()) {
          warning.append(i.next());
        }
        while (i.hasNext()) {
          String absPath = i.next();
          warning.append(", ");
          warning.append((!i.hasNext() ? " and " : ""));
          warning.append(absPath);
        }
        warning.append(".");
        UIHarness.INSTANCE.openMessageDialog(manager, warning.toString(),
          "Unable to Add File(s)");
      }
    }

    private void load(BatchRunTomoMetaData metaData) {
      int firstIndex = list.size();
      OrderedHashMap.ReadOnlyArray<BatchRunTomoRowMetaData> array =
        metaData.getOrderedRows();
      boolean fileAdded = false;
      if (array != null) {
        for (int i = 0; i < array.size(); i++) {
          BatchRunTomoRowMetaData rowMetaData = array.get(i);
          if (rowMetaData != null) {
            int index = list.size();
            String stackID = rowMetaData.getStackID();
            BatchRunTomoRow row = BatchRunTomoRow.getInstance(table, basicDirectives,
              index + 1, new File(tableReference.getFilePath(stackID)), initialValueRow,
              null, stackID, preferredTableSize);
            row.addStatusChangeListener(this);
            row.expandStack(btnStack.isExpanded());
            if (rowListeners != null) {
              Iterator<StatusChangeListener> iterator = rowListeners.iterator();
              while (iterator.hasNext()) {
                row.addStatusChangeListener(iterator.next());
              }
            }
            list.add(row);
            fileAdded = true;
            row.display(viewport, curTab);
            if (tableListener != null && i == 0 && firstIndex == 0) {
              tableListener.firstRowAdded(eventObject);
            }
          }
        }
      }
      if (fileAdded) {
        viewport.adjustViewport(firstIndex);
        rowList.removeAll();
        rowList.display(viewport);
        UIHarness.INSTANCE.pack(manager);
        updateDisplay();
      }
    }

    private BatchRunTomoRow getFirstRow() {
      if (!list.isEmpty()) {
        return list.get(0);
      }
      return null;
    }

    private boolean validate(final BatchRunTomoDatasetDialog globalDatasetDialog) {
      int size = list.size();
      for (int i = 0; i < size; i++) {
        if (!list.get(i).validate(globalDatasetDialog)) {
          return false;
        }
      }
      return true;
    }

    /**
     * Get stackID's from rows with Run checked.
     * @return
     */
    private CurrentArrayList<String> createRunKeys() {
      runKeys = new CurrentArrayList<String>();
      int len = list.size();
      for (int i = 0; i < len; i++) {
        BatchRunTomoRow row = list.get(i);
        if (row.isRun()) {
          runKeys.add(row.getStackID());
        }
      }
      return runKeys;
    }

    /**
     * Creates an new runKeys array to reflect the current state of the table, while
     * keeping the completed keys as placeholders.  Used for resuming from a kill or
     * pause.
     * @return
     */
    private CurrentArrayList<String> updateRunKeys() {
      if (runKeys == null) {
        return null;
      }
      CurrentArrayList<String> newRunKeys = new CurrentArrayList<String>();
      // Copy the completed keys from the original runKeys.
      newRunKeys.copyToCurrent(runKeys);
      int runKeyIndex = runKeys.getIndex() + 1;
      int runKeysSize = runKeys.size();
      if (runKeyIndex < runKeysSize) {
        // Incomplete keys available - try to add them
        int size = list.size();
        for (int i = 0; i < size; i++) {
          BatchRunTomoRow row = list.get(i);
          if (row.equalsStackID(runKeys.get(runKeyIndex))) {
            if (row.isRun()) {
              // The row is run-checked so the key can be added.
              newRunKeys.add(runKeys.get(runKeyIndex));
            }
            runKeyIndex++;
            if (runKeyIndex >= runKeysSize) {
              break;
            }
          }
        }
      }
      runKeys = newRunKeys;
      return runKeys;
    }

    private BatchRunTomoRow getRow(final String stackID) {
      for (int i = 0; i < list.size(); i++) {
        BatchRunTomoRow row = list.get(i);
        if (row.equalsStackID(stackID)) {
          return row;
        }
      }
      return null;
    }

    private boolean rowExists(final String stackID) {
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i).equalsStackID(stackID)) {
          return true;
        }
      }
      return false;
    }

    /**
     * @return true if a row was deleted
     */
    private boolean removeHighlighted() {
      int index = getHighlightedIndex();
      if (index != -1) {
        BatchRunTomoRow row = list.get(index);
        if (row == null) {
          return false;
        }
        if (UIHarness.INSTANCE.openYesNoDialog(manager, "Delete the highlighted row?",
          AXIS_ID)) {
          boolean montage = row.isMontage();
          row.remove();
          row.delete();
          list.remove(index);
          viewport.adjustViewport(index);
          setFrame(false);
        }
        for (int i = index; i < list.size(); i++) {
          list.get(i).setNumber(i + 1);
        }
        // Highlight the row after the deleted row, or the previous one if at
        // the end of
        // the
        // table.
        if (index == list.size()) {
          index--;
        }
        highlight(index);
        if (tableListener != null && list.isEmpty()) {
          tableListener.lastRowDeleted(eventObject);
        }
        return true;
      }
      return false;
    }

    private void highlight(final int index) {
      if (index < 0 || index >= list.size()) {
        return;
      }
      list.get(index).selectHighlightButton();
    }

    private int highlightDown() {
      int index = getHighlightedIndex();
      if (index < 0) {
        return -1;
      }
      index++;
      if (index >= size()) {
        index = 0;
      }
      highlight(index);
      return index;
    }

    private int highlightUp() {
      int index = getHighlightedIndex();
      if (index < 0) {
        return -1;
      }
      index--;
      int size = size();
      if (index < 0 || index >= size) {
        index = size - 1;
      }
      highlight(index);
      return index;
    }

    private void expandStack(final boolean expanded) {
      for (int i = 0; i < list.size(); i++) {
        list.get(i).expandStack(expanded);
      }
    }

    private void removeAll() {
      for (int i = 0; i < list.size(); i++) {
        list.get(i).remove();
      }
    }

    private void display(Viewport viewport) {
      for (int i = 0; i < list.size(); i++) {
        list.get(i).display(viewport, curTab);
      }
    }

    private int size() {
      return list.size();
    }

    private boolean isEmpty() {
      return list.isEmpty();
    }

    private boolean isSingleFrame() {
      for (int i = 0; i < list.size(); i++) {
        if (!list.get(i).isMontage()) {
          return true;
        }
      }
      return false;
    }

    private boolean isMontageFrame() {
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i).isMontage()) {
          return true;
        }
      }
      return false;
    }

    boolean hasDual() {
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i).isDual()) {
          return true;
        }
      }
      return false;
    }

    /**
     * Sets singleFrame and montageFrame and sends status change events.
     */
    private void setFrame(final boolean force) {
      boolean origSingleFrame = singleFrame;
      boolean origMontageFrame = montageFrame;
      singleFrame = false;
      montageFrame = false;
      int count = 2;
      int size = list.size();
      for (int i = 0; i < size; i++) {
        if (list.get(i).isMontage()) {
          if (!montageFrame) {
            count--;
            montageFrame = true;
          }
        }
        else if (!singleFrame) {
          count--;
          singleFrame = true;
        }
        if (count <= 0) {
          break;
        }
      }
      if (!singleFrame && !montageFrame) {
        // There are no rows so enable single and montage.
        singleFrame = true;
        montageFrame = true;
      }
      if (force || origSingleFrame != singleFrame) {
        sendStatusChanged(singleFrame, FrameStatus.SINGLE);
      }
      if (force || origMontageFrame != montageFrame) {
        sendStatusChanged(montageFrame, FrameStatus.MONTAGE);
      }
    }

    public void statusChanged(final Status status) {
      if (!(status instanceof BatchRunTomoStatus)) {
        return;
      }
      if (status == BatchRunTomoStatus.KILLED_PAUSED && runKeys != null) {
        // Only incomplete rows can be run via resume. For row found in runKeys,
        // disable
        // rows with checked run checkboxes.
        int currentSize = Math.min(runKeys.getIndex() + 1, runKeys.size());
        int runKeysIndex = 0;
        if (runKeysIndex < currentSize) {
          int size = list.size();
          for (int i = 0; i < size; i++) {
            BatchRunTomoRow row = list.get(i);
            if (row.equalsStackID(runKeys.get(runKeysIndex)) && row.isRun()) {
              runKeysIndex++;
              row.setEnabledRun(false);
              if (runKeysIndex >= currentSize) {
                break;
              }
            }
          }
        }
      }
    }

    public void statusChanged(final StatusChangeEvent event) {}

    private void sendStatusChanged(final boolean bool, final Status status) {
      StatusChangeBooleanEvent event = new StatusChangeBooleanEvent(bool, status);
      if (listeners != null) {
        int size = listeners.size();
        for (int i = 0; i < size; i++) {
          listeners.get(i).statusChanged(event);
        }
      }
    }

    /**
     * Gets the earliest ending step in the row list where the Run checkbox is checked.
     */
    private EndingStep getEarliestRunEndingStep() {
      EndingStep earliest = null;
      int size = list.size();
      for (int i = 0; i < size; i++) {
        BatchRunTomoRow row = list.get(i);
        if (row.isRun()) {
          EndingStep endingStep = row.getEndingStep();
          // Set earliest to the earliest ending step.
          if (endingStep != null && (earliest == null || endingStep.lt(earliest))) {
            earliest = endingStep;
            if (earliest.isFirst()) {
              return earliest;
            }
          }
        }
      }
      return earliest;
    }

    /**
     * Gets the image file name style from the first dataset that has a valid one.
     */
    private ImageFilenameStyle getImageFilenameStyle() {
      int size = list.size();
      for (int i = 0; i < size; i++) {
        ImageFilenameStyle imageFilenameStyle = null;
        TomosetextsOutput output = manager.tomosetexts(list.get(i).getStackPath());
        if (output != null) {
          imageFilenameStyle = output.getImageFilenameStyle();
        }
        if (imageFilenameStyle != null) {
          return imageFilenameStyle;
        }
      }
      return null;
    }

    private boolean isHighlighted() {
      int size = list.size();
      for (int i = 0; i < size; i++) {
        if (list.get(i).isHighlighted()) {
          return true;
        }
      }
      return false;
    }

    private int getHighlightedIndex() {
      for (int i = 0; i < list.size(); i++) {
        BatchRunTomoRow row = list.get(i);
        if (row.isHighlighted()) {
          return i;
        }
      }
      return -1;
    }

    private void copyDown() {
      int index = getHighlightedIndex();
      if (index != -1 && index < list.size() - 1) {
        list.get(index + 1).copy(list.get(index));
      }
    }

    /**
     * Check each field to see if it has been changed from its checkpoint.  If it has
     * changed, then back up its current value.
     *
     * @param onlyStackIDDatasetDialog only loading the dataset dialog attached to the row with this stackID
     * @param onlyAdvancedDatasetDialog only loading an advanced dataset dialog
     * 
     * @return true if any field has been changed from its checkpoint
     */
    private boolean backupIfChanged(final String onlyStackIDDatasetDialog,
      final boolean onlyAdvancedDatasetDialog) {
      boolean onlyDatasetDialog = onlyStackIDDatasetDialog != null;
      if (!onlyDatasetDialog) {
        boolean changed = false;
        for (int i = 0; i < list.size(); i++) {
          if (list.get(i).backupIfChanged(onlyDatasetDialog, onlyAdvancedDatasetDialog)) {
            changed = true;
          }
        }
        return changed;
      }
      else {
        BatchRunTomoRow row = getRow(onlyStackIDDatasetDialog);
        if (row != null) {
          return row.backupIfChanged(onlyDatasetDialog, onlyAdvancedDatasetDialog);
        }
      }
      return true;
    }

    /**
     * 
     * @param init
     * @param retainUserValues
     * @param directiveFileCollection
     * @param onlyStackIDDatasetDialog only loading the dataset dialog attached to the row with this stackID
     * @param onlyAdvancedDatasetDialog only loading an advanced dataset dialog
     */
    private void applyValues(final boolean init, final boolean retainUserValues,
      final DirectiveFileCollection directiveFileCollection,
      final String onlyStackIDDatasetDialog, final boolean onlyAdvancedDatasetDialog) {
      UserConfiguration userConfiguration = EtomoDirector.INSTANCE.getUserConfiguration();
      boolean onlyDatasetDialog = onlyStackIDDatasetDialog != null;
      if (!onlyDatasetDialog) {
        if (initialValueRow == null) {
          initialValueRow = BatchRunTomoRow.getDefaultsInstance(basicDirectives);
        }
        initialValueRow.setValues(userConfiguration);
        initialValueRow.setValues(directiveFileCollection);
        for (int i = 0; i < list.size(); i++) {
          list.get(i).applyValues(init, retainUserValues, userConfiguration,
            directiveFileCollection, onlyDatasetDialog, onlyAdvancedDatasetDialog);
        }
        setFrame(false);
      }
      else {
        BatchRunTomoRow row = getRow(onlyStackIDDatasetDialog);
        if (row != null) {
          row.applyValues(init, retainUserValues, userConfiguration,
            directiveFileCollection, onlyDatasetDialog, onlyAdvancedDatasetDialog);
        }
      }
    }

    /**
     * 
     * @param metaData
     * @param onlyStackIDDatasetDialog only loading the dataset dialog attached to the row with this stackID
     */
    private void setParameters(final BatchRunTomoMetaData metaData,
      final String onlyStackIDDatasetDialog) {
      boolean onlyDatasetDialog = onlyStackIDDatasetDialog != null;
      if (!onlyDatasetDialog) {
        load(metaData);
        for (int i = 0; i < list.size(); i++) {
          list.get(i).setParameters(metaData, onlyDatasetDialog);
        }
        statusChanged(metaData.getStatus());
      }
      else {
        BatchRunTomoRow row = getRow(onlyStackIDDatasetDialog);
        if (row != null) {
          row.setParameters(metaData, onlyDatasetDialog);
        }
      }
    }

    /**
     * @param metaData
     */
    private void getParameters(final BatchRunTomoMetaData metaData) {
      for (int i = 0; i < list.size(); i++) {
        list.get(i).getParameters(metaData);
      }
    }

    /**
     * @param param
     * @param deliverOff
     * @param deliverToDirectory
     * @param errMsg
     * @param doValidation
     * @param managerKeyList
     * @return
     */
    private boolean getParameters(final BatchruntomoParam param, final boolean deliverOff,
      final boolean deliverToDirectory, final StringBuilder errMsg,
      final boolean doValidation, final List<UniqueKey> managerKeyList) {
      boolean run = false;
      for (int i = 0; i < list.size(); i++) {
        run = list.get(i).getParameters(param, deliverOff, deliverToDirectory, errMsg,
          doValidation, managerKeyList) || run;
      }
      if (doValidation && !run) {
        UIHarness.INSTANCE.openMessageDialog(manager, table,
          "Must check at least one " + RUN_LABEL + " checkbox", "Nothing to Do", AXIS_ID);
        return false;
      }
      return true;
    }

    /**
     * @param templatePanel
     * @param templates
     * @param doValidation
     * @param deliverToDirectory
     * @param autodocStackID don't save all dataset autodocs - just this one
     * @return
     */
    private boolean saveAutodocs(final TemplatePanel templatePanel,
      final NameValuePairList templates, final boolean doValidation,
      final File deliverToDirectory, final String autodocStackID) {
      if (autodocStackID == null) {
        for (int i = 0; i < list.size(); i++) {
          if (!list.get(i).saveAutodoc(templatePanel, templates, doValidation,
            deliverToDirectory, table)) {
            return false;
          }
        }
      }
      else {
        BatchRunTomoRow row = getRow(autodocStackID);
        if (row != null && !row.saveAutodoc(templatePanel, templates, doValidation,
          deliverToDirectory, table)) {
          return false;
        }
      }
      return true;
    }

    /**
     * @param onlyStackIDDatasetDialog only loading the dataset dialog attached to the row with this stackID
     * @param onlyAdvancedDatasetDialog only loading an advanced dataset dialog
     */
    private void loadAutodocs(final String onlyStackIDDatasetDialog,
      final boolean onlyAdvancedDatasetDialog) {
      boolean onlyDatasetDialog = onlyStackIDDatasetDialog != null;
      if (!onlyDatasetDialog) {
        for (int i = 0; i < list.size(); i++) {
          list.get(i).loadAutodoc(onlyDatasetDialog, onlyAdvancedDatasetDialog);
        }
      }
      else {
        BatchRunTomoRow row = getRow(onlyStackIDDatasetDialog);
        if (row != null) {
          row.loadAutodoc(onlyDatasetDialog, onlyAdvancedDatasetDialog);
        }
      }
    }
  }
}
