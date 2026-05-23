package etomo.type;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2007 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.1  2007/04/13 19:56:28  sueh
 * <p> bug# 964 Interface for enumeration types, so that they can be stored in radio
 * <p> buttons.
 * <p> </p>
 */
public interface EnumeratedType {
  public boolean isDefault();

  public ConstEtomoNumber getValue();

  /**
   * @return string version of value
   */
  public String toString();

  public String getLabel();
}
