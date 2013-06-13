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
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.web.mime.MimeType

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
 * @since 2.0
 */
@CompileStatic
class MimeTypesApiSupport {

    def withFormat(HttpServletRequest request, Closure callable) {
        return withFormatInternal(request, getDefinedFormats(callable))
    }

    def withFormat(HttpServletResponse response, Closure callable) {
        return withFormatInternal(response, getDefinedFormats(callable))
    }

    protected Object withFormatInternal(formatProvider, LinkedHashMap<String, Object> formats) {
        def result = null
        String format = lookupFormat(formatProvider)
        if (formats) {
            if (format == 'all') {
                def firstKey = formats.keySet().iterator().next()
                result = getResponseForFormat(formats[firstKey], firstKey, formatProvider)
            }
            else {
                // if the format has been specified then use that
                if (formats.containsKey(format)) {
                    result = getResponseForFormat(formats[format], format, formatProvider)
                }
                // otherwise look for the best match
                else {
                    boolean matchFound = false
                    for (MimeType mime in lookupMimeTypes(formatProvider)) {
                        if (formats.containsKey(mime.extension)) {
                            matchFound = true
                            result = getResponseForFormat(formats[mime.extension], mime.extension, formatProvider)
                            break
                        }
                        else {
                            if (mime.extension == 'all') {
                                matchFound = true
                                def firstKey = formats.keySet().iterator().next()
                                result = getResponseForFormat(formats[firstKey], firstKey, formatProvider)
                                break
                            }
                        }
                    }
                    if (!matchFound && formats.containsKey('*')) {
                        result = getResponseForFormat(formats['*'], format, formatProvider)
                    }
                }
            }
        }
        return result
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected MimeType[] lookupMimeTypes(formatProvider) {
        formatProvider.mimeTypes
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected String lookupFormat(formatProvider) {
        formatProvider.format
    }

    LinkedHashMap<String, Object> getDefinedFormats(Closure callable) {
        LinkedHashMap<String, Object> formats = null
        def original = callable.delegate
        try {
            final interceptor = new FormatInterceptor()
            callable.delegate = interceptor
            callable.resolveStrategy = Closure.DELEGATE_ONLY
            callable.call()
            formats = interceptor.formatOptions
        }
        finally {
            callable.delegate = original
            callable.resolveStrategy = Closure.OWNER_FIRST
        }
        return formats
    }

    private Object getResponseForFormat(formatResponse, format, formatProvider) {
        if (formatProvider instanceof ServletRequest) {
            formatProvider.setAttribute(GrailsApplicationAttributes.CONTENT_FORMAT, format)
        }
        else {
            GrailsWebRequest.lookup().currentRequest.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT, format)
        }

        if (formatResponse instanceof Closure) {
            return formatResponse?.call()
        }
        else {
            return formatResponse
        }

    }
}
