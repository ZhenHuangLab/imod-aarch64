package etomo.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;

import etomo.BaseManager;
import etomo.comscript.ConstTiltParam;
import etomo.comscript.SirtsetupParam;
import etomo.comscript.TiltParam;
import etomo.storage.LogFile;
import etomo.storage.autodoc.AutodocFactory;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.type.AxisID;
import etomo.type.ConstMetaData;
import etomo.type.EtomoAutodoc;
import etomo.type.MetaData;
import etomo.type.PanelId;
import etomo.ui.DisplaySettings;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.SharedStrings;

/**
* <p>Description: Radial filter panel</p>
* 
* <p>Copyright: Copyright 2010 - 2016 by the Regents of the University of Colorado</p>
*
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$ $
* 
* <p> $Log$
* <p> Revision 1.2  2011/02/12 00:24:53  sueh
* <p> bug# 1437 Reformatting.
* <p>
* <p> Revision 1.1  2010/12/05 05:16:07  sueh
* <p> bug# 1416 Moved radial filter fields to RadialPanel so they can be reused
* <p> in SirtPanel.
* </p>
*/
final class RadialPanel implements ActionListener {
  private static final String RADIAL_FALLOFF_OLD_LABEL = " Falloff (1.4 * sigma): ";
  private static final String RADIAL_FALLOFF_IS_TRUE_SIGMA_LABEL = " Falloff (sigma): ";
  private static final String FAKE_SIRT_ITERATIONS_LABEL =
    "Use SIRT-like filter equivalent to: ";
  private static final String EXACT_FILTER_SIZE_LABEL =
    "Use 'exact filter' functions with 'object size' of: ";
  private static final String HAMMING_LIKE_FITLER_LABEL =
    "Hamming-like filter (as in tomo3d) starting from: ";
  private static final String RADIAL_BUTTON_LABEL = "Standard Gaussian,";
  private static final String RADIAL_MAX_LABEL = "cutoff: ";
  private static final String RADIAL_LABEL = RADIAL_BUTTON_LABEL + " " + RADIAL_MAX_LABEL;

  private final JPanel pnlRoot = new JPanel();
  private final LabeledTextField ltfRadialMax =
    new LabeledTextField(FieldType.FLOATING_POINT, RADIAL_LABEL);
  private final LabeledTextField ltfRadialFallOff =
    new LabeledTextField(FieldType.FLOATING_POINT, RADIAL_FALLOFF_OLD_LABEL);
  private final JPanel pnlHighFrequencyFiltering = new JPanel();

  private final TextField tfHammingLikeFilter;
  private final TextField tfExactFilterSize;
  private final PanelId panelId;
  private final BaseManager manager;
  private final AxisID axisID;
  private final DisplaySettings displaySettings;
  private final RadioButton rbDefault;
  private final TextField tfFakeSIRTiterations;
  private final RadioButton rbFakeSIRTiterations;
  private final RadioButton rbExactFilterSize;
  private final RadioButton rbRadial;
  private final RadioButton rbHammingLikeFilter;
  private final JPanel pnlForm;
  private final TomogramGenerationMethod method;
  private final JLabel lFakeSIRTiterations;

  private FilterType multifiltFilterType = null;
  private boolean debug = false;

