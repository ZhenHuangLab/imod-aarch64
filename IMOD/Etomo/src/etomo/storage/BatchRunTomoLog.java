package etomo.storage;

import java.io.FileNotFoundException;
import java.io.IOException;

import etomo.BaseManager;
import etomo.process.ProcessOutputStrings;
import etomo.process.ProcessState;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.DialogType;
import etomo.type.FileType;
import etomo.type.MetaData;
import etomo.type.ProcessName;
import etomo.type.Step;

/**
* <p>Description: Represents the dataset-level batchruntomo.log file.  Can seek and 
* iterate through this file.  Sets dialog button status.  Uses a selection of comfiles
* and step numbers as signals.  To avoid using a signal at the wrong time, uses dialog
* and step information to respond to signals only when appropriate.</p>
* <pre>
* Signals: comfiles and step numbers
* 
* In-process signals (brt2 process msg or step msg):
* coarse: xcorr
* track: autfidseed/raptor/beadtrack/xcorr_pt/transferfid
* fine: align
* pos: tomopitch/cryposition/findsection_pos
* stack: newst/blend/step 10
* gen: tilt/sirtSetup
* combine: solvematch/findsection_lim
* 
* Complete signals (brt3 process msg or step msg):
* preprocess: eraser
* coarse: prenewst/preblend
* track:step 5
* fine:step 6
* pos:step 7
* stack: step 13
* gen: step 14
* combine: volcombine
* postprocess: trimvol
* </pre>
* 
* <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
*
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public final class BatchRunTomoLog {
  private LogFile log = null;

  public BatchRunTomoLog() {}

  /**
   * Seeks a location based on metadata and returns a new iterator.
   * @param manager
   * @param metaData
   * @return
   */
  public Iterator seek(BaseManager manager, final MetaData metaData) {
    try {
      if (log == null) {
        log =
          LogFile.getInstance(FileType.BATCH_RUN_TOMO_DATASET_LOG.getFile(manager, null));
      }
      Iterator iterator = new Iterator(metaData);
      if (iterator.seek()) {
        return iterator;
      }
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    return null;
  }

  public final class Iterator {
    private final MetaData metaData;
    private final AxisType axisType;

    private LogFile.ReaderId readId = null;
    private ProcessSignal next = null;
    private AxisID axisID = AxisID.ONLY;
    private DialogType completedDialogTypeA = null;
    private DialogType completedDialogTypeB = null;
    private Step completedStepA = null;
    private Step completedStepB = null;

    private Iterator(final MetaData metaData) {
      this.metaData = metaData;
      if (metaData != null) {
        axisType = metaData.getAxisType();
      }
      else {
        axisType = null;
      }
    }

    /**
     * Sets readId to the line after the location specified by metadata.  Sets readId to
     * beginning of the file if there is no metadata or the location is not found.  If
     * seek is called more then once, will continue to scan the file.  A failed scan of
     * part of the file will cause it to scan the whole file.
     * 
     * @return true if a location was successfully set
     */
    public boolean seek() {
      if (log == null) {
        return false;
      }
      // Start the reader if necessary.
      boolean readFromStart = false;
      try {
        if (readId == null) {
          readFromStart = true;
          readId = log.openReader();
        }
        // Get the last axisID saved from a previous scan.
        if (axisType == AxisType.DUAL_AXIS) {
          axisID = metaData.getBatchRunTomoLogReadAxisID();
        }
        // Get the timestamp saved from a previous scan.
        String timestamp = metaData.getBatchRunTomoLogReadTimestamp();
        if (timestamp == null) {
          // No last location is available - scanning will start at the beginning of the
          // file.
          if (!readFromStart) {
            done();
            readId = log.openReader();
          }
          return true;
        }
        // If the finish line wasn't found, processing will start from the started line.
        // This means that some settings may be applied twice.
        boolean finished = metaData.isBatchRunTomoLogReadFinished();
        // Scan for the timestamp in a dataset started or finished line
        ProcessSignal processSignal = null;
        int seeks = 0;
        do {
          while ((processSignal = next(true)) != null) {
            if (processSignal.isTimestamp() && finished == processSignal.isFinished()
              && processSignal.equalsTimestamp(timestamp)) {
              return true;
            }
          }
          seeks++;
          // Seek failed. Make sure entire file has been sought.
          // Go to the beginning of the file.
          done();
          readId = log.openReader();
          if (readFromStart) {
            // Last location has not been found - scanning will start at the beginning of
            // the file.
            return true;
          }
        } while (seeks < 2);
        // Last location has not been found - scanning will start at the beginning of the
        // file.
        return true;
      }
      catch (FileNotFoundException e) {
        // No batchruntomo log available for this dataset.
      }
      catch (LogFile.LockException e) {
        e.printStackTrace();
      }
      // Exception caught
      done();
      return false;
    }

    /**
     * A boolean peek.  Attempts to set the next member variable if it is null.
     * @return true if next member variable is not null
     */
    public boolean hasNext() {
      if (next == null) {
        next = find(false);
      }
      return next != null;
    }

    /**
     * Get the next recognized process name or step process signal
     * @return
     */
    public ProcessSignal next() {
      return next(false);
    }

    /**
     * Returns and deletes the next member variable if available.  Otherwise return the
     * result of find().
     * @timestamp if true, only finds a process signal with a timestamp.
     * @return next or find()
     */
    private ProcessSignal next(final boolean timestamp) {
      if (next != null) {
        // The next signal was retrieved by hasNext.
        ProcessSignal localNext = next;
        next = null;
        return localNext;
      }
      // Return the next signal
      return find(timestamp);
    }

    /**
     * Find the next process signal with meaningful information.  Adds the dialogType and
     * the process state (when necessary) to the signal to be returned.  With timestamp
     * off a timestamp is not return - it is used to modify meta data.  With timestamp on,
     * only timestamps are returned..
     * @timestamp - if true just return timestamps without updating metadata
     * @return
     */
    private ProcessSignal find(final boolean timestamp) {
      if (log == null || readId == null) {
        return null;
      }
      try {
        String line = null;
        ProcessSignal processSignal = null;
        while ((line = log.readLine(readId)) != null) {
          processSignal = ProcessSignal.getInstance(axisType, line, timestamp);
          if (processSignal == null) {
            continue;
          }
          if (timestamp) {
            // Only looking for timestamps.
            return processSignal;
          }
          // Save timestamps. The last timestamp saved will be used the next time the log
          // is checked.
          if (processSignal.isTimestamp()) {
            metaData.setBatchRunTomoLogReadTimestamp(processSignal.getTimestamp());
            metaData.setBatchRunTomoLogReadFinished(processSignal.isFinished());
            continue;
          }
          if (processSignal.isAxisID()) {
            // Save the axisID being worked on
            axisID = processSignal.getAxisID();
            metaData.setBatchRunTomoLogReadAxisID(axisID);
            continue;
          }
          // Get the dialog.
          DialogType dialogType = null;
          Step afterStep = null;
          ProcessState processState = processSignal.getProcessState();
          // Check for a step signal
          Step step = processSignal.getStep();
          if (step != null) {
            // Update completed step
            if (axisID == AxisID.SECOND) {
              completedStepB = step;
            }
            else {
              completedStepA = step;
            }
            // Step line cantains no axisID information - get it from a previous line.
            processSignal.setAxisID(axisID);
            if (step == Step.GOLD_DETECTION_3D) {
              processSignal.setProcessState(ProcessState.INPROGRESS);
              dialogType = DialogType.FINAL_ALIGNED_STACK;
            }
            else {
              processState = ProcessState.COMPLETE;
              if (step == Step.BEAD_TRACKING) {
                processSignal.setProcessState(processState);
                dialogType = DialogType.FIDUCIAL_MODEL;
              }
              else if (step == Step.FINE_ALIGNMENT) {
                processSignal.setProcessState(processState);
                dialogType = DialogType.FINE_ALIGNMENT;
              }
              else if (step == Step.POSITIONING) {
                processSignal.setProcessState(processState);
                dialogType = DialogType.TOMOGRAM_POSITIONING;
              }
              else if (step == Step.TWO_D_FILTERING) {
                processSignal.setProcessState(processState);
                dialogType = DialogType.FINAL_ALIGNED_STACK;
              }
              else if (step == Step.RECONSTRUCTION) {
                processSignal.setProcessState(processState);
                dialogType = DialogType.TOMOGRAM_GENERATION;
              }
            }
          }
          else {
            // Check for a signal with process name. Process state will already be set.
            // Add the dialog type to the signal
            ProcessName processName = processSignal.getProcessName();
            if (processState == null || processName == null) {
              continue;
            }
            if (processState == ProcessState.INPROGRESS) {
              if (processName == ProcessName.XCORR) {
                dialogType = DialogType.COARSE_ALIGNMENT;
              }
              else if (processName == ProcessName.AUTOFIDSEED
                || processName == ProcessName.RUNRAPTOR
                || processName == ProcessName.TRACK
                || processName == ProcessName.XCORR_PT) {
                dialogType = DialogType.FIDUCIAL_MODEL;
              }
              else if (processName == ProcessName.TRANSFERFID) {
                dialogType = DialogType.FIDUCIAL_MODEL;
                if (line.indexOf(ProcessOutputStrings.BRT_TRANSFER_FID_B) != -1) {
                  processSignal.setAxisID(AxisID.SECOND);
                }
                else if (line.indexOf(ProcessOutputStrings.BRT_TRANSFER_FID_A) != -1) {
                  processSignal.setAxisID(AxisID.FIRST);
                }
              }
              else if (processName == ProcessName.ALIGN) {
                dialogType = DialogType.FINE_ALIGNMENT;
                afterStep = Step.BEAD_TRACKING;
              }
              else if (processName == ProcessName.TOMOPITCH
                || processName == ProcessName.CRYO_POSITION
                || processName == ProcessName.FIND_SECTION_POS) {
                dialogType = DialogType.TOMOGRAM_POSITIONING;
                afterStep = Step.FINE_ALIGNMENT;
              }
              else if ((processName == ProcessName.NEWST
                || processName == ProcessName.BLEND)) {
                dialogType = DialogType.FINAL_ALIGNED_STACK;
                afterStep = Step.POSITIONING;
              }
              else if ((processName == ProcessName.TILT
                || processName == ProcessName.SIRTSETUP)) {
                dialogType = DialogType.TOMOGRAM_GENERATION;
                afterStep = Step.TWO_D_FILTERING;
              }
              else if (processName == ProcessName.SOLVEMATCH
                || processName == ProcessName.FIND_SECTION_LIM) {
                dialogType = DialogType.TOMOGRAM_COMBINATION;
                afterStep = Step.RECONSTRUCTION;
              }
            }
            else if (processState == ProcessState.COMPLETE) {
              if (processName == ProcessName.ERASER) {
                dialogType = DialogType.PRE_PROCESSING;
              }
              else if (processName == ProcessName.PRENEWST
                || processName == ProcessName.PREBLEND) {
                dialogType = DialogType.COARSE_ALIGNMENT;
              }
              else if (processName == ProcessName.VOLCOMBINE) {
                dialogType = DialogType.TOMOGRAM_COMBINATION;
                afterStep = Step.RECONSTRUCTION;
              }
              else if (processName == ProcessName.TRIMVOL) {
                dialogType = DialogType.POST_PROCESSING;
                afterStep = Step.RECONSTRUCTION;
              }
            }
          }
          // Processes can be used outside of the dialog they are associated with.
          if (dialogType != null) {
            DialogType completedDialogType =
              axisID == AxisID.SECOND ? completedDialogTypeB : completedDialogTypeA;
            // Ignore a process that appears after its dialog is completed.
            if (completedDialogType != null
              && completedDialogType.getIndex() >= dialogType.getIndex()) {
              continue;
            }
          }
          if (afterStep != null) {
            Step completedStep =
              axisID == AxisID.SECOND ? completedStepB : completedStepA;
            // Ignore a process that comes too early - it has to come after a
            // specific step.
            if (completedStep == null || completedStep.lt(afterStep)) {
              continue;
            }
          }
          // Update completed dialog
          if (dialogType != null && processState == ProcessState.COMPLETE) {
            if (axisID == AxisID.SECOND) {
              completedDialogTypeB = dialogType;
            }
            else {
              completedDialogTypeA = dialogType;
            }
          }
          if (dialogType != null) {
            processSignal.setDialogType(dialogType);
            return processSignal;
          }
        }
      }
      catch (LogFile.LockException e) {
        e.printStackTrace();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }

    /**
     * Close the reader.
     */
    public void done() {
      if (log != null && readId != null) {
        log.closeRead(readId);
        readId = null;
      }
      next = null;
    }
  }
}