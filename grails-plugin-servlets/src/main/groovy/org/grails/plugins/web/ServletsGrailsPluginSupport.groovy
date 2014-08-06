/*
 * Copyright 2010 SpringSource
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
package org.grails.plugins.web

import grails.util.GrailsMetaClassUtils

import javax.servlet.http.HttpServletRequest

import org.grails.core.metaclass.MetaClassEnhancer
import org.grails.plugins.web.api.ServletRequestApi

/**
 * Support class for the Servlets Grails plugin.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class ServletsGrailsPluginSupport {

    static enhanceServletApi(ConfigObject config = new ConfigObject()) {
        // enables access to request attributes with request["foo"] syntax
        def requestMetaClass = GrailsMetaClassUtils.getExpandoMetaClass(HttpServletRequest)

        final servletRequestApi = new ServletRequestApi()
        final xhrIdentifier = config?.grails?.web?.xhr?.identifier
        if (xhrIdentifier instanceof Closure) {
            servletRequestApi.xhrRequestIdentifier = xhrIdentifier
        }
        def requestEnhancer = new MetaClassEnhancer()
        requestEnhancer.addApi servletRequestApi
        requestEnhancer.enhance requestMetaClass
    }
}
