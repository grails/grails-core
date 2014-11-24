package grails.boot

import grails.artefact.Artefact
import grails.boot.config.GrailsAutoConfiguration
import grails.web.Controller

import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import spock.lang.Specification

/**
 * Created by graemerocher on 28/05/14.
 */
class EmbeddedContainerWithGrailsSpec extends Specification {

    AnnotationConfigEmbeddedWebApplicationContext context

    void cleanup() {
        context.close()
    }

    void "Test that you can load Grails in an embedded server config"() {
        when:"An embedded server config is created"
            this.context = new AnnotationConfigEmbeddedWebApplicationContext(Application)

        then:"The context is valid"
            context != null
            new URL("http://localhost:${context.embeddedServletContainer.port}/foo/bar").text == 'hello world'
            new URL("http://localhost:${context.embeddedServletContainer.port}/foos").text == 'all foos'
    }

    @Configuration
    @EnableWebMvc
    static class Application extends GrailsAutoConfiguration {
        @Bean
        public EmbeddedServletContainerFactory containerFactory() {
            return new TomcatEmbeddedServletContainerFactory(0);
        }
    }

}

@Controller
class FooController {
    def bar() {
        render "hello world"
    }
    def list() {
        render "all foos"
    }

    def closure = {}
}

@Artefact('UrlMappings')
class UrlMappings {
    static mappings = {
        "/$controller/$action?/$id?(.$format)?"()
        "/foos"(controller:'foo', action:"list")
    }
}

