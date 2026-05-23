package etomo.storage;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import etomo.Arguments;
import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.comscript.ExcludeViewsParam;
import etomo.logic.DatasetTool;
import etomo.logic.UserEnv;
import etomo.logic.ValidationSet;
import etomo.storage.DirectiveAttribute.AttributeMatch;
import etomo.storage.DirectiveAttribute.Match;
import etomo.storage.autodoc.ReadOnlyAttribute;
import etomo.storage.autodoc.ReadOnlyAttributeIterator;
import etomo.storage.autodoc.ReadOnlyStatement;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.ConstEtomoNumber;
import etomo.type.DirectiveFileType;
import etomo.type.Extension;
import etomo.type.FileType;
import etomo.type.ImageFilenameStyle;
import etomo.type.TiltAngleSpec;
import etomo.type.TiltAngleType;
import etomo.type.UserConfiguration;
import etomo.type.ViewType;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.SetupReconInterface;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2012 - 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class DirectiveFileCollection
  implements SetupReconInterface, DirectiveFileInterface {
  private final DirectiveFile[] directiveFileArray =
    new DirectiveFile[] { null, null, null, null, null };
  private final ValidationSet binningValidationSet;

  /**
   * Scan header and user preference values - lowest priority
   */
  private Map<String, String> copyArgExtraValues = null;
  /**
   * command line copyarg directive values - highest priority
   */
  private Map<String, String> copyArgCommandLineValues = null;
  private boolean overrideSkipA = false;
  private boolean overrideSkipB = false;

  private final BaseManager manager;
  private final AxisID axisID;

  private boolean debug = false;

  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < directiveFileArray.length; i++) {
      String string = null;
      DirectiveFile directiveFile = directiveFileArray[i];
      if (directiveFile != null) {
        builder.append(i + ":" + directiveFile.toString() + "   ");
      }
    }
    return builder.toString();
  }

  private DirectiveFileCollection(final BaseManager manager, final AxisID axisID,
    final ValidationSet binningValidationSet, final boolean includeBatchDefaults) {
    this.manager = manager;
    this.axisID = axisID;
    this.binningValidationSet = binningValidationSet;
    if (includeBatchDefaults) {
      directiveFileArray[DirectiveFileType.BATCH_DEFAULTS.getIndex()] =
        DirectiveFile.getInstance(manager, axisID,
          FileType.DEFAULT_BATCH_RUN_TOMO_AUTODOC.getFile(manager, axisID),
          DirectiveFileType.BATCH_DEFAULTS);
    }
    // When a directive file is set, get the values of the command line options which
    // correspond to copyarg directives.
    Arguments arguments = EtomoDirector.INSTANCE.getArguments();
    if (arguments.getDirective() != null) {
      if (arguments.getAxis() == AxisType.DUAL_AXIS) {
        setCopyArgCommandLineValue(DirectiveDef.DUAL, "1");
      }
      String name = arguments.getRawImageStack();
      if (name != null && !name.matches("\\s*")) {
        setCopyArgCommandLineValue(DirectiveDef.NAME, name);
      }
      ConstEtomoNumber gold = arguments.getFiducial();
      if (gold != null && !gold.isNull()) {
        setCopyArgCommandLineValue(DirectiveDef.GOLD, gold.toString());
      }
      if (arguments.getFrame() == ViewType.MONTAGE) {
        setCopyArgCommandLineValue(DirectiveDef.MONTAGE, "1");
      }
    }
  }

  public DirectiveFileCollection(final BaseManager manager, final AxisID axisID) {
    this(manager, axisID, null, false);
  }

  public DirectiveFileCollection(final BaseManager manager, final AxisID axisID,
    final ValidationSet binningValidationSet) {
    this(manager, axisID, binningValidationSet, false);
  }

  public static DirectiveFileCollection getBatchInstance(final BaseManager manager,
    final AxisID axisID) {
    return new DirectiveFileCollection(manager, axisID, null, true);
  }

  public static DirectiveFileCollection getBatchInstance(final BaseManager manager,
    final AxisID axisID, final ValidationSet binningValidationSet) {
    return new DirectiveFileCollection(manager, axisID, binningValidationSet, true);
  }

  public Iterator<ReadOnlyStatement> iterator(final boolean templateOnly) {
    return new StatementIterator(templateOnly);
  }

  /**
   * Iterates through all the global name/value pairs in all files.  Duplicates from
   * different files are NOT excluded:  this class does not function the way the keyed
   * retrieval does.  This iterator is read-only:  name/value pairs cannot be removed from
   * a file using this class.
   */
  private final class StatementIterator implements Iterator<ReadOnlyStatement> {
    private final DirectiveFileArrayIterator fileIterator;
    final boolean templateOnly;

    private Iterator<ReadOnlyStatement> iterator = null;

    private StatementIterator(final boolean templateOnly) {
      this.templateOnly = templateOnly;
      fileIterator = new DirectiveFileArrayIterator(null, templateOnly, true);
    }

    public boolean hasNext() {
      // Find a file with something in it
      while (iterator == null || !iterator.hasNext()) {
        if (!fileIterator.hasNext()) {
          return false;
        }
        else {
          iterator = fileIterator.next().iterator(templateOnly);
        }
      }
      return true;
    }

    public ReadOnlyStatement next() {
      if (hasNext()) {
        return iterator.next();
      }
      return null;
    }

    public void remove() {}
  }

  public boolean getParameters(final ExcludeViewsParam param, final AxisID axisID,
    final boolean dualAxis, final boolean doValidation) {
    String name = getValue(DirectiveDef.NAME, axisID);
    if (name != null) {
      String stackFileName = DatasetTool.getStackFileName(
        getValue(DirectiveDef.DATASET_DIRECTORY, axisID), name, axisID, dualAxis);
      param.setStackName(stackFileName);
    }
    param.setMontagedImages(isValue(DirectiveDef.MONTAGE, axisID));
    param.setDeleteOldFiles(isValue(DirectiveDef.DELETE_OLD_FILES));
    return true;
  }

  public String getDatasetName(final AxisID axisID) {
    return getValue(DirectiveDef.NAME, axisID);
  }

  public void setDebug(final boolean input) {
    debug = input;
  }

  // Bug# 2322 done
  public String getSkip(final AxisID axisID, final boolean doValidation) {
    return getValue(DirectiveDef.SKIP, axisID);
  }

  /**
   * Iterates through the directive files from high to low.  If hasNext is true, next will
   * not return null.
   */
  private final class DirectiveFileArrayIterator implements Iterator<DirectiveFile> {
    private final boolean templateOnly;
    private final boolean ignoreFileType;
    private final int start;
    private final int end;

    private int next = -1;

    /**
     * @param directiveDef - to set template and batch if ignoreFileType is false, may be null
     * @param templateOnly - batch file is not retrieved
     * @param ignoreFileType - directiveDef is ignored
     */
    private DirectiveFileArrayIterator(DirectiveDef directiveDef,
      final boolean templateOnly, final boolean ignoreFileType) {
      if (directiveDef != null) {
        directiveDef = directiveDef.getAxisIDInstance(axisID);
      }
      this.templateOnly = templateOnly;
      this.ignoreFileType = ignoreFileType;
      //
      // Get directive permission (permission to be in a specific type of directive file).
      // If ignorePermissions is on, the directive can be in either type of file.
      boolean template = true;
      boolean batch = true;
      if (!ignoreFileType && directiveDef != null) {
        template = directiveDef.isTemplate(axisID);
        batch = directiveDef.isBatch(axisID);
      }
      //
      // Set start and end
      if (templateOnly && batch && !template) {
        // Retrieve nothing. Its asking for templates only but the directive permission is
        // batch only.
        start = -1;
        end = -1;
      }
      else if (templateOnly || (!batch && template)) {
        // Retrieve templates.
        start = directiveFileArray.length - 2;
        end = 0;
      }
      else {
        start = directiveFileArray.length - 1;
        if (batch && !template) {
          // Retrieve batch
          end = start;
        }
        else {
          // Retrieve everything
          end = 0;
        }
      }
      next = start;
    }

    private Iterator<DirectiveFile> iterator(final DirectiveDef directiveDef,
      final boolean templateOnly, final boolean ignorePermissions) {
      return new DirectiveFileArrayIterator(directiveDef, templateOnly,
        ignorePermissions);
    }

    private void restart() {
      next = start;
    }

    /**
     * Returns true if the next member variable is the index of a non-null
     * directiveFileArray element.  If it is not will attempt to make this happen by
     * decrementing next.
     * 
     */
    public boolean hasNext() {
      if (start == -1 || end == -1 || next < 0 || next >= directiveFileArray.length) {
        return false;
      }
      if (directiveFileArray[next] != null) {
        return true;
      }
      for (int i = next; i >= end; i--) {
        if (directiveFileArray[i] != null) {
          next = i;
          return true;
        }
      }
      return false;
    }

    /**
     * If hasNext is true, returns what the next member variable is pointing to and
     * decrements next.  Returns null if hasNExt is false.
     */
    public DirectiveFile next() {
      if (hasNext()) {
        int index = next;
        next--;
        return directiveFileArray[index];
      }
      return null;
    }

    public void remove() {}
  }

  /**
   * Get the attribute matching directiveDef.  If the primary match is not found, use the
   * secondary match.  The secondary match less closely matches the axisID.
   * @param iterator
   * @param directiveDef
   * @param axisID
   * @return
   */
  private AttributeMatch getAttribute(final DirectiveFileArrayIterator iterator,
    DirectiveDef directiveDef, final AxisID axisID, final boolean ignoreFileType) {
    if (directiveDef != null) {
      directiveDef = directiveDef.getAxisIDInstance(axisID);
    }
    iterator.restart();
    AttributeMatch secondaryMatch = null;
    // Search for a primary match starting with the highest priority file
    while (iterator.hasNext()) {
      DirectiveFile directiveFile = iterator.next();
      if (directiveFile == null) {
        continue;
      }
      AttributeMatch primaryMatch =
        directiveFile.getAttribute(Match.PRIMARY, directiveDef, axisID, ignoreFileType);
      if (primaryMatch != null && !primaryMatch.isEmpty()) {
        return primaryMatch;
      }
      // Get the secondary match in case no primary match is found. Lower priority files
      // with a primary match outweigh higher priority files with a secondary match.
      if (secondaryMatch == null || secondaryMatch.isEmpty()) {
        secondaryMatch = directiveFile.getAttribute(Match.SECONDARY, directiveDef, axisID,
          ignoreFileType);
      }
    }
    // A primary match was not found. Use the secondary match if it was found.
    if (secondaryMatch != null && !secondaryMatch.isEmpty()) {
      return secondaryMatch;
    }
    return null;
  }

  AttributeMatch getAttribute(final DirectiveDef directiveDef, final AxisID axisID,
    final boolean templateOnly, final boolean ignoreFileType) {
    return getAttribute(directiveDef, axisID, templateOnly, ignoreFileType, false);
  }

  /**
   * Returns a non-empty attributeMatch or null.  Directive files are checked in order of
   * priority (batch to scope) and the highest priority file containing the directive, is
   * used.  An overridden directive (no value and non-boolean) causes a null to be
   * returned.  Two different matches are used - first the primary match, and then the
   * secondary match.  The difference between them is how closely they match the axisID.
   * The primary match has priority over the secondary match, even when the primary match
   * is found in a lower priority file.  If the match is in override, return null.
   *
   * @param directiveDef
   * @param axisID
   * @param templateOnly - causes function to ignore the batch directive file
   * @return
   */
  AttributeMatch getAttribute(DirectiveDef directiveDef, final AxisID axisID,
    final boolean templateOnly, final boolean ignoreFileType,
    final boolean includeOverride) {
    if (directiveDef == null) {
      return null;
    }
    directiveDef = directiveDef.getAxisIDInstance(axisID);
    DirectiveFileArrayIterator iterator =
      new DirectiveFileArrayIterator(directiveDef, templateOnly, ignoreFileType);
    AttributeMatch attributeMatch =
      getAttribute(iterator, directiveDef, axisID, ignoreFileType);
    if (attributeMatch == null) {
      return null;
    }
    if (attributeMatch.isOverride() && !includeOverride) {
      return null;
    }
    if (!attributeMatch.isEmpty()) {
      return attributeMatch;
    }
    return null;
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
   * Returns true if a non-empty, non-overriding directive, or an extraValue is found.
   *
   * @param directiveDef
   * @param axisID
   * @param templateOnly - if true, batch directive file, copyArgExtraValues, and copyArgCommandLineValues are ignored
   * @return
   */
  public boolean contains(DirectiveDef directiveDef, final AxisID axisID,
    final boolean templateOnly, final boolean ignoreFileType,
    final boolean includeOverride) {
    if (directiveDef == null) {
      return false;
    }
    directiveDef = directiveDef.getAxisIDInstance(axisID);
    // Before looking in the directive files look in copyArgCommandLineValues.
    if (directiveDef.getDirectiveType() == DirectiveType.COPY_ARG
      && copyArgCommandLineValues != null
      && copyArgCommandLineValues.containsKey(directiveDef.getName(axisID))) {
      return true;
    }
    AttributeMatch attributeMatch =
      getAttribute(directiveDef, axisID, templateOnly, ignoreFileType, includeOverride);
    if (attributeMatch != null) {
      return true;
    }
    if (templateOnly) {
      return false;
    }
    // An attribute has not been found - look for an extra value - these are associated
    // with the directive file.
    if (directiveDef.getDirectiveType() == DirectiveType.COPY_ARG
      && copyArgExtraValues != null) {
      return copyArgExtraValues.containsKey(directiveDef.getName(axisID));
    }
    return false;
  }

  /**
   * Returns true if the directive is found.  True whether or not directive overrides.
   * @param directiveDef
   * @param templateOnly
   * @return
   */
  public boolean containsDirective(DirectiveDef directiveDef,
    final boolean templateOnly) {
    if (directiveDef == null) {
      return false;
    }
    directiveDef = directiveDef.getAxisIDInstance(axisID);
    DirectiveFileArrayIterator iterator =
      new DirectiveFileArrayIterator(directiveDef, templateOnly, false);
    AttributeMatch attributeMatch = getAttribute(iterator, directiveDef, axisID, false);
    return attributeMatch != null;
  }

  public boolean contains(final DirectiveDef directiveDef, final AxisID axisID) {
    return contains(directiveDef, axisID, false);
  }

  /**
   * Returns true a non-empty, non-overriding directive, or an extraValue is found.
   *
   * @param directiveDef
   * @return
   */
  public boolean contains(final DirectiveDef directiveDef) {
    return contains(directiveDef, null, false);
  }

  public boolean contains(final DirectiveDef directiveDef, final boolean templateOnly) {
    return contains(directiveDef, null, templateOnly);
  }

  public boolean contains(final DirectiveDef directiveDef, final boolean templateOnly,
    final boolean ignoreFileType) {
    return contains(directiveDef, null, templateOnly, ignoreFileType);
  }

  public boolean contains(final DirectiveDef directiveDef, final boolean templateOnly,
    final boolean ignoreFileType, final boolean includeOverride) {
    return contains(directiveDef, null, templateOnly, ignoreFileType, includeOverride);
  }

  public DirectiveValue getValue(final DirectiveDef directiveDef, final AxisID axisID,
    final boolean templateOnly, final boolean ignoreFileType) {
    return getValue(directiveDef, axisID, templateOnly, ignoreFileType, false);
  }

  /**
   * Returns the directive value if a non-empty, non-overriding directive,
   * or an extraValue
   * is found.
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
    // Look for a command line value before checking the directive files.
    if (directiveDef.getDirectiveType() == DirectiveType.COPY_ARG
      && copyArgCommandLineValues != null
      && copyArgCommandLineValues.containsKey(directiveDef.getName(axisID))) {
      return new CopyArgValue(copyArgCommandLineValues.get(directiveDef.getName(axisID)));
    }
    AttributeMatch attributeMatch =
      getAttribute(directiveDef, axisID, templateOnly, ignoreFileType, includeOverride);
    if (attributeMatch != null) {
      return attributeMatch;
    }
    if (templateOnly) {
      return null;
    }
    // An attribute has not been found - look for an extra value.
    if (directiveDef.getDirectiveType() == DirectiveType.COPY_ARG
      && copyArgExtraValues != null) {
      return new CopyArgValue(copyArgExtraValues.get(directiveDef.getName(axisID)));
    }
    return null;
  }

  private static final class CopyArgValue implements DirectiveValue {
    private final String value;

    private CopyArgValue(final String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public boolean isBatch() {
      return true;
    }

    public boolean isOverride() {
      return value == null;
    }
  }

  // TODO Bug# 2322 - need to get the paired directive that matches axisID?
  public String getValue(final DirectiveDef directiveDef, final AxisID axisID) {
    DirectiveValue directiveValue = getValue(directiveDef, axisID, false, false);
    if (directiveValue == null) {
      return null;
    }
    return directiveValue.getValue();
  }

  /**
   * Returns the directive value if a non-empty, non-overriding directive,
   * or an extraValue
   * is found.
   *
   * @param directiveDef
   * @return
   */
  public String getValue(final DirectiveDef directiveDef) {
    DirectiveValue directiveValue = getValue(directiveDef, null, false, false);
    if (directiveValue == null) {
      return null;
    }
    return directiveValue.getValue();
  }

  public String getValue(final DirectiveDef directiveDef, final boolean templateOnly) {
    DirectiveValue directiveValue = getValue(directiveDef, null, templateOnly, false);
    if (directiveValue == null) {
      return null;
    }
    return directiveValue.getValue();
  }

  public DirectiveValue getValue(final DirectiveDef directiveDef,
    final boolean templateOnly, final boolean ignoreFileType) {
    return getValue(directiveDef, null, templateOnly, ignoreFileType);
  }

  // TODO 2342
  public DirectiveValue getValue(final DirectiveDef directiveDef,
    final boolean templateOnly, final boolean ignoreFileType,
    final boolean includeOverride) {
    return getValue(directiveDef, null, templateOnly, ignoreFileType, includeOverride);
  }

  /**
   * Return the specified element of the directive value if a non-empty, non-overriding
   * directive, or an extraValue is found.
   *
   * @param directiveDef
   * @param index        - index of element in value to return
   * @return
   */
  public String getValue(DirectiveDef directiveDef, final int index) {
    if (directiveDef != null) {
      directiveDef = directiveDef.getAxisIDInstance(axisID);
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

  public boolean isValue(final DirectiveDef directiveDef, final AxisID axisID,
    final boolean templateOnly) {
    return isValue(directiveDef, axisID, templateOnly, false);
  }

  /**
   * Returns the boolean version of a directive value if a non-empty, non-overriding
   * directive, or an extraValue is found.
   *
   * @param directiveDef
   * @param axisID
   * @param templateOnly - causes function to ignore the batch directive file and
   *                     copyrgExtraValues
   * @return
   */
  public boolean isValue(DirectiveDef directiveDef, final AxisID axisID,
    final boolean templateOnly, final boolean ignoreFileType) {
    if (directiveDef != null) {
      directiveDef = directiveDef.getAxisIDInstance(axisID);
    }
    // Check the command line before checking the file
    if (directiveDef.getDirectiveType() == DirectiveType.COPY_ARG
      && copyArgCommandLineValues != null
      && copyArgCommandLineValues.containsKey(directiveDef.getName(axisID))) {
      return DirectiveAttribute
        .toBoolean(copyArgCommandLineValues.get(directiveDef.getName(axisID)));
    }
    AttributeMatch attributeMatch =
      getAttribute(directiveDef, axisID, templateOnly, false);
    if (attributeMatch != null) {
      return attributeMatch.isValue();
    }
    if (templateOnly) {
      return false;
    }
    // An attribute has not been found - look for scan header values.
    if (directiveDef.getDirectiveType() == DirectiveType.COPY_ARG
      && copyArgExtraValues != null) {
      return DirectiveAttribute
        .toBoolean(copyArgExtraValues.get(directiveDef.getName(axisID)));
    }
    return false;
  }

  public boolean isValue(final DirectiveDef directiveDef, final AxisID axisID) {
    return isValue(directiveDef, axisID, false);
  }

  /**
   * Returns the boolean version of a directive value if a non-empty, non-overriding
   * directive, or an extraValue is found.
   *
   * @param directiveDef
   * @return
   */
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
   * @param doValidation has no effect.
   * @return true
   */
  public boolean getTiltAngleFields(final AxisID axisID,
    final TiltAngleSpec tiltAngleSpec, final boolean doValidation) {
    if (tiltAngleSpec == null) {
      return true;
    }
    DirectiveDef firstIncDirectiveDef = DirectiveDef.FIRST_INC.getAxisIDInstance(axisID);
    if (contains(firstIncDirectiveDef, axisID)) {
      tiltAngleSpec.setType(TiltAngleType.RANGE);
      String value = getValue(firstIncDirectiveDef, axisID);
      String[] arrayValue = null;
      if (value != null) {
        arrayValue = value.trim().split(FieldType.CollectionType.ARRAY.getSplitter());
      }
      if (arrayValue != null && arrayValue.length > 0) {
        tiltAngleSpec.setRangeMin(arrayValue[0]);
      }
      if (arrayValue != null && arrayValue.length > 1) {
        tiltAngleSpec.setRangeStep(arrayValue[1]);
      }
    }
    else if (isValue(DirectiveDef.EXTRACT.getAxisIDInstance(axisID), axisID)) {
      tiltAngleSpec.setType(TiltAngleType.EXTRACT);
    }
    else if (isValue(DirectiveDef.USE_RAW_TLT.getAxisIDInstance(axisID), axisID)) {
      tiltAngleSpec.setType(TiltAngleType.FILE);
    }
    else {
      // Must set something here, so use the settings values
      UserConfiguration userConfiguration = EtomoDirector.INSTANCE.getUserConfiguration();
      if (userConfiguration.isTiltAnglesRawtltFile()) {
        tiltAngleSpec.setType(TiltAngleType.FILE);
      }
      else {
        // Default
        tiltAngleSpec.setType(TiltAngleType.EXTRACT);
      }
    }
    return true;
  }

  public void msgExcludeViewsSucceeded(final AxisID axisID, final boolean processRunning,
    final boolean processDone) {
    if (axisID == AxisID.SECOND) {
      overrideSkipB = true;
    }
    else if (axisID == null) {
      overrideSkipA = true;
      overrideSkipB = true;
    }
    else {
      overrideSkipA = true;
    }
  }

  /**
   * Puts the DirectiveDef/value pair into commandLineValues.
   *
   * @param directiveDef
   * @param axisID
   * @param value
   */
  private void setCopyArgCommandLineValue(DirectiveDef directiveDef, final String value) {
    if (directiveDef == null) {
      return;
    }
    directiveDef = directiveDef.getAxisIDInstance(axisID);
    if (copyArgCommandLineValues == null) {
      copyArgCommandLineValues = new HashMap<String, String>();
    }
    copyArgCommandLineValues.put(directiveDef.getName(null), value);
  }

  /**
   * Puts the DirectiveDef/value pair into extraValues.
   *
   * @param directiveDef
   * @param axisID
   * @param value
   */
  private void setCopyArgExtraValue(DirectiveDef directiveDef, final AxisID axisID,
    final String value) {
    if (directiveDef == null) {
      return;
    }
    directiveDef = directiveDef.getAxisIDInstance(axisID);
    if (copyArgExtraValues == null) {
      copyArgExtraValues = new HashMap<String, String>();
    }
    copyArgExtraValues.put(directiveDef.getName(axisID), value);
  }

  public void setBinning(final String input) {
    setCopyArgExtraValue(DirectiveDef.BINNING, null, input);
  }

  public void setImageRotation(final String input) {
    setCopyArgExtraValue(DirectiveDef.ROTATION, AxisID.FIRST, input);
    // not valid for B if there is a brotation directive in this collection.
    // if (isValue(DirectiveDef.DUAL)) {
    // setCopyArgExtraValue(DirectiveDef.ROTATION, AxisID.SECOND, input);
    // }
  }

  public void setPixelSize(final double input) {
    setCopyArgExtraValue(DirectiveDef.PIXEL, null, String.valueOf(input));
  }

  public void setTwodir(final AxisID axisID, final double input) {
    setCopyArgExtraValue(DirectiveDef.TWODIR, axisID, String.valueOf(input));
  }

  /**
   * Add tilt angle directives to copyArgExtraValues.  Behaves as an init function by only
   * adding directives that are not in the collection.  Can be called at any time without
   * overriding other settings.
   * @param axisID
   * @param tiltAngleSpec
   * @param userConfiguration
   */
  public void initTiltAngleFields(final AxisID axisID, final TiltAngleSpec tiltAngleSpec,
    final UserConfiguration userConfiguration) {
    if (copyArgExtraValues == null) {
      copyArgExtraValues = new HashMap<String, String>();
    }
    if (tiltAngleSpec.getType() == TiltAngleType.FILE
      || userConfiguration.isTiltAnglesRawtltFile()) {
      if (!copyArgExtraValues.containsKey(
        DirectiveDef.USE_RAW_TLT.getAxisIDInstance(axisID).getName(axisID))) {
        setCopyArgExtraValue(DirectiveDef.USE_RAW_TLT, axisID, DirectiveDef.TRUE_VALUE);
      }
    }
    else if (tiltAngleSpec.getType() == TiltAngleType.EXTRACT) {
      if (!copyArgExtraValues
        .containsKey(DirectiveDef.EXTRACT.getAxisIDInstance(axisID).getName(axisID))) {
        setCopyArgExtraValue(DirectiveDef.EXTRACT, axisID, DirectiveDef.TRUE_VALUE);
      }
    }
    else if (tiltAngleSpec.getType() == TiltAngleType.RANGE) {
      DirectiveDef firstIncDirectiveDef =
        DirectiveDef.FIRST_INC.getAxisIDInstance(axisID);
      if (!copyArgExtraValues.containsKey(firstIncDirectiveDef.getName(axisID))) {
        setCopyArgExtraValue(firstIncDirectiveDef, axisID,
          String.valueOf(tiltAngleSpec.getRangeMin()) + ", "
            + String.valueOf(tiltAngleSpec.getRangeStep()));
      }
    }
  }

  public boolean containsTiltAngleSpec(final AxisID axisID) {
    return contains(DirectiveDef.FIRST_INC.getAxisIDInstance(axisID), axisID)
      || contains(DirectiveDef.USE_RAW_TLT.getAxisIDInstance(axisID), axisID)
      || contains(DirectiveDef.EXTRACT.getAxisIDInstance(axisID), axisID);
  }

  public String getBackupDirectory() {
    return null;
  }

  public String getBinning() {
    try {
      return getBinning(false);
    }
    catch (FieldValidationFailedException e) {
      return null;
    }
  }

  public String getBinning(final boolean doValidation)
    throws FieldValidationFailedException {
    String value = getValue(DirectiveDef.BINNING);
    if (doValidation) {
      String errmsg = binningValidationSet.validate(value);
      if (errmsg != null) {
        errmsg = "Error: binning directive value is invalid:  " + errmsg;
        System.err.println(errmsg);
        throw new FieldValidationFailedException(errmsg);
      }
    }
    return value;
  }

  /**
   * @deprecated 4/8/2019
   * No need to get the dataset name. It can be derived from the file name.
   */
  @SuppressWarnings("deprecation")
  public String getDataset() {
    return getRawImageStack();
  }

  /**
   * @return the raw image stack file name recreated from the directives NAME and STACK_EXT.
   */
  public String getRawImageStack() {
    String axisString = "";
    if (isDualAxisSelected()) {
      axisString = AxisID.FIRST.getExtension();
    }
    String name = getValue(DirectiveDef.NAME);
    String stackExt = getValue(DirectiveDef.STACK_EXT);
    if ((name == null || name.matches("\\s*"))
      && (stackExt == null || stackExt.matches("\\s*"))) {
      return null;
    }
    if (name == null) {
      name = "";
    }
    if (stackExt != null && stackExt.matches("\\s*")) {
      stackExt = null;
    }
    return name + axisString + Extension.EXTENSION_DIVIDER + (stackExt != null ? stackExt
      : ImageFilenameStyle.DEFAULT.getDefaultRawImageStackExtension().toString());
  }

  public DirectiveFile getDirectiveFile(final DirectiveFileType type) {
    return directiveFileArray[type.getIndex()];
  }

  public DirectiveFileCollection getDirectiveFileCollection() {
    return this;
  }

  public String getDistortionFile() {
    return getValue(DirectiveDef.DISTORT);
  }

  public String getCurrentStackExt() {
    String currentStackExt = getValue(DirectiveDef.CURRENT_STACK_EXT);
    if (currentStackExt != null) {
      return currentStackExt;
    }
    // Use currentBStackExt if currentStackExt wasn't set.
    return getValue(DirectiveDef.CURRENT_B_STACK_EXT);
  }

  /**
   * @param doValidation has no effect
   */
  public String getExcludeList(final AxisID axisID, final boolean doValidation) {
    return getValue(DirectiveDef.SKIP, axisID);
  }

  /**
   * @param doValidation has no effect
   */
  public String getTwodir(final AxisID axisID, final boolean doValidation) {
    return getValue(DirectiveDef.TWODIR, axisID);
  }

  public boolean isTwodir(final AxisID axisID) {
    // Contains returns true if a directive contains this attribute, and it is set to a
    // value. Since twodir is not a boolean, no value would mean that it is overridden -
    // which causes contains() to return false.
    return contains(DirectiveDef.TWODIR, axisID);
  }

  /**
   * @param doValidation has no effect
   */
  public String getFiducialDiameter(final boolean doValidation) {
    return getValue(DirectiveDef.GOLD);
  }

  /**
   * Return rotation or brotation.  For the B axis use rotation if brotation is not set.
   * Rotation is required.
   * @param doValidation has no effect
   */
  public String getImageRotation(final AxisID axisID, final boolean doValidation) {
    String value = getValue(DirectiveDef.ROTATION, axisID);
    // This function should never return null if any rotation exists. Use A rotation when
    // B rotation is missing.
    if (axisID == AxisID.SECOND && value == null) {
      return getValue(DirectiveDef.ROTATION, AxisID.FIRST);
    }
    return value;
  }

  public String getMagGradientFile() {
    return getValue(DirectiveDef.GRADIENT);
  }

  public String getPixelSize(final boolean doValidation) {
    return getValue(DirectiveDef.PIXEL);
  }

  public String getStackExt() {
    return getValue(DirectiveDef.STACK_EXT);
  }

  public boolean isAdjustedFocusSelected(final AxisID axisID) {
    return isValue(DirectiveDef.FOCUS, axisID);
  }

  public boolean isDualAxisSelected() {
    return isValue(DirectiveDef.DUAL);
  }

  public boolean isGpuProcessingSelected(final String propertyUserDir) {
    return UserEnv.isGpuProcessing(manager, axisID, propertyUserDir);
  }

  public boolean isParallelProcessSelected(final String propertyUserDir) {
    return UserEnv.isParallelProcessing(manager, axisID, propertyUserDir);
  }

  public boolean isSingleAxisSelected() {
    return !isValue(DirectiveDef.DUAL);
  }

  public boolean isSingleViewSelected() {
    return !isValue(DirectiveDef.MONTAGE);
  }

  /**
   * Sets the batch directive file, and then sets the template files from the the batch
   * directive file.
   *
   * @param batchDirectiveFile
   */
  public void setup(final DirectiveFile batchDirectiveFile) {
    directiveFileArray[DirectiveFileType.BATCH.getIndex()] = batchDirectiveFile;
    if (batchDirectiveFile != null) {
      setDirectiveFile(
        batchDirectiveFile.getAttribute(DirectiveDef.SCOPE_TEMPLATE, null, false, false),
        DirectiveFileType.SCOPE);
      setDirectiveFile(
        batchDirectiveFile.getAttribute(DirectiveDef.SYSTEM_TEMPLATE, null, false, false),
        DirectiveFileType.SYSTEM);
      setDirectiveFile(
        batchDirectiveFile.getAttribute(DirectiveDef.USER_TEMPLATE, null, false, false),
        DirectiveFileType.USER);
    }
  }

  /**
   * Sets a directive file.  No directive file other then the one being set is changed by
   * this function.
   *
   * @param attribute
   * @param type
   */
  private void setDirectiveFile(final AttributeMatch attribute,
    final DirectiveFileType type) {
    if (attribute == null) {
      directiveFileArray[type.getIndex()] = null;
    }
    else {
      String absPath = attribute.getValue();
      if (absPath == null) {
        directiveFileArray[type.getIndex()] = null;
      }
      else {
        directiveFileArray[type.getIndex()] =
          DirectiveFile.getInstance(manager, axisID, new File(absPath), type);
      }
    }
  }

  /**
   * Sets a directive file.  No directive file other then the one being set is changed by
   * this function.
   *
   * @param file
   * @param type
   */
  public void setDirectiveFile(final File file, final DirectiveFileType type) {
    if (file == null) {
      directiveFileArray[type.getIndex()] = null;
    }
    else {
      directiveFileArray[type.getIndex()] =
        DirectiveFile.getInstance(manager, axisID, file, type);
    }
  }

  public boolean validateTiltAngle(final AxisID axisID, final String errorTitle) {
    TiltAngleSpec tiltAngleSpec = new TiltAngleSpec();
    getTiltAngleFields(axisID, tiltAngleSpec, false);
    return DatasetTool.validateTiltAngle(manager, AxisID.ONLY, errorTitle, axisID,
      tiltAngleSpec.getType() == TiltAngleType.RANGE,
      String.valueOf(tiltAngleSpec.getRangeMin()),
      String.valueOf(tiltAngleSpec.getRangeStep()));
  }

  public Set<Entry<String, String>> getCopyArgCommandLineSet() {
    if (copyArgCommandLineValues == null) {
      return null;
    }
    return copyArgCommandLineValues.entrySet();
  }

  /**
   * This function will not return null.
   */
  public CopyArgEntrySet getCopyArgEntrySet() {
    return CopyArgEntrySet.getInstance(this);
  }

  public static final class CopyArgEntrySet {
    private final Map<String, String> pairMap = new HashMap<String, String>();

    private final DirectiveFileCollection directiveFileCollection;

    /**
     * Don't call constructor directly.
     */
    private CopyArgEntrySet(final DirectiveFileCollection directiveFileCollection) {
      this.directiveFileCollection = directiveFileCollection;
    }

    /**
     * Returns true if pairMap contains name.  Tilt angle spec directives exclude
     * each other.
     * @param name
     */
    private boolean contains(final String name) {
      if (pairMap.containsKey(name)) {
        return true;
      }
      AxisID axisID = AxisID.FIRST;
      if (isTiltAngleSpec(name, axisID)) {
        return containsTiltAngleSpec(axisID);
      }
      axisID = AxisID.SECOND;
      if (isTiltAngleSpec(name, axisID)) {
        return containsTiltAngleSpec(axisID);
      }
      return false;
    }

    private boolean isTiltAngleSpec(final String name, final AxisID pairAxisID) {
      return DirectiveDef.USE_RAW_TLT.getAxisIDInstance(pairAxisID).equalsName(name,
        pairAxisID)
        || DirectiveDef.EXTRACT.getAxisIDInstance(pairAxisID).equalsName(name, pairAxisID)
        || DirectiveDef.FIRST_INC.getAxisIDInstance(pairAxisID).equalsName(name,
          pairAxisID);
    }

    /**
     * @param axisID - the axis of the the tilt angle spec directives
     * @return true if pairMap contains any of the tilt angle spec directives
     */
    private boolean containsTiltAngleSpec(final AxisID axisID) {
      return pairMap
        .containsKey(DirectiveDef.USE_RAW_TLT.getAxisIDInstance(axisID).getName(axisID))
        || pairMap
          .containsKey(DirectiveDef.EXTRACT.getAxisIDInstance(axisID).getName(axisID))
        || pairMap
          .containsKey(DirectiveDef.FIRST_INC.getAxisIDInstance(axisID).getName(axisID));

    }

    /**
     * This function should not return null.
     *
     * @return initialized instance
     */
    private static CopyArgEntrySet
      getInstance(final DirectiveFileCollection directiveFileCollection) {
      CopyArgEntrySet instance = new CopyArgEntrySet(directiveFileCollection);
      instance.init(directiveFileCollection.directiveFileArray);
      return instance;
    }

    /**
     * Load all of the directive file copyarg values into pairMap.  Pairs with the same
     * name as a previously saved pair overrides the previous pair.  A pair with a blank
     * value is not saved and causes the pair with the same name in the map to be removed.
     * After loading all of copyarg values, load the scan header output from the directive
     * file if scan header is in the map and is set to "1".  Only load pairs with names
     * that are not already in the map, because the directive files all override scan
     * header.
     *
     * @param directiveFileArray
     */
    private void init(final DirectiveFile[] directiveFileArray) {
      for (int i = 0; i < directiveFileArray.length; i++) {
        if (directiveFileArray[i] != null) {
          ReadOnlyAttributeIterator iterator = directiveFileArray[i].getCopyArgIterator();
          if (iterator == null) {
            continue;
          }
          while (iterator.hasNext()) {
            ReadOnlyAttribute attribute = iterator.next();
            String name = attribute.getName();
            String value = attribute.getValue();
            pairMap.remove(name);
            // A blank value means remove the previously added pair, otherwise override
            // the previous added pair with the new value.
            if (value != null) {
              pairMap.put(name, value);
            }
          }
        }
      }
      // Get values from the .etomo file and scanning the header which aren't already in
      // the map.
      if (directiveFileCollection.copyArgExtraValues != null) {
        Iterator<Entry<String, String>> iterator =
          directiveFileCollection.copyArgExtraValues.entrySet().iterator();
        while (iterator.hasNext()) {
          Entry<String, String> entry = iterator.next();
          String name = entry.getKey();
          if (!contains(name)) {
            pairMap.put(name, entry.getValue());
          }
        }
      }
      if (directiveFileCollection.overrideSkipA) {
        String key = DirectiveDef.SKIP.getName(AxisID.FIRST);
        if (pairMap.containsKey(key)) {
          pairMap.remove(key);
        }
      }
      if (directiveFileCollection.overrideSkipB) {
        String key = DirectiveDef.BSKIP.getName(AxisID.SECOND);
        if (pairMap.containsKey(key)) {
          pairMap.remove(key);
        }
      }
    }

    public Iterator<Entry<String, String>> iterator() {
      return pairMap.entrySet().iterator();
    }
  }
}
