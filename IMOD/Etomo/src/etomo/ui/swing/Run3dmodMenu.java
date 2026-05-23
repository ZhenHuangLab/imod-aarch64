package etomo.ui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import etomo.type.Run3dmodMenuOptions;
import etomo.ui.Run3dmodMenuTarget;

/**
* <p>Description: Constructs a run 3dmod context menu.  When the context menu is
* triggered, it shows the menu based on the ContextMenuTarget, and tells the target what
* menu option was chosen.</p>
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
final class Run3dmodMenu implements ActionListener {
  public static final String rcsid = "$Id:$";

  private static final String DEFAULT_DESCR = "3dmod";

  private final JPopupMenu contextMenu = new JPopupMenu("3dmod Options");

  private final Run3dmodMenuTarget target;
  private final JMenuItem startupWindow;
  private final JMenuItem binBy2;
  private final JMenuItem run3dmod;

  private Run3dmodMenu(final Run3dmodMenuTarget target, final String openString,
      final boolean processButton) {
    this.target = target;
    startupWindow = new MenuItem(openString + " with startup window");
    binBy2 = new MenuItem(openString + " binned by 2");
    if (processButton) {
      run3dmod = new MenuItem(openString);
    }
    else {
      run3dmod = null;
    }
  }

  static Run3dmodMenu get3dmodButtonInstance(final Run3dmodMenuTarget button,
      final String descr) {
    Run3dmodMenu instance = new Run3dmodMenu(button, descr == null ? "Open" : "Open "
        + descr, false);
    instance.createMenu();
    instance.addListeners();
    return instance;
  }

  static Run3dmodMenu getProcessButtonInstance(final Run3dmodMenuTarget button,
      String descr) {
    if (descr == null) {
      descr = DEFAULT_DESCR;
    }
    Run3dmodMenu instance = new Run3dmodMenu(button, "And open " + descr, true);
    instance.createMenu();
    instance.addListeners();
    return instance;
  }

  private void createMenu() {
    if (run3dmod != null) {
      contextMenu.add(run3dmod);
    }
    contextMenu.add(startupWindow);
    contextMenu.add(binBy2);
  }

  void popUpContextMenu(MouseEvent mouseEvent) {
    if (!target.isEnabled()) {
      return;
    }
    contextMenu.show(target.getComponent(), mouseEvent.getX(), mouseEvent.getY());
    contextMenu.setVisible(true);
  }

  private void addListeners() {
    if (run3dmod != null) {
      run3dmod.addActionListener(this);
    }
    startupWindow.addActionListener(this);
    binBy2.addActionListener(this);
  }

  public void actionPerformed(final ActionEvent event) {
    String actionCommand = event.getActionCommand();
    // MenuOptions holds the current menu choice.
    Run3dmodMenuOptions menuOptions = new Run3dmodMenuOptions();
    if (actionCommand.equals(startupWindow.getText())) {
      menuOptions.setStartupWindow(true);
    }
    else if (actionCommand.equals(binBy2.getText())) {
      menuOptions.setBinBy2(true);
    }
    else if (actionCommand.equals(run3dmod)) {
      menuOptions.setNoOptions(true);
    }
    if (target != null) {
      target.menuAction(menuOptions);
    }
  }
}
