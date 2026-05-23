package etomo.ui;

/**
 * <p>Description: Interface for extensible fields that need their value modified.</p>
 * 
 * <p>Copyright: Copyright 2017 - 2020 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface ValueManipulationField {
  public void addValueManipulationListener(ValueManipulationListener listener);

  public boolean isEmpty();

  public void setText(String text);
}