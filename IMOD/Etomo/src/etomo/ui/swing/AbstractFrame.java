package etomo.ui.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.logic.PopupTool;
import etomo.process.ProcessMessages;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.AxisID;
import etomo.type.FrameType;
import etomo.type.UITestFieldType;
import etomo.type.UserConfiguration;
import etomo.ui.UIComponent;
import etomo.util.Utilities;

/**
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright 2010 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
abstract class AbstractFrame extends JFrame implements UIComponent, SwingComponent {
  private static final boolean PRINT_NAMES =
    EtomoDirector.INSTANCE.getArguments().isPrintNames();
  private static final String OK = "OK";
  private static final String ETOMO_QUESTION = "Etomo question";
  private static final String YES = "Yes";
  private static final String NO = "No";
  private static final String CANCEL = "Cancel";
  private static final String[] YES_NO_LABEL_ARRAY = new String[] { YES, NO };
  private static final int NO_INDEX = 1;
  private static final String[] OK_LABEL_ARRAY = new String[] { OK };

  private boolean verbose = false;

  abstract void menuFileAction(ActionEvent actionEvent);

  abstract void menuToolsAction(ActionEvent actionEvent);

  abstract void menuViewAction(ActionEvent actionEvent);

  abstract void menuOptionsAction(ActionEvent actionEvent);

  abstract void menuHelpAction(ActionEvent actionEvent);

  abstract FrameType getFrameType();

  abstract void cancel();

  abstract void save(AxisID axisID);

  abstract void saveAs();

  abstract void close();

  public SwingComponent getUIComponent() {
    return this;
  }

  public Component getComponent() {
    return this;
  }

  public void setVisible(boolean visible) {
    if (visible) {
      UserConfiguration userConfiguration = EtomoDirector.INSTANCE.getUserConfiguration();
      if (!EtomoDirector.INSTANCE.getArguments().isIgnoreLoc()
        && userConfiguration.isLastLocationSet(getFrameType())) {
        setLocation(userConfiguration.getLastLocationX(getFrameType()),
          userConfiguration.getLastLocationY(getFrameType()));
      }
    }
    super.setVisible(visible);
  }

  final void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  AxisID getAxisID() {
    return AxisID.ONLY;
  }

  void pack(boolean force) {
    if (!force && !EtomoDirector.INSTANCE.getUserConfiguration().isAutoFit()) {
      setVisible(true);
    }
    else {
      Rectangle bounds = getBounds();
      bounds.height++;
      bounds.width++;
      setBounds(bounds);
      try {
        super.pack();
      }
      catch (NullPointerException e) {
        e.printStackTrace();
      }
    }
  }

  void repaint(AxisID axisID) {
    repaint();
  }

  void pack(AxisID axisID) {
    pack();
  }

  void pack(AxisID axisID, boolean force) {
    pack(force);
  }

  void repaintWindow() {
    repaintContainer(this);
    this.repaint();
  }

  private void repaintContainer(Container container) {
    Component[] comps = container.getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof Container) {
        Container cont = (Container) comps[i];
        repaintContainer(cont);
      }
      comps[i].repaint();
    }
  }

  void menuFileMRUListAction(ActionEvent event) {}

  /**
   * Open a message dialog
   *
   * @param message
   * @param title
   */
  void displayMessage(BaseManager manager, String message, String title, AxisID axisID) {
    openMessageDialog(manager, axisID, message, title);
  }

  void displayMessage(final BaseManager manager, final Component parentComponent,
    final String message, final String title, final AxisID axisID) {
    openMessageDialog(manager, parentComponent, axisID, message, title);
  }

  boolean displayYesNoMessage(final BaseManager manager, final Component parentComponent,
    final String message, final AxisID axisID) {
    return openYesNoDialog(manager, parentComponent, axisID, message);
  }

  void displayWarningMessage(final BaseManager manager, final Component parentComponent,
    final String message, final String title, final AxisID axisID) {
    openWarningMessageDialog(manager, parentComponent, axisID, message, title);
  }

  void displayMessage(final BaseManager manager, final Component parentComponent,
    final String[] message, final String title, final AxisID axisID) {
    openMessageDialog(manager, parentComponent, axisID, message, title);
  }

  /**
   * Open a message dialog
   *
   * @param message
   * @param title
   */
  void displayMessage(BaseManager manager, String message, String title) {
    openMessageDialog(manager, AxisID.ONLY, message, title);
  }

  void displayInfoMessage(BaseManager manager, final Component parentComponent,
    String message, String title, AxisID axisID) {
    openInfoMessageDialog(manager, parentComponent, axisID, message, title);
  }

  int displayYesNoCancelMessage(BaseManager manager, String message, AxisID axisID) {
    return openYesNoCancelDialog(manager, axisID, message);
  }

  boolean displayYesNoMessage(BaseManager manager, String[] message, AxisID axisID) {
    return openYesNoDialog(manager, axisID, message);
  }

  boolean displayYesNoMessage(BaseManager manager, String message, AxisID axisID) {
    return openYesNoDialog(manager, axisID, message);
  }

  boolean openYesNoDialogWithDefaultNo(final BaseManager manager, final String message,
    final String title, final AxisID axisID) {
    return openYesNoDialog(manager, axisID, message, title, NO_INDEX, true);
  }

  boolean displayDeleteMessage(BaseManager manager, String message[], AxisID axisID) {
    return openDeleteDialog(manager, axisID, message);
  }

  /**
   * Open a message dialog
   *
   * @param message
   * @param title
   */
  void displayMessage(BaseManager manager, String[] message, String title,
    AxisID axisID) {
    openMessageDialog(manager, axisID, message, title);
  }

  void displayErrorMessage(BaseManager manager, ProcessMessages processMessages,
    String title, AxisID axisID) {
    openErrorMessageDialog(manager, axisID, processMessages, title);
  }

  void displayMessage(BaseManager manager, ProcessMessages processMessages, String title,
    AxisID axisID) {
    openMessageDialog(manager, axisID, processMessages, title);
  }

  boolean displayYesNoWarningDialog(BaseManager manager, String message, AxisID axisID) {
    return openYesNoWarningDialog(manager, axisID, message);
  }

  void displayWarningMessage(BaseManager manager, ProcessMessages processMessages,
    String title, AxisID axisID) {
    openWarningMessageDialog(manager, axisID, processMessages, title);
  }

  /**
   * Open a message dialog with a wrapped message with the dataset appended.
   *
   * @param message
   * @param title
   */
  void openMessageDialog(BaseManager manager, AxisID axisID, String message,
    String title) {
    showOptionPane(manager, axisID, wrap(message), title, JOptionPane.ERROR_MESSAGE);
  }

  void openMessageDialog(final BaseManager manager, final Component parentComponent,
    final AxisID axisID, final String message, final String title) {
    showOptionPane(manager, parentComponent, axisID, wrap(message), title,
      JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Open a Yes or No question dialog
   *
   * @param message
   * @return
   */
  boolean openYesNoDialog(final BaseManager manager, final Component parentComponent,
    final AxisID axisID, final String message) {
    int result = showOptionConfirmPane(manager, parentComponent, axisID, wrap(message),
      ETOMO_QUESTION, JOptionPane.YES_NO_OPTION, YES_NO_LABEL_ARRAY);
    return result == JOptionPane.YES_OPTION;
  }

  void openWarningMessageDialog(final BaseManager manager,
    final Component parentcComponent, final AxisID axisID, final String message,
    final String title) {
    showOptionPane(manager, parentcComponent, axisID, wrap(message), title,
      JOptionPane.WARNING_MESSAGE);
  }

  void openMessageDialog(final BaseManager manager, final Component parentcComponent,
    final AxisID axisID, final String[] message, final String title) {
    showOptionPane(manager, parentcComponent, axisID, wrap(message), title,
      JOptionPane.ERROR_MESSAGE);
  }

  void openWarningMessageDialog(BaseManager manager, AxisID axisID,
    ProcessMessages processMessages, String title) {
    showOptionPane(manager, axisID, wrapWarning(processMessages), title,
      JOptionPane.ERROR_MESSAGE);
  }

  void openErrorMessageDialog(BaseManager manager, AxisID axisID,
    ProcessMessages processMessages, String title) {
    showOptionPane(manager, axisID, wrapError(processMessages), title,
      JOptionPane.ERROR_MESSAGE);
  }

  void openMessageDialog(BaseManager manager, AxisID axisID,
    ProcessMessages processMessages, String title) {
    showOptionPane(manager, axisID, wrapMessages(processMessages), title,
      JOptionPane.ERROR_MESSAGE);
  }

  boolean openYesNoWarningDialog(BaseManager manager, AxisID axisID, String message) {
    int result = showOptionPane(manager, axisID, wrap(message), "Etomo Warning",
      JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
      YES_NO_LABEL_ARRAY[NO_INDEX], false, YES_NO_LABEL_ARRAY);
    return result == 0;
  }

  /**
   * Open a Yes or No question dialog
   *
   * @param message
   * @return
   */
  boolean openYesNoDialog(BaseManager manager, AxisID axisID, String message) {
    int result = showOptionConfirmPane(manager, axisID, wrap(message), ETOMO_QUESTION,
      JOptionPane.YES_NO_OPTION, YES_NO_LABEL_ARRAY);
    return result == JOptionPane.YES_OPTION;
  }

  /**
   * Open a Yes or No question dialog.  Control which option is the default.
   *
   * @param manager
   * @param axisID
   * @param message
   * @param title
   * @return
   */
  boolean openYesNoDialog(final BaseManager manager, final AxisID axisID,
    final String message, final String title, final int initialValueIndex,
    final boolean overrideDefaultLabels) {
    int result = showOptionConfirmPane(manager, axisID, wrap(message), title,
      JOptionPane.YES_NO_OPTION, YES_NO_LABEL_ARRAY[initialValueIndex],
      overrideDefaultLabels, YES_NO_LABEL_ARRAY);
    return result == JOptionPane.YES_OPTION;
  }

  /**
   * Open a Yes or No question dialog
   *
   * @param message
   * @return
   */
  boolean openDeleteDialog(final BaseManager manager, final AxisID axisID,
    final String[] message) {
    int result = showOptionPane(manager, axisID, wrap(message), "Delete File?",
      JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, true,
      new String[] { "Delete", NO });
    return result == 0;
  }

  /**
   * Open a Yes or No question dialog
   *
   * @param message
   * @return
   */
  boolean openYesNoDialog(BaseManager manager, AxisID axisID, String[] message) {
    int result = showOptionConfirmPane(manager, axisID, wrap(message), ETOMO_QUESTION,
      JOptionPane.YES_NO_OPTION, YES_NO_LABEL_ARRAY);
    return result == JOptionPane.YES_OPTION;
  }

  void openInfoMessageDialog(BaseManager manager, final Component parentComponent,
    AxisID axisID, String message, String title) {
    showOptionPane(manager, parentComponent, axisID, wrap(message), title,
      JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Open a message dialog
   *
   * @param message
   * @param title
   */
  void openMessageDialog(BaseManager manager, AxisID axisID, String[] message,
    String title) {
    showOptionPane(manager, axisID, wrap(message), title, JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Open a Yes, No or Cancel question dialog
   *
   * @param message
   * @return int state of the users select
   */
  int openYesNoCancelDialog(BaseManager manager, AxisID axisID, String message) {
    return showOptionConfirmPane(manager, axisID, wrap(message), ETOMO_QUESTION,
      JOptionPane.YES_NO_CANCEL_OPTION, new String[] { YES, NO, CANCEL });
  }

  private int showOptionConfirmPane(BaseManager manager, AxisID axisID, String[] message,
    String title, int optionType, String[] optionStrings) {
    return showOptionPane(manager, axisID, message, title, optionType,
      JOptionPane.QUESTION_MESSAGE, null, false, optionStrings);
  }

  private int showOptionConfirmPane(final BaseManager manager, final AxisID axisID,
    final String[] message, final String title, final int optionType,
    final String initialValue, final boolean overrideDefaultLabels,
    final String[] optionStrings) {
    return showOptionPane(manager, axisID, message, title, optionType,
      JOptionPane.QUESTION_MESSAGE, initialValue, overrideDefaultLabels, optionStrings);
  }

  private final String[] wrapWarning(final ProcessMessages processMessages) {
    ArrayList messageArray = null;
    for (int i = 0; i < processMessages.size(ProcessMessages.MessageType.WARNING); i++) {
      messageArray = PopupTool.wrapMessage(
        processMessages.get(ProcessMessages.MessageType.WARNING, i), messageArray);
    }
    return toStringArray(messageArray);
  }

  /**
   * Add the current dataset name to the message and wrap
   *
   * @param message
   * @return
   */
  private final String[] wrapError(ProcessMessages processMessages) {
    ArrayList messageArray = null;
    for (int i = 0; i < processMessages.size(ProcessMessages.MessageType.ERROR); i++) {
      messageArray = PopupTool.wrapMessage(
        processMessages.get(ProcessMessages.MessageType.ERROR, i), messageArray);
    }
    return toStringArray(messageArray);
  }

  /**
   * Add the current dataset name to the message and wrap
   *
   * @param message
   * @return
   */
  private final String[] wrapMessages(ProcessMessages processMessages) {
    ArrayList messageArray = null;
    ProcessMessages.ListTypeIterator listTypeIterator =
      processMessages.listTypeIterator();
    while (listTypeIterator.hasNext()) {
      Iterator<String> iterator = processMessages.iterator(listTypeIterator.next());
      while (iterator.hasNext()) {
        messageArray = PopupTool.wrapMessage(iterator.next(), messageArray);
      }
      messageArray = PopupTool.linefeed(messageArray);
    }
    return toStringArray(messageArray);
  }

  /**
   * Add the current dataset name to the message and wrap
   *
   * @param message
   * @return
   */
  private String[] wrap(final String message) {
    ArrayList messageArray = PopupTool.wrapMessage(message, null);
    return toStringArray(messageArray);
  }

  /**
   * Add the current dataset name to the message and wrap
   *
   * @param message
   * @return
   */
  private String[] wrap(final String[] message) {
    ArrayList messageArray = null;
    for (int i = 0; i < message.length; i++) {
      messageArray = PopupTool.wrapMessage(message[i], messageArray);
    }
    return toStringArray(messageArray);
  }

  private final String[] toStringArray(ArrayList arrayList) {
    if (arrayList == null) {
      return null;
    }
    if (arrayList.size() == 1) {
      String[] returnArray = { (String) arrayList.get(0) };
      return returnArray;
    }
    return (String[]) arrayList.toArray(new String[arrayList.size()]);
  }

  private void showOptionPane(BaseManager manager, AxisID axisID, String[] message,
    String title, int messageType) {
    showOptionPane(manager, axisID, message, title, JOptionPane.DEFAULT_OPTION,
      messageType, null, false, OK_LABEL_ARRAY);
  }

  private void showOptionPane(final BaseManager manager, final Component parentComponent,
    final AxisID axisID, final String[] message, final String title,
    final int messageType) {
    showOptionPane(manager, parentComponent, axisID, message, title,
      JOptionPane.DEFAULT_OPTION, messageType, null, false, OK_LABEL_ARRAY);
  }

  private int showOptionConfirmPane(BaseManager manager, final Component parentComponent,
    AxisID axisID, String[] message, String title, int optionType,
    String[] optionStrings) {
    return showOptionPane(manager, parentComponent, axisID, message, title, optionType,
      JOptionPane.QUESTION_MESSAGE, null, false, optionStrings);
  }

  private int showOptionPane(final BaseManager manager, final AxisID axisID,
    final String[] message, final String title, final int optionType,
    final int messageType, final Object initialValue, final boolean overrideDefaultLabels,
    final String[] optionLabels) {
    int result = showOptionDialog(manager, axisID, this, message, title, optionType,
      messageType, null, initialValue, overrideDefaultLabels, optionLabels);
    return result;
  }

  private int showOptionPane(final BaseManager manager, final Component parentComponent,
    final AxisID axisID, final String[] message, final String title, final int optionType,
    final int messageType, final Object initialValue, final boolean overrideDefaultLabels,
    final String[] optionStrings) {
    int result = showOptionDialog(manager, axisID, parentComponent, message, title,
      optionType, messageType, null, initialValue, overrideDefaultLabels, optionStrings);
    return result;
  }

  /**
   * Shows all pop up message dialogs.  Pass in in BaseManager so that the
   *
   * @param uiComponent
   * @param message
   * @param title
   * @param optionType
   * @param messageType
   * @param icon
   * @param initialValue
   * @param overrideDefaultLabels
   * @param optionLabels
   * @param manager
   * @return
   * @throws HeadlessException
   */
  private int showOptionDialog(final BaseManager manager, final AxisID axisID,
    final Component parentComponent, final String[] message, final String title,
    final int optionType, int messageType, final Icon icon, final Object initialValue,
    final boolean overrideDefaults, final String[] options) throws HeadlessException {
    if (manager != null) {
      manager.logMessage(message, title, axisID);
    }
    else {
      System.err
        .println(Utilities.getDateTimeStamp() + "\n" + title + " - " + axisID + " axis:");
      for (int i = 0; i < message.length; i++) {
        System.err.println(message[i]);
      }
    }
    JOptionPane pane = null;
    // Change the message icon to match the message.
    if (icon == null && messageType == JOptionPane.ERROR_MESSAGE) {
      boolean errorMessage = false;
      boolean warningMessage = false;
      if (title != null) {
        String lcTitle = title.toLowerCase();
        // Change the icon if the message contains "warning", and does not contain
        // "error".
        if (lcTitle.indexOf("error") != -1) {
          errorMessage = true;
        }
        else if (lcTitle.indexOf("warning") != -1) {
          warningMessage = true;
        }
        if (!errorMessage && message != null) {
          // Check the first three lines of the message.
          for (int i = 0; i < Math.min(message.length, 3); i++) {
            String lcMessage = message[i].toLowerCase();
            if (lcMessage.indexOf("error:") != -1) {
              errorMessage = true;
              break;
            }
            if (lcMessage.indexOf("warning:") != -1) {
              warningMessage = true;
            }
          }
        }
        if (!errorMessage && warningMessage) {
          messageType = JOptionPane.WARNING_MESSAGE;
        }
      }
    }
    // Decide whether to pass an array of button labels (to override the defaults) or
    // null.
    if (overrideDefaults) {
      pane =
        new JOptionPane(message, messageType, optionType, icon, options, initialValue);
    }
    else {
      pane = new JOptionPane(message, messageType, optionType, icon, null, initialValue);
    }
    pane.setInitialValue(initialValue);
    pane.setComponentOrientation(
      ((parentComponent == null) ? JOptionPane.getRootFrame() : parentComponent)
        .getComponentOrientation());

    JDialog dialog = pane.createDialog(parentComponent, title);
    // A popup with a parent component and no axis is most likely connected to a field.
    if (parentComponent != null && axisID == null) {
      Point location = dialog.getLocation();
      location.y = PopupTool.adjustLocationY(location.y, parentComponent.getHeight(),
        dialog.getHeight());
      dialog.setLocation(location);
    }
    pane.selectInitialValue();
    String name = Utilities.convertLabelToName(title);
    pane.setName(name);
    printName(name, options, title, message);
    dialog.setVisible(true);
    dialog.dispose();

    Object selectedValue = pane.getValue();

    if (selectedValue == null) {
      return JOptionPane.CLOSED_OPTION;
    }
    // If a null array of options was passed, pane returns an integer.
    if (!overrideDefaults || options == null) {
      if (selectedValue instanceof Integer) {
        return ((Integer) selectedValue).intValue();
      }
      return JOptionPane.CLOSED_OPTION;
    }
    // If an array of options was passed, pane returns the label of the button selected.
    for (int counter = 0, maxCounter = options.length; counter < maxCounter; counter++) {
      if (options[counter].equals(selectedValue)) {
        return counter;
      }
    }
    return JOptionPane.CLOSED_OPTION;
  }

  private synchronized final void printName(String name, String[] options, String title,
    String[] message) {
    if (PRINT_NAMES) {
      // print popup name/value pair
      StringBuffer buffer = new StringBuffer(
        UITestFieldType.POPUP.toString() + AutodocTokenizer.SEPARATOR_CHAR + name + ' '
          + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
      // if there are options, then print a popup name/value pair
      if (options != null && options.length > 0) {
        boolean appended = false;
        for (int i = 1; i < options.length; i++) {
          if (options[i] instanceof String) {
            if (appended) {
              buffer.append(',');
            }
            buffer.append(options[i]);
            appended = true;
          }
        }
        System.out.println(buffer);
      }
    }
    if (verbose) {
      // if verbose then print the popup title and message
      System.err.println("Popup:");
      System.err.println(title);
      if (message != null) {
        for (int i = 0; i < message.length; i++) {
          System.err.println(message[i]);
        }
      }
    }
  }
}
