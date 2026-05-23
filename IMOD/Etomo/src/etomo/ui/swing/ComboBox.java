package etomo.ui.swing;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import etomo.Arguments.DebugLevel;
import etomo.EtomoDirector;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.UITestFieldType;
import etomo.util.Utilities;

/**
* <p>Description: </p>
* 
 * <p>Copyright: Copyright 2008 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
* 
* <p> $Log$
* <p> Revision 1.2  2010/12/05 05:01:29  sueh
* <p> bug# 1416 Added ComboBox(String).
* <p>
* <p> Revision 1.1  2010/11/13 16:07:35  sueh
* <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
* <p>
* <p> Revision 3.2  2009/11/20 17:02:34  sueh
* <p> bug# 1282 Added prefixes to all of the field names, so that the fields that
* <p> are actually abstract buttons (radio buttons, etc) won't be activated by a
* <p> "bn." field command.
* <p>
* <p> Revision 3.1  2009/09/01 03:18:25  sueh
* <p> bug# 1222
* <p> </p>
*/
final class ComboBox {
  private final JComboBox comboBox;
  private final JLabel label;
  private final JPanel pnlRoot;
  private final boolean textEntryPolicy;// presidence:1
  private final boolean emptyChoice;// presidence:2

  private boolean checkpointed = false;
  private int checkpointIndex = -1;
  private DebugLevel debug = EtomoDirector.INSTANCE.getArguments().getDebugLevel();
  private boolean enabledPolicy = true;
  private boolean enabled = true;
  private boolean editable = true;
  private boolean placeholder = false;// presidence:3
  private String emptyLabel = null;
  private String noSelectionLabel = null;

  private ComboBox(final String name, final boolean labeled,
    final boolean textEntryPolicy, final boolean emptyChoice) {
    this.textEntryPolicy = textEntryPolicy;
    comboBox = new JComboBox();
    setName(name);
    if (labeled) {
      label = new JLabel(name);
      pnlRoot = new JPanel();
    }
    else {
      label = null;
      pnlRoot = null;
    }
    if (textEntryPolicy) {
      this.emptyChoice = false;
    }
    else {
      this.emptyChoice = emptyChoice;
      if (emptyChoice) {
        comboBox.addItem(null);
      }
    }
    updateDisplay();
  }

  static ComboBox getInstance(final String name) {
    ComboBox instance = new ComboBox(name, true, false, false);
    instance.createPanel();
    return instance;
  }

  static ComboBox getUnlabeledInstance(final String name) {
    ComboBox instance = new ComboBox(name, false, false, false);
    instance.createPanel();
    return instance;
  }

  static ComboBox getEditableInstance(final String name) {
    ComboBox instance = new ComboBox(name, true, true, false);
    instance.createPanel();
    return instance;
  }

  static ComboBox getUnlabeledEmptyChoiceInstance(final String name) {
    ComboBox instance = new ComboBox(name, false, false, true);
    instance.createPanel();
    return instance;
  }

  static ComboBox getEmptyChoiceInstance(final String name) {
    ComboBox instance = new ComboBox(name, true, false, true);
    instance.createPanel();
    return instance;
  }

