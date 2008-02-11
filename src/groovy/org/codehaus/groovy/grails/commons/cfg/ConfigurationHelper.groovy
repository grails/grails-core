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
import org.springframework.core.io.ResourceLoader
import org.apache.commons.logging.*
import grails.util.GrailsUtil

/**
* Helper methods for initialising config object

* @author Graeme Rocher
* @since 1.0
*
* Created: Oct 9, 2007
*/
class ConfigurationHelper {

    private static final LOG = LogFactory.getLog(ConfigurationHelper)
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
                        LOG.warn "Unable to load specified config location $location : ${e.message}"
                        LOG.debug "Unable to load specified config location $location : ${e.message}", e
                    }
                }
            }
        }

    }

}