package etomo.ui.swing;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;

/**
 * <p>Description:  Panel that holds paging buttons.  May also receive display
 * Components and define key actions on them.  Passes button and key action
 * paging commands on to Viewport.</p>
 * 
 * <p>Copyright: Copyright 2008 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.3  2008/10/07 16:43:13  sueh
 * <p> bug# 1113 Improved named - changed parent... to focusableParent....
 * <p>
 * <p> Revision 1.2  2008/10/06 22:40:03  sueh
 * <p> bug# 1113 Changed tooltips to reflect whether hotkeys are used.
 * <p>
 * <p> Revision 1.1  2008/09/30 22:00:03  sueh
 * <p> bug# 1113 Added a panel to hold paging buttons and set hotkeys for
 * <p> paging.
 * <p> </p>
 */

final class PagingPanel {
  private final JPanel pnlRoot = new JPanel();
  private final SingleLineButton btnHome = new SingleLineButton();
  private final SingleLineButton btnPageUp = new SingleLineButton();
  private final SingleLineButton btnUp = new SingleLineButton();
  private final SingleLineButton btnDown = new SingleLineButton();
  private final SingleLineButton btnPageDown = new SingleLineButton();
  private final SingleLineButton btnEnd = new SingleLineButton();

  private final Viewport viewport;
  private final String uniqueKey;

  private PagingPanel(final Viewport viewport, final String uniqueKey) {
    this.viewport = viewport;
    this.uniqueKey = uniqueKey;
  }

  /**
   * Get PagingPanel instance with hotkey connections to 0 to three
   * JComponents.  The JComponents can be the panel that the table has been
   * placed on.  The uniqueKey uniquely identifies the table.  This is necessary
   * when two tables appear on the same JPanel, so that separate actions are
   * stored (no sure if this works yet).
   * @param viewer
   * @param uniqueKey
   * @return
   */
  static PagingPanel getInstance(final Viewport viewport, final String uniqueKey) {
    PagingPanel instance = new PagingPanel(viewport, uniqueKey);
    instance.createPanel();
    instance.addListeners();
    instance.addTooltips();
    return instance;
  }

  private void createPanel() {
    // init
    setupButton(btnHome, "home.png");
    setupButton(btnPageUp, "pageUp.png");
    setupButton(btnUp, "up.png");
    setupButton(btnDown, "down.png");
    setupButton(btnPageDown, "pageDown.png");
    setupButton(btnEnd, "end.png");
    // root
    pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
    pnlRoot.add(btnHome.getComponent());
    pnlRoot.add(btnPageUp.getComponent());
    pnlRoot.add(btnUp.getComponent());
    pnlRoot.add(Box.createVerticalGlue());
    pnlRoot.add(btnDown.getComponent());
    pnlRoot.add(btnPageDown.getComponent());
    pnlRoot.add(btnEnd.getComponent());
  }

  private void setupButton(final SingleLineButton button, final String iconFile) {
    button.setManualName();
    button.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    button.setIcon(new ImageIcon(ClassLoader.getSystemResource("images/" + iconFile)));
    Dimension size = button.getPreferredSize();
    if (size.width < size.height) {
      size.width = size.height;
    }
    button.setSize(size);
  }

  private void addAction(final InputMap[] inputMapsForParent, final ActionMap actionMap,
    final int keyStroke, String key, final AbstractAction action) {
    if (inputMapsForParent != null) {
      key = uniqueKey + key;
      KeyStroke keystroke = KeyStroke.getKeyStroke(keyStroke, 0);
      for (int i = 0; i < inputMapsForParent.length; i++) {
        if (inputMapsForParent[i] != null) {
          inputMapsForParent[i].put(keystroke, key);
        }
      }
      actionMap.put(key, action);
    }
  }

