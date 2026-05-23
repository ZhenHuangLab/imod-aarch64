package etomo.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import etomo.EtomoDirector;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.UITestFieldType;
import etomo.ui.FlagDisplay;
import etomo.ui.FlagType;
import etomo.util.Utilities;

/**
 * <p>Description: An extensible JButton.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class Ebutton implements ActionListener, FlagDisplay, Controller {
  private final JButton button = new JButton();

  private final ButtonStyleExtension buttonStyle;
  private final ControlTarget target;
  private final ControlMode controlMode;
  private final SelectFileExtension selectFileExtension;

  /**
   * implementToggle gives a JButton the appearance of toggling, the button style used
   * comtains a complete icon with a selected image.
   */
  private final boolean implementToggle;

  private GridBagExtension gridBagExtension = null;
  private boolean selected = false;
  private AppearanceExtension appearanceExtension = null;
  private List<ActionListener> actionListeners = null;
  private boolean allowFlagEditableControl = true;
  private boolean respondToNonErrorFlags = true;
  private FlagType flagType = null;
  private ControlMediator controlMediator = null;
  private boolean buttonActionListening = false;
  private File overrideSelectFileDirectory = null;

  private Ebutton(final ControlMode controlMode, final ControlTarget target,
    final String label, final Dimension fixedSize, final ButtonStyleExtension buttonStyle,
    final boolean implementToggle, final SelectFileExtension sharedSelectFileExtension) {
    this.controlMode = controlMode;
    this.target = target;
    this.buttonStyle = buttonStyle;
    this.implementToggle = implementToggle;
    if (label != null) {
      button.setText(label);
    }
    setName(label, target, controlMode);
    if (fixedSize != null) {
      button.setPreferredSize(fixedSize);
      button.setMaximumSize(fixedSize);
    }
    if (buttonStyle != null) {
      buttonStyle.setup(button, label);
    }
    if (controlMode == ControlMode.SELECT_FILE) {
      if (sharedSelectFileExtension != null) {
        // Shared extension avaiable
        selectFileExtension = sharedSelectFileExtension;
      }
      else {
        selectFileExtension = new SelectFileExtension();
      }
    }
    else {
      selectFileExtension = null;
    }
  }

  public String toString() {
    return "[" + button.getName() + "," + button.getText() + "]";
  }

  private void setName(final String label, final ControlTarget target,
    final ControlMode controlMode) {
    // build name
    String name = null;
    if (label != null) {
      name = appendToName(name, Utilities.convertLabelToName(label));
    }
    else {
      if (target != null) {
        name = Utilities.convertLabelToName(target.getLabel());
      }
      if (controlMode != null) {
        name = appendToName(name, controlMode.getFieldName());
      }
    }
    if (name == null) {
      return;
    }
    // set the name in the field
    button.setName(
      UITestFieldType.BUTTON.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
      System.out
        .println(button.getName() + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
    }
  }

  private String appendToName(final String name, final String appendName) {
    if (name == null) {
      return appendName;
    }
    if (appendName == null) {
      return name;
    }
    return name + Utilities.NAME_SEPARATOR + appendName;
  }

  static Ebutton getSingleLineInstance(final String label) {
    Ebutton instance = new Ebutton(null, null, label, null,
      SingleLineButtonStyleExtension.getInstance(), false, null);
    instance.createPanel();
    return instance;
  }

  static Ebutton getOpenCloseInstance(final String label) {
    Ebutton instance = new Ebutton(null, null, label, null,
      OpenCloseButtonStyleExtension.getInstance(), true, null);
    instance.createPanel();
    return instance;
  }

  /**
   * 
   * @param target
   * @param sharedFileOpenExtension (optional) for group of button that can use the same file chooser
   * @return
   */
  static Ebutton getSelectFileInstance(final ControlTarget target,
    final SelectFileExtension sharedSelectFileExtension) {
    Ebutton instance =
      new Ebutton(ControlMode.SELECT_FILE, target, null, FixedDim.INLINE_SQUARE_SIZE,
        FileOpenButtonStyleExtension.getInstance(), false, sharedSelectFileExtension);
    instance.createPanel();
    return instance;
  }

  static Ebutton getClearInstance(final ControlTarget target) {
    Ebutton instance = new Ebutton(ControlMode.CLEAR, target, null,
      FixedDim.INLINE_SQUARE_SIZE, ClearButtonStyleExtension.getInstance(), false, null);
    instance.createPanel();
    return instance;
  }

  static Ebutton getHeaderInstance(final String label) {
    Ebutton instance = new Ebutton(null, null, label, null,
      HeaderButtonStyleExtension.getInstance(), false, null);
    instance.createPanel();
    return instance;
  }

  static Ebutton getHeaderInstance() {
    Ebutton instance = new Ebutton(null, null, null, null,
      HeaderButtonStyleExtension.getInstance(), false, null);
    instance.createPanel();
    return instance;
  }

  public void setAllowFlagEditableControl(final boolean allow) {
    allowFlagEditableControl = allow;
  }

  public void setRespondToNonErrorFlags(final boolean respond) {
    respondToNonErrorFlags = respond;
  }

  private void createPanel() {
    if ((implementToggle && buttonStyle != null) || controlMode != null) {
      addActionListener();
    }
  }

  private void addActionListener() {
    if (!buttonActionListening) {
      button.addActionListener(this);
      buttonActionListening = true;
    }
  }

  Component getComponent() {
    return button;
  }

  void setHorizontalAlignment(final int alignment) {
    button.setHorizontalAlignment(alignment);
  }

  void doClick() {
    button.doClick();
  }

  void addActionListener(final ActionListener listener) {
    if (listener == null) {
      return;
    }
    if (actionListeners == null) {
      addActionListener();
      actionListeners = new ArrayList<ActionListener>();
    }
    actionListeners.add(listener);
  }

  String getActionCommand() {
    return button.getActionCommand();
  }

  String getText() {
    return button.getText();
  }

  void setVisible(final boolean visible) {
    getComponent().setVisible(visible);
  }

  void setOverrideFileOpenDirectory(final File overrideSelectFileDirectory) {
    this.overrideSelectFileDirectory = overrideSelectFileDirectory;
  }

  void setFileFilter(final FileFilter fileFilter) {
    if (selectFileExtension != null) {
      selectFileExtension.setFileFilter(fileFilter);
    }
  }

  void setFileSelectionMode(final int fileSelectionMode) {
    if (selectFileExtension != null) {
      selectFileExtension.setFileSelectionMode(fileSelectionMode);
    }
  }

  void setSelectFileDir(final String dir) {
    if (selectFileExtension != null) {
      selectFileExtension.setDir(dir);
    }
  }

  SelectFileExtension getSelectFileExtension() {
    return selectFileExtension;
  }

  void setSelected(final boolean selected) {
    this.selected = selected;
    if (buttonStyle != null && implementToggle) {
      buttonStyle.updateAppearance(button, flagType, implementToggle, selected);
    }
  }

  public boolean isControl() {
    return isSelected() && isEnabled();
  }

  boolean isSelected() {
    return selected;
  }

  void setTooltip(final String tooltip) {
    button.setToolTipText(TooltipFormatter.INSTANCE.format(tooltip));
  }

  // controlMediator

  public void actionPerformed(final ActionEvent event) {
    if (implementToggle && buttonStyle != null) {
      selected = !selected;
      buttonStyle.updateAppearance(button, flagType, implementToggle, selected);
    }
    if (controlMode != null) {
      ControlMediator.INSTANCE.controlEvent(this, target, controlMode);
    }
    if (actionListeners != null) {
      Iterator<ActionListener> iterator = actionListeners.iterator();
      while (iterator.hasNext()) {
        iterator.next().actionPerformed(event);
      }
    }
  }

  public File selectFile() {
    if (selectFileExtension != null) {
      return selectFileExtension.selectFile(button, overrideSelectFileDirectory);
    }
    return null;
  }

  /// appearanceExtension

  private void createAppearanceExtension() {
    if (appearanceExtension == null) {
      appearanceExtension = new AppearanceExtension(button);
      appearanceExtension.setAllowFlagEditableControl(allowFlagEditableControl);
      appearanceExtension.setRespondToNonErrorFlags(respondToNonErrorFlags);
    }
  }

  public void setFlag(final FlagType flagType) {
    this.flagType = flagType;
    if (buttonStyle != null) {
      buttonStyle.updateAppearance(button, flagType, implementToggle, selected);
    }
    createAppearanceExtension();
    appearanceExtension.setFlag(flagType);
  }

  public void setEnabled(final boolean enabled) {
    if (appearanceExtension != null) {
      appearanceExtension.setEnabled(enabled);
    }
    else {
      button.setEnabled(enabled);
    }
  }

  boolean isEnabled() {
    if (appearanceExtension != null) {
      return appearanceExtension.isEnabled();
    }
    return button.isEnabled();
  }

  public void setEditable(final boolean editable) {
    createAppearanceExtension();
    appearanceExtension.setEditable(editable);
  }

  /// gridBagExtension

  void remove() {
    if (gridBagExtension != null) {
      gridBagExtension.remove(getComponent());
    }
  }

  void add(final JPanel panel, final GridBagLayout layout,
    final GridBagConstraints constraints) {
    if (gridBagExtension == null) {
      gridBagExtension = new GridBagExtension();
    }
    gridBagExtension.add(getComponent(), panel, layout, constraints);
  }
}