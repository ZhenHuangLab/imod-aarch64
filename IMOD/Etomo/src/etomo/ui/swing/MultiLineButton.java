package etomo.ui.swing;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.border.Border;

import etomo.EtomoDirector;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.BaseScreenState;
import etomo.type.DialogType;
import etomo.type.ProcessResult;
import etomo.type.ProcessResultDisplay;
import etomo.type.ProcessResultDisplayState;
import etomo.type.UITestFieldType;
import etomo.util.Utilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.lang.String;

/**
 * <p>Description: Contains either a JButton or a JToggleButton.  
 * <p> Construct with the contructor to get a JButton.  Construct with a
 * getToggleButtonInstance call to get a JToggleButton.
 * <p>This class wraps the button label in html so that the text will wrap.
 * This prevents the text from changing color when the button is disabled, so
 * this class controls the text color on enable/disable.
 * 
 * <p>This class can only create standard-sized buttons with the ability to wrap longer
 * labels. </p>
 *
 * <p>Copyright: Copyright 2002 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 1.4  2011/04/04 17:21:11  sueh
 * <p> bug# 1416 Made getUnformattedLabel public.
 * <p>
 * <p> Revision 1.3  2011/02/22 18:16:04  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.2  2010/12/05 05:12:31  sueh
 * <p> bug# 1416 Added getUnformattedLabel.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 3.47  2010/07/02 20:17:59  sueh
 * <p> bug# 1387 System information should always be printed during a failure.
 * <p> Removed it from here.
 * <p>
 * <p> Revision 3.46  2010/07/02 20:04:16  sueh
 * <p> bug# 1387 Debugging all the time
 * <p>
 * <p> Revision 3.45  2010/07/01 01:30:54  sueh
 * <p> bug# 1387 Added more info to debug print.
 * <p>
 * <p> Revision 3.44  2010/06/30 21:55:05  sueh
 * <p> bug# 1387 Added Java info to debug print.
 * <p>
 * <p> Revision 3.43  2010/06/30 21:10:14  sueh
 * <p> bug# 1387 Fixed debug instance.
 * <p>
 * <p> Revision 3.42  2010/06/30 21:04:12  sueh
 * <p> bug# 1387 Added getDebugInstance.
 * <p>
 * <p> Revision 3.41  2009/11/20 17:28:22  sueh
 * <p> bug# 1282 Added prefixes to all of the field names, so that the fields that
 * <p> are actually abstract buttons (radio buttons, etc) won't be activated by a
 * <p> "bn." field command.
 * <p>
 * <p> Revision 3.40  2009/09/01 03:18:25  sueh
 * <p> bug# 1222
 * <p>
 * <p> Revision 3.39  2009/04/13 22:57:09  sueh
 * <p> Removed newstuff.
 * <p>
 * <p> Revision 3.38  2009/02/27 03:54:11  sueh
 * <p> bug# 1172 Added experimental automation recording border color
 * <p> (newstuff only).
 * <p>
 * <p> Revision 3.37  2009/01/20 20:15:58  sueh
 * <p> bug# 1102 Changed UITestField to UITestFieldType.
 * <p>
 * <p> Revision 3.36  2008/11/20 01:46:26  sueh
 * <p> bug# 1147 Added isVisible.
 * <p>
 * <p> Revision 3.35  2008/09/30 21:59:13  sueh
 * <p> bug# 1113 Added setIncon.
 * <p>
 * <p> Revision 3.34  2008/05/30 22:32:19  sueh
 * <p> bug# 1102 Isolating the etomo.uitest package so it is not need for
 * <p> running EtomoDirector.
 * <p>
 * <p> Revision 3.33  2008/05/30 21:32:45  sueh
 * <p> bug# 1102 Moved uitest classes to etomo.uitest.
 * <p>
 * <p> Revision 3.32  2008/05/03 00:51:16  sueh
 * <p> bug# 847 Reformatted.
 * <p>
 * <p> Revision 3.31  2007/12/26 22:25:32  sueh
 * <p> bug# 1052 Moved argument handling from EtomoDirector to a separate class.
 * <p>
 * <p> Revision 3.30  2007/11/06 19:53:05  sueh
 * <p> bug# 1047 Simplified toString().
 * <p>
 * <p> Revision 3.29  2007/09/10 20:43:31  sueh
 * <p> bug# 925 Removed lazy initialization for ProcessResultDisplay so initialized is
 * <p> no longer needed.  In setScreenState, calling button.setSelected() immediately
 * <p>
 * <p> Revision 3.28  2007/09/07 00:27:38  sueh
 * <p> bug# 989 Using a public INSTANCE to refer to the EtomoDirector singleton
 * <p> instead of getInstance and createInstance.
 * <p>
 * <p> Revision 3.27  2007/03/01 01:39:53  sueh
 * <p> bug# 964 Added functions getWidth, getBorder, and getHeight.
 * <p>
 * <p> Revision 3.26  2007/02/09 00:50:44  sueh
 * <p> bug# 962 Made TooltipFormatter a singleton and moved its use to low-level ui
 * <p> classes.
 * <p>
 * <p> Revision 3.25  2007/02/05 23:40:00  sueh
 * <p> bug# 962 Added setHighlight.
 * <p>
 * <p> Revision 3.24  2006/07/26 16:40:45  sueh
 * <p> bug# 868 Added msg(ProcessResult)
 * <p>
 * <p> Revision 3.23  2006/07/20 17:20:36  sueh
 * <p> bug# 848 Made UIParameters a singleton.
 * <p>
 * <p> Revision 3.22  2006/05/01 21:18:45  sueh
 * <p> bug# 854
 * <p>
 * <p> Revision 3.21  2006/04/28 21:02:15  sueh
 * <p> bug# 787 Added getButton and getToggleButtonInstance.
 * <p>
 * <p> Revision 3.20  2006/04/25 19:17:01  sueh
 * <p> bug# 787 Added UITestField, an enum style class which contains the
 * <p> fields found in uitestaxis.adoc files.
 * <p>
 * <p> Revision 3.19  2006/04/06 20:17:14  sueh
 * <p> bug# 808 Moved the function convertLabelToName from UIUtilities to
 * <p> util.Utilities.
 * <p>
 * <p> Revision 3.18  2006/03/28 17:03:31  sueh
 * <p> bug# 437 Added setOriginalProcessResultDisplayState to be used by
 * <p> setButtonState.
 * <p>
 * <p> Revision 3.17  2006/03/28 00:54:29  sueh
 * <p> bug# 437 Save dialogType and do a lazy creation of stateKey.  This allows
 * <p> more flexiblility in setting the name.  Change getButtonStateKey(DialogType)
 * <p> to createButtonStateKey(DialogType).
 * <p>
 * <p> Revision 3.16  2006/02/06 21:21:04  sueh
 * <p> bug# 521 ProcessResultDisplayState:  changed following display to
 * <p> dependent display.  Added dependecy index and initialized.
 * <p>
 * <p> Revision 3.15  2006/01/31 20:59:14  sueh
 * <p> bug# 521 Added failureDisplayList, successDisplayList, and
 * <p> followingDisplayList to change the state of other displays when the
 * <p> process associated with the current instance succeeds or fails.
 * <p> Added BaseScreenState so that buttons on dialogs not current
 * <p> displayed could change their state.  Without the ability to change the
 * <p> screen state setting, they're old state would be reloaded when the
 * <p> dialog was redisplayed.
 * <p>
 * <p> Revision 3.14  2006/01/26 22:05:23  sueh
 * <p> bug# 401 Turn ProcessResultDisplay into an interface.  Place the
 * <p> functionality into ProcessResultDisplayState.  This allows a greater
 * <p> variety of classes to be ProcessResultDisplay's.
 * <p>
 * <p> Revision 3.13  2006/01/20 21:11:06  sueh
 * <p> bug# 401 Allow MultiLineButton to get and set its state and build a key to
 * <p> be used for saving to a properties file.  Extending ProcessResultDisplay.
 * <p>
 * <p> Revision 3.12  2006/01/12 22:12:22  sueh
 * <p> bug# 401 added an action listener to keep toggle buttons selected every
 * <p> time they are pressed.  Added isToggleButton().
 * <p> bug# 798 Reducing the visibility and inheritability of ui classes.
 * <p>
 * <p> Revision 3.11  2006/01/12 17:11:48  sueh
 * <p> bug# 798 Moved the autodoc classes to etomo.storage.autodoc.
 * <p>
 * <p> Revision 3.10  2006/01/11 22:16:13  sueh
 * <p> bug# 675 Added boolean toggleButton to save whether the button is a
 * <p> JButton or JToggleButton.  This is needed in printName.
 * <p>
 * <p> Revision 3.9  2006/01/04 20:26:30  sueh
 * <p> bug# 675 For printing the name:  putting the type first and making the type
 * <p> as constant.
 * <p>
 * <p> Revision 3.8  2006/01/03 23:42:21  sueh
 * <p> bug# 675 Made setName protected.
 * <p>
 * <p> Revision 3.7  2005/12/23 02:16:44  sueh
 * <p> bug# 675 Named the button so it can be found by JfcUnit.
 * <p>
 * <p> Revision 3.6  2005/08/22 17:56:32  sueh
 * <p> bug#  Added isDisplayable().
 * <p>
 * <p> Revision 3.5  2005/08/10 20:44:42  sueh
 * <p> bug# 711  Added getToggleButtonInstance(), isEnabled(), and
 * <p> isSelected()
 * <p>
 * <p> Revision 3.4  2005/08/09 20:25:15  sueh
 * <p> bug# 711  No longer inheriting JButton in MultiLineButton.  This allows
 * <p> MultiLineButton to treate toggling as an attribute.  Then we can get rid of
 * <p> MultiLineToggleButton.  Then we can have one Run3dmodButton which
 * <p> can be toggle or non-toggle.
 * <p>
 * <p> Revision 3.3  2005/07/06 23:37:05  sueh
 * <p> bug# 619 removed unused constructors
 * <p>
 * <p> Revision 3.2  2004/11/19 23:59:12  sueh
 * <p> bug# 520 merging Etomo_3-4-6_JOIN branch to head.
 * <p>
 * <p> Revision 3.1.4.1  2004/09/17 21:37:56  sueh
 * <p> bug# 520 getDefaultUIColor() was moved to UIUtilities
 * <p>
 * <p> Revision 3.1  2004/04/26 03:16:22  rickg
 * <p> Set text margin to 2 all around
 * <p>
 * <p> Revision 3.0  2003/11/07 23:19:01  rickg
 * <p> Version 1.0.0
 * <p>
 * <p> Revision 1.1  2003/10/21 02:32:40  sueh
 * <p> Bug325 New class, behaves like JButton, except that it automatically makes button text multi-line.
 * <p> </p>
 */
