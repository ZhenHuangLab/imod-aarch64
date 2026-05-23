package etomo.ui.swing;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import etomo.Arguments.DebugLevel;
import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.logic.BusyStatusListener;
import etomo.process.ProcessState;
import etomo.storage.DataFileFilter;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.BaseMetaData;
import etomo.type.ProcessEndState;
import etomo.type.ProcessName;

/**
 * <p>Description: Associated with a single manager.  Manages the panels associated with
 * its manager.  May display the A or B axis.  Provides B axis support for SubFrame,
 * which has to do some panel tasks.</p>
 *
 * <p>Copyright: Copyright 2004 - 2020 by the Regents of the University of Colorado</p>
 *
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 1.2  2011/02/03 06:20:01  sueh
 * <p> bug# 1422 Removed setParallelDialog.  Control of the processing method
 * <p> has been centralized in the processing method mediator class.
 * <p>
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.44  2010/03/03 05:04:38  sueh
 * <p> bug# 1311 Removed unnecessary ProcessName references.
 * <p>
 * <p> Revision 1.43  2010/02/17 05:03:12  sueh
 * <p> bug# 1301 Using manager instead of manager key for popping up messages.
 * <p>
 * <p> Revision 1.42  2009/09/25 16:06:42  sueh
 * <p> bug# 1250 Setting scroll bar increments to make scrolling work properly.
 * <p>
 * <p> Revision 1.41  2009/02/04 23:36:48  sueh
 * <p> bug# 1158 Changed id and exception classes in LogFile.
 * <p>
 * <p> Revision 1.40  2009/01/20 20:14:01  sueh
 * <p> bug# 1102 Inheriting EtomoPanel so that MainPanel can name itself.
 * <p>
 * <p> Revision 1.39  2008/10/06 22:38:47  sueh
 * <p> bug# 1113 Removed pack, which is unecessary since table scrolling was
 * <p> removed.
 * <p>
 * <p> Revision 1.38  2008/09/30 21:58:37  sueh
 * <p> bug# 1113 Added repaint, so that focus can be requested after repaint.
 * <p>
 * <p> Revision 1.37  2008/07/19 00:56:23  sueh
 * <p> Reduced exposure by removing "protected" directive.
 * <p>
 * <p> Revision 1.36  2007/02/19 22:02:20  sueh
 * <p> bug# 964 Fixed function names:  was AxisPanelIsNull, now its isAxisPanelNull.
 * <p>
 * <p> Revision 1.35  2006/11/29 00:19:49  sueh
 * <p> bug# 934 Added endThreads() to notify the load average threads when the
 * <p> manager exits.
 * <p>
 * <p> Revision 1.34  2006/10/24 22:34:34  sueh
 * <p> bug# 947
 * <p>
 * <p> Revision 1.33  2006/03/20 18:03:59  sueh
 * <p> Improved the name of the parameter of setParallelDialog
 * <p>
 * <p> Revision 1.32  2005/10/15 00:33:53  sueh
 * <p> bug# 532 Changed showParallelStatus() to setParallelDialog().
 * <p>
 * <p> Revision 1.31  2005/09/27 23:30:56  sueh
 * <p> bug# 532 Modified pass-through functions that control the axis panel.
 * <p> Added getParallelPanel() and done().  Changed showParallelStatus() to
 * <p> showParallelPanel().
 * <p>
 * <p> Revision 1.30  2005/09/22 21:24:06  sueh
 * <p> bug# 532 Moved the parallel process panel to AxisProcessPanel.  Added
 * <p> pack() and showParallelStatus().  Removed setParallelProgressDisplay().
 * <p>
 * <p> Revision 1.29  2005/09/21 16:39:52  sueh
 * <p> bug# 532 Added abstract setState(ProcessState, AxisID, ParallelDialog)
 * <p> so that one processchunks function in BaseManager can handle multiple
 * <p> dialogs.
 * <p>
 * <p> Revision 1.28  2005/08/22 17:55:47  sueh
 * <p> bug# 532  Add status string, remove pause button and add parallel
 * <p> progress display instead so that the button will be managed indirectly.
 * <p> The pause button should only be turned on for processes that can be
 * <p> paused.
 * <p>
 * <p> Revision 1.27  2005/08/09 20:24:50  sueh
 * <p> bug# 711  No longer inheriting JButton in MultiLineButton.
 * <p>
 * <p> Revision 1.26  2005/08/04 20:12:03  sueh
 * <p> bug# 532  Centralizing fit window functionality by placing fitting functions
 * <p> in UIHarness.  Removing packMainWindow from the manager.  Sending
 * <p> the manager to UIHarness.pack() so that packDialogs() can be called.
 * <p>
 * <p> Revision 1.25  2005/07/26 23:07:25  sueh
 * <p> bug# 701 When stopping the progress bar, pass the process end state.
 * <p>
 * <p> Revision 1.24  2005/07/21 22:19:58  sueh
 * <p> bug# 532 removed "kill / pause" label from kill process button.  Pause
 * <p> button with be managed by separately by AxisProcessPanel, which
 * <p> receives a pointer to the button through setPauseButton().  No longer
 * <p> changing the kill process button label.
 * <p>
 * <p> Revision 1.23  2005/07/11 23:03:21  sueh
 * <p> bug# 619 Allow the kill process button label to be changed to
 * <p> "Kill / Pause".  Added setProgressBar(String, int, boolean) and
 * <p> startProgressBar(String, boolean).
 * <p>
 * <p> Revision 1.22  2005/05/17 19:38:40  sueh
 * <p> bug# 663 Renamed updateDataParameters() to setStatusBarText() and
 * <p> moved the common functionality to MainPanel.setStatusBarText().  Add
 * <p> functionality to prevent the status bar text from becoming too long.
 * <p>
 * <p> Revision 1.21  2005/04/26 17:40:36  sueh
 * <p> bug# 615 Made MainFrame a package-level class.  All MainFrame
 * <p> functionality is handled through UIHarness to make Etomo more
 * <p> compatible with JUnit.
 * <p>
 * <p> Revision 1.20  2005/04/25 21:09:24  sueh
 * <p> bug# 615 Moving message dialog functions from mainPanel to
 * <p> EtomoFrame.
 * <p>
 * <p> Revision 1.19  2005/04/21 20:43:47  sueh
 * <p> bug# 615 Moved two frame code out of newstuff.  Removed
 * <p> packAxis, since it is not necessary.  Moved getTestParamFilename() to
 * <p> EtomoFrame, since it is only used for menu commands.  Moved
 * <p> fitWindow() to EtomoFrame.
 * <p>
 * <p> Revision 1.18  2005/04/20 01:50:19  sueh
 * <p> bug# 615 Removed getAxisB because its name was misleading.  It
 * <p> shows axis like a show function.  Place the functionality in showAxisB.
 * <p> Added boolean subFrame to showAxisB to distinguish it from showing
 * <p> AxisB in the main frame.
 * <p>
 * <p> Revision 1.17  2005/04/16 02:00:01  sueh
 * <p> bug# 615 Removed split pane function when --newstuff is used.  Bring up
 * <p> A axis alone for a dual axis tomogram.
 * <p>
 * <p> Revision 1.16  2005/04/12 19:38:54  sueh
 * <p> bug# 615 Made a newstuff version with the split pane and a very simple
 * <p> fitting algorithm.
 * <p>
 * <p> Revision 1.15  2005/04/01 02:54:29  sueh
 * <p> bug# 622 newstuff:  changed showPRocessingPanel to remove split pane
 * <p> and individual scroll bars.
 * <p>
 * <p> Revision 1.14  2005/04/01 00:14:07  sueh
 * <p> bug# 622 Trying to get packAxis() to work with divider removed on A only
 * <p> and B only.  Problem with wide window not solved.
 * <p>
 * <p> Revision 1.13  2005/03/30 23:45:19  sueh
 * <p> bug# 622 Adding show functions to remove and restore the divider when
 * <p> showing Axis A, B, or both.
 * <p>
 * <p> Revision 1.12  2005/02/24 20:08:03  sueh
 * <p> Comments for dealing with java 1.5.0
 * <p>
 * <p> Revision 1.11  2005/02/24 02:24:53  sueh
 * <p> bug# 605 In fitWindows: Tab height is different in Mac.  Adjust the
 * <p> tabHeight for mac os.
 * <p>
 * <p> Revision 1.10  2005/02/19 00:31:01  sueh
 * <p> bug# 605 fitWindow():  When tabs are used correct the frameBorder height
 * <p> to avoid repacking when there is not vertical scroll bar.  This prevents a
 * <p> B axis only display from going to and A and B display during fit.
 * <p>
 * <p> Revision 1.9  2005/02/17 20:25:39  sueh
 * <p> bug# 513 fitWindow(boolean):  Repaired scroll bar functionality.
 * <p>
 * <p> Revision 1.8  2005/02/17 02:43:37  sueh
 * <p> bug# 605 Added abstract saveDisplayState().  If both axis panels have
 * <p> width = 0, show both panels and do a plain pack().
 * <p>
 * <p> Revision 1.7  2005/02/11 19:03:31  sueh
 * <p> bug# 594 Add show to fitWindow() to handle the case when autofit is off.
 * <p> This updates the main frame tabs.
 * <p>
 * <p> Revision 1.6  2005/02/09 22:30:51  sueh
 * <p> Removing unnecessary import.
 * <p>
 * <p> Revision 1.5  2005/02/09 20:51:56  sueh
 * <p> bug# 594 Moved maximumSize from MainPanel to MainFrame so that it
 * <p> will work with the tabbedPane.
 * <p>
 * <p> Revision 1.4  2005/01/27 20:22:18  sueh
 * <p> bug# 513 Synchronizing fit window code.
 * <p>
 * <p> Revision 1.3  2005/01/27 00:13:03  sueh
 * <p> bug# 543 Checking autofit before fitting window.  Added
 * <p> fitWindow(boolean force) to force a fit window for Ctrl-F.
 * <p>
 * <p> Revision 1.2  2004/11/19 23:58:52  sueh
 * <p> bug# 520 merging Etomo_3-4-6_JOIN branch to head.
 * <p>
 * <p> Revision 1.1.2.13  2004/11/19 19:38:56  sueh
 * <p> bug# 520 Added wrap functions to wrap message dialog messages.
 * <p>
 * <p> Revision 1.1.2.12  2004/11/19 00:23:45  sueh
 * <p> bug# 520 Changed the file extension in ConstJoinMetaData to contain the
 * <p> period.
 * <p>
 * <p> Revision 1.1.2.11  2004/10/15 00:50:18  sueh
 * <p> bug# 520 Made getTestParamFilename() generic.  Removed
 * <p> openEtomoDataFileDialog().
 * <p>
 * <p> Revision 1.1.2.10  2004/10/11 02:15:57  sueh
 * <p> bug# 520 Moved responsibility for axisPanelA and axisPanelB member
 * <p> variables to the child classes.  Used abstract functions to use these
 * <p> variables in the base class.  This is more reliable and doesn't require
 * <p> casting.
 * <p>
 * <p> Revision 1.1.2.9  2004/10/08 16:34:05  sueh
 * <p> bug# 520 Since EtomoDirector is a singleton, made all functions and
 * <p> member variables non-static.
 * <p>
 * <p> Revision 1.1.2.8  2004/10/01 20:00:21  sueh
 * <p> bug# 520 Standardized getting the metadata file name.
 * <p>
 * <p> Revision 1.1.2.7  2004/09/29 19:37:04  sueh
 * <p> bug# 520 Moved status bar initialization to child classes.
 * <p>
 * <p> Revision 1.1.2.6  2004/09/21 18:02:52  sueh
 * <p> bug# 520 Added openYesNoDialog(String, String).  For
 * <p> openMessageDialog(), handling the situation where no dataset name is
 * <p> set by opening the message without adding anything to it.
 * <p>
 * <p> Revision 1.1.2.5  2004/09/15 22:45:34  sueh
 * <p> bug# 520 Moved openSetupPanel to MainTomogramPanel.  Moved
 * <p> showProcessingPanel() to this class.  Moved AxisProcessPanel creation
 * <p> to abstract functions.  Added the dataset name before the message in
 * <p> openMessageDialog.
 * <p>
 * <p> Revision 1.1.2.4  2004/09/09 17:34:41  sueh
 * <p> bug# 520 remove unnecessary functions that are duplicated in MainFrame:
 * <p> menuFileMRUListAction and popUpContextMenu
 * <p>
 * <p> Revision 1.1.2.3  2004/09/08 22:39:54  sueh
 * <p> bug# 520 class doesn't need to be abstract, fixed problem with packing
 * <p> setup dialog by calling revalidate()
 * <p>
 * <p> Revision 1.1.2.2  2004/09/08 20:11:31  sueh
 * <p> bug# 520 make this class into a base class.  Move all tomogram specific
 * <p> functionality to MainTomogramPanel.
 * <p>
 * <p> Revision 1.1.2.1  2004/09/07 18:01:08  sueh
 * <p> bug# 520 moved all variables and functions associated with mainPAnel
 * <p> to MainPanel.
 * <p> </p>
 */
