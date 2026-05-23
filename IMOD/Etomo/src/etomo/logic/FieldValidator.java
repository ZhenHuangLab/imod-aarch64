package etomo.logic;

import java.io.File;

import etomo.EtomoDirector;
import etomo.type.EtomoNumber;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldType;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.UIComponent;
import etomo.ui.swing.UIHarness;
import etomo.ui.swing.ValidationExtension;
import etomo.util.Utilities;

/**
* <p>Description: Validator for numeric text fields.</p>
* 
 * <p>Copyright: Copyright 2012 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
*/
public final class FieldValidator {
  private static final String TITLE = "Field Validation Failed";

  private static boolean DEBUG = EtomoDirector.INSTANCE.getArguments().isDebug();

  public static void setDebug(final boolean debug) {
    DEBUG = debug;
  }

  public static void resetDebug() {
    DEBUG = EtomoDirector.INSTANCE.getArguments().isDebug();
  }

  @Deprecated // 9/18/18 - not standard
  public static void fail(final String fieldText, final UIComponent component,
    final String label, final String locationDescr, final FieldDisplayer fieldDisplayer)
    throws FieldValidationFailedException {
    String descr = label + (locationDescr == null ? "" : " in " + locationDescr);
    fail("illegal entry " + fieldText, fieldText, null, null, component, descr,
      fieldDisplayer, null, true, false);
  }

  public static boolean isTextValid(final String fieldText, final FieldType fieldType,
    final UIComponent component, final ValidationExtension validationExtension,
    final boolean debug) {
    try {
      boolean required = false;
      if (validationExtension != null) {
        required = validationExtension.isRequired();
      }
      validateText(fieldText, fieldType, component, null, required, false, false, null,
        validationExtension, null, null, false, debug);
      return true;
    }
    catch (FieldValidationFailedException e) {
      return false;
    }
  }

  /**
   * 
   * @param fieldText
   * @param fieldType
   * @param component
   * @param descr
   * @param required
   * @param fileOnly - causes FieldType.File validation when true
   * @param fileMustExist - causes FieldType.File validation when true
   * @param validationSet
   * @param fieldDisplayer - makes sure that the current field is visible
   * @return
   * @throws FieldValidationFailedException
   */
  public static String validateText(final String fieldText, final FieldType fieldType,
    final UIComponent component, final String descr, final boolean required,
    final boolean fileMustExist, final boolean fileOnly,
    final ValidationSet validationSet, final FieldDisplayer fieldDisplayer)
    throws FieldValidationFailedException {
    return validateText(fieldText, fieldType, component, descr, required, fileMustExist,
      fileOnly, validationSet, null, fieldDisplayer, null, true, false);
  }

  public static String validateText(final String fieldText, final FieldType fieldType,
    final UIComponent component, final String descr, final boolean required,
    final boolean fileMustExist, final boolean fileOnly,
    final ValidationSet validationSet, final FieldDisplayer fieldDisplayer1,
    final FieldDisplayer fieldDisplayer2) throws FieldValidationFailedException {
    return validateText(fieldText, fieldType, component, descr, required, fileMustExist,
      fileOnly, validationSet, null, fieldDisplayer1, fieldDisplayer2, true, false);
  }

  public static String validateText(final String fieldText, final FieldType fieldType,
    final UIComponent component, final String descr,
    final ValidationExtension validationExtension, final FieldDisplayer fieldDisplayer1,
    final FieldDisplayer fieldDisplayer2, final boolean debug)
    throws FieldValidationFailedException {
    boolean required = false;
    if (validationExtension != null) {
      required = validationExtension.isRequired();
    }
    return validateText(fieldText, fieldType, component, descr, required, false, false,
      null, validationExtension, fieldDisplayer1, fieldDisplayer2, true, debug);
  }

