package etomo.storage;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2013 - 2018</p>
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
public final class DirectiveDescrEtomoColumn {
  public static final String rcsid = "$Id:$";

  // No effect on etomo and not saved (template)
  public static final DirectiveDescrEtomoColumn NE =
    new DirectiveDescrEtomoColumn("NE", false);
  // No effect on etomo, optionally saved (template)
  static final DirectiveDescrEtomoColumn NES = new DirectiveDescrEtomoColumn("NES", true);
  // Saved by default (template)
  public static final DirectiveDescrEtomoColumn SD =
    new DirectiveDescrEtomoColumn("SD", true);
  // Save optional (template)
  public static final DirectiveDescrEtomoColumn SO =
    new DirectiveDescrEtomoColumn("SO", true);

  private final String tag;
  private final boolean recommended;

  public String toString() {
    return tag;
  }

  private DirectiveDescrEtomoColumn(final String tag, final boolean recommended) {
    this.tag = tag;
    this.recommended = recommended;
  }

  static DirectiveDescrEtomoColumn getInstance(String input) {
    if (input.equals(NE.tag)) {
      return NE;
    }
    if (input.equals(NES.tag)) {
      return NES;
    }
    if (input.equals(SD.tag)) {
      return SD;
    }
    if (input.equals(SO.tag)) {
      return SO;
    }
    return null;
  }

  /**
   * @return true if saved for template by default
   */
  public boolean isRecommended() {
    return recommended;
  }
}