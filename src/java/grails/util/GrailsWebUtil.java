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
package grails.util;

import groovy.lang.GroovyObject;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.mvc.ParameterCreationListener;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;

/**
 * Utility methods for clients using the web framework
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 19, 2007
 *        Time: 6:18:22 PM
 */
public class GrailsWebUtil {
    public static final String DEFAULT_ENCODING = "UTF-8";
    private static final String CHARSET_ATTRIBUTE = ";charset=";

    /**
     * Binds a Mock implementation of a GrailsWebRequest object to the current thread. The mock version uses
     * instances of the Spring MockHttpServletRequest, MockHttpServletResponse and MockServletContext classes
     *
     * @param ctx The WebApplicationContext to use
     *
     * @see org.springframework.mock.web.MockHttpServletRequest
     * @see org.springframework.mock.web.MockHttpServletResponse
     * @see org.springframework.mock.web.MockServletContext
     *
     * @return The GrailsWebRequest instance
     */

    public static GrailsWebRequest bindMockWebRequest(WebApplicationContext ctx) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        GrailsWebRequest webRequest = new GrailsWebRequest(
                                                request,
                                                new MockHttpServletResponse(),
                                                ctx.getServletContext()
                                            );
        request.setAttribute(GrailsApplicationAttributes.WEB_REQUEST, webRequest);
        String[] paramListenerBeans = ctx.getBeanNamesForType(ParameterCreationListener.class);
        for (String paramListenerBean : paramListenerBeans) {
            ParameterCreationListener creationListenerBean = (ParameterCreationListener) ctx.getBean(paramListenerBean);
            webRequest.addParameterListener(creationListenerBean);
        }
        RequestContextHolder.setRequestAttributes(webRequest);
        return webRequest;
    }

    /**
     * Binds a Mock implementation of a GrailsWebRequest object to the current thread. The mock version uses
     * instances of the Spring MockHttpServletRequest, MockHttpServletResponse and MockServletContext classes
     *
     * @see org.springframework.mock.web.MockHttpServletRequest
     * @see org.springframework.mock.web.MockHttpServletResponse
     * @see org.springframework.mock.web.MockServletContext
     *
     * @return The GrailsWebRequest instance
     */
    public static GrailsWebRequest bindMockWebRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        GrailsWebRequest webRequest = new GrailsWebRequest(
                                                request,
                                                new MockHttpServletResponse(),
                                                new MockServletContext()
                                            );
        request.setAttribute(GrailsApplicationAttributes.WEB_REQUEST, webRequest);
        RequestContextHolder.setRequestAttributes(webRequest);
        return webRequest;
    }

    /**
     * Retrieves the URI from the request from either the include attribute or the request.getRequestURI() method
     *
     * @param request The HttpServletRequest instance
     * @return The String URI
     */
    public static String getUriFromRequest(HttpServletRequest request) {
        Object includeUri = request.getAttribute("javax.servlet.include.request_uri");
        String uri;
        if (includeUri != null) {
        	uri = (String) includeUri;
        } else {
        	uri = request.getRequestURI();
        }
        return uri;
    }

    /**
     * Obtains the currently executing controller from the given request if any
     * @param request The request object
     * @return The controller or null
     */
    public static GroovyObject getControllerFromRequest(HttpServletRequest request) {
        return (GroovyObject)request.getAttribute(GrailsApplicationAttributes.CONTROLLER);
    }

    public static String getContentType(String name, String encoding) {
        if(StringUtils.isBlank(encoding)) encoding = DEFAULT_ENCODING;
        return name + CHARSET_ATTRIBUTE + encoding;
    }

}
