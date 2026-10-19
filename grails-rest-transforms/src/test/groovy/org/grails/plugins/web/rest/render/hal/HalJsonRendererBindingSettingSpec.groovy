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
package org.grails.plugins.web.rest.render.hal

import org.springframework.context.annotation.AnnotationConfigUtils
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification

import grails.databinding.CollectionDataBindingSource
import grails.databinding.DataBindingSource
import grails.databinding.SimpleMapDataBindingSource
import grails.rest.render.RendererRegistry
import grails.rest.render.hal.HalJsonRenderer
import grails.web.mapping.LinkGenerator
import grails.web.mime.MimeType
import org.grails.databinding.bindingsource.DataBindingSourceCreator
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.web.databinding.bindingsource.DataBindingSourceRegistry
import org.grails.web.databinding.bindingsource.DefaultDataBindingSourceRegistry

class HalJsonRendererBindingSettingSpec extends Specification {

    void 'a HAL renderer bean reads grails.databinding.json.jackson, which is #jackson, to parse the bodies of its type'() {
        given: 'a HAL renderer bean, and an application HAL creator'
        def context = new GenericApplicationContext()
        context.environment.propertySources.addFirst(new MapPropertySource('test', ['grails.databinding.json.jackson': jackson]))
        AnnotationConfigUtils.registerAnnotationConfigProcessors(context)
        context.registerBean(DataBindingSourceRegistry, { new DefaultDataBindingSourceRegistry() })
        def linkGenerator = Stub(LinkGenerator)
        context.registerBean(LinkGenerator, { linkGenerator })
        context.registerBean('grailsDomainClassMappingContext', MappingContext, { new KeyValueMappingContext('hal') })
        def rendererRegistry = Stub(RendererRegistry)
        context.registerBean(RendererRegistry, { rendererRegistry })
        context.registerBean('halJsonDataBindingSourceCreator', DataBindingSourceCreator, { new ApplicationHalCreator() })
        context.registerBean('productRenderer', HalJsonRenderer, Product)
        context.refresh()
        def request = new MockHttpServletRequest()
        request.contentType = HalJsonRenderer.MIME_TYPE.name
        request.content = '{"name": "MacBook"}'.bytes

        when:
        def source = context.getBean(DataBindingSourceRegistry).createDataBindingSource(HalJsonRenderer.MIME_TYPE, Product, request)

        then:
        source.getPropertyValue('name') == name

        cleanup:
        context.close()

        where:
        jackson || name
        'true'  || 'from the application creator'
        'false' || 'MacBook'
    }
}

class ApplicationHalCreator implements DataBindingSourceCreator {

    @Override
    Class getTargetType() {
        Object
    }

    @Override
    MimeType[] getMimeTypes() {
        [HalJsonRenderer.MIME_TYPE] as MimeType[]
    }

    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) {
        new SimpleMapDataBindingSource([name: 'from the application creator'])
    }

    @Override
    CollectionDataBindingSource createCollectionDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) {
        throw new UnsupportedOperationException()
    }
}
