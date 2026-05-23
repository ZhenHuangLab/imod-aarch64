package etomo.ui.swing;

import etomo.ParallelManager;
import etomo.type.AxisID;
import etomo.type.InterfaceType;

/**
* <p>Description: </p>
* 
 * <p>Copyright: Copyright 2006 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
*/
final class ParallelProcessPanel extends AxisProcessPanel {
  ParallelProcessPanel(ParallelManager manager,
    final AxisProgressPanel axisProgressPanel) {
    super(AxisID.ONLY, manager, true, true, InterfaceType.PP, false, axisProgressPanel);
    createProcessControlPanel();
    showBothAxis();
    initializePanels();
  }

  void showBothAxis() {
    setBackground(Colors.getBackgroundParallel());
  }
}
/**
* <p> $Log$
* <p> Revision 1.1  2010/11/13 16:07:34  sueh
* <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
* <p>
* <p> Revision 1.4  2010/07/02 03:19:20  sueh
* <p> bug# 1388 Constructing super class with popupChunkWarnings equal to true.
* <p>
* <p> Revision 1.3  2006/04/07 23:32:39  sueh
* <p> bug# 846 Changing the background colors for java 1.5.
* <p>
* <p> Revision 1.2  2006/04/06 23:34:45  sueh
* <p> bug# 844 Added a color for the generic parallel processing window.
* <p>
* <p> Revision 1.1  2006/03/20 18:05:57  sueh
* <p> bug# 835 Added an AxisProcessPanel for ParallelManager.
* <p> </p>
*/
