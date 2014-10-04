package grails.config;

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
        Yaml yaml = new Yaml();
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
        if(requiredType==Integer.class) {
            if(value instanceof Number) {
                return Integer.valueOf(((Number)value).intValue())
            } else {
                return Integer.valueOf(String.valueOf(value))
            }
        } else if(requiredType==String.class) {
            return String.valueOf(value)
        } else {
            return convertToOtherTypes(value, requiredType)
        }
    }
    
    protected <T> T convertToOtherTypes(Object value, Class<T> requiredType) {
        throw new RuntimeException("conversion to $requiredType.name not implemented")
    }

    public String navigateConfig(String... path) {
        return navigateConfigForType(String, path);
    }
}