public abstract class MainPanel extends EtomoPanel implements BusyStatusListener {
  private static final String STATUS_BAR_EMPTY_TITLE = "No data set loaded";
  private static final String STATUS_BAR_BASE_TITLE = "Data file: ";

  final JLabel statusBar = new JLabel(STATUS_BAR_EMPTY_TITLE);
  final JPanel panelCenter = new JPanel();

  // These panels get instantiated as needed
  private ScrollPanel scrollA;
  private JScrollPane scrollPaneA;
  private ScrollPanel scrollB;
  private JScrollPane scrollPaneB;

  final BaseManager manager;

  private AxisProgressPanel axisProgressPanelA = null;
  private AxisProgressPanel axisProgressPanelB = null;

  private boolean showingBothAxis = false;
  private boolean showingAxisA = true;
  private boolean showingSetup = false;
  AxisType axisType = AxisType.NOT_SET;
  private DebugLevel debugLevel = EtomoDirector.INSTANCE.getArguments().getDebugLevel();

  private static final int estimatedMenuHeight = 60;
  private static final int extraScreenWidthMultiplier = 2;
  private static final Dimension frameBorder = FixedDim.frameBorder;

  private final JPanel pnlBusyStatus = new JPanel();
  private final JLabel lBusyStatusA = createBusyStatusLabel();
  private final JLabel lGuiBusyStatusA = createGuiBusyStatusLabel();