  private RadialPanel(final BaseManager manager, final AxisID axisID,
    final PanelId panelId, final DisplaySettings displaySettings,
    final TomogramGenerationMethod method) {
    this.manager = manager;
    this.axisID = axisID;
    this.panelId = panelId;
    this.displaySettings = displaySettings;
    this.method = method;
    if (panelId == PanelId.TILT) {
      ButtonGroup bgForm = new ButtonGroup();
      rbDefault = new RadioButton("Standard ramp function", bgForm);
      rbFakeSIRTiterations = new RadioButton(FAKE_SIRT_ITERATIONS_LABEL, bgForm);
      tfFakeSIRTiterations =
        new TextField(FieldType.INTEGER, FAKE_SIRT_ITERATIONS_LABEL, null);
      rbExactFilterSize = new RadioButton(EXACT_FILTER_SIZE_LABEL, bgForm);
      tfExactFilterSize = new TextField(FieldType.INTEGER, EXACT_FILTER_SIZE_LABEL, null);
      ButtonGroup bgHighFrequencyFiltering = new ButtonGroup();
      rbRadial = new RadioButton(RADIAL_BUTTON_LABEL, bgHighFrequencyFiltering);
      rbRadial.setName(RADIAL_LABEL);
      ltfRadialMax.setLabel(RADIAL_MAX_LABEL);
      ltfRadialMax.setName(RADIAL_LABEL);
      rbHammingLikeFilter = new RadioButton(
        "Hamming-like filter (as in tomo3d) starting from: ", bgHighFrequencyFiltering);
      tfHammingLikeFilter = new TextField(FieldType.FLOATING_POINT,
        "Hamming-like filter (as in tomo3d) starting from: ", null);
      pnlForm = new JPanel();
      lFakeSIRTiterations = new JLabel(" iterations");
    }
    else {
      rbDefault = null;
      rbFakeSIRTiterations = null;
      tfFakeSIRTiterations = null;
      rbExactFilterSize = null;
      tfExactFilterSize = null;
      rbRadial = null;
      rbHammingLikeFilter = null;
      tfHammingLikeFilter = null;
      pnlForm = null;
      lFakeSIRTiterations = null;
    }
  }

  static RadialPanel getInstance(final BaseManager manager, final AxisID axisID,
    final PanelId panelId, final DisplaySettings displaySettings,
    final TomogramGenerationMethod method) {
    RadialPanel instance =
      new RadialPanel(manager, axisID, panelId, displaySettings, method);
    instance.createPanel();
    instance.addListeners();
    instance.setTooltips();
    return instance;
  }

  private void createPanel() {
    // init
    if (panelId == PanelId.TILT) {
      rbDefault.setSelected(true);
    }
    // panel
    JPanel pnlRadial = new JPanel();
    JPanel pnlFakeSIRTiterations = null;
    JPanel pnlDefault = null;
    JPanel pnlExactFilterSize = null;
    JPanel pnlHammingLikeFilter = null;
    if (panelId == PanelId.TILT) {
      pnlFakeSIRTiterations = new JPanel();
      pnlDefault = new JPanel();
      pnlExactFilterSize = new JPanel();
      pnlHammingLikeFilter = new JPanel();
    }
    // Root
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
    pnlRoot.setBorder(new EtchedBorder("Radial Filtering").getBorder());
    if (panelId == PanelId.TILT) {
      pnlRoot.add(pnlForm);
    }
    pnlRoot.add(pnlHighFrequencyFiltering);
    if (panelId == PanelId.TILT) {
      // Form
      pnlForm.setLayout(new BoxLayout(pnlForm, BoxLayout.Y_AXIS));
      pnlForm.setBorder(new EtchedBorder("Form of Radial Filter").getBorder());
      pnlForm.add(pnlDefault);
      pnlForm.add(pnlFakeSIRTiterations);
      pnlForm.add(pnlExactFilterSize);
      // Default
      pnlDefault.setLayout(new BoxLayout(pnlDefault, BoxLayout.X_AXIS));
      pnlDefault.add(rbDefault.getComponent());
      pnlDefault.add(Box.createHorizontalGlue());
      // FakeSIRTiterations
      pnlFakeSIRTiterations
        .setLayout(new BoxLayout(pnlFakeSIRTiterations, BoxLayout.X_AXIS));
      pnlFakeSIRTiterations.add(rbFakeSIRTiterations.getComponent());
      pnlFakeSIRTiterations.add(tfFakeSIRTiterations.getComponent());
      pnlFakeSIRTiterations.add(lFakeSIRTiterations);
      pnlFakeSIRTiterations.add(Box.createHorizontalGlue());
      // ExactFilterSize
      pnlExactFilterSize.setLayout(new BoxLayout(pnlExactFilterSize, BoxLayout.X_AXIS));
      pnlExactFilterSize.add(rbExactFilterSize.getComponent());
      pnlExactFilterSize.add(tfExactFilterSize.getComponent());
      pnlExactFilterSize.add(Box.createHorizontalGlue());
    }
    // HighFrequencyFiltering
    pnlHighFrequencyFiltering
      .setLayout(new BoxLayout(pnlHighFrequencyFiltering, BoxLayout.Y_AXIS));
    pnlHighFrequencyFiltering
      .setBorder(new EtchedBorder("High-Frequency Filtering").getBorder());
    pnlHighFrequencyFiltering.add(pnlRadial);
    if (panelId == PanelId.TILT) {
      pnlHighFrequencyFiltering.add(pnlHammingLikeFilter);
    }
    // Radial
    pnlRadial.setLayout(new BoxLayout(pnlRadial, BoxLayout.X_AXIS));
    if (panelId == PanelId.TILT) {
      pnlRadial.add(rbRadial.getComponent());
    }
    pnlRadial.add(ltfRadialMax.getContainer());
    pnlRadial.add(ltfRadialFallOff.getContainer());
    pnlRadial.add(Box.createHorizontalGlue());
    // HammingLikeFilter
    if (panelId == PanelId.TILT) {
      pnlHammingLikeFilter
        .setLayout(new BoxLayout(pnlHammingLikeFilter, BoxLayout.X_AXIS));
      pnlHammingLikeFilter.add(rbHammingLikeFilter.getComponent());
      pnlHammingLikeFilter.add(tfHammingLikeFilter.getComponent());
      pnlHammingLikeFilter.add(Box.createHorizontalGlue());
    }
    //
    updateDisplay(true);
  }

