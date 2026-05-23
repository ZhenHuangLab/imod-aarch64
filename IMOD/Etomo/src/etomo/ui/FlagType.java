package etomo.ui;

import java.awt.Color;

import etomo.ui.swing.Colors;
import etomo.ui.swing.ProcessControlPanel;

/**
 * <p>Description: Types of flags.</p>
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class FlagType {
  public static final FlagType TEMPLATE = new FlagType(Colors.FIELD_HIGHLIGHT, false);
  public static final FlagType TEMPLATE_ERROR =
    new FlagType(ProcessControlPanel.colorNotStarted, false);
  public static final FlagType ERROR =
    new FlagType(ProcessControlPanel.colorNotStarted, false);
  public static final FlagType WARNING = new FlagType(Colors.WARNING_BACKGROUND, true);

  public final Color color;
  public final boolean background;

  private FlagType(final Color color, final boolean background) {
    this.color = color;
    this.background = background;
  }

  public boolean isTemplate() {
    return this == TEMPLATE;
  }

  public boolean isError() {
    return this == ERROR || this == TEMPLATE_ERROR;
  }

  public String toString() {
    if (this == TEMPLATE) {
      return "TEMPLATE";
    }
    if (this == TEMPLATE_ERROR) {
      return "TEMPLATE_ERROR";
    }
    if (this == ERROR) {
      return "ERROR";
    }
    if (this == WARNING) {
      return "WARNING";
    }
    return super.toString();
  }
}