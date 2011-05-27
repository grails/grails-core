/*
 * Copyright 2010 the original author or authors.
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

import groovy.lang.GString;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.runtime.metaclass.ReflectionMetaMethod;

/**
 * @author Graeme Rocher
 * @since 1.4
 */
public abstract class BaseApiProvider {

    private static List<String> EXCLUDED_METHODS = Arrays.asList("setMetaClass", "getMetaClass");

    public static final String CONSTRUCTOR_METHOD = "initialize";
    public static final String CTOR_GROOVY_METHOD = "<ctor>";

    @SuppressWarnings("rawtypes")
    protected List instanceMethods = new ArrayList();
    protected List<Method> staticMethods = new ArrayList<Method>();

    @SuppressWarnings("unchecked")
    public void addApi(final Object apiInstance) {
        if (apiInstance == null) {
            return;
        }

        Class<?> currentClass = apiInstance.getClass();
        while (currentClass != Object.class) {
            final Method[] declaredMethods = currentClass.getDeclaredMethods();

            for (final Method method : declaredMethods) {
                final int modifiers = method.getModifiers();
                if (!isNotExcluded(method, modifiers)) {
                    continue;
                }

                if (Modifier.isStatic(modifiers)) {
                    staticMethods.add(method);
                }
                else {
                    instanceMethods.add(new ReflectionMetaMethod(new CachedMethod(method)) {
                        @Override
                        public String getName() {

                            String methodName = super.getName();
                            if (methodName.equals(CONSTRUCTOR_METHOD)) {
                                return CTOR_GROOVY_METHOD;
                            }
                            return methodName;
                        }

                        @Override
                        public Object invoke(Object object, Object[] arguments) {
                            if (arguments.length == 0) {
                                return super.invoke(apiInstance, new Object[]{object});
                            }
                            return super.invoke(apiInstance, ArrayUtils.add(checkForGStrings(arguments), 0, object));
                        }

                        private Object[] checkForGStrings(Object[] arguments) {
                            for (int i = 0; i < arguments.length; i++) {
                                if (arguments[i] instanceof GString) {
                                    arguments[i] = arguments[i].toString();
                                }
                            }
                            return arguments;
                        }

                        @Override
                        public CachedClass[] getParameterTypes() {
                            final CachedClass[] paramTypes = method.getParameterTypes();
                            if (paramTypes.length > 0) {
                                return (CachedClass[]) ArrayUtils.subarray(paramTypes, 1, paramTypes.length);
                            }
                            return paramTypes;
                        }
                    });
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    private boolean isNotExcluded(Method method, final int modifiers) {
        final String name = method.getName();

        if (EXCLUDED_METHODS.contains(name)) return false;

        boolean isStatic = Modifier.isStatic(modifiers);
        int minParameters = isStatic ? 0 : 1;

        return Modifier.isPublic(modifiers) &&
                !(method.isSynthetic() || method.isBridge()) &&
                !Modifier.isAbstract(modifiers) &&
                    !name.contains("$") &&
                      (method.getParameterTypes().length >= minParameters);
    }
}
