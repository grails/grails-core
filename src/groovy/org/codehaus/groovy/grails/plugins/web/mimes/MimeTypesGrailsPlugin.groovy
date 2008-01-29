/*
 * Copyright 2004-2005 the original author or authors.
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

package org.codehaus.groovy.grails.plugins.web.mimes

import javax.servlet.http.HttpServletRequest
import org.codehaus.groovy.grails.web.mime.DefaultAcceptHeaderParser
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.HttpHeaders
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.mime.*

/**
* A plug-in that provides content negotiation capabilities to Grails via a new withFormat method on controllers
* as well as a format property on the HttpServletRequest instance
*
* @author Graeme Rocher
* @since 1.0
*
* Created: Nov 26, 2007
*/
class MimeTypesGrailsPlugin {

    def version = grails.util.GrailsUtil.getGrailsVersion()
    def dependsOn = [core:version, servlets:version, controllers:version]
    def observe = ['controllers']

    def doWithDynamicMethods = { ctx ->

	    // Reads the request format by parsing request headers. Will check for the existance of a format parameter first as an override
        HttpServletRequest.metaClass.getFormat = {->
            def result = delegate.getAttribute(GrailsApplicationAttributes.CONTENT_FORMAT)
            if(!result) {
                def formatOverride = RequestContextHolder.currentRequestAttributes().params.format
                def format
                if(formatOverride) {
                    def allMimes = MimeType.getConfiguredMimeTypes()
                    def mime = allMimes.find { it.extension == formatOverride }
                    format = mime ? mime.extension : mimeTypes[0].extension

                }
                else {
                    format = delegate.mimeTypes[0].extension
                }
                result = format
            }
            result
        }

        HttpServletRequest.metaClass.getMimeTypes = {->
            def result = delegate.getAttribute(GrailsApplicationAttributes.REQUEST_FORMATS)
            if(!result) {

                def parser = new DefaultAcceptHeaderParser()
                def header = delegate.contentType
                if(!header) header = delegate.getHeader(HttpHeaders.CONTENT_TYPE)
                if(!header) header = delegate.getHeader(HttpHeaders.ACCEPT)
                result = parser.parse(header)

                delegate.setAttribute(GrailsApplicationAttributes.REQUEST_FORMATS, result)
            }
            result
        }

        // TODO: Remove toList() here when bug in Groovy related to arrays and spreadlists is fixed. (GROOVY-2333)
        addWithFormatMethod(application.controllerClasses.toList())

    }


    def onChange = { event ->
        addWithFormatMethod([event.source])
    }

    private addWithFormatMethod(controllers) {
        controllers*.metaClass*.withFormat = { Closure callable ->
            def formats = [:]
            try {
                callable.delegate = new FormatInterceptor()
                callable.resolveStrategy = Closure.DELEGATE_ONLY
                callable.call()
                formats = callable.delegate.formatOptions
            } finally {
                callable.delegate = delegate
                callable.resolveStrategy = Closure.OWNER_FIRST
            }


            def result
            def req = request
            def mimeTypes = req.mimeTypes
            def format = req.format
            if(formats) {
                if(format == 'all') {
                    def first = formats.entrySet().iterator().next()
                    result = getResponseForFormat(first.value, first.key, req)
                }
                else {
                    // if the format has been specified then use that
                    if(formats.containsKey(format)) {
                        result = getResponseForFormat(formats[format], format, req)
                    }
                    // otherwise look for the best match
                    else {
                        for(mime in req.mimeTypes) {
                            if(formats.containsKey(mime.extension)) {
                                result = getResponseForFormat(formats[mime.extension], mime.extension, req)
                                break
                            }
                        }
                    }
                }
            }
            result
        }
    }

    private getResponseForFormat(formatResponse, format, req) {
        req[GrailsApplicationAttributes.CONTENT_FORMAT] = format
        if(formatResponse instanceof Map) {
            return formatResponse
        }
        else {
            return formatResponse?.call()
        }

    }
}
class FormatInterceptor {
    def formatOptions = [:]
    Object invokeMethod(String name,args) {
        if(args.size() > 0 && (args[0] instanceof Closure || args[0] instanceof Map)) {
            formatOptions[name] = args[0] 
        }
        else {
            formatOptions[name] = null
        }
    }
}