package etomo.type;

import etomo.storage.DirectiveDef;

public interface DirectiveMapInterface {
  /**
   * Gets the directive based on pairAxisID and directiveDef.  If directiveDef is the
   * member of a pair of directives (CopyArg with an A and a B form), then the directive
   * corresponding to pairAxisID is returned.
   * @param directiveDef
   * @param pairAxisID
   * @return
   */
  public DirectiveInterface getDirectiveFromPair(DirectiveDef directiveDef,
    AxisID pairAxisID);

  public DirectiveInterface getDirective(DirectiveDef directiveDef);
}
