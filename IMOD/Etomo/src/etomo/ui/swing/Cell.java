package etomo.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import etomo.logic.TableState;
import etomo.ui.TableField;

/**
 * <p>Description: Parent class for header cells and input cells.</p>
 * 
 * <p>Copyright: Copyright 2006 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.2  2009/10/15 23:27:31  sueh
 * <p> bug# 1274 Added msgLabelChanged.
 * <p>
 * <p> Revision 1.1  2007/04/02 21:44:11  sueh
 * <p> bug# 964 Interface for HeaderCell and InputCell.
 * <p> </p>
 */
abstract class Cell {
  private TableField tableField = null;
  private TableState tableState = null;

  abstract void setEnabled(boolean enable);

  /**
   * Message from row header or column header that their label has changed.
   */
  abstract void msgLabelChanged();

  abstract void add(JPanel panel, GridBagLayout layout, GridBagConstraints constraints);

  void setTableState(final TableField tableField, final TableState tableState) {
    this.tableField = tableField;
    this.tableState = tableState;
  }

  boolean isDisplay() {
    if (tableState == null) {
      return true;
    }
    return tableState.isDisplay(tableField);
  }

  int getGridwidth() {
    if (tableState == null) {
      return TableState.DEFAULT_GRIDWIDTH;
    }
    return tableState.getGridwidth(tableField);
  }
}
