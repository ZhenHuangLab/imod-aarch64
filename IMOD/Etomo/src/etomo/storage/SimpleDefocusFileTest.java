package etomo.storage;

import java.io.FileNotFoundException;
import java.io.IOException;

import etomo.storage.LogFile.Id;
import etomo.storage.LogFile.LockException;
import etomo.storage.LogFile.ReaderId;
import etomo.storage.LogFile.WriterId;
import junit.framework.TestCase;

public class SimpleDefocusFileTest extends TestCase {
  private static final String EXPECTED_DEFOCUS_IN_NANOMETERS = "8000.0";
  private static final String UNMATCHED_EXPECTED_DEFOCUS_IN_NANOMETERS = "9000.0";
  private static final String PHASE_SHIFT_IN_DEGREES = "4.2";
  private static final String UNMATCHED_PHASE_SHIFT_IN_DEGREES = "5.2";

  public SimpleDefocusFileTest(String name) {
    super(name);
  }

  public void testIsUpToDate() {
    // file tests
    assertFalse("null log file - not up to date", SimpleDefocusFile
      .isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS, null, null, false));
    assertFalse("file not found - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS, null,
        new TestLogFile(false, TestType.FILE_NOT_FOUND, null, false), false));
    assertFalse("lock exception - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS, null,
        new TestLogFile(false, TestType.LOCK_EXCEPTION, null, false), false));
    assertFalse("IO exception - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS, null,
        new TestLogFile(false, TestType.IO_EXCEPTION, null, false), false));
    assertFalse("empty file - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS, null,
        new TestLogFile(false, TestType.EMPTY_FILE, null, false), false));
    // required data tests
    assertFalse("empty data line - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS, null,
        new TestLogFile(false, TestType.BLANK_LINE, LineToBeTested.DATA, false), false));
    assertFalse("empty data line - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS, null,
        new TestLogFile(false, TestType.BAD_LINE, LineToBeTested.DATA, false), false));
    assertFalse("missing required data - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, null, null,
        new TestLogFile(false, null, null, false), false));
    assertFalse("expected defocus doesn't match - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, UNMATCHED_EXPECTED_DEFOCUS_IN_NANOMETERS,
        null, new TestLogFile(false, null, null, false), false));
    assertTrue("expected defocus matches - up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS, null,
        new TestLogFile(false, null, null, false), false));
    // optional data tests
    assertFalse("missing flag line with phase shift - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS,
        PHASE_SHIFT_IN_DEGREES, new TestLogFile(false, null, null, false), false));
    assertFalse("empty flag line with phase shift - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS,
        PHASE_SHIFT_IN_DEGREES,
        new TestLogFile(true, TestType.BLANK_LINE, LineToBeTested.FLAG, false), false));
    assertFalse("bad flag line - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS,
        PHASE_SHIFT_IN_DEGREES,
        new TestLogFile(true, TestType.BAD_LINE, LineToBeTested.FLAG, false), false));
    assertFalse("flag line with no phase shift - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS, null,
        new TestLogFile(true, null, null, false), false));
    assertFalse("empty flag line with phase shift - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS,
        PHASE_SHIFT_IN_DEGREES,
        new TestLogFile(true, TestType.BLANK_LINE, LineToBeTested.FLAG, false), false));
    assertFalse("bad flag line with phase shift - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS,
        PHASE_SHIFT_IN_DEGREES,
        new TestLogFile(true, TestType.BAD_LINE, LineToBeTested.FLAG, false), false));
    assertFalse("unmatched phase shift - not up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS,
        UNMATCHED_PHASE_SHIFT_IN_DEGREES, new TestLogFile(true, null, null, false),
        false));
    assertTrue("matched phase shift - up to date",
      SimpleDefocusFile.isUpToDate(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS,
        PHASE_SHIFT_IN_DEGREES, new TestLogFile(true, null, null, false), false));
  }

  public void testWriteFile() {
    // file tests
    assertFalse("null log file - cannot write", SimpleDefocusFile.writeFile(null,
      null, EXPECTED_DEFOCUS_IN_NANOMETERS, null, null, false));
    assertFalse("lock exception - cannot write",
      SimpleDefocusFile.writeFile(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS, null,
        new TestLogFile(false, TestType.LOCK_EXCEPTION, null, false), false));
    assertFalse("IO exception - cannot write",
      SimpleDefocusFile.writeFile(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS, null,
        new TestLogFile(false, TestType.IO_EXCEPTION, null, false), false));
    // required data tests
    assertFalse("missing expected defocus - cannot write",
      SimpleDefocusFile.writeFile(null, null, null, null,
        new TestLogFile(false, null, null, false), false));
    //
    TestLogFile testLogFile = new TestLogFile(false, null, null, false);
    assertTrue("expected defocus available - can write", SimpleDefocusFile
      .writeFile(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS, null, testLogFile, false));
    assertEquals("expecting the data line with expected defocus",
      SimpleDefocusFile.DATA_PREFIX + EXPECTED_DEFOCUS_IN_NANOMETERS,
      testLogFile.lines[0]);
    assertNull("no flag line without optional data", testLogFile.lines[1]);
    //
    // optional data tests
    //
    testLogFile = new TestLogFile(false, null, null, false);
    assertTrue(
      "expected defocus available & blank phase shift ignored - can write",
      SimpleDefocusFile.writeFile(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS, "   ",
        testLogFile, false));
    assertEquals("blank phase shift ignored - no flag line",
      SimpleDefocusFile.DATA_PREFIX + EXPECTED_DEFOCUS_IN_NANOMETERS,
      testLogFile.lines[0]);
    assertNull("blank phase shift ignored - no flag line", testLogFile.lines[1]);
    //
    testLogFile = new TestLogFile(false, null, null, false);
    assertTrue("expected defocus & phase shift available - can write",
      SimpleDefocusFile.writeFile(null, null, EXPECTED_DEFOCUS_IN_NANOMETERS,
        PHASE_SHIFT_IN_DEGREES, testLogFile, false));
    assertEquals("phase shift included - flag line", SimpleDefocusFile.FLAG_LINE,
      testLogFile.lines[0]);
    assertEquals(
      "data line includes phase shift", SimpleDefocusFile.DATA_PREFIX
        + EXPECTED_DEFOCUS_IN_NANOMETERS + " " + PHASE_SHIFT_IN_DEGREES,
      testLogFile.lines[1]);
  }

  private static final class TestType {
    private static final TestType FILE_NOT_FOUND = new TestType();
    private static final TestType LOCK_EXCEPTION = new TestType();
    private static final TestType IO_EXCEPTION = new TestType();
    private static final TestType EMPTY_FILE = new TestType();
    private static final TestType BLANK_LINE = new TestType();
    private static final TestType BAD_LINE = new TestType();

    private TestType() {}
  }

  private static final class LineToBeTested {
    private static final LineToBeTested FLAG = new LineToBeTested();
    private static final LineToBeTested DATA = new LineToBeTested();

    private LineToBeTested() {}
  }

  private static final class TestLogFile implements LogFileInterface {
    private final boolean includeFlagLine;
    private final TestType testType;
    private final LineToBeTested lineToBeTested;
    private final boolean debug;

    private int numReads = 0;
    private int lineIndex = 0;
    private String[] lines = new String[] { null, null };
    private boolean fileExists = true;

    private TestLogFile(final boolean includeFlagLine, final TestType testType,
      final LineToBeTested lineToBeTested, final boolean debug) {
      this.includeFlagLine = includeFlagLine;
      this.testType = testType;
      this.lineToBeTested = lineToBeTested;
      if (testType == TestType.FILE_NOT_FOUND) {
        fileExists = false;
      }
      this.debug = debug;
    }

    public boolean exists() {
      return fileExists;
    }

    public ReaderId openReader() throws LockException, FileNotFoundException {
      if (testType == TestType.LOCK_EXCEPTION) {
        throw LogFile.getTestLockException();
      }
      if (!fileExists) {
        throw new FileNotFoundException();
      }
      return new LogFile.ReaderId();
    }

    public String readLine(final ReaderId readId) throws LockException, IOException {
      if (debug) {
        System.out.println("ReadLine0:includeFlagLine:" + includeFlagLine);
        System.out.println(",numReads:" + numReads + ",lineIndex:" + lineIndex + ",");
        System.out.print(",lines:[" + lines[0]);
        for (int i = 1; i < lines.length; i++) {
          System.out.print("," + lines[i]);
        }
        System.out.println("],fileExists:" + fileExists);
      }
      if (testType == TestType.LOCK_EXCEPTION) {

        throw LogFile.getTestLockException();
      }
      if (!fileExists || testType == TestType.IO_EXCEPTION) {
        throw new IOException();
      }
      if (testType == TestType.EMPTY_FILE) {
        return null;
      }
      // First read is flag line or data line
      if (numReads == 0) {
        numReads++;
        if (debug) {
          System.out.println("ReadLine1:numReads:" + numReads);
        }
        if (includeFlagLine) {
          return getFlagLine();
        }
        return getDataLine();
      }
      // Second read is data line or nothing
      else if (numReads == 1) {
        numReads++;
        if (debug) {
          System.out.println("ReadLine2:numReads:" + numReads);
        }
        if (includeFlagLine) {
          return getDataLine();
        }
        return null;
      }
      else {
        return null;
      }
    }

    private String getFlagLine() {
      if (lineToBeTested == LineToBeTested.FLAG) {
        if (testType == TestType.BLANK_LINE) {
          return "  ";
        }
        if (testType == TestType.BAD_LINE) {
          return "Bad Flag Line";
        }
      }
      return SimpleDefocusFile.FLAG_LINE;
    }

    private String getDataLine() {
      if (lineToBeTested == LineToBeTested.DATA) {
        if (testType == TestType.BLANK_LINE) {
          return "  ";
        }
        if (testType == TestType.BAD_LINE) {
          return "Bad Data Line";
        }
      }
      return SimpleDefocusFile.DATA_PREFIX + EXPECTED_DEFOCUS_IN_NANOMETERS
        + (includeFlagLine ? " " + PHASE_SHIFT_IN_DEGREES : "");
    }

    public boolean closeRead(final Id readId) {
      if (testType == TestType.LOCK_EXCEPTION || !fileExists) {
        return false;
      }
      return true;
    }

    public WriterId openWriter() throws LockException, IOException {
      if (testType == TestType.LOCK_EXCEPTION) {
        throw LogFile.getTestLockException();
      }
      if (testType == TestType.IO_EXCEPTION) {
        throw new IOException();
      }
      return new LogFile.WriterId();
    }

    public boolean backup() throws LockException {
      if (testType == TestType.LOCK_EXCEPTION) {
        throw LogFile.getTestLockException();
      }
      if (!fileExists) {
        return false;
      }
      fileExists = false;
      return true;
    }

    public void write(final String string, final WriterId writerId)
      throws LockException, IOException {
      if (testType == TestType.LOCK_EXCEPTION) {
        throw LogFile.getTestLockException();
      }
      if (testType == TestType.IO_EXCEPTION) {
        throw new IOException();
      }
      fileExists = true;
      if (lineIndex >= 0 && lineIndex < lines.length) {
        lines[lineIndex++] = string;
      }
    }

    public boolean closeWriter(final WriterId writerId) {
      if (testType == TestType.LOCK_EXCEPTION || !fileExists) {
        return false;
      }
      return true;
    }
  }
}
