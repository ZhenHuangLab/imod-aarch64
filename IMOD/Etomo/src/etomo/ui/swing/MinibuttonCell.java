package etomo.ui.swing;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.border.BevelBorder;

import etomo.type.Run3dmodMenuOptions;
import etomo.type.UITestFieldType;
import etomo.ui.Run3dmodMenuTarget;
import etomo.ui.UIComponent;

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
final class MinibuttonCell extends InputCell
  implements UIComponent, SwingComponent, Run3dmodMenuTarget, ContextMenu {
  public static final String rcsid = "$Id:$";

  private final Minibutton button;
  private final Run3dmodMenu contextMenu;
  private final Run3dmodButtonContainer container;

  private boolean enabled = true;
  private boolean debug = false;

  private MinibuttonCell(final Icon icon, final boolean run3dmod,
    final Run3dmodButtonContainer container) {
    this.container = container;
    button = Minibutton.getSquareInstance(icon,
      BorderFactory.createBevelBorder(BevelBorder.RAISED));
    if (run3dmod) {
      contextMenu = Run3dmodMenu.get3dmodButtonInstance(this, null);
    }
    else {
      contextMenu = null;
    }
  }

  String getUniqueActionCommand() {
    return getClass().getName() + "@" + Integer.toHexString(hashCode());
  }

  static MinibuttonCell getInstance(final Icon icon) {
    MinibuttonCell instance = new MinibuttonCell(icon, false, null);
    return instance;
  }

  static MinibuttonCell getRun3dmodInstance(final Icon icon,
    final Run3dmodButtonContainer container) {
    MinibuttonCell instance = new MinibuttonCell(icon, true, container);
    instance.addListeners();
    return instance;
  }

  private void addListeners() {
    if (contextMenu != null) {
      button.addMouseListener(new GenericMouseAdapter(this));
    }
  }

  public Component getComponent() {
    return button;
  }

  public SwingComponent getUIComponent() {
    return this;
  }

  public void popUpContextMenu(MouseEvent mouseEvent) {
    contextMenu.popUpContextMenu(mouseEvent);
  }

  public void menuAction(Run3dmodMenuOptions run3dmodMenuOptions) {
    if (container != null) {
      container.action(getActionCommand(), null, run3dmodMenuOptions);
    }
  }

  UITestFieldType getFieldType() {
    return UITestFieldType.BUTTON;
  }

  int getWidth() {
    return button.getWidth();
  }

  void addActionListener(final ActionListener listener) {
    button.addActionListener(listener);
  }

  void setDebug(final boolean input) {
    super.setDebug(input);
    debug = input;
  }

  void setEnabled(boolean enabled) {
    this.enabled = enabled;
    button.setEnabled(enabled && isEditable());
  }

  public boolean isEnabled() {
    return enabled;
  }

  void setDisabledIcon(final Icon icon) {
    button.setDisabledIcon(icon);
  }

  void setPressedIcon(final Icon icon) {
    button.setPressedIcon(icon);
  }

  void setToolTipText(String text) {
    button.setToolTipText(TooltipFormatter.INSTANCE.format(text));
  }

  void setActionCommand(final String input) {
    button.setActionCommand(input);
  }

  String getActionCommand() {
    return button.getActionCommand();
  }
}
