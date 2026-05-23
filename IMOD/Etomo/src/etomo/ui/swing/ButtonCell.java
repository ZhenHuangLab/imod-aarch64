package etomo.ui.swing;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;

import etomo.type.UITestFieldType;
import etomo.ui.TableComponent;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2014</p>
*
* <p>Organization:
* Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
* University of Colorado</p>
* 
* @author $Author$
* 
* @version $Revision$
* 
* <p> $Log$ </p>
*/
final class ButtonCell extends InputCell implements TableComponent {
  public static final String rcsid = "$Id:$";

  private final AbstractButton button;

  private FontMetrics fontMetrics = null;
  private boolean enabled = true;

  private ButtonCell(final Icon icon, final String title, final boolean toggle) {
    if (!toggle) {
      button = new JButton();
    }
    else {
      button = new JToggleButton();
    }
    if (icon != null) {
      button.setIcon(icon);
    }
    else if (title != null) {
      button.setText(title);
    }
    button.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
  }

  static ButtonCell getInstance(final Icon icon) {
    return new ButtonCell(icon, null, false);
  }

  static ButtonCell getToggleInstance(final String title) {
    return new ButtonCell(null, title, true);
  }

  String getUniqueActionCommand() {
    return getClass().getName() + "@" + Integer.toHexString(hashCode());
  }

  Component getComponent() {
    return button;
  }

  public int getPreferredWidth() {
    if (fontMetrics == null) {
      fontMetrics = UIUtilities.getFontMetrics(button);
    }
    return UIUtilities.getPreferredWidth(button, button.getText(), fontMetrics);
  }

  UITestFieldType getFieldType() {
    return UITestFieldType.BUTTON;
  }

  int getWidth() {
    return button.getWidth();
  }

  void setSelected(final boolean selected) {
    button.setSelected(selected);
  }

  void setActionCommand(final String input) {
    button.setActionCommand(input);
  }

  String getActionCommand() {
    return button.getActionCommand();
  }

  void addActionListener(final ActionListener listener) {
    button.addActionListener(listener);
  }

  void setEnabled(boolean enabled) {
    this.enabled = enabled;
    button.setEnabled(enabled && isEditable());
  }

  boolean isEnabled() {
    return enabled;
  }

  void setDisabledIcon(final Icon icon) {
    button.setDisabledIcon(icon);
  }

  void setToolTipText(String text) {
    button.setToolTipText(TooltipFormatter.INSTANCE.format(text));
  }
}
