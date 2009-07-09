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
package org.codehaus.groovy.grails.web.pages.ext.jsp;

import groovy.lang.Binding;
import org.apache.commons.collections.ArrayStack;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.tagext.BodyContent;
import java.io.IOException;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;


/**
 * A JSP PageContext implementation for use with GSP
 *
 * @author Graeme Rocher
 * @since 1.1
 *
 *        <p/>
 *        Created: May 1, 2008
 */
public class GroovyPagesPageContext extends PageContext {
    private ServletContext servletContext;
    private Servlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private ServletConfig servletconfig;
    private Binding pageScope;
    private static final IteratorEnumeration EMPTY_ENUMERATION = new IteratorEnumeration();
    private GrailsWebRequest webRequest;
    private JspWriter jspOut;
    private ArrayStack outStack = new ArrayStack();
    private List tags = new ArrayList();
    private HttpSession session;

    public GroovyPagesPageContext(Servlet pagesServlet, Binding pageScope) {
        super();

        Assert.notNull(pagesServlet, "GroovyPagesPageContext class requires a reference to the GSP servlet");
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();

        this.servletContext = webRequest.getServletContext();
        this.request = webRequest.getCurrentRequest();
        this.response = webRequest.getCurrentResponse();
        this.servlet = pagesServlet;
        this.servletconfig = pagesServlet.getServletConfig();
        this.pageScope = pageScope;
        this.webRequest = webRequest;
        this.session = request.getSession(false);
        // setup initial writer
        pushWriter(new JspWriterDelegate(webRequest.getOut()));
        // Register page attributes as per JSP spec
        setAttribute(REQUEST, request);
        setAttribute(RESPONSE, response);
        if (session != null)
            setAttribute(SESSION, session);
        setAttribute(PAGE, servlet);
        setAttribute(CONFIG, servlet.getServletConfig());
        setAttribute(PAGECONTEXT, this);
        setAttribute(APPLICATION, servletContext);        
    }

    void popWriter() {
        outStack.pop();
        jspOut = (JspWriter) outStack.peek();
        setCurrentOut();
    }

    void pushWriter(JspWriter out) {
        outStack.push(out);
        jspOut = out;
        setCurrentOut();
    }

    private void setCurrentOut() {
        setAttribute(OUT, jspOut);
        setAttribute(GroovyPage.OUT, jspOut);
        webRequest.setOut(jspOut);
    }


    Object peekTopTag(Class tagClass) {
        for (ListIterator iter = tags.listIterator(tags.size()); iter.hasPrevious();)
        {
            Object tag = iter.previous();
            if(tagClass.isInstance(tag)) {
                return tag;
            }
        }
        return null;
    }

    void popTopTag() {
        tags.remove(tags.size() - 1);
    }

    void pushTopTag(Object tag) {
        tags.add(tag);
    }

    public BodyContent pushBody() {
        BodyContent bc = new BodyContentImpl(getOut(), true);
        pushWriter(bc);
        return bc;
    }

    public JspWriter popBody() {
        popWriter();
        return (JspWriter) getAttribute(OUT);
    }

