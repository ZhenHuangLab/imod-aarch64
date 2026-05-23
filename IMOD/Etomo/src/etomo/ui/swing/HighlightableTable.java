package etomo.ui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * <p>Description: An abstract class that allows the table highlight to be moved using
 * keys.  Adds to the action maps of up to three components.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
abstract class HighlightableTable implements Highlightable {
  private final String uniqueKey;

  abstract void highlightUpActionPerformed();

  abstract void highlightDownActionPerformed();

  abstract JComponent[] getFocusableParents();

  HighlightableTable(final String uniqueKey) {
    this.uniqueKey = uniqueKey;
  }

  void initHighlightHotkeys() {
    addListeners();
  }

  /**
   * Optionally add a key stroke action for one parent.
   * @param inputMap
   * @param actionMap
   * @param keyStroke
   * @param key
   * @param action
   */
  private void addAction(final InputMap[] inputMapsForParent, final ActionMap actionMap,
    final int keyStroke, String key, final AbstractAction action) {
    if (inputMapsForParent != null) {
      key = uniqueKey + key;
      KeyStroke keystroke = KeyStroke.getKeyStroke(keyStroke, InputEvent.ALT_MASK);
      for (int i = 0; i < inputMapsForParent.length; i++) {
        if (inputMapsForParent[i] != null) {
          inputMapsForParent[i].put(keystroke, key);
        }
      }
      actionMap.put(key, action);
    }
  }

  private void addListeners() {
    JComponent[] focusableParents = getFocusableParents();
    if (focusableParents == null) {
      return;
    }
    int focusTypes = 3;
    InputMap[][] inputMaps = new InputMap[focusableParents.length][focusTypes];
    ActionMap[] actionMaps = new ActionMap[focusableParents.length];
    AbstractAction upAction = new HighlightUpAction(this);
    AbstractAction downAction = new HighlightDownAction(this);
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
      addAction(inputMaps[i], actionMaps[i], KeyEvent.VK_UP, "ALT_UP", upAction);
      addAction(inputMaps[i], actionMaps[i], KeyEvent.VK_DOWN, "ALT_DOWN", downAction);
    }
  }

  private class HighlightUpAction extends AbstractAction {
    private final HighlightableTable table;

    HighlightUpAction(final HighlightableTable table) {
      this.table = table;
    }

    public void actionPerformed(final ActionEvent actionEvent) {
      table.highlightUpActionPerformed();
    }
  }

  private class HighlightDownAction extends AbstractAction {
    private final HighlightableTable table;

    HighlightDownAction(final HighlightableTable table) {
      this.table = table;
    }

    public void actionPerformed(final ActionEvent actionEvent) {
      table.highlightDownActionPerformed();
    }
  }
}
