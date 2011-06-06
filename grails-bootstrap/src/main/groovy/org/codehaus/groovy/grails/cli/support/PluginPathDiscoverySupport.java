/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.cli.support;

import grails.util.BuildSettings;
import grails.util.GrailsNameUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for plugin discovery when running the Grails command line
 *
 * @since 1.4
 * @author Graeme Rocher
 *
 */
public class PluginPathDiscoverySupport {

    BuildSettings settings;

    public PluginPathDiscoverySupport(BuildSettings settings) {
        this.settings = settings;
    }

    /**
     * List all plugin directories that we know about: those in the
     * project's "plugins" directory, those in the global "plugins"
     * dir, and those declared explicitly in the build config.
     * @return A list of all known plugin directories, or an empty list if there are none.
     */
    public List<File> listKnownPluginDirs() {
        List<File> dirs = new ArrayList<File>();
        dirs.addAll(settings.getPluginDirectories());
        return dirs;
    }
    /**
     * Gets the name of a plugin based on its directory. The method
     * basically finds the plugin descriptor and uses the name of the
     * class to determine the plugin name. To be honest, this class
     * shouldn't be plugin-aware in my view, so hopefully this will
     * only be a temporary method.
     * @param pluginDir The directory containing the plugin.
     * @return The name of the plugin contained in the given directory.
     */
    public String getPluginName(File pluginDir) {
        // Get the plugin descriptor from the given directory and use
        // it to infer the name of the plugin.
        File desc = getPluginDescriptor(pluginDir);

        if (desc == null) {
            throw new RuntimeException("Cannot find plugin descriptor in plugin directory '" + pluginDir + "'.");
        }
        return GrailsNameUtils.getPluginName(desc.getName());
    }

    /**
     * Retrieves the first plugin descriptor it finds in the given
     * directory. The search is not recursive.
     * @param dir The directory to search in.
     * @return The location of the plugin descriptor, or <code>null</code>
     * if none can be found.
     */
    public File getPluginDescriptor(File dir) {
        if (!dir.exists()) return null;

        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.endsWith("GrailsPlugin.groovy");
            }
        });

        return files.length > 0 ? files[0] : null;
    }
}
