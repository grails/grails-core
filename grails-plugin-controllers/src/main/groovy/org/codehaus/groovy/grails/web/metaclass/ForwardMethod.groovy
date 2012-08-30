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

import grails.web.UrlConverter

import org.apache.commons.beanutils.BeanUtils
import org.codehaus.groovy.grails.web.mapping.ForwardUrlMappingInfo
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.context.ApplicationContext

/**
 * Implements performing a forward.
 *
 * @author Graeme Rocher
 * @author Jonathan Pearlin
 * @since 1.1
 */
class ForwardMethod {

    public static final String CALLED = "org.codehaus.groovy.grails.FORWARD_CALLED"

    UrlConverter urlConverter

    String forward(HttpServletRequest request, HttpServletResponse response, Map params) {
        def urlInfo = new ForwardUrlMappingInfo()

        GrailsWebRequest webRequest = GrailsWebRequest.lookup(request)
        Map cleansedParams = [:]
		
        /*
         * Make sure that any controller/action parameter is passed through the URL converter
         * to ensure that it will match the URL mappings based on the configured URL converter
         * scheme (camel case, hyphenated, etc).
         */
        cleansedParams = params?.collectEntries([:]) { key, value ->
            (key == 'controller' || key == 'action') ? [(key) : (convert(webRequest, value))] : [(key) : value]
        }

        // If the cleansed map contains a controller/action parameter, use it.  Otherwise, cleanse the value from the web request and use that.
        urlInfo.controllerName = (cleansedParams.controller) ? cleansedParams.controller : webRequest?.controllerName
        urlInfo.actionName = (cleansedParams.action) ? cleansedParams.action : webRequest?.actionName
        BeanUtils.populate(urlInfo, cleansedParams)

        def model = cleansedParams.model instanceof Map ? cleansedParams.model : Collections.EMPTY_MAP
        String uri = WebUtils.forwardRequestForUrlMappingInfo(request, response, urlInfo, model, true)
        request.setAttribute(CALLED, true)
        return uri
    }

    void setUrlConverter(UrlConverter urlConverter) {
        this.urlConverter = urlConverter
    }

    private UrlConverter getUrlConverter(GrailsWebRequest webRequest) {
        if (!urlConverter) {
            ApplicationContext applicationContext = webRequest?.getApplicationContext()
            if (applicationContext) {
                urlConverter = applicationContext.getBean("grailsUrlConverter", UrlConverter)
            }
        }

        urlConverter
    }

    private String convert(GrailsWebRequest webRequest, String value) {
        UrlConverter urlConverter = getUrlConverter(webRequest)
        (urlConverter) ? urlConverter.toUrlElement(value) : value
    }
}
