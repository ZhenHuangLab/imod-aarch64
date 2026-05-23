package etomo.storage;

import java.io.File;

import etomo.TestManager;
import etomo.type.AxisType;
import etomo.type.ImageFilenameStyle;
import junit.framework.TestCase;

public class IntermediateFileFilterTest extends TestCase {
  // constants
  private static final String CLOSE_REASON = ")\n";
  private static final String OLD = "old";
  private static final String STYLE = " style/";
  private static final String OLD_STYLE = OLD + STYLE;
  private static final String MRC_STYLE = "mrc" + STYLE;
  private static final String HDF_STYLE = "hdf" + STYLE;
  private static final String ALGORITHM = " algorithm:\n";
  private static final String OLD_ALGORITHM = OLD + ALGORITHM;
  private static final String NEW_ALGORITHM = "new" + ALGORITHM;
  // single
  private static final IntermediateFileFilter FILTER_SINGLE_OLD = IntermediateFileFilter
    .getInstance(new TestManager(AxisType.SINGLE_AXIS, ImageFilenameStyle.OLD),
      TestManager.DATASET_NAME);
  private static final IntermediateFileFilter FILTER_SINGLE_MRC = IntermediateFileFilter
    .getInstance(new TestManager(AxisType.SINGLE_AXIS, ImageFilenameStyle.MRC),
      TestManager.DATASET_NAME);
  private static final IntermediateFileFilter FILTER_SINGLE_HDF = IntermediateFileFilter
    .getInstance(new TestManager(AxisType.SINGLE_AXIS, ImageFilenameStyle.HDF),
      TestManager.DATASET_NAME);
  // dual
  private static final IntermediateFileFilter FILTER_DUAL_OLD = IntermediateFileFilter
    .getInstance(new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.OLD),
      TestManager.DATASET_NAME);
  private static final IntermediateFileFilter FILTER_DUAL_MRC = IntermediateFileFilter
    .getInstance(new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.MRC),
      TestManager.DATASET_NAME);
  private static final IntermediateFileFilter FILTER_DUAL_HDF = IntermediateFileFilter
    .getInstance(new TestManager(AxisType.DUAL_AXIS, ImageFilenameStyle.HDF),
      TestManager.DATASET_NAME);
  //
  //
  // files
  //
  //
  //
  // Single
  //
  private static final File BACKUP = new File(TestManager.DATASET_NAME + ".edf~");
  private static final File REC_MAT =
    new File(TestManager.DATASET_NAME + ".rec.mat7890123");
  private static final File REC_WRP =
    new File(TestManager.DATASET_NAME + ".rec.wrp4567890");
  private static final File PROCESS_CHUNKS_TILT_LOG = new File("tilt-123.log");
  private static final File PROCESS_CHUNKS_TILT_COM = new File("tilt-890.com");
  // No match
  private static final File MRC_FAIL = new File(TestManager.DATASET_NAME + ".mrc");
  private static final File HDF_FAIL = new File(TestManager.DATASET_NAME + ".hdf");
  private static final File PROCESS_CHUNKS_TILT_LOG_FAIL = new File("tilt-12.log");
  private static final File PROCESS_CHUNKS_TILT_COM_FAIL = new File("tilt-89.com");
  // Old
  private static final File FIXED_X_RAYS =
    new File(TestManager.DATASET_NAME + "_fixed.st");
  private static final File ALI = new File(TestManager.DATASET_NAME + ".ali");
  private static final File PREALI = new File(TestManager.DATASET_NAME + ".preali");
  private static final File BOT = new File(TestManager.DATASET_NAME + "bot.rec");
  private static final File MID = new File(TestManager.DATASET_NAME + "mid.rec");
  private static final File TOP = new File(TestManager.DATASET_NAME + "top.rec");
  private static final File BL = new File(TestManager.DATASET_NAME + ".bl");
  private static final File DCST = new File(TestManager.DATASET_NAME + ".dcst");
  private static final File ALI_LOG10 = new File(TestManager.DATASET_NAME + ".alilog10");
  private static final File VSR = new File(TestManager.DATASET_NAME + ".vsr");
  private static final File THREE_D_FIND =
    new File(TestManager.DATASET_NAME + "_3dfind.rec");
  private static final File OUTPUT = new File(TestManager.DATASET_NAME + "_full.rec");
  private static final File PROCESS_CHUNKS_REC =
    new File(TestManager.DATASET_NAME + "-123.rec");
  private static final File ERASED_BEADS =
    new File(TestManager.DATASET_NAME + "_erased.ali");
  private static final File CTF_CORRECTED =
    new File(TestManager.DATASET_NAME + "_ctfcorr.ali");
  private static final File MTF_FILTERED =
    new File(TestManager.DATASET_NAME + "_filt.ali");
  private static final File FULL_VSR = new File(TestManager.DATASET_NAME + "_full.vsr45");
  private static final File SUB_VSR = new File(TestManager.DATASET_NAME + "_sub.vsr67");
  // No match
  private static final File NAD_FAIL = new File(TestManager.DATASET_NAME + ".nad");
  // mrc
  private static final File FIXED_X_RAYS_MRC =
    new File(TestManager.DATASET_NAME + "_fixed.mrc");
  private static final File ALI_MRC = new File(TestManager.DATASET_NAME + "_ali.mrc");
  private static final File PREALI_MRC =
    new File(TestManager.DATASET_NAME + "_preali.mrc");
  private static final File BOT_MRC = new File(TestManager.DATASET_NAME + "bot_rec.mrc");
  private static final File MID_MRC = new File(TestManager.DATASET_NAME + "mid_rec.mrc");
  private static final File TOP_MRC = new File(TestManager.DATASET_NAME + "top_rec.mrc");
  private static final File BL_MRC = new File(TestManager.DATASET_NAME + "_bl.mrc");
  private static final File DCST_MRC = new File(TestManager.DATASET_NAME + "_dcst.mrc");
  private static final File ALI_LOG10_MRC =
    new File(TestManager.DATASET_NAME + "_alilog10.mrc");
  private static final File VSR_MRC = new File(TestManager.DATASET_NAME + "_vsr.mrc");
  private static final File THREE_D_FIND_MRC =
    new File(TestManager.DATASET_NAME + "_3dfind_rec.mrc");
  private static final File OUTPUT_MRC =
    new File(TestManager.DATASET_NAME + "_full_rec.mrc");
  private static final File PROCESS_CHUNKS_REC_MRC =
    new File(TestManager.DATASET_NAME + "-890_rec.mrc");
  private static final File ERASED_BEADS_MRC =
    new File(TestManager.DATASET_NAME + "_erased_ali.mrc");
  private static final File CTF_CORRECTED_MRC =
    new File(TestManager.DATASET_NAME + "_ctfcorr_ali.mrc");
  private static final File MTF_FILTERED_MRC =
    new File(TestManager.DATASET_NAME + "_filt_ali.mrc");
  private static final File FULL_VSR_MRC =
    new File(TestManager.DATASET_NAME + "_full_vsr12.mrc");
  private static final File SUB_VSR_MRC =
    new File(TestManager.DATASET_NAME + "_sub_vsr34.mrc");
  // No match
  private static final File NAD_FAIL_MRC =
    new File(TestManager.DATASET_NAME + "_nad.mrc");
  // hdf
  private static final File FIXED_X_RAYS_HDF =
    new File(TestManager.DATASET_NAME + "_fixed.hdf");
  private static final File ALI_HDF = new File(TestManager.DATASET_NAME + "_ali.hdf");
  private static final File PREALI_HDF =
    new File(TestManager.DATASET_NAME + "_preali.hdf");
  private static final File BOT_HDF = new File(TestManager.DATASET_NAME + "bot_rec.hdf");
  private static final File MID_HDF = new File(TestManager.DATASET_NAME + "mid_rec.hdf");
  private static final File TOP_HDF = new File(TestManager.DATASET_NAME + "top_rec.hdf");
  private static final File BL_HDF = new File(TestManager.DATASET_NAME + "_bl.hdf");
  private static final File DCST_HDF = new File(TestManager.DATASET_NAME + "_dcst.hdf");
  private static final File ALI_LOG10_HDF =
    new File(TestManager.DATASET_NAME + "_alilog10.hdf");
  private static final File VSR_HDF = new File(TestManager.DATASET_NAME + "_vsr.hdf");
  private static final File THREE_D_FIND_HDF =
    new File(TestManager.DATASET_NAME + "_3dfind_rec.hdf");
  private static final File OUTPUT_HDF =
    new File(TestManager.DATASET_NAME + "_full_rec.hdf");
  private static final File PROCESS_CHUNKS_REC_HDF =
    new File(TestManager.DATASET_NAME + "-567_rec.hdf");
  private static final File ERASED_BEADS_HDF =
    new File(TestManager.DATASET_NAME + "_erased_ali.hdf");
  private static final File CTF_CORRECTED_HDF =
    new File(TestManager.DATASET_NAME + "_ctfcorr_ali.hdf");
  private static final File MTF_FILTERED_HDF =
    new File(TestManager.DATASET_NAME + "_filt_ali.hdf");
  private static final File FULL_VSR_HDF =
    new File(TestManager.DATASET_NAME + "_full_vsr89.hdf");
  private static final File SUB_VSR_HDF =
    new File(TestManager.DATASET_NAME + "_sub_vsr01.hdf");
  // No match
  private static final File NAD_FAIL_HDF =
    new File(TestManager.DATASET_NAME + "_nad.hdf");
  //
  // dual
  //
  private static final File VOLCOMBINE_LOG_DUAL = new File("volcombine.log");
  private static final File PROCESS_CHUNKS_VOLCOMBINE_LOG_DUAL =
    new File("volcombine-123.log");
  private static final File PROCESS_CHUNKS_VOLCOMBINE_COM_DUAL =
    new File("volcombine-456.com");
  private static final File BACKUP_DUAL = new File(TestManager.DATASET_NAME + "a.edf~");
  private static final File REC_MAT_DUAL =
    new File(TestManager.DATASET_NAME + "b.rec.mat2345678");
  private static final File REC_WRP_DUAL =
    new File(TestManager.DATASET_NAME + "a.rec.wrp9012345");
  private static final File PROCESS_CHUNKS_TILT_LOG_DUAL = new File("tiltb-678.log");
  private static final File PROCESS_CHUNKS_TILT_COM_DUAL = new File("tilta-901.com");
  // No match
  private static final File MRC_FAIL_DUAL = new File(TestManager.DATASET_NAME + "a.mrc");
  private static final File HDF_FAIL_DUAL = new File(TestManager.DATASET_NAME + "b.hdf");
  private static final File PROCESS_CHUNKS_TILT_LOG_FAIL_DUAL = new File("tiltb-6.log");
  private static final File PROCESS_CHUNKS_TILT_COM_FAIL_DUAL = new File("tilta-9.com");
  // Old
  private static final File FIXED_X_RAYS_DUAL =
    new File(TestManager.DATASET_NAME + "b_fixed.st");
  private static final File ALI_DUAL = new File(TestManager.DATASET_NAME + "a.ali");
  private static final File PREALI_DUAL = new File(TestManager.DATASET_NAME + "b.preali");
  private static final File BOT_DUAL = new File(TestManager.DATASET_NAME + "bota.rec");
  private static final File MID_DUAL = new File(TestManager.DATASET_NAME + "midb.rec");
  private static final File TOP_DUAL = new File(TestManager.DATASET_NAME + "topa.rec");
  private static final File BL_DUAL = new File(TestManager.DATASET_NAME + "b.bl");
  private static final File DCST_DUAL = new File(TestManager.DATASET_NAME + "a.dcst");
  private static final File ALI_LOG10_DUAL =
    new File(TestManager.DATASET_NAME + "b.alilog10");
  private static final File VSR_DUAL = new File(TestManager.DATASET_NAME + "a.vsr");
  private static final File THREE_D_FIND_DUAL =
    new File(TestManager.DATASET_NAME + "b_3dfind.rec");
  private static final File OUTPUT_DUAL = new File("sum.rec");
  private static final File PROCESS_CHUNKS_REC_DUAL =
    new File(TestManager.DATASET_NAME + "a-234.rec");
  private static final File ERASED_BEADS_DUAL =
    new File(TestManager.DATASET_NAME + "b_erased.ali");
  private static final File CTF_CORRECTED_DUAL =
    new File(TestManager.DATASET_NAME + "a_ctfcorr.ali");
  private static final File MTF_FILTERED_DUAL =
    new File(TestManager.DATASET_NAME + "b_filt.ali");
  private static final File FULL_VSR_DUAL =
    new File(TestManager.DATASET_NAME + "a_full.vsr56");
  private static final File SUB_VSR_DUAL =
    new File(TestManager.DATASET_NAME + "b_sub.vsr78");
  // No match
  private static final File NAD_FAIL_DUAL = new File(TestManager.DATASET_NAME + "a.nad");
  // mrc
  private static final File FIXED_X_RAYS_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "b_fixed.mrc");
  private static final File ALI_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "b_ali.mrc");
  private static final File PREALI_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "a_preali.mrc");
  private static final File BOT_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "botb_rec.mrc");
  private static final File MID_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "mida_rec.mrc");
  private static final File TOP_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "topb_rec.mrc");
  private static final File BL_MRC_DUAL = new File(TestManager.DATASET_NAME + "a_bl.mrc");
  private static final File DCST_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "b_dcst.mrc");
  private static final File ALI_LOG10_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "a_alilog10.mrc");
  private static final File VSR_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "b_vsr.mrc");
  private static final File THREE_D_FIND_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "a_3dfind_rec.mrc");
  private static final File OUTPUT_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "sum_rec.mrc");
  private static final File PROCESS_CHUNKS_REC_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "b-901_rec.mrc");
  private static final File ERASED_BEADS_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "a_erased_ali.mrc");
  private static final File CTF_CORRECTED_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "b_ctfcorr_ali.mrc");
  private static final File MTF_FILTERED_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "a_filt_ali.mrc");
  private static final File FULL_VSR_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "b_full_vsr23.mrc");
  private static final File SUB_VSR_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "a_sub_vsr45.mrc");
  // No match
  private static final File NAD_FAIL_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "b_nad.mrc");
  // hdf
  private static final File FIXED_X_RAYS_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "b_fixed.hdf");
  private static final File ALI_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "a_ali.hdf");
  private static final File PREALI_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "b_preali.hdf");
  private static final File BOT_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "bota_rec.hdf");
  private static final File MID_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "midb_rec.hdf");
  private static final File TOP_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "topa_rec.hdf");
  private static final File BL_HDF_DUAL = new File(TestManager.DATASET_NAME + "b_bl.hdf");
  private static final File DCST_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "a_dcst.hdf");
  private static final File ALI_LOG10_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "b_alilog10.hdf");
  private static final File VSR_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "a_vsr.hdf");
  private static final File THREE_D_FIND_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "b_3dfind_rec.hdf");
  private static final File OUTPUT_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "sum_rec.hdf");
  private static final File PROCESS_CHUNKS_REC_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "a-678_rec.hdf");
  private static final File ERASED_BEADS_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "b_erased_ali.hdf");
  private static final File CTF_CORRECTED_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "a_ctfcorr_ali.hdf");
  private static final File MTF_FILTERED_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "b_filt_ali.hdf");
  private static final File FULL_VSR_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "a_full_vsr78.hdf");
  private static final File SUB_VSR_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "b_sub_vsr90.hdf");
  // No match
  private static final File NAD_FAIL_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "a_nad.hdf");
  //
  // New functionality
  //
  // The number of digits can be increased.
  //
  // single
  private static final File PROCESS_CHUNKS_REC_XD =
    new File(TestManager.DATASET_NAME + "-567123.rec");
  private static final File FULL_VSR_XD =
    new File(TestManager.DATASET_NAME + "_full.vsr891");
  private static final File SUB_VSR_XD =
    new File(TestManager.DATASET_NAME + "_sub.vsr1012");
  private static final File PROCESS_CHUNKS_TILT_LOG_XD = new File("tilt-123123.log");
  private static final File PROCESS_CHUNKS_TILT_COM_XD = new File("tilt-4561.com");
  private static final File PROCESS_CHUNKS_REC_XD_MRC =
    new File(TestManager.DATASET_NAME + "-78912_rec.mrc");
  private static final File FULL_VSR_XD_MRC =
    new File(TestManager.DATASET_NAME + "_full_vsr01123.mrc");
  private static final File SUB_VSR_XD_MRC =
    new File(TestManager.DATASET_NAME + "_sub_vsr231.mrc");
  private static final File PROCESS_CHUNKS_REC_XD_HDF =
    new File(TestManager.DATASET_NAME + "-45612_rec.hdf");
  private static final File FULL_VSR_XD_HDF =
    new File(TestManager.DATASET_NAME + "_full_vsr78123.hdf");
  private static final File SUB_VSR_XD_HDF =
    new File(TestManager.DATASET_NAME + "_sub_vsr901.hdf");
  // dual
  private static final File PROCESS_CHUNKS_VOLCOMBINE_LOG_XD_DUAL =
    new File("volcombine-12312.log");
  private static final File PROCESS_CHUNKS_VOLCOMBINE_COM_XD_DUAL =
    new File("volcombine-456123.com");
  private static final File PROCESS_CHUNKS_TILT_LOG_XD_DUAL =
    new File("tiltb-123123.log");
  private static final File PROCESS_CHUNKS_TILT_COM_XD_DUAL = new File("tilta-4561.com");
  private static final File PROCESS_CHUNKS_REC_XD_DUAL =
    new File(TestManager.DATASET_NAME + "a-78912.rec");
  private static final File FULL_VSR_XD_DUAL =
    new File(TestManager.DATASET_NAME + "a_full.vsr01123");
  private static final File SUB_VSR_XD_DUAL =
    new File(TestManager.DATASET_NAME + "b_sub.vsr231");
  private static final File PROCESS_CHUNKS_REC_XD_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "b-45612_rec.mrc");
  private static final File FULL_VSR_XD_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "b_full_vsr78123.mrc");
  private static final File SUB_VSR_XD_MRC_DUAL =
    new File(TestManager.DATASET_NAME + "a_sub_vsr901.mrc");
  private static final File PROCESS_CHUNKS_REC_XD_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "a-12312_rec.hdf");
  private static final File FULL_VSR_XD_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "a_full_vsr45123.hdf");
  private static final File SUB_VSR_XD_HDF_DUAL =
    new File(TestManager.DATASET_NAME + "b_sub_vsr671.hdf");

  public void setUp() {
    FILTER_SINGLE_OLD.setAcceptPretrimmedTomograms();
    FILTER_SINGLE_MRC.setAcceptPretrimmedTomograms();
    FILTER_SINGLE_HDF.setAcceptPretrimmedTomograms();
    FILTER_DUAL_OLD.setAcceptPretrimmedTomograms();
    FILTER_DUAL_MRC.setAcceptPretrimmedTomograms();
    FILTER_DUAL_HDF.setAcceptPretrimmedTomograms();
  }

  public void testRegression() {
    test(true, VOLCOMBINE_LOG_DUAL);
    test(true, PROCESS_CHUNKS_VOLCOMBINE_LOG_DUAL);
    test(true, PROCESS_CHUNKS_VOLCOMBINE_COM_DUAL);
    test(true, BACKUP, BACKUP_DUAL);
    test(true, REC_MAT, REC_MAT_DUAL);
    test(true, REC_WRP, REC_WRP_DUAL);
    test(true, PROCESS_CHUNKS_TILT_LOG, PROCESS_CHUNKS_TILT_LOG_DUAL);
    test(true, PROCESS_CHUNKS_TILT_COM, PROCESS_CHUNKS_TILT_COM_DUAL);
    test(false, "Non-standardized mrc files are not included in filter.", MRC_FAIL,
      MRC_FAIL_DUAL);
    test(false, "Non-standardized hdf files are not included in filter.", HDF_FAIL,
      HDF_FAIL_DUAL);
    test(false, "Too few digits.", PROCESS_CHUNKS_TILT_LOG_FAIL,
      PROCESS_CHUNKS_TILT_LOG_FAIL_DUAL);
    test(false, "Too few digits.", PROCESS_CHUNKS_TILT_COM_FAIL,
      PROCESS_CHUNKS_TILT_COM_FAIL_DUAL);
    test(true, FIXED_X_RAYS, FIXED_X_RAYS_MRC, FIXED_X_RAYS_HDF, FIXED_X_RAYS_DUAL,
      FIXED_X_RAYS_MRC_DUAL, FIXED_X_RAYS_HDF_DUAL);
    test(true, ALI, ALI_MRC, ALI_HDF, ALI_DUAL, ALI_MRC_DUAL, ALI_HDF_DUAL);
    test(true, PREALI, PREALI_MRC, PREALI_HDF, PREALI_DUAL, PREALI_MRC_DUAL,
      PREALI_HDF_DUAL);
    // FIXME 2206
    /*    test(true, BOT, BOT_MRC, BOT_HDF, BOT_DUAL, BOT_MRC_DUAL, BOT_HDF_DUAL);
    test(true, MID, MID_MRC, MID_HDF, MID_DUAL, MID_MRC_DUAL, MID_HDF_DUAL);
    test(true, TOP, TOP_MRC, TOP_HDF, TOP_DUAL, TOP_MRC_DUAL, TOP_HDF_DUAL);*/
    test(true, BL, BL_MRC, BL_HDF, BL_DUAL, BL_MRC_DUAL, BL_HDF_DUAL);
    test(true, DCST, DCST_MRC, DCST_HDF, DCST_DUAL, DCST_MRC_DUAL, DCST_HDF_DUAL);
    test(true, ALI_LOG10, ALI_LOG10_MRC, ALI_LOG10_HDF, ALI_LOG10_DUAL,
      ALI_LOG10_MRC_DUAL, ALI_LOG10_HDF_DUAL);
    test(true, VSR, VSR_MRC, VSR_HDF, VSR_DUAL, VSR_MRC_DUAL, VSR_HDF_DUAL);
    // FIXME 2206
    /*test(true, THREE_D_FIND, THREE_D_FIND_MRC, THREE_D_FIND_HDF, THREE_D_FIND_DUAL,
      THREE_D_FIND_MRC_DUAL, THREE_D_FIND_HDF_DUAL);
    test(true, OUTPUT, OUTPUT_MRC, OUTPUT_HDF, OUTPUT_DUAL, OUTPUT_MRC_DUAL,
      OUTPUT_HDF_DUAL);*/
    test(true, PROCESS_CHUNKS_REC, PROCESS_CHUNKS_REC_MRC, PROCESS_CHUNKS_REC_HDF,
      PROCESS_CHUNKS_REC_DUAL, PROCESS_CHUNKS_REC_MRC_DUAL, PROCESS_CHUNKS_REC_HDF_DUAL);
    test(true, ERASED_BEADS, ERASED_BEADS_MRC, ERASED_BEADS_HDF, ERASED_BEADS_DUAL,
      ERASED_BEADS_MRC_DUAL, ERASED_BEADS_HDF_DUAL);
    test(true, CTF_CORRECTED, CTF_CORRECTED_MRC, CTF_CORRECTED_HDF, CTF_CORRECTED_DUAL,
      CTF_CORRECTED_MRC_DUAL, CTF_CORRECTED_HDF_DUAL);
    test(true, MTF_FILTERED, MTF_FILTERED_MRC, MTF_FILTERED_HDF, MTF_FILTERED_DUAL,
      MTF_FILTERED_MRC_DUAL, MTF_FILTERED_HDF_DUAL);
    test(true, FULL_VSR, FULL_VSR_MRC, FULL_VSR_HDF, FULL_VSR_DUAL, FULL_VSR_MRC_DUAL,
      FULL_VSR_HDF_DUAL);
    test(true, SUB_VSR, SUB_VSR_MRC, SUB_VSR_HDF, SUB_VSR_DUAL, SUB_VSR_MRC_DUAL,
      SUB_VSR_HDF_DUAL);
    test(false, false, "Nad is not included in filter.", NAD_FAIL, NAD_FAIL_MRC,
      NAD_FAIL_HDF, NAD_FAIL_DUAL, NAD_FAIL_MRC_DUAL, NAD_FAIL_HDF_DUAL);
  }

  public void testNewFunctionality() {
    String extraDigitsDescr =
      "Processchunks/SIRT: the file # digits can go beyond the minimum.";
    test(true, false, extraDigitsDescr, PROCESS_CHUNKS_REC_XD, PROCESS_CHUNKS_REC_XD_MRC,
      PROCESS_CHUNKS_REC_XD_HDF, PROCESS_CHUNKS_REC_XD_DUAL,
      PROCESS_CHUNKS_REC_XD_MRC_DUAL, PROCESS_CHUNKS_REC_XD_HDF_DUAL);
    test(true, false, extraDigitsDescr, FULL_VSR_XD, FULL_VSR_XD_MRC, FULL_VSR_XD_HDF,
      FULL_VSR_XD_DUAL, FULL_VSR_XD_MRC_DUAL, FULL_VSR_XD_HDF_DUAL);
    test(true, false, extraDigitsDescr, SUB_VSR_XD, SUB_VSR_XD_MRC, SUB_VSR_XD_HDF,
      SUB_VSR_XD_DUAL, SUB_VSR_XD_MRC_DUAL, SUB_VSR_XD_HDF_DUAL);
    test(true, false, extraDigitsDescr, PROCESS_CHUNKS_TILT_LOG_XD,
      PROCESS_CHUNKS_TILT_LOG_XD_DUAL);
    test(true, false, extraDigitsDescr, PROCESS_CHUNKS_TILT_COM_XD,
      PROCESS_CHUNKS_TILT_COM_XD_DUAL);
    test(true, false, extraDigitsDescr, PROCESS_CHUNKS_VOLCOMBINE_LOG_XD_DUAL);
    test(true, false, extraDigitsDescr, PROCESS_CHUNKS_VOLCOMBINE_COM_XD_DUAL);

  }

  private void test(final boolean accept, final File file) {
    test(accept, accept, null, file, file, file, file, file, file);
  }

  private void test(final boolean accept, final boolean oldFunctionAccept,
    final String reason, final File file) {
    test(accept, oldFunctionAccept, reason, file, file, file, file, file, file);
  }

  private void test(final boolean accept, final File singleFile, final File dualFile) {
    test(accept, accept, null, singleFile, singleFile, singleFile, dualFile, dualFile,
      dualFile);
  }

  private void test(final boolean accept, final String reason, final File singleFile,
    final File dualFile) {
    test(accept, accept, reason, singleFile, singleFile, singleFile, dualFile, dualFile,
      dualFile);
  }

  private void test(final boolean accept, final boolean oldFunctionAccept,
    final String reason, final File singleFile, final File dualFile) {
    test(accept, oldFunctionAccept, reason, singleFile, singleFile, singleFile, dualFile,
      dualFile, dualFile);
  }

  private void test(final boolean accept, final File oldFile, final File mrcFile,
    final File hdfFile, final File oldDualFile, final File mrcDualFile,
    final File hdfDualFile) {
    test(accept, accept, null, oldFile, mrcFile, hdfFile, oldDualFile, mrcDualFile,
      hdfDualFile);
  }

  @SuppressWarnings("deprecation")
  private void test(final boolean accept, final boolean oldFunctionAccept, String reason,
    final File oldFile, final File mrcFile, final File hdfFile, final File oldDualFile,
    final File mrcDualFile, final File hdfDualFile) {
    String ar = "filter should generate the suffix of this file or be able to match it (";
    String nar =
      "filter does not generate the suffix of this file or is unable to match it (";
    String notAcceptReason;
    if (accept) {
      if (reason == null) {
        reason = ar;
      }
      notAcceptReason = nar;
    }
    else {
      if (reason == null) {
        reason = nar;
      }
      notAcceptReason = ar;
    }
    String axisType = "single/";
    String style = OLD_STYLE;
    String algorithm = OLD_ALGORITHM;
    File file = oldFile;
    assertEquals(
      axisType + style + algorithm + notAcceptReason + file.getName() + CLOSE_REASON,
      oldFunctionAccept, FILTER_SINGLE_OLD.acceptForRegressionTest(file));
    algorithm = NEW_ALGORITHM;
    assertEquals(axisType + style + algorithm + reason + file.getName() + CLOSE_REASON,
      accept, FILTER_SINGLE_OLD.accept(file));
    style = MRC_STYLE;
    file = mrcFile;
    assertEquals(axisType + style + algorithm + reason + file.getName() + CLOSE_REASON,
      accept, FILTER_SINGLE_MRC.accept(mrcFile));
    style = HDF_STYLE;
    file = hdfFile;
    assertEquals(axisType + style + algorithm + reason + file.getName() + CLOSE_REASON,
      accept, FILTER_SINGLE_HDF.accept(hdfFile));
    axisType = "dual/";
    style = OLD_STYLE;
    algorithm = OLD_ALGORITHM;
    file = oldDualFile;
    assertEquals(
      axisType + style + algorithm + notAcceptReason + file.getName() + CLOSE_REASON,
      oldFunctionAccept, FILTER_DUAL_OLD.acceptForRegressionTest(file));
    algorithm = NEW_ALGORITHM;
    assertEquals(axisType + style + algorithm + reason + file.getName() + CLOSE_REASON,
      accept, FILTER_DUAL_OLD.accept(file));
    style = MRC_STYLE;
    file = mrcDualFile;
    assertEquals(axisType + style + algorithm + reason + file.getName() + CLOSE_REASON,
      accept, FILTER_DUAL_MRC.accept(file));
    style = HDF_STYLE;
    file = hdfDualFile;
    assertEquals(axisType + style + algorithm + reason + file.getName() + CLOSE_REASON,
      accept, FILTER_DUAL_HDF.accept(file));
  }
}
