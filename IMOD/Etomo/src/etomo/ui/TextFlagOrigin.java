package etomo.ui;

/**
 * <p>Description: An interface for a field which generates a flagged state.  Replaces
 * FlagOrigin.</p>
 * 
 * <p>Copyright: Copyright 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface TextFlagOrigin {
  public boolean equals(String value);

  public void addFlagOriginListener(FlagOriginListener listener);

  public boolean isValid();
}
