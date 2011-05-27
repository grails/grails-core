/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.commons.cfg;

import groovy.util.ConfigObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.List;
import java.util.Map;

/**
 * Type safe abstraction over Grails configuration
 *
 * @author Graeme Rocher
 * @since  1.4
 */
public class GrailsConfig {

    private static final Log LOG = LogFactory.getLog(GrailsConfig.class);

    private GrailsApplication grailsApplication;

    public GrailsConfig(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    /**
     * Utility method for retrieving a configuration value.
     *
     * @param key the flattened key
     * @param <T> the type parameter
     * @return the value retrieved by ConfigurationHolder.flatConfig
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (key.indexOf(".") != -1) {
            return (T) getFlatConfig().get(key);
        }

        return (T)getConfig().get(key);
    }

    /**
     * Utility method for retrieving a configuration value and performs type checking
     * (i.e. logs a verbose WARN message on type mismatch).
     *
     * @param key the flattened key
     * @param type the required type
     * @param <T> the type parameter
     * @return the value retrieved by ConfigurationHolder.flatConfig
     */
    public <T> T get(String key, Class<T> type) {
        Object o = get(key);
        if (o != null) {
            if (!type.isAssignableFrom(o.getClass())) {
                LOG.warn(String.format(
                     "Configuration type mismatch for configuration value %s (%s)", key, type.getName()));
                return null;
            }
            return type.cast(o);
        }
        return null;
    }

    /**
     * Configuration Value lookup with a default value.
     *
     * @param key          the flattened key
     * @param defaultValue the default value
     * @param <T>          the type parameter
     * @return the value retrieved by ConfigurationHolder.flatConfig if not null, otherwise the <code>defaultValue</code>
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        T v;
        if (defaultValue != null) {
            v = (T) get(key, defaultValue.getClass());
        }
        else {
            v = (T) get(key);
        }
        return v != null ? v : defaultValue;
    }

    /**
     * Configuration Value lookup with a default value and a list of allowed values.
     *
     * @param key           the flattened key
     * @param defaultValue  the default value
     * @param allowedValues List of allowed values
     * @param <T>           the type parameter
     * @return the value retrieved by ConfigurationHolder.flatConfig, if it is contained in <code>allowedValues</code>, otherwise the <code>defaultValue</code>
     */
    public <T> T get(String key, T defaultValue, List<T> allowedValues) {
        T v = get(key, defaultValue);
        if (!allowedValues.contains(v)) {
            LOG.warn(String.format(
                  "Configuration value for key %s is not one of the allowed values (%s)",
                  key, DefaultGroovyMethods.inspect(allowedValues)));
            return defaultValue;
        }
        return v;
    }

    /**
     * Configuration Value lookup with thows a GrailsConfigurationException when the value is null
     *
     * @param key the flattened key
     * @param <T> the type parameter
     * @return the value retrieved by ConfigurationHolder.flatConfig
     */
    @SuppressWarnings("unchecked")
    public <T> T getMandatory(String key) {
        T v = (T) getFlatConfig().get(key);
        if (v == null) {
            throw new GrailsConfigurationException(String.format(
                    "Mandatory configuration value (%s) is not defined!", key));
        }

        return v;
    }

    /**
     * Configuration Value lookup with thows a GrailsConfigurationException when the value is null
     * or not within the allowedValues.
     *
     * @param key           the flattened key
     * @param allowedValues List of allowed values
     * @param <T>           the type parameter
     * @return the value retrieved by ConfigurationHolder.flatConfig
     */
    @SuppressWarnings("unchecked")
    public <T> T getMandatory(String key, List<T> allowedValues) {
        T val = (T)getMandatory(key);
        if (!allowedValues.contains(val)) {
            throw new GrailsConfigurationException(
                String.format("Configuration value for key %s is not one of the allowed values (%s)",
                    key, DefaultGroovyMethods.inspect(allowedValues)));
        }
        return val;
    }

    /**
     * Configuration Value lookup for Groovy's array-like property access <code>GrailsConfig['my.config.key']</code>
     *
     * @param key the config key
     * @return the configured value
     */
    public Object getAt(Object key) {
        if (key instanceof String) {
            return get((String)key);
        }

        return getConfig().get(key);
    }

    @SuppressWarnings("rawtypes")
    public Map getFlatConfig() {
        return grailsApplication.getFlatConfig();
    }

    public ConfigObject getConfig() {
        return grailsApplication.getConfig();
    }
}
