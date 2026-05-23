package etomo.type;

import java.util.Properties;

/**
* <p>Description: </p>
* 
* <p>Copyright: Copyright 2014</p>
*
* <p>Organization:
* Boulder Laboratory for 3-Dimensional Electron Microscopy of Cells (BL3DEMC),
* University of Colorado</p>
* 
* @author $Author$
* 
* @version $Revision$
* 
* <p> $Log$ </p>
*/
final class PanelHeaderSettings implements ConstPanelHeaderSettings {
  public static final String rcsid = "$Id:$";

  private static final String OPEN_KEY = "open";
  private static final String ADVANCED_KEY = "advanced";
  private static final String MORE_KEY = "more";

  private final String name;

  EtomoBoolean2 open = null;
  EtomoBoolean2 advanced = null;
  EtomoBoolean2 more = null;

  PanelHeaderSettings(final String name) {
    this.name = name;
  }

  private static String createPrepend(String prepend, final String name) {
    if (prepend == null || prepend.matches("\\s*")) {
      return name;
    }
    if (prepend.endsWith(".")) {
      return prepend + name;
    }
    return prepend + "." + name;
  }

  /**
   * Attempt to get the properties specified by prepend and name.  If it doesn't
   * exist, return null.  If it does, set them in instance (create instance
   * if it doesn't exist).  Return the instance.
   * @param PanelHeaderSettings
   * @param name
   * @param props
   * @param prepend
   * @return PanelHeaderSettings
   */
  public static PanelHeaderSettings load(PanelHeaderSettings instance, final String name,
      final Properties props, String prepend) {
    prepend = createPrepend(prepend, name);
    if (instance != null) {
      instance.open = EtomoBoolean2.load(instance.open, OPEN_KEY, props, prepend);
      instance.advanced = EtomoBoolean2.load(instance.advanced, ADVANCED_KEY, props,
          prepend);
      instance.more = EtomoBoolean2.load(instance.more, MORE_KEY, props, prepend);
      if (instance.open == null && instance.advanced == null && instance.more == null) {
        return null;
      }
    }
    else {
      EtomoBoolean2 open = EtomoBoolean2.load(null, OPEN_KEY, props, prepend);
      EtomoBoolean2 advanced = EtomoBoolean2.load(null, ADVANCED_KEY, props, prepend);
      EtomoBoolean2 more = EtomoBoolean2.load(null, MORE_KEY, props, prepend);
      if (open == null && advanced == null && more == null) {
        return null;
      }
      instance = new PanelHeaderSettings(name);
      instance.open = open;
      instance.advanced = advanced;
      instance.more = more;
    }
    return instance;
  }

  public void reset() {
    if (open != null) {
      open.reset();
    }
    if (advanced != null) {
      advanced.reset();
    }
    if (more != null) {
      more.reset();
    }
  }

  public void set(final ConstPanelHeaderSettings input) {
    if (!input.isOpenNull()) {
      if (open == null) {
        open = new EtomoBoolean2(OPEN_KEY);
      }
      open.set(input.isOpen());
    }
    if (!input.isAdvancedNull()) {
      if (advanced == null) {
        advanced = new EtomoBoolean2(ADVANCED_KEY);
      }
      advanced.set(input.isAdvanced());
    }
    if (!input.isMoreNull()) {
      if (more == null) {
        more = new EtomoBoolean2(MORE_KEY);
      }
      more.set(input.isMore());
    }
  }

  public void load(final Properties props, String prepend) {
    prepend = createPrepend(prepend, name);
    open = EtomoBoolean2.load(open, OPEN_KEY, props, prepend);
    advanced = EtomoBoolean2.load(advanced, ADVANCED_KEY, props, prepend);
    more = EtomoBoolean2.load(more, MORE_KEY, props, prepend);
  }

  public void store(final Properties props, String prepend) {
    prepend = createPrepend(prepend, name);
    if (open != null && !open.isNull()) {
      open.store(props, prepend);
    }
    else {
      EtomoBoolean2.remove(OPEN_KEY, props, prepend);
    }
    if (advanced != null && !advanced.isNull()) {
      advanced.store(props, prepend);
    }
    else {
      EtomoBoolean2.remove(ADVANCED_KEY, props, prepend);
    }
    if (more != null && !more.isNull()) {
      more.store(props, prepend);
    }
    else {
      EtomoBoolean2.remove(MORE_KEY, props, prepend);
    }
  }

  public void remove(final Properties props, String prepend) {
    prepend = createPrepend(prepend, name);
    if (open != null) {
      open.remove(props, prepend);
    }
    else {
      EtomoBoolean2.remove(OPEN_KEY, props, prepend);
    }
    if (advanced != null) {
      advanced.store(props, prepend);
    }
    else {
      EtomoBoolean2.remove(ADVANCED_KEY, props, prepend);
    }
    if (more != null) {
      more.store(props, prepend);
    }
    else {
      EtomoBoolean2.remove(MORE_KEY, props, prepend);
    }
  }

  public boolean isOpenNull() {
    return open == null || open.isNull();
  }

  public boolean isOpen() {
    return open != null && open.is();
  }

  public boolean isAdvancedNull() {
    return advanced == null || advanced.isNull();
  }

  public boolean isAdvanced() {
    return advanced != null && advanced.is();
  }

  public boolean isMoreNull() {
    return more == null || more.isNull();
  }

  public boolean isMore() {
    return more != null && more.is();
  }
}
