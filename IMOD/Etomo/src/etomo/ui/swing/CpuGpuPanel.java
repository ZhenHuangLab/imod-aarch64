package etomo.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import etomo.BaseManager;
import etomo.ProcessingMethodMediator;
import etomo.comscript.ConstCtfPhaseFlipParam;
import etomo.comscript.ConstTiltParam;
import etomo.comscript.CtfPhaseFlipParam;
import etomo.comscript.FortranInputSyntaxException;
import etomo.comscript.TiltParam;
import etomo.storage.CpuAdoc;
import etomo.storage.Network;
import etomo.type.AxisID;
import etomo.type.ConstEtomoNumber;
import etomo.type.ConstMetaData;
import etomo.type.MetaData;
import etomo.type.PanelId;
import etomo.type.ProcessingMethod;

final class CpuGpuPanel implements ActionListener, ProcessInterface {
  private final JPanel pnlRoot = new JPanel();
  /**
   * cbParallelProcess: Call mediator.msgChangedMethod when
   * cbParallelProcess's value is changed.
   */
  private final CheckBox cbParallelProcess = new CheckBox(ParallelPanel.FIELD_LABEL);
  private final BaseManager manager;
  private final AxisID axisID;

  /**
   * cbUseGpu: Enable/disable cbUseGpu by changing gpuEnabled and calling
   * updateDisplay.  Call mediator.msgChangedMethod when cbUseGpu's value is
   * changed.
   */
  private final CheckBox cbUseGpu;
  private final ProcessingMethodMediator mediator;

  private boolean alwaysParallel = false;
  private boolean processingMethodLocked = false;
  private boolean gpusAvailable = false;
  private boolean nonLocalHostGpusAvailable = false;
  private boolean localGpuAvailable = true;
  private boolean gpuEnabled = true;

  /**
   * @param manageMediator - gets and manages its own mediator if true.
   * @param maxGPU - max recommended GPU - use -1 for no recommendation
   */
  private CpuGpuPanel(final BaseManager manager, final AxisID axisID, final int maxGpu) {
    this.manager = manager;
    this.axisID = axisID;
    cbUseGpu = new CheckBox("Use the GPU"
      + (maxGpu > 0 ? ": Maximum number of GPUs recommended is " + maxGpu : ""));
    mediator = manager.getProcessingMethodMediator(axisID);
    mediator.register(this);
  }

  /**
   * 
   * @param manager
   * @param axisID
   * @param panelId
   * @param maxGpu - optional -1 to ignore - displays as recommended max GPUs
   * @param useMaxCpu - when true displays recommended max CPUs
   * @param boxLayoutAxis - optional -1 to ignore - BoxLayout.Y_AXIS (default) or .X_AXIS
   * @return
   */
  static CpuGpuPanel getInstance(final BaseManager manager, final AxisID axisID,
    final PanelId panelId, final int maxGpu, final boolean useMaxCpu,
    final int boxLayoutAxis) {
    CpuGpuPanel instance = new CpuGpuPanel(manager, axisID, maxGpu);
    instance.createPanel(boxLayoutAxis);
    instance.setTooltips();
    instance.init(panelId, useMaxCpu);
    instance.addListeners();
    return instance;
  }

  /**
   * 
   * @param boxLayoutAxis - default is Y_AXIS
   */
  private void createPanel(int boxLayoutAxis) {
    // fixup params
    if (boxLayoutAxis != BoxLayout.X_AXIS) {
      boxLayoutAxis = BoxLayout.Y_AXIS;// default
    }
    // local panels
    JPanel pnlParallelProcess = new JPanel();
    JPanel pnlUseGpu = new JPanel();
    // Root
    pnlRoot.setLayout(new BoxLayout(pnlRoot, boxLayoutAxis));
    pnlRoot.add(pnlParallelProcess);
    pnlRoot.add(pnlUseGpu);
    // ParallelProcess
    pnlParallelProcess.setLayout(new BoxLayout(pnlParallelProcess, BoxLayout.X_AXIS));
    pnlParallelProcess.add(cbParallelProcess.getComponent());
    pnlParallelProcess.add(Box.createHorizontalGlue());
    // UseGpu
    pnlUseGpu.setLayout(new BoxLayout(pnlUseGpu, BoxLayout.X_AXIS));
    pnlUseGpu.add(cbUseGpu.getComponent());
    pnlUseGpu.add(Box.createHorizontalGlue());
  }

  private void init(final PanelId panelId, final boolean useMaxCpu) {
    if ((panelId == PanelId.TILT || panelId == PanelId.TILT_3D_FIND) && useMaxCpu) {
      ConstEtomoNumber maxCPUs = CpuAdoc.INSTANCE.getMaxTilt();
      if (maxCPUs != null && !maxCPUs.isNull()) {
        cbParallelProcess.setText(
          ParallelPanel.FIELD_LABEL + ParallelPanel.MAX_CPUS_STRING + maxCPUs.toString());
      }
    }
    // Parallel processing is optional in tomogram reconstruction, so only use it
    // if the user set it up.
    // Use GPU
    gpusAvailable = Network.isNonLocalOnlyGpuProcessingEnabled();
    nonLocalHostGpusAvailable = Network.isNonLocalHostGpuProcessingEnabled(manager,
      axisID, manager.getPropertyUserDir());
    localGpuAvailable = Network.isLocalHostGpuProcessingEnabled(manager, axisID,
      manager.getPropertyUserDir());
    updateDisplay();
    mediator.setMethod(this, getProcessingMethod());
  }

  void reregisterProcessingMethodMediator() {
    mediator.register(this);
    mediator.setMethod(this, getProcessingMethod());
  }

