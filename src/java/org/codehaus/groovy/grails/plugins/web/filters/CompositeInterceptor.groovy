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

import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.web.servlet.ModelAndView
import org.apache.commons.logging.LogFactory

/**
 * A HandlerInterceptor that is composed of other HandlerInterceptor instances
 * 
 * @author mike
 * @author Graeme Rocher
 */
class CompositeInterceptor implements HandlerInterceptor {
    static final LOG = LogFactory.getLog(CompositeInterceptor)
    
    def handlers

    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) {
        if (LOG.isDebugEnabled()) LOG.debug "preHandle ${request}, ${response}, ${o}"

        for (handler in handlers) {
            if (!handler.preHandle(request, response, o)) return false;
        }
        return true;
    }

    void postHandle(HttpServletRequest request, HttpServletResponse response,Object o, ModelAndView modelAndView) throws java.lang.Exception {
        if (LOG.isDebugEnabled()) LOG.debug "postHandle ${request}, ${response}, ${o}, ${modelAndView}"

        handlers.reverseEach{ handler ->
            handler.postHandle(request, response, o, modelAndView);
        }
    }

    void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) throws java.lang.Exception {
        if (LOG.isDebugEnabled()) LOG.debug "afterCompletion ${request}, ${response}, ${o}, ${e}"

        handlers.reverseEach{ handler ->
            handler.afterCompletion(request, response, o, e);
        }
    }
}