package etomo.ui.swing;

import javax.swing.AbstractButton;
import javax.swing.SwingConstants;

import etomo.ui.FlagType;

/**
 * <p>Description: A class to handle the appearance of buttons.  Contains CompleteIcons
 * corresponding to different types of flags.
 * Specifically for handling icons, but setup may be overridden to modify the button in
 * other ways (such as size, background, or boundary).
 * For foreground text changes, and editable and enabled settings use AppearanceExtension.
 * </p>
 * 
 * <p>Statelessness:This class is designed to be inherited by singletons which can be used
 * to give a large number of the same type of button the same style.  Do not add any state
 * information related to a button instance to a singleton.</p>
 * 
 * <p>Use:  Pass an instance into the button field's constructor and call setup to
 * immediately give the button a style.  For buttons that can not toggle, this class can
 * be used to make them appear to toggle when setSelectedAppearance is used.  This class
 * can be inherited by a stateless singleton to avoid constructing a style for each button
 * instance.
 * 
 * <p>Modifications:  The abstract limitation can be removed from this class if necessary.
 * Final can be removed from setSelectedAppearance.
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
abstract class ButtonStyleExtension {
  private final boolean textGap;
  private final CompleteIcon icon;
  private final CompleteIcon templateIcon;
  private final CompleteIcon errorIcon;

  /**
   * All CompleteIcon parameters may be set to null.
   * @param textGap
   * @param icon
   * @param templateIcon
   * @param errorIcon
   */
  ButtonStyleExtension(final boolean textGap, final CompleteIcon icon,
    final CompleteIcon templateIcon, final CompleteIcon errorIcon) {
    this.textGap = textGap;
    this.icon = icon;
    this.templateIcon = templateIcon;
    this.errorIcon = errorIcon;
  }

  /**
   * Call this function to make the button conform to its style.
   * @param button
   * @param label
   */
  void setup(final AbstractButton button, final String label) {
    if (button == null) {
      return;
    }
    if (icon != null) {
      icon.setup(button);
    }
    if (textGap && label != null && (icon != null || templateIcon != null)) {
      button.setIconTextGap(10);
      button.setHorizontalAlignment(SwingConstants.LEFT);
    }
  }

  /**
   * Sets the appearance.  Can create a appearance of a toggle button.  Can change the
   * icon in response to the flagType.
   * @param button
   * @param flagType
   * @param implementToggle
   * @param selected
   */
  public final void updateAppearance(final AbstractButton button, final FlagType flagType,
    final boolean implementToggle, final boolean selected) {
    CompleteIcon curIcon = null;
    if (flagType != null) {
      if (flagType.isTemplate()) {
        curIcon = templateIcon;
      }
      else if (flagType.isError()) {
        curIcon = errorIcon;
      }
    }
    if (curIcon == null) {
      curIcon = icon;
    }
    if (curIcon != null) {
      curIcon.setup(button);
      if (implementToggle) {
        curIcon.setSelectedAppearance(button, selected);
      }
    }
  }
}