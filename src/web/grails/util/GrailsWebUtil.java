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
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
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
    
    public static GrailsWebRequest bindMockWebRequest(GrailsWebApplicationContext ctx) {
        GrailsWebRequest webRequest = new GrailsWebRequest(
                                                new MockHttpServletRequest(),
                                                new MockHttpServletResponse(),
                                                ctx.getServletContext()
                                            );
        RequestContextHolder.setRequestAttributes(webRequest);
        return webRequest;
    }

    public static GrailsWebRequest bindMockWebRequest() {
        GrailsWebRequest webRequest = new GrailsWebRequest(
                                                new MockHttpServletRequest(),
                                                new MockHttpServletResponse(),
                                                new MockServletContext()
                                            );
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

    public static GroovyObject getControllerFromRequest(HttpServletRequest request) {
        return (GroovyObject)request.getAttribute(GrailsApplicationAttributes.CONTROLLER);
    }
}
