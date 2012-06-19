/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.commons.metaclass;

import groovy.lang.MissingPropertyException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;

/**
 * A generic dyanmic property for any type.
 *
 * @author Graeme Rocher
 */
public class GenericDynamicProperty extends AbstractDynamicProperty {

    private Class<?> type;
    private boolean readOnly;
    private Map<String, Object> propertyToInstanceMap = new ConcurrentHashMap<String, Object>();
    private Object initialValue;
    private FunctionCallback initialValueGenerator;

    /**
     * @param propertyName The name of the property
     * @param type The type of the property
     * @param initialValue The initial value of the property
     * @param readOnly True for read-only property
     */
    public GenericDynamicProperty(String propertyName, Class<?> type, Object initialValue, boolean readOnly) {
        super(propertyName);
        Assert.notNull(type, "Constructor argument 'type' cannot be null");
        this.readOnly = readOnly;
        this.type = type;
        this.initialValue = initialValue;
    }

    /**
     * @param propertyName The name of the property
     * @param type The type of the property
     * @param readOnly True for read-only property
     */
    public GenericDynamicProperty(String propertyName, Class<?> type, boolean readOnly) {
        this(propertyName, type, (Object)null, readOnly);
    }

    /**
     * <p>Variant that allows supply of a lazy-initialization function for the initial value.</p>
     * <p>This function is called only on the first access to the property for a given object, unless
     * the function returns null in which case it will be called again if another request is made.</p>
     *
     * @param propertyName The name of the property
     * @param type The type of the property
     * @param readOnly True for read-only property
     */
    public GenericDynamicProperty(String propertyName, Class<?> type, FunctionCallback initialValueGenerator,
            boolean readOnly) {
        this(propertyName, type, readOnly);
        this.initialValueGenerator = initialValueGenerator;
    }

    @Override
    public Object get(Object object) {
        String propertyKey = System.identityHashCode(object) + getPropertyName();
        if (propertyToInstanceMap.containsKey(propertyKey)) {
            return propertyToInstanceMap.get(propertyKey);
        }

        if (initialValueGenerator != null) {
            final Object value = initialValueGenerator.execute(object);
            propertyToInstanceMap.put(propertyKey, value);
            return value;
        }

        if (initialValue != null) {
            propertyToInstanceMap.put(propertyKey, initialValue);
            return initialValue;
        }

        return null;
    }

    @Override
    public void set(Object object, Object newValue) {
        if (readOnly) {
            throw new MissingPropertyException("Property '" + getPropertyName() + "' for object '" +
                    object.getClass().getName() + "' is read-only!", object.getClass());
        }

        if (newValue == null) {
            propertyToInstanceMap.put(String.valueOf(System.identityHashCode(object)) + getPropertyName(), null);
        }
        else if (type.isInstance(newValue)) {
            propertyToInstanceMap.put(String.valueOf(System.identityHashCode(object)) + getPropertyName(), newValue);
        }
        else {
            throw new MissingPropertyException("Property '" + getPropertyName() + "' for object '" +
                    object.getClass().getName() + "' cannot be set with value '" + newValue +
                    "'. Incorrect type.", object.getClass());
        }
    }
}
