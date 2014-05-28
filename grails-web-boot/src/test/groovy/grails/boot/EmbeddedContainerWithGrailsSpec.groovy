package grails.boot

import grails.artefact.Artefact
import grails.boot.config.GrailsConfiguration
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsDispatcherServlet
import org.springframework.beans.BeansException
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory
import org.springframework.boot.context.embedded.ServletRegistrationBean
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.type.AnnotationMetadata
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.util.StreamUtils
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import spock.lang.Specification

import java.nio.charset.Charset

import static org.junit.Assert.assertThat

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
            this.context = new AnnotationConfigEmbeddedWebApplicationContext(
                    TomcatConfig.class);

        then:"The context is valid"
            context != null
            new URL("http://localhost:${context.embeddedServletContainer.port}/foo/bar").text == 'hello world'
    }

    @Configuration
    @Import([GrailsConfig.class])
    static class TomcatConfig {
        @Bean
        public EmbeddedServletContainerFactory containerFactory() {
            return new TomcatEmbeddedServletContainerFactory(0);
        }
    }

    @Configuration
    @EnableWebMvc
    static class GrailsConfig extends GrailsConfiguration{
        @Override
        Collection<Class> classes() {
            [FooController, UrlMappings]
        }
    }
}

@Artefact("Controller")
class FooController {
    def bar() {
        render "hello world"
    }
}

class UrlMappings {
    static mappings = {
        "/$controller/$action?/$id?(.$format)?"()
    }
}

