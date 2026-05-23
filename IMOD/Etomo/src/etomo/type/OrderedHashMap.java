package etomo.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description: Map which also contains an optional array which may be used to
 * order the values with a separate ordinal that functions as an index.  A value with an
 * identical ordinal will replace the existing element in the array list.</p>
 * <p/>
 * <p>Copyright: Copyright 2014 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class OrderedHashMap<K, V> {
  private final Map<K, V> map = new HashMap<K, V>();

  private Array<V> array = null;

  /**
   * Clears the map and the array.
   */
  public void clear() {
    map.clear();
    if (array != null) {
      array.clear();
    }
  }

  /**
   * Add to map without adding to the array.  The array should be considered
   * invalid after this function has been called, and before clear() has been called.
   * @param key
   * @param value
   */
  public void put(final K key, final V value) {
    map.put(key, value);
  }

  /**
   * Adds an element and also placed the element in the array to create an ordered
   * list.
   * @param ordinal
   * @param key
   * @param value
   */
  public void put(final int ordinal, final K key, final V value) {
    map.put(key, value);
    if (array == null) {
      array = new Array<V>();
    }
    array.add(ordinal, value);
  }

  public V get(final String key) {
    return map.get(key);
  }

  /**
   * Retrieves all values in an unspecified order.
   * @return
   */
  public Collection<V> values() {
    return map.values();
  }

  /**
   * Retrieves all values in order by ordinal.  Will return null if the ordinal was never
   * used.
   * @return
   */
  public ReadOnlyArray<V> orderedValues() {
    return array;
  }

  private static final class Array<V> implements ReadOnlyArray {
    private final ArrayList<V> array = new ArrayList<V>();

    private void clear() {
      array.clear();
    }

    private void add(final int ordinal, final V value) {
      if (array.size() <= ordinal) {
        for (int i = array.size(); i < ordinal + 1; i++) {
          array.add(i, null);
        }
      }
      // Add(int,V) doesn't work like a primative array, it shoves existing elements
      // forwards to make a new space. Remove whatever is in the location of the ordinal
      // so as to create an ordered list.
      array.remove(ordinal);
      array.add(ordinal, value);
    }

    public int size() {
      return array.size();
    }

    public V get(int index) {
      return array.get(index);
    }
  }

  public static interface ReadOnlyArray<V> {
    public int size();

    public V get(int index);
  }
}