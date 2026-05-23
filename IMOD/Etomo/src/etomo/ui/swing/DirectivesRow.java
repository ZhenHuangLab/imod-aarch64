package etomo.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import etomo.type.BatchRunTomoStatus;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
interface DirectivesRow {
  public void display(JPanel pnlTable, GridBagLayout layout,
    GridBagConstraints constraints);

  public void remove();

  public void statusChanged(BatchRunTomoStatus status);
}
