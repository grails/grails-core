/*
 * Copyright 2015 original authors
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
package org.grails.plugins.web.interceptors

import grails.artefact.Interceptor
import grails.interceptors.Matcher
import grails.util.GrailsNameUtils
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.OrderComparator
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse



/**
 * Adapts Grails {@link Interceptor} instances to the Spring {@link HandlerInterceptor} interface
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsInterceptorHandlerInterceptorAdapter implements HandlerInterceptor {

    private static final Log LOG = LogFactory.getLog(Interceptor)

    protected List<Interceptor> interceptors = []
    protected List<Interceptor> reverseInterceptors = []

    @Autowired(required = false)
    @CompileDynamic
    void setInterceptors(Interceptor[] interceptors) {
        this.interceptors = interceptors.sort(new OrderComparator()) as List<Interceptor>
        this.reverseInterceptors = this.interceptors.reverse()
        if(LOG.isDebugEnabled()) {
            LOG.debug("Computed interceptor execution order:")
            for(Interceptor i in interceptors) {
                LOG.debug("- ${GrailsNameUtils.getLogicalPropertyName(i.getClass().name, "Interceptor")} (order: ${i.order}) ")
            }
        }
    }

    @Override
    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(interceptors) {
            for(i in interceptors) {
                if(i.doesMatch()) {
                    if( !i.before() ) {
                        return false
                    }
                }
            }
        }
        return true
    }

    @Override
    void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if(reverseInterceptors) {
            if(modelAndView != null) {
                request.setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, modelAndView)
            }
            for(i in reverseInterceptors) {
                if(i.doesMatch()) {
                    if( !i.after() ) {
                        modelAndView.setView(null)
                        modelAndView.setViewName(null)
                        break
                    }
                }
            }
        }
    }

    @Override
    void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        request.setAttribute(Matcher.THROWABLE, ex)
        if(reverseInterceptors) {
            for(i in reverseInterceptors) {
                if(i.doesMatch(request)) {
                    i.afterView()
                }
            }
        }
    }
}
