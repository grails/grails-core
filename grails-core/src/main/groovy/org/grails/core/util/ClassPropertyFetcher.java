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

import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;

/**
 * Accesses class "properties": static fields, static getters, instance fields
 * or instance getters.
 *
 * Method and Field instances are cached for fast access.

 * @author Lari Hotari, Sagire Software Oy
 * @author Graeme Rocher
 * @deprecated Use {@link org.grails.datastore.mapping.reflect.ClassPropertyFetcher} instead
 */
@Deprecated
public class ClassPropertyFetcher {


    private final org.grails.datastore.mapping.reflect.ClassPropertyFetcher fetcher;

    public static void clearClassPropertyFetcherCache() {
        // no-op
    }

    public static ClassPropertyFetcher forClass(Class<?> c) {
        return new ClassPropertyFetcher(c);
    }

    protected ClassPropertyFetcher(Class<?> clazz) {
        this.fetcher = org.grails.datastore.mapping.reflect.ClassPropertyFetcher.forClass(clazz);
    }

    public Object getReference() {
        return BeanUtils.instantiate(fetcher.getJavaClass());
    }

    public PropertyDescriptor[] getPropertyDescriptors() {
        return fetcher.getPropertyDescriptors();
    }

    public boolean isReadableProperty(String name) {
        return fetcher.isReadableProperty(name);
    }

    public Object getPropertyValue(String name) {
        return getPropertyValue(name, false);
    }

    public Object getPropertyValue(final Object object,String name) {
        return fetcher.getPropertyValue(object, name);
    }

    public Object getPropertyValue(String name, boolean onlyInstanceProperties) {
        return fetcher.getPropertyValue(name);
    }

    public <T> T getStaticPropertyValue(String name, Class<T> c) {
        return fetcher.getPropertyValue(name,c);
    }

    public <T> T getPropertyValue(String name, Class<T> c) {
        return fetcher.getPropertyValue(name,c);
    }

    public Class<?> getPropertyType(String name) {
        return getPropertyType(name, false);
    }

    public Class<?> getPropertyType(String name, boolean onlyInstanceProperties) {
        return fetcher.getPropertyType(name);
    }

}