  private void addListeners() {
    if (panelId == PanelId.TILT) {
      rbDefault.addActionListener(this);
      rbFakeSIRTiterations.addActionListener(this);
      rbExactFilterSize.addActionListener(this);
      rbRadial.addActionListener(this);
      rbHammingLikeFilter.addActionListener(this);
    }
  }

  /**
   * Disable filter types on this panel if they are selected in the multifilt panel.
   * @param multifiltFilterType - MultifiltPanel
   */
  void setMultifiltFilterType(final FilterType multifiltFilterType) {
    this.multifiltFilterType = multifiltFilterType;
    updateDisplay(false);
  }

  void setVisible(final boolean visible) {
    pnlRoot.setVisible(visible);
  }

  Component getRoot() {
    return pnlRoot;
  }

  public void actionPerformed(final ActionEvent event) {
    updateDisplay(false);
  }

  void setEditable(boolean editable) {
    ltfRadialMax.setEditable(editable);
    ltfRadialFallOff.setEditable(editable);
    if (panelId == PanelId.TILT) {
      rbDefault.setEditable(editable);
      rbFakeSIRTiterations.setEditable(editable);
      tfFakeSIRTiterations.setEditable(editable);
      rbExactFilterSize.setEditable(editable);
      tfExactFilterSize.setEditable(editable);
      rbRadial.setEditable(editable);
      rbHammingLikeFilter.setEditable(editable);
      tfHammingLikeFilter.setEditable(editable);
    }
  }

  final void msgMethodChanged() {
    updateDisplay(false);
  }

  /**
   * @param init - true when called from getInstance or the constructor - used to avoid querying other classes before they are complete.
   */
  private void updateDisplay(final boolean init) {
    // multifiltFilterType disables the same type of filter in this panel when they are
    // displayed together.
    boolean enableHighFrequencyFilter =
      init || !method.isMultifilt() || (multifiltFilterType != null
        ? !multifiltFilterType.isHighFrequencyFilter() : true);
    pnlHighFrequencyFiltering.setEnabled(enableHighFrequencyFilter);
    // radial
    boolean enableRadial = enableHighFrequencyFilter;
    if (panelId == PanelId.TILT) {
      rbRadial.setEnabled(enableHighFrequencyFilter);
      enableRadial = enableRadial && rbRadial.isSelected();
    }
    ltfRadialMax.setEnabled(enableRadial);
    ltfRadialFallOff.setEnabled(enableRadial);
    if (panelId == PanelId.TILT) {
      // hammingLikeFilter
      rbHammingLikeFilter.setEnabled(enableHighFrequencyFilter);
      tfHammingLikeFilter
        .setEnabled(enableHighFrequencyFilter && rbHammingLikeFilter.isSelected());
      //
      boolean enableRadialFilter = init || !method.isMultifilt()
        || (multifiltFilterType != null ? !multifiltFilterType.isRadialFilter() : true);
      pnlForm.setEnabled(enableRadialFilter);
      // Default
      rbDefault.setEnabled(enableRadialFilter);
      // FakeSIRTiterations
      rbFakeSIRTiterations.setEnabled(enableRadialFilter);
      tfFakeSIRTiterations
        .setEnabled(enableRadialFilter && rbFakeSIRTiterations.isSelected());
      lFakeSIRTiterations.setEnabled(enableRadialFilter);
      // ExactFilterSize
      rbExactFilterSize.setEnabled(enableRadialFilter);
      tfExactFilterSize.setEnabled(enableRadialFilter && rbExactFilterSize.isSelected());
    }
  }

