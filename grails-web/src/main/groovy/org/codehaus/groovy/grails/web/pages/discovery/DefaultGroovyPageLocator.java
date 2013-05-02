/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.web.pages.discovery;

import grails.util.CollectionUtils;
import grails.util.Environment;

import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.BinaryGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerAware;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextResourceLoader;

/**
 * Used to locate GSPs whether in development or WAR deployed mode from static
 * resources, custom resource loaders and binary plugins.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class DefaultGroovyPageLocator implements GroovyPageLocator, ServletContextAware, ApplicationContextAware, PluginManagerAware {

    private static final Log LOG = LogFactory.getLog(DefaultGroovyPageLocator.class);
    private static final String PATH_TO_WEB_INF_VIEWS = GrailsApplicationAttributes.PATH_TO_VIEWS;
    private static final String SLASHED_VIEWS_DIR_PATH = "/" + GrailsResourceUtils.VIEWS_DIR_PATH;
    private static final String PLUGINS_PATH = "/plugins/";
    private static final String BLANK = "";
    protected Collection<ResourceLoader> resourceLoaders = new ConcurrentLinkedQueue<ResourceLoader>();
    protected GrailsPluginManager pluginManager;
    private ConcurrentMap<String, String> precompiledGspMap;
    protected boolean warDeployed = Environment.isWarDeployed();
    protected boolean reloadEnabled = !warDeployed;
    private Set<String> reloadedPrecompiledGspClassNames = new CopyOnWriteArraySet<String>();

    public void setResourceLoader(ResourceLoader resourceLoader) {
        addResourceLoader(resourceLoader);
    }

    public void addResourceLoader(ResourceLoader resourceLoader) {
        if (resourceLoader != null && !resourceLoaders.contains(resourceLoader)) {
            resourceLoaders.add(resourceLoader);
        }
    }

    public void setPrecompiledGspMap(Map<String, String> precompiledGspMap) {
        if (precompiledGspMap == null) {
            this.precompiledGspMap = null;
        } else {
            this.precompiledGspMap = new ConcurrentHashMap<String, String>(precompiledGspMap);
        }
    }

    public GroovyPageScriptSource findPage(final String uri) {
        GroovyPageScriptSource scriptSource = findResourceScriptSource(uri);
        if (scriptSource == null) {
            scriptSource = findBinaryScriptSource(uri);
        }
        if (scriptSource == null) {
            scriptSource = findResourceScriptSourceInPlugins(uri);
        }
        return scriptSource;
    }

    protected Resource findReloadablePage(final String uri) {
        Resource resource = findResource(uri);
        if (resource == null) {
            resource = findResourceInPlugins(uri);
        }
        return resource;
    }

    public GroovyPageScriptSource findPageInBinding(String pluginName, String uri, GroovyPageBinding binding) {
        String contextPath = resolveContextPath(pluginName, uri, binding);

        GroovyPageScriptSource scriptSource = findPageInBinding(GrailsResourceUtils.appendPiecesForUri(contextPath, uri), binding);
        if (scriptSource == null) {
            scriptSource = findResourceScriptSource(uri);
        }
        return scriptSource;
    }

    protected String resolveContextPath(String pluginName, String uri, GroovyPageBinding binding) {
        String contextPath = null;

        if (uri.startsWith("/plugins/")) {
            contextPath = BLANK;
        } else if (pluginName != null && pluginManager != null) {
            contextPath = pluginManager.getPluginPath(pluginName);
        } else {
            contextPath = binding.getPluginContextPath() != null ? binding.getPluginContextPath() : BLANK;
        }

        return contextPath;
    }

    public void removePrecompiledPage(GroovyPageCompiledScriptSource scriptSource) {
        reloadedPrecompiledGspClassNames.add(scriptSource.getCompiledClass().getName());
        if (scriptSource.getURI() != null && precompiledGspMap != null) {
            precompiledGspMap.remove(scriptSource.getURI());
        }
    }

    public GroovyPageScriptSource findPageInBinding(String uri, GroovyPageBinding binding) {
         GroovyPageScriptSource scriptSource = findResourceScriptSource(uri);

        if (scriptSource == null) {
            GrailsPlugin pagePlugin = binding.getPagePlugin();
            if (pagePlugin instanceof BinaryGrailsPlugin) {
                BinaryGrailsPlugin binaryPlugin = (BinaryGrailsPlugin) pagePlugin;
                scriptSource = resolveViewInBinaryPlugin(binaryPlugin, uri);
            }
            else if (pagePlugin != null) {
                scriptSource = findResourceScriptSource(resolvePluginViewPath(uri, pagePlugin));
            }
        }

        if (scriptSource == null) {
            scriptSource = findBinaryScriptSource(uri);
        }

        return scriptSource;
    }

    protected GroovyPageScriptSource resolveViewInBinaryPlugin(BinaryGrailsPlugin binaryPlugin, String uri) {
        GroovyPageScriptSource scriptSource = null;
        uri = removeViewLocationPrefixes(uri);
        uri = GrailsResourceUtils.appendPiecesForUri(PATH_TO_WEB_INF_VIEWS, uri);
        Class<?> viewClass = binaryPlugin.resolveView(uri);
        if (viewClass != null && !reloadedPrecompiledGspClassNames.contains(viewClass.getName())) {
             scriptSource = createGroovyPageCompiledScriptSource(uri, uri, viewClass);
        }
        return scriptSource;
    }

    protected GroovyPageCompiledScriptSource createGroovyPageCompiledScriptSource(final String uri, String fullPath, Class<?> viewClass) {
        GroovyPageCompiledScriptSource scriptSource = new GroovyPageCompiledScriptSource(uri, fullPath,viewClass);
        if (reloadEnabled) {
            scriptSource.setResourceCallable(new PrivilegedAction<Resource>() {
                public Resource run() {
                    return findReloadablePage(uri);
                }
            });
        }
        return scriptSource;
    }

    protected GroovyPageScriptSource findBinaryScriptSource(String uri) {
        if (pluginManager == null) {
            return null;
        }

        for (GrailsPlugin plugin : pluginManager.getAllPlugins()) {
            if (!(plugin instanceof BinaryGrailsPlugin)) {
                continue;
            }
            GroovyPageScriptSource scriptSource = resolveViewInBinaryPlugin((BinaryGrailsPlugin) plugin, uri);
            if (scriptSource != null) {
                return scriptSource;
            }
        }

        return null;
    }

    protected GroovyPageScriptSource findResourceScriptSourceInPlugins(String uri) {
        if (pluginManager == null) {
            return null;
        }

        for (GrailsPlugin plugin : pluginManager.getAllPlugins()) {
            if (plugin instanceof BinaryGrailsPlugin) {
                continue;
            }

            GroovyPageScriptSource scriptSource = findResourceScriptSource(resolvePluginViewPath(uri, plugin));
            if (scriptSource != null) {
                return scriptSource;
            }
        }

        return null;
    }

    protected Resource findResourceInPlugins(String uri) {
        if (pluginManager == null) {
            return null;
        }

        for (GrailsPlugin plugin : pluginManager.getAllPlugins()) {
            if (plugin instanceof BinaryGrailsPlugin) {
                continue;
            }

            Resource resource = findResource(resolvePluginViewPath(uri, plugin));
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

    protected String resolvePluginViewPath(String uri, GrailsPlugin plugin) {
        uri = removeViewLocationPrefixes(uri);
        return GrailsResourceUtils.appendPiecesForUri(plugin.getPluginPath(), GrailsResourceUtils.VIEWS_DIR_PATH, uri);
    }

    protected String removeViewLocationPrefixes(String uri) {
        uri = removePrefix(uri, GrailsResourceUtils.WEB_INF);
        uri = removePrefix(uri, SLASHED_VIEWS_DIR_PATH);
        uri = removePrefix(uri, GrailsResourceUtils.VIEWS_DIR_PATH);
        return uri;
    }

    protected String removePrefix(String uri, String prefix) {
        if (uri.startsWith(prefix)) {
            uri = uri.substring(prefix.length());
        }
        return uri;
    }

    protected GroovyPageScriptSource findResourceScriptSource(final String uri) {
        List<String> searchPaths = resolveSearchPaths(uri);

        return findResourceScriptPathForSearchPaths(uri, searchPaths);
    }

    protected List<String> resolveSearchPaths(String uri) {
        List<String> searchPaths = null;

        uri = removeViewLocationPrefixes(uri);
        if (warDeployed) {
            if (uri.startsWith(PLUGINS_PATH)) {
                PluginViewPathInfo pathInfo = getPluginViewPathInfo(uri);

                searchPaths = CollectionUtils.newList(
                    GrailsResourceUtils.appendPiecesForUri(GrailsResourceUtils.WEB_INF, PLUGINS_PATH, pathInfo.pluginName,GrailsResourceUtils.VIEWS_DIR_PATH, pathInfo.path),
                    GrailsResourceUtils.appendPiecesForUri(GrailsResourceUtils.WEB_INF, uri),
                    uri);
            }
            else {
                searchPaths = CollectionUtils.newList(
                    GrailsResourceUtils.appendPiecesForUri(PATH_TO_WEB_INF_VIEWS, uri),
                    uri);
            }
        }
        else {
            searchPaths = CollectionUtils.newList(
                GrailsResourceUtils.appendPiecesForUri(SLASHED_VIEWS_DIR_PATH, uri),
                GrailsResourceUtils.appendPiecesForUri(PATH_TO_WEB_INF_VIEWS, uri),
                uri);
        }
        return searchPaths;
    }

    @SuppressWarnings("unchecked")
    protected GroovyPageScriptSource findResourceScriptPathForSearchPaths(String uri, List<String> searchPaths) {
        if (isPrecompiledAvailable()) {
            for (String searchPath : searchPaths) {
                String gspClassName = precompiledGspMap.get(searchPath);
                if (gspClassName != null && !reloadedPrecompiledGspClassNames.contains(gspClassName)) {
                    Class<GroovyPage> gspClass = null;
                    try {
                        gspClass = (Class<GroovyPage>)Class.forName(gspClassName, true, Thread.currentThread().getContextClassLoader());
                    }
                    catch (ClassNotFoundException e) {
                        LOG.warn("Cannot load class " + gspClassName + ". Resuming on non-precompiled implementation.", e);
                    }
                    if (gspClass != null) {
                        return createGroovyPageCompiledScriptSource(uri, searchPath, gspClass);
                    }
                }
            }
        }

        Resource foundResource = findResource(searchPaths);
        return foundResource == null ? null : new GroovyPageResourceScriptSource(uri,foundResource);
    }

    protected Resource findResource(String uri) {
        return findResource(resolveSearchPaths(uri));
    }

    protected Resource findResource(List<String> searchPaths) {
        Resource foundResource = null;
        Resource resource;
        for (ResourceLoader loader : resourceLoaders) {
            for (String path : searchPaths) {
                resource = loader.getResource(path);
                if (resource != null && resource.exists()) {
                    foundResource = resource;
                    break;
                }
            }
            if (foundResource != null) break;
        }
        return foundResource;
    }

    private boolean isPrecompiledAvailable() {
        return precompiledGspMap != null && precompiledGspMap.size() > 0 && warDeployed;
    }

    public void setServletContext(ServletContext servletContext) {
        addResourceLoader(new ServletContextResourceLoader(servletContext));
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        addResourceLoader(applicationContext);
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public static PluginViewPathInfo getPluginViewPathInfo(String uri) {
        return new PluginViewPathInfo(uri);
    }

    public static class PluginViewPathInfo {
        public String basePath;
        public String pluginName;
        public String path;

        public PluginViewPathInfo(String uri) {
            basePath = uri.substring(PLUGINS_PATH.length(), uri.length());
            int i = basePath.indexOf("/");
            if (i > -1) {
                pluginName = basePath.substring(0,i);
                path = basePath.substring(i, basePath.length());
            }
        }
    }

    public boolean isReloadEnabled() {
        return reloadEnabled;
    }

    public void setReloadEnabled(boolean reloadEnabled) {
        this.reloadEnabled = reloadEnabled;
    }
}
