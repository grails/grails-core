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

import groovy.lang.GroovyObjectSupport;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * An adapter class that takes a regular HttpSession and allows you to access it like a Groovy map
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
public class GrailsHttpSession implements
		HttpSession, Map {

	private HttpSession adaptee;

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

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getSessionContext()
	 */
	public HttpSessionContext getSessionContext() {
        synchronized (adaptee) {
    		return adaptee.getSessionContext();
        }
    }

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getValue(java.lang.String)
	 */
	public Object getValue(String name) {
        synchronized (adaptee) {
    		return adaptee.getAttribute(name);
        }
    }

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSession#getValueNames()
	 */
	public String[] getValueNames() {
        synchronized (adaptee) {
    		return adaptee.getValueNames();
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
	 * @see javax.servlet.http.HttpSession#putValue(java.lang.String, java.lang.Object)
	 */
	public void putValue(String name, Object value) {
        synchronized (adaptee) {
    		adaptee.setAttribute(name, value);
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
	 * @see javax.servlet.http.HttpSession#removeValue(java.lang.String)
	 */
	public void removeValue(String name) {
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

	/* (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	public void clear() {
        synchronized (adaptee) {
    		adaptee.invalidate();
        }
    }

	/* (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		if(key == null)return false;
        synchronized (adaptee) {
    		return adaptee.getAttribute(key.toString()) != null;
        }
    }

	/* (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
        synchronized (adaptee) {
            for (Enumeration e = getAttributeNames(); e.hasMoreElements();) {
                if(getAttribute(e.nextElement().toString()).equals(value))
                    return true;

            }
        }
        return false;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet() {
		throw new UnsupportedOperationException("Method 'entrySet()' is not support by session Map." );
	}

	/* (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public Object get(Object key) {
		if(key == null)return null;
        synchronized (adaptee) {
    		return getAttribute(key.toString());
        }
    }

	/* (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
        synchronized (adaptee) {
    		return !getAttributeNames().hasMoreElements();
        }
    }

	/* (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	public Set keySet() {
		throw new UnsupportedOperationException("Method 'keySet()' is not support by session Map." );
	}

	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public Object put(Object key, Object value) {
        synchronized (adaptee) {
    		setAttribute(key.toString(), value);
        }
        return value;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map t) {
		if(t!=null) {
            synchronized (adaptee) {
                for (Iterator i = t.keySet().iterator(); i.hasNext();) {
                    Object key = i.next();
                    String name = key.toString();

                    setAttribute(name, t.get(key));

                }
            }
        }
	}

	/* (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Object remove(Object key) {
		if(key == null)return null;
		
        Object obj;
        synchronized (adaptee) {
    		obj = getAttribute(key.toString());
	    	removeAttribute(key.toString());
        }

        return obj;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	public int size() {
		throw new UnsupportedOperationException("Method 'size()' is not support by session Map." );
	}

	/* (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	public Collection values() {
		throw new UnsupportedOperationException("Method 'values()' is not support by session Map." );
	}

}
