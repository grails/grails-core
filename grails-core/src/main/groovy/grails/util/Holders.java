/*
 * Copyright 2011 SpringSource.
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
package grails.util;

import groovy.util.ConfigObject;

import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author Burt Beckwith
 * @since 2.0
 */
public class Holders {

    private static final Log LOG = LogFactory.getLog(Holders.class);
    private static final String APPLICATION_BEAN_NAME = "grailsApplication";

    private static Holder<GrailsResourceLoader> resourceLoaders = new Holder<GrailsResourceLoader>("ResourceLoader");
    private static Holder<GrailsPluginManager> pluginManagers = new Holder<GrailsPluginManager>("PluginManager");
    private static Holder<Boolean> pluginManagersInCreation = new Holder<Boolean>("PluginManagers in creation");
    private static Holder<ConfigObject> configs = new Holder<ConfigObject>("config");
    private static Holder<Map<?, ?>> flatConfigs = new Holder<Map<?, ?>>("flat config");
    private static Holder<ServletContext> servletContexts;
    static {
        createServletContextsHolder();
    }

    private static GrailsApplication applicationSingleton; // TODO remove

    private Holders() {
        // static only
    }

    public static void clear() {
        resourceLoaders.set(null);
        pluginManagers.set(null);
        pluginManagersInCreation.set(null);
        configs.set(null);
        if (servletContexts != null) {
            servletContexts.set(null);
        }
    }

    public static void setServletContext(final ServletContext servletContext) {
        servletContexts.set(servletContext);
    }

    public static ServletContext getServletContext() {
        return get(servletContexts, "servletContext");
    }

    public static ApplicationContext getApplicationContext() {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
    }

    /**
     *
     * @return The ApplicationContext or null if it doesn't exist
     */
    public static ApplicationContext findApplicationContext() {
        ServletContext servletContext = getServletContext();
        if(servletContext != null) {
            return WebApplicationContextUtils.getWebApplicationContext(servletContext);
        }
        return null;
    }

    public static GrailsApplication getGrailsApplication() {
        try {
            return (GrailsApplication)getApplicationContext().getBean(APPLICATION_BEAN_NAME);
        }
        catch (IllegalStateException e) {
            return applicationSingleton;
        }
        catch (IllegalArgumentException e) {
            return applicationSingleton;
        }
    }

    public static void setGrailsApplication(GrailsApplication application) {
        applicationSingleton = application;
    }

    public static void setConfig(ConfigObject config) {
        configs.set(config);

        // reset flat config
        flatConfigs.set(config == null ? null : config.flatten());
    }

    public static ConfigObject getConfig() {
        return get(configs, "config");
    }

    public static Map<?, ?> getFlatConfig() {
        Map<?, ?> flatConfig = get(flatConfigs, "flatConfig");
        return flatConfig == null ? Collections.emptyMap() : flatConfig;
    }

    public static GrailsResourceLoader getResourceLoader() {
        return getResourceLoader(false);
    }

    public static GrailsResourceLoader getResourceLoader(boolean mappedOnly) {
        return get(resourceLoaders, "resourceLoader", mappedOnly);
    }

    public static void setResourceLoader(GrailsResourceLoader resourceLoader) {
        resourceLoaders.set(resourceLoader);
    }

    public static void setPluginManagerInCreation(boolean inCreation) {
        pluginManagersInCreation.set(inCreation);
    }

    public static void setPluginManager(GrailsPluginManager pluginManager) {
        if (pluginManager != null) {
            pluginManagersInCreation.set(false);
        }
        pluginManagers.set(pluginManager);
    }

    public static GrailsPluginManager getPluginManager() {
        return getPluginManager(false);
    }

    public static GrailsPluginManager getPluginManager(boolean mappedOnly) {
        while (true) {
            Boolean inCreation = get(pluginManagersInCreation, "PluginManager in creation", mappedOnly);
            if (inCreation == null) {
                inCreation = false;
            }
            if (!inCreation) {
                break;
            }

            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                break;
            }
        }

        return get(pluginManagers, "PluginManager", mappedOnly);
    }

    public static GrailsPluginManager currentPluginManager() {
        GrailsPluginManager current = getPluginManager();
        Assert.notNull(current, "No PluginManager set");
        return current;
    }

    public static void reset() {
        setPluginManager(null);
        setGrailsApplication(null);
        setServletContext(null);
        setResourceLoader(null);
        setPluginManager(null);
        setPluginManagerInCreation(false);
    }

    private static <T> T get(Holder<T> holder, String type) {
        return get(holder, type, false);
    }

    private static <T> T get(Holder<T> holder, String type, boolean mappedOnly) {
        return holder.get(mappedOnly);
    }

    @SuppressWarnings("unchecked")
    private static void createServletContextsHolder() {
        try {
            Class<?> clazz = Holders.class.getClassLoader().loadClass("org.codehaus.groovy.grails.web.context.WebRequestServletHolder");
            servletContexts = (Holder<ServletContext>)clazz.newInstance();
        }
        catch (ClassNotFoundException e) {
            // shouldn't happen
            LOG.error("Error initializing servlet context holder", e);
        }
        catch (InstantiationException e) {
            // shouldn't happen
            LOG.error("Error initializing servlet context holder", e);
        }
        catch (IllegalAccessException e) {
            // shouldn't happen
            LOG.error("Error initializing servlet context holder", e);
        }
    }
}
