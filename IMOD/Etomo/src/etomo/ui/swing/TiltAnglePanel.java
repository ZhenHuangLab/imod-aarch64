package etomo.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import etomo.ApplicationManager;
import etomo.storage.DirectiveFileCollection;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.Extension;
import etomo.type.FileType;
import etomo.type.ImageFileMetaData;
import etomo.type.ImageFilenameStyle;
import etomo.type.MetaData;
import etomo.type.TiltAngleSpec;
import etomo.type.TiltAngleType;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;

/**
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2002</p>
 *
 * <p>Organization: Boulder Laboratory for 3D Fine Structure,
 * University of Colorado</p>
 *
 * @author $Author$
 *
 * @version $Revision$
 *
 * <p> $Log$
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.1  2007/12/26 22:36:25  sueh
 * <p> bug# 1052 Turned TiltAnglePanel into an extremely thin GUI.  Moved decisions
 * <p> and knowledge to TiltAnglePanelExpert.
 * <p>
 * <p> Revision 3.6  2007/08/08 15:04:25  sueh
 * <p> bug# 834 Using UserConfiguration to initialize fields.
 * <p>
 * <p> Revision 3.5  2007/03/07 21:15:28  sueh
 * <p> bug# 981 Turned RadioButton into a wrapper rather then a child of JRadioButton,
 * <p> because it is getting more complicated.
 * <p>
 * <p> Revision 3.4  2007/02/09 00:53:54  sueh
 * <p> bug# 962 Made TooltipFormatter a singleton and moved its use to low-level ui
 * <p> classes.
 * <p>
 * <p> Revision 3.3  2006/01/03 23:58:37  sueh
 * <p> bug# 675 Converted JRadioButton's toRadioButton.
 * <p>
 * <p> Revision 3.2  2004/08/03 18:51:27  sueh
 * <p> bug# 519 removing rangeMax
 * <p>
 * <p> Revision 3.1  2003/11/10 18:51:43  sueh
 * <p> bug332 getErrorMessage(): New function validate panel and
 * <p> returns (rather then displaying) error message.
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:01  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 2.2  2003/10/09 20:27:43  sueh
 * <p> bug264
 * <p> UI Changes
 * <p>
 * <p> Revision 2.1  2003/05/07 23:38:36  rickg
 * <p> Added tooltips to object as well as panel
 * <p>
 * <p> Revision 2.0  2003/01/24 20:30:31  rickg
 * <p> Single window merge to main branch
 * <p>
 * <p> Revision 1.2.2.1  2003/01/24 18:43:37  rickg
 * <p> Single window GUI layout initial revision
 * <p>
 * <p> Revision 1.2  2003/01/06 20:17:40  rickg
 * <p> Changed edit boxed to LabeledTextFields
 * <p> Added an action listener and enable funcsions to
 * <p> enable the specify dialogs only when selected
 * <p>
 * <p> Revision 1.1  2002/09/09 22:57:02  rickg
 * <p> Initial CVS entry, basic functionality not including combining
 * <p> </p>
 */
final class TiltAnglePanel {
  public static final String rcsid =
    "$Id$";

  static final String EXISTING_RAWTILT_FILE = "Tilt angles in existing rawtlt file";

  private final JPanel pnlSource = new JPanel();
  private final EtomoButtonGroup bgSource = new EtomoButtonGroup();
  private final RadioButton rbExtract =
    new RadioButton("Extract tilt angles from data", bgSource);
  private final JPanel pnlAngle = new JPanel();
  private final RadioEbutton rbSpecify =
    RadioEbutton.getInstance("Specify the starting angle and step (degrees)", bgSource);
  private final LabeledTextField ltfMin =
    new LabeledTextField(FieldType.FLOATING_POINT, "Starting angle:");
  private final LabeledTextField ltfStep =
    new LabeledTextField(FieldType.FLOATING_POINT, "Increment:");
  private final RadioEbutton rbFile =
    RadioEbutton.getInstance(EXISTING_RAWTILT_FILE, bgSource);
  private final JLabel lExcludeViewsMsg = new JLabel();
  private final TiltAnglePanelExpert expert;
  private final AxisID axisID;
  private final ApplicationManager manager;

  private SetupDialog parent = null;

