package etomo.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import etomo.comscript.BatchruntomoParam;
import etomo.comscript.ProcesschunksParam;
import etomo.logic.ProcessorTableState;
import etomo.storage.Node;
import etomo.storage.Storable;
import etomo.type.ConstEtomoNumber;
import etomo.type.EtomoNumber;
import etomo.ui.ProcessorTableField;
import etomo.ui.swing.ProcessorTable.ColumnName;
import etomo.util.Utilities;

/**
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright 2005 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class ProcessorTableRow implements Storable {
  private static final String STORE_SELECTED = "Selected";
  private static final String STORE_CPUS_SELECTED = "CPUsSelected";
  private static final int DEFAULT_CPUS_SELECTED = 1;

  // Temporary storage for building the row.
  private final ArrayList<Cell> tempDisplayedFields = new ArrayList();

  /**
   * A computer or queue.
   */
  private final ToggleCell cellComputer;
  private final InputCell cellCPUsSelected;
  private final FieldCell cellNumberCpus;
  private final FieldCell cellLoad1;
  private final FieldCell cellLoad5;
  private final FieldCell cellCPUUsage;
  private final FieldCell[] cellLoadArray;
  private final FieldCell cellUsers;
  private final FieldCell cellCPUType;
  private final FieldCell cellSpeed;
  private final FieldCell cellMemory;
  private final FieldCell cellOS;
  private final FieldCell cellRestarts;
  private final FieldCell cellSuccesses;
  private final FieldCell cellFailureReason;
  private final FieldCell cellGpuType;
  private final FieldCell cellGpuSpeed;
  private final FieldCell cellGpuMemory;
  private final FieldCell cellGpuNcores;

  private final boolean displayQueues;
  private final int numRowsInTable;

  private final String[] gpuDeviceArray;

  private ProcessorTable table = null;
  private int numCpus = 1;
  private String memory = null;
  private String os = null;
  private boolean rowInitialized = false;
  private boolean displayed = false;
  private boolean loadWarning = true;

  private String computerName;
  final ProcessorTableState tableState;

  private ProcessorTableRow(final ProcessorTable table, final Node node,
    final int numCpus, final boolean displayQueues, final ButtonGroup queueButtonGroup,
    final int queueLoadArraySize, final int numRowsInTable,
    final ProcessorTableState tableState) {
    this.table = table;
    this.numRowsInTable = numRowsInTable;
    this.tableState = tableState;
    computerName = node.getName();
    this.numCpus = numCpus;
    memory = node.getMemory();
    gpuDeviceArray = node.getGpuDeviceArray();
    os = node.getOs();
    this.displayQueues = displayQueues;
    if (displayQueues) {
      cellComputer = new RadioButtonCell(queueButtonGroup);
    }
    else {
      cellComputer = new CheckBoxCell();
    }
    if (numCpus > 1) {
      cellCPUsSelected = SpinnerCell.getIntInstance(0, numCpus);
      SpinnerCell spinnerCell = (SpinnerCell) cellCPUsSelected;
    }
    else {
      cellCPUsSelected = FieldCell.getIneditableInstance();
    }
    if (tableState.isUse(ProcessorTableField.NUM_CPUS_MAX_H2)) {
      cellNumberCpus = FieldCell.getIneditableInstance();
    }
    else {
      cellNumberCpus = null;
    }
    if (tableState.isUse(ProcessorTableField.LOAD_AVERAGE_H1)) {
      cellLoad1 = FieldCell.getIneditableInstance();
      cellLoad5 = FieldCell.getIneditableInstance();
    }
    else {
      cellLoad1 = null;
      cellLoad5 = null;
    }
    if (tableState.isUse(ProcessorTableField.CPU_USAGE_H1)) {
      cellCPUUsage = FieldCell.getIneditableInstance();
    }
    else {
      cellCPUUsage = null;
    }
    if (tableState.isUse(ProcessorTableField.LOAD_ARRAY_0_H1)) {
      if (!tableState.isUse(ProcessorTableField.LOAD_ARRAY_X_H1)) {
        cellLoadArray = new FieldCell[1];
        cellLoadArray[0] = FieldCell.getIneditableInstance();
      }
      else {
        cellLoadArray = new FieldCell[queueLoadArraySize];
        for (int i = 0; i < queueLoadArraySize; i++) {
          cellLoadArray[i] = FieldCell.getIneditableInstance();
        }
      }
    }
    else {
      cellLoadArray = null;
    }
    if (tableState.isUse(ProcessorTableField.USERS_H1)) {
      cellUsers = FieldCell.getIneditableInstance();
    }
    else {
      cellUsers = null;
    }
    if (tableState.isUse(ProcessorTableField.TYPE_H1)) {
      cellCPUType = FieldCell.getIneditableInstance();
    }
    else {
      cellCPUType = null;
    }
    if (tableState.isUse(ProcessorTableField.SPEED_H1)) {
      cellSpeed = FieldCell.getIneditableInstance();
    }
    else {
      cellSpeed = null;
    }
    if (tableState.isUse(ProcessorTableField.MEMORY_H1)) {
      cellMemory = FieldCell.getIneditableInstance();
    }
    else {
      cellMemory = null;
    }
    if (tableState.isUse(ProcessorTableField.OS_H1)) {
      cellOS = FieldCell.getIneditableInstance();
    }
    else {
      cellOS = null;
    }
    if (tableState.isUse(ProcessorTableField.GPU_TYPE_H1)) {
      cellGpuType = FieldCell.getIneditableInstance();
    }
    else {
      cellGpuType = null;
    }
    if (tableState.isUse(ProcessorTableField.GPU_SPEED_H1)) {
      cellGpuSpeed = FieldCell.getIneditableInstance();
    }
    else {
      cellGpuSpeed = null;
    }
    if (tableState.isUse(ProcessorTableField.GPU_MEMORY_H1)) {
      cellGpuMemory = FieldCell.getIneditableInstance();
    }
    else {
      cellGpuMemory = null;
    }
    if (tableState.isUse(ProcessorTableField.GPU_NCORES_H1)) {
      cellGpuNcores = FieldCell.getIneditableInstance();
    }
    else {
      cellGpuNcores = null;
    }
    if (tableState.isUse(ProcessorTableField.RESTARTS_H1)) {
      cellRestarts = FieldCell.getIneditableInstance();
      cellSuccesses = FieldCell.getIneditableInstance();
      cellFailureReason = FieldCell.getIneditableInstance();
    }
    else {
      cellRestarts = null;
      cellSuccesses = null;
      cellFailureReason = null;
    }
  }

  static ProcessorTableRow getComputerInstance(final ProcessorTable table,
    final Node node, final int numCpus, final int numRowsInTable,
    final ProcessorTableState tableState) {
    ProcessorTableRow instance = new ProcessorTableRow(table, node, numCpus, false, null,
      0, numRowsInTable, tableState);
    instance.initRow(node);
    instance.addListeners();
    return instance;
  }

  static ProcessorTableRow getQueueInstance(final ProcessorTable table, final Node node,
    final int numCpus, final ButtonGroup buttonGroup, final int loadArraySize,
    final int numRowsInTable, final ProcessorTableState tableState) {
    ProcessorTableRow instance = new ProcessorTableRow(table, node, numCpus, true,
      buttonGroup, loadArraySize, numRowsInTable, tableState);
    instance.initRow(node);
    instance.addListeners();
    return instance;
  }

  private void addListeners() {
    if (numCpus > 1) {
      ((SpinnerCell) cellCPUsSelected).addChangeListener(new PTRCPUChangeListener(this));
    }
  }

  public void store(Properties props) {
    store(props, "");
  }

  public void store(Properties props, String prepend) {
    String group;
    if (prepend == "") {
      prepend = cellComputer.getLabel();
    }
    else {
      prepend += "." + cellComputer.getLabel();
    }
    group = prepend + ".";
    props.setProperty(group + STORE_SELECTED, String.valueOf(isSelected()));
    if (numCpus == 1) {
      props.setProperty(group + STORE_CPUS_SELECTED,
        String.valueOf(((FieldCell) cellCPUsSelected).getValue()));
    }
    else if (cellCPUsSelected != null) {
      props.setProperty(group + STORE_CPUS_SELECTED,
        String.valueOf(((SpinnerCell) cellCPUsSelected).getIntValue()));
    }
  }

  /**
   * Get the objects attributes from the properties object.
   */
  public void load(Properties props) {
    load(props, "");
  }

  /**
   * load the computers and number of CPUs selected
   */
  public void load(Properties props, String prepend) {
    String group;
    if (prepend == "") {
      prepend = cellComputer.getLabel();
    }
    else {
      prepend += "." + cellComputer.getLabel();
    }
    group = prepend + ".";
    boolean selected =
      Boolean.valueOf(props.getProperty(group + STORE_SELECTED, "false")).booleanValue();
    setSelected(selected);
    if (numCpus > 1 && isSelected()) {
      ((SpinnerCell) cellCPUsSelected)
        .setValue(Integer.parseInt(props.getProperty(group + STORE_CPUS_SELECTED,
          Integer.toString(DEFAULT_CPUS_SELECTED))));
    }
  }

  private void initRow(final Node node) {
    // table state
    if (cellNumberCpus != null) {
      cellNumberCpus.setTableState(ProcessorTableField.NUM_CPUS_MAX_H2, tableState);
    }
    if (cellLoad1 != null) {
      cellLoad1.setTableState(ProcessorTableField.LOAD_AVERAGE_H1, tableState);
      cellLoad5.setTableState(ProcessorTableField.LOAD_AVERAGE_H1, tableState);
    }
    if (cellCPUUsage != null) {
      cellCPUUsage.setTableState(ProcessorTableField.CPU_USAGE_H1, tableState);
    }
    if (cellLoadArray != null) {
      for (int i = 0; i < cellLoadArray.length; i++) {
        if (i > 0) {
          cellLoadArray[i].setTableState(ProcessorTableField.LOAD_ARRAY_X_H1, tableState);
        }
        else {
          cellLoadArray[i].setTableState(ProcessorTableField.LOAD_ARRAY_0_H1, tableState);
        }
      }
    }
    if (cellUsers != null) {
      cellUsers.setTableState(ProcessorTableField.USERS_H1, tableState);
    }
    if (cellCPUType != null) {
      cellCPUType.setTableState(ProcessorTableField.TYPE_H1, tableState);
    }
    if (cellSpeed != null) {
      cellSpeed.setTableState(ProcessorTableField.SPEED_H1, tableState);
    }
    if (cellMemory != null) {
      cellMemory.setTableState(ProcessorTableField.MEMORY_H1, tableState);
    }
    if (cellOS != null) {
      cellOS.setTableState(ProcessorTableField.OS_H1, tableState);
    }
    if (cellGpuType != null) {
      cellGpuType.setTableState(ProcessorTableField.GPU_TYPE_H1, tableState);
    }
    if (cellGpuSpeed != null) {
      cellGpuSpeed.setTableState(ProcessorTableField.GPU_SPEED_H1, tableState);
    }
    if (cellGpuMemory != null) {
      cellGpuMemory.setTableState(ProcessorTableField.GPU_MEMORY_H1, tableState);
    }
    if (cellGpuNcores != null) {
      cellGpuNcores.setTableState(ProcessorTableField.GPU_NCORES_H1, tableState);
    }
    // init
    rowInitialized = true;
    cellComputer.addActionListener(new ProcessorTableRowActionListener(this));
    if (displayQueues) {
      cellComputer.addChangeListener(new PTRComputerChangeListener(this));
    }
    if (numCpus > 1) {
      ((SpinnerCell) cellCPUsSelected).setValue(DEFAULT_CPUS_SELECTED);
      ((SpinnerCell) cellCPUsSelected).setDisabledValue(0);
    }
    else {
      ((FieldCell) cellCPUsSelected).setValue(1);
    }
    if (cellNumberCpus != null) {
      cellNumberCpus.setValue(numCpus);
    }
    if (cellMemory != null) {
      cellMemory.setEditable(false);
    }
    if (node != null) {
      cellComputer.setLabel(node.getName());
      if (cellCPUType != null) {
        cellCPUType.setValue(node.getType());
      }
      if (cellSpeed != null) {
        cellSpeed.setValue(node.getSpeed());
      }
      if (cellMemory != null) {
        cellMemory.setValue(node.getMemory());
      }
      if (cellOS != null) {
        cellOS.setValue(os);
      }
      if (cellGpuType != null) {
        cellGpuType.setValue(node.getGpuType());
      }
      if (cellGpuSpeed != null) {
        cellGpuSpeed.setValue(node.getGpuSpeed());
      }
      if (cellGpuMemory != null) {
        cellGpuMemory.setValue(node.getGpuMemory());
      }
      if (cellGpuNcores != null) {
        cellGpuNcores.setValue(node.getGpuNcores());
      }
    }
    updateSelected(false);
  }

  void turnOffLoadWarning() {
    loadWarning = false;
    if (cellCPUUsage != null) {
      cellCPUUsage.setWarning(false);
    }
    if (cellLoad1 != null) {
      cellLoad1.setWarning(false);
      cellLoad5.setWarning(false);
    }
  }

  boolean isDisplayed() {
    return displayed;
  }

  void deleteRow() {
    displayed = false;
  }

  private void add(final InputCell cell, final boolean use, final ColumnName columnName,
    final ColumnName lastColumnName, final JPanel panel, final GridBagLayout layout,
    final GridBagConstraints constraints) {
    if (use) {
      if (lastColumnName == columnName) {
        constraints.gridwidth = GridBagConstraints.REMAINDER;
      }
      cell.add(panel, layout, constraints);
    }
  }

  void display(int index, Viewport viewport) {
    displayed = true;
    if (!viewport.inViewport(index)) {
      return;
    }
    // create row
    JPanel panel = table.getTablePanel();
    GridBagLayout layout = table.getTableLayout();
    GridBagConstraints constraints = table.getTableConstraints();
    constraints.weighty = 0.0;
    constraints.weightx = 0.0;
    constraints.gridheight = 1;
    // Set display columns
    tempDisplayedFields.clear();
    tempDisplayedFields.add((Cell) cellComputer);
    tempDisplayedFields.add(cellCPUsSelected);
    if (cellNumberCpus != null && cellNumberCpus.isDisplay()) {
      tempDisplayedFields.add(cellNumberCpus);
    }
    if (cellLoad1 != null) {
      if (cellLoad1.isDisplay()) {
        tempDisplayedFields.add(cellLoad1);
      }
      if (cellLoad5.isDisplay()) {
        tempDisplayedFields.add(cellLoad5);
      }
    }
    if (cellCPUUsage != null && cellCPUUsage.isDisplay()) {
      tempDisplayedFields.add(cellCPUUsage);
    }
    if (cellLoadArray != null) {
      for (int i = 0; i < cellLoadArray.length; i++) {
        if (cellLoadArray[i].isDisplay()) {
          tempDisplayedFields.add(cellLoadArray[i]);
        }
      }
    }
    if (cellUsers != null && cellUsers.isDisplay()) {
      tempDisplayedFields.add(cellUsers);
    }
    if (cellCPUType != null && cellCPUType.isDisplay()) {
      tempDisplayedFields.add(cellCPUType);
    }
    if (cellSpeed != null && cellSpeed.isDisplay()) {
      tempDisplayedFields.add(cellSpeed);
    }
    if (cellMemory != null && cellMemory.isDisplay()) {
      tempDisplayedFields.add(cellMemory);
    }
    if (cellOS != null && cellOS.isDisplay()) {
      tempDisplayedFields.add(cellOS);
    }
    if (cellGpuType != null && cellGpuType.isDisplay()) {
      tempDisplayedFields.add(cellGpuType);
    }
    if (cellGpuSpeed != null && cellGpuSpeed.isDisplay()) {
      tempDisplayedFields.add(cellGpuSpeed);
    }
    if (cellGpuMemory != null && cellGpuMemory.isDisplay()) {
      tempDisplayedFields.add(cellGpuMemory);
    }
    if (cellGpuNcores != null && cellGpuNcores.isDisplay()) {
      tempDisplayedFields.add(cellGpuNcores);
    }
    if (cellRestarts != null) {
      tempDisplayedFields.add(cellRestarts);
      tempDisplayedFields.add(cellSuccesses);
      tempDisplayedFields.add(cellFailureReason);
    }
    // Add fields to the table
    constraints.gridwidth = 1;
    int size = tempDisplayedFields.size();
    for (int i = 0; i < size; i++) {
      Cell cell = tempDisplayedFields.get(i);
      if (i == size - 1) {
        constraints.gridwidth = GridBagConstraints.REMAINDER;
      }
      cell.add(panel, layout, constraints);
    }
    tempDisplayedFields.clear();
  }

  void performAction() {
    updateSelected(cellComputer.isSelected());
  }

  void stateChangedCPU() {
    table.msgCPUsSelectedChanged();
  }

  void stateChangedComputer() {
    if (!displayQueues) {
      return;
    }
    // handle radio button changes
    updateSelected(cellComputer.isSelected());
  }

  final void msgDropped(String reason) {
    setSelected(false);
    if (cellFailureReason != null) {
      cellFailureReason.setValue(reason);
      cellFailureReason.setToolTipText(
        "This computer was dropped from the current distributed process.");
    }
  }

  void setSelected(boolean selected) {
    // Do not allow the row to be unselected if it is disabled and the only row.
    if (!selected && numRowsInTable == 1 && !cellComputer.isEnabled()) {
      return;
    }
    cellComputer.setSelected(selected);
    updateSelected(selected);
  }

  void enableSelectionField(final boolean enabled) {
    cellComputer.setEnabled(enabled);
  }

  void setCPUsSelected(String cpusSelected) {
    if (numCpus == 1) {
      ((FieldCell) cellCPUsSelected).setValue(Integer.parseInt(cpusSelected));
    }
    else {
      ((SpinnerCell) cellCPUsSelected).setValue(Integer.parseInt(cpusSelected));
    }
  }

  void setParameters() {
    cellComputer.setSelected(false);
  }

  private void updateSelected(boolean selected) {
    cellCPUsSelected.setEnabled(selected);
    setSelectedError();
    table.msgCPUsSelectedChanged();
  }

  void setSelectedError() {
    if (table.isSecondary()) {
      cellComputer.setWarning(false);
      return;
    }
    boolean noloadAverage = false;
    if (displayQueues && cellLoadArray != null) {
      noloadAverage = cellLoadArray[0].isEmpty() || cellLoadArray[0].equals("NA");
    }
    else if (Utilities.isWindowsOS() && cellCPUUsage != null) {
      noloadAverage = cellCPUUsage.isEmpty();
    }
    else if (cellLoad1 != null) {
      noloadAverage = cellLoad1.isEmpty();
    }
    cellComputer.setWarning(cellComputer.isSelected() && noloadAverage);
  }

  final boolean isSelected() {
    return cellComputer.isSelected();
  }

  final void getParameters(final ProcesschunksParam param) {
    int numCpus = getCPUsSelected();
    if (numCpus > 0) {
      param.addMachineName(cellComputer.getLabel(), numCpus, gpuDeviceArray);
    }
  }

  final void getParameters(final BatchruntomoParam param) {
    int numCpus = getCPUsSelected();
    if (numCpus > 0) {
      if (table.isCpuTable()) {
        param.addCPUMachine(cellComputer.getLabel(), numCpus);
      }
      else if (table.isGpuTable()) {
        param.addGPUMachine(cellComputer.getLabel(), numCpus, gpuDeviceArray);
      }
    }
  }

  int getSuccesses() {
    if (cellSuccesses != null) {
      return cellSuccesses.getIntValue();
    }
    return 0;
  }

  int getCPUsSelected() {
    if (!isSelected()) {
      return 0;
    }
    if (cellCPUsSelected instanceof SpinnerCell) {
      return ((SpinnerCell) cellCPUsSelected).getIntValue();
    }
    int cpusSelected = ((FieldCell) cellCPUsSelected).getIntValue();
    if (cpusSelected == EtomoNumber.INTEGER_NULL_VALUE) {
      cpusSelected = 0;
    }
    return cpusSelected;
  }

  public boolean equals(String computer) {
    if (cellComputer.getLabel().equals(computer)) {
      return true;
    }
    return false;
  }

  void addSuccess() {
    if (cellSuccesses != null) {
      int successes = cellSuccesses.getIntValue();
      if (successes == EtomoNumber.INTEGER_NULL_VALUE) {
        successes = 1;
      }
      else {
        successes++;
      }
      cellSuccesses.setValue(successes);
    }
  }

  void resetResults() {
    if (cellRestarts != null) {
      cellSuccesses.setValue();
      cellRestarts.setValue();
      cellRestarts.setError(false);
      cellRestarts.setWarning(false);
    }
  }

  void addRestart() {
    if (cellRestarts != null) {
      int restarts = cellRestarts.getIntValue();
      if (restarts == EtomoNumber.INTEGER_NULL_VALUE) {
        restarts = 1;
      }
      else {
        restarts++;
      }
      cellRestarts.setValue(restarts);
      if (restarts >= ProcesschunksParam.DROP_VALUE) {
        cellRestarts.setError(true);
      }
      else if (restarts > 0) {
        cellRestarts.setWarning(true);
      }
    }
  }

  void setLoad(double load1, double load5, int users, String usersTooltip) {
    setLoad(cellLoad1, load1, numCpus);
    setLoad(cellLoad5, load5, numCpus);
    cellComputer.setWarning(false);
    if (cellUsers != null) {
      cellUsers.setValue(users);
      cellUsers.setToolTipText(usersTooltip);
    }
  }

  void setLoad(String[] loadArray) {
    if (cellLoadArray != null) {
      for (int i = 0; i < loadArray.length; i++) {
        if (i < cellLoadArray.length) {
          cellLoadArray[i].setValue(loadArray[i]);
        }
      }
    }
    cellComputer.setWarning(false);
  }

  void setCPUUsage(double cpuUsage, final ConstEtomoNumber numberOfProcessors) {
    double usage;
    if (numberOfProcessors == null || numberOfProcessors.isNull()) {
      usage = cpuUsage / 100.0;
    }
    else {
      usage = cpuUsage * numberOfProcessors.getInt() / 100.0;
    }
    if (cellCPUUsage != null) {
      if (loadWarning) {
        cellCPUUsage.setWarning(cpuUsage > 75);
      }
      cellCPUUsage.setValue(usage);
    }
    cellComputer.setWarning(false);
  }

  final void clearLoad(String reason, String tooltip) {
    String loadName;
    if (Utilities.isWindowsOS() && cellCPUUsage != null) {
      loadName = "CPU usage";
      cellCPUUsage.setValue();
      cellCPUUsage.setWarning(false);
    }
    else {
      loadName = "load averages";
      if (cellLoad1 != null) {
        cellLoad1.setValue();
        cellLoad1.setWarning(false);
      }
      if (cellLoad5 != null) {
        cellLoad5.setValue();
        cellLoad5.setWarning(false);
      }
      if (cellUsers != null) {
        cellUsers.setValue();
      }
    }
    setSelectedError();
    if (cellFailureReason != null) {
      cellFailureReason.setValue(reason);
      cellFailureReason.setToolTipText(tooltip);
    }
  }

  /**
   * Clear failure reason, if failure reason equals failureReason1 or 2.  This means
   * that processes can only clear their own messages.  This is useful
   * for restarting an intermittent process without losing the processchunks
   * failure reason.
   */
  final void clearFailureReason(String failureReason1, String failureReason2) {
    if (cellFailureReason != null) {
      String value = cellFailureReason.getValue();
      if (value == null
        || (!value.equals(failureReason1) && !value.equals(failureReason2))) {
        return;
      }
      clearFailureReason();
    }
  }

  final void clearFailureReason() {
    if (cellFailureReason != null) {
      cellFailureReason.setValue();
      cellFailureReason.setToolTipText(null);
    }
  }

  private final void setLoad(FieldCell cellLoad, double load, int numberCpus) {
    if (cellLoad == null) {
      return;
    }
    if (loadWarning) {
      cellLoad.setWarning(load >= numberCpus);
    }
    cellLoad.setValue(load);
  }

  final int getHeight() {
    return cellComputer.getHeight();
  }

  final String getComputer() {
    return cellComputer.getLabel();
  }

  private class ProcessorTableRowActionListener implements ActionListener {
    ProcessorTableRow adaptee;

    ProcessorTableRowActionListener(ProcessorTableRow adaptee) {
      this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent event) {
      adaptee.performAction();
    }
  }

  private class PTRCPUChangeListener implements ChangeListener {
    ProcessorTableRow adaptee;

    PTRCPUChangeListener(ProcessorTableRow adaptee) {
      this.adaptee = adaptee;
    }

    public void stateChanged(ChangeEvent event) {
      adaptee.stateChangedCPU();
    }
  }

  private class PTRComputerChangeListener implements ChangeListener {
    ProcessorTableRow adaptee;

    PTRComputerChangeListener(ProcessorTableRow adaptee) {
      this.adaptee = adaptee;
    }

    public void stateChanged(ChangeEvent event) {
      adaptee.stateChangedComputer();
    }
  }
}
/**
 * <p> $Log$
 * <p> Revision 1.5  2011/07/23 03:05:33  sueh
 * <p> Bug# 1517 In setCPUUsage, incorporate the numberOfProcessors into the CPU usage, and set the
 * <p> warning and 75%.
 * <p>
 * <p> Revision 1.4  2011/04/04 17:24:16  sueh
 * <p> bug# 1416 Fixed a bug in setCPUsSelected - class wasn't handling a computer with a single CPU or GPU.
 * <p>
 * <p> Revision 1.3  2011/02/22 18:20:22  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.2  2011/02/03 06:16:40  sueh
 * <p> bug# 1422 Passing a Node instead of values that came from a Node.
 * <p> Added the ability to turn off the load warning.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.35  2009/04/20 20:13:50  sueh
 * <p> bug# 1192 Added setCPUsSelected.  Setting the computer and CPUs by
 * <p> calling ProcesschunksParam.addMachineName once.
 * <p>
 * <p> Revision 1.34  2009/04/13 22:57:49  sueh
 * <p> Removed unnecessary print.
 * <p>
 * <p> Revision 1.33  2008/10/06 22:44:46  sueh
 * <p> bug# 1113 Changed addRow to display.  Display now takes index and viewport so it can check if it should display itself.
 * <p>
 * <p> Revision 1.32  2007/09/27 21:03:23  sueh
 * <p> bug# 1044 Added a displayQueues mode.
 * <p>
 * <p> Revision 1.31  2007/07/30 19:21:13  sueh
 * <p> bug# 1029 Resetting restarts in resetResults().
 * <p>
 * <p> Revision 1.30  2007/05/25 00:28:35  sueh
 * <p> bug# 964 Added tooltip to clearLoadAverage.  Added a second reason to
 * <p> clearFailureReason.
 * <p>
 * <p> Revision 1.29  2007/05/21 18:11:30  sueh
 * <p> bug# 992 Added usersColumn.  Do not display Users column when
 * <p> usersColumn is false.
 * <p>
 * <p> Revision 1.28  2007/04/02 21:52:50  sueh
 * <p> bug# 964 Added FieldCell.editable to make instances of FieldCell that can't be
 * <p> edited.  This allows FieldCell.setEditable and setEnabled to be called without
 * <p> checking whether a field should be editable.
 * <p>
 * <p> Revision 1.27  2007/03/27 19:31:56  sueh
 * <p> bug# 964 Changed InputCell.setEnabled() to setEditable.
 * <p>
 * <p> Revision 1.26  2007/02/09 00:52:03  sueh
 * <p> bug# 962 Made TooltipFormatter a singleton and moved its use to low-level ui
 * <p> classes.
 * <p>
 * <p> Revision 1.25  2007/02/05 23:42:05  sueh
 * <p> bug# 962 Added SpinnerCell.getInstance.
 * <p>
 * <p> Revision 1.24  2006/11/29 00:22:13  sueh
 * <p> bug# 934 Added the parameter String failureReason to clearFailureReason(), so
 * <p> that it won't delete a failure reason by another processes.  Changed
 * <p> setSelectedError() to set a warning instead of an error.
 * <p>
 * <p> Revision 1.23  2006/11/18 00:50:01  sueh
 * <p> bug# 936 Parallel Processing:  added user list tooltip to user column.
 * <p>
 * <p> Revision 1.22  2006/11/08 21:10:13  sueh
 * <p> bug# 936:  Remove cellLoad15 and add cellUsers.  Clear cellUsers with the load
 * <p> averages, but never set its warning.  Check that the load average has been set
 * <p> by just looking at cellLoad1 (for Linux/Mac) since they are all set together.
 * <p>
 * <p> Revision 1.21  2006/02/08 03:46:14  sueh
 * <p> bug# 796 added cellCPUUsage to use instead of load averages in windows
 * <p>
 * <p> Revision 1.20  2005/12/16 01:46:10  sueh
 * <p> bug# 784 Added tool tips.
 * <p>
 * <p> Revision 1.19  2005/12/14 20:58:29  sueh
 * <p> bug# 784 Added context sensitive tool tips to failure reasons.
 * <p>
 * <p> Revision 1.18  2005/11/14 22:17:35  sueh
 * <p> bug# 762 Made performAction() and stateChanged() protected.
 * <p>
 * <p> Revision 1.17  2005/11/04 00:55:08  sueh
 * <p> bug# 732 Added isDisplayed() and deleteRow().  The resize functionality
 * <p> needs to know what rows are displayed.
 * <p>
 * <p> Revision 1.16  2005/09/27 23:46:56  sueh
 * <p> bug# 532 Moved call to initRow() to the constructor so that the cells will
 * <p> all be constructed before that perferences are read.
 * <p>
 * <p> Revision 1.15  2005/09/22 21:32:55  sueh
 * <p> bug# 532 Removed restartsError.  Taking error level from ParallelPanel in
 * <p> ProcessorTableRow.
 * <p>
 * <p> Revision 1.14  2005/09/13 00:02:07  sueh
 * <p> bug# 532 Implemented storable to store whether cellComputer is selected
 * <p> and how many CPUs are selected.
 * <p>
 * <p> Revision 1.13  2005/09/10 01:55:15  sueh
 * <p> bug# 532 Added clearFailureReason() so that the failure reason can be
 * <p> cleared when a new connection to the computer is attempted.
 * <p>
 * <p> Revision 1.12  2005/09/09 21:47:55  sueh
 * <p> bug# 532 Passed reason string to clearLoadAverage().
 * <p>
 * <p> Revision 1.11  2005/09/01 18:03:33  sueh
 * <p> bug# 532 Added clearLoadAverage() to clear the load averages when the
 * <p> load average command fails.  Added a drop reason.  Added a error level
 * <p> for the restarts column.
 * <p>
 * <p> Revision 1.10  2005/08/27 22:42:07  sueh
 * <p> bug# 532 Added cells for speed and memory.  Displaying columns based
 * <p> on booleans:  memoryColumn, numberColumn, etc.  Changed cellOs to
 * <p> cellOS.  In setSelected() set cellComputer.error to true if the load fields
 * <p> are empty.  Turn off cellComputer.error when load fields are set.
 * <p>
 * <p> Revision 1.9  2005/08/22 18:15:05  sueh
 * <p> bug# 532 Removed dummy load averages.
 * <p>
 * <p> Revision 1.8  2005/08/04 20:19:33  sueh
 * <p> bug# 532  Removed demo fields and functions.  Added functions:
 * <p> getWidth, addSuccess, and drop.
 * <p>
 * <p> Revision 1.7  2005/08/01 18:15:40  sueh
 * <p> bug# 532 Changed ProcessorTableRow.signalRestart() to addRestart.
 * <p> Added Failure Reason Column.  Added
 * <p> getParameters(ProcesschunksParam).  Changed getCpusSelected() to
 * <p> take the selection state into account.  Changed resetResults() to reset
 * <p> error and warning states.  Added setLoad().
 * <p>
 * <p> Revision 1.6  2005/07/19 22:35:28  sueh
 * <p> bug# 532 Since the error setting affects the background, don't set error
 * <p> == true for CellRestarts automatically.
 * <p>
 * <p> Revision 1.5  2005/07/15 16:32:05  sueh
 * <p> bug# 532 Removed experiment about not scrolling headers
 * <p>
 * <p> Revision 1.4  2005/07/14 22:15:01  sueh
 * <p> bug# 532 Experimenting with extending GridBagLayout to make a header
 * <p> in the scroll pane.
 * <p>
 * <p> Revision 1.3  2005/07/11 23:22:56  sueh
 * <p> bug# 619 Showing results when signals rather then all at once.  Add
 * <p> functions:  getBorderHeight, getHeight, getSuccessFactor, isSelected,
 * <p> signalRestart, and signalSuccess.
 * <p>
 * <p> Revision 1.2  2005/07/01 23:06:06  sueh
 * <p> bug# 619 Added getCpusSelected, setSuccesses, stateChanged
 * <p>
 * <p> Revision 1.1  2005/07/01 21:22:02  sueh
 * <p> bug# 619 A row in a table containing a list of computers and CPUs
 * <p> </p>
 */
