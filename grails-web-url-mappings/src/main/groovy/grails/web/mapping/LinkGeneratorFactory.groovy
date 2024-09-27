package grails.web.mapping

import grails.web.CamelCaseUrlConverter
import grails.web.UrlConverter
import groovy.transform.CompileStatic
import org.grails.web.mapping.DefaultLinkGenerator
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Helper class for creating a {@link LinkGenerator}. Useful for testing
 *
 * @author Graeme Rocher
 * @since 3.1
 */
@CompileStatic
class LinkGeneratorFactory implements ApplicationContextAware {

    UrlMappingsFactory urlMappingsFactory = new UrlMappingsFactory()
    UrlConverter urlConverter = new CamelCaseUrlConverter()
    String baseURL = "http://localhost"
    String contextPath = null

    LinkGenerator create(Class mappings) {
        def urlMappings = urlMappingsFactory.create(mappings)
        return create(urlMappings)
    }
    LinkGenerator create(Closure mappings) {
        def urlMappings = urlMappingsFactory.create(mappings)
        return create(urlMappings)
    }

    LinkGenerator create(UrlMappings urlMappings) {
        def generator = new DefaultLinkGenerator(baseURL, contextPath)
        generator.grailsUrlConverter = urlConverter
        generator.urlMappingsHolder = (UrlMappingsHolder)urlMappings
        return generator
    }

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        urlMappingsFactory.applicationContext = applicationContext
    }
}