  private JLabel lBusyStatusB = null;
  private JLabel lGuiBusyStatusB = null;

  abstract void createAxisPanelA(AxisID axisID, AxisProgressPanel axisProgressPanel);

  abstract void createAxisPanelB(AxisProgressPanel axisProgressPanel);

  abstract void resetAxisPanels();

  abstract void addAxisPanelA();

  abstract void addAxisPanelB();

  abstract boolean isAxisPanelANull();

  abstract boolean isAxisPanelBNull();

  abstract boolean hideAxisPanelA();

  abstract boolean hideAxisPanelB();

  abstract void showAxisPanelA();

  abstract void showAxisPanelB();

  abstract AxisProcessPanel mapBaseAxisProcessPanel(AxisID axisID);

  abstract DataFileFilter getDataFileFilter();

  abstract void saveDisplayState();

  abstract AxisProcessPanel getAxisPanelA();

  abstract AxisProcessPanel getAxisPanelB();

  abstract void setState(ProcessState processState, AxisID axisID,
    AbstractParallelDialog parallelDialog);

  abstract AxisProgressPanel mapAxisProgressPanel(AxisID axisID);

  /**
   * Main window constructor.  This sets up the menus and status line.
   */
  MainPanel(final BaseManager manager) {
    this.manager = manager;
    manager.addBusyStatusListener(this);
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    lBusyStatusA.setEnabled(false);
    lGuiBusyStatusA.setEnabled(false);
    setLayout(new BorderLayout());
    // Construct the main frame panel layout
    panelCenter.setLayout(new BoxLayout(panelCenter, BoxLayout.X_AXIS));
    add(panelCenter, BorderLayout.CENTER);
    JPanel pnlStatus = new JPanel();
    add(pnlStatus, BorderLayout.SOUTH);
    // Status
    pnlStatus.setLayout(createStatusBorderLayout());
    pnlStatus.add(statusBar, BorderLayout.WEST);
    pnlStatus.add(pnlBusyStatus, BorderLayout.EAST);
    // BusyStatus
    pnlBusyStatus.setLayout(new BorderLayout());
    pnlBusyStatus.add(lBusyStatusA, BorderLayout.EAST);
    pnlBusyStatus.add(lGuiBusyStatusA, BorderLayout.WEST);
  }

