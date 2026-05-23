package etomo.type;

import etomo.ProcessSeries;
import etomo.ui.UIComponent;
import etomo.ui.swing.ProcessDisplay;

/**
 * <p>Description: Any class that provides a nextProcess function for ProcesSeries can
 * implement this interface.  This target is set at the process level, rather then the
 * series level.  The interface manager is still set at the series level.  Its
 * nextProcess function will not be called for process where the NextProcessTarget is set.
 * </p>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface NextProcessTarget {
  public boolean startNextProcess(UIComponent uiComponent, AxisID axisID,
    ProcessSeries.Process process, ProcessResultDisplay processResultDisplay,
    ProcessSeries processSeries, DialogType dialogType, ProcessDisplay display);
}