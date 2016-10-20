package grails.web.mapping.cors

import org.springframework.core.Ordered
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

/**
 * Created by jameskleeh on 10/20/16.
 */
class GrailsCorsFilter extends CorsFilter implements Ordered {

    public GrailsCorsFilter(GrailsCorsConfiguration corsConfiguration) {
        super(configurationSource(corsConfiguration.corsConfigurations))
    }

    private static UrlBasedCorsConfigurationSource configurationSource(Map<String, CorsConfiguration> corsConfigurations) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource()
        corsConfigurations.each { String key, CorsConfiguration config ->
            source.registerCorsConfiguration(key, config)
        }
        source
    }

    @Override
    int getOrder() {
        HIGHEST_PRECEDENCE
    }
}
