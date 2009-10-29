/*
 * Copyright 2004-2008 the original author or authors.
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
package org.codehaus.groovy.grails.web.converters.configuration;

import org.codehaus.groovy.grails.web.converters.Converter;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton which holds all default and named configurations for the Converter classes
 *
 * @author Siegfried Puchbauer
 * @since 1.1
 */
public class ConvertersConfigurationHolder {

    public static final String CONVERTERS_DEFAULT_ENCODING = "UTF-8";

    private static ConvertersConfigurationHolder INSTANCE = new ConvertersConfigurationHolder();

    private final Map<Class<? extends Converter>, ConverterConfiguration> defaultConfiguration =
            new HashMap<Class<? extends Converter>, ConverterConfiguration>();

    private final Map<Class<? extends Converter>, Map<String, ConverterConfiguration>> namedConfigurations =
            new HashMap<Class<? extends Converter>, Map<String, ConverterConfiguration>>();

    private final Map<Class<? extends Converter>, ThreadLocal<ConverterConfiguration>> threadLocalConfiguration =
            new HashMap<Class<? extends Converter>, ThreadLocal<ConverterConfiguration>>();

    private ConvertersConfigurationHolder() {
    }

    public static <C extends Converter> void setDefaultConfiguration(Class<C> c, ConverterConfiguration<C> cfg) {
        getInstance().defaultConfiguration.put(c, cfg);    
    }

    public static <C extends Converter> void setDefaultConfiguration(Class<C> c, List<ObjectMarshaller<C>> om) {
        getInstance().defaultConfiguration.put(c, new DefaultConverterConfiguration<C>(om));
    }

    private static ConvertersConfigurationHolder getInstance() throws ConverterException{
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <C extends Converter> ConverterConfiguration<C> getConverterConfiguration(Class<C> converterClass) throws ConverterException {
        ConverterConfiguration<C> cfg = getThreadLocalConverterConfiguration(converterClass);
        if(cfg == null) {
            cfg = (ConverterConfiguration<C>) getInstance().defaultConfiguration.get(converterClass);
        }
        if(cfg == null) {
            cfg = new DefaultConverterConfiguration();
        }
        return cfg;
    }

    @SuppressWarnings("unchecked")
    public static <C extends Converter> ConverterConfiguration<C> getNamedConverterConfiguration(String name, Class<C> converterClass) throws ConverterException {
        Map<String, ConverterConfiguration> map = getNamedConfigMapForConverter(converterClass, false);
        return map != null ? map.get(name) : null;
    }

    @SuppressWarnings("unchecked")
    public static <C extends Converter> ConverterConfiguration<C> getThreadLocalConverterConfiguration(Class<C> converterClass) throws ConverterException {
        return getThreadLocalForConverter(converterClass, true).get();
    }

    public static <C extends Converter> void setTheadLocalConverterConfiguration(Class<C> converterClass, ConverterConfiguration<C> cfg) throws ConverterException {
        getThreadLocalForConverter(converterClass, true).set(cfg);
    }

    private static <C extends Converter> ThreadLocal<ConverterConfiguration> getThreadLocalForConverter(Class<C> converter, boolean create) {
        ThreadLocal<ConverterConfiguration> threadlocal = getInstance().threadLocalConfiguration.get(converter);
        if(threadlocal == null && create) {
            threadlocal = new ThreadLocal<ConverterConfiguration>();
            getInstance().threadLocalConfiguration.put(converter, threadlocal);
        }
        return threadlocal;
    }

    public static <C extends Converter> void setNamedConverterConfiguration(Class<C> converterClass, String name, ConverterConfiguration<C> cfg) throws ConverterException {
        getNamedConfigMapForConverter(converterClass, true).put(name, cfg);
    }

    private static <C extends Converter> Map<String, ConverterConfiguration> getNamedConfigMapForConverter(Class<C> clazz, boolean create) {
        Map<String, ConverterConfiguration> namedConfigs = getInstance().namedConfigurations.get(clazz);
        if(namedConfigs == null && create) {
            namedConfigs = new HashMap<String, ConverterConfiguration>();
            getInstance().namedConfigurations.put(clazz, namedConfigs);
        }
        return namedConfigs;
    }

    public static <C extends Converter> void setNamedConverterConfiguration(Class<C> converterClass, String name, List<ObjectMarshaller<C>> om) throws ConverterException {
        getNamedConfigMapForConverter(converterClass, true).put(name, new DefaultConverterConfiguration<C>(om));
    }

}
