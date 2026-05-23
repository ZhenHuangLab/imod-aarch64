package etomo.ui;

import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Description: Chooses a flag type and informs flag displays.</p>
 * 
 * @see Replaced by TextFlagExtension
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * @deprecated 1/12/2019
 */
public final class FlagExtension implements FlagOriginListener {
  @SuppressWarnings("deprecation")
  private final FlagOrigin flagOrigin;

  private List<FlagDisplay> flagDisplays = null;
  private List<FlagDisplay> finalFlagDisplays = null;
  private String flaggedTemplateValue = null;
  private boolean flagErrors = false;
  private boolean debug = false;

  @SuppressWarnings("deprecation")
  public FlagExtension(final FlagOrigin flagOrigin) {
    this.flagOrigin = flagOrigin;
    flagOrigin.addFlagOriginListener(this);
  }

  public void setDebug(final boolean debug) {
    this.debug = debug;
  }

  public void update() {
    update(false);
  }

  /**
   * Template error:  matches  flaggedTemplateValue and not valid
   * Error: does not match flaggedTemplateValue and not valid
   * template: matches flaggedTemplateValue
   * 
   * Appearance change precedence order: template error, error, template, standard
   * 
   * @param force - call setFlag when no flag checking is in use.  Used after flags are
   * cleared.
   */
  @SuppressWarnings("deprecation")
  private void update(final boolean force) {
    if (!force && !flagErrors && flaggedTemplateValue == null) {
      return;
    }
    FlagType flagType = null;
    boolean templateFlag =
      flaggedTemplateValue != null && flagOrigin.equals(flaggedTemplateValue);
    if (flagErrors && !flagOrigin.isValid()) {
      if (templateFlag) {
        flagType = FlagType.TEMPLATE_ERROR;
      }
      else {
        flagType = FlagType.ERROR;
      }
    }
    else if (templateFlag) {
      flagType = FlagType.TEMPLATE;
    }
    Iterator<FlagDisplay> iterator;
    if (flagDisplays != null) {
      iterator = flagDisplays.iterator();
      while (iterator.hasNext()) {
        iterator.next().setFlag(flagType);
      }
    }
    if (finalFlagDisplays != null) {
      iterator = finalFlagDisplays.iterator();
      while (iterator.hasNext()) {
        iterator.next().setFlag(flagType);
      }
    }
  }

  public String getFlaggedTemplateValue() {
    return flaggedTemplateValue;
  }

  public void setFlagErrors() {
    flagErrors = true;
  }

  public void itemStateChanged(final ItemEvent event) {
    update();
  }

  public void focusLost(final FocusEvent event) {
    update();
  }

  public void focusGained(final FocusEvent event) {}

  /**
   * Change appearance when value matches the template value.
   * @param templateValue
   */
  public void flagTemplate(final String templateValue) {
    if (templateValue == null) {
      return;
    }
    flaggedTemplateValue = templateValue;
  }

  public void addFinalFlagDisplay(final FlagDisplay flagDisplay) {
    if (flagDisplay == null) {
      return;
    }
    if (finalFlagDisplays == null) {
      finalFlagDisplays = new ArrayList<FlagDisplay>();
    }
    finalFlagDisplays.add(flagDisplay);
  }

  public void addFlagDisplay(final FlagDisplay flagDisplay) {
    if (flagDisplay == null) {
      return;
    }
    if (flagDisplays == null) {
      flagDisplays = new ArrayList<FlagDisplay>();
    }
    flagDisplays.add(flagDisplay);
  }

  public void clearFlags() {
    flaggedTemplateValue = null;
    update(true);
  }
}