/*
 * Copyright 2015 the original author or authors.
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
package grails.artefact.controller.support

import grails.web.UrlConverter
import grails.web.api.WebAttributes
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.transform.CompileStatic
import org.grails.plugins.web.controllers.metaclass.ForwardMethod
import org.grails.web.mapping.ForwardUrlMappingInfo
import org.grails.web.mapping.UrlMappingUtils
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.DataBinder

/**
 * A Trait for classes that forward the request
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
trait RequestForwarder implements WebAttributes {
    private UrlConverter urlConverter

    @Autowired(required=false)
    void setUrlConverter(UrlConverter urlConverter) {
        this.urlConverter = urlConverter
    }

    /**
     * Forwards a request for the given parameters using the RequestDispatchers forward method
     *
     * @param params The parameters
     * @return The forwarded URL
     */
    String forward(Map params) {
        def urlInfo = new ForwardUrlMappingInfo()
        DataBinder binder = new DataBinder(urlInfo)
        binder.bind(new MutablePropertyValues(params))

        GrailsWebRequest webRequest = getWebRequest()

        if (webRequest) {
            def controllerName
            if(params.controller) {
                controllerName = params.controller
            } else {
                controllerName = webRequest.controllerName
            }

            if(controllerName) {
                def convertedControllerName = convert(controllerName.toString())
                webRequest.controllerName = convertedControllerName
            }
            urlInfo.controllerName = webRequest.controllerName

            if(params.action) {
                urlInfo.actionName = convert(params.action.toString())
            }

            if(params.namespace) {
                urlInfo.namespace = params.namespace
            }

            if(params.plugin) {
                urlInfo.pluginName = params.plugin
            }
        }

        def model = params.model instanceof Map ? params.model : Collections.EMPTY_MAP

        def request = webRequest.currentRequest
        def response = webRequest.currentResponse
        request.setAttribute(ForwardMethod.IN_PROGRESS, true)

        if(params.params instanceof Map) {
            urlInfo.parameters.putAll((Map)params.params)
        }
        request.setAttribute(ForwardMethod.CALLED, true)
        String uri = UrlMappingUtils.forwardRequestForUrlMappingInfo(request, response, urlInfo, (Map)model, true)
        return uri
    }


    private String convert(String value) {
        (urlConverter) ? urlConverter.toUrlElement(value) : value
    }
}