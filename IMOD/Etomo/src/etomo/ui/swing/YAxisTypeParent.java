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
 * <p> Revision 1.1  2009/12/08 02:51:18  sueh
 * <p> bug# 1286 Interface of PeetDialog.
 * <p> </p>
 */
interface YAxisTypeParent {
  public void updateDisplay(boolean init);

  public boolean isVolumeTableEmpty();
}
