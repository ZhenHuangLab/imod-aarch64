package etomo.comscript;

/**
 * <p>Description: </p>
 * 
 * <p>Copyright: Copyright 2002 - 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * <p> $Log$
 * <p> Revision 1.1  2004/06/24 18:37:01  sueh
 * <p> bug# 482 const param for matchshifts command
 * <p> </p>
 */
class ConstMatchshiftsParam {
  String rootName1;
  String rootName2;
  int xDim;
  int yDim;
  int zDim;
  String xfIn;
  String xfOut;

  private static final String command = "matchshifts";

  ConstMatchshiftsParam() {
    reset();
  }

  void reset() {
    rootName1 = new String();
    rootName2 = new String();
    xDim = Integer.MIN_VALUE;
    yDim = Integer.MIN_VALUE;
    zDim = Integer.MIN_VALUE;
    xfIn = new String();
    xfOut = new String();
  }

  String getCommand() {
    return command;
  }
}
