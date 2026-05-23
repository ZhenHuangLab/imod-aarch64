package etomo.logic;

import etomo.ui.TableField;

/**
 * <p>Description: Collects and distributes information about the state of a table.</p>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface TableState {
  public static final int DEFAULT_GRIDWIDTH = 1;

  public boolean isDisplay(TableField tableField);

  public int getGridwidth(TableField tableField);
}
