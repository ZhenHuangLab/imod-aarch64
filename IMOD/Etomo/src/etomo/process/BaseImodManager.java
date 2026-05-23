package etomo.process;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.AxisTypeException;
import etomo.type.FileType;
import etomo.type.Run3dmodMenuOptions;
import etomo.ui.swing.UIHarness;
import etomo.util.Utilities;

/**
* <p>Description: This class manages the opening, closing and sending of 
* messages to 3dmod instances.  It stores information about each 3dmod instance that it
* manages.</p>
* 
* <p>Inherit this class in order to create a customized 3dmod instances.</p>
*
* <p>Copyright: Copyright 2016 - 2020 by the Regents of the University of Colorado</p>
* 
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
*/
public abstract class BaseImodManager {
  public static final int DEFAULT_BEADFIXER_DIAMETER = 3;

  private AxisType axisType = AxisType.SINGLE_AXIS;
  private boolean useMap = true;
  private boolean debug = false;

  final HashMap imodMap = new HashMap();

  protected final BaseManager manager;
  private final ImodRequestHandler requestHandler;

  protected BaseImodManager(final BaseManager manager) {
    this.manager = manager;
    // Only run the request handler when necesary. In Windows when 3dmod is
    // listening to stdin and it wants to exit, it sends a request to stderr to
    // ask that the stdin receive a stop listening command. This is because
    // 3dmod in Windows can't exit when it is listening to stdin.
    if (Utilities.isWindowsOS() && EtomoDirector.INSTANCE.getArguments().isListen()) {
      requestHandler = ImodRequestHandler.getInstance(this);
    }
    else {
      requestHandler = null;
    }
  }

  /**
   * Return an imodState specific to the key.
   * @param key
   * @param fileExtension
   * @param axisID
   * @param datasetName
   * @param file
   * @param fileNameArray
   * @param subdirName
   * @param fileList
   * @return
   */
  protected ImodState newImodState(final String key, final String fileExtension,
    final AxisID axisID, final String datasetName, final File file,
    final String[] fileNameArray, final String subdirName, final File[] fileList) {
    return new ImodState(manager, file, axisID);
  }

  /**
   * Return the public key, unless there is a corresponding private key that can have a
   * different value from the public key.
   * @param publicKey
   * @return
   */
  String getPrivateKey(final String publicKey) {
    return publicKey;
  }

  /**
   * Return true if the 3dmod instance(s) are associated the whole dataset, rather then
   * an individual axis.
   * @param key
   * @return
   */
  boolean isDualAxisOnly(final String key) {
    return false;
  }

  /**
   * Return true if there is a different 3dmod instance for each axis.
   * @param key
   * @return
   */
  boolean isPerAxis(final String key) {
    return true;
  }

  final void setAxisType(final AxisType axisType) {
    this.axisType = axisType;
  }

  final boolean equalsAxisType(final AxisType input) {
    return axisType == input;
  }

  final String getAxisTypeString() {
    if (axisType == null) {
      return AxisType.NOT_SET.toString();
    }
    return axisType.toString();
  }

  private final int newImod(final String key) throws AxisTypeException {
    return newImod(key, (AxisID) null, (String) null);
  }

  final int newImod(final String key, final AxisID axisID) throws AxisTypeException {
    return newImod(key, axisID, (String) null);
  }

  public final int newImod(final String key, final String fileExtension,
    final AxisID axisID) throws AxisTypeException {
    return newImod(key, fileExtension, axisID, (String) null);
  }

  public final int newImod(final String key, final AxisID axisID,
    final String datasetName) throws AxisTypeException {
    return newImod(key, null, axisID, datasetName);
  }

  private final int newImod(String key, final String fileExtension, final AxisID axisID,
    final String datasetName) throws AxisTypeException {
    Vector vector;
    ImodState imodState;
    key = getPrivateKey(key);
    vector = getVector(key, axisID);
    if (vector == null) {
      vector = newVector(key, fileExtension, axisID, datasetName);
      if (axisID == null) {
        imodMap.put(key, vector);
      }
      else {
        imodMap.put(key + axisID.getExtension(), vector);
      }
      return 0;
    }
    imodState = newImodState(key, fileExtension, axisID, datasetName);
    vector.add(imodState);
    return vector.lastIndexOf(imodState);
  }

