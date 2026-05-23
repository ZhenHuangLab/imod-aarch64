package etomo.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import etomo.type.Option;

/**
 * <p>Description: Creates a list of value/description pairs.  Will not contain any
 * empty entries, but may contain entries without a description.</p>
 */
public final class DirectiveDescrChoiceList {
  private final List<Option> choiceList = new ArrayList<Option>();

  private DirectiveDescrChoiceList(final String[] array) {
    for (int i = 0; i < array.length; i++) {
      if (array[i] != null && array[i].length() > 0) {
        String[] pair = array[i].split("\\s*\\:\\s*");
        if (pair != null && pair.length >= 0) {
          choiceList.add(new Option(pair));
        }
      }
    }
  }

  public String toString() {
    return "[" + choiceList + "]";
  }

  static DirectiveDescrChoiceList getInstance(final String choices) {
    if (choices == null) {
      return null;
    }
    String[] array = choices.split("\\s*;\\s*");
    if (array == null || array.length == 0) {
      return null;
    }
    return new DirectiveDescrChoiceList(array);
  }

  boolean isEmpty() {
    return choiceList.isEmpty();
  }

  public Iterator<Option> iterator() {
    return choiceList.iterator();
  }

  public int size() {
    return choiceList.size();
  }

  /**
   * If the index is valid, returns the choice description.  If the decription is
   * missing, returns the choice number.  If the index is not valid, returns null.
   * @param index
   * @return
   */
  public String getDescr(final int index) {
    if (index >= 0 && index < choiceList.size()) {
      return choiceList.get(index).getDisplayString();
    }
    return null;
  }

  /**
   * If the index is valid, returns the value.  If the index is not valid, returns null.
   * @param index
   * @return
   */
  public String getValue(final int index) {
    if (index >= 0 && index < choiceList.size()) {
      return choiceList.get(index).getValue();
    }
    return null;
  }
}