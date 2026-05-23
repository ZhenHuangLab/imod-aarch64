package etomo.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import etomo.BaseManager;
import etomo.logic.ProcessorType;
import etomo.storage.autodoc.AutodocFactory;
import etomo.storage.autodoc.ReadOnlyAttribute;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.storage.autodoc.ReadOnlySection;
import etomo.storage.autodoc.SectionLocation;
import etomo.type.AxisID;
import etomo.type.ConstEtomoNumber;
import etomo.type.EtomoNumber;
import etomo.type.InterfaceType;
import etomo.util.EnvironmentVariable;

/**
 * <p>Description:</p>
 * <p>Represents the cpu.adoc file except the mount rules, which
 * are handled by RemotePath.</p>
 * 
 * <p>Assumptions:</p>
 * <p>The Computer section names in cpu.adoc must be unique.</p>
 * 
 * <p>Copyright: Copyright 2006 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * @threadsafe
 * @immutable
 * @singleton
 */
public class CpuAdoc {
  public static final String COMPUTER_SECTION_TYPE = "Computer";
  public static final String QUEUE_SECTION_TYPE = "Queue";
  static final String GPU_KEY = "gpu";
  private static final String SPEED_KEY = "speed";
  private static final String MEMORY_KEY = "memory";
  private static final int MIN_NICE_DEFAULT = 0;

  public static final CpuAdoc INSTANCE = new CpuAdoc();

  // list of computer names
  private final ArrayList<String> computerList = new ArrayList<String>();
  private final Map<String, Node> computerMap = new Hashtable<String, Node>();
  private final ArrayList<String> queueList = new ArrayList<String>();
  private final Map<String, Node> queueMap = new Hashtable<String, Node>();
  private final EtomoNumber minNice = new EtomoNumber();
  private final EtomoNumber maxTilt = new EtomoNumber();
  private final EtomoNumber maxVolcombine = new EtomoNumber();
  private final String user = System.getProperty("user.name");
  private final OrListMap orListMap = new OrListMap();

  private boolean separateChunks = false;
  private boolean usersColumn = false;
  private String speedUnits = "";
  private String memoryUnits = "";
  private String gpuSpeedUnits = "";// Not in use - has not been added to cpuadoc man page
  private String gpuMemoryUnits = "";// Not in use - has not been added to cpuadoc man
                                     // page
  private String[] loadUnits = new String[0];
  private boolean envVar = false;
  private boolean userConfig = false;
  private boolean loaded = false;
  private boolean exists = false;

  private CpuAdoc() {
    minNice.setDisplayValue(MIN_NICE_DEFAULT);
    minNice.setDefault(MIN_NICE_DEFAULT);
  }

  public boolean isSeparateChunks() {
    load();
    return separateChunks;
  }

  public boolean isUsersColumn() {
    load();
    return usersColumn;
  }

  public int getMinNice() {
    load();
    return minNice.getInt();
  }

  public String getSpeedUnits() {
    load();
    return speedUnits;
  }

  public String getGpuSpeedUnits() {
    return gpuSpeedUnits;
  }

  public String getMemoryUnits() {
    load();
    return memoryUnits;
  }

  public String getGpuMemoryUnits() {
    load();
    return gpuMemoryUnits;
  }

  public String[] getLoadUnitsArray() {
    load();
    return loadUnits;
  }

  public int getLoadUnits() {
    load();
    if (loadUnits != null) {
      return loadUnits.length;
    }
    return 0;
  }

  public ConstEtomoNumber getMaxTilt() {
    load();
    return maxTilt;
  }

  public ConstEtomoNumber getMaxVolcombine() {
    load();
    return maxVolcombine;
  }