  public final int newImod(String key, final AxisID axisID, final File[] fileList)
    throws AxisTypeException {
    Vector vector;
    ImodState imodState;
    key = getPrivateKey(key);
    vector = getVector(key, axisID);
    if (vector == null) {
      vector = newVector(key, axisID, fileList);
      if (axisID == null) {
        imodMap.put(key, vector);
      }
      else {
        imodMap.put(key + axisID.getExtension(), vector);
      }
      return 0;
    }
    imodState = newImodState(key, axisID, fileList);
    vector.add(imodState);
    return vector.lastIndexOf(imodState);
  }

  public final int newImod(String key, final File file) throws AxisTypeException {
    Vector vector;
    ImodState imodState;
    key = getPrivateKey(key);
    vector = getVector(key);
    if (vector == null) {
      vector = newVector(key, file);
      imodMap.put(key, vector);
      return 0;
    }
    imodState = newImodState(key, file);
    vector.add(imodState);
    return vector.lastIndexOf(imodState);
  }

  /**
   * Changes the file in an existing imod, if the imod exists
   * @param key
   * @param index
   * @param file
   * @return
   * @throws AxisTypeException
   */
  public final void updateImod(final String key, final int index, final File file)
    throws AxisTypeException {
    Vector vector = getVector(getPrivateKey(key));
    if (vector != null && vector.size() > index) {
      ImodState imodState = (ImodState) vector.get(index);
      if (imodState != null) {
        imodState.setFile(file);
      }
    }
  }

  private final int newImod(String key, AxisID axisID, final File file)
    throws AxisTypeException {
    Vector vector;
    ImodState imodState;
    key = getPrivateKey(key);
    vector = getVector(key, axisID);
    if (vector == null) {
      vector = newVector(key, axisID, file);
      if (axisID == null) {
        imodMap.put(key, vector);
      }
      else {
        // Correct axis
        if (axisID == AxisID.FIRST) {
          axisID = AxisID.ONLY;
        }
        imodMap.put(key + axisID.getExtension(), vector);
      }
      return 0;
    }
    imodState = newImodState(key, axisID, file);
    vector.add(imodState);
    return vector.lastIndexOf(imodState);
  }

  private final int newImod(String key, final String[] fileNameArray)
    throws AxisTypeException {
    Vector vector;
    ImodState imodState;
    key = getPrivateKey(key);
    vector = getVector(key);
    if (vector == null) {
      vector = newVector(key, fileNameArray);
      imodMap.put(key, vector);
      return 0;
    }
    imodState = newImodState(key, fileNameArray);
    vector.add(imodState);
    return vector.lastIndexOf(imodState);
  }

  private final int newImod(String key, final String[] fileNameArray,
    final String subdirName) throws AxisTypeException {
    Vector vector;
    ImodState imodState;
    key = getPrivateKey(key);
    vector = getVector(key);
    if (vector == null) {
      vector = newVector(key, fileNameArray, subdirName);
      imodMap.put(key, vector);
      return 0;
    }
    imodState = newImodState(key, fileNameArray, subdirName);
    vector.add(imodState);
    if (debug) {
      System.out.println("vector=" + vector);
    }
    return vector.lastIndexOf(imodState);
  }

  public final void open(final String key)
    throws AxisTypeException, SystemProcessException, IOException {
    open(key, null, (String) null, null);
  }

  public final void open(final String key, final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    open(key, null, (String) null, menuOptions);
    // used for:
    // openCombinedTomogram
  }

  public final void open(final String key, final String model,
    final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    open(key, null, model, menuOptions);
    // used for:
    // openCombinedTomogram
  }

  public final void open(final String key, final AxisID axisID,
    final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    open(key, axisID, (String) null, menuOptions);
  }

  public final void open(final String key, final AxisID axisID, final String model)
    throws AxisTypeException, SystemProcessException, IOException {
    open(key, axisID, model, new Run3dmodMenuOptions());
  }

  public final void open(final String key, final AxisID axisID)
    throws AxisTypeException, SystemProcessException, IOException {
    open(key, axisID, (String) null, new Run3dmodMenuOptions());
  }