  TiltAnglePanel(final ApplicationManager manager, final TiltAnglePanelExpert expert,
    final AxisID axisID) {
    this.expert = expert;
    this.axisID = axisID;
    this.manager = manager;
    JPanel pnlFile = new JPanel();
    JPanel pnlExtract = new JPanel();
    JPanel pnlSpecify = new JPanel();
    pnlAngle.setLayout(new BoxLayout(pnlAngle, BoxLayout.X_AXIS));
    pnlAngle.add(ltfMin.getComponent());
    pnlAngle.add(Box.createRigidArea(FixedDim.x10_y0));
    pnlAngle.add(ltfStep.getComponent());
    pnlAngle.add(Box.createHorizontalGlue());

    pnlSource.setLayout(new BoxLayout(pnlSource, BoxLayout.Y_AXIS));
    pnlSource.add(lExcludeViewsMsg);
    pnlSource.add(pnlExtract);
    pnlSource.add(pnlSpecify);
    pnlSource.add(pnlAngle);

    pnlSource.add(pnlFile);
    pnlSource.add(pnlFile);
    // Extract
    pnlExtract.setLayout(new BoxLayout(pnlExtract, BoxLayout.X_AXIS));
    pnlExtract.add(rbExtract.getComponent());
    pnlExtract.add(Box.createHorizontalGlue());
    // Specify
    pnlSpecify.setLayout(new BoxLayout(pnlSpecify, BoxLayout.X_AXIS));
    pnlSpecify.add(rbSpecify.getComponent());
    pnlSpecify.add(Box.createHorizontalGlue());
    // File
    pnlFile.setLayout(new BoxLayout(pnlFile, BoxLayout.X_AXIS));
    pnlFile.add(rbFile.getComponent());
    pnlFile.add(Box.createHorizontalGlue());

    TiltAngleDialogListener tiltAlignRadioButtonListener =
      new TiltAngleDialogListener(expert);
    rbExtract.addActionListener(tiltAlignRadioButtonListener);
    rbFile.addActionListener(tiltAlignRadioButtonListener);
    rbSpecify.addActionListener(tiltAlignRadioButtonListener);
  }

  void msgExcludeViewsSucceeded() {
    if (rbSpecify.isSelected()) {
      rbFile.setSelected(true);
    }
    updateDisplay();
  }

  void setParent(final SetupDialog parent) {
    this.parent = parent;
  }

  void updateDisplay() {
    boolean specify = rbSpecify.isSelected() && rbSpecify.isEnabled();
    ltfMin.setEnabled(specify);
    ltfStep.setEnabled(specify);
    String datasetName = parent.getDatasetName();
    if (datasetName == null) {
      rbSpecify.disableWarning();
      rbFile.setLabel(EXISTING_RAWTILT_FILE);
      rbFile.disableWarning();
    }
    else if (parent != null) {
      AxisType axisType = parent.getAxisType();
      AxisID curAxisID = axisID;
      if (curAxisID != AxisID.SECOND) {
        if (axisType == AxisType.DUAL_AXIS) {
          curAxisID = AxisID.FIRST;
        }
        else {
          curAxisID = AxisID.ONLY;
        }
      }
      // Metadata hasn't been created so derive the file name.
      MetaData metaData = manager.getMetaData();
      Extension origImageStackExt;
      ImageFilenameStyle imageFilenameStyle;
      Extension rawStackExtension;
      if (metaData != null) {
        imageFilenameStyle = metaData.getImageFilenameStyle();
        rawStackExtension = metaData.getRawImageStackExtension();
      }
      else {
        ImageFileMetaData imageFileMetaData = ImageFileMetaData.getTempInstance();
        imageFilenameStyle = imageFileMetaData.getImageFilenameStyle();
        rawStackExtension = imageFileMetaData.getDefaultRawImageStackExtension();
      }
      boolean exists =
        new File(parent.getDirectory(), FileType.RAW_TILT_ANGLES.deriveFileName(
          datasetName, axisType, curAxisID, imageFilenameStyle, rawStackExtension))
            .exists();
      if (exists || parent.isRemoveExcludeViewsMsg(axisID)) {
        rbSpecify.enableWarning(true);
      }
      else {
        rbSpecify.disableWarning();
      }
      if (exists) {
        rbFile.setLabel(EXISTING_RAWTILT_FILE + " (File was found)");
        rbFile.disableWarning();
      }
      else {
        rbFile.setLabel(EXISTING_RAWTILT_FILE + " (File was not found)");
        rbFile.enableWarning(true);
      }
    }
  }

