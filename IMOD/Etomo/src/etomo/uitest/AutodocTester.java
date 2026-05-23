package etomo.uitest;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.process.SystemProgram;
import etomo.storage.LogFile;
import etomo.storage.autodoc.AutodocFactory;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.storage.autodoc.ReadOnlySection;
import etomo.type.AxisID;
import etomo.type.EtomoNumber;
import etomo.type.Extension;
import etomo.type.ImageFilenameStyle;
import etomo.type.ProcessEndState;
import etomo.type.ToolType;
import etomo.type.UITestActionType;
import etomo.type.UITestFieldType;
import etomo.type.UITestSubjectType;
import etomo.ui.swing.AxisProgressPanel;
import etomo.ui.swing.BusyStatusPanel;
import etomo.ui.swing.Popup;
import etomo.ui.swing.ProgressPanel;
import etomo.ui.swing.UIHarness;
import etomo.uitest.tests.ExternalUITestSuitFactory;
import etomo.uitest.tests.ExternalUITestSuite;
import etomo.util.Utilities;
import junit.extensions.jfcunit.JFCTestHelper;
import junit.extensions.jfcunit.eventdata.AbstractMouseEventData;
import junit.extensions.jfcunit.eventdata.JSpinnerMouseEventData;
import junit.extensions.jfcunit.eventdata.JTabbedPaneMouseEventData;
import junit.extensions.jfcunit.eventdata.KeyEventData;
import junit.extensions.jfcunit.eventdata.MouseEventData;
import junit.extensions.jfcunit.finder.AbstractButtonFinder;
import junit.extensions.jfcunit.finder.ComponentFinder;
import junit.extensions.jfcunit.finder.NamedComponentFinder;
import junit.framework.AssertionFailedError;

/**
 * <p>Description: The uitest language interpreter.</p>
 * 
 * <p>Copyright: Copyright 2008 - 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.55  2011/07/14 21:21:26  sueh
 * <p> Increasing waiting in formatApplication.
 * <p>
 * <p> Revision 1.54  2011/07/12 03:33:27  sueh
 * <p> In executeCommand handled a file chooser in Windows.
 * <p>
 * <p> Revision 1.53  2011/07/08 18:34:01  sueh
 * <p> In formatApplication, Increasing format time.
 * <p>
 * <p> Revision 1.52  2011/06/29 03:13:46  sueh
 * <p> In formatApplication increasing format wait time.
 * <p>
 * <p> Revision 1.51  2011/06/28 02:33:02  sueh
 * <p> Allow an extra reformat because the advanced button could not be found in a UITest.
 * <p>
 * <p> Revision 1.50  2011/05/26 02:42:35  sueh
 * <p> First format isn't always working - allow a maximum of two.
 * <p>
 * <p> Revision 1.49  2011/05/25 14:20:12  sueh
 * <p> Increasing wait after formatting application.
 * <p>
 * <p> Revision 1.48  2011/05/24 23:07:31  sueh
 * <p> Bug# 1478 In assertField, fixed combo box comparison.
 * <p>
 * <p> Revision 1.47  2011/05/24 15:49:39  sueh
 * <p> FormatApplication was not being called in all cases - fixed.
 * <p>
 * <p> Revision 1.46  2011/05/23 16:08:00  sueh
 * <p> Trying to get formatApplication to solve the missing Advanced button problem more often.  Increased
 * <p> the wait after formatting.  Made it easy to change the number of formats done.
 * <p>
 * <p> Revision 1.45  2011/05/20 21:49:56  sueh
 * <p> Trying to get formatApplication to solve the missing Advanced button problem more often.
 * <p>
 * <p> Revision 1.44  2011/05/13 03:47:14  sueh
 * <p> Increase formatApplication sleep.
 * <p>
 * <p> Revision 1.43  2011/02/22 21:51:07  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.42  2010/11/13 16:08:08  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.41  2010/11/10 20:56:59  sueh
 * <p> In executeCommand slowing down assert.exists.file.  In Windows it
 * <p> may have run too fast to avoid file locks.
 * <p>
 * <p> Revision 1.40  2010/11/09 21:54:53  sueh
 * <p> In executeCommand slowing down assert.exists.file.  In Windows it
 * <p> may have run too fast to avoid file locks.
 * <p>
 * <p> Revision 1.39  2010/11/09 01:33:07  sueh
 * <p> In executeCommand slowing down assert.not-exists.file.  In Windows it
 * <p> may have run too fast to avoid file locks.
 * <p>
 * <p> Revision 1.38  2010/06/12 00:50:36  sueh
 * <p> bug# 1383 Try a REDRAW_WAIT of 3.
 * <p>
 * <p> Revision 1.37  2010/06/10 21:58:45  sueh
 * <p> bug# 1383 Added REDRAW_WAIT.  Using it with fields that may require a
 * <p> panel to be redrawn.
 * <p>
 * <p> Revision 1.36  2010/06/09 21:27:48  sueh
 * <p> bug# 1383 In executeField increased the button sleep to 3 milliseconds.
 * <p>
 * <p> Revision 1.35  2010/06/08 19:04:33  sueh
 * <p> bug# 1383 Adding format action.  Changed formatApplication sleep to a quarter of a second.  Increased the checkbox and radio button sleeps to
 * <p> 3 milliseconds.
 * <p>
 * <p> Revision 1.34  2010/06/08 16:36:55  sueh
 * <p> In formatApplication increased sleep.
 * <p>
 * <p> Revision 1.33  2010/06/07 14:54:24  sueh
 * <p> In formatApplication increased sleep.
 * <p>
 * <p> Revision 1.32  2010/06/07 13:47:23  sueh
 * <p> In formatApplication increased sleep.
 * <p>
 * <p> Revision 1.31  2010/06/02 17:09:03  sueh
 * <p> In formatApplication, increasing wait.
 * <p>
 * <p> Revision 1.30  2010/05/28 19:10:02  sueh
 * <p> In formatApplication added a System.err.print so that I can see if it is helping.
 * <p>
 * <p> Revision 1.29  2010/05/24 17:09:50  sueh
 * <p> In formatApplication increased sleep.
 * <p>
 * <p> Revision 1.28  2010/05/21 17:24:55  sueh
 * <p> bug# 1322 In executeCommand removed a print that repeated endlessly.
 * <p> In formatApplication excreased the sleep.
 * <p>
 * <p> Revision 1.27  2010/05/18 13:51:51  sueh
 * <p> bug# 1322 Printing the current progress bar label when the --names
 * <p> parameter is used.
 * <p>
 * <p> Revision 1.26  2010/05/16 00:41:50  sueh
 * <p> bug# 1371 Reformatting one time when a field is not found.
 * <p>
 * <p> Revision 1.24  2010/05/10 20:42:22  sueh
 * <p> bug# 1358 In executeCommand doing more sleeping for wait.process to avoid
 * <p>being fooled when kill button is disabled for a second.
 * <p>
 * $Log$
 * Revision 1.55  2011/07/14 21:21:26  sueh
 * Increasing waiting in formatApplication.
 *
 * Revision 1.54  2011/07/12 03:33:27  sueh
 * In executeCommand handled a file chooser in Windows.
 *
 * Revision 1.53  2011/07/08 18:34:01  sueh
 * In formatApplication, Increasing format time.
 *
 * Revision 1.52  2011/06/29 03:13:46  sueh
 * In formatApplication increasing format wait time.
 *
 * Revision 1.51  2011/06/28 02:33:02  sueh
 * Allow an extra reformat because the advanced button could not be found in a UITest.
 *
 * Revision 1.50  2011/05/26 02:42:35  sueh
 * First format isn't always working - allow a maximum of two.
 *
 * Revision 1.49  2011/05/25 14:20:12  sueh
 * Increasing wait after formatting application.
 *
 * Revision 1.48  2011/05/24 23:07:31  sueh
 * Bug# 1478 In assertField, fixed combo box comparison.
 *
 * Revision 1.47  2011/05/24 15:49:39  sueh
 * FormatApplication was not being called in all cases - fixed.
 *
 * Revision 1.46  2011/05/23 16:08:00  sueh
 * Trying to get formatApplication to solve the missing Advanced button problem more often.  Increased
 * the wait after formatting.  Made it easy to change the number of formats done.
 *
 * Revision 1.45  2011/05/20 21:49:56  sueh
 * Trying to get formatApplication to solve the missing Advanced button problem more often.
 *
 * Revision 1.44  2011/05/13 03:47:14  sueh
 * Increase formatApplication sleep.
 *
 * Revision 1.43  2011/02/22 21:51:07  sueh
 * bug# 1437 Reformatting.
 *
 * Revision 1.42  2010/11/13 16:08:08  sueh
 * bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 *
 * Revision 1.41  2010/11/10 20:56:59  sueh
 * In executeCommand slowing down assert.exists.file.  In Windows it
 * may have run too fast to avoid file locks.
 *
 * Revision 1.40  2010/11/09 21:54:53  sueh
 * In executeCommand slowing down assert.exists.file.  In Windows it
 * may have run too fast to avoid file locks.
 *
 * Revision 1.39  2010/11/09 01:33:07  sueh
 * In executeCommand slowing down assert.not-exists.file.  In Windows it
 * may have run too fast to avoid file locks.
 *
 * Revision 1.38  2010/06/12 00:50:36  sueh
 * bug# 1383 Try a REDRAW_WAIT of 3.
 *
 * Revision 1.37  2010/06/10 21:58:45  sueh
 * bug# 1383 Added REDRAW_WAIT.  Using it with fields that may require a
 * panel to be redrawn.
 *
 * Revision 1.36  2010/06/09 21:27:48  sueh
 * bug# 1383 In executeField increased the button sleep to 3 milliseconds.
 *
 * Revision 1.35  2010/06/08 19:04:33  sueh
 * bug# 1383 Adding format action.  Changed formatApplication sleep to a quarter of a second.  Increased the checkbox and radio button sleeps to
 * 3 milliseconds.
 *
 * Revision 1.34  2010/06/08 16:36:55  sueh
 * In formatApplication increased sleep.
 *
 * Revision 1.33  2010/06/07 14:54:24  sueh
 * In formatApplication increased sleep.
 *
 * Revision 1.32  2010/06/07 13:47:23  sueh
 * In formatApplication increased sleep.
 *
 * Revision 1.31  2010/06/02 17:09:03  sueh
 * In formatApplication, increasing wait.
 *
 * Revision 1.30  2010/05/28 19:10:02  sueh
 * In formatApplication added a System.err.print so that I can see if it is helping.
 *
 * Revision 1.29  2010/05/24 17:09:50  sueh
 * In formatApplication increased sleep.
 *
 * Revision 1.28  2010/05/21 17:24:55  sueh
 * bug# 1322 In executeCommand removed a print that repeated endlessly.
 * In formatApplication excreased the sleep.
 *
 * Revision 1.27  2010/05/18 13:51:51  sueh
 * bug# 1322 Printing the current progress bar label when the --names
 * parameter is used.
 *
 * Revision 1.26  2010/05/16 00:41:50  sueh
 * bug# 1371 Reformatting one time when a field is not found.
 *
 * Revision 1.25  2010/05/15 17:41:17  sueh
 * bug# 1358 For Windows using a 1 second wait after a button is pressed
 * instead of a 1 millisecond wait.
 * <p>
 * Revision 1.24 2010/05/10 20:42:22 sueh
 * <p>
 * bug# 1358 In executeCommand doing more sleeping for wait.process to avoid
 * being fooled when kill button is disabled for a second.
 * <p>
 * <p>
 * Revision 1.23 2010/04/29 01:36:30 sueh
 * <p>
 * <p> Revision 1.23  2010/04/29 01:36:30  sueh
 * <p> bug# 1356 In assertFileContainsString handle wildcards in the targetString.
 * <p>
 * <p> Revision 1.22  2010/03/03 05:11:20  sueh
 * <p> bug# 1311 Implemented set.debug.
 * <p>
 * <p> Revision 1.21  2010/02/24 02:57:43  sueh
 * <p> bug# 1301 Fixed null pointer exception in ifEnabled.
 * <p>
 * <p> Revision 1.20  2010/02/23 00:23:52  sueh
 * <p> bug# 1301 Fixed bug in assertFileContainsString.
 * <p>
 * <p> Revision 1.19  2010/02/17 05:04:02  sueh
 * <p> bug# 1301 Pulled enableField and ifField out of executeField.
 * <p>
 * <p> Revision 1.18  2009/11/20 17:43:01  sueh
 * <p> bug# 1282 Changed areFilesSame to assertSameFile and added the ability to compare files with different names.  Added assertFileContains.
 * <p>
 * <p> Revision 1.17  2009/10/29 20:03:24  sueh
 * <p> bug# 1280 Fix file chooser handler, which was crashing instead of waiting
 * <p> of the file chooser wasn't available.
 * <p>
 * <p> Revision 1.16  2009/10/23 19:48:43  sueh
 * <p> bug# 1275 In executeCommand implemented open.interface.field.
 * <p>
 * <p> Revision 1.15  2009/10/08 20:59:53  sueh
 * <p> bug# 1277 In executeField changed the name of minibuttons to mb.name.
 * <p>
 * <p> Revision 1.14  2009/09/28 18:37:28  sueh
 * <p> bug# 1235 In executeCommand, for wait.file-chooser, make sure that
 * <p> value isn't null and handle a fully qualified chosen_file.
 * <p>
 * <p> Revision 1.13  2009/09/22 21:04:34  sueh
 * <p> bug# 1259 Changed assert.equals.com to assert.same.file.
 * <p>
 * <p> Revision 1.12  2009/09/20 21:34:47  sueh
 * <p> bug# 1268 In executeCommand, fixed bug in ASSERT.
 * <p>
 * <p> Revision 1.11  2009/09/11 22:41:36  sueh
 * <p> bug# 1259 comparing com files by sorting them first.
 * <p>
 * <p> Revision 1.10  2009/09/06 01:40:21  sueh
 * <p> bug# 1259 Fixed a uitest bug.
 * <p>
 * <p> Revision 1.9  2009/09/01 03:18:33  sueh
 * <p> bug# 1222
 * <p>
 * <p> Revision 1.8  2009/07/13 20:19:42  sueh
 * <p> Increased sleep for wait.process.  Ubuntu may have had a problem with
 * <p> checking the process bar too soon after the kill button is disabled.
 * <p>
 * <p> Revision 1.7  2009/06/10 17:26:41  sueh
 * <p> bug# 1202, bug # 1216 Added if.not-exists.field.subcommand to handle
 * <p> RAPTOR's not existing on all computers.
 * <p>
 * <p> Revision 1.6  2009/03/17 00:46:33  sueh
 * <p> bug# 1186 Pass managerKey to everything that pops up a dialog.
 * <p>
 * <p> Revision 1.5  2009/03/02 20:58:00  sueh
 * <p> bug# 1102 Added assert.ge.tf and assert.le.tf.  Added compare
 * <p> (JTextComponent, Command) to test these commands.
 * <p>
 * <p> Revision 1.4  2009/02/20 18:14:56  sueh
 * <p> bug# 1102 Do a quick sleep after finding a panel to avoid unreliable
 * <p> behavior.
 * <p>
 * <p> Revision 1.3  2009/02/04 23:37:15  sueh
 * <p> bug# 1158 Changed id and exception classes in LogFile.
 * <p>
 * <p> Revision 1.2  2009/01/28 00:57:50  sueh
 * <p> bug# 1102 In nextSection, moving subframe out of the the way.  In executeCommand handling the ifnot subsection and if.enabled.field.subcommand.  In executeField handling if.enabled.field.subcommand.  Changed assertEnabled to enabled to handle if.enabled.field.subcommand.
 * <p>
 * <p> Revision 1.1  2009/01/20 20:44:46  sueh
 * <p> bug# 1102 Tester of autodocs, functions, and subsections.
 * <p> </p>
 */
