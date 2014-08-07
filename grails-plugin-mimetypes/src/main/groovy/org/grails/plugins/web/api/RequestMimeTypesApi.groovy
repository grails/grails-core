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
package org.grails.plugins.web.api

import grails.core.GrailsApplication
import grails.web.http.HttpHeaders
import grails.web.mime.MimeType
import grails.web.util.GrailsApplicationAttributes
import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletRequest

import org.grails.web.mime.DefaultAcceptHeaderParser

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

    RequestMimeTypesApi() {}

    RequestMimeTypesApi(GrailsApplication application, MimeType[] mimeTypes) {
        this.mimeTypes = mimeTypes
        grailsApplication = application
    }

    MimeType[] getMimeTypes() { mimeTypes }


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
            result = parser.parse(header, header ? new MimeType(header) : MimeType.HTML)

            request.setAttribute(GrailsApplicationAttributes.REQUEST_FORMATS, result)
        }
        result
    }
}