  private void addListeners() {
    JComponent[] focusableParents = viewport.getFocusableParents();
    if (focusableParents == null) {
      return;
    }
    int focusTypes = 3;
    InputMap[][] inputMaps = new InputMap[focusableParents.length][focusTypes];
    ActionMap[] actionMaps = new ActionMap[focusableParents.length];
    AbstractAction homeAction = new PagingPanelHomeAction(viewport);
    AbstractAction pageUpAction = new PagingPanelPageUpAction(viewport);
    AbstractAction upAction = new PagingPanelUpAction(viewport);
    AbstractAction downAction = new PagingPanelDownAction(viewport);
    AbstractAction pageDownAction = new PagingPanelPageDownAction(viewport);
    AbstractAction endAction = new PagingPanelEndAction(viewport);
    for (int i = 0; i < focusableParents.length; i++) {
      if (focusableParents[i] == null) {
        continue;
      }
      focusableParents[i].setFocusable(true);
      inputMaps[i][0] = focusableParents[i].getInputMap(JComponent.WHEN_FOCUSED);
      inputMaps[i][1] =
        focusableParents[i].getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
      inputMaps[i][2] =
        focusableParents[i].getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
      actionMaps[i] = focusableParents[i].getActionMap();
      addAction(inputMaps[i], actionMaps[i], KeyEvent.VK_HOME, "HOME", homeAction);
      addAction(inputMaps[i], actionMaps[i], KeyEvent.VK_PAGE_UP, "PAGE_UP",
        pageUpAction);
      addAction(inputMaps[i], actionMaps[i], KeyEvent.VK_UP, "UP", upAction);
      addAction(inputMaps[i], actionMaps[i], KeyEvent.VK_DOWN, "DOWN", downAction);
      addAction(inputMaps[i], actionMaps[i], KeyEvent.VK_PAGE_DOWN, "PAGE_DOWN",
        pageDownAction);
      addAction(inputMaps[i], actionMaps[i], KeyEvent.VK_END, "END", endAction);
    }
    // add listeners
    btnHome.addActionListener(homeAction);
    btnPageUp.addActionListener(pageUpAction);
    btnUp.addActionListener(upAction);
    btnDown.addActionListener(downAction);
    btnPageDown.addActionListener(pageDownAction);
    btnEnd.addActionListener(endAction);
  }

  private void addTooltips() {
    boolean hotkeys = false;
    JComponent[] focusableParents = viewport.getFocusableParents();
    if (focusableParents != null && focusableParents.length > 0) {
      hotkeys = true;
    }
    btnHome.setToolTipText("Top of table");
    btnPageUp.setToolTipText("Page up" + (hotkeys ? " [Page Up]" : ""));
    btnUp.setToolTipText("Up one line" + (hotkeys ? " [Up_Arrow]" : ""));
    btnDown.setToolTipText("Down one line" + (hotkeys ? " [Down_Arrow]" : ""));
    btnPageDown.setToolTipText("Page up" + (hotkeys ? " [Page Down]" : ""));
    btnEnd.setToolTipText("Bottom of table");
  }

  Container getContainer() {
    return pnlRoot;
  }

  void setUpEnabled(final boolean enabled) {
    btnHome.setEnabled(enabled);
    btnPageUp.setEnabled(enabled);
    btnUp.setEnabled(enabled);
  }

  void setDownEnabled(final boolean enabled) {
    btnEnd.setEnabled(enabled);
    btnPageDown.setEnabled(enabled);
    btnDown.setEnabled(enabled);
  }

  void setVisible(final boolean visible) {
    pnlRoot.setVisible(visible);
  }

  private static final class PagingPanelHomeAction extends AbstractAction {
    private final Viewport viewport;

    private PagingPanelHomeAction(final Viewport viewport) {
      this.viewport = viewport;
    }

    public void actionPerformed(final ActionEvent event) {
      viewport.homeButtonAction();
    }
  }

  private static final class PagingPanelPageUpAction extends AbstractAction {
    private final Viewport viewport;

    private PagingPanelPageUpAction(final Viewport viewport) {
      this.viewport = viewport;
    }

    public void actionPerformed(final ActionEvent event) {
      viewport.pageUpButtonAction();
    }
  }

  private static final class PagingPanelUpAction extends AbstractAction {
    private final Viewport viewport;

    private PagingPanelUpAction(final Viewport viewport) {
      this.viewport = viewport;
    }

    public void actionPerformed(final ActionEvent event) {
      viewport.upButtonAction();
    }
  }

  private static final class PagingPanelDownAction extends AbstractAction {
    private final Viewport viewport;

    private PagingPanelDownAction(final Viewport viewport) {
      this.viewport = viewport;
    }

    public void actionPerformed(final ActionEvent event) {
      viewport.downButtonAction();
    }
  }

  private static final class PagingPanelPageDownAction extends AbstractAction {
    private final Viewport viewport;

    private PagingPanelPageDownAction(final Viewport viewport) {
      this.viewport = viewport;
    }

    public void actionPerformed(final ActionEvent event) {
      viewport.pageDownButtonAction();
    }
  }

  private static final class PagingPanelEndAction extends AbstractAction {
    private final Viewport viewport;

    private PagingPanelEndAction(final Viewport viewport) {
      this.viewport = viewport;
    }

    public void actionPerformed(final ActionEvent event) {
      viewport.endButtonAction();
    }
  }
}