public class MultiLineButton implements ProcessResultDisplay {
  private final AbstractButton button;
  private final boolean toggleButton;
  private final ProcessResultDisplayState processResultDisplayState;
  private final Color background;
  private final boolean html;

  private BaseScreenState screenState = null;
  private DialogType dialogType = null;
  private String stateKey = null;
  private boolean manualName = false;
  private Color buttonForeground = null;
  private Color buttonHighlightForeground = null;
  private boolean debug = false;
  private String unformattedLabel = null;
  private FontMetrics fontMetrics = null;
  private boolean enabled = true;
  private boolean editable = true;
  private int width = -1;
  private JLabel label1 = null;
  private JLabel label2 = null;
  private boolean actionCommandSet = false;

  public void dumpState() {
    System.err.print("[toggleButton:" + toggleButton + ",stateKey:" + stateKey
      + ",\nmanualName:" + manualName + ",buttonForeground:" + buttonForeground
      + ",\nbuttonHighlightForeground:" + buttonHighlightForeground + ",debug:" + debug
      + ",\nunformattedLabel:" + unformattedLabel + "]");
  }

  MultiLineButton() {
    this(null, false, null, false, false, false);
  }

  MultiLineButton(String label) {
    this(label, false, null, false, false, false);
  }

  MultiLineButton(final boolean setMinimumSize, String label) {
    this(label, false, null, setMinimumSize, false, false);
  }

