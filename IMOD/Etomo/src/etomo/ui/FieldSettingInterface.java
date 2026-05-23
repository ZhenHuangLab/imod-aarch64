package etomo.ui;

/**
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright 2014</p>
 * <p/>
 * <p>Organization:
 * Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
 * University of Colorado</p>
 *
 * @author $Author$
 * @version $Revision$  $Date: $
 */
public interface FieldSettingInterface {
  public BooleanFieldSetting getBooleanSetting();

  public TextFieldSetting getTextSetting();

  public boolean isBoolean();

  public boolean isText();

  public boolean isSet();
}