  static final BorderLayout createStatusBorderLayout() {
    BorderLayout blStatus = new BorderLayout();
    blStatus.setHgap(2);
    return blStatus;
  }

  final String getStatusBarText() {
    return statusBar.getText();
  }

  public final String getStatus() {
    String status = statusBar.getText();
    if (status.equals(STATUS_BAR_EMPTY_TITLE)) {
      return "";
    }
    if (status.startsWith(STATUS_BAR_BASE_TITLE)) {
      return status.substring(STATUS_BAR_BASE_TITLE.length());
    }
    return status;
  }

  public final void repaint() {
    super.repaint();
    if (manager != null) {
      Component focusComponent = manager.getFocusComponent();
      if (focusComponent != null) {
        focusComponent.requestFocus();
      }
    }
  }

  ScrollPanel getScrollA() {
    return scrollA;
  }

  ScrollPanel getScrollB() {
    return scrollB;
  }

  public void setStatusBarText(final File paramFile, final BaseMetaData metaData,
    final LogWindow logWindow) {
    // Set the title of log panel. SetStatusBarText is used by all of the
    // interfaces so this is good place to do it.
    if (logWindow != null) {
      logWindow.setTitle(paramFile, metaData, manager.getPropertyUserDir());
    }
    // Set the status bar
    int maxTitleLength = 79;
    if (metaData == null) {
      statusBar.setText(STATUS_BAR_EMPTY_TITLE);
    }
    else {
      if (paramFile == null) {
        statusBar.setText(STATUS_BAR_BASE_TITLE + "NOT SAVED");
      }
      else {
        String datasetName = paramFile.getAbsolutePath();
        if (STATUS_BAR_BASE_TITLE.length() + datasetName.length() > maxTitleLength) {
          // Shorten the dataset name
          datasetName = "..." + datasetName.substring(
            datasetName.length() - (maxTitleLength - STATUS_BAR_BASE_TITLE.length() - 3));
        }
        String title = STATUS_BAR_BASE_TITLE + datasetName;
        statusBar.setText(title);
      }
    }
  }

