/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.web.databinding.bindingsource

import groovy.transform.Canonical
import groovy.transform.CompileStatic

import jakarta.annotation.PostConstruct

import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.ClassUtils

import grails.databinding.CollectionDataBindingSource
import grails.databinding.DataBindingSource
import grails.web.mime.MimeType
import org.grails.databinding.bindingsource.DataBindingSourceCreator
import org.grails.web.util.ClassAndMimeTypeRegistry

@CompileStatic
class DefaultDataBindingSourceRegistry extends ClassAndMimeTypeRegistry<DataBindingSourceCreator, DataBindingSourceCreatorCacheKey> implements DataBindingSourceRegistry {

    /**
     * The XML binding source creator of the {@code grails-xml} module, found by name since that module depends on this
     * one, or {@code null} without it; {@code grails-xml} registers it for an ahead-of-time image.
     */
    private static final Class<? extends DataBindingSourceCreator> XML_CREATOR = xmlCreator()

    @Autowired(required = false)
    void setDataBindingSourceCreators(DataBindingSourceCreator[] dataBindingSourceCreators) {
        for (dbsc in dataBindingSourceCreators) {
            addToRegisteredObjects(dbsc.targetType, dbsc)
        }
    }

    /**
     * The creator for the JSON bodies of a type no registered creator handles; where none is set, a
     * {@link JsonDataBindingSourceCreator} that parses with a default {@code JsonSlurper}, as in Grails 8. It is read
     * when the registry is initialized, so it is set before then.
     *
     * @since 9.0
     */
    DataBindingSourceCreator defaultJsonDataBindingSourceCreator

    @PostConstruct
    void initialize() {
        registerDefault(MimeType.JSON, defaultJsonDataBindingSourceCreator ?: new JsonDataBindingSourceCreator())
        registerDefault(MimeType.TEXT_JSON, defaultJsonDataBindingSourceCreator ?: new JsonDataBindingSourceCreator())
        if (XML_CREATOR != null) {
            // the XML bodies of a type no creator bean handles, as Grails 8 parsed them
            registerDefault(MimeType.XML, BeanUtils.instantiateClass(XML_CREATOR))
            registerDefault(MimeType.TEXT_XML, BeanUtils.instantiateClass(XML_CREATOR))
        }
    }

    @SuppressWarnings('unchecked')
    private static Class<? extends DataBindingSourceCreator> xmlCreator() {
        String name = 'org.grails.web.databinding.bindingsource.XmlDataBindingSourceCreator'
        ClassLoader classLoader = DefaultDataBindingSourceRegistry.classLoader
        ClassUtils.isPresent(name, classLoader) ?
                (Class<? extends DataBindingSourceCreator>) ClassUtils.resolveClassName(name, classLoader) : null
    }

    protected DataBindingSourceCreator getDataBindingSourceCreator(MimeType mimeType, Class targetType, Object bindingSource) {
        def bindingSourceCreator = findMatchingObjectForMimeType(mimeType, targetType)
        if (bindingSourceCreator == null) {
            bindingSourceCreator = new DefaultDataBindingSourceCreator()
        }
        return bindingSourceCreator
    }

    @Override
    void addDataBindingSourceCreator(DataBindingSourceCreator creator) {
        addToRegisteredObjects(creator.targetType, creator)
    }

    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Class bindingTargetType, bindingSource) {
        def helper = getDataBindingSourceCreator(mimeType, bindingTargetType, bindingSource)
        helper.createDataBindingSource(mimeType, bindingTargetType, bindingSource)
    }

    @Override
    CollectionDataBindingSource createCollectionDataBindingSource(MimeType mimeType, Class bindingTargetType, bindingSource) {
        def helper = getDataBindingSourceCreator(mimeType, bindingTargetType, bindingSource)
        helper.createCollectionDataBindingSource(mimeType, bindingTargetType, bindingSource)
    }

    @Override
    DataBindingSourceCreatorCacheKey createCacheKey(Class type, MimeType mimeType) {
        return new DataBindingSourceCreatorCacheKey(type, mimeType)
    }

    @Canonical
    static class DataBindingSourceCreatorCacheKey {
        Class type
        MimeType mimeType
    }
}
