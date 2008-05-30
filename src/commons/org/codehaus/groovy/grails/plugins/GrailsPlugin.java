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

import groovy.lang.GroovyObject;
import groovy.util.slurpersupport.GPathResult;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * <p>Plugin interface that adds Spring {@link org.springframework.beans.factory.config.BeanDefinition}s
 * to a registry based on a {@link GrailsApplication} object. After all <code>GrailsPlugin</code> classes
 * have been processed the {@link org.springframework.beans.factory.config.BeanDefinition}s in the registry are
 * loaded in a Spring {@link org.springframework.context.ApplicationContext} that's the singular
 * configuration unit of Grails applications.</p>
 *
 * <p>It's up to implementation classes to determine where <code>GrailsPlugin</code> instances are loaded
 * from.</p>
 *
 * @author Steven Devijver
 * @author Graeme Rocher
 * 
 * @since 0.2
 * @see BeanDefinitionRegistry
 */
public interface GrailsPlugin extends ApplicationContextAware {

    int EVENT_ON_CHANGE = 0;
    int EVENT_ON_CONFIG_CHANGE = 1;
    int EVENT_ON_SHUTDOWN = 2;

    String DO_WITH_DYNAMIC_METHODS = "doWithDynamicMethods";

    /**
     * The prefix used in plug-ins paths 
     */
    String PLUGINS_PATH = "/plugins";
    /**
	 * Defines the name of the property that specifies resources which this plugin monitors for changes
	 * in the format a Ant-style path
	 */
    String WATCHED_RESOURCES = "watchedResources";
    /**
     * Defines the name of the property that specifies a List or plugins that this plugin evicts
     * Eviction occurs when the PluginManager loads 
     */
    String EVICT = "evict";
    
    /**
     * The status of the plugin
     */
    String STATUS = "status";

    /**
     * When a plugin is "enabled" it will be loaded as usual
     */
    String STATUS_ENABLED = "enabled";
    /**
     * When a plugin is "disabled" it will not be loaded
     */
    String STATUS_DISABLED = "disabled";
    /**
     * Defines the name of the property that defines a list of plugin names that this plugin influences.
     * A influenced plugin will be refreshed (@see refresh()) when a watched resource changes
     */
	String INFLUENCES = "influences";
    /**
	 * Defines the name of the property that defines the closure that will be invoked
	 * when a watched resource changes
	 */
	String ON_CHANGE = "onChange";
    /**
     * Defines the name of the property that holds a closure to be invoked when shutdown is called
     */
    String ON_SHUTDOWN = "onShutdown";
    /**
	 * Defines the name of the property that defines the closure that will be invoked
	 * when a the Grails configuration object changes
	 */
	String ON_CONFIG_CHANGE = "onConfigChange";
    /**
	 * Defines the name of the property that defines the closure that will be invoked
	 * when the web.xml is being generated
	 */
	String DO_WITH_WEB_DESCRIPTOR = "doWithWebDescriptor";
    /**
	 * Defines the convention that appears within plugin class names
	 */
	String TRAILING_NAME = "GrailsPlugin";
    /**
	 * Defines the name of the property that specifies the plugin version
	 */
	String VERSION = "version";
    /**
	 * Defines the name of the property that defines the closure that will be invoked during runtime spring configuration
	 */
	String DO_WITH_SPRING = "doWithSpring";
    /**
	 * Defines the name of the property that defines a closure that will be invoked after intialisation
	 * and when the application context has been built
	 */
	String DO_WITH_APPLICATION_CONTEXT = "doWithApplicationContext";

    /**
	 * Defines the name of the property that specifies which plugins this plugin depends on
	 */
	String DEPENDS_ON = "dependsOn";
    /**
	 * Define the list of ArtefactHandlers supporting by the plugin
	 */
	String ARTEFACTS = "artefacts";
    /**
     * The name of the property that provides a list of shipped, but overridable artefactssw
     */
    String PROVIDED_ARTEFACTS = "providedArtefacts";

