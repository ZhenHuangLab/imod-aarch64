package etomo.storage;

import java.io.File;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

public final class DoseWeightingFileFilter extends FileFilter {
  /**
  * Whether the given file is accepted by this filter.
  */
  public boolean accept(final File file) {
    if (file == null || !file.isFile()) {
      return false;
    }
    String name = file.getName();
    return name != null && (name.endsWith(".txt") || name.endsWith(".mdoc"));
  }

  /**
   * The description of this filter. For example: "JPG and GIF Images"
   * @see FileView#getName
   */
  public String getDescription() {
    return "Dose information file (.mdoc,.txt)";
  }
}
