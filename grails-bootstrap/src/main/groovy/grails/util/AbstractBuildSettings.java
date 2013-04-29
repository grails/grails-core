/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.util;

import groovy.util.ConfigObject;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Methods optimized to Java for the BuildSettings class.
 *
 * @since 1.3.4
 */
public abstract class AbstractBuildSettings {

    private static final String KEY_PLUGIN_DIRECTORY_RESOURCES = "pluginDirectoryResources";
    private static final String KEY_INLINE_PLUGIN_LOCATIONS = "inlinePluginLocations";
    private static final String KEY_PLUGIN_BASE_DIRECTORIES = "pluginBaseDirectories";

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
    @SuppressWarnings("rawtypes")
    protected Map flatConfig = Collections.emptyMap();

    abstract File getBaseDir();

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
        getPluginDirectories();
        if (location != null) {
            Collection<File> directories = getPluginDirectories();
            if (!directories.contains(location)) {
                directories.add(location);
                if (isInline) {
                    getInlinePluginDirectories().add(location);
                }
            }
        }
    }

    /**
     * Obtains a list of plugin directories for the application
     */
    @SuppressWarnings("unchecked")
    public Collection<File> getPluginDirectories() {
        Collection<File> pluginDirectoryResources = (Collection<File>)cache.get(KEY_PLUGIN_DIRECTORY_RESOURCES);
        if (pluginDirectoryResources == null) {
            pluginDirectoryResources = getImplicitPluginDirectories();

            // Also add any explicit plugin locations specified by the
            // BuildConfig setting "grails.plugin.location.<name>"
            Collection<File> inlinePlugins = getInlinePluginsFromConfiguration(config);
            cache.put(KEY_INLINE_PLUGIN_LOCATIONS, inlinePlugins);
            ArrayList<File> list = new ArrayList<File>(pluginDirectoryResources);
            list.addAll(inlinePlugins);
            Collections.reverse(list);
            pluginDirectoryResources = new ConcurrentLinkedQueue<File>(list);

            cache.put(KEY_PLUGIN_DIRECTORY_RESOURCES, pluginDirectoryResources);
        }
        return pluginDirectoryResources;
    }

    /**
     * Extracts the inline plugin dirs relative to the base dir of this project.
     *
     * @see #getInlinePluginsFromConfiguration(Map, File)
     */
    @SuppressWarnings({ "rawtypes" })
    protected Collection<File> getInlinePluginsFromConfiguration(Map config) {
        return getInlinePluginsFromConfiguration(config, getBaseDir());
    }

    /**
     * Extracts the inline plugin dirs from the given config, relative to the given baseDir.
     *
     * TODO: consider trowing an error here if an plugin does not exists at the location.
     */
    @SuppressWarnings("rawtypes")
    protected Collection<File> getInlinePluginsFromConfiguration(Map config, File baseDir) {
        Collection<File> inlinePlugins = new ConcurrentLinkedQueue<File>();
        if (config != null) {
            Map pluginLocations = lookupPluginLocationConfig(config);
            if (pluginLocations != null) {
                for (Object value : pluginLocations.values()) {
                    if (value != null) {
                        File resource;
                        try {
                            // GRAILS-7045: Check whether the plugin location is absolute
                            // before resolving against the project's base dir.
                            resource = new File(value.toString());
                            if (!resource.isAbsolute()) {
                                resource = new File(baseDir, resource.getPath());
                            }

                            resource = resource.getCanonicalFile();
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

    @SuppressWarnings("rawtypes")
    private Map lookupPluginLocationConfig(Map config) {
        return getIfMap(getIfMap(getIfMap(config, "grails"), "plugin"), "location");
    }

    @SuppressWarnings("rawtypes")
    private Map getIfMap(Map config, String name) {
        if (config != null) {
            Object o = config.get(name);
            if (o instanceof Map) {
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
            File[] pluginDirs = new File(pluginBase).listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    final String fileName = pathname.getName();
                    return pathname.isDirectory() && (!fileName.startsWith(".") && fileName.indexOf('-') >- 1);
                }
            });
            if (pluginDirs != null) {
                dirList.addAll(Arrays.asList(pluginDirs));
            }
        }

        return dirList;
    }

    /**
     * Gets a list of all the known plugin base directories (directories where plugins are installed to).
     * @return Returns the base location where plugins are kept
     */
    @SuppressWarnings("unchecked")
    public Collection<String> getPluginBaseDirectories() {
        List<String> dirs = (List<String>) cache.get(KEY_PLUGIN_BASE_DIRECTORIES);
        if (dirs == null) {
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
    @SuppressWarnings("unchecked")
    public boolean isInlinePluginLocation(File pluginLocation) {
        if (pluginLocation == null) return false;
        getPluginDirectories(); // initialize the cache
        ConcurrentLinkedQueue<File> locations = (ConcurrentLinkedQueue<File>) cache.get(KEY_INLINE_PLUGIN_LOCATIONS);
        return locations != null && locations.contains(pluginLocation);
    }

    /**
     * Returns an array of the inplace plugin locations.
     */
    @SuppressWarnings("unchecked")
    public Collection<File> getInlinePluginDirectories() {
        getPluginDirectories(); // initailize the cache
        Collection<File> locations = (ConcurrentLinkedQueue<File>) cache.get(KEY_INLINE_PLUGIN_LOCATIONS);
        if (locations == null) {
            locations = new ConcurrentLinkedQueue<File>();
            cache.put(KEY_INLINE_PLUGIN_LOCATIONS, locations);
        }
        return locations;
    }
}
