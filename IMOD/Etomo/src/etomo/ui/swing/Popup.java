package etomo.ui.swing;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import etomo.EtomoDirector;
import etomo.logic.PopupTool;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.UITestFieldType;
import etomo.ui.FieldDisplayer;
import etomo.ui.UIComponent;
import etomo.util.Utilities;

/**
 * <p>Description: Opens a popup dialog.  Contains the result of the popup.  Can be used
 * in a headless run.  More configurable then UIHarness.</p>
 * <p/>
 * <p>Copyright: Copyright 2015 - 2020 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class Popup {
  private final Component component;
  private final String message;
  private final CheckBox checkBox;
  private final String title;
  private final String[] buttonLabels;
  private final int optionType;
  private final int messageType;
  private final boolean formatMessage;
  private final FieldDisplayer fieldDisplayer1;
  private final FieldDisplayer fieldDisplayer2;

  private Object value = null;

  private Popup(final UIComponent uiComponent, final int messageType, final String title,
    final String message, final String checkBoxTitle, final int optionType,
    final String[] buttonLabels, final boolean formatMessage,
    final FieldDisplayer fieldDisplayer1, final FieldDisplayer fieldDisplayer2) {
    this.messageType = messageType;
    this.formatMessage = formatMessage;
    this.fieldDisplayer1 = fieldDisplayer1;
    this.fieldDisplayer2 = fieldDisplayer2;
    if (uiComponent != null) {
      SwingComponent swingComponent = uiComponent.getUIComponent();
      component = swingComponent != null ? swingComponent.getComponent() : null;
    }
    else {
      component = null;
    }
    this.title = title;
    this.message = message;
    this.optionType = optionType;
    this.buttonLabels = buttonLabels;
    if (checkBoxTitle != null) {
      checkBox = new CheckBox(checkBoxTitle);
    }
    else {
      checkBox = null;
    }
  }

  public static Popup getErrorInstance(final UIComponent uiComponent, final String title,
    final String message, final FieldDisplayer fieldDisplayer1,
    final FieldDisplayer fieldDisplayer2) {
    return new Popup(uiComponent, JOptionPane.ERROR_MESSAGE, title, message, null,
      JOptionPane.DEFAULT_OPTION, null, true, fieldDisplayer1, fieldDisplayer2);
  }

  /**
   * Use this instance by passing it to UIHarness.openPopup, or calling open or log
   * @return
   */
  public static Popup getYesNoInstance(final UIComponent uiComponent, final String title,
    final String message, final String checkBoxMessage) {
    return new Popup(uiComponent, JOptionPane.QUESTION_MESSAGE, title, message,
      checkBoxMessage, JOptionPane.YES_NO_OPTION, null, true, null, null);
  }

  /**
   * Use this instance by passing it to UIHarness.openPopup, or calling open or log.
   * Uses custom buttons and can have a checkbox.
   * @return
   */
  public static Popup getCustomButtonInstance(final UIComponent uiComponent,
    final String title, final String message, final String checkBoxMessage,
    final String[] buttonLabels) {
    return new Popup(uiComponent, JOptionPane.QUESTION_MESSAGE, title, message,
      checkBoxMessage, JOptionPane.DEFAULT_OPTION, buttonLabels, true, null, null);
  }

  public static Popup getUnformattedErrorInstance(final UIComponent uiComponent,
    final String title, final String message) {
    return new Popup(uiComponent, JOptionPane.ERROR_MESSAGE, title, message, null,
      JOptionPane.DEFAULT_OPTION, null, false, null, null);
  }

  /**
   * Creates and pops-up the dialog.
   */
  public void open() {
    if (fieldDisplayer1 != null) {
      fieldDisplayer1.display();
    }
    if (fieldDisplayer2 != null) {
      fieldDisplayer2.display();
    }
    Object popupMessage;
    if (formatMessage) {
      // Wrap the message
      ArrayList<String> wrappedMessage = PopupTool.wrapMessage(message, null);
      Object[] messageArray;
      if (wrappedMessage == null || wrappedMessage.isEmpty()) {
        messageArray = null;
      }
      else if (wrappedMessage.size() == 1) {
        messageArray = new Object[] { wrappedMessage.get(0) };
      }
      else {
        messageArray = wrappedMessage.toArray();
      }
      popupMessage = messageArray;
    }
    else {
      popupMessage = message;
    }
    // Create the pane.
    if (checkBox != null) {
      popupMessage = new Object[] { popupMessage, Box.createRigidArea(FixedDim.x0_y10),
        checkBox.getComponent() };
    }
    JOptionPane pane = new JOptionPane(popupMessage, messageType, optionType,
      /*icon*/null, buttonLabels, /*Object initialValue*/null);
    // Build and display the dialog.
    JDialog dialog = pane.createDialog(component, title);
    if (component != null) {
      // Adjust the location of the dialog so it will be entirely visible.
      Point location = dialog.getLocation();
      location.y =
        PopupTool.adjustLocationY(location.y, component.getHeight(), dialog.getHeight());
      dialog.setLocation(location);
    }
    // Give the dialog a name for uitest
    String name = Utilities.convertLabelToName(title);
    pane.setName(name);
    printName(name);
    // Display dialog
    dialog.setVisible(true);
    dialog.dispose();
    // Get the selected value
    value = pane.getValue();
  }

  private final void printName(String name) {
    if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
      // print popup name/value pair
      StringBuilder builder = new StringBuilder(
        UITestFieldType.POPUP.toString() + AutodocTokenizer.SEPARATOR_CHAR + name + ' '
          + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
      // if there are options, then print a popup name/value pair
      if (optionType == JOptionPane.YES_NO_OPTION) {
        builder.append("Yes,No");
      }
      System.out.println(builder);
    }
  }

  void log() {
    System.err.println("Popup:" + message);
    System.err.println();
    System.err.flush();
    if (EtomoDirector.INSTANCE.isTestFailed()) {
      value = JOptionPane.YES_OPTION;
    }
  }

  public boolean isYes() {
    if (value == null) {
      return false;
    }
    return ((Integer) value).intValue() == JOptionPane.YES_OPTION;
  }

  public int getSelectedButtonIndex() {
    if (value == null || buttonLabels == null || !(value instanceof String)) {
      return -1;
    }
    for (int i = 0; i < buttonLabels.length; i++) {
      if (value.equals(buttonLabels[i])) {
        return i;
      }
    }
    return -1;
  }

  public boolean isCheckboxSelected() {
    return checkBox != null && checkBox.isSelected();
  }
}
