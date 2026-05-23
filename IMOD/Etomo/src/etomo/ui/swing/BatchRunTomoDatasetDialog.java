package etomo.ui.swing;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import etomo.BatchRunTomoManager;
import etomo.logic.AutodocAttributeRetriever;
import etomo.logic.BatchTool;
import etomo.logic.ConfigTool;
import etomo.logic.SeedingMethod;
import etomo.logic.TrackingMethod;
import etomo.storage.DirectiveDef;
import etomo.storage.DirectiveFileCollection;
import etomo.storage.DirectiveFileInterface;
import etomo.storage.autodoc.WritableAutodoc;
import etomo.type.AxisID;
import etomo.type.BatchRunTomoDatasetMetaData;
import etomo.type.BatchRunTomoStatus;
import etomo.type.DialogType;
import etomo.type.EnumeratedType;
import etomo.type.EraseGold;
import etomo.type.EtomoNumber;
import etomo.type.FileType;
import etomo.type.FrameStatus;
import etomo.type.SampleType;
import etomo.type.Status;
import etomo.type.StatusChangeBooleanEvent;
import etomo.type.StatusChangeEvent;
import etomo.ui.BatchRunTomoTab;
import etomo.ui.BrowsingDirectory;
import etomo.ui.Field;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.TableListener;
import etomo.ui.UIComponent;
import etomo.util.DatasetFiles;
import etomo.util.Utilities;

