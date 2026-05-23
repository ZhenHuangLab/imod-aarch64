package etomo.type;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import etomo.util.Utilities;

/**
* <p>Description: Generates and stores IDs which are unique within an instance of the
* class.  Each ID is linked to a unique, non-null parameter string value, the instance-
* level uniqueness of which is enforced.  This string is a file name minus the extension.
* IDs are not modifiable and are never deleted.
* The string values may be modified.  The IDs can be used as stable keys for serializing.
* Each instance of TableReference needs a unique prefix string for its IDs.</p>
* 
* <p>IMPORTANT:  The constructor parameter idPrefix value is used as a key in data files,
* so changing its value requires backwards compatibility code to be added.</p>
* 
* <p>Properties example:</p>
* <p>meta.ref.ebt1=/home/sueh/NOBACKUP/test datasets/Linux/Development4/UITests/dual-test-guiORIG/BBa.st</p>
* <p>meta.ref.ebt2=/home/sueh/NOBACKUP/test datasets/Linux/Development4/UITests/dual-montage/midzone2a.st</p>
* <p>meta.ref.ebt3=/home/sueh/NOBACKUP/test datasets/Linux/Development4/UITests/dual-test-gui-plugin/BBa_efos100.mrc</p>
* <p>meta.ref.ebt.lastID=ebt3</p>
*
* 
* <p>Copyright: Copyright 2014 - 2017 by the Regents of the University of Colorado</p>
* <p/>
* <p>Organization: Dept. of MCD Biology, University of Colorado</p>
*
* @version $Id$
* 
* <p> $Log$ </p>
*/
public final class TableReference {
  public static final String rcsid = "$Id:$";

  private static final String GROUP_KEY = "ref";
  private static final String BASE_ID_NUM = "0";
  private static final String LAST_ID_KEY = "lastID";

  // Map<fileKey (file-path minus extension), ID>
  private final Map<String, String> idMap = new HashMap<String, String>();
  // Map<ID, file-path>
  private final Map<String, String> filePathMap = new HashMap<String, String>();

  private final String idPrefix;

  private boolean loaded = false;
  private EtomoNumber lastIDNum = new EtomoNumber(EtomoNumber.Type.LONG);

  public TableReference(final String idPrefix) {
    this.idPrefix = idPrefix;
    lastIDNum.set(BASE_ID_NUM);
  }

  /**
   * @param filePath
   * @return an ID if the key derived from filePath has already been loaded into this reference, otherwise null
   */
  public String getID(final String filePath) {
    return idMap.get(makeFileKey(filePath));
  }

  private static String makeFileKey(final String filePath) {
    return Utilities.removeExtension(filePath);
  }

  public String getFilePath(final String id) {
    return filePathMap.get(id);
  }

  /**
   * Retrieve the entries associated with this id and replace the filePath and fileKey.
   * @param id
   * @param newFilePath
   */
  public void changeFilePath(final String id, final String newFilePath) {
    if (filePathMap.containsKey(id)) {
      filePathMap.remove(id);
      idMap.remove(makeFileKey(getFilePath(id)));
      filePathMap.put(id, newFilePath);
      idMap.put(makeFileKey(newFilePath), id);
    }
  }

  /**
   * Generates an ID and adds it as the value to idMap, with uniqueString as the key.
   * Add the uniqueString to unqueStringMap, with the ID as the key.
   * @param uniqueString - string to associate with generated ID
   * @return - the generated ID
   * @throws DuplicateException if uniqueString in this instance
   * @throws NotLoadedException if this instance has not been loaded from properties
   */
  public String put(final String filePath) throws DuplicateException, NotLoadedException {
    if (filePath == null) {
      throw new NullPointerException("file path cannot be null");
    }
    String fileKey = makeFileKey(filePath);
    if (idMap.containsKey(fileKey)) {
      throw new DuplicateException(
        "String is already associated with an ID:  " + filePath);
    }
    String id = nextID();
    idMap.put(fileKey, id);
    filePathMap.put(id, filePath);
    return id;
  }

  Iterator<String> idIterator() {
    return idMap.values().iterator();
  }

  /**
   * Generates an instance-level unique ID.
   * @return ID
   * @throws NotLoadedException if this instance has not been loaded from properties
   */
  private String nextID() throws NotLoadedException {
    if (!loaded) {
      throw new NotLoadedException("Reference uninitialized");
    }
    lastIDNum.add(1);
    return idPrefix + lastIDNum.toString();
  }

  private String createPrepend(String prepend) {
    if (prepend == null || prepend.matches("\\s*")) {
      return GROUP_KEY;
    }
    prepend = prepend.trim();
    if (prepend.endsWith(".")) {
      return prepend + GROUP_KEY;
    }
    return prepend + "." + GROUP_KEY;
  }

