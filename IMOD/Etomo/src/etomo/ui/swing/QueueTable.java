package etomo.ui.swing;

import javax.swing.ButtonGroup;

import etomo.BaseManager;
import etomo.comscript.BatchruntomoParam;
import etomo.comscript.IntermittentCommand;
import etomo.comscript.ProcesschunksParam;
import etomo.comscript.QueuechunkParam;
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
 * <p>Copyright: Copyright 2010 - 2014 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *          <p/>
 *          <p> $Log$
 *          <p> Revision 1.3  2011/07/18 22:44:59  sueh
 *          <p> Bug# 1515 Removed isSelectOnlyRow - no longer needed.
 *          <p>
 *          <p> Revision 1.2  2011/02/22 18:20:39  sueh
 *          <p> bug# 1437 Reformatting.
 *          <p>
 *          <p> Revision 1.1  2011/02/03 06:17:02  sueh
 *          <p> bug# 1422 Child of ProcessorTable that makes a ProcessorTable display
 *          <p> queues.
 *          <p> </p>
 */

final class QueueTable extends ProcessorTable {
  private static final String PREPEND = ".Queue";

  private ButtonGroup buttonGroup = null;

  QueueTable(final BaseManager manager, final ParallelPanel parent, final AxisID axisID,
    final boolean runnable, final Expander moreLess, final InterfaceType interfaceType) {
    super(manager, parent, axisID, true, runnable, moreLess, interfaceType);
  }

  ProcessorType getProcessorType() {
    return ProcessorType.QUEUE;
  }

  String getStorePrepend() {
    return getGroupKey() + PREPEND;
  }

  String getLoadPrepend(ConstEtomoVersion version) {
    return getGroupKey() + PREPEND;
  }

  int getSize() {
    buttonGroup = new ButtonGroup();
    return Network.getNumQueues();
  }

  Node getNode(final int index) {
    return Network.getQueue(index);
  }

  ProcessorTableRow createProcessorTableRow(final ProcessorTable processorTable,
    final Node node, final int numRowsInTable, final ProcessorTableState tableState) {
    return ProcessorTableRow.getQueueInstance(processorTable, node, node.getNumber(),
      buttonGroup, Math.max(1, CpuAdoc.INSTANCE.getLoadUnitsArray().length), numRowsInTable,
      tableState);
  }

  String getHeader1ComputerText() {
    return "Queue";
  }

  String getNoCpusSelectedErrorMessage() {
    return "A queue must be selected.";
  }

  boolean useUsersColumn() {
    return false;
  }

  boolean isCpuTable() {
    return false;
  }

  boolean isGpuTable() {
    return false;
  }

  void getParameters(final ProcesschunksParam param) {
    String queue = getFirstSelectedComputer();
    Node node = Network.getQueue(queue);
    if (node != null) {
      node.getParameters(param);
    }
    param.setQueue(queue);
    super.getParameters(param);
  }

  void getParameters(final ProcessingMethod method, final BatchruntomoParam param) {
    if (method == ProcessingMethod.PP_CPU) {
      param.resetCPUMachineList();
      getParameters(param);
    }
  }

  IntermittentCommand getIntermittentCommand(final String computer) {
    return QueuechunkParam.getLoadInstance(computer, axisID, manager);
  }

  boolean isExcludeNode(final Node node) {
    return false;
  }

  boolean isNiceable() {
    return true;
  }

  void initRow(ProcessorTableRow row) {}
}