public final class AutodocTester extends UITestAssert implements VariableList {
  private static final char WILD_CARD = '*';

  private static final int WAIT_SLEEP = 1;
  // For changing the modified date of files:
  private static final int MODIFY_FILE_TIME_SLEEP = 1000;

  //
  // For multiRun calls:
  //
  private static final int MULTI_RUNS = 30;

  private final ReadOnlyAutodoc autodoc;
  private final JFCTestHelper helper;
  private final TestRunner testRunner;
  private final AxisID axisID;
  private final String sectionType;
  private final String sectionName;
  private final File sourceDir;
  private final VariableList parentVariableList;
  private final ReadOnlySection subsection;
  private final AutodocTester parentTester;

  private static boolean all_stop = false;

  private Container currentPanel = null;
  private CommandReader reader = null;
  // Prevents the next command from being read.
  private boolean wait = false;
  private ReadOnlyAutodoc functionAutodoc = null;
  private String functionSectionType = null;
  private AutodocTester childTester = null;
  private Map globalVariableMap = null;
  private Map<String, Object> variableMap = null;
  private NamedComponentFinder namedFinder = null;
  private NamedComponentFinder namedNoWaitFinder = null;
  private ComponentFinder finder = null;
  private ComponentFinder noWaitfinder = null;
  private AbstractButtonFinder buttonFinder = null;
  private Command command = null;
  private String skipToDialogSection = null;
  private boolean debug = false;
  private Set completedDialogSections = null;
  private boolean interfaceOpen = false;
  // Allows testUntilWait to end so the other axis can run.
  private boolean yield = false;
  private ExternalUITestSuite externalUITestSuite = null;
  // True when an external function is being run.
  private boolean external = false;
  // When isDone is called, causes it to override other settings and return true.
  private boolean done = false;
  private boolean end = false;

  /**
   * Tests all the sections of sectionType in order.  This is assumed to be a
   * top-level autodoc.
   * 
   * Autodoc Testers:
   * These top level testers are associated with one autodoc (other top level
   * testers and function testers may use the same autodoc).  They are uniquely
   * identified by the axisID that is passed to them.  They attempt to test
   * every section in the autodoc that matches sectionType.  Autodoc testers
   * cannot create child function testers until they have used the set.adoc
   * command to set an autodoc and section type for function calls.  Autodoc
   * testers have no parent.  They can have a function tester or a subsection
   * tester as a child.
   * @param testRunner
   * @param helper
   * @param autodoc
   * @param sourceDir
   * @param sectionType
   * @param axisID
   * @return
   */
  static AutodocTester getAutodocTester(final TestRunner testRunner,
    final JFCTestHelper helper, final ReadOnlyAutodoc autodoc, final File sourceDir,
    final String sectionType, final AxisID axisID, final VariableList parentVariableList,
    final ExternalUITestSuite externalUITestSuite) {
    AutodocTester tester = new AutodocTester(testRunner, helper, autodoc, sourceDir,
      sectionType, null, null, axisID, parentVariableList, null, externalUITestSuite);
    tester.globalVariableMap = new HashMap();
    tester.globalVariableMap.put("axis", axisID.getExtension());
    tester.globalVariableMap.put("cap-axis-key", axisID.getCapitalizedKeyString());
    tester.completedDialogSections = new HashSet();
    // openInterface must only run once
    if (axisID == AxisID.SECOND) {
      tester.interfaceOpen = true;
    }
    return tester;
  }

  /**
   * Tests one section.  Sets the function autodoc and section type for function
   * calls from the parent.
   * 
   * Function Testers:
   * These testers are associated with one section.  This section must have a
   * unique type and name in its autodoc.  The autodoc containing the section
   * may be different from the parent autodoc, but doesn't have to be.  Function
   * testers may create child function testers, which will have the same autodoc
   * and section type for function calls as the parent, unless set.adoc is used
   * to change them.  Function testers can have any kind of tester as a parent.
   * They can have a function tester or a subsection tester as a child.
   * @param autodoc
   * @param sectionType
   * @param sectionName
   * @param parentTester
   * @return
   */
  static AutodocTester getFunctionTester(final ReadOnlyAutodoc autodoc,
    final String sectionType, final String sectionName, final AutodocTester parentTester,
    final ExternalUITestSuite externalUITestSuite) {
    AutodocTester tester = new AutodocTester(parentTester.testRunner, parentTester.helper,
      autodoc, parentTester.sourceDir, sectionType, sectionName, null,
      parentTester.axisID, parentTester, parentTester, externalUITestSuite);
    // Set up the function tester to run more functions.
    tester.functionAutodoc = autodoc;
    tester.functionSectionType = sectionType;
    tester.currentPanel = parentTester.currentPanel;
    // openInterface must only run once
    tester.interfaceOpen = true;
    return tester;
  }

  /**
   * Tests one subsection.  Sets the function autodoc and section type for
   * function calls from the parent.  Does not pass the section type into the
   * constructor since the subsection does not have to be found.
   * 
   * Subsection Testers:
   * These testers are associated with one subsection.  This subsection is
   * passed to the tester, so it doesn't have to be unique.  If the subsection
   * tester did not get an autodoc and section type for calling functions from
   * its parent, it must use the set.adoc command to set them.  Subsection
   * testers can have an autodoc tester or a function tester as a parent.  They
   * can have a function tester as a child.
   * @param subsection
   * @param parentTester
   * @return
   */
  static AutodocTester getSubsectionTester(final ReadOnlySection subsection,
    final AutodocTester parentTester, boolean interfaceOpen,
    final ExternalUITestSuite externalUITestSuite) {
    AutodocTester tester = new AutodocTester(parentTester.testRunner, parentTester.helper,
      parentTester.autodoc, parentTester.sourceDir, null, null, subsection,
      parentTester.axisID, parentTester, parentTester, externalUITestSuite);
    tester.functionAutodoc = parentTester.functionAutodoc;
    tester.functionSectionType = parentTester.functionSectionType;
    tester.currentPanel = parentTester.currentPanel;
    // openInterface must only run once
    tester.interfaceOpen = interfaceOpen;
    return tester;
  }

  /**
   * @param testRunner
   * @param helper
   * @param autodoc
   * @param sourceDir
   * @param sectionType
   * @param sectionName
   * @param subsection
   * @param axisID
   * @param parentVariableList
   * @param parentTester
   */
  private AutodocTester(final TestRunner testRunner, final JFCTestHelper helper,
    final ReadOnlyAutodoc autodoc, final File sourceDir, final String sectionType,
    final String sectionName, final ReadOnlySection subsection, final AxisID axisID,
    final VariableList parentVariableList, final AutodocTester parentTester,
    final ExternalUITestSuite externalUITestSuite) {
    this.testRunner = testRunner;
    this.helper = helper;
    this.autodoc = autodoc;
    this.axisID = axisID;
    this.sourceDir = sourceDir;
    this.sectionType = sectionType;
    this.sectionName = sectionName;
    this.subsection = subsection;
    this.parentVariableList = parentVariableList;
    this.parentTester = parentTester;
    this.externalUITestSuite = externalUITestSuite;
  }

  /**
   * A tester is done when it has no childTester and its reader is done.  Or if an end
   * command was executed, then its always done.
   * 
   * @return
   */
  boolean isDone() {
    if (all_stop && (reader == null || !reader.isDone())) {
      stopAllInstances();
    }
    if (done) {
      return true;
    }
    if (reader == null || wait || childTester != null) {
      return false;
    }
    return reader.isDone();
  }

  /**
   * Passes a variable up to the enclosing scope, if it exists, and exits the
   * current scope.
   * @param variableName
   */
  private void setReturn(final String variableName) {
    if (variableMap != null && variableMap.containsKey(variableName)) {
      parentVariableList.setVariable(variableName, variableMap.get(variableName));
    }
    setReturn();
  }

  /**
   * Exit from the current scope.
   */
  private void setReturn() {
    if (all_stop && (reader == null || !reader.isDone())) {
      stopAllInstances();
    }
    if (sectionType == null || sectionName != null) {
      // Either a function tester or a subsection test so just set
      // reader.done=true to leave the scope.
      reader.setDone();
    }
    else {
      // This is an autodoc tester so make testUntilWait() call nextSection().
      command = null;
    }
  }

  /**
   * Exit from all scopes and end the test.  Make sure that isDone() returns
   * true.
   */
  private void setEnd() {
    if (all_stop && (reader == null || !reader.isDone())) {
      stopAllInstances();
    }
    reader.setDone();
    wait = false;
    if (childTester != null) {
      // The childTester is active, so this call will have originated from it.
      // Set the childTester to null so that isDone() will return true.
      childTester = null;
    }
    if (parentTester != null) {
      // This is either a function tester or a subsection tester so must tell
      // parent to end.
      parentTester.setEnd();
    }
    done = true;
  }

  void setDebug() {
    debug = true;
  }

  /**
   * A tester is in a yield state if the yield member variable is true, unless it
   * has a childTester; in that case the yield state is dependent on the child
   * tester's yield state.
   * @return
   */
  private boolean isYield() {
    if (all_stop) {
      stopAllInstances();
    }
    if (childTester != null) {
      return childTester.isYield();
    }
    return yield;
  }

  /**
   * Processes commands until a wait state is encountered or the test is done.
   * 
   * When an open.frame command exists for this tester:
   * The first time this function is run it attempts to open the frame.  If this
   * fails, it goes into a wait state and will continue to try to open the frame
   * each time it runs until it succeeds.
   * @throws FileNotFoundException
   * @throws IOException
   * @throws LogFile.ReadException
   * @throws LogFile.FileException
   */
  void testUntilWait() throws FileNotFoundException, IOException, LogFile.LockException,
    ClassNotFoundException {
    if (all_stop && (reader == null || !reader.isDone())) {
      stopAllInstances();
    }
    boolean openingInterface = false;
    if (!interfaceOpen) {
      // OpeningInterface is set to true if the open interface subsection is
      // going to be run.
      openingInterface = openInterface() && childTester != null;
    }
    if (reader == null) {
      // first time running testUntilWait.
      nextSection();
    }
    if (reader.isDone()) {
      return;
    }
    if (interfaceOpen) {
      gotoFrame();
    }
    while (!reader.isDone()) {
      if (all_stop && (reader == null || !reader.isDone())) {
        stopAllInstances();
      }
      do {
        if (all_stop && (reader == null || !reader.isDone())) {
          stopAllInstances();
        }
        if (!external) {
          // Don't get a new command until the current function is completed.
          if (childTester != null) {
            childTester.testUntilWait();
            if (openingInterface && (childTester == null || childTester.isDone())) {
              // Successfully opened the interface.
              openingInterface = false;
              interfaceOpen = true;
              gotoFrame();
            }
            // SetEnd() could have set the childTester to null.
            if (childTester != null) {
              if (childTester.isDone()) {
                childTester = null;
              }
              else if (childTester.isYield()) {
                return;
              }
            }
          }
          // Before getting a new command, wait for the current command if it is a
          // wait command.
          if (!wait) {
            command = reader.nextCommand(command);
          }
          if (command != null && command.isKnown()) {
            executeCommand(command);
          }
        }
        // Yield allows the other axis to run. Only the wait.test command
        // currently needs this.
        if (yield) {
          return;
        }
      } while (!reader.isDone() && command != null && skipToDialogSection == null);
      nextSection();
    }
  }

  /**
   * Get the next section or starts the reader (which gets the first section).
   * For Autodoc testers it also removes section-level
   * scope.
   * @throws FileNotFoundException
   * @throws IOException
   * @throws LogFile.ReadException
   * @throws LogFile.FileException
   */
  private void nextSection()
    throws FileNotFoundException, IOException, LogFile.LockException {
    if (all_stop && (reader == null || !reader.isDone())) {
      stopAllInstances();
    }
    boolean isAutodocTester = sectionType != null && sectionName == null;
    do {
      // Get the next section
      if (reader == null) {
        if (sectionType == null) {
          // Gets a reader for the subsection that was passed in
          reader = CommandReader.getSubsectionReader(autodoc, subsection, axisID, this);
        }
        else if (sectionName == null) {
          // Opens the first section to be read.
          reader = CommandReader.getAutodocReader(autodoc, sectionType, axisID, this);
          if (skipToDialogSection != null
            && skipToDialogSection.equals(reader.getSectionName())) {
            // Found the starting dialog - stop skipping
            skipToDialogSection = null;
          }
        }
        else {
          // open function section
          reader = CommandReader.getSectionReader(autodoc, sectionType, sectionName,
            axisID, this);
          assertFalse(
            "called function that doesn't exist - " + sectionType + " = " + sectionName
              + " - in " + autodoc.getName() + formatCommandInfo(command),
            reader.isDone());
        }
      }
      else if (!reader.isDone()) {
        if (completedDialogSections != null) {
          // add to list of completed (and skipped) dialog sections
          completedDialogSections.add(reader.getSectionName());
        }
        reader.nextSection();
      }
      if (reader.isDone()) {
        return;
      }
      // If this is a top level autodoc, try to turn off skipping
      if (skipToDialogSection != null && isAutodocTester
        && skipToDialogSection.equals(reader.getSectionName())) {
        // Found the section to skip to - stop skipping
        skipToDialogSection = null;
      }
      // If section type is not null and section name is null then this is an
      // autodoc tester. This means that
      // it tests multiple sections in a Dialog autodoc, each with it's own scope.
      // It also means that each section is associated with a different dialog as
      // described in the interface section.
      if (isAutodocTester) {
        // Remove section level scope
        // function scope
        functionAutodoc = null;
        functionSectionType = null;
        // Variable scope
        if (variableMap != null && variableMap.size() > 0) {
          variableMap.clear();
        }
      }
    } while (skipToDialogSection != null);
  }

