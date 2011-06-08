/* Copyright 2006-2007 Graeme Rocher
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
import grails.util.Metadata
import java.util.concurrent.ConcurrentHashMap
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.ConfigurationHolder
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

    private static final LOG = LogFactory.getLog(ConfigurationHelper)

    private static final String CONFIG_BINDING_USER_HOME = "userHome"
    private static final String CONFIG_BINDING_GRAILS_HOME = "grailsHome"
    private static final String CONFIG_BINDING_APP_NAME = "appName"
    private static final String CONFIG_BINDING_APP_VERSION = "appVersion"

    private static Map<Integer, ConfigObject> cachedConfigs = new ConcurrentHashMap<Integer, ConfigObject>();

    static ConfigObject loadConfigFromClasspath(String environment) {
        loadConfigFromClasspath(null, environment)
    }

    static void clearCachedConfigs() {
        cachedConfigs.clear()
    }

    static ConfigObject loadConfigFromClasspath(GrailsApplication application = null, String environment = Environment.current.name) {


        ConfigObject co
        ClassLoader classLoader
        Integer cacheKey = -1

        if (application != null) {
            classLoader = application.getClassLoader()
            if (Environment.isWarDeployed() || !Environment.isWithinShell()) {
                // use unique cache keys for each config based on the application instance
                // this to ensure each application gets a unique config and avoid the scenario
                // where applications deployed in a shared library mode (shared jars) share the
                // same config
                cacheKey = System.identityHashCode(application)
            }
        }
        else {
            classLoader = Thread.currentThread().contextClassLoader
        }

        co = cachedConfigs.get(cacheKey)
        if (co == null) {
            ConfigSlurper configSlurper = getConfigSlurper(environment, application)
            try {
                try {
                    application?.config = new ConfigObject() // set empty config to avoid stack overflow
                    Class scriptClass = classLoader.loadClass(GrailsApplication.CONFIG_CLASS)
                    co = configSlurper.parse(scriptClass)
                    application?.config = co
                }
                catch (ClassNotFoundException e) {
                    LOG.debug "Could not find config class [" + GrailsApplication.CONFIG_CLASS + "]. This is probably " +
                            "nothing to worry about, it is not required to have a config: $e.message"
                    // ignore, it is ok not to have a configuration file
                    co = new ConfigObject()
                }
                try {
                    Class dataSourceClass = classLoader.loadClass(GrailsApplication.DATA_SOURCE_CLASS)
                    co.merge(configSlurper.parse(dataSourceClass))
                }
                catch (ClassNotFoundException e) {
                    LOG.debug "Cound not find data source class [" + GrailsApplication.DATA_SOURCE_CLASS + "]. This may " +
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
            cachedConfigs.put(cacheKey, co)
            ConfigurationHolder.config = co
        }

        return co
    }

    static ConfigSlurper getConfigSlurper(String environment, GrailsApplication application) {
        ConfigSlurper configSlurper = new ConfigSlurper(environment)
        Map binding = new HashMap()

        // configure config slurper binding
        binding.put(CONFIG_BINDING_USER_HOME, System.getProperty("user.home"))
        binding.put(CONFIG_BINDING_GRAILS_HOME, System.getProperty("grails.home"))

        if (application) {
            binding.put(CONFIG_BINDING_APP_NAME, application.getMetadata().get(Metadata.APPLICATION_NAME))
            binding.put(CONFIG_BINDING_APP_VERSION, application.getMetadata().get(Metadata.APPLICATION_VERSION))
            binding.put(GrailsApplication.APPLICATION_ID, application);
        }


        configSlurper.setBinding(binding)
        return configSlurper
    }

    /**
     * Loads external configuration and merges with ConfigObject
     */
    static void initConfig(ConfigObject config, ResourceLoader resourceLoader = null, ClassLoader classLoader = null) {

        def resolver = resourceLoader ?
                new PathMatchingResourcePatternResolver(resourceLoader) :
                new PathMatchingResourcePatternResolver()

        // Get these now before we do any merging
        def defaultsLocations = config.grails.config.defaults.locations
        def locations = config.grails.config.locations

        // We load defaults in a way that allows them to be overridden by the main config
        if (isLocations(defaultsLocations)) {
            def newConfigObject = new ConfigObject()
            mergeInLocations(newConfigObject, defaultsLocations, resolver, classLoader)
            newConfigObject.merge(config)
            config.merge(newConfigObject)
        }

        // We load non-defaults in a way that overrides the main config
        if (isLocations(locations)) {
            mergeInLocations(config, locations, resolver, classLoader)
        }
    }


    static private void mergeInLocations(ConfigObject config, List locations, PathMatchingResourcePatternResolver resolver, ClassLoader classLoader) {
        for (location in locations) {
            if (!location) {
                continue
            }

            try {
                ConfigSlurper configSlurper = new ConfigSlurper(Environment.current.name)
                configSlurper.setBinding(config)
                if (classLoader) {
                    if (classLoader instanceof GroovyClassLoader) {
                        configSlurper.classLoader = classLoader
                    }
                    else {
                        configSlurper.classLoader = new GroovyClassLoader(classLoader)
                    }
                }

                if (location instanceof Class) {
                    def newConfig = configSlurper.parse(location)
                    config.merge(newConfig)
                }
                else {
                    def resource = resolver.getResource(location.toString())
                    def stream
                    try {
                        stream = resource.getInputStream()
                        if (resource.filename.endsWith('.groovy')) {
                            def newConfig = configSlurper.parse(stream.text)
                            config.merge(newConfig)
                        }
                        else if (resource.filename.endsWith('.properties')) {
                            def props = new Properties()
                            props.load(stream)
                            def newConfig = configSlurper.parse(props)
                            config.merge(newConfig)
                        }
                        else if (resource.filename.endsWith('.class')) {
                            def configClass = new GroovyClassLoader(configSlurper.classLoader).defineClass(null, stream.bytes)
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
                System.err.println "Unable to load specified config location $location : ${e.message}"
            }
        }
    }

    static private boolean isLocations(locations) {
        locations != null && locations instanceof List
    }
}
