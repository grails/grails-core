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
    GrailsDefaultCorsMapping grailsCorsMapping = new GrailsDefaultCorsMapping()

    Map<String, TypeConvertingMap> mappings = [:]

    Map<String, CorsConfiguration> getCorsConfigurations() {
        Map<String, CorsConfiguration> corsConfigurationMap = [:]

        if (enabled) {
            if (mappings.size() > 0) {
                mappings.each { String key, TypeConvertingMap config ->
                    CorsConfiguration corsConfiguration = grailsCorsMapping.toSpringConfig()
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
                    corsConfigurationMap[key] = corsConfiguration
                }
            } else {
                CorsConfiguration defaultConfiguration = grailsCorsMapping.toSpringConfig()
                corsConfigurationMap["/**"] = defaultConfiguration
            }
        }

        corsConfigurationMap
    }
}
