package etomo.ui.swing;

import java.awt.Color;
import java.awt.Component;

import etomo.ui.FlagDisplay;
import etomo.ui.FlagType;

/**
 * <p>Description: Flag display for controlling the foreground.  Handles enabled and
 * editable settings.  Adds an editable setting to a Component (but ineditable still looks
 * like desabled).</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public class AppearanceExtension implements FlagDisplay {
  final Component component;
  private final Color origForeground;
  private final boolean enabledField;
  final boolean editableComponent;

  private boolean enabled = true;
  private boolean editable = true;
  private FlagType flagType = null;
  private boolean allowFlagEditableControl = true;
  private boolean allowForegroundChangeOnError = true;
  private boolean respondToNonErrorFlags = true;
  private boolean debug = false;
  // Can enable a field that can't be enabled.
  private ControlState enableControlState = null;
  private Controller[] childControllers = null;

  /**
   * 
   * @param component - required
   */
  AppearanceExtension(final Component component) {
    this(component, true, true, true);
  }

  /**
   * 
   * @param component - required
   * @param enabledField
   * @param editableComponent
   * @param editable
   */
  AppearanceExtension(final Component component, final boolean enabledField,
    final boolean editableComponent, final boolean editable) {
    this.component = component;
    this.enabledField = enabledField;
    this.editableComponent = editableComponent;
    this.editable = editable;
    Color color = component.getForeground();
    if (color != null) {
      origForeground = color;
    }
    else {
      origForeground = Color.BLACK;
    }
    // Get the field enabled setting from the component
    enabled = component.isEnabled();
    if (!enabledField) {
      // Disable the field
      setEnabled(false);
    }
    if (!editableComponent) {
      // Make the component ineditable
      setComponentEditable(false);
    }
    // Get the field editable setting from the parameter
    setEditable(editable);
  }

  final void setChildControllers(final Controller[] childControllers) {
    this.childControllers = childControllers;
    if (childControllers != null) {
      for (int i = 0; i < childControllers.length; i++) {
        if (childControllers[i] != null) {
          childControllers[i].setEditable(editable);
          childControllers[i].setEnabled(enabled);
        }
      }
    }
  }

  final void setAllowFlagEditableControl(final boolean allow) {
    allowFlagEditableControl = allow;
  }

  final void setAllowForegroundChangeOnError(final boolean allow) {
    allowForegroundChangeOnError = allow;
  }

  final void setRespondToNonErrorFlags(final boolean respond) {
    respondToNonErrorFlags = respond;
  }

  final void setDebug(final boolean debug) {
    this.debug = debug;
  }

  final FlagType getFlagType() {
    return flagType;
  }

  public final void setFlag(FlagType flagType) {
    if (!respondToNonErrorFlags && flagType != null && !flagType.isError()) {
      // Respond to non-error flags by turning off the flag.
      flagType = null;
    }
    if (this.flagType == flagType) {
      return;
    }
    this.flagType = flagType;
    setForeground();
  }

  /**
   * Inherit and return true for componets which have a setEditable function that should
   * be used.
   * @return
   */
  boolean isNativeSetEditable() {
    return false;
  }

  /**
   * Make a field editable or ineditable.
   * @param editable
   */
  final void setEditable(final boolean editable) {
    // Ineditable components are never editable but the entire field (including file and
    // clear buttons) can be editable. So editableComponent is ignored here.
    if (this.editable == editable) {
      return;
    }
    this.editable = editable;
    setComponentEditable(editable);
    setForeground();
    if (childControllers != null) {
      for (int i = 0; i < childControllers.length; i++) {
        if (childControllers[i] != null) {
          childControllers[i].setEditable(editable);
        }
      }
    }
  }

  /**
   * Make the component enabled or disabled.
   * @param editable
   */
  void setComponentEditable(final boolean editable) {
    // Using the component's enable/disable function to show editability as well as
    // enableness.
    // Ineditable components are never editable, even when the field is editable.
    if (!editable || editableComponent) {
      component.setEnabled(editable);
    }
  }

  final void setEnableControlState(final ControlState enableControlState) {
    this.enableControlState = enableControlState;
    setEnabled(enabled);
  }

  /**
   * Make a field enabled or disabled.
   * @param enabled
   */
  final void setEnabled(boolean enabled) {
    // ControlState.ENABLE keeps the component enabled, even if its a disabled field
    if (enableControlState == ControlState.ENABLE) {
      enabled = true;
    }
    else if (!enabledField) {
      // Prevent a disabled field from being enabled.
      enabled = false;
    }
    if (this.enabled == enabled) {
      return;
    }
    this.enabled = enabled;
    // If this component doesn't have a native editable setting, then component's
    // enabled/disabled functionality is being used to show both states. In this case
    // don't call the components's setEnabled function with TRUE while the field is
    // ineditable or the component is not allowed to be editable.
    if (!enabled || isNativeSetEditable() || (editable && editableComponent)) {
      component.setEnabled(enabled);
    }
    setForeground();
    if (childControllers != null) {
      for (int i = 0; i < childControllers.length; i++) {
        if (childControllers[i] != null) {
          childControllers[i].setEnabled(enabled);
        }
      }
    }
  }

  /**
   * Set the foreground.  This will have no effect on a component that has been disabled.
   * This class is not currently set up to handle background changes.
   */
  final void setForeground() {
    if (flagType != null && !flagType.background
      && (allowForegroundChangeOnError || !flagType.isError())) {
      component.setForeground(flagType.color);
    }
    else {
      component.setForeground(origForeground);
    }
  }

  final boolean isEditable() {
    return editable;
  }

  final boolean isEnabled() {
    return enabled;
  }
}