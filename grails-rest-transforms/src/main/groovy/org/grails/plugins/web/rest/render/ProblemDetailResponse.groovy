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

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.InvalidMediaTypeException
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.util.InvalidMimeTypeException
import org.springframework.util.MimeTypeUtils

import grails.rest.render.RenderContext

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
     * Whether a problem is written for the request in XML, as Spring MVC writes it with Spring Boot's message converters,
     * an XML converter among them, which Boot lists after the JSON one: only for a client whose {@code Accept} header asks
     * for a problem in XML, such as {@code application/problem+xml} or {@code application/*+xml}, ahead of one in JSON. A
     * client that asks for {@code application/xml}, a browser, and one that accepts any type are answered in JSON, and the
     * format a request names is not consulted, as Spring MVC does not consult it. Nor is whether an XML converter is
     * present: without {@code jackson-dataformat-xml}, which Spring's XML converter needs, Spring MVC answers in JSON a
     * client that asks for a problem in XML, where the XML renderer writes the XML itself.
     *
     * @param request the request
     * @return {@code true} where the problem is written as {@code application/problem+xml}
     */
    static boolean negotiatesXml(HttpServletRequest request) {
        List<MediaType> accepted = acceptedTypes(request)
        if (accepted == null) {
            return false
        }
        MediaType preferred = accepted.find { MediaType type ->
            type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) || type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_XML)
        }
        preferred != null && !preferred.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
    }

    /**
     * Whether Spring MVC reads the {@code Accept} header of the request: one with a type it cannot parse, such as a
     * quality value above 1, or with more than the 50 types {@code MimeTypeUtils} sorts, it does not, and then writes no
     * problem.
     *
     * @param request the request
     * @return {@code true} where Spring MVC reads the header
     */
    static boolean readsAcceptHeader(HttpServletRequest request) {
        acceptedTypes(request) != null
    }

    /** The types the request accepts, as Spring MVC reads and orders them, or {@code null} where it cannot read them. */
    private static List<MediaType> acceptedTypes(HttpServletRequest request) {
        try {
            List<MediaType> accepted = MediaType.parseMediaTypes(Collections.list(request.getHeaders(HttpHeaders.ACCEPT)))
            // Spring's order: quality, then specificity
            MimeTypeUtils.sortBySpecificity(accepted)
            return accepted
        }
        catch (InvalidMediaTypeException | InvalidMimeTypeException ignored) {
            return null
        }
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