  /**
   * Returns true if there is no computerList entries, or false if any of the computerList
   * entries have a non-local-only GPU.
   * @param manager
   * @param axisID
   * @param propertyUserDir
   * @param ignoredNode if this node is non-null and present in the list, the list is still considered empty.  Parameter may be null
   * @return
   */
  boolean isGpuComputerListEmpty(final Node ignoredNode) {
    load();
    if (computerList.isEmpty()) {
      return true;
    }
    for (int i = 0; i < computerList.size(); i++) {
      Node node = computerMap.get(computerList.get(i));
      if ((ignoredNode == null || node != ignoredNode
        || (node == ignoredNode && node.getGpuNumber() > 1)) && node.isGpu()) {
        return false;
      }
    }
    return true;
  }

  boolean isComputerListEmpty() {
    load();
    return computerList.isEmpty();
  }

  boolean isQueueListEmpty() {
    load();
    return queueList.isEmpty();
  }

  /**
   * Return computerMap.get(computerList[index]) or null.
   * @param index
   * @param axisID
   * @param propertyUserDir
   * @param managerKey
   * @return
   */
  Node getComputer(int index) {
    load();
    return computerMap.get(computerList.get(index));
  }

  /**
   * Get the Computer section for the current computer.
   * @param axisID
   * @param propertyUserDir
   * @param managerKey
   * @return
   */
  Node getLocalHostComputer(BaseManager manager, AxisID axisID, String propertyUserDir) {
    // Search for "localhost":
    Node localHost = getComputer(Node.LOCAL_HOST_NAME);
    if (localHost != null) {
      return localHost;
    }
    // Search for the computer name:
    String localHostName = Network.getLocalHostName(manager, axisID, propertyUserDir);
    if (localHostName == null) {
      return null;
    }
    localHost = getComputer(localHostName);
    if (localHost != null) {
      return localHost;
    }
    // Local host not found. Try removing everything from the local host name
    // starting with the first ".".
    int index = localHostName.indexOf('.');
    if (index != -1) {
      localHostName = localHostName.substring(0, index);
      localHost = getComputer(localHostName);
    }
    return localHost;
  }

  Node getQueue(int index) {
    load();
    return queueMap.get(queueList.get(index));
  }

  /**
   * Get queue from queueMap by name.  Return null if not found.
   * @param name
   * @param axisID
   * @param propertyUserDir
   * @param managerKey
   * @return
   */
  Node getQueue(String name) {
    load();
    return queueMap.get(name);
  }

  /**
   * Get computer from computerMap by name.  Return null if not found.
   * @param name
   * @param axisID
   * @param propertyUserDir
   * @param managerKey
   * @return
   */
  Node getComputer(String name) {
    load();
    if (name != null) {
      return computerMap.get(name);
    }
    return null;
  }

  int getComputerListSize() {
    load();
    return computerList.size();
  }

  int getQueueListSize() {
    load();
    return queueList.size();
  }

  boolean exists() {
    load();
    return exists;
  }

  private synchronized void load() {
    if (loaded) {
      return;
    }
    loaded = true;
    ReadOnlyAutodoc autodoc = getAutodoc();
    if (autodoc != null && autodoc.exists()) {
      exists = true;
      separateChunks = loadBooleanAttribute(autodoc, "separate-chunks");
      loadAttribute(minNice, autodoc, "min", "nice");
      usersColumn = loadBooleanAttribute(autodoc, "users-column");
      ReadOnlyAttribute attrib = autodoc.getAttribute("max");
      if (attrib != null) {
        loadAttribute(maxTilt, attrib, "tilt");
        loadAttribute(maxVolcombine, attrib, "volcombine");
      }
      attrib = autodoc.getAttribute("units");
      if (attrib != null) {
        speedUnits = loadStringAttribute(attrib, SPEED_KEY);
        memoryUnits = loadStringAttribute(attrib, MEMORY_KEY);
        loadUnits = loadStringListAttribute(attrib, "load");
        attrib = attrib.getAttribute(GPU_KEY);
        if (attrib != null) {
          gpuSpeedUnits = loadStringAttribute(attrib, SPEED_KEY);
          gpuMemoryUnits = loadStringAttribute(attrib, MEMORY_KEY);
        }
      }
      loadComputers(autodoc);
      loadQueues(autodoc);
    }
  }