  static MultiLineButton getDebugInstance(String label) {
    return new MultiLineButton(label, false, null, false, false, true);
  }

  void setFocusable(final boolean input) {
    button.setFocusable(input);
  }

  int getPreferredWidth() {
    if (fontMetrics == null) {
      fontMetrics = UIUtilities.getFontMetrics(button);
    }
    return UIUtilities.getPreferredWidth(button, unformattedLabel, fontMetrics);
  }

  public void setDebug(final boolean input) {
    debug = input;
  }

  boolean isDebug() {
    return debug;
  }

  int getPreferredWidth(final String text) {
    if (fontMetrics == null) {
      fontMetrics = UIUtilities.getFontMetrics(button);
    }
    return UIUtilities.getPreferredWidth(button, text, fontMetrics);
  }

  int getWidth() {
    return button.getWidth();
  }

  private MultiLineButton(String label, boolean toggleButton) {
    this(label, toggleButton, null, false, false, true);
  }

  /**
   * Pass the dialogType so that a button that is managed by
   * ProcessResultDisplay can save itself in BaseScreenState.
   * @param label
   * @param toggleButton
   * @param dialogType
   */
  MultiLineButton(String label, boolean toggleButton, DialogType dialogType,
    final boolean setMinimumSize, final boolean html, boolean debug) {
    this.toggleButton = toggleButton;
    this.dialogType = dialogType;
    this.debug = debug;
    this.html = html;
    unformattedLabel = label;
    button = newButton();
    setupButton(setMinimumSize);
    init();
    processResultDisplayState = new ProcessResultDisplayState(this);
    String text = button.getText();
    background = button.getBackground();
  }

