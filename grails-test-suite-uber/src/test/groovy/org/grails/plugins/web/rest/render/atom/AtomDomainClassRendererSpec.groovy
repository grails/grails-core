/*
 * Copyright 2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.plugins.web.rest.render.atom

import grails.config.Config
import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.rest.render.atom.AtomRenderer
import grails.util.GrailsWebMockUtil
import grails.util.GrailsWebUtil
import grails.web.CamelCaseUrlConverter
import grails.web.mapping.LinkGenerator
import grails.web.mapping.UrlMappingsHolder
import grails.web.mime.MimeType
import org.grails.config.PropertySourcesConfig
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.plugins.web.mime.MimeTypesFactoryBean
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.grails.plugins.web.rest.render.hal.Author
import org.grails.plugins.web.rest.render.hal.Book
import org.grails.support.MockApplicationContext
import org.grails.web.mapping.DefaultLinkGenerator
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.grails.web.mime.DefaultMimeUtility
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.MutablePropertySources
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.GenericWebApplicationContext
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
            def webRequest = boundMimeTypeRequest()
            webRequest.request.addHeader("ACCEPT", "application/atom+xml")
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
            response.contentAsString == '<?xml version="1.0" encoding="UTF-8"?><feed xmlns="http://www.w3.org/2005/Atom"><id>tag:localhost:1</id><link rel="self" href="http://localhost/books/1" hreflang="en" type="application/atom+xml" /><link rel="alternate" href="http://localhost/books/1" hreflang="en" /><link rel="The Publisher" href="/publisher" hreflang="en" /><link rel="author" href="http://localhost/authors/2" hreflang="en" /><title>The Stand</title><authors><entry><title>org.grails.plugins.web.rest.render.hal.Author : 2</title><id>tag:localhost:2</id><link rel="self" href="http://localhost/authors/2" hreflang="en" type="application/atom+xml" /><link rel="alternate" href="http://localhost/authors/2" hreflang="en" /><name>Stephen King</name></entry><entry><title>org.grails.plugins.web.rest.render.hal.Author : 3</title><id>tag:localhost:3</id><link rel="self" href="http://localhost/authors/3" hreflang="en" type="application/atom+xml" /><link rel="alternate" href="http://localhost/authors/3" hreflang="en" /><name>King Stephen</name></entry></authors></feed>'
            response.contentType == GrailsWebUtil.getContentType(AtomRenderer.MIME_TYPE.name, GrailsWebUtil.DEFAULT_ENCODING)


    }

    void "Test that the Atom renderer renders a list of domain objects with the appropriate links"() {
        given:"A Atom renderer"
            AtomRenderer renderer = getRenderer()

        when:"A domain object is rendered"
            def webRequest = GrailsWebMockUtil.bindMockWebRequest()
            webRequest.request.addHeader("ACCEPT", "application/atom+xml")
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
            response.contentType == GrailsWebUtil.getContentType(AtomRenderer.MIME_TYPE.name, GrailsWebUtil.DEFAULT_ENCODING)

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
        def ctx = new MockApplicationContext()
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, new DefaultGrailsApplication())
        def evaluator = new DefaultUrlMappingEvaluator(ctx)
        def allMappings = evaluator.evaluateMappings mappings
        return new DefaultUrlMappingsHolder(allMappings)
    }

    private GrailsWebRequest boundMimeTypeRequest() {
        def servletContext = new MockServletContext()
        def ctx = new GenericWebApplicationContext(servletContext)
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ctx)
        def application = new DefaultGrailsApplication()
        application.config = testConfig
        ctx.beanFactory.registerSingleton("mimeUtility", new DefaultMimeUtility(buildMimeTypes(application)))

        ctx.beanFactory.registerSingleton(GrailsApplication.APPLICATION_ID, application)
        ctx.refresh()
        GrailsWebMockUtil.bindMockWebRequest(ctx)
    }

    String applicationConfigText = '''
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = true
grails.mime.types = [
                      all: '*/*',
                      html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      hal:           ['application/hal+json','application/hal+xml'],
                      multipartForm: 'multipart/form-data'
                    ]
'''

    private Config getTestConfig() {
        def s = new ConfigSlurper()
        def config = s.parse(String.valueOf(applicationConfigText))

        def propertySources = new MutablePropertySources()
        propertySources.addLast(new MapPropertySource("grails", config))


        return new PropertySourcesConfig(propertySources)
    }

    private MimeType[] buildMimeTypes(application) {
        def mimeTypesFactory = new MimeTypesFactoryBean()
        mimeTypesFactory.grailsApplication = application
        def mimeTypes = mimeTypesFactory.getObject()
        mimeTypes
    }
}
