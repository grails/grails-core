/*
 * Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.commons.cfg

import grails.util.Environment
import grails.util.Holder
import grails.util.Metadata
import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

/**
 * Helper methods for initialising config object.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class ConfigurationHelper {

    private static final Log LOG = LogFactory.getLog(this)

    private static final String CONFIG_BINDING_USER_HOME = "userHome"
    private static final String CONFIG_BINDING_GRAILS_HOME = "grailsHome"
    private static final String CONFIG_BINDING_APP_NAME = "appName"
    private static final String CONFIG_BINDING_APP_VERSION = "appVersion"

    private static Holder<Map<Integer, ConfigObject>> cachedConfigs = new Holder<Map<Integer, ConfigObject>>('cachedConfigs')
    public static final int DEV_CACHE_KEY = -1

    @CompileStatic
    static ConfigObject loadConfigFromClasspath(String environment) {
        loadConfigFromClasspath(null, environment)
    }

    @CompileStatic
    static void clearCachedConfigs() {
        getCachedConfigs().clear()
    }

    @CompileStatic
    static ConfigObject loadConfigFromClasspath(DefaultGrailsApplication application = null,
            String environment = Environment.current.name) {

        ConfigObject co
        ClassLoader classLoader
        Integer cacheKey = DEV_CACHE_KEY

        if (application == null) {
            classLoader = Thread.currentThread().contextClassLoader
        }
        else {
            classLoader = application.getClassLoader()
            if (Environment.isWarDeployed() || !Environment.isWithinShell()) {
                // use unique cache keys for each config based on the application instance
                // to ensure each application gets a unique config and avoid the scenario
                // where applications deployed in a shared library mode (shared jars) share the
                // same config
                cacheKey = System.identityHashCode(application)
            }
        }

        co = getCachedConfigs().get(cacheKey)
        if (co == null) {
            ConfigSlurper configSlurper = getConfigSlurper(environment, application)
            try {
                try {
                    application?.config = new ConfigObject() // set empty config to avoid stack overflow
                    co = configSlurper.parse(classLoader.loadClass(GrailsApplication.CONFIG_CLASS))
                    application?.config = co
                }
                catch (ClassNotFoundException e) {
                    LOG.debug "Could not find config class [$GrailsApplication.CONFIG_CLASS]. This is probably " +
                            "nothing to worry about; it is not required to have a config: $e.message"
                    // ignore, it is ok not to have a configuration file
                    co = new ConfigObject()
                }
                try {
                    co.merge(configSlurper.parse(classLoader.loadClass(GrailsApplication.DATA_SOURCE_CLASS)))
                }
                catch (ClassNotFoundException e) {
                    LOG.debug "Cound not find data source class [$GrailsApplication.DATA_SOURCE_CLASS]. This may " +
                        "be what you are expecting, but will result in Grails loading with an in-memory database"
                    // ignore
                }
            }
            catch (Throwable t) {
                LOG.error("Error loading application Config: $t.message", t)
                throw t
            }

            if (co == null) co = new ConfigObject()

            initConfig(co, null, classLoader)
            getCachedConfigs().put(cacheKey, co)
            ConfigurationHolder.config = co
        }

        return co
    }

    @CompileStatic
    static ConfigSlurper getConfigSlurper(String environment, GrailsApplication application) {
        ConfigSlurper configSlurper = new ConfigSlurper(environment)
        Map binding = new HashMap()

        // configure config slurper binding
        binding.put(CONFIG_BINDING_USER_HOME, System.getProperty("user.home"))
        binding.put(CONFIG_BINDING_GRAILS_HOME, System.getProperty("grails.home"))

        if (application) {
            binding.put(CONFIG_BINDING_APP_NAME, application.getMetadata().get(Metadata.APPLICATION_NAME))
            binding.put(CONFIG_BINDING_APP_VERSION, application.getMetadata().get(Metadata.APPLICATION_VERSION))
            binding.put(GrailsApplication.APPLICATION_ID, application)
        }

        configSlurper.setBinding(binding)
        return configSlurper
    }

    /**
     * Loads external configuration and merges with ConfigObject
     */
    @CompileStatic
    static void initConfig(ConfigObject config, ResourceLoader resourceLoader = null, ClassLoader classLoader = null) {

        if (Environment.isWithinShell()) {
            getCachedConfigs().put(DEV_CACHE_KEY, config)
        }
        def resolver = resourceLoader ?
                new PathMatchingResourcePatternResolver(resourceLoader) :
                new PathMatchingResourcePatternResolver()

        // Get these now before we do any merging
        def defaultsLocations = getDefaultLocations(config)
        def locations = getLocations(config)

        // We load defaults in a way that allows them to be overridden by the main config
        if (isLocations(defaultsLocations)) {
            def newConfigObject = new ConfigObject()
            mergeInLocations(newConfigObject, (List)defaultsLocations, resolver, classLoader)
            newConfigObject.merge(config)
            config.merge(newConfigObject)
        }

        // We load non-defaults in a way that overrides the main config
        if (isLocations(locations)) {
            mergeInLocations(config, (List)locations, resolver, classLoader)
        }
    }

    private static getLocations(ConfigObject config) {
        config.grails.config.locations
    }

    private static getDefaultLocations(ConfigObject config) {
        config.grails.config.defaults.locations
    }

    @CompileStatic
    private static void mergeInLocations(ConfigObject config, List locations, PathMatchingResourcePatternResolver resolver, ClassLoader classLoader) {
        for (location in locations) {
            if (!location) {
                continue
            }

            try {
                ConfigSlurper configSlurper = new ConfigSlurper(Environment.current.name)
                configSlurper.setBinding(config)
                if (classLoader) {
                    if (classLoader instanceof GroovyClassLoader) {
                        configSlurper.classLoader = (GroovyClassLoader)classLoader
                    }
                    else {
                        configSlurper.classLoader = new GroovyClassLoader(classLoader)
                    }
                }

                if (location instanceof Class) {
                    ConfigObject newConfig = configSlurper.parse((Class)location)
                    config.merge(newConfig)
                }
                else {
                    def resource = resolver.getResource(location.toString())
                    InputStream stream = null
                    try {
                        stream = resource.getInputStream()
                        if (resource.filename.endsWith('.groovy')) {
                            def newConfig = configSlurper.parse(stream.getText())
                            config.merge(newConfig)
                        }
                        else if (resource.filename.endsWith('.properties')) {
                            def props = new Properties()
                            props.load(stream)
                            def newConfig = configSlurper.parse(props)
                            config.merge(newConfig)
                        }
                        else if (resource.filename.endsWith('.class')) {
                            def configClass = new GroovyClassLoader(configSlurper.classLoader).defineClass( (String)null, stream.getBytes())
                            def newConfig = configSlurper.parse(configClass)
                            config.merge(newConfig)
                        }
                    }
                    finally {
                        stream?.close()
                    }
                }
            }
            catch (Exception e) {
                LOG.warn "Unable to load specified config location $location : ${e.message}"
            }
        }
    }

    @CompileStatic
    private static boolean isLocations(locations) {
        locations instanceof List
    }

    private static Map<Integer, ConfigObject> getCachedConfigs() {
        Map<Integer, ConfigObject> configs = cachedConfigs.get()
        if (configs == null) {
            configs = new ConcurrentHashMap<Integer, ConfigObject>()
            cachedConfigs.set configs
        }
        configs
    }
}