  /**
   * Execute required command to get to the frame panel for the current axis.
   * Called every time testUntilWait is called.
   * @throws FileNotFoundException
   * @throws IOException
   * @throws LogFile.ReadException
   */
  private void gotoFrame() throws FileNotFoundException, IOException,
    LogFile.LockException, ClassNotFoundException {
    executeCommand(testRunner.getInterfaceSection().getGotoFrameCommand(axisID));
  }

  /**
   * Execute the optional subsection to open the interface (open = interface).
   * @throws FileNotFoundException
   * @throws IOException
   * @throws LogFile.ReadException
   * @return true if the function completed
   */
  private boolean openInterface() throws FileNotFoundException, IOException,
    LogFile.LockException, ClassNotFoundException {
    if (parentTester != null || axisID == AxisID.SECOND) {
      // Only the first top-level tester is allowed to open the interface.
      return false;
    }
    executeCommand(testRunner.getInterfaceSection().getOpenInterfaceCommand());
    return true;
  }

  private void openTool(final String toolName, final String formattedCommand) {
    if (toolName == null) {
      fail("tool name reqired" + formattedCommand);
    }
    BaseManager manager = null;
    if ((manager = openTool(toolName, ToolType.FLATTEN_VOLUME)) != null) {
      testRunner.setCurrentManager(manager);
    }
    else if ((manager = openTool(toolName, ToolType.GPU_TILT_TEST)) != null) {
      testRunner.setCurrentManager(manager);
    }
    else {
      fail("unknown tool" + formattedCommand);
    }
  }

  private BaseManager openTool(final String toolName, final ToolType toolType) {
    if (toolType != null && toolType.equals(toolName)) {
      return EtomoDirector.INSTANCE.openTool(toolType);
    }
    return null;
  }

  /**
   * Execute optional command to open a dialog called dialogName.  Called when "open.dialog ="
   * executed.
   * @throws FileNotFoundException
   * @throws IOException
   * @throws LogFile.ReadException
   */
  private void openDialog(final String dialogName) throws FileNotFoundException,
    IOException, LogFile.LockException, ClassNotFoundException {
    executeCommand(testRunner.getInterfaceSection().getOpenDialogCommand(dialogName));
  }

  private String formatCommandInfo(final UITestCommandInfo command) {
    if (command != null) {
      return command.formatCommandInfo(axisID);
    }
    return " (" + axisID.getExtension() + ": null)";
  }

