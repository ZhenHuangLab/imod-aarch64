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
 * <p> Revision 1.1  2004/08/19 01:32:09  sueh
 * <p> Constant object for the goto command
 * <p> </p>
 */

class ConstGotoParam {
  static final char DELIMITER = ':';
  static final String COMMAND_NAME = "goto";

  String label = "";

  ConstGotoParam() {
    reset();
  }

  void reset() {
    label = "";
  }

  /**
   * @return String
   */
  String getLabel() {
    return label;
  }

}
