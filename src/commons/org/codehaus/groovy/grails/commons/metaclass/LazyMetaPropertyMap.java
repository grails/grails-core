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
package org.codehaus.groovy.grails.commons.metaclass;

import groovy.lang.GString;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaProperty;
import groovy.util.MapEntry;

import java.util.*;

/**
 * A map implementation that reads an objects properties lazily using Groovy's MetaClass
 *
 * @author Graeme Rocher
 */
public class LazyMetaPropertyMap implements Map {
    private MetaClass metaClass;
    private Object instance;
    private static List EXCLUDES = new ArrayList() {{
        add("properties");
    }};

    /**
     * Constructs the map
     * @param o The object to inspect
     */
    public LazyMetaPropertyMap(Object o) {
        super();
        if(o == null) throw new IllegalArgumentException("Object cannot be null");

        this.instance = o;
        this.metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(o.getClass());
    }

    /**
     * @see java.util.Map#size()
     */
    public int size() {
        return keySet().size();
    }
    /**
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return false; // will never be empty
    }

    /**
     * @see java.util.Map#containsKey(Object)
     */
    public boolean containsKey(Object propertyName) {
        if(propertyName instanceof GString) propertyName = propertyName.toString();
        if(!(propertyName instanceof String)) throw new IllegalArgumentException("This map implementation only supports String based keys!");

        if(EXCLUDES.contains(propertyName)) return false;
        return metaClass.getMetaProperty((String)propertyName) != null;
    }

    /**
     * Checks whether the specified value is contained within the Map. Note that because this implementation
     * lazily initialises property values the behaviour may not be consistent with the actual values of the
     * contained object unless they have already been initialised by calling get(Object)
     *
     * @see java.util.Map#containsValue(Object)
     */
    public boolean containsValue(Object o) {
        return values().contains(o);
    }

    /**
     * Obtains the value of an object's properties on demand using Groovy's MOP
     *
     * @param propertyName The name of the property
     * @return The property value or null
     */
    public Object get(Object propertyName) {
        if(propertyName instanceof GString) propertyName = propertyName.toString();
        if(!(propertyName instanceof String)) throw new IllegalArgumentException("This map implementation only supports String based keys!");
        if(EXCLUDES.contains(propertyName)) return null;
        Object val = null;
        MetaProperty mp = metaClass.getMetaProperty((String)propertyName);
        if(mp != null) {
            val = mp.getProperty(this.instance);            
        }
        return val;
    }

    public Object put(Object propertyName, Object propertyValue) {
        if(propertyName instanceof GString) propertyName = propertyName.toString();
        if(!(propertyName instanceof String)) throw new IllegalArgumentException("This map implementation only supports String based keys!");

        Object old = null;
        MetaProperty mp = metaClass.getMetaProperty((String)propertyName);
        if(mp!=null) {
            old = mp.getProperty(instance);
            mp.setProperty(instance, propertyValue);
        }
        return old;
    }

    /**
     * @throws UnsupportedOperationException
     */
    public Object remove(Object o) {
        throw new UnsupportedOperationException("Method remove(Object o) is not supported by this implementation");
    }

    public void putAll(Map map) {
        for (Iterator i = map.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            put(key, map.get(i));
        }
    }

    /**
     * @throws UnsupportedOperationException
     */
    public void clear() {
        throw new UnsupportedOperationException("Method clear() is not supported by this implementation");
    }

    public Set keySet() {
        Collection properties = metaClass.getProperties();
        Set names = new HashSet();
        for (Iterator i = properties.iterator(); i.hasNext();) {
            MetaProperty mp = (MetaProperty) i.next();
            if(EXCLUDES.contains(mp.getName()))continue;
            names.add(mp.getName());
        }
        return names;
    }

    public Collection values() {
        Collection properties = metaClass.getProperties();
        Collection values = new ArrayList();
        for (Iterator i = properties.iterator(); i.hasNext();) {
            MetaProperty mp = (MetaProperty) i.next();
            if(EXCLUDES.contains(mp.getName()))continue;
            values.add(mp.getProperty(instance));
        }
        return values;
    }

    public int hashCode() {
        return instance.hashCode();
    }

    public boolean equals(Object o) {
        if(o instanceof LazyMetaPropertyMap) {
            LazyMetaPropertyMap other = (LazyMetaPropertyMap)o;
            return this.instance.equals(other.getInstance());

        }
        return false;
    }

    /**
     * Returns the wrapped instance
     *
     * @return The wrapped instance
     */
    public Object getInstance() {
        return instance;
    }

    public Set entrySet() {

        Collection properties = metaClass.getProperties();
        Set entries = new HashSet();
        for (Iterator i = properties.iterator(); i.hasNext();) {
            MetaProperty mp = (MetaProperty) i.next();
            if(EXCLUDES.contains(mp.getName()))continue;
            entries.add(new MapEntry(mp.getName(), mp.getProperty(instance)));
        }
        return entries;  
    }
}
