package grails.util;

import groovy.util.ConfigObject;
import groovy.util.Eval;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Methods optimized to Java for the BuildSettings class
 *
 * @since 1.3.4
 */
public class AbstractBuildSettings {


    private static final String KEY_PLUGIN_DIRECTORY_RESOURCES = "pluginDirectoryResources";
    private static final String KEY_INLINE_PLUGIN_LOCATIONS = "inlinePluginLocations";
    private static final String KEY_PLUGIN_BASE_DIRECTORIES = "pluginBaseDirectories";
    private static final String CONFIG_GRAILS_PLUGIN_LOCATION = "grails.plugin.location";

    /**
     * Used to cache results of certain expensive operations
     */
    protected Map<String, Object> cache = new ConcurrentHashMap<String, Object>();
    /** The settings stored in the project's BuildConfig.groovy file if there is one. */
    protected ConfigObject config = new ConfigObject();
    /** The location where project-specific plugins are installed to. */
    protected File projectPluginsDir;

    /** The location where global plugins are installed to. */
    protected File globalPluginsDir;


    protected boolean projectPluginsDirSet;
    protected boolean globalPluginsDirSet;

    /**
     * Flattened version of the ConfigObject for easy access from Java
     */
    protected Map flatConfig = Collections.emptyMap();


    /**
     * Clears any locally cached values
     */
    void clearCache() {
        cache.clear();
    }

    public ConfigObject getConfig() {
        return config;
    }

    public void setConfig(ConfigObject config) {
        this.config = config;
    }

    public File getProjectPluginsDir() {
        return projectPluginsDir;
    }

    public void setProjectPluginsDir(File projectPluginsDir) {
        this.projectPluginsDir = projectPluginsDir;
        projectPluginsDirSet = true;
    }

    public File getGlobalPluginsDir() {
        return globalPluginsDir;
    }

    public void setGlobalPluginsDir(File globalPluginsDir) {
        this.globalPluginsDir = globalPluginsDir;
        globalPluginsDirSet = true;
    }

    /**
     * Adds a plugin directory
     * @param location The plugin's locatino
     */
    public void addPluginDirectory(File location, boolean isInline) {
        if(location != null) {
            Collection<File> directories = getPluginDirectories();
            directories.add(location);
            if(isInline) {
                getInlinePluginDirectories().add(location);
            }
        }
    }
    /**
      * Obtains a list of plugin directories for the application
      */
    public Collection<File> getPluginDirectories() {
         Collection<File> pluginDirectoryResources = (Collection<File>)cache.get(KEY_PLUGIN_DIRECTORY_RESOURCES);
         if (pluginDirectoryResources == null) {
             pluginDirectoryResources = getImplicitPluginDirectories();

             // Also add any explicit plugin locations specified by the
             // BuildConfig setting "grails.plugin.location.<name>"
             Collection<File> inlinePlugins = getInlinePluginsFromConfiguration(config);
             cache.put(KEY_INLINE_PLUGIN_LOCATIONS, inlinePlugins);
             pluginDirectoryResources.addAll(inlinePlugins);

             cache.put(KEY_PLUGIN_DIRECTORY_RESOURCES, pluginDirectoryResources);
         }
         return pluginDirectoryResources;
     }

    protected Collection<File> getInlinePluginsFromConfiguration(Map config) {
        Collection<File> inlinePlugins = new ConcurrentLinkedQueue<File>();
        if(config != null) {
            Map pluginLocations = lookupPluginLocationConfig(config);
            if (pluginLocations != null) {
                for (Object value : pluginLocations.values()) {
                    if(value != null) {
                        File resource;
                        try {
                            resource = new File(value.toString()).getCanonicalFile();
                            inlinePlugins.add(resource);
                        }
                        catch (IOException e) {
                            System.err.println("Cannot add location ["+value+"] as an inline plugin dependencies due to I/O error: " + e.getMessage());
                        }
                    }
                }
            }
        }
        return inlinePlugins;
    }

    private Map lookupPluginLocationConfig(Map config) {
        return getIfMap(getIfMap(getIfMap(config, "grails"), "plugin"), "location");
    }

    private Map getIfMap(Map config, String name) {
        if(config != null) {
            Object o = config.get(name);
            if(o instanceof Map) {
                return ((Map) o);
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Returns a list of all plugin directories in both the given path
     * and the global "plugins" directory together.
     *
     * @return A list of plugin directories as File objects
     */
    public Collection<File> getImplicitPluginDirectories() {
        ConcurrentLinkedQueue<File> dirList = new ConcurrentLinkedQueue<File>();

        for (String pluginBase : getPluginBaseDirectories()) {
            File[] pluginDirs = new File(pluginBase).listFiles(new FileFilter(){
                public boolean accept(File pathname) {
                    final String fileName = pathname.getName();
                    return pathname.isDirectory() && (!fileName.startsWith(".") && fileName.indexOf('-') >- 1);
                }
            });
            if(pluginDirs != null) {
                dirList.addAll(Arrays.asList(pluginDirs));
            }
        }

        return dirList;
    }

    /**
     * Gets a list of all the known plugin base directories (directories where plugins are installed to).
     * @return Returns the base location where plugins are kept
     */
    public Collection<String> getPluginBaseDirectories() {
        List<String> dirs = (List<String>) cache.get(KEY_PLUGIN_BASE_DIRECTORIES);
        if(dirs == null) {
            dirs = new ArrayList<String>();
            if (projectPluginsDir != null) try {
                dirs.add(projectPluginsDir.getCanonicalPath());
            }
            catch (IOException e) {
                System.err.println("Cannot read project plugins directory ["+projectPluginsDir+"] due to I/O error: " + e.getMessage());
            }

            if (globalPluginsDir != null) try {
                dirs.add(globalPluginsDir.getCanonicalPath());
            }
            catch (IOException e) {
                System.err.println("Cannot read global plugins directory ["+globalPluginsDir+"] due to I/O error: " + e.getMessage());
            }
            cache.put(KEY_PLUGIN_BASE_DIRECTORIES, dirs);
        }
        return dirs;
    }

    /**
     * Returns true if the specified plugin location is an inline location.
     */
    public boolean isInlinePluginLocation(File pluginLocation) {
        if(pluginLocation == null) return false;
        getPluginDirectories(); // initialize the cache
        ConcurrentLinkedQueue<File> locations = (ConcurrentLinkedQueue<File>) cache.get(KEY_INLINE_PLUGIN_LOCATIONS);
        return locations != null && locations.contains(pluginLocation);
    }

    /**
     * Returns an array of the inplace plugin locations.
     */
    public Collection<File> getInlinePluginDirectories() {
        getPluginDirectories(); // initailize the cache
        Collection<File> locations = (ConcurrentLinkedQueue<File>) cache.get(KEY_INLINE_PLUGIN_LOCATIONS);
        if(locations == null){
            locations = new ConcurrentLinkedQueue<File>();
            cache.put(KEY_INLINE_PLUGIN_LOCATIONS, locations);
        }
        return locations;
    }
}
