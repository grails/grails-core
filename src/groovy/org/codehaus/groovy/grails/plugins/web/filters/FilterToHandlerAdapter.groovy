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

import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.UrlPathHelper
import org.springframework.util.AntPathMatcher
import org.codehaus.groovy.grails.web.servlet.view.NullView

/**
 * Adapter between a FilterConfig object and a Spring HandlerInterceptor.
 * @author mike
 * @author Graeme Rocher
 */
class FilterToHandlerAdapter implements HandlerInterceptor {
    def filterConfig;
    def configClass;

    def controllerRegex;
    def actionRegex;
    def uriPattern;
    def urlPathHelper = new UrlPathHelper()

    /**
     * Returns the name of the controller targeted by the given request.
     */
    String controllerName(request) {
        return request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE).toString()
    }

    /**
     * Returns the name of the action targeted by the given request.
     */
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
            def result = callable.call();
            if(result instanceof Boolean) {
                if(!result && filterConfig.modelAndView) {
                    renderModelAndView(filterConfig, request, response, controllerName)
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
            def result = callable.call(modelAndView?.model);
            if(result instanceof Boolean) {
                // if false is returned don't render a view
                if(!result) {
                    modelAndView.viewName = null
                    modelAndView.view = new NullView(response.contentType)
                }
            }
            else if(filterConfig.modelAndView && modelAndView) {
                if(filterConfig.modelAndView.viewName) {
                    modelAndView.viewName = filterConfig.modelAndView.viewName
                }
                modelAndView.model.putAll(filterConfig.modelAndView.model)
            }
            else if(filterConfig.modelAndView?.viewName) {
                renderModelAndView(filterConfig, request, response, controllerName)
            }

        }
    }

    private renderModelAndView(delegate, request, response, controllerName) {
        def viewResolver = WebUtils.lookupViewResolver(delegate.servletContext)
        def view
        ModelAndView modelAndView = delegate.modelAndView
        if (modelAndView.viewName)
            view = WebUtils.resolveView(request, modelAndView.viewName, controllerName, viewResolver)
        else if (modelAndView.view)
            view = modelAndView.view
        view?.render(modelAndView.model, request, response)
    }

    void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) throws java.lang.Exception {
        if (filterConfig.afterView) {

            String controllerName = controllerName(request)
            String actionName = actionName(request)
            String uri = uri(request)

            if (!accept(controllerName, actionName, uri)) return;
            def callable = filterConfig.afterView.clone()
            callable.call(e);
        }
    }

    def pathMatcher = new AntPathMatcher()
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
                uriPattern = scope.uri.toString()
            }
        }

        if(uriPattern) {
            return pathMatcher.match(uriPattern, uri)
        }
        else if(controllerRegex && actionRegex) {
            return controllerRegex.matcher(controllerName).matches() && actionRegex.matcher(actionName).matches()
        }
    }

    String toString() {
        return "FilterToHandlerAdapter[$filterConfig, $configClass]"
    }
}