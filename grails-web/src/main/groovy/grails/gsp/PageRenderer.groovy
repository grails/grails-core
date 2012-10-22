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
package grails.gsp

import java.security.Principal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

import javax.servlet.AsyncContext
import javax.servlet.DispatcherType
import javax.servlet.RequestDispatcher
import javax.servlet.ServletContext
import javax.servlet.ServletInputStream
import javax.servlet.ServletOutputStream
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import javax.servlet.http.Part
import org.apache.commons.collections.iterators.IteratorEnumeration
import org.codehaus.groovy.grails.web.pages.FastStringWriter
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.pages.GroovyPagesUriSupport
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.web.context.ServletContextAware
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.ServletContextResourceLoader
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageLocator
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageResourceScriptSource
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageCompiledScriptSource
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator

/**
 * Simplified API for rendering GSP pages from services, jobs and other non-request classes.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class PageRenderer implements ApplicationContextAware, ServletContextAware{

    private GroovyPagesTemplateEngine templateEngine
    GrailsConventionGroovyPageLocator groovyPageLocator
    ApplicationContext applicationContext
    ServletContext servletContext

    PageRenderer(GroovyPagesTemplateEngine templateEngine) {
        this.templateEngine = templateEngine
    }

    /**
     * Renders a page and returns the contents
     *
     * @param args The named arguments
     *
     * @arg view The view to render. Must be an absolute view path since the controller name is unknown.
     * @arg template The template to render. Must be an absolute template path since the controller name is unknown.
     * @arg model The model to use for rendering
     *
     * @return The resulting string contents
     */
    String render(Map args) {
        def fsw = new FastStringWriter()
        renderViewToWriter(args, fsw)
        return fsw.toString()
    }

    /**
     * Renders a page and returns the contents
     *
     * @param args The named arguments
     * @param writer The target writer
     *
     * @arg view The view to render. Must be an absolute view path since the controller name is unknown.
     * @arg template The template to render. Must be an absolute template path since the controller name is unknown.
     * @arg model The model to use for rendering
     *
     * @return The resulting string contents
     */
    void renderTo(Map args, Writer writer) {
        renderViewToWriter(args, writer)
    }
    /**
     * Renders a page and returns the contents
     *
     * @param args The named arguments
     * @param stream The target stream
     *
     * @arg view The view to render. Must be an absolute view path since the controller name is unknown.
     * @arg template The template to render. Must be an absolute template path since the controller name is unknown.
     * @arg model The model to use for rendering
     *
     * @return The resulting string contents
     */
    void renderTo(Map args, OutputStream stream) {
        renderTo(args, new OutputStreamWriter(stream))
    }

    private void renderViewToWriter(Map args, Writer writer) {
        def source = null
        if (args.view) {
           source = groovyPageLocator.findViewByPath(args.view.toString())
        }
        else if (args.template) {
            source = groovyPageLocator.findTemplateByPath(args.template.toString())
        }
        if (source == null) {
            return
        }

        def oldRequestAttributes = RequestContextHolder.getRequestAttributes()
        try {
            def webRequest = new GrailsWebRequest(new PageRenderRequest(source.URI),
                  new PageRenderResponse(writer instanceof PrintWriter ? writer : new PrintWriter(writer)),
                  servletContext, applicationContext)
            RequestContextHolder.setRequestAttributes(webRequest)
            def template = templateEngine.createTemplate(source)
            if (template != null) {
                template.make(args.model ?: [:]).writeTo(writer)
            }
        } finally {
            RequestContextHolder.setRequestAttributes(oldRequestAttributes)
        }
    }

    protected GroovyPageScriptSource findResource(String basePath) {
        return groovyPageLocator.findViewByPath(basePath)
    }

    /*
     * A request object used during the GSP rendering pipeline for render operations outside a web request
     */
    class PageRenderRequest implements HttpServletRequest {

        PageRenderRequest(String requestURI) {
            this.requestURI = requestURI
        }

        def params = new ConcurrentHashMap()
        def attributes = new ConcurrentHashMap()

        String contentType
        String requestURI
        String characterEncoding = "UTF-8"

        String getAuthType() { null }

        Cookie[] getCookies() { return new Cookie[0] }

        long getDateHeader(String name) { -1L }

        String getHeader(String name) { null }

        Enumeration<?> getHeaders(String name) {
            return new IteratorEnumeration([].iterator())
        }

        Enumeration<?> getHeaderNames() {
            return new IteratorEnumeration([].iterator())
        }

        int getIntHeader(String name) { -1 }

        String getMethod() {"GET"}

        String getPathInfo() {""}

        String getPathTranslated() {""}

        String getContextPath() {"/"}

        String getQueryString() { ""}

        String getRemoteUser() { null }

        boolean isUserInRole(String role) { false }

        Principal getUserPrincipal() { null }

        String getRequestedSessionId() { null }

        StringBuffer getRequestURL() {
            return new StringBuffer(getRequestURI())
        }

        String getServletPath() {
            return "/"
        }

        HttpSession getSession(boolean create) { throw new UnsupportedOperationException("You cannot use the session in non-request rendering operations") }

        HttpSession getSession() { throw new UnsupportedOperationException("You cannot use the session in non-request rendering operations") }

        boolean isRequestedSessionIdValid() { true }

        boolean isRequestedSessionIdFromCookie() { false }

        boolean isRequestedSessionIdFromURL() { false }

        boolean isRequestedSessionIdFromUrl() { false }

        boolean authenticate(HttpServletResponse response) {
            return false
        }

        void login(String username, String password) {
            // no op
        }

        void logout() {
            // no op
        }

        Collection<Part> getParts() {
            return Collections.emptyList()
        }

        Part getPart(String name) {
            return null
        }

        Object getAttribute(String name) {
            return attributes[name]
        }

        Enumeration<?> getAttributeNames() {
            return attributes.keys()
        }

        int getContentLength() { 0 }

        ServletInputStream getInputStream() {
            throw new UnsupportedOperationException("You cannot read the input stream in non-request rendering operations")
        }

        String getParameter(String name) {
            return params[name]
        }

        Enumeration<?> getParameterNames() {
            return params.keys()
        }

        String[] getParameterValues(String name) {
            return new String[0]
        }

        Map<?, ?> getParameterMap() {
            return params
        }

        String getProtocol() {
            throw new UnsupportedOperationException("You cannot read the protocol in non-request rendering operations")
        }

        String getScheme() {
            throw new UnsupportedOperationException("You cannot read the scheme in non-request rendering operations")
        }

        String getServerName() {
            throw new UnsupportedOperationException("You cannot read server name in non-request rendering operations")
        }

        int getServerPort() {
            throw new UnsupportedOperationException("You cannot read the server port in non-request rendering operations")
        }

        BufferedReader getReader() {
            throw new UnsupportedOperationException("You cannot read input in non-request rendering operations")
        }

        String getRemoteAddr() {
            throw new UnsupportedOperationException("You cannot read the remote address in non-request rendering operations")
        }

        String getRemoteHost() {
            throw new UnsupportedOperationException("You cannot read the remote host in non-request rendering operations")
        }

        void setAttribute(String name, Object o) {
            if(o != null) {
                attributes[name] = o
            } else {
                attributes.remove name
            }
        }

        void removeAttribute(String name) {
            attributes.remove name
        }

        Locale getLocale() {
            return Locale.getDefault()
        }

        Enumeration<?> getLocales() {
            return new IteratorEnumeration(Locale.getAvailableLocales().iterator())
        }

        boolean isSecure() { false }

        RequestDispatcher getRequestDispatcher(String path) {
            throw new UnsupportedOperationException("You cannot use the request dispatcher in non-request rendering operations")
        }

        String getRealPath(String path) {
            return requestURI
        }

        int getRemotePort() {
            throw new UnsupportedOperationException("You cannot read the remote port in non-request rendering operations")
        }

        String getLocalName() {
            return "localhost"
        }

        String getLocalAddr() {
            return "127.0.0.1"
        }

        int getLocalPort() {
            return 80
        }

        ServletContext getServletContext() {
            return null  //To change body of implemented methods use File | Settings | File Templates.
        }

        AsyncContext startAsync() {
            return null  //To change body of implemented methods use File | Settings | File Templates.
        }

        AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
            return null  //To change body of implemented methods use File | Settings | File Templates.
        }

        boolean isAsyncStarted() {
            return false  //To change body of implemented methods use File | Settings | File Templates.
        }

        boolean isAsyncSupported() {
            return false  //To change body of implemented methods use File | Settings | File Templates.
        }

        AsyncContext getAsyncContext() {
            return null  //To change body of implemented methods use File | Settings | File Templates.
        }

        DispatcherType getDispatcherType() {
            return null  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    class PageRenderResponse implements HttpServletResponse {

        String characterEncoding = "UTF-8"
        String contentType
        Locale locale = Locale.getDefault()
        PrintWriter writer
        int bufferSize = 0

        PageRenderResponse(PrintWriter writer) {
            this.writer = writer
        }

        void addCookie(Cookie cookie) {
            // no-op
        }

        boolean containsHeader(String name) { false }

        String encodeURL(String url) { url }

        String encodeRedirectURL(String url) { url }

        String encodeUrl(String url) { url }

        String encodeRedirectUrl(String url) { url }

        void sendError(int sc, String msg) {
            // no-op
        }

        void sendError(int sc) {
            // no-op
        }

        void sendRedirect(String location) {
            // no-op
        }

        void setDateHeader(String name, long date) {
            // no-op
        }

        void addDateHeader(String name, long date) {
            // no-op
        }

        void setHeader(String name, String value) {
            // no-op
        }

        void addHeader(String name, String value) {
            // no-op
        }

        void setIntHeader(String name, int value) {
            // no-op
        }

        void addIntHeader(String name, int value) {
            // no-op
        }

        void setStatus(int sc) {
            // no-op
        }

        void setStatus(int sc, String sm) {
            // no-op
        }

        int getStatus() {
            return 0
        }

        String getHeader(String name) {
            return null
        }

        Collection<String> getHeaders(String name) {
            return null
        }

        Collection<String> getHeaderNames() {
            return Collections.emptyList()
        }

        ServletOutputStream getOutputStream() {
            throw new UnsupportedOperationException("You cannot use the OutputStream in non-request rendering operations. Use getWriter() instead")
        }

        void setContentLength(int len) {
            // no-op
        }

        void flushBuffer() {
           // no-op
        }

        void resetBuffer() {
           // no-op
        }

        boolean isCommitted() { false }

        void reset() {
            // no-op
        }
    }
}
