package etomo.plugin.demo;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;

import etomo.ApplicationManager;
import etomo.plugin.PluginPanel;
import etomo.storage.LogFile;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.type.AxisID;
import etomo.type.BaseScreenState;
import etomo.type.ConstEtomoNumber;
import etomo.type.DialogType;
import etomo.type.EnumeratedType;
import etomo.type.EtomoAutodoc;
import etomo.type.Run3dmodMenuOptions;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.UIComponent;
import etomo.ui.swing.CheckBox;
import etomo.ui.swing.CheckTextField;
import etomo.ui.swing.ContextMenu;
import etomo.ui.swing.ContextPopup;
import etomo.ui.swing.Deferred3dmodButton;
import etomo.ui.swing.ExpandButton;
import etomo.ui.swing.Expandable;
import etomo.ui.swing.FileTextField2;
import etomo.ui.swing.FixedDim;
import etomo.ui.swing.GenericMouseAdapter;
import etomo.ui.swing.GlobalExpandButton;
import etomo.ui.swing.LabeledTextField;
import etomo.ui.swing.MultiLineButton;
import etomo.ui.swing.PanelHeader;
import etomo.ui.swing.RadioButton;
import etomo.ui.swing.RadioTextField;
import etomo.ui.swing.Run3dmodButton;
import etomo.ui.swing.Run3dmodButtonContainer;
import etomo.ui.swing.SwingComponent;
import etomo.ui.swing.TomogramGenerationDialog;
import etomo.ui.swing.UIHarness;

