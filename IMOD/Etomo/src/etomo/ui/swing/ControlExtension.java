package etomo.ui.swing;

/**
 * <p>Works with ControlState to display the controlled state.</p>
 * 
 * <p>Copyright: Copyright 2018 - 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * @deprecated 1/23/2019 - replaced by ControlState.
 */
final class ControlExtension {
  private ControlExtension(ControlState controlState) {}

  /**
   * @deprecated 2/7/2019
   * @param target
   * @param controlled
   * @param curState
   * @param newState
   * @return
   */
  public final ControlState setControlled(final ControlTarget target,
    final boolean controlled, final ControlState curState, final ControlState newState) {
    return null;
  }
}