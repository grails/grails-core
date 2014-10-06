package grails.config
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import org.yaml.snakeyaml.Yaml

@CompileStatic
@Canonical
public class GrailsConfig implements Cloneable {
    final NullSafeNavigatorMap configMap

    public GrailsConfig() {
        configMap = new NullSafeNavigatorMap()
    }
    
    public GrailsConfig(GrailsConfig copyOf) {
        this(copyOf.@configMap)
    }

    public GrailsConfig(Map copyOf) {
        this()
        mergeMap(copyOf)
    }
    
    public GrailsConfig clone() {
        new GrailsConfig(this)
    }

    public void loadYml(File ymlFile) {
        ymlFile.withInputStream { InputStream input ->
            loadYml(input)
        }
    }

    @groovy.transform.CompileDynamic // fails with CompileStatic!
    public void loadYml(InputStream input) {
        Yaml yaml = new Yaml()
        for(Object yamlObject : yaml.loadAll(input)) {
            if(yamlObject instanceof Map) { // problem here with CompileStatic
                mergeMap((Map)yamlObject)
            }
        }
    }
    
    public void mergeMap(Map sourceMap) {
        mergeMaps(configMap, sourceMap)
    }
    
    private static void mergeMaps(Map<String, Object> targetMap, Map sourceMap) {
        sourceMap.each { Object sourceKeyObject, Object sourceValue ->
            String sourceKey = String.valueOf(sourceKeyObject)
            Object currentValue = targetMap.containsKey(sourceKey) ? targetMap.get(sourceKey) : null
            Object newValue
            if(sourceValue instanceof Map) {
                newValue = new NullSafeNavigatorMap(currentValue instanceof Map ? (Map)currentValue : [:])
                mergeMaps((Map)newValue, (Map)sourceValue)
            } else {
                newValue = sourceValue
            }
            if (newValue == null) {
                targetMap.remove(sourceKey)
            } else {
                targetMap.put(sourceKey, newValue)
            }
        }
    }
    
    private Object navigateMap(Map<String, Object> map, String... path) {
        if(map==null) return null
        if(path.length == 1) {
            return map.get(path[0])
        } else {
            return navigateMap((Map<String, Object>)map.get(path[0]), path.tail())
        }
    }

    public <T> T navigate(Class<T> requiredType, String... path) {
        Object result = navigateMap(configMap, path)
        if(result == null) {
            return null
        }
        return convertToType(result, requiredType)
    }
    
    protected <T> T convertToType(Object value, Class<T> requiredType) {
        if(requiredType.isInstance(value)) {
            return (T)value
        }
        if(requiredType==String.class) {
            return String.valueOf(value)
        } else if(requiredType==Boolean.class) {
            Boolean booleanObject = toBooleanObject(String.valueOf(value))
            return booleanObject != null ? booleanObject : Boolean.FALSE
        } else if(requiredType==Integer.class) {
            if(value instanceof Number) {
                return Integer.valueOf(((Number)value).intValue())
            } else {
                return Integer.valueOf(String.valueOf(value))
            }
        } else if(requiredType==Long.class) {
            if(value instanceof Number) {
                return Long.valueOf(((Number)value).longValue())
            } else {
                return Long.valueOf(String.valueOf(value))
            }
        } else if(requiredType==Double.class) {
            if(value instanceof Number) {
                return Double.valueOf(((Number)value).doubleValue())
            } else {
                return Double.valueOf(String.valueOf(value))
            }
        } else if(requiredType==BigDecimal.class) {
            return new BigDecimal(String.valueOf(value))
        } else {
            return convertToOtherTypes(value, requiredType)
        }
    }
    
    protected <T> T convertToOtherTypes(Object value, Class<T> requiredType) {
        throw new RuntimeException("conversion to $requiredType.name not implemented")
    }

    public Object navigate(String... path) {
        return navigate(Object, path)
    }
    
    public boolean asBoolean() {
        return !configMap.isEmpty()
    }
    
    public Map<String, Object> asMap() {
        new GrailsConfig(this).@configMap
    }
    
    public Object asType(Class type) {
        if(type==Boolean || type==boolean) {
            return asBoolean()
        } else if (type==String) {
            return toString()
        } else if (type==Map) {
            return asMap()
        } else if (type==GrailsConfig) {
            return new GrailsConfig(this)
        } else {
            throw new GroovyCastException(this, type)
        }
    }
    
    public Object getAt(Object key) {
        getProperty(String.valueOf(key))
    }
    
    public void setAt(Object key, Object value) {
        setProperty(String.valueOf(key), value)
    }
    
    public Object getProperty(String name) {
        if ("configMap".equals(name))
            return this.configMap
        return configMap.getProperty(name)
    }
    
