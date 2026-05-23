package etomo.ui.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;

import etomo.ApplicationManager;
import etomo.comscript.CombineParams;
import etomo.comscript.ConstCombineParams;
import etomo.comscript.ConstSolvematchParam;
import etomo.comscript.DualvolmatchParam;
import etomo.comscript.SolvematchParam;
import etomo.logic.CombineTool;
import etomo.storage.LogFile;
import etomo.storage.autodoc.AutodocFactory;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.storage.autodoc.ReadOnlySection;
import etomo.type.AxisID;
import etomo.type.DialogType;
import etomo.type.EtomoAutodoc;
import etomo.type.FiducialMatch;
import etomo.type.ReconScreenState;
import etomo.type.Run3dmodMenuOptions;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;

/**
 * <p>Description: Handles solvematch and dualvolmatch.  The name should be InitialMatchPanel.</p>
 * 
 * <p>Copyright: Copyright 2002 - 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.4  2011/02/22 19:30:16  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.3  2011/02/03 06:22:16  sueh
 * <p> bug# 1422 Control of the processing method has been centralized in the
 * <p> processing method mediator class.  Implementing ProcessInterface.
 * <p> Supplying processes with the current processing method.
 * <p>
 * <p> Revision 1.2  2010/12/05 05:18:46  sueh
 * <p> bug# 1420 Moved ProcessResultDisplayFactory to etomo.ui.swing package.  Removed static button construction functions.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:35  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 3.40  2010/02/17 05:03:12  sueh
 * <p> bug# 1301 Using manager instead of manager key for popping up messages.
 * <p>
 * <p> Revision 3.39  2009/09/01 03:18:25  sueh
 * <p> bug# 1222
 * <p>
 * <p> Revision 3.38  2009/03/17 00:46:24  sueh
 * <p> bug# 1186 Pass managerKey to everything that pops up a dialog.
 * <p>
 * <p> Revision 3.37  2009/02/04 23:36:48  sueh
 * <p> bug# 1158 Changed id and exception classes in LogFile.
 * <p>
 * <p> Revision 3.36  2009/01/20 20:28:19  sueh
 * <p> bug# 1102 Changed labeled panels to type EtomoPanel so that they can name themselves.
 * <p>
 * <p> Revision 3.35  2008/05/28 02:51:30  sueh
 * <p> bug# 1111 Add a dialogType parameter to the ProcessSeries
 * <p> constructor.  DialogType must be passed to any function that constructs
 * <p> a ProcessSeries instance.
 * <p>
 * <p> Revision 3.34  2008/05/13 23:07:29  sueh
 * <p> bug# 847 Adding a right click menu for deferred 3dmods to some
 * <p> process buttons.
 * <p>
 * <p> Revision 3.33  2008/05/03 00:57:13  sueh
 * <p> bug# 847 Passing null for ProcessSeries to process funtions.
 * <p>
 * <p> Revision 3.32  2007/07/27 21:39:25  sueh
 * <p> bug# 980 In getParameters(CombineParams) setting useList to "" if the user
 * <p> enters "/".
 * <p>
 * <p> Revision 3.31  2007/03/21 19:46:50  sueh
 * <p> bug# 964 Limiting access to autodoc classes by using ReadOnly interfaces.
 * <p> Added AutodocFactory to create Autodoc instances.
 * <p>
 * <p> Revision 3.30  2007/03/07 21:14:14  sueh
 * <p> bug# 981 Turned RadioButton into a wrapper rather then a child of JRadioButton,
 * <p> because it is getting more complicated.
 * <p>
 * <p> Revision 3.29  2007/03/01 01:43:35  sueh
 * <p> bug# 964 Added LogFile to Autodoc.
 * <p>
 * <p> Revision 3.28  2007/02/09 00:53:14  sueh
 * <p> bug# 962 Made TooltipFormatter a singleton and moved its use to low-level ui
 * <p> classes.
 * <p>
 * <p> Revision 3.27  2006/09/13 23:55:24  sueh
 * <p> bug# 921 Added center shift limit
 * <p>
 * <p> Revision 3.26  2006/09/05 17:41:10  sueh
 * <p> bug# 917 Moved Restart Combine button to solvematch panel.
 * <p>
 * <p> Revision 3.25  2006/07/21 23:50:28  sueh
 * <p> bug# 892 Added show().
 * <p>
 * <p> Revision 3.24  2006/07/20 17:21:58  sueh
 * <p> bug# 848 Made UIParameters a singleton.
 * <p>
 * <p> Revision 3.23  2006/07/05 23:26:19  sueh
 * <p> Added tooltips.
 * <p>
 * <p> Revision 3.22  2006/06/09 17:06:31  sueh
 * <p> bug# 869 Enabling/disabling the tabs doesn't using this class.
 * <p> UseCorrespondingPoints is always visible, except when there is not
 * <p> transferfid.coord file.
 * <p>
 * <p> Revision 3.21  2006/05/16 21:37:50  sueh
 * <p> bug# 856 Added useCorrespondingPoints and useList.  Added isChanged(),
 * <p> which looks at useCorrespondingPoints.
 * <p>
 * <p> Revision 3.20  2006/04/28 21:05:06  sueh
 * <p> bug# 787 PanelHeader:  Removed the member variable title, which was
 * <p> not used.
 * <p>
 * <p> Revision 3.19  2006/03/27 21:07:03  sueh
 * <p> bug# 836 Added DialogType to PanelHeader get instances functions so
 * <p> that the buttons in PanelHeader could save themselves.
 * <p>
 * <p> Revision 3.18  2006/03/16 01:59:33  sueh
 * <p> bug# 828 SolvematchPanel doesn't need to implement InitialCombineFields.
 * <p>
 * <p> Revision 3.17  2006/01/12 17:38:03  sueh
 * <p> bug# 798 Moved the autodoc classes to etomo.storage.autodoc.
 * <p>
 * <p> Revision 3.16  2006/01/03 23:53:52  sueh
 * <p> bug# 675 Converted JCheckBox's to CheckBox.  Converted JRadioButton's
 * <p> toRadioButton.
 * <p>
 * <p> Revision 3.15  2005/11/14 22:21:15  sueh
 * <p> bug# 762 Made buttonAction() protected.
 * <p>
 * <p> Revision 3.14  2005/10/13 22:36:24  sueh
 * <p> Bug# 532 In synchronized(), always copying all fields
 * <p>
 * <p> Revision 3.13  2005/09/29 19:11:02  sueh
 * <p> bug# 532 Add panel headers to all of the sections in Combine.  Hide the
 * <p> sections in the tabs that are not visible so that the visible tab can become
 * <p> small.  Added an expand() function to each tab to handle the
 * <p> expand/contract requests of the panel header buttons.  Added set and get
 * <p> parameters for ReconScreenState to set and get the state of the panel
 * <p> headers.
 * <p>
 * <p> Revision 3.12  2005/08/27 22:42:22  sueh
 * <p> bug# 532 Changed Autodoc.get() to getInstance().
 * <p>
 * <p> Revision 3.11  2005/08/12 00:00:55  sueh
 * <p> bug# 711  Change enum Run3dmodMenuOption to
 * <p> Run3dmodMenuOptions, which can turn on multiple options at once.
 * <p> This allows ImodState to combine input from the context menu and the
 * <p> pulldown menu.  Prevent context menu from popping up when button is
 * <p> disabled.  Get rid of duplicate code by running the 3dmods from a private
 * <p> function called run3dmod(String, Run3dmodMenuOptions).  It can be
 * <p> called from run3dmod(Run3dmodButton, Run3dmodMenuOptions) and the
 * <p> action function.
 * <p>
 * <p> Revision 3.10  2005/08/09 21:00:01  sueh
 * <p> bug# 711  Implemented Run3dmodButtonContainer:  added run3dmod().
 * <p> Changed 3dmod buttons to Run3dmodButton.  No longer inheriting
 * <p> MultiLineButton from JButton.
 * <p>
 * <p> Revision 3.9  2005/04/25 21:39:05  sueh
 * <p> bug# 615 Passing the axis where a command originates to the message
 * <p> functions so that the message will be popped up in the correct window.
 * <p> This requires adding AxisID to many objects.
 * <p>
 * <p> Revision 3.8  2005/03/01 20:59:58  sueh
 * <p> Removed print statement.
 * <p>
 * <p> Revision 3.7  2005/02/24 00:52:18  sueh
 * <p> bug# 600 Removed unnecessary import.
 * <p>
 * <p> Revision 3.6  2005/02/23 01:44:35  sueh
 * <p> bug# 600 Getting solvematch tooltips from autodoc.
 * <p>
 * <p> Revision 3.5  2004/08/31 17:43:01  sueh
 * <p> bug# 542 Calling TomogramCombinationDialog.setBinningWarning(true)
 * <p> when Bin by 2 checkbox is first checked.
 * <p>
 * <p> Revision 3.4  2004/06/17 20:43:50  sueh
 * <p> bug# 472
 * <p>
 * <p> Revision 3.3  2004/06/15 21:37:16  rickg
 * <p> Bug #383 Correct synchronization of solvematch sub-panel
 * <p>
 * <p> Revision 3.2  2004/06/14 23:39:53  rickg
 * <p> Bug #383 Transitioned to using solvematch
 * <p>
 * <p> Revision 3.1  2004/06/13 17:03:23  rickg
 * <p> Solvematch mid change
 * <p> </p>
 */