  AbstractButton newButton() {
    if (toggleButton) {
      return new JToggleButton();
    }
    return new JButton();
  }

  boolean isHtml() {
    return html;
  }
  
  void doClick() {
    button.doClick();
  }

  void setupButton(final boolean setMinimumSize) {
    setSize(setMinimumSize);
    if (unformattedLabel != null) {
      setText(unformattedLabel);
    }
  }

  void setHighlight(boolean highlight) {}

  static final MultiLineButton getToggleButtonInstance() {
    return new MultiLineButton(null, true, null, false, false, false);
  }

  public static final MultiLineButton getToggleButtonInstance(String label,
    DialogType dialogType) {
    return new MultiLineButton(label, true, dialogType, false, false, false);
  }

  static final MultiLineButton getToggleButtonInstance(String label) {
    return new MultiLineButton(label, true);
  }

  /**
   * return's the button's state key for the property file
   * Creates the button state instance if necessary.  Sets the button state name
   * based on the dialog and the button's name.  Once the key is set, it cannot
   * be changed.  Does not store the dialog type.
   * @param dialogType
   * @return
   */
  String createButtonStateKey(DialogType dialogType) {
    if (dialogType != null) {
      stateKey = dialogType.getStorableName() + '.' + button.getName() + ".done";
    }
    return stateKey;
  }

