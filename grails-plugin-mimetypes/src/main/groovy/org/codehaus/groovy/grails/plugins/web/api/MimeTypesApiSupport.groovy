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

import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.codehaus.groovy.grails.plugins.web.mimes.FormatInterceptor
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

/**
 * Support class for dealing with calls to withFormat.
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class MimeTypesApiSupport {

    def withFormat(HttpServletRequest request, Closure callable) {
        def formats = getDefinedFormats(callable)

        return withFormatInternal(request, formats)
    }

    def withFormat(HttpServletResponse response, Closure callable) {
        def formats = getDefinedFormats(callable)

        return withFormatInternal(response, formats)
    }

    protected withFormatInternal(formatProvider, Map formats) {
        def result
        def format = formatProvider.format
        if (formats) {
            if (format == 'all') {
                def firstKey = formats.firstKey()
                result = getResponseForFormat(formats[firstKey], firstKey, formatProvider)
            }
            else {
                // if the format has been specified then use that
                if (formats.containsKey(format)) {
                    result = getResponseForFormat(formats[format], format, formatProvider)
                }
                // otherwise look for the best match
                else {
                    for (mime in formatProvider.mimeTypes) {
                        if (formats.containsKey(mime.extension)) {
                            result = getResponseForFormat(formats[mime.extension], mime.extension, formatProvider)
                            break
                        }
                    }
                }
            }
        }
        return result
    }

    Map getDefinedFormats(Closure callable) {
        def formats
        def original = callable.delegate
        try {
            callable.delegate = new FormatInterceptor()
            callable.resolveStrategy = Closure.DELEGATE_ONLY
            callable.call()
            formats = callable.delegate.formatOptions
        }
        finally {
            callable.delegate = original
            callable.resolveStrategy = Closure.OWNER_FIRST
        }
        return formats
    }

    private getResponseForFormat(formatResponse, format, formatProvider) {
        if (formatProvider instanceof ServletRequest) {
            formatProvider.setAttribute(GrailsApplicationAttributes.CONTENT_FORMAT, format)
        }
        else {
            GrailsWebRequest.lookup().currentRequest.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT, format)
        }

        if (formatResponse instanceof Map) {
            return formatResponse
        }

        return formatResponse?.call()
    }
}
