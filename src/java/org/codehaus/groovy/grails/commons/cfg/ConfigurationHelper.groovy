
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

import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import grails.util.Metadata;

import org.springframework.core.io.ResourceLoader
import org.apache.commons.logging.*
import grails.util.GrailsUtil
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.apache.log4j.helpers.LogLog

/**
* Helper methods for initialising config object

* @author Graeme Rocher
* @since 1.0
*
* Created: Oct 9, 2007
*/
class ConfigurationHelper {

    private static final LOG = LogFactory.getLog(ConfigurationHelper)

    private static final String CONFIG_BINDING_USER_HOME = "userHome";
    private static final String CONFIG_BINDING_GRAILS_HOME = "grailsHome";
    private static final String CONFIG_BINDING_APP_NAME = "appName";
    private static final String CONFIG_BINDING_APP_VERSION = "appVersion";

    static ConfigObject loadConfigFromClasspath(GrailsApplication application=null) {

        ConfigSlurper configSlurper = new ConfigSlurper(GrailsUtil.getEnvironment())
        Map binding = new HashMap();

        // configure config slurper binding
        binding.put(CONFIG_BINDING_USER_HOME, System.getProperty("user.home"));
        binding.put(CONFIG_BINDING_GRAILS_HOME, System.getProperty("grails.home"));

        if (application) {
            binding.put(CONFIG_BINDING_APP_NAME, application.getMetadata().get(Metadata.APPLICATION_NAME))
            binding.put(CONFIG_BINDING_APP_VERSION, application.getMetadata().get(Metadata.APPLICATION_VERSION))
        };


        configSlurper.setBinding(binding);
        ConfigObject co;
        ClassLoader classLoader = application != null ? application.getClassLoader() : ConfigurationHelper.getClassLoader()
        try {
            try {
                Class scriptClass = classLoader.loadClass(GrailsApplication.CONFIG_CLASS);

                co = configSlurper.parse(scriptClass);
                ConfigurationHolder.setConfig(co)
            } catch (ClassNotFoundException e) {
                LOG.debug("Could not find config class [" + GrailsApplication.CONFIG_CLASS + "]. This is probably nothing to worry about, it is not required to have a config: " + e.getMessage());
                // ignore, it is ok not to have a configuration file
                co = new ConfigObject();
            }
            try {
                Class dataSourceClass = classLoader.loadClass(GrailsApplication.DATA_SOURCE_CLASS);
                co.merge(configSlurper.parse(dataSourceClass));
            } catch (ClassNotFoundException e) {
                LOG.debug("Cound not find data source class [" + GrailsApplication.DATA_SOURCE_CLASS + "]. This may be what you are expecting, but will result in Grails loading with an in-memory database");
                // ignore
            }
        }
        catch (Throwable t) {
            GrailsUtil.deepSanitize(t)
            LOG.error("Error loading application Config: " + t.getMessage(),t)
            throw t
        }


        if (co == null) co = new ConfigObject();

        ConfigurationHolder.setConfig(co)
        initConfig(co, null, classLoader)
        return co
    }
    /**
     * Loads external configuration and merges with ConfigObject
     */
    static void initConfig(ConfigObject config, ResourceLoader resourceLoader = null, ClassLoader classLoader = null) {

        def resolver = resourceLoader ? new PathMatchingResourcePatternResolver(resourceLoader) : new PathMatchingResourcePatternResolver()

        def locations = config.grails.config.locations
        if(locations) {
            for(location in locations) {
                if(location) {
                    try {
                        def resource = resolver.getResource(location)
                        def stream
                        try {
                            stream = resource.getInputStream()
                            ConfigSlurper configSlurper = new ConfigSlurper(GrailsUtil.getEnvironment())
                            configSlurper.setBinding(config) 
                            if(classLoader) {
                                if(classLoader instanceof GroovyClassLoader)
                                    configSlurper.classLoader = classLoader
                                else
                                    configSlurper.classLoader = new GroovyClassLoader(classLoader)                                
                            }
                            if(resource.filename.endsWith('.groovy')) {
                                def newConfig = configSlurper.parse(stream.text)
                                config.merge(newConfig)
                            }
                            else if(resource.filename.endsWith('.properties')) {
                                def props = new Properties()
                                props.load(stream)
                                def newConfig = configSlurper.parse(props)
                                config.merge(newConfig)
                            }
                       } finally {
                          stream?.close()
                       }

                } catch (Exception e) {
                        System.err << "Unable to load specified config location $location : ${e.message}"
                    }
                }
            }
        }

    }

}