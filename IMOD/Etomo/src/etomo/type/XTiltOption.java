package etomo.type;

public final class XTiltOption {
  public static final XTiltOption FIX = new XTiltOption(0);
  public static final XTiltOption AUTOMAP_SAME = new XTiltOption(4);

  private final int option;

  private XTiltOption(final int option) {
    this.option = option;
  }

  public static XTiltOption getInstance(final int option) {
    if (FIX.option == option) {
      return FIX;
    }
    if (AUTOMAP_SAME.option == option) {
      return AUTOMAP_SAME;
    }
    return null;
  }

  public int getOption() {
    return option;
  }
}
