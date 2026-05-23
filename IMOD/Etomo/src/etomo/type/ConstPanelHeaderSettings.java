package etomo.type;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2014</p>
*
* <p>Organization:
* Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
* University of Colorado</p>
* 
* @author $Author$
* 
* @version $Revision$
* 
* <p> $Log$ </p>
*/
public interface ConstPanelHeaderSettings {
  public static final String rcsid = "$Id:$";

  public boolean isAdvanced();

  public boolean isMore();

  public boolean isOpen();

  public boolean isAdvancedNull();

  public boolean isOpenNull();

  public boolean isMoreNull();
}
