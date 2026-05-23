package etomo.ui.swing;

import java.awt.event.MouseEvent;

import etomo.type.DialogType;
import etomo.type.Run3dmodMenuOptions;
import etomo.ui.Run3dmodMenuTarget;

/**
 * <p>Description: A single line run 3dmod button. </p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class Run3dmodSingleLineButton extends SingleLineButton
  implements Deferred3dmodButton, Run3dmodMenuTarget, ContextMenu {
  // When the button is not a 3dmod button, then it may run 3dmod deferred; first
  // running the process associated with the button and then running 3dmod as
  // directed by the right-click menu. The right click menu contains a plain
  // 3dmod option (noMenuOption) when deferred is true.
  private final boolean deferred;
  private final Run3dmodMenu run3dmodMenu;

  private Run3dmodButtonContainer container = null;
  // When deferred is true, need a button that knows how to run the 3dmod command.
  private Deferred3dmodButton deferred3dmodButton = null;

  private Run3dmodSingleLineButton(final String label,
    final Run3dmodButtonContainer container, final boolean toggleButton,
    final DialogType dialogType, boolean deferred, String description) {
    super(label, toggleButton, dialogType);
    this.container = container;
    this.deferred = deferred;
    if (deferred) {
      run3dmodMenu = Run3dmodMenu.getProcessButtonInstance(this, description);
    }
    else {
      run3dmodMenu = Run3dmodMenu.get3dmodButtonInstance(this, description);
    }
  }

  static Run3dmodSingleLineButton get3dmodInstance(final String label,
    final Run3dmodButtonContainer container) {
    Run3dmodSingleLineButton instance =
      new Run3dmodSingleLineButton(label, container, false, null, false, null);
    instance.addListeners();
    return instance;
  }

  private void addListeners() {
    addMouseListener(new GenericMouseAdapter(this));
  }

  Deferred3dmodButton getDeferred3dmodButton() {
    return deferred3dmodButton;
  }

  public void popUpContextMenu(MouseEvent mouseEvent) {
    run3dmodMenu.popUpContextMenu(mouseEvent);
  }

  public void menuAction(Run3dmodMenuOptions run3dmodMenuOptions) {
    action(run3dmodMenuOptions);
    if (isToggleButton()) {
      setSelected(true);
    }
  }

  public void action(Run3dmodMenuOptions menuOptions) {
    if (container != null) {
      container.action(getActionCommand(), getDeferred3dmodButton(), menuOptions);
    }
  }
}