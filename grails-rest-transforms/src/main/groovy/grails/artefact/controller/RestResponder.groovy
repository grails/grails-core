/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package grails.artefact.controller

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.Generated
import groovy.transform.TypeCheckingMode

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.web.servlet.View
import org.springframework.web.servlet.view.InternalResourceView

import grails.artefact.Controller
import grails.artefact.controller.support.ResponseRenderer
import grails.core.support.proxy.ProxyHandler
import grails.rest.Resource
import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import grails.web.mime.MimeType
import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.plugins.web.rest.render.DefaultRendererRegistry
import org.grails.plugins.web.rest.render.ProblemDetailResponse
import org.grails.plugins.web.rest.render.SelectiveRenderer
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.grails.plugins.web.rest.render.html.DefaultHtmlRenderer
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.servlet.view.CompositeViewResolver
import org.grails.web.util.GrailsApplicationAttributes

/**
 *
 * @author Jeff Brown
 * @since 3.0
 *
 */
@CompileStatic
trait RestResponder {

    private String PROPERTY_RESPONSE_FORMATS = 'responseFormats'

    private RendererRegistry rendererRegistry
    private ProxyHandler proxyHandler

    @Generated
    @Autowired(required = false)
    void setRendererRegistry(RendererRegistry rendererRegistry) {
        this.rendererRegistry = rendererRegistry
    }

    @Generated
    RendererRegistry getRendererRegistry() {
        return this.rendererRegistry
    }

    @Generated
    @Autowired(required = false)
    void setProxyHandler(ProxyHandler proxyHandler) {
        this.proxyHandler = proxyHandler
    }

    @Generated
    ProxyHandler getProxyHandler() {
        return this.proxyHandler
    }

    /**
     * Same as {@link RestResponder#respond(java.lang.Object, java.lang.Object, java.util.Map)}, but here to support Groovy named arguments
     */
    @Generated
    def respond(Map args, value) {
        internalRespond(value, args)
    }

    /**
     * The respond method will attempt to delivery an appropriate response for the
     * requested response format and Map value.
     *
     * If the value is null then a 404 will be returned. Otherwise the {@link RendererRegistry}
     * will be consulted for an appropriate response renderer for the requested response format.
     *
     * @param value The value
     * @return
     */
    @Generated
    def respond(Map value) {
        internalRespond(value)
    }

    /**
     * Same as {@link RestResponder#respond(java.util.Map)}, but here to support Groovy named arguments
     */
    @Generated
    def respond(Map namedArgs, Map value) {
        internalRespond(value, namedArgs)
    }

    /**
     * The respond method will attempt to delivery an appropriate response for the
     * requested response format and value.
     *
     * If the value is null then a 404 will be returned. Otherwise the {@link RendererRegistry}
     * will be consulted for an appropriate response renderer for the requested response format.
     *
     * @param value The value
     * @param args The arguments
     * @return
     */
    @Generated
    def respond(value, Map args = [:]) {
        internalRespond(value, args)
    }

