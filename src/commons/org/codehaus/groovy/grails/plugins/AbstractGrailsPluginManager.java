/**
 * 
 */
package org.codehaus.groovy.grails.plugins;

import grails.util.BuildScope;
import grails.util.GrailsNameUtils;
import groovy.lang.ExpandoMetaClass;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.ArtefactHandler;
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

	protected List<GrailsPlugin> pluginList = new ArrayList<GrailsPlugin>();
	protected GrailsApplication application;
	protected Resource[] pluginResources = new Resource[0];
	protected Map<String, GrailsPlugin> plugins = new HashMap<String, GrailsPlugin>();
    protected Map<String, GrailsPlugin> classNameToPluginMap = new HashMap<String, GrailsPlugin>();
	protected Class[] pluginClasses = new Class[0];
	protected boolean initialised = false;
	protected ApplicationContext applicationContext;
    protected Map<String, GrailsPlugin> failedPlugins = new HashMap<String, GrailsPlugin>();
    protected boolean loadCorePlugins = true;


    public AbstractGrailsPluginManager(GrailsApplication application) {
		super();
		if(application == null)
			throw new IllegalArgumentException("Argument [application] cannot be null!");
		
		this.application = application;
	}

    public GrailsPlugin[] getAllPlugins() {
        return pluginList.toArray(new GrailsPlugin[pluginList.size()]);
    }

    public GrailsPlugin[] getFailedLoadPlugins() {
        return failedPlugins.values().toArray(new GrailsPlugin[failedPlugins.size()]);
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
        if(name.indexOf('-') > -1) name = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        return this.failedPlugins.get(name);
    }

    /**
	 * Base implementation that simply goes through the list of plugins and calls doWithRuntimeConfiguration on each
	 * @param springConfig The RuntimeSpringConfiguration instance
	 */
	public void doRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		checkInitialised();
        for (GrailsPlugin plugin : pluginList) {
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                plugin.doWithRuntimeConfiguration(springConfig);
            }
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
          if(!plugin.supportsCurrentScopeAndEnvironment()) return;

          String[] dependencyNames = plugin.getDependencyNames();
          doRuntimeConfigurationForDependencies(dependencyNames, springConfig);
          String[] loadAfters = plugin.getLoadAfterNames();
          for (String name : loadAfters) {
             GrailsPlugin current = getGrailsPlugin(name);
             if (current != null) {
                current.doWithRuntimeConfiguration(springConfig);
             }
          }
          plugin.doWithRuntimeConfiguration(springConfig);
      }

      private void doRuntimeConfigurationForDependencies(String[] dependencyNames, RuntimeSpringConfiguration springConfig) {
          for (String dn : dependencyNames) {
              GrailsPlugin current = getGrailsPlugin(dn);
              if (current == null)
                  throw new PluginException("Cannot load Plugin. Dependency [" + current + "] not found");
              String[] pluginDependencies = current.getDependencyNames();
              if (pluginDependencies.length > 0)
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
        for (Object aPluginList : pluginList) {
            GrailsPlugin plugin = (GrailsPlugin) aPluginList;
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                plugin.doWithApplicationContext(applicationContext);
            }
        }
	}
	public Resource[] getPluginResources() {
		return this.pluginResources;
	}
	public GrailsPlugin getGrailsPlugin(String name) {
        if(name.indexOf('-') > -1) name = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        return this.plugins.get(name);
	}

    public GrailsPlugin getGrailsPluginForClassName(String name) {
        return this.classNameToPluginMap.get(name);
    }

    public GrailsPlugin getGrailsPlugin(String name, Object version) {
      if(name.indexOf('-') > -1) name = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        GrailsPlugin plugin = this.plugins.get(name);
		if(plugin != null) {
			if(GrailsPluginUtils.isValidVersion(plugin.getVersion(), version.toString()))
				return plugin;
		}
		return null;
	}
	public boolean hasGrailsPlugin(String name) {
        if(name.indexOf('-') > -1) name = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        return this.plugins.containsKey(name);
	}
	public void doDynamicMethods() {
		checkInitialised();
		Class[] allClasses = application.getAllClasses();
		if(allClasses != null) {
            for (Class c : allClasses) {
                ExpandoMetaClass emc = new ExpandoMetaClass(c, true, true);
                emc.initialize();
            }
            for (Object aPluginList : pluginList) {
                GrailsPlugin plugin = (GrailsPlugin) aPluginList;
                plugin.doWithDynamicMethods(applicationContext);
            }
		}
	}
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
        for (Object aPluginList : pluginList) {
            GrailsPlugin plugin = (GrailsPlugin) aPluginList;
            plugin.setApplicationContext(applicationContext);
        }
	}
	public void setApplication(GrailsApplication application) {
		if(application == null) throw new IllegalArgumentException("Argument [application] cannot be null");
		this.application = application;
        for (Object aPluginList : pluginList) {
            GrailsPlugin plugin = (GrailsPlugin) aPluginList;
            plugin.setApplication(application);
        }
	}

    public void registerProvidedArtefacts(GrailsApplication application) {
        checkInitialised();
        for (Object aPluginList : pluginList) {
            GrailsPlugin plugin = (GrailsPlugin) aPluginList;
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                final Class[] artefacts = plugin.getProvidedArtefacts();
                for (Class artefact : artefacts) {
                    String shortName = GrailsNameUtils.getShortName(artefact);
                    if (!isAlreadyRegistered(application, artefact, shortName)) {
                        application.addOverridableArtefact(artefact);
                    }

                }
            }
        }

    }
    private boolean isAlreadyRegistered(GrailsApplication application, Class artefact, String shortName) {
        return application.getClassForName(shortName) != null || application.getClassForName(artefact.getName()) != null;
    }

    public void doArtefactConfiguration() {
        checkInitialised();
        for (Object aPluginList : pluginList) {
            GrailsPlugin plugin = (GrailsPlugin) aPluginList;

            if (plugin.supportsCurrentScopeAndEnvironment()) {
                plugin.doArtefactConfiguration();
            }
        }
    }

    public void shutdown() {
        checkInitialised();
        for (Object aPluginList : pluginList) {
            GrailsPlugin plugin = (GrailsPlugin) aPluginList;
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_SHUTDOWN, plugin);
            }
        }
    }

    public boolean supportsCurrentBuildScope(String pluginName) {
        GrailsPlugin plugin = getGrailsPlugin(pluginName);
        return plugin == null || plugin.supportsScope(BuildScope.getCurrent());
    }

    public void setLoadCorePlugins(boolean shouldLoadCorePlugins) {
        this.loadCorePlugins = shouldLoadCorePlugins;
    }

    public void informOfClassChange(Class aClass) {
        if(aClass !=null && application!=null) {
            ArtefactHandler handler = application.getArtefactType(aClass);
            if(handler!=null) {
                String pluginName = handler.getPluginName();
                if(pluginName!=null) {
                    GrailsPlugin plugin = getGrailsPlugin(pluginName);
                    if(plugin!=null) {
                        plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CHANGE, aClass);
                    }
                }

            }
        }
    }
}
