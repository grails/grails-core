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
package org.codehaus.groovy.grails.plugins;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.web.context.ServletContextAware;

import java.io.File;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * <p>A class that handles the loading and management of plug-ins in the Grails system.
 * A plugin a just like a normal Grails application except that it contains a file ending
 * in *Plugin.groovy  in the root of the directory.
 * 
 * <p>A Plugin class is a Groovy class that has a version and optionally closures
 * called doWithSpring, doWithContext and doWithWebDescriptor
 * 
 * <p>The doWithSpring closure uses the BeanBuilder syntax (@see grails.spring.BeanBuilder) to
 * provide runtime configuration of Grails via Spring
 * 
 * <p>The doWithContext closure is called after the Spring ApplicationContext is built and accepts 
 * a single argument (the ApplicationContext)
 * 
 * <p>The doWithWebDescriptor uses mark-up building to provide additional functionality to the web.xml 
 * file
 * 
 *<p> Example:
 * <pre>
 * class ClassEditorGrailsPlugin {
 *      def version = 1.1
 *      def doWithSpring = { application ->
 *      	classEditor(org.springframework.beans.propertyeditors.ClassEditor, application.classLoader)
 *      }
 * }
 * </pre>
 * 
 * <p>A plugin can also define "dependsOn" and "evict" properties that specify what plugins the plugin
 * depends on and which ones it is incompatable with and should evict 
 * 
 * @author Graeme Rocher
 * @since 0.4
 *
 */
public interface GrailsPluginManager extends ApplicationContextAware, ServletContextAware {

	String BEAN_NAME = "pluginManager";

    /**
     * Returns an array of all the loaded plug-ins
     * @return An array of plug-ins
     */
    GrailsPlugin[] getAllPlugins();

    /**
	 * Performs the initial load of plug-ins throwing an exception if any dependencies
	 * don't resolve 
	 * 
	 * @throws PluginException Thrown when an error occurs loading the plugins
	 */
	public abstract void loadPlugins() throws PluginException;

	/**
	 * Executes the runtime configuration phase of plug-ins
	 * 
	 * @param springConfig The RuntimeSpringConfiguration instance
	 */
	public abstract void doRuntimeConfiguration(
			RuntimeSpringConfiguration springConfig);

	/**
	 * Performs post initialization configuration for each plug-in, passing
	 * the built application context
	 * 
	 * @param applicationContext The ApplicationContext instance
	 */
	public abstract void doPostProcessing(ApplicationContext applicationContext);

	/**
	 * Takes the specified web descriptor reference and configures it with all the plugins outputting
	 * the result to the target Writer instance
	 * 
	 * @param descriptor The Resource of the descriptor
	 * @param target The Writer to write the result to
	 */
	public abstract void doWebDescriptor(Resource descriptor, Writer target);
	
	/**
	 * @see #doWebDescriptor(Resource, Writer)
	 * 
	 * @param descriptor The File of the descriptor
	 * @param target The target to write the changes to
	 */
	public abstract void doWebDescriptor(File descriptor, Writer target);
	
	/**
	 * This is called on all plugins so that they can add new methods/properties/constructors etc.
	 *
	 */
	public abstract void doDynamicMethods();
	/**
	 * Retrieves a name Grails plugin instance
	 * 
	 * @param name The name of the plugin
	 * @return The GrailsPlugin instance or null if it doesn't exist	
	 */
	public abstract GrailsPlugin getGrailsPlugin(String name);
	
	/**
	 * 
	 * @param name The name of the plugin
	 * @return True if the the manager has a loaded plugin with the given name
	 */
	public boolean hasGrailsPlugin(String name);

    /**
     * Retrieves a plug-in that failed to load, or null if it doesn't exist
     *
     * @param name The name of the plugin
     * @return A GrailsPlugin or null
     */
    public GrailsPlugin getFailedPlugin(String name);

    /**
	 * Retrieves a plug-in for its name and version
	 * 
	 * @param name The name of the plugin
	 * @param version The version of the plugin
	 * @return The GrailsPlugin instance or null if it doesn't exist
	 */
	public abstract GrailsPlugin getGrailsPlugin(String name, Object version);

	/**
	 * Executes the runtime configuration for a specific plugin AND all its dependencies
	 * 
	 * @param pluginName The name of he plugin
	 * @param springConfig The runtime spring config instance
	 */
	public abstract void doRuntimeConfiguration(String pluginName, RuntimeSpringConfiguration springConfig);

	/**
	 * Checks all the plugins to see whether they have any changes
	 *
	 */
	public abstract void checkForChanges();

	/**
	 * Sets the GrailsApplication used be this plugin manager
	 * @param application The GrailsApplication instance
	 */
	public abstract void setApplication(GrailsApplication application);

	/**
	 * @return the initialised
	 */
	public boolean isInitialised();

    /**
     * Refreshes the specified plugin. A refresh will force to plugin to "touch" each of its watched resources
     * and fire modified events for each
     *
     * @param name The name of the plugin to refresh
     */
    public void refreshPlugin(String name);

    /**
     * Retrieves a collection of plugins that are observing the specified plugin
     *
     * @param plugin The plugin to retrieve observers for
     * @return A collection of observers
     */
    public Collection getPluginObservers(GrailsPlugin plugin);

    /**
     * inform the specified plugins observers of the event specified by the passsed Map instance
     *
     * @param pluginName The name of the plugin
     * @param event The event
     */
    public void informObservers(String pluginName, Map event);

    /**
     * Called prior to the initialisation of the GrailsApplication object to allow registration of additional ArtefactHandler objects
     *
     * @see org.codehaus.groovy.grails.commons.ArtefactHandler
     * 
     */
    public void doArtefactConfiguration();

    /**
     * Registers pre-compiled artefacts with the GrailsApplication instance, only overriding if the application doesn't already provide an artefact of the same
     * name
     *
     * @param application The GrailsApplication object
     */
    void registerProvidedArtefacts(GrailsApplication application);

    /**
     * Shuts down the PluginManager
     */
    void shutdown();
}