  private static void fail(String errMsg, final String fieldText,
    final FieldType fieldType, final String validationType, final UIComponent component,
    final String descr, final FieldDisplayer fieldDisplayer1,
    final FieldDisplayer fieldDisplayer2, final boolean popupMessage, final boolean debug)
    throws FieldValidationFailedException {
    if (popupMessage) {
      UIHarness.INSTANCE.openMessageDialog(null, component,
        "Validation falure in " + descr + ": " + errMsg + ".", TITLE, fieldDisplayer1,
        fieldDisplayer2);
    }
    FieldValidationFailedException exception =
      new FieldValidationFailedException(errMsg + ",descr:" + descr + ",fieldText:"
        + fieldText + (fieldType != null ? (",fieldType:" + fieldType) : "")
        + (validationType != null ? (",validationType:" + validationType) : ""));
    if (debug || DEBUG || EtomoDirector.INSTANCE.getArguments().isTest()
      || EtomoDirector.INSTANCE.getArguments().isDebug()
      || EtomoDirector.INSTANCE.isUnitTest()) {
      exception.printStackTrace();
    }
    throw exception;
  }

  /**
   * Validates field text based on field type.  Pops up an error message and throws an
   * exception if the validation fails.  Does number format validation on individual
   * numbers and on arrays and lists.  Validates the number of elements in pairs and
   * triples.  Does minimal validatation of the syntax of arrays and lists.  If required
   * is true, validation fails when the field is empty or contains nothing but whitespace.
   * @param fieldText
   * @param fieldType - required
   * @param component
   * @param descr
   * @param validationSet
   * @param validationExtension overrides some parameters
   * @param fieldDisplayer makes sure that the field being validated is displayed (optional)
   * @return fieldText, trimmed if validation is possible on fieldType
   * @throws FieldValidationFailedException if the validation fails
   */
  private static String validateText(final String fieldText, final FieldType fieldType,
    final UIComponent component, final String descr, final boolean required,
    boolean fileMustExist, boolean fileOnly, final ValidationSet validationSet,
    final ValidationExtension validationExtension, final FieldDisplayer fieldDisplayer1,
    final FieldDisplayer fieldDisplayer2, final boolean popupMessage, final boolean debug)
    throws FieldValidationFailedException {
    if (validationExtension != null) {
      fileMustExist = validationExtension.isFileMustExist();
      fileOnly = validationExtension.isFileOnly();
    }
    // validate required (field level)
    if (required && (fieldText == null || fieldText.matches("\\s*"))) {
      fail("an entry is required", fieldText, fieldType, null, component, descr,
        fieldDisplayer1, fieldDisplayer2, popupMessage, debug);
    }
    if (fieldText == null) {
      return null;
    }
    // Don't trim strings or file names.
    if (!fieldType.validationType.numeric) {
      // File validation only if at least one file validation boolean is set.
      if (fieldType == FieldType.FILE && (fileMustExist || fileOnly)
        && !fieldText.matches("\\s*")) {
        File file = new File(fieldText);
        if (fileMustExist && !file.exists()) {
          fail("must contain a file " + (fileOnly ? "" : "or directory ") + "that exists",
            fieldText, fieldType, null, component, descr, fieldDisplayer1,
            fieldDisplayer2, popupMessage, debug);
        }
        if (fileOnly && !file.isFile()) {
          fail("must contain a file", fieldText, fieldType, null, component, descr,
            fieldDisplayer1, fieldDisplayer2, popupMessage, debug);
        }
      }
      return fieldText;
    }
    // Trim because Number.parse... will fail on external spaces.
    String text = fieldText.trim();
    if (fieldType.isCollection()) {
      // Empty collections are valid
      if (text.equals("")) {
        return text;
      }
      // Validate arrays and lists.
      ElementList elementList = new ElementList(fieldType, text);
      if (fieldType.hasRequiredSize()) {
        // Validate the number of elements
        if (!elementList.equalsNElements(fieldType.requiredSize)) {
          // Wrong number of elements
          fail(
            "wrong number of elements - should have " + fieldType.requiredSize
              + " elements",
            fieldText, fieldType, null, component, descr, fieldDisplayer1,
            fieldDisplayer2, popupMessage, debug);
        }
      }
      // Validate integers or floating point numbers in the array or list.
      ElementListIterator iterator = elementList.iterator();
      while (iterator.hasNext()) {
        validateNumber(validationExtension, validationSet, iterator.next(), component,
          descr, fieldType, fieldDisplayer1, fieldDisplayer2, popupMessage, debug);
      }
    }
    else {
      // Validate integers and floating point numbers.
      validateNumber(validationExtension, validationSet, text, component, descr,
        fieldType, fieldDisplayer1, fieldDisplayer2, popupMessage, debug);
    }
    // Validation succeeded - return original trimmed field text.
    return fieldText.trim();
  }

