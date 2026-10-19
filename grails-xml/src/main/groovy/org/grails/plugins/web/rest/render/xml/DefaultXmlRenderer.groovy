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
package org.grails.plugins.web.rest.render.xml

import java.nio.charset.Charset
import java.util.function.Supplier
import javax.xml.stream.XMLStreamException

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.http.converter.json.ProblemDetailJacksonMixin
import org.springframework.util.ClassUtils
import org.springframework.validation.Errors

import grails.converters.XML
import grails.rest.render.RenderContext
import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import grails.util.GrailsWebUtil
import grails.web.mime.MimeType
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.plugins.web.rest.render.html.DefaultHtmlRenderer
import org.grails.plugins.web.rest.render.JacksonWriteFailures
import org.grails.plugins.web.rest.render.ProblemDetailResponse
import org.grails.plugins.web.rest.render.SpringMessageConverters
import org.grails.plugins.web.rest.render.WriterOutputStream
import org.grails.web.gsp.io.GrailsConventionGroovyPageLocator

/**
 * Default renderer for XML responses
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class DefaultXmlRenderer<T> implements Renderer<T> {

    @Value('${grails.web.rendering.xml.spring:false}')
    boolean useSpringXml = false

    final Class<T> targetType
    MimeType[] mimeTypes = [MimeType.XML, MimeType.TEXT_XML] as MimeType[]

    @Value('${grails.converters.encoding:UTF-8}')
    String encoding = GrailsWebUtil.DEFAULT_ENCODING

    GrailsConventionGroovyPageLocator groovyPageLocator

    // Resolving the locator while collecting MIME type providers creates a bean cycle.
    @Autowired(required = false)
    ObjectProvider<GrailsConventionGroovyPageLocator> groovyPageLocatorProvider

    RendererRegistry rendererRegistry

    @Autowired(required = false)
    ObjectProvider<RendererRegistry> rendererRegistryProvider

    // the registry an autowired controller is given, where the application has several
    @Autowired(required = false)
    @Qualifier('rendererRegistry')
    private ObjectProvider<RendererRegistry> namedRendererRegistryProvider

    /**
     * Resolved when a response is written rather than when this renderer is built, so that
     * obtaining the converters cannot force MVC initialization during bean creation.
     */
    Supplier<List<HttpMessageConverter<?>>> springHttpMessageConvertersSupplier

    /** Lets an XML renderer bean, which the plugin does not configure, reach MVC's converters. */
    @Autowired(required = false)
    ObjectProvider<SpringMessageConverters> springMessageConvertersProvider

    private volatile GrailsConventionGroovyPageLocator resolvedGroovyPageLocator
    private volatile RendererRegistry resolvedRendererRegistry

    private volatile Tuple2<ObjectMapper, ObjectMapper> problemMapper

    private static final ObjectMapper DEFAULT_PROBLEM_MAPPER = JsonMapper.builder().build()

    private GrailsConventionGroovyPageLocator resolveGroovyPageLocator() {
        if (groovyPageLocator != null) {
            return groovyPageLocator
        }
        // Only a locator that was found is kept: one registered after the first response is still found
        if (resolvedGroovyPageLocator == null) {
            resolvedGroovyPageLocator = groovyPageLocatorProvider?.getIfAvailable()
        }
        return resolvedGroovyPageLocator
    }

    private RendererRegistry resolveRendererRegistry() {
        if (rendererRegistry != null) {
            return rendererRegistry
        }
        if (resolvedRendererRegistry == null) {
            // the application's only or primary registry, or else the one respond writes with
            resolvedRendererRegistry = rendererRegistryProvider?.getIfUnique() ?: namedRendererRegistryProvider?.getIfAvailable()
        }
        return resolvedRendererRegistry
    }

    private List<HttpMessageConverter<?>> resolveSpringHttpMessageConverters() {
        List<HttpMessageConverter<?>> supplied = springHttpMessageConvertersSupplier?.get() ?:
                springMessageConvertersProvider?.getIfAvailable()?.converters
        return supplied ?: []
    }

    String namedConfiguration

    DefaultXmlRenderer(Class<T> targetType) {
        this.targetType = targetType
    }

    DefaultXmlRenderer(Class<T> targetType, MimeType...mimeTypes) {
        this.targetType = targetType
        this.mimeTypes = mimeTypes
    }

    DefaultXmlRenderer(Class<T> targetType, GrailsConventionGroovyPageLocator groovyPageLocator) {
        this.targetType = targetType
        this.groovyPageLocator = groovyPageLocator
    }

    DefaultXmlRenderer(Class<T> targetType, GrailsConventionGroovyPageLocator groovyPageLocator, RendererRegistry rendererRegistry) {
        this.targetType = targetType
        this.groovyPageLocator = groovyPageLocator
        this.rendererRegistry = rendererRegistry
    }

    @Override
    void render(Object object, RenderContext context) {
        final mimeType = context.acceptMimeType ?: MimeType.XML
        context.setContentType(GrailsWebUtil.getContentType(mimeType.name, encoding))

        // A problem is rendered only by the view respond names, as in any other format
        def viewName = object instanceof ProblemDetail ? context.arguments?.view?.toString() :
                (context.viewName ?: context.actionName)
        final view = viewName && !(object instanceof Errors) ?
                resolveGroovyPageLocator()?.findViewForFormat(context.controllerName, viewName, mimeType.extension) : null
        if (view) {
            // if a view is provided, we use the HTML renderer to return an appropriate model to the view; one an
            // application registers for problems renders their HTML page rather than the view respond names
            RendererRegistry registry = resolveRendererRegistry()
            Renderer htmlRenderer = object instanceof ProblemDetail ? null : registry?.findRenderer(MimeType.HTML, object)
            if (htmlRenderer == null) {
                htmlRenderer = DefaultHtmlRenderer.forFormatView(targetType, registry, encoding)
            }
            htmlRenderer.render((Object) object, context)
        } else {
            if (object instanceof Errors) {
                context.setStatus(HttpStatus.UNPROCESSABLE_ENTITY)
            }
            renderXml(object, context)
        }

    }

    /**
     * Writes a problem as Spring MVC does, as {@code application/problem+xml} through its message converters, whatever
     * {@code grails.web.rendering.xml.spring} is set to. Where none can write it, as without
     * {@code jackson-dataformat-xml}, it is written in the format Spring's converter writes, with the extension members
     * written by the mapper of Spring's JSON converter with Spring's mix-in for problems, as Spring's own XML
     * converter writes them: without the Grails serializers of the problem written in JSON, so that validation errors
     * or a value that leads back to itself are written as Jackson writes any bean, and a domain class instance too,
     * unless {@code grails.converters.json.domain.jackson.enabled} gives the application's mapper the domain serializer,
     * and fails, as Spring's converters fail, for a problem nested deeper than that mapper's
     * {@code StreamWriteConstraints} allow.
     */
    private void renderProblem(ProblemDetail problem, RenderContext context) {
        ProblemDetailResponse.prepare(problem, context)
        MediaType problemXml = MediaType.APPLICATION_PROBLEM_XML
        context.setContentType(GrailsWebUtil.getContentType(problemXml.toString(), encoding))
        List<HttpMessageConverter<?>> converters = resolveSpringHttpMessageConverters()
        // asked about the class of the problem, as Spring MVC asks
        Class<?> problemType = problem.getClass()
        HttpMessageConverter<Object> converter = (HttpMessageConverter<Object>) converters.find {
            HttpMessageConverter<?> candidate -> candidate.canWrite(problemType, problemXml) &&
                    candidate.getSupportedMediaTypes(problemType).any { MediaType supported ->
                        supported.subtype == 'xml' || supported.subtype.endsWith('+xml')
                    }
        }
        if (converter != null) {
            renderWithSpringConverter(converter, problem, problemXml, context)
            return
        }
        try {
            ProblemXml.write(problem, context.writer, problemMapper(converters, problemType))
        }
        catch (JacksonException | XMLStreamException | IOException e) {
            // as Spring's converter reports it, also a failure of the response's writer while it writes a raw value
            throw JacksonWriteFailures.reported(e)
        }
    }

    /**
     * The mapper the JSON converter Spring MVC writes the problem as {@code application/problem+json} with uses for it,
     * with Spring's mix-in for problems, derived once for the mapper it derives from.
     */
    @CompileDynamic
    private ObjectMapper problemMapper(List<HttpMessageConverter<?>> converters, Class<?> problemType) {
        def json = (JacksonJsonHttpMessageConverter) converters.find {
            it instanceof JacksonJsonHttpMessageConverter && it.canWrite(problemType, MediaType.APPLICATION_PROBLEM_JSON)
        }
        // the one the converter has for the type of the problem, or else its own; without one, a default mapper, with
        // Jackson's limits, which the legacy writer's has not
        ObjectMapper source = json != null ? (mapperForType(json, problemType) ?: json.mapper) : DEFAULT_PROBLEM_MAPPER
        Tuple2<ObjectMapper, ObjectMapper> derived = problemMapper
        if (derived == null || !derived.v1.is(source)) {
            derived = new Tuple2<ObjectMapper, ObjectMapper>(source,
                    source.rebuild().addMixIn(ProblemDetail, ProblemDetailJacksonMixin).build())
            problemMapper = derived
        }
        derived.v2
    }

    /**
     * The mapper a JSON converter has for a type the problem is of and {@code application/problem+json}, as the converter
     * selects it, looked for from the problem's class to its superclasses and interfaces.
     */
    private static ObjectMapper mapperForType(JacksonJsonHttpMessageConverter json, Class<?> problemType) {
        List<Class<?>> types = []
        for (Class<?> type = problemType; type != null; type = type.superclass) {
            types << type
        }
        types.addAll(ClassUtils.getAllInterfacesForClassAsSet(problemType))
        for (Class<?> type : types) {
            Map<MediaType, ? extends ObjectMapper> mappers = json.getMappersForType(type)
            Map.Entry<MediaType, ? extends ObjectMapper> match = mappers?.find { Map.Entry<MediaType, ? extends ObjectMapper> entry ->
                entry.key.includes(MediaType.APPLICATION_PROBLEM_JSON)
            }
            if (match != null) {
                return match.value
            }
        }
        null
    }

    /**
     * Subclasses should override to customize XML response rendering. A problem is written as Spring MVC writes it,
     * whatever the renderer's own settings, unless a subclass renders it itself.
     *
     * @param object
     * @param context
     */
    protected void renderXml(Object object, RenderContext context) {
        if (object instanceof ProblemDetail) {
            renderProblem((ProblemDetail) object, context)
            return
        }
        HttpMessageConverter<Object> springConverter = findSpringConverter(object, context)
        if (springConverter != null) {
            renderWithSpringConverter(springConverter, object,
                    MediaType.parseMediaType((context.acceptMimeType ?: MimeType.XML).name), context)
            return
        }

        XML converter

        if (namedConfiguration) {
            XML.use(namedConfiguration) {
                converter = new XML(object)
            }
        } else {
            converter = new XML(object)
        }
        renderXml(converter, context)
    }

    /**
     * The includes the legacy converter applies, and whose presence keeps a response on it. A
     * subclass that carries its own includes returns them here.
     */
    protected List<String> getEffectiveIncludes(RenderContext context) {
        return context.includes
    }

    /** The excludes the legacy converter applies; see {@link #getEffectiveIncludes}. */
    protected List<String> getEffectiveExcludes(RenderContext context) {
        return context.excludes
    }

    private HttpMessageConverter<Object> findSpringConverter(Object object, RenderContext context) {
        // the legacy converter applies includes, also empty ones, which include none of the properties, and excludes
        if (!useSpringXml || namedConfiguration || getEffectiveIncludes(context) != null || getEffectiveExcludes(context)) {
            return null
        }
        List<HttpMessageConverter<?>> converters = resolveSpringHttpMessageConverters()
        if (!converters) {
            return null
        }
        if (object == null || object instanceof Errors || object instanceof Map || object instanceof Collection ||
                object.getClass().isArray() || DomainClassArtefactHandler.isDomainClass(object.getClass(), true)) {
            return null
        }
        MediaType mediaType = MediaType.parseMediaType((context.acceptMimeType ?: MimeType.XML).name)
        return (HttpMessageConverter<Object>) converters.find { HttpMessageConverter<?> converter ->
            converter.canWrite(object.getClass(), mediaType) &&
                    converter.getSupportedMediaTypes(object.getClass()).any { MediaType supported ->
                        supported.subtype == 'xml' || supported.subtype.endsWith('+xml')
                    }
        }
    }

    private void renderWithSpringConverter(
            HttpMessageConverter<Object> converter, Object object, MediaType mediaType, RenderContext context) {
        // Jackson only writes UTF encodings; the servlet writer applies the response encoding.
        Charset charset = SpringMessageConverters.writeCharset(converter, encoding)
        MediaType contentType = new MediaType(mediaType, charset)
        WriterOutputStream.writeThrough(context.writer, charset) { OutputStream body ->
            HttpOutputMessage message = new HttpOutputMessage() {
                private final HttpHeaders headers = new HttpHeaders()

                @Override
                OutputStream getBody() {
                    return body
                }

                @Override
                HttpHeaders getHeaders() {
                    return headers
                }
            }
            converter.write(object, contentType, message)
        }
    }

    /**
     * Subclasses should override to customize XML response rendering
     *
     * @param object
     * @param context
     */
    protected void renderXml(XML converter, RenderContext context) {
        converter.setExcludes(getEffectiveExcludes(context))
        converter.setIncludes(getEffectiveIncludes(context))
        converter.render(context.getWriter())
    }
}