  final void setStatusBarTextToDirectory(String directory, final int maxTitleLength) {
    // Set the status bar
    if (directory == null || directory.matches("\\s*")) {
      statusBar.setText("");
    }
    else {
      if (directory.length() > maxTitleLength) {
        // Shorten the dataset name
        directory = "..." + directory.substring(directory.length() - (maxTitleLength));
      }
      String title = directory;
      statusBar.setText(title);
    }
  }

  /**
   * set divider location
   * @param value
   */
  public final void setDividerLocation(final double value) {}

  /**
   * Show a blank processing panel
   */
  void showBlankProcess(final AxisID axisID) {
    mapBaseAxisProcessPanel(axisID).eraseDialogPanel();
  }

  /**
   * Show the specified processing panel
   */
  public final void showProcess(final Container processPanel, final AxisID axisID) {
    AxisProcessPanel axisPanel = mapBaseAxisProcessPanel(axisID);
    axisPanel.replaceDialogPanel(processPanel);
    UIHarness.INSTANCE.pack(axisID, manager);
  }

  public final void setProgressBar(final String label, final int nSteps,
    final AxisID axisID) {
    setProgressBar(label, nSteps, axisID, false);
  }

  public final void setProcessCanBeKilled(final boolean input, final AxisID axisID) {
    AxisProgressPanel panel = mapAxisProgressPanel(axisID);
    if (panel != null) {
      panel.setProcessCanBeKilled(input);
    }
  }

