/*
 * Copyright 2014 original authors
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
package org.grails.web.filters

import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import groovy.util.logging.Commons
import org.grails.plugins.web.taglib.JavascriptTagLib
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse



/**
 * @author Graeme Rocher
 * @since 3.0
 */
@Commons
@CompileStatic
class JavascriptLibraryHandlerInterceptor extends HandlerInterceptorAdapter  {

    public static final String JAVA_SCRIPT_LIBRARY = "grails.views.javascript.library"

    protected String library

    public JavascriptLibraryHandlerInterceptor(GrailsApplication application) {
        library = application.config.getProperty(JAVA_SCRIPT_LIBRARY)
        log.debug "Using [$library] as the default Ajax provider."
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (library) {
            List libraries = (List)request.getAttribute(JavascriptTagLib.INCLUDED_LIBRARIES)
            if (libraries == null) {
                libraries = new ArrayList<String>(1)
                request.setAttribute(JavascriptTagLib.INCLUDED_LIBRARIES, libraries)
            }
            libraries<< library
        }
        return true
    }
}
