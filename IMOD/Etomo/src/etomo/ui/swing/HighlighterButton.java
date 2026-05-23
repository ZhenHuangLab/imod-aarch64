package etomo.ui.swing;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import etomo.util.Utilities;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2006 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.3  2009/10/15 23:32:11  sueh
 * <p> bug# 1274 Added setHeaders to allow non-automatic naming.
 * <p>
 * <p> Revision 1.2  2007/03/27 00:03:25  sueh
 * <p> bug# 964 Automatically added tooltip.  Made class more thread-safe.
 * <p>
 * <p> Revision 1.1  2007/03/01 01:38:00  sueh
 * <p> bug# 964 Wraps a toggle HeaderCell button.  When pressed in runs the highlight
 * <p> function in parent and group.  Also tells the other highlighter buttons with the
 * <p> same group to turn off their highlight.
 * <p> </p>
 */
final class HighlighterButton {
  private static final HashedLists groupLists = new HashedLists();

  private final Highlightable parent;
  private final Highlightable group;
  private final HeaderCell cell;

  /**
   * Lazy constructor
   * @param parent
   * @param group
   */
  private HighlighterButton(final Highlightable parent, final Highlightable group) {
    this.parent = parent;
    this.group = group;
    // group
    if (group != null) {
      groupLists.put(group, this);
    }
    // button
    cell = HeaderCell.getToggleInstance("=>",
      (int) (40 * UIParameters.getInstance().getFontSizeAdjustment()));
    cell.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    cell.setEnabled(true);
    cell.addActionListener(new HBActionListener(this));
    cell.setFocusable(true);
    setToolTipText();
  }

  static HighlighterButton getInstance(final Highlightable parent,
    final Highlightable group) {
    HighlighterButton instance = new HighlighterButton(parent, group);
    instance.addListeners();
    return instance;
  }

  void setHeaders(String tableHeader, HeaderCell rowHeader, HeaderCell columnHeader) {
    cell.setTableHeader(tableHeader);
    cell.setRowHeader(rowHeader);
    cell.setColumnHeader(columnHeader);
    cell.setName();
    rowHeader.addChild(cell);
    columnHeader.addChild(cell);
  }

  boolean isHighlighted() {
    return cell.isSelected();
  }

  int getWidth() {
    return cell.getWidth();
  }

  void setToolTipText(final String text) {
    cell.setToolTipText(text);
  }

  void add(final JPanel panel, final GridBagLayout layout,
    final GridBagConstraints constraints) {
    double oldWeightx = constraints.weightx;
    constraints.weightx = 0.0;
    cell.add(panel, layout, constraints);
    constraints.weightx = oldWeightx;
  }

  void remove() {
    cell.remove();
  }

  void setSelected(final boolean select) {
    cell.setSelected(select);
    action();
  }

  private void action() {
    boolean highlight = cell.isSelected();
    parent.highlight(highlight);
    if (group == null) {
      return;
    }
    // If turning on the highlight, all other highlighters in the group must be
    // turned off
    ArrayList list = groupLists.get(group);
    if (list == null) {
      throw new IllegalStateException("Should be in the list.  group=" + group);
    }
    for (int i = 0; i < list.size(); i++) {
      HighlighterButton highlighterButton = (HighlighterButton) list.get(i);
      if (highlighterButton != this) {
        highlighterButton.turnOffHighlight();
      }
    }
    // The group may also need to respond to the highlight
    group.highlight(highlight);
  }

  final Component getComponent() {
    return cell.getComponent();
  }

  void setForeground() {}

  private void setToolTipText() {
    String mask;
    if (Utilities.isMacOS()) {
      mask = "[alt option]";
    }
    else {
      mask = "[Alt]";
    }
    cell.setToolTipText("Press to highlight row.  Hotkeys: " + mask + "+[Up_Arrow] and "
      + mask + "+[Down_Arrow]");
  }

  private void addListeners() {
    cell.addActionListener(new HBActionListener(this));
  }

  private void turnOffHighlight() {
    cell.setSelected(false);
    parent.highlight(false);
  }

  private static final class HBActionListener implements ActionListener {
    private final HighlighterButton highlighterButton;

    HBActionListener(final HighlighterButton highlighterButton) {
      this.highlighterButton = highlighterButton;
    }

    public void actionPerformed(ActionEvent actionEvent) {
      highlighterButton.action();
    }
  }

  private static final class HashedLists {
    private final Hashtable hash = new Hashtable();

    void put(final Object groupKey, final HighlighterButton element) {
      ArrayList list = (ArrayList) hash.get(groupKey);
      if (list == null) {
        list = new ArrayList();
        list.add(element);
        hash.put(groupKey, list);
      }
      else {
        list.add(element);
      }
    }

    ArrayList get(final Object groupKey) {
      return (ArrayList) hash.get(groupKey);
    }
  }
}
