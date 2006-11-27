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

import groovy.lang.GroovyClassLoader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

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
public class DefaultGrailsPluginManager implements GrailsPluginManager {

	private static final Log LOG = LogFactory.getLog(DefaultGrailsPluginManager.class);
	private Resource[] pluginResources = new Resource[0];
	private GrailsApplication application;
	private Map plugins = new HashMap();
	private Class[] pluginClasses = new Class[0];
	private List delayedLoadPlugins = new LinkedList();

	public DefaultGrailsPluginManager(String resourcePath, GrailsApplication application) throws IOException {
		super();
		if(application == null)
			throw new IllegalArgumentException("Argument [application] cannot be null!");
		
		this.pluginResources = new PathMatchingResourcePatternResolver().getResources(resourcePath);
		this.application = application;	
	}
	
	public DefaultGrailsPluginManager(Class[] plugins, GrailsApplication application) {
		this.pluginClasses = plugins;
		if(application == null)
			throw new IllegalArgumentException("Argument [application] cannot be null!");
		this.application = application;
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.plugins.GrailsPluginManager#loadPlugins()
	 */
	public void loadPlugins() 
					throws PluginException {
		GroovyClassLoader gcl = application.getClassLoader();
		
		for (int i = 0; i < pluginResources.length; i++) {
			Resource r = pluginResources[i];
			
			Class pluginClass = loadPluginClass(gcl, r);
			GrailsPlugin plugin = new DefaultGrailsPlugin(pluginClass, application);
			
			attemptPluginLoad(plugin);
		}
		for (int i = 0; i < pluginClasses.length; i++) {
			Class pluginClass = pluginClasses[i];
			GrailsPlugin plugin = new DefaultGrailsPlugin(pluginClass, application);
			attemptPluginLoad(plugin);			
		}
		
		if(!delayedLoadPlugins.isEmpty()) {
			loadDelayedPlugins();
		}
	}

	/**
	 * This method will attempt to load that plug-ins not loaded in the first pass
	 *
	 */
	private void loadDelayedPlugins() {
		while(!delayedLoadPlugins.isEmpty()) {
			GrailsPlugin plugin = (GrailsPlugin)delayedLoadPlugins.remove(0);
			if(areDependenciesResolved(plugin)) {
				LOG.info("Grails plug-in ["+plugin.getName()+"] with version ["+plugin.getVersion()+"] loaded successfully");
				plugins.put(plugin.getName(), plugin);
			}
			else {
				// ok, it still hasn't resolved the dependency after the initial
				// load of all plugins. All hope is not lost, however, so lets first
				// look inside the remaining delayed loads before giving up
				boolean foundInDelayed = false;
				for (Iterator i = delayedLoadPlugins.iterator(); i.hasNext();) {
					GrailsPlugin remainingPlugin = (GrailsPlugin) i.next();
					if(isDependantOn(plugin, remainingPlugin)) {
						foundInDelayed = true;
						break;
					}
				}
				if(foundInDelayed)
					delayedLoadPlugins.add(plugin);
				else 
					throw new PluginException("Plugin ["+plugin.getName()+"] cannot be loaded because its dependencies ["+ArrayUtils.toString(plugin.getDependencyNames())+"] cannot be resolved");
			}
		}
	}

	/**
	 * Checks whether the first plugin is dependant on the second plugin 
	 * @param plugin The plugin to check
	 * @param dependancy The plugin which the first argument may be dependant on
	 * @return True if it is 
	 */
	private boolean  isDependantOn(GrailsPlugin plugin, GrailsPlugin dependancy) {
		String[] dependencies = plugin.getDependencyNames();
		for (int i = 0; i < dependencies.length; i++) {
			String name = dependencies[i];
			BigDecimal version = plugin.getDependentVersion(name);
			
			if(name.equals(dependancy.getName()) && version.equals(dependancy.getVersion()))
				return true;
		}
		return false;
	}

	private boolean areDependenciesResolved(GrailsPlugin plugin) {
		String[] dependencies = plugin.getDependencyNames();
		if(dependencies.length > 0) {
			for (int i = 0; i < dependencies.length; i++) {
				String name = dependencies[i];
				BigDecimal version = plugin.getDependentVersion(name);
				if(hasGrailsPlugin(name, version)) {
					return true;	
				}
				else{
					return false;
				}
			}
		}
		return true;
	}

	private Class loadPluginClass(GroovyClassLoader gcl, Resource r) {
		Class pluginClass;
		try {
			pluginClass = gcl.parseClass(r.getInputStream());
		} catch (CompilationFailedException e) {
			throw new PluginException("Error compiling plugin ["+r.getFilename()+"] " + e.getMessage(), e);
		} catch (IOException e) {
			throw new PluginException("Error reading plugin ["+r.getFilename()+"] " + e.getMessage(), e);
		}
		return pluginClass;
	}

	/**
	 * This method attempts to load a plugin based on its dependencies. If a plugin's 
	 * dependencies cannot be resolved it will add it to the list of dependencies to 
	 * be resolved later
	 * 
	 * @param plugin The plugin
	 */
	private void attemptPluginLoad(GrailsPlugin plugin) {
		
		if(areDependenciesResolved(plugin)) {
			LOG.info("Grails plug-in ["+plugin.getName()+"] with version ["+plugin.getVersion()+"] loaded successfully");
			plugins.put(plugin.getName(), plugin);
		}
		else {
			delayedLoadPlugins.add(plugin);
		}	
	}
	
	private boolean hasGrailsPlugin(String name, BigDecimal version) {
		return getGrailsPlugin(name, version) != null;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.plugins.GrailsPluginManager#doRuntimeConfiguration(org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration)
	 */
	public void doRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		for (Iterator i = plugins.values().iterator(); i.hasNext();) {
			GrailsPlugin plugin = (GrailsPlugin) i.next();
			plugin.doWithRuntimeConfiguration(springConfig);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.plugins.GrailsPluginManager#doPostProcessing(org.springframework.context.ApplicationContext)
	 */
	public void doPostProcessing(ApplicationContext applicationContext) {
		for (Iterator i = plugins.values().iterator(); i.hasNext();) {
			GrailsPlugin plugin = (GrailsPlugin) i.next();
			plugin.doWithApplicationContext(applicationContext);
		}		
	}

	public Resource[] getPluginResources() {
		return this.pluginResources;
	}

	public GrailsPlugin getGrailsPlugin(String name) {
		return (GrailsPlugin)this.plugins.get(name);
	}

	public GrailsPlugin getGrailsPlugin(String name, BigDecimal version) {
		GrailsPlugin plugin = (GrailsPlugin)this.plugins.get(name);
		if(plugin != null) {
			if(plugin.getVersion().equals(version))
				return plugin;
		}
		return null;
	}
	
}
