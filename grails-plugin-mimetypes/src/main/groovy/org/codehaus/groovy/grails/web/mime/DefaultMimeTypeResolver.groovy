/*
 * Copyright 2013 GoPivotal, Inc. All Rights Reserved.
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

    @Override
    /**
     * Resolve the {@link MimeType} to be used for the response, typically established from the ACCEPT header
     *
     * @since 2.3
     * @return the {@link MimeType}
     */
    MimeType resolveResponseMimeType() {
        resolveResponseMimeType(GrailsWebRequest.lookup())
    }

    @Override
    MimeType resolveResponseMimeType(GrailsWebRequest webRequest) {
        if (webRequest != null) {
            return responseMimeTypesApi.getMimeTypeForRequest(webRequest)
        }
        return null
    }
}
