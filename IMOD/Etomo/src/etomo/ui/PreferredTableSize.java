package etomo.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* <p>Description: Used for storing and comparing the preferred size of table
* components.</p>
* 
* <p>Copyright: Copyright 2014</p>
*
* <p>Organization:
* Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
* University of Colorado</p>
* 
* @author $Author$
* 
* @version $Revision$
* 
* <p> $Log$ </p>
*/
public final class PreferredTableSize {
  public static final String rcsid = "$Id:$";

  private final Column[] columnList;

  public PreferredTableSize(final int numColumns) {
    if (numColumns > 0) {
      columnList = new Column[numColumns];
      for (int i = 0; i < columnList.length; i++) {
        columnList[i] = new Column();
      }
    }
    else {
      columnList = null;
    }
  }

  public void addColumn(final int index, final TableComponent component) {
    if (component != null && columnList != null && index >= 0
        && index < columnList.length) {
      columnList[index].add(component);
    }
  }

  public void addColumn(final int index, final TableComponent component1,
      final TableComponent component2) {
    if (columnList != null && index >= 0 && index < columnList.length) {
      if (component1 != null && component2 != null) {
        columnList[index].add(component1, component2);
      }
      else if (component1 != null) {
        columnList[index].add(component1);
      }
      else if (component2 != null) {
        columnList[index].add(component2);
      }
    }
  }

  /**
   * Returns the sum of the widest component or group of components in each column.
   * @return
   */
  public int getPreferredWidth() {
    if (columnList != null) {
      int width = 0;
      for (int i = 0; i < columnList.length; i++) {
        width += columnList[i].getPreferredWidth();
      }
      return width;
    }
    return 0;
  }

  private static final class Column {
    private final List<TableComponent> list = new ArrayList<TableComponent>();

    private Column() {
    }

    private void add(final TableComponent component) {
      list.add(component);
    }

    private void add(final TableComponent component1, final TableComponent component2) {
      ComponentList componentList = new ComponentList();
      componentList.add(component1);
      componentList.add(component2);
      list.add(componentList);
    }

    /**
     * Returns the width of the widest component or group of components in this column.
     * @return
     */
    private int getPreferredWidth() {
      int width = 0;
      Iterator<TableComponent> iterator = list.iterator();
      while (iterator.hasNext()) {
        width = Math.max(width, iterator.next().getPreferredWidth());
      }
      return width;
    }
  }

  private static final class ComponentList implements TableComponent {
    private final List<TableComponent> list = new ArrayList<TableComponent>();

    private ComponentList() {
    }

    private void add(final TableComponent component) {
      list.add(component);
    }

    /**
     * Returns the sum of the preferred width of all the components
     */
    public int getPreferredWidth() {
      int width = 0;
      Iterator<TableComponent> iterator = list.iterator();
      while (iterator.hasNext()) {
        width += iterator.next().getPreferredWidth();
      }
      return width;
    }
  }
}
