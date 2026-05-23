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
 * <p> Revision 1.1  2004/08/19 01:31:23  sueh
 * <p> Constant object for the exit command
 * <p> </p>
 */
class ConstExitParam {
  static final String COMMAND_NAME = "exit";

  int resultValue;

  ConstExitParam() {
    reset();
  }

  void reset() {
    resultValue = 0;
  }

  /**
   * @return String
   */
  public int getResultValue() {
    return resultValue;
  }

}
