package etomo.storage;

public interface DirectiveValue {
  public String getValue();

  public boolean isBatch();

  public boolean isOverride();
}
