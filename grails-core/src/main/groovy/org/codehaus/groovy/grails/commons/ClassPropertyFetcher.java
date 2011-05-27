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
package org.codehaus.groovy.grails.commons;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * Accesses class "properties": static fields, static getters, instance fields
 * or instance getters.
 *
 * Method and Field instances are cached for fast access.

 * @author Lari Hotari, Sagire Software Oy
 * @author Graeme Rocher
 */
public class ClassPropertyFetcher {

    private final Log log = LogFactory.getLog(getClass());
    private final Class<?> clazz;
    final Map<String, PropertyFetcher> staticFetchers = new HashMap<String, PropertyFetcher>();
    final Map<String, PropertyFetcher> instanceFetchers = new HashMap<String, PropertyFetcher>();
    private final ReferenceInstanceCallback callback;
    private PropertyDescriptor[] propertyDescriptors;

    private static Map<Class<?>, ClassPropertyFetcher> cachedClassPropertyFetchers = new ConcurrentHashMap<Class<?>, ClassPropertyFetcher>();

    public static void clearClassPropertyFetcherCache() {
        cachedClassPropertyFetchers.clear();
    }

    public static ClassPropertyFetcher forClass(Class<?> c) {
        return forClass(c, null);
    }

    public static ClassPropertyFetcher forClass(final Class<?> c, ReferenceInstanceCallback callback) {

        ClassPropertyFetcher cpf = cachedClassPropertyFetchers.get(c);
        if (cpf == null) {
            if (callback == null) {
                callback = new ReferenceInstanceCallback() {
                    private Object o;

                    public Object getReferenceInstance() {
                        if (o == null) {
                            o = BeanUtils.instantiate(c);
                        }
                        return o;
                    }
                };
            }
            cpf = new ClassPropertyFetcher(c, callback);
            cachedClassPropertyFetchers.put(c, cpf);
        }
        return cpf;
    }

    ClassPropertyFetcher(Class<?> clazz, ReferenceInstanceCallback callback) {
        this.clazz = clazz;
        this.callback = callback;
        init();
    }

    public Object getReference() {
        if (callback != null) {
            return callback.getReferenceInstance();
        }
        return null;
    }

