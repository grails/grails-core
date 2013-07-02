package org.grails.plugins.web.rest.render.atom

import grails.rest.render.atom.AtomRenderer
import grails.util.GrailsWebUtil
import grails.web.CamelCaseUrlConverter
import org.codehaus.groovy.grails.web.mapping.*
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.grails.plugins.web.rest.render.hal.Author
import org.grails.plugins.web.rest.render.hal.Book
import org.springframework.context.support.StaticMessageSource
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.util.WebUtils
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class AtomDomainClassRendererSpec extends Specification {

    void cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }
    void "Test that the Atom renderer renders domain objects with appropriate links"() {
        given:"A Atom renderer"
            AtomRenderer renderer = getRenderer()

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

        then:"The resulting Atom is correct"
            response.contentAsString == '<?xml version="1.0" encoding="UTF-8"?><feed xmlns="http://www.w3.org/2005/Atom"><id>tag:localhost:1</id><link rel="self" href="http://localhost/books/1" hreflang="en" type="application/atom+xml" /><link rel="alternate" href="http://localhost/books/1" hreflang="en" /><link rel="The Publisher" href="/publisher" hreflang="en" /><link rel="author" href="http://localhost/authors/2" hreflang="en" /><title>The Stand</title><entry><title>org.grails.plugins.web.rest.render.hal.Author : 2</title><id>tag:localhost:2</id><link rel="self" href="http://localhost/authors/2" hreflang="en" type="application/atom+xml" /><link rel="alternate" href="http://localhost/authors/2" hreflang="en" /><name>Stephen King</name></entry><authors><entry><title>org.grails.plugins.web.rest.render.hal.Author : 2</title><id>tag:localhost:2</id><link rel="self" href="http://localhost/authors/2" hreflang="en" type="application/atom+xml" /><link rel="alternate" href="http://localhost/authors/2" hreflang="en" /><name>Stephen King</name></entry><entry><title>org.grails.plugins.web.rest.render.hal.Author : 3</title><id>tag:localhost:3</id><link rel="self" href="http://localhost/authors/3" hreflang="en" type="application/atom+xml" /><link rel="alternate" href="http://localhost/authors/3" hreflang="en" /><name>King Stephen</name></entry></authors></feed>'
            response.contentType == AtomRenderer.MIME_TYPE.name


    }

    void "Test that the Atom renderer renders a list of domain objects with the appropriate links"() {
        given:"A Atom renderer"
            AtomRenderer renderer = getRenderer()

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
        then:"The resulting Atom is correct"
            response.contentAsString == '<?xml version="1.0" encoding="UTF-8"?><feed xmlns="http://www.w3.org/2005/Atom"><title></title><id>tag:localhost:/authors</id><link rel="self" href="http://localhost/authors" hreflang="en" type="application/atom+xml" /><link rel="alternate" href="http://localhost/authors" hreflang="en" /><entry><title>org.grails.plugins.web.rest.render.hal.Author : 2</title><id>tag:localhost:2</id><link rel="self" href="http://localhost/authors/2" hreflang="en" type="application/atom+xml" /><link rel="alternate" href="http://localhost/authors/2" hreflang="en" /><name>Stephen King</name></entry><entry><title>org.grails.plugins.web.rest.render.hal.Author : 3</title><id>tag:localhost:3</id><link rel="self" href="http://localhost/authors/3" hreflang="en" type="application/atom+xml" /><link rel="alternate" href="http://localhost/authors/3" hreflang="en" /><name>King Stephen</name></entry></feed>'
            response.contentType == AtomRenderer.MIME_TYPE.name

    }

    protected AtomRenderer getRenderer() {
        def renderer = new AtomRenderer(Book)
//        renderer.prettyPrint = true
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
