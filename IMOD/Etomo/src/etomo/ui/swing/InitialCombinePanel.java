package etomo.ui.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import etomo.ApplicationManager;
import etomo.comscript.ConstSolvematchParam;
import etomo.comscript.DualvolmatchParam;
import etomo.comscript.MatchvolParam;
import etomo.comscript.SolvematchParam;
import etomo.type.AxisID;
import etomo.type.DialogType;
import etomo.type.FiducialMatch;
import etomo.type.FileType;
import etomo.type.MatchMode;
import etomo.type.ProcessingMethod;
import etomo.type.ReconScreenState;
import etomo.type.Run3dmodMenuOptions;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.util.InvalidParameterException;
import etomo.util.MRCHeader;

/**
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright 2002 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 1.3  2011/02/03 06:22:16  sueh
 * <p> bug# 1422 Control of the processing method has been centralized in the
 * <p> processing method mediator class.  Implementing ProcessInterface.
 * <p> Supplying processes with the current processing method.
 * <p>
 * <p> Revision 1.2  2010/12/05 05:10:56  sueh
 * <p> bug# 1420 Moved ProcessResultDisplayFactory to etomo.ui.swing package.  Removed static button construction functions.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 3.43  2010/02/17 05:03:12  sueh
 * <p> bug# 1301 Using manager instead of manager key for popping up messages.
 * <p>
 * <p> Revision 3.42  2009/09/01 03:18:25  sueh
 * <p> bug# 1222
 * <p>
 * <p> Revision 3.41  2009/03/17 00:46:24  sueh
 * <p> bug# 1186 Pass managerKey to everything that pops up a dialog.
 * <p>
 * <p> Revision 3.40  2009/02/13 02:33:00  sueh
 * <p> bug# 1176 Checking return value of MRCHeader.read.
 * <p>
 * <p> Revision 3.39  2009/01/20 20:08:23  sueh
 * <p> bug# 1102 Changed labeled panels to type EtomoPanel so that they can name themselves.
 * <p>
 * <p> Revision 3.38  2008/09/30 21:41:29  sueh
 * <p> bug# 1113 Using a private constructor in SpacedPanel.
 * <p>
 * <p> Revision 3.37  2008/05/28 02:50:03  sueh
 * <p> bug# 1111 Add a dialogType parameter to the ProcessSeries
 * <p> constructor.  DialogType must be passed to any function that constructs
 * <p> a ProcessSeries instance.
 * <p>
 * <p> Revision 3.36  2008/05/13 23:02:12  sueh
 * <p> bug# 847 Adding a right click menu for deferred 3dmods to some
 * <p> process buttons.
 * <p>
 * <p> Revision 3.35  2008/05/03 00:50:05  sueh
 * <p> bug# 847 Passing null for ProcessSeries to process funtions.
 * <p>
 * <p> Revision 3.34  2007/05/26 00:32:20  sueh
 * <p> bug# 994 Not automatically setting button size in SpacedPanel anymore.
 * <p> Setting button size in UI.
 * <p>
 * <p> Revision 3.33  2007/02/09 00:49:42  sueh
 * <p> bug# 962 Made TooltipFormatter a singleton and moved its use to low-level ui
 * <p> classes.
 * <p>
 * <p> Revision 3.32  2006/09/13 23:47:23  sueh
 * <p> bug# 921 Added setMatchMode(); use it to set the text in lOutputSizeYInfo.
 * <p>
 * <p> Revision 3.31  2006/09/05 17:40:23  sueh
 * <p> bug# 917 Moved Restart Combine button to solvematch panel.  Created a panel
 * <p> for matchvol1.  Moved Restart Matchvol1 button to matchvol1 panel.
 * <p>
 * <p> Revision 3.30  2006/07/20 17:20:16  sueh
 * <p> bug# 848 Made UIParameters a singleton.
 * <p>
 * <p> Revision 3.29  2006/06/21 15:53:32  sueh
 * <p> bug# 581 Passing axis to ContextPopup, so that imodqtassist can be run.
 * <p>
 * <p> Revision 3.28  2006/06/09 16:59:54  sueh
 * <p> bug# 869 Using "Use corresponding points" checkbox in the initial panel.
 * <p>
 * <p> Revision 3.27  2006/05/16 21:36:08  sueh
 * <p> bug# 856 Added useCorrespondingPoints and useList.
 * <p>
 * <p> Revision 3.26  2006/03/28 00:54:50  sueh
 * <p> bug# 437 Change getButtonStateKey(DialogType) to
 * <p> createButtonStateKey(DialogType).
 * <p>
 * <p> Revision 3.25  2006/03/16 01:56:07  sueh
 * <p> bug# 828 Added isEnabled().  Returns true if the initial tab is enabled.
 * <p>
 * <p> Revision 3.24  2006/01/31 20:58:22  sueh
 * <p> bug# 521 Managing the restart buttons (combine and matchvol1) in
 * <p> ProcessResultDisplayFactory.
 * <p>
 * <p> Revision 3.23  2006/01/26 22:04:59  sueh
 * <p> bug# 401 For MultiLineButton toggle buttons:  save the state and keep
 * <p> the buttons turned on each they are run, unless the process fails or is
 * <p> killed.
 * <p>
 * <p> Revision 3.22  2005/11/14 22:06:13  sueh
 * <p> bug# 762 Made buttonAction() protected.
 * <p>
 * <p> Revision 3.21  2005/10/13 22:35:40  sueh
 * <p> Bug# 532 In synchronized(), always copying all fields
 * <p>
 * <p> Revision 3.20  2005/09/29 19:09:56  sueh
 * <p> bug# 532 Add panel headers to all of the sections in Combine.  Hide the
 * <p> sections in the tabs that are not visible so that the visible tab can become
 * <p> small.  Added an expand() function to each tab to handle the
 * <p> expand/contract requests of the panel header buttons.  Added set and get
 * <p> parameters for ReconScreenState to set and get the state of the panel
 * <p> headers.
 * <p>
 * <p> Revision 3.19  2005/08/11 23:52:30  sueh
 * <p> bug# 711  Change enum Run3dmodMenuOption to
 * <p> Run3dmodMenuOptions, which can turn on multiple options at once.
 * <p> This allows ImodState to combine input from the context menu and the
 * <p> pulldown menu.  Get rid of duplicate code by running the 3dmods from a
 * <p> private function called run3dmod(String, Run3dmodMenuOptions).  It can
 * <p> be called from run3dmod(Run3dmodButton, Run3dmodMenuOptions) and
 * <p> the action function.
 * <p>
 * <p> Revision 3.18  2005/08/10 20:43:50  sueh
 * <p> bug# 711 Removed MultiLineToggleButton.  Making toggling an attribute
 * <p> of MultiLineButton.
 * <p>
 * <p> Revision 3.17  2005/08/09 20:23:02  sueh
 * <p> bug# 711  Implemented Run3dmodButtonContainer:  added run3dmod().
 * <p> Changed 3dmod buttons to Run3dmodButton.  No longer inheriting
 * <p> MultiLineButton from JButton.
 * <p>
 * <p> Revision 3.16  2004/12/02 20:39:50  sueh
 * <p> bug# 566 ContextPopup can specify an anchor in both the tomo guide and
 * <p> the join guide.  Need to specify the guide to anchor.
 * <p>
 * <p> Revision 3.15  2004/11/19 23:56:16  sueh
 * <p> bug# 520 merging Etomo_3-4-6_JOIN branch to head.
 * <p>
 * <p> Revision 3.14.2.1  2004/10/11 02:13:30  sueh
 * <p> bug# 520 Passed the manager to the ContextPopup object in order to get
 * <p> the propertyUserDir.
 * <p>
 * <p> Revision 3.14  2004/08/19 02:50:25  sueh
 * <p> bug# 508 changed the name of restartAtMatchvol1() to a more standard matchvol1Combine.
 * <p> Changed:
 * <p> buttonAction(ActionEvent event)
 * <p>
 * <p> Revision 3.13  2004/06/21 00:02:34  sueh
 * <p> bug# 436 calling restartAtMatchvol1 instead of matchvol1
 * <p>
 * <p> Revision 3.12  2004/06/15 21:37:16  rickg
 * <p> Bug #383 Correct synchronization of solvematch sub-panel
 * <p>
 * <p> Revision 3.11  2004/06/14 23:39:53  rickg
 * <p> Bug #383 Transitioned to using solvematch
 * <p>
 * <p> Revision 3.10  2004/06/13 17:03:23  rickg
 * <p> Solvematch mid change
 * <p>
 * <p> Revision 3.9  2004/05/11 21:47:13  sueh
 * <p> bug# 302 removing print statements
 * <p>
 * <p> Revision 3.8  2004/05/11 20:57:07  sueh
 * <p> bug# 302
 * <p>
 * <p> Revision 3.7  2004/05/11 20:53:22  sueh
 * <p> bug# 302 adding InitialCombineValues interface
 * <p> standardizing synchronization
 * <p>
 * <p> Revision 3.6  2004/05/05 22:24:09  sueh
 * <p> bug# 416 moving binned by 2 checkbox to above matching models
 * <p> button
 * <p>
 * <p> Revision 3.5  2004/05/03 22:26:10  sueh
 * <p> bug# 416 Adding Bin By 2 checkbox.  Passing tab identifier to
 * <p> imodMatchingModel so that checkbox settings can be copied between
 * <p> Setup and Initial tabs when the Matching Models button is pressed.
 * <p>
 * <p> Revision 3.4  2004/03/22 23:22:49  sueh
 * <p> bug# 250 synchronize tab with combineParams, matchorwarp does affect Setup
 * <p> tab
 * <p>
 * <p> Revision 3.3  2004/03/01 23:59:54  sueh
 * <p> bug# 250 add getCombineParams()
 * <p> Call combine and restart functions with tab information
 * <p>
 * <p> Revision 3.2  2004/02/27 20:01:58  sueh
 * <p> bug# 250 renamed setMatchingModels() to setUseMatchingModels()
 * <p> added getUseMatchingModels()
 * <p>
 * <p> Revision 3.1  2004/01/30 22:45:13  sueh
 * <p> bug# 356 Changing buttons with html labels to
 * <p> MultiLineButton and MultiLineToggleButton
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:01  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 1.16  2003/11/05 19:56:58  rickg
 * <p> Bug# 300 Selecting matching models on setup patch now
 * <p> selects matching models on initial page
 * <p>
 * <p> Revision 1.15  2003/10/29 17:23:02  rickg
 * <p> Bug# 301 Tooltips
 * <p>
 * <p> Revision 1.14  2003/10/21 23:43:42  rickg
 * <p> Changed imod buttons to non multiline
 * <p>
 * <p> Revision 1.13  2003/10/20 20:25:59  rickg
 * <p> Bug# 228 added Restart at matchvol1 button
 * <p>
 * <p> Revision 1.12  2003/10/15 22:46:41  rickg
 * <p> Button size change
 * <p> Label changes
 * <p>
 * <p> Revision 1.11  2003/10/15 19:15:52  rickg
 * <p> Bug# 299 Moved buttons up
 * <p>
 * <p> Revision 1.10  2003/06/05 04:42:37  rickg
 * <p> Label change for view match shift results
 * <p>
 * <p> Revision 1.9  2003/04/28 23:25:25  rickg
 * <p> Changed visible imod references to 3dmod
 * <p>
 * <p> Revision 1.8  2003/03/20 21:19:08  rickg
 * <p> Added matchshift results button/access
 * <p>
 * <p> Revision 1.7  2003/03/20 17:47:21  rickg
 * <p> Initial implementation of panel
 * <p>
 * <p> Revision 1.6  2003/03/18 23:41:07  rickg
 * <p> Restructured for both model and non model based combines
 * <p>
 * <p> </p>
 */
