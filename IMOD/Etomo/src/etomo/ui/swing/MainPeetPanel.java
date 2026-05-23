package etomo.ui.swing;

import etomo.BaseManager;
import etomo.process.ProcessState;
import etomo.storage.DataFileFilter;
import etomo.storage.PeetFileFilter;
import etomo.type.AxisID;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2006</p>
 *
 * <p>Organization:
 * Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
 * University of Colorado</p>
 * 
 * @author $Author$
 * 
 * @version $Revision$
 * 
 * <p> $Log$
 * <p> Revision 1.3  2009/02/04 23:36:48  sueh
 * <p> bug# 1158 Changed id and exception classes in LogFile.
 * <p>
 * <p> Revision 1.2  2007/02/21 04:22:33  sueh
 * <p> bug# 964 In getDataFileFilter, returning PeetFileFilter.
 * <p>
 * <p> Revision 1.1  2007/02/19 22:02:49  sueh
 * <p> bug# 964 Main panel for PEET interface.
 * <p> </p>
 */
public final class MainPeetPanel extends MainPanel {
  public static final String rcsid =
    "$Id$";

  private PeetProcessPanel axisPanelA = null;

  public MainPeetPanel(BaseManager manager) {
    super(manager);
  }

  void addAxisPanelA() {
    getScrollA().add(axisPanelA.getContainer());
  }

  void addAxisPanelB() {}

  boolean isAxisPanelANull() {
    return axisPanelA == null;
  }

  boolean isAxisPanelBNull() {
    return true;
  }

  void createAxisPanelA(AxisID axisID, final AxisProgressPanel axisProgressPanel) {
    axisPanelA = new PeetProcessPanel(manager, axisProgressPanel);
  }

  void createAxisPanelB(final AxisProgressPanel axisProgressPanel) {}

  AxisProcessPanel getAxisPanelA() {
    return axisPanelA;
  }

  AxisProcessPanel getAxisPanelB() {
    return null;
  }

  DataFileFilter getDataFileFilter() {
    return new PeetFileFilter();
  }

  boolean hideAxisPanelA() {
    return axisPanelA.hide();
  }

  boolean hideAxisPanelB() {
    return true;
  }

  AxisProcessPanel mapBaseAxisProcessPanel(AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return null;
    }
    return axisPanelA;
  }

  AxisProgressPanel mapAxisProgressPanel(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      return null;
    }
    return getProgressPanel(axisID);
  }

  void resetAxisPanels() {
    axisPanelA = null;
  }

  void saveDisplayState() {}

  void setState(ProcessState processState, AxisID axisID,
    AbstractParallelDialog parallelDialog) {}

  void showAxisPanelA() {
    axisPanelA.show();
  }

  void showAxisPanelB() {}
}
