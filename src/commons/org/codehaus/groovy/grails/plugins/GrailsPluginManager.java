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

import java.math.BigDecimal;

import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.context.ApplicationContext;

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
public interface GrailsPluginManager {

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
	 * Retrieves a name Grails plugin instance
	 * 
	 * @param name The name of the plugin
	 * @return The GrailsPlugin instance or null if it doesn't exist	
	 */
	public abstract GrailsPlugin getGrailsPlugin(String name);

	/**
	 * Retrieves a plug-in for its name and version
	 * 
	 * @param name The name of the plugin
	 * @param version The version of the plugin
	 * @return The GrailsPlugin instance or null if it doesn't exist
	 */
	public abstract GrailsPlugin getGrailsPlugin(String name, BigDecimal version);

}