  /**
   * Executes an action command.  Returns without doing anything for  null
   * command.  If the action command read a field command, it will call
   * executeFieldCommand.
   * @param command
   * @return false if an optional command fails, otherwise return true or fail an assert
   */
  private void executeCommand(final Command command) throws FileNotFoundException,
    IOException, LogFile.LockException, ClassNotFoundException {
    if (command == null || !command.isKnown()) {
      return;
    }
    BaseManager manager = EtomoDirector.INSTANCE.getCurrentManagerForTest();
    UITestActionType actionType = command.getActionType();
    UITestSubjectType subjectType = command.getSubjectType();
    UITestModifierType modifierType = command.getModifierType();
    Subject subject = command.getSubject();
    Command subcommand = command.getSubcommand();
    final String subjectName = subject != null ? subject.getName() : null;
    Field field = command.getField();
    final String value = command.getValue();
    String formattedCommand = formatCommandInfo(command);
    // FieldInterface
    if (actionType == null) {
      executeField(command, formattedCommand);
    }
    // ASSERT
    else if (actionType == UITestActionType.ASSERT) {
      // assert.file
      if (subjectType == UITestSubjectType.FILE
        || subjectType == UITestSubjectType.IMAGE_FILE) {
        String tempFileName = command.getValue(0);
        if (subjectType == UITestSubjectType.IMAGE_FILE) {
          tempFileName = convertToStandardizedFileName(tempFileName, formattedCommand);
        }
        String fileName = tempFileName;
        File file = new File(System.getProperty("user.dir"), fileName);
        // assert.contains.file =
        if (modifierType == UITestModifierType.CONTAINS) {
          assertFileContainsString(fileName, command.getValue(1), formattedCommand);
        }
        // assert.exists.file = file_name
        else if (modifierType == UITestModifierType.EXISTS) {
          assertTrue("file does not exist - " + fileName + formattedCommand,
            file.exists());
        }
        // assert.not-exists.file = file_name
        else if (modifierType == UITestModifierType.NOT_EXISTS) {
          assertFalse("file exists - " + fileName + formattedCommand, file.exists());
        }
        // assert.same.file = file_name
        else if (modifierType == UITestModifierType.SAME) {
          String compareToFileName = command.getValue(1);
          if (subjectType == UITestSubjectType.IMAGE_FILE) {
            compareToFileName =
              convertToStandardizedFileName(compareToFileName, formattedCommand);
          }
          assertSameFile(manager, fileName, compareToFileName, formattedCommand);
        }
        else {
          fail("unexpected command" + formattedCommand);
        }
      }
      // assert.field = value
      // assert.contains.tf = search_pattern
      else if (field != null) {
        assertField(command, formattedCommand);
      }
      else {
        fail("unexpected command" + formattedCommand);
      }
    }
    // COPY
    else if (actionType == UITestActionType.COPY) {
      assertTrue(
        "only the always modifier is allowed with this actionType" + formattedCommand,
        modifierType == null || modifierType == UITestModifierType.ALWAYS);
      // copy.file = file_name
      assertEquals("can only copy a file" + formattedCommand, UITestSubjectType.FILE,
        subjectType);
      copyFile(command.getValue(0), command.getValue(1),
        modifierType == UITestModifierType.ALWAYS, formattedCommand);
    }
    // END
    else if (actionType == UITestActionType.END) {
      setEnd();
    }
    // FORMAT
    else if (actionType == UITestActionType.FORMAT) {
      formatApplication();
      // format.field
      if (field != null) {
        executeField(command, formattedCommand);
      }
    }
    // IF
    else if (actionType == UITestActionType.IF) {
      // [[if = variable]]
      if (command.isSubsection()) {
        assertNull("modifier not used with this actionType" + formattedCommand,
          modifierType);
        assertFalse("illegal section name - " + value + formattedCommand,
          value.startsWith("="));
        if (isVariableSet(value, axisID)) {
          childTester = AutodocTester.getSubsectionTester(command.getSubsection(), this,
            interfaceOpen, externalUITestSuite);
        }
      }
      // if.var
      else if (subjectType == UITestSubjectType.VAR) {
        boolean variableSet = isVariableSet(subjectName, axisID);
        if (modifierType == null) {
          if (variableSet) {
            // if.var.action
            if (subcommand != null) {
              executeCommand(subcommand);
            }
            // if.var.field
            else {
              executeField(command, formattedCommand);
            }
          }
        }
        // if.not.var
        else if (modifierType == UITestModifierType.NOT) {
          if (!variableSet) {
            // if.not.var.action
            if (subcommand != null) {
              executeCommand(subcommand);
            }
            // if.not.var.field
            else {
              executeField(command, formattedCommand);
            }
          }
        }
        else {
          String variableValue = getVariableValue(subjectName, axisID);
          assertNotNull(
            "subcommand required in an if.comparison command" + formattedCommand,
            subcommand);
          // if.equals.var.variable_name.subcommand = variable_value
          if (modifierType == UITestModifierType.EQUALS) {
            if (variableValue.equals(value)) {
              executeCommand(subcommand);
            }
          }
          // if.not-equals.var.variable_name.subcommand = variable_value
          else if (modifierType == UITestModifierType.NOT_EQUALS) {
            if (!variableValue.equals(value)) {
              executeCommand(subcommand);
            }
          }
        }
      }
      // if.enabled.field
      // if.disabled.field
      else if (subjectType == null) {
        ifField(command, formattedCommand);
      }
    }
    // IFNOT
    else if (actionType == UITestActionType.IFNOT) {
      // [[ifnot = variable]]
      if (command.isSubsection()) {
        assertNull("modifier not used with this actionType" + formattedCommand,
          modifierType);
        assertFalse("illegal section name - " + value + formattedCommand,
          value.startsWith("="));
        if (!isVariableSet(value, axisID)) {
          childTester = AutodocTester.getSubsectionTester(command.getSubsection(), this,
            interfaceOpen, externalUITestSuite);
        }
      }
      else {
        fail("unexpected command" + formattedCommand);
      }
    }
    // GET
    else if (actionType == UITestActionType.GET) {
      // get.instance.class_tag
      assertNull("modifier not used with this actionType" + formattedCommand,
        modifierType);
      assertEquals("unknown get command - expecting 'instance'", subjectType,
        UITestSubjectType.INSTANCE);
      assertNotNull("missing class_tag" + formattedCommand, subjectName);
      assertNull("value not used with this actionType" + formattedCommand, value);
      externalUITestSuite =
        ExternalUITestSuitFactory.getUITestSuiteInstance(subjectName, this);
    }
    // GOTO
    else if (actionType == UITestActionType.GOTO) {
      assertNull("modifier not used with this actionType" + formattedCommand,
        modifierType);
      // goto.frame
      if (subjectType == UITestSubjectType.FRAME) {
        executeField(command, formattedCommand);
      }
      else {
        fail("unexpected command" + formattedCommand);
      }
    }
    // OPEN
    else if (actionType == UITestActionType.OPEN) {
      assertNull("modifier not used with this actionType" + formattedCommand,
        modifierType);
      // [[open = interface]]
      if (command.isSubsection()) {
        assertTrue(
          "the only legal value for an open subsection is interface" + formattedCommand,
          value.equals(UITestSubjectType.INTERFACE.toString()));
        assertFalse("illegal section name - " + value + formattedCommand,
          value.startsWith("="));
        childTester = AutodocTester.getSubsectionTester(command.getSubsection(), this,
          interfaceOpen, externalUITestSuite);
      }
      // open.dialog
      else if (subjectType == UITestSubjectType.DIALOG) {
        assertNotNull("dialog name is required" + formattedCommand, subjectName);
        // open.dialog
        if (field == null) {
          // refers to the interface section
          openDialog(subjectName);
        }
        // open.dialog.field
        else {
          // probably a command from the interface section
          executeField(command, formattedCommand);
        }
      }
      // open.tool
      else if (subjectType == UITestSubjectType.TOOL) {
        assertNotNull("tool name is required" + formattedCommand, subjectName);
        // open.tool.tool-menu-choice
        if (field == null) {
          // refers to the interface section
          openTool(subjectName, formattedCommand);
        }
        else {
          fail("unexpected command" + formattedCommand);
        }
      }
    }
    // PAUSE
    else if (actionType == UITestActionType.PAUSE) {
      if (subjectType == null) {
        pause();
      }
      else {
        assertEquals("only axis subject type is valid for pause" + formattedCommand,
          UITestSubjectType.AXIS, subjectType);
        assertNull("subject name is not valid for pause" + formattedCommand, subjectName);
        AxisID subjectAxisID = subject.getAxisID();
        assertNotNull("axis ID is required with pause.axis" + formattedCommand,
          subjectAxisID);
        if (subjectAxisID == axisID) {
          pause();
        }
      }
    }
    // PRINT
    else if (actionType == UITestActionType.PRINT) {
      // print.var.variable_name
      if (subjectType == UITestSubjectType.VAR) {
        assertNotNull("missing variable name" + formattedCommand, subjectName);
      }
      if (!isVariableSet(subjectName, axisID)) {
        System.err.println("###>>>>>>> variable " + subjectName + " is not set.");
      }
      else {
        System.err.println("###>>>>>>> variable " + subjectName + " = "
          + getVariableValue(subjectName, axisID));
      }
    }
    // RETURN
    else if (actionType == UITestActionType.RETURN) {
      // return.var.variable_name
      if (subjectType == UITestSubjectType.VAR) {
        assertNotNull("missing variable name" + formattedCommand, subjectName);
        setReturn(subjectName);
      }
      else {
        // return
        setReturn();
      }
    }
    // RUN
    else if (actionType == UITestActionType.RUN) {
      if (subjectType == UITestSubjectType.FUNCTION) {
        // run.external.function.function_name
        if (modifierType == UITestModifierType.EXTERNAL) {
          assertNotNull("missing function name" + formattedCommand, subjectName);
          assertNull("value not used with this actionType" + formattedCommand, value);
          // Prevent uitest command functionality while the external test function is
          // running.
          external = true;
          ExternalUITestSuitFactory.runFunction(externalUITestSuite, subjectName);
          external = false;
        }
        else {
          // run.function.section_name
          if (debug) {
            System.err.println(
              "run function " + command + ",functionSectionType=" + functionSectionType);
          }
          assertNull("modifier not used with this actionType" + formattedCommand,
            modifierType);
          assertNotNull("missing section name" + formattedCommand, subjectName);
          assertNotNull("missing function autodoc - run set.adoc" + formattedCommand,
            functionAutodoc);
          assertNotNull("missing function section type - run set.adoc" + formattedCommand,
            functionSectionType);
          childTester = AutodocTester.getFunctionTester(functionAutodoc,
            functionSectionType, subjectName, this, externalUITestSuite);
          if (debug) {
            childTester.setDebug();
          }
        }
      }
    }
    // SAVE
    else if (actionType == UITestActionType.SAVE) {
      // save
      assertNull("subject not used with this actionType" + formattedCommand, subject);
      assertNull("modifier not used with this actionType" + formattedCommand,
        modifierType);
      assertNull("field not used with this actionType" + formattedCommand, field);
      assertNull("value not used with this actionType" + formattedCommand, value);
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException e) {}
      UIHarness.INSTANCE.save(axisID);
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException e) {}
    }
    // SET
    else if (actionType == UITestActionType.SET) {
      assertNull("modifier not used with this actionType" + formattedCommand,
        modifierType);
      // set.adoc.section_type
      if (subjectType == UITestSubjectType.ADOC) {
        assertNotNull("missing section type" + formattedCommand, subjectName);
        functionSectionType = subjectName;
        // set.adoc.section_type =
        if (value == null) {
          functionAutodoc = autodoc;
        }
        // set.adoc.section_type = autodoc_name
        else {
          functionAutodoc =
            AutodocFactory.getTestInstance(manager, sourceDir, value, AxisID.ONLY);
        }
      }
      // set.debug
      else if (subjectType == UITestSubjectType.DEBUG) {
        debug = convertToBoolean(value, formattedCommand);
        testRunner.setDebug(debug);
      }
      // set.index
      else if (subjectType == UITestSubjectType.INDEX) {
        executeField(command, formattedCommand);
      }
      // set.file
      else if (subjectType == UITestSubjectType.FILE) {
        executeField(command, formattedCommand);
      }
      // set.var.variable_name
      else if (subjectType == UITestSubjectType.VAR) {
        assertNotNull("missing variable name" + formattedCommand, subjectName);
        if (variableMap == null) {
          variableMap = new HashMap();
        }
        variableMap.put(subjectName, value);
      }
      else {
        fail("unexpected command" + formattedCommand);
      }
    }
    // SKIPTO
    else if (actionType == UITestActionType.SKIPTO) {
      assertNull("modifier not used with this actionType" + formattedCommand,
        modifierType);
      // skipto.dialog.dialog_section_name
      if (subjectType == UITestSubjectType.DIALOG) {
        assertNotNull("dialog name is missing" + formattedCommand, subjectName);
        skipToDialogSection = subjectName;
      }
      else {
        fail("unexpected command" + formattedCommand);
      }
    }
    // SLEEP
    else if (actionType == UITestActionType.SLEEP) {
      assertNull("modifier not used with this actionType" + formattedCommand,
        modifierType);
      assertNull("subject not used with this actionType" + formattedCommand, subjectType);
      assertNull("field not used with this actionType" + formattedCommand, field);
      EtomoNumber interval = new EtomoNumber();
      // sleep = interval
      if (value != null) {
        interval.set(value);
      }
      // sleep =
      else {
        interval.set(1000);
      }
      try {
        Thread.sleep(interval.getInt());
      }
      catch (InterruptedException e) {}
    }
    // TOUCH
    else if (actionType == UITestActionType.TOUCH) {
      assertTrue(
        "only the always modifier is allowed with this actionType" + formattedCommand,
        modifierType == null);
      // touch.file = file_name
      if (subjectType == UITestSubjectType.FILE) {
        touchFile(command.getValue(0));
      }
      else if (subjectType == UITestSubjectType.DIR) {
        touchDir(command.getValue(0));
      }
      else {
        fail("can only touch a file or a directory" + formattedCommand);
      }
    }
    // UNSET
    else if (actionType == UITestActionType.UNSET) {
      // unset.var.variable_name
      if (subjectType == UITestSubjectType.VAR) {
        assertNotNull("missing variable name" + formattedCommand, subjectName);
        unsetVariable(subjectName, axisID);
      }
      else {
        fail("unexpected command" + formattedCommand);
      }
    }
    // WAIT
    else if (actionType == UITestActionType.WAIT) {
      assertNotNull("value is required" + formattedCommand, value);
      assertNull("modifier not used with this actionType" + formattedCommand,
        modifierType);
      // wait.popup.popup_title = dismiss_button_label
      if (subjectType == null) {
        // Popups that come up during processes have to be waited for. Other popups come
        // up too quickly to be waited for.
        executeField(command, formattedCommand);
      }
      // wait.process
      else if (subjectType == UITestSubjectType.PROCESS) {
        waitProcess(subjectName, value, formattedCommand);
      }
      // wait.test
      else if (subjectType == UITestSubjectType.TEST) {
        if (testRunner.isDialogSectionComplete(subject.getAxisID(), value,
          formattedCommand)) {
          wait = false;
          yield = false;
        }
        else {
          if (testRunner.isDone(subject.getAxisID())) {
            setEnd();
          }
          else {
            wait = true;
            yield = true;
            try {
              Thread.sleep(WAIT_SLEEP);
            }
            catch (InterruptedException e) {}
          }
        }
      }
      else {
        fail("unexpected command" + formattedCommand);
      }
    }
    // WRITE
    // write.file
    else if (actionType == UITestActionType.WRITE) {
      appendStringToFile(command.getValue(0), command.getValue(1), formattedCommand);
    }
    else if (!actionType.isNoOp()) {
      fail("unexpected command" + formattedCommand);
    }
    return;
  }

  private void findPopup(final boolean waitAction, final String subjectName,
    final String value, final String formattedCommand) {
    // wait.popup.popup_title = dismiss_button_label
    // popup.popup_title = dismiss_button_label
    assertNotNull("popup name is required" + formattedCommand, subjectName);
    assertNotNull("button to close popup is required" + formattedCommand, value);
    // Try to get the pop up.
    setupComponentFinder(JOptionPane.class);
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        Container popup = (Container) finder.find();
        assertNotNull("Popup did not pop up" + formattedCommand, popup);
        // Make sure its the right popup
        String popupName = popup.getName();
        if (popupName == null || !popupName.equals(subjectName)) {
          // Close incorrect popup and fail
          helper.sendKeyAction(new KeyEventData(testRunner, popup, KeyEvent.VK_ESCAPE));
          fail("wrong popup - " + popupName + formattedCommand);
        }
        // close popup
        setupAbstractButtonFinder(value);
        AbstractButton button = (AbstractButton) buttonFinder.find(popup, 0);
        assertNotNull(
          "unable to find button to close popup - " + value + formattedCommand, button);
        assertTrue("popup button is not enabled" + formattedCommand, button.isEnabled());
        assertTrue("popup button is not visible" + formattedCommand,
          new MouseEventData(testRunner, button).prepareComponent());
        helper.enterClickAndLeave(new MouseEventData(testRunner, button));
      }
    };
    if (waitAction) {
      multiRun(runnable, null);
    }
    else {
      runnable.run();
    }
  }

  /**
   * Returns true if the endStateString shows the ETC overflow bug:
   * "0% ETC: 2147483647:2147483647".
   * Bug# 2334
   * @param endStateString
   * @return
   */
  private boolean isEtcOverflowMonitorBug(final JButton killButton,
    final String endStateString) {
    if (killButton == null || killButton.isEnabled() || endStateString == null
      || !endStateString.matches("\\s*0%\\s*ETC:\\s*2147483647:2147483647\\s*")) {
      return false;
    }
    System.err
      .println("Ignoring Bug# 2334 ETC process watcher completes with wrong message");
    return true;
  }

  /**
   * Not in use now because it now happens when the busy icon is on, and I haven't seen it
   * lately so it might be caused by an earlier version of uitest.
   * 
   * Returns true if the
   * state of progress panel matches this bug (where the monitor
   * missed the "Done" statement in processchunks.out).
   * Bug# 2336
   * @param progressBarLabel
   * @param endStateString
   * @return
   */
  private boolean isProcesschunksDoneMonitorBug(final JButton killButton,
    final JLabel progressBarLabel, final JProgressBar progressBar) {
    String actualProcessName = Utilities.convertLabelToName(progressBarLabel.getText());
    // If this bug occurred, the Kill Process button will be
    // enabled, and the progress bar label will start with "Processchunks". The busy
    // icon is most likely enabled because it covers the monitor as well as the process.
    if (killButton == null || !killButton.isEnabled() || actualProcessName == null
      || !actualProcessName.trim().startsWith("Processchunks") || progressBar == null) {
      return false;
    }
    String endStateString = progressBar.getString();
    if (endStateString == null) {
      return false;
    }
    String[] array = endStateString.split("\\s*");
    try {
      // If this bug occurred, the progress bar will say: "n of n completed".
      if (array == null || array.length != 4
        || Integer.parseInt(array[0]) != Integer.parseInt(array[2])
        || !array[1].equals("of") || !array[3].equals("completed")) {
        return false;
      }
    }
    catch (NumberFormatException e) {
      return false;
    }
    System.err.println("Ignoring Bug# 2336 Processchunks watcher never says Done");
    return true;
  }

  /**
   * Returns true if progressString contains an end string like "done" or "killed".
   * @param progressString
   * @param expectedString
   * @param modifierType
   * @return
   */
  private boolean isEndProgressString(final String progressString,
    final String expectedString) {
    if (ProcessEndState.isValid(progressString)) {
      return true;
    }
    if (progressString == null) {
      return false;
    }
    return progressString.indexOf(expectedString) != -1;
  }

  /**
   * touch a file in the working directory.
   * @param fileName
   */
  private void touchFile(final String fileName) {
    if (fileName == null || fileName.matches("\\s*")) {
      return;
    }
    try {
      Thread.sleep(MODIFY_FILE_TIME_SLEEP);
    }
    catch (InterruptedException e) {}
    touch(new File(System.getProperty("user.dir"), fileName));
  }

  /**
   * touch a directory in the working directory if it exists, otherwise create it.
   * @param fileName
   */
  private void touchDir(final String dirName) {
    if (dirName == null || dirName.matches("\\s*")) {
      return;
    }
    File dir = new File(System.getProperty("user.dir"), dirName);
    if (dir.exists()) {
      touch(dir);
    }
    else {
      dir.mkdirs();
    }
  }

  private void touch(final File file) {
    SystemProgram program =
      new SystemProgram(
        null, System.getProperty("user.dir"), new String[] { "python",
          BaseManager.getIMODBinPath() + "b3dtouch", file.getAbsolutePath() },
        AxisID.ONLY);
    program.run();
  }

  boolean isDialogSectionComplete(final String dialogSection,
    final String formattedCommand) {
    assertNotNull(
      "this function should be called in only top level testers" + formattedCommand,
      completedDialogSections);
    return completedDialogSections.contains(dialogSection);
  }

  public AxisID getAxisID() {
    return axisID;
  }

  public void setVariable(final String variableName, final Object variableValue) {
    if (variableMap == null) {
      variableMap = new HashMap();
    }
    variableMap.put(variableName, variableValue);
  }

  /**
   * Attempts to find variableName in variableMap.  It checks the global map.
   * If the variable isn't there, is calls this function in the parent variable
   * list.
   * @param variableName
   * @return variableValue
   */
  public String getVariableValue(final String variableName, final AxisID axisID) {
    if (variableMap != null && variableMap.containsKey(variableName)) {
      return (String) variableMap.get(variableName);
    }
    if (globalVariableMap != null && globalVariableMap.containsKey(variableName)) {
      return (String) globalVariableMap.get(variableName);
    }
    return parentVariableList.getVariableValue(variableName, axisID);
  }

  public void unsetVariable(final String variableName, final AxisID axisID) {
    if (variableMap != null && variableMap.containsKey(variableName)) {
      variableMap.remove(variableName);
    }
    if (globalVariableMap != null && globalVariableMap.containsKey(variableName)) {
      globalVariableMap.remove(variableName);
    }
    parentVariableList.unsetVariable(variableName, axisID);
  }

  /**
   * Checks to see if variableMap contains the key variableName.  If it doesn't
   * calls the parent doesVariableExist.
   * @param variableName
   * @return variableExists
   */
  public boolean isVariableSet(final String variableName, final AxisID axisID) {
    if (variableMap != null && variableMap.containsKey(variableName)) {
      return true;
    }
    if (globalVariableMap != null && globalVariableMap.containsKey(variableName)) {
      return true;
    }
    return parentVariableList.isVariableSet(variableName, axisID);
  }

  public String toString() {
    if (sectionName == null) {
      return autodoc.getName() + "," + sectionType;
    }
    return autodoc.getName() + "," + sectionType + "," + sectionName;
  }

  /**
   * Handle if.enabled/disabled.  Checks Component.isEnabled.  Fails if the
   * action isn't "if".
   * @param component
   * @param command
   */
  private void ifEnabled(final Component component, final Command command,
    final String formattedCommand) throws FileNotFoundException, IOException,
    LogFile.LockException, ClassNotFoundException {
    UITestActionType actionType = command.getActionType();
    UITestModifierType modifierType = command.getModifierType();
    boolean enabled = false;
    if (modifierType == UITestModifierType.ENABLED) {
      enabled = true;
    }
    else if (modifierType == UITestModifierType.DISABLED) {
      enabled = false;
    }
    else {
      fail("unexpected command" + formattedCommand);
    }
    // if.enabled.field.subcommand
    // if.disabled.field.subcommand
    assertNotNull("cannot find component" + formattedCommand, component);
    if (actionType == UITestActionType.IF) {
      if (component.isEnabled() == enabled) {
        executeCommand(command.getSubcommand());
      }
    }
    else {
      fail("unexpected command" + formattedCommand);
    }
  }

  /**
   * Asserts modifiers enabled and disabled.
   * @param component
   * @param command
   * @return true if a recognized modifier is present
   */
  private boolean assertEnabled(final Component component, final Command command,
    final String formattedCommand) {
    UITestModifierType modifierType = command.getModifierType();
    if (modifierType != UITestModifierType.ENABLED
      && modifierType != UITestModifierType.DISABLED) {
      return false;
    }
    assertEnabled(component, modifierType == UITestModifierType.ENABLED,
      formattedCommand);
    return true;
  }

  /**
   * Asserts modifiers enabled and disabled.  Checks isEnabled and isEditable.
   * @param component
   * @param command
   * @return true if recognized modifier is present
   */
  private boolean assertEnabled(final JTextComponent textComponent, final Command command,
    final String formattedCommand) {
    UITestModifierType modifierType = command.getModifierType();
    if (modifierType != UITestModifierType.ENABLED
      && modifierType != UITestModifierType.DISABLED) {
      return false;
    }
    assertEnabled(textComponent, modifierType == UITestModifierType.ENABLED,
      formattedCommand);
    return true;
  }

  /**
   * Asserts modifiers enabled and disabled.  Uses the index with tabbed pane.
   * @param component
   * @param command
   * @return true if recognized modifier is present
   * @throws FileNotFoundException
   * @throws IOException
   * @throws LogFile.LockException
   */
  private boolean assertEnabled(final JTabbedPane tabbedPane, final Command command,
    final String formattedCommand)
    throws FileNotFoundException, IOException, LogFile.LockException {
    UITestModifierType modifierType = command.getModifierType();
    int index = command.getField().getIndex();
    if (modifierType == UITestModifierType.ENABLED) {
      assertTrue("component is not enabled" + formattedCommand,
        tabbedPane.isEnabledAt(index));
      return true;
    }
    else if (modifierType == UITestModifierType.DISABLED) {
      assertFalse("component is not disabled" + formattedCommand,
        tabbedPane.isEnabledAt(index));
      return true;
    }
    return false;
  }

  /**
   * Handle if.enabled/disabled.  Checks Component.isEnabled and
   * JTextComponent.isEditable.  Fails if the action isn't "if".
   * @param textComponent
   * @param command
   */
  private void ifEnabled(final JTextComponent textComponent, final Command command,
    final String formattedCommand) throws FileNotFoundException, IOException,
    LogFile.LockException, ClassNotFoundException {
    assertNull("assert.enabled/disabled command does not use a value" + formattedCommand,
      command.getValue());
    UITestActionType actionType = command.getActionType();
    UITestModifierType modifierType = command.getModifierType();
    boolean enabled = false;
    if (modifierType == UITestModifierType.ENABLED) {
      enabled = true;
    }
    else if (modifierType == UITestModifierType.DISABLED) {
      enabled = false;
    }
    else {
      fail("unexpected command" + formattedCommand);
    }
    // if.enabled.field.subcommand
    if (actionType == UITestActionType.IF) {
      if (enabled && textComponent.isEnabled() && textComponent.isEditable()) {
        executeCommand(command.getSubcommand());
      }
      // if.disabled.field.subcommand
      else if (!enabled && (!textComponent.isEnabled() || !textComponent.isEditable())) {
        executeCommand(command.getSubcommand());
      }
    }
    else {
      fail("unexpected command" + formattedCommand);
    }
  }

  /**
   * Compares files.  Sorts the files and them compares them line by line.
   * The sorting is necessary because the order of a command may change
   * Returns null if file is equals with uitestDataDir/storedFileName.
   * Otherwise returns an error message.  Fails if the files are not the same.
   * @param file
   * @param storedFileName
   * @throws LogFile.LockException
   * @throws FileNotFoundException
   * @throws IOException
   */
  private void assertSameFile(final BaseManager manager, final String fileName,
    final String compareToFileName, final String formattedCommand)
    throws LogFile.LockException, FileNotFoundException, IOException {
    // Create dataset file from fileName
    if (fileName == null) {
      // One or more of the files are missing.
      fail("Missing fileName" + formattedCommand);
    }
    File file = new File(System.getProperty("user.dir"), fileName);
    assertTrue(file.getAbsolutePath() + " does not exist" + formattedCommand + "\n",
      file.exists());
    assertTrue(file.getAbsolutePath() + " is not a file" + formattedCommand + "\n",
      file.isFile());

    // Create stored file from either compareToFileName or fileName.
    File storedFile;
    if (compareToFileName == null || compareToFileName.matches("\\s*")) {
      storedFile = new File(testRunner.getUitestDataDir(), fileName);
    }
    else {
      storedFile = new File(testRunner.getUitestDataDir(), compareToFileName);
    }
    assertTrue(storedFile.getAbsolutePath() + " does not exist" + formattedCommand + "\n",
      storedFile.exists());
    assertTrue(storedFile.getAbsolutePath() + " is not a file" + formattedCommand + "\n",
      storedFile.isFile());
    // Sort files because order of parameters can vary inside a command.
    if (debug) {
      System.err
        .println("assertSameFile:file.getAbsolutePath()=" + file.getAbsolutePath());
    }
    String sortCommand;
    if (Utilities.isWindowsOS()) {
      sortCommand = "C:/cygwin/bin/sort";
    }
    else {
      sortCommand = "sort";
    }
    SystemProgram sortFile = new SystemProgram(manager, System.getProperty("user.dir"),
      new String[] { sortCommand, file.getAbsolutePath() }, AxisID.ONLY);
    sortFile.run();
    String[] stdOut = sortFile.getStdOutput();
    stdOut = stripCommentsAndBlankLines(stdOut);
    if (debug) {
      System.err.println(
        "assertSameFile:storedFile.getAbsolutePath()=" + storedFile.getAbsolutePath());
    }
    SystemProgram sortStoredFile =
      new SystemProgram(manager, System.getProperty("user.dir"),
        new String[] { sortCommand, storedFile.getAbsolutePath() }, AxisID.ONLY);
    sortStoredFile.run();
    String[] storedStdOut = sortStoredFile.getStdOutput();
    storedStdOut = stripCommentsAndBlankLines(storedStdOut);
    if (stdOut == null && storedStdOut == null) {
      // Both files contains no comparable lines so they are the same.
      return;
    }
    assertTrue(
      "One file has commands(s) and the other does not(\n" + file.getAbsolutePath()
        + ",\n" + storedFile.getAbsolutePath() + "). " + formattedCommand + "\n",
      stdOut != null && storedStdOut != null);
    // Compare lengths
    if (stdOut.length != storedStdOut.length) {
      assertTrue(
        "Command(s) have unequal lengths:  " + file.getAbsolutePath() + ": "
          + stdOut.length + ",\n " + storedFile.getAbsolutePath() + ": "
          + storedStdOut.length + formattedCommand + "\n",
        stdOut.length == storedStdOut.length);
    }
    // Compare lines.
    for (int i = 0; i < stdOut.length; i++) {
      String[] line = stdOut[i].trim().split("\\s+");
      String[] storedLine = storedStdOut[i].trim().split("\\s+");
      for (int j = 0; j < line.length; j++) {
        if (j >= storedLine.length) {
          fail("Unequal line lengths:" + file.getAbsolutePath() + ":\n" + stdOut[i] + "\n"
            + storedFile.getAbsolutePath() + ":\n" + storedStdOut[i] + ".\n"
            + formattedCommand + "\n");
        }
        else if (!line[j].equals(storedLine[j])) {
          // Found an unequal line that is not a comment.
          fail("Unequal lines:" + file.getAbsolutePath() + ":\n" + stdOut[i] + "\n"
            + storedFile.getAbsolutePath() + ":\n" + storedStdOut[i] + ".\n"
            + formattedCommand + "\n");
        }
      }
    }
  }

  /**
   * Appends a new line and writeString to the file called fileName.  Fails if the file
   * does not exist or the  .
   * @param fileName
   * @param writeString
   * @throws LogFile.LockException
   * @throws IOException
   */
  private void appendStringToFile(final String fileName, final String writeString,
    final String formattedCommand) throws LogFile.LockException, IOException {
    // Create dataset file from fileName
    if (fileName == null) {
      // One or more of the files are missing.
      fail("Missing fileName" + formattedCommand + "\n");
    }
    File file = new File(System.getProperty("user.dir"), fileName);
    assertTrue(file.getAbsolutePath() + " does not exist" + formattedCommand + "\n",
      file.exists());
    assertTrue(file.getAbsolutePath() + " is not a file" + formattedCommand + "\n",
      file.isFile());
    assertFalse("Target string is empty" + formattedCommand + "\n",
      writeString == null || writeString.matches(""));
    // write string
    LogFile logFile = LogFile.getInstance(file);
    LogFile.WriterId writerId = logFile.openWriter(true);
    assertFalse("Unable to write to " + fileName + formattedCommand + "\n",
      writerId.isEmpty());
    logFile.newLine(writerId);
    logFile.write(writeString, writerId);
    logFile.closeWriter(writerId);
  }

  /**
   * Standardize file names based on ImageFilenameStyle (from FilenameSettings).
   * <p>ImageFilenameStyle is 0: all files unchanged</p>
   * <p>ImageFilenameStyle is 1:</p>
   * <p>Dataseta_info.oldext => dataseta_info_oldext.mrc</p>
   * <p>Dataseta_info.oldext01 => dataseta_info_oldext01.mrc</p>
   * <p>dataseta_info.mrc => unchanged</p>
   * <p>dataseta_info.hdf => dataseta_info.mrc</p>
   * <p>ImageFilenameStyle is 2:</p>
   * <p>Dataseta_info.oldext => dataseta_info_oldext.hdf/p>
   * <p>Dataseta_info.oldext01 => dataseta_info_oldext01.hdf/p>
   * <p>dataseta_info.mrc => dataseta_info.hdf</p>
   * <p>dataseta_info.hdf => unchanged</p>
   * @param fileName
   * @return
   */
  private String convertToStandardizedFileName(final String fileName,
    final String formattedCommand) {
    if (fileName == null) {
      return null;
    }
    ImageFilenameStyle imageFilenameStyle = testRunner.getImageFilenameStyle();
    if (imageFilenameStyle == ImageFilenameStyle.OLD) {
      return fileName;
    }
    Extension extension = Extension.getInstance(fileName);
    if (extension == null || !extension.isEtomoImageFile()) {
      return fileName;
    }
    Extension standardExtension = imageFilenameStyle.getDefaultRawImageStackExtension();
    // This doesn't work for the double backup suffix "~~" but these files are currently
    // not included in the test.
    String newFileName = fileName;
    String suffix =
      fileName.endsWith(Extension.BACKUP_SUFFIX) ? Extension.BACKUP_SUFFIX : "";
    if (!extension.isStandardImageFileExtension()) {
      // Test for .st/.tif/tiff files. These extensions are for input images only so the
      // wrong keyword was used if fileName has this extension.
      assertFalse(
        "This is an input image file and cannot be changed.  Use input-file instead of "
          + "file." + formattedCommand + "\n",
        extension.isInputImageFile());
      String variableSuffix = "";
      if (extension.isVariable()) {
        variableSuffix = fileName
          .substring(fileName.lastIndexOf(extension.toString()) + extension.length());
        if (variableSuffix == null) {
          variableSuffix = "";
        }
      }
      // Go from old style to standard style.
      newFileName = Utilities.removeExtension(fileName)
        + Extension.STANDARDIZATION_DIVIDER + extension.toString() + variableSuffix
        + Extension.EXTENSION_DIVIDER + standardExtension.toString() + suffix;
    }
    else if (extension != standardExtension) {
      // Switch between mrc and hdf extensions.
      newFileName = Utilities.removeExtension(fileName) + Extension.EXTENSION_DIVIDER
        + standardExtension.toString() + suffix;
    }
    return newFileName;
  }

  /**
   * Converts a uitest match string (uses "*" for zero or more characters) into a regular
   * expression.
   * @param matchString
   * @return regular expression or null if matchString is null or empty
   */
  private String getRegularExpression(final String matchString) {
    if (matchString == null || matchString.isEmpty()) {
      return null;
    }
    // Break up the match string based on the uitest wildcard character.
    String[] array = matchString.split("\\" + WILD_CARD);
    if (array == null || array.length < 1) {
      return null;
    }
    // Build regular expression
    String regularExpressionWildCard = ".*";
    // Java forces regular expressions to match to the beginning and end of a line.
    StringBuilder regex = new StringBuilder(regularExpressionWildCard);
    for (int i = 0; i < array.length; i++) {
      if (array[i] == null || array[i].isEmpty()) {
        continue;
      }
      // Literal match for the non-wildcard portions. Match to the end of the line.
      regex.append("\\Q" + array[i] + "\\E" + regularExpressionWildCard);
    }
    return regex.toString();
  }

  /**
   * Looks for a string in a file.  Fails if the file does not exist or the 
   * string is not found.
   * @param fileName
   * @param targetString
   * @throws LogFile.LockException
   * @throws FileNotFoundException
   * @throws IOException
   */
  private void assertFileContainsString(final String fileName, final String targetString,
    final String formattedCommand)
    throws LogFile.LockException, FileNotFoundException, IOException {
    // Create dataset file from fileName
    if (fileName == null) {
      // One or more of the files are missing.
      fail("Missing fileName" + formattedCommand + "\n");
    }
    File file = new File(System.getProperty("user.dir"), fileName);
    assertTrue(file.getAbsolutePath() + " does not exist" + formattedCommand + "\n",
      file.exists());
    assertTrue(file.getAbsolutePath() + " is not a file" + formattedCommand + "\n",
      file.isFile());
    assertFalse("Target string is empty" + formattedCommand + "\n",
      targetString == null || targetString.matches("\\s*"));
    // Compare lines.
    LogFile logFile = LogFile.getInstance(file);
    LogFile.ReaderId readerId = logFile.openReader();
    assertFalse("this,Unable to read " + fileName + formattedCommand + "\n",
      readerId.isEmpty());
    String line;
    // Break up the target string based on the wildcard character.
    String regex = getRegularExpression(targetString);
    // See if one of the lines matches the target array. Null sygnals the end of the file.
    while ((line = logFile.readLine(readerId)) != null) {
      // Succeeded
      if (line.matches(regex)) {
        logFile.closeRead(readerId);
        return;
      }
    }
    logFile.closeRead(readerId);
    fail(targetString + "\nnot found in " + fileName + formattedCommand + "\n");
  }

  String[] stripCommentsAndBlankLines(String[] stringArray) {
    if (stringArray == null || stringArray.length == 0) {
      return stringArray;
    }
    List strippedList = new ArrayList();
    for (int i = 0; i < stringArray.length; i++) {
      String line = stringArray[i].trim();
      if (line.length() > 0 && !line.startsWith("#")) {
        strippedList.add(line);
      }
    }
    if (strippedList.size() == 0) {
      return new String[0];
    }
    else if (strippedList.size() == 1) {
      return new String[] { (String) strippedList.get(0) };
    }
    else {
      return (String[]) strippedList.toArray(new String[strippedList.size()]);
    }
  }

  /**
   * Handle assert.ge/le.  Checks text field value against command value.
   * @param textComponent
   * @param command
   */
  private void assertComparison(final JTextComponent textComponent, final Command command,
    final String formattedCommand)
    throws FileNotFoundException, IOException, LogFile.LockException {
    assertNotNull("assert.ge/le command must use a value" + formattedCommand,
      command.getValue());
    UITestModifierType modifierType = command.getModifierType();
    EtomoNumber fieldValue = new EtomoNumber(EtomoNumber.Type.DOUBLE);
    fieldValue.set(textComponent.getText());
    EtomoNumber commandValue = new EtomoNumber(EtomoNumber.Type.DOUBLE);
    commandValue.set(command.getValue());
    assertTrue("only numeric comparisons are valid" + formattedCommand,
      fieldValue.isValid() && commandValue.isValid());
    // assert.ge.field
    if (modifierType == UITestModifierType.GE) {
      assertTrue(
        "the value of this field is not greater or equal to the value if this command"
          + formattedCommand,
        fieldValue.ge(commandValue));
    }
    else if (modifierType == UITestModifierType.LE) {
      assertTrue(
        "the value of this field is not less then or equal to the value if this command"
          + formattedCommand,
        fieldValue.le(commandValue));
    }
    else {
      fail("unknown modifier - " + textComponent.getText() + "," + command.getValue()
        + formattedCommand);
    }
  }

  private JCheckBox findCheckBox(final String name, final int index) {
    setupNamedComponentFinder(JCheckBox.class,
      UITestFieldType.CHECK_BOX.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    return (JCheckBox) namedFinder.find(currentPanel, index);
  }

  private JComboBox findComboBox(final String name, final int index) {
    setupNamedComponentFinder(JComboBox.class,
      UITestFieldType.COMBO_BOX.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    return (JComboBox) namedFinder.find(currentPanel, index);
  }

  private JMenuItem findMenuItem(final String name, final int index) {
    setupNamedComponentFinder(JMenuItem.class,
      UITestFieldType.MENU_ITEM.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    return (JMenuItem) namedFinder.find();
  }

  private void findContainer(String name) {
    setupNamedComponentFinder(JPanel.class,
      UITestFieldType.PANEL.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    currentPanel = (Container) namedFinder.find();
  }

  private JRadioButton findRadioButton(final String name, final int index) {
    setupNamedComponentFinder(JRadioButton.class,
      UITestFieldType.RADIO_BUTTON.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    return (JRadioButton) namedFinder.find(currentPanel, index);
  }

  private JTabbedPane findTabbedPane(final String name) {
    setupNamedComponentFinder(JTabbedPane.class,
      UITestFieldType.TAB.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    return (JTabbedPane) namedFinder.find(currentPanel, 0);
  }

  /**
   * Makes sure that etomo is not busy and then checks the process watcher's state.
   * Processes cause etomo's busy signal to come on.
   * @param processName
   * @param endState
   * @param formattedCommand
   * @return !wait - return true when command is complete
   */
  private void waitProcessRun(final String processName, final String endState,
    final String formattedCommand) {
    assertNotNull("process name is required" + formattedCommand, processName);
    assertNotNull("end state is required" + formattedCommand, endState);
    // wait until etomo is not busy tracking processes
    waitUntilNotBusy(formattedCommand, false);
    // Test Kill Process button - should be disabled if the process is done
    setupNamedComponentFinder(JButton.class,
      UITestFieldType.BUTTON.toString() + AutodocTokenizer.SEPARATOR_CHAR
        + Utilities.convertLabelToName(AxisProgressPanel.KILL_BUTTON_LABEL));
    final JButton killButton = (JButton) namedFinder.find(currentPanel, 0);
    assertNotNull("can't find kill button" + formattedCommand, killButton);
    assertFalse(
      "Kill Process button should be disabled when process is done." + formattedCommand,
      killButton.isEnabled());
    // Test process name (on the progress bar label). Should be the same as the
    // processName.
    setupNamedComponentFinder(JLabel.class, ProgressPanel.LABEL_NAME);
    final JLabel progressBarLabel = (JLabel) namedFinder.find(currentPanel, 0);
    assertNotNull("can't find progress bar label" + formattedCommand, progressBarLabel);
    String actualProcessName = Utilities.convertLabelToName(progressBarLabel.getText());
    assertEquals("Process name is wrong: " + actualProcessName + formattedCommand,
      processName, actualProcessName);
    // Test the end state in the progress bar. Should equal endState.
    setupNamedComponentFinder(JProgressBar.class, ProgressPanel.NAME);
    final JProgressBar progressBar = (JProgressBar) namedFinder.find(currentPanel, 0);
    assertNotNull("can't find progress bar label" + formattedCommand, progressBar);
    String progressBarText = progressBar.getString();
    assertEquals("wrong end state: " + progressBarText + formattedCommand,
      ProcessEndState.getInstance(endState),
      ProcessEndState.getInstance(progressBarText));
  }

  /**
   * Executes the field part of a command.  Does not handle assert or if.
   * @param command
   * @param optional in selected fields prevents assertions from failing
   * @param throwExceptionIfNotFound
   */
  private void executeField(final Command command, final String formattedCommand)
    throws FileNotFoundException, IOException, LogFile.LockException {
    UITestModifierType modifierType = command.getModifierType();
    Field field = command.getField();
    UITestFieldType fieldType = field.getFieldType();
    String name = field.getName();
    int index = field.getIndex();
    String value = command.getValue();
    assertNotNull("missing field" + formattedCommand, field);
    // PANEL
    // pnl.panel_title
    if (fieldType == UITestFieldType.PANEL) {
      assertNull("value not valid in a panel command" + formattedCommand, value);
      findContainer(name);
      assertNotNull(
        "can't find field - " + command.getField().getName() + formattedCommand,
        currentPanel);
    }
    // BUTTON
    else if (fieldType == UITestFieldType.BUTTON) {
      click(findButton(UITestFieldType.BUTTON, name, index), name, value,
        formattedCommand);
    }
    // CHECK BOX
    else if (fieldType == UITestFieldType.CHECK_BOX) {
      waitUntilNotBusy(formattedCommand, true);
      JCheckBox checkBox = findCheckBox(name, index);
      assertNotNull(
        "can't find field - " + command.getField().getName() + formattedCommand,
        checkBox);
      // cb.check_box_name
      // if value is present,only click on check box to get it to match value
      boolean oldSelected = checkBox.isSelected();
      Boolean bValue = null;
      if (value != null) {
        bValue = convertToBoolean(value, formattedCommand);
      }
      if (value == null || oldSelected != bValue) {
        assertTrue("checkbox is not enabled" + formattedCommand, checkBox.isEnabled());
        prepareComponent(new MouseEventData(testRunner, checkBox),
          "checkbox is not visible", formattedCommand);
        helper.enterClickAndLeave(new MouseEventData(testRunner, checkBox));
        if (value == null) {
          assertEquals("check box was not clicked" + formattedCommand, !oldSelected,
            checkBox.isSelected());
        }
        else {
          assertEquals("check box was not set equal to " + value + formattedCommand,
            bValue.booleanValue(), checkBox.isSelected());
        }
      }
    }
    // COMBO BOX
    else if (fieldType == UITestFieldType.COMBO_BOX) {
      JComboBox comboBox = findComboBox(name, index);
      assertNotNull(
        "can't find field - " + command.getField().getName() + formattedCommand,
        comboBox);
      assertTrue("comboBox is not enabled" + formattedCommand, comboBox.isEnabled());
      if (command.getActionType() == UITestActionType.SET
        && command.getSubjectType() == UITestSubjectType.INDEX) {
        // set.index.cbb.combo_box_label
        EtomoNumber nValue = new EtomoNumber();
        nValue.set(value);
        assertTrue("value isn't a valid index - " + command.getField().getName()
          + formattedCommand, nValue.isValid());
        comboBox.setSelectedIndex(nValue.getInt());
      }
      else {
        // cbb.combo_box_label
        comboBox.addItem(value);
        comboBox.setSelectedItem(value);
      }
    }
    // FILE CHOOSER
    // file-chooser.file_chooser_title = chosen_file
    else if (fieldType == UITestFieldType.FILE_CHOOSER) {
      setValueFileChooser(name, value, formattedCommand);
    }
    // MENU_ITEM
    else if (fieldType == UITestFieldType.MENU_ITEM) {
      waitUntilNotBusy(formattedCommand, true);
      JMenuItem menuItem = findMenuItem(name, index);
      assertNotNull(
        "can't find field - " + command.getField().getName() + formattedCommand,
        menuItem);
      // mn.menu_item_label
      assertTrue("menu item is not enabled" + formattedCommand, menuItem.isEnabled());
      prepareComponent(new MouseEventData(testRunner, menuItem),
        "menu item is not visible", formattedCommand);
      helper.enterClickAndLeave(new MouseEventData(testRunner, menuItem));
    }
    // MINI BUTTON
    else if (fieldType == UITestFieldType.MINI_BUTTON) {
      waitUntilNotBusy(formattedCommand, true);
      AbstractButton miniButton = findButton(UITestFieldType.MINI_BUTTON, name, index);
      assertNotNull(
        "can't find field - " + command.getField().getName() + formattedCommand,
        miniButton);
      // mb.title_with_mini_button
      // if value is present,only click on mini-button when it matches value
      // mb.title_with_mini_button =
      // mb.title_with_mini_button = current_label
      if (value == null
        || miniButton.getText().equals("<html><b><center>" + value + "</center></b>")) {
        assertTrue("mini-button is not enabled" + formattedCommand,
          miniButton.isEnabled());
        prepareComponent(new MouseEventData(testRunner, miniButton),
          "mini-button is not visible", formattedCommand);
        helper.enterClickAndLeave(new MouseEventData(testRunner, miniButton));
        try {
          Thread.sleep(1);
        }
        catch (InterruptedException e) {}
      }
    }
    // POPUP
    // wait.popup.popup_title = dismiss_button_label
    // popup.popup_title = dismiss_button_label
    else if (fieldType == UITestFieldType.POPUP) {
      findPopup(command.getActionType() == UITestActionType.WAIT, name, value,
        formattedCommand);
    }
    // RADIO BUTTON
    else if (fieldType == UITestFieldType.RADIO_BUTTON) {
      waitUntilNotBusy(formattedCommand, true);
      JRadioButton radioButton = findRadioButton(name, index);
      assertNotNull(
        "can't find field - " + command.getField().getName() + formattedCommand,
        radioButton);
      // rb.radio_button_label
      assertTrue("radio button is not enabled" + formattedCommand,
        radioButton.isEnabled());
      assertNull("value not valid in a radio command" + formattedCommand, value);
      prepareComponent(new MouseEventData(testRunner, radioButton),
        "radio button is not visible", formattedCommand);
      helper.enterClickAndLeave(new MouseEventData(testRunner, radioButton));
      assertTrue("radio button was not click successfully" + formattedCommand,
        radioButton.isSelected());
    }
    // SPINNER
    else if (fieldType == UITestFieldType.SPINNER) {
      waitUntilNotBusy(formattedCommand, true);
      JSpinner spinner = findSpinner(name, index, command);
      assertNotNull("cannot find spinner" + formattedCommand, spinner);
      EtomoNumber nValue = new EtomoNumber();
      nValue.set(value);
      // sp.spinner_label = integer_value|up|down
      if (nValue.isValid()) {
        spinner.setValue(nValue.getNumber());
      }
      else {
        boolean spinnerControl = convertToSpinnerControl(value, formattedCommand);
        assertTrue("spinner is not enabled" + formattedCommand, spinner.isEnabled());
        prepareComponent(
          new JSpinnerMouseEventData(testRunner, spinner,
            spinnerControl ? JSpinnerMouseEventData.UP_ARROW_SUBCOMPONENT
              : JSpinnerMouseEventData.DOWN_ARROW_SUBCOMPONENT),
          "spinner is not visible", formattedCommand);
        helper.enterClickAndLeave(new JSpinnerMouseEventData(testRunner, spinner,
          spinnerControl ? JSpinnerMouseEventData.UP_ARROW_SUBCOMPONENT
            : JSpinnerMouseEventData.DOWN_ARROW_SUBCOMPONENT));
      }
    }
    // TAB
    // tb.tabbed_panel_title.index_of_tab
    else if (fieldType == UITestFieldType.TAB) {
      waitUntilNotBusy(formattedCommand, true);
      // find the tabbed panel and click on the tab
      assertNull("value not valid in a tab command" + formattedCommand, value);
      JTabbedPane tabbedPane = findTabbedPane(name);
      assertNotNull(
        "can't find field - " + command.getField().getName() + formattedCommand,
        tabbedPane);
      assertTrue("tab is not enabled" + formattedCommand, tabbedPane.isEnabledAt(index));
      prepareComponent(new JTabbedPaneMouseEventData(testRunner, tabbedPane, index, 1),
        "tabbed pane is not visible", formattedCommand);
      helper.enterClickAndLeave(
        new JTabbedPaneMouseEventData(testRunner, tabbedPane, index, 1));
    }
    // TEXT FIELD
    else if (fieldType == UITestFieldType.TEXT_FIELD) {
      JTextComponent textField = findTextField(name, index);
      assertNotNull("cannot find text field" + formattedCommand, textField);
      // tf.text_field_label
      assertTrue("textField is not enabled" + formattedCommand, textField.isEnabled());
      if (command.getActionType() == UITestActionType.SET
        && command.getSubjectType() == UITestSubjectType.FILE) {
        // set.file.tf.text_field_label
        textField
          .setText(new File(System.getProperty("user.dir"), value).getAbsolutePath());
      }
      else {
        textField.setText(value);
      }
    }
    else {
      fail("unexpected command" + formattedCommand);
    }
    return;
  }

  private void prepareComponent(final AbstractMouseEventData mouseEventData,
    final String assertMessage, final String formattedCommand) {
    multiRun(new Runnable() {
      @Override
      public void run() {
        try {
          assertTrue(assertMessage + formattedCommand, mouseEventData.prepareComponent());
        }
        catch (NullPointerException e) {
          // Try to make the field visible. UItest is called with moveb.
          UIHarness.INSTANCE.moveSubFrame();
        }
      }
    }, null);
  }

  /**
   * Call UIHarness.pack.
   */
  private void formatApplication() {
    // System.err.println("Formatting application");
    // UIHarness.INSTANCE.pack(axisID, EtomoDirector.INSTANCE.getCurrentManagerForTest());
    /* try {
      Thread.sleep(FORMAT_WAIT);
    }
    catch (InterruptedException e) {}*/
  }

  void pause() {
    Thread.dumpStack();
    Popup popup = Popup.getCustomButtonInstance(null, "Test Paused", "Test is paused",
      "Save", new String[] { "Stop Test", "Continue Test" });
    popup.open();
    boolean save = popup.isCheckboxSelected();
    if (save) {
      UIHarness.INSTANCE.save(null);
    }
    if (popup.getSelectedButtonIndex() == 0) {
      // Stop the entire test and leave etomo running
      // Probably won't be able to exit etomo in the normal way after this.
      testRunner.setMaintainEtomo(true);
      stopAllInstances();
    }
  }

  private void stopAllInstances() {
    all_stop = true;
    yield = true;
    if (reader != null) {
      reader.stopAllInstances();
    }
    if (parentTester != null) {
      parentTester.stopAllInstances();
    }
  }

  /**
   * Asserts modifiers exists and not-exists.  Also fails on fields that can't be found.
   * @param component
   * @param command
   * @return true if a recognized modifier is present
   * @throws FileNotFoundException
   * @throws IOException
   * @throws LogFile.LockException
   */
  private boolean assertExists(final Component component, final Command command,
    final String formattedCommand)
    throws FileNotFoundException, IOException, LogFile.LockException {
    UITestModifierType modifierType = command.getModifierType();
    if (modifierType == UITestModifierType.NOT_EXISTS) {
      assertNull("invalid modifier" + formattedCommand, component);
      return true;
    }
    // assert.exists.field
    if (modifierType == UITestModifierType.EXISTS) {
      assertNotNull("field must exist and be visible - " + command.getField().getName()
        + formattedCommand, component);
      return true;
    }
    // assert.field
    assertNotNull(
      "field was not found - " + command.getField().getName() + formattedCommand,
      component);
    return false;
  }

  /**
   * Runs code multiple times until the maximum is reached or it stops throwing an
   * AssertionFailedError.
   * @param runnable - run multiple times & throws AssertionFailedError.
   * @param sleep - sleep between runs
   * @param maxRuns - maximum times runnable.run is run
   * @param cancelThrow - cancel throwing the exception after the max was reached.
   */
  private void multiRun(final Runnable runnable, final Callable<Boolean> cancelThrow) {
    for (int i = 0; i < MULTI_RUNS; i++) {
      try {
        runnable.run();
        return;
      }
      catch (AssertionFailedError e) {}
      try {
        Thread.sleep(WAIT_SLEEP);
      }
      catch (InterruptedException e) {}
    }
    if (cancelThrow == null) {
      runnable.run();
    }
    else {
      // Run cancelThrow after last runnable.run fails.
      try {
        runnable.run();
        return;
      }
      catch (AssertionFailedError assertionFailedError) {
        try {
          if (!cancelThrow.call()) {
            throw assertionFailedError;
          }
        }
        catch (Exception e) {
          throw assertionFailedError;
        }
      }
    }
  }

  private Object multiCall(final Callable<Object> callable)
    throws AssertionFailedException {
    for (int i = 0; i < MULTI_RUNS; i++) {
      try {
        return callable.call();
      }
      catch (AssertionFailedException e) {}
      catch (Exception e) {
        e.printStackTrace();
      }
      try {
        Thread.sleep(WAIT_SLEEP);
      }
      catch (InterruptedException e) {}
    }
    try {
      return callable.call();
    }
    catch (AssertionFailedException e) {
      throw e.getAssertionFailedError();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
    return null;
  }

  /**
   * Runs assert.field actions.
   * @param command
   * @param throwExceptionIfNotFound
   */
  private void assertField(final Command command, final String formattedCommand)
    throws FileNotFoundException, IOException, LogFile.LockException {
    UITestModifierType modifierType = command.getModifierType();
    Field field = command.getField();
    UITestFieldType fieldType = field.getFieldType();
    String name = field.getName();
    final int index = field.getIndex();
    final String value = command.getValue();
    assertNotNull("missing field" + formattedCommand, field);
    assertEquals("function can only handle assert field commands" + formattedCommand,
      UITestActionType.ASSERT, command.getActionType());
    assertNull("function can only handle assert field commands" + formattedCommand,
      command.getSubject());
    // assert.field
    if (modifierType == UITestModifierType.NOT_EXISTS) {
      fail("invalid modifier" + formattedCommand);
    }
    // BUTTON
    if (fieldType == UITestFieldType.BUTTON) {
      // assert.exists.bn
      if (modifierType == UITestModifierType.EXISTS) {
        setupNamedNoWaitComponentFinder(AbstractButton.class,
          fieldType.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
        multiRun(new Runnable() {
          @Override
          public void run() {
            assertNotNull("field must exist and be visible - "
              + command.getField().getName() + formattedCommand,
              namedNoWaitFinder.find(currentPanel, index));
          }
        }, null);
        return;
      }
      else {
        AbstractButton button = findButton(UITestFieldType.BUTTON, name, index);
        if (assertEnabled(button, command, formattedCommand)) {
          return;
        }
        // assert.bn.button_name = button_state
        assertNotNull("value is required in an assert.bn command" + formattedCommand,
          value);
        assertEquals("button state is not equal to value - " + value + formattedCommand,
          convertToBoolean(value, formattedCommand), button.isSelected());
      }
    }
    // CHECK BOX
    else if (fieldType == UITestFieldType.CHECK_BOX) {
      JCheckBox checkBox = findCheckBox(name, index);
      if (assertExists(checkBox, command, formattedCommand)
        || assertEnabled(checkBox, command, formattedCommand)) {
        return;
      }
      // assert.cb.check_box_name = check_box_state
      assertNotNull("value is required in an assert.cb command" + formattedCommand,
        value);
      assertEquals(
        "check box state is not equal to value - " + checkBox.getText() + "," + value
          + formattedCommand,
        convertToBoolean(value, formattedCommand), checkBox.isSelected());
    }
    // COMBO BOX
    else if (fieldType == UITestFieldType.COMBO_BOX) {
      final JComboBox comboBox = findComboBox(name, index);
      if (assertExists(comboBox, command, formattedCommand)
        || assertEnabled(comboBox, command, formattedCommand)) {
        return;
      }
      String selectedItemText = null;
      Object selectedItem = comboBox.getSelectedItem();
      if (selectedItem != null) {
        selectedItemText = selectedItem.toString();
      }
      String fieldName = comboBox.getName();
      // assert.contains.cbb = search_pattern
      if (modifierType == UITestModifierType.CONTAINS) {
        assertNotNull("search pattern is required." + " - " + fieldName + ":"
          + selectedItemText + formattedCommand, value);
        // Translate search_pattern into a regular expression
        String regex = getRegularExpression(value);
        assertNotNull("search pattern is invalid: " + regex + " - " + fieldName + ":"
          + selectedItemText + formattedCommand, regex);
        assertTrue("field text does not match search pattern: " + value + " - "
          + fieldName + ":" + selectedItemText + formattedCommand,
          selectedItemText.matches(regex));
      }
      // assert.cbb.combo_box_label = value
      else if (value != null) {
        assertNotNull(
          "combo box is empty and is not equal to value - " + value + formattedCommand,
          selectedItem);
        assertTrue("combo box selected text is not equal to value - " + selectedItem + ","
          + value + formattedCommand, selectedItemText.equals(value));
      }
      // assert.contains.cbb = search_pattern
      else if (modifierType == UITestModifierType.CONTAINS) {
        assertNotNull("search pattern is required." + " - " + fieldName + ":"
          + selectedItemText + formattedCommand, value);
        // Translate search_pattern into a regular expression
        String regex = getRegularExpression(value);
        assertNotNull("search pattern is invalid: " + regex + " - " + fieldName + ":"
          + selectedItemText + formattedCommand, regex);
        assertTrue("field text does not match search pattern: " + value + " - "
          + fieldName + ":" + selectedItemText + formattedCommand,
          selectedItemText.matches(regex));
      }
      // assert.cbb.combo_box_label =
      else {
        assertEquals("combo box selected text is not empty - " + selectedItem + ","
          + value + formattedCommand, null, selectedItem);
      }

    }
    // MENU_ITEM
    else if (fieldType == UITestFieldType.MENU_ITEM) {
      JMenuItem menuItem = findMenuItem(name, index);
      if (assertExists(menuItem, command, formattedCommand)
        || assertEnabled(menuItem, command, formattedCommand)) {
        return;
      }
      assertNotNull("modifier is required" + formattedCommand, modifierType);
    }
    // MINI BUTTON
    else if (fieldType == UITestFieldType.MINI_BUTTON) {
      AbstractButton miniButton = findButton(UITestFieldType.MINI_BUTTON, name, index);
      if (assertExists(miniButton, command, formattedCommand)
        || assertEnabled(miniButton, command, formattedCommand)) {
        return;
      }
      // assert.mb.title_with_mini_button = current_label
      assertNotNull("value is required in an assert.mb command" + formattedCommand,
        value);
      assertTrue("mini-button label is not equal to value - " + miniButton.getText() + ","
        + value + formattedCommand, miniButton.getText().equals(value));
    }
    // RADIO BUTTON
    else if (fieldType == UITestFieldType.RADIO_BUTTON) {
      JRadioButton radioButton = findRadioButton(name, index);
      if (assertExists(radioButton, command, formattedCommand)
        || assertEnabled(radioButton, command, formattedCommand)) {
        return;
      }
      // assert.rb.radio_button_label = radio_button_state
      assertNotNull("value is required in an assert.rb command" + formattedCommand,
        value);
      assertEquals(
        "radio button state is not equal to value - " + radioButton.getText() + ","
          + value + formattedCommand,
        convertToBoolean(value, formattedCommand), radioButton.isSelected());
    }
    // SPINNER
    else if (fieldType == UITestFieldType.SPINNER) {
      JSpinner spinner = findSpinner(name, index, command);
      if (assertExists(spinner, command, formattedCommand)
        || assertEnabled(spinner, command, formattedCommand)) {
        return;
      }
      assertEquals(spinner, value, formattedCommand);
    }
    // TAB
    else if (fieldType == UITestFieldType.TAB) {
      // find the tabbed panel and click on the tab
      JTabbedPane tabbedPane = findTabbedPane(name);
      if (assertExists(tabbedPane, command, formattedCommand)
        || assertEnabled(tabbedPane, command, formattedCommand)) {
        return;
      }
      // tb.tabbed_panel_title.index_of_tab
      assertNull("value not valid in a tab command" + formattedCommand, value);
    }
    // TEXT FIELD
    else if (fieldType == UITestFieldType.TEXT_FIELD) {
      final JTextComponent textField = findTextField(name, index);
      if (assertExists(textField, command, formattedCommand)
        || assertEnabled(textField, command, formattedCommand)) {
        return;
      }
      assertNotNull("cannot find text field" + formattedCommand, textField);
      if (modifierType != null) {
        // assert.ge
        // assert.le
        if (modifierType == UITestModifierType.GE
          || modifierType == UITestModifierType.LE) {
          assertComparison(textField, command, formattedCommand);
        }
        // assert.contains.tf = search_pattern
        else if (modifierType == UITestModifierType.CONTAINS) {
          assertNotNull("search pattern is required." + " - " + textField.getName() + ":"
            + textField.getText() + formattedCommand, value);
          // Translate search_pattern into a regular expression
          String regex = getRegularExpression(value);
          assertNotNull("search pattern is invalid: " + regex + " - "
            + textField.getName() + ":" + textField.getText() + formattedCommand, regex);
          assertTrue(
            "field text does not match search pattern: " + value + " - "
              + textField.getName() + ":" + textField.getText() + formattedCommand,
            textField.getText().matches(regex));
        }
        else {
          fail("unknown action/modifier - " + textField.getText() + "," + value
            + formattedCommand);
        }
      }
      // assert.tf.text_field_label
      else {
        assertEquals(textField, value, formattedCommand);
      }
    }
  }

  //
  // functions called by UITest API
  //

  // assertEnabled

  void assertEnabled(final JTextComponent textComponent, final boolean enabled,
    final String formattedCommand) {
    assertNotNull("cannot find text field" + formattedCommand, textComponent);
    if (enabled) {
      assertTrue("text field is not enabled" + formattedCommand,
        textComponent.isEnabled() && textComponent.isEditable());
      return;
    }
    else {
      assertFalse("text field is not disabled" + formattedCommand,
        textComponent.isEnabled() && textComponent.isEditable());
      return;
    }
  }

  void assertEnabled(final Component component, final boolean enabled,
    final String formattedCommand) {
    assertNotNull("cannot find component" + formattedCommand, component);
    if (enabled) {
      assertTrue("component is not enabled" + formattedCommand, component.isEnabled());
      return;
    }
    else {
      assertFalse("component is not disabled" + formattedCommand, component.isEnabled());
      return;
    }
  }

  // assertEquals

  void assertEquals(final JTextComponent textField, final String value,
    final String formattedCommand) {
    assertNotNull("cannot find text field" + formattedCommand, textField);
    // assert.tf.text_field_label = value
    if (value != null) {
      multiRun(new Runnable() {
        @Override
        public void run() {
          assertTrue(
            "field text is not equal to value: " + value + " - " + textField.getName()
              + ":" + textField.getText() + formattedCommand,
            textField.getText().equals(value));
        }
      }, null);
    }
    // assert.tf.text_field_label =
    else {
      assertTrue("field text is not empty - " + textField.getText() + formattedCommand,
        textField.getText().equals(""));
    }
  }

  void assertEquals(final JSpinner spinner, final String value,
    final String formattedCommand) {
    assertNotNull("cannot find spinner" + formattedCommand, spinner);
    // assert.sp.spinner_label = integer_value
    EtomoNumber nValue = new EtomoNumber();
    nValue.set(value);
    if (value != null) {
      assertTrue(
        "field text is not equal to value - " + spinner.getValue() + "," + value
          + formattedCommand,
        ((Number) spinner.getValue()).intValue() == nValue.getInt());
    }
    else {
      nValue.set((Number) spinner.getValue());
      assertTrue("field text is not empty - " + spinner.getValue() + "," + value
        + formattedCommand, nValue.isNull());
    }
  }

  // assertExists
  void assertExistsImageFile(String fileName, final String formattedCommand) {
    fileName = convertToStandardizedFileName(fileName, formattedCommand);
    assertNotNull("invalid image file name" + formattedCommand, fileName);
    File file = new File(System.getProperty("user.dir"), fileName);
    assertTrue("file does not exist: " + file.getAbsolutePath() + formattedCommand,
      file.exists());
  }

  // click
  void click(final AbstractButton button, final String fieldName, final String value,
    final String formattedCommand) {
    waitUntilNotBusy(formattedCommand, true);
    assertNotNull("can't find field - " + fieldName + formattedCommand, button);
    // bn.button_name =
    assertNull("value not valid in a button command" + formattedCommand, value);
    assertTrue("button is not enabled" + formattedCommand, button.isEnabled());
    prepareComponent(new MouseEventData(testRunner, button), "button is not visible",
      formattedCommand);
    assertTrue("button is not visible" + formattedCommand,
      new MouseEventData(testRunner, button).prepareComponent());
    helper.enterClickAndLeave(new MouseEventData(testRunner, button));
    // Some buttons change the frame, so may have to get a new busyIconLabel.
  }

  // close
  void closePopup(final String title, final String closeButtonLabel,
    final String formattedCommand) {
    findPopup(false, title, closeButtonLabel, formattedCommand);
  }

  // copy
  void copyFile(final String fileName, final String toFileName, final boolean always,
    final String formattedCommand) {
    testRunner.copyFile(fileName, toFileName, always, formattedCommand);
  }

  // find

  AbstractButton findButton(final UITestFieldType fieldType, final String name,
    final int index) {
    setupNamedComponentFinder(AbstractButton.class,
      fieldType.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    if (debug) {
      namedFinder.setDebug(true);
    }
    return (AbstractButton) namedFinder.find(currentPanel, index);
  }

  JSpinner findSpinner(final String name, final int index, UITestCommandInfo command) {
    setupNamedComponentFinder(JSpinner.class,
      UITestFieldType.SPINNER.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    return (JSpinner) namedFinder.find(currentPanel, index);
  }

  JTextComponent findTextField(final String name, final int index) {
    setupNamedComponentFinder(JTextField.class,
      UITestFieldType.TEXT_FIELD.toString() + AutodocTokenizer.SEPARATOR_CHAR + name);
    return (JTextComponent) namedFinder.find(currentPanel, index);
  }

  // setValue

  void setValueFileChooser(final String title, final String fileName,
    final String formattedCommand) {
    // Check values
    assertNotNull("file chooser title is required" + formattedCommand, title);
    assertNotNull(
      "file name to accept in the file chooser is required" + formattedCommand, fileName);
    setupComponentFinder(JFileChooser.class);
    // Wait for the file chooser to finish showing.
    waitUntilNotBusy(formattedCommand, true);
    // Get the file chooser.
    JFileChooser fileChooser = (JFileChooser) finder.find();
    assertNotNull("file chooser did not pop up" + formattedCommand, fileChooser);
    // Make sure its the right fileChooser
    String fileChooserName = fileChooser.getName();
    if (fileChooserName == null || !fileChooserName.equals(title)) {
      // Close incorrect fileChooser and fail
      helper.sendKeyAction(new KeyEventData(testRunner, fileChooser, KeyEvent.VK_ESCAPE));
      fail("wrong file chooser - " + fileChooserName + formattedCommand);
    }
    // set the file name in the file chooser
    File file;
    if (fileName.startsWith(File.separator)
      || (Utilities.isWindowsOS() && fileName.charAt(1) == ':')) {
      file = new File(fileName);
    }
    else {
      file = new File(getVariableValue(UITestSubjectType.TESTDIR.toString(), axisID),
        fileName);
    }
    fileChooser.setSelectedFile(file);
    // check this it worked
    assertNotNull("unable to set selected file" + formattedCommand,
      fileChooser.getSelectedFile());
    //
    fileChooser.approveSelection();
  }

  void setValue(final JTextComponent textField, final String value,
    final String formattedCommand) {
    assertNotNull("cannot find text field" + formattedCommand, textField);
    assertTrue("text field is not enabled" + formattedCommand, textField.isEnabled());
    textField.setText(value);
    String textFieldText = textField.getText();
    if (textFieldText != null && textFieldText.isEmpty()) {
      textFieldText = null;
    }
    if (value == null) {
      assertNull("text field was not cleared" + formattedCommand, textFieldText);
    }
    else {
      assertEquals("text field was not set" + formattedCommand, value, textFieldText);
    }
  }

  // wait

  void waitProcess(final String processName, final String endState,
    final String formattedCommand) {
    multiRun(new Runnable() {
      @Override
      public void run() {
        waitProcessRun(processName, endState, formattedCommand);
      }
    }, null);
  }

  void waitPopup(final String subjectName, final String value,
    final String formattedCommand) {
    findPopup(true, subjectName, value, formattedCommand);
  }

  void waitUntilNotBusy(final String formattedCommand, final boolean waitForGui) {
    // Attempt to get the busy signal using multiple runs
    setupNamedNoWaitComponentFinder(JLabel.class,
      waitForGui ? BusyStatusPanel.GUI_LABEL : BusyStatusPanel.LABEL);
    JLabel busySignal = null;
    try {
      busySignal = (JLabel) multiCall(new Callable<Object>() {
        @Override
        public final JLabel call() throws AssertionFailedException {
          JLabel busySignal = (JLabel) namedNoWaitFinder.find(currentPanel, 0);
          try {
            assertNotNull("missing GUI busy signal" + formattedCommand, busySignal);
            return busySignal;
          }
          catch (AssertionFailedError e) {
            throw new AssertionFailedException(e);
          }
        }
      });
    }
    catch (AssertionFailedException e) {
      throw e.getAssertionFailedError();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
    while (busySignal.isEnabled()) {
      try {
        Thread.sleep(WAIT_SLEEP);
      }
      catch (InterruptedException e) {}
    }
  }

  /**
   * Runs if.field actions.
   * @param command
   * @param throwExceptionIfNotFound
   */
  private void ifField(final Command command, final String formattedCommand)
    throws FileNotFoundException, IOException, LogFile.LockException,
    ClassNotFoundException {
    UITestModifierType modifierType = command.getModifierType();
    Field field = command.getField();
    UITestFieldType fieldType = field.getFieldType();
    String name = field.getName();
    int index = field.getIndex();
    String value = command.getValue();
    assertNotNull("missing field" + formattedCommand, field);
    assertEquals("function can only handle assert field commands" + formattedCommand,
      UITestActionType.IF, command.getActionType());
    assertNull("function can only handle if field commands" + formattedCommand,
      command.getSubject());
    // if.field
    if (modifierType == UITestModifierType.EXISTS
      || modifierType == UITestModifierType.NOT_EXISTS) {
      fail("invalid modifier" + formattedCommand);
    }
    // BUTTON
    if (fieldType == UITestFieldType.BUTTON) {
      if (modifierType == UITestModifierType.ENABLED
        || modifierType == UITestModifierType.DISABLED) {
        // if.enabled
        // if.disabled
        ifEnabled(findButton(UITestFieldType.BUTTON, name, index), command,
          formattedCommand);
      }
      else {
        fail("invalid field command" + formattedCommand);
      }
    }
    // CHECK BOX
    else if (fieldType == UITestFieldType.CHECK_BOX) {
      if (modifierType == UITestModifierType.ENABLED
        || modifierType == UITestModifierType.DISABLED) {
        // if.enabled
        // if.disabled
        ifEnabled(findCheckBox(name, index), command, formattedCommand);
      }
      else {
        fail("invalid field command" + formattedCommand);
      }
    }
    // COMBO BOX
    else if (fieldType == UITestFieldType.COMBO_BOX) {
      if (modifierType == UITestModifierType.ENABLED
        || modifierType == UITestModifierType.DISABLED) {
        // if.enabled
        // if.disabled
        ifEnabled(findComboBox(name, index), command, formattedCommand);
      }
      else {
        fail("invalid field command" + formattedCommand);
      }
    }
    // MENU_ITEM
    else if (fieldType == UITestFieldType.MENU_ITEM) {
      if (modifierType == UITestModifierType.ENABLED
        || modifierType == UITestModifierType.DISABLED) {
        // if.enabled
        // if.disabled
        ifEnabled(findMenuItem(name, index), command, formattedCommand);
      }
      else {
        fail("invalid field command" + formattedCommand);
      }
    }
    // MINI BUTTON
    else if (fieldType == UITestFieldType.MINI_BUTTON) {
      if (modifierType == UITestModifierType.ENABLED
        || modifierType == UITestModifierType.DISABLED) {
        // if.enabled
        // if.disabled
        ifEnabled(findButton(UITestFieldType.MINI_BUTTON, name, index), command,
          formattedCommand);
      }
      else {
        fail("invalid field command" + formattedCommand);
      }
    }
    // RADIO BUTTON
    else if (fieldType == UITestFieldType.RADIO_BUTTON) {
      if (modifierType == UITestModifierType.ENABLED
        || modifierType == UITestModifierType.DISABLED) {
        // if.enabled
        // if.disabled
        ifEnabled(findRadioButton(name, index), command, formattedCommand);
      }
      else {
        fail("invalid field command" + formattedCommand);
      }
    }
    // SPINNER
    else if (fieldType == UITestFieldType.SPINNER) {
      if (modifierType == UITestModifierType.ENABLED
        || modifierType == UITestModifierType.DISABLED) {
        // if.enabled
        // if.disabled
        JSpinner spinner = findSpinner(name, index, command);
        assertNotNull("cannot find spinner" + formattedCommand, spinner);
        ifEnabled(spinner, command, formattedCommand);
      }
      else {
        fail("invalid field command" + formattedCommand);
      }
    }
    // TEXT FIELD
    else if (fieldType == UITestFieldType.TEXT_FIELD) {
      if (modifierType == UITestModifierType.ENABLED
        || modifierType == UITestModifierType.DISABLED) {
        // if.enabled
        // if.disabled
        JTextComponent textField = findTextField(name, index);
        assertNotNull("cannot find text field" + formattedCommand, textField);
        ifEnabled(textField, command, formattedCommand);
      }
      else {
        fail("invalid field command" + formattedCommand);
      }
    }
    else {
      fail("unexpected command" + formattedCommand);
    }
    return;
  }

  /**
   * Converts all possible boolean strings to boolean.
   * @param input
   * @return
   */
  private boolean convertToBoolean(final String input, final String formattedCommand) {
    if (input.equalsIgnoreCase("t") || input.equalsIgnoreCase("true")
      || input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes")
      || input.equalsIgnoreCase("on")) {
      return true;
    }
    if (input.equalsIgnoreCase("f") || input.equalsIgnoreCase("false")
      || input.equalsIgnoreCase("n") || input.equalsIgnoreCase("no")
      || input.equalsIgnoreCase("off")) {
      return false;
    }
    EtomoNumber number = new EtomoNumber();
    number.set(input);
    if (number.isValid()) {
      if (number.equals(1)) {
        return true;
      }
      if (number.equals(0)) {
        return false;
      }
    }
    fail("boolean value is required - " + input + formattedCommand);
    return false;
  }

  /**
   * Converts all possible spinner control strings to boolean where 1 equals up
   * and 0 equals down.
   * @param input
   * @return
   */
  private boolean convertToSpinnerControl(final String input,
    final String formattedCommand) {
    if (input.equalsIgnoreCase("up")) {
      return true;
    }
    if (input.equalsIgnoreCase("down")) {
      return false;
    }
    fail("allowabled strings:  up, down - " + input + formattedCommand);
    return false;
  }

  /**
   * Creates or reuses the named component finder.  Updates the component class
   * and name.  Sets the wait to 2.  Sets the operation to OP_EQUALS.
   * @param componentoClass
   * @param fieldName
   */
  private void setupNamedComponentFinder(final Class componentoClass, final String name) {
    if (namedFinder == null) {
      namedFinder = new NamedComponentFinder(componentoClass, name);
      namedFinder.setOperation(NamedComponentFinder.OP_EQUALS);
    }
    else {
      namedFinder.setComponentClass(componentoClass);
      namedFinder.setName(name);
    }
  }

  private void setupNamedNoWaitComponentFinder(final Class componentoClass,
    final String name) {
    if (namedNoWaitFinder == null) {
      namedNoWaitFinder = new NamedComponentFinder(componentoClass, name);
      namedNoWaitFinder.setWait(0);
      namedNoWaitFinder.setOperation(NamedComponentFinder.OP_EQUALS);
    }
    else {
      namedNoWaitFinder.setComponentClass(componentoClass);
      namedNoWaitFinder.setName(name);
    }
  }

  /**
   * Creates or reuses the named component finder.  Updates the component class
   * and name.  Sets the operation to OP_EQUALS.
   * @param componentoClass
   * @param fieldName
   */
  private void setupComponentFinder(final Class componentoClass) {
    if (finder == null) {
      finder = new ComponentFinder(componentoClass);
      finder.setOperation(NamedComponentFinder.OP_EQUALS);
    }
    else {
      finder.setComponentClass(componentoClass);
    }
  }

  private void setupNoWaitComponentFinder(final Class componentoClass) {
    if (noWaitfinder == null) {
      noWaitfinder = new ComponentFinder(componentoClass);
      noWaitfinder.setWait(0);
      noWaitfinder.setOperation(NamedComponentFinder.OP_EQUALS);
    }
    else {
      noWaitfinder.setComponentClass(componentoClass);
    }
  }

  /**
   * Creates or reuses the abstract button finder.  Updates the button label.
   * Sets the wait to 2.  Sets the operation to OP_EQUALS.
   * @param buttonText
   */
  private void setupAbstractButtonFinder(final String buttonLabel) {
    if (buttonFinder == null) {
      buttonFinder = new AbstractButtonFinder(buttonLabel);
      buttonFinder.setWait(0);
      buttonFinder.setOperation(NamedComponentFinder.OP_EQUALS);
    }
    else {
      buttonFinder.setText(buttonLabel);
    }
  }
}
