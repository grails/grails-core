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
public class PropertySourcesConfig extends NavigableMapConfig {

    protected PropertySources propertySources;
    protected PropertySourcesPropertyResolver propertySourcesPropertyResolver;

    protected String prefix;

    public PropertySourcesConfig(PropertySources propertySources) {
        this(propertySources, null);
    }

    public PropertySourcesConfig(PropertySources propertySources, String prefix) {
        this.propertySources = propertySources;
        this.propertySourcesPropertyResolver = new PropertySourcesPropertyResolver(propertySources);
        this.prefix = prefix;
        initializeFromPropertySources(propertySources);
    }

    public PropertySourcesConfig() {
        this.propertySources = new MutablePropertySources();
        this.propertySourcesPropertyResolver = new PropertySourcesPropertyResolver(propertySources);
    }

    public PropertySourcesConfig(Map<String, Object> mapPropertySource) {
        MutablePropertySources mutablePropertySources = new MutablePropertySources();
        mutablePropertySources.addFirst(new MapPropertySource("config", mapPropertySource));
        this.propertySources = mutablePropertySources;
        this.propertySourcesPropertyResolver = new PropertySourcesPropertyResolver(propertySources);
        initializeFromPropertySources(propertySources);
    }

    public PropertySources getPropertySources() {
        return propertySources;
    }

    public void refresh() {
        initializeFromPropertySources(propertySources);
    }

    protected void initializeFromPropertySources(PropertySources propertySources) {
        List<PropertySource<?>> propertySourceList = DefaultGroovyMethods.toList(propertySources);
        Collections.reverse(propertySourceList);
        for(PropertySource propertySource : propertySourceList) {
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
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for(String propertyName : enumerablePropertySource.getPropertyNames()) {
            if(prefix != null) {
                map.put(prefix + '.' + propertyName, enumerablePropertySource.getProperty(propertyName));
            }
            else {
                map.put(propertyName, enumerablePropertySource.getProperty(propertyName));
            }
        }

        configMap.merge(map, true);
        configMap.merge(map);
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void setConversionService(ConfigurableConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public String resolvePlaceholders(String text) {
        return propertySourcesPropertyResolver.resolvePlaceholders(text);
    }

    @Override
    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        return propertySourcesPropertyResolver.resolveRequiredPlaceholders(text);
    }

    public static class ClassConversionException extends ConversionException {

        public ClassConversionException(Class<?> actual, Class<?> expected) {
            super(String.format("Actual type %s is not assignable to expected type %s", actual.getName(), expected.getName()));
        }

        public ClassConversionException(String actual, Class<?> expected, Exception ex) {
            super(String.format("Could not find/load class %s during attempt to convert to %s", actual, expected.getName()), ex);
        }
    }
}
