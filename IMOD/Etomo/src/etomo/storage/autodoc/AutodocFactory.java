package etomo.storage.autodoc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.storage.AutodocFilter;
import etomo.storage.LogFile;
import etomo.type.AxisID;
import etomo.type.FileType;

/**
 * <p>Description: Creates Autodoc classes.</p>
 * <p/>
 * <p>Copyright: Copyright 2006 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *          <p/>
 *          <p> $Log$
 *          <p> Revision 1.19  2011/02/18 22:51:44  sueh
 *          <p> bug# 1437 Reformatting.
 *          <p>
 *          <p> Revision 1.18  2010/03/05 03:58:58  sueh
 *          <p> bug# 1319 Added the tilt autodoc.
 *          <p>
 *          <p> Revision 1.17  2010/02/17 04:49:43  sueh
 *          <p> bug# 1301 Using the manager instead of the manager key do pop up
 *          <p> messages.
 *          <p>
 *          <p> Revision 1.16  2009/09/01 03:18:16  sueh
 *          <p> bug# 1222
 *          <p>
 *          <p> Revision 1.15  2009/06/05 02:01:14  sueh
 *          <p> bug# 1219 Added FLATTEN_WARP, FLATTEN_WARP_INSTANCE,
 *          <p> WARP_VOL, and WARP_VOL_INSTANCE.
 *          <p>
 *          <p> Revision 1.14  2009/03/17 00:45:53  sueh
 *          <p> bug# 1186 Pass managerKey to everything that pops up a dialog.
 *          <p>
 *          <p> Revision 1.13  2009/03/09 17:27:54  sueh
 *          <p> bug# 1199 Added a getInstance function that takes a File and doesn't
 *          <p> require a version.
 *          <p>
 *          <p> Revision 1.12  2009/02/04 23:30:00  sueh
 *          <p> bug# 1158 Changed id and exceptions classes in LogFile.
 *          <p>
 *          <p> Revision 1.11  2009/01/20 19:33:30  sueh
 *          <p> bug# 1102 In getInstance(String,AxisID) added else to if name equals CPU.
 *          <p>
 *          <p> Revision 1.10  2008/10/27 18:35:26  sueh
 *          <p> bug# 1141 Added ctfplotter and ctfcorrection
 *          <p>
 *          <p> Revision 1.9  2008/05/30 21:23:42  sueh
 *          <p> bug# 1102 Added writable.  Will be used to limit functionality of non-
 *          <p> matlab autodocs to original autodoc definition.
 *          <p>
 *          <p> Revision 1.8  2008/01/31 20:24:49  sueh
 *          <p> bug# 1055 throwing a FileException when LogFile.getInstance fails.
 *          <p>
 *          <p> Revision 1.7  2007/08/16 16:31:55  sueh
 *          <p> bug# 1035 Added NEWSTACK.
 *          <p>
 *          <p> Revision 1.6  2007/06/07 21:31:52  sueh
 *          <p> bug# 1012 Added function getMatlabDebugInstance.
 *          <p>
 *          <p> Revision 1.5  2007/04/13 18:42:59  sueh
 *          <p> bug# 964 Added getDebugInstance(String, AxisID).
 *          <p>
 *          <p> Revision 1.4  2007/03/26 23:33:52  sueh
 *          <p> bug# 964 Added getInstance(String name) which opens an n'ton autodoc with
 *          <p> AxisID.ONLY.
 *          <p>
 *          <p> Revision 1.3  2007/03/26 18:36:41  sueh
 *          <p> bug# 964 Made Version optional so that it is not necessary in matlab
 *          param files.
 *          <p>
 *          <p> Revision 1.2  2007/03/23 20:32:07  sueh
 *          <p> bug# 964 Added PEET_PRM - an autodoc which contains Feld sections that
 *          <p> represent the fields that may be used in the PEET .prm file.
 *          <p>
 *          <p> Revision 1.1  2007/03/21 18:14:50  sueh
 *          <p> bug# 964 Limiting access to autodoc classes by using ReadOnly interfaces.
 *          <p> Added AutodocFactory to create Autodoc instances.
 *          <p> </p>
 */