  void updateAdvanced(final boolean advanced) {
    if (panelId == PanelId.TILT) {
      boolean multifilt = method != null ? method.isMultifilt() : false;
      rbExactFilterSize.setVisible(advanced || multifilt);
      tfExactFilterSize.setVisible(advanced || multifilt);
    }
  }

  void getParameters(final MetaData metaData) {
    metaData.setRadialRadius(panelId, axisID, ltfRadialMax.getText());
    metaData.setRadialSigma(panelId, axisID, ltfRadialFallOff.getText());
    if (panelId == PanelId.TILT) {
      metaData.setHammingLikeFilter(panelId, axisID, tfHammingLikeFilter.getText());
      metaData.setFakeSIRTiterations(panelId, axisID, tfFakeSIRTiterations.getText());
      metaData.setExactFilterSize(panelId, axisID, tfExactFilterSize.getText());
    }
  }

  void setParameters(final ConstMetaData metaData) {
    ltfRadialMax.setText(metaData.getRadialRadius(panelId, axisID));
    ltfRadialFallOff.setText(metaData.getRadialSigma(panelId, axisID));
    if (panelId == PanelId.TILT) {
      tfHammingLikeFilter.setText(metaData.getHammingLikeFilter(panelId, axisID));
      tfFakeSIRTiterations.setText(metaData.getFakeSIRTiterations(panelId, axisID));
      tfExactFilterSize.setText(metaData.getExactFilterSize(panelId, axisID));
    }
    updateAdvanced(displaySettings.isAdvanced());
    updateDisplay(false);
  }

  void setParameters(final ConstTiltParam tiltParam) {
    if (tiltParam.hasRadialWeightingFunction()) {
      ltfRadialMax.setNonEmptyText(tiltParam.getRadialBandwidth());
      ltfRadialFallOff.setNonEmptyText(tiltParam.getRadialFalloff());
    }
    if (tiltParam.isFalloffIsTrueSigma()) {
      ltfRadialFallOff.setLabel(RADIAL_FALLOFF_IS_TRUE_SIGMA_LABEL);
    }
    else {
      ltfRadialFallOff.setLabel(RADIAL_FALLOFF_OLD_LABEL);
    }
    if (panelId == PanelId.TILT) {
      if (tiltParam.isHammingLikeFilter()) {
        rbHammingLikeFilter.setSelected(true);
        tfHammingLikeFilter.setText(tiltParam.getHammingLikeFilter());
      }
      else {
        // Default
        rbRadial.setSelected(true);
      }
      if (tiltParam.isFakeSIRTiterations()) {
        rbFakeSIRTiterations.setSelected(true);
        tfFakeSIRTiterations.setText(tiltParam.getFakeSIRTiterations());
      }
      if (tiltParam.isExactFilterSize()) {
        rbExactFilterSize.setSelected(true);
        tfExactFilterSize.setText(tiltParam.getExactFilterSize());
      }
    }
    updateAdvanced(displaySettings.isAdvanced());
    updateDisplay(false);
  }