  public static void validatePairedArrays(final String text1, final UIComponent component,
    final String descr1, final String text2, final String descr2)
    throws FieldValidationFailedException {
    String validationType = "comma-separated paired arrays";
    String fieldText = "[text1:" + text1 + ",text2:" + text2 + "]";
    String descr = descr1 + " and " + descr2;
    // At least one must have a valid array
    int num1 = Utilities.getNumberElements(text1);
    int num2 = Utilities.getNumberElements(text2);
    if (num1 == 0 && num2 == 0) {
      fail("required - at least one field must be filled in", fieldText, null,
        validationType, component, descr, null, null, true, false);
    }
    // If both have a value they must have an equal number of array elements, or one must
    // have only one array element
    if (num1 > 1 && num2 > 1 && num1 != num2) {
      fail("the number of elements in the two fields are unequal.  Either use an equal "
        + "number of elements in both fields, use only one field, or put a single element"
        + " in one field", fieldText, null, validationType, component, descr, null, null,
        true, false);
    }
  }

  /**
   * 
   * @param validationSet
   * @param text - trimmed field value
   * @param component
   * @param descr
   * @param fieldType required
   * @param fieldDisplayer
   * @throws FieldValidationFailedException
   */
  private static void validateNumber(final ValidationExtension validationExtension,
    final ValidationSet validationSet, final String text, final UIComponent component,
    final String descr, final FieldType fieldType, final FieldDisplayer fieldDisplayer1,
    final FieldDisplayer fieldDisplayer2, final boolean popupMessage, final boolean debug)
    throws FieldValidationFailedException {
    String errmsg = null;
    if (validationSet != null) {
      errmsg = validationSet.validate(text);
    }
    if (errmsg != null) {
      fail(errmsg, text, fieldType, null, component, descr, null, null, popupMessage,
        false);
    }
    if (text == null || text.isEmpty()) {
      return;
    }
    boolean mustBePositive = false;
    if (validationExtension != null && validationExtension.isMustBePositive()) {
      mustBePositive = true;
    }
    EtomoNumber.Type type = fieldType.getNumericType();
    boolean failed = false;
    try {
      if (type == EtomoNumber.Type.LONG) {
        long l = Long.parseLong(text);
        if (mustBePositive && l <= 0) {
          failed = true;
        }
      }
      else if (type == EtomoNumber.Type.DOUBLE) {
        double d = Double.parseDouble(text);
        if (mustBePositive && d <= 0) {
          failed = true;
        }
      }
      else {
        int i = Integer.parseInt(text);
        if (mustBePositive && i <= 0) {
          failed = true;
        }
      }
      if (failed) {
        fail("requires a postive number", text, fieldType, null, component, descr, null,
          null, popupMessage, false);
      }
    }
    catch (NumberFormatException e) {
      fail("wrong type - should be " + fieldType.validationType, text, fieldType, null,
        component, descr, null, null, popupMessage, false);
    }
  }