public final class AutodocFactory {
  public static final String VERSION = "1.2";
  public static final String TILTXCORR = "tiltxcorr";
  public static final String MTF_FILTER = "mtffilter";
  public static final String COMBINE_FFT = "combinefft";
  public static final String TILTALIGN = "tiltalign";
  public static final String CCDERASER = "ccderaser";
  public static final String SOLVEMATCH = "solvematch";
  public static final String BEADTRACK = "beadtrack";
  public static final String DENS_MATCH = "densmatch";
  public static final String CORR_SEARCH_3D = "corrsearch3d";
  public static final String XFJOINTOMO = "xfjointomo";
  public static final String CPU = "cpu";
  public static final String UITEST = "uitest";
  public static final String PEET_PRM = "peetprm";
  public static final String NEWSTACK = "newstack";
  public static final String CTF_PLOTTER = "ctfplotter";
  public static final String CTF_PHASE_FLIP = "ctfphaseflip";
  public static final String FLATTEN_WARP = "flattenwarp";
  public static final String WARP_VOL = "warpvol";
  public static final String FIND_BEADS_3D = "findbeads3d";
  public static final String TILT = "tilt";
  public static final String SIRTSETUP = "sirtsetup";
  public static final String BLENDMONT = "blendmont";
  public static final String XFTOXG = "xftoxg";
  public static final String XFALIGN = "xfalign";
  public static final String AUTOFIDSEED = "autofidseed";
  public static final String ETOMO = "etomo";
  public static final String PROG_DEFAULTS = "progDefaults";
  public static final String IMODCHOPCONTS = "imodchopconts";
  public static final String DUALVOLMATCH = "dualvolmatch";
  public static final String RESTRICT_ALIGN = "restrictalign";
  public static final String BATCH_RUN_TOMO = "batchruntomo";
  public static final String MULTIFILT_SETUP = "multifiltsetup";

  private static final String TEST = "test";
  private static final String UITEST_AXIS = "uitest_axis";

  private static Autodoc TILTXCORR_INSTANCE = null;
  private static Autodoc TEST_INSTANCE = null;
  private static Autodoc UITEST_INSTANCE = null;
  private static Autodoc MTF_FILTER_INSTANCE = null;
  private static Autodoc COMBINE_FFT_INSTANCE = null;
  private static Autodoc TILTALIGN_INSTANCE = null;
  private static Autodoc CCDERASER_INSTANCE = null;
  private static Autodoc SOLVEMATCH_INSTANCE = null;
  private static Autodoc BEADTRACK_INSTANCE = null;
  private static Autodoc CPU_INSTANCE = null;
  private static Autodoc DENS_MATCH_INSTANCE = null;
  private static Autodoc CORR_SEARCH_3D_INSTANCE = null;
  private static Autodoc XFJOINTOMO_INSTANCE = null;
  private static Autodoc PEET_PRM_INSTANCE = null;
  private static Autodoc NEWSTACK_INSTANCE = null;
  private static Autodoc CTF_PLOTTER_INSTANCE = null;
  private static Autodoc CTF_PHASE_FLIP_INSTANCE = null;
  private static Autodoc FLATTEN_WARP_INSTANCE = null;
  private static Autodoc WARP_VOL_INSTANCE = null;
  private static Autodoc FIND_BEADS_3D_INSTANCE = null;
  private static Autodoc TILT_INSTANCE = null;
  private static Autodoc SIRTSETUP_INSTANCE = null;
  private static Autodoc BLENDMONT_INSTANCE = null;
  private static Autodoc XFTOXG_INSTANCE = null;
  private static Autodoc XFALIGN_INSTANCE = null;
  private static Autodoc AUTOFIDSEED_INSTANCE = null;
  private static Autodoc ETOMO_INSTANCE = null;
  private static Autodoc PROG_DEFAULTS_INSTANCE = null;
  private static Autodoc IMODCHOPCONTS_INSTANCE = null;
  private static Autodoc DUALVOLMATCH_INSTANCE = null;
  private static Autodoc RESTRICT_ALIGN_INSTANCE = null;
  private static Autodoc BATCH_RUN_TOMO_INSTANCE = null;
  private static Autodoc MULTIFILT_SETUP_INSTANCE = null;