  /**
   * Set the progress bar to the beginning of determinant sequence
   * @param label
   * @param nSteps
   */
  public final void setProgressBar(final String label, final int nSteps,
    final AxisID axisID, boolean pauseEnabled) {
    AxisProgressPanel panel = mapAxisProgressPanel(axisID);
    if (panel != null) {
      panel.setProgressBar(label, nSteps);
      panel.setProgressBarValue(0);
    }
    AxisProcessPanel axisPanel = mapBaseAxisProcessPanel(axisID);
    if (axisPanel != null) {
      axisPanel.setPauseEnabled(pauseEnabled);
    }
  }

  public final void setStaticProgressBar(final String label, final AxisID axisID) {
    AxisProgressPanel panel = mapAxisProgressPanel(axisID);
    if (panel != null) {
      panel.setStaticProgressBar(label);
    }
  }

  public final ParallelPanel getParallelPanel(final AxisID axisID) {
    AxisProcessPanel axisPanel = mapBaseAxisProcessPanel(axisID);
    if (axisPanel != null) {
      return axisPanel.getParallelPanel();
    }
    return null;
  }

  public final JPanel getParallelStatusPanel(final AxisID axisID) {
    AxisProcessPanel axisPanel = mapBaseAxisProcessPanel(axisID);
    if (axisPanel != null) {
      return axisPanel.getParallelStatusPanel();
    }
    return null;
  }

  public final void done() {
    AxisProcessPanel axisPanel = mapBaseAxisProcessPanel(AxisID.FIRST);
    if (axisPanel != null) {
      axisPanel.done();
    }
    axisPanel = mapBaseAxisProcessPanel(AxisID.SECOND);
    if (axisPanel != null) {
      axisPanel.done();
    }
  }

  /**
   * Set the progress bar to the specified value
   * @param value
   * @param axisID
   */
  public final void setProgressBarValue(final int value, final AxisID axisID) {
    mapAxisProgressPanel(axisID).setProgressBarValue(value);
  }

  /**
   * Set the progress bar to the speficied value and update the string
   * @param value
   * @param string
   * @param axisID
   */
  public final void setProgressBarValue(final int value, final String string,
    final AxisID axisID) {
    mapAxisProgressPanel(axisID).setProgressBarValue(value, string);
  }

  public final void startProgressBar(String name, AxisID axisID) {
    startProgressBar(name, axisID, null);
  }

  /**
   *  Start the indeterminate progress bar on the specified axis 
   */
  public final void startProgressBar(final String name, final AxisID axisID,
    final ProcessName processName) {
    mapAxisProgressPanel(axisID).startProgressBar(name, processName);
  }

  public final void stopProgressBar(final AxisID axisID) {
    stopProgressBar(axisID, ProcessEndState.DONE, null);
  }

  public final void stopProgressBar(final AxisID axisID,
    final ProcessEndState processEndState) {
    stopProgressBar(axisID, processEndState, null);
  }

  /**
   * Stop the specified progress bar
   * @param axisID
   */
  public void stopProgressBar(final AxisID axisID, final ProcessEndState processEndState,
    final String statusString) {
    AxisProgressPanel panel = mapAxisProgressPanel(axisID);
    if (panel != null) {
      panel.stopProgressBar(processEndState, statusString);
    }
    AxisProcessPanel axisPanel = mapBaseAxisProcessPanel(axisID);
    if (axisPanel != null) {
      axisPanel.setPauseEnabled(false);
    }
  }

