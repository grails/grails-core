/*
 * Copyright 2004-2005 the original author or authors.
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
package grails.web.servlet.mvc;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * An adapter class that takes a regular HttpSession and allows you to access it like a Groovy map.
 *
 * @author Graeme Rocher
 * @since 3.0
 */
public class GrailsHttpSession implements HttpSession {

    private HttpSession adaptee;
    private HttpServletRequest request;

    public GrailsHttpSession(HttpServletRequest request) {
        this.request = request;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name) {
        createSessionIfNecessary();
        synchronized (this) {
            return adaptee.getAttribute(name);
        }
    }

    private void createSessionIfNecessary() {
        if (adaptee == null) adaptee = request.getSession(true);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getAttributeNames()
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Enumeration getAttributeNames() {
        createSessionIfNecessary();
        synchronized (this) {
            return adaptee.getAttributeNames();
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getCreationTime()
     */
    public long getCreationTime() {
        createSessionIfNecessary();
        synchronized (this) {
            return adaptee.getCreationTime();
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getId()
     */
    public String getId() {
        createSessionIfNecessary();
        synchronized (this) {
            return adaptee.getId();
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getLastAccessedTime()
     */
    public long getLastAccessedTime() {
        createSessionIfNecessary();
        synchronized (this) {
            return adaptee.getLastAccessedTime();
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getMaxInactiveInterval()
     */
    public int getMaxInactiveInterval() {
        createSessionIfNecessary();
        synchronized (this) {
            return adaptee.getMaxInactiveInterval();
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#getServletContext()
     */
    public ServletContext getServletContext() {
        createSessionIfNecessary();
        synchronized (this) {
            return adaptee.getServletContext();
        }
    }

    /**
     * @see javax.servlet.http.HttpSession#getSessionContext()
     * @deprecated
     */
    @Deprecated
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        createSessionIfNecessary();
        synchronized (this) {
            return adaptee.getSessionContext();
        }
    }

    /**
     * @see javax.servlet.http.HttpSession#getValue(java.lang.String)
     * @deprecated
     */
    @Deprecated
    public Object getValue(String name) {
        createSessionIfNecessary();
        synchronized (this) {
            return adaptee.getAttribute(name);
        }
    }

    /**
     * @see javax.servlet.http.HttpSession#getValueNames()
     * @deprecated
     */
    @Deprecated
    public String[] getValueNames() {
        createSessionIfNecessary();
        synchronized (this) {
            return adaptee.getValueNames();
        }
    }

    /**
     * @see javax.servlet.http.HttpSession#putValue(java.lang.String, java.lang.Object)
     * @deprecated
     */
    @Deprecated
    public void putValue(String name, Object value) {
        createSessionIfNecessary();
        synchronized (this) {
            adaptee.setAttribute(name, value);
        }
    }

    /**
     * @see javax.servlet.http.HttpSession#removeValue(java.lang.String)
     * @deprecated
     */
    @Deprecated
    public void removeValue(String name) {
        createSessionIfNecessary();
        synchronized (this) {
            adaptee.removeAttribute(name);
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#invalidate()
     */
    public void invalidate() {
        createSessionIfNecessary();
        synchronized (this) {
            adaptee.invalidate();
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#isNew()
     */
    public boolean isNew() {
        createSessionIfNecessary();
        synchronized (this) {
            return adaptee.isNew();
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name) {
        createSessionIfNecessary();
        synchronized (this) {
            adaptee.removeAttribute(name);
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String name, Object value) {
        createSessionIfNecessary();
        synchronized (this) {
            adaptee.setAttribute(name, value);
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSession#setMaxInactiveInterval(int)
     */
    public void setMaxInactiveInterval(int arg0) {
        createSessionIfNecessary();
        synchronized (this) {
            adaptee.setMaxInactiveInterval(arg0);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public String toString() {
        createSessionIfNecessary();
        StringBuilder sb = new StringBuilder("Session Content:\n");
        Enumeration e = adaptee.getAttributeNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            sb.append("  ");
            sb.append(name);
            sb.append(" = ");
            sb.append(adaptee.getAttribute(name));
            sb.append('\n');
        }
        return sb.toString();
    }
}
