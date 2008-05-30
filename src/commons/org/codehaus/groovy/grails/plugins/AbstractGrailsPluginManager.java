/**
 * 
 */
package org.codehaus.groovy.grails.plugins;

import groovy.lang.ExpandoMetaClass;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.util.*;

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
    protected Map failedPlugins = new HashMap();


    public AbstractGrailsPluginManager(GrailsApplication application) {
		super();
		if(application == null)
			throw new IllegalArgumentException("Argument [application] cannot be null!");
		
		this.application = application;
	}

    public GrailsPlugin[] getAllPlugins() {
        return (GrailsPlugin[])pluginList.toArray(new GrailsPlugin[pluginList.size()]);
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

    public GrailsPlugin getFailedPlugin(String name) {
        if(name.indexOf('-') > -1) name = GrailsClassUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        return (GrailsPlugin)this.failedPlugins.get(name);
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
          doRuntimeConfigurationForDependencies(dependencyNames, springConfig);
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

      private void doRuntimeConfigurationForDependencies(String[] dependencyNames, RuntimeSpringConfiguration springConfig) {
          for (int i = 0; i < dependencyNames.length; i++) {
              String dn = dependencyNames[i];
              GrailsPlugin current = getGrailsPlugin(dn);
              if(current == null) throw new PluginException("Cannot load Plugin. Dependency ["+current+"] not found");
              String[] pluginDependencies = current.getDependencyNames();
              if(pluginDependencies.length > 0)
                  doRuntimeConfigurationForDependencies(pluginDependencies, springConfig);
              current.doWithRuntimeConfiguration(springConfig);
          }
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
        if(name.indexOf('-') > -1) name = GrailsClassUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        return (GrailsPlugin)this.plugins.get(name);
	}
	public GrailsPlugin getGrailsPlugin(String name, Object version) {
      if(name.indexOf('-') > -1) name = GrailsClassUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        GrailsPlugin plugin = (GrailsPlugin)this.plugins.get(name);
		if(plugin != null) {
			if(GrailsPluginUtils.isValidVersion(plugin.getVersion(), version.toString()))
				return plugin;
		}
		return null;
	}
	public boolean hasGrailsPlugin(String name) {
        if(name.indexOf('-') > -1) name = GrailsClassUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        return this.plugins.containsKey(name);
	}
	public void doDynamicMethods() {
		checkInitialised();
		Class[] allClasses = application.getAllClasses();
		if(allClasses != null) {
			ExpandoMetaClass[] metaClasses = new ExpandoMetaClass[allClasses.length];
			for (int i = 0; i < allClasses.length; i++) {
				Class c = allClasses[i];
                ExpandoMetaClass emc = new ExpandoMetaClass(c,true, true);
                emc.initialize();
                metaClasses[i] = emc;
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

    public void registerProvidedArtefacts(GrailsApplication application) {
        checkInitialised();
        for (Iterator i = pluginList.iterator(); i.hasNext();) {
            GrailsPlugin plugin = (GrailsPlugin) i.next();
            final Class[] artefacts = plugin.getProvidedArtefacts();
            for (int j = 0; j < artefacts.length; j++) {
                Class artefact = artefacts[j];

                String shortName = GrailsClassUtils.getShortName(artefact);
                if(!isAlreadyRegistered(application, artefact, shortName)) {
                    application.addArtefact(artefact);
                }

            }
        }

    }
    private boolean isAlreadyRegistered(GrailsApplication application, Class artefact, String shortName) {
        return application.getClassForName(shortName) != null || application.getClassForName(artefact.getName()) != null;
    }

    public void doArtefactConfiguration() {
        checkInitialised();
        for (Iterator i = pluginList.iterator(); i.hasNext();) {
            GrailsPlugin plugin = (GrailsPlugin) i.next();
            plugin.doArtefactConfiguration();
        }
    }

    public void shutdown() {
        checkInitialised();
        for (Iterator i = pluginList.iterator(); i.hasNext();) {
            GrailsPlugin plugin = (GrailsPlugin) i.next();
            plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_SHUTDOWN, plugin);
        }
    }
}