  boolean isParallelProcess() {
    return cbParallelProcess.isEnabled() && cbParallelProcess.isSelected();
  }

  final void setParameters(final ConstMetaData metaData,
    final ConstEtomoNumber parallelProcess) {
    // Parallel processing is optional in tomogram reconstruction, so only use it
    // if the user set it up.
    boolean validAutodoc =
      Network.isParallelProcessingEnabled(manager, axisID, manager.getPropertyUserDir());
    cbUseGpu.setSelected(metaData.isDefaultGpuProcessing());
    // updateUseGpu();
    // Parallel processing
    cbParallelProcess.setEnabled(validAutodoc);
    // If only a local GPU is available and the Use GPU checkbox defaults to on, do not
    // select the parallel processing checkbox.
    if (nonLocalHostGpusAvailable || !cbUseGpu.isSelected()) {
      if (parallelProcess == null) {
        cbParallelProcess.setSelected(validAutodoc && metaData.isDefaultParallel());
      }
      else {
        cbParallelProcess.setSelected(validAutodoc && parallelProcess.is());
      }
    }
    updateDisplay();
    mediator.setMethod(this, getProcessingMethod());
  }

  void getParameters(final PanelId panelId, final MetaData metaData)
    throws FortranInputSyntaxException {
    metaData.setTiltParallel(axisID, panelId, isParallelProcess());
  }

  /**
   * @param tiltParam
   * @param initialize - true when the dialog is first created for the dataset
   */
  void setParameters(final ConstTiltParam tiltParam, boolean initialize) {
    if (!initialize) {
      // During initialization the value should coming from setup
      cbUseGpu.setSelected(tiltParam.isUseGpu());
    }
    updateDisplay();
    mediator.setMethod(this, getProcessingMethod());
  }

  void getParameters(final TiltParam tiltParam) {
    tiltParam.setUseGpu(cbUseGpu.isEnabled() && cbUseGpu.isSelected());
  }

  void getParameters(final CtfPhaseFlipParam param) {
    param.setUseGpu(cbUseGpu.isEnabled() && cbUseGpu.isSelected());
  }
  /**
   * @param tiltParam
   * @param initialize - true when the dialog is first created for the dataset
   */
  void setParameters(final ConstCtfPhaseFlipParam param, boolean initialize) {
    if (!initialize) {
      // During initialization the value should coming from setup
      cbUseGpu.setSelected(param.isUseGpu());
    }
    updateDisplay();
    mediator.setMethod(this, getProcessingMethod());
  }

  void addListeners() {
    cbParallelProcess.addActionListener(this);
    cbUseGpu.addActionListener(this);
  }

  Component getComponent() {
    return pnlRoot;
  }

  public void actionPerformed(final ActionEvent event) {
    String actionCommand = event.getActionCommand();
    // If there is a only a local GPU available, turn off parallel processing when Use
    // GPU is selected.
    if (cbParallelProcess.getActionCommand().equals(actionCommand)) {
      if (cbParallelProcess.isSelected() && !nonLocalHostGpusAvailable
        && cbUseGpu.isSelected()) {
        cbUseGpu.setSelected(false);
      }
    }
    else if (cbUseGpu.getActionCommand().equals(actionCommand)) {
      if (cbUseGpu.isSelected() && !nonLocalHostGpusAvailable
        && cbParallelProcess.isSelected()) {
        cbParallelProcess.setSelected(false);
      }
    }
    updateDisplay();
    mediator.setMethod(this, getProcessingMethod());
  }

  ProcessingMethod getRunMethodForProcessInterface() {
    return mediator.getRunMethodForProcessInterface(getProcessingMethod());
  }

  void msgProcessingMethodChanged(final boolean alwaysParallel) {
    this.alwaysParallel = alwaysParallel;
    updateDisplay();
    mediator.setMethod(this, getProcessingMethod());
  }

  final void done() {
    mediator.deregister(this);
  }

  public void lockProcessingMethod(final boolean lock) {
    processingMethodLocked = lock;
    updateDisplay();
  }

  public void disableGpu(final boolean disable) {
    gpuEnabled = !disable;
    updateDisplay();
  }

  void updateDisplay() {
    cbParallelProcess.setVisible(!alwaysParallel);
    cbParallelProcess.setEnabled(!processingMethodLocked);
    // A local GPU installed on this computer means that GPU processing is available
    // with or without parallel processing. Non-local GPU(s) installed in the network mean
    // that GPU processing is available with parallel processing.
    cbUseGpu.setEnabled((((gpusAvailable || localGpuAvailable)
      && (cbParallelProcess.isSelected() || alwaysParallel))
      || (localGpuAvailable && !cbParallelProcess.isSelected() && !alwaysParallel))
      && gpuEnabled && !processingMethodLocked);
  }

  public ProcessingMethod getProcessingMethod() {
    boolean parallelProcess =
      alwaysParallel || (cbParallelProcess.isEnabled() && cbParallelProcess.isSelected());
    if (parallelProcess) {
      if (cbUseGpu.isEnabled() && cbUseGpu.isSelected()) {
        return ProcessingMethod.PP_GPU;
      }
      return ProcessingMethod.PP_CPU;
    }
    if (cbUseGpu.isEnabled() && cbUseGpu.isSelected()) {
      return ProcessingMethod.LOCAL_GPU;
    }
    return ProcessingMethod.LOCAL_CPU;
  }

  public ProcessingMethod getSecondaryProcessingMethod() {
    return null;
  }

  private void setTooltips() {
    cbParallelProcess
      .setToolTipText("Check to distribute the process across multiple computers.");
    cbUseGpu.setToolTipText("Check to run the process on one or more graphics cards.");
  }
}
