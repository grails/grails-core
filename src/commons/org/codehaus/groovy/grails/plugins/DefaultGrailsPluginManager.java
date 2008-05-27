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

import grails.util.GrailsUtil;
import groovy.lang.*;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.codehaus.groovy.grails.support.ParentApplicationContextAware;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import javax.servlet.ServletContext;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
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
public class DefaultGrailsPluginManager extends AbstractGrailsPluginManager implements GrailsPluginManager {

    private static final Log LOG = LogFactory.getLog(DefaultGrailsPluginManager.class);
    private static final Class[] COMMON_CLASSES =
            new Class[]{Boolean.class, Byte.class, Character.class, Class.class, Double.class,Float.class, Integer.class, Long.class,
                        Number.class, Short.class, String.class, BigInteger.class, BigDecimal.class, URL.class, URI.class};





    private List delayedLoadPlugins = new LinkedList();
    private ApplicationContext parentCtx;
    private PathMatchingResourcePatternResolver resolver;
    private Map delayedEvictions = new HashMap();
    private ServletContext servletContext;
    private Map pluginToObserverMap = new HashMap();

    private long configLastModified;
    private PluginFilter pluginFilter;
    private static final String GRAILS_PLUGIN_SUFFIX = "GrailsPlugin";

    public DefaultGrailsPluginManager(String resourcePath, GrailsApplication application) throws IOException {
          super(application);
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
          setPluginFilter();
      }


      public DefaultGrailsPluginManager(String[] pluginResources, GrailsApplication application) {
          super(application);
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
          setPluginFilter();
      }

      public DefaultGrailsPluginManager(Class[] plugins, GrailsApplication application) throws IOException {
          super(application);
          this.pluginClasses = plugins;
          resolver = new PathMatchingResourcePatternResolver();
          //this.corePlugins = new PathMatchingResourcePatternResolver().getResources("classpath:org/codehaus/groovy/grails/**/plugins/**GrailsPlugin.groovy");
          this.application = application;
          setPluginFilter();
      }

      public DefaultGrailsPluginManager(Resource[] pluginFiles, GrailsApplication application) {
          super(application);
          resolver = new PathMatchingResourcePatternResolver();
          this.pluginResources = pluginFiles;
          this.application = application;
          setPluginFilter();
      }
      
      private void setPluginFilter() {
  		 this.pluginFilter = new PluginFilterRetriever().getPluginFilter(this.application.getConfig().toProperties());
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

        // Add any wildcard observers.
        Collection wildcardObservers = (Collection)this.pluginToObserverMap.get("*");
        if(wildcardObservers != null) {
            if(c != null) {
                c.addAll(wildcardObservers);
            }
            else {
                c = wildcardObservers;
            }
        }

        if(c != null) {
            // Make sure this plugin is not observing itself!
            c.remove(plugin);
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
          
          attemptLoadPlugins(gcl);

          if(!delayedLoadPlugins.isEmpty()) {
              loadDelayedPlugins();
          }
          if(!delayedEvictions.isEmpty()) {
              processDelayedEvictions();
          }

          initializePlugins();
          initialised = true;
      }
  }


  private void attemptLoadPlugins(GroovyClassLoader gcl) {
	  // retrieve load core plugins first
	  List grailsCorePlugins = findCorePlugins();
	  List grailsUserPlugins = findUserPlugins(gcl);
	  
	  List allPlugins = new ArrayList(grailsCorePlugins);
	  allPlugins.addAll(grailsUserPlugins);
	
	  //filtering applies to user as well as core plugins
	  List filteredPlugins = getPluginFilter().filterPluginList(allPlugins);
	  
	  //make sure core plugins are loaded first
	  List orderedCorePlugins = new ArrayList();
	  List orderedUserPlugins = new ArrayList();
	  
	  for (Iterator iter = filteredPlugins.iterator(); iter.hasNext();) {
		  GrailsPlugin plugin = (GrailsPlugin) iter.next();
		  
		  if (grailsCorePlugins.contains(plugin))
		  {
			  orderedCorePlugins.add(plugin);
		  }
		  else
		  {
			  orderedUserPlugins.add(plugin);
		  }
	  }
	  
	  List orderedPlugins = new ArrayList();
	  orderedPlugins.addAll(orderedCorePlugins);
	  orderedPlugins.addAll(orderedUserPlugins);

	  for (Iterator iter = orderedPlugins.iterator(); iter.hasNext();) {
		  GrailsPlugin plugin = (GrailsPlugin) iter.next();
		  attemptPluginLoad(plugin);
	  }
  }
  
  
  private List findCorePlugins() {
	  CorePluginFinder finder = new CorePluginFinder(application);
	  
	  Set classes = finder.getPluginClasses();
	  
	  Iterator classesIterator = classes.iterator();
	  List grailsCorePlugins = new ArrayList();
	  
	  while (classesIterator.hasNext())
	  {
		  Class pluginClass = (Class) classesIterator.next();
		  
	      if(pluginClass != null && !Modifier.isAbstract(pluginClass.getModifiers()) && pluginClass != DefaultGrailsPlugin.class) {
	          GrailsPlugin plugin = new DefaultGrailsPlugin(pluginClass, application);
	          plugin.setApplicationContext(applicationContext);
	          grailsCorePlugins.add(plugin);
	      }
  	  }
	  return grailsCorePlugins;
  }


  private List findUserPlugins(GroovyClassLoader gcl) {
	List grailsUserPlugins = new ArrayList();
	  
	  LOG.info("Attempting to load ["+pluginResources.length+"] user defined plugins");
      for (int i = 0; i < pluginResources.length; i++) {
          Resource r = pluginResources[i];

          Class pluginClass = loadPluginClass(gcl, r);

          if(isGrailsPlugin(pluginClass)) {
              GrailsPlugin plugin = new DefaultGrailsPlugin(pluginClass, r, application);
              //attemptPluginLoad(plugin);
              grailsUserPlugins.add(plugin);
          }
          else {
              LOG.warn("Class ["+pluginClass+"] not loaded as plug-in. Grails plug-ins must end with the convention 'GrailsPlugin'!");
          }
      }
      for (int i = 0; i < pluginClasses.length; i++) {
          Class pluginClass = pluginClasses[i];
          if(isGrailsPlugin(pluginClass)) {
              GrailsPlugin plugin = new DefaultGrailsPlugin(pluginClass, application);
              //attemptPluginLoad(plugin);
              grailsUserPlugins.add(plugin);
          }
          else {
              LOG.warn("Class ["+pluginClass+"] not loaded as plug-in. Grails plug-ins must end with the convention 'GrailsPlugin'!");
          }
          
      }
	return grailsUserPlugins;
  }