  private void createPanel() {
    if (pnlRoot != null) {
      pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.X_AXIS));
      pnlRoot.add(Box.createRigidArea(FixedDim.x2_y0));
      pnlRoot.add(label);
      pnlRoot.add(Box.createRigidArea(FixedDim.x3_y0));
      pnlRoot.add(comboBox);
      pnlRoot.add(Box.createRigidArea(FixedDim.x2_y0));
    }
  }

  void addActionListener(final ActionListener listener) {
    comboBox.addActionListener(listener);
  }

  void addFocusListener(final FocusListener listener) {
    comboBox.addFocusListener(listener);
  }

  /**
   * Adds a placeholder item at the beginning of the combobox.  Can be called at any time.
   * This function has no effect if textEntryPolicy or emptyChoice are on.
   * Calling it more then once replaces the labels.  Calling it with two null labels turns
   * off placeholder, or has no effect if placeholder is already off.
   * @param emptyLabel
   * @param noSelectionLabel
   */
  void setPlaceholder(final String emptyLabel, final String noSelectionLabel) {
    if (textEntryPolicy || emptyChoice) {
      // these take presidence
      return;
    }
    this.emptyLabel = emptyLabel;
    this.noSelectionLabel = noSelectionLabel;
    boolean noLabel = emptyLabel == null && noSelectionLabel == null;
    if (!placeholder && noLabel)
      // nothing to do
      return;
    // Check empty before changing placeholder boolean.
    boolean empty = isEmpty();
    if (empty) {
      // Empty, but may contain an out-of-date placeholder
      comboBox.removeAllItems();
    }
    if (placeholder && noLabel) {
      // turn off placeholder
      placeholder = false;
    }
    else {
      placeholder = true;
    }
    if (placeholder && empty) {
      comboBox.addItem(emptyLabel);
    }
    if (!empty) {
      // Temporarily store existing items so a placeholder can be added, changed, or
      // removed.
      int count = comboBox.getItemCount();
      Object[] items = new Object[count];
      for (int i = 0; i < count; i++) {
        items[i] = comboBox.getItemAt(i);
      }
      comboBox.removeAllItems();
      if (placeholder) {
        comboBox.addItem(noSelectionLabel);
      }
      for (int i = 0; i < items.length; i++) {
        comboBox.addItem(items[i]);
      }
    }
  }

  void addItem(final Object input) {
    if (placeholder && isEmpty()) {
      // Update placeholder
      comboBox.removeAllItems();
      comboBox.addItem(noSelectionLabel);
    }
    comboBox.addItem(input);
    updateDisplay();
  }

  void setFieldHighlight() {
    label.setForeground(Colors.FIELD_HIGHLIGHT);
    comboBox.setForeground(Colors.FIELD_HIGHLIGHT);
    comboBox.setBorder(BorderFactory.createLineBorder(Colors.FIELD_HIGHLIGHT));
  }

  String getActionCommand() {
    return comboBox.getActionCommand();
  }

  Component getComponent() {
    if (pnlRoot != null) {
      return pnlRoot;
    }
    return comboBox;
  }

  /**
   * Returns the selected index.  If emptyChoice or placeholder is on, then the index is
   * adjusted so that it starts from zero.  If the placeholder was selected it returns -1.
   * @return
   */
  int getSelectedIndex() {
    int index = comboBox.getSelectedIndex();
    if ((emptyChoice || placeholder) && index > -1) {
      return index - 1;
    }
    return index;
  }

  Object getSelectedItem() {
    return comboBox.getSelectedItem();
  }

  void removeAllItems() {
    comboBox.removeAllItems();
    if (emptyChoice) {
      comboBox.addItem(null);
    }
    else if (placeholder) {
      comboBox.addItem(emptyLabel);
    }
    updateDisplay();
  }

  /**
   * Set combo box display based on the settings and what is in the pulldown list.
   * Enabled:
   * - should not be enabled if the enabled policy is false.
   * - should not be enabled if it is empty - unless the text entry policy is true.
   * - When the text entry policy is false, makeing the field editable/ineditable is done by
   *   enabling/disabling it.  So in this case substitute (enabled && editable) for enabled.
   * Editable:
   * - Set to editable when editable, and the text entry policy is true.
   */
  private void updateDisplay() {
    if (!enabledPolicy || (isEmpty() && !textEntryPolicy)) {
      comboBox.setEnabled(false);
    }
    else if (!textEntryPolicy) {
      comboBox.setEnabled(enabled && editable);
    }
    else {
      comboBox.setEnabled(enabled);
    }
    comboBox.setEditable(textEntryPolicy && editable);
  }

  /**
   * Takes the placeholder into account.
   * @return
   */
  boolean isEmpty() {
    return comboBox.getItemCount() <= ((emptyChoice || placeholder) ? 1 : 0);
  }

  boolean isEnabled() {
    return enabled;
  }

  void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    if (label != null) {
      label.setEnabled(enabled);
    }
    updateDisplay();
  }

  void setEditable(final boolean editable) {
    this.editable = editable;
    updateDisplay();
  }

  /**
   * Sets the enabled policy.  The enabled policy default is true.  Makes sure that
   * combobox is not in an illegal state.
   * @param input
   */
  void setEnabledPolicy(final boolean input) {
    enabledPolicy = input;
    updateDisplay();
  }

  String getLabel() {
    return label.getText();
  }

  void setDebug(final DebugLevel input) {
    debug = input;
  }

  void setName(String text) {
    String name = Utilities.convertLabelToName(text);
    comboBox.setName(
      UITestFieldType.COMBO_BOX.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
      System.out
        .println(comboBox.getName() + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
    }
  }

  /**
   * Saves the current selected index as the checkpoint.
   */
  void checkpoint() {
    checkpointed = true;
    checkpointIndex = getSelectedIndex();
  }

  /**
   * 
   * @param alwaysCheck - check for difference even when the field is disables or invisible
   * @return
   */
  boolean isDifferentFromCheckpoint(final boolean alwaysCheck) {
    if (!alwaysCheck && (!isEnabled() || !isVisible())) {
      return false;
    }
    if (!checkpointed) {
      return true;
    }
    return checkpointIndex != getSelectedIndex();
  }

  public boolean isVisible() {
    return comboBox.isVisible();
  }

  /**
   * Selects an item.  If emptyChoice or placeholder is on, it adjusts for it, so that a zero index
   * refers to first non-empty choice.
   * @param index
   */
  void setSelectedIndex(int index) {
    if (emptyChoice || placeholder) {
      index++;
    }
    comboBox.setSelectedIndex(index);
  }

  /**
   * Turns off selection unless emptyChoice or placeholder are set.  If so sets to either the
   * empty choice or the placeholder.
   */
  void unselect() {
    if (emptyChoice || placeholder) {
      comboBox.setSelectedIndex(0);
    }
    else {
      comboBox.setSelectedItem(null);
    }
  }

  void setToolTipText(final String tooltip) {
    if (label != null) {
      label.setToolTipText(tooltip);
    }
    comboBox.setToolTipText(tooltip);
  }
}
