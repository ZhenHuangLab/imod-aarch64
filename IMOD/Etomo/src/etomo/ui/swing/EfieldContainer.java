package etomo.ui.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

final class EfieldContainer {
  private static final Insets FIELD_INSETS = new Insets(0, 0, 0, -1);
  private static final Insets COMPONENT_INSETS = new Insets(0, 0, 0, -1);

  private final GridBagLayout gridBaglayout;
  private final GridBagConstraints constraints;
  private final boolean useGridBag;

  private Container root = null;
  private boolean lock = false;

  /**
   * If label and/or fieldControl are present create a JPanel and add to it in order: label,
   * field, fieldControl.  If only field is present then no JPanel is created and the
   * field is returned in getComponent.
   * @param modifiable - field components will be added after the field is placed in a panel
   * @param useGridBag - format field with grid bag layout
   * @param field - required
   * @param label - optional
   * @param fieldControl - optional
   */
  EfieldContainer(final boolean modifiable, final boolean useGridBag,
    final Component label, final Container field, final Component fieldControl) {
    this.useGridBag = useGridBag;
    if (useGridBag) {
      gridBaglayout = new GridBagLayout();
      constraints = new GridBagConstraints();
    }
    else {
      gridBaglayout = null;
      constraints = null;
    }
    if (!modifiable && label == null && fieldControl == null) {
      root = field;
    }
    else {
      createPanel();
      // label
      if (label != null) {
        if (useGridBag) {
          gridBaglayout.setConstraints(label, constraints);
        }
        root.add(label);
      }
      // field
      if (useGridBag) {
        constraints.insets = FIELD_INSETS;
        gridBaglayout.setConstraints(field, constraints);
      }
      root.add(field);
      // fieldControl
      if (fieldControl != null) {
        if (useGridBag) {
          gridBaglayout.setConstraints(fieldControl, constraints);
        }
        root.add(fieldControl);
      }
    }
  }

  /**
   * Add a component.  Creates a JPanel to hold field and components if necessary.
   * @param component
   */
  void add(final Component component) {
    if (component == null) {
      return;
    }
    if (lock) {
      throw new IllegalStateException(
        "ERROR:  Serious Software Error.  Attempting to add component "
          + component.toString()
          + " to a non-existent container.  A container cannot be created for the "
          + root.toString()
          + " field because the field has already been placed in a panel.");
    }
    if (!(root instanceof JPanel)) {
      // Did not need a JPanel until now. Set root to a new JPanel and add the field to
      // it.
      Container field = root;
      createPanel();
      // field
      if (useGridBag) {
        constraints.insets = FIELD_INSETS;
        gridBaglayout.setConstraints(field, constraints);
      }
      root.add(field);
      // component
      if (useGridBag) {
        constraints.insets = COMPONENT_INSETS;
        gridBaglayout.setConstraints(component, constraints);
      }
    }
    root.add(component);
  }

  private void createPanel() {
    root = new JPanel();
    if (useGridBag) {
      root.setLayout(gridBaglayout);
      constraints.fill = GridBagConstraints.BOTH;
      constraints.weightx = 0.0;
      constraints.weighty = 0.0;
      constraints.gridheight = 1;
      constraints.gridwidth = 1;
    }
    else {
      root.setLayout(new BoxLayout(root, BoxLayout.X_AXIS));
    }
  }

  Component getComponent() {
    if (!lock && !(root instanceof JPanel)) {
      lock = true;
    }
    return root;
  }
}