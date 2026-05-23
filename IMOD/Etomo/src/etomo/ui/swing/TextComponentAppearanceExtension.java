package etomo.ui.swing;

import javax.swing.text.JTextComponent;

/**
 * <p>Description: An extendion of AppearanceExtension which uses
 * JTextComponent.setEditable().</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class TextComponentAppearanceExtension extends AppearanceExtension {
  /**
   * @param textComponent - required
   * @param enabledField
   * @param editableComponent
   */
  TextComponentAppearanceExtension(final JTextComponent textComponent,
    final boolean enabledField, final boolean editableComponent) {
    super(textComponent, enabledField, editableComponent, textComponent.isEditable());
  }

  /**
   * Make the component editable or ineditable.
   */
  @Override
  void setComponentEditable(final boolean editable) {
    // Ineditable components are never editable, even when the field is editable.
    if (!editable || editableComponent) {
      ((JTextComponent) component).setEditable(editable);
    }
  }

  @Override
  boolean isNativeSetEditable() {
    return true;
  }
}