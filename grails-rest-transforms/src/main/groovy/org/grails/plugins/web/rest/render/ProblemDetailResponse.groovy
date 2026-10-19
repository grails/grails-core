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
package org.grails.plugins.web.rest.render

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import jakarta.servlet.http.HttpServletRequest

import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.util.MimeTypeUtils
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.accept.ContentNegotiationManager
import org.springframework.web.context.request.ServletWebRequest

import grails.rest.render.RenderContext
import org.grails.web.servlet.mvc.GrailsWebRequest

/**
 * Prepares the response to a {@link ProblemDetail}, as the JSON and XML renderers write it: the response takes the
 * status the problem carries, unless a {@code status} argument names another, and a problem without an
 * {@code instance} is given the path of the resource.
 *
 * @since 9.0
 */
@Slf4j
@CompileStatic
class ProblemDetailResponse {

    /** The request attribute that holds the problem the response has been prepared for. */
    private static final String PREPARED = ProblemDetailResponse.name + '.prepared'

    /** The name of the bean Spring MVC's content negotiation manager is, which its handler adapter is given by name. */
    private static final String MVC_CONTENT_NEGOTIATION_MANAGER = 'mvcContentNegotiationManager'

    /** A manager that reads the Accept header alone, as Spring MVC does by default. */
    private static final ContentNegotiationManager HEADER_NEGOTIATION = new ContentNegotiationManager()

    /** The types Spring Boot's converters write a problem in, in their order. */
    private static final List<MediaType> PROBLEM_TYPES = List.of(MediaType.APPLICATION_PROBLEM_JSON, MediaType.APPLICATION_PROBLEM_XML)

    private ProblemDetailResponse() {
    }

    /**
     * Prepares the response to a problem the application responds with. A {@code status} argument decides the status
     * of the response, as the status of a {@code ResponseEntity} does in Spring MVC, and a problem that carries another
     * is written with its own, with a warning.
     *
     * @param problem the problem a response is written for
     * @param context the context of the response
     */
    static void prepare(ProblemDetail problem, RenderContext context) {
        if (context instanceof ServletRenderContext) {
            // respond prepares a problem for whichever renderer writes it, and a renderer that prepares it again leaves it
            HttpServletRequest request = ((ServletRenderContext) context).webRequest.currentRequest
            if (request.getAttribute(PREPARED).is(problem)) {
                return
            }
            request.setAttribute(PREPARED, problem)
        }
        Integer requested = requestedStatus(context)
        if (requested != null && requested != problem.status) {
            log.warn("The response is sent with the status argument [${requested}], " +
                    "but its ProblemDetail carries status [${problem.status}].")
        }
        prepare(problem, context, requested ?: problem.status)
    }

    /**
     * Prepares the response to a problem a renderer builds for the response, such as one for validation errors, which
     * carries the status the response is sent with.
     *
     * @param problem the problem a response is written for
     * @param context the context of the response
     */
    static void prepareBuilt(ProblemDetail problem, RenderContext context) {
        prepare(problem, context, problem.status)
    }

    /**
     * The type Spring MVC writes a problem in for the request with Spring Boot's message converters, an XML one among
     * them, which Boot lists after the JSON one, as its content negotiation, {@code spring.mvc.contentnegotiation.*} and
     * the application's {@code WebMvcConfigurer} beans included, selects it: {@code application/problem+xml} only where
     * the types it resolves for the request ask for a problem in XML, such as {@code application/problem+xml} or
     * {@code application/*+xml}, ahead of one in JSON, and {@code application/problem+json} otherwise, also for a client
     * that asks for {@code application/xml}, a browser, and one that accepts any type; or {@code null} where that
     * negotiation rejects the request, such as an {@code Accept} header with a quality value above 1 or more than 50
     * types, or, with parameter negotiation, a format it does not know. Whether an XML converter is present is not
     * consulted: without {@code jackson-dataformat-xml}, which Spring's XML converter needs, Spring MVC answers in JSON a
     * client that asks for a problem in XML, where the XML renderer writes the XML itself.
     *
     * @param webRequest the request
     * @return {@code application/problem+xml}, {@code application/problem+json}, or {@code null}
     */
    static MediaType negotiatedType(GrailsWebRequest webRequest) {
        List<MediaType> acceptable
        try {
            acceptable = contentNegotiationManager(webRequest).resolveMediaTypes(new ServletWebRequest(webRequest.currentRequest))
        }
        catch (HttpMediaTypeNotAcceptableException ignored) {
            return null
        }
        // the problem types the request accepts, each as the more specific of the two, as Spring MVC selects them, or the
        // problem types themselves where it accepts none
        List<MediaType> compatible = compatibleTypes(acceptable)
        if (compatible.isEmpty()) {
            compatible = compatibleTypes(PROBLEM_TYPES)
        }
        // Spring's order: quality, then specificity
        MimeTypeUtils.sortBySpecificity(compatible)
        compatible[0].isCompatibleWith(MediaType.APPLICATION_PROBLEM_XML) ? MediaType.APPLICATION_PROBLEM_XML :
                MediaType.APPLICATION_PROBLEM_JSON
    }

    /** The manager Spring MVC negotiates with, or one that reads the {@code Accept} header alone, as Spring MVC's default. */
    private static ContentNegotiationManager contentNegotiationManager(GrailsWebRequest webRequest) {
        ApplicationContext context = webRequest.applicationContext
        context != null && context.containsBean(MVC_CONTENT_NEGOTIATION_MANAGER) ?
                context.getBean(MVC_CONTENT_NEGOTIATION_MANAGER, ContentNegotiationManager) : HEADER_NEGOTIATION
    }

    private static List<MediaType> compatibleTypes(List<MediaType> acceptable) {
        Set<MediaType> compatible = new LinkedHashSet<MediaType>()
        for (MediaType accepted : acceptable) {
            for (MediaType problemType : PROBLEM_TYPES) {
                if (accepted.isCompatibleWith(problemType)) {
                    MediaType produced = problemType.copyQualityValue(accepted)
                    compatible.add(accepted.isLessSpecific(produced) ? produced : accepted)
                }
            }
        }
        new ArrayList<MediaType>(compatible)
    }

    private static void prepare(ProblemDetail problem, RenderContext context, int status) {
        setStatus(context, status)
        if (problem.instance == null && context.resourcePath) {
            problem.instance = URI.create(context.resourcePath)
        }
    }

    /**
     * The status {@code respond} is given, read as {@code respond} reads it.
     */
    private static Integer requestedStatus(RenderContext context) {
        Object status = context.arguments?.get('status')
        if (!status) {
            return null
        }
        if (status instanceof Number) {
            return ((Number) status).intValue()
        }
        if (status instanceof HttpStatusCode) {
            return ((HttpStatusCode) status).value()
        }
        status.toString().toInteger()
    }

    /**
     * A problem may carry any status code, as in Spring MVC, while {@link RenderContext} takes an
     * {@link HttpStatus}; a code without a constant is written to the servlet response directly.
     */
    private static void setStatus(RenderContext context, int code) {
        HttpStatus status = HttpStatus.resolve(code)
        if (status != null) {
            context.setStatus(status)
        }
        else if (context instanceof ServletRenderContext) {
            ((ServletRenderContext) context).webRequest.currentResponse.setStatus(code)
        }
        else {
            throw new IllegalArgumentException(
                    "Status [${code}] has no HttpStatus constant, which ${context.getClass().name} requires.")
        }
    }
}
