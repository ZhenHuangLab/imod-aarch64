package etomo.ui.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import etomo.ApplicationManager;
import etomo.EtomoDirector;
import etomo.ProcessingMethodMediator;
import etomo.comscript.BlendmontParam;
import etomo.comscript.ConstCCDEraserParam;
import etomo.comscript.ConstCtfPhaseFlipParam;
import etomo.comscript.ConstFindBeads3dParam;
import etomo.comscript.ConstMTFFilterParam;
import etomo.comscript.ConstNewstParam;
import etomo.comscript.ConstTiltParam;
import etomo.comscript.ConstTiltalignParam;
import etomo.comscript.CtfPhaseFlipParam;
import etomo.comscript.CtfPlotterParam;
import etomo.comscript.FortranInputSyntaxException;
import etomo.comscript.MTFFilterParam;
import etomo.comscript.NewstParam;
import etomo.storage.DoseWeightingFileFilter;
import etomo.storage.LogFile;
import etomo.storage.MtfFileFilter;
import etomo.storage.autodoc.AutodocFactory;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.storage.autodoc.ReadOnlySection;
import etomo.type.AxisID;
import etomo.type.ConstEtomoNumber;
import etomo.type.ConstMetaData;
import etomo.type.DialogType;
import etomo.type.EnumeratedType;
import etomo.type.EtomoAutodoc;
import etomo.type.EtomoNumber;
import etomo.type.FileType;
import etomo.type.ImageFilenameStyle;
import etomo.type.MetaData;
import etomo.type.PanelHeaderState;
import etomo.type.PanelId;
import etomo.type.ProcessName;
import etomo.type.ProcessingMethod;
import etomo.type.ReconScreenState;
import etomo.type.Run3dmodMenuOptions;
import etomo.type.TomogramState;
import etomo.type.ViewType;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.util.DatasetFiles;
import etomo.util.Utilities;

/**
 * <p>Description: Main dialog for the final aligned stack.</p>
 * 
 * <p>Copyright: Copyright 2008 - 2022 by the Regents of the University of Colorado</p>
 *
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 * 
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.8  2011/04/04 17:19:43  sueh
 * <p> Refactored.
 * <p>
 * <p> Revision 1.7  2011/03/02 00:00:12  sueh
 * <p> bug# 1452 Removing image rotation conversion between float and
 * <p> double.  Using string where possible.
 * <p>
 * <p> Revision 1.5  2011/02/24 23:37:14  sueh
 * <p> bug# 1452 imageRotation needs to be double everywhere.
 * <p>
 * <p> Revision 1.4  2011/02/22 18:10:05  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.3  2011/02/03 06:22:16  sueh
 * <p> bug# 1422 Control of the processing method has been centralized in the
 * <p> processing method mediator class.  Implementing ProcessInterface.
 * <p> Supplying processes with the current processing method.
 * <p>
 * <p> Revision 1.2  2010/12/05 05:03:54  sueh
 * <p> bug# 1420 Moved ProcessResultDisplayFactory to etomo.ui.swing package.  Removed static button construction functions.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.30  2010/10/11 20:38:44  sueh
 * <p> bug# 1379 Removed erase beads items from pop menu.  Varying the
 * <p> menu based on the tab.
 * <p>
 * <p> Revision 1.29  2010/04/28 16:38:06  sueh
 * <p> bug# 1344 Removed the manager from the some of the
 * <p> openMessageDialog calls so that they won't go into the project log.
 * <p>
 * <p> Revision 1.28  2010/03/27 05:02:56  sueh
 * <p> Reformatted
 * <p>
 * <p> Revision 1.27  2010/03/19 02:39:45  sueh
 * <p> bug# 1325 Added setParameters(ConstTiltalignParam,boolean).
 * <p>
 * <p> Revision 1.26  2010/03/12 04:14:29  sueh
 * <p> bug# 1325 Made CTF and 2D filtering labels public.
 * <p>
 * <p> Revision 1.25  2010/02/17 05:03:12  sueh
 * <p> bug# 1301 Using manager instead of manager key for popping up messages.
 * <p>
 * <p> Revision 1.24  2010/01/11 23:59:00  sueh
 * <p> bug# 1299 Removed responsibility anything other then cpu.adoc from
 * <p> CpuAdoc.  Placed responsibility for information about the network in the
 * <p> Network class.
 * <p>
 * <p> Revision 1.23  2009/12/19 01:14:34  sueh
 * <p> bug# 1294 Fixed broken right click popup menu.
 * <p>
 * <p> Revision 1.22  2009/11/20 17:11:47  sueh
 * <p> bug# 1282 Naming all the file choosers by constructing a FileChooser
 * <p> instance instead of a JFileChooser instance.  Added isMenuSaveEnabled to
 * <p> allow a save function to have the same limits as the save menu option.
 * <p>
 * <p> Revision 1.21  2009/10/19 21:07:03  sueh
 * <p> bug# 1263 Calling updateParallelProcess from changeTab.  In
 * <p> usingParallelProcessing take the current tab into account.
 * <p>
 * <p> Revision 1.20  2009/10/19 16:28:56  sueh
 * <p> bug# 1253 Added invertTiltAngles.
 * <p>
 * <p> Revision 1.19  2009/09/22 23:54:54  sueh
 * <p> bug# 1269 Added setEnabledTiltParameters.
 * <p>
 * <p> Revision 1.18  2009/09/01 03:18:25  sueh
 * <p> bug# 1222
 * <p>
 * <p> Revision 1.17  2009/06/16 22:53:53  sueh
 * <p> bug# 1221 Factored out newst and ccderaser.  Move some of the expert
 * <p> functionality back into the dialog because it doesn't seem to be compatible
 * <p> with process panels.
 * <p>
 * <p> Revision 1.16  2009/06/12 19:49:14  sueh
 * <p> bug# 1221 Factored running newst, making it independent of the
 * <p> final aligned dialog and expert.
 * <p>
 * <p> Revision 1.15  2009/06/10 22:16:55  sueh
 * <p> bug# 1221 Factoring Newstack and blendmont into NewstackPanel.
 * <p>
 * <p> Revision 1.14  2009/03/17 00:46:24  sueh
 * <p> bug# 1186 Pass managerKey to everything that pops up a dialog.
 * <p>
 * <p> Revision 1.13  2009/02/04 23:36:48  sueh
 * <p> bug# 1158 Changed id and exception classes in LogFile.
 * <p>
 * <p> Revision 1.12  2009/01/26 23:10:57  sueh
 * <p> bug# 1173 Correct tag for tomogram doc
 * <p>
 * <p> Revision 1.11  2009/01/26 22:42:55  sueh
 * <p> bug# 1173 Added mouse listener to tabbed panel.  Added tooltips.
 * <p>
 * <p> Revision 1.10  2009/01/20 20:04:05  sueh
 * <p> bug# 1102 Changed labeled panels to type EtomoPanel so that they can name themselves.  Changed JButtons to SimpleButtons to they they can
 * <p> name themselves.
 * <p>
 * <p> Revision 1.9  2008/12/02 21:39:10  sueh
 * <p> bug# 1157 Added "(pixels)" to the final stack fiducial diameter.
 * <p>
 * <p> Revision 1.8  2008/12/02 21:21:51  sueh
 * <p> bug# 1157 Changed better radius to fiducial diameter.
 * <p>
 * <p> Revision 1.7  2008/11/20 01:44:06  sueh
 * <p> bug# 1147 Added Erase Gold tab.  Added polynomialOrder and
 * <p> betterRadius.  Added buttons to run xfmodel and ccderaser.  Bug# 1153
 * <p> Removed listeners on dialog done from buttons that are created by the
 * <p> display result factory.
 * <p>
 * <p> Revision 1.6  2008/11/11 23:51:07  sueh
 * <p> bug# 1149 Changed the tab names.
 * <p>
 * <p> Revision 1.5  2008/11/11 00:18:12  sueh
 * <p> bug# 1147 Coverted newst, ctfcorrection, and mtffilter panels to tabs.
 * <p> Removed the open/close part of the panel headers.
 * <p>
 * <p> Revision 1.4  2008/11/07 21:26:14  sueh
 * <p> bug# 1146 Added ctfcorrection log to right-click menu.  Also add mtffilter
 * <p> options.
 * <p>
 * <p> Revision 1.3  2008/10/27 23:20:27  sueh
 * <p> bug# 1141 Changed the names of the ctfplotter button and the
 * <p> ctfcorrection button.
 * <p>
 * <p> Revision 1.2  2008/10/27 20:38:44  sueh
 * <p> bug# 1141 Added CTF Correction.
 * <p>
 * <p> Revision 1.1  2008/10/16 21:24:25  sueh
 * <p> bug# 1141 Dialog for running newst (full align) and filtering
 * <p> </p>
 */
