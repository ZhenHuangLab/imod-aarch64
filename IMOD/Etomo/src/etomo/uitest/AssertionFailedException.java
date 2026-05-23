package etomo.uitest;

import junit.framework.AssertionFailedError;

/**
 * <p>Description: An exception to carry an AssertionFailedError instance.</p>
 * 
 * <p>Copyright: Copyright 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
final class AssertionFailedException extends Exception {
  private final AssertionFailedError error;

  AssertionFailedException(final AssertionFailedError error) {
    this.error = error;
  }

  AssertionFailedError getAssertionFailedError() {
    return error;
  }
}
