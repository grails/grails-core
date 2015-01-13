/*
 * Copyright 2014 the original author or authors.
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
package grails.artefact

import grails.events.Events
import org.grails.web.util.GrailsApplicationAttributes
import groovy.transform.CompileStatic

import javax.servlet.AsyncContext
import javax.servlet.http.HttpServletRequest

import org.grails.plugins.web.async.GrailsAsyncContext
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder

/**
 * 
 * @author Jeff Brown
 * @since 3.0
 *
 */
@CompileStatic
trait AsyncController extends Events {

    /**
     * Raw access to the Servlet 3.0 startAsync method
     *
     * @return a new {@link javax.servlet.AsyncContext}
     */
    AsyncContext startAsync() {
        GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes()
        HttpServletRequest request = webRequest.currentRequest
        def ctx = request.startAsync(request, webRequest.currentResponse)
        request.setAttribute(GrailsApplicationAttributes.ASYNC_STARTED, true)
        new GrailsAsyncContext(ctx, webRequest)
    }
}