  /**
   * Gets the button state key.  If the state key is null and the dialog type
   * is available, creates the button state key.
   * @return
   */
  public final String getButtonStateKey() {
    if (stateKey == null && dialogType != null) {
      return createButtonStateKey(dialogType);
    }
    return stateKey;
  }

  public void setButtonState(boolean state) {
    setOriginalProcessResultDisplayState(state);
    setSelected(state);
  }

  final void setOriginalProcessResultDisplayState(boolean state) {
    processResultDisplayState.setOriginalState(state);
  }

  /**
   * Returns isSelected() since the button state is selected or not selected
   * @return
   */
  boolean getButtonState() {
    return isSelected();
  }
  
  public boolean equalsID(final int displayID, final String factoryID) {
    return processResultDisplayState.equalsID(displayID,factoryID);
  }
  
  public void setID(final int displayID,final String factoryID) {
    processResultDisplayState.setID(displayID,factoryID);
  }
  
  public void setFactoryID(final String factoryID) {
    processResultDisplayState.setFactoryID(factoryID);
  }
  
  public int getDisplayID() {
    return processResultDisplayState.getDisplayID();
  }
  
  public String getFactoryID() {
    return processResultDisplayState.getFactoryID();
  }
  /**
   * Follows link list pointer - use for setting dependencies
   */
  public ProcessResultDisplay getNext() {
  return  processResultDisplayState.getNext();
  }
  public void setNext(final ProcessResultDisplay display) {
    processResultDisplayState.setNext(display);
  }
  public  void setUseGlobalDependencyList(final boolean use) {
    processResultDisplayState.setUseGlobalDependencyList(use);
  }
  /**
   * Sets manualName to true.  When manualName is true, setText() cannot change
   * the name.
   * @param label
   */
  final void setManualName() {
    manualName = true;
  }

  void setIcon(Icon icon) {
    button.setIcon(icon);
  }

