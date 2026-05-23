package etomo.ui.swing;

/**
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright 2008 - 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.1  2009/12/01 23:41:38  sueh
 * <p> bug# 1290 Parent of iteration table.
 * <p> </p>
 */
interface IterationParent {
  public void updateDisplay(boolean init);

  public boolean isSampleSphere();
}
