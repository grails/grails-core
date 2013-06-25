/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.plugins.web.api

import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletRequest

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.mime.DefaultAcceptHeaderParser
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.HttpHeaders

/**
 * Methods added to the {@link javax.servlet.http.HttpServletRequest} instance for request format handling.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@CompileStatic
class RequestMimeTypesApi {

    MimeType[] mimeTypes
    GrailsApplication grailsApplication

    MimeTypesApiSupport apiSupport = new MimeTypesApiSupport()

    RequestMimeTypesApi() {}

    RequestMimeTypesApi(GrailsApplication application, MimeType[] mimeTypes) {
        this.mimeTypes = mimeTypes
        grailsApplication = application
    }

    MimeType[] getMimeTypes() { mimeTypes }

    /**
     * Obtains the request format, which is dictated by the CONTENT_TYPE header and evaluated using the
     * configured {@link org.codehaus.groovy.grails.web.mime.MimeType} instances. Only configured MimeTypes
     * are allowed.
     *
     * @param request The request object
     * @return The request format or null if exists
     */
    String getFormat(HttpServletRequest request) {
        def result = request.getAttribute(GrailsApplicationAttributes.CONTENT_FORMAT)
        if (!result) {
            result = getMimeTypes(request)[0].extension
            request.setAttribute(GrailsApplicationAttributes.CONTENT_FORMAT, result)
        }
        result
    }

    /**
     * Obtains a list of configured {@link MimeType} instances for the request
     *
     * @param request The request
     * @return A list of configured mime types
     */
    MimeType[] getMimeTypes(HttpServletRequest request) {
        MimeType[] result = (MimeType[])request.getAttribute(GrailsApplicationAttributes.REQUEST_FORMATS)
        if (!result) {
            def parser = new DefaultAcceptHeaderParser(getMimeTypes())
            def header = request.contentType
            if (!header) header = request.getHeader(HttpHeaders.CONTENT_TYPE)
            result = parser.parse(header, new MimeType(header))

            request.setAttribute(GrailsApplicationAttributes.REQUEST_FORMATS, result)
        }
        result
    }

    /**
     * Allows for the request.withFormat { } syntax
     *
     * @param request The request
     * @param callable A closure
     * @return The result of the closure call
     */
    Object withFormat(HttpServletRequest request, Closure callable) {
        apiSupport.withFormat(request, callable)
    }
}
