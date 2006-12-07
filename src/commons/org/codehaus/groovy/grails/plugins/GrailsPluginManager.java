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

import java.io.File;
import java.io.Writer;
import java.math.BigDecimal;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

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
public interface GrailsPluginManager extends ApplicationContextAware {

	String BEAN_NAME = "grailsPluginManager";

	/**
	 * Performs the initial load of plug-ins throwing an exception if any dependencies
	 * don't resolve 
	 * 
	 * @throws PluginException 
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
	 * @see doWebDescriptor(Resource, Writer target)
	 * 
	 * @param descriptor The File of the descriptor
	 * @param target The target to write the changes to
	 */
	public abstract void doWebDescriptor(File descriptor, Writer target);
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
	 * Retrieves a plug-in for its name and version
	 * 
	 * @param name The name of the plugin
	 * @param version The version of the plugin
	 * @return The GrailsPlugin instance or null if it doesn't exist
	 */
	public abstract GrailsPlugin getGrailsPlugin(String name, BigDecimal version);

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

}