package etomo.logic;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import etomo.BaseManager;
import etomo.storage.DirectiveDef;
import etomo.storage.DirectiveFileInterface;
import etomo.storage.DirectiveValue;
import etomo.storage.LogFile;
import etomo.storage.NameValuePairList;
import etomo.storage.autodoc.Autodoc;
import etomo.storage.autodoc.AutodocFactory;
import etomo.storage.autodoc.WritableAutodoc;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.ConstEtomoNumber;
import etomo.type.EnumeratedType;
import etomo.type.FileType;
import etomo.ui.BooleanFieldInterface;
import etomo.ui.BooleanTextFieldInterface;
import etomo.ui.Field;
import etomo.ui.FieldDisplayer;
import etomo.ui.FieldValidationFailedException;
import etomo.ui.TextFieldInterface;
import etomo.ui.swing.RadioButton;
import etomo.ui.swing.TextEfieldInterface;

/**
 * <p>Description: Shared functions for the BatchRunTomo interface.</p>
 * 
 * <pre>
 * 
 * Directives:
 * - When a directiveFiles contains a directiveDef, the directive is set and not
 *   overridden.
 * - Only a non-boolean directive can be overridden.  Boolean directives can only be true
 *   or false.
 *   
 * Setting Fields:
 * 
 * Text Field Values:
 * The value may be:
 * - A text directive value
 * - A value derived from a directive value
 * - When to set:
 *   - !setFieldHighlightValue
 *   - directiveFiles contains directiveDef
 *   
 * Boolean Field Values:
 * The value may be:
 * - A boolean directive value
 * - A value derived from a directive value
 * - True if directiveFiles contains directiveDef
 * - False if directiveFiles does not contain directiveDef
 * - When to set:
 *   - !setFieldHighlightValue
 *   
 * Field Highlight Values:
 * The value is matched against the field value, so that same rules apply
 * - When to set:
 *   - setFieldHighlightValue
 *   - directiveFiles contains directiveDef
 *  
 * TemplateValues elements:
 * - The value is always the directive value
 * - When to set:
 *   - setFieldHighlightValue
 *   - directiveFiles contains directiveDef
 *   
 * Saving to the Autodoc:
 * - Do not save when the directive value equals the value in templateValues.
 * 
 * Boolean Field Value:
 * - To boolean directive:
 *   - True if enabled and selected
 *   - False if disabled or not selected
 * - To text directive:
 *   - Override if false
 *   - A value derived from boolean field value
 * 
 * Text Field Value:
 * - To boolean directive:
 *   - True if enabled and not blank
 *   - False if disabled or blank
 * - To text directive:
 *   - The value of the field
 *   - A value derived from the value of the field
 *   - An override if disabled or blank
 *   
 * </pre>
 *
 * <p>Copyright: Copyright 2014 - 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class BatchTool {
  private static boolean debug = false;

  public static void setDebug(final boolean input) {
    debug = input;
  }

  public static boolean isDebug() {
    return debug;
  }

  /**
   * Combine templates.  Merge templates in order of precedence.  Call this once per save.
   * 
   * Formula:
   *
   * <  : merge (when name is missing on left side) higher priority + lower priority
   * 
   * templates =  templateUser < templateSystem < templateScope < batchDefaults
   * 
   * @param manager
   * @param templateFiles
   * @return templates
   */
  public static NameValuePairList mergeTemplates(final BaseManager manager,
    final File[] templateFiles) {
    // Merge templates together in order of decreasing priority (highest first)
    // templates = templateUser + templateSystem + templateScope
    NameValuePairList templates = null;
    if (templateFiles != null) {
      for (int i = templateFiles.length - 1; i >= 0; i--) {
        try {
          if (templateFiles[i] != null) {
            if (templates == null) {
              templates = new NameValuePairList(
                AutodocFactory.getAutodocInstance(manager, templateFiles[i]));
            }
            else {
              templates.merge(new NameValuePairList(
                AutodocFactory.getAutodocInstance(manager, templateFiles[i])));
            }
          }
        }
        catch (LogFile.LockException e) {
          e.printStackTrace();
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    // + batchDefaults
    try {
      File batchDefault =
        FileType.DEFAULT_BATCH_RUN_TOMO_AUTODOC.getFile(manager, AxisID.ONLY);
      if (batchDefault.exists()) {
        if (templates == null) {
          templates = new NameValuePairList(
            AutodocFactory.getAutodocInstance(manager, batchDefault));
        }
        else {
          templates.merge(new NameValuePairList(
            AutodocFactory.getAutodocInstance(manager, batchDefault)));
        }
      }
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return templates;
  }

  /**
   * Combine dialog data with autodocs.  Data and autodocs are merged in order of
   * precedence.  Then templates are subtracted when they match both name and value.  Call
   * for global dialog and each row.
   * 
   * Formula:
   *
   * <  : merge when name is missing on left side - higher priority + lower priority
   * -- : subtract from left side when both name and value match
   * -  : subtract from left side when the directive matches
   * 
   * templates =  templateUser < templateSystem < templateScope < batchDefaults
   * 
   * If the advanced dialog has not been created:
   * directiveFiles = (batchFile - basidDirectives) < (startingBatchFile - basicDirectives) < templates
   * 
   * If the advanced dialog has been created:
   * directiveFiles = templates
   * 
   * batchState =  (dialogData < directiveFiles) -- templates
   * 
   * @param manager
   * @param dialogData (required) - represents the dialogs
   * @param advancedDialogExists - true if the advanced dialog has been created
   * @param loadedBatchList - not used if advancedDialogExists.  The loaded batch.adoc file.
   * @param basicDirectives - not used if advancedDialogExists.  A list of basic directives
   * @param advancedStartingBatchList - not used if advancedDialogExists.  The starting batch file minus the basic dirctives
   * @param templates
   * @return savebatchList - respresents the new batch.adoc file
   */
  public static NameValuePairList createBatchFile(final BaseManager manager,
    final Autodoc dialogData, final boolean advancedDialogExists,
    final NameValuePairList loadedBatchList, final Set<DirectiveDef> basicDirectives,
    final NameValuePairList advancedStartingBatchList,
    final NameValuePairList templates) {
    //
    // directiveFiles - represents a merging of all of the directives files.
    NameValuePairList directiveFiles = null;
    // Included the advanced directives by getting them from files. This is unnecessary if
    // the advanced dialog is open.
    if (!advancedDialogExists) {
      // the advanced directives from the loaded batch file
      if (loadedBatchList != null) {
        loadedBatchList.subtract(basicDirectives);
        directiveFiles = new NameValuePairList(loadedBatchList);
      }
      // The advanced part of the starting batch
      if (advancedStartingBatchList != null) {
        if (directiveFiles == null) {
          directiveFiles = new NameValuePairList(advancedStartingBatchList);
        }
        else {
          directiveFiles.merge(advancedStartingBatchList);
        }
      }
    }
    // templates
    if (directiveFiles == null) {
      directiveFiles = new NameValuePairList(templates);
    }
    else {
      directiveFiles.merge(templates);
    }
    //
    // savebatchList - combine the dialog data with the directive files
    // merge directiveFiles
    NameValuePairList savebatchList = new NameValuePairList(dialogData);
    if (directiveFiles != null) {
      savebatchList.merge(directiveFiles);
    }
    if (templates == null) {
      return savebatchList;
    }
    // Substract the templates and batch default since these directives do not have to be
    // saved.
    savebatchList.subtract(templates, NumericComparisonStrategy.INSTANCE);
    return savebatchList;
  }

  // private static final class NumericComparisonStrategy implements ComparisonStrategy {}

  /**
   * @param textFieldTd - text field with text directive
   * @param directiveFiles
   * @param setFieldHighlightValue
   * @param templateValues
   * @return true if directiveFiles contains directiveDef
   */
  public static boolean setTextValue(final TextFieldInterface textFieldTd,
    final DirectiveFileInterface directiveFiles, final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues) {
    if (textFieldTd == null) {
      return false;
    }
    DirectiveDef directiveDef = textFieldTd.getDirectiveDef();
    if (!directiveFiles.contains(directiveDef, setFieldHighlightValue, true)) {
      return false;
    }
    DirectiveValue directiveValue =
      directiveFiles.getValue(directiveDef, setFieldHighlightValue, true);
    if (directiveValue == null) {
      return false;
    }
    String value = directiveValue.getValue();
    if (setFieldHighlightValue) {
      if (templateValues != null) {
        templateValues.put(directiveDef, value);
      }
      textFieldTd.setFieldHighlight(value);
    }
    else {
      textFieldTd.setValue(value);
    }
    return true;
  }

  /**
   * @param textFieldTd - text field with text directive
   * @param directiveFiles
   * @param setFieldHighlightValue
   * @param templateValues
   * @return the value if directiveFiles contains directiveDef, "" for an override, null if it does not cotain the directiveDef
   */
  public static DirectiveValue setTextValue(final TextEfieldInterface textFieldTd,
    final DirectiveFileInterface directiveFiles, final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues, final boolean includeOverride) {
    if (textFieldTd == null) {
      return null;
    }
    DirectiveDef directiveDef = textFieldTd.getDirectiveDef();
    if (!directiveFiles.contains(directiveDef, setFieldHighlightValue, true,
      includeOverride)) {
      return null;
    }
    DirectiveValue directiveValue = directiveFiles.getValue(directiveDef,
      setFieldHighlightValue, true, includeOverride);
    if (directiveValue == null) {
      return null;
    }
    String value = directiveValue.getValue();
    if (setFieldHighlightValue) {
      if (templateValues != null) {
        templateValues.put(directiveDef, value);
      }
      textFieldTd.setFieldHighlight(value);
    }
    else {
      // !!!set override here if it was found in a batch file
      textFieldTd.setText(value);
    }
    return directiveValue;
  }

  /**
   * @param textFieldTd
   * @param directiveFiles
   * @param setFieldHighlightValue
   * @param templateValues
   * @param axisID
   * @return
   */
  public static boolean setTextValue(final TextFieldInterface textFieldTd,
    final DirectiveFileInterface directiveFiles, final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues, final AxisID axisID) {
    if (textFieldTd == null) {
      return false;
    }
    DirectiveDef directiveDef = textFieldTd.getDirectiveDef();
    if (!directiveFiles.contains(directiveDef, axisID, setFieldHighlightValue, true)) {
      return false;
    }
    DirectiveValue directiveValue =
      directiveFiles.getValue(directiveDef, axisID, setFieldHighlightValue, true);
    if (directiveValue == null) {
      return false;
    }
    String value = directiveValue.getValue();
    if (setFieldHighlightValue) {
      if (templateValues != null) {
        templateValues.put(directiveDef, value);
      }
      textFieldTd.setFieldHighlight(value);
    }
    else {
      textFieldTd.setValue(value);
    }
    return true;
  }

  /**
   * Set text fields to directive array value.
   * @param textFieldAd0 - text field with array directive
   * @param textFieldAd1 - text field with array directive
   * @param directiveFiles
   * @param setFieldHighlightValue
   * @param templateValues
   * @return true if directiveFiles contains directiveDef
   */
  public static boolean setTextValues(final TextFieldInterface textFieldAd0,
    final TextFieldInterface textFieldAd1, final DirectiveFileInterface directiveFiles,
    final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues) {
    if (textFieldAd0 == null) {
      return false;
    }
    DirectiveDef directiveDef = textFieldAd0.getDirectiveDef();
    if (!directiveFiles.contains(directiveDef, setFieldHighlightValue, true)) {
      return false;
    }
    DirectiveValue directiveValue =
      directiveFiles.getValue(directiveDef, setFieldHighlightValue, true);
    if (directiveValue == null) {
      return false;
    }
    String value = directiveValue.getValue();
    String value0 = null;
    String value1 = null;
    String[] valueArray = value.split("\\s*,\\s*");
    if (valueArray != null && valueArray.length > 0) {
      value0 = valueArray[0];
      if (valueArray.length > 1) {
        value1 = valueArray[1];
      }
    }
    if (setFieldHighlightValue) {
      if (templateValues != null) {
        templateValues.put(directiveDef, value);
      }
      textFieldAd0.setFieldHighlight(value0);
      textFieldAd1.setFieldHighlight(value1);
    }
    else {
      textFieldAd0.setValue(value0);
      textFieldAd1.setValue(value1);
    }
    return true;
  }

  /**
   * @param textFieldTd - text field with a text directive
   * @param directiveValue - the directive value
   * @param derivedValue
   * @param templateValues
   * @param setFieldHighlightValue
   */
  public static void setTextValue(final TextFieldInterface textFieldTd,
    final String directiveValue, final String derivedValue,
    final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues) {
    if (textFieldTd == null) {
      return;
    }
    if (directiveValue == null) {
      return;
    }
    if (setFieldHighlightValue) {
      if (templateValues != null) {
        templateValues.put(textFieldTd.getDirectiveDef(), directiveValue);
      }
      if (derivedValue != null) {
        textFieldTd.setFieldHighlight(derivedValue);
      }
    }
    else {
      textFieldTd.setValue(derivedValue);
    }
  }

  /**
   * @param textFieldTd - text field with a text directive
   * @param directiveValue - the directive value
   * @param templateValues
   * @param setFieldHighlightValue
   */
  public static void setTextValue(final TextFieldInterface textFieldTd,
    final String directiveValue, final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues) {
    if (textFieldTd == null) {
      return;
    }
    if (directiveValue == null) {
      return;
    }
    if (setFieldHighlightValue) {
      if (templateValues != null) {
        templateValues.put(textFieldTd.getDirectiveDef(), directiveValue);
      }
      if (directiveValue != null) {
        textFieldTd.setFieldHighlight(directiveValue);
      }
    }
    else {
      textFieldTd.setValue(directiveValue);
    }
  }

  /**
   * @param booleanFieldBd - boolean field with boolean directive
   * @param directiveFiles
   * @param setFieldHighlightValue
   * @param templateValues
   * @return true if directiveFiles contains directiveDef
   */
  public static boolean setBooleanValue(final BooleanFieldInterface booleanFieldBd,
    final DirectiveFileInterface directiveFiles, final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues) {
    if (booleanFieldBd == null) {
      return false;
    }
    DirectiveDef directiveDef = booleanFieldBd.getDirectiveDef();
    if (!directiveFiles.contains(directiveDef, setFieldHighlightValue, true)) {
      return false;
    }
    boolean selected = false;
    selected = directiveFiles.isValue(directiveDef, setFieldHighlightValue, true);
    if (setFieldHighlightValue) {
      if (templateValues != null) {
        templateValues.put(directiveDef, DirectiveDef.getBooleanValue(selected));
      }
      booleanFieldBd.setFieldHighlight(selected);
    }
    else {
      booleanFieldBd.setValue(selected);
    }
    return true;
  }

  /**
   * @param booleanFieldBd - boolean field with boolean directive
   * @param directiveFiles
   * @param setFieldHighlightValue
   * @param templateValues
   * @return true if directiveFiles contains directiveDef
   */
  public static boolean setBooleanValue(final BooleanFieldInterface booleanFieldBd,
    final DirectiveFileInterface directiveFiles, final boolean setFieldHighlightValue,
    final boolean missingFieldSetsValue, final Map<DirectiveDef, String> templateValues) {
    if (booleanFieldBd == null) {
      return false;
    }
    DirectiveDef directiveDef = booleanFieldBd.getDirectiveDef();
    if (!directiveFiles.contains(directiveDef, setFieldHighlightValue, true)) {
      if (missingFieldSetsValue) {
        booleanFieldBd.setValue(false);
      }
      return false;
    }
    boolean selected = false;
    selected = directiveFiles.isValue(directiveDef, setFieldHighlightValue, true);
    if (setFieldHighlightValue) {
      if (templateValues != null) {
        templateValues.put(directiveDef, DirectiveDef.getBooleanValue(selected));
      }
      booleanFieldBd.setFieldHighlight(selected);
    }
    else {
      booleanFieldBd.setValue(selected);
    }
    return true;
  }

  /**
   * @param booleanFieldTd
   * @param selectedText
   * @param directiveFiles
   * @param setFieldHighlightValue
   * @param templateValues
   * @return
   */
  public static boolean setBooleanValueFromSelectedText(
    final BooleanFieldInterface booleanFieldTd, final String selectedText,
    final DirectiveFileInterface directiveFiles, final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues) {
    if (booleanFieldTd == null) {
      return false;
    }
    DirectiveDef directiveDef = booleanFieldTd.getDirectiveDef();
    if (!directiveFiles.contains(directiveDef, setFieldHighlightValue, true)) {
      return false;
    }
    boolean selected = false;
    DirectiveValue directiveValue =
      directiveFiles.getValue(directiveDef, setFieldHighlightValue, true);
    if (directiveValue == null) {
      return false;
    }
    String value = directiveValue.getValue();
    if (value != null && value.equals(selectedText)) {
      selected = true;
    }
    if (setFieldHighlightValue) {
      if (templateValues != null) {
        templateValues.put(directiveDef, value);
      }
      booleanFieldTd.setFieldHighlight(selected);
    }
    else {
      booleanFieldTd.setValue(selected);
    }
    return true;
  }

  /**
   * Sets boolean field to the inverse of the directive value
   * @param booleanFieldBd - boolean field with boolean directive
   * @param directiveFiles
   * @param setFieldHighlightValue
   * @param templateValues
   * @return true if directiveFiles contains directiveDef
   */
  public static boolean setNegatedBooleanValue(final BooleanFieldInterface booleanFieldBd,
    final DirectiveFileInterface directiveFiles, final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues) {
    if (booleanFieldBd == null) {
      return false;
    }
    DirectiveDef directiveDef = booleanFieldBd.getDirectiveDef();
    if (!directiveFiles.contains(directiveDef, setFieldHighlightValue, true)) {
      return false;
    }
    boolean selected = false;
    selected = directiveFiles.isValue(directiveDef, setFieldHighlightValue, true);
    if (setFieldHighlightValue) {
      if (templateValues != null) {
        templateValues.put(directiveDef, DirectiveDef.getBooleanValue(selected));
      }
      booleanFieldBd.setFieldHighlight(!selected);
    }
    else {
      booleanFieldBd.setValue(!selected);
    }
    return true;
  }

  /**
   * Unable to add an entry to templateValues, as the directive value is not known.
   * @param booleanField - boolean field
   * @param selected
   * @param setFieldHighlightValue
   */
  public static void setBooleanValue(final BooleanFieldInterface booleanField,
    final boolean selected, final boolean setFieldHighlightValue) {
    if (booleanField == null) {
      return;
    }
    if (setFieldHighlightValue) {
      booleanField.setFieldHighlight(selected);
    }
    else {
      booleanField.setValue(selected);
    }
  }

  /**
   * Unable to add an entry to templateValues, as the directive value is not known.
   * @param booleanField - boolean field
   * @param directiveValue
   * @param highlight
   * @param setFieldHighlightValue
   */
  public static void setBooleanValue(final BooleanFieldInterface booleanField,
    final boolean selected, final boolean highlight,
    final boolean setFieldHighlightValue) {
    if (booleanField == null) {
      return;
    }
    if (setFieldHighlightValue) {
      if (highlight) {
        booleanField.setFieldHighlight(selected);
      }
    }
    else {
      booleanField.setValue(selected);
    }
  }

  /**
   * Unable to add an entry to templateValues, as the directive value is not known.
   * @param booleanField - boolean field
   * @param element0
   * @param element1
   * @param setFieldHighlightValue
   * @return field exists and element0 == element1
   */
  public static boolean setBooleanValueIfEquals(final BooleanFieldInterface booleanField,
    final String element0, final int element1, final boolean setFieldHighlightValue) {
    if (booleanField == null) {
      return false;
    }
    boolean selected = false;
    try {
      selected = Double.parseDouble(element0) == element1;
      if (setFieldHighlightValue) {
        booleanField.setFieldHighlight(selected);
      }
    }
    catch (NumberFormatException e) {}
    if (!setFieldHighlightValue) {
      booleanField.setValue(selected);
    }
    return selected;
  }

  /**
   * @param booleanFieldTd - boolean field with text directive
   * @param directiveFiles
   * @param setFieldHighlightValue
   * @param templateValues
   * @return directive value
   */
  public static String setBooleanValueFromText(final BooleanFieldInterface booleanFieldTd,
    final DirectiveFileInterface directiveFiles, final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues) {
    if (booleanFieldTd == null) {
      return null;
    }
    DirectiveDef directiveDef = booleanFieldTd.getDirectiveDef();
    if (!directiveFiles.contains(directiveDef, setFieldHighlightValue, true, true)) {
      return null;
    }
    DirectiveValue directiveValue =
      directiveFiles.getValue(directiveDef, setFieldHighlightValue, true);
    // If directive value is null then its an override because this is a string directive.
    String value = null;
    if (directiveValue != null) {
      value = directiveValue.getValue();
    }
    if (setFieldHighlightValue) {
      if (templateValues != null && value != null) {
        templateValues.put(directiveDef, DirectiveDef.getBooleanValue(value));
      }
      booleanFieldTd.setFieldHighlight(value != null);
    }
    if (!setFieldHighlightValue) {
      booleanFieldTd.setValue(value != null);
    }
    return value;
  }

  /**
   * @param booleanFieldTd - boolean field with text directive
   * @param directiveFiles
   * @param setFieldHighlightValue
   * @param notValue - set field to false if directive value equals notValue
   * @param templateValues
   * @return directive value
   */
  public static String setBooleanValueFromUnselectedText(
    final BooleanFieldInterface booleanFieldTd, final String unselectedText,
    final DirectiveFileInterface directiveFiles, final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues) {
    if (booleanFieldTd == null) {
      return null;
    }
    DirectiveDef directiveDef = booleanFieldTd.getDirectiveDef();
    if (!directiveFiles.contains(directiveDef, setFieldHighlightValue, true)) {
      return null;
    }
    boolean selected = false;
    DirectiveValue directiveValue =
      directiveFiles.getValue(directiveDef, setFieldHighlightValue, true);
    if (directiveValue == null) {
      return null;
    }
    String value = directiveValue.getValue();
    if (!value.equals(unselectedText)) {
      selected = true;
    }
    if (setFieldHighlightValue) {
      if (templateValues != null) {
        templateValues.put(directiveDef, DirectiveDef.getBooleanValue(value));
      }
      booleanFieldTd.setFieldHighlight(selected);
    }
    else {
      booleanFieldTd.setValue(selected);
    }
    return value;
  }

  /**
   * @param booleanTextFieldTd - boolean text field with text directive
   * @param directiveFiles
   * @param setFieldHighlightValue
   * @param templateValues
   * @return true if directiveFiles contains directive
   */
  public static boolean setBooleanTextValue(
    final BooleanTextFieldInterface booleanTextFieldTd,
    final DirectiveFileInterface directiveFiles, final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues) {
    if (booleanTextFieldTd == null) {
      return false;
    }
    DirectiveDef directiveDef = booleanTextFieldTd.getDirectiveDef();
    if (!directiveFiles.contains(directiveDef, setFieldHighlightValue, true)) {
      return false;
    }
    DirectiveValue directiveValue =
      directiveFiles.getValue(directiveDef, setFieldHighlightValue, true);
    if (directiveValue == null) {
      return false;
    }
    String value = directiveValue.getValue();
    if (setFieldHighlightValue) {
      if (templateValues != null) {
        templateValues.put(directiveDef, DirectiveDef.getBooleanValue(value));
      }
      booleanTextFieldTd.setFieldHighlight(value);
      booleanTextFieldTd.setFieldHighlight(true);
    }
    else {
      booleanTextFieldTd.setValue(value);
      booleanTextFieldTd.setValue(true);
    }
    return true;
  }

  /**
   * @param booleanTextField - boolean text field
   * @param text
   * @param setFieldHighlightValue
   * @return true if text is not empty
   */
  public static boolean setBooleanTextValue(
    final BooleanTextFieldInterface booleanTextField, final String text,
    final boolean setFieldHighlightValue) {
    if (booleanTextField == null) {
      return false;
    }
    if (text == null || text.matches("\\s*")) {
      return false;
    }
    if (setFieldHighlightValue) {
      booleanTextField.setFieldHighlight(text);
      booleanTextField.setFieldHighlight(true);
    }
    else {
      booleanTextField.setValue(text);
      booleanTextField.setValue(true);
    }
    return true;
  }

  /**
   * Assumes the two fields have the same directive
   * @param booleanFieldTd - boolean field with text directive
   * @param textFieldTd - text field with text directive
   * @param directiveFiles
   * @param setFieldHighlightValue
   * @param templateValues
   * @return true if directiveFiles contains directive
   */
  public static boolean setBooleanAndTextValue(final BooleanFieldInterface booleanFieldTd,
    final TextFieldInterface textFieldTd, final DirectiveFileInterface directiveFiles,
    final boolean setFieldHighlightValue,
    final Map<DirectiveDef, String> templateValues) {
    if (booleanFieldTd == null) {
      return false;
    }
    DirectiveDef directiveDef = booleanFieldTd.getDirectiveDef();
    if (!directiveFiles.contains(directiveDef, setFieldHighlightValue, true)) {
      return false;
    }
    DirectiveValue directiveValue =
      directiveFiles.getValue(directiveDef, setFieldHighlightValue, true);
    if (directiveValue == null) {
      return false;
    }
    String value = directiveValue.getValue();
    if (setFieldHighlightValue) {
      if (templateValues != null) {
        templateValues.put(directiveDef, value);
      }
      booleanFieldTd.setFieldHighlight(true);
      textFieldTd.setFieldHighlight(value);
    }
    else {
      booleanFieldTd.setValue(true);
      textFieldTd.setValue(value);
    }
    return true;
  }

  /**
   * @param enabled
   * @param directiveDef
   * @param text
   * @param defaultText
   * @param autodoc
   * @param templateValues
   * @throws FieldValidationFailedException
   */
  public static void saveTextToAutodoc(final boolean enabled,
    final DirectiveDef directiveDef, String text, final String defaultText,
    final WritableAutodoc autodoc, final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    if (text == null && defaultText != null) {
      text = defaultText;
    }
    if (text == null || !enabled || text.matches("\\s*")) {
      text = "";
    }
    if (templateValues != null) {
      if (text.equals("") && !templateValues.containsKey(directiveDef)) {
        return;
      }
      String templateValue = templateValues.get(directiveDef);
      if (text.equals(templateValue)) {
        return;
      }
    }
    autodoc.addNameValuePairAttribute(directiveDef.getDirective(), text);
  }

  /**
   * Save text which causes a function to for performed.  This is only necessary when the
   * template value is set to something else.  Do not save if there is no template value.
   * @param directiveDef
   * @param enumeratedType
   * @param autodoc
   * @param templateValues
   * @throws FieldValidationFailedException
   */
  public static void saveNotTextToAutodoc(final DirectiveDef directiveDef,
    final EnumeratedType enumeratedType, final WritableAutodoc autodoc,
    final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    String templateValue = null;
    if (templateValues != null) {
      if (enumeratedType == null || !templateValues.containsKey(directiveDef)) {
        return;
      }
      templateValue = templateValues.get(directiveDef);
    }
    String text = enumeratedType.toString();
    if (templateValue != null && templateValue.equals(text)) {
      return;
    }
    autodoc.addNameValuePairAttribute(directiveDef.getDirective(), text);
  }

  /** 
   * @param fieldTd - field with text directive
   * @param text
   * @param autodoc
   * @param templateValues
   * @throws FieldValidationFailedException
   */
  public static void saveTextToAutodoc(final Field fieldTd, final String text,
    final WritableAutodoc autodoc, final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    if (fieldTd == null) {
      return;
    }
    saveTextToAutodoc(fieldTd.isEnabled(), fieldTd.getDirectiveDef(), text, null, autodoc,
      templateValues);
  }

  /** 
  * @param fieldTd - field with text directive
  * @param text
  * @param autodoc
  * @param templateValues
  * @throws FieldValidationFailedException
  */
  public static void saveTextToAutodoc(final TextEfieldInterface fieldTd,
    final String text, final WritableAutodoc autodoc,
    final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    if (fieldTd == null) {
      return;
    }
    saveTextToAutodoc(fieldTd.isEnabled(), fieldTd.getDirectiveDef(), text, null, autodoc,
      templateValues);
  }

  /** 
  * @param fieldTd - field with text directive
  * @param text
  * @param autodoc
  * @param templateValues
  * @throws FieldValidationFailedException
  */
  public static void saveTextToAutodoc(final TextEfieldInterface fieldTd,
    final String text, final boolean overrideAvailable, final boolean override,
    final WritableAutodoc autodoc, final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    if (fieldTd == null) {
      return;
    }
    if (!overrideAvailable) {
      saveTextToAutodoc(fieldTd.isEnabled(), fieldTd.getDirectiveDef(), text, null,
        autodoc, templateValues);
    }
    else {
      saveTextToAutodoc(fieldTd.isEnabled(), fieldTd.getDirectiveDef(), text, override,
        autodoc, templateValues);
    }
  }

  /**
   * @param enabled
   * @param directiveDef
   * @param text
   * @param override - specifies the override - use this instead of text, and always override if it is on
   * @param defaultText
   * @param autodoc
   * @param templateValues
   * @throws FieldValidationFailedException
   */
  public static void saveTextToAutodoc(final boolean enabled,
    final DirectiveDef directiveDef, String text, final boolean override,
    final WritableAutodoc autodoc, final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    if (!enabled) {
      // Field is not in use
      return;
    }
    if (!override) {
      if (text == null || text.matches("\\s*")) {
        // Not override and no text - nothing to do
        return;
      }
    }
    else {
      // override
      text = "";
    }
    // Keep values that are the same as template values out of the output.
    if (!override && templateValues != null && templateValues.containsKey(directiveDef)
      && text.equals(templateValues.get(directiveDef))) {
      return;
    }
    autodoc.addNameValuePairAttribute(directiveDef.getDirective(), text);
  }

  /** 
   * @param booleanFieldTd - boolean field with text directive
   * @param text
   * @param autodoc
   * @throws FieldValidationFailedException
   */
  public static void saveBooleanTextToAutodoc(final BooleanFieldInterface booleanFieldTd,
    final String text, final WritableAutodoc autodoc,
    final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    if (booleanFieldTd == null) {
      return;
    }
    saveTextToAutodoc(booleanFieldTd.isEnabled() && booleanFieldTd.isSelected(),
      booleanFieldTd.getDirectiveDef(), text, null, autodoc, templateValues);
  }

  /** 
   * @param booleanFieldTd - boolean field with text directive
   * @param text
   * @param autodoc
   * @throws FieldValidationFailedException
   */
  public static void saveBooleanTextToAutodoc(final BooleanFieldInterface booleanFieldTd,
    final String text, final WritableAutodoc autodoc)
    throws FieldValidationFailedException {
    if (booleanFieldTd == null) {
      return;
    }
    saveTextToAutodoc(booleanFieldTd.isEnabled() && booleanFieldTd.isSelected(),
      booleanFieldTd.getDirectiveDef(), text, null, autodoc, null);
  }

  /**
   * @param booleanFieldTd - boolean field with text directive
   * @param autodoc
   * @param selectedText
   * @param unselectedText
   * @throws FieldValidationFailedException
   */
  public static void saveBooleanTextToAutodoc(final BooleanFieldInterface booleanFieldTd,
    final WritableAutodoc autodoc, final String selectedText, final String unselectedText)
    throws FieldValidationFailedException {
    if (booleanFieldTd == null) {
      return;
    }
    if (booleanFieldTd.isSelected()) {
      saveTextToAutodoc(booleanFieldTd.isEnabled(), booleanFieldTd.getDirectiveDef(),
        selectedText, null, autodoc, null);
    }
    else {
      saveTextToAutodoc(booleanFieldTd.isEnabled(), booleanFieldTd.getDirectiveDef(),
        unselectedText, null, autodoc, null);
    }
  }

  /** 
   * @param booleanFieldTd - boolean field with text directive
   * @param text
   * @param defaultText
   * @param autodoc
   * @param templateValues
   * @throws FieldValidationFailedException
   */
  public static void saveBooleanTextToAutodoc(final BooleanFieldInterface booleanFieldTd,
    final String text, final String defaultText, final WritableAutodoc autodoc,
    final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    if (booleanFieldTd == null) {
      return;
    }
    saveTextToAutodoc(booleanFieldTd.isEnabled() && booleanFieldTd.isSelected(),
      booleanFieldTd.getDirectiveDef(), text, defaultText, autodoc, templateValues);
  }

  /**
   * @param booleanTextFieldTd - boolean text field with text directive
   * @param autodoc
   * @param doValidation
   * @param fieldDisplayer
   * @param templateValues
   * @throws FieldValidationFailedException
   */
  public static void saveBooleanTextToAutodoc(
    final BooleanTextFieldInterface booleanTextFieldTd, final WritableAutodoc autodoc,
    final boolean doValidation, final FieldDisplayer fieldDisplayer,
    final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    if (booleanTextFieldTd == null) {
      return;
    }
    saveTextToAutodoc(booleanTextFieldTd.isEnabled() && booleanTextFieldTd.isSelected(),
      booleanTextFieldTd.getDirectiveDef(),
      booleanTextFieldTd.getText(doValidation, fieldDisplayer), null, autodoc,
      templateValues);
  }

  /**
   * @param textFieldTd - text field with text directive
   * @param autodoc
   * @param doValidation
   * @param fieldDisplayer
   * @param templateValues
   * @throws FieldValidationFailedException
   */
  public static void saveTextToAutodoc(final TextFieldInterface textFieldTd,
    final WritableAutodoc autodoc, final boolean doValidation,
    final FieldDisplayer fieldDisplayer, final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    if (textFieldTd == null) {
      return;
    }
    saveTextToAutodoc(textFieldTd.isEnabled(), textFieldTd.getDirectiveDef(),
      textFieldTd.getText(doValidation, fieldDisplayer), null, autodoc, templateValues);
  }

  /**
   * Do not override if no enumerated type is retrieved.  Some radio buttons in the
   * group may not have an enumerated type, and will have to be handled differently.
   * @param modelTd - model of a radio button with a text directive
   * @param autodoc
   * @param templateValues
   * @return true if an enumerated type was retrieved
   * @throws FieldValidationFailedException
   */
  public static boolean saveTextToAutodoc(final RadioButton.RadioButtonModel modelTd,
    final WritableAutodoc autodoc, final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    if (modelTd == null) {
      return false;
    }
    EnumeratedType enumeratedType = modelTd.getEnumeratedType();
    if (enumeratedType == null) {
      return false;
    }
    String text = null;
    if (enumeratedType != null) {
      ConstEtomoNumber number = enumeratedType.getValue();
      if (number != null && !number.isNull()) {
        text = number.toString();
      }
    }
    Field field = modelTd.getField();
    saveTextToAutodoc(field.isEnabled(), field.getDirectiveDef(), text, null, autodoc,
      templateValues);
    return true;
  }

  /**
   * @param enabled
   * @param directiveDef
   * @param enumeratedType
   * @param defaultText
   * @param autodoc
   * @param templateValues
   * @throws FieldValidationFailedException
   */
  public static void saveTextToAutodoc(final boolean enabled,
    final DirectiveDef directiveDef, final EnumeratedType enumeratedType,
    final WritableAutodoc autodoc, final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    String text = null;
    if (enumeratedType != null) {
      ConstEtomoNumber number = enumeratedType.getValue();
      if (number != null && !number.isNull()) {
        text = number.toString();
      }
    }
    saveTextToAutodoc(enabled, directiveDef, text, null, autodoc, templateValues);
  }

  /**
   * @param booleanFieldBd
   * @param autodoc
   * @param templateValues
   * @return true if field is selected
   * @throws FieldValidationFailedException
   */
  public static boolean saveBooleanToAutodoc(final BooleanFieldInterface booleanFieldBd,
    final WritableAutodoc autodoc, final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    if (booleanFieldBd == null) {
      return false;
    }
    return saveBooleanToAutodoc(booleanFieldBd,
      booleanFieldBd.isEnabled() && booleanFieldBd.isSelected(), autodoc, templateValues);
  }

  /**
   * @param fieldBd - field with boolean directive
   * @param selected
   * @param autodoc
   * @param templateValues
   * @return true if field is selected
   * @throws FieldValidationFailedException
   */
  public static boolean saveBooleanToAutodoc(final Field fieldBd, final boolean selected,
    final WritableAutodoc autodoc, final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    if (fieldBd == null) {
      return false;
    }
    DirectiveDef directiveDef = fieldBd.getDirectiveDef();
    if (templateValues != null) {
      if (!selected && !templateValues.containsKey(directiveDef)) {
        return selected;
      }
      boolean templateValue =
        DirectiveDef.convertToBoolean(templateValues.get(directiveDef));
      if (selected == templateValue) {
        return selected;
      }
    }
    autodoc.addNameValuePairAttribute(directiveDef.getDirective(),
      DirectiveDef.getBooleanValue(selected));
    return selected;
  }

  /**
   * @param directiveDef - must be a text directive
   * @param autodoc
   * @param templateValues
   * @throws FieldValidationFailedException
   */
  public static void overrideInAutodoc(final DirectiveDef directiveDef,
    final WritableAutodoc autodoc, final Map<DirectiveDef, String> templateValues)
    throws FieldValidationFailedException {
    if (templateValues != null && templateValues.containsKey(directiveDef)) {
      // not there, or already overridden.
      return;
    }
    autodoc.addNameValuePairAttribute(directiveDef.getDirective(), "");
  }

  public static String getModelFileName(final BaseManager manager,
    final FileType fileType, final String stack, final boolean dualAxis) {
    if (fileType != null) {
      return fileType.getFileName(manager, DatasetTool.getDatasetName(stack, dualAxis),
        dualAxis ? AxisType.DUAL_AXIS : AxisType.SINGLE_AXIS, null);
    }
    return null;
  }
}
