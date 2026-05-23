package etomo.type;

import java.util.Properties;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2012</p>
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
public class SerialSectionsState extends BaseState {
  public static final String rcsid = "$Id:$";

  private static final String groupString = "SerialSectionsState";
  EtomoState invalidEdgeFunctions = new EtomoState("InvalidEdgeFunctions");

  private final EtomoNumber preblendRobustFitting =
    new EtomoNumber(EtomoNumber.Type.DOUBLE, "Preblend.RobustFitting");

  public void initialize() {
    invalidEdgeFunctions.set(EtomoState.NO_RESULT_VALUE);
  }

  public void store(Properties props) {
    store(props, "");
  }

  public void store(Properties props, String prepend) {
    super.store(props, prepend);
    prepend = createPrepend(prepend);
    String group = prepend + ".";
    invalidEdgeFunctions.store(props, prepend);
    preblendRobustFitting.store(props, prepend);
  }

  public void load(Properties props) {
    load(props, "");
  }

  public void load(Properties props, String prepend) {
    super.load(props, prepend);
    // reset
    invalidEdgeFunctions.reset();
    preblendRobustFitting.reset();
    // load
    invalidEdgeFunctions.load(props, prepend);
    preblendRobustFitting.load(props, prepend);
  }

  public ConstEtomoNumber setInvalidEdgeFunctions(boolean invalidEdgeFunctions) {
    return this.invalidEdgeFunctions.set(invalidEdgeFunctions);
  }

  public ConstEtomoNumber getInvalidEdgeFunctions() {
    return invalidEdgeFunctions;
  }

  public void setPreblendRobustFitting(final double value) {
    preblendRobustFitting.set(value);
  }

  public boolean equalsPreblendRobustFitting(final boolean set, final String value) {
    if (!set && preblendRobustFitting.isNull()) {
      return true;
    }
    return set && preblendRobustFitting.equals(value);
  }

  String createPrepend(String prepend) {
    if (prepend == "") {
      return groupString;
    }
    return prepend + "." + groupString;
  }

}
