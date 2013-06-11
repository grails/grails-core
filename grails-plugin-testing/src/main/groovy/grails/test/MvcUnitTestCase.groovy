/*
 * Copyright 2008 GoPivotal, Inc. All Rights Reserved.
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
package grails.test

import grails.util.GrailsNameUtils

import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.pages.DefaultGroovyPagesUriService
import org.codehaus.groovy.grails.web.pages.GroovyPagesUriService
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

import org.springframework.mock.web.MockHttpSession
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.WebApplicationContext
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication

/**
 * Common test case support class for controllers, tag libraries, and
 * anything else that has access to the standard web properties such
 * as "request", "response", and "session".
 *
 * @author Graeme Rocher
 * @author Peter Ledbrook
 */
class MvcUnitTestCase extends GrailsUnitTestCase {
    private Class testClass

    protected GrailsMockHttpServletRequest mockRequest
    protected GrailsMockHttpServletResponse mockResponse
    protected MockHttpSession mockSession
    protected GrailsWebRequest webRequest

    protected Map forwardArgs
    protected Map redirectArgs
    protected Map renderArgs
    protected Map mockParams
    protected Map mockFlash

    /**
     * Creates a new test case for the class whose name and package
     * matches this test's class up to and including the given suffix.
     * In other words, if this test is <code>org.example.MyControllerTests</code>
     * then the class under test is <code>org.example.MyController</code>.
     * This example assumes that the suffix is "Controller".
     */
    MvcUnitTestCase(String suffix) {
        def m = getClass().name =~ /^([\w\.]*?[A-Z]\w*?${suffix})\w+/
        if (!m) {
            throw new RuntimeException("Cannot find matching class for this test.")
        }
        testClass = Thread.currentThread().contextClassLoader.loadClass(m[0][1])
    }

    /**
     * Creates a new test case for the given class.
     */
    MvcUnitTestCase(Class clazz) {
        testClass = clazz
    }

    protected void tearDown() {
        super.tearDown()
        RequestContextHolder.resetRequestAttributes()
    }

    Class getTestClass() { testClass }

    protected void reset() {
        mockRequest?.clearAttributes()
        mockRequest?.removeAllParameters()
        mockResponse?.committed = false
        mockResponse?.reset()
        mockSession?.clearAttributes()
        mockSession?.setNew(true)

        forwardArgs?.clear()
        redirectArgs?.clear()
        renderArgs?.clear()
        mockParams?.clear()
        mockFlash?.clear()
    }

    protected newInstance() {
        def instance = testClass.newInstance()

        forwardArgs = instance.forwardArgs
        redirectArgs = instance.redirectArgs
        renderArgs = instance.renderArgs
        mockRequest = instance.request
        mockResponse = instance.response
        mockSession = instance.session

        mockParams = instance.params
        mockFlash = instance.flash

        bindMockWebRequest(mockRequest, mockResponse)

        return instance
    }

    protected def bindMockWebRequest(GrailsMockHttpServletRequest mockRequest, GrailsMockHttpServletResponse mockResponse) {
        MockApplicationContext ctx = new MockApplicationContext()
        def application = new DefaultGrailsApplication([testClass] as Class[], getClass().classLoader)
        application.initialise()
        ctx.registerMockBean(testClass.name, testClass.newInstance())
        def lookup = new TagLibraryLookup(applicationContext: ctx, grailsApplication: application)
        lookup.afterPropertiesSet()
        ctx.registerMockBean("gspTagLibraryLookup", lookup)
        ctx.registerMockBean(GroovyPagesUriService.BEAN_ID, new DefaultGroovyPagesUriService())
        mockRequest.servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT, ctx)
        mockRequest.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ctx)

        webRequest = new GrailsWebRequest(mockRequest, mockResponse, mockRequest.servletContext)

        mockRequest.setAttribute(GrailsApplicationAttributes.WEB_REQUEST, webRequest)
        RequestContextHolder.setRequestAttributes(webRequest)
    }
}
