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
package org.grails.web.util

import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.View
import org.springframework.web.servlet.ViewResolver
import spock.lang.Issue
import spock.lang.Specification

/**
 * @Author Sudhir Nimavat
 */
class WebUtilsSpec extends Specification {

    @Issue("https://github.com/apache/grails-core/issues/10545")
    def testToQueryString() {
        given:
        Map params = ["name":"sudhir-nimavat", "address.zip":"12345"]

        when:
        String result = WebUtils.toQueryString(params)

        then:
        result.startsWith("?")
        def tokens = result[1..-1].split('&')
        tokens.find({ it == "name=sudhir-nimavat"}) != null
        tokens.find({ it == "address.zip=12345"}) != null
    }

    def "decodes query strings and collects repeated parameters in order"() {
        expect:
        WebUtils.fromQueryString('?name=caf%C3%A9&tag=one&tag=two&tag=three&empty=&ignored&first+name=A+B') == [
            name: 'café', tag: ['one', 'two', 'three'], empty: '', 'first name': 'A B'
        ]
        WebUtils.fromQueryString('') == [:]
        WebUtils.fromQueryString('name=value') == [name: 'value']
    }

    def "rejects malformed percent escapes in query strings"() {
        when:
        WebUtils.fromQueryString('name=%ZZ')

        then:
        thrown(IllegalArgumentException)
    }

    def "adds controller prefixes only to relative view names"() {
        expect:
        WebUtils.addViewPrefix(viewName, controllerName) == expected

        where:
        viewName            | controllerName | expected
        'show'              | 'book'         | '/book/show'
        'show'              | null           | '/show'
        '/shared/show'      | 'book'         | '/shared/show'
        'redirect:/login'   | 'book'         | 'redirect:/login'
        'forward:/internal' | 'book'         | 'forward:/internal'
    }

    def "resolves a controller view using the current default locale without a web request"() {
        given:
        def request = new MockHttpServletRequest()
        def resolver = Mock(ViewResolver)
        def view = Stub(View)

        when:
        def resolved = WebUtils.resolveView(request, 'show', 'book', resolver)

        then:
        1 * resolver.resolveViewName('/book/show', Locale.getDefault()) >> view
        resolved.is(view)
    }

    def "resolves a controller view using the locale of the bound web request"() {
        given:
        def previous = RequestContextHolder.getRequestAttributes()
        def request = new MockHttpServletRequest()
        request.preferredLocales = [Locale.FRANCE]
        def resolver = Mock(ViewResolver)
        def view = Stub(View)
        WebUtils.storeGrailsWebRequest(new GrailsWebRequest(request, new MockHttpServletResponse(), request.servletContext))

        when:
        def resolved = WebUtils.resolveView(request, 'show', 'book', resolver)

        then:
        1 * resolver.resolveViewName('/book/show', Locale.FRANCE) >> view
        resolved.is(view)

        cleanup:
        WebUtils.clearGrailsWebRequest()
        RequestContextHolder.setRequestAttributes(previous)
    }

    def "strips the application context and Grails dispatch path from request URIs"() {
        given:
        def request = new MockHttpServletRequest()
        request.contextPath = '/app'
        request.requestURI = uri

        expect:
        WebUtils.getRequestURIForGrailsDispatchURI(request) == expected

        where:
        uri                                 | expected
        '/app/grails/book/show.dispatch'     | '/book/show'
        '/app/book/show.dispatch'            | '/book/show'
        '/app/book/show'                     | '/book/show'
    }

    def "exposes include paths without overwriting existing attributes and restores old values"() {
        given:
        def request = new MockHttpServletRequest()
        request.requestURI = '/app/book/show'
        request.contextPath = '/app'
        request.servletPath = '/book'
        request.pathInfo = '/show'
        request.queryString = 'id=1'
        request.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, '/original')
        def attributes = [
            (WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE): '/included',
            (WebUtils.INCLUDE_QUERY_STRING_ATTRIBUTE): 'id=2'
        ]

        when:
        WebUtils.exposeIncludeRequestAttributes(request)