    public PropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptors;
    }

    public boolean isReadableProperty(String name) {
        return staticFetchers.containsKey(name)
                || instanceFetchers.containsKey(name);
    }

    private void init() {
        FieldCallback fieldCallback = new ReflectionUtils.FieldCallback() {
            public void doWith(Field field) {
                if (field.isSynthetic())
                    return;
                final int modifiers = field.getModifiers();
                if (!Modifier.isPublic(modifiers))
                    return;

                final String name = field.getName();
                if (name.indexOf('$') == -1) {
                    boolean staticField = Modifier.isStatic(modifiers);
                    if (staticField) {
                        staticFetchers.put(name, new FieldReaderFetcher(field,
                                staticField));
                    } else {
                        instanceFetchers.put(name, new FieldReaderFetcher(
                                field, staticField));
                    }
                }
            }
        };

        MethodCallback methodCallback = new ReflectionUtils.MethodCallback() {
            public void doWith(Method method) throws IllegalArgumentException,
                    IllegalAccessException {
                if (method.isSynthetic())
                    return;
                if (!Modifier.isPublic(method.getModifiers()))
                    return;
                if (Modifier.isStatic(method.getModifiers())
                        && method.getReturnType() != Void.class) {
                    if (method.getParameterTypes().length == 0) {
                        String name = method.getName();
                        if (name.indexOf('$') == -1) {
                            if (name.length() > 3 && name.startsWith("get")
                                    && Character.isUpperCase(name.charAt(3))) {
                                name = name.substring(3);
                            } else if (name.length() > 2
                                    && name.startsWith("is")
                                    && Character.isUpperCase(name.charAt(2))
                                    && (method.getReturnType() == Boolean.class || method
                                            .getReturnType() == boolean.class)) {
                                name = name.substring(2);
                            }
                            PropertyFetcher fetcher = new GetterPropertyFetcher(
                                    method, true);
                            staticFetchers.put(name, fetcher);
                            staticFetchers.put(StringUtils.uncapitalize(name),
                                    fetcher);
                        }
                    }
                }
            }
        };

        List<Class<?>> allClasses = resolveAllClasses(clazz);
        for (Class<?> c : allClasses) {
            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                try {
                    fieldCallback.doWith(field);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(
                            "Shouldn't be illegal to access field '"
                                    + field.getName() + "': " + ex);
                }
            }
            Method[] methods = c.getDeclaredMethods();
            for (Method method : methods) {
                try {
                    methodCallback.doWith(method);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(
                            "Shouldn't be illegal to access method '"
                                    + method.getName() + "': " + ex);
                }
            }
        }

        propertyDescriptors = BeanUtils.getPropertyDescriptors(clazz);
        for (PropertyDescriptor desc : propertyDescriptors) {
            Method readMethod = desc.getReadMethod();
            if (readMethod != null) {
                boolean staticReadMethod = Modifier.isStatic(readMethod
                        .getModifiers());
                if (staticReadMethod) {
                    staticFetchers.put(desc.getName(),
                            new GetterPropertyFetcher(readMethod,
                                    staticReadMethod));
                } else {
                    instanceFetchers.put(desc.getName(),
                            new GetterPropertyFetcher(readMethod,
                                    staticReadMethod));
                }
            }
        }
    }

    private List<Class<?>> resolveAllClasses(Class<?> c) {
        List<Class<?>> list = new ArrayList<Class<?>>();
        Class<?> currentClass = c;
        while (currentClass != null) {
            list.add(currentClass);
            currentClass = currentClass.getSuperclass();
        }
        Collections.reverse(list);
        return list;
    }

    public Object getPropertyValue(String name) {
        return getPropertyValue(name, false);
    }

    public Object getPropertyValue(String name, boolean onlyInstanceProperties) {
        PropertyFetcher fetcher = resolveFetcher(name, onlyInstanceProperties);
        return getPropertyValueWithFetcher(name, fetcher);
    }

    private Object getPropertyValueWithFetcher(String name, PropertyFetcher fetcher) {
        if (fetcher != null) {
            try {
                return fetcher.get(callback);
            }
            catch (Exception e) {
                log.warn("Error fetching property's " + name + " value from class " + clazz.getName(), e);
            }
        }
        return null;
    }

    public <T> T getStaticPropertyValue(String name, Class<T> c) {
        PropertyFetcher fetcher = staticFetchers.get(name);
        if (fetcher != null) {
            Object v = getPropertyValueWithFetcher(name, fetcher);
            return returnOnlyIfInstanceOf(v, c);
        }
        return null;
    }
    public <T> T getPropertyValue(String name, Class<T> c) {
        return returnOnlyIfInstanceOf(getPropertyValue(name, false), c);
    }

    @SuppressWarnings("unchecked")
    private <T> T returnOnlyIfInstanceOf(Object value, Class<T> type) {
        if ((value != null) && (type==Object.class || GrailsClassUtils.isGroovyAssignableFrom(type, value.getClass()))) {
            return (T)value;
        }

        return null;
    }

    private PropertyFetcher resolveFetcher(String name, boolean onlyInstanceProperties) {
        PropertyFetcher fetcher = null;
        if (!onlyInstanceProperties) {
            fetcher = staticFetchers.get(name);
        }
        if (fetcher == null) {
            fetcher = instanceFetchers.get(name);
        }
        return fetcher;
    }

    public Class<?> getPropertyType(String name) {
        return getPropertyType(name, false);
    }

    public Class<?> getPropertyType(String name, boolean onlyInstanceProperties) {
        PropertyFetcher fetcher = resolveFetcher(name, onlyInstanceProperties);
        if (fetcher != null) {
            return fetcher.getPropertyType(name);
        }
        return null;
    }

    public static interface ReferenceInstanceCallback {
        public Object getReferenceInstance();
    }

    static interface PropertyFetcher {
        public Object get(ReferenceInstanceCallback callback)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException;
        public Class<?> getPropertyType(String name);
    }

    static class GetterPropertyFetcher implements PropertyFetcher {
        private final Method readMethod;
        private final boolean staticMethod;

        GetterPropertyFetcher(Method readMethod, boolean staticMethod) {
            this.readMethod = readMethod;
            this.staticMethod = staticMethod;
            ReflectionUtils.makeAccessible(readMethod);
        }

        public Object get(ReferenceInstanceCallback callback)
                throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            if (staticMethod) {
                return readMethod.invoke(null);
            }

            if (callback != null) {
                return readMethod.invoke(callback.getReferenceInstance());
            }

            return null;
        }

        public Class<?> getPropertyType(String name) {
            return readMethod.getReturnType();
        }
    }

    static class FieldReaderFetcher implements PropertyFetcher {
        private final Field field;
        private final boolean staticField;

        public FieldReaderFetcher(Field field, boolean staticField) {
            this.field = field;
            this.staticField = staticField;
            ReflectionUtils.makeAccessible(field);
        }

        public Object get(ReferenceInstanceCallback callback)
                throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            if (staticField) {
                return field.get(null);
            }

            if (callback != null) {
                return field.get(callback.getReferenceInstance());
            }

            return null;
        }

        public Class<?> getPropertyType(String name) {
            return field.getType();
        }
    }
}