  private String loadStringAttribute(ReadOnlyAttribute attrib, String key) {
    if (attrib == null) {
      return "";
    }
    attrib = attrib.getAttribute(key);
    if (attrib == null) {
      return "";
    }
    return attrib.getValue();
  }

  private void loadAttribute(EtomoNumber number, ReadOnlyAttribute attrib, String key) {
    number.reset();
    if (attrib == null) {
      return;
    }
    attrib = attrib.getAttribute(key);
    if (attrib == null) {
      return;
    }
    number.set(attrib.getValue());
  }

  private String[] loadStringListAttribute(ReadOnlyAttribute attrib, String key) {
    if (attrib == null) {
      return new String[0];
    }
    attrib = attrib.getAttribute(key);
    if (attrib == null) {
      return new String[0];
    }
    String list = attrib.getValue();
    if (list == null) {
      return new String[0];
    }
    return list.split("\\s*,\\s*");
  }

  private ReadOnlyAutodoc getAutodoc() {
    ReadOnlyAutodoc autodoc = null;
    try {
      autodoc = AutodocFactory.getInstance(null, AutodocFactory.CPU, null, false);
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    if (autodoc == null) {
      System.err.println("Missing $" + EnvironmentVariable.CALIB_DIR + "/cpu.adoc file.\n"
        + "Parallel processing cannot be used.\n" + "See $IMOD_DIR/autodoc/cpu.adoc.");
    }
    return autodoc;
  }

  private void loadComputers(ReadOnlyAutodoc autodoc) {
    SectionLocation location = autodoc.getSectionLocation(COMPUTER_SECTION_TYPE);
    if (location == null) {
      return;
    }
    ReadOnlySection section = null;
    while ((section = autodoc.nextSection(location)) != null) {
      Node computer = Node.getComputerInstance();
      computer.load(section);
      if (!computer.isExcludedUser(user)) {
        String name = section.getName();
        computerList.add(name);
        computerMap.put(name, computer);
        orListMap.addNode(ProcessorType.CPU, computer);
      }
    }
  }

  public boolean isNumberGt1(final InterfaceType interfaceType,
    final ProcessorType processorType) {
    load();
    return orListMap.isValueTrue(interfaceType, processorType, OrValue.NUMBER_GT_1);
  }

  public boolean isGpuGt1(final InterfaceType interfaceType,
    final ProcessorType processorType) {
    load();
    return orListMap.isValueTrue(interfaceType, processorType, OrValue.GPU_GT_1);
  }

  public boolean isType(final InterfaceType interfaceType,
    final ProcessorType processorType) {
    load();
    if (processorType == ProcessorType.GPU) {
      return false;
    }
    return orListMap.isValueTrue(interfaceType, processorType, OrValue.CPU_TYPE);
  }

  public boolean isSpeed(final InterfaceType interfaceType,
    final ProcessorType processorType) {
    load();
    if (processorType == ProcessorType.GPU) {
      return false;
    }
    return orListMap.isValueTrue(interfaceType, processorType, OrValue.SPEED);
  }

  public boolean isMemory(final InterfaceType interfaceType,
    final ProcessorType processorType) {
    load();
    if (processorType == ProcessorType.GPU) {
      return false;
    }
    return orListMap.isValueTrue(interfaceType, processorType, OrValue.MEMORY);
  }

  public boolean isOs(final InterfaceType interfaceType,
    final ProcessorType processorType) {
    load();
    return orListMap.isValueTrue(interfaceType, processorType, OrValue.OS);
  }

  public boolean isGpuType(final InterfaceType interfaceType,
    final ProcessorType processorType) {
    load();
    if (processorType == ProcessorType.CPU) {
      return false;
    }
    return orListMap.isValueTrue(interfaceType, processorType, OrValue.GPU_TYPE);
  }

  public boolean isGpuSpeed(final InterfaceType interfaceType,
    final ProcessorType processorType) {
    load();
    if (processorType == ProcessorType.CPU) {
      return false;
    }
    return orListMap.isValueTrue(interfaceType, processorType, OrValue.GPU_SPEED);
  }

  public boolean isGpuMemory(final InterfaceType interfaceType,
    final ProcessorType processorType) {
    load();
    if (processorType == ProcessorType.CPU) {
      return false;
    }
    return orListMap.isValueTrue(interfaceType, processorType, OrValue.GPU_MEMORY);
  }

  public boolean isGpuNcores(final InterfaceType interfaceType,
    final ProcessorType processorType) {
    load();
    if (processorType == ProcessorType.CPU) {
      return false;
    }
    return orListMap.isValueTrue(interfaceType, processorType, OrValue.GPU_NCORES);
  }

  private void loadQueues(ReadOnlyAutodoc autodoc) {
    SectionLocation location = autodoc.getSectionLocation(QUEUE_SECTION_TYPE);
    if (location == null) {
      return;
    }
    ReadOnlySection section = null;
    while ((section = autodoc.nextSection(location)) != null) {
      Node queue = Node.getQueueInstance();
      queue.load(section);
      if (!queue.isExcludedUser(user)) {
        String name = section.getName();
        queueList.add(name);
        queueMap.put(name, queue);
        orListMap.addNode(ProcessorType.QUEUE, queue);
      }
    }
  }

  private boolean loadBooleanAttribute(ReadOnlyAutodoc autodoc, String key) {
    ReadOnlyAttribute attrib = autodoc.getAttribute(key);
    if (attrib != null && (attrib.getValue() == null || !attrib.getValue().equals("0"))) {
      return true;
    }
    return false;
  }

  private void loadAttribute(EtomoNumber number, ReadOnlyAutodoc autodoc, String key1,
    String key2) {
    number.reset();
    ReadOnlyAttribute attrib = autodoc.getAttribute(key1);
    if (attrib == null) {
      return;
    }
    attrib = attrib.getAttribute(key2);
    if (attrib == null) {
      return;
    }
    number.set(attrib.getValue());
  }

  private String loadStringAttribute(ReadOnlyAutodoc autodoc, String key1, String key2) {
    ReadOnlyAttribute attrib = autodoc.getAttribute(key1);
    if (attrib == null) {
      return "";
    }
    attrib = attrib.getAttribute(key2);
    if (attrib == null) {
      return "";
    }
    return attrib.getValue();
  }

  private String[] loadStringListAttribute(ReadOnlyAutodoc autodoc, String key1,
    String key2) {
    ReadOnlyAttribute attrib = autodoc.getAttribute(key1);
    if (attrib == null) {
      return new String[0];
    }
    attrib = attrib.getAttribute(key2);
    if (attrib == null) {
      return new String[0];
    }
    String list = attrib.getValue();
    if (list == null) {
      return new String[0];
    }
    return list.split("\\s*,\\s*");
  }

  /**
   * Keeps track of boolean values derived from the cpu.adoc nodes.  Each value is true
   * if it is true for any node that meets the criteria.  The orMap member variable will
   * contain a set of these values for each sectionType (computer and queue), and each
   * excluded interface mentioned in the cpu.adoc.  This class does not take the user list
   * into account.
   */
  private static final class OrListMap {
    /**
     * interfaceTypeSet contains section type + interface
     */
    private Set<String> interfaceTypeSet = null;
    /**
     * orMap: key is section type + interface, value is an orList
     */
    private Map<String, boolean[]> orMap = null;

    private OrListMap() {}

    /**
     * Returns true of the orValue for this processor and interface type is true.
     * @param interfaceType
     * @param processorType
     * @param orValue
     * @return
     */
    private boolean isValueTrue(final InterfaceType interfaceType,
      final ProcessorType processorType, final OrValue orValue) {
      if (orValue == null || orMap == null) {
        return false;
      }
      // First look for the interfaceType-level orList
      boolean[] orList = orMap.get(getKey(interfaceType, processorType));
      if (orList != null) {
        return orList[orValue.index];
      }
      // If the interfaceType-level orList doesn't exist, use the processor-level orList.
      orList = orMap.get(getKey(processorType));
      if (orList != null) {
        return orList[orValue.index];
      }
      return false;
    }

    /**
     * Add the boolean values contained in the node parameter.
     * @param processorType
     * @param node
     */
    private void addNode(final ProcessorType processorType, final Node node) {
      if (node == null) {
        return;
      }
      String sectionKey = getKey(processorType);
      // If there is an excluded interface, and hasn't been seen before, add it to the
      // interface name list.
      InterfaceType excludeInterfaceType = node.getExcludeInterface();
      boolean[] orList = null;
      if (excludeInterfaceType != null) {
        // The excluded interface name is new - add it to the interface name list.
        if (interfaceTypeSet == null
          || !interfaceTypeSet.contains(excludeInterfaceType.toString())) {
          if (interfaceTypeSet == null) {
            interfaceTypeSet = new HashSet<String>();
          }
          interfaceTypeSet.add(getKey(excludeInterfaceType, processorType));
          // The new interface name gets a copy of the orList for this processorType.
          if (orMap != null) {
            orList = orMap.get(sectionKey);
            if (orList != null) {
              boolean[] newOrList = createOrList();
              orMap.put(getKey(excludeInterfaceType, processorType), newOrList);
              for (int i = 0; i < OrValue.ARRAY.length; i++) {
                newOrList[i] = orList[i];
              }
            }
          }
        }
      }
      // Set any true boolean values in the processor-level orList
      boolean[] valueList = createOrList();
      boolean trueValueFound = false;
      for (int i = 0; i < OrValue.ARRAY.length; i++) {
        // Hold on to the values from the node.
        valueList[i] = OrValue.ARRAY[i].isTrue(node);
        if (valueList[i]) {
          trueValueFound = true;
          if (orList == null && orMap != null) {
            orList = orMap.get(sectionKey);
          }
          if (orList == null) {
            if (orMap == null) {
              orMap = new HashMap<String, boolean[]>();
            }
            orList = createOrList();
            orMap.put(sectionKey, orList);
          }
          orList[i] = true;
        }
      }
      // Set any true boolean values in any interface-level orLists
      if (trueValueFound && orMap != null && interfaceTypeSet != null) {
        // Reusing the orList variable
        orList = null;
        Iterator<String> iterator = interfaceTypeSet.iterator();
        while (iterator.hasNext()) {
          InterfaceType interfaceType = decodeKey(processorType, iterator.next());
          if (interfaceType != null && interfaceType != excludeInterfaceType) {
            for (int i = 0; i < valueList.length; i++) {
              if (valueList[i]) {
                String key = getKey(interfaceType, processorType);
                orList = orMap.get(key);
                if (orList == null) {
                  orList = createOrList();
                  orMap.put(key, orList);
                }
                orList[i] = true;
              }
            }
          }
        }
      }
    }

    private boolean[] createOrList() {
      boolean[] orList = new boolean[OrValue.ARRAY.length];
      for (int i = 0; i < orList.length; i++) {
        orList[i] = false;
      }
      return orList;
    }

    /**
     * Returns the queue section type when the queue processor type is passed in.
     * Otherwise returns the computer section type.
     * @param processorType
     * @return
     */
    private String getKey(final ProcessorType processorType) {
      if (processorType == ProcessorType.QUEUE) {
        return QUEUE_SECTION_TYPE;
      }
      return COMPUTER_SECTION_TYPE;
    }

    private String getKey(final InterfaceType interfaceType,
      final ProcessorType processorType) {
      return getKey(processorType)
        + (interfaceType != null ? "." + interfaceType.toString() : "");
    }

    /**
     * Returns the interface type in the key, if the key contains a matching processor
     * type.
     * @param processorType
     * @param key
     * @return
     */
    private InterfaceType decodeKey(final ProcessorType processorType, final String key) {
      String firstPart = getKey(processorType);
      if (key == null || key.indexOf('.') == -1 || !key.startsWith(firstPart)) {
        return null;
      }
      String[] array = key.split("\\.");
      if (array == null || array.length < 2) {
        return null;
      }
      return InterfaceType.getInstance(array[1]);
    }
  }

  /**
   * An enum of boolean values derived from the cpu.adoc nodes.
   */
  private static final class OrValue {
    private static final int TOTAL = 10;// total number of class instances
    private static final OrValue[] ARRAY = new OrValue[TOTAL];

    private static int curIndex = 0;
    private static final OrValue CPU_TYPE = new OrValue(Node.TYPE_KEY, curIndex++);
    private static final OrValue NUMBER_GT_1 = new OrValue(Node.NUMBER_KEY, curIndex++);
    private static final OrValue GPU_GT_1 = new OrValue(Node.NUMBER_KEY, curIndex++);
    private static final OrValue MEMORY = new OrValue(Node.MEMORY_KEY, curIndex++);
    private static final OrValue OS = new OrValue(Node.OS_KEY, curIndex++);
    private static final OrValue SPEED = new OrValue(Node.SPEED_KEY, curIndex++);
    private static final OrValue GPU_TYPE = new OrValue(Node.TYPE_KEY, curIndex++);
    private static final OrValue GPU_MEMORY = new OrValue(Node.MEMORY_KEY, curIndex++);
    private static final OrValue GPU_NCORES = new OrValue(Node.NCORES_KEY, curIndex++);
    private static final OrValue GPU_SPEED = new OrValue(Node.SPEED_KEY, curIndex++);

    private final String key;
    private final int index;

    private OrValue(final String key, final int index) {
      this.key = key;
      this.index = index;
      ARRAY[index] = this;
    }

    private boolean isTrue(final Node node) {
      if (this == CPU_TYPE) {
        return node.isType();
      }
      if (this == NUMBER_GT_1) {
        return node.getNumber() > 1;
      }
      if (this == GPU_GT_1) {
        String[] gpuArray = node.getGpuDeviceArray();
        return gpuArray != null && gpuArray.length > 1;
      }
      if (this == MEMORY) {
        return node.isMemory();
      }
      if (this == OS) {
        return node.isOs();
      }
      if (this == SPEED) {
        return node.isSpeed();
      }
      if (this == GPU_TYPE) {
        return node.isGpuType();
      }
      if (this == GPU_MEMORY) {
        return node.isGpuMemory();
      }
      if (this == GPU_NCORES) {
        return node.isGpuNcores();
      }
      if (this == GPU_SPEED) {
        return node.isGpuSpeed();
      }
      return false;
    }
  }
}
/**
 * <p> $Log$
 * <p> Revision 1.23  2011/06/28 21:09:37  sueh
 * <p> Bug# 1480 Added isGpuComputerListEmpty.
 * <p>
 * <p> Revision 1.22  2011/06/23 14:59:11  sueh
 * <p> Bug# 1495 In getLocalHostComputer added a search for "localhost".
 * <p>
 * <p> Revision 1.21  2011/04/04 16:56:54  sueh
 * <p> bug# 1469 Added/modified GPU_KEY, gpuMemoryUnits, gpuSpeedUnits, MEMORY_KEY, load (refactored),
 * <p> loadAttribute, loadStringAttribute, loadStringListAttribute.  Removed UNITS_KEY.
 * <p>
 * <p> Revision 1.20  2011/02/22 04:32:04  sueh
 * <p> bug# 1437 Reformatting.
 * <p>
 * <p> Revision 1.19  2010/11/04 21:02:25  sueh
 * <p> bug# 1415 Changed MIN_NICE_DEFAULT to 0.
 * <p>
 * <p> Revision 1.18  2010/02/17 04:49:31  sueh
 * <p> bug# 1301 Using the manager instead of the manager key do pop up
 * <p> messages.
 * <p>
 * <p> Revision 1.17  2010/01/11 23:56:47  sueh
 * <p> bug# 1299 Removed responsibility anything other then cpu.adoc from
 * <p> CpuAdoc.  Placed responsibility for information about the network in the
 * <p> Network class.
 * <p>
 * <p> Revision 1.16  2009/03/17 00:44:22  sueh
 * <p> bug# 1186 Pass managerKey to everything that pops up a dialog.
 * <p>
 * <p> Revision 1.15  2009/02/04 23:29:40  sueh
 * <p> bug# 1158 Changed id and exceptions classes in LogFile.
 * <p>
 * <p> Revision 1.14  2008/07/19 00:28:49  sueh
 * <p> bug# 1125 Get rid of isSetByUser and isValid because they where always
 * <p> used together.  Have file, envVar, and userConfig booleans.  IsAvailable
 * <p> is true when one of these booleans is true.  SettingsDialog can also check
 * <p> whether file or envVar is true.  Added setting userConfig and the number
 * <p> of CPUs.
 * <p>
 * <p> Revision 1.13  2008/01/25 22:23:44  sueh
 * <p> bug# 1070 Added setByUser which is true when the user sets the cpu data with
 * <p> cpu.adoc or IMOD_PROCESSORS.
 * <p>
 * <p> Revision 1.12  2007/11/06 19:28:32  sueh
 * <p> bug# 1047 Allow parallel processing even when that is no cpu.adoc and
 * <p> IMOD_PROCESSORS is not set.
 * <p>
 * <p> Revision 1.11  2007/09/27 20:30:38  sueh
 * <p> bug# 1044 Retrieving Queue sections.
 * <p>
 * <p> Revision 1.10  2007/08/29 22:42:32  sueh
 * <p> bug# 1040 Fixing a bug in load(AxisID, BaseManager) where the value max
 * <p> volcombine value was loaded into maxTilt.
 * <p>
 * <p> Revision 1.9  2007/07/17 21:25:47  sueh
 * <p> bug# 1018 Move data reading and storage to CpuAdoc, except mount
 * <p> rules.
 * <p>
 * <p> Revision 1.8  2007/05/22 21:07:57  sueh
 * <p> bug# 999 Added class Computer, to hold section level data.  Added users.
 * <p>
 * <p> Revision 1.7  2007/05/21 22:29:13  sueh
 * <p> bug# 1000 Added excludeInterface and loadExcludeInterfaceMap().
 * <p>
 * <p> Revision 1.6  2007/05/21 18:16:27  sueh
 * <p> bug# 992 Fixed a bug in loadBoolean(); wasn't handle attrib.getValue()
 * <p> returning null.
 * <p>
 * <p> Revision 1.5  2007/05/21 18:10:27  sueh
 * <p> bug# 964 Added usersColumn.
 * <p>
 * <p> Revision 1.4  2007/05/18 23:52:22  sueh
 * <p> bug# 987 Made CpuAdoc thread-safe.  Added minNice.
 * <p>
 * <p> Revision 1.3  2007/03/21 18:10:49  sueh
 * <p> bug# 964 Moved Adoc classes out of the autodoc package because
 * <p> they not part of the autodoc.
 * <p>
 * <p> Revision 1.5  2007/03/15 21:46:20  sueh
 * <p> bug# 964 Added ReadOnlyAttribute, which is used as an interface for Attribute,
 * <p> unless the Attribute needs to be modified.
 * <p>
 * <p> Revision 1.4  2007/03/01 01:19:30  sueh
 * <p> bug# 964 Added LogFile to Autodoc.
 * <p>
 * <p> Revision 1.3  2006/07/21 22:11:49  sueh
 * <p> bug# 901 Getting the calibration directory environment variable name from
 * <p> EnvironmentVariable.
 * <p>
 * <p> Revision 1.2  2006/06/30 17:02:15  sueh
 * <p> Improved warning about missing cpu.adoc.
 * <p>
 * <p> Revision 1.1  2006/06/14 00:33:47  sueh
 * <p> bug# 852 Moved classes to the autodoc package that parse an autodoc or find
 * <p> attributes specific to a type of autdoc.
 * <p>
 * <p> Revision 1.1  2006/06/08 19:04:38  sueh
 * <p> bug# 867 Class to read the cpu autodoc.
 * <p> </p>
 */
