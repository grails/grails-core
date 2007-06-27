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
package org.codehaus.groovy.grails.web.servlet.mvc;


import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.*;

/**
 * An adapter class that takes a regular HttpSession and allows you to access it like a Groovy map
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
public class GrailsHttpSession implements
		HttpSession {

	private final HttpSession adaptee;

	public GrailsHttpSession(HttpSession session) {
		this.adaptee = session;
	}
    
    /* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String name) {
		synchronized (adaptee) {
            return adaptee.getAttribute(name);
        }
    }

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getAttributeNames()
	 */
	public Enumeration getAttributeNames() {
        synchronized (adaptee) {
    		return adaptee.getAttributeNames();
        }
    }

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getCreationTime()
	 */
	public long getCreationTime() {
        synchronized (adaptee) {
    		return adaptee.getCreationTime();
        }
    }

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getId()
	 */
	public String getId() {
        synchronized (adaptee) {
    		return adaptee.getId();
        }
    }

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getLastAccessedTime()
	 */
	public long getLastAccessedTime() {
        synchronized (adaptee) {
    		return adaptee.getLastAccessedTime();
        }
    }

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getMaxInactiveInterval()
	 */
	public int getMaxInactiveInterval() {
        synchronized (adaptee) {
    		return adaptee.getMaxInactiveInterval();
        }
    }

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getServletContext()
	 */
	public ServletContext getServletContext() {
        synchronized (adaptee) {
    		return adaptee.getServletContext();
        }
    }

	/**
	 * @see javax.servlet.http.HttpSession#getSessionContext()
	 * @deprecated
	 */
	public HttpSessionContext getSessionContext() {
        synchronized (adaptee) {
    		return adaptee.getSessionContext();
        }
    }

	/**
	 * @see javax.servlet.http.HttpSession#getValue(java.lang.String)
	 * @deprecated
	 */
	public Object getValue(String name) {
        synchronized (adaptee) {
    		return adaptee.getAttribute(name);
        }
    }

	/**
	 * @see javax.servlet.http.HttpSession#getValueNames()
     * @deprecated
	 */
	public String[] getValueNames() {
        synchronized (adaptee) {
    		return adaptee.getValueNames();
        }
    }

    /**
     * @see javax.servlet.http.HttpSession#putValue(java.lang.String, java.lang.Object)
     * @deprecated
     */
    public void putValue(String name, Object value) {
        synchronized (adaptee) {
            adaptee.setAttribute(name, value);
        }
    }

    /**
     * @see javax.servlet.http.HttpSession#removeValue(java.lang.String)
     * @deprecated
     */
    public void removeValue(String name) {
        synchronized (adaptee) {
            adaptee.removeAttribute(name);
        }
    }

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#invalidate()
	 */
	public void invalidate() {
        synchronized (adaptee) {
    		adaptee.invalidate();
        }
    }

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#isNew()
	 */
	public boolean isNew() {
        synchronized (adaptee) {
    		return adaptee.isNew();
        }
    }

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String name) {
        synchronized (adaptee) {
    		adaptee.removeAttribute(name);
        }
    }

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String, java.lang.Object)
	 */
	public void setAttribute(String name, Object value) {
        synchronized (adaptee) {
    		adaptee.setAttribute(name, value);
        }
    }

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#setMaxInactiveInterval(int)
	 */
	public void setMaxInactiveInterval(int arg0) {
        synchronized (adaptee) {
    		adaptee.setMaxInactiveInterval(arg0);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Session Content:\n");
        Enumeration e = adaptee.getAttributeNames();
        while(e.hasMoreElements()) {
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
