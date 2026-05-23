package etomo.type;

import java.util.Properties;

import junit.framework.TestCase;

public class TableReferenceTest extends TestCase {
  private static final String PREPEND = "meta";
  private static final String REF_PREPEND = "ref";
  private static final String ID_PREFIX = "trt";
  private static final String FILE_KEY1 = "/file/path/file1.part.of.file1";
  private static final String FILE_KEY2 = "/file/path/file2";
  private static final String FILE_KEY3 = "/file/path/file3";
  private static final String ALT_FILE_KEY3 = "/alt/file/alt/path/altfile3";
  private static final String FILE_KEY4 = "/file/path/file4";

  private final TableReference ref = new TableReference(ID_PREFIX);
  private final Properties props = new Properties();

  public TableReferenceTest(String name) {
    super(name);
    /*
     *<p>Properties example:</p>
     * <p>meta.ref.ebt1=/home/sueh/NOBACKUP/test datasets/Linux/Development4/UITests/dual-test-guiORIG/BBa.st</p>
     * <p>meta.ref.ebt2=/home/sueh/NOBACKUP/test datasets/Linux/Development4/UITests/dual-montage/midzone2a.st</p>
     * <p>meta.ref.ebt3=/home/sueh/NOBACKUP/test datasets/Linux/Development4/UITests/dual-test-gui-plugin/BBa_efos100.mrc</p>
     * <p>meta.ref.ebt.lastID=ebt3</p>
     */
    String key = PREPEND + "." + REF_PREPEND + "." + ID_PREFIX;
    int id = 1;
    props.setProperty(key + "1", FILE_KEY1 + ".mrc");
    props.setProperty(key + "2", FILE_KEY2 + ".tiff");
    props.setProperty(key + "3", FILE_KEY3 + ".st");
    props.setProperty(key + ".lastID", ID_PREFIX + "3");
  }

  public void testGetFilePath() {
    ref.load(props, PREPEND);
    assertEquals("can find the orginal file path by ID", FILE_KEY1 + ".mrc",
      ref.getFilePath(ID_PREFIX + "1"));
    assertEquals("can find the orginal file path by ID", FILE_KEY2 + ".tiff",
      ref.getFilePath(ID_PREFIX + "2"));
    assertEquals("can find the orginal file path by ID", FILE_KEY3 + ".st",
      ref.getFilePath(ID_PREFIX + "3"));
    assertEquals("returns null for an unknown ID", null,
      ref.getFilePath(ID_PREFIX + "4"));
  }

  public void testGetID() {
    ref.load(props, PREPEND);
    assertEquals("can find the ID using the orginal file path", ID_PREFIX + "1",
      ref.getID(FILE_KEY1 + ".mrc"));
    assertEquals(
      "the extension is not part of the key - all extensions return the same ID",
      ID_PREFIX + "1", ref.getID(FILE_KEY1 + ".tif"));
    assertEquals("returns null for an unknown ID", null, ref.getID("/unknown/file.ext"));
  }

  public void testPut() {
    try {
      ref.put(FILE_KEY4 + ".tif");
      fail("should have thrown NotLoadedException");
    }
    catch (NotLoadedException e) {
      // expecting this because load was not done
    }
    catch (DuplicateException e) {
      fail("should have thrown NotLoadedException");
    }
    ref.load(props, PREPEND);
    try {
      ref.put(null);
      fail("should have thrown NullPointerException");
    }
    catch (NotLoadedException e) {
      fail("should have thrown NullPointerException");
    }
    catch (DuplicateException e) {
      fail("should have thrown NullPointerException");
    }
    catch (NullPointerException e) {
      // expecting this - null file path not allowed
    }
    try {
      ref.put(FILE_KEY3 + ".hdf");
      fail("should have thrown DuplicateException for any extension");
    }
    catch (NotLoadedException e) {
      fail("should have thrown DuplicateException for any extension");
    }
    catch (DuplicateException e) {
      // expecting this because the file key is duplicated
    }
    try {
      ref.put(FILE_KEY4 + ".tif");
      assertEquals("can find the orginal file path by ID", FILE_KEY4 + ".tif",
        ref.getFilePath(ID_PREFIX + "4"));
      assertEquals("can find the ID using the orginal file path", ID_PREFIX + "4",
        ref.getID(FILE_KEY4 + ".tif"));
      assertEquals(
        "the extension is not part of the key - all extensions return the same ID",
        ID_PREFIX + "4", ref.getID(FILE_KEY4 + ".hdf"));
    }
    catch (NotLoadedException e) {
      fail("should not throw an exception");
    }
    catch (DuplicateException e) {
      fail("should not throw an exception");
    }
  }

  public void testChangeFilePath() {
    ref.load(props, PREPEND);
    ref.changeFilePath(ID_PREFIX + "4", FILE_KEY4 + ".tif");
    assertEquals("does not add an entry - only changes existing ones", null,
      ref.getFilePath(ID_PREFIX + "4"));
    ref.changeFilePath(ID_PREFIX + "3", ALT_FILE_KEY3 + ".st");
    assertEquals("changes an existing entry", ALT_FILE_KEY3 + ".st",
      ref.getFilePath(ID_PREFIX + "3"));
  }

  public void testSetNew() {
    ref.setNew();
    try {
      ref.put(FILE_KEY4 + ".tif");
      // load not required when new
    }
    catch (NotLoadedException e) {
      fail("load not required when new");
    }
    catch (DuplicateException e) {
      fail("load not required when new");
    }
  }
}
