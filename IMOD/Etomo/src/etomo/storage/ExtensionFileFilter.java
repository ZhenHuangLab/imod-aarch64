package etomo.storage;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileFilter;

import etomo.BaseManager;
import etomo.type.AxisID;
import etomo.type.AxisType;
import etomo.type.Extension;
import etomo.type.FileType;
import etomo.type.ImageFilenameStyle;

/**
 * <p>Description: Abstract file filter that filters on suffixes, Extension instances, and
 * regex strings from FileType instances that use patterns.</p>
 * 
 * <p>Keep this case sensitive:  Mac and Windows allow only one file with the same name
 * and extension - case doesn't matter and you can't create another file with a different
 * case extension.  Linux is case sensitive and can have multiple files in this situation.
 * Files  with capitalized extension would have to be renamed to work with FileType.  But
 * this could cause a raw stack collision on Linux so case insenstivity is essential on
 * Linux.  Its less ideal on Mac/Windows because Java provides the file name that the file
 * was created with using whatever the extension was - caps or not.  But no one has ever
 * complained about Etomo's inability to find capitalized extensions on Mac/Windows.</p>
 * 
 * <p>Copyright: Copyright 2019 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public abstract class ExtensionFileFilter extends FileFilter
  implements java.io.FileFilter, FilenameFilter {
  private final boolean acceptDirectory;
  private final boolean useAllFileStyles;
  private final BaseManager manager;

  private SearchCollection searchCollection = null;
  private boolean debug = false;

  /**
   * Call setup at add search elements.
   * @param manager
   * @param acceptDirectory
   */
  public ExtensionFileFilter(final BaseManager manager, final boolean acceptDirectory,
    final boolean useAllFileStyles, final boolean debug) {
    this.acceptDirectory = acceptDirectory;
    this.useAllFileStyles = useAllFileStyles;
    this.manager = manager;
    this.debug = debug;
  }

  void setDebug(final boolean debug) {
    this.debug = debug;
    if (searchCollection != null) {
      searchCollection.setDebug(debug);
    }
  }

  /**
   * Call before running accept.  If the standard extension is included in the search, use
   * Extension and FileType to allow unwanted standardized files to be excluded even
   * though they have the same suffix as the standard extension.
   * @param datasetName
   * @param axisType
   * @param axisID
   * @param suffixes - will be compared with String.endsWith
   * @param extensions - getSuffix result will be compared with String.endsWith
   * @param fileTypes - getRegexp result will be compared against the file name with String.matches
   */
  public void setup(String datasetName, final AxisType axisType, final AxisID axisID,
    final String[] suffixes, final Extension[] extensions, final FileType[] fileTypes) {
    // reset
    if (searchCollection != null) {
      searchCollection.clear();
    }
    // Add search elements
    if ((suffixes != null && suffixes.length > 0)
      || (extensions != null && extensions.length > 0)
      || (fileTypes != null && fileTypes.length > 0)) {
      searchCollection = new SearchCollection(manager);
      searchCollection.add(suffixes);
      searchCollection.add(extensions, useAllFileStyles);
      searchCollection.add(fileTypes, datasetName, axisType, axisID, useAllFileStyles);
    }
  }

  void add(final String suffix) {
    if (suffix == null || suffix.isEmpty()) {
      return;
    }
    if (searchCollection == null) {
      searchCollection = new SearchCollection(manager);
    }
    searchCollection.add(suffix);
  }

  public boolean accept(final File dir, final String name) {
    if (name == null) {
      return false;
    }
    File file = new File(dir, name);
    if (acceptDirectory && file.isDirectory()) {
      return true;
    }
    return accept(file);
  }

  public boolean accept(final File file) {
    if (file == null) {
      return false;
    }
    if (acceptDirectory && file.isDirectory()) {
      return true;
    }
    if (searchCollection == null || searchCollection.isEmpty()) {
      return false;
    }
    return searchCollection.accept(file.getName());
  }

  public abstract String getDescription();

  private static final class SearchCollection {
    private final BaseManager manager;
    private final ImageFilenameStyle imageFilenameStyle;

    private Map<String, Pattern> fileTypePatterns = null;
    private Set<String> suffixes = null;
    private Set<Extension> extensions = null;
    private boolean debug = false;

    private SearchCollection(final BaseManager manager) {
      this.manager = manager;
      imageFilenameStyle = manager.getBaseMetaData().getImageFilenameStyle();
    }

    private boolean accept(final String fileName) {
      if (fileTypePatterns != null && !fileTypePatterns.isEmpty()) {
        Iterator<Map.Entry<String, Pattern>> iterator =
          fileTypePatterns.entrySet().iterator();
        while (iterator.hasNext()) {
          if (iterator.next().getValue().matcher(fileName).matches()) {
            return true;
          }
        }
      }
      if (suffixes != null && !suffixes.isEmpty()) {
        Iterator<String> iterator = suffixes.iterator();
        while (iterator.hasNext()) {
          if (fileName.endsWith(iterator.next())) {
            return true;
          }
        }
      }
      if (extensions != null) {
        if (extensions
          .contains(Extension.getInstance(manager, fileName, imageFilenameStyle))) {
          return true;
        }
      }
      return false;
    }

    private void add(final FileType[] fileTypes, final String datasetName,
      final AxisType axisType, final AxisID axisID, final boolean useAllFileStyles) {
      if (fileTypes == null || fileTypes.length == 0) {
        return;
      }
      int styleIndex = imageFilenameStyle.getIndex();
      // Either loop through all styles or just process this dataset's style.
      for (int iStyle = (useAllFileStyles ? 0 : styleIndex);
        iStyle < (useAllFileStyles ? ImageFilenameStyle.TOTAL : styleIndex + 1);
        iStyle++) {
        ImageFilenameStyle curImageFilenameStyle =
          ImageFilenameStyle.getInstanceFromIndex(iStyle);
        for (int i = 0; i < fileTypes.length; i++) {
          if (fileTypes[i] == null) {
            continue;
          }
          String regex = fileTypes[i].getRegex(datasetName, axisType, axisID,
            curImageFilenameStyle, manager.getBaseMetaData().getRawImageStackExtension());
          if (regex == null) {
            continue;
          }
          Pattern pattern = Pattern.compile(regex);
          if (pattern == null) {
            continue;
          }
          String key = pattern.toString();
          if (fileTypePatterns == null) {
            fileTypePatterns = new HashMap<String, Pattern>();
          }
          else if (fileTypePatterns.containsKey(key)) {
            continue;
          }
          fileTypePatterns.put(key, pattern);
        }
      }
    }

    private void add(final String[] inputSuffixes) {
      if (inputSuffixes == null || inputSuffixes.length == 0) {
        return;
      }
      for (int i = 0; i < inputSuffixes.length; i++) {
        if (inputSuffixes[i] == null || inputSuffixes[i].length() == 0) {
          continue;
        }
        if (suffixes == null) {
          suffixes = new HashSet<String>();
        }
        else if (suffixes.contains(inputSuffixes[i])) {
          continue;
        }
        suffixes.add(inputSuffixes[i]);
      }
    }

    private void add(final String suffix) {
      if (suffix == null || suffix.isEmpty()) {
        return;
      }
      if (suffixes == null) {
        suffixes = new HashSet<String>();
      }
      else if (suffixes.contains(suffix)) {
        return;
      }
      suffixes.add(suffix);
    }

    private void add(final Extension[] inputExtensions, final boolean useAllFileStyles) {
      if (inputExtensions == null || inputExtensions.length == 0) {
        return;
      }
      for (int i = 0; i < inputExtensions.length; i++) {
        if (inputExtensions[i] == null) {
          continue;
        }
        if (extensions == null) {
          extensions = new HashSet<Extension>();
        }
        else if (extensions.contains(inputExtensions[i])) {
          continue;
        }
        // The extension is new - add it.
        extensions.add(inputExtensions[i]);
      }
    }

    private void clear() {
      if (fileTypePatterns != null) {
        fileTypePatterns.clear();
      }
      if (suffixes != null) {
        suffixes.clear();
      }
      if (extensions != null) {
        extensions.clear();
      }
    }

    private boolean isEmpty() {
      if (fileTypePatterns != null && !fileTypePatterns.isEmpty()) {
        return false;
      }
      if (suffixes != null && !suffixes.isEmpty()) {
        return false;
      }
      if (extensions != null && !extensions.isEmpty()) {
        return false;
      }
      return true;
    }

    public String toString() {
      return "[" + imageFilenameStyle + "]";
    }

    private void setDebug(final boolean debug) {
      this.debug = debug;
    }
  }
}
