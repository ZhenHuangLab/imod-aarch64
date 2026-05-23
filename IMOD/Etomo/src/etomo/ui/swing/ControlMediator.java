package etomo.ui.swing;

import java.io.File;

/**
 * <p>Description: ControlMediator allows a controller and its target to
 * communicate.
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class ControlMediator {
  static ControlMediator INSTANCE = new ControlMediator();

  private ControlMediator() {}

  /**
   * Execute a control event.  After this is done the target is notified so so it
   * can send control events to its listeners.
   * @param target
   * @param mode
   */
  public void controlEvent(final Controller controller, final ControlTarget target,
    final ControlMode mode) {
    if (mode instanceof ControlState) {
      controlEvent(controller, target, (ControlState) mode);
      return;
    }
    if (target == null) {
      return;
    }
    if (mode == ControlMode.CLEAR) {
      target.clear();
    }
    else if (mode == ControlMode.SELECT_FILE) {
      File file = controller.selectFile();
      if (file != null) {
        target.setText(file);
      }
    }
    target.sendControlEvent();
  }

  /**
   * Set a control state on or off.  After this is done the target is notified so so it
   * can send control events to its listeners.
   * @param controller
   * @param target
   * @param state - new state
   */
  public void controlEvent(final Controller controller, final ControlTarget target,
    final ControlState state) {
    if (controller == null || state == null || target == null) {
      return;
    }
    boolean control = controller != null && controller.isControl();
    if (state.isComponentDisplay()) {
      target.setComponentControl(control, state);
    }
    else if (state.isEnableDisplay()) {
      target.setEnableControl(control, state);
      return;
    }
    target.sendControlEvent();
  }
}