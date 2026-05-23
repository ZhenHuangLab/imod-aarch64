package etomo.logic;

import java.math.BigDecimal;

/**
 * <p>Description: Used to modify how a comparison is done.</p>
 * 
 * <p>Copyright: Copyright 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class NumericComparisonStrategy implements ComparisonStrategy {
  public static final NumericComparisonStrategy INSTANCE =
    new NumericComparisonStrategy();

  private NumericComparisonStrategy() {}

  /**
   * Attempts to do a numeric comparison on leftString and rightString.  Handles integers
   * and floats of any size.  Does not have a problem with floating point errors.
   * @param leftString
   * @param rightString
   * @throws StrategyDoesNotApplyException if either parameter is non-numeric or null
   * @return true if parameters are numerically equal
   */
  public boolean equals(final String leftString, final String rightString)
    throws StrategyDoesNotApplyException {
    if (leftString == null || rightString == null) {
      throw new StrategyDoesNotApplyException("null");
    }
    try {
      return new BigDecimal(leftString).compareTo(new BigDecimal(rightString)) == 0;
    }
    catch (NumberFormatException e) {
      throw new StrategyDoesNotApplyException(e.getMessage());
    }
  }
}