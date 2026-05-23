package etomo.ui.swing;

import etomo.DirectiveEditorManager;
import etomo.process.ProcessState;
import etomo.storage.DataFileFilter;
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
public final class MainDirectiveEditorPanel extends MainPanel {
  private final DirectiveEditorManager manager;

  private DirectiveEditorProcessPanel axisPanelA = null;

  public MainDirectiveEditorPanel(final DirectiveEditorManager manager) {
    super(manager);
    this.manager = manager;
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
    axisPanelA = new DirectiveEditorProcessPanel(manager, InterfaceType.DIRECTIVE_EDITOR,
      axisProgressPanel);
  }

  void createAxisPanelB(final AxisProgressPanel axisProgressPanel) {}

  AxisProcessPanel getAxisPanelA() {
    return axisPanelA;
  }

  AxisProcessPanel getAxisPanelB() {
    return null;
  }

  DataFileFilter getDataFileFilter() {
    return null;
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

  public void saveDisplayState() {}

  public void setState(ProcessState processState, AxisID axisID,
    AbstractParallelDialog parallelDialog) {}

  void showAxisPanelA() {
    axisPanelA.show();
  }

  void showAxisPanelB() {}

  public final void setStatusBarText(final String directory, final int maxTitleLength) {
    super.setStatusBarTextToDirectory(directory, maxTitleLength);
  }
}
