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
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
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

    ServletRenderContext(GrailsWebRequest webRequest) {
        this.webRequest = webRequest
    }

    @Override
    Writer getWriter() {
        webRequest.currentResponse.writer
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
        modelAndView.model.putAll(model)
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
