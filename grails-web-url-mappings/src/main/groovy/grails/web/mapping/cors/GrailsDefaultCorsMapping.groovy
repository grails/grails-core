package grails.web.mapping.cors

import groovy.transform.CompileStatic
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.cors.CorsConfiguration
import static org.springframework.web.bind.annotation.CrossOrigin.*
/**
 * A class that stores default CORS settings
 *
 * @author James Kleeh
 * @since 3.2.1
 */
@CompileStatic
class GrailsDefaultCorsMapping {

    List<String> allowedOrigins = DEFAULT_ORIGINS.toList()
    List<String> allowedMethods = ["*"]
    List<String> allowedHeaders = DEFAULT_ALLOWED_HEADERS.toList()
    List<String> exposedHeaders = null
    Long maxAge = DEFAULT_MAX_AGE
    Boolean allowCredentials = DEFAULT_ALLOW_CREDENTIALS

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
