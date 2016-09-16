package grails.web.mapping.cors

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.web.cors.CorsConfiguration

@ConfigurationProperties(prefix = 'cors')
class GrailsCorsConfiguration {

    Boolean enabled = false

    @Delegate
    GrailsDefaultCorsMapping grailsCorsMapping = new GrailsDefaultCorsMapping()

    Map<String, GrailsDefaultCorsMapping> mappings = [:]

    Map<String, CorsConfiguration> getCorsConfigurations() {
        Map<String, CorsConfiguration> corsConfigurationMap = [:]

        if (enabled) {
            if (mappings.size() > 0) {
                mappings.each { String key, GrailsDefaultCorsMapping value ->
                    corsConfigurationMap[key] = grailsCorsMapping.combine(value)
                }
            } else {
                CorsConfiguration defaultConfiguration = grailsCorsMapping.toSpringConfig()
                corsConfigurationMap["/**"] = defaultConfiguration
            }
        }

        corsConfigurationMap
    }
}
