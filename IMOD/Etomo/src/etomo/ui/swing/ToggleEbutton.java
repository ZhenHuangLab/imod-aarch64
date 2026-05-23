package etomo.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

import etomo.ui.BooleanEfieldInterface;
import etomo.ui.FlagDisplay;
import etomo.ui.FlagType;

/**
 * <p>Description: An extensible JToggleButton.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class ToggleEbutton
  implements ActionListener, BooleanEfieldInterface, FlagDisplay, Controller {
  private final JToggleButton button = new JToggleButton();

  private final ControlTarget target;
  private final ControlMode controlMode;
  private final ButtonStyleExtension buttonStyle;

  private GridBagExtension gridBagExtension = null;
  private ControlMediator controlMediator = null;
  private AppearanceExtension appearanceExtension = null;
  private boolean debug = false;

  private ToggleEbutton(final ControlMode controlMode, final ControlTarget target,
    final Dimension fixedSize, final ButtonStyleExtension buttonStyle) {
    this.controlMode = controlMode;

    this.target = target;
    this.buttonStyle = buttonStyle;
    if (fixedSize != null) {
      button.setPreferredSize(fixedSize);
      button.setMaximumSize(fixedSize);
    }
    if (buttonStyle != null) {
      buttonStyle.setup(button, null);
    }
  }

  static ToggleEbutton getOverrideInstance(final ControlTarget target) {
    ToggleEbutton instance = new ToggleEbutton(ControlState.OVERRIDE, target,
      FixedDim.INLINE_SQUARE_SIZE, XButtonStyleExtension.getInstance());
    instance.createPanel();
    return instance;
  }

  private void createPanel() {
    if (controlMode != null) {
      button.addActionListener(this);
    }
  }

  Component getComponent() {
    return button;
  }

  public boolean isControl() {
    return isSelected() && isEnabled();
  }

  public File selectFile() {
    return null;
  }

  public boolean isSelected() {
    return button.isSelected();
  }

  boolean isEnabled() {
    return button.isEnabled();
  }

  void setDebug(final boolean debug) {
    this.debug = debug;
  }

  public void setSelected(final boolean selected) {
    button.setSelected(selected);
    actionPerformed(null);
  }

  void setVisible(final boolean visible) {
    button.setVisible(visible);
  }

  /// appearanceExtension

  private void createAppearanceExtension() {
    if (appearanceExtension == null) {
      appearanceExtension = new AppearanceExtension(button);
    }
  }

  public void setEnabled(final boolean enabled) {
    if (appearanceExtension != null) {
      appearanceExtension.setEnabled(enabled);
    }
    else {
      button.setEnabled(enabled);
    }
  }

  public void setEditable(final boolean editable) {
    createAppearanceExtension();
    appearanceExtension.setEditable(editable);
  }

  public void setFlag(final FlagType flagType) {
    if (buttonStyle != null) {
      buttonStyle.updateAppearance(button, flagType, false, isSelected());
    }
    createAppearanceExtension();
    appearanceExtension.setFlag(flagType);
  }

  /**
   * @param event ignored
   */
  public void actionPerformed(final ActionEvent event) {
    if (controlMode != null) {
      ControlMediator.INSTANCE.controlEvent(this, target, controlMode);
    }
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