public final class FinalAlignedStackDialog extends ProcessDialog
  implements Expandable, Run3dmodButtonContainer, ContextMenu, ProcessInterface {
  private static final String MTF_FILE_LABEL = "MTF file: ";
  public static final String USE_CTF_CORRECTION_LABEL = "Use CTF Correction";
  public static final String CTF_TAB_LABEL = "Correct CTF";
  public static final String USE_FILTERED_STACK_LABEL = "Use Filtered Stack";
  public static final String MTF_FILTER_TAB_LABEL = "2D Filter";

  private static final DialogType DIALOG_TYPE = DialogType.FINAL_ALIGNED_STACK;
  public static final String CTF_CORRECTION_LABEL = "Correct CTF";
  private static final String FIXED_IMAGE_DOSE_LABEL =
    "No dose file; use fixed dose per image: ";

  private final NewstackOrBlendmontPanel newstackOrBlendmontPanel;
  private final EtomoPanel pnlFinalAlignedStack = new EtomoPanel();

  // MTF Filter objects
  private final LabeledTextField ltfLowPassRadiusSigma =
    new LabeledTextField(FieldType.FLOATING_POINT_PAIR, "Low pass (cutoff,sigma): ");
  private final ImageIcon iconFolder = new ImageIcon(ClassLoader.getSystemResource(
    !Utilities.APRIL_FOOLS ? "images/openFile.gif" : "images/openFileFool.png"));
  private final LabeledTextField ltfMtfFile =
    new LabeledTextField(FieldType.STRING, MTF_FILE_LABEL);
  private final SimpleButton btnMtfFile = new SimpleButton(iconFolder);
  private final LabeledTextField ltfMaximumInverse =
    new LabeledTextField(FieldType.FLOATING_POINT, "Maximum Inverse: ");
  private final LabeledTextField ltfInverseRolloffRadiusSigma =
    new LabeledTextField(FieldType.FLOATING_POINT_PAIR, "Rolloff (radius,sigma): ");
  private final Run3dmodButton btnFilter;
  private final Run3dmodButton btnViewFilter =
    Run3dmodButton.get3dmodInstance("View Filtered Stack", this);
  private final MultiLineButton btnUseFilter;
  private final SpacedTextField ltfStartingAndEndingZ =
    new SpacedTextField(FieldType.INTEGER_PAIR, "Starting and ending views: ");
  private final PanelHeader filterHeader = PanelHeader.getAdvancedBasicOnlyInstance(
    "2D Filtering (optional)", this, DIALOG_TYPE, btnAdvanced, false);
  // panels that are changed in setAdvanced()
  private final SpacedPanel inverseParamsPanel;
  private final JPanel filterBodyPanel;

  // backward compatibility functionality - if the metadata binning is missing
  // get binning from newst

  private final ReconScreenState screenState;
  private final ButtonListener finalAlignedStackListener = new ButtonListener(this);
  private final FinalAlignedStackExpert expert;
  private final ProcessingMethodMediator mediator;

  // ctf correction
  private final PanelHeader ctfCorrectionHeader =
    PanelHeader.getAdvancedBasicOnlyInstance("CTF Correction", this, DIALOG_TYPE,
      btnAdvanced, false);
  private final SpacedPanel ctfCorrectionBodyPanel = SpacedPanel.getInstance(true);
  private final LabeledTextField ltfVoltage =
    new LabeledTextField(FieldType.INTEGER, "Voltage (KV): ");
  private final LabeledTextField ltfSphericalAberration =
    new LabeledTextField(FieldType.FLOATING_POINT, "Spherical Aberration (mm): ");
  private final CheckBox cbInvertTiltAngles = new CheckBox("Invert sign of tilt angles");
  private final LabeledTextField ltfAmplitudeContrast =
    new LabeledTextField(FieldType.FLOATING_POINT, "Amplitude contrast: ");
  private final LabeledTextField ltfExpectedDefocus =
    new LabeledTextField(FieldType.FLOATING_POINT, "Expected defocus (microns): ");
  private final TextEfield tfPhaseShiftInDegrees = TextEfield
    .getLabeledInstance("Expected phase shift (degrees): ", FieldType.FLOATING_POINT);
  private final LabeledTextField ltfOffsetToAdd =
    new LabeledTextField(FieldType.FLOATING_POINT, "Offset to add to image values: ");
  private final LabeledTextField ltfInterpolationWidth =
    new LabeledTextField(FieldType.INTEGER, "Interpolation width (pixels): ");
  private final LabeledTextField ltfDefocusTol =
    new LabeledTextField(FieldType.INTEGER, "Defocus tolerance (nm): ");
  private final MultiLineButton btnCtfPlotter = new MultiLineButton("Run Ctf Plotter");
  private final Run3dmodButton btnCtfCorrection;
  private final Run3dmodButton btnImodCtfCorrection =
    Run3dmodButton.get3dmodInstance("View CTF Correction", this);
  private final MultiLineButton btnUseCtfCorrection;
  private final CheckBox cbUseExpectedDefocus =
    new CheckBox("Use expected defocus instead of ctfplotter output");

  private final TabbedPane tabbedPane = new TabbedPane();
  private final EtomoPanel ctfCorrectionMainPanel = new EtomoPanel();
  private final JPanel filterPanel = new JPanel();
  private final FileTextField2 ftfConfigFile;

  // Dose Weighting
  private final CheckBoxEfield cbDoseWeightFiltering =
    CheckBoxEfield.getInstance("Apply dose weighting");
  private final JPanel pnlUniformFiltering = new JPanel();
  private final CheckBoxEfield cbFixedImageDose =
    CheckBoxEfield.getInstance(FIXED_IMAGE_DOSE_LABEL);
  private final TextEfield tfFixedImageDose =
    TextEfield.getInstance(FIXED_IMAGE_DOSE_LABEL, FieldType.FLOATING_POINT);
  private final JLabel lFixedImageDose = new JLabel(" e/sq A");
  private final ButtonControlTextEfield bctfDoseWeightingFile =
    ButtonControlTextEfield.getLabeledFileInstance("Dose information file: ");
  private final JPanel pnlTypeOfDoseFile = new JPanel();
  private final EtomoButtonGroup bgTypeOfDoseFile = new EtomoButtonGroup();
  private final RadioEbutton rbTypeOfDoseFileMdocFile =
    RadioEbutton.getEnumInstance(TypeOfDoseFile.MDOC_FILE, bgTypeOfDoseFile);
  private final RadioEbutton rbTypeOfDoseFileImageDose =
    RadioEbutton.getEnumInstance(TypeOfDoseFile.IMAGE_DOSE, bgTypeOfDoseFile);
  private final RadioEbutton rbTypeOfDoseFileAccumulatedAndImageDose = RadioEbutton
    .getEnumInstance(TypeOfDoseFile.ACCUMULATED_AND_IMAGE_DOSE, bgTypeOfDoseFile);
  private final RadioEbutton rbTypeOfDoseFilePriorAndCumulativeDose = RadioEbutton
    .getEnumInstance(TypeOfDoseFile.PRIOR_AND_CUMULATIVE_DOSE, bgTypeOfDoseFile);
  private final CheckBoxEfield cbVoltage200 =
    CheckBoxEfield.getInstance("Microscope voltage is 200 KV");
  private final TextEfield tfOptimalDoseScaling =
    TextEfield.getLabeledInstance("Optimal dose scaling", FieldType.FLOATING_POINT);
  private final TextEfield tfBidirectionalNumViews = TextEfield.getLabeledInstance(
    "\n" + "# of views in first half of bidirectional series", FieldType.INTEGER);
  private final TextEfield tfCtfPhaseFlipXAxisTilt = TextEfield
    .getDisabledInstance("Correct for X axis tilt of", FieldType.FLOATING_POINT);
  private final CheckBoxEfield cbCtfPhaseFlipXAxisTilt =
    CheckBoxEfield.getEnableControlInstance(tfCtfPhaseFlipXAxisTilt);
  private final TextEfield tfScaleByCtfPower = TextEfield
    .getDisabledInstance("Scale by CTF to the power:", FieldType.FLOATING_POINT);
  private final CheckBoxEfield cbScaleByCtfPower =
    CheckBoxEfield.getEnableControlInstance(tfScaleByCtfPower);
  private final TextEfield tfMinimumZeroSpacing = TextEfield
    .getLabeledInstance("Minimum zero spacing in strip FFTs:", FieldType.FLOATING_POINT);

  private final EraseGoldPanel eraseGoldPanel;
  private final CpuGpuPanel cpuGpuPanel;

  private boolean trialTilt = false;
  private Tab curTab = Tab.DEFAULT;
  private boolean processingMethodLocked = false;
  private boolean validAutodoc = false;
  private boolean eraseBeadsInitialized = false;

  private FinalAlignedStackDialog(ApplicationManager appMgr,
    FinalAlignedStackExpert expert, AxisID axisID) {
    super(appMgr, axisID, DIALOG_TYPE);
    this.expert = expert;
    mediator = appMgr.getProcessingMethodMediator(axisID);
    ProcessResultDisplayFactory displayFactory =
      appMgr.getProcessResultDisplayFactory(axisID);
    eraseGoldPanel = EraseGoldPanel.getInstance(appMgr, axisID, dialogType, btnAdvanced);
    cpuGpuPanel = CpuGpuPanel.getInstance(appMgr, axisID, PanelId.CTF_PHASE_FLIP, -1,
      false, BoxLayout.X_AXIS);
    if (appMgr.getMetaData().getViewType() == ViewType.MONTAGE) {
      newstackOrBlendmontPanel =
        BlendmontPanel.getInstance(appMgr, axisID, DIALOG_TYPE, btnAdvanced);
    }
    else {
      newstackOrBlendmontPanel =
        NewstackPanel.getInstance(appMgr, axisID, DIALOG_TYPE, btnAdvanced);
    }
    screenState = appMgr.getScreenState(axisID);
    btnFilter = (Run3dmodButton) displayFactory.getFilter();
    // init
    bctfDoseWeightingFile.setLimitDisplayedFilePath(50);
    if (appMgr.getMetaData().getImageFilenameStyle() != ImageFilenameStyle.HDF) {
      bctfDoseWeightingFile.setRequired(true);
      bctfDoseWeightingFile
        .setText(FileType.MTF_FILTER_MDOC.getFile(applicationManager, axisID));
    }
    btnFilter.setContainer(this);
    btnFilter.setDeferred3dmodButton(btnViewFilter);
    btnUseFilter = (MultiLineButton) displayFactory.getUseFilteredStack();
    rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
    btnExecute.setText("Done");
    btnMtfFile.setName(MTF_FILE_LABEL);
    // Layout the main panel (and sub panels) and add it to the root panel
    rootPanel.setBorder(new BeveledBorder("Final Aligned Stack").getBorder());
    filterBodyPanel = new JPanel();
    inverseParamsPanel = SpacedPanel.getInstance(true);

    btnCtfCorrection = (Run3dmodButton) displayFactory.getCtfCorrection();
    btnCtfCorrection.setContainer(this);
    btnCtfCorrection.setDeferred3dmodButton(btnImodCtfCorrection);
    btnUseCtfCorrection = (MultiLineButton) displayFactory.getUseCtfCorrection();
    ftfConfigFile = FileTextField2.getInstance(appMgr, "Config file: ");
    ftfConfigFile.setOrigin(expert.getConfigDir());
    // field instantiation
    layoutNewstPanel();
    layoutCtfCorrectionPanel();
    layoutCcdEraser();
    layoutFilterPanel();
    rootPanel.add(tabbedPane);
    addExitButtons();
    // Set the default advanced dialog state
    updateAdvanced();
    setToolTipText();
    reregisterProcessingMethodMediator();
  }

  static FinalAlignedStackDialog getInstance(ApplicationManager appMgr,
    FinalAlignedStackExpert expert, AxisID axisID, Tab curTab) {
    FinalAlignedStackDialog instance =
      new FinalAlignedStackDialog(appMgr, expert, axisID);
    instance.addListeners();
    instance.tabbedPane.setSelectedIndex(curTab.toInt());
    return instance;
  }

  private void addListeners() {
    // Bind the buttons to the action listener
    btnFilter.addActionListener(finalAlignedStackListener);
    btnViewFilter.addActionListener(finalAlignedStackListener);
    btnUseFilter.addActionListener(finalAlignedStackListener);
    btnMtfFile.addActionListener(new MtfFileActionListener(this));
    ltfStartingAndEndingZ.addKeyListener(new StartingAndEndingZKeyListener(this));
    cbUseExpectedDefocus.addActionListener(finalAlignedStackListener);
    btnCtfPlotter.addActionListener(finalAlignedStackListener);
    btnCtfCorrection.addActionListener(finalAlignedStackListener);
    btnImodCtfCorrection.addActionListener(finalAlignedStackListener);
    btnUseCtfCorrection.addActionListener(finalAlignedStackListener);
    tabbedPane.addChangeListener(new TabChangeListener(this));
    cbDoseWeightFiltering.addActionListener(finalAlignedStackListener);
    cbFixedImageDose.addActionListener(finalAlignedStackListener);
    rbTypeOfDoseFileMdocFile.addActionListener(finalAlignedStackListener);
    rbTypeOfDoseFileImageDose.addActionListener(finalAlignedStackListener);
    rbTypeOfDoseFileAccumulatedAndImageDose.addActionListener(finalAlignedStackListener);
    rbTypeOfDoseFilePriorAndCumulativeDose.addActionListener(finalAlignedStackListener);
    // Mouse adapter for context menu
    GenericMouseAdapter mouseAdapter = new GenericMouseAdapter(this);
    rootPanel.addMouseListener(mouseAdapter);
    tabbedPane.addMouseListener(mouseAdapter);
  }

  public static String getTilt3dFindButtonLabel() {
    return Tilt3dFindPanel.TILT_3D_FIND_LABEL;
  }

  public static String getReprojectModelButtonLabel() {
    return ReprojectModelPanel.REPROJECT_MODEL_LABEL;
  }

  static String getUseErasedStackLabel() {
    return CcdEraserBeadsPanel.USE_ERASED_STACK_LABEL;
  }

  static String getErasedStackTabLabel() {
    return EraseGoldPanel.ERASE_GOLD_TAB_LABEL;
  }

  void setFilterButtonEnabled(boolean enable) {
    btnFilter.setEnabled(enable);
  }

  void setFilterButtonState(ReconScreenState screenState) {
    btnFilter.setButtonState(screenState.getButtonState(btnFilter.getButtonStateKey()));
  }

  void setCtfCorrectionButtonState(ReconScreenState screenState) {
    btnCtfCorrection
      .setButtonState(screenState.getButtonState(btnCtfCorrection.getButtonStateKey()));
  }

  void setViewFilterButtonEnabled(boolean enable) {
    btnViewFilter.setEnabled(enable);
  }

  void setVoltage(ConstEtomoNumber input) {
    ltfVoltage.setText(input);
  }

  String getConfigFile() {
    return ftfConfigFile.getText();
  }

  void setConfigFile(final String input) {
    ftfConfigFile.setText(input);
  }

  void setSphericalAberration(ConstEtomoNumber input) {
    ltfSphericalAberration.setText(input);
  }

  void setInvertTiltAngles(boolean input) {
    cbInvertTiltAngles.setSelected(input);
  }

  void setAmplitudeContrast(ConstEtomoNumber input) {
    ltfAmplitudeContrast.setText(input);
  }

  void setDefocusTol(ConstEtomoNumber input) {
    ltfDefocusTol.setText(input);
  }

  void setExpectedDefocus(ConstEtomoNumber input) {
    ltfExpectedDefocus.setText(input);
  }

  void setPhaseShiftInDegrees(final String input) {
    tfPhaseShiftInDegrees.setText(input);
  }

  void setOffsetToAdd(ConstEtomoNumber input) {
    ltfOffsetToAdd.setText(input);
  }

  String getDefocusTol(final boolean doValidation) throws FieldValidationFailedException {
    return ltfDefocusTol.getText(doValidation);
  }

  void setTiltState(TomogramState state, ConstMetaData metaData) {
    eraseGoldPanel.setTiltState(state, metaData);
  }

  String getExpectedDefocus(final boolean doValidation)
    throws FieldValidationFailedException {
    return ltfExpectedDefocus.getText(doValidation);
  }

  String getPhaseShiftInDegrees(final boolean doValidation)
    throws FieldValidationFailedException {
    return tfPhaseShiftInDegrees.getText(doValidation);
  }

  String getOffsetToAdd(final boolean doValidation)
    throws FieldValidationFailedException {
    return ltfOffsetToAdd.getText(doValidation);
  }

  void setUseFilterEnabled(boolean enable) {
    btnUseFilter.setEnabled(enable);
  }

  void setInterpolationWidth(ConstEtomoNumber input) {
    ltfInterpolationWidth.setText(input);
  }

  void setFiducialessAlignment(boolean input) {
    newstackOrBlendmontPanel.setFiducialessAlignment(input);
  }

  void setImageRotation(String input) {
    newstackOrBlendmontPanel.setImageRotation(input);
  }

  void setFilterHeaderState(PanelHeaderState state) {
    filterHeader.setState(state);
  }

  void setCtfCorrectionHeaderState(PanelHeaderState state) {
    ctfCorrectionHeader.setState(state);
  }

  Tab getCurTab() {
    return curTab;
  }

  String getInterpolationWidth(final boolean doValidation)
    throws FieldValidationFailedException {
    return ltfInterpolationWidth.getText(doValidation);
  }

  String getStartingAndEndingZ() {
    return ltfStartingAndEndingZ.getText();
  }

  String getCtfPhaseFlipXAxisTilt(final boolean doValidation)
    throws FieldValidationFailedException {
    if (tfCtfPhaseFlipXAxisTilt.isEnabled()) {
      return tfCtfPhaseFlipXAxisTilt.getText(doValidation);
    }
    return null;
  }

  String getScaleByCtfPower(final boolean doValidation)
    throws FieldValidationFailedException {
    if (tfScaleByCtfPower.isEnabled()) {
      return tfScaleByCtfPower.getText(doValidation);
    }
    return null;
  }

  String getMinimumZeroSpacing(final boolean doValidation)
    throws FieldValidationFailedException {
    return tfMinimumZeroSpacing.getText(doValidation);
  }

  void setCtfPhaseFlipXAxisTilt(final String xAxisTilt, final boolean fromTiltCom) {
    boolean xAxisTiltSet = xAxisTilt != null && !xAxisTilt.isEmpty();
    // For CTF correction correcting for X axis tilt takes a long time, so don't switch it
    // on unless the setting is coming from the CTF correction com file.
    if (!fromTiltCom && xAxisTiltSet) {
      cbCtfPhaseFlipXAxisTilt.setSelected(true);
    }
    if (xAxisTiltSet) {
      tfCtfPhaseFlipXAxisTilt.setText(xAxisTilt);
    }
  }

  void setScaleByCtfPower(final String scaleByCtfPower) {
    cbScaleByCtfPower.setSelected(scaleByCtfPower != null && !scaleByCtfPower.isEmpty());
    if (cbScaleByCtfPower.isSelected()) {
      tfScaleByCtfPower.setText(scaleByCtfPower);
    }
  }

  void setMinimumZeroSpacing(final String minimumZeroSpacing) {
    tfMinimumZeroSpacing.setText(minimumZeroSpacing);
  }

  void setUseExpectedDefocus(boolean input) {
    cbUseExpectedDefocus.setSelected(input);
  }

  void setUseFilterButtonState(ReconScreenState screenState) {
    btnUseFilter
      .setButtonState(screenState.getButtonState(btnUseFilter.getButtonStateKey()));
  }

  void setUseCtfCorrectionButtonState(ReconScreenState screenState) {
    btnUseCtfCorrection.setButtonState(
      screenState.getButtonState(btnUseCtfCorrection.getButtonStateKey()));
  }

  String getVoltage(final boolean doValidation) throws FieldValidationFailedException {
    return ltfVoltage.getText(doValidation);
  }

  String getSphericalAberration(final boolean doValidation)
    throws FieldValidationFailedException {
    return ltfSphericalAberration.getText(doValidation);
  }

  boolean getInvertTiltAngles() {
    return cbInvertTiltAngles.isSelected();
  }

  /**
   * The Metadata values that are from the setup dialog should not be overrided
   * by this dialog unless the Metadata values are empty.
   * @param metaData
   * @throws FortranInputSyntaxException
   */
  void getParameters(MetaData metaData) throws FortranInputSyntaxException {
    metaData.setEraseBeadsInitialized(eraseBeadsInitialized);
    metaData.setFinalStackCtfCorrectionParallel(axisID, isParallelProcess());
    metaData.setUseStackCtfPhaseFlipXAxisTilt(axisID,
      cbCtfPhaseFlipXAxisTilt.isSelected());
    metaData.setStackCtfPhaseFlipXAxisTilt(axisID, tfCtfPhaseFlipXAxisTilt.getText());
    metaData.setStackCtfPhaseFlipScaleByCtfPower(axisID, tfScaleByCtfPower.getText());
    metaData.setStackMtfFilterLowPassRadiusSigma(axisID, ltfLowPassRadiusSigma.getText());
    metaData.setStackMtfFilterMtfFile(axisID, ltfMtfFile.getText());
    metaData.setStackMtfFilterMaximumInverse(axisID, ltfMaximumInverse.getText());
    metaData.setStackMtfFilterInverseRolloffRadiusSigma(axisID,
      ltfInverseRolloffRadiusSigma.getText());
    metaData.setUseStackMtfFilterFixedImageDose(axisID, cbFixedImageDose.isSelected());
    metaData.setStackMtfFilterFixedImageDose(axisID, tfFixedImageDose.getText());
    metaData.setStackMtfFilterDoseWeightingFile(axisID, bctfDoseWeightingFile.getText());
    metaData.setStackMtfFilterTypeOfDoseFile(axisID,
      ((AbstractRadioButtonModel) bgTypeOfDoseFile.getSelection()).getEnumeratedType());
    metaData.setStackMtfFilterVoltage200(axisID, cbVoltage200.isSelected());
    metaData.setStackMtfFilterOptimalDoseScaling(axisID, tfOptimalDoseScaling.getText());
    metaData.setStackMtfFilterBidirectionalNumViews(axisID,
      tfBidirectionalNumViews.getText());
    newstackOrBlendmontPanel.getParameters(metaData);
    eraseGoldPanel.getParameters(metaData);
  }

  BlendmontDisplay getBlendmontDisplay() {
    if (applicationManager.getMetaData().getViewType() == ViewType.MONTAGE) {
      return (BlendmontDisplay) newstackOrBlendmontPanel;
    }
    return null;
  }

  BlendmontDisplay getBlendmont3dFindDisplay() {
    return eraseGoldPanel.getBlendmont3dFindDisplay();
  }

  NewstackDisplay getNewstackDisplay() {
    if (applicationManager.getMetaData().getViewType() != ViewType.MONTAGE) {
      return (NewstackDisplay) newstackOrBlendmontPanel;
    }
    return null;
  }

  NewstackDisplay getNewstack3dFindDisplay() {
    return eraseGoldPanel.getNewstack3dFindDisplay();
  }

  TiltDisplay getTilt3dFindDisplay() {
    return eraseGoldPanel.getTilt3dFindDisplay();
  }

  FindBeads3dDisplay getFindBeads3dDisplay() {
    return eraseGoldPanel.getFindBeads3dDisplay();
  }

  CcdEraserDisplay getCcdEraserBeadsDisplay() {
    return eraseGoldPanel.getCcdEraserBeadsDisplay();
  }

  FiducialessParams getFiducialessParams() {
    return newstackOrBlendmontPanel.getFiducialessParams();
  }

  String getAmplitudeContrast(final boolean doValidation)
    throws FieldValidationFailedException {
    return ltfAmplitudeContrast.getText(doValidation);
  }

  void getFilterHeaderState(PanelHeaderState state) {
    filterHeader.getState(state);
  }

  void getCtfCorrectionHeaderState(PanelHeaderState state) {
    ctfCorrectionHeader.getState(state);
  }

  void getParameters(CtfPhaseFlipParam param) {
    cpuGpuPanel.getParameters(param);
  }

  void setParameters(final CtfPhaseFlipParam param, boolean initialize) {
    cpuGpuPanel.setParameters(param, initialize);
  }

  void getParameters(ReconScreenState screenState) {
    newstackOrBlendmontPanel.getParameters(screenState);
    eraseGoldPanel.getParameters(screenState);
    getFilterHeaderState(screenState.getStackMtffilterHeaderState());
    getCtfCorrectionHeaderState(screenState.getStackCtfCorrectionHeaderState());
  }

  void setParameters(ReconScreenState screenState) {
    newstackOrBlendmontPanel.setParameters(screenState);
    eraseGoldPanel.setParameters(screenState);
    setFilterHeaderState(screenState.getStackMtffilterHeaderState());
    setCtfCorrectionHeaderState(screenState.getStackCtfCorrectionHeaderState());
    setUseFilterButtonState(screenState);
    setFilterButtonState(screenState);
    setCtfCorrectionButtonState(screenState);
  }

  void setStartingAndEndingZ(String startingAndEndingZ) {
    ltfStartingAndEndingZ.setText(startingAndEndingZ);
  }

  public void expand(GlobalExpandButton button) {}

  /**
   * Expands the appropriate panel.  Also called when advanced/basic buttons
   * are pressed in child classes.  Responsible for changing the state of the
   * big advanced/basic button at the bottom of the dialog.
   */
  public void expand(ExpandButton button) {
    if (filterHeader.equalsAdvancedBasic(button)) {
      updateAdvancedFilter(button.isExpanded());
    }
    else if (ctfCorrectionHeader.equalsAdvancedBasic(button)) {
      updateAdvancedCtfCorrection(button.isExpanded());
    }
    UIHarness.INSTANCE.pack(axisID, applicationManager);
  }

  private void updateAdvancedFilter(boolean advanced) {
    ltfStartingAndEndingZ.setVisible(advanced);
    inverseParamsPanel.setVisible(advanced);
    tfOptimalDoseScaling.setVisible(advanced);
    tfBidirectionalNumViews.setVisible(advanced);
  }

  private void updateAdvancedCtfCorrection(boolean advanced) {
    ltfAmplitudeContrast.setVisible(advanced);
    ltfDefocusTol.setVisible(advanced);
    tfMinimumZeroSpacing.setVisible(advanced);
    cbInvertTiltAngles.setVisible(advanced);
    ltfOffsetToAdd.setVisible(advanced);
  }

  void setParameters(ConstMetaData metaData) {
    eraseBeadsInitialized = metaData.isEraseBeadsInitialized();
    cbCtfPhaseFlipXAxisTilt.setSelected(metaData.isUseStackCtfPhaseFlipXAxisTilt(axisID));
    tfCtfPhaseFlipXAxisTilt.setText(metaData.getStackCtfPhaseFlipXAxisTilt(axisID));
    tfScaleByCtfPower.setText(metaData.getStackCtfPhaseFlipScaleByCtfPower(axisID));
    ltfLowPassRadiusSigma.setText(metaData.getStackMtfFilterLowPassRadiusSigma(axisID));
    ltfMtfFile.setText(metaData.getStackMtfFilterMtfFile(axisID));
    ltfMaximumInverse.setText(metaData.getStackMtfFilterMaximumInverse(axisID));
    ltfInverseRolloffRadiusSigma
      .setText(metaData.getStackMtfFilterInverseRolloffRadiusSigma(axisID));
    cbFixedImageDose.setSelected(metaData.isUseStackMtfFilterFixedImageDose(axisID));
    tfFixedImageDose.setText(metaData.getStackMtfFilterFixedImageDose(axisID));
    String doseWeightingFile = metaData.getStackMtfFilterDoseWeightingFile(axisID);
    if (doseWeightingFile != null && !doseWeightingFile.isEmpty()
      && !doseWeightingFile.startsWith(".")) {
      bctfDoseWeightingFile.setFile(doseWeightingFile);
    }
    bgTypeOfDoseFile.setSelected(
      TypeOfDoseFile.getInstance(metaData.getStackMtfFilterTypeOfDoseFile(axisID)));
    cbVoltage200.setSelected(metaData.isStackMtfFilterVoltage200(axisID));
    tfOptimalDoseScaling.setText(metaData.getStackMtfFilterOptimalDoseScaling(axisID));
    tfBidirectionalNumViews
      .setText(metaData.getStackMtfFilterBidirectionalNumViews(axisID));

    cpuGpuPanel.setParameters(metaData,
      metaData.getFinalStackCtfCorrectionParallel(axisID));
    newstackOrBlendmontPanel.setParameters(metaData);
    eraseGoldPanel.setParameters(metaData);
    mediator.setMethod(this, getProcessingMethod());
  }

  void setParameters(BlendmontParam param) {
    if (applicationManager.getMetaData().getViewType() == ViewType.MONTAGE) {
      ((BlendmontDisplay) newstackOrBlendmontPanel).setParameters(param);
    }
  }

  void setEraseGoldParameters(BlendmontParam param) {
    eraseGoldPanel.setParameters(param);
  }

  void setEraseGoldParameters(NewstParam param) {
    eraseGoldPanel.setParameters(param);
  }

  void setParameters(ConstTiltParam param, boolean calculateValues)
    throws FileNotFoundException, IOException, LogFile.LockException {
    eraseGoldPanel.setParameters(param, calculateValues);
  }

  void setParameters(ConstFindBeads3dParam param, boolean initialize) {
    eraseGoldPanel.setParameters(param, initialize);
  }

  void setParameters(final ConstCtfPhaseFlipParam param, final boolean initialize) {
    cpuGpuPanel.setParameters(param, initialize);
  }

  void initialize() {
    eraseGoldPanel.initialize();
  }

  void setParameters(ConstTiltalignParam param, boolean initialize) {
    eraseGoldPanel.setParameters(param, initialize);
  }

  void setParameters(ConstCCDEraserParam param) {
    eraseGoldPanel.setParameters(param);
  }

  void setOverrideParameters(final ConstMetaData metaData) {
    eraseGoldPanel.setOverrideParameters(metaData);
  }

  void setParameters(ConstNewstParam param) {
    if (applicationManager.getMetaData().getViewType() != ViewType.MONTAGE) {
      ((NewstackDisplay) newstackOrBlendmontPanel).setParameters(param);
    }
  }

  /**
   * Update the dialog with the current advanced state
   */
  private void updateAdvanced() {
    boolean advanced = isAdvanced();
    newstackOrBlendmontPanel.updateAdvanced(advanced);
    eraseGoldPanel.updateAdvanced(advanced);
    updateAdvancedFilter(advanced);
    updateAdvancedCtfCorrection(advanced);
    UIHarness.INSTANCE.pack(axisID, applicationManager);
  }

  boolean isParallelProcess() {
    return cpuGpuPanel.isParallelProcess();
  }

  boolean isUseExpectedDefocus() {
    return cbUseExpectedDefocus.isSelected();
  }

  boolean isCtfPhaseFlipXAxisTiltEmpty() {
    return tfCtfPhaseFlipXAxisTilt.isEmpty();
  }

  void setTiltComParameters(final ConstTiltParam param) {
    setCtfPhaseFlipXAxisTilt(param.getXAxisTiltString(), true);
  }

  private void layoutCcdEraser() {
    // panel
    JPanel ccdEraserRoot = new JPanel();
    tabbedPane.addTab(EraseGoldPanel.ERASE_GOLD_TAB_LABEL, ccdEraserRoot);
  }

  private void layoutCtfCorrectionPanel() {
    // panels
    JPanel ctfCorrectionRoot = new JPanel();
    JPanel pnlCtfPhaseFlipXAxisTilt = new JPanel();
    Container pnlScaleByCtfPower = new JPanel();
    //
    tabbedPane.addTab(CTF_TAB_LABEL, ctfCorrectionRoot);
    ctfCorrectionMainPanel
      .setLayout(new BoxLayout(ctfCorrectionMainPanel, BoxLayout.Y_AXIS));
    ctfCorrectionMainPanel.setBorder(BorderFactory.createEtchedBorder());
    ctfCorrectionMainPanel.add(ctfCorrectionHeader.getContainer());
    ctfCorrectionMainPanel.add(ctfCorrectionBodyPanel.getContainer());
    // body
    ctfCorrectionBodyPanel.setBoxLayout(BoxLayout.Y_AXIS);
    ctfCorrectionBodyPanel.add(ltfVoltage);
    ctfCorrectionBodyPanel.add(ltfSphericalAberration);
    ctfCorrectionBodyPanel.add(ltfAmplitudeContrast);
    JPanel pnlInvertTiltAngles = new JPanel();
    pnlInvertTiltAngles.setLayout(new BoxLayout(pnlInvertTiltAngles, BoxLayout.X_AXIS));
    pnlInvertTiltAngles.setAlignmentX(Component.CENTER_ALIGNMENT);
    pnlInvertTiltAngles.add(cbInvertTiltAngles.getComponent());
    pnlInvertTiltAngles.add(Box.createHorizontalGlue());
    ctfCorrectionBodyPanel.add(pnlInvertTiltAngles);
    // ctf plotter
    SpacedPanel ctfPlotterPanel = SpacedPanel.getInstance();
    ctfPlotterPanel.setBoxLayout(BoxLayout.Y_AXIS);
    ctfPlotterPanel.setBorder(new EtchedBorder("CTF Plotter").getBorder());
    ctfPlotterPanel.setComponentAlignmentX(Component.CENTER_ALIGNMENT);
    ctfCorrectionBodyPanel.add(ctfPlotterPanel);
    ctfPlotterPanel.add(ftfConfigFile.getComponent());
    ctfPlotterPanel.add(ltfExpectedDefocus);
    ctfPlotterPanel.add(tfPhaseShiftInDegrees.getComponent());
    ctfPlotterPanel.add(ltfOffsetToAdd);
    ctfPlotterPanel.add(btnCtfPlotter);
    // ctf phase flip
    SpacedPanel ctfCorrectionPanel = SpacedPanel.getInstance();
    ctfCorrectionPanel.setBoxLayout(BoxLayout.Y_AXIS);
    ctfCorrectionPanel.setBorder(new EtchedBorder("CTF Phase Flip").getBorder());
    ctfCorrectionBodyPanel.add(ctfCorrectionPanel);
    // use expected defocus
    JPanel useExpectedDefocusPanel = new JPanel();
    useExpectedDefocusPanel
      .setLayout(new BoxLayout(useExpectedDefocusPanel, BoxLayout.X_AXIS));
    useExpectedDefocusPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    ctfCorrectionPanel.add(useExpectedDefocusPanel);
    useExpectedDefocusPanel.add(cbUseExpectedDefocus.getComponent());
    useExpectedDefocusPanel.add(Box.createHorizontalGlue());

    ctfCorrectionPanel.add(cpuGpuPanel.getComponent());
    ctfCorrectionPanel.add(ltfInterpolationWidth);
    ctfCorrectionPanel.add(pnlCtfPhaseFlipXAxisTilt);
    ctfCorrectionPanel.add(pnlScaleByCtfPower);
    ctfCorrectionPanel.add(ltfDefocusTol);
    ctfCorrectionPanel.add(tfMinimumZeroSpacing.getComponent());
    // CtfPhaseFlipXAxisTilt
    pnlCtfPhaseFlipXAxisTilt
      .setLayout(new BoxLayout(pnlCtfPhaseFlipXAxisTilt, BoxLayout.X_AXIS));
    pnlCtfPhaseFlipXAxisTilt.add(cbCtfPhaseFlipXAxisTilt.getComponent());
    pnlCtfPhaseFlipXAxisTilt.add(tfCtfPhaseFlipXAxisTilt.getComponent());
    // TODO 2220
    pnlCtfPhaseFlipXAxisTilt.add(new JLabel(" (takes much longer)"));
    // ScaleByCtfPower
    pnlScaleByCtfPower.setLayout(new BoxLayout(pnlScaleByCtfPower, BoxLayout.X_AXIS));
    pnlScaleByCtfPower.add(cbScaleByCtfPower.getComponent());
    pnlScaleByCtfPower.add(tfScaleByCtfPower.getComponent());
    // buttons
    SpacedPanel buttonPanel = SpacedPanel.getInstance();
    buttonPanel.setBoxLayout(BoxLayout.X_AXIS);
    ctfCorrectionPanel.add(buttonPanel);
    buttonPanel.addHorizontalGlue();
    buttonPanel.add(btnCtfCorrection);
    buttonPanel.add(btnImodCtfCorrection);
    buttonPanel.add(btnUseCtfCorrection);
    buttonPanel.addHorizontalGlue();
    // init
    ctfCorrectionHeader.setOpen(false);
  }

  /**
   * Layout the newstack panel
   */
  private void layoutNewstPanel() {
    // panels
    JPanel newstRoot = new JPanel();
    tabbedPane.addTab("Create", newstRoot);
    newstRoot.add(newstackOrBlendmontPanel.getComponent());
  }

  /**
   * Layout the MTF filter panel
   *
   */
  private void layoutFilterPanel() {
    // panels
    JPanel filterRoot = new JPanel();
    JPanel pnlDoseWeightFiltering = new JPanel();
    JPanel pnlDoseWeighting = new JPanel();
    JPanel pnlFixedImageDose = new JPanel();
    JPanel pnlVoltage200 = new JPanel();
    JPanel pnlGlueTypeOfDoseFile = new JPanel();
    // init
    tabbedPane.addTab(MTF_FILTER_TAB_LABEL, filterRoot);
    bctfDoseWeightingFile.setFileFilter(new DoseWeightingFileFilter());
    bctfDoseWeightingFile.setSelectFileDir(applicationManager.getPropertyUserDir());
    tfFixedImageDose.setMustBePositive(true);
    // filter
    filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
    filterPanel.setBorder(BorderFactory.createEtchedBorder());
    filterPanel.add(filterHeader.getContainer());
    filterPanel.add(filterBodyPanel);
    filterBodyPanel.setLayout(new BoxLayout(filterBodyPanel, BoxLayout.Y_AXIS));
    filterBodyPanel.add(pnlDoseWeightFiltering);
    filterBodyPanel.add(pnlUniformFiltering);
    filterBodyPanel.add(pnlDoseWeighting);
    // DoseWeightFiltering
    pnlDoseWeightFiltering
      .setLayout(new BoxLayout(pnlDoseWeightFiltering, BoxLayout.X_AXIS));
    pnlDoseWeightFiltering.add(cbDoseWeightFiltering.getComponent());
    pnlDoseWeightFiltering.add(Box.createHorizontalGlue());
    // UniformFiltering
    pnlUniformFiltering.setBorder(new EtchedBorder("Uniform Filtering").getBorder());
    pnlUniformFiltering.setLayout(new BoxLayout(pnlUniformFiltering, BoxLayout.Y_AXIS));
    pnlUniformFiltering.add(ltfLowPassRadiusSigma.getContainer());
    pnlUniformFiltering.add(inverseParamsPanel.getContainer());
    // DoseWeighting
    pnlDoseWeighting.setBorder(new EtchedBorder("Dose Weighting").getBorder());
    pnlDoseWeighting.setLayout(new BoxLayout(pnlDoseWeighting, BoxLayout.Y_AXIS));
    pnlDoseWeighting.add(pnlFixedImageDose);
    pnlDoseWeighting.add(pnlGlueTypeOfDoseFile);
    pnlDoseWeighting.add(Box.createRigidArea(FixedDim.x0_y1));
    pnlDoseWeighting.add(bctfDoseWeightingFile.getComponent());
    pnlDoseWeighting.add(pnlVoltage200);
    pnlDoseWeighting.add(tfOptimalDoseScaling.getComponent());
    pnlDoseWeighting.add(Box.createRigidArea(FixedDim.x0_y1));
    pnlDoseWeighting.add(tfBidirectionalNumViews.getComponent());
    // GlueTypeOfDoseFile
    pnlGlueTypeOfDoseFile
      .setLayout(new BoxLayout(pnlGlueTypeOfDoseFile, BoxLayout.X_AXIS));
    pnlGlueTypeOfDoseFile.add(Box.createHorizontalGlue());
    pnlGlueTypeOfDoseFile.add(pnlTypeOfDoseFile);
    pnlGlueTypeOfDoseFile.add(Box.createHorizontalGlue());
    // TypeOfDoseFile
    pnlTypeOfDoseFile.setBorder(new EtchedBorder("Type of Dose File").getBorder());
    pnlTypeOfDoseFile.setLayout(new BoxLayout(pnlTypeOfDoseFile, BoxLayout.Y_AXIS));
    pnlTypeOfDoseFile.add(rbTypeOfDoseFileMdocFile.getComponent());
    pnlTypeOfDoseFile.add(rbTypeOfDoseFileImageDose.getComponent());
    pnlTypeOfDoseFile.add(rbTypeOfDoseFileAccumulatedAndImageDose.getComponent());
    pnlTypeOfDoseFile.add(rbTypeOfDoseFilePriorAndCumulativeDose.getComponent());
    // Voltage200
    pnlVoltage200.setLayout(new BoxLayout(pnlVoltage200, BoxLayout.X_AXIS));
    pnlVoltage200.add(cbVoltage200.getComponent());
    pnlVoltage200.add(Box.createHorizontalGlue());
    // inverseParams
    inverseParamsPanel.setBoxLayout(BoxLayout.Y_AXIS);
    inverseParamsPanel
      .setBorder(new EtchedBorder("Inverse Filtering Parameters: ").getBorder());
    // FixedImageDose
    pnlFixedImageDose.setLayout(new BoxLayout(pnlFixedImageDose, BoxLayout.X_AXIS));
    pnlFixedImageDose.add(cbFixedImageDose.getComponent());
    pnlFixedImageDose.add(tfFixedImageDose.getComponent());
    pnlFixedImageDose.add(lFixedImageDose);
    SpacedPanel mtfFilePanel = SpacedPanel.getInstance();
    mtfFilePanel.setBoxLayout(BoxLayout.X_AXIS);
    SpacedPanel inversePanel = SpacedPanel.getInstance();
    inversePanel.setBoxLayout(BoxLayout.X_AXIS);
    SpacedPanel buttonPanel = SpacedPanel.getInstance(true);
    buttonPanel.setBoxLayout(BoxLayout.X_AXIS);
    // buttonPanel
    buttonPanel.add(btnFilter);
    buttonPanel.add(btnViewFilter);
    buttonPanel.add(btnUseFilter);
    // inversePanel
    inversePanel.add(ltfMaximumInverse);
    inversePanel.add(ltfInverseRolloffRadiusSigma);
    // mtfFilePanel
    mtfFilePanel.add(ltfMtfFile);
    mtfFilePanel.add(btnMtfFile);
    // inverseParamsPanel
    inverseParamsPanel.add(mtfFilePanel);
    inverseParamsPanel.add(inversePanel);
    // filterBodyPanel
    filterBodyPanel.add(Box.createRigidArea(FixedDim.x0_y5));
    filterBodyPanel.add(ltfStartingAndEndingZ.getContainer());
    filterBodyPanel.add(buttonPanel.getContainer());
    updateDisplay();
  }

  void btnMtfFileAction(ActionEvent event) {
    // Open up the file chooser in the $IMOD_CALIB_DIR/Camera, if available,
    // otherwise open in the working directory
    String currentMtfDirectory = null;
    try {
      currentMtfDirectory = ltfMtfFile.getText(true);
      if (currentMtfDirectory.equals("")) {
        File calibrationDir = EtomoDirector.INSTANCE.getIMODCalibDirectory();
        File cameraDir = new File(calibrationDir.getAbsolutePath(), "Camera");
        if (cameraDir.exists()) {
          currentMtfDirectory = cameraDir.getAbsolutePath();
        }
        else {
          currentMtfDirectory = applicationManager.getPropertyUserDir();
        }
      }
      JFileChooser chooser = new FileChooser(applicationManager, currentMtfDirectory);
      MtfFileFilter mtfFileFilter = new MtfFileFilter();
      chooser.setFileFilter(mtfFileFilter);
      chooser.setPreferredSize(FixedDim.fileChooser);
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      int returnVal = chooser.showOpenDialog(rootPanel);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File mtfFile = chooser.getSelectedFile();
        try {
          ltfMtfFile.setText(mtfFile.getAbsolutePath());
        }
        catch (Exception excep) {
          excep.printStackTrace();
        }
      }
    }
    catch (FieldValidationFailedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Right mouse button context menu
   */
  public void popUpContextMenu(final MouseEvent mouseEvent) {
    String alignManpageLabel;
    String alignManpage;
    String alignLogfileLabel;
    String alignLogfile;
    String anchor;
    if (curTab == Tab.CTF_CORRECTION) {
      String[] manPagelabel = { "Ctfplotter", "Ctfphaseflip", "3dmod" };
      String[] manPage = { "ctfplotter.html", "ctfphaseflip.html", "3dmod.html" };
      String[] logFileLabel = { "Ctfplotter", "Ctfcorrection" };
      String[] logFile = new String[2];
      logFile[0] = "ctfplotter" + axisID.getExtension() + ".log";
      logFile[1] = "ctfcorrection" + axisID.getExtension() + ".log";
      ContextPopup contextPopup =
        new ContextPopup(rootPanel, mouseEvent, "CorrectingCTF", ContextPopup.TOMO_GUIDE,
          manPagelabel, manPage, logFileLabel, logFile, applicationManager, axisID);
    }
    else if (curTab == Tab.MTF_FILTER) {
      String[] manPagelabel = { "Mtffilter", "3dmod" };
      String[] manPage = { "mtffilter.html", "3dmod.html" };
      String[] logFileLabel = { "Mtffilter" };
      String[] logFile = new String[1];
      logFile[0] = "mtffilter" + axisID.getExtension() + ".log";
      ContextPopup contextPopup =
        new ContextPopup(rootPanel, mouseEvent, "Filtering2D", ContextPopup.TOMO_GUIDE,
          manPagelabel, manPage, logFileLabel, logFile, applicationManager, axisID);
    }
    else {
      if (applicationManager.getMetaData().getViewType() == ViewType.MONTAGE) {
        alignManpageLabel = "Blendmont";
        alignManpage = "blendmont";
        alignLogfileLabel = "Blend";
        alignLogfile = "blend";
      }
      else {
        alignManpageLabel = "Newstack";
        alignManpage = "newstack";
        alignLogfileLabel = "Newst";
        alignLogfile = "newst";
      }
      String[] manPagelabel = { alignManpageLabel, "3dmod" };
      String[] manPage = { alignManpage + ".html", "3dmod.html" };
      String[] logFileLabel = { alignLogfileLabel };
      String[] logFile = new String[1];
      logFile[0] = alignLogfile + axisID.getExtension() + ".log";
      ContextPopup contextPopup =
        new ContextPopup(rootPanel, mouseEvent, "FinalAligned", ContextPopup.TOMO_GUIDE,
          manPagelabel, manPage, logFileLabel, logFile, applicationManager, axisID);
    }
  }

  void startingAndEndingZKeyReleased(KeyEvent event) {
    expert.enableUseFilter();
  }

  void done() {
    expert.doneDialog();
    newstackOrBlendmontPanel.done();
    eraseGoldPanel.done();
    btnUseFilter.removeActionListener(finalAlignedStackListener);
    btnFilter.removeActionListener(finalAlignedStackListener);
    btnCtfCorrection.removeActionListener(finalAlignedStackListener);
    btnUseCtfCorrection.removeActionListener(finalAlignedStackListener);
    setDisplayed(false);
    mediator.deregister(this);
  }

  void updateCtfPlotter() {
    boolean enable = !cbUseExpectedDefocus.isSelected();
    ftfConfigFile.setEnabled(enable);
    btnCtfPlotter.setEnabled(enable);
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
  public void action(final String command, final Deferred3dmodButton deferred3dmodButton,
    final Run3dmodMenuOptions run3dmodMenuOptions) {
    if (command.equals(btnFilter.getActionCommand())) {
      expert.mtffilter(btnFilter, null, deferred3dmodButton, run3dmodMenuOptions);
    }
    else if (command.equals(btnUseFilter.getActionCommand())) {
      expert.useMtfFilter(btnUseFilter);
    }
    else if (command.equals(btnViewFilter.getActionCommand())) {
      applicationManager.imodMTFFilter(axisID, run3dmodMenuOptions);
    }
    else if (command.equals(cbUseExpectedDefocus.getActionCommand())) {
      updateCtfPlotter();
    }
    else if (command.equals(btnCtfPlotter.getActionCommand())) {
      expert.ctfPlotter(btnCtfPlotter);
    }
    else if (command.equals(btnCtfCorrection.getActionCommand())) {
      expert.ctfCorrection(btnCtfCorrection, null, deferred3dmodButton,
        run3dmodMenuOptions, cpuGpuPanel.getRunMethodForProcessInterface());
    }
    else if (command.equals(btnImodCtfCorrection.getActionCommand())) {
      applicationManager.imodCtfCorrection(axisID, run3dmodMenuOptions);
    }
    else if (command.equals(btnUseCtfCorrection.getActionCommand())) {
      expert.useCtfCorrection(btnUseCtfCorrection);
    }
    else {
      updateDisplay();
    }
  }

  private void updateDisplay() {
    boolean doseWeightFiltering = cbDoseWeightFiltering.isSelected();
    pnlUniformFiltering.setEnabled(!doseWeightFiltering);
    ltfLowPassRadiusSigma.setEnabled(!doseWeightFiltering);
    inverseParamsPanel.setEnabled(!doseWeightFiltering);
    ltfMtfFile.setEnabled(!doseWeightFiltering);
    btnMtfFile.setEnabled(!doseWeightFiltering);
    ltfMaximumInverse.setEnabled(!doseWeightFiltering);
    ltfInverseRolloffRadiusSigma.setEnabled(!doseWeightFiltering);
    cbFixedImageDose.setEnabled(doseWeightFiltering);
    //
    boolean fixedImageDose = cbFixedImageDose.isSelected();
    tfFixedImageDose.setEnabled(doseWeightFiltering && fixedImageDose);
    lFixedImageDose.setEnabled(doseWeightFiltering);
    bctfDoseWeightingFile.setEnabled(doseWeightFiltering && !fixedImageDose);
    pnlTypeOfDoseFile.setEnabled(doseWeightFiltering && !fixedImageDose);
    rbTypeOfDoseFileMdocFile.setEnabled(doseWeightFiltering && !fixedImageDose);
    rbTypeOfDoseFileImageDose.setEnabled(doseWeightFiltering && !fixedImageDose);
    rbTypeOfDoseFileAccumulatedAndImageDose
      .setEnabled(doseWeightFiltering && !fixedImageDose);
    rbTypeOfDoseFilePriorAndCumulativeDose
      .setEnabled(doseWeightFiltering && !fixedImageDose);
    cbVoltage200.setEnabled(doseWeightFiltering);
    tfOptimalDoseScaling.setEnabled(doseWeightFiltering);
    tfBidirectionalNumViews.setEnabled(doseWeightFiltering
      && ((TypeOfDoseFile) ((AbstractRadioButtonModel) bgTypeOfDoseFile.getSelection())
        .getEnumeratedType()).enableBidirectionalNumViews);
  }

  boolean getParameters(final MTFFilterParam param, final boolean doValidation)
    throws FortranInputSyntaxException {
    try {
      if (ltfLowPassRadiusSigma.isEnabled()) {
        param.setLowPassRadiusSigma(ltfLowPassRadiusSigma.getText(doValidation));
      }
      else {
        param.resetLowPassRadiusSigma();
      }
      if (ltfMtfFile.isEnabled()) {
        param.setMtfFile(ltfMtfFile.getText(doValidation));
      }
      else {
        param.resetMtfFile();
      }
      if (ltfMaximumInverse.isEnabled()) {
        param.setMaximumInverse(ltfMaximumInverse.getText(doValidation));
      }
      else {
        param.resetMaximumInverse();
      }
      if (ltfInverseRolloffRadiusSigma.isEnabled()) {
        param.setInverseRolloffRadiusSigma(
          ltfInverseRolloffRadiusSigma.getText(doValidation));
      }
      else {
        param.resetInverseRolloffRadiusSigma();
      }
      if (tfFixedImageDose.isEnabled()) {
        param.setFixedImageDose(tfFixedImageDose.getText(doValidation));
      }
      else {
        param.resetFixedImageDose();
      }
      TypeOfDoseFile typeOfDoseFile = null;
      if (pnlTypeOfDoseFile.isEnabled()) {
        typeOfDoseFile =
          (TypeOfDoseFile) ((AbstractRadioButtonModel) bgTypeOfDoseFile.getSelection())
            .getEnumeratedType();
        param.setTypeOfDoseFile(typeOfDoseFile);
      }
      else {
        param.resetTypeOfDoseFile();
      }
      if (cbFixedImageDose.isEnabled() && !cbFixedImageDose.isSelected()
        && bctfDoseWeightingFile.isEnabled()) {
        param.setDoseWeightingFile(bctfDoseWeightingFile.getText(doValidation));
      }
      else {
        param.resetDoseWeightingFile();
      }
      if (cbVoltage200.isEnabled()) {
        param.setVoltage200(cbVoltage200.isSelected());
      }
      else {
        param.resetVoltage();
      }
      if (tfOptimalDoseScaling.isEnabled()) {
        param.setOptimalDoseScaling(tfOptimalDoseScaling.getText(doValidation));
      }
      else {
        param.resetOptimalDoseScaling();
      }
      if (tfBidirectionalNumViews.isEnabled()) {
        param.setBidirectionalNumViews(tfBidirectionalNumViews.getText(doValidation));
      }
      else {
        param.resetBidirectionalNumViews();
      }
      param.setStartingAndEndingZ(ltfStartingAndEndingZ.getText(doValidation));
    }
    catch (

    FieldValidationFailedException e) {
      return false;
    }
    return true;
  }

  void setParameters(final ConstMTFFilterParam param) {
    cbDoseWeightFiltering
      .setSelected(param.isTypeOfDoseFileSet() || param.isFixedImageDoseSet());
    if (param.isMtfFileSet()) {
      ltfMtfFile.setText(param.getMtfFile());
    }
    if (param.isMaximumInverseSet()) {
      ltfMaximumInverse.setText(param.getMaximumInverseString());
    }
    if (param.isLowPassRadiusSigmaSet()) {
      ltfLowPassRadiusSigma.setText(param.getLowPassRadiusSigmaString());
    }
    if (param.isInverseRolloffRadiusSigmaSet()) {
      ltfInverseRolloffRadiusSigma.setText(param.getInverseRolloffRadiusSigmaString());
    }
    cbFixedImageDose.setSelected(param.isFixedImageDoseSet());
    if (cbFixedImageDose.isSelected()) {
      tfFixedImageDose.setText(param.getFixedImageDose());
    }
    if (param.isDoseWeightingFileSet()) {
      String doseWeightingFile = param.getDoseWeightingFile();
      // Treat an extension (like .mrc or .st) as a string.
      if (doseWeightingFile != null && !doseWeightingFile.isEmpty()
        && !doseWeightingFile.startsWith(".")) {
        bctfDoseWeightingFile.setFile(doseWeightingFile);
      }
    }
    bgTypeOfDoseFile.setSelected(TypeOfDoseFile.getInstance(param.getTypeOfDoseFile()));
    cbVoltage200.setSelected(param.isVoltage200());
    if (param.isOptimalDoseScalingSet()) {
      tfOptimalDoseScaling.setText(param.getOptimalDoseScaling());
    }
    if (param.isBidirectionalNumViewsSet()) {
      tfBidirectionalNumViews.setText(param.getBidirectionalNumViews());
    }
    ltfStartingAndEndingZ.setText(param.getStartingAndEndingZString());
    updateDisplay();
  }

  public void lockProcessingMethod(final boolean lock) {
    processingMethodLocked = lock;
    cpuGpuPanel.lockProcessingMethod(lock);
  }

  public void disableGpu(final boolean disable) {}

  /**
   * Return local processing method. Ctf correction and
   * bead erasing are handled by different instances of the CpuGpuPanel.
   * @see reregisterProcessingMethodMediator
   */
  public ProcessingMethod getProcessingMethod() {
    return ProcessingMethod.LOCAL_CPU;
  }

  public ProcessingMethod getSecondaryProcessingMethod() {
    return null;
  }

  private void reregisterProcessingMethodMediator() {
    if (curTab == Tab.CCD_ERASER) {
      eraseGoldPanel.reregisterProcessingMethodMediator();
    }
    else if (curTab == Tab.CTF_CORRECTION) {
      cpuGpuPanel.reregisterProcessingMethodMediator();
    }
    else {
      mediator.register(this);
      mediator.setMethod(this, getProcessingMethod());
    }
  }

  private void changeTab() {
    Tab prevTab = curTab;
    curTab = Tab.getInstance(tabbedPane.getSelectedIndex());
    if (prevTab == curTab) {
      // Tab didn't change - nothing to do
      return;
    }
    ((Container) tabbedPane.getComponentAt(prevTab.toInt())).removeAll();
    if (prevTab != Tab.CCD_ERASER && curTab == Tab.CCD_ERASER && !eraseBeadsInitialized) {
      eraseGoldPanel.initializeBeads();
      eraseBeadsInitialized = true;
    }
    reregisterProcessingMethodMediator();
    // Add panel for new tab
    Container panel = (Container) tabbedPane.getSelectedComponent();
    if (curTab == Tab.NEWST) {
      panel.add(newstackOrBlendmontPanel.getComponent());
    }
    else if (curTab == Tab.CTF_CORRECTION) {
      panel.add(ctfCorrectionMainPanel);
    }
    else if (curTab == Tab.CCD_ERASER) {
      panel.add(eraseGoldPanel.getComponent());
    }
    else if (curTab == Tab.MTF_FILTER) {
      panel.add(filterPanel);
    }
    UIHarness.INSTANCE.pack(axisID, applicationManager);
    // Warning caused by leaving the previous tab
    if (curTab != prevTab) {
      TomogramState state = applicationManager.getState();
      if (prevTab == Tab.CTF_CORRECTION && state.isUseCtfCorrectionWarning(axisID)) {
        // The use button wasn't pressed and the user is moving on to the next
        // dialog. Don't put this message in the log.
        UIHarness.INSTANCE.openMessageDialog(null,
          "To use the CTF correction go back to the " + CTF_TAB_LABEL
            + " tab and press the \"" + USE_CTF_CORRECTION_LABEL + "\" button.",
          "Entry Warning", axisID);
        // Only warn once.
        state.setUseCtfCorrectionWarning(axisID, false);
      }
      else if (prevTab == Tab.CCD_ERASER && state.isUseErasedStackWarning(axisID)) {
        // The use button wasn't pressed and the user is moving on to the next
        // dialog. Don't put this message in the log.
        UIHarness.INSTANCE.openMessageDialog(null,
          "To use the stack with the erased beads go back to the "
            + EraseGoldPanel.ERASE_GOLD_TAB_LABEL + " tab and press the \""
            + CcdEraserBeadsPanel.USE_ERASED_STACK_LABEL + "\" button.",
          "Entry Warning", axisID);
        // Only warn once.
        state.setUseErasedStackWarning(axisID, false);
      }
      else if (prevTab == Tab.MTF_FILTER && state.isUseFilteredStackWarning(axisID)) {
        // The use button wasn't pressed and the user is moving on to the next
        // dialog. Don't put this message in the log.
        UIHarness.INSTANCE.openMessageDialog(null,
          "To use the MTF filtered stack go back to the " + MTF_FILTER_TAB_LABEL
            + " tab and press the \"" + USE_FILTERED_STACK_LABEL + "\" button.",
          "Entry Warning", axisID);
        // Only warn once.
        state.setUseFilteredStackWarning(axisID, false);
      }
    }
    UIHarness.INSTANCE.moveSubFrame();
  }

  /**
   * Initialize the tooltip text for the axis panel objects
   */
  private void setToolTipText() {
    ReadOnlyAutodoc autodoc = null;
    try {
      autodoc = AutodocFactory.getInstance(applicationManager, AutodocFactory.MTF_FILTER,
        axisID, false);
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
    if (autodoc != null) {
      ltfStartingAndEndingZ
        .setToolTipText(EtomoAutodoc.getTooltip(autodoc, "StartingAndEndingZ"));
      ltfLowPassRadiusSigma
        .setToolTipText(EtomoAutodoc.getTooltip(autodoc, "LowPassRadiusSigma"));
      String text = EtomoAutodoc.getTooltip(autodoc, "MtfFile");
      if (text != null) {
        ltfMtfFile.setToolTipText(text);
        btnMtfFile.setToolTipText(text);
      }
      ltfMaximumInverse
        .setToolTipText(EtomoAutodoc.getTooltip(autodoc, "MaximumInverse"));
      ltfInverseRolloffRadiusSigma
        .setToolTipText(EtomoAutodoc.getTooltip(autodoc, "InverseRolloffRadiusSigma"));
      bctfDoseWeightingFile.setTooltip(
        EtomoAutodoc.getTooltip(autodoc, MTFFilterParam.DOSE_WEIGHTING_FILE_KEY));
      pnlTypeOfDoseFile.setToolTipText(TooltipFormatter.INSTANCE
        .format(EtomoAutodoc.getTooltip(autodoc, MTFFilterParam.TYPE_OF_DOSE_FILE_KEY)));
      ReadOnlySection section = autodoc.getSection(EtomoAutodoc.FIELD_SECTION_NAME,
        MTFFilterParam.TYPE_OF_DOSE_FILE_KEY);
      rbTypeOfDoseFileImageDose.setToolTipText(AutodocFactory.MTF_FILTER, section);
      rbTypeOfDoseFileAccumulatedAndImageDose.setToolTipText(AutodocFactory.MTF_FILTER,
        section);
      rbTypeOfDoseFilePriorAndCumulativeDose.setToolTipText(AutodocFactory.MTF_FILTER,
        section);
      tfOptimalDoseScaling.setTooltip(
        EtomoAutodoc.getTooltip(autodoc, MTFFilterParam.OPTIMAL_DOSE_SCALING_KEY));
      tfBidirectionalNumViews.setTooltip(
        EtomoAutodoc.getTooltip(autodoc, MTFFilterParam.BIDIRECTIONAL_NUM_VIEWS_KEY));
    }
    rbTypeOfDoseFileMdocFile.setToolTipText(
      "Use dose information from a SerialEM .mdoc file named setname.mrc.mdoc and in the "
        + "current directory");
    tfFixedImageDose.setTooltip(
      "Use the same dose for each image instead of getting dose information from a file.");
    cbDoseWeightFiltering.setToolTipText(
      "Enables 'dose weighting', which filters out high frequencies as a function of the "
        + "dose already applied to a cryo-specimen.");
    cbVoltage200
      .setToolTipText("Set microscope voltage in kV to 200; the default is 300 KV.");
    btnFilter.setToolTipText("Run mtffilter on the full aligned stack.");
    btnViewFilter.setToolTipText(
      "View the results of running mtffilter on the full " + "aligned stack.");
    btnUseFilter.setToolTipText(
      "Use the results of running mtffilter as the new full " + "aligned stack.");
    try {
      autodoc = AutodocFactory.getInstance(applicationManager, AutodocFactory.CTF_PLOTTER,
        axisID, false);
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
    if (autodoc != null) {
      ftfConfigFile.setToolTipText(EtomoAutodoc.getTooltip(autodoc, "ConfigFile"));
      ltfVoltage
        .setToolTipText(EtomoAutodoc.getTooltip(autodoc, CtfPhaseFlipParam.VOLTAGE_OPTION)
          + "  Also used in " + CtfPhaseFlipParam.COMMAND + ".");
      ltfSphericalAberration.setToolTipText(
        EtomoAutodoc.getTooltip(autodoc, CtfPhaseFlipParam.SPHERICAL_ABERRATION_OPTION)
          + "  Also used in " + CtfPhaseFlipParam.COMMAND + ".");
      cbInvertTiltAngles.setToolTipText(
        EtomoAutodoc.getTooltip(autodoc, CtfPhaseFlipParam.INVERT_TILT_ANGLES_OPTION)
          + "  Also used in " + CtfPhaseFlipParam.COMMAND + ".");
      ltfAmplitudeContrast.setToolTipText(
        EtomoAutodoc.getTooltip(autodoc, CtfPhaseFlipParam.AMPLITUDE_CONTRAST_OPTION)
          + "  Also used in " + CtfPhaseFlipParam.COMMAND + ".");
      ltfExpectedDefocus.setToolTipText(
        EtomoAutodoc.getTooltip(autodoc, CtfPlotterParam.EXPECTED_DEFOCUS_OPTION));
      tfPhaseShiftInDegrees.setTooltip(
        EtomoAutodoc.getTooltip(autodoc, CtfPlotterParam.PHASE_SHIFT_IN_DEGREES_OPTION));
      ltfOffsetToAdd.setToolTipText(
        EtomoAutodoc.getTooltip(autodoc, CtfPlotterParam.OFFSET_TO_ADD_OPTION));
    }
    btnCtfPlotter.setToolTipText("Run ctfplotter");
    try {
      autodoc = AutodocFactory.getInstance(applicationManager,
        AutodocFactory.CTF_PHASE_FLIP, axisID, false);
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
    if (autodoc != null) {
      ltfInterpolationWidth.setToolTipText(
        EtomoAutodoc.getTooltip(autodoc, CtfPhaseFlipParam.INTERPOLATION_WIDTH_OPTION));
      cbCtfPhaseFlipXAxisTilt.setToolTipText(
        "Correct for X-axis tilt, which may take roughly 10 times longer.  A few degrees "
          + "of X axis tilt is insignificant except for the highest resolution work.");
      tfCtfPhaseFlipXAxisTilt.setTooltip(
        EtomoAutodoc.getTooltip(autodoc, CtfPhaseFlipParam.X_AXIS_TILT_OPTION));
      String tooltip =
        EtomoAutodoc.getTooltip(autodoc, CtfPhaseFlipParam.SCALE_BY_CTF_POWER_OPTION);
      cbScaleByCtfPower.setToolTipText(tooltip);
      tfScaleByCtfPower.setTooltip(tooltip);
      tfMinimumZeroSpacing.setTooltip(
        EtomoAutodoc.getTooltip(autodoc, CtfPhaseFlipParam.MINIMUM_ZERO_SPACING_OPTION));
      ltfDefocusTol.setToolTipText(
        EtomoAutodoc.getTooltip(autodoc, CtfPhaseFlipParam.DEFOCUS_TOL_OPTION));
    }
    cbUseExpectedDefocus.setToolTipText(
      "Instead of using the CTF plotter output (" + DatasetFiles.CTF_PLOTTER_EXT
        + ") use a one line file containing the expected defocus ("
        + DatasetFiles.SIMPLE_DEFOCUS_EXT + ").  Etomo will create the "
        + DatasetFiles.SIMPLE_DEFOCUS_EXT + " file when this checkbox is checked.");
    btnCtfCorrection.setToolTipText("Run " + ProcessName.CTF_CORRECTION + "."
      + DatasetFiles.COMSCRIPT_EXT + ", which calls " + CtfPhaseFlipParam.COMMAND + ".");
    String ctfCorrectedFileName =
      FileType.CTF_CORRECTED_STACK.getFileName(applicationManager, axisID);
    btnImodCtfCorrection
      .setToolTipText("Open CTF corrected stack (" + ctfCorrectedFileName + ").");
    btnUseCtfCorrection.setToolTipText("Replace full aligned stack ("
      + FileType.ALIGNED_STACK.getFileName(applicationManager, axisID)
      + ") with CTF corrected stack (" + ctfCorrectedFileName + ").");
  }

  private static final class ButtonListener implements ActionListener {
    private final FinalAlignedStackDialog adaptee;

    private ButtonListener(final FinalAlignedStackDialog adaptee) {
      this.adaptee = adaptee;
    }

    public void actionPerformed(final ActionEvent event) {
      adaptee.action(event.getActionCommand(), null, null);
    }
  }

  private static final class MtfFileActionListener implements ActionListener {
    private final FinalAlignedStackDialog adaptee;

    private MtfFileActionListener(final FinalAlignedStackDialog adaptee) {
      this.adaptee = adaptee;
    }

    public void actionPerformed(ActionEvent event) {
      adaptee.btnMtfFileAction(event);
    }
  }

  private static final class StartingAndEndingZKeyListener implements KeyListener {
    private final FinalAlignedStackDialog adaptee;

    private StartingAndEndingZKeyListener(final FinalAlignedStackDialog adaptee) {
      this.adaptee = adaptee;
    }

    public void keyReleased(final KeyEvent event) {
      adaptee.startingAndEndingZKeyReleased(event);
    }

    public void keyPressed(final KeyEvent event) {}

    public void keyTyped(final KeyEvent event) {}
  }

  private static final class TabChangeListener implements ChangeListener {
    private final FinalAlignedStackDialog adaptee;

    private TabChangeListener(FinalAlignedStackDialog dialog) {
      adaptee = dialog;
    }

    public void stateChanged(final ChangeEvent event) {
      adaptee.changeTab();
    }
  }

  static final class Tab {
    private static final Tab NEWST = new Tab(0, "NEWST");
    private static final Tab CTF_CORRECTION = new Tab(1, "CTF_CORRECTION");
    private static final Tab CCD_ERASER = new Tab(2, "CCD_ERASER");
    private static final Tab MTF_FILTER = new Tab(3, "MTF_FILTER");

    static final Tab DEFAULT = NEWST;

    private final int index;
    private final String string;

    private Tab(int index, String string) {
      this.index = index;
      this.string = string;
    }

    private static Tab getInstance(int index) {
      if (index == NEWST.index) {
        return NEWST;
      }
      if (index == CTF_CORRECTION.index) {
        return CTF_CORRECTION;
      }
      if (index == CCD_ERASER.index) {
        return CCD_ERASER;
      }
      if (index == MTF_FILTER.index) {
        return MTF_FILTER;
      }
      return DEFAULT;
    }

    private boolean isDefault() {
      return this == DEFAULT;
    }

    private int toInt() {
      return index;
    }

    public String toString() {
      return string;
    }
  }

  private static final class TypeOfDoseFile implements EnumeratedType {
    private static final TypeOfDoseFile IMAGE_DOSE =
      new TypeOfDoseFile(1, "Dose for each image", true);
    private static final TypeOfDoseFile ACCUMULATED_AND_IMAGE_DOSE =
      new TypeOfDoseFile(2, "Prior dose and dose of image", false);
    private static final TypeOfDoseFile PRIOR_AND_CUMULATIVE_DOSE =
      new TypeOfDoseFile(3, "Cumulative dose before and after image", false);
    private static final TypeOfDoseFile MDOC_FILE =
      new TypeOfDoseFile(4, "Metadata in .mdoc file", true);

    private static final TypeOfDoseFile DEFAULT = MDOC_FILE;

    private final EtomoNumber value = new EtomoNumber();

    private final String label;
    private final boolean enableBidirectionalNumViews;

    private TypeOfDoseFile(final int value, final String label,
      final boolean enableBidirectionalNumViews) {
      this.value.set(value);
      this.label = label;
      this.enableBidirectionalNumViews = enableBidirectionalNumViews;
    }

    private static TypeOfDoseFile getInstance(final String value) {
      if (value == null) {
        return DEFAULT;
      }
      if (IMAGE_DOSE.value.equals(value)) {
        return IMAGE_DOSE;
      }
      if (ACCUMULATED_AND_IMAGE_DOSE.value.equals(value)) {
        return ACCUMULATED_AND_IMAGE_DOSE;
      }
      if (PRIOR_AND_CUMULATIVE_DOSE.value.equals(value)) {
        return PRIOR_AND_CUMULATIVE_DOSE;
      }
      if (MDOC_FILE.value.equals(value)) {
        return MDOC_FILE;
      }
      return DEFAULT;
    }

    public ConstEtomoNumber getValue() {
      return value;
    }

    public String toString() {
      return value.toString();
    }

    public boolean isDefault() {
      return this == DEFAULT;
    }

    public String getLabel() {
      return label;
    }
  }
}
