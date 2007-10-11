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
import java.util.regex.Pattern
import org.springframework.web.util.UrlPathHelper
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.WebRequestDelegatingRequestContext
import org.codehaus.groovy.grails.web.util.WebUtils

/**
 * @author mike
 * @author Graeme Rocher
 */
class FilterToHandlerAdapter implements HandlerInterceptor {
    def filterConfig;
    def configClass;

    def controllerRegex;
    def actionRegex;
    def uriRegex;
    def urlPathHelper = new UrlPathHelper()

    private static final LOG = LogFactory.getLog(FilterToHandlerAdapter)

    String controllerName(request) {
        return request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE).toString()
    }

    String actionName(request) {
        return request.getAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE).toString()
    }

    String uri(HttpServletRequest request) {
        def uri = request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE)
        if(!uri) uri = request.getRequestURI()
        return uri.substring(request.getContextPath().length())
    }

    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) {
        if (filterConfig.before) {

            String controllerName = controllerName(request)
            String actionName = actionName(request)
            String uri = uri(request)

            if (!accept(controllerName, actionName, uri)) return true;

            def callable = filterConfig.before.clone()
            FilterActionDelegate delegate = new FilterActionDelegate()
            callable.delegate = delegate
            callable.resolveStrategy = Closure.DELEGATE_FIRST
            def result = callable.call();
            if(result instanceof Boolean) {
                if(!result && delegate.modelAndView) {
                    renderModelAndView(delegate, request, response, controllerName)
                }
                return result
            }
        }

        return true;
    }

    void postHandle(HttpServletRequest request, HttpServletResponse response, o, ModelAndView modelAndView) throws java.lang.Exception {
        if (filterConfig.after) {

            String controllerName = controllerName(request)
            String actionName = actionName(request)
            String uri = uri(request)

            if (!accept(controllerName, actionName, uri)) return;

            def callable = filterConfig.after.clone()
            FilterActionDelegate delegate = new FilterActionDelegate()
            callable.delegate = delegate
            callable.resolveStrategy = Closure.DELEGATE_FIRST

            callable.call(modelAndView?.getModel());
            if(delegate.modelAndView && modelAndView) {
                if(delegate.modelAndView.viewName) {
                    modelAndView.viewName = delegate.modelAndView.viewName
                }
                modelAndView.getModel().putAll(delegate.modelAndView.getModel())
            }
            else if(delegate.modelAndView?.viewName) {
                renderModelAndView(delegate, request, response, controllerName)
            }
        }
    }

    private renderModelAndView(FilterActionDelegate delegate, request, response, controllerName) {
        def viewResolver = WebUtils.lookupViewResolver(delegate.servletContext)
        def view
        ModelAndView modelAndView = delegate.modelAndView
        if (modelAndView.viewName)
            view = WebUtils.resolveView(request, modelAndView.viewName, controllerName, viewResolver)
        else if (modelAndView.view)
            view = modelAndView.view
        view?.render(modelAndView.getModel(), request, response)
    }

    void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) throws java.lang.Exception {
        if (filterConfig.afterComplete) {

            String controllerName = controllerName(request)
            String actionName = actionName(request)
            String uri = uri(request)

            if (!accept(controllerName, actionName, uri)) return;
            def callable = filterConfig.afterView.clone()
            callable.delegate = new FilterActionDelegate()
            callable.resolveStrategy = Closure.DELEGATE_FIRST
            
            callable.call(e);
        }
    }

    boolean accept(String controllerName, String actionName, String uri) {
        if (controllerRegex == null || actionRegex == null) {
            def scope = filterConfig.scope

            if (scope.controller) {
                controllerRegex = Pattern.compile(scope.controller.replaceAll("\\*", ".*"))
            }
            else {
                controllerRegex = Pattern.compile(".*")
            }

            if (scope.action) {
                actionRegex = Pattern.compile(scope.action.replaceAll("\\*", ".*"))
            }
            else {
                actionRegex = Pattern.compile(".*")
            }

            if (scope.uri) {
                uriRegex = Pattern.compile(
                        scope.uri.
                                replaceAll("\\*\\*", ".*").
                                replaceAll("\\*", "[^/]*")
                        )
            }
            else {
                uriRegex = Pattern.compile(".*")
            }
        }

        return controllerRegex.matcher(controllerName).matches() && actionRegex.matcher(actionName).matches() && uriRegex.matcher(uri).matches()
    }

    String toString() {
        return "FilterToHandlerAdapter[$filterConfig, $configClass]"
    }
}