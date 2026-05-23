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
 * <p> Revision 1.1  2009/12/01 00:25:25  sueh
 * <p> bug# 1285 Factored MissingWedgeCompensation out of PeetDialog.
 * <p> </p>
 */
interface MissingWedgeCompensationParent {
  public boolean isVolumeTableEmpty();

  public boolean isReferenceParticleSelected();

  public void updateDisplay(boolean init);
}
