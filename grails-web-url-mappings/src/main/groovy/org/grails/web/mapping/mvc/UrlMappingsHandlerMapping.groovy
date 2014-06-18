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
package org.grails.web.mapping.mvc

import groovy.transform.CompileStatic
import grails.web.mapping.UrlMapping
import grails.web.mapping.UrlMappingInfo
import grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.mime.MimeTypeResolver
import org.codehaus.groovy.grails.web.servlet.HttpHeaders
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.Assert
import org.springframework.web.servlet.handler.AbstractHandlerMapping
import org.springframework.web.util.UrlPathHelper

import javax.servlet.http.HttpServletRequest

/**
 *
 * Spring MVC {@link org.springframework.web.servlet.HandlerMapping} to match requests onto Grails controllers
 *
 * @since 3.0
 */
@CompileStatic
class UrlMappingsHandlerMapping extends AbstractHandlerMapping{


    protected UrlMappingsHolder urlMappingsHolder
    protected UrlPathHelper urlHelper = new UrlPathHelper();
    protected MimeTypeResolver mimeTypeResolver

    UrlMappingsHandlerMapping(UrlMappingsHolder urlMappingsHolder) {
        Assert.notNull(urlMappingsHolder, "Argument [urlMappingsHolder] cannot be null")
        this.urlMappingsHolder = urlMappingsHolder
        setOrder(-5)
    }


    @Autowired(required = false)
    void setMimeTypeResolver(MimeTypeResolver mimeTypeResolver) {
        this.mimeTypeResolver = mimeTypeResolver
    }

    @Override
    protected Object getHandlerInternal(HttpServletRequest request) throws Exception {

        String uri = urlHelper.getPathWithinApplication(request);
        def webRequest = GrailsWebRequest.lookup(request)

        Assert.notNull(webRequest, "HandlerMapping requires a Grails web request")

        String version = findRequestedVersion(webRequest)


        def infos = urlMappingsHolder.matchAll(uri, request.getMethod(), version != null ? version : UrlMapping.ANY_VERSION)

        for(UrlMappingInfo info in infos) {
            if(info) {
                if(info.redirectInfo) return info

                webRequest.resetParams()
                info.configure(webRequest)
                if(info instanceof GrailsControllerUrlMappingInfo) {
                   return info
                }
                else if(info.viewName || info.URI) return info
            }
        }

        return null
    }

    protected String findRequestedVersion(GrailsWebRequest currentRequest) {
        String version = currentRequest.getHeader(HttpHeaders.ACCEPT_VERSION)
        if(!version && mimeTypeResolver) {
            MimeType mimeType = mimeTypeResolver.resolveResponseMimeType(currentRequest)
            version = mimeType.version
        }
        return version
    }
}
