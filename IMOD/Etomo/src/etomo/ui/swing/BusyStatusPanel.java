package etomo.ui.swing;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import etomo.BaseManager;
import etomo.logic.BusyStatusListener;
import etomo.type.AxisID;

/**
 * <p>Description: A panel for the busy status icon.  Works with only one axisID and
 * manager.</p>
 * 
 * <p>Copyright: Copyright 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class BusyStatusPanel implements BusyStatusListener {
  public static final String LABEL = "lb.busy";
  public static final String GUI_LABEL = "lb.gui-busy";
  static final ImageIcon ICON = CompleteIcon.createIcon("busy.png");
  static final ImageIcon GUI_ICON = CompleteIcon.createIcon("etomoBusy.png");

  private final JPanel pnlRoot = new JPanel();

  private final JLabel lBusyStatus = new JLabel(ICON);
  private final JLabel lGuiBusyStatus = new JLabel(GUI_ICON);

  private final AxisID axisID;

  private BusyStatusPanel(final AxisID axisID) {
    this.axisID = axisID;
  }

  static BusyStatusPanel getInstance(final BaseManager manager, final AxisID axisID) {
    BusyStatusPanel instance = new BusyStatusPanel(axisID);
    instance.createPanel();
    instance.addListeners(manager);
    return instance;
  }

  private void createPanel() {
    BorderLayout borderLayout = new BorderLayout();
    borderLayout.setHgap(2);
    lBusyStatus.setName(LABEL);
    lBusyStatus.setEnabled(false);
    lGuiBusyStatus.setName(GUI_LABEL);
    lGuiBusyStatus.setEnabled(false);
    // Root
    pnlRoot.setLayout(borderLayout);
    pnlRoot.add(lBusyStatus, BorderLayout.EAST);
    pnlRoot.add(lGuiBusyStatus, BorderLayout.WEST);
  }

  void addListeners(final BaseManager manager) {
    manager.addBusyStatusListener(this);
  }

  void removeListeners(final BaseManager manager) {
    manager.removeBusyStatusListener(this);
  }

  Component getComponent() {
    return pnlRoot;
  }

  public void msgBusyStatusChanged(final AxisID axisID, final boolean processStatus) {
    if (this.axisID.isSameAxis(axisID)) {
      SwingUtilities.invokeLater(new SetBusyStatus(false, processStatus));
    }
  }

  public void msgGuiBusyStatusChanged(final AxisID axisID, final boolean processStatus) {
    if (this.axisID.isSameAxis(axisID)) {
      SwingUtilities.invokeLater(new SetBusyStatus(true, processStatus));
    }
  }

  private final class SetBusyStatus implements Runnable {
    private final boolean gui;
    final boolean enabled;

    private SetBusyStatus(final boolean gui, final boolean enabled) {
      this.gui = gui;
      this.enabled = enabled;
    }

    public void run() {
      if (gui) {
        lGuiBusyStatus.setEnabled(enabled);
      }
      else {
        lBusyStatus.setEnabled(enabled);
      }
    }
  }
}
