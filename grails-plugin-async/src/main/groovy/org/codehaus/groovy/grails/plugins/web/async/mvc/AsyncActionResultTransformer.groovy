/*
 * Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.web.async.mvc

import grails.async.Promise
import grails.async.PromiseList
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.plugins.web.async.GrailsAsyncContext
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.ActionResultTransformer
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.servlet.ModelAndView

import javax.servlet.AsyncContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Handles an Async response from a controller
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class AsyncActionResultTransformer implements ActionResultTransformer {


    private GrailsExceptionResolver exceptionResolver

    Object transformActionResult(GrailsWebRequest webRequest, String viewName, Object actionResult) {
        if (actionResult instanceof Promise) {

            final request = webRequest.getCurrentRequest()
            final response = webRequest.getResponse()

            def asyncContext = request.startAsync(request, response)
            request.setAttribute(GrailsApplicationAttributes.ASYNC_STARTED, true)
            asyncContext = new GrailsAsyncContext(asyncContext, webRequest)

            asyncContext.start {
                Promise p = (Promise) actionResult
                if (p instanceof PromiseList) {
                    p.onComplete { List results ->
                        handleComplete(request, response, asyncContext)
                    }
                } else {
                    p.onComplete {
                        if (it instanceof Map) {
                            def modelAndView = new ModelAndView(viewName, it)
                            asyncContext.getRequest().setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, modelAndView);

                            asyncContext.dispatch()
                        } else {
                            final modelAndView = asyncContext.getRequest().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
                            if (modelAndView) {
                                asyncContext.dispatch()
                            } else {
                                handleComplete(request, response, asyncContext)
                            }
                        }
                    }
                }
                p.onError { Throwable t ->
                    if (!response.isCommitted()) {
                        GrailsExceptionResolver exceptionResolver = createExceptionResolver(webRequest)
                        request.setAttribute(GrailsExceptionResolver.EXCEPTION_ATTRIBUTE, t)
                        exceptionResolver.resolveException(request, response, this, (Exception) t)
                        asyncContext.complete()
                    }
                }
            }
            return null;
        }
        return actionResult;
    }

    protected void handleComplete(HttpServletRequest request, HttpServletResponse response, AsyncContext asyncContext) {
        asyncContext.complete()
    }

    private GrailsExceptionResolver createExceptionResolver(GrailsWebRequest webRequest) {
        if (!exceptionResolver) {
            exceptionResolver = new GrailsExceptionResolver()
            exceptionResolver.servletContext = webRequest.servletContext
            exceptionResolver.grailsApplication = webRequest.attributes.grailsApplication
            exceptionResolver.mappedHandlers = [this] as Set
            def properties = new Properties()
            properties['java.lang.Exception'] = '/error'
            exceptionResolver.exceptionMappings = properties
        }
        return exceptionResolver
    }
}
