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
package org.grails.plugins.web.filters

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.grails.web.servlet.mvc.GrailsWebRequest

import java.util.regex.Pattern

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import grails.web.util.GrailsApplicationAttributes
import org.grails.web.servlet.view.NullView
import org.grails.web.util.WebUtils

import org.springframework.beans.factory.InitializingBean
import org.springframework.util.AntPathMatcher
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.UrlPathHelper
import grails.core.support.GrailsApplicationAware
import grails.core.GrailsApplication

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
    Pattern controllerNamespaceRegex
    Pattern controllerNamespaceExcludeRegex

    Pattern actionRegex
    Pattern actionExcludeRegex
    String uriPattern
    String uriExcludePattern
    UrlPathHelper urlPathHelper = new UrlPathHelper()
    AntPathMatcher pathMatcher = new AntPathMatcher()
    boolean useRegex  // standard regex
    boolean invertRule // invert rule
    boolean useRegexFind // use find instead of match
    List dependsOn = [] // any filters that need to be processed before this one

    GrailsApplication grailsApplication

    void afterPropertiesSet() {
        def scope = filterConfig.scope

        useRegex = scope.regex
        invertRule = scope.invert
        useRegexFind = scope.find


        def controller = scope.controller
        if (controller) {
            controllerRegex = Pattern.compile( useRegex ? controller.toString() : controller.toString().replaceAll("\\*", ".*") )
        }
        else {
            controllerRegex = Pattern.compile(".*")
        }


        def controllerExclude = scope.controllerExclude
        if (controllerExclude) {
            controllerExcludeRegex = Pattern.compile( useRegex ? controllerExclude.toString() : controllerExclude.toString().replaceAll("\\*", ".*") )
        }


        def namespace = scope.namespace
        if(namespace) {
            controllerNamespaceRegex = Pattern.compile( useRegex ? namespace.toString() : namespace.toString().replaceAll("\\*", ".*"))
        } else {
            controllerNamespaceRegex = Pattern.compile(".*")
        }


        def namespaceExclude = scope.namespaceExclude
        if(namespaceExclude) {
            controllerNamespaceExcludeRegex = Pattern.compile( useRegex ? namespaceExclude.toString() : namespaceExclude.toString().replaceAll("\\*", ".*"))
        }


        def action = scope.action
        if (action) {
            actionRegex = Pattern.compile( useRegex ? action.toString() : action.toString().replaceAll("\\*", ".*") )
        }
        else {
            actionRegex = Pattern.compile(".*")
        }


        def actionExclude = scope.actionExclude
        if (actionExclude) {
            actionExcludeRegex = Pattern.compile(useRegex ? actionExclude.toString() : actionExclude.toString().replaceAll("\\*", ".*") )
        }


        def uri = scope.uri
        if (uri) {
            uriPattern = uri.toString()
        }

        def uriExclude = scope.uriExclude
        if (uriExclude) {
            uriExcludePattern = uriExclude.toString()
        }
    }

    /**
     * Returns the name of the controller targeted by the given request.
     */
    String controllerName(HttpServletRequest request) {
        return request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE)?.toString()
    }

    GrailsControllerClass controllerClass(HttpServletRequest request) {
        return (GrailsControllerClass) request.getAttribute(GrailsApplicationAttributes.GRAILS_CONTROLLER_CLASS)
    }

    /**
    * Returns the namespace of the controller targeted by the given request.
    **/
    String controllerNamespace(HttpServletRequest request) {
     return request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAMESPACE_ATTRIBUTE)?.toString()
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
            def controllerClass   = controllerClass(request)
            String controllerNamespace = controllerNamespace(request)
            String actionName = actionName(request)
            String uri = uri(request)

            if (!accept(controllerName, actionName, uri, controllerNamespace, controllerClass)) return true

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
        final filterConfig = this.filterConfig
        if (!filterConfig.after) {
            return
        }

        String controllerName = controllerName(request)
        def controllerClass = controllerClass(request)
        String controllerNamespace = controllerNamespace(request)
        String actionName = actionName(request)
        String uri = uri(request)

        if (!accept(controllerName, actionName, uri, controllerNamespace, controllerClass)) return

        def callable = (Closure) filterConfig.after.clone()
        def currentModel = modelAndView?.model
        if (currentModel == null) {
            final templateModel = request.getAttribute(GrailsApplicationAttributes.TEMPLATE_MODEL)
            if (templateModel != null) {
                currentModel = templateModel
            }
        }
        def result = callable.call(currentModel)
        final filterConfigModel = filterConfig.modelAndView
        if (result instanceof Boolean) {
            // if false is returned don't render a view
            if (!result) {
                modelAndView.viewName = null
                modelAndView.view = new NullView(response.contentType)
            }
        }
        else if (filterConfigModel && modelAndView) {
            if (filterConfigModel.viewName) {
                modelAndView.viewName = filterConfigModel.viewName
            }
            modelAndView.model.putAll(filterConfigModel.model)
        }
        else if (filterConfigModel?.viewName) {
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
        String controllerNamespace = controllerNamespace(request)
        def controller = this.controllerClass(request)
        String actionName = actionName(request)
        String uri = uri(request)

        if (!accept(controllerName, actionName, uri, controllerNamespace, controller)) return

        def callable = (Closure)filterConfig.afterView.clone()
        callable.call(e)
    }

    boolean accept(String controllerName, String actionName, String uri, String controllerNamespace, GrailsControllerClass controllerClass) {
        boolean matched=true

        uri = uri.replace(';', '')
        final pathMatcher = this.pathMatcher
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
                if(matched && controllerNamespaceRegex) {
                    if(!controllerNamespace) {
                        controllerNamespace = ''
                    }
                    matched = doesMatch(controllerNamespaceRegex, controllerNamespace)
                    if(matched && controllerNamespaceExcludeRegex) {
                        matched = !doesMatch(controllerNamespaceExcludeRegex, controllerNamespace)
                    }
                }
            }
            if (matched && (filterConfig.scope.action)) {
                if (!actionName && controllerName) {
                    if(controllerClass) {
                        actionName = controllerClass.defaultAction
                    }
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