        then:
        request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE) == '/original'
        request.getAttribute(WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE) == '/app'
        request.getAttribute(WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE) == '/book'
        request.getAttribute(WebUtils.INCLUDE_PATH_INFO_ATTRIBUTE) == '/show'
        request.getAttribute(WebUtils.INCLUDE_QUERY_STRING_ATTRIBUTE) == 'id=1'

        when:
        def oldValues = WebUtils.exposeRequestAttributesAndReturnOldValues(request, attributes)

        then:
        oldValues == [
            (WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE): '/original',
            (WebUtils.INCLUDE_QUERY_STRING_ATTRIBUTE): 'id=1'
        ]
        request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE) == '/included'
        request.getAttribute(WebUtils.INCLUDE_QUERY_STRING_ATTRIBUTE) == 'id=2'

        when:
        WebUtils.cleanupIncludeRequestAttributes(request, oldValues)

        then:
        request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE) == '/original'
        request.getAttribute(WebUtils.INCLUDE_QUERY_STRING_ATTRIBUTE) == 'id=1'
        request.getAttribute(WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE) == null
        request.getAttribute(WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE) == null
        request.getAttribute(WebUtils.INCLUDE_PATH_INFO_ATTRIBUTE) == null
    }

    def "attribute exposure returns only values that existed before the include"() {
        given:
        def request = new MockHttpServletRequest()
        request.setAttribute('existing', 'old')

        expect:
        WebUtils.exposeRequestAttributesAndReturnOldValues(request, [existing: 'new', added: 'value']) == [existing: 'old']
        request.getAttribute('existing') == 'new'
        request.getAttribute('added') == 'value'
    }

    def "attribute exposure requires a request and an attributes map"() {
        when:
        WebUtils.exposeRequestAttributesAndReturnOldValues(request, attributes)

        then:
        thrown(IllegalArgumentException)

        where:
        request                      | attributes
        null                         | [:]
        new MockHttpServletRequest() | null
    }

    def "detects dispatch types and prefers a nonblank forward URI"() {
        given:
        def request = new MockHttpServletRequest()
        request.requestURI = '/current'

        expect:
        !WebUtils.isForward(request)
        !WebUtils.isInclude(request)
        !WebUtils.isForwardOrInclude(request)
        !WebUtils.isAsync(request)
        !WebUtils.isError(request)
        WebUtils.getForwardURI(request) == '/current'

        when:
        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, '/original')
        request.setAttribute(WebUtils.ASYNC_REQUEST_URI_ATTRIBUTE, '/async')
        request.setAttribute(WebUtils.ERROR_STATUS_CODE_ATTRIBUTE, 500)

        then:
        WebUtils.isForward(request)
        WebUtils.isForwardOrInclude(request)
        WebUtils.isAsync(request)
        WebUtils.isError(request)
        WebUtils.getForwardURI(request) == '/original'

        when:
        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, ' ')

        then:
        WebUtils.getForwardURI(request) == '/current'

        when:
        request.removeAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE)
        request.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, '/included')

        then:
        !WebUtils.isForward(request)
        WebUtils.isInclude(request)
        WebUtils.isForwardOrInclude(request)
    }

    def "stores retrieves and clears the web request in both request contexts"() {
        given:
        def previous = RequestContextHolder.getRequestAttributes()
        def request = new MockHttpServletRequest()
        def webRequest = new GrailsWebRequest(request, new MockHttpServletResponse(), request.servletContext)

        when:
        WebUtils.storeGrailsWebRequest(webRequest)

        then:
        WebUtils.retrieveGrailsWebRequest().is(webRequest)
        GrailsWebRequest.lookup(request).is(webRequest)

        when:
        WebUtils.clearGrailsWebRequest()
        WebUtils.clearGrailsWebRequest()

        then:
        RequestContextHolder.getRequestAttributes() == null
        GrailsWebRequest.lookup(request) == null
        !request.attributeNames.hasMoreElements()

        cleanup:
        RequestContextHolder.setRequestAttributes(previous)
    }

    def "retrieving a web request fails without request attributes and returns null for a non-Grails request"() {
        given:
        def previous = RequestContextHolder.getRequestAttributes()
        RequestContextHolder.resetRequestAttributes()

        when:
        WebUtils.retrieveGrailsWebRequest()

        then:
        thrown(IllegalStateException)

        when:
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()))

        then:
        WebUtils.retrieveGrailsWebRequest() == null

        cleanup:
        RequestContextHolder.setRequestAttributes(previous)
    }
}
