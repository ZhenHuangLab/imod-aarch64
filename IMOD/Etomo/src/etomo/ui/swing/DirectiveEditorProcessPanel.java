package etomo.ui.swing;

import etomo.DirectiveEditorManager;
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
final class DirectiveEditorProcessPanel extends AxisProcessPanel {
  DirectiveEditorProcessPanel(final DirectiveEditorManager manager,
    final InterfaceType interfaceType, final AxisProgressPanel axisProgressPanel) {
    super(AxisID.ONLY, manager, true, true, interfaceType, false, axisProgressPanel);
    createProcessControlPanel();
    showBothAxis();
    initializePanels();
  }

  void showBothAxis() {
    setBackground(Colors.getBackgroundTools());
  }
}