  boolean getParameters(final SirtsetupParam param, final boolean doValidation) {
    try {
      if (ltfRadialMax.isEditable() && ltfRadialMax.isEnabled()) {
        param.setRadiusAndSigma(0, ltfRadialMax.getText(doValidation));
      }
      if (ltfRadialFallOff.isEnabled()) {
        param.setRadiusAndSigma(1, ltfRadialFallOff.getText(doValidation));
      }
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  void setParameters(final SirtsetupParam param) {
    ltfRadialMax.setText(param.getRadiusAndSigma(0));
    ltfRadialFallOff.setText(param.getRadiusAndSigma(1));
    if (param.isFalloffIsTrueSigma()) {
      ltfRadialFallOff.setLabel(RADIAL_FALLOFF_IS_TRUE_SIGMA_LABEL);
    }
    else {
      ltfRadialFallOff.setLabel(RADIAL_FALLOFF_OLD_LABEL);
    }
  }

  public void setDebug(final boolean debug) {
    this.debug = debug;
  }

  boolean getParameters(final TiltParam tiltParam, final boolean doValidation)
    throws NumberFormatException {
    // Bug# 2098 comments 6 and 7. Ignore whether the panels these fields are on are
    // disabled. They should be saved to tilt.com as usual.
    try {
      if (((panelId == PanelId.TILT && rbRadial.isSelected()) || panelId != PanelId.TILT)
        && (!ltfRadialMax.isEmpty() || !ltfRadialFallOff.isEmpty())) {
        tiltParam.setRadialBandwidth(ltfRadialMax.getText(doValidation));
        tiltParam.setRadialFalloff(ltfRadialFallOff.getText(doValidation));
      }
      else {
        tiltParam.resetRadialFilter();
      }
      if (panelId == PanelId.TILT) {
        if (rbHammingLikeFilter.isSelected()) {
          tiltParam.setHammingLikeFilter(tfHammingLikeFilter.getText(doValidation));
        }
        else {
          tiltParam.resetHammingLikeFilter();
        }
        if (rbFakeSIRTiterations.isSelected()) {
          tiltParam.setFakeSIRTiterations(tfFakeSIRTiterations.getText(doValidation));
        }
        else {
          tiltParam.resetFakeSIRTiterations();
        }
        if (rbExactFilterSize.isSelected()) {
          tiltParam.setExactFilterSize(tfExactFilterSize.getText(doValidation));
        }
        else {
          tiltParam.resetExactFilterSize();
        }
      }
      else {
        tiltParam.resetExactFilterSize();
      }
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  private void setTooltips() {
    if (panelId == PanelId.SIRTSETUP) {
      ReadOnlyAutodoc autodoc = null;
      try {
        autodoc =
          AutodocFactory.getInstance(manager, AutodocFactory.SIRTSETUP, axisID, false);
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
      String tooltip =
        EtomoAutodoc.getTooltip(autodoc, SirtsetupParam.RADIUS_AND_SIGMA_KEY);
      ltfRadialMax.setToolTipText(tooltip);
      ltfRadialFallOff.setToolTipText(tooltip);
    }
    else {
      ltfRadialMax.setToolTipText(
        "The spatial frequency at which to switch from the R-weighted radial "
          + "filter to a Gaussian falloff.  Frequency is in cycles/pixel and "
          + "ranges from 0-0.5.  Both a cutoff and a falloff must be entered.");
      ltfRadialFallOff.setToolTipText(
        "The sigma value of a Gaussian which determines how fast the radial "
          + "filter falls off at spatial frequencies above the cutoff frequency."
          + "  Frequency is in cycles/pixel and ranges from 0-0.5.  Both a "
          + "cutoff and a falloff must be entered ");
      if (panelId == PanelId.TILT) {
        rbDefault.setToolTipText(
          "Use the standard ramp filter linearly proportional to spatial frequency until "
            + "the high-frequency filter starts");
        rbRadial.setToolTipText(SharedStrings.GAUSSIAN_FILTER_RADIO_BUTTON_TOOLTIP);
        rbHammingLikeFilter
          .setToolTipText(SharedStrings.HAMMING_LIKE_FILTER_RADIO_BUTTON_TOOLTIP);
        tfHammingLikeFilter.setToolTipText(
          "Spatial frequency at which to start multiplying by an approximate Hamming "
            + "window that attenuates by 0.06 at 0.5 cycles/pixel.  Frequency ranges "
            + "from 0 to 0.5.");
        rbFakeSIRTiterations
          .setToolTipText(SharedStrings.SIRT_LIKE_FILTER_RADIO_BUTTON_TOOLTIP);
        tfFakeSIRTiterations.setToolTipText(
          "Number of iterations of SIRT that the filtered back-projection should match "
            + "approximately");
        rbExactFilterSize.setToolTipText(SharedStrings.EXACT_FILTER_RADIO_BUTTON_TOOLTIP);
        tfExactFilterSize.setToolTipText(
          "A size in unbinned pixels that determines at what frequency the filter "
            + "functions reach a plateau");
      }
    }
  }
}