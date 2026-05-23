package etomo.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import etomo.ApplicationManager;
import etomo.comscript.ConstTiltParam;
import etomo.comscript.FortranInputSyntaxException;
import etomo.comscript.SplittiltParam;
import etomo.comscript.TiltParam;
import etomo.logic.TomogramTool;
import etomo.storage.LogFile;
import etomo.storage.autodoc.AutodocFactory;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.type.AxisID;
import etomo.type.ConstEtomoNumber;
import etomo.type.ConstMetaData;
import etomo.type.DialogType;
import etomo.type.EtomoNumber;
import etomo.type.MetaData;
import etomo.type.PanelId;
import etomo.type.ProcessResultDisplay;
import etomo.type.ProcessingMethod;
import etomo.type.ReconScreenState;
import etomo.type.Run3dmodMenuOptions;
import etomo.type.TomogramState;
import etomo.type.ViewType;
import etomo.ui.DisplaySettings;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.util.InvalidParameterException;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2008 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.9  2011/06/28 21:49:38  sueh
 * <p> Bug# 1480 Change gpuAvailable to gpusAvailable - non-local GPUs.  Added localGpuAvailable.  Calling
 * <p> updateDisplay from msgMethodChanged so that the display is up to date before the method is
 * <p> changed.  Corrected enabling the GPU checkbox in updateDisplay.
 * <p>
 * <p> Revision 1.8  2011/05/11 01:35:39  sueh
 * <p> bug# 1483 In updateDisplay fixed the boolean value used to call TrialTiltPanel.setResume.
 * <p>
 * <p> Revision 1.7  2011/05/03 03:05:18  sueh
 * <p> bug# 1416 Placed the field change listeners in this class.  Checkpoint from the tilt param (tilt_for_sirt.com).
 * <p> Added fieldChangeAction.  Added overrideable functions isBackProjection and isSirt.
 * <p>
 * <p> Revision 1.6  2011/04/25 23:21:20  sueh
 * <p> bug# 1416 Moved responsibility of when to checkpoint to child class.  Added addStateChangedReporter.
 * <p> Added a numeric type to checkpointed fields that contain a number.
 * <p>
 * <p> Revision 1.5  2011/04/04 17:16:53  sueh
 * <p> bug# 1416 Removed enabled, getEnabled, setEnabled.  Added method, pnlButton, pnlSlicesInY,
 * <p> resume,trialPanel, checkpoint, isChanged, msgTiltComLoaded, msgTiltComSaved, setMethod, setVisible,
 * <p> update.  Modified radialPanel, constructor, action, addListeners, createPanel, done,
 * <p> get3dmodTomogramButton, getParameters, getPRocessingMethod, getTiltButton, initialPanel, setParameters,
 * <p> setTiltButtonTooltip, setToolTipText, updateAdvanced, updateDisplay.
 * <p>
 * <p> Revision 1.4  2011/02/10 04:31:50  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.3  2011/02/03 06:22:16  sueh
 * <p> bug# 1422 Control of the processing method has been centralized in the
 * <p> processing method mediator class.  Implementing ProcessInterface.
 * <p> Supplying processes with the current processing method.
 * <p>
 * <p> Revision 1.2  2010/12/05 04:48:30  sueh
 * <p> bug# 1416 Moved radial filter fields to RadialPanel.  Added setEnabled
 * <p> boolean) to enable/disable all the fields in the panel.  Added booleans for
 * <p> remember the state so enabling all the fields will be down correctly.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:35  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 3.10  2010/05/27 22:09:06  sueh
 * <p> bug# 1377 Added updateUseGPU and updateParallelProcess calls
 * <p> everywhere these checkboxes are modified.
 * <p>
 * <p> Revision 3.9  2010/04/09 03:01:05  sueh
 * <p> bug# 1352 Passing the ProcessResultDisplay via parameter instead of retrieving it with a function so that it always be passed.
 * <p>
 * <p> Revision 3.8  2010/03/27 04:53:59  sueh
 * <p> bug# 1333 Save parallel processing according the panel ID.  Initialize GPU
 * <p> from default GPU.
 * <p>
 * <p> Revision 3.7  2010/03/12 04:09:06  sueh
 * <p> bug# 1325 Changed the logarithm fields.
 * <p>
 * <p> Revision 3.6  2010/03/05 04:11:07  sueh
 * <p> bug# 1319 Changed all SpacedTextField variables to LabeledTextField to
 * <p> line up the fields better.
 * <p>
 * <p> Revision 3.5  2010/03/05 04:01:37  sueh
 * <p> bug# 1319 Convert ltfLogOffset to ctfLog.  Added linear scale for when
 * <p> the log is turned off.
 * <p>
 * <p> Revision 3.4  2010/02/17 05:03:12  sueh
 * <p> bug# 1301 Using manager instead of manager key for popping up messages.
 * <p>
 * <p> Revision 3.3  2010/01/11 23:58:46  sueh
 * <p> bug# 1299 Added GPU checkbox.
 * <p>
 * <p> Revision 3.2  2009/09/22 23:43:07  sueh
 * <p> bug# 1269 Moved setEnabledTiltParameters to abstract tilt panel so it can be use by tilt 3dfind.
 * <p>
 * <p> Revision 3.1  2009/09/01 03:18:25  sueh
 * <p> bug# 1222
 * <p> </p>
 */
