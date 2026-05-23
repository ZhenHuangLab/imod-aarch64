package etomo.logic;

/**
 * <p>Description: Used to modify how a comparison is done.</p>
 * 
 * <p>Copyright: Copyright 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class StrategyDoesNotApplyException extends Exception {
  public StrategyDoesNotApplyException() {
    super();
  }

  public StrategyDoesNotApplyException(final String message) {
    super(message);
  }
}
