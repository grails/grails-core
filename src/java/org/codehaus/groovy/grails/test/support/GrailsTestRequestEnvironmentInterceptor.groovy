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

    ApplicationContext applicationContext
    
    GrailsTestRequestEnvironmentInterceptor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    /**
     * Establishes a mock request environment
     */
    void init() {
        GrailsWebRequest webRequest = GrailsWebUtil.bindMockWebRequest(applicationContext)
        ServletContextHolder.servletContext = webRequest.servletContext
        webRequest.servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, applicationContext)
    }

    /**
     * Removes the mock request environment
     */    
    void destroy() {
        RequestContextHolder.requestAttributes = null
        ServletContextHolder.servletContext = null
    }
    
    /**
     * Calls init() before and destroy() after invoking {@code body}.
     */    
    void doInRequestEnvironment(Closure body) {
        init() 
        try {
            body()
        } finally {
            destroy()
        }
    }

}