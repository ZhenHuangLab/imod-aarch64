package etomo.ui;

/**
 * <p>Description: Holds a boolean setting and a text setting.  Both are optional.</p>
 * <p/>
 * <p>Copyright: Copyright 2014</p>
 * <p/>
 * <p>Organization:
 * Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
 * University of Colorado</p>
 *
 * @version $Revision$ $Date: $ $Author$ $State: $
 */
public final class FieldSettingBundle implements FieldSettingInterface {
  private BooleanFieldSetting boolSetting = null;
  private TextFieldSetting textSetting = null;

  public boolean isBoolean() {
    return boolSetting != null;
  }

  public boolean isText() {
    return textSetting != null;
  }

  public boolean isSet() {
    return (boolSetting != null && boolSetting.isSet()) ||
        (textSetting != null && textSetting.isSet());
  }

  public BooleanFieldSetting getBooleanSetting() {
    return boolSetting;
  }

  public TextFieldSetting getTextSetting() {
    return textSetting;
  }

  public void addBooleanSetting(final FieldSettingInterface input) {
    if (input != null) {
      boolSetting = input.getBooleanSetting();
    }
    else {
      boolSetting = null;
    }
  }

  public void addTextSetting(final FieldSettingInterface input) {
    if (input != null) {
      textSetting = input.getTextSetting();
    }
    else {
      textSetting = null;
    }
  }
}
