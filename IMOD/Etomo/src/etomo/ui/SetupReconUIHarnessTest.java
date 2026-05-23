package etomo.ui;

import java.io.File;

import etomo.ApplicationManager;
import etomo.EtomoDirector;
import etomo.comscript.ExcludeViewsParam;
import etomo.storage.DirectiveFileCollection;
import etomo.type.AxisID;
import etomo.type.TiltAngleSpec;
import etomo.type.UserConfiguration;
import etomo.util.Utilities;

public class SetupReconUIHarnessTest implements SetupReconInterface {
  private static final File TEST_DIR =
    new File(UITests.TEST_ROOT_DIR, "SetupReconUIHarness");

  private final SetupReconUIHarness harness = new SetupReconUIHarness(
    (ApplicationManager) EtomoDirector.INSTANCE.getCurrentManagerForTest(), null);

  private String rawImageStack = null;
  private boolean dual = false;

  public SetupReconUIHarnessTest() {
    super();
  }

  public void testGetStackFileName() {
    rawImageStack = new File(TEST_DIR, "realfile.join").getAbsolutePath();
    Utilities.deleteFile(new File(rawImageStack + ".st"), null, null);
    Utilities.deleteFile(new File(rawImageStack + ".st"), null, null);
    Utilities.deleteFile(new File(rawImageStack + ".mrc"), null, null);
    Utilities.deleteFile(new File(rawImageStack + ".hdf"), null, null);
    Utilities.deleteFile(new File(rawImageStack + "a.tif"), null, null);
    Utilities.deleteFile(new File(rawImageStack + "a.tiff"), null, null);
    Utilities.deleteFile(new File(rawImageStack + "b.tiff"), null, null);
    Utilities.deleteFile(new File(rawImageStack + "a.rec"), null, null);
    Utilities.deleteFile(new File(rawImageStack + ".hdf"), null, null);
    rawImageStack = null;
    //
    /*   testGetStackFileName("null raw image stack is returned", rawImageStack, null);
    //
    rawImageStack = "";
    testGetStackFileName("empty raw image stack causes functions to return null", null,
      null);
    //
    rawImageStack = new File(TEST_DIR, "dataset.mrc").getAbsolutePath();
    testGetStackFileName("mrc ok for input image files", rawImageStack, null);
    //
    dual = true;
    rawImageStack = new File(TEST_DIR, "dataset.hdf").getAbsolutePath();
    testGetStackFileName("for valid input image file extension it ignores the axis type",
      rawImageStack, rawImageStack + "a.st");
    //
    rawImageStack = new File(TEST_DIR, "dataset.st").getAbsolutePath();
    testGetStackFileName("st ok for input image files", rawImageStack, null);
    //
    dual = false;
    rawImageStack = new File(TEST_DIR, "dataset.tif").getAbsolutePath();
    testGetStackFileName("tif ok for input image files", rawImageStack,
      rawImageStack + ".st");
    //
    rawImageStack = new File(TEST_DIR, "dataset.tiff").getAbsolutePath();
    testGetStackFileName("tiff ok for input image files", rawImageStack,
      rawImageStack + ".st");
    //
    rawImageStack = new File(TEST_DIR, "dataset.join").getAbsolutePath();
    testGetStackFileName(
      "join not ok for input image files - since no valid input image files exist uses "
        + "the default extension",
      rawImageStack + ".mrc", rawImageStack + ".st");
    //
    dual = true;
    testGetStackFileName("uses the default extension", rawImageStack + "a.mrc",
      rawImageStack + "a.st");
    //
    rawImageStack = new File(TEST_DIR, "realfile.st").getAbsolutePath();
    TEST_DIR.mkdirs();
    BaseProcessManager.touch(rawImageStack + ".st", null);
    testGetStackFileName(
      "st ok for input image files - does not attempt to change file paths that are ok",
      rawImageStack, null);
    //
    dual = false;
    rawImageStack = new File(TEST_DIR, "realfile.join").getAbsolutePath();
    BaseProcessManager.touch(rawImageStack, null);
    BaseProcessManager.touch(rawImageStack + ".st", null);
    testGetStackFileName("join is not ok for input image files - corrects it",
      rawImageStack + ".st", null);
    //
    BaseProcessManager.touch(rawImageStack + ".mrc", null);
    testGetStackFileName(
      "the new functionality defaults to mrc with the default name settings, the old one "
        + "defaults to st",
      rawImageStack + ".mrc", rawImageStack + ".st");
    //
    dual = true;
    testGetStackFileName(
      "the new functionality defaults to mrc with the default name settings, the old one "
        + "defaults to st",
      rawImageStack + "a.mrc", rawImageStack + "a.st");
    //
    dual = false;
    Utilities.deleteFile(new File(rawImageStack + ".mrc"), null, null);
    BaseProcessManager.touch(rawImageStack + ".hdf", null);
    testGetStackFileName("old functionality cannot handle hdf", rawImageStack + ".hdf",
      rawImageStack + ".st");
    //
    dual = true;
    BaseProcessManager.touch(rawImageStack + "a.tif", null);
    testGetStackFileName(
      "old functionality cannot handle tif & when dual single axis files are ignored",
      rawImageStack + "a.tif", rawImageStack + "a.st");
    //
    Utilities.deleteFile(new File(rawImageStack + "a.tif"), null, null);
    BaseProcessManager.touch(rawImageStack + "a.tiff", null);
    testGetStackFileName("old functionality cannot handle tiff", rawImageStack + "a.tiff",
      rawImageStack + "a.st");
    //
    Utilities.deleteFile(new File(rawImageStack + "a.tiff"), null, null);
    BaseProcessManager.touch(rawImageStack + "b.tiff", null);
    testGetStackFileName("b axis is never checked", rawImageStack + "a.mrc",
      rawImageStack + "a.st");
    //
    BaseProcessManager.touch(rawImageStack + "a.rec", null);
    testGetStackFileName("non input image files are never checked and won't be returned",
      rawImageStack + "a.mrc", rawImageStack + "a.st");
    */ }

