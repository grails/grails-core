/*
 * Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.web.mime

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.plugins.web.api.RequestMimeTypesApi
import org.codehaus.groovy.grails.plugins.web.api.ResponseMimeTypesApi
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.annotation.Autowired

/**
 * Resolves the {@link MimeType} instance for a request
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class DefaultMimeTypeResolver implements MimeTypeResolver{

    @Autowired ResponseMimeTypesApi responseMimeTypesApi
    @Autowired RequestMimeTypesApi requestMimeTypesApi

    /**
     * Resolve the {@link MimeType} to be used for the response, typically established from the ACCEPT header
     *
     * @since 2.3
     * @return the {@link MimeType}
     */
    @Override
    MimeType resolveResponseMimeType(GrailsWebRequest webRequest= GrailsWebRequest.lookup()) {
        if (webRequest != null) {
            return responseMimeTypesApi.getMimeTypeForRequest(webRequest)
        }
        return null
    }

    /**
     * Resolve the {@link MimeType} to be used for the request, typically established from the CONTENT_TYPE header
     *
     * @since 2.3
     * @return the {@link MimeType}
     */
    @Override
    MimeType resolveRequestMimeType(GrailsWebRequest webRequest = GrailsWebRequest.lookup()) {
        if (webRequest != null) {
            final allMimeTypes = requestMimeTypesApi.getMimeTypes(webRequest.request)
            if(allMimeTypes) {
                return allMimeTypes[0]
            }
        }
        return null
    }
}
