package etomo.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import etomo.logic.ComparisonStrategy;
import etomo.logic.StrategyDoesNotApplyException;
import etomo.storage.autodoc.Autodoc;
import etomo.storage.autodoc.AutodocTokenizer;
import etomo.storage.autodoc.Statement;
import etomo.storage.autodoc.StatementLocation;

/**
 * <p>Description: List of name/value pairs.  The list is loaded from the global autodoc
 * statements.  It can be merged, subtracted from, and printed to a file.  Not thread
 * safe.  Will only print the default autodoc delimiter.</p>
 * 
 * <pre>
 * Merging:  each entry in the merge list where the name does not exist in the instance's
 * list will be added to the instance's list.  The merge list will not change.
 * 
 * Subtraction:  an entry in the subtraction list that is identical (name and value) to an
 * entry in the instance's list will cause the entry in the instance's list to be removed.
 * The subtraction list will not change.
 * 
 * Handling duplicates during construction:  the first entry is clobbered by the next
 * with the same name.  Only one entry is printed per name.</pre>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class NameValuePairList {
  private final List<Pair> list = new ArrayList<Pair>();
  private final Map<String, Pair> map = new HashMap<String, Pair>();

  public NameValuePairList(final Autodoc autodoc) {
    StatementLocation loc = autodoc.getStatementLocation();
    Statement statement = null;
    // load global autodoc statements
    while ((statement = autodoc.nextStatement(loc)) != null) {
      if (statement.getType() != Statement.Type.NAME_VALUE_PAIR) {
        continue;
      }
      Pair pair = new Pair(statement.getLeftSide(), statement.getRightSide());
      if (map.containsKey(pair.name)) {
        // Clobber entry with a preexisting name.
        list.remove(pair);
      }
      // Add entry
      list.add(pair);
      map.put(pair.name, pair);
    }
  }

  /**
   * Copy constructor
   * @param input
   */
  public NameValuePairList(final NameValuePairList input) {
    int len = input.list.size();
    for (int i = 0; i < len; i++) {
      // Pairs can be shared because they are never modified.
      Pair pair = input.list.get(i);
      list.add(pair);
      map.put(pair.name, pair);
    }
  }

  public String toString() {
    return "[list:" + list + "]";
  }

  /**
   * Add pairs to this instance from merge, where the name is not in this instance.
   * @param merge
   */
  public void merge(final NameValuePairList merge) {
    if (merge == null) {
      return;
    }
    int len = merge.list.size();
    for (int i = 0; i < len; i++) {
      Pair mergePair = merge.list.get(i);
      if (!map.containsKey(mergePair.name)) {
        // pair doesn't exist in instance - add it
        list.add(mergePair);
        map.put(mergePair.name, mergePair);
      }
    }
  }

  /**
   * Remove sub pairs from this instance, when the name and the value are the same.
   * @param sub
   */
  public void subtract(final NameValuePairList sub, final ComparisonStrategy strategy) {
    if (sub == null) {
      return;
    }
    int len = sub.list.size();
    for (int i = 0; i < len; i++) {
      Pair subPair = sub.list.get(i);
      Pair pair = map.get(subPair.name);
      if (pair != null && pair.equalsValue(subPair, strategy)) {
        // identical - remove it
        list.remove(pair);
        map.remove(pair.name);
      }
    }
  }

  /**
   * Removes pairs from this instance where the left side matches an element in
   * directiveDefSet.
   * @param directiveDefSet
   */
  public void subtract(final Set<DirectiveDef> directiveDefSet) {
    if (directiveDefSet == null) {
      return;
    }
    // Go through the list backwards and strip off any pair that matches.
    int len = list.size();
    for (int i = len - 1; i >= 0; i--) {
      Pair pair = list.get(i);
      if (pair != null && directiveDefSet.contains(DirectiveDef.getInstance(pair.name))) {
        list.remove(i);
        map.remove(pair.name);
      }
    }
  }

  /**
   * Write list to a file.
   * @param logFile
   */
  public void write(final LogFile logFile) {
    LogFile.WriterId id = null;
    try {
      id = logFile.openWriter();
      int len = list.size();
      for (int i = 0; i < len; i++) {
        Pair pair = list.get(i);
        if (pair.value == null) {
          logFile.write(pair.name + " " + AutodocTokenizer.DEFAULT_DELIMITER + " ", id);
        }
        else {
          logFile.write(
            pair.name + " " + AutodocTokenizer.DEFAULT_DELIMITER + " " + pair.value, id);
        }
        logFile.newLine(id);
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    logFile.closeWriter(id);
  }

  int size() {
    return list.size();
  }

  Pair get(final int index) {
    return list.get(index);
  }

  Pair get(final String name) {
    return map.get(name);
  }

  /**
   * Name/value pair.
   */
  static final class Pair {
    final String name;// required
    final String value;// may be null

    private Pair(final String name, final String value) {
      this.name = name;
      this.value = value;
    }

    public String toString() {
      return "[name:" + name + ",value:" + value + "]";
    }

    /**
     * True if values are equal, or are both null.
     * @param pair
     * @param strategy (optional) changes the comparison algorithm
     * @return true if value and pair.value are equal
     */
    private boolean equalsValue(final Pair pair, final ComparisonStrategy strategy) {
      if (strategy != null) {
        try {
          return strategy.equals(value, pair.value);
        }
        catch (StrategyDoesNotApplyException e) {}
      }
      return (value == null && pair.value == null)
        || (value != null && value.equals(pair.value));
    }

    String getName() {
      return name;
    }

    String getValue() {
      return value;
    }
  }
}
