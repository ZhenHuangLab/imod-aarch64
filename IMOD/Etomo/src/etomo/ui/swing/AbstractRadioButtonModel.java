package etomo.ui.swing;

import javax.swing.JToggleButton;

import etomo.type.EnumeratedType;

abstract class AbstractRadioButtonModel extends JToggleButton.ToggleButtonModel {
  public abstract EnumeratedType getEnumeratedType();
}
