/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.filters;

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Based off the Spring implementation, but this one works with Grails params object
 * and also deals with the X-HTTP-Method-Override HTTP header.
 *
 * @see org.springframework.web.filter.HiddenHttpMethodFilter
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class HiddenHttpMethodFilter extends OncePerRequestFilter{

    /** Default method parameter: <code>_method</code> */
    public static final String DEFAULT_METHOD_PARAM = "_method";

    private String methodParam = DEFAULT_METHOD_PARAM;
    public static final String HEADER_X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";


    /**
     * Set the parameter name to look for HTTP methods.
     * @see #DEFAULT_METHOD_PARAM
     */
    public void setMethodParam(String methodParam) {
        Assert.hasText(methodParam, "'methodParam' must not be empty");
        this.methodParam = methodParam;
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if("POST".equalsIgnoreCase(request.getMethod())) {
            String httpMethod = getHttpMethodOverride(request);
            if(StringUtils.hasLength(httpMethod))
                filterChain.doFilter(new HttpMethodRequestWrapper(httpMethod, request), response);
            else
                filterChain.doFilter(request, response);
        }
        else {
            filterChain.doFilter(request, response);
        }

    }

    protected String getHttpMethodOverride(HttpServletRequest request) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup(request);
        String httpMethod = null;
        if(webRequest != null) {
            final Object val = webRequest.getParams().get(methodParam);
            if(val!=null)
                httpMethod = val.toString();
        }
        else {
            httpMethod = request.getParameter(methodParam);
        }
        if(httpMethod == null) {
            httpMethod = request.getHeader(HEADER_X_HTTP_METHOD_OVERRIDE);
        }
        return httpMethod;
    }

    /**
     * Simple {@link HttpServletRequest} wrapper that returns the supplied method for
     * {@link HttpServletRequest#getMethod()}.
     */
    private static class HttpMethodRequestWrapper extends HttpServletRequestWrapper {

        private final String method;

        public HttpMethodRequestWrapper(String method, HttpServletRequest request) {
            super(request);
            this.method = method;
        }

        public String getMethod() {
            return this.method;
        }
    }
    
}
