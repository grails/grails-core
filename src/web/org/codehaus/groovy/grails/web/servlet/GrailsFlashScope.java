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
package org.codehaus.groovy.grails.web.servlet;

import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * Grails implementation of Flash scope (@see org.codehaus.groovy.grails.web.servlet.FlashScope)
 *
 * @author Graeme Rocher
 * @since 07-Feb-2006
 */
public class GrailsFlashScope implements FlashScope {
    private HashMap current = new HashMap();
    private HashMap next = new HashMap();
    public static final String ERRORS_PREFIX = "org.codehaus.groovy.grails.ERRORS_";
    private static final String ERRORS_PROPERTY = "errors";

    public GrailsFlashScope() {
    }

    public void next() {
        current.clear();
        current = (HashMap)next.clone();
        next.clear();
        reassociateObjectsWithErrors(current);
    }

    private void reassociateObjectsWithErrors(Map scope) {
        for (Iterator i = scope.keySet().iterator(); i.hasNext();) {
            Object key =  i.next();
            Object value = scope.get(key);
            if(value instanceof Map) {
                reassociateObjectsWithErrors((Map)value);
            }
            reasssociateObjectWithErrors(scope, value);

        }
    }

    private void reasssociateObjectWithErrors(Map scope, Object value) {
        if(value instanceof Collection) {
             Collection values = (Collection)value;
            for (Iterator i = values.iterator(); i.hasNext();) {
                Object current = i.next();
                reasssociateObjectWithErrors(scope, current);
            }
        }
        else {
            String errorsKey = ERRORS_PREFIX + System.identityHashCode(value);
            Object errors = scope.get(errorsKey);
            if(value!=null && errors != null) {
                MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(value.getClass());
                if(mc.hasProperty(value, ERRORS_PROPERTY)!=null) {
                    mc.setProperty(value, ERRORS_PROPERTY, errors);
                }
            }
        }
    }

    public int size() {
        return current.size() + next.size();
    }

    public void clear() {
        current.clear();
        next.clear();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean containsKey(Object key) {
        return (current.containsKey(key) || next.containsKey(key));
    }

    public boolean containsValue(Object value) {
        return (current.containsValue(value) || next.containsValue(value));
    }

    public Collection values() {
        Collection c = new ArrayList();
        c.addAll(current.values());
        c.addAll(next.values());
        return c;
    }

    public void putAll(Map t) {
        for (Iterator i = t.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            put(key,t.get(key));
        }
    }

    public Set entrySet() {
        Set keySet = new HashSet();
        keySet.addAll(current.entrySet());
        keySet.addAll(next.entrySet());
        return keySet;
    }

    public Set keySet() {
        Set keySet = new HashSet();
        keySet.addAll(current.keySet());
        keySet.addAll(next.keySet());
        return keySet;
    }

    public Object get(Object key) {
        if(next.containsKey(key))
            return next.get(key);
        return current.get(key);
    }

    public Object remove(Object key) {
        if(current.containsKey(key))
            return current.remove(key);
        else
            return next.remove(key);
    }

    public Object put(Object key, Object value) {
        // create the session if it doesn't exist
        registerWithSessionIfNecessary();
        if(current.containsKey(key)) {
            current.remove(key);
        }
        storeErrorsIfPossible(next,value);

        return next.put(key,value);
    }

    private void storeErrorsIfPossible(Map scope,Object value) {
        if(value != null) {

            if(value instanceof Collection) {
                Collection values = (Collection)value;
                for (Iterator i = values.iterator(); i.hasNext();) {
                    Object current = i.next();
                    storeErrorsIfPossible(scope,current);
                }            
            }
            else if(value instanceof Map) {
                Map map = (Map)value;
                Set keys = map.keySet();
                for (Iterator i = keys.iterator(); i.hasNext();) {
                    Object key = i.next();
                    Object val = map.get(key);
                    storeErrorsIfPossible(map,val);
                }
            }
            else {
                MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(value.getClass());
                if(mc.hasProperty(value, ERRORS_PROPERTY)!=null) {
                    Object errors = mc.getProperty(value, ERRORS_PROPERTY);
                    if(errors != null) {
                        scope.put(ERRORS_PREFIX + System.identityHashCode(value), errors);
                    }
                }
            }

        }
    }

    private void registerWithSessionIfNecessary() {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        HttpSession session = webRequest.getCurrentRequest().getSession(true);
        if(session.getAttribute(GrailsApplicationAttributes.FLASH_SCOPE) == null) session.setAttribute(GrailsApplicationAttributes.FLASH_SCOPE, this);
    }
}