  void checkpoint() {
    rbExtract.checkpoint();
    rbFile.checkpoint();
    rbSpecify.checkpoint();
  }

  void updateTemplateValues(final DirectiveFileCollection directiveFileCollection,
    final AxisID axisID) {
    if (directiveFileCollection.containsTiltAngleSpec(axisID)) {
      TiltAngleSpec tiltAngleSpec = new TiltAngleSpec();
      directiveFileCollection.getTiltAngleFields(axisID, tiltAngleSpec, false);
      if (tiltAngleSpec.getType() == TiltAngleType.EXTRACT) {
        rbExtract.setSelected(true);
      }
      else if (tiltAngleSpec.getType() == TiltAngleType.FILE) {
        rbFile.setSelected(true);
      }
    }
    else {
      if (rbExtract.isCheckpointValue()) {
        rbExtract.setSelected(true);
      }
      else if (rbFile.isCheckpointValue()) {
        rbFile.setSelected(true);
      }
      else if (rbSpecify.isCheckpointValue()) {
        rbSpecify.setSelected(true);
      }
      else {
        rbExtract.setSelected(false);
        rbFile.setSelected(false);
        rbSpecify.setSelected(false);
      }
    }
  }

  Component getComponent() {
    return pnlSource;
  }

  void setFile(final boolean input) {
    rbFile.setSelected(input);
  }

  void setExtract(final boolean input) {
    rbExtract.setSelected(input);
  }

  void setSpecify(final boolean input) {
    rbSpecify.setSelected(input);
  }

  void setMin(final double input) {
    ltfMin.setText(input);
  }

  void setStep(final double input) {
    ltfStep.setText(input);
  }

  void setMinEnabled(final boolean enable) {
    ltfMin.setEnabled(enable);
  }

  void setStepEnabled(final boolean enable) {
    ltfStep.setEnabled(enable);
  }

  boolean isExtractSelected() {
    return rbExtract.isSelected();
  }

  boolean isSpecifySelected() {
    return rbSpecify.isSelected();
  }

  boolean isFileSelected() {
    return rbFile.isSelected();
  }

  String getMin() {
    return ltfMin.getText();
  }

  String getMin(final boolean doValidation) throws FieldValidationFailedException {
    return ltfMin.getText(doValidation);
  }

  String getStep() {
    return ltfStep.getText();
  }

  String getStep(final boolean doValidation) throws FieldValidationFailedException {
    return ltfStep.getText(doValidation);
  }

  void setSourceEnabled(final boolean enable) {
    pnlSource.setEnabled(enable);
  }

  void setAngleEnabled(final boolean enable) {
    pnlAngle.setEnabled(enable);
  }

  void setExtractEnabled(final boolean enable) {
    rbExtract.setEnabled(enable);
  }

  void setFileEnabled(final boolean enable) {
    rbFile.setEnabled(enable);
  }

  void setSpecifyEnabled(final boolean enable) {
    rbSpecify.setEnabled(enable);
  }

  void setSourceTooltip(final String tooltip) {
    pnlSource.setToolTipText(TooltipFormatter.INSTANCE.format(tooltip));
  }

  void setExtractTooltip(final String tooltip) {
    rbExtract.setToolTipText(tooltip);
  }

  void setSpecifyTooltip(final String tooltip) {
    rbSpecify.setToolTipText(tooltip);
  }

  void setMinTooltip(final String tooltip) {
    ltfMin.setToolTipText(tooltip);
  }

  void setStepTooltip(final String tooltip) {
    ltfStep.setToolTipText(tooltip);
  }

  void setFileTooltip(final String tooltip) {
    rbFile.setToolTipText(tooltip);
  }

  String getSpecify() {
    return rbSpecify.getLabel();
  }
}

final class TiltAngleDialogListener implements ActionListener {

  private final TiltAnglePanelExpert adaptee;

  TiltAngleDialogListener(final TiltAnglePanelExpert adaptee) {
    this.adaptee = adaptee;
  }

  public void actionPerformed(final ActionEvent event) {
    adaptee.setRadioButtonState(event);
  }
}
