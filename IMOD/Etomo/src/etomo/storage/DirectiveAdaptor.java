package etomo.storage;

import etomo.storage.autodoc.ReadOnlyStatement;

public final class DirectiveAdaptor {
  private ReadOnlyStatement statement = null;
  private String directive = null;
  private DirectiveDef directiveDef = null;

  public void set(final ReadOnlyStatement statement) {
    this.statement = statement;
    reset();
  }

  private void reset() {
    directive = null;
    directiveDef = null;
  }

  private void setup() {
    if (statement == null || (directive != null && directiveDef != null)) {
      return;
    }
    directive = statement.getLeftSide();
    directiveDef = DirectiveDef.getInstance(directive);
  }

  public DirectiveDef getDirectiveDef() {
    setup();
    return directiveDef;
  }
}