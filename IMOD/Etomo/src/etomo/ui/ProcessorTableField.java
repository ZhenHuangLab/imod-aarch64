package etomo.ui;

/**
 * <p>Description: An enum used to identify fields in the Processor Table..</p>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class ProcessorTableField implements TableField {
  public static final ProcessorTableField NUM_CPUS_H1 = new ProcessorTableField();
  public static final ProcessorTableField NUM_CPUS_MAX_H2 = new ProcessorTableField();
  public static final ProcessorTableField LOAD_AVERAGE_H1 = new ProcessorTableField();
  public static final ProcessorTableField CPU_USAGE_H1 = new ProcessorTableField();
  public static final ProcessorTableField LOAD_ARRAY_0_H1 = new ProcessorTableField();
  public static final ProcessorTableField LOAD_ARRAY_X_H1 = new ProcessorTableField();
  public static final ProcessorTableField USERS_H1 = new ProcessorTableField();
  public static final ProcessorTableField TYPE_H1 = new ProcessorTableField();
  public static final ProcessorTableField SPEED_H1 = new ProcessorTableField();
  public static final ProcessorTableField MEMORY_H1 = new ProcessorTableField();
  public static final ProcessorTableField OS_H1 = new ProcessorTableField();
  public static final ProcessorTableField RESTARTS_H1 = new ProcessorTableField();
  public static final ProcessorTableField GPU_TYPE_H1 = new ProcessorTableField();
  public static final ProcessorTableField GPU_SPEED_H1 = new ProcessorTableField();
  public static final ProcessorTableField GPU_MEMORY_H1 = new ProcessorTableField();
  public static final ProcessorTableField GPU_NCORES_H1 = new ProcessorTableField();

  private ProcessorTableField() {}
}
