package etomo.comscript;

/**
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright 2004 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 *
 * <p> $Log$
 * <p> Revision 1.1  2004/08/19 01:30:31  sueh
 * <p> Constant object for the echo command
 * <p> </p>
 */
class ConstEchoParam {
  static final String COMMAND_NAME = "echo";

  StringBuffer string;

  ConstEchoParam() {
    reset();
  }

  void reset() {
    string = new StringBuffer();
  }

  /**
   * @return String
   */
  public String getString() {
    return string.toString();
  }

}
