/**
 * 
 */
package org.codehaus.groovy.grails.plugins;

import groovy.lang.MetaClass;
import groovy.lang.MetaClassRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.metaclass.DynamicMethodsMetaClass;
import org.codehaus.groovy.grails.commons.metaclass.ExpandoMetaClass;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * Abstract implementation of the GrailsPluginManager interface
 * 
 * @author Graeme Rocher
 * @since 0.4
 *
 */
public abstract class AbstractGrailsPluginManager implements GrailsPluginManager {

	protected List pluginList = new ArrayList();
	protected GrailsApplication application;
	protected Resource[] pluginResources = new Resource[0];
	protected Map plugins = new HashMap();
	protected Class[] pluginClasses = new Class[0];
	protected boolean initialised = false;
	protected ApplicationContext applicationContext;
	
	
	public AbstractGrailsPluginManager(GrailsApplication application) {
		super();
		if(application == null)
			throw new IllegalArgumentException("Argument [application] cannot be null!");
		
		this.application = application;
	}
	/**
	 * @return the initialised
	 */
	public boolean isInitialised() {
		return initialised;
	}
	protected void checkInitialised() {
		if(!initialised)
			throw new IllegalStateException("Must call loadPlugins() before invoking configurational methods on GrailsPluginManager");
	}
	
	/**
	 * Base implementation that simply goes through the list of plugins and calls doWithRuntimeConfiguration on each
	 * @param springConfig The RuntimeSpringConfiguration instance
	 */
	public void doRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		checkInitialised();
		for (Iterator i = pluginList.iterator(); i.hasNext();) {
			GrailsPlugin plugin = (GrailsPlugin) i.next();
			plugin.doWithRuntimeConfiguration(springConfig);
		}
	}
	/**
	 * Base implementation that will perform runtime configuration for the specified plugin 
	 * name
	 */
	public void doRuntimeConfiguration(String pluginName, RuntimeSpringConfiguration springConfig) {
		checkInitialised();
		GrailsPlugin plugin = getGrailsPlugin(pluginName);
		if(plugin == null) throw new PluginException("Plugin ["+pluginName+"] not found");
	
		String[] dependencyNames = plugin.getDependencyNames();
		for (int i = 0; i < dependencyNames.length; i++) {
			String dn = dependencyNames[i];
			GrailsPlugin current = getGrailsPlugin(dn);
			current.doWithRuntimeConfiguration(springConfig);
		}
		String[] loadAfters = plugin.getLoadAfterNames();
		for (int i = 0; i < loadAfters.length; i++) {
			String name = loadAfters[i];
			GrailsPlugin current = getGrailsPlugin(name);
			if(current != null) {
				current.doWithRuntimeConfiguration(springConfig);
			}
		}
		plugin.doWithRuntimeConfiguration(springConfig);
	}
	/**
	 * Base implementation that will simply go through each plugin and call doWithApplicationContext
	 * on each
	 */
	public void doPostProcessing(ApplicationContext applicationContext) {
		checkInitialised();
		for (Iterator i = pluginList.iterator(); i.hasNext();) {
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
	public GrailsPlugin getGrailsPlugin(String name, Object version) {
		GrailsPlugin plugin = (GrailsPlugin)this.plugins.get(name);
		if(plugin != null) {
			if(plugin.getVersion().equals(version))
				return plugin;
		}
		return null;
	}
	public boolean hasGrailsPlugin(String name) {
		return this.plugins.containsKey(name);
	}
	public void doDynamicMethods() {
		checkInitialised();
		Class[] allClasses = application.getAllClasses();
		if(allClasses != null) {
			ExpandoMetaClass[] metaClasses = new ExpandoMetaClass[allClasses.length];
			MetaClassRegistry registry = InvokerHelper.getInstance().getMetaRegistry();
			
			
			for (int i = 0; i < allClasses.length; i++) {
				Class c = allClasses[i];
				MetaClass mc = registry.getMetaClass(c);
				if(mc instanceof DynamicMethodsMetaClass) {
					ExpandoMetaClass adaptee = new ExpandoMetaClass(c);
					adaptee.setAllowChangesAfterInit(true);
					adaptee.initialize();
					
					((DynamicMethodsMetaClass)mc).setAdaptee(adaptee);
					metaClasses[i] = adaptee;
				}
				else {
					ExpandoMetaClass emc = new ExpandoMetaClass(c,true);
					emc.setAllowChangesAfterInit(true);
					emc.initialize();
					metaClasses[i] = emc;	
				}
				
			}
			for (Iterator i = pluginList.iterator(); i.hasNext();) {
				GrailsPlugin plugin = (GrailsPlugin) i.next();
				plugin.doWithDynamicMethods(applicationContext);
			}						
		}
	}
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		for (Iterator i = pluginList.iterator(); i.hasNext();) {
			GrailsPlugin plugin = (GrailsPlugin) i.next();
			plugin.setApplicationContext(applicationContext);
		}
	}
	public void setApplication(GrailsApplication application) {
		if(application == null) throw new IllegalArgumentException("Argument [application] cannot be null");
		this.application = application;
		for (Iterator i = pluginList.iterator(); i.hasNext();) {
			GrailsPlugin plugin = (GrailsPlugin) i.next();
			plugin.setApplication(application);
		}
	}

}
