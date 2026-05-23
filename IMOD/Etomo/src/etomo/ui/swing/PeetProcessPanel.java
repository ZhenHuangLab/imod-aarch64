package etomo.ui.swing;

import etomo.BaseManager;
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
* 
* <p> $Log$
* <p> Revision 1.1  2010/11/13 16:07:35  sueh
* <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
* <p>
* <p> Revision 1.3  2010/07/02 03:19:33  sueh
* <p> bug# 1388 Constructing super class with popupChunkWarnings equal to false.
* <p>
* <p> Revision 1.2  2007/02/19 22:14:55  sueh
* <p> bug# 964 Overriding createProcessControlPanel
* <p>
* <p> Revision 1.1  2007/02/19 22:03:37  sueh
* <p> bug# 964 Process panel for PEET interface.
* <p> </p>
*/
class PeetProcessPanel extends AxisProcessPanel {
  PeetProcessPanel(BaseManager manager, final AxisProgressPanel axisProgressPanel) {
    super(AxisID.ONLY, manager, false, true, InterfaceType.PEET, false,
      axisProgressPanel);
    createProcessControlPanel();
    initializePanels();
  }
}
