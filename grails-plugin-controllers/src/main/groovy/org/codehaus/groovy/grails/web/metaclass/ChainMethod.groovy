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
package org.codehaus.groovy.grails.web.metaclass

import grails.util.GrailsNameUtils

import javax.servlet.http.HttpServletRequest

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.web.mapping.UrlCreator
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.support.RequestDataValueProcessor

/**
 * Implementation of the chain() method for controllers.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class ChainMethod {

    static invoke(target, Map args = [:]) {
        def controller = args.controller ?: GrailsNameUtils.getLogicalPropertyName(
            target.getClass().name, ControllerArtefactHandler.TYPE)
        def action = args.action
        def plugin = args.remove('plugin')
        def id = args.id
        def params = args.params ?: [:]
        def model = args.model ?: [:]

        def actionParams = params.findAll { it.key?.startsWith('_action_') }
        actionParams.each { params.remove(it.key) }

        GrailsWebRequest webRequest = RequestContextHolder.currentRequestAttributes()
        def flash = webRequest.getFlashScope()

        def chainModel = flash.chainModel
        if (chainModel instanceof Map) {
            chainModel.putAll(model)
            model = chainModel
        }
        flash.chainModel = model

        if (action instanceof Closure) {
            def prop = GCU.getPropertyDescriptorForValue(target, action)
            if (prop) {
                action = prop.name
            }
        }
        else {
            action = action?.toString()
        }

        def appCtx = webRequest.getApplicationContext()

        UrlMappingsHolder mappings = appCtx.getBean(UrlMappingsHolder.BEAN_ID)

        // Make sure that if an ID was given, it is used to evaluate
        // the reverse URL mapping.
        if (id) params.id = id

        UrlCreator creator = mappings.getReverseMapping(controller, action, plugin, params)
        def response = webRequest.getCurrentResponse()

        String url = creator.createURL(controller, action, plugin, params, 'utf-8')

        if (appCtx.containsBean("requestDataValueProcessor")) {
            RequestDataValueProcessor valueProcessor = appCtx.getBean("requestDataValueProcessor")
            if (valueProcessor != null) {
                HttpServletRequest request = webRequest.getCurrentRequest()
                url = response.encodeRedirectURL(valueProcessor.processUrl(request, url))
            }
        } else {
            url = response.encodeRedirectURL(url)
        }
        response.sendRedirect url
    }
}
