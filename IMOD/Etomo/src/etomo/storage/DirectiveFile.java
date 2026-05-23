package etomo.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.storage.DirectiveAttribute.AttributeMatch;
import etomo.storage.DirectiveAttribute.Match;
import etomo.storage.autodoc.AutodocFactory;
import etomo.storage.autodoc.ReadOnlyAttribute;
import etomo.storage.autodoc.ReadOnlyAttributeIterator;
import etomo.storage.autodoc.ReadOnlyAttributeList;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.storage.autodoc.ReadOnlyStatement;
import etomo.storage.autodoc.StatementLocation;
import etomo.type.AxisID;
import etomo.type.DirectiveFileType;
import etomo.ui.swing.UIHarness;

/**
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright 2012 - 2018 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class DirectiveFile implements DirectiveFileInterface {
  public static final String rcsid = "$Id:$";

  public static final int AUTO_FIT_RANGE_INDEX = 0;
  public static final int AUTO_FIT_STEP_INDEX = 1;

  private final AxisID axisID;
  private final BaseManager manager;

  private File file = null;
  private DirectiveFileType directiveFileType = null;
  private ReadOnlyAttribute copyArg = null;
  private ReadOnlyAttribute runtime = null;
  private ReadOnlyAttribute setupSet = null;
  private ReadOnlyAttribute comparam = null;
  private boolean copyArgSet = false;
  private boolean runtimeSet = false;
  private boolean setupSetSet = false;
  private boolean comparamSet = false;
  private ReadOnlyAutodoc autodoc = null;
  private boolean debug = false;

  private DirectiveFile(final BaseManager manager, final AxisID axisID) {
    this.manager = manager;
    this.axisID = axisID;
  }

  public Iterator<ReadOnlyStatement> iterator(final boolean templateOnly) {
    if (directiveFileType != null && directiveFileType.isBatch() && templateOnly) {
      return null;
    }
    return new StatementIterator();
  }

  /**
   * Returns an instance loaded with the batch directive from the etomo parameters.
   * Returns null if an autodoc could not be loaded.
   *
   * @param manager
   * @param axisID
   * @return
   */
  public static DirectiveFile getArgInstance(final BaseManager manager,
    final AxisID axisID) {
    DirectiveFile instance = new DirectiveFile(manager, axisID);
    if (instance.setFile(EtomoDirector.INSTANCE.getArguments().getDirective(),
      DirectiveFileType.BATCH)) {
      return instance;
    }
    return null;
  }

  /**
   * Returns an instance loaded with the file parameter.  Returns null if an autodoc could
   * not be loaded.
   *
   * @param manager
   * @param axisID
   * @return
   */
  public static DirectiveFile getInstance(final BaseManager manager, final AxisID axisID,
    final File file, final DirectiveFileType directiveFileType) {
    DirectiveFile instance = new DirectiveFile(manager, axisID);
    if (instance.setFile(file, directiveFileType)) {
      return instance;
    }
    return null;
  }

  /**
   * Returns an attribute from this directive file that matches the parameters.
   *
   * @param match
   * @param directiveDef
   * @param axisID
   * @return
   */
  AttributeMatch getAttribute(final Match match, DirectiveDef directiveDef,
    final AxisID axisID, final boolean ignoreFileType) {
    if (directiveDef != null) {
      directiveDef = directiveDef.getAxisIDInstance(axisID);
    }
    boolean batch = directiveFileType != null && directiveFileType.isBatch();
    if (directiveDef == null
      || (!ignoreFileType && ((batch && !directiveDef.isBatch(axisID))
        || (!batch && !directiveDef.isTemplate(axisID))))) {
      return null;
    }
    return DirectiveAttribute.getMatch(match, this, getParentAttribute(directiveDef),
      directiveDef, axisID);
  }

  /**
   * Returns the best matching AttributeMatch.
   *
   * @param directiveDef
   * @param axisID
   * @return
   */
  AttributeMatch getAttribute(DirectiveDef directiveDef, final AxisID axisID,
    final boolean ignoreFileType, final boolean includeOverride) {
    if (directiveDef != null) {
      directiveDef = directiveDef.getAxisIDInstance(axisID);
    }
    boolean batch = directiveFileType != null && directiveFileType.isBatch();
    if (directiveDef == null
      || (!ignoreFileType && ((batch && !directiveDef.isBatch(axisID))
        || (!batch && !directiveDef.isTemplate(axisID))))) {
      return null;
    }
    AttributeMatch attributeMatch =
      getAttribute(Match.PRIMARY, directiveDef, axisID, ignoreFileType);
    if (attributeMatch != null && !attributeMatch.isEmpty()) {
      if (attributeMatch.isOverride() && !includeOverride) {
        return null;
      }
      return attributeMatch;
    }
    attributeMatch = getAttribute(Match.SECONDARY, directiveDef, axisID, ignoreFileType);
    if (attributeMatch != null && !attributeMatch.isEmpty()) {
      if (attributeMatch.isOverride() && !includeOverride) {
        return null;
      }
      return attributeMatch;
    }
    return null;
  }

  public boolean contains(final DirectiveDef directiveDef, final AxisID axisID) {
    return contains(directiveDef, axisID, false);
  }

  public boolean contains(final DirectiveDef directiveDef, final AxisID axisID,
    final boolean templateOnly) {
    return contains(directiveDef, axisID, templateOnly, false);
  }

  public boolean contains(final DirectiveDef directiveDef, final AxisID axisID,
    final boolean templateOnly, final boolean ignoreFileType) {
    return contains(directiveDef, axisID, templateOnly, ignoreFileType, false);
  }

  /**
   * Returns true if the directive file contains the directive attribute and does not
   * override it (an empty value for a non-boolean directive).
   *
   * @param directiveDef
   * @param axisID
   * @templateOnly
   * @return
   */
  public boolean contains(DirectiveDef directiveDef, final AxisID axisID,
    final boolean templateOnly, final boolean ignoreFileType,
    final boolean includeOverride) {
    if (directiveDef != null) {
      directiveDef = directiveDef.getAxisIDInstance(axisID);
    }
    if (templateOnly && directiveFileType != null && directiveFileType.isBatch()) {
      return false;
    }
    AttributeMatch attribute =
      getAttribute(directiveDef, axisID, ignoreFileType, includeOverride);
    return attribute != null;
  }

  public boolean containsValue(final DirectiveDef directiveDef) {
    AttributeMatch attribute = getAttribute(directiveDef, null, false, false);
    if (attribute != null) {
      String value = attribute.getValue();
      return value != null && !value.matches("\\s*");
    }
    return false;
  }

  /**
   * Returns true if the directive file contains the directive attribute and does not
   * override it (an empty value for a non-boolean directive).
   *
   * @param directiveDef
   * @return
   */
  public boolean contains(final DirectiveDef directiveDef) {
    return contains(directiveDef, null, false);
  }

  public boolean contains(final DirectiveDef directiveDef, final boolean templateOnly) {
    // template/batch issues are already being handled in getAttribute
    return contains(directiveDef, null, templateOnly);
  }

  public boolean contains(final DirectiveDef directiveDef, final boolean templateOnly,
    final boolean ignoreFileType) {
    // template/batch issues are already being handled in getAttribute
    return contains(directiveDef, null, templateOnly, ignoreFileType);
  }

  public boolean contains(final DirectiveDef directiveDef, final boolean templateOnly,
    final boolean ignoreFileType, final boolean includeOverride) {
    // template/batch issues are already being handled in getAttribute
    return contains(directiveDef, null, templateOnly, ignoreFileType, includeOverride);
  }

  DirectiveFileType getDirectiveFileType() {
    return directiveFileType;
  }

  public String getValue(final DirectiveDef directiveDef, final AxisID axisID) {
    DirectiveValue directiveValue = getValue(directiveDef, axisID, false, false);
    if (directiveValue == null) {
      return null;
    }
    return directiveValue.getValue();
  }

  public DirectiveValue getValue(final DirectiveDef directiveDef, final AxisID axisID,
    final boolean templateOnly, final boolean ignoreFileType) {
    return getValue(directiveDef, axisID, templateOnly, ignoreFileType, false);
  }

  /**
   * Return the value of the attribute in the directive file.
   *
   * @param directiveDef
   * @param axisID
   * @return
   */
  public DirectiveValue getValue(DirectiveDef directiveDef, final AxisID axisID,
    final boolean templateOnly, final boolean ignoreFileType,
    final boolean includeOverride) {
    if (directiveDef != null) {
      directiveDef = directiveDef.getAxisIDInstance(axisID);
    }
    if (templateOnly && directiveFileType != null && directiveFileType.isBatch()) {
      return null;
    }
    AttributeMatch attribute =
      getAttribute(directiveDef, axisID, ignoreFileType, includeOverride);
    if (attribute != null) {
      return attribute;
    }
    return null;
  }

  /**
   * Return the value of the attribute in the directive file.
   *
   * @param directiveDef
   * @return
   */
  public String getValue(DirectiveDef directiveDef) {
    if (directiveDef != null) {
      directiveDef = directiveDef.getAxisIDInstance(null);
    }
    DirectiveValue directiveValue = getValue(directiveDef, null, false, false);
    if (directiveValue == null) {
      return null;
    }
    return directiveValue.getValue();
  }

  public String getValue(DirectiveDef directiveDef, final boolean templateOnly) {
    if (directiveDef != null) {
      directiveDef = directiveDef.getAxisIDInstance(null);
    }
    // template/batch issues are already being handled in getAttribute
    DirectiveValue directiveValue = getValue(directiveDef, null, templateOnly, false);
    if (directiveValue == null) {
      return null;
    }
    return directiveValue.getValue();
  }

  public DirectiveValue getValue(final DirectiveDef directiveDef,
    final boolean templateOnly, final boolean ignoreFileType) {
    // template/batch issues are already being handled in getAttribute
    return getValue(directiveDef, null, templateOnly, ignoreFileType);
  }

  public DirectiveValue getValue(final DirectiveDef directiveDef,
    final boolean templateOnly, final boolean ignoreFileType,
    final boolean includeOverride) {
    // template/batch issues are already being handled in getAttribute
    return getValue(directiveDef, null, templateOnly, ignoreFileType, includeOverride);
  }

  public String getValue(DirectiveDef directiveDef, final int index) {
    if (directiveDef != null) {
      directiveDef = directiveDef.getAxisIDInstance(null);
    }
    if (index < 0) {
      return null;
    }
    String value = getValue(directiveDef);
    // Get the element specified by index
    if (value != null) {
      String divider = ",";
      if (value.indexOf(divider) != -1) {
        String[] array = value.split(divider);
        if (array != null && index < array.length) {
          return array[index];
        }
      }
      if (index == 0) {
        return value;
      }
    }
    return null;
  }

  public boolean isValue(final DirectiveDef directiveDef, final AxisID axisID) {
    return isValue(directiveDef, axisID, false);
  }

  public boolean isValue(final DirectiveDef directiveDef, final AxisID axisID,
    final boolean templateOnly) {
    return isValue(directiveDef, axisID, templateOnly, false);
  }

  /**
   * Return the boolean value of the attribute in the directive file.
   *
   * @param directiveDef
   * @param axisID
   * @param templateOnly
   * @return
   */
  public boolean isValue(final DirectiveDef directiveDef, final AxisID axisID,
    final boolean templateOnly, final boolean ignoreFileType) {
    if (templateOnly && directiveFileType != null && directiveFileType.isBatch()) {
      return false;
    }
    AttributeMatch attribute = getAttribute(directiveDef, axisID, ignoreFileType, false);
    if (attribute != null) {
      return attribute.isValue();
    }
    return false;
  }

  public boolean isValue(final DirectiveDef directiveDef) {
    return isValue(directiveDef, null, false);
  }

  public boolean isValue(final DirectiveDef directiveDef, final boolean templateOnly) {
    return isValue(directiveDef, null, templateOnly, false);
  }

  public boolean isValue(final DirectiveDef directiveDef, final boolean templateOnly,
    final boolean ignoreFileType) {
    return isValue(directiveDef, null, templateOnly, ignoreFileType);
  }

  /**
   * Loads the file and opens the autodoc.  Resets the instance so that it will read from
   * the new autodoc.  Returns true if the autodoc was opened successfully, false if the
   * autodoc open failed.
   *
   * @return
   */
  public boolean setFile(final File directiveFile,
    final DirectiveFileType directiveFileType) {
    file = directiveFile;
    this.directiveFileType = directiveFileType;
    copyArgSet = false;
    runtimeSet = false;
    setupSetSet = false;
    comparamSet = false;
    copyArg = null;
    runtime = null;
    setupSet = null;
    comparam = null;
    try {
      autodoc =
        (ReadOnlyAutodoc) AutodocFactory.getInstance(manager, file, axisID, false);
    }
    catch (FileNotFoundException e) {
      UIHarness.INSTANCE.openMessageDialog(manager, e.getMessage(),
        "Directive File Not Found");
      return false;
    }
    catch (IOException e) {
      UIHarness.INSTANCE.openMessageDialog(manager, e.getMessage(),
        "Directive File Read Failure");
      return false;
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
      UIHarness.INSTANCE.openMessageDialog(manager, e.getMessage(),
        "Directive File Read Failure");
      return false;
    }
    return true;
  }

  private ReadOnlyAttribute getParentAttribute(final DirectiveDef directiveDef) {
    if (directiveDef == null) {
      return null;
    }
    return getParentAttribute(directiveDef.getDirectiveType());
  }

  private ReadOnlyAttribute getParentAttribute(final DirectiveType type) {
    ReadOnlyAttribute parentAttribute = null;
    if (type == DirectiveType.COPY_ARG) {
      if (!copyArgSet) {
        copyArgSet = true;
        setupSet = getParentAttribute(DirectiveType.SETUP_SET);
        if (setupSet != null && autodoc != null) {
          copyArg = setupSet.getAttribute(type.toString());
        }
      }
      parentAttribute = copyArg;
    }
    else if (type == DirectiveType.SETUP_SET) {
      if (!setupSetSet) {
        setupSetSet = true;
        if (autodoc != null) {
          setupSet = autodoc.getAttribute(type.toString());
        }
      }
      parentAttribute = setupSet;
    }
    else if (type == DirectiveType.RUN_TIME) {
      if (!runtimeSet) {
        runtimeSet = true;
        if (autodoc != null) {
          runtime = autodoc.getAttribute(type.toString());
        }
      }
      parentAttribute = runtime;
    }
    else if (type == DirectiveType.COM_PARAM) {
      if (!comparamSet) {
        comparamSet = true;
        if (autodoc != null) {
          comparam = autodoc.getAttribute(type.toString());
        }
      }
      parentAttribute = comparam;
    }
    return parentAttribute;
  }

  public void setDebug(final boolean input) {
    debug = input;
  }

  ReadOnlyAttributeIterator getCopyArgIterator() {
    if (copyArg == null) {
      return null;
    }
    ReadOnlyAttributeList list = copyArg.getChildren();
    if (list != null) {
      return list.iterator();
    }
    return null;
  }

  public File getFile() {
    return file;
  }

  public String toString() {
    return "[" + (file != null ? "file:" + file.getAbsolutePath() : super.toString())
      + ",directiveFileType:" + directiveFileType + "]";
  }

  private final class StatementIterator implements Iterator<ReadOnlyStatement> {
    private StatementLocation loc = null;
    private ReadOnlyStatement next = null;

    private StatementIterator() {}

    public boolean hasNext() {
      if (next != null) {
        return true;
      }
      next = getNext();
      return next != null;
    }

    public ReadOnlyStatement next() {
      if (next != null) {
        ReadOnlyStatement temp = next;
        next = null;
        return temp;
      }
      return getNext();
    }

    private ReadOnlyStatement getNext() {
      if (loc == null) {
        if (autodoc == null) {
          return null;
        }
        loc = autodoc.getStatementLocation();
        if (loc == null) {
          return null;
        }
      }
      ReadOnlyStatement temp = autodoc.nextStatement(loc);
      return temp;
    }

    public void remove() {}
  }

  static final class Module {
    public static final Module ALIGNED_STACK = new Module("AlignedStack");
    public static final Module BEAD_TRACKING = new Module("BeadTracking");
    public static final Module COMBINE = new Module("Combine");
    public static final Module CTF_CORRECTION = new Module("CTFcorrection");
    public static final Module CTF_PLOTTING = new Module("CTFplotting");
    public static final Module EXCLUDE_VIEWS = new Module("Excludeviews");
    public static final Module FIDUCIALS = new Module("Fiducials");
    public static final Module GOLD_ERASING = new Module("GoldErasing");
    public static final Module NAD = new Module("NAD");
    public static final Module PATCH_TRACKING = new Module("PatchTracking");
    public static final Module POSITIONING = new Module("Positioning");
    public static final Module POSTPROCESS = new Module("Postprocess");
    public static final Module PREPROCESSING = new Module("Preprocessing");
    public static final Module RAPTOR = new Module("RAPTOR");
    public static final Module RECONSTRUCTION = new Module("Reconstruction");
    public static final Module RESTRICT_ALIGN = new Module("RestrictAlign");
    public static final Module REPLACE_STEP = new Module("ReplaceStep");
    public static final Module RUN_AFTER_STEP = new Module("RunAfterStep");
    public static final Module SEED_FINDING = new Module("SeedFinding");
    public static final Module TILT_ALIGNMENT = new Module("TiltAlignment");
    public static final Module TRIMVOL = new Module("Trimvol");

    private final String tag;

    private Module(final String tag) {
      this.tag = tag;
    }

    public String toString() {
      return tag;
    }
  }

  static final class Comfile {
    static final Comfile ALIGN = new Comfile("align");
    static final Comfile AUTOFIDSEED = new Comfile("autofidseed");
    static final Comfile ERASER = new Comfile("eraser");
    static final Comfile GOLD_ERASER = new Comfile("golderaser");
    static final Comfile PREBLEND = new Comfile("preblend");
    static final Comfile PRENEWST = new Comfile("prenewst");
    static final Comfile SIRTSETUP = new Comfile("sirtsetup");
    static final Comfile TILT = new Comfile("tilt");
    static final Comfile TRACK = new Comfile("track");
    static final Comfile XCORR = new Comfile("xcorr");
    static final Comfile XCORR_PT = new Comfile("xcorr_pt");

    private final String tag;

    private Comfile(final String tag) {
      this.tag = tag;
    }

    public String toString() {
      return tag;
    }
  }

  static final class Command {
    static final Command AUTOFIDSEED = new Command("autofidseed");
    static final Command BLENDMONT = new Command("blendmont");
    static final Command BEADTRACK = new Command("beadtrack");
    static final Command CCDERASER = new Command("ccderaser");
    static final Command IMODCHOPCONTS = new Command("imodchopconts");
    static final Command NEWSTACK = new Command("newstack");
    static final Command SIRTSETUP = new Command("sirtsetup");
    static final Command TILT = new Command("tilt");
    static final Command TILTALIGN = new Command("tiltalign");
    static final Command TILTXCORR = new Command("tiltxcorr");

    private final String tag;

    private Command(final String tag) {
      this.tag = tag;
    }

    public String toString() {
      return tag;
    }
  }
}
