package org.grails.plugins.web.rest.render.hal

import grails.persistence.Entity
import grails.rest.Resource
import grails.rest.render.hal.HalDomainClassJsonRenderer
import grails.util.GrailsWebUtil
import grails.web.CamelCaseUrlConverter
import org.codehaus.groovy.grails.web.mapping.DefaultLinkGenerator
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingEvaluator
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.springframework.context.support.StaticMessageSource
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class HalDomainClassJsonRendererSpec extends Specification {


    void cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }
    void "Test that the HAL renderer renders domain objects with appropriate links"() {
        given:"A HAL renderer"
            def renderer = new HalDomainClassJsonRenderer(Book)
            renderer.mappingContext = mappingContext
            renderer.messageSource = new StaticMessageSource()
            renderer.linkGenerator = getLinkGenerator {
                "/books"(resources:"book")
                "/authors"(resources: "author")
            }

        when:"A domain object is rendered"
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            def response = webRequest.response
            def renderContext = new ServletRenderContext(webRequest)
            final author = new Author(name: "Stephen King")
            author.id = 2L
            def book = new Book(title:"The Stand", author: author)
            book.id = 1L
            renderer.render(book, renderContext)

        then:"The resulting HAL is correct"
            response.contentType == HalDomainClassJsonRenderer.MIME_TYPE.name
            response.contentAsString == '{"_links":{"self":{"href":"/books/1","hreflang":"en"},"author":{"href":"/authors/2","hreflang":"en"}},"_embedded":{"author":{"_links":{"self":{"href":"/authors/2","hreflang":"en"}},"name":"\\"Stephen King\\""}}}'


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

@Entity
@Resource
class Book {
    Author author
}

@Entity
@Resource
class Author {
    String name
}
