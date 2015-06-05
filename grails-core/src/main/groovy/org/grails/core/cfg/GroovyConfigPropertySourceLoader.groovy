/*
 * Copyright 2014 original authors
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
package org.grails.core.cfg

import grails.util.BuildSettings
import grails.util.Environment
import grails.util.Metadata
import groovy.transform.CompileStatic
import groovy.util.logging.Commons
import org.springframework.boot.env.PropertySourceLoader
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.Resource

/**
 * Adds support for defining a 'application.groovy' file in ConfigSlurper format in order to configure Spring Boot within Grails
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@Commons
class GroovyConfigPropertySourceLoader implements PropertySourceLoader {

    final String[] fileExtensions = ['groovy'] as String[]

    @Override
    PropertySource<?> load(String name, Resource resource, String profile) throws IOException {
        def env = Environment.current.name
        ConfigSlurper configSlurper = env ? new ConfigSlurper(env) : new ConfigSlurper()

        configSlurper.setBinding(userHome: System.getProperty('user.home'),
                                 grailsHome: BuildSettings.GRAILS_HOME?.absolutePath,
                                 springProfile: profile,
                                 appName: Metadata.getCurrent().getApplicationName(),
                                 appVersion: Metadata.getCurrent().getApplicationVersion() )

        if(resource.exists()) {
            try {
                def configObject = configSlurper.parse(resource.URL)
                def flatMap = configObject.flatten()
                Map<String, Object> finalMap = [:]
                finalMap.putAll(configObject)
                finalMap.putAll(flatMap)
                return new MapPropertySource(name, finalMap)
            } catch (Throwable e) {
                log.error("Unable to load $resource.filename: $e.message", e)
            }
        }
        return null
    }
}
