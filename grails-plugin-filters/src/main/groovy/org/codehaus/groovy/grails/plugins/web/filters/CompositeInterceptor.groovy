/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.web.filters

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.metaclass.ForwardMethod

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

/**
 * Composed of other HandlerInterceptor instances.
 *
 * @author mike
 * @author Graeme Rocher
 */
@CompileStatic
class CompositeInterceptor implements HandlerInterceptor {

    static final Log LOG = LogFactory.getLog(CompositeInterceptor)

    List<HandlerInterceptor> handlers

    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) {
        if (LOG.isDebugEnabled()) LOG.debug "preHandle ${request}, ${response}, ${o}"

        for (handler in handlers) {
            if (!handler.preHandle(request, response, o)) {
                return false
            }

            // if forward is called, bail out
            if (request.getAttribute(ForwardMethod.CALLED) != null) {
                return false
            }

        }
        return true
    }

    void postHandle(HttpServletRequest request, HttpServletResponse response,Object o, ModelAndView modelAndView) throws Exception {
        if (LOG.isDebugEnabled()) LOG.debug "postHandle ${request}, ${response}, ${o}, ${modelAndView}"

        handlers.reverseEach { HandlerInterceptor handler ->
            handler.postHandle(request, response, o, modelAndView)
        }
    }

    void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) throws Exception {
        if (LOG.isDebugEnabled()) LOG.debug "afterCompletion ${request}, ${response}, ${o}, ${e}"

        handlers.reverseEach { HandlerInterceptor handler ->
            handler.afterCompletion(request, response, o, e)
        }
    }
}
