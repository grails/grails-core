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
package grails.artefact

import grails.artefact.controller.support.RequestForwarder
import grails.artefact.controller.support.ResponseRedirector
import grails.artefact.controller.support.ResponseRenderer
import grails.interceptors.Matcher
import grails.util.GrailsNameUtils
import grails.web.api.ServletAttributes
import grails.web.api.WebAttributes
import grails.web.databinding.DataBinder
import grails.web.mapping.UrlMappingInfo
import groovy.transform.CompileStatic
import org.grails.plugins.web.controllers.metaclass.RenderDynamicMethod
import org.grails.plugins.web.interceptors.GrailsInterceptorHandlerInterceptorAdapter
import org.grails.plugins.web.interceptors.InterceptorArtefactHandler
import org.grails.plugins.web.interceptors.UrlMappingMatcher
import org.grails.web.mapping.mvc.UrlMappingsHandlerMapping
import org.grails.web.servlet.mvc.exceptions.ControllerExecutionException
import org.grails.web.servlet.view.CompositeViewResolver
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.core.Ordered
import org.springframework.web.servlet.ModelAndView

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.regex.Pattern

/**
 * An interceptor can be used to intercept requests to controllers and URIs
 *
 * They replace the notion of filters from earlier versions of Grails, prior to Grails 3.0
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
trait Interceptor implements ResponseRenderer, ResponseRedirector, RequestForwarder, DataBinder, WebAttributes, ServletAttributes, Ordered {

    /**
     * The order the interceptor should execute in
     */
    int order = 0

    /**
     * The matchers defined by this interceptor
     */
    final Collection<Matcher> matchers = new ConcurrentLinkedQueue<>()

    /**
     * @return Whether the current interceptor does match
     */
    boolean doesMatch() {
        doesMatch(request)
    }
    /**
     * @return Whether the current interceptor does match
     */
    boolean doesMatch(HttpServletRequest request) {
        Collection<Matcher> allMatchers = matchers
        if (allMatchers.isEmpty()) {
            // default to map just the controller by convention
            def matcher = new UrlMappingMatcher(this)
            matcher.matches(controller: Pattern.compile(GrailsNameUtils.getLogicalPropertyName(getClass().simpleName, InterceptorArtefactHandler.TYPE)))
            allMatchers << matcher
        }

        HttpServletRequest req = request
        String ctxPath = req.contextPath
        String uri = req.requestURI
        String noCtxUri = uri - ctxPath
        boolean checkNoCtxUri = ctxPath && uri.startsWith(ctxPath)

        def matchedInfo = request.getAttribute(UrlMappingsHandlerMapping.MATCHED_REQUEST)

        UrlMappingInfo grailsMappingInfo = (UrlMappingInfo) matchedInfo

        for (Matcher matcher in allMatchers) {
            boolean matchUri = matcher.doesMatch(uri, grailsMappingInfo, req.method)
            boolean matchNoCtxUri = matcher.doesMatch(noCtxUri, grailsMappingInfo, req.method)

            if (matcher.isExclude() && matchUri && matchNoCtxUri) {
                // Exclude interceptors are special because with only one of the conditions being false the interceptor
                // won't be applied to the request
                return true
            } else if (!matcher.isExclude() && (matchUri || (checkNoCtxUri && matchNoCtxUri))) {
                return true
            }
        }
        return false
    }

    /**
     * Matches all requests
     */
    Matcher matchAll() {
        def matcher = new UrlMappingMatcher(this)
        matcher.matchAll()
        matchers << matcher
        return matcher
    }

    /**
     * @return The current model
     */
    Map<String, Object> getModel() {
        def modelAndView = (ModelAndView) currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
        return modelAndView?.modelMap
    }

    /**
     * Sets the model
     *
     * @param model The model to set
     */
    void setModel(Map<String, Object> model) {
        def request = currentRequestAttributes()
        def modelAndView = (ModelAndView) request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
        if (modelAndView == null) {
            modelAndView = new ModelAndView()
            request.setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, modelAndView, 0)
        }
        modelAndView.getModel().putAll(model)
    }

    /**
     * @return The current view
     */
    String getView() {
        def modelAndView = (ModelAndView) currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
        return modelAndView?.viewName
    }

    /**
     * Sets the view name
     *
     * @param view The name of the view
     */
    void setView(String view) {
        def request = currentRequestAttributes()
        def modelAndView = (ModelAndView) request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
        if (modelAndView == null) {
            modelAndView = new ModelAndView()
            request.setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, modelAndView, 0)
        }
        modelAndView.viewName = view
    }

    /**
     * Obtains the ModelAndView for the currently executing controller
     *
     * @return The ModelAndView
     */
    ModelAndView getModelAndView() {
        (ModelAndView) currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
    }

    /**
     * Sets the ModelAndView of the current controller
     *
     * @param mav The ModelAndView
     */
    void setModelAndView(ModelAndView mav) {
        currentRequestAttributes().setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, mav, 0)
    }

    /**
     * Obtains the exception thrown by an action execution
     *
     * @param t The exception or null if none was thrown
     */
    Throwable getThrowable() {
        def request = currentRequestAttributes()

        (Throwable) request.getAttribute(Matcher.THROWABLE, 0)
    }

    /**
     * Used to define a match. Example: match(controller:'book', action:'*')
     *
     * @param arguments The match arguments
     */
    Matcher match(Map arguments) {
        def matcher = new UrlMappingMatcher(this)
        matcher.matches(arguments)
        matchers << matcher
        return matcher
    }

    /**
     * Executed before a matched action
     *
     * @return Whether the action should continue and execute
     */
    boolean before() { true }

    /**
     * Executed after the action executes but prior to view rendering
     *
     * @return True if view rendering should continue, false otherwise
     */
    boolean after() { true }

    /**
     * Executed after view rendering completes
     *
     * @param t The exception instance if an exception was thrown, null otherwise
     */
    void afterView() {}

    /**
     * Sets a response header for the given name and value
     *
     * @param headerName The header name
     * @param headerValue The header value
     */
    void header(String headerName, headerValue) {
        if (headerValue != null) {
            final HttpServletResponse response = getResponse()
            response?.setHeader headerName, headerValue.toString()
        }
    }

    /**
     * Overridden render method that renders the view directly inside before()
     *
     * @param argMap
     */
    void render(Map argMap) {
        boolean isRenderView = argMap.containsKey(RenderDynamicMethod.ARGUMENT_VIEW)
        if (isRenderView) {
            def req = request
            def previous = (ModelAndView) req.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
            req.setAttribute(GrailsInterceptorHandlerInterceptorAdapter.INTERCEPTOR_RENDERED_VIEW, true)
            ResponseRenderer.super.render(argMap)
            def mav = (ModelAndView) req.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
            if (mav != null) {
                def view = applicationContext.getBean(CompositeViewResolver.BEAN_NAME, CompositeViewResolver).resolveView(mav.viewName, request.getLocale())
                if (view != null) {
                    def resp = response
                    view.render(mav.model, req, resp)
                    mav.clear()
                    previous?.clear()
                } else {
                    throw new ControllerExecutionException("No view found for name [$mav.viewName]")
                }
            }
        } else {
            ResponseRenderer.super.render(argMap)
        }
    }

}