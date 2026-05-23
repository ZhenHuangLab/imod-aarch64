package etomo.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import etomo.ApplicationManager;
import etomo.comscript.MultifiltSetupParam;
import etomo.process.ImodManager;
import etomo.storage.LogFile;
import etomo.storage.MultifiltOutputFileFilter;
import etomo.storage.autodoc.AutodocFactory;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.type.AxisID;
import etomo.type.ConstEtomoNumber;
import etomo.type.ConstMetaData;
import etomo.type.DialogType;
import etomo.type.EnumeratedType;
import etomo.type.EtomoAutodoc;
import etomo.type.FileType;
import etomo.type.ImageFilenameStyle;
import etomo.type.MetaData;
import etomo.type.ReconScreenState;
import etomo.type.Run3dmodMenuOptions;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.SharedStrings;

final class MultifiltPanel implements ActionListener, MultifiltSetupDisplay,
  Run3dmodButtonContainer, Expandable, FilterType {
  private final JPanel pnlRoot = new JPanel();
  private final JPanel pnlParametersBody = new JPanel();
  private final ButtonGroup bgFilter = new ButtonGroup();
  private final RadioButton rbFakeSIRTiterations =
    new RadioButton(FilterType.FAKE_SIRT_ITERATIONS, bgFilter);
  private final RadioButton rbExactObjectSizes =
    new RadioButton(FilterType.EXACT_OBJECT_SIZES, bgFilter);
  private final RadioButton rbGaussian = new RadioButton(FilterType.GAUSSIAN, bgFilter);
  private final RadioButton rbHammingLikeStarts =
    new RadioButton(FilterType.HAMMING_LIKE_STARTS, bgFilter);
  private final TextField tfFakeSIRTiterations = new TextField(FieldType.INTEGER_LIST,
    FilterType.FAKE_SIRT_ITERATIONS.getLabel(), null);
  private final TextField tfExactObjectSizes =
    new TextField(FieldType.INTEGER_LIST, FilterType.EXACT_OBJECT_SIZES.getLabel(), null);
  private final TextField tfGaussianCutoffs =
    new TextField(FieldType.FLOATING_POINT_ARRAY, FilterType.GAUSSIAN.getLabel(), null);
  private final LabeledTextField ltfGaussianFalloffs =
    new LabeledTextField(FieldType.FLOATING_POINT_ARRAY, " falloffs:");
  private final TextField tfHammingLikeStarts = new TextField(
    FieldType.FLOATING_POINT_ARRAY, FilterType.HAMMING_LIKE_STARTS.getLabel(), null);
  private final Run3dmodButton btn3dmodMultifiltSetup =
    Run3dmodButton.get3dmodInstance("View Tomogram(s) In 3dmod", this);
  //
  private final LabeledTextField ltfWidthInX =
    new LabeledTextField(FieldType.INTEGER, "Tomogram width in X: ");
  private final LabeledTextField ltfShiftInX =
    new LabeledTextField(FieldType.FLOATING_POINT, "X shift: ");
  private final LabeledTextField ltfSizeInY =
    new LabeledTextField(FieldType.INTEGER, "Tomogram height in Y: ");
  private final LabeledTextField ltfShiftInY =
    new LabeledTextField(FieldType.INTEGER, " Y shift: ");
  private final LabeledTextField ltfThicknessInZ =
    new LabeledTextField(FieldType.INTEGER, "Tomogram thickness in Z: ");
  private final LabeledTextField ltfShiftInDepth =
    new LabeledTextField(FieldType.FLOATING_POINT, " Z shift: ");

  private final AxisID axisID;
  private final ApplicationManager manager;
  private final TomogramGenerationDialog parent;
  private final DialogType dialogType;
  private final Run3dmodButton btnMultifiltSetup;
  private final PanelHeader header;
  private final MultifiltOutputFileFilter[] fileFilterArray =
    new MultifiltOutputFileFilter[FilterType.ARRAY_SIZE];
  private final ImageFilenameStyle imageFilenameStyle;

  private List<ActionListener> listenerList = null;

  private MultifiltPanel(final ApplicationManager manager, final AxisID axisID,
    final TomogramGenerationDialog parent, final DialogType dialogType) {
    this.manager = manager;
    this.axisID = axisID;
    this.parent = parent;
    this.dialogType = dialogType;
    imageFilenameStyle = manager.getBaseMetaData().getImageFilenameStyle();
    btnMultifiltSetup =
      (Run3dmodButton) manager.getProcessResultDisplayFactory(axisID).getMultifiltSetup();
    header = PanelHeader.getInstance("Filter Trials", this, dialogType);
  }

  static MultifiltPanel getInstance(final ApplicationManager manager, final AxisID axisID,
    final TomogramGenerationDialog parent, final DialogType dialogType) {
    MultifiltPanel instance = new MultifiltPanel(manager, axisID, parent, dialogType);
    instance.createPanel();
    instance.setTooltips();
    instance.addListeners();
    return instance;
  }

  private void createPanel() {
    // init
    rbFakeSIRTiterations.setSelected(true);
    tfFakeSIRTiterations.setRequired(true);
    tfExactObjectSizes.setRequired(true);
    tfHammingLikeStarts.setRequired(true);
    ltfWidthInX.setPreferredWidth(163);
    ltfSizeInY.setPreferredWidth(159);
    btnMultifiltSetup.setContainer(this);
    btnMultifiltSetup.setDeferred3dmodButton(btn3dmodMultifiltSetup);
    tfGaussianCutoffs.setPreferredWidth(108);
    ltfGaussianFalloffs.setPreferredWidth(109);
    createFileFilter(FilterType.FAKE_SIRT_ITERATIONS);
    createFileFilter(FilterType.EXACT_OBJECT_SIZES);
    createFileFilter(FilterType.GAUSSIAN);
    createFileFilter(FilterType.HAMMING_LIKE_STARTS);
    createFileFilter(null);
    // panels
    JPanel pnlParameters = new JPanel();
    JPanel pnlFilterToTry = new JPanel();
    JPanel pnlFakeSIRTiterations = new JPanel();
    JPanel pnlExactObjectSizes = new JPanel();
    JPanel pnlGaussian = new JPanel();
    JPanel pnlHammingLikeStarts = new JPanel();
    JPanel pnlSubarea = new JPanel();
    JPanel pnlX = new JPanel();
    JPanel pnlY = new JPanel();
    JPanel pnlZ = new JPanel();
    JPanel pnlButtons = new JPanel();
    //
    // Root
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
    pnlRoot.add(pnlParameters);
    pnlRoot.add(pnlButtons);
    //
    // Parameters
    pnlParameters.setLayout(new BoxLayout(pnlParameters, BoxLayout.Y_AXIS));
    pnlParameters.setBorder(BorderFactory.createEtchedBorder());
    pnlParameters.add(header.getComponent());
    pnlParameters.add(pnlParametersBody);
    // ParametersBody
    pnlParametersBody.setLayout(new BoxLayout(pnlParametersBody, BoxLayout.Y_AXIS));
    pnlParametersBody.add(pnlFilterToTry);
    pnlParametersBody.add(pnlSubarea);
    //
    // FilterToTry
    pnlFilterToTry.setLayout(new BoxLayout(pnlFilterToTry, BoxLayout.Y_AXIS));
    pnlFilterToTry.setBorder(new EtchedBorder("Filter To Try").getBorder());
    pnlFilterToTry.add(pnlFakeSIRTiterations);
    pnlFilterToTry.add(pnlExactObjectSizes);
    pnlFilterToTry.add(pnlGaussian);
    pnlFilterToTry.add(pnlHammingLikeStarts);
    // FakeSIRTiterations
    pnlFakeSIRTiterations
      .setLayout(new BoxLayout(pnlFakeSIRTiterations, BoxLayout.X_AXIS));
    pnlFakeSIRTiterations.add(rbFakeSIRTiterations.getComponent());
    pnlFakeSIRTiterations.add(tfFakeSIRTiterations.getComponent());
    // ExactObjectSizes
    pnlExactObjectSizes.setLayout(new BoxLayout(pnlExactObjectSizes, BoxLayout.X_AXIS));
    pnlExactObjectSizes.add(rbExactObjectSizes.getComponent());
    pnlExactObjectSizes.add(tfExactObjectSizes.getComponent());
    // Gaussian
    pnlGaussian.setLayout(new BoxLayout(pnlGaussian, BoxLayout.X_AXIS));
    pnlGaussian.add(rbGaussian.getComponent());
    pnlGaussian.add(tfGaussianCutoffs.getComponent());
    pnlGaussian.add(ltfGaussianFalloffs.getComponent());
    // HammingLikeStarts
    pnlHammingLikeStarts.setLayout(new BoxLayout(pnlHammingLikeStarts, BoxLayout.X_AXIS));
    pnlHammingLikeStarts.add(rbHammingLikeStarts.getComponent());
    pnlHammingLikeStarts.add(tfHammingLikeStarts.getComponent());
    //
    // Subarea
    pnlSubarea.setLayout(new BoxLayout(pnlSubarea, BoxLayout.Y_AXIS));
    pnlSubarea.setBorder(new EtchedBorder("Subarea").getBorder());
    pnlSubarea.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlSubarea.add(pnlX);
    pnlSubarea.add(Box.createRigidArea(FixedDim.x0_y3));
    pnlSubarea.add(pnlY);
    pnlSubarea.add(Box.createRigidArea(FixedDim.x0_y3));
    pnlSubarea.add(pnlZ);
    pnlSubarea.add(Box.createRigidArea(FixedDim.x0_y5));
    // X panel
    pnlX.setLayout(new BoxLayout(pnlX, BoxLayout.X_AXIS));
    pnlX.add(ltfWidthInX.getContainer());
    pnlX.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlX.add(ltfShiftInX.getContainer());
    // Y panel
    pnlY.setLayout(new BoxLayout(pnlY, BoxLayout.X_AXIS));
    pnlY.add(ltfSizeInY.getContainer());
    pnlY.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlY.add(ltfShiftInY.getContainer());
    // Z panel
    pnlZ.setLayout(new BoxLayout(pnlZ, BoxLayout.X_AXIS));
    pnlZ.add(ltfThicknessInZ.getContainer());
    pnlZ.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlZ.add(ltfShiftInDepth.getContainer());
    //
    // Buttons
    pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
    pnlButtons.add(btnMultifiltSetup.getComponent());
    pnlButtons.add(btn3dmodMultifiltSetup.getComponent());
    updateDisplay();
  }

  /**
   * Load file filters into an array.
   * @param filterType
   */
  private void createFileFilter(final FilterType filterType) {
    if (filterType != null) {
      fileFilterArray[filterType.index] = MultifiltOutputFileFilter.getInstance(manager,
        imageFilenameStyle, axisID, filterType.fileType);
    }
    else {
      // This is the all-filters element
      fileFilterArray[FilterType.ARRAY_SIZE - 1] =
        MultifiltOutputFileFilter.getInstance(manager, imageFilenameStyle, axisID, null);
    }
  }

  private void addListeners() {
    rbFakeSIRTiterations.addActionListener(this);
    rbExactObjectSizes.addActionListener(this);
    rbGaussian.addActionListener(this);
    rbHammingLikeStarts.addActionListener(this);
    btnMultifiltSetup.addActionListener(this);
    btn3dmodMultifiltSetup.addActionListener(this);
  }

  public void addActionListener(ActionListener listener) {
    if (listener == null) {
      return;
    }
    // In order to signal the listener when fields values have been set.
    if (listenerList == null) {
      listenerList = new ArrayList<ActionListener>();
    }
    listenerList.add(listener);
    rbFakeSIRTiterations.addActionListener(listener);
    rbExactObjectSizes.addActionListener(listener);
    rbGaussian.addActionListener(listener);
    rbHammingLikeStarts.addActionListener(listener);
  }

  Component getComponent() {
    return pnlRoot;
  }

  public void actionPerformed(ActionEvent event) {
    action(event != null ? event.getActionCommand() : null, null, null);
  }

  public void action(final String actionCommand,
    final Deferred3dmodButton deferred3dmodButton,
    final Run3dmodMenuOptions run3dmodMenuOptions) {
    if (actionCommand.equals(btnMultifiltSetup.getActionCommand())) {
      manager.multifiltSetup(axisID, btnMultifiltSetup, null, dialogType,
        parent.getProcessingMethod(), this);
    }
    else if (actionCommand.equals(btn3dmodMultifiltSetup.getActionCommand())) {
      openFilesInImod(run3dmodMenuOptions);
    }
    else {
      updateDisplay();
    }
  }

  public boolean isRadialFilter() {
    return rbFakeSIRTiterations.isSelected() || rbExactObjectSizes.isSelected();
  }

  public boolean isHighFrequencyFilter() {
    return rbGaussian.isSelected() || rbHammingLikeStarts.isSelected();
  }

  @Override
  public final void expand(final ExpandButton button) {
    if (header != null) {
      if (header.equalsOpenClose(button)) {
        pnlParametersBody.setVisible(button.isExpanded());
      }
    }
    UIHarness.INSTANCE.pack(axisID, manager);
  }

  @Override
  public final void expand(final GlobalExpandButton button) {}

  /**
   * Ask the user to one or more files to open in 3dmod.  If only one file is available,
   * open in 3dmod without asking.
   * @param run3dmodMenuOptions
   */
  private void openFilesInImod(final Run3dmodMenuOptions run3dmodMenuOptions) {
    // Don't open the file chooser if there is only one file to choose
    MultifiltOutputFileFilter fileFilter =
      MultifiltOutputFileFilter.getInstance(manager, imageFilenameStyle, axisID, null);
    JFileChooser chooser = new FileChooser(manager);
    chooser.setPreferredSize(UIParameters.getInstance().getFileChooserDimension());
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setMultiSelectionEnabled(true);
    for (int i = 0; i < fileFilterArray.length; i++) {
      chooser.addChoosableFileFilter(fileFilterArray[i]);
    }
    FilterType filterType =
      (FilterType) ((RadioButton.RadioButtonModel) bgFilter.getSelection())
        .getEnumeratedType();
    chooser.setFileFilter(fileFilterArray[filterType.index]);
    int returnVal = chooser.showOpenDialog(pnlRoot);
    if (returnVal != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File[] fileList = chooser.getSelectedFiles();
    if (fileList == null || fileList.length == 0) {
      return;
    }
    manager.openFilesInImod(axisID, ImodManager.MULTIFILT_KEY, fileList,
      run3dmodMenuOptions);
  }

  private void updateDisplay() {
    tfFakeSIRTiterations.setEnabled(rbFakeSIRTiterations.isSelected());
    tfExactObjectSizes.setEnabled(rbExactObjectSizes.isSelected());
    boolean enabled = rbGaussian.isSelected();
    tfGaussianCutoffs.setEnabled(enabled);
    ltfGaussianFalloffs.setEnabled(enabled);
    tfHammingLikeStarts.setEnabled(rbHammingLikeStarts.isSelected());
  }

  void msgMethodChanged(final TiltPanel tiltPanel) {
    pnlRoot.setVisible(parent.isMultifilt());
  }

  void done() {
    rbFakeSIRTiterations.removeActionListener(this);
    rbExactObjectSizes.removeActionListener(this);
    rbGaussian.removeActionListener(this);
    rbHammingLikeStarts.removeActionListener(this);
    btnMultifiltSetup.removeActionListener(this);
  }

  void getParameters(final ReconScreenState screenState) {
    btnMultifiltSetup
      .setButtonState(screenState.getButtonState(btnMultifiltSetup.getButtonStateKey()));
  }

  void setParameters(final ReconScreenState screenState) {
    btnMultifiltSetup
      .setButtonState(screenState.getButtonState(btnMultifiltSetup.getButtonStateKey()));
  }

  void getParameters(final MetaData metaData) {
    metaData.setGenFilterTrialsFakeSIRTiterations(axisID, tfFakeSIRTiterations.getText());
    metaData.setGenFilterTrialsExactObjectSizes(axisID, tfExactObjectSizes.getText());
    metaData.setGenFilterTrialsGaussianCutoffs(axisID, tfGaussianCutoffs.getText());
    metaData.setGenFilterTrialsGaussianFalloffs(axisID, ltfGaussianFalloffs.getText());
    metaData.setGenFilterTrialsHammingLikeStarts(axisID, tfHammingLikeStarts.getText());
  }

  void setParameters(final ConstMetaData metaData) {
    tfFakeSIRTiterations.setText(metaData.getGenFilterTrialsFakeSIRTiterations(axisID));
    tfExactObjectSizes.setText(metaData.getGenFilterTrialsExactObjectSizes(axisID));
    tfGaussianCutoffs.setText(metaData.getGenFilterTrialsGaussianCutoffs(axisID));
    ltfGaussianFalloffs.setText(metaData.getGenFilterTrialsGaussianFalloffs(axisID));
    tfHammingLikeStarts.setText(metaData.getGenFilterTrialsHammingLikeStarts(axisID));
    signalListeners();
  }

  private void signalListeners() {
    if (listenerList != null) {
      Iterator<ActionListener> iterator = listenerList.iterator();
      while (iterator.hasNext()) {
        iterator.next().actionPerformed(null);
      }
    }
  }

  void setParameters(final MultifiltSetupParam param) {
    if (param.isFakeSIRTiterations()) {
      rbFakeSIRTiterations.setSelected(true);
      tfFakeSIRTiterations.setText(param.getFakeSIRTiterations());
    }
    else if (param.isExactObjectSizes()) {
      rbExactObjectSizes.setSelected(true);
      tfExactObjectSizes.setText(param.getExactObjectSizes());
    }
    else if (param.isGaussianCutoffs() || param.isGaussianFalloffs()) {
      rbGaussian.setSelected(true);
      tfGaussianCutoffs.setText(param.getGaussianCutoffs());
      ltfGaussianFalloffs.setText(param.getGaussianFalloffs());
    }
    else if (param.isHammingLikeStarts()) {
      rbHammingLikeStarts.setSelected(true);
      tfHammingLikeStarts.setText(param.getHammingLikeStarts());
    }
    ltfWidthInX.setText(param.getWidthInX());
    ltfShiftInX.setText(param.getShiftInX());
    ltfSizeInY.setText(param.getSizeInY());
    ltfShiftInY.setText(param.getShiftInY());
    ltfThicknessInZ.setText(param.getThicknessInZ());
    ltfShiftInDepth.setText(param.getShiftInDepth());
    updateDisplay();
    signalListeners();
  }

  public boolean getParameters(final MultifiltSetupParam param,
    final boolean doValidation) {
    try {
      if (rbFakeSIRTiterations.isSelected()) {
        param.setFakeSIRTiterations(tfFakeSIRTiterations.getText(doValidation));
      }
      else {
        param.resetFakeSIRTiterations();
      }
      if (rbExactObjectSizes.isSelected()) {
        param.setExactObjectSizes(tfExactObjectSizes.getText(doValidation));
      }
      else {
        param.resetExactObjectSizes();
      }
      if (rbGaussian.isSelected()) {
        param.setGaussianCutoffs(tfGaussianCutoffs.getText(doValidation));
        param.setGaussianFalloffs(ltfGaussianFalloffs.getText(doValidation));
        if (doValidation) {
          tfGaussianCutoffs.validatePairedArrays(ltfGaussianFalloffs);
        }
      }
      else {
        param.resetGaussianCutoffs();
        param.resetGaussianFalloffs();
      }
      if (rbHammingLikeStarts.isSelected()) {
        param.setHammingLikeStarts(tfHammingLikeStarts.getText(doValidation));
      }
      else {
        param.resetHammingLikeStarts();
      }
      param.setWidthInX(ltfWidthInX.getText(doValidation));
      param.setShiftInX(ltfShiftInX.getText(doValidation));
      param.setSizeInY(ltfSizeInY.getText(doValidation));
      param.setShiftInY(ltfShiftInY.getText(doValidation));
      param.setThicknessInZ(ltfThicknessInZ.getText(doValidation));
      param.setShiftInDepth(ltfShiftInDepth.getText(doValidation));

    }
    catch (FieldValidationFailedException e) {
      return false;
    }
    return true;
  }

  private void setTooltips() {
    ReadOnlyAutodoc autodoc = null;
    try {
      autodoc = AutodocFactory.getInstance(manager, AutodocFactory.MULTIFILT_SETUP,
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
    rbFakeSIRTiterations
      .setToolTipText(SharedStrings.SIRT_LIKE_FILTER_RADIO_BUTTON_TOOLTIP);
    tfFakeSIRTiterations.setToolTipText(
      EtomoAutodoc.getTooltip(autodoc, MultifiltSetupParam.FAKE_SIRT_ITERATIONS));
    rbExactObjectSizes.setToolTipText(SharedStrings.EXACT_FILTER_RADIO_BUTTON_TOOLTIP);
    tfExactObjectSizes.setToolTipText(
      EtomoAutodoc.getTooltip(autodoc, MultifiltSetupParam.EXACT_OBJECT_SIZES));
    rbGaussian.setToolTipText(SharedStrings.GAUSSIAN_FILTER_RADIO_BUTTON_TOOLTIP);
    tfGaussianCutoffs.setToolTipText(
      EtomoAutodoc.getTooltip(autodoc, MultifiltSetupParam.GAUSSIAN_CUTOFFS));
    ltfGaussianFalloffs.setToolTipText(
      EtomoAutodoc.getTooltip(autodoc, MultifiltSetupParam.GAUSSIAN_FALLOFFS));
    rbHammingLikeStarts
      .setToolTipText(SharedStrings.HAMMING_LIKE_FILTER_RADIO_BUTTON_TOOLTIP);
    tfHammingLikeStarts.setToolTipText(
      EtomoAutodoc.getTooltip(autodoc, MultifiltSetupParam.HAMMING_LIKE_STARTS));
    //
    btnMultifiltSetup.setToolTipText(
      "Run multifiltsetup, and then run the resulting .com files with processchunks.");
    btn3dmodMultifiltSetup.setToolTipText(
      "Opens a file chooser for picking filter trial output to open together in 3dmod");
    //
    ltfWidthInX
      .setToolTipText("This entry specifies the width, in unbinned pixels, of the output "
        + "image; the default is the width of the input image.");
    ltfShiftInX
      .setToolTipText("Amount, in unbinned pixels, to shift the reconstructed slices in "
        + "X before output.  A positive value will shift the slice to the right, and "
        + "the output will contain the left part of the whole potentially "
        + "reconstructable area.  The default is no shift.");
    ltfSizeInY
      .setToolTipText("This entry specifies the Y extent of the output tomogram, in "
        + "unbinned pixels; the default is the height of the aligned stack.");
    ltfShiftInY.setToolTipText(
      "Amount to shift the reconstructed region in Y, in unbinned pixels.  "
        + "A positive value will shift the region upward and reconstruct an area lower "
        + "in Y.  The default is no shift.");
    ltfThicknessInZ
      .setToolTipText("Thickness, in unbinned pixels, along the z-axis of the "
        + "reconstructed volume.  The default is the value on the Back Projection "
        + "panel.");
    ltfShiftInDepth
      .setToolTipText("Amount, in unbinned pixels, to shift the reconstructed slices in "
        + "Z before output.  A positive value will shift the slice upward.  The default "
        + "is the value on the Back Projection panel.");
  }

  /**
   * Used to keep track of the file filters which correspond to different radio buttons.
   */
  private static final class FilterType implements EnumeratedType {
    private static int indexValue = 0;
    private static final FilterType FAKE_SIRT_ITERATIONS =
      new FilterType("SIRT-like filter with iterations:", indexValue++,
        FileType.MUTLIFILT_FAKE_SIRT_ITERATIONS_OUTPUT_TEMPLATE, true);
    private static final FilterType EXACT_OBJECT_SIZES =
      new FilterType("'Exact filter' function with 'object sizes':", indexValue++,
        FileType.MUTLIFILT_EXACT_OBJECT_SIZES_OUTPUT_TEMPLATE);
    private static final FilterType GAUSSIAN =
      new FilterType("Standard Gaussian with cutoffs:", indexValue++,
        FileType.MUTLIFILT_GAUSSIAN_OUTPUT_TEMPLATE);
    private static final FilterType HAMMING_LIKE_STARTS =
      new FilterType("Hamming-like filter with start frequencies:", indexValue++,
        FileType.MUTLIFILT_HAMMING_LIKE_STARTS_OUTPUT_TEMPLATE);

    // Make room in the array for the all-filter file filter
    private static final int ARRAY_SIZE = indexValue + 1;

    private final String label;
    private final int index;
    private final FileType fileType;
    private final boolean defaultInstance;

    private FilterType(final String label, final int index, final FileType fileType,
      final boolean defaultInstance) {
      this.label = label;
      this.index = index;
      this.fileType = fileType;
      this.defaultInstance = defaultInstance;
    }

    private FilterType(final String label, final int index, final FileType fileType) {
      this.label = label;
      this.index = index;
      this.fileType = fileType;
      this.defaultInstance = false;
    }

    public String getLabel() {
      return label;
    }

    public boolean isDefault() {
      return defaultInstance;
    }

    public ConstEtomoNumber getValue() {
      return null;
    }
  }
}