  private static final HashMap UITEST_AXIS_MAP = new HashMap();

  private static String replacementDir = null;

  private AutodocFactory() {}

  public static ReadOnlyAutodoc getInstance(BaseManager manager, String name)
    throws FileNotFoundException, IOException, LogFile.LockException {
    return getInstance(manager, name, AxisID.ONLY, false);
  }

  public static ReadOnlyAutodoc getComInstance(String name)
    throws FileNotFoundException, IOException, LogFile.LockException {
    if (name == null) {
      throw new IllegalStateException("name is null");
    }
    Autodoc autodoc = getExistingAutodoc(name);
    if (autodoc != null) {
      return autodoc;
    }
    autodoc = new Autodoc(name, null);
    autodoc.initializeGenericInstance(null, EtomoDirector.IMOD_DIR_ENV_VAR,
      FileType.COM_DIR, name, AxisID.ONLY, false);
    return autodoc;
  }

  public static Autodoc getUnmanagedAutodocInstance(final BaseManager manager,
    final String name, final FileType fileType, final AxisID axisID)
    throws FileNotFoundException, IOException, LogFile.LockException {
    if (name == null) {
      throw new IllegalStateException("name is null");
    }
    Autodoc autodoc = new Autodoc(name, null);
    File autodocFile = null;
    if (fileType != null) {
      autodocFile = fileType.getFile(manager, axisID);
    }
    autodoc.initializeUnmanagedAutodocInstance(manager, name, autodocFile, axisID);
    return autodoc;
  }

  public static ReadOnlyAutodoc getInstance(final BaseManager manager, final String name,
    final AxisID axisID, final boolean debug)
    throws FileNotFoundException, IOException, LogFile.LockException {
    if (name == null) {
      throw new IllegalStateException("name is null");
    }
    Autodoc autodoc = getExistingAutodoc(name);
    if (autodoc != null) {
      return autodoc;
    }
    autodoc = new Autodoc(name, null);
    setInstance(name, autodoc);
    autodoc.setDebug(debug);
    if (name.equals(UITEST)) {
      autodoc.initializeUITestInstance(manager, name, axisID);
    }
    if (name.equals(CPU)) {
      autodoc.initializeCpuInstance(manager, name, axisID);
    }
    else {
      autodoc.initializeAutodocInstance(manager, name, axisID);
    }
    return autodoc;
  }

