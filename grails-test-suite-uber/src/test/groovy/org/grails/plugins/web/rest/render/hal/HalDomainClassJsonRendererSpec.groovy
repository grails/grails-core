package org.grails.plugins.web.rest.render.hal

import grails.persistence.Entity
import grails.rest.Linkable
import grails.rest.Resource
import grails.rest.render.hal.HalJsonRenderer
import grails.util.GrailsWebUtil
import grails.web.CamelCaseUrlConverter
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
class HalDomainClassJsonRendererSpec extends Specification {


    void cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }
    void "Test that the HAL renderer renders domain objects with appropriate links"() {
        given:"A HAL renderer"
            HalJsonRenderer renderer = getRenderer()

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
            response.contentType == HalJsonRenderer.MIME_TYPE.name
            response.contentAsString == '{"_links":{"self":{"href":"http://localhost/books/1","hreflang":"en","type":"application/hal+json"},"The Publisher":{"href":"/publisher","hreflang":"en"},"author":{"href":"http://localhost/authors/2","hreflang":"en"}},"title":"\\"The Stand\\"","_embedded":{"author":{"_links":{"self":{"href":"http://localhost/authors/2","hreflang":"en"}},"name":"\\"Stephen King\\""},"authors":[{"_links":{"self":{"href":"http://localhost/authors/2","hreflang":"en"}},"name":"\\"Stephen King\\""},{"_links":{"self":{"href":"http://localhost/authors/3","hreflang":"en"}},"name":"\\"King Stephen\\""}]}}'


    }

    void "Test that the HAL renderer renders regular linkable groovy objects with appropriate links"() {
        given:"A HAL renderer"
            HalJsonRenderer renderer = getRenderer()
            renderer.prettyPrint = true

        when:"A domain object is rendered"
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/product/Macbook")
            def response = webRequest.response
            def renderContext = new ServletRenderContext(webRequest)
            def product = new Product(name: "MacBook", category: new Category(name: "laptop"))
            product.link(rel:"company",href: "http://apple.com", title: "Made by Apple")
            renderer.render(product, renderContext)

        then:"The resulting HAL is correct"
            response.contentType == HalJsonRenderer.MIME_TYPE.name
            response.contentAsString == '''{
  "_links": {
    "self": {
      "href": "http://localhost/product/Macbook",
      "hreflang": "en",
      "type": "application/hal+json"
    },
    "company": {
      "href": "http://apple.com",
      "hreflang": "en",
      "title": "Made by Apple"
    }
  },
  "category": "{\\"name\\":\\"laptop\\"}",
  "name": "\\"MacBook\\""
}'''


    }

    void "Test that the HAL renderer renders a list of domain objects with the appropriate links"() {
        given:"A HAL renderer"
            HalJsonRenderer renderer = getRenderer()

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
            response.contentType == HalJsonRenderer.MIME_TYPE.name
            response.contentAsString == '{"_links":{"self":{"href":"http://localhost/authors","hreflang":"en","type":"application/hal+json"}},"_embedded":[{"_links":{"self":{"href":"http://localhost/authors/2","hreflang":"en","type":"application/hal+json"}},"name":"\\"Stephen King\\""},{"_links":{"self":{"href":"http://localhost/authors/3","hreflang":"en","type":"application/hal+json"}},"name":"\\"King Stephen\\""}]'

    }

    protected HalJsonRenderer getRenderer() {
        def renderer = new HalJsonRenderer(Book)
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

@Linkable
class Product {
    String name
    Category category
}
class Category {
    String name
}
@Entity
@Resource
class Book {
    Date dateCreated
    Date lastUpdated

    String title
    Author author

    List authors
    static hasMany = [authors:Author]
}

@Entity
@Resource
class Author {
    Date dateCreated
    Date lastUpdated

    String name
}
