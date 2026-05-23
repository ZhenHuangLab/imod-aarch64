package etomo.type;

import etomo.BaseManager;
import etomo.process.ImodManager;

public class FileKey {
  public static final FileKey AVERAGED_VOLUMES = new FileKey(ImodManager.AVG_VOL_KEY);
  public static final FileKey NAD_TEST_VARYING_ITERATIONS =
    new FileKey(ImodManager.VARYING_ITERATION_TEST_KEY);
  public static final FileKey NAD_TEST_VARYING_K =
    new FileKey(ImodManager.VARYING_K_TEST_KEY);
  public static final FileKey POSITIONING_SAMPLE = new FileKey(ImodManager.SAMPLE_KEY);
  public static final FileKey REFERENCE_VOLUMES = new FileKey(ImodManager.REF_KEY);
  // This file name is created by the user.
  public static final FileKey TRIAL_TOMOGRAM =
    new FileKey(ImodManager.TRIAL_TOMOGRAM_KEY);

  private final String imodManagerKey;
  private final String imodManagerKey2;
  private final String descr;

  FileKey(final String imodManagerKey) {
    this.imodManagerKey = imodManagerKey;
    imodManagerKey2 = null;
    descr = null;
  }

  FileKey(final String imodManagerKey, final String imodManagerKey2, final String descr) {
    this.imodManagerKey = imodManagerKey;
    this.imodManagerKey2 = imodManagerKey2;
    this.descr = descr;
  }

  public String getDescription() {
    if (descr != null) {
      return descr;
    }
    if (imodManagerKey != null) {
      return imodManagerKey;
    }
    if (imodManagerKey2 != null) {
      return imodManagerKey2;
    }
    return null;
  }

  public String getFileName(final BaseManager manager, final AxisID axisID) {
    return getDescription();
  }

  public final String getImodManagerKey() {
    return imodManagerKey;
  }

  public final String getImodManagerKey2() {
    return imodManagerKey2;
  }

  public final String getDescr() {
    return descr;
  }
}
