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

import org.codehaus.groovy.grails.plugins.web.mimes.FormatInterceptor
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

/**
 * Api extensions to controllers for the MimeTypes plugin
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class ControllersMimeTypesApi {

    /**
     * <p>The withFormat method is used to allow controllers to handle different types of
     * request formats such as HTML, XML and so on. Example usage:</p>
     *
     * <pre>
     * <code>
     *    withFormat {
     *        html { render "html" }
     *        xml { render "xml}
     *    }
     * </code>
     * </pre>
     *
     * @param instance
     * @param callable
     * @return  The result of the closure execution selected
     */

    def withFormat(instance, Closure callable) {
            def formats = [:]
            try {
                callable.delegate = new FormatInterceptor()
                callable.resolveStrategy = Closure.DELEGATE_ONLY
                callable.call()
                formats = callable.delegate.formatOptions
            }
            finally {
                callable.delegate = instance
                callable.resolveStrategy = Closure.OWNER_FIRST
            }

            def result
            def req = instance.request
            def format = req.format
            if (formats) {
                if (format == 'all') {
                    def firstKey = formats.firstKey()
                    result = getResponseForFormat(formats[firstKey], firstKey, req)
                }
                else {
                    // if the format has been specified then use that
                    if (formats.containsKey(format)) {
                        result = getResponseForFormat(formats[format], format, req)
                    }
                    // otherwise look for the best match
                    else {
                        for (mime in req.mimeTypes) {
                            if (formats.containsKey(mime.extension)) {
                                result = getResponseForFormat(formats[mime.extension], mime.extension, req)
                                break
                            }
                        }
                    }
                }
            }
            return result
    }

    private getResponseForFormat(formatResponse, format, req) {
        req[GrailsApplicationAttributes.CONTENT_FORMAT] = format
        if (formatResponse instanceof Map) {
            return formatResponse
        }

        return formatResponse?.call()
    }
}