  /**
   * Call when the instance is new, and doesn't have to be loaded from properties.
   */
  public void setNew() {
    loaded = true;
  }

  /**
   * Load lastIdNum from lastId.  Places an error message in the _err.log if it fails.
   * @param lastId
   * @param repairDone - changes the error message
   * @param prepend - used in the error message
   * @return
   */
  private boolean loadLastIDNum(final String lastID, final boolean repairDone,
    final String prepend) {
    loaded = false;
    lastIDNum.reset();
    if (lastID != null) {
      if (!lastID.startsWith(idPrefix)) {
        // May be just the number part of the ID
        lastIDNum.set(lastID);
      }
      else {
        lastIDNum.set(lastID.substring(idPrefix.length()));
      }
    }
    if (lastIDNum.isNull() || !lastIDNum.isValid() || lastIDNum.lt(BASE_ID_NUM)) {
      if (!repairDone) {
        System.err.println("WARNING: property " + getLastIDKey(prepend)
          + " is invalid in the dataset file: " + lastIDNum
          + "\\nAttempting to repair...");
      }
      else {
        System.err.println("ERROR: property " + getLastIDKey(prepend)
          + " is invalid in the dataset file: " + lastIDNum + ".  Unable to load.");
      }
      return false;
    }
    return true;
  }

  /**
   * Assumes that prepend has already been set up.
   * @param prepend
   * @return
   */
  private String getLastIDKey(final String prepend) {
    return prepend + "." + idPrefix + "." + LAST_ID_KEY;
  }

  /**
   * Load from properties.
   * @param props
   * @param prepend
   */
  void load(final Properties props, String prepend) {
    // reset
    loaded = false;
    lastIDNum.reset();
    idMap.clear();
    filePathMap.clear();
    // load
    prepend = createPrepend(prepend);
    String lastID = props.getProperty(getLastIDKey(prepend));
    if (!loadLastIDNum(lastID, false, prepend)) {
      // Attempt to repair lastID
      Iterator iterator = props.entrySet().iterator();
      String genericKey = prepend + "." + idPrefix;
      lastID = null;
      // To repair lastId, find the highest ID in props
      while (iterator.hasNext()) {
        Entry entry = (Entry) iterator.next();
        String key = (String) entry.getKey();
        if (key != null) {
          if (key.startsWith(genericKey)) {
            String curID = key.substring(genericKey.length() - idPrefix.length());
            if (lastID == null || lastID.compareTo(curID) < 0) {
              lastID = curID;
            }
          }
        }
      }
      if (!loadLastIDNum(lastID, true, prepend)) {
        return;
      }
    }
    // Loading prepend properties. All properties with IDs greater then lastID will not be
    // loaded, and may be overwritten.;
    // loading prepend.ID = uniqueString
    for (long idNum = 1; idNum <= lastIDNum.getLong(); idNum++) {
      String id = idPrefix + idNum;
      String filePath = props.getProperty(prepend + "." + id);
      // Since put prevents a null uniqueString from being saved, assume that a null
      // uniqueString here mean that this ID was deleted - not an error.
      if (filePath != null) {
        if (filePath.indexOf("/dual/") != -1) {
          // Looking for mystery bug:
          // meta.ref.ebt4=/home/NOBACKUP/sueh/test
          // datasets/Development/linux/UITests/dual/dual/BBa.st
          System.err.println("loaded ref " + filePath);
        }
        String fileKey = makeFileKey(filePath);
        if (!idMap.containsKey(fileKey)) {
          idMap.put(fileKey, id);
          filePathMap.put(id, filePath);
        }
        else {
          System.err.println("ERROR: duplicate string, " + filePath + ", under " + prepend
            + "." + id + " in the dataset file.  This property will not be loaded.");
        }
      }
    }
    loaded = true;
  }

  /**
   * Store in properties.
   * @param props
   * @param prepend
   */
  void store(Properties props, String prepend) {
    if (!loaded) {
      return;
    }
    prepend = createPrepend(prepend);
    props.setProperty(getLastIDKey(prepend), idPrefix + lastIDNum);
    Iterator<Entry<String, String>> iterator = idMap.entrySet().iterator();
    // saving:
    // prepend.ID = filePath
    while (iterator.hasNext()) {
      Entry<String, String> entry = iterator.next();
      String fileKey = entry.getKey();
      if (fileKey.indexOf("/dual/") != -1) {
        // Looking for mystery bug:
        // meta.ref.ebt4=/home/NOBACKUP/sueh/test
        // datasets/Development/linux/UITests/dual/dual/BBa.st
        System.err.println("stored ref " + fileKey);
      }
      // Save filePath
      String id = entry.getValue();
      props.setProperty(prepend + "." + id, filePathMap.get(id));
    }
  }
}