  /**
   * @param axisID - axis ID that the progress panel will be created with if it is null.
   * @return an axis progress panel - never null
   */
  final AxisProgressPanel getProgressPanel(final AxisID axisID) {
    if (axisID == AxisID.SECOND) {
      if (axisProgressPanelB == null) {
        axisProgressPanelB = AxisProgressPanel.getInstance(axisID, manager);
      }
      return axisProgressPanelB;
    }
    if (axisProgressPanelA == null) {
      axisProgressPanelA = AxisProgressPanel.getInstance(axisID, manager);
    }
    return axisProgressPanelA;
  }

  /**
   * Show the processing panel for the requested AxisType
   * Corrects the axisProgressPanel AXisID based on axisType.
   */
  public void showProcessingPanel(final AxisType axisType) {
    // Delete any existing panels
    resetAxisPanels();
    this.axisType = axisType;
    panelCenter.removeAll();
    if (axisProgressPanelA != null) {
      axisProgressPanelA.correctAxisID(axisType);
    }
    if (axisType == AxisType.SINGLE_AXIS) {
      AxisID axisID = AxisID.ONLY;
      if (axisProgressPanelA == null) {
        axisProgressPanelA = AxisProgressPanel.getInstance(axisID, manager);
      }
      createAxisPanelA(axisID, axisProgressPanelA);
      scrollA = new ScrollPanel();
      addAxisPanelA();
      scrollPaneA = new JScrollPane(scrollA);
      setScrollBarIncrements(scrollPaneA.getVerticalScrollBar());
      setScrollBarIncrements(scrollPaneA.getHorizontalScrollBar());
      panelCenter.add(scrollPaneA);
    }
    else {
      AxisID axisID = AxisID.FIRST;
      if (axisProgressPanelA == null) {
        axisProgressPanelA = AxisProgressPanel.getInstance(axisID, manager);
      }
      createAxisPanelA(axisID, axisProgressPanelA);
      scrollA = new ScrollPanel();
      addAxisPanelA();
      scrollPaneA = new JScrollPane(scrollA);
      setScrollBarIncrements(scrollPaneA.getVerticalScrollBar());
      setScrollBarIncrements(scrollPaneA.getHorizontalScrollBar());
      axisID = AxisID.SECOND;
      if (axisProgressPanelB == null) {
        axisProgressPanelB = AxisProgressPanel.getInstance(axisID, manager);
      }
      createAxisPanelB(axisProgressPanelB);
      scrollB = new ScrollPanel();
      addAxisPanelB();
      scrollPaneB = new JScrollPane(scrollB);
      setScrollBarIncrements(scrollPaneB.getVerticalScrollBar());
      setScrollBarIncrements(scrollPaneB.getHorizontalScrollBar());
      setAxisA();
    }
  }

  private final void setScrollBarIncrements(final JScrollBar scrollBar) {
    scrollBar.setUnitIncrement(10);
    scrollBar.setBlockIncrement(50);
  }

  JScrollPane showBothAxis() {
    if (axisType != AxisType.DUAL_AXIS || showingBothAxis) {
      return null;
    }
    showingBothAxis = true;
    showingAxisA = true;
    AxisProcessPanel axisPanel = getAxisPanelB();
    if (axisPanel != null) {
      axisPanel.showBothAxis();
    }
    getAxisPanelA().showBothAxis();
    return scrollPaneB;
  }

  final boolean isShowingBothAxis() {
    return showingBothAxis;
  }

  final boolean isShowingAxisA() {
    return showingAxisA;
  }

  public final String toString() {
    if (manager != null) {
      return "[" + manager.getPropertyUserDir() + "," + super.toString() + "]";
    }
    return "[" + super.toString() + "]";
  }

