package etomo.ui.swing;

import etomo.BaseManager;
import etomo.type.AxisID;
import etomo.type.InterfaceType;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2013 - 2015 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
* 
* <p> $Log$ </p>
*/
final class BatchRunTomoProcessPanel extends AxisProcessPanel {
  BatchRunTomoProcessPanel(final BaseManager manager, final InterfaceType interfaceType,
    final AxisProgressPanel axisProgressPanel) {
    super(AxisID.ONLY, manager, true, false, interfaceType, true, axisProgressPanel);
    createProcessControlPanel();
    showBothAxis();
    initializePanels();
  }

  void showBothAxis() {
    setBackground(Colors.getBackgroundBatchruntomo());
  }
}
