package etomo.ui.swing;

import etomo.ToolsManager;
import etomo.type.AxisID;
import etomo.type.InterfaceType;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2010 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.2  2010/07/02 03:20:10  sueh
 * <p> bug# 1388 Constructing super class with popupChunkWarnings equal to true.
 * <p>
 * <p> Revision 1.1  2010/02/17 04:57:21  sueh
 * <p> bug# 1301 Process panel for the ToolsManager.
 * <p> </p>
 */
final class ToolsProcessPanel extends AxisProcessPanel {
  ToolsProcessPanel(ToolsManager manager, final AxisProgressPanel axisProgressPanel) {
    super(AxisID.ONLY, manager, true, true, InterfaceType.TOOLS, false,
      axisProgressPanel);
    createProcessControlPanel();
    showBothAxis();
    initializePanels();
  }

  void showBothAxis() {
    setBackground(Colors.getBackgroundTools());
  }
}
