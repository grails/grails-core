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

import grails.spring.BeanBuilder;
import grails.util.BuildScope;
import grails.util.BuildSettings;
import grails.util.BuildSettingsHolder;
import grails.util.Environment;
import grails.util.GrailsUtil;
import grails.util.Metadata;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.util.slurpersupport.GPathResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.compiler.GrailsClassLoader;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoaderHolder;
import org.codehaus.groovy.grails.documentation.DocumentationContext;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.codehaus.groovy.grails.support.ParentApplicationContextAware;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Implementation of the GrailsPlugin interface that wraps a Groovy plugin class
 * and provides the magic to invoke its various methods from Java.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
@SuppressWarnings("rawtypes")
public class DefaultGrailsPlugin extends AbstractGrailsPlugin implements ParentApplicationContextAware {

    private static final String PLUGIN_CHANGE_EVENT_CTX = "ctx";
    private static final String PLUGIN_CHANGE_EVENT_APPLICATION = "application";
    private static final String PLUGIN_CHANGE_EVENT_PLUGIN = "plugin";
    private static final String PLUGIN_CHANGE_EVENT_SOURCE = "source";
    private static final String PLUGIN_CHANGE_EVENT_MANAGER = "manager";

    private static final String PLUGIN_OBSERVE = "observe";
    private static final Log LOG = LogFactory.getLog(DefaultGrailsPlugin.class);
    private static final String INCLUDES = "includes";
    private static final String EXCLUDES = "excludes";
    private GrailsPluginClass pluginGrailsClass;

    private GroovyObject plugin;
    protected BeanWrapper pluginBean;
    private Closure onChangeListener;
    private Resource[] watchedResources = new Resource[0];

    private long[] modifiedTimes = new long[0];
    private PathMatchingResourcePatternResolver resolver;
    private String[] resourcesReferences;
    private int[] resourceCount;
    private String[] loadAfterNames = new String[0];
    private String[] loadBeforeNames = new String[0];
    private String[] influencedPluginNames = new String[0];
    private String status = STATUS_ENABLED;
    private String[] observedPlugins;
    private long pluginLastModified = Long.MAX_VALUE;
    private URL pluginUrl;
    private Closure onConfigChangeListener;
    private Closure onShutdownListener;
    private Class<?>[] providedArtefacts = new Class[0];
    private Map pluginScopes;
    private Map pluginEnvs;
    private List<String> pluginExcludes = new ArrayList<String>();
    private Collection<? extends TypeFilter> typeFilters = new ArrayList<TypeFilter>();
    private Resource pluginDescriptor;

    public DefaultGrailsPlugin(Class<?> pluginClass, Resource resource, GrailsApplication application) {
        super(pluginClass, application);
        // create properties
        dependencies = Collections.emptyMap();
        pluginDescriptor = resource;
        resolver = new PathMatchingResourcePatternResolver();
        if (resource != null) {
            try {
                pluginUrl = resource.getURL();
                URLConnection urlConnection = null;
                try {
                    urlConnection = pluginUrl.openConnection();
                    pluginLastModified = urlConnection.getLastModified();
                }
                finally {
                    InputStream is = urlConnection != null ? urlConnection.getInputStream() : null;
                    IOUtils.closeQuietly(is);
                }
            }
            catch (IOException e) {
                LOG.warn("I/O error reading last modified date of plug-in [" + pluginClass +
                        "], you won't be able to reload changes: " + e.getMessage(),e);
            }
        }

        initialisePlugin(pluginClass);
    }

    private void initialisePlugin(Class<?> clazz) {
        pluginGrailsClass = new GrailsPluginClass(clazz);
        plugin = (GroovyObject)pluginGrailsClass.newInstance();
        pluginBean = new BeanWrapperImpl(plugin);

        // configure plugin
        evaluatePluginVersion();
        evaluatePluginDependencies();
        evaluatePluginLoadAfters();
        evaluateProvidedArtefacts();
        evaluatePluginEvictionPolicy();
        evaluatePluginInfluencePolicy();
        evaluateOnChangeListener();
        evaluateObservedPlugins();
        evaluatePluginStatus();
        evaluatePluginScopes();
        evaluatePluginExcludes();
        evaluateTypeFilters();
    }

