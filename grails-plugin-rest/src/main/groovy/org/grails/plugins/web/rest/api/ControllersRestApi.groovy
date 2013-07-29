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

import grails.rest.Resource
import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import javax.servlet.http.HttpServletResponse

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.codehaus.groovy.grails.plugins.web.api.ControllersMimeTypesApi
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageLocator
import org.grails.plugins.web.rest.render.DefaultRendererRegistry
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

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

    @Autowired
    GroovyPageLocator groovyPageLocator

    ControllersRestApi(RendererRegistry rendererRegistry, ControllersApi controllersApi, ControllersMimeTypesApi mimeTypesApi) {
        this.controllersApi = controllersApi
        this.mimeTypesApi = mimeTypesApi
        this.rendererRegistry = rendererRegistry
    }

    /**
     * Same as {@link ControllersRestApi#respond(java.lang.Object, java.lang.Object, java.util.Map)}, but here to support Groovy named arguments
     */
    public <T> Object respond(controller, Map args, value) {
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
    Object respond(Object controller, Object value, Map args = [:]) {
        Integer statusCode
        if (args.status) {
            final Object statusValue = args.status
            if (statusValue instanceof Number) {
                statusCode = statusValue.intValue()
            } else {
                statusCode = statusValue.toString().toInteger()
            }
        }
        if (value == null) {
            return render(controller,[status:statusCode ?: 404 ])
        }

        final webRequest = getWebRequest(controller)
        List<String> formats = calculateFormats(controller, webRequest.actionName, value, args)
        final response = webRequest.getCurrentResponse()
        MimeType mimeType = getResponseFormat(response)
        def registry = rendererRegistry
        if (registry == null) {
            registry = new DefaultRendererRegistry()
            registry.initialize()
        }
        if (mimeType == MimeType.ALL && formats) {
            final allMimeTypes = MimeType.getConfiguredMimeTypes()
            final firstFormat = formats[0]
            mimeType = allMimeTypes.find { MimeType mt -> mt.extension == firstFormat}
        }

        if (mimeType && formats.contains(mimeType.extension)) {
            Errors errors = value.hasProperty(GrailsDomainClassProperty.ERRORS) ? getDomainErrors(value) : null

            Renderer renderer
            if (errors && errors.hasErrors()) {
                def target = errors instanceof BeanPropertyBindingResult ? errors.getTarget() : null
                Renderer<Errors> errorsRenderer = registry.findContainerRenderer(mimeType, Errors.class, target)
                if (errorsRenderer) {
                    final context = new ServletRenderContext(webRequest, (Map)args.model)
                    if (args.view) {
                        context.viewName = args.view
                    }
                    if(statusCode != null) {
                        context.setStatus(HttpStatus.valueOf(statusCode))
                    }
                    return errorsRenderer.render(errors, context)
                }

                return render(controller,[status: statusCode ?: 404 ])
            }

            final valueType = value.getClass()
            if (registry.isContainerType(valueType)) {
                renderer = registry.findContainerRenderer(mimeType,valueType, value)
                if (renderer == null) {
                    renderer = registry.findRenderer(mimeType, value)
                }
            } else {
                renderer = registry.findRenderer(mimeType, value)
            }

            if (renderer) {
                final context = new ServletRenderContext(webRequest, args)
                if(statusCode != null) {
                    context.setStatus(HttpStatus.valueOf(statusCode))
                }
                renderer.render(value, context)
            } else {
                render(controller,[status: statusCode ?: HttpStatus.UNSUPPORTED_MEDIA_TYPE.value() ])
            }
        } else {
            render(controller,[status: statusCode ?: HttpStatus.UNSUPPORTED_MEDIA_TYPE.value() ])
        }
    }

    protected List<String> calculateFormats(controller, String actionName, value, Map args) {
        if (args.formats) {
            return (List<String>) args.formats
        }

        if (controller.hasProperty(PROPERTY_RESPONSE_FORMATS)) {
            final responseFormatsProperty = ((GroovyObject) controller).getProperty(PROPERTY_RESPONSE_FORMATS)
            if (responseFormatsProperty instanceof List) {
                return (List<String>) responseFormatsProperty
            }
            if ((responseFormatsProperty instanceof Map) && actionName) {
                Map<String, Object> responseFormatsMap = (Map<String, Object>) responseFormatsProperty

                final responseFormatsForAction = responseFormatsMap.get(actionName)
                if (responseFormatsForAction instanceof List) {
                    return (List<String>) responseFormatsForAction
                }
                return getDefaultResponseFormats(value)
            }
            return getDefaultResponseFormats(value)
        }
        return getDefaultResponseFormats(value)
    }

    protected List<String> getDefaultResponseFormats(value) {
        Resource resAnn = value != null ? value.getClass().getAnnotation(Resource) : null
        if (resAnn) {
            return resAnn.formats().toList()
        }
        return MimeType.getConfiguredMimeTypes().collect { MimeType mt -> mt.extension }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected Errors getDomainErrors(object) {
        if (object instanceof Errors) {
            return object
        }
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
