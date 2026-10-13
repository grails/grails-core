/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.plugins.web.rest.render.atom

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.persistence.Entity
import grails.rest.render.atom.AtomCollectionRenderer
import grails.rest.render.atom.AtomRenderer
import grails.spring.BeanBuilder
import grails.util.GrailsWebMockUtil
import grails.util.GrailsWebUtil
import grails.web.CamelCaseUrlConverter
import grails.web.mapping.LinkGenerator
import org.grails.config.PropertySourcesConfig
import org.grails.core.lifecycle.ShutdownOperations
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.plugins.web.mime.MimeTypesConfiguration
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.grails.plugins.web.rest.render.hal.Product
import org.grails.plugins.web.rest.render.hal.SimpleProduct
import org.grails.support.MockApplicationContext
import org.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.grails.web.mapping.DefaultLinkGenerator
import org.grails.web.mapping.DefaultUrlMappingEvaluator
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.grails.web.mime.DefaultMimeUtility
import org.grails.web.mime.HttpServletResponseExtension
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.StaticMessageSource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.GenericWebApplicationContext
import org.springframework.web.util.WebUtils
import spock.lang.Specification

class AtomRendererSpec extends Specification {

    void setup() {
        ShutdownOperations.runOperations()
        HttpServletResponseExtension.@mimeTypes = null
        def initializer = new ConvertersConfigurationInitializer()
        initializer.grailsApplication = new DefaultGrailsApplication()
        initializer.initialize()
    }

    void cleanup() {
        ConvertersConfigurationHolder.clear()
        RequestContextHolder.resetRequestAttributes()
        ShutdownOperations.runOperations()
        HttpServletResponseExtension.@mimeTypes = null
    }

    void 'Test rendering a single domain object as an Atom feed'() {
        given:
        def renderer = configureRenderer(new AtomRenderer(AtomBook))
        def webRequest = boundMimeTypeRequest()
        def response = webRequest.response as MockHttpServletResponse
        def book = new AtomBook(title: 'Grails & Groovy')
        book.id = 1L
        book.dateCreated = new Date(1383908400000L)
        book.lastUpdated = new Date(1383994800000L)

        when:
        renderer.render(book, new ServletRenderContext(webRequest))

        then:
        response.contentType == GrailsWebUtil.getContentType(AtomRenderer.MIME_TYPE.name, GrailsWebUtil.DEFAULT_ENCODING)
        response.contentAsString.startsWith('<?xml version="1.0" encoding="UTF-8"?>')
        response.contentAsString.contains('<feed xmlns="http://www.w3.org/2005/Atom">')
        response.contentAsString.contains('<title>Grails &amp; Groovy</title>')
        response.contentAsString.contains('<published>')
        response.contentAsString.contains('<updated>')
        response.contentAsString.contains('<id>tag:localhost,')
        response.contentAsString.contains(':1</id>')
        response.contentAsString.contains('<link rel="self" href="http://localhost/books/1" hreflang="en" title="Books" type="application/atom+xml" />')
        response.contentAsString.contains('<link rel="alternate" href="http://localhost/books/1" hreflang="en" title="Books" />')
        response.contentAsString.endsWith('</feed>')
    }

    void 'Test rendering a collection of domain objects as Atom entries'() {
        given:
        def renderer = configureRenderer(new AtomCollectionRenderer(AtomBook))
        def webRequest = boundMimeTypeRequest()
        def response = webRequest.response as MockHttpServletResponse
        def books = [
            new AtomBook(title: 'Grails'),
            new AtomBook(title: 'Groovy')
        ]
        books[0].id = 1L
        books[1].id = 2L

        when:
        renderer.render(books, new ServletRenderContext(webRequest))

        then:
        renderer.componentType == AtomBook
        response.contentType == GrailsWebUtil.getContentType(AtomRenderer.MIME_TYPE.name, GrailsWebUtil.DEFAULT_ENCODING)
        response.contentAsString.contains('<feed xmlns="http://www.w3.org/2005/Atom"><title>Books</title><id>tag:localhost:/books</id>')
        response.contentAsString.count('<entry>') == 2
        response.contentAsString.contains('<entry><id>tag:localhost:1</id>')
        response.contentAsString.contains('<entry><id>tag:localhost:2</id>')
        response.contentAsString.contains('rel="self" href="http://localhost/books/1"')
        response.contentAsString.contains('rel="self" href="http://localhost/books/2"')
        response.contentAsString.contains('<title>Grails</title>')
        response.contentAsString.contains('<title>Groovy</title>')
        !response.contentAsString.contains('<published>')
        !response.contentAsString.contains('<updated>')
        response.contentAsString.endsWith('</entry></feed>')
    }

