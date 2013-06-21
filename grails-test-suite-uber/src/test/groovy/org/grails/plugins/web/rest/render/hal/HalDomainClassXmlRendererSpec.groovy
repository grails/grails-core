package org.grails.plugins.web.rest.render.hal

import grails.rest.render.hal.HalXmlRenderer
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
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.springframework.context.support.StaticMessageSource
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.util.WebUtils
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class HalDomainClassXmlRendererSpec extends Specification {

    void setup() {
        final initializer = new ConvertersConfigurationInitializer()
        initializer.initialize(new DefaultGrailsApplication())

    }
    void cleanup() {
        RequestContextHolder.resetRequestAttributes()
        ConvertersConfigurationHolder.clear()
    }
    void "Test that the HAL renderer renders domain objects with appropriate links"() {
        given:"A HAL renderer"
            HalXmlRenderer renderer = getRenderer()

        when:"A domain object is rendered"
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            def response = webRequest.response
            def renderContext = new ServletRenderContext(webRequest)
            final author = new Author(name: "Stephen King")
            author.id = 2L
            def book = new Book(title:"The Stand", author: author)
            book.authors = []
            book.authors << author
            book.link(href:"/publisher", rel:"The Publisher")
            final author2 = new Author(name: "King Stephen")
            author2.id = 3L
            book.authors << author2
            book.id = 1L
            renderer.render(book, renderContext)

        then:"The resulting HAL is correct"
            response.contentAsString == '<?xml version="1.0" encoding="UTF-8"?><resource href="http://localhost/books/1" hreflang="en"><link rel="The Publisher" href="/publisher" hreflang="en" /><link rel="author" href="http://localhost/authors/2" hreflang="en" /><title>The Stand</title><resource href="http://localhost/authors/2" hreflang="en"><name>Stephen King</name></resource><resource href="http://localhost/authors/2" hreflang="en"><name>Stephen King</name></resource><resource href="http://localhost/authors/3" hreflang="en"><name>King Stephen</name></resource></resource>'
            response.contentType == HalXmlRenderer.MIME_TYPE.name


    }

    void "Test that the HAL renderer renders regular linkable groovy objects with appropriate links"() {
        given:"A HAL renderer"
            HalXmlRenderer renderer = getRenderer()
//            renderer.prettyPrint = true

        when:"A domain object is rendered"
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/product/Macbook")
            def response = webRequest.response
            def renderContext = new ServletRenderContext(webRequest)
            def product = new Product(name: "MacBook", category: new Category(name: "laptop"))
            product.link(rel:"company",href: "http://apple.com", title: "Made by Apple")
            renderer.render(product, renderContext)

        then:"The resulting HAL is correct"
        response.contentType == HalXmlRenderer.MIME_TYPE.name
        response.contentAsString == '<?xml version="1.0" encoding="UTF-8"?><resource href="http://localhost/product/Macbook" hreflang="en"><link rel="company" href="http://apple.com" hreflang="en" title="Made by Apple" /><category><name>laptop</name></category><name>MacBook</name></resource>'


    }

    void "Test that the HAL renderer renders a list of domain objects with the appropriate links"() {
        given:"A HAL renderer"
            HalXmlRenderer renderer = getRenderer()

        when:"A domain object is rendered"
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            def response = webRequest.response
            def renderContext = new ServletRenderContext(webRequest)
            final author = new Author(name: "Stephen King")
            author.id = 2L
            def book = new Book(title:"The Stand", author: author)
            book.authors = []
            book.authors << author
            book.link(href:"/publisher", rel:"The Publisher")
            final author2 = new Author(name: "King Stephen")
            author2.id = 3L
            book.authors << author2
            book.id = 1L
            webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/authors")
            renderer.render(book.authors, renderContext)
        then:"The resulting HAL is correct"
            response.contentAsString == '<?xml version="1.0" encoding="UTF-8"?><resource href="http://localhost/authors" hreflang="en"><resource href="http://localhost/authors/2" hreflang="en"><name>Stephen King</name></resource><resource href="http://localhost/authors/3" hreflang="en"><name>King Stephen</name></resource></resource>'
            response.contentType == HalXmlRenderer.MIME_TYPE.name

    }

    protected HalXmlRenderer getRenderer() {
        def renderer = new HalXmlRenderer(Book)
        renderer.mappingContext = mappingContext
        renderer.messageSource = new StaticMessageSource()
        renderer.linkGenerator = getLinkGenerator {
            "/books"(resources: "book")
            "/authors"(resources: "author")
        }
        renderer
    }



    MappingContext getMappingContext() {
        final context = new KeyValueMappingContext("")
        context.addPersistentEntity(Book)
        context.addPersistentEntity(Author)
        return context
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
