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

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.DefaultGrailsControllerClass
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.view.NullView
import org.codehaus.groovy.grails.web.util.WebUtils

import org.springframework.beans.factory.InitializingBean
import org.springframework.util.AntPathMatcher
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.UrlPathHelper

/**
 * Adapter between a FilterConfig object and a Spring HandlerInterceptor.
 * @author mike
 * @author Graeme Rocher
 */
class FilterToHandlerAdapter implements HandlerInterceptor, InitializingBean {
    def filterConfig
    def configClass

    def controllerRegex
    def controllerExcludeRegex
    def actionRegex
    def actionExcludeRegex
    def uriPattern
    def uriExcludePattern
    def urlPathHelper = new UrlPathHelper()
    def pathMatcher = new AntPathMatcher()
    def useRegex  // standard regex
    def invertRule // invert rule
    def useRegexFind // use find instead of match
    def dependsOn = [] // any filters that need to be processed before this one

    void afterPropertiesSet() {
        def scope = filterConfig.scope

        useRegex = scope.regex
        invertRule = scope.invert
        useRegexFind = scope.find

        if (scope.controller) {
            controllerRegex = Pattern.compile((useRegex)?scope.controller:scope.controller.replaceAll("\\*", ".*"))
        }
        else {
            controllerRegex = Pattern.compile(".*")
        }

        if(scope.controllerExclude) {
            controllerExcludeRegex = Pattern.compile((useRegex)?scope.controllerExclude:scope.controllerExclude.replaceAll("\\*", ".*"))
        }

        if (scope.action) {
            actionRegex = Pattern.compile((useRegex)?scope.action:scope.action.replaceAll("\\*", ".*"))
        }
        else {
            actionRegex = Pattern.compile(".*")
        }

        if(scope.actionExclude) {
            actionExcludeRegex = Pattern.compile((useRegex)?scope.actionExclude:scope.actionExclude.replaceAll("\\*", ".*"))
        }

        if (scope.uri) {
            uriPattern = scope.uri.toString()
        }
        if(scope.uriExclude) {
            uriExcludePattern = scope.uriExclude.toString()
        }
    }

    /**
     * Returns the name of the controller targeted by the given request.
     */
    String controllerName(request) {
        return request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE)?.toString()
    }

    /**
     * Returns the name of the action targeted by the given request.
     */
    String actionName(request) {
        return request.getAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE)?.toString()
    }

    String uri(HttpServletRequest request) {
        def uri = request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE)
        if (!uri) uri = request.getRequestURI()
        return uri.substring(request.getContextPath().length())
    }

    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) {
        if (filterConfig.before) {

            String controllerName = controllerName(request)
            String actionName = actionName(request)
            String uri = uri(request)

            if (!accept(controllerName, actionName, uri)) return true

            def callable = filterConfig.before.clone()
            def result = callable.call()
            if (result instanceof Boolean) {
                if (!result && filterConfig.modelAndView) {
                    renderModelAndView(filterConfig, request, response, controllerName)
                }
                return result
            }
        }

        return true
    }

    void postHandle(HttpServletRequest request, HttpServletResponse response, o, ModelAndView modelAndView) {
        if (!filterConfig.after) {
            return
        }

        String controllerName = controllerName(request)
        String actionName = actionName(request)
        String uri = uri(request)

        if (!accept(controllerName, actionName, uri)) return

        def callable = filterConfig.after.clone()
        def result = callable.call(modelAndView?.model)
        if (result instanceof Boolean) {
            // if false is returned don't render a view
            if (!result) {
                modelAndView.viewName = null
                modelAndView.view = new NullView(response.contentType)
            }
        }
        else if (filterConfig.modelAndView && modelAndView) {
            if (filterConfig.modelAndView.viewName) {
                modelAndView.viewName = filterConfig.modelAndView.viewName
            }
            modelAndView.model.putAll(filterConfig.modelAndView.model)
        }
        else if (filterConfig.modelAndView?.viewName) {
            renderModelAndView(filterConfig, request, response, controllerName)
        }
    }

    private renderModelAndView(delegate, request, response, controllerName) {
        def viewResolver = WebUtils.lookupViewResolver(delegate.servletContext)
        def view
        ModelAndView modelAndView = delegate.modelAndView
        if (modelAndView.viewName) {
            view = WebUtils.resolveView(request, modelAndView.viewName, controllerName, viewResolver)
        }
        else if (modelAndView.view) {
            view = modelAndView.view
        }
        view?.render(modelAndView.model, request, response)
    }

    void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) throws java.lang.Exception {
        if (!filterConfig.afterView) {
            return
        }

        String controllerName = controllerName(request)
        String actionName = actionName(request)
        String uri = uri(request)

        if (!accept(controllerName, actionName, uri)) return

        def callable = filterConfig.afterView.clone()
        callable.call(e)
    }

    boolean accept(String controllerName, String actionName, String uri) {
        boolean matched=true

        if (uriPattern) {
            matched = pathMatcher.match(uriPattern, uri)
            if(matched && uriExcludePattern) {
                matched = !pathMatcher.match(uriExcludePattern, uri)
            }
        }
        else if (controllerRegex && actionRegex) {
            if (controllerName == null) {
                matched = ('/' == uri)
            }
            if (matched) {
                matched = doesMatch(controllerRegex, controllerName)
                if(matched && controllerExcludeRegex) {
                    matched = !doesMatch(controllerExcludeRegex, controllerName)
                }
            }
            if (matched && filterConfig.scope.action) {
                if (!actionName && controllerName) {
                    def controllerClass = ApplicationHolder.application?.getArtefactByLogicalPropertyName(
                       DefaultGrailsControllerClass.CONTROLLER, controllerName)
                    actionName = controllerClass?.getDefaultAction()
                }
                matched = doesMatch(actionRegex, actionName)
                if(matched && actionExcludeRegex) {
                    matched = !doesMatch(actionExcludeRegex, actionName)
                }
            }
        }

        invertRule ? !matched : matched
    }

    boolean doesMatch(Pattern pattern, CharSequence string) {
        def matcher=pattern.matcher(string ?: '')
        useRegexFind ? matcher.find() : matcher.matches()
    }

    String toString() {
        return "FilterToHandlerAdapter[$filterConfig, $configClass]"
    }
}
