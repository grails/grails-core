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

import javax.servlet.ServletContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Used to locate GSPs whether in development or WAR deployed mode from static resources, custom resource loaders and binary plugins
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class DefaultGroovyPageLocator implements GroovyPageLocator, ServletContextAware, ApplicationContextAware, PluginManagerAware {

    private static final Log LOG = LogFactory.getLog(DefaultGroovyPageLocator.class);
    private static final String PATH_TO_WEB_INF_VIEWS = GrailsApplicationAttributes.PATH_TO_VIEWS;
    private static final String BLANK = "";
    protected Collection<ResourceLoader> resourceLoaders = new ConcurrentLinkedQueue<ResourceLoader>();
    protected GrailsPluginManager pluginManager;
    private Map<String, String> precompiledGspMap;

    public void setResourceLoader(ResourceLoader resourceLoader) {
        addResourceLoader(resourceLoader);
    }

    public void addResourceLoader(ResourceLoader resourceLoader) {
        if(resourceLoader != null) {
            if(!resourceLoaders.contains(resourceLoader))
                resourceLoaders.add(resourceLoader);
        }
    }

    public void setPrecompiledGspMap(Map<String, String> precompiledGspMap) {
        this.precompiledGspMap = precompiledGspMap;
    }

    public GroovyPageScriptSource findPage(final String uri) {
        GroovyPageScriptSource scriptSource = findResourceScriptSource(uri);
        if(scriptSource == null) {
            scriptSource = findBinaryScriptSource(uri);
        }
        if(scriptSource == null) {
            scriptSource = findResourceScriptSourceInPlugins(uri);
        }
        return scriptSource;
    }

    public GroovyPageScriptSource findPageInBinding(String pluginName, String uri, GroovyPageBinding binding) {
        String contextPath = null;

        if (pluginName != null && pluginManager != null) {
            contextPath = pluginManager.getPluginPath(pluginName);
        }
        else if (contextPath == null) {
            if (uri.startsWith("/plugins/")) {
                contextPath = BLANK;
            }
            else {
                contextPath = binding.getPluginContextPath() != null ? binding.getPluginContextPath() : "";
            }
        }

        GroovyPageScriptSource scriptSource = findPageInBinding(GrailsResourceUtils.appendPiecesForUri(contextPath, uri), binding);
        if(scriptSource == null) {
            scriptSource = findResourceScriptSource(uri);
        }
        return scriptSource;
    }

    public void removePrecompiledPage(String uri) {
        if(uri != null)
            precompiledGspMap.remove(uri);
    }

    public GroovyPageScriptSource findPageInBinding(String uri, GroovyPageBinding binding) {
        GroovyPageScriptSource scriptSource = findResourceScriptSource(uri);

        if(scriptSource == null) {
            GrailsPlugin pagePlugin = binding.getPagePlugin();
            if (pagePlugin instanceof BinaryGrailsPlugin) {
                BinaryGrailsPlugin binaryPlugin = (BinaryGrailsPlugin) pagePlugin;
                String binaryView = GrailsResourceUtils.appendPiecesForUri(PATH_TO_WEB_INF_VIEWS, uri);
                Class<?> viewClass = binaryPlugin.resolveView(binaryView);
                if (viewClass != null) {
                     scriptSource = new GroovyPageCompiledScriptSource(uri, viewClass);
                }
            }
            else if (pagePlugin != null) {
                String pluginPath = pluginManager != null ? pluginManager.getPluginPath(pagePlugin.getName()) : null;
                if (pluginPath != null) {
                    uri = GrailsResourceUtils.appendPiecesForUri(pluginPath, "/grails-app/views", uri);
                    scriptSource = findResourceScriptSource(uri);
                }
            }
        }

        if(scriptSource == null) {
            scriptSource = findBinaryScriptSource(uri);
        }

        return scriptSource;
    }

    protected GroovyPageScriptSource findBinaryScriptSource(String uri) {
        uri = GrailsResourceUtils.appendPiecesForUri(PATH_TO_WEB_INF_VIEWS, uri);
        if(pluginManager != null) {
            GrailsPlugin[] allPlugins = pluginManager.getAllPlugins();
            for (GrailsPlugin plugin : allPlugins) {
                if(plugin instanceof BinaryGrailsPlugin) {
                    BinaryGrailsPlugin binaryPlugin = (BinaryGrailsPlugin) plugin;
                    Class<?> viewClass = binaryPlugin.resolveView(uri);
                    if(viewClass != null) {
                        return new GroovyPageCompiledScriptSource(uri, viewClass);
                    }
                }
            }
        }
        return null;
    }

    protected GroovyPageScriptSource findResourceScriptSourceInPlugins(String uri) {
        if(pluginManager != null) {
            GrailsPlugin[] allPlugins = pluginManager.getAllPlugins();
            for (GrailsPlugin plugin : allPlugins) {
                if(!(plugin instanceof BinaryGrailsPlugin)) {
                    GroovyPageScriptSource scriptSource = findResourceScriptSource(
                            GrailsResourceUtils.appendPiecesForUri("/plugins/", plugin.getFileSystemName(),
                                    GrailsResourceUtils.VIEWS_DIR_PATH, uri));
                    if(scriptSource != null) {
                        return scriptSource;
                    }
                }
            }
        }
        return null;
    }

    protected GroovyPageScriptSource findResourceScriptSource(final String uri) {
        List<String> searchPaths = null;

        if(Environment.isWarDeployed()) {
            if(uri.startsWith("/plugins")) {
                searchPaths = CollectionUtils.newList(
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
                GrailsResourceUtils.appendPiecesForUri('/' + GrailsResourceUtils.VIEWS_DIR_PATH, uri),
                uri);
        }

        return findResourceScriptPathForSearchPaths(uri, searchPaths);
    }

    @SuppressWarnings("unchecked")
    protected GroovyPageScriptSource findResourceScriptPathForSearchPaths(String uri, List<String> searchPaths) {
        if(isPrecompiledAvailable()) {
            for (String searchPath : searchPaths) {
                String gspClassName = precompiledGspMap.get(searchPath);
                if (gspClassName != null) {
                    Class<GroovyPage> gspClass = null;
                    try {
                        gspClass = (Class<GroovyPage>)Class.forName(gspClassName, true, Thread.currentThread().getContextClassLoader());
                    }
                    catch (ClassNotFoundException e) {
                        LOG.warn("Cannot load class " + gspClassName + ". Resuming on non-precompiled implementation.", e);
                    }
                    if (gspClass != null) {
                        return  new GroovyPageCompiledScriptSource(uri, gspClass);
                    }
                }
            }
        }

        Resource foundResource = null;
        Resource resource;
        for(String path : searchPaths) {
            for(ResourceLoader loader : resourceLoaders) {
                resource = loader.getResource(path);
                if(resource != null && resource.exists()) {
                    foundResource = resource;
                    break;
                }
            }
            if(foundResource != null) break;
        }

        return foundResource != null ? new GroovyPageResourceScriptSource(uri,foundResource) : null;
    }

    private boolean isPrecompiledAvailable() {
        return precompiledGspMap != null && precompiledGspMap.size() > 0 && Environment.isWarDeployed();
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
}