    public GroovyPagesPageContext(Binding pageScope) {
        this(new GenericServlet() {
            public ServletConfig getServletConfig() {
                return this;
            }
            public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
                // do nothing;
            }
        }, pageScope != null ? pageScope : new Binding());
    }

    public GroovyPagesPageContext() {
        this(new Binding());
    }

    public void initialize(Servlet servlet, ServletRequest servletRequest, ServletResponse servletResponse, String errorPageURL, boolean needSession, int bufferSize, boolean autoFlush) throws IOException, IllegalStateException, IllegalArgumentException {
        // do nothing, not constructed for container
    }

    public void release() {
        // do nothing, not released by container
    }

    public HttpSession getSession() {
        return request.getSession(false);
    }

    public Object getPage() {
        return servlet;
    }

    public ServletRequest getRequest() {
        return this.request;
    }

    public ServletResponse getResponse() {
        return this.response;
    }

    public Exception getException() {
        throw new UnsupportedOperationException();
    }

    public ServletConfig getServletConfig() {
        return servletconfig;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void forward(String url) throws ServletException, IOException {
       request.getRequestDispatcher(url).forward(request, response);
    }

    public void include(String url) throws ServletException, IOException {
       include(url, false);
    }

    public void include(String url, boolean flush) throws ServletException, IOException {
       request.getRequestDispatcher(url).include(request, response);
    }

    public void handlePageException(Exception e) throws ServletException, IOException {
        throw new UnsupportedOperationException();
    }

    public void handlePageException(Throwable throwable) throws ServletException, IOException {
        throw new UnsupportedOperationException();
    }

    public void setAttribute(String name, Object value) {
        if(name == null) throw new NullPointerException("Attribute name cannot be null");
        pageScope.setVariable(name, value);
    }

    public void setAttribute(String name, Object value, int scope) {
        if(name == null) throw new NullPointerException("Attribute name cannot be null");
        switch(scope) {
            case PAGE_SCOPE:
                setAttribute(name, value);
            break;
            case REQUEST_SCOPE:
                request.setAttribute(name,value);
            break;
            case SESSION_SCOPE:
                request.getSession(true).setAttribute(name, value);
            break;
            case APPLICATION_SCOPE:
                servletContext.setAttribute(name, value);
            break;
            default:
                setAttribute(name, value);
        }
    }

    public Object getAttribute(String name) {
        if(name == null) throw new NullPointerException("Attribute name cannot be null");
        if(pageScope.getVariables().containsKey(name))
            return pageScope.getVariable(name);
        return null;
    }

    public Object getAttribute(String name, int scope) {
       if(name == null) throw new NullPointerException("Attribute name cannot be null");
       switch(scope) {
            case PAGE_SCOPE:
                return getAttribute(name);
            case REQUEST_SCOPE:
                return request.getAttribute(name);
            case SESSION_SCOPE:
                return request.getSession(true).getAttribute(name);
            case APPLICATION_SCOPE:
                return servletContext.getAttribute(name);
            default:
                return getAttribute(name);
        }
    }

    public Object findAttribute(String name) {
        if(name == null) throw new NullPointerException("Attribute name cannot be null");
        int scope = getAttributesScope(name);
        if(scope > 0) {
            return getAttribute(name, scope);
        }
        return null;
    }

    public void removeAttribute(String name) {
        if(name == null) throw new NullPointerException("Attribute name cannot be null");
        pageScope.getVariables().remove(name);
    }

    public void removeAttribute(String name, int scope) {
        if(name == null) throw new NullPointerException("Attribute name cannot be null");
        switch(scope) {
            case PAGE_SCOPE:
                removeAttribute(name);
            break;
            case REQUEST_SCOPE:
                request.removeAttribute(name);
            break;
            case SESSION_SCOPE:
                HttpSession session = request.getSession(false);
                if(session!=null)
                    session.removeAttribute(name);
            break;
            case APPLICATION_SCOPE:
                servletContext.removeAttribute(name);
            break;
            default:
                removeAttribute(name);
        }
    }

    public int getAttributesScope(String name) {
        if(name == null) throw new NullPointerException("Attribute name cannot be null");
        if(pageScope.getVariables().containsKey(name))
            return PAGE_SCOPE;
        else if(request.getAttribute(name) != null)
            return REQUEST_SCOPE;
        else {
            HttpSession session = request.getSession(false);
            if(session!=null && session.getAttribute(name)!=null)
                return SESSION_SCOPE;
            else if(servletContext.getAttribute(name) !=null) {
                return APPLICATION_SCOPE;
            }
        }
        return 0;
    }

    public Enumeration getAttributeNamesInScope(int scope) {
        switch(scope) {
            case PAGE_SCOPE:
                  return new IteratorEnumeration(pageScope.getVariables().keySet().iterator());
            case REQUEST_SCOPE:
                return request.getAttributeNames();
            case SESSION_SCOPE:
                HttpSession session = request.getSession(false);
                if(session!=null)return session.getAttributeNames();
                else return EMPTY_ENUMERATION;
            case APPLICATION_SCOPE:
                return servletContext.getAttributeNames();
        }
        return EMPTY_ENUMERATION;
    }

    public JspWriter getOut() {
        Writer out = webRequest.getOut();
        if(out instanceof JspWriter) {
            return (JspWriter)out;
        }
        else {
            out = new JspWriterDelegate(out);
            webRequest.setOut(out);
            return (JspWriter)out;
        }
    }

    public ExpressionEvaluator getExpressionEvaluator() {
        try {
            Class type = ((ClassLoader) AccessController.doPrivileged(
                    new PrivilegedAction() {
                        public Object run() {
                            return Thread.currentThread().getContextClassLoader();
                        }
                    })).loadClass
                    ("org.apache.commons.el.ExpressionEvaluatorImpl");
            return (ExpressionEvaluator) type.newInstance();
        }
        catch (Exception e) {
            throw new UnsupportedOperationException("In order for the getExpressionEvaluator() " +
                "method to work, you must have downloaded the apache commons-el jar and " +
                "made it available in the classpath.");
        }
    }

    public VariableResolver getVariableResolver() {
        final PageContext ctx = this;
        return new VariableResolver() {
            public Object resolveVariable(String name) throws ELException {
                return ctx.findAttribute(name);
            }
        };
    }

}
                                            