  public static boolean equals(final FieldType fieldType, String fieldText1,
    String fieldText2) {
    if (fieldText1 == null || fieldText2 == null) {
      if (fieldText1 == null && fieldText2 == null) {
        return true;
      }
      return false;
    }
    if (fieldType == FieldType.INTEGER) {
      EtomoNumber number1 = new EtomoNumber();
      EtomoNumber number2 = new EtomoNumber();
      number1.set(fieldText1);
      number2.set(fieldText2);
      return number1.equals(number2);
    }
    if (fieldType == FieldType.FLOATING_POINT) {
      EtomoNumber number1 = new EtomoNumber(EtomoNumber.Type.DOUBLE);
      EtomoNumber number2 = new EtomoNumber(EtomoNumber.Type.DOUBLE);
      number1.set(fieldText1);
      number2.set(fieldText2);
      return number1.equals(number2);
    }
    if (fieldType == null || fieldType == FieldType.STRING) {
      return fieldText1.trim().equals(fieldText2.trim());
    }
    if (fieldType == FieldType.FILE) {
      return new File(fieldText1).equals(new File(fieldText2));
    }
    if (fieldType == FieldType.INTEGER_LIST) {
      // Lists are too complicated to parse (they contain things like "1 - 3"). Remove all
      // spaces and compare as strings.
      return fieldText1.replaceAll("\\s+", "").equals(fieldText2.replaceAll("\\s+", ""));
    }
    // Handle arrays
    FieldType numberFieldType = null;
    if (fieldType == FieldType.INTEGER_ARRAY || fieldType == FieldType.INTEGER_PAIR
      || fieldType == FieldType.INTEGER_TRIPLE) {
      numberFieldType = FieldType.INTEGER;
    }
    if (fieldType == FieldType.FLOATING_POINT_ARRAY
      || fieldType == FieldType.FLOATING_POINT_PAIR) {
      numberFieldType = FieldType.FLOATING_POINT;
    }
    ElementList elementList1 = new ElementList(fieldType, fieldText1);
    ElementList elementList2 = new ElementList(fieldType, fieldText2);
    if (elementList1.getNElements() != elementList2.getNElements()) {
      return false;
    }
    ElementListIterator iterator1 = elementList1.iterator();
    ElementListIterator iterator2 = elementList2.iterator();
    while (iterator1.hasNext()) {
      // Call this function with the field type for either float or integer.
      if (!equals(numberFieldType, iterator1.next(), iterator2.next())) {
        return false;
      }
    }
    return true;
  }

  private static final class ElementList {
    private final FieldType fieldType;
    private final String fieldText;

    private String[] list = null;
    private int nElements = -1;

    private ElementList(final FieldType fieldType, final String fieldText) {
      this.fieldType = fieldType;
      this.fieldText = fieldText;
    }

    private boolean equalsNElements(final int input) {
      return getNElements() == input;
    }

    private ElementListIterator iterator() {
      if (list == null) {
        split();
      }
      return new ElementListIterator(list);
    }

    private int getNElements() {
      if (list == null) {
        split();
      }
      if (nElements == -1) {
        countElements();
      }
      return nElements;
    }

    private void split() {
      list = fieldText.split(fieldType.getSplitter());
    }

    private void countElements() {
      nElements = 0;
      if (fieldText == null) {
        return;
      }
      String text = fieldText;
      // The commas at the end of the collection count as elements. Splitting will
      // eliminate these commas, so they have to be counted.
      if (text.endsWith(",")) {
        // Remove all whitespace
        text = text.replaceAll("\\s+", "");
        // Count ending commas. The last comma doesn't count because an extra comma is
        // necessary to add an element onto the end.
        for (int i = text.length() - 2; i >= 0; i--) {
          if (text.charAt(i) == ',') {
            nElements++;
          }
          else {
            break;
          }
        }
      }
      if (list != null) {
        nElements += list.length;
      }
    }
  }

  private static final class ElementListIterator {
    private final String[] list;

    private int index = 0;

    private ElementListIterator(final String[] list) {
      this.list = list;
    }

    private boolean hasNext() {
      if (list == null || index < 0 || index >= list.length) {
        return false;
      }
      return true;
    }

    private String next() {
      if (list == null || index < 0 || index >= list.length) {
        return null;
      }
      return list[index++];
    }
  }
}