final class SolvematchPanel
  implements Run3dmodButtonContainer, Expandable, ActionListener {
  private static final String INITIAL_MATCH_LABEL = "Initial Matching Parameters";

  private final EtomoPanel pnlRoot = new EtomoPanel();
  private final JPanel pnlFiducialRadio = new JPanel();
  private final JPanel pnlFiducialSelect = new JPanel();
  private final ButtonGroup bgFiducialParams = new ButtonGroup();
  private final RadioButton rbBothSides =
    new RadioButton("Fiducials on both sides", bgFiducialParams);
  private final RadioButton rbOneSide =
    new RadioButton("Fiducials on one side", bgFiducialParams);
  /**
   * @deprecated
   */
  private final RadioButton rbOneSideInverted =
    new RadioButton("Fiducials on one side, inverted", bgFiducialParams);
  /**
   * @deprecated
   */
  private final RadioButton rbUseModel =
    new RadioButton("Use matching models and fiducials", bgFiducialParams);
  private final RadioButton rbUseModelOnly =
    new RadioButton("Use matching models only", bgFiducialParams);
  private final JPanel pnlImodMatchModels = new JPanel();
  private final CheckBox cbBinBy2 = new CheckBox("Load binned by 2");
  private final Run3dmodButton btnImodMatchModels =
    Run3dmodButton.get3dmodInstance("Create Matching Models in 3dmod", this);
  private final LabeledTextField ltfFiducialMatchListA =
    new LabeledTextField(FieldType.INTEGER_LIST, "Corresponding fiducial list A: ");
  private final LabeledTextField ltfFiducialMatchListB =
    new LabeledTextField(FieldType.INTEGER_LIST, "Corresponding fiducial list B: ");
  private final LabeledTextField ltfUseList =
    new LabeledTextField(FieldType.INTEGER_LIST, "Starting points to use from A: ");
  private final CheckBox cbUseCorrespondingPoints =
    new CheckBox("Specify corresponding points instead of using coordinate file");
  private final CheckBox cbInitialVolumeMatching =
    new CheckBox("Use image correlations instead of Solvematch for initial match");
  private final JPanel pnlRootBody = new JPanel();

  private final ApplicationManager manager;
  private final String headerGroup;
  private final TomogramCombinationDialog parent;
  private final DialogType dialogType;
  private final PanelHeader phInitialMatch;
  private final String parentTitle;
  private final Run3dmodButton btnRestart;
  private final LabeledTextField ltfSolvematchMaximumResidual;
  private final LabeledTextField ltfSolvematchCenterShiftLimit;
  private final LabeledTextField ltfDualvolmatchMaximumResidual;
  private final LabeledTextField ltfDualvolmatchCenterShiftLimit;

  private boolean binningWarning = false;
  // initial tab only
  private boolean useCorrespondingPointsChanged = false;
  private boolean debug = false;

  private SolvematchPanel(final TomogramCombinationDialog parent,
    final String parentTitle, final ApplicationManager manager, final String headerGroup,
    final DialogType dialogType, final boolean debug,
    final GlobalExpandButton globalAdvancedButton) {
    this.dialogType = dialogType;
    this.parent = parent;
    this.parentTitle = parentTitle;
    this.manager = manager;
    this.headerGroup = headerGroup;
    this.debug = debug;
    if (parentTitle.equals(TomogramCombinationDialog.lblInitial)) {
      phInitialMatch = PanelHeader.getAdvancedBasicInstance(INITIAL_MATCH_LABEL, this,
        dialogType, globalAdvancedButton);
      btnRestart = (Run3dmodButton) manager.getProcessResultDisplayFactory(AxisID.ONLY)
        .getRestartCombine();
      ltfSolvematchMaximumResidual =
        new LabeledTextField(FieldType.FLOATING_POINT, "Limit on maximum residual: ");
      ltfSolvematchCenterShiftLimit =
        new LabeledTextField(FieldType.FLOATING_POINT, "Limit on center shift: ");
      ltfDualvolmatchMaximumResidual = new LabeledTextField(FieldType.FLOATING_POINT,
        "Limit on mean residual in patch correlations: ");
      ltfDualvolmatchCenterShiftLimit =
        new LabeledTextField(FieldType.FLOATING_POINT, "Limit on center shift: ");
    }
    else {
      phInitialMatch = PanelHeader.getInstance(INITIAL_MATCH_LABEL, this, dialogType);
      btnRestart = null;
      ltfSolvematchMaximumResidual = null;
      ltfSolvematchCenterShiftLimit = null;
      ltfDualvolmatchMaximumResidual = null;
      ltfDualvolmatchCenterShiftLimit = null;
    }
    if (globalAdvancedButton != null) {
      globalAdvancedButton.register(this);
    }
  }

  static SolvematchPanel getInstance(final TomogramCombinationDialog parent,
    final String parentTitle, final ApplicationManager manager, final String headerGroup,
    final DialogType dialogType, final boolean debug,
    final GlobalExpandButton globalAdvancedButton) {
    SolvematchPanel instance = new SolvematchPanel(parent, parentTitle, manager,
      headerGroup, dialogType, debug, globalAdvancedButton);
    instance.createPanel();
    instance.setToolTipText();
    instance.show();
    instance.addListeners();
    return instance;
  }

  private void createPanel() {
    // locals
    JPanel pnlSolveMatch = new JPanel();
    JPanel pnlFiducialRadioOuter = new JPanel();
    JPanel pnlUseCorrespondingPoints = new JPanel();
    JPanel pnlRestart = null;
    if (btnRestart != null) {
      pnlRestart = new JPanel();
    }
    JPanel pnlInitialVolumeMatching = new JPanel();
    JPanel pnlDualvolmatch = null;
    if (ltfDualvolmatchMaximumResidual != null) {
      pnlDualvolmatch = new JPanel();
    }
    // init
    rbBothSides.setAlignmentX(Component.LEFT_ALIGNMENT);
    rbOneSide.setAlignmentX(Component.LEFT_ALIGNMENT);
    rbOneSideInverted.setAlignmentX(Component.LEFT_ALIGNMENT);
    rbUseModel.setAlignmentX(Component.LEFT_ALIGNMENT);
    pnlFiducialRadioOuter.setAlignmentX(Component.CENTER_ALIGNMENT);
    if (btnRestart != null) {
      btnRestart.setContainer(this);
    }
    cbInitialVolumeMatching
      .setSelected(CombineTool.getInitialVolumeMatchingInitValue(manager));
    rbOneSideInverted.setVisible(false);
    rbUseModel.setVisible(false);
    // Root
    pnlRoot.setBorder(BorderFactory.createEtchedBorder());
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
    pnlRoot.add(phInitialMatch);
    pnlRoot.add(pnlRootBody);
    // RootBody
    pnlRootBody.setLayout(new BoxLayout(pnlRootBody, BoxLayout.Y_AXIS));
    pnlRootBody.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlRootBody.add(pnlInitialVolumeMatching);
    pnlRootBody.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlRootBody.add(pnlSolveMatch);
    if (pnlDualvolmatch != null) {
      pnlRootBody.add(pnlDualvolmatch);
    }
    if (pnlRestart != null) {
      UIUtilities.addWithYSpace(pnlRootBody, pnlRestart);
    }
    // InitialVolumeMatching
    pnlInitialVolumeMatching
      .setLayout(new BoxLayout(pnlInitialVolumeMatching, BoxLayout.X_AXIS));
    pnlInitialVolumeMatching.add(cbInitialVolumeMatching.getComponent());
    pnlInitialVolumeMatching.add(Box.createHorizontalGlue());
    // SolveMatch
    pnlSolveMatch.setBorder(new EtchedBorder("Solvematch Parameters").getBorder());
    pnlSolveMatch.setLayout(new BoxLayout(pnlSolveMatch, BoxLayout.Y_AXIS));
    UIUtilities.addWithSpace(pnlSolveMatch, pnlFiducialSelect, FixedDim.x0_y10);
    UIUtilities.addWithYSpace(pnlSolveMatch, pnlUseCorrespondingPoints);
    UIUtilities.addWithYSpace(pnlSolveMatch, ltfUseList.getContainer());
    UIUtilities.addWithYSpace(pnlSolveMatch, ltfFiducialMatchListA.getContainer());
    UIUtilities.addWithYSpace(pnlSolveMatch, ltfFiducialMatchListB.getContainer());
    if (ltfSolvematchMaximumResidual != null) {
      UIUtilities.addWithYSpace(pnlSolveMatch,
        ltfSolvematchMaximumResidual.getContainer());
    }
    if (ltfSolvematchCenterShiftLimit != null) {
      UIUtilities.addWithYSpace(pnlSolveMatch,
        ltfSolvematchCenterShiftLimit.getContainer());
    }
    // FiducialSelect
    pnlFiducialSelect.setLayout(new BoxLayout(pnlFiducialSelect, BoxLayout.X_AXIS));
    UIUtilities.addWithSpace(pnlFiducialSelect, pnlFiducialRadioOuter, FixedDim.x20_y0);
    pnlFiducialSelect.add(pnlImodMatchModels);
    pnlFiducialSelect.add(Box.createHorizontalGlue());
    // FiducialRadioOuter
    pnlFiducialRadioOuter
      .setLayout(new BoxLayout(pnlFiducialRadioOuter, BoxLayout.X_AXIS));
    pnlFiducialRadioOuter.add(pnlFiducialRadio);
    pnlFiducialRadioOuter.add(Box.createHorizontalGlue());
    // FiducialRadio
    pnlFiducialRadio.setLayout(new BoxLayout(pnlFiducialRadio, BoxLayout.Y_AXIS));
    pnlFiducialRadio.add(rbBothSides.getComponent());
    pnlFiducialRadio.add(rbOneSide.getComponent());
    pnlFiducialRadio.add(rbOneSideInverted.getComponent());
    pnlFiducialRadio.add(rbUseModel.getComponent());
    pnlFiducialRadio.add(rbUseModelOnly.getComponent());
    // ImodMatchModels
    pnlImodMatchModels.setLayout(new BoxLayout(pnlImodMatchModels, BoxLayout.Y_AXIS));
    pnlImodMatchModels.add(cbBinBy2.getComponent());
    pnlImodMatchModels.add(btnImodMatchModels.getComponent());
    // UseCorrespondingPoints
    pnlUseCorrespondingPoints
      .setLayout(new BoxLayout(pnlUseCorrespondingPoints, BoxLayout.X_AXIS));
    pnlUseCorrespondingPoints.setAlignmentX(Component.CENTER_ALIGNMENT);
    pnlUseCorrespondingPoints.add(cbUseCorrespondingPoints.getComponent());
    pnlUseCorrespondingPoints.add(Box.createHorizontalGlue());
    // Dualvolmatch
    if (pnlDualvolmatch != null) {
      pnlDualvolmatch.setBorder(new EtchedBorder("Dualvolmatch Parameters").getBorder());
      pnlDualvolmatch.setLayout(new BoxLayout(pnlDualvolmatch, BoxLayout.Y_AXIS));
      if (ltfDualvolmatchMaximumResidual != null) {
        pnlDualvolmatch.add(ltfDualvolmatchMaximumResidual.getComponent());
      }
      if (ltfDualvolmatchCenterShiftLimit != null) {
        pnlDualvolmatch.add(ltfDualvolmatchCenterShiftLimit.getComponent());
      }
    }
    // Restart
    if (pnlRestart != null) {
      pnlRestart.setLayout(new BoxLayout(pnlRestart, BoxLayout.X_AXIS));
      pnlRestart.setAlignmentX(Component.CENTER_ALIGNMENT);
      pnlRestart.add(Box.createHorizontalGlue());
      pnlRestart.add(btnRestart.getComponent());
      pnlRestart.add(Box.createHorizontalGlue());
    }
    // adjust
    UIUtilities.setButtonSizeAll(pnlImodMatchModels,
      UIParameters.getInstance().getButtonDimension());
    // update
    updateDisplay();
    updateAdvanced(phInitialMatch.isAdvanced());
  }

  void show() {
    if (manager.coordFileExists()) {
      cbUseCorrespondingPoints.setSelected(false);
      cbUseCorrespondingPoints.setVisible(true);
      ltfUseList.setVisible(true);
    }
    else {
      cbUseCorrespondingPoints.setSelected(true);
      cbUseCorrespondingPoints.setVisible(false);
      ltfUseList.setVisible(false);
    }
    updateDisplay();
  }

  void setDeferred3dmodButtons() {
    if (btnRestart != null) {
      btnRestart.setDeferred3dmodButton(parent.getImodCombinedButton());
    }
  }

  private void addListeners() {
    // Bind the ui elements to their listeners
    if (btnRestart != null) {
      btnRestart.addActionListener(this);
    }
    btnImodMatchModels.addActionListener(this);
    cbBinBy2.addActionListener(this);
    cbUseCorrespondingPoints.addActionListener(this);
    rbBothSides.addActionListener(this);
    rbOneSide.addActionListener(this);
    rbOneSideInverted.addActionListener(this);
    rbUseModel.addActionListener(this);
    rbUseModelOnly.addActionListener(this);
    cbInitialVolumeMatching.addActionListener(this);
  }

  Container getContainer() {
    return pnlRoot;
  }

  // FIXME there are current two ways to get the parameters into and out of the
  // panel. Does this need to be the case? It seem redundant.
  @SuppressWarnings("deprecation")
  void setParameters(final ConstCombineParams combineParams, final boolean init) {
    FiducialMatch match = combineParams.getFiducialMatch();
    if (match == FiducialMatch.BOTH_SIDES) {
      rbBothSides.setSelected(true);
    }
    else if (match == FiducialMatch.ONE_SIDE) {
      rbOneSide.setSelected(true);
    }
    // backwards compatibility
    else if (match == FiducialMatch.ONE_SIDE_INVERTED) {
      rbOneSideInverted.setSelected(true);
      rbOneSideInverted.setVisible(true);
    }
    else if (match == FiducialMatch.USE_MODEL) {
      rbUseModel.setSelected(true);
      rbUseModel.setVisible(true);
    }
    else if (match == FiducialMatch.USE_MODEL_ONLY) {
      rbUseModelOnly.setSelected(true);
    }
    ltfFiducialMatchListA.setText(combineParams.getFiducialMatchListA());
    ltfFiducialMatchListB.setText(combineParams.getFiducialMatchListB());
    ltfUseList.setText(combineParams.getUseList());
    if (cbUseCorrespondingPoints.isVisible()) {
      cbUseCorrespondingPoints.setSelected(!combineParams.isTransfer());
      updateDisplay();
    }
    boolean initialVolumeMatching = combineParams.isInitialVolumeMatching();
    if (!init || initialVolumeMatching) {
      cbInitialVolumeMatching.setSelected(initialVolumeMatching);
    }
  }

  void setParameters(final DualvolmatchParam param) {
    if (ltfDualvolmatchMaximumResidual != null) {
      ltfDualvolmatchMaximumResidual.setText(param.getMaximumResidual());
    }
    if (ltfDualvolmatchCenterShiftLimit != null) {
      ltfDualvolmatchCenterShiftLimit.setText(param.getCenterShiftLimit());
    }
  }

  void getParameters(final ReconScreenState screenState) {
    if (btnRestart != null) {
      // initial tab
      phInitialMatch.getState(screenState.getCombineInitialSolvematchHeaderState());
    }
    else {
      // setup tab
      phInitialMatch.getState(screenState.getCombineSetupSolvematchHeaderState());
    }
  }

  final void setParameters(final ReconScreenState screenState) {
    if (btnRestart != null) {
      // initial tab
      phInitialMatch.setState(screenState.getCombineInitialSolvematchHeaderState());
      btnRestart.setButtonState(screenState
        .getButtonState(btnRestart.createButtonStateKey(parent.getDialogType())));
      btnRestart
        .setButtonState(screenState.getButtonState(btnRestart.getButtonStateKey()));
    }
    else {
      // setup tab
      phInitialMatch.setState(screenState.getCombineSetupSolvematchHeaderState());
    }
  }

  void setVisible(final boolean visible) {
    pnlRoot.setVisible(visible);
  }

  /**
   * Get the parameters from the ui and filling in the appropriate fields in the
   * CombineParams object 
   * @param combineParams
   */
  @SuppressWarnings("deprecation")
  boolean getParameters(final CombineParams combineParams, final boolean doValidation) {
    try {
      if (rbBothSides.isSelected() && rbBothSides.isEnabled()) {
        combineParams.setFiducialMatch(FiducialMatch.BOTH_SIDES);
      }
      if (rbOneSide.isSelected() && rbOneSide.isEnabled()) {
        combineParams.setFiducialMatch(FiducialMatch.ONE_SIDE);
      }
      if (rbOneSideInverted.isSelected() && rbOneSideInverted.isEnabled()
        && rbOneSideInverted.isVisible()) {
        combineParams.setFiducialMatch(FiducialMatch.ONE_SIDE_INVERTED);
      }
      if (rbUseModel.isSelected() && rbUseModel.isEnabled() && rbUseModel.isVisible()) {
        combineParams.setFiducialMatch(FiducialMatch.USE_MODEL);
      }
      if (rbUseModelOnly.isSelected() && rbUseModelOnly.isEnabled()) {
        combineParams.setFiducialMatch(FiducialMatch.USE_MODEL_ONLY);
      }
      combineParams.setTransfer(!cbUseCorrespondingPoints.isSelected());
      combineParams.setFiducialMatchListA(ltfFiducialMatchListA.getText(doValidation));
      combineParams.setFiducialMatchListB(ltfFiducialMatchListB.getText(doValidation));
      if (ltfUseList.getText().matches("\\s*/\\s*")) {
        combineParams.setUseList("");
      }
      else {
        combineParams.setUseList(ltfUseList.getText(doValidation));
      }
      combineParams.setInitialVolumeMatching(cbInitialVolumeMatching.isSelected());
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  void setParameters(final ConstSolvematchParam solvematchParam) {
    setSurfacesOrModels(solvematchParam.getSurfacesOrModel());
    if (solvematchParam.isMatchBToA()) {
      ltfFiducialMatchListA.setText(solvematchParam.getToCorrespondenceList().toString());
      ltfFiducialMatchListB
        .setText(solvematchParam.getFromCorrespondenceList().toString());
    }
    else {
      ltfFiducialMatchListB.setText(solvematchParam.getToCorrespondenceList().toString());
      ltfFiducialMatchListA
        .setText(solvematchParam.getFromCorrespondenceList().toString());
    }
    if (ltfSolvematchMaximumResidual != null) {
      ltfSolvematchMaximumResidual.setText(solvematchParam.getMaximumResidual());
    }
    if (ltfSolvematchCenterShiftLimit != null) {
      ltfSolvematchCenterShiftLimit.setText(solvematchParam.getCenterShiftLimit());
    }
    ltfUseList.setText(solvematchParam.getUsePoints().toString());
  }

  boolean getParameters(final DualvolmatchParam param, final boolean doValidation) {
    try {
      if (ltfDualvolmatchMaximumResidual != null) {
        param.setMaximumResidual(ltfDualvolmatchMaximumResidual.getText(doValidation));
      }
      if (ltfDualvolmatchCenterShiftLimit != null) {
        param.setCenterShiftLimit(ltfDualvolmatchCenterShiftLimit.getText(doValidation));
      }
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  /**
   * Get the parameters from the ui and filling in the appropriate fields in the
   * SolvematchParam object 
   * @param combineParams
   */
  boolean getParameters(final SolvematchParam solvematchParam,
    final boolean doValidation) {
    try {
      solvematchParam.setSurfacesOrModel(getSurfacesOrModels());
      if (solvematchParam.isMatchBToA()) {
        solvematchParam
          .setToCorrespondenceList(ltfFiducialMatchListA.getText(doValidation));
        solvematchParam
          .setFromCorrespondenceList(ltfFiducialMatchListB.getText(doValidation));
      }
      else {
        solvematchParam
          .setFromCorrespondenceList(ltfFiducialMatchListA.getText(doValidation));
        solvematchParam
          .setToCorrespondenceList(ltfFiducialMatchListB.getText(doValidation));
      }
      if (ltfSolvematchMaximumResidual != null) {
        solvematchParam
          .setMaximumResidual(ltfSolvematchMaximumResidual.getText(doValidation));
      }
      if (ltfSolvematchCenterShiftLimit != null) {
        solvematchParam
          .setCenterShiftLimit(ltfSolvematchCenterShiftLimit.getText(doValidation));
      }

      solvematchParam.setTransferCoordinateFile(cbUseCorrespondingPoints.isSelected());
      solvematchParam.setUsePoints(ltfUseList.getText(doValidation));
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  /**
   * 
   * @return
   */
  @SuppressWarnings("deprecation")
  FiducialMatch getSurfacesOrModels() {
    if (rbBothSides.isSelected() && rbBothSides.isEnabled()) {
      return FiducialMatch.BOTH_SIDES;
    }
    if (rbOneSide.isSelected() && rbOneSide.isEnabled()) {
      return FiducialMatch.ONE_SIDE;
    }
    if (rbOneSideInverted.isSelected() && rbOneSideInverted.isEnabled()
      && rbOneSideInverted.isVisible()) {
      return FiducialMatch.ONE_SIDE_INVERTED;
    }
    if (rbUseModel.isSelected() && rbUseModel.isEnabled() && rbUseModel.isVisible()) {
      return FiducialMatch.USE_MODEL;
    }
    if (rbUseModelOnly.isSelected() && rbUseModelOnly.isEnabled()) {
      return FiducialMatch.USE_MODEL_ONLY;
    }
    return FiducialMatch.NOT_SET;
  }

  @SuppressWarnings("deprecation")
  void setSurfacesOrModels(final FiducialMatch value) {
    if (value == FiducialMatch.USE_MODEL_ONLY) {
      rbUseModelOnly.setSelected(true);
    }
    // backwards compatibility
    if (value == FiducialMatch.ONE_SIDE_INVERTED) {
      rbOneSideInverted.setSelected(true);
      rbOneSideInverted.setVisible(true);
    }
    if (value == FiducialMatch.USE_MODEL) {
      rbUseModel.setSelected(true);
      rbUseModel.setVisible(true);
    }
    if (value == FiducialMatch.ONE_SIDE) {
      rbOneSide.setSelected(true);
    }
    if (value == FiducialMatch.BOTH_SIDES) {
      rbBothSides.setSelected(true);
    }
    updateDisplay();
  }

  boolean isBinBy2() {
    return cbBinBy2.isSelected();
  }

  void setBinBy2(final boolean state) {
    cbBinBy2.setSelected(state);
  }

  void setUseList(final String useList) {
    ltfUseList.setText(useList);
  }

  void setFiducialMatchListA(final String fiducialMatchListA) {
    ltfFiducialMatchListA.setText(fiducialMatchListA);
  }

  String getUseList(final boolean doValidation) throws FieldValidationFailedException {
    return ltfUseList.getText(doValidation);
  }

  String getUseList() {
    return ltfUseList.getText();
  }

  String getFiducialMatchListA(final boolean doValidation)
    throws FieldValidationFailedException {
    return ltfFiducialMatchListA.getText(doValidation);
  }

  String getFiducialMatchListA() {
    return ltfFiducialMatchListA.getText();
  }

  void setFiducialMatchListB(final String fiducialMatchListB) {
    ltfFiducialMatchListB.setText(fiducialMatchListB);
  }

  String getFiducialMatchListB(final boolean doValidation)
    throws FieldValidationFailedException {
    return ltfFiducialMatchListB.getText(doValidation);
  }

  String getFiducialMatchListB() {
    return ltfFiducialMatchListB.getText();
  }

  public void expand(final ExpandButton button) {
    if (phInitialMatch.equalsOpenClose(button)) {
      pnlRootBody.setVisible(button.isExpanded());
    }
    else if (phInitialMatch.equalsAdvancedBasic(button)) {
      updateAdvanced(button.isExpanded());
    }
  }

  public void expand(final GlobalExpandButton button) {
    updateAdvanced(button.isExpanded());
    UIHarness.INSTANCE.pack(AxisID.ONLY, manager);
  }

  public void actionPerformed(final ActionEvent event) {
    action(event.getActionCommand(), null, null);
  }

  public void action(final String command, final Deferred3dmodButton deferred3dmodButton,
    final Run3dmodMenuOptions run3dmodMenuOptions) {
    if (command.equals(cbUseCorrespondingPoints.getActionCommand())) {
      useCorrespondingPointsChanged = true;
      updateDisplay();
    }
    else if (command.equals(rbBothSides.getActionCommand())
      || command.equals(rbOneSide.getActionCommand())
      || command.equals(rbUseModelOnly.getActionCommand())
      || command.equals(cbInitialVolumeMatching.getActionCommand())) {
      updateDisplay();
    }
    else {
      // Synchronize this panel with the others
      parent.synchronize(parentTitle, true);
      if (command.equals(cbBinBy2.getActionCommand())) {
        if (!binningWarning && cbBinBy2.isSelected()) {
          parent.setBinningWarning(true);
          binningWarning = true;
        }
      }
      else if (btnRestart != null && command.equals(btnRestart.getActionCommand())) {
        manager.combine(btnRestart, null, deferred3dmodButton, run3dmodMenuOptions,
          dialogType, parent.getRunProcessingMethod(),
          cbInitialVolumeMatching.isSelected(), parent.isParallel(),
          !parent.isRunVolcombine());
      }
      else if (command.equals(btnImodMatchModels.getActionCommand())) {
        manager.imodMatchingModel(cbBinBy2.isSelected(), run3dmodMenuOptions);
      }
    }
  }

  /**
   * Manage fiducial radio button action
   * 
   * @param event
   */
  private void rbFiducialAction(final ActionEvent event) {
    updateDisplay();
  }

  private void updateAdvanced(final boolean advanced) {
    if (ltfSolvematchCenterShiftLimit != null) {
      ltfSolvematchCenterShiftLimit.setVisible(advanced);
    }
    if (ltfDualvolmatchCenterShiftLimit != null) {
      ltfDualvolmatchCenterShiftLimit.setVisible(advanced);
    }
  }

  void updateDisplay() {
    boolean initialVolumeMatching = cbInitialVolumeMatching.isSelected();
    if (ltfSolvematchCenterShiftLimit != null) {
      ltfSolvematchCenterShiftLimit.setEnabled(!initialVolumeMatching);
    }
    if (ltfSolvematchMaximumResidual != null) {
      ltfSolvematchMaximumResidual.setEnabled(!initialVolumeMatching);
    }
    rbBothSides.setEnabled(!initialVolumeMatching);
    rbOneSide.setEnabled(!initialVolumeMatching);
    rbUseModelOnly.setEnabled(!initialVolumeMatching);
    boolean fiducialMode = rbUseModel.isSelected() || rbUseModelOnly.isSelected();
    btnImodMatchModels.setEnabled(fiducialMode && !initialVolumeMatching);
    cbBinBy2.setEnabled(fiducialMode && !initialVolumeMatching);
    cbUseCorrespondingPoints.setEnabled(!initialVolumeMatching);
    ltfFiducialMatchListA.setEnabled(!initialVolumeMatching);
    ltfFiducialMatchListB.setEnabled(!initialVolumeMatching);
    ltfUseList.setEnabled(!initialVolumeMatching);
    if (ltfDualvolmatchMaximumResidual != null) {
      ltfDualvolmatchMaximumResidual.setEnabled(initialVolumeMatching);
    }
    if (ltfDualvolmatchCenterShiftLimit != null) {
      ltfDualvolmatchCenterShiftLimit.setEnabled(initialVolumeMatching);
    }
    if (cbUseCorrespondingPoints.isSelected()) {
      ltfFiducialMatchListA.setVisible(true);
      ltfFiducialMatchListB.setVisible(true);
      ltfUseList.setVisible(false);
    }
    else {
      ltfFiducialMatchListA.setVisible(false);
      ltfFiducialMatchListB.setVisible(false);
      ltfUseList.setVisible(true);
    }
    if (useCorrespondingPointsChanged) {
      useCorrespondingPointsChanged = false;
      parent.updateDisplay();
    }
  }

  boolean isUseCorrespondingPoints() {
    return cbUseCorrespondingPoints.isSelected();
  }

  boolean isInitialVolumeMatching() {
    return cbInitialVolumeMatching.isSelected();
  }

  void setInitialVolumeMatching(final boolean input) {
    cbInitialVolumeMatching.setSelected(input);
  }

  void setUseCorrespondingPoints(final boolean selected) {
    cbUseCorrespondingPoints.setSelected(selected);
    updateDisplay();
  }

  /**
   * Initialize the tooltip text
   */
  private void setToolTipText() {
    ReadOnlySection solvematchSection;
    ReadOnlyAutodoc solvematchAutodoc = null;
    ReadOnlyAutodoc dualvolmatchAutodoc = null;
    try {
      solvematchAutodoc = AutodocFactory.getInstance(manager, AutodocFactory.SOLVEMATCH,
        AxisID.ONLY, false);
      dualvolmatchAutodoc = AutodocFactory.getInstance(manager,
        AutodocFactory.DUALVOLMATCH, AxisID.ONLY, false);
    }
    catch (FileNotFoundException except) {
      except.printStackTrace();
    }
    catch (IOException except) {
      except.printStackTrace();
    }
    catch (LogFile.LockException except) {
      except.printStackTrace();
    }
    String autodocName = solvematchAutodoc.getAutodocName();
    solvematchSection = solvematchAutodoc.getSection(EtomoAutodoc.FIELD_SECTION_NAME,
      SolvematchParam.SURFACE_OR_USE_MODELS);
    cbUseCorrespondingPoints
      .setToolTipText("Check to use the points in A and B in the transferfid log file.  "
        + "Leave unchecked to use transferfid.coord.");
    if (solvematchSection != null) {
      rbBothSides.setToolTipText(EtomoAutodoc.getTooltip(autodocName, solvematchSection,
        SolvematchParam.BOTH_SIDES_OPTION));
      rbOneSideInverted.setToolTipText(EtomoAutodoc.getTooltip(autodocName,
        solvematchSection, SolvematchParam.ONE_SIDE_INVERTED_OPTION));
      rbOneSide.setToolTipText(EtomoAutodoc.getTooltip(autodocName, solvematchSection,
        SolvematchParam.ONE_SIDE_OPTION));
      rbUseModel.setToolTipText(EtomoAutodoc.getTooltip(autodocName, solvematchSection,
        SolvematchParam.USE_MODEL_OPTION));
      rbUseModelOnly.setToolTipText(EtomoAutodoc.getTooltip(autodocName,
        solvematchSection, SolvematchParam.USE_MODEL_ONLY_OPTION));
      if (btnRestart != null) {
        btnRestart.setToolTipText(
          "Restart the combine operation from the beginning with the parameters "
            + "specified here.");
      }
    }
    cbBinBy2.setToolTipText(
      "Use binning by 2 when opening matching models to allow the two 3dmods "
        + "to fit into the computer's memory.");
    btnImodMatchModels.setToolTipText("Create models of corresponding points.");
    ltfFiducialMatchListA.setToolTipText(
      EtomoAutodoc.getTooltip(solvematchAutodoc, SolvematchParam.TO_CORRESPONDENCE_LIST));
    ltfFiducialMatchListB.setToolTipText(EtomoAutodoc.getTooltip(solvematchAutodoc,
      SolvematchParam.FROM_CORRESPONDENCE_LIST));
    ltfUseList.setToolTipText(
      EtomoAutodoc.getTooltip(solvematchAutodoc, SolvematchParam.USE_POINTS));
    cbInitialVolumeMatching.setToolTipText(
      "Use Dualvolmatch when there are no or too few matching fiducials to use "
        + "Solvematch.");
    if (ltfSolvematchMaximumResidual != null) {
      ltfSolvematchMaximumResidual.setToolTipText(
        EtomoAutodoc.getTooltip(solvematchAutodoc, SolvematchParam.MAXIMUM_RESIDUAL));
    }
    if (ltfSolvematchCenterShiftLimit != null) {
      ltfSolvematchCenterShiftLimit.setToolTipText(EtomoAutodoc
        .getTooltip(solvematchAutodoc, SolvematchParam.CENTER_SHIFT_LIMIT_KEY));
    }
    if (ltfDualvolmatchMaximumResidual != null) {
      ltfDualvolmatchMaximumResidual.setToolTipText(
        EtomoAutodoc.getTooltip(dualvolmatchAutodoc, DualvolmatchParam.MAXIMUM_RESIDUAL));
    }
    if (ltfDualvolmatchCenterShiftLimit != null) {
      ltfDualvolmatchCenterShiftLimit.setToolTipText(EtomoAutodoc
        .getTooltip(dualvolmatchAutodoc, DualvolmatchParam.CENTER_SHIFT_LIMIT));
    }
  }
}