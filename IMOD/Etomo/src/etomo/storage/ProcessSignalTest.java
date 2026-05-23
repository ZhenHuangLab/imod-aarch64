package etomo.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.BitSet;

import etomo.process.ProcessState;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.ProcessName;
import etomo.util.EnvironmentVariable;
import junit.framework.TestCase;

/**
* <p>Description: Regression test for both ProcessSignal and the batchruntomo messages
* being recognized.</p>
* 
* <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
*
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public class ProcessSignalTest extends TestCase {
  private static final String SOURCE_DIR_ENV_VAR = "BRT_TEST_SOURCE_DIR";
  private static final String LOG_NAME = "BRTtestBBdual.log";

  public ProcessSignalTest(String name) {
    super(name);
  }

  private LogFile getLog() {
    if (!EnvironmentVariable.INSTANCE.exists(null, null, SOURCE_DIR_ENV_VAR,
      AxisID.ONLY)) {
      System.err.println("Warning: " + SOURCE_DIR_ENV_VAR
        + " environment variable is not set.  ProcessSignalTest will not run.");
      return null;
    }
    String sourceDir =
      EnvironmentVariable.INSTANCE.getValue(null, null, SOURCE_DIR_ENV_VAR, AxisID.ONLY);
    if (sourceDir == null || sourceDir.matches("\\s*")) {
      System.err.println("Warning: " + SOURCE_DIR_ENV_VAR
        + " environment variable is empty.  ProcessSignalTest will not run.");
      return null;
    }
    File file = new File(sourceDir, LOG_NAME);
    System.err.println(file+":\n");
    if (!file.exists() || !file.isFile()) {
      System.err
        .println("Warning: " + SOURCE_DIR_ENV_VAR + " environment variable is invalid.  "
          + file + " does not exist.  ProcessSignalTest will not run.");
      return null;
    }
    try {
      return LogFile.getInstance(file);
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    return null;
  }

  /**
   * Looks for one of each recognized signal in a batchruntomo.log file.  The test has no
   * control over the batchruntomo log used (but it assumes its a dual axis log) so it
   * does not check for order or which steps or process names are found.  The log file is
   * refreshed automatically so this test should failure if either ProcessSignal or
   * batchruntomo come incompatible.
   */
  public void testBatchruntomoLogDual() {
    AxisType axisType = AxisType.DUAL_AXIS;
    LogFile logFile = getLog();
    if (logFile == null) {
      return;
    }
    LogFile.ReaderId readId = null;
    try {
      readId = logFile.openReader();
      String line = null;
      SignalsFound signalsFound = new SignalsFound(axisType);
      // get the opening timestamp
      while ((line = logFile.readLine(readId)) != null) {
        System.err.println(line);
        signalsFound.find(line);
      }
      signalsFound.assertSuccess();
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    catch (IOException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    if (logFile != null && readId != null) {
      logFile.closeRead(readId);
    }
  }

  /**
   * Looks for one of each recognized signal in a batchruntomo.log file.  The log is
   * a dual access log, but will be searched as if it is single axis.  The test has no
   * control over the batchruntomo log used (but it assumes its a dual axis log) so it
   * does not check for order or which steps or process names are found.
   */
  public void testBatchruntomoLogSingle() {
    AxisType axisType = AxisType.SINGLE_AXIS;
    LogFile logFile = getLog();
    if (logFile == null) {
      return;
    }
    LogFile.ReaderId readId = null;
    try {
      readId = logFile.openReader();
      String line = null;
      SignalsFound signalsFound = new SignalsFound(axisType);
      // get the opening timestamp
      while ((line = logFile.readLine(readId)) != null) {
        signalsFound.find(line);
      }
      signalsFound.assertSuccess();
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    catch (IOException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    if (logFile != null && readId != null) {
      logFile.closeRead(readId);
    }
  }

  /**
   * Class to keep track of the different types of signals found.  Tracks one signal of
   * each type.
   * @author sueh
   */
  private static final class SignalsFound {
    // signals
    private static int _index_ = 0;
    private static final int START_TIMESTAMP_INDEX = _index_++;
    private static final int END_TIMESTAMP_INDEX = _index_++;
    private static final int STEP_INDEX = _index_++;
    private static final int A_INDEX = _index_++;
    private static final int B_INDEX = _index_++;
    private static final int START_PROCESS_BOTH_INDEX = _index_++;
    private static final int START_PROCESS_A_INDEX = _index_++;
    private static final int START_PROCESS_B_INDEX = _index_++;
    private static final int END_PROCESS_BOTH_INDEX = _index_++;
    private static final int END_PROCESS_A_INDEX = _index_++;
    // _index_ points to last signal
    private static final int END_PROCESS_B_INDEX = _index_;

    private final BitSet success = new BitSet(_index_ + 1);
    private final BitSet lookForStartTimestamp = new BitSet(_index_ + 1);
    private final BitSet lookForEndTimestamp = new BitSet(_index_ + 1);
    private final BitSet found = new BitSet(_index_ + 1);

    private final AxisType axisType;

    private SignalsFound(final AxisType axisType) {
      this.axisType = axisType;
      if (axisType == AxisType.DUAL_AXIS) {
        success.set(0, _index_ + 1);
      }
      else {
        // No single axis log is available, so the dual axis log is searched with the axis
        // type set to single.When a dual log is searched this way, volcombine and
        // other axis-free processes will be found and treated as first axis processes.
        // ProcessSignal will also find the axis messages, which wouldn't be in a real
        // single axis log.
        success.set(START_TIMESTAMP_INDEX);
        success.set(END_TIMESTAMP_INDEX);
        success.set(STEP_INDEX);
        success.set(A_INDEX);// remove if a real single axis log is used
        success.set(B_INDEX);// remove if a real single axis log is used
        success.set(START_PROCESS_A_INDEX);
        success.set(END_PROCESS_A_INDEX);
      }
      // Timestamps: Look for the beginning timestamp before anything is found. Look for
      // the ending timestamp after everything else is found.
      lookForEndTimestamp.or(success);
      lookForEndTimestamp.clear(END_TIMESTAMP_INDEX);
    }

    private void find(final String line) {
      ProcessSignal signal = ProcessSignal.getInstance(axisType, line,
        found.equals(lookForStartTimestamp) || found.equals(lookForEndTimestamp));
      if (signal == null) {
        return;
      }
      if (signal.isTimestamp()) {
        found.set(signal.isFinished() ? END_TIMESTAMP_INDEX : START_TIMESTAMP_INDEX);
        return;
      }
      if (signal.getStep() != null) {
        found.set(STEP_INDEX);
        return;
      }
      AxisID axisID = signal.getAxisID();
      if (signal.isAxisID()) {
        if (axisID == AxisID.FIRST) {
          found.set(A_INDEX);
        }
        else if (axisID == AxisID.SECOND) {
          found.set(B_INDEX);
        }
        return;
      }
      ProcessName processName = signal.getProcessName();
      if (processName == null) {
        return;
      }
      ProcessState processState = signal.getProcessState();
      if (processState == ProcessState.INPROGRESS) {
        if (axisID == null) {
          found.set(START_PROCESS_BOTH_INDEX);
        }
        else if (axisID == AxisID.FIRST) {
          found.set(START_PROCESS_A_INDEX);
        }
        else if (axisID == AxisID.SECOND) {
          found.set(START_PROCESS_B_INDEX);
        }
      }
      else if (processState == ProcessState.COMPLETE) {
        if (axisID == null) {
          found.set(END_PROCESS_BOTH_INDEX);
        }
        else if (axisID == AxisID.FIRST) {
          found.set(END_PROCESS_A_INDEX);
        }
        else if (axisID == AxisID.SECOND) {
          found.set(END_PROCESS_B_INDEX);
        }
      }
    }

    private void assertSuccess() {
      if (found.equals(success)) {
        return;
      }
      // Failure bits will be set to true where the found bit and the success bit are
      // different.
      BitSet failure = new BitSet(_index_ + 1);
      // load found
      failure.or(found);
      // set 1 where success and found differ
      failure.xor(success);
      // Describe the first failure.
      int index = failure.nextSetBit(0);
      if (index == START_TIMESTAMP_INDEX) {
        fail("START_TIMESTAMP_INDEX not found");
      }
      else if (index == END_TIMESTAMP_INDEX) {
        fail("END_TIMESTAMP_INDEX not found");
      }
      else if (index == STEP_INDEX) {
        fail("STEP_INDEX not found");
      }
      else if (index == A_INDEX) {
        fail("A_INDEX not found");
      }
      else if (index == B_INDEX) {
        fail("B_INDEX not found");
      }
      else if (index == START_PROCESS_BOTH_INDEX) {
        fail("START_PROCESS_BOTH_INDEX not found");
      }
      else if (index == START_PROCESS_A_INDEX) {
        fail("START_PROCESS_A_INDEX not found");
      }
      else if (index == START_PROCESS_B_INDEX) {
        fail("START_PROCESS_B_INDEX not found");
      }
      else if (index == END_PROCESS_BOTH_INDEX) {
        fail("END_PROCESS_BOTH_INDEX not found");
      }
      else if (index == END_PROCESS_A_INDEX) {
        fail("END_PROCESS_A_INDEX not found");
      }
      else if (index == END_PROCESS_B_INDEX) {
        fail("END_PROCESS_B_INDEX not found");
      }
      else {
        System.err.println("found:" + found + ",success:" + success);
        System.err.println("failure:" + failure);
        fail("Unknown error");
      }
    }
  }
}