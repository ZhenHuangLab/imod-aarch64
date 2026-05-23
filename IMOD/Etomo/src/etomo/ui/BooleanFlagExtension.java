package etomo.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Description: Chooses a flag type and informs flag displays.</p>
 * 
 * <p>Copyright: Copyright 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class BooleanFlagExtension {
  private final BooleanFlagOrigin flagOrigin;

  private List<FlagDisplay> flagDisplays = null;
  private boolean warningEnabled = false;
  private boolean warningValue = false;

  public BooleanFlagExtension(final BooleanFlagOrigin flagOrigin) {
    this.flagOrigin = flagOrigin;
  }

  public void enableWarning(final boolean value) {
    warningEnabled = true;
    warningValue = value;
  }

  public void disableWarning() {
    warningEnabled = false;
  }

  public void update() {
    FlagType flagType = null;
    if (warningEnabled && flagOrigin.isSelected() == warningValue) {
      flagType = FlagType.WARNING;
    }
    Iterator<FlagDisplay> iterator;
    if (flagDisplays != null) {
      iterator = flagDisplays.iterator();
      while (iterator.hasNext()) {
        iterator.next().setFlag(flagType);
      }
    }
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
}
