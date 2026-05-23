package etomo.ui.swing;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

final class GridBagExtension {
  private JPanel parent = null;

  GridBagExtension() {}

  void add(final Component field, final JPanel panel, final GridBagLayout layout,
    final GridBagConstraints constraints) {
    layout.setConstraints(field, constraints);
    panel.add(field);
    parent = panel;
  }

  void remove(final Component field) {
    if (parent != null) {
      parent.remove(field);
      parent = null;
    }
  }
}
