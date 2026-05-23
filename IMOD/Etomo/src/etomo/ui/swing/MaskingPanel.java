package etomo.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;

import etomo.BaseManager;
import etomo.storage.LogFile;
import etomo.storage.MatlabParam;
import etomo.storage.VolumeFileFilter;
import etomo.storage.autodoc.AutodocFactory;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.type.AxisID;
import etomo.type.ConstPeetMetaData;
import etomo.type.EtomoAutodoc;
import etomo.type.EtomoNumber;
import etomo.type.PeetMetaData;
import etomo.type.Run3dmodMenuOptions;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.UIComponent;
import etomo.util.FilePath;
import etomo.util.Utilities;

/**
 * <p>Description:</p>
 * 
 * Copyright: Copyright 2009 - 2017 by the Regents of the University of Colorado
 * 
 * Organization: Dept. of MCD Biology, University of Colorado
 * 
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.2 2011/02/22 18:15:17 sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.1 2010/11/13 16:07:34 sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.3 2010/01/13 21:56:08 sueh
 * <p> bug# 1298 Passing parametersOnly to
 * <p> CylinderOrientationPanel.setParameters functions.
 * <p>
 * <p> Revision 1.2 2009/12/23 02:25:32 sueh
 * <p> bug# 1296 Stop taking tooltips from peetprm.adoc.
 * <p>
 * <p> Revision 1.1 2009/12/08 02:47:07 sueh
 * <p> bug# 1286 Factored MaskingPanel out of PeetDialog.
 * </p>
 */
final class MaskingPanel implements UIComponent, SwingComponent {
  private static final String INSIDE_MASK_RADIUS_LABEL = "Inner radius: ";
  private static final String OUTSIDE_MASK_RADIUS_LABEL = "Outer radius: ";
  static final String MAST_TYPE_LABEL = "Masking";
  private static final String MAST_TYPE_FILE_LABEL = "User supplied binary file:";

  private final EtomoPanel pnlRoot = new EtomoPanel();
  private final ButtonGroup bgMaskType = new ButtonGroup();
  private final RadioButton rbMaskTypeNone =
    new RadioButton("None", MatlabParam.MaskType.NONE, bgMaskType);
  private final RadioButton rbMaskTypeFile =
    new RadioButton(MAST_TYPE_FILE_LABEL, MatlabParam.MaskType.VOLUME, bgMaskType);
  private final RadioButton rbMaskTypeSphere =
    new RadioButton("Sphere", MatlabParam.MaskType.SPHERE, bgMaskType);
  private final RadioButton rbMaskTypeCylinder =
    new RadioButton("Cylinder", MatlabParam.MaskType.CYLINDER, bgMaskType);
  private final FileTextField2 ftfMaskTypeFile;
  private final LabeledTextField ltfInsideMaskRadius = new LabeledTextField(
    FieldType.FLOATING_POINT, INSIDE_MASK_RADIUS_LABEL, PeetDialog.SETUP_LOCATION_DESCR);
  private final LabeledTextField ltfOutsideMaskRadius = new LabeledTextField(
    FieldType.FLOATING_POINT, OUTSIDE_MASK_RADIUS_LABEL, PeetDialog.SETUP_LOCATION_DESCR);
  private final LabeledTextField ltfZRotation = new LabeledTextField(
    FieldType.FLOATING_POINT, "Z Rotation: ", PeetDialog.SETUP_LOCATION_DESCR);
  private final LabeledTextField ltfYRotation = new LabeledTextField(
    FieldType.FLOATING_POINT, "Y Rotation: ", PeetDialog.SETUP_LOCATION_DESCR);
  private final CheckBox cbCylinderOrientation =
    new CheckBox("Manual Cylinder Orientation");
  private final LabeledTextField ltfCylinderHeight = new LabeledTextField(
    FieldType.FLOATING_POINT, "Height: ", PeetDialog.SETUP_LOCATION_DESCR);
  private final LabeledTextField ltfMaskBlurStdDev = new LabeledTextField(
    FieldType.FLOATING_POINT, "Blur mask by: ", PeetDialog.SETUP_LOCATION_DESCR);

  private final MaskingParent parent;
  private final BaseManager manager;

