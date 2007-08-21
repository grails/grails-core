/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.plugins;

/**
 * <p>An interface that enables the evaluation of Plug-in Metadata supplied by the plug-ins plugin.xml  file
 *
 * @author Graeme Rocher
 * @since 0.6
 *
 *        <p/>
 *        Created: Aug 21, 2007
 *        Time: 8:01:22 AM
 */
public interface PluginMetaManager extends PluginManagerAware {
    /**
     * Id of the bean in the app ctx
     */
    String BEAN_ID = "pluginMetaManager";

    /**
     * Retrieves all the plugin resource names for the given plugin name
     * @param pluginName The plugin name
     * @return An array of plugin resource names
     */
    String[] getPluginResources(String pluginName);

    /**
     * Obtains a plug-in instance from the pluginManager for the given resource name
     *
     * @param name The name of the resource
     * @return A GrailsPlugin instance or null
     */
    GrailsPlugin getPluginForResource(String name);

    /**
     * Obtains the path to the plug-in for the given resource
     * @param resourceName The name of the resource
     * @return The path to the plug-in or null if it doesn't exist
     */
    String getPluginPathForResource(String resourceName);

    /**
     * Obtains the path to the plug-in views directory for the given resource name
     * @param resourceName The resource name
     * @return The path to the plug-in views directory or null if the plug-in doesn't exist
     */
    String getPluginViewsPathForResource(String resourceName);
}
