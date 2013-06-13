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
import grails.rest.render.ContainerRenderer
import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import groovy.transform.Canonical
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.grails.plugins.web.rest.render.html.DefaultHtmlRenderer
import org.grails.plugins.web.rest.render.json.DefaultJsonRenderer
import org.grails.plugins.web.rest.render.xml.DefaultXmlRenderer
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.mime.MimeType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.ClassUtils
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

import javax.annotation.PostConstruct
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

    private Map<ContainerRendererCacheKey, Renderer> containerRenderers = new ConcurrentHashMap<>();
    private Map<Class, Collection<Renderer>> registeredRenderers = new ConcurrentHashMap<>();
    private Map<MimeType, Renderer> defaultRenderers = new ConcurrentHashMap<>();
    private Map<ContainerRendererCacheKey, Renderer<?>> containerRendererCache = new ConcurrentLinkedHashMap.Builder<ContainerRendererCacheKey, Renderer<?>>()
                                                                            .initialCapacity(500)
                                                                            .maximumWeightedCapacity(1000)
                                                                            .build();
    private Map<RendererCacheKey, Renderer<?>> rendererCache = new ConcurrentLinkedHashMap.Builder<RendererCacheKey, Renderer<?>>()
                                                            .initialCapacity(500)
                                                            .maximumWeightedCapacity(1000)
                                                            .build();
    @Autowired(required = false)
    GrailsConventionGroovyPageLocator groovyPageLocator

    DefaultRendererRegistry() {
    }

    @PostConstruct
    void initialize() {
        addDefaultRenderer(new DefaultXmlRenderer<Object>(Object, groovyPageLocator))
        addDefaultRenderer(new DefaultJsonRenderer<Object>(Object, groovyPageLocator))
        addDefaultRenderer(new DefaultHtmlRenderer<Object>(Object))
        addDefaultRenderer(new DefaultHtmlRenderer<Object>(Object, MimeType.ALL))
        containerRenderers.put(new ContainerRendererCacheKey(Errors, Object, MimeType.XML), new DefaultXmlRenderer(Errors))
        containerRenderers.put(new ContainerRendererCacheKey(Errors, Object, MimeType.TEXT_XML), new DefaultXmlRenderer(Errors))
        containerRenderers.put(new ContainerRendererCacheKey(Errors, Object, MimeType.JSON), new DefaultJsonRenderer(Errors))
        containerRenderers.put(new ContainerRendererCacheKey(Errors, Object, MimeType.TEXT_JSON), new DefaultJsonRenderer(Errors))
        containerRenderers.put(new ContainerRendererCacheKey(Errors, Object, MimeType.HTML), new DefaultHtmlRenderer(Errors))
        containerRenderers.put(new ContainerRendererCacheKey(Errors, Object, MimeType.ALL), new DefaultHtmlRenderer(Errors))
    }

    @Autowired(required = false)
    void setRenderers(Renderer[] renderers) {
        for(Renderer r in renderers) {
            addRenderer(r)
        }
    }

    @Override
    def <T> void addRenderer(Renderer<T> renderer) {
        if (renderer instanceof ContainerRenderer) {
            ContainerRenderer cr = (ContainerRenderer)renderer
            addContainerRenderer(cr.componentType, cr)
        }
        else {
            Class targetType = renderer.targetType

            final renderers = registeredRenderers.get(targetType)
            if (renderers == null) {
                renderers = new ConcurrentLinkedQueue<Renderer>()
                registeredRenderers.put(targetType, renderers)
            }
            renderers.add(renderer)
        }
    }

    @Override
    void addDefaultRenderer(Renderer<Object> renderer) {
        for(MimeType mt in renderer.mimeTypes) {
            defaultRenderers.put(mt, renderer)
            rendererCache.remove(new RendererCacheKey(renderer.getTargetType(), mt))
        }
    }

    @Override
    void addContainerRenderer(Class objectType, Renderer renderer) {
        for(MimeType mt in renderer.mimeTypes) {

            def key = new ContainerRendererCacheKey(renderer.getTargetType(), objectType, mt)

            containerRendererCache.remove(key)
            containerRenderers.put(key, renderer)
        }
    }

    @Override
    def <T> Renderer<T> findRenderer(MimeType mimeType, T object) {
        if(object == null) return null

        final clazz = object instanceof Class ? (Class)object : object.getClass()

        final cacheKey = new RendererCacheKey(clazz, mimeType)
        Renderer<T> renderer = (Renderer<T>)rendererCache.get(cacheKey)
        if (renderer == null) {

            Class currentClass = clazz
            while(currentClass != null) {

                renderer = findRendererForType(currentClass, mimeType)
                if (renderer) {
                    rendererCache.put(cacheKey, renderer)
                    return renderer
                }
                if (currentClass == Object) break
                currentClass = currentClass.getSuperclass()
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

    @Override
    def <C, T> Renderer<C> findContainerRenderer(MimeType mimeType, Class<C> containerType, T object) {
        if (object == null) return null
        def targetClass = object instanceof Class ? (Class) object : object.getClass()
        targetClass = getTargetClassForContainer(targetClass, object)
        def originalKey = new ContainerRendererCacheKey(containerType, targetClass, mimeType)

        Renderer<C> renderer = (Renderer<C>)containerRendererCache.get(originalKey)

        if (renderer == null) {

            def key = originalKey

            while(targetClass != null) {

                renderer = containerRenderers.get(key)
                if (renderer != null) break
                else {
                    if (targetClass == Object) break
                    targetClass = targetClass.getSuperclass()
                    key = new ContainerRendererCacheKey(containerType, targetClass, mimeType)
                    renderer = containerRenderers.get(key)
                    if (renderer != null) break
                    else {
                        final containerInterfaces = ClassUtils.getAllInterfacesForClass(containerType)
                        for(Class i in containerInterfaces) {
                            key = new ContainerRendererCacheKey(i, targetClass, mimeType)
                            renderer = containerRenderers.get(key)
                            if (renderer != null) break
                        }
                    }
                }
            }

            if (renderer == null) {
                final interfaces = ClassUtils.getAllInterfaces(object)
                for(Class i in interfaces) {
                    key = new ContainerRendererCacheKey(containerType, i, mimeType)
                    renderer = containerRenderers.get(key)
                    if (renderer) break
                    else {
                        final containerInterfaces = ClassUtils.getAllInterfacesForClass(containerType)
                        for(Class ci in containerInterfaces) {
                            key = new ContainerRendererCacheKey(ci, i, mimeType)
                            renderer = containerRenderers.get(key)
                            if (renderer != null) break
                        }
                    }
                }
            }

            if (renderer) {
                containerRendererCache.put(originalKey, renderer)
            }
        }

        return renderer
    }

    protected Class<? extends Object> getTargetClassForContainer(Class containerClass, Object object) {
        Class targetClass = containerClass
        if (containerClass.isArray()) {
            targetClass = containerClass.getComponentType()
        } else if (object instanceof Iterable) {
            if (object) {
                final iterator = object.iterator()
                final first = iterator.next()
                if (first) {
                    targetClass = first.getClass()
                }
            }
        } else if (object instanceof Map) {
            if (object) {
                final first = object.values().iterator().next()
                if (first) {
                    targetClass = first.getClass()
                }
            }
        } else if (object instanceof BeanPropertyBindingResult) {
            final target = ((BeanPropertyBindingResult) object).target
            if (target) {
                targetClass = target.getClass()
            }
        }
        return targetClass
    }

    @Override
    boolean isContainerType(Class<?> aClass) {
        if (containerRenderers.keySet().any { ContainerRendererCacheKey key -> key.containerType.isAssignableFrom(aClass) }) {
            return true
        }
        return false
    }

    protected <T> Renderer findRendererForType(Class<T> currentClass, MimeType mimeType) {
        Renderer<T> renderer = null
        final rendererList = registeredRenderers.get(currentClass)
        if (rendererList) {
            renderer = rendererList.find { Renderer<T> r -> r.mimeTypes.any { MimeType mt -> mt  == mimeType  }}
        }
        renderer
    }

    @Canonical
    class RendererCacheKey {
        Class clazz
        MimeType mimeType
    }

    @Canonical
    class ContainerRendererCacheKey {
        Class containerType
        Class clazz
        MimeType mimeType
    }
}
