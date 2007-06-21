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

import grails.config.ConfigObject;
import grails.config.ConfigSlurper;
import grails.util.GrailsUtil;
import groovy.lang.*;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.codehaus.groovy.grails.support.ParentApplicationContextAware;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.xml.sax.SAXException;

import javax.servlet.ServletContext;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

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
      private ApplicationContext applicationContext;
      private List pluginList = new ArrayList();
      private ApplicationContext parentCtx;
      private PathMatchingResourcePatternResolver resolver;
      boolean initialised = false;
      private Map delayedEvictions = new HashMap();
      private ServletContext servletContext;
      private Map pluginToObserverMap = new HashMap();
      private long configLastModified;


    public DefaultGrailsPluginManager(String resourcePath, GrailsApplication application) throws IOException {
          super();
          if(application == null)
              throw new IllegalArgumentException("Argument [application] cannot be null!");

          resolver = new PathMatchingResourcePatternResolver();
          try {
              this.pluginResources = resolver.getResources(resourcePath);
          }
          catch(IOException ioe) {
              LOG.debug("Unable to load plugins for resource path " + resourcePath, ioe);
          }
          //this.corePlugins = new PathMatchingResourcePatternResolver().getResources("classpath:org/codehaus/groovy/grails/**/plugins/**GrailsPlugin.groovy");
          this.application = application;
      }


      public DefaultGrailsPluginManager(String[] pluginResources, GrailsApplication application) {
          if(application == null)
              throw new IllegalArgumentException("Argument [application] cannot be null!");
          resolver = new PathMatchingResourcePatternResolver();

          List resourceList = new ArrayList();
          for (int i = 0; i < pluginResources.length; i++) {
              String resourcePath = pluginResources[i];
              try {
                  Resource[] resources = resolver.getResources(resourcePath);
                  for (int j = 0; j < resources.length; j++) {
                      Resource resource = resources[j];
                      resourceList.add(resource);
                  }

              }
              catch(IOException ioe) {
                  LOG.debug("Unable to load plugins for resource path " + resourcePath, ioe);
              }

          }

          this.pluginResources = (Resource[])resourceList.toArray(new Resource[resourceList.size()]);
          this.application = application;
      }

      public DefaultGrailsPluginManager(Class[] plugins, GrailsApplication application) throws IOException {
          this.pluginClasses = plugins;
          resolver = new PathMatchingResourcePatternResolver();
          //this.corePlugins = new PathMatchingResourcePatternResolver().getResources("classpath:org/codehaus/groovy/grails/**/plugins/**GrailsPlugin.groovy");
          this.application = application;
      }

      public DefaultGrailsPluginManager(Resource[] pluginFiles, GrailsApplication application) {
          if(application == null)
              throw new IllegalArgumentException("Argument [application] cannot be null!");

          resolver = new PathMatchingResourcePatternResolver();
          this.pluginResources = pluginFiles;
          this.application = application;
      }


      /**
       * @return the initialised
       */
      public boolean isInitialised() {
          return initialised;
      }

      public void refreshPlugin(String name) {
          if(hasGrailsPlugin(name)) {
              GrailsPlugin plugin = getGrailsPlugin(name);
              plugin.refresh();
          }
      }

    public Collection getPluginObservers(GrailsPlugin plugin) {
        if(plugin == null) throw new IllegalArgumentException("Argument [plugin] cannot be null");
        
        Collection c = (Collection)this.pluginToObserverMap.get(plugin.getName());
        if(c != null) {
            return c;
        }
        return Collections.EMPTY_SET;
    }

    public void informObservers(String pluginName, Map event) {
        GrailsPlugin plugin = getGrailsPlugin(pluginName);
        if(plugin != null) {
            Collection observers = getPluginObservers(plugin);
            for (Iterator i = observers.iterator(); i.hasNext();) {
                GrailsPlugin observingPlugin = (GrailsPlugin) i.next();
                observingPlugin.notifyOfEvent(event);
            }
        }
    }

    /* (non-Javadoc)
    * @see org.codehaus.groovy.grails.plugins.GrailsPluginManager#loadPlugins()
    */
  public void loadPlugins()
                  throws PluginException {
      if(!this.initialised) {
          GroovyClassLoader gcl = application.getClassLoader();
          // load core plugins first
          loadCorePlugins();

          LOG.info("Attempting to load ["+pluginResources.length+"] user defined plugins");
          for (int i = 0; i < pluginResources.length; i++) {
              Resource r = pluginResources[i];

              Class pluginClass = loadPluginClass(gcl, r);
              GrailsPlugin plugin = new DefaultGrailsPlugin(pluginClass, r, application);
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
          if(!delayedEvictions.isEmpty()) {
              processDelayedEvictions();
          }

          // TODO: Remove this message in further releases
          if( new File("./grails-app/jobs").exists() && plugins.get("quartz") == null ) {
              GrailsUtil.deprecated( "Job scheduling with Quartz was moved from Grails core " +
                      "to Quartz plugin. Please, install this plugin with " +
                      "'grails install-plugin Quartz 0.1'. If you don't want use scheduling " +
                      "just remove 'jobs' folder under 'grails-app'" );
          }
          initializePlugins();
          initialised = true;
      }
  }

  private void processDelayedEvictions() {
      for (Iterator i = delayedEvictions.keySet().iterator(); i.hasNext();) {
          GrailsPlugin plugin = (GrailsPlugin) i.next();
          String[] pluginToEvict = (String[])delayedEvictions.get(plugin);

          for (int j = 0; j < pluginToEvict.length; j++) {
              String pluginName = pluginToEvict[j];
              evictPlugin(plugin, pluginName);
          }
      }
  }

  private void initializePlugins() {
      for (Iterator i = plugins.values().iterator(); i.hasNext();) {
          Object plugin = i.next();
          if(plugin instanceof ApplicationContextAware) {
              ((ApplicationContextAware)plugin).setApplicationContext(applicationContext);
          }
      }
  }

  /**
   * This method will search the classpath for .class files inside the org.codehaus.groovy.grails package
   * which are the core plugins. The ones found will be loaded automatically
   *
   */
  private void loadCorePlugins() {
      try {
          Resource[] resources = resolver.getResources("classpath*:org/codehaus/groovy/grails/**/plugins/**/*GrailsPlugin.class");
          if(resources.length > 0) {
              loadCorePluginsFromResources(resources);
          }
          else {
              LOG.warn("WARNING: Grails was unable to load core plugins dynamically. This is normally a problem with the container class loader configuration, see troubleshooting and FAQ for more info. ");
              loadCorePluginsStatically();
          }
      } catch (IOException e) {
          throw new PluginException("I/O exception configuring core plug-ins: " + e.getMessage(), e);
      }
  }

  private void loadCorePluginsStatically() {


      // This is a horrible hard coded hack, but there seems to be no way to resolve .class files dynamically
      // on OC4J. If anyones knows how to fix this shout
      loadCorePlugin("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin");
      loadCorePlugin("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin");
      loadCorePlugin("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin");
      loadCorePlugin("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin");
      loadCorePlugin("org.codehaus.groovy.grails.plugins.web.ServletsGrailsPlugin");
      loadCorePlugin("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin");
      loadCorePlugin("org.codehaus.groovy.grails.plugins.orm.hibernate.HibernateGrailsPlugin");
      loadCorePlugin("org.codehaus.groovy.grails.plugins.services.ServicesGrailsPlugin");
      loadCorePlugin("org.codehaus.groovy.grails.plugins.scaffolding.ScaffoldingGrailsPlugin");
  }

  private void loadCorePluginsFromResources(Resource[] resources) throws IOException {
      LOG.debug("Attempting to load ["+resources.length+"] core plugins");
      for (int i = 0; i < resources.length; i++) {
          Resource resource = resources[i];
          String url = resource.getURL().toString();
          int packageIndex = url.indexOf("org/codehaus/groovy/grails");
          url = url.substring(packageIndex, url.length());
          url = url.substring(0,url.length()-6);
          String className = url.replace('/', '.');

          loadCorePlugin(className);
      }
  }


  private Class attemptCorePluginClassLoad(String pluginClassName) {
      try {
          return application.getClassLoader().loadClass(pluginClassName);
      } catch (ClassNotFoundException e) {
          LOG.warn("[GrailsPluginManager] Core plugin ["+pluginClassName+"] not found, resuming load without..");
          if(LOG.isDebugEnabled())
              LOG.debug(e.getMessage(),e);
      }
      return null;
  }

  private void loadCorePlugin(String pluginClassName) {
      Class pluginClass = attemptCorePluginClassLoad(pluginClassName);

      if(pluginClass != null && !Modifier.isAbstract(pluginClass.getModifiers()) && pluginClass != DefaultGrailsPlugin.class) {
          GrailsPlugin plugin = new DefaultGrailsPlugin(pluginClass, application);
          plugin.setApplicationContext(applicationContext);
          attemptPluginLoad(plugin);
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
              if(!hasValidPluginsToLoadBefore(plugin)) {
                  registerPlugin(plugin);
              }
              else {
                  delayedLoadPlugins.add(plugin);
              }
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
              else {
                  LOG.warn("WARNING: Plugin ["+plugin.getName()+"] cannot be loaded because its dependencies ["+ArrayUtils.toString(plugin.getDependencyNames())+"] cannot be resolved");
              }

          }
      }
  }

  private boolean hasValidPluginsToLoadBefore(GrailsPlugin plugin) {
      String[] loadAfterNames = plugin.getLoadAfterNames();
      for (Iterator i = this.delayedLoadPlugins.iterator(); i.hasNext();) {
          GrailsPlugin other = (GrailsPlugin) i.next();
          for (int j = 0; j < loadAfterNames.length; j++) {
              String name = loadAfterNames[j];
              if(other.getName().equals(name) && areDependenciesResolved(other))
                  return true;
          }
      }
      return false;
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
          String version = plugin.getDependentVersion(name);

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
              String version = plugin.getDependentVersion(name);
              if(!hasGrailsPlugin(name, version)) {
                  return false;
              }
          }
      }
      return true;
  }

  /**
   * Returns true if there are no plugins left that should, if possible, be loaded before this plugin
   *
   * @param plugin The plugin
   * @return True if there are
   */
  private boolean areNoneToLoadBefore(GrailsPlugin plugin) {
      String[] loadAfterNames = plugin.getLoadAfterNames();
      if(loadAfterNames.length > 0) {
          for (int i = 0; i < loadAfterNames.length; i++) {
              String name = loadAfterNames[i];
              if(getGrailsPlugin(name) == null)
                  return false;
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
      if(areDependenciesResolved(plugin) && areNoneToLoadBefore(plugin)) {
          registerPlugin(plugin);
      }
      else {
          delayedLoadPlugins.add(plugin);
      }
  }


  private void registerPlugin(GrailsPlugin plugin) {
      if(plugin.isEnabled()) {
          if(LOG.isDebugEnabled()) {
              LOG.debug("Grails plug-in ["+plugin.getName()+"] with version ["+plugin.getVersion()+"] loaded successfully");
          }

          if(plugin instanceof ParentApplicationContextAware) {
              ((ParentApplicationContextAware)plugin).setParentApplicationContext(parentCtx);
          }
          plugin.setManager(this);
          String[] evictionNames = plugin.getEvictionNames();
          if(evictionNames.length > 0)
              delayedEvictions.put(plugin, evictionNames);

          String[] observedPlugins = plugin.getObservedPluginNames();
          for (int i = 0; i < observedPlugins.length; i++) {
              String observedPlugin = observedPlugins[i];
              Set observers = (Set)pluginToObserverMap.get(observedPlugin);
              if(observers == null) {
                  observers = new HashSet();
                  pluginToObserverMap.put(observedPlugin, observers);
              }
              observers.add(plugin);
          }
          pluginList.add(plugin);
          plugins.put(plugin.getName(), plugin);
      }
      else {
          if(LOG.isInfoEnabled()) {
              LOG.info("Grails plugin " + plugin + " is disabled and was not loaded");
          }
      }
  }

  protected void evictPlugin(GrailsPlugin evictor, String evicteeName) {
      GrailsPlugin pluginToEvict = (GrailsPlugin)plugins.get(evicteeName);
      if(pluginToEvict!=null) {
          pluginList.remove(pluginToEvict);
          plugins.remove(pluginToEvict.getName());

          if(LOG.isInfoEnabled()) {
              LOG.info("Grails plug-in "+pluginToEvict+" was evicted by " + evictor);
          }
      }
  }

  private boolean hasGrailsPlugin(String name, String version) {
      return getGrailsPlugin(name, version) != null;
  }

  /* (non-Javadoc)
   * @see org.codehaus.groovy.grails.plugins.GrailsPluginManager#doRuntimeConfiguration(org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration)
   */
  public void doRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
      checkInitialised();
      for (Iterator i = pluginList.iterator(); i.hasNext();) {
          GrailsPlugin plugin = (GrailsPlugin) i.next();
          plugin.doWithRuntimeConfiguration(springConfig);
      }
  }

  private void checkInitialised() {
      if(!initialised)
          throw new IllegalStateException("Must call loadPlugins() before invoking configurational methods on GrailsPluginManager");
  }

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

  /* (non-Javadoc)
    * @see org.codehaus.groovy.grails.plugins.GrailsPluginManager#doPostProcessing(org.springframework.context.ApplicationContext)
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
          if(plugin.getVersion().equals(version.toString()))
              return plugin;
      }
      return null;
  }

  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      this.applicationContext = applicationContext;
      for (Iterator i = pluginList.iterator(); i.hasNext();) {
          GrailsPlugin plugin = (GrailsPlugin) i.next();
          plugin.setApplicationContext(applicationContext);
      }
  }

  public boolean hasGrailsPlugin(String name) {
      return this.plugins.containsKey(name);
  }

  public void setParentApplicationContext(ApplicationContext parent) {
      this.parentCtx = parent;
  }

  public void checkForChanges() {
      checkForConfigChanges();
      for (Iterator i = pluginList.iterator(); i.hasNext();) {
          GrailsPlugin plugin = (GrailsPlugin) i.next();
          if(plugin.checkForChanges()) {
              LOG.info("Plugin "+plugin+" changed, re-registering beans...");
              reloadPlugin(plugin);
          }
      }
  }

    private void checkForConfigChanges() {
        ConfigObject config = application.getConfig();
        URL configURL = config.getConfigFile();
        if(configURL != null) {
            URLConnection connection;
            try {
                connection = configURL.openConnection();
            } catch (IOException e) {
                LOG.error("I/O error obtaining URL connection for configuration ["+configURL+"]: " + e.getMessage(),e);
                return;
            }


            long lastModified = connection.getLastModified();
            if(configLastModified == 0) {
                configLastModified = lastModified;
            }
            else {
                if(configLastModified<lastModified) {
                    LOG.info("Configuration ["+configURL+"] changed, reloading changes..");
                    ConfigSlurper slurper = new ConfigSlurper(GrailsUtil.getEnvironment());

                    try {
                        config = slurper.parse(configURL);
                        ConfigurationHolder.setConfig(config);
                        configLastModified = lastModified;
                        informPluginsOfConfigChange();

                    } catch (GroovyRuntimeException gre) {
                        LOG.error("Unable to reload configuration. Please correct problem and try again: " + gre.getMessage(),gre);
                    }
                }
            }
        }
    }

    private void informPluginsOfConfigChange() {
        LOG.info("Informing plug-ins of configuration change..");
        for (Iterator i = pluginList.iterator(); i.hasNext();) {
            GrailsPlugin plugin = (GrailsPlugin) i.next();
            plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CONFIG_CHANGE, application.getConfig());
        }
    }

    private void reloadPlugin(GrailsPlugin plugin) {
        plugin.doArtefactConfiguration();

        RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration(this.parentCtx);

        this.doRuntimeConfiguration(plugin.getName(), springConfig);
        springConfig.registerBeansWithContext((StaticApplicationContext)this.applicationContext);

        plugin.doWithApplicationContext(this.applicationContext);
        plugin.doWithDynamicMethods(this.applicationContext);
    }

    public void doWebDescriptor(Resource descriptor, Writer target) {
      try {
          doWebDescriptor( descriptor.getInputStream(), target);
      } catch (IOException e) {
          throw new PluginException("Unable to read web.xml ["+descriptor+"]: " + e.getMessage(),e);
      }
  }

  private void doWebDescriptor(InputStream inputStream, Writer target) {
      checkInitialised();
      try {
          XmlSlurper slurper = new XmlSlurper();

          GPathResult result = slurper.parse(inputStream);

          for (Iterator i = pluginList.iterator(); i.hasNext();) {
              GrailsPlugin plugin = (GrailsPlugin) i.next();
              plugin.doWithWebDescriptor(result);
          }
          Binding b = new Binding();
          b.setVariable("node", result);
          // this code takes the XML parsed by XmlSlurper and writes it out using StreamingMarkupBuilder
          // don't ask me how it works, refer to John Wilson ;-)
          Writable w = (Writable)new GroovyShell(b)
                                      .evaluate("new groovy.xml.StreamingMarkupBuilder().bind { mkp.declareNamespace(\"\":  \"http://java.sun.com/xml/ns/j2ee\"); mkp.yield node}");
          w.writeTo(target);

      } catch (ParserConfigurationException e) {
          throw new PluginException("Unable to configure web.xml due to parser configuration problem: " + e.getMessage(),e);
      } catch (SAXException e) {
          throw new PluginException("XML parsing error configuring web.xml: " + e.getMessage(),e);
      } catch (IOException e) {
          throw new PluginException("Unable to read web.xml" + e.getMessage(),e);
      }
  }

  public void doWebDescriptor(File descriptor, Writer target) {
      try {
          doWebDescriptor(new FileInputStream(descriptor), target);
      } catch (FileNotFoundException e) {
          throw new PluginException("Unable to read web.xml ["+descriptor+"]: " + e.getMessage(),e);
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

  public void doDynamicMethods() {
      checkInitialised();
      for (Iterator i = pluginList.iterator(); i.hasNext();) {
          GrailsPlugin plugin = (GrailsPlugin) i.next();
          plugin.doWithDynamicMethods(applicationContext);
      }
  }

  public void setServletContext(ServletContext servletContext) {
      this.servletContext = servletContext;
  }
  public ServletContext getServletContext() {
      return servletContext;
  }
    
  public void doArtefactConfiguration() {
      checkInitialised();
      for (Iterator i = pluginList.iterator(); i.hasNext();) {
          GrailsPlugin plugin = (GrailsPlugin) i.next();
          plugin.doArtefactConfiguration();
      }	  	  
  }  
}