    @SuppressWarnings("unchecked")
    private void evaluateTypeFilters() {
        Object result = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, TYPE_FILTERS);
        if (result instanceof List) {
            typeFilters = (List<TypeFilter>) result;
        }
    }

    @SuppressWarnings("unchecked")
    private void evaluatePluginExcludes() {
        Object result = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, PLUGIN_EXCLUDES);
        if (result instanceof List) {
            pluginExcludes = (List<String>) result;
        }
    }

    private void evaluatePluginScopes() {
        // Damn I wish Java had closures
        pluginScopes = evaluateIncludeExcludeProperty(SCOPES, new Closure(this) {
            private static final long serialVersionUID = 1;
            @Override
            public Object call(Object arguments) {
                final String scopeName = ((String) arguments).toUpperCase();
                try {
                    return BuildScope.valueOf(scopeName);
                }
                catch (IllegalArgumentException e) {
                    throw new GrailsConfigurationException("Plugin " + this + " specifies invalid scope [" + scopeName + "]");
                }
            }
        });
        pluginEnvs = evaluateIncludeExcludeProperty(ENVIRONMENTS, new Closure(this) {
            private static final long serialVersionUID = 1;
            @Override
            public Object call(Object arguments) {
                String envName = (String)arguments;
                Environment env = Environment.getEnvironment(envName);
                if (env != null) return env.getName();
                return arguments;
            }
        });
    }

    private Map evaluateIncludeExcludeProperty(String name, Closure converter) {
        Map resultMap = new HashMap();
        Object propertyValue = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, name);
        if (propertyValue instanceof Map) {
            Map containedMap = (Map)propertyValue;

            Object includes = containedMap.get(INCLUDES);
            evaluateAndAddIncludeExcludeObject(resultMap, includes, true, converter);

            Object excludes = containedMap.get(EXCLUDES);
            evaluateAndAddIncludeExcludeObject(resultMap, excludes, false, converter);
        }
        else {
            evaluateAndAddIncludeExcludeObject(resultMap, propertyValue, true, converter);
        }
        return resultMap;
    }

    private void evaluateAndAddIncludeExcludeObject(Map targetMap, Object includeExcludeObject, boolean include, Closure converter) {
        if (includeExcludeObject instanceof String) {
            final String includeExcludeString = (String) includeExcludeObject;
            evaluateAndAddToIncludeExcludeSet(targetMap,includeExcludeString, include, converter);
        }
        else if (includeExcludeObject instanceof List) {
            List includeExcludeList = (List) includeExcludeObject;
            evaluateAndAddListOfValues(targetMap,includeExcludeList, include, converter);
        }
    }

    private void evaluateAndAddListOfValues(Map targetMap, List includeExcludeList, boolean include, Closure converter) {
        for (Object scope : includeExcludeList) {
            if (scope instanceof String) {
                final String scopeName = (String) scope;
                evaluateAndAddToIncludeExcludeSet(targetMap, scopeName, include, converter);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void evaluateAndAddToIncludeExcludeSet(Map targetMap, String includeExcludeString, boolean include, Closure converter) {
        Set set = lazilyCreateIncludeOrExcludeSet(targetMap,include);
        set.add(converter.call(includeExcludeString));
    }

    @SuppressWarnings("unchecked")
    private Set lazilyCreateIncludeOrExcludeSet(Map targetMap, boolean include) {
        String key = include ? INCLUDES : EXCLUDES ;
        Set set = (Set) targetMap.get(key);
        if (set == null) {
            set = new HashSet();
            targetMap.put(key, set);
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private void evaluateProvidedArtefacts() {
        Object result = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, PROVIDED_ARTEFACTS);
        if (result instanceof Collection) {
            final Collection artefactList = (Collection) result;
            providedArtefacts = (Class<?>[])artefactList.toArray(new Class[artefactList.size()]);
        }
    }

    public DefaultGrailsPlugin(Class<?> pluginClass, GrailsApplication application) {
        this(pluginClass, null, application);
    }

    private void evaluateObservedPlugins() {
        if (pluginBean.isReadableProperty(PLUGIN_OBSERVE)) {
            Object observeProperty = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, PLUGIN_OBSERVE);
            if (observeProperty instanceof Collection) {
                Collection  observeList = (Collection)observeProperty;
                observedPlugins = new String[observeList.size()];
                int j = 0;
                for (Object anObserveList : observeList) {
                    String pluginName = anObserveList.toString();
                    observedPlugins[j++] = pluginName;
                }
            }
        }
        if (observedPlugins == null) {
            observedPlugins = new String[0];
        }
    }

    private void evaluatePluginStatus() {
        if (pluginBean.isReadableProperty(STATUS)) {
            Object statusObj = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, STATUS);
            if (statusObj != null) {
                status = statusObj.toString().toLowerCase();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void evaluateOnChangeListener() {
        if (pluginBean.isReadableProperty(ON_SHUTDOWN)) {
            onShutdownListener = (Closure)GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, ON_SHUTDOWN);
        }
        if (pluginBean.isReadableProperty(ON_CONFIG_CHANGE)) {
            onConfigChangeListener = (Closure)GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, ON_CONFIG_CHANGE);
        }
        if (pluginBean.isReadableProperty(ON_CHANGE)) {
            onChangeListener = (Closure) GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, ON_CHANGE);
        }

        final boolean warDeployed = Metadata.getCurrent().isWarDeployed();
        final boolean reloadEnabled = Environment.getCurrent().isReloadEnabled();

        if (!((reloadEnabled || !warDeployed) && onChangeListener != null)) {
            return;
        }

        Object referencedResources = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, WATCHED_RESOURCES);

        try {
            List resourceList = null;
            if (referencedResources instanceof String) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Configuring plugin "+this+" to watch resources with pattern: " + referencedResources);
                }
                resourceList = new ArrayList();
                resourceList.add(referencedResources.toString());
            }
            else if (referencedResources instanceof List) {
                resourceList = (List)referencedResources;
            }

            if (resourceList != null) {

                List<String> resourceListTmp = new ArrayList<String>();
                final Resource[] pluginDirs = GrailsPluginUtils.getPluginDirectories();
                final Environment env = Environment.getCurrent();
                final String baseLocation = env.getReloadLocation();

                for (Object ref : resourceList) {
                    String stringRef = ref.toString();
                    if (!warDeployed) {
                        for (Resource pluginDir : pluginDirs) {
                            if (pluginDir !=null) {
                                String pluginResources = getResourcePatternForBaseLocation(pluginDir.getFile().getCanonicalPath(), stringRef);
                                resourceListTmp.add(pluginResources);
                            }
                        }
                        addBaseLocationPattern(resourceListTmp, baseLocation, stringRef);
                    }
                    else {
                        addBaseLocationPattern(resourceListTmp, baseLocation, stringRef);
                    }
                }

                resourcesReferences = new String[resourceListTmp.size()];
                resourceCount = new int[resourceListTmp.size()];
                for (int i = 0; i < resourcesReferences.length; i++) {
                    String resRef = resourceListTmp.get(i);
                    resourcesReferences[i]=resRef;
                }
                for (int i = 0; i < resourcesReferences.length; i++) {
                    String res = resourcesReferences[i];

                    // Try to load the resources that match the "res" pattern.
                    Resource[] tmp = new Resource[0];
                    try {
                        try {
                            tmp = (Resource[]) ArrayUtils.addAll(tmp,resolver.getResources(res));
                        }
                        catch (IOException e) {
                            // ignore, no resources at default location
                        }
                    }
                    catch (Exception ex) {
                        // The pattern is invalid so we continue as if there are no matching files.
                        LOG.debug("Resource pattern [" + res + "] is not valid - maybe base directory does not exist?");
                    }
                    resourceCount[i] = tmp.length;

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Watching resource set ["+(i+1)+"]: " + ArrayUtils.toString(tmp));
                    }
                    if (tmp.length == 0) {
                        tmp = resolver.getResources("classpath*:" + res);
                    }

                    if (tmp.length > 0) {
                        watchedResources = (Resource[])ArrayUtils.addAll(watchedResources, tmp);
                    }
                }
            }
        }
        catch (IllegalArgumentException e) {
            if (GrailsUtil.isDevelopmentEnv()) {
                LOG.debug("Cannot load plug-in resource watch list from [" + ArrayUtils.toString(resourcesReferences) +
                        "]. This means that the plugin " + this +
                        ", will not be able to auto-reload changes effectively. Try runnng grails upgrade.: " + e.getMessage());
            }
        }
        catch (IOException e) {
            if (GrailsUtil.isDevelopmentEnv()) {
                LOG.debug("Cannot load plug-in resource watch list from [" + ArrayUtils.toString(resourcesReferences) +
                        "]. This means that the plugin " + this +
                        ", will not be able to auto-reload changes effectively. Try runnng grails upgrade.: " + e.getMessage());
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Plugin "+this+" found ["+watchedResources.length+"] to watch");
        }
        try {
            initializeModifiedTimes();
        }
        catch (IOException e) {
            LOG.warn("I/O exception initializing modified times for watched resources: " + e.getMessage(), e);
        }
    }

    private void addBaseLocationPattern(List<String> resourceList, final String baseLocation, String pattern) {
        if (baseLocation != null) {
            final String reloadLocationResourcePattern = getResourcePatternForBaseLocation(baseLocation, pattern);
            resourceList.add(reloadLocationResourcePattern);
        }
        else {
            resourceList.add(pattern);
        }
    }

    private String getResourcePatternForBaseLocation(String baseLocation, String resourcePath) {
        String location = baseLocation;
        if (!location.endsWith(File.separator)) location = location + File.separator;
        if (resourcePath.startsWith(".")) resourcePath = resourcePath.substring(1);
        else if (resourcePath.startsWith("file:./")) resourcePath = resourcePath.substring(7);
        resourcePath = "file:" + location + resourcePath;
        return resourcePath;
    }

    @SuppressWarnings("unchecked")
    private void evaluatePluginInfluencePolicy() {
        if (pluginBean.isReadableProperty(INFLUENCES)) {
            List influencedList = (List)pluginBean.getPropertyValue(INFLUENCES);
            if (influencedList != null) {
                influencedPluginNames = (String[])influencedList.toArray(new String[influencedList.size()]);
            }
        }
    }

    private void evaluatePluginVersion() {
        if (!pluginBean.isReadableProperty(VERSION)) {
            throw new PluginException("Plugin [" + getName() + "] must specify a version!");
        }

        Object vobj = plugin.getProperty(VERSION);
        if (vobj != null) {
            version = vobj.toString();
        }
        else {
            throw new PluginException("Plugin " + this + " must specify a version. eg: def version = 0.1");
        }
    }

    private void evaluatePluginEvictionPolicy() {
        if (pluginBean.isReadableProperty(EVICT)) {
            List pluginsToEvict = (List) GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, EVICT);
            if (pluginsToEvict != null) {
                evictionList = new String[pluginsToEvict.size()];
                int index = 0;
                for (Object o : pluginsToEvict) {
                    evictionList[index++] = o != null ? o.toString() : "";
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void evaluatePluginLoadAfters() {
        if (pluginBean.isReadableProperty(PLUGIN_LOAD_AFTER_NAMES)) {
            List loadAfterNamesList = (List) GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, PLUGIN_LOAD_AFTER_NAMES);
            if (loadAfterNamesList != null) {
                loadAfterNames = (String[])loadAfterNamesList.toArray(new String[loadAfterNamesList.size()]);
            }
        }
        if (pluginBean.isReadableProperty(PLUGIN_LOAD_BEFORE_NAMES)) {
            List loadBeforeNamesList = (List) GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, PLUGIN_LOAD_BEFORE_NAMES);
            if (loadBeforeNamesList != null) {
                loadBeforeNames = (String[])loadBeforeNamesList.toArray(new String[loadBeforeNamesList.size()]);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void evaluatePluginDependencies() {
        if (pluginBean.isReadableProperty(DEPENDS_ON)) {
            dependencies = (Map) GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(plugin, DEPENDS_ON);
            dependencyNames = dependencies.keySet().toArray(new String[dependencies.size()]);
        }
    }

    @Override
    public String[] getLoadAfterNames() {
        return loadAfterNames;
    }

    @Override
    public String[] getLoadBeforeNames() {
        return loadBeforeNames;
    }

    /**
     * @return the resolver
     */
    public PathMatchingResourcePatternResolver getResolver() {
        return resolver;
    }

    public ApplicationContext getParentCtx() {
        return application.getParentContext();
    }

    public BeanBuilder beans(Closure closure) {
        BeanBuilder bb = new BeanBuilder(getParentCtx(), new GroovyClassLoader(application.getClassLoader()));
        bb.invokeMethod("beans", new Object[]{closure});
        return bb;
    }

    private void initializeModifiedTimes() throws IOException {
        modifiedTimes = new long[watchedResources.length];
        for (int i = 0; i < watchedResources.length; i++) {
            Resource r = watchedResources[i];
            URLConnection c = null;
            try {
                c = r.getURL().openConnection();
                c.setDoInput(false);
                c.setDoOutput(false);
                modifiedTimes[i] = c.getLastModified();
            }
            finally {
                if (c != null) {
                    try {
                        InputStream is = c.getInputStream();
                        if (is != null) {
                            is.close();
                        }
                    }
                    catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }

    public void doWithApplicationContext(ApplicationContext ctx) {
        try {
            if (pluginBean.isReadableProperty(DO_WITH_APPLICATION_CONTEXT)) {
                Closure c = (Closure)plugin.getProperty(DO_WITH_APPLICATION_CONTEXT);
                if (enableDocumentationGeneration()) {
                    DocumentationContext.getInstance().setActive(true);
                }

                c.setDelegate(this);
                c.call(new Object[]{ctx});
            }
        }
        finally {
            if (enableDocumentationGeneration()) {
                DocumentationContext.getInstance().reset();
            }
        }
    }

    private boolean enableDocumentationGeneration() {
        return !Metadata.getCurrent().isWarDeployed() && isBasePlugin();
    }

    public void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {

        if (pluginBean.isReadableProperty(DO_WITH_SPRING)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Plugin " + this + " is participating in Spring configuration...");
            }

            Closure c = (Closure)plugin.getProperty(DO_WITH_SPRING);
            BeanBuilder bb = new BeanBuilder(getParentCtx(),springConfig, application.getClassLoader());
            Binding b = new Binding();
            b.setVariable("application", application);
            b.setVariable("manager", getManager());
            b.setVariable("plugin", this);
            b.setVariable("parentCtx", getParentCtx());
            b.setVariable("resolver", getResolver());
            bb.setBinding(b);
            c.setDelegate(bb);
            bb.invokeMethod("beans", new Object[]{c});
        }
    }

    @Override
    public String getName() {
        return pluginGrailsClass.getLogicalPropertyName();
    }

    public void addExclude(BuildScope buildScope) {
        addExcludeRuleInternal(pluginScopes, buildScope);
    }

    @SuppressWarnings("unchecked")
    private void addExcludeRuleInternal(Map map, Object o) {
        Collection excludes = (Collection) map.get(EXCLUDES);
        if (excludes == null) {
            excludes = new ArrayList();
            map.put(EXCLUDES, excludes);
        }
        Collection includes = (Collection) map.get(INCLUDES);
        if (includes != null) includes.remove(o);
        excludes.add(o);
    }

    public void addExclude(Environment env) {
        addExcludeRuleInternal(pluginEnvs, env);
    }

    public boolean supportsScope(BuildScope buildScope) {
        return supportsValueInIncludeExcludeMap(pluginScopes, buildScope);
    }

    public boolean supportsEnvironment(Environment environment) {
        return supportsValueInIncludeExcludeMap(pluginEnvs, environment.getName());
    }

    public boolean supportsCurrentScopeAndEnvironment() {
        BuildScope bs = BuildScope.getCurrent();
        Environment e = Environment.getCurrent();
        return supportsEnvironment(e) && supportsScope(bs);
    }

    private boolean supportsValueInIncludeExcludeMap(Map includeExcludeMap, Object value) {
        if (includeExcludeMap.isEmpty()) {
            return true;
        }

        Set includes = (Set) includeExcludeMap.get(INCLUDES);
        if (includes != null) {
            return includes.contains(value);
        }

        Set excludes = (Set)includeExcludeMap.get(EXCLUDES);
        return !(excludes != null && excludes.contains(value));
    }

    public void doc(String text) {
        if (enableDocumentationGeneration()) {
            DocumentationContext.getInstance().document(text);
        }
    }

    @Override
    public String[] getDependencyNames() {
        return dependencyNames;
    }

    /**
     * @return the watchedResources
     */
    public Resource[] getWatchedResources() {
        return watchedResources;
    }

    @Override
    public String getDependentVersion(String name) {
        Object dependentVersion = dependencies.get(name);
        if (dependentVersion == null) {
            throw new PluginException("Plugin [" + getName() + "] referenced dependency [" + name + "] with no version!");
        }
        return dependentVersion.toString();
    }

    @Override
    public String toString() {
        return "[" + getName() + ":" + getVersion() + "]";
    }

    @Override
    public void doWithWebDescriptor(GPathResult webXml) {
        if (pluginBean.isReadableProperty(DO_WITH_WEB_DESCRIPTOR)) {
            Closure c = (Closure)plugin.getProperty(DO_WITH_WEB_DESCRIPTOR);
            c.setResolveStrategy(Closure.DELEGATE_FIRST);
            c.setDelegate(this);
            c.call(webXml);
        }
    }

    /**
     * Monitors the plugin resources defined in the watchResources property for changes and
     * fires onChange events by calling an onChange closure defined in the plugin (if it exists)
     */
    @Override
    public boolean checkForChanges() {
        if (pluginUrl != null) {
            long currentModified = -1;
            URLConnection conn = null;
            try {
                conn = pluginUrl.openConnection();

                currentModified = conn.getLastModified();
                if (currentModified > pluginLastModified) {

                    if (LOG.isInfoEnabled()) {
                        LOG.info("Grails plug-in " + this + " changed, reloading changes..");
                    }

                    GroovyClassLoader gcl = new GroovyClassLoader(application.getClassLoader());
                    initialisePlugin(gcl.parseClass(DefaultGroovyMethods.getText(conn.getInputStream())));
                    pluginLastModified = currentModified;
                    return true;
                }
            }
            catch (IOException e) {
                LOG.warn("Error reading plugin [" + pluginClass + "] last modified date, cannot reload following change: " + e.getMessage());
            }
            finally {
                if (conn!=null) {
                    try {
                        conn.getInputStream().close();
                    }
                    catch (IOException e) {
                        LOG.warn("Error closing URL connection to plugin resource [" + pluginUrl + "]: " + e.getMessage(), e);
                    }
                }
            }
        }

        if (onChangeListener != null) {
            checkForNewResources(this);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Plugin " + this + " checking [" + watchedResources.length + "] resources for changes..");
            }
            for (int i = 0; i < watchedResources.length; i++) {
                final Resource r = watchedResources[i];
                long modifiedFlag = checkModified(r, modifiedTimes[i]) ;
                if ( modifiedFlag > -1) {
                    if (LOG.isInfoEnabled()) LOG.info("Grails plug-in resource [" + r + "] changed, reloading changes..");

                    modifiedTimes[i] = modifiedFlag;
                    fireModifiedEvent(r, this);
                    refreshInfluencedPlugins();
                }
            }
        }
        return false;
    }

    /**
     * This method will retrieve all the influenced plugins from the manager and
     * call refresh() on each one.
     */
    private void refreshInfluencedPlugins() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Plugin " + this + " starting refresh of influenced plugins " + ArrayUtils.toString(influencedPluginNames));
        }
        if (manager != null) {
            for (String name : influencedPluginNames) {
                GrailsPlugin grailsPlugin = manager.getGrailsPlugin(name);

                if (grailsPlugin != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(this + " plugin is refreshing influenced plugin " + grailsPlugin + " following change to resource");
                    }

                    grailsPlugin.refresh();
                }
            }
        }
        else if (LOG.isDebugEnabled()) {
            LOG.debug("Plugin "+this+" cannot refresh influenced plugins, manager is not found");
        }
    }

    /**
     * Takes a Resource and checks it against the previous modified time passed
     * in the arguments. If the resource was modified it will return the new modified time, otherwise
     * it will return -1.
     *
     * @param r the Resource instance
     * @param previousModifiedTime the last time the Resource was modified
     * @return the new modified time or -1
     */
    private long checkModified(Resource r, long previousModifiedTime) {
        // If the resource doesn't exist, skip the check.
        if (!r.exists()) {
            return -1;
        }

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Checking modified for resource " + r);
            }

            long lastModified = r.lastModified();

            if ( previousModifiedTime < lastModified ) {
                return lastModified;
            }
        }
        catch (IOException e) {
            LOG.debug("Unable to read last modified date of plugin resource" +e.getMessage(),e);
        }
        return -1;
    }

    private void checkForNewResources(final GrailsPlugin grailsPlugin) {

        if (resourcesReferences == null) {
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Plugin " + grailsPlugin + " checking [" + ArrayUtils.toString(resourcesReferences) +
                    "] resource references new resources that have been added..");
        }

        for (int i = 0; i < resourcesReferences.length; i++) {
            String resourcesReference = resourcesReferences[i];
            try {
                Resource[] tmp = resolver.getResources(resourcesReference);
                if (resourceCount[i] < tmp.length) {
                    Resource newResource = null;
                    resourceCount[i] = tmp.length;

                    for (Resource resource : tmp) {
                        if (!ArrayUtils.contains(watchedResources, resource)) {
                            newResource = resource;
                            break;
                        }
                    }

                    if (newResource != null) {
                        watchedResources = (Resource[])ArrayUtils.add(watchedResources, newResource);

                        if (LOG.isInfoEnabled()) {
                            LOG.info("Found new Grails plug-in resource [" + newResource + "], adding to application..");
                        }

                        if (newResource.getFilename().endsWith(".groovy")) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("[GrailsPlugin] plugin resource ["+newResource+"] added, registering resource with class loader...");
                            }

                            ClassLoader classLoader = application.getClassLoader();

                            GrailsResourceLoader resourceLoader = GrailsResourceLoaderHolder.getResourceLoader();

                            Resource[] classLoaderResources = resourceLoader.getResources();
                            classLoaderResources = (Resource[])ArrayUtils.add(classLoaderResources, newResource);
                            resourceLoader.setResources(classLoaderResources);

                            if (classLoader instanceof GrailsClassLoader) {
                                ((GrailsClassLoader)classLoader).setGrailsResourceLoader(resourceLoader);
                            }
                        }

                        initializeModifiedTimes();

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("[GrailsPlugin] plugin resource [" + newResource + "] added, firing event if possible..");
                        }
                        fireModifiedEvent(newResource, grailsPlugin);
                    }
                }
            }
            catch (Exception e) {
                LOG.debug("Plugin " + this + "  was unable to check for new plugin resources: " + e.getMessage());
            }
        }
    }

    protected void fireModifiedEvent(final Resource resource,
            @SuppressWarnings("unused") final GrailsPlugin grailsPlugin) {

        Class<?> loadedClass = null;
        String className = GrailsResourceUtils.getClassName(resource);

        Object source;
        if (className != null) {
            Class<?> oldClass = application.getClassForName(className);
            loadedClass = attemptClassReload(className);
            source = loadedClass != null ? loadedClass : oldClass;
        }
        else {
            source = resource;
        }

        if (loadedClass != null && Modifier.isAbstract(loadedClass.getModifiers())) {
            restartContainer();
        }
        else if (source != null) {
            Map event = notifyOfEvent(EVENT_ON_CHANGE, source);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Firing onChange event listener with event object ["+event+"]");
            }
        }
    }

    public void restartContainer() {
        // here we touch the classes directory if the file is abstract to force the Grails server
        // to restart the web application
        try {
            BuildSettings settings = BuildSettingsHolder.getSettings();
            File classesDir = settings != null ? settings.getClassesDir() : null;
            if (classesDir == null) {
                Resource r = applicationContext.getResource("/WEB-INF/classes");
                classesDir = r.getFile();
            }
            classesDir.setLastModified(System.currentTimeMillis());
        }
        catch (IOException e) {
            LOG.error("Error retrieving /WEB-INF/classes directory: " + e.getMessage(),e);
        }
    }

    private Class<?> attemptClassReload(String className) {
        final ClassLoader loader = application.getClassLoader();
        if (loader instanceof GrailsClassLoader) {
            GrailsClassLoader grailsLoader = (GrailsClassLoader)loader;
            return grailsLoader.reloadClass(className);
        }
        // Added this to see whether it helps track down an intermittent
        // bug with dynamic loading of new artifacts.
        LOG.warn("Expected GrailsClassLoader - got " + loader.getClass());
        return null;
    }

    public void setWatchedResources(Resource[] watchedResources) throws IOException {
        this.watchedResources = watchedResources;
        initializeModifiedTimes();
    }

    /*
     * These two properties help the closures to resolve a log and plugin variable during executing
     */
    public Log getLog() {
        return LOG;
    }
    public GrailsPlugin getPlugin() {
        return this;
    }

    public void setParentApplicationContext(ApplicationContext parent) {
        // do nothing for the moment
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.plugins.AbstractGrailsPlugin#refresh()
     */
    @Override
    public void refresh() {
        refresh(true);
    }

    public void refresh(boolean fireEvent) {
        for (Resource r : watchedResources) {
            try {
                r.getFile().setLastModified(System.currentTimeMillis());
            }
            catch (IOException e) {
                // ignore
            }
            if (fireEvent) {
                fireModifiedEvent(r, this);
            }
        }
    }

    public GroovyObject getInstance() {
        return plugin;
    }

    public void doWithDynamicMethods(ApplicationContext ctx) {
        try {
            if (pluginBean.isReadableProperty(DO_WITH_DYNAMIC_METHODS)) {
                Closure c = (Closure)plugin.getProperty(DO_WITH_DYNAMIC_METHODS);
                if (enableDocumentationGeneration()) {
                    DocumentationContext.getInstance().setActive(true);
                }

                c.setDelegate(this);
                c.call(new Object[]{ctx});
            }
        }
        finally {
            if (enableDocumentationGeneration()) {
                DocumentationContext.getInstance().reset();
            }
        }
    }

    public boolean isEnabled() {
        return STATUS_ENABLED.equals(status);
    }

    public String[] getObservedPluginNames() {
        return observedPlugins;
    }

    public void notifyOfEvent(Map event) {
        if (onChangeListener != null) {
            invokeOnChangeListener(event);
        }
    }

    @SuppressWarnings("serial")
    public Map notifyOfEvent(int eventKind, final Object source) {
        Map<String, Object> event = new HashMap<String, Object>() {{
            put(PLUGIN_CHANGE_EVENT_SOURCE, source);
            put(PLUGIN_CHANGE_EVENT_PLUGIN, plugin);
            put(PLUGIN_CHANGE_EVENT_APPLICATION, application);
            put(PLUGIN_CHANGE_EVENT_MANAGER, getManager());
            put(PLUGIN_CHANGE_EVENT_CTX, applicationContext);
        }};

        switch (eventKind) {
            case EVENT_ON_CHANGE:
                notifyOfEvent(event);
                getManager().informObservers(getName(), event);
                break;
            case EVENT_ON_SHUTDOWN:
                invokeOnShutdownEventListener(event);
                break;

            case EVENT_ON_CONFIG_CHANGE:
                invokeOnConfigChangeListener(event);
                break;
            default:
                notifyOfEvent(event);
        }
        return event;
    }

    private void invokeOnShutdownEventListener(Map event) {
        callEvent(onShutdownListener,event);
    }

    private void invokeOnConfigChangeListener(Map event) {
        callEvent(onConfigChangeListener,event);
    }

    private void callEvent(Closure closureHook, Map event) {
        if (closureHook != null) {
            closureHook.setDelegate(this);
            closureHook.call(new Object[]{event});
        }
    }

    private void invokeOnChangeListener(Map event) {
        onChangeListener.setDelegate(this);
        onChangeListener.call(new Object[]{event});
        
        // Apply any factory post processors in case the change listener has changed any
        // bean definitions (GRAILS-5763)
        if (applicationContext instanceof GenericApplicationContext) {
            GenericApplicationContext ctx = (GenericApplicationContext) applicationContext;
            ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
            for (BeanFactoryPostProcessor postProcessor : ctx.getBeanFactoryPostProcessors()) {
                postProcessor.postProcessBeanFactory(beanFactory);
            }
        }
    }

    public void doArtefactConfiguration() {
        if (!pluginBean.isReadableProperty(ARTEFACTS)) {
            return;
        }

        List l = (List)plugin.getProperty(ARTEFACTS);
        for (Object artefact : l) {
            if (artefact instanceof Class) {
                Class artefactClass = (Class) artefact;
                if (ArtefactHandler.class.isAssignableFrom(artefactClass)) {
                    try {
                        application.registerArtefactHandler((ArtefactHandler) artefactClass.newInstance());
                    }
                    catch (InstantiationException e) {
                        LOG.error("Cannot instantiate an Artefact Handler:" + e.getMessage(), e);
                    }
                    catch (IllegalAccessException e) {
                        LOG.error("The constructor of the Artefact Handler is not accessible:" + e.getMessage(), e);
                    }
                }
                else {
                    LOG.error("This class is not an ArtefactHandler:" + artefactClass.getName());
                }
            }
            else {
                if (artefact instanceof ArtefactHandler) {
                    application.registerArtefactHandler((ArtefactHandler) artefact);
                }
                else {
                    LOG.error("This object is not an ArtefactHandler:" + artefact + "[" + artefact.getClass().getName() + "]");
                }
            }
        }
    }

    public Class<?>[] getProvidedArtefacts() {
        return providedArtefacts;
    }

    public List<String> getPluginExcludes() {
        return pluginExcludes;
    }

    public Collection<? extends TypeFilter> getTypeFilters() {
        return typeFilters;
    }

    public String getFullName() {
        return getName() + '-' + getVersion();
    }

    public Resource getDescriptor() {
        return pluginDescriptor;
    }

    public Resource getPluginDir() {
        try {
            return pluginDescriptor.createRelative(".");
        }
        catch (IOException e) {
            return null;
        }
    }

    public Map getProperties() {
        return DefaultGroovyMethods.getProperties(plugin);
    }
}