    private internalRespond(value, Map args = [:]) {
        Integer statusCode
        if (args.status) {
            final statusValue = args.status
            if (statusValue instanceof Number) {
                statusCode = statusValue.intValue()
            } else {
                if (statusValue instanceof HttpStatus) {
                    statusCode = ((HttpStatus) statusValue).value()
                } else {
                    statusCode = statusValue.toString().toInteger()
                }
            }
        }
        if (value == null) {
            return callRender([status: statusCode ?: 404 ])
        }

        if (proxyHandler != null) {
            value = proxyHandler.unwrapIfProxy(value)
        }

        final webRequest = ((Controller) this).getWebRequest()
        List<String> formats = calculateFormats(webRequest.actionName, value, args)
        final response = webRequest.getCurrentResponse()
        MimeType[] mimeTypes = getResponseFormat(response)
        def registry = rendererRegistry
        if (registry == null) {
            // a controller Spring did not create, such as one a test creates with new, responds with the registry an
            // autowired one is given, which has the renderers of the application and of modules such as grails-xml: the
            // application's only or primary one, or else the one of that name
            ApplicationContext applicationContext = webRequest.applicationContext
            registry = applicationContext?.getBeanProvider(RendererRegistry)?.getIfUnique()
            if (registry == null && applicationContext?.containsBean('rendererRegistry')) {
                registry = applicationContext.getBean('rendererRegistry', RendererRegistry)
            }
        }
        if (registry == null) {
            registry = new DefaultRendererRegistry()
            registry.initialize()
        }

        Renderer renderer = null
        // A problem is written as Spring MVC writes it with Spring Boot's converters, an XML one among them: in XML for a
        // client that asks for a problem in XML, where the action responds in XML, and in JSON for any other. Without
        // jackson-dataformat-xml, where Spring MVC would answer in JSON, the XML renderer writes the XML itself. A format's
        // renderer of any object writes it only in that format, and the HTML renderer only with the view respond names
        boolean problem = value instanceof ProblemDetail
        MimeType problemFormat = problem && !args.view ?
                (formats.contains('xml') && ProblemDetailResponse.negotiatesXml(webRequest.currentRequest) ? MimeType.XML : MimeType.JSON) :
                null
        final context = new ServletRenderContext(webRequest, args)

        Errors errors = value.hasProperty(GormProperties.ERRORS) ? getDomainErrors(value) : null
        boolean hasErrors = errors && errors.hasErrors()
        def target = null
        ServletRenderContext errorsContext = null
        boolean errorsFormatListed = false
        if (hasErrors) {
            target = errors instanceof BeanPropertyBindingResult ? errors.getTarget() : null
            if (proxyHandler != null && target != null) {
                target = proxyHandler.unwrapIfProxy(target)
            }
            // the configuration, which the errors are not written with, so that a name that is wrong is found, as it is
            // for the errors themselves
            errorsContext = new ServletRenderContext(webRequest, [model: args.model, jsonConfiguration: args.jsonConfiguration])
        }

        // The response is in the first type the client accepts, in its order, that the action responds in and a renderer
        // renders the value in; a client that accepts any type is answered in the first of the action's formats that is
        MimeType matched = null
        negotiation:
        for (MimeType accepted in mimeTypes) {
            List<MimeType> candidates = [accepted]
            if (accepted == MimeType.ALL && formats) {
                final allMimeTypes = MimeType.getConfiguredMimeTypes()
                candidates = formats.collect { String format -> allMimeTypes.find { MimeType mt -> mt.extension == format } }
                        .findAll { MimeType mt -> mt != null }
            }
            for (MimeType mimeType in candidates) {
                if (!formats.contains(mimeType.extension)) {
                    continue
                }
                if (hasErrors) {
                    errorsFormatListed = true
                    Renderer<Errors> errorsRenderer = registry.findContainerRenderer(mimeType, Errors, target)
                    if (!rendersValue(errorsRenderer, errors, errorsContext, mimeType)) {
                        continue
                    }
                    webRequest.request.setAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPE, mimeType)
                    if (args.view) {
                        errorsContext.viewName = args.view as String
                    }
                    if (statusCode != null) {
                        setStatus(errorsContext, statusCode)
                    }
                    errorsRenderer.render(errors, errorsContext)
                    if (errorsContext.wasWrittenTo() && !response.isCommitted()) {
                        response.flushBuffer()
                    }
                    return
                }

                Renderer candidate
                final valueType = value.getClass()
                if (registry.isContainerType(valueType)) {
                    candidate = registry.findContainerRenderer(mimeType, valueType, value)
                    if (candidate == null) {
                        candidate = registry.findRenderer(mimeType, value)
                    }
                } else {
                    candidate = registry.findRenderer(mimeType, value)
                }
                if (problemFormat != null && mimeType.extension != problemFormat.extension && candidate != null &&
                        candidate.is(registry.findRenderer(mimeType, new Object()))) {
                    continue
                }
                if (rendersValue(candidate, value, context, mimeType)) {
                    renderer = candidate
                    matched = mimeType
                    break negotiation
                }
            }
        }
        if (hasErrors) {
            // Validation errors that no renderer writes in a format the action responds in are not found, as in Grails 8,
            // and a client that accepts none of its formats is not acceptable
            callRender([status: statusCode ?: (errorsFormatListed ? HttpStatus.NOT_FOUND.value() : HttpStatus.NOT_ACCEPTABLE.value())])
            return
        }
        if (renderer != null && problem && args.view && renderer instanceof DefaultHtmlRenderer &&
                renderer.is(registry.findRenderer(matched, new Object())) &&
                !resolvesView(webRequest, args.view.toString(), matched)) {
            // The HTML page respond names for a problem, which does not exist, does not render it: the problem is written
            // as in a format nothing renders it in, so that a browser is answered as Spring MVC answers it
            renderer = null
        }
        if (renderer == null && problem) {
            // The format negotiated for the problem, or JSON where nothing renders it in that, whose view respond names
            // renders the problem, as for a client that asks for JSON
            matched = problemFormat ?: MimeType.JSON
            renderer = registry.findRenderer(matched, value)
            if (matched != MimeType.JSON && !rendersValue(renderer, value, context, matched)) {
                matched = MimeType.JSON
                renderer = registry.findRenderer(matched, value)
            }
        }
        if (renderer != null) {
            // The response is in the format the renderer was found for, which may not be the client's first: a client
            // that lists one the action does not respond in ahead of it is answered in the next it does
            webRequest.request.setAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPE, matched)
            if (statusCode != null) {
                setStatus(context, statusCode)
            }
            if (problem) {
                // A problem answers with the status it carries and the path of the resource whichever renderer writes it,
                // one the application registers included, as in Spring MVC
                ProblemDetailResponse.prepare((ProblemDetail) value, context)
            }
            renderer.render(value, context)
            if (context.wasWrittenTo() && !response.isCommitted()) {
                response.flushBuffer()
            }
            return
        }
        callRender([status: statusCode ?: HttpStatus.NOT_ACCEPTABLE.value() ])
    }

    /**
     * Whether a renderer renders a value: one that renders some values only, such as that of markup views without a
     * renderer of XML, says so, as a Spring message converter says whether it writes a value.
     */
    private static boolean rendersValue(Renderer renderer, Object value, ServletRenderContext context, MimeType mimeType) {
        if (renderer == null) {
            return false
        }
        if (!(renderer instanceof SelectiveRenderer)) {
            return true
        }
        // asked in the type it would render in, which the view it looks up depends on
        SelectiveRenderer selective = (SelectiveRenderer) renderer
        return inResponseType(context.webRequest.currentRequest, mimeType) { selective.canRender(value, context) }
    }

    /**
     * Whether the view resolvers that render the response find a view of the name, looked up in the type of the response,
     * as when they render it; one resolved to a servlet resource, such as a JSP, exists where the resource does. Without
     * view resolvers to ask, a view is taken to exist.
     */
    private static boolean resolvesView(GrailsWebRequest webRequest, String viewName, MimeType mimeType) {
        ApplicationContext applicationContext = webRequest.applicationContext
        if (applicationContext == null || !applicationContext.containsBean(CompositeViewResolver.BEAN_NAME)) {
            return true
        }
        CompositeViewResolver resolver = applicationContext.getBean(CompositeViewResolver.BEAN_NAME, CompositeViewResolver)
        return inResponseType(webRequest.currentRequest, mimeType) {
            View view = resolver.resolveView(viewName, webRequest.locale)
            view instanceof InternalResourceView ?
                    webRequest.servletContext.getResource(((InternalResourceView) view).url) != null :
                    view != null
        }
    }

    /** Answers a question about the response as if it were in the given type, which is restored afterwards. */
    private static boolean inResponseType(HttpServletRequest request, MimeType mimeType, Closure<Boolean> question) {
        Object previous = request.getAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPE)
        request.setAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPE, mimeType)
        try {
            return question.call()
        }
        finally {
            if (previous == null) {
                request.removeAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPE)
            }
            else {
                request.setAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPE, previous)
            }
        }
    }

    /** Any status code, as a {@code ResponseEntity} takes it, also one {@link HttpStatus} has no constant for. */
    private static void setStatus(ServletRenderContext context, int statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode)
        if (status != null) {
            context.setStatus(status)
        }
        else {
            context.webRequest.currentResponse.setStatus(statusCode)
        }
    }

    private callRender(Map args) {
        ((ResponseRenderer) this).render(args)
    }

    private List<String> calculateFormats(String actionName, value, Map args) {
        if (args.formats) {
            return (List<String>) args.formats
        }

        if (this.hasProperty(PROPERTY_RESPONSE_FORMATS)) {
            final responseFormatsProperty = this[PROPERTY_RESPONSE_FORMATS]
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

    @CompileDynamic
    private MimeType[] getResponseFormat(HttpServletResponse response) {
        response.mimeTypesFormatAware
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private Errors getDomainErrors(object) {
        if (object instanceof Errors) {
            return object
        }
        final errors = object.errors
        if (errors instanceof Errors) {
            return errors
        }
        return null
    }

    private List<String> getDefaultResponseFormats(value) {
        Resource resAnn = value != null ? value.getClass().getAnnotation(Resource) : null
        if (resAnn) {
            return resAnn.formats().toList()
        }
        return MimeType.getConfiguredMimeTypes().collect { MimeType mt -> mt.extension }
    }
}
