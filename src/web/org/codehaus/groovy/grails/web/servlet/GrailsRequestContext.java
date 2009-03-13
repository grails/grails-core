/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.servlet;

import org.springframework.context.ApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.Writer;
import java.util.Map;

/**
 * An interface that defines the methods and objects available
 * during a Grails request context
 *
 * @author Graeme Rocher
 * @since 0.6
 *
 *        <p/>
 *        Created: Jul 20, 2007
 *        Time: 6:28:17 PM
 */
public interface GrailsRequestContext {

    /**
     * The request object
     * @return The request object
     */
    HttpServletRequest getRequest();

    /**
     * The response object
     * @return The response object
     */
    HttpServletResponse getResponse();

    /**
     * The session object
     * @return The session object
     */
    HttpSession getSession();

    /**
     * The servletContext object
     * @return The servletContext Object
     */
    ServletContext getServletContext();

    /**
     * The params object
     * @return The params object
     */
    Map getParams();

    /**
     * The ApplicationContext instance
     *
     * @return The ApplicationCOntext
     */
    ApplicationContext getApplicationContext();

    /**
     * The response writer
     * @return The response writer
     */
    Writer getOut();


    /**
     * @return The Action name
     */
    String getActionName();


    /**
     * @return The Controller Name
     */
    String getControllerName();

    /**
     * @return The Request URI
     */
    String getRequestURI();

}