    /**
     * <p>This method is called to allow the plugin to add {@link org.springframework.beans.factory.config.BeanDefinition}s
     * to the {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}.</p>
     *
     * @param applicationContext The Spring ApplicationContext instance
     */
    void doWithApplicationContext(ApplicationContext applicationContext);
    
    
    /**
     * Executes the plugin code that performs runtime configuration as defined in the doWithSpring closure
     * 
     * @param springConfig The RuntimeSpringConfiguration instance
     */
    void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig);

    /**
     * Handles processing of web.xml. The method is passed a GPathResult which is parsed by
     * groovy.util.XmlSlurper. A plug-in can then manipulate the in-memory XML however it chooses
     * Once all plug-ins have been processed the web.xml is then written to disk based on its in-memory form
     * 
     * @param webXml The GPathResult representing web.xml
     */
    void doWithWebDescriptor(GPathResult webXml);
    
    /**
     * 
     * @return The name of the plug-in
     */
	String getName();


	/**
	 * 
	 * @return The version of the plug-in
	 */
	String getVersion();


    /**
     * Returns the path of the plug-in
     *
     * @return A String that makes up the path to the plug-in in the format /plugins/PLUGIN_NAME-PLUGIN_VERSION
     */
    String getPluginPath();


    /**
	 * 
	 * @return The names of the plugins this plugin is dependant on
	 */
	String[] getDependencyNames();
	
	/**
	 * 
	 * @return The names of the plugins this plugin should evict onload
	 */
	String[] getEvictionNames();
	
	/**
	 * Retrieves the names of plugins that this plugin should be loaded after. This differs
	 * from dependencies in that if that plugin doesn't exist this plugin will still be loaded.
	 * It is a way of enforcing plugins are loaded before, but not necessarily needed
	 *  
	 * @return The names of the plugins that this plugin should be loaded after
	 */
	String[] getLoadAfterNames();


	/**
	 * The version of the specified dependency
	 * 
	 * @param name the name of the dependency
	 * @return The version
	 */
	String getDependentVersion(String name);
	
	
	/**
	 * When called this method checks for any changes to the plug-ins watched resources
	 * and reloads appropriately
     *
     * @return Returns true when the plug-in itself changes in some way, as oppose to plug-in resources 
	 *
	 */
	boolean checkForChanges();
	
	/**
	 * Refreshes this Grails plugin reloading any watched resources as necessary
	 *
	 */
	void refresh();
	
	/**
	 * Retrieves the plugin manager if known, otherwise returns null
	 * @return The PluginManager or null
	 */
	GrailsPluginManager getManager();

    /**
     * Retrieves the wrapped plugin instance for this plugin
     * @return The plugin instance
     */
    GroovyObject getInstance();

    /**
	 * Sets the plugin manager for this plugin
	 * 
	 * @param manager A GrailsPluginManager instance 
	 */
	void setManager(GrailsPluginManager manager);


	void setApplication(GrailsApplication application);


	/**
	 * Calls a "doWithDynamicMethods" closure that allows a plugin to register dynamic methods at runtime
	 * @param applicationContext The Spring ApplicationContext instance 
	 *
	 */
	void doWithDynamicMethods(ApplicationContext applicationContext);

    /** 
     * @return Whether the plugin is enabled or not
     */
    boolean isEnabled();

    /**
     * Retrieve the plugin names that this plugin is observing for changes
     *
     * @return The names of the observed plugins
     */
    String[] getObservedPluginNames();

    /**
     * Notifies this plugin of the specified Event calling the onChange listener
     *
     * @param event The event to listen for
     */
    void notifyOfEvent(Map event);

    /**
     * Notifies the plugin of a specific event for the given event id, which is one of ON_CHANGE, ON_CONFIG_CHANGE
     *
     * @param eventKind The event kind
     * @param source The source of the event
     * @return a Map that represents the event
     */
    Map notifyOfEvent(int eventKind, Object source);

    /**
     * Called prior to the initialisation of the GrailsApplication instance to allow the registration
     * of additonal ArtefactHandlers
     *
     * @see org.codehaus.groovy.grails.commons.ArtefactHandler
     *
     */
    void doArtefactConfiguration();

    /**
     * Retrieves an array of provided Artefacts that are pre-compiled additions to the GrailsApplication object
     * but are overridable by the end-user
     *
     * @return A list of provided artefacts
     */
    Class[] getProvidedArtefacts();
}
