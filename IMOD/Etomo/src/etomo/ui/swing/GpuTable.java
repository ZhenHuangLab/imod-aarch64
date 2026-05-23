package etomo.ui.swing;

import etomo.BaseManager;
import etomo.comscript.BatchruntomoParam;
import etomo.comscript.ProcesschunksParam;
import etomo.logic.ProcessorTableState;
import etomo.logic.ProcessorType;
import etomo.storage.Node;
import etomo.type.AxisID;
import etomo.type.ConstEtomoVersion;
import etomo.type.InterfaceType;
import etomo.type.ProcessingMethod;
import etomo.ui.Expander;

import java.util.Map;

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
 *          <p> Revision 1.2  2011/02/22 18:11:51  sueh
 *          <p> bug# 1437 Reformatting.
 *          <p>
 *          <p> Revision 1.1  2011/02/03 06:13:08  sueh
 *          <p> bug# 1422 Child of CpuTable that makes a ProcessorTable display GPUs.
 *          <p> </p>
 */
final class GpuTable extends CpuTable {
  private static final String PREPEND = ".Gpu";

  GpuTable(final BaseManager manager, final ParallelPanel parent, final AxisID axisID,
    final boolean runnable, final Expander moreLess, final InterfaceType interfaceType) {
    super(manager, parent, axisID, runnable, moreLess, interfaceType);
  }
  
  ProcessorType getProcessorType() {
    return ProcessorType.GPU;
  }

  boolean isCpuTable() {
    return false;
  }

  boolean isGpuTable() {
    return true;
  }

  String getheader1NumberCPUsTitle() {
    return "# GPUs";
  }

  String getStorePrepend() {
    return getGroupKey() + PREPEND;
  }

  String getLoadPrepend(ConstEtomoVersion version) {
    return getGroupKey() + PREPEND;
  }

  String getHeader1ComputerText() {
    return "GPU";
  }

  String getNoCpusSelectedErrorMessage() {
    return "At least one GPU must be selected.";
  }

  boolean isExcludeNode(final Node node) {
    if (!node.isGpu()) {
      return true;
    }
    if (node.isGpuLocal()
      && !node.isLocalHost(manager, axisID, manager.getPropertyUserDir())) {
      return true;
    }
    return false;
  }

  Map<String, String> getMachineMap(final BatchruntomoParam param) {
    return param.getGPUMachineMap();
  }

  void getParameters(final ProcesschunksParam param) {
    param.setGpuProcessing(true);
    super.getParameters(param);
  }

  void getParameters(final ProcessingMethod method, final BatchruntomoParam param) {
    if (method == ProcessingMethod.PP_GPU) {
      param.resetGPUMachineList();
      getParameters(param);
    }
  }

  boolean enableNumberColumn(final Node node) {
    // numberColumn is true if an number attribute is not defaulted to 1
    // 1436 unnecessary column (was !isDefault and was always true)
    return node.getGpuNumber() > 1;
  }

  ProcessorTableRow createProcessorTableRow(final ProcessorTable processorTable,
    final Node node, final int numRowsInTable, final ProcessorTableState tableState) {
    return ProcessorTableRow.getComputerInstance(processorTable, node, node
      .getGpuNumber(), numRowsInTable, tableState);
  }

  void initRow(ProcessorTableRow row) {
    row.turnOffLoadWarning();
  }
}
