/*
 * Copyright 2012 the original author or authors.
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
package org.grails.plugins.web.rest.render

import grails.rest.render.RenderContext
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.plugins.web.api.ResponseMimeTypesApi
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.ModelAndView

/**
 * RenderContext for the servlet environment
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class ServletRenderContext implements RenderContext{

    GrailsWebRequest webRequest
    Map<String, Object> arguments
    ResponseMimeTypesApi responseMimeTypesApi
    private String resourcePath

    ServletRenderContext(GrailsWebRequest webRequest) {
        this(webRequest, Collections.emptyMap())
    }

    ServletRenderContext(GrailsWebRequest webRequest, Map<String, Object> arguments) {
        this.webRequest = webRequest
        if(arguments != null) {
            this.arguments = Collections.unmodifiableMap(arguments)
        }
        else {
            this.arguments = Collections.emptyMap()
        }
    }

    @Override
    String getResourcePath() {
        if (resourcePath == null) {
            return WebUtils.getForwardURI(webRequest.request)
        }
        return resourcePath
    }

    void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    MimeType getAcceptMimeType() {
        final response = webRequest.response
        if (response.hasProperty('mimeType'))
            return response.mimeType
        return null
    }

    @Override
    Locale getLocale() {
        webRequest.locale
    }

    @Override
    Writer getWriter() {
        webRequest.currentResponse.writer
    }

    @Override
    HttpMethod getHttpMethod() {
        HttpMethod.valueOf(webRequest.currentRequest.method)
    }

    @Override
    void setStatus(HttpStatus status) {
        webRequest.response.setStatus(status.value())
    }

    @Override
    void setContentType(String contentType) {
        webRequest.currentResponse.contentType = contentType
    }

    @Override
    void setViewName(String viewName) {
        ModelAndView modelAndView = getModelAndView()
        modelAndView.setViewName(viewName)
    }

    @Override
    String getViewName() {
        final request = webRequest.currentRequest
        ModelAndView modelAndView = (ModelAndView) request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
        if (modelAndView) {
            return modelAndView.viewName
        }
        return null
    }

    protected ModelAndView getModelAndView() {
        final request = webRequest.currentRequest
        ModelAndView modelAndView = (ModelAndView) request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
        if (modelAndView == null) {
            modelAndView = new ModelAndView()
            request.setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, modelAndView)
        }
        modelAndView
    }

    @Override
    void setModel(Map model) {
        ModelAndView modelAndView = getModelAndView()
        final viewModel = modelAndView.model
        if (arguments?.model instanceof Map) {
            viewModel.putAll((Map)arguments.model)
        }
        viewModel.putAll(model)
    }

    @Override
    String getActionName() {
        webRequest.actionName
    }

    @Override
    String getControllerName() {
        webRequest.controllerName
    }
}
