package grails.config

import groovy.transform.CompileStatic

import org.yaml.snakeyaml.Yaml

@CompileStatic
public class GrailsConfig {
    final Map<String, Object> config

    public GrailsConfig() {
        config = createEmptyConfigMap()
    }

    protected Map<String, Object> createEmptyConfigMap() {
        [:]
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
        mergeMaps(config, sourceMap)
    }
    
    private static void mergeMaps(Map targetMap, Map sourceMap) {
        sourceMap.each { Object sourceKeyObject, Object sourceValue ->
            String sourceKey = String.valueOf(sourceKeyObject)
            Object currentValue = targetMap.containsKey(sourceKey) ? targetMap.get(sourceKey) : null
            Object newValue = sourceValue
            if(currentValue instanceof Map && sourceValue instanceof Map) {
                newValue = new LinkedHashMap(currentValue)
                mergeMaps((Map)newValue, (Map)sourceValue)
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

    public <T> T navigateConfigForType(Class<T> requiredType, String... path) {
        Object result = navigateMap(config, path)
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

    public String navigateConfig(String... path) {
        return navigateConfigForType(String, path)
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
