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

import groovy.xml.XmlSlurper

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import grails.web.mime.MimeType
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.grails.plugins.web.rest.render.xml.DefaultXmlRenderer
import org.grails.web.servlet.mvc.GrailsWebRequest
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

        and:
        def problem = new XmlSlurper().parseText(response.contentAsString)
        problem.status.text() == '409'
        problem.detail.text() == 'The title is taken'
    }

    void 'without a converter for problems, the status is still the problem one, and the legacy converter writes it once warned'() {
        given:
        def renderer = new DefaultXmlRenderer<Object>(Object)
        renderer.springHttpMessageConvertersSupplier = { [] }

        when:
        def first = renderProblem(renderer, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, 'The title is taken'))
        def second = renderProblem(renderer, ProblemDetail.forStatus(HttpStatus.GONE))

        then:
        first.response.status == 409
        first.response.contentType.startsWith('application/xml')
        new XmlSlurper().parseText(first.response.contentAsString).detail.text() == 'The title is taken'
        first.log.contains('so the legacy XML converter writes it as a bean')

        and: 'the warning is logged once'
        second.response.status == 410
        !second.log.contains('legacy XML converter')
    }

    private static Map renderProblem(DefaultXmlRenderer<Object> renderer, ProblemDetail problem) {
        def response = new MockHttpServletResponse()
        def webRequest = new GrailsWebRequest(new MockHttpServletRequest(), response, new MockServletContext())
        PrintStream original = System.err
        def buffer = new ByteArrayOutputStream()
        System.setErr(new PrintStream(buffer, true))
        try {
            renderer.render(problem, new ServletRenderContext(webRequest) {
                @Override
                MimeType getAcceptMimeType() {
                    MimeType.XML
                }
            })
        }
        finally {
            System.setErr(original)
        }
        [response: response, log: buffer.toString()]
    }
}

@Artefact('Controller')
class ProblemXmlController { }
