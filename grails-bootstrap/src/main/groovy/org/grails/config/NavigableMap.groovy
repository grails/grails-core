package org.grails.config

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode

import org.codehaus.groovy.runtime.DefaultGroovyMethods

@EqualsAndHashCode
@CompileStatic
class NavigableMap implements Map<String, Object>, Cloneable {
    final NavigableMap rootConfig
    final List<String> path
    final Map<String, Object> delegateMap

    public NavigableMap() {
        rootConfig = this
        path = []
        delegateMap = new LinkedHashMap<>()
    }
    
    public NavigableMap(NavigableMap rootConfig, List<String> path) {
        super()
        this.rootConfig = rootConfig
        this.path = path
        delegateMap = new LinkedHashMap<>()
    }
    
    public NavigableMap clone() {
        NavigableMap cloned = new NavigableMap()
        cloned.merge(delegateMap)
        return cloned
    }

    @Override
    int size() {
        delegateMap.size()
    }

    @Override
    boolean isEmpty() {
        delegateMap.isEmpty()
    }

    @Override
    boolean containsKey(Object key) {
        delegateMap.containsKey key
    }

    @Override
    boolean containsValue(Object value) {
        delegateMap.containsValue value
    }

    @Override
    Object get(Object key) {
        delegateMap.get(key)
    }

    @Override
    Object put(String key, Object value) {
        delegateMap.put(key, value)
    }

    @Override
    Object remove(Object key) {
        delegateMap.remove key
    }

    @Override
    void putAll(Map<? extends String, ?> m) {
        delegateMap.putAll m
    }

    @Override
    void clear() {
        delegateMap.clear()
    }

    @Override
    Set<String> keySet() {
        delegateMap.keySet()
    }

    @Override
    Collection<Object> values() {
        delegateMap.values()
    }

    @Override
    Set<Map.Entry<String, Object>> entrySet() {
        delegateMap.entrySet()
    }

    public void merge(Map sourceMap, boolean parseFlatKeys=false) {
        mergeMaps(this, sourceMap, parseFlatKeys)
    }
    
    private static void mergeMaps(NavigableMap targetMap, Map sourceMap, boolean parseFlatKeys) {
        sourceMap.each { Object sourceKeyObject, Object sourceValue ->
            String sourceKey = String.valueOf(sourceKeyObject)
            NavigableMap actualTarget
            if(parseFlatKeys) {
                String[] keyParts = sourceKey.split(/\./)
                if(keyParts.length > 1) {
                    actualTarget = targetMap.navigateSubMap(keyParts[0..-2] as List, true)
                    sourceKey = keyParts[-1]
                } else {
                    actualTarget = targetMap
                }
            } else {
                actualTarget = targetMap
            }
            mergeMapEntry(actualTarget, sourceKey, sourceValue, parseFlatKeys)
        }
    }
    
    private static void mergeMapEntry(NavigableMap targetMap, String sourceKey, Object sourceValue, boolean parseFlatKeys) {
        Object currentValue = targetMap.containsKey(sourceKey) ? targetMap.get(sourceKey) : null
        Object newValue
        if(sourceValue instanceof Map) {
            NavigableMap subMap = new NavigableMap( (NavigableMap)targetMap.rootConfig, ((targetMap.path + [sourceKey]) as List<String>).asImmutable())
            if(currentValue instanceof Map) {
                subMap.putAll((Map)currentValue)
            }
            mergeMaps(subMap, (Map)sourceValue, parseFlatKeys)
            newValue = subMap
        } else {
            newValue = sourceValue
        }
        if (newValue == null) {
            targetMap.remove(sourceKey)
        } else {
            targetMap.put(sourceKey, newValue)
        }
    }
    
    public Object getAt(Object key) {
        getProperty(String.valueOf(key))
    }
    
    public void setAt(Object key, Object value) {
        setProperty(String.valueOf(key), value)
    }
    
    public Object getProperty(String name) {
        if (!containsKey(name)) {
            return new NullSafeNavigator(this, [name].asImmutable())
        }
        return get(name)
    }
    
    public void setProperty(String name, Object value) {
        mergeMapEntry(this, name, value, false)
    }
    
    public Object navigate(String... path) {
        return navigateMap(this, path)
    }
    
    private Object navigateMap(Map<String, Object> map, String... path) {
        if(map==null || path == null) return null
        if(path.length == 0) {
            return map
        } else if (path.length == 1) {
            return map.get(path[0])
        } else {
            def submap = map.get(path[0])
            if(submap instanceof Map) {
                return navigateMap((Map<String, Object>) submap, path.tail())
            }
            return submap
        }
    }
    
    public NavigableMap navigateSubMap(List<String> path, boolean createMissing) {
        NavigableMap currentMap = this
        for(String pathElement : path) {
            Object currentItem = currentMap.get(pathElement) 
            if(currentItem instanceof NavigableMap) {
                currentMap = (NavigableMap)currentItem
            } else if (createMissing) {
                Map<String, Object> newMap = new NavigableMap( (NavigableMap)currentMap.rootConfig, ((currentMap.path + [pathElement]) as List<String>).asImmutable())
                currentMap.put(pathElement, newMap)
                currentMap = newMap
            } else {
                return null
            }
        }
        currentMap
    }
    
