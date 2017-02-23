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
package grails.web.mapping.cors

import grails.util.TypeConvertingMap
import groovy.transform.CompileStatic
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.web.cors.CorsConfiguration

/**
 * A bean that stores config and converts it to the format expected by Spring
 *
 * @author James Kleeh
 * @since 3.2.1
 */
@CompileStatic
@ConfigurationProperties(prefix = 'grails.cors')
class GrailsCorsConfiguration {

    Boolean enabled = false

    @Delegate
    GrailsDefaultCorsConfiguration grailsCorsMapping = new GrailsDefaultCorsConfiguration()

    Map<String, Object> mappings = [:]

    Map<String, CorsConfiguration> getCorsConfigurations() {
        grailsCorsMapping.applyPermitDefaultValues()
        Map<String, CorsConfiguration> corsConfigurationMap = [:]

        if (enabled) {
            if (mappings.size() > 0) {
                mappings.each { String key, Object value ->
                    GrailsDefaultCorsConfiguration corsConfiguration = new GrailsDefaultCorsConfiguration(grailsCorsMapping)
                    if (value instanceof Map) {
                        TypeConvertingMap config = new TypeConvertingMap((Map)value)
                        if (config.containsKey('allowedOrigins')) {
                            corsConfiguration.allowedOrigins = config.list('allowedOrigins')
                        }
                        if (config.containsKey('allowedMethods')) {
                            corsConfiguration.allowedMethods = config.list('allowedMethods')
                        }
                        if (config.containsKey('allowedHeaders')) {
                            corsConfiguration.allowedHeaders = config.list('allowedHeaders')
                        }
                        if (config.containsKey('exposedHeaders')) {
                            corsConfiguration.exposedHeaders = config.list('exposedHeaders')
                        }
                        if (config.containsKey('maxAge')) {
                            corsConfiguration.maxAge = config.long('maxAge')
                        }
                        if (config.containsKey('allowCredentials')) {
                            corsConfiguration.allowCredentials = config.boolean('allowCredentials')
                        }
                    }
                    corsConfigurationMap[key] = corsConfiguration
                }
            } else {
                corsConfigurationMap["/**"] = grailsCorsMapping
            }
        }

        corsConfigurationMap
    }
}
