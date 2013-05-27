/*
 * Copyright 2013 the original author or authors.
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

package org.grails.plugins.web.rest.api

import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.codehaus.groovy.grails.plugins.web.api.ControllersMimeTypesApi
import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.plugins.web.rest.render.DefaultRendererRegistry
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.springframework.validation.Errors

import javax.servlet.http.HttpServletResponse

/**
 * Provides the "respond" method in controllers
 *
 * @since 2.3
 * @author Graeme Rocher
 */
@CompileStatic
class ControllersRestApi {


    public static final String PROPERTY_RESPONSE_FORMATS = "responseFormats"
    protected @Delegate ControllersApi controllersApi
    protected @Delegate ControllersMimeTypesApi mimeTypesApi

    protected RendererRegistry rendererRegistry

    protected MimeType[] mimeTypes


    ControllersRestApi(RendererRegistry rendererRegistry, ControllersApi controllersApi, ControllersMimeTypesApi mimeTypesApi) {
        this.controllersApi = controllersApi
        this.mimeTypesApi = mimeTypesApi
        this.rendererRegistry = rendererRegistry
        mimeTypes = MimeType.getConfiguredMimeTypes()
    }

    /**
     * Same as {@link ControllersRestApi#respond(java.lang.Object, java.lang.Object, java.util.Map)}, but here to support Groovy named arguments
     */
    public <T> Object respond(Object controller, Map args, Object value) {
        respond(controller, value, args)
    }
    /**
     * The respond method will attempt to delivery an appropriate response for the
     * requested response format and value.
     *
     * If the value is null then a 404 will be returned. Otherwise the {@link RendererRegistry}
     * will be consulted for an appropriate response renderer for the requested response format.
     *
     * @param controller The controller
     * @param value The value
     * @param args The arguments
     * @return
     */
    public <T> Object respond(Object controller, Object value, Map args = [:]) {
        def statusCode = args.status ?: null
        if (value == null) {
            return render(controller,[status:statusCode ?: 404 ])
        }


        final webRequest = getWebRequest(controller)
        List<String> formats = calculateFormats(controller, webRequest.actionName, args)
        final response = webRequest.getCurrentResponse()
        MimeType mimeType = getResponseFormat(response)
        def registry = rendererRegistry
        if (registry == null) {
            registry = new DefaultRendererRegistry()
        }

        if (mimeType && formats.contains(mimeType.extension)) {

            Errors errors = value.hasProperty(GrailsDomainClassProperty.ERRORS) ? getDomainErrors(value) : null

            Renderer<T> renderer
            if (errors && errors.hasErrors()) {
                Renderer<Errors> errorsRenderer = registry.findRenderer(mimeType, errors)
                return errorsRenderer.render(errors, new ServletRenderContext(webRequest))
            }
            else {
                renderer = (Renderer<T>)registry.findRenderer(mimeType, (T)value)
            }


            if (renderer) {
                return renderer.render((T)value, new ServletRenderContext(webRequest))
            }
            else {
                // TODO: Check correct status code here
                return render(controller,[status: statusCode ?: 404 ])
            }
        }
        else {
            // TODO: Check correct status code here
            return render(controller,[status: statusCode ?: 404 ])
        }
    }

    List<String> calculateFormats(def controller, String actionName, Map args) {
        if (args.formats) {
            return (List<String>) args.formats
        }
        else if (controller.hasProperty(PROPERTY_RESPONSE_FORMATS)) {
            final responseFormatsProperty = ((GroovyObject) controller).getProperty(PROPERTY_RESPONSE_FORMATS)
            if (responseFormatsProperty instanceof List) {
                return (List<String>) responseFormatsProperty
            }
            else if ((responseFormatsProperty instanceof Map) && actionName) {
                Map<String, Object> responseFormatsMap = (Map<String, Object>) responseFormatsProperty

                final responseFormatsForAction = responseFormatsMap.get(actionName)
                if (responseFormatsForAction instanceof List) {
                    return (List<String>) responseFormatsForAction
                }
                else {
                    return MimeType.getConfiguredMimeTypes().collect { MimeType mt -> mt.extension }
                }
            }
        }
        else {
            return MimeType.getConfiguredMimeTypes().collect { MimeType mt -> mt.extension }
        }

    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected Errors getDomainErrors(def object) {
        final errors = object.errors
        if (errors instanceof Errors) {
            return errors
        }
        return null
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected MimeType getResponseFormat(HttpServletResponse response) {
        response.mimeType
    }
}