abstract class AbstractTiltPanel implements Expandable, TrialTiltParent,
  Run3dmodButtonContainer, TiltDisplay, DisplaySettings, TomogramGenerationMethod {
  private static final String BACK_PROJECTION_HEADER = "Tilt";
  private static final String PARAMETERS_HEADER = "Tilt Parameters";
  private static final String SUPER_SAMPLE_FACTOR_LABEL = "Super-sample by";
  private final SpacedPanel pnlRoot = SpacedPanel.getInstance();
  // Keep components with listeners private.
  private final Run3dmodButton btn3dmodTomogram =
    Run3dmodButton.get3dmodInstance("View Tomogram In 3dmod", this);
  private final ActionListener actionListener = new TiltActionListener(this);
  private final JPanel pnlBody = new JPanel();
  private final LabeledTextField ltfTomoWidth =
    new LabeledTextField(FieldType.INTEGER, "Tomogram width in X: ");
  final LabeledTextField ltfTomoThickness = LabeledTextField
    .getNumericInstance("Tomogram thickness in Z: ", EtomoNumber.Type.INTEGER);
  final LabeledTextField ltfXAxisTilt =
    LabeledTextField.getNumericInstance("X axis tilt: ", EtomoNumber.Type.DOUBLE);
  final LabeledTextField ltfExtraExcludeList =
    new LabeledTextField(FieldType.INTEGER_LIST, "Extra views to exclude: ");
  private final LabeledTextField ltfXShift =
    new LabeledTextField(FieldType.FLOATING_POINT, "X shift: ");
  private final LabeledTextField ltfTomoHeight =
    new LabeledTextField(FieldType.INTEGER, "Tomogram height in Y: ");
  private final LabeledTextField ltfYShift =
    new LabeledTextField(FieldType.INTEGER, " Y shift: ");
  private final RadialPanel radialPanel;
  private final SpacedPanel trialPanel = SpacedPanel.getInstance();
  private final SpacedPanel pnlButton = SpacedPanel.getInstance(true);
  private final JPanel pnlReconWithSuperSampling = new JPanel();
  private final CheckBox cbSuperSampleFactor = new CheckBox(SUPER_SAMPLE_FACTOR_LABEL);
  private final Spinner spSuperSampleFactor =
    Spinner.getInstance(SUPER_SAMPLE_FACTOR_LABEL, 2, 2, 8);
  private final CheckBox cbExpandInputLines = new CheckBox("Super-sample input also");

  // Protected variables can be modified by plugin child classes. Assuming that variables
  // not touched by AbstractTiltPanel and TiltPanel are wrong and fix them in every update
  // function.
  protected final CheckTextField ctfLog =
    CheckTextField.getNumericInstance(FieldType.FLOATING_POINT,
      "Take logarithm of densities with offset: ", EtomoNumber.Type.DOUBLE);
  protected final LabeledTextField ltfTiltAngleOffset =
    LabeledTextField.getNumericInstance("Tilt angle offset: ", EtomoNumber.Type.DOUBLE);
  protected final LabeledTextField ltfLogDensityScaleFactor = LabeledTextField
    .getNumericInstance("Logarithm density scaling factor: ", EtomoNumber.Type.DOUBLE);
  protected final LabeledTextField ltfLogDensityScaleOffset =
    LabeledTextField.getNumericInstance(" Offset: ", EtomoNumber.Type.DOUBLE);
  protected final LabeledTextField ltfLinearDensityScaleFactor = LabeledTextField
    .getNumericInstance("Linear density scaling factor: ", EtomoNumber.Type.DOUBLE);
  protected final LabeledTextField ltfLinearDensityScaleOffset =
    LabeledTextField.getNumericInstance(" Offset: ", EtomoNumber.Type.DOUBLE);
  protected final LabeledTextField ltfZShift =
    LabeledTextField.getNumericInstance(" Z shift: ", EtomoNumber.Type.DOUBLE);
  protected final CheckBox cbUseLocalAlignment = new CheckBox("Use local alignments");
  protected final CheckBox cbUseZFactors = new CheckBox("Use Z factors");

  private final PanelHeader header;
  final ApplicationManager manager;
  final AxisID axisID;
  final DialogType dialogType;
  private final TrialTiltPanel trialTiltPanel;
  // Keep components with listeners private.
  private final Run3dmodButton btnTilt;
  private final MultiLineButton btnDeleteStack;
  private final PanelId panelId;
  final boolean listenForFieldChanges;
  // advancedFieldDisplayer's display function puts the dialog in advanced mode. Add to
  // advanced fields that are validated.
  protected final FieldDisplayer advancedFieldDisplayer;
  private final CpuGpuPanel cpuGpuPanel;
  private boolean madeZFactors = false;
  private boolean newstFiducialessAlignment = false;
  private boolean usedLocalAlignments = false;

  abstract void tiltAction(ProcessResultDisplay processResultDisplay,
    final Deferred3dmodButton deferred3dmodButton,
    final Run3dmodMenuOptions run3dmodMenuOptions, ProcessingMethod tiltProcessingMethod);

  abstract void imodTomogramAction(final Deferred3dmodButton deferred3dmodButton,
    final Run3dmodMenuOptions run3dmodMenuOptions);

  // backward compatibility functionality - if the metadata binning is missing
  // get binning from newst
  AbstractTiltPanel(final ApplicationManager manager, final AxisID axisID,
    final DialogType dialogType, final GlobalExpandButton globalAdvancedButton,
    final PanelId panelId, final boolean listenForFieldChanges) {
    this.manager = manager;
    this.axisID = axisID;
    this.dialogType = dialogType;
    this.panelId = panelId;
    this.listenForFieldChanges = listenForFieldChanges;
    advancedFieldDisplayer = globalAdvancedButton;
    radialPanel = RadialPanel.getInstance(manager, axisID, panelId, this, this);
    // mediator = manager.getProcessingMethodMediator(axisID);
    header = PanelHeader.getAdvancedBasicInstance(BACK_PROJECTION_HEADER, this,
      dialogType, globalAdvancedButton);
    trialTiltPanel = TrialTiltPanel.getInstance(manager, axisID, dialogType, this);
    ProcessResultDisplayFactory displayFactory =
      manager.getProcessResultDisplayFactory(axisID);
    btnTilt = (Run3dmodButton) displayFactory.getTilt(dialogType);
    btnDeleteStack = (MultiLineButton) displayFactory.getDeleteAlignedStack();
    cpuGpuPanel = CpuGpuPanel.getInstance(manager, axisID, panelId, 3, true, -1);
  }

  final void initializePanel() {
    btnTilt.setContainer(this);
    btnTilt.setDeferred3dmodButton(btn3dmodTomogram);
  }

  void createPanel() {
    // Initialize
    setAdvancedFieldDisplayer();
    initializePanel();
    ltfLinearDensityScaleFactor.setText(TiltParam.LINEAR_SCALE_FACTOR_DEFAULT);
    ltfLinearDensityScaleOffset.setText(TiltParam.LINEAR_SCALE_OFFSET_DEFAULT);
    ltfTomoWidth.setPreferredWidth(163);
    ltfTomoHeight.setPreferredWidth(159);
    // local panels
    JPanel pnlLogDensity = new JPanel();
    JPanel pnlLinearDensity = new JPanel();
    JPanel pnlCheckBox = new JPanel();
    JPanel pnlX = new JPanel();
    JPanel pnlY = new JPanel();
    JPanel pnlZ = new JPanel();
    JPanel pnlUseLocalAlignment = new JPanel();
    JPanel pnlUseZFactors = new JPanel();
    JPanel pnlSupersampleBySpinner = new JPanel();
    // Root panel
    pnlRoot.setBoxLayout(BoxLayout.Y_AXIS);
    pnlRoot.setBorder(BorderFactory.createEtchedBorder());
    pnlRoot.add(header);
    pnlRoot.add(pnlBody);
    // Body panel
    pnlBody.setLayout(new BoxLayout(pnlBody, BoxLayout.Y_AXIS));
    pnlBody.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlBody.add(cpuGpuPanel.getComponent());
    pnlBody.add(ctfLog.getRootComponent());
    pnlBody.add(pnlLogDensity);
    pnlBody.add(pnlLinearDensity);
    pnlBody.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlBody.add(pnlX);
    pnlBody.add(Box.createRigidArea(FixedDim.x0_y3));
    pnlBody.add(pnlY);
    pnlBody.add(Box.createRigidArea(FixedDim.x0_y3));
    pnlBody.add(pnlZ);
    pnlBody.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlBody.add(ltfXAxisTilt.getContainer());
    pnlBody.add(ltfTiltAngleOffset.getContainer());
    pnlBody.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlBody.add(pnlReconWithSuperSampling);
    pnlBody.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlBody.add(radialPanel.getRoot());
    pnlBody.add(ltfExtraExcludeList.getContainer());
    pnlBody.add(pnlCheckBox);
    pnlBody.add(trialPanel.getContainer());
    pnlBody.add(pnlButton.getContainer());
    // Log density panel
    pnlLogDensity.setLayout(new BoxLayout(pnlLogDensity, BoxLayout.X_AXIS));
    pnlLogDensity.add(ltfLogDensityScaleFactor.getContainer());
    pnlLogDensity.add(ltfLogDensityScaleOffset.getContainer());
    // Linear density panel
    pnlLinearDensity.setLayout(new BoxLayout(pnlLinearDensity, BoxLayout.X_AXIS));
    pnlLinearDensity.add(ltfLinearDensityScaleFactor.getContainer());
    pnlLinearDensity.add(ltfLinearDensityScaleOffset.getContainer());
    // X panel
    pnlX.setLayout(new BoxLayout(pnlX, BoxLayout.X_AXIS));
    pnlX.add(ltfTomoWidth.getContainer());
    pnlX.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlX.add(ltfXShift.getContainer());
    // Y panel
    pnlY.setLayout(new BoxLayout(pnlY, BoxLayout.X_AXIS));
    pnlY.add(ltfTomoHeight.getContainer());
    pnlY.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlY.add(ltfYShift.getContainer());
    // Z panel
    pnlZ.setLayout(new BoxLayout(pnlZ, BoxLayout.X_AXIS));
    pnlZ.add(ltfTomoThickness.getContainer());
    pnlZ.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlZ.add(ltfZShift.getContainer());
    // Reconstruction with supersampling of pixels panel
    pnlReconWithSuperSampling
      .setLayout(new BoxLayout(pnlReconWithSuperSampling, BoxLayout.Y_AXIS));
    pnlReconWithSuperSampling.setBorder(
      new EtchedBorder("Reconstruction with super-sampling of pixels").getBorder());
    pnlReconWithSuperSampling.add(pnlSupersampleBySpinner);
    // Supersample by spinner panel
    pnlSupersampleBySpinner
      .setLayout(new BoxLayout(pnlSupersampleBySpinner, BoxLayout.X_AXIS));
    pnlSupersampleBySpinner.add(cbSuperSampleFactor.getComponent());
    pnlSupersampleBySpinner.add(spSuperSampleFactor.getComponent());
    pnlSupersampleBySpinner.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlSupersampleBySpinner.add(cbExpandInputLines.getComponent());
    pnlSupersampleBySpinner.add(Box.createHorizontalGlue());
    // Check box panel
    pnlCheckBox.setLayout(new BoxLayout(pnlCheckBox, BoxLayout.Y_AXIS));
    pnlCheckBox.add(pnlUseLocalAlignment);
    pnlCheckBox.add(pnlUseZFactors);
    // UseLocalAlignment
    pnlUseLocalAlignment.setLayout(new BoxLayout(pnlUseLocalAlignment, BoxLayout.X_AXIS));
    pnlUseLocalAlignment.add(cbUseLocalAlignment.getComponent());
    pnlUseLocalAlignment.add(Box.createHorizontalGlue());
    // UseZFactors
    pnlUseZFactors.setLayout(new BoxLayout(pnlUseZFactors, BoxLayout.X_AXIS));
    pnlUseZFactors.add(cbUseZFactors.getComponent());
    pnlUseZFactors.add(Box.createHorizontalGlue());
    // Trial panel
    trialPanel.setBoxLayout(BoxLayout.X_AXIS);
    trialPanel.add(trialTiltPanel.getComponent());
    // Button panel
    pnlButton.setBoxLayout(BoxLayout.X_AXIS);
    pnlButton.add(btnTilt);
    pnlButton.add(btn3dmodTomogram);
    pnlButton.add(btnDeleteStack);
  }

  final SpacedPanel getRootPanel() {
    return pnlRoot;
  }

  private final PanelId getPanelId() {
    return panelId;
  }

  final Component getTiltButton() {
    return btnTilt.getComponent();
  }

  final Component get3dmodTomogramButton() {
    return btn3dmodTomogram.getComponent();
  }

  final Component getCpuGpuPanel() {
    return cpuGpuPanel.getComponent();
  }

  final void setTiltButtonTooltip(String tooltip) {
    btnTilt.setToolTipText(tooltip);
  }

  void addListeners() {
    btnTilt.addActionListener(actionListener);
    btn3dmodTomogram.addActionListener(actionListener);
    btnDeleteStack.addActionListener(actionListener);
    ctfLog.addActionListener(actionListener);
    cbSuperSampleFactor.addActionListener(actionListener);
    // If the child class has field listeners, it must set listenForFieldChanges to true.
    // Otherwise changes in these fields will be ignored.
    if (listenForFieldChanges) {
      cbUseLocalAlignment.addActionListener(actionListener);
      cbUseZFactors.addActionListener(actionListener);
    }
  }

  /**
   * Allow radial panel to react to the multifilt panel which also contains filter
   * selections.
   * @param externalFilterType
   */
  void setFilterTypeActionListener(final MultifiltPanel multifiltPanel) {
    multifiltPanel.addActionListener(radialPanel);
    radialPanel.setMultifiltFilterType(multifiltPanel);
  }

  Component getRoot() {
    return pnlRoot.getContainer();
  }

  @Deprecated // 8/3/2018 See TiltDisplay.
  @SuppressWarnings("deprecation")
  public boolean allowTiltComSave() {
    return true;
  }

  @Override
  public final void expand(final GlobalExpandButton button) {}

  @Override
  public final void expand(final ExpandButton button) {
    if (header != null) {
      if (header.equalsOpenClose(button)) {
        pnlBody.setVisible(button.isExpanded());
      }
      else if (header.equalsAdvancedBasic(button)) {
        updateAdvanced(button.isExpanded());
      }
    }
    UIHarness.INSTANCE.pack(axisID, manager);
  }

  @Override
  public boolean isAdvanced() {
    return header.isAdvanced();
  }

  final void msgMethodChanged() {
    updateAdvanced(isAdvanced());
    updateDisplay();
    radialPanel.msgMethodChanged();
    cpuGpuPanel.msgProcessingMethodChanged(!isBackProjection());
  }

  void updateAdvanced(final boolean advanced) {
    setVisible(advanced);
    radialPanel.updateAdvanced(advanced);
  }

  /**
   * Call fields' and panels' setVisible functions based on advanced and the current
   * method.
   * @param advanced
   */
  protected void setVisible(final boolean advanced) {
    resetToNonpluginState();
    boolean backProjection = isBackProjection();
    boolean multifilt = isMultifilt();
    boolean filterTrials = isMultifilt();
    ltfLogDensityScaleOffset.setVisible(advanced);
    ltfLogDensityScaleFactor.setVisible(advanced);
    ltfLinearDensityScaleOffset.setVisible(advanced);
    ltfLinearDensityScaleFactor.setVisible(advanced);
    ltfTomoWidth.setVisible(advanced && backProjection);
    ltfTomoHeight.setVisible(advanced && backProjection);
    ltfTomoThickness.setVisible(!multifilt);
    ltfYShift.setVisible(advanced && backProjection);
    ltfXShift.setVisible(advanced && backProjection);
    ltfZShift.setVisible(!multifilt);
    pnlReconWithSuperSampling.setVisible(advanced && backProjection);
    cbSuperSampleFactor.setVisible(advanced && backProjection);
    spSuperSampleFactor.setVisible(advanced && backProjection);
    cbExpandInputLines.setVisible(advanced && backProjection);
    ltfTiltAngleOffset.setVisible(advanced);
    radialPanel.setVisible(backProjection || multifilt);
    ltfExtraExcludeList.setVisible(advanced);
    trialPanel.setVisible(backProjection);
    trialTiltPanel.setVisible(advanced);
    pnlButton.setVisible(backProjection);
    cpuGpuPanel.updateDisplay();
  }

  /**
   * Allows advanced fields to be displayed if they get a popup error message.  Only
   * needed for fields that are validated.
   */
  protected void setAdvancedFieldDisplayer() {
    ltfLogDensityScaleOffset.setFieldDisplayer(advancedFieldDisplayer);
    ltfLogDensityScaleFactor.setFieldDisplayer(advancedFieldDisplayer);
    ltfLinearDensityScaleOffset.setFieldDisplayer(advancedFieldDisplayer);
    ltfLinearDensityScaleFactor.setFieldDisplayer(advancedFieldDisplayer);
    ltfTomoWidth.setFieldDisplayer(advancedFieldDisplayer);
    ltfTomoHeight.setFieldDisplayer(advancedFieldDisplayer);
    ltfYShift.setFieldDisplayer(advancedFieldDisplayer);
    ltfXShift.setFieldDisplayer(advancedFieldDisplayer);
    // Z shift is not always an advanced field.
    ltfTiltAngleOffset.setFieldDisplayer(advancedFieldDisplayer);
    ltfExtraExcludeList.setFieldDisplayer(advancedFieldDisplayer);
  }

  /**
   * Normalize protected fields when the plugin in not active.  Do this with any protected
   * fields.  Call this method BEFORE adjusting visibility or enabledness.  It is an
   * extremely blunt instrument.
   */
  private void resetToNonpluginState() {
    if (!isMethodPlugin()) {
      ctfLog.setVisible(true);
      ctfLog.setTextFieldVisible(true);
      ctfLog.switchLabels(false);
      ctfLog.switchTooltips(false);
      ltfTiltAngleOffset.setVisible(true);
      ltfLogDensityScaleFactor.setVisible(true);
      ltfLogDensityScaleOffset.setVisible(true);
      ltfLinearDensityScaleFactor.setVisible(true);
      ltfLinearDensityScaleOffset.setVisible(true);
      ltfZShift.setVisible(true);
      cbUseLocalAlignment.setVisible(true);
      cbUseLocalAlignment.switchTooltips(false);
      cbUseZFactors.setVisible(true);
      cbUseZFactors.switchTooltips(false);
    }
  }

  final void done() {
    btnTilt.removeActionListener(actionListener);
    btnDeleteStack.removeActionListener(actionListener);
    trialTiltPanel.done();
    cpuGpuPanel.done();
  }

  boolean isBackProjection() {
    return true;
  }

  public boolean isMultifilt() {
    return false;
  }

  protected boolean isMethodPlugin() {
    return false;
  }

  protected void updateDisplay() {
    setVisible(header.isAdvanced());
    btn3dmodTomogram.setEnabled(true);
    ctfLog.setEnabled(true);
    ltfTomoWidth.setEnabled(true);
    ltfTomoThickness.setEnabled(true);
    ltfXAxisTilt.setEnabled(true);
    ltfTiltAngleOffset.setEnabled(true);
    ltfExtraExcludeList.setEnabled(true);

    boolean logIsSelected = ctfLog.isSelected();
    ltfLogDensityScaleFactor.setEnabled(logIsSelected);
    ltfLogDensityScaleOffset.setEnabled(logIsSelected);
    ltfLinearDensityScaleFactor.setEnabled(!logIsSelected);
    ltfLinearDensityScaleOffset.setEnabled(!logIsSelected);

    ltfTomoHeight.setEnabled(true);
    ltfYShift.setEnabled(true);
    ltfZShift.setEnabled(true);
    ltfXShift.setEnabled(true);

    cbSuperSampleFactor.setEnabled(true);
    boolean supersampleIsSelected = cbSuperSampleFactor.isSelected();
    spSuperSampleFactor.setEnabled(supersampleIsSelected);
    cbExpandInputLines.setEnabled(supersampleIsSelected);

    radialPanel.setEditable(true);
    cbUseLocalAlignment.setEnabled(usedLocalAlignments && !newstFiducialessAlignment);
    cbUseZFactors.setEnabled(madeZFactors && !newstFiducialessAlignment);
    btnTilt.setEnabled(true);
    btnDeleteStack.setEnabled(true);
    header.setText(isBackProjection() ? BACK_PROJECTION_HEADER : PARAMETERS_HEADER);
  }

  protected final boolean isParallelProcess() {
    return cpuGpuPanel.isParallelProcess();
  }

  ProcessingMethod getRunMethodForProcessInterface() {
    return cpuGpuPanel.getRunMethodForProcessInterface();
  }

  final boolean isZShiftSet() {
    String text = ltfZShift.getText();
    return text.matches("\\S+");
  }

  final boolean isUseLocalAlignment() {
    return cbUseLocalAlignment.isSelected();
  }

  void setState(TomogramState state, ConstMetaData metaData) {
    // madeZFactors
    if (!state.getMadeZFactors(axisID).isNull()) {
      madeZFactors = state.getMadeZFactors(axisID).is();
    }
    else {
      madeZFactors = state.getBackwardCompatibleMadeZFactors(axisID);
    }
    // newstFiducialessAlignment
    if (!state.getNewstFiducialessAlignment(axisID).isNull()) {
      newstFiducialessAlignment = state.getNewstFiducialessAlignment(axisID).is();
    }
    else {
      newstFiducialessAlignment = metaData.isFiducialessAlignment(axisID);
    }
    // usedLocalAlignments
    if (!state.getUsedLocalAlignments(axisID).isNull()) {
      usedLocalAlignments = state.getUsedLocalAlignments(axisID).is();
    }
    else {
      usedLocalAlignments = state.getBackwardCompatibleUsedLocalAlignments(axisID);
    }
    updateDisplay();
  }

  boolean isUseZFactors() {
    return cbUseZFactors.isSelected();
  }

  boolean isCbSuperSampleFactor() {
    return cbSuperSampleFactor.isSelected();
  }

  boolean isCbExpandInputLines() {
    return cbExpandInputLines.isSelected();
  }

  void getParameters(final MetaData metaData) throws FortranInputSyntaxException {
    cpuGpuPanel.getParameters(panelId, metaData);
    trialTiltPanel.getParameters(metaData);
    metaData.setGenLog(axisID, ctfLog.getText());
    metaData.setGenScaleFactorLog(axisID, ltfLogDensityScaleFactor.getText());
    metaData.setGenScaleOffsetLog(axisID, ltfLogDensityScaleOffset.getText());
    metaData.setGenScaleFactorLinear(axisID, ltfLinearDensityScaleFactor.getText());
    metaData.setGenScaleOffsetLinear(axisID, ltfLinearDensityScaleOffset.getText());
    metaData.setSuperSampleFactor(axisID, spSuperSampleFactor.getValue());
    metaData.setExpandInputLines(axisID, cbExpandInputLines.isSelected());
    radialPanel.getParameters(metaData);
  }

  final void getParameters(final ReconScreenState screenState) {
    header.getState(screenState.getTomoGenTiltHeaderState());
    trialTiltPanel.getParameters(screenState);
  }

  final void setParameters(final ConstMetaData metaData) {
    cpuGpuPanel.setParameters(metaData, metaData.getTiltParallel(axisID, panelId));
    trialTiltPanel.setParameters(metaData);
    ctfLog.setText(metaData.getGenLog(axisID));
    ltfLogDensityScaleFactor.setText(metaData.getGenScaleFactorLog(axisID));
    ltfLogDensityScaleOffset.setText(metaData.getGenScaleOffsetLog(axisID));
    if (metaData.isGenScaleFactorLinearSet(axisID)) {
      ltfLinearDensityScaleFactor.setText(metaData.getGenScaleFactorLinear(axisID));
    }
    if (metaData.isGenScaleOffsetLinearSet(axisID)) {
      ltfLinearDensityScaleOffset.setText(metaData.getGenScaleOffsetLinear(axisID));
    }
    spSuperSampleFactor.setValue(metaData.getGenSuperSampleFactor(axisID));
    cbExpandInputLines.setSelected(metaData.getGenExpandInputLines(axisID).is());
    radialPanel.setParameters(metaData);
    updateDisplay();
  }

  /**
   * @deprecated 2/4/2019 No longer implements ProcessInterface
   * @see CpuGpuPanel
   */
  public void disableGpu(final boolean disable) {
    cpuGpuPanel.disableGpu(disable);
    updateDisplay();
  }

  /**
   * @deprecated 2/4/2019 No longer implements ProcessInterface
   * @see CpuGpuPanel
   */
  public void lockProcessingMethod(final boolean lock) {
    cpuGpuPanel.lockProcessingMethod(lock);
    updateDisplay();
  }

  public ProcessingMethod getProcessingMethod() {
    return cpuGpuPanel.getProcessingMethod();
  }

  /**
   * @deprecated 2/4/2019 No longer implements ProcessInterface
   * @see CpuGpuPanel
   */
  public ProcessingMethod getSecondaryProcessingMethod() {
    return null;
  }

  void reregisterProcessingMethodMediator() {
    cpuGpuPanel.reregisterProcessingMethodMediator();
  }

  /**
   * Set the UI parameters with the specified tiltParam values
   * WARNING: be sure the setNewstParam is called first so the binning value for
   * the stack is known.  The thickness, first and last slice, width and x,y,z
   * offsets are scaled so that they are represented to the user in unbinned
   * dimensions.
   * @param tiltParam
   * @param initialize - true when the dialog is first created for the dataset
   */
  void setParameters(final ConstTiltParam tiltParam, boolean initialize) {
    if (tiltParam.hasWidth()) {
      ltfTomoWidth.setText(tiltParam.getWidth());
    }
    if (tiltParam.hasThickness()) {
      ltfTomoThickness.setText(tiltParam.getThickness());
    }
    if (tiltParam.hasXShift()) {
      ltfXShift.setText(tiltParam.getXShift());
    }
    if (tiltParam.hasZShift()) {
      ltfZShift.setText(tiltParam.getZShift());
    }
    if (tiltParam.hasSlice()) {
      int[] yHeightAndShift = TomogramTool.getYHeightAndShift(manager, axisID,
        tiltParam.getIdxSliceStart(), tiltParam.getIdxSliceStop());
      if (yHeightAndShift != null && yHeightAndShift.length == 2) {
        ltfTomoHeight.setText(yHeightAndShift[0]);
        ltfYShift.setText(yHeightAndShift[1]);
      }
    }
    if (tiltParam.hasXAxisTilt()) {
      ltfXAxisTilt.setText(tiltParam.getXAxisTilt());
    }
    if (tiltParam.hasTiltAngleOffset()) {
      ltfTiltAngleOffset.setText(tiltParam.getTiltAngleOffset());
    }
    radialPanel.setParameters(tiltParam);
    ctfLog.setSelected(tiltParam.hasLogOffset());
    boolean log = tiltParam.hasLogOffset();
    // If initialize is true, get defaults from tilt.com
    if (log || initialize) {
      String text = tiltParam.getLogShift();
      if (text != null && !text.matches("\\s*")) {
        ctfLog.setText(text);
      }
    }
    if (!log && initialize) {
      String text = ctfLog.getText();
      if (text == null || text.matches("\\s*")) {
        ctfLog.setText("0.0");
      }
    }
    if ((log || initialize) && tiltParam.hasScale()) {
      if (log) {
        ltfLogDensityScaleOffset.setText(tiltParam.getScaleFLevel());
        ltfLogDensityScaleFactor.setText(tiltParam.getScaleCoeff());
      }
      else {
        // New combination of parameters: !log and initialize
        ltfLogDensityScaleOffset.setText(tiltParam.getScaleFLevel());
        ltfLogDensityScaleFactor
          .setText((Math.round((tiltParam.getScaleCoeff() * 5000.) / 10.) * 10.));
      }
    }
    if (!log && tiltParam.hasScale()) {
      ltfLinearDensityScaleOffset.setText(tiltParam.getScaleFLevel());
      ltfLinearDensityScaleFactor.setText(tiltParam.getScaleCoeff());
    }
    if (initialize && log) {
      EtomoNumber logScale = new EtomoNumber(EtomoNumber.Type.DOUBLE);
      String text = null;
      text = ltfLogDensityScaleFactor.getText();
      logScale.set(text);
      if (!logScale.isNull() && logScale.isValid()) {
        ltfLinearDensityScaleFactor
          .setText(Math.round((logScale.getDouble() / 5000.) * 1000.) / 1000.);
      }
    }
    cbSuperSampleFactor.setSelected(tiltParam.isSuperSampleFactorSet());
    if (cbSuperSampleFactor.isSelected()) {
      spSuperSampleFactor.setValue(tiltParam.getSuperSampleFactor());
      cbExpandInputLines.setSelected(tiltParam.isExpandInputLinesSet());
    }
    cpuGpuPanel.setParameters(tiltParam, initialize);
    if (!initialize) {
      // During initialization the value should coming from setup
      // cbUseGpu.setSelected(tiltParam.isUseGpu());
      // updateUseGpu();
    }
    MetaData metaData = manager.getMetaData();
    cbUseLocalAlignment.setSelected(metaData.getUseLocalAlignments(axisID));
    cbUseZFactors.setSelected(metaData.getUseZFactors(axisID).is());
    ltfExtraExcludeList.setText(tiltParam.getExcludeList2());
    updateDisplay();
  }

  final void setParameters(final ReconScreenState screenState) {
    trialTiltPanel.setParameters(screenState);
    header.setState(screenState.getTomoGenTiltHeaderState());
    btnTilt.setButtonState(screenState.getButtonState(btnTilt.getButtonStateKey()));
    btnDeleteStack
      .setButtonState(screenState.getButtonState(btnDeleteStack.getButtonStateKey()));
  }

  public boolean getParameters(final SplittiltParam param, final boolean doValidation) {
    try {
      ParallelPanel parallelPanel = manager.getMainPanel().getParallelPanel(axisID);
      if (parallelPanel == null) {
        return false;
      }
      ConstEtomoNumber numMachines =
        param.setNumMachines(parallelPanel.getCPUsSelected(doValidation));
      if (!numMachines.isValid()) {
        if (numMachines.equals(0)) {
          UIHarness.INSTANCE.openMessageDialog(manager,
            parallelPanel.getNoCpusSelectedErrorMessage(), "Unable to run splittilt",
            axisID);
          return false;
        }
        else {
          UIHarness.INSTANCE.openMessageDialog(manager,
            parallelPanel.getCPUsSelectedLabel() + " " + numMachines.getInvalidReason(),
            "Unable to run splittilt", axisID);
          return false;
        }
      }
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  public void setDebug(final boolean debug) {
    radialPanel.setDebug(debug);
  }

  /**
   * Get the tilt parameters from the requested axis panel
   */
  public boolean getParameters(final TiltParam tiltParam, final boolean doValidation)
    throws NumberFormatException, InvalidParameterException, IOException {
    try {
      if (!radialPanel.getParameters(tiltParam, doValidation)) {
        return false;
      }
      String badParameter = "";
      try {
        badParameter = "IMAGEBINNED";
        tiltParam.setImageBinned();
        // Do not manage full image size. It is coming from copytomocoms.
        if (ltfTomoWidth.getText().matches("\\S+")) {
          badParameter = ltfTomoWidth.getLabel();
          tiltParam.setWidth(ltfTomoWidth.getText(doValidation));
        }
        else {
          tiltParam.resetWidth();
        }

        // set Z Shift
        if (isZShiftSet()) {
          badParameter = ltfZShift.getLabel();
          tiltParam.setZShift(ltfZShift.getText(doValidation));
        }
        else {
          tiltParam.resetZShift();
        }
        // set X Shift
        if (ltfXShift.getText().matches("\\S+")) {
          badParameter = ltfXShift.getLabel();
          tiltParam.setXShift(Double.parseDouble(ltfXShift.getText(doValidation)));
        }
        else if (isZShiftSet()) {
          tiltParam.setXShift(0);
          ltfXShift.setText(0);
        }
        else {
          tiltParam.resetXShift();
        }

        ConstEtomoNumber startingSlice = TomogramTool.getYStartingSlice(manager, axisID,
          ltfTomoHeight.getText(doValidation), ltfYShift.getText(doValidation),
          ltfTomoHeight.getQuotedLabel(), ltfYShift.getQuotedLabel());
        if (startingSlice == null) {
          return false;
        }
        if (startingSlice.isNull()) {
          tiltParam.resetIdxSlice();
        }
        else {
          ConstEtomoNumber endingSlice =
            TomogramTool.getYEndingSlice(manager, axisID, startingSlice,
              ltfTomoHeight.getText(doValidation), ltfTomoHeight.getQuotedLabel());
          if (endingSlice == null) {
            return false;
          }
          if (endingSlice.isNull()) {
            tiltParam.resetIdxSlice();
          }
          else {
            tiltParam.setIdxSliceStart(startingSlice.getInt());
            tiltParam.setIdxSliceStop(endingSlice.getInt());
          }
        }

        if (ltfTomoThickness.getText().matches("\\S+")) {
          badParameter = ltfTomoThickness.getLabel();
          tiltParam.setThickness(ltfTomoThickness.getText(doValidation));
        }
        else {
          tiltParam.resetThickness();
        }

        if (ltfXAxisTilt.getText().matches("\\S+")) {
          badParameter = ltfXAxisTilt.getLabel();
          tiltParam.setXAxisTilt(ltfXAxisTilt.getText(doValidation));
        }
        else {
          tiltParam.resetXAxisTilt();
        }

        if (ltfTiltAngleOffset.getText().matches("\\S+")) {
          badParameter = ltfTiltAngleOffset.getLabel();
          tiltParam.setTiltAngleOffset(ltfTiltAngleOffset.getText(doValidation));
        }
        else {
          tiltParam.resetTiltAngleOffset();
        }

        if (ltfLogDensityScaleOffset.isEnabled()
          && (ltfLogDensityScaleOffset.getText().matches("\\S+")
            || ltfLogDensityScaleFactor.getText().matches("\\S+"))) {
          badParameter = ltfLogDensityScaleFactor.getLabel();
          tiltParam.setScaleCoeff(
            Double.parseDouble(ltfLogDensityScaleFactor.getText(doValidation)));
          badParameter = ltfLogDensityScaleOffset.getLabel();
          tiltParam.setScaleFLevel(
            Double.parseDouble(ltfLogDensityScaleOffset.getText(doValidation)));
        }
        else if (ltfLinearDensityScaleOffset.isEnabled()
          && (ltfLinearDensityScaleOffset.getText().matches("\\S+")
            || ltfLinearDensityScaleFactor.getText().matches("\\S+"))) {
          badParameter = ltfLinearDensityScaleFactor.getLabel();
          tiltParam.setScaleCoeff(
            Double.parseDouble(ltfLinearDensityScaleFactor.getText(doValidation)));
          badParameter = ltfLinearDensityScaleOffset.getLabel();
          tiltParam.setScaleFLevel(
            Double.parseDouble(ltfLinearDensityScaleOffset.getText(doValidation)));
        }
        else {
          tiltParam.resetScale();
        }

        if (ctfLog.isSelected() && ctfLog.getText().matches("\\S+")) {
          badParameter = ctfLog.getLabel();
          tiltParam.setLogShift(Double.parseDouble(ctfLog.getText(doValidation)));
        }
        else {
          tiltParam.setLogShift(Double.NaN);
        }

        MetaData metaData = manager.getMetaData();
        if (isUseLocalAlignment() && cbUseLocalAlignment.isEnabled()) {
          tiltParam.setLocalAlignFile(
            metaData.getDatasetName() + axisID.getExtension() + "local.xf");
        }
        else {
          tiltParam.setLocalAlignFile("");
        }
        metaData.setUseLocalAlignments(axisID, isUseLocalAlignment());
        // TiltParam.fiducialess is based on whether final alignment was run
        // fiducialess.
        // newstFiducialessAlignment
        boolean newstFiducialessAlignment = false;
        TomogramState state = manager.getState();
        if (!state.getNewstFiducialessAlignment(axisID).isNull()) {
          newstFiducialessAlignment = state.getNewstFiducialessAlignment(axisID).is();
        }
        else {
          newstFiducialessAlignment = metaData.isFiducialessAlignment(axisID);
        }
        tiltParam.setFiducialess(newstFiducialessAlignment);

        tiltParam.setUseZFactors(isUseZFactors() && cbUseZFactors.isEnabled());
        metaData.setUseZFactors(axisID, isUseZFactors());

        if (isCbSuperSampleFactor()) {
          tiltParam.setSuperSampleFactor((int) spSuperSampleFactor.getValue());
          tiltParam.setExpandInputLines(isCbExpandInputLines());
        }
        else {
          tiltParam.resetSuperSampleFactor();
          tiltParam.setExpandInputLines(false);
        }

        tiltParam.setExcludeList2(ltfExtraExcludeList.getText(doValidation));
        badParameter = TiltParam.SUBSETSTART_KEY;
        if (metaData.getViewType() == ViewType.MONTAGE) {
          tiltParam.setMontageSubsetStart();
        }
        else if (!tiltParam.setSubsetStart()) {
          return false;
        }
      }
      catch (NumberFormatException except) {
        String message = badParameter + " " + except.getMessage();
        throw new NumberFormatException(message);
      }
      catch (IOException e) {
        e.printStackTrace();
        throw new IOException(badParameter + ":  " + e.getMessage());
      }
      cpuGpuPanel.getParameters(tiltParam);
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  public final void action(final Run3dmodButton button,
    final Run3dmodMenuOptions run3dmodMenuOptions) {
    action(button.getActionCommand(), button.getDeferred3dmodButton(),
      run3dmodMenuOptions);
  }

  /**
   * Executes the action associated with command.  Deferred3dmodButton is null
   * if it comes from the dialog's ActionListener.  Otherwise is comes from a
   * Run3dmodButton which called action(Run3dmodButton, Run3dmoMenuOptions).  In
   * that case it will be null unless it was set in the Run3dmodButton.
   * @param command
   * @param deferred3dmodButton
   * @param run3dmodMenuOptions
   */
  public final void action(final String command,
    final Deferred3dmodButton deferred3dmodButton,
    final Run3dmodMenuOptions run3dmodMenuOptions) {
    if (command.equals(btnTilt.getActionCommand())) {
      tiltAction(btnTilt, deferred3dmodButton, run3dmodMenuOptions,
        getRunMethodForProcessInterface());
    }
    else if (command.equals(btnDeleteStack.getActionCommand())) {
      manager.deleteIntermediateImageStacks(axisID, btnDeleteStack);
    }
    else if (command.equals(btn3dmodTomogram.getActionCommand())) {
      imodTomogramAction(deferred3dmodButton, run3dmodMenuOptions);
    }
    else {
      updateDisplay();
    }
  }

  /**
   * Initialize the tooltip text for the axis panel objects
   */
  void setToolTipText() {
    ReadOnlyAutodoc autodoc = null;
    try {
      autodoc = AutodocFactory.getInstance(manager, AutodocFactory.TILT, axisID, false);
    }
    catch (FileNotFoundException except) {
      except.printStackTrace();
    }
    catch (IOException except) {
      except.printStackTrace();
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    ltfTomoThickness
      .setToolTipText("Thickness, in unbinned pixels, along the z-axis of the "
        + "reconstructed volume.");
    ltfTomoHeight
      .setToolTipText("This entry specifies the Y extent of the output tomogram, in "
        + "unbinned pixels; the default is the height of the aligned stack.");
    ltfYShift.setToolTipText(
      "Amount to shift the reconstructed region in Y, in unbinned pixels.  "
        + "A positive value will shift the region upward and reconstruct an area lower "
        + "in Y.");
    ltfTomoWidth
      .setToolTipText("This entry specifies the width, in unbinned pixels, of the output "
        + "image; the default is the width of the input image.");
    ltfXShift
      .setToolTipText("Amount, in unbinned pixels, to shift the reconstructed slices in "
        + "X before output.  A positive value will shift the slice to the right, and "
        + "the output will contain the left part of the whole potentially "
        + "reconstructable area.");
    ltfZShift
      .setToolTipText("Amount, in unbinned pixels, to shift the reconstructed slices in "
        + "Z before output.  A positive value will shift the slice upward.");
    ltfXAxisTilt.setToolTipText(TomogramGenerationDialog.X_AXIS_TILT_TOOLTIP);
    ltfTiltAngleOffset.setToolTipText(
      "Offset in degrees to apply to the tilt angles; a positive offset will "
        + "rotate the reconstructed slices counterclockwise.");
    cbSuperSampleFactor.setToolTipText(
      "Compute slices with sub-pixel resolution and scale the result down to reduce "
        + "artifacts from stray lines in Fourier space");
    spSuperSampleFactor.setToolTipText("Factor by which to supersample the reconstructed "
      + "slice; bigger factors take more memory and time");
    cbExpandInputLines.setToolTipText(
      "Expand the input lines in Fourier space by the supersampling factor to reduce loss"
        + " of information from interpolation");
    ltfLogDensityScaleOffset.setToolTipText(
      "Amount to add to reconstructed density values before multiplying by"
        + " the scale factor and outputting the values.");
    ltfLogDensityScaleFactor.setToolTipText(
      "Amount to multiply reconstructed density values by, after adding the "
        + "offset value.");
    ltfLinearDensityScaleOffset.setToolTipText(
      "Amount to add to reconstructed density values before multiplying by"
        + " the scale factor and outputting the values.");
    ltfLinearDensityScaleFactor.setToolTipText(
      "Amount to multiply reconstructed density values by, after adding the "
        + "offset value.");
    ctfLog
      .setToolTipText("This parameter allows one to generate a reconstruction using the "
        + "logarithm of the densities in the input file, with the value "
        + "specified added before taking the logarithm.  If no parameter is "
        + "specified the logarithm of the input data is not taken.");
    cbUseLocalAlignment
      .setToolTipText("Select this checkbox to use local alignments.  You must have "
        + "created the local alignments in the Fine Alignment step");
    btnTilt.setToolTipText("Compute the tomogram from the full aligned stack.  This runs "
      + "the tilt.com script.");
    btn3dmodTomogram.setToolTipText("View the reconstructed volume in 3dmod.");
    btnDeleteStack.setToolTipText("Delete the aligned stack for this axis.  Once the "
      + "tomogram is calculated this intermediate file is not used and "
      + "can be deleted to free up disk space.");
    cbUseZFactors.setToolTipText(
      "Use the file containing factors for adjusting the backprojection position "
        + "in each image as a function of Z height in the output slice (.zfac file).  "
        + "These factors are necessary when input images have been transformed to "
        + "correct for an apparent specimen stretch.  " + "If this box is not checked, "
        + "Z factors in a local alignment file will not be applied.");
    ltfExtraExcludeList
      .setToolTipText("List of views to exclude from the reconstruction, in "
        + "addition to the ones excluded from fine alignment.");
  }

  private static final class TiltActionListener implements ActionListener {
    private final AbstractTiltPanel adaptee;

    private TiltActionListener(final AbstractTiltPanel adaptee) {
      this.adaptee = adaptee;
    }

    public void actionPerformed(final ActionEvent event) {
      adaptee.action(event.getActionCommand(), null, null);
    }
  }
}
