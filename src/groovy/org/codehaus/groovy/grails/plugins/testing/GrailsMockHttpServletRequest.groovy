/* Copyright 2008 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.testing

import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.mock.web.MockHttpServletRequest

/**
 * A custom mock HTTP servlet request that provides the extra properties
 * and methods normally injected by the "servlets" plugin.
 */
class GrailsMockHttpServletRequest extends MockHttpServletRequest {
    boolean invalidToken

    private cachedJson
    private cachedXml

    /**
     * Implementation of the dynamic "forwardURI" property.
     */
    String getForwardURI() {
        def result = getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE)
        if (!result) result = requestURI
        return result
    }

    /**
     * Sets the "forwardURI" property for the request.
     */
    void setForwardURI(String uri) {
        setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, uri)
    }

    /**
     * Indicates whether this is an AJAX request or not (as far as
     * Grails is concerned). Returns <code>true</code> if it is an
     * AJAX request, otherwise <code>false</code>.
     */
    boolean isXhr() {
        return getHeader("X-Requested-With") == "XMLHttpRequest"
    }

    /**
     * Makes this request an AJAX request as Grails understands it.
     * This cannot be undone, so if you need a non-AJAX request you
     * will have to create a new instance.
     */
    void makeAjaxRequest() {
        addHeader("X-Requested-With", "XMLHttpRequest")
    }

    /**
     * Map-like access to request attributes, e.g. request["count"].
     */
    def getAt(String key) {
        return getAttribute(key)
    }

    /**
     * Map-like setting of request attributes, e.g. request["count"] = 10.
     */
    void putAt(String key, Object val) {
        setAttribute(key, val)
    }

    /**
     * Property access for request attributes.
     */
    def getProperty(String name) {
        def mp = getClass().metaClass.getMetaProperty(name)
        if (mp) {
            return mp.getProperty(this)
        }
        else {
            return getAttribute(name)
        }
    }

    /**
     * Property setting of request attributes.
     */
    void setProperty(String name, value) {
        def mp = getClass().metaClass.getMetaProperty(name)
        if (mp) {
            mp.setProperty(this, value)
        }
        else {
            setAttribute(name, value)
        }
    }

    boolean isGet() {
        method == "GET"
    }

    boolean isPost() {
        method == "POST"
    }

    /**
     * Parses the request content as XML using XmlSlurper and returns
     * the GPath result object. Throws an exception if there is no
     * content or the content is not valid XML.
     */
    def getXML() {
        if (!cachedXml) {
            cachedXml = grails.converters.XML.parse(this)
        }
        return cachedXml
    }

    /**
     * Parses the request content as JSON using the JSON converter.
     * Throws an exception if there is no content or the content is
     * not valid JSON.
     */
    def getJSON() {
        if (!cachedJson) {
            cachedJson = grails.converters.JSON.parse(this)
        }
        return cachedJson
    }

    /**
     * Adds a "find()" method to the request that searches the request's
     * attributes. Returns the first attribute for which the closure
     * returns <code>true</code>, just like the normal Groovy find()
     * method.
     */
    def find(Closure c) {
        def result = [:]
        for (String name in attributeNames) {
            def match = false
            switch (c.parameterTypes.length) {
            case 0:
                match = c.call()
                break

            case 1:
                match = c.call(key:name, value:getAttribute(name))
                break

            default:
                match =  c.call(name, getAttribute(name))
            }
            if (match) {
                result[name] = getAttribute(name)
                break
            }
        }

        return result
    }

    /**
     * Like the {@link #find(Closure)} method, this searches the request
     * attributes. Returns all the attributes that match the closure
     * conditions.
     */
    def findAll(Closure c) {
       def results = [:]
       for (String name in attributeNames) {
            def match = false
            switch (c.parameterTypes.length) {
            case 0:
                match = c.call()
                break

            case 1:
                match = c.call(key:name, value:getAttribute(name))
                break

            default:
               match =  c.call(name, getAttribute(name))
            }
            if (match) { results[name] = getAttribute(name) }
       }
       results
    }

    /**
     * Iterates over the request attributes.
     */
    def each(Closure c) {
        for (String name in attributeNames) {
            switch (c.parameterTypes.length) {
            case 0:
                c.call()
                break

            case 1:
                c.call(key:name, value:getAttribute(name))
                break

            default:
                c.call(name, getAttribute(name))
            }
        }
    }
}