  public String getRawImageStack() {
    return rawImageStack;
  }

  @SuppressWarnings("deprecation")
  public String getDataset() {
    return getRawImageStack();
  }

  public boolean isDualAxisSelected() {
    return dual;
  }

  public boolean isSingleAxisSelected() {
    return !dual;
  }

  public void setBinning(String input) {}

  public void setImageRotation(String input) {}

  public void setPixelSize(double input) {}

  public String getDistortionFile() {
    return null;
  }

  public String getMagGradientFile() {
    return null;
  }

  public boolean validateTiltAngle(AxisID axisID, String errorTitle) {
    return false;
  }

  public boolean isSingleViewSelected() {
    return false;
  }

  public String getBackupDirectory() {
    return null;
  }

  public String getBinning(boolean doValidation) throws FieldValidationFailedException {
    return null;
  }

  public String getExcludeList(AxisID axisID, boolean doValidation)
    throws FieldValidationFailedException {
    return null;
  }

  public String getTwodir(AxisID axisID, boolean doValidation)
    throws FieldValidationFailedException {
    return null;
  }

  public boolean isTwodir(AxisID axisID) {
    return false;
  }

  public void setTwodir(AxisID axisID, double input) {}

  public String getFiducialDiameter(boolean doValidation)
    throws FieldValidationFailedException {
    return null;
  }

  public String getImageRotation(AxisID axisID, boolean doValidation)
    throws FieldValidationFailedException {
    return null;
  }

  public String getPixelSize(boolean doValidation) throws FieldValidationFailedException {
    return null;
  }

  public boolean isAdjustedFocusSelected(AxisID axisID) {
    return false;
  }

  public boolean isGpuProcessingSelected(String propertyUserDir) {
    return false;
  }

  public boolean isParallelProcessSelected(String propertyUserDir) {
    return false;
  }

  public boolean getTiltAngleFields(AxisID axisID, TiltAngleSpec tiltAngleSpec,
    boolean doValidation) {
    return false;
  }

  public DirectiveFileCollection getDirectiveFileCollection() {
    return null;
  }

  public void initTiltAngleFields(AxisID axisID, TiltAngleSpec tiltAngleSpec,
    UserConfiguration userConfiguration) {}

  public boolean getParameters(ExcludeViewsParam param, AxisID axisID, boolean dualAxis,
    boolean doValidation) {
    return false;
  }

  public void msgExcludeViewsSucceeded(AxisID axisID, boolean processRunning,
    boolean processDone) {}
}