    private boolean isGrailsPlugin(Class pluginClass) {
        return pluginClass != null && pluginClass.getName().endsWith(GRAILS_PLUGIN_SUFFIX);
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
                  if(isDependentOn(plugin, remainingPlugin)) {
                      foundInDelayed = true;
                      break;
                  }
              }
              if(foundInDelayed)
                  delayedLoadPlugins.add(plugin);
              else {
                  failedPlugins.put(plugin.getName(),plugin);
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
              if(other.getName().equals(name)) {
                  return hasDelayedDependencies(other) || areDependenciesResolved(other);

              }
          }
      }
      return false;
  }

    private boolean hasDelayedDependencies(GrailsPlugin other) {
        String[] dependencyNames = other.getDependencyNames();
        for (int i = 0; i < dependencyNames.length; i++) {
            String dependencyName = dependencyNames[i];
            for (Iterator j = delayedLoadPlugins.iterator(); j.hasNext();) {
                GrailsPlugin grailsPlugin = (GrailsPlugin) j.next();
                if(grailsPlugin.getName().equals(dependencyName)) return true;
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
  private boolean isDependentOn(GrailsPlugin plugin, GrailsPlugin dependancy) {
      String[] dependencies = plugin.getDependencyNames();
      for (int i = 0; i < dependencies.length; i++) {
          String name = dependencies[i];
          String requiredVersion = plugin.getDependentVersion(name);

          if(name.equals(dependancy.getName()) &&
                GrailsPluginUtils.isValidVersion(dependancy.getVersion(), requiredVersion))
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
          if(LOG.isInfoEnabled()) {
              LOG.info("Grails plug-in ["+plugin.getName()+"] with version ["+plugin.getVersion()+"] loaded successfully");
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



  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      this.applicationContext = applicationContext;
      for (Iterator i = pluginList.iterator(); i.hasNext();) {
          GrailsPlugin plugin = (GrailsPlugin) i.next();
          plugin.setApplicationContext(applicationContext);
      }
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

        WebRuntimeSpringConfiguration springConfig = new WebRuntimeSpringConfiguration(this.parentCtx);
        springConfig.setServletContext(getServletContext());

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
          slurper.setEntityResolver(new EntityResolver() {
              public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                  if (systemId != null && systemId.equals("http://java.sun.com/dtd/web-app_2_3.dtd")) {
                      return new InputSource(new StringReader(getWeb23DTD()));
                  }
                  return null;
              }
          });
          
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
      // remove common meta classes just to be sure
      MetaClassRegistry registry = GroovySystem.getMetaClassRegistry();
      for (int i = 0; i < COMMON_CLASSES.length; i++) {
          Class commonClass = COMMON_CLASSES[i];
          registry.removeMetaClass(commonClass);
      }
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

 void setPluginFilter(PluginFilter pluginFilter)
  {
	  this.pluginFilter = pluginFilter;
  }

  private PluginFilter getPluginFilter() {
	 if (pluginFilter == null)
	 {
		pluginFilter = new IdentityPluginFilter(); 
	 }
	 return pluginFilter;
  }
  
  List getPluginList()
  {
	  return Collections.unmodifiableList(pluginList);
  }

  public static String getWeb23DTD(){
      return "<!--\n" +
              "DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.\n" +
              "\n" +
              "Copyright 2000-2007 Sun Microsystems, Inc. All rights reserved.\n" +
              "\n" +
              "The contents of this file are subject to the terms of either the GNU\n" +
              "General Public License Version 2 only (\"GPL\") or the Common Development\n" +
              "and Distribution License(\"CDDL\") (collectively, the \"License\").  You\n" +
              "may not use this file except in compliance with the License. You can obtain\n" +
              "a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html\n" +
              "or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific\n" +
              "language governing permissions and limitations under the License.\n" +
              "\n" +
              "When distributing the software, include this License Header Notice in each\n" +
              "file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.\n" +
              "Sun designates this particular file as subject to the \"Classpath\" exception\n" +
              "as provided by Sun in the GPL Version 2 section of the License file that\n" +
              "accompanied this code.  If applicable, add the following below the License\n" +
              "Header, with the fields enclosed by brackets [] replaced by your own\n" +
              "identifying information: \"Portions Copyrighted [year]\n" +
              "[name of copyright owner]\"\n" +
              "\n" +
              "Contributor(s):\n" +
              "\n" +
              "If you wish your version of this file to be governed by only the CDDL or\n" +
              "only the GPL Version 2, indicate your decision by adding \"[Contributor]\n" +
              "elects to include this software in this distribution under the [CDDL or GPL\n" +
              "Version 2] license.\"  If you don't indicate a single choice of license, a\n" +
              "recipient has the option to distribute your version of this file under\n" +
              "either the CDDL, the GPL Version 2 or to extend the choice of license to\n" +
              "its licensees as provided above.  However, if you add GPL Version 2 code\n" +
              "and therefore, elected the GPL Version 2 license, then the option applies\n" +
              "only if the new code is made subject to such option by the copyright\n" +
              "holder.\n" +
              "-->\n" +
              "\n" +
              "<!--\n" +
              "This is the XML DTD for the Servlet 2.3 deployment descriptor.\n" +
              "All Servlet 2.3 deployment descriptors must include a DOCTYPE\n" +
              "of the following form:\n" +
              "\n" +
              "  <!DOCTYPE web-app PUBLIC\n" +
              "\t\"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\"\n" +
              "\t\"http://java.sun.com/dtd/web-app_2_3.dtd\">\n" +
              "\n" +
              "-->\n" +
              "\n" +
              "<!--\n" +
              "The following conventions apply to all J2EE deployment descriptor\n" +
              "elements unless indicated otherwise.\n" +
              "\n" +
              "- In elements that contain PCDATA, leading and trailing whitespace\n" +
              "  in the data may be ignored.\n" +
              "\n" +
              "- In elements whose value is an \"enumerated type\", the value is\n" +
              "  case sensitive.\n" +
              "\n" +
              "- In elements that specify a pathname to a file within the same\n" +
              "  JAR file, relative filenames (i.e., those not starting with \"/\")\n" +
              "  are considered relative to the root of the JAR file's namespace.\n" +
              "  Absolute filenames (i.e., those starting with \"/\") also specify\n" +
              "  names in the root of the JAR file's namespace.  In general, relative\n" +
              "  names are preferred.  The exception is .war files where absolute\n" +
              "  names are preferred for consistency with the servlet API.\n" +
              "-->\n" +
              "\n" +
              "\n" +
              "<!--\n" +
              "The web-app element is the root of the deployment descriptor for\n" +
              "a web application.\n" +
              "-->\n" +
              "<!ELEMENT web-app (icon?, display-name?, description?, distributable?,\n" +
              "context-param*, filter*, filter-mapping*, listener*, servlet*,\n" +
              "servlet-mapping*, session-config?, mime-mapping*, welcome-file-list?,\n" +
              "error-page*, taglib*, resource-env-ref*, resource-ref*, security-constraint*,\n" +
              "login-config?, security-role*, env-entry*, ejb-ref*,  ejb-local-ref*)>\n" +
              "\n" +
              "<!--\n" +
              "The auth-constraint element indicates the user roles that should\n" +
              "be permitted access to this resource collection. The role-name\n" +
              "used here must either correspond to the role-name of one of the\n" +
              "security-role elements defined for this web application, or be\n" +
              "the specially reserved role-name \"*\" that is a compact syntax for\n" +
              "indicating all roles in the web application. If both \"*\" and\n" +
              "rolenames appear, the container interprets this as all roles.\n" +
              "If no roles are defined, no user is allowed access to the portion of\n" +
              "the web application described by the containing security-constraint.\n" +
              "The container matches role names case sensitively when determining\n" +
              "access.\n" +
              "\n" +
              "\n" +
              "Used in: security-constraint\n" +
              "-->\n" +
              "<!ELEMENT auth-constraint (description?, role-name*)>\n" +
              "\n" +
              "<!--\n" +
              "The auth-method element is used to configure the authentication\n" +
              "mechanism for the web application. As a prerequisite to gaining access to any web resources which are protected by an authorization\n" +
              "constraint, a user must have authenticated using the configured\n" +
              "mechanism. Legal values for this element are \"BASIC\", \"DIGEST\",\n" +
              "\"FORM\", or \"CLIENT-CERT\".\n" +
              "\n" +
              "Used in: login-config\n" +
              "-->\n" +
              "<!ELEMENT auth-method (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The context-param element contains the declaration of a web\n" +
              "application's servlet context initialization parameters.\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT context-param (param-name, param-value, description?)>\n" +
              "\n" +
              "<!--\n" +
              "The description element is used to provide text describing the parent\n" +
              "element.  The description element should include any information that\n" +
              "the web application war file producer wants to provide to the consumer of\n" +
              "the web application war file (i.e., to the Deployer). Typically, the tools\n" +
              "used by the web application war file consumer will display the description\n" +
              "when processing the parent element that contains the description.\n" +
              "\n" +
              "Used in: auth-constraint, context-param, ejb-local-ref, ejb-ref,\n" +
              "env-entry, filter, init-param, resource-env-ref, resource-ref, run-as,\n" +
              "security-role, security-role-ref, servlet, user-data-constraint,\n" +
              "web-app, web-resource-collection\n" +
              "-->\n" +
              "<!ELEMENT description (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The display-name element contains a short name that is intended to be\n" +
              "displayed by tools.  The display name need not be unique.\n" +
              "\n" +
              "Used in: filter, security-constraint, servlet, web-app\n" +
              "\n" +
              "Example:\n" +
              "\n" +
              "<display-name>Employee Self Service</display-name>\n" +
              "-->\n" +
              "<!ELEMENT display-name (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The distributable element, by its presence in a web application\n" +
              "deployment descriptor, indicates that this web application is\n" +
              "programmed appropriately to be deployed into a distributed servlet\n" +
              "container\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT distributable EMPTY>\n" +
              "\n" +
              "<!--\n" +
              "The ejb-link element is used in the ejb-ref or ejb-local-ref\n" +
              "elements to specify that an EJB reference is linked to an\n" +
              "enterprise bean.\n" +
              "\n" +
              "The name in the ejb-link element is composed of a\n" +
              "path name specifying the ejb-jar containing the referenced enterprise\n" +
              "bean with the ejb-name of the target bean appended and separated from\n" +
              "the path name by \"#\".  The path name is relative to the war file\n" +
              "containing the web application that is referencing the enterprise bean.\n" +
              "This allows multiple enterprise beans with the same ejb-name to be\n" +
              "uniquely identified.\n" +
              "\n" +
              "Used in: ejb-local-ref, ejb-ref\n" +
              "\n" +
              "Examples:\n" +
              "\n" +
              "\t<ejb-link>EmployeeRecord</ejb-link>\n" +
              "\n" +
              "\t<ejb-link>../products/product.jar#ProductEJB</ejb-link>\n" +
              "\n" +
              "-->\n" +
              "<!ELEMENT ejb-link (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The ejb-local-ref element is used for the declaration of a reference to\n" +
              "an enterprise bean's local home. The declaration consists of:\n" +
              "\n" +
              "\t- an optional description\n" +
              "\t- the EJB reference name used in the code of the web application\n" +
              "\t  that's referencing the enterprise bean\n" +
              "\t- the expected type of the referenced enterprise bean\n" +
              "\t- the expected local home and local interfaces of the referenced\n" +
              "\t  enterprise bean\n" +
              "\t- optional ejb-link information, used to specify the referenced\n" +
              "\t  enterprise bean\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT ejb-local-ref (description?, ejb-ref-name, ejb-ref-type,\n" +
              "\t\tlocal-home, local, ejb-link?)>\n" +
              "\n" +
              "<!--\n" +
              "The ejb-ref element is used for the declaration of a reference to\n" +
              "an enterprise bean's home. The declaration consists of:\n" +
              "\n" +
              "\t- an optional description\n" +
              "\t- the EJB reference name used in the code of\n" +
              "\t  the web application that's referencing the enterprise bean\n" +
              "\t- the expected type of the referenced enterprise bean\n" +
              "\t- the expected home and remote interfaces of the referenced\n" +
              "\t  enterprise bean\n" +
              "\t- optional ejb-link information, used to specify the referenced\n" +
              "\t  enterprise bean\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT ejb-ref (description?, ejb-ref-name, ejb-ref-type,\n" +
              "\t\thome, remote, ejb-link?)>\n" +
              "\n" +
              "<!--\n" +
              "The ejb-ref-name element contains the name of an EJB reference. The\n" +
              "EJB reference is an entry in the web application's environment and is\n" +
              "relative to the java:comp/env context.  The name must be unique\n" +
              "within the web application.\n" +
              "\n" +
              "It is recommended that name is prefixed with \"ejb/\".\n" +
              "\n" +
              "Used in: ejb-local-ref, ejb-ref\n" +
              "\n" +
              "Example:\n" +
              "\n" +
              "<ejb-ref-name>ejb/Payroll</ejb-ref-name>\n" +
              "-->\n" +
              "<!ELEMENT ejb-ref-name (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The ejb-ref-type element contains the expected type of the\n" +
              "referenced enterprise bean.\n" +
              "\n" +
              "The ejb-ref-type element must be one of the following:\n" +
              "\n" +
              "\t<ejb-ref-type>Entity</ejb-ref-type>\n" +
              "\t<ejb-ref-type>Session</ejb-ref-type>\n" +
              "\n" +
              "Used in: ejb-local-ref, ejb-ref\n" +
              "-->\n" +
              "<!ELEMENT ejb-ref-type (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The env-entry element contains the declaration of a web application's\n" +
              "environment entry. The declaration consists of an optional\n" +
              "description, the name of the environment entry, and an optional\n" +
              "value.  If a value is not specified, one must be supplied\n" +
              "during deployment.\n" +
              "-->\n" +
              "<!ELEMENT env-entry (description?, env-entry-name, env-entry-value?,\n" +
              "env-entry-type)>\n" +
              "\n" +
              "<!--\n" +
              "The env-entry-name element contains the name of a web applications's\n" +
              "environment entry.  The name is a JNDI name relative to the\n" +
              "java:comp/env context.  The name must be unique within a web application.\n" +
              "\n" +
              "Example:\n" +
              "\n" +
              "<env-entry-name>minAmount</env-entry-name>\n" +
              "\n" +
              "Used in: env-entry\n" +
              "-->\n" +
              "<!ELEMENT env-entry-name (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The env-entry-type element contains the fully-qualified Java type of\n" +
              "the environment entry value that is expected by the web application's\n" +
              "code.\n" +
              "\n" +
              "The following are the legal values of env-entry-type:\n" +
              "\n" +
              "\tjava.lang.Boolean\n" +
              "\tjava.lang.Byte\n" +
              "\tjava.lang.Character\n" +
              "\tjava.lang.String\n" +
              "\tjava.lang.Short\n" +
              "\tjava.lang.Integer\n" +
              "\tjava.lang.Long\n" +
              "\tjava.lang.Float\n" +
              "\tjava.lang.Double\n" +
              "\n" +
              "Used in: env-entry\n" +
              "-->\n" +
              "<!ELEMENT env-entry-type (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The env-entry-value element contains the value of a web application's\n" +
              "environment entry. The value must be a String that is valid for the\n" +
              "constructor of the specified type that takes a single String\n" +
              "parameter, or for java.lang.Character, a single character.\n" +
              "\n" +
              "Example:\n" +
              "\n" +
              "<env-entry-value>100.00</env-entry-value>\n" +
              "\n" +
              "Used in: env-entry\n" +
              "-->\n" +
              "<!ELEMENT env-entry-value (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The error-code contains an HTTP error code, ex: 404\n" +
              "\n" +
              "Used in: error-page\n" +
              "-->\n" +
              "<!ELEMENT error-code (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The error-page element contains a mapping between an error code\n" +
              "or exception type to the path of a resource in the web application\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT error-page ((error-code | exception-type), location)>\n" +
              "\n" +
              "<!--\n" +
              "The exception type contains a fully qualified class name of a\n" +
              "Java exception type.\n" +
              "\n" +
              "Used in: error-page\n" +
              "-->\n" +
              "<!ELEMENT exception-type (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The extension element contains a string describing an\n" +
              "extension. example: \"txt\"\n" +
              "\n" +
              "Used in: mime-mapping\n" +
              "-->\n" +
              "<!ELEMENT extension (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "Declares a filter in the web application. The filter is mapped to\n" +
              "either a servlet or a URL pattern in the filter-mapping element, using\n" +
              "the filter-name value to reference. Filters can access the\n" +
              "initialization parameters declared in the deployment descriptor at\n" +
              "runtime via the FilterConfig interface.\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT filter (icon?, filter-name, display-name?, description?,\n" +
              "filter-class, init-param*)>\n" +
              "\n" +
              "<!--\n" +
              "The fully qualified classname of the filter.\n" +
              "\n" +
              "Used in: filter\n" +
              "-->\n" +
              "<!ELEMENT filter-class (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "Declaration of the filter mappings in this web application. The\n" +
              "container uses the filter-mapping declarations to decide which filters\n" +
              "to apply to a request, and in what order. The container matches the\n" +
              "request URI to a Servlet in the normal way. To determine which filters\n" +
              "to apply it matches filter-mapping declarations either on servlet-name,\n" +
              "or on url-pattern for each filter-mapping element, depending on which\n" +
              "style is used. The order in which filters are invoked is the order in\n" +
              "which filter-mapping declarations that match a request URI for a\n" +
              "servlet appear in the list of filter-mapping elements.The filter-name\n" +
              "value must be the value of the <filter-name> sub-elements of one of the\n" +
              "<filter> declarations in the deployment descriptor.\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT filter-mapping (filter-name, (url-pattern | servlet-name))>\n" +
              "\n" +
              "<!--\n" +
              "The logical name of the filter. This name is used to map the filter.\n" +
              "Each filter name is unique within the web application.\n" +
              "\n" +
              "Used in: filter, filter-mapping\n" +
              "-->\n" +
              "<!ELEMENT filter-name (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The form-error-page element defines the location in the web app\n" +
              "where the error page that is displayed when login is not successful\n" +
              "can be found. The path begins with a leading / and is interpreted\n" +
              "relative to the root of the WAR.\n" +
              "\n" +
              "Used in: form-login-config\n" +
              "-->\n" +
              "<!ELEMENT form-error-page (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The form-login-config element specifies the login and error pages\n" +
              "that should be used in form based login. If form based authentication\n" +
              "is not used, these elements are ignored.\n" +
              "\n" +
              "Used in: login-config\n" +
              "-->\n" +
              "<!ELEMENT form-login-config (form-login-page, form-error-page)>\n" +
              "\n" +
              "<!--\n" +
              "The form-login-page element defines the location in the web app\n" +
              "where the page that can be used for login can be found. The path\n" +
              "begins with a leading / and is interpreted relative to the root of the WAR.\n" +
              "\n" +
              "Used in: form-login-config\n" +
              "-->\n" +
              "<!ELEMENT form-login-page (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The home element contains the fully-qualified name of the enterprise\n" +
              "bean's home interface.\n" +
              "\n" +
              "Used in: ejb-ref\n" +
              "\n" +
              "Example:\n" +
              "\n" +
              "<home>com.aardvark.payroll.PayrollHome</home>\n" +
              "-->\n" +
              "<!ELEMENT home (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The http-method contains an HTTP method (GET | POST |...).\n" +
              "\n" +
              "Used in: web-resource-collection\n" +
              "-->\n" +
              "<!ELEMENT http-method (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The icon element contains small-icon and large-icon elements that\n" +
              "specify the file names for small and a large GIF or JPEG icon images\n" +
              "used to represent the parent element in a GUI tool.\n" +
              "\n" +
              "Used in: filter, servlet, web-app\n" +
              "-->\n" +
              "<!ELEMENT icon (small-icon?, large-icon?)>\n" +
              "\n" +
              "<!--\n" +
              "The init-param element contains a name/value pair as an\n" +
              "initialization param of the servlet\n" +
              "\n" +
              "Used in: filter, servlet\n" +
              "-->\n" +
              "<!ELEMENT init-param (param-name, param-value, description?)>\n" +
              "\n" +
              "<!--\n" +
              "The jsp-file element contains the full path to a JSP file within\n" +
              "the web application beginning with a `/'.\n" +
              "\n" +
              "Used in: servlet\n" +
              "-->\n" +
              "<!ELEMENT jsp-file (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The large-icon element contains the name of a file\n" +
              "containing a large (32 x 32) icon image. The file\n" +
              "name is a relative path within the web application's\n" +
              "war file.\n" +
              "\n" +
              "The image may be either in the JPEG or GIF format.\n" +
              "The icon can be used by tools.\n" +
              "\n" +
              "Used in: icon\n" +
              "\n" +
              "Example:\n" +
              "\n" +
              "<large-icon>employee-service-icon32x32.jpg</large-icon>\n" +
              "-->\n" +
              "<!ELEMENT large-icon (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The listener element indicates the deployment properties for a web\n" +
              "application listener bean.\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT listener (listener-class)>\n" +
              "\n" +
              "<!--\n" +
              "The listener-class element declares a class in the application must be\n" +
              "registered as a web application listener bean. The value is the fully qualified classname of the listener class.\n" +
              "\n" +
              "\n" +
              "Used in: listener\n" +
              "-->\n" +
              "<!ELEMENT listener-class (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The load-on-startup element indicates that this servlet should be\n" +
              "loaded (instantiated and have its init() called) on the startup\n" +
              "of the web application. The optional contents of\n" +
              "these element must be an integer indicating the order in which\n" +
              "the servlet should be loaded. If the value is a negative integer,\n" +
              "or the element is not present, the container is free to load the\n" +
              "servlet whenever it chooses. If the value is a positive integer\n" +
              "or 0, the container must load and initialize the servlet as the\n" +
              "application is deployed. The container must guarantee that\n" +
              "servlets marked with lower integers are loaded before servlets\n" +
              "marked with higher integers. The container may choose the order\n" +
              "of loading of servlets with the same load-on-start-up value.\n" +
              "\n" +
              "Used in: servlet\n" +
              "-->\n" +
              "<!ELEMENT load-on-startup (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "\n" +
              "The local element contains the fully-qualified name of the\n" +
              "enterprise bean's local interface.\n" +
              "\n" +
              "Used in: ejb-local-ref\n" +
              "\n" +
              "-->\n" +
              "<!ELEMENT local (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "\n" +
              "The local-home element contains the fully-qualified name of the\n" +
              "enterprise bean's local home interface.\n" +
              "\n" +
              "Used in: ejb-local-ref\n" +
              "-->\n" +
              "<!ELEMENT local-home (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The location element contains the location of the resource in the web\n" +
              "application relative to the root of the web application. The value of\n" +
              "the location must have a leading `/'.\n" +
              "\n" +
              "Used in: error-page\n" +
              "-->\n" +
              "<!ELEMENT location (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The login-config element is used to configure the authentication\n" +
              "method that should be used, the realm name that should be used for\n" +
              "this application, and the attributes that are needed by the form login\n" +
              "mechanism.\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT login-config (auth-method?, realm-name?, form-login-config?)>\n" +
              "\n" +
              "<!--\n" +
              "The mime-mapping element defines a mapping between an extension\n" +
              "and a mime type.\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT mime-mapping (extension, mime-type)>\n" +
              "\n" +
              "<!--\n" +
              "The mime-type element contains a defined mime type. example:\n" +
              "\"text/plain\"\n" +
              "\n" +
              "Used in: mime-mapping\n" +
              "-->\n" +
              "<!ELEMENT mime-type (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The param-name element contains the name of a parameter. Each parameter\n" +
              "name must be unique in the web application.\n" +
              "\n" +
              "\n" +
              "Used in: context-param, init-param\n" +
              "-->\n" +
              "<!ELEMENT param-name (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The param-value element contains the value of a parameter.\n" +
              "\n" +
              "Used in: context-param, init-param\n" +
              "-->\n" +
              "<!ELEMENT param-value (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The realm name element specifies the realm name to use in HTTP\n" +
              "Basic authorization.\n" +
              "\n" +
              "Used in: login-config\n" +
              "-->\n" +
              "<!ELEMENT realm-name (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The remote element contains the fully-qualified name of the enterprise\n" +
              "bean's remote interface.\n" +
              "\n" +
              "Used in: ejb-ref\n" +
              "\n" +
              "Example:\n" +
              "\n" +
              "<remote>com.wombat.empl.EmployeeService</remote>\n" +
              "-->\n" +
              "<!ELEMENT remote (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The res-auth element specifies whether the web application code signs\n" +
              "on programmatically to the resource manager, or whether the Container\n" +
              "will sign on to the resource manager on behalf of the web application. In the\n" +
              "latter case, the Container uses information that is supplied by the\n" +
              "Deployer.\n" +
              "\n" +
              "The value of this element must be one of the two following:\n" +
              "\n" +
              "\t<res-auth>Application</res-auth>\n" +
              "\t<res-auth>Container</res-auth>\n" +
              "\n" +
              "Used in: resource-ref\n" +
              "-->\n" +
              "<!ELEMENT res-auth (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The res-ref-name element specifies the name of a resource manager\n" +
              "connection factory reference.  The name is a JNDI name relative to the\n" +
              "java:comp/env context.  The name must be unique within a web application.\n" +
              "\n" +
              "Used in: resource-ref\n" +
              "-->\n" +
              "<!ELEMENT res-ref-name (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The res-sharing-scope element specifies whether connections obtained\n" +
              "through the given resource manager connection factory reference can be\n" +
              "shared. The value of this element, if specified, must be one of the\n" +
              "two following:\n" +
              "\n" +
              "\t<res-sharing-scope>Shareable</res-sharing-scope>\n" +
              "\t<res-sharing-scope>Unshareable</res-sharing-scope>\n" +
              "\n" +
              "The default value is Shareable.\n" +
              "\n" +
              "Used in: resource-ref\n" +
              "-->\n" +
              "<!ELEMENT res-sharing-scope (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The res-type element specifies the type of the data source. The type\n" +
              "is specified by the fully qualified Java language class or interface\n" +
              "expected to be implemented by the data source.\n" +
              "\n" +
              "Used in: resource-ref\n" +
              "-->\n" +
              "<!ELEMENT res-type (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The resource-env-ref element contains a declaration of a web application's\n" +
              "reference to an administered object associated with a resource\n" +
              "in the web application's environment.  It consists of an optional\n" +
              "description, the resource environment reference name, and an\n" +
              "indication of the resource environment reference type expected by\n" +
              "the web application code.\n" +
              "\n" +
              "Used in: web-app\n" +
              "\n" +
              "Example:\n" +
              "\n" +
              "<resource-env-ref>\n" +
              "    <resource-env-ref-name>jms/StockQueue</resource-env-ref-name>\n" +
              "    <resource-env-ref-type>javax.jms.Queue</resource-env-ref-type>\n" +
              "</resource-env-ref>\n" +
              "-->\n" +
              "<!ELEMENT resource-env-ref (description?, resource-env-ref-name,\n" +
              "\t\tresource-env-ref-type)>\n" +
              "\n" +
              "<!--\n" +
              "The resource-env-ref-name element specifies the name of a resource\n" +
              "environment reference; its value is the environment entry name used in\n" +
              "the web application code.  The name is a JNDI name relative to the\n" +
              "java:comp/env context and must be unique within a web application.\n" +
              "\n" +
              "Used in: resource-env-ref\n" +
              "-->\n" +
              "<!ELEMENT resource-env-ref-name (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The resource-env-ref-type element specifies the type of a resource\n" +
              "environment reference.  It is the fully qualified name of a Java\n" +
              "language class or interface.\n" +
              "\n" +
              "Used in: resource-env-ref\n" +
              "-->\n" +
              "<!ELEMENT resource-env-ref-type (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The resource-ref element contains a declaration of a web application's\n" +
              "reference to an external resource. It consists of an optional\n" +
              "description, the resource manager connection factory reference name,\n" +
              "the indication of the resource manager connection factory type\n" +
              "expected by the web application code, the type of authentication\n" +
              "(Application or Container), and an optional specification of the\n" +
              "shareability of connections obtained from the resource (Shareable or\n" +
              "Unshareable).\n" +
              "\n" +
              "Used in: web-app\n" +
              "\n" +
              "Example:\n" +
              "\n" +
              "    <resource-ref>\n" +
              "\t<res-ref-name>jdbc/EmployeeAppDB</res-ref-name>\n" +
              "\t<res-type>javax.sql.DataSource</res-type>\n" +
              "\t<res-auth>Container</res-auth>\n" +
              "\t<res-sharing-scope>Shareable</res-sharing-scope>\n" +
              "    </resource-ref>\n" +
              "-->\n" +
              "<!ELEMENT resource-ref (description?, res-ref-name, res-type, res-auth,\n" +
              "\t\tres-sharing-scope?)>\n" +
              "\n" +
              "<!--\n" +
              "The role-link element is a reference to a defined security role. The\n" +
              "role-link element must contain the name of one of the security roles\n" +
              "defined in the security-role elements.\n" +
              "\n" +
              "Used in: security-role-ref\n" +
              "-->\n" +
              "<!ELEMENT role-link (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The role-name element contains the name of a security role.\n" +
              "\n" +
              "The name must conform to the lexical rules for an NMTOKEN.\n" +
              "\n" +
              "Used in: auth-constraint, run-as, security-role, security-role-ref\n" +
              "-->\n" +
              "<!ELEMENT role-name (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The run-as element specifies the run-as identity to be used for the\n" +
              "execution of the web application. It contains an optional description, and\n" +
              "the name of a security role.\n" +
              "\n" +
              "Used in: servlet\n" +
              "-->\n" +
              "<!ELEMENT run-as (description?, role-name)>\n" +
              "\n" +
              "<!--\n" +
              "The security-constraint element is used to associate security\n" +
              "constraints with one or more web resource collections\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT security-constraint (display-name?, web-resource-collection+,\n" +
              "auth-constraint?, user-data-constraint?)>\n" +
              "\n" +
              "<!--\n" +
              "The security-role element contains the definition of a security\n" +
              "role. The definition consists of an optional description of the\n" +
              "security role, and the security role name.\n" +
              "\n" +
              "Used in: web-app\n" +
              "\n" +
              "Example:\n" +
              "\n" +
              "    <security-role>\n" +
              "\t<description>\n" +
              "\t    This role includes all employees who are authorized\n" +
              "\t    to access the employee service application.\n" +
              "\t</description>\n" +
              "\t<role-name>employee</role-name>\n" +
              "    </security-role>\n" +
              "-->\n" +
              "<!ELEMENT security-role (description?, role-name)>\n" +
              "\n" +
              "<!--\n" +
              "The security-role-ref element contains the declaration of a security\n" +
              "role reference in the web application's code. The declaration consists\n" +
              "of an optional description, the security role name used in the code,\n" +
              "and an optional link to a security role. If the security role is not\n" +
              "specified, the Deployer must choose an appropriate security role.\n" +
              "\n" +
              "The value of the role-name element must be the String used as the\n" +
              "parameter to the EJBContext.isCallerInRole(String roleName) method\n" +
              "or the HttpServletRequest.isUserInRole(String role) method.\n" +
              "\n" +
              "Used in: servlet\n" +
              "\n" +
              "-->\n" +
              "<!ELEMENT security-role-ref (description?, role-name, role-link?)>\n" +
              "\n" +
              "<!--\n" +
              "The servlet element contains the declarative data of a\n" +
              "servlet. If a jsp-file is specified and the load-on-startup element is\n" +
              "present, then the JSP should be precompiled and loaded.\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT servlet (icon?, servlet-name, display-name?, description?,\n" +
              "(servlet-class|jsp-file), init-param*, load-on-startup?, run-as?, security-role-ref*)>\n" +
              "\n" +
              "<!--\n" +
              "The servlet-class element contains the fully qualified class name\n" +
              "of the servlet.\n" +
              "\n" +
              "Used in: servlet\n" +
              "-->\n" +
              "<!ELEMENT servlet-class (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The servlet-mapping element defines a mapping between a servlet\n" +
              "and a url pattern\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT servlet-mapping (servlet-name, url-pattern)>\n" +
              "\n" +
              "<!--\n" +
              "The servlet-name element contains the canonical name of the\n" +
              "servlet. Each servlet name is unique within the web application.\n" +
              "\n" +
              "Used in: filter-mapping, servlet, servlet-mapping\n" +
              "-->\n" +
              "<!ELEMENT servlet-name (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The session-config element defines the session parameters for\n" +
              "this web application.\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT session-config (session-timeout?)>\n" +
              "\n" +
              "<!--\n" +
              "The session-timeout element defines the default session timeout\n" +
              "interval for all sessions created in this web application. The\n" +
              "specified timeout must be expressed in a whole number of minutes.\n" +
              "If the timeout is 0 or less, the container ensures the default\n" +
              "behaviour of sessions is never to time out.\n" +
              "\n" +
              "Used in: session-config\n" +
              "-->\n" +
              "<!ELEMENT session-timeout (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The small-icon element contains the name of a file\n" +
              "containing a small (16 x 16) icon image. The file\n" +
              "name is a relative path within the web application's\n" +
              "war file.\n" +
              "\n" +
              "The image may be either in the JPEG or GIF format.\n" +
              "The icon can be used by tools.\n" +
              "\n" +
              "Used in: icon\n" +
              "\n" +
              "Example:\n" +
              "\n" +
              "<small-icon>employee-service-icon16x16.jpg</small-icon>\n" +
              "-->\n" +
              "<!ELEMENT small-icon (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The taglib element is used to describe a JSP tag library.\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT taglib (taglib-uri, taglib-location)>\n" +
              "\n" +
              "<!--\n" +
              "the taglib-location element contains the location (as a resource\n" +
              "relative to the root of the web application) where to find the Tag\n" +
              "Libary Description file for the tag library.\n" +
              "\n" +
              "Used in: taglib\n" +
              "-->\n" +
              "<!ELEMENT taglib-location (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The taglib-uri element describes a URI, relative to the location\n" +
              "of the web.xml document, identifying a Tag Library used in the Web\n" +
              "Application.\n" +
              "\n" +
              "Used in: taglib\n" +
              "-->\n" +
              "<!ELEMENT taglib-uri (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The transport-guarantee element specifies that the communication\n" +
              "between client and server should be NONE, INTEGRAL, or\n" +
              "CONFIDENTIAL. NONE means that the application does not require any\n" +
              "transport guarantees. A value of INTEGRAL means that the application\n" +
              "requires that the data sent between the client and server be sent in\n" +
              "such a way that it can't be changed in transit. CONFIDENTIAL means\n" +
              "that the application requires that the data be transmitted in a\n" +
              "fashion that prevents other entities from observing the contents of\n" +
              "the transmission. In most cases, the presence of the INTEGRAL or\n" +
              "CONFIDENTIAL flag will indicate that the use of SSL is required.\n" +
              "\n" +
              "Used in: user-data-constraint\n" +
              "-->\n" +
              "<!ELEMENT transport-guarantee (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The url-pattern element contains the url pattern of the mapping. Must\n" +
              "follow the rules specified in Section 11.2 of the Servlet API\n" +
              "Specification.\n" +
              "\n" +
              "Used in: filter-mapping, servlet-mapping, web-resource-collection\n" +
              "-->\n" +
              "<!ELEMENT url-pattern (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The user-data-constraint element is used to indicate how data\n" +
              "communicated between the client and container should be protected.\n" +
              "\n" +
              "Used in: security-constraint\n" +
              "-->\n" +
              "<!ELEMENT user-data-constraint (description?, transport-guarantee)>\n" +
              "\n" +
              "<!--\n" +
              "The web-resource-collection element is used to identify a subset\n" +
              "of the resources and HTTP methods on those resources within a web\n" +
              "application to which a security constraint applies. If no HTTP methods\n" +
              "are specified, then the security constraint applies to all HTTP\n" +
              "methods.\n" +
              "\n" +
              "Used in: security-constraint\n" +
              "-->\n" +
              "<!ELEMENT web-resource-collection (web-resource-name, description?,\n" +
              "url-pattern*, http-method*)>\n" +
              "\n" +
              "<!--\n" +
              "The web-resource-name contains the name of this web resource\n" +
              "collection.\n" +
              "\n" +
              "Used in: web-resource-collection\n" +
              "-->\n" +
              "<!ELEMENT web-resource-name (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The welcome-file element contains file name to use as a default\n" +
              "welcome file, such as index.html\n" +
              "\n" +
              "Used in: welcome-file-list\n" +
              "-->\n" +
              "<!ELEMENT welcome-file (#PCDATA)>\n" +
              "\n" +
              "<!--\n" +
              "The welcome-file-list contains an ordered list of welcome files\n" +
              "elements.\n" +
              "\n" +
              "Used in: web-app\n" +
              "-->\n" +
              "<!ELEMENT welcome-file-list (welcome-file+)>\n" +
              "\n" +
              "<!--\n" +
              "The ID mechanism is to allow tools that produce additional deployment\n" +
              "information (i.e., information beyond the standard deployment\n" +
              "descriptor information) to store the non-standard information in a\n" +
              "separate file, and easily refer from these tool-specific files to the\n" +
              "information in the standard deployment descriptor.\n" +
              "\n" +
              "Tools are not allowed to add the non-standard information into the\n" +
              "standard deployment descriptor.\n" +
              "-->\n" +
              "\n" +
              "<!ATTLIST auth-constraint id ID #IMPLIED>\n" +
              "<!ATTLIST auth-method id ID #IMPLIED>\n" +
              "<!ATTLIST context-param id ID #IMPLIED>\n" +
              "<!ATTLIST description id ID #IMPLIED>\n" +
              "<!ATTLIST display-name id ID #IMPLIED>\n" +
              "<!ATTLIST distributable id ID #IMPLIED>\n" +
              "<!ATTLIST ejb-link id ID #IMPLIED>\n" +
              "<!ATTLIST ejb-local-ref id ID #IMPLIED>\n" +
              "<!ATTLIST ejb-ref id ID #IMPLIED>\n" +
              "<!ATTLIST ejb-ref-name id ID #IMPLIED>\n" +
              "<!ATTLIST ejb-ref-type id ID #IMPLIED>\n" +
              "<!ATTLIST env-entry id ID #IMPLIED>\n" +
              "<!ATTLIST env-entry-name id ID #IMPLIED>\n" +
              "<!ATTLIST env-entry-type id ID #IMPLIED>\n" +
              "<!ATTLIST env-entry-value id ID #IMPLIED>\n" +
              "<!ATTLIST error-code id ID #IMPLIED>\n" +
              "<!ATTLIST error-page id ID #IMPLIED>\n" +
              "<!ATTLIST exception-type id ID #IMPLIED>\n" +
              "<!ATTLIST extension id ID #IMPLIED>\n" +
              "<!ATTLIST filter id ID #IMPLIED>\n" +
              "<!ATTLIST filter-class id ID #IMPLIED>\n" +
              "<!ATTLIST filter-mapping id ID #IMPLIED>\n" +
              "<!ATTLIST filter-name id ID #IMPLIED>\n" +
              "<!ATTLIST form-error-page id ID #IMPLIED>\n" +
              "<!ATTLIST form-login-config id ID #IMPLIED>\n" +
              "<!ATTLIST form-login-page id ID #IMPLIED>\n" +
              "<!ATTLIST home id ID #IMPLIED>\n" +
              "<!ATTLIST http-method id ID #IMPLIED>\n" +
              "<!ATTLIST icon id ID #IMPLIED>\n" +
              "<!ATTLIST init-param id ID #IMPLIED>\n" +
              "<!ATTLIST jsp-file id ID #IMPLIED>\n" +
              "<!ATTLIST large-icon id ID #IMPLIED>\n" +
              "<!ATTLIST listener id ID #IMPLIED>\n" +
              "<!ATTLIST listener-class id ID #IMPLIED>\n" +
              "<!ATTLIST load-on-startup id ID #IMPLIED>\n" +
              "<!ATTLIST local id ID #IMPLIED>\n" +
              "<!ATTLIST local-home id ID #IMPLIED>\n" +
              "<!ATTLIST location id ID #IMPLIED>\n" +
              "<!ATTLIST login-config id ID #IMPLIED>\n" +
              "<!ATTLIST mime-mapping id ID #IMPLIED>\n" +
              "<!ATTLIST mime-type id ID #IMPLIED>\n" +
              "<!ATTLIST param-name id ID #IMPLIED>\n" +
              "<!ATTLIST param-value id ID #IMPLIED>\n" +
              "<!ATTLIST realm-name id ID #IMPLIED>\n" +
              "<!ATTLIST remote id ID #IMPLIED>\n" +
              "<!ATTLIST res-auth id ID #IMPLIED>\n" +
              "<!ATTLIST res-ref-name id ID #IMPLIED>\n" +
              "<!ATTLIST res-sharing-scope id ID #IMPLIED>\n" +
              "<!ATTLIST res-type id ID #IMPLIED>\n" +
              "<!ATTLIST resource-env-ref id ID #IMPLIED>\n" +
              "<!ATTLIST resource-env-ref-name id ID #IMPLIED>\n" +
              "<!ATTLIST resource-env-ref-type id ID #IMPLIED>\n" +
              "<!ATTLIST resource-ref id ID #IMPLIED>\n" +
              "<!ATTLIST role-link id ID #IMPLIED>\n" +
              "<!ATTLIST role-name id ID #IMPLIED>\n" +
              "<!ATTLIST run-as id ID #IMPLIED>\n" +
              "<!ATTLIST security-constraint id ID #IMPLIED>\n" +
              "<!ATTLIST security-role id ID #IMPLIED>\n" +
              "<!ATTLIST security-role-ref id ID #IMPLIED>\n" +
              "<!ATTLIST servlet id ID #IMPLIED>\n" +
              "<!ATTLIST servlet-class id ID #IMPLIED>\n" +
              "<!ATTLIST servlet-mapping id ID #IMPLIED>\n" +
              "<!ATTLIST servlet-name id ID #IMPLIED>\n" +
              "<!ATTLIST session-config id ID #IMPLIED>\n" +
              "<!ATTLIST session-timeout id ID #IMPLIED>\n" +
              "<!ATTLIST small-icon id ID #IMPLIED>\n" +
              "<!ATTLIST taglib id ID #IMPLIED>\n" +
              "<!ATTLIST taglib-location id ID #IMPLIED>\n" +
              "<!ATTLIST taglib-uri id ID #IMPLIED>\n" +
              "<!ATTLIST transport-guarantee id ID #IMPLIED>\n" +
              "<!ATTLIST url-pattern id ID #IMPLIED>\n" +
              "<!ATTLIST user-data-constraint id ID #IMPLIED>\n" +
              "<!ATTLIST web-app id ID #IMPLIED>\n" +
              "<!ATTLIST web-resource-collection id ID #IMPLIED>\n" +
              "<!ATTLIST web-resource-name id ID #IMPLIED>\n" +
              "<!ATTLIST welcome-file id ID #IMPLIED>\n" +
              "<!ATTLIST welcome-file-list id ID #IMPLIED>";
  }

}
