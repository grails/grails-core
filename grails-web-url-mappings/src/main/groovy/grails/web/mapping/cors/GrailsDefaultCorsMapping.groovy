package grails.web.mapping.cors

import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.cors.CorsConfiguration

/**
 * A class that stores default CORS settings
 *
 * @author James Kleeh
 * @since 3.2.1
 */
class GrailsDefaultCorsMapping {

    List<String> allowedOrigins = CrossOrigin.DEFAULT_ORIGINS
    List<String> allowedMethods = ["*"]
    List<String> allowedHeaders = CrossOrigin.DEFAULT_ALLOWED_HEADERS
    List<String> exposedHeaders = null
    Long maxAge = CrossOrigin.DEFAULT_MAX_AGE
    Boolean allowCredentials = CrossOrigin.DEFAULT_ALLOW_CREDENTIALS

    CorsConfiguration toSpringConfig() {
        CorsConfiguration defaultCorsConfiguration = new CorsConfiguration()
        defaultCorsConfiguration.allowedOrigins = allowedOrigins
        defaultCorsConfiguration.allowedMethods = allowedMethods
        defaultCorsConfiguration.allowedHeaders = allowedHeaders
        defaultCorsConfiguration.exposedHeaders = exposedHeaders
        defaultCorsConfiguration.maxAge = maxAge
        defaultCorsConfiguration.allowCredentials = allowCredentials
        defaultCorsConfiguration
    }
}