/**
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 *
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 */
final class DemoPanel implements PluginPanel, ContextMenu, Run3dmodButtonContainer,
  Expandable, ActionListener, UIComponent, SwingComponent {
  private static final String TITLE = "Demo";

  private final JPanel pnlRoot = new JPanel();
  private final JPanel pnlDemoBody = new JPanel(true);

  private final CheckBox cbSleepTime = new CheckBox("Modify sleep time");
  private final ButtonGroup bgSleepTime = new ButtonGroup();
  private final RadioButton rbSleepTime2 = new RadioButton(SleepTime.TWO, bgSleepTime);
  private final RadioButton rbSleepTime3 = new RadioButton(SleepTime.THREE, bgSleepTime);
  private final RadioTextField rtfSleepTime =
    RadioTextField.getInstance(FieldType.INTEGER, SleepTime.USER_ENTRY, bgSleepTime);

  private final CheckTextField ctfMessage =
    CheckTextField.getInstance(FieldType.STRING, "Override default message: ");
  private final CheckBox cbSwapYZ = new CheckBox("Swap Y and Z axes");
  private final Run3dmodButton btn3dmod =
    Run3dmodButton.get3dmodInstance("Open File", this);
  private final LabeledTextField ltfSleepTimeUsed =
    new LabeledTextField(FieldType.STRING, "Sleep time used: ");
  private final LabeledTextField ltfCpus =
    new LabeledTextField(FieldType.STRING, "CPUs selected: ");

  private final ApplicationManager manager;
  private final AxisID axisID;
  private final DemoPluginManager pluginManager;
  private final TomogramGenerationDialog parent;
  private final PanelHeader phDemo;
  private final FileTextField2 ftfFile;
  // Run3dmodButton is used for this run button so that it has a right-click menu
  private final Run3dmodButton btnEtomoPluginDemo;
  private final MultiLineButton btnUnselectDependencies;

  private DemoPanel(final DemoPluginManager pluginManager,
    final ApplicationManager manager, final AxisID axisID, final DialogType dialogType,
    final GlobalExpandButton globalAdvancedButton,
    final TomogramGenerationDialog parent) {
    this.manager = manager;
    this.axisID = axisID;
    this.pluginManager = pluginManager;
    this.parent = parent;
    phDemo =
      PanelHeader.getAdvancedBasicInstance(TITLE, this, dialogType, globalAdvancedButton);
    ftfFile = FileTextField2.getInstance(manager, "Choose a file: ");
    DemoProcessResultDisplayFactory factory =
      pluginManager.getProcessResultDisplayFactory();
    btnEtomoPluginDemo = (Run3dmodButton) factory.getEtomoPluginDemo();
    btnUnselectDependencies = (MultiLineButton) factory.getUnselectDependencies();
  }

  public static DemoPanel getInstance(final DemoPluginManager pluginManager,
    final ApplicationManager manager, final AxisID axisID, final DialogType dialogType,
    final GlobalExpandButton globalAdvancedButton,
    final TomogramGenerationDialog parent) {
    DemoPanel instance = new DemoPanel(pluginManager, manager, axisID, dialogType,
      globalAdvancedButton, parent);
    instance.createPanel();
    instance.setToolTipText();
    instance.addListeners();
    return instance;
  }

  /**
   * Implementing SwingComponent and UIComponent allows UIHarness and Popup to pop up
   * messages without using the manager and the axisID.
   */
  public SwingComponent getUIComponent() {
    return this;
  }

  public Component getComponent() {
    return pnlRoot;
  }

  public String getButtonTitle() {
    return TITLE;
  }

  /**
   * This context menu will override the dialog's menu.
   */
  public void popUpContextMenu(MouseEvent mouseEvent) {
    String[] manPageLabel = new String[1];
    String[] manPage = new String[1];
    String[] logFileLabel = new String[1];
    String[] logFile = new String[1];
    int i = 0;
    manPageLabel[i] = "3dmod";
    manPage[i] = "3dmod.html";
    logFileLabel[i] = "EtomoPluginDemo";
    logFile[i++] = "demo" + axisID.getExtension() + ".log";

    ContextPopup contextPopup = new ContextPopup(pnlRoot, mouseEvent,
      "TOMOGRAM GENERATION", ContextPopup.TOMO_GUIDE, manPageLabel, manPage, logFileLabel,
      logFile, manager, axisID);
  }

  private void createPanel() {
    // local panels
    JPanel pnlDemo = new JPanel();
    JPanel pnlButtons = new JPanel();
    JPanel pnlSleepTime = new JPanel();
    JPanel pnlFile = new JPanel();
    JPanel pnlOutput = new JPanel();
    // init
    rbSleepTime2.setSelected(true);
    ftfFile.setFileFilter(Generic3dmodFileFilter.getInstance(manager));
    // Allow the demo button to run 3dmod via its right-click menu. The 3dmod run command
    // is sent to the action functon in the container.
    btnEtomoPluginDemo.setContainer(this);
    btnEtomoPluginDemo.setDeferred3dmodButton(btn3dmod);
    ltfCpus.setEditable(false);
    ltfSleepTimeUsed.setEditable(false);
    // Root
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
    pnlRoot.add(pnlDemo);
    pnlRoot.add(Box.createRigidArea(FixedDim.x0_y10));
    pnlRoot.add(pnlButtons);
    // Demo
    pnlDemo.setLayout(new BoxLayout(pnlDemo, BoxLayout.Y_AXIS));
    pnlDemo.setBorder(BorderFactory.createEtchedBorder());
    pnlDemo.add(phDemo.getComponent());
    pnlDemo.add(pnlDemoBody);
    // DemoBody
    pnlDemoBody.setLayout(new BoxLayout(pnlDemoBody, BoxLayout.Y_AXIS));
    pnlDemoBody.add(Box.createRigidArea(FixedDim.x0_y2));
    pnlDemoBody.add(pnlSleepTime);
    pnlDemoBody.add(Box.createRigidArea(FixedDim.x0_y2));
    pnlDemoBody.add(ctfMessage.getComponent());
    pnlDemoBody.add(Box.createRigidArea(FixedDim.x0_y2));
    pnlDemoBody.add(pnlFile);
    pnlDemoBody.add(Box.createRigidArea(FixedDim.x0_y10));
    pnlDemoBody.add(pnlOutput);
    pnlDemoBody.add(Box.createRigidArea(FixedDim.x0_y2));
    // SleepTime
    pnlSleepTime.setLayout(new BoxLayout(pnlSleepTime, BoxLayout.X_AXIS));
    pnlSleepTime.add(cbSleepTime.getComponent());
    pnlSleepTime.add(rbSleepTime2.getComponent());
    pnlSleepTime.add(rbSleepTime3.getComponent());
    pnlSleepTime.add(rtfSleepTime.getContainer());
    // File
    pnlFile.setLayout(new BoxLayout(pnlFile, BoxLayout.X_AXIS));
    pnlFile.add(ftfFile.getRootPanel());
    pnlFile.add(cbSwapYZ.getComponent());
    // Output
    pnlOutput.setLayout(new BoxLayout(pnlOutput, BoxLayout.X_AXIS));
    pnlOutput.add(ltfSleepTimeUsed.getComponent());
    pnlOutput.add(Box.createRigidArea(FixedDim.x10_y0));
    pnlOutput.add(ltfCpus.getComponent());
    // Buttons
    pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
    pnlButtons.add(btnEtomoPluginDemo.getComponent());
    pnlButtons.add(btn3dmod.getComponent());
    pnlButtons.add(btnUnselectDependencies.getComponent());
    updateDisplay();
  }

  private void addListeners() {
    GenericMouseAdapter mouseAdapter = new GenericMouseAdapter(this);
    pnlRoot.addMouseListener(mouseAdapter);
    // An action listener is so simple that it makes sense for this class to implement it.
    btnEtomoPluginDemo.addActionListener(this);
    btn3dmod.addActionListener(this);
    btnUnselectDependencies.addActionListener(this);
    cbSleepTime.addActionListener(this);
  }

  public void msgFieldChanged(final boolean differentFromCheckpoint) {}

  public void updateDisplay() {
    boolean sleepTime = cbSleepTime.isSelected();
    rbSleepTime2.setEnabled(sleepTime);
    rbSleepTime3.setEnabled(sleepTime);
    rtfSleepTime.setEnabled(sleepTime);
  }

  public void msgVisibilityChanged(boolean visible) {
    pnlRoot.setVisible(visible);
  }

  public void done() {
    btnEtomoPluginDemo.removeActionListener(this);
    btnUnselectDependencies.removeActionListener(this);
  }

  /**
   * Created DemoScreenState for custom fields.  This class can be inserted into the
   * manager's screen state, and so does not require management within the plugin.
   * @param baseScreenState
   */
  public void getParameters(final BaseScreenState screenState) {
    DemoScreenState demoScreenState = pluginManager.getDemoScreenState();
    btnEtomoPluginDemo
      .setButtonState(screenState.getButtonState(btnEtomoPluginDemo.getButtonStateKey()));
    btnUnselectDependencies.setButtonState(
      screenState.getButtonState(btnUnselectDependencies.getButtonStateKey()));
    phDemo.getState(demoScreenState.getTomoGenDemoHeaderState());
  }

  public void setParameters(final BaseScreenState screenState) {
    DemoScreenState demoScreenState = pluginManager.getDemoScreenState();
    phDemo.setState(demoScreenState.getTomoGenDemoHeaderState());
    btnEtomoPluginDemo
      .setButtonState(screenState.getButtonState(btnEtomoPluginDemo.getButtonStateKey()));
    btnUnselectDependencies.setButtonState(
      screenState.getButtonState(btnUnselectDependencies.getButtonStateKey()));
  }

  public boolean getParameters(final EtomoPluginDemoParam param,
    final boolean doValidation) {
    try {
      if (cbSleepTime.isSelected()) {
        // Demonstration of the use of EnumeratedType with RadioButton
        EnumeratedType sleepTime =
          ((RadioButton.RadioButtonModel) bgSleepTime.getSelection()).getEnumeratedType();
        if (sleepTime != null) {
          ConstEtomoNumber value = sleepTime.getValue();
          if (value == null && sleepTime == SleepTime.USER_ENTRY) {
            param.setSleepTime(rtfSleepTime.getText(doValidation));
          }
          else {
            param.setSleepTime(value);
          }
        }
      }
      else {
        param.resetSleepTime();
      }
      if (ctfMessage.isSelected()) {
        param.setMessage(ctfMessage.getText(doValidation));
      }
      else {
        param.resetMessage();
      }
      return true;
    }
    catch (FieldValidationFailedException e) {
      // The validation has its own, built-in message popups.
      return false;
    }
  }

  void setParameters(final EtomoPluginDemoParam param) {
    SleepTime sleepTime = param.getSleepTime();
    if (sleepTime == null || sleepTime.isDefault()) {
      cbSleepTime.setSelected(false);
    }
    else {
      cbSleepTime.setSelected(true);
      if (sleepTime == SleepTime.TWO) {
        rbSleepTime2.setSelected(true);
      }
      else if (sleepTime == SleepTime.THREE) {
        rbSleepTime3.setSelected(true);
      }
      else if (sleepTime == SleepTime.USER_ENTRY) {
        rtfSleepTime.setSelected(true);
        rtfSleepTime.setText(param.getSleepTimeValue());
      }
    }
    ctfMessage.setSelected(param.isMessageSet());
    if (ctfMessage.isSelected()) {
      ctfMessage.setText(param.getMessage());
    }
    updateDisplay();
  }

  void setSleepTimeUsed(final int sleepTime) {
    ltfSleepTimeUsed.setText(sleepTime);
  }

  String getSleepTimeUsed() {
    return ltfSleepTimeUsed.getText();
  }

  void setCpus(final String input) {
    ltfCpus.setText(input);
  }

  public void updateAdvanced(final boolean advanced) {
    ctfMessage.setVisible(advanced);
  }

  public void expand(final GlobalExpandButton button) {
    // Handled by the panel header, to which the global advanced button was added.
  }

  public void expand(final ExpandButton button) {
    if (phDemo != null) {
      if (phDemo.equalsOpenClose(button)) {
        pnlDemoBody.setVisible(button.isExpanded());
      }
      else if (phDemo.equalsAdvancedBasic(button)) {
        updateAdvanced(button.isExpanded());
      }
    }
    UIHarness.INSTANCE.pack(axisID, manager);
  }

  public void actionPerformed(final ActionEvent event) {
    action(event.getActionCommand(), null, null);
  }

  /**
   * @param command - action command of the event
   * @param deferred3dmodButton - the button assigned to run 3dmod after the process series associated with this event is complete
   * @param run3dmodMenuOptions - result of right-click menu of the button
   */
  public void action(final String command, Deferred3dmodButton deferred3dmodButton,
    final Run3dmodMenuOptions run3dmodMenuOptions) {
    if (command.equals(btnEtomoPluginDemo.getActionCommand())) {
      ltfSleepTimeUsed.setText("");
      pluginManager.etomoPluginDemo(btnEtomoPluginDemo, null, deferred3dmodButton,
        run3dmodMenuOptions);
    }
    else if (command.equals(btn3dmod.getActionCommand())) {
      pluginManager.open3dmod(ftfFile.getFile(), cbSwapYZ.isSelected(),
        run3dmodMenuOptions);
    }
    else if (command.equals(btnUnselectDependencies.getActionCommand())) {
      pluginManager.unselectDependencies(btnUnselectDependencies, null);
    }
    else {
      updateDisplay();
    }
  }

  private void setToolTipText() {
    ReadOnlyAutodoc autodoc = null;
    try {
      autodoc = pluginManager.getAutodoc();
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

    String tooltipText =
      EtomoAutodoc.getTooltip(autodoc, EtomoPluginDemoParam.SLEEP_TIME_KEY);
    cbSleepTime.setToolTipText(tooltipText);
    rbSleepTime2.setToolTipText(tooltipText);
    rbSleepTime3.setToolTipText(tooltipText);
    rtfSleepTime.setToolTipText(tooltipText);
    ctfMessage
      .setToolTipText(EtomoAutodoc.getTooltip(autodoc, EtomoPluginDemoParam.MESSAGE_KEY));
    ltfCpus.setToolTipText("The CPUs selected when the process completed");
  }
}