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
import org.codehaus.groovy.grails.plugins.web.api.ResponseMimeTypesApi
import org.springframework.util.Assert

import javax.servlet.http.HttpServletResponse

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.codehaus.groovy.grails.plugins.web.api.ControllersMimeTypesApi
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageLocator
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
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
    @Autowired(required = false)
    ProxyHandler proxyHandler

    @Autowired
    GroovyPageLocator groovyPageLocator

    @Autowired
    ResponseMimeTypesApi responseMimeTypesApi

    ControllersRestApi(RendererRegistry rendererRegistry, ControllersApi controllersApi, ControllersMimeTypesApi mimeTypesApi) {
        this.controllersApi = controllersApi
        this.mimeTypesApi = mimeTypesApi
        this.rendererRegistry = rendererRegistry
    }

    /**
     * Same as {@link ControllersRestApi#respond(java.lang.Object, java.lang.Object, java.util.Map)}, but here to support Groovy named arguments
     */
    public <T> Object respond(controller, Map args, value) {
        internalRespond(controller, value, args)
    }

    /**
     * The respond method will attempt to delivery an appropriate response for the
     * requested response format and Map value.
     *
     * If the value is null then a 404 will be returned. Otherwise the {@link RendererRegistry}
     * will be consulted for an appropriate response renderer for the requested response format.
     *
     * @param controller The controller
     * @param value The value
     * @param namedArgs The arguments
     * @return
     */
    def respond(Object controller, Map namedArgs, Map value) {
        internalRespond controller, value, namedArgs
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
    def respond(Object controller, Object value, Map args = [:]) {
        internalRespond controller, value, args
    }

    private internalRespond(Object controller, Object value, Map args = [:]) {
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

        if (proxyHandler != null) {
            value = proxyHandler.unwrapIfProxy(value)
        }
        
        final webRequest = getWebRequest(controller)
        List<String> formats = calculateFormats(controller, webRequest.actionName, value, args)
        final response = webRequest.getCurrentResponse()
        MimeType[] mimeTypes = getResponseFormat(response)
        def registry = rendererRegistry
        if (registry == null) {
            registry = new DefaultRendererRegistry()
            registry.initialize()
        }

        Renderer renderer = null

        for(MimeType mimeType in mimeTypes) {
            if (mimeType == MimeType.ALL && formats) {
                final allMimeTypes = MimeType.getConfiguredMimeTypes()
                final firstFormat = formats[0]
                mimeType = allMimeTypes.find { MimeType mt -> mt.extension == firstFormat}
                if(mimeType) {
                    webRequest.currentRequest.setAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPE, mimeType)
                }
            }



            if (mimeType && formats.contains(mimeType.extension)) {
                Errors errors = value.hasProperty(GrailsDomainClassProperty.ERRORS) ? getDomainErrors(value) : null


                if (errors && errors.hasErrors()) {
                    def target = errors instanceof BeanPropertyBindingResult ? errors.getTarget() : null
                    if (proxyHandler != null && target != null) {
                        target = proxyHandler.unwrapIfProxy(target)
                    }
                    Renderer<Errors> errorsRenderer = registry.findContainerRenderer(mimeType, Errors.class, target)
                    if (errorsRenderer) {
                        final context = new ServletRenderContext(webRequest, [model: args.model])
                        if (args.view) {
                            context.viewName = args.view as String
                        }
                        if(statusCode != null) {
                            context.setStatus(HttpStatus.valueOf(statusCode))
                        }
                        errorsRenderer.render(errors, context)
                        return
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
            }

            if(renderer) break
        }


        if (renderer) {
            final context = new ServletRenderContext(webRequest, args)
            if(statusCode != null) {
                context.setStatus(HttpStatus.valueOf(statusCode))
            }
            renderer.render(value, context)
            if(context.wasWrittenTo() && !response.isCommitted()) {
                response.flushBuffer()
            }
            return
        }
        render(controller,[status: statusCode ?: HttpStatus.NOT_ACCEPTABLE.value() ])
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

    protected MimeType[] getResponseFormat(HttpServletResponse response) {
        Assert.notNull(responseMimeTypesApi, "No configured ResponseMimeTypesApi instance")
        responseMimeTypesApi.getMimeTypesFormatAware(response)
    }
}