  /**
   * Calls the setName function of the button.  Called when the text is set,
   * unless manualName is true.
   * @param label
   */
  void setName(String label) {
    String name = Utilities.convertLabelToName(label);
    button.setName(
      UITestFieldType.BUTTON.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    if (EtomoDirector.INSTANCE.getArguments().isPrintNames()) {
      System.out.println(getName() + ' ' + AutodocTokenizer.DEFAULT_DELIMITER + ' ');
    }
  }

  final AbstractButton getButton() {
    return button;
  }

   final String getName() {
    return button.getName();
  }

  /**
   * Sets stateKey.  For overriding createButtonStateKey().
   * @param stateKey
   * @return
   */
  final void setStateKey(String stateKey) {
    this.stateKey = stateKey;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    // Only visually enabled if both enabled and editable
    button.setEnabled(enabled && editable);
    // For uneditable only, keep the enabled text color
    if (!toggleButton || html) {
      button.setForeground(
        ColorTool.getButtonInstance(toggleButton).getForeground(isEnabled()));
    }
    if (label1 != null) {
      label1.setEnabled(enabled);
      label2.setEnabled(enabled);
    }
  }

   void setEditable(final boolean editable) {
    this.editable = editable;
    // Editable has no visible effect if the button is disabled.
    if (enabled) {
      button.setEnabled(editable);
      // For uneditable only, keep the enabled text color - so editable doesn't effect the
      // foreground
    }
  }

 public  boolean isEnabled() {
    return enabled;
  }

  final boolean isToggleButton() {
    return toggleButton;
  }

  final void setText(String text) {
    if (!manualName) {
      setName(text);
    }
    unformattedLabel = text;
    setTextLabel(text);
  }

  /**
   * Sets the button text without setting the name or unformattedLabel
   * @param text
   */
  void setTextLabel(final String text) {
    String text1 = null;
    String text2 = null;
    if (text != null && text.indexOf("\\n") != -1) {
      // \n breaks the label into two lines. Expecting only one \n.
      String[] labels = text.split("\\s*\\Q\\n\\E\\s*");
      if (labels.length >= 2) {
        text1 = labels[0];
        text2 = labels[1];
      }
    }
    else if (text != null && width != -1) {
      // If the width has been fixed, and the button can have a mutliline label, then
      // check whether the text fits, and make multiline if necessary.
      if (fontMetrics == null) {
        fontMetrics = UIUtilities.getFontMetrics(button);
      }
      if (fontMetrics != null) {
        int stringWidth = fontMetrics.stringWidth(text);
        int extraLabelSpaceAvailable = 9;
        int space = width - extraLabelSpaceAvailable;
        if (stringWidth > 0 && stringWidth >= space) {
          float firstLinePercent = (float) space / stringWidth;
          // Needs to be a multiline label - break it, completely filling in the first
          // line.
          int firstLineEnd = Math.round(text.length() * firstLinePercent) - 1;
          // Text must break on a space or a dash. Its as long is possible, so back up.
          int size = 2;
          int[] breaks = new int[size];
          breaks[0] = text.lastIndexOf(' ', firstLineEnd);
          breaks[1] = text.lastIndexOf('-', firstLineEnd) + 1;
          int closestBreakIndex = -1;
          for (int i = 0; i < size; i++) {
            if (breaks[i] == -1) {
              continue;
            }
            if (breaks[i] == firstLineEnd) {
              closestBreakIndex = i;
              break;
            }
            if (closestBreakIndex == -1) {
              closestBreakIndex = i;
            }
            else {
              if (Math.abs(firstLineEnd - breaks[i]) < Math
                .abs(firstLineEnd - breaks[closestBreakIndex])) {
                closestBreakIndex = i;
              }
            }
          }
          if (closestBreakIndex != -1) {
            text1 = text.substring(0, breaks[closestBreakIndex]).trim();
            text2 = text.substring(breaks[closestBreakIndex]).trim();
          }
        }
      }
    }
    if (text1 == null) {
      // Only one line needed
      if (label1 == null) {
        // No border layout added or needed
        button.setText(text);
      }
      else {
        // border layout already in use - remove labels and set then to null
        button.remove(label1);
        button.remove(label2);
        label1 = null;
        label2 = null;
        button.setText(text);
      }
    }
    else {
      button.setText("");
      // Using two labels instead of the button's text
      // Make sure a real action command is available
      if (!actionCommandSet) {
        button.setActionCommand(text);
      }
      if (label1 != null) {
        // Two line label and border layout is already set up.
        label1.setText(text1);
        label2.setText(text2);
        button.add(label2);// May have been removed
      }
      else {
        // Two line label - need to set up border layout
        label1 = new JLabel(text1);
        label1.setHorizontalAlignment(JLabel.CENTER);
        label2 = new JLabel(text2);
        label2.setHorizontalAlignment(JLabel.CENTER);
        button.setLayout(new BorderLayout());
        button.add(BorderLayout.NORTH, label1);
        button.add(BorderLayout.SOUTH, label2);
      }
    }
  }

  public String toString() {
    return getText();
  }

  public String getUnformattedLabel() {
    return unformattedLabel;
  }

  public final void addActionListener(ActionListener actionListener) {
    button.addActionListener(actionListener);
  }

  void setActionCommand(final String actionCommand) {
    actionCommandSet = true;
    button.setActionCommand(actionCommand);
  }

  public final String getActionCommand() {
    return button.getActionCommand();
  }

  public final Component getComponent() {
    return button;
  }

  final void setVisible(boolean visible) {
    button.setVisible(visible);
  }

  final void setBorder(Border border) {
    button.setBorder(border);
  }

  final void setBorderPainted(boolean borderPainted) {
    button.setBorderPainted(borderPainted);
  }

  final int getHeight() {
    return button.getHeight();
  }

  final Border getBorder() {
    return button.getBorder();
  }

  public final void setToolTipText(String text) {
    button.setToolTipText(TooltipFormatter.INSTANCE.format(text));
  }

  final void setTooltip(MultiLineButton multiLineButton) {
    button.setToolTipText(multiLineButton.button.getToolTipText());
  }

  final Dimension getPreferredSize() {
    return button.getPreferredSize();
  }

  final void setAlignmentX(float alignmentX) {
    button.setAlignmentX(alignmentX);
  }

  final void setAlignmentY(float alignmentY) {
    button.setAlignmentY(alignmentY);
  }

  /**
   * @return a label suitable for a message - in single quotes and truncated at the colon.
   */
  String getQuotedLabel() {
    return Utilities.quoteLabel(unformattedLabel);
  }

  final String getText() {
    if (label1 == null) {
      return button.getText();
    }
    return unformattedLabel;
  }

  final void addMouseListener(MouseListener mouseListener) {
    button.addMouseListener(mouseListener);
  }

  public final void removeActionListener(ActionListener actionListener) {
    button.removeActionListener(actionListener);
  }

  /**
   * Set the button sizes (preferred and maximum) of a button
   * @param container
   * @param size
   */
  // void setSize() {
  // setSize(false);
  // }

  final FontMetrics getFontMetrics() {
    return fontMetrics;
  }

  private final void setSize(boolean setMinimum) {
    FontMetrics fontMetrics = null;
    if (UIParameters.needButtonFontMetrics()) {
      fontMetrics = UIUtilities.getFontMetrics(button);
    }
    Dimension size = UIParameters.getInstance(fontMetrics).getButtonDimension();
    if (size != null) {
      width = size.width;
    }
    button.setPreferredSize(size);
    button.setMaximumSize(size);
    if (setMinimum) {
      button.setMinimumSize(size);
    }
  }

  public final void setProcessDone(boolean done) {
    setSelected(done);
  }

  /**
   * Make sure that the displayed button matches its internal state.  A button
   * won't update its display if it is not being displayed.
   */
  public final void setScreenState(BaseScreenState screenState) {
    this.screenState = screenState;
    button.setSelected(screenState.getButtonState(getButtonStateKey()));
  }

  final void setSelected(boolean selected) {
    button.setSelected(selected);
    if (screenState != null) {
      screenState.setButtonState(getButtonStateKey(), getButtonState());
    }
  }

  public final boolean getOriginalState() {
    // button has just been pushed, so that original state is the state of the
    // button before it was pushed
    return !isSelected();
  }

  final boolean isSelected() {
    return button.isSelected();
  }

  final boolean isVisible() {
    return button.isVisible();
  }

  final boolean isDisplayable() {
    return button.isDisplayable();
  }

  void init() {
    button.setMargin(new Insets(2, 2, 2, 2));
  }

  public void msgProcessStarting() {
    processResultDisplayState.msgProcessStarting();
  }

  public void msg(ProcessResult processResult) {
    processResultDisplayState.msg(processResult);
  }

  public void msgProcessSucceeded() {
    processResultDisplayState.msgProcessSucceeded();
  }

  public void msgProcessFailed() {
    processResultDisplayState.msgProcessFailed();
  }

  public void msgProcessFailedToStart() {
    processResultDisplayState.msgProcessFailedToStart();
  }

  public void msgSecondaryProcess() {
    processResultDisplayState.msgSecondaryProcess();
  }

  public void addDependentDisplay(ProcessResultDisplay dependentDisplay) {
    processResultDisplayState.addDependentDisplay(dependentDisplay);
  }

  public void setOriginalState(boolean originalState) {
    processResultDisplayState.setOriginalState(originalState);
  }

  public void addFailureDisplay(ProcessResultDisplay failureDisplay) {
    processResultDisplayState.addFailureDisplay(failureDisplay);
  }

  public void addSuccessDisplay(ProcessResultDisplay successDisplay) {
    processResultDisplayState.addSuccessDisplay(successDisplay);
  }
}