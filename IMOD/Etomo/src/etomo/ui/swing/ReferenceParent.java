package etomo.ui.swing;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2009 - 2017 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.1  2010/11/13 16:07:34  sueh
 * <p> bug# 1417 Renamed etomo.ui to etomo.ui.swing.
 * <p>
 * <p> Revision 1.2  2009/12/01 00:27:36  sueh
 * <p> bug# 1285 Making PeetDialog.gotoSetupTab private.  Panels should not
 * <p> need to know about the tabs on the dialog they are on.
 * <p>
 * <p> Revision 1.1  2009/11/20 17:32:43  sueh
 * <p> bug# 1282 Parent of ReferencePanel.
 * <p> </p>
 */
interface ReferenceParent {
  public boolean fixIncorrectPath(FileTextFieldInterface fileTextField, boolean choosePath);

  public int getVolumeTableSize();

  public void updateDisplay(final boolean init);
  
  public boolean isFlgVolNamesAreTemplates();
}
