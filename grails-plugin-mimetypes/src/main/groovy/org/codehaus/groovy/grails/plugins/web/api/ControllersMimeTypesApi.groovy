/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.plugins.web.api

import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletResponse

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

/**
 * Api extensions to controllers for the MimeTypes plugin.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@CompileStatic
class ControllersMimeTypesApi {

    protected MimeTypesApiSupport apiSupport = new MimeTypesApiSupport()

    /**
     * <p>The withFormat method is used to allow controllers to handle different types of
     * request formats such as HTML, XML and so on. Example usage:</p>
     *
     * <pre>
     * <code>
     *    withFormat {
     *        html { render "html" }
     *        xml { render "xml}
     *    }
     * </code>
     * </pre>
     *
     * @param instance
     * @param callable
     * @return  The result of the closure execution selected
     */
    def withFormat(instance, Closure callable) {
        HttpServletResponse response = GrailsWebRequest.lookup().currentResponse
        apiSupport.withFormat(response, callable)
    }
}
