package etomo.plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import etomo.EtomoDirector;
import etomo.plugin.demo.DemoPluginManager;
import etomo.type.UserConfiguration;
import etomo.ui.UIComponent;
import etomo.ui.swing.Popup;
import etomo.ui.swing.UIHarness;

/**
 * <p>Description:  A factory that uses the Service Loader to return plugins that
 * correspond to specific interfaces found in the etomo.plugin packages.  See
 * etomo.plugin.Plugin for more information.</p>
 * 
 * <p>Plugin interfaces are created and expanded on demand.  More classes and functions
 * can be exposed, and more plugin interfaces can be created and integrated with eTomo.
 * If the functionality you need doesn't exist, or isn't public or inheritable, please
 * contact the developers.
 * 
 * <p>Changes:  The plugin directory has been moved, and its name changed.  It is now
 * $IMOD_DIR/Plugins.  The main plugin interface is now etomo.plugin.Plugin.  Syntax that
 * should work on Windows has been added to the instructions below.</p>
 * 
 * <p>An example of creating a plugin jar file.</p>
 * 
 * <pre>
 * The plugin directory is located in IMOD directory:
 * $IMOD_DIR/Plugins.  The plugin should be in a .jar file.
 * 
 * Creating a plugin from test plugin classes:
 * Please note that the file paths are Linux-style.
 *
cd TestPlugin
ls -R
.:
bin  src

./bin:
plugin

./bin/plugin:
TestPluginManager.class  ui

./bin/plugin/ui:
TestPluginPanel.class

./src:
plugin

./src/plugin:
TestPluginManager.java  ui

./src/plugin/ui:
TestPluginPanel.java

 * This is how you would make TestPlugin available for use in eTomo as a plugin:
 * 
 * 1. Use Java 1.6 for compiling.  The javac compiler takes two options that will force
 * another version to compile for Java 1.6:
-source 1.6 -target 1.6

 * 2. In TestPluginManager.java, inherit a plugin interface.
 * 3. Compile the TestPlugin files.  This should place .class files in the bin
 *    directory.
 * 4. Go to the bin directory.
 * 5. Create a directory called META-INF
 * 6. In META-INF create a directory called services
 * 7. In META-INF/services create a file called etomo.plugin.TomoGenMethodPlugin (or the
 *    full name of the plugin interface you are implementing).
 * 8. Add a reference to the class implementing the plugin interface to
 *    etomo.plugin.TomoGenMethodPlugin (and end the file with an empty line):
plugin.TestPluginManager


 * 9. In the bin directory, create a jar containing a services directory and a generated
 *    manifest:
jar cf TestPluginManager.jar META-INF/services plugin

 * 10. This will create a file called TestPluginManager.jar.  You can look at the contents of
 *     The jar file by running:
jar -tf TestPluginManager.jar

 * 11. A default manifest file was created by the jar program.  It has to be modifed.
 * 12. Extract the manifest file by running:
jar -xf TestPluginManager.jar META-INF/MANIFEST.MF

 * 13. Edit META-INF/MANIFEST.MF and add a Class-Path entry, set to the relative location
 *     of the etomo.jar file (the Created-By version does not matter - only the version
 *     the .class files where compiled with):
Manifest-Version: 1.0
Class-Path: ../bin/etomo.jar
Created-By: 1.7.0_79 (Oracle Corporation)


 * 14. In the bin directory, recreate the jar file with the corrected manifest by running:
jar cmf META-INF/MANIFEST.MF TestPluginManager.jar META-INF/services plugin

 * 15. Place TestPluginManager.jar in $IMOD_DIR/Plugins.
 * </pre>
 * 
 * <p>Copyright: Copyright 2016 by the Regents of the University of Colorado</p>
 * 
 * <p>Organization: Dept. of MCD Biology, University of Colorado</p>
 *
 * @version $Id$
 */
public final class PluginFactory {
  /**
   * Returns a plugin that has been incorporated into the etomo jar file.  Plugin is self-
   * contained in its own package.
   * @param uiComponent
   * @return
   */
  public static TomoGenMethodPlugin loadDemoPlugin(final UIComponent uiComponent) {
    Plugin plugin = new DemoPluginManager();
    printInfo(plugin, "TomoGenMethodPlugin");
    return (TomoGenMethodPlugin) plugin;
  }

  /**
   * Gets one plugin corresponding to the TomoGenMethodPlugin interface.
   * @param uiComponent
   * @return
   */
  public static TomoGenMethodPlugin
    loadTomoGenMethodPlugin(final UIComponent uiComponent) {
    Iterator loadedPlugins = ServiceLoader.load(TomoGenMethodPlugin.class).iterator();
    Plugin plugin = pickPlugin(loadedPlugins, uiComponent);
    printInfo(plugin, "TomoGenMethodPlugin");
    return (TomoGenMethodPlugin) plugin;
  }

  /**
   * Gets one plugin corresponding to the Plugin interface.
   * @param uiComponent
   * @return
   */
  public static Plugin loadBasicPlugin(final UIComponent uiComponent) {
    Iterator<Plugin> loadedPlugins = ServiceLoader.load(Plugin.class).iterator();
    Plugin plugin = pickPlugin(loadedPlugins, uiComponent);
    printInfo(plugin, "Plugin");
    return (TomoGenMethodPlugin) plugin;
  }

  private static void printInfo(final Plugin plugin, final String interfaceName) {
    if (plugin != null) {
      System.err.println("Successfully loaded plugin that implements the " + interfaceName
        + " interface.\nPlugin information:" + plugin.getTitle() + ","
        + plugin.getVersion() + "," + plugin.getDescription());
    }
  }

  private static Plugin pickPlugin(final Iterator loadedPlugins,
    final UIComponent uiComponent) {
    List<Plugin> list = null;
    try {
      UserConfiguration configuration = EtomoDirector.INSTANCE.getUserConfiguration();
      while (loadedPlugins.hasNext()) {
        Plugin plugin = (Plugin) loadedPlugins.next();
        // Only use plugins that work with this niche.
        if (plugin != null) {
          String key = plugin.getKey();
          // Check for plugins that have already been allowed or excluded.
          if (configuration.hasPlugin(key)) {
            // Does the user want this plugin?
            if (configuration.isPlugin(key)) {
              // Plugin has been verified.
              return plugin;
            }
            if (list == null) {
              list = new ArrayList<Plugin>();
            }
            list.add(plugin);
          }
          else {
            // No information on the this plugin so ask the user.
            Popup popup = Popup.getYesNoInstance(uiComponent, "Allow plugin?",
              "An external plugin for eTomo was found.\n" + plugin.getTitle()
                + " version: " + plugin.getVersion() + "\n" + plugin.getDescription()
                + "\n\nUse this plugin?",
              "Do not ask about this plugin again.");
            UIHarness.INSTANCE.openPopup(popup);
            boolean allowed = false;
            if (popup.isYes()) {
              allowed = true;
            }
            if (popup.isCheckboxSelected()) {
              configuration.setPlugin(key, allowed);
            }
            if (allowed) {
              return plugin;
            }
          }
        }
      }
    }
    catch (ServiceConfigurationError e) {
      System.err
        .println("ERROR:  Plugin service loader failure.  Unable to retrieve plugins.");
      Thread.dumpStack();
    }
    return null;
  }
}