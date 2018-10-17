package grails.web.mapping.cors

import grails.util.TypeConvertingMap
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.cors.CorsConfiguration
import spock.lang.Specification

/**
 * Created by Jim on 9/17/2016.
 */
class GrailsCorsConfigurationSpec extends Specification {

    static List<String> DEFAULT_ORIGINS = ["*"]
    static List<String> DEFAULT_ALLOWED_HEADERS = ["*"]
    static List<String> DEFAULT_METHODS = ["*"]
    static List<String> DEFAULT_EXPOSED_HEADERS = null
    static boolean DEFAULT_ALLOW_CREDENTIALS = true
    static long DEFAULT_MAX_AGE = 1800L

    void "test cors is disabled by default"() {
        expect:
        !new GrailsCorsConfiguration().enabled
    }

    void "test default configuration is as expected"() {
        given:
        def config = new GrailsCorsConfiguration().applyPermitDefaultValues()

        expect:
        config.allowedOrigins == DEFAULT_ORIGINS
        config.allowedHeaders == DEFAULT_ALLOWED_HEADERS
        config.allowedMethods == DEFAULT_METHODS
        config.exposedHeaders == DEFAULT_EXPOSED_HEADERS
        config.allowCredentials == null
        config.maxAge == DEFAULT_MAX_AGE
    }

    void "test default path is /**"() {
        given:
        Map<String, CorsConfiguration> config = new GrailsCorsConfiguration(enabled: true).corsConfigurations
        CorsConfiguration defaultConfig = config.get('/**')

        expect:
        config.size() == 1
        config.containsKey("/**")
        defaultConfig.allowedOrigins == DEFAULT_ORIGINS
        defaultConfig.allowedHeaders == DEFAULT_ALLOWED_HEADERS
        defaultConfig.allowedMethods == DEFAULT_METHODS
        defaultConfig.exposedHeaders == DEFAULT_EXPOSED_HEADERS
        defaultConfig.allowCredentials == null
        defaultConfig.maxAge == DEFAULT_MAX_AGE
    }

    void "test config is empty when not enabled"() {
        given:
        Map<String, CorsConfiguration> config = new GrailsCorsConfiguration().corsConfigurations

        expect:
        config.isEmpty()
    }

    Map<String, CorsConfiguration> buildConfig(Map mappings) {
        def grailsCorsConf = new GrailsCorsConfiguration(enabled: true)
        mappings.each {
            it.value = it.value
        }
        grailsCorsConf.mappings = mappings
        grailsCorsConf.corsConfigurations
    }

    void "test additional mappings inherit and override the global config"() {
        given:
        GrailsCorsConfiguration grailsCorsConf
        Map<String, CorsConfiguration> config

        when:
        grailsCorsConf = new GrailsCorsConfiguration(enabled: true)
        grailsCorsConf.allowedMethods = ["GET"]
        grailsCorsConf.allowedOrigins = ["foo"]
        grailsCorsConf.allowedHeaders = ["Content-Type"]
        grailsCorsConf.exposedHeaders = ["Content-Length"]
        grailsCorsConf.allowCredentials = false
        grailsCorsConf.maxAge = 1L
        grailsCorsConf.mappings = ["/foo": [allowedOrigins: ["bar"]]]
        config = grailsCorsConf.corsConfigurations

        then: //Config is inherited from global
        config.size() == 1
        config["/foo"].allowedOrigins == ["bar"] // overridden
        config["/foo"].allowedMethods == ["GET"]
        config["/foo"].allowedHeaders == ["Content-Type"]
        config["/foo"].exposedHeaders == ["Content-Length"]
        config["/foo"].allowCredentials == false
        config["/foo"].maxAge == 1L
    }

    void "test additional mappings"() {
        given:
        Map<String, CorsConfiguration> config


        when:
        config = buildConfig(["/foo": [allowedOrigins: ["bar"]]])

        then: //The global mapping is not created. Provided values override defaults
        config.size() == 1
        config["/foo"].allowedOrigins == ["bar"]
        config["/foo"].allowedMethods == DEFAULT_METHODS
        config["/foo"].allowedHeaders == DEFAULT_ALLOWED_HEADERS
        config["/foo"].exposedHeaders == DEFAULT_EXPOSED_HEADERS
        config["/foo"].allowCredentials == null
        config["/foo"].maxAge == DEFAULT_MAX_AGE

        when:
        config = buildConfig(["/foo": [allowedMethods: ["GET", "PUT"]]])

        then: //The global mapping is not created. Provided values override defaults
        config.size() == 1
        config["/foo"].allowedOrigins == DEFAULT_ORIGINS
        config["/foo"].allowedMethods == ["GET", "PUT"]
        config["/foo"].allowedHeaders == DEFAULT_ALLOWED_HEADERS
        config["/foo"].exposedHeaders == DEFAULT_EXPOSED_HEADERS
        config["/foo"].allowCredentials == null
        config["/foo"].maxAge == DEFAULT_MAX_AGE

        when:
        config = buildConfig(["/foo": [allowedHeaders: ["Content-Type"]]])

        then: //The global mapping is not created. Provided values override defaults
        config.size() == 1
        config["/foo"].allowedOrigins == DEFAULT_ORIGINS
        config["/foo"].allowedMethods == DEFAULT_METHODS
        config["/foo"].allowedHeaders == ["Content-Type"]
        config["/foo"].exposedHeaders == DEFAULT_EXPOSED_HEADERS
        config["/foo"].allowCredentials == null
        config["/foo"].maxAge == DEFAULT_MAX_AGE

        when:
        config = buildConfig(["/foo": [exposedHeaders: ["Content-Type"]]])

        then: //The global mapping is not created. Provided values override defaults
        config.size() == 1
        config["/foo"].allowedOrigins == DEFAULT_ORIGINS
        config["/foo"].allowedMethods == DEFAULT_METHODS
        config["/foo"].allowedHeaders == DEFAULT_ALLOWED_HEADERS
        config["/foo"].exposedHeaders == ["Content-Type"]
        config["/foo"].allowCredentials == null
        config["/foo"].maxAge == DEFAULT_MAX_AGE

        when:
        config = buildConfig(["/foo": [allowCredentials: false]])

        then: //The global mapping is not created. Provided values override defaults
        config.size() == 1
        config["/foo"].allowedOrigins == DEFAULT_ORIGINS
        config["/foo"].allowedMethods == DEFAULT_METHODS
        config["/foo"].allowedHeaders == DEFAULT_ALLOWED_HEADERS
        config["/foo"].exposedHeaders == DEFAULT_EXPOSED_HEADERS
        config["/foo"].allowCredentials == false
        config["/foo"].maxAge == DEFAULT_MAX_AGE

        when:
        config = buildConfig(["/foo": [maxAge: 3600L]])

        then: //The global mapping is not created. Provided values override defaults
        config.size() == 1
        config["/foo"].allowedOrigins == DEFAULT_ORIGINS
        config["/foo"].allowedMethods == DEFAULT_METHODS
        config["/foo"].allowedHeaders == DEFAULT_ALLOWED_HEADERS
        config["/foo"].exposedHeaders == DEFAULT_EXPOSED_HEADERS
        config["/foo"].allowCredentials == null
        config["/foo"].maxAge == 3600L
    }



}
