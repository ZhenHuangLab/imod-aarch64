package etomo.storage;

import etomo.storage.autodoc.AutodocTokenizer;
import etomo.type.DirectiveFileType;

public final class DirectiveDescrElement implements DirectiveDescr {
  private static final String COMMENT = "#";
  private static final String HEADER_STRING = "Directives";
  private static final int NAME_COLUMN_INDEX = 0;
  private static final int DESCR_COLUMN_INDEX = 1;
  private static final int VALUE_TYPE_COLUMN_INDEX = 2;
  private static final int BATCH_COLUMN_INDEX = 3;
  private static final int TEMPLATE_COLUMN_INDEX = 4;
  private static final int ETOMO_COLUMN_INDEX = 5;
  private static final int NOTE_COLUMN_INDEX = 6;
  private static final int LABEL_COLUMN_INDEX = 7;
  private static final int CHOICES_COLUMN_INDEX = 8;

  private String[] lineArray = null;

  DirectiveDescrElement() {}

  public DirectiveDescrElement(final String[] lineArray) {
    this.lineArray = lineArray;
  }

  String[] getLineArray() {
    return lineArray;
  }

  public String toString() {
    if (lineArray != null && lineArray.length > 0) {
      return "[" + lineArray[0] + "]";
    }
    return "[]";
  }

  void setLineArray(final String[] lineArray) {
    this.lineArray = lineArray;
  }

  public static DirectiveDef getDirectiveDef(final String[] lineArray,
    final DirectiveDef prevDirectiveDef) {
    if (isDirective(lineArray)) {
      return DirectiveDef.getInstanceFromCsv(lineArray[NAME_COLUMN_INDEX],
        prevDirectiveDef);
    }
    return null;
  }

  public static String getNote(final String[] lineArray) {
    if (lineArray != null && lineArray.length > NOTE_COLUMN_INDEX) {
      return lineArray[NOTE_COLUMN_INDEX];
    }
    return null;
  }

  public static String getDescription(final String[] lineArray) {
    if (lineArray != null && lineArray.length > DESCR_COLUMN_INDEX) {
      return lineArray[DESCR_COLUMN_INDEX];
    }
    return null;
  }

  public String getDescription() {
    return getDescription(lineArray);
  }

  public static boolean isLabel(final String[] lineArray) {
    return lineArray != null && lineArray.length > LABEL_COLUMN_INDEX
      && lineArray[LABEL_COLUMN_INDEX] != null
      && lineArray[LABEL_COLUMN_INDEX].length() > 0;
  }

  public static String getLabel(final String[] lineArray) {
    if (isLabel(lineArray)) {
      return lineArray[LABEL_COLUMN_INDEX];
    }
    else {
      return null;
    }
  }

  public String getLabel() {
    if (lineArray != null && lineArray.length > LABEL_COLUMN_INDEX) {
      return lineArray[LABEL_COLUMN_INDEX];
    }
    return null;
  }

  public static boolean isChoiceList(final String[] lineArray) {
    return lineArray != null && lineArray.length > CHOICES_COLUMN_INDEX
      && lineArray[CHOICES_COLUMN_INDEX] != null
      && lineArray[CHOICES_COLUMN_INDEX].length() > 0;
  }

  public static DirectiveDescrChoiceList getChoiceList(final String[] lineArray) {
    if (isChoiceList(lineArray)) {
      return DirectiveDescrChoiceList.getInstance(lineArray[CHOICES_COLUMN_INDEX]);
    }
    return null;
  }

  public DirectiveDescrChoiceList getChoiceList() {
    return getChoiceList(lineArray);
  }

  public static DirectiveDescrEtomoColumn getEtomoColumn(final String[] lineArray) {
    if (lineArray != null && lineArray.length > ETOMO_COLUMN_INDEX) {
      return DirectiveDescrEtomoColumn.getInstance(lineArray[ETOMO_COLUMN_INDEX]);
    }
    return null;
  }

  public DirectiveDescrEtomoColumn getEtomoColumn() {
    return getEtomoColumn(lineArray);
  }

  public static String getName(final String[] lineArray) {
    if (lineArray != null && lineArray.length > NAME_COLUMN_INDEX) {
      return lineArray[NAME_COLUMN_INDEX];
    }
    return null;
  }

  public String getName() {
    return getName(lineArray);
  }

  public String getSectionHeader() {
    return getSectionHeader(lineArray);
  }

  public static String getSectionHeader(String[] lineArray) {
    if (lineArray != null && lineArray.length > NAME_COLUMN_INDEX) {
      return lineArray[NAME_COLUMN_INDEX];
    }
    return null;
  }

  public static DirectiveValueType getValueType(String[] lineArray) {
    if (lineArray != null && lineArray.length > VALUE_TYPE_COLUMN_INDEX) {
      return DirectiveValueType.getInstance(lineArray[VALUE_TYPE_COLUMN_INDEX]);
    }
    return DirectiveValueType.UNKNOWN;
  }

  public DirectiveValueType getValueType() {
    return getValueType(lineArray);
  }

  public static boolean isIncluded(final String[] lineArray,
    final DirectiveFileType directiveFileType) {
    if (directiveFileType == null) {
      return true;
    }
    if (directiveFileType.isBatch() && isBatch(lineArray)) {
      return true;
    }
    if (directiveFileType.isTemplate() && isTemplate(lineArray)) {
      return true;
    }
    return false;
  }

  public static boolean isBatch(final String[] lineArray) {
    if (lineArray != null && lineArray.length > BATCH_COLUMN_INDEX) {
      return lineArray[BATCH_COLUMN_INDEX].compareToIgnoreCase("Y") == 0;
    }
    return false;
  }

  public boolean isBatch() {
    return isBatch(lineArray);
  }

  public boolean isDirective() {
    return isDirective(lineArray);
  }

  public static boolean isDirective(final String[] lineArray) {
    return lineArray != null && lineArray.length >= 3
      && lineArray[NAME_COLUMN_INDEX].indexOf(AutodocTokenizer.SEPARATOR_CHAR) != -1;
  }

  public boolean isSection() {
    return isSection(lineArray);
  }

  public static boolean isSection(final String[] lineArray) {
    return lineArray != null && lineArray.length == 1 && !lineArray[0].startsWith(COMMENT)
      && lineArray[0].indexOf(HEADER_STRING) == -1;
  }

  public static boolean isTemplate(final String[] lineArray) {
    if (lineArray != null && lineArray.length > TEMPLATE_COLUMN_INDEX) {
      return lineArray[TEMPLATE_COLUMN_INDEX].compareToIgnoreCase("Y") == 0;
    }
    return false;
  }

  public boolean isTemplate() {
    return isTemplate(lineArray);
  }
}