    public Map<String, Object> toFlatConfig() {
        Map<String,Object> flatConfig = [:]
        flattenKeys(flatConfig, this, [], false)
        flatConfig
    }
    
    public Properties toProperties() {
        Properties properties = new Properties()
        flattenKeys((Map<String, Object>)properties, this, [], true)
        properties
    }
    
    private void flattenKeys(Map<String, Object> flatConfig, Map currentMap, List<String> path, boolean forceStrings) {
        currentMap.each { key, value ->
            String stringKey = String.valueOf(key)
            if(value != null) {
                if(value instanceof Map) {
                    flattenKeys(flatConfig, (Map)value, ((path + [stringKey]) as List<String>).asImmutable(), forceStrings)
                } else {
                    String fullKey
                    if(path) {
                        fullKey = path.join('.') + '.' + stringKey
                    } else {
                        fullKey = stringKey
                    }
                    if(value instanceof Collection) {
                        if(forceStrings) {
                            flatConfig.put(fullKey, ((Collection)value).join(","))
                        } else {
                            flatConfig.put(fullKey, value)
                        }
                        int index = 0
                        for(Object item: (Collection)value) {
                            String collectionKey = "${fullKey}[${index}]".toString()
                            flatConfig.put(collectionKey, forceStrings ? String.valueOf(item) : item)
                            index++
                        }
                    } else {
                        flatConfig.put(fullKey, forceStrings ? String.valueOf(value) : value)
                    }
                }
            }
        }        
    }
    
    @CompileStatic
    static class NullSafeNavigator implements Map<String, Object>{
        final NavigableMap parent
        final List<String> path
        
        NullSafeNavigator(NavigableMap parent, List<String> path) {
            this.parent = parent
            this.path = path
        }
        
        Object getAt(Object key) {
            getProperty(String.valueOf(key))
        }
        
        void setAt(Object key, Object value) {
            setProperty(String.valueOf(key), value)
        }

        @Override
        int size() {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if(parentMap != null) {
                return parentMap.size()
            }
            return 0
        }

        @Override
        boolean isEmpty() {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if(parentMap != null) {
                return parentMap.isEmpty()
            }
            return true
        }

        boolean containsKey(Object key) {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if(parentMap == null) return false
            else {
                return parentMap.containsKey(key)
            }
        }

        @Override
        boolean containsValue(Object value) {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if(parentMap != null) {
                return parentMap.containsValue(value)
            }
            return false
        }

        @Override
        Object get(Object key) {
            return getAt(key)
        }

        @Override
        Object put(String key, Object value) {
            throw new UnsupportedOperationException("Configuration cannot be modified");
        }

        @Override
        Object remove(Object key) {
            throw new UnsupportedOperationException("Configuration cannot be modified");
        }

        @Override
        void putAll(Map<? extends String, ?> m) {
            throw new UnsupportedOperationException("Configuration cannot be modified");
        }

        @Override
        void clear() {
            throw new UnsupportedOperationException("Configuration cannot be modified");
        }

        @Override
        Set<String> keySet() {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if(parentMap != null) {
                return parentMap.keySet()
            }
            return Collections.emptySet()
        }

        @Override
        Collection<Object> values() {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if(parentMap != null) {
                return parentMap.values()
            }
            return Collections.emptySet()
        }

        @Override
        Set<Map.Entry<String, Object>> entrySet() {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if(parentMap != null) {
                return parentMap.entrySet()
            }
            return Collections.emptySet()
        }

        Object getProperty(String name) {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if(parentMap == null) {
                return new NullSafeNavigator(parent, ((path + [name]) as List<String>).asImmutable())
            } else {
                return parentMap.get(name)
            }
        }
        
        public void setProperty(String name, Object value) {
            NavigableMap parentMap = parent.navigateSubMap(path, true)
            parentMap.put(name, value)
        }
        
        public boolean asBoolean() {
            false
        }
        
        public Object invokeMethod(String name, Object args) {
            throw new NullPointerException("Cannot invoke method " + name + "() on NullSafeNavigator");
        }
    
        public boolean equals(Object to) {
            return to == null || DefaultGroovyMethods.is(this, to)
        }
    
        public Iterator iterator() {
            return Collections.EMPTY_LIST.iterator()
        }
    
        public Object plus(String s) {
            return toString() + s
        }
    
        public Object plus(Object o) {
            throw new NullPointerException("Cannot invoke method plus on NullSafeNavigator")
        }
    
        public boolean is(Object other) {
            return other == null || DefaultGroovyMethods.is(this, other)
        }
    
        public Object asType(Class c) {
            if(c==Boolean || c==boolean) return false
            return null
        }
    
        public String toString() {
            return null
        }
    
//        public int hashCode() {
//            throw new NullPointerException("Cannot invoke method hashCode() on NullSafeNavigator");
//        }
    }
}