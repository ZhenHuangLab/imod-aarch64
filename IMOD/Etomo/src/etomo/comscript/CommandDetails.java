package etomo.comscript;

/**
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright 2006 - 2011 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public interface CommandDetails extends Command, ProcessDetails {}
/**
* <p> $Log$
* <p> Revision 1.1  2006/05/11 19:38:43  sueh
* <p> bug# 838 Add CommandDetails, which extends Command and
* <p> ProcessDetails.  Changed ProcessDetails to only contain generic get
* <p> functions.  Command contains all the command oriented functions.
* <p> </p>
*/
