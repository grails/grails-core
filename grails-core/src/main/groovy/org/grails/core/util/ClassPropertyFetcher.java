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
package org.grails.core.util;

import grails.util.GrailsClassUtils;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.Script;
import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.reflection.CachedField;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.reflection.ClassInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Accesses class "properties": static fields, static getters, instance fields
 * or instance getters.
 *
 * Method and Field instances are cached for fast access.

 * @author Lari Hotari, Sagire Software Oy
 * @author Graeme Rocher
 */
public class ClassPropertyFetcher {

    private static final Set<String> IGNORED_FIELD_NAMES = new HashSet<String>() {{
        add("class");
        add("metaClass");
    }};
    private final Class<?> clazz;
    private final Map<String, PropertyFetcher> staticFetchers = new HashMap<>();
    private final Map<String, PropertyFetcher> instanceFetchers = new HashMap<>();
    private final ReferenceInstanceCallback callback;
    private final FieldCallback fieldCallback;
    private final MethodCallback methodCallback;

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
                            try {
                                o = c.newInstance();
                            } catch (InstantiationException e) {
                                throw new RuntimeException("Could not instantiate instance: " + e.getMessage(), e);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Could not instantiate instance: " + e.getMessage(), e);
                            }
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

    protected ClassPropertyFetcher(Class<?> clazz, ReferenceInstanceCallback callback) {
        this.clazz = clazz;
        this.callback = callback;
        fieldCallback = new FieldCallback() {
            public void doWith(CachedField field) {
                final int modifiers = field.getModifiers();
                if (!Modifier.isPublic(modifiers)) {
                    return;
                }

                final String name = field.getName();
                if (name.indexOf('$') == -1) {
                    if(IGNORED_FIELD_NAMES.contains(name)) return;

                    boolean staticField = Modifier.isStatic(modifiers);
                    if (staticField) {
                        staticFetchers.put(name, new FieldReaderFetcher(field,true));
                    } else {
                        instanceFetchers.put(name, new FieldReaderFetcher(field, false));
                    }
                }
            }
        };

        methodCallback = new MethodCallback() {
            public void doWith(CachedMethod method) throws IllegalArgumentException,
                    IllegalAccessException {
                Class<?> returnType = method.getReturnType();

                if (!method.isPublic()) {
                    return;
                }
                if (returnType == Void.class || returnType == void.class || method.getParameterTypes().length != 0) {
                    return;
                }

                String propertyName = GrailsClassUtils.getPropertyForGetter(method.getName());
                if(propertyName == null || propertyName.indexOf('$') != -1) {
                    return;
                }

                if (method.getName().startsWith("is") &&
                            !(returnType == Boolean.class || returnType == boolean.class)) {
                    return;
                }

                if (method.isStatic()) {
                    staticFetchers.put(propertyName, new GetterPropertyFetcher(method, true));
                } else {
                    instanceFetchers.put(propertyName, new GetterPropertyFetcher(method, false));
                }
            }
        };
        init();
    }

    public Object getReference() {
        if (callback != null) {
            return callback.getReferenceInstance();
        }
        return null;
    }

    public PropertyDescriptor[] getPropertyDescriptors() {
        return getPropertyDescriptors(clazz);
    }

    public boolean isReadableProperty(String name) {
        return staticFetchers.containsKey(name)
                || instanceFetchers.containsKey(name);
    }

    private void init() {
        ClassInfo classInfo = ClassInfo.getClassInfo(clazz);
        Class<?> superclass = clazz.getSuperclass();
        while (superclass != Object.class && superclass != Script.class && superclass != GroovyObjectSupport.class && superclass != null) {
            ClassPropertyFetcher superFetcher = ClassPropertyFetcher.forClass(superclass);
            staticFetchers.putAll(superFetcher.staticFetchers);
            instanceFetchers.putAll(superFetcher.instanceFetchers);
            superclass = superclass.getSuperclass();
        }

        CachedClass cachedClass = classInfo.getCachedClass();
        CachedField[] fields = cachedClass.getFields();
        for (CachedField field : fields) {
            try {
                fieldCallback.doWith(field);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(
                        "Shouldn't be illegal to access field '"
                                + field.getName() + "': " + ex);
            }
        }
        CachedMethod[] methods = cachedClass.getMethods();
        for (CachedMethod method : methods) {
            try {
                methodCallback.doWith(method);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(
                        "Shouldn't be illegal to access method '"
                                + method.getName() + "': " + ex);
            }
        }

    }

    private PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) {
        return BeanUtils.getPropertyDescriptors(clazz);
    }

    public Object getPropertyValue(String name) {
        return getPropertyValue(name, false);
    }

    public Object getPropertyValue(final Object object,String name) {
        PropertyFetcher fetcher = resolveFetcher(name, true);
        return getPropertyWithFetcherAndCallback(name, fetcher, new ReferenceInstanceCallback() {
            @Override
            public Object getReferenceInstance() {
                return object;
            }
        });
    }


    public Object getPropertyValue(String name, boolean onlyInstanceProperties) {
        PropertyFetcher fetcher = resolveFetcher(name, onlyInstanceProperties);
        return getPropertyValueWithFetcher(name, fetcher);
    }

    private Object getPropertyValueWithFetcher(String name, PropertyFetcher fetcher) {
        ReferenceInstanceCallback referenceInstanceCallback = callback;
        return getPropertyWithFetcherAndCallback(name, fetcher, referenceInstanceCallback);
    }

    private Object getPropertyWithFetcherAndCallback(String name, PropertyFetcher fetcher, ReferenceInstanceCallback referenceInstanceCallback) {
        if (fetcher != null) {
            try {
                return fetcher.get(referenceInstanceCallback);
            }
            catch (Exception e) {
                // ignore
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

    public interface ReferenceInstanceCallback {
        Object getReferenceInstance();
    }

    interface PropertyFetcher {
        Object get(ReferenceInstanceCallback callback)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException;
        Class<?> getPropertyType(String name);
    }

    static class GetterPropertyFetcher implements PropertyFetcher {
        private static final Object[] ZERO_ARGS = new Object[0];
        private final CachedMethod readMethod;
        private final boolean staticMethod;

        GetterPropertyFetcher(CachedMethod readMethod, boolean staticMethod) {
            this.readMethod = readMethod;
            this.staticMethod = staticMethod;
        }

        public Object get(ReferenceInstanceCallback callback)
                throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            if (staticMethod) {
                return readMethod.invoke(null, ZERO_ARGS);
            }

            if (callback != null) {
                return readMethod.invoke(callback.getReferenceInstance(), ZERO_ARGS);
            }

            return null;
        }

        public Class<?> getPropertyType(String name) {
            return readMethod.getReturnType();
        }
    }

    static class FieldReaderFetcher implements PropertyFetcher {
        private final CachedField field;
        private final boolean staticField;

        public FieldReaderFetcher(CachedField field, boolean staticField) {
            this.field = field;
            this.staticField = staticField;
        }

        public Object get(ReferenceInstanceCallback callback)
                throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            if (staticField) {
                return field.getProperty(null);
            }

            if (callback != null) {
                return field.getProperty(callback.getReferenceInstance());
            }

            return null;
        }

        public Class<?> getPropertyType(String name) {
            return field.getType();
        }
    }

    private interface FieldCallback {
        void doWith(CachedField field) throws IllegalAccessException;
    }
    private interface MethodCallback {
        void doWith(CachedMethod field) throws IllegalAccessException;
    }
}
