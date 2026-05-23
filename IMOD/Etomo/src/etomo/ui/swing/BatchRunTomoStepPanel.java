package etomo.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;

import etomo.BaseManager;
import etomo.comscript.BatchruntomoParam;
import etomo.type.AxisID;
import etomo.type.BatchRunTomoMetaData;
import etomo.type.BatchRunTomoStatus;
import etomo.type.EndingStep;
import etomo.type.StartingStep;
import etomo.type.Status;
import etomo.type.StatusChangeEvent;
import etomo.type.StatusChangeListener;

/**
 * <p>Description: Batchruntomo StartingStep and EndingStep. </p>
 * <p/>
 * <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class BatchRunTomoStepPanel implements ActionListener, StatusChangeListener {
  static final int STEP_PAIRS = 5;
  private final JPanel pnlRoot = new JPanel();
  private final CheckBox cbEndingStep = new CheckBox("Stop after");
  private final ButtonGroup bgEndingStep = new ButtonGroup();
  private final RadioButton rbEndingStep[] = new RadioButton[STEP_PAIRS];
  private final CheckBox cbStartingStep = new CheckBox("Start from");
  private final ButtonGroup bgStartingStep = new ButtonGroup();
  private final RadioButton rbStartingStep[] = new RadioButton[STEP_PAIRS];
  private final CheckBox cbEnableStartingStep =
    new CheckBox("Enable starting from any step");

  private final BaseManager manager;
  private final AxisID axisID;
  private final BatchRunTomoTable table;

  private BatchRunTomoStatus status = BatchRunTomoStatus.DEFAULT;

  private BatchRunTomoStepPanel(final BaseManager manager, final AxisID axisID,
    final BatchRunTomoTable table) {
    this.manager = manager;
    this.axisID = axisID;
    this.table = table;
  }

  static BatchRunTomoStepPanel getInstance(final BaseManager manager, final AxisID axisID,
    final BatchRunTomoTable table) {
    BatchRunTomoStepPanel instance = new BatchRunTomoStepPanel(manager, axisID, table);
    instance.createPanel();
    instance.setTooltips();
    instance.addListeners();
    return instance;
  }

  private void createPanel() {
    // panels
    JPanel pnlEndingStep = new JPanel();
    JPanel pnlStartingStep = new JPanel();
    JPanel pnlStep = new JPanel();
    // Root
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
    pnlRoot.setBorder(new EtchedBorder("Subset of steps to run").getBorder());
    pnlRoot.add(pnlStep);
    pnlRoot.add(cbEnableStartingStep.getComponent());
    // Step
    pnlStep.setLayout(new BoxLayout(pnlStep, BoxLayout.X_AXIS));
    pnlStep.add(pnlEndingStep);
    pnlStep.add(pnlStartingStep);
    // EndingStep
    pnlEndingStep.setLayout(new BoxLayout(pnlEndingStep, BoxLayout.Y_AXIS));
    pnlEndingStep.add(cbEndingStep.getComponent());
    // StartingStep
    pnlStartingStep.setLayout(new BoxLayout(pnlStartingStep, BoxLayout.Y_AXIS));
    pnlStartingStep.add(cbStartingStep.getComponent());
    // Step
    EndingStep endingStep;
    StartingStep startingStep;
    for (int i = 0; i < STEP_PAIRS; i++) {
      endingStep = EndingStep.getInstance(i);
      if (endingStep != null) {
        // init
        rbEndingStep[i] =
          new RadioButton(endingStep.getLabel(), endingStep, bgEndingStep);
        if (endingStep.isDefault()) {
          rbEndingStep[i].setSelected(true);
        }
        rbEndingStep[i].setToolTipText(endingStep.getTooltip());
      }
      startingStep = StartingStep.getInstance(i);
      if (startingStep != null) {
        rbStartingStep[i] =
          new RadioButton(startingStep.getLabel(), startingStep, bgStartingStep);
        if (startingStep.isDefault()) {
          rbStartingStep[i].setSelected(true);
        }
        rbStartingStep[i].setToolTipText(startingStep.getTooltip());
        // EndingStep
        pnlEndingStep.add(rbEndingStep[i].getComponent());
        // StartingStep
        pnlStartingStep.add(rbStartingStep[i].getComponent());
      }
    }
    // align
    UIUtilities.alignComponentsX(pnlRoot, Component.LEFT_ALIGNMENT);
    // update
    updateDisplay();
  }

  private void addListeners() {
    cbEndingStep.addActionListener(this);
    cbStartingStep.addActionListener(this);
    for (int i = 0; i < STEP_PAIRS; i++) {
      rbEndingStep[i].addActionListener(this);
      rbStartingStep[i].addActionListener(this);
    }
    cbEnableStartingStep.addActionListener(this);
    table.addStatusChangeListenerToRowList(this);
    table.addStatusChangeListenerToRows(this);
  }

  Component getComponent() {
    return pnlRoot;
  }

  public void actionPerformed(ActionEvent e) {
    updateDisplay();
  }

  public void statusChanged(final Status status) {
    if (status instanceof BatchRunTomoStatus) {
      this.status = (BatchRunTomoStatus) status;
      boolean editable = status == null || status == BatchRunTomoStatus.OPEN
        || status == BatchRunTomoStatus.STOPPED;
      cbStartingStep.setEditable(editable);
      for (int i = 0; i < rbStartingStep.length; i++) {
        rbStartingStep[i].setEditable(editable);
      }
      cbEndingStep.setEditable(editable);
      for (int i = 0; i < rbStartingStep.length; i++) {
        rbEndingStep[i].setEditable(editable);
      }
      cbEnableStartingStep.setEditable(editable);
    }
    updateDisplay();
  }

  public void statusChanged(final StatusChangeEvent statusChangeEvent) {
    updateDisplay();
  }

  private void updateDisplay() {
    // Don't update the display while it is ineditable during the run.
    if (!cbStartingStep.isEditable()) {
      return;
    }
    // Starting Step - dependent on defined stop point
    boolean startingStepSelectionChanged = false;
    RadioButton.RadioButtonModel startingStepModel = null;
    boolean startingStepSelected = cbStartingStep.isSelected();
    boolean enabledStartingStep = cbEnableStartingStep.isSelected();
    EndingStep earliestRunEndingStep = table.getEarliestRunEndingStep();
    // Disable starting step if none of the radio buttons are available.
    cbStartingStep.setEnabled(enabledStartingStep || earliestRunEndingStep != null);
    if (!startingStepSelected) {
      // Checkbox is not checked - yeay - just disable everything
      for (int i = 0; i < STEP_PAIRS; i++) {
        rbStartingStep[i].setEnabled(false);
      }
    }
    else if (enabledStartingStep) {
      for (int i = 0; i < STEP_PAIRS; i++) {
        rbStartingStep[i].setEnabled(true);
      }
    }
    else {
      // Rule:
      // Disable past the defined stop point (paired with run to)
      // Don't jump ahead when restarting
      // defined stop point:
      // earliest completion status of all run checked datasets (disabled or enabled)
      int maxEnabled = 0;
      // earliestRunStep is the defined stop point
      if (earliestRunEndingStep != null) {
        maxEnabled = earliestRunEndingStep.getIndex() + 1;
      }
      for (int i = 0; i < maxEnabled; i++) {
        rbStartingStep[i].setEnabled(true);
      }
      for (int i = maxEnabled; i < STEP_PAIRS; i++) {
        rbStartingStep[i].setEnabled(false);
      }
      if (maxEnabled < STEP_PAIRS && maxEnabled > 0) {
        // At least one radio button was disabled
        // Rule:
        // Move the selection to the first enabled one
        // Move it only if the selected button is disabled
        startingStepModel = (RadioButton.RadioButtonModel) bgStartingStep.getSelection();
        if (startingStepModel != null && !startingStepModel.isEnabled()) {
          // The selected radio button is now disabled - move it
          rbStartingStep[maxEnabled - 1].setSelected(true);
          startingStepSelectionChanged = true;
        }
      }
    }
    // Ending Step - dependent on Starting Step
    boolean enableEndingStep = cbEndingStep.isSelected();
    if (!enableEndingStep) {
      // Checkbox is not checked - yeay - just disable everything
      for (int i = 0; i < STEP_PAIRS; i++) {
        rbEndingStep[i].setEnabled(false);
      }
    }
    else {
      // Rule:
      // disable up to checked & enabled start from (can't go backwards)
      // End has to be at least one more then start
      int enabledStartIndex = 0;
      if (startingStepSelected) {
        // Get the starting step model if it has changed or wasn't already retrieved
        if (startingStepModel == null || startingStepSelectionChanged) {
          startingStepModel =
            (RadioButton.RadioButtonModel) bgStartingStep.getSelection();
        }
        if (startingStepModel != null && startingStepModel.isEnabled()) {
          enabledStartIndex =
            ((StartingStep) startingStepModel.getEnumeratedType()).getIndex() + 1;
        }
      }
      for (int i = 0; i <= enabledStartIndex - 1; i++) {
        rbEndingStep[i].setEnabled(false);
      }
      for (int i = enabledStartIndex; i < STEP_PAIRS; i++) {
        rbEndingStep[i].setEnabled(true);
      }
      if (enabledStartIndex > 0 && enabledStartIndex < STEP_PAIRS) {
        // some radio buttons where disabled
        // Move the selection to the first enabled one
        // I think this should read "last enabled one"
        // Move it only if the selected button is disabled
        RadioButton.RadioButtonModel endingStepModel =
          (RadioButton.RadioButtonModel) bgEndingStep.getSelection();
        if (endingStepModel != null && !endingStepModel.isEnabled()) {
          // The selected radio button is now disabled - move it
          rbEndingStep[enabledStartIndex].setSelected(true);
        }
      }
    }
  }

  void getParameters(final BatchRunTomoMetaData metaData) {
    metaData.setUseEndingStep(cbEndingStep.isSelected());
    RadioButton.RadioButtonModel model =
      (RadioButton.RadioButtonModel) bgEndingStep.getSelection();
    if (model != null) {
      EndingStep endingStep = (EndingStep) model.getEnumeratedType();
      if (endingStep != null) {
        metaData.setEndingStep(endingStep);
      }
    }
    metaData.setUseStartingStep(cbStartingStep.isSelected());
    model = (RadioButton.RadioButtonModel) bgStartingStep.getSelection();
    if (model != null) {
      StartingStep startingStep = (StartingStep) model.getEnumeratedType();
      if (startingStep != null) {
        metaData.setStartingStep(startingStep);
      }
    }
    metaData.setEnableStartingStep(cbEnableStartingStep.isSelected());
  }

  void setParameters(final BatchRunTomoMetaData metaData) {
    cbEndingStep.setSelected(metaData.isUseEndingStep());
    EndingStep endingStep = metaData.getEndingStep();
    if (endingStep != null) {
      rbEndingStep[endingStep.getIndex()].setSelected(true);
    }
    cbStartingStep.setSelected(metaData.isUseStartingStep());
    StartingStep startingStep = metaData.getStartingStep();
    if (startingStep != null) {
      rbStartingStep[startingStep.getIndex()].setSelected(true);
    }
    cbEnableStartingStep.setSelected(metaData.isEnableStartingStep());
    statusChanged(metaData.getStatus());
    statusChanged(metaData.getEarliestRunEndingStep());
  }

  void setParameters(final BatchruntomoParam param) {
    EndingStep endingStep = EndingStep.getInstance(param.getEndingStep());
    if (endingStep != null) {
      rbEndingStep[endingStep.getIndex()].setSelected(true);
    }
    StartingStep startingStep = StartingStep.getInstance(param.getStartingStep());
    if (startingStep != null) {
      rbStartingStep[startingStep.getIndex()].setSelected(true);
    }
    updateDisplay();
  }

  void getParameters(final BatchruntomoParam param) {
    RadioButton.RadioButtonModel model;
    param.resetEndingStep();
    if (cbEndingStep.isEnabled() && cbEndingStep.isSelected()) {
      model = (RadioButton.RadioButtonModel) bgEndingStep.getSelection();
      if (model != null) {
        RadioButtonInterface button = model.getButton();
        if (button.isEnabled()) {
          param.setEndingStep(model.getEnumeratedType().getValue());
        }
      }
    }
    param.resetStartingStep();
    if (cbStartingStep.isEnabled() && cbStartingStep.isSelected()) {
      model = (RadioButton.RadioButtonModel) bgStartingStep.getSelection();
      if (model != null) {
        RadioButtonInterface button = model.getButton();
        if (button.isEnabled()) {
          param.setStartingStep(model.getEnumeratedType().getValue());
        }
      }
    }
  }

  private void setTooltips() {
    cbEndingStep.setToolTipText("Process all datasets through the selected step.");
    cbStartingStep.setToolTipText("Start all datasets from the selected step.");
    cbEnableStartingStep.setToolTipText(
      "Allow 'Start from' to be set past the point reached by all datasets.");
  }
}
