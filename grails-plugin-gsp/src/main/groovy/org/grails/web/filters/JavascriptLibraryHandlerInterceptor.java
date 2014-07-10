/*
 * Copyright 2013 the original author or authors.
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
package org.grails.web.filters;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import grails.core.GrailsApplication;
import org.grails.plugins.web.taglib.JavascriptTagLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Sets up the Javascript library to use based on configuration.
 *
 * @author Burt Beckwith
 * @since 2.3
 */
public class JavascriptLibraryHandlerInterceptor extends HandlerInterceptorAdapter  {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected String library;

    public JavascriptLibraryHandlerInterceptor(GrailsApplication application) {
        Object lib = application.getFlatConfig().get("grails.views.javascript.library");
        if (lib instanceof CharSequence) {
            library = lib.toString();
            log.debug("Using [{}] as the default Ajax provider.", library);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (library != null) {
            @SuppressWarnings("unchecked")
            List<String> libraries = (List<String>) request.getAttribute(JavascriptTagLib.INCLUDED_LIBRARIES);
            if (libraries == null) {
                libraries = new ArrayList<String>(1);
                request.setAttribute(JavascriptTagLib.INCLUDED_LIBRARIES, libraries);
            }
            libraries.add(library);
        }
        return true;
    }
}
