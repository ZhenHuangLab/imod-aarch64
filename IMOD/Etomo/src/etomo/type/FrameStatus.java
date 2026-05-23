package etomo.type;

public final class FrameStatus implements Status {
  public static final FrameStatus SINGLE = new FrameStatus("Single");
  public static final FrameStatus MONTAGE = new FrameStatus("Montage");

  private final String descr;

  private FrameStatus(final String descr) {
    this.descr = descr;
  }

  public String getText() {
    return null;
  }

  public String toString() {
    return descr;
  }
}
