package org.grails.plugins.web.rest.render

import grails.persistence.Entity
import grails.rest.render.errors.VndErrorJsonRenderer
import grails.rest.render.errors.VndErrorXmlRenderer
import grails.util.GrailsWebUtil
import grails.web.CamelCaseUrlConverter
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.codehaus.groovy.grails.web.mapping.DefaultLinkGenerator
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingEvaluator
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.mime.MimeType
import org.springframework.context.support.StaticMessageSource
import org.springframework.mock.web.MockServletContext
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Specification

/**
 *
 * @author Graeme Rocher
 */
class VndErrorRenderingSpec extends Specification{

    void setup() {
        new ConvertersConfigurationInitializer().initialize(new DefaultGrailsApplication())
    }

    void cleanup() {
        RequestContextHolder.resetRequestAttributes()
        ConvertersConfigurationHolder.clear()

    }

    void "Test VND error rendering in XML" () {
        given:"A registry with VND error registered"
            def registry = new DefaultRendererRegistry()
            final vndRenderer = new VndErrorXmlRenderer()
            final messageSource = new StaticMessageSource()
            final request = GrailsWebUtil.bindMockWebRequest()
            messageSource.addMessage("title.invalid", request.locale, "Bad Title")
            vndRenderer.messageSource = messageSource
            vndRenderer.linkGenerator = getLinkGenerator {
                "/books"(resources:"book")
            }
            registry.addRenderer(vndRenderer)

        when:"A renderer is looked up"
            final book = new Book()
            book.id = 1
            def error = new BeanPropertyBindingResult(book, Book.name)
            error.rejectValue("title", "title.invalid")
            final renderer = registry.findContainerRenderer(MimeType.XML, Errors, book)

        then:"It is a VND error renderer"
            renderer instanceof VndErrorXmlRenderer


        when:"The renderer renders an error"
            renderer.render(error, new ServletRenderContext(request))

        then:"The response is correct"
            request.response.contentType == VndErrorXmlRenderer.MIME_TYPE.name
            request.response.contentAsString == '<?xml version="1.0" encoding="UTF-8"?><errors xml:lang="en"><error logref="book.title.invalid.1"><message>Bad Title</message><link rel="resource" href="http://localhost/books/1" /></error></errors>'

    }

    void "Test VND error rendering in JSON" () {
        given:"A registry with VND error registered"
            def registry = new DefaultRendererRegistry()
            final vndRenderer = new VndErrorJsonRenderer()
            final messageSource = new StaticMessageSource()
            final request = GrailsWebUtil.bindMockWebRequest()
            messageSource.addMessage("title.invalid", request.locale, "Bad Title")
            messageSource.addMessage("title.bad", request.locale, "Title Bad")
            vndRenderer.messageSource = messageSource
            vndRenderer.linkGenerator = getLinkGenerator {
                "/books"(resources:"book")
            }
            registry.addRenderer(vndRenderer)

        when:"A renderer is looked up"
            final book = new Book()
            book.id = 1
            def error = new BeanPropertyBindingResult(book, Book.name)
            error.rejectValue("title", "title.invalid")
            error.rejectValue("title", "title.bad")
            final renderer = registry.findContainerRenderer(MimeType.JSON, Errors, book)

        then:"It is a VND error renderer"
            renderer instanceof VndErrorJsonRenderer


        when:"The renderer renders an error"
            renderer.render(error, new ServletRenderContext(request))

            then:"The response is correct"
            request.response.contentType == VndErrorJsonRenderer.MIME_TYPE.name
            request.response.contentAsString == '[{"logref":"\\"book.title.invalid.1\\"","message":"Bad Title","_links":{"resource":{"href":"http://localhost/books/1"}}},{"logref":"\\"book.title.bad.1\\"","message":"Title Bad","_links":{"resource":{"href":"http://localhost/books/1"}}}]'

    }

    LinkGenerator getLinkGenerator(Closure mappings) {
        def generator = new DefaultLinkGenerator("http://localhost", null)
        generator.grailsUrlConverter = new CamelCaseUrlConverter()
        generator.urlMappingsHolder = getUrlMappingsHolder mappings
        return generator;
    }
    UrlMappingsHolder getUrlMappingsHolder(Closure mappings) {
        def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())
        def allMappings = evaluator.evaluateMappings mappings
        return new DefaultUrlMappingsHolder(allMappings)
    }

}

@Entity
class Book {
    Long id
    String title
}
