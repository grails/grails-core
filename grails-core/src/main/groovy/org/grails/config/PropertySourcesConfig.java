/*
 * Copyright 2014 original authors
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
package org.grails.config;

import grails.config.Config;
import grails.config.ConfigMap;
import grails.util.*;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.*;
import org.springframework.util.ClassUtils;

import java.util.*;

/**
 * @author Graeme Rocher
 * @since 3.0
 */
public class PropertySourcesConfig implements Config, Map<String,Object> {

    protected PropertySources propertySources;
    protected PropertySourcesPropertyResolver propertySourcesPropertyResolver;
    protected ClassLoader classLoader = getClass().getClassLoader();
    protected ConfigurableConversionService conversionService = new DefaultConversionService();

    protected ConfigMap configMap = new ConfigMap();

    public PropertySourcesConfig(PropertySources propertySources) {
        this.propertySources = propertySources;
        this.propertySourcesPropertyResolver = new PropertySourcesPropertyResolver(propertySources);
        initializeFromPropertySources(propertySources);
    }

    public PropertySourcesConfig() {
        this.propertySources = new MutablePropertySources();
        this.propertySourcesPropertyResolver = new PropertySourcesPropertyResolver(propertySources);
    }

    public void refresh() {
        initializeFromPropertySources(propertySources);
    }

    protected void initializeFromPropertySources(PropertySources propertySources) {
        for(PropertySource propertySource : propertySources) {
            if(propertySource instanceof EnumerablePropertySource) {
                EnumerablePropertySource enumerablePropertySource = (EnumerablePropertySource)propertySource;
                mergeEnumerablePropertySource(enumerablePropertySource);
            }
        }

        EnvironmentAwarePropertySource environmentAwarePropertySource = new EnvironmentAwarePropertySource(propertySources);
        mergeEnumerablePropertySource(environmentAwarePropertySource);
        if(propertySources instanceof MutablePropertySources) {
            ((MutablePropertySources)propertySources).addLast(environmentAwarePropertySource);
        }
    }

    private void mergeEnumerablePropertySource(EnumerablePropertySource enumerablePropertySource) {
        Map map = new HashMap();
        for(String propertyName : enumerablePropertySource.getPropertyNames()) {
            map.put(propertyName, enumerablePropertySource.getProperty(propertyName));
        }

        configMap.merge(map, true);
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void setConversionService(ConfigurableConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Object getAt(Object key) {
        return get(key);
    }

    @Override
    public void setAt(Object key, Object value) {
        configMap.put(key.toString(), value);
    }

    @Override
    public int size() {
        return configMap.size();
    }

    @Override
    public boolean isEmpty() {
        return configMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return containsProperty(key.toString());
    }

    @Override
    public boolean containsValue(Object value) {
        return configMap.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return configMap.getProperty(key.toString());
    }

    @Override
    public Object put(String key, Object value) {
        return configMap.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return configMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        configMap.putAll(m);
    }

    @Override
    public void clear() {
        configMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return configMap.keySet();
    }

    @Override
    public Collection<Object> values() {
        return configMap.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return configMap.entrySet();
    }

    @Override
    public Map<String, Object> flatten() {
        return configMap.toFlatConfig();
    }

    @Override
    public Properties toProperties() {
        return configMap.toProperties();
    }

    @Override
    public Object navigate(String... path) {
        return configMap.navigate(path);
    }

    @Override
    public Config merge(Map<String, Object> toMerge) {
        configMap.merge(toMerge, true);
        return this;
    }

    public Object asType(Class c) {
        if(c==Boolean.class || c==boolean.class) return false;
        return null;
    }

    @Override
    public Iterator<Entry<String, Object>> iterator() {
        return DefaultGroovyMethods.iterator(configMap);
    }

    @Override
    public boolean containsProperty(String key) {
        return getProperty(key, Object.class) != null;
    }

    @Override
    public String getProperty(String key) {
        return getProperty(key, String.class);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return getProperty(key, String.class, defaultValue);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType) {
        return getProperty(key, targetType, null);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        Object originalValue = configMap.get(key);
        if(originalValue == null && key.contains(".")) {
            originalValue = configMap.navigate(key.split("\\."));
        }
        T value = conversionService.convert(originalValue, targetType);
        return DefaultGroovyMethods.asBoolean(value) ? value : defaultValue;
    }

    @Override
    public <T> Class<T> getPropertyAsClass(String key, Class<T> targetType) {
        String className = getProperty(key, String.class);

        if(!GrailsStringUtils.isBlank(className)) {
            try {
                Class<T> clazz = (Class<T>) ClassUtils.forName((String) className, classLoader);
                if(clazz != targetType) {
                    throw new ClassConversionException(clazz, targetType);
                }
                return clazz;
            } catch (Exception e) {
                throw new ClassConversionException(className, targetType, e);
            }
        }
        else {
            throw new IllegalStateException("Value for $key cannot be resolved");
        }
    }

    @Override
    public String getRequiredProperty(String key) throws IllegalStateException {
        String value = getProperty(key);
        if(GrailsStringUtils.isBlank(value)) {
            throw new IllegalStateException("Value for key ["+key+"] cannot be resolved");
        }
        return value;
    }

    @Override
    public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
        T value = getProperty(key, targetType);
        if(value == null) {
            throw new IllegalStateException("Value for key ["+key+"] cannot be resolved");
        }
        return value;
    }

    @Override
    public String resolvePlaceholders(String text) {
        return propertySourcesPropertyResolver.resolvePlaceholders(text);
    }

    @Override
    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        return propertySourcesPropertyResolver.resolveRequiredPlaceholders(text);
    }

    private static class ClassConversionException extends ConversionException {

        public ClassConversionException(Class<?> actual, Class<?> expected) {
            super(String.format("Actual type %s is not assignable to expected type %s", actual.getName(), expected.getName()));
        }

        public ClassConversionException(String actual, Class<?> expected, Exception ex) {
            super(String.format("Could not find/load class %s during attempt to convert to %s", actual, expected.getName()), ex);
        }
    }
}
