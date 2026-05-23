package etomo.util;

import java.io.File;

import etomo.BaseManager;
import etomo.EtomoDirector;
import etomo.ui.BrowsingDirectory;

/**
 * <p>Description: Takes a file or a string.  Always contains either null or a directory
 * that is readable and exists.  Will use the parent directory if it is given a file.</p>
 * 
 * <p>Copyright: Copyright 2017 - 2018 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 * 
 * @see BrowsingDirectory
 * 
 */
public final class ValidDirectory {
  private static final boolean DEBUG = EtomoDirector.INSTANCE.getArguments().isDebug();
  private final BaseManager manager;

  private File dir = null;
  boolean allowGetParent = true;

  public ValidDirectory(final BaseManager manager) {
    this.manager = manager;
  }

  public String toString() {
    if (dir == null) {
      return "[dir:]";
    }
    return "[dir:" + dir.getAbsolutePath() + "]";
  }

  public File get() {
    return dir;
  }

  public File getParent() {
    if (!allowGetParent) {
      return get();
    }
    if (dir == null) {
      return null;
    }
    File parent = dir.getParentFile();
    if (parent == null) {
      return dir;
    }
    return parent;
  }

  public boolean isNull() {
    return dir == null;
  }

  public void setToPropertyUserDir() {
    allowGetParent = false;
    if (manager != null) {
      String propertyUserDir = manager.getPropertyUserDir();
      if (propertyUserDir != null) {
        File userDir = new File(propertyUserDir);
        if (userDir.isDirectory() && userDir.exists() && userDir.canRead()) {
          dir = userDir;
          return;
        }
      }
    }
    dir = new File(".");
  }

  /**
   * Returns true if dir or dir's parent is a valid readable directory.
   * @param dir
   * @return
   */
  public static boolean isValid(File dir) {
    if (dir == null) {
      return false;
    }
    if (!dir.isDirectory()) {
      dir = dir.getParentFile();
    }
    return dir.exists() && dir.isDirectory() && dir.canRead();
  }

  /**
   * Returns true if browsing dir or dir's parent is a valid readable directory.
   * @param dir
   * @return
   */
  public static boolean isValid(final BrowsingDirectory browsingDirectory) {
    if (browsingDirectory == null) {
      return false;
    }
    return isValid(browsingDirectory.getBrowsingDir());
  }

  /**
   * Returns dir or dir's parent if one is a valid readable directory.
   * @param dir
   * @return
   */
  public static File get(File dir) {
    if (dir == null) {
      return null;
    }
    if (!dir.isDirectory()) {
      dir = dir.getParentFile();
    }
    if (dir.exists() && dir.isDirectory() && dir.canRead()) {
      return dir;
    }
    return null;
  }

  /**
   * Returns browsing dir or dir's parent if one is a valid readable directory.
   * @param dir
   * @return
   */
  public static File get(final BrowsingDirectory browsingDirectory) {
    if (browsingDirectory == null) {
      return null;
    }
    return get(browsingDirectory.getBrowsingDir());
  }

  /**
   * Attempts to set a valid, readable directory.  If input is null, the instance resets.
   * The parent directory will be used if the input is a file.  If the input is not
   * null and no directory can be set, nothing is changed.
   *
   * @param input - any file or null
   */
  public void set(final File input) {
    allowGetParent = true;
    if (input == null) {
      dir = null;
      return;
    }
    File temp = input;
    if (!temp.isDirectory()) {
      temp = temp.getParentFile();
    }
    if (temp == null) {
      setToPropertyUserDir();
      return;
    }
    if (temp.isDirectory() && temp.exists() && temp.canRead()) {
      dir = temp;
    }
    else {
      if (!EtomoDirector.INSTANCE.isUnitTest() || DEBUG) {
        System.err.println("Warning:  unable to set browsing directory from: "
          + input.getAbsolutePath() + ".");
      }
      return;
    }
  }

  /**
   * Attempts to call set(File) with new File(input).  Null input causes a reset.  Empty
   * or blank input is treated as the current directory.
   * @param input
   */
  public void set(String input) {
    allowGetParent = true;
    if (input == null) {
      dir = null;
      return;
    }
    // A File class instance created from an empty or blank string does not have a parent.
    // Use the current directory.
    if (input.matches("\\s*")) {
      setToPropertyUserDir();
      return;
    }
    set(new File(input));
  }
}