    public void setProperty(String name, Object value) {
        configMap.setProperty(name, value)
    }
    
    @InheritConstructors
    private static class NullSafeNavigatorMap extends LinkedHashMap<String, Object> {
        public Object getProperty(String name) {
            if (!containsKey(name)) {
                return new NullSafeNavigator(this, [name].asImmutable())
            }
            return get(name)
        }
        
        public void setProperty(String name, Object value) {
            if(value instanceof Map && containsKey(name) && get(name) instanceof Map) {
                GrailsConfig.mergeMaps((Map)get(name), value)
            } else {
                if(value==null) {
                    remove(name)
                } else {
                    put(name, value)
                }
            }
        }
    }
    
    @CompileStatic
    private static class NullSafeNavigator {
        final Map<String, Object> parent
        final List<String> path
        
        NullSafeNavigator(Map<String, Object> parent, List<String> path) {
            this.parent = parent
            this.path = path
        }
        
        public Object getProperty(String name) {
            return new NullSafeNavigator(parent, ((path + [name]) as List<String>).asImmutable())
        }
        
        public void setProperty(String name, Object value) {
            Map<String, Object> parentMap = navigateAndCreateParent(parent, path)
            parentMap.put(name, value)
        }
        
        private Map<String, Object> navigateAndCreateParent(Map<String, Object> parent, List<String> path) {
            Map<String, Object> currentMap = parent
            for(String pathElement : path) {
                Map<String, Object> newMap = new NullSafeNavigatorMap()
                currentMap.put(pathElement, newMap)
                currentMap = newMap
            }
            currentMap
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
            return "null"
        }
    
        public int hashCode() {
            throw new NullPointerException("Cannot invoke method hashCode() on NullSafeNavigator");
        }
    }
    
    /**
     * toBooleanObject method ported from org.apache.commons.lang.BooleanUtils.toBooleanObject to Groovy code
     * @param str
     * @return
     */
    private static Boolean toBooleanObject(String str) {
        if (str.is("true")) {
            return Boolean.TRUE
        }
        if (str == null) {
            return null
        }
        int strlen = str.length()
        if (strlen==0) {
            return null
        } else if (strlen == 1) {
            char ch0 = str.charAt(0)
            if ((ch0 == 'y' || ch0 == 'Y') ||
                (ch0 == 't' || ch0 == 'T')) {
                return Boolean.TRUE
            }
            if ((ch0 == 'n' || ch0 == 'N') ||
                (ch0 == 'f' || ch0 == 'F')) {
                return Boolean.FALSE
            }
        } else if (strlen == 2) {
            char ch0 = str.charAt(0)
            char ch1 = str.charAt(1)
            if ((ch0 == 'o' || ch0 == 'O') &&
                (ch1 == 'n' || ch1 == 'N') ) {
                return Boolean.TRUE
            }
            if ((ch0 == 'n' || ch0 == 'N') &&
                (ch1 == 'o' || ch1 == 'O') ) {
                return Boolean.FALSE
            }
        } else if (strlen == 3) {
            char ch0 = str.charAt(0)
            char ch1 = str.charAt(1)
            char ch2 = str.charAt(2)
            if ((ch0 == 'y' || ch0 == 'Y') &&
                (ch1 == 'e' || ch1 == 'E') &&
                (ch2 == 's' || ch2 == 'S') ) {
                return Boolean.TRUE
            }
            if ((ch0 == 'o' || ch0 == 'O') &&
                (ch1 == 'f' || ch1 == 'F') &&
                (ch2 == 'f' || ch2 == 'F') ) {
                return Boolean.FALSE
            }
        } else if (strlen == 4) {
            char ch0 = str.charAt(0)
            char ch1 = str.charAt(1)
            char ch2 = str.charAt(2)
            char ch3 = str.charAt(3)
            if ((ch0 == 't' || ch0 == 'T') &&
                (ch1 == 'r' || ch1 == 'R') &&
                (ch2 == 'u' || ch2 == 'U') &&
                (ch3 == 'e' || ch3 == 'E') ) {
                return Boolean.TRUE
            }
        } else if (strlen == 5) {
            char ch0 = str.charAt(0)
            char ch1 = str.charAt(1)
            char ch2 = str.charAt(2)
            char ch3 = str.charAt(3)
            char ch4 = str.charAt(4)
            if ((ch0 == 'f' || ch0 == 'F') &&
                (ch1 == 'a' || ch1 == 'A') &&
                (ch2 == 'l' || ch2 == 'L') &&
                (ch3 == 's' || ch3 == 'S') &&
                (ch4 == 'e' || ch4 == 'E') ) {
                return Boolean.FALSE
            }
        }
        return null
    }
}
