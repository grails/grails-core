/*
 * Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.context;

import grails.util.Holders;

import javax.servlet.ServletContext;

/**
 * Holds a reference to the ServletContext.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class ServletContextHolder {

    public static void setServletContext(final ServletContext servletContext) {
        Holders.setServletContext(servletContext);
    }

    public static ServletContext getServletContext() {
        return Holders.getServletContext();
    }
}
