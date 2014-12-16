/*
 * Copyright 2014 the original author or authors.
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
package org.grails.web.mime

import grails.web.http.HttpHeaders
import grails.web.mime.MimeType
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.web.context.support.WebApplicationContextUtils

import javax.servlet.http.HttpServletRequest

import org.grails.plugins.web.api.MimeTypesApiSupport

/**
 * An extension that adds methods to the {@link HttpServletRequest} object for mime types
 *
 * @author Jeff Brown
 * @author Graeme Rocher
 *
 * @since 3.0
 * 
 */
class HttpServletRequestExtension {
    
    protected static MimeTypesApiSupport apiSupport = new MimeTypesApiSupport()
    
    static withFormat(HttpServletRequest request, Closure callable) {
        apiSupport.withFormat(request, callable)
    }
     
    /**
     * Obtains the request format, which is dictated by the CONTENT_TYPE header and evaluated using the
     * configured {@link MimeType} instances. Only configured MimeTypes
     * are allowed.
     *
     * @param request The request object
     * @return The request format or null if exists
     */
    static String getFormat(HttpServletRequest request) {
        def result = request.getAttribute(GrailsApplicationAttributes.CONTENT_FORMAT)
        if (!result) {
            result = request.getMimeTypes()[0].extension
            request.setAttribute(GrailsApplicationAttributes.CONTENT_FORMAT, result)
        }
        result
    }

    /**
     * Obtains a list of configured {@link grails.web.mime.MimeType} instances for the request
     *
     * @param request The request
     * @return A list of configured mime types
     */
    static MimeType[] getMimeTypes(HttpServletRequest request) {
        MimeType[] result = (MimeType[])request.getAttribute(GrailsApplicationAttributes.REQUEST_FORMATS)
        if (!result) {
            def context = WebApplicationContextUtils.getRequiredWebApplicationContext(request.servletContext)
            def parser = new DefaultAcceptHeaderParser((MimeType[])context.getBean(MimeType.BEAN_NAME))
            def header = request.contentType
            if (!header) header = request.getHeader(HttpHeaders.CONTENT_TYPE)
            result = parser.parse(header, header ? new MimeType(header) : MimeType.HTML)

            request.setAttribute(GrailsApplicationAttributes.REQUEST_FORMATS, result)
        }
        result
    }
}
