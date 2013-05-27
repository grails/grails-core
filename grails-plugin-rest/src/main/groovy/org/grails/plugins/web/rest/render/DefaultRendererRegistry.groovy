/*
 * Copyright 2013 the original author or authors.
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

package org.grails.plugins.web.rest.render

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import groovy.transform.Canonical
import org.grails.plugins.web.rest.render.html.DefaultHtmlRenderer
import org.grails.plugins.web.rest.render.json.DefaultJsonRenderer
import org.grails.plugins.web.rest.render.xml.DefaultXmlRenderer
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.mime.MimeType
import org.springframework.util.ClassUtils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Default implementation of the {@link RendererRegistry} interface
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class DefaultRendererRegistry implements RendererRegistry{

    private Map<Class, Collection<Renderer>> registeredRenderers = new ConcurrentHashMap<>();
    private Map<MimeType, Renderer> defaultRenderers = new ConcurrentHashMap<>();
    private Map<RendererCacheKey, Renderer<?>> rendererCache = new ConcurrentLinkedHashMap.Builder<RendererCacheKey, Renderer<?>>()
                                                            .initialCapacity(500)
                                                            .maximumWeightedCapacity(1000)
                                                            .build();

    DefaultRendererRegistry() {
        addDefaultRenderer(new DefaultXmlRenderer<Object>(Object.class))
        addDefaultRenderer(new DefaultJsonRenderer<Object>(Object.class))
        addDefaultRenderer(new DefaultHtmlRenderer<Object>(Object.class))
        addDefaultRenderer(new DefaultHtmlRenderer<Object>(Object.class) {
            @Override
            MimeType getMimeType() { MimeType.ALL }
        })
    }

    @Override
    def <T> void addRenderer(Renderer<T> renderer) {
        Class targetType = renderer.targetType

        final renderers = registeredRenderers.get(targetType)
        if (renderers == null) {
            renderers = new ConcurrentLinkedQueue<Renderer>()
            registeredRenderers.put(targetType, renderers)
        }
        renderers.add(renderer)
    }

    @Override
    void addDefaultRenderer(Renderer<Object> renderer) {
        defaultRenderers.put(renderer.getMimeType(), renderer)
    }

    @Override
    def <T> Renderer<T> findRenderer(MimeType mimeType, T object) {
        if(object == null) return null

        final clazz = object instanceof Class ? (Class)object : object.getClass()

        final cacheKey = new RendererCacheKey(clazz, mimeType)
        Renderer<T> renderer = (Renderer<T>)rendererCache.get(cacheKey)
        if (renderer == null) {

            Class currentClass = clazz
            while(currentClass != Object) {

                renderer = findRendererForType(currentClass, mimeType)
                if (renderer) break
                currentClass = clazz.getSuperclass()
            }
            final interfaces = ClassUtils.getAllInterfaces(object)
            for(i in interfaces) {
                renderer = findRendererForType(i, mimeType)
                if (renderer) break
            }

            if (renderer == null) {
                renderer = defaultRenderers.get(mimeType)
            }
        }
        if (renderer != null) {
            rendererCache.put(cacheKey, renderer)
        }
        return renderer
    }

    protected <T> Renderer findRendererForType(Class<T> currentClass, MimeType mimeType) {
        Renderer<T> renderer = null
        final rendererList = registeredRenderers.get(currentClass)
        if (rendererList) {
            renderer = rendererList.find { Renderer<T> r -> r.mimeType == mimeType }
        }
        renderer
    }

    @Canonical
    class RendererCacheKey {
        Class clazz
        MimeType mimeType
    }
}
