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
public interface ComparisonStrategy {
  public boolean equals(String leftString, String rightString)
    throws StrategyDoesNotApplyException;
}
