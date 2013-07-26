/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.metaclass

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import grails.web.*
import org.apache.commons.beanutils.BeanUtils
import org.codehaus.groovy.grails.web.mapping.ForwardUrlMappingInfo
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.context.ApplicationContext

/**
 * Implements performing a forward.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
class ForwardMethod {

    public static final String IN_PROGRESS = "org.codehaus.groovy.grails.FORWARD_IN_PROGRESS"
    public static final String CALLED = "org.codehaus.groovy.grails.FORWARD_CALLED"

    private UrlConverter urlConverter

    String forward(HttpServletRequest request, HttpServletResponse response, Map params) {
        def urlInfo = new ForwardUrlMappingInfo()

        GrailsWebRequest webRequest = GrailsWebRequest.lookup(request)

        if (params.controller) {
            webRequest?.controllerName = params.controller
        }
        else {
            urlInfo.controllerName = webRequest?.controllerName
        }

        BeanUtils.populate(urlInfo, params)

        def model = params.model instanceof Map ? params.model : Collections.EMPTY_MAP
        request.setAttribute(IN_PROGRESS, true)
        String uri = WebUtils.forwardRequestForUrlMappingInfo(request, response, urlInfo, model, true)
        request.setAttribute(CALLED, true)
        return uri
    }

    void setUrlConverter(UrlConverter urlConverter) {
        this.urlConverter = urlConverter
    }

    private UrlConverter lookupUrlConverter(GrailsWebRequest webRequest) {
        if (!urlConverter) {
            ApplicationContext applicationContext = webRequest?.getApplicationContext()
            if (applicationContext) {
                urlConverter = applicationContext.getBean("grailsUrlConverter", UrlConverter)
            }
        }

        urlConverter
    }

    private String convert(GrailsWebRequest webRequest, String value) {
        UrlConverter urlConverter = lookupUrlConverter(webRequest)
        (urlConverter) ? urlConverter.toUrlElement(value) : value
    }

}
