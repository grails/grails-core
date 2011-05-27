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

import grails.util.BuildScope;
import grails.util.Environment;
import grails.util.GrailsNameUtils;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClassRegistry;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import org.codehaus.groovy.grails.commons.ArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;

/**
 * Abstract implementation of the GrailsPluginManager interface
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public abstract class AbstractGrailsPluginManager implements GrailsPluginManager {

    private static final String BLANK = "";
    public static final String CONFIG_FILE = "Config";
    protected List<GrailsPlugin> pluginList = new ArrayList<GrailsPlugin>();
    protected GrailsApplication application;
    protected Resource[] pluginResources = new Resource[0];
    protected Map<String, GrailsPlugin> plugins = new HashMap<String, GrailsPlugin>();
    protected Map<String, GrailsPlugin> classNameToPluginMap = new HashMap<String, GrailsPlugin>();
    protected Class<?>[] pluginClasses = new Class[0];
    protected boolean initialised = false;
    protected ApplicationContext applicationContext;
    protected Map<String, GrailsPlugin> failedPlugins = new HashMap<String, GrailsPlugin>();
    protected boolean loadCorePlugins = true;

    public AbstractGrailsPluginManager(GrailsApplication application) {
        Assert.notNull(application, "Argument [application] cannot be null!");
        this.application = application;
    }

    public List<TypeFilter> getTypeFilters() {
        List<TypeFilter> list = new ArrayList<TypeFilter>();
        for (GrailsPlugin grailsPlugin : pluginList) {
            list.addAll(grailsPlugin.getTypeFilters());
        }
        return Collections.unmodifiableList(list);
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
        Assert.state(initialised, "Must call loadPlugins() before invoking configurational methods on GrailsPluginManager");
    }

    public GrailsPlugin getFailedPlugin(String name) {
        if (name.indexOf('-') > -1) {
            name = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        }
        return failedPlugins.get(name);
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
     * Base implementation that will perform runtime configuration for the specified plugin name.
     */
    public void doRuntimeConfiguration(String pluginName, RuntimeSpringConfiguration springConfig) {
        checkInitialised();
        GrailsPlugin plugin = getGrailsPlugin(pluginName);
        if (plugin == null) {
            throw new PluginException("Plugin [" + pluginName + "] not found");
        }

        if (!plugin.supportsCurrentScopeAndEnvironment()) {
            return;
        }

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
            if (current == null) {
                throw new PluginException("Cannot load Plugin. Dependency [" + current + "] not found");
            }

            String[] pluginDependencies = current.getDependencyNames();
            if (pluginDependencies.length > 0) {
                doRuntimeConfigurationForDependencies(pluginDependencies, springConfig);
            }
            current.doWithRuntimeConfiguration(springConfig);
        }
    }

    /**
     * Base implementation that will simply go through each plugin and call doWithApplicationContext on each.
     */
    public void doPostProcessing(ApplicationContext ctx) {
        checkInitialised();
        for (GrailsPlugin plugin : pluginList) {
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                plugin.doWithApplicationContext(ctx);
            }
        }
    }

    public Resource[] getPluginResources() {
        return pluginResources;
    }

    public GrailsPlugin getGrailsPlugin(String name) {
        if (name.indexOf('-') > -1) {
            name = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        }
        return plugins.get(name);
    }

    public GrailsPlugin getGrailsPluginForClassName(String name) {
        return classNameToPluginMap.get(name);
    }

    public GrailsPlugin getGrailsPlugin(String name, Object version) {
        if (name.indexOf('-') > -1) {
            name = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        }
        GrailsPlugin plugin = plugins.get(name);
        if (plugin != null && GrailsPluginUtils.isValidVersion(plugin.getVersion(), version.toString())) {
            return plugin;
        }
        return null;
    }

    public boolean hasGrailsPlugin(String name) {
        if (name.indexOf('-') > -1) {
            name = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(name);
        }
        return plugins.containsKey(name);
    }

    public void doDynamicMethods() {
        checkInitialised();
        Class<?>[] allClasses = application.getAllClasses();
        if (allClasses != null) {
            for (Class<?> c : allClasses) {
                ExpandoMetaClass emc = new ExpandoMetaClass(c, true, true);
                emc.initialize();
            }
            for (GrailsPlugin plugin : pluginList) {
                plugin.doWithDynamicMethods(applicationContext);
            }
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        for (GrailsPlugin plugin : pluginList) {
            plugin.setApplicationContext(applicationContext);
        }
    }

    public void setApplication(GrailsApplication application) {
        Assert.notNull(application, "Argument [application] cannot be null");
        this.application = application;

        for (GrailsPlugin plugin : pluginList) {
            plugin.setApplication(application);
        }
    }

    public void registerProvidedArtefacts(GrailsApplication app) {
        checkInitialised();
        for (GrailsPlugin plugin : pluginList) {
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                for (Class<?> artefact : plugin.getProvidedArtefacts()) {
                    String shortName = GrailsNameUtils.getShortName(artefact);
                    if (!isAlreadyRegistered(app, artefact, shortName)) {
                        app.addOverridableArtefact(artefact);
                    }
                }
            }
        }
    }

    private boolean isAlreadyRegistered(GrailsApplication app, Class<?> artefact, String shortName) {
        return app.getClassForName(shortName) != null || app.getClassForName(artefact.getName()) != null;
    }

    public void doArtefactConfiguration() {
        checkInitialised();
        for (GrailsPlugin plugin : pluginList) {
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                plugin.doArtefactConfiguration();
            }
        }
    }

    public void shutdown() {
        checkInitialised();
        for (GrailsPlugin plugin : pluginList) {
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
        loadCorePlugins = shouldLoadCorePlugins;
    }

    public void informOfClassChange(Class<?> aClass) {
        if (aClass ==null || application == null) {
            return;
        }

        ArtefactHandler handler = application.getArtefactType(aClass);
        if (handler == null) {
            return;
        }

        String pluginName = handler.getPluginName();
        if (pluginName == null) {
            return;
        }

        GrailsPlugin plugin = getGrailsPlugin(pluginName);
        if (plugin != null) {
            plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CHANGE, aClass);
        }
    }

    public String getPluginPath(String name) {
        GrailsPlugin plugin = getGrailsPlugin(name);
        if (plugin != null && !plugin.isBasePlugin()) {
            return plugin.getPluginPath();
        }
        return BLANK;
    }

    public String getPluginPathForInstance(Object instance) {
        if (instance != null) {
            return getPluginPathForClass(instance.getClass());
        }
        return null;
    }

    public GrailsPlugin getPluginForInstance(Object instance) {
        if (instance != null) {
            return getPluginForClass(instance.getClass());
        }
        return null;
    }

    public GrailsPlugin getPluginForClass(Class<?> theClass) {
        if (theClass != null) {
            org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin ann =
                theClass.getAnnotation(org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin.class);
            if (ann != null) {
                return getGrailsPlugin(ann.name());
            }
        }
        return null;
    }

    public void informPluginsOfConfigChange() {
        for (GrailsPlugin plugin : pluginList) {
            plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CONFIG_CHANGE, application.getConfig());
        }
    }

    public void informOfFileChange(File file) {
        String className = GrailsResourceUtils.getClassName(file.getAbsolutePath());
        Class<?> cls = null;

        if (className != null) {
            cls = loadApplicationClass(className);
        }

        informOfClassChange(file, cls);
    }

    public void informOfClassChange(File file, @SuppressWarnings("rawtypes") Class cls) {
        if (cls != null && cls.getName().equals(CONFIG_FILE)) {
            ConfigSlurper configSlurper = ConfigurationHelper.getConfigSlurper(Environment.getCurrent().getName(), application);
            ConfigObject c;
            try {
                c = configSlurper.parse(file.toURI().toURL());
                application.getConfig().merge(c);
                informPluginsOfConfigChange();

            } catch (Exception e) {
                // ignore
            }
        }
        else {

            MetaClassRegistry registry = GroovySystem.getMetaClassRegistry();
            registry.removeMetaClass(cls);
            ExpandoMetaClass newMc = new ExpandoMetaClass(cls, true, true);
            newMc.initialize();
            registry.setMetaClass(cls, newMc);


            for (GrailsPlugin grailsPlugin : pluginList) {
                if (grailsPlugin.hasInterestInChange(file.getAbsolutePath())) {
                    if (cls != null) {
                        grailsPlugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CHANGE, cls);
                    }
                    else {
                        grailsPlugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CHANGE, new FileSystemResource(file));
                    }
                }
            }
        }
    }

    private Class<?> loadApplicationClass(String className) {
        Class<?> cls = null;
        try {
            cls = application.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            // ignore
        }
        return cls;
    }


    public String getPluginPathForClass(Class<?> theClass) {
        if (theClass != null) {
            org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin ann =
                theClass.getAnnotation(org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin.class);
            if (ann != null) {
                return getPluginPath(ann.name());
            }
        }
        return null;
    }

    public String getPluginViewsPathForInstance(Object instance) {
        if (instance != null) {
            return getPluginViewsPathForClass(instance.getClass());
        }
        return null;
    }

    public String getPluginViewsPathForClass(Class<?> theClass) {
        if (theClass != null) {
            final String path = getPluginPathForClass(theClass);
            if (StringUtils.hasText(path)) {
                return path + '/' + GrailsResourceUtils.GRAILS_APP_DIR + "/views";
            }
        }
        return null;
    }
}