  private MaskingPanel(BaseManager manager, MaskingParent parent) {
    this.manager = manager;
    this.parent = parent;
    ftfMaskTypeFile =
      FileTextField2.getUnlabeledPeetInstance(manager, MAST_TYPE_FILE_LABEL);
  }

  static MaskingPanel getInstance(BaseManager manager, MaskingParent parent) {
    MaskingPanel instance = new MaskingPanel(manager, parent);
    instance.createPanel();
    instance.setTooltips();
    instance.addListeners();
    return instance;
  }

  private void addListeners() {
    MaskingActionListener actionListener = new MaskingActionListener(this);
    rbMaskTypeNone.addActionListener(actionListener);
    rbMaskTypeFile.addActionListener(actionListener);
    rbMaskTypeSphere.addActionListener(actionListener);
    rbMaskTypeCylinder.addActionListener(actionListener);
    cbCylinderOrientation.addActionListener(actionListener);
  }

  private void createPanel() {
    // init
    ftfMaskTypeFile.setFileFilter(VolumeFileFilter.getInstance(manager));
    ftfMaskTypeFile.setUseTextAsFileChooserDir(true);
    ftfMaskTypeFile.setBrowsingDirectory(manager);
    ltfCylinderHeight.setNumberMustBePositive(true);
    ltfMaskBlurStdDev.setPreferredWidth(80);
    // local panels
    JPanel pnlMaskType = new JPanel();
    JPanel pnlMaskVolumeRadii = new JPanel();
    JPanel pnlFile = new JPanel();
    JPanel pnlMaskTypeNone = new JPanel();
    JPanel pnlMaskTypeSphere = new JPanel();
    JPanel pnlMaskTypeCylinder = new JPanel();
    JPanel pnlSphereCylinder = new JPanel();
    JPanel pnlRadius = new JPanel();
    JPanel pnlCylinderOrientation = new JPanel();
    JPanel pnlCylinderRotation = new JPanel();
    JPanel pnlCylinderOrientationCheckBox = new JPanel();
    JPanel pnlCylinderOrientationX = new JPanel();
    JPanel pnlMaskBlurStdDev = new JPanel();
    if (Utilities.APRIL_FOOLS) {
      Color background = new Color(255, 192, 136);
      pnlRoot.setBackground(background);
      pnlMaskType.setBackground(background);
      pnlSphereCylinder.setBackground(background);
      pnlMaskTypeNone.setBackground(background);
      pnlMaskTypeSphere.setBackground(background);
      pnlMaskTypeCylinder.setBackground(background);
      pnlFile.setBackground(background);
      ftfMaskTypeFile.setBackground(background);
      pnlRadius.setBackground(background);
      pnlMaskBlurStdDev.setBackground(background);
      background = new Color(251, 212, 159);
      pnlCylinderOrientationX.setBackground(background);
      pnlCylinderOrientation.setBackground(background);
      pnlCylinderRotation.setBackground(background);
      pnlCylinderOrientationCheckBox.setBackground(background);
    }
    // initalization
    ftfMaskTypeFile.setAdjustedFieldWidth(190);
    // root panel
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.X_AXIS));
    pnlRoot.setBorder(new EtchedBorder(MAST_TYPE_LABEL).getBorder());
    pnlRoot.add(pnlMaskType);
    pnlRoot.add(pnlSphereCylinder);
    // mask type
    pnlMaskType.setLayout(new BoxLayout(pnlMaskType, BoxLayout.Y_AXIS));
    pnlMaskType.add(pnlMaskTypeNone);
    pnlMaskType.add(pnlMaskTypeSphere);
    pnlMaskType.add(pnlMaskTypeCylinder);
    pnlMaskType.add(pnlFile);
    // SphereCylinder
    pnlSphereCylinder.setLayout(new BoxLayout(pnlSphereCylinder, BoxLayout.Y_AXIS));
    pnlSphereCylinder.add(pnlRadius);
    pnlSphereCylinder.add(Box.createRigidArea(FixedDim.x0_y2));
    pnlSphereCylinder.add(pnlMaskBlurStdDev);
    pnlSphereCylinder.add(Box.createRigidArea(FixedDim.x0_y5));
    pnlSphereCylinder.add(pnlCylinderOrientationX);
    pnlSphereCylinder.add(Box.createRigidArea(FixedDim.x0_y3));
    // MaskTypeNone
    pnlMaskTypeNone.setLayout(new BoxLayout(pnlMaskTypeNone, BoxLayout.X_AXIS));
    pnlMaskTypeNone.setAlignmentX(Box.RIGHT_ALIGNMENT);
    pnlMaskTypeNone.add(rbMaskTypeNone.getComponent());
    pnlMaskTypeNone.add(Box.createHorizontalGlue());
    // MaskTypeSphere
    pnlMaskTypeSphere.setLayout(new BoxLayout(pnlMaskTypeSphere, BoxLayout.X_AXIS));
    pnlMaskTypeSphere.setAlignmentX(Box.RIGHT_ALIGNMENT);
    pnlMaskTypeSphere.add(rbMaskTypeSphere.getComponent());
    pnlMaskTypeSphere.add(Box.createHorizontalGlue());
    // MaskTypeCylinder
    pnlMaskTypeCylinder.setLayout(new BoxLayout(pnlMaskTypeCylinder, BoxLayout.X_AXIS));
    pnlMaskTypeCylinder.setAlignmentX(Box.RIGHT_ALIGNMENT);
    pnlMaskTypeCylinder.add(rbMaskTypeCylinder.getComponent());
    pnlMaskTypeCylinder.add(Box.createRigidArea(FixedDim.x50_y0));
    pnlMaskTypeCylinder.add(ltfCylinderHeight.getComponent());
    pnlMaskTypeCylinder.add(Box.createHorizontalGlue());
    // File
    pnlFile.setLayout(new BoxLayout(pnlFile, BoxLayout.X_AXIS));
    pnlFile.add(rbMaskTypeFile.getComponent());
    pnlFile.add(ftfMaskTypeFile.getRootPanel());
    pnlFile.add(Box.createRigidArea(FixedDim.x10_y0));
    // radius
    pnlRadius.setLayout(new BoxLayout(pnlRadius, BoxLayout.X_AXIS));
    pnlRadius.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlRadius.add(ltfInsideMaskRadius.getContainer());
    pnlRadius.add(Box.createRigidArea(FixedDim.x40_y0));
    pnlRadius.add(ltfOutsideMaskRadius.getContainer());
    pnlRadius.add(Box.createRigidArea(FixedDim.x40_y0));
    // MaskBlurStdDev
    pnlMaskBlurStdDev.setLayout(new BoxLayout(pnlMaskBlurStdDev, BoxLayout.X_AXIS));
    pnlMaskBlurStdDev.add(Box.createRigidArea(new Dimension(4, 0)));
    pnlMaskBlurStdDev.add(ltfMaskBlurStdDev.getComponent());
    pnlMaskBlurStdDev.add(Box.createHorizontalGlue());
    // CylinderOrientationX
    pnlCylinderOrientationX
      .setLayout(new BoxLayout(pnlCylinderOrientationX, BoxLayout.X_AXIS));
    pnlCylinderOrientationX.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlCylinderOrientationX.add(pnlCylinderOrientation);
    pnlCylinderOrientationX.add(Box.createRigidArea(FixedDim.x30_y0));
    // CylinderOrientation
    pnlCylinderOrientation
      .setLayout(new BoxLayout(pnlCylinderOrientation, BoxLayout.Y_AXIS));
    pnlCylinderOrientation.setBorder(BorderFactory.createEtchedBorder());
    pnlCylinderOrientation.add(pnlCylinderOrientationCheckBox);
    pnlCylinderOrientation.add(pnlCylinderRotation);
    pnlCylinderOrientation.add(Box.createRigidArea(FixedDim.x0_y5));
    // pnlCylinderOrientationCheckBox
    pnlCylinderOrientationCheckBox
      .setLayout(new BoxLayout(pnlCylinderOrientationCheckBox, BoxLayout.X_AXIS));
    pnlCylinderOrientationCheckBox.setAlignmentX(Box.RIGHT_ALIGNMENT);
    pnlCylinderOrientationCheckBox.add(cbCylinderOrientation.getComponent());
    pnlCylinderOrientationCheckBox.add(Box.createHorizontalGlue());
    // cylinder rotation
    pnlCylinderRotation.setLayout(new BoxLayout(pnlCylinderRotation, BoxLayout.X_AXIS));
    pnlCylinderRotation.add(Box.createRigidArea(FixedDim.x5_y0));
    pnlCylinderRotation.add(ltfZRotation.getContainer());
    pnlCylinderRotation.add(Box.createRigidArea(FixedDim.x40_y0));
    pnlCylinderRotation.add(ltfYRotation.getContainer());
    pnlCylinderRotation.add(Box.createRigidArea(FixedDim.x10_y0));
  }

  public SwingComponent getUIComponent() {
    return this;
  }

  public Component getComponent() {
    return pnlRoot;
  }

  /**
   * Called by the parent updateDisplay(). Enabled/disables fields. Calls
   * updateDisplay() in subordinate panels.
   */
  void updateDisplay() {
    ftfMaskTypeFile.setEnabled(rbMaskTypeFile.isSelected());
    ltfMaskBlurStdDev.setEnabled(!rbMaskTypeNone.isSelected());
    boolean cylinder = rbMaskTypeCylinder.isSelected();
    cbCylinderOrientation.setEnabled(cylinder);
    ltfCylinderHeight.setEnabled(cylinder);
    boolean cylinderOrientation = cylinder && cbCylinderOrientation.isSelected();
    ltfZRotation.setEnabled(cylinderOrientation);
    ltfYRotation.setEnabled(cylinderOrientation);
    boolean sphereCylinder = cylinder || rbMaskTypeSphere.isSelected();
    ltfInsideMaskRadius.setEnabled(sphereCylinder);
    ltfOutsideMaskRadius.setEnabled(sphereCylinder);
  }

  /**
   * Make the copied path relative to this dataset, preserving the location of
   * the files that the old dataset was using. So if the file was in the
   * original dataset directory, the new path will point (with a relative path
   * if possible) to the file in the original dataset directory. If the file
   * path is absolute, don't change it.
   * 
   * @param rootOfCopiedFilePaths
   */
  void convertCopiedPaths(final String origDatasetDir) {
    String propertyUserDir = manager.getPropertyUserDir();
    if (!ftfMaskTypeFile.isEmpty()) {
      ftfMaskTypeFile.setText(FilePath.getRerootedRelativePath(origDatasetDir,
        propertyUserDir, ftfMaskTypeFile.getText()));
    }
  }

  boolean isIncorrectPaths() {
    return !ftfMaskTypeFile.isEmpty() && !ftfMaskTypeFile.exists();
  }

  /**
   * If ftfMaskTypeVolume has an invalid path, call
   * ReferenceParent.fixIncorrectPath(FileTextField,boolean). Returns true to
   * keep fixing paths. Returns false to stop fixing paths.
   * 
   * @param choosePathEveryRow
   * @return
   */
  boolean fixIncorrectPaths(final boolean choosePathEveryRow) {
    if (isIncorrectPaths()) {
      return parent.fixIncorrectPath(ftfMaskTypeFile, choosePathEveryRow);
    }
    return true;
  }

  void getParameters(final PeetMetaData metaData) {
    metaData.setMaskModelPtsZRotation(ltfZRotation.getText());
    metaData.setMaskModelPtsYRotation(ltfYRotation.getText());
    metaData.setMaskTypeVolume(ftfMaskTypeFile.getText());
    metaData.setManualCylinderOrientation(cbCylinderOrientation.isSelected());
    metaData.setCylinderHeight(ltfCylinderHeight.getText());
    metaData.setMaskBlurStdDev(ltfMaskBlurStdDev.getText());
  }

  /**
   * Set parameters from metaData and then overwrite them with parameters from
   * MatlabParamFile. This allows inactive data to appear on the screen but
   * allows MatlabParamFile's active data to override active metaData. So if
   * the user changes the .prm file, the active data on the screen will be
   * correct.
   * 
   * @param metaData
   */
  void setParameters(final ConstPeetMetaData metaData) {
    ftfMaskTypeFile.setText(metaData.getMaskTypeVolume());
    ltfZRotation.setText(metaData.getMaskModelPtsZRotation());
    ltfYRotation.setText(metaData.getMaskModelPtsYRotation());
    cbCylinderOrientation.setSelected(metaData.isManualCylinderOrientation());
    ltfCylinderHeight.setText(metaData.getCylinderHeight());
    ltfMaskBlurStdDev.setText(metaData.getMaskBlurStdDev());
  }

  /**
   * Load data from MatlabParamFile. Load only active data after the meta data
   * has been loaded. Do not rely on whether fields are enabled to make
   * decisions in this function; updateDisplay may not have been run with data
   * in the screen. When looking at the settings of dialog fields, make sure
   * that they have already been loaded in THIS function. This is sometimes
   * the first data- loading function to be run.
   * 
   * @param matlabParamFile
   * @param paramatersOnly
   */
  void setParameters(final MatlabParam matlabParam) {
    String maskTypeValue = matlabParam.getMaskType();
    MatlabParam.MaskType maskType = MatlabParam.MaskType.getInstance(maskTypeValue);
    if (maskType == MatlabParam.MaskType.NONE) {
      rbMaskTypeNone.setSelected(true);
    }
    else {
      ltfMaskBlurStdDev.setText(matlabParam.getMaskBlurStdDev());
      if (maskType == MatlabParam.MaskType.VOLUME) {
        rbMaskTypeFile.setSelected(true);
        ftfMaskTypeFile.setText(maskTypeValue);
      }
      else if (maskType == MatlabParam.MaskType.SPHERE) {
        rbMaskTypeSphere.setSelected(true);
      }
      else if (maskType == MatlabParam.MaskType.CYLINDER) {
        rbMaskTypeCylinder.setSelected(true);
        ltfCylinderHeight.setText(matlabParam.getCylinderHeight());
      }
    }
    if (!matlabParam.isMaskModelPtsEmpty()) {
      cbCylinderOrientation.setSelected(true);
      ltfZRotation.setText(matlabParam.getMaskModelPtsZRotation());
      ltfYRotation.setText(matlabParam.getMaskModelPtsYRotation());
    }
    ltfInsideMaskRadius.setText(matlabParam.getInsideMaskRadius());
    ltfOutsideMaskRadius.setText(matlabParam.getOutsideMaskRadius());
  }

  boolean getParameters(final MatlabParam matlabParam, final boolean doValidation) {
    try {
      if (rbMaskTypeFile.isSelected()) {
        matlabParam.setMaskType(ftfMaskTypeFile.getText());
      }
      else {
        matlabParam.setMaskType(
          ((RadioButton.RadioButtonModel) bgMaskType.getSelection()).getEnumeratedType());
        if (rbMaskTypeCylinder.isSelected()) {
          matlabParam.setCylinderHeight(ltfCylinderHeight.getText(doValidation));
        }
      }
      if (!rbMaskTypeNone.isSelected()) {
        matlabParam.setMaskBlurStdDev(ltfMaskBlurStdDev.getText(doValidation));

      }
      if (cbCylinderOrientation.isEnabled() && cbCylinderOrientation.isSelected()) {
        matlabParam.setMaskModelPts(ltfZRotation.getText(doValidation),
          ltfYRotation.getText(doValidation));
      }
      else {
        matlabParam.clearMaskModelPts();
      }
      matlabParam.setInsideMaskRadius(ltfInsideMaskRadius.getText(doValidation));
      matlabParam.setOutsideMaskRadius(ltfOutsideMaskRadius.getText(doValidation));
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  void setDefaults() {
    rbMaskTypeNone.setSelected(true);
  }

  /**
   * Validate for running.
   * 
   * @return null if valid, error message if invalid.
   */
  String validateRun() {
    // Masking
    // volume
    if (rbMaskTypeFile.isSelected() && ftfMaskTypeFile.isEmpty()) {
      return "In " + MAST_TYPE_LABEL + ", " + MatlabParam.MaskType.VOLUME.getLabel()
        + " is required when " + MatlabParam.MaskType.VOLUME.getLabel() + " "
        + MAST_TYPE_LABEL + " is selected. ";
    }
    // validate radii
    if (((rbMaskTypeSphere.isSelected() || rbMaskTypeCylinder.isSelected()))
      && ltfInsideMaskRadius.isEnabled() && ltfInsideMaskRadius.isEmpty()
      && ltfOutsideMaskRadius.isEnabled() && ltfOutsideMaskRadius.isEmpty()) {
      return "In " + MAST_TYPE_LABEL + ", " + INSIDE_MASK_RADIUS_LABEL + " and/or "
        + OUTSIDE_MASK_RADIUS_LABEL + " are required when either "
        + MatlabParam.MaskType.SPHERE.getLabel() + " or "
        + MatlabParam.MaskType.CYLINDER.getLabel() + " is selected.";
    }
    // validate cylinder orientation
    EtomoNumber rotation = new EtomoNumber(EtomoNumber.Type.DOUBLE);
    // validate Z Rotation
    if (ltfZRotation.isEnabled()) {
      rotation.set(ltfZRotation.getText());
      String description = ltfZRotation.getLabel() + " field in " + pnlRoot.getName();
      if (!rotation.isValid()) {
        return description + " must be numeric - " + rotation.getInvalidReason() + ".";

      }
      if (!rotation.isNull()) {
        double fZRotation = Math.abs(rotation.getDouble());
        if (fZRotation < 0 || fZRotation > 90) {
          return "Valid values for " + description + " are 0 to 90.";
        }
      }
    }
    // validate Y Rotation
    if (ltfZRotation.isEnabled()) {
      rotation.set(ltfZRotation.getText());
      if (!rotation.isValid()) {
        return ltfYRotation.getLabel() + " field in " + pnlRoot.getName()
          + " must be numeric - " + rotation.getInvalidReason() + ".";
      }
    }
    return null;
  }

  private void action(final String actionCommand,
    final Run3dmodMenuOptions run3dmodMenuOptions) {
    if (actionCommand.equals(rbMaskTypeNone.getActionCommand())
      || actionCommand.equals(rbMaskTypeFile.getActionCommand())
      || actionCommand.equals(rbMaskTypeSphere.getActionCommand())
      || actionCommand.equals(rbMaskTypeCylinder.getActionCommand())
      || actionCommand.equals(cbCylinderOrientation.getActionCommand())) {
      parent.updateDisplay(false);
    }
  }

  private void setTooltips() {
    ReadOnlyAutodoc autodoc = null;
    try {
      autodoc =
        AutodocFactory.getInstance(manager, AutodocFactory.PEET_PRM, AxisID.ONLY, false);
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
    String tooltip = EtomoAutodoc.getTooltip(autodoc, "maskModelPts", false);
    ltfZRotation.setToolTipText(tooltip);
    ltfYRotation.setToolTipText(tooltip);
    ltfCylinderHeight.setToolTipText(
      EtomoAutodoc.getTooltip(autodoc, MatlabParam.CYLINDER_HEIGHT_KEY, false));
    ltfMaskBlurStdDev.setToolTipText(
      EtomoAutodoc.getTooltip(autodoc, MatlabParam.MASK_BLUR_STD_DEV_KEY, false));
    rbMaskTypeNone.setToolTipText("No reference masking");
    rbMaskTypeFile.setToolTipText("Mask the reference using a specified file");
    rbMaskTypeSphere.setToolTipText(
      "Mask the reference using inner and out spherical shells " + "of specified radii.");
    rbMaskTypeCylinder
      .setToolTipText("Mask the reference using inner and out cylindrical "
        + "shells of specified radii.");
    ftfMaskTypeFile
      .setToolTipText("The name of file containing the binary mask in MRC " + "format.");
    ltfInsideMaskRadius.setToolTipText("Inner radius of the mask region in pixels.");
    ltfOutsideMaskRadius
      .setToolTipText("Inner and outer radii of the mask region in pixels.");
    cbCylinderOrientation
      .setToolTipText("Manually specify cylindrical mask orientation.");
  }

  private static final class MaskingActionListener implements ActionListener {
    private final MaskingPanel maskingPanel;

    private MaskingActionListener(final MaskingPanel maskingPanel) {
      this.maskingPanel = maskingPanel;
    }

    public void actionPerformed(final ActionEvent event) {
      maskingPanel.action(event.getActionCommand(), null);
    }
  }
}
