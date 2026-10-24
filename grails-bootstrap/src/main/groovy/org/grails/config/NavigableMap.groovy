/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.config

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.DefaultGroovyMethods

/**
 * @deprecated This class is deprecated to reduce complexity, improve performance, and increase maintainability. Use {@code config.getProperty(String key, Class<T> targetType)} instead.
 */
@Deprecated
@EqualsAndHashCode
@CompileStatic
class NavigableMap implements Map<String, Object>, Cloneable {

    final NavigableMap rootConfig
    final List<String> path
    final Map<String, Object> delegateMap
    final String dottedPath

    NavigableMap() {
        rootConfig = this
        path = []
        dottedPath = ''
        delegateMap = new LinkedHashMap<>()
    }

    NavigableMap(NavigableMap rootConfig, List<String> path) {
        super()
        this.rootConfig = rootConfig
        this.path = path
        dottedPath = path.join('.')
        delegateMap = new LinkedHashMap<>()
    }

    @Override
    String toString() {
        delegateMap.toString()
    }

    /**
     * A copy whose nested maps and lists are copies as well. The copy of a root is the root of its own
     * configuration, so a write through the copy, by merge, dot notation or subscript, never reaches the
     * original; the copy of a nested map stays under the root of the original. A map or list the original
     * holds twice, as the nested entry and the flat alias, is copied once, so the copy keeps both
     * representations on one object.
     */
    @Override
    NavigableMap clone() {
        Map<Object, Object> copies = new IdentityHashMap<>()
        NavigableMap copy = getRootConfig().is(this) ? new NavigableMap() : new NavigableMap(getRootConfig(), getPath())
        copies.put(this, copy)
        copyEntries(copy, copies)
        copy
    }

    private NavigableMap cloneUnder(NavigableMap root, Map<Object, Object> copies) {
        NavigableMap copy = (NavigableMap) copies.get(this)
        if (copy == null) {
            copy = new NavigableMap(root, getPath())
            copies.put(this, copy)
            copyEntries(copy, copies)
        }
        copy
    }

    private void copyEntries(NavigableMap copy, Map<Object, Object> copies) {
        NavigableMap root = copy.getRootConfig()
        for (Map.Entry<String, Object> entry in getDelegateMap().entrySet()) {
            copy.getDelegateMap().put(entry.key, copyValue(entry.value, root, copies))
        }
    }

    private static Object copyValue(Object value, NavigableMap root, Map<Object, Object> copies) {
        if (value instanceof NavigableMap) {
            return ((NavigableMap) value).cloneUnder(root, copies)
        }
        if (!(value instanceof List) && !(value instanceof Map)) {
            return value
        }
        Object copy = copies.get(value)
        if (copy == null) {
            if (value instanceof List) {
                List<Object> listCopy = []
                copies.put(value, listCopy)
                for (Object element in (List) value) {
                    listCopy.add(copyValue(element, root, copies))
                }
                copy = listCopy
            } else {
                Map<Object, Object> mapCopy = [:]
                copies.put(value, mapCopy)
                for (Map.Entry element in ((Map) value).entrySet()) {
                    mapCopy.put(element.key, copyValue(element.value, root, copies))
                }
                copy = mapCopy
            }
        }
        copy
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
        delegateMap.containsKey(key)
    }

    @Override
    boolean containsValue(Object value) {
        delegateMap.containsValue(value)
    }

    @Override
    Object get(Object key) {
        Object result = delegateMap.get(key)
        if (result != null) {
            return result
        }
        null
    }

    @Override
    Object put(String key, Object value) {
        delegateMap.put(key, value)
    }

    @Override
    Object remove(Object key) {
        delegateMap.remove(key)
    }

