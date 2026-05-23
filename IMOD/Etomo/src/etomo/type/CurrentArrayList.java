package etomo.type;

import java.util.ArrayList;

/**
* <p>Description: An array list with some state information:  a current index and another
* integer (total).  The index points to the first element, unless it is incremented.  The
* total variable defaults to zero.</p>
* 
* <p>Copyright: Copyright 2015 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public final class CurrentArrayList<E> extends ArrayList<E> {
  private int index = -1;
  // May be set equal to the size of a subset of elemnts - or anything else. No controls
  // on this variable - just part of the bundle.
  private int total = 0;

  public CurrentArrayList() {
    super();
  }

  public CurrentArrayList(CurrentArrayList<E> input) {
    super(input);
    index = input.index;
    total = input.total;
  }

  /**
   * Gets the element that the index is pointing to.  Returns null if the array is
   * empty, or the index isn't valid.
   */
  public E getCurrent() {
    if (isEmpty() || index < 0 || index >= size()) {
      return null;
    }
    return get(index);
  }

  public void next() {
    index++;
  }

  public void previous() {
    index--;
  }

  public int getIndex() {
    return index;
  }

  /**
   * Copies the state of input, up to and including the current element.
   * After this function is called, getCurrent will return the last element.  Does not
   * deep copy the elements.
   * @param input - from instance.
   */
  public void copyToCurrent(final CurrentArrayList<E> input) {
    if (input == null) {
      clear();
      index = -1;
      total = 0;
      return;
    }
    index = input.index;
    total = input.total;
    int size = Math.min(input.index + 1, input.size());
    for (int i = 0; i < size; i++) {
      add(input.get(i));
    }
  }

  public void incrementTotal() {
    total++;
  }

  public int getTotal() {
    return total;
  }
}
