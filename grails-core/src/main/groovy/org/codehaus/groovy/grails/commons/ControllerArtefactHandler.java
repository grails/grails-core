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
package org.codehaus.groovy.grails.commons;

import grails.util.Environment;

import java.util.Map;

import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.springframework.context.ApplicationContext;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

/**
 * Lookup controllers for uris.
 *
 * <p>This class is responsible for looking up controller classes for uris.</p>
 *
 * <p>Lookups are cached in non-development mode, and the cache size can be controlled using the grails.urlmapping.cache.maxsize config property.</p>
 *
 * @author Marc Palmer (marc@anyware.co.uk)
*/
public class ControllerArtefactHandler extends ArtefactHandlerAdapter implements GrailsApplicationAware {

    private static final String URL_MAPPING_CACHE_MAX_SIZE = "grails.urlmapping.cache.maxsize";
    private static final GrailsClass NO_CONTROLLER = new AbstractGrailsClass(Object.class, "Controller") {};

    public static final String TYPE = "Controller";
    public static final String PLUGIN_NAME = "controllers";
    private ConcurrentLinkedHashMap<ControllerCacheKey, GrailsClass> uriToControllerClassCache;
    private ArtefactInfo artefactInfo;

    private GrailsApplication grailsApplication;

    public ControllerArtefactHandler() {
        super(TYPE, GrailsControllerClass.class, DefaultGrailsControllerClass.class,
                DefaultGrailsControllerClass.CONTROLLER, false);
    }

    @Override
    public void initialize(ArtefactInfo artefacts) {
        Object cacheSize = grailsApplication.getFlatConfig().get(URL_MAPPING_CACHE_MAX_SIZE);
        if (cacheSize == null) {
            cacheSize = 10000;
        }

        uriToControllerClassCache = new ConcurrentLinkedHashMap.Builder<ControllerCacheKey, GrailsClass>()
                .initialCapacity(500)
                .maximumWeightedCapacity(new Integer(cacheSize.toString()))
                .build();

        artefactInfo = artefacts;
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public GrailsClass getArtefactForFeature(Object featureId) {
        if (artefactInfo == null) {
            return null;
        }

        String uri;
        String pluginName = null;
        String namespace = null;

        ControllerCacheKey cacheKey;
        if (featureId instanceof ControllerCacheKey) {
            cacheKey = (ControllerCacheKey)featureId;
            pluginName = cacheKey.plugin;
            namespace = cacheKey.namespace;
            uri = cacheKey.uri;
        } else {
            uri = featureId.toString();
            cacheKey = new ControllerCacheKey(uri, null,null);
        }

//        String cacheKey = (namespace != null ? namespace : "") + ":" + (pluginName != null ? pluginName : "") + ":" + uri;

        GrailsClass controllerClass = uriToControllerClassCache.get(cacheKey);
        if (controllerClass == null) {
            final ApplicationContext mainContext = grailsApplication.getMainContext();
            GrailsPluginManager grailsPluginManager = null;
            if (mainContext.containsBean(GrailsPluginManager.BEAN_NAME)) {
                final Object pluginManagerBean = mainContext.getBean(GrailsPluginManager.BEAN_NAME);
                if (pluginManagerBean instanceof GrailsPluginManager) {
                    grailsPluginManager = (GrailsPluginManager) pluginManagerBean;
                }
            }
            final GrailsClass[] controllerClasses = artefactInfo.getGrailsClasses();
            // iterate in reverse in order to pick up application classes first
            for (int i = (controllerClasses.length-1); i >= 0; i--) {
                GrailsClass c = controllerClasses[i];
                if (((GrailsControllerClass) c).mapsToURI(uri)) {
                    boolean pluginMatches = false;
                    boolean namespaceMatches = false;

                    namespaceMatches = namespaceMatches((GrailsControllerClass)c, namespace);

                    if (namespaceMatches) {
                        pluginMatches = pluginMatches(c, pluginName, grailsPluginManager);
                    }

                    boolean foundController = pluginMatches && namespaceMatches;
                    if (foundController) {
                        controllerClass = c;
                        break;
                    }
                }
            }
            if (controllerClass == null) {
                controllerClass = NO_CONTROLLER;
            }

            // don't cache for dev environment
            if (Environment.getCurrent() != Environment.DEVELOPMENT) {
                uriToControllerClassCache.put(cacheKey, controllerClass);
            }
        }

        if (controllerClass == NO_CONTROLLER) {
            controllerClass = null;
        }
        return controllerClass;
    }

    /**
     * @param c the class to inspect
     * @param namespace a controller namespace
     * @return true if c is in namespace
     */
    protected boolean namespaceMatches(GrailsControllerClass c, String namespace) {
        boolean namespaceMatches;
        if (namespace != null) {
            namespaceMatches = namespace.equals(c.getNamespace());
        } else {
            namespaceMatches = (c.getNamespace() == null);
        }
        return namespaceMatches;
    }

    /**
     *
     * @param c the class to inspect
     * @param pluginName the name of a plugin
     * @param grailsPluginManager the plugin manager
     * @return true if c is provided by a plugin with the name pluginName or if pluginName is null, otherwise false
     */
    protected boolean pluginMatches(GrailsClass c, String pluginName, GrailsPluginManager grailsPluginManager) {
        boolean pluginMatches = false;
        if (pluginName != null && grailsPluginManager != null) {
            final GrailsPlugin pluginForClass = grailsPluginManager.getPluginForClass(c.getClazz());
            if (pluginForClass != null && pluginName.equals(pluginForClass.getName())) {
                pluginMatches = true;
            }
        } else {
            pluginMatches = true;
        }
        return pluginMatches;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    public static class ControllerCacheKey {
        private String uri;
        private String plugin;
        private String namespace;

        public ControllerCacheKey(String uri, String plugin, String namespace) {
            this.uri = uri;
            this.plugin = plugin;
            this.namespace = namespace;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ControllerCacheKey that = (ControllerCacheKey) o;

            if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) {
                return false;
            }
            if (plugin != null ? !plugin.equals(that.plugin) : that.plugin != null) {
                return false;
            }
            if (!uri.equals(that.uri)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = uri.hashCode();
            result = 31 * result + (plugin != null ? plugin.hashCode() : 0);
            result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
            return result;
        }
    }
}
