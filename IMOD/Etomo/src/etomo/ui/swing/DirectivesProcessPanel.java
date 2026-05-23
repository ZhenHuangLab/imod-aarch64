package etomo.ui.swing;

import etomo.BaseManager;
import etomo.type.AxisID;
import etomo.type.InterfaceType;
/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class DirectivesProcessPanel extends AxisProcessPanel {
  DirectivesProcessPanel(final BaseManager manager,
      final InterfaceType interfaceType,final AxisProgressPanel axisProgressPanel) {
    super(AxisID.ONLY, manager, true, true, interfaceType, false,  axisProgressPanel);
    createProcessControlPanel();
    showBothAxis();
    initializePanels();
  }
}
