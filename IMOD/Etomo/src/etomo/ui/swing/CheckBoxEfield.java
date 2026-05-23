package etomo.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JCheckBox;

import etomo.EtomoDirector;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.UITestFieldType;
import etomo.util.Utilities;

/**
 * <p>Description: An extensible check box.</p>
 * @see Ebutton
 * @see ToggleEbutton
 * 
 * <p>Copyright: Copyright 2017 - 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class CheckBoxEfield implements ActionListener, Controller {
  private final JCheckBox checkBox;
  private final ControlTarget controlTarget;
  private final ControlMode controlMode;

  private AppearanceExtension appearanceExtension = null;

  private CheckBoxEfield(String label, final ControlMode controlMode,
    final ControlTarget controlTarget) {
    this.controlMode = controlMode;
    this.controlTarget = controlTarget;
    if (label == null && controlTarget != null) {
      label = controlTarget.getLabel();
    }
    if (label == null && controlMode != null) {
      label = controlMode.getFieldName();
    }
    checkBox = new JCheckBox(label);
    setName(label, controlTarget, controlMode);
  }

  static CheckBoxEfield getInstance(final String label) {
    return new CheckBoxEfield(label, null, null);
  }

  static CheckBoxEfield getEnableControlInstance(final ControlTarget controlTarget) {
    CheckBoxEfield instance =
      new CheckBoxEfield(null, ControlState.ENABLE, controlTarget);
    instance.addListeners();
    return instance;
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
    checkBox.setName(
      UITestFieldType.CHECK_BOX.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
      System.out
        .println(checkBox.getName() + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
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

  Component getComponent() {
    return checkBox;
  }

  private void addListeners() {
    if (controlMode != null) {
      checkBox.addActionListener(this);
    }
  }

  void addActionListener(final ActionListener listener) {
    checkBox.addActionListener(listener);
  }

  /// controlMediator

  public void actionPerformed(final ActionEvent event) {
    if (controlMode != null) {
      ControlMediator.INSTANCE.controlEvent(this, controlTarget, controlMode);
    }
  }

  public boolean isControl() {
    return isSelected() && isEnabled();
  }
  
  public File selectFile() {
    return null;
  }

  /// AppearanceExtension

  public void setEditable(final boolean editable) {
    createAppearanceExtension();
    appearanceExtension.setEditable(editable);
  }

  private void createAppearanceExtension() {
    if (appearanceExtension == null) {
      appearanceExtension = new AppearanceExtension(checkBox);
    }
  }

  boolean isSelected() {
    return checkBox.isSelected();
  }

  public void setEnabled(final boolean enabled) {
    if (appearanceExtension != null) {
      appearanceExtension.setEnabled(enabled);
    }
    else {
      checkBox.setEnabled(enabled);
    }
    if (controlMode != null) {
      ControlMediator.INSTANCE.controlEvent(this, controlTarget, controlMode);
    }
  }

  void clear() {
    setSelected(false);
  }
 
  void setVisible(final boolean visible) {
    getComponent().setVisible(visible);
  }

  boolean isVisible() {
    return getComponent().isVisible();
  }

  boolean isEnabled() {
    if (appearanceExtension != null) {
      return appearanceExtension.isEnabled();
    }
    return checkBox.isEnabled();
  }

  boolean isEditable() {
    if (appearanceExtension != null) {
      return appearanceExtension.isEditable();
    }
    return true;
  }

  void setToolTipText(String text) {
    checkBox.setToolTipText(TooltipFormatter.INSTANCE.format(text));
  }

  String getActionCommand() {
    return checkBox.getActionCommand();
  }

  void setSelected(final boolean selected) {
    checkBox.setSelected(selected);
    if (controlMode != null) {
      ControlMediator.INSTANCE.controlEvent(this, controlTarget, controlMode);
    }
  }
}