    @Override
    void putAll(Map<? extends String, ? extends Object> m) {
        delegateMap.putAll(m)
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

    void merge(Map sourceMap, boolean parseFlatKeys = false) {
        mergeMaps(this, '', this, sourceMap, parseFlatKeys)
    }

    private void mergeMaps(NavigableMap rootMap,
                           String path,
                           NavigableMap targetMap,
                           Map sourceMap,
                           boolean parseFlatKeys) {

        for (Entry entry in sourceMap) {
            Object sourceKeyObject = entry.key
            Object sourceValue = entry.value
            String sourceKey = String.valueOf(sourceKeyObject)
            if (parseFlatKeys && subscriptStart(sourceKey) < 0) {
                // an indexed key such as a.rows[0].name is merged whole: the dots after the subscript name an
                // entry of the element, and those before it are the path of the container
                String[] keyParts = sourceKey.split(/\./)
                if (keyParts.length > 1) {
                    if (!path) {
                        // at the root the dotted key is the flat key itself; below the root it would be a copy
                        // inside the sub-map that nothing keeps current, and the nested write below writes the
                        // flat key under its full path
                        mergeMapEntry(rootMap, path, targetMap, sourceKey, sourceValue, parseFlatKeys)
                    }
                    def pathParts = keyParts[0..-2]
                    Map actualTarget = targetMap.navigateSubMap(pathParts as List, true)
                    sourceKey = keyParts[-1]
                    String nestedPath = path ? "${path}.${pathParts.join('.')}" : pathParts.join('.')
                    mergeMapEntry(rootMap, nestedPath, actualTarget, sourceKey, sourceValue, parseFlatKeys)
                } else {
                    mergeMapEntry(rootMap, path, targetMap, sourceKey, sourceValue, parseFlatKeys)
                }
            } else {
                mergeMapEntry(rootMap, path, targetMap, sourceKey, sourceValue, parseFlatKeys)
            }
        }
    }

    protected void mergeMapEntry(NavigableMap rootMap, String path, NavigableMap targetMap, String sourceKey, Object sourceValue, boolean parseFlatKeys, boolean isNestedSet = false) {
        int subscriptStart = subscriptStart(sourceKey)
        if (subscriptStart > -1) {
            mergeIndexedEntry(rootMap, path, targetMap, sourceKey.substring(0, subscriptStart), sourceKey.substring(subscriptStart),
                    sourceValue, parseFlatKeys)
        } else {
            Object currentValue = targetMap.containsKey(sourceKey) ? targetMap.get(sourceKey) : null
            Object newValue
            if (sourceValue instanceof Map) {
                List<String> newPathList = []
                newPathList.addAll(targetMap.getPath())
                newPathList.add(sourceKey)
                NavigableMap subMap
                if (currentValue instanceof NavigableMap) {
                    subMap = (NavigableMap) currentValue
                }
                else {
                    subMap = new NavigableMap(targetMap.getRootConfig(), newPathList.asImmutable())
                    if (currentValue instanceof Map) {
                        subMap.putAll((Map) currentValue)
                    }
                }
                String newPath = path ? "${path}.${sourceKey}" : sourceKey
                mergeMaps(rootMap, newPath , subMap, (Map) sourceValue, parseFlatKeys)
                newValue = subMap
            } else {
                newValue = mergedValue(sourceValue)
            }
            if (isNestedSet && newValue == null) {
                // the flat key of the removed property, those below it and its indexed aliases go with it;
                // the flat keys of its siblings stay
                String removedKey = path ? "${path}.${sourceKey}".toString() : sourceKey
                List<String> keysToRemove = rootMap.keySet().findAll { String key ->
                    key == removedKey || key.startsWith(removedKey + '.') || key.startsWith(removedKey + '[')
                } as List<String>
                for (String key in keysToRemove) {
                    rootMap.remove(key)
                }
                targetMap.remove(sourceKey)
            } else {
                String flatKey = path ? "${path}.${sourceKey}".toString() : sourceKey
                if (path) {
                    mergeMapEntry(rootMap, flatKey, newValue)
                }
                mergeMapEntry(targetMap, sourceKey, newValue)
                if (newValue instanceof List) {
                    writeElementKeys(rootMap, flatKey, (List) newValue)
                }
            }
        }
    }

    /**
     * Writes the flat keys of the elements of a list merged as a value, {@code a.rows[0]} and, for an element
     * that is a map, {@code a.rows[0].name}, as the indexed keys of a configuration file write them, so that a
     * list is read alike however it was written. The element keys of the list the value replaces go first: they
     * name elements the new list does not hold.
     */
    private void writeElementKeys(NavigableMap rootMap, String flatKey, List list) {
        List<String> staleKeys = rootMap.keySet().findAll { String key -> key.startsWith(flatKey + '[') } as List<String>
        for (String key in staleKeys) {
            rootMap.remove(key)
        }
        writeElements(rootMap, flatKey, list)
    }

    private void writeElements(NavigableMap rootMap, String flatKey, Object value) {
        if (value instanceof List) {
            int index = 0
            for (Object element in (List) value) {
                String elementKey = "${flatKey}[${index++}]".toString()
                mergeMapEntry(rootMap, elementKey, element)
                writeElements(rootMap, elementKey, element)
            }
        } else if (value instanceof Map && !(value instanceof NavigableMap)) {
            for (Entry entry in ((Map) value).entrySet()) {
                String entryKey = "${flatKey}.${entry.key}".toString()
                mergeMapEntry(rootMap, entryKey, entry.value)
                writeElements(rootMap, entryKey, entry.value)
            }
        }
    }

    /** The index of the first subscript of a key such as {@code rows[0].name}, or -1 when the key has none. */
    private static int subscriptStart(String key) {
        int start = key.indexOf('[')
        start > -1 && key.indexOf(']', start) > start ? start : -1
    }

    /**
     * Merges a key with a subscript, such as {@code rows[0].name}, {@code groups[0][1]} or {@code mappings[/api/**]}.
     * The container named before the first subscript, a list for a numeric index and a map otherwise, is held by
     * the nested map that owns it and, under its flat key, by the root, as one object in both places. The root
     * also holds a flat key for each element the subscripts step through and for the value itself, so that
     * {@code a.rows}, {@code a.rows[0]} and {@code a.rows[0].name} are each read without a walk. The dots of the
     * container's name are a path only when the merge parses flat keys, as for any other key.
     */
    private void mergeIndexedEntry(NavigableMap rootMap, String path, NavigableMap targetMap, String containerName,
                                   String subscripts, Object sourceValue, boolean parseFlatKeys) {
        NavigableMap containerParent = targetMap
        String containerKey = containerName
        if (parseFlatKeys && containerName.contains('.')) {
            List<String> nameParts = containerName.split(/\./) as List<String>
            containerParent = targetMap.navigateSubMap(nameParts[0..-2], true)
            containerKey = nameParts[-1]
        }
        String containerPath = path ? "${path}.${containerName}".toString() : containerName
        int subscriptEnd = subscripts.indexOf(']')
        Object value = mergedValue(sourceValue)
        Object container = setSubscriptValue(containerParent.get(containerKey), subscripts.substring(1, subscriptEnd),
                subscripts.substring(subscriptEnd + 1), value)
        mergeMapEntry(containerParent, containerKey, container)
        if (!containerParent.is(rootMap)) {
            mergeMapEntry(rootMap, containerPath, container)
        }
        Object element = container
        String elementPath = containerPath
        String remaining = subscripts
        while (remaining.startsWith('[') && remaining.indexOf(']') > 0) {
            int end = remaining.indexOf(']')
            String index = remaining.substring(1, end)
            element = index.isNumber() ? ((List) element).get(index.toInteger()) : ((Map) element).get(index)
            elementPath += remaining.substring(0, end + 1)
            remaining = remaining.substring(end + 1)
            mergeMapEntry(rootMap, elementPath, element)
        }
        if (remaining) {
            mergeMapEntry(rootMap, elementPath + remaining, value)
        }
    }

    /**
     * The value a merge stores for an entry that is not a map, in the nested entry and in the flat key
     * alike. A config resolves placeholders here, so that a value such as {@code ${random.uuid}} is
     * resolved once and both representations hold the same result.
     */
    protected Object mergedValue(Object value) {
        value
    }

    protected Object mergeMapEntry(NavigableMap targetMap, String sourceKey, newValue) {
        targetMap.put(sourceKey, newValue)
    }

    Object getAt(Object key) {
        getProperty(String.valueOf(key))
    }

    void setAt(Object key, Object value) {
        setProperty(String.valueOf(key), value)
    }

    /**
     * Groovy compiles {@code map[key] = value} to {@code putAt}, so without this the subscript form wrote
     * the raw entry only; it writes the flat key and the nested entry, as {@link #setProperty} does.
     */
    void putAt(String key, Object value) {
        setProperty(key, value)
    }

    Object getProperty(String name) {
        if (!containsKey(name)) {
            return new NullSafeNavigator(this, [name].asImmutable())
        }
        Object result = get(name)
        return result
    }

    void setProperty(String name, Object value) {
        mergeMapEntry(rootConfig, dottedPath, this, name, value, false, true)
    }

    Object navigate(String... path) {
        return navigateMap(this, path)
    }

    private Object navigateMap(Map<String, Object> map, String... path) {
        if (map == null || path == null) return null
        if (path.length == 0) {
            return map
        } else if (path.length == 1) {
            return map.get(path[0])
        } else {
            def submap = map.get(path[0])
            if (submap instanceof Map) {
                return navigateMap((Map<String, Object>) submap, path.tail())
            }
            return null
        }
    }

    /**
     * Sets a value in a list (numeric index) or map (other index) at the given subscript, creating
     * the container when the current value is not one. The text following the subscript is either
     * empty, further subscripts such as {@code [1]} for a list nested in a list, or a {@code .name}
     * remainder stored as a key of a map element. A container that already holds the object is left
     * as it is: the element keys of a list replay what it holds when the map is merged again, as a
     * config merges the map of a property source. A container that refuses the write, such as a list
     * {@code application.groovy} built with {@code asImmutable()}, is replaced by a copy that holds it.
     */
    private Object setSubscriptValue(Object currentValue, String index, String rest, Object sourceValue) {
        if (index.isNumber()) {
            List list = currentValue instanceof List ? (List) currentValue : []
            int i = index.toInteger()
            boolean held = i < list.size()
            Object element = held ? list.get(i) : null
            Object newElement = subscriptElementValue(element, rest, sourceValue)
            if (held && element.is(newElement)) {
                return list
            }
            return writeElement(list, i, newElement)
        }
        Map map = currentValue instanceof Map ? (Map) currentValue : [:]
        return writeEntry(map, index, subscriptElementValue(map.get(index), rest, sourceValue))
    }

    private Object subscriptElementValue(Object currentValue, String rest, Object sourceValue) {
        if (!rest) {
            return sourceValue
        }
        if (rest.startsWith('[')) {
            int end = rest.indexOf(']')
            if (end > 0) {
                return setSubscriptValue(currentValue, rest[1..<end], rest.substring(end + 1), sourceValue)
            }
        }
        Map element = currentValue instanceof Map ? (Map) currentValue : [:]
        return writeEntry(element, rest.substring(1), sourceValue)
    }

    /**
     * Writes an element into a list and returns the list that holds it: the one given, or a copy when
     * the list refuses the write, as one built with {@code asImmutable()} or {@code List.of} does.
     */
    private static List writeElement(List list, int index, Object element) {
        try {
            fill(list, index, null)
            list.set(index, element)
            return list
        } catch (UnsupportedOperationException ignored) {
            List copy = new ArrayList(list)
            fill(copy, index, null)
            copy.set(index, element)
            return copy
        }
    }

    /**
     * Writes an entry into a map and returns the map that holds it: the one given, left as it is when it
     * already holds that object, or a copy when the map refuses the write, as an immutable map does.
     */
    private static Map writeEntry(Map map, String key, Object value) {
        if (map.containsKey(key) && map.get(key).is(value)) {
            return map
        }
        try {
            map.put(key, value)
            return map
        } catch (UnsupportedOperationException ignored) {
            Map copy = new LinkedHashMap(map)
            copy.put(key, value)
            return copy
        }
    }

    private static void fill(List list, Integer toIndex, Object value) {
        if (toIndex >= list.size()) {
            for (int i = list.size(); i <= toIndex; i++) {
                list.add(i, value)
            }
        }
    }

    NavigableMap navigateSubMap(List<String> path, boolean createMissing) {
        NavigableMap currentMap = this
        for (String pathElement : path) {
            Object currentItem = currentMap.get(pathElement)
            if (currentItem instanceof NavigableMap) {
                currentMap = (NavigableMap) currentItem
            } else if (createMissing) {
                List<String> newPathList = []
                newPathList.addAll(currentMap.getPath())
                newPathList.add(pathElement)

                NavigableMap root = currentMap.getRootConfig()
                NavigableMap newMap = new NavigableMap(root, newPathList.asImmutable())
                currentMap.put(pathElement, newMap)

                // the flat alias of a new block lives at the configuration root under the block's full path,
                // whichever map the walk started on; what an earlier merge left there, a scalar or the block
                // a scalar has since replaced, is stale and is replaced, as a block merged as a map value is
                if (!currentMap.is(root)) {
                    mergeMapEntry(root, newMap.dottedPath, newMap)
                }
                currentMap = newMap
            } else {
                return null
            }
        }
        currentMap
    }

    Map<String, Object> toFlatConfig() {
        Map<String, Object> flatConfig = [:]
        flattenKeys(flatConfig, this, [], false)
        flatConfig
    }

    Properties toProperties() {
        Properties properties = new Properties()
        flattenKeys((Map<Object, Object>) properties, this, [], true)
        properties
    }

    private void flattenKeys(Map<? extends Object, Object> flatConfig, Map currentMap, List<String> path, boolean forceStrings) {
        currentMap.each { key, value ->
            String stringKey = String.valueOf(key)
            if (value != null) {
                if (value instanceof Map) {
                    List<String> newPathList = []
                    newPathList.addAll(path)
                    newPathList.add(stringKey)

                    flattenKeys(flatConfig, (Map) value, newPathList.asImmutable(), forceStrings)
                } else {
                    String fullKey
                    if (path) {
                        fullKey = path.join('.') + '.' + stringKey
                    } else {
                        fullKey = stringKey
                    }
                    if (value instanceof Collection) {
                        if (forceStrings) {
                            ((Map<Object, Object>) flatConfig).put(fullKey, ((Collection) value).join(','))
                        } else {
                            ((Map<Object, Object>) flatConfig).put(fullKey, value)
                        }
                        int index = 0
                        for (Object item: (Collection) value) {
                            String collectionKey = "${fullKey}[${index}]".toString()
                            ((Map<Object, Object>) flatConfig).put(collectionKey, forceStrings ? String.valueOf(item) : item)
                            index++
                        }
                    } else {
                        ((Map<Object, Object>) flatConfig).put(fullKey, forceStrings ? String.valueOf(value) : value)
                    }
                }
            }
        }
    }

    @Override
    int hashCode() {
        return delegateMap.hashCode()
    }

    @Override
    boolean equals(Object obj) {
        return delegateMap.equals(obj)
    }

    /**
     * @deprecated This class should be avoided due to known performance reasons. Use {@code config.getProperty(String key, Class<T> targetType)} instead of dot based navigation.
     */
    @Slf4j
    @Deprecated
    @CompileStatic
    static class NullSafeNavigator implements Map<String, Object> {
        final NavigableMap parent
        final List<String> path

        NullSafeNavigator(NavigableMap parent, List<String> path) {
            this.parent = parent
            this.path = path
            log.warn("Accessing config key '{}' through dot notation has known performance issues, consider using 'config.getProperty(key, targetClass)' instead.", path)
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
            if (parentMap != null) {
                return parentMap.size()
            }
            return 0
        }

        @Override
        boolean isEmpty() {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if (parentMap != null) {
                return parentMap.isEmpty()
            }
            return true
        }

        boolean containsKey(Object key) {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if (parentMap == null) return false
            else {
                return parentMap.containsKey(key)
            }
        }

        @Override
        boolean containsValue(Object value) {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if (parentMap != null) {
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
            throw new UnsupportedOperationException('Configuration cannot be modified')
        }

        @Override
        Object remove(Object key) {
            throw new UnsupportedOperationException('Configuration cannot be modified')
        }

        @Override
        void putAll(Map<? extends String, ?> m) {
            throw new UnsupportedOperationException('Configuration cannot be modified')
        }

        @Override
        void clear() {
            throw new UnsupportedOperationException('Configuration cannot be modified')
        }

        @Override
        Set<String> keySet() {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if (parentMap != null) {
                return parentMap.keySet()
            }
            return Collections.emptySet()
        }

        @Override
        Collection<Object> values() {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if (parentMap != null) {
                return parentMap.values()
            }
            return Collections.emptySet()
        }

        @Override
        Set<Entry<String, Object>> entrySet() {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if (parentMap != null) {
                return parentMap.entrySet()
            }
            return Collections.emptySet()
        }

        Object getProperty(String name) {
            NavigableMap parentMap = parent.navigateSubMap(path, false)
            if (parentMap == null) {
                return new NullSafeNavigator(parent, ((path + [name]) as List<String>).asImmutable())
            } else {
                return parentMap.get(name)
            }
        }

        void setProperty(String name, Object value) {
            NavigableMap parentMap = parent.navigateSubMap(path, true)
            parentMap.setProperty(name, value)
        }

        boolean asBoolean() {
            false
        }

        Object invokeMethod(String name, Object args) {
            throw new NullPointerException('Cannot invoke method ' + name + '() on NullSafeNavigator')
        }

        boolean equals(Object to) {
            return to == null || DefaultGroovyMethods.is(this, to)
        }

        Iterator iterator() {
            return Collections.EMPTY_LIST.iterator()
        }

        Object plus(String s) {
            return toString() + s
        }

        Object plus(Object o) {
            throw new NullPointerException('Cannot invoke method plus on NullSafeNavigator')
        }

        boolean is(Object other) {
            return other == null || DefaultGroovyMethods.is(this, other)
        }

        Object asType(Class c) {
            if (c == Boolean || c == boolean) return false
            return null
        }

        String toString() {
            return null
        }

//        public int hashCode() {
//            throw new NullPointerException('Cannot invoke method hashCode() on NullSafeNavigator')
//        }
    }
}