  public void msgBusyStatusChanged(AxisID axisID, boolean processStatus) {
    SwingUtilities.invokeLater(new SetBusyStatus(axisID, false, processStatus));
  }

  public void msgGuiBusyStatusChanged(AxisID axisID, boolean processStatus) {
    SwingUtilities.invokeLater(new SetBusyStatus(axisID, true, processStatus));
  }

  private final class SetBusyStatus implements Runnable {
    final AxisID axisID;
    final boolean gui;
    final boolean enabled;

    private SetBusyStatus(final AxisID axisID, final boolean gui, final boolean enabled) {
      this.axisID = axisID;
      this.gui = gui;
      this.enabled = enabled;
    }

    public void run() {
      if (!gui) {
        if (axisID == AxisID.SECOND) {
          if (lBusyStatusB == null) {
            lBusyStatusB = createBusyStatusLabel();
          }
          lBusyStatusB.setEnabled(enabled);
        }
        else {
          lBusyStatusA.setEnabled(enabled);
        }
      }
      else {
        if (axisID == AxisID.SECOND) {
          if (lGuiBusyStatusB == null) {
            lGuiBusyStatusB = createGuiBusyStatusLabel();
          }
          lGuiBusyStatusB.setEnabled(enabled);
        }
        else {
          lGuiBusyStatusA.setEnabled(enabled);
        }
      }
    }
  }

  static JLabel createBusyStatusLabel() {
    JLabel lBusyIcon = new JLabel(BusyStatusPanel.ICON);
    lBusyIcon.setName(BusyStatusPanel.LABEL);
    return lBusyIcon;
  }

  static JLabel createGuiBusyStatusLabel() {
    JLabel lBusyIcon = new JLabel(BusyStatusPanel.GUI_ICON);
    lBusyIcon.setName(BusyStatusPanel.GUI_LABEL);
    return lBusyIcon;
  }

  void showAxisA() {
    panelCenter.removeAll();
    pnlBusyStatus.removeAll();
    setAxisA();
  }

  private final void setAxisA() {
    showingBothAxis = false;
    showingAxisA = true;
    if (manager.isValid()) {
      panelCenter.add(scrollPaneA);
    }
    pnlBusyStatus.add(lBusyStatusA, BorderLayout.EAST);
    pnlBusyStatus.add(lGuiBusyStatusA, BorderLayout.WEST);
  }

  void showAxisB() {
    showingBothAxis = false;
    showingAxisA = false;
    panelCenter.removeAll();
    pnlBusyStatus.removeAll();
    panelCenter.add(scrollPaneB);
    if (lBusyStatusB == null) {
      lBusyStatusB = createBusyStatusLabel();
      lBusyStatusB.setEnabled(false);
    }
    if (lGuiBusyStatusB == null) {
      lGuiBusyStatusB = createGuiBusyStatusLabel();
      lGuiBusyStatusB.setEnabled(false);
    }
    pnlBusyStatus.add(lBusyStatusB, BorderLayout.EAST);
    pnlBusyStatus.add(lGuiBusyStatusB, BorderLayout.WEST);
  }

  /**
   * set vertical scrollbar policy
   * @param always
   */
  final void setVerticalScrollBarPolicy(final boolean always) {
    int policy = always ? JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
      : JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED;
    if (scrollPaneA != null) {
      scrollPaneA.setVerticalScrollBarPolicy(policy);
    }
    if (scrollPaneB != null) {
      scrollPaneB.setVerticalScrollBarPolicy(policy);
    }
  }

  // TODO Need a way to repaint the existing font
  final void repaintWindow() {
    repaintContainer(this);
    this.repaint();
  }

  private final void repaintContainer(final Container container) {
    Component[] comps = container.getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof Container) {
        Container cont = (Container) comps[i];
        repaintContainer(cont);
      }
      comps[i].repaint();
    }
  }

  final AxisType getAxisType() {
    return axisType;
  }

  final boolean isShowingSetup() {
    return showingSetup;
  }

  final void setShowingSetup(final boolean input) {
    showingSetup = input;
  }
}
