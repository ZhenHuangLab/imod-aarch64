package etomo.ui;

/**
 * <p>Description: An interface for a field which generates a flagged state.</p>
 * 
 * @see Replaced by TextFlagOrigin
 * 
 * <p>Copyright: Copyright 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * @deprecated 1/12/2019
 */
public interface FlagOrigin {
  public boolean equals(String flaggedValue);

  public void addFlagOriginListener(FlagOriginListener listener);
  
  public boolean isValid();
}
