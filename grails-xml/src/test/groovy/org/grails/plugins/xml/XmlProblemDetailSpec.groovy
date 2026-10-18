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
package org.grails.plugins.xml

import groovy.namespace.QName
import groovy.xml.XmlParser
import groovy.xml.XmlSlurper

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.servlet.ModelAndView

import grails.artefact.Artefact
import grails.rest.render.RenderContext
import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import grails.testing.web.controllers.ControllerUnitTest
import grails.web.mime.MimeType
import org.grails.gsp.io.GroovyPageScriptSource
import org.grails.plugins.web.rest.render.DefaultRendererRegistry
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.grails.plugins.web.rest.render.xml.DefaultXmlRenderer
import org.grails.web.gsp.io.GrailsConventionGroovyPageLocator
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Specification

class XmlProblemDetailSpec extends Specification implements ControllerUnitTest<ProblemXmlController> {

    void 'a ProblemDetail responded to an XML request is application/problem+xml, with the status it carries'() {
        given:
        response.format = 'xml'

        when:
        controller.respond(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, 'The title is taken'))

        then: 'Spring MVC writes it, whatever grails.web.rendering.xml.spring is set to'
        response.status == 409
        response.contentType.startsWith('application/problem+xml')

        and: 'as the problem element of RFC 9457'
        def problem = new XmlSlurper().parseText(response.contentAsString)
        tree(response.contentAsString)[0..1] == ['problem', 'urn:ietf:rfc:7807']
        problem.status.text() == '409'
        problem.detail.text() == 'The title is taken'
    }

    void 'a browser, whose Accept header lists XML, is answered with application/problem+xml, as Spring MVC answers it with an XML converter'() {
        given:
        request.addHeader('Accept', 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8')
        request.addHeader('User-Agent', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)')

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT))

        then:
        response.status == 409
        response.contentType.startsWith('application/problem+xml')
        new XmlSlurper().parseText(response.contentAsString).status.text() == '409'
    }

    void 'a client that asks for application/problem+xml is answered with it, as Spring MVC answers it with an XML converter'() {
        given:
        request.addHeader('Accept', 'application/problem+xml')

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT))

        then:
        response.status == 409
        response.contentType.startsWith('application/problem+xml')
    }

    void 'a status respond is given decides the status of the response, and the problem keeps its own'() {
        given:
        response.format = 'xml'

        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT), status: 400)

        then:
        response.status == 400
        new XmlSlurper().parseText(response.contentAsString).status.text() == '409'
    }

    void 'without a converter for problems, as without jackson-dataformat-xml, a problem is written as Spring writes it'() {
        given: 'a problem with extension members, which Spring writes'
        def problem = { ->
            def written = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, 'The title is taken & <gone>')
            written.instance = URI.create('/books/1')
            written.setProperty('errors', [[objectName: 'book', field: 'title', codes: ['a', 'b']],
                                           [objectName: 'book', message: null]])
            written.setProperty('retryAt', new Date(0))
            written.setProperty('none', null)
            written.setProperty('empty', [])
            written
        }
        response.format = 'xml'
        controller.respond(problem())
        def spring = response.contentAsString
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [] }

        when:
        def fallback = renderProblem(renderer, problem())

        then:
        fallback.status == 409
        fallback.contentType.startsWith('application/problem+xml')
        tree(fallback.contentAsString) == tree(spring)
    }

    void 'without a converter for problems, the properties of a subclass of ProblemDetail are written as Spring writes them'() {
        given:
        def problem = { -> new OutOfStock(balance: 12.5G, product: 'Grails').tap { instance = URI.create('/books/1') } }
        response.format = 'xml'
        controller.respond(problem())
        def spring = response.contentAsString
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [] }

        when:
        def fallback = renderProblem(renderer, problem())

        then:
        new XmlSlurper().parseText(spring).balance.text() == '12.5'
        tree(fallback.contentAsString) == tree(spring)
    }

    void 'a ProblemDetail is rendered by the XML view respond names, with the status it carries'() {
        given: 'an XML view of the action, and one that respond names'
        def locator = Stub(GrailsConventionGroovyPageLocator) {
            findViewForFormat('book', 'show', 'xml') >> Stub(GroovyPageScriptSource)
            findViewForFormat('book', 'conflict', 'xml') >> Stub(GroovyPageScriptSource)
        }
        def renderer = new DefaultXmlRenderer<Object>(Object, locator)
        def problem = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        def named = xmlRequest()

        when:
        renderer.render(problem, new ServletRenderContext(named, [view: 'conflict']) {
            @Override
            MimeType getAcceptMimeType() {
                MimeType.XML
            }
        })
        ModelAndView modelAndView = named.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)

        then:
        modelAndView.viewName == 'conflict'
        modelAndView.model == [problemDetail: problem]
        named.currentResponse.status == 409

        when: 'respond names no view'
        def unnamed = xmlRequest()
        renderer.springHttpMessageConvertersSupplier = { [] }
        renderer.render(ProblemDetail.forStatus(HttpStatus.CONFLICT), new ServletRenderContext(unnamed) {
            @Override
            MimeType getAcceptMimeType() {
                MimeType.XML
            }
        })

        then: 'the view of the action does not render the problem'
        unnamed.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW) == null
        unnamed.currentResponse.contentType.startsWith('application/problem+xml')
    }

    void 'the XML view respond names renders a ProblemDetail, rather than the HTML renderer an application registers for problems'() {
        given:
        def locator = Stub(GrailsConventionGroovyPageLocator) {
            findViewForFormat('book', 'conflict', 'xml') >> Stub(GroovyPageScriptSource)
        }
        def page = Stub(Renderer) {
            render(_, _) >> { Object problem, RenderContext context -> context.viewName = '/errors/problem' }
        }
        def registry = Stub(RendererRegistry) {
            findRenderer(MimeType.HTML, _) >> page
        }
        def renderer = new DefaultXmlRenderer<Object>(Object, locator, registry)
        def problem = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        def named = xmlRequest()

        when:
        renderer.render(problem, new ServletRenderContext(named, [view: 'conflict']) {
            @Override
            MimeType getAcceptMimeType() {
                MimeType.XML
            }
        })
        ModelAndView modelAndView = named.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)

        then:
        modelAndView.viewName == 'conflict'
        modelAndView.model == [problemDetail: problem]
        named.currentResponse.status == 409
    }

    void 'the XML view respond names gets a ProblemDetail under the model name its HTML view gets it'() {
        given: 'a registry that names the model of a view with the domain suffix'
        def locator = Stub(GrailsConventionGroovyPageLocator) {
            findViewForFormat('book', 'conflict', 'xml') >> Stub(GroovyPageScriptSource)
        }
        def registry = new DefaultRendererRegistry(modelSuffix: 'Instance')
        registry.initialize()
        def renderer = new DefaultXmlRenderer<Object>(Object, locator, registry)
        def problem = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        def html = xmlRequest()

        when: 'the HTML view'
        registry.findRenderer(MimeType.HTML, problem).render(problem, new ServletRenderContext(html, [view: 'conflict']))

        then:
        html.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW).model == [problemDetailInstance: problem]

        when: 'the XML view'
        def xml = xmlRequest()
        renderer.render(problem, new ServletRenderContext(xml, [view: 'conflict']) {
            @Override
            MimeType getAcceptMimeType() {
                MimeType.XML
            }
        })

        then:
        xml.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW).model == [problemDetailInstance: problem]
    }

    private static GrailsWebRequest xmlRequest() {
        def webRequest = new GrailsWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
        webRequest.controllerName = 'book'
        webRequest.actionName = 'show'
        webRequest
    }

    private static MockHttpServletResponse renderProblem(DefaultXmlRenderer<Object> renderer, ProblemDetail problem) {
        def response = new MockHttpServletResponse()
        def webRequest = new GrailsWebRequest(new MockHttpServletRequest(), response, new MockServletContext())
        renderer.render(problem, new ServletRenderContext(webRequest) {
            @Override
            MimeType getAcceptMimeType() {
                MimeType.XML
            }
        })
        response
    }

    /** The name, namespace, nil attribute and content of each element, which the XML a writer formats leaves alike. */
    private static List tree(String xml) {
        node(new XmlParser(false, true).parseText(xml))
    }

    private static List node(Node element) {
        def named = element.name()
        QName name = named instanceof QName ? (QName) named : new QName('', named.toString())
        Object nil = null
        element.attributes().each { Object key, Object value ->
            if (key instanceof QName && ((QName) key).localPart == 'nil') {
                nil = value
            }
        }
        List<Node> children = element.children().findAll { it instanceof Node } as List<Node>
        List content = []
        for (Node child : children) {
            content << node(child)
        }
        [name.localPart, name.namespaceURI, nil, children ? content : element.text()]
    }
}

@Artefact('Controller')
class ProblemXmlController { }

class OutOfStock extends ProblemDetail {
    BigDecimal balance
    String product

    OutOfStock() {
        super(409)
    }
}