    void 'Test rendering an empty Atom collection'() {
        given:
        def renderer = configureRenderer(new AtomCollectionRenderer(AtomBook))
        def webRequest = boundMimeTypeRequest()
        def response = webRequest.response as MockHttpServletResponse

        when:
        renderer.render([], new ServletRenderContext(webRequest))

        then:
        response.contentType == GrailsWebUtil.getContentType(AtomRenderer.MIME_TYPE.name, GrailsWebUtil.DEFAULT_ENCODING)
        response.contentAsString.contains('<feed xmlns="http://www.w3.org/2005/Atom"><title>Books</title><id>tag:localhost:/books</id>')
        response.contentAsString.contains('rel="self" href="http://localhost/books"')
        response.contentAsString.contains('rel="alternate" href="http://localhost/books"')
        !response.contentAsString.contains('<entry>')
        response.contentAsString.endsWith('</feed>')
    }

    void 'Test Atom rejects non domain objects for #collection'() {
        given:
        def renderer = configureRenderer(collection ? new AtomCollectionRenderer(SimpleProduct) : new AtomRenderer(SimpleProduct))
        def webRequest = boundMimeTypeRequest()
        def product = new SimpleProduct(name: 'MacBook')

        when:
        renderer.render(collection ? [product] : product, new ServletRenderContext(webRequest))

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains("The AtomRenderer can only be used with domain classes that specify 'dateCreated' and 'lastUpdated' properties")

        where:
        collection << [false, true]
    }

    void 'Test Atom rejects domain objects without lastUpdated'() {
        given:
        def renderer = configureRenderer(new AtomRenderer(Product))
        def webRequest = boundMimeTypeRequest()

        when:
        renderer.render(new Product(name: 'MacBook'), new ServletRenderContext(webRequest))

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains("The AtomRenderer can only be used with domain classes that specify 'dateCreated' and 'lastUpdated' properties")
    }

    private AtomRenderer configureRenderer(AtomRenderer renderer) {
        def mappingContext = new KeyValueMappingContext('')
        mappingContext.addPersistentEntity(AtomBook)
        mappingContext.addPersistentEntity(Product)
        renderer.mappingContext = mappingContext
        def messages = new StaticMessageSource()
        messages.addMessage('resource.atomBook.href.title', Locale.ENGLISH, 'Books')
        messages.addMessage('resource.books.href.title', Locale.ENGLISH, 'Books')
        renderer.messageSource = messages
        renderer.linkGenerator = getLinkGenerator()
        renderer.prettyPrint = false
        renderer
    }

    private LinkGenerator getLinkGenerator() {
        def ctx = new MockApplicationContext()
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, new DefaultGrailsApplication())
        def evaluator = new DefaultUrlMappingEvaluator(ctx)
        def mappings = evaluator.evaluateMappings {
            '/books'(resources: 'atomBook')
        }
        def generator = new DefaultLinkGenerator('http://localhost', null)
        generator.grailsUrlConverter = new CamelCaseUrlConverter()
        generator.urlMappingsHolder = new DefaultUrlMappingsHolder(mappings)
        generator
    }

    private GrailsWebRequest boundMimeTypeRequest() {
        def servletContext = new MockServletContext()
        def ctx = new GenericWebApplicationContext(servletContext)
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ctx)
        def application = new DefaultGrailsApplication()
        application.config = new PropertySourcesConfig([
            'grails.mime.use.accept.header': true,
            'grails.mime.types': [atom: 'application/atom+xml']
        ])
        def mainContext = new GenericApplicationContext()
        mainContext.refresh()
        application.setApplicationContext(mainContext)
        def bb = new BeanBuilder()
        bb.beans {
            grailsApplication = application
            mimeConfiguration(MimeTypesConfiguration, application, [])
        }
        def mimeConfiguration = bb.createApplicationContext().getBean(MimeTypesConfiguration)
        ctx.beanFactory.registerSingleton('mimeUtility', new DefaultMimeUtility(mimeConfiguration.mimeTypes()))
        ctx.beanFactory.registerSingleton(GrailsApplication.APPLICATION_ID, application)
        ctx.refresh()
        def webRequest = GrailsWebMockUtil.bindMockWebRequest(ctx)
        (webRequest.request as MockHttpServletRequest).addHeader('ACCEPT', 'application/atom+xml')
        webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, '/books')
        webRequest
    }
}

@Entity
class AtomBook {
    String title
    Date dateCreated
    Date lastUpdated
}
