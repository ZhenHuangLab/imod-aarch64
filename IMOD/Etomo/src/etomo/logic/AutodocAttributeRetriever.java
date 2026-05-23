package etomo.logic;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import etomo.storage.DirectiveDef;
import etomo.storage.LogFile;
import etomo.storage.autodoc.AutodocFactory;
import etomo.storage.autodoc.ReadOnlyAttribute;
import etomo.storage.autodoc.ReadOnlyAutodoc;
import etomo.storage.autodoc.ReadOnlySection;
import etomo.type.EtomoAutodoc;

/**
* <p>Description: </p>
*
 * <p>Copyright: Copyright 2014 - 2015 by the Regents of the University of Colorado</p>
 * <p/>
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
*/
public final class AutodocAttributeRetriever {
  public static final AutodocAttributeRetriever INSTANCE =
    new AutodocAttributeRetriever();

  private ReadOnlyAutodoc progDefaultsAutodoc = null;
  // Avoid trying to open nonexistent autodocs multiple times.
  private Set<String> failedAutodocNames = null;

  private AutodocAttributeRetriever() {}

  /**
   * Gets the default value from the autodoc corresponding to the directiveDef, or from
   * progDefaults as a fallback.
   * @param manager
   * @param directiveDef - a directiveDef
   * @return
   */
  public String getDefaultValue(final DirectiveDef directiveDef) {
    if (directiveDef == null) {
      return null;
    }
    String autodocName = getAutodocName(directiveDef);
    if (autodocName == null) {
      return null;
    }
    String fieldName = getFieldName(directiveDef);
    if (fieldName == null) {
      return null;
    }
    String value = null;
    // If the autodoc has not been loaded, look for directiveDef.command in
    // progDefaults.adoc to see if the autodoc has defaults before loading it. If
    // progDefefault.adoc doesn't show any defaults for the command, return null.
    ReadOnlyAutodoc autodoc = getAutodoc(autodocName, true);
    if (autodoc != null) {
      ReadOnlySection fieldSection =
        autodoc.getSection(EtomoAutodoc.FIELD_SECTION_NAME, fieldName);
      if (fieldSection != null) {
        ReadOnlyAttribute attribute = fieldSection.getAttribute("default");
        if (attribute != null) {
          value = attribute.getValue();
          if (value != null) {
            return value;
          }
        }
      }
    }
    if (value == null) {
      // Fallback - if the default was not found in the autodoc, try to find it in
      // progDefaults.
      ReadOnlySection commandSection = getProgDefaultsCommandSection(autodocName);
      if (commandSection == null) {
        return null;
      }
      ReadOnlyAttribute attribute = commandSection.getAttribute(fieldName);
      if (attribute != null) {
        return attribute.getValue();
      }
    }
    return null;
  }

  /**
   * Gets the tooltip from the autodoc corresponding to the directiveDef.  The fallback
   * is the description from the directive def file.
   * @param manager
   * @param directiveDef - a directiveDef
   * @return
   */
  public String getTooltip(final DirectiveDef directiveDef) {
    if (directiveDef == null) {
      return null;
    }
    String tooltip = null;
    String autodocName = getAutodocName(directiveDef);
    if (autodocName != null) {
      String fieldName = getFieldName(directiveDef);
      if (fieldName != null) {
        ReadOnlyAutodoc autodoc = getAutodoc(autodocName, false);
        if (autodoc != null) {
          tooltip = EtomoAutodoc.getTooltip(autodoc, fieldName);
          if (tooltip != null) {
            return tooltip + " (" + directiveDef.toString() + ")";
          }
        }
      }
    }
    return directiveDef.getTooltip();
  }

  private String getAutodocName(final DirectiveDef directiveDef) {
    String autodocName = null;
    if (directiveDef.isComparam()) {
      autodocName = directiveDef.getCommand();
    }
    else if (directiveDef.isRuntime()) {
      // Warning: not all of the directive modules have a corresponding .adoc file.
      String command = directiveDef.getModule();
      if (command != null) {
        autodocName = command.toLowerCase();
      }
    }
    if (autodocName != null && failedAutodocNames != null
      && failedAutodocNames.contains(autodocName)) {
      return null;
    }
    return autodocName;
  }

  private String getFieldName(final DirectiveDef directiveDef) {
    String name = directiveDef.getName();
    if (!directiveDef.isRuntime() || name == null || name.isEmpty()) {
      return name;
    }
    // Runtime names start with a small letter. Capitalize it so it matches the
    // parameter name, which will be capitalized.
    String firstLetter = name.substring(0, 1).toUpperCase();
    if (name.length() > 1) {
      name = firstLetter + name.substring(1);
    }
    else {
      name = firstLetter;
    }
    return name;
  }

  private ReadOnlyAutodoc getAutodoc(final String autodocName,
    final boolean ifAutodocLoaded) {
    if (ifAutodocLoaded && !AutodocFactory.isLoaded(autodocName)) {
      return null;
    }
    try {
      ReadOnlyAutodoc autodoc = null;
      autodoc = AutodocFactory.getInstance(null, autodocName);
      if (autodoc == null || autodoc.toString() == null || autodoc.toString().equals("")) {
        if (failedAutodocNames == null) {
          failedAutodocNames = new HashSet<String>();
        }
        if (!failedAutodocNames.contains(autodocName)) {
          failedAutodocNames.add(autodocName);
        }
        return null;
      }
      else {
        return autodoc;
      }
    }
    catch (FileNotFoundException e) {
      if (failedAutodocNames == null) {
        failedAutodocNames = new HashSet<String>();
      }
      if (!failedAutodocNames.contains(autodocName)) {
        failedAutodocNames.add(autodocName);
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (LogFile.LockException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Returns a command section in the progDefaults autodoc.
   * @param command
   * @return
   */
  private ReadOnlySection getProgDefaultsCommandSection(final String command) {
    try {
      if (progDefaultsAutodoc == null) {
        synchronized (this) {
          if (progDefaultsAutodoc == null) {
            progDefaultsAutodoc =
              AutodocFactory.getComInstance(AutodocFactory.PROG_DEFAULTS);
          }
        }
      }
      if (progDefaultsAutodoc != null) {
        return progDefaultsAutodoc.getSection("Program", command);
      }
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
    return null;
  }
}
