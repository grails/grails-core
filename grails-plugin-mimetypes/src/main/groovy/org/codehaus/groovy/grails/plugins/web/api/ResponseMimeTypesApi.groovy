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
import javax.servlet.http.HttpServletResponse

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.mime.DefaultAcceptHeaderParser
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.HttpHeaders
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

import java.util.regex.Pattern

/**
 * Methods added to {@link javax.servlet.http.HttpServletResponse} for response format handling.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@CompileStatic
class ResponseMimeTypesApi {

    GrailsApplication grailsApplication
    MimeType[] mimeTypes
    // The ACCEPT header will not be used for content negotiation for user agents containing the following strings (defaults to the 4 major rendering engines)
    protected Pattern disableForUserAgents = ~/(Gecko(?i)|WebKit(?i)|Presto(?i)|Trident(?i))/
    protected boolean useAcceptHeader

    MimeTypesApiSupport apiSupport = new MimeTypesApiSupport()

    /**
     * Initialize with default settings
     */
    ResponseMimeTypesApi() {
    }

    MimeType[] getMimeTypes() { mimeTypes }

    /**
     * Initialize with settings provided by GrailsApplication and the given MimeType[]
     *
     * @param application The GrailsApplication
     * @param mimeTypes The mime types
     */
    ResponseMimeTypesApi(GrailsApplication application, MimeType[] types) {
        grailsApplication = application
        mimeTypes = types
        final config = grailsApplication.flatConfig
        final useAcceptHeader = config.get("grails.mime.use.accept.header")
        this.useAcceptHeader = useAcceptHeader instanceof Boolean ? useAcceptHeader : true
        final disableForUserAgentsConfig = config.get('grails.mime.disable.accept.header.userAgents')
        if (disableForUserAgentsConfig instanceof Collection) {
            final userAgents = disableForUserAgentsConfig.join('(?i)|')
            this.disableForUserAgents = Pattern.compile("(${userAgents})")
        }

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
            final mimeType = getMimeType(response)
            if (mimeType) {
                result = mimeType.extension
                request.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT, result)
            }
        }
        return result
    }

    /**
     * Obtains the MimeType for the response using either the file extension or the ACCEPT header
     *
     * @param response The response
     * @return The MimeType
     */
    MimeType getMimeType(HttpServletResponse response) {
        final webRequest = GrailsWebRequest.lookup()
        return getMimeTypeForRequest(webRequest)
    }

    MimeType getMimeTypeForRequest(GrailsWebRequest webRequest) {
        HttpServletRequest request = webRequest.getCurrentRequest()
        MimeType result = (MimeType) request.getAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPE)
        if (!result) {
            def formatOverride = webRequest?.params?.format
            if (!formatOverride) {
                formatOverride = request.getAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT)
            }
            if (formatOverride) {
                def allMimes = getMimeTypes()
                MimeType mime = allMimes.find { MimeType it -> it.extension == formatOverride }
                result = mime ? mime : getMimeTypes()[0]

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
                request.setAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPE, result)
            } else {
                result = getMimeTypesInternal(request)[0]
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
        return getMimeTypesInternal(GrailsWebRequest.lookup().currentRequest)
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

    private MimeType[] getMimeTypesInternal(HttpServletRequest request) {
        MimeType[] result = (MimeType[])request.getAttribute(GrailsApplicationAttributes.RESPONSE_FORMATS)
        if (!result) {

            def userAgent = request.getHeader(HttpHeaders.USER_AGENT)
            def msie = userAgent && userAgent ==~ /msie(?i)/ ?: false

            def parser = new DefaultAcceptHeaderParser(getMimeTypes())
            String header = null

            boolean disabledForUserAgent = userAgent ? disableForUserAgents.matcher(userAgent).find() : false
            if (msie) header = "*/*"
            if (!header && useAcceptHeader && !disabledForUserAgent) header = request.getHeader(HttpHeaders.ACCEPT)
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