class InitialCombinePanel
  implements ContextMenu, InitialCombineFields, Run3dmodButtonContainer, Expandable {
  private final JPanel pnlRoot = new JPanel();
  private final Run3dmodButton btnMatchvolRestart;
  private final SpacedPanel pnlMatchvol1Body = SpacedPanel.getInstance(true);
  private final PanelHeader matchvol1Header;
  private final LabeledTextField ltfOutputSizeY =
    new LabeledTextField(FieldType.INTEGER, "Initial match size: ");
  private final JLabel lOutputSizeYInfo = new JLabel();
  private final ButtonActionListener buttonAction = new ButtonActionListener(this);
  private final TomogramCombinationDialog tomogramCombinationDialog;
  private final ApplicationManager applicationManager;
  private final DialogType dialogType;
  private final SolvematchPanel pnlSolvematch;

  private MatchMode matchMode = null;

  /**
   * Default constructor
   * @param appMgr
   */
  private InitialCombinePanel(final TomogramCombinationDialog parent,
    final ApplicationManager appMgr, final DialogType dialogType,
    final GlobalExpandButton globalAdvancedButton) {
    this.dialogType = dialogType;
    tomogramCombinationDialog = parent;
    applicationManager = appMgr;
    globalAdvancedButton.register(this);
    matchvol1Header = PanelHeader.getAdvancedBasicInstance("Matchvol1", this,
      DialogType.TOMOGRAM_COMBINATION, globalAdvancedButton);
    btnMatchvolRestart = (Run3dmodButton) appMgr
      .getProcessResultDisplayFactory(AxisID.ONLY).getRestartMatchvol1();
    pnlSolvematch = SolvematchPanel.getInstance(tomogramCombinationDialog,
      TomogramCombinationDialog.lblInitial, appMgr,
      ReconScreenState.COMBINE_INITIAL_SOLVEMATCH_HEADER_GROUP, dialogType, true,
      globalAdvancedButton);
  }

  static InitialCombinePanel getInstance(final TomogramCombinationDialog parent,
    final ApplicationManager manager, final DialogType dialogType,
    final GlobalExpandButton globalAdvancedButton) {
    InitialCombinePanel instance =
      new InitialCombinePanel(parent, manager, dialogType, globalAdvancedButton);
    instance.createPanel();
    instance.addListeners();
    instance.setToolTipText();
    return instance;
  }

  private void createPanel() {
    // panels
    EtomoPanel pnlMatchvol1 = new EtomoPanel();
    // init
    btnMatchvolRestart.setContainer(this);
    lOutputSizeYInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
    btnMatchvolRestart.setAlignmentX(Component.CENTER_ALIGNMENT);
    // root
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
    pnlRoot.add(pnlSolvematch.getContainer());
    pnlRoot.add(pnlMatchvol1);
    // Matchvol1
    pnlMatchvol1.setBorder(BorderFactory.createEtchedBorder());
    pnlMatchvol1.setLayout(new BoxLayout(pnlMatchvol1, BoxLayout.Y_AXIS));
    pnlMatchvol1.add(matchvol1Header);
    pnlMatchvol1.add(pnlMatchvol1Body.getContainer());
    // Matchvol1Body
    pnlMatchvol1Body.setBoxLayout(BoxLayout.Y_AXIS);
    pnlMatchvol1Body.add(ltfOutputSizeY);
    pnlMatchvol1Body.add(lOutputSizeYInfo);
    pnlMatchvol1Body.add(btnMatchvolRestart);
  }

  private void addListeners() {
    // Bind the UI objects to their ActionListeners
    btnMatchvolRestart.addActionListener(buttonAction);
    // Mouse listener for context menu
    GenericMouseAdapter mouseAdapter = new GenericMouseAdapter(this);
    pnlRoot.addMouseListener(mouseAdapter);
  }

  void removeListeners() {
    btnMatchvolRestart.removeActionListener(buttonAction);
  }

  void setDeferred3dmodButtons() {
    btnMatchvolRestart
      .setDeferred3dmodButton(tomogramCombinationDialog.getImodCombinedButton());
    pnlSolvematch.setDeferred3dmodButtons();
  }

  public void setMatchMode(final MatchMode matchMode) {
    if (this.matchMode == matchMode) {
      return;
    }
    this.matchMode = matchMode;
    // set lOutputSizeYInfo
    AxisID toAxisID = AxisID.FIRST;
    AxisID fromAxisID = AxisID.SECOND;
    if (matchMode == MatchMode.A_TO_B) {
      toAxisID = AxisID.SECOND;
      fromAxisID = AxisID.FIRST;
    }
    MRCHeader toHeader =
      MRCHeader.getInstance(applicationManager, toAxisID, FileType.TILT_OUTPUT);
    MRCHeader fromHeader =
      MRCHeader.getInstance(applicationManager, fromAxisID, FileType.TILT_OUTPUT);
    int toY = -1;
    try {
      if (!toHeader.read(applicationManager)) {
        return;
      }
    }
    catch (InvalidParameterException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    toY = toHeader.getNRows();
    int fromY = -1;
    try {
      if (!fromHeader.read(applicationManager)) {
        return;
      }
    }
    catch (InvalidParameterException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    fromY = fromHeader.getNRows();
    lOutputSizeYInfo.setText("Original " + fromAxisID.getExtension().toUpperCase()
      + " size is " + fromY + ".  Final size will be " + toY);
  }

  ProcessingMethod getProcessingMethod() {
    return ProcessingMethod.LOCAL_CPU;
  }

  /**
   * Since match mode isn't modified in initial tab, always return null.
   */
  public MatchMode getMatchMode() {
    return null;
  }

  public boolean isUseCorrespondingPoints() {
    return pnlSolvematch.isUseCorrespondingPoints();
  }

  public void setUseCorrespondingPoints(final boolean use) {
    pnlSolvematch.setUseCorrespondingPoints(use);
  }

  Container getContainer() {
    return pnlRoot;
  }

  void updateAdvanced(final boolean state) {
    updateMatchvol1Advanced(state);
  }

  void updateDisplay() {
    pnlSolvematch.updateDisplay();
  }

  void updateMatchvol1Advanced(final boolean advanced) {
    ltfOutputSizeY.setVisible(advanced);
    lOutputSizeYInfo.setVisible(advanced);
  }

  final void setVisible(final boolean visible) {
    pnlSolvematch.setVisible(visible);
  }

  public boolean isEnabled() {
    return tomogramCombinationDialog.isTabEnabled(TomogramCombinationDialog.lblInitial);
  }

  public boolean isInitialVolumeMatching() {
    return pnlSolvematch.isInitialVolumeMatching();
  }

  public void setInitialVolumeMatching(final boolean input) {
    pnlSolvematch.setInitialVolumeMatching(input);
  }

  boolean getParameters(final MatchvolParam param, final boolean doValidation) {
    try {
      param.setOutputSizeY(ltfOutputSizeY.getText(doValidation));
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  public void setParameters(final MatchvolParam param) {
    ltfOutputSizeY.setText(param.getOutputSizeY());
  }

  public void expand(final GlobalExpandButton button) {
    updateAdvanced(button.isExpanded());
    UIHarness.INSTANCE.pack(AxisID.ONLY, applicationManager);
  }

  public void expand(final ExpandButton button) {
    boolean expanded = button.isExpanded();
    if (matchvol1Header != null && matchvol1Header.equalsOpenClose(button)) {
      pnlMatchvol1Body.setVisible(expanded);
    }
    else if (matchvol1Header != null && matchvol1Header.equalsAdvancedBasic(button)) {
      updateMatchvol1Advanced(expanded);
    }
    UIHarness.INSTANCE.pack(AxisID.ONLY, applicationManager);
  }

  /**
   * Set the solvematch parameters from the ConstSolvematchParam object
   * @param solvematchParam
   */
  void setSolvematchParams(final ConstSolvematchParam solvematchParam) {
    pnlSolvematch.setParameters(solvematchParam);
  }

  void setDualvolmatchParams(final DualvolmatchParam param) {
    pnlSolvematch.setParameters(param);
  }

  /**
   * Get the solvematch parameters from the UI
   * @param solvematchsParam
   */
  boolean getSolvematchParams(final SolvematchParam solvematchParam,
    final boolean doValidation) {
    return pnlSolvematch.getParameters(solvematchParam, doValidation);
  }

  boolean getParameters(final DualvolmatchParam param, final boolean doValidation) {
    return pnlSolvematch.getParameters(param, doValidation);
  }

  void getParameters(final ReconScreenState screenState) {
    pnlSolvematch.getParameters(screenState);
  }

  final void setParameters(final ReconScreenState screenState) {
    pnlSolvematch.setParameters(screenState);
    btnMatchvolRestart
      .setButtonState(screenState.getButtonState(btnMatchvolRestart.getButtonStateKey()));
  }

  // InitialCombineFields interface pass-thru
  public FiducialMatch getSurfacesOrModels() {
    return pnlSolvematch.getSurfacesOrModels();
  }

  public void setSurfacesOrModels(final FiducialMatch state) {
    pnlSolvematch.setSurfacesOrModels(state);
  }

  public boolean isBinBy2() {
    return pnlSolvematch.isBinBy2();
  }

  public void setBinBy2(final boolean state) {
    pnlSolvematch.setBinBy2(state);
  }

  public void setUseList(final String useList) {
    pnlSolvematch.setUseList(useList);
  }

  public void setFiducialMatchListA(final String fiducialMatchListA) {
    pnlSolvematch.setFiducialMatchListA(fiducialMatchListA);
  }

  public String getUseList(final boolean doValidation)
    throws FieldValidationFailedException {
    return pnlSolvematch.getUseList(doValidation);
  }

  public String getUseList() {
    return pnlSolvematch.getUseList();
  }

  public String getFiducialMatchListA(final boolean doValidation)
    throws FieldValidationFailedException {
    return pnlSolvematch.getFiducialMatchListA(doValidation);
  }

  public String getFiducialMatchListA() {
    return pnlSolvematch.getFiducialMatchListA();
  }

  public void setFiducialMatchListB(final String fiducialMatchListB) {
    pnlSolvematch.setFiducialMatchListB(fiducialMatchListB);
  }

  public String getFiducialMatchListB(final boolean doValidation)
    throws FieldValidationFailedException {
    return pnlSolvematch.getFiducialMatchListB(doValidation);
  }

  public String getFiducialMatchListB() {
    return pnlSolvematch.getFiducialMatchListB();
  }

  /**
   * Right mouse button context menu
   */
  public void popUpContextMenu(final MouseEvent mouseEvent) {
    String[] manPagelabel = { "Solvematch", "Matchshifts" };
    String[] manPage = { "solvematch.html", "matchshifts.html" };
    String[] logFileLabel = { "Transferfid", "Solvematch" };
    String[] logFile = { "transferfid.log", "solvematch.log" };

    ContextPopup contextPopup = new ContextPopup(pnlRoot, mouseEvent,
      "Initial Problems in Combining", ContextPopup.TOMO_GUIDE, manPagelabel, manPage,
      logFileLabel, logFile, applicationManager, AxisID.ONLY);
  }

  public void action(final String command, final Deferred3dmodButton deferred3dmodButton,
    final Run3dmodMenuOptions run3dmodMenuOptions) {
    // Synchronize this panel with the others
    tomogramCombinationDialog.synchronize(TomogramCombinationDialog.lblInitial, true);

    if (command.equals(btnMatchvolRestart.getActionCommand())) {
      applicationManager.matchvol1Combine(btnMatchvolRestart, null, deferred3dmodButton,
        run3dmodMenuOptions, dialogType,
        tomogramCombinationDialog.getRunProcessingMethod(),
        tomogramCombinationDialog.isParallel(),
        !tomogramCombinationDialog.isRunVolcombine());
    }
  }

  /**
   * Button action listener inner class
   */
  class ButtonActionListener implements ActionListener {
    private final InitialCombinePanel listenee;

    ButtonActionListener(final InitialCombinePanel initialCombinePanel) {
      listenee = initialCombinePanel;
    }

    public void actionPerformed(final ActionEvent event) {
      listenee.action(event.getActionCommand(), null, null);
    }
  }

  /**
   * Initialize the tooltip text for the axis panel objects
   */
  private void setToolTipText() {
    String text = "Thickness to make initial matching volume, which may need to be "
      + "thicker than the final matching volume to contain all the material needed for "
      + "patch correlations.";
    ltfOutputSizeY.setToolTipText(text);
    lOutputSizeYInfo.setToolTipText(text);
    btnMatchvolRestart.setToolTipText(
      "Resume and make first matching volume, despite a small displacement "
        + "between the match check volumes");
  }
}
