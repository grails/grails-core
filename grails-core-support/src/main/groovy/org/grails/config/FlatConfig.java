package org.grails.config;

import grails.config.Config;
import groovy.util.ConfigObject;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Graeme Rocher
 * @since 3.0.10
 *
 * @deprecated Here for backwards compatibility, do not use directly
 */
@Deprecated
public class FlatConfig implements Map<String,Object>  {

    private final Config config;

    public FlatConfig(Config config) {
        this.config = config;
    }

    @Override
    public int size() {
        return config.size();
    }

    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return config.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return config.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        Object o = config.get(key);
        if(o instanceof NavigableMap.NullSafeNavigator) {
            return null;
        }
        else if(o instanceof ConfigObject) {
            return null;
        }
        return o;
    }

    @Override
    public Object put(String key, Object value) {
        return config.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return config.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        config.putAll(m);
    }

    @Override
    public void clear() {
        config.clear();
    }

    @Override
    public Set<String> keySet() {
        return config.keySet();
    }

    @Override
    public Collection<Object> values() {
        return config.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return config.entrySet();
    }
}
