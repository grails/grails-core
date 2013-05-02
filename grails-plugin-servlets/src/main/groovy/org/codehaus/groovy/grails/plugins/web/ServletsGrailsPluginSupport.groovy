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
package org.codehaus.groovy.grails.plugins.web

import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.web.api.ServletRequestApi

/**
 * Support class for the Servlets Grails plugin.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class ServletsGrailsPluginSupport {

    static enhanceServletApi(ConfigObject config = new ConfigObject()) {

        def getAttributeClosure = { String name ->
            def mp = delegate.class.metaClass.getMetaProperty(name)
            return mp ? mp.getProperty(delegate) : delegate.getAttribute(name)
        }

        def setAttributeClosure = { String name, value ->
            def mp = delegate.class.metaClass.getMetaProperty(name)
            if (mp) {
                mp.setProperty(delegate, value)
            }
            else {
                delegate.setAttribute(name, value)
            }
        }

        def getAttributeSubscript = { String key ->
            delegate.getAttribute(key)
        }
        def setAttributeSubScript = { String key, Object val ->
            delegate.setAttribute(key, val)
        }
        // enables access to servlet context with servletContext.foo syntax
        ServletContext.metaClass.getProperty = getAttributeClosure
        ServletContext.metaClass.setProperty = setAttributeClosure
        ServletContext.metaClass.getAt = getAttributeSubscript
        ServletContext.metaClass.putAt = setAttributeSubScript

        // enables access to session attributes using session.foo syntax
        HttpSession.metaClass.getProperty = getAttributeClosure
        HttpSession.metaClass.setProperty = setAttributeClosure
        // enables access to session attributes with session["foo"] syntax
        HttpSession.metaClass.getAt = getAttributeSubscript
        // enables setting of session attributes with session["foo"] = "bar" syntax
        HttpSession.metaClass.putAt = setAttributeSubScript
        // enables access to request attributes with request["foo"] syntax
        HttpServletRequest.metaClass.getAt = { String key ->
            delegate.getAttribute(key)
        }
        // enables setting of request attributes with request["foo"] = "bar" syntax
        HttpServletRequest.metaClass.putAt = { String key, Object val ->
            delegate.setAttribute(key, val)
        }
        // enables access to request attributes using property syntax
        HttpServletRequest.metaClass.getProperty = getAttributeClosure
        HttpServletRequest.metaClass.setProperty = setAttributeClosure

        final servletRequestApi = new ServletRequestApi()
        final xhrIdentifier = config?.grails?.web?.xhr?.identifier
        if (xhrIdentifier instanceof Closure) {
            servletRequestApi.xhrRequestIdentifier = xhrIdentifier
        }
        def requestEnhancer = new MetaClassEnhancer()
        requestEnhancer.addApi servletRequestApi
        requestEnhancer.enhance HttpServletRequest.metaClass

        // allows the syntax response << "foo"
        HttpServletResponse.metaClass.leftShift = { Object o ->
            delegate.writer << o
        }
    }
}
