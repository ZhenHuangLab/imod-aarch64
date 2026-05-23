package etomo.logic;

/**
 * <p>Description: Processor type: CPU, GPU, or queue.</p>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class ProcessorType {
  public static final ProcessorType CPU = new ProcessorType("cpu");
  public static final ProcessorType GPU = new ProcessorType("gpu");
  public static final ProcessorType QUEUE = new ProcessorType("queue");

  private final String name;

  private ProcessorType(final String name) {
    this.name = name;
  }

  public String toString() {
    return name;
  }
}
