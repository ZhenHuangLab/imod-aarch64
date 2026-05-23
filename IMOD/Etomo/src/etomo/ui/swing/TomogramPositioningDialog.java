package etomo.ui.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import etomo.ApplicationManager;
import etomo.comscript.ConstTiltParam;
import etomo.comscript.ConstTiltalignParam;
import etomo.comscript.ConstTomopitchParam;
import etomo.comscript.CryoPositionParam;
import etomo.comscript.MakecomfileParam;
import etomo.comscript.TiltParam;
import etomo.comscript.TiltalignParam;
import etomo.comscript.TomopitchParam;
import etomo.storage.Network;
import etomo.storage.TomopitchLog;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.ConstEtomoNumber;
import etomo.type.ConstMetaData;
import etomo.type.DialogType;
import etomo.type.EtomoBoolean2;
import etomo.type.EtomoNumber;
import etomo.type.MetaData;
import etomo.type.ReconScreenState;
import etomo.type.Run3dmodMenuOptions;
import etomo.type.SampleType;
import etomo.type.TomogramState;
import etomo.type.ViewType;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;

/**
 * <p>Description: Tomogram Positioning User Interface</p>
 * 
 * <p>Copyright: Copyright 2002 - 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 1.4  2011/02/24 23:39:02  sueh
 * <p> bug# 1452 imageRotation needs to be double everywhere.
 * <p>
 * <p> Revision 1.3  2011/02/22 21:40:44  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.2  2010/12/05 05:23:50  sueh
 * <p> bug# 1420 Moved ProcessResultDisplayFactory to etomo.ui.swing package.  Removed static button construction functions.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 3.68  2010/03/30 00:05:47  sueh
 * <p> bug# 1331 Added useGpu checkbox.
 * <p>
 * <p> Revision 3.67  2009/09/20 21:34:09  sueh
 * <p> bug# 1268 Added a default value to LabeledSpinner.
 * <p>
 * <p> Revision 3.66  2009/09/01 03:18:25  sueh
 * <p> bug# 1222
 * <p>
 * <p> Revision 3.65  2009/01/20 20:31:55  sueh
 * <p> bug# 1102 Changed labeled panels to type EtomoPanel so that they can name themselves.
 * <p>
 * <p> Revision 3.64  2008/10/16 22:32:47  sueh
 * <p> bug# 1141 Removed fixRootPanel because it doesn't do anything.
 * <p>
 * <p> Revision 3.63  2008/09/30 22:46:19  sueh
 * <p> bug# 1113 Using a private constructor in SpacedPanel.
 * <p>
 * <p> Revision 3.62  2008/05/13 23:08:07  sueh
 * <p> bug# 847 Adding a right click menu for deferred 3dmods to some
 * <p> process buttons.
 * <p>
 * <p> Revision 3.61  2008/05/07 02:45:38  sueh
 * <p> bug# 847 Getting the the postioning buttons from the expert.
 * <p>
 * <p> Revision 3.60  2008/05/07 00:27:46  sueh
 * <p> bug# 847 Putting a shared label into the same string.
 * <p>
 * <p> Revision 3.59  2008/05/03 00:57:39  sueh
 * <p> bug# 847 Passing null for ProcessSeries to process funtions.
 * <p>
 * <p> Revision 3.58  2007/12/26 22:37:09  sueh
 * <p> bug# 1052 Return true when done() completes successfully.
 * <p>
 * <p> Revision 3.57  2007/05/01 22:30:41  sueh
 * <p> bug# 964 In LabeledSpinner, saving SpinnerNumberModel so that the
 * <p> maximum can be changed.
 * <p>
 * <p> Revision 3.56  2007/02/09 00:54:13  sueh
 * <p> bug# 962 Made TooltipFormatter a singleton and moved its use to low-level ui
 * <p> classes.
 * <p>
 * <p> Revision 3.55  2007/02/05 23:45:57  sueh
 * <p> bug# 962 Moved EtomoNumber type info to inner class.
 * <p>
 * <p> Revision 3.54  2006/10/26 23:58:19  sueh
 * <p> bug# 953 added/updated tooltips
 * <p>
 * <p> Revision 3.53  2006/09/14 00:01:30  sueh
 * <p> bug# z shift and angle offset fields associated with tilt.
 * <p>
 * <p> Revision 3.52  2006/07/28 20:14:28  sueh
 * <p> bug# 868 Changed isFiduciallessAlignment to isFiducialess
 * <p>
 * <p> Revision 3.51  2006/07/20 17:23:17  sueh
 * <p> bug# 848 Made UIParameters a singleton.
 * <p>
 * <p> Revision 3.50  2006/07/18 18:01:45  sueh
 * <p> bug# 906 Added isAlignButtonEnabled().
 * <p>
 * <p> Revision 3.49  2006/07/05 23:26:51  sueh
 * <p> Added tooltips.
 * <p>
 * <p> Revision 3.48  2006/07/04 18:04:16  sueh
 * <p> bug# 896 Fixed done() - don't remove action listeners if done returns false.
 * <p>
 * <p> Revision 3.47  2006/07/04 05:35:12  mast
 * <p> Fixed double colon on Z shift display
 * <p>
 * <p> Revision 3.46  2006/06/30 20:04:25  sueh
 * <p> bug# 877 Calling all the done dialog functions from the dialog done() functions,
 * <p> which is called by the button action functions and saveAction() in
 * <p> ProcessDialog.  Removed the button action function overides.  Set displayed to
 * <p> false after the done dialog function is called.
 * <p>
 * <p> Revision 3.45  2006/06/21 15:55:26  sueh
 * <p> bug# 581 Passing axis to ContextPopup, so that imodqtassist can be run.
 * <p>
 * <p> Revision 3.44  2006/06/09 19:51:29  sueh
 * <p> bug# 870 Added isTomopitchButtonSelected() and isAlignButtonSelected().
 * <p>
 * <p> Revision 3.43  2006/05/19 19:51:27  sueh
 * <p> bug# 866 Moved set/getParam functions, and updateDisplay functions to TomogramPositioningExpert.
 * <p> Added field-level set, get, and setEnabled functions.
 * <p>
 * <p> Revision 3.42  2006/05/11 19:32:29  sueh
 * <p> bug# 838 Added CalcPanel - a three variable panel which shows the
 * <p> calculation of angles calculated by tomopitch.  Added x axis tilt and
 * <p> thickness (separate from the sample thickness.)  Added extra thickness.
 * <p>
 * <p> Revision 3.41  2006/02/06 21:22:42  sueh
 * <p> bug# 521 Getting toggle buttons through ProcessResultDisplayFactory.
 * <p>
 * <p> Revision 3.40  2006/01/26 22:09:00  sueh
 * <p> bug# 401 For MultiLineButton toggle buttons:  save the state and keep
 * <p> the buttons turned on each they are run, unless the process fails or is
 * <p> killed.
 * <p>
 * <p> Revision 3.39  2006/01/20 21:13:46  sueh
 * <p> bug# 401 Saving the state of btnSample.
 * <p>
 * <p> Revision 3.38  2006/01/04 00:01:21  sueh
 * <p> bug# 675 Converted JCheckBox's to CheckBox.
 * <p>
 * <p> Revision 3.37  2005/11/14 22:29:13  sueh
 * <p> bug# 762 Made buttonAction() protected.
 * <p>
 * <p> Revision 3.36  2005/08/12 00:01:26  sueh
 * <p> bug# 711  Change enum Run3dmodMenuOption to
 * <p> Run3dmodMenuOptions, which can turn on multiple options at once.
 * <p> This allows ImodState to combine input from the context menu and the
 * <p> pulldown menu.  Prevent context menu from popping up when button is
 * <p> disabled.  Get rid of duplicate code by running the 3dmods from a private
 * <p> function called run3dmod(String, Run3dmodMenuOptions).  It can be
 * <p> called from run3dmod(Run3dmodButton, Run3dmodMenuOptions) and the
 * <p> action function.
 * <p>
 * <p> Revision 3.35  2005/08/10 20:48:26  sueh
 * <p> bug# 711 Removed MultiLineToggleButton.  Making toggling an attribute
 * <p> of MultiLineButton.
 * <p>
 * <p> Revision 3.34  2005/08/09 21:11:23  sueh
 * <p> bug# 711  Implemented Run3dmodButtonContainer:  added run3dmod().
 * <p> Changed 3dmod buttons to Run3dmodButton.  No longer inheriting
 * <p> MultiLineButton from JButton.
 * <p>
 * <p> Revision 3.33  2005/08/04 20:17:32  sueh
 * <p> bug# 532  Centralizing fit window functionality by placing fitting functions
 * <p> in UIHarness.  Removing packMainWindow from the manager.  Sending
 * <p> the manager to UIHarness.pack() so that packDialogs() can be called.
 * <p>
 * <p> Revision 3.32  2005/07/14 22:16:51  sueh
 * <p> bug# 626 Enabling whole tomogram for montage view.  Setting binning in
 * <p> set and getParameters(BlendmontParam).
 * <p>
 * <p> Revision 3.31  2005/06/13 23:40:13  sueh
 * <p> bug# 675 Using NamedCheckBox instead of JCheckBox to try out
 * <p> jfcUnit.  Bug# 583 Always save the screen value of binning in metaData.
 * <p>
 * <p> Revision 3.30  2005/06/11 03:01:20  sueh
 * <p> bug# 583, bug# 682, bug# 677, bug# 584  Moved binning calculation to
 * <p> ApplicationManager.  Storing screen binning for Tomo Pos and Tomo
 * <p> Gen in MetaData separately (Tomo Pos default is 3).  Upgraded tilt.com
 * <p> to have all unbinned parameters and a binning value.  No longer
 * <p> managing full image size in tilt.com, except to upgrade the file.  Fixed
 * <p> potential divide by 0 errors and incorrect binning calculation errors in
 * <p> Tomo Pos.  Added member variable:  getBinningFromNewst.  Removed
 * <p> member variables:  fullImageSize, prealiHeader, rawstackheader.  Add
 * <p> functions:  setParameters(ConstMetaData),
 * <p> setParameters(ConstNewstParam).  Modified functions:
 * <p> TomogramPostioningDialog(), getAlignParams, getBinning,
 * <p> getNewstParamst, getTiltParams, getTomopitchParams,
 * <p> setAlignParams, setTiltParams, updateMetaData.  Deleted functions:
 * <p> getFullImageSize, setFullImageSize, setNewstParams.
 * <p>
 * <p> Revision 3.29  2005/06/01 21:28:01  sueh
 * <p> bug# 667 Standardizing getMetaData function names.
 * <p>
 * <p> Revision 3.28  2005/04/21 20:55:25  sueh
 * <p> bug# 615 Pass axisID to packMainWindow so it can pack only the frame
 * <p> that requires it.
 * <p>
 * <p> Revision 3.27  2005/04/16 02:05:36  sueh
 * <p> bug# 615 Moved the adding of exit buttons to the base class.
 * <p>
 * <p> Revision 3.26  2005/03/29 19:54:59  sueh
 * <p> bug# 623 Added getBinning().
 * <p>
 * <p> Revision 3.25  2005/03/02 00:14:11  sueh
 * <p> bug# 533 disabled whole tomogram for montaging.
 * <p>
 * <p> Revision 3.24  2005/02/19 00:31:30  sueh
 * <p> bug# 606 Removed MetaData (Setup) zfactors, fiducialess, wholetomogram,
 * <p> and localalignments.  Add them for A and B.
 * <p>
 * <p> Revision 3.23  2005/01/14 03:11:45  sueh
 * <p> bug# 511 Added DialogType to super constructor.
 * <p>
 * <p> Revision 3.22  2004/12/29 00:16:56  sueh
 * <p> bug# 567 Adapting to new tiltalignParam.
 * <p>
 * <p> Revision 3.21  2004/12/09 04:58:45  sueh
 * <p> bug# 565 Removed BaseManager.isDataParamDirty.  Automatically
 * <p> saving to param file on exit.
 * <p>
 * <p> Revision 3.20  2004/12/03 02:41:43  sueh
 * <p> bug# Added updateMetaData().  For new it just copies wholeTomogram to
 * <p> meta data.  Calling updateMetaData() from all getParam() functions.
 * <p>
 * <p> Revision 3.19  2004/12/02 20:42:52  sueh
 * <p> bug# 566 ContextPopup can specify an anchor in both the tomo guide and
 * <p> the join guide.  Need to specify the guide to anchor.
 * <p>
 * <p> Revision 3.18  2004/11/20 00:06:50  sueh
 * <p> bug# 520 merging Etomo_3-4-6_JOIN branch to head.
 * <p>
 * <p> Revision 3.17.2.1  2004/10/11 02:19:56  sueh
 * <p> bug# 520 Passed the manager to the ContextPopup object in order to get
 * <p> the propertyUserDir.
 * <p>
 * <p> Revision 3.17  2004/07/02 17:43:11  sueh
 * <p> bug# 461 scale z shift by preali binning
 * <p>
 * <p> Revision 3.16  2004/06/28 20:44:45  sueh
 * <p> bug# 472
 * <p>
 * <p> Revision 3.15  2004/06/28 20:40:44  sueh
 * <p> bug# 472
 * <p>
 * <p> Revision 3.14  2004/06/22 02:19:39  sueh
 * <p> bug# 459 switch tooltips on btnSample when switch button
 * <p> title.
 * <p>
 * <p> Revision 3.13  2004/06/22 02:07:12  sueh
 * <p> bug# 481 Fixed display error.
 * <p>
 * <p> Revision 3.12  2004/06/17 20:18:53  sueh
 * <p> bug# 472
 * <p>
 * <p> Revision 3.11  2004/06/17 18:50:08  sueh
 * <p> bug# 472
 * <p>
 * <p> Revision 3.10  2004/06/01 18:56:32  rickg
 * <p> Bug #391 whole tomogram sampling state implementation
 * <p>
 * <p> Revision 3.9  2004/05/25 23:25:52  rickg
 * <p> Bug #391 added fiducialess parameter interface and UI objects
 * <p>
 * <p> Revision 3.8  2004/05/03 18:24:57  sueh
 * <p> bug# 427 set TomopitchParam.ScaleFactor from binning
 * <p>
 * <p> Revision 3.7  2004/04/29 20:25:57  sueh
 * <p> bug# 427 added commented out testing code
 * <p>
 * <p> Revision 3.6  2004/04/27 01:02:46  sueh
 * <p> bugt# 427 added set and get TomopitchParam
 * <p>
 * <p> Revision 3.5  2004/04/26 18:32:05  rickg
 * <p> bug #426 Added full image code
 * <p>
 * <p> Revision 3.4  2004/04/26 00:20:21  rickg
 * <p> bug #426 Implemented full tomogram sampling
 * <p>
 * <p> Revision 3.3  2004/04/22 23:25:05  rickg
 * <p> bug #391 adding non-fid alignement capability
 * <p>
 * <p> Revision 3.2  2004/03/15 20:33:55  rickg
 * <p> button variable name changes to btn...
 * <p>
 * <p> Revision 3.1  2004/01/30 22:45:28  sueh
 * <p> bug# 356 Changing buttons with html labels to
 * <p> MultiLineButton and MultiLineToggleButton
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:01  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.10  2003/10/30 01:43:44  rickg
 * <p> Bug# 338 Remapped context menu entries
 * <p>
 * <p> Revision 2.9  2003/10/29 00:28:32  rickg
 * <p> Bug# 297 Tooltips
 * <p>
 * <p> Revision 2.8  2003/10/28 23:35:48  rickg
 * <p> Bug# 336 Context menu label capitalization
 * <p>
 * <p> Revision 2.7  2003/10/28 00:23:47  rickg
 * <p> Bug# 336 Context menu label capitalization
 * <p>
 * <p> Revision 2.6  2003/10/27 23:58:18  rickg
 * <p> Bug# 284 tooltips
 * <p>
 * <p> Revision 2.5  2003/10/14 23:15:24  rickg
 * <p> Bug# 282 Label fixes
 * <p>
 * <p> Revision 2.4  2003/06/05 21:07:12  rickg
 * <p> Label change to match log file
 * <p>
 * <p> Revision 2.3  2003/05/23 22:13:47  rickg
 * <p> Removed any extensions from log file labels in context menu
 * <p>
 * <p> Revision 2.2  2003/04/28 23:25:25  rickg
 * <p> Changed visible imod references to 3dmod
 * <p>
 * <p> Revision 2.1  2003/03/02 23:30:41  rickg
 * <p> Combine layout in progress
 * <p>
 * <p> Revision 2.0  2003/01/24 20:30:31  rickg
 * <p> Single window merge to main branch
 * <p>
 * <p> Revision 1.7.2.1  2003/01/24 18:43:37  rickg
 * <p> Single window GUI layout initial revision
 * <p>
 * <p> Revision 1.7  2002/12/19 17:45:22  rickg
 * <p> Implemented advanced dialog state processing
 * <p> including:
 * <p> default advanced state set on start up
 * <p> advanced button management now handled by
 * <p> super class
 * <p>
 * <p> Revision 1.6  2002/12/19 00:30:26  rickg
 * <p> app manager and root pane moved to super class
 * <p>
 * <p> Revision 1.5  2002/11/14 21:18:37  rickg
 * <p> Added anchors into the tomoguide
 * <p>
 * <p> Revision 1.4  2002/10/17 22:40:29  rickg
 * <p> Added fileset name to window title
 * <p> this reference removed applicationManager messages
 * <p>
 * <p> Revision 1.3  2002/10/07 22:31:18  rickg
 * <p> removed unused imports
 * <p> reformat after emacs trashed it
 * <p>
 * <p> Revision 1.2  2002/09/19 21:37:57  rickg
 * <p> Removed stdout messages
 * <p>
 * <p> Revision 1.1  2002/09/09 22:57:02  rickg
 * <p> Initial CVS entry, basic functionality not including combining
 * <p> </p>
 */
