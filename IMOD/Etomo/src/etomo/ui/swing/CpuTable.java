package etomo.ui.swing;

import javax.swing.ButtonGroup;

import etomo.BaseManager;
import etomo.comscript.BatchruntomoParam;
import etomo.comscript.IntermittentCommand;
import etomo.comscript.LoadAverageParam;
import etomo.logic.ProcessorTableState;
import etomo.logic.ProcessorType;
import etomo.storage.CpuAdoc;
import etomo.storage.Network;
import etomo.storage.Node;
import etomo.type.AxisID;
import etomo.type.ConstEtomoVersion;
import etomo.type.InterfaceType;
import etomo.type.ProcessingMethod;
import etomo.ui.Expander;

/**
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright 2010 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *          <p/>
 *          <p> $Log$
 *          <p> Revision 1.3  2011/07/18 22:44:44  sueh
 *          <p> Bug# 1515 Removed isSelectOnlyRow - no longer needed.
 *          <p>
 *          <p> Revision 1.2  2011/02/22 18:07:08  sueh
 *          <p> bug# 1437 Reformatting.
 *          <p>
 *          <p> Revision 1.1  2011/02/03 06:13:39  sueh
 *          <p> bug# 1422 Child of ProcessorTable that makes a ProcessorTable display
 *          <p> CPUs.
 *          <p> </p>
 */
class CpuTable extends ProcessorTable {
  private final String PREPEND = ".Cpu";

  private final boolean usersColumn;

  CpuTable(final BaseManager manager, final ParallelPanel parent, final AxisID axisID,
    final boolean runnable, final Expander moreLess, final InterfaceType interfaceType) {
    super(manager, parent, axisID, false, runnable, moreLess, interfaceType);
    usersColumn = CpuAdoc.INSTANCE.isUsersColumn();
  }

  ProcessorType getProcessorType() {
    return ProcessorType.CPU;
  }

  boolean isCpuTable() {
    return true;
  }

  boolean isGpuTable() {
    return false;
  }

  String getStorePrepend() {
    return getGroupKey() + PREPEND;
  }

  String getLoadPrepend(ConstEtomoVersion version) {
    if (version.ge("1.1")) {
      return getGroupKey() + PREPEND;
    }
    return "ProcessorTable";
  }

  final int getSize() {
    return Network.getNumComputers();
  }

  void getParameters(final ProcessingMethod method, final BatchruntomoParam param) {
    if (method == ProcessingMethod.PP_CPU) {
      param.resetCPUMachineList();
      getParameters(param);
    }
  }

  final ButtonGroup getButtonGroup() {
    return null;
  }

  final Node getNode(final int index) {
    return Network.getComputer(manager, index, axisID, manager.getPropertyUserDir());
  }

  ProcessorTableRow createProcessorTableRow(final ProcessorTable processorTable,
    final Node node, final int numRowsInTable, final ProcessorTableState tableState) {
    return ProcessorTableRow.getComputerInstance(processorTable, node, node.getNumber(),
      numRowsInTable, tableState);
  }

  String getHeader1ComputerText() {
    return "Computer";
  }

  String getNoCpusSelectedErrorMessage() {
    return "At least one computer must be selected.";
  }

  final IntermittentCommand getIntermittentCommand(final String computer) {
    return LoadAverageParam.getInstance(computer, manager);
  }

  boolean isExcludeNode(final Node node) {
    return false;
  }

  final boolean isNiceable() {
    return true;
  }

  void initRow(ProcessorTableRow row) {}
}
