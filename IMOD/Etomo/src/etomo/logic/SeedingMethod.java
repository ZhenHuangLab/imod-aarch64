package etomo.logic;

import etomo.type.AxisID;
import etomo.type.ConstEtomoNumber;
import etomo.type.EnumeratedType;
import etomo.type.EtomoNumber;
import etomo.type.MetaData;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2013</p>
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
public final class SeedingMethod implements EnumeratedType {
  public static final String rcsid = "$Id:$";

  public static final SeedingMethod MANUAL = new SeedingMethod("0");
  public static final SeedingMethod AUTO_FID_SEED = new SeedingMethod("1");
  public static final SeedingMethod TRANSFER_FID = new SeedingMethod("2");
  public static final SeedingMethod BOTH = new SeedingMethod("3");

  private final EtomoNumber value = new EtomoNumber();

  private SeedingMethod(final String value) {
    this.value.set(value);
  }

  public static SeedingMethod getInstance(String value) {
    if (value == null) {
      return null;
    }
    value = value.trim();
    if (MANUAL.value.equals(value)) {
      return MANUAL;
    }
    if (AUTO_FID_SEED.value.equals(value)) {
      return AUTO_FID_SEED;
    }
    if (TRANSFER_FID.value.equals(value)) {
      return TRANSFER_FID;
    }
    if (BOTH.value.equals(value)) {
      return BOTH;
    }
    return null;
  }

  /**
   * Does not return null;
   */
  public ConstEtomoNumber getValue() {
    return value;
  }

  public boolean isDefault() {
    return false;
  }

  public String getLabel() {
    return null;
  }

  public static String toDirectiveValue(final MetaData metaData, final AxisID axisID) {
    if (metaData.isTrackSeedModelManual(axisID)) {
      return MANUAL.value.toString();
    }
    if (metaData.isTrackSeedModelAuto(axisID)) {
      return AUTO_FID_SEED.value.toString();
    }
    if (metaData.isTrackSeedModelTransfer(axisID)) {
      return TRANSFER_FID.value.toString();
    }
    return null;
  }
}
