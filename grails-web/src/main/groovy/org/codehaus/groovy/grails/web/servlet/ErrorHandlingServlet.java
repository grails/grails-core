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

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;
import org.codehaus.groovy.grails.web.errors.GrailsWrappedRuntimeException;
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.IncludeResponseWrapper;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * A servlet for handling errors.
 *
 * @author mike
 * @since 1.0-RC1
 */
public class ErrorHandlingServlet extends GrailsDispatcherServlet {

    private static final long serialVersionUID = 8792197458391395589L;
    private static final String TEXT_HTML = "text/html";
    private static final String GSP_SUFFIX = ".gsp";
    private static final String JSP_SUFFIX = ".jsp";

    @Override
    protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
        return request; // ignore multipart requests when an error occurs
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doDispatch(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        int statusCode;

        if (request.getAttribute("javax.servlet.error.status_code") != null) {
            statusCode = Integer.parseInt(request.getAttribute("javax.servlet.error.status_code").toString());
        }
        else {
            statusCode = 500;
        }

        Throwable t = null;
        if (request.getAttribute("javax.servlet.error.exception") != null) {
            t = (Throwable)request.getAttribute("javax.servlet.error.exception");
            if (!(t instanceof GrailsWrappedRuntimeException) && request.getAttribute("exception") == null) {
                request.setAttribute("exception", new GrailsWrappedRuntimeException(getServletContext(), t));
            }
        }
        final UrlMappingsHolder urlMappingsHolder = lookupUrlMappings();
        UrlMappingInfo matchedInfo = null;
        if (t != null) {
            matchedInfo = urlMappingsHolder.matchStatusCode(statusCode, t);
            if (matchedInfo == null) {
                matchedInfo = urlMappingsHolder.matchStatusCode(statusCode, GrailsExceptionResolver.getRootCause(t));
            }
        }

        if (matchedInfo == null) {
            matchedInfo = urlMappingsHolder.matchStatusCode(statusCode);
        }
        final UrlMappingInfo urlMappingInfo = matchedInfo;

        if (urlMappingInfo != null) {
            final GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();
            request.setAttribute("com.opensymphony.sitemesh.APPLIED_ONCE", null);
            urlMappingInfo.configure(webRequest);

            String viewName = urlMappingInfo.getViewName();
            if (viewName == null || viewName.endsWith(GSP_SUFFIX) || viewName.endsWith(JSP_SUFFIX)) {
                WebUtils.forwardRequestForUrlMappingInfo(request, response, urlMappingInfo, Collections.EMPTY_MAP);
            }
            else {
                ViewResolver viewResolver = WebUtils.lookupViewResolver(getServletContext());
                if (viewResolver != null) {
                    View v;
                    try {
                        v = WebUtils.resolveView(request, urlMappingInfo, viewName, viewResolver);
                        IncludeResponseWrapper includeResponse = new IncludeResponseWrapper(response);
                        v.render(Collections.EMPTY_MAP, request, response);
                    }
                    catch (Exception e) {
                        throw new UrlMappingException("Error mapping onto view ["+viewName+"]: " + e.getMessage(),e);
                    }
                }
            }
        }
        else {
            renderDefaultResponse(response, statusCode);
        }
    }

    private void renderDefaultResponse(HttpServletResponse response, int statusCode) throws IOException {
        if (statusCode == 404) {
            renderDefaultResponse(response, statusCode, "Not Found", "Page not found.");
        }
        else {
            renderDefaultResponse(response, statusCode, "Internal Error", "Internal server error.");
        }
    }

    private void renderDefaultResponse(HttpServletResponse response, int statusCode, String title, String text) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType(TEXT_HTML);

        Writer writer = response.getWriter();

        writer.write("<HTML>\n<HEAD>\n<TITLE>Error " + statusCode + " - " + title);
        writer.write("</TITLE>\n<BODY>\n<H2>Error " + statusCode + " - " + title + ".</H2>\n");
        writer.write(text + "<BR/>");

        for (int i = 0; i < 20; i++) {
            writer.write("\n<!-- Padding for IE                  -->");
        }

        writer.write("\n</BODY>\n</HTML>\n");
        writer.flush();
    }

    private UrlMappingsHolder lookupUrlMappings() {
        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        return (UrlMappingsHolder)wac.getBean(UrlMappingsHolder.BEAN_ID);
    }
}
