package etomo.storage;

import java.io.FileNotFoundException;
import java.io.IOException;

import etomo.BaseManager;
import etomo.type.AxisID;

import etomo.util.DatasetFiles;

/**
 * <p>Description: A simple defocus file (_simple.defocus) that can only contain the
 * expected defocus and one phase plate shift in degrees.  The data from this file is
 * ignored, but it is checked to avoid writing the file unnecessarily.  See the 
 * ctfphaseflip man page; Defocus File Format section.</p>
 * <p>The entire file:</p>
 * <p>Without the phase plate shift value:</p>
 * <p>1 1 0 0 expected_defocus_in_nanometers</p>
 * <p>With the phase plate shift value:</p>
 * <p>4 0 0. 0. 0 3</p>
 * <p>1 1 0 0 expected_defocus_in_nanometers phase_plate_shift_in_degrees</p>
 * <p>The "4" in the first line is the flag showing the phase plate shifts are included.
 * </p>
 * <p>Copyright: Copyright 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class SimpleDefocusFile {
  // Only uses flag 4 (000100) phase shifts included.
  static final String FLAG_LINE = "4 0 0. 0. 0 3";
  static final String DATA_PREFIX = "1 1 0 0 ";
  private static final int EXPECTED_DEFOCUS_INDEX = 4;
  private static final int PHASE_SHIFT_INDEX = 5;

  /**
   * @param manager
   * @param axisID
   * @param expectedDefocusInNanometers - required
   * @param phaseShiftInDegrees
   * @return true if the file exists and matches the parameters
   */
  public static boolean isUpToDate(final BaseManager manager, final AxisID axisID,
    final String expectedDefocusInNanometers, final String phaseShiftInDegrees) {
    try {
      return isUpToDate(manager, axisID, expectedDefocusInNanometers, phaseShiftInDegrees,
        LogFile.getInstance(DatasetFiles.getSimpleDefocusFile(manager, axisID)), false);
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    return false;
  }

  static boolean isUpToDate(final BaseManager manager, final AxisID axisID,
    final String expectedDefocusInNanometers, final String phaseShiftInDegrees,
    final LogFileInterface file, final boolean debug) {
    if (debug) {
      System.out.println("IsUpToDate0:expectedDefocusInNanometers:"
        + expectedDefocusInNanometers + ",phaseShiftInDegrees:" + phaseShiftInDegrees);
    }
    if (expectedDefocusInNanometers == null
      || expectedDefocusInNanometers.matches("\\s*")) {
      Thread.dumpStack();
      System.err
        .println("Warning: expectedDefocusInNanometers is empty.  Unable to proceed.");
      return false;
    }
    if (file == null || !file.exists()) {
      return false;
    }
    boolean upToDate = false;
    LogFile.ReaderId readerId = null;
    try {
      readerId = file.openReader();
      // Look for the flag
      String line = file.readLine(readerId);
      int numReads = 1;
      if (line == null) {
        file.closeRead(readerId);
        return false;
      }
      boolean phaseShiftSet =
        phaseShiftInDegrees != null && !phaseShiftInDegrees.matches("\\s*");
      if (debug) {
        System.out
          .println("IsUpToDate1:line:" + line + ",phaseShiftSet:" + phaseShiftSet);
      }
      if (line.equals(FLAG_LINE)) {
        // Found the flag line. Should only be there if the phase plate shift value is
        // available.
        if (!phaseShiftSet) {
          file.closeRead(readerId);
          return false;
        }
        // Get the data line
        line = file.readLine(readerId);
        numReads++;
      }
      // Check the data line against parameters
      if (line == null) {
        file.closeRead(readerId);
        return false;
      }
      if (line.startsWith(DATA_PREFIX)) {
        if (phaseShiftSet && numReads == 1) {
          // Missing flag line
          file.closeRead(readerId);
          return false;
        }
      }
      else {
        file.closeRead(readerId);
        return false;
      }
      String[] array = line.split("\\s+");
      if (debug) {
        System.out
          .print("IsUpToDate2:line:" + line + ",numReads:" + numReads + ",array:[");
        if (array != null) {
          if (array.length > 0) {
            System.out.print(array[0]);
          }
          for (int i = 1; i < array.length; i++) {
            System.out.print("," + array[i]);
          }
        }
        System.out.println("]");
      }
      // Check the required expected defocus
      if (array == null || array.length <= EXPECTED_DEFOCUS_INDEX
        || array[EXPECTED_DEFOCUS_INDEX] == null
        || !array[EXPECTED_DEFOCUS_INDEX].equals(expectedDefocusInNanometers)) {
        file.closeRead(readerId);
        return false;
      }
      // Check the optional phase shift
      if (array.length > PHASE_SHIFT_INDEX && array[PHASE_SHIFT_INDEX] != null
        && !array[PHASE_SHIFT_INDEX].equals(phaseShiftInDegrees)) {
        file.closeRead(readerId);
        return false;
      }
      upToDate = true;
      if (debug) {
        System.out.println("IsUpToDate3:upToDate:" + upToDate);
      }
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    catch (FileNotFoundException e) {}
    catch (IOException e) {
      e.printStackTrace();
    }
    if (file != null) {
      file.closeRead(readerId);
    }
    return upToDate;
  }

  /** 
   * @param manager
   * @param axisID
   * @param expectedDefocusInNanometers - required
   * @param phaseShiftInDegrees
   * @return true if file was successfully written
   */
  public static boolean writeFile(final BaseManager manager, final AxisID axisID,
    final String expectedDefocusInNanometers, final String phaseShiftInDegrees) {
    try {
      return writeFile(manager, axisID, expectedDefocusInNanometers, phaseShiftInDegrees,
        LogFile.getInstance(DatasetFiles.getSimpleDefocusFile(manager, axisID)), false);
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    return false;
  }

  static boolean writeFile(final BaseManager manager, final AxisID axisID,
    final String expectedDefocusInNanometers, final String phaseShiftInDegrees,
    final LogFileInterface file, final boolean debug) {
    if (debug) {
      System.out.println("WriteFile0:expectedDefocusInNanometers:"
        + expectedDefocusInNanometers + ",phaseShiftInDegrees:" + phaseShiftInDegrees);
    }
    if (expectedDefocusInNanometers == null
      || expectedDefocusInNanometers.matches("\\s*")) {
      Thread.dumpStack();
      System.err
        .println("Warning: expectedDefocusInNanometers is empty.  Unable to write file.");
      return false;
    }
    if (file == null) {
      return false;
    }
    boolean succeeded = false;
    LogFile.WriterId writerId = null;
    try {
      if (file.exists()) {
        file.backup();
      }
      writerId = file.openWriter();
      boolean phaseShift =
        phaseShiftInDegrees != null && !phaseShiftInDegrees.matches("\\s*");
      if (debug) {
        System.out.println("WriteFile1:phaseShift:" + phaseShift);
      }
      if (phaseShift) {
        file.write(FLAG_LINE, writerId);
      }
      file.write(DATA_PREFIX + expectedDefocusInNanometers
        + (phaseShift ? " " + phaseShiftInDegrees : ""), writerId);
      succeeded = true;
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    if (file != null) {
      file.closeWriter(writerId);
    }
    return succeeded;
  }
}