  public final void open(String key, final File file,
    final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key);
    if (imodState == null) {
      newImod(key, file);
      imodState = get(key);
    }
    if (imodState != null) {
      imodState.open(menuOptions);
    }
  }

  public final void open(String key, final AxisID axisID, final File file,
    final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID, file);
    if (imodState == null) {
      newImod(key, axisID, file);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.open(menuOptions);
    }
  }

  public final void open(String key, final File file,
    final Run3dmodMenuOptions menuOptions, boolean swapYZ)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key);
    if (imodState == null) {
      newImod(key, file);
      imodState = get(key);
    }
    if (imodState != null) {
      imodState.setSwapYZ(swapYZ);
      imodState.open(menuOptions);
    }
  }

  public final void open(String key, final AxisID axisID, final String model,
    final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      if (model == null) {
        imodState.open(menuOptions);
      }
      else {
        imodState.open(model, menuOptions);
      }
    }
  }

  public final void open(String key, final AxisID axisID, final List<String> modelList,
    final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      if (modelList == null) {
        imodState.open(menuOptions);
      }
      else {
        imodState.open(modelList, menuOptions);
      }
    }
  }

  public final void setOpenModelView(String key, final AxisID axisID)
    throws AxisTypeException, IOException, SystemProcessException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setOpenModelView();
    }
  }

  public final void open(String key, final String[] fileNameArray,
    final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, AxisID.ONLY);
    if (imodState == null || !imodState.equalsFileNameArray(fileNameArray)) {
      newImod(key, fileNameArray);
      imodState = get(key, AxisID.ONLY);
    }
    if (imodState != null) {
      imodState.open(menuOptions);
    }
  }

  public final void open(String key, final String[] fileNameArray,
    final Run3dmodMenuOptions menuOptions, final String subdirName, final boolean swapYZ)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, AxisID.ONLY);
    if (imodState == null || !imodState.equalsSubdirName(subdirName)
      || !imodState.equalsFileNameArray(fileNameArray)) {
      newImod(key, fileNameArray, subdirName);
      imodState = get(key, AxisID.ONLY);
    }
    if (imodState != null) {
      imodState.setSwapYZ(swapYZ);
      imodState.open(menuOptions);
    }
  }

  /**
   * 
   * @param key
   * @param axisID
   * @param model
   * @param modelMode
   * @throws AxisTypeException
   * @throws SystemProcessException
   */
  public final void open(String key, final AxisID axisID, final String model,
    final boolean modelMode, final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    // TEMP
    System.err.println("E:key=" + key + ",axis=" + imodState.getAxisID());
    if (imodState != null) {
      imodState.open(model, modelMode, menuOptions);
    }
    // rawStack.model(modelName, modelMode);
  }

  public final void open(String key, final AxisID axisID, final FileType model,
    final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    // TEMP
    System.err.println("key=" + key + ",axis=" + imodState.getAxisID());
    if (imodState != null) {
      imodState.open(model, menuOptions);
    }
    // rawStack.model(modelName, modelMode);
  }

  public final void open(String key, final AxisID axisID, final File file,
    final String model, final boolean modelMode, final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID, file);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.open(model, modelMode, menuOptions);
    }
  }

  /**
   * 
   * @param key
   * @param axisID
   * @param model
   * @param modelMode
   * @throws AxisTypeException
   * @throws SystemProcessException
   */
  public final void open(String key, final File file, final String model,
    final boolean modelMode, final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key);
    if (imodState == null) {
      newImod(key, file);
      imodState = get(key);
    }
    // TEMP
    System.err.println("key=" + key + ",axis=" + imodState.getAxisID());
    if (imodState != null) {
      imodState.open(model, modelMode, menuOptions);
    }
  }

  public final int open(String key, final File file, final AxisID axisID, int vectorIndex,
    final String model, final boolean modelMode, final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = null;
    if (vectorIndex != -1) {
      imodState = get(key, axisID, vectorIndex);
    }
    if (imodState == null) {
      vectorIndex = newImod(key, axisID, file);
      imodState = get(key, axisID, vectorIndex);
    }
    if (imodState != null) {
      imodState.open(model, modelMode, menuOptions);
    }
    return vectorIndex;
  }

  public final int open(String key, final File file, final AxisID axisID, int vectorIndex,
    final File modelFile, final boolean modelMode, final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = null;
    if (vectorIndex != -1) {
      imodState = get(key, axisID, vectorIndex);
    }
    if (imodState == null) {
      vectorIndex = newImod(key, axisID, file);
      imodState = get(key, axisID, vectorIndex);
    }
    if (imodState != null) {
      if (modelFile != null) {
        imodState.open(modelFile.getAbsolutePath(), modelMode, menuOptions);
      }
      else {
        imodState.open(menuOptions);
      }
    }
    return vectorIndex;
  }

  public final int open(String key, final File file, int vectorIndex,
    final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = null;
    if (vectorIndex != -1) {
      imodState = get(key, vectorIndex);
    }
    if (imodState == null) {
      vectorIndex = newImod(key, file);
      imodState = get(key, vectorIndex);
    }
    if (imodState != null) {
      imodState.open(menuOptions);
    }
    return vectorIndex;
  }

  public final void openModel(String key, final AxisID axisID, final int vectorIndex,
    final String model, final boolean modelMode)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = null;
    if (vectorIndex != -1) {
      imodState = get(key, axisID, vectorIndex);
    }
    if (imodState != null) {
      imodState.openModel(model, modelMode);
    }
  }

  public final int open(String key, final File file, final AxisID axisID, int vectorIndex,
    final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = null;
    if (vectorIndex != -1) {
      imodState = get(key, axisID, vectorIndex);
    }
    if (imodState == null) {
      vectorIndex = newImod(key, axisID, file);
      imodState = get(key, axisID, vectorIndex);
    }
    if (imodState != null) {
      imodState.open(menuOptions);
    }
    return vectorIndex;
  }

  /**
   * 
   * @param key
   * @param axisID
   * @param vectorIndex
   * @throws AxisTypeException
   * @throws SystemProcessException
   */
  public final void open(String key, final AxisID axisID, final int vectorIndex,
    final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID, vectorIndex);
    if (imodState == null) {
      throw new IllegalArgumentException(
        key + " was not created in " + axisType.toString() + " with axisID="
          + axisID.getExtension() + " at index " + vectorIndex);
    }
    imodState.open(menuOptions);
  }

  public final void open(String key, final int vectorIndex,
    final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, vectorIndex);
    if (imodState == null) {
      throw new IllegalArgumentException(
        key + " was not created in " + axisType.toString() + " at index " + vectorIndex);
    }
    imodState.open(menuOptions);
  }

  public final void open(String key, final int vectorIndex, final String model,
    final boolean modelMode, final Run3dmodMenuOptions menuOptions)
    throws AxisTypeException, SystemProcessException, IOException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, vectorIndex);
    if (imodState == null) {
      throw new IllegalArgumentException(
        key + " was not created in " + axisType.toString() + " at index " + vectorIndex);
    }
    imodState.open(model, modelMode, menuOptions);
  }

  public final void delete(String key, final int vectorIndex)
    throws AxisTypeException, IOException, SystemProcessException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, vectorIndex);
    if (imodState == null) {
      throw new IllegalArgumentException(
        key + " was not created in " + axisType.toString() + " at index " + vectorIndex);
    }
    imodState.quit();
    deleteImodState(key, vectorIndex);
  }

  public final boolean isOpen(final String key) throws AxisTypeException {
    return isOpen(key, null);
  }

  public final boolean isOpen(String key, final AxisID axisID) throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      return false;
    }
    return imodState.isOpen();
  }

  public final boolean isOpen(String key, final AxisID axisID, final String datasetName)
    throws AxisTypeException {
    if (key == null) {
      return false;
    }
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID, datasetName);
    if (imodState == null) {
      return false;
    }
    return imodState.isOpen();
  }

  public final boolean isOpen() throws AxisTypeException {
    if (imodMap.size() == 0) {
      return false;
    }
    Set set = imodMap.keySet();
    Iterator iterator = set.iterator();
    while (iterator.hasNext()) {
      Vector vector = getVector((String) iterator.next(), true);
      Iterator vectorInterator = vector.iterator();
      while (vectorInterator.hasNext()) {
        ImodState imodState = (ImodState) vectorInterator.next();
        if (imodState != null && imodState.isOpen()) {
          return true;
        }
      }
    }
    return false;
  }

  public final String getModelName(String key, final AxisID axisID)
    throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      return "";
    }
    return imodState.getModelName();
  }

  public final Vector getRubberbandCoordinates(String key)
    throws AxisTypeException, IOException, SystemProcessException {
    key = getPrivateKey(key);
    ImodState imodState = get(key);
    if (imodState == null) {
      UIHarness.INSTANCE.openMessageDialog(manager, "3dmod is not running.",
        "3dmod Warning", AxisID.ONLY);
      return null;
    }
    return imodState.getRubberbandCoordinates();
  }

  public final Vector getSlicerAngles(String key, final int vectorIndex)
    throws AxisTypeException, IOException, SystemProcessException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, vectorIndex);
    if (imodState == null || !imodState.isOpen()) {
      UIHarness.INSTANCE.openMessageDialog(manager, "3dmod is not running.",
        "3dmod Warning", AxisID.ONLY);
      return null;
    }
    return imodState.getSlicerAngles();
  }

  public final void quit(final String key)
    throws AxisTypeException, IOException, SystemProcessException {
    quit(key, null);
  }

  public final void quit(String key, final AxisID axisID)
    throws AxisTypeException, IOException, SystemProcessException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState != null) {
      imodState.quit();
    }
  }

  public final void quit(String key, final AxisID axisID, final String datasetName)
    throws AxisTypeException, IOException, SystemProcessException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID, datasetName);
    if (imodState != null) {
      imodState.quit();
    }
  }

  public final void quitAll(final String key, final AxisID axisID)
    throws AxisTypeException, IOException, SystemProcessException {
    Vector imodStateVector = getVector(getPrivateKey(key), axisID);
    if (imodStateVector == null || imodStateVector.size() == 0) {
      return;
    }
    for (int i = 0; i < imodStateVector.size(); i++) {
      ImodState imodState = (ImodState) imodStateVector.get(i);
      if (imodState != null && imodState.isOpen()) {
        imodState.quit();
        try {
          Thread.sleep(500);
        }
        catch (InterruptedException e) {}
      }
    }
  }

  public final void quit() throws AxisTypeException, IOException, SystemProcessException {
    if (imodMap.size() == 0) {
      return;
    }
    Set set = imodMap.keySet();
    Iterator iterator = set.iterator();
    while (iterator.hasNext()) {
      Vector vector = getVector((String) iterator.next(), true);
      int size = vector.size();
      for (int i = 0; i < size; i++) {
        ImodState imodState = (ImodState) vector.get(i);
        if (imodState != null) {
          imodState.quit();
        }
      }
    }
  }

  final void processRequest() throws AxisTypeException {
    if (imodMap.size() == 0) {
      return;
    }
    Set set = imodMap.keySet();
    Iterator iterator = set.iterator();
    while (iterator.hasNext()) {
      Vector vector = getVector((String) iterator.next(), true);
      int size = vector.size();
      for (int i = 0; i < size; i++) {
        ImodState imodState = (ImodState) vector.get(i);
        if (imodState != null) {
          imodState.processRequest();
        }
      }
    }
  }

  public final void disconnect() {
    if (imodMap.size() == 0) {
      return;
    }
    Set set = imodMap.keySet();
    Iterator iterator = set.iterator();
    while (iterator.hasNext()) {
      try {
        Vector vector = getVector((String) iterator.next(), true);
        int size = vector.size();
        for (int i = 0; i < size; i++) {
          try {
            ImodState imodState = (ImodState) vector.get(i);
            if (imodState != null && imodState.isOpen()) {
              imodState.disconnect();
            }
          }
          catch (Throwable e) {
            e.printStackTrace();
          }
        }
      }
      catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }

  public final void setSwapYZ(String key, final AxisID axisID, final boolean swapYZ)
    throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    imodState.setSwapYZ(swapYZ);
  }

  public final void setFile(String key, final AxisID axisID, final File file)
    throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    imodState.setFile(file);
  }

  public final void setSwapYZ(String key, final File file, final boolean swapYZ)
    throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key);
    if (imodState == null) {
      newImod(key, file);
      imodState = get(key);
    }
    imodState.setSwapYZ(swapYZ);
  }

  public final void setOpenBeadFixer(String key, final AxisID axisID,
    final boolean openBeadFixer) throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      if (imodState.isUseModv()) {
        throw new UnsupportedOperationException(
          "The Bead Fixer cannot be opened in 3dmodv");
      }
      imodState.setOpenBeadFixer(openBeadFixer);
    }
  }

  public final void setOpenSurfContPoint(String key, final AxisID axisID,
    final boolean open) throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setOpenSurfContPoint(open);
    }
  }

  public final void setAutoCenter(final String key, final AxisID axisID,
    final boolean autoCenter) throws AxisTypeException {
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setAutoCenter(autoCenter);
    }
  }

  public final void setSkipList(final String key, final AxisID axisID,
    final String skipList) throws AxisTypeException {
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setSkipList(skipList);
    }
  }

  public final void setDeleteAllSections(final String key, final AxisID axisID,
    final boolean on) throws AxisTypeException {
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setDeleteAllSections(on);
    }
  }

  public final void setBeadfixerMode(final String key, final AxisID axisID,
    final ImodProcess.BeadFixerMode mode) throws AxisTypeException {
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setBeadfixerMode(mode);
    }
  }

  public final void setOpenLog(final String key, final AxisID axisID,
    final boolean openLog, final String logName) throws AxisTypeException {
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setOpenLog(openLog, logName);
    }
  }

  public final void reopenLog(final String key, final AxisID axisID)
    throws AxisTypeException, SystemProcessException, IOException {
    ImodState imodState = get(key, axisID);
    if (imodState == null || !imodState.isOpen()) {
      return;
    }
    imodState.reopenLog();
  }

  public final void setOpenLogOff(final String key, final AxisID axisID)
    throws AxisTypeException {
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setOpenLogOff();
    }
  }

  public final void setNewContours(final String key, final AxisID axisID,
    final boolean newContours) throws AxisTypeException {
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setNewContours(newContours);
    }
  }

  public final void setBinning(String key, final AxisID axisID, final int binning)
    throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setBinning(binning);
    }
  }

  public final void setTiltFile(String key, final AxisID axisID, final String tiltFile)
    throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setTiltFile(tiltFile);
    }
  }

  public final void resetTiltFile(String key, final AxisID axisID)
    throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.resetTiltFile();
    }
  }

  public final void setBinning(String key, final int binning) throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key);
    if (imodState == null) {
      newImod(key);
      imodState = get(key);
    }
    if (imodState != null) {
      imodState.setBinning(binning);
    }
  }

  public final void setBinning(String key, final int vectorIndex, final int binning)
    throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, vectorIndex);
    if (imodState == null) {
      throw new IllegalArgumentException(
        key + " was not created in " + axisType.toString() + " at index " + vectorIndex);
    }
    imodState.setBinning(binning);
  }

  public final void setBinningXY(String key, final int binning) throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key);
    if (imodState == null) {
      newImod(key);
      imodState = get(key);
    }
    if (imodState != null) {
      imodState.setBinningXY(binning);
    }
  }

  public final void setContinuousListenerTarget(String key, final AxisID axisID,
    final ContinuousListenerTarget continuousListenerTarget) throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    imodState.setContinuousListenerTarget(continuousListenerTarget);
  }

  public final void setBinningXY(String key, final int vectorIndex, final int binning)
    throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, vectorIndex);
    if (imodState == null) {
      throw new IllegalArgumentException(
        key + " was not created in " + axisType.toString() + " at index " + vectorIndex);
    }
    imodState.setBinningXY(binning);
  }

  public final void setOpenContours(String key, final AxisID axisID,
    final boolean openContours) throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setOpenContours(openContours);
    }
  }

  public final void setStartNewContoursAtNewZ(String key, final AxisID axisID,
    final boolean startNewContoursAtNewZ) throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setStartNewContoursAtNewZ(startNewContoursAtNewZ);
    }
  }

  public final void setPointLimit(String key, final AxisID axisID, final int pointLimit)
    throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setPointLimit(pointLimit);
    }
  }

  /**
   * Use this when opening a model newly created by software that doesn't have previous
   * contrast settings in it.
   * @param key
   * @param axisID
   * @param preserveContrast
   * @throws AxisTypeException
   */
  public final void setPreserveContrast(String key, final AxisID axisID,
    final boolean preserveContrast) throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setPreserveContrast(preserveContrast);
    }
  }

  public final void setFrames(String key, final AxisID axisID, final boolean frames)
    throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setFrames(frames);
    }
  }

  public final void setPieceListFileName(String key, final AxisID axisID,
    final String pieceListFileName) throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setPieceListFileName(pieceListFileName);
    }
  }

  public final void setMontageSeparation(String key, final AxisID axisID)
    throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setMontageSeparation();
    }
  }

  public final void setInterpolation(String key, final AxisID axisID,
    final boolean interpolation) throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setInterpolation(interpolation);
    }
  }

  public final void setWorkingDirectory(String key, final AxisID axisID,
    final int vectorIndex, final File workingDirectory) throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID, vectorIndex);
    if (imodState != null) {
      imodState.setWorkingDirectory(workingDirectory);
    }
  }

  public final void setPieceListFileName(String key, final AxisID axisID,
    final int vectorIndex, final String pieceListFileName) throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID, vectorIndex);
    if (imodState == null) {
      newImod(key, axisID);
      imodState = get(key, axisID);
    }
    if (imodState != null) {
      imodState.setPieceListFileName(pieceListFileName);
    }
  }

  public final void stopRequestHandler() {
    if (requestHandler != null) {
      requestHandler.stop();
    }
  }

  /**
   * Used to prevent warnings about a stale file from popping up over and over.
   * Returns true if ImodState.isWarnedStaleFile returns false.  Also turns on
   * ImodState.warnedStaleFile if it is off.
   * @param key
   * @param axisID
   * @return
   * @throws AxisTypeException
   */
  public final boolean warnStaleFile(String key, final AxisID axisID)
    throws AxisTypeException {
    key = getPrivateKey(key);
    ImodState imodState = get(key, axisID);
    if (imodState != null && !imodState.isWarnedStaleFile() && imodState.isOpen()) {
      imodState.setWarnedStaleFile(true);
      return true;
    }
    return false;
  }

  final Vector newVector(final ImodState imodState) {
    Vector vector = new Vector(1);
    vector.add(imodState);
    return vector;
  }

  private final Vector newVector(final String key, final String fileExtension,
    final AxisID axisID, final String datasetName) {
    return newVector(newImodState(key, fileExtension, axisID, datasetName));
  }

  private final Vector newVector(final String key, final File file) {
    return newVector(newImodState(key, file));
  }

  private final Vector newVector(final String key, final AxisID axisID, final File file) {
    return newVector(newImodState(key, axisID, file));
  }

  private final Vector newVector(final String key, final String[] fileNameArray) {
    return newVector(newImodState(key, fileNameArray));
  }

  private final Vector newVector(final String key, final AxisID axisID,
    final File[] fileList) {
    return newVector(newImodState(key, axisID, fileList));
  }

  private final Vector newVector(final String key, final String[] fileNameArray,
    final String subdirName) {
    return newVector(newImodState(key, fileNameArray, subdirName));
  }

  private final ImodState newImodState(final String key) {
    return newImodState(key, null, null, null, null, null, null, null);
  }

  private final ImodState newImodState(final String key, final AxisID axisID) {
    return newImodState(key, null, axisID, null, null, null, null, null);
  }

  private final ImodState newImodState(final String key, final AxisID axisID,
    final File[] fileList) {
    return newImodState(key, null, axisID, null, null, null, null, fileList);
  }

  private final ImodState newImodState(final String key, final String fileExtension,
    final AxisID axisID, final String datasetName) {
    return newImodState(key, fileExtension, axisID, datasetName, null, null, null, null);
  }

  private final ImodState newImodState(final String key, final File file) {
    return newImodState(key, null, null, null, file, null, null, null);
  }

  private final ImodState newImodState(final String key, final AxisID axisID,
    final File file) {
    return newImodState(key, null, axisID, null, file, null, null, null);
  }

  private final ImodState newImodState(final String key, final String[] fileNameArray) {
    return newImodState(key, null, null, null, null, fileNameArray, null, null);
  }

  private final ImodState newImodState(final String key, final String[] fileNameArray,
    final String subdirName) {
    return newImodState(key, null, null, null, null, fileNameArray, subdirName, null);
  }

  final ImodState get(final String key) throws AxisTypeException {
    Vector vector = getVector(key);
    if (vector == null) {
      return null;
    }
    return (ImodState) vector.lastElement();
  }

  final ImodState get(final String key, final AxisID axisID) throws AxisTypeException {
    Vector vector = getVector(key, axisID);
    if (vector == null) {
      return null;
    }
    return (ImodState) vector.lastElement();
  }

  private final ImodState get(final String key, final AxisID axisID,
    final String datasetName) throws AxisTypeException {
    Vector vector = getVector(key, axisID);
    if (vector == null) {
      return null;
    }
    Iterator iterator = vector.iterator();
    while (iterator.hasNext()) {
      ImodState imodState = (ImodState) iterator.next();
      if (imodState.getDatasetName().equals(datasetName)) {
        return imodState;
      }
    }
    return null;
  }

  private final ImodState get(final String key, final AxisID axisID, final File file)
    throws AxisTypeException {
    Vector vector = getVector(key, axisID);
    if (vector == null) {
      return null;
    }
    Iterator iterator = vector.iterator();
    while (iterator.hasNext()) {
      ImodState imodState = (ImodState) iterator.next();
      if (imodState.getDatasetName().equals(file.getAbsolutePath())) {
        return imodState;
      }
    }
    return null;
  }

  private final ImodState get(final String key, final AxisID axisID,
    final int vectorIndex) throws AxisTypeException {
    Vector vector = getVector(key, axisID);
    if (vector == null) {
      return null;
    }
    return (ImodState) vector.get(vectorIndex);
  }

  private final ImodState get(String key, int vectorIndex) throws AxisTypeException {
    Vector vector = getVector(key);
    if (vector == null || vectorIndex >= vector.size() || vectorIndex < 0) {
      return null;
    }
    return (ImodState) vector.get(vectorIndex);
  }

  private final void deleteImodState(String key, int vectorIndex)
    throws AxisTypeException {
    Vector vector = getVector(key);
    if (vector == null) {
      return;
    }
    vector.remove(vectorIndex);
  }

  private final Vector getVector(String key) throws AxisTypeException {
    Vector vector;
    if (!useMap) {
      throw new UnsupportedOperationException(
        "This operation is not supported when useMap is false");
    }
    if (axisType == AxisType.SINGLE_AXIS && isDualAxisOnly(key)) {
      throw new AxisTypeException(key + " cannot be found in " + axisType.toString());
    }
    if (isDualAxisOnly(key) && isPerAxis(key)) {
      throw new UnsupportedOperationException(
        key + " cannot be found without axisID information");
    }
    if (isPerAxis(key)) {
      vector = (Vector) imodMap.get(key + AxisID.ONLY.getExtension());
    }
    else {
      vector = (Vector) imodMap.get(key);
    }
    if (vector == null) {
      return null;
    }
    return (Vector) vector;
  }

  private final Vector getVector(final String key, final boolean axisIdInKey)
    throws AxisTypeException {
    if (!axisIdInKey) {
      return getVector(key);
    }
    return (Vector) imodMap.get(key);
  }

  private final Vector getVector(final String key, AxisID axisID)
    throws AxisTypeException {
    Vector vector;
    if (axisID == null) {
      return getVector(key);
    }
    if (!useMap) {
      throw new UnsupportedOperationException(
        "This operation is not supported when useMap is false");
    }
    if (axisType == AxisType.SINGLE_AXIS) {
      if (isDualAxisOnly(key)) {
        throw new AxisTypeException(key + " cannot be found in " + axisType.toString());
      }
      // Correct axis
      if (axisID == AxisID.FIRST) {
        axisID = AxisID.ONLY;
      }
    }
    if (!isPerAxis(key)) {
      vector = (Vector) imodMap.get(key);
    }
    else {
      vector = (Vector) imodMap.get(key + axisID.getExtension());
    }
    if (vector == null) {
      return null;
    }
    return (Vector) vector;
  }
}
