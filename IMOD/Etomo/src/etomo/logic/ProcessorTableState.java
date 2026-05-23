package etomo.logic;

import etomo.storage.CpuAdoc;
import etomo.type.InterfaceType;
import etomo.ui.Expander;
import etomo.ui.ProcessorTableField;
import etomo.ui.TableField;
import etomo.ui.swing.ParallelProgressDisplay;
import etomo.util.Utilities;

/**
 * <p>Description: Collects and distributes information about the state of a processor
 * table.</p>
 * <pre>Permanent state (per table):
 * - windows - bool (from OS)
 * - usersColumn - bool (from cpu.adoc global section)
 * - loadUnits - number of elements - int (from cpu.adoc global section)
 * - cpuType - bool (from cpu.adoc all sections)
 * - memory - bool (from cpu.adoc all sections)
 * - numberGt1 (includes gpuGt1) - bool (from cpu.adoc all sections)
 * - os - bool (from cpu.adoc all sections)
 * - speed - bool (from cpu.adoc all sections)
 * - runnable - bool (from table)
 * - type - cpu, gpu, queue - enum (from table)
 * 
 * Changeable state:
 * - less - bool (from panel header)
 * - secondary - bool (from table)
 * 
 * Information from cpu.adoc sections requires the user name, and the current interface.
 * 
 * Fields
 * 
 * Fields are displayed from left to right, and then from top row to bottom row. 
 * Fields should only depend on previously displayed fields.  Checking whether a
 * field is used in a table should only rely on the permanent state.  H2 and Row
 * fields always have a grid width of 1.
 * 
 * - computerH1
 * - computerH2
 * - computerRow
 * 
 * - #cpusH1:
 *   -gridwidth: 2
 *    - numberGt1
 *    - !less
 * - #cpusUsedH2
 * - #cpusUsedRow
 * - #cpusMaxH2:
 *   - use: numberGt1
 *   - display: !less
 * - #cpusMaxRow: #cpusMaxH2
 * 
 * - loadAverageH1:
 *   - use:
 *     - type == (cpu || gpu)
 *     - !windows
 *   - display: !secondary
 *   - grid width: 2
 * - load1H2: loadAverageH1
 * - load1Row: loadAverageH1
 * - load5H2: loadAverageH1
 * - load5Row: loadAverageH1
 * 
 * - cpuUsageH1:
 *   - use:
 *     - type == (cpu || gpu)
 *     - windows
 *   - display: !secondary
 * - cpuUsageH2: cpuUsageH1
 * - cpuUsageRow: cpuUsageH1
 * 
 * - loadArray0H1:
 *   - use: type == queue
 *   - display: !secondary
 *  - loadH2: loadH1
 *  - loadRow: loadH1
 * 
 * - loadArrayXH1:
 *   - use:
 *     - type == queue
 *     - loadUnits > 1
 *   - display: !secondary
 * - loadXH2: loadXH1
 * - loadXRow: loadXH1
 * 
 * - usersH1:
 *   - use:
 *     - type == (cpu || gpu)
 *     - !windows
 *     - usersColumn
 *   - display:
 *     - !secondary
 *     - !less
 * - usersH2: usersH1
 * - usersRow: usersH1
 * 
 * - typeH1:
 *   - use:
 *     - cpuType
 *   - display:
 *     - !secondary
 *     - !less
 * - typeH2: typeH1
 * - typeRow: typeH1
 * 
 * - speedH1:
 *   - use:
 *     - speed
 *   - display:
 *     - !secondary
 *     - !less
 * - speedH2: speedH1
 * - speedRow: speedH1
 * 
 * - memoryH1:
 *   - use:
 *     - memory
 *   - display:
 *     - !secondary
 *     - !less
 * - memoryH2: memoryH1
 * - memoryRow: memoryH1
 * 
 * - osH1:
 *   - use:
 *     - os
 *   - display:
 *     - !secondary
 *     - !less
 * - osH2: osH1
 * - osRow: osH1
 * 
 * - restartsH1: use: runnable
 * - restartsH2: restartsH1
 * - restartsRow: restartsH1
 * 
 * - finishedH1: restartsH1
 * - finishedH2: restartsH1
 * - finishedRow: restartsH1
 * 
 * - failureH1: restartsH1
 * - failureH2: restartsH1
 * - failureRow: restartsH1</pre>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class ProcessorTableState implements TableState {
  private static final boolean WINDOWS = Utilities.isWindowsOS();
  private static final boolean USERS_COLUMN = CpuAdoc.INSTANCE.isUsersColumn();
  private static final int LOAD_UNITS = CpuAdoc.INSTANCE.getLoadUnits();
  private static final int GRIDWIDTH_2 = 2;

  private final Expander moreLess;
  private final boolean runnable;
  private final ParallelProgressDisplay display;
  private final ProcessorType processorType;
  private final boolean cpuType;
  private final boolean numberGt1;
  private final boolean gpuGt1;
  private final boolean memory;
  private final boolean os;
  private final boolean speed;
  private final boolean gpuType;
  private final boolean gpuMemory;
  private final boolean gpuNcores;
  private final boolean gpuSpeed;

  public ProcessorTableState(final InterfaceType interfaceType, final Expander moreLess,
    final boolean runnable, final ParallelProgressDisplay display,
    final ProcessorType processorType) {
    this.moreLess = moreLess;
    this.runnable = runnable;
    this.display = display;
    this.processorType = processorType;
    cpuType = CpuAdoc.INSTANCE.isType(interfaceType, processorType);
    numberGt1 = CpuAdoc.INSTANCE.isNumberGt1(interfaceType, processorType);
    gpuGt1 = CpuAdoc.INSTANCE.isGpuGt1(interfaceType, processorType);
    memory = CpuAdoc.INSTANCE.isMemory(interfaceType, processorType);
    os = CpuAdoc.INSTANCE.isOs(interfaceType, processorType);
    speed = CpuAdoc.INSTANCE.isSpeed(interfaceType, processorType);
    gpuType = CpuAdoc.INSTANCE.isGpuType(interfaceType, processorType);
    gpuMemory = CpuAdoc.INSTANCE.isGpuMemory(interfaceType, processorType);
    gpuNcores = CpuAdoc.INSTANCE.isGpuNcores(interfaceType, processorType);
    gpuSpeed = CpuAdoc.INSTANCE.isGpuSpeed(interfaceType, processorType);
  }

  public boolean isUse(final ProcessorTableField tableField) {
    if (tableField == ProcessorTableField.NUM_CPUS_MAX_H2) {
      if (processorType == ProcessorType.GPU) {
        return gpuGt1;
      }
      return numberGt1;
    }
    if (tableField == ProcessorTableField.LOAD_AVERAGE_H1) {
      return !WINDOWS
        && (processorType == ProcessorType.CPU || processorType == ProcessorType.GPU);
    }
    if (tableField == ProcessorTableField.CPU_USAGE_H1) {
      return WINDOWS
        && (processorType == ProcessorType.CPU || processorType == ProcessorType.GPU);
    }
    if (tableField == ProcessorTableField.LOAD_ARRAY_0_H1) {
      return processorType == ProcessorType.QUEUE;
    }
    if (tableField == ProcessorTableField.LOAD_ARRAY_X_H1) {
      return processorType == ProcessorType.QUEUE && LOAD_UNITS > 1;
    }
    if (tableField == ProcessorTableField.USERS_H1) {
      return USERS_COLUMN && !WINDOWS
        && (processorType == ProcessorType.CPU || processorType == ProcessorType.GPU);
    }
    if (tableField == ProcessorTableField.TYPE_H1) {
      return cpuType;
    }
    if (tableField == ProcessorTableField.SPEED_H1) {
      return speed;
    }
    if (tableField == ProcessorTableField.MEMORY_H1) {
      return memory;
    }
    if (tableField == ProcessorTableField.OS_H1) {
      return os;
    }
    if (tableField == ProcessorTableField.RESTARTS_H1) {
      return runnable;
    }
    if (tableField == ProcessorTableField.GPU_TYPE_H1) {
      return gpuType;
    }
    if (tableField == ProcessorTableField.GPU_SPEED_H1) {
      return gpuSpeed;
    }
    if (tableField == ProcessorTableField.GPU_MEMORY_H1) {
      return gpuMemory;
    }
    if (tableField == ProcessorTableField.GPU_NCORES_H1) {
      return gpuNcores;
    }
    return true;
  }

  public boolean isDisplay(final TableField tableField) {
    if (tableField == ProcessorTableField.NUM_CPUS_MAX_H2) {
      return moreLess == null || moreLess.isExpanded();
    }
    if (tableField == ProcessorTableField.LOAD_AVERAGE_H1
      || tableField == ProcessorTableField.CPU_USAGE_H1
      || tableField == ProcessorTableField.LOAD_ARRAY_0_H1) {
      return !display.isSecondary();
    }
    if (tableField == ProcessorTableField.USERS_H1
      || tableField == ProcessorTableField.TYPE_H1
      || tableField == ProcessorTableField.SPEED_H1
      || tableField == ProcessorTableField.MEMORY_H1
      || tableField == ProcessorTableField.OS_H1
      || tableField == ProcessorTableField.GPU_TYPE_H1
      || tableField == ProcessorTableField.GPU_SPEED_H1
      || tableField == ProcessorTableField.GPU_MEMORY_H1
      || tableField == ProcessorTableField.GPU_NCORES_H1) {
      return (moreLess == null || moreLess.isExpanded()) && !display.isSecondary();
    }
    return true;
  }

  public int getGridwidth(final TableField tableField) {
    if (tableField == ProcessorTableField.NUM_CPUS_H1) {
      if (moreLess.isExpanded() && ((processorType == ProcessorType.GPU && gpuGt1)
        || (processorType != ProcessorType.GPU && numberGt1))) {
        return GRIDWIDTH_2;
      }
    }
    else if (tableField == ProcessorTableField.LOAD_AVERAGE_H1) {
      return GRIDWIDTH_2;
    }
    return DEFAULT_GRIDWIDTH;
  }
}
