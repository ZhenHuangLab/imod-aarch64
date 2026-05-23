package etomo.process;

/**
* <p>Description: Class to hold all strings that etomo needs to recognize that come from
* the output of IMOD applications.</p>
* 
* <p>Copyright: Copyright 2015 - 2017 by the Regents of the University of Colorado</p>
* 
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public final class ProcessOutputStrings {
  static final String END_PARAMETERS_TAG = "*** End of entries ***";

  //
  // batchruntomo
  //

  static final String BRT_START_PARAMETERS_TAG =
    "*** Entries to program batchruntomo ***";
  // dataset
  static final String BRT_DATASET_MSG_ID = "[brt1]";
  // This works for depracated searches as well as current ones.
  static final String BRT_DATASET_LOCATION_TAG = "set ";
  /**
   * @deprecated replaced by a message ID
   */
  static final String BRT_DATASET_TAG = "Starting data set";
  /**
   * @deprecated - capitalization changed
   */
  // etomo
  static final String BRT_ETOMO_TAG_OLD = "starting eTomo with log in";
  static final String BRT_ETOMO_TAG = "starting Etomo with log in";
  // step
  public static final String BRT_STEP_MSG_ID = "[brt2]";
  static final String BRT_STEP_END_TAG = ".com";
  /**
   * @deprecated replaced by a message ID
   */
  static final String BRT_STEP_TAG = "running ";
  // step success
  public static final String BRT_STEP_SUCCESS_MSG_ID = "[brt3]";
  /**
   * @deprecated replaced by a message ID
   */
  static final String BRT_STEP_SUCCESS_TAG = "Successfully finished";
  // dataset success - brt4 processing has stopped and the status is done.
  static final String BRT_DATASET_COMPLETED_MSG_ID = "[brt4]";
  /**
   * @deprecated replaced by a message ID
   */
  static final String BRT_DATASET_SUCCESS_TAG = "Completed dataset";

  static final String BRT_TIME_STAMP_TAG = " at ";
  static final String BRT_SUCCESS_TAG = "SUCCESSFULLY COMPLETED";
  public static final String BRT_BATCH_RUN_TOMO_ERROR_TAG = "ERROR: batchruntomo -";
  // logged
  /**
   * @deprecated - replaced by [:LOG]
   */
  static final String BRT_AXIS_B_TAG = "Starting axis B";
  /**
   * @deprecated - [:LOG] was added, so no need to 
   */
  static final String[] BRT_LOG_TAGS = new String[] { "Final align -",
    "AUTOPATCHFIT - Refinematch found a good ", "AUTOPATCHFIT - Findwarp found a good " };
  // killing
  static final String BRT_KILLING_MSG_ID = "[brt5]";
  /**
   * @deprecated replaced by a message ID
   */
  static final String BRT_KILLING_TAG = "RECEIVED SIGNAL TO QUIT, JUST EXITING";
  // paused
  static final String BRT_PAUSED_MSG_ID = "[brt6]";
  /**
   * @deprecated replaced by a message ID
   */
  static final String BRT_PAUSED_TAG = "Exiting after finishing dataset as requested";
  // starting dataset
  static final String BRT_STARTING_DATASET_MSG_ID = "[brt7]";
  /**
   * @deprecated replaced by a message ID
   */
  static final String BRT_STARTING_DATASET_TAG = "Beginning to process directive file";
  //
  public static final String BRT_REACHED_STEP_TAG = "Reached step";
  static final String BRT_ABORT_SET_TAG = "ABORT SET:";
  static final String BRT_ABORT_AXIS_TAG = "ABORT AXIS:";
  static final String BRT_ENDING_STEP_PARAM_TAG = "EndingStep =";
  static final String BRT_STARTING_STEP_PARAM_TAG = "StartingStep =";
  static final String BRT_FILE_LOCATION_TAG = "to:";
  // delivered
  static final String BRT_DELIVERED_MSG_ID = "[brt8]";
  /**
   * @deprecated replaced by a message ID
   */
  static final String BRT_DELIVERED_TAG = "Delivered stack";
  // renamed
  static final String BRT_RENAMED_MSG_ID = "[brt9]";
  /**
   * @deprecated replaced by a message ID
   */
  static final String BRT_RENAMED_TAG = "Renamed stack from";
  //
  public static final String BRT_ABORT_TAG = "ABORT";
  public static final String BRT_STARTED_DATASET_TAG = "[brt11]";
  // brt12 means that the processing of the dataset has stopped, the log is closed, and
  // the state may be error, stopped, or done.
  public static final String BRT_DATASET_LOG_CLOSED_TAG = "[brt12]";
  public static final String BRT_START_AXIS = "Starting axis";
  public static final String BRT_TRANSFER_FID_B = "from A to B";
  public static final String BRT_TRANSFER_FID_A = "from B to A";
}
