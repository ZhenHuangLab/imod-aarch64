package etomo.ui.swing;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.comscript.BatchruntomoParam;
import etomo.comscript.IntermittentCommand;
import etomo.comscript.ProcesschunksParam;
import etomo.logic.ProcessorTableState;
import etomo.logic.ProcessorType;
import etomo.process.LoadAverageMonitor;
import etomo.process.LoadMonitor;
import etomo.process.QueuechunkLoadMonitor;
import etomo.storage.CpuAdoc;
import etomo.storage.Node;
import etomo.storage.ParameterStore;
import etomo.storage.Storable;
import etomo.type.AxisID;
import etomo.type.ConstEtomoNumber;
import etomo.type.ConstEtomoVersion;
import etomo.type.InterfaceType;
import etomo.type.ProcessingMethod;
import etomo.ui.Expander;
import etomo.ui.ProcessorTableField;

/**
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright 2005 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
abstract class ProcessorTable
  implements Storable, ParallelProgressDisplay, LoadDisplay, Viewable {
  private static final String RUNNABLE_KEY = "ProcessorTable";
  private static final String RESOURCE_KEY = "ProcessorResourceTable";

  private final JPanel rootPanel = new JPanel();
  private final HeaderCell header1Computer = new HeaderCell(getHeader1ComputerText());
  private final HeaderCell header2Computer = new HeaderCell();
  private final HeaderCell header2NumberCPUsUsed = new HeaderCell("Used");
  private final RowList rowList = new RowList();
  private final Viewport viewport = new Viewport(this,
    EtomoDirector.INSTANCE.getUserConfiguration().getParallelTableSize().getInt(),
    "Processor");
  // Temporary storage of the displayed fields in the current row being built.
  private final ArrayList<Cell> tempDisplayedFields = new ArrayList();
  private final JComponent[] focusableParents = new JComponent[] { rootPanel };

  final AxisID axisID;
  final BaseManager manager;
  private final boolean runnable;
  private final ParallelPanel parent;

  private final ProcessorTableState tableState;
  private final LoadMonitor loadMonitor;
  private final HeaderCell header1NumberCPUs;
  private final HeaderCell header2NumberCPUsMax;
  private final HeaderCell header1Load;
  private final HeaderCell header2Load1;
  private final HeaderCell header2Load5;
  private final HeaderCell header1CPUUsage;
  private final HeaderCell header2CPUUsage;
  private final HeaderCell[] header1LoadArray;
  private final HeaderCell[] header2LoadArray;
  private final HeaderCell header1Users;
  private final HeaderCell header2Users;
  private final HeaderCell header1CPUType;
  private final HeaderCell header2CPUType;
  private final HeaderCell header1Speed;
  private final HeaderCell header2Speed;
  private final HeaderCell header1RAM;
  private final HeaderCell header2RAM;
  private final HeaderCell header1OS;
  private final HeaderCell header2OS;
  private final HeaderCell header1Restarts;
  private final HeaderCell header2Restarts;
  private final HeaderCell header1Finished;
  private final HeaderCell header2Finished;
  private final HeaderCell header1Failure;
  private final HeaderCell header2Failure;
  private final HeaderCell header1GPUType;
  private final HeaderCell header2GPUType;
  private final HeaderCell header1GPUNcores;
  private final HeaderCell header2GPUNcores;
  private final HeaderCell header1GPUSpeed;
  private final HeaderCell header2GPUSpeed;
  private final HeaderCell header1GPURAM;
  private final HeaderCell header2GPURAM;

  private JPanel tablePanel = null;
  private GridBagLayout layout = null;
  private GridBagConstraints constraints = null;
  private boolean scrolling = false;
  private boolean expanded = false;
  private boolean stopped = true;
  private boolean secondary = false;

  abstract int getSize();

  abstract Node getNode(int index);

  abstract ProcessorTableRow createProcessorTableRow(ProcessorTable processorTable,
    Node node, int numRowsInTable, ProcessorTableState tableState);

  abstract String getHeader1ComputerText();

  abstract IntermittentCommand getIntermittentCommand(String computer);

  abstract boolean isExcludeNode(Node node);

  abstract boolean isNiceable();

  abstract String getStorePrepend();

  abstract String getLoadPrepend(final ConstEtomoVersion version);

  abstract void initRow(ProcessorTableRow row);

  abstract String getNoCpusSelectedErrorMessage();

  abstract boolean isCpuTable();

  abstract boolean isGpuTable();

  abstract void getParameters(ProcessingMethod method, BatchruntomoParam param);

  abstract ProcessorType getProcessorType();

  ProcessorTable(final BaseManager manager, final ParallelPanel parent,
    final AxisID axisID, final boolean displayQueues, final boolean runnable,
    final Expander moreLess, final InterfaceType interfaceType) {
    this.manager = manager;
    this.parent = parent;
    this.axisID = axisID;
    this.runnable = runnable;
    // init
    viewport.initPaging();
    if (displayQueues) {
      loadMonitor = new QueuechunkLoadMonitor(this, axisID, manager);
    }
    else {
      loadMonitor = new LoadAverageMonitor(this, axisID, manager);
    }
    header1NumberCPUs = new HeaderCell(getheader1NumberCPUsTitle());
    tableState = new ProcessorTableState(interfaceType, moreLess, runnable, this,
      getProcessorType());
    if (tableState.isUse(ProcessorTableField.NUM_CPUS_MAX_H2)) {
      header2NumberCPUsMax = new HeaderCell("Max.");
    }
    else {
      header2NumberCPUsMax = null;
    }
    if (tableState.isUse(ProcessorTableField.LOAD_AVERAGE_H1)) {
      header1Load = new HeaderCell("Load Average");
      header2Load1 = new HeaderCell("1 Min.");
      header2Load5 = new HeaderCell("5 Min.");
    }
    else {
      header1Load = null;
      header2Load1 = null;
      header2Load5 = null;
    }
    if (tableState.isUse(ProcessorTableField.CPU_USAGE_H1)) {
      header1CPUUsage = new HeaderCell("CPU Usage");
      header2CPUUsage = new HeaderCell();
    }
    else {
      header1CPUUsage = null;
      header2CPUUsage = null;
    }
    if (tableState.isUse(ProcessorTableField.LOAD_ARRAY_0_H1)) {
      String[] loadUnitsArray = CpuAdoc.INSTANCE.getLoadUnitsArray();
      if (!tableState.isUse(ProcessorTableField.LOAD_ARRAY_X_H1)
        || loadUnitsArray == null) {
        header1LoadArray = new HeaderCell[1];
        header1LoadArray[0] = new HeaderCell("Load");
        header2LoadArray = new HeaderCell[1];
        header2LoadArray[0] = new HeaderCell();
      }
      else {
        header1LoadArray = new HeaderCell[loadUnitsArray.length];
        header2LoadArray = new HeaderCell[loadUnitsArray.length];
        for (int i = 0; i < loadUnitsArray.length; i++) {
          header1LoadArray[i] = new HeaderCell(loadUnitsArray[i]);
          header2LoadArray[i] = new HeaderCell();
        }
      }
    }
    else {
      header1LoadArray = null;
      header2LoadArray = null;
    }
    if (tableState.isUse(ProcessorTableField.USERS_H1)) {
      header1Users = new HeaderCell("Users");
      header2Users = new HeaderCell();
    }
    else {
      header1Users = null;
      header2Users = null;
    }
    if (tableState.isUse(ProcessorTableField.TYPE_H1)) {
      header1CPUType = new HeaderCell("CPU Type");
      header2CPUType = new HeaderCell();
    }
    else {
      header1CPUType = null;
      header2CPUType = null;
    }
    if (tableState.isUse(ProcessorTableField.SPEED_H1)) {
      header1Speed = new HeaderCell("Speed");
      header2Speed = new HeaderCell(CpuAdoc.INSTANCE.getSpeedUnits());
    }
    else {
      header1Speed = null;
      header2Speed = null;
    }
    if (tableState.isUse(ProcessorTableField.MEMORY_H1)) {
      header1RAM = new HeaderCell("RAM");
      header2RAM = new HeaderCell(CpuAdoc.INSTANCE.getMemoryUnits());
    }
    else {
      header1RAM = null;
      header2RAM = null;
    }
    if (tableState.isUse(ProcessorTableField.OS_H1)) {
      header1OS = new HeaderCell("OS");
      header2OS = new HeaderCell();
    }
    else {
      header1OS = null;
      header2OS = null;
    }
    if (tableState.isUse(ProcessorTableField.RESTARTS_H1)) {
      header1Restarts = new HeaderCell("Restarts");
      header2Restarts = new HeaderCell();
      header1Finished = new HeaderCell("Finished");
      header2Finished = new HeaderCell("Chunks");
      header1Failure = new HeaderCell("Failure");
      header2Failure = new HeaderCell("Reason");
    }
    else {
      header1Restarts = null;
      header2Restarts = null;
      header1Finished = null;
      header2Finished = null;
      header1Failure = null;
      header2Failure = null;
    }
    if (tableState.isUse(ProcessorTableField.GPU_TYPE_H1)) {
      header1GPUType = new HeaderCell("Type");
      header2GPUType = new HeaderCell();
    }
    else {
      header1GPUType = null;
      header2GPUType = null;
    }
    if (tableState.isUse(ProcessorTableField.GPU_SPEED_H1)) {
      header1GPUSpeed = new HeaderCell("Speed");
      header2GPUSpeed = new HeaderCell(CpuAdoc.INSTANCE.getGpuSpeedUnits());
    }
    else {
      header1GPUSpeed = null;
      header2GPUSpeed = null;
    }
    if (tableState.isUse(ProcessorTableField.GPU_MEMORY_H1)) {
      header1GPURAM = new HeaderCell("RAM");
      header2GPURAM = new HeaderCell(CpuAdoc.INSTANCE.getGpuMemoryUnits());
    }
    else {
      header1GPURAM = null;
      header2GPURAM = null;
    }
    if (tableState.isUse(ProcessorTableField.GPU_NCORES_H1)) {
      header1GPUNcores = new HeaderCell("Cores");
      header2GPUNcores = new HeaderCell();
    }
    else {
      header1GPUNcores = null;
      header2GPUNcores = null;
    }
  }

  public JComponent[] getFocusableParents() {
    return focusableParents;
  }

  String getheader1NumberCPUsTitle() {
    return "# CPUs";
  }

  void createTable() {
    expanded = true;
    initTable();
    rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.X_AXIS));
    rootPanel.setBorder(LineBorder.createBlackLineBorder());
    build();
  }

  void setExpanded(final boolean expanded) {
    if (this.expanded == expanded) {
      return;
    }
    this.expanded = expanded;
    rowList.setContractedIndex(expanded);
    build();
  }

  void setVisible(final boolean visible) {
    rootPanel.setVisible(visible);
  }

  void build() {
    rootPanel.removeAll();
    buildTable();
    rootPanel.add(tablePanel);
    rootPanel.add(viewport.getPagingPanel());
    // configure
    UIHarness.INSTANCE.repaintWindow(manager);
  }

  private void initTable() {
    // table state
    header1NumberCPUs.setTableState(ProcessorTableField.NUM_CPUS_H1, tableState);
    if (header2NumberCPUsMax != null) {
      header2NumberCPUsMax.setTableState(ProcessorTableField.NUM_CPUS_MAX_H2, tableState);
    }
    if (header1Load != null) {
      header1Load.setTableState(ProcessorTableField.LOAD_AVERAGE_H1, tableState);
      header2Load1.setTableState(ProcessorTableField.LOAD_AVERAGE_H1, tableState);
      header2Load5.setTableState(ProcessorTableField.LOAD_AVERAGE_H1, tableState);
    }
    if (header1CPUUsage != null) {
      header1CPUUsage.setTableState(ProcessorTableField.CPU_USAGE_H1, tableState);
      header2CPUUsage.setTableState(ProcessorTableField.CPU_USAGE_H1, tableState);
    }
    if (header1LoadArray != null) {
      for (int i = 0; i < header1LoadArray.length; i++) {
        if (i > 0) {
          header1LoadArray[i].setTableState(ProcessorTableField.LOAD_ARRAY_X_H1,
            tableState);
          header2LoadArray[i].setTableState(ProcessorTableField.LOAD_ARRAY_X_H1,
            tableState);
        }
        else {
          header1LoadArray[i].setTableState(ProcessorTableField.LOAD_ARRAY_0_H1,
            tableState);
          header2LoadArray[i].setTableState(ProcessorTableField.LOAD_ARRAY_0_H1,
            tableState);
        }
      }
    }
    if (header1Users != null) {
      header1Users.setTableState(ProcessorTableField.USERS_H1, tableState);
      header2Users.setTableState(ProcessorTableField.USERS_H1, tableState);
    }
    if (header1CPUType != null) {
      header1CPUType.setTableState(ProcessorTableField.TYPE_H1, tableState);
      header2CPUType.setTableState(ProcessorTableField.TYPE_H1, tableState);
    }
    if (header1Speed != null) {
      header1Speed.setTableState(ProcessorTableField.SPEED_H1, tableState);
      header2Speed.setTableState(ProcessorTableField.SPEED_H1, tableState);
    }
    if (header1RAM != null) {
      header1RAM.setTableState(ProcessorTableField.MEMORY_H1, tableState);
      header2RAM.setTableState(ProcessorTableField.MEMORY_H1, tableState);
    }
    if (header1OS != null) {
      header1OS.setTableState(ProcessorTableField.OS_H1, tableState);
      header2OS.setTableState(ProcessorTableField.OS_H1, tableState);
    }
    if (header1GPUType != null) {
      header1GPUType.setTableState(ProcessorTableField.GPU_TYPE_H1, tableState);
      header2GPUType.setTableState(ProcessorTableField.GPU_TYPE_H1, tableState);
    }
    if (header1GPUSpeed != null) {
      header1GPUSpeed.setTableState(ProcessorTableField.GPU_SPEED_H1, tableState);
      header2GPUSpeed.setTableState(ProcessorTableField.GPU_SPEED_H1, tableState);
    }
    if (header1GPURAM != null) {
      header1GPURAM.setTableState(ProcessorTableField.GPU_MEMORY_H1, tableState);
      header2GPURAM.setTableState(ProcessorTableField.GPU_MEMORY_H1, tableState);
    }
    if (header1GPUNcores != null) {
      header1GPUNcores.setTableState(ProcessorTableField.GPU_NCORES_H1, tableState);
      header2GPUNcores.setTableState(ProcessorTableField.GPU_NCORES_H1, tableState);
    }
    // loop through the nodes
    // loop on nodes
    int size = getSize();
    for (int i = 0; i < size; i++) {
      // get the node
      Node node = getNode(i);
      // exclude any node with the "exclude-interface" attribute set to the
      // current interface
      if (node != null && !node.isExcludedInterface(manager.getInterfaceType())
        && (!node.isExcludedUser(System.getProperty("user.name")))
        && !isExcludeNode(node)) {
        // create the row
        ProcessorTableRow row = createProcessorTableRow(this, node, size, tableState);
        initRow(row);
        // add the row to the rows HashedArray
        rowList.add(row);
      }
    }
    // try {
    ParameterStore parameterStore = EtomoDirector.INSTANCE.getParameterStore();
    parameterStore.load(this);
    setToolTipText();
    if (rowList.size() == 1) {
      rowList.setSelected(0, true);
      rowList.enableSelectionField(0, false);
    }
  }

  public void msgViewportPaged() {
    build();
    UIHarness.INSTANCE.pack(axisID, manager);
  }

  void setSecondary(final boolean input) {
    if (input != secondary) {
      secondary = input;
      build();
      rowList.setSelectedError();
      UIHarness.INSTANCE.pack(axisID, manager);
    }
  }

  private void buildTable() {
    tablePanel = new JPanel();
    layout = new GridBagLayout();
    constraints = new GridBagConstraints();
    // build table
    tablePanel.setLayout(layout);
    constraints.fill = GridBagConstraints.BOTH;
    // header row 1
    constraints.anchor = GridBagConstraints.CENTER;
    // constraints.weightx = 1.0;
    constraints.weightx = 0.0;
    // constraints.weighty = 1.0;
    constraints.weighty = 0.0;
    constraints.gridheight = 1;
    constraints.gridwidth = 1;
    // Header 1
    // Set display columns
    tempDisplayedFields.clear();
    tempDisplayedFields.add(header1Computer);
    tempDisplayedFields.add(header1NumberCPUs);
    if (header1Load != null && header1Load.isDisplay()) {
      tempDisplayedFields.add(header1Load);
    }
    if (header1CPUUsage != null && header1CPUUsage.isDisplay()) {
      tempDisplayedFields.add(header1CPUUsage);
    }
    if (header1LoadArray != null) {
      for (int i = 0; i < header1LoadArray.length; i++) {
        if (header1LoadArray[i].isDisplay()) {
          tempDisplayedFields.add(header1LoadArray[i]);
        }
      }
    }
    if (header1Users != null && header1Users.isDisplay()) {
      tempDisplayedFields.add(header1Users);
    }
    if (header1CPUType != null && header1CPUType.isDisplay()) {
      tempDisplayedFields.add(header1CPUType);
    }
    if (header1Speed != null && header1Speed.isDisplay()) {
      tempDisplayedFields.add(header1Speed);
    }
    if (header1RAM != null && header1RAM.isDisplay()) {
      tempDisplayedFields.add(header1RAM);
    }
    if (header1OS != null && header1OS.isDisplay()) {
      tempDisplayedFields.add(header1OS);
    }
    if (header1GPUType != null && header1GPUType.isDisplay()) {
      tempDisplayedFields.add(header1GPUType);
    }
    if (header1GPUSpeed != null && header1GPUSpeed.isDisplay()) {
      tempDisplayedFields.add(header1GPUSpeed);
    }
    if (header1GPURAM != null && header1GPURAM.isDisplay()) {
      tempDisplayedFields.add(header1GPURAM);
    }
    if (header1GPUNcores != null && header1GPUNcores.isDisplay()) {
      tempDisplayedFields.add(header1GPUNcores);
    }
    if (header1Restarts != null) {
      tempDisplayedFields.add(header1Restarts);
      tempDisplayedFields.add(header1Finished);
      tempDisplayedFields.add(header1Failure);
    }
    // Add fields to the table
    int size = tempDisplayedFields.size();
    for (int i = 0; i < size; i++) {
      Cell cell = tempDisplayedFields.get(i);
      if (i == size - 1) {
        constraints.gridwidth = GridBagConstraints.REMAINDER;
      }
      else {
        constraints.gridwidth = cell.getGridwidth();
      }
      cell.add(tablePanel, layout, constraints);
    }
    // Header 2
    // Set display columns
    tempDisplayedFields.clear();
    tempDisplayedFields.add(header2Computer);
    tempDisplayedFields.add(header2NumberCPUsUsed);
    if (header2NumberCPUsMax != null && header2NumberCPUsMax.isDisplay()) {
      tempDisplayedFields.add(header2NumberCPUsMax);
    }
    if (header2Load1 != null) {
      if (header2Load1.isDisplay()) {
        tempDisplayedFields.add(header2Load1);
      }
      if (header2Load5.isDisplay()) {
        tempDisplayedFields.add(header2Load5);
      }
    }
    if (header2CPUUsage != null && header2CPUUsage.isDisplay()) {
      tempDisplayedFields.add(header2CPUUsage);
    }
    if (header2LoadArray != null) {
      for (int i = 0; i < header2LoadArray.length; i++) {
        if (header2LoadArray[i].isDisplay()) {
          tempDisplayedFields.add(header2LoadArray[i]);
        }
      }
    }
    if (header2Users != null && header2Users.isDisplay()) {
      tempDisplayedFields.add(header2Users);
    }
    if (header2CPUType != null && header2CPUType.isDisplay()) {
      tempDisplayedFields.add(header2CPUType);
    }
    if (header2Speed != null && header2Speed.isDisplay()) {
      tempDisplayedFields.add(header2Speed);
    }
    if (header2RAM != null && header2RAM.isDisplay()) {
      tempDisplayedFields.add(header2RAM);
    }
    if (header2OS != null && header2OS.isDisplay()) {
      tempDisplayedFields.add(header2OS);
    }
    if (header2GPUType != null && header2GPUType.isDisplay()) {
      tempDisplayedFields.add(header2GPUType);
    }
    if (header2GPUSpeed != null && header2GPUSpeed.isDisplay()) {
      tempDisplayedFields.add(header2GPUSpeed);
    }
    if (header2GPURAM != null && header2GPURAM.isDisplay()) {
      tempDisplayedFields.add(header2GPURAM);
    }
    if (header2GPUNcores != null && header2GPUNcores.isDisplay()) {
      tempDisplayedFields.add(header2GPUNcores);
    }
    if (header2Restarts != null) {
      tempDisplayedFields.add(header2Restarts);
      tempDisplayedFields.add(header2Finished);
      tempDisplayedFields.add(header2Failure);
    }
    // Add fields to the table
    constraints.gridwidth = 1;
    size = tempDisplayedFields.size();
    for (int i = 0; i < size; i++) {
      Cell cell = tempDisplayedFields.get(i);
      if (i == size - 1) {
        constraints.gridwidth = GridBagConstraints.REMAINDER;
      }
      cell.add(tablePanel, layout, constraints);
    }
    tempDisplayedFields.clear();
    // add rows to the table
    viewport.msgViewableChanged();
    rowList.display(expanded, viewport);
  }

  private void add(final HeaderCell cell, final boolean use, final ColumnName columnName,
    final ColumnName lastColumnName) {
    if (use) {
      if (lastColumnName == columnName) {
        constraints.gridwidth = GridBagConstraints.REMAINDER;
      }
      cell.add(tablePanel, layout, constraints);
    }
  }

  Container getContainer() {
    return rootPanel;
  }

  JPanel getTablePanel() {
    return tablePanel;
  }

  GridBagLayout getTableLayout() {
    return layout;
  }

  GridBagConstraints getTableConstraints() {
    return constraints;
  }

  public void resetResults() {
    rowList.resetResults();
  }

  int getTotalSuccesses() {
    return rowList.getTotalSuccesses();
  }

  void msgCPUsSelectedChanged() {
    if (!secondary) {
      parent.setCPUsSelected(getCPUsSelected());
    }
    else {
      parent.setSecondaryCPUsSelected(getCPUsSelected());
    }
  }

  public void msgEndingProcess() {
    parent.msgEndingProcess();
  }

  public void msgKillingProcess() {
    parent.msgKillingProcess();
  }

  public void msgProcessStarted() {
    parent.msgProcessStarted();
  }

  public void msgPausingProcess() {
    parent.msgPausingProcess();
  }

  int getCPUsSelected() {
    return rowList.getCPUsSelected();
  }

  void restartLoadMonitor() {
    loadMonitor.restart();
  }

  public boolean isSecondary() {
    return secondary;
  }

  boolean isRunnable() {
    return runnable;
  }

  int getFirstSelectedIndex() {
    return rowList.getFirstSelectedIndex();
  }

  int getNextSelectedIndex(final int lastIndex) {
    return rowList.getNextSelectedIndex(lastIndex);
  }

  void getParameters(final ProcesschunksParam param) {
    rowList.getParameters(param);
  }

  void getParameters(final BatchruntomoParam param) {
    rowList.getParameters(param);
  }

  void setParameters(final BatchruntomoParam param) {
    setComputerMap(getMachineMap(param));
  }

  Map<String, String> getMachineMap(final BatchruntomoParam param) {
    return param.getCPUMachineMap();
  }

  String getFirstSelectedComputer() {
    return rowList.getComputer(rowList.getFirstSelectedIndex());
  }

  public int size() {
    return rowList.size(expanded);
  }

  private ProcessorTableRow getRow(final String computer) {
    return rowList.get(computer);
  }

  public void addRestart(final String computer) {
    ProcessorTableRow row = getRow(computer);
    if (row == null) {
      return;
    }
    row.addRestart();
  }

  public void addSuccess(final String computer) {
    ProcessorTableRow row = getRow(computer);
    if (row == null) {
      return;
    }
    row.addSuccess();
  }

  public void setComputerMap(final Map<String, String> computerMap) {
    rowList.setComputerMap(computerMap);
    parent.msgComputerMapSet();
  }

  public void msgDropped(final String computer, final String reason) {
    ProcessorTableRow row = getRow(computer);
    if (row == null) {
      return;
    }
    row.msgDropped(reason);
  }

  String getHelpMessage() {
    return "Click on check boxes in the " + header1Computer.getText()
      + " column and use the spinner in the " + header1NumberCPUs.getText() + " "
      + header2NumberCPUsUsed.getText() + " column where available.";
  }

  public void startLoad() {
    if (secondary) {
      // The secondary table should not also run the load
      return;
    }
    stopped = false;
    for (int i = 0; i < rowList.size(); i++) {
      manager.startLoad(getIntermittentCommand(i), loadMonitor);
    }
  }

  private IntermittentCommand getIntermittentCommand(final int index) {
    ProcessorTableRow row = (ProcessorTableRow) rowList.get(index);
    String computer = row.getComputer();
    return getIntermittentCommand(computer);
  }

  public void endLoad() {
    stopped = true;
    for (int i = 0; i < rowList.size(); i++) {
      manager.endLoad(getIntermittentCommand(i), loadMonitor);
    }
  }

  public void stopLoad() {
    stopped = true;
    for (int i = 0; i < rowList.size(); i++) {
      manager.stopLoad(getIntermittentCommand(i), loadMonitor);
    }
  }

  public boolean isStopped() {
    return stopped;
  }

  public void setLoad(final String computer, final double load1, final double load5,
    final int users, final String usersTooltip) {
    ((ProcessorTableRow) rowList.get(computer)).setLoad(load1, load5, users,
      usersTooltip);
  }

  public void setLoad(final String computer, final String[] loadArray) {
    ((ProcessorTableRow) rowList.get(computer)).setLoad(loadArray);
  }

  public void setCPUUsage(final String computer, final double cpuUsage,
    final ConstEtomoNumber numberOfProcessors) {
    ((ProcessorTableRow) rowList.get(computer)).setCPUUsage(cpuUsage, numberOfProcessors);
  }

  /**
   * Clears the load from the display.  Does not ask the monitor to
   * drop the computer because processchunks handles this very well, and it is
   * possible that the computer may still be available.
   */
  public void msgLoadFailed(final String computer, final String reason,
    final String tooltip) {
    ((ProcessorTableRow) rowList.get(computer)).clearLoad(reason, tooltip);
  }

  public void msgStartingProcessOnSelectedComputers() {
    clearFailureReason(true);
  }

  /**
   * Clear failure reason, if failure reason equals failureReason1 or 2.  This means
   * that intermittent processes only clear their own messages.  This is useful
   * for restarting an intermittent process without losing the processchunks
   * failure reason.
   */
  public void msgStartingProcess(final String computer, final String failureReason1,
    String failureReason2) {
    ((ProcessorTableRow) rowList.get(computer)).clearFailureReason(failureReason1,
      failureReason2);
  }

  void clearFailureReason(final boolean selectedComputers) {
    rowList.clearFailureReason(selectedComputers);
  }

  String getGroupKey() {
    if (runnable) {
      return RUNNABLE_KEY;
    }
    return RESOURCE_KEY;
  }

  public void store(final Properties props) {
    store(props, "");
  }

  public void store(final Properties props, String prepend) {
    String group;
    if (prepend == "") {
      prepend = getStorePrepend();
    }
    else {
      prepend += "." + getStorePrepend();
    }
    rowList.store(props, prepend);
  }

  /**
   * Get the objects attributes from the properties object.
   */
  public final void load(final Properties props) {
    load(props, "");
  }

  public void load(final Properties props, String prepend) {
    String group;
    if (prepend == "") {
      prepend = getLoadPrepend(parent.getVersion());
    }
    else {
      prepend += "." + getLoadPrepend(parent.getVersion());
    }
    rowList.load(props, prepend);
  }

  final boolean isScrolling() {
    return scrolling;
  }

  final private void setToolTipText() {
    String text;
    text = "Select computers to use for parallel processing.";
    header1Computer.setToolTipText(text);
    header2Computer.setToolTipText(text);
    text = "Select the number of CPUs to use for each computer.";
    header1NumberCPUs.setToolTipText(text);
    header2NumberCPUsUsed.setToolTipText(text);
    if (header2NumberCPUsMax != null) {
      header2NumberCPUsMax
        .setToolTipText("The maximum number of CPUs available on each computer.");
    }
    if (header1Load != null) {
      header1Load.setToolTipText("Represents how busy each computer is.");
      header2Load1.setToolTipText("The load averaged over one minute.");
      header2Load5.setToolTipText("The load averaged over five minutes.");
    }
    if (header1CPUUsage != null) {
      header1CPUUsage
        .setToolTipText("The CPU usage (0 to number of CPUs) averaged over one second.");
    }
    text = "The number of users logged into the computer.";
    if (header1Users != null) {
      header1Users.setToolTipText(text);
    }
    if (header2Users != null) {
      header2Users.setToolTipText(text);
    }
    text = "The number of times processes failed on each computer.";
    if (header1Restarts != null) {
      header1Restarts.setToolTipText(text);
      header2Restarts.setToolTipText(text);
      text = "The number of processes each computer completed for a distributed process.";
      header1Finished.setToolTipText(text);
      header2Finished.setToolTipText(text);
      text = "Reason for a failure by the load average or a process";
      header1Failure.setToolTipText(text);
      header2Failure.setToolTipText(text);
    }
    if (header1CPUType != null) {
      text = "The CPU type of each computer.";
      header1CPUType.setToolTipText(text);
      header2CPUType.setToolTipText(text);
    }
    if (header1Speed != null) {
      text = "The speed of each computer.";
      header1Speed.setToolTipText(text);
      header2Speed.setToolTipText(text);
    }
    if (header1RAM != null) {
      text = "The amount of RAM in each computer.";
      header1RAM.setToolTipText(text);
      header2RAM.setToolTipText(text);
    }
    if (header1OS != null) {
      text = "The operating system of each computer.";
      header1OS.setToolTipText(text);
      header2OS.setToolTipText(text);
    }
    if (header1GPUType != null) {
      text = "The GPU type of each computer.";
      header1GPUType.setToolTipText(text);
      header2GPUType.setToolTipText(text);
    }
    if (header1GPUSpeed != null) {
      text = "The GPU speed of each computer.";
      header1GPUSpeed.setToolTipText(text);
      header2GPUSpeed.setToolTipText(text);
    }
    if (header1GPURAM != null) {
      text = "The amount of GPU RAM in each computer.";
      header1GPURAM.setToolTipText(text);
      header2GPURAM.setToolTipText(text);
    }
    if (header1GPUNcores != null) {
      text = "The number of GPU cores in each computer.";
      header1GPUNcores.setToolTipText(text);
      header2GPUNcores.setToolTipText(text);
    }
  }

  static final class ColumnName {
    static final ColumnName NUMBER_USED = new ColumnName();
    static final ColumnName NUMBER = new ColumnName();
    static final ColumnName LOAD = new ColumnName();
    static final ColumnName TYPE = new ColumnName();
    static final ColumnName SPEED = new ColumnName();
    static final ColumnName MEMORY = new ColumnName();
    static final ColumnName OS = new ColumnName();
    static final ColumnName RUN = new ColumnName();
    static final ColumnName USERS = new ColumnName();

    private ColumnName() {}

    public String toString() {
      if (this == NUMBER_USED) {
        return "NUMBER_USED";
      }
      if (this == NUMBER) {
        return "NUMBER";
      }
      if (this == LOAD) {
        return "LOAD";
      }
      if (this == TYPE) {
        return "TYPE";
      }
      if (this == SPEED) {
        return "SPEED";
      }
      if (this == MEMORY) {
        return "MEMORY";
      }
      if (this == OS) {
        return "OS";
      }
      if (this == RUN) {
        return "RUN";
      }
      if (this == USERS) {
        return "USERS";
      }
      return null;
    }
  }

  private static final class RowList {
    private final List list = new ArrayList();
    // Contracted index for use when the table is not expanded..
    private final List contractedIndex = new ArrayList();

    private RowList() {}

    /**
     * Changes the selected computers and CPUs to match computerMap.
     *
     * @param computerMap
     */
    private void setComputerMap(final Map<String, String> computerMap) {
      if (computerMap == null || computerMap.isEmpty()) {
        return;
      }
      for (int i = 0; i < list.size(); i++) {
        // First unselect a computer. Then select the computer if it is in
        // computerMap.
        ProcessorTableRow row = get(i);
        row.setSelected(false);
        String key = row.getComputer();
        if (computerMap.containsKey(key)) {
          row.setSelected(true);
          row.setCPUsSelected(computerMap.get(key));
        }
      }
    }

    private void setSelectedError() {
      for (int i = 0; i < list.size(); i++) {
        get(i).setSelectedError();
      }
    }

    private void add(final ProcessorTableRow row) {
      list.add(row);
    }

    private void display(final boolean expanded, final Viewport viewport) {
      for (int i = 0; i < size(expanded); i++) {
        ProcessorTableRow row;
        if (expanded) {
          row = get(i);
        }
        else {
          row = (ProcessorTableRow) contractedIndex.get(i);
        }
        row.deleteRow();
        row.display(i, viewport);
      }
    }

    private int size(final boolean expanded) {
      if (expanded) {
        return size();
      }
      return contractedIndex.size();
    }

    private int size() {
      return list.size();
    }

    private ProcessorTableRow get(final int index) {
      if (index == -1) {
        return null;
      }
      return (ProcessorTableRow) list.get(index);
    }

    private ProcessorTableRow get(final String computer) {
      for (int i = 0; i < size(); i++) {
        ProcessorTableRow row = get(i);
        if (row.equals(computer)) {
          return row;
        }
      }
      return null;
    }

    private void setContractedIndex(final boolean expanded) {
      contractedIndex.clear();
      if (!expanded) {
        for (int i = 0; i < size(); i++) {
          ProcessorTableRow row = get(i);
          if (row.isSelected()) {
            contractedIndex.add(row);
          }
        }
      }
    }

    private void getParameters(final ProcesschunksParam param) {
      for (int i = 0; i < size(); i++) {
        get(i).getParameters(param);
      }
    }

    private void getParameters(final BatchruntomoParam param) {
      for (int i = 0; i < size(); i++) {
        get(i).getParameters(param);
      }
    }

    private void setSelected(final int index, final boolean selected) {
      get(index).setSelected(selected);
    }

    private void enableSelectionField(final int index, final boolean enabled) {
      get(index).enableSelectionField(enabled);
    }

    private void resetResults() {
      for (int i = 0; i < size(); i++) {
        get(i).resetResults();
      }
    }

    private int getTotalSuccesses() {
      int successes = 0;
      for (int i = 0; i < size(); i++) {
        successes += get(i).getSuccesses();
      }
      return successes;
    }

    private int getCPUsSelected() {
      int cpusSelected = 0;
      for (int i = 0; i < size(); i++) {
        cpusSelected += get(i).getCPUsSelected();
      }
      return cpusSelected;
    }

    private int getFirstSelectedIndex() {
      for (int i = 0; i < size(); i++) {
        if (get(i).isSelected()) {
          return i;
        }
      }
      return -1;
    }

    private int getNextSelectedIndex(final int lastIndex) {
      for (int i = lastIndex + 1; i < size(); i++) {
        if (get(i).isSelected()) {
          return i;
        }
      }
      return -1;
    }

    private String getComputer(final int index) {
      ProcessorTableRow row = get(index);
      if (row == null) {
        return null;
      }
      return row.getComputer();
    }

    private void clearFailureReason(final boolean selectedComputers) {
      for (int i = 0; i < size(); i++) {
        ProcessorTableRow row = get(i);
        if (!selectedComputers || row.isSelected()) {
          row.clearFailureReason();
        }
      }
    }

    private void store(final Properties props, final String prepend) {
      for (int i = 0; i < size(); i++) {
        get(i).store(props, prepend);
      }
    }

    private void load(final Properties props, final String prepend) {
      for (int i = 0; i < size(); i++) {
        get(i).load(props, prepend);
      }
    }
  }
}
/**
 * <p> $Log$
 * <p> Revision 1.8  2011/07/22 19:55:51  sueh
 * <p> Bug# 1515 In initTable, replaced to select a single row.
 * <p>
 * <p> Revision 1.7  2011/07/18 23:13:31  sueh
 * <p> Bug# 1515 Added getNoCpusSelectedErrorMessage
 * <p>
 * <p> Revision 1.6  2011/07/18 22:45:52  sueh
 * <p> Bug# 1515 In initTable always selecting a single row.  Removed isSelectOnlyRow, which is no longer
 * <p> being called.
 * <p>
 * <p> Revision 1.5  2011/06/25 03:11:41  sueh
 * <p> Bug# 1499 In RowList.get(int) and getComputer(int) handling index == -1.
 * <p>
 * <p> Revision 1.4  2011/05/19 16:33:13  sueh
 * <p> bug# 1473 In ParameterStore.load, removed unused throw.
 * <p>
 * <p> Revision 1.3  2011/02/22 18:20:14  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.2  2011/02/03 06:15:08  sueh
 * <p> bug# 1422 Handling the three types of tables (queue, CPU, and GPU) by
 * <p> putting the differences into child classes.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.58  2010/10/12 02:44:42  sueh
 * <p> bug# 1391 Added setComputerMap.
 * <p>
 * <p> Revision 1.57  2010/02/17 05:03:12  sueh
 * <p> bug# 1301 Using manager instead of manager key for popping up messages.
 * <p>
 * <p> Revision 1.56  2010/01/11 23:59:01  sueh
 * <p> bug# 1299 Removed responsibility anything other then cpu.adoc from
 * <p> CpuAdoc.  Placed responsibility for information about the network in the
 * <p> Network class.
 * <p>
 * <p> Revision 1.55  2009/04/20 20:07:51  sueh
 * <p> bug# 1192 Added setComputerMap and RowList.setComputerMap, which
 * <p> changes the row to match computerMap.
 * <p>
 * <p> Revision 1.54  2009/03/17 00:46:24  sueh
 * <p> bug# 1186 Pass managerKey to everything that pops up a dialog.
 * <p>
 * <p> Revision 1.53  2009/02/04 23:36:48  sueh
 * <p> bug# 1158 Changed id and exception classes in LogFile.
 * <p>
 * <p> Revision 1.52  2008/10/07 16:43:35  sueh
 * <p> bug# 1113 Improved names:  changed Viewport.msgViewportMoved to
 * <p> msgViewportPaged.
 * <p>
 * <p> Revision 1.51  2008/10/06 22:43:25  sueh
 * <p> bug# 1113 Removed pack, which is unecessary since table scrolling was
 * <p> removed.  Moved rows into RowList.  Used a regular array instead of HashedArray, since the hash was never used and the table is not very big.  Implemented Viewable.  Added a Viewport.  Got rid of scrolling and
 * <p> all functions associated with scrolling.
 * <p>
 * <p> Revision 1.50  2008/07/19 01:07:35  sueh
 * <p> bug# 1125 sharing # CPUs header.
 * <p>
 * <p> Revision 1.49  2008/01/31 20:30:37  sueh
 * <p> bug# 1055 throwing a FileException when LogFile.getInstance fails.
 * <p>
 * <p> Revision 1.48  2007/09/27 21:02:47  sueh
 * <p> bug# 1044 Added a displayQueues mode.  Implementing
 * <p> ParallelProgressDisplay and LoadDisplay.
 * <p>
 * <p> Revision 1.47  2007/09/07 00:28:13  sueh
 * <p> bug# 989 Using a public INSTANCE to refer to the EtomoDirector singleton
 * <p> instead of getInstance and createInstance.
 * <p>
 * <p> Revision 1.46  2007/07/30 18:54:16  sueh
 * <p> bug# 1002 ParameterStore.getInstance can return null - handle it.
 * <p>
 * <p> Revision 1.45  2007/07/17 21:44:05  sueh
 * <p> bug# 1018 Adding all cpu.adoc information from CpuAdoc.
 * <p>
 * <p> Revision 1.44  2007/05/25 00:28:19  sueh
 * <p> bug# 964 Added tooltip to clearLoadAverage.  Added a second reason to
 * <p> clearFailureReason.
 * <p>
 * <p> Revision 1.43  2007/05/22 21:21:40  sueh
 * <p> bug# 999 Checking CpuAdoc.users before adding a row.
 * <p>
 * <p> Revision 1.42  2007/05/21 22:31:28  sueh
 * <p> bug# 1000 In initTable(), excluding sections based on exclude-interface.
 * <p>
 * <p> Revision 1.41  2007/05/21 18:11:22  sueh
 * <p> bug# 992 Added usersColumn.  Do not display Users column when
 * <p> usersColumn is false.
 * <p>
 * <p> Revision 1.40  2007/03/21 19:46:38  sueh
 * <p> bug# 964 Limiting access to autodoc classes by using ReadOnly interfaces.
 * <p> Added AutodocFactory to create Autodoc instances.
 * <p>
 * <p> Revision 1.39  2007/03/15 21:48:22  sueh
 * <p> bug# 964 Added ReadOnlyAttribute, which is used as an interface for Attribute,
 * <p> unless the Attribute needs to be modified.
 * <p>
 * <p> Revision 1.38  2007/02/09 00:51:54  sueh
 * <p> bug# 962 Made TooltipFormatter a singleton and moved its use to low-level ui
 * <p> classes.
 * <p>
 * <p> Revision 1.37  2007/02/05 23:40:45  sueh
 * <p> bug# 962 Moved EtomoNumber type info to inner class.
 * <p>
 * <p> Revision 1.36  2006/11/29 00:20:49  sueh
 * <p> bug# 934 Added endGetLoadAverage().  Does the same thing as
 * <p> stopGetLoadAverage(), but also removes the monitor.  Uses when a manager
 * <p> exits.
 * <p>
 * <p> Revision 1.35  2006/11/18 00:49:50  sueh
 * <p> bug# 936 Parallel Processing:  added user list tooltip to user column.
 * <p>
 * <p> Revision 1.34  2006/11/15 21:24:34  sueh
 * <p> bug# 872 Saving to .etomo with ParameterStore.
 * <p>
 * <p> Revision 1.33  2006/11/08 21:08:01  sueh
 * <p> bug# 936:  Remove the 15 Min. column and add the Users column.
 * <p>
 * <p> Revision 1.32  2006/11/07 22:53:36  sueh
 * <p> bug# 954 Added tooltips
 * <p>
 * <p> Revision 1.31  2006/02/09 23:40:36  sueh
 * <p> bug# 796 In Windows an exception was caused by tool tips being set
 * <p>
 * <p> Revision 1.30  2006/02/08 03:38:11  sueh
 * <p> bug# 796 Use cpu usage instead of load average for windows.
 * <p>
 * <p> Revision 1.29  2006/01/12 17:37:12  sueh
 * <p> bug# 798 Moved the autodoc classes to etomo.storage.autodoc.
 * <p>
 * <p> Revision 1.28  2006/01/11 22:32:32  sueh
 * <p> bug# 675 replaced Attribute.getUnformattedValue with getValue
 * <p>
 * <p> Revision 1.27  2005/12/23 02:18:44  sueh
 * <p> bug# 675 Renamed Section.getFirstSectionLocation to getSectionLocation.
 * <p> Removed getSection(sectionLocation).  Changed nextSection so it gets the
 * <p> current section and increments.
 * <p>
 * <p> Revision 1.26  2005/12/16 01:46:01  sueh
 * <p> bug# 784 Added tool tips.
 * <p>
 * <p> Revision 1.25  2005/12/14 20:58:10  sueh
 * <p> bug# 784 Added tool tips.
 * <p>
 * <p> Revision 1.24  2005/11/19 02:45:10  sueh
 * <p> bug# 744 Added clearFailureReason(boolean selectedComputers).
 * <p>
 * <p> Revision 1.23  2005/11/14 22:16:36  sueh
 * <p> bug# 762 The internal class is now accessing protected functions instead
 * <p> of private variables.
 * <p>
 * <p> Revision 1.22  2005/11/10 18:16:57  sueh
 * <p> bug# 733 Made the class public so that its constance SECTION_TYPE
 * <p> could be used.
 * <p>
 * <p> Revision 1.21  2005/11/04 01:03:46  sueh
 * <p> bug# 732 Cleaned up RunPack.run().  Moved the code from
 * <p> setMaximumSize() and setPreferredSize() into RunPack.run().  Made
 * <p> getWidth() look at the second header instead of a row which might not be
 * <p> displayed.  Made getHeight() look at each header and row to get an
 * <p> accurate size.  Removed calcMaximumHeight() because it was
 * <p> unecessary.  Correct the scrollbar width to get the width right and remove
 * <p> the horizontal scrollbar.  Recalculating size and resizing each time
 * <p> RunPack.run() is run, so that fit works on the table.  Made
 * <p> ProcessorTableRow keep track of whether it was displayed to that a fit of
 * <p> a contracted table will show all displayed rows.  Took the maximum
 * <p> number of rows into account in getHeight().  Only setting preferred size
 * <p> when the number of row is greater then MAXIMUM_ROWS.
 * <p>
 * <p> Revision 1.20  2005/10/12 22:46:17  sueh
 * <p> bug# 532 Moved the section type string to a public static final, so it can
 * <p> be used by ParallelPanel.
 * <p>
 * <p> Revision 1.19  2005/09/27 23:44:29  sueh
 * <p> bug# 532 Moved loading prefererences to the end of initTable() from
 * <p> ParallelPanel.
 * <p>
 * <p> Revision 1.18  2005/09/22 21:29:36  sueh
 * <p> bug# 532 Removed restartsError.  Taking error level from ParallelPanel in
 * <p> ProcessorTableRow.
 * <p>
 * <p> Revision 1.17  2005/09/21 17:04:33  sueh
 * <p> bug# 532 getting autodoc from ParallelPanel.getAutodoc().  Fix getWidth()
 * <p> so that it gets the width from a row which is currently displayed (rows that
 * <p> are not selected are not displayed when the table is contracted).
 * <p>
 * <p> Revision 1.16  2005/09/20 19:12:40  sueh
 * <p> bug# 532  Implementing setExpanded(boolean).  Add boolean expanded.
 * <p> When expanded is false, only display fields and rows which the user
 * <p> would look at while running a process.  Divide createTable() into initTable()
 * <p> and buildTable().  Create fields and get audodoc data in initTable().  Then
 * <p> use buildTable() to build the table according to the autodoc data and
 * <p> expanded.  Change getMinimumHeight() to use expanded.  SetExpanded()
 * <p> runs buildTable().  All table fields are member variables.
 * <p>
 * <p> Revision 1.15  2005/09/13 00:01:26  sueh
 * <p> bug# 532 Implemented Storable.
 * <p>
 * <p> Revision 1.14  2005/09/10 01:54:59  sueh
 * <p> bug# 532 Added clearFailureReason() so that the failure reason can be
 * <p> cleared when a new connection to the computer is attempted.
 * <p>
 * <p> Revision 1.13  2005/09/09 21:47:45  sueh
 * <p> bug# 532 Passed reason string to clearLoadAverage().
 * <p>
 * <p> Revision 1.12  2005/09/01 18:03:19  sueh
 * <p> bug# 532 Added clearLoadAverage() to clear the load averages when the
 * <p> load average command fails.  Added a drop reason.  Added a error level
 * <p> for the restarts column.
 * <p>
 * <p> Revision 1.11  2005/08/27 22:38:40  sueh
 * <p> bug# 532 Populating the table from cpu.adoc:  getting the rows of the
 * <p> table, the units of speed and memory, and whether a column needs to be
 * <p> displayed.
 * <p>
 * <p> Revision 1.10  2005/08/24 00:25:25  sueh
 * <p> bug# 532 Added ashtray.  Made tubule a 2 cpu system
 * <p>
 * <p> Revision 1.9  2005/08/22 18:14:27  sueh
 * <p> bug# 532 Removed dummy load averages.  Added a key to each row,
 * <p>
 * <p> Revision 1.8  2005/08/04 20:16:24  sueh
 * <p> bug# 532  Fixed table resizing problems.  Added RunPack() to use with
 * <p> invoke later.  Added functions to calculate height and width of the table.
 * <p>
 * <p> Revision 1.7  2005/08/01 18:13:12  sueh
 * <p> bug# 532 Changed ProcessorTableRow.signalRestart() to addRestart.
 * <p> Added Failure Reason column.
 * <p>
 * <p> Revision 1.6  2005/07/21 22:22:30  sueh
 * <p> bug# 532 Added getHelpMessage so that the parallel panel can complain
 * <p> when no CPUs are selected.
 * <p>
 * <p> Revision 1.5  2005/07/15 16:31:49  sueh
 * <p> bug# 532 Removed experiment about not scrolling headers
 * <p>
 * <p> Revision 1.4  2005/07/14 22:14:40  sueh
 * <p> bug# 532 Experimenting with extending GridBagLayout to make a header
 * <p> in the scroll pane.
 * <p>
 * <p> Revision 1.3  2005/07/11 23:19:33  sueh
 * <p> bug# 619 Added scrolling and sized table.  Added functions:
 * <p> getContainer, getCpusSelected, getFirstSelectedIndex,
 * <p> getNextSelectedIndex, getRestartFactor, getSuccessFactor,
 * <p> getTablePanel, setMaximumSize, sizeSize, signalRestart,
 * <p> signalSuccess.
 * <p>
 * <p> Revision 1.2  2005/07/01 23:05:20  sueh
 * <p> bug# 619 added getTotalSUccesses(), signalCpusSelectedChanged()
 * <p>
 * <p> Revision 1.1  2005/07/01 21:21:47  sueh
 * <p> bug# 619 Table containing a list of computers and CPUs
 * <p> </p>
 */
