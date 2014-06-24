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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

import java.util.regex.Pattern

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.codehaus.groovy.grails.commons.DefaultGrailsControllerClass
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.view.NullView
import org.codehaus.groovy.grails.web.util.WebUtils

import org.springframework.beans.factory.InitializingBean
import org.springframework.util.AntPathMatcher
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.UrlPathHelper
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * Adapter between a FilterConfig object and a Spring HandlerInterceptor.
 * @author mike
 * @author Graeme Rocher
 */
@CompileStatic
class FilterToHandlerAdapter implements HandlerInterceptor, InitializingBean, GrailsApplicationAware {
    FilterConfig filterConfig
    def configClass

    Pattern controllerRegex
    Pattern controllerExcludeRegex
    Pattern actionRegex
    Pattern actionExcludeRegex
    String uriPattern
    String uriExcludePattern
    UrlPathHelper urlPathHelper = new UrlPathHelper()
    AntPathMatcher pathMatcher = new AntPathMatcher()
    def useRegex  // standard regex
    def invertRule // invert rule
    def useRegexFind // use find instead of match
    def dependsOn = [] // any filters that need to be processed before this one

    GrailsApplication grailsApplication

    void afterPropertiesSet() {
        def scope = filterConfig.scope

        useRegex = scope.regex
        invertRule = scope.invert
        useRegexFind = scope.find

        if (scope.controller) {
            controllerRegex = Pattern.compile( useRegex ? scope.controller.toString() : scope.controller.toString().replaceAll("\\*", ".*") )
        }
        else {
            controllerRegex = Pattern.compile(".*")
        }

        if (scope.controllerExclude) {
            controllerExcludeRegex = Pattern.compile( useRegex ? scope.controllerExclude.toString() : scope.controllerExclude.toString().replaceAll("\\*", ".*") )
        }

        if (scope.action) {
            actionRegex = Pattern.compile( useRegex ? scope.action.toString() : scope.action.toString().replaceAll("\\*", ".*") )
        }
        else {
            actionRegex = Pattern.compile(".*")
        }

        if (scope.actionExclude) {
            actionExcludeRegex = Pattern.compile(useRegex ? scope.actionExclude.toString() : scope.actionExclude.toString().replaceAll("\\*", ".*") )
        }

        if (scope.uri) {
            uriPattern = scope.uri.toString()
        }
        if (scope.uriExclude) {
            uriExcludePattern = scope.uriExclude.toString()
        }
    }

    /**
     * Returns the name of the controller targeted by the given request.
     */
    String controllerName(HttpServletRequest request) {
        return request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE)?.toString()
    }

    /**
     * Returns the name of the action targeted by the given request.
     */
    String actionName(HttpServletRequest request) {
        return request.getAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE)?.toString()
    }

    String uri(HttpServletRequest request) {
        String uri = request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE)?.toString()
        if (!uri) uri = request.getRequestURI()
        return uri.substring(request.getContextPath().length())
    }

    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) {
        if (filterConfig.before) {

            String controllerName = controllerName(request)
            String actionName = actionName(request)
            String uri = uri(request)

            if (!accept(controllerName, actionName, uri)) return true

            def callable = (Closure)filterConfig.before.clone()
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

        def callable = (Closure)filterConfig.after.clone()
        def currentModel = modelAndView?.model
        if (currentModel == null) {
            final templateModel = request.getAttribute(GrailsApplicationAttributes.TEMPLATE_MODEL)
            if (templateModel != null) {
                currentModel = templateModel
            }
        }
        def result = callable.call(currentModel)
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

    @CompileDynamic
    private renderModelAndView(delegate,  HttpServletRequest request, HttpServletResponse response, String controllerName) {
        def webRequest = GrailsWebRequest.lookup(request)
        def viewResolver = WebUtils.lookupViewResolver(webRequest.servletContext)
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

        def callable = (Closure)filterConfig.afterView.clone()
        callable.call(e)
    }

    boolean accept(String controllerName, String actionName, String uri) {
        boolean matched=true

        uri = uri.replace(';', '')
        if (uriPattern) {
            matched = pathMatcher.match(uriPattern, uri)
            if (matched && uriExcludePattern) {
                matched = !pathMatcher.match(uriExcludePattern, uri)
            }
        }
        else if (uriExcludePattern) {
            matched = !pathMatcher.match(uriExcludePattern, uri)
        }
        else if (controllerRegex && actionRegex) {
            if (controllerName == null) {
                matched = ('/' == uri)
            }
            if (matched) {
                matched = doesMatch(controllerRegex, controllerName)
                if (matched && controllerExcludeRegex) {
                    matched = !doesMatch(controllerExcludeRegex, controllerName)
                }
            }
            if (matched && filterConfig.scope.action) {
                if (!actionName && controllerName) {
                    def controllerClass = (GrailsControllerClass)grailsApplication?.getArtefactByLogicalPropertyName(
                       DefaultGrailsControllerClass.CONTROLLER, controllerName)
                    actionName = controllerClass?.getDefaultAction()
                }
                matched = doesMatch(actionRegex, actionName)
                if (matched && actionExcludeRegex) {
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

    void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }
}