final class TomogramPositioningDialog extends ProcessDialog
  implements ContextMenu, FiducialessParams, Run3dmodButtonContainer {
  static final String SAMPLE_TOMOGRAMS_LABEL = "Create Sample Tomograms";
  static final String SAMPLE_TOMOGRAMS_TOOLTIP =
    "Build 3 sample tomograms for finding location and angles of section.";
  private static final String HAS_GOLD_BEADS_LABEL = "Sample has gold beads of size:";
  private static final String AUTO_WHOLE_TOMOGRAM_TOOLTIP =
    "Builds a sample tomogram and creates a boundary model.";
  private static final String CREATE_BOUNDARY_LABEL = "Create Boundary Model";
  private static final String EXTRA_THICKNESS_LABEL =
    "Added border thickness (unbinned): ";

  private final LabeledTextField ltfSampleThickness =
    new LabeledTextField(FieldType.INTEGER, "Positioning tomogram thickness: ");
  private final LabeledTextField ltfExtraThickness =
    new LabeledTextField(FieldType.FLOATING_POINT, EXTRA_THICKNESS_LABEL);
  private final LabeledTextField ltfThickness =
    new LabeledTextField(FieldType.INTEGER, "Final Tomogram Thickness: ");
  private final CheckBox cbFiducialess = new CheckBox("Coarse alignment only");
  private final LabeledTextField ltfRotation =
    new LabeledTextField(FieldType.FLOATING_POINT, "Tilt axis rotation:");
  private final LabeledSpinner spinBinning =
    LabeledSpinner.getInstance("   Binning ", 3, 1, 8, 1);
  private final CheckBox cbWholeTomogram = new CheckBox("Use whole tomogram");
  private final Run3dmodButton btnCreateBoundary =
    Run3dmodButton.get3dmodInstance(CREATE_BOUNDARY_LABEL, this);
  private final EtomoPanel pnlFinalAlign = new EtomoPanel();
  private final CalcPanel cpAngleOffset = new CalcPanel("Angle offset");
  private CalcPanel cpTiltAxisZShift = new CalcPanel("Z shift");
  private final CalcPanel cpXAxisTilt = new CalcPanel("X axis tilt");
  private final LocalActionListener localActionListener = new LocalActionListener(this);
  private final CalcPanel cpTiltAngleOffset = new CalcPanel("Tilt angle offset");
  private final CalcPanel cpZShift = new CalcPanel("Z shift");
  private final CheckBox cbUseGpu = new CheckBox("Use the GPU");
  private final CheckBox cbSampleTypeAuto =
    new CheckBox("Find boundary model automatically");
  private final CheckBox cbSampleTypeCryo =
    new CheckBox("Do positioning for cryo sample");
  private final CheckBox cbHasGoldBeads = new CheckBox(HAS_GOLD_BEADS_LABEL);
  private final TextField tfBeadSize =
    new TextField(FieldType.FLOATING_POINT, HAS_GOLD_BEADS_LABEL, null);
  private final JLabel lBeadSize = new JLabel(" pixels");
  private final LabeledTextField ltfExtraThicknessCryo =
    new LabeledTextField(FieldType.FLOATING_POINT, EXTRA_THICKNESS_LABEL);

  private final Run3dmodButton btnSample;
  private final MultiLineButton btnTomopitch;
  private final MultiLineButton btnAlign;
  private final TomogramPositioningExpert expert;
  private final TomogramState state;
  private final AxisType axisType;
  private final ViewType viewType;

  private TomogramPositioningDialog(ApplicationManager appMgr,
    TomogramPositioningExpert expert, AxisID axisID, final AxisType axisType,
    final ViewType viewType) {
    super(appMgr, axisID, DialogType.TOMOGRAM_POSITIONING);
    this.expert = expert;
    this.axisType = axisType;
    this.viewType = viewType;
    state = appMgr.getState();
    ProcessResultDisplayFactory displayFactory =
      appMgr.getProcessResultDisplayFactory(axisID);
    btnSample = (Run3dmodButton) displayFactory.getSampleTomogram();
    btnSample.setContainer(this);
    btnSample.setDeferred3dmodButton(btnCreateBoundary);
    btnTomopitch = (MultiLineButton) displayFactory.getComputePitch();
    btnAlign = (MultiLineButton) displayFactory.getFinalAlignment();
    rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
    btnExecute.setText("Done");
    // Construct the binning spinner
    spinBinning.setTextMaxmimumSize(UIParameters.getInstance().getSpinnerDimension());

    // Create the primary panels
    JPanel pnlWholeTomogram = new JPanel();
    pnlWholeTomogram.setLayout(new BoxLayout(pnlWholeTomogram, BoxLayout.X_AXIS));
    // if (appMgr.getMetaData().getViewType() == ViewType.MONTAGE) {
    // cbWholeTomogram.setEnabled(false);
    // }
    pnlWholeTomogram.add(cbWholeTomogram.getComponent());
    pnlWholeTomogram.add(spinBinning.getContainer());

    JPanel pnlTomoParams = new JPanel();
    JPanel pnlGoldBeads = new JPanel();
    pnlTomoParams.setLayout(new BoxLayout(pnlTomoParams, BoxLayout.Y_AXIS));
    UIUtilities.addWithYSpace(pnlTomoParams, cbUseGpu.getComponent());
    UIUtilities.addWithYSpace(pnlTomoParams, cbSampleTypeAuto.getComponent());
    UIUtilities.addWithYSpace(pnlTomoParams, cbSampleTypeCryo.getComponent());
    UIUtilities.addWithYSpace(pnlTomoParams, pnlGoldBeads);
    UIUtilities.addWithYSpace(pnlTomoParams, ltfSampleThickness.getContainer());
    UIUtilities.addWithYSpace(pnlTomoParams, cbFiducialess.getComponent());
    UIUtilities.addWithYSpace(pnlTomoParams, ltfRotation.getContainer());
    UIUtilities.addWithYSpace(pnlTomoParams, pnlWholeTomogram);
    UIUtilities.alignComponentsX(pnlTomoParams, Component.LEFT_ALIGNMENT);
    // GoldBeads
    pnlGoldBeads.setLayout(new BoxLayout(pnlGoldBeads, BoxLayout.X_AXIS));
    pnlGoldBeads.add(cbHasGoldBeads.getComponent());
    pnlGoldBeads.add(tfBeadSize.getComponent());
    pnlGoldBeads.add(lBeadSize);
    //
    pnlFinalAlign.setLayout(new BoxLayout(pnlFinalAlign, BoxLayout.Y_AXIS));
    pnlFinalAlign.setBorder(new EtchedBorder("Final Alignment").getBorder());
    UIUtilities.addWithYSpace(pnlFinalAlign, cpAngleOffset.getContainer());
    UIUtilities.addWithYSpace(pnlFinalAlign, cpTiltAxisZShift.getContainer());
    UIUtilities.addWithSpace(pnlFinalAlign, btnAlign.getComponent(), FixedDim.x0_y10);
    UIUtilities.alignComponentsX(pnlFinalAlign, Component.CENTER_ALIGNMENT);

    JPanel pnlPosition = new JPanel();
    pnlPosition.setBorder(new BeveledBorder("Tomogram Positioning").getBorder());
    pnlPosition.setLayout(new BoxLayout(pnlPosition, BoxLayout.Y_AXIS));

    UIUtilities.addWithYSpace(pnlPosition, pnlTomoParams);
    UIUtilities.addWithSpace(pnlPosition, btnSample.getComponent(), FixedDim.x0_y10);
    UIUtilities.addWithSpace(pnlPosition, btnCreateBoundary.getComponent(),
      FixedDim.x0_y10);
    UIUtilities.addWithSpace(pnlPosition, ltfExtraThickness.getContainer(),
      FixedDim.x0_y10);
    UIUtilities.addWithSpace(pnlPosition, ltfExtraThicknessCryo.getContainer(),
      FixedDim.x0_y10);
    UIUtilities.addWithSpace(pnlPosition, btnTomopitch.getComponent(), FixedDim.x0_y10);
    UIUtilities.addWithYSpace(pnlPosition, pnlFinalAlign);

    SpacedPanel pnlTiltParameters = SpacedPanel.getInstance();
    pnlTiltParameters.setBoxLayout(BoxLayout.Y_AXIS);
    pnlTiltParameters.setBorder(new EtchedBorder("Tilt Parameters").getBorder());
    pnlTiltParameters.add(cpTiltAngleOffset.getContainer());
    pnlTiltParameters.add(cpZShift.getContainer());
    pnlTiltParameters.add(cpXAxisTilt.getContainer());
    pnlTiltParameters.add(ltfThickness);
    pnlPosition.add(pnlTiltParameters.getContainer());

    UIUtilities.alignComponentsX(pnlPosition, Component.CENTER_ALIGNMENT);
    UIUtilities.setButtonSizeAll(pnlPosition,
      UIParameters.getInstance().getButtonDimension());

    // Create dialog content pane
    rootPanel.add(pnlPosition);
    addExitButtons();
    setToolTipText();
    UIHarness.INSTANCE.pack(axisID, applicationManager);
  }

  static TomogramPositioningDialog getInstance(ApplicationManager manager,
    TomogramPositioningExpert expert, AxisID axisID, final AxisType axisType,
    final ViewType viewType) {
    TomogramPositioningDialog instance =
      new TomogramPositioningDialog(manager, expert, axisID, axisType, viewType);
    instance.addListeners();
    return instance;
  }

  private void addListeners() {
    cbFiducialess.addActionListener(localActionListener);
    cbWholeTomogram.addActionListener(localActionListener);
    btnSample.addActionListener(localActionListener);
    btnCreateBoundary.addActionListener(localActionListener);
    btnTomopitch.addActionListener(localActionListener);
    btnAlign.addActionListener(localActionListener);
    cbSampleTypeAuto.addActionListener(localActionListener);
    cbSampleTypeCryo.addActionListener(localActionListener);
    cbHasGoldBeads.addActionListener(localActionListener);
    // Mouse adapter for context menu
    rootPanel.addMouseListener(new GenericMouseAdapter(this));
  }

  public boolean isFiducialess() {
    return cbFiducialess.isSelected();
  }

  void setImageRotation(String tiltAxisAngle) {
    ltfRotation.setText(tiltAxisAngle);
  }

  public String getImageRotation(final boolean doValidation)
    throws NumberFormatException, FieldValidationFailedException {
    return ltfRotation.getText(doValidation);
  }

  boolean getAlignParams(final TiltalignParam tiltalignParam, final MetaData metaData,
    final boolean doValidation) {
    try {
      tiltalignParam.setAngleOffset(cpAngleOffset.getTotal(doValidation));
      tiltalignParam.setAxisZShift(cpTiltAxisZShift.getTotal(doValidation));
      updateMetaData(metaData);
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  /**
   * Get the tomopitch.com parameters from the dialog
   * @param TomopitchParam
   */
  boolean getTomopitchParam(TomopitchParam tomopitchParam, final MetaData metaData,
    final boolean doValidation) {
    try {
      tomopitchParam.setScaleFactor(cbWholeTomogram.isSelected());
      if (!isSampleTypeCryo()) {
        tomopitchParam.setExtraThickness(ltfExtraThickness.getText(doValidation));
      }
      else {
        tomopitchParam.setExtraThickness(ltfExtraThicknessCryo.getText(doValidation));
      }
      tomopitchParam.setAngleOffsetOld(state.getSampleAngleOffset(axisID));
      tomopitchParam.setZShiftOld(state.getSampleAxisZShift(axisID));
      tomopitchParam.setXAxisTiltOld(state.getSampleXAxisTilt(axisID));
      updateMetaData(metaData);
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  boolean getParameters(final MakecomfileParam param, final boolean doValidation) {
    try {
      param.setThicknessToMake(ltfSampleThickness.getText(doValidation));
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  boolean getTiltParamsForSample(TiltParam tiltParam, final boolean doValidation) {
    try {
      tiltParam.setThickness(ltfSampleThickness.getText(doValidation));
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  boolean isWholeTomogram() {
    return cbWholeTomogram.isSelected();
  }

  void updateMetaData(final MetaData metaData) {
    boolean wholeTomogram = isWholeTomogram();
    if (wholeTomogram != metaData.isWholeTomogramSample(axisID)) {
      metaData.setWholeTomogramSample(axisID, wholeTomogram);
    }
    metaData.setPosBinning(axisID, ((Integer) spinBinning.getValue()).intValue());
  }

  int getBinning() {
    if (!isWholeTomogram()) {
      return 1;
    }
    return ((Integer) spinBinning.getValue()).intValue();
  }

  /**
   * Get the tilt.com parameters from the dialog
   * @param tiltParam
   * @throws NumberFormatException
   */
  boolean getTiltParams(final TiltParam tiltParam, final MetaData metaData,
    final boolean doValidation) {
    tiltParam.setUseGpu(cbUseGpu.isEnabled() && cbUseGpu.isSelected());
    boolean fiducialess = isFiducialess();
    tiltParam.setFiducialess(fiducialess);
    try {
      if (fiducialess) {
        tiltParam.setZShift(cpZShift.getTotal(doValidation));
        tiltParam.setTiltAngleOffset(cpTiltAngleOffset.getTotal(doValidation));
      }
      if (isSampleTypeCryo()) {
        tiltParam.setXAxisTilt(0);
      }
      else {
        tiltParam.setXAxisTilt(cpXAxisTilt.getTotal(doValidation));
      }
      tiltParam.setImageBinned(getBinning());
      tiltParam.setThickness(ltfThickness.getText(doValidation));
      updateMetaData(metaData);
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  private SampleType getSampleType() {
    if (!cbSampleTypeAuto.isSelected()) {
      return SampleType.NONE;
    }
    if (cbSampleTypeCryo.isSelected()) {
      return SampleType.CRYO;
    }
    return SampleType.PLASTIC_SECTION;
  }

  private void setSampleType(final SampleType input) {
    cbSampleTypeAuto
      .setSelected(input == SampleType.PLASTIC_SECTION || input == SampleType.CRYO);
    cbSampleTypeCryo.setSelected(input == SampleType.CRYO);
    updateDisplay();
  }

  /**
   * If state.sampleFiducialess doesn't match the fiducialess checkbox, don't
   * allow Tomopitch to be run.  If state.sampleFiducialess is null (sample was
   * never run) enable Tomopitch.
   */
  void updateDisplay() {
    // If state.sampleFiducialess doesn't match the fiducialess checkbox, don't
    // allow Tomopitch to be run. If state.sampleFiducialess is null (sample was
    // never run) enable Tomopitch.
    EtomoBoolean2 sampleFiducialess = state.getSampleFiducialess(axisID);
    boolean enable = false;
    boolean fiducialess = isFiducialess();
    if (sampleFiducialess == null || sampleFiducialess.is() == fiducialess) {
      enable = true;
    }
    btnTomopitch.setEnabled(enable);
    cpAngleOffset.setEnabled(enable && !fiducialess);
    cpTiltAxisZShift.setEnabled(enable && !fiducialess);
    btnAlign.setEnabled(enable && !fiducialess);
    cpTiltAngleOffset.setEnabled(enable);
    cpZShift.setEnabled(enable);
    cpXAxisTilt.setEnabled(enable);
    ltfThickness.setEnabled(enable);
    // Fiducialless
    ltfRotation.setEnabled(fiducialess);
    pnlFinalAlign.setVisible(!fiducialess);
    cpTiltAngleOffset.setVisible(fiducialess);
    cpZShift.setVisible(fiducialess);
    boolean sampleTypeAuto = cbSampleTypeAuto.isSelected();
    cbSampleTypeCryo.setEnabled(sampleTypeAuto && viewType != ViewType.MONTAGE);
    boolean cryo = isSampleTypeCryo();
    cbHasGoldBeads.setEnabled(cryo);
    tfBeadSize.setEnabled(cryo);
    lBeadSize.setEnabled(cryo);
    ltfExtraThickness.setVisible(!cryo);
    ltfExtraThicknessCryo.setVisible(cryo);
    // WholeTomogram
    // Cryo is always whole tomogram
    cbWholeTomogram.setEditable(!cryo);
    if (cryo) {
      cbWholeTomogram.setSelected(true);
    }
    boolean wholeTomogram = cbWholeTomogram.isSelected();
    spinBinning.setEnabled(!cryo && wholeTomogram);
    // Sample
    if (!sampleTypeAuto) {
      btnCreateBoundary.setText(CREATE_BOUNDARY_LABEL);
      if (wholeTomogram) {
        btnSample.setText("Create Whole Tomogram");
        btnSample.setToolTipText("Create whole tomogram for drawing positioning model.");
      }
      else {
        btnSample.setText(SAMPLE_TOMOGRAMS_LABEL);
        btnSample.setToolTipText(SAMPLE_TOMOGRAMS_TOOLTIP);
      }
    }
    else {
      btnCreateBoundary.setText("View Boundary Model");
      if (!cryo) {
        if (!wholeTomogram) {
          btnSample.setText("Create Samples & Boundary Model");
          btnSample
            .setToolTipText("Builds 3 sample tomograms and creates a boundary model.");
        }
        else {
          btnSample.setText("Create Tomogram & Boundary Model");
          btnSample.setToolTipText(AUTO_WHOLE_TOMOGRAM_TOOLTIP);
        }
      }
      else {
        btnSample.setText("Find Boundary Model for Cryo");
        btnSample.setToolTipText(AUTO_WHOLE_TOMOGRAM_TOOLTIP);
      }
    }
    boolean hasGoldBeads = cbHasGoldBeads.isEnabled() && cbHasGoldBeads.isSelected();
    tfBeadSize.setEnabled(hasGoldBeads);
    lBeadSize.setEnabled(hasGoldBeads);
  }

  boolean isSampleTypeAuto() {
    return cbSampleTypeAuto.isSelected();
  }

  boolean isSampleTypeCryo() {
    return cbSampleTypeCryo.isEnabled() && cbSampleTypeCryo.isSelected();
  }

  /**
   * Set the metadata parameters in the dialog
   * @param newstParam
   */
  void setParameters(ConstMetaData metaData) {
    // Use GPU
    cbUseGpu.setEnabled(Network.isLocalHostGpuProcessingEnabled(applicationManager,
      axisID, applicationManager.getPropertyUserDir()));
    cbUseGpu.setSelected(metaData.isDefaultGpuProcessing());
    spinBinning.setValue(metaData.getPosBinning(axisID));
    ltfSampleThickness.setText(metaData.getSampleThickness(axisID));
    setSampleType(metaData.getSampleType(axisID));
    if (metaData.isPositioningNewDialog(axisID)) {
      // Default bead size
      cbHasGoldBeads.setSelected(metaData.isFiducialDiameterAvailable());
      tfBeadSize
        .setText(Double.toString(applicationManager.calcUnbinnedBeadDiameterPixels()));
    }
    else {
      // Saved bead size
      cbHasGoldBeads.setSelected(metaData.isHasGoldBeads(axisID));
      if (!metaData.isPositioningBeadSizeNull(axisID)) {
        tfBeadSize.setText(metaData.getPositioningBeadSize(axisID));
      }
    }
    ltfExtraThickness.setText(metaData.getExtraThickness(axisID));
    ltfExtraThicknessCryo.setText(metaData.getExtraThicknessCryo(axisID));
  }

  boolean isTomopitchButton() {
    return btnTomopitch.isSelected();
  }

  void getParameters(MetaData metaData) {
    metaData.setPositioningNewDialog(axisID, false);
    metaData.setSampleType(axisID, getSampleType());
    metaData.setSampleThickness(axisID, ltfSampleThickness.getText());
    metaData.setHasGoldBeads(axisID, cbHasGoldBeads.isSelected());
    metaData.setPositioningBeadSize(axisID, tfBeadSize.getText());
    metaData.setExtraThickness(axisID, ltfExtraThickness.getText());
    metaData.setExtraThicknessCryo(axisID, ltfExtraThicknessCryo.getText());
  }

  void setParameters(final CryoPositionParam param) {
    cbHasGoldBeads.setSelected(param.isBeadSizeSet());
    tfBeadSize.setText(param.getBeadSize());
  }

  boolean getParameters(final CryoPositionParam param, final boolean doValidation) {
    try {
      boolean hasGoldBeads = cbHasGoldBeads.isSelected();
      param.setFindBeadsInVolume(hasGoldBeads);
      if (hasGoldBeads) {
        param.setBeadSize(tfBeadSize.getText(doValidation));
      }
      else {
        param.resetBeadSize();
      }
      param.setThicknessOfTomograms(ltfSampleThickness.getText(doValidation));
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  boolean isAlignButton() {
    return btnAlign.isSelected();
  }

  boolean isAlignButtonEnabled() {
    return btnAlign.isEnabled();
  }

  void setButtonState(final ReconScreenState screenState) {
    btnSample.setButtonState(screenState.getButtonState(btnSample.getButtonStateKey()));
    btnTomopitch
      .setButtonState(screenState.getButtonState(btnTomopitch.getButtonStateKey()));
    btnAlign.setButtonState(screenState.getButtonState(btnAlign.getButtonStateKey()));
  }

  void setTiltParam(ConstTiltParam tiltParam, boolean initialize) {
    if (!initialize) {
      // During initialization the value should coming from setup
      cbUseGpu.setSelected(tiltParam.isUseGpu());
    }
    cpXAxisTilt.set(tiltParam.getXAxisTilt());
    ltfThickness.setText(tiltParam.getThickness());
    cpTiltAngleOffset.set(tiltParam.getTiltAngleOffset());
    cpZShift.set(tiltParam.getZShift());
  }

  void rollAlignComAngles() {
    cpAngleOffset.updateDisplay(false);
    cpTiltAxisZShift.updateDisplay(false);
  }

  void rollTiltComAngles() {
    cpXAxisTilt.updateDisplay(false);
  }

  void setAlignParam(ConstTiltalignParam tiltalignParam) {
    cpAngleOffset.set(tiltalignParam.getAngleOffset());
    cpTiltAxisZShift.set(tiltalignParam.getAxisZShift());
  }

  void setParametersFiducialess(final TiltParam param, final MetaData metaData) {
    param.setFiducialess(metaData.isFiducialess(axisID));
    if (isFiducialess()) {
      cpTiltAngleOffset.set(param.getTiltAngleOffset());
      cpZShift.set(param.getZShift());
    }
    else {
      param.setTiltAngleOffset(cpTiltAngleOffset.getTotal());
      param.setZShift(cpZShift.getTotal());
    }
    param.resetSubsetStart();
  }

  void setFiducialess(final ConstMetaData metaData) {
    cbFiducialess.setSelected(metaData.isFiducialessAlignment(axisID));
    updateDisplay();
  }

  boolean setParameters(TomopitchLog log) {
    boolean missingData = false;
    ConstEtomoNumber angleOffsetOriginal = log.getAngleOffsetOriginal();
    ConstEtomoNumber angleOffsetAdded = log.getAngleOffsetAdded();
    ConstEtomoNumber angleOffsetTotal = log.getAngleOffsetTotal();
    ConstEtomoNumber axisZShiftOriginal = log.getAxisZShiftOriginal();
    ConstEtomoNumber axisZShiftAdded = log.getAxisZShiftAdded();
    ConstEtomoNumber axisZShiftTotal = log.getAxisZShiftTotal();
    missingData =
      !cpAngleOffset.set(angleOffsetOriginal, angleOffsetAdded, angleOffsetTotal);
    missingData =
      !cpTiltAxisZShift.set(axisZShiftOriginal, axisZShiftAdded, axisZShiftTotal);
    missingData = !cpXAxisTilt.set(log.getXAxisTiltOriginal(), log.getXAxisTiltAdded(),
      log.getXAxisTiltTotal());
    cpTiltAngleOffset.set(angleOffsetOriginal, angleOffsetAdded, angleOffsetTotal);
    cpZShift.set(axisZShiftOriginal, axisZShiftAdded, axisZShiftTotal);
    ConstEtomoNumber thickness = log.getThickness();
    if (thickness.isNull()) {
      missingData = true;
    }
    else {
      ltfThickness.setText(thickness);
    }
    UIHarness.INSTANCE.pack(axisID, applicationManager);
    return !missingData;
  }

  void setTomopitchParam(ConstTomopitchParam tomopitchParam) {
    if (!tomopitchParam.isExtraThicknessNull()) {
      String extraThickness = tomopitchParam.getExtraThicknessString();
      if (!isSampleTypeCryo()) {
        ltfExtraThickness.setText(extraThickness);
      }
      else {
        ltfExtraThicknessCryo.setText(extraThickness);
      }
    }
  }

  /**
   * Set the whole tomogram sampling state
   * @param state
   */
  void setWholeTomogram(boolean state) {
    cbWholeTomogram.setSelected(state);
  }

  /**
   * Right mouse button context menu
   */
  public void popUpContextMenu(MouseEvent mouseEvent) {
    String[] manPagelabel =
      { "Tomopitch", "Findsection", "Cryoposition", "Newstack", "3dmod", "Tilt" };
    String[] manPage = { "tomopitch.html", "findsection.html", "cryoposition.html",
      "newstack.html", "3dmod.html", "tilt.html" };
    String[] logFileLabel = { "Tomopitch", "Sample", "Cryoposition" };
    String[] logFile = new String[3];
    logFile[0] = "tomopitch" + axisID.getExtension() + ".log";
    logFile[1] = "sample" + axisID.getExtension() + ".log";
    logFile[2] = "cryoposition" + axisID.getExtension() + ".log";
    ContextPopup contextPopup = new ContextPopup(rootPanel, mouseEvent,
      "TOMOGRAM POSITIONING", ContextPopup.TOMO_GUIDE, manPagelabel, manPage,
      logFileLabel, logFile, applicationManager, axisID);
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
    if (command.equals(btnSample.getActionCommand())) {
      expert.sampleAction(btnSample, null, deferred3dmodButton, run3dmodMenuOptions);
    }
    else if (command.equals(btnTomopitch.getActionCommand())) {
      expert.tomopitch(btnTomopitch, null);
    }
    else if (command.equals(btnAlign.getActionCommand())) {
      expert.finalAlign(btnAlign, null);
    }
    else if (command.equals(cbFiducialess.getActionCommand())) {
      expert.fiducialessAction();
    }
    else if (command.equals(btnCreateBoundary.getActionCommand())) {
      expert.createBoundary(run3dmodMenuOptions);
    }
    else {
      updateDisplay();
    }
  }

  public void done() {
    expert.doneDialog();
    btnSample.removeActionListener(localActionListener);
    btnTomopitch.removeActionListener(localActionListener);
    btnAlign.removeActionListener(localActionListener);
    setDisplayed(false);
  }

  //
  // Action listener adapters
  //
  private final class LocalActionListener implements ActionListener {
    private final TomogramPositioningDialog adaptee;

    private LocalActionListener(final TomogramPositioningDialog adaptee) {
      this.adaptee = adaptee;
    }

    public void actionPerformed(final ActionEvent event) {
      adaptee.action(event.getActionCommand(), null, null);
    }
  }

  /**
   * Initialize the tooltip text for the axis panel objects
   */
  private void setToolTipText() {
    ltfSampleThickness.setToolTipText(
      "Thickness of sample slices, or unbinned thickness of whole tomogram.  Make this "
        + "much larger than expected section thickness to see borders of section.");
    btnSample.setToolTipText(SAMPLE_TOMOGRAMS_TOOLTIP);
    btnCreateBoundary.setToolTipText(
      "Open samples in 3dmod to make a model with lines along top and bottom "
        + "edges of the section in each sample.");
    btnTomopitch.setToolTipText(
      "Run tomopitch.  This will compute the positioning values and adjust the totals shown here.");
    cpAngleOffset.setToolTipText(
      "The total offset is sum of the original offset and the additional offset from tomopitch.");
    cpXAxisTilt.setToolTipText(TomogramGenerationDialog.X_AXIS_TILT_TOOLTIP);
    cpTiltAxisZShift
      .setToolTipText("The total shift is the sum of the original shift and the"
        + "additional shift from tomopitch.");
    btnAlign.setToolTipText("Run tiltalign with these final offset parameters.");
    cbWholeTomogram.setToolTipText(
      "Generate an entire tomogram instead of 3 samples and draw boundary "
        + "lines in this tomogram.");
    spinBinning
      .setToolTipText("Set the binning for the whole tomogram to be used for positioning."
        + "  With a binned tomogram, the tomopitch output and entries for "
        + "offset and thickness will still be in unbinned pixels.");
    cbFiducialess.setToolTipText("Use cross-correlation alignment only.");
    ltfRotation
      .setToolTipText("Rotation angle of tilt axis for generating aligned stack from "
        + "cross-correlation alignment only.");
    String tooltip =
      "Extra thickness to be added to the top and bottom of the final tomogram.";
    ltfExtraThickness.setToolTipText(tooltip);
    ltfExtraThicknessCryo.setToolTipText(tooltip);
    ltfThickness.setToolTipText("The thickness of the final tomogram.");
    cpTiltAngleOffset.setToolTipText(
      "Tilt parameter:  the spatial frequency at which to switch from the R-weighted radial "
        + "filter to a Gaussian falloff.  Frequency is in cycles/pixel and "
        + "ranges from 0-0.5.  Both a cutoff and a falloff must be entered.");
    cpZShift.setToolTipText(
      "Tilt parameter:  amount to shift the reconstructed slices in Z before output.  A "
        + "positive value will shift the slice upward.  Do not use this option"
        + " if you have fiducials and the tomogram is part of a dual-axis " + "series.");
    cbUseGpu.setToolTipText("Check to run the tilt process on the graphics card.");
    cbSampleTypeAuto.setToolTipText(
      "Find surfaces of material automatically using Findsection for plastic section "
        + "data or Cryposition for a cryo sample.");
    cbSampleTypeCryo.setToolTipText(
      "Use Cryoposition to find surfaces of material in low contrast reconstructions.");
    cbHasGoldBeads.setToolTipText(
      "Check this if there are any gold beads, even if not used for alignment; "
        + "Cryoposition needs to take them into account.");
    tfBeadSize.setToolTipText("Size of gold beads in unbinned pixels");
  }

  public static final class CalcPanel {
    public static final String ADDED_KEY = "Added";
    public static final int MAX_DIGITS = 6;
    private final SpacedPanel panel = SpacedPanel.getInstance();
    private final JLabel label;
    private final LabeledTextField ltfOriginal =
      new LabeledTextField(FieldType.FLOATING_POINT, "Original:");
    private final LabeledTextField ltfAdded =
      new LabeledTextField(FieldType.FLOATING_POINT, ADDED_KEY + ':');
    private final LabeledTextField ltfTotal =
      new LabeledTextField(FieldType.FLOATING_POINT, "Total:");
    // utility field
    private final EtomoNumber number = new EtomoNumber(EtomoNumber.Type.DOUBLE);
    private boolean more = true;

    CalcPanel(String label) {
      this.label = new JLabel(label + ":");
      panel.setBoxLayout(BoxLayout.X_AXIS);
      panel.add(this.label);
      panel.addRigidArea();
      panel.add(ltfOriginal);
      panel.add(ltfAdded);
      panel.add(ltfTotal);
      ltfOriginal.setEditable(false);
      ltfOriginal.setColumns(MAX_DIGITS);
      ltfAdded.setEditable(false);
      ltfAdded.setColumns(MAX_DIGITS);
      ltfTotal.setColumns(MAX_DIGITS);
      ltfOriginal.setText("0.0");
      ltfAdded.setText("0.0");
      ltfTotal.setText("0.0");
      updateDisplay(false);
    }

    Container getContainer() {
      return panel.getContainer();
    }

    void setEnabled(boolean enabled) {
      ltfTotal.setEnabled(enabled);
    }

    void setToolTipText(String text) {
      label.setToolTipText(text);
      ltfOriginal.setToolTipText(text);
      ltfAdded.setToolTipText(text);
      ltfTotal.setToolTipText(text);
    }

    void setVisible(boolean visible) {
      panel.setVisible(visible);
    }

    void set(ConstEtomoNumber total) {
      updateDisplay(false);
      set(total, ltfTotal);
    }

    void set(double total) {
      number.set(total);
      set(number);
    }

    boolean set(ConstEtomoNumber original, ConstEtomoNumber added,
      ConstEtomoNumber total) {
      if (total.isNull()) {
        return false;
      }
      updateDisplay(true);
      set(original, ltfOriginal);
      set(added, ltfAdded);
      set(total, ltfTotal);
      return true;
    }

    private void set(ConstEtomoNumber number, LabeledTextField field) {
      if (number == null || number.isNull() || !number.isValid()) {
        field.setText("0.0");
      }
      else {
        field.setText(number.toString());
      }
    }

    void updateDisplay(boolean more) {
      if (this.more != more) {
        this.more = more;
        ltfOriginal.setVisible(more);
        ltfAdded.setVisible(more);
      }
    }

    String getOriginal(final boolean doValidation) throws FieldValidationFailedException {
      return ltfOriginal.getText(doValidation);
    }

    private String getTotal(final boolean doValidation)
      throws FieldValidationFailedException {
      return ltfTotal.getText(doValidation);
    }

    private String getTotal() {
      return ltfTotal.getText();
    }
  }
}