/**
 * <p>Description: Contains parameters that are dataset values.  Can be used for all
 * datasets and individual ones. </p>
 * 
 * 
 * <p>Copyright: Copyright 2014 - 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class BatchRunTomoDatasetDialog implements ActionListener, Expandable, UIComponent,
  SwingComponent, TableListener, FieldDisplayer {
  private static final String LENGTH_OF_PIECES_DEFAULT = "-1";
  private static final String SCALE_TO_INTEGER_VALUE = "-20000,20000";
  private static final String CTF_RANGE_DEFAULT = "0.0";
  private static final String CTF_STEP_DEFAULT = "0.0";
  private static final EtomoNumber CTF_RANGE_FIT_EVERY_IMAGE =
    new EtomoNumber(EtomoNumber.Type.DOUBLE);
  private static final EtomoNumber CTF_STEP_FIT_EVERY_IMAGE =
    new EtomoNumber(EtomoNumber.Type.DOUBLE);
  private static final AxisID AXIS_ID = AxisID.ONLY;

  private final JPanel pnlRoot = new JPanel();
  private final CheckBox cbRemoveXrays = new CheckBox("Remove X-rays");
  private final CheckBox cbEnableStretching =
    new CheckBox("Enable distortion (stretching) in alignment");
  private final CheckBox cbLocalAlignments = new CheckBox("Use local alignments");
  private final SingleLineButton btnModelFile = new SingleLineButton("Make in 3dmod");
  private final ButtonGroup bgTrackingMethod = new ButtonGroup();
  private final RadioButton rbTrackingMethodSeed =
    new RadioButton("Autoseed and track", TrackingMethod.SEED, bgTrackingMethod);
  private final RadioButton rbTrackingMethodRaptor =
    new RadioButton("Raptor and track", TrackingMethod.RAPTOR, bgTrackingMethod);
  private final RadioButton rbTrackingMethodPatchTracking =
    new RadioButton("Patch tracking", TrackingMethod.PATCH_TRACKING, bgTrackingMethod);
  private final RadioButton rbFiducialless =
    new RadioButton("Coarse alignment only", bgTrackingMethod);
  private final LabeledTextField ltfGold =
    new LabeledTextField(FieldType.FLOATING_POINT, "Bead size (nm): ");
  private final LabeledTextField ltfTargetNumberOfBeads =
    new LabeledTextField(FieldType.INTEGER, "Target number of beads: ");
  private final LabeledTextField ltfSizeOfPatchesXandY =
    new LabeledTextField(FieldType.INTEGER_PAIR, "Patch tracking size: ");
  private final CheckBox cbLengthOfPieces = new CheckBox("Break contours into pieces");
  private final LabeledSpinner lsBinByFactor =
    LabeledSpinner.getInstance("Aligned stack binning: ", 1, 1, 8, 1);
  private final CheckBox cbCorrectCTF = new CheckBox("Correct CTF");
  private final LabeledTextField ltfDefocus =
    new LabeledTextField(FieldType.FLOATING_POINT, "Defocus: ");
  private final ButtonGroup bgAutofit = new ButtonGroup();
  private final RadioButton rbFitEveryImage =
    new RadioButton("Fit every image", bgAutofit);
  private final RadioTextField rtfAutoFitRangeAndStep =
    RadioTextField.getInstance(FieldType.FLOATING_POINT, "Autofit range ", bgAutofit);
  private final LabeledTextField ltfAutoFitStep =
    new LabeledTextField(FieldType.FLOATING_POINT, " and step ");
  private final CheckBox cbDoBackprojAlso = new CheckBox("R-weighted backprojection");
  private final CheckBox cbFakeSIRTiterations = new CheckBox("SIRT-like filter");
  private final LabeledTextField ltfFakeSIRTiterations =
    new LabeledTextField(FieldType.INTEGER, "Equivalent iterations: ");
  private final CheckBox cbUseSirt = new CheckBox("SIRT");
  private final LabeledTextField ltfLeaveIterations =
    new LabeledTextField(FieldType.STRING, "Leave iterations: ");
  private final CheckBox cbScaleToInteger = new CheckBox("Scale to integers");
  private final ButtonGroup bgThickness = new ButtonGroup();
  private final RadioTextField rtfThickness = RadioTextField
    .getInstance(FieldType.INTEGER, "Thickness total (unbinned pixels): ", bgThickness);
  private final RadioTextField rtfBinnedThickness = RadioTextField
    .getInstance(FieldType.INTEGER, "Thickness total (binned pixels): ", bgThickness);
  private final RadioButton rbFallbackAndExtraThickness =
    new RadioButton("Calculated thickness (unbinned pixels):", bgThickness);
  private final LabeledTextField ltfExtraThickness =
    new LabeledTextField(FieldType.INTEGER, "          Plus (optional): ");
  private final LabeledTextField ltfFallbackThickness =
    new LabeledTextField(FieldType.INTEGER, "          With fallback: ");
  private final List<Field> fieldList = new ArrayList<Field>();
  private final JPanel pnlRootBody = new JPanel();
  private final Spacer spaceModelFile = new Spacer(FixedDim.x5_y0);
  private final LabeledSpinner lsPrenewstBinByFactor = LabeledSpinner
    .getInstance("Coarse aligned stack binning - single frame: ", 1, 1, 8, 1);
  private final LabeledSpinner lsPreblendBinByFactor =
    LabeledSpinner.getInstance("Montage: ", 1, 1, 8, 1);
  private final CheckBox cbSampleType = new CheckBox("Do positioning for:");
  private final CheckBox cbDoTrimvol = new CheckBox("Postprocess with trimvol");
  private final JPanel pnlPostprocessingBody = new JPanel();
  private final CheckTextField ctfFindSecAddThickness = CheckTextField
    .getInstance(FieldType.FLOATING_POINT, "Find plastic section limits and add: ");
  private final CheckTextField ctfScaleFromZ = CheckTextField
    .getInstance(FieldType.FLOATING_POINT, "Fraction of Z slices to analyze:");
  private final CheckBox cbEraseGold = new CheckBox("Erase gold");
  private final ButtonGroup bgEraseGold = new ButtonGroup();
  private final RadioButton rbEraseGoldFid =
    new RadioButton("Use fiducial model", EraseGold.FID, bgEraseGold);
  private final RadioButton rbEraseGold3d =
    new RadioButton("Find beads in 3D", EraseGold.FIND_3D, bgEraseGold);
  private final LabeledTextField ltfGoldErasingThickness =
    new LabeledTextField(FieldType.INTEGER, "Tomogram thickness (pixels): ");
  private final ButtonGroup bgSampleType = new ButtonGroup();
  private final RadioButton rbSampleTypePlasticSection =
    new RadioButton("Plastic section", SampleType.PLASTIC_SECTION, bgSampleType);
  private final RadioButton rbSampleTypeCryo =
    new RadioButton("Cryo sample", SampleType.CRYO, bgSampleType);
  private final LabeledTextField ltfPositioningThickness =
    new LabeledTextField(FieldType.INTEGER, "Tomogram thickness: ");
  private final CheckBox cbHasGoldBeads = new CheckBox("Sample has gold beads");
  private final LabeledTextField ltfPositioningGold =
    new LabeledTextField(FieldType.FLOATING_POINT, "Bead size (nm): ");
  private final JSeparator sDistortion = new JSeparator();
  private final Ebutton btnAdvanced = Ebutton.getSingleLineInstance("Advanced");
  private final Ebutton btnBasic = Ebutton.getSingleLineInstance("Basic");
  private final JPanel pnlBasic = new JPanel();
  private final PanelHeader phRoot =
    PanelHeader.getInstance("Global Dataset Values", this, DialogType.BATCH_RUN_TOMO);
  private final AdvancedFieldDisplayer advancedFieldDisplayer =
    new AdvancedFieldDisplayer(this);

  private final FileTextField2 ftfDistort;
  private final FileTextField2 ftfGradient;
  private final FileTextField2 ftfModelFile;
  // Not used in global instance
  private final JFrame frame;
  private final JMenuItem menuFitWindow;
  //
  private final BatchRunTomoManager manager;
  private final File datasetFile;
  private final BatchRunTomoDialog parent;
  private final SingleLineButton btnOk;
  private final SingleLineButton btnRevertToGlobal;
  private final PanelHeader phPostprocessing;
  private final Map<DirectiveDef, String> templateValues;
  private final BrowsingDirectory browsingDir;
  private final String stackID;
  private final Set<DirectiveDef> basicDirectives;
  private final boolean global;

  private String lengthOfPieces = null;
  private BatchRunTomoStatus status = BatchRunTomoStatus.DEFAULT;
  private DirectivesDialog directivesDialog = null;

  private BatchRunTomoRow row;
  private boolean emptyTable;
  private boolean fiducialModelMode = true;
  private boolean shiftBasicButton = false;
  private boolean advanced = false;

  private BatchRunTomoDatasetDialog(final BatchRunTomoManager manager,
    final File datasetFile, final boolean global, final BatchRunTomoRow row,
    final boolean emptyTable, final BatchRunTomoDialog parent,
    final Map<DirectiveDef, String> templateValues,
    final Set<DirectiveDef> basicDirectives, final BrowsingDirectory browsingDir,
    final String stackID) {
    this.manager = manager;
    this.datasetFile = datasetFile;
    this.row = row;
    this.emptyTable = emptyTable;
    this.parent = parent;
    this.templateValues = templateValues;
    this.basicDirectives = basicDirectives;
    this.browsingDir = browsingDir;
    this.stackID = stackID;
    this.global = global;
    CTF_RANGE_FIT_EVERY_IMAGE.set(1);
    CTF_STEP_FIT_EVERY_IMAGE.set(0);
    File distortionDir =
      DatasetFiles.getDistortionDir(manager, manager.getPropertyUserDir(), AXIS_ID);
    if (distortionDir != null && distortionDir.exists()) {
      ftfDistort =
        FileTextField2.getAltLayoutInstance(manager, "Image distortion file: ");
      ftfGradient = FileTextField2.getAltLayoutInstance(manager, "Mag gradient file: ");
    }
    else {
      ftfDistort = null;
      ftfGradient = null;
      shiftBasicButton = true;
    }
    ftfModelFile =
      FileTextField2.getAltLayoutInstance(manager, "Manual replacement model: ");
    phPostprocessing =
      PanelHeader.getInstance("Postprocessing", this, DialogType.BATCH_RUN_TOMO);
    if (global) {
      frame = null;
      menuFitWindow = null;
      btnOk = null;
      btnRevertToGlobal = null;
    }
    else {
      frame = new Frame(this);
      menuFitWindow = new MenuItem("Fit Window", KeyEvent.VK_F);
      phRoot.setText(" Dataset Values");
      btnOk = new SingleLineButton("OK");
      btnRevertToGlobal = new SingleLineButton("Revert to Global");
    }
  }

  static BatchRunTomoDatasetDialog getGlobalInstance(final BatchRunTomoManager manager,
    final BatchRunTomoDialog parent, final Map<DirectiveDef, String> templateValues,
    final Set<DirectiveDef> basicDirectives, final BrowsingDirectory browsingDir) {
    Utilities.timestamp("new", "BatchRunTomoDatasetDialog getGlobalInstance",
      Utilities.STARTED_STATUS);
    BatchRunTomoDatasetDialog instance = new BatchRunTomoDatasetDialog(manager, null,
      true, null, true, parent, templateValues, basicDirectives, browsingDir, null);
    instance.createPanel(true);
    instance.setTooltips();
    instance.addListeners();
    Utilities.timestamp("new", "BatchRunTomoDatasetDialog getGlobalInstance",
      Utilities.FINISHED_STATUS);
    return instance;
  }

  static BatchRunTomoDatasetDialog getRowInstance(final BatchRunTomoManager manager,
    final File datasetFile, final BatchRunTomoRow row,
    final Map<DirectiveDef, String> templateValues,
    final Set<DirectiveDef> basicDirectives, final BrowsingDirectory browsingDir,
    final String stackID) {
    Utilities.timestamp("new", "BatchRunTomoDatasetDialog getRowInstance",
      Utilities.STARTED_STATUS);
    BatchRunTomoDatasetDialog instance =
      new BatchRunTomoDatasetDialog(manager, datasetFile, false, row, false, null,
        templateValues, basicDirectives, browsingDir, stackID);
    instance.createPanel(false);
    instance.setTooltips();
    instance.addListeners();
    instance.setVisible(true);
    Utilities.timestamp("new", "BatchRunTomoDatasetDialog getRowInstance",
      Utilities.FINISHED_STATUS);
    return instance;
  }

  static BatchRunTomoDatasetDialog getSavedRowInstance(final BatchRunTomoManager manager,
    final File datasetFile, final BatchRunTomoRow row,
    final Map<DirectiveDef, String> templateValues,
    final Set<DirectiveDef> basicDirectives, final BrowsingDirectory browsingDir,
    final String stackID) {
    BatchRunTomoDatasetDialog instance =
      new BatchRunTomoDatasetDialog(manager, datasetFile, false, row, false, null,
        templateValues, basicDirectives, browsingDir, stackID);
    instance.createPanel(false);
    instance.setTooltips();
    instance.addListeners();
    return instance;
  }

  boolean isFindSecAddThicknessSet() {
    return ctfFindSecAddThickness.isSelected() && !ctfFindSecAddThickness.isEmpty();
  }

  boolean isScaleFromZSet() {
    return ctfScaleFromZ.isSelected() && !ctfScaleFromZ.isEmpty();
  }

  boolean hasDual() {
    if (row != null) {
      return row.isDual();
    }
    return parent.hasDual();
  }

  private void createPanel(final boolean global) {
    // local panels
    JPanel pnlModelFile = new JPanel();
    JPanel pnlTrackingMethod = new JPanel();
    JPanel pnlGold = new JPanel();
    JPanel pnlSizeOfPatchesXandY = new JPanel();
    JPanel pnlBinByFactor = new JPanel();
    JPanel pnlAlignedStack = new JPanel();
    JPanel pnlReconstruction = new JPanel();
    JPanel pnlReconstructionType = new JPanel();
    JPanel pnlThickness = new JPanel();
    JPanel pnlAutoFitRangeAndStep = new JPanel();
    JPanel pnlButtons = null;
    if (frame != null) {
      pnlButtons = new JPanel();
    }
    JPanel pnlRemoveXrays = new JPanel();
    JPanel pnlEnableStretching = new JPanel();
    JPanel pnlLocalAlignments = new JPanel();
    JPanel pnlPreBinByFactor = new JPanel();
    JPanel pnlPostprocessing = new JPanel();
    JPanel pnlDoTrimvol = new JPanel();
    JPanel pnlTrimvol = new JPanel();
    JPanel pnlSampleType = new JPanel();
    JPanel pnlSampleTypeChooser = new JPanel();
    JPanel pnlSampleTypeData = new JPanel();
    JPanel pnlDistort = null;
    if (ftfDistort != null) {
      pnlDistort = new JPanel();
    }
    // init
    rbTrackingMethodSeed.setSelected(true);
    btnModelFile.setToPreferredSize();
    if (btnRevertToGlobal != null) {
      btnRevertToGlobal.setToPreferredSize();
    }
    if (btnOk != null) {
      btnOk.setToPreferredSize();
    }
    if (ftfDistort != null) {
      ftfDistort.setOrigin(ConfigTool.getDistortionDir(manager, null));
      ftfDistort.setAbsolutePath(true);
    }
    if (ftfGradient != null) {
      ftfGradient.setPreferredWidth(272);
      ftfGradient.setOrigin(ConfigTool.getDistortionDir(manager, null));
      ftfGradient.setAbsolutePath(true);
    }
    ftfModelFile.setAbsolutePath(true);
    rtfAutoFitRangeAndStep.setRequired(true);
    ltfAutoFitStep.setRequired(true);
    ltfDefocus.setRequired(true);
    ltfGold.setRequired(true);
    ltfPositioningGold.setRequired(true);
    ltfTargetNumberOfBeads.setRequired(true);
    ltfSizeOfPatchesXandY.setRequired(true);
    ltfFakeSIRTiterations.setRequired(true);
    ltfLeaveIterations.setRequired(true);
    ltfFallbackThickness.setRequired(true);
    rtfThickness.setRequired(true);
    rtfBinnedThickness.setRequired(true);
    ctfScaleFromZ.setText(BatchRunTomoDatasetMetaData.SCALE_FROM_Z_DEFAULT);
    phPostprocessing.setOpen(false);
    ctfFindSecAddThickness.setRequired(true);
    rbEraseGoldFid.setSelected(true);
    rtfAutoFitRangeAndStep.setSelected(true);
    cbDoBackprojAlso.setSelected(true);
    // Set directive defs where there is a one-to-one correspondence between
    // field and
    // directive. The directive def is used for setting the default in comparam
    // directives, and saving fields to autodoc files.
    //
    // IMPORTANT: Code currently assumes that all defaults (for comparam
    // directive fields)
    // can be set generically.
    setupField(ftfDistort, DirectiveDef.DISTORT);
    setupField(ftfGradient, DirectiveDef.GRADIENT);
    setupField(cbRemoveXrays, DirectiveDef.REMOVE_XRAYS);
    setupField(ftfModelFile, DirectiveDef.MODEL_FILE);
    // manual - also affects seedingMethod
    setupField(rbTrackingMethodSeed, DirectiveDef.TRACKING_METHOD,
      DirectiveDef.SEEDING_METHOD);
    setupField(rbTrackingMethodRaptor, DirectiveDef.TRACKING_METHOD);
    setupField(rbTrackingMethodPatchTracking, DirectiveDef.TRACKING_METHOD);
    setupField(rbFiducialless, DirectiveDef.FIDUCIALLESS);
    setupField(ltfGold, DirectiveDef.GOLD);
    // Hiding targetDensityOfBeads
    setupField(ltfTargetNumberOfBeads, DirectiveDef.TARGET_NUMBER_OF_BEADS,
      DirectiveDef.TARGET_DENSITY_OF_BEADS);
    setupField(ltfSizeOfPatchesXandY, DirectiveDef.SIZE_OF_PATCHES_X_AND_Y);
    setupField(cbLengthOfPieces, DirectiveDef.LENGTH_OF_PIECES);
    setupField(cbEnableStretching, DirectiveDef.ENABLE_STRETCHING);
    setupField(cbLocalAlignments, DirectiveDef.LOCAL_ALIGNMENTS);
    setupField(lsBinByFactor, DirectiveDef.BIN_BY_FACTOR_FOR_ALIGNED_STACK);
    setupField(cbCorrectCTF, DirectiveDef.CORRECT_CTF);
    setupField(ltfDefocus, DirectiveDef.DEFOCUS);
    setupField(rtfAutoFitRangeAndStep, DirectiveDef.AUTO_FIT_RANGE_AND_STEP);// manual
    setupField(ltfAutoFitStep, DirectiveDef.AUTO_FIT_RANGE_AND_STEP);// manual
    setupField(rbFitEveryImage, DirectiveDef.AUTO_FIT_RANGE_AND_STEP);// manual
    setupField(cbSampleType, DirectiveDef.SAMPLE_TYPE);
    setupField(cbDoBackprojAlso, DirectiveDef.DO_BACKPROJ_ALSO);
    setupField(cbFakeSIRTiterations, DirectiveDef.FAKE_SIRT_ITERATIONS);
    setupField(ltfFakeSIRTiterations, DirectiveDef.FAKE_SIRT_ITERATIONS);
    setupField(cbUseSirt, DirectiveDef.USE_SIRT);
    setupField(ltfLeaveIterations, DirectiveDef.LEAVE_ITERATIONS);
    setupField(cbScaleToInteger, DirectiveDef.SCALE_TO_INTEGER);
    setupField(rtfThickness, DirectiveDef.THICKNESS_FOR_TILT);// manual
    setupField(rtfBinnedThickness, DirectiveDef.BINNED_THICKNESS);// manual
    setupField(rbFallbackAndExtraThickness, DirectiveDef.FALLBACK_THICKNESS,
      DirectiveDef.EXTRA_THICKNESS);
    setupField(ltfExtraThickness, DirectiveDef.EXTRA_THICKNESS);// manual
    setupField(ltfFallbackThickness, DirectiveDef.FALLBACK_THICKNESS);// manual
    setupField(lsPrenewstBinByFactor, DirectiveDef.BIN_BY_FACTOR_FOR_PRENEWST);
    setupField(lsPreblendBinByFactor, DirectiveDef.BIN_BY_FACTOR_FOR_PREBLEND);
    setupField(cbDoTrimvol, DirectiveDef.DO_TRIMVOL);
    setupField(ctfFindSecAddThickness, DirectiveDef.FIND_SEC_ADD_THICKNESS);
    setupField(ctfScaleFromZ, DirectiveDef.SCALE_FROM_Z);
    setupField(cbEraseGold, DirectiveDef.ERASE_GOLD);
    setupField(rbEraseGoldFid, DirectiveDef.ERASE_GOLD);
    setupField(rbEraseGold3d, DirectiveDef.ERASE_GOLD);
    setupField(ltfGoldErasingThickness, DirectiveDef.THICKNESS_FOR_GOLD_ERASING);
    setupField(rbSampleTypePlasticSection, DirectiveDef.SAMPLE_TYPE);
    setupField(rbSampleTypeCryo, DirectiveDef.SAMPLE_TYPE);
    setupField(ltfPositioningThickness, DirectiveDef.THICKNESS_FOR_POSITIONING);
    setupField(cbHasGoldBeads, DirectiveDef.HAS_GOLD_BEADS);
    setupField(ltfPositioningGold, DirectiveDef.GOLD);
    // defaults
    setIndividualDefaults();
    if (frame != null) {
      JScrollPane scrollPane = new JScrollPane(pnlRoot);
      scrollPane.getVerticalScrollBar().setUnitIncrement(16);
      // construct
      JMenuBar menuBar = new JMenuBar();
      JMenu menu = new Menu("View");
      // frame
      frame.add(scrollPane);
      frame.setJMenuBar(menuBar);
      // frame.add(pnlRoot);
      // menuBar
      menuBar.add(menu);
      // menu
      menu.setMnemonic(KeyEvent.VK_V);
      if (menuFitWindow != null) {
        menu.add(menuFitWindow);
        // menuFitWindow
        menuFitWindow
          .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
      }
      // title
      if (datasetFile != null) {
        String file = datasetFile.getName();
        String name = "";
        if (file != null) {
          int index = file.lastIndexOf(".");
          if (index != -1) {
            name = file.substring(0, index);
          }
          else {
            name = file;
          }
        }
        File dir = datasetFile.getParentFile();
        String dirName = null;
        if (dir != null) {
          dirName = dir.getName();
        }
        frame.setTitle((dirName != null ? dirName + File.separatorChar : "") + name);
      }
    }
    // root
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
    pnlRoot.setBorder(BorderFactory.createEtchedBorder());
    pnlRoot.add(phRoot.getContainer());
    pnlRoot.add(Box.createRigidArea(FixedDim.x0_y2));
    pnlRoot.add(pnlRootBody);
    // root body
    pnlRootBody.setLayout(new BoxLayout(pnlRootBody, BoxLayout.Y_AXIS));
    pnlRootBody.add(pnlBasic);
    // Basic
    pnlBasic.setLayout(new BoxLayout(pnlBasic, BoxLayout.Y_AXIS));
    if (pnlDistort != null) {
      pnlBasic.add(pnlDistort);
    }
    if (ftfGradient != null) {
      pnlBasic.add(ftfGradient.getRootPanel());
    }
    pnlBasic.add(Box.createRigidArea(FixedDim.x0_y3));
    pnlBasic.add(sDistortion);
    pnlBasic.add(pnlRemoveXrays);
    pnlBasic.add(pnlModelFile);
    pnlBasic.add(Box.createRigidArea(FixedDim.x0_y3));
    pnlBasic.add(new JSeparator());
    pnlBasic.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlBasic.add(pnlPreBinByFactor);
    pnlBasic.add(Box.createRigidArea(FixedDim.x0_y10));
    pnlBasic.add(pnlTrackingMethod);
    pnlBasic.add(Box.createRigidArea(FixedDim.x0_y3));
    pnlBasic.add(pnlGold);
    pnlBasic.add(pnlSizeOfPatchesXandY);
    pnlBasic.add(Box.createRigidArea(FixedDim.x0_y10));
    pnlBasic.add(pnlEnableStretching);
    pnlBasic.add(pnlLocalAlignments);
    pnlBasic.add(Box.createRigidArea(FixedDim.x0_y1));
    pnlBasic.add(new JSeparator());
    pnlBasic.add(Box.createRigidArea(FixedDim.x0_y3));
    pnlBasic.add(pnlSampleType);
    pnlBasic.add(Box.createRigidArea(FixedDim.x0_y3));
    pnlBasic.add(new JSeparator());
    pnlBasic.add(Box.createRigidArea(FixedDim.x0_y3));
    pnlBasic.add(pnlBinByFactor);
    pnlBasic.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlBasic.add(pnlAlignedStack);
    pnlBasic.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlBasic.add(pnlReconstruction);
    pnlBasic.add(pnlPostprocessing);
    if (pnlButtons != null) {
      pnlBasic.add(pnlButtons);
    }
    // Distort
    if (pnlDistort != null) {
      pnlDistort.setLayout(new BoxLayout(pnlDistort, BoxLayout.X_AXIS));
      pnlDistort.add(ftfDistort.getRootPanel());
      pnlDistort.add(btnAdvanced.getComponent());
    }

    // RemoveXrays
    pnlRemoveXrays.setLayout(new BoxLayout(pnlRemoveXrays, BoxLayout.X_AXIS));
    pnlRemoveXrays.add(cbRemoveXrays.getComponent());
    pnlRemoveXrays.add(Box.createHorizontalGlue());
    // Alternate place to put the advanced button.
    if (pnlDistort == null) {
      pnlRemoveXrays.add(btnAdvanced.getComponent());
    }
    // ModelFile
    pnlModelFile.setLayout(new BoxLayout(pnlModelFile, BoxLayout.X_AXIS));
    pnlModelFile.add(ftfModelFile.getRootPanel());
    pnlModelFile.add(spaceModelFile.getComponent());
    pnlModelFile.add(btnModelFile.getComponent());
    pnlModelFile.add(Box.createHorizontalGlue());
    // PreBinByFactor
    pnlPreBinByFactor.setLayout(new BoxLayout(pnlPreBinByFactor, BoxLayout.X_AXIS));
    pnlPreBinByFactor.add(lsPrenewstBinByFactor.getContainer());
    pnlPreBinByFactor.add(Box.createRigidArea(FixedDim.x15_y0));
    pnlPreBinByFactor.add(lsPreblendBinByFactor.getContainer());
    // TrackingMethod
    pnlTrackingMethod.setLayout(new GridLayout(2, 2, 0, 0));
    pnlTrackingMethod.setBorder(new EtchedBorder("Alignment Method").getBorder());
    pnlTrackingMethod.add(rbTrackingMethodSeed.getComponent());
    pnlTrackingMethod.add(rbTrackingMethodPatchTracking.getComponent());
    pnlTrackingMethod.add(rbTrackingMethodRaptor.getComponent());
    pnlTrackingMethod.add(rbFiducialless.getComponent());
    // Gold
    pnlGold.setLayout(new GridLayout(1, 2, 15, 1));
    pnlGold.add(ltfGold.getComponent());
    pnlGold.add(ltfTargetNumberOfBeads.getComponent());
    // SizeOfPatchesXandY
    pnlSizeOfPatchesXandY.setLayout(new GridLayout(1, 2, 15, 0));
    pnlSizeOfPatchesXandY.add(ltfSizeOfPatchesXandY.getComponent());
    pnlSizeOfPatchesXandY.add(cbLengthOfPieces.getComponent());
    // EnableStretching
    pnlEnableStretching.setLayout(new BoxLayout(pnlEnableStretching, BoxLayout.X_AXIS));
    pnlEnableStretching.add(cbEnableStretching.getComponent());
    pnlEnableStretching.add(Box.createHorizontalGlue());
    // LocalAlignments
    pnlLocalAlignments.setLayout(new BoxLayout(pnlLocalAlignments, BoxLayout.X_AXIS));
    pnlLocalAlignments.add(cbLocalAlignments.getComponent());
    pnlLocalAlignments.add(Box.createHorizontalGlue());
    // SampleType
    pnlSampleType.setLayout(new BoxLayout(pnlSampleType, BoxLayout.Y_AXIS));
    pnlSampleType.add(pnlSampleTypeChooser);
    pnlSampleType.add(Box.createRigidArea(FixedDim.x0_y3));
    pnlSampleType.add(pnlSampleTypeData);
    // SampleTypeChooser
    pnlSampleTypeChooser.setLayout(new BoxLayout(pnlSampleTypeChooser, BoxLayout.X_AXIS));
    pnlSampleTypeChooser.add(cbSampleType.getComponent());
    pnlSampleTypeChooser.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlSampleTypeChooser.add(rbSampleTypePlasticSection.getComponent());
    pnlSampleTypeChooser.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlSampleTypeChooser.add(rbSampleTypeCryo.getComponent());
    pnlSampleTypeChooser.add(Box.createHorizontalGlue());
    // SampleTypeData
    pnlSampleTypeData.setLayout(new GridLayout(1, 3, 15, 5));
    pnlSampleTypeData.add(ltfPositioningThickness.getComponent());
    pnlSampleTypeData.add(cbHasGoldBeads.getComponent());
    pnlSampleTypeData.add(ltfPositioningGold.getComponent());
    // BinByFactor
    pnlBinByFactor.setLayout(new BoxLayout(pnlBinByFactor, BoxLayout.X_AXIS));
    pnlBinByFactor.add(lsBinByFactor.getContainer());
    pnlBinByFactor.add(Box.createHorizontalGlue());
    // AlignedStack
    pnlAlignedStack.setLayout(new GridLayout(4, 2, 30, 0));
    pnlAlignedStack.add(cbCorrectCTF.getComponent());
    pnlAlignedStack.add(cbEraseGold.getComponent());
    pnlAlignedStack.add(pnlAutoFitRangeAndStep);
    pnlAlignedStack.add(rbEraseGoldFid.getComponent());
    pnlAlignedStack.add(rbFitEveryImage.getComponent());
    pnlAlignedStack.add(rbEraseGold3d.getComponent());
    pnlAlignedStack.add(ltfDefocus.getComponent());
    pnlAlignedStack.add(ltfGoldErasingThickness.getComponent());
    // AutoFitRangeAndStep
    pnlAutoFitRangeAndStep
      .setLayout(new BoxLayout(pnlAutoFitRangeAndStep, BoxLayout.X_AXIS));
    pnlAutoFitRangeAndStep.add(rtfAutoFitRangeAndStep.getContainer());
    pnlAutoFitRangeAndStep.add(ltfAutoFitStep.getComponent());
    // Reconstruction
    pnlReconstruction.setLayout(new BoxLayout(pnlReconstruction, BoxLayout.X_AXIS));
    pnlReconstruction.setBorder(new EtchedBorder("Reconstruction").getBorder());
    pnlReconstruction.add(pnlReconstructionType);
    pnlReconstruction.add(Box.createRigidArea(FixedDim.x12_y0));
    pnlReconstruction.add(pnlThickness);
    // ReconstructionType
    pnlReconstructionType
      .setLayout(new BoxLayout(pnlReconstructionType, BoxLayout.Y_AXIS));
    pnlReconstructionType.add(cbDoBackprojAlso.getComponent());
    pnlReconstructionType.add(cbFakeSIRTiterations.getComponent());
    pnlReconstructionType.add(cbUseSirt.getComponent());
    pnlReconstructionType.add(Box.createRigidArea(FixedDim.x0_y6));
    pnlReconstructionType.add(ltfFakeSIRTiterations.getComponent());
    pnlReconstructionType.add(ltfLeaveIterations.getComponent());
    pnlReconstructionType.add(cbScaleToInteger.getComponent());
    // Thickness
    pnlThickness.setLayout(new BoxLayout(pnlThickness, BoxLayout.Y_AXIS));
    pnlThickness.add(rtfThickness.getContainer());
    pnlThickness.add(Box.createRigidArea(FixedDim.x0_y2));
    pnlThickness.add(rtfBinnedThickness.getContainer());
    pnlThickness.add(Box.createRigidArea(FixedDim.x0_y2));
    pnlThickness.add(rbFallbackAndExtraThickness.getComponent());
    pnlThickness.add(ltfFallbackThickness.getComponent());
    pnlThickness.add(ltfExtraThickness.getComponent());
    // Postprocessing
    pnlPostprocessing.setLayout(new BoxLayout(pnlPostprocessing, BoxLayout.Y_AXIS));
    pnlPostprocessing.setBorder(BorderFactory.createEtchedBorder());
    pnlPostprocessing.add(phPostprocessing.getContainer());
    pnlPostprocessing.add(pnlPostprocessingBody);
    // PostprocessingBody
    pnlPostprocessingBody
      .setLayout(new BoxLayout(pnlPostprocessingBody, BoxLayout.Y_AXIS));
    pnlPostprocessingBody.add(pnlDoTrimvol);
    pnlPostprocessingBody.add(pnlTrimvol);
    // DoTrimvol
    pnlDoTrimvol.setLayout(new BoxLayout(pnlDoTrimvol, BoxLayout.X_AXIS));
    pnlDoTrimvol.add(cbDoTrimvol.getComponent());
    pnlDoTrimvol.add(Box.createHorizontalGlue());
    // Trimvol
    pnlTrimvol.setLayout(new BoxLayout(pnlTrimvol, BoxLayout.X_AXIS));
    pnlTrimvol.add(ctfFindSecAddThickness.getComponent());
    pnlTrimvol.add(Box.createRigidArea(FixedDim.x3_y0));
    pnlTrimvol.add(ctfScaleFromZ.getComponent());
    // buttons
    if (pnlButtons != null) {
      pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
      pnlButtons.add(Box.createHorizontalGlue());
      if (btnOk != null) {
        pnlButtons.add(btnOk.getComponent());
      }
      pnlButtons.add(Box.createRigidArea(FixedDim.x20_y0));
      if (btnRevertToGlobal != null) {
        pnlButtons.add(btnRevertToGlobal.getComponent());
      }
    }
    // align
    UIUtilities.alignComponentsX(pnlRoot, Component.LEFT_ALIGNMENT);
    UIUtilities.alignComponentsX(pnlRootBody, Component.LEFT_ALIGNMENT);
    UIUtilities.alignComponentsX(pnlReconstructionType, Component.LEFT_ALIGNMENT);
    UIUtilities.alignComponentsX(pnlThickness, Component.LEFT_ALIGNMENT);
    // update
    updateDisplay();
    updateAdvanced(false);
    statusChanged(status);
    // display
    pack();
  }

  private void setupField(final Field field, final DirectiveDef directiveDef) {
    setupField(field, directiveDef, null);
  }

  private void setupField(final Field field, final DirectiveDef directiveDef,
    final DirectiveDef directiveDef2) {
    if (field == null) {
      return;
    }
    field.setDirectiveDef(directiveDef);
    fieldList.add(field);
    if (directiveDef != null && !basicDirectives.contains(directiveDef)) {
      basicDirectives.add(directiveDef);
    }
    if (directiveDef2 != null && !basicDirectives.contains(directiveDef2)) {
      basicDirectives.add(directiveDef2);
    }
  }

  boolean isAdvancedDialogExists() {
    return directivesDialog != null;
  }

  boolean isTrackingMethodSeed() {
    return rbTrackingMethodSeed.isSelected();
  }

  int getPreferredWidth() {
    return ftfModelFile.getPreferredWidth() + spaceModelFile.getPreferredWidth()
      + btnModelFile.getPreferredWidth();
  }

  void setVisible(final boolean visible) {
    if (frame != null) {
      frame.setVisible(visible);
    }
  }

  private void addListeners() {
    cbRemoveXrays.addActionListener(this);
    rbTrackingMethodSeed.addActionListener(this);
    rbTrackingMethodRaptor.addActionListener(this);
    rbTrackingMethodPatchTracking.addActionListener(this);
    rbFiducialless.addActionListener(this);
    cbCorrectCTF.addActionListener(this);
    rtfAutoFitRangeAndStep.addActionListener(this);
    rbFitEveryImage.addActionListener(this);
    cbDoBackprojAlso.addActionListener(this);
    cbFakeSIRTiterations.addActionListener(this);
    cbUseSirt.addActionListener(this);
    rtfThickness.addActionListener(this);
    rtfBinnedThickness.addActionListener(this);
    rbFallbackAndExtraThickness.addActionListener(this);
    if (btnOk != null) {
      btnOk.addActionListener(this);
    }
    if (btnRevertToGlobal != null) {
      btnRevertToGlobal.addActionListener(this);
    }
    btnModelFile.addActionListener(this);
    cbDoTrimvol.addActionListener(this);
    cbEraseGold.addActionListener(this);
    rbEraseGoldFid.addActionListener(this);
    rbEraseGold3d.addActionListener(this);
    cbSampleType.addActionListener(this);
    rbSampleTypePlasticSection.addActionListener(this);
    rbSampleTypeCryo.addActionListener(this);
    cbHasGoldBeads.addActionListener(this);
    btnAdvanced.addActionListener(this);
    btnBasic.addActionListener(this);
    if (menuFitWindow != null) {
      menuFitWindow.addActionListener(this);
    }
  }

  public Component getComponent() {
    return pnlRoot;
  }

  public SwingComponent getUIComponent() {
    return this;
  }

  public void expand(final ExpandButton button) {
    boolean expanded = button.isExpanded();
    if (phRoot.equalsOpenClose(button)) {
      pnlRootBody.setVisible(expanded);
    }
    else if (phPostprocessing.equalsOpenClose(button)) {
      pnlPostprocessingBody.setVisible(expanded);
    }
    pack();
  }

  private void pack() {
    if (frame == null) {
      UIHarness.INSTANCE.pack(manager);
    }
    else {
      frame.pack();
    }
  }

  public void expand(final GlobalExpandButton button) {}

  boolean validate() {
    if (rbSampleTypeCryo.isSelected() && ltfPositioningThickness.isEmpty()) {
      display();
      UIHarness.INSTANCE.openMessageDialog(manager, ltfPositioningThickness,
        ltfPositioningThickness.getQuotedLabel() + " is required.", "Required Field");
      return false;
    }
    if (directivesDialog != null) {
      return directivesDialog.validate(advancedFieldDisplayer);
    }
    return true;
  }

  boolean validate(final boolean surfacesToAnalyze2) {
    if (!surfacesToAnalyze2 && ltfGoldErasingThickness.isEnabled()
      && ltfGoldErasingThickness.isEmpty()) {
      display();
      UIHarness.INSTANCE.openMessageDialog(manager, ltfGoldErasingThickness,
        ltfGoldErasingThickness.getQuotedLabel() + " is required.", "Required Field");
      return false;
    }
    return true;
  }

  private void updateAdvanced(final boolean advanced) {
    if (advanced) {
      if (directivesDialog == null) {
        // Save the state so it can be loaded into the advanced dialog. Don't save all
        // the dataset autodocs as this might be too slow. If this is the global dialog
        // then stackID will be null and not dataset autodocs will be saved.
        manager.saveBatchRunTomoDialog(false, stackID, true);
        Utilities.timestamp("new", "DirectivesDialog", Utilities.STARTED_STATUS);
        directivesDialog =
          DirectivesDialog.getDialogInstance(manager, this, templateValues,
            basicDirectives, btnBasic, shiftBasicButton, browsingDir, global);
        manager.initDialog(stackID, true);
      }
      pnlRootBody.removeAll();
      pnlRootBody.add(directivesDialog.getComponent());
    }
    else {
      pnlRootBody.removeAll();
      pnlRootBody.add(pnlBasic);
    }
    this.advanced = advanced;
    if (parent != null) {
      parent.setDatasetTableVisible(!advanced);
    }
    pack();
    Utilities.timestamp("new", "DirectivesDialog", Utilities.FINISHED_STATUS);
  }

  DirectivesDialog getDirectivesDialog() {
    return directivesDialog;
  }

  private void updateDisplay() {
    boolean removeXrays = cbRemoveXrays.isSelected();
    ftfModelFile.setEnabled(removeXrays);
    btnModelFile.setEnabled(removeXrays && !emptyTable);
    boolean fiducialless = rbFiducialless.isSelected();
    cbEnableStretching.setEnabled(!fiducialless);
    cbLocalAlignments.setEnabled(!fiducialless);
    boolean beadTracking =
      rbTrackingMethodSeed.isSelected() || rbTrackingMethodRaptor.isSelected();
    ltfGold.setEnabled(beadTracking);
    ltfTargetNumberOfBeads.setEnabled(beadTracking);
    boolean sampleType = cbSampleType.isSelected();
    rbSampleTypePlasticSection.setEnabled(sampleType);
    rbSampleTypeCryo.setEnabled(sampleType);
    boolean sampleTypeCryo = sampleType && rbSampleTypeCryo.isSelected();
    cbHasGoldBeads.setEnabled(!beadTracking && sampleTypeCryo);
    ltfPositioningGold
      .setEnabled(!beadTracking && sampleTypeCryo && cbHasGoldBeads.isSelected());
    // Set gold and positioning values
    if (beadTracking) {
      cbHasGoldBeads.setSelected(true);
    }
    // Transfer gold size from active field to inactive field. ltfGold is active when
    // fiducialModelMode is true. Positioning gold is active when it is false. Once the
    // gold is transfered, reset the mode.
    if (fiducialModelMode) {
      ltfPositioningGold.setText(ltfGold.getText());
    }
    else {
      ltfGold.setText(ltfPositioningGold.getText());
    }
    fiducialModelMode =
      rbTrackingMethodSeed.isSelected() || rbTrackingMethodRaptor.isSelected();
    boolean patchTracking = rbTrackingMethodPatchTracking.isSelected();
    ltfSizeOfPatchesXandY.setEnabled(patchTracking);
    cbLengthOfPieces.setEnabled(patchTracking);
    boolean ctf = cbCorrectCTF.isSelected();
    ltfDefocus.setEnabled(ctf);
    rtfAutoFitRangeAndStep.setEnabled(ctf);
    rbFitEveryImage.setEnabled(ctf);
    boolean autoFitRangeAndStep = ctf && rtfAutoFitRangeAndStep.isSelected();
    ltfAutoFitStep.setEnabled(autoFitRangeAndStep);
    ltfFakeSIRTiterations.setEnabled(cbFakeSIRTiterations.isSelected());
    boolean useSirt = cbUseSirt.isSelected();
    ltfFakeSIRTiterations.setVisible(!useSirt);
    ltfLeaveIterations.setVisible(useSirt);
    ltfLeaveIterations.setEnabled(useSirt);
    cbScaleToInteger.setEnabled(useSirt);
    boolean fallbackAndExtraThickness = rbFallbackAndExtraThickness.isSelected();
    ltfExtraThickness.setEnabled(fallbackAndExtraThickness);
    ltfFallbackThickness.setEnabled(fallbackAndExtraThickness);
    boolean doTrimvol = cbDoTrimvol.isSelected();
    ctfFindSecAddThickness.setEnabled(doTrimvol);
    ctfScaleFromZ.setEnabled(doTrimvol);
    boolean eraseGold = cbEraseGold.isSelected();
    rbEraseGoldFid.setEnabled(eraseGold && !fiducialless && !patchTracking);
    rbEraseGold3d.setEnabled(eraseGold);
    if (eraseGold && (fiducialless || patchTracking)) {
      rbEraseGold3d.setSelected(true);
    }
    ltfGoldErasingThickness.setEnabled(eraseGold && rbEraseGold3d.isSelected());
  }

  void statusChanged(final BatchRunTomoStatus status) {
    this.status = status;
    boolean open = status == null || status == BatchRunTomoStatus.OPEN;
    cbRemoveXrays.setEditable(open);
    rbTrackingMethodSeed.setEditable(open);
    rbTrackingMethodRaptor.setEditable(open);
    rbTrackingMethodPatchTracking.setEditable(open);
    rbFiducialless.setEditable(open);
    lsBinByFactor.setEditable(open);
    cbCorrectCTF.setEditable(open);
    cbDoBackprojAlso.setEditable(open);
    cbFakeSIRTiterations.setEditable(open);
    ltfFakeSIRTiterations.setEditable(open);
    cbUseSirt.setEditable(open);
    rtfThickness.setEditable(open);
    rtfBinnedThickness.setEditable(open);
    rbFallbackAndExtraThickness.setEditable(open);
    if (btnOk != null) {
      btnOk.setEditable(open);
    }
    if (btnRevertToGlobal != null) {
      btnRevertToGlobal.setEditable(open);
    }
    if (ftfDistort != null) {
      ftfDistort.setEditable(open);
    }
    if (ftfGradient != null) {
      ftfGradient.setEditable(open);
    }
    ftfModelFile.setEditable(open);
    btnModelFile.setEditable(open);
    cbEnableStretching.setEditable(open);
    cbLocalAlignments.setEditable(open);
    ltfGold.setEditable(open);
    ltfTargetNumberOfBeads.setEditable(open);
    ltfSizeOfPatchesXandY.setEditable(open);
    cbLengthOfPieces.setEditable(open);
    ltfDefocus.setEditable(open);
    rtfAutoFitRangeAndStep.setEditable(open);
    rbFitEveryImage.setEditable(open);
    cbSampleType.setEditable(open);
    ltfAutoFitStep.setEditable(open);
    ltfLeaveIterations.setEditable(open);
    cbScaleToInteger.setEditable(open);
    ltfExtraThickness.setEditable(open);
    ltfFallbackThickness.setEditable(open);
    lsPrenewstBinByFactor.setEditable(open);
    lsPreblendBinByFactor.setEditable(open);
    cbDoTrimvol.setEditable(open);
    ctfFindSecAddThickness.setEditable(open);
    ctfScaleFromZ.setEditable(open);
    cbEraseGold.setEditable(open);
    rbEraseGoldFid.setEditable(open);
    rbEraseGold3d.setEditable(open);
    ltfGoldErasingThickness.setEditable(open);
    rbSampleTypePlasticSection.setEditable(open);
    rbSampleTypeCryo.setEditable(open);
    ltfPositioningThickness.setEditable(open);
    cbHasGoldBeads.setEditable(open);
    ltfPositioningGold.setEditable(open);
    if (directivesDialog != null) {
      directivesDialog.statusChanged(this.status);
    }
  }

  void statusChanged(final StatusChangeEvent statusChangeEvent) {
    if (statusChangeEvent == null
      || !(statusChangeEvent instanceof StatusChangeBooleanEvent)) {
      return;
    }
    StatusChangeBooleanEvent event = (StatusChangeBooleanEvent) statusChangeEvent;
    Status status = event.getStatus();
    if (status == null || !(status instanceof FrameStatus)) {
      return;
    }
    if (status == FrameStatus.SINGLE) {
      lsPrenewstBinByFactor.setEnabled(event.is());
    }
    else if (status == FrameStatus.MONTAGE) {
      lsPreblendBinByFactor.setEnabled(event.is());
    }
  }

  /**
   * Check isDifferentFromCheckpoint on all data entry fields
   * 
   * @param onlyAdvancedDatasetDialog only loading the advanced dataset dialog
   *
   * @return true if any field's isDifferentFromCheckpoint function returned true
   */
  // TODO 2103
  boolean backupIfChanged(final boolean onlyAdvancedDatasetDialog) {
    boolean changed = false;
    if (!onlyAdvancedDatasetDialog) {
      int len = fieldList.size();
      for (int i = 0; i < len; i++) {
        Field field = fieldList.get(i);
        if (field.isDifferentFromCheckpoint(true)) {
          field.backup();
          changed = true;
        }
      }
    }
    // TODO 2103
    if (onlyAdvancedDatasetDialog && directivesDialog != null) {
      directivesDialog.backupIfChanged();
    }
    return changed;
  }

  /**
   * Called when the template has changed
   *
   * @param directiveFileCollection - templates and base directive file
   * @param retainUserValues        - put the backed-up values back after values have
   *                                been applied
   * @param onlyAdvancedDatasetDialog only loading an advanced dataset dialog
   */
  void applyValues(final boolean init, final boolean retainUserValues,
    final DirectiveFileCollection directiveFileCollection,
    final boolean onlyAdvancedDatasetDialog) {
    // to apply values and highlights, start with a clean slate
    if (!onlyAdvancedDatasetDialog) {
      if (!init) {
        int len = fieldList.size();
        for (int i = 0; i < len; i++) {
          Field field = fieldList.get(i);
          field.clearFieldHighlight();
          field.clear();
        }
      }
    }
    if (directivesDialog != null) {
      directivesDialog.clearTemplateValues();
      directivesDialog.clear();
    }
    // Apply default values
    if (!onlyAdvancedDatasetDialog) {
      setIndividualDefaults();
    }
    if (!onlyAdvancedDatasetDialog) {
      int len = fieldList.size();
      for (int i = 0; i < len; i++) {
        fieldList.get(i).useDefaultValue();
        Field field = fieldList.get(i);
      }
    }
    // No settings values to apply
    // Apply the directive collection values
    setValues(directiveFileCollection, false, onlyAdvancedDatasetDialog, false);
    // Checkpoint and restore from backup
    if (!onlyAdvancedDatasetDialog) {
      int len = fieldList.size();
      for (int i = 0; i < len; i++) {
        Field field = fieldList.get(i);
        // checkpoint
        field.checkpoint();
        // If the user wants to retain their values, apply backed up values and
        // then
        // delete
        // them.
        if (retainUserValues) {
          field.restoreFromBackup();
        }
      }
    }
    if (directivesDialog != null) {
      directivesDialog.checkpointAndRestoreFromBackup(retainUserValues);
    }
    // Set new highlight values - batch directive file must be ignored
    setValues(directiveFileCollection, true, onlyAdvancedDatasetDialog, false);
  }

  /**
   * Sets coarse alignment binning based on a single row.
   * @param montage
   */
  void setMontage(final boolean montage) {
    lsPrenewstBinByFactor.setEnabled(!montage);
    lsPreblendBinByFactor.setEnabled(montage);
  }

  private void setIndividualDefaults() {
    rtfAutoFitRangeAndStep.setSelected(true);
  }

  void setParameters(final BatchRunTomoDatasetMetaData metaData) {
    phRoot.set(metaData.getHeader());
    phPostprocessing.set(metaData.getPostprocessingHeader());
    ftfModelFile.setText(metaData.getModelFile(), false);
    cbEnableStretching.setSelected(metaData.getEnableStretching(), false);
    cbLocalAlignments.setSelected(metaData.getLocalAlignments(), false);
    ltfGold.setText(metaData.getGold(), false);
    ltfTargetNumberOfBeads.setText(metaData.getTargetNumberOfBeads(), false);
    ltfSizeOfPatchesXandY.setText(metaData.getSizeOfPatchesXandY(), false);
    cbLengthOfPieces.setSelected(metaData.getLengthOfPieces(), false);
    ltfDefocus.setText(metaData.getDefocus(), false);
    rtfAutoFitRangeAndStep.setSelected(metaData.getAutoFitRangeAndStep(), false);
    rtfAutoFitRangeAndStep.setText(metaData.getAutoFitRange(), false);
    rbFitEveryImage.setSelected(metaData.getFitEveryImage(), false);
    ltfAutoFitStep.setText(metaData.getAutoFitStep(), false);
    cbFakeSIRTiterations.setSelected(metaData.isUseFakeSIRTiterations());
    ltfFakeSIRTiterations.setText(metaData.getFakeSIRTiterations(), false);
    ltfLeaveIterations.setText(metaData.getLeaveIterations(), false);
    cbScaleToInteger.setSelected(metaData.getScaleToInteger(), false);
    rtfThickness.setText(metaData.getThickness(), false);
    rtfBinnedThickness.setText(metaData.getBinnedThickness(), false);
    ltfExtraThickness.setText(metaData.getExtraThickness(), false);
    ltfFallbackThickness.setText(metaData.getFallbackThickness(), false);
    lsPrenewstBinByFactor.setValue(metaData.getPrenewstBinByFactor(), false);
    lsPreblendBinByFactor.setValue(metaData.getPreblendBinByFactor(), false);
    ctfFindSecAddThickness.setSelected(metaData.getUseFindSecAddThickness(), false);
    ctfFindSecAddThickness.setText(metaData.getFindSecAddThickness(), true);
    ctfScaleFromZ.setSelected(metaData.getUseScaleFromZ(), false);
    ctfScaleFromZ.setText(metaData.getScaleFromZ(), false);
    rbEraseGoldFid.setSelected(metaData.getEraseGoldFid(), false);
    rbEraseGold3d.setSelected(metaData.getEraseGold3d(), false);
    ltfGoldErasingThickness.setText(metaData.getGoldErasingThickness(), false);
    rbSampleTypePlasticSection.setSelected(metaData.getSampleTypePlasticSection(), false);
    rbSampleTypeCryo.setSelected(metaData.getSampleTypeCryo(), false);
    ltfPositioningThickness.setText(metaData.getPositioningThickness(), false);
    cbHasGoldBeads.setSelected(metaData.getHasGoldBeads(), false);
    ltfPositioningGold.setText(metaData.getPositioningGold(), false);
    updateDisplay();
    statusChanged(status);
  }

  void getParameters(final BatchRunTomoDatasetMetaData metaData) {
    metaData.setHeader(phRoot);
    metaData.setPostprocessingHeader(phPostprocessing);
    metaData.setModelFile(ftfModelFile.getFile());
    metaData.setEnableStretching(cbEnableStretching.isSelected());
    metaData.setLocalAlignments(cbLocalAlignments.isSelected());
    metaData.setGold(ltfGold.getText());
    metaData.setTargetNumberOfBeads(ltfTargetNumberOfBeads.getText());
    metaData.setSizeOfPatchesXandY(ltfSizeOfPatchesXandY.getText());
    metaData.setLengthOfPieces(cbLengthOfPieces.isSelected());
    metaData.setDefocus(ltfDefocus.getText());
    metaData.setAutoFitRangeAndStep(rtfAutoFitRangeAndStep.isSelected());
    metaData.setAutoFitRange(rtfAutoFitRangeAndStep.getText());
    metaData.setFitEveryImage(rbFitEveryImage.isSelected());
    metaData.setAutoFitStep(ltfAutoFitStep.getText());
    metaData.setUseFakeSIRTiterations(cbFakeSIRTiterations.isSelected());
    metaData.setFakeSIRTiterations(ltfFakeSIRTiterations.getText());
    metaData.setLeaveIterations(ltfLeaveIterations.getText());
    metaData.setScaleToInteger(cbScaleToInteger.isSelected());
    metaData.setThickness(rtfThickness.getText());
    metaData.setBinnedThickness(rtfBinnedThickness.getText());
    metaData.setExtraThickness(ltfExtraThickness.getText());
    metaData.setFallbackThickness(ltfFallbackThickness.getText());
    metaData.setPrenewstBinByFactor(lsPrenewstBinByFactor.getValue());
    metaData.setPreblendBinByFactor(lsPreblendBinByFactor.getValue());
    metaData.setUseFindSecAddThickness(ctfFindSecAddThickness.isSelected());
    metaData.setFindSecAddThickness(ctfFindSecAddThickness.getText());
    metaData.setUseScaleFromZ(ctfScaleFromZ.isSelected());
    metaData.setScaleFromZ(ctfScaleFromZ.getText());
    metaData.setEraseGoldFid(rbEraseGoldFid.isSelected());
    metaData.setEraseGold3d(rbEraseGold3d.isSelected());
    metaData.setGoldErasingThickness(ltfGoldErasingThickness.getText());
    metaData.setSampleTypePlasticSection(rbSampleTypePlasticSection.isSelected());
    metaData.setSampleTypeCryo(rbSampleTypeCryo.isSelected());
    metaData.setPositioningThickness(ltfPositioningThickness.getText());
    metaData.setHasGoldBeads(cbHasGoldBeads.isSelected());
    metaData.setPositioningGold(ltfPositioningGold.getText());
  }

  public void display() {
    display(false);
  }

  private void display(final boolean advanced) {
    if (frame != null) {
      if (!frame.isVisible()) {
        frame.setVisible(true);
      }
      frame.setState(Frame.NORMAL);
      frame.toFront();
    }
    else {
      parent.display(BatchRunTomoTab.DATASET);
    }
    setAdvanced(advanced);
  }

  private void setAdvanced(final boolean advanced) {
    if (this.advanced == advanced) {
      return;
    }
    // There are two buttons for advanced/basic rather then a toggle button.
    if (!this.advanced) {
      btnAdvanced.doClick();
    }
    else {
      btnBasic.doClick();
    }
  }

  /**
   * Set values from the directive file collection.  Only change fields that exist in
   * directive file collection.
   *
   * @param autodoc - a writable autodoc
   */
  boolean saveAutodoc(final WritableAutodoc autodoc, final boolean doValidation) {
    if (autodoc == null) {
      return true;
    }
    try {
      // Phasing out BatchTool
      BatchTool.saveTextToAutodoc(ftfDistort, autodoc, doValidation, this,
        templateValues);
      BatchTool.saveTextToAutodoc(ftfGradient, autodoc, doValidation, this,
        templateValues);
      BatchTool.saveBooleanToAutodoc(cbRemoveXrays, autodoc, templateValues);
      BatchTool.saveTextToAutodoc(ftfModelFile, autodoc, doValidation, this,
        templateValues);
      BatchTool.saveTextToAutodoc(lsPrenewstBinByFactor, autodoc, doValidation, this,
        templateValues);
      BatchTool.saveTextToAutodoc(lsPreblendBinByFactor, autodoc, doValidation, this,
        templateValues);
      // Tracking method and fiducialess
      // Get the radio button model and save the value of the enumerated type to the
      // autodoc.
      if (!BatchTool.saveTextToAutodoc(
        ((RadioButton.RadioButtonModel) bgTrackingMethod.getSelection()), autodoc,
        templateValues)) {
        // No enumerated type was found, so fidless was selected
        BatchTool.saveBooleanToAutodoc(rbFiducialless, autodoc, templateValues);
        // Also save an empty tracking method
        BatchTool.overrideInAutodoc(DirectiveDef.TRACKING_METHOD, autodoc,
          templateValues);
        BatchTool.overrideInAutodoc(DirectiveDef.SEEDING_METHOD, autodoc, templateValues);
      }
      else if (rbTrackingMethodSeed.isSelected()) {
        // autodoc and track also sets the Both seeding method
        BatchTool.saveTextToAutodoc(rbTrackingMethodSeed.isEnabled(),
          DirectiveDef.SEEDING_METHOD, SeedingMethod.BOTH, autodoc, templateValues);
      }
      // Gold
      // Both gold fields use the same directive. Use positioning gold if it is enabled.
      // To avoid the value in these fields being overridden with the batchDefaults value,
      // save the value even if it is disabled.
      String gold = null;
      if (fiducialModelMode) {
        gold = ltfGold.getText(doValidation, this);
      }
      else if (ltfPositioningGold.isEnabled()) {
        gold = ltfPositioningGold.getText(doValidation, this);
      }
      else {
        gold = "0";
      }
      BatchTool.saveTextToAutodoc(true, DirectiveDef.GOLD, gold, null, autodoc,
        templateValues);
      //
      BatchTool.saveTextToAutodoc(ltfTargetNumberOfBeads, autodoc, doValidation, this,
        templateValues);
      BatchTool.saveTextToAutodoc(ltfSizeOfPatchesXandY, autodoc, doValidation, this,
        templateValues);
      // lengthOfPieces or default length of pieces should be saved when the checkbox is
      // true.
      BatchTool.saveBooleanTextToAutodoc(cbLengthOfPieces, lengthOfPieces,
        LENGTH_OF_PIECES_DEFAULT, autodoc, templateValues);
      BatchTool.saveBooleanToAutodoc(cbEnableStretching, autodoc, templateValues);
      BatchTool.saveBooleanToAutodoc(cbLocalAlignments, autodoc, templateValues);
      BatchTool.saveTextToAutodoc(lsBinByFactor, autodoc, doValidation, this,
        templateValues);
      BatchTool.saveBooleanToAutodoc(cbCorrectCTF, autodoc, templateValues);
      //
      // Defocus
      EtomoNumber defocus = new EtomoNumber(EtomoNumber.Type.DOUBLE);
      String directiveValue = ltfDefocus.getText(doValidation, this);
      defocus.set(directiveValue);
      if (!defocus.isNull()) {
        directiveValue = String.valueOf(defocus.getDouble() * 1000.);
      }
      BatchTool.saveTextToAutodoc(ltfDefocus, directiveValue, autodoc, templateValues);
      //
      // AUTO_FIT_RANGE_AND_STEP
      String range = CTF_RANGE_DEFAULT;
      String separator = ",";
      String step = CTF_STEP_DEFAULT;
      if (rbFitEveryImage.isSelected()) {
        range = CTF_RANGE_FIT_EVERY_IMAGE.toString();
        step = CTF_STEP_FIT_EVERY_IMAGE.toString();
      }
      else {
        range = rtfAutoFitRangeAndStep.getText(doValidation, this);
        step = ltfAutoFitStep.getText(doValidation, this);
        if (step == null || step.matches("\\s*")) {
          step = "";
          separator = "";
        }
      }
      BatchTool.saveTextToAutodoc(cbCorrectCTF.isSelected(),
        rtfAutoFitRangeAndStep.getDirectiveDef(), range + separator + step, null, autodoc,
        templateValues);
      // back projection, sirt-like, and sirt
      BatchTool.saveBooleanToAutodoc(cbDoBackprojAlso, autodoc, templateValues);
      BatchTool.saveTextToAutodoc(ltfFakeSIRTiterations, autodoc, doValidation, this,
        templateValues);
      BatchTool.saveBooleanToAutodoc(cbUseSirt, autodoc, templateValues);
      //
      BatchTool.saveTextToAutodoc(ltfLeaveIterations, autodoc, doValidation, this,
        templateValues);
      // Replacing user scale to integer value with recommended value.
      BatchTool.saveBooleanTextToAutodoc(cbScaleToInteger, null, SCALE_TO_INTEGER_VALUE,
        autodoc, templateValues);
      //
      BatchTool.saveTextToAutodoc(ltfFallbackThickness, autodoc, doValidation, this,
        templateValues);
      BatchTool.saveTextToAutodoc(ltfExtraThickness, autodoc, doValidation, this,
        templateValues);
      BatchTool.saveBooleanTextToAutodoc(rtfBinnedThickness, autodoc, doValidation, this,
        templateValues);
      BatchTool.saveBooleanTextToAutodoc(rtfThickness, autodoc, doValidation, this,
        templateValues);
      // If scaleFromZ is enabled & unchecked, override the scaleFromZ directive.
      if (ctfScaleFromZ.isEnabled() && !ctfScaleFromZ.isSelected()) {
        BatchTool.overrideInAutodoc(DirectiveDef.SCALE_FROM_Z, autodoc, templateValues);
      }
      else {
        BatchTool.saveBooleanTextToAutodoc(ctfScaleFromZ, autodoc, doValidation, this,
          templateValues);
      }
      BatchTool.saveBooleanToAutodoc(cbDoTrimvol, autodoc, templateValues);
      BatchTool.saveBooleanTextToAutodoc(ctfFindSecAddThickness, autodoc, doValidation,
        this, templateValues);
      // erase gold
      EnumeratedType enumeratedType = null;
      if (cbEraseGold.isSelected()) {
        enumeratedType =
          ((RadioButton.RadioButtonModel) bgEraseGold.getSelection()).getEnumeratedType();
      }
      BatchTool.saveTextToAutodoc(cbEraseGold.isEnabled(), cbEraseGold.getDirectiveDef(),
        enumeratedType, autodoc, templateValues);
      BatchTool.saveTextToAutodoc(ltfGoldErasingThickness, autodoc, doValidation, this,
        templateValues);
      // SampleType
      if (cbSampleType.isSelected()) {
        BatchTool.saveTextToAutodoc(cbSampleType.isEnabled(),
          cbSampleType.getDirectiveDef(),
          ((RadioButton.RadioButtonModel) bgSampleType.getSelection())
            .getEnumeratedType(),
          autodoc, templateValues);
      }
      else {
        // Save sampleType = 0, if necessary
        BatchTool.saveNotTextToAutodoc(DirectiveDef.SAMPLE_TYPE, SampleType.NONE, autodoc,
          templateValues);
      }
      //
      BatchTool.saveTextToAutodoc(ltfPositioningThickness, autodoc, doValidation, this,
        templateValues);
      BatchTool.saveBooleanToAutodoc(cbHasGoldBeads, autodoc, templateValues);
      if (directivesDialog != null) {
        return directivesDialog.saveAutodoc(autodoc, doValidation,
          advancedFieldDisplayer);
      }
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  /**
   * Returns a valid, comma separated, list made from element1 and element2.
   * @param element1
   * @param element2
   * @return
   */
  private String makeList(String element1, String element2) {
    boolean set1 = element1 != null && !element1.matches("\\s");
    boolean set2 = element2 != null && !element2.matches("\\s");
    String separator = "";
    if (set2) {
      separator = ",";
    }
    if (!set1) {
      element1 = "";
    }
    if (!set2) {
      element2 = "";
    }
    return element1 + separator + element2;
  }

  /**
   * Set values from the directive file collection.  Only change fields that exist in
   * directive file collection.  Make sure the template values are set for each field when
   * setFieldHighlightValue is on.
   *
   * @param directiveFiles - templates and base directive file, or a single directive file
   * @param setFieldHighlightValue - template only value is set in field highlight and template value
   * @param onlyAdvancedDatasetDialog only loading an advanced dataset dialog
   */
  void setValues(final DirectiveFileInterface directiveFiles,
    final boolean setFieldHighlightValue, final boolean onlyAdvancedDatasetDialog,
    final boolean loadingDirectiveFile) {
    if (!onlyAdvancedDatasetDialog) {
      // Phasing out BatchTool
      BatchTool.setTextValue(ftfDistort, directiveFiles, setFieldHighlightValue,
        templateValues);
      BatchTool.setTextValue(ftfGradient, directiveFiles, setFieldHighlightValue,
        templateValues);
      BatchTool.setBooleanValue(cbRemoveXrays, directiveFiles, setFieldHighlightValue,
        templateValues);
      BatchTool.setTextValue(ftfModelFile, directiveFiles, setFieldHighlightValue,
        templateValues);
      BatchTool.setTextValue(lsPrenewstBinByFactor, directiveFiles,
        setFieldHighlightValue, templateValues);
      BatchTool.setTextValue(lsPreblendBinByFactor, directiveFiles,
        setFieldHighlightValue, templateValues);
      BatchTool.setBooleanValue(rbFiducialless, directiveFiles, setFieldHighlightValue,
        templateValues);
      // Tracking, seeding radio buttons
      // Tracking method
      DirectiveDef directiveDef = DirectiveDef.TRACKING_METHOD;
      boolean containsTrackingMethod = false;
      TrackingMethod trackingMethod = null;
      if (directiveFiles.contains(directiveDef, setFieldHighlightValue)) {
        containsTrackingMethod = true;
        String directiveValue = directiveFiles.getValue(directiveDef);
        if (setFieldHighlightValue) {
          templateValues.put(directiveDef, directiveValue);
        }
        trackingMethod = TrackingMethod.getInstance(directiveValue);
      }
      // Seeding method
      directiveDef = DirectiveDef.SEEDING_METHOD;
      boolean containsSeedingMethod = false;
      SeedingMethod seedingMethod = null;
      if (directiveFiles.contains(directiveDef, setFieldHighlightValue)) {
        containsSeedingMethod = true;
        String directiveValue = directiveFiles.getValue(directiveDef);
        if (setFieldHighlightValue) {
          templateValues.put(directiveDef, directiveValue);
        }
        seedingMethod = SeedingMethod.getInstance(directiveValue);
      }
      if (trackingMethod != null) {
        // Don't highlight auto seed unless both seeding method and tracking method are
        // present.
        if (trackingMethod == TrackingMethod.SEED
          && (seedingMethod == SeedingMethod.AUTO_FID_SEED
            || seedingMethod == SeedingMethod.BOTH)) {
          BatchTool.setBooleanValue(rbTrackingMethodSeed, true,
            containsTrackingMethod && containsSeedingMethod, setFieldHighlightValue);
        }
        else if (trackingMethod == TrackingMethod.RAPTOR) {
          BatchTool.setBooleanValue(rbTrackingMethodRaptor, true, setFieldHighlightValue);
        }
        else if (trackingMethod == TrackingMethod.PATCH_TRACKING) {
          BatchTool.setBooleanValue(rbTrackingMethodPatchTracking, true,
            setFieldHighlightValue);
        }
      }
      // Gold
      String directiveValue =
        directiveFiles.getValue(DirectiveDef.GOLD, setFieldHighlightValue);
      BatchTool.setTextValue(ltfGold, directiveValue, setFieldHighlightValue,
        templateValues);
      BatchTool.setTextValue(ltfPositioningGold, directiveValue, setFieldHighlightValue,
        templateValues);
      //
      BatchTool.setTextValue(ltfTargetNumberOfBeads, directiveFiles,
        setFieldHighlightValue, templateValues);
      BatchTool.setTextValue(ltfSizeOfPatchesXandY, directiveFiles,
        setFieldHighlightValue, templateValues);
      // length of pieces
      String value = BatchTool.setBooleanValueFromText(cbLengthOfPieces, directiveFiles,
        setFieldHighlightValue, templateValues);
      // Save length of pieces
      if (!setFieldHighlightValue) {
        lengthOfPieces = value;
      }
      //
      BatchTool.setBooleanValue(cbEnableStretching, directiveFiles,
        setFieldHighlightValue, templateValues);
      BatchTool.setBooleanValue(cbLocalAlignments, directiveFiles, setFieldHighlightValue,
        templateValues);
      BatchTool.setTextValue(lsBinByFactor, directiveFiles, setFieldHighlightValue,
        templateValues);
      BatchTool.setBooleanValue(cbCorrectCTF, directiveFiles, setFieldHighlightValue,
        templateValues);
      // Defocus
      directiveDef = ltfDefocus.getDirectiveDef();
      directiveValue = null;
      String calculatedValue = null;
      if (directiveFiles.contains(directiveDef, setFieldHighlightValue)) {
        directiveValue = directiveFiles.getValue(directiveDef, setFieldHighlightValue);
        EtomoNumber defocus = new EtomoNumber(EtomoNumber.Type.DOUBLE);
        defocus.set(directiveValue);
        if (!defocus.isNull() && defocus.isValid()) {
          calculatedValue = String.valueOf(defocus.getDouble() / 1000.);
        }
      }
      BatchTool.setTextValue(ltfDefocus, directiveValue, calculatedValue,
        setFieldHighlightValue, templateValues);
      // Three AUTO_FIT_RANGE_AND_STEP fields
      if (BatchTool.setTextValues(rtfAutoFitRangeAndStep, ltfAutoFitStep, directiveFiles,
        setFieldHighlightValue, templateValues)) {
        if (!BatchTool.setBooleanValueIfEquals(rbFitEveryImage, ltfAutoFitStep.getText(),
          0, setFieldHighlightValue) && !setFieldHighlightValue) {
          BatchTool.setBooleanValue(rtfAutoFitRangeAndStep, true, setFieldHighlightValue);
        }
      }
      if (cbCorrectCTF.isSelected()) {
        if (CTF_RANGE_FIT_EVERY_IMAGE.equals(rtfAutoFitRangeAndStep.getText())
          && CTF_STEP_FIT_EVERY_IMAGE.equals(ltfAutoFitStep.getText())) {
          rbFitEveryImage.setSelected(true);
        }
        else {
          rtfAutoFitRangeAndStep.setSelected(true);
        }
      }
      // back-projection, sirt-like, and sirt
      BatchTool.setBooleanValueFromText(cbFakeSIRTiterations, directiveFiles,
        setFieldHighlightValue, templateValues);
      BatchTool.setTextValue(ltfFakeSIRTiterations, directiveFiles,
        setFieldHighlightValue, templateValues);
      BatchTool.setBooleanValue(cbUseSirt, directiveFiles, setFieldHighlightValue,
        templateValues);
      // doBackprojAlso
      BatchTool.setBooleanValue(cbDoBackprojAlso, directiveFiles, setFieldHighlightValue,
        loadingDirectiveFile, templateValues);
      // DoBackprojAlso is also the default.
      if (!cbFakeSIRTiterations.isSelected() && !cbUseSirt.isSelected()) {
        cbDoBackprojAlso.setSelected(true);
      }
      BatchTool.setTextValue(ltfLeaveIterations, directiveFiles, setFieldHighlightValue,
        templateValues);
      // For ScaleToInteger, only using the recommended value, so don't save the directive
      // value.
      BatchTool.setBooleanValueFromText(cbScaleToInteger, directiveFiles,
        setFieldHighlightValue, templateValues);
      // Priority of directives
      // 1. THICKNESS
      // 2. binnedThickness
      // 3. fallbackThickness (default)
      // Initialize them in reverse order of priority.
      boolean thicknessSet = false;
      if (BatchTool.setBooleanAndTextValue(rbFallbackAndExtraThickness,
        ltfFallbackThickness, directiveFiles, setFieldHighlightValue, templateValues)) {
        thicknessSet = true;
      }
      // Highlight radio button for Fallback and extra thickness when either fallback or
      // extra is set.
      if (BatchTool.setTextValue(ltfExtraThickness, directiveFiles,
        setFieldHighlightValue, templateValues) && setFieldHighlightValue) {
        rbFallbackAndExtraThickness.setFieldHighlight(true);
      }
      if (BatchTool.setBooleanTextValue(rtfBinnedThickness, directiveFiles,
        setFieldHighlightValue, templateValues)) {
        thicknessSet = true;
      }
      if (BatchTool.setBooleanTextValue(rtfThickness, directiveFiles,
        setFieldHighlightValue, templateValues)) {
        thicknessSet = true;
      }
      // set fallback setting
      if (!setFieldHighlightValue && !thicknessSet) {
        rbFallbackAndExtraThickness.setSelected(true);
      }
      // trimvol and scaling
      boolean containsScaleFromX =
        directiveFiles.contains(DirectiveDef.SCALE_FROM_X, setFieldHighlightValue);
      boolean containsScaleFromY =
        directiveFiles.contains(DirectiveDef.SCALE_FROM_Y, setFieldHighlightValue);
      boolean containsScaleFromZ = BatchTool.setBooleanTextValue(ctfScaleFromZ,
        directiveFiles, setFieldHighlightValue, templateValues);
      // Scale from Z is checked and set to a default when either scale from X or Y is
      // set.
      if (!containsScaleFromZ && (containsScaleFromX || containsScaleFromY)) {
        BatchTool.setBooleanTextValue(ctfScaleFromZ,
          BatchRunTomoDatasetMetaData.SCALE_FROM_Z_DEFAULT, setFieldHighlightValue);
      }
      // Turn on trimvol if any trimvol values are set.
      if (!BatchTool.setBooleanValue(cbDoTrimvol, directiveFiles, setFieldHighlightValue,
        templateValues)) {
        if (containsScaleFromX || containsScaleFromY || containsScaleFromZ
        // Backwards compatibility - older trimvol values.
          || directiveFiles.contains(DirectiveDef.FIND_SEC_ADD_THICKNESS,
            setFieldHighlightValue)
          || directiveFiles.contains(DirectiveDef.REORIENT, setFieldHighlightValue)
          || directiveFiles.contains(DirectiveDef.THICKNESS_FOR_TRIMVOL,
            setFieldHighlightValue)
          || directiveFiles.contains(DirectiveDef.SIZE_IN_X, setFieldHighlightValue)
          || directiveFiles.contains(DirectiveDef.SIZE_IN_Y, setFieldHighlightValue)) {
          BatchTool.setBooleanValue(cbDoTrimvol, true, setFieldHighlightValue);
        }
      }
      //
      BatchTool.setBooleanTextValue(ctfFindSecAddThickness, directiveFiles,
        setFieldHighlightValue, templateValues);
      EraseGold eraseGold =
        EraseGold.getInstance(BatchTool.setBooleanValueFromText(cbEraseGold,
          directiveFiles, setFieldHighlightValue, templateValues));
      if (eraseGold == EraseGold.FID) {
        BatchTool.setBooleanValue(rbEraseGoldFid, true, setFieldHighlightValue);
      }
      else if (eraseGold == EraseGold.FIND_3D) {
        BatchTool.setBooleanValue(rbEraseGold3d, true, setFieldHighlightValue);
      }
      BatchTool.setTextValue(ltfGoldErasingThickness, directiveFiles,
        setFieldHighlightValue, templateValues);
      SampleType sampleType =
        SampleType.getInstance(BatchTool.setBooleanValueFromUnselectedText(cbSampleType,
          SampleType.NONE.toString(), directiveFiles, setFieldHighlightValue,
          templateValues));
      if (sampleType == SampleType.PLASTIC_SECTION) {
        BatchTool.setBooleanValue(rbSampleTypePlasticSection, true,
          setFieldHighlightValue);
      }
      else if (sampleType == SampleType.CRYO) {
        BatchTool.setBooleanValue(rbSampleTypeCryo, true, setFieldHighlightValue);
      }
      BatchTool.setTextValue(ltfPositioningThickness, directiveFiles,
        setFieldHighlightValue, templateValues);
      BatchTool.setBooleanValue(cbHasGoldBeads, directiveFiles, setFieldHighlightValue,
        templateValues);
      updateDisplay();
    }
    if (directivesDialog != null) {
      directivesDialog.setValues(directiveFiles, setFieldHighlightValue);
    }
  }

  public void actionPerformed(final ActionEvent event) {
    String actionCommand;
    if (event == null || (actionCommand = event.getActionCommand()) == null) {
      return;
    }
    if (actionCommand.equals(btnModelFile.getActionCommand()) && row != null) {
      // One 3dmod instance is created for each stack file, and the row controls
      // them.
      File modelFile = ftfModelFile.getFile();
      if (modelFile != null) {
        row.imodStack(modelFile);
      }
      else {
        ftfModelFile.setFile(row.imodStack(FileType.MANUAL_REPLACEMENT_MODEL));
      }
    }
    else if (btnOk != null && actionCommand.equals(btnOk.getActionCommand())) {
      setVisible(false);
    }
    else if (btnRevertToGlobal != null
      && actionCommand.equals(btnRevertToGlobal.getActionCommand())) {
      if (UIHarness.INSTANCE.openYesNoDialog(manager, this,
        "Data in this window will be lost.  Revert to global dataset data for this "
          + "stack?")) {
        setVisible(false);
        if (row != null) {
          row.deleteDataset();
        }
      }
    }
    else if (actionCommand.equals(btnAdvanced.getActionCommand())) {
      updateAdvanced(true);
    }
    else if (actionCommand.equals(btnBasic.getActionCommand())) {
      updateAdvanced(false);
    }
    else if (menuFitWindow != null
      && actionCommand.equals(menuFitWindow.getActionCommand())) {
      pack();
    }
    else if (actionCommand.equals(cbDoBackprojAlso.getActionCommand())) {
      if (!cbFakeSIRTiterations.isSelected() && !cbUseSirt.isSelected()) {
        // If neither SIRT or SIRT-like is used, back project is done automatically.
        cbDoBackprojAlso.setSelected(true);
      }
      updateDisplay();
    }
    else if (actionCommand.equals(cbFakeSIRTiterations.getActionCommand())) {
      if (cbFakeSIRTiterations.isSelected()) {
        cbUseSirt.setSelected(false);
      }
      else if (!cbUseSirt.isSelected()) {
        // If neither SIRT or SIRT-like is used, back project is done automatically.
        cbDoBackprojAlso.setSelected(true);
      }
      updateDisplay();
    }
    else if (actionCommand.equals(cbUseSirt.getActionCommand())) {
      if (cbUseSirt.isSelected()) {
        cbFakeSIRTiterations.setSelected(false);
      }
      else if (!cbFakeSIRTiterations.isSelected()) {
        // If neither SIRT or SIRT-like is used, back project is done automatically.
        cbDoBackprojAlso.setSelected(true);
      }
      updateDisplay();
    }
    else {
      updateDisplay();
    }
  }

  /**
   * Only the global instance exists when there is an empty table.  So this function
   * should only be called in the global instance.
   */
  public void lastRowDeleted(final EventObject event) {
    emptyTable = true;
    row = null;
    updateDisplay();
  }

  /**
   * Only the global instance exists when there is an empty table.  So this function
   * should only be called in the global instance.
   */
  public void firstRowAdded(final EventObject event) {
    emptyTable = false;
    if (parent != null) {
      // The global instance is attached to the first row for the purpose of
      // opening
      // 3dmod.
      row = parent.getFirstRow();
    }
    updateDisplay();
  }

  private void setTooltips() {
    int len = fieldList.size();
    for (int i = 0; i < len; i++) {
      Field field = fieldList.get(i);
      if (parent == null || frame == null || global) {
        field.setToolTipText(
          AutodocAttributeRetriever.INSTANCE.getTooltip(field.getDirectiveDef()));
      }
      else {
        // Get tooltips from the global instance.
        field.setTooltip(parent.getDatasetDialog().fieldList.get(i));
      }
    }
    // constant tooltips
    btnModelFile.setToolTipText(
      "Open raw stack in 3dmod to draw points, lines, or contours to be replaced on all "
        + "sections in objects 1, 2 or 3.");
    lsPrenewstBinByFactor.setToolTipText(
      "Image reduction used to make coarse aligned stack for single-frame datasets");
    lsPreblendBinByFactor.setToolTipText(
      "Image reduction used to make coarse aligned stack for montaged datasets");
    rbTrackingMethodSeed.setToolTipText(
      "Make fiducial model of beads by finding seed points and tracking them with "
        + "Beadtrack.");
    rbTrackingMethodRaptor.setToolTipText(
      "Make fiducial model wih Raptor and run Beadtrack to complete model.");
    rbTrackingMethodPatchTracking.setToolTipText(
      "Align images without fiducials by tracking local patches from one view to the "
        + "next.");
    rbFiducialless.setToolTipText("Make tomogram from coarse alignment only.");
    cbLengthOfPieces.setToolTipText(
      "Divide contours from patch tracking into multiple overlapping pieces.");
    cbLocalAlignments
      .setToolTipText("Compute alignments in a local areas if there are enough beads.");
    cbSampleType.setToolTipText(
      "Find specimen surfaces and do positioning for chosen sample type.");
    rbSampleTypePlasticSection.setToolTipText(
      "Find surfaces of stained, sectioned material and use for positioning.");
    rbSampleTypeCryo
      .setToolTipText("Find best orientation and Z shift of a cryospecimen.");
    ltfPositioningThickness.setToolTipText(
      "Unbinned thickness of tomogram used for positioning (required for cryo, optional "
        + "for plastic)");
    cbHasGoldBeads.setToolTipText(
      "Indicate whether there are any gold beads; this information is needed for "
        + "cryopositioning.");
    lsBinByFactor.setToolTipText("Image reduction when creating the aligned stack");
    cbCorrectCTF.setToolTipText(
      "Find defocus by fitting to power spectrum of groups of views in Ctfplotter.");
    rtfAutoFitRangeAndStep.setToolTipText("Tilt angle range for groups of views to fit");
    ltfAutoFitStep.setToolTipText("Tilt angle step between groups of views to fit");
    rbFitEveryImage.setToolTipText(
      "Find defocus by fitting to power spectrum of each image in Ctfplotter.");
    ltfDefocus.setToolTipText("Nominal defocus in microns, underfocus positive.");
    cbEraseGold.setToolTipText("Erase gold by the selected method.");
    rbEraseGoldFid
      .setToolTipText("Erase gold selected as fiducial markers using a filled-in model.");
    rbEraseGold3d
      .setToolTipText("Find all gold in tomogram and erase from aligned stack.");
    ltfGoldErasingThickness
      .setToolTipText("Unbinned thickness of tomogram used to find beads");
    cbDoBackprojAlso.setToolTipText("Use backprojection for reconstruction.");
    cbUseSirt.setToolTipText("Use the SIRT-like filter for reconstruction.");
    cbUseSirt.setToolTipText("Use SIRT for reconstruction.");
    rtfThickness.setToolTipText("Use a fixed unbinned thickness for reconstruction.");
    rtfThickness
      .setTextFieldToolTipText("Thickness of reconstruction in unbinned pixels");
    rtfBinnedThickness.setToolTipText("Use a fixed binned thickness for reconstruction.");
    rtfBinnedThickness
      .setTextFieldToolTipText("Thickness of reconstruction in binned pixels");
    rbFallbackAndExtraThickness.setToolTipText(
      "Use thickness computed from positioning, or from the distance between two layers "
        + "of fiducials.");
    ltfExtraThickness.setToolTipText(
      "Unbinned thickness to add to a thickness computed from positioning or fiducial "
        + "positions");
    ctfFindSecAddThickness.setCheckBoxToolTipText(
      "Use Findsection to find the limits in Z of the specimen; suitable for plastic "
        + "sections only.");
    ctfFindSecAddThickness.setFieldToolTipText(
      "Number of binned pixels to add to the extent found by Findsection; a fraction of "
        + "the Z extent can also be entered");
    ctfScaleFromZ.setCheckBoxToolTipText(
      "Scale to bytes based on densities in the specified central fraction of the Z "
        + "slices");
    ctfScaleFromZ.setFieldToolTipText(
      "Fraction of Z slice to analyze to find scaling to bytes; the number of slices can "
        + "also be entered");

    if (btnOk != null) {
      btnOk.setToolTipText("Exit dialog, keeping the dataset-specific values.");
    }
    if (btnRevertToGlobal != null) {
      btnRevertToGlobal.setToolTipText("Do not keep the dataset-specfic values.");
    }
  }

  private final class AdvancedFieldDisplayer implements FieldDisplayer {
    private final BatchRunTomoDatasetDialog dialog;

    private AdvancedFieldDisplayer(final BatchRunTomoDatasetDialog dialog) {
      this.dialog = dialog;
    }

    public void display() {
      dialog.display(true);
    }
  }

  private static final class Frame extends JFrame {
    private final BatchRunTomoDatasetDialog dialog;

    private Frame(final BatchRunTomoDatasetDialog dialog) {
      this.dialog = dialog;
      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    /**
     * Overridden so we can hide when window is closed
     */
    protected void processWindowEvent(final WindowEvent event) {
      super.processWindowEvent(event);
      if (event.getID() == WindowEvent.WINDOW_CLOSING) {
        dialog.setVisible(false);
      }
    }
  }
}
