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

import groovy.lang.GroovySystem;
import groovy.lang.MetaBeanProperty;
import groovy.lang.MetaClass;
import groovy.lang.MetaProperty;
import groovy.util.MapEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * A map implementation that reads an objects properties lazily using Groovy's MetaClass.
 *
 * @author Graeme Rocher
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class LazyMetaPropertyMap implements Map {

    private MetaClass metaClass;
    private Object instance;
    private static List<String> EXCLUDES = Arrays.asList("properties");

    /**
     * Constructs the map
     * @param o The object to inspect
     */
    public LazyMetaPropertyMap(Object o) {
        Assert.notNull(o, "Object cannot be null");

        instance = o;
        metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(o.getClass());
    }

    /**
     * {@inheritDoc}
     * @see java.util.Map#size()
     */
    public int size() {
        return keySet().size();
    }

    /**
     * {@inheritDoc}
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return false; // will never be empty
    }

    /**
     * {@inheritDoc}
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object propertyName) {
        if (propertyName instanceof CharSequence) propertyName = propertyName.toString();
        if (!(propertyName instanceof String)) throw new IllegalArgumentException("This map implementation only supports String based keys!");

        if (EXCLUDES.contains(propertyName)) return false;
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
     * Obtains the value of an object's properties on demand using Groovy's MOP.
     *
     * @param propertyName The name of the property
     * @return The property value or null
     */
    public Object get(Object propertyName) {
        if (propertyName instanceof CharSequence) {
            propertyName = propertyName.toString();
        }

        if (propertyName instanceof List) {
            Map submap = new HashMap();
            List propertyNames = (List)propertyName;
            for (Object currentName : propertyNames) {
                if (currentName != null) {
                    currentName = currentName.toString();
                    if (containsKey(currentName)) {
                        submap.put(currentName, get(currentName));
                    }
                }
            }
            return submap;
        }

        if (EXCLUDES.contains(propertyName)) {
            return null;
        }

        Object val = null;
        MetaProperty mp = metaClass.getMetaProperty((String)propertyName);
        if (mp != null) {
            val = mp.getProperty(instance);
        }
        return val;
    }

    public Object put(Object propertyName, Object propertyValue) {
        if (propertyName instanceof CharSequence) {
            propertyName = propertyName.toString();
        }

        Object old = null;
        MetaProperty mp = metaClass.getMetaProperty((String)propertyName);
        if (mp != null) {
            old = mp.getProperty(instance);
            if (propertyValue instanceof Map) {
                propertyValue = ((Map)propertyValue).get(propertyName);
            }
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

    public Set<String> keySet() {
        Set<String> names = new HashSet<String>();
        for (MetaProperty mp : metaClass.getProperties()) {
            if (isExcluded(mp)) continue;
            names.add(mp.getName());
        }
        return names;
    }

    public Collection<Object> values() {
        Collection<Object> values = new ArrayList<Object>();
        for (MetaProperty mp : metaClass.getProperties()) {
            if (isExcluded(mp)) continue;
            values.add(mp.getProperty(instance));
        }
        return values;
    }

    @Override
    public int hashCode() {
        return instance.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LazyMetaPropertyMap) {
            LazyMetaPropertyMap other = (LazyMetaPropertyMap)o;
            return instance.equals(other.getInstance());
        }
        return false;
    }

    /**
     * Returns the wrapped instance.
     *
     * @return The wrapped instance
     */
    public Object getInstance() {
        return instance;
    }

    public Set<MapEntry> entrySet() {
        Set<MapEntry> entries = new HashSet<MapEntry>();
        for (MetaProperty mp : metaClass.getProperties()) {
            if (isExcluded(mp)) continue;

            entries.add(new MapEntry(mp.getName(), mp.getProperty(instance)));
        }
        return entries;
    }

    private boolean isExcluded(MetaProperty mp) {
        if (EXCLUDES.contains(mp.getName())) return true;
        if ((mp instanceof MetaBeanProperty) && (((MetaBeanProperty) mp).getGetter()) == null) return true;
        return false;
    }
}
