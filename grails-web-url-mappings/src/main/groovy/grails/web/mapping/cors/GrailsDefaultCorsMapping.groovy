package grails.web.mapping.cors

import org.springframework.http.HttpMethod
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.cors.CorsConfiguration

/**
 * Created by Jim on 9/16/2016.
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

    CorsConfiguration combine(GrailsDefaultCorsMapping mapping) {
        CorsConfiguration configuration = this.toSpringConfig().combine(mapping.toSpringConfig())
        configuration.allowedOrigins = configuration.allowedOrigins?.unique()
        configuration.allowedMethods = configuration.allowedMethods?.unique()
        configuration.allowedHeaders = configuration.allowedHeaders?.unique()
        configuration.exposedHeaders = configuration.exposedHeaders?.unique()
        configuration
    }
}
