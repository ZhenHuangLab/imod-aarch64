package etomo.plugin.demo;

import etomo.ApplicationManager;
import etomo.type.AxisID;
import etomo.type.DialogType;
import etomo.type.PanelId;
import etomo.type.ProcessingMethod;
import etomo.ui.swing.GlobalExpandButton;
import etomo.ui.swing.TiltPanel;
import etomo.ui.swing.TomogramGenerationDialog;

/**
 * <p>Description: Inherit TiltPanel to modify field behavior and appearance.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class DemoTiltPanel extends TiltPanel {
  private DemoTiltPanel(final ApplicationManager manager, final AxisID axisID,
    final DialogType dialogType, final GlobalExpandButton globalAdvancedButton,
    final PanelId panelId, final TomogramGenerationDialog parent) {
    super(manager, axisID, dialogType, globalAdvancedButton, panelId, parent);
  }

  static TiltPanel getInstance(final ApplicationManager manager, final AxisID axisID,
    final DialogType dialogType, final GlobalExpandButton globalAdvancedButton,
    final TomogramGenerationDialog parent) {
    DemoTiltPanel instance = new DemoTiltPanel(manager, axisID, dialogType,
      globalAdvancedButton, PanelId.TILT, parent);
    instance.init();
    instance.createPanel();
    instance.setToolTipText();
    instance.setDemoToolTipText();
    instance.addListeners();
    return instance;
  }

  private void init() {
    ctfLog.setAlternateLabel(
      "Take logarithm of densities with maximum density value as the offset");
  }

  /**
   * This demo does do parallel processing.
   */
  public ProcessingMethod getProcessingMethod() {
    if (isMethodPlugin()) {
      return ProcessingMethod.LOCAL_CPU;
    }
    return super.getProcessingMethod();
  }

  protected void setVisible(final boolean advanced) {
    super.setVisible(advanced);
    if (isMethodPlugin()) {
      ltfLogDensityScaleOffset.setVisible(false);
      ltfLogDensityScaleFactor.setVisible(false);
      ltfLinearDensityScaleOffset.setVisible(false);
      ltfLinearDensityScaleFactor.setVisible(false);
      ltfTiltAngleOffset.setVisible(false);
      ltfZShift.setVisible(false);
      ctfLog.setTextFieldVisible(false);
    }
  }

  protected void updateAdvanced(final boolean advanced) {
    super.updateAdvanced(advanced);
    if (isMethodPlugin()) {
      ltfZShift.setVisible(false);
    }
  }

  protected void updateDisplay() {
    super.updateDisplay();
    boolean demo = isMethodPlugin();
    if (demo) {
      cbUseLocalAlignment.setEnabled(false);
      cbUseZFactors.setEnabled(false);
    }
    cbUseZFactors.switchTooltips(demo);
    cbUseLocalAlignment.switchTooltips(demo);
    ctfLog.switchLabels(demo);
    ctfLog.switchTooltips(demo);
  }

  private void setDemoToolTipText() {
    cbUseZFactors.setAlternateTooltipText("Demo currently does not support Z factors.");
    cbUseLocalAlignment
      .setAlternateTooltipText("Demo currently does not support local alignments.");
    ctfLog.setAlternateTooltipText(
      "Take logarithm of densities with maximum density value as an offset.");
  }
}