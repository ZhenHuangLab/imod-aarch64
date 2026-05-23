package etomo.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import etomo.BaseManager;
import etomo.logic.BusyStatusMediator;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.ProcessEndState;
import etomo.type.ProcessName;

/**
 * <p>Description: Holds the progress panel and the kill button</p>
 *
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */

public final class AxisProgressPanel implements ActionListener {
  public static final String KILL_BUTTON_LABEL = "Kill Process";

  private final JPanel pnlRoot = new JPanel();
  private final SimpleButton buttonKillProcess = new SimpleButton(KILL_BUTTON_LABEL);

  private final BaseManager manager;
  private final ProgressPanel progressPanel;
  private final BusyStatusMediator busyStatusMediator;

  private AxisID axisID = AxisID.ONLY;

  private boolean processCanBeKilled = true;

  private AxisProgressPanel(final AxisID axisID, final BaseManager manager) {
    if (axisID != null) {
      this.axisID = axisID;
    }
    this.manager = manager;
    progressPanel = ProgressPanel.getInstance("No process", manager, axisID);
    busyStatusMediator = manager.getBusyStatusMediator();
  }

  static AxisProgressPanel getInstance(final AxisID axisID, final BaseManager manager) {
    AxisProgressPanel instance = new AxisProgressPanel(axisID, manager);
    instance.createPanel();
    instance.setTooltips();
    instance.addListeners();
    return instance;
  }

  public void correctAxisID(final AxisType axisType) {
    if (axisID == AxisID.SECOND) {
      // Always correct.
      return;
    }
    if (axisType == AxisType.DUAL_AXIS) {
      axisID = AxisID.FIRST;
    }
    else {
      axisID = AxisID.ONLY;
    }
  }

  boolean isStopped() {
    return progressPanel.isStopped();
  }

  private void createPanel() {
    // init
    buttonKillProcess.setEnabled(false);
    busyStatusMediator.msgKillProcessButton(axisID, false);
    buttonKillProcess.setAlignmentY(Component.BOTTOM_ALIGNMENT);
    // root
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.X_AXIS));
    pnlRoot.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlRoot.add(progressPanel.getContainer());
    pnlRoot.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlRoot.add(buttonKillProcess);
    pnlRoot.add(Box.createRigidArea(FixedDim.x0_y5));
  }

  Component getComponent() {
    return pnlRoot;
  }

  public void setVisible(final boolean input) {
    pnlRoot.setVisible(input);
  }

  private void setTooltips() {
    buttonKillProcess.setToolTipText("Press to end the current process.");
  }

  private void addListeners() {
    buttonKillProcess.addActionListener(this);
  }

  void setBackground(final Color color) {
    pnlRoot.setBackground(color);
    progressPanel.setBackground(color);
  }

  public void actionPerformed(final ActionEvent event) {
    manager.kill(axisID);
  }

  void setProcessCanBeKilled(final boolean input) {
    processCanBeKilled = input;
  }

  private void setButtonKillProcessEnabled(final boolean enabled) {
    if (enabled) {
      busyStatusMediator.msgKillProcessButton(axisID, enabled);
    }
    buttonKillProcess.setEnabled(enabled);
    if (!enabled) {
      busyStatusMediator.msgKillProcessButton(axisID, enabled);
    }
  }

  /**
   * Setup the progress bar for a determinate
   * @param label
   * @param nSteps
   */
  void setProgressBar(final String label, final int nSteps) {
    progressPanel.setLabel(label);
    progressPanel.setMinimum(0);
    progressPanel.setMaximum(nSteps);
    setButtonKillProcessEnabled(processCanBeKilled && true);
  }

  /**
   * Setup the progress bar for a state, not a process (with no moving bar, or percentage,
   * and without the kill button being enabled.
   * @param label
   * @param nSteps
   */
  void setStaticProgressBar(final String label) {
    progressPanel.setLabel(label);
  }

  void setProgressBarValue(final int n) {
    progressPanel.setValue(n);
  }

  void setProgressBarValue(final int n, final String string) {
    progressPanel.setValue(n, string);
  }

  void startProgressBar(final String label, final ProcessName processName) {
    progressPanel.setLabel(label);
    progressPanel.start();
    setButtonKillProcessEnabled(processCanBeKilled && true);
  }

  void stopProgressBar(final ProcessEndState processEndState, final String statusString) {
    progressPanel.stop(processEndState, statusString);
    processCanBeKilled = true;
    setButtonKillProcessEnabled(false);
  }
}