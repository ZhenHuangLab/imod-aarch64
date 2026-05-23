package etomo.plugin;

import java.awt.Component;

import etomo.type.BaseScreenState;

/**
 * <p>Description: This is not a plugin interface.  It is a panel belonging to a plugin.
 * This interface is currently used with TomogramGenerationDialog.  It can be added to
 * other locations on demand.</p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface PluginPanel {
  public String getButtonTitle();

  public Component getComponent();

  public void getParameters(BaseScreenState screenState);

  public void setParameters(BaseScreenState screenState);

  public void updateDisplay();

  public void done();

  public void updateAdvanced(boolean advanced);

  public void msgVisibilityChanged(boolean visible);
}