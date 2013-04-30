/*
 * Copyright 2009 the original author or authors.
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
package org.codehaus.groovy.grails.test.support

import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.web.context.request.RequestContextHolder

import org.springframework.context.ApplicationContext

import grails.util.GrailsWebUtil

/**
 * Establishes a “mock” request environment suitable for running tests in.
 */
class GrailsTestRequestEnvironmentInterceptor {

    static final String DEFAULT_CONTROLLER_NAME = 'test'

    ApplicationContext applicationContext

    GrailsTestRequestEnvironmentInterceptor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    /**
     * Establishes a mock request environment
     */
    void init(String controllerName = DEFAULT_CONTROLLER_NAME) {
        def request = new GrailsMockHttpServletRequest()
        def response = new GrailsMockHttpServletResponse()
        GrailsWebRequest webRequest = GrailsWebUtil.bindMockWebRequest(applicationContext, request, response)
        ServletContextHolder.servletContext = webRequest.servletContext
        webRequest.servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, applicationContext)
        webRequest.controllerName = controllerName
    }

    /**
     * Removes the mock request environment
     */
    void destroy() {
        RequestContextHolder.requestAttributes = null
        ServletContextHolder.servletContext = null
    }

    /**
     * Passes {@code body} to {@code doInRequestEnvironment(String,Closure)} with the {@code DEFAULT_CONTROLLER_NAME}.
     */
    void doInRequestEnvironment(Closure body) {
        doInRequestEnvironment(DEFAULT_CONTROLLER_NAME, body)
    }

    /**
     * Calls {@code init()} before and {@code destroy()} after invoking {@code body}.
     */
    void doInRequestEnvironment(String controllerName, Closure body) {
        init(controllerName)
        try {
            body()
        } finally {
            destroy()
        }
    }
}