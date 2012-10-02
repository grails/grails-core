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
package org.grails.plugins.tomcat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A class loader that searches the parent
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class ParentDelegatingClassLoader extends ClassLoader{

    private Method findClassMethod;

    protected ParentDelegatingClassLoader(ClassLoader parent) {
        super(parent);
        findClassMethod = findMethod(ClassLoader.class,"findClass", String.class);
        findClassMethod.setAccessible(true);
    }

    private Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Class<?> searchType = clazz;
        while (searchType != null) {
            Method[] methods = (searchType.isInterface() ? searchType.getMethods() : searchType.getDeclaredMethods());
            for (Method method : methods) {
                if (name.equals(method.getName())
                        && (paramTypes == null || Arrays.equals(paramTypes, method.getParameterTypes()))) {
                    return method;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }
    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        try {
            return (Class<?>) findClassMethod.invoke(getParent(), className);
        } catch (IllegalAccessException e) {
            throw new ClassNotFoundException(className);
        } catch (InvocationTargetException e) {
            throw new ClassNotFoundException(className);
        }
    }
}