  public static WritableAutodoc getMatlabInstance(final BaseManager manager,
    final File file, final boolean writable) throws IOException, LogFile.LockException {
    if (file == null) {
      throw new IllegalStateException("file is null");
    }
    Autodoc autodoc = new Autodoc(stripFileExtension(file), null);
    try {
      autodoc.initializeMatlabInstance(manager, file, writable);
      return autodoc;
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static WritableAutodoc getWritableInstance(final BaseManager manager,
    final File file) throws IOException, LogFile.LockException {
    if (file == null) {
      throw new IllegalStateException("file is null");
    }
    Autodoc autodoc = new Autodoc(stripFileExtension(file), null);
    try {
      autodoc.initializeWritableInstance(manager, file);
      return autodoc;
    }
    catch (FileNotFoundException e) {
      return null;
    }
  }

  public static WritableAutodoc getEmptyWritableInstance(final BaseManager manager,
    final File file) throws IOException, LogFile.LockException {
    if (file == null) {
      throw new IllegalStateException("file is null");
    }
    Autodoc autodoc = new Autodoc(stripFileExtension(file), null);
    try {
      autodoc.initializeEmptyWritableInstance(manager, file);
      return autodoc;
    }
    catch (FileNotFoundException e) {
      return null;
    }
  }

  private static String stripFileExtension(final File file) {
    return stripFileExtension(file.getName());
  }

  private static String stripFileExtension(final String fileName) {
    int extensionIndex = fileName.lastIndexOf('.');
    if (extensionIndex == -1) {
      return fileName;
    }
    String retval = fileName.substring(0, extensionIndex);
    return retval;
  }

  public static WritableAutodoc getEmptyMatlabInstance(final BaseManager manager,
    final File file) throws IOException, LogFile.LockException {
    if (file == null) {
      throw new IllegalStateException("file is null");
    }
    Autodoc autodoc = new Autodoc(stripFileExtension(file), null);
    try {
      autodoc.initializeEmptyMatlapInstance(manager, file);
      return autodoc;
    }
    catch (FileNotFoundException e) {
      return null;
    }
  }

  public static ReadOnlyAutodoc getInstance(BaseManager manager, File file,
    final boolean writable) throws IOException, LogFile.LockException {
    if (file == null) {
      throw new IllegalStateException("file is null");
    }
    Autodoc autodoc = new Autodoc(stripFileExtension(file), null);
    try {
      autodoc.initializeGenericInstance(manager, file, null, writable, null);
      return autodoc;
    }
    catch (FileNotFoundException e) {
      return null;
    }
  }

  public static ReadOnlyAutodoc getUnmanagedInstance(final BaseManager manager,
    final AxisID axisID, final FileType autodocFileType)
    throws IOException, LogFile.LockException {
    if (autodocFileType == null) {
      System.err.println("Warning: autodocFileType is null");
      Thread.dumpStack();
      return null;
    }
    return getUnmanagedInstance(manager, axisID,
      autodocFileType.getFile(manager, axisID));
  }

  public static ReadOnlyAutodoc getUnmanagedInstance(final BaseManager manager,
    final AxisID axisID, final File autodocFile)
    throws IOException, LogFile.LockException {
    if (autodocFile == null) {
      System.err.println("Warning: autodocFile is null");
      Thread.dumpStack();
      return null;
    }
    Autodoc autodoc = new Autodoc(stripFileExtension(autodocFile), null);
    try {
      autodoc.initializeGenericInstance(manager, autodocFile, axisID, false, null);
      return autodoc;
    }
    catch (FileNotFoundException e) {
      return null;
    }
  }

  /**
   * Initialize one of the known autodoc instances with a generic instance.  If the known
   * autodoc has an instance assigned to it, this overrides the old instance with the one
   * created here.
   *
   * @param manager
   * @param file
   * @param autodocName
   * @return
   * @throws IOException
   * @throws LogFile.LockException
   */
  public static ReadOnlyAutodoc getInstance(BaseManager manager, File file,
    final String autodocName, final boolean writable)
    throws IOException, LogFile.LockException {
    if (file == null) {
      throw new IllegalStateException("file is null");
    }
    Autodoc autodoc = new Autodoc(stripFileExtension(file), null);
    try {
      autodoc.initializeGenericInstance(manager, file, null, writable, null);
      if (autodocName != null) {
        setInstance(autodocName, autodoc);
      }
      return autodoc;
    }
    catch (FileNotFoundException e) {
      return null;
    }
  }

  /**
   * open and preserve an autodoc without a type for testing
   *
   * @param axisID
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static Autodoc getTestInstance(final BaseManager manager, final File directory,
    final String autodocFileName, final AxisID axisID)
    throws FileNotFoundException, IOException, LogFile.LockException {
    if (autodocFileName == null) {
      return null;
    }
    File autodocFile = new File(directory, autodocFileName);
    AutodocFilter filter = new AutodocFilter();
    if (!filter.accept(autodocFile)) {
      throw new IllegalArgumentException(autodocFile + " is not an autodoc.");
    }
    if (EtomoDirector.INSTANCE.getArguments().isTest()) {
      System.err.println("autodoc file:" + autodocFile.getAbsolutePath());
    }
    Autodoc autodoc = getExistingUITestAxisAutodoc(autodocFile);
    if (autodoc != null) {
      return autodoc;
    }
    autodoc = new Autodoc(stripFileExtension(autodocFileName), null);
    UITEST_AXIS_MAP.put(autodocFile, autodoc);
    autodoc.initializeGenericInstance(manager, autodocFile, axisID, false, null);
    return autodoc;
  }

  /**
   * open and return an autodoc without a type.
   *
   * @param autodocFile
   * @param axisID
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static Autodoc getInstance(final BaseManager manager, final File autodocFile,
    final AxisID axisID, final boolean writable)
    throws FileNotFoundException, IOException, LogFile.LockException {
    if (autodocFile == null || !autodocFile.exists()) {
      return null;
    }
    AutodocFilter filter = new AutodocFilter();
    if (!filter.accept(autodocFile)) {
      throw new IllegalArgumentException(autodocFile + " is not an autodoc.");
    }
    if (EtomoDirector.INSTANCE.getArguments().isTest()) {
      System.err.println("autodoc file:" + autodocFile.getAbsolutePath());
    }
    Autodoc autodoc = new Autodoc(stripFileExtension(autodocFile.getName()), null);
    autodoc.initializeGenericInstance(manager, autodocFile, axisID, writable, null);
    return autodoc;
  }

  /**
   * Causes all autodocs that don't already exist, and whose location comes from an
   * environment variable to be opened in the replacement directory instead of the
   * location specified by the environment variable.
   */
  public static void setReplacementDir(final String input) {
    replacementDir = input;
  }

  public static String getReplacementDir() {
    return replacementDir;
  }

  /**
   * return writable autodoc
   *
   * @param manager
   * @param autodocFile
   * @return
   * @throws IOException
   * @throws LogFile.LockException
   */
  public static Autodoc getWritableAutodocInstance(final BaseManager manager,
    final File autodocFile) throws IOException, LogFile.LockException {
    if (autodocFile == null) {
      return null;
    }
    Autodoc autodoc;
    autodoc = new Autodoc(stripFileExtension(autodocFile.getName()), null);
    autodoc.initializeGenericInstance(manager, autodocFile, AxisID.ONLY, true, null);
    return autodoc;
  }

  public static Autodoc getAutodocInstance(final BaseManager manager,
    final File autodocFile, final StringBuilder errMsg)
    throws IOException, LogFile.LockException {
    if (autodocFile == null) {
      return null;
    }
    Autodoc autodoc;
    autodoc = new Autodoc(stripFileExtension(autodocFile.getName()), errMsg);
    autodoc.initializeGenericInstance(manager, autodocFile, AxisID.ONLY, false, errMsg);
    return autodoc;
  }

  public static Autodoc getAutodocInstance(final BaseManager manager,
    final File autodocFile) throws IOException, LogFile.LockException {
    if (autodocFile == null) {
      return null;
    }
    Autodoc autodoc;
    autodoc = new Autodoc(stripFileExtension(autodocFile.getName()), null);
    autodoc.initializeGenericInstance(manager, autodocFile, AxisID.ONLY, false, null);
    return autodoc;
  }

  private static Autodoc getExistingUITestAxisAutodoc(File autodocFile) {
    if (UITEST_AXIS_MAP == null) {
      return null;
    }
    Autodoc autodoc = (Autodoc) UITEST_AXIS_MAP.get(autodocFile);
    return autodoc;
  }

  private static Autodoc getExistingAutodoc(String fileName, String name) {
    if (name.equals(UITEST_AXIS)) {
      if (UITEST_AXIS_MAP == null) {
        return null;
      }
      return (Autodoc) UITEST_AXIS_MAP.get(fileName);
    }
    throw new IllegalArgumentException("Illegal autodoc name: " + name + ".");
  }

  private static Autodoc getExistingAutodoc(final String name) {
    if (name.equals(TILTXCORR)) {
      return TILTXCORR_INSTANCE;
    }
    if (name.equals(TEST)) {
      return TEST_INSTANCE;
    }
    if (name.equals(UITEST)) {
      return UITEST_INSTANCE;
    }
    if (name.equals(MTF_FILTER)) {
      return MTF_FILTER_INSTANCE;
    }
    if (name.equals(NEWSTACK)) {
      return NEWSTACK_INSTANCE;
    }
    if (name.equals(CTF_PLOTTER)) {
      return CTF_PLOTTER_INSTANCE;
    }
    if (name.equals(CTF_PHASE_FLIP)) {
      return CTF_PHASE_FLIP_INSTANCE;
    }
    if (name.equals(FLATTEN_WARP)) {
      return FLATTEN_WARP_INSTANCE;
    }
    if (name.equals(WARP_VOL)) {
      return WARP_VOL_INSTANCE;
    }
    if (name.equals(FIND_BEADS_3D)) {
      return FIND_BEADS_3D_INSTANCE;
    }
    if (name.equals(COMBINE_FFT)) {
      return COMBINE_FFT_INSTANCE;
    }
    if (name.equals(TILTALIGN)) {
      return TILTALIGN_INSTANCE;
    }
    if (name.equals(CCDERASER)) {
      return CCDERASER_INSTANCE;
    }
    if (name.equals(SOLVEMATCH)) {
      return SOLVEMATCH_INSTANCE;
    }
    if (name.equals(BEADTRACK)) {
      return BEADTRACK_INSTANCE;
    }
    if (name.equals(CPU)) {
      return CPU_INSTANCE;
    }
    if (name.equals(DENS_MATCH)) {
      return DENS_MATCH_INSTANCE;
    }
    if (name.equals(CORR_SEARCH_3D)) {
      return CORR_SEARCH_3D_INSTANCE;
    }
    if (name.equals(XFJOINTOMO)) {
      return XFJOINTOMO_INSTANCE;
    }
    if (name.equals(PEET_PRM)) {
      return PEET_PRM_INSTANCE;
    }
    if (name.equals(TILT)) {
      return TILT_INSTANCE;
    }
    if (name.equals(SIRTSETUP)) {
      return SIRTSETUP_INSTANCE;
    }
    if (name.equals(BLENDMONT)) {
      return BLENDMONT_INSTANCE;
    }
    if (name.equals(XFTOXG)) {
      return XFTOXG_INSTANCE;
    }
    if (name.equals(XFALIGN)) {
      return XFALIGN_INSTANCE;
    }
    if (name.equals(AUTOFIDSEED)) {
      return AUTOFIDSEED_INSTANCE;
    }
    if (name.equals(ETOMO)) {
      return ETOMO_INSTANCE;
    }
    if (name.equals(PROG_DEFAULTS)) {
      return PROG_DEFAULTS_INSTANCE;
    }
    if (name.equals(IMODCHOPCONTS)) {
      return IMODCHOPCONTS_INSTANCE;
    }
    if (name.equals(DUALVOLMATCH)) {
      return DUALVOLMATCH_INSTANCE;
    }
    if (name.equals(RESTRICT_ALIGN)) {
      return RESTRICT_ALIGN_INSTANCE;
    }
    if (name.equals(BATCH_RUN_TOMO)) {
      return BATCH_RUN_TOMO_INSTANCE;
    }
    if (name.equals(MULTIFILT_SETUP)) {
      return MULTIFILT_SETUP_INSTANCE;
    }
    return null;
  }

  public static boolean isLoaded(final String name) {
    return getExistingAutodoc(name) != null;
  }

  /**
   * for testing
   *
   * @param name
   */
  public static void resetInstance(String name) {
    if (name.equals(TILTXCORR)) {
      TILTXCORR_INSTANCE = null;
    }
    else if (name.equals(TEST)) {
      TEST_INSTANCE = null;
    }
    else if (name.equals(UITEST)) {
      UITEST_INSTANCE = null;
    }
    else if (name.equals(MTF_FILTER)) {
      MTF_FILTER_INSTANCE = null;
    }
    else if (name.equals(NEWSTACK)) {
      NEWSTACK_INSTANCE = null;
    }
    else if (name.equals(CTF_PLOTTER)) {
      CTF_PLOTTER_INSTANCE = null;
    }
    else if (name.equals(CTF_PHASE_FLIP)) {
      CTF_PHASE_FLIP_INSTANCE = null;
    }
    else if (name.equals(FLATTEN_WARP)) {
      FLATTEN_WARP_INSTANCE = null;
    }
    else if (name.equals(WARP_VOL)) {
      WARP_VOL_INSTANCE = null;
    }
    else if (name.equals(FIND_BEADS_3D)) {
      FIND_BEADS_3D_INSTANCE = null;
    }
    else if (name.equals(COMBINE_FFT)) {
      COMBINE_FFT_INSTANCE = null;
    }
    else if (name.equals(TILTALIGN)) {
      TILTALIGN_INSTANCE = null;
    }
    else if (name.equals(CCDERASER)) {
      CCDERASER_INSTANCE = null;
    }
    else if (name.equals(SOLVEMATCH)) {
      SOLVEMATCH_INSTANCE = null;
    }
    else if (name.equals(BEADTRACK)) {
      BEADTRACK_INSTANCE = null;
    }
    else if (name.equals(CPU)) {
      CPU_INSTANCE = null;
    }
    else if (name.equals(DENS_MATCH)) {
      DENS_MATCH_INSTANCE = null;
    }
    else if (name.equals(CORR_SEARCH_3D)) {
      CORR_SEARCH_3D_INSTANCE = null;
    }
    else if (name.equals(XFJOINTOMO)) {
      XFJOINTOMO_INSTANCE = null;
    }
    else if (name.equals(PEET_PRM)) {
      PEET_PRM_INSTANCE = null;
    }
    else if (name.equals(TILT)) {
      TILT_INSTANCE = null;
    }
    else if (name.equals(SIRTSETUP)) {
      SIRTSETUP_INSTANCE = null;
    }
    else if (name.equals(BLENDMONT)) {
      BLENDMONT_INSTANCE = null;
    }
    else if (name.equals(XFTOXG)) {
      XFTOXG_INSTANCE = null;
    }
    else if (name.equals(XFALIGN)) {
      XFALIGN_INSTANCE = null;
    }
    else if (name.equals(AUTOFIDSEED)) {
      AUTOFIDSEED_INSTANCE = null;
    }
    else if (name.equals(ETOMO)) {
      ETOMO_INSTANCE = null;
    }
    else if (name.equals(PROG_DEFAULTS)) {
      PROG_DEFAULTS_INSTANCE = null;
    }
    else if (name.equals(IMODCHOPCONTS)) {
      IMODCHOPCONTS_INSTANCE = null;
    }
    else if (name.equals(DUALVOLMATCH)) {
      DUALVOLMATCH_INSTANCE = null;
    }
    else if (name.equals(RESTRICT_ALIGN)) {
      RESTRICT_ALIGN_INSTANCE = null;
    }
    else if (name.equals(BATCH_RUN_TOMO)) {
      BATCH_RUN_TOMO_INSTANCE = null;
    }
    else if (name.equals(MULTIFILT_SETUP)) {
      MULTIFILT_SETUP_INSTANCE = null;
    }
    else {
      throw new IllegalArgumentException("Illegal autodoc name: " + name + ".");
    }
  }

  /**
   * Override an old autodoc instance with a new one
   *
   * @param name
   * @param autodoc
   */
  private static boolean setInstance(String name, final Autodoc autodoc) {
    if (name.equals(TILTXCORR)) {
      TILTXCORR_INSTANCE = autodoc;
    }
    else if (name.equals(TEST)) {
      TEST_INSTANCE = autodoc;
    }
    else if (name.equals(UITEST)) {
      UITEST_INSTANCE = autodoc;
    }
    else if (name.equals(MTF_FILTER)) {
      MTF_FILTER_INSTANCE = autodoc;
    }
    else if (name.equals(NEWSTACK)) {
      NEWSTACK_INSTANCE = autodoc;
    }
    else if (name.equals(CTF_PLOTTER)) {
      CTF_PLOTTER_INSTANCE = autodoc;
    }
    else if (name.equals(CTF_PHASE_FLIP)) {
      CTF_PHASE_FLIP_INSTANCE = autodoc;
    }
    else if (name.equals(FLATTEN_WARP)) {
      FLATTEN_WARP_INSTANCE = autodoc;
    }
    else if (name.equals(WARP_VOL)) {
      WARP_VOL_INSTANCE = autodoc;
    }
    else if (name.equals(FIND_BEADS_3D)) {
      FIND_BEADS_3D_INSTANCE = autodoc;
    }
    else if (name.equals(COMBINE_FFT)) {
      COMBINE_FFT_INSTANCE = autodoc;
    }
    else if (name.equals(TILTALIGN)) {
      TILTALIGN_INSTANCE = autodoc;
    }
    else if (name.equals(CCDERASER)) {
      CCDERASER_INSTANCE = autodoc;
    }
    else if (name.equals(SOLVEMATCH)) {
      SOLVEMATCH_INSTANCE = autodoc;
    }
    else if (name.equals(BEADTRACK)) {
      BEADTRACK_INSTANCE = autodoc;
    }
    else if (name.equals(CPU)) {
      CPU_INSTANCE = autodoc;
    }
    else if (name.equals(DENS_MATCH)) {
      DENS_MATCH_INSTANCE = autodoc;
    }
    else if (name.equals(CORR_SEARCH_3D)) {
      CORR_SEARCH_3D_INSTANCE = autodoc;
    }
    else if (name.equals(XFJOINTOMO)) {
      XFJOINTOMO_INSTANCE = autodoc;
    }
    else if (name.equals(PEET_PRM)) {
      PEET_PRM_INSTANCE = autodoc;
    }
    else if (name.equals(TILT)) {
      TILT_INSTANCE = autodoc;
    }
    else if (name.equals(SIRTSETUP)) {
      SIRTSETUP_INSTANCE = autodoc;
    }
    else if (name.equals(BLENDMONT)) {
      BLENDMONT_INSTANCE = autodoc;
    }
    else if (name.equals(XFTOXG)) {
      XFTOXG_INSTANCE = autodoc;
    }
    else if (name.equals(XFALIGN)) {
      XFALIGN_INSTANCE = autodoc;
    }
    else if (name.equals(AUTOFIDSEED)) {
      AUTOFIDSEED_INSTANCE = autodoc;
    }
    else if (name.equals(ETOMO)) {
      ETOMO_INSTANCE = autodoc;
    }
    else if (name.equals(PROG_DEFAULTS)) {
      PROG_DEFAULTS_INSTANCE = autodoc;
    }
    else if (name.equals(IMODCHOPCONTS)) {
      IMODCHOPCONTS_INSTANCE = autodoc;
    }
    else if (name.equals(DUALVOLMATCH)) {
      DUALVOLMATCH_INSTANCE = autodoc;
    }
    else if (name.equals(RESTRICT_ALIGN)) {
      RESTRICT_ALIGN_INSTANCE = autodoc;
    }
    else if (name.equals(BATCH_RUN_TOMO)) {
      BATCH_RUN_TOMO_INSTANCE = autodoc;
    }
    else if (name.equals(MULTIFILT_SETUP)) {
      MULTIFILT_SETUP_INSTANCE = autodoc;
    }
    else {
      return false;
    }
    return true;
  }

  public static boolean endsWithAutodocExtension(final String path) {
    if (path == null) {
      return false;
    }
    return path.endsWith(Extension.DEFAULT.extension)
      || path.endsWith(Extension.MATLAB.extension);
  }

  public static final class Extension {
    public static final Extension DEFAULT = new Extension(".adoc");
    private static final Extension MATLAB = new Extension(".prm");

    private final String extension;

    private Extension(final String extension) {
      this.extension = extension;
    }

    public String toString() {
      return extension;
    }
  }
}
