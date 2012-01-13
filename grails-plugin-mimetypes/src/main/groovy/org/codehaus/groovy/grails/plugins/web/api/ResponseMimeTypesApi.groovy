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

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.mime.DefaultAcceptHeaderParser
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.HttpHeaders
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

/**
 * Methods added to {@link javax.servlet.http.HttpServletResponse} for response format handling.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class ResponseMimeTypesApi {

    GrailsApplication grailsApplication
    MimeType[] mimeTypes
    private boolean useAcceptHeader

    MimeTypesApiSupport apiSupport = new MimeTypesApiSupport()

    /**
     * Initialize with default settings
     */
    ResponseMimeTypesApi() {
    }

    MimeType[] getMimeTypes() {
        return this.mimeTypes
    }

    /**
     * Initialize with settings provided by GrailsApplication and the given MimeType[]
     *
     * @param grailsApplication The GrailsApplication
     * @param mimeTypes The mime types
     */
    ResponseMimeTypesApi(GrailsApplication grailsApplication, MimeType[] mimeTypes) {
        this.grailsApplication = grailsApplication
        this.mimeTypes = mimeTypes
        this.useAcceptHeader = grailsApplication.flatConfig.get("grails.mime.use.accept.header") ? true : false
    }

    /**
     * Obtains the format to use for the response using either the file extension or the ACCEPT header
     *
     * @param response The response
     * @return The request format
     */
    String getFormat(HttpServletResponse response) {

        final webRequest = GrailsWebRequest.lookup()
        HttpServletRequest request = webRequest.getCurrentRequest()
        def result = request.getAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT)
        if (!result) {
            def formatOverride = webRequest?.params?.format
            if (formatOverride) {
                def allMimes = getMimeTypes()
                MimeType mime = allMimes.find { it.extension == formatOverride }
                result = mime ? mime.extension : getMimeTypes()[0].extension

                // Save the evaluated format as a request attribute.
                // This is a blatant hack because we should to this
                // on the first call. Unfortunately, doing so breaks
                // integration tests:
                //   - Test uses "c.params.format = ..."
                //   - "c.params" creates parameter map
                //   - which triggers the parameter parsing listeners
                //   - which call "request.format"
                //   - which initialises the CONTENT_FORMAT attribute
                //   - *before* the "format" parameter is added to the map
                //   - so the saved format is wrong
                request.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT, result)
            }
            else {
                result = getMimeTypesInternal(request, response)[0].extension
            }
        }
        return result
    }

    /**
     * Gets the configured mime types for the response
     *
     * @param response The response
     * @return The configured mime types
     */
    MimeType[] getMimeTypes(HttpServletResponse response) {
        return getMimeTypesInternal(GrailsWebRequest.lookup().currentRequest, response)
    }

    /**
     * Allows for the response.withFormat { } syntax
     *
     * @param response The response
     * @param callable A closure
     * @return The result of the closure call
     */
    Object withFormat(HttpServletResponse response, Closure callable) {
        apiSupport.withFormat(response, callable)
    }

    private MimeType[] getMimeTypesInternal(HttpServletRequest request, HttpServletResponse response) {
        MimeType[] result = request.getAttribute(GrailsApplicationAttributes.RESPONSE_FORMATS)
        if (!result) {

            def userAgent = request.getHeader(HttpHeaders.USER_AGENT)
            def msie = userAgent && userAgent ==~ /msie(?i)/ ?: false

            def parser = new DefaultAcceptHeaderParser(grailsApplication)
            parser.configuredMimeTypes = getMimeTypes()
            def header = null
            if (msie) header = "*/*"
            if (!header && useAcceptHeader) header = request.getHeader(HttpHeaders.ACCEPT)
            result = parser.parse(header)
            
            // GRAILS-8341 - If no header the parser would have returned all configured mime types.  Since no format
            // was specified in the request we look for the 'all' format and return that if found.  If 'all' is
            // not found the fallback behavior is to return all configured mime types from the parser.
            if (!header) {
                for (mime in result) {
                    if (mime.extension == 'all') {
                        result = [mime] as MimeType[]
                        break
                    }
                }
            }

            request.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMATS, result)
        }
        return result
    }
}
