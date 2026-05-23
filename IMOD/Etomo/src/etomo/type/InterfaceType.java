package etomo.type;

/**
 * <p>Description: Represents each etomo interface.  String parameter is used in cpu.adoc.</p>
 * 
 * <p>Copyright: Copyright 2006 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * @threadsafe
 * @immutable
 * @n'ton
 * 
 * <p> $Log$
 * <p> Revision 1.2  2007/05/22 21:14:32  sueh
 * <p> bug# 999 Added a comment.
 * <p>
 * <p> Revision 1.1  2007/05/21 22:29:55  sueh
 * <p> bug# 1000 Class that enumerates the four different interfaces.
 * <p> </p>
 */
public final class InterfaceType {
  public static final InterfaceType BATCH_RUN_TOMO = new InterfaceType("batchRunTomo");
  public static final InterfaceType JOIN = new InterfaceType("join");
  public static final InterfaceType PEET = new InterfaceType("peet");
  public static final InterfaceType PP = new InterfaceType("pp");
  public static final InterfaceType RECON = new InterfaceType("recon");
  public static final InterfaceType SERIAL_SECTIONS = new InterfaceType("serialSections");
  public static final InterfaceType TOOLS = new InterfaceType("tools");
  public static final InterfaceType DIRECTIVE_EDITOR = new InterfaceType("directiveEditor");
  //Not used in cpu.adoc
  public static final InterfaceType FRONT_PAGE = new InterfaceType("frontPage");

  private final String name;

  private InterfaceType(String name) {
    this.name = name;
  }

  public static InterfaceType getInstance(String name) {
    if (name == null) {
      return null;
    }
    if (name.equals(RECON.name)) {
      return RECON;
    }
    if (name.equals(JOIN.name)) {
      return JOIN;
    }
    if (name.equals(PP.name)) {
      return PP;
    }
    if (name.equals(BATCH_RUN_TOMO.name)) {
      return BATCH_RUN_TOMO;
    }
    if (name.equals(PEET.name)) {
      return PEET;
    }
    return null;
  }

  public boolean equals(InterfaceType interfaceType) {
    return this == interfaceType;
  }

  public String toString() {
    return name;
  }
}
