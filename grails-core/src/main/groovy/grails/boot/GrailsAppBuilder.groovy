package grails.boot

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.springframework.boot.SpringApplication
import org.springframework.boot.builder.SpringApplicationBuilder

/**
 * Fluent API for constructing GrailsApp instances. Simple extension of {@link SpringApplicationBuilder}.
 *
 * @author Graeme Rocher
 * @since 3.0.6
 */
@CompileStatic
@InheritConstructors
class GrailsAppBuilder extends SpringApplicationBuilder {

    @Override
    protected SpringApplication createSpringApplication(Class<?>... sources) {
        return new GrailsApp(sources)
    }
}
